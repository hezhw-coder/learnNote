package springdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefultController {

    @GetMapping("demo2")
    public String demo1(){
        System.out.println("777");
        return "777";
    }


    @GetMapping("FeignDemo/{id}")
    public String FeignDemo(@PathVariable("id") Integer id){
        System.out.println(id);
        return Integer.toString(id);
    }
}
