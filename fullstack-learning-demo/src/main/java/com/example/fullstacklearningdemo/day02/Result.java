package com.example.fullstacklearningdemo.day02;

import lombok.Data;

@Data // Lombok 自动生成 Getter/Setter
public class Result<T> {
    private Integer code;      // 业务状态码 (如 200 成功, 500 失败)
    private String message;   // 提示信息
    private T data;           // 泛型数据载体
    private long timestamp;   // 响应时间戳

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // 快捷方式：成功响应
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 快捷方式：失败响应
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}