-- ============================================================
-- 初始化数据：测试用户（密码均为 BCrypt 加密的 123456）
-- ============================================================
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `status`) VALUES
  (1, 'admin',   '$2a$10$axDaOwTNkypM57GCgnBfQ.nqyTR7itBBounclGUtI3HbBzhpcW26W', '系统管理员', '13800000001', 'admin@example.com',   1),
  (2, 'courier', '$2a$10$axDaOwTNkypM57GCgnBfQ.nqyTR7itBBounclGUtI3HbBzhpcW26W', '王快递',     '13800000002', 'courier@example.com', 1),
  (3, 'user01',  '$2a$10$axDaOwTNkypM57GCgnBfQ.nqyTR7itBBounclGUtI3HbBzhpcW26W', '李收件',     '13800000003', 'user01@example.com',  1),
  (4, 'user02',  '$2a$10$axDaOwTNkypM57GCgnBfQ.nqyTR7itBBounclGUtI3HbBzhpcW26W', '赵收件',     '13800000004', 'user02@example.com',  1);

-- 分配角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `sys_user` u JOIN `sys_role` r ON r.role_code = 'ADMIN'   WHERE u.username = 'admin';

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `sys_user` u JOIN `sys_role` r ON r.role_code = 'COURIER' WHERE u.username = 'courier';

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `sys_user` u JOIN `sys_role` r ON r.role_code = 'USER'    WHERE u.username IN ('user01', 'user02');
