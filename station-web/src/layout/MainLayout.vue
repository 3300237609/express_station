<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getUnreadCount } from '@/api/notification'
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

/** 底部菜单高亮：当前路由是菜单项自身或其子路由 */
function isNavActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

/** 内容区容器（路由切换时回顶） */
const mainRef = ref(null)

// 路由变化：移动端收起抽屉 + 内容区回到顶部（避免底部菜单切页时继承上一页滚动位置）
watch(
  () => route.path,
  () => {
    mobileMenuOpen.value = false
    const el = mainRef.value?.$el || mainRef.value
    if (el) el.scrollTop = 0
  }
)

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
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">
                {{ (authStore.user?.realName || authStore.user?.username || '?').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ authStore.user?.realName || authStore.user?.username }}</span>
              <el-tag size="small" effect="plain" class="role-tag">{{ authStore.roleName }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main ref="mainRef" class="layout-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 移动端底部菜单（≤768px）：主菜单 tab 直达，组菜单由路由 redirect 落到默认子页 -->
    <nav v-if="isMobile && menuList.length" class="bottom-nav">
      <div
        v-for="item in menuList"
        :key="item.path"
        class="bottom-nav-item"
        :class="{ active: isNavActive(item.path) }"
        @click="router.push(item.path)"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span class="bottom-nav-label">{{ item.title }}</span>
      </div>
    </nav>
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

/* ---------- 移动端底部菜单（≤768px）：主导航 tab 化，顶栏汉堡保留为完整菜单入口 ---------- */
@media (max-width: 768px) {
  .bottom-nav {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    /* 低于抽屉遮罩（1000）：抽屉展开时底部菜单被遮罩盖住 */
    z-index: 999;
    display: flex;
    overflow-x: auto;
    background-color: #fff;
    border-top: 1px solid #e9f1ef;
    box-shadow: 0 -4px 16px rgba(31, 60, 55, 0.08);
    scrollbar-width: none;
    padding-bottom: env(safe-area-inset-bottom, 0px);
    -webkit-overflow-scrolling: touch;
  }

  /* 隐藏横向滚动条（菜单多时可滑动查看） */
  .bottom-nav::-webkit-scrollbar {
    display: none;
  }

  .bottom-nav-item {
    display: flex;
    /* 少量菜单等分铺满整行，超出（管理端 9 项）则可横向滑动 */
    flex: 1 0 auto;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    min-width: 64px;
    padding: 8px 6px;
    font-size: 11px;
    white-space: nowrap;
    color: #6b7280;
    cursor: pointer;
    transition: color 0.15s;
  }

  .bottom-nav-item.active {
    font-weight: 600;
    color: #1d9186;
  }

  /* 内容区底部留出菜单高度，避免最后内容被遮住 */
  .layout-main {
    padding-bottom: 56px;
    padding-bottom: calc(56px + env(safe-area-inset-bottom, 0px));
  }
}
</style>
