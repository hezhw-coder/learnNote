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
                        return context.Response.WriteAsync($"身份验证失败!");
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
            });
#endregion

var app = builder.Build();

// Configure the HTTP request pipeline.
app.UseAuthentication();//启用鉴权中间件
app.UseAuthorization();

app.MapControllers();

app.Run();
