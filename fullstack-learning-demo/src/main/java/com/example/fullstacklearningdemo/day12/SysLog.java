package com.example.fullstacklearningdemo.day12;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysLog {
  private String title; // 操作描述
  private String method; // 请求方法
  private String requestUrl; // 请求 URL
  private String ip; // 请求 IP
  private String params; // 请求参数
  private Long executionTime; // 耗时（毫秒）
  private String username; // 操作人
  private LocalDateTime createTime; // 操作时间
}