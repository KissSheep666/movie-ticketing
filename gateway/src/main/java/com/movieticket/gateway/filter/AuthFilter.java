package com.movieticket.gateway.filter;

import com.movieticket.common.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权全局过滤器。
 *
 * 为什么鉴权放 Gateway 而不是每个服务自己做：
 *   1. 集中管理：只在一个地方写 JWT 解析逻辑，子服务不用重复代码
 *   2. 安全：外部请求必须先过 Gateway，未登录的请求到不了子服务
 *   3. 解耦：子服务只管从 X-User-Id 头取值，不用关心 JWT 怎么解析的
 *
 * 执行流程：
 *   请求 → AuthFilter → 判断是否公开路径
 *     ├── 公开路径（/api/user/register, /api/user/login）→ 直接放行
 *     └── 其他路径 → 取 Authorization 头 → 解析 JWT
 *           ├── 解析成功 → 加 X-User-Id + X-Username 头 → 放行
 *           └── 解析失败（过期/签名错/无token）→ 返回 401
 *
 * 为什么用 GlobalFilter 而不是 GatewayFilter：
 *   GlobalFilter 对所有路由生效，不需要在每个路由配置里单独声明
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /** 不需要鉴权的白名单路径 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/register",   // 注册
            "/api/user/login",      // 登录
            "/api/movie/list",      // 电影列表（可浏览不登录）
            "/api/movie/category/list" // 分类列表
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 核心过滤逻辑。
     *
     * 为什么用 WebFlux（Mono/ServerWebExchange）而不是 Servlet（HttpServletRequest）：
     *   Spring Cloud Gateway 基于 WebFlux 响应式栈，整个请求链都是非阻塞的。
     *   Servlet 的 HttpServletRequest 是阻塞 IO，和 Gateway 不兼容。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单路径 → 直接放行，不检查 JWT
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 取 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没有 Token 或格式不对 → 401
            return unauthorized(exchange, "未登录或 Token 格式错误");
        }

        // 3. 解析 JWT（剥掉 "Bearer " 前缀，拿到纯 token）
        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.parseToken(token, jwtSecret);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);

            // 4. 将 userId 和 username 写入请求头，透传给下游服务
            //    下游服务直接从 X-User-Id 取值，不用再解析 JWT
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .build();

            log.info("JWT 验证通过: userId={}, username={}, path={}", userId, username, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException | NumberFormatException e) {
            // Token 过期、签名不对、格式错误 → 401
            log.warn("JWT 验证失败: path={}, error={}", path, e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期");
        }
    }

    /**
     * 判断当前路径是否在白名单中。
     * 为什么用 startsWith 而不是 equals：
     *   /api/movie/list?page=1 带查询参数，equals 匹配不上，startsWith 忽略参数
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回 401 JSON 响应。
     * 为什么不抛异常：GlobalExceptionHandler 是 Servlet 体系的，在 WebFlux 下不生效。
     * Gateway 只能手动构造响应字节流返回。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 手动拼 JSON 返回体（不用 Jackson，避免引入额外依赖）
        String body = String.format("{\"code\":401,\"msg\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器执行顺序，值越小越先执行。
     * 设为 -100，确保在其他过滤器之前执行。
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
