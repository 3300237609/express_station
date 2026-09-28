<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { changePasswordApi } from '@/api/auth'
import { updateUser } from '@/api/user'
import { ROLE } from '@/constants/dict'
import { ElMessage } from 'element-plus'

/**
 * 个人中心：查看/修改个人资料、修改密码
 */
const authStore = useAuthStore()
const activeTab = ref('profile')

// ==================== 个人资料 ====================
const profileLoading = ref(false)

const profileForm = reactive({
  id: null,
  realName: '',
  phone: '',
  email: '',
  avatar: ''
})

const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确！', trigger: 'blur' }
  ]
}

const profileRef = ref()

/** 初始化个人资料：取本地登录态（登录返回并持久化的 userInfo，进页零请求；需最新值可调 store.refresh() 走 /auth/me 远程刷新） */
function loadProfile() {
  const u = authStore.user || {}
  Object.assign(profileForm, {
    id: u.id,
    realName: u.realName,
    phone: u.phone,
    email: u.email,
    avatar: u.avatar
  })
}

/** 保存资料 */
async function saveProfile() {
  // 1. 表单校验
  const valid = await profileRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 提交修改
  profileLoading.value = true
    try {
    const { id, realName, phone, email, avatar } = profileForm
    // username 满足后端 UserDTO 必填校验（后端更新时不落库，仅姓名/手机/邮箱等基本资料生效）
    await updateUser({ id, username: authStore.user?.username, realName, phone, email, avatar })
    authStore.setUser({ ...authStore.user, realName, phone, email, avatar })
    ElMessage.success('资料修改成功！')
  } finally {
    profileLoading.value = false
  }
}

// ==================== 修改密码 ====================
const pwdLoading = ref(false)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

/** 两次密码一致性校验 */
const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致！'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const pwdRef = ref()

/** 提交修改密码（成功后需重新登录） */
async function savePassword() {
  // 1. 表单校验
  const valid = await pwdRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 提交
  pwdLoading.value = true
  try {
    await changePasswordApi({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录！')
    await authStore.logout()
    window.location.href = '/login'
  } finally {
    pwdLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="page-container">
    <el-row :gutter="16" justify="center">
      <el-col :xs="24" :md="16">
        <div class="card">
          <!-- 用户头卡 -->
          <div class="profile-header">
            <el-avatar :size="64" class="profile-avatar">
              {{ (authStore.user?.realName || authStore.user?.username || '?').charAt(0) }}
            </el-avatar>
            <div class="profile-meta">
              <div class="profile-name">{{ authStore.user?.realName || authStore.user?.username }}</div>
              <div class="profile-sub">
                用户名：{{ authStore.user?.username }}
                <el-tag size="small" style="margin-left: 8px">{{ ROLE[authStore.role] || authStore.role }}</el-tag>
              </div>
            </div>
          </div>

          <el-divider />

          <el-tabs v-model="activeTab">
            <!-- 修改资料 -->
            <el-tab-pane label="修改资料" name="profile">
              <el-form
                ref="profileRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="90px"
                v-loading="profileLoading"
                style="max-width: 440px; margin-top: 10px"
              >
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="profileForm.realName" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="选填" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="profileLoading" @click="saveProfile">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 修改密码 -->
            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="pwdRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="90px"
                style="max-width: 440px; margin-top: 10px"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwdLoading" @click="savePassword">确认修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-avatar {
  background-color: #26b6a8;
  color: #fff;
  font-size: 26px;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.profile-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

/* ---------- 移动端（≤768px）：头卡收紧 ---------- */
@media (max-width: 768px) {
  .profile-header {
    gap: 14px;
  }

  .profile-name {
    font-size: 16px;
  }
}
</style>
