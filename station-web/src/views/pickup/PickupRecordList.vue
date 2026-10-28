<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listPickupRecord } from '@/api/pickup'
import { PICKUP_METHOD, labelOf } from '@/constants/dict'
import { useMobileCollapse } from '@/composables/useMobileCollapse'

/**
 * 取件记录（快递员/管理员）：历史取件出库记录查询
 */
const { isMobile, filterCollapsed, toggleFilter } = useMobileCollapse()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  kw: '',
  startTime: '',
  endTime: ''
})

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listPickupRecord(params)
    list.value = r.data || []
    total.value = Number(r.map?.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { kw: '', startTime: '', endTime: '', pageNum: 1 })
  dateRange.value = []
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <div class="card filter-bar" :class="{ collapsed: filterCollapsed }">
      <div class="filter-header" @click="toggleFilter">
        <span class="filter-title">筛选条件</span>
        <el-icon class="filter-arrow"><component :is="filterCollapsed ? 'ArrowDown' : 'ArrowUp'" /></el-icon>
      </div>
      <div class="filter-body">
      <el-input v-model="query.kw" placeholder="单号/取件码/用户" clearable @keyup.enter="handleSearch" />
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="取件开始时间"
        end-placeholder="取件结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
        @change="
          (val) => {
            query.startTime = val?.[0] || ''
            query.endTime = val?.[1] || ''
          }
        "
      />
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="expressNo" label="快递单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="pickupCode" label="取件码" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">{{ row.pickupCode }}</el-tag>
          </template>
        </el-table-column>
        <!-- 取件用户：后端 userName 与 username 大小写撞名时 username 承载显示名，双读回退 -->
        <el-table-column label="取件用户" width="110">
          <template #default="{ row }">{{ row.userName || row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="receiverPhone" label="手机号" width="120" />
        <el-table-column label="取件方式" width="130" align="center">
          <template #default="{ row }">
            <el-tag>{{ labelOf(PICKUP_METHOD, row.pickupMethod) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="110">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="pickupTime" label="取件时间" width="170" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>

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
