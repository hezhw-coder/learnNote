# MyBatis

官方文档连接:[mybatis – MyBatis 3 | 入门](https://mybatis.org/mybatis-3/zh/getting-started.html)

## `maven`引用`mybatis`核心`jar`包

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.11</version>
</dependency>
```

## maven引用oracle驱动包

```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
    <version>19.17.0.0</version>
</dependency>
```

## 配置主配置文件`SqlMapConfig.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="oracle.jdbc.driver.OracleDriver"/>
                <property name="url" value="jdbc:oracle:thin:@192.168.18.195:1521:yitihuatest"/>
                <property name="username" value="hit_app"/>
                <property name="password" value="hit_app#2022"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="Config/employee.xml"/>
<!--        <mapper resource="Config/mapper/StudentMapper.xml"/>-->
    </mappers>
</configuration>
```

![image-20221211180132923](images\image-20221211180132923.png)

## 添加实体配置文件`Config/employee.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="test">
    
    <select id="findEmployeeById" parameterType="java.lang.String" resultType="comdemo.employee">
        SELECT *FROM com_employee t WHERE t.empl_code=#{empl_code}
    </select>
</mapper>
```

![image-20221211175925947](images\image-20221211175925947.png)

## 创建对应pojo类

```java
package com.demo;

public class employee {
    private  String empl_code;
    private  String empl_name;
    @Override
    public String toString() {
        return "employee{" +
                "empl_code='" + empl_code + '\'' +
                ", empl_name='" + empl_name + '\'' +
                '}';
    }

    public String getEmpl_code() {
        return empl_code;
    }

    public void setEmpl_code(String empl_code) {
        this.empl_code = empl_code;
    }

    public String getEmpl_name() {
        return empl_name;
    }

    public void setEmpl_name(String empl_name) {
        this.empl_name = empl_name;
    }
}
```

![image-20221211180900246](images\image-20221211180900246.png)

## 测试代码

```java
package com;

import com.demo.employee;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class employeeTest {
 @Test
 public void myTest() throws Exception {
  String resource = "SqlMapConfig.xml";
  InputStream resourceAsStream = Resources.getResourceAsStream(resource);

  SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);

  SqlSession sqlSession = sqlSessionFactory.openSession(true);

  employee o = sqlSession.selectOne("test.findEmployeeById", "008888");

  System.out.println(o);
     sqlSession.close();
 }
}
```

![image-20221211181217232](images\image-20221211181217232.png)

### oracle下字符集报错处理

![image-20221211195954129](images\image-20221211195954129.png)

- `maven`引入`orai18n`包

  ```xml
  <dependency>
      <groupId>com.oracle.database.nls</groupId>
      <artifactId>orai18n</artifactId>
      <version>19.17.0.0</version>
  </dependency>
  ```

  ![image-20221211200744970](images\image-20221211200744970.png)

## 基于mapper代理的开发

- 定义一个接口

  ```java
  package com.demo.service;
  
  import com.demo.employee;
  
  public interface EmployeeService {
      employee findEmployeeByCode(String emp_code);
  
  }
  ```

  ![image-20221211210502071](images\image-20221211210502071.png)

- 新增`Config/mapper/employeeMapper.xml`配置文件,并在主配置配置映射

  ![image-20221211211247022](images\image-20221211211247022.png)

  ![image-20221211211906352](images\image-20221211211906352.png)

### 测试代码

```java
package com.demo.service;

import com.demo.employee;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeServiceTest {
    @Test
    public void Test() throws IOException {
        String resource = "SqlMapConfig.xml";
        InputStream resourceAsStream = Resources.getResourceAsStream(resource);

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);

        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        EmployeeService employeeService = sqlSession.getMapper(EmployeeService.class);

        employee employee = employeeService.findEmployeeByCode("008888");
        System.out.println(employee);
        sqlSession.close();
    }
}
```

![image-20221211212208524](images\image-20221211212208524.png)

## 增加sql日志

- 增加log4j配置文件

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <Configuration xmlns="http://logging.apache.org/log4j/2.0/config">
  
      <Appenders>
          <Console name="stdout" target="SYSTEM_OUT">
              <PatternLayout pattern="%5level [%t] - %msg%n"/>
          </Console>
      </Appenders>
  
      <Loggers>
          <Root level="debug" >
              <AppenderRef ref="stdout"/>
          </Root>
      </Loggers>
  
  </Configuration>
  ```

  ![image-20221211214828351](images\image-20221211214828351.png)

- 测试结果

  ![image-20221211215514852](images\image-20221211215514852.png)

## 动态Sql

详细可参考文档[(44条消息) MyBatis动态SQL标签_Hiyiin的博客-CSDN博客_mybatis动态sql标签](https://blog.csdn.net/Hiyiin/article/details/125107921)

# 关系模式

## 一对一

`Department类`

![image-20221213220320792](images\image-20221213220320792.png)

`employee`与`Department`类的映射

![image-20221213220916441](images\image-20221213220916441.png)

mapper文件配置

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.demo.service.EmployeeService">
<!--    <select id="findEmployeeByCode" parameterType="java.lang.String" resultType="com.demo.employee">
&lt;!&ndash;        SELECT *FROM com_employee t WHERE t.empl_code=#{empl_code}&ndash;&gt;
        SELECT *FROM com_employee t
        <where>
            <if test="empl_code!=null and empl_code!=''">
                and t.empl_name like'%${empl_code}%'
            </if>
        </where>

    </select>-->
    <resultMap id="Employeemap" type="com.demo.employee">
        <result column="empl_code" property="empl_code"/>
        <result column="empl_name" property="empl_name"/>
        <association property="department" javaType="com.demo.Department">
            <result column="dept_code" property="dept_code"/>
            <result column="dept_name" property="dept_name"/>
        </association>
    </resultMap>
    <select id="findEmployeeByCode" parameterType="java.lang.String" resultMap="Employeemap">
        SELECT E.EMPL_CODE, E.EMPL_NAME, D.DEPT_CODE, D.DEPT_NAME
        FROM COM_EMPLOYEE E, COM_DEPARTMENT D
        WHERE E.DEPT_CODE = D.DEPT_CODE
        AND E.EMPL_CODE = #{empl_code}

    </select>
</mapper>
```

![image-20221213221919995](images\image-20221213221919995.png)

### 一对一测试结果

![image-20221213222149428](images\image-20221213222149428.png)

## 一对多

- 接口新增按科室编码查询科室信息的方法

  ![image-20221213224513270](images\image-20221213224513270.png)

- department类与employee类的关系

  ![image-20221213224651890](images\image-20221213224651890.png)

- mapper配置文件的配置

  ![image-20221213224945125](images\image-20221213224945125.png)

- 测试结果

  ![image-20221213225144512](images\image-20221213225144512.png)

## 多对多
