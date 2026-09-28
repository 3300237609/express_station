<script setup>
/**
 * 统计卡片：首页/统计页顶部指标展示
 */
defineProps({
  /** 指标名称 */
  title: { type: String, required: true },
  /** 指标值 */
  value: { type: [String, Number], default: 0 },
  /** 单位（如 %、件） */
  unit: { type: String, default: '' },
  /** Element Plus 图标名 */
  icon: { type: String, default: 'DataLine' },
  /** 主题色 */
  color: { type: String, default: '#26b6a8' }
})
</script>

<template>
  <div class="stat-card card">
    <div class="stat-info">
      <div class="stat-title">{{ title }}</div>
      <div class="stat-value" :style="{ color }">
        {{ value }}<span class="stat-unit">{{ unit }}</span>
      </div>
    </div>
    <div class="stat-icon" :style="{ backgroundColor: color + '1a', color }">
      <el-icon :size="26"><component :is="icon" /></el-icon>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.stat-title {
  margin-bottom: 8px;
  color: #909399;
  font-size: 13px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
}

.stat-unit {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 400;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 12px;
}

/* ---------- 移动端（≤768px）：卡片 2×2 排布时收紧内距与字号，防止大数值顶到图标 ---------- */
@media (max-width: 768px) {
  .stat-card {
    padding: 14px;
  }

  .stat-title {
    margin-bottom: 6px;
    font-size: 12px;
  }

  .stat-value {
    font-size: 20px;
  }

  .stat-icon {
    width: 42px;
    height: 42px;
    border-radius: 10px;
  }
}
</style>
