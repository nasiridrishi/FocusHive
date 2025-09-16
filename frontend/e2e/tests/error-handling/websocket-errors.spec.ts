/**
 * WebSocket Error Handling E2E Tests for FocusHive
 * Comprehensive testing of real-time connection failures and recovery mechanisms
 *
 * Test Coverage:
 * - Connection establishment failures
 * - Unexpected disconnections
 * - Reconnection with exponential backoff
 * - Message delivery failures
 * - Protocol errors and malformed messages
 * - Heartbeat timeout scenarios
 * - Maximum reconnection attempts
 * - Concurrent connection limits
 * - Message queue management during outages
 */

import {BrowserContext, expect, Page, test} from '@playwright/test';
import {ErrorHandlingHelper} from '../../helpers/error-handling.helper';
import {AuthHelper} from '../../helpers/auth-helpers';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('WebSocket Error Handling', () => {
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

    // Set up WebSocket monitoring
    await errorHelper.setupWebSocketMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
    await errorHelper.clearWebSocketMocks();
  });

  test.describe('Connection Establishment Failures', () => {
    test('should handle WebSocket connection failure on page load', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Block WebSocket connection
      await errorHelper.blockWebSocketConnection();

      await page.goto('/hive/123');

      // Should show connection error
      await expect(page.locator('[data-testid="websocket-connection-failed"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Verify fallback to polling mode
      await expect(page.locator('[data-testid="polling-mode-notice"]')).toBeVisible();
      await expect(page.locator('[data-testid="realtime-status"]')).toContainText(/polling|degraded/i);

      // Basic functionality should still work
      await expect(page.locator('[data-testid="hive-content"]')).toBeVisible();
      await expect(page.locator('[data-testid="participant-list"]')).toBeVisible();

      // Should offer retry connection
      await expect(page.locator('[data-testid="retry-websocket"]')).toBeVisible();

      // Test manual retry
      await errorHelper.allowWebSocketConnection();
      await page.click('[data-testid="retry-websocket"]');

      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });
      await expect(page.locator('[data-testid="realtime-status"]')).toContainText(/connected|live/i);
    });

    test('should handle WebSocket protocol mismatch errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock WebSocket protocol error
      await page.addInitScript(() => {
        interface MockWebSocketInterface {
          readonly CONNECTING: number;
          readonly OPEN: number;
          readonly CLOSING: number;
          readonly CLOSED: number;

          new(url: string, protocols?: string | string[]): WebSocket;
        }

        const _originalWebSocket = window.WebSocket;
        window.WebSocket = class MockWebSocket extends EventTarget implements WebSocket {
          readonly CONNECTING = WebSocket.CONNECTING;
          readonly OPEN = WebSocket.OPEN;
          readonly CLOSING = WebSocket.CLOSING;
          readonly CLOSED = WebSocket.CLOSED;

          binaryType: BinaryType = 'blob';
          bufferedAmount: number = 0;
          extensions: string = '';
          protocol: string = '';
          url: string;
          onopen: ((this: WebSocket, ev: Event) => unknown) | null = null;
          onclose: ((this: WebSocket, ev: CloseEvent) => unknown) | null = null;
          onerror: ((this: WebSocket, ev: Event) => unknown) | null = null;
          onmessage: ((this: WebSocket, ev: MessageEvent) => unknown) | null = null;

          constructor(url: string, _protocols?: string | string[]) {
            super();
            this.url = url;
            setTimeout(() => {
              this.dispatchEvent(new Event('error'));
              this.dispatchEvent(new CloseEvent('close', {
                code: 1002,
                reason: 'Protocol error',
                wasClean: false
              }));
            }, 100);
          }

          get readyState(): number {
            return WebSocket.CONNECTING;
          }

          close(): void {
          }

          send(): void {
          }
        } as MockWebSocketInterface;
      });

      await page.goto('/hive/123');

      // Should detect protocol error
      await expect(page.locator('[data-testid="websocket-protocol-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show appropriate error message
      const protocolError = page.locator('[data-testid="protocol-error-message"]');
      await expect(protocolError).toContainText(/protocol.*error|version.*mismatch/i);

      // Should not retry automatically for protocol errors
      await expect(page.locator('[data-testid="auto-retry-disabled"]')).toBeVisible();

      // Should offer manual diagnostics
      await expect(page.locator('[data-testid="run-connection-test"]')).toBeVisible();
    });

    test('should handle WebSocket security/certificate errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock WebSocket security error
      await page.addInitScript(() => {
        interface MockWebSocketConstructor {
          readonly CONNECTING: number;
          readonly OPEN: number;
          readonly CLOSING: number;
          readonly CLOSED: number;

          new(): WebSocket;
        }

        window.WebSocket = class MockWebSocket extends EventTarget implements WebSocket {
          readonly CONNECTING = WebSocket.CONNECTING;
          readonly OPEN = WebSocket.OPEN;
          readonly CLOSING = WebSocket.CLOSING;
          readonly CLOSED = WebSocket.CLOSED;

          binaryType: BinaryType = 'blob';
          bufferedAmount: number = 0;
          extensions: string = '';
          protocol: string = '';
          url: string = '';
          onopen: ((this: WebSocket, ev: Event) => unknown) | null = null;
          onclose: ((this: WebSocket, ev: CloseEvent) => unknown) | null = null;
          onerror: ((this: WebSocket, ev: Event) => unknown) | null = null;
          onmessage: ((this: WebSocket, ev: MessageEvent) => unknown) | null = null;

          constructor() {
            super();
            setTimeout(() => {
              interface ErrorEventWithCode extends Event {
                code: string;
              }

              const error = new Event('error') as ErrorEventWithCode;
              error.code = 'CERT_UNTRUSTED';
              this.dispatchEvent(error);
              this.dispatchEvent(new CloseEvent('close', {
                code: 1015,
                reason: 'TLS handshake failure',
                wasClean: false
              }));
            }, 100);
          }

          get readyState(): number {
            return WebSocket.CLOSED;
          }

          close(): void {
          }

          send(): void {
          }
        } as MockWebSocketConstructor;
      });

      await page.goto('/hive/123');

      // Should detect security error
      await expect(page.locator('[data-testid="websocket-security-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show security-specific messaging
      const securityMessage = page.locator('[data-testid="security-error-message"]');
      await expect(securityMessage).toContainText(/certificate|security|tls|ssl/i);

      // Should not offer unsafe workarounds
      await expect(page.locator('button:has-text("Ignore Certificate")')).not.toBeVisible();

      // Should offer safe alternatives
      await expect(page.locator('[data-testid="contact-admin"]')).toBeVisible();
      await expect(page.locator('[data-testid="use-polling-mode"]')).toBeVisible();
    });
  });

  test.describe('Unexpected Disconnections', () => {
    test('should detect sudden WebSocket disconnection', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for initial connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate sudden disconnection
      await errorHelper.simulateWebSocketDisconnection('network_error');

      // Should detect disconnection quickly
      await expect(page.locator('[data-testid="websocket-disconnected"]')).toBeVisible({
        timeout: TIMEOUTS.SHORT
      });

      // Should show appropriate status indicator
      await expect(page.locator('[data-testid="connection-status"]')).toHaveClass(/disconnected|error/);

      // Should show reconnecting indicator
      await expect(page.locator('[data-testid="reconnecting-indicator"]')).toBeVisible();

      // Should buffer outgoing messages
      await page.fill('[data-testid="chat-input"]', 'Test message during disconnection');
      await page.press('[data-testid="chat-input"]', 'Enter');

      await expect(page.locator('[data-testid="message-queued"]')).toBeVisible();
      await expect(page.locator('[data-testid="queued-message-count"]')).toContainText('1');
    });

    test('should handle server-initiated disconnection gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate server-initiated close
      await errorHelper.simulateWebSocketDisconnection('server_shutdown', {
        code: 1001,
        reason: 'Server maintenance',
        wasClean: true
      });

      // Should show server shutdown message
      await expect(page.locator('[data-testid="server-maintenance"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show maintenance-specific messaging
      const maintenanceMessage = page.locator('[data-testid="maintenance-message"]');
      await expect(maintenanceMessage).toContainText(/maintenance|server.*shutdown/i);

      // Should not attempt reconnection during maintenance
      await expect(page.locator('[data-testid="reconnection-suspended"]')).toBeVisible();

      // Should offer status updates
      await expect(page.locator('[data-testid="maintenance-status-link"]')).toBeVisible();
    });

    test('should handle abnormal connection termination', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate abnormal termination
      await errorHelper.simulateWebSocketDisconnection('abnormal', {
        code: 1006,
        reason: '',
        wasClean: false
      });

      // Should detect abnormal closure
      await expect(page.locator('[data-testid="abnormal-disconnection"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should start aggressive reconnection
      await expect(page.locator('[data-testid="aggressive-reconnect"]')).toBeVisible();

      // Should show connection diagnostic tools
      await expect(page.locator('[data-testid="connection-diagnostics"]')).toBeVisible();

      // Should preserve user data
      await page.fill('[data-testid="timer-description"]', 'Abnormal disconnect test');
      await page.click('[data-testid="start-timer"]');

      // Data should be preserved in local storage
      const timerData = await page.evaluate(() =>
          JSON.parse(localStorage.getItem('currentTimer') || '{}')
      );
      expect(timerData.description).toBe('Abnormal disconnect test');
    });
  });

  test.describe('Reconnection Logic', () => {
    test('should implement exponential backoff reconnection', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for initial connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Track reconnection attempts
      const _reconnectTimes: number[] = [];
      await page.evaluate(() => {
        window.reconnectTimes = [];
        window.addEventListener('websocket-reconnect-attempt', () => {
          window.reconnectTimes.push(Date.now());
        });
      });

      // Simulate connection failure that persists for multiple attempts
      await errorHelper.simulateIntermittentWebSocketFailure(3);

      // Should show multiple reconnection attempts
      await expect(page.locator('[data-testid="reconnect-attempt-1"]')).toBeVisible();
      await expect(page.locator('[data-testid="reconnect-attempt-2"]')).toBeVisible();
      await expect(page.locator('[data-testid="reconnect-attempt-3"]')).toBeVisible();

      // Verify exponential backoff timing
      const times = await page.evaluate(() => window.reconnectTimes) as number[];
      if (times.length >= 3) {
        const delay1 = times[1] - times[0];
        const delay2 = times[2] - times[1];
        expect(delay2).toBeGreaterThan(delay1 * 1.5); // Exponential increase
      }

      // Should show backoff countdown
      await expect(page.locator('[data-testid="reconnect-countdown"]')).toBeVisible();

      // Eventually should reconnect successfully
      await errorHelper.allowWebSocketConnection();
      await expect(page.locator('[data-testid="websocket-reconnected"]')).toBeVisible({
        timeout: TIMEOUTS.LONG
      });
    });

    test('should handle maximum reconnection attempts gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate persistent connection failure
      await errorHelper.simulatePersistentWebSocketFailure();

      // Should attempt reconnections up to limit
      await expect(page.locator('[data-testid="max-reconnect-attempts"]')).toBeVisible({
        timeout: TIMEOUTS.LONG
      });

      // Should give up gracefully
      const maxAttemptsMessage = page.locator('[data-testid="max-attempts-message"]');
      await expect(maxAttemptsMessage).toContainText(/maximum.*attempts|gave up reconnecting/i);

      // Should offer manual reconnection
      await expect(page.locator('[data-testid="manual-reconnect"]')).toBeVisible();

      // Should fall back to polling mode automatically
      await expect(page.locator('[data-testid="fallback-polling"]')).toBeVisible();
      await expect(page.locator('[data-testid="polling-mode-active"]')).toBeVisible();

      // Manual reconnect should work
      await errorHelper.allowWebSocketConnection();
      await page.click('[data-testid="manual-reconnect"]');

      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });
    });

    test('should preserve message order during reconnection', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Send some messages before disconnection
      for (let i = 1; i <= 3; i++) {
        await page.fill('[data-testid="chat-input"]', `Pre-disconnect message ${i}`);
        await page.press('[data-testid="chat-input"]', 'Enter');
        await page.waitForTimeout(100);
      }

      // Simulate disconnection
      await errorHelper.simulateWebSocketDisconnection('network_error');

      // Send messages during disconnection (should be queued)
      for (let i = 1; i <= 3; i++) {
        await page.fill('[data-testid="chat-input"]', `Queued message ${i}`);
        await page.press('[data-testid="chat-input"]', 'Enter');
        await page.waitForTimeout(100);
      }

      // Verify messages are queued
      await expect(page.locator('[data-testid="queued-message-count"]')).toContainText('3');

      // Reconnect
      await errorHelper.allowWebSocketConnection();
      await expect(page.locator('[data-testid="websocket-reconnected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Verify all messages appear in correct order
      const messages = page.locator('[data-testid="chat-message"]');
      await expect(messages).toHaveCount(6, {timeout: TIMEOUTS.MEDIUM});

      // Check message order
      await expect(messages.nth(0)).toContainText('Pre-disconnect message 1');
      await expect(messages.nth(3)).toContainText('Queued message 1');
      await expect(messages.nth(5)).toContainText('Queued message 3');

      // Queue should be cleared
      await expect(page.locator('[data-testid="queued-message-count"]')).toContainText('0');
    });
  });

  test.describe('Message Delivery Failures', () => {
    test('should handle message send failures with retry', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate message send failure
      await errorHelper.simulateWebSocketSendFailure();

      // Send a message
      await page.fill('[data-testid="chat-input"]', 'Test message with send failure');
      await page.press('[data-testid="chat-input"]', 'Enter');

      // Should show sending status
      const lastMessage = page.locator('[data-testid="chat-message"]').last();
      await expect(lastMessage.locator('[data-testid="message-status"]')).toHaveClass(/sending/);

      // Should show send failure
      await expect(lastMessage.locator('[data-testid="message-status"]')).toHaveClass(/failed/, {
        timeout: TIMEOUTS.MEDIUM
      });

      // Should offer retry option
      const retryButton = lastMessage.locator('[data-testid="retry-send"]');
      await expect(retryButton).toBeVisible();

      // Test retry functionality
      await errorHelper.allowWebSocketSending();
      await retryButton.click();

      await expect(lastMessage.locator('[data-testid="message-status"]')).toHaveClass(/sent|delivered/);
    });

    test('should handle malformed message errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate receiving malformed message
      await errorHelper.simulateMalformedWebSocketMessage();

      // Should handle malformed message gracefully
      await expect(page.locator('[data-testid="malformed-message-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should not crash the application
      await expect(page.locator('[data-testid="chat-container"]')).toBeVisible();

      // Should log error for debugging
      const consoleLogs = await errorHelper.getConsoleErrors();
      expect(consoleLogs.some(log => log.includes('malformed') || log.includes('parse'))).toBeTruthy();

      // Should continue processing other messages
      await page.fill('[data-testid="chat-input"]', 'Normal message after error');
      await page.press('[data-testid="chat-input"]', 'Enter');

      await expect(page.locator('[data-testid="chat-message"]:has-text("Normal message after error")')).toBeVisible();
    });

    test('should handle message size limit errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Attempt to send oversized message
      const largeMessage = 'A'.repeat(10000); // 10KB message
      await page.fill('[data-testid="chat-input"]', largeMessage);
      await page.press('[data-testid="chat-input"]', 'Enter');

      // Should show size limit error
      await expect(page.locator('[data-testid="message-too-large"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show size limit information
      const sizeLimitMessage = page.locator('[data-testid="size-limit-message"]');
      await expect(sizeLimitMessage).toContainText(/too large|size limit|maximum.*characters/i);

      // Should offer message truncation option
      await expect(page.locator('[data-testid="truncate-message"]')).toBeVisible();

      // Should keep original message in input for editing
      const inputValue = await page.inputValue('[data-testid="chat-input"]');
      expect(inputValue).toBe(largeMessage);
    });
  });

  test.describe('Heartbeat and Keep-Alive', () => {
    test('should detect heartbeat timeout and reconnect', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate heartbeat timeout
      await errorHelper.simulateHeartbeatTimeout();

      // Should detect heartbeat failure
      await expect(page.locator('[data-testid="heartbeat-timeout"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should attempt reconnection
      await expect(page.locator('[data-testid="reconnecting-heartbeat"]')).toBeVisible();

      // Should show connection health information
      await expect(page.locator('[data-testid="connection-health-poor"]')).toBeVisible();

      // Should eventually reconnect
      await errorHelper.restoreHeartbeat();
      await expect(page.locator('[data-testid="heartbeat-restored"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      await expect(page.locator('[data-testid="connection-health-good"]')).toBeVisible();
    });

    test('should handle keep-alive mechanism failures', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Monitor keep-alive messages
      const keepAliveCount = await page.evaluate(() => {
        let count = 0;
        window.addEventListener('websocket-keepalive', () => count++);
        return count;
      });

      // Simulate keep-alive failure
      await errorHelper.simulateKeepAliveFailure();

      // Should detect keep-alive issues
      await expect(page.locator('[data-testid="keepalive-failed"]')).toBeVisible({
        timeout: TIMEOUTS.LONG
      });

      // Should show connection degradation warning
      await expect(page.locator('[data-testid="connection-unstable"]')).toBeVisible();

      // Should increase keep-alive frequency
      await page.waitForTimeout(5000);
      const newKeepAliveCount = await page.evaluate(() => window.keepAliveAttempts || 0);
      expect(newKeepAliveCount).toBeGreaterThan(keepAliveCount + 2);
    });
  });

  test.describe('Concurrent Connection Management', () => {
    test('should handle multiple tab connection limits', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Open first tab
      await page.goto('/hive/123');
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Open second tab with same user
      const secondTab = await context.newPage();
      await secondTab.goto('/hive/123');

      // Should detect multiple connections
      await expect(secondTab.locator('[data-testid="multiple-connections-detected"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should offer connection management options
      await expect(secondTab.locator('[data-testid="close-other-connections"]')).toBeVisible();
      await expect(secondTab.locator('[data-testid="use-this-tab"]')).toBeVisible();

      // Should show connection conflict resolution
      const conflictMessage = secondTab.locator('[data-testid="connection-conflict"]');
      await expect(conflictMessage).toContainText(/multiple.*tabs|another.*session/i);

      // First tab should show conflict notification
      await expect(page.locator('[data-testid="session-conflict"]')).toBeVisible();

      // Resolve conflict by choosing primary connection
      await secondTab.click('[data-testid="use-this-tab"]');

      // First tab should show disconnection
      await expect(page.locator('[data-testid="connection-transferred"]')).toBeVisible();

      // Second tab should become primary
      await expect(secondTab.locator('[data-testid="primary-connection"]')).toBeVisible();

      await secondTab.close();
    });

    test('should handle session takeover scenarios', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate session takeover from another device
      await errorHelper.simulateSessionTakeover();

      // Should show session takeover notification
      await expect(page.locator('[data-testid="session-taken-over"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show device/location information if available
      const takeoverDetails = page.locator('[data-testid="takeover-details"]');
      await expect(takeoverDetails).toContainText(/another.*device|different.*location/i);

      // Should offer security options
      await expect(page.locator('[data-testid="secure-account"]')).toBeVisible();
      await expect(page.locator('[data-testid="view-sessions"]')).toBeVisible();

      // Should prevent unauthorized actions
      await expect(page.locator('[data-testid="readonly-mode"]')).toBeVisible();
      await expect(page.locator('button:has-text("Save")')).toBeDisabled();
    });
  });

  test.describe('Protocol and State Errors', () => {
    test('should handle WebSocket protocol version mismatch', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock protocol version error
      await page.addInitScript(() => {
        interface WebSocketConstructor {
          readonly CONNECTING: number;
          readonly OPEN: number;
          readonly CLOSING: number;
          readonly CLOSED: number;

          new(url: string, protocols?: string | string[]): WebSocket;
        }

        const originalWebSocket = window.WebSocket;
        window.WebSocket = class extends originalWebSocket {
          constructor(url: string, _protocols?: string | string[]) {
            super(url, protocols);
            setTimeout(() => {
              this.dispatchEvent(new Event('error'));
              this.dispatchEvent(new CloseEvent('close', {
                code: 1002,
                reason: 'Protocol version not supported',
                wasClean: false
              }));
            }, 500);
          }
        } as WebSocketConstructor;
      });

      await page.goto('/hive/123');

      // Should detect protocol version error
      await expect(page.locator('[data-testid="protocol-version-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should show version-specific error message
      const versionError = page.locator('[data-testid="version-error-message"]');
      await expect(versionError).toContainText(/protocol.*version|not supported/i);

      // Should suggest browser update
      await expect(page.locator('[data-testid="browser-update-suggestion"]')).toBeVisible();

      // Should offer fallback mode
      await expect(page.locator('[data-testid="enable-fallback-mode"]')).toBeVisible();
    });

    test('should handle WebSocket state synchronization errors', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Wait for connection
      await expect(page.locator('[data-testid="websocket-connected"]')).toBeVisible();

      // Simulate state desynchronization
      await errorHelper.simulateStateSyncError();

      // Should detect sync error
      await expect(page.locator('[data-testid="state-sync-error"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should offer state resynchronization
      await expect(page.locator('[data-testid="resync-state"]')).toBeVisible();

      // Should show current state conflicts
      await expect(page.locator('[data-testid="state-conflicts"]')).toBeVisible();

      // Test resynchronization
      await page.click('[data-testid="resync-state"]');

      // Should show resync progress
      await expect(page.locator('[data-testid="resyncing-state"]')).toBeVisible();

      // Should eventually resolve conflicts
      await expect(page.locator('[data-testid="state-synchronized"]')).toBeVisible({
        timeout: TIMEOUTS.MEDIUM
      });

      // Should resume normal operation
      await expect(page.locator('[data-testid="normal-operation"]')).toBeVisible();
    });
  });
});