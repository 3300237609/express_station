<script setup>
import { useRouter } from 'vue-router'

/**
 * 404 页面不存在
 */
const router = useRouter()
</script>

<template>
  <div class="error-page">
    <el-result icon="info" title="404 页面不存在" sub-title="您访问的页面不存在或已被移除！">
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
