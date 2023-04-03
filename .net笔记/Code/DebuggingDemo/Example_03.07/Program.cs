using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Example_03._07
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Debugger.Break();
            Person person = new Person();
            person.Name = "jack";
            person.Age = 18;
            Console.ReadLine();
        }
    }

    public class Person
    {
        public int Age { get; set; }

        public string Name { get; set; }
    }
}
