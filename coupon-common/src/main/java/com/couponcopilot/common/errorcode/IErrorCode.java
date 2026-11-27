package com.couponcopilot.common.errorcode;

/**
 * 统一错误码契约接口。
 */
public interface IErrorCode {

    /**
     * 错误码（如 A000101 / B000201 / C000001）。
     */
    String code();

    /**
     * 面向调用方的可读错误提示。
     */
    String message();
}
