CREATE DATABASE IF NOT EXISTS coupon_copilot
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE coupon_copilot;

-- 1. 优惠券模板表（未来按 shop_id 分片）
CREATE TABLE IF NOT EXISTS t_coupon_template (
    id               BIGINT       NOT NULL COMMENT '雪花算法主键ID',
    shop_id          BIGINT       NOT NULL COMMENT '店铺ID（分片键，平台券固定0）',
    name             VARCHAR(128) NOT NULL COMMENT '优惠券名称',
    source           TINYINT      NOT NULL DEFAULT 0 COMMENT '来源：0店铺券 1平台券',
    target           TINYINT      NOT NULL DEFAULT 1 COMMENT '适用对象：0指定商品 1全店通用',
    goods_code       VARCHAR(64)  NULL     COMMENT '商品编码（全店通用为空）',
    type             TINYINT      NOT NULL COMMENT '类型：0立减 1满减 2折扣',
    valid_start_time DATETIME     NOT NULL COMMENT '生效开始时间',
    valid_end_time   DATETIME     NOT NULL COMMENT '生效结束时间',
    stock            INT          NOT NULL COMMENT '剩余发行库存',
    receive_rule     JSON         NOT NULL COMMENT '领取规则JSON，如 {"limitPerPerson":2}',
    consume_rule     JSON         NOT NULL COMMENT '使用规则JSON，如 {"termsOfUse":200,"maximumDiscountAmount":30,"discountRate":0.85}',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0生效中 1已结束',
    del_flag         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop_status (shop_id, status, valid_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 2. 用户领券表（未来按 user_id 分片）
CREATE TABLE IF NOT EXISTS t_user_coupon (
    id                 BIGINT   NOT NULL COMMENT '雪花算法主键ID',
    user_id            BIGINT   NOT NULL COMMENT '用户ID（分片键）',
    shop_id            BIGINT   NOT NULL COMMENT '店铺ID',
    coupon_template_id BIGINT   NOT NULL COMMENT '优惠券模板ID',
    receive_count      INT      NOT NULL COMMENT '用户领取该模板的第N张',
    receive_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    valid_start_time   DATETIME NOT NULL COMMENT '有效期开始时间',
    valid_end_time     DATETIME NOT NULL COMMENT '有效期结束时间',
    use_time           DATETIME NULL     COMMENT '核销时间',
    source             TINYINT  NOT NULL DEFAULT 0 COMMENT '来源：0主动领券 1批量发券',
    status             TINYINT  NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1锁定中 2已使用 3已过期 4已撤回',
    create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tpl_cnt (user_id, coupon_template_id, receive_count),
    KEY idx_user_status (user_id, status, valid_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户领券表';

-- 3. 优惠券锁定与核销结算单表
CREATE TABLE IF NOT EXISTS t_coupon_settlement (
    id                 BIGINT        NOT NULL COMMENT '雪花算法主键ID',
    order_id           VARCHAR(64)   NOT NULL COMMENT '业务订单号（防重唯一键）',
    user_id            BIGINT        NOT NULL COMMENT '用户ID',
    user_coupon_id     BIGINT        NOT NULL COMMENT '用户优惠券ID',
    coupon_template_id BIGINT        NOT NULL COMMENT '优惠券模板ID',
    original_amount    DECIMAL(10,2) NOT NULL COMMENT '订单原价',
    discount_amount    DECIMAL(10,2) NOT NULL COMMENT '优惠金额',
    pay_amount         DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    status             TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0锁定中 1已核销 2已退款',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_user_coupon (user_id, user_coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券结算单表';
