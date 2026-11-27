package com.couponcopilot.core.service;

import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;

/**
 * 优惠券模板业务服务接口。
 */
public interface CouponTemplateService {

    /**
     * B 端创建优惠券模板。
     */
    CouponTemplateCreateResp createTemplate(CouponTemplateCreateReq req);
}
