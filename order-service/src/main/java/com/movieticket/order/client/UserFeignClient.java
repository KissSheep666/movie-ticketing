package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.fallback.UserFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端：调用 user-service 验证用户是否存在。
 */
@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    /** 按 ID 查用户，code=200 即存在 */
    @GetMapping("/api/user/{id}")
    Result<Map<String, Object>> getUserById(@PathVariable("id") Long id);
}
