package com.movieticket.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItem {

    // 一个订单 可以买 多个座位 因此使用订单明细表 来承载数据
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;       // 所属订单ID
    private Long seatId;        // 座位ID
    private String seatCode;    // 座位编号（冗余存储，方便展示）
    private BigDecimal price;   // 该座位价格
}
