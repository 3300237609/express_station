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

  /** 渲染/更新图表 */
  function render() {
    if (!chartRef.value) return
    if (!chart) {
      chart = echarts.init(chartRef.value)
    }
    chart.setOption(getOption())
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
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    chart?.dispose()
    chart = null
  })

  return { chartRef, render }
}
