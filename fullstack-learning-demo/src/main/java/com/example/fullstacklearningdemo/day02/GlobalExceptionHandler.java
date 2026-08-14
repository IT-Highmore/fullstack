package com.example.fullstacklearningdemo.day02;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 告诉 Spring：这个类用来捕获所有 Controller 抛出的未处理异常
public class GlobalExceptionHandler {

    // 捕获所有运行时异常 (RuntimeException)
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        // 将未捕获的崩溃/异常，优雅包装为 { code: 500, message: "...", data: null } 返回给前端
        return Result.error(500, "服务器异常：" + e.getMessage());
    }
}