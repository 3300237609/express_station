<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getUnreadCount } from '@/api/notification'
import { getPickupRecommendation } from '@/api/pickup'
import { useIsMobile } from '@/composables/useIsMobile'

/**
 * 主布局：左侧菜单 + 顶栏 + 内容区
 * 菜单由路由 meta 生成，并按当前角色过滤（不同角色只见自己的功能）
 * meta.bare 的页面（门户首页）整页独立展示，不渲染侧栏/顶栏
 */
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

/** 顶栏标题取当前路由（一级/二级取最深一层有标题的） */
const currentTitle = computed(() => {
  const titles = route.matched
    .filter((r) => r.meta?.title)
    .map((r) => r.meta.title)
  return titles[titles.length - 1] || '首页'
})

/** 从路由表生成侧边菜单（带 icon 且未 hidden 的一级项） */
const menuList = computed(() => {
  const root = router.options.routes.find((r) => r.path === '/')
  if (!root) return []
  return (root.children || [])
    // 1. 只要配了 icon 的一级菜单，排除 hidden（详情页、个人中心、门户首页）
    .filter((r) => r.meta?.title && r.meta?.icon && !r.meta?.hidden)
    // 2. 按角色过滤（meta.roles 为空视为不限角色）
    .filter((r) => authStore.hasRole(r.meta?.roles))
    // 3. 组装二级菜单
    .map((r) => {
      const routeChildren = r.children || []
      const children = routeChildren
        .filter((c) => c.path && c.meta?.title && !c.meta?.hidden)
        .filter((c) => authStore.hasRole(c.meta?.roles))
        .map((c) => ({ path: `/${r.path}/${c.path}`, title: c.meta.title }))
      return {
        path: `/${r.path}`,
        title: r.meta.title,
        icon: r.meta.icon,
        children,
        // 供第 4 步判断：路由里本有二级（分组型一级），即使角色过滤后为空也不降级成一级项
        isGroup: routeChildren.length > 0
      }
    })
    // 4. 分组型一级菜单在角色过滤后二级为空则不展示（避免渲染出点进去 403 的空菜单）
    .filter((item) => !item.isGroup || item.children.length > 0)
})

/** 移动端（≤768px）判定：侧栏改为抽屉浮层，不占内容区宽度 */
const isMobile = useIsMobile()

/** 移动端抽屉菜单是否展开 */
const mobileMenuOpen = ref(false)

/** 顶栏折叠按钮：移动端开合抽屉菜单，桌面端折叠/展开侧栏 */
function handleCollapse() {
  if (isMobile.value) {
    mobileMenuOpen.value = !mobileMenuOpen.value
  } else {
    appStore.toggleSidebar()
  }
}

// 路由变化：移动端收起抽屉菜单（导航后不残留遮罩）
watch(
  () => route.path,
  () => {
    mobileMenuOpen.value = false
  }
)

// ==================== 智能取件时间推荐（普通用户全局悬浮机器人） ====================
/** 是否展示悬浮机器人：登录后的普通用户（管理员/快递员不展示；未登录门户页不渲染本布局） */
const robotVisible = computed(
  () => authStore.isLogin && authStore.role !== 'ADMIN' && authStore.role !== 'COURIER'
)

/** 推荐弹窗开合（受控 visible：登录成功即自动打开；仅右上角 X 与登出/换身份会关闭） */
const robotOpen = ref(false)

/** 过滤 EP 的关闭请求：只收“打开”，点外部/再点机器人一律不关（永久打开，X 才关） */
function onRobotPopVisible(v) {
  if (v) robotOpen.value = true
}

/** 推荐结果（首次打开机器人时加载，成功后缓存，跨页面保持；失败下次打开自动重试） */
const recommendation = ref(null)
const recLoading = ref(false)
const recLoaded = ref(false)

/** 推荐理由详情折叠（默认收起，避免大段 AI 分析撑高弹窗） */
const reasonExpanded = ref(false)

/** 紧迫度枚举 → 标签（对齐后端 urgencyLevel：URGENT/TIGHT/NORMAL/RELAXED） */
const URGENCY_META = {
  URGENT: { label: '紧急', type: 'danger' },
  TIGHT: { label: '较紧', type: 'warning' },
  NORMAL: { label: '正常', type: 'primary' },
  RELAXED: { label: '宽松', type: 'success' }
}

/** 当前紧迫度标签（未知枚举兜底 info） */
const urgencyMeta = computed(
  () => URGENCY_META[recommendation.value?.urgencyLevel] || { label: recommendation.value?.urgencyLevel || '-', type: 'info' }
)

/** 数据摘要键名 → 中文标签（对齐后端 context 字段；后端新增字段时在此补映射） */
const CTX_LABELS = {
  pendingCount: '待取件数',
  overdueCount: '超期件数',
  hasUrgentExpress: '有紧急件',
  capacityLevel: '货架容量',
  usageRatePercent: '货架使用率'
}

/** 货架容量枚举 → 中文（后端 CapacityLevel：AMPLE/NORMAL/TIGHT/URGENT） */
const CAPACITY_LABELS = {
  AMPLE: '充裕',
  NORMAL: '正常',
  TIGHT: '紧张',
  URGENT: '告急'
}

/** 数据摘要条目：键名翻中文、值转可读文本（后端新键未映射时保留原键兜底） */
const ctxEntries = computed(() =>
  Object.entries(recommendation.value?.context || {}).map(([k, v]) => ({
    label: CTX_LABELS[k] || k,
    value: fmtCtx(k, v)
  }))
)

/** 摘要值 → 中文可读：布尔转是否、容量枚举翻中文、件数补“件”、使用率补 %、对象序列化兜底 */
function fmtCtx(k, v) {
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (k === 'capacityLevel') return CAPACITY_LABELS[v] || v
  if (k === 'pendingCount' || k === 'overdueCount') return `${v} 件`
  if (k === 'usageRatePercent') return `${v}%`
  if (v !== null && typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

/** 打开机器人弹窗时加载推荐（LLM 模式可能稍慢，loading 兜底） */
async function loadRecommendation() {
  if (recLoaded.value || recLoading.value) return
  recLoading.value = true
  try {
    const r = await getPickupRecommendation()
    recommendation.value = r.data || null
    recLoaded.value = true
  } catch (e) {
    // 失败由 request 拦截器统一提示；recLoaded 保持 false，下次打开自动重试
  } finally {
    recLoading.value = false
  }
}

/** 登录成功（或刷新已持登录态）即自动打开推荐弹窗并预取；登出/切身份时关闭并清缓存 */
watch(
  robotVisible,
  (visible) => {
    if (visible) {
      robotOpen.value = true
      loadRecommendation()
    } else {
      robotOpen.value = false
      recommendation.value = null
      recLoaded.value = false
    }
  },
  { immediate: true }
)

// ==================== 悬浮机器人：可拖动 + 右缘停靠约束 ====================
/** 右缘停靠区：距右缘 8 ~ 112 px；松手时出区（如拖到中间）即弹回右缘 */
const DOCK_MIN_R = 8
const DOCK_MAX_R = 112

/** 默认右缘间距（与 CSS 一致：桌面 24、窄屏 16） */
function defaultRightGap() {
  return window.innerWidth <= 768 ? 16 : 24
}

/** 拖动后的位置（视口 left/top 像素）；null = 使用 CSS 默认右下角 */
const robotPos = ref(loadSavedRobotPos())

/** 机器人定位样式（拖动后接管 left/top，覆盖默认的 right/bottom） */
const robotStyle = computed(() =>
  robotPos.value
    ? { left: `${robotPos.value.left}px`, top: `${robotPos.value.top}px`, right: 'auto', bottom: 'auto' }
    : null
)

/** 拖动锁：拖动松手后的 click 不触发弹窗（期间禁掉 EP 的 trigger=click） */
const fabLock = ref(false)

/** 读取本地保存的位置：按当前视口收拢，并校正进右缘停靠区（越界则回默认位） */
function loadSavedRobotPos() {
  try {
    const v = JSON.parse(localStorage.getItem('station_robot_pos') || 'null')
    if (v && Number.isFinite(v.left) && Number.isFinite(v.top)) {
      const size = window.innerWidth <= 768 ? 48 : 54
      const gap = window.innerWidth - v.left - size // 距右缘间距
      const left = gap >= DOCK_MIN_R && gap <= DOCK_MAX_R
        ? v.left
        : window.innerWidth - size - defaultRightGap()
      return {
        left: Math.min(Math.max(left, 0), window.innerWidth - size),
        top: Math.min(Math.max(v.top, 0), window.innerHeight - size)
      }
    }
  } catch (e) {
    // 本地数据损坏时回退默认定位
  }
  return null
}

/** 拖动过程上下文（非响应式，不参与渲染） */
let dragCtx = null

/** 弹回过渡定时器（再次按下时中断，避免旧定时器提前掐掉新过渡） */
let snapTimer = 0

/** 按下：记录起点与指针相对元素的偏移，点击/拖动以 5px 位移为界 */
function onFabPointerDown(e) {
  const isTouch = e.type === 'touchstart'
  clearTimeout(snapTimer)
  e.currentTarget.classList.remove('is-snapping') // 弹回过渡中被再次按下 → 接管定位、改回即时跟随
  const p = isTouch ? e.touches[0] : e
  const rect = e.currentTarget.getBoundingClientRect()
  dragCtx = {
    isTouch,
    el: e.currentTarget,
    offsetX: p.clientX - rect.left,
    offsetY: p.clientY - rect.top,
    startX: p.clientX,
    startY: p.clientY,
    moved: false
  }
  if (isTouch) {
    window.addEventListener('touchmove', onFabPointerMove, { passive: false })
    window.addEventListener('touchend', onFabPointerUp)
    window.addEventListener('touchcancel', onFabPointerUp)
  } else {
    window.addEventListener('mousemove', onFabPointerMove)
    window.addEventListener('mouseup', onFabPointerUp)
  }
}

/** 移动：过阈值即进入拖动，位置实时跟随并夹在视口内 */
function onFabPointerMove(e) {
  if (!dragCtx) return
  const p = dragCtx.isTouch ? e.touches[0] : e
  if (!dragCtx.moved) {
    if (Math.hypot(p.clientX - dragCtx.startX, p.clientY - dragCtx.startY) < 5) return
    dragCtx.moved = true
    fabLock.value = true
    // 首次进入拖动：把默认 right/bottom 换算成 left/top，视觉零跳变
    const r = dragCtx.el.getBoundingClientRect()
    robotPos.value = { left: r.left, top: r.top }
  }
  if (dragCtx.isTouch) e.preventDefault() // 拖动中禁止页面跟随滚动
  const rect = dragCtx.el.getBoundingClientRect()
  robotPos.value = {
    left: Math.min(Math.max(p.clientX - dragCtx.offsetX, 0), window.innerWidth - rect.width),
    top: Math.min(Math.max(p.clientY - dragCtx.offsetY, 0), window.innerHeight - rect.height)
  }
}

/** 松手：落位在右缘停靠区外（如拖到中间）则先弹回右缘，再持久化；锁延迟到紧随的 click 走完再放开 */
function onFabPointerUp() {
  window.removeEventListener('mousemove', onFabPointerMove)
  window.removeEventListener('mouseup', onFabPointerUp)
  window.removeEventListener('touchmove', onFabPointerMove)
  window.removeEventListener('touchend', onFabPointerUp)
  window.removeEventListener('touchcancel', onFabPointerUp)
  if (dragCtx && dragCtx.moved && robotPos.value) {
    const width = dragCtx.el.getBoundingClientRect().width
    const gap = window.innerWidth - robotPos.value.left - width
    if (gap < DOCK_MIN_R || gap > DOCK_MAX_R) {
      snapToDock(dragCtx.el, width) // 出区 → 平滑弹回右缘（纵向高度保留）
    }
    localStorage.setItem('station_robot_pos', JSON.stringify(robotPos.value))
    setTimeout(() => {
      fabLock.value = false
    }, 0)
  }
  dragCtx = null
}

/** 平滑弹回右侧默认停靠位（top 保持不变） */
function snapToDock(el, width) {
  el.classList.add('is-snapping')
  robotPos.value = {
    left: window.innerWidth - width - defaultRightGap(),
    top: robotPos.value.top
  }
  clearTimeout(snapTimer)
  snapTimer = setTimeout(() => el.classList.remove('is-snapping'), 320)
}

/** 加载未读消息数（顶栏红点；门户首页/未登录时不请求，避免触发 401） */
async function loadUnread() {
  if (route.meta.bare || !authStore.isLogin) return
  try {
    const r = await getUnreadCount()
    appStore.setUnreadCount(Number(r.data || 0))
  } catch (e) {
    // 接口未就绪时静默，不影响布局使用
  }
}

/** 头像下拉命令 */
async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'changePassword') {
    // 直达个人中心的「修改密码」标签页
    router.push({ path: '/profile', query: { tab: 'password' } })
  } else if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}

onMounted(loadUnread)
</script>

<template>
  <!-- 门户首页等 bare 页面：整页独立展示，不套侧栏/顶栏 -->
  <router-view v-if="route.meta.bare" />
  <el-container v-else class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside
      :width="appStore.isCollapse ? '64px' : '220px'"
      class="layout-aside"
      :class="{ 'mobile-open': mobileMenuOpen }"
    >
      <div class="logo">
        <el-icon :size="24" color="#26b6a8"><Van /></el-icon>
        <span v-show="!appStore.isCollapse" class="logo-text">智能驿站系统</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.isCollapse"
        :collapse-transition="false"
        router
        class="layout-menu"
      >
        <template v-for="item in menuList" :key="item.path">
          <!-- 有二级菜单 -->
          <el-sub-menu v-if="item.children.length > 0" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <!-- 无二级菜单（同属主菜单，class 用于与子菜单区分字重） -->
          <el-menu-item v-else :index="item.path" class="menu-top">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉遮罩：点击收起菜单 -->
    <div v-if="isMobile && mobileMenuOpen" class="aside-mask" @click="mobileMenuOpen = false" />

    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header" height="56px">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="18" @click="handleCollapse">
            <component :is="isMobile || appStore.isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 未读消息红点 -->
          <el-badge :value="appStore.unreadCount" :max="99" class="msg-badge">
            <el-icon :size="18" class="msg-btn" @click="router.push('/message')"><Bell /></el-icon>
          </el-badge>

          <!-- 用户下拉 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info" @click.stop>
              <el-avatar :size="30" class="user-avatar">
                {{ (authStore.user?.realName || authStore.user?.username || '?').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ authStore.user?.realName || authStore.user?.username }}</span>
              <el-tag size="small" effect="plain" class="role-tag">{{ authStore.roleName }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 智能取件时间推荐（普通用户全局悬浮机器人）：登录后全站页面可见，点击弹出推荐时段/紧迫度/备选 -->
    <el-popover
      v-if="robotVisible"
      placement="left"
      :width="320"
      :visible="robotOpen"
      @update:visible="onRobotPopVisible"
      trigger="click"
      :disabled="fabLock"
      @before-enter="loadRecommendation"
    >
      <template #reference>
        <div
          class="robot-fab"
          title="智能推荐取件时间（可拖动）"
          :style="robotStyle"
          @mousedown="onFabPointerDown"
          @touchstart="onFabPointerDown"
        >
          <!-- 自绘小机器人（图标包无 robot），白线稿配主题色渐变底 -->
          <svg class="robot-svg" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="3.4" r="1.5" fill="#fff" />
            <path d="M12 4.9v2.4" stroke="#fff" stroke-width="1.5" stroke-linecap="round" />
            <rect x="2.6" y="10.5" width="2.2" height="5" rx="1.1" fill="#fff" />
            <rect x="19.2" y="10.5" width="2.2" height="5" rx="1.1" fill="#fff" />
            <rect x="4.8" y="7.3" width="14.4" height="11.4" rx="3.2" fill="#fff" />
            <circle cx="9.4" cy="12.4" r="1.6" fill="#1d9186" />
            <circle cx="14.6" cy="12.4" r="1.6" fill="#1d9186" />
            <path d="M9.7 15.7h4.6" stroke="#1d9186" stroke-width="1.4" stroke-linecap="round" />
          </svg>
        </div>
      </template>

      <div class="robot-wrap">
        <button class="rb-close" type="button" aria-label="关闭推荐弹窗" @click="robotOpen = false">
          <svg viewBox="0 0 14 14" width="12" height="12" aria-hidden="true">
            <path d="M2 2l10 10M12 2L2 12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
        </button>
        <div class="robot-body" v-loading="recLoading" element-loading-text="正在分析驿站忙闲…">
          <template v-if="recommendation">
            <div class="rb-head">
              <span class="rb-title">智能取件时间推荐</span>
              <el-tag size="small" effect="plain" :type="recommendation.source === 'LLM' ? 'primary' : 'info'">
                {{ recommendation.source === 'LLM' ? 'AI 分析' : '规则推荐' }}
              </el-tag>
            </div>

            <div class="rb-urgency">
              <el-tag size="small" :type="urgencyMeta.type">{{ urgencyMeta.label }}</el-tag>
              <span class="rb-hint">{{ recommendation.urgencyHint || '-' }}</span>
            </div>

            <div class="rb-main">
              <div class="rb-label">推荐时段</div>
              <div class="rb-slot">{{ recommendation.recommendedTimeSlot || '-' }}</div>
              <div v-if="recommendation.reason" class="rb-reason-wrap">
                <div class="rb-reason" :class="{ 'is-collapsed': !reasonExpanded }">
                  {{ recommendation.reason }}
                </div>
                <button type="button" class="rb-reason-toggle" @click="reasonExpanded = !reasonExpanded">
                  {{ reasonExpanded ? '收起' : '展开详情' }}
                  <svg viewBox="0 0 12 12" width="10" height="10" aria-hidden="true">
                    <path
                      d="M3 4.5l3 3 3-3"
                      stroke="currentColor"
                      stroke-width="1.6"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      fill="none"
                      :style="{ transform: reasonExpanded ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform 0.2s' }"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <div v-if="ctxEntries.length" class="rb-context">
              <div class="rb-label">数据摘要</div>
              <div class="rb-ctx-list">
                <span v-for="(e, i) in ctxEntries" :key="i" class="rb-ctx">{{ e.label }}：{{ e.value }}</span>
              </div>
            </div>
          </template>
          <div v-else-if="!recLoading" class="rb-empty">推荐暂时不可用，请稍后再试～</div>
        </div>
      </div>
    </el-popover>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

/* ---------- 侧边栏：白底浅色风 ---------- */
.layout-aside {
  background-color: #fff;
  border-right: 1px solid #e9f1ef;
  transition: width 0.2s;
  overflow-x: hidden;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 56px;
  color: #1f2733;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #eef2f1;
  white-space: nowrap;
}

.layout-menu {
  --el-menu-bg-color: #fff;
  --el-menu-text-color: #4b5563;
  --el-menu-active-color: #1d9186;
  --el-menu-hover-color: #1d9186;
  border-right: none;
}

/* 子菜单弱化：浅底区块 + 13px 小字 + 文字变浅（置于 hover/选中态之前，二者仍优先生效） */
.layout-menu :deep(.el-menu--inline) {
  background-color: #f7faf9;
}

.layout-menu :deep(.el-menu--inline .el-menu-item) {
  font-size: 13px;
  /* 右缩进：文字起点推到 60px，明显右于主菜单文字（约 46px 起） */
  padding-left: 60px;
}

.layout-menu :deep(.el-menu--inline .el-menu-item:not(.is-active):not(:hover)) {
  color: #6b7280;
}

/* 悬停：浅薄荷底 + 青绿字 */
.layout-menu :deep(.el-menu-item:hover),
.layout-menu :deep(.el-sub-menu__title:hover) {
  background-color: #eef8f6;
  color: #1d9186;
}

/* 选中：薄荷底 + 青绿字 + 左侧指示条 */
.layout-menu :deep(.el-menu-item.is-active) {
  position: relative;
  background-color: #d7f2ed;
  font-weight: 600;
  color: #1d9186;
}

.layout-menu :deep(.el-menu-item.is-active)::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 4px;
  height: 60%;
  border-radius: 0 4px 4px 0;
  background: #26b6a8;
  transform: translateY(-50%);
  content: '';
}

/* 主菜单强调：700 加粗 + 15px 放大 + 加字距，与子菜单（13px 常规字重）拉开层级；
   置于选中态规则之后，保证选中的一级项同样保持 700，颜色仍由选中/悬停规则控制 */
.layout-menu :deep(.el-sub-menu__title),
.layout-menu :deep(.el-menu-item.menu-top) {
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.5px;
}

/* ---------- 顶栏 ---------- */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e9f1ef;
  box-shadow: 0 2px 10px rgba(31, 60, 55, 0.04);
  padding: 0 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #26b6a8;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.msg-btn {
  cursor: pointer;
  color: #606266;
}

.msg-btn:hover {
  color: #26b6a8;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background-color: #26b6a8;
  color: #fff;
}

.user-name {
  color: #303133;
  font-size: 14px;
}

/* ---------- 内容区：浅薄荷底 ---------- */
.layout-main {
  padding: 0;
  overflow-y: auto;
  background-color: #f0f7f5;
}

/* ---------- 移动端（≤768px）：侧栏抽屉化 + 顶栏精简 ---------- */
@media (max-width: 768px) {
  /* 侧栏脱离文档流变抽屉：默认藏在左侧屏外，展开时滑入（!important 覆盖 el-aside 行内宽度，抽屉固定 220px） */
  .layout-aside {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1001;
    visibility: hidden;
    width: 220px !important;
    height: 100vh;
    box-shadow: 6px 0 24px rgba(31, 60, 55, 0.14);
    transform: translateX(-100%);
    transition: transform 0.25s ease, visibility 0.25s ease;
  }

  .layout-aside.mobile-open {
    visibility: visible;
    transform: translateX(0);
  }

  /* 抽屉遮罩：半透明压住内容，点击收起 */
  .aside-mask {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background-color: rgba(15, 23, 42, 0.45);
  }

  /* 顶栏精简：保留头像，隐藏昵称与角色标签，避免窄屏拥挤 */
  .user-name,
  .role-tag {
    display: none;
  }

  .header-left {
    gap: 10px;
  }

  .header-right {
    gap: 14px;
  }
}

/* ---------- 移动端（≤768px）：悬浮机器人内收 ---------- */
@media (max-width: 768px) {
  .robot-fab {
    right: 16px;
    bottom: 24px;
    width: 48px;
    height: 48px;
  }
}

/* ---------- 智能推荐悬浮机器人（普通用户全局悬浮） ---------- */
.robot-fab {
  position: fixed;
  right: 24px;
  bottom: 48px;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  background: linear-gradient(135deg, #26b6a8, #1d9186);
  border-radius: 50%;
  box-shadow: 0 6px 18px rgba(38, 182, 168, 0.42);
  cursor: pointer;
  /* 拖动支持：触屏不触发页面滚动、不产生文本选择 */
  touch-action: none;
  user-select: none;
  transition: transform 0.2s;
}

.robot-fab.is-snapping {
  /* 拖出右侧停靠区松手后，弹回右缘的横向过渡 */
  transition: left 0.28s ease, transform 0.2s;
}

.robot-fab:hover {
  transform: scale(1.08);
}

/* 呼吸光环：提示可点击 */
.robot-fab::after {
  position: absolute;
  inset: -5px;
  border: 2px solid rgba(38, 182, 168, 0.45);
  border-radius: 50%;
  animation: robot-pulse 2.2s ease-out infinite;
  content: '';
}

@keyframes robot-pulse {
  0% {
    transform: scale(0.92);
    opacity: 1;
  }

  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

.robot-svg {
  width: 30px;
  height: 30px;
}

/* ---------- 推荐弹窗内容 ---------- */
.robot-wrap {
  position: relative;
}

.robot-body {
  min-height: 76px;
}

.rb-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  /*给右上角关闭按钮留位，来源标签不压到 X */
  padding-right: 24px;
}

.rb-close {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: #909399;
  background-color: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.rb-close:hover {
  color: #303133;
  background-color: #f0f1f2;
}

.rb-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.rb-urgency {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.rb-hint {
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
}

.rb-main {
  padding: 10px 12px;
  background-color: #f0f7f5;
  border-radius: 10px;
}

.rb-label {
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.rb-slot {
  font-size: 16px;
  font-weight: 700;
  color: #1d9186;
}

.rb-reason-wrap {
  margin-top: 6px;
}

.rb-reason {
  font-size: 12px;
  line-height: 1.7;
  color: #6b7280;
  white-space: pre-wrap;
}

.rb-reason.is-collapsed {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.rb-reason-toggle {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
  padding: 0;
  font-size: 12px;
  color: #1d9186;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.15s ease;
}

.rb-reason-toggle:hover {
  color: #26b6a8;
}

.rb-context {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}

.rb-ctx-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rb-ctx {
  padding: 2px 8px;
  font-size: 11px;
  color: #6b7280;
  background-color: #f5f7fa;
  border-radius: 999px;
  word-break: break-all;
}

.rb-empty {
  padding: 12px 0;
  font-size: 13px;
  color: #909399;
  text-align: center;
}
</style>
