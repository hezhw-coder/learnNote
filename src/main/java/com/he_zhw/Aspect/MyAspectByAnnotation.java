package com.he_zhw.Aspect;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 基于注解的切面类
 */
@Component
@Aspect
public class MyAspectByAnnotation {

    @Before("execution(* com.he_zhw.serviceImpl.*.*(..))")
    public void Before(){
        System.out.println("666");
    }

    @AfterReturning("execution(* com.he_zhw.serviceImpl.*.*(..))")
    public  void After(){
        System.out.println("7778");
    }
}
