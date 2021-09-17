using Spring.Data.Core;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.Ado.net.Dao
{
    public class EmployeeInfoDao : AdoDaoSupport, IEmployeeInfoDao
    {
        public object ExecuteScalar(string sqlText)
        {
            return this.AdoTemplate.ExecuteScalar(System.Data.CommandType.Text,sqlText);
        }
    }
}
