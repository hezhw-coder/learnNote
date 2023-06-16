package com.he_zhw.springdemo.controller;

import com.he_zhw.springdemo.clients.ClientDemo;
import com.he_zhw.springdemo.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
//@RefreshScope
public class DefultController {

    @Autowired
    private RestTemplate restTemplate;

//    @Value("${testdemo.demostring}")
//    private String testString;

    @Autowired
    private ConfigProperties configProperties;

    private ClientDemo clientDemo;

    public DefultController(ClientDemo clientDemo) {
        this.clientDemo = clientDemo;
    }

    @GetMapping("demo1")
    public String demo1(){
//        String forObject = restTemplate.getForObject("http://SpringBootDemo2/demo2", String.class);
//        System.out.println(forObject);
////        System.out.println(testString);
//        System.out.println(configProperties.getDemostring());
//        System.out.println(configProperties.getSharestring());
        String s = this.clientDemo.FeignDemo(1);
        System.out.println(s);
        return "666";
    }
}
