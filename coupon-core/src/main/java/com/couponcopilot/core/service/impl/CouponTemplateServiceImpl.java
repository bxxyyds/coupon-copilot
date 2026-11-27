package com.couponcopilot.core.service.impl;

import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.common.enums.CouponSourceEnum;
import com.couponcopilot.core.common.enums.CouponStatusEnum;
import com.couponcopilot.core.common.enums.CouponTargetEnum;
import com.couponcopilot.core.dao.entity.CouponTemplateDO;
import com.couponcopilot.core.dao.mapper.CouponTemplateMapper;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.service.CouponTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 优惠券模板业务服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final ObjectMapper objectMapper;

    @Override
    public CouponTemplateCreateResp createTemplate(CouponTemplateCreateReq req) {
        UserContext.requireRole(UserRole.MERCHANT);
        Long shopId = resolveShopId(req.getSource());
        String normalizedGoodsCode = validateAndNormalize(req);

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

    private String validateAndNormalize(CouponTemplateCreateReq req) {
        String normalizedGoodsCode = null;
        if (CouponTargetEnum.SPECIFIC_GOODS.getCode().equals(req.getTarget())) {
            if (!StringUtils.hasText(req.getGoodsCode())) {
                throw new ClientException("指定商品券必须填写商品编码");
            }
            normalizedGoodsCode = req.getGoodsCode().trim();
        }

        if (!req.getValidEndTime().isAfter(req.getValidStartTime())) {
            throw new ClientException("有效期结束时间必须晚于开始时间", BaseErrorCode.INVALID_TIME_RANGE);
        }
        if (!req.getValidEndTime().isAfter(LocalDateTime.now())) {
            throw new ClientException("有效期结束时间必须晚于当前时间", BaseErrorCode.INVALID_TIME_RANGE);
        }

        validateJsonObject("receiveRule", req.getReceiveRule());
        validateJsonObject("consumeRule", req.getConsumeRule());
        return normalizedGoodsCode;
    }

    private void validateJsonObject(String fieldName, String rawJson) {
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            if (node == null || !node.isObject()) {
                throw new ClientException(fieldName + " 必须为合法的 JSON 对象");
            }
        } catch (JsonProcessingException ex) {
            throw new ClientException(fieldName + " 必须为合法的 JSON 对象");
        }
    }
}
