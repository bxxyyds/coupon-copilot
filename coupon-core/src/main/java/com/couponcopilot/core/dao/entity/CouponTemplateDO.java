package com.couponcopilot.core.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券模板持久化实体，对应表 t_coupon_template（按 shop_id 分片）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_coupon_template")
public class CouponTemplateDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    private String name;

    private Integer source;

    private Integer target;

    private String goodsCode;

    private Integer type;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private Integer stock;

    private String receiveRule;

    private String consumeRule;

    private Integer status;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
