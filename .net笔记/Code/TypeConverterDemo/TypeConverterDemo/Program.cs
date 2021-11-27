// See https://aka.ms/new-console-template for more information
using System.ComponentModel;
using TypeConverterDemo;

Student student = new Student();
student.StuId = "125";
student.StuName = "he_zhw";
TypeConverter typeConverter = TypeDescriptor.GetConverter(typeof(Student));
bool v1 = typeConverter.CanConvertTo(typeof(string));//判断是否能将Student类型转换成string类型，如果CanConvertTo参数的为string类型的，则默认返回True
object? v = typeConverter.ConvertTo(student, typeof(string));//将Student类型转换成对应类型，如果子类未重写ConvertTo则默认转换成类型ToString后的字符串
//Student student1 = new Student();
//object? v1 = typeConverter.CanConvertFrom(student.GetType());
//StudentType a = StudentType.A;
//TypeConverter typeConverter = TypeDescriptor.GetConverter(typeof(StudentType));
//bool v1 = typeConverter.CanConvertFrom(a.GetType());
Console.WriteLine(v1);




