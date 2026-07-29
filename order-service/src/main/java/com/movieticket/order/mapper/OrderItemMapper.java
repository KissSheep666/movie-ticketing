package com.movieticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    // 基础 CRUD 已由 BaseMapper 提供
}
