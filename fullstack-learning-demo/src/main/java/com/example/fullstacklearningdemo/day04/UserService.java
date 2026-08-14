package com.example.fullstacklearningdemo.day04;

import com.example.fullstacklearningdemo.day03.User;
import com.example.fullstacklearningdemo.day03.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service("day04UserService")
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis Key 前缀规范：业务名:模块名:主键
    private static final String USER_CACHE_PREFIX = "user:info:";

    // 1. 带缓存的查询用户详情
    public User getUserByIdWithCache(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;

        // Step 1: 先查 Redis 缓存
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            System.out.println(">>> 🎯 命中 Redis 缓存，直接返回！");
            return cachedUser;
        }

        // Step 2: 缓存未命中，查询 MySQL 数据库
        System.out.println(">>> 🔍 未命中缓存，查询 MySQL 数据库...");
        User user = userMapper.selectById(id);

        // Step 3: 若 DB 存在，写入 Redis 并设置 10 分钟过期时间（防止过期数据长期堆积）
        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);
        }

        return user;
    }

    // 2. 更新/清理缓存（数据更新时保持一致性）
    public void evictUserCache(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
        System.out.println(">>> 🗑️ 已清理 Redis 用户缓存: " + cacheKey);
    }
}