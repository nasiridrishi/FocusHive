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
    // Add worker limits to prevent runaway processes
    pool: 'threads',
    poolOptions: {
      threads: {
        maxThreads: 4, // Limit to 4 worker threads max
        minThreads: 1  // Start with 1 thread
      }
    },
    // Coverage configuration
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json', 'lcov'],
      reportsDirectory: './coverage',
      exclude: [
        'node_modules/**',
        'src/test-utils/**',
        'src/test-setup.ts',
        'src/**/*.test.{ts,tsx}',
        'src/**/*.spec.{ts,tsx}',
        'src/**/*.d.ts',
        'src/vite-env.d.ts',
        'src/types/**',
        'src/examples/**',
        'src/components/demo/**',
        '**/index.{ts,tsx}',
        '**/*.config.{ts,js}',
        'src/main.tsx',
        'vite.config.ts',
        'vitest.config.ts'
      ],
      thresholds: {
        global: {
          statements: 70,
          branches: 65,
          functions: 65,
          lines: 70
        }
      },
      include: ['src/**/*.{ts,tsx}'],
      all: true
    },
    // Test file patterns
    include: [
      'src/**/*.{test,spec}.{ts,tsx}'
    ],
    exclude: [
      'node_modules/**',
      'src/examples/**',
      'src/components/demo/**'
    ]
  },
  esbuild: {
    // Fix for MUI X DatePickers ESM import issue
    target: 'esnext'
  },
  optimizeDeps: {
    // Force pre-bundle MUI packages to avoid ESM issues
    include: [
      '@mui/material',
      '@mui/system',
      '@mui/icons-material',
      '@mui/x-date-pickers',
      '@mui/x-date-pickers/DatePicker',
      '@mui/x-date-pickers/LocalizationProvider',
      '@mui/x-date-pickers/AdapterDateFns'
    ]
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
      // Fix MUI X Date Pickers import issue - comprehensive fix
      '@mui/material/styles$': path.resolve(__dirname, './node_modules/@mui/material/node/styles/index.js'),
      '@mui/material/styles/': path.resolve(__dirname, './node_modules/@mui/material/node/styles/'),
      '@mui/material$': path.resolve(__dirname, './node_modules/@mui/material/node/index.js'),
      '@mui/system$': path.resolve(__dirname, './node_modules/@mui/system/index.js'),
      '@mui/system/': path.resolve(__dirname, './node_modules/@mui/system/'),
      // Mock virtual PWA modules for testing
      'virtual:pwa-register': path.resolve(__dirname, './src/test-utils/virtual-pwa-register-mock.ts'),
      'virtual:pwa-register/react': path.resolve(__dirname, './src/test-utils/virtual-pwa-register-react-mock.ts'),
      'virtual:pwa-info': path.resolve(__dirname, './src/test-utils/virtual-pwa-info-mock.ts'),
    },
  },
});
