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