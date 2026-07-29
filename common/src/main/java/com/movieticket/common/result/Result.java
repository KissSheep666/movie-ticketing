package com.movieticket.common.result;

import lombok.Data;

@Data
public class Result<T>{
    //错误码
    private int code;
    //错误信息
    private String msg;
    //数据
    private T data;

    public Result(int code, String msg, T data){
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Result(){

    }

    /**
     * 调用成功
     */
    public static <T> Result<T> success(T data){
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }


    /**
     * 调用失败 此时需要传入错误码和错误信息
     */
    public static <T> Result<T> error(int code, String msg){
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
