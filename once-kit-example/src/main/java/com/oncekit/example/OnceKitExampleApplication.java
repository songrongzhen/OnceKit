package com.oncekit.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OnceKit 幂等中间件 示例启动类
 */
@SpringBootApplication
public class OnceKitExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnceKitExampleApplication.class, args);
    }
}