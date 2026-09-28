import request from './request'

/**
 * 快递公司管理接口模块
 * /company/list 对 ADMIN+COURIER 开放（快递员入库选公司），/detail、/volume、/add、/update、/updateStatus、/delete 仍仅 ADMIN；
 * /company/enabled 登录即可访问（入库/寄件选择公司用）
 */

/**
 * 分页查询快递公司列表（管理员）
 * @param params { pageNum, pageSize, keyword（公司名称/编码模糊，可选）, status（1 启用 0 停用，可选） }
 * @returns 公司列表（id/companyName/companyCode/contactName/contactPhone/status/createTime/updateTime），
 *          分页字段在 map（total/pageNum/pageSize/pages）
 */
export function listCompany(params) {
  return request.get('/company/list', { params })
}

/**
 * 查询快递公司详情（管理员）
 * @param id 公司ID
 * @returns 公司详情 CompanyVO
 */
export function detailCompany(id) {
  return request.get(`/company/detail/${id}`)
}

/**
 * 查询各快递公司业务量（管理员）
 * @returns [{ companyId, companyName, totalCount }]，按业务量倒序
 */
export function listCompanyVolume() {
  return request.get('/company/volume')
}

/**
 * 查询启用中的快递公司（入库/寄件下拉选择用，登录即可访问）
 * @returns 启用中的公司列表（id/companyName/companyCode/contactName/contactPhone/status）
 */
export function listCompanyOptions() {
  return request.get('/company/enabled')
}

/**
 * 新增快递公司（管理员，后端校验名称/编码唯一，新增默认启用）
 * @param data { companyName, companyCode, contactName?, contactPhone? }
 */
export function addCompany(data) {
  return request.post('/company/add', data)
}

/**
 * 修改快递公司（管理员，不含状态；启用/停用走 updateStatusCompany）
 * @param data { id, companyName, companyCode, contactName?, contactPhone? }
 */
export function updateCompany(data) {
  return request.put('/company/update', data)
}

/**
 * 启用/停用快递公司（管理员，专用状态接口）
 * @param id 公司ID
 * @param status 1 启用，0 停用
 */
export function updateStatusCompany(id, status) {
  return request.put(`/company/updateStatus/${id}`, null, { params: { status } })
}

/**
 * 删除快递公司（管理员；名下存在快递记录时后端拒绝，提示改用停用）
 * @param id 公司ID
 */
export function deleteCompany(id) {
  return request.delete(`/company/delete/${id}`)
}
