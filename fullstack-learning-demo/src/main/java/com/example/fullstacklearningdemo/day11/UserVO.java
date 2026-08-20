package com.example.fullstacklearningdemo.day11;

import lombok.Data;

@Data
public class UserVO {
  private Long id;
  private String name;
  private Integer age;
  private String email;
  private String roleName; // 多表 Join 查出来的角色名称（如：超级管理员/普通用户）
}