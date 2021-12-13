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
