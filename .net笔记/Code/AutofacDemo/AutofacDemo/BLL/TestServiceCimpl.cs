using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo.BLL
{
    public class TestServiceCimpl : IBLL.ITestServiceC
    {
        public void SayHello(string str)
        {
            Console.WriteLine(str);
        }
    }
}
