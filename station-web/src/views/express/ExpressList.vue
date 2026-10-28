<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listExpress, deleteExpress, updateExpressStatus, updateExpressLocation } from '@/api/express'
import { listCompanyOptions } from '@/api/company'
import { recommendLocation, detailLocation } from '@/api/shelf'
import { EXPRESS_STATUS_OPTIONS } from '@/constants/dict'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMobileCollapse } from '@/composables/useMobileCollapse'

/**
 * 快递列表（管理员）：条件查询、详情、状态修正、调整货位、删除
 * 查询条件对应后端参数：单号 keyword、收件人 receiverName、手机号 receiverPhone、
 * 状态 status、公司 companyId、到达时间范围 arrivalTimeStart/End（yyyy-MM-dd，含当日）
 */
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isMobile, filterCollapsed, toggleFilter } = useMobileCollapse()

const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 到达时间范围选择器绑定值（拆分为 arrivalTimeStart/End 提交） */
const dateRange = ref([])

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  receiverName: '',
  receiverPhone: '',
  status: '',
  companyId: '',
  arrivalTimeStart: '',
  arrivalTimeEnd: ''
})

/** 快递公司选项（下拉） */
const companyOptions = ref([])

/** 查询列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    // 去掉空条件，避免后端把空串当有效条件
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listExpress(params)
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
  Object.assign(query, {
    keyword: '',
    receiverName: '',
    receiverPhone: '',
    status: '',
    companyId: '',
    arrivalTimeStart: '',
    arrivalTimeEnd: '',
    pageNum: 1
  })
  dateRange.value = []
  loadList()
}

/** 翻页/改页大小 */
function handlePageChange(page) {
  query.pageNum = page
  loadList()
}
function handleSizeChange(size) {
  query.pageSize = size
  query.pageNum = 1
  loadList()
}

/** 查看详情 */
function goDetail(row) {
  router.push(`/express/detail/${row.id}`)
}

/** 移动端下拉菜单命令分发 */
function handleRowCommand(command, row) {
  switch (command) {
    case 'detail':
      goDetail(row); break
    case 'location':
      openLocationDialog(row); break
    case 'status':
      openStatusDialog(row); break
    case 'delete':
      handleDelete(row); break
  }
}

/** 删除快递（逻辑删除，需确认） */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除快递【${row.expressNo}】吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await deleteExpress(row.id)
  ElMessage.success('删除成功！')
  loadList()
}

// ==================== 调整货位 ====================
const locationDialogVisible = ref(false)
const currentRow = ref(null)
const freeLocations = ref([])   // 实际为 recommendLocation 返回的推荐列表
const selectedLocationId = ref(null)
const locationLoading = ref(false)

/** 打开调整货位弹窗：按快递尺寸/重量调智能推荐 */
async function openLocationDialog(row) {
  currentRow.value = row
  selectedLocationId.value = null
  locationDialogVisible.value = true
  locationLoading.value = true
  try {
    const params = {}
    if (row.sizeType) params.sizeType = row.sizeType
    if (row.weight) params.weight = row.weight
    const [recR, curR] = await Promise.all([
      recommendLocation(params).catch(() => ({ data: [] })),
      row.locationId ? detailLocation(row.locationId).catch(() => ({ data: null })) : Promise.resolve({ data: null })
    ])
    let list = recR.data || []

    // 给当前所在货位打 isCurrent 标记（无论是否在推荐列表里都显示标签）
    if (row.locationId) {
      const idx = list.findIndex(l => l.locationId === row.locationId)
      if (idx >= 0) {
        list[idx].isCurrent = true
      } else {
        const cur = curR.data || {}
        const cap = Number(cur.capacity ?? 0)
        const used = Number(cur.usedCount ?? 0)
        list.unshift({
          locationId: row.locationId,
          locationNo: row.locationNo,
          shelfNo: row.shelfNo || cur.shelfNo || '',
          remaining: Math.max(cap - used, 0),
          score: null,
          reason: cur.status === 4 ? '当前货位已停用' : '当前所在货位',
          isCurrent: true
        })
      }
    }
    freeLocations.value = list
    selectedLocationId.value = row.locationId
  } finally {
    locationLoading.value = false
  }
}

/** 提交货位调整 */
async function submitLocation() {
  // 1. 空校验
  if (!selectedLocationId.value) {
    ElMessage.error('请选择目标货位！')
    return
  }
  // 2. 执行调整
  await updateExpressLocation({ expressId: currentRow.value.id, locationId: selectedLocationId.value })
  ElMessage.success('货位调整成功！')
  locationDialogVisible.value = false
  loadList()
}

// ==================== 状态修正 ====================
const statusDialogVisible = ref(false)
const statusRow = ref(null)
const targetStatus = ref('')

/** 打开状态修正弹窗（管理员专用，走后端 updateStatus 接口，不动单据本身） */
function openStatusDialog(row) {
  statusRow.value = row
  targetStatus.value = row.status
  statusDialogVisible.value = true
}

/** 提交状态修正 */
async function submitStatus() {
  if (targetStatus.value === statusRow.value.status) {
    ElMessage.info('状态未变化，无需修正！')
    return
  }
  await updateExpressStatus(statusRow.value.id, targetStatus.value)
  ElMessage.success('状态修正成功！')
  statusDialogVisible.value = false
  loadList()
}

/** 从路由参数带入筛选（如用户列表「TA的快递」跳转携带收件人与手机号） */
function applyRouteQuery() {
  const q = route.query
  if (q.keyword) query.keyword = String(q.keyword)
  if (q.receiverName) query.receiverName = String(q.receiverName)
  if (q.receiverPhone) query.receiverPhone = String(q.receiverPhone)
  if (q.status) query.status = String(q.status)
}

onMounted(async () => {
  applyRouteQuery()
  await loadList()
  // 公司选项加载失败不影响主流程
  try {
    const r = await listCompanyOptions()
    companyOptions.value = r.data || []
  } catch (e) {
    companyOptions.value = []
  }
})
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
      <el-input v-model="query.keyword" placeholder="快递单号" clearable @keyup.enter="handleSearch" />
      <el-input v-model="query.receiverName" placeholder="收件人" clearable @keyup.enter="handleSearch" />
      <el-input v-model="query.receiverPhone" placeholder="收件手机号" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="快递状态" clearable>
        <el-option v-for="opt in EXPRESS_STATUS_OPTIONS" :key="opt.value" v-bind="opt" />
      </el-select>
      <el-select v-model="query.companyId" placeholder="快递公司" clearable>
        <el-option
          v-for="opt in companyOptions"
          :key="opt.id"
          :label="opt.companyName"
          :value="opt.id"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="到达开始日期"
        end-placeholder="到达结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="
          (val) => {
            query.arrivalTimeStart = val?.[0] || ''
            query.arrivalTimeEnd = val?.[1] || ''
          }
        "
      />
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      <el-button
        v-if="authStore.hasRole(['ADMIN', 'COURIER'])"
        type="success"
        :icon="'FolderAdd'"
        @click="router.push('/express/in')"
      >
        快递入库
      </el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="expressNo" label="快递单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="companyName" label="快递公司" min-width="100" show-overflow-tooltip />
        <el-table-column prop="receiverName" label="收件人" width="100" />
        <el-table-column prop="receiverPhone" label="收件手机号" width="120" />
        <el-table-column prop="pickupCode" label="取件码" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pickupCode" type="warning" effect="plain">{{ row.pickupCode }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="locationNo" label="货位" width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.locationNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <dict-tag type="EXPRESS_STATUS" :code="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="超期" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overdueFlag === 1" type="danger" size="small">已超期</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="storageTime" label="入库时间" width="170" />
        <el-table-column label="操作" width="290" :fixed="!isMobile ? 'right' : false">
          <template #default="{ row }">
            <!-- 桌面端：完整按钮组 -->
            <template v-if="!isMobile">
              <el-button link type="primary" @click="goDetail(row)">详情</el-button>
              <el-button
                v-if="authStore.hasRole(['ADMIN']) && row.status === 'WAITING_PICKUP'"
                link
                type="success"
                @click="openLocationDialog(row)"
              >
                调整货位
              </el-button>
              <el-button
                v-if="authStore.hasRole(['ADMIN'])"
                link
                type="warning"
                @click="openStatusDialog(row)"
              >
                状态修正
              </el-button>
              <el-button
                v-if="authStore.hasRole(['ADMIN'])"
                link
                type="danger"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
            <!-- 移动端：更多下拉 -->
            <el-dropdown v-else trigger="click" @command="(c) => handleRowCommand(c, row)">
              <el-button link type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="detail">详情</el-dropdown-item>
                  <el-dropdown-item
                    v-if="authStore.hasRole(['ADMIN']) && row.status === 'WAITING_PICKUP'"
                    command="location"
                  >调整货位</el-dropdown-item>
                  <el-dropdown-item
                    v-if="authStore.hasRole(['ADMIN'])"
                    command="status"
                  >状态修正</el-dropdown-item>
                  <el-dropdown-item
                    v-if="authStore.hasRole(['ADMIN'])"
                    command="delete"
                    divided
                  >删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
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
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 调整货位弹窗 -->
    <el-dialog v-model="locationDialogVisible" title="调整货位" width="480px">
      <div v-loading="locationLoading" style="min-height: 120px">
        <el-alert
          v-if="freeLocations.length === 0 && !locationLoading"
          title="当前没有可用货位，请先在货位管理中释放或启用货位！"
          type="warning"
          :closable="false"
          style="margin-bottom: 14px"
        />
        <el-form label-width="80px">
          <el-form-item label="目标货位">
            <el-select v-model="selectedLocationId" placeholder="选择推荐货位" style="width: 100%">
              <el-option
                v-for="(loc, idx) in freeLocations"
                :key="loc.locationId"
                :label="`${loc.locationNo}（${loc.shelfNo || '-'} · 剩余 ${loc.remaining ?? 0}${loc.score != null ? ' · 评分 ' + loc.score : ''}）`"
                :value="loc.locationId"
              >
                <span>{{ loc.locationNo }}</span>
                <span style="color: var(--el-text-color-secondary); margin-left: 8px">
                  {{ loc.shelfNo || '-' }} · 剩余 {{ loc.remaining ?? 0 }}
                </span>
                <el-tag v-if="loc.score != null" type="warning" size="small" style="margin-left: 8px">{{ loc.score }} 分</el-tag>
                <el-tag v-if="idx === 0 && !loc.isCurrent" type="success" size="small" style="margin-left: 6px">首推</el-tag>
                <el-tag v-if="loc.isCurrent" type="info" size="small" style="margin-left: 6px">当前</el-tag>
                <span v-if="loc.reason" style="color: var(--el-text-color-secondary); margin-left: 8px; font-size: 12px">
                  {{ loc.reason }}
                </span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLocation">确定调整</el-button>
      </template>
    </el-dialog>

    <!-- 状态修正弹窗 -->
    <el-dialog v-model="statusDialogVisible" title="状态修正" width="420px">
      <el-form label-width="90px">
        <el-form-item label="快递单号">
          <el-input :model-value="statusRow?.expressNo" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <dict-tag type="EXPRESS_STATUS" :code="statusRow?.status" />
        </el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="targetStatus" style="width: 100%">
            <el-option v-for="opt in EXPRESS_STATUS_OPTIONS" :key="opt.value" v-bind="opt" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatus">确定修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>
