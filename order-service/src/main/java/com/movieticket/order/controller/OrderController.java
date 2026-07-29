package com.movieticket.order.controller;

import com.movieticket.common.result.Result;
import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器。
 *
 * 接口设计：
 *   POST   /api/order/create        下单（需登录）
 *   POST   /api/order/pay            支付（需登录）
 *   POST   /api/order/cancel         取消（需登录）
 *   GET    /api/order/{id}           订单详情（需登录）
 *   GET    /api/order/list           我的订单列表（需登录）
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 下单 */
    @PostMapping("/create")
    public Result<Orders> create(@RequestHeader("X-User-Id") Long userId,
                                  @RequestBody Map<String, Object> body) {
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        Long movieId = Long.valueOf(body.get("movieId").toString());
        @SuppressWarnings("unchecked")
        List<Integer> seatIdInts = (List<Integer>) body.get("seatIds");
        List<Long> seatIds = seatIdInts.stream().map(Long::valueOf).toList();

        Orders order = orderService.create(userId, sessionId, movieId, seatIds);
        return Result.success(order);
    }

    /** 支付 */
    @PostMapping("/pay")
    public Result<?> pay(@RequestHeader("X-User-Id") Long userId,
                          @RequestParam Long orderId) {
        orderService.pay(orderId, userId);
        return Result.success(null);
    }

    /** 取消 */
    @PostMapping("/cancel")
    public Result<?> cancel(@RequestHeader("X-User-Id") Long userId,
                             @RequestParam Long orderId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<Orders> getById(@PathVariable Long id) {
        Orders order = orderService.getById(id);
        return Result.success(order);
    }

    /** 我的订单列表 */
    @GetMapping("/list")
    public Result<List<Orders>> list(@RequestHeader("X-User-Id") Long userId) {
        List<Orders> orders = orderService.listByUserId(userId);
        return Result.success(orders);
    }

    /** 订单明细（座位列表） */
    @GetMapping("/{id}/items")
    public Result<List<OrderItem>> items(@PathVariable Long id) {
        List<OrderItem> items = orderService.getOrderItems(id);
        return Result.success(items);
    }
}
