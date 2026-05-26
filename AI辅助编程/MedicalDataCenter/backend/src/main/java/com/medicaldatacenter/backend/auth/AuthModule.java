package com.medicaldatacenter.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.openapi.OpenApiModule;
import com.medicaldatacenter.backend.security.SecuritySupport;
import com.medicaldatacenter.backend.system.SystemModule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public final class AuthModule {

    private AuthModule() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            long refreshExpiresIn,
            String username,
            String displayName,
            List<String> authorities) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record OAuthTokenRequest(@NotBlank String clientId, @NotBlank String clientSecret, String scope) {
    }

    public record OAuthTokenResponse(String accessToken, String tokenType, long expiresIn, List<String> scopes) {
    }

    public record CurrentUserResponse(String username, String displayName, String tokenKind, List<String> authorities) {
    }

    @Service
    public static class PasswordPolicyService {
        private final int minLength;

        public PasswordPolicyService(@Value("${app.security.password-min-length:8}") int minLength) {
            this.minLength = minLength;
        }

        public int minLength() {
            return minLength;
        }

        public void validate(String password) {
            if (!isValid(password)) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST,
                        "password must be at least %d characters and include upper, lower, digit, special character"
                                .formatted(minLength));
            }
        }

        private boolean isValid(String password) {
            if (password == null || password.length() < minLength) {
                return false;
            }
            boolean hasUpper = false;
            boolean hasLower = false;
            boolean hasDigit = false;
            boolean hasSpecial = false;
            for (char character : password.toCharArray()) {
                if (Character.isUpperCase(character)) {
                    hasUpper = true;
                } else if (Character.isLowerCase(character)) {
                    hasLower = true;
                } else if (Character.isDigit(character)) {
                    hasDigit = true;
                } else {
                    hasSpecial = true;
                }
            }
            return hasUpper && hasLower && hasDigit && hasSpecial;
        }
    }

    @Service
    @Validated
    public static class SessionService {
        private final JdbcTemplate jdbcTemplate;
        private final SystemModule.UserAccountService userAccountService;
        private final int maxFailedAttempts;
        private final int lockMinutes;

        public SessionService(
                JdbcTemplate jdbcTemplate,
                SystemModule.UserAccountService userAccountService,
                @Value("${app.security.max-failed-attempts:5}") int maxFailedAttempts,
                @Value("${app.security.lock-minutes:15}") int lockMinutes) {
            this.jdbcTemplate = jdbcTemplate;
            this.userAccountService = userAccountService;
            this.maxFailedAttempts = maxFailedAttempts;
            this.lockMinutes = lockMinutes;
        }

        public void assertLoginAllowed(SystemModule.AuthenticatedUser user) {
            if (!user.enabled()) {
                throw new CommonSupport.BusinessException(HttpStatus.FORBIDDEN, "account disabled");
            }
            if (user.lockedUntil() != null && user.lockedUntil().isAfter(LocalDateTime.now())) {
                throw new CommonSupport.BusinessException(HttpStatus.LOCKED,
                        "account locked until " + user.lockedUntil().toInstant(ZoneOffset.UTC));
            }
        }

        public CommonSupport.BusinessException recordLoginFailure(String username) {
            List<LoginAttemptState> states = jdbcTemplate.query(
                    "select id, failed_login_attempts, locked_until from sys_user where username = ? and deleted = false",
                    (rs, rowNum) -> new LoginAttemptState(
                            rs.getLong("id"),
                            rs.getInt("failed_login_attempts"),
                            rs.getObject("locked_until", LocalDateTime.class)),
                    username);
            if (states.isEmpty()) {
                return null;
            }
            LoginAttemptState state = states.get(0);
            LocalDateTime now = LocalDateTime.now();
            if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
                return new CommonSupport.BusinessException(HttpStatus.LOCKED,
                        "account locked until " + state.lockedUntil().toInstant(ZoneOffset.UTC));
            }
            int nextAttempts = state.failedLoginAttempts() + 1;
            LocalDateTime lockedUntil = nextAttempts >= maxFailedAttempts ? now.plusMinutes(lockMinutes) : null;
            jdbcTemplate.update(
                    "update sys_user set failed_login_attempts = ?, locked_until = ?, updated_at = current_timestamp where id = ?",
                    nextAttempts, lockedUntil, state.userId());
            if (lockedUntil != null) {
                userAccountService.audit(username, "AUTH_ACCOUNT_LOCKED", "sys_user", String.valueOf(state.userId()),
                        "failedAttempts=" + nextAttempts, HttpStatus.LOCKED.value());
                return new CommonSupport.BusinessException(HttpStatus.LOCKED,
                        "account locked due to repeated failures until " + lockedUntil.toInstant(ZoneOffset.UTC));
            }
            userAccountService.audit(username, "AUTH_LOGIN_FAILED", "sys_user", String.valueOf(state.userId()),
                    "failedAttempts=" + nextAttempts, HttpStatus.UNAUTHORIZED.value());
            return null;
        }

        public void recordLoginSuccess(SystemModule.AuthenticatedUser user) {
            jdbcTemplate.update(
                    "update sys_user set failed_login_attempts = 0, locked_until = null, last_login_at = current_timestamp, updated_at = current_timestamp where id = ?",
                    user.id());
        }

        public AuthSessionTokens createLoginSession(SystemModule.AuthenticatedUser user, SecuritySupport.JwtService jwtService) {
            String sessionId = java.util.UUID.randomUUID().toString();
            String refreshToken = jwtService.createPlatformRefreshToken(user, sessionId);
            LocalDateTime refreshExpiresAt = LocalDateTime.ofInstant(Instant.now()
                    .plusSeconds(jwtService.refreshTokenMinutes() * 60), ZoneOffset.UTC);
            jdbcTemplate.update(
                    "insert into auth_session(session_id, user_id, username, refresh_token_hash, refresh_expires_at, revoked, last_refreshed_at) "
                            + "values (?, ?, ?, ?, ?, false, current_timestamp)",
                    sessionId, user.id(), user.username(), hashToken(refreshToken), refreshExpiresAt);
            String accessToken = jwtService.createPlatformAccessToken(user, sessionId);
            return new AuthSessionTokens(sessionId, accessToken, refreshToken, jwtService.platformTokenMinutes() * 60,
                    jwtService.refreshTokenMinutes() * 60);
        }

        public AuthSessionTokens refresh(String refreshToken, SecuritySupport.JwtService jwtService) {
            SecuritySupport.TokenClaims claims = jwtService.parse(refreshToken);
            if (!"REFRESH".equals(claims.tokenKind()) || !StringUtils.hasText(claims.sessionId())) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
            }
            SessionRecord session = loadSession(claims.sessionId());
            if (session.revoked()) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "session revoked");
            }
            if (session.refreshExpiresAt() != null && session.refreshExpiresAt().isBefore(LocalDateTime.now())) {
                revokeSession(session.sessionId(), "REFRESH_EXPIRED");
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "refresh token expired");
            }
            if (!session.username().equals(claims.subject())
                    || !session.refreshTokenHash().equals(hashToken(refreshToken))) {
                revokeSession(session.sessionId(), "REFRESH_TOKEN_REPLAY");
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
            }

            SystemModule.AuthenticatedUser user = userAccountService.loadByUsername(session.username());
            if (!user.enabled()) {
                revokeSession(session.sessionId(), "ACCOUNT_DISABLED");
                throw new CommonSupport.BusinessException(HttpStatus.FORBIDDEN, "account disabled");
            }

            String nextRefreshToken = jwtService.createPlatformRefreshToken(user, session.sessionId());
            LocalDateTime refreshExpiresAt = LocalDateTime.ofInstant(Instant.now()
                    .plusSeconds(jwtService.refreshTokenMinutes() * 60), ZoneOffset.UTC);
            jdbcTemplate.update(
                    "update auth_session set refresh_token_hash = ?, refresh_expires_at = ?, revoked = false, revoked_at = null, revoked_reason = null, last_refreshed_at = current_timestamp where session_id = ?",
                    hashToken(nextRefreshToken), refreshExpiresAt, session.sessionId());
            String accessToken = jwtService.createPlatformAccessToken(user, session.sessionId());
            return new AuthSessionTokens(session.sessionId(), accessToken, nextRefreshToken,
                    jwtService.platformTokenMinutes() * 60, jwtService.refreshTokenMinutes() * 60);
        }

        public void assertSessionAvailable(String sessionId) {
            if (!StringUtils.hasText(sessionId)) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid session");
            }
            Integer count = jdbcTemplate.queryForObject(
                    "select count(1) from auth_session s join sys_user u on u.id = s.user_id "
                            + "where s.session_id = ? and s.revoked = false and u.deleted = false and u.enabled = true",
                    Integer.class,
                    sessionId);
            if (count == null || count == 0) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "session expired");
            }
        }

        public void revokeSession(String sessionId, String reason) {
            if (!StringUtils.hasText(sessionId)) {
                return;
            }
            jdbcTemplate.update(
                    "update auth_session set revoked = true, revoked_at = current_timestamp, revoked_reason = ? where session_id = ? and revoked = false",
                    reason, sessionId);
        }

        private SessionRecord loadSession(String sessionId) {
            List<SessionRecord> sessions = jdbcTemplate.query(
                    "select session_id, user_id, username, refresh_token_hash, refresh_expires_at, revoked from auth_session where session_id = ?",
                    (rs, rowNum) -> new SessionRecord(
                            rs.getString("session_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("refresh_token_hash"),
                            rs.getObject("refresh_expires_at", LocalDateTime.class),
                            rs.getBoolean("revoked")),
                    sessionId);
            if (sessions.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "session expired");
            }
            return sessions.get(0);
        }

        private String hashToken(String token) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("SHA-256 not available", ex);
            }
        }

        private record LoginAttemptState(Long userId, int failedLoginAttempts, LocalDateTime lockedUntil) {
        }

        private record SessionRecord(String sessionId, Long userId, String username, String refreshTokenHash,
                LocalDateTime refreshExpiresAt, boolean revoked) {
        }
    }

    public record AuthSessionTokens(String sessionId, String accessToken, String refreshToken, long expiresIn,
            long refreshExpiresIn) {
    }

    @Service
    @Validated
    public static class AuthService {
        private final SystemModule.UserAccountService userAccountService;
        private final PasswordEncoder passwordEncoder;
        private final SecuritySupport.JwtService jwtService;
        private final OpenApiModule.OpenApiAdminService openApiAdminService;
        private final SessionService sessionService;

        public AuthService(SystemModule.UserAccountService userAccountService, PasswordEncoder passwordEncoder,
                SecuritySupport.JwtService jwtService, OpenApiModule.OpenApiAdminService openApiAdminService,
                SessionService sessionService) {
            this.userAccountService = userAccountService;
            this.passwordEncoder = passwordEncoder;
            this.jwtService = jwtService;
            this.openApiAdminService = openApiAdminService;
            this.sessionService = sessionService;
        }

        public LoginResponse login(LoginRequest request) {
            SystemModule.AuthenticatedUser user = userAccountService.loadByUsername(request.username());
            sessionService.assertLoginAllowed(user);
            if (!passwordEncoder.matches(request.password(), user.password())) {
                CommonSupport.BusinessException failure = sessionService.recordLoginFailure(request.username());
                if (failure != null) {
                    throw failure;
                }
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid credentials");
            }
            sessionService.recordLoginSuccess(user);
            AuthSessionTokens sessionTokens = sessionService.createLoginSession(user, jwtService);
            userAccountService.audit(user.username(), "AUTH_LOGIN", "sys_user", String.valueOf(user.id()),
                    "platform login session=" + sessionTokens.sessionId());
            return new LoginResponse(
                    sessionTokens.accessToken(),
                    sessionTokens.refreshToken(),
                    "Bearer",
                    sessionTokens.expiresIn(),
                    sessionTokens.refreshExpiresIn(),
                    user.username(),
                    user.displayName(),
                    user.authorities());
        }

        public LoginResponse refresh(RefreshTokenRequest request) {
            AuthSessionTokens sessionTokens = sessionService.refresh(request.refreshToken(), jwtService);
            SystemModule.AuthenticatedUser user = userAccountService.loadByUsername(jwtService.parse(request.refreshToken()).subject());
            userAccountService.audit(user.username(), "AUTH_REFRESH", "auth_session", sessionTokens.sessionId(),
                    "session refreshed");
            return new LoginResponse(
                    sessionTokens.accessToken(),
                    sessionTokens.refreshToken(),
                    "Bearer",
                    sessionTokens.expiresIn(),
                    sessionTokens.refreshExpiresIn(),
                    user.username(),
                    user.displayName(),
                    user.authorities());
        }

        public void logout(SecuritySupport.AppPrincipal principal) {
            sessionService.revokeSession(principal.sessionId(), "USER_LOGOUT");
            userAccountService.audit(principal.getUsername(), "AUTH_LOGOUT", "auth_session", principal.sessionId(),
                    "platform logout");
        }

        public OAuthTokenResponse clientCredentials(OAuthTokenRequest request) {
            try {
                OpenApiModule.ApiClientView client = openApiAdminService.validateClient(request.clientId(),
                        request.clientSecret(), request.scope());
                List<String> scopes = client.scopes();
                String token = jwtService.createOpenApiToken(client.clientId(), scopes);
                userAccountService.audit(client.clientId(), "OPEN_API_TOKEN", "api_client", client.clientId(),
                        String.join(",", scopes));
                return new OAuthTokenResponse(token, "Bearer", 3600, scopes);
            } catch (CommonSupport.BusinessException exception) {
                userAccountService.audit(
                        StringUtils.hasText(request.clientId()) ? request.clientId() : "anonymous",
                        "OPEN_API_TOKEN_FAILURE",
                        "api_client",
                        StringUtils.hasText(request.clientId()) ? request.clientId() : "unknown",
                        "grant rejected",
                        exception.getStatus().value());
                throw exception;
            }
        }
    }

    @RestController
    public static class AuthController {
        private final AuthService authService;

        public AuthController(AuthService authService) {
            this.authService = authService;
        }

        @PostMapping("/api/auth/login")
        public CommonSupport.ApiResponse<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
            return CommonSupport.success(authService.login(request));
        }

        @PostMapping("/api/auth/refresh")
        public CommonSupport.ApiResponse<LoginResponse> refresh(@RequestBody @Validated RefreshTokenRequest request) {
            return CommonSupport.success(authService.refresh(request));
        }

        @PostMapping("/api/auth/logout")
        public CommonSupport.ApiResponse<Void> logout(Authentication authentication) {
            SecuritySupport.AppPrincipal principal = (SecuritySupport.AppPrincipal) authentication.getPrincipal();
            authService.logout(principal);
            return CommonSupport.successMessage("logged out");
        }

        @PostMapping(path = "/oauth2/token", consumes = MediaType.APPLICATION_JSON_VALUE)
        public CommonSupport.ApiResponse<OAuthTokenResponse> token(@RequestBody @Validated OAuthTokenRequest request) {
            return CommonSupport.success(authService.clientCredentials(request));
        }

        @PostMapping(path = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        public CommonSupport.ApiResponse<OAuthTokenResponse> tokenForm(
                @RequestHeader(value = "Authorization", required = false) String authorization,
                @RequestParam(value = "client_id", required = false) String clientId,
                @RequestParam(value = "client_secret", required = false) String clientSecret,
                @RequestParam(value = "scope", required = false) String scope,
                @RequestParam(value = "grant_type", required = false) String grantType) {
            if (grantType != null && !"client_credentials".equals(grantType)) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "unsupported grant_type");
            }
            Credentials credentials = resolveCredentials(authorization, clientId, clientSecret);
            return CommonSupport.success(authService.clientCredentials(
                    new OAuthTokenRequest(credentials.clientId(), credentials.clientSecret(), scope)));
        }

        @GetMapping("/api/auth/me")
        @PreAuthorize("hasAuthority('AUTH_ME')")
        public CommonSupport.ApiResponse<CurrentUserResponse> me(Authentication authentication) {
            SecuritySupport.AppPrincipal principal = (SecuritySupport.AppPrincipal) authentication.getPrincipal();
            List<String> authorities = principal.getAuthorities().stream().map(Object::toString).toList();
            return CommonSupport.success(
                    new CurrentUserResponse(principal.getUsername(), principal.displayName(), principal.tokenKind(),
                            authorities));
        }

        private Credentials resolveCredentials(String authorization, String clientId, String clientSecret) {
            if (authorization != null && authorization.startsWith("Basic ")) {
                String decoded = new String(java.util.Base64.getDecoder().decode(authorization.substring(6)),
                        StandardCharsets.UTF_8);
                int separatorIndex = decoded.indexOf(':');
                if (separatorIndex <= 0) {
                    throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "invalid basic authorization");
                }
                return new Credentials(decoded.substring(0, separatorIndex), decoded.substring(separatorIndex + 1));
            }
            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "client credentials required");
            }
            return new Credentials(clientId, clientSecret);
        }

        private record Credentials(@NotEmpty String clientId, @NotEmpty String clientSecret) {
        }
    }
}
