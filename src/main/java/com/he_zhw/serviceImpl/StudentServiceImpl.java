package com.he_zhw.serviceImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import com.he_zhw.service.StudentService;

public class StudentServiceImpl implements StudentService {
    private StudentDao studentDao;

    public void setStudentDao(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    @Override
    public Student getStudentById(int id) {
        return studentDao.getStudentById(id);
    }
}
