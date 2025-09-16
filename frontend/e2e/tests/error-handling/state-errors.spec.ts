/**
 * State Management Error Handling E2E Tests for FocusHive
 * Comprehensive testing of state management failures, corruption, and recovery
 *
 * Test Coverage:
 * - Corrupted local storage and session storage
 * - Session expiration during operations
 * - Concurrent modification conflicts
 * - Optimistic locking failures
 * - Cache invalidation issues
 * - State synchronization errors
 * - Redux store corruption
 * - Invalid state transitions
 * - Memory leaks and cleanup
 */

import {BrowserContext, expect, Page, test} from '@playwright/test';
import {ErrorHandlingHelper} from '../../helpers/error-handling.helper';
import {AuthHelper} from '../../helpers/auth-helpers';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('State Management Error Handling', () => {
  let page: Page;
  let context: BrowserContext;
  let errorHelper: ErrorHandlingHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page: testPage, context: testContext}) => {
    page = testPage;
    context = testContext;
    errorHelper = new ErrorHandlingHelper(page, context);
    authHelper = new AuthHelper(page);

    validateTestEnvironment();

    // Set up state monitoring
    await errorHelper.setupStateMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
    await errorHelper.clearStateIssues();
  });

  test.describe('Local Storage Corruption', () => {
    test('should detect and recover from corrupted local storage', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Wait for normal state to load
      await expect(page.locator('[data-testid="dashboard-loaded"]')).toBeVisible();

      // Corrupt local storage data
      await page.evaluate(() => {
        localStorage.setItem('user-preferences', '{invalid json}');
        localStorage.setItem('hive-cache', 'corrupted-data');
        localStorage.setItem('timer-state', '{"duration":null,"status":"invalid"}');
      });

      // Refresh to trigger state loading
      await page.reload();

      // Should detect corruption and show recovery dialog
      await expect(page.locator('[data-testid="storage-corruption-detected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should offer recovery options
      await expect(page.locator('[data-testid="clear-corrupted-data"]')).toBeVisible();
      await expect(page.locator('[data-testid="restore-defaults"]')).toBeVisible();
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();

      // Test data clearing recovery
      await page.click('[data-testid="clear-corrupted-data"]');

      // Should successfully recover with defaults
      await expect(page.locator('[data-testid="recovery-successful"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();

      // Should have cleared corrupted data
      const preferences = await page.evaluate(() => localStorage.getItem('user-preferences'));
      expect(preferences).toBeFalsy();
    });

    test('should handle quota exceeded errors gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Fill up local storage to near quota limit
      await page.evaluate(() => {
        try {
          const largeData = 'A'.repeat(1024 * 1024); // 1MB chunks
          for (let i = 0; i < 10; i++) {
            localStorage.setItem(`large-data-${i}`, largeData);
          }
        } catch {
          // Storage might already be full
        }
      });

      // Try to save user preferences (should trigger quota exceeded)
      await page.goto('/settings');
      await page.click('[data-testid="theme-dark"]');
      await page.click('[data-testid="save-preferences"]');

      // Should detect quota exceeded and show appropriate error
      await expect(page.locator('[data-testid="storage-quota-exceeded"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show storage management options
      await expect(page.locator('[data-testid="storage-quota-message"]')).toContainText(/storage.*full|quota.*exceeded/i);
      await expect(page.locator('[data-testid="clear-cache-data"]')).toBeVisible();
      await expect(page.locator('[data-testid="manage-storage"]')).toBeVisible();

      // Test cache clearing
      await page.click('[data-testid="clear-cache-data"]');

      // Should free up space and allow saving
      await expect(page.locator('[data-testid="storage-cleared"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Retry saving preferences
      await page.click('[data-testid="save-preferences"]');
      await expect(page.locator('[data-testid="preferences-saved"]')).toBeVisible();
    });

    test('should handle missing critical local storage data', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');

      // Start a timer to create state
      await page.fill('[data-testid="task-description"]', 'Critical state test');
      await page.click('[data-testid="start-timer"]');

      // Wait for timer to run
      await page.waitForTimeout(2000);

      // Remove critical timer state
      await page.evaluate(() => {
        localStorage.removeItem('current-timer');
        localStorage.removeItem('timer-history');
        sessionStorage.removeItem('timer-session');
      });

      // Refresh page
      await page.reload();

      // Should detect missing critical state
      await expect(page.locator('[data-testid="critical-state-missing"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should offer state recovery options
      await expect(page.locator('[data-testid="recover-timer-state"]')).toBeVisible();
      await expect(page.locator('[data-testid="start-fresh"]')).toBeVisible();

      // Should not crash or show broken UI
      await expect(page.locator('[data-testid="timer-container"]')).toBeVisible();

      // Test starting fresh
      await page.click('[data-testid="start-fresh"]');

      // Should reset to clean state
      await expect(page.locator('[data-testid="timer-ready"]')).toBeVisible();
      await expect(page.locator('[data-testid="task-description"]')).toHaveValue('');
    });
  });

  test.describe('Session Management', () => {
    test('should handle session expiration during active operations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Start some activity
      await page.fill('[data-testid="chat-input"]', 'Testing session expiration');

      // Mock session expiration
      await context.route('**/api/**', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Unauthorized',
            message: 'Session expired',
            code: 'SESSION_EXPIRED'
          })
        });
      });

      // Try to send message
      await page.press('[data-testid="chat-input"]', 'Enter');

      // Should detect session expiration
      await expect(page.locator('[data-testid="session-expired-dialog"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should preserve user data
      await expect(page.locator('[data-testid="chat-input"]')).toHaveValue('Testing session expiration');

      // Should offer re-authentication options
      await expect(page.locator('[data-testid="reauth-inline"]')).toBeVisible();
      await expect(page.locator('[data-testid="reauth-redirect"]')).toBeVisible();

      // Should show data preservation notice
      await expect(page.locator('[data-testid="data-preserved-notice"]')).toContainText(/data.*preserved|will.*saved/i);

      // Test inline re-authentication
      await page.click('[data-testid="reauth-inline"]');

      await expect(page.locator('[data-testid="inline-login-form"]')).toBeVisible();

      // Clear route mock for successful login
      await context.unroute('**/api/**');

      await page.fill('[data-testid="reauth-password"]', TEST_USERS.VALID_USER.password);
      await page.click('[data-testid="reauth-submit"]');

      // Should restore session and retry operation
      await expect(page.locator('[data-testid="session-restored"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Should send queued message
      await expect(page.locator('[data-testid="chat-message"]:has-text("Testing session expiration")')).toBeVisible();
    });

    test('should handle concurrent session conflicts', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Simulate another session login for same user
      await errorHelper.simulateSessionConflict();

      // Should detect session conflict
      await expect(page.locator('[data-testid="session-conflict-detected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show conflict details
      const conflictMessage = page.locator('[data-testid="conflict-message"]');
      await expect(conflictMessage).toContainText(/another.*session|different.*device/i);

      // Should offer conflict resolution options
      await expect(page.locator('[data-testid="take-over-session"]')).toBeVisible();
      await expect(page.locator('[data-testid="logout-current"]')).toBeVisible();
      await expect(page.locator('[data-testid="view-active-sessions"]')).toBeVisible();

      // Should prevent unauthorized actions
      await expect(page.locator('[data-testid="actions-suspended"]')).toBeVisible();

      // Test session takeover
      await page.click('[data-testid="take-over-session"]');

      // Should confirm takeover action
      await expect(page.locator('[data-testid="confirm-takeover"]')).toBeVisible();
      await page.click('[data-testid="confirm-takeover"]');

      // Should restore normal operation
      await expect(page.locator('[data-testid="session-restored"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
    });

    test('should handle refresh token expiration', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock refresh token expiration
      await context.route('**/api/auth/refresh', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Unauthorized',
            message: 'Refresh token expired',
            code: 'REFRESH_TOKEN_EXPIRED'
          })
        });
      });

      // Trigger token refresh (simulate background token refresh)
      await page.evaluate(() => {
        // Simulate expired access token
        localStorage.setItem('access_token_expires', String(Date.now() - 3600000));
      });

      // Perform action that requires authentication
      await page.click('[data-testid="create-hive-btn"]');

      // Should detect refresh token expiration
      await expect(page.locator('[data-testid="refresh-token-expired"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should require full re-authentication
      await expect(page.locator('[data-testid="full-login-required"]')).toBeVisible();
      await expect(page.locator('[data-testid="security-notice"]')).toContainText(/security.*expired|login.*again/i);

      // Should preserve current page for redirect after login
      const returnUrl = await page.evaluate(() => window.location.pathname);
      expect(returnUrl).toBe('/dashboard');

      // Should redirect to login with return URL
      await page.click('[data-testid="go-to-login"]');

      await expect(page.url()).toContain('/login');
      const urlParams = new URL(page.url()).searchParams;
      expect(urlParams.get('returnUrl')).toBe('/dashboard');
    });
  });

  test.describe('Concurrent Modification Conflicts', () => {
    test('should detect and resolve optimistic locking conflicts', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123/settings');

      // Wait for settings to load
      await expect(page.locator('[data-testid="hive-settings-loaded"]')).toBeVisible();

      // Start editing
      await page.fill('[data-testid="hive-description"]', 'Updated description');

      // Mock concurrent modification conflict
      await context.route('**/api/hives/123', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'Conflict',
              message: 'Resource has been modified by another user',
              conflictType: 'OPTIMISTIC_LOCK_CONFLICT',
              currentVersion: 5,
              requestVersion: 3,
              lastModifiedBy: 'other-user@example.com',
              lastModifiedAt: '2024-01-01T12:30:00Z',
              conflictingFields: ['description', 'maxParticipants']
            })
          });
        } else {
          await route.continue();
        }
      });

      // Attempt to save
      await page.click('[data-testid="save-settings"]');

      // Should detect conflict
      await expect(page.locator('[data-testid="optimistic-lock-conflict"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show conflict details
      const conflictDetails = page.locator('[data-testid="conflict-details"]');
      await expect(conflictDetails).toContainText('other-user@example.com');
      await expect(conflictDetails).toContainText('description');

      // Should offer resolution strategies
      await expect(page.locator('[data-testid="reload-and-merge"]')).toBeVisible();
      await expect(page.locator('[data-testid="force-overwrite"]')).toBeVisible();
      await expect(page.locator('[data-testid="view-differences"]')).toBeVisible();

      // Test view differences
      await page.click('[data-testid="view-differences"]');

      await expect(page.locator('[data-testid="diff-viewer"]')).toBeVisible();
      await expect(page.locator('[data-testid="your-changes"]')).toContainText('Updated description');
      await expect(page.locator('[data-testid="their-changes"]')).toBeVisible();

      // Test reload and merge
      await page.click('[data-testid="close-diff"]');

      // Clear route mock
      await context.unroute('**/api/hives/123');

      await page.click('[data-testid="reload-and-merge"]');

      // Should reload current data and preserve user changes where possible
      await expect(page.locator('[data-testid="merge-completed"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Should highlight merged fields
      await expect(page.locator('[data-testid="merged-field-description"]')).toHaveClass(/merged|highlighted/);
    });

    test('should handle race conditions in state updates', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Set up race condition scenario
      await errorHelper.simulateStateRaceCondition();

      // Trigger multiple rapid state updates
      const rapidActions = [
        () => page.click('[data-testid="start-timer"]'),
        () => page.click('[data-testid="join-hive"]'),
        () => page.fill('[data-testid="status-update"]', 'Working on task'),
        () => page.click('[data-testid="update-status"]'),
        () => page.click('[data-testid="pause-timer"]')
      ];

      // Execute actions rapidly
      await Promise.all(rapidActions.map(action => action()));

      // Should detect race condition
      await expect(page.locator('[data-testid="race-condition-detected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should resolve to consistent state
      await expect(page.locator('[data-testid="state-synchronized"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Should show final consistent state
      const timerState = page.locator('[data-testid="timer-state"]');
      const hiveStatus = page.locator('[data-testid="hive-status"]');

      // States should be consistent and valid
      await expect(timerState).toHaveAttribute('data-state', /running|paused|stopped/);
      await expect(hiveStatus).toHaveAttribute('data-status', /joined|left/);

      // Should log race condition for debugging
      const consoleLogs = await errorHelper.getConsoleMessages();
      expect(consoleLogs.some(log => log.includes('race condition') || log.includes('state conflict'))).toBeTruthy();
    });

    test('should handle cache invalidation during updates', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Wait for cache to populate
      await expect(page.locator('[data-testid="hive-list-loaded"]')).toBeVisible();

      // Open hive in new tab to modify data
      const secondTab = await context.newPage();
      await secondTab.goto('/hive/123/settings');

      // Modify hive in second tab
      await secondTab.fill('[data-testid="hive-name"]', 'Modified Hive Name');
      await secondTab.click('[data-testid="save-settings"]');
      await secondTab.waitForSelector('[data-testid="settings-saved"]');

      // Back to first tab - should detect stale cache
      await page.bringToFront();

      // Trigger cache validation
      await page.click('[data-testid="refresh-hives"]');

      // Should detect cache invalidation
      await expect(page.locator('[data-testid="cache-invalidated"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should update with fresh data
      await expect(page.locator('[data-testid="hive-card"]:has-text("Modified Hive Name")')).toBeVisible();

      // Should show cache update notification
      await expect(page.locator('[data-testid="data-refreshed"]')).toBeVisible();

      await secondTab.close();
    });
  });

  test.describe('Redux Store Issues', () => {
    test('should handle Redux store corruption', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Wait for store to initialize
      await page.waitForTimeout(2000);

      // Corrupt Redux store
      await page.evaluate(() => {
        // Access Redux store (assuming it's exposed for debugging)
        if (window.__REDUX_STORE__) {
          // Corrupt the store state
          window.__REDUX_STORE__.dispatch({
            type: '@@CORRUPT_STATE',
            payload: {corrupted: true, invalidData: undefined}
          });
        } else if (window.__REDUX_DEVTOOLS_EXTENSION__) {
          // Alternative approach for Redux DevTools
          window.__REDUX_DEVTOOLS_EXTENSION__.dispatch({
            type: '@@TEST_CORRUPTION'
          });
        }
      });

      // Trigger store usage
      await page.click('[data-testid="create-hive-btn"]');

      // Should detect store corruption
      await expect(page.locator('[data-testid="store-corruption-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should offer store recovery options
      await expect(page.locator('[data-testid="reset-store"]')).toBeVisible();
      await expect(page.locator('[data-testid="reload-page"]')).toBeVisible();

      // Should prevent further actions that could cause issues
      await expect(page.locator('[data-testid="safe-mode-active"]')).toBeVisible();

      // Test store reset
      await page.click('[data-testid="reset-store"]');

      // Should reinitialize store
      await expect(page.locator('[data-testid="store-reset-complete"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Should restore normal functionality
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
      await expect(page.locator('[data-testid="create-hive-btn"]')).toBeEnabled();
    });

    test('should handle middleware errors gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Simulate middleware error
      await page.evaluate(() => {
        // Inject middleware error simulation
        if (window.__REDUX_STORE__) {
          const originalDispatch = window.__REDUX_STORE__.dispatch;
          window.__REDUX_STORE__.dispatch = function (action: unknown) {
            if (typeof action === 'object' && action !== null && 'type' in action && action.type === 'CREATE_HIVE') {
              throw new Error('Middleware error: Action processing failed');
            }
            return originalDispatch.call(this, action);
          };
        }
      });

      // Trigger action that will cause middleware error
      await page.click('[data-testid="create-hive-btn"]');
      await page.fill('[data-testid="hive-name"]', 'Middleware Test Hive');
      await page.click('[data-testid="submit-hive"]');

      // Should handle middleware error gracefully
      await expect(page.locator('[data-testid="action-processing-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should not crash the application
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();

      // Should offer error recovery
      await expect(page.locator('[data-testid="retry-action"]')).toBeVisible();
      await expect(page.locator('[data-testid="report-error"]')).toBeVisible();

      // Should show error boundary instead of white screen
      await expect(page.locator('[data-testid="error-boundary"]')).toBeVisible();

      // Should log error details for debugging
      const errorLogs = await errorHelper.getConsoleErrors();
      expect(errorLogs.some(log => log.includes('Middleware error'))).toBeTruthy();
    });

    test('should handle action payload validation errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Simulate invalid action payload
      await page.evaluate(() => {
        if (window.__REDUX_STORE__) {
          // Dispatch action with invalid payload
          window.__REDUX_STORE__.dispatch({
            type: 'UPDATE_TIMER',
            payload: {
              duration: 'invalid-duration',
              status: 123, // Wrong type
              invalidField: undefined
            }
          });
        }
      });

      // Should validate action payloads
      await expect(page.locator('[data-testid="invalid-action-payload"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show validation errors
      const validationError = page.locator('[data-testid="payload-validation-error"]');
      await expect(validationError).toContainText(/invalid.*payload|validation.*failed/i);

      // Should maintain store integrity
      const storeState = await page.evaluate(() => {
        return window.__REDUX_STORE__?.getState?.()?.timer || {};
      });

      // Store should not contain invalid data
      expect(typeof storeState.duration).toBe('number');
      expect(typeof storeState.status).toBe('string');

      // Should offer debugging information in dev mode
      if (process.env.NODE_ENV === 'development') {
        await expect(page.locator('[data-testid="action-debug-info"]')).toBeVisible();
      }
    });
  });

  test.describe('Memory and Cleanup Issues', () => {
    test('should detect and prevent memory leaks', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Navigate between pages to create potential memory leaks
      const pages = ['/dashboard', '/timer', '/hive/123', '/profile', '/settings'];

      for (let i = 0; i < 3; i++) {
        for (const pagePath of pages) {
          await page.goto(pagePath);
          await page.waitForTimeout(1000);

          // Check memory usage
          const memoryInfo = await page.evaluate(() => {
            const perfMemory = (performance as unknown as {
              memory?: { usedJSHeapSize: number; totalJSHeapSize: number }
            }).memory;
            return perfMemory ? {
              used: perfMemory.usedJSHeapSize,
              total: perfMemory.totalJSHeapSize
            } : null;
          });

          if (memoryInfo && memoryInfo.used > 50 * 1024 * 1024) { // 50MB threshold
            console.log(`Memory usage: ${Math.round(memoryInfo.used / 1024 / 1024)}MB`);
          }
        }
      }

      // Check for memory leak warnings
      const memoryWarning = page.locator('[data-testid="memory-usage-warning"]');
      if (await memoryWarning.isVisible()) {
        await expect(memoryWarning).toContainText(/memory.*usage|performance.*impact/i);

        // Should offer memory cleanup options
        await expect(page.locator('[data-testid="clear-cache"]')).toBeVisible();
        await expect(page.locator('[data-testid="restart-app"]')).toBeVisible();
      }

      // Verify cleanup on page unload
      await page.evaluate(() => {
        // Simulate page unload event
        window.dispatchEvent(new Event('beforeunload'));
      });

      // Check that event listeners are cleaned up
      const listenerCount = await page.evaluate(() => {
        return window.__EVENT_LISTENERS_COUNT__ || 0;
      });

      // Should have reasonable number of active listeners
      expect(listenerCount).toBeLessThan(100);
    });

    test('should cleanup WebSocket connections and timers', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Start timer and establish connections
      await page.click('[data-testid="start-timer"]');
      await page.waitForTimeout(2000);

      // Track active resources
      const initialResources = await page.evaluate(() => ({
        timers: window.__ACTIVE_TIMERS__ || 0,
        intervals: window.__ACTIVE_INTERVALS__ || 0,
        websockets: window.__ACTIVE_WEBSOCKETS__ || 0
      }));

      // Navigate away
      await page.goto('/dashboard');

      // Check resource cleanup
      const finalResources = await page.evaluate(() => ({
        timers: window.__ACTIVE_TIMERS__ || 0,
        intervals: window.__ACTIVE_INTERVALS__ || 0,
        websockets: window.__ACTIVE_WEBSOCKETS__ || 0
      }));

      // Resources should be cleaned up
      expect(finalResources.timers).toBeLessThanOrEqual(initialResources.timers);
      expect(finalResources.websockets).toBeLessThanOrEqual(initialResources.websockets);

      // Should not have resource leak warnings
      await expect(page.locator('[data-testid="resource-leak-warning"]')).not.toBeVisible();
    });

    test('should handle component unmount cleanup errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');

      // Start timer with resources
      await page.click('[data-testid="start-timer"]');
      await page.waitForTimeout(1000);

      // Simulate cleanup error during unmount
      await page.evaluate(() => {
        // Mock cleanup error
        window.__CLEANUP_ERROR_SIMULATION__ = true;
      });

      // Navigate away quickly to trigger unmount
      await page.goto('/dashboard');

      // Should handle cleanup errors gracefully
      await expect(page.locator('[data-testid="cleanup-error-handled"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should not crash or leave broken state
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();

      // Should log cleanup errors for debugging
      const errorLogs = await errorHelper.getConsoleErrors();
      expect(errorLogs.some(log => log.includes('cleanup') || log.includes('unmount'))).toBeTruthy();

      // Return to timer page should work normally
      await page.goto('/timer');
      await expect(page.locator('[data-testid="timer-container"]')).toBeVisible();
    });
  });

  test.describe('State Transition Validation', () => {
    test('should validate timer state transitions', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');

      // Test invalid state transition
      await page.evaluate(() => {
        // Force invalid state transition
        if (window.__REDUX_STORE__) {
          window.__REDUX_STORE__.dispatch({
            type: 'SET_TIMER_STATE',
            payload: {status: 'invalid-state', duration: -100}
          });
        }
      });

      // Should detect invalid state
      await expect(page.locator('[data-testid="invalid-timer-state"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should reset to valid state
      await expect(page.locator('[data-testid="timer-state-reset"]')).toBeVisible();

      // Should show valid timer controls
      await expect(page.locator('[data-testid="start-timer"]')).toBeVisible();
      await expect(page.locator('[data-testid="timer-display"]')).toContainText(/00:00|ready/i);

      // Test valid state transitions
      await page.click('[data-testid="start-timer"]');
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible();

      await page.click('[data-testid="pause-timer"]');
      await expect(page.locator('[data-testid="timer-paused"]')).toBeVisible();

      await page.click('[data-testid="stop-timer"]');
      await expect(page.locator('[data-testid="timer-stopped"]')).toBeVisible();
    });

    test('should validate hive membership state transitions', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Force invalid membership state
      await page.evaluate(() => {
        if (window.__REDUX_STORE__) {
          window.__REDUX_STORE__.dispatch({
            type: 'SET_MEMBERSHIP_STATE',
            payload: {
              status: 'joined-and-left', // Invalid simultaneous states
              role: 'invalid-role',
              permissions: null
            }
          });
        }
      });

      // Should detect invalid membership state
      await expect(page.locator('[data-testid="invalid-membership-state"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should resolve to valid state
      await expect(page.locator('[data-testid="membership-state-resolved"]')).toBeVisible();

      // Should show appropriate controls for resolved state
      const joinButton = page.locator('[data-testid="join-hive"]');
      const leaveButton = page.locator('[data-testid="leave-hive"]');

      // Only one of these should be visible
      const joinVisible = await joinButton.isVisible();
      const leaveVisible = await leaveButton.isVisible();

      expect(joinVisible && leaveVisible).toBeFalsy(); // Not both visible
      expect(joinVisible || leaveVisible).toBeTruthy(); // At least one visible
    });
  });
});