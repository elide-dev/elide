import { defineConfig } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react-swc'
import { version } from './package.json'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react() , tailwindcss()],
  define: {
    __APP_VERSION__: JSON.stringify(version),
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Proxy API requests to Elide server during development
      '/api': {
        target: 'http://localhost:4983', // Default port for `elide db studio`
        changeOrigin: true,
      },
      '/health': {
        target: 'http://localhost:4983',
        changeOrigin: true,
      }
    }
  }
})
