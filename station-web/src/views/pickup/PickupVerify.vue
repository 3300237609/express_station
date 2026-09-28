<script setup>
import { ref, reactive } from 'vue'
import { verifyPickupCode, confirmPickup } from '@/api/pickup'
import { formatOverdue } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useIsMobile } from '@/composables/useIsMobile'

/**
 * 取件码验证（驿站端取件流程）
 * 流程：输入取件码 → 验证（只读）→ 展示快递与货位 → 确认取件（出库事务）
 */
/** 移动端判定：验证结果描述列表窄屏改单列，避免 2 列挤压换行 */
const isMobile = useIsMobile()
const pickupCode = ref('')
const verifying = ref(false)
const confirming = ref(false)

/** 验证返回的快递与货位信息 */
const express = ref(null)

/** 验证取件码：不产生状态变化 */
async function handleVerify() {
  // 1. 空校验
  const code = pickupCode.value.trim()
  if (!code) {
    ElMessage.error('请输入取件码！')
    return
  }
  // 2. 调用验证接口
  verifying.value = true
  express.value = null
  try {
    const r = await verifyPickupCode(code)
    express.value = r.data
    ElMessage.success('取件码验证通过，请核对快递信息！')
  } finally {
    verifying.value = false
  }
}

/** 确认取件：后端事务完成状态更新 + 释放货位 + 生成取件记录 */
async function handleConfirm() {
  // 1. 存在性校验
  if (!express.value) {
    ElMessage.error('请先验证取件码！')
    return
  }
  // 2. 二次确认
  await ElMessageBox.confirm(
    `确定领取快递【${express.value.expressNo}】吗？确认后将完成出库，货位同步释放。`,
    '确认取件',
    { type: 'warning', confirmButtonText: '确认取件', cancelButtonText: '再想想' }
  )
  // 3. 执行出库
  confirming.value = true
  try {
    await confirmPickup({
      code: pickupCode.value.trim(),
      pickupMethod: 'CODE'
    })
    ElMessage.success('取件成功，快递已出库！')
    resetAll()
  } finally {
    confirming.value = false
  }
}

/** 重置，继续下一单 */
function resetAll() {
  pickupCode.value = ''
  express.value = null
}
</script>

<template>
  <div class="page-container">
    <el-row :gutter="16" justify="center" class="verify-row">
      <!-- 左：取件码输入 -->
      <el-col :xs="24" :md="10">
        <div class="card verify-card">
          <div class="card-title">输入取件码</div>
          <el-input
            v-model="pickupCode"
            size="large"
            maxlength="20"
            placeholder="请输入 / 扫描取件码"
            clearable
            class="code-input"
            @keyup.enter="handleVerify"
          />
          <el-button
            type="primary"
            size="large"
            style="width: 100%; margin-top: 16px"
            :loading="verifying"
            @click="handleVerify"
          >
            验证取件码
          </el-button>
          <el-button text style="width: 100%; margin-top: 8px" @click="resetAll">清空重来</el-button>

          <el-divider />
          <div class="tips">
            <div class="tips-title">取件须知</div>
            <p>1. 取件码在快递入库时自动生成，一个快递只能使用一次。</p>
            <p>2. 验证通过后请按提示的货位领取包裹。</p>
            <p>3. 领取前请核对收件人与快递单号。</p>
          </div>
        </div>
      </el-col>

      <!-- 右：验证结果 -->
      <el-col :xs="24" :md="14">
        <div class="card result-card" v-loading="verifying">
          <div class="card-title">快递信息</div>

          <el-empty v-if="!express && !verifying" description="请输入取件码完成验证" :image-size="110" />

          <template v-else-if="express">
            <el-result icon="success" title="取件码验证通过" :sub-title="`取件码：${pickupCode}`" />
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item label="快递单号">{{ express.expressNo }}</el-descriptions-item>
              <el-descriptions-item label="快递公司">{{ express.companyName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="收件人">{{ express.receiverName }}</el-descriptions-item>
              <el-descriptions-item label="收件手机号">{{ express.receiverPhone }}</el-descriptions-item>
              <el-descriptions-item label="货位编号">
                <el-tag type="warning" size="large">{{ express.locationNo || '-' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="所属货架">{{ express.shelfNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="入库时间">{{ express.storageTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="存放时长">{{ formatOverdue(express.storageTime) }}</el-descriptions-item>
            </el-descriptions>

            <div style="margin-top: 18px; text-align: center">
              <el-button
                type="success"
                size="large"
                :icon="'CircleCheck'"
                :loading="confirming"
                @click="handleConfirm"
              >
                确认取件（完成出库）
              </el-button>
            </div>
          </template>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.verify-card,
.result-card {
  min-height: 420px;
}

/* 窄屏两卡上下堆叠时补纵向行距（gutter 只给横向间距） */
.verify-row {
  row-gap: 16px;
}

.card-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.code-input :deep(.el-input__inner) {
  font-size: 22px;
  letter-spacing: 3px;
  text-align: center;
}

.tips-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.tips p {
  margin: 4px 0;
  font-size: 12px;
  color: #909399;
}
</style>
