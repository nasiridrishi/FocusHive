import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test-setup.ts',
    testTimeout: 10000, // Increase timeout to 10 seconds
    hookTimeout: 10000, // Increase hook timeout
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@app': path.resolve(__dirname, './src/app'),
      '@features': path.resolve(__dirname, './src/features'),
      '@shared': path.resolve(__dirname, './src/shared'),
      // Mock virtual PWA modules for testing
      'virtual:pwa-register': path.resolve(__dirname, './src/test-utils/virtual-pwa-register-mock.ts'),
      'virtual:pwa-register/react': path.resolve(__dirname, './src/test-utils/virtual-pwa-register-react-mock.ts'),
      'virtual:pwa-info': path.resolve(__dirname, './src/test-utils/virtual-pwa-info-mock.ts'),
    },
  },
});