/**
 * Hive Workflow E2E Test Configuration
 * Specialized configuration for running the complete hive workflow test suite
 */

import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
  testDir: './hive',

  // Test execution settings
  fullyParallel: false, // Sequential execution for workflow tests
  timeout: 120000, // 2 minutes per test (longer for complex workflows)
  expect: {
    timeout: 10000, // 10 seconds for assertions
  },

  // Retry and worker configuration
  retries: process.env.CI ? 3 : 1,
  workers: process.env.CI ? 1 : 2, // Limit workers for WebSocket tests

  // Reporter configuration
  reporter: [
    ['html', {
      outputFolder: 'hive-workflow-report',
      open: process.env.CI ? 'never' : 'on-failure'
    }],
    ['json', {outputFile: 'hive-workflow-results.json'}],
    ['junit', {outputFile: 'hive-workflow-results.xml'}],
    ['line']
  ],

  // Global test configuration
  use: {
    // Base URL for hive tests
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:5173',

    // Browser settings
    headless: process.env.CI ? true : false,
    viewport: {width: 1280, height: 720},
    ignoreHTTPSErrors: true,

    // Network and timing
    actionTimeout: 15000, // 15 seconds for actions
    navigationTimeout: 30000, // 30 seconds for navigation

    // Capture settings for debugging
    trace: process.env.CI ? 'retain-on-failure' : 'on-first-retry',
    screenshot: 'only-on-failure',
    video: process.env.CI ? 'retain-on-failure' : 'off',

    // WebSocket and real-time testing
    waitForLoadState: 'networkidle',

    // Additional context for hive tests
    extraHTTPHeaders: {
      'Accept-Language': 'en-US,en;q=0.9',
    },
  },

  // Test projects for different scenarios
  projects: [
    // Setup project for test data
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
      teardown: 'cleanup',
    },

    // Cleanup project
    {
      name: 'cleanup',
      testMatch: /global\.teardown\.ts/,
    },

    // Main workflow tests - Desktop Chrome
    {
      name: 'hive-workflow-chrome',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome' // Use stable Chrome for WebSocket features
      },
      dependencies: ['setup'],
      testMatch: [
        '**/hive-creation.spec.ts',
        '**/hive-joining.spec.ts',
        '**/timer-session.spec.ts',
        '**/presence-updates.spec.ts',
        '**/session-analytics.spec.ts'
      ],
    },

    // Cross-browser validation - Firefox
    {
      name: 'hive-workflow-firefox',
      use: {
        ...devices['Desktop Firefox']
      },
      dependencies: ['setup'],
      testMatch: [
        '**/hive-creation.spec.ts',
        '**/hive-joining.spec.ts'
      ],
    },

    // Mobile testing - Critical workflows only
    {
      name: 'hive-workflow-mobile',
      use: {
        ...devices['iPhone 13']
      },
      dependencies: ['setup'],
      testMatch: [
        '**/hive-creation.spec.ts',
        '**/timer-session.spec.ts'
      ],
    },

    // Performance testing
    {
      name: 'hive-performance',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome'
      },
      dependencies: ['setup'],
      testMatch: [
        '**/presence-updates.spec.ts',
        '**/timer-session.spec.ts'
      ],
      grep: /@performance/,
    },

    // Accessibility testing
    {
      name: 'hive-accessibility',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome'
      },
      dependencies: ['setup'],
      testMatch: ['**/*.spec.ts'],
      grep: /accessibility|keyboard|screen reader|aria/i,
    },
  ],

  // Global setup and teardown
  globalSetup: require.resolve('./global.setup.ts'),
  globalTeardown: require.resolve('./global.teardown.ts'),

  // Web server configuration for local testing
  webServer: process.env.CI ? undefined : {
    command: 'npm run dev',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: true,
    timeout: 120 * 1000,
    env: {
      // Test environment variables
      VITE_API_BASE_URL: process.env.E2E_API_BASE_URL || 'http://localhost:8080',
      VITE_IDENTITY_API_URL: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081',
      VITE_WS_URL: process.env.E2E_WS_URL || 'ws://localhost:8080/ws',
      NODE_ENV: 'test',
    },
  },
});