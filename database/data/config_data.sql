-- ============================================================
-- 初始化数据：系统配置
-- ============================================================
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `description`, `status`) VALUES
  ('station.name',            '快递末端智能驿站', '驿站名称',           1),
  ('station.overdue.hours',   '72',                '超期提醒时长（小时）', 1),
  ('pickup.code.length',      '6',                 '取件码长度',         1),
  ('notification.enabled',    'true',              '是否开启消息通知',   1);
