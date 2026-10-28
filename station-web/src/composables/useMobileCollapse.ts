import { ref, onMounted, onUnmounted } from 'vue'

/** 移动端（≤768px）下筛选区默认折叠，桌面端始终展开 */
export function useMobileCollapse(breakpoint = 768) {
  const isMobile = ref(false)
  const filterCollapsed = ref(false)

  const checkMobile = () => {
    isMobile.value = window.innerWidth <= breakpoint
    // 移动端初始折叠，桌面端强制展开
    if (isMobile.value) {
      filterCollapsed.value = true
    } else {
      filterCollapsed.value = false
    }
  }

  const toggleFilter = () => {
    filterCollapsed.value = !filterCollapsed.value
  }

  onMounted(() => {
    checkMobile()
    window.addEventListener('resize', checkMobile)
  })
  onUnmounted(() => {
    window.removeEventListener('resize', checkMobile)
  })

  return { isMobile, filterCollapsed, toggleFilter }
}
