package com.movieticket.order.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class UserClientFallback implements UserFeignClient {
    @Override
    public Result<Map<String, Object>> getUserById(Long id) {
        log.warn("UserFeignClient 降级: getUserById({})", id);
        return Result.error(500, "服务降级");
    }
}
