package com.movieticket.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * movie-service 启动类。
 * scanBasePackages 扫到 com.movieticket 以加载 common 模块的 Bean。
 */
@SpringBootApplication(scanBasePackages = "com.movieticket")
public class MovieApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieApplication.class, args);
        System.out.println("--------------movie-service started---------------");
    }
}