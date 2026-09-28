-- ============================================================
-- 04 快递公司表 express_company
-- ============================================================
CREATE TABLE IF NOT EXISTS `express_company` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_name`  VARCHAR(100) NOT NULL                COMMENT '公司名称',
  `company_code`  VARCHAR(50)  NOT NULL                COMMENT '公司编码',
  `contact_name`  VARCHAR(50)           DEFAULT NULL    COMMENT '联系人',
  `contact_phone` VARCHAR(20)           DEFAULT NULL    COMMENT '联系电话',
  `status`        TINYINT      NOT NULL DEFAULT 1       COMMENT '状态：1=启用，0=停用',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_code` (`company_code`),
  KEY `idx_company_name` (`company_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快递公司表';
