package com.medicaldatacenter.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapidb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareData() {
        jdbcTemplate.update("delete from cdm_patient");
        jdbcTemplate.update(
                "insert into cdm_patient(source_job_id, patient_code, patient_name, gender, birth_date) values (?, ?, ?, ?, ?)",
                1L, "P100", "Test Patient", "F", "1995-05-20");
    }

    @Test
    void issueOpenApiTokenAndQueryPatients() throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "clientId": "demo-client",
                          "clientSecret": "demo-secret",
                          "scope": "patients.read"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopes[0]").value("patients.read"))
                .andReturn();

        String token = objectMapper.readTree(tokenResult.getResponse().getContentAsString()).path("data")
                .path("accessToken").asText();

        mockMvc.perform(get("/open-api/v1/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P100"))
                .andExpect(jsonPath("$.data[0].patient_name").value("Test Patient"));
    }
}
