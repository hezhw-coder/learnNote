using Spring.Stereotype;
using System;
using System.Collections.Generic;
using System.Text;


namespace SpringDotnetDemo
{

    public class UserInfoDal 
    {
        public string Name
        {
            get;
            set;
        }

        public void Show()
        {
            //Console.WriteLine(this.Stutent);

            Console.WriteLine("阿大声道"+Name);
        }
    }
}
