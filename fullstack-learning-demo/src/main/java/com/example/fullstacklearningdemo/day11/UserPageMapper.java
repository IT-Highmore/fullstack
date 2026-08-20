package com.example.fullstacklearningdemo.day11;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstacklearningdemo.day03.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPageMapper extends BaseMapper<User> {

  // 自定义多表分页查询：将 Page 对象作为第一个参数，MyBatis-Plus 会自动注入分页逻辑
  @Select("SELECT u.id, u.name, u.age, u.email, '超级管理员' AS role_name " +
      "FROM sys_user u " +
      "WHERE u.is_deleted = 0 AND (#{name} IS NULL OR u.name LIKE CONCAT('%', #{name}, '%'))")
  IPage<UserVO> selectUserPageVo(Page<UserVO> page, @Param("name") String name);
}