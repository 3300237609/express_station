import request from './request'

/**
 * 系统配置接口模块（管理员）
 * 示例配置键：station.name、station.overdue.hours、pickup.code.length、notification.enabled
 */

/**
 * 查询全部系统配置（按配置键升序；后端不分页，筛选由前端本地过滤）
 */
export function listConfig() {
  return request.get('/config/list')
}

/**
 * 修改系统配置
 * @param data { id, configKey, configValue, description, status }（configKey 必填，后端查重与操作日志都要用）
 */
export function updateConfig(data) {
  return request.put('/config/update', data)
}
