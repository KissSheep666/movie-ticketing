package com.movieticket.gateway.filter;


import com.movieticket.common.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.awt.image.DataBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**全局 jwt 鉴权过滤器
 *
 * 所有数据 进来 之后 只用在网关进行一个处理和解析 后续所有的服务模块 直接拿到的就是解析后的数据
 * 同时可以设置白名单 不需要进行 jwt 鉴权 对所有路由都生效
 *
 * 逻辑链路
 * 请求 -> 过滤器 -> 判断是否是白名单 -> 进行jwt解析 user-id 和 user-name 头
 */

@Slf4j
@Component
//Global Filter 实现接口 执行过滤 方法
//Order 接口 顺序执行 返回顺序值 按照顺序进行过滤处理
public class AuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of(
            "/api/user/register",   // 注册
            "/api/user/login",      // 登录
            "/api/movie/list",      // 电影列表（可浏览不登录）
            "/api/movie/category/list" // 分类列表
    );

    //密钥
    @Value("${jwt.secret}")//从yml文件中获取数据
    private String jwtSecret;



    /**
     * 核心过滤逻辑
     * WebFlux 是非阻塞式的响应式栈 跟 网关 兼容
     *全链路的过滤器链
     *
     * @param exchange 参数理解 理解为 请求时携带的数据 包含目的地之类的
     * @param chain 参数理解 链式调用 过滤链 这一步过了 才能到下一步继续进行
     * @return 返回结果 符合 非阻塞元素的响应式栈
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //从携带的数据中获得 具体的数据(请求头)
        ServerHttpRequest request = exchange.getRequest();
        //从请求头中获得具体的url路径 及 访问地址
        String path = request.getURI().getPath();

        //判断是否是白名单
        if (isWhiteList(path)) {
            //直接放行 过滤链路通过
            //通行后 继续把携带exchange信息的数据传递给下一个过滤器
            return chain.filter(exchange);
        }

        //进行验证
        String anthorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        //对Anthorization 进行验证
        if (anthorization == null && !anthorization.startsWith("Bearer")) {
            log.warn("用户信息解析失败,用户信息不存在");
            //此时应该返回一个json数据给上层 告诉用户没有权限
            return unauthorized(exchange, "用户信息解析失败");
        }

        try {
            //进行解析,校验
            String token = anthorization.substring(7);
            Claims claims = JwtUtil.parseToken(token, jwtSecret);
            //拿到解析后的数据
            String username = claims.get("username", String.class);
            Long userId = Long.valueOf(claims.get("userId", String.class));
            //因为JWTtoken是登录校验通过后菜下发的 所以此时不需要再做一次校验
            //将解析出来的数据 写入请求头 传递给下游的 数据链
            //因为HttpRequest 是immutable的 所以不能直接修改
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-id", userId.toString())
                    .header("X-User-name", username)
                    .build();

            log.info("用户信息解析成功,userID = {} , username = {},path = {}", userId, username, path);
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (NumberFormatException e) {
            log.warn("用户信息解析失败,userId解析失败");
            return unauthorized(exchange, "用户信息解析失败");
        }


    }

    /**
     * 过滤器执行原则值越小的越先执行 设置-100 保证最先执行
     * @return
     */
    @Override
    public int getOrder() {
        return -100;
    }


    /**
     * 判断是否是白名单里的用户
     * @param path
     * @return
     */
    public boolean isWhiteList(String path){
        for (String s : WHITE_LIST) {
            if (path.startsWith(s)){
                return true;
            }
        }
        return false;
    }


    /**
     * 返回JSON相应 这个是WebFlux 框架的 所以不会被异常捕捉器所处理 需要自己处理(GateWay)特殊处理
     * @param exchange
     * @param message
     * @return
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

// 手动拼 JSON 返回体（不用 Jackson，避免引入额外依赖）
        String body = String.format("{\"code\":401,\"msg\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = (DataBuffer) response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith((org.reactivestreams.Publisher<? extends org.springframework.core.io.buffer.DataBuffer>) Mono.just(buffer));
    }
}
