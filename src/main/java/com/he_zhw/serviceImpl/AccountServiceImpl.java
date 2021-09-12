package com.he_zhw.serviceImpl;

import com.he_zhw.Dao.AccountDao;
import com.he_zhw.service.AccountService;

public class AccountServiceImpl implements AccountService {
    private AccountDao accountDao=null;

    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public int transfer(int TransferorID, int recipientId, double money) {
        int i = accountDao.updateAccountById(TransferorID, -100);
        //int j=1/0;
        i=accountDao.updateAccountById(recipientId,100);
        return i;
    }
}
