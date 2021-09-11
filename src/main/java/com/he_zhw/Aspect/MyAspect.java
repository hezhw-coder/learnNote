package com.he_zhw.Aspect;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class MyAspect implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        System.out.println("666");
        Object obj = methodInvocation.proceed();
        System.out.println("777");
        return  obj;
    }
}
