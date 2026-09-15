import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

const backendTarget = 'http://localhost:8080'

const proxyConfig = {
  target: backendTarget,
  changeOrigin: true,
  bypass: (req) => {
    if (req.headers.accept && req.headers.accept.includes('text/html')) {
      return '/index.html'
    }
  },
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/auth': proxyConfig,
      '/users': proxyConfig,
      '/recruiter': proxyConfig,
      '/jobs': proxyConfig,
      '/candidates': proxyConfig,
      '/candidate': proxyConfig,
      '/applications': proxyConfig,
      '/test': proxyConfig,
    },
  },
})

