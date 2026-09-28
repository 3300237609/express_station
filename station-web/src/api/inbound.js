import request from './request'

/**
 * 快递入库接口模块（管理员/快递员）
 * 事务一步完成：创建或绑定快递 + 绑定推荐货位 + 生成取件码 + 状态置为待取件
 */

/**
 * 快递入库（事务）
 * @param data { locationId 必填（智能推荐后工作人员确认的货位）,
 *               expressId 选填（绑定状态为待入库的已录入快递；不传则连同基础字段直接创建）,
 *               ...ExpressDTO 基础字段（expressId 为空时必填：expressNo/companyId/receiverName/receiverPhone） }
 * @returns 入库后的快递详情（含取件码与货位）
 */
export function inboundAdd(data) {
  return request.post('/inbound/add', data)
}
