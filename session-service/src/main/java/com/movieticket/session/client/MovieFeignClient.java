package com.movieticket.session.client;

import com.movieticket.common.result.Result;
import com.movieticket.session.client.fallback.MovieFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端：调用 movie-service。
 *
 * 为什么用 Feign 而不是 RestTemplate：
 *   Feign 声明式调用，写一个接口 + 注解就行，不用手动拼 URL、序列化、反序列化。
 *   底层用动态代理生成 HTTP 请求代码，跟调用本地方法一样自然。
 *
 * 为什么 name = "movie-service"：
 *   这是 Nacos 注册中心里 movie-service 的服务名，
 *   Feign + LoadBalancer 自动从 Nacos 发现所有 movie-service 实例并负载均衡。
 */
@FeignClient(name = "movie-service", fallback = MovieFeignClientFallback.class)
public interface MovieFeignClient {

    /**
     * 查询电影详情，验证电影是否存在。
     * 返回格式：Result<Map>，code=200 即存在。
     */
    @GetMapping("/api/movie/{id}")
    Result<Map<String, Object>> getMovieById(@PathVariable("id") Long id);
}
