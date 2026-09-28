<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listUser, addUser, updateUser, deleteUser, updateUserStatus, resetUserPassword } from '@/api/user'
import { ROLE, USER_STATUS } from '@/constants/dict'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 用户管理（管理员）：普通用户增删改查、启用禁用、重置密码、查看其快递与取件记录
 */
const router = useRouter()
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
    const r = await listUser(params)
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
  email: '',
  role: 'USER'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确！', trigger: 'blur' }
  ]
}

const formRef = ref()

function openAdd() {
  isEdit.value = false
  formRef.value?.resetFields()
  Object.assign(form, { id: null, username: '', password: '', realName: '', phone: '', email: '', role: 'USER' })
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
    email: row.email,
    role: row.role || 'USER'
  })
  dialogVisible.value = true
}

/** 提交新增/编辑（编辑时不提交 password） */
async function handleSubmit() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 执行写操作
  dialogLoading.value = true
  try {
    if (isEdit.value) {
      const { password, ...data } = form
      await updateUser(data)
      ElMessage.success('修改成功！')
    } else {
      await addUser({ ...form })
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
  await ElMessageBox.confirm(`确定${text}用户【${row.realName}】吗？`, `${text}确认`, {
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
  await ElMessageBox.confirm(
    `确定重置用户【${row.realName}】的密码为默认密码吗？`,
    '重置密码',
    { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' }
  )
  await resetUserPassword(row.id)
  ElMessage.success('密码已重置为默认密码！')
}

/** 删除用户 */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户【${row.realName}】吗？删除后数据不可恢复！`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await deleteUser(row.id)
  ElMessage.success('删除成功！')
  loadList()
}

/** 查看该用户的快递（带收件人与手机号筛选，对应后端 receiverName/receiverPhone 参数） */
function viewExpress(row) {
  router.push({ path: '/express/list', query: { receiverName: row.realName, receiverPhone: row.phone } })
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-input v-model="query.kw" placeholder="用户名/姓名/手机号" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="账号状态" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button type="success" :icon="'Plus'" @click="openAdd">新增用户</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'COURIER' ? 'warning' : 'primary'">
              {{ ROLE[row.role] || row.role || '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <dict-tag type="USER_STATUS" :code="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button link type="primary" @click="viewExpress(row)">TA的快递</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
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
