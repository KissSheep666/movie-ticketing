package com.movieticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.order.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /** 查询已过期的待支付订单（XXL-Job 定时扫） */
    @Select("SELECT * FROM orders WHERE status = 0 AND expire_time < NOW()")
    List<Orders> selectExpiredOrders();

    /** 取消订单 */
    @Update("UPDATE orders SET status = 2 WHERE id = #{orderId} AND status = 0")
    int cancelOrder(@Param("orderId") Long orderId);

    /** 支付订单 */
    @Update("UPDATE orders SET status = 1 WHERE id = #{orderId} AND status = 0")
    int payOrder(@Param("orderId") Long orderId);
}
