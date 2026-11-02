import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

/**
 * ECharts 按需挂载组合式函数
 * 用法：const { chartRef, render } = useEcharts(() => option)，
 * 数据变化后调用 render() 重新渲染，组件卸载时自动销毁并解绑 resize
 * @param getOption 获取 ECharts option 的函数（每次渲染重新调用）
 */
export function useEcharts(getOption) {
  const chartRef = ref(null)
  let chart = null
  let observer = null

  /** 渲染/更新图表（notMerge：避免旧 option 残留，如空态标题在数据到达后不消失） */
  function render() {
    if (!chartRef.value) return
    if (!chart) {
      chart = echarts.init(chartRef.value)
    }
    chart.setOption(getOption(), true)
  }

  /** 窗口尺寸变化时重绘 */
  function resize() {
    chart?.resize()
  }

  onMounted(async () => {
    // 等待 DOM 更新完成后再初始化（容器可能刚渲染出来）
    await nextTick()
    render()
    window.addEventListener('resize', resize)
    // 容器尺寸变化（侧栏折叠、栅格换行、空态高度切换）不触发 window.resize，需单独监听
    if (typeof ResizeObserver !== 'undefined' && chartRef.value) {
      observer = new ResizeObserver(() => resize())
      observer.observe(chartRef.value)
    }
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    observer?.disconnect()
    observer = null
    chart?.dispose()
    chart = null
  })

  return { chartRef, render }
}
