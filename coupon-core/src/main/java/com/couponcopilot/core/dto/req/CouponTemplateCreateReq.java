package com.couponcopilot.core.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * B 端创建优惠券模板请求。
 * 注意：不接受前端传入 shopId，店铺 ID 统一由服务端从 UserContext 读取或按平台券规则固定为 0。
 */
@Data
@Schema(description = "B 端创建优惠券模板请求")
public class CouponTemplateCreateReq {

    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 128, message = "优惠券名称长度不能超过128")
    @Schema(description = "优惠券名称", example = "双十一满200减30店铺券")
    private String name;

    @NotNull(message = "优惠券来源不能为空")
    @Min(value = 0, message = "优惠券来源非法")
    @Max(value = 1, message = "优惠券来源非法")
    @Schema(description = "来源：0店铺券 1平台券", example = "0")
    private Integer source;

    @NotNull(message = "适用对象不能为空")
    @Min(value = 0, message = "适用对象非法")
    @Max(value = 1, message = "适用对象非法")
    @Schema(description = "适用对象：0指定商品 1全店通用", example = "1")
    private Integer target;

    @Size(max = 64, message = "商品编码长度不能超过64")
    @Schema(description = "商品编码（指定商品时必填，全店通用留空）")
    private String goodsCode;

    @NotNull(message = "优惠券类型不能为空")
    @Min(value = 0, message = "优惠券类型非法")
    @Max(value = 2, message = "优惠券类型非法")
    @Schema(description = "类型：0立减 1满减 2折扣", example = "1")
    private Integer type;

    @NotNull(message = "生效开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效开始时间", example = "2026-12-01 00:00:00")
    private LocalDateTime validStartTime;

    @NotNull(message = "生效结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效结束时间", example = "2026-12-31 23:59:59")
    private LocalDateTime validEndTime;

    @NotNull(message = "发行库存不能为空")
    @Min(value = 1, message = "发行库存必须大于0")
    @Schema(description = "初始发行库存", example = "1000")
    private Integer stock;

    @NotBlank(message = "领取规则不能为空")
    @Schema(description = "领取规则JSON", example = "{\"limitPerPerson\":2}")
    private String receiveRule;

    @NotBlank(message = "使用规则不能为空")
    @Schema(description = "使用规则JSON", example = "{\"termsOfUse\":200,\"maximumDiscountAmount\":30}")
    private String consumeRule;
}
