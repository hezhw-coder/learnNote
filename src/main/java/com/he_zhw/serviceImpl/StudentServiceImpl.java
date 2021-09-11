package com.he_zhw.serviceImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import com.he_zhw.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("studentService")
public class StudentServiceImpl implements StudentService {

@Autowired
    private StudentDao studentDao;

/*    public void setStudentDao(StudentDao studentDao) {
        this.studentDao = studentDao;
    }*/

    @Override
    public Student getStudentById(int id) {
        System.out.println("888");
        return studentDao.getStudentById(id);
    }
}
