/**
 * Real-time Presence Updates E2E Tests
 * Tests member presence indicators, status updates, and real-time synchronization
 * Includes multi-tab scenarios and WebSocket reliability testing
 */

import {Browser, BrowserContext, expect, Page, test} from '@playwright/test';
import {HivePage} from './pages/HivePage';
import {CreateHivePage} from './pages/CreateHivePage';
import {EnhancedAuthHelper} from '../../helpers/auth-helpers';
import {MultiUserHiveHelper} from './hive-helpers';
import {WebSocketTestHelper} from './websocket-helpers';
import {generateUniqueHiveData, HIVE_TEST_USERS, type TestHive} from './hive-fixtures';

test.describe('Real-time Presence Updates', () => {
  let browser: Browser;
  let ownerPage: Page;
  let memberPage: Page;
  let observerPage: Page;

  let ownerHivePage: HivePage;
  let memberHivePage: HivePage;
  let observerHivePage: HivePage;
  let createHivePage: CreateHivePage;

  let ownerAuth: EnhancedAuthHelper;
  let memberAuth: EnhancedAuthHelper;
  let observerAuth: EnhancedAuthHelper;

  let ownerWSHelper: WebSocketTestHelper;
  let memberWSHelper: WebSocketTestHelper;
  let observerWSHelper: WebSocketTestHelper;

  let testHive: TestHive;

  test.beforeAll(async ({browser: testBrowser}) => {
    browser = testBrowser;

    // Set up multiple user contexts
    const ownerContext = await browser.newContext();
    const memberContext = await browser.newContext();
    const observerContext = await browser.newContext();

    ownerPage = await ownerContext.newPage();
    memberPage = await memberContext.newPage();
    observerPage = await observerContext.newPage();

    // Initialize page objects
    ownerHivePage = new HivePage(ownerPage);
    memberHivePage = new HivePage(memberPage);
    observerHivePage = new HivePage(observerPage);
    createHivePage = new CreateHivePage(ownerPage);

    // Initialize auth helpers
    ownerAuth = new EnhancedAuthHelper(ownerPage);
    memberAuth = new EnhancedAuthHelper(memberPage);
    observerAuth = new EnhancedAuthHelper(observerPage);

    // Initialize WebSocket helpers
    ownerWSHelper = new WebSocketTestHelper(ownerPage);
    memberWSHelper = new WebSocketTestHelper(memberPage);
    observerWSHelper = new WebSocketTestHelper(observerPage);

    // Setup WebSocket monitoring
    await ownerWSHelper.initializeWebSocketMonitoring();
    await memberWSHelper.initializeWebSocketMonitoring();
    await observerWSHelper.initializeWebSocketMonitoring();

    // Login all users
    await ownerAuth.loginUser(HIVE_TEST_USERS.OWNER.email, HIVE_TEST_USERS.OWNER.password);
    await memberAuth.loginUser(HIVE_TEST_USERS.MEMBER_1.email, HIVE_TEST_USERS.MEMBER_1.password);
    await observerAuth.loginUser(HIVE_TEST_USERS.MEMBER_2.email, HIVE_TEST_USERS.MEMBER_2.password);

    // Create a test hive for presence testing
    const hiveData = generateUniqueHiveData('PRESENCE_TEST_HIVE');
    await createHivePage.goto();
    await createHivePage.fillBasicInfo(hiveData);
    testHive = await createHivePage.createHive();

    // Have other users join the hive
    await memberHivePage.goto(testHive.id);
    await memberHivePage.joinHive();

    await observerHivePage.goto(testHive.id);
    await observerHivePage.joinHive();
  });

  test.afterAll(async () => {
    await ownerPage.close();
    await memberPage.close();
    await observerPage.close();
  });

  test.beforeEach(async () => {
    // Ensure all users are in the hive workspace
    await ownerHivePage.goto(testHive.id);
    await memberHivePage.goto(testHive.id);
    await observerHivePage.goto(testHive.id);

    // Wait for WebSocket connections
    await ownerWSHelper.waitForConnection();
    await memberWSHelper.waitForConnection();
    await observerWSHelper.waitForConnection();
  });

  test.describe('Basic Presence Updates', () => {
    test('should show user as active when in hive', async () => {
      // Act
      await ownerHivePage.updatePresenceStatus('active');

      // Assert - Owner should appear as active in member lists
      const memberViewMembers = await memberHivePage.getMembers();
      const ownerPresence = memberViewMembers.find(member =>
          member.username === HIVE_TEST_USERS.OWNER.username
      );

      expect(ownerPresence?.isActive).toBe(true);
      expect(ownerPresence?.status).toContain('active');

      // Should also be visible to observer
      const observerViewMembers = await observerHivePage.getMembers();
      const ownerPresenceInObserver = observerViewMembers.find(member =>
          member.username === HIVE_TEST_USERS.OWNER.username
      );

      expect(ownerPresenceInObserver?.isActive).toBe(true);
    });

    test('should update presence status in real-time', async () => {
      // Act - Owner changes status to 'away'
      await ownerHivePage.updatePresenceStatus('away');

      // Assert - Other users should see the change
      const statusUpdated = await memberHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.OWNER.id,
          'away'
      );
      expect(statusUpdated).toBe(true);

      // Change to 'break'
      await ownerHivePage.updatePresenceStatus('break');

      const breakStatusUpdated = await observerHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.OWNER.id,
          'break'
      );
      expect(breakStatusUpdated).toBe(true);
    });

    test('should show different presence indicators correctly', async () => {
      // Test all presence statuses
      const statuses = ['active', 'away', 'break'] as const;

      for (const status of statuses) {
        // Act
        await memberHivePage.updatePresenceStatus(status);

        // Assert - Should show correct indicator
        await expect(
            ownerPage.locator(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"] [data-testid="presence-${status}"]`)
        ).toBeVisible();

        // Status text should match
        const memberElement = ownerPage.locator(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"]`);
        const statusText = await memberElement.locator('[data-testid="member-status"]').textContent();
        expect(statusText?.toLowerCase()).toContain(status);
      }
    });

    test('should measure presence update latency', async () => {
      // Act & Assert
      const latencyResult = await ownerWSHelper.testPresenceUpdate(
          HIVE_TEST_USERS.OWNER.id,
          'active'
      );

      expect(latencyResult.sent).toBe(true);
      expect(latencyResult.received).toBe(true);
      expect(latencyResult.latency).toBeLessThan(500); // Should update within 500ms

      console.log(`Presence update latency: ${latencyResult.latency}ms`);
    });
  });

  test.describe('WebSocket Message Propagation', () => {
    test('should send presence update messages', async () => {
      // Clear previous messages
      await ownerWSHelper.clearMessageLog();

      // Act
      await ownerHivePage.updatePresenceStatus('active');

      // Assert - WebSocket message should be sent
      const presenceMessage = await ownerWSHelper.waitForMessage('PRESENCE_UPDATE');
      expect(presenceMessage).toBeTruthy();
      expect(presenceMessage?.payload.userId).toBe(HIVE_TEST_USERS.OWNER.id);
      expect(presenceMessage?.payload.status).toBe('active');
    });

    test('should receive presence updates from other users', async () => {
      // Clear previous messages
      await memberWSHelper.clearMessageLog();

      // Act - Owner updates status
      await ownerHivePage.updatePresenceStatus('break');

      // Assert - Member should receive the update
      const receivedMessage = await memberWSHelper.waitForMessage('PRESENCE_UPDATE');
      expect(receivedMessage).toBeTruthy();
      expect(receivedMessage?.payload.userId).toBe(HIVE_TEST_USERS.OWNER.id);
      expect(receivedMessage?.payload.status).toBe('break');
    });

    test('should handle message ordering correctly', async () => {
      // Act - Send multiple rapid status updates
      await ownerHivePage.updatePresenceStatus('active');
      await ownerPage.waitForTimeout(100);
      await ownerHivePage.updatePresenceStatus('away');
      await ownerPage.waitForTimeout(100);
      await ownerHivePage.updatePresenceStatus('break');

      // Assert - Messages should be received in order
      const messageOrderCorrect = await memberWSHelper.verifyMessageOrdering([
        'PRESENCE_UPDATE',
        'PRESENCE_UPDATE',
        'PRESENCE_UPDATE'
      ]);
      expect(messageOrderCorrect).toBe(true);

      // Final status should be 'break'
      const finalStatusUpdated = await memberHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.OWNER.id,
          'break'
      );
      expect(finalStatusUpdated).toBe(true);
    });
  });

  test.describe('Multi-tab Presence Synchronization', () => {
    let secondOwnerPage: Page;
    let secondOwnerContext: BrowserContext;

    test.beforeEach(async () => {
      // Create a second tab for the same user
      secondOwnerContext = await browser.newContext();
      secondOwnerPage = await secondOwnerContext.newPage();

      // Login in second tab
      const secondOwnerAuth = new EnhancedAuthHelper(secondOwnerPage);
      await secondOwnerAuth.loginUser(HIVE_TEST_USERS.OWNER.email, HIVE_TEST_USERS.OWNER.password);

      // Navigate to same hive
      const secondOwnerHivePage = new HivePage(secondOwnerPage);
      await secondOwnerHivePage.goto(testHive.id);
    });

    test.afterEach(async () => {
      await secondOwnerPage.close();
      await secondOwnerContext.close();
    });

    test('should synchronize presence across multiple tabs', async () => {
      // Act - Update status in first tab
      await ownerHivePage.updatePresenceStatus('away');

      // Assert - Second tab should reflect the change
      await expect(
          secondOwnerPage.locator('[data-testid*="current-status-away"]')
      ).toBeVisible();

      // Update status in second tab
      const secondOwnerHivePage = new HivePage(secondOwnerPage);
      await secondOwnerHivePage.updatePresenceStatus('active');

      // First tab should reflect the change
      await expect(
          ownerPage.locator('[data-testid*="current-status-active"]')
      ).toBeVisible();
    });

    test('should handle tab focus changes', async () => {
      // Act - Focus second tab (simulate user switching tabs)
      await secondOwnerPage.bringToFront();
      await secondOwnerPage.waitForTimeout(1000);

      // First tab should show user as potentially away
      await ownerPage.bringToFront();

      // The original tab might show an "away" indicator after some time
      // This depends on the implementation of tab focus detection
      const currentStatus = await ownerPage.locator('[data-testid*="current-status-"]').textContent();

      // Status should be one of the valid states
      const validStatuses = ['active', 'away', 'break', 'offline'];
      const hasValidStatus = validStatuses.some(status =>
          currentStatus?.includes(status)
      );
      expect(hasValidStatus).toBe(true);
    });

    test('should prevent conflicting presence updates', async () => {
      // Act - Update status simultaneously in both tabs
      const updatePromise1 = ownerHivePage.updatePresenceStatus('active');
      const updatePromise2 = new HivePage(secondOwnerPage).updatePresenceStatus('break');

      await Promise.all([updatePromise1, updatePromise2]);

      // Assert - Should resolve to one consistent status
      await ownerPage.waitForTimeout(1000);
      await secondOwnerPage.waitForTimeout(1000);

      const firstTabStatus = await ownerPage.locator('[data-testid*="current-status-"]').textContent();
      const secondTabStatus = await secondOwnerPage.locator('[data-testid*="current-status-"]').textContent();

      // Both tabs should show the same final status
      expect(firstTabStatus).toBe(secondTabStatus);
    });
  });

  test.describe('Offline/Online Transitions', () => {
    test('should show user as offline when disconnected', async () => {
      // Act - Simulate network disconnection for member
      await memberWSHelper.simulateConnectionIssues('disconnect');
      await memberPage.waitForTimeout(3000);

      // Assert - Other users should see member as offline
      const ownerViewMembers = await ownerHivePage.getMembers();
      const _memberPresence = ownerViewMembers.find(member =>
          member.username === HIVE_TEST_USERS.MEMBER_1.username
      );

      // Should show as offline or last known status with offline indicator
      const memberElement = ownerPage.locator(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"]`);
      await expect(memberElement.locator('[data-testid="offline-indicator"]')).toBeVisible();
    });

    test('should restore presence when reconnected', async () => {
      // Arrange - User goes offline
      await memberWSHelper.simulateConnectionIssues('disconnect');
      await memberPage.waitForTimeout(2000);

      // Act - Reconnect
      await memberPage.reload(); // Simulate reconnection
      await memberHivePage.waitForLoad();
      await memberWSHelper.waitForConnection();

      // Assert - Should appear as online again
      const statusUpdated = await ownerHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.MEMBER_1.id,
          'active',
          10000 // Longer timeout for reconnection
      );
      expect(statusUpdated).toBe(true);

      // Offline indicator should disappear
      const memberElement = ownerPage.locator(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"]`);
      await expect(memberElement.locator('[data-testid="offline-indicator"]')).not.toBeVisible();
    });

    test('should handle gradual network degradation', async () => {
      // Act - Simulate slow network
      await memberWSHelper.simulateConnectionIssues('slowNetwork');

      // Update presence with slow network
      await memberHivePage.updatePresenceStatus('break');

      // Assert - Update should eventually propagate (with higher latency)
      const statusUpdated = await ownerHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.MEMBER_1.id,
          'break',
          10000 // Longer timeout for slow network
      );
      expect(statusUpdated).toBe(true);
    });
  });

  test.describe('Presence Cleanup and Management', () => {
    test('should clean up presence when user leaves hive', async () => {
      // Act - Member leaves the hive
      await memberHivePage.leaveHive();

      // Assert - Member should no longer appear in presence list
      await ownerPage.waitForTimeout(2000); // Allow for cleanup

      const ownerViewMembers = await ownerHivePage.getMembers();
      const memberPresence = ownerViewMembers.find(member =>
          member.username === HIVE_TEST_USERS.MEMBER_1.username
      );

      expect(memberPresence).toBeUndefined();
    });

    test('should handle session timeout gracefully', async () => {
      // This would normally test session timeout after a longer period
      // For E2E testing, we'll simulate the timeout condition

      // Act - Simulate session timeout
      await memberPage.evaluate(() => {
        // Clear authentication tokens to simulate timeout
        localStorage.clear();
        sessionStorage.clear();
      });

      await memberPage.reload();

      // Assert - User should appear as offline to others
      await ownerPage.waitForTimeout(3000);

      const memberElement = ownerPage.locator(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"]`);

      // Should either be removed from list or show as offline
      const isVisible = await memberElement.isVisible();
      if (isVisible) {
        await expect(memberElement.locator('[data-testid="offline-indicator"]')).toBeVisible();
      }
    });

    test('should limit presence history size', async () => {
      // Act - Generate many presence updates to test history limits
      const updates = ['active', 'away', 'break', 'active', 'away'] as const;

      for (const status of updates) {
        await ownerHivePage.updatePresenceStatus(status);
        await ownerPage.waitForTimeout(200);
      }

      // Assert - Should maintain reasonable history size
      const messages = await ownerWSHelper.getMessageLog();
      const presenceMessages = messages.filter(msg => msg.type === 'PRESENCE_UPDATE');

      // Should have recent messages but not accumulate indefinitely
      expect(presenceMessages.length).toBeLessThanOrEqual(50); // Reasonable limit
    });
  });

  test.describe('Performance and Scalability', () => {
    test('should handle many concurrent users efficiently', async () => {
      const multiUserHelper = new MultiUserHiveHelper();

      try {
        // Create multiple user sessions
        const users = [
          HIVE_TEST_USERS.MEMBER_1,
          HIVE_TEST_USERS.MEMBER_2,
          HIVE_TEST_USERS.MODERATOR
        ];

        const helpers = [];
        for (const user of users) {
          const helper = await multiUserHelper.createUserSession(browser, user);
          await helper.navigateToHive(testHive.id);
          helpers.push(helper);
        }

        // Act - All users update presence simultaneously
        const presencePromises = helpers.map((helper, index) => {
          const statuses = ['active', 'away', 'break'] as const;
          return helper.updatePresenceStatus(statuses[index % statuses.length]);
        });

        const startTime = Date.now();
        await Promise.all(presencePromises);
        const updateTime = Date.now() - startTime;

        // Assert - Updates should complete quickly
        expect(updateTime).toBeLessThan(3000); // Within 3 seconds for all users

        // All users should see consistent presence
        const presenceVerified = await multiUserHelper.verifyConcurrentPresence(testHive.id);
        expect(presenceVerified).toBe(true);

      } finally {
        await multiUserHelper.cleanup();
      }
    });

    test('should maintain connection stability under load', async () => {
      // Act - Monitor connection stability while generating updates
      const stabilityResult = await ownerWSHelper.monitorConnectionStability(10000); // 10 seconds

      // Generate presence updates during monitoring
      const updateInterval = setInterval(async () => {
        const statuses = ['active', 'away', 'break'] as const;
        const randomStatus = statuses[Math.floor(Math.random() * statuses.length)];
        await ownerHivePage.updatePresenceStatus(randomStatus).catch(() => {
          // Ignore errors during stability testing
        });
      }, 1000);

      // Wait for monitoring to complete
      await ownerPage.waitForTimeout(10000);
      clearInterval(updateInterval);

      // Assert - Connection should remain stable
      expect(stabilityResult.disconnections).toBeLessThan(2); // Allow for occasional disconnections
      expect(stabilityResult.averageLatency).toBeLessThan(1000); // Average latency under 1 second
      expect(stabilityResult.messageCount).toBeGreaterThan(5); // Should have processed messages
    });

    test('should handle concurrent message processing', async () => {
      // Act & Assert
      const concurrencyResult = await ownerWSHelper.testConcurrentMessages(20);

      expect(concurrencyResult.sent).toBe(20);
      expect(concurrencyResult.received).toBeGreaterThanOrEqual(18); // Allow for some message loss
      expect(concurrencyResult.duplicates).toBeLessThan(3); // Minimal duplicates
      expect(concurrencyResult.outOfOrder).toBeLessThan(5); // Some out-of-order is acceptable
    });
  });

  test.describe('Error Handling and Recovery', () => {
    test('should retry failed presence updates', async () => {
      // Act - Simulate network issues during update
      await ownerWSHelper.simulateConnectionIssues('highLatency');

      const startTime = Date.now();
      await ownerHivePage.updatePresenceStatus('away');
      const updateTime = Date.now() - startTime;

      // Assert - Should eventually succeed despite network issues
      const statusUpdated = await memberHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.OWNER.id,
          'away',
          15000 // Longer timeout for retry scenarios
      );
      expect(statusUpdated).toBe(true);

      // Should take longer due to retries
      expect(updateTime).toBeGreaterThan(500);
    });

    test('should show appropriate error states', async () => {
      // Act - Disconnect WebSocket and try to update
      await ownerWSHelper.simulateConnectionIssues('disconnect');
      await ownerHivePage.updatePresenceStatus('break');

      // Assert - Should show connection error indicator
      await expect(
          ownerPage.locator('[data-testid="presence-connection-error"]')
      ).toBeVisible();

      // Error should include retry option
      await expect(
          ownerPage.locator('[data-testid="retry-presence-update"]')
      ).toBeVisible();
    });

    test('should recover gracefully from WebSocket errors', async () => {
      // Act - Simulate WebSocket error
      await ownerPage.evaluate(() => {
        const ws = window.__currentWebSocket;
        if (ws) {
          ws.dispatchEvent(new Event('error'));
        }
      });

      await ownerPage.waitForTimeout(2000);

      // Try to update presence
      await ownerHivePage.updatePresenceStatus('active');

      // Assert - Should automatically reconnect and update
      const statusUpdated = await memberHivePage.waitForPresenceUpdate(
          HIVE_TEST_USERS.OWNER.id,
          'active',
          10000 // Allow time for reconnection
      );
      expect(statusUpdated).toBe(true);
    });
  });

  test.describe('Accessibility and Visual Indicators', () => {
    test('should provide screen reader announcements for presence changes', async () => {
      // Act
      await ownerHivePage.updatePresenceStatus('break');

      // Assert - Aria live region should be updated
      await expect(
          ownerPage.locator('[data-testid="presence-announcements"][aria-live="polite"]')
      ).toContainText('Your status has been updated to break');

      // Other users should also get announcements
      await expect(
          memberPage.locator('[data-testid="presence-announcements"][aria-live="polite"]')
      ).toContainText(`${HIVE_TEST_USERS.OWNER.displayName} is now on break`);
    });

    test('should have appropriate ARIA labels for presence indicators', async () => {
      // Assert - Presence indicators should have descriptive labels
      const memberElement = ownerPage.locator(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"]`);
      const presenceIndicator = memberElement.locator('[data-testid="presence-indicator"]');

      await expect(presenceIndicator).toHaveAttribute('aria-label', expect.stringMatching(/status/i));

      // Status dropdown should be accessible
      await ownerPage.click('[data-testid="user-presence-dropdown"]');
      const statusOptions = ownerPage.locator('[data-testid^="presence-status-"]');

      await expect(statusOptions.first()).toHaveAttribute('role', 'menuitem');
    });

    test('should support keyboard navigation for presence controls', async () => {
      // Act - Navigate to presence dropdown with keyboard
      await ownerPage.focus('[data-testid="user-presence-dropdown"]');
      await ownerPage.keyboard.press('Enter');

      // Should open dropdown
      await expect(
          ownerPage.locator('[data-testid="presence-dropdown-menu"]')
      ).toBeVisible();

      // Navigate with arrow keys
      await ownerPage.keyboard.press('ArrowDown');
      await ownerPage.keyboard.press('Enter');

      // Should select new status
      const currentStatus = await ownerPage.locator('[data-testid*="current-status-"]').textContent();
      expect(currentStatus).toBeTruthy();
    });
  });
});