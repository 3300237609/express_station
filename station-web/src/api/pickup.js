import request from './request'

/**
 * 取件管理接口模块（登录即可；普通用户仅可操作收件关联到本人的快递）
 * 流程：验证取件码（只读）→ 展示快递与货位 → 确认取件（事务出库 + 释放货位 + 写取件记录 + 发通知）
 */

/**
 * 验证取件码（只读，不产生任何状态变化）
 * 后端为表单参数 @RequestParam code，不是 JSON 请求体
 * @param code 取件码
 * @returns 待取件快递展示信息
 */
export function verifyPickupCode(code) {
  return request.post('/pickup/verify', null, { params: { code } })
}

/**
 * 确认取件出库（事务）
 * @param data { code, pickupMethod? } code 取件码必填；
 *              pickupMethod：CODE/SCAN/ADMIN/COURIER（选填默认 CODE，普通用户强制 CODE）
 */
export function confirmPickup(data) {
  return request.post('/pickup/confirm', data)
}

/**
 * 分页查询取件记录（管理员/快递员；kw 单号/取件码/取件用户模糊，startTime/endTime 为取件时间范围）
 * @param params { pageNum, pageSize, kw, startTime, endTime }
 */
export function listPickupRecord(params) {
  return request.get('/pickup/recordList', { params })
}
