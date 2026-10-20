package com.example.fullstacklearningdemo.day15;

import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/day15")
@RefreshScope // 👈 关键注解：当 Nacos 配置发生变更时，自动刷新变量
public class NacosConfigController {

    @Value("${app.info:默认本地配置}")
    private String appInfo;

    @GetMapping("/config")
    public Result<String> getDynamicConfig() {
        return Result.success("当前 Nacos 实时动态配置内容为：" + appInfo);
    }
}