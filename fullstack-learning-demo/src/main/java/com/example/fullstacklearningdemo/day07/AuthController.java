package com.example.fullstacklearningdemo.day07;

import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    // 1. 登录接口（演示硬编码密码 admin / 123456）
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        List<String> permissions;

        if ("admin".equals(username) && "123456".equals(password)) {
            // 超级管理员：完整 CRUD 权限
            permissions = Arrays.asList("user:list", "user:add", "user:edit", "user:delete");
        } else if ("test".equals(username) && "123456".equals(password)) {
            // 测试普通用户：仅有列表查询权限
            permissions = Arrays.asList("user:list");
        } else {
            return Result.error(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(username, permissions);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("permissions", permissions); // 返回给前端持久化保存

        return Result.success(data);
    }
}