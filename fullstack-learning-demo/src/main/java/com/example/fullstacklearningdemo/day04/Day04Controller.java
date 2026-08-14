package com.example.fullstacklearningdemo.day04;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fullstacklearningdemo.day02.Result;
import com.example.fullstacklearningdemo.day03.User;
import com.example.fullstacklearningdemo.day03.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day04")
public class Day04Controller {

    @Autowired
    @Qualifier("day04UserService")
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    // 1. 获取用户列表（支持按姓名模糊搜索）
    // GET http://localhost:8080/api/day04/users?name=张
    @GetMapping("/users")
    public Result<List<User>> getUserList(@RequestParam(required = false) String name) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            queryWrapper.like(User::getName, name);
        }
        List<User> list = userMapper.selectList(queryWrapper);
        return Result.success(list);
    }

    // 2. 新增或更新用户
    // POST http://localhost:8080/api/day04/user
    @PostMapping("/user")
    public Result<String> saveOrUpdateUser(@RequestBody User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
            userService.evictUserCache(user.getId()); // 更新时同步清除 Redis 缓存
        }
        return Result.success("保存成功");
    }

    // 3. 删除用户
    // DELETE http://localhost:8080/api/day04/user/1
    @DeleteMapping("/user/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        userService.evictUserCache(id); // 删除时清理缓存
        return Result.success("删除成功");
    }

    // 单条带缓存查询（保留 Day04 接口）
    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserByIdWithCache(id);
        return Result.success(user);
    }
}