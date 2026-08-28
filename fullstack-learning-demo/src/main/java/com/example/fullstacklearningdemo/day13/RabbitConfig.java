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