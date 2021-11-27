using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TypeConverterDemo
{
    [TypeConverter(typeof(MyTypeConverter))]
    public class Student
    {
        public string StuId { get; set; }
        public string StuName { get; set; }
    }
    public enum StudentType
    {
        A,
        B,
        C
    }
}
