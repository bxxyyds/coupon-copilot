package com.couponcopilot.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserInfo;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.common.exception.ServiceException;
import com.couponcopilot.core.dao.entity.CouponTemplateDO;
import com.couponcopilot.core.dao.mapper.CouponTemplateMapper;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.req.CouponTemplateIncreaseStockReq;
import com.couponcopilot.core.dto.req.CouponTemplatePageReq;
import com.couponcopilot.core.dto.req.CouponTemplateTerminateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.dto.resp.CouponTemplateResp;
import com.couponcopilot.core.dto.resp.PageResp;
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

    @Test
    @SuppressWarnings("unchecked")
    void shouldPageTemplatesForCurrentShop() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplatePageReq req = new CouponTemplatePageReq();
        req.setCurrent(1L);
        req.setSize(10L);
        req.setMaxStock(100);

        CouponTemplateDO row1 = CouponTemplateDO.builder().id(1L).shopId(1001L).name("低库存券A").stock(20).status(0).build();
        CouponTemplateDO row2 = CouponTemplateDO.builder().id(2L).shopId(1001L).name("低库存券B").stock(50).status(0).build();
        when(couponTemplateMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<CouponTemplateDO> page = invocation.getArgument(0);
            page.setTotal(2L);
            page.setRecords(List.of(row1, row2));
            return page;
        });

        PageResp<CouponTemplateResp> resp = service.pageTemplates(req);

        assertEquals(1L, resp.getCurrent());
        assertEquals(10L, resp.getSize());
        assertEquals(2L, resp.getTotal());
        assertEquals(2, resp.getRecords().size());
        assertEquals(20, resp.getRecords().get(0).getStock());
    }

    @Test
    void shouldIncreaseStockForActiveTemplate() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateDO active = CouponTemplateDO.builder()
                .id(100L).shopId(1001L).name("测试券").stock(20).status(0)
                .validEndTime(LocalDateTime.now().plusDays(5))
                .build();
        CouponTemplateDO refreshed = CouponTemplateDO.builder()
                .id(100L).shopId(1001L).name("测试券").stock(70).status(0)
                .validEndTime(active.getValidEndTime())
                .build();

        when(couponTemplateMapper.selectOne(any())).thenReturn(active, refreshed);
        when(couponTemplateMapper.increaseStock(1001L, 100L, 50)).thenReturn(1);

        CouponTemplateIncreaseStockReq req = new CouponTemplateIncreaseStockReq();
        req.setTemplateId(100L);
        req.setDelta(50);

        CouponTemplateResp resp = service.increaseStock(req);
        assertEquals(70, resp.getStock());
        verify(couponTemplateMapper).increaseStock(1001L, 100L, 50);
    }

    @Test
    void shouldRejectIncreaseStockWhenEndedOrExpired() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateIncreaseStockReq req = new CouponTemplateIncreaseStockReq();
        req.setTemplateId(100L);
        req.setDelta(50);

        CouponTemplateDO ended = CouponTemplateDO.builder()
                .id(100L).shopId(1001L).stock(20).status(1)
                .validEndTime(LocalDateTime.now().plusDays(5))
                .build();
        when(couponTemplateMapper.selectOne(any())).thenReturn(ended);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> service.increaseStock(req));
        assertEquals(BaseErrorCode.COUPON_TEMPLATE_ENDED.code(), ex1.getErrorCode());

        CouponTemplateDO expired = CouponTemplateDO.builder()
                .id(100L).shopId(1001L).stock(20).status(0)
                .validEndTime(LocalDateTime.now().minusMinutes(1))
                .build();
        when(couponTemplateMapper.selectOne(any())).thenReturn(expired);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> service.increaseStock(req));
        assertEquals(BaseErrorCode.COUPON_TEMPLATE_ENDED.code(), ex2.getErrorCode());

        verify(couponTemplateMapper, never()).increaseStock(any(), any(), any());
    }

    @Test
    void shouldRejectIncreaseStockWhenTemplateNotFound() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        when(couponTemplateMapper.selectOne(any())).thenReturn(null);

        CouponTemplateIncreaseStockReq req = new CouponTemplateIncreaseStockReq();
        req.setTemplateId(999L);
        req.setDelta(10);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.increaseStock(req));
        assertEquals(BaseErrorCode.COUPON_TEMPLATE_NOT_FOUND.code(), ex.getErrorCode());
    }

    @Test
    void shouldTerminateActiveTemplate() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateDO active = CouponTemplateDO.builder().id(100L).shopId(1001L).status(0).build();
        when(couponTemplateMapper.selectOne(any())).thenReturn(active);
        when(couponTemplateMapper.terminateTemplate(1001L, 100L)).thenReturn(1);

        CouponTemplateTerminateReq req = new CouponTemplateTerminateReq();
        req.setTemplateId(100L);
        service.terminateTemplate(req);

        verify(couponTemplateMapper).terminateTemplate(1001L, 100L);
    }

    @Test
    void shouldRejectDuplicateTerminateWhenAlreadyEnded() {
        UserContext.set(UserInfo.builder().role(UserRole.MERCHANT).userId(9001L).shopId(1001L).build());
        CouponTemplateDO ended = CouponTemplateDO.builder().id(100L).shopId(1001L).status(1).build();
        when(couponTemplateMapper.selectOne(any())).thenReturn(ended);

        CouponTemplateTerminateReq req = new CouponTemplateTerminateReq();
        req.setTemplateId(100L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.terminateTemplate(req));
        assertEquals(BaseErrorCode.COUPON_TEMPLATE_ENDED.code(), ex.getErrorCode());
        verify(couponTemplateMapper, never()).terminateTemplate(any(), any());
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
