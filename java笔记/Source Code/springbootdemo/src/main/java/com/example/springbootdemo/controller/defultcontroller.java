package com.example.springbootdemo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springbootdemo.dao.Studentdao;
import com.example.springbootdemo.domain.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("demo")
public class defultcontroller {

    //    @Value("${demeo.test1}")
    private String serverPort;

    @GetMapping("test")
    public String test() {
        System.out.println(serverPort);
        return "test";
    }

    @Autowired
    private Studentdao studentdao;

    @GetMapping("StudentTest")
    public Student studentTest() {
        IPage page = new Page<Student>(2, 2);
        page = studentdao.selectPage(page, null);
        List records = page.getRecords();
        Student student = (Student)records.get(1);
        log.info("213145646");
        System.out.println(log);
        return student;
    }
}
