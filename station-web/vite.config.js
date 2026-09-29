import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// Vite 配置：别名、开发代理、构建输出
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // @ -> src，与 jsconfig.json 保持一致
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 允许 cpolar 等内网穿透动态域名访问（开发期用 true 放行所有 host）
    allowedHosts: true,
    proxy: {
      // 开发期将 /api 转发到后端 station-server（后端接口无 /api 前缀，故重写时去掉）
      '/api': {
        target: 'http://localhost:80',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
