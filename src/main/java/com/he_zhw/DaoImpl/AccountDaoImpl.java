package com.he_zhw.DaoImpl;

import com.he_zhw.Dao.AccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class AccountDaoImpl extends JdbcDaoSupport implements AccountDao {

    @Autowired
    public void setDaoDataSource(DataSource DataSource) {
        this.setDataSource(DataSource);
    }

    @Override
    public int updateAccountById(int id, double money) {

/*        UPDATE account
        SET
        id = ? ,
        NAME = ? ，
        money = ? WHERE id = ?
        ;*/
        String sql = "update account set money=money+? where id=?";
        return this.getJdbcTemplate().update(sql, money, id);
    }
}
