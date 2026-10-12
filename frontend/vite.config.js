import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The proxy sends every /api request to the Spring Boot backend, so the React code
// never hard-codes a host and nothing breaks on another computer.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
