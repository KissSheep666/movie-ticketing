package com.movieticket.order.job;

import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.mapper.OrderMapper;
import com.movieticket.order.service.impl.OrderServiceImpl;
import com.movieticket.order.service.OrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单超时处理
 *
 * 每分钟扫描过期订单 扫描过期订单 -> 释放座位 -> 取消订单
 *
 * XXl-Job 调度 : 每分钟执行一次
 *
 * 调度策略  可以在黑鸡在控制台 进行配置 设置 时间名 执行的间隔事件
 * @Scheduled 不适合 大型的项目 同步性 分区性 不够复杂 同时 代码的 耦合性太高了
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    /**
     * 定期 释放过期订单 取消订单
     * 为什么不批量Update 涉及到座位的释放 需要 利用Figen 调用 另一个服务模块的内容
     */
    //指定 任务名称不同的任务名 代表着 不同的 执行策略
    @XxlJob("orderTimeoutJob")
    public void handle() {
        log.info("订单超时处理任务开始执行");

        List<Orders> expiredOrders = orderMapper.findExpiredOrders();
        if (expiredOrders.isEmpty()) {
            //此时没有订单需要处理
            log.info("没有订单需要处理");
            return;
        }

        //批量处理
        try {
            for (Orders order : expiredOrders) {
                //订单取消 需要 释放座位 修改订单状态
                orderService.cancel(order.getId(), order.getUserId());
                log.info("订单{}处理完成", order.getId());
                //释放座位,获取订单明细
                List<OrderItem> orderItems = orderService.getOrderItems(order.getId());
                //释放座位,修改座位状态
                List<Long> seatIds = new ArrayList<>();

                for (OrderItem item : orderItems) {
                    seatIds.add(item.getSeatId());
                }

                //涉及 相关的 操作 都在 service 层面 进行 调用
                orderService.cancel(order.getId(), order.getUserId());
            }
        } catch (Exception e) {
            log.error("订单超时处理任务执行异常", e);
            throw new RuntimeException(e);
        }
        log.info("订单超时处理任务执行完毕");
    }
}
