# 基于 Spring Boot 的快递末端智能驿站系统

# 架构设计文档

## 1. 文档概述

### 1.1 项目名称

**基于 Spring Boot 的快递末端智能驿站系统**

### 1.2 设计目的

本架构设计文档用于根据系统需求确定整体技术架构、应用分层、业务模块、数据存储、缓存、认证授权、部署方式以及关键业务模块之间的协作关系，为后续数据库设计、接口设计、编码实现和测试提供统一的技术基础。

### 1.3 设计原则

系统架构设计遵循以下原则：

1. **分层设计**：前后端分离，后端采用清晰的业务分层。
2. **模块化设计**：按照业务领域拆分模块，降低模块之间的耦合。
3. **安全优先**：统一身份认证与权限控制，保护用户和业务数据。
4. **易于维护**：代码结构清晰，职责单一，便于后续扩展。
5. **可扩展性**：为消息通知、智能推荐、移动端等后续能力预留扩展空间。
6. **稳定可靠**：保证快递入库、取件、出库等核心业务的数据一致性。
7. **适度设计**：以单体应用为主，不为了架构复杂度而引入不必要的微服务。

---

# 2. 总体架构

## 2.1 架构模式

系统采用：

> **前后端分离 + Spring Boot 单体应用 + 模块化分层架构**

前端使用 Vue 3，后端使用 Spring Boot，通过 RESTful API 进行数据交互。

整体架构如下：

```text
┌──────────────────────────────────────────────┐
│                  用户访问层                  │
├──────────────────────────────────────────────┤
│  Vue 3 管理端  │  Vue 3 用户端 │ 快递员端   │
└──────────────────────┬───────────────────────┘
                       │ HTTP/HTTPS + JSON
                       ▼
┌──────────────────────────────────────────────┐
│                Spring Boot 应用层            │
├──────────────────────────────────────────────┤
│ Controller / REST API                        │
│                  ↓                           │
│ Security / JWT / Permission                  │
│                  ↓                           │
│ Service 业务层                               │
│                  ↓                           │
│ Domain / Business Logic                      │
│                  ↓                           │
│ Mapper + MyBatis                          │
└───────────────┬──────────────────┬───────────┘
                │                  │
                ▼                  ▼
        ┌──────────────┐    ┌──────────────┐
        │   MySQL      │    │    Redis     │
        │ 业务数据存储 │    │ 缓存/验证码等│
        └──────────────┘    └──────────────┘

                ┌───────────────────────┐
                │ 文件存储/对象存储      │
                │ MinIO / 本地文件       │
                └───────────────────────┘
```

---

# 3. 技术架构

## 3.1 前端技术

| 技术 | 用途 |
|---|---|
| Vue 3 | 前端框架 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 请求 |
| Element Plus | UI 组件 |
| ECharts | 数据可视化 |
| Vite | 前端构建 |

### 前端职责

- 页面展示
- 表单处理
- 用户交互
- 路由控制
- 权限菜单展示
- API 调用
- 数据图表展示
- Token 管理

---

## 3.2 后端技术

| 技术 | 用途 |
|---|---|
| Spring Boot | 后端应用基础框架 |
| RESTful API | 前后端数据交互方式 |
| Spring MVC | REST API 请求处理 |
| MyBatis | 持久层框架 |
| Mapper | 数据访问接口 |
| MySQL 8.0 | 核心业务数据存储 |
| Spring Security | 身份认证与权限控制 |
| JWT | Token 身份认证 |
| Redis | 缓存及临时数据 |
| Lombok | 简化 Java 代码 |
| Maven | 项目构建与依赖管理 |

### 后端技术路线

系统采用 **RESTful API** 作为前后端数据交互方式，后端采用 **Controller + Service + Mapper** 分层架构，使用 **MyBatis** 作为持久层框架，使用 **MySQL** 作为数据库。

```text
RESTful API
    ↓
Controller
    ↓
Service
    ↓
Mapper
    ↓
MyBatis
    ↓
MySQL
```

- **Controller**：负责接收 HTTP 请求并返回 RESTful API 响应。
- **Service**：负责业务逻辑处理。
- **Mapper**：负责定义数据库访问接口。
- **MyBatis**：负责 Mapper 与 SQL 之间的映射和执行。
- **MySQL**：负责持久化系统核心业务数据。

---

# 3.3 数据存储

### MySQL

用于保存核心业务数据：

- 用户
- 角色
- 快递
- 快递公司
- 货架
- 货位
- 取件记录
- 异常件
- 通知
- 操作日志
- 系统配置

### Redis

用于保存高频或临时数据：

- 登录 Token/会话辅助信息
- 验证码
- 取件码短期校验数据
- 热门查询缓存
- Dashboard 统计缓存
- 分布式锁场景

### MinIO / 文件系统

用于保存：

- 用户头像
- 异常件图片
- 处理凭证
- 其他业务附件

---

# 4. 后端分层架构

后端采用典型的多层架构。

```text
┌─────────────────────┐
│    Controller 层    │
│  接收请求/返回响应  │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│     Service 层      │
│      业务逻辑       │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│     Domain/DTO/VO   │
│ 业务对象/请求响应模型│
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│   Mapper 层         │
│   数据访问          │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│      MySQL          │
└─────────────────────┘
```

---

## 4.1 Controller 层

负责：

- 接收 HTTP 请求
- 参数校验
- 调用 Service
- 返回统一响应结果

Controller 不直接编写复杂业务逻辑。

示例：

```text
ExpressController
├── 分页查询快递
├── 查询快递详情
├── 新增快递
├── 修改快递
├── 删除快递
├── 快递入库
└── 快递出库
```

---

## 4.2 Service 层

负责核心业务逻辑。

例如快递入库业务：

```text
ExpressService
    ↓
验证快递信息
    ↓
查询收件用户
    ↓
生成取件码
    ↓
调用货位推荐
    ↓
保存快递
    ↓
更新货位
    ↓
发送通知
```

Service 层是系统业务逻辑的核心。

---

## 4.3 Mapper 层

负责数据库访问。

主要完成：

- 数据查询
- 新增
- 更新
- 删除
- 分页查询
- 条件查询
- 聚合统计

---

## 4.4 DTO

DTO 用于接收前端请求数据。

例如：

```text
ExpressCreateDTO
ExpressStorageDTO
PickupVerifyDTO
UserLoginDTO
ShelfCreateDTO
ExceptionCreateDTO
```

---

## 4.5 VO

VO 用于向前端返回展示数据。

例如：

```text
ExpressVO
PickupRecordVO
DashboardVO
ShelfStatisticsVO
UserInfoVO
```

---

# 5. 业务模块架构

系统按照业务领域划分为以下核心模块：

```text
快递末端智能驿站系统
│
├── 认证与权限模块
├── 用户管理模块
├── 快递员管理模块
├── 快递公司模块
├── 快递管理模块
├── 入库管理模块
├── 货架/货位模块
├── 取件管理模块
├── 出库管理模块
├── 异常件模块
├── 超期快递模块
├── 消息通知模块
├── 数据统计模块
├── 操作日志模块
└── 系统配置模块
```

---

# 6. 核心模块设计

## 6.1 认证与权限模块

负责系统统一登录和访问控制。

### 主要职责

- 用户登录
- 用户退出
- Token 签发
- Token 校验
- 密码加密
- 角色识别
- 权限校验

### 认证流程

```text
用户输入账号密码
       ↓
AuthController
       ↓
认证 Service
       ↓
Spring Security
       ↓
查询用户
       ↓
校验密码
       ↓
生成 JWT
       ↓
返回 Token
```

---

# 7. 用户管理模块

负责维护系统用户资料和角色关系。

### 核心能力

- 用户注册
- 用户登录
- 用户资料管理
- 用户状态管理
- 用户角色管理
- 用户快递查询
- 用户取件记录查询

---

# 8. 快递管理模块

快递模块是整个系统的核心领域模块。

### 核心对象

```text
Express
ExpressCompany
PickupCode
Shelf
PickupRecord
ExpressException
```

### 主要职责

- 快递信息维护
- 快递状态管理
- 快递入库
- 快递查询
- 快递出库
- 快递生命周期管理

---

# 9. 智能货位模块

智能货位模块负责快递与货位之间的匹配。

### 核心流程

```text
获取快递属性
       ↓
查询所有可用货位
       ↓
过滤容量不足货位
       ↓
计算匹配评分
       ↓
排序
       ↓
返回推荐货位
```

### 推荐因素

```text
容量匹配
+
区域匹配
+
货位空闲程度
+
快递属性匹配
```

### 架构关系

```text
ExpressService
       ↓
ShelfRecommendService
       ↓
ShelfMapper
       ↓
MySQL
```

推荐模块与快递模块解耦，未来可以替换推荐算法。

---

# 10. 取件模块

负责用户从查询快递到完成领取的全过程。

### 核心流程

```text
输入取件码
    ↓
PickupController
    ↓
PickupService
    ↓
验证取件码
    ↓
查询快递
    ↓
校验快递状态
    ↓
用户确认
    ↓
更新快递状态
    ↓
释放货位
    ↓
生成取件记录
```

---

# 11. 出库模块

出库不是单纯修改一个状态，而是一个事务性业务过程。

### 出库需要同时完成：

1. 修改快递状态。
2. 更新取件时间。
3. 释放货位。
4. 写入取件记录。
5. 更新统计数据。
6. 发送取件成功通知。

因此建议使用数据库事务保证核心操作的一致性。

```text
开始事务
   ↓
验证快递状态
   ↓
更新 Express
   ↓
释放 Shelf
   ↓
创建 PickupRecord
   ↓
提交事务
```

任何核心步骤失败，都应回滚关键数据库操作。

---

# 12. 异常件模块

用于管理异常快递。

### 架构流程

```text
快递员/用户
      ↓
提交异常
      ↓
ExceptionController
      ↓
ExceptionService
      ↓
保存异常记录
      ↓
通知管理员
      ↓
管理员处理
      ↓
更新异常状态
      ↓
通知用户
```

---

# 13. 超期管理模块

超期管理建议通过定时任务实现。

### 工作流程

```text
定时任务
   ↓
查询待取快递
   ↓
判断入库时间
   ↓
判断是否超期
   ↓
更新超期状态
   ↓
生成提醒
   ↓
通知用户/管理员
```

### 技术实现

第一阶段可以使用 Spring Scheduler：

```text
@Scheduled
```

后续数据量增大后可以考虑：

```text
Quartz
XXL-JOB
消息队列
```

---

# 14. 消息通知模块

通知模块用于统一处理系统消息。

### 通知来源

```text
快递入库
超期提醒
异常件
取件成功
系统公告
```

### 架构方式

建议将消息通知抽象为独立 Service：

```text
NotificationService
├── sendPickupNotice()
├── sendOverdueNotice()
├── sendExceptionNotice()
├── sendSystemNotice()
└── markAsRead()
```

第一阶段可以使用站内消息。

后续可以扩展：

```text
站内消息
微信
短信
邮件
```

---

# 15. 数据统计模块

统计模块负责 Dashboard 和报表数据。

### 统计内容

- 快递总量
- 今日入库
- 今日取件
- 待取快递
- 异常件
- 超期件
- 货位使用率
- 快递公司业务量
- 入库趋势
- 取件趋势

### 数据流

```text
Vue Dashboard
       ↓
StatisticsController
       ↓
StatisticsService
       ↓
统计 SQL / Redis
       ↓
DashboardVO
       ↓
ECharts
```

---

# 16. Redis 架构设计

Redis 主要作为缓存和临时数据存储，不作为核心业务数据的唯一存储。

## 16.1 缓存场景

### 用户信息缓存

```text
user:info:{userId}
```

### Dashboard 缓存

```text
dashboard:statistics:{date}
```

### 取件码辅助缓存

```text
pickup:code:{code}
```

### 验证码

```text
verify:code:{phone}
```

---

## 16.2 防止重复取件

用户取件属于高并发下需要重点保护的业务。

可以使用 Redis 分布式锁：

```text
pickup:lock:{expressId}
```

流程：

```text
用户请求取件
      ↓
获取快递锁
      ↓
校验快递状态
      ↓
完成取件
      ↓
释放锁
```

这样可以降低同一个快递被重复领取的风险。

---

# 17. 安全架构

## 17.1 身份认证

采用：

```text
Spring Security + JWT
```

登录成功后：

```text
用户
 ↓
账号密码认证
 ↓
生成 JWT
 ↓
前端保存 Token
 ↓
请求携带 Authorization
 ↓
后端验证 Token
```

---

## 17.2 权限控制

采用 RBAC 思路：

```text
用户
 ↓
角色
 ↓
权限
 ↓
接口/菜单
```

角色：

```text
ADMIN
COURIER
USER
```

---

## 17.3 数据安全

系统需要：

- 密码加密存储
- 参数合法性校验
- 权限验证
- 防止越权查询
- 重要操作记录日志
- 敏感信息脱敏展示
- 防止重复提交

例如普通用户查询快递时，后端需要限制：

```text
receiver_user_id = 当前登录用户 ID
```

不能仅依赖前端传递的用户 ID。

---

# 18. 统一响应设计

所有 REST API 建议使用统一返回结构。

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

错误响应：

```json
{
  "code": 400,
  "message": "取件码无效",
  "data": null
}
```

这样便于前端统一处理。

---

# 19. 全局异常处理

系统使用统一异常处理机制。

建议设计：

```text
BusinessException
ValidationException
AuthenticationException
AuthorizationException
SystemException
```

通过全局异常处理器统一转换：

```text
@RestControllerAdvice
```

例如：

```text
业务异常
   ↓
Service 抛出异常
   ↓
GlobalExceptionHandler
   ↓
统一 JSON 响应
```

---

# 20. 日志架构

日志分为两类：

## 20.1 系统运行日志

用于开发和排查问题：

```text
INFO
WARN
ERROR
DEBUG
```

## 20.2 业务操作日志

用于审计：

```text
谁
在什么时候
执行了什么操作
操作了什么数据
操作是否成功
```

例如：

```text
管理员
2026-09-22 14:20:10
执行：处理异常件
快递单号：SF123456
结果：成功
```

---

# 21. 数据一致性设计

核心业务需要重点保证以下一致性：

```text
快递状态
     ↕
货位状态
     ↕
取件记录
```

例如用户取件成功后：

```text
Express.status = PICKED_UP
Shelf.status = EMPTY
PickupRecord = 新增
```

三个操作应尽可能处于同一个数据库事务中。

---

# 22. 并发控制

重点场景：

## 22.1 重复取件

同一个快递同时被两个请求领取。

解决方案：

```text
数据库事务
+
状态条件更新
+
Redis 分布式锁
```

---

## 22.2 同一货位重复分配

两个快递同时被推荐到同一货位。

解决方案：

```text
查询空闲货位
      ↓
锁定货位
      ↓
创建快递与货位关系
      ↓
提交事务
```

---

# 23. 部署架构

第一阶段建议使用单服务器部署，降低部署复杂度。

```text
                    Internet
                       │
                       ▼
                ┌─────────────┐
                │ Nginx       │
                │ 前端静态资源│
                └──────┬──────┘
                       │
          ┌────────────┴────────────┐
          ↓                         ↓
   Vue 前端                     API 请求
                                    ↓
                            ┌──────────────┐
                            │ Spring Boot  │
                            └──────┬───────┘
                                   │
                       ┌───────────┼───────────┐
                       ↓           ↓           ↓
                    MySQL       Redis        MinIO
```

---

# 24. Docker 部署方案

推荐后期使用 Docker 统一部署。

```text
docker-compose
│
├── nginx
├── frontend
├── backend
├── mysql
├── redis
└── minio
```

部署结构：

```text
┌──────────────────────────┐
│ Docker Host              │
│                          │
│  nginx                   │
│  frontend                │
│  backend                 │
│  mysql                   │
│  redis                   │
│  minio                   │
└──────────────────────────┘
```

---

# 25. 前后端项目结构

## 25.1 后端

```text
station-server
│
├── src/main/java
│   └── com.example.station
│       ├── controller
│       ├── service
│       │   └── impl
│       ├── mapper
│       ├── entity
│       ├── dto
│       ├── vo
│       ├── config
│       ├── security
│       ├── exception
│       ├── common
│       └── utils
│
└── src/main/resources
    ├── mapper
    ├── application.yml
    └── application-dev.yml
```

## 25.2 前端

```text
station-web
│
├── src
│   ├── api
│   ├── assets
│   ├── components
│   ├── layout
│   ├── router
│   ├── stores
│   ├── views
│   │   ├── login
│   │   ├── dashboard
│   │   ├── express
│   │   ├── shelf
│   │   ├── pickup
│   │   ├── exception
│   │   ├── statistics
│   │   └── user
│   └── utils
│
└── package.json
```

---

# 26. 模块依赖关系

推荐保持如下依赖方向：

```text
RESTful API
    ↓
Controller
    ↓
Service
    ↓
Mapper
    ↓
MyBatis
    ↓
MySQL
```

业务模块之间：

```text
认证模块
    ↓
用户/权限模块
    ↓
快递模块
 ┌──┼─────────────┐
 ↓  ↓             ↓
货位  取件        异常
 │    │             │
 └────┼─────────────┘
      ↓
   通知模块
      ↓
   统计模块
```

避免出现：

```text
Controller → Mapper
Controller → Database
Entity → Service
```

等不合理依赖。

---

# 27. 核心领域关系

```text
                 ┌─────────────┐
                 │    用户     │
                 └──────┬──────┘
                        │
                        │ 1:N
                        ↓
                 ┌─────────────┐
                 │    快递     │
                 └──────┬──────┘
             ┌──────────┼───────────┐
             │          │           │
             │          │           │
             ↓          ↓           ↓
       ┌─────────┐ ┌─────────┐ ┌────────────┐
       │ 快递公司 │ │ 货位    │ │ 异常件     │
       └─────────┘ └─────────┘ └────────────┘
                          │
                          ↓
                    ┌────────────┐
                    │ 取件记录   │
                    └────────────┘
```

---

# 28. 核心业务架构——快递入库

```text
快递员端
   ↓
ExpressController
   ↓
ExpressService
   ├── 用户匹配
   ├── 快递信息校验
   ├── PickupCodeService
   ├── ShelfRecommendService
   ├── ExpressMapper
   ├── ShelfMapper
   └── NotificationService
```

最终完成：

```text
创建快递
+
生成取件码
+
绑定货位
+
修改状态
+
发送通知
```

---

# 29. 核心业务架构——用户取件

```text
用户端
   ↓
PickupController
   ↓
PickupService
   ├── 验证用户身份
   ├── 验证取件码
   ├── 查询快递
   ├── 获取 Redis 锁
   ├── 开启数据库事务
   ├── 更新快递
   ├── 更新货位
   ├── 创建取件记录
   └── 发送通知
```

---

# 30. 核心业务架构——超期检测

```text
Scheduler
   ↓
OverdueService
   ↓
查询待取快递
   ↓
判断超期
   ↓
更新超期状态
   ↓
NotificationService
   ↓
用户/管理员收到提醒
```

---

# 31. 架构选型说明

## 31.1 为什么使用 Spring Boot

Spring Boot 适合快速构建 Java Web 后端应用，生态成熟，能够与 Spring Security、MyBatis、Redis 等技术方便集成。

## 31.2 为什么采用单体架构

本系统属于中小型业务系统，核心业务相对集中。

采用模块化单体架构可以：

- 降低开发难度
- 降低部署成本
- 简化事务处理
- 便于毕业设计实现和演示
- 方便后续逐步拆分

当前没有必要为了微服务而引入复杂的服务治理体系。

## 31.3 为什么使用 Redis

Redis 适用于：

- 高频查询缓存
- 临时数据
- 登录相关数据
- 分布式锁
- 统计数据缓存

可以减少数据库压力。

## 31.4 为什么使用 JWT

JWT 适合前后端分离应用，可以让前端通过 Token 完成身份认证，并便于后端进行接口权限校验。

---

# 32. 架构扩展方向

系统后续可以在现有架构上扩展：

## 32.1 微信/移动端

新增：

```text
微信小程序
App
移动 H5
```

直接复用后端 REST API。

## 32.2 物流平台对接

未来可以通过第三方物流接口获取物流信息：

```text
第三方物流平台
        ↓
物流接口适配层
        ↓
ExpressService
```

## 32.3 消息队列

当通知量和业务量增加时，可以加入：

```text
RabbitMQ
Kafka
RocketMQ
```

将通知、统计等非核心实时业务异步化。

## 32.4 微服务演进

后续可以将：

```text
用户服务
快递服务
通知服务
统计服务
```

逐步拆分为独立服务。

但第一阶段仍建议保持模块化单体结构。

---

# 33. 架构演进路线

```text
阶段一
单体 Spring Boot
+
MySQL
+
Redis
+
Vue

        ↓

阶段二
单体模块化
+
Docker
+
MinIO
+
定时任务

        ↓

阶段三
引入消息队列
+
异步通知
+
统计缓存

        ↓

阶段四
根据实际业务量
逐步微服务化
```

---

# 34. 最终架构总结

系统采用：

> **Vue 3 + Spring Boot + MySQL + Redis + Spring Security + JWT**

构建前后端分离的模块化单体系统。

整体结构如下：

```text
┌──────────────────────────────────────┐
│              Vue 3 前端              │
│ 管理端 / 用户端 / 快递员端            │
└─────────────────┬────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────┐
│            Spring Boot 后端           │
│                                      │
│ 认证权限                             │
│ 用户管理                             │
│ 快递管理                             │
│ 入库管理                             │
│ 货位管理                             │
│ 取件管理                             │
│ 出库管理                             │
│ 异常件管理                           │
│ 超期管理                             │
│ 消息通知                             │
│ 数据统计                             │
│ 操作日志                             │
└──────────────┬───────────┬───────────┘
               │           │
               ▼           ▼
          ┌────────┐  ┌────────┐
          │ MySQL  │  │ Redis  │
          └────────┘  └────────┘
               │
               ▼
          ┌────────┐
          │ MinIO  │
          └────────┘
```

通过以上架构，系统可以在满足当前快递驿站核心业务的同时，为后续的智能推荐、消息异步处理、移动端接入以及微服务演进预留空间。
