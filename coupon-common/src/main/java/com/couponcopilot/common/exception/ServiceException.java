package com.couponcopilot.common.exception;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.errorcode.IErrorCode;

/**
 * 业务状态或内部服务异常（对应 Bxxxx 错误码）。
 */
public class ServiceException extends AbstractException {

    public ServiceException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode != null ? errorCode : BaseErrorCode.SERVICE_ERROR);
    }
}
