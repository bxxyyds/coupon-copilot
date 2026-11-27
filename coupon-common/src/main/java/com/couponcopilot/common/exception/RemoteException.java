package com.couponcopilot.common.exception;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.errorcode.IErrorCode;

/**
 * 下游微服务、中间件或大模型调用异常（对应 Cxxxx 错误码）。
 */
public class RemoteException extends AbstractException {

    public RemoteException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode != null ? errorCode : BaseErrorCode.REMOTE_ERROR);
    }
}
