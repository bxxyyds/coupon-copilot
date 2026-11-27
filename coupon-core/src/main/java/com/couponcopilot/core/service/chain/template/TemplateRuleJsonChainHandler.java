package com.couponcopilot.core.service.chain.template;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.common.enums.CouponTypeEnum;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.service.chain.AbstractChainHandler;
import com.couponcopilot.core.service.chain.ChainBizMark;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 建券校验第 3 环：领取规则与使用规则 JSON 语义深度校验（order=30）。
 * 给出明确且可定位字段的错误提示，供调用方及后续 AI 建券自我修正。
 */
@Component
@RequiredArgsConstructor
public class TemplateRuleJsonChainHandler implements AbstractChainHandler<CouponTemplateCreateReq> {

    private static final BigDecimal MIN_DISCOUNT_RATE = new BigDecimal("0.10");
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.99");

    private final ObjectMapper objectMapper;

    @Override
    public void handle(CouponTemplateCreateReq req) {
        JsonNode receiveNode = parseJsonObject("receiveRule", req.getReceiveRule());
        validateReceiveRule(receiveNode);

        JsonNode consumeNode = parseJsonObject("consumeRule", req.getConsumeRule());
        CouponTypeEnum couponType = CouponTypeEnum.of(req.getType());
        if (couponType == null) {
            throw ruleException("优惠券类型非法，无法校验 consumeRule");
        }
        validateConsumeRule(couponType, consumeNode);
    }

    @Override
    public String mark() {
        return ChainBizMark.COUPON_TEMPLATE_CREATE;
    }

    @Override
    public int getOrder() {
        return 30;
    }

    private JsonNode parseJsonObject(String fieldName, String rawJson) {
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            if (node == null || !node.isObject()) {
                throw ruleException(fieldName + " 必须为合法的 JSON 对象");
            }
            return node;
        } catch (JsonProcessingException ex) {
            throw ruleException(fieldName + " JSON 格式解析失败，请检查引号与括号是否合法");
        }
    }

    private void validateReceiveRule(JsonNode receiveNode) {
        JsonNode limitNode = receiveNode.get("limitPerPerson");
        if (limitNode == null || limitNode.isNull() || !limitNode.isIntegralNumber() || limitNode.longValue() < 1) {
            throw ruleException("领取规则 receiveRule.limitPerPerson 必须为大于 0 的整数");
        }
    }

    private void validateConsumeRule(CouponTypeEnum type, JsonNode consumeNode) {
        switch (type) {
            case INSTANT_DISCOUNT -> {
                readMoney(consumeNode, "maximumDiscountAmount", "立减金额", true, true);
                BigDecimal terms = readMoney(consumeNode, "termsOfUse", "使用门槛", false, false);
                if (terms != null && terms.compareTo(BigDecimal.ZERO) > 0) {
                    throw ruleException("立减券无需设置大于 0 的使用门槛 termsOfUse，满减场景请改选满减券（type=1）");
                }
            }
            case THRESHOLD_DISCOUNT -> {
                BigDecimal terms = readMoney(consumeNode, "termsOfUse", "满减门槛", true, true);
                BigDecimal discount = readMoney(consumeNode, "maximumDiscountAmount", "满减优惠金额", true, true);
                if (terms.compareTo(discount) <= 0) {
                    throw ruleException(String.format(
                            "满减券使用门槛(%s)必须大于优惠金额(%s)，请调整 termsOfUse 或 maximumDiscountAmount",
                            terms.stripTrailingZeros().toPlainString(),
                            discount.stripTrailingZeros().toPlainString()));
                }
            }
            case PERCENTAGE_DISCOUNT -> {
                validateDiscountRate(consumeNode);
                readMoney(consumeNode, "maximumDiscountAmount", "折扣券最高优惠封顶金额", true, true);
                readMoney(consumeNode, "termsOfUse", "使用门槛", false, false);
            }
        }
    }

    private void validateDiscountRate(JsonNode consumeNode) {
        JsonNode rateNode = consumeNode.get("discountRate");
        if (rateNode == null || rateNode.isNull() || !rateNode.isNumber()) {
            throw ruleException("折扣券必须填写数值型的折扣率 discountRate（范围 0.1~0.99）");
        }
        BigDecimal rate = rateNode.decimalValue();
        if (rate.stripTrailingZeros().scale() > 2
                || rate.compareTo(MIN_DISCOUNT_RATE) < 0
                || rate.compareTo(MAX_DISCOUNT_RATE) > 0) {
            throw ruleException("折扣券折扣率 discountRate 必须在 0.1~0.99 之间且最多保留 2 位小数");
        }
    }

    private BigDecimal readMoney(JsonNode root, String fieldName, String label, boolean required, boolean mustBePositive) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            if (required) {
                throw ruleException(String.format("使用规则缺少必填字段 %s（%s）", fieldName, label));
            }
            return null;
        }
        if (!node.isNumber()) {
            throw ruleException(String.format("使用规则字段 %s（%s）必须为数字", fieldName, label));
        }
        BigDecimal value = node.decimalValue();
        if (value.stripTrailingZeros().scale() > 2) {
            throw ruleException(String.format("使用规则字段 %s（%s）最多保留 2 位小数", fieldName, label));
        }
        int cmp = value.compareTo(BigDecimal.ZERO);
        if (mustBePositive ? cmp <= 0 : cmp < 0) {
            String condition = mustBePositive ? "必须大于 0" : "不能小于 0";
            throw ruleException(String.format("使用规则字段 %s（%s）%s", fieldName, label, condition));
        }
        return value;
    }

    private static ClientException ruleException(String message) {
        return new ClientException(message, BaseErrorCode.INVALID_COUPON_RULE);
    }
}
