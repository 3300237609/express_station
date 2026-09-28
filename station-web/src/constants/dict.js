/**
 * 业务字典与状态映射（与《数据库设计》第 7/8/10/11 节保持一致）
 * 页面统一通过 labelOf / tagTypeOf 取值，禁止在视图里散写状态字面量
 */

/** 快递状态 */
export const EXPRESS_STATUS = {
  WAITING: { label: '待入库', type: 'info' },
  STORED: { label: '已入库', type: 'primary' },
  WAITING_PICKUP: { label: '待取件', type: 'warning' },
  PICKED_UP: { label: '已取件', type: 'success' },
  EXCEPTION: { label: '异常', type: 'danger' },
  RETURNED: { label: '已退回', type: 'info' }
}

/** 快递状态下拉选项 */
export const EXPRESS_STATUS_OPTIONS = Object.entries(EXPRESS_STATUS).map(([value, v]) => ({
  value,
  label: v.label
}))

/** 快递尺寸类型 */
export const SIZE_TYPE = {
  SMALL: '小件',
  MEDIUM: '中件',
  LARGE: '大件'
}

/** 尺寸类型下拉选项 */
export const SIZE_TYPE_OPTIONS = Object.entries(SIZE_TYPE).map(([value, label]) => ({ value, label }))

/** 异常类型 */
export const EXCEPTION_TYPE = {
  DAMAGE: '快递破损',
  LOST: '快递丢失',
  WRONG_ADDR: '地址错误',
  WRONG_INFO: '收件人信息错误',
  REFUSED: '用户拒收',
  OVERDUE: '快递超期',
  OTHER: '其他异常'
}

/** 异常类型下拉选项 */
export const EXCEPTION_TYPE_OPTIONS = Object.entries(EXCEPTION_TYPE).map(([value, label]) => ({
  value,
  label
}))

/** 异常处理状态 */
export const HANDLE_STATUS = {
  PENDING: { label: '待处理', type: 'warning' },
  PROCESSING: { label: '处理中', type: 'primary' },
  RESOLVED: { label: '已处理', type: 'success' },
  CLOSED: { label: '已关闭', type: 'info' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

/** 异常处理状态下拉选项 */
export const HANDLE_STATUS_OPTIONS = Object.entries(HANDLE_STATUS).map(([value, v]) => ({
  value,
  label: v.label
}))

/** 货位状态 */
export const LOCATION_STATUS = {
  1: { label: '空闲', type: 'success' },
  2: { label: '部分占用', type: 'warning' },
  3: { label: '已满', type: 'danger' },
  4: { label: '停用', type: 'info' }
}

/** 货位状态下拉选项 */
export const LOCATION_STATUS_OPTIONS = Object.entries(LOCATION_STATUS).map(([value, v]) => ({
  value: Number(value),
  label: v.label
}))

/** 消息类型 */
export const NOTIFICATION_TYPE = {
  PICKUP_NOTICE: { label: '入库/取件通知', type: 'primary' },
  OVERDUE_NOTICE: { label: '超期提醒', type: 'warning' },
  EXCEPTION_NOTICE: { label: '异常件通知', type: 'danger' },
  PICKUP_SUCCESS: { label: '取件成功', type: 'success' },
  SYSTEM_NOTICE: { label: '系统通知', type: 'info' }
}

/** 消息类型下拉选项 */
export const NOTIFICATION_TYPE_OPTIONS = Object.entries(NOTIFICATION_TYPE).map(([value, v]) => ({
  value,
  label: v.label
}))

/** 操作日志结果 */
export const LOG_RESULT = {
  SUCCESS: { label: '成功', type: 'success' },
  FAIL: { label: '失败', type: 'danger' }
}

/** 操作日志结果下拉选项 */
export const LOG_RESULT_OPTIONS = Object.entries(LOG_RESULT).map(([value, v]) => ({
  value,
  label: v.label
}))

/** 取件方式 */
export const PICKUP_METHOD = {
  CODE: '取件码',
  SCAN: '扫码',
  ADMIN: '管理员代取',
  COURIER: '快递员操作'
}

/** 角色编码 */
export const ROLE = {
  ADMIN: '管理员',
  COURIER: '快递员',
  USER: '普通用户'
}

/** 账号状态 */
export const USER_STATUS = {
  1: { label: '正常', type: 'success' },
  0: { label: '禁用', type: 'danger' }
}

/** 日期时间格式（与后端 DATE_FORMAT 出参一致） */
export const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'

/**
 * 取字典显示文案
 * @param map 字典对象（值为字符串或 { label } 结构）
 * @param code 状态编码
 * @returns 显示文案，未匹配返回编码本身或 -
 */
export function labelOf(map, code) {
  if (code === null || code === undefined || code === '') return '-'
  const item = map?.[code]
  if (item === undefined || item === null) return String(code)
  return typeof item === 'string' ? item : item.label
}

/**
 * 取字典标签颜色类型
 * @param map 字典对象
 * @param code 状态编码
 * @param fallback 默认颜色
 * @returns Element Plus el-tag 的 type
 */
export function tagTypeOf(map, code, fallback = 'info') {
  const item = map?.[code]
  if (item === null || item === undefined || typeof item === 'string') return fallback
  return item.type || fallback
}
