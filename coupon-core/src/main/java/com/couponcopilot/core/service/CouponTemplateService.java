package com.couponcopilot.core.service;

import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.req.CouponTemplateIncreaseStockReq;
import com.couponcopilot.core.dto.req.CouponTemplatePageReq;
import com.couponcopilot.core.dto.req.CouponTemplateTerminateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.dto.resp.CouponTemplateResp;
import com.couponcopilot.core.dto.resp.PageResp;

/**
 * 优惠券模板业务服务接口。
 */
public interface CouponTemplateService {

    /**
     * B 端创建优惠券模板。
     */
    CouponTemplateCreateResp createTemplate(CouponTemplateCreateReq req);

    /**
     * B 端分页查询优惠券模板。
     */
    PageResp<CouponTemplateResp> pageTemplates(CouponTemplatePageReq req);

    /**
     * B 端原子增发优惠券模板库存。
     */
    CouponTemplateResp increaseStock(CouponTemplateIncreaseStockReq req);

    /**
     * B 端紧急下线优惠券模板。
     */
    void terminateTemplate(CouponTemplateTerminateReq req);
}
