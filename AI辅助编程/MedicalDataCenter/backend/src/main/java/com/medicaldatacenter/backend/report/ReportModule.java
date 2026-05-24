package com.medicaldatacenter.backend.report;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    public record ReportPreview(String datasetCode, List<Map<String, Object>> rows) {
    }

    public record ReportScheduleRequest(@NotNull Long templateId, @NotBlank String cronExpression, boolean enabled,
            String channel) {
    }

    public record ReportScheduleView(Long id, Long templateId, String cronExpression, boolean enabled, String channel) {
    }

    @Service
    @Validated
    public static class ReportService {
        private final JdbcTemplate jdbcTemplate;
        private final WarehouseModule.DatasetService datasetService;
        private final ObjectMapper objectMapper;
        private final SystemModule.UserAccountService userAccountService;
        private final Path exportDir;

        public ReportService(JdbcTemplate jdbcTemplate, WarehouseModule.DatasetService datasetService,
                ObjectMapper objectMapper, SystemModule.UserAccountService userAccountService,
                @Value("${app.export.dir}") String exportDir) {
            this.jdbcTemplate = jdbcTemplate;
            this.datasetService = datasetService;
            this.objectMapper = objectMapper;
            this.userAccountService = userAccountService;
            this.exportDir = Path.of(exportDir);
        }

        public List<ReportTemplateView> listTemplates() {
            return jdbcTemplate.query(
                    "select id, name, dataset_code, design_json, status, created_at from report_template order by id",
                    (rs, rowNum) -> new ReportTemplateView(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("dataset_code"),
                            rs.getString("design_json"),
                            rs.getString("status"),
                            rs.getObject("created_at", LocalDateTime.class)));
        }

        public ReportTemplateView createTemplate(ReportTemplateRequest request) {
            jdbcTemplate.update("insert into report_template(name, dataset_code, design_json, status) values (?, ?, ?, ?)",
                    request.name(), request.datasetCode(), request.designJson(), "PUBLISHED");
            Long id = jdbcTemplate.queryForObject("select max(id) from report_template", Long.class);
            userAccountService.audit(userAccountService.currentActor(), "REPORT_CREATE", "report_template",
                    String.valueOf(id), request.name());
            return getTemplate(id);
        }

        public ReportTemplateView updateTemplate(Long id, ReportTemplateRequest request) {
            getTemplate(id);
            jdbcTemplate.update(
                    "update report_template set name = ?, dataset_code = ?, design_json = ?, status = ?, updated_at = current_timestamp where id = ?",
                    request.name(), request.datasetCode(), request.designJson(), "PUBLISHED", id);
            userAccountService.audit(userAccountService.currentActor(), "REPORT_UPDATE", "report_template",
                    String.valueOf(id), request.name());
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
            ReportTemplateView template = getTemplate(templateId);
            return new ReportPreview(template.datasetCode(), datasetService.sampleRows(template.datasetCode()));
        }

        public byte[] exportPdf(Long templateId) throws Exception {
            ReportTemplateView template = getTemplate(templateId);
            ReportPreview preview = preview(templateId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph(template.name()));
            PdfPTable table = new PdfPTable(Math.max(1, preview.rows().isEmpty() ? 1 : preview.rows().get(0).size()));
            if (!preview.rows().isEmpty()) {
                preview.rows().get(0).keySet().forEach(table::addCell);
                for (Map<String, Object> row : preview.rows()) {
                    row.values().forEach(value -> table.addCell(String.valueOf(value)));
                }
            } else {
                table.addCell("No data");
            }
            document.add(table);
            document.close();
            persistSnapshot(templateId, outputStream.toByteArray(), "pdf", preview.rows());
            return outputStream.toByteArray();
        }

        public byte[] exportExcel(Long templateId) throws Exception {
            ReportTemplateView template = getTemplate(templateId);
            ReportPreview preview = preview(templateId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                var sheet = workbook.createSheet("report");
                if (!preview.rows().isEmpty()) {
                    Row header = sheet.createRow(0);
                    List<String> columns = preview.rows().get(0).keySet().stream().toList();
                    for (int index = 0; index < columns.size(); index++) {
                        header.createCell(index).setCellValue(columns.get(index));
                    }
                    for (int rowIndex = 0; rowIndex < preview.rows().size(); rowIndex++) {
                        Row row = sheet.createRow(rowIndex + 1);
                        Map<String, Object> item = preview.rows().get(rowIndex);
                        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                            row.createCell(columnIndex).setCellValue(String.valueOf(item.get(columns.get(columnIndex))));
                        }
                    }
                }
                workbook.write(outputStream);
            }
            persistSnapshot(templateId, outputStream.toByteArray(), "xlsx", preview.rows());
            return outputStream.toByteArray();
        }

        public List<ReportScheduleView> listSchedules() {
            return jdbcTemplate.query(
                    "select id, template_id, cron_expression, enabled, channel from report_schedule order by id",
                    (rs, rowNum) -> new ReportScheduleView(
                            rs.getLong("id"),
                            rs.getLong("template_id"),
                            rs.getString("cron_expression"),
                            rs.getBoolean("enabled"),
                            rs.getString("channel")));
        }

        public ReportScheduleView createSchedule(ReportScheduleRequest request) {
            jdbcTemplate.update(
                    "insert into report_schedule(template_id, cron_expression, enabled, channel) values (?, ?, ?, ?)",
                    request.templateId(), request.cronExpression(), request.enabled(),
                    request.channel() == null || request.channel().isBlank() ? "IN_APP" : request.channel());
            Long id = jdbcTemplate.queryForObject("select max(id) from report_schedule", Long.class);
            return jdbcTemplate.query(
                    "select id, template_id, cron_expression, enabled, channel from report_schedule where id = ?",
                    (rs, rowNum) -> new ReportScheduleView(
                            rs.getLong("id"),
                            rs.getLong("template_id"),
                            rs.getString("cron_expression"),
                            rs.getBoolean("enabled"),
                            rs.getString("channel")),
                    id).get(0);
        }

        private void persistSnapshot(Long templateId, byte[] content, String extension, List<Map<String, Object>> rows)
                throws Exception {
            Files.createDirectories(exportDir);
            Path target = exportDir.resolve("report-" + templateId + "-" + System.currentTimeMillis() + "." + extension);
            Files.write(target, content);
            jdbcTemplate.update("insert into report_snapshot(template_id, snapshot_json, export_path) values (?, ?, ?)",
                    templateId, objectMapper.writeValueAsString(rows), target.toAbsolutePath().toString());
            jdbcTemplate.update("insert into notify_message(title, content, recipient) values (?, ?, ?)",
                    "report ready", "report template " + templateId + " exported", userAccountService.currentActor());
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

        @GetMapping("/templates/{id}/preview")
        @PreAuthorize("hasAuthority('REPORT_MANAGE')")
        public CommonSupport.ApiResponse<ReportPreview> preview(@PathVariable Long id) {
            return CommonSupport.success(reportService.preview(id));
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
    }
}
