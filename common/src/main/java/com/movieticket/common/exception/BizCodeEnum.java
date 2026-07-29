package com.movieticket.common.exception;


import lombok.Getter;

/**
 * 由于此枚举类不可被修改 所以仅仅提供Getter方法即可
 * 业务错误码枚举，按模块号段分段：
 *   1xxx — 用户模块（注册/登录/信息）
 *   2xxx — 电影模块（列表/详情/分类）
 *   3xxx — 场次模块（场次/座位）
 *   4xxx — 订单模块（下单/支付/取消）
 *   9xxx — 通用错误（参数/权限）
 */
@Getter
public enum BizCodeEnum {
    // ==================== 通用（9xxx）====================
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ==================== 用户模块（1xxx）====================
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),

    // ==================== 电影模块（2xxx）====================
    MOVIE_NOT_FOUND(2001, "电影不存在"),
    CATEGORY_NOT_FOUND(2002, "分类不存在"),

    // ==================== 场次模块（3xxx）====================
    SESSION_NOT_FOUND(3001, "场次不存在"),
    SEAT_LOCKED(3002, "座位已被锁定"),
    SEAT_SOLD(3003, "座位已售出"),

    // ==================== 订单模块（4xxx）====================
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_TIMEOUT(4002, "订单已超时"),
    ORDER_CANNOT_CANCEL(4003, "订单不可取消"),

    ;
    //指定异常
    private final int code;
    private final String msg;
    BizCodeEnum(int code, String msg){
        this.code = code;
        this.msg = msg;
    }
}
