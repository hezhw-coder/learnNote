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

**这个由使用者自己控制**
