# E2E Testing Suite

## Overview
Comprehensive end-to-end testing using Playwright for cross-browser and mobile testing.

## Test Categories

### Feature Tests
- **auth**: Authentication flows (login, signup, OAuth)
- **hive**: Virtual workspace operations
- **timer**: Focus timer functionality
- **chat**: Real-time messaging
- **analytics**: Dashboard and reporting
- **forum**: Community discussions
- **buddy**: Accountability partner features
- **gamification**: Points and achievements

### Quality Tests
- **accessibility**: WCAG 2.1 AA compliance
- **performance**: Load times and Core Web Vitals
- **security**: XSS, CSRF, authentication
- **error-handling**: Error recovery and fallbacks
- **data-integrity**: Data consistency checks

### Platform Tests
- **cross-browser**: Chrome, Firefox, Safari, Edge
- **mobile**: Responsive design and touch interactions
- **network-error-fallback**: Offline and slow network

## Running Tests

```bash
# All E2E tests
npm run test:e2e

# Specific category
npm run test:e2e -- tests/auth

# With UI
npm run test:e2e:ui

# Specific browser
npm run test:e2e:chromium
npm run test:e2e:firefox
npm run test:e2e:webkit

# Mobile viewports
npm run test:e2e:mobile
```

## Writing Tests

```typescript
import { test, expect } from '@playwright/test';

test.describe('Feature', () => {
  test('should perform action', async ({ page }) => {
    await page.goto('/');
    await page.click('button[aria-label="Action"]');
    await expect(page.locator('.result')).toBeVisible();
  });
});
```

## Configuration
See `playwright.config.ts` for browser settings, timeouts, and viewport configurations.