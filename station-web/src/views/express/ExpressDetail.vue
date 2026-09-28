<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { detailExpress, listExpressFlow } from '@/api/express'
import { EXPRESS_STATUS, SIZE_TYPE, labelOf } from '@/constants/dict'

/**
 * 快递详情：基础信息 + 存储/取件信息 + 流转记录时间线
 */
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref({})
const flows = ref([])

/** 加载详情与流转记录 */
async function loadDetail() {
  loading.value = true
  try {
    const id = route.params.id
    const [d, f] = await Promise.all([detailExpress(id), listExpressFlow(id)])
    detail.value = d.data || {}
    flows.value = f.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- 顶部返回条 -->
    <div class="card" style="display: flex; align-items: center; justify-content: space-between">
      <div>
        <el-button :icon="'ArrowLeft'" text @click="router.back()">返回</el-button>
        <span class="express-no">{{ detail.expressNo || '-' }}</span>
        <dict-tag type="EXPRESS_STATUS" :code="detail.status" style="margin-left: 10px" />
        <el-tag v-if="detail.overdueFlag === 1" type="danger" style="margin-left: 6px">已超期</el-tag>
      </div>
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 基础信息 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-title">基础信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="快递单号">{{ detail.expressNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="快递公司">{{ detail.companyName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="寄件人">
              {{ detail.senderName || '-' }} {{ detail.senderPhone || '' }}
            </el-descriptions-item>
            <el-descriptions-item label="收件人">{{ detail.receiverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收件手机号">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="尺寸类型">
              {{ labelOf(SIZE_TYPE, detail.sizeType) }}
            </el-descriptions-item>
            <el-descriptions-item label="重量">
              {{ detail.weight != null ? `${detail.weight} kg` : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>

      <!-- 存储/取件信息 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-title">存储与取件</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="取件码">
              <el-tag v-if="detail.pickupCode" type="warning" effect="dark" size="large">
                {{ detail.pickupCode }}
              </el-tag>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="货位">{{ detail.locationNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属货架">{{ detail.shelfNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="到达时间">{{ detail.arrivalTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入库时间">{{ detail.storageTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="取件时间">{{ detail.pickupTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收件用户">
              {{ detail.receiverUserId ? `已关联（用户ID：${detail.receiverUserId}）` : '未关联' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
    </el-row>

    <!-- 流转记录 -->
    <div class="card" style="margin-top: 16px">
      <div class="card-title">流转记录</div>
      <el-empty v-if="flows.length === 0 && !loading" description="暂无流转记录" :image-size="80" />
      <el-timeline v-else>
        <!-- 后端节点 { time, event, detail } 按时间正序返回，高亮最新一条 -->
        <el-timeline-item
          v-for="(flow, index) in flows"
          :key="index"
          :timestamp="flow.time"
          :type="index === flows.length - 1 ? 'primary' : undefined"
          placement="top"
        >
          <div class="flow-title">{{ flow.event }}</div>
          <div class="flow-sub" v-if="flow.detail">详情：{{ flow.detail }}</div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<style scoped>
.card-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.express-no {
  margin-left: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.flow-title {
  font-size: 14px;
  color: #303133;
}

.flow-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
