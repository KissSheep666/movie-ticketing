package com.movieticket.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * session-service 启动类。
 *
 * 为什么需要 @EnableFeignClients：
 *   场次服务需要调用 movie-service 验证电影是否存在（Feign 远程调用），
 *   这个注解告诉 Spring 扫描 @FeignClient 接口并生成代理实现类。
 */
@SpringBootApplication(scanBasePackages = "com.movieticket")
@EnableFeignClients(basePackages = "com.movieticket.session.client")
public class SessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionApplication.class, args);
    }
}
