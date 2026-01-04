import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  optimizeDeps: {
    include: ['katex', 'katex/dist/contrib/auto-render']
  },
  server: {
    port: 5100,
    host: '127.0.0.1',
    proxy: {
      '/api': {
        target: 'http://localhost:18081',
        changeOrigin: true,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (_proxyReq, req, _res) => {
            console.log('[Proxy]', req.method, req.url, '->', (options.target || '') + (req.url || ''));
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('[Proxy]', req.url, '->', proxyRes.statusCode);
          });
        },
      },
    },
  },
})
