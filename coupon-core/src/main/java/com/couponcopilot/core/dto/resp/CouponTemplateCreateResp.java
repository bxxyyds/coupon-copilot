package com.couponcopilot.core.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建优惠券模板响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建优惠券模板响应")
public class CouponTemplateCreateResp {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "优惠券模板ID（雪花算法，字符串防前端精度丢失）", example = "1860000000000000001")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "店铺ID（平台券固定为0）", example = "1001")
    private Long shopId;

    @Schema(description = "优惠券名称", example = "双十一满200减30店铺券")
    private String name;

    @Schema(description = "状态：0生效中 1已结束", example = "0")
    private Integer status;

    @Schema(description = "发行库存", example = "1000")
    private Integer stock;
}
