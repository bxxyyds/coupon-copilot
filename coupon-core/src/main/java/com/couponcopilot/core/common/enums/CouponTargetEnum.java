package com.couponcopilot.core.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券适用对象枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CouponTargetEnum {

    SPECIFIC_GOODS(0, "指定商品"),
    ALL_SHOP(1, "全店通用");

    private final Integer code;
    private final String desc;

    public static CouponTargetEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponTargetEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
