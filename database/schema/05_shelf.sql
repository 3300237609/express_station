-- ============================================================
-- 05 货架表 shelf
-- ============================================================
CREATE TABLE IF NOT EXISTS `shelf` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shelf_no`    VARCHAR(50)  NOT NULL                COMMENT '货架编号',
  `area`        VARCHAR(50)  NOT NULL                COMMENT '所属区域',
  `capacity`    INT          NOT NULL                COMMENT '总货位数量',
  `used_count`  INT          NOT NULL DEFAULT 0      COMMENT '已使用货位数量',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '货架状态：1=正常，0=停用',
  `remark`      VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shelf_no` (`shelf_no`),
  KEY `idx_area` (`area`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货架表';
