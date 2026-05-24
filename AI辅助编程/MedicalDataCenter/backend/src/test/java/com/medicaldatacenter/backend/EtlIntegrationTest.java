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

    @BeforeEach
    void prepareSource() throws Exception {
        try (Connection connection = DriverManager.getConnection(SOURCE_URL, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists patient_source");
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
