/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [tailwindcss(), react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/api-ai': {
        target: 'http://127.0.0.1:8083',
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/api-ai/, ''),
      },
      '/api-collector': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/api-collector/, ''),
      },
      '/api-research': {
        target: 'http://127.0.0.1:8084',
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/api-research/, ''),
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
  },
})
