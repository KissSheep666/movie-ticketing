package com.movieticket.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * API 网关启动类 为什么需要王瓜
 * 微服务架构下 信息进来 通过网关进行转发 多个端口之间微服务之间的调用
 * 1. 统一入口 2. 负载均衡 3. 鉴权 4. 监控 5. 缓存 6. 灰度发布 7.  circuit breaker 8. 限流 9. 熔断 10. 降级 11. 负载均衡 12. 缓存 13. 灰度发布 14.  circuit breaker 15. 限流 16.
 *大概流程逻辑
 *  前端 → http://localhost:8080/api/user/login → Gateway → user-service:8081/api/user/login
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("--------------gateway started---------------");
    }
}
