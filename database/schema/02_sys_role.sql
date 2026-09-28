-- ============================================================
-- 02 角色表 sys_role
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name`   VARCHAR(50) NOT NULL                COMMENT '角色名称',
  `role_code`   VARCHAR(50) NOT NULL                COMMENT '角色编码',
  `description` VARCHAR(255)         DEFAULT NULL    COMMENT '角色描述',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
