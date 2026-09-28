-- ============================================================
-- 06 货位表 shelf_location
-- ============================================================
CREATE TABLE IF NOT EXISTS `shelf_location` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shelf_id`    BIGINT      NOT NULL                COMMENT '所属货架 ID',
  `location_no` VARCHAR(50) NOT NULL                COMMENT '货位编号',
  `row_no`      INT         NOT NULL                COMMENT '行号',
  `column_no`   INT         NOT NULL                COMMENT '列号',
  `capacity`    INT         NOT NULL DEFAULT 1      COMMENT '容量',
  `used_count`  INT         NOT NULL DEFAULT 0      COMMENT '已使用数量',
  `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '货位状态：1=空闲，2=部分占用，3=已满，4=停用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_location_no` (`location_no`),
  KEY `idx_shelf_id` (`shelf_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货位表';
