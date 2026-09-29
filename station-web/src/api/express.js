import request from './request'

/**
 * 快递管理接口模块（列表 ADMIN+COURIER 供快递员异常登记换单号；我的快递登录即可；其余管理操作均 ADMIN）
 * 列表/详情/流转/我的/新增/修改/删除/状态修正/调整货位已全部对齐后端 ExpressController；
 * 入库一步创建见 api/inbound.js，货位推荐见 api/shelf.js，超期管理见 api/overdue.js；
 */

/**
 * 分页条件查询快递列表（管理员）
 * @param params { pageNum, pageSize, keyword（单号模糊，可选）, pickupCode（取件码模糊，可选）,
 *                 receiverName（收件人模糊，可选）, receiverPhone（收件人电话模糊，可选）,
 *                 companyId（可选）, status（EXPRESS_STATUS 编码，可选）,
 *                 arrivalTimeStart（到达时间起 yyyy-MM-dd，可选）, arrivalTimeEnd（到达时间止 yyyy-MM-dd，可选） }
 * @returns 快递列表（含 companyName/shelfNo/locationNo/overdueFlag），total 在 map
 */
export function listExpress(params) {
  return request.get('/express/list', { params })
}

/**
 * 查询快递详情
 * @param id 快递ID
 * @returns 快递详情 ExpressVO（含公司名与货架/货位编号）
 */
export function detailExpress(id) {
  return request.get(`/express/detail/${id}`)
}

/**
 * 按快递单号精确查询（货架可视化搜索定位用，单号全局唯一）
 * @param expressNo 快递单号
 * @returns 快递详情 ExpressVO（含 locationId、locationNo、shelfNo 等）
 */
export function detailExpressByNo(expressNo) {
  return request.get(`/express/detailByNo/${expressNo}`)
}

/**
 * 查询快递流转记录
 * @param id 快递ID
 * @returns 流转节点列表（按时间正序，节点 { time, event, detail }）
 */
export function listExpressFlow(id) {
  return request.get(`/express/flow/${id}`)
}

/**
 * 查询我的快递（登录即可，分视角：快递员=本人经手入库的快递；其余=收件用户=我 或 收件手机号=我的手机号；含取件码与货位）
 * @param params { pageNum, pageSize, keyword（单号，可选）, status（EXPRESS_STATUS 编码，可选） }
 * @returns 名下快递分页列表（ExpressVO 全字段），total 在 map
 */
export function listMyExpress(params) {
  return request.get('/express/my', { params })
}

/**
 * 新增快递（录入，初始状态待入库；取件码由后端生成）
 * @param data { expressNo, companyId, receiverName, receiverPhone,
 *               senderName?, senderPhone?, receiverUserId?, sizeType?（SMALL/MEDIUM/LARGE）,
 *               weight?（kg）, arrivalTime?（ISO 格式 2025-01-15T08:00:00）, remark? }
 * @returns 当前后端 data 为提示文案，不含新快递 id（需要 id 的入库流程已改走 /inbound/add 一步创建入库）
 */
export function addExpress(data) {
  return request.post('/express/add', data)
}

/**
 * 修改快递基础信息（不改取件码、状态与货位）
 * @param data { id, ...同新增必填字段 }
 */
export function updateExpress(data) {
  return request.put('/express/update', data)
}

/**
 * 删除快递（占用货位/有取件记录/有异常登记时后端拒绝）
 * @param id 快递ID
 */
export function deleteExpress(id) {
  return request.delete(`/express/delete/${id}`)
}

/**
 * 状态修正（管理员，专用状态接口）
 * @param id 快递ID
 * @param status 目标状态 EXPRESS_STATUS 编码（仍关联货位时禁止置为 PICKED_UP/RETURNED）
 */
export function updateExpressStatus(id, status) {
  return request.put(`/express/updateStatus/${id}`, null, { params: { status } })
}

/**
 * 调整快递货位（事务：释放原货位、占用目标货位并修正计数；空闲货位下拉见 api/shelf.js listFreeLocation）
 * @param data { expressId, locationId }
 */
export function updateExpressLocation(data) {
  return request.put('/express/updateLocation', data)
}
