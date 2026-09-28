<script setup>
/**
 * 登录/注册页布局：左右分栏
 * - 品牌介绍区：浅色渐变背景 + Logo/系统名/特性列表
 * - 表单区：白底，左上角「返回首页」，内容垂直水平居中，经默认插槽传入
 * 插槽内容约定类名（样式统一在此维护，页面无需再写样式）：
 *   auth-title / auth-subtitle（表单标题区）、remember-row（记住密码行）、auth-btn（主按钮）、auth-switch（底部切换链接）
 */
defineProps({
  /** 表单区放左侧（注册页用）；默认在右侧（登录页用） */
  formLeft: { type: Boolean, default: false }
})
</script>

<template>
  <div class="auth-page" :class="{ 'form-left': formLeft }">
    <!-- 品牌介绍区 -->
    <div class="auth-brand">
      <div class="brand-box">
        <div class="brand-logo">
          <el-icon :size="34"><Van /></el-icon>
        </div>
        <h1>快递末端智能驿站系统</h1>
        <p class="brand-slogan">一站式末端驿站管理平台</p>
        <span class="brand-divider" />
        <div class="brand-desc">
          <p>高效收发快递，提升驿站运营效率</p>
          <p>专为驿站管理员、快递员与用户打造的现代化快递管理平台</p>
        </div>
        <ul class="brand-features">
          <li>快递入库与出库管理</li>
          <li>超期提醒与异常件处理</li>
          <li>取件码核验与业务统计</li>
        </ul>
      </div>
    </div>

    <!-- 表单区 -->
    <div class="auth-panel">
      <router-link class="auth-back" to="/">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回首页</span>
      </router-link>
      <div class="auth-body">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 分栏容器：登录（品牌左/表单右），注册翻转（表单左/品牌右） */
.auth-page {
  display: flex;
  min-height: 100vh;
}

.auth-page.form-left {
  flex-direction: row-reverse;
}

/* ---------- 品牌介绍区 ---------- */
.auth-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 255, 255, 0.95), transparent 45%),
    linear-gradient(160deg, #e8f7f4 0%, #f1fbf9 45%, #e2f4f1 100%);
}

.brand-box {
  max-width: 460px;
  text-align: center;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76px;
  height: 76px;
  margin: 0 auto 26px;
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 10px 24px rgba(38, 182, 168, 0.18);
  color: #26b6a8;
}

.auth-brand h1 {
  margin: 0;
  font-size: 34px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: 1px;
  color: #1f2733;
}

.brand-slogan {
  margin: 10px 0 0;
  font-size: 19px;
  font-weight: 700;
  color: #26b6a8;
}

.brand-divider {
  display: block;
  width: 52px;
  height: 3px;
  margin: 26px auto;
  background: #26b6a8;
  border-radius: 2px;
}

.brand-desc p {
  margin: 0 0 10px;
  font-size: 15px;
  line-height: 1.7;
  color: #6b7280;
}

.brand-features {
  display: inline-block;
  margin: 26px 0 0;
  padding: 0;
  text-align: left;
  list-style: none;
}

.brand-features li {
  position: relative;
  padding: 7px 0 7px 26px;
  font-size: 15px;
  color: #374151;
}

.brand-features li::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #26b6a8;
  transform: translateY(-50%);
  content: '';
}

/* ---------- 表单区 ---------- */
.auth-panel {
  position: relative;
  display: flex;
  flex: 1;
  padding: 72px 32px 48px;
  background: #fff;
  overflow-y: auto;
}

/* margin: auto 实现居中且内容过高时不裁切顶部 */
.auth-body {
  width: 420px;
  max-width: 100%;
  margin: auto;
}

.auth-back {
  position: absolute;
  top: 26px;
  left: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #6b7280;
  text-decoration: none;
}

.auth-back:hover {
  color: #26b6a8;
}

/* ---------- 插槽内容（标题/表单/按钮/底部链接） ---------- */
.auth-body :deep(.auth-title) {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  text-align: center;
  color: #1f2733;
}

.auth-body :deep(.auth-subtitle) {
  margin: 10px 0 34px;
  font-size: 15px;
  text-align: center;
  color: #9ca3af;
}

/* 标签居中显示在输入框上方 */
.auth-body :deep(.el-form-item__label) {
  display: flex;
  width: 100%;
  height: auto;
  margin-bottom: 8px;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  color: #303133;
}

.auth-body :deep(.el-form-item) {
  margin-bottom: 22px;
}

/* 灰底圆角输入框 */
.auth-body :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 5px 16px;
  background: #f5f7fa;
  border-radius: 12px;
  box-shadow: none;
}

.auth-body :deep(.el-input__wrapper:hover) {
  background: #eef1f5;
}

.auth-body :deep(.el-input__wrapper.is-focus) {
  background: #f5f7fa;
  box-shadow: inset 0 0 0 2px rgba(38, 182, 168, 0.45);
}

/* 记住密码行 */
.auth-body :deep(.remember-row) {
  display: flex;
  align-items: center;
  margin: -4px 0 14px;
}

.auth-body :deep(.remember-row .el-checkbox__label) {
  font-size: 13px;
  color: #6b7280;
}

/* 主按钮：青绿大圆角 */
.auth-body :deep(.el-button.auth-btn) {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  border: none;
  border-radius: 12px;
  background: #26b6a8;
  font-size: 16px;
  font-weight: 600;
}

.auth-body :deep(.el-button.auth-btn:hover) {
  border: none;
  background: #1fa89b;
}

.auth-body :deep(.el-button.auth-btn:focus-visible) {
  outline: 2px solid rgba(38, 182, 168, 0.5);
  outline-offset: 2px;
}

/* 底部 登录/注册 切换链接 */
.auth-body :deep(.auth-switch) {
  margin-top: 22px;
  font-size: 14px;
  text-align: center;
  color: #9ca3af;
}

.auth-body :deep(.auth-switch a) {
  font-weight: 600;
  color: #26b6a8;
  text-decoration: none;
}

.auth-body :deep(.auth-switch a:hover) {
  text-decoration: underline;
}

/* 窄屏隐藏品牌区，表单占满整行 */
@media (max-width: 960px) {
  .auth-brand {
    display: none;
  }
}
</style>
