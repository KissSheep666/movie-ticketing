package com.movieticket.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，映射 db_order 库的 orders 表。
 *
 * 订单状态流转：
 *   PENDING(0) ──支付──→ PAID(1)
 *       │                  │
 *       └──超时/取消──→ CANCELLED(2)
 *                          │
 *                   PAID(1) ──退款──→ REFUNDED(3)
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;         // 订单编号（雪花ID或时间戳生成，唯一）
    private Long userId;            // 用户ID
    private Long movieId;           // 电影ID
    private Long sessionId;         // 场次ID
    private BigDecimal totalAmount; // 订单总金额
    private Integer status;         // 0=PENDING，1=PAID，2=CANCELLED，3=REFUNDED

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime expireTime; // 过期时间（创建时间 + 15分钟，超时未支付自动取消）
}
