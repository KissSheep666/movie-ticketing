package com.movieticket.movie.client;

import com.movieticket.common.result.Result;
import com.mysql.cj.protocol.a.BinaryRowFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MovieFeignClientFallback implements MovieFeignClient{
    @Override
    public Result<Map<String, Object>> getMovieById(Long id) {
        log.info("MovieFeignClient 降级: getMovieById({})", id);
        return Result.error(500, "MovieFeignClient 降级: getMovieById({})");
    }
}
