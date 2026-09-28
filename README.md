# 快递末端智能驿站系统 · 后端（station-server）

基于 **Spring Boot 3.5.16 + JDK 17** 的快递驿站后端，需求与设计见 [`docs/`](docs/)，业务接口已全部实现并通过 7 组端到端测试（432 项断言）。

> 前端 Vue 3 框架已搭建于 [`station-web/`](station-web/)（Vue 3 + Vite + Pinia + Element Plus + ECharts），启动方式见 [`station-web/README.md`](station-web/README.md)。

## 技术栈

| 类别 | 选型 |
|---|---|
| 基础框架 | Spring Boot 3.5.16（`jakarta.*`，JDK 17） |
| 构建 | Maven |
| 认证授权 | **Spring Security + JWT**（HttpOnly Cookie `authToken` + `Authorization: Bearer` 兼容） |
| 持久层 | MyBatis + XML Mapper（不用 JPA / MyBatis-Plus） |
| 数据库 | MySQL 8.0 + Druid 连接池 |
| 分页 | PageHelper |
| 工具库 | Lombok、Hutool |
| 统一响应 | `R<T>`：`{ code, msg, data, map }`，`code=1` 成功 / `code=0` 失败 |

> 说明：认证方案两份文档有冲突，已确认采用《架构设计》的 **Spring Security + JWT**（替代《通用开发规范》中的拦截器方案）。Redis 按约定**暂未引入**，实现缓存/分布式锁时再加 `spring-boot-starter-data-redis`。

## 目录结构

```
express_station（后端根目录）
├── docs/                                # 需求 / 架构 / 数据库 / 开发规范文档
├── database/                            # 数据库脚本：schema/ 12 张表 + data/ 初始数据 + init.sql 总脚本
├── pom.xml
└── src/main
    ├── java/com/example/station/
    │   ├── ExpressStationApplication.java   # 启动类
    │   ├── controller/                      # 控制层：参数接收 + 一行转发，不写业务
    │   ├── service/                         # 业务层接口
    │   │   └── Impl/                        # 业务层实现
    │   ├── mapper/                          # MyBatis Mapper 接口（@Mapper）
    │   ├── entity/                          # 数据库表实体
    │   ├── dto/                             # 请求入参对象
    │   ├── vo/                              # 出参视图对象
    │   ├── config/                          # Security / 跨域
    │   ├── security/                        # JWT 签发校验 + 认证过滤器
    │   ├── exception/                       # 全局异常处理器
    │   ├── common/                          # 统一响应 R
    │   ├── utils/                           # 静态工具类（SecurityUtils 当前登录人门面）
    │   └── constant/                        # 常量类
    └── resources/
        ├── application.yml                  # 唯一配置入口
        ├── mybatis/
        │   ├── mybatis-config.xml           # 全局 settings（驼峰映射、SQL 打印）
        │   └── mapper/                      # XML，与 Mapper 接口同名对应
        └── static/
```

## 快速开始

### 1. 环境要求

- JDK 17（**注意**：若 `mvn -version` 显示 1.8，需先把 `JAVA_HOME` 指向 JDK 17）
- Maven 3.6+
- MySQL 8.0，初始化数据库（在**项目根目录**执行，包含 12 张表 + 角色/测试账号等初始数据）：

```bash
mysql -uroot -proot --default-character-set=utf8mb4 < database/init.sql
```

初始测试账号（密码均为 `123456`）：`admin`（管理员）、`courier`（快递员）、`user01` / `user02`（普通用户）。

### 2. 配置项（`src/main/resources/application.yml`）

| 配置 | 环境变量 | 本地默认值 | 说明 |
|---|---|---|---|
| 数据库密码 | `MYSQL_PASSWORD` | `root` | 生产必须注入，不提交明文 |
| JWT 密钥 | `JWT_SECRET` | 开发默认值 | 长度 ≥ 32 字节，生产必须注入 |
| 数据库地址/账号 | — | `localhost:3306/express_station` / `root` | 按本地环境修改 |

### 3. 启动

```bash
mvn spring-boot:run
```

验证：

```bash
curl http://localhost/health
# {"code":1,"msg":"操作成功！","data":"服务运行正常！","map":{}}

curl http://localhost/任意业务路径
# HTTP 401 {"code":0,"msg":"未登录或登录已过期，请重新登录！","data":null,"map":{}}

curl -X POST http://localhost/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# code=1 返回 token + userInfo，同时写入 HttpOnly Cookie authToken（后续请求自动携带即为已登录）
```

编译验证：`mvn -q -DskipTests compile`

## 已实现能力

- 统一响应 `R<T>`（`success` / `error` / `add` 链式扩展分页字段）
- 全局异常处理 `GlobalExceptionHandler`（参数校验、缺参、类型不匹配、兜底）
- 登录/注册/退出：`POST /auth/login`、`/auth/register`、`/auth/logout`；BCrypt 密码加密存储；签发 JWT → 写入 HttpOnly Cookie `authToken`（path=/，7 天），兼容 `Authorization: Bearer`；注册默认分配 `USER` 角色（RBAC 三表 `sys_user` / `sys_role` / `sys_user_role` 已建好）；`GET /auth/me` 取当前登录人信息（与登录出参同结构，登录即可）、`PUT /auth/changePassword` 本人改密（原密码 BCrypt 校验 + 新旧一致性，Token 不作废由前端引导重登）
- 快递公司管理（管理员）：`GET /company/list`（关键字/状态筛选 + PageHelper 分页）、`/company/detail/{id}`、`/company/volume`（业务量统计），`POST /company/add`，`PUT /company/update`、`/company/updateStatus/{id}`，`DELETE /company/delete/{id}`（名下有快递记录禁止删除，提示改用停用）；`GET /company/enabled` 登录即可（入库/寄件选择公司用）；权限在 `SecurityConfig` 按 `hasRole("ADMIN")` 细分
- 货架与货位管理（管理员）：货架 `/shelf` 列表（关键字/状态筛选 + 分页）、详情（含货位使用率 `usageRate`）、新增、修改、删除（占用守卫，空货位级联删除）、`updateStatus` 启停；货位 `/shelfLocation` 列表（按货架/状态筛选）、详情、`/free` 空闲查询、新增、修改（所属货架不可迁移、容量≥已用）、删除（占用禁删）、`updateStatus` 状态修正；增删货位在同一事务内同步 `shelf.capacity` 总货位计数
- 快递信息管理（列表 = 管理员 + 快递员——供快递员异常登记换单号，其余仅管理员）：`GET /express/list`（单号/取件码/收件人/电话/公司/状态/到达时间范围筛选 + 分页）、`/express/detail/{id}`（含公司名与货位编号）、`/express/flow/{id}`（流转记录 = 自身时间轴 + 取件记录 + 异常记录合并，按时间正序），`POST /express/add`（自动生成 6 位唯一取件码，初始状态 `WAITING`），`PUT /express/update`（不改取件码/状态/货位/业务时间戳），`PUT /express/updateStatus/{id}`（六状态枚举校验 + 仍关联货位时禁止置为 `PICKED_UP`/`RETURNED`），`PUT /express/updateLocation`（调整货位，仅管理员：已出库/未上架/同货位拒绝，目标货位存在未停用 + 货架正常 + 容量预检 → `increaseUsed` 首个写 → `decreaseUsed` 释放原位 → 货位与状态双守卫更新关联 → `refreshUsed` 自愈 → 货位调整通知），`DELETE /express/delete/{id}`（占用货位 / 有取件记录 / 有异常登记三重禁删守卫）；`GET /express/my` 登录即可、分视角——**快递员 = 本人经手入库的快递**（「完成入库」SUCCESS 动作经 `description` 反查归属，与收件视角共用列/筛选/分页），管理员/普通用户 = 本人收件 OR 本人手机号口径；SecurityConfig 显式 `authenticated()` 声明；`GET /user/express/{id}`、`/user/pickupRecord/{id}`、`/courier/deliveryList|storageList|exceptionList/{id}` 五个记录视图（管理员，分页 + total）
- 用户与快递员管理（管理员）：`/user` 列表/详情/新增/修改/`updateStatus`/`resetPassword`/删除（禁删自己）、`GET /user/matchByPhone` 手机号精确匹配用户（管理员 + 快递员，入库选择收件人联动，11 位格式校验前置、未匹配 `data=null`），`/courier` 列表/新增/修改/删除；用户名、手机号唯一键查重按规范不过滤逻辑删除
- 快递入库（管理员 + 快递员）：`POST /inbound/add`（传 `expressId` 绑定既有快递 / 不传则创建并自动生成取件码），全部校验前置 → `increaseUsed` 首个写（行级守卫）→ 快递写入 → `refreshUsed` 自愈 → 入库通知，单号查重先于货位资源校验报出
- 货位智能推荐（管理员 + 快递员）：`GET /shelfLocation/recommend`（适配 40 + 空闲 40 + 区域 20 评分，按评分降序 → 货架/行/列升序，全量返回不分页）
- 取件出库（登录即可）：`POST /pickup/verify`（凭码只读校验）、`POST /pickup/confirm`（事务：`updatePickup` 状态守卫首个写 → `decreaseUsed` 释放货位 → `refreshUsed` → 取件记录 → 通知；`USER` 角色强制凭码方式，未关联收件用户的快递以当前登录人兜底落记录）；`GET /pickup/recordList` 取件记录（管理员 + 快递员，单号/取件码/用户关键字 + 取件时间范围分页）
- 异常件管理：`POST /exception/add`（`expressId` 与 `expressNo` **二选一**，按单号登记免去换查快递列表的权限依赖；登记置快递 `EXCEPTION`，普通用户须为收件归属人）、`PUT /exception/handle`（仅管理员；非终态免填处理结果，终态必填并按「是否仍关联货位」恢复 `WAITING_PICKUP`/`WAITING`）、`GET /exception/list|detail`（管理员 + 快递员，类型/处理状态/关键字/单号多条件分页）
- 超期管理（仅管理员）：`GET /overdue/list`（超期时长按 `station.overdue.hours` 实时计算）、`POST /overdue/refresh`（候选：待取 + 未标记 + 入库早于阈值，行级守卫每件只标一次）、`POST /overdue/remind/{id}`（收件人通知 + 全量管理员广播，未超期/未关联收件用户拒绝）；`OverdueTask` 每小时 `0 0 * * * *` 自动检测（try/catch 防中断）
- 消息通知：用户侧 `GET /notification/list|unreadCount`、`PUT /notification/read/{id}|readAll`（严格按 `user_id` 归属），管理员 `POST /notification/publish`（全量活跃用户广播，分片 500）、`GET /notification/manage`；`notification.enabled=false` 时业务外发（入库/取件/异常/超期）静默跳过
- 数据统计：`GET /stat/dashboard`（管理员 + 快递员——快递员首页取业务指标；今日入库/取件、待取件、超期数、未处理异常数、货位使用率、各公司业务量）、`GET /stat/trend`（仅管理员，缺省近 7 天，区间倒置/超 366 天中文报错，缺失日期补 0）、`GET /stat/courierDashboard` 个人业绩（登录即可、固定当前登录人口径：入库 = 本人完成入库 `SUCCESS` 且快递仍存在、按快递当前 `storage_time` 分档——与全站今日入库同键，结构上保证 个人 ≤ 全站，杜绝夹具回拨/删单造成的个人 > 全站 / `pickup_record.operator_id` 经手出库 / `express_exception.reporter_id` 登记 × 今日/本月/累计三档，接口无入参杜绝越权查他人业绩）
- 操作日志：写接口统一 `@OperationLog` + `OperationLogAspect` 落 `operation_log`（SUCCESS/FAIL、`#{...}` SpEL 描述、匿名注册经注解 `operator` 反查新用户落日志，写失败仅告警不中断）；`GET /log/list`（模块/类型/结果/关键字分页，仅管理员）
- 系统配置（仅管理员）：`GET /config/list`、`POST /config/add`、`PUT /config/update`、`DELETE /config/delete/{id}`；业务按 `configKey` 读取（`station.overdue.hours`、`notification.enabled` 等，缺失按默认值）
- Spring Security 无状态过滤器链：CSRF 关闭、Session 关闭、匿名认证关闭、白名单（`/health`、`/auth/login`、`/auth/register`、`/auth/logout`、`OPTIONS/**`）、401/403 返回统一 JSON；权限按方法 + 路径细分至 ADMIN / ADMIN+COURIER / 登录即可（`GET /pickup/recordList`、`GET /express/list`、`GET /stat/dashboard`、`GET /user/matchByPhone` = ADMIN+COURIER，`PUT /express/updateLocation` = ADMIN）
- `JwtUtils` 签发/校验 + `JwtAuthenticationFilter` 解析 Token（角色列表 → `ROLE_xxx` 权限）写入 `SecurityContextHolder`，业务代码经 `SecurityUtils.getCurrentUserId()` 读取当前登录人（框架请求结束自动清理，**无自建 ThreadLocal**）
- 跨域配置（`CorsConfigurationSource`，被 Security 复用）
- MyBatis / Druid / PageHelper 配置就绪，`druid.initial-size=0`（本地无 MySQL 也能启动）

## 尚未引入（按约定可选）

- Redis 缓存与分布式锁：按约定暂未引入，需要时加 `spring-boot-starter-data-redis` 即可（业务接口已全部完成并验证）

## 编码约定（详见 `docs/通用开发规范.md`）

- Controller 四字段 Javadoc（功能描述 / 请求方式 / 请求路径 / 参数 / 返回值），方法体只写一行转发
- Service 方法内固定步骤：`// 1. 空校验 → 2. 存在性 → 3. 业务规则 → 4. 执行`，错误提前 `return R.error("中文！")`，不抛业务异常
- SQL 关键字全大写、禁 `SELECT *`、分页统一 `LIMIT #{offset}, #{pageSize}`、`resultType` 用全类名
- 错误文案中文 + 全角 `！`；注释全部中文
