package com.he_zhw.serviceImpl;

import com.he_zhw.domain.Student;
import com.he_zhw.service.AccountService;
import com.he_zhw.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class StudentServiceImplTest {

    @Test
    public void studentTest(){
        ApplicationContext applicationContext=new ClassPathXmlApplicationContext("applicationconfig.xml");
//        StudentService studentService = applicationContext.getBean("studentService", StudentService.class);
//        Student student = studentService.getStudentById(1);
//        System.out.println(student);

        AccountService accountService = applicationContext.getBean("AccountService", AccountService.class);
        int transfer = accountService.transfer(1, 2, 100);
        System.out.println(transfer);
    }
}