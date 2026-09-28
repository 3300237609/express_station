-- ============================================================
-- 12 系统配置表 sys_config
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(100) NOT NULL                COMMENT '配置键',
  `config_value` VARCHAR(500) NOT NULL                COMMENT '配置值',
  `description`  VARCHAR(255)          DEFAULT NULL    COMMENT '配置说明',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态：1=启用，0=停用',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
