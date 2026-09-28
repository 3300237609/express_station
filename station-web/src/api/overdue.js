import request from './request'

/**
 * 超期管理接口模块（仅管理员；超期标记另有每小时定时任务，与此共用实现）
 */

/**
 * 分页查询超期快递列表（超期时长按 station.overdue.hours 配置实时计算）
 * @param params { pageNum, pageSize, keyword（单号/收件人/电话，可选） }
 * @returns [{ id, expressNo, receiverName, receiverPhone, pickupCode,
 *             shelfNo, locationNo, storageTime, overdueFlag, overdueHours, remark }]，total 在 map
 */
export function listOverdueExpress(params) {
  return request.get('/overdue/list', { params })
}

/**
 * 手动触发超期检测（与定时任务共用实现）
 * @returns 检测结果文案（data 为字符串，含本次新标记情况）
 */
export function refreshOverdue() {
  return request.post('/overdue/refresh')
}

/**
 * 对单个超期快递再次发送提醒
 * @param id 快递ID
 */
export function remindOverdue(id) {
  return request.post(`/overdue/remind/${id}`)
}
