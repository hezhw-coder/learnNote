package com.medicaldatacenter.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.medicaldatacenter.backend.auth.AuthModule;
import com.medicaldatacenter.backend.system.SystemModule;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;

import com.medicaldatacenter.backend.common.CommonSupport;

public final class SecuritySupport {

    private SecuritySupport() {
    }

    @Configuration
    @EnableMethodSecurity
    public static class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, RequestIdFilter requestIdFilter,
                JwtAuthenticationFilter jwtAuthenticationFilter)
                throws Exception {
            return http
                    .cors(cors -> {
                    })
                    .csrf(csrf -> csrf.disable())
                    .headers(headers -> headers
                            .contentTypeOptions(contentTypeOptions -> {
                            })
                            .frameOptions(frameOptions -> frameOptions.sameOrigin())
                            .referrerPolicy(referrer -> referrer
                                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/api/auth/login", "/api/auth/refresh", "/oauth2/token",
                                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                            .requestMatchers("/open-api/**").authenticated()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll())
                    .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(jwtAuthenticationFilter, RequestIdFilter.class)
                    .build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource(
                @Value("${app.security.allowed-origin-patterns:http://localhost:*}") String allowedOriginPatterns) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowCredentials(true);
            configuration.setAllowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList());
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setExposedHeaders(List.of("Content-Disposition", CommonSupport.REQUEST_ID_HEADER));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    @Component
    public static class RequestIdFilter extends OncePerRequestFilter {
        private final String requestIdHeaderName;

        public RequestIdFilter(@Value("${app.security.request-id-header:" + CommonSupport.REQUEST_ID_HEADER + "}") String requestIdHeaderName) {
            this.requestIdHeaderName = requestIdHeaderName;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, java.io.IOException {
            String requestId = sanitizeRequestId(request.getHeader(requestIdHeaderName));
            if (!StringUtils.hasText(requestId)) {
                requestId = UUID.randomUUID().toString();
            }
            long requestStartedAt = System.currentTimeMillis();
            request.setAttribute(CommonSupport.REQUEST_ID_ATTRIBUTE, requestId);
            request.setAttribute(CommonSupport.REQUEST_STARTED_AT_ATTRIBUTE, requestStartedAt);
            response.setHeader(requestIdHeaderName, requestId);
            CommonSupport.RequestIdContext.setCurrent(requestId);
            CommonSupport.RequestStartContext.setCurrent(requestStartedAt);
            try {
                filterChain.doFilter(request, response);
            } finally {
                CommonSupport.RequestStartContext.clear();
                CommonSupport.RequestIdContext.clear();
            }
        }

        private String sanitizeRequestId(String requestId) {
            if (!StringUtils.hasText(requestId)) {
                return null;
            }
            String normalized = requestId.replace("\r", "").replace("\n", "").trim();
            if (!StringUtils.hasText(normalized)) {
                return null;
            }
            return normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
        }
    }

    @Service
    public static class JwtService {
        private final CandidateKey activeKey;
        private final List<CandidateKey> verifyKeys;
        private final String issuer;
        private final long platformMinutes;
        private final long refreshMinutes;
        private final long openApiMinutes;

        public JwtService(
                @Value("${app.security.jwt-secret}") String secret,
                @Value("${app.security.jwt-active-kid:mdc-k1}") String activeKid,
                @Value("${app.security.jwt-previous-secrets:}") String previousSecrets,
                @Value("${app.security.issuer}") String issuer,
                @Value("${app.security.platform-token-minutes}") long platformMinutes,
                @Value("${app.security.refresh-token-minutes:10080}") long refreshMinutes,
                @Value("${app.security.open-api-token-minutes}") long openApiMinutes) {
            this.activeKey = new CandidateKey(normalizeKeyId(activeKid), buildKey(secret));
            this.verifyKeys = buildVerifyKeys(activeKey, previousSecrets);
            this.issuer = issuer;
            this.platformMinutes = platformMinutes;
            this.refreshMinutes = refreshMinutes;
            this.openApiMinutes = openApiMinutes;
        }

        public long platformTokenMinutes() {
            return platformMinutes;
        }

        public long refreshTokenMinutes() {
            return refreshMinutes;
        }

        public String createPlatformAccessToken(SystemModule.AuthenticatedUser user, String sessionId) {
            return createPlatformToken(user, sessionId, "PLATFORM", platformMinutes);
        }

        public String createPlatformRefreshToken(SystemModule.AuthenticatedUser user, String sessionId) {
            return createPlatformToken(user, sessionId, "REFRESH", refreshMinutes);
        }

        public String createOpenApiToken(String clientId, List<String> scopes) {
            Instant now = Instant.now();
            List<String> authorities = scopes.stream().map(scope -> "SCOPE_" + scope).toList();
            return Jwts.builder()
                    .id(UUID.randomUUID().toString())
                    .issuer(issuer)
                    .subject(clientId)
                    .issuedAt(java.util.Date.from(now))
                    .expiration(java.util.Date.from(now.plus(openApiMinutes, ChronoUnit.MINUTES)))
                    .claim("tokenKind", "OPEN_API")
                    .claim("kid", activeKey.keyId())
                    .claim("authorities", authorities)
                    .claim("scopes", scopes)
                    .signWith(activeKey.key())
                    .compact();
        }

        public TokenClaims parse(String token) {
            RuntimeException lastException = null;
            for (CandidateKey candidateKey : verifyKeys) {
                try {
                    Claims claims = Jwts.parser().verifyWith(candidateKey.key()).build().parseSignedClaims(token)
                            .getPayload();
                    @SuppressWarnings("unchecked")
                    List<String> authorities = (List<String>) claims.getOrDefault("authorities", List.of());
                    return new TokenClaims(
                            claims.getSubject(),
                            Objects.toString(claims.get("tokenKind"), "PLATFORM"),
                            Objects.toString(claims.get("displayName"), claims.getSubject()),
                            authorities,
                            Objects.toString(claims.get("sid"), null),
                            claims.getId(),
                            claims.getExpiration().toInstant(),
                            Objects.toString(claims.get("kid"), candidateKey.keyId()));
                } catch (RuntimeException exception) {
                    lastException = exception;
                }
            }
            throw new IllegalArgumentException("invalid token", lastException);
        }

        public Authentication toAuthentication(TokenClaims claims, String token) {
            List<GrantedAuthority> grantedAuthorities = claims.authorities().stream()
                    .map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority))
                    .toList();
            AppPrincipal principal = new AppPrincipal(
                    claims.subject(),
                    claims.tokenKind(),
                    grantedAuthorities,
                    claims.displayName(),
                    claims.sessionId());
            return new UsernamePasswordAuthenticationToken(principal, token, grantedAuthorities);
        }

        private String createPlatformToken(SystemModule.AuthenticatedUser user, String sessionId, String tokenKind,
                long expiresMinutes) {
            Instant now = Instant.now();
            return Jwts.builder()
                    .id(UUID.randomUUID().toString())
                    .issuer(issuer)
                    .subject(user.username())
                    .issuedAt(java.util.Date.from(now))
                    .expiration(java.util.Date.from(now.plus(expiresMinutes, ChronoUnit.MINUTES)))
                    .claim("tokenKind", tokenKind)
                    .claim("kid", activeKey.keyId())
                    .claim("displayName", user.displayName())
                    .claim("authorities", user.authorities())
                    .claim("sid", sessionId)
                    .signWith(activeKey.key())
                    .compact();
        }

        private SecretKey buildKey(String secret) {
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalArgumentException("app.security.jwt-secret must be at least 32 bytes for HS256");
            }
            return Keys.hmacShaKeyFor(secretBytes);
        }

        private String normalizeKeyId(String keyId) {
            if (!StringUtils.hasText(keyId)) {
                throw new IllegalArgumentException("app.security.jwt-active-kid must not be blank");
            }
            return keyId.trim();
        }

        private List<CandidateKey> buildVerifyKeys(CandidateKey active, String previousSecretsSpec) {
            Map<String, CandidateKey> keys = new LinkedHashMap<>();
            keys.put(active.keyId(), active);
            if (StringUtils.hasText(previousSecretsSpec)) {
                Arrays.stream(previousSecretsSpec.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(this::parseCandidateKey)
                        .forEach(candidateKey -> keys.putIfAbsent(candidateKey.keyId(), candidateKey));
            }
            return List.copyOf(keys.values());
        }

        private CandidateKey parseCandidateKey(String rawValue) {
            int separator = rawValue.indexOf(':');
            if (separator <= 0 || separator >= rawValue.length() - 1) {
                throw new IllegalArgumentException(
                        "app.security.jwt-previous-secrets format must be kid:secret");
            }
            String keyId = normalizeKeyId(rawValue.substring(0, separator));
            String secret = rawValue.substring(separator + 1).trim();
            return new CandidateKey(keyId, buildKey(secret));
        }

        private record CandidateKey(String keyId, SecretKey key) {
        }
    }

    public record TokenClaims(
            String subject,
            String tokenKind,
            String displayName,
            List<String> authorities,
            String sessionId,
            String tokenId,
            Instant expiresAt,
            String keyId) {
    }

    @Component
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final AuthModule.SessionService sessionService;

        public JwtAuthenticationFilter(JwtService jwtService, AuthModule.SessionService sessionService) {
            this.jwtService = jwtService;
            this.sessionService = sessionService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, java.io.IOException {
            String header = request.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                try {
                    TokenClaims claims = jwtService.parse(header.substring(7));
                    if ("PLATFORM".equals(claims.tokenKind())) {
                        sessionService.assertSessionAvailable(claims.sessionId());
                    } else if (!"OPEN_API".equals(claims.tokenKind())) {
                        throw new IllegalArgumentException("unsupported token");
                    }
                    Authentication authentication = jwtService.toAuthentication(claims, header.substring(7));
                    org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                } catch (Exception ignored) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"code\":401,\"message\":\"invalid token\",\"data\":null,\"requestId\":\""
                            + CommonSupport.currentRequestId() + "\",\"timestamp\":\"" + OffsetDateTime.now() + "\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }

    @Component
    @Profile("prod")
    public static class ProdSecurityBaselineValidator {
        private static final List<String> DISALLOWED_JWT_SECRETS = List.of(
                "change-this-secret-change-this-secret-change-this-secret",
                "medical-data-center-docker-jwt-secret-2026");
        private static final String DEFAULT_AES_KEY = "0123456789abcdef0123456789abcdef";

        private final String jwtSecret;
        private final String jwtActiveKid;
        private final String jwtPreviousSecrets;
        private final String issuer;
        private final String allowedOriginPatterns;
        private final String aesKey;
        private final String previousKeys;

        public ProdSecurityBaselineValidator(
                @Value("${app.security.jwt-secret:}") String jwtSecret,
                @Value("${app.security.jwt-active-kid:}") String jwtActiveKid,
                @Value("${app.security.jwt-previous-secrets:}") String jwtPreviousSecrets,
                @Value("${app.security.issuer:}") String issuer,
                @Value("${app.security.allowed-origin-patterns:}") String allowedOriginPatterns,
                @Value("${app.crypto.aes-key:}") String aesKey,
                @Value("${app.crypto.previous-keys:}") String previousKeys) {
            this.jwtSecret = jwtSecret;
            this.jwtActiveKid = jwtActiveKid;
            this.jwtPreviousSecrets = jwtPreviousSecrets;
            this.issuer = issuer;
            this.allowedOriginPatterns = allowedOriginPatterns;
            this.aesKey = aesKey;
            this.previousKeys = previousKeys;
        }

        @PostConstruct
        void validate() {
            require(StringUtils.hasText(jwtSecret), "app.security.jwt-secret 不能为空");
            require(jwtSecret.getBytes(StandardCharsets.UTF_8).length >= 32,
                    "app.security.jwt-secret 必须至少 32 字节");
            require(!DISALLOWED_JWT_SECRETS.contains(jwtSecret), "app.security.jwt-secret 不能使用默认或演示值");
            require(StringUtils.hasText(jwtActiveKid), "app.security.jwt-active-kid 不能为空");
            validatePreviousJwtSecrets(jwtPreviousSecrets);

            require(StringUtils.hasText(issuer), "app.security.issuer 不能为空");
            require(issuer.startsWith("https://"), "app.security.issuer 在 prod 下必须使用 https");
            require(!issuer.contains("localhost") && !issuer.contains("127.0.0.1"),
                    "app.security.issuer 不能指向 localhost/127.0.0.1");

            List<String> originPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
            require(!originPatterns.isEmpty(), "app.security.allowed-origin-patterns 不能为空");
            require(originPatterns.stream().noneMatch(origin -> "*".equals(origin) || origin.contains("://*")),
                    "app.security.allowed-origin-patterns 不能使用通配来源");
            require(originPatterns.stream().noneMatch(
                    origin -> origin.contains("localhost") || origin.contains("127.0.0.1")),
                    "app.security.allowed-origin-patterns 不能放行 localhost/127.0.0.1");

            require(StringUtils.hasText(aesKey), "app.crypto.aes-key 不能为空");
            require(aesKey.length() == 32, "app.crypto.aes-key 必须为 32 个字符");
            require(!DEFAULT_AES_KEY.equals(aesKey), "app.crypto.aes-key 不能使用默认演示值");
            validatePreviousAesKeys(previousKeys);
        }

        private void require(boolean expression, String message) {
            if (!expression) {
                throw new IllegalStateException("生产安全基线校验失败: " + message);
            }
        }

        private void validatePreviousJwtSecrets(String spec) {
            if (!StringUtils.hasText(spec)) {
                return;
            }
            Arrays.stream(spec.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(entry -> {
                        int separator = entry.indexOf(':');
                        require(separator > 0 && separator < entry.length() - 1,
                                "app.security.jwt-previous-secrets 格式必须为 kid:secret");
                        String secret = entry.substring(separator + 1).trim();
                        require(secret.getBytes(StandardCharsets.UTF_8).length >= 32,
                                "app.security.jwt-previous-secrets 中的 secret 必须至少 32 字节");
                    });
        }

        private void validatePreviousAesKeys(String spec) {
            if (!StringUtils.hasText(spec)) {
                return;
            }
            Arrays.stream(spec.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(entry -> {
                        int separator = entry.indexOf(':');
                        require(separator > 0 && separator < entry.length() - 1,
                                "app.crypto.previous-keys 格式必须为 version:key");
                        String key = entry.substring(separator + 1).trim();
                        require(key.length() == 32, "app.crypto.previous-keys 中的 key 必须为 32 个字符");
                    });
        }
    }

    public static class AppPrincipal implements UserDetails {
        private final String username;
        private final String tokenKind;
        private final List<? extends GrantedAuthority> authorities;
        private final String displayName;
        private final String sessionId;

        public AppPrincipal(String username, String tokenKind, List<? extends GrantedAuthority> authorities,
                String displayName, String sessionId) {
            this.username = username;
            this.tokenKind = tokenKind;
            this.authorities = authorities;
            this.displayName = displayName;
            this.sessionId = sessionId;
        }

        public String tokenKind() {
            return tokenKind;
        }

        public String displayName() {
            return displayName;
        }

        public String sessionId() {
            return sessionId;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
