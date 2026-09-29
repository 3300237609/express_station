# 基于 Spring Boot 的快递末端智能驿站系统

## 技术栈清单

---

## 1. 后端技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **语言** | Java | 17 | LTS，性能稳定，生态成熟 |
| **构建工具** | Maven | — | 依赖管理与构建 |
| **基础框架** | Spring Boot | 3.5.16 | 提供自动配置、内嵌服务器、依赖管理 |
| **Web 层** | Spring MVC | 6.x（随 Boot 3.5） | RESTful API、拦截器、异常处理 |
| **安全框架** | Spring Security | 6.5.x | 认证授权、RBAC 三表权限、过滤器链 |
| **AI 框架** | Spring AI | 1.1.8（GA） | Function Calling、ChatClient、Tool 自动注册 |
| **持久层** | MyBatis + XML Mapper | 3.0.4 | SQL 可控、XML 映射、无 JPA |
| **分页** | PageHelper | 2.1.0 | MyBatis 物理分页 |
| **连接池** | Druid | 1.2.23 | 连接池、监控、防 SQL 注入 |
| **工具库** | Hutool | 5.8.35 | Java 工具类（日期、字符串、加密等） |
| **JWT** | jjwt | 0.12.6 | 无状态 Token 认证、BCrypt 密码加密 |
| **JSON** | Jackson | 随 Boot | 对象序列化/反序列化 |
| **日志** | SLF4J + Logback | 随 Boot | 日志门面与实现 |
| **Lombok** | 随 Boot | 简化实体类、Service 样板代码 |

### 1.1 Spring AI 配置

- **接入模型**：阿里云百炼 DashScope（OpenAI 兼容协议）
- **端点**：`https://dashscope.aliyuncs.com/compatible-mode`
- **默认模型**：`qwen3.7-flash`（可通过环境变量 `LLM_MODEL` 覆盖）
- **接入方式**：`spring-ai-starter-model-openai`（GA 版，自动发现 `@Tool` 注解）
- **降级策略**：API Key 未配置或 LLM 调用异常时自动降级为**纯规则引擎**，接口始终可用
- **重试保护**：`spring.ai.retry.max-attempts=2`

### 1.2 Spring AI Function Calling 工具集

| 工具方法 | 作用 |
|----------|------|
| `getUserPendingExpress` | 查询当前用户待取快递清单 |
| `getHourlyPickupDistribution` | 按小时聚合的历史取件分布 |
| `getOverdueThreshold` | 系统配置的超期阈值（小时） |
| `getStationCapacityStatus` | 当前货架总容量与使用率 |

LLM 通过 Function Calling 自主编排调用上述工具，综合用户件量、历史忙闲、货架紧张度等数据，推荐最优取件时段。

---

## 2. 前端技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| **语言** | JavaScript / TypeScript | 浏览器端开发语言 |
| **框架** | Vue 3 | 组合式 API、响应式、轻量高效 |
| **构建工具** | Vite | 极速热更新、ESM 原生支持 |
| **UI 组件库** | Element Plus | Vue 3 官方推荐组件库 |
| **状态管理** | Pinia | Vue 3 官方状态管理 |
| **图表** | ECharts | 数据可视化、仪表盘、趋势图 |
| **HTTP 客户端** | Axios / Fetch | 与后端 API 通信 |
| **样式** | CSS / SCSS | 组件样式管理 |

---

## 3. 数据层技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **数据库** | MySQL | 8.0 | 关系型数据存储，12 张业务表 |
| **连接池** | Druid | 1.2.23 | 高性能连接池 + 监控统计 |
| **ORM** | MyBatis + XML | 3.0.4 | SQL 手写 + XML 映射，支持动态 SQL |

### 3.1 数据库表清单（12 张）

| 分类 | 表名 | 说明 |
|------|------|------|
| **用户** | sys_user / sys_role / sys_user_role | RBAC 三表权限模型 |
| **基础** | express_company | 快递公司字典 |
| **货架** | shelf / shelf_location | 货架与货位（动态状态：空闲/部分占用/已满/停用） |
| **核心业务** | express | 快递主表（状态机：WAITING → STORED → WAITING_PICKUP → PICKED_UP / EXCEPTION / RETURNED） |
| **异常** | express_exception | 异常件登记与处理 |
| **通知** | notification | 站内消息通知 |
| **审计** | operation_log | 操作日志（AOP 自动记录） |
| **配置** | sys_config | 系统动态配置（超期阈值、通知开关等） |

### 3.2 缓存说明

Redis 暂未引入。后续可用于：
- 热门货架查询缓存
- API 限流
- 分布式锁（多驿站部署时入库互斥）

---

## 4. 开发与运维工具

| 工具 | 说明 |
|------|------|
| **IDEA / VS Code** | 开发 IDE |
| **Git** | 版本控制 |
| **Postman / curl** | 接口调试 |
| **Navicat / DBeaver** | 数据库管理 |
| **Maven settings.xml** | 阿里云镜像加速（用户级 `~/.m2/settings.xml`） |

---

## 5. 技术选型依据

### 5.1 后端选型理由

1. **Spring Boot 3.5 + JDK 17**：LTS 版本，自动配置生态完善，社区活跃
2. **MyBatis 而非 JPA**：项目 SQL 可控，需要复杂联表查询和动态条件，XML 映射更直观
3. **Druid 连接池**：自带监控页，便于排查连接泄漏和慢 SQL
4. **PageHelper**：轻量物理分页，对现有 SQL 零侵入
5. **Hutool**：避免重复造轮子，覆盖日期、加密、IO 等高频工具场景
6. **jjwt 0.12.6**：Spring Boot 3.x 兼容，签名与解析 API 稳定
7. **Spring AI 1.1.8（GA）**：Function Calling 自动注册 `@Tool`，与 Spring Boot 3.5.x 兼容性好
8. **百炼 DashScope 而非 OpenAI**：国内访问稳定、价格亲民、中文场景表现好，同时保持 OpenAI 协议兼容性，便于切换其他模型

### 5.2 前端选型理由

1. **Vue 3 + Element Plus**：学习曲线平缓，企业级后台管理系统主流组合
2. **Vite**：冷启动快，HMR 秒级更新，开发体验好
3. **Pinia**：Vue 3 官方推荐，比 Vuex 更简洁
4. **ECharts**：图表类型丰富，文档齐全，适合数据统计模块

### 5.3 技术约束

- **不引入 Redis**：按项目约定，单机部署场景下缓存通过 MySQL 索引 + PageHelper 解决
- **不引入 JPA / MyBatis-Plus**：保持 MyBatis + XML 手写 SQL 的可控性
- **不引入微服务框架**：单体应用足够支撑当前业务规模，避免分布式复杂度
- **不部署上云**：本地开发 + 数据库直连，通过环境变量区分配置
