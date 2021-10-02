using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.AOPDemo
{
    public interface ICommand
    {
        object Execute(object context);

    }
}
