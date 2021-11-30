# WebApi整合Autofac

实例代码路径**<u>.net笔记\Code\WebApi4Autofac\WebApi4Autofac</u>**

**以下例子使用的.NET SDK版本为6.0.100,开发工具使用Microsoft Visual Studio Enterprise 2022 (64 位) 版本 17.0.1**

**Autofac版本为6.3.0**

## 安装Autofac包

```powershell
Install-Package Autofac -Version 6.3.0
```

## Autofac工厂替换默认工厂

**安装Autofac.Extensions.DependencyInjection包**

```powershell
Install-Package Autofac.Extensions.DependencyInjection -Version 7.2.0
```

- 引用命名空间using Autofac.Extensions.DependencyInjection;

```C#
builder.Host.UseServiceProviderFactory(new AutofacServiceProviderFactory());//使用Autofac工厂替换默认工厂
```

![image-20211128230634153](images\image-20211128230634153.png)

- 引用命名空间using Autofac;
- 调用ConfigureContainer注册服务

```C#
builder.Host.ConfigureContainer<ContainerBuilder>((HostBuilderContext, ContainerBuilder) => {
    ContainerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
});//调用ConfigureContainer注册服务
```

![image-20211128233735428](images\image-20211128233735428.png)

在通过控制器的构造函数注入服务

```C#
private readonly ITestServiceA _testServiceA;

public WeatherForecastController(ILogger<WeatherForecastController> logger, ITestServiceA testServiceA)
{
    _logger = logger;
   _testServiceA = testServiceA;
}
```

![image-20211128234128393](images\image-20211128234128393.png)

## 使用Autofac配置文件

nuget安装Autofac.Configuration配置扩展包(例子使用6.0.0版本)

```powershell
Install-Package Autofac.Configuration -Version 6.0.0
```

创建`AutofacJson.json`文件配置

```json
{
  "components": [
    {
      "type": "WebApi4Autofac.BLL.TestServiceAimpl, WebApi4Autofac",
      "services": [
        {
          "type": "WebApi4Autofac.IBLL.ITestServiceA,WebApi4Autofac"
        }
      ],
      "instanceScope": "Per-Lifetime-Scope" //生命周期
    }
  ]
}
```

![image-20211128232053793](images\image-20211128232053793.png)

- 调用ConfigureContainer注册服务

```C#
builder.Host.ConfigureContainer<ContainerBuilder>((HostBuilderContext, ContainerBuilder) => {
    // 实例化ConfigurationBuilder.
    var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
    //使用Microsoft.Extensions.Configuration.Json读取json配置文件
    config.AddJsonFile("Conf/AutofacJson.json");

    // Register the ConfigurationModule with Autofac.
    var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
    ContainerBuilder.RegisterModule(module);
});//调用ConfigureContainer注册服务
```

- 在控制器的构造函数进行注入

![](images\image-20211128234128393.png)

## WebApi中的过滤器(Filters)

### ActionFilter

- 新建一个类继承ActionFilterAttribute,并重写里面的方法(建议重写异步的方法，例子中使用的是同步方法)

  ![image-20211130224708895](images\image-20211130224708895.png)

**全局ActionFilter**

在添加控制器的中间件注册全局过滤器

```C#
builder.Services.AddControllers(configure =>
{
    configure.Filters.Add<WebApi4Autofac.Filters.MyActionFilter>();

}).AddNewtonsoftJson(options =>
{
    options.SerializerSettings.DateFormatString = "yyyy-MM-dd HH:mm:ss";
});
```

![image-20211130224921405](images\image-20211130224921405.png)
