package com.he_zhw.DaoImpl;

import com.he_zhw.Dao.AccountDao;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class AccountDaoImpl extends JdbcDaoSupport implements AccountDao {


    @Override
    public int updateAccountById(int id,double money) {

/*        UPDATE account
        SET
        id = ? ,
        NAME = ? ，
        money = ? WHERE id = ?
        ;*/
        String sql="update account set money=money+? where id=?";
        return this.getJdbcTemplate().update(sql,money,id);
    }
}
