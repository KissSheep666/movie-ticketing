package com.movieticket.order.job;

import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.mapper.OrderItemMapper;
import com.movieticket.order.mapper.OrderMapper;
import com.movieticket.order.service.OrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * XXL-Job 定时任务：扫描过期订单 → 释放座位 → 取消订单。
 *
 * 为什么需要这个定时任务：
 *   用户锁座+下单后，如果15分钟内不支付，座位应该释放给其他人。
 *   不能依赖用户主动取消——有些人直接关页面走了。
 *
 * XXL-Job 调度：每分钟执行一次
 *   在 XXL-Job Admin 控制台配置：JobHandler = "orderTimeoutHandler"，Cron = "0 * * * * ?"
 *
 * 为什么用 XXL-Job 而不是 @Scheduled：
 *   @Scheduled 写死在代码里，改频率要重新部署。
 *   XXL-Job 在控制台动态改 Cron，且有任务日志、失败重试、分片等功能。
 *   多实例部署时，@Scheduled 会重复执行，XXL-Job 通过路由策略保证只执行一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;

    /**
     * 定时释放过期订单。
     * 逻辑：扫 orders WHERE status=0 AND expire_time < NOW() → 逐一取消。
     *
     * 为什么逐一取消而不是批量 UPDATE：
     *   取消订单需要 Feign 调 session-service 释放座位，涉及跨服务调用。
     *   批量 UPDATE 只能改本地库，座位的释放需要一条一条通知 session-service。
     */
    @XxlJob("orderTimeoutHandler")
    public void handle() {
        log.info("开始扫描过期订单...");

        List<Orders> expiredOrders = orderMapper.selectExpiredOrders();
        if (expiredOrders.isEmpty()) {
            log.info("没有过期订单");
            return;
        }

        int count = 0;
        for (Orders order : expiredOrders) {
            try {
                // 获取订单的座位明细 → 释放座位
                List<OrderItem> items = orderService.getOrderItems(order.getId());
                List<Long> seatIds = items.stream().map(OrderItem::getSeatId).toList();

                // cancel 内部会调 Feign 释放座位 + 改订单状态
                orderService.cancel(order.getId(), order.getUserId());
                log.info("过期订单已取消: orderNo={}, seatIds={}", order.getOrderNo(), seatIds);
                count++;
            } catch (Exception e) {
                log.error("取消过期订单失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        log.info("过期订单扫描完成: 共{}条, 成功取消{}条", expiredOrders.size(), count);
    }
}
