package com.movieticket.session.controller;

import com.movieticket.common.result.Result;
import com.movieticket.session.entity.Seat;
import com.movieticket.session.entity.Session;
import com.movieticket.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场次控制器。
 *
 * 接口设计：
 *   GET    /api/session/list?movieId=1           某电影的场次列表（公开）
 *   GET    /api/session/{id}                     场次详情（公开）
 *   GET    /api/session/{id}/seats               座位图（公开，选座页用）
 *   POST   /api/session/lock                     锁座（需登录，Gateway 透传 X-User-Id）
 *   POST   /api/session/release                  释放座位（需登录）
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /** 根据电影ID查场次列表 */
    @GetMapping("/list")
    public Result<List<Session>> list(@RequestParam Long movieId) {
        List<Session> list = sessionService.listByMovieId(movieId);
        return Result.success(list);
    }

    /** 场次详情 */
    @GetMapping("/{id}")
    public Result<Session> getById(@PathVariable Long id) {
        Session session = sessionService.getById(id);
        return Result.success(session);
    }

    /** 座位图 */
    @GetMapping("/{id}/seats")
    public Result<List<Seat>> seats(@PathVariable Long id) {
        List<Seat> seats = sessionService.getSeatsBySessionId(id);
        return Result.success(seats);
    }

    /** 锁座：用户选中座位 → 锁定15分钟 → 去下单 */
    @PostMapping("/lock")
    public Result<?> lock(@RequestHeader("X-User-Id") Long userId,
                          @RequestParam List<Long> seatIds,
                          @RequestParam Long sessionId) {
        sessionService.lockSeats(userId, seatIds, sessionId);
        return Result.success(null);
    }

    /** 释放座位：用户取消锁座 / 取消订单时调用 */
    @PostMapping("/release")
    public Result<?> release(@RequestParam List<Long> seatIds,
                              @RequestParam Long sessionId) {
        sessionService.releaseSeats(seatIds, sessionId);
        return Result.success(null);
    }
}
