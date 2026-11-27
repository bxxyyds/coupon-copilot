package com.couponcopilot.core.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CouponTypeEnum {

    INSTANT_DISCOUNT(0, "立减"),
    THRESHOLD_DISCOUNT(1, "满减"),
    PERCENTAGE_DISCOUNT(2, "折扣");

    private final Integer code;
    private final String desc;

    public static CouponTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
