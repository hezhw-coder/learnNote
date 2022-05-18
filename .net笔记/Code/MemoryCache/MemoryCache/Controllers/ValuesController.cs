using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;

namespace MemoryCache.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class ValuesController : ControllerBase
    {
        private static readonly SemaphoreSlim semaphoreSlim = new SemaphoreSlim(1);
        private readonly IMemoryCache memoryCache;
        public ValuesController(IMemoryCache memoryCache)
        {
            this.memoryCache = memoryCache;
        }
        [HttpGet]
        public async  Task<ActionResult> Test()
        {
            string datimeStr = await this.memoryCache.GetOrCreateAsync<string>("date_time", async e => {
                await semaphoreSlim.WaitAsync();
                datimeStr = this.memoryCache.Get<string>("date_time");
                if (string.IsNullOrEmpty(datimeStr))
                {
                    Console.WriteLine("无缓存,从数据库中取...");
                }
                semaphoreSlim.Release();
                return datimeStr;
            });
            return Ok(datimeStr);

        }
    }
}
