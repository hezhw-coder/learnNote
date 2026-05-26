package com.medicaldatacenter.backend.report;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.medicaldatacenter.backend.common.CommonSupport;
import com.medicaldatacenter.backend.system.SystemModule;
import com.medicaldatacenter.backend.warehouse.WarehouseModule;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ReportModule {

    private ReportModule() {
    }

    public record ReportTemplateRequest(@NotBlank String name, @NotBlank String datasetCode, @NotBlank String designJson) {
    }

    public record ReportTemplateView(Long id, String name, String datasetCode, String designJson, String status,
            LocalDateTime createdAt) {
    }

    public record TemplateLayout(Integer columns, Integer gap, Integer gridColumns, Integer rowHeight, String canvasWidthMode) {
    }

    public record WidgetGrid(Integer x, Integer y, Integer w, Integer h) {
    }

    public record TemplateFilter(String field, String label, String operator, String value) {
    }

    public record TemplateVersioning(Integer baseVersion, Integer draftVersion, Integer publishedVersion,
            Integer effectiveVersion, String state, String lastSavedAt, String lastPublishedAt) {
    }

    public record TemplateParameter(String id, String code, String label, String field, String operator,
            String defaultValue, boolean required, String type) {
    }

    public record WidgetBinding(String id, String role, String label, String field, String aggregation) {
    }

    public record TemplateWidget(String id, String type, String title, String datasetId, Integer span, Integer sortOrder,
            Boolean visible, WidgetGrid grid, List<WidgetBinding> bindings, Map<String, Object> config, String description) {
    }

    public record InteractionRule(String id, String name, String sourceWidgetId, String targetWidgetId, String trigger,
            String action, String sourceField, String targetField) {
    }

    public record TemplateSchema(String id, String name, String schemaVersion, String status, String datasetCode,
            TemplateVersioning versioning, TemplateLayout layout,
            List<TemplateFilter> filters, List<TemplateParameter> parameters, List<TemplateWidget> widgets,
            List<InteractionRule> interactions) {
    }

    public record RenderedReport(Long templateId, String templateName, String datasetCode, TemplateLayout layout,
            List<TemplateFilter> filters, List<TemplateWidget> widgets, List<String> appliedFilters,
            List<String> bindingSummary, List<String> parameterSummary, List<String> interactionSummary,
            List<Map<String, Object>> rows) {
    }

    public record ReportPreview(String templateName, String datasetCode, Integer layoutColumns, Integer widgetCount,
            Integer filterCount, List<String> appliedFilters, List<String> bindingSummary, List<String> parameterSummary,
            List<String> interactionSummary,
            List<Map<String, Object>> rows) {
    }

    public record PreviewInteractionContext(@NotBlank String interactionId, @NotBlank String sourceWidgetId,
            @NotBlank String targetWidgetId, @NotBlank String sourceField, @NotBlank String targetField, @NotBlank String value) {
    }

    public record ReportPreviewRequest(List<RuntimeParameterValue> parameters, List<PreviewInteractionContext> interactions) {
    }

    public record RuntimeParameterValue(String parameterCode, String value) {
    }

    public record ReportRuntimeParams(List<RuntimeParameterValue> parameters, List<PreviewInteractionContext> interactions) {
    }

    public record ReportScheduleRequest(@NotNull Long templateId, @NotBlank String cronExpression, boolean enabled,
            String channel, ReportRuntimeParams runtimeParams) {
    }

    public record ReportScheduleView(Long id, Long templateId, String cronExpression, boolean enabled, String channel,
            Integer templateVersion, LocalDateTime latestRun, LocalDateTime nextRun, String owner,
            ReportRuntimeParams runtimeParams) {
    }

    public record ReportSnapshotView(Long id, Long templateId, Long scheduleId, Integer templateVersion, String templateName,
            String exportPath, ReportRuntimeParams runtimeParams, LocalDateTime createdAt) {
    }

    public record ReportNotificationView(Long id, String title, String content, String recipient, boolean read,
            LocalDateTime createdAt) {
    }

    public record ReportActionResult(String message) {
    }

    @Service
    @Validated
    public static class ReportService {
        private final JdbcTemplate jdbcTemplate;
        private final WarehouseModule.DatasetService datasetService;
        private final ObjectMapper objectMapper;
        private final SystemModule.UserAccountService userAccountService;
        private final Path exportDir;
        private final Scheduler scheduler;
        private static final int DEFAULT_GRID_COLUMNS = 24;

        public ReportService(JdbcTemplate jdbcTemplate, WarehouseModule.DatasetService datasetService,
                ObjectMapper objectMapper, SystemModule.UserAccountService userAccountService,
                @Value("${app.export.dir}") String exportDir, Scheduler scheduler) {
            this.jdbcTemplate = jdbcTemplate;
            this.datasetService = datasetService;
            this.objectMapper = objectMapper;
            this.userAccountService = userAccountService;
            this.exportDir = Path.of(exportDir);
            this.scheduler = scheduler;
        }

        @PostConstruct
        void initializeScheduler() throws SchedulerException {
            scheduler.getContext().put("reportService", this);
            syncAllSchedules();
        }

        public List<ReportTemplateView> listTemplates() {
            return jdbcTemplate.query(
                    "select id, name, dataset_code, design_json, status, created_at from report_template order by id desc",
                    (rs, rowNum) -> new ReportTemplateView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("dataset_code"),
                            rs.getString("design_json"),
                            rs.getString("status"),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public ReportTemplateView createTemplate(ReportTemplateRequest request) {
            ReportTemplateRequest normalizedRequest = normalizeTemplateRequest(request, null, false);
            jdbcTemplate.update("insert into report_template(name, dataset_code, design_json, status) values (?, ?, ?, ?)",
                    normalizedRequest.name(), normalizedRequest.datasetCode(), normalizedRequest.designJson(), "DRAFT");
            Long id = jdbcTemplate.queryForObject("select max(id) from report_template", Long.class);
            userAccountService.audit(userAccountService.currentActor(), "REPORT_CREATE", "report_template",
                    String.valueOf(id), normalizedRequest.name());
            return getTemplate(id);
        }

        public ReportTemplateView updateTemplate(Long id, ReportTemplateRequest request) {
            ReportTemplateView existing = getTemplate(id);
            ReportTemplateRequest normalizedRequest = normalizeTemplateRequest(request, existing, false);
            jdbcTemplate.update(
                    "update report_template set name = ?, dataset_code = ?, design_json = ?, status = ?, updated_at = current_timestamp where id = ?",
                    normalizedRequest.name(), normalizedRequest.datasetCode(), normalizedRequest.designJson(), "DRAFT", id);
            userAccountService.audit(userAccountService.currentActor(), "REPORT_UPDATE", "report_template",
                    String.valueOf(id), normalizedRequest.name());
            return getTemplate(id);
        }

        public ReportTemplateView publishTemplate(Long id) {
            ReportTemplateView existing = getTemplate(id);
            TemplateSchema schema = parseTemplateSchema(existing);
            validatePublishable(schema);
            TemplateSchema published = markAsPublished(schema);
            try {
                jdbcTemplate.update(
                        "update report_template set name = ?, dataset_code = ?, design_json = ?, status = ?, updated_at = current_timestamp where id = ?",
                        published.name(), published.datasetCode(), objectMapper.writeValueAsString(published), "PUBLISHED", id);
            } catch (Exception exception) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "invalid report template schema");
            }
            userAccountService.audit(userAccountService.currentActor(), "REPORT_PUBLISH", "report_template",
                    String.valueOf(id), published.name());
            return getTemplate(id);
        }

        public ReportTemplateView getTemplate(Long id) {
            return jdbcTemplate.query(
                    "select id, name, dataset_code, design_json, status, created_at from report_template where id = ?",
                    (rs, rowNum) -> new ReportTemplateView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("dataset_code"),
                            rs.getString("design_json"),
                            rs.getString("status"),
                            rs.getObject("created_at", LocalDateTime.class)),
                    id).get(0);
        }

        public ReportPreview preview(Long templateId) {
            RenderedReport rendered = renderTemplate(templateId, new ReportRuntimeParams(List.of(), List.of()));
            return new ReportPreview(rendered.templateName(), rendered.datasetCode(), rendered.layout().columns(),
                    rendered.widgets().size(), rendered.filters().size(), rendered.appliedFilters(),
                    rendered.bindingSummary(), rendered.parameterSummary(), rendered.interactionSummary(), rendered.rows());
        }

        public ReportPreview preview(Long templateId, ReportPreviewRequest request) {
            RenderedReport rendered = renderTemplate(templateId, normalizeRuntimeParams(request == null ? null
                    : new ReportRuntimeParams(request.parameters(), request.interactions())));
            return new ReportPreview(rendered.templateName(), rendered.datasetCode(), rendered.layout().columns(),
                    rendered.widgets().size(), rendered.filters().size(), rendered.appliedFilters(),
                    rendered.bindingSummary(), rendered.parameterSummary(), rendered.interactionSummary(), rendered.rows());
        }

        public byte[] exportPdf(Long templateId) throws Exception {
            return exportPdf(templateId, new ReportRuntimeParams(List.of(), List.of()));
        }

        public byte[] exportPdf(Long templateId, ReportRuntimeParams runtimeParams) throws Exception {
            ReportRuntimeParams normalizedRuntimeParams = normalizeRuntimeParams(runtimeParams);
            RenderedReport rendered = renderTemplate(templateId, normalizedRuntimeParams);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph(rendered.templateName()));
            document.add(new Paragraph("dataset=" + rendered.datasetCode() + ", layoutColumns="
                    + rendered.layout().columns() + ", widgets=" + rendered.widgets().size() + ", filters="
                    + rendered.filters().size() + ", rows=" + rendered.rows().size()));
            if (!rendered.appliedFilters().isEmpty()) {
                document.add(new Paragraph("applied filters: " + String.join(" ; ", rendered.appliedFilters())));
            }
            if (!rendered.bindingSummary().isEmpty()) {
                document.add(new Paragraph("bindings: " + String.join(" ; ", rendered.bindingSummary())));
            }
            if (!rendered.parameterSummary().isEmpty()) {
                document.add(new Paragraph("parameters: " + String.join(" ; ", rendered.parameterSummary())));
            }
            if (!rendered.interactionSummary().isEmpty()) {
                document.add(new Paragraph("interactions: " + String.join(" ; ", rendered.interactionSummary())));
            }
            PdfPTable table = new PdfPTable(Math.max(1, rendered.rows().isEmpty() ? 1 : rendered.rows().get(0).size()));
            if (!rendered.rows().isEmpty()) {
                rendered.rows().get(0).keySet().forEach(table::addCell);
                for (Map<String, Object> row : rendered.rows()) {
                    row.values().forEach(value -> table.addCell(String.valueOf(value)));
                }
            } else {
                table.addCell("No data");
            }
            document.add(table);
            document.close();
            persistSnapshot(templateId, null, resolveSchedulableTemplateVersion(getTemplate(templateId)), normalizedRuntimeParams,
                    outputStream.toByteArray(), rendered, "pdf", "report ready",
                    "report template " + rendered.templateName() + " exported as pdf with " + rendered.rows().size()
                            + " rows",
                    userAccountService.currentActor());
            return outputStream.toByteArray();
        }

        public byte[] exportExcel(Long templateId) throws Exception {
            return exportExcel(templateId, new ReportRuntimeParams(List.of(), List.of()));
        }

        public byte[] exportExcel(Long templateId, ReportRuntimeParams runtimeParams) throws Exception {
            ReportRuntimeParams normalizedRuntimeParams = normalizeRuntimeParams(runtimeParams);
            RenderedReport rendered = renderTemplate(templateId, normalizedRuntimeParams);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                var sheet = workbook.createSheet("report");
                Row titleRow = sheet.createRow(0);
                titleRow.createCell(0).setCellValue(rendered.templateName());
                Row summaryRow = sheet.createRow(1);
                summaryRow.createCell(0).setCellValue("dataset=" + rendered.datasetCode() + ", widgets="
                        + rendered.widgets().size() + ", filters=" + rendered.filters().size() + ", rows="
                        + rendered.rows().size());
                Row filterRow = sheet.createRow(2);
                filterRow.createCell(0).setCellValue(rendered.appliedFilters().isEmpty() ? "filters=none"
                        : "filters=" + String.join(" ; ", rendered.appliedFilters()));
                Row parameterRow = sheet.createRow(3);
                parameterRow.createCell(0).setCellValue(rendered.parameterSummary().isEmpty() ? "parameters=none"
                        : "parameters=" + String.join(" ; ", rendered.parameterSummary()));
                if (!rendered.rows().isEmpty()) {
                    Row header = sheet.createRow(5);
                    List<String> columns = rendered.rows().get(0).keySet().stream().toList();
                    for (int index = 0; index < columns.size(); index++) {
                        header.createCell(index).setCellValue(columns.get(index));
                    }
                    for (int rowIndex = 0; rowIndex < rendered.rows().size(); rowIndex++) {
                        Row row = sheet.createRow(rowIndex + 6);
                        Map<String, Object> item = rendered.rows().get(rowIndex);
                        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                            row.createCell(columnIndex).setCellValue(String.valueOf(item.get(columns.get(columnIndex))));
                        }
                    }
                }
                workbook.write(outputStream);
            }
            persistSnapshot(templateId, null, resolveSchedulableTemplateVersion(getTemplate(templateId)), normalizedRuntimeParams,
                    outputStream.toByteArray(), rendered, "xlsx", "report ready",
                    "report template " + rendered.templateName() + " exported as xlsx with " + rendered.rows().size()
                            + " rows",
                    userAccountService.currentActor());
            return outputStream.toByteArray();
        }

        public List<ReportScheduleView> listSchedules() {
            List<ReportScheduleView> schedules = jdbcTemplate.query(
                    """
                            select s.id, s.template_id, s.cron_expression, s.enabled, s.channel, s.owner, s.template_version, s.runtime_params_json,
                                   max(ss.created_at) as latest_run
                            from report_schedule s
                            left join report_snapshot ss on ss.schedule_id = s.id
                            group by s.id, s.template_id, s.cron_expression, s.enabled, s.channel, s.owner, s.template_version, s.runtime_params_json
                            order by s.id
                            """,
                    (rs, rowNum) -> new ReportScheduleView(
                            rs.getLong("id"),
                            rs.getLong("template_id"),
                            rs.getString("cron_expression"),
                            rs.getBoolean("enabled"),
                            rs.getString("channel"),
                            rs.getInt("template_version"),
                            rs.getObject("latest_run", LocalDateTime.class),
                            resolveNextRun(rs.getLong("id")),
                            rs.getString("owner"),
                            parseRuntimeParamsJson(rs.getString("runtime_params_json"))));
            return schedules;
        }

        public ReportScheduleView createSchedule(ReportScheduleRequest request) {
            validateSchedule(request);
            ReportTemplateView template = getTemplate(request.templateId());
            Integer templateVersion = resolveSchedulableTemplateVersion(template);
            jdbcTemplate.update(
                    "insert into report_schedule(template_id, cron_expression, enabled, channel, owner, template_version, runtime_params_json) values (?, ?, ?, ?, ?, ?, ?)",
                    request.templateId(), request.cronExpression(), request.enabled(),
                    request.channel() == null || request.channel().isBlank() ? "IN_APP" : request.channel(),
                    userAccountService.currentActor(),
                    templateVersion,
                    serializeRuntimeParams(request.runtimeParams()));
            Long id = jdbcTemplate.queryForObject("select max(id) from report_schedule", Long.class);
            syncSchedule(id);
            userAccountService.audit(userAccountService.currentActor(), "REPORT_SCHEDULE_CREATE", "report_schedule",
                    String.valueOf(id), request.cronExpression());
            return getSchedule(id);
        }

        public ReportScheduleView updateSchedule(Long id, ReportScheduleRequest request) {
            validateSchedule(request);
            ReportScheduleView schedule = getSchedule(id);
            ReportTemplateView template = getTemplate(request.templateId());
            Integer templateVersion = resolveSchedulableTemplateVersion(template);
            jdbcTemplate.update(
                    "update report_schedule set template_id = ?, cron_expression = ?, enabled = ?, channel = ?, owner = ?, template_version = ?, runtime_params_json = ? where id = ?",
                    request.templateId(), request.cronExpression(), request.enabled(),
                    request.channel() == null || request.channel().isBlank() ? "IN_APP" : request.channel(),
                    schedule.owner(), templateVersion, serializeRuntimeParams(request.runtimeParams()), id);
            syncSchedule(id);
            userAccountService.audit(userAccountService.currentActor(), "REPORT_SCHEDULE_UPDATE", "report_schedule",
                    String.valueOf(id), request.cronExpression());
            return getSchedule(id);
        }

        public ReportActionResult runSchedule(Long scheduleId) throws Exception {
            ReportScheduleView schedule = getSchedule(scheduleId);
            String message = executeSchedule(schedule, userAccountService.currentActor(), "report schedule complete");
            userAccountService.audit(userAccountService.currentActor(), "REPORT_SCHEDULE_RUN", "report_schedule",
                    String.valueOf(scheduleId), message);
            return new ReportActionResult(message);
        }

        public void runScheduledJob(Long scheduleId) throws Exception {
            ReportScheduleView schedule = getSchedule(scheduleId);
            if (!schedule.enabled()) {
                unschedule(scheduleId);
                return;
            }
            String message = executeSchedule(schedule, schedule.owner(), "report schedule auto run");
            userAccountService.audit("scheduler", "REPORT_SCHEDULE_AUTO_RUN", "report_schedule",
                    String.valueOf(scheduleId), message);
        }

        public List<ReportSnapshotView> listSnapshots() {
            return jdbcTemplate.query(
                    """
                            select s.id, s.template_id, s.schedule_id, s.template_version, t.name as template_name, s.export_path, s.runtime_params_json, s.created_at
                            from report_snapshot s
                            join report_template t on t.id = s.template_id
                            order by s.created_at desc, s.id desc
                            limit 20
                            """,
                    (rs, rowNum) -> new ReportSnapshotView(
                            rs.getLong("id"),
                            rs.getLong("template_id"),
                            rs.getObject("schedule_id", Long.class),
                            rs.getInt("template_version"),
                            rs.getString("template_name"),
                            rs.getString("export_path"),
                            parseRuntimeParamsJson(rs.getString("runtime_params_json")),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public List<ReportNotificationView> listNotifications() {
            return jdbcTemplate.query(
                    """
                            select id, title, content, recipient, read_flag, created_at
                            from notify_message
                            where recipient = ?
                            order by created_at desc, id desc
                            limit 20
                            """,
                    (rs, rowNum) -> new ReportNotificationView(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getString("recipient"),
                            rs.getBoolean("read_flag"),
                            rs.getObject("created_at", LocalDateTime.class)),
                    userAccountService.currentActor());
        }

        private ReportScheduleView getSchedule(Long id) {
            return jdbcTemplate.query(
                    "select id, template_id, cron_expression, enabled, channel, owner, template_version, runtime_params_json from report_schedule where id = ?",
                    (rs, rowNum) -> new ReportScheduleView(
                            rs.getLong("id"),
                            rs.getLong("template_id"),
                            rs.getString("cron_expression"),
                            rs.getBoolean("enabled"),
                            rs.getString("channel"),
                            rs.getInt("template_version"),
                            null,
                            resolveNextRun(rs.getLong("id")),
                            rs.getString("owner"),
                            parseRuntimeParamsJson(rs.getString("runtime_params_json"))),
                    id).get(0);
        }

        private String executeSchedule(ReportScheduleView schedule, String recipient, String title) throws Exception {
            ReportRuntimeParams runtimeParams = normalizeRuntimeParams(schedule.runtimeParams());
            ReportTemplateView template = getTemplate(schedule.templateId());
            Integer currentTemplateVersion = resolveSchedulableTemplateVersion(template);
            if (!Objects.equals(currentTemplateVersion, schedule.templateVersion())) {
                throw new CommonSupport.BusinessException(HttpStatus.CONFLICT,
                        "report schedule template version mismatch: expected " + schedule.templateVersion()
                                + " but current effective version is " + currentTemplateVersion);
            }
            RenderedReport rendered = renderTemplate(schedule.templateId(), runtimeParams);
            String message = "report schedule " + schedule.id() + " executed for template " + rendered.templateName()
                    + " with " + rendered.rows().size() + " rows";
            persistSnapshot(schedule.templateId(), schedule.id(), schedule.templateVersion(), runtimeParams,
                    objectMapper.writeValueAsBytes(rendered), rendered, "json",
                    title, message, recipient);
            return message;
        }

        private void persistSnapshot(Long templateId, Long scheduleId, Integer templateVersion, ReportRuntimeParams runtimeParams, byte[] content, Object snapshotPayload, String extension,
                String title, String notifyContent, String recipient) throws Exception {
            Files.createDirectories(exportDir);
            Path target = exportDir.resolve("report-" + templateId + "-" + System.currentTimeMillis() + "." + extension);
            Files.write(target, content);
            jdbcTemplate.update(
                    "insert into report_snapshot(template_id, schedule_id, template_version, snapshot_json, export_path, runtime_params_json) values (?, ?, ?, ?, ?, ?)",
                    templateId, scheduleId, templateVersion, objectMapper.writeValueAsString(snapshotPayload), target.toAbsolutePath().toString(),
                    serializeRuntimeParams(runtimeParams));
            jdbcTemplate.update("insert into notify_message(title, content, recipient) values (?, ?, ?)",
                    title, notifyContent, recipient);
        }

        private Integer resolveSchedulableTemplateVersion(ReportTemplateView template) {
            TemplateSchema schema = parseTemplateSchema(template);
            int effectiveVersion = schema.versioning().effectiveVersion() == null ? 0 : schema.versioning().effectiveVersion();
            return effectiveVersion > 0 ? effectiveVersion : schema.versioning().draftVersion();
        }

        private void validateSchedule(ReportScheduleRequest request) {
            if (!CronExpression.isValidExpression(request.cronExpression())) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "invalid cron expression");
            }
        }

        private ReportRuntimeParams normalizeRuntimeParams(ReportRuntimeParams runtimeParams) {
            List<RuntimeParameterValue> normalizedParameters = new ArrayList<>();
            if (runtimeParams != null && runtimeParams.parameters() != null) {
                for (RuntimeParameterValue parameter : runtimeParams.parameters()) {
                    if (parameter == null) {
                        continue;
                    }
                    String parameterCode = firstNonBlank(parameter.parameterCode(), "");
                    if (parameterCode.isBlank()) {
                        continue;
                    }
                    normalizedParameters.add(new RuntimeParameterValue(parameterCode, firstNonBlank(parameter.value(), "")));
                }
            }
            List<PreviewInteractionContext> normalized = new ArrayList<>();
            if (runtimeParams != null && runtimeParams.interactions() != null) {
                for (PreviewInteractionContext interaction : runtimeParams.interactions()) {
                    if (interaction == null) {
                        continue;
                    }
                    String interactionId = firstNonBlank(interaction.interactionId(), "");
                    String sourceWidgetId = firstNonBlank(interaction.sourceWidgetId(), "");
                    String targetWidgetId = firstNonBlank(interaction.targetWidgetId(), "");
                    String sourceField = firstNonBlank(interaction.sourceField(), "");
                    String targetField = firstNonBlank(interaction.targetField(), "");
                    String value = firstNonBlank(interaction.value(), "");
                    if (interactionId.isBlank() || sourceWidgetId.isBlank() || targetWidgetId.isBlank()
                            || sourceField.isBlank() || targetField.isBlank() || value.isBlank()) {
                        continue;
                    }
                    normalized.add(new PreviewInteractionContext(interactionId, sourceWidgetId, targetWidgetId, sourceField,
                            targetField, value));
                }
            }
            return new ReportRuntimeParams(normalizedParameters, normalized);
        }

        private String serializeRuntimeParams(ReportRuntimeParams runtimeParams) {
            try {
                return objectMapper.writeValueAsString(normalizeRuntimeParams(runtimeParams));
            } catch (Exception exception) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "invalid report runtime params");
            }
        }

        private ReportRuntimeParams parseRuntimeParamsJson(String runtimeParamsJson) {
            if (runtimeParamsJson == null || runtimeParamsJson.isBlank()) {
                return new ReportRuntimeParams(List.of(), List.of());
            }
            try {
                return normalizeRuntimeParams(objectMapper.readValue(runtimeParamsJson, ReportRuntimeParams.class));
            } catch (Exception ignored) {
                return new ReportRuntimeParams(List.of(), List.of());
            }
        }

        private ReportTemplateRequest normalizeTemplateRequest(ReportTemplateRequest request, ReportTemplateView existing,
                boolean publishMode) {
            try {
                TemplateSchema requestedSchema = normalizeSchema(
                        objectMapper.readValue(request.designJson(), TemplateSchema.class),
                        request.name(),
                        request.datasetCode());
                TemplateSchema schema = publishMode
                        ? markAsPublished(applyVersioning(requestedSchema, existing, true))
                        : applyVersioning(requestedSchema, existing, false);
                return new ReportTemplateRequest(request.name(), schema.datasetCode(), objectMapper.writeValueAsString(schema));
            } catch (Exception exception) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "invalid report template schema");
            }
        }

        private RenderedReport renderTemplate(Long templateId, ReportRuntimeParams runtimeParams) {
            ReportTemplateView template = getTemplate(templateId);
            TemplateSchema schema = parseTemplateSchema(template);
            ReportRuntimeParams normalizedRuntimeParams = normalizeRuntimeParams(runtimeParams);
            List<Map<String, Object>> sourceRows = datasetService.sampleRows(schema.datasetCode());
            List<TemplateFilter> runtimeFilters = mergeFilters(schema, normalizedRuntimeParams);
            List<Map<String, Object>> filteredRows = applyFilters(sourceRows, runtimeFilters);
            return new RenderedReport(templateId, template.name(), schema.datasetCode(), schema.layout(), runtimeFilters,
                    schema.widgets(), summarizeFilters(runtimeFilters), summarizeBindings(schema.widgets()),
                    summarizeParameters(schema.parameters(), normalizedRuntimeParams.parameters()),
                    summarizeInteractions(schema.interactions(), schema.widgets(), normalizedRuntimeParams.interactions()), filteredRows);
        }

        private TemplateSchema parseTemplateSchema(ReportTemplateView template) {
            try {
                return normalizeSchema(objectMapper.readValue(template.designJson(), TemplateSchema.class), template.name(),
                        template.datasetCode());
            } catch (Exception exception) {
                return normalizeSchema(null, template.name(), template.datasetCode());
            }
        }

        private TemplateSchema normalizeSchema(TemplateSchema schema, String fallbackName, String fallbackDatasetCode) {
            String datasetCode = firstNonBlank(schema == null ? null : schema.datasetCode(), fallbackDatasetCode, "cdm_patient");
            Integer columns = clampInt(schema != null && schema.layout() != null ? schema.layout().columns() : null, 1, 4, 2);
            Integer gap = clampInt(schema != null && schema.layout() != null ? schema.layout().gap() : null, 8, 32, 16);
            TemplateLayout layout = new TemplateLayout(
                    columns,
                    gap,
                    clampInt(schema != null && schema.layout() != null ? schema.layout().gridColumns() : null, 12, 24, 24),
                    clampInt(schema != null && schema.layout() != null ? schema.layout().rowHeight() : null, 80, 200, 120),
                    firstNonBlank(schema != null && schema.layout() != null ? schema.layout().canvasWidthMode() : null, "full"));
            TemplateVersioning versioning = normalizeVersioning(schema == null ? null : schema.versioning(),
                    schema == null ? null : schema.status());
            List<TemplateFilter> filters = new ArrayList<>();
            if (schema != null && schema.filters() != null) {
                for (TemplateFilter filter : schema.filters()) {
                    String field = firstNonBlank(filter.field(), "");
                    if (field.isBlank()) {
                        continue;
                    }
                    filters.add(new TemplateFilter(field, firstNonBlank(filter.label(), field),
                            normalizeOperator(filter.operator()), firstNonBlank(filter.value(), "")));
                }
            }
            List<TemplateParameter> parameters = normalizeParameters(schema == null ? null : schema.parameters());
            List<TemplateWidget> widgets = new ArrayList<>();
            int cursorX = 0;
            int cursorY = 0;
            int currentRowHeight = 1;
            if (schema != null && schema.widgets() != null) {
                for (int index = 0; index < schema.widgets().size(); index++) {
                    TemplateWidget widget = schema.widgets().get(index);
                    if (widget == null) {
                        continue;
                    }
                    Map<String, Object> config = widget.config() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(widget.config());
                    String widgetId = firstNonBlank(widget.id(), "widget-" + (widgets.size() + 1));
                    List<WidgetBinding> bindings = normalizeBindings(widgetId, firstNonBlank(widget.type(), "metric"), widget.bindings(),
                            config);
                    WidgetGrid normalizedGrid = normalizeGrid(widget, columns);
                    int width = normalizedGrid.w();
                    int height = normalizedGrid.h();
                    if (cursorX + width > DEFAULT_GRID_COLUMNS) {
                        cursorX = 0;
                        cursorY += currentRowHeight;
                        currentRowHeight = 1;
                    }
                    WidgetGrid packedGrid = new WidgetGrid(cursorX, cursorY, width, height);
                    widgets.add(new TemplateWidget(
                            widgetId,
                            firstNonBlank(widget.type(), "metric"),
                            firstNonBlank(widget.title(), "组件-" + (widgets.size() + 1)),
                            datasetCode,
                            clampInt(widget.span(), 1, layout.columns(), "table".equalsIgnoreCase(widget.type()) ? layout.columns() : 1),
                            index + 1,
                            widget.visible() == null ? true : widget.visible(),
                            packedGrid,
                            bindings,
                            config,
                            firstNonBlank(widget.description(), "")));
                    cursorX += width;
                    currentRowHeight = Math.max(currentRowHeight, height);
                    if (cursorX >= DEFAULT_GRID_COLUMNS) {
                        cursorX = 0;
                        cursorY += currentRowHeight;
                        currentRowHeight = 1;
                    }
                }
            }
            if (widgets.isEmpty()) {
                widgets.add(new TemplateWidget("widget-1", "metric", "默认指标卡", datasetCode, 1,
                        1,
                        true,
                        new WidgetGrid(0, 0, 12, 1),
                        normalizeBindings("widget-1", "metric", null,
                                Map.of("xField", "patient_name", "yField", "patient_code", "color", "#409eff")),
                        Map.of("xField", "patient_name", "yField", "patient_code", "color", "#409eff"), ""));
            }
            List<InteractionRule> interactions = normalizeInteractions(schema == null ? null : schema.interactions(), widgets);
            return new TemplateSchema(firstNonBlank(schema == null ? null : schema.id(), "template-new"),
                    firstNonBlank(schema == null ? null : schema.name(), fallbackName, "未命名报表"),
                    firstNonBlank(schema == null ? null : schema.schemaVersion(), "1.1.0"),
                    firstNonBlank(schema == null ? null : schema.status(), versioning.state().equals("published") ? "PUBLISHED" : "DRAFT"),
                    datasetCode, versioning, layout,
                    filters, parameters, widgets, interactions);
        }

        private TemplateVersioning normalizeVersioning(TemplateVersioning versioning, String status) {
            int draftVersion = clampInt(versioning == null ? null : versioning.draftVersion(), 1, 9999, 1);
            int publishedVersion = clampInt(versioning == null ? null : versioning.publishedVersion(), 0, 9999, 0);
            int effectiveVersion = clampInt(versioning == null ? null : versioning.effectiveVersion(), 0, 9999, publishedVersion);
            return new TemplateVersioning(
                    clampInt(versioning == null ? null : versioning.baseVersion(), 0, 9999, draftVersion),
                    draftVersion,
                    publishedVersion,
                    effectiveVersion,
                    firstNonBlank(versioning == null ? null : versioning.state(),
                            "PUBLISHED".equalsIgnoreCase(status) ? "published" : "draft"),
                    firstNonBlank(versioning == null ? null : versioning.lastSavedAt(), ""),
                    firstNonBlank(versioning == null ? null : versioning.lastPublishedAt(), ""));
        }

        private TemplateSchema applyVersioning(TemplateSchema requestedSchema, ReportTemplateView existing, boolean publishMode) {
            if (existing == null) {
                TemplateVersioning requestedVersioning = normalizeVersioning(requestedSchema.versioning(), requestedSchema.status());
                return new TemplateSchema(requestedSchema.id(), requestedSchema.name(), requestedSchema.schemaVersion(),
                        publishMode ? "PUBLISHED" : "DRAFT", requestedSchema.datasetCode(),
                        new TemplateVersioning(
                                0,
                                1,
                                publishMode ? 1 : 0,
                                publishMode ? 1 : 0,
                                publishMode ? "published" : "draft",
                                LocalDateTime.now().toString(),
                                publishMode ? LocalDateTime.now().toString() : ""),
                        requestedSchema.layout(), requestedSchema.filters(), requestedSchema.parameters(), requestedSchema.widgets(),
                        requestedSchema.interactions());
            }
            TemplateSchema existingSchema = parseTemplateSchema(existing);
            TemplateVersioning requestedVersioning = normalizeVersioning(requestedSchema.versioning(), requestedSchema.status());
            if (requestedVersioning.baseVersion() != existingSchema.versioning().draftVersion()) {
                throw new CommonSupport.BusinessException(HttpStatus.CONFLICT,
                        "report template version conflict: expected baseVersion " + existingSchema.versioning().draftVersion());
            }
            int nextDraftVersion = existingSchema.versioning().draftVersion() + 1;
            String now = LocalDateTime.now().toString();
            TemplateVersioning nextVersioning = new TemplateVersioning(
                    existingSchema.versioning().draftVersion(),
                    nextDraftVersion,
                    publishMode ? nextDraftVersion : existingSchema.versioning().publishedVersion(),
                    publishMode ? nextDraftVersion : existingSchema.versioning().effectiveVersion(),
                    publishMode ? "published" : "draft",
                    now,
                    publishMode ? now : existingSchema.versioning().lastPublishedAt());
            return new TemplateSchema(existingSchema.id(), requestedSchema.name(), requestedSchema.schemaVersion(),
                    publishMode ? "PUBLISHED" : "DRAFT", requestedSchema.datasetCode(), nextVersioning,
                    requestedSchema.layout(), requestedSchema.filters(), requestedSchema.parameters(), requestedSchema.widgets(),
                    requestedSchema.interactions());
        }

        private TemplateSchema markAsPublished(TemplateSchema schema) {
            TemplateVersioning versioning = normalizeVersioning(schema.versioning(), schema.status());
            String now = LocalDateTime.now().toString();
            int effectiveVersion = Math.max(versioning.effectiveVersion(), versioning.draftVersion());
            return new TemplateSchema(schema.id(), schema.name(), schema.schemaVersion(), "PUBLISHED", schema.datasetCode(),
                    new TemplateVersioning(
                            versioning.baseVersion(),
                            versioning.draftVersion(),
                            Math.max(versioning.publishedVersion(), effectiveVersion),
                            effectiveVersion,
                            "published",
                            firstNonBlank(versioning.lastSavedAt(), now),
                            now),
                    schema.layout(), schema.filters(), schema.parameters(), schema.widgets(), schema.interactions());
        }

        private void validatePublishable(TemplateSchema schema) {
            if (schema.widgets().isEmpty()) {
                throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "report template requires at least one widget");
            }
            for (TemplateWidget widget : schema.widgets()) {
                if (firstNonBlank(widget.title(), "").isBlank()) {
                    throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST, "report widget title is required");
                }
                boolean hasBindingField = widget.bindings().stream().anyMatch(binding -> !firstNonBlank(binding.field(), "").isBlank());
                if (!hasBindingField) {
                    throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST,
                            "report widget binding is incomplete: " + firstNonBlank(widget.id(), "unknown"));
                }
            }
        }

        private List<Map<String, Object>> applyFilters(List<Map<String, Object>> rows, List<TemplateFilter> filters) {
            return rows.stream().filter(row -> matchesAllFilters(row, filters)).toList();
        }

        private boolean matchesAllFilters(Map<String, Object> row, List<TemplateFilter> filters) {
            for (TemplateFilter filter : filters) {
                String actual = stringify(row.get(filter.field()));
                String expected = filter.value() == null ? "" : filter.value().trim();
                switch (normalizeOperator(filter.operator())) {
                    case "contains" -> {
                        if (!actual.toLowerCase().contains(expected.toLowerCase())) {
                            return false;
                        }
                    }
                    case "not_empty" -> {
                        if (actual.isBlank()) {
                            return false;
                        }
                    }
                    default -> {
                        if (!actual.equalsIgnoreCase(expected)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private List<String> summarizeFilters(List<TemplateFilter> filters) {
            return filters.stream()
                    .map(filter -> firstNonBlank(filter.label(), filter.field()) + " " + normalizeOperator(filter.operator())
                            + ("not_empty".equals(normalizeOperator(filter.operator())) ? "" : " " + firstNonBlank(filter.value(), "")))
                    .toList();
        }

        private List<String> summarizeParameters(List<TemplateParameter> parameters, List<RuntimeParameterValue> runtimeValues) {
            Map<String, String> valueMap = runtimeValues.stream()
                    .collect(LinkedHashMap::new, (map, item) -> map.put(item.parameterCode(), item.value()), LinkedHashMap::putAll);
            List<String> summary = new ArrayList<>();
            for (TemplateParameter parameter : parameters) {
                String value = firstNonBlank(valueMap.get(parameter.code()), parameter.defaultValue(), "");
                String operator = normalizeOperator(parameter.operator());
                if ("not_empty".equals(operator)) {
                    summary.add(firstNonBlank(parameter.label(), parameter.code()) + " " + operator);
                    continue;
                }
                if (!value.isBlank()) {
                    summary.add(firstNonBlank(parameter.label(), parameter.code()) + " " + operator + " " + value);
                }
            }
            return summary;
        }

        private List<TemplateFilter> mergeFilters(TemplateSchema schema, ReportRuntimeParams runtimeParams) {
            List<TemplateFilter> merged = new ArrayList<>(schema.filters());
            Map<String, String> runtimeParameterMap = new LinkedHashMap<>();
            if (runtimeParams != null && runtimeParams.parameters() != null) {
                for (RuntimeParameterValue runtimeValue : runtimeParams.parameters()) {
                    if (runtimeValue == null || firstNonBlank(runtimeValue.parameterCode(), "").isBlank()) {
                        continue;
                    }
                    runtimeParameterMap.put(runtimeValue.parameterCode(), firstNonBlank(runtimeValue.value(), ""));
                }
            }
            for (TemplateParameter parameter : schema.parameters()) {
                String operator = normalizeOperator(parameter.operator());
                String value = firstNonBlank(runtimeParameterMap.get(parameter.code()), parameter.defaultValue(), "");
                if ("not_empty".equals(operator)) {
                    merged.add(new TemplateFilter(parameter.field(), firstNonBlank(parameter.label(), parameter.code()),
                            operator, ""));
                    continue;
                }
                if (value.isBlank()) {
                    if (parameter.required()) {
                        throw new CommonSupport.BusinessException(HttpStatus.BAD_REQUEST,
                                "missing required report parameter: " + parameter.code());
                    }
                    continue;
                }
                merged.add(new TemplateFilter(parameter.field(), firstNonBlank(parameter.label(), parameter.code()),
                        operator, value));
            }
            List<PreviewInteractionContext> interactionContexts = runtimeParams == null ? List.of() : runtimeParams.interactions();
            if (interactionContexts == null || interactionContexts.isEmpty()) {
                return merged;
            }
            Map<String, InteractionRule> interactionMap = schema.interactions().stream()
                    .collect(LinkedHashMap::new, (map, rule) -> map.put(rule.id(), rule), LinkedHashMap::putAll);
            for (PreviewInteractionContext context : interactionContexts) {
                if (context == null) {
                    continue;
                }
                InteractionRule rule = interactionMap.get(context.interactionId());
                if (rule == null) {
                    continue;
                }
                if (!Objects.equals(rule.sourceWidgetId(), context.sourceWidgetId())
                        || !Objects.equals(rule.targetWidgetId(), context.targetWidgetId())) {
                    continue;
                }
                if (!Objects.equals(rule.sourceField(), context.sourceField())
                        || !Objects.equals(rule.targetField(), context.targetField())) {
                    continue;
                }
                merged.add(new TemplateFilter(rule.targetField(), firstNonBlank(rule.name(), rule.targetField()), "eq",
                        firstNonBlank(context.value(), "")));
            }
            return merged;
        }

        private List<TemplateParameter> normalizeParameters(List<TemplateParameter> parameters) {
            List<TemplateParameter> normalized = new ArrayList<>();
            if (parameters == null) {
                return normalized;
            }
            for (int index = 0; index < parameters.size(); index++) {
                TemplateParameter parameter = parameters.get(index);
                if (parameter == null) {
                    continue;
                }
                String field = firstNonBlank(parameter.field(), "");
                if (field.isBlank()) {
                    continue;
                }
                normalized.add(new TemplateParameter(
                        firstNonBlank(parameter.id(), "param-" + (index + 1)),
                        firstNonBlank(parameter.code(), "param_" + (index + 1)),
                        firstNonBlank(parameter.label(), field),
                        field,
                        normalizeOperator(parameter.operator()),
                        firstNonBlank(parameter.defaultValue(), ""),
                        parameter.required(),
                        "string"));
            }
            return normalized;
        }

        private List<String> summarizeBindings(List<TemplateWidget> widgets) {
            return widgets.stream().map(widget -> {
                String bindingText = widget.bindings().stream()
                        .map(binding -> firstNonBlank(binding.label(), binding.role()) + "=" + firstNonBlank(binding.field(), "-"))
                        .reduce((left, right) -> left + " / " + right)
                        .orElse("-");
                return firstNonBlank(widget.title(), widget.id()) + ": " + bindingText;
            }).toList();
        }

        private List<String> summarizeInteractions(List<InteractionRule> interactions, List<TemplateWidget> widgets,
                List<PreviewInteractionContext> interactionContexts) {
            Map<String, String> widgetTitles = widgets.stream()
                    .collect(LinkedHashMap::new, (map, widget) -> map.put(widget.id(), firstNonBlank(widget.title(), widget.id())),
                            LinkedHashMap::putAll);
            Map<String, PreviewInteractionContext> activeContextMap = new LinkedHashMap<>();
            if (interactionContexts != null) {
                for (PreviewInteractionContext context : interactionContexts) {
                    if (context != null) {
                        activeContextMap.put(context.interactionId(), context);
                    }
                }
            }
            return interactions.stream()
                    .map(interaction -> firstNonBlank(interaction.name(), interaction.id()) + ": "
                            + firstNonBlank(widgetTitles.get(interaction.sourceWidgetId()), interaction.sourceWidgetId()) + " -> "
                            + firstNonBlank(widgetTitles.get(interaction.targetWidgetId()), interaction.targetWidgetId()) + " ("
                            + firstNonBlank(interaction.sourceField(), "-") + " => "
                            + firstNonBlank(interaction.targetField(), "-") + ")"
                            + (activeContextMap.containsKey(interaction.id())
                                    ? " [已应用=" + firstNonBlank(activeContextMap.get(interaction.id()).value(), "-") + "]"
                                    : ""))
                    .toList();
        }

        private List<WidgetBinding> normalizeBindings(String widgetId, String widgetType, List<WidgetBinding> bindings,
                Map<String, Object> config) {
            List<WidgetBinding> normalized = new ArrayList<>();
            if (bindings != null) {
                for (int index = 0; index < bindings.size(); index++) {
                    WidgetBinding binding = bindings.get(index);
                    if (binding == null) {
                        continue;
                    }
                    normalized.add(new WidgetBinding(
                            firstNonBlank(binding.id(), widgetId + "-binding-" + (index + 1)),
                            normalizeBindingRole(binding.role(), index),
                            firstNonBlank(binding.label(), defaultBindingLabel(widgetType, normalizeBindingRole(binding.role(), index))),
                            firstNonBlank(binding.field(), ""),
                            firstNonBlank(binding.aggregation(), "")));
                }
            }
            if (normalized.isEmpty()) {
                normalized.add(new WidgetBinding(widgetId + "-binding-1", "category", defaultBindingLabel(widgetType, "category"),
                        firstNonBlank(stringify(config.get("xField")), ""), ""));
                normalized.add(new WidgetBinding(widgetId + "-binding-2", "value", defaultBindingLabel(widgetType, "value"),
                        firstNonBlank(stringify(config.get("yField")), ""), firstNonBlank(stringify(config.get("aggregation")), "")));
            }
            return normalized;
        }

        private WidgetGrid normalizeGrid(TemplateWidget widget, Integer columns) {
            int defaultWidth = "table".equalsIgnoreCase(widget.type()) ? DEFAULT_GRID_COLUMNS
                    : Math.max(6, DEFAULT_GRID_COLUMNS / Math.max(1, columns));
            int width = clampInt(widget.grid() == null ? null : widget.grid().w(), 1, DEFAULT_GRID_COLUMNS, defaultWidth);
            int height = clampInt(widget.grid() == null ? null : widget.grid().h(), 1, 4,
                    "metric".equalsIgnoreCase(widget.type()) ? 1 : 2);
            return new WidgetGrid(0, 0, width, height);
        }

        private List<InteractionRule> normalizeInteractions(List<InteractionRule> interactions, List<TemplateWidget> widgets) {
            Map<String, String> widgetIds = widgets.stream()
                    .collect(LinkedHashMap::new, (map, widget) -> map.put(widget.id(), widget.id()), LinkedHashMap::putAll);
            List<InteractionRule> normalized = new ArrayList<>();
            if (interactions == null) {
                return normalized;
            }
            for (int index = 0; index < interactions.size(); index++) {
                InteractionRule interaction = interactions.get(index);
                if (interaction == null) {
                    continue;
                }
                if (!widgetIds.containsKey(interaction.sourceWidgetId()) || !widgetIds.containsKey(interaction.targetWidgetId())) {
                    continue;
                }
                normalized.add(new InteractionRule(
                        firstNonBlank(interaction.id(), "interaction-" + (index + 1)),
                        firstNonBlank(interaction.name(), "联动规则-" + (index + 1)),
                        interaction.sourceWidgetId(),
                        interaction.targetWidgetId(),
                        firstNonBlank(interaction.trigger(), "click"),
                        firstNonBlank(interaction.action(), "filter"),
                        firstNonBlank(interaction.sourceField(), ""),
                        firstNonBlank(interaction.targetField(), "")));
            }
            return normalized;
        }

        private String normalizeBindingRole(String role, int index) {
            String normalized = firstNonBlank(role, index == 0 ? "category" : "value").toLowerCase();
            return Objects.equals(normalized, "value") ? "value" : "category";
        }

        private String defaultBindingLabel(String widgetType, String role) {
            if (Objects.equals(role, "value")) {
                if (Objects.equals(widgetType, "metric")) {
                    return "指标字段";
                }
                if (Objects.equals(widgetType, "table")) {
                    return "辅助列字段";
                }
                return "数值字段";
            }
            if (Objects.equals(widgetType, "metric")) {
                return "主展示字段";
            }
            if (Objects.equals(widgetType, "table")) {
                return "主列字段";
            }
            if (Objects.equals(widgetType, "line") || Objects.equals(widgetType, "bar")) {
                return "维度字段";
            }
            return "分类字段";
        }

        private int clampInt(Integer value, int min, int max, int defaultValue) {
            int actual = value == null ? defaultValue : value;
            return Math.min(max, Math.max(min, actual));
        }

        private String normalizeOperator(String operator) {
            String normalized = firstNonBlank(operator, "eq").toLowerCase();
            if (!Objects.equals(normalized, "contains") && !Objects.equals(normalized, "not_empty")) {
                return "eq";
            }
            return normalized;
        }

        private String stringify(Object value) {
            return value == null ? "" : String.valueOf(value).trim();
        }

        private String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return "";
        }

        private void syncAllSchedules() throws SchedulerException {
            for (Long id : jdbcTemplate.query("select id from report_schedule where enabled = true order by id",
                    (rs, rowNum) -> rs.getLong("id"))) {
                syncSchedule(id);
            }
        }

        private void syncSchedule(Long scheduleId) {
            try {
                unschedule(scheduleId);
                ReportScheduleView schedule = getSchedule(scheduleId);
                if (!schedule.enabled()) {
                    return;
                }
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put("scheduleId", scheduleId);
                JobDetail jobDetail = JobBuilder.newJob(ReportScheduleQuartzJob.class)
                        .withIdentity(jobKey(scheduleId))
                        .usingJobData(jobDataMap)
                        .build();
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey(scheduleId))
                        .forJob(jobDetail)
                        .withSchedule(CronScheduleBuilder.cronSchedule(schedule.cronExpression())
                                .withMisfireHandlingInstructionDoNothing())
                        .build();
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (SchedulerException exception) {
                throw new IllegalStateException("failed to sync report schedule", exception);
            }
        }

        private void unschedule(Long scheduleId) throws SchedulerException {
            JobKey jobKey = jobKey(scheduleId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        }

        private LocalDateTime resolveNextRun(Long scheduleId) {
            try {
                Trigger trigger = scheduler.getTrigger(triggerKey(scheduleId));
                if (trigger == null || trigger.getNextFireTime() == null) {
                    return null;
                }
                return LocalDateTime.ofInstant(trigger.getNextFireTime().toInstant(), ZoneId.systemDefault());
            } catch (SchedulerException exception) {
                return null;
            }
        }

        public static JobKey jobKey(Long scheduleId) {
            return JobKey.jobKey("report-schedule-" + scheduleId, "report-schedules");
        }

        public static TriggerKey triggerKey(Long scheduleId) {
            return TriggerKey.triggerKey("report-schedule-trigger-" + scheduleId, "report-schedules");
        }
    }

    @DisallowConcurrentExecution
    public static class ReportScheduleQuartzJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                ReportService reportService = (ReportService) context.getScheduler().getContext().get("reportService");
                Long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");
                reportService.runScheduledJob(scheduleId);
            } catch (Exception exception) {
                throw new JobExecutionException(exception);
            }
        }
    }

    @RestController
    @RequestMapping("/api/reports")
    public static class ReportController {
        private final ReportService reportService;

        public ReportController(ReportService reportService) {
            this.reportService = reportService;
        }

        @GetMapping("/templates")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<List<ReportTemplateView>> templates() {
            return CommonSupport.success(reportService.listTemplates());
        }

        @PostMapping("/templates")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportTemplateView> createTemplate(
                @RequestBody @Validated ReportTemplateRequest request) {
            return CommonSupport.success(reportService.createTemplate(request));
        }

        @GetMapping("/templates/{id}")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportTemplateView> template(@PathVariable Long id) {
            return CommonSupport.success(reportService.getTemplate(id));
        }

        @PutMapping("/templates/{id}")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportTemplateView> updateTemplate(@PathVariable Long id,
                @RequestBody @Validated ReportTemplateRequest request) {
            return CommonSupport.success(reportService.updateTemplate(id, request));
        }

        @PostMapping("/templates/{id}/publish")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportTemplateView> publishTemplate(@PathVariable Long id) {
            return CommonSupport.success(reportService.publishTemplate(id));
        }

        @GetMapping("/templates/{id}/preview")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportPreview> preview(@PathVariable Long id) {
            return CommonSupport.success(reportService.preview(id));
        }

        @PostMapping("/templates/{id}/preview")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportPreview> previewWithInteractions(@PathVariable Long id,
                @RequestBody(required = false) ReportPreviewRequest request) {
            return CommonSupport.success(reportService.preview(id, request));
        }

        @GetMapping("/templates/{id}/export/pdf")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) throws Exception {
            byte[] body = reportService.exportPdf(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(body);
        }

        @PostMapping("/templates/{id}/export/pdf")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public ResponseEntity<byte[]> exportPdf(@PathVariable Long id,
                @RequestBody(required = false) ReportRuntimeParams runtimeParams) throws Exception {
            byte[] body = reportService.exportPdf(id, runtimeParams);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(body);
        }

        @GetMapping("/templates/{id}/export/excel")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) throws Exception {
            byte[] body = reportService.exportExcel(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(body);
        }

        @PostMapping("/templates/{id}/export/excel")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public ResponseEntity<byte[]> exportExcel(@PathVariable Long id,
                @RequestBody(required = false) ReportRuntimeParams runtimeParams) throws Exception {
            byte[] body = reportService.exportExcel(id, runtimeParams);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(body);
        }

        @GetMapping("/schedules")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<List<ReportScheduleView>> schedules() {
            return CommonSupport.success(reportService.listSchedules());
        }

        @PostMapping("/schedules")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportScheduleView> createSchedule(
                @RequestBody @Validated ReportScheduleRequest request) {
            return CommonSupport.success(reportService.createSchedule(request));
        }

        @PutMapping("/schedules/{id}")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportScheduleView> updateSchedule(@PathVariable Long id,
                @RequestBody @Validated ReportScheduleRequest request) {
            return CommonSupport.success(reportService.updateSchedule(id, request));
        }

        @PostMapping("/schedules/{id}/run")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportActionResult> runSchedule(@PathVariable Long id) throws Exception {
            return CommonSupport.success(reportService.runSchedule(id));
        }

        @GetMapping("/snapshots")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<List<ReportSnapshotView>> snapshots() {
            return CommonSupport.success(reportService.listSnapshots());
        }

        @GetMapping("/notifications")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<List<ReportNotificationView>> notifications() {
            return CommonSupport.success(reportService.listNotifications());
        }
    }
}
