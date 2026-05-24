package com.medicaldatacenter.backend.warehouse;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medicaldatacenter.backend.common.CommonSupport;

public final class WarehouseModule {

    private WarehouseModule() {
    }

    public record DatasetField(String fieldCode, String fieldName, String fieldType) {
    }

    public record DatasetView(Long id, String code, String name, String description, List<DatasetField> fields,
            List<Map<String, Object>> sampleRows) {
    }

    @Service
    public static class DatasetService {
        private final JdbcTemplate jdbcTemplate;

        public DatasetService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public List<DatasetView> listDatasets() {
            return jdbcTemplate.query("select id, code, name, description from meta_dataset order by id",
                    (rs, rowNum) -> new DatasetView(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description"),
                            fields(rs.getString("code")),
                            sampleRows(rs.getString("code"))));
        }

        public List<Map<String, Object>> sampleRows(String code) {
            if (!"cdm_patient".equals(code)) {
                return List.of();
            }
            return jdbcTemplate.queryForList(
                    "select patient_code, patient_name, gender, birth_date from cdm_patient order by id limit 20");
        }

        private List<DatasetField> fields(String code) {
            return jdbcTemplate.query(
                    "select field_code, field_name, field_type from meta_dataset_field where dataset_code = ? order by id",
                    (rs, rowNum) -> new DatasetField(rs.getString("field_code"), rs.getString("field_name"),
                            rs.getString("field_type")),
                    code);
        }
    }

    @RestController
    @RequestMapping("/api/datasets")
    public static class DatasetController {
        private final DatasetService datasetService;

        public DatasetController(DatasetService datasetService) {
            this.datasetService = datasetService;
        }

        @GetMapping
        @PreAuthorize("hasAuthority('DATASET_VIEW')")
        public CommonSupport.ApiResponse<List<DatasetView>> list() {
            return CommonSupport.success(datasetService.listDatasets());
        }

        @GetMapping("/sample")
        @PreAuthorize("hasAuthority('DATASET_VIEW')")
        public CommonSupport.ApiResponse<List<Map<String, Object>>> sample(@RequestParam String code) {
            return CommonSupport.success(datasetService.sampleRows(code));
        }
    }
}
