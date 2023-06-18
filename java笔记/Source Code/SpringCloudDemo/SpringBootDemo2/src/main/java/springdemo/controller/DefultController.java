package springdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefultController {

    @GetMapping("demo2")
    public String demo1(){
        System.out.println("777");
        return "777";
    }


    @GetMapping("demo2/{id}")
    public String demo3(@PathVariable("id") Integer id,@RequestHeader(value = "X-Request-red",required = false) String colorStr){
        System.out.println(colorStr);
        return "777";
    }


    @GetMapping("FeignDemo/{id}")
    public String FeignDemo(@PathVariable("id") Integer id){
        System.out.println(id);
        return Integer.toString(id);
    }
}
