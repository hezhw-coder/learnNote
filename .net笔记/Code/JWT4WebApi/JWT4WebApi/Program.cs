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

    //????????Header??????JWT????????????,????????????
    c.OperationFilter<AddResponseHeadersFilter>();
    c.OperationFilter<AppendAuthorizeToSummaryOperationFilter>();
    c.OperationFilter<SecurityRequirementsOperationFilter>();
    //????????
    c.AddSecurityDefinition("oauth2",new OpenApiSecurityScheme()
    {
        Description = "????????Token????????Bearer??????????",
        Name = "Authorization",
        In=ParameterLocation.Header,
        Type = SecuritySchemeType.ApiKey
    });

});

#region JWT????
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
                    IssuerSigningKey = new SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes("9E1668E9-13CF-4A60-8B22-EB662E165CA7")),//????,//??OAuthController????key????(??????????????????)
                    ValidateIssuer = true,
                    ValidIssuer = "http://localhost:5166/",//??OAuthController????issuer????(??????????????????)
                    ValidateAudience = true,
                    ValidAudience = "http://localhost:5166/",//??OAuthController????audience????(??????????????????)
                    ValidateLifetime = true,
                    ClockSkew = TimeSpan.FromSeconds(5)
                };
                jwtBearerOptions.Events = new JwtBearerEvents
                {
                    //??????????????,????????401,????????????????????????????
                    OnChallenge = context =>
                    {
                        //??????????????.Net Core????????????????????????,??????????????,??????????????????????
                        context.HandleResponse();
                        //????????????????????
                        context.Response.ContentType = "text/plain;charset=UTF-8";
                        ////????????????????????????401 ?????????? 200
                        context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                        ////????????????
                        return context.Response.WriteAsync($"????????????!");
                    },
                    //??????????????,????????403????????????????????????????????????
                    OnForbidden = context =>
                    {
                        context.Response.ContentType = "text/plain;charset=UTF-8";
                        ////????????????????????????401 ?????????? 200
                        context.Response.StatusCode = StatusCodes.Status403Forbidden;
                        ////????????????
                        return context.Response.WriteAsync("????????");
                    }

                };
            });
#endregion



var app = builder.Build();

if (app.Environment.IsDevelopment())//????????????????
{
    app.UseSwagger();

    app.UseSwaggerUI();
}
app.UseExceptionHandler("/error");
// Configure the HTTP request pipeline.
app.UseAuthentication();//??????????????
app.UseAuthorization();

app.MapControllers();

app.Run();
