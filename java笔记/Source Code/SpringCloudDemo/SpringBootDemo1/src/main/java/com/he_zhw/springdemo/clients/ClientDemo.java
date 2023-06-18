package com.he_zhw.springdemo.clients;

import com.he_zhw.springdemo.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "SpringBootDemo2",configuration = FeignClientConfiguration.class)
public interface ClientDemo {
    @GetMapping("FeignDemo/{id}")
    String FeignDemo(@PathVariable("id") Integer id);
}
