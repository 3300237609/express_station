import request from './request'

/**
 * 异常件管理接口模块
 * 类型：破损/丢失/地址错误/收件人信息错误/拒收/超期/其他
 * 处理状态：待处理 → 处理中 → 已处理 / 已关闭 / 已驳回
 * 权限：列表与详情 管理员+快递员；登记登录即可（普通用户须归属校验）；处理仅管理员
 */

/**
 * 分页查询异常件列表
 * @param params { pageNum, pageSize, keyword（单号/收件人，可选）, expressId（可选）,
 *                 exceptionType（可选）, handleStatus（可选）, reporterId（可选） }
 */
export function listException(params) {
  return request.get('/exception/list', { params })
}

/**
 * 查询异常件详情
 * @param id 异常记录ID
 */
export function detailException(id) {
  return request.get(`/exception/detail/${id}`)
}

/**
 * 登记异常快递（expressId 与 expressNo 二选一必填，按单号直传即可，无须换查快递列表）
 * @param data { expressNo, exceptionType, description, imageUrl? }（或传 expressId 替代 expressNo）
 */
export function addException(data) {
  return request.post('/exception/add', data)
}

/**
 * 处理异常件（终态须填处理结果并恢复快递状态；仅管理员）
 * @param data { id, handleStatus, handleRemark }
 */
export function handleException(data) {
  return request.put('/exception/handle', data)
}
