package com.example.spingbootdemo03.controller;

import com.example.springbootdemostarter.service.SpringBootdemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("Default")
public class DefaultController {
@Autowired
private SpringBootdemoService springBootdemoService;

    @GetMapping("test")
    public  String Test(){
        springBootdemoService.sayHello();
        return "test";
    }
}
