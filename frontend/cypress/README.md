# Cypress E2E Testing for FocusHive Frontend

This directory contains the Cypress end-to-end testing setup for the FocusHive frontend application.

## Setup

Cypress is already installed and configured. The setup includes:

- **Cypress Configuration**: `cypress.config.ts` with TypeScript support
- **Custom Commands**: Extended Cypress commands for common testing patterns
- **Test Fixtures**: Mock data for consistent testing
- **Support Files**: Global configuration and utilities
- **Example Tests**: Comprehensive test examples for different features

## Directory Structure

```
cypress/
├── e2e/                    # E2E test files
│   ├── smoke.cy.ts         # Smoke tests for basic functionality
│   ├── auth.cy.ts          # Authentication flow tests
│   └── hives.cy.ts         # Hive management tests
├── fixtures/               # Test data
│   ├── users.json          # User test data
│   └── hives.json          # Hive test data
├── support/                # Support files and utilities
│   ├── commands.ts         # Custom commands
│   ├── e2e.ts              # Global E2E configuration
│   ├── component.ts        # Component testing setup
│   └── index.d.ts          # TypeScript declarations
├── screenshots/            # Auto-generated screenshots (ignored)
├── videos/                 # Auto-generated videos (ignored)
└── README.md               # This file
```

## Running Tests

### Interactive Mode (Cypress Test Runner)
```bash
npm run cypress:open
```

### Headless Mode (CI/CD)
```bash
npm run cypress:run
```

### Specific Browsers
```bash
npm run cypress:run:chrome
npm run cypress:run:firefox
```

### Individual Test Files
```bash
npm run test:cypress:smoke    # Run only smoke tests
npm run test:cypress:auth     # Run only authentication tests
```

## Custom Commands

The setup includes several custom commands to simplify common testing patterns:

### Authentication
```typescript
// Login with session caching
cy.login('user@example.com', 'password')

// Setup authenticated state without UI interaction
cy.setupAuthState({ userId: '123', email: 'user@example.com' })

// Logout
cy.logout()
```

### API Mocking
```typescript
// Mock API responses
cy.mockApiCall('GET', '/api/hives', { fixture: 'hives.json' })
cy.mockApiCall('POST', '/api/hives', { statusCode: 201, body: { id: 'new-hive' } })
```

### UI Helpers
```typescript
// Wait for stable elements (animations, async rendering)
cy.waitForStableElement('[data-testid="submit-button"]')

// Material UI specific waiting
cy.waitForMuiComponent('[data-testid="dialog"]')

// Tab navigation testing
cy.get('input').tab().tab()
```

### Accessibility
```typescript
// Check accessibility compliance
cy.checkA11y()
```

## Test Data (Fixtures)

Test fixtures provide consistent mock data:

- **`users.json`**: User accounts, authentication data
- **`hives.json`**: Hive data, member information

## Configuration

### Environment Variables

Configure test environment in `cypress.config.ts`:

```typescript
env: {
  apiUrl: 'http://localhost:8080',           // Backend API
  identityServiceUrl: 'http://localhost:8081' // Identity service
}
```

### Base URL

The default base URL is set to `http://localhost:5173` (Vite dev server).

### Viewports

Default viewport: 1280x720 (configurable per test)

## Writing Tests

### Test Structure

Follow the AAA pattern (Arrange, Act, Assert):

```typescript
describe('Feature Name', () => {
  beforeEach(() => {
    // Arrange: Setup test state
    cy.setupAuthState(/* user data */)
    cy.visit('/page')
  })

  it('should perform expected behavior', () => {
    // Act: Interact with the application
    cy.get('[data-testid="button"]').click()
    
    // Assert: Verify expected outcomes
    cy.get('[data-testid="result"]').should('be.visible')
  })
})
```

### Data Test IDs

Use `data-testid` attributes for reliable element selection:

```tsx
// In React components
<button data-testid="submit-button">Submit</button>

// In tests
cy.get('[data-testid="submit-button"]').click()
```

### Mock WebSocket Connections

For real-time features:

```typescript
beforeEach(() => {
  cy.window().then((win) => {
    win.mockStompClient = {
      connected: true,
      send: cy.stub(),
      subscribe: cy.stub()
    }
  })
})
```

## Best Practices

### 1. Test Organization
- Group related tests in `describe` blocks
- Use descriptive test names
- Keep tests independent and isolated

### 2. Element Selection
- Prefer `data-testid` over CSS classes or text content
- Use `cy.findByRole()` from Testing Library for semantic selection
- Avoid fragile selectors that break with UI changes

### 3. Assertions
- Use appropriate Cypress assertions (`should`, `expect`)
- Test user-visible behavior, not implementation details
- Verify both positive and negative scenarios

### 4. Performance
- Use `cy.session()` for authentication to avoid repeated logins
- Mock API calls to control test data and improve reliability
- Use fixtures for consistent test data

### 5. Accessibility
- Include accessibility tests using `cy.checkA11y()`
- Test keyboard navigation with custom `tab()` command
- Verify semantic markup and ARIA attributes

## Integration with CI/CD

The setup is configured for continuous integration:

- **Video Recording**: Disabled by default to save space
- **Screenshots**: Captured on test failures
- **Retries**: Configured for flaky test resilience
- **Parallel Execution**: Supports parallel test runs

## Troubleshooting

### Common Issues

1. **Test Flakiness**: Use `cy.waitForStableElement()` for dynamic content
2. **WebSocket Issues**: Mock WebSocket connections in tests
3. **Authentication**: Use `cy.session()` for consistent auth state
4. **API Dependencies**: Mock external API calls

### Debug Mode

Run tests with additional debugging:

```bash
npm run cypress:run:headed  # See browser interactions
DEBUG=cypress:* npm run cypress:run  # Verbose logging
```

## Comparison with Playwright

This project also has Playwright E2E tests. Key differences:

- **Cypress**: Better developer experience, easier debugging, extensive community
- **Playwright**: Better cross-browser support, faster execution, built-in waiting
- **Choice**: Use Cypress for development and debugging, Playwright for CI/CD

Both frameworks coexist and can be used complementarily based on testing needs.