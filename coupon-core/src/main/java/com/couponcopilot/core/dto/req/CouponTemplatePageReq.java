package com.couponcopilot.core.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * B 端分页查询优惠券模板请求。
 */
@Data
@Schema(description = "B 端分页查询优惠券模板请求")
public class CouponTemplatePageReq {

    @Min(value = 1, message = "页码 current 必须大于等于 1")
    @Schema(description = "当前页码", example = "1", defaultValue = "1")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数 size 必须在 1~100 之间")
    @Max(value = 100, message = "每页条数 size 必须在 1~100 之间")
    @Schema(description = "每页条数（最大100）", example = "10", defaultValue = "10")
    private Long size = 10L;

    @Schema(description = "优惠券名称模糊搜索", example = "满200")
    private String name;

    @Min(value = 0, message = "状态筛选非法")
    @Max(value = 1, message = "状态筛选非法")
    @Schema(description = "状态筛选：0生效中 1已结束")
    private Integer status;

    @Min(value = 0, message = "券类型筛选非法")
    @Max(value = 2, message = "券类型筛选非法")
    @Schema(description = "类型筛选：0立减 1满减 2折扣")
    private Integer type;

    @Min(value = 1, message = "低库存阈值 maxStock 必须大于 0")
    @Schema(description = "低库存筛选阈值（查询剩余库存严格小于该值的模板）", example = "100")
    private Integer maxStock;
}
