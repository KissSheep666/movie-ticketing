package com.movieticket.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.movie.entity.Seat;
import com.movieticket.movie.entity.Session;
import com.movieticket.movie.mapper.SeatMapper;
import com.movieticket.movie.mapper.SessionMapper;
import com.movieticket.movie.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final SeatMapper seatMapper;
    private final RedisTemplate<Object, Object> redisTemplate;


    //定义常量参数
    //锁的key
    private static final String LOCk_KEY_PREFIX = "lock:seat:";
    //指定锁的过期时间
    private static final int LOCK_EXPIRE_SECONDS = 30;
    //座位的过期时间,如果过时还没有完成支付,那么就双方订单
    private static final int SEAT_EXPIRE_MINUTES = 15;


    /**
     * 获得某电影的场次列表
     * @param movieId
     * @return
     */
    @Override
    public List<Session> listByMovieId(Long movieId) {
        //根据传进来的电影场次列表,使用构造器进行拼接
        LambdaQueryWrapper<Session> queryWrapper = new LambdaQueryWrapper<>();
        //只查询正在售票中的场次
        //结果按照时间升序排列
        queryWrapper.eq(Session::getMovieId, movieId)
                .eq(Session::getStatus, 2)
                .orderByDesc(Session::getCreateTime);
        return sessionMapper.selectList(queryWrapper);
    }


    /**
     * 获得场次详细信息
     * @param id
     * @return
     */
    @Override
    public Session getById(Long id) {

        Session session = sessionMapper.selectById(id);
        if (session == null){
            throw new BizException(BizCodeEnum.MOVIE_NOT_FOUND);
        }
        return session;
    }

    /**
     * 根据场次id获得座位列表
     * @param sessionId
     * @return
     */
    @Override
    public List<Seat> getSeatsBySessionId(Long sessionId) {
        LambdaQueryWrapper<Seat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Seat::getSessionId, sessionId)
                .orderByAsc(Seat::getSeatRow, Seat::getSeatCol);
        return seatMapper.selectList(queryWrapper);
    }

    /**
     * 锁座 - 需要用事务进行管理,要么成功要么失败
     * 多个座位锁定之间的关系 使用 事务进行绑定
     * @param userId
     * @param sessionId
     * @param seatIds
     *
     *总体逻辑总结
     * 对多个座位进行锁座的操作
     * 在写入的时候 利用redis锁 key 不重复的特性 避免重复 在较短时间内 保证不会超卖
     * 进行 第二层的过滤利用sqol语句的原子性 在写入的时候 对比 版本号 实现了 锁座的实现 以及数据库的写入
     * 还有一个 就是 DB 锁在执行失败后 要手动释放redis 锁 方便执行下一次的操作
     * 同时,都可以根据判断受影响的 行数来判断是否成功
     */
    //指定哪些异常进行回滚
    @Transactional(rollbackFor = Exception.class)
    @Override
    //传入当前用户的id 目标场次 以及 需要的座位
    public void lockSeats(Long userId, Long sessionId, List<Long> seatIds) {
        //对每个座位执行相应的操作
        for (Long seatId : seatIds){
            //锁座 使用分布式锁
            //获得redis的唯一key
            String lockKey = LOCk_KEY_PREFIX + seatId;
            Boolean acquired = redisTemplate
                    .opsForValue()//值操作 操作String
                    .setIfAbsent(lockKey, String.valueOf(userId), LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);//进行redis 键和值的写入 同时设定过期时间
            if (Boolean.FALSE.equals( acquired)){//规范写法 避免出现空指针的现象
                //此时是写入失败 此id的座位以及被占用了
                throw new BizException(BizCodeEnum.SEAT_LOCKED);
            }

            //第二层 锁 乐观锁 双重保险
            //获得座位信息
            try {
                Seat seat = seatMapper.selectById(seatId);
                if (seat == null || seat.getStatus() != 0){
                    //座位信息不合法
                    throw new BizException(BizCodeEnum.SEAT_LOCKED);
                }

                //获得相应的过期时间
                LocalDateTime expireTime = LocalDateTime.now().plusMinutes(SEAT_EXPIRE_MINUTES);
                //进行相应的锁座
                int rows = seatMapper.lockSeat(seatId, seat.getVersion(), expireTime);

                if (rows == 0){
                    //锁座失败
                    throw new BizException(BizCodeEnum.SEAT_LOCKED);
                }
            } catch (BizException e) {
                //释放 redis 锁
                redisTemplate.delete(lockKey);
                throw new RuntimeException(e);
            }
        }

        //所有座位锁成功 扣减 session 的可用座位数
        int i = sessionMapper.updateAvailableSeats(sessionId, -seatIds.size());
        if (i == 0){
            throw new BizException(BizCodeEnum.SEAT_LOCKED);
        }

        log.info("锁座成功");
    }

    /**
     * 释放座位
     * @param sessionId
     * @param seatIds
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void releaseSeats(Long sessionId, List<Long> seatIds) {
        //释放座位 删除redis锁 同时 更新场次可用座位
        for (Long seatId : seatIds){
            String lockKey = LOCk_KEY_PREFIX + seatId;
            redisTemplate.delete(lockKey);
            int i = sessionMapper.releaseSeat();
        }
        //更新session的可用座位
        sessionMapper.updateAvailableSeats(sessionId, seatIds.size());
        log.info("释放座位成功");
    }
}
