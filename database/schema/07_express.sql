-- ============================================================
-- 07 快递表 express（核心业务表）
-- 状态：WAITING / STORED / WAITING_PICKUP / PICKED_UP / EXCEPTION / RETURNED
-- ============================================================
CREATE TABLE IF NOT EXISTS `express` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `express_no`       VARCHAR(100)  NOT NULL                COMMENT '快递单号',
  `company_id`       BIGINT        NOT NULL                COMMENT '快递公司 ID',
  `sender_name`      VARCHAR(50)            DEFAULT NULL    COMMENT '寄件人',
  `sender_phone`     VARCHAR(20)            DEFAULT NULL    COMMENT '寄件人电话',
  `receiver_name`    VARCHAR(50)   NOT NULL                COMMENT '收件人',
  `receiver_phone`   VARCHAR(20)   NOT NULL                COMMENT '收件人电话',
  `receiver_user_id` BIGINT                 DEFAULT NULL    COMMENT '收件用户 ID',
  `pickup_code`      VARCHAR(20)   NOT NULL                COMMENT '取件码',
  `location_id`      BIGINT                 DEFAULT NULL    COMMENT '货位 ID',
  `status`           VARCHAR(30)   NOT NULL                COMMENT '快递状态：WAITING/STORED/WAITING_PICKUP/PICKED_UP/EXCEPTION/RETURNED',
  `size_type`        VARCHAR(20)            DEFAULT NULL    COMMENT '尺寸类型：SMALL/MEDIUM/LARGE',
  `weight`           DECIMAL(10,2)          DEFAULT NULL    COMMENT '重量（kg）',
  `arrival_time`     DATETIME               DEFAULT NULL    COMMENT '到达时间',
  `storage_time`     DATETIME               DEFAULT NULL    COMMENT '入库时间',
  `pickup_time`      DATETIME               DEFAULT NULL    COMMENT '取件时间',
  `overdue_flag`     TINYINT      NOT NULL DEFAULT 0        COMMENT '是否超期：0=未超期，1=超期',
  `remark`           VARCHAR(500)          DEFAULT NULL     COMMENT '备注',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_express_no` (`express_no`),
  UNIQUE KEY `uk_pickup_code` (`pickup_code`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_receiver_user_id` (`receiver_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_location_id` (`location_id`),
  KEY `idx_arrival_time` (`arrival_time`),
  KEY `idx_pickup_time` (`pickup_time`),
  KEY `idx_overdue_flag` (`overdue_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快递表';
