package com.movieticket.movie.controller;

import com.movieticket.common.result.Result;
import com.movieticket.movie.entity.Seat;
import com.movieticket.movie.entity.Session;
import com.movieticket.movie.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场次控制器。
 *
 * 接口设计：
 *   GET    /api/session/list?movieId=1      某电影的场次列表（公开）
 *   GET    /api/session/{id}                场次详情（公开）
 *   GET    /api/session/{id}/seats          座位图（公开，选座页用）
 *   POST   /api/session/lock                锁座（需登录，取 X-User-Id）
 *   POST   /api/session/release             释放座位（需登录）
 */
//获得相应电影的场次列表 场次详情 座位图 锁座处理 释放座位(利用mysql的原子操作进行实现)

    @RequiredArgsConstructor
    @RestController
    @RequestMapping("/api/session")
    public class SessionController {

        private final SessionService sessionService;

    @GetMapping("/list")
    public Result<List<Session>> list(@RequestParam Long movieId) {
        List<Session> list = sessionService.listByMovieId(movieId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Session> getById(@PathVariable Long id) {
        Session session = sessionService.getById(id);
        return Result.success(session);
    }

    @GetMapping("/{id}/seats")
    public Result<List<Seat>> seats(@PathVariable Long id) {
        List<Seat> seats = sessionService.getSeatsBySessionId(id);
        return Result.success(seats);
    }

    @PostMapping("/lock")
    public Result<?> lock(@RequestHeader("X-User-Id") Long userId,
                          @RequestParam List<Long> seatIds,
                          @RequestParam Long sessionId) {
        sessionService.lockSeats(userId,sessionId,seatIds);
        return Result.success(null);
    }

    @PostMapping("/release")
    public Result<?> release(@RequestParam List<Long> seatIds,
                             @RequestParam Long sessionId) {
        sessionService.releaseSeats(sessionId, seatIds);
        return Result.success(null);
    }

}
