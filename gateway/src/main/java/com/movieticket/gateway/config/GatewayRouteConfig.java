package com.movieticket.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 路由配置。
 *
 * 路由逻辑（前端只和 Gateway:8080 通信）：
 *   /api/user/**    → lb://user-service     （Nacos 负载均衡 → 找 user-service 实例）
 *   /api/movie/**   → lb://movie-service
 *   /api/session/** → lb://session-service
 *   /api/order/**   → lb://order-service
 *
 * 为什么用 lb:// 而不是 http://localhost:8081：
 *   lb = LoadBalancer，从 Nacos 发现所有健康的服务实例，自动负载均衡。
 *   如果 user-service 有 3 个实例，lb://user-service 会轮询分发请求。
 *   如果用 http://localhost:8081，写死了地址，服务挂了都不知道。
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f.stripPrefix(0))  // 不剥离前缀，原样转发
                        .uri("lb://user-service"))

                // 电影服务
                .route("movie-service", r -> r
                        .path("/api/movie/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://movie-service"))

                // 场次服务
                .route("session-service", r -> r
                        .path("/api/session/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://session-service"))

                // 订单服务
                .route("order-service", r -> r
                        .path("/api/order/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://order-service"))

                .build();
    }
}
