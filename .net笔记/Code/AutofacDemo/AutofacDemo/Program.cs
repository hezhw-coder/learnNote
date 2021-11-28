// See https://aka.ms/new-console-template for more information
using Autofac;
using Autofac.Extras.DynamicProxy;
using AutofacDemo.BLL;
using AutofacDemo.IBLL;
using Microsoft.Extensions.Configuration;

#region 使用API创建容器
//实例化容器Builder
//ContainerBuilder containerBuilder = new ContainerBuilder();
////注册服务
//containerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
////containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().PropertiesAutowired();//属性注入
////containerBuilder.RegisterType<TestServiceBimpl>().OnActivated(u=>u.Instance.SetService(u.Context.Resolve<ITestServiceA>())).As<ITestServiceB>();//方法注入
////containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().InstancePerLifetimeScope();
////containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().InstancePerMatchingLifetimeScope("AutofacDemo");
//containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().SingleInstance();
////创建容器
//IContainer container = containerBuilder.Build(); 
#endregion
//从容器中获取服务
//ITestServiceA testServiceA = container.Resolve<ITestServiceA>();
#region 瞬时生命周期
//ITestServiceB testServiceB = container.Resolve<ITestServiceB>();
//ITestServiceB testServiceB1 = container.Resolve<ITestServiceB>();
//Console.WriteLine(Object.ReferenceEquals(testServiceB, testServiceB1)); 
#endregion
#region 范围内生命周期
//ITestServiceB testServiceB1 = null;
//ITestServiceB testServiceB2 = null;
//ITestServiceB testServiceB5 = null;
//using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
//{
//    testServiceB1 = lifetimeScope.Resolve<ITestServiceB>();
//    testServiceB2 = lifetimeScope.Resolve<ITestServiceB>();
//    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB2));//打印True
//    using (ILifetimeScope lifetimeScope1 = lifetimeScope.BeginLifetimeScope())
//    {
//        testServiceB5 = lifetimeScope1.Resolve<ITestServiceB>();
//    }
//    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB5));//打印False
//}
//ITestServiceB testServiceB3 = null;
//ITestServiceB testServiceB4 = null;
//using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
//{
//    testServiceB3 = lifetimeScope.Resolve<ITestServiceB>();
//    testServiceB4 = lifetimeScope.Resolve<ITestServiceB>();
//    Console.WriteLine(Object.ReferenceEquals(testServiceB3, testServiceB4));//打印True
//}
//Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB3));//打印False
#endregion
#region 每个匹配范围内生命周期
//ITestServiceB testServiceB1 = null;
//ITestServiceB testServiceB2 = null;
//ITestServiceB testServiceB5 = null;
//using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope("AutofacDemo"))
//{
//    testServiceB1 = lifetimeScope.Resolve<ITestServiceB>();
//    testServiceB2 = lifetimeScope.Resolve<ITestServiceB>();
//    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB2));//打印True
//    using (ILifetimeScope lifetimeScope1 = lifetimeScope.BeginLifetimeScope())
//    {
//        testServiceB5 = lifetimeScope1.Resolve<ITestServiceB>();
//    }
//    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB5));//打印True
//}
//ITestServiceB testServiceB3 = null;
//ITestServiceB testServiceB4 = null;
//using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope("AutofacDemo"))
//{
//    testServiceB3 = lifetimeScope.Resolve<ITestServiceB>();
//    testServiceB4 = lifetimeScope.Resolve<ITestServiceB>();
//    Console.WriteLine(Object.ReferenceEquals(testServiceB3, testServiceB4));//打印True
//}
//Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB3));//打印False

#endregion
#region 单例生命周期
//ITestServiceB testServiceB1 = null;
//ITestServiceB testServiceB2 = null;
//ITestServiceB testServiceB5 = null;
//using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
//{
//    testServiceB1 = lifetimeScope.Resolve<ITestServiceB>();
//    testServiceB2 = lifetimeScope.Resolve<ITestServiceB>();
//    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB2));//打印True
//    using (ILifetimeScope lifetimeScope1 = lifetimeScope.BeginLifetimeScope())
//    {
//        testServiceB5 = lifetimeScope1.Resolve<ITestServiceB>();
//    }
//    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB5));//打印True
//}
//ITestServiceB testServiceB3 = null;
//ITestServiceB testServiceB4 = null;
//using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
//{
//    testServiceB3 = lifetimeScope.Resolve<ITestServiceB>();
//    testServiceB4 = lifetimeScope.Resolve<ITestServiceB>();
//    Console.WriteLine(Object.ReferenceEquals(testServiceB3, testServiceB4));//打印True
//}
//Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB3));//打印True

#endregion
//调用方法
//testServiceA.Hello("Hello, World!");
//testServiceB.Hello("Hello, World!");

#region 使用配置文件配置服务
//// 实例化ConfigurationBuilder.
//var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
////使用Microsoft.Extensions.Configuration.Json读取json配置文件
//config.AddJsonFile("Conf/AutofacJson.json");

//// Register the ConfigurationModule with Autofac.
//var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
//var builder = new ContainerBuilder();//创建ContainerBuilder
//builder.RegisterModule(module);//注册服务
//IContainer container = builder.Build();//创建容器
//ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
//testServiceB.Hello("Hello, World!");
#endregion

#region 使用模块(Module)
//// Register the ConfigurationModule with Autofac.
//var module = new AutofacDemo.MyConfigurationModule();//实例化自定义的module实例
//var builder = new ContainerBuilder();//创建容器ContainerBuilder
//builder.RegisterModule(module);//注册module
//IContainer container = builder.Build();//创建容器
//ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
//testServiceB.Hello("Hello, World!");
#endregion


#region 使用配置文件配置module
//// 实例化ConfigurationBuilder.
//var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
////使用Microsoft.Extensions.Configuration.Json读取json配置文件
//config.AddJsonFile("Conf/moduleConfig.json");

//// Register the ConfigurationModule with Autofac.
//var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
//var builder = new ContainerBuilder();//创建ContainerBuilder
//builder.RegisterModule(module);//注册服务
//IContainer container = builder.Build();//创建容器
//ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
//testServiceB.Hello("Hello, World!");
#endregion

#region 使用Xml配置
//// 实例化ConfigurationBuilder.
//var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
////使用Microsoft.Extensions.Configuration.Xml读取xml配置文件
//config.AddXmlFile("Conf/AutofacXml.xml");

//// Register the ConfigurationModule with Autofac.
//var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
//var builder = new ContainerBuilder();//创建ContainerBuilder
//builder.RegisterModule(module);//注册服务
//IContainer container = builder.Build();//创建容器
//ITestServiceB testServiceB = container.Resolve<ITestServiceB>();//获取实例
//testServiceB.Hello("Hello, World!");
#endregion

#region 接口上配置AOP
////实例化容器Builder
//ContainerBuilder containerBuilder = new ContainerBuilder();
//containerBuilder.RegisterType(typeof(AutofacDemo.AOPDemo.CustomAutofacAop));//注册AOP服务
//containerBuilder.RegisterType<TestServiceCimpl>().As<ITestServiceC>().EnableInterfaceInterceptors();//注册实现类服务
////创建容器
//IContainer container = containerBuilder.Build();
//ITestServiceC testServiceC = container.Resolve<ITestServiceC>();
//testServiceC.SayHello("Hello World");
#endregion

#region 类上配置AOP
//实例化容器Builder
ContainerBuilder containerBuilder = new ContainerBuilder();
containerBuilder.RegisterType(typeof(AutofacDemo.AOPDemo.CustomAutofacAop));//注册AOP服务
containerBuilder.RegisterType<TestServiceDimpl>().As<ITestServiceD>().EnableClassInterceptors();//注册实现类服务
//创建容器
IContainer container = containerBuilder.Build();
ITestServiceD testServiceD = container.Resolve<ITestServiceD>();
testServiceD.SayHello("Hello World");
Console.WriteLine("--------------------------");
testServiceD.SayHi("Hello World");
#endregion
