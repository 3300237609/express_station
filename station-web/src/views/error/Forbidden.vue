<script setup>
import { useRouter } from 'vue-router'

/**
 * 403 无访问权限页
 */
const router = useRouter()
</script>

<template>
  <div class="error-page">
    <el-result icon="warning" title="403 无访问权限" sub-title="当前角色没有访问该功能的权限，请联系管理员！">
      <template #extra>
        <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f5f7fa;
}
</style>
