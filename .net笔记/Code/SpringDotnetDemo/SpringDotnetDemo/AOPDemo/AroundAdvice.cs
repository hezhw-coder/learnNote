using AopAlliance.Intercept;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.AOPDemo
{
    class AroundAdvice : IMethodInterceptor
    {
        object IMethodInterceptor.Invoke(IMethodInvocation invocation)
        {
            Console.WriteLine("第二个代理Before....");
            
            object v = invocation.Proceed();

            Console.WriteLine("第二个代理After....");
            return v;
        }
    }
}
