package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import com.movieticket.order.client.fallback.UserClientFallback;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserFeignClient {
    //完成对客户端的调用

    @GetMapping("/api/user/{id}")
    Result<Map<String,Object>> getUserById(@PathVariable Long id);
}
