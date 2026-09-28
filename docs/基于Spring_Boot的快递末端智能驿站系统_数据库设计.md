# 基于 Spring Boot 的快递末端智能驿站系统

# 数据库设计文档

## 1. 数据库设计概述

### 1.1 设计目标

数据库主要用于保存快递末端智能驿站系统中的核心业务数据，为用户管理、快递管理、货位管理、取件管理、异常件管理、消息通知和数据统计等功能提供可靠的数据基础。

数据库设计遵循以下原则：

1. 业务数据结构清晰。
2. 表之间关系明确。
3. 尽量避免数据冗余。
4. 保证核心业务数据的一致性。
5. 为常用查询设计合理索引。
6. 支持分页查询和统计查询。
7. 为后续功能扩展预留空间。

---

# 2. 数据库基本信息

## 2.1 数据库

使用：

```text
MySQL 8.0
```

建议数据库名称：

```text
express_station
```

## 2.2 字符集

建议统一使用：

```sql
utf8mb4
```

排序规则：

```text
utf8mb4_unicode_ci
```

## 2.3 存储引擎

建议统一使用：

```text
InnoDB
```

原因：

- 支持事务。
- 支持行级锁。
- 支持外键约束。
- 适合快递入库、取件、出库等事务性业务。

---

# 3. 数据库总体结构

系统核心数据表包括：

```text
1. sys_user                用户表
2. sys_role                角色表
3. sys_user_role           用户角色关联表
4. express_company         快递公司表
5. express                 快递表
6. shelf                   货架表
7. shelf_location          货位表
8. pickup_record            取件记录表
9. express_exception       异常件表
10. notification           消息通知表
11. operation_log          操作日志表
12. sys_config              系统配置表
```

数据关系概览：

```text
                         ┌──────────────┐
                         │   sys_role   │
                         └──────┬───────┘
                                │
                                │
                       ┌────────▼────────┐
                       │  sys_user_role  │
                       └────────┬────────┘
                                │
                         ┌──────▼───────┐
                         │   sys_user   │
                         └──────┬───────┘
                                │
                                │ 1:N
                                ▼
                         ┌──────────────┐
                         │   express    │
                         └───┬─────┬────┘
                             │     │
                  ┌──────────┘     └───────────┐
                  ▼                            ▼
       ┌─────────────────┐             ┌───────────────┐
       │express_company  │             │shelf_location │
       └─────────────────┘             └───────┬───────┘
                                               │
                                               ▼
                                       ┌──────────────┐
                                       │    shelf     │
                                       └──────────────┘

express
   ├── pickup_record
   ├── express_exception
   └── notification

sys_user
   ├── notification
   └── operation_log
```

---

# 4. 表设计规范

所有业务表建议统一包含：

```text
id
create_time
update_time
```

部分需要逻辑删除的业务表增加：

```text
deleted
```

建议主键统一使用：

```text
BIGINT
```

状态字段建议采用：

```text
TINYINT
```

或业务枚举对应的字符串字段。

对于毕业设计项目，为保证数据库可读性，可以采用字符串状态值，例如：

```text
WAITING_PICKUP
PICKED_UP
EXCEPTION
```

---

# 5. 用户与权限相关表

## 5.1 用户表 `sys_user`

用于保存系统用户、快递员和管理员的基础信息。

| 字段 | 类型 | 是否为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| id | BIGINT | 否 | - | 主键 |
| username | VARCHAR(50) | 否 | - | 登录用户名 |
| password | VARCHAR(255) | 否 | - | 加密后的密码 |
| real_name | VARCHAR(50) | 否 | - | 真实姓名 |
| phone | VARCHAR(20) | 否 | - | 手机号 |
| email | VARCHAR(100) | 是 | NULL | 邮箱 |
| avatar | VARCHAR(255) | 是 | NULL | 头像地址 |
| status | TINYINT | 否 | 1 | 账号状态 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 否 | 0 | 逻辑删除 |

### 状态

```text
1 = 正常
0 = 禁用
```

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_username (username)
UNIQUE KEY uk_phone (phone)
INDEX idx_real_name (real_name)
```

---

## 5.2 角色表 `sys_role`

用于保存系统角色。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| role_name | VARCHAR(50) | 否 | 角色名称 |
| role_code | VARCHAR(50) | 否 | 角色编码 |
| description | VARCHAR(255) | 是 | 角色描述 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |

### 默认角色

```text
1  管理员  ADMIN
2  快递员  COURIER
3  普通用户 USER
```

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_role_code (role_code)
```

---

## 5.3 用户角色关联表 `sys_user_role`

用于实现用户和角色的多对多关系。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| user_id | BIGINT | 否 | 用户 ID |
| role_id | BIGINT | 否 | 角色 ID |
| create_time | DATETIME | 否 | 创建时间 |

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_user_role (user_id, role_id)
INDEX idx_role_id (role_id)
```

---

# 6. 快递公司表

## 6.1 `express_company`

用于保存快递公司的基础资料。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| company_name | VARCHAR(100) | 否 | 公司名称 |
| company_code | VARCHAR(50) | 否 | 公司编码 |
| contact_name | VARCHAR(50) | 是 | 联系人 |
| contact_phone | VARCHAR(20) | 是 | 联系电话 |
| status | TINYINT | 否 | 状态 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |
| deleted | TINYINT | 否 | 逻辑删除 |

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_company_code (company_code)
INDEX idx_company_name (company_name)
```

---

# 7. 快递相关表

## 7.1 快递表 `express`

这是系统最核心的业务表。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| express_no | VARCHAR(100) | 否 | 快递单号 |
| company_id | BIGINT | 否 | 快递公司 ID |
| sender_name | VARCHAR(50) | 是 | 寄件人 |
| sender_phone | VARCHAR(20) | 是 | 寄件人电话 |
| receiver_name | VARCHAR(50) | 否 | 收件人 |
| receiver_phone | VARCHAR(20) | 否 | 收件人电话 |
| receiver_user_id | BIGINT | 是 | 收件用户 ID |
| pickup_code | VARCHAR(20) | 否 | 取件码 |
| location_id | BIGINT | 是 | 货位 ID |
| status | VARCHAR(30) | 否 | 快递状态 |
| size_type | VARCHAR(20) | 是 | 尺寸类型 |
| weight | DECIMAL(10,2) | 是 | 重量 |
| arrival_time | DATETIME | 是 | 到达时间 |
| storage_time | DATETIME | 是 | 入库时间 |
| pickup_time | DATETIME | 是 | 取件时间 |
| overdue_flag | TINYINT | 否 | 是否超期 |
| remark | VARCHAR(500) | 是 | 备注 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |
| deleted | TINYINT | 否 | 逻辑删除 |

### 快递状态

```text
WAITING          待入库
STORED           已入库
WAITING_PICKUP   待取件
PICKED_UP        已取件
EXCEPTION        异常
RETURNED         已退回
```

### 尺寸类型

```text
SMALL
MEDIUM
LARGE
```

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_express_no (express_no)
UNIQUE KEY uk_pickup_code (pickup_code)
INDEX idx_company_id (company_id)
INDEX idx_receiver_user_id (receiver_user_id)
INDEX idx_status (status)
INDEX idx_location_id (location_id)
INDEX idx_arrival_time (arrival_time)
INDEX idx_pickup_time (pickup_time)
INDEX idx_overdue_flag (overdue_flag)
```

### 说明

`express` 表不建议直接删除历史数据，采用逻辑删除方式保留业务记录。

---

# 8. 货架和货位表

## 8.1 货架表 `shelf`

用于保存驿站货架信息。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| shelf_no | VARCHAR(50) | 否 | 货架编号 |
| area | VARCHAR(50) | 否 | 所属区域 |
| capacity | INT | 否 | 总货位数量 |
| used_count | INT | 否 | 已使用货位数量 |
| status | TINYINT | 否 | 货架状态 |
| remark | VARCHAR(255) | 是 | 备注 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |
| deleted | TINYINT | 否 | 逻辑删除 |

### 状态

```text
1 = 正常
0 = 停用
```

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_shelf_no (shelf_no)
INDEX idx_area (area)
INDEX idx_status (status)
```

---

## 8.2 货位表 `shelf_location`

一个货架可以拥有多个具体货位。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| shelf_id | BIGINT | 否 | 所属货架 ID |
| location_no | VARCHAR(50) | 否 | 货位编号 |
| row_no | INT | 否 | 行号 |
| column_no | INT | 否 | 列号 |
| capacity | INT | 否 | 容量 |
| used_count | INT | 否 | 已使用数量 |
| status | TINYINT | 否 | 货位状态 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |

### 状态

```text
1 = 空闲
2 = 部分占用
3 = 已满
4 = 停用
```

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_location_no (location_no)
INDEX idx_shelf_id (shelf_id)
INDEX idx_status (status)
```

---

# 9. 取件记录表

## 9.1 `pickup_record`

用于保存用户完成取件后的历史记录。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| express_id | BIGINT | 否 | 快递 ID |
| user_id | BIGINT | 否 | 用户 ID |
| pickup_code | VARCHAR(20) | 否 | 取件码 |
| operator_id | BIGINT | 是 | 操作人员 ID |
| pickup_method | VARCHAR(30) | 否 | 取件方式 |
| pickup_time | DATETIME | 否 | 取件时间 |
| remark | VARCHAR(255) | 是 | 备注 |
| create_time | DATETIME | 否 | 创建时间 |

### 取件方式

```text
CODE       取件码
SCAN       扫码
ADMIN      管理员代取
COURIER    快递员操作
```

### 索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_express_id (express_id)
INDEX idx_user_id (user_id)
INDEX idx_pickup_time (pickup_time)
INDEX idx_operator_id (operator_id)
```

---

# 10. 异常件表

## 10.1 `express_exception`

用于记录快递异常以及异常处理过程。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| express_id | BIGINT | 否 | 快递 ID |
| exception_type | VARCHAR(30) | 否 | 异常类型 |
| description | VARCHAR(1000) | 否 | 异常描述 |
| image_url | VARCHAR(500) | 是 | 异常图片 |
| reporter_id | BIGINT | 否 | 登记人 |
| handler_id | BIGINT | 是 | 处理人 |
| handle_status | VARCHAR(30) | 否 | 处理状态 |
| handle_remark | VARCHAR(1000) | 是 | 处理结果 |
| create_time | DATETIME | 否 | 登记时间 |
| handle_time | DATETIME | 是 | 处理时间 |
| update_time | DATETIME | 否 | 更新时间 |

### 异常类型

```text
DAMAGE       破损
LOST         丢失
WRONG_ADDR   地址错误
WRONG_INFO   收件人信息错误
REFUSED      拒收
OVERDUE      超期
OTHER        其他
```

### 处理状态

```text
PENDING      待处理
PROCESSING   处理中
RESOLVED     已处理
CLOSED       已关闭
REJECTED     已驳回
```

### 索引

```text
PRIMARY KEY (id)
INDEX idx_express_id (express_id)
INDEX idx_exception_type (exception_type)
INDEX idx_handle_status (handle_status)
INDEX idx_reporter_id (reporter_id)
INDEX idx_handler_id (handler_id)
INDEX idx_create_time (create_time)
```

---

# 11. 消息通知表

## 11.1 `notification`

用于保存系统发送给用户的站内消息。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| user_id | BIGINT | 否 | 接收用户 ID |
| express_id | BIGINT | 是 | 关联快递 ID |
| title | VARCHAR(200) | 否 | 消息标题 |
| content | VARCHAR(1000) | 否 | 消息内容 |
| type | VARCHAR(30) | 否 | 消息类型 |
| is_read | TINYINT | 否 | 是否已读 |
| create_time | DATETIME | 否 | 创建时间 |
| read_time | DATETIME | 是 | 阅读时间 |

### 消息类型

```text
PICKUP_NOTICE      入库/取件通知
OVERDUE_NOTICE     超期提醒
EXCEPTION_NOTICE   异常件通知
PICKUP_SUCCESS     取件成功
SYSTEM_NOTICE      系统通知
```

### 索引

```text
PRIMARY KEY (id)
INDEX idx_user_id (user_id)
INDEX idx_express_id (express_id)
INDEX idx_is_read (is_read)
INDEX idx_create_time (create_time)
```

---

# 12. 操作日志表

## 12.1 `operation_log`

用于记录管理员、快递员等人员的关键操作。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| user_id | BIGINT | 否 | 操作人 ID |
| username | VARCHAR(50) | 否 | 操作人用户名 |
| role_code | VARCHAR(50) | 是 | 操作角色 |
| module | VARCHAR(100) | 否 | 操作模块 |
| operation | VARCHAR(100) | 否 | 操作类型 |
| description | VARCHAR(500) | 是 | 操作描述 |
| request_method | VARCHAR(20) | 是 | HTTP 方法 |
| request_url | VARCHAR(500) | 是 | 请求地址 |
| ip_address | VARCHAR(50) | 是 | IP 地址 |
| result | VARCHAR(20) | 是 | 操作结果 |
| create_time | DATETIME | 否 | 操作时间 |

### 索引

```text
PRIMARY KEY (id)
INDEX idx_user_id (user_id)
INDEX idx_module (module)
INDEX idx_create_time (create_time)
INDEX idx_result (result)
```

---

# 13. 系统配置表

## 13.1 `sys_config`

用于保存系统运行参数。

| 字段 | 类型 | 是否为空 | 说明 |
|---|---|---:|---|
| id | BIGINT | 否 | 主键 |
| config_key | VARCHAR(100) | 否 | 配置键 |
| config_value | VARCHAR(500) | 否 | 配置值 |
| description | VARCHAR(255) | 是 | 配置说明 |
| status | TINYINT | 否 | 状态 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间 |

### 示例配置

```text
station.name
station.overdue.hours
pickup.code.length
notification.enabled
```

---

# 14. 表之间的关系

## 14.1 用户与角色

```text
sys_user
   │
   │ 1:N
   ▼
sys_user_role
   │
   │ N:1
   ▼
sys_role
```

说明：

一个用户可以拥有一个或多个角色，一个角色也可以分配给多个用户。

---

## 14.2 用户与快递

```text
sys_user
   │
   │ 1:N
   ▼
express
```

一个用户可以拥有多个快递。

`express.receiver_user_id` 关联：

```text
sys_user.id
```

---

## 14.3 快递公司与快递

```text
express_company
       │
       │ 1:N
       ▼
    express
```

一个快递公司可以对应多个快递。

---

## 14.4 货架与货位

```text
shelf
 │
 │ 1:N
 ▼
shelf_location
```

一个货架拥有多个货位。

---

## 14.5 货位与快递

```text
shelf_location
      │
      │ 1:N
      ▼
   express
```

在系统设计中，一个具体货位可以根据容量存储一个或多个快递。

若后续调整为“一格一件”，可将 `shelf_location.capacity` 固定为 1。

---

## 14.6 快递与取件记录

```text
express
   │
   │ 1:1
   ▼
pickup_record
```

一个快递正常情况下对应一条最终取件记录。

---

## 14.7 快递与异常件

```text
express
   │
   │ 1:N
   ▼
express_exception
```

一个快递在生命周期内可以产生多条异常处理记录。

---

## 14.8 用户与消息

```text
sys_user
   │
   │ 1:N
   ▼
notification
```

一个用户可以拥有多条系统消息。

---

# 15. 核心 ER 关系

```text
┌─────────────┐
│  sys_user   │
└──────┬──────┘
       │
       │ 1:N
       ▼
┌────────────────┐
│    express     │
└──┬────┬────┬───┘
   │    │    │
   │    │    └─────────────────┐
   │    │                      │
   │    ▼                      ▼
   │  pickup_record     express_exception
   │
   ├───────────────┐
   │               │
   ▼               ▼
express_company  shelf_location
                       │
                       │ N:1
                       ▼
                     shelf

sys_user
   │
   ├──────────────► notification
   │
   ├──────────────► operation_log
   │
   └──────────────► sys_user_role
                           │
                           ▼
                       sys_role
```

---

# 16. 核心状态设计

## 16.1 快递状态流转

```text
WAITING
待入库
  │
  ▼
STORED
已入库
  │
  ▼
WAITING_PICKUP
待取件
  │
  ├───────────────┐
  ▼               ▼
PICKED_UP       EXCEPTION
已取件            异常
  │               │
  ▼               ▼
完成             处理完成

特殊流程：

WAITING_PICKUP
      ↓
RETURNED
已退回
```

---

## 16.2 货位状态流转

```text
EMPTY
空闲
  ↓
OCCUPIED
占用
  ↓
FULL
已满
```

取件后：

```text
OCCUPIED
   ↓
释放
   ↓
EMPTY
```

---

## 16.3 异常处理状态

```text
PENDING
   ↓
PROCESSING
   ↓
RESOLVED
   ↓
CLOSED
```

---

# 17. 核心业务事务

## 17.1 快递入库事务

快递入库时需要处理：

```text
1. 查询/创建快递信息
2. 生成唯一取件码
3. 分配货位
4. 保存快递
5. 更新货位占用数量
6. 更新货架使用数量
7. 创建用户通知
```

这些核心数据库操作建议放在同一个事务中。

```text
BEGIN
  ↓
保存 Express
  ↓
更新 ShelfLocation
  ↓
更新 Shelf
  ↓
创建 Notification
  ↓
COMMIT
```

任一核心步骤失败时：

```text
ROLLBACK
```

---

## 17.2 用户取件事务

用户取件时需要：

```text
1. 验证用户
2. 验证取件码
3. 查询快递
4. 检查快递状态
5. 修改快递状态
6. 更新取件时间
7. 释放货位
8. 更新货架使用量
9. 创建取件记录
10. 创建取件成功通知
```

事务流程：

```text
BEGIN
  ↓
验证 Express
  ↓
更新 Express
  ↓
更新 ShelfLocation
  ↓
更新 Shelf
  ↓
创建 PickupRecord
  ↓
创建 Notification
  ↓
COMMIT
```

---

# 18. 索引设计原则

数据库索引主要针对高频查询字段。

### 用户相关

```text
username
phone
real_name
```

### 快递相关

```text
express_no
pickup_code
receiver_user_id
status
company_id
arrival_time
location_id
```

### 货位相关

```text
shelf_id
location_no
status
```

### 取件相关

```text
user_id
express_id
pickup_time
```

### 异常件相关

```text
express_id
handle_status
create_time
```

### 通知相关

```text
user_id
is_read
create_time
```

---

# 19. 数据完整性设计

## 19.1 唯一性

以下字段需要保证唯一：

```text
sys_user.username
sys_user.phone
sys_role.role_code
express_company.company_code
express.express_no
express.pickup_code
shelf.shelf_no
shelf_location.location_no
```

---

## 19.2 外键关系

逻辑关联关系包括：

```text
express.company_id
        ↓
express_company.id

express.receiver_user_id
        ↓
sys_user.id

express.location_id
        ↓
shelf_location.id

shelf_location.shelf_id
        ↓
shelf.id

pickup_record.express_id
        ↓
express.id

pickup_record.user_id
        ↓
sys_user.id

express_exception.express_id
        ↓
express.id

notification.user_id
        ↓
sys_user.id
```

实际项目中可以根据项目部署和维护需求决定是否使用 MySQL 物理外键；业务层仍需保证关联数据的一致性。

---

# 20. 数据删除策略

系统核心业务数据不建议直接物理删除。

例如：

```text
用户
快递
快递公司
货架
```

使用：

```text
deleted = 1
```

表示逻辑删除。

历史记录：

```text
pickup_record
express_exception
operation_log
```

一般不允许普通管理员直接删除，以保证业务追溯能力。

---

# 21. 数据备份需求

生产环境建议：

```text
每日全量备份
+
定期增量备份
```

重点备份：

```text
express_station
```

数据库。

毕业设计开发环境至少应该保留：

```text
数据库初始化 SQL
测试数据 SQL
数据库结构说明
```

---

# 22. 推荐初始化数据

## 22.1 角色数据

```text
ADMIN
COURIER
USER
```

## 22.2 快递公司数据

```text
顺丰
中通
圆通
申通
韵达
极兔
京东
```

## 22.3 快递状态

```text
WAITING
STORED
WAITING_PICKUP
PICKED_UP
EXCEPTION
RETURNED
```

## 22.4 示例货架

```text
A-01
A-02
A-03

B-01
B-02
B-03
```

---

# 23. 数据库目录建议

项目中可以将数据库脚本统一放置：

```text
database
├── schema
│   ├── 01_sys_user.sql
│   ├── 02_sys_role.sql
│   ├── 03_sys_user_role.sql
│   ├── 04_express_company.sql
│   ├── 05_shelf.sql
│   ├── 06_shelf_location.sql
│   ├── 07_express.sql
│   ├── 08_pickup_record.sql
│   ├── 09_express_exception.sql
│   ├── 10_notification.sql
│   ├── 11_operation_log.sql
│   └── 12_sys_config.sql
│
└── data
    ├── role_data.sql
    ├── company_data.sql
    ├── shelf_data.sql
    └── test_user_data.sql
```

---

# 24. 数据库设计总结

本系统数据库以 `express` 快递表为核心，通过用户、快递公司、货位、取件记录、异常件和消息通知等表形成完整的业务数据体系。

核心关系可以概括为：

```text
用户
 ↓
快递
 ├── 快递公司
 ├── 货位
 ├── 取件记录
 ├── 异常记录
 └── 消息通知
```

货位体系：

```text
货架
 ↓
货位
 ↓
快递
```

权限体系：

```text
用户
 ↓
用户角色
 ↓
角色
```

通过上述设计，可以支持系统的核心业务：

```text
快递入库
    ↓
生成取件码
    ↓
分配货位
    ↓
通知用户
    ↓
用户取件
    ↓
快递出库
    ↓
释放货位
    ↓
生成取件记录
```

同时满足：

```text
异常件管理
超期快递管理
消息通知
数据统计
操作日志
权限控制
```

为后续 MyBatis Mapper、Service 业务层以及 RESTful API 的开发提供完整的数据基础。
