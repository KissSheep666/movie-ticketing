package com.movieticket.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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


    //创建时间,在见表世界直接自动进行插入

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
