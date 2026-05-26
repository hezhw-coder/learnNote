package com.medicaldatacenter.backend.openapi;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.infrastructure.CryptoSupport;
import com.medicaldatacenter.backend.security.SecuritySupport;
import com.medicaldatacenter.backend.system.SystemModule;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class OpenApiModule {

    private OpenApiModule() {
    }

    public record ApiClientRequest(@NotBlank String name, @NotBlank String clientId, @NotBlank String clientSecret,
            @NotBlank String scopes) {
    }

    public record ApiClientView(Long id, String name, String clientId, List<String> scopes, boolean enabled) {
    }

    public record ApiScopeView(Long id, String code, String name, String description) {
    }

    public record ApiAccessLogView(Long id, String clientId, String endpoint, String scope, int status, String message,
            String requestId, String remoteIp, String userAgent, Long durationMs, LocalDateTime createdAt) {
    }

    @Service
    @Validated
    public static class OpenApiAdminService {
        private final JdbcTemplate jdbcTemplate;
        private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
        private final CryptoSupport.TextEncryptor textEncryptor;
        private final SystemModule.UserAccountService userAccountService;

        public OpenApiAdminService(JdbcTemplate jdbcTemplate, CryptoSupport.TextEncryptor textEncryptor,
                SystemModule.UserAccountService userAccountService) {
            this.jdbcTemplate = jdbcTemplate;
            this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
            this.textEncryptor = textEncryptor;
            this.userAccountService = userAccountService;
        }

        public List<ApiClientView> listClients() {
            return jdbcTemplate.query("select id, name, client_id, scopes, enabled from api_client order by id",
                    (rs, rowNum) -> new ApiClientView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("client_id"),
                            List.of(rs.getString("scopes").split(",")),
                            rs.getBoolean("enabled")));
        }

        public ApiClientView createClient(ApiClientRequest request) {
            validateScopes(request.scopes());
            jdbcTemplate.update("insert into api_client(name, client_id, encrypted_secret, scopes, enabled) values (?, ?, ?, ?, ?)",
                    request.name(), request.clientId(), textEncryptor.encrypt(request.clientSecret()), request.scopes(),
                    true);
            Long id = jdbcTemplate.queryForObject("select max(id) from api_client", Long.class);
            userAccountService.audit(userAccountService.currentActor(), "OPEN_API_CLIENT_CREATE", "api_client",
                    request.clientId(), request.scopes());
            return jdbcTemplate.query(
                    "select id, name, client_id, scopes, enabled from api_client where id = ?",
                    (rs, rowNum) -> new ApiClientView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("client_id"),
                            List.of(rs.getString("scopes").split(",")),
                            rs.getBoolean("enabled")),
                    id).get(0);
        }

        public List<ApiScopeView> listScopes() {
            return jdbcTemplate.query("select id, code, name, description from api_scope order by id",
                    (rs, rowNum) -> new ApiScopeView(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description")));
        }

        public List<ApiAccessLogView> listLogs() {
            return jdbcTemplate.query(
                    "select id, client_id, endpoint, scope, status, message, request_id, remote_ip, user_agent, duration_ms, created_at "
                            + "from api_access_log order by id desc",
                    (rs, rowNum) -> new ApiAccessLogView(
                            rs.getLong("id"),
                            rs.getString("client_id"),
                            rs.getString("endpoint"),
                            rs.getString("scope"),
                            rs.getInt("status"),
                            rs.getString("message"),
                            rs.getString("request_id"),
                            rs.getString("remote_ip"),
                            rs.getString("user_agent"),
                            rs.getObject("duration_ms", Long.class),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public ApiClientView validateClient(String clientId, String clientSecret, String scopeText) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select id, name, client_id, encrypted_secret, scopes, enabled from api_client where client_id = ?",
                    clientId);
            if (rows.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid client");
            }
            Map<String, Object> row = rows.get(0);
            if (!(Boolean) row.get("enabled")) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "client disabled");
            }
            String storedSecret = resolveSecret(String.valueOf(row.get("encrypted_secret")));
            if (!storedSecret.equals(clientSecret)) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid client");
            }
            List<String> allowedScopes = List.of(String.valueOf(row.get("scopes")).split(","));
            List<String> scopes = (scopeText == null || scopeText.isBlank()) ? allowedScopes
                    : List.of(scopeText.trim().split("\\s+"));
            validateScopes(String.join(",", scopes));
            if (!allowedScopes.containsAll(scopes)) {
                throw new CommonSupport.BusinessException(HttpStatus.FORBIDDEN, "scope not allowed");
            }
            return new ApiClientView(((Number) row.get("id")).longValue(), String.valueOf(row.get("name")),
                    String.valueOf(row.get("client_id")), scopes, true);
        }

        public void logAccess(String clientId, String endpoint, String scope, int status, String message) {
            jdbcTemplate.update(
                    "insert into api_access_log(client_id, endpoint, scope, status, message, request_id, remote_ip, user_agent, duration_ms) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    clientId, endpoint, scope, status, message, CommonSupport.currentRequestId(),
                    CommonSupport.currentRemoteIp(), CommonSupport.currentUserAgent(), CommonSupport.currentDurationMs());
        }

        public List<Map<String, Object>> patients(String patientCode, String gender, String nameKeyword, String startDate,
                String endDate) {
            QuerySpec query = new QuerySpec(
                    "select patient_code, patient_name, gender, birth_date from cdm_patient",
                    " order by id desc");
            addEqualsFilter(query, "patient_code", "patientCode", patientCode);
            addEqualsFilter(query, "gender", "gender", gender);
            addLikeFilter(query, "patient_name", "nameKeyword", nameKeyword);
            addStartDateFilter(query, "birth_date", "startDate", startDate);
            addEndDateFilter(query, "birth_date", "endDate", endDate);
            return applyMaskers(execute(query), Map.of("patient_name", this::maskName));
        }

        public List<Map<String, Object>> encounters(String patientCode, String encounterType, String departmentName,
                String doctorName, String startDate, String endDate) {
            QuerySpec query = new QuerySpec(
                    "select patient_code, encounter_code, encounter_type, department_name, doctor_name, encounter_date "
                            + "from cdm_encounter",
                    " order by encounter_date desc, id desc");
            addEqualsFilter(query, "patient_code", "patientCode", patientCode);
            addEqualsFilter(query, "encounter_type", "encounterType", encounterType);
            addLikeFilter(query, "department_name", "departmentName", departmentName);
            addLikeFilter(query, "doctor_name", "doctorName", doctorName);
            addStartDateFilter(query, "encounter_date", "startDate", startDate);
            addEndDateFilter(query, "encounter_date", "endDate", endDate);
            return applyMaskers(execute(query), Map.of("doctor_name", this::maskName));
        }

        public List<Map<String, Object>> labs(String patientCode, String encounterCode, String itemCode, String itemName,
                Boolean abnormalOnly, String startDate, String endDate) {
            QuerySpec query = new QuerySpec(
                    "select patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date "
                            + "from cdm_lab",
                    " order by report_date desc, id desc");
            addEqualsFilter(query, "patient_code", "patientCode", patientCode);
            addEqualsFilter(query, "encounter_code", "encounterCode", encounterCode);
            addEqualsFilter(query, "item_code", "itemCode", itemCode);
            addLikeFilter(query, "item_name", "itemName", itemName);
            if (Boolean.TRUE.equals(abnormalOnly)) {
                query.conditions().add("coalesce(upper(result_flag), '') <> 'NORMAL'");
            }
            addStartDateFilter(query, "report_date", "startDate", startDate);
            addEndDateFilter(query, "report_date", "endDate", endDate);
            return execute(query);
        }

        public List<Map<String, Object>> medications(String patientCode, String drugCode, String drugName, String startDate,
                String endDate) {
            QuerySpec query = new QuerySpec(
                    "select patient_code, drug_code, drug_name, dose_value, dose_unit, order_time from cdm_medication_order",
                    " order by order_time desc, id desc");
            addEqualsFilter(query, "patient_code", "patientCode", patientCode);
            addEqualsFilter(query, "drug_code", "drugCode", drugCode);
            addLikeFilter(query, "drug_name", "drugName", drugName);
            addStartDateFilter(query, "order_time", "startDate", startDate);
            addEndDateFilter(query, "order_time", "endDate", endDate);
            return execute(query);
        }

        public List<Map<String, Object>> reports() {
            return jdbcTemplate.queryForList("select id, template_id, snapshot_json, export_path, created_at from report_snapshot order by id desc");
        }

        private List<Map<String, Object>> execute(QuerySpec query) {
            String whereClause = query.conditions().isEmpty() ? ""
                    : " where " + String.join(" and ", query.conditions());
            return namedParameterJdbcTemplate.queryForList(query.baseSql() + whereClause + query.orderBy(),
                    query.parameters());
        }

        private List<Map<String, Object>> applyMaskers(List<Map<String, Object>> rows,
                Map<String, UnaryOperator<String>> maskers) {
            List<Map<String, Object>> sanitized = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> copy = new LinkedHashMap<>(row);
                maskers.forEach((field, masker) -> {
                    Object value = copy.get(field);
                    if (value instanceof String text) {
                        copy.put(field, masker.apply(text));
                    }
                });
                sanitized.add(copy);
            }
            return sanitized;
        }

        private void addEqualsFilter(QuerySpec query, String column, String parameterName, String rawValue) {
            String value = normalize(rawValue);
            if (value == null) {
                return;
            }
            query.conditions().add(column + " = :" + parameterName);
            query.parameters().addValue(parameterName, value);
        }

        private void addLikeFilter(QuerySpec query, String column, String parameterName, String rawValue) {
            String value = normalize(rawValue);
            if (value == null) {
                return;
            }
            query.conditions().add("lower(" + column + ") like :" + parameterName);
            query.parameters().addValue(parameterName, "%" + value.toLowerCase() + "%");
        }

        private void addStartDateFilter(QuerySpec query, String column, String parameterName, String rawValue) {
            String value = normalize(rawValue);
            if (value == null) {
                return;
            }
            query.conditions().add(column + " >= :" + parameterName);
            query.parameters().addValue(parameterName, value);
        }

        private void addEndDateFilter(QuerySpec query, String column, String parameterName, String rawValue) {
            String value = normalize(rawValue);
            if (value == null) {
                return;
            }
            query.conditions().add(column + " <= :" + parameterName);
            query.parameters().addValue(parameterName, value);
        }

        private String normalize(String rawValue) {
            if (rawValue == null) {
                return null;
            }
            String normalized = rawValue.trim();
            return normalized.isEmpty() ? null : normalized;
        }

        private String maskName(String value) {
            String normalized = normalize(value);
            if (normalized == null) {
                return value;
            }
            if (normalized.length() == 1) {
                return "*";
            }
            if (normalized.length() == 2) {
                return normalized.charAt(0) + "*";
            }
            return normalized.charAt(0) + "*".repeat(normalized.length() - 2) + normalized.charAt(normalized.length() - 1);
        }

        private record QuerySpec(String baseSql, String orderBy, List<String> conditions,
                MapSqlParameterSource parameters) {
            private QuerySpec(String baseSql, String orderBy) {
                this(baseSql, orderBy, new ArrayList<>(), new MapSqlParameterSource());
            }
        }

        private String resolveSecret(String stored) {
            try {
                return textEncryptor.decrypt(stored);
            } catch (Exception ignored) {
                try {
                    return new String(Base64.getDecoder().decode(stored), StandardCharsets.UTF_8);
                } catch (Exception second) {
                    return stored;
                }
            }
        }

        private void validateScopes(String scopeText) {
            if (scopeText == null || scopeText.isBlank()) {
                return;
            }
            List<String> requestedScopes = List.of(scopeText.replace(",", " ").trim().split("\\s+"));
            Integer knownScopeCount = jdbcTemplate.queryForObject(
                    "select count(1) from api_scope where code in (%s)".formatted(requestedScopes.stream()
                            .map(ignored -> "?")
                            .reduce((left, right) -> left + "," + right)
                            .orElse("?")),
                    Integer.class,
                    requestedScopes.toArray());
            if (knownScopeCount == null || knownScopeCount != requestedScopes.size()) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "unknown scope requested");
            }
        }
    }

    @Service
    public static class LocalRateLimiter {
        private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
                """
                        local key = KEYS[1]
                        local now = tonumber(ARGV[1])
                        local capacity = tonumber(ARGV[2])
                        local refillTokens = tonumber(ARGV[3])
                        local refillSeconds = tonumber(ARGV[4])
                        local ttl = tonumber(ARGV[5])
                        local state = redis.call('HMGET', key, 'tokens', 'lastRefill')
                        local tokens = tonumber(state[1])
                        local lastRefill = tonumber(state[2])
                        if tokens == nil then
                          tokens = capacity
                        end
                        if lastRefill == nil then
                          lastRefill = now
                        end
                        if refillSeconds > 0 then
                          local elapsed = now - lastRefill
                          if elapsed > 0 then
                            local refillTimes = math.floor(elapsed / refillSeconds)
                            if refillTimes > 0 then
                              tokens = math.min(capacity, tokens + refillTimes * refillTokens)
                              lastRefill = lastRefill + refillTimes * refillSeconds
                            end
                          end
                        end
                        if tokens <= 0 then
                          redis.call('HSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
                          redis.call('EXPIRE', key, ttl)
                          return {0, tokens}
                        end
                        tokens = tokens - 1
                        redis.call('HSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
                        redis.call('EXPIRE', key, ttl)
                        return {1, tokens}
                        """,
                List.class);
        private final JdbcTemplate jdbcTemplate;
        private final StringRedisTemplate stringRedisTemplate;
        private final int defaultCapacity;
        private final int defaultRefillTokens;
        private final int defaultRefillSeconds;
        private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

        public LocalRateLimiter(JdbcTemplate jdbcTemplate, ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                @Value("${app.rate-limit.default-capacity}") int defaultCapacity,
                @Value("${app.rate-limit.refill-tokens}") int defaultRefillTokens,
                @Value("${app.rate-limit.refill-seconds}") int defaultRefillSeconds) {
            this.jdbcTemplate = jdbcTemplate;
            this.stringRedisTemplate = redisTemplateProvider.getIfAvailable();
            this.defaultCapacity = defaultCapacity;
            this.defaultRefillTokens = defaultRefillTokens;
            this.defaultRefillSeconds = defaultRefillSeconds;
        }

        public boolean tryConsume(String clientId, String endpoint) {
            Rule rule = jdbcTemplate.query(
                    "select capacity, refill_tokens, refill_seconds from api_rate_limit_rule where client_id = ? and endpoint = ?",
                    rs -> rs.next()
                            ? new Rule(rs.getInt("capacity"), rs.getInt("refill_tokens"), rs.getInt("refill_seconds"))
                            : new Rule(defaultCapacity, defaultRefillTokens, defaultRefillSeconds),
                    clientId, endpoint);
            Boolean redisDecision = tryConsumeWithRedis(clientId, endpoint, rule);
            if (redisDecision != null) {
                return redisDecision;
            }
            return tryConsumeLocally(clientId, endpoint, rule);
        }

        private Boolean tryConsumeWithRedis(String clientId, String endpoint, Rule rule) {
            if (stringRedisTemplate == null) {
                return null;
            }
            try {
                List result = stringRedisTemplate.execute(TOKEN_BUCKET_SCRIPT,
                        List.of("mdc:openapi:ratelimit:" + clientId + ":" + endpoint.replace("/", "_")),
                        String.valueOf(Instant.now().getEpochSecond()),
                        String.valueOf(rule.capacity()),
                        String.valueOf(rule.refillTokens()),
                        String.valueOf(rule.refillSeconds()),
                        String.valueOf(Math.max(rule.refillSeconds() * 2L, 120L)));
                if (result == null || result.isEmpty()) {
                    return null;
                }
                Object allowed = result.get(0);
                return String.valueOf(allowed).equals("1");
            } catch (Exception ignored) {
                return null;
            }
        }

        private boolean tryConsumeLocally(String clientId, String endpoint, Rule rule) {
            String key = clientId + "::" + endpoint;
            BucketState state = buckets.computeIfAbsent(key, ignored -> new BucketState(rule.capacity(), Instant.now()));
            synchronized (state) {
                Instant now = Instant.now();
                long seconds = Math.max(0, now.getEpochSecond() - state.lastRefill().getEpochSecond());
                long refillTimes = rule.refillSeconds() == 0 ? 0 : seconds / rule.refillSeconds();
                if (refillTimes > 0) {
                    long replenished = Math.min(rule.capacity(), state.tokens() + refillTimes * rule.refillTokens());
                    state.tokens = replenished;
                    state.lastRefill = now;
                }
                if (state.tokens <= 0) {
                    return false;
                }
                state.tokens--;
                return true;
            }
        }

        private record Rule(int capacity, int refillTokens, int refillSeconds) {
        }

        private static final class BucketState {
            private long tokens;
            private Instant lastRefill;

            private BucketState(long tokens, Instant lastRefill) {
                this.tokens = tokens;
                this.lastRefill = lastRefill;
            }

            public long tokens() {
                return tokens;
            }

            public Instant lastRefill() {
                return lastRefill;
            }
        }
    }

    @RestController
    @RequestMapping("/api/open-api")
    public static class OpenApiAdminController {
        private final OpenApiAdminService openApiAdminService;

        public OpenApiAdminController(OpenApiAdminService openApiAdminService) {
            this.openApiAdminService = openApiAdminService;
        }

        @GetMapping("/clients")
        @PreAuthorize("hasAuthority('OPEN_API_MANAGE')")
        public CommonSupport.ApiResponse<List<ApiClientView>> clients() {
            return CommonSupport.success(openApiAdminService.listClients());
        }

        @PostMapping("/clients")
        @PreAuthorize("hasAuthority('OPEN_API_MANAGE')")
        public CommonSupport.ApiResponse<ApiClientView> createClient(@RequestBody @Validated ApiClientRequest request) {
            return CommonSupport.success(openApiAdminService.createClient(request));
        }

        @GetMapping("/scopes")
        @PreAuthorize("hasAuthority('OPEN_API_MANAGE')")
        public CommonSupport.ApiResponse<List<ApiScopeView>> scopes() {
            return CommonSupport.success(openApiAdminService.listScopes());
        }

        @GetMapping("/logs")
        @PreAuthorize("hasAuthority('OPEN_API_MANAGE')")
        public CommonSupport.ApiResponse<List<ApiAccessLogView>> logs() {
            return CommonSupport.success(openApiAdminService.listLogs());
        }
    }

    @RestController
    @RequestMapping("/open-api/v1")
    public static class PublicOpenApiController {
        private final OpenApiAdminService openApiAdminService;
        private final LocalRateLimiter localRateLimiter;

        public PublicOpenApiController(OpenApiAdminService openApiAdminService, LocalRateLimiter localRateLimiter) {
            this.openApiAdminService = openApiAdminService;
            this.localRateLimiter = localRateLimiter;
        }

        @GetMapping("/patients")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> patients(
                Authentication authentication,
                @RequestParam(required = false) String patientCode,
                @RequestParam(required = false) String gender,
                @RequestParam(required = false) String nameKeyword,
                @RequestParam(required = false) String startDate,
                @RequestParam(required = false) String endDate) {
            return handle(authentication, "/open-api/v1/patients", "patients.read",
                    openApiAdminService.patients(patientCode, gender, nameKeyword, startDate, endDate));
        }

        @GetMapping("/encounters")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> encounters(
                Authentication authentication,
                @RequestParam(required = false) String patientCode,
                @RequestParam(required = false) String encounterType,
                @RequestParam(required = false) String departmentName,
                @RequestParam(required = false) String doctorName,
                @RequestParam(required = false) String startDate,
                @RequestParam(required = false) String endDate) {
            return handle(authentication, "/open-api/v1/encounters", "encounters.read",
                    openApiAdminService.encounters(patientCode, encounterType, departmentName, doctorName, startDate,
                            endDate));
        }

        @GetMapping("/labs")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> labs(
                Authentication authentication,
                @RequestParam(required = false) String patientCode,
                @RequestParam(required = false) String encounterCode,
                @RequestParam(required = false) String itemCode,
                @RequestParam(required = false) String itemName,
                @RequestParam(required = false) Boolean abnormalOnly,
                @RequestParam(required = false) String startDate,
                @RequestParam(required = false) String endDate) {
            return handle(authentication, "/open-api/v1/labs", "labs.read",
                    openApiAdminService.labs(patientCode, encounterCode, itemCode, itemName, abnormalOnly, startDate,
                            endDate));
        }

        @GetMapping("/medications")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> medications(
                Authentication authentication,
                @RequestParam(required = false) String patientCode,
                @RequestParam(required = false) String drugCode,
                @RequestParam(required = false) String drugName,
                @RequestParam(required = false) String startDate,
                @RequestParam(required = false) String endDate) {
            return handle(authentication, "/open-api/v1/medications", "medications.read",
                    openApiAdminService.medications(patientCode, drugCode, drugName, startDate, endDate));
        }

        @GetMapping("/reports")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> reports(Authentication authentication) {
            return handle(authentication, "/open-api/v1/reports", "reports.read", openApiAdminService.reports());
        }

        private CommonSupport.ApiResponse<List<Map<String, Object>>> handle(
                Authentication authentication,
                String endpoint,
                String scope,
                List<Map<String, Object>> payload) {
            SecuritySupport.AppPrincipal principal = authorize(authentication, endpoint, scope);
            if (!localRateLimiter.tryConsume(principal.getUsername(), endpoint)) {
                openApiAdminService.logAccess(principal.getUsername(), endpoint, scope, 429, "rate limited");
                throw new CommonSupport.BusinessException(HttpStatus.TOO_MANY_REQUESTS, "rate limited");
            }
            openApiAdminService.logAccess(principal.getUsername(), endpoint, scope, 200, "ok rows=" + payload.size());
            return CommonSupport.success(payload);
        }

        private SecuritySupport.AppPrincipal authorize(Authentication authentication, String endpoint, String scope) {
            if (authentication == null || !(authentication.getPrincipal() instanceof SecuritySupport.AppPrincipal principal)) {
                openApiAdminService.logAccess("anonymous", endpoint, scope, 401, "missing token");
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "missing token");
            }
            if (!"OPEN_API".equals(principal.tokenKind())) {
                openApiAdminService.logAccess(principal.getUsername(), endpoint, scope, 403, "platform token rejected");
                throw new CommonSupport.BusinessException(HttpStatus.FORBIDDEN, "open api token required");
            }
            boolean allowed = principal.getAuthorities().stream()
                    .map(Object::toString)
                    .anyMatch(authority -> authority.equals("SCOPE_" + scope));
            if (!allowed) {
                openApiAdminService.logAccess(principal.getUsername(), endpoint, scope, 403, "scope not allowed");
                throw new CommonSupport.BusinessException(HttpStatus.FORBIDDEN, "scope not allowed");
            }
            return principal;
        }
    }
}
