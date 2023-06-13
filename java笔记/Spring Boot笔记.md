# Spring Boot笔记

**注:Spring Boot3.0开始最低支持jdk17,已经不支持jdk8,本文档使用的是2.7.12的版本,使用的是jdk1.8**

## idea下使用Maven构建SpringBoot项目

### 新建项目时选择Spring Initializr,填写好对应的项目信息之后点击下一步

![image-20230522151809804](images\image-20230522151809804.png)

### 选择对应的Spring Boot版本，需要开发什么类型的项目就勾选左侧列表的功能

![image-20230522152504984](images\image-20230522152504984.png)

## 使用阿里云的镜像构建

将官网的url改成阿里云的url，http://start.aliyun.com

![image-20230522155510232](images\image-20230522155510232.png)

### 创建测试接口进行测试

**注:创建的controller必须在引导类SpringbootdemoApplication同级目录或者子目录下**

![image-20230522160536974](images\image-20230522160536974.png)

#### 引用配置文件的属性

![image-20230522161919004](images\image-20230522161919004.png)

![image-20230522161952682](images\image-20230522161952682.png)

#### 这配置文件里引用其它配置的值

![image-20230522163010455](images\image-20230522163010455.png)

![image-20230522163036111](images\image-20230522163036111.png)

## Spring Boot整合MybatisPlus

#### Maven引用mybatis plus整合包

```xml
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.3.1</version>
        </dependency>
```

#### 引用mysql包

```xml
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
```

#### 导入lombok包

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

#### 配置数据源

```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://192.168.214.128:3306/bookstores
spring.datasource.username=root
spring.datasource.password=root
```

![image-20230522181541496](images\image-20230522181541496.png)

#### 创建实体类

```java
package com.example.springbootdemo.domain;

import lombok.Data;

@Data
public class Student {

    private  String id;
    private  String name;
    private  String chinese;
    private  String english;
    private  String math;
}
```

![image-20230522181712914](images\image-20230522181712914.png)

#### 创建mapper接口

- 第一种创建mapper方式

```java
package com.example.springbootdemo.dao;

import com.example.springbootdemo.domain.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface Studentdao {
    @Select("SELECT *FROM student WHERE id=#{id}")
    public Student QueryById(Integer id);
}
```

![image-20230522181805011](images\image-20230522181805011.png)

- 第二种创建mapper方式(继承BaseMapper)

  ```java
  package com.example.springbootdemo.dao;
  
  import com.baomidou.mybatisplus.core.mapper.BaseMapper;
  import com.example.springbootdemo.domain.Student;
  import org.apache.ibatis.annotations.Mapper;
  
  
  @Mapper
  public interface Studentdao extends BaseMapper<Student> {
  
  }
  ```

  ![image-20230524171710017](images\image-20230524171710017.png)

  注意:继承BaseMapper默认查询的表名是BaseMapper的泛型类型的全小写，如果类名和数据库的表明不一致会报错

  ![image-20230524172438487](images\image-20230524172438487.png)

  解决方式:在实体类上添加注解指定表名

  ![image-20230524172918871](images\image-20230524172918871.png)

  

#### 测试

![image-20230522181902078](images\image-20230522181902078.png)

### MybatisPlus开启执行日志

```java
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
```

![image-20230524173434090](images\image-20230524173434090.png)

### MybatisPlus分页

#### 配置拦截器

```java
package com.example.springbootdemo.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MPConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }
}
```

![image-20230524175013829](images\image-20230524175013829.png)

#### 测试代码

查询student表第二页,每页两条数据

```java
    @GetMapping("StudentTest")
    public Student studentTest() {
        IPage page = new Page<Student>(2, 2);
        page = studentdao.selectPage(page, null);
        List records = page.getRecords();
        Student student = (Student)records.get(1);
        return student;
    }
```

![image-20230524175312761](images\image-20230524175312761.png)