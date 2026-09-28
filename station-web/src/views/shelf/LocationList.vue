<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listLocation, addLocation, updateLocation, deleteLocation, updateLocationStatus, listShelf } from '@/api/shelf'
import { LOCATION_STATUS, LOCATION_STATUS_OPTIONS } from '@/constants/dict'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 货位管理（管理员）：货位增删改查、启停与状态展示、查询空闲货位
 * 后端列表仅支持货架/状态两个筛选（无编号模糊查询），启停走专用状态接口
 */
const loading = ref(false)
const list = ref([])
const total = ref(0)
const shelfOptions = ref([])

/** 查询条件 */
const query = reactive({ pageNum: 1, pageSize: 10, shelfId: '', status: '' })

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listLocation(params)
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

/** 快捷：只看空闲货位 */
function showFree() {
  query.status = 1
  handleSearch()
}

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  shelfId: '',
  locationNo: '',
  rowNo: 1,
  columnNo: 1,
  capacity: 1
})

const rules = {
  shelfId: [{ required: true, message: '请选择所属货架', trigger: 'change' }],
  locationNo: [{ required: true, message: '请输入货位编号', trigger: 'blur' }]
}

const formRef = ref()

function openAdd() {
  isEdit.value = false
  formRef.value?.resetFields()
  Object.assign(form, { id: null, shelfId: '', locationNo: '', rowNo: 1, columnNo: 1, capacity: 1 })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    shelfId: row.shelfId,
    locationNo: row.locationNo,
    rowNo: row.rowNo,
    columnNo: row.columnNo,
    capacity: row.capacity
  })
  dialogVisible.value = true
}

/** 提交新增/编辑 */
async function handleSubmit() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 执行写操作
  dialogLoading.value = true
  try {
    if (isEdit.value) {
      await updateLocation({ ...form })
      ElMessage.success('修改成功！')
    } else {
      await addLocation({ ...form })
      ElMessage.success('新增成功！')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    dialogLoading.value = false
  }
}

/**
 * 启用/停用货位（专用状态接口）
 * 停用目标为 4；启用时按已用数恢复系统态：有件回 2 部分占用，无件回 1 空闲
 */
async function handleToggleStatus(row) {
  const disabling = row.status !== 4
  const next = disabling ? 4 : row.usedCount > 0 ? 2 : 1
  const text = disabling ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      disabling ? `确定停用货位【${row.locationNo}】吗？停用后将无法存放快递。` : `确定启用货位【${row.locationNo}】吗？`,
      `${text}确认`,
      { type: 'warning', confirmButtonText: `确定${text}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await updateLocationStatus(row.id, next)
  row.status = next
  ElMessage.success(`${text}成功！`)
}

/** 删除货位（已占用时后端会拒绝） */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除货位【${row.locationNo}】吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await deleteLocation(row.id)
  ElMessage.success('删除成功！')
  loadList()
}

onMounted(async () => {
  await loadList()
  try {
    const r = await listShelf({ pageNum: 1, pageSize: 200 })
    shelfOptions.value = r.data || []
  } catch (e) {
    shelfOptions.value = []
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-select v-model="query.shelfId" placeholder="所属货架" clearable>
        <el-option v-for="s in shelfOptions" :key="s.id" :label="s.shelfNo" :value="s.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="货位状态" clearable>
        <el-option v-for="opt in LOCATION_STATUS_OPTIONS" :key="opt.value" v-bind="opt" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button :icon="'Filter'" @click="showFree">只看空闲</el-button>
      <el-button type="success" :icon="'Plus'" @click="openAdd">新增货位</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="locationNo" label="货位编号" width="130" />
        <el-table-column prop="shelfNo" label="所属货架" width="110" />
        <el-table-column prop="area" label="区域" width="100" />
        <el-table-column prop="rowNo" label="行" width="70" align="center" />
        <el-table-column prop="columnNo" label="列" width="70" align="center" />
        <el-table-column label="容量" width="130" align="center">
          <template #default="{ row }">
            {{ row.usedCount ?? 0 }} / {{ row.capacity }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <dict-tag type="LOCATION_STATUS" :code="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleToggleStatus(row)">
              {{ row.status === 4 ? '启用' : '停用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="(p) => ((query.pageNum = p), loadList())"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑货位' : '新增货位'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属货架" prop="shelfId">
          <el-select v-model="form.shelfId" placeholder="选择货架" style="width: 100%" :disabled="isEdit">
            <el-option v-for="s in shelfOptions" :key="s.id" :label="`${s.shelfNo}（${s.area}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="货位编号" prop="locationNo">
          <el-input v-model="form.locationNo" placeholder="如 A-01-1-1" />
        </el-form-item>
        <el-form-item label="行 / 列">
          <el-col :span="12">
            <el-input-number v-model="form.rowNo" :min="1" :max="99" style="width: 90%" />
          </el-col>
          <el-col :span="12">
            <el-input-number v-model="form.columnNo" :min="1" :max="99" style="width: 90%" />
          </el-col>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="1" :max="99" />
          <span class="capacity-tip">1 表示一格一件</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确定新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.capacity-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
