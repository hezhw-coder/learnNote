using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();

#region JWT����
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
                    IssuerSigningKey = new SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes("9E1668E9-13CF-4A60-8B22-EB662E165CA7")),//��Կ,//��OAuthController�е�keyһ��(һ��������ļ���ȡ)
                    ValidateIssuer = true,
                    ValidIssuer = "http://localhost:5166/",//��OAuthController�е�issuerһ��(һ��������ļ���ȡ)
                    ValidateAudience = true,
                    ValidAudience = "http://localhost:5166/",//��OAuthController�е�audienceһ��(һ��������ļ���ȡ)
                    ValidateLifetime = true,
                    ClockSkew = TimeSpan.FromSeconds(5)
                };
                jwtBearerOptions.Events= new JwtBearerEvents
                {
                    //��Ȩʧ��ʱ����,Ĭ�Ϸ���401,���Լ��Զ���״̬�뼰���ؽ��
                    OnChallenge = context =>
                    {
                        //�˴�����Ϊ��ֹ.Net CoreĬ�ϵķ������ͺ����ݽ��,������Լ��ӹ�,�Զ���״̬��ʱ����д��
                        context.HandleResponse();
                        //�Զ��巵�ص���������
                        context.Response.ContentType = "text/plain;charset=UTF-8";
                        ////�Զ��巵��״̬�룬Ĭ��Ϊ401 ������ĳ� 200
                        context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                        ////������ݽ��
                        return context.Response.WriteAsync($"�����֤ʧ��!");
                    },
                    //��Ȩʧ��ʱ����,Ĭ�Ϸ���403״̬�룬���Լ��Զ���״̬�뼰���ؽ��
                    OnForbidden = context =>
                    {
                        context.Response.ContentType = "text/plain;charset=UTF-8";
                        ////�Զ��巵��״̬�룬Ĭ��Ϊ401 ������ĳ� 200
                        context.Response.StatusCode = StatusCodes.Status403Forbidden;
                        ////������ݽ��
                        return context.Response.WriteAsync("Ȩ�޲���");
                    }

                };
            });
#endregion

var app = builder.Build();

// Configure the HTTP request pipeline.
app.UseAuthentication();//���ü�Ȩ�м��
app.UseAuthorization();

app.MapControllers();

app.Run();
