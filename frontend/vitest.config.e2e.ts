import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test-setup.e2e.ts', // Use E2E setup, NO MOCKS
    testTimeout: 30000, // 30 seconds for real service calls
    hookTimeout: 30000, // 30 seconds for hooks
    pool: 'threads',
    poolOptions: {
      threads: {
        maxThreads: 2, // Limit threads for E2E tests
        minThreads: 1
      }
    },
    // Test file patterns - only E2E test files
    include: [
      'src/**/*.e2e.test.{ts,tsx}',
      'src/**/*.e2e.spec.{ts,tsx}'
    ],
    exclude: [
      'node_modules/**',
      'src/examples/**',
      'src/components/demo/**'
    ]
  },
  esbuild: {
    target: 'esnext'
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@app': path.resolve(__dirname, './src/app'),
      '@features': path.resolve(__dirname, './src/features'),
      '@shared': path.resolve(__dirname, './src/shared'),
      '@services': path.resolve(__dirname, './src/services'),
      '@hooks': path.resolve(__dirname, './src/hooks'),
      '@lib': path.resolve(__dirname, './src/lib'),
      '@components': path.resolve(__dirname, './src/components'),
      '@contracts': path.resolve(__dirname, './src/contracts'),
      '@contexts': path.resolve(__dirname, './src/contexts'),
      '@utils': path.resolve(__dirname, './src/utils'),
      '@types': path.resolve(__dirname, './src/types'),
      '@config': path.resolve(__dirname, './src/config')
    },
  },
});