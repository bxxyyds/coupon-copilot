package com.couponcopilot.common.result;

import com.couponcopilot.common.errorcode.IErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.slf4j.MDC;

/**
 * 全局统一 REST API 返回体。
 */
@Data
public class Result<T> {

    public static final String SUCCESS_CODE = "0";
    public static final String SUCCESS_MESSAGE = "ok";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private String code;
    private String message;
    private T data;
    private String traceId;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage(SUCCESS_MESSAGE);
        result.setData(data);
        result.setTraceId(MDC.get(TRACE_ID_MDC_KEY));
        return result;
    }

    public static <T> Result<T> failure(IErrorCode errorCode) {
        return failure(errorCode.code(), errorCode.message());
    }

    public static <T> Result<T> failure(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTraceId(MDC.get(TRACE_ID_MDC_KEY));
        return result;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
