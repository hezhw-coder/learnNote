package com.medicaldatacenter.backend.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.infrastructure.CryptoSupport;
import com.medicaldatacenter.backend.system.SystemModule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class DataSourceModule {

    private DataSourceModule() {
    }

    public enum SourceType {
        MYSQL, POSTGRESQL, SQLSERVER, ORACLE, MONGODB, TIDB, H2
    }

    public record DataSourceRequest(
            @NotBlank String name,
            @NotBlank String type,
            String jdbcUrl,
            String username,
            String password,
            String databaseName) {
    }

    public record DataSourceView(Long id, String name, String type, String jdbcUrl, String username,
            String databaseName, String status) {
    }

    public record TestConnectionRequest(
            @NotBlank String type,
            String jdbcUrl,
            String username,
            String password) {
    }

    public record StatusRequest(@NotBlank String status) {
    }

    public record ConnectionTestResult(boolean success, String message) {
    }

    @Service
    @Validated
    public static class DataSourceService {
        private final JdbcTemplate jdbcTemplate;
        private final CryptoSupport.TextEncryptor textEncryptor;
        private final SystemModule.UserAccountService userAccountService;

        public DataSourceService(JdbcTemplate jdbcTemplate, CryptoSupport.TextEncryptor textEncryptor,
                SystemModule.UserAccountService userAccountService) {
            this.jdbcTemplate = jdbcTemplate;
            this.textEncryptor = textEncryptor;
            this.userAccountService = userAccountService;
        }

        public List<DataSourceView> list() {
            return jdbcTemplate.query(
                    "select id, name, type, jdbc_url, username, database_name, status from ds_source where deleted = false order by id",
                    (rs, rowNum) -> new DataSourceView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getString("jdbc_url"),
                            rs.getString("username"),
                            rs.getString("database_name"),
                            rs.getString("status")));
        }

        public DataSourceView create(DataSourceRequest request) {
            SourceType.valueOf(request.type().toUpperCase(Locale.ROOT));
            jdbcTemplate.update(
                    "insert into ds_source(name, type, jdbc_url, username, database_name, status) values (?, ?, ?, ?, ?, ?)",
                    request.name(), request.type().toUpperCase(Locale.ROOT), request.jdbcUrl(), request.username(),
                    request.databaseName(), "ENABLED");
            Long id = jdbcTemplate.queryForObject("select max(id) from ds_source", Long.class);
            jdbcTemplate.update("merge into ds_source_secret key(source_id) values (?, ?)", id,
                    textEncryptor.encrypt(request.password()));
            userAccountService.audit(userAccountService.currentActor(), "DS_CREATE", "ds_source", String.valueOf(id),
                    request.name());
            return getById(id);
        }

        public DataSourceView update(Long id, DataSourceRequest request) {
            getById(id);
            SourceType.valueOf(request.type().toUpperCase(Locale.ROOT));
            jdbcTemplate.update(
                    "update ds_source set name = ?, type = ?, jdbc_url = ?, username = ?, database_name = ?, updated_at = current_timestamp where id = ?",
                    request.name(), request.type().toUpperCase(Locale.ROOT), request.jdbcUrl(), request.username(),
                    request.databaseName(), id);
            if (request.password() != null && !request.password().isBlank()) {
                jdbcTemplate.update("merge into ds_source_secret key(source_id) values (?, ?)", id,
                        textEncryptor.encrypt(request.password()));
            }
            userAccountService.audit(userAccountService.currentActor(), "DS_UPDATE", "ds_source", String.valueOf(id),
                    request.name());
            return getById(id);
        }

        public DataSourceView getById(@NotNull Long id) {
            List<DataSourceView> items = jdbcTemplate.query(
                    "select id, name, type, jdbc_url, username, database_name, status from ds_source where id = ? and deleted = false",
                    (rs, rowNum) -> new DataSourceView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getString("jdbc_url"),
                            rs.getString("username"),
                            rs.getString("database_name"),
                            rs.getString("status")),
                    id);
            if (items.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "data source not found");
            }
            return items.get(0);
        }

        public String getPassword(Long id) {
            String cipher = jdbcTemplate.queryForObject("select encrypted_password from ds_source_secret where source_id = ?",
                    String.class, id);
            return textEncryptor.decrypt(cipher);
        }

        public DataSourceView updateStatus(Long id, String status) {
            jdbcTemplate.update("update ds_source set status = ?, updated_at = current_timestamp where id = ?",
                    status.toUpperCase(Locale.ROOT), id);
            userAccountService.audit(userAccountService.currentActor(), "DS_STATUS", "ds_source", String.valueOf(id),
                    status);
            return getById(id);
        }

        public void delete(Long id) {
            getById(id);
            jdbcTemplate.update("update ds_source set deleted = true, updated_at = current_timestamp where id = ?", id);
            jdbcTemplate.update("delete from ds_source_secret where source_id = ?", id);
            userAccountService.audit(userAccountService.currentActor(), "DS_DELETE", "ds_source", String.valueOf(id),
                    "soft delete");
        }

        public ConnectionTestResult test(TestConnectionRequest request) {
            SourceType type = SourceType.valueOf(request.type().toUpperCase(Locale.ROOT));
            if (type == SourceType.MONGODB) {
                return new ConnectionTestResult(false, "mongodb adapter is reserved in mvp skeleton");
            }
            try (Connection ignored = DriverManager.getConnection(request.jdbcUrl(), request.username(),
                    request.password())) {
                return new ConnectionTestResult(true, "connection success");
            } catch (Exception exception) {
                return new ConnectionTestResult(false, exception.getMessage());
            }
        }
    }

    @RestController
    @RequestMapping("/api/data-sources")
    public static class DataSourceController {
        private final DataSourceService dataSourceService;

        public DataSourceController(DataSourceService dataSourceService) {
            this.dataSourceService = dataSourceService;
        }

        @GetMapping
        @PreAuthorize("hasAuthority('DATASOURCE_MANAGE')")
        public CommonSupport.ApiResponse<List<DataSourceView>> list() {
            return CommonSupport.success(dataSourceService.list());
        }

        @PostMapping
        @PreAuthorize("hasAuthority('DATASOURCE_MANAGE')")
        public CommonSupport.ApiResponse<DataSourceView> create(@RequestBody @Validated DataSourceRequest request) {
            return CommonSupport.success(dataSourceService.create(request));
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('DATASOURCE_MANAGE')")
        public CommonSupport.ApiResponse<DataSourceView> update(@PathVariable Long id,
                @RequestBody @Validated DataSourceRequest request) {
            return CommonSupport.success(dataSourceService.update(id, request));
        }

        @PostMapping("/test")
        @PreAuthorize("hasAuthority('DATASOURCE_MANAGE')")
        public CommonSupport.ApiResponse<ConnectionTestResult> test(@RequestBody @Validated TestConnectionRequest request) {
            return CommonSupport.success(dataSourceService.test(request));
        }

        @PatchMapping("/{id}/status")
        @PreAuthorize("hasAuthority('DATASOURCE_MANAGE')")
        public CommonSupport.ApiResponse<DataSourceView> status(@PathVariable Long id,
                @RequestBody @Validated StatusRequest request) {
            return CommonSupport.success(dataSourceService.updateStatus(id, request.status()));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('DATASOURCE_MANAGE')")
        public CommonSupport.ApiResponse<Void> delete(@PathVariable Long id) {
            dataSourceService.delete(id);
            return CommonSupport.success(null);
        }
    }
}
