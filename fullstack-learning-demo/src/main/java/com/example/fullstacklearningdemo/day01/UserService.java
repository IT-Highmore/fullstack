package com.example.fullstacklearningdemo.day01;

import org.springframework.stereotype.Service;

@Service("day01UserService") // 1. 告诉 Spring 容器：请帮我实例化这个类，并管理它的生命周期 (IoC)
public class UserService {

    public String getUserRole(Long userId) {
        // 模拟简单的业务逻辑
        if (userId != null && userId == 1L) {
            return "ADMIN (超级管理员)";
        }
        return "DEVELOPER (全栈工程师)";
    }
}