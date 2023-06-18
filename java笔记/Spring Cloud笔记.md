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


### 修改Feign日志级别

#### 首先开启默认日志

```yaml
logging:
  level:
    com:
      he_zhw: debug
```

![image-20230617165524916](images\image-20230617165524916.png)

- #### 基于配置文件

  - #### 全局生效

    ```yaml
    feign:
      client:
        config:
          default: # 这里用default就是全局配置，如果是写服务名称，则是针对某个微服务的配置
            loggerLevel: FULL # 日志级别
    ```

    ![image-20230617165739484](images\image-20230617165739484.png)

  - #### 局部生效

    ```yaml
    feign:
      client:
        config:
          SpringBootDemo2: # 这里用default就是全局配置，如果是写服务名称，则是针对某个微服务的配置
            loggerLevel: FULL # 日志级别
    ```

    ![image-20230617170039285](images\image-20230617170039285.png)

- #### 基于代码

  **增加一个Feign配置类**

  ```java
  package com.he_zhw.springdemo.config;
  
  import feign.Logger;
  import org.springframework.context.annotation.Bean;
  
  public class FeignClientConfiguration {
      @Bean
      public Logger.Level feignLogLevel() {
          return Logger.Level.FULL;
      }
  }
  ```

  ![image-20230617171158097](images\image-20230617171158097.png)

  - #### 全局生效

    **在配置类的EnableFeignClients注解中引入自定义配置类**

    ![image-20230617171421911](images\image-20230617171421911.png)

  - #### 局部生效

    **在Feign接口类的FeignClient注解上引入自定义配置类**

    ![image-20230617172126307](images\image-20230617172126307.png)

### Feign的性能优化

![image-20230617172335745](images\image-20230617172335745.png)

- #### 方式一:使用连接池代替默认的URLConnection

  **本例中使用HttpClient代替URLConnection**

  - #### **引入HttpClient依赖**

    ```xml
    <dependency>
        <groupId>io.github.openfeign</groupId>
        <artifactId>feign-httpclient</artifactId>
    </dependency>
    ```

    ![image-20230617173239160](images\image-20230617173239160.png)

  - #### **配置连接池**

    ```yaml
    feign:
      httpclient:
        enabled: true # 开feign对HttpClient的支持
        max-connections: 200 # 最大的连接数
        max-connections-per-route: 50 # 每个路径的最大连接数
    ```

    ![image-20230617174013403](images\image-20230617174013403.png)

  - 

- #### 方式二:日志级别，最好用basic或none

### Feign的最佳实践

- #### 方式一:给消费者的FeignClient和提供者的controller定义统一的父接口作为标准

- #### 方式二:将FeignClient抽取为独立模块，并且把接口有关的POJO、默认的Feign配置都放到这个模块中，提供给所有消费者使用

## 网关Gateway

### 网关功能:

- #### 身份认证和权限校验

- #### 服务路由、负载均衡

- #### 请求限流

### Gateway环境搭建

#### 新建一个微服务

![image-20230617175444063](images\image-20230617175444063.png)

![image-20230617175556664](images\image-20230617175556664.png)

![image-20230617180453965](images\image-20230617180453965.png)



#### 引入Gateway与Nacos服务注册发现依赖

```xml
 <!--nacos服务发现依赖-->
 <dependency>
     <groupId>com.alibaba.cloud</groupId>
     <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
 </dependency>
 <!--Gateway依赖-->
 <dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-gateway</artifactId>
 </dependency>
```

![image-20230617180108602](images\image-20230617180108602.png)

### 引入负载均衡依赖

- #### 低版本SpringCloud内置了Ribbon负载均衡，可不用引用此依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

![image-20230617183629417](images\image-20230617183629417.png)

#### 编写路由配置及nacos地址

```yaml
server:
  port: 10010 #网关端口
spring:
  application:
    name: GatewayDemo #服务名
  profiles:
    active: dev #当前环境名称
  cloud:
    nacos:
      server-addr: localhost:8848
      discovery:
        cluster-name: SH

    gateway:
      routes:
        - id: Spring-BootDemo2 # 路由id，自定义，只要唯一即可
          uri: lb://SpringBootDemo2 # 路由的目标地址 Lb就是负载均衡，后面跟服务名称uri
          predicates: #路由言，也就是判断请求是否符合路由规则的条件
            - Path=/demo2/** # 这个是按照路径匹配，只要以/demo2/开头就符合要求
```

![image-20230617183905153](images\image-20230617183905153.png)

### 访问测试

- #### 访问demo2能成功

  ![image-20230617184057134](images\image-20230617184057134.png)

- #### 访问FeignDemo失败

  ![image-20230617184222339](images\image-20230617184222339.png)

### 断言工厂

官方参考文档链接[Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/4.0.6/reference/html/#gateway-request-predicates-factories)

![image-20230618095232154](images\image-20230618095232154.png)

### 网关过滤器(GatewayFilter)

**GatewayFilter是网关中提供的一种过滤器,可以对进入网关的请求和微服务返回的响应做处理**

官方参考文档链接[Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/4.0.6/reference/html/#gatewayfilter-factories)

![image-20230618100147374](images\image-20230618100147374.png)

#### 示例

- #### 给访问SpringBootDemo2的请求添加请求头

  ```yaml
      gateway:
        routes:
          - id: Spring-BootDemo2 # 路由id，自定义，只要唯一即可
            uri: lb://SpringBootDemo2 # 路由的目标地址 Lb就是负载均衡，后面跟服务名称uri
            predicates: #路由言，也就是判断请求是否符合路由规则的条件
              - Path=/demo2/** # 这个是按照路径匹配，只要以/demo2/开头就符合要求
            filters:
            - AddRequestHeader=X-Request-red, blue
  ```

  ![image-20230618103116838](images\image-20230618103116838.png)

- #### 接收请求头

  ![image-20230618103315591](images\image-20230618103315591.png)

- #### 测试

  ![image-20230618103430545](images\image-20230618103430545.png)

#### 默认过滤器

**对网关代理的服务都生效**

```yaml
server:
  port: 10010 #网关端口
spring:
  application:
    name: GatewayDemo #服务名
  profiles:
    active: dev #当前环境名称
  cloud:
    nacos:
      server-addr: localhost:8848
      discovery:
        cluster-name: SH

    gateway:
      routes:
        - id: Spring-BootDemo2 # 路由id，自定义，只要唯一即可
          uri: lb://SpringBootDemo2 # 路由的目标地址 Lb就是负载均衡，后面跟服务名称uri
          predicates: #路由言，也就是判断请求是否符合路由规则的条件
            - Path=/demo2/** # 这个是按照路径匹配，只要以/demo2/开头就符合要求
#          filters:
#          - AddRequestHeader=X-Request-red, blue
        - id: Spring-BootDemo1 # 路由id，自定义，只要唯一即可
          uri: lb://SpringBootDemo1 # 路由的目标地址 Lb就是负载均衡，后面跟服务名称uri
          predicates: #路由言，也就是判断请求是否符合路由规则的条件
            - Path=/demo1/** # 这个是按照路径匹配，只要以/demo2/开头就符合要求
      default-filters:
        - AddRequestHeader=X-Request-red, blue
```

![image-20230618104900756](images\image-20230618104900756.png)

#### 全局过滤器(GlobalFilter)

**与默认过滤器的功能一致,也是处理一切进入网关的请求和微服务响应,区别在于默认过滤器通过配置定义，处理逻辑是固定的。而全局过滤器的逻辑需要自己写代码实现**

![image-20230618111026019](images\image-20230618111026019.png)

- #### 编写实现类实现GlobalFilter接口

  ```java
  package com.he_zhw.springdemo;
  
  import org.springframework.cloud.gateway.filter.GatewayFilterChain;
  import org.springframework.cloud.gateway.filter.GlobalFilter;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.server.reactive.ServerHttpRequest;
  import org.springframework.stereotype.Component;
  import org.springframework.util.MultiValueMap;
  import org.springframework.web.server.ServerWebExchange;
  import reactor.core.publisher.Mono;
  
  @Order(1)
  @Component
  public class AuthorizeFilter implements GlobalFilter {
      @Override
      public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
          // 1.获取请求参数
          ServerHttpRequest request = exchange.getRequest();
          MultiValueMap<String,String> params = request.getQueryParams();//
          // 2.获取参数中的 authorization 参数
          String auth = params.getFirst("authorization");
          //3.判断参数值是否等于 admin
          if ("admin".equals(auth)) {
              // 4.是，放行
              return chain.filter(exchange);
          }
          // 5.否，拦截
          // 5.1.设置状态码
          exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
          // 5.2.拦截请求
          return exchange.getResponse().setComplete();
      }
  }
  ```

  ![image-20230618110526001](images\image-20230618110526001.png)

- #### 测试

  - 当传的参数authorization为admin时可以访问

    ![image-20230618110837397](images\image-20230618110837397.png)

  - 当传的参数authorization不为admin时返回401

    ![image-20230618110937769](images\image-20230618110937769.png)

  

#### 过滤器的执行顺序

![image-20230618111439233](images\image-20230618111439233.png)

#### 跨域问题处理

![image-20230618111823854](images\image-20230618111823854.png)

## Docker

### Docker的安装

Docker CE 支持 64 位版本 CentOS 7，并且要求内核版本不低于 3.10， CentOS 7 满足最低内核的要求，所以我们在CentOS 7安装Docker。

#### 卸载Docker(防止和之前的版本冲突)

```shell
yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-selinux \
                  docker-engine-selinux \
                  docker-engine \
                  docker-ce
```

没有匹配说明之前未安装过Docker

![image-20230618163500970](images\image-20230618163500970.png)

#### 安装docker

首先需要虚拟机联网，安装yum工具

```sh
yum install -y yum-utils \
           device-mapper-persistent-data \
           lvm2 --skip-broken
```

然后更新本地镜像源：

```shell
# 设置docker镜像源
yum-config-manager \
    --add-repo \
    https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
    
sed -i 's/download.docker.com/mirrors.aliyun.com\/docker-ce/g' /etc/yum.repos.d/docker-ce.repo

yum makecache fast
```

然后输入以下命令安装Docker：

```shell
yum install -y docker-ce
```

docker-ce为社区免费版本。

#### 启动docker

Docker应用需要用到各种端口，逐一去修改防火墙设置。因此学习过程中直接关闭防火墙！

```sh
# 关闭
systemctl stop firewalld
# 禁止开机启动防火墙
systemctl disable firewalld
```

通过命令启动docker：

```sh
systemctl start docker  # 启动docker服务

systemctl stop docker  # 停止docker服务

systemctl restart docker  # 重启docker服务
```



然后输入命令，可以查看docker版本：

```sh
docker -v
```

如图：

![image-20230618170516026](images\image-20230618170516026.png)

#### 配置镜像加速

docker官方镜像仓库网速较差，我们需要设置国内镜像服务：

参考阿里云的镜像加速文档：https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors

![image-20230618171037230](images\image-20230618171037230.png)

```sh
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://chwdftpk.mirror.aliyuncs.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

![image-20230618171008141](images\image-20230618171008141.png)

验证是否写入

```sh
cat /etc/docker/daemon.json
```

![image-20230618171325883](images\image-20230618171325883.png)

### Docker的基本操作

[Docker Hub Container Image Library | App Containerization](https://hub.docker.com/)

- 拉取镜像

  ```sh
  docker pull nginx #默认拉取最新的版本
  
  docker pull nginx:stable-alpine3.17-slim #拉取版本号为stable-alpine3.17-slim的版本
  ```

  ![image-20230618172508887](images\image-20230618172508887.png)

  ![image-20230618173154087](images\image-20230618173154087.png)

- 查看镜像

  ```sh
  docker images
  ```

  ![image-20230618173228495](images\image-20230618173228495.png)

- 将镜像导出到本地

  ```sh
  docker save -o nginx.tar nginx:latest
  ```

  本地就会出现对应的文件

  ![image-20230618173744086](images\image-20230618173744086.png)

- 删除服务中的镜像

  ```sh
  docker rmi nginx:latest #根据镜像名和版本号删除
  
  docker rmi 605c77e624dd #根据镜像ID删除
  ```

  ![image-20230618174043327](images\image-20230618174043327.png)

  再根据docker images命令查询时就查不出镜像

  ![image-20230618174142641](images\image-20230618174142641.png)

- 从本地重新加载镜像

  ```sh
  docker load -i nginx.tar
  ```

  ![image-20230618174409862](images\image-20230618174409862.png)

  再根据docker images命令查询时可查出镜像

  ![image-20230618174521538](images\image-20230618174521538.png)



#### 运行容器

![image-20230618174921713](images\image-20230618174921713.png)

- 在官网有对应的命令执行

  ![image-20230618175322229](images\image-20230618175322229.png)

- 示例

  ![image-20230618175634836](images\image-20230618175634836.png)

  ```sh
  docker run --name nginxtest -p 80:80 -d nginx
  ```

  ![image-20230618175833065](images\image-20230618175833065.png)

- 查看容器的执行状态

  ```sh
  docker ps
  ```

  ![image-20230618180238055](images\image-20230618180238055.png)

- 访问服务器的80端口

  ![image-20230618180345440](images\image-20230618180345440.png)

- 查看nginx日志

  ```sh
  docker logs nginxtest
  
  docker logs -f nginxtest  #持续跟踪日志
  ```

  ![image-20230618180621136](images\image-20230618180621136.png)

- 进入容器内部

  ![image-20230618181040901](images\image-20230618181040901.png)

  

  ```sh
  docker exec -it nginxtest bash
  ```

  - 切换到nginx容器静态页面的位置

    ```sh
    cd /usr/share/nginx/html
    ```

  - 将欢迎页的Welcome to nginx!替换成中文

    ```sh
    sed -i 's#Welcome to nginx#欢迎来到nginx#g' index.html
    sed -i 's#<head>#<head><meta charset="utf-8">#g' index.html
    ```

    ![image-20230618182607847](images\image-20230618182607847.png)

    

- 退出容器

- ```
  exit
  ```

  ![image-20230618183113448](images\image-20230618183113448.png)

- 停止容器

  ```sh
  docker stop nginxtest
  ```

  查看容器状态

  ```sh
  docker ps -a #docker ps只能查看在运行的容器,查看停止的容器需要添加-a参数
  ```

  ![image-20230618183214788](images\image-20230618183214788.png)

- 重启容器

  ```sh
  docker start nginxtest
  ```

  

- 删除容器

  ```sh
  docker rm nginxtest #不能删除正在运行的容器
  ```

  ![image-20230618193955189](images\image-20230618193955189.png)

  ```sh
  docker rm -f nginxtest #强制删除正在运行的容器
  ```

  ![image-20230618194620626](images\image-20230618194620626.png)



### Docker数据卷

**数据卷 (volume)是一个虚拟目录，指向宿主机文件系统中的某个目录**

#### 数据卷基本命令

![image-20230618200007430](images\image-20230618200007430.png)

- #### 创建一个名为html的数据卷

  ```sh
  docker volume create html
  ```

  

- 列出现有的数据卷

  ```sh
  docker volume ls
  ```

  ![image-20230618200501069](images\image-20230618200501069.png)

- 显示数据卷的具体信息

  ```sh
  docker volume inspect html
  ```

  ![image-20230618200811296](images\image-20230618200811296.png)

- 删除未使用的数据卷

  ```sh
  docker volume prune
  ```

  

- 删除指定的数据卷

  ```sh
  docker volume rm html
  ```

  

#### 数据卷挂载

![image-20230618201236904](images\image-20230618201236904.png)

- #### 启动时将nginx的静态目录挂载到html数据卷

```sh
docker run --name nginxtest -v html:/usr/share/nginx/html -p 80:80 -d nginx
```

![image-20230618203626577](images\image-20230618203626577.png)

- #### 目录挂载

  ![ ](images\image-20230618205635163.png)
  
  - ##### 将mysql镜像包上传到temp目录下
  
    ![image-20230618215108002](images\image-20230618215108002.png)
  
  - ##### 将镜像包加载为镜像
  
    ```sh
    docker load -i mysql.tar
    ```
  
    ![image-20230618215954444](images\image-20230618215954444.png)
  
  - ##### 在temp目录下创建mysql/data文件夹，用于存放数据文件
  
    ```sh
    mkdir -p mysql/data
    ```
  
  - ##### 在temp目录下创建mysql/conf文件夹，用于存放mysql配置文件
  
    ```sh
    mkdir -p mysql/conf
    ```
  
  - ##### 将hmy.cnf配置文件上传到conf目录下
  
    ![image-20230618220509195](images\image-20230618220509195.png)
  
  - ##### 运行容器
  
    ```sh
    docker run \
    --name mysql \
    -e MYSQL_ROOT_PASSWORD=root \
    -p 3306:3306 \
    -v /tmp/mysql/conf/hmy.cnf:/etc/mysql/conf.d/hmy.cnf \
    -v /tmp/mysql/data:/var/lib/mysql \
    -d \
    mysql:5.7.25
    ```
  
    ![image-20230618221937563](images\image-20230618221937563.png)
  
  - ##### 执行上述命令后data文件夹会生成数据文件
  
    ![image-20230618223310732](images\image-20230618223310732.png)
  
  - ##### 连接测试
  
    ![image-20230618223510378](images\image-20230618223510378.png)



