<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listOperationLog } from '@/api/log'
import { LOG_RESULT_OPTIONS } from '@/constants/dict'

/**
 * 操作日志（管理员）：关键操作审计查询，只读不可编辑
 */
const loading = ref(false)
const list = ref([])
const total = ref(0)

/** 查询条件 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  module: '',
  operation: '',
  result: ''
})

/** 加载列表 */
async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => params[k] === '' && delete params[k])
    const r = await listOperationLog(params)
    list.value = r.data || []
    total.value = Number(r.map?.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { keyword: '', module: '', operation: '', result: '', pageNum: 1 })
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <div class="card filter-bar">
      <el-input v-model="query.keyword" placeholder="操作人/描述" clearable @keyup.enter="handleSearch" />
      <el-input v-model="query.module" placeholder="操作模块" clearable @keyup.enter="handleSearch" />
      <el-input v-model="query.operation" placeholder="操作类型" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.result" placeholder="操作结果" clearable style="width: 130px">
        <el-option v-for="opt in LOG_RESULT_OPTIONS" :key="opt.value" v-bind="opt" />
      </el-select>
      <el-button type="primary" :icon="'Search'" @click="handleSearch">搜索</el-button>
      <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人" width="110" show-overflow-tooltip />
        <el-table-column prop="roleCode" label="角色" width="90" align="center">
          <template #default="{ row }">{{ row.roleCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="120" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作类型" width="110" show-overflow-tooltip />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="方法" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.requestMethod || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.requestUrl || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <dict-tag type="LOG_RESULT" :code="row.result" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" />
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
  </div>
</template>
