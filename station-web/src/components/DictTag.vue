<script setup>
import { labelOf, tagTypeOf } from '@/constants/dict'
import { EXPRESS_STATUS, HANDLE_STATUS, LOCATION_STATUS, LOG_RESULT, NOTIFICATION_TYPE, USER_STATUS } from '@/constants/dict'
import { computed } from 'vue'

/**
 * 字典标签组件：按字典名与状态码渲染 el-tag
 * 用法：<dict-tag type="EXPRESS_STATUS" :code="row.status" />
 */
const props = defineProps({
  /** 字典名称（见 constants/dict.js 中导出的字典常量名） */
  type: { type: String, required: true },
  /** 状态码 */
  code: { type: [String, Number], default: '' }
})

/** 字典注册表：新增字典在此登记即可全局使用 */
const DICT_MAP = {
  EXPRESS_STATUS,
  HANDLE_STATUS,
  LOCATION_STATUS,
  LOG_RESULT,
  NOTIFICATION_TYPE,
  USER_STATUS
}

const dict = computed(() => DICT_MAP[props.type] || {})
const label = computed(() => labelOf(dict.value, props.code))
const tagType = computed(() => tagTypeOf(dict.value, props.code))
</script>

<template>
  <el-tag :type="tagType" disable-transitions>{{ label }}</el-tag>
</template>
