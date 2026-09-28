import request from './request'

/**
 * 货架/货位管理接口模块（管理员）
 * /shelf/list、/detail、/add、/update、/updateStatus、/delete 仅 ADMIN 可用；
 * /shelfLocation 同理（含 /free 空闲货位查询）
 */

/**
 * 分页查询货架列表
 * @param params { pageNum, pageSize, keyword（编号或区域模糊，可选）, status（1 正常 0 停用，可选） }
 * @returns 货架列表（id/shelfNo/area/capacity/usedCount/status/remark/usageRate/...），total 在 map
 */
export function listShelf(params) {
  return request.get('/shelf/list', { params })
}

/**
 * 查询货架详情（含货位使用率）
 * @param id 货架ID
 */
export function detailShelf(id) {
  return request.get(`/shelf/detail/${id}`)
}

/**
 * 新增货架（总货位数/使用率由系统按货位增删自动维护，入参不传）
 * @param data { shelfNo, area, remark }
 */
export function addShelf(data) {
  return request.post('/shelf/add', data)
}

/**
 * 修改货架基础信息（不改状态与系统计数；状态走 updateShelfStatus）
 * @param data { id, shelfNo, area, remark }
 */
export function updateShelf(data) {
  return request.put('/shelf/update', data)
}

/**
 * 启用/停用货架（专用状态接口，状态不随编辑弹窗提交）
 * @param id 货架ID
 * @param status 目标状态：1 正常，0 停用
 */
export function updateShelfStatus(id, status) {
  return request.put(`/shelf/updateStatus/${id}`, null, { params: { status } })
}

/**
 * 删除货架（名下有占用货位时后端拒绝，空货位级联删除）
 * @param id 货架ID
 */
export function deleteShelf(id) {
  return request.delete(`/shelf/delete/${id}`)
}

/**
 * 分页查询货位列表
 * @param params { pageNum, pageSize, shelfId（所属货架，可选）, status（1 空闲 2 部分占用 3 已满 4 停用，可选） }
 * @returns 货位列表（含 shelfNo/area），total 在 map
 */
export function listLocation(params) {
  return request.get('/shelfLocation/list', { params })
}

/**
 * 查询货位详情
 * @param id 货位ID
 */
export function detailLocation(id) {
  return request.get(`/shelfLocation/detail/${id}`)
}

/**
 * 查询空闲货位（状态=空闲且未被占用、所属货架正常；调整货位下拉用）
 * @param shelfId 所属货架ID，可空（不传查全部货架）
 * @returns 空闲货位列表（默认拉取前 100 条）
 */
export function listFreeLocation(shelfId) {
  return request.get('/shelfLocation/free', {
    params: { shelfId, pageNum: 1, pageSize: 100 }
  })
}

/**
 * 智能货位推荐（管理员/快递员入库选位用：按容量匹配+空闲程度+区域打分）
 * @param params { sizeType?（SMALL/MEDIUM/LARGE）, weight?（kg）, area?（如 A 区） }
 * @returns [{ shelfId, shelfNo, area, locationId, locationNo, rowNo, columnNo,
 *             capacity, usedCount, remaining, status, score, reason }]（评分降序）
 */
export function recommendLocation(params) {
  return request.get('/shelfLocation/recommend', { params })
}

/**
 * 新增货位（同步货架总货位数 +1）
 * @param data { shelfId, locationNo, rowNo, columnNo, capacity }
 */
export function addLocation(data) {
  return request.post('/shelfLocation/add', data)
}

/**
 * 修改货位（所属货架不可迁移；容量不得小于已用）
 * @param data { id, shelfId, locationNo, rowNo, columnNo, capacity }
 */
export function updateLocation(data) {
  return request.put('/shelfLocation/update', data)
}

/**
 * 删除货位（被占用时后端拒绝；同步货架总货位数 -1）
 * @param id 货位ID
 */
export function deleteLocation(id) {
  return request.delete(`/shelfLocation/delete/${id}`)
}

/**
 * 启用/停用（状态修正）货位（专用状态接口）
 * @param id 货位ID
 * @param status 目标状态：1 空闲，2 部分占用，3 已满，4 停用
 */
export function updateLocationStatus(id, status) {
  return request.put(`/shelfLocation/updateStatus/${id}`, null, { params: { status } })
}
