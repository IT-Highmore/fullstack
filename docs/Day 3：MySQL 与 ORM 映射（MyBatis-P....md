# Day 3：MySQL 与 ORM 映射（MyBatis-Plus 实战）

对于前端开发者而言，操作数据库（MySQL）其实并不陌生——只要你用过 Node.js 生态中的 **Prisma**、**TypeORM** 或 **Sequelize**，就能瞬间理解 Java 中的 **ORM (Object-Relational Mapping)** 概念。

今天我们学习中国国内企业级 Java 开发中最流行、效率极高的 ORM 框架：**MyBatis-Plus**。

​

### 💡 15 分钟概念映射（前端 vs 后端）

#### 什么是 ORM？

* **关系型数据库（MySQL）**：数据存储是以表（Table）**和**行（Row）的形式存在的。
* **面向对象语言（Java/TS）**：数据是以类（Class）**和**对象（Object）的形式存在的。
* **ORM 的任务**：作为“中间翻译官”，自动把 Java 中的一个类映射为 MySQL 的一张表，把一个对象映射为表中的一行记录。

​

#### 前后端 ORM 框架对标

| **概念**                 | **前端/Node.js 生态 (如 Prisma)**    | **Java 生态 (MyBatis-Plus)**          |
| ---------------------- | ------------------------------- | ----------------------------------- |
| **定义数据模型**             | `schema.prisma` 中的 `model User` | 打了 `@TableName` 注解的 Java 类 (Entity) |
| **数据库操作接口**            | `prisma.user.findMany()`        | 继承 `BaseMapper<User>` 的 Mapper 接口   |
| **主键自增策略**             | `@id @default(autoincrement())` | `@TableId(type = IdType.AUTO)`      |
| **逻辑删除 (Soft Delete)** | 手动过滤 `deletedAt == null`        | 注解 `@TableLogic`（框架自动处理软删过滤）        |

​

🛠️ 30 分钟动手实战

假设我们在 MySQL 中创建了一张用户表 `sys_user`：

```sql
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100),
    is_deleted INT DEFAULT 0 COMMENT '0-未删除, 1-已删除'
);
```

#### 1. 定义实体类 (Entity)

在 Java 中创建一个类与表 `sys_user` 进行绑定：

```java
package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_user") // 告诉 MyBatis-Plus 这个类映射 MySQL 的 sys_user 表
public class User {

    @TableId(type = IdType.AUTO) // 标记主键，自增策略
    private Long id;

    private String name;

    private Integer age;

    private String email;

    @TableLogic // 开启逻辑删除（软删除功能）
    private Integer isDeleted;
}
```

**2. 创建 Mapper 接口（零 SQL 实现 CRUD）**

传统的 MyBatis 需要写大量繁琐的 XML 文件和 SQL 语句。但使用了 **MyBatis-Plus** 之后，你只需要定义一个接口继承 `BaseMapper<T>`，就能凭空拥有完整的增删改查能力！

```java
package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 没错！这里完全不需要写任何代码和 SQL！
    // 父类 BaseMapper 已经内置了 insert, deleteById, updateById, selectList 等十几个方法。
}
```

**3. 在 Service/Controller 中直接调用 CRUD**

```java
package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    // 1. 新增用户
    public void createUser(String name, Integer age, String email) {
        User user = new User();
        user.setName(name);
        user.setAge(age);
        user.setEmail(email);
        userMapper.insert(user); // 自动生成并执行 INSERT INTO sys_user...
    }

    // 2. 条件查询（类似于 Prisma 的 where）
    public List<User> getUsersOverAge(Integer minAge) {
        // 使用 LambdaQueryWrapper，杜绝硬编码列名，提供强类型语法检查
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(User::getAge, minAge) // age > minAge
               .like(User::getEmail, "@gmail.com"); // email LIKE '%@gmail.com%'

        return userMapper.selectList(wrapper);
    }

    // 3. 逻辑删除用户
    public void deleteUser(Long id) {
        userMapper.deleteById(id); 
        // 因为标记了 @TableLogic，它不会真执行 DELETE FROM，
        // 而是自动执行 UPDATE sys_user SET is_deleted = 1 WHERE id = id !
    }
}
```

​

### 🧠 15 分钟复盘与思考

1. **逻辑删除（Soft Delete）的好处**：为什么企业生产环境很少使用物理删除（`DELETE FROM`），而是普遍采用逻辑删除（修改状态标记）？前端在处理逻辑删除的数据时，一般需要做特殊处理吗？
2. **连接池概念**：前端单线程 Node.js 常用单连接或简单池，而 Java 后端常用 **HikariCP**（Spring Boot 默认的高性能数据库连接池）。数据库连接池的主要目的是解决什么开销？

​

### 1. 为什么企业生产环境普遍采用“逻辑删除（Soft Delete）”？

**物理删除**是真正执行 `DELETE FROM sys_user WHERE id = 1`，数据在磁盘上被彻底抹去。

**逻辑删除**则是执行 `UPDATE sys_user SET is_deleted = 1 WHERE id = 1`，数据还在，只是被打上了“废弃”标签。

#### 为什么企业不敢用物理删除？

1. **防止数据误删（“删库跑路”兜底方案）**：
   * 现实业务中，用户误操作（如“我不小心删除了我的订单/文档”），或者后台管理员误操作的情况极其频繁。如果是逻辑删除，后台只需改一下 `is_deleted = 0`，数据瞬间恢复；如果是物理删除，就必须通过复杂的数据库备份日志（Binlog）去拉取恢复，代价巨大。
2. **保障数据关联完整性（外键与历史账单）**：
   * 假设用户购买了商品，生成了一张订单。如果几年后商家在后台把这个商品**物理删除**了，那么用户在查看历史订单时，关联的商品信息就会变成 `null` 甚至导致整个订单列表页面报错崩掉。
   * 用逻辑删除，历史订单依然能拉取到商品名称和价格，前端展示时只需显示一个灰色标签：`[该商品已下架]`。
3. **数据审计与大数据分析（数据就是资产）**：
   * 用户注销账号或者删除了行为日志，并不代表这些数据没有价值。企业的推荐算法、风控系统、财务报表仍然需要这些历史数据来进行建模和数据分析。

#### 前端需要关注什么？

* **完全无感**：只要后端使用了像 MyBatis-Plus 的 `@TableLogic` 注解，**前端不需要做任何特殊处理**！
* 当你调用 `GET /users` 时，MyBatis-Plus 会自动在所有 SQL 后面默默加上 `WHERE is_deleted = 0`。前端拿到的列表天生就是未删除的数据。

​

### 2. 数据库连接池（如 HikariCP）解决的是什么开销？

在理解连接池之前，我们要先看**不使用连接池**时，每次前端发一个请求，后端去读写 MySQL 会发生什么：

#### 传统模式（一次请求 = 一次建立/销毁连接）：

```text
[前端请求] ➔ [后端接收] ➔ [向 MySQL 发起 TCP 三次握手] ➔ [MySQL 身份验证/权限校验]
                          ➔ [执行 SQL 并返回数据]
                          ➔ [向 MySQL 发起 TCP 四次挥手断开连接]
```

* **痛点所在**：建立一个 MySQL 连接的开销**极其昂贵**！每次连接都需要网络握手、分配内存、验证身份。如果 TCP 建立握手耗时 **50ms**，而真正执行 SQL 查询只花了 **2ms**，那么 90% 以上的时间都浪费在“建连接”和“销毁连接”上了。
* 如果同时有 1000 个并发请求过来，服务器会因为瞬间频繁创建/销毁连接而直接 CPU 爆满或崩溃。

​

#### 连接池模式（HikariCP / 池化思想）：

连接池的思想和前端的**线程池、DOM 对象池**一模一样。

```text
[预先创建好 10 个闲置的数据库连接]
                      │
[前端请求 A] ────> 借用连接 #1 ➔ 执行 SQL ➔ 归还连接 #1 (不断开 TCP)
[前端请求 B] ────> 借用连接 #2 ➔ 执行 SQL ➔ 归还连接 #2 (不断开 TCP)
```

1. **复用连接，零握手开销**：应用启动时，连接池就预先和 MySQL 建立好一批 TCP 连接（比如 10 到 20 个）。有请求来了直接“借”一个用，用完“还”回去，TCP 连接**永远不断开**。响应时间从几十毫秒瞬间降到几毫秒！
2. **限流防压垮（保护 MySQL）**：MySQL 的承载能力是有限的。如果连接池最大只给 50 个连接，当来 1000 个并发请求时，超出部分会在连接池队列里排队等待，而不会一下子把 1000 个请求全部塞给 MySQL，从而**保护数据库不会被瞬时高并发冲垮**。

​
