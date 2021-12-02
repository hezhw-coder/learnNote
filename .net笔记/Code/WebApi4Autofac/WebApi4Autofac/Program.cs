using Autofac;
using Autofac.Extensions.DependencyInjection;
using WebApi4Autofac.BLL;
using WebApi4Autofac.IBLL;

var builder = WebApplication.CreateBuilder(args);

builder.Host.UseServiceProviderFactory(new AutofacServiceProviderFactory());//ʹ��Autofac�����滻Ĭ�Ϲ���


//builder.Host.ConfigureContainer<ContainerBuilder>((HostBuilderContext, ContainerBuilder) => {
//    ContainerBuilder.RegisterType<TestServiceAimpl>().As<ITestServiceA>();
//});//����ConfigureContainerע�����

builder.Host.ConfigureContainer<ContainerBuilder>((HostBuilderContext, ContainerBuilder) => {
    // ʵ����ConfigurationBuilder.
    var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder();
    //ʹ��Microsoft.Extensions.Configuration.Json��ȡjson�����ļ�
    config.AddJsonFile("Conf/AutofacJson.json");

    // Register the ConfigurationModule with Autofac.
    var module = new Autofac.Configuration.ConfigurationModule(config.Build());//�������ļ�������module
    ContainerBuilder.RegisterModule(module);
    ContainerBuilder.RegisterType(typeof(WebApi4Autofac.Filters.MyActionFilterD));
});//����ConfigureContainerע�����

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
