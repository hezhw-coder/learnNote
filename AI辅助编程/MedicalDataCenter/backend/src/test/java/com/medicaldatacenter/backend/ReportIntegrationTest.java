package com.medicaldatacenter.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
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
        "spring.datasource.url=jdbc:h2:mem:reportdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "app.export.dir=./target/test-exports"
})
@AutoConfigureMockMvc
class ReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createTemplateAndRunScheduleChain() throws Exception {
        String token = login();

        MvcResult templateResult = mockMvc.perform(post("/api/reports/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "患者主数据任务报表",
                          "datasetCode": "cdm_patient",
                          "designJson": "{\"id\":\"template-new\",\"name\":\"患者主数据任务报表\",\"schemaVersion\":\"1.1.0\",\"datasetCode\":\"cdm_patient\",\"layout\":{\"columns\":2,\"gap\":16,\"gridColumns\":24,\"rowHeight\":120,\"canvasWidthMode\":\"full\"},\"filters\":[{\"id\":\"filter-1\",\"field\":\"gender\",\"label\":\"性别\",\"operator\":\"eq\",\"value\":\"女\"}],\"parameters\":[{\"id\":\"param-1\",\"code\":\"patientNameKeyword\",\"label\":\"患者姓名关键词\",\"field\":\"patient_name\",\"operator\":\"contains\",\"defaultValue\":\"\",\"required\":false,\"type\":\"string\"}],\"widgets\":[{\"id\":\"widget-1\",\"type\":\"table\",\"title\":\"患者表格\",\"datasetId\":\"cdm_patient\",\"span\":2,\"sortOrder\":1,\"visible\":true,\"grid\":{\"x\":0,\"y\":0,\"w\":24,\"h\":2},\"bindings\":[{\"id\":\"binding-1\",\"role\":\"category\",\"label\":\"主列字段\",\"field\":\"patient_name\"},{\"id\":\"binding-2\",\"role\":\"value\",\"label\":\"辅助列字段\",\"field\":\"patient_code\"}],\"config\":{\"xField\":\"patient_name\",\"yField\":\"patient_code\",\"color\":\"#409eff\"}},{\"id\":\"widget-2\",\"type\":\"bar\",\"title\":\"性别统计\",\"datasetId\":\"cdm_patient\",\"span\":1,\"sortOrder\":2,\"visible\":true,\"grid\":{\"x\":0,\"y\":2,\"w\":12,\"h\":2},\"bindings\":[{\"id\":\"binding-3\",\"role\":\"category\",\"label\":\"维度字段\",\"field\":\"gender\"},{\"id\":\"binding-4\",\"role\":\"value\",\"label\":\"指标字段\",\"field\":\"patient_code\",\"aggregation\":\"count\"}],\"config\":{\"xField\":\"gender\",\"yField\":\"patient_code\",\"color\":\"#67c23a\",\"aggregation\":\"count\"}}],\"interactions\":[{\"id\":\"interaction-1\",\"name\":\"性别联动患者表格\",\"sourceWidgetId\":\"widget-2\",\"targetWidgetId\":\"widget-1\",\"trigger\":\"click\",\"action\":\"filter\",\"sourceField\":\"gender\",\"targetField\":\"gender\"}]}"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.datasetCode").value("cdm_patient"))
                .andReturn();

        long templateId = readId(templateResult);

        String createdDesignJson = jdbcTemplate.queryForObject(
                "select design_json from report_template where id = ?",
                String.class,
                templateId);
        org.assertj.core.api.Assertions.assertThat(createdDesignJson)
                .contains("\"state\":\"draft\"")
                .contains("\"draftVersion\":1");

        mockMvc.perform(post("/api/reports/templates/" + templateId + "/publish")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        String publishedDesignJson = jdbcTemplate.queryForObject(
                "select design_json from report_template where id = ?",
                String.class,
                templateId);
        org.assertj.core.api.Assertions.assertThat(publishedDesignJson)
                .contains("\"state\":\"published\"")
                .contains("\"publishedVersion\":1")
                .contains("\"effectiveVersion\":1");

        mockMvc.perform(get("/api/reports/templates/" + templateId + "/preview")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.datasetCode").value("cdm_patient"))
                .andExpect(jsonPath("$.data.layoutColumns").value(2))
                .andExpect(jsonPath("$.data.widgetCount").value(2))
                .andExpect(jsonPath("$.data.filterCount").value(1))
                .andExpect(jsonPath("$.data.appliedFilters[0]").value(org.hamcrest.Matchers.containsString("性别")))
                .andExpect(jsonPath("$.data.bindingSummary[0]").value(org.hamcrest.Matchers.containsString("患者表格")))
                .andExpect(jsonPath("$.data.interactionSummary[0]").value(org.hamcrest.Matchers.containsString("性别联动患者表格")));

        mockMvc.perform(post("/api/reports/templates/" + templateId + "/preview")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "parameters": [
                            {
                              "parameterCode": "patientNameKeyword",
                              "value": "王"
                            }
                          ],
                          "interactions": [
                            {
                              "interactionId": "interaction-1",
                              "sourceWidgetId": "widget-2",
                              "targetWidgetId": "widget-1",
                              "sourceField": "gender",
                              "targetField": "gender",
                              "value": "女"
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parameterSummary[0]").value(org.hamcrest.Matchers.containsString("患者姓名关键词")))
                .andExpect(jsonPath("$.data.appliedFilters[1]").value(org.hamcrest.Matchers.containsString("性别联动患者表格")))
                .andExpect(jsonPath("$.data.appliedFilters[2]").value(org.hamcrest.Matchers.containsString("患者姓名关键词")))
                .andExpect(jsonPath("$.data.interactionSummary[0]").value(org.hamcrest.Matchers.containsString("已应用=女")))
                .andExpect(jsonPath("$.data.rows.length()").value(1));

        mockMvc.perform(get("/api/reports/templates/" + templateId + "/export/pdf")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        mockMvc.perform(post("/api/reports/templates/" + templateId + "/export/pdf")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "parameters": [
                            {
                              "parameterCode": "patientNameKeyword",
                              "value": "王"
                            }
                          ],
                          "interactions": [
                            {
                              "interactionId": "interaction-1",
                              "sourceWidgetId": "widget-2",
                              "targetWidgetId": "widget-1",
                              "sourceField": "gender",
                              "targetField": "gender",
                              "value": "女"
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        mockMvc.perform(get("/api/reports/templates/" + templateId + "/export/excel")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        MvcResult scheduleResult = mockMvc.perform(post("/api/reports/schedules")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "templateId": %d,
                          "cronExpression": "0 0 8 * * ?",
                          "enabled": true,
                          "channel": "IN_APP",
                          "runtimeParams": {
                            "parameters": [
                              {
                                "parameterCode": "patientNameKeyword",
                                "value": "王"
                              }
                            ],
                            "interactions": [
                              {
                                "interactionId": "interaction-1",
                                "sourceWidgetId": "widget-2",
                                "targetWidgetId": "widget-1",
                                "sourceField": "gender",
                                "targetField": "gender",
                                "value": "女"
                              }
                            ]
                          }
                        }
                        """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextRun").isNotEmpty())
                .andExpect(jsonPath("$.data.templateVersion").value(1))
                .andExpect(jsonPath("$.data.runtimeParams.parameters[0].value").value("王"))
                .andExpect(jsonPath("$.data.runtimeParams.interactions[0].value").value("女"))
                .andReturn();

        long scheduleId = readId(scheduleResult);
        org.assertj.core.api.Assertions.assertThat(scheduler.checkExists(
                com.medicaldatacenter.backend.report.ReportModule.ReportService.jobKey(scheduleId))).isTrue();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/reports/schedules/" + scheduleId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "templateId": %d,
                          "cronExpression": "0 15 9 * * ?",
                          "enabled": false,
                          "channel": "IN_APP",
                          "runtimeParams": {
                            "parameters": [
                              {
                                "parameterCode": "patientNameKeyword",
                                "value": "李"
                              }
                            ],
                            "interactions": [
                              {
                                "interactionId": "interaction-1",
                                "sourceWidgetId": "widget-2",
                                "targetWidgetId": "widget-1",
                                "sourceField": "gender",
                                "targetField": "gender",
                                "value": "男"
                              }
                            ]
                          }
                        }
                        """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.runtimeParams.parameters[0].value").value("李"))
                .andExpect(jsonPath("$.data.runtimeParams.interactions[0].value").value("男"));

        org.assertj.core.api.Assertions.assertThat(scheduler.checkExists(
                com.medicaldatacenter.backend.report.ReportModule.ReportService.jobKey(scheduleId))).isFalse();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/reports/schedules/" + scheduleId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "templateId": %d,
                          "cronExpression": "0 0 8 * * ?",
                          "enabled": true,
                          "channel": "IN_APP",
                          "runtimeParams": {
                            "parameters": [
                              {
                                "parameterCode": "patientNameKeyword",
                                "value": "王"
                              }
                            ],
                            "interactions": [
                              {
                                "interactionId": "interaction-1",
                                "sourceWidgetId": "widget-2",
                                "targetWidgetId": "widget-1",
                                "sourceField": "gender",
                                "targetField": "gender",
                                "value": "女"
                              }
                            ]
                          }
                        }
                        """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextRun").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(scheduler.checkExists(
                com.medicaldatacenter.backend.report.ReportModule.ReportService.jobKey(scheduleId))).isTrue();

        mockMvc.perform(post("/api/reports/schedules/" + scheduleId + "/run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("executed")));

        mockMvc.perform(get("/api/reports/snapshots")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].templateId").value(templateId))
                .andExpect(jsonPath("$.data[0].templateVersion").value(1))
                .andExpect(jsonPath("$.data[0].templateName").value("患者主数据任务报表"))
                .andExpect(jsonPath("$.data[0].runtimeParams.parameters[0].value").value("王"))
                .andExpect(jsonPath("$.data[0].runtimeParams.interactions[0].value").value("女"));

        mockMvc.perform(get("/api/reports/notifications")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("report schedule complete"))
                .andExpect(jsonPath("$.data[0].content").value(org.hamcrest.Matchers.containsString("患者主数据任务报表")));

        String snapshotJson = jdbcTemplate.queryForObject(
                "select snapshot_json from report_snapshot where template_id = ? order by id desc limit 1",
                String.class,
                templateId);
        org.assertj.core.api.Assertions.assertThat(snapshotJson)
                .contains("appliedFilters")
                .contains("bindingSummary")
                .contains("parameterSummary")
                .contains("interactionSummary")
                .contains("患者表格");

        String scheduleRuntimeParamsJson = jdbcTemplate.queryForObject(
                "select runtime_params_json from report_schedule where id = ?",
                String.class,
                scheduleId);
        org.assertj.core.api.Assertions.assertThat(scheduleRuntimeParamsJson)
                .contains("\"parameterCode\":\"patientNameKeyword\"")
                .contains("\"value\":\"王\"")
                .contains("\"value\":\"女\"");

        Integer scheduleTemplateVersion = jdbcTemplate.queryForObject(
                "select template_version from report_schedule where id = ?",
                Integer.class,
                scheduleId);
        org.assertj.core.api.Assertions.assertThat(scheduleTemplateVersion).isEqualTo(1);

        String snapshotRuntimeParamsJson = jdbcTemplate.queryForObject(
                "select runtime_params_json from report_snapshot where schedule_id = ? order by id desc limit 1",
                String.class,
                scheduleId);
        org.assertj.core.api.Assertions.assertThat(snapshotRuntimeParamsJson)
                .contains("\"parameterCode\":\"patientNameKeyword\"")
                .contains("\"value\":\"王\"")
                .contains("\"value\":\"女\"");

        Integer snapshotTemplateVersion = jdbcTemplate.queryForObject(
                "select template_version from report_snapshot where schedule_id = ? order by id desc limit 1",
                Integer.class,
                scheduleId);
        org.assertj.core.api.Assertions.assertThat(snapshotTemplateVersion).isEqualTo(1);
    }

    @Test
    void rejectTemplateUpdateWhenBaseVersionIsStale() throws Exception {
        String token = login();

        MvcResult templateResult = mockMvc.perform(post("/api/reports/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "版本冲突测试报表",
                          "datasetCode": "cdm_patient",
                          "designJson": "{\"id\":\"template-new\",\"name\":\"版本冲突测试报表\",\"schemaVersion\":\"1.1.0\",\"datasetCode\":\"cdm_patient\",\"layout\":{\"columns\":2,\"gap\":16,\"gridColumns\":24,\"rowHeight\":120,\"canvasWidthMode\":\"full\"},\"widgets\":[{\"id\":\"widget-1\",\"type\":\"metric\",\"title\":\"患者指标卡\",\"datasetId\":\"cdm_patient\",\"span\":1,\"sortOrder\":1,\"visible\":true,\"grid\":{\"x\":0,\"y\":0,\"w\":12,\"h\":1},\"bindings\":[{\"id\":\"binding-1\",\"role\":\"category\",\"label\":\"主展示字段\",\"field\":\"patient_name\"},{\"id\":\"binding-2\",\"role\":\"value\",\"label\":\"指标字段\",\"field\":\"patient_code\",\"aggregation\":\"count\"}],\"config\":{\"xField\":\"patient_name\",\"yField\":\"patient_code\",\"aggregation\":\"count\"}}],\"filters\":[],\"parameters\":[],\"interactions\":[]}"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        long templateId = readId(templateResult);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/reports/templates/" + templateId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "版本冲突测试报表-过期提交",
                          "datasetCode": "cdm_patient",
                          "designJson": "{\"id\":\"%d\",\"name\":\"版本冲突测试报表-过期提交\",\"schemaVersion\":\"1.1.0\",\"status\":\"DRAFT\",\"datasetCode\":\"cdm_patient\",\"versioning\":{\"baseVersion\":0,\"draftVersion\":1,\"publishedVersion\":0,\"effectiveVersion\":0,\"state\":\"draft\",\"lastSavedAt\":\"\",\"lastPublishedAt\":\"\"},\"layout\":{\"columns\":2,\"gap\":16,\"gridColumns\":24,\"rowHeight\":120,\"canvasWidthMode\":\"full\"},\"widgets\":[{\"id\":\"widget-1\",\"type\":\"metric\",\"title\":\"患者指标卡\",\"datasetId\":\"cdm_patient\",\"span\":1,\"sortOrder\":1,\"visible\":true,\"grid\":{\"x\":0,\"y\":0,\"w\":12,\"h\":1},\"bindings\":[{\"id\":\"binding-1\",\"role\":\"category\",\"label\":\"主展示字段\",\"field\":\"patient_name\"},{\"id\":\"binding-2\",\"role\":\"value\",\"label\":\"指标字段\",\"field\":\"patient_code\",\"aggregation\":\"count\"}],\"config\":{\"xField\":\"patient_name\",\"yField\":\"patient_code\",\"aggregation\":\"count\"}}],\"filters\":[],\"parameters\":[],\"interactions\":[]}"
                        }
                        """.formatted(templateId)))
                .andExpect(status().isConflict());
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
