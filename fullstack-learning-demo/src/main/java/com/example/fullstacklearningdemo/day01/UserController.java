package com.example.fullstacklearningdemo.day01;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController // 1. 标记为控制器，返回 JSON 或文本数据
public class UserController {

    @Autowired // 2. 依赖注入：告诉 Spring 自动把 UserService 的实例塞进来 (DI)
    @Qualifier("day01UserService")
    private UserService userService;

    // 3. 暴露 GET 接口: /api/day01/role?id=1
    @GetMapping("/api/day01/role")
    public String getUserRole(@RequestParam(value = "id", required = false) Long id) {
        return userService.getUserRole(id);
    }
}