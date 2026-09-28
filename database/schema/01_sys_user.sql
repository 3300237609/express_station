-- ============================================================
-- 01 用户表 sys_user
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
  `password`    VARCHAR(255) NOT NULL                COMMENT '加密后的密码',
  `real_name`   VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
  `phone`       VARCHAR(20)  NOT NULL                COMMENT '手机号',
  `email`       VARCHAR(100)          DEFAULT NULL    COMMENT '邮箱',
  `avatar`      VARCHAR(255)          DEFAULT NULL    COMMENT '头像地址',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '账号状态：1=正常，0=禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_real_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
