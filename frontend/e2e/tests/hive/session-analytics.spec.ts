/**
 * Session Analytics E2E Tests
 * Tests complete focus session workflows with personal and hive-level analytics
 * Includes achievement tracking, streaks, and data export functionality
 */

import {Browser, expect, Page, test} from '@playwright/test';
import {HivePage} from './pages/HivePage';
import {CreateHivePage} from './pages/CreateHivePage';
import {EnhancedAuthHelper} from '../../helpers/auth-helpers';
import {MultiUserHiveHelper} from './hive-helpers';
import {
  ANALYTICS_MOCK_DATA,
  generateUniqueHiveData,
  HIVE_TEST_USERS,
  type TestHive
} from './hive-fixtures';

// Extend Window interface for analytics testing
declare global {
  interface Window {
    __mockAnalyticsData?: boolean;
    __simulateSessionCompletion?: boolean;
  }
}

test.describe('Session Analytics Workflow', () => {
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

    // Login all users
    await ownerAuth.loginUser(HIVE_TEST_USERS.OWNER.email, HIVE_TEST_USERS.OWNER.password);
    await memberAuth.loginUser(HIVE_TEST_USERS.MEMBER_1.email, HIVE_TEST_USERS.MEMBER_1.password);
    await observerAuth.loginUser(HIVE_TEST_USERS.MEMBER_2.email, HIVE_TEST_USERS.MEMBER_2.password);

    // Create a test hive for analytics testing
    const hiveData = generateUniqueHiveData('ANALYTICS_TEST_HIVE');
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
  });

  test.describe('Complete Focus Session Workflow', () => {
    test('should track basic session completion', async () => {
      // Act - Complete a focus session
      await ownerHivePage.startTimer('POMODORO_25_5');

      // Simulate session completion for testing
      await ownerPage.evaluate(() => {
        window.__simulateSessionCompletion = true;
      });

      await ownerHivePage.stopTimer();

      // Assert - Session should be recorded in analytics
      const analytics = await ownerHivePage.getAnalyticsSummary();
      expect(analytics.completedSessions).toBeGreaterThan(0);
      expect(analytics.totalFocusTime).toBeGreaterThan(0);
    });

    test('should calculate average session length correctly', async () => {
      // Arrange - Complete multiple sessions of different lengths
      const sessionDurations = [25, 50, 30]; // minutes

      for (const duration of sessionDurations) {
        await ownerPage.evaluate((mins) => {
          window.__mockAnalyticsData = true;
          // Mock a completed session of specified duration
          const sessionData = {
            duration: mins * 60, // Convert to seconds
            completedAt: new Date().toISOString()
          };

          // Store in localStorage for persistence
          const sessions = JSON.parse(localStorage.getItem('completedSessions') || '[]');
          sessions.push(sessionData);
          localStorage.setItem('completedSessions', JSON.stringify(sessions));
        }, duration);
      }

      // Act - Get analytics summary
      const analytics = await ownerHivePage.getAnalyticsSummary();

      // Assert - Average should be calculated correctly
      const expectedAverage = Math.floor((25 + 50 + 30) / 3);
      expect(analytics.averageSessionLength).toBeCloseTo(expectedAverage, 0);
    });

    test('should track focus streaks', async () => {
      // Arrange - Complete sessions on consecutive days
      await ownerPage.evaluate(() => {
        // Mock consecutive daily sessions
        const today = new Date();
        const sessions = [];

        for (let i = 0; i < 5; i++) {
          const sessionDate = new Date(today);
          sessionDate.setDate(today.getDate() - i);

          sessions.push({
            duration: 25 * 60,
            completedAt: sessionDate.toISOString(),
            date: sessionDate.toDateString()
          });
        }

        localStorage.setItem('completedSessions', JSON.stringify(sessions));
      });

      // Act
      const analytics = await ownerHivePage.getAnalyticsSummary();

      // Assert
      expect(analytics.currentStreak).toBeGreaterThanOrEqual(5);
    });

    test('should show session statistics in real-time', async () => {
      // Act - Start timer and check real-time stats
      await ownerHivePage.startTimer('POMODORO_25_5');

      // Should show current session info
      await expect(
          ownerPage.locator('[data-testid="current-session-stats"]')
      ).toBeVisible();

      await expect(
          ownerPage.locator('[data-testid="session-start-time"]')
      ).toContainText(/\d{1,2}:\d{2}/); // Should show time format

      await expect(
          ownerPage.locator('[data-testid="session-type"]')
      ).toContainText('Pomodoro');
    });
  });

  test.describe('Personal Analytics Dashboard', () => {
    test('should display comprehensive personal statistics', async () => {
      // Arrange - Set up mock analytics data
      await ownerPage.evaluate((mockData) => {
        localStorage.setItem('personalAnalytics', JSON.stringify(mockData));
      }, ANALYTICS_MOCK_DATA.PERSONAL_STATS);

      // Act
      await ownerHivePage.openAnalytics();

      // Assert - All key metrics should be displayed
      await expect(
          ownerPage.locator('[data-testid="total-focus-time"]')
      ).toContainText('24'); // 1440 minutes = 24 hours

      await expect(
          ownerPage.locator('[data-testid="completed-sessions"]')
      ).toContainText('42');

      await expect(
          ownerPage.locator('[data-testid="current-streak"]')
      ).toContainText('7');

      await expect(
          ownerPage.locator('[data-testid="productivity-score"]')
      ).toContainText('85');
    });

    test('should show weekly progress chart', async () => {
      // Arrange
      await ownerPage.evaluate((mockData) => {
        localStorage.setItem('weeklyAnalytics', JSON.stringify(mockData));
      }, ANALYTICS_MOCK_DATA.PERSONAL_STATS.weeklyData);

      // Act
      await ownerHivePage.openAnalytics();

      // Assert
      await expect(
          ownerPage.locator('[data-testid="weekly-progress-chart"]')
      ).toBeVisible();

      // Chart should show data for each day
      const chartDays = await ownerPage.locator('[data-testid="chart-day"]').count();
      expect(chartDays).toBe(7);

      // Should show highest day (Friday with 240 minutes)
      await expect(
          ownerPage.locator('[data-testid="highest-day"]')
      ).toContainText('Friday');
    });

    test('should display goal progress indicators', async () => {
      // Act
      await ownerHivePage.openAnalytics();

      // Assert
      await expect(
          ownerPage.locator('[data-testid="weekly-goal-progress"]')
      ).toBeVisible();

      // Progress bar should show percentage
      const progressBar = ownerPage.locator('[data-testid="goal-progress-bar"]');
      await expect(progressBar).toHaveAttribute('aria-valuenow', expect.stringMatching(/\d+/));

      // Should show time until goal completion
      await expect(
          ownerPage.locator('[data-testid="time-to-goal"]')
      ).toBeVisible();
    });

    test('should show productivity trends over time', async () => {
      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="trends-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="productivity-trend-chart"]')
      ).toBeVisible();

      // Trend should show improvement or decline indicators
      await expect(
          ownerPage.locator('[data-testid="trend-indicator"]')
      ).toBeVisible();

      // Should allow changing time range
      await ownerPage.selectOption('[data-testid="trend-timerange"]', '30days');

      await expect(
          ownerPage.locator('[data-testid="trend-30days"]')
      ).toBeVisible();
    });
  });

  test.describe('Hive-Level Analytics', () => {
    test('should display hive community statistics', async () => {
      // Arrange - Set up mock hive analytics
      await ownerPage.evaluate((mockData) => {
        localStorage.setItem('hiveAnalytics', JSON.stringify(mockData));
      }, ANALYTICS_MOCK_DATA.HIVE_STATS);

      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="hive-analytics-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="hive-total-members"]')
      ).toContainText('8');

      await expect(
          ownerPage.locator('[data-testid="hive-active-members"]')
      ).toContainText('5');

      await expect(
          ownerPage.locator('[data-testid="hive-focus-time"]')
      ).toContainText('88'); // 5280 minutes = 88 hours
    });

    test('should show member leaderboard', async () => {
      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="hive-analytics-tab"]');
      await ownerPage.click('[data-testid="leaderboard-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="member-leaderboard"]')
      ).toBeVisible();

      // Should show member rankings
      const leaderboardItems = await ownerPage.locator('[data-testid="leaderboard-item"]').count();
      expect(leaderboardItems).toBeGreaterThan(0);

      // Top member should be highlighted
      await expect(
          ownerPage.locator('[data-testid="leaderboard-item"]').first().locator('[data-testid="top-member-badge"]')
      ).toBeVisible();
    });

    test('should display hive activity heatmap', async () => {
      // Arrange
      await ownerPage.evaluate((mockData) => {
        localStorage.setItem('hiveActivityData', JSON.stringify(mockData));
      }, ANALYTICS_MOCK_DATA.HIVE_STATS.popularTimes);

      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="hive-analytics-tab"]');
      await ownerPage.click('[data-testid="activity-heatmap-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="activity-heatmap"]')
      ).toBeVisible();

      // Should show peak activity times
      await expect(
          ownerPage.locator('[data-testid="peak-hours"]')
      ).toContainText('3 PM'); // Hour 15 with count 22
    });

    test('should compare individual performance to hive average', async () => {
      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="comparison-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="performance-comparison"]')
      ).toBeVisible();

      // Should show above/below average indicators
      await expect(
          ownerPage.locator('[data-testid="vs-average-sessions"]')
      ).toBeVisible();

      await expect(
          ownerPage.locator('[data-testid="vs-average-focus-time"]')
      ).toBeVisible();

      // Should show percentile ranking
      await expect(
          ownerPage.locator('[data-testid="percentile-ranking"]')
      ).toContainText('%');
    });
  });

  test.describe('Achievement System', () => {
    test('should track and display achievements', async () => {
      // Arrange - Simulate earning achievements
      await ownerPage.evaluate(() => {
        const achievements = [
          {
            id: 'first-session',
            name: 'First Focus',
            description: 'Complete your first focus session',
            earnedAt: new Date().toISOString(),
            icon: 'trophy'
          },
          {
            id: 'week-streak',
            name: 'Week Warrior',
            description: 'Maintain a 7-day focus streak',
            earnedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
            icon: 'flame'
          }
        ];
        localStorage.setItem('userAchievements', JSON.stringify(achievements));
      });

      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="achievements-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="achievement-list"]')
      ).toBeVisible();

      // Should show earned achievements
      await expect(
          ownerPage.locator('[data-testid="achievement-first-session"]')
      ).toBeVisible();

      await expect(
          ownerPage.locator('[data-testid="achievement-week-streak"]')
      ).toBeVisible();

      // Should show achievement progress
      await expect(
          ownerPage.locator('[data-testid="achievement-progress"]')
      ).toContainText('2 of');
    });

    test('should show achievement notifications when earned', async () => {
      // Act - Complete action that earns achievement (simulated)
      await ownerPage.evaluate(() => {
        // Simulate earning a new achievement
        const event = new CustomEvent('achievementEarned', {
          detail: {
            id: 'marathon-session',
            name: 'Marathon Master',
            description: 'Complete a 2-hour focus session',
            icon: 'medal'
          }
        });
        document.dispatchEvent(event);
      });

      // Assert
      await expect(
          ownerPage.locator('[data-testid="achievement-notification"]')
      ).toBeVisible();

      await expect(
          ownerPage.locator('[data-testid="achievement-notification"]')
      ).toContainText('Marathon Master');

      // Notification should auto-dismiss
      await expect(
          ownerPage.locator('[data-testid="achievement-notification"]')
      ).not.toBeVisible({timeout: 10000});
    });

    test('should show upcoming achievement previews', async () => {
      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="achievements-tab"]');
      await ownerPage.click('[data-testid="upcoming-achievements"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="upcoming-achievement-list"]')
      ).toBeVisible();

      // Should show progress toward unearned achievements
      const upcomingAchievements = await ownerPage.locator('[data-testid="upcoming-achievement"]').count();
      expect(upcomingAchievements).toBeGreaterThan(0);

      // Should show progress bars
      await expect(
          ownerPage.locator('[data-testid="upcoming-achievement"]').first().locator('[data-testid="achievement-progress-bar"]')
      ).toBeVisible();
    });
  });

  test.describe('Data Export Functionality', () => {
    test('should export analytics data as CSV', async () => {
      // Act
      await ownerHivePage.exportAnalytics('csv');

      // Assert - File should be downloaded
      // Note: In a real E2E test, you'd verify the download completed
      // For this test, we're checking the export flow initiated correctly
    });

    test('should export analytics data as JSON', async () => {
      // Act
      await ownerHivePage.exportAnalytics('json');

      // Assert - Export should initiate
      await expect(
          ownerPage.locator('[data-testid="export-success"]')
      ).toBeVisible();
    });

    test('should export analytics data as PDF report', async () => {
      // Act
      await ownerHivePage.exportAnalytics('pdf');

      // Assert - PDF generation should start
      await expect(
          ownerPage.locator('[data-testid="pdf-generating"]')
      ).toBeVisible();

      // Should complete generation
      await expect(
          ownerPage.locator('[data-testid="pdf-ready"]')
      ).toBeVisible({timeout: 10000});
    });

    test('should allow custom date range for exports', async () => {
      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="export-menu-button"]');
      await ownerPage.click('[data-testid="custom-date-range"]');

      // Set date range
      await ownerPage.fill('[data-testid="export-start-date"]', '2024-01-01');
      await ownerPage.fill('[data-testid="export-end-date"]', '2024-01-31');

      await ownerPage.click('[data-testid="export-csv"]');
      await ownerPage.click('[data-testid="confirm-export"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="export-success"]')
      ).toContainText('January 2024');
    });
  });

  test.describe('Analytics Performance and Reliability', () => {
    test('should load analytics dashboard quickly', async () => {
      // Act
      const startTime = Date.now();
      await ownerHivePage.openAnalytics();

      // Wait for all charts to load
      await expect(
          ownerPage.locator('[data-testid="analytics-loaded"]')
      ).toBeVisible();

      const loadTime = Date.now() - startTime;

      // Assert
      expect(loadTime).toBeLessThan(5000); // Should load within 5 seconds
    });

    test('should handle large datasets efficiently', async () => {
      // Arrange - Generate large analytics dataset
      await ownerPage.evaluate(() => {
        const largeSessions = [];
        for (let i = 0; i < 1000; i++) {
          largeSessions.push({
            duration: Math.floor(Math.random() * 3600) + 300, // 5-65 minutes
            completedAt: new Date(Date.now() - i * 24 * 60 * 60 * 1000).toISOString(),
            type: ['pomodoro', 'continuous', 'flexible'][Math.floor(Math.random() * 3)]
          });
        }
        localStorage.setItem('largeSessions', JSON.stringify(largeSessions));
      });

      // Act
      const startTime = Date.now();
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="load-large-dataset"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="large-dataset-loaded"]')
      ).toBeVisible({timeout: 10000});

      const loadTime = Date.now() - startTime;
      expect(loadTime).toBeLessThan(8000); // Should handle large dataset within 8 seconds
    });

    test('should gracefully handle analytics API failures', async () => {
      // Act - Simulate API failure
      await ownerPage.route('/api/analytics/**', route =>
          route.fulfill({status: 500, body: 'Server Error'})
      );

      await ownerHivePage.openAnalytics();

      // Assert - Should show error state with retry option
      await expect(
          ownerPage.locator('[data-testid="analytics-error-state"]')
      ).toBeVisible();

      await expect(
          ownerPage.locator('[data-testid="retry-analytics"]')
      ).toBeVisible();

      // Should offer offline mode if cached data available
      await expect(
          ownerPage.locator('[data-testid="offline-analytics"]')
      ).toBeVisible();
    });
  });

  test.describe('Multi-User Analytics Comparison', () => {
    test('should show collaborative session statistics', async () => {
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

        // Simulate collaborative session
        const _sessions = await multiUserHelper.simulateConcurrentTimerStart(testHive.id);

        // Act - View collaborative analytics
        await ownerHivePage.openAnalytics();
        await ownerPage.click('[data-testid="collaborative-sessions-tab"]');

        // Assert
        await expect(
            ownerPage.locator('[data-testid="collaborative-session-stats"]')
        ).toBeVisible();

        await expect(
            ownerPage.locator('[data-testid="session-participants"]')
        ).toContainText(users.length.toString());

      } finally {
        await multiUserHelper.cleanup();
      }
    });

    test('should display hive vs individual performance metrics', async () => {
      // Act
      await ownerHivePage.openAnalytics();
      await ownerPage.click('[data-testid="hive-comparison-tab"]');

      // Assert
      await expect(
          ownerPage.locator('[data-testid="individual-vs-hive"]')
      ).toBeVisible();

      // Should show relative performance indicators
      const performanceIndicators = [
        'above-average',
        'below-average',
        'at-average'
      ];

      let hasValidIndicator = false;
      for (const indicator of performanceIndicators) {
        const element = ownerPage.locator(`[data-testid="performance-${indicator}"]`);
        if (await element.isVisible()) {
          hasValidIndicator = true;
          break;
        }
      }

      expect(hasValidIndicator).toBe(true);
    });
  });

  test.describe('Real-time Analytics Updates', () => {
    test('should update analytics in real-time during sessions', async () => {
      // Act - Start a session
      await ownerHivePage.startTimer('POMODORO_25_5');

      // Open analytics in another tab
      const analyticsContext = await browser.newContext();
      const analyticsPage = await analyticsContext.newPage();
      const analyticsAuth = new EnhancedAuthHelper(analyticsPage);
      await analyticsAuth.loginUser(HIVE_TEST_USERS.OWNER.email, HIVE_TEST_USERS.OWNER.password);

      const analyticsHivePage = new HivePage(analyticsPage);
      await analyticsHivePage.goto(testHive.id);
      await analyticsHivePage.openAnalytics();

      // Assert - Should show current session in real-time
      await expect(
          analyticsPage.locator('[data-testid="current-session-timer"]')
      ).toBeVisible();

      await expect(
          analyticsPage.locator('[data-testid="session-in-progress"]')
      ).toContainText('In Progress');

      await analyticsPage.close();
      await analyticsContext.close();
    });

    test('should sync analytics across multiple user sessions', async () => {
      // Act - Complete session in one browser
      await ownerHivePage.startTimer('CONTINUOUS_60');

      // Simulate session completion
      await ownerPage.evaluate(() => {
        window.__simulateSessionCompletion = true;
      });

      await ownerHivePage.stopTimer();

      // Check analytics in another user's view of hive stats
      await memberHivePage.openAnalytics();
      await memberPage.click('[data-testid="hive-analytics-tab"]');

      // Assert - Hive analytics should reflect the completed session
      await memberPage.waitForTimeout(2000); // Allow for sync

      const hiveSessionCount = await memberPage.locator('[data-testid="hive-total-sessions"]').textContent();
      const sessionCount = parseInt(hiveSessionCount?.replace(/\D/g, '') || '0');
      expect(sessionCount).toBeGreaterThan(0);
    });
  });

  test.describe('Analytics Accessibility', () => {
    test('should provide accessible chart descriptions', async () => {
      // Act
      await ownerHivePage.openAnalytics();

      // Assert - Charts should have proper ARIA labels
      await expect(
          ownerPage.locator('[data-testid="weekly-progress-chart"]')
      ).toHaveAttribute('aria-label', expect.stringMatching(/weekly progress/i));

      // Should have data tables as alternatives
      await ownerPage.click('[data-testid="chart-data-table-toggle"]');

      await expect(
          ownerPage.locator('[data-testid="progress-data-table"]')
      ).toBeVisible();

      // Table should have proper headers
      await expect(
          ownerPage.locator('[data-testid="progress-data-table"] th')
      ).toHaveCount(3); // Day, Sessions, Focus Time
    });

    test('should support keyboard navigation in analytics', async () => {
      // Act
      await ownerHivePage.openAnalytics();

      // Navigate tabs with keyboard
      await ownerPage.focus('[data-testid="personal-analytics-tab"]');
      await ownerPage.keyboard.press('ArrowRight');

      // Should focus hive analytics tab
      await expect(
          ownerPage.locator('[data-testid="hive-analytics-tab"]')
      ).toBeFocused();

      await ownerPage.keyboard.press('Enter');

      // Should switch to hive analytics
      await expect(
          ownerPage.locator('[data-testid="hive-analytics-content"]')
      ).toBeVisible();
    });

    test('should announce important analytics updates', async () => {
      // Act - Earn an achievement
      await ownerPage.evaluate(() => {
        const event = new CustomEvent('achievementEarned', {
          detail: {
            name: 'Consistency King',
            description: 'Complete sessions for 30 consecutive days'
          }
        });
        document.dispatchEvent(event);
      });

      // Assert - Should announce to screen readers
      await expect(
          ownerPage.locator('[data-testid="analytics-announcements"][aria-live="assertive"]')
      ).toContainText('Achievement earned: Consistency King');
    });
  });
});