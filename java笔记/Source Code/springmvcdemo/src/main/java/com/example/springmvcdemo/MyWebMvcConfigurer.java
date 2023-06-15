package com.example.springmvcdemo;

import com.example.springmvcdemo.Interceptor.MyInterceptor1;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class MyWebMvcConfigurer implements WebMvcConfigurer {
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        /**
         * 等价于替换以下xml配置
         *     <!--配置支持静态资源的访问-->
         *     <mvc:default-servlet-handler/>
         */
        configurer.enable();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /**
         * 等价于替换以下xml配置
         *     <mvc:interceptors>
         *         <mvc:interceptor>
         *             <mvc:mapping path="/**"/><!--/**是对多级路径的拦截,/*只能拦截医技路径-->
         *             <bean class="com.example.springmvcdemo.Interceptor.MyInterceptor1"/>
         *         </mvc:interceptor>
         *     </mvc:interceptors>
         */
        registry.addInterceptor(new MyInterceptor1()).addPathPatterns("/**");//添加拦截器
    }
}
