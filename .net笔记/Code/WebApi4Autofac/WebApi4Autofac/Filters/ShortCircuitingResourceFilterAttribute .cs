using Microsoft.AspNetCore.Mvc.Filters;

namespace WebApi4Autofac.Filters
{
    public class ShortCircuitingResourceFilterAttribute : Attribute, IResourceFilter
    {
        public void OnResourceExecuting(ResourceExecutingContext context)
        {
            context.Result = new Microsoft.AspNetCore.Mvc.ContentResult()
            {
                Content = "Resource unavailable - header not set."
            };
        }

        public void OnResourceExecuted(ResourceExecutedContext context)
        {
        }
    }
}
