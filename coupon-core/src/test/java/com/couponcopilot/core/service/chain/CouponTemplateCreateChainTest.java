package com.couponcopilot.core.service.chain;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.service.chain.template.TemplateParamNotNullChainHandler;
import com.couponcopilot.core.service.chain.template.TemplateRuleJsonChainHandler;
import com.couponcopilot.core.service.chain.template.TemplateTimeChainHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CouponTemplateCreateChainTest {

    private ChainContext chainContext;

    @BeforeEach
    void setUp() {
        // 故意打乱传入顺序（30 -> 10 -> 20），验证 ChainContext 会按 Ordered 排序
        chainContext = new ChainContext(List.of(
                new TemplateRuleJsonChainHandler(new ObjectMapper()),
                new TemplateParamNotNullChainHandler(),
                new TemplateTimeChainHandler()
        ));
    }

    @Test
    void shouldPassForValidInstantThresholdAndPercentageCoupons() {
        CouponTemplateCreateReq instant = baseReq(0, "{\"maximumDiscountAmount\":15}");
        CouponTemplateCreateReq threshold = baseReq(1, "{\"termsOfUse\":200,\"maximumDiscountAmount\":30}");
        CouponTemplateCreateReq percentage = baseReq(2, "{\"discountRate\":0.85,\"maximumDiscountAmount\":50,\"termsOfUse\":100}");

        assertDoesNotThrow(() -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, instant));
        assertDoesNotThrow(() -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, threshold));
        assertDoesNotThrow(() -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, percentage));
    }

    @Test
    void shouldShortCircuitAtParamHandlerBeforeTimeAndJson() {
        CouponTemplateCreateReq req = baseReq(1, "bad-json");
        req.setName(" ");
        req.setValidEndTime(req.getValidStartTime().minusDays(1));

        ClientException ex = assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, req));
        assertEquals(BaseErrorCode.CLIENT_ERROR.code(), ex.getErrorCode());
        assertTrue(ex.getMessage().contains("优惠券名称"));
    }

    @Test
    void shouldShortCircuitAtTimeHandlerBeforeJson() {
        CouponTemplateCreateReq req = baseReq(1, "bad-json");
        req.setValidEndTime(req.getValidStartTime().minusHours(2));

        ClientException ex = assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, req));
        assertEquals(BaseErrorCode.INVALID_TIME_RANGE.code(), ex.getErrorCode());
    }

    @Test
    void shouldRejectThresholdNotGreaterThanDiscount() {
        CouponTemplateCreateReq overDiscount = baseReq(1, "{\"termsOfUse\":100,\"maximumDiscountAmount\":120}");
        ClientException ex1 = assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, overDiscount));
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex1.getErrorCode());
        assertTrue(ex1.getMessage().contains("100"));
        assertTrue(ex1.getMessage().contains("120"));

        CouponTemplateCreateReq equalDiscount = baseReq(1, "{\"termsOfUse\":100,\"maximumDiscountAmount\":100}");
        ClientException ex2 = assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, equalDiscount));
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex2.getErrorCode());
        assertTrue(ex2.getMessage().contains("必须大于优惠金额"));
    }

    @Test
    void shouldRejectInvalidDiscountRateAndMissingCap() {
        CouponTemplateCreateReq tooLow = baseReq(2, "{\"discountRate\":0.05,\"maximumDiscountAmount\":50}");
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, tooLow)).getErrorCode());

        CouponTemplateCreateReq tooHigh = baseReq(2, "{\"discountRate\":1.0,\"maximumDiscountAmount\":50}");
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, tooHigh)).getErrorCode());

        CouponTemplateCreateReq tooManyDecimals = baseReq(2, "{\"discountRate\":0.855,\"maximumDiscountAmount\":50}");
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, tooManyDecimals)).getErrorCode());

        CouponTemplateCreateReq missingCap = baseReq(2, "{\"discountRate\":0.85}");
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, missingCap)).getErrorCode());
    }

    @Test
    void shouldRejectInstantCouponWithPositiveTermsOfUse() {
        CouponTemplateCreateReq req = baseReq(0, "{\"termsOfUse\":100,\"maximumDiscountAmount\":15}");

        ClientException ex = assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, req));
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex.getErrorCode());
    }

    @Test
    void shouldRejectInvalidReceiveRuleLimitPerPerson() {
        for (String badReceiveRule : List.of(
                "{\"limitPerPerson\":0}",
                "{\"limitPerPerson\":1.5}",
                "{\"limitPerPerson\":\"2\"}",
                "[]"
        )) {
            CouponTemplateCreateReq req = baseReq(0, "{\"maximumDiscountAmount\":10}");
            req.setReceiveRule(badReceiveRule);
            ClientException ex = assertThrows(ClientException.class,
                    () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, req));
            assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex.getErrorCode());
        }
    }

    @Test
    void shouldRejectMoneyWithMoreThanTwoDecimalPlaces() {
        CouponTemplateCreateReq req = baseReq(0, "{\"maximumDiscountAmount\":10.123}");

        ClientException ex = assertThrows(ClientException.class,
                () -> chainContext.execute(ChainBizMark.COUPON_TEMPLATE_CREATE, req));
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex.getErrorCode());
    }

    private static CouponTemplateCreateReq baseReq(int type, String consumeRule) {
        CouponTemplateCreateReq req = new CouponTemplateCreateReq();
        req.setName("测试优惠券");
        req.setSource(0);
        req.setTarget(1);
        req.setType(type);
        req.setValidStartTime(LocalDateTime.now().plusDays(1));
        req.setValidEndTime(LocalDateTime.now().plusDays(15));
        req.setStock(100);
        req.setReceiveRule("{\"limitPerPerson\":2}");
        req.setConsumeRule(consumeRule);
        return req;
    }
}
