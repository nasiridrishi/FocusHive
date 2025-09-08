# Error Handling E2E Tests

This directory contains comprehensive end-to-end tests for error handling in the FocusHive application. These tests ensure that the application handles various error scenarios gracefully, provides clear feedback to users, and maintains data integrity.

## Overview

The error handling test suite covers **50+ scenarios** across multiple categories to ensure robust error management throughout the FocusHive application.

### Test Categories

1. **Network Error Handling** (6 tests)
2. **API Error Responses** (10 tests)
3. **Client-Side Errors** (6 tests)
4. **Form Validation Errors** (6 tests)
5. **WebSocket Errors** (5 tests)
6. **Authentication Errors** (5 tests)
7. **Data Loading Failures** (5 tests)
8. **User Feedback and Recovery** (6 tests)
9. **Performance Impact** (3 tests)

## Files Structure

```
error-handling/
├── README.md                    # This documentation
├── error-handling.spec.ts       # Main test suite (50+ test scenarios)
├── ../../pages/
│   └── ErrorHandlingPage.ts     # Page Object Model for error testing
└── ../../helpers/
    └── error-handling.helper.ts # Error simulation utilities
```

## Test Categories in Detail

### 1. Network Error Handling

Tests various network-related failures and recovery mechanisms:

- **Connection Timeout**: Simulates slow/hanging network requests
- **Network Disconnection/Reconnection**: Tests offline/online state handling
- **Slow Network Conditions**: Simulates 3G/2G network speeds
- **DNS Failures**: Tests domain name resolution failures
- **SSL Certificate Errors**: Tests certificate validation failures
- **Intermittent Connectivity**: Tests unreliable network conditions

**Key Verifications:**
- Timeout error messages are displayed
- Retry mechanisms are available
- Offline indicators appear when disconnected
- Connection recovery works automatically
- App remains functional during network issues

### 2. API Error Responses

Tests handling of various HTTP status codes and API failures:

- **400 Bad Request**: Form validation errors from server
- **401 Unauthorized**: Authentication failures
- **403 Forbidden**: Access permission errors
- **404 Not Found**: Missing resources
- **429 Rate Limiting**: Too many requests handling
- **500 Internal Server Error**: Server-side failures
- **502 Bad Gateway**: Proxy/gateway errors
- **503 Service Unavailable**: Service maintenance
- **Malformed JSON**: Invalid response parsing
- **Empty Responses**: Missing response data

**Key Verifications:**
- Appropriate error messages are shown
- Sensitive information is not leaked
- Rate limiting provides retry guidance
- Server errors show user-friendly messages
- Retry mechanisms are provided where appropriate

### 3. Client-Side Errors

Tests JavaScript runtime errors and client-side failures:

- **JavaScript Runtime Errors**: Uncaught exceptions
- **Memory Exhaustion**: Memory pressure handling
- **Chunk Loading Failures**: Dynamic import failures
- **localStorage Quota**: Storage limit exceeded
- **IndexedDB Failures**: Database access errors
- **Service Worker Failures**: PWA functionality errors

**Key Verifications:**
- Error boundaries catch component errors
- App remains responsive during memory pressure
- Fallback UI is shown for failed components
- Storage failures don't break functionality
- Service Worker errors don't prevent app usage

### 4. Form Validation Errors

Tests form validation and submission error handling:

- **Required Field Validation**: Empty field detection
- **Email Format Validation**: Invalid email formats
- **Password Strength Validation**: Weak password detection
- **Form Submission Prevention**: Validation error blocking
- **File Upload Validation**: Size/type restrictions
- **Duplicate Submission Prevention**: Multiple submission blocking
- **Async Validation Errors**: Server-side validation

**Key Verifications:**
- Clear validation error messages
- Form submission is blocked with errors
- Field-specific error highlighting
- File upload restrictions work
- Duplicate submissions are prevented

### 5. WebSocket Errors

Tests real-time communication error handling:

- **Connection Failures**: WebSocket connection errors
- **Unexpected Disconnection**: Connection loss handling
- **Message Delivery Failures**: Failed message transmission
- **Heartbeat Timeouts**: Connection health monitoring
- **WebSocket Recovery**: Automatic reconnection

**Key Verifications:**
- Connection errors are reported
- Automatic reconnection attempts
- Message delivery failures are detected
- Real-time features recover after reconnection

### 6. Authentication Errors

Tests authentication and session management errors:

- **Token Expiry**: Expired JWT handling
- **Session Timeout**: Session expiration handling
- **Concurrent Login Detection**: Multiple session handling
- **OAuth Flow Failures**: Third-party auth errors
- **Password Reset Errors**: Password recovery failures

**Key Verifications:**
- Token expiry redirects to login
- Session warnings are displayed
- Concurrent sessions are detected
- OAuth errors are handled gracefully
- Password reset failures show clear messages

### 7. Data Loading Failures

Tests dynamic content loading error handling:

- **Lazy Loading Failures**: Component loading errors
- **Infinite Scroll Failures**: Pagination errors
- **Pagination Errors**: Page navigation failures
- **Cache Invalidation Issues**: Stale cache handling
- **Stale Data Detection**: Outdated content detection

**Key Verifications:**
- Fallback UI for failed components
- Pagination errors are recoverable
- Cache corruption is handled
- Stale data warnings are shown

### 8. User Feedback and Recovery

Tests user experience during error states:

- **Clear Error Messages**: User-friendly error text
- **Actionable Recovery Options**: Retry mechanisms
- **Data Loss Prevention**: Form data preservation
- **Offline Mode Activation**: Offline functionality
- **Concurrent Error Handling**: Multiple error prioritization
- **Error Accessibility**: Screen reader compatibility

**Key Verifications:**
- Error messages are clear and helpful
- Users can recover from errors
- Data is preserved during errors
- Errors are accessible to all users
- Multiple errors are handled gracefully

### 9. Performance Impact

Tests performance during error conditions:

- **Error Detection Performance**: How quickly errors are detected
- **UI Responsiveness**: Interface performance during errors
- **Performance Recovery**: Performance restoration after error resolution

**Key Verifications:**
- Error detection is fast (<5 seconds)
- UI remains responsive (<1 second response)
- Memory usage stays reasonable (<100MB)
- Performance recovers after error resolution

## Key Features Tested

### Error Simulation Capabilities

The test suite includes comprehensive error simulation utilities:

```typescript
// Network conditions
await networkSim.goOffline();
await networkSim.simulate3G();
await networkSim.timeout(endpoint, 5000);

// API responses
await apiSim.serverError('/api/endpoint');
await apiSim.rateLimited('/api/endpoint', 60);
await apiSim.unauthorized('/api/endpoint');

// Client-side errors
await clientSim.runtimeError('Component failed');
await clientSim.memoryExhaustion();
await clientSim.chunkLoadFailure('dashboard');
```

### Recovery Testing

All error scenarios test recovery mechanisms:

```typescript
// Verify error is shown
await errorPage.waitForError();
await errorPage.verifyErrorMessage(/network error/i);

// Test recovery
await errorPage.verifyRetryMechanismAvailable();
await errorPage.clickRetry();

// Verify recovery success
await expect(page).toHaveURL(/\/dashboard/);
```

### Accessibility Testing

Error states are tested for accessibility compliance:

```typescript
// ARIA attributes
await expect(errorMessage).toHaveAttribute('role', 'alert');
await expect(errorMessage).toHaveAttribute('aria-live', 'polite');

// Focus management
const focusedElement = page.locator(':focus');
await expect(focusedElement).toBeVisible();
```

## Configuration

### Test Environment

The tests require the following environment setup:

```typescript
const testConfig = {
  identityServiceUrl: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081',
  backendServiceUrl: process.env.E2E_API_BASE_URL || 'http://localhost:8080',
  frontendUrl: process.env.E2E_BASE_URL || 'http://127.0.0.1:5173'
};
```

### Network Simulation

Tests use Chrome DevTools Protocol for network simulation:

```typescript
interface NetworkConditions {
  offline: boolean;
  downloadThroughput: number; // bytes/sec
  uploadThroughput: number;   // bytes/sec
  latency: number;            // milliseconds
}
```

### Error Injection

Multiple error injection methods are supported:

- **Route Interception**: Mock API responses
- **CDP Network Emulation**: Simulate network conditions
- **JavaScript Evaluation**: Trigger client-side errors
- **DOM Manipulation**: Simulate component failures

## Running the Tests

### Prerequisites

1. FocusHive backend services running (ports 8080-8087)
2. Frontend development server running (port 5173)
3. Test user accounts configured
4. Playwright installed and browsers configured

### Execution Commands

```bash
# Run all error handling tests
npm run test:e2e -- error-handling

# Run specific test category
npm run test:e2e -- error-handling --grep "Network Error"

# Run with debugging
npm run test:e2e -- error-handling --debug

# Run in headed mode (visible browser)
npm run test:e2e -- error-handling --headed

# Generate test report
npm run test:e2e -- error-handling --reporter=html
```

### Parallel Execution

Tests are designed to run in parallel safely:

```bash
# Run with multiple workers
npm run test:e2e -- error-handling --workers=4
```

## Test Data and Fixtures

### Test Users

```typescript
const TEST_USERS = {
  VALID_USER: {
    username: 'e2e_test_user',
    email: 'e2e.test@focushive.com',
    password: 'TestPassword123!'
  },
  INVALID_USER: {
    username: 'invalid_user',
    email: 'invalid@example.com',
    password: 'wrongpassword'
  }
};
```

### Error Scenarios

```typescript
const ERROR_SCENARIOS = [
  {
    name: 'Connection Timeout',
    type: 'network',
    endpoint: '/api/login',
    statusCode: 'timeout'
  },
  {
    name: 'Server Error',
    type: 'api',
    endpoint: '/api/data',
    statusCode: 500
  }
  // ... more scenarios
];
```

## Assertions and Verifications

### Error Message Verification

```typescript
// Verify error message appears
await expect(errorPage.errorMessage).toBeVisible();

// Verify error message content
await expect(errorPage.errorMessage).toContainText(/network error/i);

// Verify no sensitive information
const errorText = await errorPage.errorMessage.textContent();
expect(errorText?.toLowerCase()).not.toContain('password');
```

### Recovery Mechanism Testing

```typescript
// Verify retry button is available
await expect(errorPage.retryButton).toBeVisible();

// Test retry functionality
await errorPage.clickRetry();
await expect(errorPage.errorMessage).not.toBeVisible();
```

### Performance Assertions

```typescript
// Error detection time
expect(metrics.errorDetectionTime).toBeLessThan(5000);

// UI responsiveness
expect(metrics.uiResponseTime).toBeLessThan(1000);

// Memory usage
expect(metrics.memoryUsage).toBeLessThan(100 * 1024 * 1024);
```

## Debugging and Troubleshooting

### Common Issues

1. **Network Simulation Failures**
   - Ensure Chrome DevTools Protocol is enabled
   - Check for conflicting network policies
   - Verify CDP session is properly created

2. **Route Interception Issues**
   - Confirm route patterns match actual requests
   - Check route handler execution order
   - Verify route cleanup in test teardown

3. **Timing Issues**
   - Use appropriate timeouts for error detection
   - Wait for error states before assertions
   - Allow time for recovery mechanisms

### Debug Utilities

```typescript
// Enable verbose logging
const errorHelper = new ErrorHandlingHelper(page, context);
await errorHelper.enableDebugMode();

// Capture network requests
const requests = await page.evaluate(() => 
  performance.getEntriesByType('resource')
);

// Screenshot on error
await page.screenshot({ path: 'error-state.png' });
```

### Test Isolation

Each test ensures proper cleanup:

```typescript
test.afterEach(async () => {
  // Restore network conditions
  await errorHelper.cleanup();
  
  // Clear storage
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  
  // Remove route handlers
  await page.unrouteAll();
});
```

## Metrics and Reporting

### Coverage Metrics

The test suite measures:
- **Error Scenario Coverage**: 50+ distinct error conditions
- **Recovery Path Coverage**: All error types have recovery mechanisms
- **User Journey Coverage**: Errors tested across all major user flows

### Performance Metrics

- **Error Detection Time**: Average time to detect errors
- **Recovery Time**: Time from error to successful recovery  
- **UI Response Time**: Interface responsiveness during errors
- **Memory Usage**: Memory consumption during error states

### Accessibility Compliance

- **ARIA Compliance**: Error messages use proper ARIA attributes
- **Focus Management**: Focus is properly managed during errors
- **Screen Reader Support**: Errors are announced to assistive technology

## Future Enhancements

### Planned Improvements

1. **Error Analytics**: Track error frequency and patterns
2. **Visual Regression Testing**: Ensure error UI consistency  
3. **Performance Benchmarks**: Establish performance baselines
4. **Mobile Error Testing**: Test error handling on mobile devices
5. **Multi-Browser Testing**: Verify error handling across browsers

### Test Extensions

1. **Error Boundary Testing**: More comprehensive React error boundary tests
2. **PWA Error Handling**: Service Worker and offline functionality errors
3. **Real-User Monitoring**: Integration with RUM tools for production error tracking
4. **Chaos Engineering**: Random error injection for robustness testing

## Contributing

### Adding New Error Tests

1. Identify the error scenario to test
2. Add simulation logic to `ErrorHandlingHelper`
3. Create test case in appropriate describe block
4. Verify error detection and recovery
5. Update documentation

### Test Guidelines

- Each test should be independent and isolated
- Use descriptive test names and assertions
- Include both positive and negative test cases
- Test error recovery, not just error detection
- Verify user experience during error states
- Ensure tests are accessible and performant

### Code Standards

- Use TypeScript with strict type checking
- Follow existing page object patterns
- Include comprehensive error handling
- Add appropriate timeouts and waits
- Clean up resources in test teardown

---

This comprehensive error handling test suite ensures that FocusHive provides a robust and user-friendly experience even when errors occur, maintaining data integrity and providing clear recovery paths for all error scenarios.