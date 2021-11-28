# Autofac

**以下例子使用的.NET SDK版本为6.0.100,开发工具使用Microsoft Visual Studio Enterprise 2022 (64 位) 版本 17.0.1**

**Autofac版本为6.3.0**

## 安装Autofac包

```powershell
Install-Package Autofac -Version 6.3.0
```

## 基本用法

**代码路径..\学习笔记\.net笔记\Code\AutofacDemo**



`..\学习笔记\.net笔记\Code\AutofacDemo\AutofacDemo\Program.cs`

```C#
using Autofac;
using AutofacDemo.BLL;
using AutofacDemo.IBLL;

//实例化容器Builder
ContainerBuilder containerBuilder = new ContainerBuilder();
//注册服务
containerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
//创建容器
IContainer container = containerBuilder.Build();
//从容器中获取服务
ITestServiceA testServiceA = container.Resolve<ITestServiceA>();
//调用方法
testServiceA.Hello("Hello, World!");
```

**执行结果**

![image-20211124152235087](images\image-20211124152235087.png)

## 构造函数注入(默认方式)

定义`TestServiceBimpl.cs`

![image-20211124151621657](images\image-20211124151621657.png)

```C#
using Autofac;
using AutofacDemo.BLL;
using AutofacDemo.IBLL;

//实例化容器Builder
ContainerBuilder containerBuilder = new ContainerBuilder();
//注册服务
containerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>();
//创建容器
IContainer container = containerBuilder.Build();
//从容器中获取服务
//ITestServiceA testServiceA = container.Resolve<ITestServiceA>();
ITestServiceB testServiceB = container.Resolve<ITestServiceB>();
//调用方法
//testServiceA.Hello("Hello, World!");
testServiceB.Hello("Hello, World!");
```

**执行结果**

![image-20211124152520876](images\image-20211124152520876.png)

## 属性注入

**如果要用属性注入，需要在注册时调用PropertiesAutowired方法**

![image-20211124153300105](images\image-20211124153300105.png)

![image-20211124153330051](images\image-20211124153330051.png)

## 方法注入

`TestServiceBimpl`类自定义一个方法，参数类别是`ITestServiceA`

![image-20211125003052193](images\image-20211125003052193.png)

注册服务使用方法注入

![image-20211125003322514](images\image-20211125003322514.png)

## 容器中对象的生命周期

### 瞬时生命周期InstancePerDependency(默认)

**瞬时生命周期：每一次从容器中获取对象都是一个全新的实例，默认的生命周期。**

```c#
using Autofac;
using AutofacDemo.BLL;
using AutofacDemo.IBLL;

//实例化容器Builder
ContainerBuilder containerBuilder = new ContainerBuilder();
//注册服务
containerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
//containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().PropertiesAutowired();//属性注入
containerBuilder.RegisterType<TestServiceBimpl>().OnActivated(u=>u.Instance.SetService(u.Context.Resolve<ITestServiceA>())).As<ITestServiceB>();//方法注入
//创建容器
IContainer container = containerBuilder.Build();
//从容器中获取服务
//ITestServiceA testServiceA = container.Resolve<ITestServiceA>();
ITestServiceB testServiceB = container.Resolve<ITestServiceB>();
ITestServiceB testServiceB1 = container.Resolve<ITestServiceB>();
Console.WriteLine(Object.ReferenceEquals(testServiceB, testServiceB1));//最后打印的是False
```

### 范围内生命周期(InstancePerLifetimeScope)

**`某个范围内获取的都是同一个实例`**

在注册实例时调用`InstancePerLifetimeScope`方法

![image-20211125235015780](images\image-20211125235015780.png)

```c#
// See https://aka.ms/new-console-template for more information
using Autofac;
using AutofacDemo.BLL;
using AutofacDemo.IBLL;

//实例化容器Builder
ContainerBuilder containerBuilder = new ContainerBuilder();
//注册服务
containerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
//containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().PropertiesAutowired();//属性注入
//containerBuilder.RegisterType<TestServiceBimpl>().OnActivated(u=>u.Instance.SetService(u.Context.Resolve<ITestServiceA>())).As<ITestServiceB>();//方法注入
containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().InstancePerLifetimeScope();
//创建容器
IContainer container = containerBuilder.Build();
//从容器中获取服务
//ITestServiceA testServiceA = container.Resolve<ITestServiceA>();
#region 瞬时生命周期
//ITestServiceB testServiceB = container.Resolve<ITestServiceB>();
//ITestServiceB testServiceB1 = container.Resolve<ITestServiceB>();
//Console.WriteLine(Object.ReferenceEquals(testServiceB, testServiceB1)); 
#endregion
#region 范围内生命周期
ITestServiceB testServiceB1 = null;
ITestServiceB testServiceB2 = null;
ITestServiceB testServiceB5 = null;
using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
{
     testServiceB1 = lifetimeScope.Resolve<ITestServiceB>();
     testServiceB2 = lifetimeScope.Resolve<ITestServiceB>();
     Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB2));//打印True
    using (ILifetimeScope lifetimeScope1 = container.BeginLifetimeScope())
    {
        testServiceB5 = lifetimeScope1.Resolve<ITestServiceB>();
    }
    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB5));//打印False
}
ITestServiceB testServiceB3 = null;
ITestServiceB testServiceB4 = null;
using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
{
    testServiceB3 = lifetimeScope.Resolve<ITestServiceB>();
    testServiceB4 = lifetimeScope.Resolve<ITestServiceB>();
    Console.WriteLine(Object.ReferenceEquals(testServiceB3, testServiceB4));//打印True
}
Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB3));//打印False

#endregion
//调用方法
//testServiceA.Hello("Hello, World!");
//testServiceB.Hello("Hello, World!");
```

### 每个匹配生命周期范围一个实例(InstancePerMatchingLifetimeScope)

![image-20211125235353166](images\image-20211125235353166.png)

还有一点与InstancePerLifetimeScope中的不同点是，如果在InstancePerMatchingLifetimeScope范围内在用IContainer的对象取开启生命周期则会报错，而在InstancePerLifetimeScope中这种用法不会报错

![image-20211125235542603](images\image-20211125235542603.png)

### 单例生命周期(SingleInstance)

![image-20211126000304107](images\image-20211126000304107.png)

### 每个请求一个实例(InstancePerRequest)

这个不好演示,等到整合web项目时再演示

### InstancePerOwned

这个由使用者自己控制

## 配置文件配置实例

nuget安装Autofac.Configuration配置扩展包(例子使用6.0.0版本)

```powershell
Install-Package Autofac.Configuration -Version 6.0.0
```

### 使用`json`文件配置

nuget安装json文件配置扩展包(例子使用6.0.0版本)

```powershell
Install-Package Microsoft.Extensions.Configuration.Json -Version 6.0.0
```

创建配置文件`Conf\AutofacJson.json`

```json
{
  "components": [
    {
      "type": "AutofacDemo.BLL.TestServiceAimpl, AutofacDemo",
      "services": [
        {
          "type": "AutofacDemo.IBLL.ITestServiceA,AutofacDemo"
        }
      ],
      "instanceScope": "Per-Lifetime-Scope"//生命周期
    },
    {
      "type": "AutofacDemo.BLL.TestServiceBimpl, AutofacDemo", //服务对应实现类配置
      "services": [
        {
          "type": "AutofacDemo.IBLL.ITestServiceB,AutofacDemo" //服务对应接口配置
        }
      ],
      "injectProperties": true //TestServiceBimpl类中使用的是属性注入
    }
  ]
}
```

![image-20211127001106543](images\image-20211127001106543.png)

Json配置文件中生命周期instanceScope值的写法

- single-instance(单例)
- per-dependency(瞬时)
- per-lifetime-scope((每个生命周期范围的实例)
- per-request(每个请求一个实例)



示例代码

```C#
// 实例化ConfigurationBuilder.
var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
//使用Microsoft.Extensions.Configuration.Json读取json配置文件
config.AddJsonFile("Conf/AutofacJson.json");

// Register the ConfigurationModule with Autofac.
var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
var builder = new ContainerBuilder();//创建ContainerBuilder
builder.RegisterModule(module);//注册服务
IContainer container = builder.Build();//创建容器
ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
testServiceB.Hello("Hello, World!");
```

其他常用属性配置

![image-20211128105041985](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20211128105041985.png)

### Module的使用

自定义MyConfigurationModule继承抽象类Autofac.Module

![image-20211128121304769](images\image-20211128121304769.png)

**Module基本使用**

```C#
// Register the ConfigurationModule with Autofac.
var module = new AutofacDemo.MyConfigurationModule();//实例化自定义的module实例
var builder = new ContainerBuilder();//创建容器ContainerBuilder
builder.RegisterModule(module);//注册module
IContainer container = builder.Build();//创建容器
ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
testServiceB.Hello("Hello, World!");
```

原理:ContainerBuilder调用Build()方法时,会调用到基类Autofac.Module的Configure方法，该方法会依次调用自定义类MyConfigurationModule中的以下方法

- void Load(ContainerBuilder builder)
- void AttachToComponentRegistration(IComponentRegistryBuilder componentRegistry, IComponentRegistration registration)
- AttachToRegistrationSource(IComponentRegistryBuilder componentRegistry, IRegistrationSource registrationSource)

![image-20211128122155581](images\image-20211128122155581.png)

### 使用配置文件配置module

Conf文件夹下新建moduleConfig.json文件

![image-20211128125201220](images\image-20211128125201220.png)

使用示例

```C#
// 实例化ConfigurationBuilder.
var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
//使用Microsoft.Extensions.Configuration.Json读取json配置文件
config.AddJsonFile("Conf/moduleConfig.json");

// Register the ConfigurationModule with Autofac.
var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
var builder = new ContainerBuilder();//创建ContainerBuilder
builder.RegisterModule(module);//注册服务
IContainer container = builder.Build();//创建容器
ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
testServiceB.Hello("Hello, World!");
```

### 使用`xml`文件配置

nuget安装xml文件配置扩展包(例子使用6.0.0版本)

```powershell
Install-Package Microsoft.Extensions.Configuration.Xml -Version 6.0.0
```

创建配置文件`Conf\AutofacXml.xml`

```xml
<?xml version="1.0" encoding="utf-8" ?>
<autofac>
	<components name="0">
		<type>AutofacDemo.BLL.TestServiceAimpl, AutofacDemo</type>
		<services name="0" type="AutofacDemo.IBLL.ITestServiceA,AutofacDemo" />
	</components>
	<components name="1">
		<type>AutofacDemo.BLL.TestServiceBimpl, AutofacDemo</type>
		<services name="0" type="AutofacDemo.IBLL.ITestServiceB,AutofacDemo" />
		<injectProperties>true</injectProperties><!--支持属性注入-->
	</components>
</autofac>
```

![image-20211128132049673](images\image-20211128132049673.png)

***请注意 XML 中components和services的序号"命名" - 这是由于 Microsoft.Extensions.Configuration 处理序号集合（数组）的方式***

使用示例

```C#
// 实例化ConfigurationBuilder.
var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
//使用Microsoft.Extensions.Configuration.Xml读取xml配置文件
config.AddXmlFile("Conf/AutofacXml.xml");

// Register the ConfigurationModule with Autofac.
var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
var builder = new ContainerBuilder();//创建ContainerBuilder
builder.RegisterModule(module);//注册服务
IContainer container = builder.Build();//创建容器
ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
testServiceB.Hello("Hello, World!");
```

