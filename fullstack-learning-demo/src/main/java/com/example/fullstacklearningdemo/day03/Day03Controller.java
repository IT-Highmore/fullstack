package com.example.fullstacklearningdemo.day03;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day03")
public class Day03Controller {

    @Autowired
    private UserMapper userMapper;

    // 1. 新增用户: POST /api/day03/user
    @PostMapping("/user")
    public Result<User> createUser(@RequestBody User user) {
        userMapper.insert(user); // 自动执行 INSERT INTO sys_user...
        return Result.success(user); // 会自动带回生成自增的 id
    }

    // 2. 条件查询（查询大于指定年龄的用户列表）: GET /api/day03/users?minAge=18
    @GetMapping("/users")
    public Result<List<User>> getUsersOverAge(@RequestParam(defaultValue = "0") Integer minAge) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(User::getAge, minAge)             // age > minAge
                .orderByDesc(User::getId);            // 按 id 降序排序

        List<User> users = userMapper.selectList(wrapper);
        return Result.success(users);
    }

    // 3. 逻辑删除用户: DELETE /api/day03/user/1
    @DeleteMapping("/user/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        // 实际上执行的是 UPDATE sys_user SET is_deleted = 1 WHERE id = ?
        return Result.success("删除成功 (已标记逻辑删除)");
    }
}