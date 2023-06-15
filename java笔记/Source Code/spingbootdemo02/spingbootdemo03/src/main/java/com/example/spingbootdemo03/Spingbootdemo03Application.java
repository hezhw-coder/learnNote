package com.example.spingbootdemo03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Spingbootdemo03Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Spingbootdemo03Application.class, args);
    }

}
