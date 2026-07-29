package com.movieticket.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.session.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 座位 Mapper。
 * 锁座和释放需要带乐观锁条件的自定义 SQL，不能简单用 MyBatis-Plus 的 updateById。
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 乐观锁锁座：只有当 status=0（可用）且 version 匹配时才更新。
     * affected rows = 1 → 锁成功，= 0 → 座位已被别人锁了。
     *
     * 为什么用乐观锁而不是悲观锁（SELECT ... FOR UPDATE）：
     *   乐观锁不阻塞读（用户查看座位图不受影响），冲突时上层重试即可。
     *   悲观锁在锁座期间阻塞所有读，电影院场景不划算——锁座冲突率低，不需要那么重。
     */
    @Update("UPDATE seat SET status = 1, expire_time = #{expireTime}, version = version + 1 " +
            "WHERE id = #{seatId} AND status = 0 AND version = #{version}")
    int lockSeat(@Param("seatId") Long seatId,
                 @Param("version") Integer version,
                 @Param("expireTime") java.time.LocalDateTime expireTime);

    /**
     * 释放座位：将 status 重置为 0，清除过期时间。
     * 调用场景：用户取消锁座、超时未支付（XXL-Job 定时扫）。
     */
    @Update("UPDATE seat SET status = 0, expire_time = NULL, version = version + 1 " +
            "WHERE id = #{seatId} AND status = 1")
    int releaseSeat(@Param("seatId") Long seatId);

    /**
     * 确认售出：将已锁定的座位标记为已售。
     */
    @Update("UPDATE seat SET status = 2, expire_time = NULL, version = version + 1 " +
            "WHERE id = #{seatId} AND status = 1")
    int confirmSeat(@Param("seatId") Long seatId);

    /**
     * 查询已过期的锁定座位（供 XXL-Job 定时释放用）。
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM seat WHERE status = 1 AND expire_time < NOW()")
    java.util.List<Seat> selectExpiredLockedSeats();
}
