package com.medicaldatacenter.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:systemdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@AutoConfigureMockMvc
class SystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completeSystemManagementFlow() throws Exception {
        String token = login();
        Long adminRoleId = jdbcTemplate.queryForObject("select id from sys_role where code = 'ADMIN'", Long.class);
        Long authMePermissionId = jdbcTemplate.queryForObject("select id from sys_permission where code = 'AUTH_ME'",
                Long.class);
        Long systemUserViewPermissionId = jdbcTemplate.queryForObject(
                "select id from sys_permission where code = 'SYSTEM_USER_VIEW'", Long.class);

        MvcResult createRoleResult = mockMvc.perform(post("/api/system/roles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "code": "ops_manager",
                          "name": "运营管理员",
                          "permissionIds": [%d]
                        }
                        """.formatted(authMePermissionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("OPS_MANAGER"))
                .andExpect(jsonPath("$.data.permissions[0]").value("AUTH_ME"))
                .andReturn();

        long roleId = objectMapper.readTree(createRoleResult.getResponse().getContentAsString()).path("data").path("id")
                .asLong();

        mockMvc.perform(put("/api/system/roles/" + roleId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "code": "ops_supervisor",
                          "name": "运营主管"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("OPS_SUPERVISOR"))
                .andExpect(jsonPath("$.data.name").value("运营主管"));

        mockMvc.perform(put("/api/system/roles/" + roleId + "/permissions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "permissionIds": [%d, %d]
                        }
                        """.formatted(authMePermissionId, systemUserViewPermissionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions[0]").value("AUTH_ME"))
                .andExpect(jsonPath("$.data.permissions[1]").value("SYSTEM_USER_VIEW"));

        MvcResult createResult = mockMvc.perform(post("/api/system/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "ops_user",
                          "password": "Ops@123456",
                          "displayName": "运营用户",
                          "roleIds": [%d]
                        }
                        """.formatted(roleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ops_user"))
                .andExpect(jsonPath("$.data.roles[0]").value("OPS_SUPERVISOR"))
                .andReturn();

        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/api/system/users/" + userId + "/roles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "roleIds": [%d]
                        }
                        """.formatted(adminRoleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));

        mockMvc.perform(put("/api/system/parameters/app.security.issuer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "value": "medical-data-center-updated"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("app.security.issuer"))
                .andExpect(jsonPath("$.data.value").value("medical-data-center-updated"));

        mockMvc.perform(put("/api/system/users/" + userId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "enabled": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/system/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.username=='ops_user')]").isNotEmpty());

        Boolean enabled = jdbcTemplate.queryForObject("select enabled from sys_user where username = ?",
                Boolean.class, "ops_user");
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, enabled);

        Integer auditCount = jdbcTemplate.queryForObject(
                "select count(1) from sys_audit_log where action in ('SYSTEM_USER_CREATE', 'SYSTEM_USER_ROLE_ASSIGN', 'SYSTEM_USER_STATUS') and target_id = ?",
                Integer.class,
                String.valueOf(userId));
        org.junit.jupiter.api.Assertions.assertEquals(3, auditCount);

        Integer roleAuditCount = jdbcTemplate.queryForObject(
                "select count(1) from sys_audit_log where action in ('SYSTEM_ROLE_CREATE', 'SYSTEM_ROLE_UPDATE', 'SYSTEM_ROLE_PERMISSION_ASSIGN') and target_id = ?",
                Integer.class,
                String.valueOf(roleId));
        org.junit.jupiter.api.Assertions.assertEquals(3, roleAuditCount);

        String issuer = jdbcTemplate.queryForObject("select param_value from sys_parameter where param_key = ?",
                String.class, "app.security.issuer");
        org.junit.jupiter.api.Assertions.assertEquals("medical-data-center-updated", issuer);
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
}
