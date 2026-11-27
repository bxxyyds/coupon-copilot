package com.couponcopilot.core.service.chain.template;

import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.common.enums.CouponSourceEnum;
import com.couponcopilot.core.common.enums.CouponTargetEnum;
import com.couponcopilot.core.common.enums.CouponTypeEnum;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.service.chain.AbstractChainHandler;
import com.couponcopilot.core.service.chain.ChainBizMark;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 建券校验第 1 环：必填项、枚举值与基础范围校验（order=10）。
 */
@Component
public class TemplateParamNotNullChainHandler implements AbstractChainHandler<CouponTemplateCreateReq> {

    @Override
    public void handle(CouponTemplateCreateReq req) {
        if (req == null) {
            throw new ClientException("建券请求参数不能为空");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new ClientException("优惠券名称不能为空");
        }
        if (req.getName().trim().length() > 128) {
            throw new ClientException("优惠券名称长度不能超过128");
        }
        if (CouponSourceEnum.of(req.getSource()) == null) {
            throw new ClientException("优惠券来源非法，仅支持 0(店铺券) 或 1(平台券)");
        }
        if (CouponTargetEnum.of(req.getTarget()) == null) {
            throw new ClientException("适用对象非法，仅支持 0(指定商品) 或 1(全店通用)");
        }
        if (CouponTargetEnum.SPECIFIC_GOODS.getCode().equals(req.getTarget())) {
            if (!StringUtils.hasText(req.getGoodsCode())) {
                throw new ClientException("指定商品券必须填写商品编码 goodsCode");
            }
            if (req.getGoodsCode().trim().length() > 64) {
                throw new ClientException("商品编码长度不能超过64");
            }
        }
        if (CouponTypeEnum.of(req.getType()) == null) {
            throw new ClientException("优惠券类型非法，仅支持 0(立减)、1(满减) 或 2(折扣)");
        }
        if (req.getStock() == null || req.getStock() <= 0) {
            throw new ClientException("发行库存 stock 必须大于 0");
        }
        if (!StringUtils.hasText(req.getReceiveRule())) {
            throw new ClientException("领取规则 receiveRule 不能为空");
        }
        if (!StringUtils.hasText(req.getConsumeRule())) {
            throw new ClientException("使用规则 consumeRule 不能为空");
        }
    }

    @Override
    public String mark() {
        return ChainBizMark.COUPON_TEMPLATE_CREATE;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
