# 快递末端智能驿站系统 · 前端（station-web）

基于《架构设计》第 3.1 / 25.2 节搭建的 **Vue 3 前端框架骨架**，覆盖管理端 / 用户端 / 快递员端三类角色的页面结构、路由权限、API 层与状态管理。

## 技术栈

| 类别 | 选型 |
|---|---|
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 6 |
| 路由 | Vue Router 4（历史模式 + 全局守卫） |
| 状态 | Pinia |
| UI | Element Plus（中文语言包）+ @element-plus/icons-vue |
| 图表 | ECharts 5（`useEcharts` 组合式函数按需挂载） |
| 请求 | Axios（统一拦截器，适配后端 `R<T>` 结构） |

## 目录结构

```
station-web/
├── index.html
├── vite.config.js               # @ 别名 + /api 开发代理（转发到 :80 并去掉前缀）
├── jsconfig.json                # IDE 路径提示
├── .env.development / .env.production
└── src/
    ├── main.js                  # 入口：Pinia → Router → Element Plus → 全局组件/图标注册
    ├── style.css                # 全局样式：青绿主题变量 + page-container / filter-bar / card / pagination-bar
    ├── api/                     # 接口层（按后端「动词进路径」约定，一个业务域一个模块）
    │   ├── request.js           # Axios 实例：R 结构解包、401 弹确认框跳登录（区分未登录/已过期）、错误统一提示
    │   ├── auth.js / user.js / company.js / express.js / shelf.js
    │   ├── pickup.js / exception.js / notification.js / statistics.js / log.js / config.js
    │   └── common.js
    ├── router/index.js          # 路由表 + meta（title/icon/roles/public/hidden/bare）+ 前置守卫
    ├── stores/
    │   ├── auth.js              # 登录态（HttpOnly Cookie，本地仅存用户信息）
    │   └── app.js               # 侧边栏折叠、未读消息数
    ├── constants/dict.js        # 业务字典（状态码→文案/标签色）+ labelOf/tagTypeOf
    ├── composables/useEcharts.js
    ├── utils/
    │   ├── format.js            # 脱敏、超期时长、使用率
    │   └── authTip.js           # 登录提示文案（未登录/登录已过期，401 与路由守卫共用）
    ├── components/
    │   ├── AuthLayout.vue        # 登录/注册左右分栏布局（品牌区 + 表单插槽，form-left 翻转）
    │   ├── DictTag.vue          # <dict-tag type="EXPRESS_STATUS" :code="row.status" />
    │   └── StatCard.vue         # 统计卡片
    ├── layout/MainLayout.vue    # 侧边菜单（按角色动态生成）+ 顶栏（未读红点/用户下拉）
    └── views/
        ├── home/                # 门户首页（登录前展示，meta.bare 免布局整页渲染）
        ├── login/               # 登录、注册
        ├── dashboard/           # 首页（按角色展示：管理员指标+趋势/业务量图、快递员业务、用户本人收件）
        ├── express/             # 快递列表、快递入库（智能货位推荐）、快递详情（流转记录）
        ├── shelf/               # 货架管理、货位管理（使用率/空闲筛选）
        ├── pickup/              # 取件码验证（两步式）、取件记录
        ├── exception/           # 异常件登记与处理
        ├── overdue/             # 超期快递与提醒
        ├── message/             # 消息中心 + 公告发布
        ├── statistics/          # 数据统计（趋势/公司业务量/货位使用率/分布）
        ├── user/                # 用户管理、快递员管理
        ├── company/             # 快递公司管理
        ├── log/                 # 操作日志
        ├── config/              # 系统配置
        ├── profile/             # 个人中心（资料/改密）
        └── error/               # 403 / 404
```

## 角色与菜单

| 角色 | 可见功能 |
|---|---|
| `ADMIN` 管理员 | 全部菜单（首页、快递、取件、货架货位、异常件、超期、消息、统计、系统管理） |
| `COURIER` 快递员 | 首页、快递入库、取件验证/记录、异常件、消息（快递列表/详情仅管理员菜单可见；异常登记内部按单号查列表已放行） |
| `USER` 普通用户 | 首页、快递管理·我的快递、取件验证、消息、个人中心 |

实现方式：路由 `meta.roles` 声明权限 → `MainLayout` 生成菜单时过滤 → `router.beforeEach` 拦截越权访问跳 `/403`。**菜单隐藏 ≠ 权限**，后端接口仍需按角色校验。

## 接口约定

- 统一响应：`{ code, msg, data, map }`，`code=1` 成功；调用方取 `r.data`、`r.map.total`
- 认证：HttpOnly Cookie `authToken`（`withCredentials: true`），401 先清态并弹确认框（按本地登录态区分「未登录，请先登录！」或「登录已过期，请重新登录！」），点「确定」后整页跳登录页（带 `redirect` 回跳原页面），点「取消」留在当前页、下次 401 重新弹框；未登录访问受保护页由守卫弹同样的确认框
- 分页：入参 `pageNum`/`pageSize`（默认 1/10），出参在 `map`（total/pageNum/pageSize/pages）
- URL 动词进路径：`GET /xxx/list`、`POST /xxx/add`、`PUT /xxx/update`、`DELETE /xxx/delete/{id}`

## 快速开始

```bash
# 安装依赖
npm install

# 开发（http://localhost:5173，/api 代理到后端 :80）
npm run dev

# 生产构建（产物 dist/，交由 Nginx 托管并反代 /api）
npm run build
```

后端需先启动（`mvn spring-boot:run`，见根目录 README）。页面在后端业务接口就绪前会提示接口错误，属预期行为。

## 开发习惯（与后端《通用开发规范》对齐）

- 注释、文案全部中文，标点用全角 `，。！`
- 列表页固定三段式：`filter-bar`（查询条件）→ `table-card`（表格）→ `pagination-bar`（分页）
- 弹窗表单固定：`// 1. 表单校验 → // 2. 执行写操作`，写操作成功 `ElMessage.success('xx成功！')`
- 危险操作（删除/禁用/重置密码）必须 `ElMessageBox.confirm` 二次确认
- 状态码一律走 `constants/dict.js` 字典 + `<dict-tag>` 渲染，不在视图中散写字面量
