<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard, getTrend, getCompanyStats } from '@/api/statistics'
import { useEcharts } from '@/composables/useEcharts'

/**
 * 数据统计（管理员）：核心指标 + 入库/取件趋势 + 公司业务量 + 货位使用率
 */
const loading = ref(false)

const dashboard = ref({
  totalExpress: 0,
  todayInbound: 0,
  todayPickup: 0,
  waitingPickup: 0,
  exceptionCount: 0,
  overdueCount: 0,
  usageRate: 0
})

const trendData = ref([])
const companyData = ref([])

/** 入库/取件趋势折线图 */
const { chartRef: trendRef, render: renderTrend } = useEcharts(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['入库量', '取件量'] },
  grid: { left: 40, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', boundaryGap: false, data: trendData.value.map((i) => i.date) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    { name: '入库量', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: trendData.value.map((i) => i.inboundCount) },
    { name: '取件量', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: trendData.value.map((i) => i.pickupCount) }
  ]
}))

/** 快递公司业务量柱状图 */
const { chartRef: barRef, render: renderBar } = useEcharts(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 60 },
  xAxis: {
    type: 'category',
    data: companyData.value.map((i) => i.companyName),
    axisLabel: { interval: 0, rotate: 30 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '业务量',
      type: 'bar',
      barMaxWidth: 40,
      itemStyle: { color: '#26b6a8', borderRadius: [4, 4, 0, 0] },
      data: companyData.value.map((i) => i.totalCount)
    }
  ]
}))

/** 货位使用率仪表盘 */
const { chartRef: gaugeRef, render: renderGauge } = useEcharts(() => ({
  series: [
    {
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      progress: { show: true, width: 16, itemStyle: { color: '#26b6a8' } },
      axisLine: { lineStyle: { width: 16, color: [[1, '#ebeef5']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      title: { show: true, offsetCenter: [0, '30%'], fontSize: 14, color: '#909399' },
      detail: {
        valueAnimation: true,
        fontSize: 30,
        fontWeight: 600,
        formatter: '{value}%',
        color: '#303133',
        offsetCenter: [0, 0]
      },
      // 后端 usageRate 返回带 % 的字符串（如 "10.00%"），parseFloat 取纯数字，% 由 formatter 拼接
      data: [{ value: parseFloat(dashboard.value.usageRate) || 0, name: '货位使用率' }]
    }
  ]
}))

/** 异常/超期环形图 */
const { chartRef: pieRef, render: renderPie } = useEcharts(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}: {c}' },
      data: [
        { name: '正常待取', value: Number(dashboard.value.waitingPickup || 0), itemStyle: { color: '#e6a23c' } },
        { name: '异常件', value: Number(dashboard.value.exceptionCount || 0), itemStyle: { color: '#f56c6c' } },
        { name: '超期件', value: Number(dashboard.value.overdueCount || 0), itemStyle: { color: '#909399' } }
      ]
    }
  ]
}))

/** 加载全部统计数据 */
async function loadAll() {
  loading.value = true
  try {
    const [dash, trend, company] = await Promise.all([
      getDashboard(),
      getTrend(),
      getCompanyStats()
    ])
    dashboard.value = { ...dashboard.value, ...(dash.data || {}) }
    trendData.value = trend.data || []
    companyData.value = company.data || []
    renderTrend()
    renderBar()
    renderGauge()
    renderPie()
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 核心指标 -->
    <el-row :gutter="16">
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
        <StatCard title="超期件" :value="dashboard.overdueCount" icon="AlarmClock" color="#909399" />
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="14">
        <div class="card">
          <div class="card-title">近 7 日入库 / 取件趋势</div>
          <div ref="trendRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="card">
          <div class="card-title">货位使用率</div>
          <div ref="gaugeRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="14">
        <div class="card">
          <div class="card-title">快递公司业务量</div>
          <div ref="barRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="card">
          <div class="card-title">待取 / 异常 / 超期分布</div>
          <div ref="pieRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.card-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
</style>
