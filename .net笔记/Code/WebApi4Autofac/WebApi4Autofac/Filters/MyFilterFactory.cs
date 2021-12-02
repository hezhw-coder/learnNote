using Microsoft.AspNetCore.Mvc.Filters;

namespace WebApi4Autofac.Filters
{
    public class MyFilterFactory : Attribute, IFilterFactory
    {
        private readonly Type _type;
        /// <summary>
        /// 构造传入的过滤器Type
        /// </summary>
        /// <param name="type"></param>
        public MyFilterFactory(Type type)
        {
            this._type = type;
        }
        /// <summary>
        /// 是否可重用
        /// </summary>
        public bool IsReusable => true;

        public IFilterMetadata CreateInstance(IServiceProvider serviceProvider)
        {
            object? oInstance = serviceProvider.GetService(_type);
            return (IFilterMetadata)oInstance;
        }
    }
}
