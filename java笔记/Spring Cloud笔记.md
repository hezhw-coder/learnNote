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

### Nacos服务发现

#### Nacos服务注册

- ##### 下载及安装Nacos服务端

  下载地址https://github.com/alibaba/nacos/releases/tag/2.2.3

  解压后在bin目录下使用cmd命令启动

  ```powershell
  .\startup.cmd -m standalone
  ```

  ![image-20230605114315335](images\image-20230605114315335.png)

  根据提示按住Ctrl键点击链接打开Nacos管理界面

  ![image-20230605114537991](images\image-20230605114537991.png)

  ​          ![image-20230605114645038](images\image-20230605114645038.png)

  

- ##### 父工程配置spring-cloud-alibaba的依赖描述

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

- ##### 将demo1与demo2的Eureka客户端的依赖注释掉,引用Nacos的客户端依赖

  ```xml
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  ```

  ![image-20230605115313934](images\image-20230605115313934.png)

  

- ##### 注释掉Eureka的配置,添加Nacos的配置

  ```yaml
  spring:
    application:
      name: SpringBootDemo2
    cloud:
      nacos:
        server-addr: 192.168.214.1:8848
  ```

  ![image-20230605115941435](images\image-20230605115941435.png)

- ##### 将替换成Nacos注册中心后会提示找不到服务提示

  ![image-20230605145035718](images\image-20230605145035718.png)

  ##### 原因与解决方式:由于Ribbon负载均衡组件已停止更新,新版本的SpringCloud已不集成,需引用loadbalancer组件

  ![image-20230605143901155](images\image-20230605143901155.png)

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
  ```

  

#### Nacos服务分级存储

- ##### 添加集群属性

```yaml
spring:
  application:
    name: SpringBootDemo2
  cloud:
    nacos:
      server-addr: localhost:8848
      discovery:
        cluster-name: SH
```

![image-20230613182521930](images\image-20230613182521930.png)

- ##### 开启Nacos负载均衡规则

  在demo1开启负载均衡规则,则优先访问同集群的服务

  ```yaml
  server:
    port: 8080
  
  spring:
    application:
      name: SpringBootDemo1
    cloud:
      nacos:
        server-addr: localhost:8848
        discovery:
          cluster-name: SH
      loadbalancer:
        nacos:
          enabled: true
  ```

  ![image-20230613185157097](images\image-20230613185157097.png)

#### Nacos根据权重负载均衡

- ##### 在服务列表的操作列点击详情进入集群配置界面

  ![image-20230614173822905](images\image-20230614173822905.png)

- ##### 在操作列点击编辑

  ![image-20230614173957181](images\image-20230614173957181.png)

#### Nacos环境隔离

- ##### 创建命名空间

  ![image-20230614175944924](images\image-20230614175944924.png)

  ![image-20230614180015484](images\image-20230614180015484.png)

- ##### 将demo1分配到dev命名空间下

  ![image-20230614180352005](images\image-20230614180352005.png)

- ##### 因demo1与demo2的服务不在同一个命名空间内,demo1远程调用demo2会报错

  ![image-20230614180623494](images\image-20230614180623494.png)

  ![image-20230614180651683](images\image-20230614180651683.png)

  ![image-20230614180839981](images\image-20230614180839981.png)

  

### Nacos配置管理

#### 统一配置管理

- ##### 在Nacos配置列表添加配置

  ![image-20230614183941883](images\image-20230614183941883.png)

  ![image-20230614184253384](images\image-20230614184253384.png)

  ![image-20230614184321044](images\image-20230614184321044.png)

- ##### 引入Nacos的配置管理客户端依赖

  ```xml
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
  </dependency>
  ```

  ![image-20230614183211466](images\image-20230614183211466.png)

- ##### 创建个bootstrap.yml文件,并将Nacos的相关配置都移植过来

  **在项目启动时bootstrap.yml优先于application.yml**

  ![image-20230614184824073](images\image-20230614184824073.png)

- ##### Spring Cloud 新版本默认将 Bootstrap 禁用，需要将 spring-cloud-[starter](https://so.csdn.net/so/search?q=starter&spm=1001.2101.3001.7020)-bootstrap 依赖引入到工程中,否则会报错

  ![image-20230614185328720](images\image-20230614185328720.png)

- ##### 引入相关依赖启动成功

  ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-bootstrap</artifactId>
   </dependency>
  ```

  ![image-20230615090607520](images\image-20230615090607520.png)

- ##### 在接口中添加测试

  ![image-20230615090836039](images\image-20230615090836039.png)

  

#### 配置热更新

- ##### 方式一:在Value注入的类上加上@RefreshScope注解

  ![image-20230615091415744](images\image-20230615091415744.png)

- ##### 方式二:使用ConfigurationProperties注解

  ![image-20230615092600462](images\image-20230615092600462.png)

  ![image-20230615092633649](images\image-20230615092633649.png)

  

#### 配置共享

- ##### 在Nacos中创建一个服务名加后缀名的后缀

  ![image-20230616161253446](D:\GitHubRepositories\learnNote\java笔记\images\image-20230616161253446.png)

  ![image-20230616161432639](images\image-20230616161432639.png)

- ##### 配置优先级

  **原则:当远程配置(Nacos的配置)和application.yml的配置一样时,使用远程配置,当远程配置中带环境的配置与共享配置的属性一样时,使用带环境的配置**

#### 搭建Nacos集群

[集群部署说明 (nacos.io)](https://nacos.io/zh-cn/docs/v2/guide/admin/cluster-mode-quick-start.html)



## Feign远程调用

- ### 引入Feign依赖

  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-openfeign</artifactId>
  </dependency>
  ```

  ![image-20230616170442456](images\image-20230616170442456.png)

- ### 在需要使用Feign的服务启动类中添加EnableFeignClients注解

  ![image-20230616171017841](images\image-20230616171017841.png)

- ### 编写Feign客户端

  - #### 在demo1定义接口

    ![image-20230616174318179](images\image-20230616174318179.png)

  - ### 在demo2定义方法

    ![image-20230616174548142](images\image-20230616174548142.png)

  - ### 在demo1中调用

    ![image-20230616174805633](images\image-20230616174805633.png)

    

- 

