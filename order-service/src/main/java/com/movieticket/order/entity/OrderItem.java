package com.movieticket.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单明细实体，映射 db_order 库的 order_item 表。
 * 一个订单可以买多个座位，每个座位一条明细。
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;       // 所属订单ID
    private Long seatId;        // 座位ID
    private String seatCode;    // 座位编号（冗余存储，方便展示，不用回查 session-service）
    private BigDecimal price;   // 该座位价格
}
