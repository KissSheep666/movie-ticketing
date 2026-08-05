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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Wrapper;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;

    private final SeatMapper seatMapper;
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
     */
    //指定哪些异常进行回滚
    @Transactional(rollbackFor = Exception.class)
    @Override
    //传入当前用户的id 目标场次 以及 需要的座位
    public void lockSeats(Long userId, Long sessionId, List<Long> seatIds) {

    }

    @Override
    public void releaseSeats(Long sessionId, List<Long> seatIds) {

    }
}
