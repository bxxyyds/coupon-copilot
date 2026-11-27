package com.couponcopilot.common.web;

import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserInfo;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.exception.ClientException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从 HTTP Header 解析轻量身份并写入 UserContext，在请求彻底完成后清理 ThreadLocal。
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_ROLE = "X-Role";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_SHOP_ID = "X-Shop-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String roleHeader = request.getHeader(HEADER_ROLE);
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String shopIdHeader = request.getHeader(HEADER_SHOP_ID);

        if (!StringUtils.hasText(roleHeader)
                && !StringUtils.hasText(userIdHeader)
                && !StringUtils.hasText(shopIdHeader)) {
            return true;
        }

        // 先全部解析校验，全部合法后再写入 ThreadLocal，避免异常时残留半截状态
        UserRole role = UserRole.fromHeader(roleHeader);
        Long userId = parseLongHeader(HEADER_USER_ID, userIdHeader);
        Long shopId = parseLongHeader(HEADER_SHOP_ID, shopIdHeader);

        UserContext.set(UserInfo.builder()
                .role(role)
                .userId(userId)
                .shopId(shopId)
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private static Long parseLongHeader(String headerName, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new ClientException("请求头 " + headerName + " 必须为数字");
        }
    }
}
