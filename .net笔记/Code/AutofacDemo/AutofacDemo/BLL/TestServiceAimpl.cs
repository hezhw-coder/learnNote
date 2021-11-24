using AutofacDemo.IBLL;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo.BLL
{
    public class TestServiceAimpl : ITestServiceA

    {
        public void Hello(string str)
        {
            Console.WriteLine($"Call by ITestServiceA:{str}");
        }
    }
}
