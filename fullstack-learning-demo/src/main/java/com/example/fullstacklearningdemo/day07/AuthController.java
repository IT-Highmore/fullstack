package com.example.fullstacklearningdemo.day07;

import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    // 1. 登录接口（演示硬编码密码 admin / 123456）
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        if ("admin".equals(username) && "123456".equals(password)) {
            String token = jwtUtils.generateToken(username);
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("username", username);
            return Result.success(data);
        }

        return Result.error(401, "用户名或密码错误");
    }
}