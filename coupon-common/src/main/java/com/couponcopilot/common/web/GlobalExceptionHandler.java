package com.couponcopilot.common.web;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.AbstractException;
import com.couponcopilot.common.exception.RemoteException;
import com.couponcopilot.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器，将业务异常、参数校验异常与未知异常统一转换为 Result 返回。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AbstractException.class)
    public Result<Void> handleAbstractException(AbstractException ex, HttpServletRequest request) {
        if (ex instanceof RemoteException) {
            log.error("Remote error on [{} {}], code={}, msg={}",
                    request.getMethod(), request.getRequestURI(), ex.getErrorCode(), ex.getErrorMessage(), ex);
        } else {
            log.warn("Domain exception on [{} {}], code={}, msg={}",
                    request.getMethod(), request.getRequestURI(), ex.getErrorCode(), ex.getErrorMessage());
        }
        return Result.failure(ex.getErrorCode(), ex.getErrorMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String msg = fieldErrors.stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (!StringUtils.hasText(msg)) {
            msg = BaseErrorCode.CLIENT_ERROR.message();
        }
        log.warn("Validation failed on [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return Result.failure(BaseErrorCode.CLIENT_ERROR.code(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        if (!StringUtils.hasText(msg)) {
            msg = BaseErrorCode.CLIENT_ERROR.message();
        }
        log.warn("Constraint violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return Result.failure(BaseErrorCode.CLIENT_ERROR.code(), msg);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public Result<Void> handleRequestFormatException(Exception ex, HttpServletRequest request) {
        String msg;
        if (ex instanceof MissingServletRequestParameterException missingEx) {
            msg = "缺少必填参数: " + missingEx.getParameterName();
        } else if (ex instanceof MethodArgumentTypeMismatchException mismatchEx) {
            msg = "参数类型错误: " + mismatchEx.getName();
        } else {
            msg = BaseErrorCode.REQUEST_BODY_ERROR.message();
        }
        log.warn("Bad request format on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return Result.failure(BaseErrorCode.REQUEST_BODY_ERROR.code(), msg);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return Result.failure(BaseErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Throwable.class)
    public Result<Void> handleThrowable(Throwable ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        // 绝不向客户端暴露底层异常栈或敏感信息
        return Result.failure(BaseErrorCode.SERVICE_ERROR);
    }
}
