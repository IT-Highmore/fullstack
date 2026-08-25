package com.example.fullstacklearningdemo.day12;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 作用于方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Documented
public @interface Log {
  String value() default ""; // 操作描述，如："新增用户"、"修改用户"
}