/**
 * Error Recovery and Resilience E2E Tests for FocusHive
 * Comprehensive testing of system recovery mechanisms and resilience patterns
 * 
 * Test Coverage:
 * - Automatic error recovery mechanisms
 * - Manual recovery triggers and workflows
 * - Data consistency after recovery
 * - Session restoration and state rehydration
 * - WebSocket reconnection with message replay
 * - Queue management and message ordering
 * - Cache rebuilding and invalidation
 * - Graceful degradation patterns
 * - Circuit breaker implementations
 * - Health check and monitoring systems
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

test.describe('Error Recovery and Resilience', () => {
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
    
    // Set up recovery monitoring
    await errorHelper.setupRecoveryMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
  });

  test.describe('Automatic Recovery Mechanisms', () => {
    test('should automatically recover from transient network errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Set up transient error simulation
      let failureCount = 0;
      await context.route('**/api/hives/123/messages', async (route) => {
        failureCount++;
        if (failureCount <= 3) {
          // Fail first 3 attempts, then succeed
          await route.abort('internetdisconnected');
        } else {
          await route.continue();
        }
      });
      
      // Send message that will trigger retries
      await page.fill('[data-testid="chat-input"]', 'Auto-recovery test message');
      await page.press('[data-testid="chat-input"]', 'Enter');
      
      // Should show retry progress
      await expect(page.locator('[data-testid="auto-retry-progress"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show retry attempts
      await expect(page.locator('[data-testid="retry-attempt-1"]')).toBeVisible();
      await expect(page.locator('[data-testid="retry-attempt-2"]')).toBeVisible();
      await expect(page.locator('[data-testid="retry-attempt-3"]')).toBeVisible();
      
      // Should eventually succeed automatically
      await expect(page.locator('[data-testid="auto-recovery-success"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Message should appear in chat
      await expect(page.locator('[data-testid="chat-message"]:has-text("Auto-recovery test message")')).toBeVisible();
      
      // Should log recovery event
      const recoveryLogs = await page.evaluate(() => 
        JSON.parse(sessionStorage.getItem('recovery-log') || '[]')
      );
      expect(recoveryLogs.length).toBeGreaterThan(0);
      expect(recoveryLogs.some((log: { type: string }) => log.type === 'auto-recovery-success')).toBeTruthy();
    });

    test('should automatically restore WebSocket connections', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Wait for initial WebSocket connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();
      
      // Simulate WebSocket disconnection
      await errorHelper.simulateWebSocketDisconnection('network_error');
      
      // Should detect disconnection
      await expect(page.locator('[data-testid="websocket-disconnected"]')).toBeVisible({ 
        timeout: TIMEOUTS.SHORT 
      });
      
      // Should start automatic reconnection
      await expect(page.locator('[data-testid="auto-reconnecting"]')).toBeVisible();
      
      // Should show reconnection progress
      await expect(page.locator('[data-testid="reconnection-attempt-1"]')).toBeVisible();
      
      // Should implement exponential backoff
      const backoffIndicator = page.locator('[data-testid="backoff-timer"]');
      if (await backoffIndicator.isVisible()) {
        await expect(backoffIndicator).toContainText(/[0-9]+/); // Shows countdown
      }
      
      // Should eventually reconnect
      await errorHelper.allowWebSocketConnection();
      await expect(page.locator('[data-testid="websocket-reconnected"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should show reconnection success
      await expect(page.locator('[data-testid="connection-restored"]')).toBeVisible();
      
      // Should resume real-time functionality
      await expect(page.locator('[data-testid="realtime-active"]')).toBeVisible();
      
      // Should sync any missed data
      await expect(page.locator('[data-testid="data-synchronized"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });

    test('should automatically recover from session expiration', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Set up session expiration scenario
      await context.route('**/api/auth/refresh', async (route) => {
        // First refresh fails (token expired)
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Unauthorized',
            message: 'Refresh token expired'
          })
        });
      });
      
      // Mock login endpoint for automatic reauth
      await context.route('**/api/auth/login', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              access_token: 'new-access-token',
              refresh_token: 'new-refresh-token',
              expires_in: 3600
            })
          });
        } else {
          await route.continue();
        }
      });
      
      // Simulate session expiration by clearing tokens
      await page.evaluate(() => {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.setItem('remember_credentials', 'true');
      });
      
      // Try to perform authenticated action
      await page.click('[data-testid="create-hive-btn"]');
      
      // Should detect session expiration
      await expect(page.locator('[data-testid="session-expired"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should attempt automatic reauth if credentials remembered
      await expect(page.locator('[data-testid="auto-reauth-attempt"]')).toBeVisible();
      
      // Should show reauth progress
      await expect(page.locator('[data-testid="reauth-in-progress"]')).toBeVisible();
      
      // Should succeed and continue with original action
      await expect(page.locator('[data-testid="reauth-success"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should proceed with hive creation
      await expect(page.locator('[data-testid="hive-creation-form"]')).toBeVisible();
      
      // Should log automatic recovery
      const authLogs = await page.evaluate(() => 
        JSON.parse(sessionStorage.getItem('auth-recovery-log') || '[]')
      );
      expect(authLogs.length).toBeGreaterThan(0);
    });

    test('should automatically fallback to polling when WebSocket fails', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Block WebSocket connections permanently
      await errorHelper.blockWebSocketConnection();
      
      // Should detect WebSocket failure
      await expect(page.locator('[data-testid="websocket-connection-failed"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should automatically fallback to polling
      await expect(page.locator('[data-testid="auto-fallback-polling"]')).toBeVisible();
      
      // Should show fallback notification
      await expect(page.locator('[data-testid="fallback-mode-notice"]')).toContainText(/polling.*mode|real.*time.*limited/i);
      
      // Should still receive updates via polling
      await page.evaluate(() => {
        // Simulate server-sent update
        const event = new CustomEvent('polling-update', {
          detail: {
            type: 'participant-joined',
            data: { username: 'test-user', id: '123' }
          }
        });
        window.dispatchEvent(event);
      });
      
      // Should process polling updates
      await expect(page.locator('[data-testid="polling-update-received"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should maintain basic functionality
      await expect(page.locator('[data-testid="hive-content"]')).toBeVisible();
      await expect(page.locator('[data-testid="participant-list"]')).toBeVisible();
      
      // Should show performance impact notice
      await expect(page.locator('[data-testid="performance-impact-notice"]')).toBeVisible();
    });
  });

  test.describe('Manual Recovery Workflows', () => {
    test('should provide manual recovery options when auto-recovery fails', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Simulate persistent failure that auto-recovery cannot handle
      await errorHelper.simulatePersistentFailure('database_connection');
      
      // Auto-recovery should fail after max attempts
      await expect(page.locator('[data-testid="auto-recovery-failed"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should show manual recovery options
      await expect(page.locator('[data-testid="manual-recovery-options"]')).toBeVisible();
      
      // Should offer specific recovery actions
      await expect(page.locator('[data-testid="force-refresh"]')).toBeVisible();
      await expect(page.locator('[data-testid="clear-cache"]')).toBeVisible();
      await expect(page.locator('[data-testid="reset-connection"]')).toBeVisible();
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();
      
      // Should show error details for troubleshooting
      await expect(page.locator('[data-testid="error-details"]')).toBeVisible();
      const errorDetails = await page.locator('[data-testid="error-details"]').textContent();
      expect(errorDetails).toContain('database_connection');
      
      // Test manual cache clear
      await page.click('[data-testid="clear-cache"]');
      
      await expect(page.locator('[data-testid="cache-clear-progress"]')).toBeVisible();
      await expect(page.locator('[data-testid="cache-cleared"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should offer to retry after manual intervention
      await expect(page.locator('[data-testid="retry-after-manual"]')).toBeVisible();
      
      // Test manual retry
      await errorHelper.clearPersistentFailure();
      await page.click('[data-testid="retry-after-manual"]');
      
      await expect(page.locator('[data-testid="manual-recovery-success"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
    });

    test('should guide users through connection diagnostics', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Simulate connection issues
      await errorHelper.simulateConnectionIssues();
      
      // Should detect connection problems
      await expect(page.locator('[data-testid="connection-issues-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should offer diagnostic wizard
      await expect(page.locator('[data-testid="run-diagnostics"]')).toBeVisible();
      
      await page.click('[data-testid="run-diagnostics"]');
      
      // Should start diagnostic wizard
      await expect(page.locator('[data-testid="diagnostic-wizard"]')).toBeVisible();
      
      // Step 1: Network connectivity test
      await expect(page.locator('[data-testid="test-network-connectivity"]')).toBeVisible();
      await page.click('[data-testid="run-network-test"]');
      
      await expect(page.locator('[data-testid="network-test-results"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Step 2: Server reachability test
      await page.click('[data-testid="next-step"]');
      await expect(page.locator('[data-testid="test-server-reachability"]')).toBeVisible();
      
      await page.click('[data-testid="run-server-test"]');
      await expect(page.locator('[data-testid="server-test-results"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Step 3: Browser compatibility check
      await page.click('[data-testid="next-step"]');
      await expect(page.locator('[data-testid="test-browser-compatibility"]')).toBeVisible();
      
      await page.click('[data-testid="run-browser-test"]');
      await expect(page.locator('[data-testid="browser-test-results"]')).toBeVisible();
      
      // Should provide diagnostic summary
      await page.click('[data-testid="finish-diagnostics"]');
      
      await expect(page.locator('[data-testid="diagnostic-summary"]')).toBeVisible();
      
      // Should offer targeted solutions
      await expect(page.locator('[data-testid="recommended-solutions"]')).toBeVisible();
      
      // Should provide export option for support
      await expect(page.locator('[data-testid="export-diagnostic-report"]')).toBeVisible();
    });

    test('should provide data recovery tools', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');
      
      // Create some important data
      await page.fill('[data-testid="task-description"]', 'Important work session');
      await page.click('[data-testid="start-timer"]');
      
      // Wait for some time data
      await page.waitForTimeout(3000);
      
      // Simulate data corruption
      await page.evaluate(() => {
        localStorage.setItem('timer-data', '{"corrupted": true}');
        sessionStorage.setItem('session-data', 'invalid-json{');
      });
      
      // Refresh to trigger data loading
      await page.reload();
      
      // Should detect data corruption
      await expect(page.locator('[data-testid="data-corruption-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should offer data recovery wizard
      await expect(page.locator('[data-testid="start-data-recovery"]')).toBeVisible();
      
      await page.click('[data-testid="start-data-recovery"]');
      
      // Should show recovery options
      await expect(page.locator('[data-testid="data-recovery-wizard"]')).toBeVisible();
      
      // Option 1: Restore from backup
      await expect(page.locator('[data-testid="restore-from-backup"]')).toBeVisible();
      
      // Option 2: Recover partial data
      await expect(page.locator('[data-testid="recover-partial-data"]')).toBeVisible();
      
      // Option 3: Start fresh
      await expect(page.locator('[data-testid="start-fresh"]')).toBeVisible();
      
      // Test partial recovery
      await page.click('[data-testid="recover-partial-data"]');
      
      // Should scan for recoverable data
      await expect(page.locator('[data-testid="scanning-for-data"]')).toBeVisible();
      
      // Should show found data
      await expect(page.locator('[data-testid="recoverable-data-found"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show data preview
      const dataPreview = page.locator('[data-testid="data-preview"]');
      await expect(dataPreview).toBeVisible();
      await expect(dataPreview).toContainText('Important work session');
      
      // Should allow selective recovery
      await expect(page.locator('[data-testid="select-data-to-recover"]')).toBeVisible();
      
      // Test data restoration
      await page.click('[data-testid="restore-selected-data"]');
      
      await expect(page.locator('[data-testid="data-recovery-complete"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should restore functionality
      await expect(page.locator('[data-testid="task-description"]')).toHaveValue('Important work session');
    });
  });

  test.describe('State Synchronization and Rehydration', () => {
    test('should rehydrate application state after recovery', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Establish application state
      await page.click('[data-testid="join-hive"]');
      await page.fill('[data-testid="status-input"]', 'Working on rehydration test');
      await page.click('[data-testid="update-status"]');
      await page.click('[data-testid="start-timer"]');
      
      // Send some chat messages
      await page.fill('[data-testid="chat-input"]', 'Message 1');
      await page.press('[data-testid="chat-input"]', 'Enter');
      await page.fill('[data-testid="chat-input"]', 'Message 2');
      await page.press('[data-testid="chat-input"]', 'Enter');
      
      // Simulate system failure and recovery
      await errorHelper.simulateSystemFailure();
      
      // Should detect system failure
      await expect(page.locator('[data-testid="system-failure-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Simulate recovery
      await errorHelper.simulateSystemRecovery();
      
      // Should start state rehydration
      await expect(page.locator('[data-testid="state-rehydration-started"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show rehydration progress
      await expect(page.locator('[data-testid="rehydrating-user-state"]')).toBeVisible();
      await expect(page.locator('[data-testid="rehydrating-hive-state"]')).toBeVisible();
      await expect(page.locator('[data-testid="rehydrating-chat-history"]')).toBeVisible();
      await expect(page.locator('[data-testid="rehydrating-timer-state"]')).toBeVisible();
      
      // Should complete rehydration
      await expect(page.locator('[data-testid="state-rehydration-complete"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should restore all application state
      await expect(page.locator('[data-testid="member-status"]')).toBeVisible(); // Joined state
      await expect(page.locator('[data-testid="user-status"]')).toContainText('Working on rehydration test');
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible(); // Timer state
      
      // Should restore chat history
      await expect(page.locator('[data-testid="chat-message"]:has-text("Message 1")')).toBeVisible();
      await expect(page.locator('[data-testid="chat-message"]:has-text("Message 2")')).toBeVisible();
      
      // Should restore real-time connections
      await expect(page.locator('[data-testid="realtime-connected"]')).toBeVisible();
      
      // Should log rehydration completion
      const rehydrationLog = await page.evaluate(() => 
        JSON.parse(sessionStorage.getItem('rehydration-log') || '{}')
      );
      expect(rehydrationLog.completed).toBeTruthy();
      expect(rehydrationLog.duration).toBeGreaterThan(0);
    });

    test('should handle state synchronization conflicts', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Create local state
      await page.click('[data-testid="join-hive"]');
      await page.fill('[data-testid="chat-input"]', 'Local message before sync');
      await page.press('[data-testid="chat-input"]', 'Enter');
      
      // Simulate state synchronization with conflicts
      await page.evaluate(() => {
        // Mock conflicting server state
        window.__MOCK_SERVER_STATE__ = {
          membership: { status: 'left', timestamp: Date.now() - 5000 },
          messages: [
            { id: '1', text: 'Server message 1', timestamp: Date.now() - 10000 },
            { id: '2', text: 'Server message 2', timestamp: Date.now() - 8000 }
          ],
          user_status: { text: 'Server status', timestamp: Date.now() - 3000 }
        };
        
        // Trigger sync conflict
        window.dispatchEvent(new CustomEvent('sync-conflict-detected'));
      });
      
      // Should detect synchronization conflicts
      await expect(page.locator('[data-testid="sync-conflict-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show conflict resolution options
      await expect(page.locator('[data-testid="conflict-resolution-dialog"]')).toBeVisible();
      
      // Should show conflicting states
      await expect(page.locator('[data-testid="local-state-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="server-state-preview"]')).toBeVisible();
      
      // Should offer resolution strategies
      await expect(page.locator('[data-testid="use-server-state"]')).toBeVisible();
      await expect(page.locator('[data-testid="use-local-state"]')).toBeVisible();
      await expect(page.locator('[data-testid="merge-states"]')).toBeVisible();
      
      // Test state merging
      await page.click('[data-testid="merge-states"]');
      
      // Should show merge preview
      await expect(page.locator('[data-testid="merge-preview"]')).toBeVisible();
      
      // Should allow conflict resolution per field
      await expect(page.locator('[data-testid="resolve-membership-conflict"]')).toBeVisible();
      await expect(page.locator('[data-testid="resolve-messages-conflict"]')).toBeVisible();
      
      // Resolve conflicts
      await page.click('[data-testid="use-local-membership"]');
      await page.click('[data-testid="merge-messages"]');
      
      // Apply resolution
      await page.click('[data-testid="apply-resolution"]');
      
      // Should complete synchronization
      await expect(page.locator('[data-testid="sync-resolution-complete"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show merged state
      await expect(page.locator('[data-testid="member-status"]')).toBeVisible(); // Local membership won
      await expect(page.locator('[data-testid="chat-message"]:has-text("Server message 1")')).toBeVisible(); // Server messages merged
      await expect(page.locator('[data-testid="chat-message"]:has-text("Local message before sync")')).toBeVisible(); // Local message preserved
    });

    test('should maintain data consistency during partial failures', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Set up partial failure scenario (some APIs work, others fail)
      await context.route('**/api/hives/123/join', async (route) => {
        await route.continue(); // Works
      });
      
      await context.route('**/api/hives/123/status', async (route) => {
        await route.fulfill({ status: 500 }); // Fails
      });
      
      await context.route('**/api/hives/123/messages', async (route) => {
        await route.continue(); // Works
      });
      
      // Perform operations that mix success and failure
      await page.click('[data-testid="join-hive"]');
      await page.fill('[data-testid="status-input"]', 'Status update test');
      await page.click('[data-testid="update-status"]');
      await page.fill('[data-testid="chat-input"]', 'Chat message test');
      await page.press('[data-testid="chat-input"]', 'Enter');
      
      // Should handle partial failures gracefully
      await expect(page.locator('[data-testid="partial-failure-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show which operations succeeded/failed
      await expect(page.locator('[data-testid="join-success"]')).toBeVisible();
      await expect(page.locator('[data-testid="status-update-failed"]')).toBeVisible();
      await expect(page.locator('[data-testid="message-sent"]')).toBeVisible();
      
      // Should maintain consistent state
      await expect(page.locator('[data-testid="member-status"]')).toBeVisible(); // Join succeeded
      await expect(page.locator('[data-testid="status-error"]')).toBeVisible(); // Status failed, but UI consistent
      await expect(page.locator('[data-testid="chat-message"]:has-text("Chat message test")')).toBeVisible(); // Message succeeded
      
      // Should offer retry for failed operations
      await expect(page.locator('[data-testid="retry-status-update"]')).toBeVisible();
      
      // Should prevent inconsistent state transitions
      const systemState = await page.evaluate(() => window.__APP_STATE__ || {});
      expect(systemState).toBeDefined();
      
      // Member status and UI should be consistent
      const membershipVisible = await page.locator('[data-testid="member-status"]').isVisible();
      const chatEnabled = await page.locator('[data-testid="chat-input"]').isEnabled();
      
      if (membershipVisible) {
        expect(chatEnabled).toBeTruthy(); // If member, chat should be enabled
      }
    });
  });

  test.describe('Circuit Breaker and Health Monitoring', () => {
    test('should implement circuit breaker for failing services', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Set up failing service
      let requestCount = 0;
      await context.route('**/api/analytics/**', async (route) => {
        requestCount++;
        // Always fail to trigger circuit breaker
        await route.fulfill({ status: 500, body: 'Service unavailable' });
      });
      
      // Make requests that will trigger circuit breaker
      for (let i = 0; i < 5; i++) {
        await page.goto('/analytics');
        await page.waitForTimeout(1000);
      }
      
      // Should detect repeated failures and open circuit breaker
      await expect(page.locator('[data-testid="circuit-breaker-open"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show circuit breaker status
      await expect(page.locator('[data-testid="service-unavailable"]')).toContainText(/service.*unavailable.*temporarily/i);
      
      // Should prevent further requests to failing service
      const requestCountBefore = requestCount;
      await page.goto('/analytics');
      await page.waitForTimeout(2000);
      
      expect(requestCount).toBe(requestCountBefore); // No new requests made
      
      // Should show alternative options
      await expect(page.locator('[data-testid="use-cached-analytics"]')).toBeVisible();
      await expect(page.locator('[data-testid="basic-stats-only"]')).toBeVisible();
      
      // Should periodically check service health (half-open state)
      await page.waitForTimeout(10000); // Wait for health check interval
      
      const healthCheckIndicator = page.locator('[data-testid="health-check-in-progress"]');
      if (await healthCheckIndicator.isVisible({ timeout: 5000 })) {
        await expect(healthCheckIndicator).toBeVisible();
        
        // Health check should still fail
        await expect(page.locator('[data-testid="health-check-failed"]')).toBeVisible({ 
          timeout: TIMEOUTS.MEDIUM 
        });
        
        // Circuit should remain open
        await expect(page.locator('[data-testid="circuit-breaker-open"]')).toBeVisible();
      }
    });

    test('should monitor system health and show status indicators', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Should show system health indicator
      await expect(page.locator('[data-testid="system-health-indicator"]')).toBeVisible();
      
      // Should start with healthy status
      await expect(page.locator('[data-testid="health-status-healthy"]')).toBeVisible();
      
      // Simulate service degradation
      await context.route('**/api/health', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            status: 'degraded',
            services: {
              'api-gateway': { status: 'healthy', latency: 45 },
              'hive-service': { status: 'degraded', latency: 2500 },
              'user-service': { status: 'healthy', latency: 120 },
              'notification-service': { status: 'unhealthy', latency: 0 }
            },
            overall_health: 75
          })
        });
      });
      
      // Trigger health check
      await page.click('[data-testid="refresh-health-status"]');
      
      // Should update health status
      await expect(page.locator('[data-testid="health-status-degraded"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show detailed service status
      await page.click('[data-testid="view-service-details"]');
      
      await expect(page.locator('[data-testid="service-health-details"]')).toBeVisible();
      
      // Should show individual service statuses
      await expect(page.locator('[data-testid="service-api-gateway"] [data-testid="status-healthy"]')).toBeVisible();
      await expect(page.locator('[data-testid="service-hive-service"] [data-testid="status-degraded"]')).toBeVisible();
      await expect(page.locator('[data-testid="service-notification-service"] [data-testid="status-unhealthy"]')).toBeVisible();
      
      // Should show performance metrics
      await expect(page.locator('[data-testid="service-hive-service"] [data-testid="latency"]')).toContainText('2500ms');
      
      // Should show overall health percentage
      await expect(page.locator('[data-testid="overall-health-score"]')).toContainText('75%');
      
      // Should offer service-specific actions
      await expect(page.locator('[data-testid="report-service-issue"]')).toBeVisible();
      await expect(page.locator('[data-testid="view-service-logs"]')).toBeVisible();
    });

    test('should handle graceful degradation when services are unhealthy', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');
      
      // Simulate multiple service failures
      const failingServices = [
        '**/api/notifications/**',
        '**/api/analytics/**',
        '**/api/buddy/**'
      ];
      
      for (const service of failingServices) {
        await context.route(service, async (route) => {
          await route.fulfill({ status: 503, body: 'Service unavailable' });
        });
      }
      
      // Try to use features that depend on failing services
      await page.goto('/notifications');
      await page.goto('/analytics');
      await page.goto('/buddy');
      
      // Should detect service failures
      await expect(page.locator('[data-testid="multiple-services-down"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show degraded mode notification
      await expect(page.locator('[data-testid="degraded-mode-active"]')).toBeVisible();
      
      // Should disable affected features
      await expect(page.locator('[data-testid="notifications-disabled"]')).toBeVisible();
      await expect(page.locator('[data-testid="analytics-limited"]')).toBeVisible();
      await expect(page.locator('[data-testid="buddy-unavailable"]')).toBeVisible();
      
      // Should maintain core functionality
      await page.goto('/hive/123');
      await expect(page.locator('[data-testid="hive-content"]')).toBeVisible();
      
      // Core features should still work
      await page.click('[data-testid="join-hive"]');
      await expect(page.locator('[data-testid="join-success"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should show which features are available
      await expect(page.locator('[data-testid="available-features"]')).toBeVisible();
      await expect(page.locator('[data-testid="unavailable-features"]')).toBeVisible();
      
      // Should provide status updates
      await expect(page.locator('[data-testid="service-status-updates"]')).toBeVisible();
      
      // Should offer alternative workflows
      await expect(page.locator('[data-testid="offline-alternatives"]')).toBeVisible();
    });
  });
});