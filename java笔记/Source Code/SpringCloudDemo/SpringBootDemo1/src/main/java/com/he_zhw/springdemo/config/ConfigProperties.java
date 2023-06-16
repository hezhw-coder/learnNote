package com.he_zhw.springdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "testdemo")
public class ConfigProperties {
private String demostring;
private String sharestring;
}
