package com.example.springmvcdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ComponentScan("com.example.springmvcdemo")
@EnableWebMvc
public class SpringMVCConfig {

    /**
     * 配置文件上传解析器
     * @return
     */
    @Bean("multipartResolver")
    public CommonsMultipartResolver MultipartResolver(){
        CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        return commonsMultipartResolver;
    }
}
