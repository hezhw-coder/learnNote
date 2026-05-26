package com.medicaldatacenter.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:etldb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@AutoConfigureMockMvc
class EtlIntegrationTest {

    private static final String SOURCE_URL = "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareSource() throws Exception {
        try (Connection connection = DriverManager.getConnection(SOURCE_URL, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists patient_source");
            statement.execute("drop table if exists encounter_source");
            statement.execute("drop table if exists lab_source");
            statement.execute("drop table if exists lab_result_source");
            statement.execute("drop table if exists medication_order_source");
            statement.execute("""
                    create table patient_source (
                        patient_id varchar(32),
                        patient_name varchar(64),
                        gender varchar(16),
                        birth_date varchar(16),
                        updated_at varchar(32)
                    )
                    """);
            statement.execute("""
                    insert into patient_source(patient_id, patient_name, gender, birth_date, updated_at)
                    values ('P001', 'Alice', 'F', '1990-01-01', '2026-05-24T10:00:00'),
                           ('P002', 'Bob', 'M', '1988-02-02', '2026-05-24T11:00:00')
                    """);
            statement.execute("""
                    create table encounter_source (
                        patient_code varchar(32),
                        encounter_code varchar(32),
                        encounter_type varchar(32),
                        department_name varchar(64),
                        doctor_name varchar(64),
                        encounter_date varchar(32),
                        updated_at varchar(32)
                    )
                    """);
            statement.execute("""
                    insert into encounter_source(patient_code, encounter_code, encounter_type, department_name, doctor_name, encounter_date, updated_at)
                    values ('P001', 'E001', '门诊', '心内科', '李医生', '2026-05-24', '2026-05-24T11:30:00'),
                           ('P002', 'E002', '住院', '内分泌科', '王医生', '2026-05-24', '2026-05-24T11:40:00')
                    """);
            statement.execute("""
                    create table lab_source (
                        patient_code varchar(32),
                        encounter_code varchar(32),
                        item_code varchar(32),
                        item_name varchar(64),
                        result_value varchar(32),
                        unit varchar(16),
                        result_flag varchar(16),
                        report_date varchar(32),
                        updated_at varchar(32)
                    )
                    """);
            statement.execute("""
                    insert into lab_source(patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date, updated_at)
                    values ('P001', 'E001', 'HB', 'Hemoglobin', '135', 'g/L', 'NORMAL', '2026-05-24', '2026-05-24T11:50:00'),
                           ('P002', 'E002', 'GLU', 'Glucose', '7.1', 'mmol/L', 'HIGH', '2026-05-24', '2026-05-24T11:55:00')
                    """);
            statement.execute("""
                    create table lab_result_source (
                        patient_code varchar(32),
                        item_code varchar(32),
                        item_name varchar(64),
                        result_value varchar(32),
                        result_unit varchar(32),
                        sample_time varchar(32),
                        updated_at varchar(32)
                    )
                    """);
            statement.execute("""
                    insert into lab_result_source(patient_code, item_code, item_name, result_value, result_unit, sample_time, updated_at)
                    values ('P001', 'HB', 'Hemoglobin', '135', 'g/L', '2026-05-24 08:00:00', '2026-05-24T12:00:00'),
                           ('P002', 'GLU', 'Glucose', '5.8', 'mmol/L', '2026-05-24 09:30:00', '2026-05-24T12:10:00')
                    """);
            statement.execute("""
                    create table medication_order_source (
                        patient_code varchar(32),
                        drug_code varchar(32),
                        drug_name varchar(64),
                        dose_value varchar(32),
                        dose_unit varchar(16),
                        order_time varchar(32),
                        updated_at varchar(32)
                    )
                    """);
            statement.execute("""
                    insert into medication_order_source(patient_code, drug_code, drug_name, dose_value, dose_unit, order_time, updated_at)
                    values ('P001', 'ASP100', 'Aspirin', '100', 'mg', '2026-05-24 08:00:00', '2026-05-24T12:20:00'),
                           ('P002', 'MET500', 'Metformin', '500', 'mg', '2026-05-24 09:00:00', '2026-05-24T12:30:00')
                    """);
        }
    }

    @Test
    void createDataSourceAndRunEtl() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        MvcResult jobResult = mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "patient-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_patient",
                          "sourceTable": "patient_source",
                          "extractSql": "select patient_id, patient_name, gender, birth_date, updated_at from patient_source",
                          "idField": "patient_id",
                          "nameField": "patient_name",
                          "genderField": "gender",
                          "birthDateField": "birth_date"
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isOk())
                .andReturn();

        long jobId = readId(jobResult);

        mockMvc.perform(post("/api/etl/jobs/" + jobId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.extractedCount").value(2));

        mockMvc.perform(get("/api/datasets/sample").param("code", "cdm_patient")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P001"))
                .andExpect(jsonPath("$.data[1].patient_name").value("Bob"));
    }

    @Test
    void createEncounterJobWithSemanticFieldBindings() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-semantic-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        MvcResult jobResult = mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "semantic-encounter-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_encounter",
                          "sourceTable": "encounter_source",
                          "fieldBindings": {
                            "patient_code": "patient_code",
                            "encounter_type": "encounter_type",
                            "encounter_code": "encounter_code",
                            "department_name": "department_name",
                            "doctor_name": "doctor_name",
                            "encounter_date": "encounter_date"
                          }
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idField").value("patient_code"))
                .andExpect(jsonPath("$.data.extraCodeField").value("encounter_code"))
                .andExpect(jsonPath("$.data.eventTimeField").value("encounter_date"))
                .andExpect(jsonPath("$.data.fieldBindings.patient_code").value("patient_code"))
                .andExpect(jsonPath("$.data.fieldBindings.encounter_code").value("encounter_code"))
                .andReturn();

        long jobId = readId(jobResult);
        String fieldBindingsJson = jdbcTemplate.queryForObject(
                "select field_bindings_json from etl_job where id = ?",
                String.class,
                jobId);
        JsonNode bindingsNode = objectMapper.readTree(fieldBindingsJson);
        org.junit.jupiter.api.Assertions.assertEquals("encounter_code", bindingsNode.path("encounter_code").asText());
        org.junit.jupiter.api.Assertions.assertEquals("encounter_date", bindingsNode.path("encounter_date").asText());

        mockMvc.perform(post("/api/etl/jobs/" + jobId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.extractedCount").value(2));
    }

    @Test
    void rejectJobWhenDatasetRequiredFieldMissing() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-invalid-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "invalid-encounter-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_encounter",
                          "sourceTable": "encounter_source",
                          "idField": "patient_code",
                          "nameField": "encounter_type"
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("就诊主题缺少必填字段: extraCodeField, eventTimeField"));
    }

    @Test
    void createLabResultJobAndPreviewDataset() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-lab-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        MvcResult jobResult = mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "lab-result-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_lab_result",
                          "sourceTable": "lab_result_source",
                          "extractSql": "select patient_code, item_code, item_name, result_value, result_unit, sample_time, updated_at from lab_result_source",
                          "idField": "patient_code",
                          "nameField": "item_name",
                          "extraCodeField": "item_code",
                          "valueField": "result_value",
                          "unitField": "result_unit",
                          "eventTimeField": "sample_time"
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isOk())
                .andReturn();

        long jobId = readId(jobResult);

        mockMvc.perform(post("/api/etl/jobs/" + jobId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.extractedCount").value(2));

        mockMvc.perform(get("/api/datasets/sample").param("code", "cdm_lab_result")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P001"))
                .andExpect(jsonPath("$.data[0].item_code").value("HB"))
                .andExpect(jsonPath("$.data[1].result_value").value("5.8"));
    }

    @Test
    void createEncounterJobAndPreviewDataset() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-encounter-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        MvcResult jobResult = mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "encounter-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_encounter",
                          "sourceTable": "encounter_source",
                          "extractSql": "select patient_code, encounter_code, encounter_type, department_name, doctor_name, encounter_date, updated_at from encounter_source",
                          "idField": "patient_code",
                          "nameField": "encounter_type",
                          "extraCodeField": "encounter_code",
                          "valueField": "department_name",
                          "unitField": "doctor_name",
                          "eventTimeField": "encounter_date"
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isOk())
                .andReturn();

        long jobId = readId(jobResult);

        mockMvc.perform(post("/api/etl/jobs/" + jobId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.extractedCount").value(2));

        mockMvc.perform(get("/api/datasets/sample").param("code", "cdm_encounter")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P001"))
                .andExpect(jsonPath("$.data[0].encounter_code").value("E001"))
                .andExpect(jsonPath("$.data[1].doctor_name").value("王医生"));

        mockMvc.perform(get("/api/datasets")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'cdm_encounter')]").isNotEmpty());
    }

    @Test
    void createLabJobAndPreviewDataset() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-lab-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        MvcResult jobResult = mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "lab-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_lab",
                          "sourceTable": "lab_source",
                          "extractSql": "select patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date, updated_at from lab_source",
                          "idField": "patient_code",
                          "nameField": "item_name",
                          "genderField": "encounter_code",
                          "birthDateField": "result_flag",
                          "extraCodeField": "item_code",
                          "valueField": "result_value",
                          "unitField": "unit",
                          "eventTimeField": "report_date"
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isOk())
                .andReturn();

        long jobId = readId(jobResult);

        mockMvc.perform(post("/api/etl/jobs/" + jobId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.extractedCount").value(2));

        mockMvc.perform(get("/api/datasets/sample").param("code", "cdm_lab")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P001"))
                .andExpect(jsonPath("$.data[0].encounter_code").value("E001"))
                .andExpect(jsonPath("$.data[1].result_flag").value("HIGH"));

        mockMvc.perform(get("/api/datasets")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'cdm_lab')]").isNotEmpty());
    }

    @Test
    void createMedicationOrderJobAndPreviewDataset() throws Exception {
        String token = login();

        MvcResult dataSourceResult = mockMvc.perform(post("/api/data-sources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "h2-medication-source",
                          "type": "H2",
                          "jdbcUrl": "jdbc:h2:mem:etl-source;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                          "username": "sa",
                          "password": "",
                          "databaseName": "etl-source"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long dataSourceId = readId(dataSourceResult);

        MvcResult jobResult = mockMvc.perform(post("/api/etl/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "medication-order-job",
                          "dataSourceId": %d,
                          "loadMode": "FULL",
                          "targetDatasetCode": "cdm_medication_order",
                          "sourceTable": "medication_order_source",
                          "extractSql": "select patient_code, drug_code, drug_name, dose_value, dose_unit, order_time, updated_at from medication_order_source",
                          "idField": "patient_code",
                          "nameField": "drug_name",
                          "extraCodeField": "drug_code",
                          "valueField": "dose_value",
                          "unitField": "dose_unit",
                          "eventTimeField": "order_time"
                        }
                        """.formatted(dataSourceId)))
                .andExpect(status().isOk())
                .andReturn();

        long jobId = readId(jobResult);

        mockMvc.perform(post("/api/etl/jobs/" + jobId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.extractedCount").value(2));

        mockMvc.perform(get("/api/datasets/sample").param("code", "cdm_medication_order")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P001"))
                .andExpect(jsonPath("$.data[0].drug_code").value("ASP100"))
                .andExpect(jsonPath("$.data[1].dose_value").value("500"));

        mockMvc.perform(get("/api/datasets")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'cdm_medication_order')]").isNotEmpty());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "admin",
                          "password": "Admin@123"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken")
                .asText();
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asLong();
    }
}
