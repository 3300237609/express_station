import { createRouter, createWebHistory } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { TIP_NOT_LOGIN } from '@/utils/authTip'

/**
 * 路由配置
 * 1. / 为主布局（MainLayout），业务页面全部挂在其 children 下
 * 2. meta.public：免登录页（登录/注册/403/404）
 * 3. meta.roles：角色权限（父级携带则整组生效），守卫与菜单共同依赖
 * 4. meta.hidden：不出现在侧边栏菜单（详情页、个人中心等）
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/Register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    children: [
      // 门户首页（登录前展示）：meta.bare 让主布局不渲染侧栏/顶栏，meta.public 免登录放行
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页', public: true, hidden: true, bare: true }
      },
      // 首页
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '首页', icon: 'Odometer' }
      },
      // 快递管理（列表/详情菜单仅 ADMIN——后端 /express/list 已对快递员放行供异常登记换单号、详情/流转仍仅 ADMIN；
      // 入库 /inbound/add 与货位推荐对快递员开放，故角色下沉到子级；「我的快递」/express/my 登录即可，全员可见）
      {
        path: 'express',
        meta: { title: '快递管理', icon: 'Box' },
        children: [
          {
            path: '',
            // 默认落地按角色：管理员进快递列表，快递员/普通用户进我的快递（避免点父菜单落 403）
            redirect: () => ({ path: useAuthStore().hasRole(['ADMIN']) ? '/express/list' : '/express/my' })
          },
          {
            path: 'list',
            name: 'ExpressList',
            component: () => import('@/views/express/ExpressList.vue'),
            meta: { title: '快递列表', roles: ['ADMIN'] }
          },
          {
            path: 'in',
            name: 'ExpressIn',
            component: () => import('@/views/express/ExpressIn.vue'),
            meta: { title: '快递入库', roles: ['ADMIN', 'COURIER'] }
          },
          {
            path: 'my',
            name: 'MyExpress',
            component: () => import('@/views/express/MyExpress.vue'),
            meta: { title: '我的快递' }
          },
          {
            path: 'detail/:id',
            name: 'ExpressDetail',
            component: () => import('@/views/express/ExpressDetail.vue'),
            meta: { title: '快递详情', hidden: true, roles: ['ADMIN'] }
          }
        ]
      },
      // 取件管理
      {
        path: 'pickup',
        meta: { title: '取件管理', icon: 'Key' },
        children: [
          { path: '', redirect: '/pickup/verify' },
          {
            path: 'verify',
            name: 'PickupVerify',
            component: () => import('@/views/pickup/PickupVerify.vue'),
            meta: { title: '取件码验证' }
          },
          {
            path: 'record',
            name: 'PickupRecord',
            component: () => import('@/views/pickup/PickupRecordList.vue'),
            meta: { title: '取件记录', roles: ['ADMIN', 'COURIER'] }
          }
        ]
      },
      // 货架货位（仅管理员）
      {
        path: 'shelf',
        meta: { title: '货架货位', icon: 'Grid', roles: ['ADMIN'] },
        children: [
          { path: '', redirect: '/shelf/list' },
          {
            path: 'list',
            name: 'ShelfList',
            component: () => import('@/views/shelf/ShelfList.vue'),
            meta: { title: '货架管理', roles: ['ADMIN'] }
          },
          {
            path: 'location',
            name: 'LocationList',
            component: () => import('@/views/shelf/LocationList.vue'),
            meta: { title: '货位管理', roles: ['ADMIN'] }
          },
          {
            path: 'visual',
            name: 'ShelfVisualization',
            component: () => import('@/views/shelf/ShelfVisualization.vue'),
            meta: { title: '货架可视化', roles: ['ADMIN'] }
          }
        ]
      },
      // 异常件
      {
        path: 'exception',
        name: 'ExceptionList',
        component: () => import('@/views/exception/ExceptionList.vue'),
        meta: { title: '异常件管理', icon: 'Warning', roles: ['ADMIN', 'COURIER'] }
      },
      // 超期快递（仅管理员）
      {
        path: 'overdue',
        name: 'OverdueList',
        component: () => import('@/views/overdue/OverdueList.vue'),
        meta: { title: '超期快递', icon: 'AlarmClock', roles: ['ADMIN'] }
      },
      // 消息中心
      {
        path: 'message',
        name: 'MessageCenter',
        component: () => import('@/views/message/MessageCenter.vue'),
        meta: { title: '消息中心', icon: 'Bell' }
      },
      // 数据统计（仅管理员）
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/Index.vue'),
        meta: { title: '数据统计', icon: 'DataLine', roles: ['ADMIN'] }
      },
      // 系统管理（仅管理员）
      {
        path: 'system',
        meta: { title: '系统管理', icon: 'Setting', roles: ['ADMIN'] },
        children: [
          { path: '', redirect: '/system/user' },
          {
            path: 'user',
            name: 'UserList',
            component: () => import('@/views/user/UserList.vue'),
            meta: { title: '用户管理', roles: ['ADMIN'] }
          },
          {
            path: 'courier',
            name: 'CourierList',
            component: () => import('@/views/user/CourierList.vue'),
            meta: { title: '快递员管理', roles: ['ADMIN'] }
          },
          {
            path: 'company',
            name: 'CompanyList',
            component: () => import('@/views/company/CompanyList.vue'),
            meta: { title: '快递公司管理', roles: ['ADMIN'] }
          },
          {
            path: 'log',
            name: 'OperationLog',
            component: () => import('@/views/log/OperationLog.vue'),
            meta: { title: '操作日志', roles: ['ADMIN'] }
          },
          {
            path: 'config',
            name: 'SystemConfig',
            component: () => import('@/views/config/SystemConfig.vue'),
            meta: { title: '系统配置', roles: ['ADMIN'] }
          }
        ]
      },
      // 个人中心（隐藏菜单，从右上角头像进入）
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心', hidden: true }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { title: '无访问权限', public: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫
 * 顺序：设置标题 → 免登录放行 → 未登录转登录 → 角色不符转 403
 */
router.beforeEach((to, from) => {
  // 1. 页面标题
  document.title = to.meta.title
    ? `${to.meta.title} · 快递末端智能驿站系统`
    : '快递末端智能驿站系统'

  // 2. 免登录页直接放行
  if (to.meta.public) return true

  // 3. 登录态校验（authStore 从 localStorage 恢复，刷新不丢失）
  const authStore = useAuthStore()
  if (!authStore.isLogin) {
    // 未登录访问受保护页：弹确认框，点确定再进登录页
    return ElMessageBox.confirm(TIP_NOT_LOGIN, '登录提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
      .then(() => ({ path: '/login', query: { redirect: to.fullPath } }))
      .catch(() =>
        // 取消：SPA 内有来源页则留在原地；冷加载无来源页时落登录页兜底，避免白屏
        from.matched.length > 0 ? false : { path: '/login' }
      )
  }

  // 4. 角色权限：取 matched 中第一个声明了 roles 的层级（父级覆盖整组）
  const requiredRoles = to.matched
    .map((record) => record.meta?.roles)
    .find((roles) => roles && roles.length > 0)
  if (requiredRoles && !authStore.hasRole(requiredRoles)) {
    return '/403'
  }

  return true
})

export default router
