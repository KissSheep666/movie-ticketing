package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import com.movieticket.order.client.fallback.SessionFeignClientFallBack;

@FeignClient(name = "session-service", fallback = SessionFeignClientFallBack.class)
public interface SessionFeignClient {

    //查询场次详情
    @GetMapping("/api/session/{id}")
    Result<Map<String, Object>> getSessionById(@PathVariable Long id);


    //确认座位已售
    @PostMapping("/api/session/confirm")
    Result<?> confirmSeats(@PathVariable Long sessionId, @PathVariable List<Long> seatIds);

    /** 确认座位已售（支付成功后） */
    @PostMapping("/api/session/lock")
    Result<?> lockSeats(@RequestParam List<Long> seatIds,
                           @RequestParam Long sessionId);

    /** 释放座位（取消订单时） */
    @PostMapping("/api/session/release")
    Result<?> releaseSeats(@RequestParam List<Long> seatIds,
                           @RequestParam Long sessionId);

}
