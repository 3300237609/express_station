import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { TIP_NOT_LOGIN, TIP_EXPIRED } from '@/utils/authTip'

/**
 * Axios 请求实例
 * 后端统一响应结构：{ code, msg, data, map }，code=1 成功 / code=0 失败
 * 认证走 HttpOnly Cookie（authToken），故 withCredentials 固定开启
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
  withCredentials: true
})

// 是否已弹出登录确认框或正在跳转（避免 401 连发时重复弹框）
let redirecting = false

/**
 * 登录失效统一处理：清理本地登录态 → 弹确认框 → 用户点「确定」后整页跳转登录页
 * 场景区分：本地存有登录态 → Cookie 已失效，提示「登录已过期」；无登录态 → 提示「未登录」
 * 点「取消」则留在当前页（复位标记，之后再有 401 会重新弹框）
 */
function handleUnauthorized() {
  // 1. 并发请求同时 401：只弹一次
  if (redirecting) return
  redirecting = true

  // 2. 场景判断（判断后立即清理本地登录态，保证后续判断准确）
  const hadLoginState = !!localStorage.getItem('station_user')
  localStorage.removeItem('station_user')
  const tip = hadLoginState ? TIP_EXPIRED : TIP_NOT_LOGIN

  // 3. 弹确认框，点确定才跳转
  const alreadyOnLogin = window.location.pathname === '/login'
  ElMessageBox.confirm(tip, '登录提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
    .then(() => {
      if (alreadyOnLogin) {
        // 已在登录页：无需跳转，复位标记
        redirecting = false
      } else {
        // 整页跳转登录页（带 redirect 参数回跳原页面）
        const back = window.location.pathname + window.location.search
        window.location.href = `/login?redirect=${encodeURIComponent(back)}`
      }
    })
    .catch(() => {
      // 取消：留在当前页，复位标记以便下次 401 重新弹框
      redirecting = false
    })
}

// 响应拦截：成功返回整个 R 对象（调用方取 r.data / r.map.total），失败统一提示并拒绝
service.interceptors.response.use(
  (response) => {
    const r = response.data
    // 文件流等非 JSON 响应直接透传
    if (r === null || typeof r !== 'object' || !('code' in r)) {
      return r
    }
    if (r.code === 1) {
      return r
    }
    // 业务失败：后端已给出中文全角文案，直接提示
    ElMessage.error(r.msg || '操作失败！')
    return Promise.reject(r)
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      // 401 清理登录态后弹确认框，按本地登录态区分「未登录/登录已过期」，点确定再跳登录页
      handleUnauthorized()
    } else if (status === 403) {
      ElMessage.error('没有该操作的权限！')
    } else {
      ElMessage.error(error.response?.data?.msg || '网络异常，请稍后重试！')
    }
    return Promise.reject(error)
  }
)

export default service
