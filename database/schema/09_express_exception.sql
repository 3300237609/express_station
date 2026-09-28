-- ============================================================
-- 09 异常件表 express_exception
-- 异常类型：DAMAGE / LOST / WRONG_ADDR / WRONG_INFO / REFUSED / OVERDUE / OTHER
-- 处理状态：PENDING / PROCESSING / RESOLVED / CLOSED / REJECTED
-- ============================================================
CREATE TABLE IF NOT EXISTS `express_exception` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `express_id`     BIGINT        NOT NULL                COMMENT '快递 ID',
  `exception_type` VARCHAR(30)   NOT NULL                COMMENT '异常类型',
  `description`    VARCHAR(1000) NOT NULL                COMMENT '异常描述',
  `image_url`      VARCHAR(500)           DEFAULT NULL    COMMENT '异常图片',
  `reporter_id`    BIGINT        NOT NULL                COMMENT '登记人',
  `handler_id`     BIGINT                 DEFAULT NULL    COMMENT '处理人',
  `handle_status`  VARCHAR(30)   NOT NULL                COMMENT '处理状态',
  `handle_remark`  VARCHAR(1000)          DEFAULT NULL    COMMENT '处理结果',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
  `handle_time`    DATETIME               DEFAULT NULL    COMMENT '处理时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_express_id` (`express_id`),
  KEY `idx_exception_type` (`exception_type`),
  KEY `idx_handle_status` (`handle_status`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_handler_id` (`handler_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常件表';
