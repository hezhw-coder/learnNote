using AutofacDemo.IBLL;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo.BLL
{
    public class TestServiceBimpl : ITestServiceB

    {
        //private readonly ITestServiceA testServiceA1;
        //public TestServiceBimpl(ITestServiceA testServiceA1)
        //{
        //    this.testServiceA1 = testServiceA1;
        //}
        public ITestServiceA TestServiceA1 { get; set; }
        public void Hello(string str)
        {
            //testServiceA1.Hello($"Call by ITestServiceA:{str}");
            TestServiceA1.Hello($"Call by ITestServiceA:{str}");
            Console.WriteLine($"Call by ITestServiceB:{str}");
        }
    }
}
