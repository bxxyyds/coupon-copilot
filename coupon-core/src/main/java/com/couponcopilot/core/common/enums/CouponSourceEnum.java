package com.couponcopilot.core.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券来源枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CouponSourceEnum {

    SHOP(0, "店铺券"),
    PLATFORM(1, "平台券");

    private final Integer code;
    private final String desc;

    public static CouponSourceEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponSourceEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
