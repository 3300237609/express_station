<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listCourier, addCourier, updateCourier, deleteCourier, updateUserStatus, resetUserPassword } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMobileCollapse } from '@/composables/useMobileCollapse'

/**
 * 快递员管理（管理员）：快递员账号增删改查、启用禁用、重置密码
 */
const { isMobile, filterCollapsed, toggleFilter } = useMobileCollapse()

const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 查询条件 */
const query = reactive({ pageNum: 1, pageSize: 10, kw: '', status: '' })

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listCourier(params)
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

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确！', trigger: 'blur' }
  ]
}

const formRef = ref()

function openAdd() {
  isEdit.value = false
  formRef.value?.resetFields()
  Object.assign(form, { id: null, username: '', password: '', realName: '', phone: '', email: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email
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
      const { password, ...data } = form
      await updateCourier(data)
      ElMessage.success('修改成功！')
    } else {
      await addCourier({ ...form })
      ElMessage.success('新增成功！')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    dialogLoading.value = false
  }
}

/** 启用/禁用 */
async function handleToggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const text = next === 0 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${text}快递员【${row.realName}】的账号吗？`, `${text}确认`, {
    type: 'warning',
    confirmButtonText: `确定${text}`,
    cancelButtonText: '取消'
  })
  await updateUserStatus({ id: row.id, status: next })
  ElMessage.success(`${text}成功！`)
  loadList()
}

/** 重置密码 */
async function handleResetPwd(row) {
  await ElMessageBox.confirm(`确定重置快递员【${row.realName}】的密码吗？`, '重置密码', {
    type: 'warning',
    confirmButtonText: '确定重置',
    cancelButtonText: '取消'
  })
  await resetUserPassword(row.id)
  ElMessage.success('密码已重置为默认密码！')
}

/** 删除快递员 */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除快递员【${row.realName}】吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await deleteCourier(row.id)
  ElMessage.success('删除成功！')
  loadList()
}

/** 移动端下拉菜单命令分发 */
function handleRowCommand(command, row) {
  switch (command) {
    case 'edit':
      openEdit(row); break
    case 'toggle':
      handleToggleStatus(row); break
    case 'resetPwd':
      handleResetPwd(row); break
    case 'delete':
      handleDelete(row); break
  }
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
      <el-input v-model="query.kw" placeholder="用户名/姓名/手机号" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="账号状态" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button type="success" :icon="'Plus'" @click="openAdd">新增快递员</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="130" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <dict-tag type="USER_STATUS" :code="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="deliveryCount" label="累计投递" width="100" align="center">
          <template #default="{ row }">{{ row.deliveryCount ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" :fixed="!isMobile ? 'right' : false">
          <template #default="{ row }">
            <!-- 桌面端：完整按钮组 -->
            <template v-if="!isMobile">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="warning" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="info" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
            <!-- 移动端：更多下拉 -->
            <el-dropdown v-else trigger="click" @command="(c) => handleRowCommand(c, row)">
              <el-button link type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="toggle">{{ row.status === 1 ? '禁用' : '启用' }}</el-dropdown-item>
                  <el-dropdown-item command="resetPwd">重置密码</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑快递员' : '新增快递员'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
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
