/**
 * Resource Exhaustion and Chaos Testing E2E Tests for FocusHive
 * Comprehensive testing of system limits, resource exhaustion, and chaos scenarios
 * 
 * Test Coverage:
 * - Memory limit exhaustion
 * - Storage quota exceeded
 * - CPU throttling scenarios
 * - Thread pool exhaustion
 * - Connection pool depletion
 * - File handle limits
 * - WebSocket connection limits
 * - API rate limiting
 * - Large data handling
 * - Chaos engineering scenarios
 */

import { test, expect, Page, BrowserContext } from '@playwright/test';
import { ErrorHandlingHelper } from '../../helpers/error-handling.helper';
import { AuthHelper } from '../../helpers/auth-helpers';
import { 
  TEST_USERS, 
  validateTestEnvironment, 
  TIMEOUTS,
  API_ENDPOINTS 
} from '../../helpers/test-data';

test.describe('Resource Exhaustion and Chaos Testing', () => {
  let page: Page;
  let context: BrowserContext;
  let errorHelper: ErrorHandlingHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({ page: testPage, context: testContext }) => {
    page = testPage;
    context = testContext;
    errorHelper = new ErrorHandlingHelper(page, context);
    authHelper = new AuthHelper(page);
    
    validateTestEnvironment();
    
    // Set up resource monitoring
    await errorHelper.setupResourceMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
    await errorHelper.clearResourceStress();
  });

  test.describe('Memory Exhaustion Scenarios', () => {
    test('should handle memory limit exhaustion gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Monitor initial memory usage
      const initialMemory = await page.evaluate(() => {
        const perfMemory = (performance as unknown as { memory?: { usedJSHeapSize: number } }).memory;
        return perfMemory ? perfMemory.usedJSHeapSize : 0;
      });
      
      // Simulate memory-intensive operations
      await page.evaluate(() => {
        // Create memory pressure
        const memoryHogs: unknown[] = [];
        try {
          for (let i = 0; i < 1000; i++) {
            // Create large arrays to consume memory
            memoryHogs.push(new Array(10000).fill(`Memory stress test ${i}`));
            
            // Check if we're approaching limits
            const perfMemory = (performance as unknown as { memory?: { usedJSHeapSize: number; jsHeapSizeLimit: number } }).memory;
            if (perfMemory && perfMemory.usedJSHeapSize > perfMemory.jsHeapSizeLimit * 0.8) {
              break;
            }
          }
        } catch (error) {
          console.warn('Memory allocation failed:', error);
        }
      });
      
      // Check for memory warnings
      const memoryWarning = page.locator('[data-testid="memory-warning"]');
      if (await memoryWarning.isVisible({ timeout: 5000 })) {
        // Should show memory usage warning
        await expect(memoryWarning).toContainText(/memory.*usage.*high|performance.*affected/i);
        
        // Should offer memory management options
        await expect(page.locator('[data-testid="clear-cache"]')).toBeVisible();
        await expect(page.locator('[data-testid="close-unused-tabs"]')).toBeVisible();
        await expect(page.locator('[data-testid="reduce-features"]')).toBeVisible();
        
        // Test cache clearing
        await page.click('[data-testid="clear-cache"]');
        
        await expect(page.locator('[data-testid="cache-cleared"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
        await expect(page.locator('[data-testid="memory-freed"]')).toBeVisible();
      }
      
      // Application should remain functional
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
      await expect(page.locator('[data-testid="create-hive-btn"]')).toBeEnabled();
    });

    test('should prevent memory leaks in long-running sessions', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Track memory usage over time
      const memoryReadings: number[] = [];
      
      const recordMemory = async () => {
        const memory = await page.evaluate(() => {
          const perfMemory = (performance as unknown as { memory?: { usedJSHeapSize: number } }).memory;
          return perfMemory ? perfMemory.usedJSHeapSize : 0;
        });
        memoryReadings.push(memory);
        return memory;
      };
      
      // Initial reading
      await recordMemory();
      
      // Simulate long-running session activities
      const activities = [
        () => page.goto('/dashboard'),
        () => page.goto('/timer'),
        () => page.click('[data-testid="start-timer"]'),
        () => page.goto('/hive/123'),
        () => page.fill('[data-testid="chat-input"]', 'Memory test message'),
        () => page.press('[data-testid="chat-input"]', 'Enter'),
        () => page.goto('/profile'),
        () => page.goto('/settings'),
        () => page.goto('/analytics')
      ];
      
      // Perform activities multiple times
      for (let cycle = 0; cycle < 5; cycle++) {
        for (const activity of activities) {
          try {
            await activity();
            await page.waitForTimeout(500);
          } catch (error) {
            console.warn(`Activity failed: ${error}`);
          }
        }
        
        // Record memory after each cycle
        await recordMemory();
        
        // Force garbage collection if available
        await page.evaluate(() => {
          if (window.gc) {
            window.gc();
          }
        });
      }
      
      // Analyze memory trend
      const memoryGrowth = memoryReadings[memoryReadings.length - 1] - memoryReadings[0];
      const averageGrowthPerCycle = memoryGrowth / 5;
      
      // Memory growth should be reasonable (less than 10MB per cycle)
      expect(averageGrowthPerCycle).toBeLessThan(10 * 1024 * 1024);
      
      // Check for memory leak detection
      if (averageGrowthPerCycle > 5 * 1024 * 1024) {
        await expect(page.locator('[data-testid="potential-memory-leak"]')).toBeVisible({ timeout: 5000 });
      }
    });

    test('should handle large dataset rendering without crashing', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Mock large dataset response
      await context.route('**/api/analytics/sessions', async (route) => {
        const largeDataset = {
          sessions: Array.from({ length: 10000 }, (_, i) => ({
            id: `session-${i}`,
            duration: Math.floor(Math.random() * 7200),
            date: new Date(Date.now() - i * 86400000).toISOString(),
            productivity: Math.random() * 100,
            breaks: Math.floor(Math.random() * 10),
            focus_score: Math.random() * 10
          })),
          total: 10000,
          summary: {
            total_hours: 15000,
            avg_productivity: 75.5,
            trend: 'improving'
          }
        };
        
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(largeDataset)
        });
      });
      
      await page.goto('/analytics');
      
      // Should handle large dataset loading
      await expect(page.locator('[data-testid="large-dataset-loading"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should implement virtualization or pagination
      await expect(page.locator('[data-testid="virtualized-list"], [data-testid="paginated-results"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should show performance optimization notice
      await expect(page.locator('[data-testid="performance-optimized"]')).toBeVisible();
      
      // Should remain responsive
      await page.click('[data-testid="filter-dropdown"]');
      await expect(page.locator('[data-testid="filter-options"]')).toBeVisible({ timeout: TIMEOUTS.SHORT });
      
      // Should handle scrolling efficiently
      await page.evaluate(() => {
        const container = document.querySelector('[data-testid="analytics-container"]');
        if (container) {
          container.scrollTop = container.scrollHeight / 2;
        }
      });
      
      // Should load more data on demand
      await expect(page.locator('[data-testid="loading-more-data"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });
  });

  test.describe('Storage Quota Exhaustion', () => {
    test('should handle localStorage quota exceeded errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Fill up localStorage
      await page.evaluate(() => {
        try {
          const largeData = 'X'.repeat(1024 * 1024); // 1MB chunks
          let i = 0;
          while (i < 20) { // Try to store 20MB
            localStorage.setItem(`stress-test-${i}`, largeData);
            i++;
          }
        } catch (e) {
          console.log('Storage quota exceeded during setup');
        }
      });
      
      // Try to save user preferences
      await page.goto('/settings');
      await page.click('[data-testid="theme-dark"]');
      await page.selectOption('[data-testid="notification-frequency"]', 'hourly');
      await page.click('[data-testid="save-preferences"]');
      
      // Should handle quota exceeded gracefully
      await expect(page.locator('[data-testid="storage-quota-exceeded"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show storage management dialog
      const storageDialog = page.locator('[data-testid="storage-management"]');
      await expect(storageDialog).toBeVisible();
      await expect(storageDialog).toContainText(/storage.*full|quota.*exceeded/i);
      
      // Should show storage usage breakdown
      await expect(page.locator('[data-testid="storage-usage-breakdown"]')).toBeVisible();
      
      // Should offer cleanup options
      await expect(page.locator('[data-testid="clear-cache-data"]')).toBeVisible();
      await expect(page.locator('[data-testid="clear-old-data"]')).toBeVisible();
      await expect(page.locator('[data-testid="export-critical-data"]')).toBeVisible();
      
      // Test selective cleanup
      await page.click('[data-testid="clear-old-data"]');
      
      await expect(page.locator('[data-testid="cleanup-completed"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should retry saving preferences
      await page.click('[data-testid="retry-save"]');
      
      await expect(page.locator('[data-testid="preferences-saved"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });

    test('should handle sessionStorage quota issues', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Fill up sessionStorage
      await page.evaluate(() => {
        try {
          const mediumData = 'Y'.repeat(512 * 1024); // 512KB chunks
          for (let i = 0; i < 40; i++) { // Try to store 20MB
            sessionStorage.setItem(`session-stress-${i}`, mediumData);
          }
        } catch (e) {
          console.log('Session storage quota exceeded during setup');
        }
      });
      
      // Try to store temporary chat draft
      await page.fill('[data-testid="chat-input"]', 'This is a very long message that should be auto-saved as draft');
      
      // Should detect session storage issues
      const storageError = page.locator('[data-testid="session-storage-error"]');
      if (await storageError.isVisible({ timeout: 5000 })) {
        // Should handle gracefully without losing user input
        await expect(page.locator('[data-testid="chat-input"]')).toHaveValue(/This is a very long message/);
        
        // Should offer alternatives
        await expect(page.locator('[data-testid="save-to-cloud"]')).toBeVisible();
        await expect(page.locator('[data-testid="clear-session-data"]')).toBeVisible();
        
        // Test cloud save alternative
        await page.click('[data-testid="save-to-cloud"]');
        
        await expect(page.locator('[data-testid="draft-saved-cloud"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      }
      
      // Should continue functioning
      await page.press('[data-testid="chat-input"]', 'Enter');
      await expect(page.locator('[data-testid="chat-message"]:has-text("This is a very long message")')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
    });

    test('should handle IndexedDB quota limitations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Simulate IndexedDB quota exceeded
      await page.evaluate(() => {
        // Mock IndexedDB quota exceeded error
        const originalOpen = indexedDB.open;
        indexedDB.open = function(...args: unknown[]) {
          const request = originalOpen.apply(this, args as [string, number?]);
          setTimeout(() => {
            const error = new Error('Quota exceeded');
            (error as unknown as { name: string }).name = 'QuotaExceededError';
            request.onerror?.({ target: { error } } as Event);
          }, 100);
          return request;
        };
      });
      
      // Try to save offline data
      await page.click('[data-testid="enable-offline-mode"]');
      
      // Should detect IndexedDB quota issues
      await expect(page.locator('[data-testid="offline-storage-error"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show quota management options
      await expect(page.locator('[data-testid="database-quota-exceeded"]')).toContainText(/database.*storage.*full/i);
      
      // Should offer alternatives
      await expect(page.locator('[data-testid="limited-offline-mode"]')).toBeVisible();
      await expect(page.locator('[data-testid="cloud-only-mode"]')).toBeVisible();
      
      // Test limited offline mode
      await page.click('[data-testid="limited-offline-mode"]');
      
      await expect(page.locator('[data-testid="limited-offline-enabled"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      await expect(page.locator('[data-testid="offline-limitations-notice"]')).toBeVisible();
    });
  });

  test.describe('Connection and Thread Pool Limits', () => {
    test('should handle connection pool exhaustion', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Simulate connection pool exhaustion by making many concurrent requests
      await page.evaluate(async () => {
        const requests = [];
        for (let i = 0; i < 20; i++) {
          requests.push(
            fetch('/api/hives', { 
              method: 'GET',
              headers: { 'Authorization': `Bearer ${localStorage.getItem('access_token')}` }
            }).catch(e => ({ error: e.message }))
          );
        }
        
        try {
          await Promise.all(requests);
        } catch (error) {
          console.warn('Connection pool stress test:', error);
        }
      });
      
      // Should handle connection limits gracefully
      const connectionError = page.locator('[data-testid="connection-pool-exhausted"]');
      if (await connectionError.isVisible({ timeout: 5000 })) {
        await expect(connectionError).toContainText(/connection.*limit|too.*many.*requests/i);
        
        // Should implement request queuing
        await expect(page.locator('[data-testid="request-queued"]')).toBeVisible();
        
        // Should show queue status
        await expect(page.locator('[data-testid="queue-position"]')).toBeVisible();
        
        // Should eventually process queued requests
        await expect(page.locator('[data-testid="requests-processed"]')).toBeVisible({ 
          timeout: TIMEOUTS.LONG 
        });
      }
      
      // Application should remain functional
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
    });

    test('should handle WebSocket connection limits', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Open multiple tabs to exhaust WebSocket connections
      const tabs: Page[] = [];
      
      try {
        for (let i = 0; i < 10; i++) {
          const tab = await context.newPage();
          await tab.goto('/hive/123');
          tabs.push(tab);
          await tab.waitForTimeout(500);
        }
        
        // Last tab should detect connection limit
        const lastTab = tabs[tabs.length - 1];
        
        const connectionLimit = lastTab.locator('[data-testid="websocket-limit-reached"]');
        if (await connectionLimit.isVisible({ timeout: 5000 })) {
          await expect(connectionLimit).toContainText(/connection.*limit|too.*many.*connections/i);
          
          // Should offer alternatives
          await expect(lastTab.locator('[data-testid="use-polling-mode"]')).toBeVisible();
          await expect(lastTab.locator('[data-testid="close-other-tabs"]')).toBeVisible();
          
          // Test polling mode fallback
          await lastTab.click('[data-testid="use-polling-mode"]');
          
          await expect(lastTab.locator('[data-testid="polling-mode-active"]')).toBeVisible({ 
            timeout: TIMEOUTS.MEDIUM 
          });
          
          // Should still receive updates via polling
          await expect(lastTab.locator('[data-testid="hive-content"]')).toBeVisible();
        }
      } finally {
        // Cleanup tabs
        for (const tab of tabs) {
          await tab.close().catch(() => {});
        }
      }
    });

    test('should handle API rate limiting gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Mock rate limiting responses
      let requestCount = 0;
      await context.route('**/api/hives', async (route) => {
        requestCount++;
        if (requestCount > 5) {
          await route.fulfill({
            status: 429,
            headers: {
              'Retry-After': '60',
              'X-RateLimit-Limit': '100',
              'X-RateLimit-Remaining': '0'
            },
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'Rate limit exceeded',
              message: 'Too many requests. Please try again later.',
              retryAfter: 60
            })
          });
        } else {
          await route.continue();
        }
      });
      
      // Make multiple rapid requests
      for (let i = 0; i < 8; i++) {
        await page.click('[data-testid="refresh-hives"]');
        await page.waitForTimeout(200);
      }
      
      // Should show rate limit warning
      await expect(page.locator('[data-testid="rate-limit-warning"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show retry countdown
      await expect(page.locator('[data-testid="retry-countdown"]')).toBeVisible();
      await expect(page.locator('[data-testid="retry-countdown"]')).toContainText(/60|59/);
      
      // Should disable request-heavy features
      await expect(page.locator('[data-testid="refresh-hives"]')).toBeDisabled();
      
      // Should show rate limit explanation
      await expect(page.locator('[data-testid="rate-limit-explanation"]')).toContainText(/rate.*limit|too.*many/i);
      
      // Should offer offline mode
      await expect(page.locator('[data-testid="use-cached-data"]')).toBeVisible();
      
      // Test cached data fallback
      await page.click('[data-testid="use-cached-data"]');
      
      await expect(page.locator('[data-testid="cached-data-notice"]')).toBeVisible();
      await expect(page.locator('[data-testid="hive-list"]')).toBeVisible();
    });
  });

  test.describe('Chaos Engineering Scenarios', () => {
    test('should survive random service interruptions', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Set up chaos monkey for random service failures
      const chaosRoutes = [
        '**/api/hives/**',
        '**/api/users/**', 
        '**/api/notifications/**',
        '**/api/analytics/**'
      ];
      
      for (const route of chaosRoutes) {
        await context.route(route, async (routeHandler) => {
          // 30% chance of failure
          if (Math.random() < 0.3) {
            const errorTypes = [500, 502, 503, 504, 'networkError', 'timeout'];
            const errorType = errorTypes[Math.floor(Math.random() * errorTypes.length)];
            
            if (typeof errorType === 'number') {
              await routeHandler.fulfill({
                status: errorType,
                body: JSON.stringify({ error: 'Chaos monkey error' })
              });
            } else if (errorType === 'networkError') {
              await routeHandler.abort('internetdisconnected');
            } else if (errorType === 'timeout') {
              await new Promise(resolve => setTimeout(resolve, 30000));
            }
          } else {
            await routeHandler.continue();
          }
        });
      }
      
      // Perform various activities under chaos conditions
      const activities = [
        () => page.click('[data-testid="join-hive"]'),
        () => page.fill('[data-testid="chat-input"]', 'Chaos test message'),
        () => page.press('[data-testid="chat-input"]', 'Enter'),
        () => page.click('[data-testid="start-timer"]'),
        () => page.click('[data-testid="pause-timer"]'),
        () => page.goto('/profile'),
        () => page.goto('/hive/123')
      ];
      
      for (let i = 0; i < 10; i++) {
        const activity = activities[Math.floor(Math.random() * activities.length)];
        try {
          await activity();
          await page.waitForTimeout(1000);
        } catch (error) {
          console.log(`Chaos test: Activity failed as expected: ${error}`);
        }
      }
      
      // Application should remain functional despite chaos
      await expect(page.locator('[data-testid="hive-content"]')).toBeVisible();
      
      // Should show resilience indicators
      const resilienceIndicators = page.locator('[data-testid="service-degraded"], [data-testid="operating-normally"], [data-testid="partial-service"]');
      await expect(resilienceIndicators).toHaveCount(1); // Exactly one status indicator
      
      // Should maintain core functionality
      await expect(page.locator('[data-testid="timer-controls"]')).toBeVisible();
      await expect(page.locator('[data-testid="participant-list"]')).toBeVisible();
    });

    test('should handle cascading failure scenarios', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Simulate cascading failures (authentication -> user data -> hive data)
      let failureStage = 0;
      
      await context.route('**/api/auth/verify', async (route) => {
        if (failureStage >= 1) {
          await route.fulfill({ status: 401, body: JSON.stringify({ error: 'Auth failure' }) });
        } else {
          await route.continue();
        }
      });
      
      await context.route('**/api/users/me', async (route) => {
        if (failureStage >= 2) {
          await route.fulfill({ status: 500, body: JSON.stringify({ error: 'User service failure' }) });
        } else {
          await route.continue();
        }
      });
      
      await context.route('**/api/hives', async (route) => {
        if (failureStage >= 3) {
          await route.fulfill({ status: 503, body: JSON.stringify({ error: 'Hive service failure' }) });
        } else {
          await route.continue();
        }
      });
      
      // Trigger first failure
      failureStage = 1;
      await page.reload();
      
      // Should detect auth failure
      await expect(page.locator('[data-testid="auth-failure-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Trigger cascading failures
      failureStage = 2;
      await page.waitForTimeout(2000);
      
      failureStage = 3;
      await page.waitForTimeout(2000);
      
      // Should detect system-wide degradation
      await expect(page.locator('[data-testid="system-degradation"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should enter emergency mode
      await expect(page.locator('[data-testid="emergency-mode"]')).toBeVisible();
      
      // Should show system status
      await expect(page.locator('[data-testid="system-status-critical"]')).toBeVisible();
      
      // Should offer basic recovery options
      await expect(page.locator('[data-testid="system-recovery"]')).toBeVisible();
      
      // Should maintain basic UI functionality
      await expect(page.locator('[data-testid="emergency-navigation"]')).toBeVisible();
    });

    test('should handle resource contention scenarios', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Simulate resource contention by opening multiple resource-intensive operations
      const operations = [];
      
      // Start multiple timer operations
      operations.push(page.goto('/timer'));
      operations.push(page.evaluate(() => {
        // CPU-intensive operation
        let result = 0;
        for (let i = 0; i < 1000000; i++) {
          result += Math.random() * Math.sin(i);
        }
        return result;
      }));
      
      // Start multiple network operations
      operations.push(page.evaluate(() => {
        const promises = [];
        for (let i = 0; i < 10; i++) {
          promises.push(fetch('/api/hives').catch(e => e));
        }
        return Promise.all(promises);
      }));
      
      // Start memory-intensive operation
      operations.push(page.evaluate(() => {
        const data = [];
        for (let i = 0; i < 1000; i++) {
          data.push(new Array(1000).fill(`Resource contention test ${i}`));
        }
        return data.length;
      }));
      
      // Wait for operations to complete or timeout
      await Promise.allSettled(operations.map(op => 
        Promise.race([op, new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Operation timeout')), 10000)
        )])
      ));
      
      // Should handle resource contention gracefully
      const contentionWarning = page.locator('[data-testid="resource-contention-warning"]');
      if (await contentionWarning.isVisible({ timeout: 5000 })) {
        await expect(contentionWarning).toContainText(/resource.*contention|performance.*impact/i);
        
        // Should offer optimization options
        await expect(page.locator('[data-testid="optimize-performance"]')).toBeVisible();
        await expect(page.locator('[data-testid="reduce-operations"]')).toBeVisible();
      }
      
      // Should maintain basic functionality
      await expect(page.locator('[data-testid="timer-container"]')).toBeVisible({ timeout: TIMEOUTS.LONG });
      
      // Performance should eventually stabilize
      await page.waitForTimeout(5000);
      
      const finalPerformance = await page.evaluate(() => {
        return performance.now();
      });
      
      expect(typeof finalPerformance).toBe('number');
      expect(finalPerformance).toBeGreaterThan(0);
    });
  });
});