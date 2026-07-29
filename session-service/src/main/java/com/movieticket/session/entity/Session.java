package com.movieticket.session.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场次实体，映射 db_session 库的 session 表。
 * 一场电影 = 一个 session，包含放映时间、影厅、票价、座位统计。
 */
@Data
@TableName("session")
public class Session {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;           // 电影ID（来自 movie-service）
    private String cinemaHall;      // 放映厅（IMAX厅 / 3号厅...）
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime;   // 结束时间
    private BigDecimal price;       // 票价
    private Integer totalSeats;     // 总座位数
    private Integer availableSeats; // 剩余可用座位数（锁座 -1，释放 +1）
    private Integer status;         // 1=即将开始，2=售票中，3=已结束，4=已取消

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
