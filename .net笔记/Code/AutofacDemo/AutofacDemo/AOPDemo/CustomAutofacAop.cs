using Castle.DynamicProxy;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo.AOPDemo
{
    public class CustomAutofacAop : Castle.DynamicProxy.IInterceptor
    {
        public void Intercept(IInvocation invocation)
        {
            Console.WriteLine($"{invocation.Method.Name}执行前.....");
            invocation.Proceed();
            Console.WriteLine($"{invocation.Method.Name}执行后.....");

        }
    }
}
