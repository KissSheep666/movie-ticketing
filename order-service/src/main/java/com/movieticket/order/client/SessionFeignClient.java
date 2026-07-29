package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.fallback.SessionFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端：调用 session-service 查场次信息 + 锁座/释放。
 */
@FeignClient(name = "session-service", fallback = SessionFeignClientFallback.class)
public interface SessionFeignClient {

    /** 查场次详情（获取票价等信息） */
    @GetMapping("/api/session/{id}")
    Result<Map<String, Object>> getSessionById(@PathVariable("id") Long id);

    /** 确认座位已售（下单成功后） */
    @PostMapping("/api/session/confirm")
    Result<?> confirmSeats(@RequestParam List<Long> seatIds,
                           @RequestParam Long sessionId);

    /** 释放座位（取消订单时） */
    @PostMapping("/api/session/release")
    Result<?> releaseSeats(@RequestParam List<Long> seatIds,
                           @RequestParam Long sessionId);
}
