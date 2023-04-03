using System;
using System.Diagnostics;

namespace Example_03
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Debugger.Break();
            MyList<int> myList = new MyList<int>();
            myList.Add(10);
            Console.ReadLine();
        }
    }

    public class MyList<T>
    {
        public T[] t = new T[10];

        public void Add(T item)
        {
            t[0] = item;
        }
    }
}
