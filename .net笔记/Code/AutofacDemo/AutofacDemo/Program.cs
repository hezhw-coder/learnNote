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
//containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().InstancePerLifetimeScope();
//containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().InstancePerMatchingLifetimeScope("AutofacDemo");
containerBuilder.RegisterType<TestServiceBimpl>().As<ITestServiceB>().SingleInstance();
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
ITestServiceB testServiceB1 = null;
ITestServiceB testServiceB2 = null;
ITestServiceB testServiceB5 = null;
using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
{
    testServiceB1 = lifetimeScope.Resolve<ITestServiceB>();
    testServiceB2 = lifetimeScope.Resolve<ITestServiceB>();
    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB2));//打印True
    using (ILifetimeScope lifetimeScope1 = lifetimeScope.BeginLifetimeScope())
    {
        testServiceB5 = lifetimeScope1.Resolve<ITestServiceB>();
    }
    Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB5));//打印True
}
ITestServiceB testServiceB3 = null;
ITestServiceB testServiceB4 = null;
using (ILifetimeScope lifetimeScope = container.BeginLifetimeScope())
{
    testServiceB3 = lifetimeScope.Resolve<ITestServiceB>();
    testServiceB4 = lifetimeScope.Resolve<ITestServiceB>();
    Console.WriteLine(Object.ReferenceEquals(testServiceB3, testServiceB4));//打印True
}
Console.WriteLine(Object.ReferenceEquals(testServiceB1, testServiceB3));//打印True

#endregion
//调用方法
//testServiceA.Hello("Hello, World!");
//testServiceB.Hello("Hello, World!");
