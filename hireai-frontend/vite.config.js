import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/users': 'http://localhost:8080',
      '/recruiter': 'http://localhost:8080',
      '/jobs': 'http://localhost:8080',
      '/candidates': 'http://localhost:8080',
      '/candidate': 'http://localhost:8080',
      '/applications': 'http://localhost:8080',
      '/test': 'http://localhost:8080',
    },
  },
})

