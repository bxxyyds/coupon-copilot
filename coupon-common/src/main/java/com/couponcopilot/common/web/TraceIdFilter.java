package com.couponcopilot.common.web;

import com.couponcopilot.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路 TraceId 过滤器：最高优先级执行，负责生成/透传 TraceId 到日志 MDC 与响应头，并在结束时清理。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final int MAX_TRACE_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        MDC.put(Result.TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Result.TRACE_ID_MDC_KEY);
        }
    }

    private static String resolveTraceId(String incoming) {
        if (!StringUtils.hasText(incoming)) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        String trimmed = incoming.trim();
        return trimmed.length() > MAX_TRACE_ID_LENGTH
                ? trimmed.substring(0, MAX_TRACE_ID_LENGTH)
                : trimmed;
    }
}
