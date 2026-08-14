# Day2：RESTful API 与统一响应封装

在前端开发中，你一定经常用 Axios 调用后端的接口。如果后端的接口数据格式千奇百怪——有的失败返回 `{ err: 1 }`，有的返回 `{ status: "fail", msg: "error" }`，甚至有时直接抛出一个 500 HTML 报错页面，前端的 Axios 拦截器（Interceptors）就会写得非常痛苦。

今天我们就从**前端消费者**和**后端生产者**的双重视角，看看如何用 Spring Boot 设计出规范、优雅的统一响应结构。

​

💡 15 分钟概念映射（前端视角）

在现代前后端分离架构中，前端最希望看到的接口响应通常具备**一致的外壳**：

```typescript
// 前端理想中的 Axios 统一接收类型定义
interface ApiResponse<T> {
  code: number;      // 业务状态码 (例如: 200 成功, 401 未登录, 50001 参数错误)
  message: string;   // 提示信息
  data: T;           // 业务数据载体 (泛型)
  timestamp: number; // 响应时间戳
}
```

后端的核心任务，就是利用 **Java 泛型** 和 **全局异常处理器**，无论程序是正常运行还是发生异常，都将结果统一包裹成上面这种格式返回。

​

🛠️ 30 分钟动手实战

**1. 定义统一响应结果类 `Result<T>`**

在后端项目中，我们首先创建一个泛型封装类：

```java
package com.example.demo.common;

import lombok.Data;

@Data // Lombok 注解：自动生成 Getter/Setter/toString 等方法
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // 成功时的快捷构造方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 失败时的快捷构造方法
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}
```

​

#### 2. 在 Controller 中使用统一封装

有了 `Result<T>` 之后，Controller 的方法返回值类型就统一为 `Result<T>`：

```java
package com.example.demo.controller;

import com.example.demo.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users") // 规范：使用名词复数作为路由基径
public class UserController {

    // GET /api/v1/users/1001
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getUserById(@PathVariable("id") Long id) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", "张三");
        user.put("role", "全栈工程师");

        // 自动装箱为 Result 结构返回
        return Result.success(user);
    }
}
```

**前端 Axios 拿到的实际 JSON 为：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "name": "张三",
    "role": "全栈工程师"
  },
  "timestamp": 1723500000000
}
```

**​**

**3. 进阶：配置全局异常拦截（防止暴露原始 500 报错）**

如果业务代码中触发了 `NullPointerException`（空指针异常）或者自定义业务异常，如果不做处理，Spring Boot 会默认给前端返回一个 500 的 HTML 页面或包含堆栈信息的 JSON，这对前端很不友好且存在安全隐患。

我们可以通过 `@RestControllerAdvice` 兜底捕获所有异常:

```java
package com.example.demo.exception;

import com.example.demo.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 全局异常拦截器（AOP 思想）
public class GlobalExceptionHandler {

    // 拦截所有的 RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        // 记录日志...
        return Result.error(500, "服务器开小差了：" + e.getMessage());
    }
}
```

​

​

### 🧠 15 分钟复盘与思考

1. **RESTful 规范对标**：前端在使用 Axios 时，通常用 `GET` 获取资源，`POST` 新建资源，`PUT` 更新资源，`DELETE` 删除资源。在 Spring Boot 中分别对应 `@GetMapping`、`@PostMapping`、`@PutMapping` 和 `@DeleteMapping`。
2. **思考题**：在 HTTP 状态码层面，统一响应结构中的 `code`（业务状态码，如 200/50001）和 HTTP Response 自身的 Status Code（如 200 OK/404 Not Found/500 Server Error）有什么区别？为什么很多公司喜欢将 HTTP Status Code 统一设为 200，而在 JSON 的 `code` 中体现具体业务错误？

​

## HTTP 状态码 vs 业务 code：核心区别

| **维度**  | **HTTP 状态码 (Response Status Code)**                               | **业务 Code (JSON 里的 code)**                  |
| ------- | ----------------------------------------------------------------- | ------------------------------------------- |
| **定义者** | 互联网标准协议（W3C / IETF）                                               | **你的公司 / 团队内部** 自由约定                        |
| **关注点** | **网络与传输层** 的状态（请求通不通？网络对不对？网关在不在？）                                | **具体业务逻辑** 的状态（余额够不够？验证码过没过期？密码错了几次？）       |
| **代表值** | `200 OK`, `401 Unauthorized`, `404 Not Found`, `500 Server Error` | `200` (成功), `20001` (余额不足), `40002` (账号被冻结) |

## 💡 为什么很多公司倾向于“HTTP 统一返回 200”，用 JSON 里的 `code` 区分？

你可能在工作中遇到过：Axios 只要遇到 HTTP 4xx 或 5xx，就会直接抛出 Exception，触发 `catch` 块或者被 Axios 响应拦截器强制中断。

很多架构师选择 **HTTP 统一返回 200，在 JSON 内区分业务错误**，主要有以下 4 个工程考量：

### 1. 业务错误的粒度远比 HTTP 状态码丰富

HTTP 状态码有限（常用的就十几种）。但现实中的业务错误千奇百怪：

* 密码错误（剩 2 次机会） -> `code: 10001`
* 密码错误（账号已被锁定） -> `code: 10002`
* 优惠券已失效 -> `code: 30005`

如果全挤在 HTTP `400` 或 `422` 里，前端只看 HTTP 状态码根本无法做精准的 UI 提示，还是得解析 Body 里的数据。

### 2. 避免基础设施（网关/CDN/运营商）劫持

在早期的移动网络或某些代理/CDN 架构中，如果后端接口返回了 HTTP `404` 或 `500`，**中间的 CDN 或运营商可能会拦截这个响应**，直接替换成运营商自定义的“网页不存在”提示 HTML，导致前端 Axios 根本拿不到后端返回的 JSON 错误信息。统一返回 `200` 能够保障 JSON 报文顺利穿透网关和 CDN 到达前端。

### 3. 前端 Axios 拦截器编写更简单统一

如果 HTTP 状态码始终是 200：

```typescript
// 前端 Axios 响应拦截器
axios.interceptors.response.use(response => {
  const { code, message, data } = response.data;
  
  if (code === 200) {
    return data; // 直接返回业务数据
  }
  
  if (code === 40100) {
    // 业务约定：token 签名失效，无感刷新或跳转登录
    router.push('/login');
  } else {
    // 统一弹窗 Toast 提示业务错误
    ElMessage.error(message);
  }
  
  return Promise.reject(new Error(message));
});
```

前端只需要在一个 `then` 流程里统一处理所有业务状态，不需要在 `try/catch` 或 Axios 的 `error` 回调里去艰难解析响应体。

## ⚖️ 两种主流架构风格的对比

业界其实分为两大阵营，没有绝对的对错，关键在于团队约定：

### 模式 A：纯粹 RESTful 风格（严格使用 HTTP 状态码）

* **做法**：HTTP Status Code 准确反映问题。比如参数错给 `400`，未登录给 `401`，无权限给 `403`，服务器报错给 `500`。
* **优点**：符合 RFC 语义标准，对 API 网关、监控系统（如 Prometheus、Datadog）非常友好。监控系统看 HTTP 500 比例就能直接告警。
* **缺点**：前端 Axios 拦截器需要同时处理 `response` 成功流和 `error` 错误流，且 HTTP 状态码语义有限。

### 模式 B：RPC / 业务包装风格（HTTP 200 + 业务 code，大厂常用）

* **做法**：只要请求正常到达了服务器并完成了业务处理（即使业务校验失败），HTTP 状态码统统给 `200 OK`，具体的成功/失败全看 JSON 里的 `code`。
* **优点**：前后端交互逻辑极度统一，错误信息传输稳定，不受基础设施干扰。
* **缺点**：破坏了 HTTP 协议语义；网关层监控 HTTP 状态码时，会发现全都是 `200`，需要深度解析 JSON Body 才能做业务告警。

> **全栈小总结**：
>
> 作为全栈开发，你在设计新项目时，**推荐采用模式 B（或混合模式：网络/系统级异常用 HTTP 4xx/5xx，业务级错误用 HTTP 200 + 自定义 code）**。这样能大大减少前后端关于接口联调的沟通成本！
