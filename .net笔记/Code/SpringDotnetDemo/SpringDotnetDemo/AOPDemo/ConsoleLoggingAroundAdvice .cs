using AopAlliance.Intercept;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.AOPDemo
{
    public class ConsoleLoggingAroundAdvice : IMethodInterceptor
    {
        public object Invoke(IMethodInvocation invocation)
        {
            Console.Out.WriteLine("Advice executing; calling the advised method..."); 
            object returnValue = invocation.Proceed();
            Console.Out.WriteLine($"Advice executed; advised method returned {returnValue}"); 
            return returnValue;
        }
    }
}
