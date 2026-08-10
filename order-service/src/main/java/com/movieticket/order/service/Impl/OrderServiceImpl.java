package com.movieticket.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.common.result.Result;
import com.movieticket.order.client.SessionFeignClient;
import com.movieticket.order.client.UserFeignClient;
import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.mapper.OrderItemMapper;
import com.movieticket.order.mapper.OrderMapper;
import com.movieticket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;



    private final UserFeignClient userFeignClient;
    private final SessionFeignClient sessionFeignClient;


    /**
     *
     * @param userId
     * @param sessionId
     * @param movieId
     * @param seatIds
     * @return
     *
     * 业务流程 获取当前用户 并且进行校验 -> 获取场次详细信息 -> 本地入库
     */
    @Override
    public Orders createOrder(Long userId, Long sessionId, Long movieId, List<Long> seatIds) {
        //利用feign来获取当前用户数据,并且验证是否合法
        Result<Map<String, Object>> curUser = userFeignClient.getUserById(userId);
        if (curUser.getCode() != 200){
            //如果获取失败
            throw new BizException(curUser.getMsg());
        }

        //获取场次详情
        Result<Map<String, Object>> session = sessionFeignClient.getSessionById(sessionId);
        if (session.getCode() != 200){
            //如果获取失败
            throw new BizException(session.getMsg());
        }
        BigDecimal price = (BigDecimal) session.getData().get("price");

        //获取相应的价格 填入 创建新的订单
        Orders order = new Orders();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setMovieId(movieId);
        order.setSessionId(sessionId);
        order.setTotalAmount(price.multiply(new BigDecimal(seatIds.size())));
        order.setStatus(0);  // PENDING
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));
        orderMapper.insert(order);

        //将相应的OrderItem 进行插入
        for (Long seatId : seatIds){
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSeatId(seatId);
            item.setSeatCode("");
            item.setPrice(price);
            orderItemMapper.insert(item);
        }

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}, seatCount={}",
                order.getOrderNo(), userId, order.getTotalAmount(), seatIds.size());

        return order;
    }

    /**
     * 订单支付
     * 逻辑 -> 查询订单 -> 如果状态正常 修改订单支付状态
     * @param orderId
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void pay(Long orderId, Long userId) {
        LambdaQueryWrapper<Orders> query = new LambdaQueryWrapper<>();
        query.eq(Orders::getId, orderId);
        query.eq(Orders::getUserId, userId);
        Orders order = orderMapper.selectOne(query);
        if (order == null){
            throw new BizException(BizCodeEnum.MOVIE_NOT_FOUND);
        }
        if (order.getStatus() != 0){
            throw new BizException(BizCodeEnum.PARAM_ERROR);
        }

        //修改相应状态之类的额
        orderMapper.payOrder(orderId);
        log.info("订单支付成功: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 取消订单 -> 修改订单状态 -> 释放座位
     * @param orderId
     * @param userId
     */
    @Override
    public void cancel(Long orderId, Long userId) {
        //查询订单
        LambdaQueryWrapper<Orders> query = new LambdaQueryWrapper<>();
        query.eq(Orders::getId, orderId);
        query.eq(Orders::getUserId, userId);
        Orders order = orderMapper.selectOne(query);
        if (order == null){
            throw new BizException(BizCodeEnum.PARAM_ERROR);
        }
        if (order.getStatus() != 0){
            throw new BizException(BizCodeEnum.PARAM_ERROR);
        }

        //释放座位,座位具体信息 从 明细表中获取 根据 orderId 获取
        LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> orderItems = orderItemMapper.selectList(itemQuery);
        List<Long> seatIds = new ArrayList<>();
        for (OrderItem item : orderItems){
            seatIds.add(item.getSeatId());
        }
        Long sessionId = order.getSessionId();
        sessionFeignClient.releaseSeats(seatIds, sessionId);
        orderMapper.cancelOrder(orderId);
        log.info("订单取消成功: orderId={}, userId={}", orderId, userId);
    }


    /**
     * 根据id获取订单
     * @param id
     * @return
     */
    @Override
    public Orders getById(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public List<Orders> listByUserId(Long userId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }
}
