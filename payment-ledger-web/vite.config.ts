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
      '/wallet/limits': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/wallet/recipient': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/wallet/transfer': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/transactions': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/beneficiaries': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/users/me/password': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/users/me': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
