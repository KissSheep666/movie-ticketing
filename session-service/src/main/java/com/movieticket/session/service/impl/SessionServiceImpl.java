package com.movieticket.session.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.session.entity.Seat;
import com.movieticket.session.entity.Session;
import com.movieticket.session.mapper.SeatMapper;
import com.movieticket.session.mapper.SessionMapper;
import com.movieticket.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 场次业务实现 —— 核心难点在锁座。
 *
 * 锁座流程（Redis 分布式锁 + DB 乐观锁 双保险）：
 *
 *   第1层：Redis 分布式锁（快速拦截）
 *     SETNX lock:seat:{seatId} userId EX 30     ← 30秒锁，防同一座位被同一请求重复处理
 *     失败 → 座位正被别人操作，直接拒绝，不等
 *
 *   第2层：DB 乐观锁（最终一致性）
 *     UPDATE seat SET status=1, version=version+1 WHERE id=? AND status=0 AND version=?
 *     affected rows = 0 → 乐观锁冲突，抛异常
 *
 * 为什么两层：
 *   - 只用 Redis：Redis 挂了锁全丢，多人同时锁到同一座位
 *   - 只用 DB 乐观锁：高并发下大量 UPDATE 冲突 + 重试，性能差
 *   - 两层结合：Redis 挡掉大部分并发冲突（快速失败），DB 保证数据绝对正确
 *
 * 为什么用 SETNX 而不是 Redisson：
 *   这个 Demo 规模小，SETNX 足够。Redisson 提供 WatchDog 自动续期 + 可重入锁，
 *   更适合锁持有时间长（>30s）的场景。这里锁座是毫秒级操作，不需要。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final SeatMapper seatMapper;
    private final StringRedisTemplate redisTemplate;  // 操作 Redis，SETNX 实现分布式锁

    private static final String LOCK_KEY_PREFIX = "lock:seat:";  // Redis key 前缀
    private static final int LOCK_EXPIRE_SECONDS = 30;           // Redis 锁过期时间（秒）
    private static final int SEAT_EXPIRE_MINUTES = 15;           // 座位锁定过期时间（分钟）

    /**
     * 根据电影ID查场次列表，按开始时间升序。
     */
    @Override
    public List<Session> listByMovieId(Long movieId) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getMovieId, movieId)
                .eq(Session::getStatus, 2)      // 只查"售票中"的场次
                .orderByAsc(Session::getStartTime);
        return sessionMapper.selectList(wrapper);
    }

    @Override
    public Session getById(Long id) {
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_FOUND);
        }
        return session;
    }

    /**
     * 查座位图：按排号、列号排序，前端直接渲染成矩阵。
     */
    @Override
    public List<Seat> getSeatsBySessionId(Long sessionId) {
        LambdaQueryWrapper<Seat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seat::getSessionId, sessionId)
                .orderByAsc(Seat::getSeatRow)
                .orderByAsc(Seat::getSeatCol);
        return seatMapper.selectList(wrapper);
    }

    /**
     * 锁座 —— 整个操作一个事务，要么全成功要么全回滚。
     *
     * 为什么 @Transactional：
     *   多个座位的锁操作 + 扣减 available_seats 必须在同一个事务里。
     *   如果第3个座位锁失败，前2个也要回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSeats(Long userId, List<Long> seatIds, Long sessionId) {
        for (Long seatId : seatIds) {
            // ---- 第1层：Redis 分布式锁 ----
            String lockKey = LOCK_KEY_PREFIX + seatId;
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, String.valueOf(userId), LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(acquired)) {
                throw new BizException(BizCodeEnum.SEAT_LOCKED);  // Redis 锁冲突 → 快速失败
            }

            try {
                // ---- 第2层：DB 乐观锁 ----
                // 先查当前 seat 的 version（用于乐观锁条件）
                Seat seat = seatMapper.selectById(seatId);
                if (seat == null || seat.getStatus() != 0) {
                    // 座位不存在或状态不是 AVAILABLE
                    throw new BizException(BizCodeEnum.SEAT_LOCKED);
                }

                // 乐观锁 UPDATE：version 匹配 + status=0 → 才更新
                LocalDateTime expireTime = LocalDateTime.now().plusMinutes(SEAT_EXPIRE_MINUTES);
                int rows = seatMapper.lockSeat(seatId, seat.getVersion(), expireTime);
                if (rows == 0) {
                    // affected rows=0 → version 被其他事务改了，乐观锁冲突
                    throw new BizException(BizCodeEnum.SEAT_LOCKED);
                }
            } catch (BizException e) {
                // DB 锁失败 → 释放 Redis 锁，让其他人可以操作
                redisTemplate.delete(lockKey);
                throw e;  // 事务回滚
            }
            // 成功：Redis 锁会在 30 秒后自动过期，不用手动删
        }

        // 所有座位锁成功 → 扣减场次的 available_seats
        int rows = sessionMapper.updateAvailableSeats(sessionId, -seatIds.size());
        if (rows == 0) {
            throw new BizException(BizCodeEnum.SEAT_SOLD);  // 座位不够了
        }

        log.info("锁座成功: userId={}, seatIds={}, sessionId={}", userId, seatIds, sessionId);
    }

    /**
     * 释放座位：DB 重置 status + Redis 删锁 + 恢复 available_seats。
     * 调用方：用户取消 / XXL-Job 超时释放。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeats(List<Long> seatIds, Long sessionId) {
        for (Long seatId : seatIds) {
            seatMapper.releaseSeat(seatId);
            redisTemplate.delete(LOCK_KEY_PREFIX + seatId);  // 同时清理 Redis 锁
        }
        sessionMapper.updateAvailableSeats(sessionId, seatIds.size());  // 恢复座位数
        log.info("释放座位: seatIds={}, sessionId={}", seatIds, sessionId);
    }
}
