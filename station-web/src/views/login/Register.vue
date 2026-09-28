<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'

/**
 * 注册页：普通用户自助注册账号
 * 布局为左右分栏：表单区在左、品牌区在右（AuthLayout 的 form-left）
 */
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

/** 两次密码一致性校验 */
const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致！'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确！', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确！', trigger: 'blur' }]
}

const formRef = ref()

/** 注册：成功后提示并返回登录页 */
async function handleRegister() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 提交注册（confirmPassword 必须随包提交，后端校验两次密码一致）
  loading.value = true
  try {
    await authStore.register({ ...form })
    ElMessage.success('注册成功，请登录！')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout form-left>
    <h2 class="auth-title">创建账号</h2>
    <p class="auth-subtitle">注册新账号开始使用系统</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入密码"
        />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="用于关联收件快递" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" />
      </el-form-item>
      <el-button type="primary" class="auth-btn" size="large" :loading="loading" @click="handleRegister">
        注 册
      </el-button>
    </el-form>

    <p class="auth-switch">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </p>
  </AuthLayout>
</template>
