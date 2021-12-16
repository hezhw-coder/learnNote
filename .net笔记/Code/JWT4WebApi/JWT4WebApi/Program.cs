using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.Filters;
using System.Reflection;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers()
    .ConfigureApiBehaviorOptions(options =>
{
    options.InvalidModelStateResponseFactory = context =>
    {
        //var result = new Microsoft.AspNetCore.Mvc.BadRequestObjectResult(context.ModelState);

        //// TODO: add `using System.Net.Mime;` to resolve MediaTypeNames
        //result.ContentTypes.Add(System.Net.Mime.MediaTypeNames.Application.Json);
        //result.ContentTypes.Add(System.Net.Mime.MediaTypeNames.Application.Xml);
        Dictionary<string, Microsoft.AspNetCore.Mvc.ModelBinding.ModelErrorCollection> dicErrors = new Dictionary<string, Microsoft.AspNetCore.Mvc.ModelBinding.ModelErrorCollection>(); 
        foreach (var key in context.ModelState.Keys)
        {
            Microsoft.AspNetCore.Mvc.ModelBinding.ModelErrorCollection errors = context.ModelState[key].Errors;
            dicErrors.Add(key, errors);
        }

        return new Microsoft.AspNetCore.Mvc.JsonResult(dicErrors);
    };
});
builder.Services.AddEndpointsApiExplorer();
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
    var xmlFile = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
    c.IncludeXmlComments(xmlPath);

    //��Ӧʱ��Header�����JWT�����̨����,������ȨС��
    c.OperationFilter<AddResponseHeadersFilter>();
    c.OperationFilter<AppendAuthorizeToSummaryOperationFilter>();
    c.OperationFilter<SecurityRequirementsOperationFilter>();
    //���÷���
    c.AddSecurityDefinition("oauth2",new OpenApiSecurityScheme()
    {
        Description = "��������Tokenǰ�����Bearer��һ���ո�",
        Name = "Authorization",
        In=ParameterLocation.Header,
        Type = SecuritySchemeType.ApiKey
    });

});

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
                jwtBearerOptions.Events = new JwtBearerEvents
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

if (app.Environment.IsDevelopment())//�ж��Ƿ񿪷�����
{
    app.UseSwagger();

    app.UseSwaggerUI();
}
app.UseExceptionHandler("/error");
// Configure the HTTP request pipeline.
app.UseAuthentication();//���ü�Ȩ�м��
app.UseAuthorization();

app.MapControllers();

app.Run();
