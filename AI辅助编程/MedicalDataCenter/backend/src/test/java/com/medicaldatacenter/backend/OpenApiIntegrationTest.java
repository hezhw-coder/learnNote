package com.medicaldatacenter.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        jdbcTemplate.update("delete from cdm_encounter");
        jdbcTemplate.update("delete from cdm_lab");
        jdbcTemplate.update("delete from cdm_medication_order");
        jdbcTemplate.update("delete from api_access_log");
        jdbcTemplate.update(
                "insert into cdm_patient(source_job_id, patient_code, patient_name, gender, birth_date) values (?, ?, ?, ?, ?)",
                1L, "P100", "张三丰", "F", "1995-05-20");
        jdbcTemplate.update(
                "insert into cdm_encounter(patient_code, encounter_code, encounter_type, department_name, doctor_name, encounter_date) values (?, ?, ?, ?, ?, ?)",
                "P100", "E100", "门诊", "心内科", "Dr.Wang", "2026-05-25");
        jdbcTemplate.update(
                "insert into cdm_lab(patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "P100", "E100", "HB", "血红蛋白", "128", "g/L", "NORMAL", "2026-05-25");
        jdbcTemplate.update(
                "insert into cdm_lab(patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "P100", "E100", "CRP", "C反应蛋白", "12.6", "mg/L", "HIGH", "2026-05-26");
        jdbcTemplate.update(
                "insert into cdm_medication_order(source_job_id, patient_code, drug_code, drug_name, dose_value, dose_unit, order_time) values (?, ?, ?, ?, ?, ?, ?)",
                1L, "P100", "ASP100", "阿司匹林肠溶片", "100", "mg", "2026-05-25 08:00:00");
    }

    @Test
    void issueOpenApiTokenAndQueryPatients() throws Exception {
        String token = requestToken("patients.read");

        mockMvc.perform(get("/open-api/v1/patients")
                .param("patientCode", "P100")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_code").value("P100"))
                .andExpect(jsonPath("$.data[0].patient_name").value("张*丰"));
    }

    @Test
    void issueTokenWithBasicAuthAndQueryRealTopicDataWithFilters() throws Exception {
        jdbcTemplate.update("delete from api_client where client_id = ?", "full-scope-client");
        jdbcTemplate.update(
                "insert into api_client(name, client_id, encrypted_secret, scopes, enabled) values (?, ?, ?, ?, ?)",
                "Full Scope Client", "full-scope-client", Base64.getEncoder().encodeToString("full-secret".getBytes()),
                "patients.read,encounters.read,labs.read,medications.read", true);

        String token = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", basic("full-scope-client", "full-secret"))
                .content("grant_type=client_credentials&scope=patients.read%20encounters.read%20labs.read%20medications.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopes[0]").value("patients.read"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(token).path("data").path("accessToken").asText();

        mockMvc.perform(get("/open-api/v1/patients")
                .param("gender", "F")
                .param("nameKeyword", "三丰")
                .param("startDate", "1995-01-01")
                .param("endDate", "1995-12-31")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].patient_name").value("张*丰"));

        mockMvc.perform(get("/open-api/v1/encounters")
                .param("patientCode", "P100")
                .param("departmentName", "心内")
                .param("doctorName", "wang")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].encounter_code").value("E100"))
                .andExpect(jsonPath("$.data[0].doctor_name").value("D*****g"));

        mockMvc.perform(get("/open-api/v1/labs")
                .param("patientCode", "P100")
                .param("abnormalOnly", "true")
                .param("itemCode", "CRP")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].item_code").value("CRP"))
                .andExpect(jsonPath("$.data[0].result_flag").value("HIGH"));

        mockMvc.perform(get("/open-api/v1/medications")
                .param("patientCode", "P100")
                .param("drugName", "阿司匹林")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].drug_code").value("ASP100"))
                .andExpect(jsonPath("$.data[0].dose_unit").value("mg"));

        Integer successCount = jdbcTemplate.queryForObject(
                "select count(1) from api_access_log where client_id = ? and status = ? and message = ?",
                Integer.class,
                "full-scope-client",
                200,
                "ok rows=1");
        org.junit.jupiter.api.Assertions.assertEquals(4, successCount);
    }

    @Test
    void rejectRequestWhenScopeMissingAndWriteAccessLog() throws Exception {
        String token = requestToken("patients.read");

        mockMvc.perform(get("/open-api/v1/labs").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("scope not allowed"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from api_access_log where endpoint = ? and status = ? and message = ?",
                Integer.class,
                "/open-api/v1/labs",
                403,
                "scope not allowed");
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void rateLimitRequestAndWriteAccessLog() throws Exception {
        jdbcTemplate.update("delete from api_client where client_id = ?", "rate-limit-client");
        jdbcTemplate.update(
                "insert into api_client(name, client_id, encrypted_secret, scopes, enabled) values (?, ?, ?, ?, ?)",
                "Rate Limit Client", "rate-limit-client",
                Base64.getEncoder().encodeToString("limit-secret".getBytes(StandardCharsets.UTF_8)),
                "patients.read", true);
        jdbcTemplate.update("delete from api_rate_limit_rule where client_id = ? and endpoint = ?",
                "rate-limit-client", "/open-api/v1/patients");
        jdbcTemplate.update(
                "insert into api_rate_limit_rule(client_id, endpoint, capacity, refill_tokens, refill_seconds) values (?, ?, ?, ?, ?)",
                "rate-limit-client", "/open-api/v1/patients", 2, 2, 60);

        String token = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "clientId": "rate-limit-client",
                          "clientSecret": "limit-secret",
                          "scope": "patients.read"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(token).path("data").path("accessToken").asText();

        mockMvc.perform(get("/open-api/v1/patients").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/open-api/v1/patients").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/open-api/v1/patients").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("rate limited"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from api_access_log where client_id = ? and endpoint = ? and status = ?",
                Integer.class,
                "rate-limit-client",
                "/open-api/v1/patients",
                429);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    private String requestToken(String scope) throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "clientId": "demo-client",
                          "clientSecret": "demo-secret",
                          "scope": "%s"
                        }
                        """.formatted(scope)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(tokenResult.getResponse().getContentAsString()).path("data")
                .path("accessToken").asText();
    }

    private String basic(String clientId, String clientSecret) {
        String raw = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
