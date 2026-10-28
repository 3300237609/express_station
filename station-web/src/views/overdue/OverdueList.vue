<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOverdueExpress, remindOverdue, refreshOverdue } from '@/api/overdue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMobileCollapse } from '@/composables/useMobileCollapse'

/**
 * 超期快递（管理员）：超期列表、超期时长、发送用户提醒
 */
const router = useRouter()
const { isMobile, filterCollapsed, toggleFilter } = useMobileCollapse()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const remindingId = ref(null)
const refreshing = ref(false)

/** 查询条件 */
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listOverdueExpress(params)
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

/** 手动触发超期检测（与后端定时任务共用实现，返回本次新标记情况） */
async function handleRefresh() {
  refreshing.value = true
  try {
    const r = await refreshOverdue()
    ElMessage.success(r.data || '超期检测完成！')
    loadList()
  } finally {
    refreshing.value = false
  }
}

/** 发送超期提醒 */
async function handleRemind(row) {
  await ElMessageBox.confirm(`向 ${row.receiverName}（${row.receiverPhone}）发送超期提醒吗？`, '发送提醒', {
    type: 'warning',
    confirmButtonText: '发送',
    cancelButtonText: '取消'
  })
  remindingId.value = row.id
  try {
    await remindOverdue(row.id)
    ElMessage.success('提醒已发送！')
    loadList()
  } finally {
    remindingId.value = null
  }
}

/** 移动端下拉菜单命令分发 */
onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 说明条 -->
    <el-alert
      title="超期规则：快递入库后超过系统配置的保管时长（系统配置 → station.overdue.hours）即标记为超期，可在此查询并提醒用户。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 查询条件 -->
    <div class="card filter-bar" :class="{ collapsed: filterCollapsed }">
      <div class="filter-header" @click="toggleFilter">
        <span class="filter-title">筛选条件</span>
        <el-icon class="filter-arrow"><component :is="filterCollapsed ? 'ArrowDown' : 'ArrowUp'" /></el-icon>
      </div>
      <div class="filter-body">
      <el-input v-model="query.keyword" placeholder="单号/收件人/手机号" clearable @keyup.enter="handleSearch" />
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button :icon="'Refresh'" :loading="refreshing" @click="handleRefresh">超期检测</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="expressNo" label="快递单号" min-width="150" show-overflow-tooltip />
        <el-table-column label="货位" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.shelfNo || '-' }} · {{ row.locationNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收件人" width="100" />
        <el-table-column prop="receiverPhone" label="手机号" width="120" />
        <el-table-column prop="pickupCode" label="取件码" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">{{ row.pickupCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storageTime" label="入库时间" width="170" />
        <el-table-column label="超期时长" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="danger" effect="dark">
              {{ row.overdueHours != null ? `${row.overdueHours} 小时` : '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" :fixed="!isMobile ? 'right' : false">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/express/detail/${row.id}`)">详情</el-button>
            <el-button
              link
              type="warning"
              :loading="remindingId === row.id"
              @click="handleRemind(row)"
            >
              发送提醒
            </el-button>
          </template>
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
