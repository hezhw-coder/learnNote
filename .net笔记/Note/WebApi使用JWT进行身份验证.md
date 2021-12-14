# WebApi使用JWT进行身份验证

JWT相关请参阅[C# 实现Jwtbearer Authentication - 红泥巴煮雪 - 博客园 (cnblogs.com)](https://www.cnblogs.com/aishangyipiyema/p/9262642.html)



示例路径`https://github.com/hezhw-coder/learnNote/tree/main/.net%E7%AC%94%E8%AE%B0/Code/JWT4WebApi`

Nuget安装Jwt包

```powershell
Install-Package Microsoft.AspNetCore.Authentication.JwtBearer -Version 6.0.0
```

## 提供一个服务用于生成Token

```c#
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace JWT4WebApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class OAuthController : ControllerBase
    {
        [HttpPost]
        public string Token(string user, string password,string role)
        {

            //验证用户名和密码(一般从数据库认证)
            if (user!="admin"&& password!="admin")
            {
                return $"没有生成Token的权限！";
            }
            //role角色信息一般是根据账户从数据库获取

            //Claim，JwtRegisteredClaimNames中预定义了好多种默认的参数名，自己定义键值.
            //ClaimTypes也预定义了好多类型如role、email、name。Role用于赋予权限，配合授权中间件(基于角色的授权)，不同的角色可以访问不同的接口
            var claims = new Claim[] { new Claim(ClaimTypes.Role,role) ,new Claim(ClaimTypes.Name, "TEST"), new Claim(JwtRegisteredClaimNames.Name, "TEST") };//这部分会在在Token的Payload里，因此不放敏感信息，比如用户名和密码

            var key = new Microsoft.IdentityModel.Tokens.SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes("9E1668E9-13CF-4A60-8B22-EB662E165CA7"));//秘钥(一般在配置文件进行配置)
            var token = new JwtSecurityToken(
                        issuer: "http://localhost:5166/",//jwt签发者(一般从配置文件读取)
                        audience: "http://localhost:5166/",//接收jwt的一方(一般从配置文件读取)
                        claims: claims,//自定义的payload
                        notBefore: DateTime.Now,//定义在什么时间之前，该jwt都是不可用的.
                        expires: DateTime.Now.AddDays(28),//jwt的过期时间，这个过期时间必须要大于签发时间
                        signingCredentials: new SigningCredentials(key, SecurityAlgorithms.HmacSha256)//
                        );

            string jwtToken = new JwtSecurityTokenHandler().WriteToken(token);//生成Token
            return jwtToken;
        }
    }
}

```

![image-20211213225834402](images\image-20211213225834402.png)

## 配置JWT服务凝启用鉴权中间件

```c#
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();

#region JWT服务
builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;

}).AddJwtBearer(JwtBearerDefaults.AuthenticationScheme,
            (jwtBearerOptions) =>
            {
                jwtBearerOptions.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes("9E1668E9-13CF-4A60-8B22-EB662E165CA7")),//秘钥,//与OAuthController中的key一致(一般从配置文件读取)
                    ValidateIssuer = true,
                    ValidIssuer = "http://localhost:5166/",//与OAuthController中的issuer一致(一般从配置文件读取)
                    ValidateAudience = true,
                    ValidAudience = "http://localhost:5166/",//与OAuthController中的audience一致(一般从配置文件读取)
                    ValidateLifetime = true,
                    ClockSkew = TimeSpan.FromSeconds(5)
                };
            });
#endregion

var app = builder.Build();

// Configure the HTTP request pipeline.
app.UseAuthentication();//启用鉴权中间件
app.UseAuthorization();

app.MapControllers();

app.Run();
```

![image-20211212234230423](images\image-20211212234230423.png)

## 标记[Authorize]让鉴权生效

- 标记在控制器上时,该控制器下的所有Action都会生效，若哪个Action不想进行校验，则标记[AllowAnonymous]特性
- 只标记在Action上就只对单签Action生效



**在控制器上标记[Authorize]，在Action标记[AllowAnonymous]时，可正常访问**

![image-20211212234930907](images\image-20211212234930907.png)



**注释掉Action标记的[AllowAnonymous]时，返回401状态码，即需要进行身份验证**

![image-20211212235144770](images\image-20211212235144770.png)

![image-20211213000348777](images\image-20211213000348777.png)

## 用Postman获取Token

http://localhost:5166/api/OAuth?user=admin&password=admin&role=admin

![image-20211213234223376](images\image-20211213234223376.png)

## postman请求时Headers带上授权信息

- KEY填写**Authorization**
- VALUE填写 Bearer +Token的值(特别注意Bearer要先加一个空格再接Token)



![image-20211212235927070](images\image-20211212235927070.png)

## 身份验证或失败后自定义回参处理

- 使用jwtBearerOptions注册事件

```C#
jwtBearerOptions.Events= new JwtBearerEvents
                {
                    //鉴权失败时调用,默认返回401,可自己自定义状态码及返回结果
                    OnChallenge = context =>
                    {
                        //此处代码为终止.Net Core默认的返回类型和数据结果,变成由自己接管,自定义状态码时必须写上
                        context.HandleResponse();
                        //自定义返回的数据类型
                        context.Response.ContentType = "text/plain;charset=UTF-8";
                        ////自定义返回状态码，默认为401 我这里改成 200
                        context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                        ////输出数据结果
                        //return context.Response.WriteAsync($"身份验证失败!");//如果前面什么都没处理，默认是返回401
                        return Task.CompletedTask;
                    },
                    //授权失败时调用,默认返回403状态码，可自己自定义状态码及返回结果
                    OnForbidden = context =>
                    {
                        context.Response.ContentType = "text/plain;charset=UTF-8";
                        ////自定义返回状态码，默认为401 我这里改成 200
                        context.Response.StatusCode = StatusCodes.Status403Forbidden;
                        ////输出数据结果
                        return context.Response.WriteAsync("权限不足");
                    }

                };
```

![image-20211213231047496](images\image-20211213231047496.png)

#### 当请求时不带上Headers时返回自定义的信息

![image-20211213231519096](images\image-20211213231519096.png)

## JWT基于角色授权

- 在生成Token时将角色信息加入到Claim数组中

  ![image-20211213233018035](images\image-20211213233018035.png)

- 在控制器或者Action上标记特性[Authorize(Roles ="admin")]，指定角色才能访问

  ![image-20211213233822010](images\image-20211213233822010.png)

#### 测试

- 注册授权失败时的自定义事件

  ![image-20211213234835229](images\image-20211213234835229.png)

- 传入`admin1`角色去生成Token然后进行调用接口会提示没有权限

  ![image-20211213234322173](images\image-20211213234322173.png)

​           ![image-20211213234513169](images\image-20211213234513169.png)

- 传入`admin`角色去生成Token然后进行调用接口可以调用成功

  ![image-20211213234628859](images\image-20211213234628859.png)

![image-20211213234714320](images\image-20211213234714320.png)

## WebApi使用Swagger 

建立项目时勾选OpenAPI，Visual Studio会自动建立

![image-20211214154455011](images\image-20211214154455011.png)

以下是在建立项目时不勾选

安装Nuget包

```powershell
Install-Package Swashbuckle.AspNetCore -Version 6.2.3
```

添加相关服务与中间件

![image-20211214144417688](images\image-20211214144417688.png)

在launchSettings.json配置将默认启动的Url改成Swagger

![image-20211214155214175](images\image-20211214155214175.png)

## 注册Swagger服务时自定义文档描述

```C#
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Version = "v1",
        Title = "ToDo API",
        Description = "A simple example ASP.NET Core Web API",
        TermsOfService = new Uri("https://example.com/terms"),
        Contact = new OpenApiContact
        {
            Name = "Shayne Boyer",
            Email = string.Empty,
            Url = new Uri("https://twitter.com/spboyer"),
        },
        License = new OpenApiLicense
        {
            Name = "Use under LICX",
            Url = new Uri("https://example.com/license"),
        }
    });
});
```

![image-20211214160958664](images\image-20211214160958664.png)

![image-20211214161125381](images\image-20211214161125381.png)

## Swagger添加注释

项目工程右键编辑项目文件,启用xml文档注释

```xml
<GenerateDocumentationFile>true</GenerateDocumentationFile><!--启用文档注释-->
```

![image-20211214162003312](images\image-20211214162003312.png)

启用文档注释后如果没对属性或者类型添加注释Visual Studio会给出警告

![image-20211214162156545](images\image-20211214162156545.png)

项目文件文件添加以下配置可消除警告

```xml
<NoWarn>$(NoWarn);1591</NoWarn>
```

![image-20211214162505977](images\image-20211214162505977.png)

Swagger配置注释服务

```C#
var xmlFile = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
c.IncludeXmlComments(xmlPath);
```

![image-20211214162911080](images\image-20211214162911080.png)

项目启动后对应的方法就会有注释

![image-20211214163120432](images\image-20211214163120432.png)

### Swagger配置JWT认证功能

安装Nuget包

```powershell
Install-Package Swashbuckle.AspNetCore.Filters -Version 7.0.2
```

AddSwaggerGen中进行配置

```C#
//响应时再Header中添加JWT传入后台方法,开启授权小锁
c.OperationFilter<AddResponseHeadersFilter>();
c.OperationFilter<AppendAuthorizeToSummaryOperationFilter>();
c.OperationFilter<SecurityRequirementsOperationFilter>();
//配置方法
c.AddSecurityDefinition("oauth2",new OpenApiSecurityScheme()
{
   Description = "请在输入Token前先添加Bearer和一个空格",
   Name = "Authorization",
   In=ParameterLocation.Header,
   Type = SecuritySchemeType.ApiKey
});
```

![image-20211214170416346](images\image-20211214170416346.png)

先通过生成Token的接口获取Token

![image-20211214172949424](images\image-20211214172949424.png)

没有输入Token的情况会返回身份验证失败

![image-20211214173124328](images\image-20211214173124328.png)

点击方法右边的小锁进行输入Token

![image-20211214173327332](images\image-20211214173327332.png)

![image-20211214173505157](images\image-20211214173505157.png)

再次调用后可正常返回数据

![image-20211214173557699](images\image-20211214173557699.png)
