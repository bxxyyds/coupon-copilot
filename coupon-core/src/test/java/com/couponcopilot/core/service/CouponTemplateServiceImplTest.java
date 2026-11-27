package com.couponcopilot.core.service;

import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserInfo;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.core.dao.entity.CouponTemplateDO;
import com.couponcopilot.core.dao.mapper.CouponTemplateMapper;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.service.chain.ChainContext;
import com.couponcopilot.core.service.chain.template.TemplateParamNotNullChainHandler;
import com.couponcopilot.core.service.chain.template.TemplateRuleJsonChainHandler;
import com.couponcopilot.core.service.chain.template.TemplateTimeChainHandler;
import com.couponcopilot.core.service.impl.CouponTemplateServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponTemplateServiceImplTest {

    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    private CouponTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        ChainContext chainContext = new ChainContext(List.of(
                new TemplateRuleJsonChainHandler(new ObjectMapper()),
                new TemplateParamNotNullChainHandler(),
                new TemplateTimeChainHandler()
        ));
        service = new CouponTemplateServiceImpl(couponTemplateMapper, chainContext);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldCreateShopCouponSuccessfully() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateCreateReq req = validReq();
        req.setGoodsCode("SHOULD_BE_IGNORED_FOR_ALL_SHOP");

        when(couponTemplateMapper.insert(any(CouponTemplateDO.class))).thenAnswer(invocation -> {
            CouponTemplateDO arg = invocation.getArgument(0);
            arg.setId(1234567890123456789L);
            return 1;
        });

        CouponTemplateCreateResp resp = service.createTemplate(req);

        ArgumentCaptor<CouponTemplateDO> captor = ArgumentCaptor.forClass(CouponTemplateDO.class);
        verify(couponTemplateMapper).insert(captor.capture());
        CouponTemplateDO saved = captor.getValue();
        assertEquals(1001L, saved.getShopId());
        assertEquals(0, saved.getStatus());
        assertNull(saved.getGoodsCode());

        assertEquals(1234567890123456789L, resp.getId());
        assertEquals(1001L, resp.getShopId());
        assertEquals(0, resp.getStatus());
        assertEquals(500, resp.getStock());
    }

    @Test
    void shouldCreatePlatformCouponWithZeroShopId() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateCreateReq req = validReq();
        req.setSource(1);

        when(couponTemplateMapper.insert(any(CouponTemplateDO.class))).thenAnswer(invocation -> {
            CouponTemplateDO arg = invocation.getArgument(0);
            arg.setId(2234567890123456789L);
            return 1;
        });

        CouponTemplateCreateResp resp = service.createTemplate(req);

        ArgumentCaptor<CouponTemplateDO> captor = ArgumentCaptor.forClass(CouponTemplateDO.class);
        verify(couponTemplateMapper).insert(captor.capture());
        assertEquals(0L, captor.getValue().getShopId());
        assertEquals(0L, resp.getShopId());
    }

    @Test
    void shouldRejectNonMerchantRole() {
        UserContext.set(UserInfo.builder().role(UserRole.CUSTOMER).userId(2001L).build());
        CouponTemplateCreateReq req = validReq();

        assertThrows(ClientException.class, () -> service.createTemplate(req));
        verify(couponTemplateMapper, never()).insert(any(CouponTemplateDO.class));
    }

    @Test
    void shouldRejectShopCouponWhenShopIdMissingOrNonPositive() {
        CouponTemplateCreateReq req = validReq();

        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(null).build());
        assertThrows(ClientException.class, () -> service.createTemplate(req));

        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(0L).build());
        assertThrows(ClientException.class, () -> service.createTemplate(req));

        verify(couponTemplateMapper, never()).insert(any(CouponTemplateDO.class));
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());

        CouponTemplateCreateReq reversedReq = validReq();
        reversedReq.setValidEndTime(reversedReq.getValidStartTime().minusHours(1));
        ClientException ex1 = assertThrows(ClientException.class, () -> service.createTemplate(reversedReq));
        assertEquals(BaseErrorCode.INVALID_TIME_RANGE.code(), ex1.getErrorCode());

        CouponTemplateCreateReq expiredReq = validReq();
        expiredReq.setValidStartTime(LocalDateTime.now().minusDays(10));
        expiredReq.setValidEndTime(LocalDateTime.now().minusDays(1));
        ClientException ex2 = assertThrows(ClientException.class, () -> service.createTemplate(expiredReq));
        assertEquals(BaseErrorCode.INVALID_TIME_RANGE.code(), ex2.getErrorCode());
    }

    @Test
    void shouldRejectSpecificGoodsWithoutGoodsCode() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateCreateReq req = validReq();
        req.setTarget(0);
        req.setGoodsCode("   ");

        assertThrows(ClientException.class, () -> service.createTemplate(req));
        verify(couponTemplateMapper, never()).insert(any(CouponTemplateDO.class));
    }

    @Test
    void shouldRejectInvalidJsonRuleWithRuleErrorCode() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateCreateReq req = validReq();
        req.setConsumeRule("not-json");

        ClientException ex = assertThrows(ClientException.class, () -> service.createTemplate(req));
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex.getErrorCode());
        verify(couponTemplateMapper, never()).insert(any(CouponTemplateDO.class));
    }

    @Test
    void shouldRejectThresholdLessThanDiscountBeforeInsert() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateCreateReq req = validReq();
        req.setConsumeRule("{\"termsOfUse\":100,\"maximumDiscountAmount\":120}");

        ClientException ex = assertThrows(ClientException.class, () -> service.createTemplate(req));
        assertEquals(BaseErrorCode.INVALID_COUPON_RULE.code(), ex.getErrorCode());
        verify(couponTemplateMapper, never()).insert(any(CouponTemplateDO.class));
    }

    private static CouponTemplateCreateReq validReq() {
        CouponTemplateCreateReq req = new CouponTemplateCreateReq();
        req.setName("双十一满200减30店铺券");
        req.setSource(0);
        req.setTarget(1);
        req.setType(1);
        req.setValidStartTime(LocalDateTime.now().plusDays(1));
        req.setValidEndTime(LocalDateTime.now().plusDays(30));
        req.setStock(500);
        req.setReceiveRule("{\"limitPerPerson\":2}");
        req.setConsumeRule("{\"termsOfUse\":200,\"maximumDiscountAmount\":30}");
        return req;
    }
}
