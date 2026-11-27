package com.couponcopilot.core.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.couponcopilot.core.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板 Mapper。
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    /**
     * 原子增发模板库存（携带分片键 shop_id，仅允许对生效中模板操作）。
     */
    @Update("""
            UPDATE t_coupon_template
            SET stock = stock + #{delta}, update_time = NOW()
            WHERE shop_id = #{shopId}
              AND id = #{templateId}
              AND status = 0
              AND del_flag = 0
            """)
    int increaseStock(@Param("shopId") Long shopId,
                      @Param("templateId") Long templateId,
                      @Param("delta") Integer delta);

    /**
     * 紧急下线模板（携带分片键 shop_id，将状态置为已结束）。
     */
    @Update("""
            UPDATE t_coupon_template
            SET status = 1, update_time = NOW()
            WHERE shop_id = #{shopId}
              AND id = #{templateId}
              AND status = 0
              AND del_flag = 0
            """)
    int terminateTemplate(@Param("shopId") Long shopId,
                          @Param("templateId") Long templateId);
}
