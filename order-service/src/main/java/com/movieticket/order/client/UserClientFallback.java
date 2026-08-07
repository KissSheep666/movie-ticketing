package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class UserClientFallback implements UserFeignClient{
    @Override
    public Result<Map<String, Object>> getUserById(Long id) {
        log.warn("UserFeignClient 降级: getUserById({})", id);
        return Result.error(500, "服务降级");
    }
}
