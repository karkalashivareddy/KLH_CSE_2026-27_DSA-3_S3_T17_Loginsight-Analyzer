import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Phase 1 skeleton. The /api proxy targets the Spring Boot backend on :8080 during development
// (docs/02 §13). Feature configuration (build output, base path) joins once the dashboard exists.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});