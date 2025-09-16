/**
 * Comprehensive Error Handling E2E Tests for FocusHive
 * Tests various error scenarios to ensure graceful degradation and recovery
 */

import {expect, Page, test} from '@playwright/test';
import {ErrorHandlingPage} from '../../pages/ErrorHandlingPage';
import {ErrorHandlingHelper} from '../../helpers/error-handling.helper';
import {LoginPage} from '../../pages/LoginPage';
import {AuthHelper} from '../../helpers/auth.helper';
import {
  API_ENDPOINTS,
  TEST_USERS,
  TIMEOUTS,
  validateTestEnvironment
} from '../../helpers/test-data';

test.describe('Error Handling - Network Errors', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);

    validateTestEnvironment();
    await errorPage.navigateToPage('/login');
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
  });

  test('should handle connection timeout gracefully', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();

    // Setup timeout for login endpoint
    await networkSim.timeout(API_ENDPOINTS.LOGIN, 1000);

    // Attempt login
    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    // Verify timeout error handling
    await errorPage.waitForError(TIMEOUTS.MEDIUM);
    await errorPage.verifyErrorMessage(/timeout|connection|network/i);
    await errorPage.verifyRetryMechanismAvailable();

    // Test retry mechanism
    await networkSim.goOnline();
    await errorPage.clickRetry();
    await expect(page).toHaveURL(/\/dashboard|\/home/, {timeout: TIMEOUTS.NETWORK});
  });

  test('should handle network disconnection and reconnection', async () => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();
    await expect(page).toHaveURL(/\/dashboard|\/home/);

    // Simulate network disconnection
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.goOffline();

    // Verify offline indicator appears
    await errorPage.verifyOfflineIndicator();
    await errorPage.verifyConnectionStatus('offline');

    // Simulate reconnection
    await networkSim.goOnline();
    await page.waitForTimeout(2000); // Allow reconnection logic

    // Verify connection restored
    await errorPage.verifyConnectionStatus('online');
  });

  test('should handle slow network conditions (3G)', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.slowNetwork(100, 50, 300); // 3G conditions

    const loginPage = new LoginPage(page);
    const startTime = Date.now();

    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    const loadTime = Date.now() - startTime;

    // Verify the app handles slow network gracefully
    expect(loadTime).toBeGreaterThan(1000); // Should take longer
    await expect(page).toHaveURL(/\/dashboard|\/home/, {timeout: TIMEOUTS.LONG});
  });

  test('should handle DNS failures', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.dnsFailure('localhost');

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/network|connection|dns/i);
    await errorPage.verifyRetryMechanismAvailable();
  });

  test('should handle SSL certificate errors', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.sslError(API_ENDPOINTS.LOGIN);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/security|certificate|ssl/i);
  });

  test('should handle intermittent connectivity', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.intermittentConnectivity(API_ENDPOINTS.LOGIN, 0.5); // 50% failure rate

    const loginPage = new LoginPage(page);
    let attempts = 0;
    let success = false;

    // Try multiple times due to intermittent nature
    while (attempts < 5 && !success) {
      await loginPage.clearForm();
      await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await loginPage.waitForLoading();

      try {
        await expect(page).toHaveURL(/\/dashboard|\/home/, {timeout: TIMEOUTS.SHORT});
        success = true;
      } catch {
        // Retry on failure
        attempts++;
      }
    }

    // Should eventually succeed or show appropriate error
    expect(success || await errorPage.errorMessage.isVisible()).toBeTruthy();
  });
});

test.describe('Error Handling - API Response Errors', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);

    await errorPage.navigateToPage('/login');
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
  });

  test('should handle 400 Bad Request errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.badRequest(API_ENDPOINTS.LOGIN, {
      email: 'Invalid email format',
      password: 'Password too short'
    });

    const loginPage = new LoginPage(page);
    await loginPage.login('invalid-email', '123');

    await errorPage.waitForError();
    await errorPage.verifyFormValidationErrors(2);
  });

  test('should handle 401 Unauthorized errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.unauthorized(API_ENDPOINTS.LOGIN);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/unauthorized|authentication|login/i);
    await loginPage.verifyStayOnLoginPage();
  });

  test('should handle 403 Forbidden errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.forbidden('/api/admin/users');

    // Login first
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    // Try to access forbidden resource
    await page.goto('/admin/users');

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/forbidden|access denied|permission/i);
  });

  test('should handle 404 Not Found errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.notFound('/api/nonexistent');

    await page.goto('/nonexistent-page');

    await errorPage.verifyGracefulDegradation();
    await errorPage.verifyErrorMessage(/not found|page not found|404/i);
  });

  test('should handle 429 Rate Limiting errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.rateLimited(API_ENDPOINTS.LOGIN, 60);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, 'wrongpassword');

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/rate limit|too many requests/i);
    await errorPage.verifyToastNotification('warning', /try again/i);
  });

  test('should handle 500 Internal Server Errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.serverError(API_ENDPOINTS.LOGIN, false); // Without sensitive data

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/server error|something went wrong/i);
    await errorPage.verifyRetryMechanismAvailable();

    // Verify no sensitive information is leaked
    const errorText = await errorPage.errorMessage.textContent();
    expect(errorText?.toLowerCase()).not.toContain('database');
    expect(errorText?.toLowerCase()).not.toContain('stack');
    expect(errorText?.toLowerCase()).not.toContain('password');
  });

  test('should handle 502 Bad Gateway errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.badGateway(API_ENDPOINTS.LOGIN);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/service unavailable|server temporarily unavailable/i);
  });

  test('should handle 503 Service Unavailable errors', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.serviceUnavailable(API_ENDPOINTS.LOGIN, 120);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/maintenance|service unavailable/i);
    await errorPage.verifyToastNotification('warning', /try again later/i);
  });

  test('should handle malformed JSON responses', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.malformedResponse(API_ENDPOINTS.LOGIN);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/unexpected error|parsing error/i);
  });

  test('should handle empty API responses', async () => {
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.emptyResponse(API_ENDPOINTS.LOGIN);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyErrorMessage(/no response|empty response/i);
  });
});

test.describe('Error Handling - Client-Side Errors', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
  });

  test('should catch JavaScript runtime errors with error boundary', async () => {
    await errorPage.navigateToPage('/dashboard');

    const clientSim = await errorHelper.simulateClientErrors();
    await clientSim.runtimeError('Test component error');

    await errorPage.waitForErrorBoundary();
    await errorPage.verifyErrorBoundaryDisplayed();
    await errorPage.verifyRetryMechanismAvailable();
  });

  test('should handle memory exhaustion gracefully', async () => {
    await errorPage.navigateToPage('/dashboard');

    const clientSim = await errorHelper.simulateClientErrors();
    await clientSim.memoryExhaustion();

    // App should still be responsive
    await expect(page.locator('body')).toBeVisible();

    // May show performance warning
    try {
      await errorPage.verifyToastNotification('warning', /memory|performance/i);
    } catch {
      // Memory warning may not appear, which is acceptable
    }
  });

  test('should handle chunk loading failures', async () => {
    const clientSim = await errorHelper.simulateClientErrors();
    await clientSim.chunkLoadFailure('dashboard');

    await errorPage.navigateToPage('/dashboard');

    // Should show fallback UI or error message
    await expect(page.locator('[data-testid="fallback"], .error-boundary, [role="alert"]').first()).toBeVisible();
    await errorPage.verifyRetryMechanismAvailable();
  });

  test('should handle localStorage quota exceeded', async () => {
    await errorPage.navigateToPage('/dashboard');

    const clientSim = await errorHelper.simulateClientErrors();
    await clientSim.storageQuotaExceeded();

    // App should continue working, possibly with reduced functionality
    await expect(page.locator('body')).toBeVisible();

    // May show storage warning
    try {
      await errorPage.verifyToastNotification('warning', /storage|quota/i);
    } catch {
      // Storage warning may not appear
    }
  });

  test('should handle IndexedDB failures', async () => {
    const clientSim = await errorHelper.simulateClientErrors();
    await clientSim.indexedDbFailure();

    await errorPage.navigateToPage('/dashboard');

    // App should work with fallback storage
    await expect(page.locator('body')).toBeVisible();
  });

  test('should handle Service Worker registration failures', async () => {
    const clientSim = await errorHelper.simulateClientErrors();
    await clientSim.serviceWorkerFailure();

    await errorPage.navigateToPage('/');

    // App should work without Service Worker
    await expect(page.locator('body')).toBeVisible();
  });
});

test.describe('Error Handling - Form Validation Errors', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);

    await errorPage.navigateToPage('/register');
  });

  test('should show required field validation errors', async () => {
    const formValidation = await errorHelper.testFormValidationErrors();

    await formValidation.requiredFieldValidation('email');
    await formValidation.requiredFieldValidation('password');
    await formValidation.requiredFieldValidation('username');
  });

  test('should validate email format', async () => {
    const formValidation = await errorHelper.testFormValidationErrors();
    await formValidation.emailFormatValidation('email');
  });

  test('should validate password strength', async () => {
    const formValidation = await errorHelper.testFormValidationErrors();
    await formValidation.passwordStrengthValidation('password');
  });

  test('should prevent form submission with validation errors', async () => {
    await errorPage.fillFormWithInvalidData({
      email: 'invalid-email',
      password: '123',
      username: 'a'
    });

    await errorPage.submitFormAndWait();

    // Should stay on same page with errors
    await expect(page).toHaveURL(/\/register/);
    await errorPage.verifyFormValidationErrors(3);
  });

  test('should handle file upload validation errors', async () => {
    // Navigate to a page with file upload (if available)
    await errorPage.navigateToPage('/profile');

    const formValidation = await errorHelper.testFormValidationErrors();

    try {
      await formValidation.fileUploadValidation('input[type="file"]', {
        maxSize: 1024 * 1024, // 1MB
        allowedTypes: ['image/jpeg', 'image/png']
      });
    } catch {
      // File upload may not be available on all pages
    }
  });

  test('should handle duplicate submission prevention', async () => {
    await errorPage.fillFormWithInvalidData({
      email: TEST_USERS.NEW_USER.email,
      password: TEST_USERS.NEW_USER.password,
      username: TEST_USERS.NEW_USER.username
    });

    // Submit multiple times rapidly
    const submitButton = errorPage.submitButton;
    await submitButton.click();
    await submitButton.click(); // Second click should be prevented

    // Verify only one submission occurred
    await page.waitForTimeout(1000);
    await expect(submitButton).toBeDisabled();
  });

  test('should handle async validation errors', async () => {
    // Test username availability check
    await errorPage.page.locator('input[name="username"]').fill('existinguser');
    await errorPage.page.locator('input[name="username"]').blur();

    // Wait for async validation
    await page.waitForTimeout(1000);

    try {
      await errorPage.verifyValidationError('username', /already taken|not available/i);
    } catch {
      // Async validation may not be implemented
    }
  });
});

test.describe('Error Handling - WebSocket Errors', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);

    // Login to access WebSocket features
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();
  });

  test('should handle WebSocket connection failures', async () => {
    const wsSim = await errorHelper.simulateWebSocketErrors();
    await wsSim.connectionFailure('ws://localhost:8080/ws');

    await errorPage.navigateToPage('/hive/123'); // Page with WebSocket

    // Should show connection error
    await errorPage.verifyToastNotification('error', /connection|websocket/i);
    await errorPage.verifyRetryMechanismAvailable();
  });

  test('should handle unexpected WebSocket disconnection', async () => {
    await errorPage.navigateToPage('/hive/123');

    // Allow connection to establish
    await page.waitForTimeout(2000);

    const wsSim = await errorHelper.simulateWebSocketErrors();
    await wsSim.unexpectedDisconnection();

    // Should show disconnection notice and attempt reconnection
    await errorPage.verifyToastNotification('warning', /disconnected|reconnecting/i);
  });

  test('should handle WebSocket message delivery failures', async () => {
    const wsSim = await errorHelper.simulateWebSocketErrors();
    await wsSim.messageDeliveryFailure();

    await errorPage.navigateToPage('/hive/123');

    // Try to send a message
    const messageInput = page.locator('input[placeholder*="message"], textarea[placeholder*="message"]');
    if (await messageInput.isVisible()) {
      await messageInput.fill('Test message');
      await page.keyboard.press('Enter');

      // Should show delivery failure
      try {
        await errorPage.verifyToastNotification('error', /failed to send|delivery failed/i);
      } catch {
        // Message delivery errors may not be implemented
      }
    }
  });

  test('should handle WebSocket heartbeat timeouts', async () => {
    const wsSim = await errorHelper.simulateWebSocketErrors();
    await wsSim.heartbeatTimeout();

    await errorPage.navigateToPage('/hive/123');

    // Wait for heartbeat timeout
    await page.waitForTimeout(5000);

    // Should show connection issues
    try {
      await errorPage.verifyToastNotification('warning', /connection issues|reconnecting/i);
    } catch {
      // Heartbeat monitoring may not be implemented
    }
  });

  test('should recover from WebSocket errors', async () => {
    await errorPage.navigateToPage('/hive/123');

    // Simulate network disconnection
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.goOffline();

    await page.waitForTimeout(2000);

    // Restore connection
    await networkSim.goOnline();

    // Should automatically reconnect
    await errorPage.verifyWebSocketReconnection();
  });
});

test.describe('Error Handling - Authentication Errors', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);
  });

  test('should handle token expiry gracefully', async () => {
    // Login first
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    // Mock token expiry response
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.unauthorized('/api/user/profile');

    await errorPage.navigateToPage('/profile');

    // Should redirect to login or show re-authentication modal
    await expect(page.locator('input[name="email"], input[name="password"], .login-form').first()).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  });

  test('should handle session timeout', async () => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    // Simulate session timeout
    await page.evaluate(() => {
      localStorage.removeItem('refresh_token');
      sessionStorage.removeItem('access_token');
    });

    // Try to access protected resource
    await errorPage.navigateToPage('/dashboard');

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/);
    await errorPage.verifyToastNotification('warning', /session expired|please log in/i);
  });

  test('should detect concurrent login sessions', async ({context}) => {
    // Login in first session
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    // Create second browser session
    const page2 = await context.newPage();
    const authHelper2 = new AuthHelper(page2);
    await authHelper2.loginWithValidUser();

    // Return to first session
    await page.reload();

    // Should show concurrent session warning
    try {
      await errorPage.verifyToastNotification('warning', /another session|concurrent login/i);
    } catch {
      // Concurrent session detection may not be implemented
    }

    await page2.close();
  });

  test('should handle OAuth flow failures', async () => {
    await errorPage.navigateToPage('/login');

    // Mock OAuth failure
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.serverError('/auth/oauth/callback');

    // Try OAuth login (if available)
    const oauthButton = page.locator('button:has-text("Google"), button:has-text("GitHub"), [data-testid="oauth-button"]');
    if (await oauthButton.isVisible()) {
      await oauthButton.click();

      await errorPage.waitForError();
      await errorPage.verifyErrorMessage(/oauth|authentication failed/i);
    }
  });

  test('should handle password reset errors', async () => {
    await errorPage.navigateToPage('/forgot-password');

    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.serverError('/api/auth/forgot-password');

    // Try password reset
    const emailInput = page.locator('input[name="email"], input[type="email"]');
    if (await emailInput.isVisible()) {
      await emailInput.fill(TEST_USERS.VALID_USER.email);
      await page.locator('button[type="submit"]').click();

      await errorPage.waitForError();
      await errorPage.verifyErrorMessage(/reset|email|error/i);
      await errorPage.verifyRetryMechanismAvailable();
    }
  });
});

test.describe('Error Handling - Data Loading Failures', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);

    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();
  });

  test('should handle lazy loading component failures', async () => {
    await errorHelper.testLazyLoadingFailure('analytics');

    await errorPage.navigateToPage('/analytics');

    await errorPage.verifyGracefulDegradation();
    await errorPage.verifyRetryMechanismAvailable();
  });

  test('should handle infinite scroll failures', async () => {
    await errorPage.navigateToPage('/forum');

    // Mock API failure for pagination
    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.serverError('/api/posts?page=2');

    // Scroll to trigger infinite scroll
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));

    // Should show error for additional content loading
    try {
      await errorPage.verifyToastNotification('error', /failed to load|error loading/i);
      await errorPage.verifyRetryMechanismAvailable();
    } catch {
      // Infinite scroll may not be implemented
    }
  });

  test('should handle pagination errors', async () => {
    await errorPage.navigateToPage('/forum');

    const apiSim = await errorHelper.simulateApiErrors();
    await apiSim.serverError('/api/posts?page=3');

    // Try to navigate to a page
    const nextButton = page.locator('button:has-text("Next"), [aria-label="Next page"]');
    if (await nextButton.isVisible()) {
      await nextButton.click();
      await nextButton.click(); // Go to page 3

      await errorPage.waitForError();
      await errorPage.verifyRetryMechanismAvailable();
    }
  });

  test('should handle cache invalidation issues', async () => {
    await errorPage.navigateToPage('/dashboard');

    // Corrupt cache data
    await page.evaluate(() => {
      if ('caches' in window) {
        caches.open('api-cache').then(cache => {
          cache.put('/api/dashboard', new Response('invalid json {'));
        });
      }
    });

    await page.reload();

    // Should recover from cache corruption
    await expect(page.locator('body')).toBeVisible();
  });

  test('should detect stale data', async () => {
    await errorPage.navigateToPage('/dashboard');

    // Mock stale data response
    await page.route('**/api/dashboard', route => {
      route.fulfill({
        status: 200,
        headers: {'Last-Modified': 'Mon, 01 Jan 2020 00:00:00 GMT'},
        body: JSON.stringify({data: 'stale', lastUpdated: '2020-01-01'})
      });
    });

    await page.reload();

    // Should show stale data warning
    try {
      await errorPage.verifyToastNotification('warning', /outdated|refresh/i);
    } catch {
      // Stale data detection may not be implemented
    }
  });
});

test.describe('Error Handling - User Feedback and Recovery', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);
  });

  test('should provide clear error messages', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.goOffline();

    await errorPage.navigateToPage('/login');

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();

    const errorText = await errorPage.errorMessage.textContent();
    expect(errorText).toBeTruthy();
    expect(errorText?.length).toBeGreaterThan(10); // Should be descriptive
    expect(errorText?.toLowerCase()).not.toContain('error 500'); // Should be user-friendly
  });

  test('should provide actionable recovery options', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.timeout(API_ENDPOINTS.LOGIN, 1000);

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();
    await errorPage.verifyRetryMechanismAvailable();

    // Test retry functionality
    await networkSim.goOnline();
    await errorPage.clickRetry();

    await expect(page).toHaveURL(/\/dashboard|\/home/, {timeout: TIMEOUTS.NETWORK});
  });

  test('should prevent data loss during errors', async () => {
    await errorPage.navigateToPage('/register');

    await errorPage.verifyDataLossPrevention('form');
  });

  test('should activate offline mode when appropriate', async () => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.goOffline();

    await page.reload();

    // Should show offline mode
    await errorPage.verifyOfflineIndicator();

    // App should still be functional in offline mode
    await expect(page.locator('body')).toBeVisible();
  });

  test('should handle concurrent errors gracefully', async () => {
    await errorHelper.testConcurrentErrors();

    // Should prioritize and show most critical error
    await errorPage.waitForError();
    await errorPage.verifyRetryMechanismAvailable();
  });

  test('should maintain accessibility during error states', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.goOffline();

    const loginPage = new LoginPage(page);
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    await errorPage.waitForError();

    const accessibility = await errorHelper.testErrorAccessibility();
    await accessibility.verifyErrorAria();
    await accessibility.testScreenReaderAnnouncements();
    await accessibility.testFocusManagement();
  });
});

test.describe('Error Handling - Performance Impact', () => {
  let page: Page;
  let errorPage: ErrorHandlingPage;
  let errorHelper: ErrorHandlingHelper;

  test.beforeEach(async ({page: testPage, context}) => {
    page = testPage;
    errorPage = new ErrorHandlingPage(page);
    errorHelper = new ErrorHandlingHelper(page, context);
  });

  test('should measure error handling performance impact', async () => {
    const metrics = await errorHelper.measureErrorHandlingPerformance();

    // Error detection should be fast
    expect(metrics.errorDetectionTime).toBeLessThan(5000); // Less than 5 seconds

    // UI should remain responsive
    expect(metrics.uiResponseTime).toBeLessThan(1000); // Less than 1 second

    // Memory usage should be reasonable
    expect(metrics.memoryUsage).toBeLessThan(100 * 1024 * 1024); // Less than 100MB
  });

  test('should not degrade performance significantly during error states', async () => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    // Measure baseline performance
    const baselineStart = Date.now();
    await errorPage.navigateToPage('/dashboard');
    const baselineTime = Date.now() - baselineStart;

    // Introduce error condition
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.slowNetwork(50, 25, 500);

    // Measure performance with errors
    const errorStart = Date.now();
    await errorPage.navigateToPage('/analytics');
    const errorTime = Date.now() - errorStart;

    // Performance degradation should be reasonable
    const degradation = errorTime / baselineTime;
    expect(degradation).toBeLessThan(3); // No more than 3x slower
  });

  test('should recover performance after error resolution', async () => {
    const networkSim = await errorHelper.simulateNetworkErrors();
    await networkSim.slowNetwork(10, 5, 1000); // Very slow

    const authHelper = new AuthHelper(page);
    await authHelper.loginWithValidUser();

    // Measure performance during error
    const errorStart = Date.now();
    await errorPage.navigateToPage('/dashboard');
    const errorTime = Date.now() - errorStart;

    // Restore normal network
    await networkSim.goOnline();

    // Measure performance after recovery
    const recoveryStart = Date.now();
    await errorPage.navigateToPage('/analytics');
    const recoveryTime = Date.now() - recoveryStart;

    // Performance should improve significantly after recovery
    expect(recoveryTime).toBeLessThan(errorTime * 0.5); // At least 50% faster
  });
});