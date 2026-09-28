import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 应用通用状态：侧边栏折叠、未读消息数等
 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否折叠 */
  const isCollapse = ref(false)

  /** 未读消息数（消息中心红点） */
  const unreadCount = ref(0)

  function toggleSidebar() {
    isCollapse.value = !isCollapse.value
  }

  function setUnreadCount(count) {
    unreadCount.value = count || 0
  }

  return { isCollapse, unreadCount, toggleSidebar, setUnreadCount }
})
