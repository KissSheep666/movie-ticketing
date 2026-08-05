package com.movieticket.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由规则定义 路径 - 服务名的映射
 *
 * 完成前端发送请求和服务鸣之间的映射 同时利用lb 实现负载均衡  如果一个服务有多个实例 则会自动进行负载均衡
 */

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){
        RouteLocator build = builder.routes()
                //用户服务
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
        return build;

    }
}
