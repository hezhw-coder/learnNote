using Microsoft.AspNetCore.Mvc.Filters;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;

namespace WebApi4Autofac.Filters
{
    public class MyActionFilterC : ActionFilterAttribute, IActionFilter
    {
        public override void OnActionExecuting(ActionExecutingContext context)
        {
            Microsoft.AspNetCore.Mvc.Controllers.ControllerActionDescriptor actionDescript = (Microsoft.AspNetCore.Mvc.Controllers.ControllerActionDescriptor)context.ActionDescriptor;
            Console.WriteLine($"当前过滤器:{nameof(MyActionFilterC)} ControllerName:{actionDescript.ControllerName} ActionName:{actionDescript.ActionName}执行前....");
            base.OnActionExecuting(context);
        }

        public override void OnActionExecuted(ActionExecutedContext context)
        {
            Microsoft.AspNetCore.Mvc.Controllers.ControllerActionDescriptor controllerActionDescriptor = (Microsoft.AspNetCore.Mvc.Controllers.ControllerActionDescriptor)context.ActionDescriptor;
            Console.WriteLine($"当前过滤器:{nameof(MyActionFilterC)} ControllerName:{controllerActionDescriptor.ControllerName} ActionName:{controllerActionDescriptor.ActionName}执行后....");
            base.OnActionExecuted(context);
        }
    }
}
