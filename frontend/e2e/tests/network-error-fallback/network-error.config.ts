/**
 * Specialized Playwright Configuration for NetworkErrorFallback E2E Testing
 * Focuses on network failure scenarios and recovery testing
 */

import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
  testDir: './e2e/tests/network-error-fallback',
  
  /* Global test timeout - longer for network testing */
  timeout: 90000,
  
  /* Expect timeout for assertions */
  expect: {
    timeout: 15000, // Longer timeout for network operations
  },
  
  /* Run tests in parallel */
  fullyParallel: true,
  
  /* Retry configuration */
  retries: process.env.CI ? 3 : 1, // More retries for network-dependent tests
  
  /* Workers configuration */
  workers: process.env.CI ? 2 : 4,
  
  /* Reporter configuration */
  reporter: [
    ['html', {outputFolder: 'e2e-network-error-report', open: 'never'}],
    ['json', {outputFile: 'e2e-network-error-results.json'}],
    ['junit', {outputFile: 'e2e-network-error-results.xml'}],
    ['allure-playwright'],
    ['list'],
  ],
  
  /* Shared settings optimized for network error testing */
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:5173',
    
    /* Collect traces and videos for debugging network issues */
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    
    /* Network-specific settings */
    ignoreHTTPSErrors: true,
    waitForLoadState: 'domcontentloaded', // Don't wait for networkidle in network error tests
    actionTimeout: 20000, // Longer timeout for actions during network issues
    navigationTimeout: 30000,
    
    /* Additional context for network testing */
    permissions: ['geolocation'], // For testing location-based features
    
    /* Network simulation capabilities */
    launchOptions: {
      args: [
        '--disable-web-security', // For CORS testing
        '--disable-dev-shm-usage',
        '--no-sandbox',
      ]
    }
  },
  
  /* Test projects for different network scenarios */
  projects: [
    // Standard desktop testing
    {
      name: 'chromium-network',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome'
      }
    },
    
    {
      name: 'firefox-network',
      use: {
        ...devices['Desktop Firefox']
      }
    },
    
    {
      name: 'webkit-network',
      use: {
        ...devices['Desktop Safari']
      }
    },
    
    // Mobile network testing
    {
      name: 'mobile-chrome-network',
      use: {
        ...devices['Pixel 5'],
        // Simulate slower mobile network
        launchOptions: {
          args: ['--force-effective-connection-type=3g']
        }
      }
    },
    
    {
      name: 'mobile-safari-network',
      use: {
        ...devices['iPhone 12'],
      }
    },
    
    // Slow network simulation
    {
      name: 'slow-network',
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: {
          args: [
            '--force-effective-connection-type=slow-2g',
            '--force-prefers-reduced-motion'
          ]
        }
      }
    },
    
    // Offline-first testing
    {
      name: 'offline-first',
      use: {
        ...devices['Desktop Chrome'],
        offline: true, // Start offline
      }
    },
    
    // High latency testing
    {
      name: 'high-latency',
      use: {
        ...devices['Desktop Chrome'],
        // Will be configured via route handling for latency simulation
      }
    },
    
    // Accessibility with network errors
    {
      name: 'accessibility-network',
      use: {
        ...devices['Desktop Chrome'],
        colorScheme: 'dark',
        reducedMotion: 'reduce',
        // High contrast mode for accessibility testing
        launchOptions: {
          args: ['--force-prefers-color-scheme=dark']
        }
      }
    }
  ],
  
  /* Global setup for network error testing */
  globalSetup: require.resolve('./network-error-global-setup.ts'),
  globalTeardown: require.resolve('./network-error-global-teardown.ts'),
  
  /* Web server configuration with backend services */
  webServer: [
    // Frontend server
    {
      command: 'npm run dev',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
      env: {
        VITE_API_BASE_URL: process.env.E2E_API_BASE_URL || 'http://localhost:8080',
        VITE_IDENTITY_API_URL: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081',
        VITE_E2E_MODE: 'true',
        VITE_NETWORK_ERROR_TESTING: 'true'
      },
    },
    
    // Mock backend service for controlled testing
    {
      command: 'node e2e/tests/network-error-fallback/mock-backend.js',
      url: 'http://localhost:8080',
      reuseExistingServer: true,
      timeout: 30 * 1000,
    }
  ],
  
  /* Test metadata */
  metadata: {
    testType: 'network-error-handling',
    coverage: [
      'api-failure-handling',
      'offline-detection',
      'retry-mechanisms',
      'network-status-display',
      'user-interaction-feedback',
      'error-boundary-integration',
      'accessibility-during-errors',
      'mobile-network-handling',
      'performance-during-errors'
    ],
    scenarios: [
      'complete-backend-failure',
      'intermittent-connectivity',
      'slow-network-conditions',
      'partial-api-failures',
      'offline-online-transitions',
      'timeout-handling',
      'retry-limit-reached',
      'network-recovery'
    ]
  }
});