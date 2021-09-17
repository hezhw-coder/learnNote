using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.Ado.net.Dao
{
    public interface IEmployeeInfoDao
    {
        object ExecuteScalar(string sqlText);
    }
}
