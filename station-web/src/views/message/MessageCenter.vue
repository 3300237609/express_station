<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  listNotification,
  listNotificationManage,
  getUnreadCount,
  readNotification,
  readAllNotification,
  publishNotice
} from '@/api/notification'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { NOTIFICATION_TYPE_OPTIONS } from '@/constants/dict'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 消息中心：站内消息查看、标记已读；管理员可发布系统公告、切「全部记录」查所有人的消息
 * activeTab：all 全部消息 / unread 未读消息（均走 /notification/list 本人范围）；
 *            manage 全部记录（走 /notification/manage，仅管理员，不可标记已读）
 */
const appStore = useAppStore()
const authStore = useAuthStore()
const isAdmin = authStore.role === 'ADMIN'

const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 查询条件：activeTab 为 unread 时只查未读 */
const query = reactive({ pageNum: 1, pageSize: 10, isRead: '' })
const activeTab = ref('all')

/** 「全部记录」页签专属筛选（仅管理员） */
const manage = reactive({ userId: '', type: '' })

/** 切换页签 */
function handleTabChange(tab) {
  query.pageNum = 1
  if (tab === 'manage') {
    // 全部记录按管理员条件查询，不带本人已读状态
    query.isRead = ''
  } else {
    query.isRead = tab === 'unread' ? 0 : ''
    // 离开全部记录时清掉管理端筛选，避免残留影响本人视图
    manage.userId = ''
    manage.type = ''
  }
  loadList()
}

/** 加载消息列表（按页签选择本人消息或全部记录接口） */
async function loadList() {
  loading.value = true
  try {
    const isManage = activeTab.value === 'manage'
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (isManage) {
      if (manage.userId !== '') params.userId = manage.userId
      if (manage.type !== '') params.type = manage.type
      const r = await listNotificationManage(params)
      list.value = r.data || []
      total.value = Number(r.map?.total || 0)
    } else {
      if (query.isRead !== '') params.isRead = query.isRead
      const r = await listNotification(params)
      list.value = r.data || []
      total.value = Number(r.map?.total || 0)
    }
    // 同步顶栏未读数
    const c = await getUnreadCount()
    appStore.setUnreadCount(Number(c.data || 0))
  } finally {
    loading.value = false
  }
}

/** 「全部记录」筛选搜索 */
function handleManageSearch() {
  query.pageNum = 1
  loadList()
}

/** 标记单条已读（仅本人消息；全部记录里是所有人的消息，不做已读操作） */
async function handleRead(row) {
  if (activeTab.value === 'manage') return
  if (row.isRead === 1) return
  await readNotification(row.id)
  row.isRead = 1
  appStore.setUnreadCount(Math.max(appStore.unreadCount - 1, 0))
}

/** 全部标记已读 */
async function handleReadAll() {
  await ElMessageBox.confirm('确定将全部消息标记为已读吗？', '全部已读', {
    type: 'info',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await readAllNotification()
  ElMessage.success('已全部标记为已读！')
  loadList()
}

// ==================== 发布公告（管理员） ====================
const publishVisible = ref(false)
const publishLoading = ref(false)

const publishForm = reactive({ title: '', content: '' })

const publishRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const publishRef = ref()

/** 提交公告（广播给全体用户） */
async function submitPublish() {
  // 1. 表单校验
  const valid = await publishRef.value.validate().catch(() => false)
  if (!valid) return
  // 2. 发布
  publishLoading.value = true
  try {
    await publishNotice({ ...publishForm })
    ElMessage.success('公告发布成功！')
    publishVisible.value = false
    publishForm.title = ''
    publishForm.content = ''
    loadList()
  } finally {
    publishLoading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <div class="card">
      <!-- 页签与操作 -->
      <div class="msg-header">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="全部消息" name="all" />
          <el-tab-pane :label="`未读消息（${appStore.unreadCount}）`" name="unread" />
          <el-tab-pane v-if="isAdmin" label="全部记录" name="manage" />
        </el-tabs>
        <div class="msg-actions">
          <el-button v-if="activeTab !== 'manage'" :icon="'Finished'" @click="handleReadAll">
            全部标记已读
          </el-button>
          <el-button v-if="isAdmin" type="primary" :icon="'Promotion'" @click="publishVisible = true">
            发布公告
          </el-button>
        </div>
      </div>

      <!-- 全部记录筛选（仅管理员页签） -->
      <div v-if="activeTab === 'manage'" class="manage-filter">
        <el-input
          v-model="manage.userId"
          placeholder="接收用户ID"
          clearable
          style="width: 170px"
          @keyup.enter="handleManageSearch"
        />
        <el-select v-model="manage.type" placeholder="消息类型" clearable style="width: 170px">
          <el-option v-for="opt in NOTIFICATION_TYPE_OPTIONS" :key="opt.value" v-bind="opt" />
        </el-select>
        <el-button type="primary" :icon="'Search'" @click="handleManageSearch">搜索</el-button>
      </div>

      <!-- 消息列表 -->
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @row-click="handleRead"
        :style="{ cursor: activeTab === 'manage' ? 'default' : 'pointer' }"
      >
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <el-badge :is-dot="row.isRead !== 1" :offset="[0, 6]">
              <el-icon :color="row.isRead === 1 ? '#c0c4cc' : '#26b6a8'" :size="16"><Bell /></el-icon>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="140" align="center">
          <template #default="{ row }">
            <dict-tag type="NOTIFICATION_TYPE" :code="row.type" />
          </template>
        </el-table-column>
        <!-- 全部记录为管理员审计视角：多显示接收用户与关联单号 -->
        <el-table-column v-if="activeTab === 'manage'" prop="userId" label="接收用户ID" width="110" align="center">
          <template #default="{ row }">{{ row.userId ?? '-' }}</template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'manage'" prop="expressNo" label="关联单号" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.expressNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span :style="{ fontWeight: row.isRead !== 1 ? 600 : 400 }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'info' : 'primary'" size="small">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(p) => ((query.pageNum = p), loadList())"
          @size-change="(s) => ((query.pageSize = s), (query.pageNum = 1), loadList())"
        />
      </div>
    </div>

    <!-- 发布公告弹窗 -->
    <el-dialog v-model="publishVisible" title="发布系统公告" width="520px">
      <el-form ref="publishRef" :model="publishForm" :rules="publishRules" label-width="80px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="publishForm.title" maxlength="200" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="publishForm.content" type="textarea" :rows="6" maxlength="1000" placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.manage-filter {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

/* ---------- 移动端（≤768px）：页签与操作纵向堆叠，避免窄屏挤压 ---------- */
@media (max-width: 768px) {
  .msg-header {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }

  .msg-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  /* 换行后按钮用 gap 控距，去掉 EP 相邻按钮的左外边距 */
  .msg-actions .el-button + .el-button {
    margin-left: 0;
  }

  /* 全部记录筛选：允许换行、输入框弹性占位 */
  .manage-filter {
    flex-wrap: wrap;
  }

  .manage-filter .el-input,
  .manage-filter .el-select {
    flex: 1 1 140px;
  }
}
</style>
