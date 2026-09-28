<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 门户首页（登录前展示）：系统介绍 + 登录/注册入口
 * 已登录（含预览身份）时主按钮为「去登录」；经主布局 meta.bare 免侧栏/顶栏整页展示
 */
const router = useRouter()
const authStore = useAuthStore()

/** 核心功能 */
const features = [
  {
    icon: 'Box',
    title: '入库出库管理',
    desc: '快递到站一键录入，出库取件全程留痕，包裹状态实时同步'
  },
  {
    icon: 'Iphone',
    title: '取件码核验',
    desc: '入库自动生成取件码，扫码核验快速取件，杜绝错拿漏拿'
  },
  {
    icon: 'Bell',
    title: '超期与异常提醒',
    desc: '超期件自动提醒，异常件标记跟进，问题包裹及时处理'
  },
  {
    icon: 'DataAnalysis',
    title: '数据统计看板',
    desc: '业务量、入库取件趋势、货位使用率一目了然，经营心中有数'
  }
]

/** 业务流程 */
const steps = [
  { no: '01', title: '快递入库', desc: '到站快递扫码或手输登记' },
  { no: '02', title: '上架存储', desc: '分配货架货位，精准存放' },
  { no: '03', title: '通知取件', desc: '取件码通知收件人取件' },
  { no: '04', title: '取件出库', desc: '核验取件码，确认出库' }
]

/** 登录 / 注册 / 首页跳转 */
function go(path) {
  router.push(path)
}
</script>

<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <header class="site-nav">
      <div class="nav-inner">
        <div class="nav-brand">
          <span class="nav-logo"><el-icon :size="20"><Van /></el-icon></span>
          <span class="nav-title">快递末端智能驿站系统</span>
        </div>
        <div class="nav-actions">
          <el-button v-if="authStore.isLogin" type="primary" @click="go('/login')">
            去登录
          </el-button>
          <template v-else>
            <el-button link class="nav-link" @click="go('/login')">登 录</el-button>
            <el-button type="primary" @click="go('/register')">免费注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- Hero 主视觉 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <span class="hero-pill">入库 · 存储 · 取件 · 出库</span>
          <h1>高效收发快递<br />让驿站管理更智能</h1>
          <p class="hero-desc">
            专为驿站管理员、快递员与用户打造的现代化快递管理平台，从入库到出库全过程数字化管理。
          </p>
          <div class="hero-cta">
            <el-button
              v-if="authStore.isLogin"
              type="primary"
              size="large"
              @click="go('/login')"
            >
              去登录
            </el-button>
            <template v-else>
              <el-button type="primary" size="large" @click="go('/login')">立即登录</el-button>
              <el-button size="large" class="cta-plain" @click="go('/register')">
                免费注册
              </el-button>
            </template>
          </div>
        </div>

        <!-- 右侧示意卡片（纯 CSS 装饰，无图片资源） -->
        <div class="hero-visual">
          <div class="visual-glow" />
          <div class="visual-card visual-main">
            <div class="vm-head">
              <span class="vm-logo"><el-icon :size="18"><Van /></el-icon></span>
              <span>驿站首页</span>
            </div>
            <div class="vm-code-label">取件码</div>
            <div class="vm-code">A-0138</div>
            <div class="vm-chips">
              <span class="chip chip-teal">待取件</span>
              <span class="chip">SF 1234567 890</span>
            </div>
            <div class="vm-rows">
              <div class="vm-row"><span>今日入库</span><b>+128</b></div>
              <div class="vm-row"><span>今日取件</span><b>96</b></div>
              <div class="vm-row"><span>货位使用率</span><b>72%</b></div>
            </div>
          </div>
          <div class="visual-card visual-float">
            <span class="vf-icon"><el-icon :size="16"><Bell /></el-icon></span>
            <div class="vf-text">
              <b>超期提醒已发送</b>
              <span>3 位收件人将收到通知</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 核心功能 -->
    <section class="features">
      <div class="section-head">
        <span class="section-pill">核心功能</span>
        <h2>一个驿站，全流程覆盖</h2>
        <p>从快递进站到离开，每一步都有数字化支撑</p>
      </div>
      <div class="feature-grid">
        <div v-for="f in features" :key="f.title" class="feature-card">
          <span class="feature-icon"><el-icon :size="24"><component :is="f.icon" /></el-icon></span>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 业务流程 -->
    <section class="steps">
      <div class="section-head">
        <span class="section-pill">业务流程</span>
        <h2>四步完成一件快递的旅程</h2>
      </div>
      <div class="step-row">
        <div v-for="s in steps" :key="s.no" class="step-item">
          <div class="step-no">{{ s.no }}</div>
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="site-footer">
      <span class="footer-brand"><el-icon :size="16"><Van /></el-icon> 快递末端智能驿站系统</span>
      <span>入库 · 存储 · 取件 · 出库 全过程数字化管理</span>
      <span>© 2026 快递末端智能驿站</span>
    </footer>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  background: #fff;
}

/* ---------- 顶部导航 ---------- */
.site-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e9f1ef;
  backdrop-filter: blur(8px);
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  height: 64px;
  margin: 0 auto;
  padding: 0 24px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #26b6a8;
  color: #fff;
}

.nav-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2733;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  color: #1d9186;
}

/* ---------- Hero ---------- */
.hero {
  padding: 84px 24px 76px;
  background:
    radial-gradient(circle at 16% 10%, rgba(255, 255, 255, 0.95), transparent 45%),
    linear-gradient(160deg, #e8f7f4 0%, #f1fbf9 45%, #e2f4f1 100%);
}

.hero-inner {
  display: flex;
  align-items: center;
  gap: 60px;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-text {
  flex: 1;
}

.hero-pill {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  background: #d9f2ee;
  font-size: 13px;
  font-weight: 600;
  color: #1d9186;
}

.hero-text h1 {
  margin: 22px 0 0;
  font-size: 44px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 1px;
  color: #1f2733;
}

.hero-desc {
  max-width: 520px;
  margin: 20px 0 0;
  font-size: 16px;
  line-height: 1.8;
  color: #6b7280;
}

.hero-cta {
  display: flex;
  gap: 14px;
  margin-top: 34px;
}

.hero-cta :deep(.el-button) {
  min-width: 132px;
}

.cta-plain {
  border-color: #26b6a8;
  color: #1d9186;
}

.cta-plain:hover {
  border-color: #26b6a8;
  background: #e7f8f6;
  color: #1d9186;
}

/* 右侧示意卡片 */
.hero-visual {
  position: relative;
  width: 420px;
  min-height: 380px;
}

.visual-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(38, 182, 168, 0.25), transparent 70%);
  transform: translate(-50%, -50%);
}

.visual-card {
  position: relative;
  background: #fff;
  box-shadow: 0 20px 50px rgba(31, 60, 55, 0.12);
}

.visual-main {
  width: 330px;
  margin: 16px 0 0 34px;
  padding: 24px;
  border-radius: 20px;
}

.vm-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2733;
}

.vm-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: #26b6a8;
  color: #fff;
}

.vm-code-label {
  margin-top: 18px;
  font-size: 13px;
  text-align: center;
  color: #9ca3af;
}

.vm-code {
  margin-top: 4px;
  font-size: 40px;
  font-weight: 800;
  text-align: center;
  letter-spacing: 4px;
  color: #26b6a8;
}

.vm-chips {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
}

.chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: #f1f5f4;
  font-size: 12px;
  color: #6b7280;
}

.chip-teal {
  background: #d9f2ee;
  font-weight: 600;
  color: #1d9186;
}

.vm-rows {
  margin-top: 18px;
  padding-top: 6px;
  border-top: 1px dashed #e5ebe9;
}

.vm-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
  color: #6b7280;
}

.vm-row b {
  font-size: 15px;
  font-weight: 700;
  color: #1f2733;
}

.visual-float {
  position: absolute;
  right: 0;
  bottom: 34px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 14px;
}

.vf-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e7f8f6;
  color: #1d9186;
}

.vf-text b {
  display: block;
  font-size: 13px;
  color: #1f2733;
}

.vf-text span {
  font-size: 12px;
  color: #9ca3af;
}

/* ---------- 区块通用 ---------- */
.section-head {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.section-pill {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 999px;
  background: #d9f2ee;
  font-size: 13px;
  font-weight: 600;
  color: #1d9186;
}

.section-head h2 {
  margin: 16px 0 0;
  font-size: 32px;
  font-weight: 800;
  color: #1f2733;
}

.section-head p {
  margin: 10px 0 0;
  font-size: 15px;
  color: #6b7280;
}

/* ---------- 核心功能 ---------- */
.features {
  padding: 80px 24px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 1200px;
  margin: 44px auto 0;
}

.feature-card {
  padding: 28px 24px;
  border: 1px solid #edf4f2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 6px 24px rgba(31, 60, 55, 0.06);
  transition: transform 0.25s, box-shadow 0.25s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(31, 60, 55, 0.1);
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #e7f8f6;
  color: #1d9186;
}

.feature-card h3 {
  margin: 18px 0 0;
  font-size: 17px;
  font-weight: 700;
  color: #1f2733;
}

.feature-card p {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

/* ---------- 业务流程 ---------- */
.steps {
  padding: 80px 24px;
  background: #f0f8f6;
}

.step-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 1100px;
  margin: 48px auto 0;
}

.step-item {
  position: relative;
  text-align: center;
}

/* 步骤间虚线连接（最后一项不画） */
.step-item:not(:last-child)::after {
  position: absolute;
  top: 22px;
  left: calc(50% + 34px);
  right: calc(-50% + 34px);
  height: 0;
  border-top: 2px dashed #b9e9e4;
  content: '';
}

.step-no {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin: 0 auto;
  border-radius: 50%;
  background: #26b6a8;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.step-item h3 {
  margin: 16px 0 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f2733;
}

.step-item p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

/* ---------- 页脚 ---------- */
.site-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  max-width: 1200px;
  margin: auto auto 0;
  padding: 26px 24px;
  border-top: 1px solid #e9f1ef;
  font-size: 13px;
  color: #9ca3af;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #1f2733;
}

.footer-brand .el-icon {
  color: #26b6a8;
}

/* ---------- 窄屏适配 ---------- */
@media (max-width: 960px) {
  .hero-inner {
    flex-direction: column;
    gap: 30px;
  }

  .hero-visual {
    display: none;
  }

  .hero-text h1 {
    font-size: 32px;
  }

  .feature-grid,
  .step-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .step-item::after {
    display: none;
  }
}

@media (max-width: 560px) {
  .feature-grid,
  .step-row {
    grid-template-columns: 1fr;
  }
}
</style>
