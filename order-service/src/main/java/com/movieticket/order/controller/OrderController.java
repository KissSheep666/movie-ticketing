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
 * 订单控制器
 *
 *  接口设计：
 *   POST   /api/order/create        下单（需登录）
 *   POST   /api/order/pay           支付（需登录）
 *   POST   /api/order/cancel        取消（需登录）
 *   GET    /api/order/{id}          订单详情（需登录）
 *   GET    /api/order/list          我的订单列表（需登录）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

        @PostMapping("/create")
        public Result<Orders> create(
                //从Gateway 获得的当前用户的id
                @RequestHeader("X-User-Id") Long userId,
                //请求体 里面储存的数据是
                @RequestBody Map<String, Object> body
    ){
        //从Map中 获取订单需要的各个数据 实际开发中可以使用DTO 进行更加规范的开发
            Long sessionId = (Long) body.get("sessionId");
            Long movieId = (Long) body.get("movieId");
            List<Integer> seatIdInts = (List<Integer>) body.get("seatIds");
            //将座位号进行转化
            List<Long> seatIds = seatIdInts.stream().map(Integer::longValue).toList();
            Orders order = orderService.createOrder(userId, sessionId, movieId, seatIds);
            return Result.success(order);
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

