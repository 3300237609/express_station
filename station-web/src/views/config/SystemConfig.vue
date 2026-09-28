<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { listConfig, updateConfig } from '@/api/config'
import { ElMessage } from 'element-plus'

/**
 * 系统配置（管理员）：保管时长、取件码长度、通知开关等运行参数维护
 */
const loading = ref(false)
const list = ref([])

/** 本地筛选关键字（后端查询全部，筛选在前端做） */
const kw = ref('')

/** 过滤后的展示列表（按配置键/配置说明模糊匹配） */
const filteredList = computed(() => {
  const key = kw.value.trim().toLowerCase()
  if (!key) return list.value
  return list.value.filter(
    (c) =>
      String(c.configKey || '').toLowerCase().includes(key) ||
      String(c.description || '').toLowerCase().includes(key)
  )
})

/** 加载列表（后端查询全部，不分页） */
async function loadList() {
  loading.value = true
  try {
    const r = await listConfig()
    list.value = r.data || []
  } finally {
    loading.value = false
  }
}

// ==================== 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogLoading = ref(false)

const form = reactive({ id: null, configKey: '', configValue: '', description: '', status: 1 })

const rules = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

const formRef = ref()

/** 打开编辑弹窗 */
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    configKey: row.configKey,
    configValue: row.configValue,
    description: row.description,
    status: row.status
  })
  dialogVisible.value = true
}

/** 提交修改 */
async function handleSubmit() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 执行修改
  dialogLoading.value = true
  try {
    await updateConfig({
      id: form.id,
      configKey: form.configKey,
      configValue: form.configValue,
      description: form.description,
      status: form.status
    })
    ElMessage.success('配置修改成功！')
    dialogVisible.value = false
    loadList()
  } finally {
    dialogLoading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 说明条 -->
    <el-alert
      title="常用配置：station.overdue.hours（超期小时数）、pickup.code.length（取件码长度）、notification.enabled（通知开关）。修改后立即生效。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-input v-model="kw" placeholder="配置键/说明，输入即筛" clearable />
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="filteredList" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configKey" label="配置键" min-width="200" show-overflow-tooltip />
        <el-table-column prop="configValue" label="配置值" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">{{ row.configValue }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="配置说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑系统配置" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="配置键">
          <el-input :model-value="form.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="配置说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>
