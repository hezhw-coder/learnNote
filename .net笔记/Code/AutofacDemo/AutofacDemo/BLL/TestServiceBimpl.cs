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

        private ITestServiceA TestServiceA2;

        public void Hello(string str)
        {
            //testServiceA1.Hello($"Call by ITestServiceA:{str}");
            TestServiceA1.Hello($"Call by ITestServiceA:{str}");
            //TestServiceA2.Hello($"Call by {TestServiceA2.GetType()}:{str}");
            Console.WriteLine($"Call by ITestServiceB:{str}");
        }

        public void SetService(ITestServiceA TestServiceA)
        {
            this.TestServiceA2 = TestServiceA;
        }
    }
}
