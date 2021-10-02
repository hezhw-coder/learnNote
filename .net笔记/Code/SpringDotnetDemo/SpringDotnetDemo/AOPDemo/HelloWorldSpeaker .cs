using AopAlliance.Intercept;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.AOPDemo
{
    public enum Language
    {
        English = 1,
        Portuguese = 2,
        Italian = 3
    }

    public interface IHelloWorldSpeaker
    {
        void SayHello();
    }

    public class HelloWorldSpeaker : IHelloWorldSpeaker
    {
        private Language language;

        public Language Language
        {
            set { language = value; }
            get { return language; }
        }

        public void SayHello()
        {
            switch (language)
            {
                case Language.English:
                    Console.WriteLine("Hello World!");
                    break;
                case Language.Portuguese:
                    Console.WriteLine("Oi Mundo!");
                    break;
                case Language.Italian:
                    Console.WriteLine("Ciao Mondo!");
                    break;
            }
        }
    }

    public class DebugInterceptor : IMethodInterceptor
    {
        public object Invoke(IMethodInvocation invocation)
        {
            Console.WriteLine("Before: " + invocation.Method.ToString());
            object rval = invocation.Proceed();
            Console.WriteLine("After:  " + invocation.Method.ToString());
            return rval;
        }
    }

}
