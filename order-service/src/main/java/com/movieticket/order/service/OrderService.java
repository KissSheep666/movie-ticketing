package com.movieticket.order.service;

import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;

import java.util.List;

/**
 * 订单业务接口。
 */
public interface OrderService {

    /**
     * 创建订单：验用户、验场次、生成订单、保存明细。
     *
     * @param userId    下单用户
     * @param sessionId 场次ID
     * @param movieId   电影ID
     * @param seatIds   锁定的座位ID列表
     * @return 订单对象
     */
    Orders create(Long userId, Long sessionId, Long movieId, List<Long> seatIds);

    /**
     * 模拟支付：将订单状态改为 PAID，确认座位售出。
     */
    void pay(Long orderId, Long userId);

    /**
     * 取消订单：释放座位 + 取消订单。
     */
    void cancel(Long orderId, Long userId);

    /**
     * 订单详情（含明细）。
     */
    Orders getById(Long id);

    /**
     * 查某用户的所有订单。
     */
    List<Orders> listByUserId(Long userId);

    /**
     * 查某订单的座位明细。
     */
    List<OrderItem> getOrderItems(Long orderId);
}
