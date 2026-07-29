package com.movieticket.common.exception;

import com.movieticket.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 Handler 类 捕获异常 进行处理
 */
@Slf4j
@RestControllerAdvice//用来捕获Controller没有被TryCatch的异常,这里用到的是AOP的思想，统一处理异常
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e){
        //将异常 -> 可观察的log
        log.error("捕获到业务异常，异常码：{}，异常信息：{}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    //兜底 对所有异常进行处理
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e){
        //将异常 -> 可观察的log
        log.error("捕获到异常，异常信息：{}", e.getMessage());
        return Result.error(500, "系统错误" + e.getMessage());
    }
}
