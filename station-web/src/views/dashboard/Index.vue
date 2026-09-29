<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getDashboard, getTrend, getCompanyStats, getCourierDashboard } from '@/api/statistics'
import { listMyExpress } from '@/api/express'
import { getUnreadCount } from '@/api/notification'
import { useEcharts } from '@/composables/useEcharts'

/**
 * 首页：按角色展示——管理员为全站指标卡 + 趋势/业务量图（趋势/公司业务量仅 ADMIN）；
 * 快递员为全站业务指标 + 未读消息 + 个人业绩仪表盘（/stat/courierDashboard 固定本人口径，图表仍不展示）；
 * 普通用户只展示与本人收件相关的数据（我的快递/待取/已取/未读消息 + 待取件卡片列表），不接触统计接口
 */
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const isAdmin = authStore.role === 'ADMIN'
const isCourier = authStore.role === 'COURIER'

/** 快捷入口（按当前角色过滤） */
const quickMenus = computed(() => {
  const all = [
    { title: '快递入库', path: '/express/in', icon: 'FolderAdd', color: '#67c23a', roles: ['ADMIN', 'COURIER'] },
    // 快递列表与详情菜单仅对管理员开放（后端 /express/list 已对快递员放行供异常登记换单号）；我的快递登录即可（快递员=本人经手入库视角，普通用户=收件视角）
    { title: '快递列表', path: '/express/list', icon: 'Box', color: '#26b6a8', roles: ['ADMIN'] },
    { title: '我的快递', path: '/express/my', icon: 'TakeawayBox', color: '#409eff', roles: ['USER', 'COURIER'] },
    { title: '取件码验证', path: '/pickup/verify', icon: 'Key', color: '#e6a23c', roles: ['ADMIN', 'COURIER', 'USER'] },
    { title: '货架货位', path: '/shelf/list', icon: 'Grid', color: '#909399', roles: ['ADMIN'] },
    { title: '货架可视化', path: '/shelf/visual', icon: 'DataBoard', color: '#5b8ff9', roles: ['ADMIN'] },
    { title: '超期快递', path: '/overdue', icon: 'AlarmClock', color: '#f56c6c', roles: ['ADMIN'] },
    { title: '异常件', path: '/exception', icon: 'Warning', color: '#f56c6c', roles: ['ADMIN', 'COURIER'] },
    { title: '数据统计', path: '/statistics', icon: 'DataLine', color: '#0eb06f', roles: ['ADMIN'] },
    { title: '消息中心', path: '/message', icon: 'Bell', color: '#26b6a8', roles: ['ADMIN', 'COURIER', 'USER'] }
  ]
  return all.filter((item) => authStore.hasRole(item.roles))
})

/** 指标数据（管理员运营指标，来自 /stat/dashboard） */
const dashboard = ref({
  totalExpress: 0,
  todayInbound: 0,
  todayPickup: 0,
  waitingPickup: 0,
  exceptionCount: 0,
  overdueCount: 0,
  usageRate: 0
})

/** 个人指标（普通用户本人收件数据；快递员首页仅复用未读消息） */
const mine = ref({
  total: 0,
  waitingPickup: 0,
  pickedUp: 0,
  unread: 0
})

/** 待取件快递列表（普通用户卡片展示，取前 8 条，来自待取统计同一请求） */
const waitingList = ref([])

/** 快递员个人业绩（本人口径：入库/取件/异常登记 × 今日/本月/累计，来自 /stat/courierDashboard） */
const courierPerf = ref({})

/** 个人业绩分组（键名与 CourierDashboardVO 字段一一对齐） */
const perfGroups = [
  { label: '入库', icon: 'FolderAdd', color: '#67c23a', todayKey: 'todayInbound', monthKey: 'monthInbound', totalKey: 'totalInbound' },
  { label: '取件', icon: 'SuccessFilled', color: '#0eb06f', todayKey: 'todayPickup', monthKey: 'monthPickup', totalKey: 'totalPickup' },
  { label: '异常登记', icon: 'WarningFilled', color: '#f56c6c', todayKey: 'todayException', monthKey: 'monthException', totalKey: 'totalException' }
]

/** 趋势数据 */
const trendData = ref([])
const companyData = ref([])

/** 趋势折线图 */
const { chartRef: trendRef, render: renderTrend } = useEcharts(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['入库量', '取件量'] },
  grid: { left: 40, right: 20, top: 40, bottom: 30 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trendData.value.map((item) => item.date)
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    { name: '入库量', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: trendData.value.map((item) => item.inboundCount) },
    { name: '取件量', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: trendData.value.map((item) => item.pickupCount) }
  ]
}))

/** 快递公司业务量柱状图 */
const { chartRef: companyRef, render: renderCompany } = useEcharts(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 50 },
  xAxis: {
    type: 'category',
    data: companyData.value.map((item) => item.companyName),
    axisLabel: { interval: 0, rotate: 30 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '业务量',
      type: 'bar',
      barMaxWidth: 34,
      itemStyle: { color: '#26b6a8', borderRadius: [4, 4, 0, 0] },
      data: companyData.value.map((item) => item.totalCount)
    }
  ]
}))

/** 加载首页数据：管理员取全站统计与图表；快递员取业务指标与个人业绩；普通用户只取本人收件数据 */
async function loadDashboard() {
  loading.value = true
  try {
    if (isAdmin) {
      const [dash, trend, company] = await Promise.all([getDashboard(), getTrend(), getCompanyStats()])
      dashboard.value = { ...dashboard.value, ...(dash.data || {}) }
      trendData.value = trend.data || []
      companyData.value = company.data || []
      renderTrend()
      renderCompany()
    } else if (isCourier) {
      // 快递员：全站业务指标（/stat/dashboard 已放 COURIER）+ 本人业绩 + 未读消息
      const [dash, perf, unread] = await Promise.all([getDashboard(), getCourierDashboard(), getUnreadCount()])
      dashboard.value = { ...dashboard.value, ...(dash.data || {}) }
      courierPerf.value = perf.data || {}
      mine.value.unread = Number(unread.data || 0)
    } else {
      // 普通用户：全部来自登录即可的本人接口，不接触统计接口
      const [myAll, myWait, myPicked, unread] = await Promise.all([
        listMyExpress({ pageNum: 1, pageSize: 1 }),
        // 待取统计与卡片列表合并为一个请求：pageSize 8 兼取前 8 条明细
        listMyExpress({ pageNum: 1, pageSize: 8, status: 'WAITING_PICKUP' }),
        listMyExpress({ pageNum: 1, pageSize: 1, status: 'PICKED_UP' }),
        getUnreadCount()
      ])
      mine.value = {
        total: Number(myAll.map?.total || 0),
        waitingPickup: Number(myWait.map?.total || 0),
        pickedUp: Number(myPicked.map?.total || 0),
        unread: Number(unread.data || 0)
      }
      waitingList.value = myWait.data || []
    }
  } catch (e) {
    // 单接口失败由 request 拦截器统一提示；此处吞掉拒绝，避免炸掉 mounted 钩子（数据保持默认 0）
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 指标卡片（管理员：全站运营指标，含货位使用率等） -->
    <el-row v-if="isAdmin" :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="快递总量" :value="dashboard.totalExpress" icon="Box" color="#26b6a8" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="今日入库" :value="dashboard.todayInbound" icon="FolderAdd" color="#67c23a" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="今日取件" :value="dashboard.todayPickup" icon="SuccessFilled" color="#0eb06f" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="待取快递" :value="dashboard.waitingPickup" icon="Clock" color="#e6a23c" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="异常件" :value="dashboard.exceptionCount" icon="WarningFilled" color="#f56c6c" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="货位使用率" :value="dashboard.usageRate" icon="Grid" color="#909399" />
      </el-col>
    </el-row>

    <!-- 指标卡片（快递员：全站业务指标 + 个人未读消息；快递总量/趋势图仅管理员） -->
    <el-row v-else-if="isCourier" :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="今日入库" :value="dashboard.todayInbound" icon="FolderAdd" color="#67c23a" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="今日取件" :value="dashboard.todayPickup" icon="SuccessFilled" color="#0eb06f" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="待取快递" :value="dashboard.waitingPickup" icon="Clock" color="#e6a23c" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="异常件" :value="dashboard.exceptionCount" icon="WarningFilled" color="#f56c6c" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="货位使用率" :value="dashboard.usageRate" icon="Grid" color="#909399" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <StatCard title="未读消息" :value="mine.unread" icon="Bell" color="#409eff" />
      </el-col>
    </el-row>

    <!-- 指标卡片（普通用户：只看与本人收件相关的数据） -->
    <el-row v-else :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="6">
        <StatCard title="我的快递" :value="mine.total" icon="Box" color="#26b6a8" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <StatCard title="待取快递" :value="mine.waitingPickup" icon="Clock" color="#e6a23c" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <StatCard title="已取快递" :value="mine.pickedUp" icon="SuccessFilled" color="#0eb06f" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <StatCard title="未读消息" :value="mine.unread" icon="Bell" color="#409eff" />
      </el-col>
    </el-row>

    <!-- 图表区（仅管理员） -->
    <el-row v-if="isAdmin" :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="14">
        <div class="card">
          <div class="card-title">近 7 日入库 / 取件趋势</div>
          <div ref="trendRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="card">
          <div class="card-title">快递公司业务量</div>
          <div ref="companyRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <div class="card" style="margin-top: 16px">
      <div class="card-title">快捷入口</div>
      <div class="quick-list">
        <div
          v-for="item in quickMenus"
          :key="item.path"
          class="quick-item"
          @click="router.push(item.path)"
        >
          <el-icon :size="24" :color="item.color"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </div>

    <!-- 个人业绩仪表盘（快递员：本人经手入库/操作出库/异常登记 × 今日/本月/累计） -->
    <div v-if="isCourier" class="card" style="margin-top: 16px">
      <div class="card-title">个人业绩</div>
      <div class="perf-grid">
        <div
          v-for="group in perfGroups"
          :key="group.label"
          class="perf-card"
          :style="{ '--perf-color': group.color }"
        >
          <div class="perf-head">
            <el-icon :size="18" :color="group.color"><component :is="group.icon" /></el-icon>
            <span class="perf-title">{{ group.label }}</span>
          </div>
          <div class="perf-today">
            <span class="perf-num">{{ courierPerf[group.todayKey] ?? 0 }}</span>
            <span class="perf-unit">今日</span>
          </div>
          <div class="perf-sub">
            <span>本月 <b>{{ courierPerf[group.monthKey] ?? 0 }}</b></span>
            <span>累计 <b>{{ courierPerf[group.totalKey] ?? 0 }}</b></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 待取件快递卡片（普通用户：直观看到待取包裹与取件码） -->
    <div v-if="!isAdmin && !isCourier" class="card" style="margin-top: 16px">
      <div class="card-title waiting-head">
        <span>待取件快递</span>
        <el-tag v-if="mine.waitingPickup > 0" type="warning" size="small">{{ mine.waitingPickup }} 件待取</el-tag>
        <a v-if="mine.waitingPickup > 0" class="view-all" @click="router.push('/express/my?status=WAITING_PICKUP')">
          查看全部 →
        </a>
      </div>
      <div v-if="waitingList.length" class="waiting-grid">
        <div v-for="item in waitingList" :key="item.id || item.expressNo" class="waiting-card">
          <div class="wc-header">
            <span class="wc-company">{{ item.companyName || '快递' }}</span>
            <el-tag v-if="item.overdueFlag === 1" type="danger" size="small">已超期</el-tag>
          </div>
          <div class="wc-no">{{ item.expressNo }}</div>
          <div class="wc-code">
            <span class="wc-code-label">取件码</span>
            <span class="wc-code-value">{{ item.pickupCode || '-' }}</span>
          </div>
          <div class="wc-meta">货位：{{ item.shelfNo || '-' }} · {{ item.locationNo || '-' }}</div>
          <div class="wc-meta">入库：{{ item.storageTime || '-' }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无待取件快递，有新包裹入库后会展示在这里" :image-size="72" />
    </div>
  </div>
</template>

<style scoped>
.card-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 110px;
  height: 88px;
  font-size: 13px;
  color: #606266;
  background-color: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  color: #26b6a8;
  background-color: #ecf5ff;
}

.waiting-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.waiting-head .view-all {
  margin-left: auto;
  font-size: 13px;
  font-weight: 400;
  color: #26b6a8;
  cursor: pointer;
}

.waiting-head .view-all:hover {
  text-decoration: underline;
}

.waiting-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.waiting-card {
  padding: 14px 16px;
  background-color: #f7faf9;
  border: 1px solid #e4f3f0;
  border-radius: 10px;
  transition: all 0.2s;
}

.waiting-card:hover {
  border-color: #26b6a8;
  box-shadow: 0 4px 12px rgba(38, 182, 168, 0.15);
  transform: translateY(-2px);
}

.wc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wc-company {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.wc-no {
  margin-top: 4px;
  overflow: hidden;
  font-size: 12px;
  color: #909399;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wc-code {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 10px 0;
  padding: 8px 10px;
  background-color: #fff;
  border-radius: 8px;
}

.wc-code-label {
  font-size: 12px;
  color: #909399;
}

.wc-code-value {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #e6a23c;
}

.wc-meta {
  overflow: hidden;
  font-size: 12px;
  line-height: 1.9;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.perf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.perf-card {
  padding: 14px 16px;
  background-color: #f7faf9;
  border: 1px solid #e4f3f0;
  border-radius: 10px;
  transition: all 0.2s;
}

.perf-card:hover {
  border-color: var(--perf-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.perf-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.perf-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.perf-today {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 10px 0 8px;
}

.perf-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  color: var(--perf-color);
}

.perf-unit {
  font-size: 12px;
  color: #909399;
}

.perf-sub {
  display: flex;
  gap: 18px;
  font-size: 12px;
  color: #6b7280;
}

.perf-sub b {
  color: #303133;
  font-weight: 600;
}

/* 指标卡折行（窄屏 2×2、平板 3+1）时补纵向行距：el-row gutter 只给横向间距，折行会贴在一起 */
.stat-row {
  row-gap: 16px;
}

/* ---------- 移动端（≤768px） ---------- */
@media (max-width: 768px) {
  /* 快捷入口：固定 110px 窄屏排不齐、行尾留洞——改弹性等分，每行自动铺满 */
  .quick-list {
    gap: 10px;
  }

  .quick-item {
    flex: 1 1 76px;
    width: auto;
    height: 78px;
    gap: 6px;
    font-size: 12px;
  }
}

</style>
