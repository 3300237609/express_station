import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import DictTag from './components/DictTag.vue'
import StatCard from './components/StatCard.vue'
import './style.css'

const app = createApp(App)

// Pinia 必须先于 router 安装：路由守卫内会读取登录态 Store
app.use(createPinia())
app.use(router)
// Element Plus 全量引入 + 中文语言包（分页/日期等组件文案）
app.use(ElementPlus, { locale: zhCn })

// 全局注册业务公共组件（字典标签、统计卡片）
app.component('DictTag', DictTag)
app.component('StatCard', StatCard)

// 全局注册 Element Plus 图标，菜单/按钮可直接按名称使用
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.mount('#app')
