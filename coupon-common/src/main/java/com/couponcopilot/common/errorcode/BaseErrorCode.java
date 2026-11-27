package com.couponcopilot.common.errorcode;

/**
 * 基础三级错误码枚举：A 客户端参数错误 / B 业务与系统错误 / C 下游远程服务错误。
 */
public enum BaseErrorCode implements IErrorCode {

    // ========== A: 客户端参数与校验错误 ==========
    CLIENT_ERROR("A000001", "请求参数校验失败"),
    REQUEST_BODY_ERROR("A000100", "请求体格式错误"),
    INVALID_TIME_RANGE("A000101", "时间参数非法"),
    NOT_FOUND("A000404", "请求资源不存在"),

    // ========== B: 业务状态与系统错误 ==========
    SERVICE_ERROR("B000001", "系统繁忙，请稍后再试"),
    COUPON_STOCK_NOT_ENOUGH("B000201", "优惠券库存不足"),
    COUPON_RECEIVE_LIMIT_EXCEEDED("B000202", "已达到每人限领上限"),
    COUPON_ALREADY_LOCKED("B000301", "优惠券已被锁定"),

    // ========== C: 下游与模型服务异常 ==========
    REMOTE_ERROR("C000001", "下游服务调用异常");

    private final String code;
    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
