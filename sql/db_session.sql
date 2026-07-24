-- ============================================
-- 数据库：db_session（场次服务）
-- ============================================
CREATE DATABASE IF NOT EXISTS db_session
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE db_session;

-- 场次表
DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '场次ID',
    `movie_id`        BIGINT        NOT NULL COMMENT '电影ID',
    `cinema_hall`     VARCHAR(32)   NOT NULL COMMENT '放映厅（如IMAX厅、3号厅）',
    `start_time`      DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME      NOT NULL COMMENT '结束时间',
    `price`           DECIMAL(8,2)  NOT NULL COMMENT '票价',
    `total_seats`     INT           NOT NULL DEFAULT 0 COMMENT '总座位数',
    `available_seats` INT           NOT NULL DEFAULT 0 COMMENT '剩余可用座位数',
    `status`          TINYINT       NOT NULL DEFAULT 2 COMMENT '状态：1-即将开始，2-售票中，3-已结束，4-已取消',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_movie_id` (`movie_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次表';

-- 座位表
DROP TABLE IF EXISTS `seat`;
CREATE TABLE `seat` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '座位ID',
    `session_id`  BIGINT       NOT NULL COMMENT '场次ID',
    `seat_row`    INT          NOT NULL COMMENT '排号',
    `seat_col`    INT          NOT NULL COMMENT '列号',
    `seat_code`   VARCHAR(8)   NOT NULL COMMENT '座位编号（如 A-03）',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-AVAILABLE，1-LOCKED，2-SOLD',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `expire_time` DATETIME     DEFAULT NULL COMMENT '锁定过期时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    UNIQUE KEY `uk_session_seat` (`session_id`, `seat_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- ============================================
-- 初始化场次 + 座位数据（以 movie_id=1 为例）
-- 每个场次生成 80 个座位（8排 × 10列）
-- ============================================

-- 场次1：流浪地球3 - IMAX厅
INSERT INTO `session` (`id`, `movie_id`, `cinema_hall`, `start_time`, `end_time`, `price`, `total_seats`, `available_seats`, `status`) VALUES
(1, 1, 'IMAX厅', '2026-07-23 10:00:00', '2026-07-23 12:53:00', 79.90, 80, 80, 2),
(2, 1, 'IMAX厅', '2026-07-23 14:00:00', '2026-07-23 16:53:00', 89.90, 80, 80, 2),
(3, 1, 'IMAX厅', '2026-07-23 19:00:00', '2026-07-23 21:53:00', 99.90, 80, 80, 2),
(4, 1, '3号厅',  '2026-07-23 11:00:00', '2026-07-23 13:53:00', 49.90, 80, 80, 2),
(5, 1, '3号厅',  '2026-07-23 16:00:00', '2026-07-23 18:53:00', 59.90, 80, 80, 2);

-- 为场次生成座位（存储过程 + 循环）
DELIMITER //
CREATE PROCEDURE init_seats()
BEGIN
    DECLARE s_id BIGINT DEFAULT 1;
    DECLARE row_idx INT;
    DECLARE col_idx INT;
    DECLARE row_char CHAR(1);

    WHILE s_id <= 5 DO
        SET row_idx = 0;
        WHILE row_idx < 8 DO
            SET col_idx = 0;
            SET row_char = CHAR(65 + row_idx); -- A-H
            WHILE col_idx < 10 DO
                INSERT INTO `seat` (`session_id`, `seat_row`, `seat_col`, `seat_code`)
                VALUES (s_id, row_idx + 1, col_idx + 1,
                        CONCAT(row_char, '-', LPAD(col_idx + 1, 2, '0')));
                SET col_idx = col_idx + 1;
            END WHILE;
            SET row_idx = row_idx + 1;
        END WHILE;
        SET s_id = s_id + 1;
    END WHILE;
END //
DELIMITER ;

CALL init_seats();
DROP PROCEDURE IF EXISTS init_seats;
