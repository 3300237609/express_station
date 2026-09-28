-- ============================================================
-- 数据库初始化总脚本（按依赖顺序执行 schema + data）
-- 用法示例：
--   mysql -uroot -proot --default-character-set=utf8mb4 < database/init.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS `express_station`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `express_station`;

SOURCE database/schema/01_sys_user.sql;
SOURCE database/schema/02_sys_role.sql;
SOURCE database/schema/03_sys_user_role.sql;
SOURCE database/schema/04_express_company.sql;
SOURCE database/schema/05_shelf.sql;
SOURCE database/schema/06_shelf_location.sql;
SOURCE database/schema/07_express.sql;
SOURCE database/schema/08_pickup_record.sql;
SOURCE database/schema/09_express_exception.sql;
SOURCE database/schema/10_notification.sql;
SOURCE database/schema/11_operation_log.sql;
SOURCE database/schema/12_sys_config.sql;

SOURCE database/data/role_data.sql;
SOURCE database/data/company_data.sql;
SOURCE database/data/shelf_data.sql;
SOURCE database/data/test_user_data.sql;
SOURCE database/data/config_data.sql;
