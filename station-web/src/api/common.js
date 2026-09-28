import request from './request'

/**
 * 公共接口模块
 */

/**
 * 按手机号匹配系统用户（快递入库时关联收件用户）
 * 后端 GET /user/matchByPhone 已实现：11 位格式校验，未匹配返回 code=1 且 data=null（正常分支非报错）
 * @param phone 收件人手机号（11 位）
 * @returns 用户简要信息 { id, username, realName, phone }，未匹配返回 null
 */
export function matchUserByPhone(phone) {
  return request.get('/user/matchByPhone', { params: { phone } })
}
