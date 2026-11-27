package com.couponcopilot.core.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券模板详情响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优惠券模板详情响应")
public class CouponTemplateResp {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "优惠券模板ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "店铺ID（平台券固定为0）")
    private Long shopId;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "来源：0店铺券 1平台券")
    private Integer source;

    @Schema(description = "适用对象：0指定商品 1全店通用")
    private Integer target;

    @Schema(description = "商品编码")
    private String goodsCode;

    @Schema(description = "类型：0立减 1满减 2折扣")
    private Integer type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效开始时间")
    private LocalDateTime validStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效结束时间")
    private LocalDateTime validEndTime;

    @Schema(description = "剩余发行库存")
    private Integer stock;

    @Schema(description = "领取规则JSON")
    private String receiveRule;

    @Schema(description = "使用规则JSON")
    private String consumeRule;

    @Schema(description = "状态：0生效中 1已结束")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
