/**
 * Network Error Handling E2E Tests for FocusHive
 * Comprehensive testing of network failure scenarios and recovery mechanisms
 *
 * Test Coverage:
 * - Complete network disconnection
 * - Intermittent connectivity
 * - High latency/slow network
 * - Packet loss simulation
 * - DNS resolution failures
 * - SSL/TLS errors
 * - Proxy connection issues
 * - CORS errors
 */

import {BrowserContext, expect, Page, test} from '@playwright/test';
import {ErrorHandlingHelper} from '../../helpers/error-handling.helper';
import {AuthHelper} from '../../helpers/auth-helpers';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Network Error Handling', () => {
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

    // Set up network monitoring
    await errorHelper.setupNetworkMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
    await errorHelper.restoreNetworkConditions();
  });

  test.describe('Complete Network Disconnection', () => {
    test('should detect offline state and show appropriate UI', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Simulate complete network disconnection
      await errorHelper.simulateNetworkOffline();

      // Verify offline detection
      await expect(page.locator('[data-testid="offline-banner"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="offline-banner"]')).toContainText(/offline|no connection/i);

      // Verify UI shows offline state
      const offlineElements = page.locator('[data-testid*="offline"], [data-testid*="disconnected"]');
      await expect(offlineElements.first()).toBeVisible();

      // Verify critical actions are disabled
      await expect(page.locator('button:has-text("Save")')).toBeDisabled();
      await expect(page.locator('button:has-text("Create Hive")')).toBeDisabled();
    });

    test('should queue actions when offline and execute when online', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Simulate offline
      await errorHelper.simulateNetworkOffline();

      // Try to perform action while offline
      await page.fill('[data-testid="hive-name-input"]', 'Offline Test Hive');
      await page.click('button:has-text("Create Hive")');

      // Verify action is queued
      await expect(page.locator('[data-testid="queued-actions-counter"]')).toBeVisible();
      await expect(page.locator('[data-testid="queued-actions-counter"]')).toContainText('1');

      // Restore connection
      await errorHelper.simulateNetworkOnline();

      // Wait for queued actions to execute
      await expect(page.locator('[data-testid="hive-card"]:has-text("Offline Test Hive")')).toBeVisible({timeout: TIMEOUTS.LONG});
      await expect(page.locator('[data-testid="queued-actions-counter"]')).toBeHidden();
    });

    test('should preserve data in local storage during network outage', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');

      // Start timer with data
      await page.fill('[data-testid="task-description"]', 'Network test task');
      await page.click('[data-testid="start-timer"]');

      // Wait for timer to run
      await page.waitForTimeout(2000);

      // Simulate network disconnection
      await errorHelper.simulateNetworkOffline();

      // Refresh page to test data persistence
      await page.reload();

      // Verify data is preserved
      await expect(page.locator('[data-testid="task-description"]')).toHaveValue('Network test task');
      await expect(page.locator('[data-testid="timer-display"]')).not.toContainText('00:00');
    });
  });

  test.describe('Intermittent Connectivity', () => {
    test('should handle flaky network connections gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Simulate intermittent connection
      const flakinessDuration = 30000; // 30 seconds
      await errorHelper.simulateIntermittentConnectivity(
          flakinessDuration,
          {offlineChance: 0.3, reconnectDelay: 1000}
      );

      // Monitor network status changes
      const networkStatusChanges = [];
      await page.evaluate(() => {
        window.addEventListener('online', () => networkStatusChanges.push('online'));
        window.addEventListener('offline', () => networkStatusChanges.push('offline'));
      });

      // Perform various actions during flaky connection
      for (let i = 0; i < 5; i++) {
        await page.fill('[data-testid="quick-note"]', `Flaky test note ${i}`);
        await page.click('[data-testid="save-note"]');
        await page.waitForTimeout(3000);
      }

      // Restore stable connection
      await errorHelper.simulateNetworkOnline();

      // Verify all data was eventually saved
      const notes = page.locator('[data-testid="note-item"]');
      await expect(notes).toHaveCount(5, {timeout: TIMEOUTS.LONG});

      // Verify retry mechanisms worked
      const retryAttempts = await page.evaluate(() =>
          parseInt(localStorage.getItem('networkRetryCount') || '0')
      );
      expect(retryAttempts).toBeGreaterThan(0);
    });

    test('should show connection status indicators during instability', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Start with stable connection
      await expect(page.locator('[data-testid="connection-status"]')).toHaveClass(/connected/);

      // Simulate unstable connection
      await errorHelper.simulateIntermittentConnectivity(15000, {
        offlineChance: 0.5,
        reconnectDelay: 2000
      });

      // Monitor status indicator changes
      const statusIndicator = page.locator('[data-testid="connection-status"]');

      // Should show unstable/reconnecting states
      await expect(statusIndicator).toHaveClass(/reconnecting|unstable/, {timeout: TIMEOUTS.MEDIUM});

      // Verify tooltip shows helpful information
      await statusIndicator.hover();
      const tooltip = page.locator('[data-testid="connection-tooltip"]');
      await expect(tooltip).toBeVisible();
      await expect(tooltip).toContainText(/reconnecting|unstable|poor/i);
    });
  });

  test.describe('High Latency Network', () => {
    test('should handle slow network responses with proper loading states', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Simulate high latency network (3-5 second delays)
      await errorHelper.simulateHighLatency(3000, 5000);

      await page.goto('/dashboard');

      // Verify loading states appear
      await expect(page.locator('[data-testid="page-loading"]')).toBeVisible();
      await expect(page.locator('[data-testid="slow-connection-warning"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Wait for page to eventually load
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible({timeout: TIMEOUTS.LONG});

      // Test action with loading feedback
      await page.click('[data-testid="create-hive-btn"]');
      await expect(page.locator('[data-testid="create-hive-btn"] .loading-spinner')).toBeVisible();

      // Fill form during slow response
      await page.fill('[data-testid="hive-name"]', 'Slow Network Test Hive');
      await page.click('[data-testid="submit-hive"]');

      // Verify extended loading state
      await expect(page.locator('[data-testid="submit-hive"]')).toHaveAttribute('disabled', '');
      await expect(page.locator('[data-testid="submit-hive"] .loading-text')).toContainText(/saving|creating/i);

      // Eventually succeeds
      await expect(page.locator('[data-testid="hive-created-success"]')).toBeVisible({timeout: TIMEOUTS.EXTRA_LONG});
    });

    test('should timeout appropriately and offer retry options', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Simulate extremely slow network (10+ second delays)
      await errorHelper.simulateHighLatency(10000, 15000);

      await page.goto('/analytics');

      // Wait for timeout error
      await expect(page.locator('[data-testid="request-timeout-error"]')).toBeVisible({timeout: TIMEOUTS.LONG});

      // Verify retry option is available
      const retryButton = page.locator('[data-testid="retry-request"]');
      await expect(retryButton).toBeVisible();
      await expect(retryButton).toBeEnabled();

      // Click retry and verify loading state
      await retryButton.click();
      await expect(page.locator('[data-testid="retrying-request"]')).toBeVisible();

      // Should eventually succeed or timeout again
      const successOrTimeout = page.locator('[data-testid="analytics-content"], [data-testid="request-timeout-error"]');
      await expect(successOrTimeout).toBeVisible({timeout: TIMEOUTS.EXTRA_LONG});
    });
  });

  test.describe('Packet Loss Simulation', () => {
    test('should handle packet loss with automatic retries', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Simulate 20% packet loss
      await errorHelper.simulatePacketLoss(0.2);

      // Perform multiple actions to trigger retries
      for (let i = 0; i < 10; i++) {
        await page.fill('[data-testid="chat-input"]', `Packet loss test message ${i}`);
        await page.press('[data-testid="chat-input"]', 'Enter');
        await page.waitForTimeout(1000);
      }

      // Verify messages eventually appear (some may need retries)
      await expect(page.locator('[data-testid="chat-message"]')).toHaveCount(10, {
        timeout: TIMEOUTS.EXTRA_LONG
      });

      // Check retry statistics
      const retryCount = await page.evaluate(() =>
          parseInt(sessionStorage.getItem('packetLossRetries') || '0')
      );
      expect(retryCount).toBeGreaterThan(0);
    });

    test('should show delivery status for critical messages', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Enable packet loss
      await errorHelper.simulatePacketLoss(0.3);

      // Send critical message
      await page.fill('[data-testid="chat-input"]', 'Important: Meeting in 5 minutes');
      await page.press('[data-testid="chat-input"]', 'Enter');

      const lastMessage = page.locator('[data-testid="chat-message"]').last();

      // Initially shows sending state
      await expect(lastMessage.locator('[data-testid="message-status"]')).toHaveClass(/sending/);

      // Eventually shows delivered or failed state
      const statusElement = lastMessage.locator('[data-testid="message-status"]');
      await expect(statusElement).toHaveClass(/delivered|failed/, {timeout: TIMEOUTS.LONG});

      // If failed, retry option should be available
      if (await statusElement.getAttribute('class').then(classes => classes?.includes('failed'))) {
        const retryButton = lastMessage.locator('[data-testid="retry-message"]');
        await expect(retryButton).toBeVisible();
      }
    });
  });

  test.describe('DNS and SSL Errors', () => {
    test('should handle DNS resolution failures gracefully', async () => {
      // Mock DNS resolution failure
      await context.route('**/api/**', async (route) => {
        await route.abort('namenotresolved');
      });

      await page.goto('/dashboard');

      // Should show DNS error message
      await expect(page.locator('[data-testid="dns-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="dns-error"]')).toContainText(/cannot resolve|dns|hostname/i);

      // Verify helpful error message
      const errorMessage = page.locator('[data-testid="error-message"]');
      await expect(errorMessage).toContainText(/network settings|dns configuration/i);

      // Should offer troubleshooting options
      await expect(page.locator('[data-testid="network-troubleshooting"]')).toBeVisible();
    });

    test('should handle SSL/TLS certificate errors', async () => {
      // Mock SSL certificate error
      await context.route('**/api/**', async (route) => {
        await route.abort('connectionfailed');
      });

      await page.goto('/login');

      // Attempt login to trigger SSL error
      await page.fill('[data-testid="username"]', TEST_USERS.VALID_USER.username);
      await page.fill('[data-testid="password"]', TEST_USERS.VALID_USER.password);
      await page.click('[data-testid="login-btn"]');

      // Should show SSL error
      await expect(page.locator('[data-testid="ssl-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify security warning
      const securityWarning = page.locator('[data-testid="security-warning"]');
      await expect(securityWarning).toBeVisible();
      await expect(securityWarning).toContainText(/certificate|security|ssl|tls/i);

      // Should not offer unsafe workarounds
      await expect(page.locator('button:has-text("Proceed Anyway")')).not.toBeVisible();
    });
  });

  test.describe('CORS and Proxy Errors', () => {
    test('should handle CORS errors with helpful messaging', async () => {
      // Mock CORS error
      await context.route('**/api/hives', async (route) => {
        await route.fulfill({
          status: 200,
          headers: {
            'Access-Control-Allow-Origin': 'https://wrong-origin.com'
          },
          body: JSON.stringify({error: 'CORS error'})
        });
      });

      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Should show CORS error message
      await expect(page.locator('[data-testid="cors-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify helpful error explanation
      const errorExplanation = page.locator('[data-testid="error-explanation"]');
      await expect(errorExplanation).toContainText(/cross-origin|cors|security policy/i);

      // Should suggest contacting support
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();
    });

    test('should detect and handle proxy connection issues', async () => {
      // Mock proxy timeout
      await context.route('**/api/**', async (_route) => {
        await new Promise(resolve => setTimeout(resolve, 30000)); // Force timeout
      });

      await page.goto('/dashboard');

      // Should eventually show proxy/connection error
      await expect(page.locator('[data-testid="proxy-error"]')).toBeVisible({
        timeout: TIMEOUTS.EXTRA_LONG
      });

      // Verify proxy-specific error handling
      const proxyMessage = page.locator('[data-testid="proxy-error-message"]');
      await expect(proxyMessage).toContainText(/proxy|gateway|connection timeout/i);

      // Should offer connection diagnostics
      await expect(page.locator('[data-testid="run-diagnostics"]')).toBeVisible();
    });
  });

  test.describe('Network Recovery Scenarios', () => {
    test('should automatically recover when network is restored', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Start with network issues
      await errorHelper.simulateNetworkOffline();

      // Verify offline state
      await expect(page.locator('[data-testid="offline-indicator"]')).toBeVisible();

      // Restore network
      await errorHelper.simulateNetworkOnline();

      // Should automatically recover
      await expect(page.locator('[data-testid="connection-restored"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Verify WebSocket reconnection
      await expect(page.locator('[data-testid="realtime-status"]')).toHaveClass(/connected/);

      // Verify data synchronization
      await expect(page.locator('[data-testid="sync-status"]')).toContainText(/synchronized|up to date/i);
    });

    test('should handle graceful degradation during partial connectivity', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Simulate partial connectivity (some endpoints fail)
      await context.route('**/api/analytics/**', route => route.abort());
      await context.route('**/api/notifications/**', route => route.abort());

      // Core functionality should still work
      await expect(page.locator('[data-testid="hive-list"]')).toBeVisible();
      await expect(page.locator('[data-testid="create-hive-btn"]')).toBeEnabled();

      // Failed features should show graceful degradation
      await expect(page.locator('[data-testid="analytics-unavailable"]')).toBeVisible();
      await expect(page.locator('[data-testid="notifications-limited"]')).toBeVisible();

      // Should still allow core workflows
      await page.click('[data-testid="create-hive-btn"]');
      await page.fill('[data-testid="hive-name"]', 'Degraded Mode Hive');
      await page.click('[data-testid="submit-hive"]');

      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    });
  });
});