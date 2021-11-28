using Autofac.Extras.DynamicProxy;
using AutofacDemo.IBLL;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo.BLL
{
    [Intercept(typeof(AOPDemo.CustomAutofacAop))]
    public class TestServiceDimpl : IBLL.ITestServiceD
    {
        public virtual void SayHello(string str)
        {
            Console.WriteLine(str);
        }

        public  void SayHi(string str)
        {
           Console.WriteLine(str);
        }
    }
}
