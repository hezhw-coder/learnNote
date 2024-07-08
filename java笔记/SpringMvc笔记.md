# [SpringMvc笔记]()

**笔记用的环境是Tomcat8,因此用的是5.3.27版本,6.0版本的Springmvc与Tomcat8不兼容**

执行流程

![image-20230514173313809](images\image-20230514173313809.png)

## 引入对应Maven坐标

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.3.27</version>
</dependency>
```

## 添加springmvc配置文件 [springMVC.xml](C:\Users\he_zhw\IdeaProjects\springmvcdemo\src\main\resources\springMVC.xml) 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="
        http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">
    <context:component-scan base-package="com.example.springmvcdemo"/><!--配置包扫描-->
</beans>
```

![image-20230506154912054](images\image-20230506154912054.png)

## 配置web.xml文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
<servlet>
    <servlet-name>springMVC</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>

    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:springMVC.xml</param-value>
    </init-param>
</servlet>
    <servlet-mapping>
        <servlet-name>springMVC</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```

![image-20230506155123875](images\image-20230506155123875.png)

## 添加控制器类及测试方法

```java
package com.example.springmvcdemo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DefultController {

    @GetMapping("/Show")
    public String Show(){
        System.out.println("show1......");
        return "/index.jsp";
    }
}
```

![image-20230506155515704](images\image-20230506155515704.png)

访问地址是服务地址加上自定义路径，即http://localhost:8080/springmvcdemo_war_exploded/Show

![image-20230506155630994](images\image-20230506155630994.png)

![image-20230506155657201](images\image-20230506155657201.png)

## 接收文件上传功能

### ~~Maven导入FileUpload相关jar包(这种方式会造成Maven报错,暂时不清楚原因)~~

```xml
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.4</version>
</dependency>
```

### 改成将commons-fileupload的jar包以及其依赖放在Tomcat的lib目录下

![image-20230514103950899](images\image-20230514103950899.png)

### 配置文件上传解析器

```xml
    <!--文件上传解析器 id必须为multipartResolver-->
    <bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
        <property name="defaultEncoding" value="UTF-8"/><!--文件格式编码 默认是ISO8859-1-->
        <property name="maxUploadSizePerFile" value="1048567"/><!--上传每个文件限制的大小 单位字节-->
        <property name="maxUploadSize" value="3145728"/><!--上传文件的总大小-->
        <property name="maxInMemorySize" value="1048576"/><!--上传文件的缓存大小-->
    </bean>
```

![image-20230514104153291](images\image-20230514104153291.png)

### 控制器接收代码

```java
    @PostMapping("/FileUpload")
    @ResponseBody
    public String FileUpload(@RequestBody MultipartFile myFile){
        System.out.println(myFile);
        return "";
    }
```

![image-20230514104438916](images\image-20230514104438916.png)

## 访问静态资源

增加配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="
        http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/mvc
        http://www.springframework.org/schema/mvc/spring-mvc.xsd">
    <context:component-scan base-package="com.example.springmvcdemo"/><!--配置包扫描-->

    <!--文件上传解析器 id必须为multipartResolver-->
    <bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
        <property name="defaultEncoding" value="UTF-8"/><!--文件格式编码 默认是ISO8859-1-->
        <property name="maxUploadSizePerFile" value="1048567"/><!--上传每个文件限制的大小 单位字节-->
        <property name="maxUploadSize" value="3145728"/><!--上传文件的总大小-->
        <property name="maxInMemorySize" value="1048576"/><!--上传文件的缓存大小-->
    </bean>

    <!--配置支持静态资源的访问-->
    <mvc:default-servlet-handler/>
</beans>
```

![image-20230514110219156](images\image-20230514110219156.png)

### 配置了静态资源访问后需要手动配置映射器

注意:映射器要配置在静态资源之前

```xml
<!--配置了静态资源访问后需要手动配置映射器-->
    <bean class="org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping"/>
```

![image-20230514114315374](images\image-20230514114315374.png)



## 手动配置消息转换器支持接收json转JavaBean

引入jackson databind坐标

```xml
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.0</version>
        </dependency>
```

### 配置消息转换器

```xml
   <!--配置消息转换器-->
    <bean class="org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter">
        <property name="messageConverters">
            <list>
                <bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"/>
            </list>
        </property>
    </bean>
```

![image-20230514114501904](images\image-20230514114501904.png)

### 测试代码

```java
    @PostMapping("JsonTest")
    @ResponseBody
    public String  JsonTest(@RequestBody Student student){
        System.out.println(student);
        return "";
    }
```

![image-20230514114624849](images\image-20230514114624849.png)

### 当发送请求时，框架会自动将JSON的值自动注入到参数中

![image-20230514114911363](images\image-20230514114911363.png)

使用<mvc:annotation-driven/>标签就不用手动配置映射器和解析器

```xml
<mvc:annotation-driven/>
```

![image-20230514115428112](images\image-20230514115428112.png)

## SpringMVC拦截器

### 编写自定义类实现HandlerInterceptor接口

![image-20230514154329731](images\image-20230514154329731.png)

### 配置拦截器

```xml
    <!--配置拦截器-->
    <mvc:interceptors>
        <mvc:interceptor>
            <mvc:mapping path="/**"/><!--/**是对多级路径的拦截,/*只能拦截医技路径-->
            <bean class="com.example.springmvcdemo.Interceptor.MyInterceptor1"/>
        </mvc:interceptor>
    </mvc:interceptors>
```

### 多个拦截器的执行顺序

![image-20230514155322727](images\image-20230514155322727.png)

## SpringMVC纯注解开发

### springMVC.xml转化为配置类

- #### 创建SpringMVCConfig配置类

```java
package com.example.springmvcdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ComponentScan("com.example.springmvcdemo")
@EnableWebMvc
public class SpringMVCConfig {

    /**
     * 配置文件上传解析器
     * @return
     */
    @Bean("multipartResolver")
    public CommonsMultipartResolver MultipartResolver(){
        CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        return commonsMultipartResolver;
    }
}

```

![image-20230514162139767](images\image-20230514162139767.png)

- #### 新增MyWebMvcConfigurer实现WebMvcConfigurer接口

```java
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
```

![image-20230514161724648](images\image-20230514161724648.png)

- ### 自定义MyAnnotationConfigWebApplicationContext继承AnnotationConfigWebApplicationContext类

  ```java
  package com.example.springmvcdemo.config;
  
  
  
  import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
  
  public class MyAnnotationConfigWebApplicationContext extends AnnotationConfigWebApplicationContext {
      public MyAnnotationConfigWebApplicationContext(){
          super.register(SpringMVCConfig.class);//注册SpingMvcConfig配置类
      }
  }
  ```

  ![image-20230514170223244](images\image-20230514170223244.png)

- 修改Web.xml配置

  ```xml
      <init-param>
          <param-name>contextClass</param-name>
          <param-value>com.example.springmvcdemo.config.MyAnnotationConfigWebApplicationContext</param-value>
      </init-param>
  ```

  ![image-20230514170312923](images\image-20230514170312923.png)

### SpringMvc去除Web.xml

- #### 编写自定义类实现

  ```java
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
  
  ```

  ![image-20230514172709213](images\image-20230514172709213.png)

  

## 报错处理

- .使用spring-webmvc6.1.10版本需要至少jdk17才能编译,tomcat10才能部署

- 运行服务时报一下错误

  ![image-20240627220445532](images\image-20240627220445532.png)

  解决方案,引用tomcat-api

  ```xml
  <dependency>
      <groupId>org.apache.tomcat</groupId>
      <artifactId>tomcat-api</artifactId>
      <version>10.1.25</version>
  </dependency>
  ```

  ![image-20240627220639703](images\image-20240627220639703.png)

- Tomcat启动日志出现乱码

  ![image-20240627220909863](images\image-20240627220909863.png)

  解决方式:修改Tomcat目录下日志配置文件的编码参数

  ![image-20240627221057553](images\image-20240627221057553.png)

  

