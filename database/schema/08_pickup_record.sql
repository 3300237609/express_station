-- ============================================================
-- 08 取件记录表 pickup_record
-- 取件方式：CODE / SCAN / ADMIN / COURIER
-- ============================================================
CREATE TABLE IF NOT EXISTS `pickup_record` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `express_id`    BIGINT      NOT NULL                COMMENT '快递 ID',
  `user_id`       BIGINT      NOT NULL                COMMENT '用户 ID（取件人）',
  `pickup_code`   VARCHAR(20) NOT NULL                COMMENT '取件码',
  `operator_id`   BIGINT               DEFAULT NULL    COMMENT '操作人员 ID',
  `pickup_method` VARCHAR(30) NOT NULL                COMMENT '取件方式：CODE/SCAN/ADMIN/COURIER',
  `pickup_time`   DATETIME    NOT NULL                COMMENT '取件时间',
  `remark`        VARCHAR(255)         DEFAULT NULL    COMMENT '备注',
  `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_express_id` (`express_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pickup_time` (`pickup_time`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='取件记录表';
