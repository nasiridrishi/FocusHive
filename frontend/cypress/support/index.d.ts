/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="cypress-axe" />

// Extend Cypress global types
declare namespace Cypress {
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
    mockApiCall(method: string, url: string, response: Record<string, unknown> | { fixture: string } | unknown[]): Chainable<void>
    
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
     * Custom command to press Tab key for keyboard navigation testing
     * @example cy.tab()
     */
    tab(): Chainable<JQuery<HTMLElement>>
  }
}

// Window interface extensions for testing
export interface TestWindow extends Window {
  Cypress?: typeof Cypress
  mockStompClient?: {
    connected: boolean
    send: (destination: string, body?: string) => void
    subscribe: (destination: string, callback: (message: unknown) => void) => void
  }
  mockWebSocketHandler?: (message: unknown) => void
}