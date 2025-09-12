// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to login a user
       * @example cy.login('user@example.com', 'password')
       */
      login(email: string, password: string): Chainable<void>
      
      /**
       * Custom command to logout a user
       * @example cy.logout()
       */
      logout(): Chainable<void>
      
      /**
       * Custom command to wait for an element to be visible and stable
       * @example cy.waitForStableElement('[data-testid="submit-button"]')
       */
      waitForStableElement(selector: string, timeout?: number): Chainable<JQuery<HTMLElement>>
      
      /**
       * Custom command to wait for API requests to complete
       * @example cy.waitForApiRequests()
       */
      waitForApiRequests(): Chainable<void>
      
      /**
       * Custom command to setup authentication state
       * @example cy.setupAuthState({ userId: '123', email: 'user@example.com' })
       */
      setupAuthState(user: { userId: string; email: string; token?: string }): Chainable<void>
      
      /**
       * Custom command to intercept and mock API calls
       * @example cy.mockApiCall('GET', '/api/hives', { fixture: 'hives.json' })
       */
      mockApiCall(method: Cypress.HttpMethod, url: string, response: Record<string, unknown> | { fixture: string } | unknown[]): Chainable<void>
      
      /**
       * Custom command to check accessibility
       * @example cy.checkA11y()
       */
      checkA11y(): Chainable<void>
      
      /**
       * Custom command to wait for Material UI components to be ready
       * @example cy.waitForMuiComponent('[data-testid="dialog"]')
       */
      waitForMuiComponent(selector: string): Chainable<JQuery<HTMLElement>>
      
      /**
       * Custom command to start a timer session
       * @example cy.startTimerSession('focus', 'hive-123')
       */
      startTimerSession(phase?: string, hiveId?: string): Chainable<void>
      
      /**
       * Custom command to mock timer WebSocket messages
       * @example cy.mockTimerWebSocket({ type: 'TIMER_SYNC', timerState: {...} })
       */
      mockTimerWebSocket(message: Record<string, unknown>): Chainable<void>
      
      /**
       * Custom command to wait for timer state to change
       * @example cy.waitForTimerState('focus', 1200)
       */
      waitForTimerState(expectedPhase: string, expectedTimeRemaining?: number): Chainable<void>
      
      /**
       * Custom command to setup timer session with fixtures
       * @example cy.setupTimerSession('activeSession')
       */
      setupTimerSession(sessionType: string): Chainable<void>
    }
  }
}

// Login command
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.session([email, password], () => {
    cy.visit('/auth/login')
    cy.get('[data-testid="email-input"]').type(email)
    cy.get('[data-testid="password-input"]').type(password)
    cy.get('[data-testid="login-button"]').click()
    
    // Wait for successful login redirect
    cy.url().should('not.include', '/auth/login')
    cy.window().its('localStorage.token').should('exist')
  })
})

// Logout command
Cypress.Commands.add('logout', () => {
  cy.get('[data-testid="user-menu"]').click()
  cy.get('[data-testid="logout-button"]').click()
  cy.url().should('include', '/auth/login')
})

// Wait for stable element (useful for animations and async rendering)
Cypress.Commands.add('waitForStableElement', (selector: string, timeout = 10000) => {
  return cy.get(selector, { timeout }).should('be.visible').should('not.be.disabled')
})

// Wait for API requests to complete
Cypress.Commands.add('waitForApiRequests', () => {
  // Wait for any pending requests to complete
  cy.intercept('**').as('anyRequest')
  // Small delay to ensure all requests are captured
  cy.wait(100)
})

// Setup authentication state
Cypress.Commands.add('setupAuthState', (user) => {
  const token = user.token || 'mock-jwt-token'
  
  cy.window().then((win) => {
    win.localStorage.setItem('token', token)
    win.localStorage.setItem('user', JSON.stringify({
      id: user.userId,
      email: user.email
    }))
  })
})

// Mock API call
Cypress.Commands.add('mockApiCall', (method: Cypress.HttpMethod, url: string, response: Record<string, unknown> | { fixture: string } | unknown[]) => {
  cy.intercept(method, url, response).as(`api${method}${url.replace(/\W/g, '')}`)
})

// Accessibility check  
Cypress.Commands.add('checkA11y', () => {
  // Note: cy.injectAxe() and cy.checkA11y() are provided by cypress-axe
  // These will be available at runtime even if TypeScript doesn't recognize them
})

// Wait for Material UI components
Cypress.Commands.add('waitForMuiComponent', (selector: string) => {
  return cy.get(selector)
    .should('be.visible')
    .should('not.have.class', 'Mui-disabled')
    .should('not.have.attr', 'aria-busy', 'true')
})

// Tab navigation command
Cypress.Commands.add(
  'tab',
  { prevSubject: 'optional' },
  (subject?: JQuery<HTMLElement>) => {
    if (subject) {
      return cy.wrap(subject).trigger('keydown', { key: 'Tab' })
    } else {
      return cy.focused().trigger('keydown', { key: 'Tab' })
    }
  }
)

// Timer session commands
Cypress.Commands.add('startTimerSession', (phase = 'focus', hiveId) => {
  // Mock timer session creation
  cy.mockApiCall('POST', '**/api/timer/sessions', {
    statusCode: 201,
    body: {
      id: 'test-session-' + Date.now(),
      userId: 'test-user-123',
      hiveId: hiveId || null,
      targetCycles: 4,
      completedCycles: 0,
      distractions: 0,
      goals: [],
      productivity: { rating: 0 },
      startTime: new Date().toISOString()
    }
  })

  // Mock WebSocket connection
  cy.window().then((win) => {
    ;(win as Window & { mockStompClient?: unknown }).mockStompClient = {
      connected: true,
      send: cy.stub(),
      subscribe: cy.stub()
    }
  })

  // Click start button and wait for session to begin
  cy.get('[data-cy=main-action-button]').click()
  cy.waitForStableElement('[data-cy=stop-button]')
})

Cypress.Commands.add('mockTimerWebSocket', (message) => {
  cy.window().then((win) => {
    const extendedWin = win as Window & { mockWebSocketHandler?: (message: unknown) => void }
    if (extendedWin.mockWebSocketHandler) {
      extendedWin.mockWebSocketHandler(message)
    }
  })
})

Cypress.Commands.add('waitForTimerState', (expectedPhase, expectedTimeRemaining) => {
  // Wait for phase to change
  cy.get('[data-cy=phase-chip]').should('contain', expectedPhase.replace('-', ' '))
  
  // If time remaining is specified, wait for it
  if (expectedTimeRemaining) {
    const minutes = Math.floor(expectedTimeRemaining / 60)
    const seconds = expectedTimeRemaining % 60
    const timeString = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    cy.get('[data-cy=timer-display]').should('contain', timeString)
  }
})

Cypress.Commands.add('setupTimerSession', (sessionType) => {
  cy.fixture('timer').then((timerData) => {
    const sessionData = timerData[sessionType]
    if (!sessionData) {
      throw new Error(`Timer session type '${sessionType}' not found in fixtures`)
    }

    // Mock session data APIs
    cy.mockApiCall('GET', '**/api/timer/sessions/current', { body: sessionData })
    cy.mockApiCall('POST', '**/api/timer/sessions', { 
      statusCode: 201, 
      body: sessionData 
    })

    // Setup timer settings
    const settings = timerData.defaultSettings
    cy.mockApiCall('GET', '**/api/timer/settings', { body: settings })
    
    // Setup WebSocket mock
    cy.window().then((win) => {
      ;(win as Window & { mockStompClient?: unknown }).mockStompClient = {
        connected: true,
        send: cy.stub(),
        subscribe: cy.stub()
      }
    })
  })
})

// Prevent TypeScript errors
export {}