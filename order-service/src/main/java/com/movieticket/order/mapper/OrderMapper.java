package com.movieticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.order.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.core.annotation.Order;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
    /**
     * 查询已经过期的待支付订单 XXL-Job 定时查询
     */
//    @Select("SELECT * FROM db_order WHERE status = 0 AND create_time < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    List<Orders> findExpiredOrders();

    /**
     * 取消订单 未支付 -> 取消
     */
//    @Update("UPDATE db_order SET status = 2 WHERE id = #{orderId} AND status = 0")
    int cancelOrder(@Param("orderId") Long orderId);

    /**
     * 支付订单 未支付 ->  支付
     */
//    @Update("UPDATE db_order SET status = 1 WHERE id = #{orderId} AND status = 0")
    int payOrder(@Param("orderId") Long orderId);

}
