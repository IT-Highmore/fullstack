package com.example.fullstacklearningdemo.day03;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring Boot 这是 MyBatis 的 Mapper 接口
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper<User> 即可直接拥有所有的 CRUD 能力，无需手写任何 SQL！
}