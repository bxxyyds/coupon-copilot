package com.couponcopilot.core.controller;

import com.couponcopilot.common.result.Result;
import com.couponcopilot.core.dto.req.CouponTemplateCreateReq;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
