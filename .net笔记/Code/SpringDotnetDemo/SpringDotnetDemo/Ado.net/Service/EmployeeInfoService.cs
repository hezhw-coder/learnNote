using SpringDotnetDemo.Ado.net.Dao;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.Ado.net.Service
{
    public class EmployeeInfoService : IEmployeeInfoService
    {
        public EmployeeInfoDao EmployeeInfoDao { get; set; }

        public int updateEmployeeInfo(string employeeID)
        {
            string sql = $"update com_employee t set t.empl_name='系统员1' where t.empl_code='{employeeID}'";
            int v = EmployeeInfoDao.ExecuteNonQuery(sql);
            //int i = 0;
            //int v1 = 9 / i;
            return v;
        }
    }
}
