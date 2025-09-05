# Testing Guide

This guide covers the comprehensive testing setup for the FocusHive frontend application.

## Table of Contents

1. [Testing Philosophy](#testing-philosophy)
2. [Testing Stack](#testing-stack)
3. [Test Types](#test-types)
4. [Running Tests](#running-tests)
5. [Writing Tests](#writing-tests)
6. [Best Practices](#best-practices)
7. [CI/CD Integration](#cicd-integration)
8. [Troubleshooting](#troubleshooting)

## Testing Philosophy

Our testing approach follows the **Testing Pyramid** principle:

- **Unit Tests (70%)**: Fast, isolated tests for individual components and functions
- **Integration Tests (20%)**: Tests for component interactions and API integration
- **E2E Tests (10%)**: End-to-end user journey tests

We prioritize **testing behavior over implementation** and focus on **accessibility-first testing**.

## Testing Stack

### Core Testing Framework
- **Vitest**: Fast unit test runner with native TypeScript support
- **React Testing Library**: Component testing with user-centric approach
- **Playwright**: Cross-browser E2E testing
- **MSW (Mock Service Worker)**: API mocking for integration tests

### Accessibility Testing
- **jest-axe**: Automated accessibility testing in unit tests
- **@axe-core/playwright**: Accessibility testing in E2E tests

### Additional Tools
- **@testing-library/user-event**: Realistic user interaction simulation
- **@testing-library/jest-dom**: Extended Jest matchers for DOM testing

## Test Types

### 1. Unit Tests

Test individual components and functions in isolation.

```typescript
// Component test example
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from './Button';

describe('Button', () => {
  it('renders with correct text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument();
  });
});
```

### 2. Integration Tests

Test component interactions and API integration using MSW.

```typescript
// API integration test example
import { server } from '@/test-utils/msw-server';
import { http, HttpResponse } from 'msw';

describe('User Authentication', () => {
  it('handles login flow', async () => {
    // Override MSW handler for this test
    server.use(
      http.post('/api/auth/login', () => {
        return HttpResponse.json({ user: mockUser, token: 'abc123' });
      })
    );

    render(<LoginForm />);
    // Test login interaction
  });
});
```

### 3. E2E Tests

Test complete user workflows across the entire application.

```typescript
// E2E test example
import { test, expect } from '@playwright/test';

test('user can complete login flow', async ({ page }) => {
  await page.goto('/login');
  await page.fill('[name="email"]', 'user@example.com');
  await page.fill('[name="password"]', 'password');
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL('/dashboard');
});
```

### 4. Accessibility Tests

Ensure components are accessible to all users.

```typescript
// Accessibility test example
import { testA11y } from '@/test-utils/accessibility-utils';

describe('Button Accessibility', () => {
  it('has no accessibility violations', async () => {
    const { container } = render(<Button>Click me</Button>);
    await testA11y(container);
  });
});
```

## Running Tests

### Development Commands

```bash
# Run all unit tests in watch mode
npm run test

# Run all unit tests once
npm run test:run

# Run tests with coverage
npm run test:coverage

# Run E2E tests
npm run test:e2e

# Run E2E tests in headed mode
npm run test:e2e:headed

# Run E2E tests with UI
npm run test:e2e:ui

# Run all tests (unit + E2E)
npm run test:all

# Run tests in CI mode
npm run test:ci
```

### Test Filtering

```bash
# Run specific test file
npm run test Button.test.tsx

# Run tests matching pattern
npm run test auth

# Run tests in specific directory
npm run test src/components
```

## Writing Tests

### Test File Structure

```
src/
├── components/
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx     # Unit tests
│   │   └── index.ts
├── features/
│   ├── auth/
│   │   ├── components/
│   │   │   └── __tests__/      # Component tests
│   │   └── hooks/
│   │       └── __tests__/      # Hook tests
e2e/                            # E2E tests
├── auth.spec.ts
├── accessibility.spec.ts
└── app.spec.ts
```

### Custom Test Utilities

Use our custom render utilities for consistent test setup:

```typescript
import { renderWithProviders } from '@/test-utils/test-utils';

// Renders with all providers (Router, Theme, Auth, QueryClient)
renderWithProviders(<MyComponent />);

// Renders with specific providers only
renderWithAuth(<MyComponent />, mockUser);
renderWithRouter(<MyComponent />, ['/dashboard']);
renderWithTheme(<MyComponent />);
```

### MSW Setup

API mocking is automatically configured. Override handlers in tests:

```typescript
import { server } from '@/test-utils/msw-server';
import { http, HttpResponse } from 'msw';

// Override for specific test
server.use(
  http.get('/api/users', () => {
    return HttpResponse.json(mockUsers);
  })
);
```

### Accessibility Testing

```typescript
import { testA11y, createA11yTestSuite } from '@/test-utils/accessibility-utils';

// Manual accessibility test
await testA11y(container);

// Automated accessibility test suite
createA11yTestSuite('MyComponent', () => render(<MyComponent />));
```

## Best Practices

### General Testing Practices

1. **Test Behavior, Not Implementation**
   ```typescript
   // ❌ Testing implementation details
   expect(wrapper.find('.button')).toHaveLength(1);
   
   // ✅ Testing behavior
   expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
   ```

2. **Use Semantic Queries**
   ```typescript
   // Priority order for queries:
   // 1. getByRole
   // 2. getByLabelText
   // 3. getByPlaceholderText
   // 4. getByText
   // 5. getByDisplayValue
   // 6. getByAltText
   // 7. getByTitle
   // 8. getByTestId (last resort)
   ```

3. **Test User Interactions**
   ```typescript
   import userEvent from '@testing-library/user-event';
   
   const user = userEvent.setup();
   await user.click(screen.getByRole('button'));
   await user.type(screen.getByLabelText('Email'), 'test@example.com');
   ```

### Component Testing

1. **Test Props and State Changes**
   ```typescript
   it('shows loading state', () => {
     render(<Button loading>Save</Button>);
     expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
   });
   ```

2. **Test Event Handlers**
   ```typescript
   it('calls onClick when clicked', async () => {
     const handleClick = vi.fn();
     render(<Button onClick={handleClick}>Click me</Button>);
     
     await user.click(screen.getByRole('button'));
     expect(handleClick).toHaveBeenCalledTimes(1);
   });
   ```

3. **Test Edge Cases**
   ```typescript
   it('handles empty state', () => {
     render(<UserList users={[]} />);
     expect(screen.getByText('No users found')).toBeInTheDocument();
   });
   ```

### Hook Testing

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { useAuth } from './useAuth';

describe('useAuth', () => {
  it('starts with unauthenticated state', () => {
    const { result } = renderHook(() => useAuth());
    expect(result.current.isAuthenticated).toBe(false);
  });
});
```

### E2E Testing

1. **Use Page Object Model**
   ```typescript
   class LoginPage {
     constructor(private page: Page) {}
     
     async login(email: string, password: string) {
       await this.page.fill('[name="email"]', email);
       await this.page.fill('[name="password"]', password);
       await this.page.click('button[type="submit"]');
     }
   }
   ```

2. **Wait for Elements**
   ```typescript
   // Wait for element to appear
   await expect(page.locator('[data-testid="success"]')).toBeVisible();
   
   // Wait for navigation
   await page.waitForURL('/dashboard');
   ```

### Accessibility Testing

1. **Test Keyboard Navigation**
   ```typescript
   await testKeyboardNavigation(renderResult, [
     'button[name="Save"]',
     'input[name="email"]'
   ]);
   ```

2. **Test Focus Management**
   ```typescript
   await testFocusTrap(renderResult, triggerButton, '.modal');
   ```

3. **Test Screen Reader Support**
   ```typescript
   expect(screen.getByLabelText('Email address')).toBeInTheDocument();
   expect(screen.getByRole('alert')).toHaveTextContent('Invalid email');
   ```

## CI/CD Integration

Our GitHub Actions workflow runs:

1. **Lint and TypeScript checks**
2. **Unit tests with coverage**
3. **E2E tests across browsers**
4. **Accessibility audits**
5. **Visual regression tests**
6. **Bundle analysis**

### Coverage Requirements

- **Statements**: 70%
- **Branches**: 65%
- **Functions**: 65%
- **Lines**: 70%

### Quality Gates

Tests must pass these checks:
- Zero accessibility violations
- No TypeScript errors
- ESLint compliance
- Minimum coverage thresholds

## Troubleshooting

### Common Issues

1. **MSW handlers not working**
   ```typescript
   // Ensure server is started in test setup
   beforeAll(() => server.listen());
   afterEach(() => server.resetHandlers());
   afterAll(() => server.close());
   ```

2. **Async tests timing out**
   ```typescript
   // Use waitFor for async operations
   await waitFor(() => {
     expect(screen.getByText('Success')).toBeInTheDocument();
   });
   ```

3. **React Testing Library queries not found**
   ```typescript
   // Use debug to see current DOM
   screen.debug();
   
   // Check if element exists but not visible
   expect(screen.queryByText('Hidden')).not.toBeVisible();
   ```

4. **Playwright tests flaky**
   ```typescript
   // Add explicit waits
   await page.waitForSelector('[data-testid="loaded"]');
   
   // Use auto-waiting locators
   await expect(page.locator('text=Success')).toBeVisible();
   ```

### Debugging Tests

```bash
# Run single test file
npm run test Button.test.tsx

# Run tests in UI mode
npm run test:ui

# Debug Playwright tests
npm run test:e2e:headed

# Run tests with verbose output
npm run test -- --reporter=verbose
```

## Resources

- [Vitest Documentation](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Playwright Documentation](https://playwright.dev/)
- [MSW Documentation](https://mswjs.io/)
- [jest-axe Documentation](https://github.com/nickcolley/jest-axe)
- [Web Accessibility Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)