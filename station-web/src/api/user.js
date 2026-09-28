import request from './request'

/**
 * 用户管理接口模块（管理员）
 * URL 约定：GET /user/list、GET /user/detail/{id}、POST /user/add、PUT /user/update、DELETE /user/delete/{id}
 */

/**
 * 分页查询用户列表
 * @param params { pageNum, pageSize, kw, status }
 */
export function listUser(params) {
  return request.get('/user/list', { params })
}

/**
 * 查询用户详情
 * @param id 用户ID
 */
export function detailUser(id) {
  return request.get(`/user/detail/${id}`)
}

/**
 * 新增用户
 * @param data { username, password, realName, phone, email, role }
 */
export function addUser(data) {
  return request.post('/user/add', data)
}

/**
 * 修改用户
 * @param data { id, ... }
 */
export function updateUser(data) {
  return request.put('/user/update', data)
}

/**
 * 删除用户
 * @param id 用户ID
 */
export function deleteUser(id) {
  return request.delete(`/user/delete/${id}`)
}

/**
 * 启用/禁用用户
 * @param data { id, status } status：1 正常 / 0 禁用
 */
export function updateUserStatus(data) {
  return request.put('/user/updateStatus', data)
}

/**
 * 重置用户密码
 * @param id 用户ID（后端重置为默认密码）
 */
export function resetUserPassword(id) {
  return request.put(`/user/resetPassword/${id}`)
}

/**
 * 分页查询快递员列表
 * @param params { pageNum, pageSize, kw, status }
 */
export function listCourier(params) {
  return request.get('/courier/list', { params })
}

/**
 * 新增快递员
 * @param data { username, password, realName, phone, email }
 */
export function addCourier(data) {
  return request.post('/courier/add', data)
}

/**
 * 修改快递员
 * @param data { id, ... }
 */
export function updateCourier(data) {
  return request.put('/courier/update', data)
}

/**
 * 删除快递员
 * @param id 快递员ID
 */
export function deleteCourier(id) {
  return request.delete(`/courier/delete/${id}`)
}
