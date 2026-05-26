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

    private static final Map<String, DatasetSpec> DATASET_SPECS = Map.of(
            "cdm_patient", new DatasetSpec("患者主题", List.of("idField", "nameField"), Map.of(
                    "idField", "patient_code",
                    "nameField", "patient_name",
                    "genderField", "gender",
                    "birthDateField", "birth_date")),
            "cdm_encounter", new DatasetSpec("就诊主题", List.of("idField", "nameField", "extraCodeField", "eventTimeField"), Map.of(
                    "idField", "patient_code",
                    "nameField", "encounter_type",
                    "extraCodeField", "encounter_code",
                    "valueField", "department_name",
                    "unitField", "doctor_name",
                    "eventTimeField", "encounter_date")),
            "cdm_lab", new DatasetSpec("检验主题", List.of("idField", "nameField", "extraCodeField", "eventTimeField"), Map.of(
                    "idField", "patient_code",
                    "genderField", "encounter_code",
                    "extraCodeField", "item_code",
                    "nameField", "item_name",
                    "valueField", "result_value",
                    "unitField", "unit",
                    "birthDateField", "result_flag",
                    "eventTimeField", "report_date")),
            "cdm_lab_result", new DatasetSpec("检验结果主题", List.of("idField", "nameField", "extraCodeField"), Map.of(
                    "idField", "patient_code",
                    "extraCodeField", "item_code",
                    "nameField", "item_name",
                    "valueField", "result_value",
                    "unitField", "result_unit",
                    "eventTimeField", "sample_time")),
            "cdm_medication_order", new DatasetSpec("用药医嘱主题", List.of("idField", "nameField", "extraCodeField"), Map.of(
                    "idField", "patient_code",
                    "extraCodeField", "drug_code",
                    "nameField", "drug_name",
                    "valueField", "dose_value",
                    "unitField", "dose_unit",
                    "eventTimeField", "order_time")));

    private EtlModule() {
    }

    private record DatasetSpec(String label, List<String> requiredFields, Map<String, String> fieldBindingTargets) {
    }

    public record EtlJobRequest(
            @NotBlank String name,
            @NotNull Long dataSourceId,
            @NotBlank String loadMode,
            @NotBlank String targetDatasetCode,
            String sourceTable,
            String extractSql,
            String incrementField,
            Map<String, String> fieldBindings,
            String idField,
            String nameField,
            String genderField,
            String birthDateField,
            String extraCodeField,
            String valueField,
            String unitField,
            String eventTimeField) {
    }

    public record EtlJobView(Long id, String name, Long dataSourceId, String loadMode, String sourceTable,
            String extractSql, String incrementField, String targetDatasetCode, String idField, String nameField,
            String genderField, String birthDateField, String extraCodeField, String valueField, String unitField,
            String eventTimeField, Map<String, String> fieldBindings, String status) {
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
                    "select id, name, data_source_id, load_mode, source_table, extract_sql, increment_field, target_dataset_code, id_field, name_field, gender_field, birth_date_field, extra_code_field, value_field, unit_field, event_time_field, field_bindings_json, status from etl_job order by id",
                    (rs, rowNum) -> new EtlJobView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("data_source_id"),
                            rs.getString("load_mode"),
                            rs.getString("source_table"),
                            rs.getString("extract_sql"),
                            rs.getString("increment_field"),
                            rs.getString("target_dataset_code"),
                            rs.getString("id_field"),
                            rs.getString("name_field"),
                            rs.getString("gender_field"),
                            rs.getString("birth_date_field"),
                            rs.getString("extra_code_field"),
                            rs.getString("value_field"),
                            rs.getString("unit_field"),
                            rs.getString("event_time_field"),
                            parseFieldBindings(
                                    rs.getString("field_bindings_json"),
                                    rs.getString("target_dataset_code"),
                                    rs.getString("id_field"),
                                    rs.getString("name_field"),
                                    rs.getString("gender_field"),
                                    rs.getString("birth_date_field"),
                                    rs.getString("extra_code_field"),
                                    rs.getString("value_field"),
                                    rs.getString("unit_field"),
                                    rs.getString("event_time_field")),
                            rs.getString("status")));
        }

        public EtlJobView createJob(EtlJobRequest request) {
            validateRequest(request);
            dataSourceService.getById(request.dataSourceId());
            String datasetCode = Objects.toString(request.targetDatasetCode(), "").toLowerCase();
            jdbcTemplate.update(
                    "insert into etl_job(name, data_source_id, load_mode, source_table, extract_sql, increment_field, target_dataset_code, id_field, name_field, gender_field, birth_date_field, extra_code_field, value_field, unit_field, event_time_field, field_bindings_json, status) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    request.name(), request.dataSourceId(), request.loadMode(), request.sourceTable(), request.extractSql(),
                    request.incrementField(), request.targetDatasetCode(),
                    resolveRequestField(request, datasetCode, "idField"),
                    resolveRequestField(request, datasetCode, "nameField"),
                    resolveRequestField(request, datasetCode, "genderField"),
                    resolveRequestField(request, datasetCode, "birthDateField"),
                    resolveRequestField(request, datasetCode, "extraCodeField"),
                    resolveRequestField(request, datasetCode, "valueField"),
                    resolveRequestField(request, datasetCode, "unitField"),
                    resolveRequestField(request, datasetCode, "eventTimeField"),
                    serializeFieldBindings(datasetCode, request),
                    "READY");
            Long id = jdbcTemplate.queryForObject("select max(id) from etl_job", Long.class);
            userAccountService.audit(userAccountService.currentActor(), "ETL_JOB_CREATE", "etl_job", String.valueOf(id),
                    request.name() + " -> " + request.targetDatasetCode());
            return getJob(id);
        }

        private void validateRequest(EtlJobRequest request) {
            String datasetCode = Objects.toString(request.targetDatasetCode(), "").toLowerCase();
            DatasetSpec datasetSpec = DATASET_SPECS.get(datasetCode);
            if (datasetSpec == null) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST,
                        "unsupported dataset: " + request.targetDatasetCode());
            }
            if (isBlank(request.sourceTable())) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "sourceTable is required");
            }
            if (!"FULL".equalsIgnoreCase(request.loadMode()) && !"INCREMENTAL".equalsIgnoreCase(request.loadMode())) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "loadMode must be FULL or INCREMENTAL");
            }
            List<String> missingFields = datasetSpec.requiredFields().stream()
                    .filter(field -> isBlank(resolveRequestField(request, datasetCode, field)))
                    .toList();
            if (!missingFields.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST,
                        datasetSpec.label() + "缺少必填字段: " + String.join(", ", missingFields));
            }
        }

        private String resolveRequestField(EtlJobRequest request, String datasetCode, String fieldName) {
            String legacyValue = switch (fieldName) {
                case "idField" -> request.idField();
                case "nameField" -> request.nameField();
                case "genderField" -> request.genderField();
                case "birthDateField" -> request.birthDateField();
                case "extraCodeField" -> request.extraCodeField();
                case "valueField" -> request.valueField();
                case "unitField" -> request.unitField();
                case "eventTimeField" -> request.eventTimeField();
                default -> null;
            };
            if (!isBlank(legacyValue)) {
                return legacyValue;
            }
            DatasetSpec datasetSpec = DATASET_SPECS.get(datasetCode);
            if (datasetSpec == null || request.fieldBindings() == null) {
                return legacyValue;
            }
            String semanticField = datasetSpec.fieldBindingTargets().get(fieldName);
            if (semanticField == null) {
                return legacyValue;
            }
            String semanticValue = request.fieldBindings().get(semanticField);
            return isBlank(semanticValue) ? legacyValue : semanticValue;
        }

        public EtlJobView getJob(Long id) {
            List<EtlJobView> items = jdbcTemplate.query(
                    "select id, name, data_source_id, load_mode, source_table, extract_sql, increment_field, target_dataset_code, id_field, name_field, gender_field, birth_date_field, extra_code_field, value_field, unit_field, event_time_field, field_bindings_json, status from etl_job where id = ?",
                    (rs, rowNum) -> new EtlJobView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("data_source_id"),
                            rs.getString("load_mode"),
                            rs.getString("source_table"),
                            rs.getString("extract_sql"),
                            rs.getString("increment_field"),
                            rs.getString("target_dataset_code"),
                            rs.getString("id_field"),
                            rs.getString("name_field"),
                            rs.getString("gender_field"),
                            rs.getString("birth_date_field"),
                            rs.getString("extra_code_field"),
                            rs.getString("value_field"),
                            rs.getString("unit_field"),
                            rs.getString("event_time_field"),
                            parseFieldBindings(
                                    rs.getString("field_bindings_json"),
                                    rs.getString("target_dataset_code"),
                                    rs.getString("id_field"),
                                    rs.getString("name_field"),
                                    rs.getString("gender_field"),
                                    rs.getString("birth_date_field"),
                                    rs.getString("extra_code_field"),
                                    rs.getString("value_field"),
                                    rs.getString("unit_field"),
                                    rs.getString("event_time_field")),
                            rs.getString("status")),
                    id);
            if (items.isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.NOT_FOUND, "etl job not found");
            }
            return items.get(0);
        }

        private String serializeFieldBindings(String datasetCode, EtlJobRequest request) {
            try {
                return objectMapper.writeValueAsString(buildSemanticBindings(datasetCode, request));
            } catch (Exception exception) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "invalid field bindings payload");
            }
        }

        private Map<String, String> buildSemanticBindings(String datasetCode, EtlJobRequest request) {
            DatasetSpec datasetSpec = DATASET_SPECS.get(datasetCode);
            Map<String, String> result = new LinkedHashMap<>();
            if (datasetSpec == null) {
                return result;
            }
            for (Map.Entry<String, String> entry : datasetSpec.fieldBindingTargets().entrySet()) {
                String value = resolveRequestField(request, datasetCode, entry.getKey());
                if (!isBlank(value)) {
                    result.put(entry.getValue(), value);
                }
            }
            return result;
        }

        private Map<String, String> parseFieldBindings(String json, String datasetCode,
                String idField, String nameField, String genderField, String birthDateField,
                String extraCodeField, String valueField, String unitField, String eventTimeField) {
            if (!isBlank(json)) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> parsed = objectMapper.readValue(json, LinkedHashMap.class);
                    return parsed;
                } catch (Exception ignored) {
                    // Fall back to synthesizing from legacy columns.
                }
            }
            DatasetSpec datasetSpec = DATASET_SPECS.get(Objects.toString(datasetCode, "").toLowerCase());
            if (datasetSpec == null) {
                return Map.of();
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : datasetSpec.fieldBindingTargets().entrySet()) {
                String value = switch (entry.getKey()) {
                    case "idField" -> idField;
                    case "nameField" -> nameField;
                    case "genderField" -> genderField;
                    case "birthDateField" -> birthDateField;
                    case "extraCodeField" -> extraCodeField;
                    case "valueField" -> valueField;
                    case "unitField" -> unitField;
                    case "eventTimeField" -> eventTimeField;
                    default -> null;
                };
                if (!isBlank(value)) {
                    result.put(entry.getValue(), value);
                }
            }
            return result;
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
                    upsertStandardizedRecord(job, row);
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

        private void upsertStandardizedRecord(EtlJobView job, Map<String, Object> row) {
            switch (Objects.toString(job.targetDatasetCode(), "").toLowerCase()) {
                case "cdm_patient" -> upsertPatient(job, row);
                case "cdm_encounter" -> upsertEncounter(job, row);
                case "cdm_lab" -> upsertLab(job, row);
                case "cdm_lab_result" -> upsertLabResult(job, row);
                case "cdm_medication_order" -> upsertMedicationOrder(job, row);
                default -> throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST,
                        "unsupported dataset: " + job.targetDatasetCode());
            }
        }

        private void upsertPatient(EtlJobView job, Map<String, Object> row) {
            String patientCode = requiredField(row, job.idField(), "患者主题缺少主标识字段值");
            String patientName = requiredField(row, job.nameField(), "患者主题缺少名称字段值");
            String gender = optionalField(row, job.genderField());
            String birthDate = optionalField(row, job.birthDateField());
            jdbcTemplate.update(
                    "insert into cdm_patient(source_job_id, patient_code, patient_name, gender, birth_date) "
                            + "values (?, ?, ?, ?, ?) "
                            + "on duplicate key update patient_name = values(patient_name), "
                            + "gender = values(gender), birth_date = values(birth_date)",
                    job.id(), patientCode, patientName, gender, birthDate);
        }

        private void upsertEncounter(EtlJobView job, Map<String, Object> row) {
            String patientCode = requiredField(row, job.idField(), "就诊主题缺少患者标识");
            String encounterType = requiredField(row, job.nameField(), "就诊主题缺少就诊类型");
            String encounterCode = requiredField(row, job.extraCodeField(), "就诊主题缺少就诊编码");
            String departmentName = optionalField(row, job.valueField());
            String doctorName = optionalField(row, job.unitField());
            String encounterDate = requiredField(row, job.eventTimeField(), "就诊主题缺少就诊日期");
            jdbcTemplate.update(
                    "insert into cdm_encounter(patient_code, encounter_code, encounter_type, department_name, doctor_name, encounter_date) "
                            + "values (?, ?, ?, ?, ?, ?) "
                            + "on duplicate key update patient_code = values(patient_code), "
                            + "encounter_type = values(encounter_type), department_name = values(department_name), "
                            + "doctor_name = values(doctor_name), encounter_date = values(encounter_date)",
                    patientCode, encounterCode, encounterType, departmentName, doctorName, encounterDate);
        }

        private void upsertLabResult(EtlJobView job, Map<String, Object> row) {
            String patientCode = requiredField(row, job.idField(), "检验结果主题缺少患者标识");
            String itemCode = requiredField(row, job.extraCodeField(), "检验结果主题缺少项目编码");
            String itemName = requiredField(row, job.nameField(), "检验结果主题缺少项目名称");
            String resultValue = optionalField(row, job.valueField());
            String resultUnit = optionalField(row, job.unitField());
            String sampleTime = optionalField(row, job.eventTimeField());
            jdbcTemplate.update(
                    "insert into cdm_lab_result(source_job_id, patient_code, item_code, item_name, result_value, result_unit, sample_time) "
                            + "values (?, ?, ?, ?, ?, ?, ?) "
                            + "on duplicate key update item_name = values(item_name), "
                            + "result_value = values(result_value), result_unit = values(result_unit)",
                    job.id(), patientCode, itemCode, itemName, resultValue, resultUnit, sampleTime);
        }

        private void upsertLab(EtlJobView job, Map<String, Object> row) {
            String patientCode = requiredField(row, job.idField(), "检验主题缺少患者标识");
            String encounterCode = optionalField(row, job.genderField());
            String itemCode = requiredField(row, job.extraCodeField(), "检验主题缺少项目编码");
            String itemName = requiredField(row, job.nameField(), "检验主题缺少项目名称");
            String resultValue = optionalField(row, job.valueField());
            String unit = optionalField(row, job.unitField());
            String resultFlag = optionalField(row, job.birthDateField());
            String reportDate = requiredField(row, job.eventTimeField(), "检验主题缺少报告日期");
            jdbcTemplate.update(
                    "insert into cdm_lab(patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                    patientCode, encounterCode, itemCode, itemName, resultValue, unit, resultFlag, reportDate);
        }

        private void upsertMedicationOrder(EtlJobView job, Map<String, Object> row) {
            String patientCode = requiredField(row, job.idField(), "用药主题缺少患者标识");
            String drugCode = requiredField(row, job.extraCodeField(), "用药主题缺少药品编码");
            String drugName = requiredField(row, job.nameField(), "用药主题缺少药品名称");
            String doseValue = optionalField(row, job.valueField());
            String doseUnit = optionalField(row, job.unitField());
            String orderTime = optionalField(row, job.eventTimeField());
            jdbcTemplate.update(
                    "insert into cdm_medication_order(source_job_id, patient_code, drug_code, drug_name, dose_value, dose_unit, order_time) "
                            + "values (?, ?, ?, ?, ?, ?, ?) "
                            + "on duplicate key update drug_name = values(drug_name), "
                            + "dose_value = values(dose_value), dose_unit = values(dose_unit)",
                    job.id(), patientCode, drugCode, drugName, doseValue, doseUnit, orderTime);
        }

        private String optionalField(Map<String, Object> row, String fieldName) {
            if (isBlank(fieldName)) {
                return null;
            }
            String value = Objects.toString(row.get(fieldName), null);
            return value == null || value.isBlank() ? null : value;
        }

        private String requiredField(Map<String, Object> row, String fieldName, String message) {
            String value = optionalField(row, fieldName);
            if (isBlank(value)) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, message);
            }
            return value;
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
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
            jdbcTemplate.update(
                    "insert into etl_increment_checkpoint(job_id, checkpoint_value, updated_at) values (?, ?, current_timestamp) "
                            + "on duplicate key update checkpoint_value = values(checkpoint_value), "
                            + "updated_at = values(updated_at)",
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
