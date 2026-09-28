-- ============================================================
-- 11 操作日志表 operation_log
-- ============================================================
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        BIGINT       NOT NULL                COMMENT '操作人 ID',
  `username`       VARCHAR(50)  NOT NULL                COMMENT '操作人用户名',
  `role_code`      VARCHAR(50)           DEFAULT NULL    COMMENT '操作角色',
  `module`         VARCHAR(100) NOT NULL                COMMENT '操作模块',
  `operation`      VARCHAR(100) NOT NULL                COMMENT '操作类型',
  `description`    VARCHAR(500)          DEFAULT NULL    COMMENT '操作描述',
  `request_method` VARCHAR(20)           DEFAULT NULL    COMMENT 'HTTP 方法',
  `request_url`    VARCHAR(500)          DEFAULT NULL    COMMENT '请求地址',
  `ip_address`     VARCHAR(50)           DEFAULT NULL    COMMENT 'IP 地址',
  `result`         VARCHAR(20)           DEFAULT NULL    COMMENT '操作结果',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
