package com.example.springmvcdemo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DefultController {

    @GetMapping("/Show")
    public String Show(){
        System.out.println("show1......");
        return "helo";
    }


    @PostMapping("/FileUpload")
    @ResponseBody
    public String FileUpload(@RequestBody MultipartFile myFile){
        System.out.println(myFile);
        return "";
    }

    @PostMapping("JsonTest")
    @ResponseBody
    public String  JsonTest(@RequestBody Student student){
        System.out.println(student);
        return "";
    }
}
