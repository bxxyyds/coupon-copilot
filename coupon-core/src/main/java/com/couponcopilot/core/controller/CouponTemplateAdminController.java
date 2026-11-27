package com.couponcopilot.core.controller;

import com.couponcopilot.common.result.Result;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.req.CouponTemplateIncreaseStockReq;
import com.couponcopilot.core.dto.req.CouponTemplatePageReq;
import com.couponcopilot.core.dto.req.CouponTemplateTerminateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.dto.resp.CouponTemplateResp;
import com.couponcopilot.core.dto.resp.PageResp;
import com.couponcopilot.core.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端优惠券模板管理控制器。
 */
@Tag(name = "B端优惠券模板管理")
@RestController
@RequestMapping("/api/coupon/admin/template")
@RequiredArgsConstructor
public class CouponTemplateAdminController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "创建优惠券模板", description = "B端商家创建优惠券模板，需要请求头 X-Role: MERCHANT 与 X-Shop-Id")
    @PostMapping("/create")
    public Result<CouponTemplateCreateResp> create(@Valid @RequestBody CouponTemplateCreateReq req) {
        return Result.success(couponTemplateService.createTemplate(req));
    }

    @Operation(summary = "分页查询优惠券模板", description = "按当前店铺分页筛选模板，支持名称模糊搜索、状态、类型与低库存筛选(stock < maxStock)")
    @GetMapping("/page")
    public Result<PageResp<CouponTemplateResp>> page(@Valid @ParameterObject CouponTemplatePageReq req) {
        return Result.success(couponTemplateService.pageTemplates(req));
    }

    @Operation(summary = "增发优惠券库存", description = "仅生效中且未过期的模板允许原子增发库存")
    @PostMapping("/increase-stock")
    public Result<CouponTemplateResp> increaseStock(@Valid @RequestBody CouponTemplateIncreaseStockReq req) {
        return Result.success(couponTemplateService.increaseStock(req));
    }

    @Operation(summary = "紧急下线优惠券模板", description = "将生效中的优惠券模板立即置为已结束")
    @PostMapping("/terminate")
    public Result<Void> terminate(@Valid @RequestBody CouponTemplateTerminateReq req) {
        couponTemplateService.terminateTemplate(req);
        return Result.success();
    }
}
