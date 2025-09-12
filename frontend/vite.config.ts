import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// Security headers configuration
const securityHeaders = {
  // Content Security Policy - strict for production, lenient for dev
  'Content-Security-Policy': process.env.NODE_ENV === 'production' 
    ? `
      default-src 'self';
      script-src 'self' 'unsafe-inline' https://www.spotify.com;
      style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
      font-src 'self' https://fonts.gstatic.com data:;
      img-src 'self' data: https: blob:;
      media-src 'self' data: https: blob:;
      connect-src 'self' wss://localhost:* ws://localhost:* https://api.spotify.com wss://*.focushive.app ws://*.focushive.app;
      worker-src 'self' blob:;
      child-src 'self';
      frame-src 'none';
      object-src 'none';
      base-uri 'self';
      form-action 'self';
      frame-ancestors 'none';
    `.replace(/\s+/g, ' ').trim()
    : `
      default-src 'self';
      script-src 'self' 'unsafe-eval' 'unsafe-inline' https://www.spotify.com;
      style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
      font-src 'self' https://fonts.gstatic.com data:;
      img-src 'self' data: https: blob:;
      media-src 'self' data: https: blob:;
      connect-src 'self' wss://localhost:* ws://localhost:* https://api.spotify.com wss://*.focushive.app ws://*.focushive.app;
      worker-src 'self' blob:;
      child-src 'self';
      frame-src 'none';
      object-src 'none';
      base-uri 'self';
      form-action 'self';
      frame-ancestors 'none';
    `.replace(/\s+/g, ' ').trim(),
  
  // Prevent clickjacking
  'X-Frame-Options': 'DENY',
  
  // Prevent MIME type sniffing
  'X-Content-Type-Options': 'nosniff',
  
  // Enable XSS protection
  'X-XSS-Protection': '1; mode=block',
  
  // Referrer policy
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  
  // Permissions policy - restrict sensitive features
  'Permissions-Policy': [
    'geolocation=()',
    'microphone=(self)',
    'camera=(self)',
    'payment=()',
    'usb=()',
    'magnetometer=()',
    'gyroscope=()',
    'accelerometer=()',
    'ambient-light-sensor=()',
    'autoplay=(self)',
    'encrypted-media=(self)',
    'fullscreen=(self)',
    'picture-in-picture=(self)',
    'screen-wake-lock=(self)',
    'web-share=(self)'
  ].join(', '),

  // Cross-Origin policies
  'Cross-Origin-Embedder-Policy': 'credentialless',
  'Cross-Origin-Opener-Policy': 'same-origin',
  'Cross-Origin-Resource-Policy': 'same-origin'
}

// Add HSTS only in production
if (process.env.NODE_ENV === 'production') {
  securityHeaders['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains; preload'
}

// Security headers plugin for Vite dev server
const securityHeadersPlugin = () => ({
  name: 'security-headers',
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      // Add security headers to all responses
      Object.entries(securityHeaders).forEach(([header, value]) => {
        res.setHeader(header, value)
      })
      next()
    })
  }
})

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react({
      fastRefresh: true,
      // Include .tsx files for fast refresh
      include: '**/*.{jsx,tsx}',
    }),
    securityHeadersPlugin(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@app': path.resolve(__dirname, './src/app'),
      '@features': path.resolve(__dirname, './src/features'),
      '@shared': path.resolve(__dirname, './src/shared'),
      '@services': path.resolve(__dirname, './src/services'),
      '@hooks': path.resolve(__dirname, './src/hooks'),
      '@lib': path.resolve(__dirname, './src/lib'),
      '@components': path.resolve(__dirname, './src/components')
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          // Vendor chunk for React ecosystem
          if (id.includes('node_modules')) {
            // React core libraries
            if (id.includes('react') || id.includes('react-dom') || id.includes('react-router')) {
              return 'react-vendor'
            }
            
            // Material UI libraries
            if (id.includes('@mui/material')) {
              return 'mui-material'
            }
            
            // Material UI icons (heavy)
            if (id.includes('@mui/icons-material')) {
              return 'mui-icons'
            }
            
            // Chart libraries (load on demand)
            if (id.includes('recharts') || id.includes('d3') || id.includes('@mui/x-charts')) {
              return 'charts'
            }
            
            // Date utilities
            if (id.includes('date-fns') || id.includes('@mui/x-date-pickers')) {
              return 'date-utils'
            }
            
            // State management
            if (id.includes('@tanstack/react-query') || id.includes('zustand')) {
              return 'state-management'
            }
            
            // Communication libraries
            if (id.includes('socket.io') || id.includes('ws')) {
              return 'realtime'
            }
            
            // Utility libraries
            if (id.includes('lodash') || id.includes('clsx') || id.includes('uuid')) {
              return 'utils'
            }
            
            // Other vendor libraries
            return 'vendor'
          }
          
          // Feature-based code splitting
          if (id.includes('/features/analytics')) {
            return 'analytics'
          }
          
          if (id.includes('/features/gamification')) {
            return 'gamification'
          }
          
          if (id.includes('/features/music')) {
            return 'music'
          }
          
          if (id.includes('/features/chat') || id.includes('/features/forum') || id.includes('/features/buddy')) {
            return 'communication'
          }
          
          // Shared utilities
          if (id.includes('/shared/components')) {
            return 'shared-components'
          }
        },
        // Optimize chunk naming for caching
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId
          if (facadeModuleId) {
            if (facadeModuleId.includes('pages')) {
              return 'pages/[name]-[hash].js'
            }
            if (facadeModuleId.includes('components')) {
              return 'components/[name]-[hash].js'
            }
            if (facadeModuleId.includes('features')) {
              return 'features/[name]-[hash].js'
            }
          }
          return '[name]-[hash].js'
        }
      }
    },
    // Enable CSS code splitting
    cssCodeSplit: true,
    // Optimize assets
    assetsInlineLimit: 4096
  },
  server: {
    port: 5173,
    host: true, // Listen on all addresses including LAN
    hmr: {
      overlay: true, // Show error overlay
      port: 5173, // Use same port for HMR
    },
    watch: {
      usePolling: true, // Use polling for file changes (better for some environments)
      interval: 1000, // Check for changes every second
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})