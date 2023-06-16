package com.he_zhw.springdemo.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("SpringBootDemo2")
public interface ClientDemo {
    @GetMapping("FeignDemo/{id}")
    String FeignDemo(@PathVariable("id") Integer id);
}
