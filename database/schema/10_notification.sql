-- ============================================================
-- 10 消息通知表 notification
-- 消息类型：PICKUP_NOTICE / OVERDUE_NOTICE / EXCEPTION_NOTICE / PICKUP_SUCCESS / SYSTEM_NOTICE
-- ============================================================
CREATE TABLE IF NOT EXISTS `notification` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT        NOT NULL                COMMENT '接收用户 ID',
  `express_id`  BIGINT                 DEFAULT NULL    COMMENT '关联快递 ID',
  `title`       VARCHAR(200)  NOT NULL                COMMENT '消息标题',
  `content`     VARCHAR(1000) NOT NULL                COMMENT '消息内容',
  `type`        VARCHAR(30)   NOT NULL                COMMENT '消息类型',
  `is_read`     TINYINT       NOT NULL DEFAULT 0       COMMENT '是否已读：0=未读，1=已读',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_time`   DATETIME               DEFAULT NULL    COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_express_id` (`express_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';
