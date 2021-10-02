using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SpringDotnetDemo.Ado.net.Dao
{
    public interface IEmployeeInfoDao
    {
        /// <summary>
        /// 执行sql返回首行首列
        /// </summary>
        /// <param name="sqlText"></param>
        /// <returns></returns>
        object ExecuteScalar(string sqlText);

        /// <summary>
        /// 执行非查询语句
        /// </summary>
        /// <param name="sqlText"></param>
        /// <returns></returns>
        int ExecuteNonQuery(string sqlText);
    }
}
