package com.movieticket.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.session.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 场次 Mapper。
 * availableSeats 用自定义 SQL 更新，保证原子性。
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    /**
     * 原子更新剩余座位数（锁座 -1，释放 +1）。
     * 为什么要原子 UPDATE 而不是先 SELECT 再 UPDATE：
     *   两个并发锁座请求同时 SELECT available_seats=10，都减1，都 UPDATE 回 9
     *   → 实际只减了1个座位，数据不一致。
     *   用 available_seats = available_seats + delta 让 MySQL 的写锁保证原子性。
     */
    @Update("UPDATE session SET available_seats = available_seats + #{delta} WHERE id = #{sessionId} AND available_seats + #{delta} >= 0")
    int updateAvailableSeats(@Param("sessionId") Long sessionId, @Param("delta") int delta);
}
