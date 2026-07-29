package com.movieticket.session.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.session.client.MovieFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MovieFeignClient 降级：电影服务不可用时的兜底响应。
 * 注意：这个降级类不能抛出 BizException（因为 BizException 里带了 Result.error 的 code 和 message），
 * 这里直接返回 Result.error，让调用方（SessionServiceImpl）根据 code 判断。
 */
@Slf4j
@Component
public class MovieFeignClientFallback implements MovieFeignClient {

    @Override
    public Result<Map<String, Object>> getMovieById(Long id) {
        log.warn("MovieFeignClient 降级: getMovieById({})", id);
        return Result.error(500, "电影服务暂不可用，请稍后重试");
    }
}
