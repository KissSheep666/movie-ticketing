package com.movieticket.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException{
    //原始的Exception只能传递字符串信息，不能传递异常对象，所以需要自定义异常类
    private final int code;

    public BizException(int code, String msg){
        super(msg);
        this.code = code;
    }

    //只传递消息
    public BizException(String msg){
        super(msg);
        this.code = -1;

    }

    //从枚举类中获得数据 并且解析 以及向上传递
    public  BizException(BizCodeEnum bizCodeEnum){
        super(bizCodeEnum.getMsg());
        this.code = bizCodeEnum.getCode();
    }
}
