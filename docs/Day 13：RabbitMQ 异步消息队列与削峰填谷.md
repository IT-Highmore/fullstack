# **Day 13：RabbitMQ 异步消息队列与削峰填谷**

在前面的 Day 12 中，我们使用了 Spring 内置的线程池实现了内存级的异步日志处理。

但在分布式与微服务架构中，处理海量高并发业务（如发送注册/通知邮件、订单创建、数据同步、秒杀削峰）时，单机的线程池容易因服务重启丢失数据或撑爆内存。今天我们将引入工业级消息中间件 —— **RabbitMQ**。

​

### 今天 Day 13 的核心目标

1. 使用 **Docker 部署 RabbitMQ** 消息队列服务（带有 Web 管理控制台）。
2. 在 Spring Boot 中集成 `spring-boot-starter-amqp` 依赖。
3. 声明交换机（Exchange）、队列（Queue）以及绑定关系（Binding）。
4. 实现生产者（Producer）发送异步操作日志消息，以及消费者（Consumer）监听队列消费消息。

​

### 第一步：使用 Docker 部署 RabbitMQ

在终端运行以下命令，启动带有 Web 管理界面的 RabbitMQ 容器：

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management
```

* **5672**：RabbitMQ 的 AMQP 通信端口。
* **15672**：RabbitMQ 后台 Web 管理界面端口（浏览器访问 `http://localhost:15672`，账号/密码均为 `guest`）。

###

### 第二步：后端集成 RabbitMQ

#### 1. 修改 `pom.xml`

添加 Spring Boot AMQP 依赖并刷新 Maven：

```html
<!-- Spring Boot AMQP (RabbitMQ) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

​

#### 2. 在 `application.properties` 配置 RabbitMQ 账户

在 `application.properties` 中追加配置：

```properties
# RabbitMQ 配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

​

### 第三步：编写 RabbitMQ 配置与消息监听组件

在后端新建 **`day13`** 包。

#### 1. 编写配置类 `RabbitConfig.java`

在 `day13` 包下创建 `RabbitConfig.java`，定义直连交换机（Direct Exchange）、队列及绑定：

```java
package com.example.fullstacklearningdemo.day13;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String LOG_EXCHANGE = "system.log.exchange";
    public static final String LOG_QUEUE = "system.log.queue";
    public static final String LOG_ROUTING_KEY = "system.log.routingKey";

    // 1. 声明交换机
    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(LOG_EXCHANGE, true, false);
    }

    // 2. 声明队列
    @Bean
    public Queue logQueue() {
        return new Queue(LOG_QUEUE, true);
    }

    // 3. 绑定队列到交换机
    @Bean
    public Binding logBinding(Queue logQueue, DirectExchange logExchange) {
        return BindingBuilder.bind(logQueue).to(logExchange).with(LOG_ROUTING_KEY);
    }
}
```

#### 2. 修改 Day 12 的 `LogService.java`（作为生产者发送消息）

修改 `day12/LogService.java`，使用 `RabbitTemplate` 替换原本的内存线程池：

```java
package com.example.fullstacklearningdemo.day12;

import com.example.fullstacklearningdemo.day13.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 生产者：将日志对象转为 JSON 序列化消息投递到 RabbitMQ
    public void saveLogAsync(SysLog sysLog) {
        try {
            String logJson = objectMapper.writeValueAsString(sysLog);
            rabbitTemplate.convertAndSend(
                    RabbitConfig.LOG_EXCHANGE,
                    RabbitConfig.LOG_ROUTING_KEY,
                    logJson
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 3. 编写消费者监听类 `LogMessageConsumer.java`

在 `day13` 包下创建 `LogMessageConsumer.java`：

```java
package com.example.fullstacklearningdemo.day13;

import com.example.fullstacklearningdemo.day12.SysLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LogMessageConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    // 消费者：实时监听 system.log.queue 队列中的消息并进行异步消费处理
    @RabbitListener(queues = RabbitConfig.LOG_QUEUE)
    public void receiveLogMessage(String messageJson) {
        try {
            SysLog sysLog = objectMapper.readValue(messageJson, SysLog.class);
            System.out.println("==================================================");
            System.out.println("【RabbitMQ 消费者监听到新消息】");
            System.out.println("【操作模块】" + sysLog.getTitle());
            System.out.println("【操作用户】" + sysLog.getUsername());
            System.out.println("【请求路径】" + sysLog.getRequestUrl());
            System.out.println("【耗时记录】" + sysLog.getExecutionTime() + " ms");
            System.out.println("==================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

​

### 🧪 验证体验

1. 确保 RabbitMQ Docker 容器正常运行（可通过 `http://localhost:15672` 登录验证）。
2. 重新启动后端 `FullstackLearningDemoApplication`。在启动日志中可以看到 RabbitMQ 自动建好了队列 `system.log.queue` 与交换机 `system.log.exchange`。
3. 在前端再次操作翻页或检索。
4. 观察 IDEA 控制台输出：原本切面截获的日志不再通过单机异步线程处理，而是通过 RabbitMQ 完成了**解耦投递与消息消费**！
