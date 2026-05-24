package com.medicaldatacenter.backend.auth;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.openapi.OpenApiModule;
import com.medicaldatacenter.backend.security.SecuritySupport;
import com.medicaldatacenter.backend.system.SystemModule;

import jakarta.validation.constraints.NotBlank;

public final class AuthModule {

    private AuthModule() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, String username, String displayName,
            List<String> authorities) {
    }

    public record OAuthTokenRequest(@NotBlank String clientId, @NotBlank String clientSecret, String scope) {
    }

    public record OAuthTokenResponse(String accessToken, String tokenType, long expiresIn, List<String> scopes) {
    }

    public record CurrentUserResponse(String username, String displayName, String tokenKind, List<String> authorities) {
    }

    @Service
    @Validated
    public static class AuthService {
        private final SystemModule.UserAccountService userAccountService;
        private final PasswordEncoder passwordEncoder;
        private final SecuritySupport.JwtService jwtService;
        private final OpenApiModule.OpenApiAdminService openApiAdminService;

        public AuthService(SystemModule.UserAccountService userAccountService, PasswordEncoder passwordEncoder,
                SecuritySupport.JwtService jwtService, OpenApiModule.OpenApiAdminService openApiAdminService) {
            this.userAccountService = userAccountService;
            this.passwordEncoder = passwordEncoder;
            this.jwtService = jwtService;
            this.openApiAdminService = openApiAdminService;
        }

        public LoginResponse login(LoginRequest request) {
            SystemModule.AuthenticatedUser user = userAccountService.loadByUsername(request.username());
            if (!user.enabled() || !passwordEncoder.matches(request.password(), user.password())) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid credentials");
            }
            String token = jwtService.createPlatformToken(user);
            userAccountService.audit(user.username(), "AUTH_LOGIN", "sys_user", String.valueOf(user.id()),
                    "platform login");
            return new LoginResponse(token, "Bearer", user.username(), user.displayName(), user.authorities());
        }

        public OAuthTokenResponse clientCredentials(OAuthTokenRequest request) {
            OpenApiModule.ApiClientView client = openApiAdminService.validateClient(request.clientId(),
                    request.clientSecret(), request.scope());
            List<String> scopes = client.scopes();
            String token = jwtService.createOpenApiToken(client.clientId(), scopes);
            userAccountService.audit(client.clientId(), "OPEN_API_TOKEN", "api_client", client.clientId(),
                    String.join(",", scopes));
            return new OAuthTokenResponse(token, "Bearer", 3600, scopes);
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

        @PostMapping("/oauth2/token")
        public CommonSupport.ApiResponse<OAuthTokenResponse> token(@RequestBody @Validated OAuthTokenRequest request) {
            return CommonSupport.success(authService.clientCredentials(request));
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
    }
}
