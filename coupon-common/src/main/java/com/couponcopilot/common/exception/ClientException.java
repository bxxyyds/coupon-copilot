package com.couponcopilot.common.exception;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.errorcode.IErrorCode;

/**
 * 客户端参数或合法性校验异常（对应 Axxxx 错误码）。
 */
public class ClientException extends AbstractException {

    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode != null ? errorCode : BaseErrorCode.CLIENT_ERROR);
    }
}
