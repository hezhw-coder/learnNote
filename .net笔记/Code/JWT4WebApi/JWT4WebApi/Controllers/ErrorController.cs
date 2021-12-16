using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
namespace JWT4WebApi.Controllers
{
    public class ErrorController : ControllerBase
    {
        [Route("error")]
        [ApiExplorerSettings(IgnoreApi = true)]
        public IActionResult Error()
        {
            return Problem();
        }
    }
}
