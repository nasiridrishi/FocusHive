// ***********************************************************
// This file is processed and loaded automatically before your test files.
// You can change the location of this file or turn off automatically serving support files with the 'supportFile' configuration option.
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'
import './timer-helpers'
import '@testing-library/cypress/add-commands'
import 'cypress-axe'

// Alternatively you can use CommonJS syntax:
// require('./commands')

// Global configuration and hooks
beforeEach(() => {
  // Reset any authentication state before each test
  cy.window().then((win) => {
    win.localStorage.clear()
    win.sessionStorage.clear()
  })
})

// Add custom error handling
Cypress.on('uncaught:exception', (err, runnable) => {
  // Prevent Cypress from failing tests on specific errors
  // that might occur during normal application operation
  
  // Don't fail on React DevTools or development-only errors
  if (err.message.includes('ResizeObserver loop limit exceeded')) {
    return false
  }
  
  // Don't fail on network errors that might occur in development
  if (err.message.includes('NetworkError') || err.message.includes('Failed to fetch')) {
    return false
  }
  
  // Return true to fail the test, false to continue
  return true
})

// Global after hook for cleanup
afterEach(() => {
  // Take screenshot on failure (already configured in cypress.config.ts)
  // Additional cleanup if needed
})