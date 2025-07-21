package com.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Redis Web GUI 主应用程序类
 * 
 * @author Redis Web GUI Team
 */
@SpringBootApplication
@EnableScheduling
public class RedisWebGuiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisWebGuiApplication.class, args);
        System.out.println("=================================");
        System.out.println("Redis Web GUI 启动成功!");
        System.out.println("访问地址: http://localhost:8080/redis-gui");
        System.out.println("H2控制台: http://localhost:8080/redis-gui/h2-console");
        System.out.println("=================================");
    }
} 