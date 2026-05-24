package com.medicaldatacenter.backend.system;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;

public final class SystemModule {

    private SystemModule() {
    }

    public record AuthenticatedUser(
            Long id,
            String username,
            String password,
            String displayName,
            boolean enabled,
            List<String> authorities) {
    }

    public record UserView(Long id, String username, String displayName, boolean enabled, LocalDateTime createdAt) {
    }

    public record RoleView(Long id, String code, String name) {
    }

    public record PermissionView(Long id, String code, String name, String module) {
    }

    public record AuditLogView(Long id, String actor, String action, String targetType, String targetId, String detail,
            LocalDateTime createdAt) {
    }

    public record SystemParameterView(String key, String value, String description) {
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

    @Service
    public static class UserAccountService {
        private final JdbcTemplate jdbcTemplate;

        public UserAccountService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public AuthenticatedUser loadByUsername(String username) {
            List<AuthenticatedUser> users = jdbcTemplate.query(
                    "select id, username, password, display_name, enabled from sys_user where username = ? and deleted = false",
                    (rs, rowNum) -> new AuthenticatedUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("display_name"),
                            rs.getBoolean("enabled"),
                            loadAuthorities(rs.getLong("id"))),
                    username);
            if (users.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.UNAUTHORIZED, "invalid credentials");
            }
            return users.get(0);
        }

        public List<UserView> listUsers() {
            return jdbcTemplate.query(
                    "select id, username, display_name, enabled, created_at from sys_user where deleted = false order by id",
                    (rs, rowNum) -> new UserView(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getBoolean("enabled"),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public List<RoleView> listRoles() {
            return jdbcTemplate.query("select id, code, name from sys_role order by id",
                    (rs, rowNum) -> new RoleView(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
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
                    "select id, actor, action, target_type, target_id, detail, created_at from sys_audit_log order by id desc limit 50",
                    (rs, rowNum) -> new AuditLogView(
                            rs.getLong("id"),
                            rs.getString("actor"),
                            rs.getString("action"),
                            rs.getString("target_type"),
                            rs.getString("target_id"),
                            rs.getString("detail"),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public List<SystemParameterView> listParameters() {
            return List.of(
                    new SystemParameterView("app.security.issuer", "medical-data-center", "JWT 签发者"),
                    new SystemParameterView("app.rate-limit.default-capacity", "30", "默认限流桶容量"),
                    new SystemParameterView("app.export.dir", "./data/exports", "报表导出目录"));
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

        public void audit(String actor, String action, String targetType, String targetId, String detail) {
            jdbcTemplate.update(
                    "insert into sys_audit_log(actor, action, target_type, target_id, detail) values (?, ?, ?, ?, ?)",
                    actor, action, targetType, targetId, detail);
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
        @PreAuthorize("hasAuthority('SYSTEM_USER_VIEW')")
        public CommonSupport.ApiResponse<List<UserView>> users() {
            return CommonSupport.success(userAccountService.listUsers());
        }

        @GetMapping("/roles")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW')")
        public CommonSupport.ApiResponse<List<RoleView>> roles() {
            return CommonSupport.success(userAccountService.listRoles());
        }

        @GetMapping("/permissions")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW')")
        public CommonSupport.ApiResponse<List<PermissionView>> permissions() {
            return CommonSupport.success(userAccountService.listPermissions());
        }

        @GetMapping("/audit-logs")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW')")
        public CommonSupport.ApiResponse<List<AuditLogView>> auditLogs() {
            return CommonSupport.success(userAccountService.listAuditLogs());
        }

        @GetMapping("/overview")
        @PreAuthorize("hasAuthority('SYSTEM_ROLE_VIEW') or hasAuthority('SYSTEM_USER_VIEW')")
        public CommonSupport.ApiResponse<SystemOverviewView> overview() {
            return CommonSupport.success(userAccountService.overview());
        }
    }
}
