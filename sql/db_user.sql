-- ============================================
-- 数据库：db_user（用户服务）
-- ============================================
CREATE DATABASE IF NOT EXISTS db_user
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE db_user;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(16)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 预置用户（密码均为 123456，BCrypt 加密）
-- 实际运行时由 user-service 动态加密，这里仅占位
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `email`) VALUES
('admin',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '管理员',  '13800000001', 'admin@test.com'),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '张三',    '13800000002', 'zhangsan@test.com'),
('lisi',     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '李四',    '13800000003', 'lisi@test.com');
