/**
 * Playwright Configuration for Authentication E2E Tests
 * Specialized configuration for comprehensive authentication testing
 */

import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/tests/auth',
  
  /* Global test timeout */
  timeout: 60000,
  
  /* Expect timeout for assertions */
  expect: {
    timeout: 10000,
  },

  /* Run tests in files in parallel */
  fullyParallel: true,
  
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  
  /* Opt out of parallel tests on CI for more stable authentication testing */
  workers: process.env.CI ? 1 : 3,
  
  /* Reporter configuration for authentication tests */
  reporter: [
    ['html', { outputFolder: 'e2e-auth-report', open: 'never' }],
    ['json', { outputFile: 'e2e-auth-results.json' }],
    ['junit', { outputFile: 'e2e-auth-results.xml' }],
    ['list'],
  ],

  /* Shared settings for all authentication tests */
  use: {
    /* Base URL for authentication tests */
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:5173',

    /* Collect trace on first retry */
    trace: 'on-first-retry',
    
    /* Take screenshot on failure */
    screenshot: 'only-on-failure',
    
    /* Record video for failed tests */
    video: 'retain-on-failure',
    
    /* Ignore HTTPS errors for local testing */
    ignoreHTTPSErrors: true,
    
    /* Wait for network idle by default for authentication flows */
    waitForLoadState: 'networkidle',
    
    /* Timeout for individual actions */
    actionTimeout: 15000,
    
    /* Store authentication state between tests */
    storageState: undefined, // Each test manages its own state
    
    /* Additional context options for authentication testing */
    contextOptions: {
      // Clear cookies and storage between tests
      clearCookies: true,
      clearStorage: true,
    },
  },

  /* Test projects for different browsers and scenarios */
  projects: [
    // Setup project for test preparation
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
      teardown: 'cleanup',
    },

    // Cleanup project
    {
      name: 'cleanup',
      testMatch: /.*\.cleanup\.ts/,
    },

    // Desktop browsers
    {
      name: 'chromium-auth',
      use: { 
        ...devices['Desktop Chrome'],
        // Chrome-specific authentication settings
        channel: 'chrome',
      },
      dependencies: ['setup'],
    },

    {
      name: 'firefox-auth',
      use: { 
        ...devices['Desktop Firefox'],
        // Firefox-specific authentication settings
      },
      dependencies: ['setup'],
    },

    {
      name: 'webkit-auth',
      use: { 
        ...devices['Desktop Safari'],
        // Safari-specific authentication settings
      },
      dependencies: ['setup'],
    },

    // Mobile testing
    {
      name: 'mobile-chrome-auth',
      use: { 
        ...devices['Pixel 5'],
        // Mobile Chrome authentication testing
      },
      dependencies: ['setup'],
    },

    {
      name: 'mobile-safari-auth',
      use: { 
        ...devices['iPhone 12'],
        // Mobile Safari authentication testing
      },
      dependencies: ['setup'],
    },

    // Tablet testing
    {
      name: 'tablet-auth',
      use: { 
        ...devices['iPad'],
        // Tablet authentication testing
      },
      dependencies: ['setup'],
    },

    // Slow network testing
    {
      name: 'slow-network-auth',
      use: {
        ...devices['Desktop Chrome'],
        // Simulate slow network for authentication
        connectOptions: {
          timeout: 30000,
        },
        navigationTimeout: 30000,
      },
      dependencies: ['setup'],
      grep: /@slow-network/,
    },

    // Accessibility testing
    {
      name: 'accessibility-auth',
      use: {
        ...devices['Desktop Chrome'],
        // High contrast and reduced motion for accessibility testing
        colorScheme: 'dark',
        reducedMotion: 'reduce',
      },
      dependencies: ['setup'],
      grep: /@accessibility/,
    },

    // Security testing
    {
      name: 'security-auth',
      use: {
        ...devices['Desktop Chrome'],
        // Security-focused testing configuration
        javaScriptEnabled: true,
        // Extra security headers
        extraHTTPHeaders: {
          'X-Test-Security': 'enabled',
        },
      },
      dependencies: ['setup'],
      grep: /@security/,
    },
  ],

  /* Global setup and teardown */
  globalSetup: require.resolve('./e2e/global-setup-auth.ts'),
  globalTeardown: require.resolve('./e2e/global-teardown-auth.ts'),

  /* Configure web server for authentication testing */
  webServer: [
    // Frontend server
    {
      command: 'npm run dev',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
      env: {
        // Authentication-specific environment variables
        VITE_IDENTITY_API_URL: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081',
        VITE_API_BASE_URL: process.env.E2E_API_BASE_URL || 'http://localhost:8080',
        VITE_OAUTH_GOOGLE_CLIENT_ID: process.env.E2E_OAUTH_GOOGLE_CLIENT_ID || 'test-google-client-id',
        VITE_OAUTH_GITHUB_CLIENT_ID: process.env.E2E_OAUTH_GITHUB_CLIENT_ID || 'test-github-client-id',
        VITE_E2E_MODE: 'true',
      },
    },
    
    // MailHog server for email testing (optional)
    {
      command: 'docker run --rm -p 1025:1025 -p 8025:8025 mailhog/mailhog',
      url: 'http://localhost:8025',
      reuseExistingServer: true,
      timeout: 30 * 1000,
      env: {
        E2E_MAILHOG_API_URL: 'http://localhost:8025',
        E2E_MAILHOG_WEB_URL: 'http://localhost:8025',
      },
    },
  ],

  /* Test metadata and annotations */
  metadata: {
    testType: 'authentication',
    coverage: [
      'user-registration',
      'email-verification', 
      'login-logout',
      'password-reset',
      'oauth-integration',
      'session-management',
      'security-features',
      'accessibility',
      'mobile-responsiveness',
      'error-handling',
    ],
    browsers: ['chromium', 'firefox', 'webkit'],
    devices: ['desktop', 'mobile', 'tablet'],
    features: [
      'cross-browser-compatibility',
      'responsive-design',
      'keyboard-navigation',
      'screen-reader-support',
      'oauth2-integration',
      'jwt-token-management',
      'session-security',
      'rate-limiting',
      'csrf-protection',
    ],
  },
});