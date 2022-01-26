using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Distributed;

namespace RedisDemo.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class RedisController : ControllerBase
    {
        //private static readonly object _locker = new object();
        private static readonly SemaphoreSlim semaphoreSlim = new SemaphoreSlim(1);
        private readonly IDistributedCache distributedCache;
        public RedisController(IDistributedCache distributedCache)
        {
            this.distributedCache = distributedCache;
        }

        [HttpGet]
        public async Task<string> Index()
        {

            try
            {
                #region Lock实现线程
                //string v = this.distributedCache.GetString("date_time");
                //if (string.IsNullOrEmpty(v))
                //{
                //    lock (_locker)
                //    {
                //        v = this.distributedCache.GetString("date_time");
                //        if (string.IsNullOrEmpty(v))
                //        {
                //            Console.WriteLine("从数据库中读取。。。。");
                //            v = DateTime.Now.ToString();
                //            this.distributedCache.SetString("date_time", v);
                //        }
                //    }
                //} 
                #endregion
                string v = await this.distributedCache.GetStringAsync("date_time");
                if (string.IsNullOrEmpty(v))
                {
                    await semaphoreSlim.WaitAsync();
                    v =await this.distributedCache.GetStringAsync("date_time");
                    if (string.IsNullOrEmpty(v))
                    {
                        Console.WriteLine("从数据库中读取。。。。");
                        v = DateTime.Now.ToString();
                        await this.distributedCache.SetStringAsync("date_time", v);
                    }
                    semaphoreSlim.Release();
                }
                return v;
            }
            catch (Exception ex)
            {

                throw;
            }
        }
    }
}
