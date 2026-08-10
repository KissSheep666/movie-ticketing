package com.movieticket.order.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.SessionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SessionFeignClientFallBack implements SessionFeignClient {
    @Override
    public Result<Map<String, Object>> getSessionById(Long id) {
        log.warn("SessionFeignClient 降级: getSessionById({})", id);
        return Result.error(500, "服务降级");
    }

    @Override
    public Result<?> confirmSeats(Long sessionId, List<Long> seatIds) {
        log.warn("SessionFeignClient 降级: confirmSeats({}, {})", sessionId);
        return Result.error(500, "服务降级");
    }

    @Override
    public Result<?> lockSeats(List<Long> seatIds, Long sessionId) {
        log.warn("SessionFeignClient 降级: lockSeats({}, {})", seatIds);
        return Result.error(500, "服务降级");
    }

    @Override
    public Result<?> releaseSeats(List<Long> seatIds, Long sessionId) {
        log.warn("SessionFeignClient 降级: releaseSeats({}, {})", seatIds);
        return Result.error(500, "服务降级");
    }
}
