import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 移动端判定（≤768px，与 Element Plus 断点一致），随窗口变化实时更新
 * 用于少量必须由 JS 切换的布局（如抽屉菜单、描述列表列数）；
 * 纯样式差异请优先用媒体查询实现
 * @param breakpoint 视口宽度上限（px）
 * @returns { import('vue').Ref<boolean> } 是否处于移动端视口
 */
export function useIsMobile(breakpoint = 768) {
  const isMobile = ref(false)
  let mq = null

  function sync() {
    isMobile.value = mq.matches
  }

  onMounted(() => {
    mq = window.matchMedia(`(max-width: ${breakpoint}px)`)
    sync()
    mq.addEventListener('change', sync)
  })

  onBeforeUnmount(() => {
    mq?.removeEventListener('change', sync)
  })

  return isMobile
}
