package com.couponcopilot.core.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * B 端增发优惠券库存请求。
 */
@Data
@Schema(description = "B 端增发优惠券库存请求")
public class CouponTemplateIncreaseStockReq {

    @NotNull(message = "优惠券模板ID不能为空")
    @Schema(description = "优惠券模板ID", example = "2126248512843792385")
    private Long templateId;

    @NotNull(message = "增发数量不能为空")
    @Min(value = 1, message = "增发数量必须大于 0")
    @Max(value = 10000000, message = "单次增发数量不能超过 10000000")
    @Schema(description = "增发库存数量（正整数）", example = "500")
    private Integer delta;
}
