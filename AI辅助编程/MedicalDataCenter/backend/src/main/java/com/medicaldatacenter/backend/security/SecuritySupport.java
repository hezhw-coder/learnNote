package com.medicaldatacenter.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.medicaldatacenter.backend.system.SystemModule;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class SecuritySupport {

    private SecuritySupport() {
    }

    @Configuration
    @EnableMethodSecurity
    public static class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
                throws Exception {
            return http
                    .cors(cors -> {
                    })
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/api/auth/login", "/oauth2/token", "/swagger-ui.html",
                                    "/swagger-ui/**", "/v3/api-docs/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                            .requestMatchers("/open-api/**").authenticated()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll())
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
            configuration.setAllowedOriginPatterns(List.of(allowedOriginPatterns.split(",")));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setExposedHeaders(List.of("Content-Disposition"));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    @Service
    public static class JwtService {
        private final SecretKey key;
        private final String issuer;
        private final long platformMinutes;
        private final long openApiMinutes;

        public JwtService(@Value("${app.security.jwt-secret}") String secret,
                @Value("${app.security.issuer}") String issuer,
                @Value("${app.security.platform-token-minutes}") long platformMinutes,
                @Value("${app.security.open-api-token-minutes}") long openApiMinutes) {
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalArgumentException("app.security.jwt-secret must be at least 32 bytes for HS256");
            }
            this.key = Keys.hmacShaKeyFor(secretBytes);
            this.issuer = issuer;
            this.platformMinutes = platformMinutes;
            this.openApiMinutes = openApiMinutes;
        }

        public String createPlatformToken(SystemModule.AuthenticatedUser user) {
            Instant now = Instant.now();
            return Jwts.builder()
                    .issuer(issuer)
                    .subject(user.username())
                    .issuedAt(java.util.Date.from(now))
                    .expiration(java.util.Date.from(now.plus(platformMinutes, ChronoUnit.MINUTES)))
                    .claim("tokenKind", "PLATFORM")
                    .claim("displayName", user.displayName())
                    .claim("authorities", user.authorities())
                    .signWith(key)
                    .compact();
        }

        public String createOpenApiToken(String clientId, List<String> scopes) {
            Instant now = Instant.now();
            List<String> authorities = scopes.stream().map(scope -> "SCOPE_" + scope).toList();
            return Jwts.builder()
                    .issuer(issuer)
                    .subject(clientId)
                    .issuedAt(java.util.Date.from(now))
                    .expiration(java.util.Date.from(now.plus(openApiMinutes, ChronoUnit.MINUTES)))
                    .claim("tokenKind", "OPEN_API")
                    .claim("authorities", authorities)
                    .claim("scopes", scopes)
                    .signWith(key)
                    .compact();
        }

        public Authentication toAuthentication(String token) {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            @SuppressWarnings("unchecked")
            List<String> authorities = (List<String>) claims.getOrDefault("authorities", List.of());
            List<GrantedAuthority> grantedAuthorities = authorities.stream()
                    .map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority))
                    .toList();
            String tokenKind = Objects.toString(claims.get("tokenKind"), "PLATFORM");
            AppPrincipal principal = new AppPrincipal(claims.getSubject(), tokenKind, grantedAuthorities,
                    Objects.toString(claims.get("displayName"), claims.getSubject()));
            return new UsernamePasswordAuthenticationToken(principal, token, grantedAuthorities);
        }
    }

    @Component
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;

        public JwtAuthenticationFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, java.io.IOException {
            String header = request.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                try {
                    Authentication authentication = jwtService.toAuthentication(header.substring(7));
                    org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                } catch (Exception ignored) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"code\":401,\"message\":\"invalid token\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }

    public static class AppPrincipal implements UserDetails {
        private final String username;
        private final String tokenKind;
        private final List<? extends GrantedAuthority> authorities;
        private final String displayName;

        public AppPrincipal(String username, String tokenKind, List<? extends GrantedAuthority> authorities,
                String displayName) {
            this.username = username;
            this.tokenKind = tokenKind;
            this.authorities = authorities;
            this.displayName = displayName;
        }

        public String tokenKind() {
            return tokenKind;
        }

        public String displayName() {
            return displayName;
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
