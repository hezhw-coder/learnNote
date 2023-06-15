package com.example.springmvcdemo.init;

import com.example.springmvcdemo.config.SpringMVCConfig;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyAbstractAnnotationConfigDispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    //配置Spring核心配置类方法
    protected Class<?>[] getRootConfigClasses() {
        return new Class[0];
    }

    @Override
    //配置SpringMvc配置类
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{SpringMVCConfig.class};
    }

    @Override
    //配置前端映射器路径
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}
