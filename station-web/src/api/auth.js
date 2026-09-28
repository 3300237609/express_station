import request from './request'

/**
 * 认证接口模块
 * 登录成功后后端写入 HttpOnly Cookie（authToken），Token 不需要前端手动管理
 */

/**
 * 登录
 * @param data { username, password }
 * @returns LoginVO：{ token, userInfo: { userId, username, realName, phone, email, avatar, roles[] } }
 */
export function loginApi(data) {
  return request.post('/auth/login', data)
}

/**
 * 注册普通用户账号（后端校验两次密码一致、用户名/手机号唯一）
 * @param data { username, password, confirmPassword, realName, phone, email? }
 */
export function registerApi(data) {
  return request.post('/auth/register', data)
}

/** 退出登录（后端清除 authToken Cookie） */
export function logoutApi() {
  return request.post('/auth/logout')
}

/**
 * 获取当前登录用户信息（登录即可）
 * @returns UserInfoVO { userId, username, realName, phone, email, avatar, roles[] }
 */
export function getMeApi() {
  return request.get('/auth/me')
}

/**
 * 修改本人密码（ChangePasswordDTO：oldPassword 原密码、newPassword 6-32 位）
 * @param data { oldPassword, newPassword }
 */
export function changePasswordApi(data) {
  return request.put('/auth/changePassword', data)
}
