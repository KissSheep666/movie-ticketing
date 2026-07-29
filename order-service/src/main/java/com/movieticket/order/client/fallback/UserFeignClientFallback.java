package com.movieticket.order.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * UserFeignClient 的降级实现。
 *
 * 为什么需要降级：
 *   user-service 挂了或超时 → Feign 抛异常 → 如果不处理，下单链路直接报 500
 *   有了降级 → 返回一个友好错误，前端能正常展示提示，不会白屏
 *
 * 触发条件（可在 Nacos 控制台动态配置）：
 *   - 慢调用比例：超过阈值自动熔断
 *   - 异常比例：N次调用中异常超过比例 → 熔断
 *   - 异常数：N次调用中M次异常 → 熔断
 */
@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public Result<Map<String, Object>> getUserById(Long id) {
        log.warn("UserFeignClient 降级: getUserById({})", id);
        return Result.error(500, "用户服务暂不可用，请稍后重试");
    }
}
