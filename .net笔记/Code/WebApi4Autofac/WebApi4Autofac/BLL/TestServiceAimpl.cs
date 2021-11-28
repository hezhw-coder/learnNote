namespace WebApi4Autofac.BLL
{
    public class TestServiceAimpl : IBLL.ITestServiceA
    {
        public void SayHello(string str)
        {
            Console.WriteLine(str);
        }
    }
}
