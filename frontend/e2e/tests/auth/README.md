# Authentication E2E Tests

Comprehensive end-to-end testing suite for FocusHive authentication flows using Playwright.

## Overview

This test suite covers all critical authentication scenarios including:

- **User Registration** with email verification
- **Login/Logout** with valid/invalid credentials  
- **Password Reset** email flow
- **OAuth2 Integration** (Google, GitHub)
- **Session Management** and security
- **Cross-browser compatibility**
- **Mobile responsiveness**
- **Accessibility compliance**
- **Security features** and error handling

## Test Files

### Core Authentication Tests

- **`registration.spec.ts`** - User registration with email verification
  - Registration form validation
  - Email verification flow with MailHog
  - Security testing (XSS, SQL injection)
  - Mobile responsiveness
  - Accessibility compliance

- **`login-enhanced.spec.ts`** - Enhanced login flow testing
  - Valid/invalid credential scenarios
  - Account lockout and progressive delays
  - Session management ("Remember Me")
  - Security features and token handling
  - Performance and error handling

- **`password-reset.spec.ts`** - Password reset email flow
  - Reset request validation
  - Email token verification
  - New password form validation
  - Security features (timing attacks, token expiry)
  - Complete reset flow testing

- **`oauth-login.spec.ts`** - OAuth2 login integration
  - Google and GitHub OAuth flows
  - Account linking scenarios
  - OAuth error handling
  - Security features (PKCE, state validation)
  - Profile data population

- **`session-management.spec.ts`** - Session and logout functionality
  - Basic logout and session clearing
  - "Logout all devices" functionality
  - Session timeout handling
  - Concurrent session management
  - Security features and performance

### Helper Files

- **`auth-fixtures.ts`** - Test data, user profiles, and configuration constants
- **`auth-helpers.ts`** - Enhanced authentication helper functions with comprehensive flow support

## Prerequisites

### Required Services

1. **Frontend Development Server**
   ```bash
   npm run dev
   # Should be running on http://127.0.0.1:5173
   ```

2. **Identity Service** (for OAuth and user management)
   ```bash
   # Should be running on http://localhost:8081
   java -jar identity-service.jar
   ```

3. **Backend API Service**
   ```bash
   # Should be running on http://localhost:8080
   java -jar focushive-backend.jar
   ```

4. **MailHog** (for email verification testing)
   ```bash
   docker run --rm -p 1025:1025 -p 8025:8025 mailhog/mailhog
   # Web UI: http://localhost:8025
   ```

### Optional Services

- **Database** (PostgreSQL) for persistent user data
- **Redis** for session management testing

## Installation

1. Install Playwright and dependencies:
   ```bash
   npm install @playwright/test
   npx playwright install
   ```

2. Install browsers for cross-browser testing:
   ```bash
   npx playwright install chromium firefox webkit
   ```

## Running Tests

### All Authentication Tests
```bash
# Run all authentication tests
npx playwright test --config=e2e/playwright-auth.config.ts

# Run with UI mode for debugging
npx playwright test --config=e2e/playwright-auth.config.ts --ui

# Run in headed mode
npx playwright test --config=e2e/playwright-auth.config.ts --headed
```

### Specific Test Categories
```bash
# Registration tests only
npx playwright test e2e/tests/auth/registration.spec.ts

# Login tests only  
npx playwright test e2e/tests/auth/login-enhanced.spec.ts

# OAuth tests only
npx playwright test e2e/tests/auth/oauth-login.spec.ts

# Password reset tests only
npx playwright test e2e/tests/auth/password-reset.spec.ts

# Session management tests only
npx playwright test e2e/tests/auth/session-management.spec.ts
```

### Browser-Specific Testing
```bash
# Test on specific browser
npx playwright test --config=e2e/playwright-auth.config.ts --project=chromium-auth
npx playwright test --config=e2e/playwright-auth.config.ts --project=firefox-auth
npx playwright test --config=e2e/playwright-auth.config.ts --project=webkit-auth

# Mobile testing
npx playwright test --config=e2e/playwright-auth.config.ts --project=mobile-chrome-auth
```

### Debug Mode
```bash
# Debug specific test
npx playwright test e2e/tests/auth/login-enhanced.spec.ts --debug

# Debug with browser console
npx playwright test e2e/tests/auth/login-enhanced.spec.ts --headed --debug

# Generate test code
npx playwright codegen http://127.0.0.1:5173/login
```

## Environment Configuration

### Required Environment Variables
```bash
# Frontend URL
E2E_BASE_URL=http://127.0.0.1:5173

# Backend services
E2E_IDENTITY_API_URL=http://localhost:8081
E2E_API_BASE_URL=http://localhost:8080

# Email testing
E2E_MAILHOG_API_URL=http://localhost:8025
E2E_MAILHOG_WEB_URL=http://localhost:8025

# OAuth testing (optional)
E2E_OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
E2E_OAUTH_GITHUB_CLIENT_ID=your-github-client-id
```

### Environment File (.env.test)
```bash
# Create .env.test file in project root
E2E_BASE_URL=http://127.0.0.1:5173
E2E_IDENTITY_API_URL=http://localhost:8081
E2E_API_BASE_URL=http://localhost:8080
E2E_MAILHOG_API_URL=http://localhost:8025
E2E_MAILHOG_WEB_URL=http://localhost:8025
```

## Test Features

### Comprehensive Coverage

- **User Registration**: Form validation, email verification, security testing
- **Login/Logout**: Credential validation, session management, security features
- **Password Reset**: Email flow, token validation, security measures
- **OAuth Integration**: Multiple providers, account linking, error scenarios
- **Session Management**: Timeouts, concurrent sessions, security

### Cross-Browser Testing
- Chromium (Chrome)
- Firefox
- WebKit (Safari)
- Mobile browsers (iOS Safari, Android Chrome)

### Accessibility Testing
- Keyboard navigation
- Screen reader compatibility
- ARIA labels and roles
- High contrast mode
- Focus management

### Security Testing
- XSS protection
- SQL injection prevention
- CSRF protection
- Session hijacking prevention
- Token validation
- Rate limiting

### Performance Testing
- Response time validation
- Slow network simulation
- Load time measurements
- Token refresh performance

## Test Reports

### HTML Report
```bash
# Generate and view HTML report
npx playwright test --config=e2e/playwright-auth.config.ts
npx playwright show-report e2e-auth-report
```

### Test Results
- **HTML Report**: `e2e-auth-report/index.html`
- **JSON Results**: `e2e-auth-results.json`
- **JUnit XML**: `e2e-auth-results.xml`

### Screenshots and Videos
- **Screenshots**: Captured on test failure
- **Videos**: Recorded for failed tests
- **Traces**: Available for debugging with `--trace on`

## Troubleshooting

### Common Issues

1. **Services Not Running**
   ```bash
   # Check if services are accessible
   curl http://127.0.0.1:5173
   curl http://localhost:8081/health
   curl http://localhost:8080/health
   curl http://localhost:8025/api/v1/messages
   ```

2. **Test Data Conflicts**
   ```bash
   # Clear test data
   npx playwright test --config=e2e/playwright-auth.config.ts --project=cleanup
   ```

3. **Browser Issues**
   ```bash
   # Reinstall browsers
   npx playwright install --force
   ```

4. **Email Testing Not Working**
   ```bash
   # Start MailHog
   docker run --rm -p 1025:1025 -p 8025:8025 mailhog/mailhog
   
   # Check MailHog is running
   curl http://localhost:8025/api/v1/messages
   ```

### Debug Tips

1. **Use Debug Mode**: `--debug` flag to step through tests
2. **Enable Tracing**: `--trace on` to record test execution
3. **Check Screenshots**: Automatically captured on failures
4. **Review Logs**: Console output shows detailed test execution
5. **Use UI Mode**: `--ui` flag for interactive test running

## CI/CD Integration

### GitHub Actions
```yaml
- name: Run Authentication E2E Tests
  run: |
    npx playwright test --config=e2e/playwright-auth.config.ts
    
- name: Upload Test Results
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: auth-test-results
    path: |
      e2e-auth-report/
      e2e-auth-results.json
      e2e-auth-results.xml
```

### Test Parallelization
- Tests run in parallel by default
- CI mode uses 1 worker for stability
- Mobile and desktop tests can run simultaneously

## Contributing

### Adding New Tests

1. Create test file in `e2e/tests/auth/`
2. Use `EnhancedAuthHelper` for common operations
3. Follow existing patterns for test structure
4. Add test data to `auth-fixtures.ts` if needed
5. Update this README with new test information

### Test Conventions

- Use descriptive test names
- Group related tests with `test.describe()`
- Include accessibility tests for new features
- Test both positive and negative scenarios
- Add mobile responsiveness tests
- Include error handling tests

### Code Quality

- Follow TypeScript best practices
- Use proper types (no `any` types)
- Add JSDoc comments for helper functions
- Follow existing code style
- Use meaningful variable names