/**
 * User Workflow Edge Case E2E Tests for FocusHive
 * Comprehensive testing of complex user workflows and edge case scenarios
 * 
 * Test Coverage:
 * - Multiple tabs/windows scenarios
 * - Browser back/forward navigation edge cases
 * - Page refresh during critical operations
 * - Form submission timeouts and retries
 * - Double-click/submit prevention
 * - Race conditions in user interactions
 * - Orphaned transactions and cleanup
 * - Incomplete multi-step processes
 * - Context switching between hives
 * - Session boundary edge cases
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

test.describe('User Workflow Edge Cases', () => {
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
    
    // Set up workflow monitoring
    await errorHelper.setupWorkflowMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
  });

  test.describe('Multiple Tabs and Windows', () => {
    test('should handle same user in multiple tabs gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Tab 1: Dashboard
      await page.goto('/dashboard');
      await expect(page.locator('[data-testid="dashboard-loaded"]')).toBeVisible();
      
      // Tab 2: Same hive
      const tab2 = await context.newPage();
      await tab2.goto('/hive/123');
      await expect(tab2.locator('[data-testid="hive-loaded"]')).toBeVisible();
      
      // Tab 3: Different hive
      const tab3 = await context.newPage();
      await tab3.goto('/hive/456');
      await expect(tab3.locator('[data-testid="hive-loaded"]')).toBeVisible();
      
      // Start timer in Tab 2
      await tab2.click('[data-testid="start-timer"]');
      await expect(tab2.locator('[data-testid="timer-running"]')).toBeVisible();
      
      // Tab 1 should show global timer notification
      await expect(page.locator('[data-testid="active-timer-notification"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Tab 3 should detect timer conflict if trying to start another
      await tab3.click('[data-testid="start-timer"]');
      await expect(tab3.locator('[data-testid="timer-conflict-warning"]')).toBeVisible();
      await expect(tab3.locator('[data-testid="timer-conflict-message"]')).toContainText(/timer.*running.*another/i);
      
      // Should offer conflict resolution
      await expect(tab3.locator('[data-testid="stop-other-timer"]')).toBeVisible();
      await expect(tab3.locator('[data-testid="view-active-timer"]')).toBeVisible();
      
      // Test cross-tab synchronization
      await tab2.click('[data-testid="pause-timer"]');
      
      // Tab 1 notification should update
      await expect(page.locator('[data-testid="active-timer-notification"]')).toContainText(/paused/i);
      
      // Tab 3 conflict should resolve
      await expect(tab3.locator('[data-testid="timer-conflict-warning"]')).not.toBeVisible();
      
      await tab2.close();
      await tab3.close();
    });

    test('should handle concurrent hive joining/leaving across tabs', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Open same hive in multiple tabs
      const tab1 = page;
      await tab1.goto('/hive/123');
      
      const tab2 = await context.newPage();
      await tab2.goto('/hive/123');
      
      // Both tabs should load successfully
      await expect(tab1.locator('[data-testid="hive-loaded"]')).toBeVisible();
      await expect(tab2.locator('[data-testid="hive-loaded"]')).toBeVisible();
      
      // Join hive in Tab 1
      await tab1.click('[data-testid="join-hive"]');
      await expect(tab1.locator('[data-testid="join-success"]')).toBeVisible();
      
      // Tab 2 should automatically update membership status
      await expect(tab2.locator('[data-testid="member-status"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      await expect(tab2.locator('[data-testid="leave-hive"]')).toBeVisible();
      
      // Attempt to leave from Tab 2 while performing actions in Tab 1
      const leavePromise = tab2.click('[data-testid="leave-hive"]');
      const chatPromise = tab1.fill('[data-testid="chat-input"]', 'Test message during leave');
      
      await Promise.all([leavePromise, chatPromise]);
      
      // Should handle concurrent operations gracefully
      await expect(tab2.locator('[data-testid="leave-success"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Tab 1 should update to reflect membership change
      await expect(tab1.locator('[data-testid="no-longer-member"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      await expect(tab1.locator('[data-testid="chat-input"]')).toBeDisabled();
      
      await tab2.close();
    });

    test('should handle window closing during active operations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Start critical operation
      await page.goto('/hive/create');
      await page.fill('[data-testid="hive-name"]', 'Important Hive');
      await page.fill('[data-testid="hive-description"]', 'Critical data that must not be lost');
      
      // Open another tab to continue session
      const tab2 = await context.newPage();
      await tab2.goto('/dashboard');
      
      // Simulate first tab closing unexpectedly (browser crash, etc.)
      await page.close();
      
      // Continue in Tab 2 and go to create hive
      await tab2.goto('/hive/create');
      
      // Should detect and recover unsaved draft
      await expect(tab2.locator('[data-testid="draft-recovery-dialog"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show draft content
      const draftPreview = tab2.locator('[data-testid="draft-preview"]');
      await expect(draftPreview).toContainText('Important Hive');
      await expect(draftPreview).toContainText('Critical data that must not be lost');
      
      // Should offer recovery options
      await expect(tab2.locator('[data-testid="restore-draft"]')).toBeVisible();
      await expect(tab2.locator('[data-testid="discard-draft"]')).toBeVisible();
      
      // Test draft restoration
      await tab2.click('[data-testid="restore-draft"]');
      
      await expect(tab2.locator('[data-testid="hive-name"]')).toHaveValue('Important Hive');
      await expect(tab2.locator('[data-testid="hive-description"]')).toHaveValue('Critical data that must not be lost');
      
      page = tab2; // Update page reference
    });
  });

  test.describe('Navigation Edge Cases', () => {
    test('should handle browser back/forward during async operations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Start on dashboard
      await page.goto('/dashboard');
      
      // Navigate to hive creation
      await page.goto('/hive/create');
      await page.fill('[data-testid="hive-name"]', 'Navigation Test Hive');
      
      // Start async operation (form submission with delay)
      await context.route('**/api/hives', async (route) => {
        // Add delay to simulate slow submission
        await new Promise(resolve => setTimeout(resolve, 3000));
        await route.continue();
      });
      
      const submitPromise = page.click('[data-testid="submit-hive"]');
      
      // Navigate back while submission is in progress
      await page.waitForTimeout(1000);
      await page.goBack();
      
      // Should handle back navigation during pending operation
      await expect(page.locator('[data-testid="operation-in-progress"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show pending operation status
      await expect(page.locator('[data-testid="pending-hive-creation"]')).toBeVisible();
      
      // Should offer options to continue or cancel
      await expect(page.locator('[data-testid="continue-operation"]')).toBeVisible();
      await expect(page.locator('[data-testid="cancel-operation"]')).toBeVisible();
      
      // Test continuing operation
      await page.click('[data-testid="continue-operation"]');
      
      // Should return to form and complete submission
      await expect(page.url()).toContain('/hive/create');
      
      // Wait for original submission to complete
      await submitPromise;
      
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({ timeout: TIMEOUTS.LONG });
    });

    test('should handle rapid navigation changes', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Perform rapid navigation
      const routes = ['/dashboard', '/timer', '/hive/123', '/profile', '/settings', '/dashboard'];
      
      for (const route of routes) {
        await page.goto(route, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(200); // Very short delay
      }
      
      // Should handle rapid navigation without breaking
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should not show loading states stuck
      await expect(page.locator('[data-testid="loading-spinner"]')).not.toBeVisible();
      
      // Should have consistent navigation state
      const currentUrl = page.url();
      expect(currentUrl).toContain('/dashboard');
      
      // Should not have memory leaks or duplicate event listeners
      const listenerCount = await page.evaluate(() => window.__EVENT_LISTENERS_COUNT__ || 0);
      expect(listenerCount).toBeLessThan(50); // Reasonable limit
    });

    test('should handle deep linking to protected resources', async () => {
      // Try to access protected route without authentication
      await page.goto('/hive/123/settings');
      
      // Should redirect to login with return URL
      await expect(page.url()).toContain('/login');
      const returnUrl = new URL(page.url()).searchParams.get('returnUrl');
      expect(returnUrl).toBe('/hive/123/settings');
      
      // Login
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Should redirect back to original URL
      await expect(page.url()).toContain('/hive/123/settings');
      await expect(page.locator('[data-testid="hive-settings"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Test with non-existent resource
      await page.goto('/hive/nonexistent/settings');
      
      // Should handle gracefully
      await expect(page.locator('[data-testid="resource-not-found"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should offer navigation options
      await expect(page.locator('[data-testid="go-to-dashboard"]')).toBeVisible();
      await expect(page.locator('[data-testid="search-hives"]')).toBeVisible();
      
      // Test with insufficient permissions
      await page.goto('/admin/users');
      
      // Should show permission denied
      await expect(page.locator('[data-testid="permission-denied"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should not expose admin interface
      await expect(page.locator('[data-testid="admin-panel"]')).not.toBeVisible();
    });
  });

  test.describe('Form Submission Edge Cases', () => {
    test('should prevent double submission and handle duplicate requests', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');
      
      // Fill form
      await page.fill('[data-testid="hive-name"]', 'Double Submit Test');
      await page.fill('[data-testid="hive-description"]', 'Testing double submission prevention');
      
      // Add delay to API to allow double-click
      await context.route('**/api/hives', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 2000));
        await route.continue();
      });
      
      // Rapidly double-click submit button
      await page.click('[data-testid="submit-hive"]');
      await page.click('[data-testid="submit-hive"]');
      await page.click('[data-testid="submit-hive"]');
      
      // Should disable button after first click
      await expect(page.locator('[data-testid="submit-hive"]')).toBeDisabled();
      
      // Should show submission in progress
      await expect(page.locator('[data-testid="submission-in-progress"]')).toBeVisible();
      
      // Should only create one hive
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({ timeout: TIMEOUTS.LONG });
      
      // Verify only one hive was created
      await page.goto('/dashboard');
      const duplicateHives = page.locator('[data-testid="hive-card"]:has-text("Double Submit Test")');
      await expect(duplicateHives).toHaveCount(1);
    });

    test('should handle form submission timeouts gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');
      
      // Mock timeout scenario
      await context.route('**/api/hives', async (route) => {
        // Never resolve - simulate timeout
        await new Promise(() => {}); // Infinite promise
      });
      
      await page.fill('[data-testid="hive-name"]', 'Timeout Test Hive');
      await page.click('[data-testid="submit-hive"]');
      
      // Should show timeout after reasonable delay
      await expect(page.locator('[data-testid="submission-timeout"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should offer retry and cancel options
      await expect(page.locator('[data-testid="retry-submission"]')).toBeVisible();
      await expect(page.locator('[data-testid="cancel-submission"]')).toBeVisible();
      
      // Should preserve form data
      await expect(page.locator('[data-testid="hive-name"]')).toHaveValue('Timeout Test Hive');
      
      // Test retry with successful response
      await context.unroute('**/api/hives');
      await page.click('[data-testid="retry-submission"]');
      
      // Should succeed on retry
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });

    test('should handle partial form completion and auto-save', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');
      
      // Fill form partially
      await page.fill('[data-testid="hive-name"]', 'Auto-save Test');
      await page.waitForTimeout(1000); // Allow auto-save to trigger
      
      // Navigate away without submitting
      await page.goto('/dashboard');
      
      // Return to form
      await page.goto('/hive/create');
      
      // Should offer to restore draft
      await expect(page.locator('[data-testid="restore-draft-dialog"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      await page.click('[data-testid="restore-draft"]');
      
      // Should restore form data
      await expect(page.locator('[data-testid="hive-name"]')).toHaveValue('Auto-save Test');
      
      // Continue filling form
      await page.fill('[data-testid="hive-description"]', 'Restored from auto-save');
      
      // Should be able to submit normally
      await page.click('[data-testid="submit-hive"]');
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });

    test('should validate form dependencies and conditional fields', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');
      
      // Test conditional field validation
      await page.selectOption('[data-testid="hive-type"]', 'private');
      
      // Private hive should require password
      await expect(page.locator('[data-testid="hive-password"]')).toBeVisible();
      await expect(page.locator('[data-testid="hive-password"]')).toHaveAttribute('required');
      
      // Try to submit without password
      await page.fill('[data-testid="hive-name"]', 'Private Hive Test');
      await page.click('[data-testid="submit-hive"]');
      
      // Should show password requirement
      await expect(page.locator('[data-testid="password-required"]')).toBeVisible();
      
      // Change to public hive
      await page.selectOption('[data-testid="hive-type"]', 'public');
      
      // Password field should be hidden and not required
      await expect(page.locator('[data-testid="hive-password"]')).toBeHidden();
      
      // Should be able to submit without password
      await page.click('[data-testid="submit-hive"]');
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });
  });

  test.describe('Race Conditions and Concurrent Operations', () => {
    test('should handle concurrent timer operations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');
      
      // Simulate rapid timer operations
      const operations = [
        () => page.click('[data-testid="start-timer"]'),
        () => page.click('[data-testid="pause-timer"]'),
        () => page.click('[data-testid="resume-timer"]'),
        () => page.click('[data-testid="stop-timer"]')
      ];
      
      // Execute operations rapidly
      for (const operation of operations) {
        try {
          await operation();
          await page.waitForTimeout(100); // Short delay
        } catch (error) {
          // Some operations may fail due to invalid state - this is expected
        }
      }
      
      // Should resolve to valid final state
      await expect(page.locator('[data-testid="timer-state-resolved"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Timer should be in a valid state
      const timerState = await page.locator('[data-testid="timer-display"]').getAttribute('data-state');
      expect(['stopped', 'running', 'paused']).toContain(timerState);
      
      // UI should be consistent with state
      if (timerState === 'running') {
        await expect(page.locator('[data-testid="pause-timer"]')).toBeVisible();
        await expect(page.locator('[data-testid="start-timer"]')).not.toBeVisible();
      }
    });

    test('should handle concurrent chat message sending', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Send multiple messages rapidly
      const messages = ['Message 1', 'Message 2', 'Message 3', 'Message 4', 'Message 5'];
      
      const sendPromises = messages.map(async (message, index) => {
        await page.fill('[data-testid="chat-input"]', message);
        await page.press('[data-testid="chat-input"]', 'Enter');
        await page.waitForTimeout(50 * index); // Staggered timing
      });
      
      await Promise.all(sendPromises);
      
      // All messages should eventually appear in order
      for (let i = 0; i < messages.length; i++) {
        await expect(page.locator(`[data-testid="chat-message"]:has-text("${messages[i]}")`)).toBeVisible({ 
          timeout: TIMEOUTS.MEDIUM 
        });
      }
      
      // Messages should be in correct order
      const chatMessages = await page.locator('[data-testid="chat-message"]').allTextContents();
      const messageOrder = messages.map(msg => chatMessages.findIndex(chat => chat.includes(msg)));
      
      for (let i = 1; i < messageOrder.length; i++) {
        expect(messageOrder[i]).toBeGreaterThan(messageOrder[i - 1]);
      }
    });

    test('should handle concurrent hive membership changes', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Simulate rapid join/leave operations
      const membershipOperations = [
        () => page.click('[data-testid="join-hive"]'),
        () => page.click('[data-testid="leave-hive"]'),
        () => page.click('[data-testid="join-hive"]'),
        () => page.click('[data-testid="leave-hive"]')
      ];
      
      for (const operation of membershipOperations) {
        try {
          await operation();
          await page.waitForTimeout(200);
        } catch (error) {
          // Operations may fail due to invalid state transitions
        }
      }
      
      // Should resolve to consistent membership state
      await expect(page.locator('[data-testid="membership-state-resolved"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show either join or leave button, not both
      const joinVisible = await page.locator('[data-testid="join-hive"]').isVisible();
      const leaveVisible = await page.locator('[data-testid="leave-hive"]').isVisible();
      
      expect(joinVisible && leaveVisible).toBeFalsy(); // Not both visible
      expect(joinVisible || leaveVisible).toBeTruthy(); // At least one visible
      
      // UI should be consistent with membership state
      if (joinVisible) {
        await expect(page.locator('[data-testid="chat-input"]')).toBeDisabled();
      } else {
        await expect(page.locator('[data-testid="chat-input"]')).toBeEnabled();
      }
    });
  });

  test.describe('Context Switching and Multi-Hive Scenarios', () => {
    test('should handle switching between multiple active hives', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Join multiple hives
      const hives = ['123', '456', '789'];
      
      for (const hiveId of hives) {
        await page.goto(`/hive/${hiveId}`);
        await page.click('[data-testid="join-hive"]');
        await expect(page.locator('[data-testid="join-success"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      }
      
      // Start timer in first hive
      await page.goto('/hive/123');
      await page.click('[data-testid="start-timer"]');
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible();
      
      // Switch to second hive
      await page.goto('/hive/456');
      
      // Should show active timer from other hive
      await expect(page.locator('[data-testid="active-timer-elsewhere"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should prevent starting conflicting timer
      await page.click('[data-testid="start-timer"]');
      await expect(page.locator('[data-testid="timer-conflict"]')).toBeVisible();
      
      // Should offer context switching options
      await expect(page.locator('[data-testid="switch-to-active-timer"]')).toBeVisible();
      await expect(page.locator('[data-testid="stop-other-timer"]')).toBeVisible();
      
      // Test context switching
      await page.click('[data-testid="switch-to-active-timer"]');
      
      // Should navigate to hive with active timer
      await expect(page.url()).toContain('/hive/123');
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible();
      
      // Test stopping timer from another hive
      await page.goto('/hive/789');
      await expect(page.locator('[data-testid="active-timer-elsewhere"]')).toBeVisible();
      
      await page.click('[data-testid="stop-other-timer"]');
      
      // Should stop timer and update all contexts
      await expect(page.locator('[data-testid="timer-stopped-remotely"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should now be able to start timer in current hive
      await page.click('[data-testid="start-timer"]');
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible();
    });

    test('should maintain context when navigating between hive sections', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Join hive and establish context
      await page.click('[data-testid="join-hive"]');
      await expect(page.locator('[data-testid="join-success"]')).toBeVisible();
      
      // Set user status
      await page.fill('[data-testid="status-input"]', 'Working on important task');
      await page.click('[data-testid="update-status"]');
      
      // Navigate to different sections
      await page.click('[data-testid="hive-chat-tab"]');
      await expect(page.locator('[data-testid="chat-section"]')).toBeVisible();
      
      await page.click('[data-testid="hive-timer-tab"]');
      await expect(page.locator('[data-testid="timer-section"]')).toBeVisible();
      
      await page.click('[data-testid="hive-participants-tab"]');
      await expect(page.locator('[data-testid="participants-section"]')).toBeVisible();
      
      // Should maintain user status across all sections
      await expect(page.locator('[data-testid="user-status"]')).toContainText('Working on important task');
      
      // Should maintain hive membership state
      await expect(page.locator('[data-testid="member-badge"]')).toBeVisible();
      
      // Should maintain real-time connection
      await expect(page.locator('[data-testid="connection-status"]')).toHaveClass(/connected/);
      
      // Test deep linking to specific section
      await page.goto('/hive/123#timer');
      
      // Should maintain context and navigate to correct section
      await expect(page.locator('[data-testid="timer-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="user-status"]')).toContainText('Working on important task');
      await expect(page.locator('[data-testid="member-badge"]')).toBeVisible();
    });

    test('should handle hive deletion while user is active', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Join hive and start activities
      await page.click('[data-testid="join-hive"]');
      await page.click('[data-testid="start-timer"]');
      
      // Start typing in chat
      await page.fill('[data-testid="chat-input"]', 'Test message before deletion');
      
      // Simulate hive deletion (admin action from another session)
      await context.route('**/api/hives/123', async (route) => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Not Found',
            message: 'Hive has been deleted',
            code: 'HIVE_DELETED'
          })
        });
      });
      
      // Try to send message (triggers API call)
      await page.press('[data-testid="chat-input"]', 'Enter');
      
      // Should detect hive deletion
      await expect(page.locator('[data-testid="hive-deleted-notification"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should stop timer automatically
      await expect(page.locator('[data-testid="timer-stopped-auto"]')).toBeVisible();
      
      // Should preserve draft message
      await expect(page.locator('[data-testid="message-draft-saved"]')).toBeVisible();
      
      // Should offer navigation options
      await expect(page.locator('[data-testid="find-similar-hives"]')).toBeVisible();
      await expect(page.locator('[data-testid="create-new-hive"]')).toBeVisible();
      await expect(page.locator('[data-testid="return-dashboard"]')).toBeVisible();
      
      // Should clean up resources and state
      await page.click('[data-testid="return-dashboard"]');
      
      // Should not show deleted hive in dashboard
      await expect(page.locator('[data-testid="hive-card"][data-hive-id="123"]')).not.toBeVisible();
      
      // Should not have memory leaks or stuck timers
      const activeTimers = await page.evaluate(() => window.__ACTIVE_TIMERS__ || 0);
      expect(activeTimers).toBe(0);
    });
  });

  test.describe('Session Boundary Edge Cases', () => {
    test('should handle operations spanning session boundaries', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');
      
      // Start long-running timer
      await page.fill('[data-testid="timer-duration"]', '120'); // 2 hours
      await page.click('[data-testid="start-timer"]');
      
      // Simulate session approaching expiration (mock server time advance)
      await page.evaluate(() => {
        // Mock token expiration warning
        localStorage.setItem('token_expires_at', String(Date.now() + 300000)); // 5 minutes
      });
      
      // Should show session expiration warning
      await expect(page.locator('[data-testid="session-expiring-soon"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should offer to extend session
      await expect(page.locator('[data-testid="extend-session"]')).toBeVisible();
      await expect(page.locator('[data-testid="save-work"]')).toBeVisible();
      
      // Should preserve timer state during extension
      await page.click('[data-testid="extend-session"]');
      
      // Should maintain timer without interruption
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible();
      await expect(page.locator('[data-testid="session-extended"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should update session expiration time
      const newExpiration = await page.evaluate(() => localStorage.getItem('token_expires_at'));
      expect(parseInt(newExpiration || '0')).toBeGreaterThan(Date.now() + 600000); // At least 10 more minutes
    });

    test('should handle page refresh during critical operations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');
      
      // Fill critical form data
      await page.fill('[data-testid="hive-name"]', 'Critical Operation Hive');
      await page.fill('[data-testid="hive-description"]', 'Important data that must survive refresh');
      await page.selectOption('[data-testid="hive-type"]', 'private');
      await page.fill('[data-testid="hive-password"]', 'secret123');
      
      // Start submission process
      const submitPromise = page.click('[data-testid="submit-hive"]');
      
      // Wait a moment then refresh during submission
      await page.waitForTimeout(1000);
      await page.reload();
      
      // Should detect interrupted operation
      await expect(page.locator('[data-testid="operation-interrupted"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should offer recovery options
      await expect(page.locator('[data-testid="resume-operation"]')).toBeVisible();
      await expect(page.locator('[data-testid="start-over"]')).toBeVisible();
      
      // Should show operation details
      const operationDetails = page.locator('[data-testid="interrupted-operation-details"]');
      await expect(operationDetails).toContainText('Creating hive');
      await expect(operationDetails).toContainText('Critical Operation Hive');
      
      // Test resume operation
      await page.click('[data-testid="resume-operation"]');
      
      // Should complete the operation
      await expect(page.locator('[data-testid="operation-completed"]')).toBeVisible({ timeout: TIMEOUTS.LONG });
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible();
      
      // Verify hive was created successfully
      await page.goto('/dashboard');
      await expect(page.locator('[data-testid="hive-card"]:has-text("Critical Operation Hive")')).toBeVisible();
    });
  });
});