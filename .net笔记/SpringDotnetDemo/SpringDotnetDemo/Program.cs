using Spring.Aop.Framework;
using Spring.Context;
using Spring.Context.Support;
using SpringDotnetDemo.AOPDemo;
using System;
using System.Collections;

namespace SpringDotnetDemo
{
    class Program
    {
        static void Main(string[] args)
        {
            IApplicationContext ctx = ContextRegistry.GetContext();
            //UserInfoDal userInfoDal = ctx.GetObject(typeof(UserInfoDal).FullName) as UserInfoDal;
            //userInfoDal.Show();
            #region 代理测试
            //ProxyFactory factory = new ProxyFactory(new ServiceCommand());
            //factory.AddAdvice(new ConsoleLoggingAroundAdvice());
            //ICommand command = (ICommand)factory.GetProxy();
            //command.Execute("This is the argument");
            #endregion
            //ICommand command = (ICommand)ctx["myServiceObject"];
            //command.Execute("This is the argument");
            IDictionary speakerDictionary = ctx.GetObjectsOfType(typeof(IHelloWorldSpeaker));
            foreach (DictionaryEntry entry in speakerDictionary)
            {
                string name = (string)entry.Key;
                IHelloWorldSpeaker worldSpeaker = (IHelloWorldSpeaker)entry.Value;
                Console.Write(name + " says; ");
                worldSpeaker.SayHello();
            }
            //IHelloWorldSpeaker helloWorldSpeaker = (IHelloWorldSpeaker)ctx.GetObject("ItalianSpeakerOne");
            //helloWorldSpeaker.SayHello();
            Console.ReadKey();
        }
    }
}
