# Spring环境搭建

代码仓库`https://github.com/hezhw-coder/learnNote/tree/master`

`maven引用Spring包`

```xml
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>5.3.9</version>
    </dependency>
```

引用spring.jdbc包

```xml
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-jdbc</artifactId>
        <version>5.3.9</version>
    </dependency>
```

引用c3p0包

```xml
<dependency>
    <groupId>com.mchange</groupId>
    <artifactId>c3p0</artifactId>
    <version>0.9.5.5</version>
</dependency>
```

引入mysql的包

```xml
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.26</version>
    </dependency>
```

sping相关配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--数据源配置-->
    <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <property name="driverClass" value="com.mysql.jdbc.Driver"/>
        <property name="jdbcUrl" value="jdbc:mysql://192.168.182.128:3306/bookstores"/>
        <property name="user" value="root"/>
        <property name="password" value="root"/>
    </bean>
    
    <!--dao层配置-->
    <bean id="studentDao" class="com.he_zhw.DaoImpl.StudentDaoImpl">
        <property name="dataSource" ref="dataSource"/>
        <!--service层配置-->
    </bean>
       <bean id="studentService" class="com.he_zhw.serviceImpl.StudentServiceImpl">
           <property name="studentDao" ref="studentDao"/>
       </bean>
</beans>
```

# Spring基于注解

配置添加

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           https://www.springframework.org/schema/context/spring-context.xsd">

    <!--数据源配置-->
    <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <property name="driverClass" value="com.mysql.jdbc.Driver"/>
        <property name="jdbcUrl" value="jdbc:mysql://192.168.182.128:3306/bookstores"/>
        <property name="user" value="root"/>
        <property name="password" value="root"/>
    </bean>
    <context:component-scan base-package="com.he_zhw"/>
</beans>
```

代码示例

service层

```java
package com.he_zhw.serviceImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import com.he_zhw.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("studentService")
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentDao studentDao;

    @Override
    public Student getStudentById(int id) {
        return studentDao.getStudentById(id);
    }
}
```

dao层

```java
package com.he_zhw.DaoImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;


import javax.sql.DataSource;
import java.util.List;

@Repository
public class StudentDaoImpl extends JdbcDaoSupport implements StudentDao {

    @Autowired
    public void setDataSource1(DataSource dataSource){
        this.setDataSource(dataSource);
    }

    @Override
    public Student getStudentById(int id) {
        BeanPropertyRowMapper<Student> studentBeanPropertyRowMapper = new BeanPropertyRowMapper<>(Student.class);
        return this.getJdbcTemplate().queryForObject("SELECT id, NAME, chinese, english, math FROM student WHERE id=?",studentBeanPropertyRowMapper, id);
    }
}
```

# Spring AOP

## 配置文件引入命名空间及xml模式

`xmlns:aop="http://www.springframework.org/schema/aop"`

`http://www.springframework.org/schema/aop https://www.springframework.org/schema/aop/spring-aop.xsd`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:aop="http://www.springframework.org/schema/aop"
    xsi:schemaLocation="
        http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/aop https://www.springframework.org/schema/aop/spring-aop.xsd">

    <!-- bean definitions here -->

</beans>
```

Maven引入aspectj包,不导入这个包会报创建切入点错误

```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.7</version>
    <scope>runtime</scope>
</dependency>
```

定义切面类

```java
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
```

### 基于Xml的配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/aop
                           https://www.springframework.org/schema/aop/spring-aop.xsd">

    <!--数据源配置-->
    <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <property name="driverClass" value="com.mysql.jdbc.Driver"/>
        <property name="jdbcUrl" value="jdbc:mysql://192.168.182.128:3306/bookstores"/>
        <property name="user" value="root"/>
        <property name="password" value="root"/>
    </bean>

    <!--dao层配置-->
    <bean id="studentDao" class="com.he_zhw.DaoImpl.StudentDaoImpl">
        <property name="dataSource" ref="dataSource"/>
        <!--service层配置-->
    </bean>
    <bean id="studentService" class="com.he_zhw.serviceImpl.StudentServiceImpl">
        <property name="studentDao" ref="studentDao"/>
    </bean>

    <bean id="myAspect" class="com.he_zhw.Aspect.MyAspect"/><!--配置切面类-->

    <aop:config proxy-target-class="true">
        <aop:pointcut id="myPointcut" expression="execution(* com.he_zhw.serviceImpl.*.*(..))"/><!--配置切点表达式-->
        <aop:advisor advice-ref="myAspect" pointcut-ref="myPointcut"/>
    </aop:config>
</beans>
```

### 基于注解的配置

Maven引入spring-aspects及aspectjrt包

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>5.3.9</version>
</dependency>
    <dependency>
        <groupId>org.aspectj</groupId>
        <artifactId>aspectjrt</artifactId>
        <version>1.5.4</version>
    </dependency>
```

定义切面类

```java
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
```

拥有公共切入点的切面类

```java
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
```

service层

```java
package com.he_zhw.serviceImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import com.he_zhw.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("studentService")
public class StudentServiceImpl implements StudentService {

@Autowired
    private StudentDao studentDao;

    @Override
    public Student getStudentById(int id) {
        System.out.println("888");
        return studentDao.getStudentById(id);
    }
}

```

dao层

```java
package com.he_zhw.DaoImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;


import javax.sql.DataSource;
import java.util.List;

@Repository
public class StudentDaoImpl extends JdbcDaoSupport implements StudentDao {

    @Autowired
    public void setDataSource1(DataSource dataSource){
        this.setDataSource(dataSource);
    }

    @Override
    public Student getStudentById(int id) {
        BeanPropertyRowMapper<Student> studentBeanPropertyRowMapper = new BeanPropertyRowMapper<>(Student.class);
        return this.getJdbcTemplate().queryForObject("SELECT id, NAME, chinese, english, math FROM student WHERE id=?",studentBeanPropertyRowMapper, id);
    }
}
```

主配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           https://www.springframework.org/schema/context/spring-context.xsd
                           http://www.springframework.org/schema/aop
                           https://www.springframework.org/schema/aop/spring-aop.xsd">

    <!--数据源配置-->
    <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <property name="driverClass" value="com.mysql.jdbc.Driver"/>
        <property name="jdbcUrl" value="jdbc:mysql://192.168.182.128:3306/bookstores"/>
        <property name="user" value="root"/>
        <property name="password" value="root"/>
    </bean>

    <context:component-scan base-package="com.he_zhw"/>
    <aop:aspectj-autoproxy/>
</beans>
```

# Spring使用properties文件配置数据源

- resources文件夹下创建并配置properties文件

  ```properties
  jdbc.driverClass=com.mysql.jdbc.Driver
  jdbc.jdbcUrl=jdbc:mysql://192.168.72.139:3306/bookstores
  jdbc.user=root
  jdbc.password=root
  ```

- 配置spring文件

  ```xml
      <context:property-placeholder location="classpath:/jdbc.properties" />
      <!--数据源配置-->
      <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
          <property name="driverClass" value="${jdbc.driverClass}"/>
          <property name="jdbcUrl" value="${jdbc.jdbcUrl}"/>
          <property name="user" value="${jdbc.user}"/>
          <property name="password" value="${jdbc.password}"/>
      </bean>
  ```

  完整配置

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <beans xmlns="http://www.springframework.org/schema/beans"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:context="http://www.springframework.org/schema/context"
         xmlns:aop="http://www.springframework.org/schema/aop"
         xsi:schemaLocation="http://www.springframework.org/schema/beans
                             http://www.springframework.org/schema/beans/spring-beans.xsd
                             http://www.springframework.org/schema/context
                             https://www.springframework.org/schema/context/spring-context.xsd
                             http://www.springframework.org/schema/aop
                             https://www.springframework.org/schema/aop/spring-aop.xsd">
      <context:property-placeholder location="classpath:/jdbc.properties" />
      <!--数据源配置-->
      <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
          <property name="driverClass" value="${jdbc.driverClass}"/>
          <property name="jdbcUrl" value="${jdbc.jdbcUrl}"/>
          <property name="user" value="${jdbc.user}"/>
          <property name="password" value="${jdbc.password}"/>
      </bean>
  
      <context:component-scan base-package="com.he_zhw"/>
      <aop:aspectj-autoproxy/>
  </beans>
  ```



# Spring事务管理



Maven引用spring-tx包

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>5.3.9</version>
</dependency>
```

配置头引入命名空间

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/tx
                           http://www.springframework.org/schema/tx/spring-tx.xsd">
```

## 基于xml完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           https://www.springframework.org/schema/context/spring-context.xsd
                           http://www.springframework.org/schema/aop
                           https://www.springframework.org/schema/aop/spring-aop.xsd
                           http://www.springframework.org/schema/tx
                           http://www.springframework.org/schema/tx/spring-tx.xsd">
    <context:property-placeholder location="classpath:/jdbc.properties"/>
    <!--数据源配置-->
    <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <property name="driverClass" value="${jdbc.driverClass}"/>
        <property name="jdbcUrl" value="${jdbc.jdbcUrl}"/>
        <property name="user" value="${jdbc.user}"/>
        <property name="password" value="${jdbc.password}"/>
    </bean>
    <bean id="AccountDao" class="com.he_zhw.DaoImpl.AccountDaoImpl">
        <property name="dataSource" ref="dataSource"/>
    </bean>
    <bean id="AccountService" class="com.he_zhw.serviceImpl.AccountServiceImpl">
        <property name="accountDao" ref="AccountDao"/>
    </bean>

    <!--事务管理器-->
    <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>


    <!--事务详情-->
    <tx:advice id="txAdvice" transaction-manager="txManager">
        <tx:attributes>
            <!--propagation-传播机制 isolation-隔离级别-->
            <tx:method name="transfer" propagation="REQUIRED" isolation="DEFAULT" no-rollback-for="java.lang.RuntimeException"/>
        </tx:attributes>
    </tx:advice>

    <!--AOP配置-->
    <aop:config>
        <aop:advisor advice-ref="txAdvice" pointcut="execution(* com.he_zhw.serviceImpl..*.*(..))"/>
    </aop:config>
<!--    <context:component-scan base-package="com.he_zhw"/>
    <aop:aspectj-autoproxy/>-->

</beans>
```

## 基于注解完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           https://www.springframework.org/schema/context/spring-context.xsd
                           http://www.springframework.org/schema/aop
                           https://www.springframework.org/schema/aop/spring-aop.xsd
                           http://www.springframework.org/schema/tx
                           http://www.springframework.org/schema/tx/spring-tx.xsd">
    <context:property-placeholder location="classpath:/jdbc.properties"/>
    <!--数据源配置-->
    <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <property name="driverClass" value="${jdbc.driverClass}"/>
        <property name="jdbcUrl" value="${jdbc.jdbcUrl}"/>
        <property name="user" value="${jdbc.user}"/>
        <property name="password" value="${jdbc.password}"/>
    </bean>
    <!--事务管理器-->
    <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!--将事务管理器交于Spring-->
    <tx:annotation-driven transaction-manager="txManager"/>

    <!--扫描注解类-->
    <context:component-scan base-package="com.he_zhw"/>

    <!--使注解类生效-->
    <aop:aspectj-autoproxy/>

</beans>
```

# Spring完全基于注解

## 环境部署

导入Spring包

```xml
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>5.3.9</version>
    </dependency>
```

### 创建配置`SpringConfig`类

```java
package Config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.springdemo")
public class SpringConfig {
}
```

![image-20221214230428829](images\image-20221214230428829.png)

### 声明一个接口及实现类

```java
package com.springdemo.service;

public interface StudentService {
    String getStudentNameById(String stuId);
}
```

![image-20221214230749778](images\image-20221214230749778.png)

```java
package com.springdemo.impl;

import com.springdemo.service.StudentService;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceimpl implements StudentService {
    @Override
    public String getStudentNameById(String stuId) {
        return stuId;
    }
}
```

![image-20221214230845139](images\image-20221214230845139.png)

### 测试代码

```java
package com.springdemo.impl;

import Config.SpringConfig;
import com.springdemo.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class StudentServiceimplTest {
    @Test
    public void test() {
        ApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        StudentService studentService = annotationConfigApplicationContext.getBean(StudentService.class);
        String studentName = studentService.getStudentNameById("666");
        System.out.println(studentName);
    }
}
```

![image-20221214231336357](images\image-20221214231336357.png)

## 配置AOP

### 引入相关包

```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.7</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>5.3.9</version>
</dependency>
    <dependency>
        <groupId>org.aspectj</groupId>
        <artifactId>aspectjrt</artifactId>
        <version>1.5.4</version>
    </dependency>
```

### 新增切面类

```java
package com.springdemo.Aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AspectByAnnotation {

    /*公共切入点*/
    @Pointcut("execution(* com.springdemo.impl.*.*(..))")
    public void pointCut() {

    }

    @Before("pointCut()")
    public void Before() {
        System.out.println("555");
    }

    @AfterReturning("pointCut()")
    public void After() {
        System.out.println("777");
    }
}
```

![image-20221214234016498](images\image-20221214234016498.png)

### 配置允许aop设置

![image-20221215183957619](images\image-20221215183957619.png)

### 测试结果

![image-20221214234142496](images\image-20221214234142496.png)

## 整合数据库及事务

引用spring.jdbc包

```xml
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-jdbc</artifactId>
        <version>5.3.9</version>
    </dependency>
```

引用c3p0包

```xml
<dependency>
    <groupId>com.mchange</groupId>
    <artifactId>c3p0</artifactId>
    <version>0.9.5.5</version>
</dependency>
```

Maven引用spring-tx事务管理包

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>5.3.9</version>
</dependency>
```

```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
    <version>19.17.0.0</version>
</dependency>
```

### 自定义加载`properties`配置文件

#### 新增jdbc的配置文件

![image-20221215182558113](images\image-20221215182558113.png)

#### 新建自定义类继承`PropertySourcesPlaceholderConfigurer`

- 重写loadProperties方法

```java
package com.springdemo.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
/*
自定义properties文件加载,方便对数据库连接串进行加密解密
* */
@Component
public class SecretPropertyPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {


    @Override
    protected void loadProperties(Properties props) throws IOException {
        props.load(ClassLoader.getSystemResourceAsStream("jdbc.properties"));
        String password = props.getProperty("password");
        if (Objects.equals(password, "888888")) {
            props.setProperty("password","hit_app#2022");
        }
        super.loadProperties(props);
    }
}
```

![image-20221215183415487](images\image-20221215183415487.png)

### 配置DataSource数据源

```java
package Config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;

@Configuration
@ComponentScan("com.springdemo")
@EnableAspectJAutoProxy
public class SpringConfig {

    @Value("${driverClass}")
    private String driverClass;

    @Value("${jdbcUrl}")
    private String jdbcUrl;

    @Value("${user}")
    private String user;

    @Value("${password}")
    private String password;

    @Bean
    public DataSource getDataSource() throws PropertyVetoException {
        ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
        comboPooledDataSource.setDriverClass(driverClass);
        comboPooledDataSource.setJdbcUrl(jdbcUrl);
        comboPooledDataSource.setUser(user);
        comboPooledDataSource.setPassword(password);
        return comboPooledDataSource;
    }
}

```

![image-20221215184614446](images\image-20221215184614446.png)

### 配置事务管理器

![image-20221216091639779](images\image-20221216091639779.png)

![image-20221216092905614](images\image-20221216092905614.png)

### 完整配置

```java
package Config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;

@Configuration
@ComponentScan("com.springdemo")
@EnableAspectJAutoProxy
@EnableTransactionManagement
public class SpringConfig {

    @Value("${driverClass}")
    private String driverClass;

    @Value("${jdbcUrl}")
    private String jdbcUrl;

    @Value("${user}")
    private String user;

    @Value("${password}")
    private String password;

    @Bean
    public DataSource getDataSource() throws PropertyVetoException {
        ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
        comboPooledDataSource.setDriverClass(driverClass);
        comboPooledDataSource.setJdbcUrl(jdbcUrl);
        comboPooledDataSource.setUser(user);
        comboPooledDataSource.setPassword(password);
        return comboPooledDataSource;
    }

    @Bean
    public TransactionManager getDataSourceTransactionManager(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);
        return  dataSourceTransactionManager;
    }
}
```

