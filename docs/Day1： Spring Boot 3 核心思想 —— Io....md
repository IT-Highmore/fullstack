# Day1: Spring Boot 3 核心思想 —— IoC 与 DI

💡 15 分钟概念映射（前端 vs 后端）

作为前端开发者，你肯定写过类似的代码：

```typescript
// 传统前端写法：手动实例化依赖
import { ApiService } from './ApiService';

class UserController {
  private apiService = new ApiService(); // 🚫 自己 new 出来的依赖，耦合度高，难做单元测试
}
```

而在 Java/Spring 的世界里，核心思想是 **控制反转（IoC, Inversion of Control）** 和 **依赖注入（DI, Dependency Injection）**：

* **IoC（控制反转）**：把对象的创建、生命周期管理和销毁的控制权，从你的代码手⾥交给了 **Spring 容器**。Spring 容器就像一个全局的大型“对象池/依赖树”。
* **DI（依赖注入）**：你需要什么对象，不需要自己 `new`，只需要在类上打个注解（如 `@Autowired`），Spring 就会自动把实例化好的对象“塞（注入）”给你。这非常类似于前端 Angular 的依赖注入，或者 React Context / Vue Provide-Inject 的全局依赖共享思想。

​

### 🛠️ 30 分钟动手实战

#### 1. 认识核心注解（Bean 的注册与注入）

在 Spring Boot 中，常用以下注解来标记一个类是 Spring 管理的 **Bean**（即托管对象）：

| **注解**            | **作用领域**         | **前端/Node.js 对标概念**                |
| ----------------- | ---------------- | ---------------------------------- |
| `@RestController` | 控制器层（处理 HTTP 请求） | Express Router / NestJS Controller |
| `@Service`        | 业务逻辑层（纯业务代码）     | Business Service                   |
| `@Repository`     | 数据访问层（操作数据库）     | ORM Model / DAO                    |
| `@Component`      | 通用组件/工具类         | Utility / Helper                   |
| `@Autowired`      | 依赖注入             | `inject()` / Context Consumer      |

​

#### 2. 代码演练：编写你的第一个 IoC/DI 模块

**第一步：定义业务服务层 (Service)**

```java
package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service // 1. 告诉 Spring：这是一个业务组件，请帮你实例化并放入 IoC 容器
public class UserService {

    public String getUserRole(Long userId) {
        // 模拟业务逻辑
        if (userId == 1L) {
            return "ADMIN (超级管理员)";
        }
        return "DEVELOPER (前端工程师)";
    }
}
```

**第二步：定义控制器层 (Controller) 并注入 Service**

```java
package com.example.demo.controller;

import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController // 标记为 HTTP 控制器，返回 JSON 格式
public class UserController {

    // 2. 自动注入 UserService，无需手动 new UserService()
    @Autowired
    private UserService userService;

    @GetMapping("/api/user/role")
    public String getUserRole(@RequestParam Long id) {
        // 3. 直接调用被注入的对象方法
        return userService.getUserRole(id);
    }
}
```

​

### 15 分钟复盘与思考

1. **思考题**：在传统前端开发中，如果 `AClass` 依赖 `BClass`，`BClass` 依赖 `CClass`，我们需要挨个实例化它们。在 Spring 中，如果 `UserService` 依赖了 `OrderService`，`OrderService` 依赖了 `PayService`，Spring 是如何帮我们处理这种依赖关系的？
2. **动手尝试**：如果你本地配置好了 Java 环境（JDK 17/21 + IntelliJ IDEA），尝试新建一个 Spring Boot 项目，尝试给 `UserService` 添加构造函数，观察 Spring 是怎么自动完成依赖注入的。

​

## ⚠️ `@RestController` vs `@Controller`（前端极易踩坑的点）

在 Spring 中有两个用于标记控制器的注解，区别非常关键：

1. **`@RestController`**（推荐/接口开发专用）：
   * `@RestController` = `@Controller` + `@ResponseBody`
   * 作用：方法的 `return` 值直接作为 **数据** 返回给前端（JSON / Text）。
2. **`@Controller`**（传统 MVC 模版引擎渲染）：
   * 作用：方法的 `return` 值代表 **视图/页面路径**（例如 `return "index"` 会去寻找 `index.html` 页面文件，而不是输出字符串 `"index"`）。

> **总结：** 做前后端分离架构（Vue/React + Spring Boot）时，后端写 API 接口**统一使用 `@RestController`** 即可！
