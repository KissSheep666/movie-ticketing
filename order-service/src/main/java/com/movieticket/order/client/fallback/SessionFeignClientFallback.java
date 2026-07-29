package com.movieticket.order.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.SessionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SessionFeignClient 降级：场次服务不可用时的兜底响应。
 */
@Slf4j
@Component
public class SessionFeignClientFallback implements SessionFeignClient {

    @Override
    public Result<Map<String, Object>> getSessionById(Long id) {
        log.warn("SessionFeignClient 降级: getSessionById({})", id);
        return Result.error(500, "场次服务暂不可用，请稍后重试");
    }

    @Override
    public Result<?> confirmSeats(List<Long> seatIds, Long sessionId) {
        log.warn("SessionFeignClient 降级: confirmSeats(sessionId={})", sessionId);
        return Result.error(500, "场次服务暂不可用，请稍后重试");
    }

    @Override
    public Result<?> releaseSeats(List<Long> seatIds, Long sessionId) {
        log.warn("SessionFeignClient 降级: releaseSeats(sessionId={})", sessionId);
        return Result.error(500, "场次服务暂不可用，请稍后重试");
    }
}
