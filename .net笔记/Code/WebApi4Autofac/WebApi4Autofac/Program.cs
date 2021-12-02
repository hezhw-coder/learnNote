using Autofac;
using Autofac.Extensions.DependencyInjection;
using WebApi4Autofac.BLL;
using WebApi4Autofac.IBLL;

var builder = WebApplication.CreateBuilder(args);

builder.Host.UseServiceProviderFactory(new AutofacServiceProviderFactory());//使用Autofac工厂替换默认工厂


//builder.Host.ConfigureContainer<ContainerBuilder>((HostBuilderContext, ContainerBuilder) => {
//    ContainerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
//});//调用ConfigureContainer注册服务

builder.Host.ConfigureContainer<ContainerBuilder>((HostBuilderContext, ContainerBuilder) => {
    // 实例化ConfigurationBuilder.
    var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
    //使用Microsoft.Extensions.Configuration.Json读取json配置文件
    config.AddJsonFile("Conf/AutofacJson.json");

    // Register the ConfigurationModule with Autofac.
    var module = new Autofac.Configuration.ConfigurationModule(config.Build());//将配置文件加载至module
    ContainerBuilder.RegisterModule(module);
    ContainerBuilder.RegisterType(typeof(WebApi4Autofac.Filters.MyActionFilterD));
});//调用ConfigureContainer注册服务

// Add services to the container.

builder.Services.AddControllers(configure =>
{
    configure.Filters.Add<WebApi4Autofac.Filters.MyActionFilter>();

}).AddNewtonsoftJson(options =>
{
    options.SerializerSettings.DateFormatString = "yyyy-MM-dd HH:mm:ss";
});


var app = builder.Build();

// Configure the HTTP request pipeline.

app.UseAuthorization();

//app.Use((context, next) =>
//{
//    context.Request.EnableBuffering();
//    return next();
//});

app.MapControllers();

app.Run();
