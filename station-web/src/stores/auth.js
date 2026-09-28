import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi, logoutApi, getMeApi } from '@/api/auth'

/**
 * 登录态 Store
 * Token 存于 HttpOnly Cookie（authToken），前端只持久化用户信息用于菜单/权限渲染
 */

/** 预览模式：跳过登录直接放行（由 .env 的 VITE_SKIP_AUTH 控制，仅开发环境开启） */
const SKIP_AUTH = import.meta.env.VITE_SKIP_AUTH === 'true'

/** 预览模式默认身份（VITE_PREVIEW_ROLE 切换角色，用于查看不同角色的菜单与页面） */
const PREVIEW_USER = SKIP_AUTH
  ? {
      id: 0,
      username: 'preview',
      realName: '预览用户',
      phone: '',
      email: '',
      role: import.meta.env.VITE_PREVIEW_ROLE || 'ADMIN',
      roles: [import.meta.env.VITE_PREVIEW_ROLE || 'ADMIN']
    }
  : null

/**
 * 归一化用户信息：兼容登录出参 UserInfoVO（userId + roles[]）与本地已存的历史结构（id + role）
 * @param info 原始用户信息
 * @returns { id, username, realName, phone, email, avatar, roles[], role }
 */
function normalizeUser(info) {
  if (!info) return null
  const roles = Array.isArray(info.roles) && info.roles.length > 0 ? info.roles : info.role ? [info.role] : []
  return {
    id: info.id ?? info.userId ?? null,
    username: info.username,
    realName: info.realName,
    phone: info.phone,
    email: info.email,
    avatar: info.avatar,
    roles,
    role: roles[0] || ''
  }
}

export const useAuthStore = defineStore('auth', () => {
  /** 当前登录用户信息（登录返回的 LoginVO.userInfo 归一化后的结构） */
  const user = ref(normalizeUser(JSON.parse(localStorage.getItem('station_user') || 'null') || PREVIEW_USER))

  const isLogin = computed(() => !!user.value)
  /** 角色编码列表（ADMIN / COURIER / USER，可能一人多角色） */
  const roles = computed(() => user.value?.roles || [])
  /** 主角色：取角色列表第一个，供页面快捷判断（role === 'ADMIN'） */
  const role = computed(() => user.value?.role || '')
  const roleName = computed(() => ({ ADMIN: '管理员', COURIER: '快递员', USER: '普通用户' })[role.value] || '-')

  /** 登录成功后持久化用户信息（归一化后写入，保证本地结构统一） */
  function setUser(info) {
    user.value = normalizeUser(info)
    localStorage.setItem('station_user', JSON.stringify(user.value))
  }

  /** 登录：成功后解析 LoginVO（token 已由后端写入 HttpOnly Cookie，前端只持久化 userInfo） */
  async function login(form) {
    const r = await loginApi(form)
    setUser(normalizeUser(r.data?.userInfo || r.data))
    return user.value
  }

  /** 注册：按后端 RegisterDTO 提交（含 confirmPassword 两次密码一致性校验） */
  async function register(form) {
    return registerApi(form)
  }

  /** 退出登录：通知后端清理 Cookie 后清空本地状态（预览模式下登出自动回到预览身份） */
  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 退出接口异常（如 Cookie 已失效）时也允许本地登出，忽略错误
    }
    localStorage.removeItem('station_user')
    user.value = PREVIEW_USER
  }

  /** 刷新用户信息（远程拉取 /auth/me；失败时保持本地登录态不变） */
  async function refresh() {
    try {
      const r = await getMeApi()
      if (r?.data) setUser(r.data)
      return r?.data
    } catch (e) {
      // Cookie 失效或网络异常时保持本地状态，不打断页面
      return null
    }
  }

  /** 角色判断：当前用户任一角色命中允许列表即通过（列表为空视为不限制） */
  function hasRole(allowRoles) {
    if (!allowRoles || allowRoles.length === 0) return true
    return allowRoles.some((r) => roles.value.includes(r))
  }

  return { user, isLogin, roles, role, roleName, login, register, logout, refresh, setUser, hasRole }
})
