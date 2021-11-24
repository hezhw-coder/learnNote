// See https://aka.ms/new-console-template for more information
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
//调用方法
//testServiceA.Hello("Hello, World!");
testServiceB.Hello("Hello, World!");
