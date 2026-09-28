/**
 * 通用展示格式化工具
 */

/**
 * 电话号码脱敏展示：13812345678 → 138****5678
 * @param phone 手机号
 * @returns 脱敏后的手机号，空值返回 -
 */
export function maskPhone(phone) {
  if (!phone) return '-'
  const s = String(phone)
  if (s.length < 7) return s
  return s.slice(0, 3) + '****' + s.slice(-4)
}

/**
 * 超期时长展示：按入库时间与当前时间差计算
 * @param storageTime 入库时间字符串（YYYY-MM-DD HH:mm:ss）
 * @returns 如「2天3小时」，未超期或参数为空返回 -
 */
export function formatOverdue(storageTime) {
  if (!storageTime) return '-'
  const start = new Date(String(storageTime).replace(/-/g, '/')).getTime()
  if (Number.isNaN(start)) return '-'
  const diff = Date.now() - start
  if (diff <= 0) return '-'
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(hours / 24)
  const rest = hours % 24
  return days > 0 ? `${days}天${rest}小时` : `${hours}小时`
}

/**
 * 使用率百分比文案
 * @param used 已用
 * @param total 总量
 * @returns 如 68.5%，总量为空返回 0%
 */
export function usagePercent(used, total) {
  const t = Number(total)
  if (!t) return '0%'
  return `${((Number(used || 0) / t) * 100).toFixed(1)}%`
}
