/**
 * Timer Session E2E Tests
 * Tests collaborative timer functionality, synchronization, and session management
 * Includes real-time features and multi-user timer coordination
 */

import { test, expect, Page, Browser } from '@playwright/test';
import { HivePage } from './pages/HivePage';
import { CreateHivePage } from './pages/CreateHivePage';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { HiveWorkflowHelper, MultiUserHiveHelper } from './hive-helpers';
import { WebSocketTestHelper } from './websocket-helpers';
import { 
  HIVE_TEST_USERS, 
  TIMER_CONFIGURATIONS,
  generateUniqueHiveData,
  type TestHive 
} from './hive-fixtures';

// Extend Window interface for timer testing
declare global {
  interface Window {
    __testMode?: boolean;
    __timerDuration?: number;
    __completeGroupTimer?: boolean;
  }
}

test.describe('Timer Session Workflow', () => {
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
  
  let testHive: TestHive;

  test.beforeAll(async ({ browser: testBrowser }) => {
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
    
    // Setup WebSocket monitoring
    await ownerWSHelper.initializeWebSocketMonitoring();
    await memberWSHelper.initializeWebSocketMonitoring();
    
    // Login all users
    await ownerAuth.loginUser(HIVE_TEST_USERS.OWNER.email, HIVE_TEST_USERS.OWNER.password);
    await memberAuth.loginUser(HIVE_TEST_USERS.MEMBER_1.email, HIVE_TEST_USERS.MEMBER_1.password);
    await observerAuth.loginUser(HIVE_TEST_USERS.MEMBER_2.email, HIVE_TEST_USERS.MEMBER_2.password);
    
    // Create a test hive for timer sessions
    const hiveData = generateUniqueHiveData('PUBLIC_STUDY_HIVE');
    await createHivePage.goto();
    await createHivePage.fillBasicInfo(hiveData);
    await createHivePage.configureSettings({
      allowChat: true,
      allowMusic: true
    });
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
  });

  test.describe('Individual Timer Sessions', () => {
    test('should start a basic pomodoro timer', async () => {
      // Act
      const sessionId = await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Assert
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.isActive).toBe(true);
      expect(timerState.isPaused).toBe(false);
      
      // Timer display should show initial time
      expect(timerState.remainingTime).toMatch(/^25:/); // Should start with 25 minutes
      
      // Session should be tracked
      expect(sessionId).toBeTruthy();
      await expect(ownerPage.locator(`[data-testid="active-timer"][data-session-id="${sessionId}"]`)).toBeVisible();
    });

    test('should start a continuous timer', async () => {
      // Act
      const sessionId = await ownerHivePage.startTimer('CONTINUOUS_60');
      
      // Assert
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.isActive).toBe(true);
      expect(timerState.remainingTime).toMatch(/^60:/); // Should start with 60 minutes
    });

    test('should pause and resume timer', async () => {
      // Arrange
      await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Act - Pause timer
      const pauseResult = await ownerHivePage.pauseResumeTimer();
      expect(pauseResult).toBe('paused');
      
      let timerState = await ownerHivePage.getTimerState();
      expect(timerState.isPaused).toBe(true);
      
      // Wait a moment to ensure timer doesn't tick down
      await ownerPage.waitForTimeout(2000);
      const timeAfterPause = await ownerHivePage.getTimerState();
      expect(timeAfterPause.remainingTime).toBe(timerState.remainingTime);
      
      // Act - Resume timer
      const resumeResult = await ownerHivePage.pauseResumeTimer();
      expect(resumeResult).toBe('resumed');
      
      timerState = await ownerHivePage.getTimerState();
      expect(timerState.isPaused).toBe(false);
    });

    test('should stop timer session', async () => {
      // Arrange
      await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Act
      await ownerHivePage.stopTimer();
      
      // Assert
      await expect(ownerPage.locator('[data-testid="timer-stopped"]')).toBeVisible();
      
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.isActive).toBe(false);
    });

    test('should persist timer state on page refresh', async () => {
      // Arrange
      const sessionId = await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Get initial timer state
      const initialState = await ownerHivePage.getTimerState();
      
      // Act - Refresh page
      await ownerPage.reload();
      await ownerHivePage.waitForLoad();
      
      // Assert - Timer should still be running
      const restoredState = await ownerHivePage.getTimerState();
      expect(restoredState.isActive).toBe(true);
      
      // Session should be restored
      await expect(
        ownerPage.locator(`[data-testid="active-timer"][data-session-id="${sessionId}"]`)
      ).toBeVisible();
    });

    test('should handle timer completion and break transition', async () => {
      // Note: This test would normally require waiting 25 minutes for a real pomodoro
      // We'll simulate fast timer completion for testing
      
      // Act - Start a very short timer for testing (1 second)
      await ownerPage.evaluate(() => {
        // Override timer duration for testing
        window.__testMode = true;
        window.__timerDuration = 1000; // 1 second
      });
      
      await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Wait for timer to complete
      await expect(
        ownerPage.locator('[data-testid="timer-completed"]')
      ).toBeVisible({ timeout: 5000 });
      
      // Assert - Should transition to break
      await expect(
        ownerPage.locator('[data-testid="break-timer-start"]')
      ).toBeVisible();
      
      // Session stats should be recorded
      await expect(
        ownerPage.locator('[data-testid="session-completed-stats"]')
      ).toBeVisible();
    });
  });

  test.describe('Timer Configuration', () => {
    test('should configure custom pomodoro settings', async () => {
      // Act
      await ownerHivePage.configureTimer('POMODORO_50_10');
      const sessionId = await ownerHivePage.startTimer();
      
      // Assert
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.remainingTime).toMatch(/^50:/); // Should start with 50 minutes
    });

    test('should save timer preferences', async () => {
      // Act
      await ownerHivePage.configureTimer('POMODORO_50_10');
      
      // Navigate away and back
      await ownerPage.goto('/dashboard');
      await ownerHivePage.goto(testHive.id);
      
      // Assert - Settings should be remembered
      await ownerPage.click('[data-testid="timer-settings-button"]');
      const workDurationInput = ownerPage.locator('[data-testid="work-duration-input"]');
      await expect(workDurationInput).toHaveValue('50');
    });

    test('should validate timer configuration limits', async () => {
      // Act & Assert - Test minimum duration
      await ownerPage.click('[data-testid="timer-settings-button"]');
      await ownerPage.fill('[data-testid="work-duration-input"]', '0');
      
      await expect(
        ownerPage.locator('[data-testid="duration-validation-error"]')
      ).toBeVisible();
      
      // Test maximum duration
      await ownerPage.fill('[data-testid="work-duration-input"]', '300'); // 5 hours
      await expect(
        ownerPage.locator('[data-testid="duration-validation-error"]')
      ).toBeVisible();
    });
  });

  test.describe('Real-time Timer Synchronization', () => {
    test('should synchronize timer start across multiple users', async () => {
      // Act - Owner starts timer
      const ownerSessionId = await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Assert - Timer sync message should be sent
      const syncMessage = await ownerWSHelper.waitForMessage('TIMER_SYNC');
      expect(syncMessage).toBeTruthy();
      expect(syncMessage?.payload.sessionId).toBe(ownerSessionId);
      
      // Member should see the synchronized timer
      await memberPage.waitForTimeout(1000); // Allow sync to propagate
      const memberTimerState = await memberHivePage.getTimerState();
      
      // Both users should have similar remaining time (within tolerance)
      const ownerTimerState = await ownerHivePage.getTimerState();
      const syncIsAccurate = await ownerHivePage.waitForTimerSync(ownerTimerState.remainingTime, 3);
      expect(syncIsAccurate).toBe(true);
    });

    test('should measure timer sync latency', async () => {
      // Act & Assert
      const latencyResult = await memberWSHelper.testTimerSync('test_session');
      
      expect(latencyResult.syncReceived).toBe(true);
      expect(latencyResult.latency).toBeLessThan(1000); // Should sync within 1 second
      expect(latencyResult.participantCount).toBeGreaterThan(0);
    });

    test('should handle multiple concurrent timers', async () => {
      // Act - Multiple users start individual timers
      const ownerSessionId = await ownerHivePage.startTimer('POMODORO_25_5');
      const memberSessionId = await memberHivePage.startTimer('CONTINUOUS_60');
      
      // Assert - Both timers should be visible to all users
      await expect(
        observerPage.locator(`[data-testid="user-timer-${HIVE_TEST_USERS.OWNER.id}"]`)
      ).toBeVisible();
      
      await expect(
        observerPage.locator(`[data-testid="user-timer-${HIVE_TEST_USERS.MEMBER_1.id}"]`)
      ).toBeVisible();
      
      // Each user should see their own active timer
      const ownerState = await ownerHivePage.getTimerState();
      const memberState = await memberHivePage.getTimerState();
      
      expect(ownerState.isActive).toBe(true);
      expect(memberState.isActive).toBe(true);
    });

    test('should sync timer pause/resume events', async () => {
      // Arrange
      await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Act - Owner pauses timer
      await ownerHivePage.pauseResumeTimer();
      
      // Assert - Pause should be synchronized
      const pauseMessage = await ownerWSHelper.waitForMessage('TIMER_PAUSE');
      expect(pauseMessage).toBeTruthy();
      
      // All users should see paused state
      await memberPage.waitForTimeout(500);
      const memberTimerState = await memberHivePage.getTimerState();
      expect(memberTimerState.isPaused).toBe(true);
      
      await observerPage.waitForTimeout(500);
      const ownerTimerElement = observerPage.locator(`[data-testid="user-timer-${HIVE_TEST_USERS.OWNER.id}"]`);
      await expect(ownerTimerElement.locator('[data-testid="timer-paused-indicator"]')).toBeVisible();
    });
  });

  test.describe('Collaborative Timer Features', () => {
    test('should start group timer session', async () => {
      // Act - Owner starts a group timer
      await ownerPage.click('[data-testid="start-group-timer-button"]');
      await ownerPage.click('[data-testid="confirm-group-timer"]');
      
      // Assert - All members should be invited to join
      await expect(
        memberPage.locator('[data-testid="group-timer-invitation"]')
      ).toBeVisible();
      
      await expect(
        observerPage.locator('[data-testid="group-timer-invitation"]')
      ).toBeVisible();
      
      // Members accept invitation
      await memberPage.click('[data-testid="join-group-timer"]');
      await observerPage.click('[data-testid="join-group-timer"]');
      
      // All users should have synchronized timers
      await ownerPage.waitForTimeout(1000);
      
      const ownerState = await ownerHivePage.getTimerState();
      const memberState = await memberHivePage.getTimerState();
      const observerState = await observerHivePage.getTimerState();
      
      expect(ownerState.isActive).toBe(true);
      expect(memberState.isActive).toBe(true);
      expect(observerState.isActive).toBe(true);
    });

    test('should handle group timer voting for breaks', async () => {
      // Arrange - Start group timer
      await ownerPage.click('[data-testid="start-group-timer-button"]');
      await memberPage.click('[data-testid="join-group-timer"]');
      await observerPage.click('[data-testid="join-group-timer"]');
      
      // Act - Member requests break
      await memberPage.click('[data-testid="request-break-button"]');
      
      // Assert - Break vote should appear for all users
      await expect(
        ownerPage.locator('[data-testid="break-vote-request"]')
      ).toBeVisible();
      
      await expect(
        observerPage.locator('[data-testid="break-vote-request"]')
      ).toBeVisible();
      
      // Vote for break
      await ownerPage.click('[data-testid="vote-yes-break"]');
      await observerPage.click('[data-testid="vote-yes-break"]');
      
      // Break should start with majority vote
      await expect(
        ownerPage.locator('[data-testid="group-break-started"]')
      ).toBeVisible();
    });

    test('should show group timer statistics', async () => {
      // Arrange - Complete a group timer session
      await ownerPage.click('[data-testid="start-group-timer-button"]');
      await memberPage.click('[data-testid="join-group-timer"]');
      
      // Simulate timer completion
      await ownerPage.evaluate(() => {
        window.__completeGroupTimer = true;
      });
      
      // Act - View group session stats
      await ownerPage.click('[data-testid="view-group-session-stats"]');
      
      // Assert - Should show collaborative statistics
      await expect(
        ownerPage.locator('[data-testid="group-session-participants"]')
      ).toBeVisible();
      
      await expect(
        ownerPage.locator('[data-testid="group-focus-time"]')
      ).toBeVisible();
      
      await expect(
        ownerPage.locator('[data-testid="group-productivity-score"]')
      ).toBeVisible();
    });
  });

  test.describe('Timer Performance and Reliability', () => {
    test('should handle network disconnection gracefully', async () => {
      // Arrange
      await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Act - Simulate network disconnection
      await ownerWSHelper.simulateConnectionIssues('disconnect');
      await ownerPage.waitForTimeout(2000);
      
      // Assert - Timer should continue locally
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.isActive).toBe(true);
      
      // Should show offline indicator
      await expect(
        ownerPage.locator('[data-testid="timer-offline-indicator"]')
      ).toBeVisible();
      
      // Reconnection should sync state
      await ownerPage.reload(); // Simulate reconnection
      await ownerHivePage.waitForLoad();
      
      const reconnectedState = await ownerHivePage.getTimerState();
      expect(reconnectedState.isActive).toBe(true);
    });

    test('should maintain accuracy under high load', async () => {
      // Create multiple timer sessions to test performance
      const multiUserHelper = new MultiUserHiveHelper();
      
      try {
        const users = [
          HIVE_TEST_USERS.MEMBER_1,
          HIVE_TEST_USERS.MEMBER_2,
          HIVE_TEST_USERS.MODERATOR
        ];
        
        const helpers = [];
        for (const user of users) {
          const helper = await multiUserHelper.createUserSession(browser, user);
          helpers.push(helper);
        }
        
        // Act - Start concurrent timer sessions
        const timerSessions = await multiUserHelper.simulateConcurrentTimerStart(testHive.id);
        
        // Assert - All timers should be synchronized
        expect(timerSessions.length).toBe(users.length);
        
        // Check sync accuracy across all sessions
        for (let i = 1; i < timerSessions.length; i++) {
          const timeDifference = Math.abs(
            timerSessions[i].remainingTime - timerSessions[0].remainingTime
          );
          expect(timeDifference).toBeLessThan(5); // Within 5 seconds
        }
        
      } finally {
        await multiUserHelper.cleanup();
      }
    });

    test('should measure WebSocket message latency for timer updates', async () => {
      // Act & Assert
      const latency = await ownerWSHelper.measureWebSocketLatency();
      
      expect(latency).toBeGreaterThan(0);
      expect(latency).toBeLessThan(1000); // Should be under 1 second
      
      console.log(`Timer WebSocket latency: ${latency}ms`);
    });

    test('should handle rapid timer actions without issues', async () => {
      // Act - Perform rapid timer operations
      const sessionId = await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Rapid pause/resume cycles
      for (let i = 0; i < 5; i++) {
        await ownerHivePage.pauseResumeTimer(); // Pause
        await ownerPage.waitForTimeout(100);
        await ownerHivePage.pauseResumeTimer(); // Resume
        await ownerPage.waitForTimeout(100);
      }
      
      // Assert - Timer should still be functional
      const finalState = await ownerHivePage.getTimerState();
      expect(finalState.isActive).toBe(true);
      expect(finalState.isPaused).toBe(false);
    });
  });

  test.describe('Timer Settings Persistence', () => {
    test('should save user timer preferences', async () => {
      // Act - Configure custom settings
      await ownerHivePage.configureTimer('POMODORO_50_10');
      
      // Navigate away and back
      await ownerPage.goto('/dashboard');
      await ownerHivePage.goto(testHive.id);
      
      // Assert - Settings should be preserved
      await ownerPage.click('[data-testid="timer-settings-button"]');
      
      const workDuration = await ownerPage.locator('[data-testid="work-duration-input"]').inputValue();
      const shortBreak = await ownerPage.locator('[data-testid="short-break-input"]').inputValue();
      
      expect(workDuration).toBe('50');
      expect(shortBreak).toBe('10');
    });

    test('should allow different timer settings per hive', async () => {
      // Create another test hive
      const secondHiveData = generateUniqueHiveData('SECOND_TIMER_HIVE');
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(secondHiveData);
      const secondHive = await createHivePage.createHive();
      
      // Configure different timer settings in each hive
      await ownerHivePage.goto(testHive.id);
      await ownerHivePage.configureTimer('POMODORO_25_5');
      
      await ownerHivePage.goto(secondHive.id);
      await ownerHivePage.configureTimer('CONTINUOUS_60');
      
      // Assert - Each hive should remember its own settings
      await ownerHivePage.goto(testHive.id);
      await ownerPage.click('[data-testid="timer-settings-button"]');
      let timerType = await ownerPage.locator('[data-testid="selected-timer-type"]').textContent();
      expect(timerType).toContain('pomodoro');
      
      await ownerHivePage.goto(secondHive.id);
      await ownerPage.click('[data-testid="timer-settings-button"]');
      timerType = await ownerPage.locator('[data-testid="selected-timer-type"]').textContent();
      expect(timerType).toContain('continuous');
    });
  });

  test.describe('Accessibility and Mobile Support', () => {
    test('should support keyboard timer controls', async () => {
      // Act - Use keyboard shortcuts
      await ownerHivePage.goto(testHive.id);
      
      // Start timer with spacebar
      await ownerPage.focus('[data-testid="start-timer-button"]');
      await ownerPage.keyboard.press('Space');
      
      // Assert - Timer should start
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.isActive).toBe(true);
      
      // Pause with P key
      await ownerPage.keyboard.press('p');
      
      const pausedState = await ownerHivePage.getTimerState();
      expect(pausedState.isPaused).toBe(true);
    });

    test('should announce timer events to screen readers', async () => {
      // Act
      await ownerHivePage.startTimer('POMODORO_25_5');
      
      // Assert - ARIA live regions should be updated
      await expect(
        ownerPage.locator('[data-testid="timer-announcements"][aria-live="polite"]')
      ).toContainText('Timer started');
      
      // Pause timer
      await ownerHivePage.pauseResumeTimer();
      
      await expect(
        ownerPage.locator('[data-testid="timer-announcements"]')
      ).toContainText('Timer paused');
    });

    test('should work on mobile viewports', async () => {
      // Act - Switch to mobile viewport
      await ownerPage.setViewportSize({ width: 375, height: 667 });
      await ownerHivePage.goto(testHive.id);
      
      // Assert - Timer controls should be accessible
      await expect(ownerPage.locator('[data-testid="mobile-timer-controls"]')).toBeVisible();
      
      // Touch interactions should work
      await ownerPage.tap('[data-testid="start-timer-button"]');
      
      const timerState = await ownerHivePage.getTimerState();
      expect(timerState.isActive).toBe(true);
    });
  });
});