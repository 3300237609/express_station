<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'

/**
 * 登录页：账号密码登录，成功后按角色进入对应功能界面
 * 布局为左右分栏：品牌区在左、表单区在右（AuthLayout）
 */
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)

/** 记住密码的本地存储键（存账号密码用于下次预填，与登录态键 station_user 分开） */
const REMEMBER_KEY = 'station_remember'

/** 记住密码有效期：7 天，到期即失效，进入登录页时给出失效提示 */
const REMEMBER_EXPIRE_MS = 7 * 24 * 60 * 60 * 1000

const form = reactive({
  username: '',
  password: ''
})

/** 是否记住密码 */
const remember = ref(false)

/** 记住的密码是否已过期失效（过期后不回填密码，仅在勾选框旁提示） */
const rememberExpired = ref(false)

/**
 * 进入登录页时回填已记住的账号密码
 * 已过期（或无有效期字段的旧数据）视为失效：清除凭据、仅回填用户名并显示失效提示
 * 数据损坏时静默清理，不影响登录页使用
 */
function loadRemembered() {
  try {
    const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY) || 'null')
    if (!saved?.username) return
    if (!saved.expires || saved.expires <= Date.now()) {
      // 失效：删掉旧凭据，用户名仍回填方便重输密码，勾选框旁提示已失效
      localStorage.removeItem(REMEMBER_KEY)
      rememberExpired.value = true
      form.username = saved.username
      return
    }
    form.username = saved.username
    form.password = saved.password || ''
    remember.value = true
  } catch (e) {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

loadRemembered()

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 32, message: '密码长度为 4-32 位', trigger: 'blur' }
  ]
}

const formRef = ref()

/** 登录：成功后跳转回原页面或首页 */
async function handleLogin() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 提交登录
  loading.value = true
  try {
    await authStore.login({ ...form })
    // 3. 登录成功后才处理记住密码：勾选则保存（带 7 天有效期）供下次预填，未勾选则清除
    //    （演示用的前端预填，密码存 localStorage 有泄露风险；生产环境应改由后端签发长效 remember-me Token）
    if (remember.value) {
      localStorage.setItem(
        REMEMBER_KEY,
        JSON.stringify({
          username: form.username,
          password: form.password,
          expires: Date.now() + REMEMBER_EXPIRE_MS
        })
      )
      rememberExpired.value = false
    } else {
      localStorage.removeItem(REMEMBER_KEY)
      rememberExpired.value = false
    }
    ElMessage.success('登录成功！')
    // 4. 按 redirect 参数或角色回跳
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <h2 class="auth-title">欢迎回来</h2>
    <p class="auth-subtitle">请登录您的账号继续使用</p>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @keyup.enter="handleLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="'User'" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          show-password
          :prefix-icon="'Lock'"
        />
      </el-form-item>
      <div class="remember-row">
        <el-checkbox v-model="remember">记住密码</el-checkbox>
        <span v-if="rememberExpired" class="remember-expired">记住的密码已过期失效，请重新输入</span>
      </div>
      <el-button type="primary" class="auth-btn" size="large" :loading="loading" @click="handleLogin">
        登 录
      </el-button>
    </el-form>

    <p class="auth-switch">
      还没有账号？
      <router-link to="/register">立即注册</router-link>
    </p>
  </AuthLayout>
</template>

<style scoped>
/* 记住的密码失效提示：靠右显示在勾选框旁 */
.remember-expired {
  margin-left: auto;
  font-size: 12px;
  color: #e6a23c;
}
</style>
