/**
 * Cross-Browser Compatibility Testing Configuration
 * Comprehensive Playwright configuration for testing across multiple browsers
 */

import { defineConfig, devices } from '@playwright/test';
import { BrowserMatrix } from './browser-matrix';

export default defineConfig({
  testDir: './tests/cross-browser',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  timeout: 60000,
  expect: {
    timeout: 10000,
    toHaveScreenshot: {
      mode: 'only-on-failure',
      caret: 'hide',
      animations: 'disabled'
    }
  },
  reporter: [
    ['html', { outputFolder: 'cross-browser-report' }],
    ['json', { outputFile: 'cross-browser-results.json' }],
    ['junit', { outputFile: 'cross-browser-junit.xml' }],
    ['allure-playwright']
  ],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
    actionTimeout: 10000,
    navigationTimeout: 30000
  },

  projects: [
    // Desktop Chrome (Latest versions)
    {
      name: 'chrome-latest',
      use: { ...devices['Desktop Chrome'] },
      metadata: {
        platform: 'desktop',
        browser: 'chrome',
        version: 'latest'
      }
    },
    {
      name: 'chrome-previous',
      use: { 
        ...devices['Desktop Chrome'],
        channel: 'chrome-dev'
      },
      metadata: {
        platform: 'desktop',
        browser: 'chrome',
        version: 'latest-1'
      }
    },
    {
      name: 'chrome-stable-1',
      use: { 
        ...devices['Desktop Chrome'],
        channel: 'chrome'
      },
      metadata: {
        platform: 'desktop',
        browser: 'chrome',
        version: 'latest-2'
      }
    },

    // Desktop Firefox (Latest versions)
    {
      name: 'firefox-latest',
      use: { ...devices['Desktop Firefox'] },
      metadata: {
        platform: 'desktop',
        browser: 'firefox',
        version: 'latest'
      }
    },
    {
      name: 'firefox-previous',
      use: { 
        ...devices['Desktop Firefox'],
        channel: 'firefox-beta'
      },
      metadata: {
        platform: 'desktop',
        browser: 'firefox',
        version: 'latest-1'
      }
    },
    {
      name: 'firefox-stable-1',
      use: { 
        ...devices['Desktop Firefox'],
        channel: 'firefox'
      },
      metadata: {
        platform: 'desktop',
        browser: 'firefox',
        version: 'latest-2'
      }
    },

    // Desktop Safari (macOS only)
    {
      name: 'safari-latest',
      use: { ...devices['Desktop Safari'] },
      metadata: {
        platform: 'desktop',
        browser: 'safari',
        version: 'latest'
      }
    },
    {
      name: 'safari-previous',
      use: { ...devices['Desktop Safari'] },
      metadata: {
        platform: 'desktop',
        browser: 'safari',
        version: 'latest-1'
      }
    },

    // Desktop Edge (Latest versions)
    {
      name: 'edge-latest',
      use: { 
        ...devices['Desktop Edge'],
        channel: 'msedge'
      },
      metadata: {
        platform: 'desktop',
        browser: 'edge',
        version: 'latest'
      }
    },
    {
      name: 'edge-previous',
      use: { 
        ...devices['Desktop Edge'],
        channel: 'msedge-dev'
      },
      metadata: {
        platform: 'desktop',
        browser: 'edge',
        version: 'latest-1'
      }
    },
    {
      name: 'edge-stable-1',
      use: { 
        ...devices['Desktop Edge'],
        channel: 'msedge-beta'
      },
      metadata: {
        platform: 'desktop',
        browser: 'edge',
        version: 'latest-2'
      }
    },

    // Mobile browsers
    {
      name: 'chrome-mobile',
      use: { 
        ...devices['Pixel 5'],
        hasTouch: true,
        isMobile: true
      },
      metadata: {
        platform: 'mobile',
        browser: 'chrome-mobile',
        version: 'latest'
      }
    },
    {
      name: 'safari-mobile',
      use: { 
        ...devices['iPhone 12'],
        hasTouch: true,
        isMobile: true
      },
      metadata: {
        platform: 'mobile',
        browser: 'safari-mobile',
        version: 'latest'
      }
    },

    // Tablet browsers
    {
      name: 'chrome-tablet',
      use: { 
        ...devices['iPad Pro'],
        hasTouch: true
      },
      metadata: {
        platform: 'tablet',
        browser: 'chrome-tablet',
        version: 'latest'
      }
    },

    // Special configurations for testing specific features
    {
      name: 'chrome-no-javascript',
      use: { 
        ...devices['Desktop Chrome'],
        javaScriptEnabled: false
      },
      metadata: {
        platform: 'desktop',
        browser: 'chrome',
        version: 'latest',
        special: 'no-javascript'
      }
    },
    {
      name: 'chrome-slow-network',
      use: { 
        ...devices['Desktop Chrome'],
        launchOptions: {
          args: ['--force-effective-connection-type=2g']
        }
      },
      metadata: {
        platform: 'desktop',
        browser: 'chrome',
        version: 'latest',
        special: 'slow-network'
      }
    },
    {
      name: 'chrome-high-contrast',
      use: { 
        ...devices['Desktop Chrome'],
        launchOptions: {
          args: ['--force-prefers-color-scheme=dark', '--force-prefers-reduced-motion']
        }
      },
      metadata: {
        platform: 'desktop',
        browser: 'chrome',
        version: 'latest',
        special: 'high-contrast'
      }
    }
  ],

  // Global setup for cross-browser testing
  globalSetup: require.resolve('./global-setup-cross-browser'),
  globalTeardown: require.resolve('./global-teardown-cross-browser'),

  // Web server configuration
  webServer: {
    command: 'npm run dev',
    port: 5173,
    reuseExistingServer: !process.env.CI,
    timeout: 120000
  }
});