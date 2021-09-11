package com.he_zhw.Aspect;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 基于注解的切面类
 */
@Component
@Aspect
public class MyAspectByAnnotation {

    /*公共切入点*/
    @Pointcut("execution(* com.he_zhw.serviceImpl.*.*(..))")
    public void  pointCut(){

    }

    @Before("pointCut()")
    public void Before(){
        System.out.println("666");
    }

    @AfterReturning("pointCut()")
    public  void After(){
        System.out.println("7779");
    }
}
