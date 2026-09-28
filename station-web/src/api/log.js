import request from './request'

/**
 * 操作日志接口模块（仅管理员审计查询，按操作时间倒序）
 */

/**
 * 分页查询操作日志
 * @param params { pageNum, pageSize, keyword（操作人/描述，可选）,
 *                 module（可选）, operation（可选）, result（SUCCESS/FAIL，可选） }
 */
export function listOperationLog(params) {
  return request.get('/log/list', { params })
}
