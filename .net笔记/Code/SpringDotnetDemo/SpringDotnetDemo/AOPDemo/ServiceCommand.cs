using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.AOPDemo
{
    class ServiceCommand : ICommand
    {
        public object Execute(object context)
        {
            Console.Out.WriteLine($"Service implementation : [{context}]");
            return null;
        }
    }
}
