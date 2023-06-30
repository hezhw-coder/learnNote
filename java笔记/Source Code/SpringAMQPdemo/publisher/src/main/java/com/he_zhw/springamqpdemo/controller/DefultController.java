package com.he_zhw.springamqpdemo.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("workQueuePublish")
    public String demo2() throws InterruptedException {
        String queueName ="simple.queue";
        String message = "hello, spring Message__";
        for (int i = 0; i < 50; i++) {
            rabbitTemplate.convertAndSend(queueName,message+i);
            Thread.sleep(20);
        }
        return "1";
    }

    @GetMapping("fanoutQueue2Publish")
    public String demo3()  {
        // 交换机名称
        String exchangeName = "FanoutDemo";
        String message = "hello, spring Message";
        rabbitTemplate.convertAndSend(exchangeName,"",message);
        return "1";
    }

    @GetMapping("DirectQueuePublish/{key}")
    public String demo4(@PathVariable("key") String key)  {
        // 交换机名称
        String exchangeName = "DirectDemo";
        String message = "hello, spring Message";
        rabbitTemplate.convertAndSend(exchangeName,key,message);
        return "1";
    }


    @GetMapping("MessageConverterPublish")
    public String demo5()  {
        String queueName ="simple.queue";
        Map<String,Object> message=new HashMap<String,Object>();
        message.put("name","he_zhw");
        message.put("age","18");
        rabbitTemplate.convertAndSend(queueName,message);
        return "1";
    }
}
