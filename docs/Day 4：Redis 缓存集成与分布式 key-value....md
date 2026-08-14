# Day 4：Redis 缓存集成与分布式 key-value 设计

在真实开发中，我们绝不会让所有查询都直接打到 MySQL 上。今天我们要利用 **Redis** 为数据查询加上一层高性能缓存，体验“高并发、低延迟”的后端架构设计！

​

**准备工作：确保 Redis 依赖与 Docker 容器就绪**

1. **更新 `docker-compose.yml` 引入 Redis**： 在根目录的 `docker-compose.yml` 中追加 redis 服务：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.4-oracle
    container_name: mp-mysql
    environment:
      MYSQL_ROOT_PASSWORD: "Root@123456"
      MYSQL_DATABASE: "mp_demo"
      TZ: "Asia/Shanghai"
    ports:
      - "3307:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d
    restart: always

  redis:
    image: redis:alpine
    container_name: mp-redis
    ports:
      - "6379:6379"
    restart: always

volumes:
  mysql-data:
```

_打开终端运行 `docker compose up -d`，确保 Redis 容器已启动。_

2. **在 `pom.xml` 中引入 Redis 依赖**：

```yaml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

加完后点击 IDEA 右上角 **Maven 刷新图标**。

3. **配置 Redis 连接**：

在 `application.yml`（或 `application.properties`）中添加 Redis 配置：

```properties
# Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

​

**第一步：让 `User` 实体类支持序列化**

打开 `day03/User.java`，让它实现 `Serializable` 接口，并加一个序列化版本号：

```java
package com.example.fullstacklearningdemo.day03;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

@TableName("sys_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String name;
    private Integer age;
    private String email;

    // Getter 和 Setter 方法保持不变
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

**📂 第二步：创建 Day 04 目录与 Service 层**

我们按照标准的后端三层架构（Controller -> Service -> Mapper），在 Service 层实现 **“先查 Redis 缓存，未命中再查 MySQL 并写入缓存”** 的旁路缓存（Cache-Aside Pattern）模式。

1. 在 `src/main/java/com/example/fullstacklearningdemo/` 下新建包 **`day04`**。
2. 在 `day04` 下新建 **`UserService.java`**：

```java
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
```

### 🚀 第二步：创建 Day04Controller 接口

在 `day04` 包下创建 `Day04Controller.java`：

```java
package com.example.fullstacklearningdemo.day04;

import com.example.fullstacklearningdemo.day02.Result;
import com.example.fullstacklearningdemo.day03.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/day04")
public class Day04Controller {

    @Autowired
    private UserService userService;

    // GET /api/day04/user/1
    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserByIdWithCache(id);
        return Result.success(user);
    }

    // DELETE /api/day04/user/cache/1  (手动清理缓存测试接口)
    @DeleteMapping("/user/cache/{id}")
    public Result<String> clearCache(@PathVariable Long id) {
        userService.evictUserCache(id);
        return Result.success("缓存清理成功");
    }
}
```

### 🧪 3 分钟验证体验

1. 启动项目 `FullstackLearningDemoApplication`。
2. 确保之前在 MySQL 的 `sys_user` 表里已有 `id = 1` 的数据（若没有，可用 Day 03 的接口先加一条）。
3. **第一次访问** `http://localhost:8080/api/day04/user/1`：
   * 观察 IDEA 终端控制台，会打印出：`>>> 🔍 未命中缓存，查询 MySQL 数据库...`
4. **第二次（及后续多次）刷新访问** `http://localhost:8080/api/day04/user/1`：
   * 观察控制台，会瞬间打印出：`>>> 🎯 命中 Redis 缓存，直接返回！`
5. **测试删除缓存**：用 Postman 或浏览器/终端访问 `DELETE http://localhost:8080/api/day04/user/cache/1`，再次查询时就会重新走 MySQL 查询。

```bash
url -X DELETE http://localhost:8080/api/day04/user/cache/1
```

这就是经典的 **Cache-Aside 缓存读写模式**。

![](attachment/1.webp)

​
