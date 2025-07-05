import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    fs: {
      // Allow serving files from the shared package
      allow: [
        // Search from the workspace root
        path.resolve(__dirname, '..'),
      ]
    }
  },
  resolve: {
    alias: {
      '@focushive/shared': path.resolve(__dirname, '../shared/src')
    }
  }
})
