package com.he_zhw.serviceImpl;

import com.he_zhw.Dao.AccountDao;
import com.he_zhw.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("AccountService")
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao accountDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public int transfer(int TransferorID, int recipientId, double money) {
        int i = accountDao.updateAccountById(TransferorID, -100);
        //int j=1/0;
        i=accountDao.updateAccountById(recipientId,100);
        return i;
    }
}
