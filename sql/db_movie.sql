-- ============================================
-- 数据库：db_movie（电影服务）
-- ============================================
CREATE DATABASE IF NOT EXISTS db_movie
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE db_movie;

-- 电影分类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(32)  NOT NULL COMMENT '分类名称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电影分类表';

INSERT INTO `category` (`id`, `name`) VALUES
(1, '动作'),
(2, '喜剧'),
(3, '科幻'),
(4, '爱情'),
(5, '悬疑'),
(6, '动画'),
(7, '恐怖'),
(8, '纪录片');

-- 电影表
DROP TABLE IF EXISTS `movie`;
CREATE TABLE `movie` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '电影ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '电影名称',
    `category_id` BIGINT       NOT NULL COMMENT '分类ID',
    `director`    VARCHAR(64)  DEFAULT NULL COMMENT '导演',
    `cast`        VARCHAR(512) DEFAULT NULL COMMENT '主演（逗号分隔）',
    `duration`    INT          NOT NULL DEFAULT 90 COMMENT '时长（分钟）',
    `poster_url`  VARCHAR(512) DEFAULT NULL COMMENT '海报URL',
    `description` TEXT         DEFAULT NULL COMMENT '简介',
    `rating`      DECIMAL(2,1) DEFAULT 0.0 COMMENT '评分',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-即将上映，2-热映中，3-已下映',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电影表';

-- 预置 20 部电影
INSERT INTO `movie` (`name`, `category_id`, `director`, `cast`, `duration`, `description`, `rating`, `status`) VALUES
('流浪地球3',      3, '郭帆',    '吴京,刘德华,李雪健',         173, '太阳急速老化，持续膨胀。面对绝境，人类将开启"流浪地球计划"的最终章。', 9.2, 2),
('战狼3',          1, '吴京',    '吴京,余男,赵文卓',           130, '冷锋再度出征，面对前所未有的国际危机。', 8.5, 2),
('满江红',         5, '张艺谋',  '沈腾,易烊千玺,张译',         159, '南宋绍兴年间，岳飞死后四年，秦桧率兵与金国会谈。', 7.8, 2),
('唐人街探案4',     2, '陈思诚',  '王宝强,刘昊然',             135, '唐仁和秦风再次踏上探案之旅，这一次的谜题更加扑朔迷离。', 7.5, 2),
('长津湖之水门桥',  1, '徐克',    '吴京,易烊千玺,段奕宏',       149, '志愿军七连战士在水门桥阻击美军陆战一师。', 9.0, 3),
('铃芽之旅',       6, '新海诚',  '原菜乃华,松村北斗',          121, '17岁少女铃芽遇见了寻找"门"的旅行青年。', 8.1, 2),
('毒舌律师',       2, '吴炜伦',  '黄子华,谢君豪,王丹妮',       134, '废柴律师林凉水为深陷虐儿案的国际名模辩护。', 7.9, 3),
('封神第二部',     3, '乌尔善',  '费翔,黄渤,于适',            152, '殷商大军兵临城下，西岐保卫战一触即发。', 8.3, 1),
('哪吒之魔童闹海',  6, '饺子',    '吕艳婷,囧森瑟夫',           110, '哪吒与敖丙在天劫后灵魂得以保全，太乙真人用七色宝莲为其重塑肉身。', 9.5, 1),
('孤注一掷',       5, '申奥',    '张艺兴,金晨,王传君',         130, '程序员潘生和模特安娜被高薪招聘诱骗至境外诈骗工厂。', 8.0, 3),
('消失的她',       5, '陈思诚',  '朱一龙,倪妮,文咏珊',         125, '何非的妻子在海外度假时离奇失踪。', 7.7, 3),
('热烈',           2, '大鹏',    '黄渤,王一博',               124, '街舞老炮儿丁雷偶遇卖艺少年陈烁。', 7.6, 3),
('速度与激情11',    1, '路易斯',  '范·迪塞尔,杰森·斯坦森',     141, '唐老大和他的家人们将面对前所未有的挑战。', 8.8, 1),
('阿凡达4',         3, '詹姆斯',  '萨姆·沃辛顿,佐伊·索尔达娜', 192, '潘多拉星球迎来了新的访客和新的威胁。', 8.9, 1),
('熊出没·重返地球', 6, '林汇达',  '张伟,张秉君,谭笑',          99, '神秘的"外星人"阿布造访狗熊岭。', 7.0, 3),
('这个杀手不太冷静', 2, '邢文雄',  '马丽,魏翔',                109, '龙套演员魏成功被女明星米兰选中出演男一号。', 7.2, 3),
('无名',            5, '程耳',    '梁朝伟,王一博,周迅',         128, '抗战时期，中共地下党员冒死送出情报。', 7.1, 3),
('九龙城寨之围城',  1, '郑保瑞',  '古天乐,林峯,洪金宝',        126, '落魄青年陈洛军误闯入九龙城寨。', 8.2, 2),
('飞驰人生3',       2, '韩寒',    '沈腾,范丞丞,尹正',          120, '张驰再次坐进赛车，这次他要冲击国际赛事。', 7.4, 1),
('奥本海默',        8, '诺兰',    '基里安·墨菲,艾米莉·布朗特',  180, '讲述了美国"原子弹之父"罗伯特·奥本海默的故事。', 8.9, 3);
