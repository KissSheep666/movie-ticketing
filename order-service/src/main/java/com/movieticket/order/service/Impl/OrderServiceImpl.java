package com.movieticket.order.service.Impl;

import com.movieticket.common.exception.BizException;
import com.movieticket.common.result.Result;
import com.movieticket.order.client.SessionFeignClient;
import com.movieticket.order.client.UserFeignClient;
import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.mapper.OrderMapper;
import com.movieticket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;



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


        return null;
    }

    @Override
    public void pay(Long orderId, Long userId) {

    }

    @Override
    public void cancel(Long orderId, Long userId) {

    }

    @Override
    public Orders getById(Long id) {
        return null;
    }

    @Override
    public List<Orders> listByUserId(Long userId) {
        return List.of();
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        return List.of();
    }
}
