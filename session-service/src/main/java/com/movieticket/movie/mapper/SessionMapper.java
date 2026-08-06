package com.movieticket.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.movie.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    /**
     * 原子更新剩余座位表(-1.+1)
     * 在一个sql语句里 是具备原子性的
     *
     * MySQL 行锁 + 原子更新的组合保证并发安全
     */

    /**
     *  @Update("UPDATE session SET available_seats = available_seats + #{delta} " +
     *             "WHERE id = #{sessionId} AND available_seats + #{delta} >= 0")
     */
    int updateAvailableSeats(@Param("sessionId") Long sessionId, @Param("delta") int delta);

    int releaseSeat();
}
