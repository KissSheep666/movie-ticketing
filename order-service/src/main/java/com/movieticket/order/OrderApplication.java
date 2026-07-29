package com.movieticket.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * order-service 启动类。
 *
 * @EnableFeignClients 告诉 Spring 扫描 @FeignClient 接口，
 * 让 order-service 可以远程调用 user-service 和 session-service。
 */
@SpringBootApplication(scanBasePackages = "com.movieticket")
@EnableFeignClients(basePackages = "com.movieticket.order.client")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
