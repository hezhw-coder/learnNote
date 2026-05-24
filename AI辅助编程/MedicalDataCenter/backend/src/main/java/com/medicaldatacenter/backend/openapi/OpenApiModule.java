package com.medicaldatacenter.backend.openapi;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.infrastructure.CryptoSupport;
import com.medicaldatacenter.backend.security.SecuritySupport;
import com.medicaldatacenter.backend.system.SystemModule;

import jakarta.validation.constraints.NotBlank;

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
            LocalDateTime createdAt) {
    }

    @Service
    @Validated
    public static class OpenApiAdminService {
        private final JdbcTemplate jdbcTemplate;
        private final CryptoSupport.TextEncryptor textEncryptor;
        private final SystemModule.UserAccountService userAccountService;

        public OpenApiAdminService(JdbcTemplate jdbcTemplate, CryptoSupport.TextEncryptor textEncryptor,
                SystemModule.UserAccountService userAccountService) {
            this.jdbcTemplate = jdbcTemplate;
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
                    "select id, client_id, endpoint, scope, status, message, created_at from api_access_log order by id desc",
                    (rs, rowNum) -> new ApiAccessLogView(
                            rs.getLong("id"),
                            rs.getString("client_id"),
                            rs.getString("endpoint"),
                            rs.getString("scope"),
                            rs.getInt("status"),
                            rs.getString("message"),
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
            if (!allowedScopes.containsAll(scopes)) {
                throw new CommonSupport.BusinessException(HttpStatus.FORBIDDEN, "scope not allowed");
            }
            return new ApiClientView(((Number) row.get("id")).longValue(), String.valueOf(row.get("name")),
                    String.valueOf(row.get("client_id")), scopes, true);
        }

        public void logAccess(String clientId, String endpoint, String scope, int status, String message) {
            jdbcTemplate.update(
                    "insert into api_access_log(client_id, endpoint, scope, status, message) values (?, ?, ?, ?, ?)",
                    clientId, endpoint, scope, status, message);
        }

        public List<Map<String, Object>> patients() {
            return jdbcTemplate.queryForList(
                    "select patient_code, patient_name, gender, birth_date from cdm_patient order by id");
        }

        public List<Map<String, Object>> encounters() {
            return List.of(Map.of("message", "encounter dataset is reserved in MVP skeleton"));
        }

        public List<Map<String, Object>> labs() {
            return List.of(Map.of("message", "lab dataset is reserved in MVP skeleton"));
        }

        public List<Map<String, Object>> reports() {
            return jdbcTemplate.queryForList("select id, template_id, snapshot_json, export_path, created_at from report_snapshot order by id desc");
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
    }

    @Service
    public static class LocalRateLimiter {
        private final JdbcTemplate jdbcTemplate;
        private final int defaultCapacity;
        private final int defaultRefillTokens;
        private final int defaultRefillSeconds;
        private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

        public LocalRateLimiter(JdbcTemplate jdbcTemplate, @Value("${app.rate-limit.default-capacity}") int defaultCapacity,
                @Value("${app.rate-limit.refill-tokens}") int defaultRefillTokens,
                @Value("${app.rate-limit.refill-seconds}") int defaultRefillSeconds) {
            this.jdbcTemplate = jdbcTemplate;
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
        @PreAuthorize("hasAuthority('SCOPE_patients.read')")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> patients(@RequestHeader("Authorization") String ignored,
                org.springframework.security.core.Authentication authentication) {
            return handle(authentication, "/open-api/v1/patients", "patients.read", openApiAdminService.patients());
        }

        @GetMapping("/encounters")
        @PreAuthorize("hasAuthority('SCOPE_encounters.read')")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> encounters(
                org.springframework.security.core.Authentication authentication) {
            return handle(authentication, "/open-api/v1/encounters", "encounters.read", openApiAdminService.encounters());
        }

        @GetMapping("/labs")
        @PreAuthorize("hasAuthority('SCOPE_labs.read')")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> labs(
                org.springframework.security.core.Authentication authentication) {
            return handle(authentication, "/open-api/v1/labs", "labs.read", openApiAdminService.labs());
        }

        @GetMapping("/reports")
        @PreAuthorize("hasAuthority('SCOPE_reports.read')")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> reports(
                org.springframework.security.core.Authentication authentication) {
            return handle(authentication, "/open-api/v1/reports", "reports.read", openApiAdminService.reports());
        }

        private CommonSupport.ApiResponse<List<Map<String, Object>>> handle(
                org.springframework.security.core.Authentication authentication,
                String endpoint,
                String scope,
                List<Map<String, Object>> payload) {
            SecuritySupport.AppPrincipal principal = (SecuritySupport.AppPrincipal) authentication.getPrincipal();
            if (!localRateLimiter.tryConsume(principal.getUsername(), endpoint)) {
                openApiAdminService.logAccess(principal.getUsername(), endpoint, scope, 429, "rate limited");
                throw new CommonSupport.BusinessException(HttpStatus.TOO_MANY_REQUESTS, "rate limited");
            }
            openApiAdminService.logAccess(principal.getUsername(), endpoint, scope, 200, "ok");
            return CommonSupport.success(payload);
        }
    }
}
