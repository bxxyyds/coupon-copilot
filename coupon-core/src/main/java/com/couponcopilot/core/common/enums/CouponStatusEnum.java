package com.couponcopilot.core.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券模板状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CouponStatusEnum {

    ACTIVE(0, "生效中"),
    ENDED(1, "已结束");

    private final Integer code;
    private final String desc;

    public static CouponStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
