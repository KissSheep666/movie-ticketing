-- ============================================
-- 数据库：db_order（订单服务）
-- ============================================
CREATE DATABASE IF NOT EXISTS db_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE db_order;

-- 订单表
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`    VARCHAR(32)   NOT NULL COMMENT '订单编号',
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `movie_id`    BIGINT        NOT NULL COMMENT '电影ID',
    `session_id`  BIGINT        NOT NULL COMMENT '场次ID',
    `total_amount` DECIMAL(8,2) NOT NULL COMMENT '订单总金额',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0-PENDING，1-PAID，2-CANCELLED，3-REFUNDED',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `expire_time` DATETIME      NOT NULL COMMENT '过期时间（创建时间+15分钟）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表（座位）
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`   BIGINT        NOT NULL COMMENT '订单ID',
    `seat_id`    BIGINT        NOT NULL COMMENT '座位ID',
    `seat_code`  VARCHAR(8)    NOT NULL COMMENT '座位编号',
    `price`      DECIMAL(8,2)  NOT NULL COMMENT '该座位价格',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
