package com.movieticket.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;         // 订单编号（UUID 前20位）
    private Long userId;            // 用户ID
    private Long movieId;           // 电影ID
    private Long sessionId;         // 场次ID
    private BigDecimal totalAmount; // 订单总金额
    private Integer status;         // 0=PENDING，1=PAID，2=CANCELLED，3=REFUNDED

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 过期时间（创建时间 + 15分钟）
    private LocalDateTime expireTime;
}
