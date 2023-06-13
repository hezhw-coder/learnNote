# Spring Cloud笔记

## 笔记中涉及到的各个版本号

| 技术名称               | 版本号         |
| ---------------------- | -------------- |
| **springboot**         | **2.7.12**     |
| **springcloud**        | **2021.0.7**   |
| **springcloudalibaba** | **2021.0.5.0** |

### Spring Cloud Alibaba版本对照说明

[版本说明 · alibaba/spring-cloud-alibaba Wiki (github.com)](https://github.com/alibaba/spring-cloud-alibaba/wiki/版本说明)

## 构建maven父工程

### 创建一个maven的父工程

![image-20230604103116602](images\image-20230604103116602.png)

### 将工程的Src文件删除

![image-20230604103320855](images\image-20230604103320855.png)

### 打包类型改成pom

![image-20230604103848127](images\image-20230604103848127.png)

### 设定父工程的springboot与Springcloud的版本

![image-20230604104357376](images\image-20230604104357376.png)

#### 对应的`pom.xml`文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>SpringCloudDemo</artifactId>
    <version>1.0-SNAPSHOT</version>

    <packaging>pom</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.12</version>
        <relativePath/>
    </parent>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
        <spring-cloud.version>2021.0.7</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- springCloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>
```

## 构建微服务子模块

### 创建测试Springboot的Maven子模块

![image-20230604105214928](images\image-20230604105214928.png)

![image-20230604105329649](images\image-20230604105329649.png)

### 引入Springbootweb环境

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
```

![image-20230604105654829](images\image-20230604105654829.png)

#### 最终的工程结构

SpringBoot启动类

![image-20230604111831185](images\image-20230604111831185.png)

控制器层

![image-20230604112244142](images\image-20230604112244142.png)

## 服务间的远程调用 

#### 复制一个demo2,启动端口及访问路径不一样

![image-20230604113011443](images\image-20230604113011443.png)

在demo1中注入RestTemplate类型,并在控制层调用demo2,将返回的值打印在控制台

![image-20230604113500449](images\image-20230604113500449.png)

## Eureka注册中心

#### 创建Eureka服务

- #### 创建继承于父工程的mavenmaven模块

引入Eureka服务坐标

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
    </dependencies>
```

- #### 编写配置文件

  ```yaml
  server:
    port: 8085
  
  spring:
    application:
      name: eurekaserver
  
  eureka:
    client:
      service-url:
        defaultZone: http://localhost:8085/eureka/
  
  ```

  ![image-20230604162949936](images\image-20230604162949936.png)

- 编写启动类并加上`@EnableEurekaServer`注解

  ![image-20230604163206715](images\image-20230604163206715.png)

- 启动成功后访问界面如下

  ![image-20230604163303971](images\image-20230604163303971.png)

#### 注册Eureka客户端服务

- #### 引入Eureka客户端依赖

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  ```

- #### 编写配置文件

  ​         ![image-20230604165552324](images\image-20230604165552324.png)

  ![image-20230604164546036](images\image-20230604164546036.png)

  

- #### 配置完成启动后Eureka的注册界面

  ![image-20230604165441624](images\image-20230604165441624.png)

  

  

#### 服务发现

- 将demo1调用demo2的访问ip及端口替换成demo2的服务名称

  ![image-20230604165822209](images\image-20230604165822209.png)

- #### 在RestTemplate的bean上加入负载均衡的注解

  ![image-20230604170007378](images\image-20230604170007378.png)

  ![image-20230604171934160](images\image-20230604171934160.png)

## Nacos

### Nacos服务注册中心

- #### 下载及安装Nacos服务端

  下载地址https://github.com/alibaba/nacos/releases/tag/2.2.3

  解压后在bin目录下使用cmd命令启动

  ```powershell
  .\startup.cmd -m standalone
  ```

  ![image-20230605114315335](images\image-20230605114315335.png)

  根据提示按住Ctrl键点击链接打开Nacos管理界面

  ![image-20230605114537991](images\image-20230605114537991.png)

  ​          ![image-20230605114645038](images\image-20230605114645038.png)

  

- #### 父工程配置spring-cloud-alibaba的依赖描述

  ```xml
              <dependency>
                  <groupId>com.alibaba.cloud</groupId>
                  <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                  <version>${spring.cloud.alibaba.version}</version>
                  <type>pom</type>
                  <scope>import</scope>
              </dependency>
  ```

  ![image-20230605100507456](images\image-20230605100507456.png)

- #### 将demo1与demo2的Eureka客户端的依赖注释掉,引用Nacos的客户端依赖

  ```xml
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  ```

  ![image-20230605115313934](images\image-20230605115313934.png)

  

- #### 注释掉Eureka的配置,添加Nacos的配置

  ```yaml
  spring:
    application:
      name: SpringBootDemo2
    cloud:
      nacos:
        server-addr: 192.168.214.1:8848
  ```

  ![image-20230605115941435](images\image-20230605115941435.png)

- #### 将替换成Nacos注册中心后会提示找不到服务提示

  ![image-20230605145035718](images\image-20230605145035718.png)

  #### 原因与解决方式:由于Ribbon负载均衡组件已停止更新,新版本的SpringCloud已不集成,需引用loadbalancer组件

  ![image-20230605143901155](images\image-20230605143901155.png)

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
  ```

  

