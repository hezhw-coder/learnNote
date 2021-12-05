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

过滤器管道

![image-20211203000113909](images\image-20211203000113909.png)

### ActionFilter

- 新建一个类继承ActionFilterAttribute,并重写里面的方法(建议重写异步的方法，例子中使用的是同步方法)

  ![image-20211130224708895](images\image-20211130224708895.png)

#### **ActionFilter的多种配置方式及特点**

- 在ControlleBa或者Action上标记特性，但是**定义的ActionFilter必须有且只有一个无参的构造函数,并且不支持依赖注入**

  ![image-20211202230814048](images\image-20211202230814048.png)

- 使用TypeFilter在ControlleBa或者Action上标记，**定义的ActionFilter可以没有无参的构造函数,支持依赖注入**

  ![image-20211202231359850](images\image-20211202231359850.png)

  ![image-20211202231538779](images\image-20211202231538779.png)

- 定义特性实现IFilterFactory接口，可以没有无参数构造函数，可以支持依赖注入。但是要使用这种方式，要在【ConfigureContainer】方法中将过滤器注册到服务

  ![image-20211202233033455](images\image-20211202233033455.png)

  ![image-20211202234324621](images\image-20211202234324621.png)

​       ![image-20211202234549738](images\image-20211202234549738.png)

#### **全局ActionFilter**

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

#### Filter生效范围和控制执行顺序

- 标记在Action上，就只对当前Action生效。
- 标记在Controller上，就对改Controller上所有的Action生效。

- 全局注册，对于当前整个项目中的Actioin都生效

**如果有三个ActionFilter，分别注册全局、控制器、Action,则执行顺序(类似中间件)如下**

先执行全局的OnActionExecuting—>控制器的OnActionExecuting—>Action上标记的OnActionExecuting—>Action上标记的OnActionExecuted—>控制器的OnActionExecuted——>全局的OnActionExecuted

详细可参考[ASP.NET Core 中的筛选器 | Microsoft Docs](https://docs.microsoft.com/zh-cn/aspnet/core/mvc/controllers/filters?view=aspnetcore-5.0#filter-scopes-and-order-of-execution)

#### 过滤器设置短路

可以在

过滤器的Executing方法里给context的Result 属性赋值，这样Executing方法执行完后就不会往下执行其他过滤器，因为ResourceFilter 在管道中比较靠前，一般使用它来设置短路,详细可见[ASP.NET Core 中的筛选器 | Microsoft Docs](https://docs.microsoft.com/zh-cn/aspnet/core/mvc/controllers/filters?view=aspnetcore-5.0#cancellation-and-short-circuiting)

![image-20211205231634029](images\image-20211205231634029.png)

![image-20211205231715722](images\image-20211205231715722.png)

由下图执行结果可见,并不会执行全局的Action过滤器及标记的过滤器的内容

![image-20211205231924791](images\image-20211205231924791.png)

### ResourceFilter 

用途

- 因为在管道中仅位于授权过滤器之后，可用来使大部分管道短路
- 用于做缓存

官网介绍可见[ASP.NET Core 中的筛选器 | Microsoft Docs](https://docs.microsoft.com/zh-cn/aspnet/core/mvc/controllers/filters?view=aspnetcore-5.0#resource-filters)

