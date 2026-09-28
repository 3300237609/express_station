<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { inboundAdd } from '@/api/inbound'
import { recommendLocation } from '@/api/shelf'
import { listCompanyOptions } from '@/api/company'
import { matchUserByPhone } from '@/api/common'
import { SIZE_TYPE_OPTIONS, labelOf, SIZE_TYPE } from '@/constants/dict'
import { ElMessage } from 'element-plus'

/**
 * 快递入库（管理员/快递员，/inbound/add 事务一步完成创建 + 绑定货位）
 * 流程：录入快递信息 → 智能推荐货位 → 确认入库 → 返回取件码与货位
 * 收件用户匹配：收件手机号失焦调 /user/matchByPhone（登录即可），命中自动填收件人姓名与 receiverUserId，
 * 未命中按普通收件登记（receiverUserId 置 null）
 */
const router = useRouter()
const loading = ref(false)
const companyOptions = ref([])

/** 录入表单 */
const form = reactive({
  expressNo: '',
  companyId: '',
  senderName: '',
  senderPhone: '',
  receiverName: '',
  receiverPhone: '',
  receiverUserId: null,
  sizeType: 'MEDIUM',
  weight: null,
  arrivalTime: '',
  remark: ''
})

const rules = {
  expressNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  receiverName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入收件人手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确！', trigger: 'blur' }
  ]
}

const formRef = ref()

// ==================== 收件用户自动匹配 ====================
/** 匹配状态：idle=未匹配 / matched=已命中 / none=未命中系统账号 / invalid=手机号格式不正确 */
const matchState = ref('idle')
/** 上次匹配命中的账号姓名与手机号（防重复请求、防改号后旧关联残留） */
const matchedName = ref('')
const matchedPhone = ref('')

/** 匹配提示（按状态给文案与配色） */
const matchTip = computed(() => {
  if (matchState.value === 'matched') {
    return { text: `✓ 已匹配系统账号：${matchedName.value}，入库后自动关联其「我的快递」`, cls: 'is-ok' }
  }
  if (matchState.value === 'none') return { text: '未匹配到系统账号，按普通收件登记', cls: 'is-warn' }
  if (matchState.value === 'invalid') return { text: '手机号格式不正确，暂不匹配', cls: 'is-err' }
  return { text: '输入 11 位手机号失焦后自动匹配系统账号，命中将自动填入收件人', cls: '' }
})

/** 手机号变更即失效旧关联（防止带着旧 receiverUserId 提交） */
function handlePhoneInput() {
  if (form.receiverPhone !== matchedPhone.value) {
    form.receiverUserId = null
    matchState.value = 'idle'
    matchedName.value = ''
  }
}

/** 收件手机号失焦：调 /user/matchByPhone 关联系统账号（命中填姓名与用户ID，未命中按普通收件登记） */
async function handleMatchUser() {
  const phone = (form.receiverPhone || '').trim()
  // 1. 空或格式不合法：不发请求，清关联
  if (!phone) {
    form.receiverUserId = null
    matchState.value = 'idle'
    matchedName.value = ''
    matchedPhone.value = ''
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    form.receiverUserId = null
    matchState.value = 'invalid'
    matchedName.value = ''
    matchedPhone.value = ''
    return
  }
  // 2. 已命中同号：跳过重复请求，避免覆盖手工改过的收件人姓名
  if (matchState.value === 'matched' && matchedPhone.value === phone) return
  // 3. 调接口匹配（接口异常不阻断登记，保持未关联）
  try {
    const r = await matchUserByPhone(phone)
    const u = r.data
    if (u && u.id) {
      form.receiverUserId = u.id
      form.receiverName = u.realName || u.username || form.receiverName
      matchedName.value = u.realName || u.username || phone
      matchedPhone.value = phone
      matchState.value = 'matched'
    } else {
      form.receiverUserId = null
      matchedName.value = ''
      matchedPhone.value = phone
      matchState.value = 'none'
    }
  } catch (e) {
    form.receiverUserId = null
    matchState.value = 'idle'
    matchedName.value = ''
  }
}

// ==================== 智能货位推荐 ====================
const recommended = ref([])
const recommendLoading = ref(false)
const selectedLocationId = ref(null)
/** 是否已确认第一步快递信息（确认后锁定表单并加载推荐） */
const formDone = ref(false)

/** 步骤 1：确认快递信息（本地校验通过后锁定表单，进入货位推荐） */
async function handleSave() {
  // 1. 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 锁定表单并加载智能推荐（推荐不依赖后端先建单）
  formDone.value = true
  await handleRecommend()
}

/** 智能货位推荐（按尺寸与重量打分，评分降序） */
async function handleRecommend() {
  recommendLoading.value = true
  try {
    const r = await recommendLocation({
      sizeType: form.sizeType,
      weight: form.weight ?? undefined
    })
    recommended.value = r.data || []
    selectedLocationId.value = recommended.value[0]?.locationId ?? null
  } finally {
    recommendLoading.value = false
  }
}

/** 返回修改第一步（未入库前可调整快递信息） */
function backToForm() {
  formDone.value = false
  recommended.value = []
  selectedLocationId.value = null
}

/** 步骤 2：确认货位并完成入库（事务：建单 + 绑定货位 + 生成取件码 + 状态置待取件） */
const resultVisible = ref(false)
const result = ref(null)

async function handleStore() {
  // 1. 空校验
  if (!selectedLocationId.value) {
    ElMessage.error('请选择货位！')
    return
  }
  // 2. 执行一步入库
  loading.value = true
  try {
    // 可选字段为空时不上送空串，避免后端 LocalDateTime 反序列化报错
    const payload = { ...form, locationId: selectedLocationId.value }
    if (!payload.arrivalTime) delete payload.arrivalTime
    const r = await inboundAdd(payload)
    result.value = r.data
    resultVisible.value = true
  } finally {
    loading.value = false
  }
}

/** 入库完成：可继续录入下一单 */
function resetAll() {
  resultVisible.value = false
  formDone.value = false
  recommended.value = []
  selectedLocationId.value = null
  matchState.value = 'idle'
  matchedName.value = ''
  matchedPhone.value = ''
  formRef.value?.resetFields()
}

onMounted(async () => {
  // 默认到达时间：当前时间（ISO 格式，与后端 LocalDateTime 入参一致）
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  form.arrivalTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  try {
    const r = await listCompanyOptions()
    companyOptions.value = r.data || []
  } catch (e) {
    companyOptions.value = []
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 入库步骤 -->
    <el-steps :active="result ? 2 : formDone ? 1 : 0" finish-status="success" simple style="margin-bottom: 16px">
      <el-step title="录入快递信息" />
      <el-step title="智能推荐货位" />
      <el-step title="完成入库" />
    </el-steps>

    <el-row :gutter="16">
      <!-- 左：录入表单 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-title">快递信息录入</div>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="formDone">
            <el-form-item label="快递单号" prop="expressNo">
              <el-input v-model="form.expressNo" placeholder="扫描或输入快递单号" clearable />
            </el-form-item>
            <el-form-item label="快递公司" prop="companyId">
              <el-select v-model="form.companyId" placeholder="请选择快递公司" style="width: 100%">
                <el-option
                  v-for="opt in companyOptions"
                  :key="opt.id"
                  :label="opt.companyName"
                  :value="opt.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="寄件人">
              <el-col :span="12">
                <el-input v-model="form.senderName" placeholder="姓名（选填）" />
              </el-col>
              <el-col :span="12">
                <el-input v-model="form.senderPhone" placeholder="电话（选填）" />
              </el-col>
            </el-form-item>
            <el-form-item label="收件人" prop="receiverName">
              <el-input v-model="form.receiverName" placeholder="收件人姓名" />
            </el-form-item>
            <el-form-item label="收件手机号" prop="receiverPhone">
              <el-input
                v-model="form.receiverPhone"
                placeholder="收件人手机号"
                clearable
                @input="handlePhoneInput"
                @blur="handleMatchUser"
              />
              <div class="match-tip" :class="matchTip.cls">{{ matchTip.text }}</div>
            </el-form-item>
            <el-form-item label="尺寸类型">
              <el-radio-group v-model="form.sizeType">
                <el-radio-button
                  v-for="opt in SIZE_TYPE_OPTIONS"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="重量(kg)">
              <el-input-number v-model="form.weight" :min="0" :precision="2" :max="999" />
            </el-form-item>
            <el-form-item label="到达时间">
              <el-date-picker
                v-model="form.arrivalTime"
                type="datetime"
                placeholder="选择到达时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">
                下一步：推荐货位
              </el-button>
              <el-button @click="router.back()">返回</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <!-- 右：货位推荐 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-title">
            智能货位推荐
            <template v-if="formDone && !result">
              <el-button
                size="small"
                :icon="'Refresh'"
                style="margin-left: 10px"
                :loading="recommendLoading"
                @click="handleRecommend"
              >
                重新推荐
              </el-button>
              <el-button size="small" style="margin-left: 8px" @click="backToForm">修改信息</el-button>
            </template>
          </div>

          <el-empty
            v-if="!formDone"
            description="请在左侧录入快递信息并点击「下一步」，系统将按容量与空闲程度智能推荐货位"
            :image-size="100"
          />

          <div v-else v-loading="recommendLoading">
            <el-alert
              v-if="recommended.length === 0 && !recommendLoading"
              title="暂无可用货位，请先在货位管理中维护货架与货位！"
              type="warning"
              :closable="false"
              style="margin-bottom: 14px"
            />

            <!-- 推荐列表：固定高度滑动窗口，长列表仅在窗口内滚动，不再撑高页面（选靠后的货位无需划到页底） -->
            <div class="loc-list">
              <div
                v-for="(loc, index) in recommended"
                :key="loc.locationId"
                class="location-option"
                :class="{ active: selectedLocationId === loc.locationId }"
                @click="selectedLocationId = loc.locationId"
              >
                <div class="loc-main">
                  <el-tag v-if="index === 0" type="success" size="small" style="margin-right: 8px">推荐</el-tag>
                  <span class="loc-no">{{ loc.locationNo }}</span>
                  <span class="loc-sub">{{ loc.shelfNo }} · {{ loc.area }}</span>
                  <div v-if="loc.reason" class="loc-reason">{{ loc.reason }}</div>
                </div>
                <div class="loc-score">
                  匹配度 {{ loc.score ?? '-' }}
                  <div class="loc-space">剩余容量 {{ loc.remaining ?? '-' }}</div>
                </div>
              </div>
            </div>

            <el-button
              v-if="!result"
              type="primary"
              style="width: 100%; margin-top: 14px"
              :disabled="!selectedLocationId"
              :loading="loading"
              @click="handleStore"
            >
              确认货位并完成入库
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 入库成功弹窗 -->
    <el-dialog v-model="resultVisible" title="入库成功" width="420px" :close-on-click-modal="false">
      <el-result icon="success" title="快递入库成功" :sub-title="`快递单号：${result?.expressNo || '-'}`">
        <template #extra>
          <div class="pickup-code-box">
            <div class="pickup-code-label">取件码</div>
            <div class="pickup-code-value">{{ result?.pickupCode || '-' }}</div>
            <div class="pickup-code-sub">
              货位：{{ result?.locationNo || '-' }}（请凭取件码取件）
            </div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button @click="router.push('/express/list')">查看快递列表</el-button>
        <el-button type="primary" @click="resetAll">继续入库下一单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.match-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.match-tip.is-ok {
  color: #67c23a;
}

.match-tip.is-warn {
  color: #e6a23c;
}

.match-tip.is-err {
  color: #f56c6c;
}

/* 推荐货位滑动窗口：上限 60vh，超出时窗口内部滚动，列表不满时自然高度不出现滚动条 */
.loc-list {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 6px;
}

.location-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.location-option:hover {
  border-color: #a0cfff;
}

.location-option.active {
  background-color: #ecf5ff;
  border-color: #26b6a8;
}

.loc-no {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.loc-sub {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.loc-reason {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.loc-score {
  font-size: 13px;
  color: #26b6a8;
  text-align: right;
}

.loc-space {
  font-size: 12px;
  color: #909399;
}

.pickup-code-box {
  text-align: center;
}

.pickup-code-label {
  font-size: 13px;
  color: #909399;
}

.pickup-code-value {
  margin: 6px 0;
  font-size: 40px;
  font-weight: 700;
  letter-spacing: 6px;
  color: #e6a23c;
}

.pickup-code-sub {
  font-size: 13px;
  color: #606266;
}
</style>
