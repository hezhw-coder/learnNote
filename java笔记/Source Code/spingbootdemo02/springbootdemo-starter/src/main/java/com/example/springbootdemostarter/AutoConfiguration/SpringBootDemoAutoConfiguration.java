package com.example.springbootdemostarter.AutoConfiguration;

import com.example.springbootdemostarter.service.SpringBootdemoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingClass("com.example.springbootdemostarter.domain.Mouse")
public class SpringBootDemoAutoConfiguration {

    @Bean
    public SpringBootdemoService springBootdemoService(){
        return new SpringBootdemoService();
    }
}
