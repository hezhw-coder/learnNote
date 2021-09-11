package com.he_zhw.DaoImpl;

import com.he_zhw.Dao.StudentDao;
import com.he_zhw.domain.Student;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import java.util.List;

public class StudentDaoImpl extends JdbcDaoSupport implements StudentDao {
    @Override
    public Student getStudentById(int id) {
        BeanPropertyRowMapper<Student> studentBeanPropertyRowMapper = new BeanPropertyRowMapper<>(Student.class);
        return this.getJdbcTemplate().queryForObject("SELECT id, NAME, chinese, english, math FROM student WHERE id=?",studentBeanPropertyRowMapper, id);
    }
}
