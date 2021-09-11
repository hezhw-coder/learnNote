package com.he_zhw.DaoImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;


import javax.sql.DataSource;
import java.util.List;


public class StudentDaoImpl extends JdbcDaoSupport implements StudentDao {

    /**//*@Autowired
    public void setDataSource1(DataSource dataSource){
        this.setDataSource(dataSource);
    }*/

    @Override
    public Student getStudentById(int id) {
        BeanPropertyRowMapper<Student> studentBeanPropertyRowMapper = new BeanPropertyRowMapper<>(Student.class);
        return this.getJdbcTemplate().queryForObject("SELECT id, NAME, chinese, english, math FROM student WHERE id=?",studentBeanPropertyRowMapper, id);
    }
}
