package com.couponcopilot.common.exception;

import com.couponcopilot.common.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 领域异常抽象基类，统一携带错误码与可读提示。
 */
@Getter
public abstract class AbstractException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    protected AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(resolveMessage(message, errorCode), throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = resolveMessage(message, errorCode);
    }

    private static String resolveMessage(String message, IErrorCode errorCode) {
        return StringUtils.hasText(message) ? message : errorCode.message();
    }
}
