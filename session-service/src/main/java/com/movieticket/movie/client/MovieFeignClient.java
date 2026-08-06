package com.movieticket.movie.client;

import com.movieticket.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端 调用movie-session
 *
 * Feign 声明式调用 写一个接口和注解就行了
 *
 *
 *
 */
@FeignClient(name = "session-service",
            fallback =  MovieFeignClientFallback.class) // 指定降级策略
public interface MovieFeignClient {
    //调用方法 判断电影是否存


    //查询电影是否存在
    @GetMapping("/api/movie/{id}")
    Result<Map<String , Object>> getMovieById(@PathVariable("id") Long id);
}
