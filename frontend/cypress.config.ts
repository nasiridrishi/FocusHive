import {defineConfig} from 'cypress'

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:5173',
    viewportWidth: 1280,
    viewportHeight: 720,
    video: false,
    screenshotOnRunFailure: true,
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.ts',
    fixturesFolder: 'cypress/fixtures',
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    responseTimeout: 15000,
    pageLoadTimeout: 30000,
    setupNodeEvents(_on, _config) {
      // implement node event listeners here
    },
    env: {
      // Environment variables for testing
      apiUrl: 'http://localhost:8080',
      identityServiceUrl: 'http://localhost:8081'
    },
    // Test isolation
    testIsolation: true,
  },

  component: {
    devServer: {
      framework: 'react',
      bundler: 'vite',
    },
    specPattern: 'src/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/component.ts',
    testIsolation: true,
  },

  retries: {
    runMode: 2,
    openMode: 0,
  },

  watchForFileChanges: true,
  chromeWebSecurity: false,

  // Screenshots and videos
  screenshotsFolder: 'cypress/screenshots',
  videosFolder: 'cypress/videos',
})