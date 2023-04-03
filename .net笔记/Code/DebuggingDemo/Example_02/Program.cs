using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;

namespace Example_02
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Debugger.Break();
            Console.WriteLine("查看未被Jit编译的方法");
            Sum(1,2);
            Debugger.Break();
            Console.WriteLine("查看被Jit编译的方法");
            Sum(1, 2);
        }

        public static  int Sum(int a,int b)
        {
            return a + b;
        }
    }
}
