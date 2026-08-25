# Day 12：Spring Boot AOP 业务操作日志与异步处理

在生产环境的后台系统中，我们需要记录用户的核心操作日志（如：谁在什么时间、调用了哪个接口、传递了什么参数、请求耗时多久）。为了不让日志记录逻辑入侵核心业务代码，我们将使用 **Spring AOP（面向切面编程）** + **自定义注解**，并使用 **`@Async` 异步线程池** 进行日志记录，避免日志写入阻塞主业务线程。

​

### 今天 Day 12 的核心目标

1. 自定义业务日志注解 `@Log`。
2. 开启 Spring 异步任务支持（`@EnableAsync`）并配置线程池。
3. 编写 AOP 切面 `LogAspect`，拦截带有 `@Log` 的方法，获取请求 IP、方法入参、执行耗时与返回结果。
4. 在关键 Controller 方法上贴上 `@Log` 注解，完成无侵入式业务日志监控。

​

### 第一步：开启异步支持与配置类

在后端新建 `day12` 包。

#### 1. 创建配置类 `AsyncConfig.java`

在 `day12` 包下创建 `AsyncConfig.java`，开启异步支持并定义日志线程池：

```java
package com.example.fullstacklearningdemo.day12;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 👈 开启异步任务支持
public class AsyncConfig {

    @Bean("logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 核心线程数
        executor.setMaxPoolSize(10);     // 最大线程数
        executor.setQueueCapacity(100);  // 缓冲队列
        executor.setThreadNamePrefix("log-async-");
        executor.initialize();
        return executor;
    }
}
```

​

### 第二步：定义注解、日志实体与异步保存服务

#### 1. 自定义日志注解 `Log.java`

在 `day12` 包下创建 `Log.java`：

```java
package com.example.fullstacklearningdemo.day12;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 作用于方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Documented
public @interface Log {
    String value() default ""; // 操作描述，如："新增用户"、"修改用户"
}
```

​

#### 2. 日志模型 `SysLog.java`

在 `day12` 包下创建 `SysLog.java`：

```java
package com.example.fullstacklearningdemo.day12;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysLog {
    private String title;          // 操作描述
    private String method;         // 请求方法
    private String requestUrl;     // 请求 URL
    private String ip;             // 请求 IP
    private String params;         // 请求参数
    private Long executionTime;    // 耗时（毫秒）
    private String username;       // 操作人
    private LocalDateTime createTime; // 操作时间
}
```

​

#### 3. 异步日志记录服务 `LogService.java`

在 `day12` 包下创建 `LogService.java`：

```java
package com.example.fullstacklearningdemo.day12;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    // 👈 声明使用 logExecutor 线程池异步执行，不阻塞主线程
    @Async("logExecutor")
    public void saveLogAsync(SysLog sysLog) {
        // 模拟保存日志数据库或发送到日志中心，打印当前线程名称验证异步效果
        System.out.println("--------------------------------------------------");
        System.out.println("【日志异步记录线程】" + Thread.currentThread().getName());
        System.out.println("【操作模块】" + sysLog.getTitle());
        System.out.println("【操作用户】" + sysLog.getUsername());
        System.out.println("【请求 URL】" + sysLog.getRequestUrl());
        System.out.println("【执行耗时】" + sysLog.getExecutionTime() + " ms");
        System.out.println("--------------------------------------------------");
    }
}
```

​

### 第三步：编写 AOP 切面 `LogAspect.java`

在 `day12` 包下创建 `LogAspect.java`：

```java
package com.example.fullstacklearningdemo.day12;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private LogService logService;

    @Autowired
    private ObjectMapper objectMapper;

    // 环绕切入点：拦截所有贴有 @Log 注解的方法
    @Pointcut("@annotation(com.example.fullstacklearningdemo.day12.Log)")
    public void logPointcut() {}

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行目标业务方法
        Object result = point.proceed();

        long executionTime = System.currentTimeMillis() - startTime;

        // 异步记录日志
        handleLog(point, executionTime);

        return result;
    }

    private void handleLog(ProceedingJoinPoint point, long executionTime) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);

            SysLog sysLog = new SysLog();
            if (logAnnotation != null) {
                sysLog.setTitle(logAnnotation.value());
            }

            if (request != null) {
                sysLog.setRequestUrl(request.getRequestURI());
                sysLog.setIp(request.getRemoteAddr());
            }

            sysLog.setMethod(point.getTarget().getClass().getName() + "." + method.getName() + "()");
            sysLog.setExecutionTime(executionTime);
            sysLog.setCreateTime(LocalDateTime.now());

            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                sysLog.setUsername(authentication.getPrincipal().toString());
            } else {
                sysLog.setUsername("匿名用户");
            }

            // 参数转 JSON 字符串
            sysLog.setParams(objectMapper.writeValueAsString(point.getArgs()));

            // 异步提交给日志服务处理
            logService.saveLogAsync(sysLog);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

​

### 第四步：在 Controller 中加上注解

给 Day 11 的 `UserPageController.java` 加上 `@Log` 注解：

```java
package com.example.fullstacklearningdemo.day11;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstacklearningdemo.day02.Result;
import com.example.fullstacklearningdemo.day12.Log; // 👈 引入注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/day11")
public class UserPageController {

    @Autowired
    private UserPageMapper userPageMapper;

    @GetMapping("/users/page")
    @Log("分页查询用户列表") // 👈 贴上注解
    public Result<IPage<UserVO>> getUserPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(required = false) String name) {

        Page<UserVO> pageParam = new Page<>(page, size);
        IPage<UserVO> userVoPage = userPageMapper.selectUserPageVo(pageParam, name);

        return Result.success(userVoPage);
    }
}
```

​

### 🛠️ 解决方案

在后端的 `pom.xml` 中添加 `spring-boot-starter-aop` 依赖：

```html
<!-- Spring Boot AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 🧪 验证体验

1. 重启 Spring Boot 后端应用。
2. 在前端操作“刷新用户列表”或执行翻页/搜索。
3. 观察 IDEA 控制台输出，会打印类似如下的日志：

​

```text
--------------------------------------------------
【日志异步记录线程】log-async-1
【操作模块】分页查询用户列表
【操作用户】admin
【请求 URL】/api/day11/users/page
【执行耗时】12 ms
--------------------------------------------------
```

​
