package com.movieticket.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关启动类。
 *
 * 为什么需要网关：
 *   微服务架构下，user-service:8081、movie-service:8082、session-service:8083、order-service:8084
 *   前端不可能记住四个地址。网关统一入口（8080），根据路径转发到对应服务。
 *
 *   前端 → http://localhost:8080/api/user/login → Gateway → user-service:8081/api/user/login
 *   前端 → http://localhost:8080/api/movie/list  → Gateway → movie-service:8082/api/movie/list
 *
 * 为什么用 Spring Cloud Gateway 而不是 Zuul：
 *   Zuul 1.x 已停维护，Zuul 2.x 生态弱。Gateway 基于 WebFlux（响应式），性能更好，Spring 官方主力。
 *
 * 注意：Gateway 基于 WebFlux，不能引入 spring-boot-starter-web（会冲突）。
 *       这里不 scan common 包，因为 common 的 GlobalExceptionHandler 是 Servlet 体系的，
 *       在 WebFlux 下不生效（Gateway 需要自己的异常处理）。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
