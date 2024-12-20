# E2E Tests for FocusHive Frontend

This directory contains modern End-to-End (E2E) tests using Playwright for the FocusHive frontend application.

## 🚀 Quick Start

```bash
# Install Playwright browsers (first time only)
npm run test:e2e:install

# Run all E2E tests
npm run test:e2e

# Run tests in UI mode (interactive)
npm run test:e2e:ui

# Run tests in headed mode (see browser)
npm run test:e2e:headed

# Debug tests
npm run test:e2e:debug
```

## 📁 Structure

```
tests/e2e/
├── auth/                 # Authentication tests
├── navigation/           # Navigation and routing tests  
├── core/                 # Core feature tests
├── pages/                # Page Object Models
├── helpers/              # Test utilities and helpers
├── fixtures/             # Test constants and fixtures
├── auth.setup.ts         # Authentication setup
├── global-setup.ts       # Global test setup
└── global-teardown.ts    # Global test cleanup
```

## 🧪 Available Test Scripts

- `npm run test:e2e` - Run all E2E tests
- `npm run test:e2e:ui` - Run tests in interactive UI mode
- `npm run test:e2e:headed` - Run tests with browser visible
- `npm run test:e2e:debug` - Debug tests step-by-step
- `npm run test:e2e:auth` - Run only authentication tests
- `npm run test:e2e:nav` - Run only navigation tests
- `npm run test:e2e:report` - Show test results report
- `npm run test:all` - Run both unit tests and E2E tests

## 🏗 Architecture

### Page Object Model
Tests use the Page Object Model pattern for maintainability:
- `BasePage` - Common functionality for all pages
- `LoginPage` - Login page interactions
- `RegistrationPage` - Registration page interactions

### Test Data Management
- `TestDataManager` - Generates and manages test data
- Automatic cleanup of test data after tests complete
- Unique test users for each test run

### Authentication Setup
- Automatic authentication state management
- Tests can run with or without authentication
- Shared authentication state across test projects

## ⚙️ Configuration

Tests are configured in `playwright.config.ts` with:
- Multiple browser support (Chrome, Firefox, Safari)
- Mobile device testing
- Automatic retry on CI
- Screenshot and video capture on failures
- Parallel test execution

## 🐛 Debugging

1. **Visual debugging**: Use `npm run test:e2e:ui`
2. **Step-by-step debugging**: Use `npm run test:e2e:debug`
3. **View test reports**: Use `npm run test:e2e:report`
4. **Screenshots**: Automatically captured on test failures
5. **Videos**: Recorded for failing tests

## 📝 Writing Tests

### Basic Test Structure
```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login-page';

test.describe('Feature Name', () => {
  test('should do something', async ({ page }) => {
    const loginPage = new LoginPage(page);
    
    await loginPage.goto();
    await loginPage.login('user@example.com', 'password');
    
    await expect(page).toHaveURL(/dashboard/);
  });
});
```

### Using Test Data
```typescript
import { TestDataManager } from '../helpers/test-data-manager';

test('should register new user', async ({ page }) => {
  const testDataManager = new TestDataManager();
  const testUser = testDataManager.generateTestUser();
  
  // Use testUser in your test...
});
```

## 🚨 Best Practices

1. **Use Page Objects** - Always use page object models for page interactions
2. **Unique Test Data** - Generate unique test data to avoid conflicts  
3. **Wait Strategies** - Use proper waiting strategies, avoid `page.waitForTimeout()`
4. **Descriptive Tests** - Write clear, descriptive test names and descriptions
5. **Clean Up** - Tests automatically clean up test data
6. **Parallel Safe** - Write tests that can run in parallel safely

## 🔧 Environment Setup

Tests run against the local development server by default:
- Frontend: `http://localhost:5173`
- Backend services should be running on their respective ports

The test configuration will automatically start the frontend dev server if it's not already running.

## 📊 CI/CD Integration

Tests are configured for CI environments with:
- Automatic retry on failure
- Junit XML reports for CI integration
- HTML reports for detailed analysis
- Screenshot and video artifacts on failures

## 🆘 Troubleshooting

**Tests fail with "service not available":**
- Make sure backend services are running
- Check that ports 8080, 8081, etc. are not blocked

**Browser issues:**
- Run `npm run test:e2e:install` to install browsers
- Check that you have sufficient disk space

**Authentication issues:**
- Tests create their own test users
- Check that the identity service is running on port 8081

**Slow tests:**
- Tests run in parallel by default
- Use `--workers=1` for sequential execution if needed