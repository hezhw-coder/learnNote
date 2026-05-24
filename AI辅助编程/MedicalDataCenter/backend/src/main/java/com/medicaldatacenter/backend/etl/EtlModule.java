package com.medicaldatacenter.backend.etl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.datasource.DataSourceModule;
import com.medicaldatacenter.backend.system.SystemModule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class EtlModule {

    private EtlModule() {
    }

    public record EtlJobRequest(
            @NotBlank String name,
            @NotNull Long dataSourceId,
            @NotBlank String loadMode,
            String sourceTable,
            String extractSql,
            String incrementField,
            @NotBlank String idField,
            @NotBlank String nameField,
            String genderField,
            String birthDateField) {
    }

    public record EtlJobView(Long id, String name, Long dataSourceId, String loadMode, String sourceTable,
            String extractSql, String incrementField, String idField, String nameField, String genderField,
            String birthDateField, String status) {
    }

    public record EtlRunView(Long id, Long jobId, String status, int extractedCount, String message,
            LocalDateTime startedAt, LocalDateTime finishedAt) {
    }

    @Service
    @Validated
    public static class EtlService {
        private final JdbcTemplate jdbcTemplate;
        private final DataSourceModule.DataSourceService dataSourceService;
        private final ObjectMapper objectMapper;
        private final SystemModule.UserAccountService userAccountService;

        public EtlService(JdbcTemplate jdbcTemplate, DataSourceModule.DataSourceService dataSourceService,
                ObjectMapper objectMapper, SystemModule.UserAccountService userAccountService) {
            this.jdbcTemplate = jdbcTemplate;
            this.dataSourceService = dataSourceService;
            this.objectMapper = objectMapper;
            this.userAccountService = userAccountService;
        }

        public List<EtlJobView> listJobs() {
            return jdbcTemplate.query(
                    "select id, name, data_source_id, load_mode, source_table, extract_sql, increment_field, id_field, name_field, gender_field, birth_date_field, status from etl_job order by id",
                    (rs, rowNum) -> new EtlJobView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("data_source_id"),
                            rs.getString("load_mode"),
                            rs.getString("source_table"),
                            rs.getString("extract_sql"),
                            rs.getString("increment_field"),
                            rs.getString("id_field"),
                            rs.getString("name_field"),
                            rs.getString("gender_field"),
                            rs.getString("birth_date_field"),
                            rs.getString("status")));
        }

        public EtlJobView createJob(EtlJobRequest request) {
            dataSourceService.getById(request.dataSourceId());
            jdbcTemplate.update(
                    "insert into etl_job(name, data_source_id, load_mode, source_table, extract_sql, increment_field, id_field, name_field, gender_field, birth_date_field, status) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    request.name(), request.dataSourceId(), request.loadMode(), request.sourceTable(), request.extractSql(),
                    request.incrementField(), request.idField(), request.nameField(), request.genderField(),
                    request.birthDateField(), "READY");
            Long id = jdbcTemplate.queryForObject("select max(id) from etl_job", Long.class);
            userAccountService.audit(userAccountService.currentActor(), "ETL_JOB_CREATE", "etl_job", String.valueOf(id),
                    request.name());
            return getJob(id);
        }

        public EtlJobView getJob(Long id) {
            List<EtlJobView> items = jdbcTemplate.query(
                    "select id, name, data_source_id, load_mode, source_table, extract_sql, increment_field, id_field, name_field, gender_field, birth_date_field, status from etl_job where id = ?",
                    (rs, rowNum) -> new EtlJobView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("data_source_id"),
                            rs.getString("load_mode"),
                            rs.getString("source_table"),
                            rs.getString("extract_sql"),
                            rs.getString("increment_field"),
                            rs.getString("id_field"),
                            rs.getString("name_field"),
                            rs.getString("gender_field"),
                            rs.getString("birth_date_field"),
                            rs.getString("status")),
                    id);
            if (items.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "etl job not found");
            }
            return items.get(0);
        }

        public EtlRunView getRun(Long runId) {
            List<EtlRunView> items = jdbcTemplate.query(
                    "select id, job_id, status, extracted_count, message, started_at, finished_at from etl_job_run where id = ?",
                    (rs, rowNum) -> new EtlRunView(
                            rs.getLong("id"),
                            rs.getLong("job_id"),
                            rs.getString("status"),
                            rs.getInt("extracted_count"),
                            rs.getString("message"),
                            rs.getObject("started_at", LocalDateTime.class),
                            rs.getObject("finished_at", LocalDateTime.class)),
                    runId);
            if (items.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "etl run not found");
            }
            return items.get(0);
        }

        public List<EtlRunView> listRuns() {
            return jdbcTemplate.query(
                    "select id, job_id, status, extracted_count, message, started_at, finished_at from etl_job_run order by id desc",
                    (rs, rowNum) -> new EtlRunView(
                            rs.getLong("id"),
                            rs.getLong("job_id"),
                            rs.getString("status"),
                            rs.getInt("extracted_count"),
                            rs.getString("message"),
                            rs.getObject("started_at", LocalDateTime.class),
                            rs.getObject("finished_at", LocalDateTime.class)));
        }

        public EtlRunView runJob(Long jobId) {
            EtlJobView job = getJob(jobId);
            Long runId = beginRun(jobId);
            try {
                List<Map<String, Object>> rows = extract(job);
                for (Map<String, Object> row : rows) {
                    jdbcTemplate.update(
                            "insert into ods_record(job_id, batch_run_id, source_table, record_json) values (?, ?, ?, ?)",
                            job.id(), runId, job.sourceTable(), objectMapper.writeValueAsString(row));
                    upsertPatient(job, row);
                }
                updateCheckpoint(job, rows);
                jdbcTemplate.update(
                        "update etl_job_run set status = ?, extracted_count = ?, message = ?, finished_at = current_timestamp where id = ?",
                        "SUCCESS", rows.size(), "etl completed", runId);
                jdbcTemplate.update("update etl_job set status = ?, updated_at = current_timestamp where id = ?",
                        "SUCCESS", jobId);
                userAccountService.audit(userAccountService.currentActor(), "ETL_RUN", "etl_job", String.valueOf(jobId),
                        "rows=" + rows.size());
                return getRun(runId);
            } catch (Exception exception) {
                jdbcTemplate.update(
                        "update etl_job_run set status = ?, message = ?, finished_at = current_timestamp where id = ?",
                        "FAILED", exception.getMessage(), runId);
                jdbcTemplate.update("update etl_job set status = ?, updated_at = current_timestamp where id = ?",
                        "FAILED", jobId);
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, exception.getMessage());
            }
        }

        private Long beginRun(Long jobId) {
            jdbcTemplate.update("insert into etl_job_run(job_id, status, extracted_count, message) values (?, ?, ?, ?)",
                    jobId, "RUNNING", 0, "started");
            return jdbcTemplate.queryForObject("select max(id) from etl_job_run", Long.class);
        }

        private List<Map<String, Object>> extract(EtlJobView job) throws Exception {
            DataSourceModule.DataSourceView source = dataSourceService.getById(job.dataSourceId());
            String sql = buildSql(job);
            try (Connection connection = DriverManager.getConnection(source.jdbcUrl(), source.username(),
                    dataSourceService.getPassword(source.id()));
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(sql)) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= metaData.getColumnCount(); index++) {
                        row.put(metaData.getColumnLabel(index), resultSet.getObject(index));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }

        private String buildSql(EtlJobView job) {
            String baseSql = (job.extractSql() != null && !job.extractSql().isBlank())
                    ? job.extractSql()
                    : "select * from " + job.sourceTable();
            if (!"INCREMENTAL".equalsIgnoreCase(job.loadMode()) || job.incrementField() == null
                    || job.incrementField().isBlank()) {
                return baseSql;
            }
            String checkpoint = jdbcTemplate.query(
                    "select checkpoint_value from etl_increment_checkpoint where job_id = ?",
                    rs -> rs.next() ? rs.getString(1) : null, job.id());
            if (checkpoint == null || checkpoint.isBlank()) {
                return baseSql;
            }
            String lower = baseSql.toLowerCase();
            String append = job.incrementField() + " > '" + checkpoint + "'";
            return lower.contains(" where ") ? baseSql + " and " + append : baseSql + " where " + append;
        }

        private void upsertPatient(EtlJobView job, Map<String, Object> row) {
            String patientCode = Objects.toString(row.get(job.idField()), "");
            String patientName = Objects.toString(row.get(job.nameField()), "");
            String gender = job.genderField() == null ? null : Objects.toString(row.get(job.genderField()), null);
            String birthDate = job.birthDateField() == null ? null
                    : Objects.toString(row.get(job.birthDateField()), null);
            jdbcTemplate.update(
                    "merge into cdm_patient key(source_job_id, patient_code) values (?, ?, ?, ?, ?)",
                    job.id(), patientCode, patientName, gender, birthDate);
        }

        private void updateCheckpoint(EtlJobView job, List<Map<String, Object>> rows) {
            if (!"INCREMENTAL".equalsIgnoreCase(job.loadMode()) || job.incrementField() == null
                    || job.incrementField().isBlank() || rows.isEmpty()) {
                return;
            }
            String value = rows.stream()
                    .map(row -> Objects.toString(row.get(job.incrementField()), ""))
                    .filter(item -> !item.isBlank())
                    .max(String::compareTo)
                    .orElse("");
            jdbcTemplate.update("merge into etl_increment_checkpoint key(job_id) values (?, ?, current_timestamp)",
                    job.id(), value);
        }
    }

    @RestController
    @RequestMapping("/api/etl")
    public static class EtlController {
        private final EtlService etlService;

        public EtlController(EtlService etlService) {
            this.etlService = etlService;
        }

        @GetMapping("/jobs")
        @PreAuthorize("hasAuthority('ETL_MANAGE')")
        public CommonSupport.ApiResponse<List<EtlJobView>> listJobs() {
            return CommonSupport.success(etlService.listJobs());
        }

        @PostMapping("/jobs")
        @PreAuthorize("hasAuthority('ETL_MANAGE')")
        public CommonSupport.ApiResponse<EtlJobView> createJob(@RequestBody @Validated EtlJobRequest request) {
            return CommonSupport.success(etlService.createJob(request));
        }

        @PostMapping("/jobs/{id}/run")
        @PreAuthorize("hasAuthority('ETL_MANAGE')")
        public CommonSupport.ApiResponse<EtlRunView> run(@PathVariable Long id) {
            return CommonSupport.success(etlService.runJob(id));
        }

        @GetMapping("/runs")
        @PreAuthorize("hasAuthority('ETL_MANAGE')")
        public CommonSupport.ApiResponse<List<EtlRunView>> runs() {
            return CommonSupport.success(etlService.listRuns());
        }

        @GetMapping("/runs/{runId}")
        @PreAuthorize("hasAuthority('ETL_MANAGE')")
        public CommonSupport.ApiResponse<EtlRunView> runDetail(@PathVariable Long runId) {
            return CommonSupport.success(etlService.getRun(runId));
        }
    }
}
