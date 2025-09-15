import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {VitePWA} from 'vite-plugin-pwa'
import path from 'path'

const isPwaEnabled = process.env.ENABLE_PWA === 'true'

// Security headers configuration
const securityHeaders: Record<string, string> = {
  // Content Security Policy - strict for production, lenient for dev
  'Content-Security-Policy': process.env.NODE_ENV === 'production'
      ? `
      default-src 'self';
      script-src 'self' 'unsafe-inline' https://www.spotify.com;
      style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
      font-src 'self' https://fonts.gstatic.com data:;
      img-src 'self' data: https: blob:;
      media-src 'self' data: https: blob:;
      connect-src 'self' wss://localhost:* ws://localhost:* http://localhost:* https://localhost:* https://api.spotify.com wss://*.focushive.app ws://*.focushive.app http://*.focushive.app https://*.focushive.app;
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
      connect-src 'self' wss://localhost:* ws://localhost:* http://localhost:* https://localhost:* https://api.spotify.com wss://*.focushive.app ws://*.focushive.app http://*.focushive.app https://*.focushive.app;
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
const securityHeadersPlugin = (): { name: string; configureServer: (server: { middlewares: { use: (middleware: (req: unknown, res: { setHeader: (key: string, value: string) => void }, next: () => void) => void) => void } }) => void } => ({
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
    VitePWA({
      disable: !isPwaEnabled,
      minify: false,
      registerType: 'autoUpdate',
      includeAssets: [
        'favicon.ico',
        'apple-touch-icon.png',
        'browserconfig.xml',
        'icons/*.png'
      ],
      manifest: {
        name: 'FocusHive - Digital Co-Working Platform',
        short_name: 'FocusHive',
        description: 'A digital co-working platform for focused productivity sessions',
        theme_color: '#1976d2',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait-primary',
        scope: '/',
        start_url: '/',
        icons: [
          {
            src: 'icons/icon-72x72.png',
            sizes: '72x72',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-96x96.png',
            sizes: '96x96',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-128x128.png',
            sizes: '128x128',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-144x144.png',
            sizes: '144x144',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-152x152.png',
            sizes: '152x152',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-192x192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-384x384.png',
            sizes: '384x384',
            type: 'image/png',
            purpose: 'maskable any'
          },
          {
            src: 'icons/icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable any'
          }
        ],
        categories: ['productivity', 'education', 'business'],
        lang: 'en',
        dir: 'ltr'
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff,woff2}'],
        maximumFileSizeToCacheInBytes: 5000000, // 5MB - to handle large MUI icons chunk
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/api\.focushive\.app\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24 // 24 hours
              },
              networkTimeoutSeconds: 3
            }
          },
          {
            urlPattern: /^https:\/\/fonts\.googleapis\.com\//,
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'google-fonts-stylesheets'
            }
          },
          {
            urlPattern: /^https:\/\/fonts\.gstatic\.com\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'google-fonts-webfonts',
              expiration: {
                maxEntries: 30,
                maxAgeSeconds: 60 * 60 * 24 * 365 // 1 year
              }
            }
          }
        ]
      },
      devOptions: {
        enabled: true,
        type: 'module'
      }
    })
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
      external: ['@sentry/react', 'logrocket'],
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
    middlewareMode: false, // Ensure proper MIME types
    cors: true, // Enable CORS for dev server
    proxy: {
      // Core Backend Service (Port 8080)
      '/api/v1/hives': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/v1/presence': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/v1/timer': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      // Identity Service (Port 8081)
      '/api/v1/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req, res) => {
            proxyRes.headers['Access-Control-Allow-Origin'] = '*';
            proxyRes.headers['Access-Control-Allow-Credentials'] = 'true';
            proxyRes.headers['Access-Control-Allow-Methods'] = 'GET,HEAD,PUT,PATCH,POST,DELETE,OPTIONS';
            proxyRes.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization, X-Requested-With';
          });
        },
      },
      '/api/v1/personas': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/privacy': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },

      // Music Service (Port 8082)
      '/api/v1/music': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/spotify': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },

      // Notification Service (Port 8083)
      '/api/v1/notifications': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },

      // Chat Service (Port 8084)
      '/api/v1/chat': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },

      // Analytics Service (Port 8085)
      '/api/v1/analytics': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },

      // Forum Service (Port 8086)
      '/api/v1/forum': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },

      // Buddy Service (Port 8087)
      '/api/v1/buddy': {
        target: 'http://localhost:8087',
        changeOrigin: true,
      },

      // WebSocket connections
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      '/chat/ws': {
        target: 'ws://localhost:8084',
        ws: true,
        changeOrigin: true,
      },
      '/music/ws': {
        target: 'ws://localhost:8082',
        ws: true,
        changeOrigin: true,
      },
      '/analytics/ws': {
        target: 'ws://localhost:8085',
        ws: true,
        changeOrigin: true,
      },
      '/forum/ws': {
        target: 'ws://localhost:8086',
        ws: true,
        changeOrigin: true,
      },
      '/buddy/ws': {
        target: 'ws://localhost:8087',
        ws: true,
        changeOrigin: true,
      },

      // Fallback for any /api requests
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
