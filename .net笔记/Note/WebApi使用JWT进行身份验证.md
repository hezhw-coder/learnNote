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
        public string Token(string user, string password)
        {

            //验证用户名和密码(一般从数据库认证)
            if (user!="admin"&& password!="admin")
            {
                return $"没有生成Token的权限！";
            }


            var claims = new Claim[] { new Claim(ClaimTypes.Name, "TEST"), new Claim(JwtRegisteredClaimNames.Name, "TEST") };//这部分会在Token的Payload里，因此不放敏感信息，比如用户名和密码

            var key = new Microsoft.IdentityModel.Tokens.SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes("9E1668E9-13CF-4A60-8B22-EB662E165CA7"));//秘钥(一般在配置文件进行配置)
            var token = new JwtSecurityToken(
                        issuer: "http://localhost:5166/",//jwt签发者
                        audience: "http://localhost:5166/",//接收jwt的一方
                        claims: claims,//自定义的payload
                        notBefore: DateTime.Now,//定义在什么时间之前，该jwt都是不可用的.
                        expires: DateTime.Now.AddDays(28),//jwt的过期时间，这个过期时间必须要大于签发时间
                        signingCredentials: new SigningCredentials(key, SecurityAlgorithms.HmacSha256)//
                        );

            string jwtToken = new JwtSecurityTokenHandler().WriteToken(token);//生成Token
            return jwtToken;
        }
    }
```

![image-20211212232550644](images\image-20211212232550644.png)

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

http://localhost:5166/api/OAuth?user=admin&password=admin

![image-20211212235508284](images\image-20211212235508284.png)

## postman请求时Headers带上授权信息

- KEY填写**Authorization**
- VALUE填写 Bearer +Token的值(特别注意Bearer要先加一个空格再接Token)



![image-20211212235927070](images\image-20211212235927070.png)

