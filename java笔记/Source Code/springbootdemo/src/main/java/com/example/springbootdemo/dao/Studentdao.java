package com.example.springbootdemo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springbootdemo.domain.Student;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface Studentdao extends BaseMapper<Student> {

}
