package com.medicaldatacenter.backend.system;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.auth.AuthModule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public final class SystemModule {

    private SystemModule() {
    }

    public record AuthenticatedUser(
            Long id,
            String username,
            String password,
            String displayName,
            boolean enabled,
            int failedLoginAttempts,
            LocalDateTime lockedUntil,
            List<String> authorities) {
    }

    public record UserView(Long id, String username, String displayName, boolean enabled, LocalDateTime createdAt,
            List<String> roles) {
    }

    public record RoleView(Long id, String code, String name, List<String> permissions) {
    }

    public record PermissionView(Long id, String code, String name, String module) {
    }

    public record AuditLogView(Long id, String actor, String action, String targetType, String targetId, String detail,
            String requestId, String remoteIp, String userAgent, Integer statusCode, Long durationMs,
            LocalDateTime createdAt) {
    }

    public record SystemParameterView(String key, String value, String description, String updatedBy,
            LocalDateTime updatedAt) {
    }

    public record DictionaryView(String type, String code, String label, boolean enabled) {
    }

    public record SystemOverviewView(
            List<UserView> users,
            List<RoleView> roles,
            List<PermissionView> permissions,
            List<DictionaryView> dictionaries,
            List<AuditLogView> auditLogs,
            List<SystemParameterView> parameters) {
    }

    public record CreateUserRequest(@NotBlank String username, @NotBlank String password, @NotBlank String displayName,
            @NotEmpty List<Long> roleIds) {
    }

    public record UpdateUserStatusRequest(boolean enabled) {
    }

    public record AssignUserRolesRequest(@NotEmpty List<Long> roleIds) {
    }

    public record CreateRoleRequest(@NotBlank String code, @NotBlank String name, @NotEmpty List<Long> permissionIds) {
    }

    public record UpdateRoleRequest(@NotBlank String code, @NotBlank String name) {
    }

    public record AssignRolePermissionsRequest(@NotEmpty List<Long> permissionIds) {
    }

    public record UpdateSystemParameterRequest(@NotBlank String value) {
    }

    @Service
    @Validated
    public static class UserAccountService {
        private final JdbcTemplate jdbcTemplate;
        private final PasswordEncoder passwordEncoder;
        private final AuthModule.PasswordPolicyService passwordPolicyService;
        private final int passwordMinLength;
        private final int maxFailedAttempts;
        private final int lockMinutes;

        public UserAccountService(
                JdbcTemplate jdbcTemplate,
                PasswordEncoder passwordEncoder,
                AuthModule.PasswordPolicyService passwordPolicyService,
                @Value("${app.security.password-min-length:8}") int passwordMinLength,
                @Value("${app.security.max-failed-attempts:5}") int maxFailedAttempts,
                @Value("${app.security.lock-minutes:15}") int lockMinutes) {
            this.jdbcTemplate = jdbcTemplate;
            this.passwordEncoder = passwordEncoder;
            this.passwordPolicyService = passwordPolicyService;
            this.passwordMinLength = passwordMinLength;
            this.maxFailedAttempts = maxFailedAttempts;
            this.lockMinutes = lockMinutes;
        }

        public AuthenticatedUser loadByUsername(String username) {
            List<AuthenticatedUser> users = jdbcTemplate.query(
                    "select id, username, password, display_name, enabled, failed_login_attempts, locked_until "
                            + "from sys_user where username = ? and deleted = false",
                    (rs, rowNum) -> new AuthenticatedUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("display_name"),
                            rs.getBoolean("enabled"),
                            rs.getInt("failed_login_attempts"),
                            rs.getObject("locked_until", LocalDateTime.class),
                            loadAuthorities(rs.getLong("id"))),
                    username);
            if (users.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid credentials");
            }
            return users.get(0);
        }

        public List<UserView> listUsers() {
            return jdbcTemplate.query("select id, username, display_name, enabled, created_at from sys_user where deleted = false order by id",
                    (rs, rowNum) -> {
                        Long id = rs.getLong("id");
                        return new UserView(
                                id,
                                rs.getString("username"),
                                rs.getString("display_name"),
                                rs.getBoolean("enabled"),
                                rs.getObject("created_at", LocalDateTime.class),
                                loadRoleCodes(id));
                    });
        }

        public List<RoleView> listRoles() {
            return jdbcTemplate.query("select id, code, name from sys_role order by id",
                    (rs, rowNum) -> new RoleView(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            loadPermissionCodes(rs.getLong("id"))));
        }

        public List<PermissionView> listPermissions() {
            return jdbcTemplate.query("select id, code, name from sys_permission order by id",
                    (rs, rowNum) -> new PermissionView(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            resolveModule(rs.getString("code"))));
        }

        public List<AuditLogView> listAuditLogs() {
            return jdbcTemplate.query(
                    "select id, actor, action, target_type, target_id, detail, request_id, remote_ip, user_agent, status_code, duration_ms, created_at "
                            + "from sys_audit_log order by id desc limit 50",
                    (rs, rowNum) -> new AuditLogView(
                            rs.getLong("id"),
                            rs.getString("actor"),
                            rs.getString("action"),
                            rs.getString("target_type"),
                            rs.getString("target_id"),
                            rs.getString("detail"),
                            rs.getString("request_id"),
                            rs.getString("remote_ip"),
                            rs.getString("user_agent"),
                            rs.getObject("status_code", Integer.class),
                            rs.getObject("duration_ms", Long.class),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public List<SystemParameterView> listParameters() {
            return jdbcTemplate.query(
                    "select param_key, param_value, description, updated_by, updated_at from sys_parameter order by param_key",
                    (rs, rowNum) -> new SystemParameterView(
                            rs.getString("param_key"),
                            rs.getString("param_value"),
                            rs.getString("description"),
                            rs.getString("updated_by"),
                            rs.getObject("updated_at", LocalDateTime.class)));
        }

        public List<DictionaryView> listDictionaries() {
            return List.of(
                    new DictionaryView("DATA_SOURCE_TYPE", "MYSQL", "MySQL", true),
                    new DictionaryView("DATA_SOURCE_TYPE", "POSTGRESQL", "PostgreSQL", true),
                    new DictionaryView("DATA_SOURCE_TYPE", "MONGODB", "MongoDB", true),
                    new DictionaryView("REPORT_CHANNEL", "IN_APP", "站内消息", true),
                    new DictionaryView("REPORT_CHANNEL", "EMAIL", "邮件接口预留", true));
        }

        public SystemOverviewView overview() {
            return new SystemOverviewView(
                    listUsers(),
                    listRoles(),
                    listPermissions(),
                    listDictionaries(),
                    listAuditLogs(),
                    listParameters());
        }

        public UserView createUser(CreateUserRequest request) {
            assertUserMissing(request.username());
            passwordPolicyService.validate(request.password());
            List<Long> roleIds = validateRoleIds(request.roleIds());
            jdbcTemplate.update(
                    "insert into sys_user(username, password, display_name, enabled, deleted, password_changed_at) "
                            + "values (?, ?, ?, ?, ?, current_timestamp)",
                    request.username(), passwordEncoder.encode(request.password()), request.displayName(), true, false);
            Long userId = jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class,
                    request.username());
            replaceUserRoles(userId, roleIds);
            audit(currentActor(), "SYSTEM_USER_CREATE", "sys_user", String.valueOf(userId),
                    request.username() + " roles=" + roleIds);
            return getUser(userId);
        }

        public UserView updateUserStatus(Long id, boolean enabled) {
            ensureUserExists(id);
            jdbcTemplate.update("update sys_user set enabled = ?, updated_at = current_timestamp where id = ?", enabled,
                    id);
            audit(currentActor(), "SYSTEM_USER_STATUS", "sys_user", String.valueOf(id), "enabled=" + enabled);
            return getUser(id);
        }

        public UserView assignUserRoles(Long id, List<Long> roleIds) {
            ensureUserExists(id);
            List<Long> validatedRoleIds = validateRoleIds(roleIds);
            replaceUserRoles(id, validatedRoleIds);
            audit(currentActor(), "SYSTEM_USER_ROLE_ASSIGN", "sys_user", String.valueOf(id), "roles=" + validatedRoleIds);
            return getUser(id);
        }

        public RoleView createRole(CreateRoleRequest request) {
            String normalizedCode = normalizeRoleCode(request.code());
            assertRoleCodeMissing(normalizedCode, null);
            List<Long> permissionIds = validatePermissionIds(request.permissionIds());
            jdbcTemplate.update("insert into sys_role(code, name) values (?, ?)", normalizedCode, request.name().trim());
            Long roleId = jdbcTemplate.queryForObject("select id from sys_role where code = ?", Long.class, normalizedCode);
            replaceRolePermissions(roleId, permissionIds);
            audit(currentActor(), "SYSTEM_ROLE_CREATE", "sys_role", String.valueOf(roleId),
                    normalizedCode + " permissions=" + permissionIds);
            return getRole(roleId);
        }

        public RoleView updateRole(Long id, UpdateRoleRequest request) {
            RoleView existing = getRole(id);
            assertRoleMutable(existing);
            String normalizedCode = normalizeRoleCode(request.code());
            assertRoleCodeMissing(normalizedCode, id);
            String trimmedName = request.name().trim();
            jdbcTemplate.update("update sys_role set code = ?, name = ? where id = ?", normalizedCode, trimmedName, id);
            audit(currentActor(), "SYSTEM_ROLE_UPDATE", "sys_role", String.valueOf(id),
                    "code=" + normalizedCode + ", name=" + trimmedName);
            return getRole(id);
        }

        public RoleView assignRolePermissions(Long id, List<Long> permissionIds) {
            RoleView existing = getRole(id);
            assertRoleMutable(existing);
            List<Long> validatedPermissionIds = validatePermissionIds(permissionIds);
            replaceRolePermissions(id, validatedPermissionIds);
            audit(currentActor(), "SYSTEM_ROLE_PERMISSION_ASSIGN", "sys_role", String.valueOf(id),
                    "permissions=" + validatedPermissionIds);
            return getRole(id);
        }

        public SystemParameterView updateParameter(String key, String value) {
            SystemParameterView parameter = getParameter(key);
            assertParameterKeyAllowed(key);
            String normalizedValue = value.trim();
            jdbcTemplate.update(
                    "update sys_parameter set param_value = ?, updated_by = ?, updated_at = current_timestamp where param_key = ?",
                    normalizedValue, currentActor(), key);
            audit(currentActor(), "SYSTEM_PARAM_UPDATE", "sys_parameter", key,
                    parameter.value() + " -> " + normalizedValue);
            return getParameter(key);
        }

        public void audit(String actor, String action, String targetType, String targetId, String detail) {
            audit(actor, action, targetType, targetId, detail, 200);
        }

        public void audit(String actor, String action, String targetType, String targetId, String detail, int statusCode) {
            jdbcTemplate.update(
                    "insert into sys_audit_log(actor, action, target_type, target_id, detail, request_id, remote_ip, user_agent, status_code, duration_ms) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    actor, action, targetType, targetId, detail, CommonSupport.currentRequestId(),
                    CommonSupport.currentRemoteIp(), CommonSupport.currentUserAgent(), statusCode,
                    CommonSupport.currentDurationMs());
        }

        public String currentActor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication == null ? "anonymous" : authentication.getName();
        }

        private List<String> loadAuthorities(Long userId) {
            Set<String> authorities = new LinkedHashSet<>();
            authorities.addAll(jdbcTemplate.query(
                    "select concat('ROLE_', r.code) authority from sys_role r join sys_user_role ur on ur.role_id = r.id where ur.user_id = ?",
                    (rs, rowNum) -> rs.getString("authority"),
                    userId));
            authorities.addAll(jdbcTemplate.query(
                    "select p.code authority from sys_permission p join sys_role_permission rp on rp.permission_id = p.id "
                            + "join sys_user_role ur on ur.role_id = rp.role_id where ur.user_id = ?",
                    (rs, rowNum) -> rs.getString("authority"),
                    userId));
            return List.copyOf(authorities);
        }

        private UserView getUser(Long id) {
            List<UserView> users = jdbcTemplate.query(
                    "select id, username, display_name, enabled, created_at from sys_user where id = ? and deleted = false",
                    (rs, rowNum) -> new UserView(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getBoolean("enabled"),
                            rs.getObject("created_at", LocalDateTime.class),
                            loadRoleCodes(rs.getLong("id"))),
                    id);
            if (users.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "user not found");
            }
            return users.get(0);
        }

        private RoleView getRole(Long id) {
            List<RoleView> roles = jdbcTemplate.query(
                    "select id, code, name from sys_role where id = ?",
                    (rs, rowNum) -> new RoleView(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            loadPermissionCodes(rs.getLong("id"))),
                    id);
            if (roles.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "role not found");
            }
            return roles.get(0);
        }

        private SystemParameterView getParameter(String key) {
            List<SystemParameterView> parameters = jdbcTemplate.query(
                    "select param_key, param_value, description, updated_by, updated_at from sys_parameter where param_key = ?",
                    (rs, rowNum) -> new SystemParameterView(
                            rs.getString("param_key"),
                            rs.getString("param_value"),
                            rs.getString("description"),
                            rs.getString("updated_by"),
                            rs.getObject("updated_at", LocalDateTime.class)),
                    key);
            if (parameters.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "system parameter not found");
            }
            return parameters.get(0);
        }

        private void assertUserMissing(String username) {
            Integer count = jdbcTemplate.queryForObject("select count(1) from sys_user where username = ? and deleted = false",
                    Integer.class, username);
            if (count != null && count > 0) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "username already exists");
            }
        }

        private void assertParameterKeyAllowed(String key) {
            String normalizedKey = key == null ? "" : key.toLowerCase().replaceAll("[^a-z0-9]", "");
            List<String> blockedMarkers = List.of(
                    "secret",
                    "credential",
                    "privatekey",
                    "aeskey",
                    "jwtsecret",
                    "redispassword",
                    "dbpassword",
                    "clientsecret",
                    "refreshtoken",
                    "accesstoken");
            boolean blocked = blockedMarkers.stream().anyMatch(normalizedKey::contains);
            if (blocked) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "sensitive parameter must be managed outside sys_parameter");
            }
        }

        private void ensureUserExists(Long id) {
            Integer count = jdbcTemplate.queryForObject("select count(1) from sys_user where id = ? and deleted = false",
                    Integer.class, id);
            if (count == null || count == 0) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "user not found");
            }
        }

        private void assertRoleCodeMissing(String code, Long excludeId) {
            String sql = excludeId == null
                    ? "select count(1) from sys_role where code = ?"
                    : "select count(1) from sys_role where code = ? and id <> ?";
            Object[] args = excludeId == null ? new Object[] { code } : new Object[] { code, excludeId };
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
            if (count != null && count > 0) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "role code already exists");
            }
        }

        private List<Long> validateRoleIds(List<Long> roleIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(1) from sys_role where id in (%s)".formatted(placeholders(roleIds.size())),
                    Integer.class,
                    roleIds.toArray());
            if (count == null || count != roleIds.size()) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "role not found");
            }
            return roleIds;
        }

        private List<Long> validatePermissionIds(List<Long> permissionIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(1) from sys_permission where id in (%s)".formatted(placeholders(permissionIds.size())),
                    Integer.class,
                    permissionIds.toArray());
            if (count == null || count != permissionIds.size()) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "permission not found");
            }
            return permissionIds;
        }

        private void replaceUserRoles(Long userId, List<Long> roleIds) {
            jdbcTemplate.update("delete from sys_user_role where user_id = ?", userId);
            for (Long roleId : roleIds) {
                jdbcTemplate.update("insert into sys_user_role(user_id, role_id) values (?, ?)", userId, roleId);
            }
        }

        private List<String> loadRoleCodes(Long userId) {
            return jdbcTemplate.query(
                    "select r.code from sys_role r join sys_user_role ur on ur.role_id = r.id where ur.user_id = ? order by r.id",
                    (rs, rowNum) -> rs.getString("code"),
                    userId);
        }

        private void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
            jdbcTemplate.update("delete from sys_role_permission where role_id = ?", roleId);
            for (Long permissionId : permissionIds) {
                jdbcTemplate.update("insert into sys_role_permission(role_id, permission_id) values (?, ?)", roleId,
                        permissionId);
            }
        }

        private List<String> loadPermissionCodes(Long roleId) {
            return jdbcTemplate.query(
                    "select p.code from sys_permission p join sys_role_permission rp on rp.permission_id = p.id where rp.role_id = ? order by p.id",
                    (rs, rowNum) -> rs.getString("code"),
                    roleId);
        }

        private void assertRoleMutable(RoleView role) {
            if ("ADMIN".equals(role.code())) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "built-in ADMIN role cannot be modified");
            }
        }

        private String normalizeRoleCode(String code) {
            String normalized = code.trim().toUpperCase();
            if (!normalized.matches("[A-Z0-9_]+")) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "role code must match [A-Z0-9_]+");
            }
            return normalized;
        }

        private String placeholders(int size) {
            return java.util.stream.IntStream.range(0, size)
                    .mapToObj(index -> "?")
                    .reduce((left, right) -> left + "," + right)
                    .orElse("?");
        }

        private String resolveModule(String code) {
            if (code.startsWith("AUTH")) {
                return "认证";
            }
            if (code.startsWith("SYSTEM")) {
                return "系统";
            }
            if (code.startsWith("DATASOURCE")) {
                return "数据源";
            }
            if (code.startsWith("ETL")) {
                return "ETL";
            }
            if (code.startsWith("DATASET")) {
                return "数据集";
            }
            if (code.startsWith("REPORT")) {
                return "报表";
            }
            if (code.startsWith("OPEN_API")) {
                return "开放API";
            }
            return "平台";
        }
    }

    @RestController
    @RequestMapping("/api/system")
    public static class SystemController {
        private final UserAccountService userAccountService;

        public SystemController(UserAccountService userAccountService) {
            this.userAccountService = userAccountService;
        }

        @GetMapping("/users")
        @PreAuthorize("hasAuthority('SYSTEM_USER_VIEW') or hasAuthority('SYSTEM_USER_MANAGE')")
        public CommonSupport.ApiResponse<List<UserView>> users() {
            return CommonSupport.success(userAccountService.listUsers());
        }

        @GetMapping("/roles")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<List<RoleView>> roles() {
            return CommonSupport.success(userAccountService.listRoles());
        }

        @GetMapping("/permissions")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<List<PermissionView>> permissions() {
            return CommonSupport.success(userAccountService.listPermissions());
        }

        @GetMapping("/audit-logs")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<List<AuditLogView>> auditLogs() {
            return CommonSupport.success(userAccountService.listAuditLogs());
        }

        @GetMapping("/dictionaries")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<List<DictionaryView>> dictionaries() {
            return CommonSupport.success(userAccountService.listDictionaries());
        }

        @GetMapping("/parameters")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_PARAM_MANAGE')")
        public CommonSupport.ApiResponse<List<SystemParameterView>> parameters() {
            return CommonSupport.success(userAccountService.listParameters());
        }

        @GetMapping("/overview")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<SystemOverviewView> overview() {
            return CommonSupport.success(userAccountService.overview());
        }

        @PostMapping("/users")
        @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
        public CommonSupport.ApiResponse<UserView> createUser(@RequestBody @Validated CreateUserRequest request) {
            return CommonSupport.success(userAccountService.createUser(request));
        }

        @PutMapping("/users/{id}/status")
        @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
        public CommonSupport.ApiResponse<UserView> updateStatus(@PathVariable Long id,
                @RequestBody UpdateUserStatusRequest request) {
            return CommonSupport.success(userAccountService.updateUserStatus(id, request.enabled()));
        }

        @PutMapping("/users/{id}/roles")
        @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
        public CommonSupport.ApiResponse<UserView> assignRoles(@PathVariable Long id,
                @RequestBody @Validated AssignUserRolesRequest request) {
            return CommonSupport.success(userAccountService.assignUserRoles(id, request.roleIds()));
        }

        @PostMapping("/roles")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<RoleView> createRole(@RequestBody @Validated CreateRoleRequest request) {
            return CommonSupport.success(userAccountService.createRole(request));
        }

        @PutMapping("/roles/{id}")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<RoleView> updateRole(@PathVariable Long id,
                @RequestBody @Validated UpdateRoleRequest request) {
            return CommonSupport.success(userAccountService.updateRole(id, request));
        }

        @PutMapping("/roles/{id}/permissions")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
        public CommonSupport.ApiResponse<RoleView> assignRolePermissions(@PathVariable Long id,
                @RequestBody @Validated AssignRolePermissionsRequest request) {
            return CommonSupport.success(userAccountService.assignRolePermissions(id, request.permissionIds()));
        }

        @PutMapping("/parameters/{key}")
        @PreAuthorize("hasAuthority('SYSTEM_PARAM_MANAGE')")
        public CommonSupport.ApiResponse<SystemParameterView> updateParameter(@PathVariable String key,
                @RequestBody @Validated UpdateSystemParameterRequest request) {
            return CommonSupport.success(userAccountService.updateParameter(key, request.value()));
        }
    }
}
