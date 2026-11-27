package com.couponcopilot.core.service.impl;

import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.common.enums.CouponSourceEnum;
import com.couponcopilot.core.common.enums.CouponStatusEnum;
import com.couponcopilot.core.common.enums.CouponTargetEnum;
import com.couponcopilot.core.dao.entity.CouponTemplateDO;
import com.couponcopilot.core.dao.mapper.CouponTemplateMapper;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.service.CouponTemplateService;
import com.couponcopilot.core.service.chain.ChainBizMark;
import com.couponcopilot.core.service.chain.ChainContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 优惠券模板业务服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final ChainContext chainContext;

    @Override
    public CouponTemplateCreateResp createTemplate(CouponTemplateCreateReq req) {
        UserContext.requireRole(UserRole.MERCHANT);
        Long shopId = resolveShopId(req.getSource());
        chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, req);

        String normalizedGoodsCode = CouponTargetEnum.SPECIFIC_GOODS.getCode().equals(req.getTarget())
                ? req.getGoodsCode().trim()
                : null;

        CouponTemplateDO entity = CouponTemplateDO.builder()
                .shopId(shopId)
                .name(req.getName().trim())
                .source(req.getSource())
                .target(req.getTarget())
                .goodsCode(normalizedGoodsCode)
                .type(req.getType())
                .validStartTime(req.getValidStartTime())
                .validEndTime(req.getValidEndTime())
                .stock(req.getStock())
                .receiveRule(req.getReceiveRule().trim())
                .consumeRule(req.getConsumeRule().trim())
                .status(CouponStatusEnum.ACTIVE.getCode())
                .build();

        couponTemplateMapper.insert(entity);
        log.info("Created coupon template id={}, shopId={}, name={}, stock={}",
                entity.getId(), entity.getShopId(), entity.getName(), entity.getStock());

        return CouponTemplateCreateResp.builder()
                .id(entity.getId())
                .shopId(entity.getShopId())
                .name(entity.getName())
                .status(entity.getStatus())
                .stock(entity.getStock())
                .build();
    }

    private Long resolveShopId(Integer source) {
        if (CouponSourceEnum.PLATFORM.getCode().equals(source)) {
            return 0L;
        }
        Long shopId = UserContext.requireShopId();
        if (shopId <= 0) {
            throw new ClientException("店铺券请求头 X-Shop-Id 必须大于0");
        }
        return shopId;
    }
}
