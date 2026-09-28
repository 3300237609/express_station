<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listCompany, listCompanyVolume, addCompany, updateCompany, updateStatusCompany, deleteCompany } from '@/api/company'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 快递公司管理（管理员）：公司信息维护 + 启用停用 + 业务量查看
 * 状态由专用接口 updateStatusCompany 维护（新增/修改接口不接收 status）
 */
const loading = ref(false)
const list = ref([])
const total = ref(0)
/** 各公司业务量：{ [companyId]: totalCount }，来自 /company/volume */
const volumeMap = ref({})

/** 查询条件（keyword 对应后端 /company/list 的查询参数） */
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })

/** 加载列表（业务量单独统计，失败时列显示 0 不阻断列表） */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const [r, rv] = await Promise.all([listCompany(params), listCompanyVolume().catch(() => null)])
    list.value = r.data || []
    total.value = Number(r.map?.total || 0)
    const map = {}
    ;(rv?.data || []).forEach((v) => (map[v.companyId] = v.totalCount))
    volumeMap.value = map
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  companyName: '',
  companyCode: '',
  contactName: '',
  contactPhone: ''
})

const rules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  companyCode: [{ required: true, message: '请输入公司编码', trigger: 'blur' }]
}

const formRef = ref()

function openAdd() {
  isEdit.value = false
  formRef.value?.resetFields()
  Object.assign(form, { id: null, companyName: '', companyCode: '', contactName: '', contactPhone: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    companyName: row.companyName,
    companyCode: row.companyCode,
    contactName: row.contactName,
    contactPhone: row.contactPhone
  })
  dialogVisible.value = true
}

/** 提交新增/编辑（状态不在弹窗内维护，由列表页状态开关直连专用接口） */
async function handleSubmit() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 执行写操作
  dialogLoading.value = true
  try {
    const payload = {
      companyName: form.companyName,
      companyCode: form.companyCode,
      contactName: form.contactName,
      contactPhone: form.contactPhone
    }
    if (isEdit.value) {
      await updateCompany({ id: form.id, ...payload })
      ElMessage.success('修改成功！')
    } else {
      await addCompany(payload)
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
 * 启用/停用快递公司（状态列开关直连专用接口，不进编辑弹窗）
 * 受控开关：接口成功才更新行数据，失败/取消时开关自动保持原状态
 */
async function handleToggleStatus(row, value) {
  const next = value ? 1 : 0
  // 停用属危险操作：二次确认，取消则开关不变
  if (next === 0) {
    try {
      await ElMessageBox.confirm(`确定停用快递公司【${row.companyName}】吗？停用后入库/寄件时将无法选择该公司。`, '停用确认', {
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
    await updateStatusCompany(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '启用成功！' : '停用成功！')
  } catch {
    // 失败已由请求拦截器统一提示，开关保持原状态
  } finally {
    statusLoadingId.value = null
  }
}

/** 删除快递公司（存在关联快递时后端会拒绝） */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除快递公司【${row.companyName}】吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteCompany(row.id)
  ElMessage.success('删除成功！')
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-input v-model="query.keyword" placeholder="公司名称/编码" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="公司状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button type="success" :icon="'Plus'" @click="openAdd">新增快递公司</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="companyName" label="公司名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="companyCode" label="公司编码" width="120" />
        <el-table-column prop="contactName" label="联系人" width="110">
          <template #default="{ row }">{{ row.contactName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系电话" width="130">
          <template #default="{ row }">{{ row.contactPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="业务量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">{{ volumeMap[row.id] ?? 0 }}</el-tag>
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
                {{ row.status === 1 ? '启用' : '停用' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑快递公司' : '新增快递公司'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="如 顺丰、中通" />
        </el-form-item>
        <el-form-item label="公司编码" prop="companyCode">
          <el-input v-model="form.companyCode" placeholder="如 SF、ZTO" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="选填" />
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
</style>
