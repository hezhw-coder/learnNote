package com.example.springmvcdemo.config;



import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class MyAnnotationConfigWebApplicationContext extends AnnotationConfigWebApplicationContext {
    public MyAnnotationConfigWebApplicationContext(){
        super.register(SpringMVCConfig.class);//注册SpingMvcConfig配置类
    }
}
