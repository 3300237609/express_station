import request from './request'

/**
 * 数据统计接口模块（看板汇总管理员+快递员、趋势仅管理员；个人业绩登录即可）
 * 汇总与趋势走 /stat/*，公司业务量复用 /company/volume
 */

/**
 * 查询首页看板统计总览
 * @returns { totalExpress（快递总量）, todayInbound（今日入库）, todayPickup（今日取件）,
 *            waitingPickup（待取）, exceptionCount（异常）, overdueCount（超期）,
 *            locationTotal / locationUsed（货位总量/已用）, usageRate（使用率%，字符串）,
 *            companyVolume[]（公司业务量） }
 */
export function getDashboard() {
  return request.get('/stat/dashboard')
}

/**
 * 查询当前登录人个人业绩（快递员仪表盘；登录即可，固定本人口径，无法查看他人业绩）
 * @returns { todayInbound / monthInbound / totalInbound（本人经手入库）,
 *            todayPickup / monthPickup / totalPickup（本人操作出库）,
 *            todayException / monthException / totalException（本人异常登记） }
 */
export function getCourierDashboard() {
  return request.get('/stat/courierDashboard')
}

/**
 * 查询入库/取件趋势（按日聚合，区间内无数据日期补 0）
 * @param params { dateStart?, dateEnd? } yyyy-MM-dd；均不传默认近 7 天（截止当天）
 * @returns [{ date, inboundCount, pickupCount }]
 */
export function getTrend(params) {
  return request.get('/stat/trend', { params })
}

/**
 * 查询快递公司业务量
 * @returns [{ companyId, companyName, totalCount }]，按业务量倒序
 */
export function getCompanyStats() {
  return request.get('/company/volume')
}
