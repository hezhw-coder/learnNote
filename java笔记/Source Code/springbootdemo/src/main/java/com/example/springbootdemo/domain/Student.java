package com.example.springbootdemo.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@TableName("student")
public class Student {

    private  String id;
    private  String name;
    private  String chinese;
    private  String english;
    private  String math;


}
