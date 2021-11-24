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
