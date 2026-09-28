<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listShelf, addShelf, updateShelf, deleteShelf, updateShelfStatus } from '@/api/shelf'
import { usagePercent } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 货架管理（管理员）：货架增删改查、启停开关 + 货位使用率展示
 * 后端只有一个关键字 keyword（同时模糊匹配编号或区域）；状态走专用启停接口，不进编辑弹窗
 */
const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 查询条件 */
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listShelf(params)
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

function handleSizeChange(size) {
  query.pageSize = size
  query.pageNum = 1
  loadList()
}

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  shelfNo: '',
  area: '',
  remark: ''
})

const rules = {
  shelfNo: [{ required: true, message: '请输入货架编号', trigger: 'blur' }],
  area: [{ required: true, message: '请输入所属区域', trigger: 'blur' }]
}

const formRef = ref()

/** 打开新增弹窗 */
function openAdd() {
  isEdit.value = false
  formRef.value?.resetFields()
  Object.assign(form, { id: null, shelfNo: '', area: '', remark: '' })
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    shelfNo: row.shelfNo,
    area: row.area,
    remark: row.remark
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
      await updateShelf({ ...form })
      ElMessage.success('修改成功！')
    } else {
      await addShelf({ ...form })
      ElMessage.success('新增成功！')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    dialogLoading.value = false
  }
}

/** 状态开关进行中的行 ID（防止重复点击） */
const statusLoadingId = ref(null)

/**
 * 启用/停用货架（状态列开关直连专用接口，不进编辑弹窗）
 * 受控开关：接口成功才更新行数据，失败/取消时开关自动保持原状态
 */
async function handleToggleStatus(row, value) {
  const next = value ? 1 : 0
  // 停用属危险操作：二次确认，取消则开关不变
  if (next === 0) {
    try {
      await ElMessageBox.confirm(`确定停用货架【${row.shelfNo}】吗？停用后该货架将不可用于存放快递。`, '停用确认', {
        type: 'warning',
        confirmButtonText: '确定停用',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  statusLoadingId.value = row.id
  try {
    await updateShelfStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '启用成功！' : '停用成功！')
  } catch {
    // 失败已由请求拦截器统一提示，开关保持原状态
  } finally {
    statusLoadingId.value = null
  }
}

/** 删除货架（名下有占用货位时后端会拒绝） */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除货架【${row.shelfNo}】吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await deleteShelf(row.id)
  ElMessage.success('删除成功！')
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-input v-model="query.keyword" placeholder="编号/区域" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="货架状态" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button type="success" :icon="'Plus'" @click="openAdd">新增货架</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="shelfNo" label="货架编号" width="120" />
        <el-table-column prop="area" label="所属区域" width="120" />
        <el-table-column prop="capacity" label="总货位" width="90" align="center" />
        <el-table-column prop="usedCount" label="已使用" width="90" align="center" />
        <el-table-column label="使用率" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(((row.usedCount / (row.capacity || 1)) * 100).toFixed(1))"
              :status="row.usedCount >= row.capacity ? 'exception' : undefined"
            />
            <span class="usage-text">{{ usagePercent(row.usedCount, row.capacity) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                :model-value="row.status === 1"
                :disabled="statusLoadingId === row.id"
                @change="(v) => handleToggleStatus(row, v)"
              />
              <span :class="['status-text', row.status === 1 ? 'is-on' : 'is-off']">
                {{ row.status === 1 ? '正常' : '停用' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑货架' : '新增货架'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="货架编号" prop="shelfNo">
          <el-input v-model="form.shelfNo" placeholder="如 A-01" />
        </el-form-item>
        <el-form-item label="所属区域" prop="area">
          <el-input v-model="form.area" placeholder="如 A 区" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
/* 状态列：紧凑开关 + 当前状态文字（不用 el-switch 的 active/inactive-text，双文案占位会把列撑爆） */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.status-text {
  font-size: 12px;
  line-height: 1;
}

.status-text.is-on {
  color: #67c23a;
}

.status-text.is-off {
  color: #909399;
}

.usage-text {
  font-size: 12px;
  color: #909399;
}
</style>
