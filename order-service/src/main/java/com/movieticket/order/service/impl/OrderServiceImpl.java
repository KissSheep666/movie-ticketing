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

/**
 * 订单业务实现。
 *
 * 下单流程（跨服务调用链）：
 *   1. Feign 调 user-service   → 验证用户是否存在
 *   2. Feign 调 session-service → 查场次信息（获取票价等）
 *   3. 生成订单号 + 订单记录 → 本地入库
 *   4. 生成订单明细（每个座位一条）→ 本地入库
 *   5. session-service 的锁座已在前一步由用户直接调用完成
 *
 * 为什么下单和锁座分两步：
 *   用户先选座 → 锁座（15分钟倒计时开始）→ 填写确认信息 → 下单
 *   如果下单和锁座合并：用户还在犹豫时座位就被锁了，其他用户无法选
 *   分开：锁座给用户缓冲时间，下单确认后才真正占用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserFeignClient userFeignClient;
    private final SessionFeignClient sessionFeignClient;

    /**
     * 创建订单。
     * 支付窗口：创建时间 + 15分钟，超时未支付 → XXL-Job 自动取消。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders create(Long userId, Long sessionId, Long movieId, List<Long> seatIds) {
        // 1. Feign 验证用户存在（200=成功，其他=异常）
        Result<Map<String, Object>> userResult = userFeignClient.getUserById(userId);
        if (userResult.getCode() != 200) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }

        // 2. Feign 查场次，获取票价
        Result<Map<String, Object>> sessionResult = sessionFeignClient.getSessionById(sessionId);
        if (sessionResult.getCode() != 200 || sessionResult.getData() == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_FOUND);
        }
        BigDecimal price = new BigDecimal(
                String.valueOf(sessionResult.getData().get("price")));

        // 3. 创建订单
        Orders order = new Orders();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setMovieId(movieId);
        order.setSessionId(sessionId);
        order.setTotalAmount(price.multiply(new BigDecimal(seatIds.size())));  // 总价 = 单价 × 座位数
        order.setStatus(0);  // PENDING
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));  // 15分钟支付窗口
        orderMapper.insert(order);

        // 4. 创建订单明细（每个锁定座位一条）
        for (Long seatId : seatIds) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSeatId(seatId);
            item.setSeatCode("");   // seatCode 由前端展示，这里留空或从 session 查
            item.setPrice(price);
            orderItemMapper.insert(item);
        }

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}, seatCount={}",
                order.getOrderNo(), userId, order.getTotalAmount(), seatIds.size());
        return order;
    }

    /**
     * 模拟支付：PENDING → PAID，同时通知 session-service 确认座位售出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId, Long userId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CANNOT_CANCEL);
        }

        // 改订单状态
        orderMapper.payOrder(orderId);

        // 查明细 → 通知 session-service 确认售出
        // （这里简化：session-service 需要 confirmSeats 接口，留给 Phase 7 完善）
        log.info("支付成功: orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, order.getTotalAmount());
    }

    /**
     * 取消订单：CANCELLED，Feign 调 session-service 释放座位。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CANNOT_CANCEL);
        }

        // 取消订单
        orderMapper.cancelOrder(orderId);

        // 获取订单明细中的 seatIds → 释放座位
        List<OrderItem> items = getOrderItems(orderId);
        List<Long> seatIds = items.stream().map(OrderItem::getSeatId).toList();
        sessionFeignClient.releaseSeats(seatIds, order.getSessionId());

        log.info("订单取消成功: orderNo={}, userId={}", order.getOrderNo(), userId);
    }

    @Override
    public Orders getById(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 按用户ID查订单列表，按创建时间降序。
     */
    @Override
    public List<Orders> listByUserId(Long userId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    /**
     * 查某个订单的座位明细。
     */
    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }
}
