package com.movieticket.movie.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 座位实体 映射seat表
 *
 * 乐观锁 version 字段说明
 * 每次锁座 售出时检查 version  如果 version 已经呗其他十五修改,a
 */
@Data
@TableName("seat")
public class Seat {
    //version 使用版本号 防止超卖 多条sql语句 只能提交一条 别的都会被sql影响
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;         // 所属场次
    private Integer seatRow;        // 排号（1-based）
    private Integer seatCol;        // 列号（1-based）
    private String seatCode;        // 座位编号（如 A-03，前端展示用）
    private Integer status;         // 0=AVAILABLE，1=LOCKED，2=SOLD

    //乐观锁注解
    @Version
    private Integer version;

    //设定 过期时间
    private LocalDateTime expireTime;


}
