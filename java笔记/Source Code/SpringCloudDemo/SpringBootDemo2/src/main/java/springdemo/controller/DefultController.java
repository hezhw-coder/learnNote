package springdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefultController {

    @GetMapping("demo2")
    public String demo1(){
        System.out.println("777");
        return "777";
    }
}
