package com.movieticket.session.service;

import com.movieticket.session.entity.Seat;
import com.movieticket.session.entity.Session;

import java.util.List;

/**
 * 场次业务接口。
 */
public interface SessionService {

    /**
     * 根据电影ID查询该电影的所有场次。
     * 为什么用 movieId 查询：用户先选电影 → 看到该电影有哪些场次可选。
     */
    List<Session> listByMovieId(Long movieId);

    /**
     * 查场次详情（含所有座位）。
     */
    Session getById(Long id);

    /**
     * 查某场次的座位图（所有座位按排号分组）。
     */
    List<Seat> getSeatsBySessionId(Long sessionId);

    /**
     * 锁座：扣减座位 + 标记座位为 LOCKED。
     *
     * @param userId    用户ID（记录谁锁的，后续 order 用）
     * @param seatIds   要锁定的座位ID列表
     * @param sessionId 场次ID
     * @throws com.movieticket.common.exception.BizException 座位不可锁时抛
     */
    void lockSeats(Long userId, List<Long> seatIds, Long sessionId);

    /**
     * 释放座位（用户取消或超时释放）。
     */
    void releaseSeats(List<Long> seatIds, Long sessionId);
}
