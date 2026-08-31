import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/wallet/me': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/wallet/deposit': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/wallet/withdraw': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
