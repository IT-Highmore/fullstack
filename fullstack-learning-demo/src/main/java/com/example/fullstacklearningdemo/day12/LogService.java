//package com.example.fullstacklearningdemo.day12;
//
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//@Service
//public class LogService {
//
//  // 👈 声明使用 logExecutor 线程池异步执行，不阻塞主线程
//  @Async("logExecutor")
//  public void saveLogAsync(SysLog sysLog) {
//    // 模拟保存日志数据库或发送到日志中心，打印当前线程名称验证异步效果
//    System.out.println("--------------------------------------------------");
//    System.out.println("【日志异步记录线程】" + Thread.currentThread().getName());
//    System.out.println("【操作模块】" + sysLog.getTitle());
//    System.out.println("【操作用户】" + sysLog.getUsername());
//    System.out.println("【请求 URL】" + sysLog.getRequestUrl());
//    System.out.println("【执行耗时】" + sysLog.getExecutionTime() + " ms");
//    System.out.println("--------------------------------------------------");
//  }
//}

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