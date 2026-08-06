package com.movieticket.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.movie.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {
    int lockSeat(@Param("seatId") Long seatId,
                 @Param("version") Integer version,
                 @Param("expireTime") LocalDateTime expireTime);
}
