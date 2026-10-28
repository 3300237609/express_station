<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listException, addException, handleException } from '@/api/exception'
import { EXCEPTION_TYPE, EXCEPTION_TYPE_OPTIONS, HANDLE_STATUS, HANDLE_STATUS_OPTIONS } from '@/constants/dict'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { useMobileCollapse } from '@/composables/useMobileCollapse'

/**
 * 异常件管理（快递员登记 / 管理员处理）
 * 流程：登记异常 → 处理中 → 已处理 → 已关闭
 */
const authStore = useAuthStore()
const { isMobile, filterCollapsed, toggleFilter } = useMobileCollapse()
const isAdmin = authStore.role === 'ADMIN'

const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 查询条件 */
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', exceptionType: '', handleStatus: '' })

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listException(params)
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

// ==================== 登记异常 ====================
const addVisible = ref(false)
const addLoading = ref(false)

const addForm = reactive({
  expressNo: '',
  exceptionType: '',
  description: '',
  imageUrl: ''
})

const addRules = {
  expressNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }],
  exceptionType: [{ required: true, message: '请选择异常类型', trigger: 'change' }],
  description: [{ required: true, message: '请填写异常描述', trigger: 'blur' }]
}

const addRef = ref()

function openAdd() {
  addRef.value?.resetFields()
  Object.assign(addForm, { expressNo: '', exceptionType: '', description: '', imageUrl: '' })
  addVisible.value = true
}

/** 提交登记（后端 expressId/expressNo 二选一，单号直传免换查） */
async function submitAdd() {
  // 1. 表单校验
  const valid = await addRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 提交登记（expressNo 直传，快递解析与归属校验由后端完成）
  addLoading.value = true
  try {
    await addException({
      expressNo: addForm.expressNo.trim(),
      exceptionType: addForm.exceptionType,
      description: addForm.description,
      imageUrl: addForm.imageUrl || undefined
    })
    ElMessage.success('异常登记成功，已通知管理员处理！')
    addVisible.value = false
    loadList()
  } finally {
    addLoading.value = false
  }
}

// ==================== 处理异常（管理员） ====================
const handleVisible = ref(false)
const handleLoading = ref(false)
const currentRow = ref(null)

const handleForm = reactive({ id: null, handleStatus: 'PROCESSING', handleRemark: '' })

const handleRules = {
  handleStatus: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  handleRemark: [{ required: true, message: '请填写处理结果', trigger: 'blur' }]
}

const handleRef = ref()

function openHandle(row) {
  currentRow.value = row
  Object.assign(handleForm, {
    id: row.id,
    handleStatus: row.handleStatus || 'PROCESSING',
    handleRemark: row.handleRemark || ''
  })
  handleVisible.value = true
}

/** 提交处理结果 */
async function submitHandle() {
  // 1. 表单校验
  const valid = await handleRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 提交
  handleLoading.value = true
  try {
    await handleException({ ...handleForm })
    ElMessage.success('处理成功，已通知相关用户！')
    handleVisible.value = false
    loadList()
  } finally {
    handleLoading.value = false
  }
}

/** 移动端下拉菜单命令分发 */
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
      <el-input v-model="query.keyword" placeholder="单号/收件人" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.exceptionType" placeholder="异常类型" clearable>
        <el-option v-for="opt in EXCEPTION_TYPE_OPTIONS" :key="opt.value" v-bind="opt" />
      </el-select>
      <el-select v-model="query.handleStatus" placeholder="处理状态" clearable>
        <el-option v-for="opt in HANDLE_STATUS_OPTIONS" :key="opt.value" v-bind="opt" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button type="danger" :icon="'Warning'" @click="openAdd">登记异常</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="expressNo" label="快递单号" min-width="150" show-overflow-tooltip />
        <el-table-column label="异常类型" width="130" align="center">
          <template #default="{ row }">
            <el-tag type="danger" effect="plain">{{ EXCEPTION_TYPE[row.exceptionType] || row.exceptionType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="异常描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reporterName" label="登记人" width="100">
          <template #default="{ row }">{{ row.reporterName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" width="100">
          <template #default="{ row }">{{ row.handlerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="110" align="center">
          <template #default="{ row }">
            <dict-tag type="HANDLE_STATUS" :code="row.handleStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="登记时间" width="170" />
        <el-table-column prop="handleTime" label="处理时间" width="170">
          <template #default="{ row }">{{ row.handleTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" :fixed="!isMobile ? 'right' : false">
          <template #default="{ row }">
            <el-button
              v-if="isAdmin && (row.handleStatus === 'PENDING' || row.handleStatus === 'PROCESSING')"
              link
              type="primary"
              @click="openHandle(row)"
            >
              处理
            </el-button>
            <span v-else>-</span>
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

    <!-- 登记异常弹窗 -->
    <el-dialog v-model="addVisible" title="登记异常快递" width="520px">
      <el-form ref="addRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="快递单号" prop="expressNo">
          <el-input v-model="addForm.expressNo" placeholder="输入快递单号" />
        </el-form-item>
        <el-form-item label="异常类型" prop="exceptionType">
          <el-select v-model="addForm.exceptionType" placeholder="选择异常类型" style="width: 100%">
            <el-option v-for="opt in EXCEPTION_TYPE_OPTIONS" :key="opt.value" v-bind="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常描述" prop="description">
          <el-input v-model="addForm.description" type="textarea" :rows="4" placeholder="描述异常情况（破损程度、丢失经过等）" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="addForm.imageUrl" placeholder="异常图片 URL（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">提交登记</el-button>
      </template>
    </el-dialog>

    <!-- 处理异常弹窗（管理员） -->
    <el-dialog v-model="handleVisible" title="处理异常件" width="520px">
      <el-alert
        v-if="currentRow"
        :title="`快递：${currentRow.expressNo} · ${EXCEPTION_TYPE[currentRow.exceptionType]}`"
        type="info"
        :closable="false"
        style="margin-bottom: 14px"
      />
      <el-form ref="handleRef" :model="handleForm" :rules="handleRules" label-width="90px">
        <el-form-item label="处理状态" prop="handleStatus">
          <el-select v-model="handleForm.handleStatus" style="width: 100%">
            <el-option
              v-for="opt in HANDLE_STATUS_OPTIONS"
              :key="opt.value"
              v-bind="opt"
              :disabled="opt.value === 'PENDING'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果" prop="handleRemark">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="4" placeholder="填写处理结果（将通知相关用户）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>
