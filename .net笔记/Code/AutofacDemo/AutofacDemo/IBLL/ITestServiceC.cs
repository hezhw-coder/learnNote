using Autofac.Extras.DynamicProxy;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo.IBLL
{
    [Intercept(typeof(AOPDemo.CustomAutofacAop))]
    public interface ITestServiceC
    {
        public void SayHello(string str);
    }
}
