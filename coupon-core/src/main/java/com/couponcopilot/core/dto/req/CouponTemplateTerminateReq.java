package com.couponcopilot.core.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * B 端紧急下线优惠券模板请求。
 */
@Data
@Schema(description = "B 端紧急下线优惠券模板请求")
public class CouponTemplateTerminateReq {

    @NotNull(message = "优惠券模板ID不能为空")
    @Schema(description = "优惠券模板ID", example = "2126248512843792385")
    private Long templateId;
}
