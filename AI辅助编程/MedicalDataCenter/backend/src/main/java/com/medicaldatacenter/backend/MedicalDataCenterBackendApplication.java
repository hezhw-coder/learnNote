package com.medicaldatacenter.backend;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class MedicalDataCenterBackendApplication {

    @Value("${app.export.dir}")
    private String exportDir;

    public static void main(String[] args) {
        SpringApplication.run(MedicalDataCenterBackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureFolders() throws Exception {
        Files.createDirectories(Path.of(exportDir));
    }
}
