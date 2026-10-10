import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Vite configuration.
 *
 * `LOGINSIGHT_API_TARGET` lets a local run point the dev proxy at a backend on a
 * non-default port without editing this file, which is useful when :8080 is
 * already taken by another project on the same machine.
 */
const apiTarget = process.env.LOGINSIGHT_API_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  build: {
    // Three.js is the single largest dependency in the product. Keeping it in its
    // own long-lived chunk means the topology upgrade path is explicit and the
    // warning threshold reflects the real per-chunk cost.
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (id.includes('/three/')) return 'vendor-three';
          if (id.includes('/motion') || id.includes('/framer-motion/')) return 'vendor-motion';
          if (id.includes('/react-dom/') || id.includes('/react-router') || id.includes('/scheduler/')) return 'vendor-react';
          if (id.includes('/lucide-react/')) return 'vendor-icons';
          return undefined;
        }
      }
    }
  },
  server: {
    port: Number(process.env.PORT ?? 5173),
    host: process.env.HOST ?? 'localhost',
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
});