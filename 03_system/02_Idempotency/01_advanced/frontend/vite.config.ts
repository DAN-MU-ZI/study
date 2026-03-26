import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://backend:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: ['frontend', 'localhost'],
    host: '0.0.0.0',
    port: 4173,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
  },
});
