package com.couponcopilot.common.context;

import com.couponcopilot.common.exception.ClientException;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 轻量身份角色枚举：B 端商家与 C 端买家。
 */
public enum UserRole {

    MERCHANT,
    CUSTOMER;

    public static UserRole fromHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ClientException("非法的 X-Role 请求头: " + value);
        }
    }
}
