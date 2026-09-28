<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyExpress } from '@/api/express'
import { useAuthStore } from '@/stores/auth'
import { EXPRESS_STATUS_OPTIONS } from '@/constants/dict'

/**
 * 我的快递（登录即可，分视角）：快递员=本人经手入库的快递；管理员/普通用户=收件人为您本人（或登记手机号为您手机号）
 * 数据来自 /express/my（含取件码与货位）；流转详情接口后端仅管理员开放，故本页内联展示、不跳详情
 */
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
/** 快递员经手视角（文案区分） */
const isCourier = authStore.role === 'COURIER'
const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 查询条件 */
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })

/** 加载我的快递 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    // 去掉空条件，避免后端把空串当有效条件
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listMyExpress(params)
    list.value = r.data || []
    total.value = Number(r.map?.total || 0)
  } finally {
    loading.value = false
  }
}

/** 搜索：回到第一页 */
function handleSearch() {
  query.pageNum = 1
  loadList()
}

/** 重置条件 */
function handleReset() {
  Object.assign(query, { keyword: '', status: '', pageNum: 1 })
  loadList()
}

onMounted(() => {
  // 兼容首页「待取件·查看全部」带 status 预筛选进入
  if (route.query.status) query.status = String(route.query.status)
  loadList()
})
</script>

<template>
  <div class="page-container">
    <!-- 说明条 -->
    <el-alert
      :title="isCourier
        ? '这里汇总由您入库的快递（经手视角，含待取/已取/异常等全部状态）；可按单号检索、按状态筛选。'
        : '这里汇总收件人为您（或登记手机号为您手机号）的全部快递；请记录取件码，凭码到驿站取件。'"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-input v-model="query.keyword" placeholder="快递单号" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="快递状态" clearable style="width: 150px">
        <el-option v-for="opt in EXPRESS_STATUS_OPTIONS" :key="opt.value" v-bind="opt" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      <el-button type="warning" :icon="'Key'" @click="router.push('/pickup/verify')">取件码验证</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="expressNo" label="快递单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="companyName" label="快递公司" min-width="100" show-overflow-tooltip />
        <el-table-column prop="pickupCode" label="取件码" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pickupCode" type="warning" effect="plain">{{ row.pickupCode }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="货位" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.shelfNo || '-' }} · {{ row.locationNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <dict-tag type="EXPRESS_STATUS" :code="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="超期" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overdueFlag === 1" type="danger" size="small">已超期</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="arrivalTime" label="到达时间" width="170">
          <template #default="{ row }">{{ row.arrivalTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="storageTime" label="入库时间" width="170">
          <template #default="{ row }">{{ row.storageTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="pickupTime" label="取件时间" width="170">
          <template #default="{ row }">{{ row.pickupTime || '-' }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(p) => ((query.pageNum = p), loadList())"
          @size-change="(s) => ((query.pageSize = s), (query.pageNum = 1), loadList())"
        />
      </div>
    </div>
  </div>
</template>
