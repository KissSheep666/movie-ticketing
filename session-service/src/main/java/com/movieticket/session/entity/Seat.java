package com.movieticket.session.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 座位实体，映射 db_session 库的 seat 表。
 *
 * 乐观锁 version 字段说明：
 *   每次锁座/售出时检查 version：UPDATE seat SET status=1, version=version+1 WHERE id=? AND version=?
 *   如果 version 已被其他事务修改（并发冲突），affected rows = 0，上层重试。
 *   比悲观锁（SELECT ... FOR UPDATE）轻量，不阻塞读操作。
 *
 * 座位状态：
 *   0 = AVAILABLE（可锁）
 *   1 = LOCKED（已锁定，15分钟未支付则释放）
 *   2 = SOLD（已售出，不可逆）
 */
@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;         // 所属场次
    private Integer seatRow;        // 排号（1-based）
    private Integer seatCol;        // 列号（1-based）
    private String seatCode;        // 座位编号（如 A-03，前端展示用）
    private Integer status;         // 0=AVAILABLE，1=LOCKED，2=SOLD

    @Version                        // MyBatis-Plus 乐观锁注解：update 时自动 WHERE version=? AND SET version=version+1
    private Integer version;

    private LocalDateTime expireTime; // 锁定过期时间（锁座时 = now + 15min，超时后 XXL-Job 释放）
}
