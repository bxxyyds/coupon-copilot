package com.couponcopilot.core.service.chain.template;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.service.chain.AbstractChainHandler;
import com.couponcopilot.core.service.chain.ChainBizMark;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 建券校验第 2 环：有效期时间合法性校验（order=20）。
 */
@Component
public class TemplateTimeChainHandler implements AbstractChainHandler<CouponTemplateCreateReq> {

    @Override
    public void handle(CouponTemplateCreateReq req) {
        if (req.getValidStartTime() == null) {
            throw new ClientException("生效开始时间 validStartTime 不能为空", BaseErrorCode.INVALID_TIME_RANGE);
        }
        if (req.getValidEndTime() == null) {
            throw new ClientException("生效结束时间 validEndTime 不能为空", BaseErrorCode.INVALID_TIME_RANGE);
        }
        if (!req.getValidEndTime().isAfter(req.getValidStartTime())) {
            throw new ClientException("有效期结束时间必须晚于开始时间", BaseErrorCode.INVALID_TIME_RANGE);
        }
        if (!req.getValidEndTime().isAfter(LocalDateTime.now())) {
            throw new ClientException("有效期结束时间必须晚于当前时间", BaseErrorCode.INVALID_TIME_RANGE);
        }
    }

    @Override
    public String mark() {
        return ChainBizMark.COUPON_TEMPLATE_CREATE;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
