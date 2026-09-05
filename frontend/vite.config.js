import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    // 编辑器/工具用「临时文件 + 重命名」的方式保存时，chokidar 会去 watch 临时目录，
    // Windows 下会抛 EBUSY 把 dev server 带崩，这里显式忽略掉
    watch: {
      ignored: ['**/.*.tmpdir/**', '**/*.tmpdir/**', '**/.*.tmp', '**/*.tmp']
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:18092',
        changeOrigin: true
      }
    }
  }
})
