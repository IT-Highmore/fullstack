package com.example.fullstacklearningdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.example.fullstacklearningdemo.day03", "com.example.fullstacklearningdemo.day11"})
public class FullstackLearningDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FullstackLearningDemoApplication.class, args);
    }

}
