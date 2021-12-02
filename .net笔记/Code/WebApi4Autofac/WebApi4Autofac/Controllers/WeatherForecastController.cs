using Microsoft.AspNetCore.Mvc;
using WebApi4Autofac.Filters;
using WebApi4Autofac.IBLL;

namespace WebApi4Autofac.Controllers
{
    [ApiController]
    [Route("[controller]")]   
    [MyActionFilterB]
    public class WeatherForecastController : ControllerBase
    {
        private static readonly string[] Summaries = new[]
        {
        "Freezing", "Bracing", "Chilly", "Cool", "Mild", "Warm", "Balmy", "Hot", "Sweltering", "Scorching"
    };

        private readonly ILogger<WeatherForecastController> _logger;

        private readonly ITestServiceA _testServiceA;

        public WeatherForecastController(ILogger<WeatherForecastController> logger, ITestServiceA testServiceA)
        {
            _logger = logger;
            _testServiceA = testServiceA;
        }

        [HttpGet]
        [MyActionFilterC(Order =1)]
        [TypeFilter(typeof(MyActionFilterD))]
        [MyFilterFactory(typeof(MyActionFilterD))]
        public IEnumerable<WeatherForecast> Get()
        {
            _testServiceA.SayHello("Hello World£¡");
            return Enumerable.Range(1, 5).Select(index => new WeatherForecast
            {
                Date = DateTime.Now.AddDays(index),
                TemperatureC = Random.Shared.Next(-20, 55),
                Summary = Summaries[Random.Shared.Next(Summaries.Length)]
            })
            .ToArray();
        }
    }
}