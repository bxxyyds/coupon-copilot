package com.couponcopilot.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.common.exception.ServiceException;
import com.couponcopilot.core.common.enums.CouponSourceEnum;
import com.couponcopilot.core.common.enums.CouponStatusEnum;
import com.couponcopilot.core.common.enums.CouponTargetEnum;
import com.couponcopilot.core.dao.entity.CouponTemplateDO;
import com.couponcopilot.core.dao.mapper.CouponTemplateMapper;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.req.CouponTemplateIncreaseStockReq;
import com.couponcopilot.core.dto.req.CouponTemplatePageReq;
import com.couponcopilot.core.dto.req.CouponTemplateTerminateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.dto.resp.CouponTemplateResp;
import com.couponcopilot.core.dto.resp.PageResp;
import com.couponcopilot.core.service.CouponTemplateService;
import com.couponcopilot.core.service.chain.ChainBizMark;
import com.couponcopilot.core.service.chain.ChainContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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
        Long shopId = resolveShopIdForCreate(req.getSource());
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

    @Override
    public PageResp<CouponTemplateResp> pageTemplates(CouponTemplatePageReq req) {
        UserContext.requireRole(UserRole.MERCHANT);
        Long shopId = resolveManageShopId();

        String keyword = StringUtils.hasText(req.getName()) ? req.getName().trim() : null;
        LambdaQueryWrapper<CouponTemplateDO> wrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopId, shopId)
                .like(keyword != null, CouponTemplateDO::getName, keyword)
                .eq(req.getStatus() != null, CouponTemplateDO::getStatus, req.getStatus())
                .eq(req.getType() != null, CouponTemplateDO::getType, req.getType())
                .lt(req.getMaxStock() != null, CouponTemplateDO::getStock, req.getMaxStock())
                .orderByDesc(CouponTemplateDO::getCreateTime, CouponTemplateDO::getId);

        Page<CouponTemplateDO> page = couponTemplateMapper.selectPage(
                new Page<>(req.getCurrent(), req.getSize()), wrapper);
        List<CouponTemplateResp> records = page.getRecords().stream()
                .map(this::toResp)
                .toList();
        return PageResp.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public CouponTemplateResp increaseStock(CouponTemplateIncreaseStockReq req) {
        UserContext.requireRole(UserRole.MERCHANT);
        Long shopId = resolveManageShopId();

        CouponTemplateDO existing = requireActiveTemplate(shopId, req.getTemplateId(), "优惠券模板已结束，禁止增发库存");
        if (!existing.getValidEndTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException("优惠券模板已过期，禁止增发库存", BaseErrorCode.COUPON_TEMPLATE_ENDED);
        }
        if (existing.getStock() + (long) req.getDelta() > Integer.MAX_VALUE) {
            throw new ClientException("增发后库存超出上限");
        }

        int updated = couponTemplateMapper.increaseStock(shopId, req.getTemplateId(), req.getDelta());
        if (updated == 0) {
            throw new ServiceException("优惠券模板已结束，禁止增发库存", BaseErrorCode.COUPON_TEMPLATE_ENDED);
        }

        CouponTemplateDO refreshed = selectByShopAndId(shopId, req.getTemplateId());
        if (refreshed == null) {
            existing.setStock(existing.getStock() + req.getDelta());
            refreshed = existing;
        }
        log.info("Increased coupon template stock id={}, shopId={}, delta={}, newStock={}",
                refreshed.getId(), shopId, req.getDelta(), refreshed.getStock());
        return toResp(refreshed);
    }

    @Override
    public void terminateTemplate(CouponTemplateTerminateReq req) {
        UserContext.requireRole(UserRole.MERCHANT);
        Long shopId = resolveManageShopId();

        requireActiveTemplate(shopId, req.getTemplateId(), "优惠券模板已结束，无需重复下线");
        int updated = couponTemplateMapper.terminateTemplate(shopId, req.getTemplateId());
        if (updated == 0) {
            throw new ServiceException("优惠券模板已结束，无需重复下线", BaseErrorCode.COUPON_TEMPLATE_ENDED);
        }
        log.info("Terminated coupon template id={}, shopId={}", req.getTemplateId(), shopId);
    }

    private Long resolveShopIdForCreate(Integer source) {
        if (CouponSourceEnum.PLATFORM.getCode().equals(source)) {
            return 0L;
        }
        Long shopId = UserContext.requireShopId();
        if (shopId <= 0) {
            throw new ClientException("店铺券请求头 X-Shop-Id 必须大于0");
        }
        return shopId;
    }

    private Long resolveManageShopId() {
        Long shopId = UserContext.requireShopId();
        if (shopId < 0) {
            throw new ClientException("请求头 X-Shop-Id 不能小于 0");
        }
        return shopId;
    }

    private CouponTemplateDO requireActiveTemplate(Long shopId, Long templateId, String endedMessage) {
        CouponTemplateDO existing = selectByShopAndId(shopId, templateId);
        if (existing == null) {
            throw new ServiceException("优惠券模板不存在", BaseErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        if (!CouponStatusEnum.ACTIVE.getCode().equals(existing.getStatus())) {
            throw new ServiceException(endedMessage, BaseErrorCode.COUPON_TEMPLATE_ENDED);
        }
        return existing;
    }

    private CouponTemplateDO selectByShopAndId(Long shopId, Long templateId) {
        return couponTemplateMapper.selectOne(Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopId, shopId)
                .eq(CouponTemplateDO::getId, templateId));
    }

    private CouponTemplateResp toResp(CouponTemplateDO entity) {
        return CouponTemplateResp.builder()
                .id(entity.getId())
                .shopId(entity.getShopId())
                .name(entity.getName())
                .source(entity.getSource())
                .target(entity.getTarget())
                .goodsCode(entity.getGoodsCode())
                .type(entity.getType())
                .validStartTime(entity.getValidStartTime())
                .validEndTime(entity.getValidEndTime())
                .stock(entity.getStock())
                .receiveRule(entity.getReceiveRule())
                .consumeRule(entity.getConsumeRule())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
