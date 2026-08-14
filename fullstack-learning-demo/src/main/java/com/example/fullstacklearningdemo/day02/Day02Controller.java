package com.example.fullstacklearningdemo.day02;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/day02") // 统一前缀
public class Day02Controller {

    // 1. 正常接口：返回统一的 Result 结构
    @GetMapping("/user/{id}")
    public Result<Map<String, Object>> getUser(@PathVariable("id") Long id) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("nickname", "全栈玩家");
        user.put("role", "Fullstack Developer");

        // 用 Result.success 包裹返回
        return Result.success(user);
    }

    // 2. 模拟异常接口：故意抛出一个 RuntimeException 报错
    @GetMapping("/test-error")
    public Result<Void> testError() {
        // 模拟业务报错（例如数据库查无此人、或空指针）
        throw new RuntimeException("用户余额不足，无法完成支付！");
    }
}