package com.he_zhw.springamqpdemo.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefultController {

    private final RabbitTemplate rabbitTemplate;

    public DefultController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("publishMessage")
    public String demo1() {
        String queueName ="simple.queue";
        String message = "hello, spring amqp!";
        rabbitTemplate.convertAndSend(queueName,message);
        return "1";
    }
}
