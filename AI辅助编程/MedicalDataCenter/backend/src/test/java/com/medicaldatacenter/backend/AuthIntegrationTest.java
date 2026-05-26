package com.medicaldatacenter.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        "spring.datasource.url=jdbc:h2:mem:authdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginRefreshLogoutFlow() throws Exception {
        JsonNode loginData = login("admin", "Admin@123");
        String accessToken = loginData.path("accessToken").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.tokenKind").value("PLATFORM"));

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        JsonNode refreshData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String refreshedAccessToken = refreshData.path("accessToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("logged out"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(refreshData.path("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordPolicyAndFailureControl() throws Exception {
        String adminToken = login("admin", "Admin@123").path("accessToken").asText();
        Long adminRoleId = jdbcTemplate.queryForObject("select id from sys_role where code = 'ADMIN'", Long.class);

        mockMvc.perform(post("/api/system/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "weak_user",
                          "password": "12345678",
                          "displayName": "弱密码用户",
                          "roleIds": [%d]
                        }
                        """.formatted(adminRoleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "password must be at least 8 characters and include upper, lower, digit, special character"));

        mockMvc.perform(post("/api/system/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "secure_user",
                          "password": "Secure@123",
                          "displayName": "安全用户",
                          "roleIds": [%d]
                        }
                        """.formatted(adminRoleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("secure_user"));

        for (int index = 0; index < 4; index++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "username": "secure_user",
                              "password": "wrong-password"
                            }
                            """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "secure_user",
                          "password": "wrong-password"
                        }
                        """))
                .andExpect(status().isLocked());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "secure_user",
                          "password": "Secure@123"
                        }
                        """))
                .andExpect(status().isLocked());
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
