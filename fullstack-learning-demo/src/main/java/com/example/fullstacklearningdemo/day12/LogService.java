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