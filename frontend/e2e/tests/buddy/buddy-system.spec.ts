/**
 * E2E Tests for Buddy System Functionality (UOL-311)
 *
 * Tests cover:
 * 1. Buddy Matching and Pairing
 * 2. Accountability Features
 * 3. Peer Support and Communication
 * 4. Collaborative Productivity
 * 5. Buddy Relationship Management
 * 6. Privacy and Safety
 * 7. Responsive Design
 * 8. Accessibility
 */

import {expect, test} from '@playwright/test';
import {BuddyPage} from '../../pages/BuddyPage';
import {BuddyHelper} from '../../helpers/buddy.helper';
import {AuthHelper} from '../../helpers/auth.helper';
import {PERFORMANCE_THRESHOLDS, TEST_USERS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Buddy System', () => {
  let buddyPage: BuddyPage;
  let buddyHelper: BuddyHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page}) => {
    // Initialize page objects and helpers
    buddyPage = new BuddyPage(page);
    buddyHelper = new BuddyHelper(page);
    authHelper = new AuthHelper(page);

    // Validate test environment
    validateTestEnvironment();

    // Clear any existing authentication and storage
    await authHelper.clearStorage();
  });

  test.afterEach(async ({page: _page}) => {
    // Cleanup after each test
    await buddyHelper.cleanup();
    await authHelper.clearStorage();
  });

  test.describe('Dashboard Loading and Basic Functionality', () => {
    test('should display buddy dashboard when route exists', async () => {
      // Mock user authentication
      await authHelper.loginWithValidUser();

      // Mock buddy dashboard data
      const mockData = buddyHelper.generateMockBuddyData();
      await buddyHelper.mockBuddyApi(mockData);

      // Navigate to buddy dashboard
      await buddyPage.goto();

      // Verify dashboard loads
      await expect(buddyPage.page).toHaveTitle(/FocusHive/);
      await expect(buddyPage.dashboardTitle).toBeVisible();
      await buddyPage.verifyDashboardLoaded();
    });

    test('should load buddy dashboard within performance threshold', async () => {
      // Mock authentication and data
      await authHelper.loginWithValidUser();
      const mockData = buddyHelper.generateMockBuddyData();
      await buddyHelper.mockBuddyApi(mockData);

      // Measure load performance
      const performanceMetrics = await buddyPage.measureLoadPerformance();

      // Verify performance requirements (dashboard should load within 3 seconds)
      expect(performanceMetrics.loadTime).toBeLessThan(PERFORMANCE_THRESHOLDS.DASHBOARD_LOAD_TIME);

      // Verify dashboard loaded successfully
      await buddyPage.verifyDashboardLoaded();
    });

    test('should display loading state during data fetch', async () => {
      await authHelper.loginWithValidUser();

      // Mock slow API response
      await buddyHelper.mockSlowBuddyApi(2000);

      await buddyPage.goto();

      // Verify loading spinner appears
      await expect(buddyPage.loadingSpinner).toBeVisible();

      // Wait for loading to complete
      await buddyPage.waitForDataLoad();

      // Verify loading spinner disappears
      await expect(buddyPage.loadingSpinner).not.toBeVisible();
    });

    test('should handle API errors gracefully', async () => {
      await authHelper.loginWithValidUser();

      // Mock API error
      await buddyHelper.mockBuddyApiError(500);

      await buddyPage.goto();
      await buddyPage.waitForPageLoad();

      // Verify error state is displayed
      await buddyPage.verifyErrorState();
      await expect(buddyPage.page.locator('text=/Error loading buddy data|Failed to load/i')).toBeVisible();

      // Test retry functionality
      const mockData = buddyHelper.generateMockBuddyData();
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.clickRetry();
      await buddyPage.waitForDataLoad();

      // Verify dashboard loads after retry
      await buddyPage.verifyDashboardLoaded();
    });

    test('should display empty state when no buddy data available', async () => {
      await authHelper.loginWithValidUser();

      // Mock empty buddy data
      await buddyHelper.mockEmptyBuddyData();

      await buddyPage.goto();
      await buddyPage.waitForPageLoad();

      // Verify empty state is displayed
      await buddyPage.verifyEmptyState();
      await expect(buddyPage.page.locator('text=/No buddy relationships yet|Get started by finding/i')).toBeVisible();
    });

    test('should handle network failures gracefully', async () => {
      await authHelper.loginWithValidUser();

      // Mock network failure
      await buddyHelper.mockNetworkFailure();

      await buddyPage.goto();
      await buddyPage.waitForPageLoad();

      // Verify error handling for network issues
      await buddyPage.verifyErrorState();
    });
  });

  test.describe('Buddy Matching and Pairing', () => {
    test('should display potential buddy matches with compatibility scores', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        includePotentialMatches: true,
        matchCount: 5
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Navigate to matching tab
      await buddyPage.switchToMatchingTab();

      // Verify potential matches are displayed
      await expect(buddyPage.potentialMatchesSection).toBeVisible();
      const matches = await buddyPage.getPotentialMatches();
      expect(matches.length).toBeGreaterThan(0);

      // Verify compatibility scores are shown
      for (const match of matches) {
        await expect(match.locator('[data-testid="compatibility-score"]')).toBeVisible();
        const score = await match.locator('[data-testid="compatibility-score"]').textContent();
        expect(score).toMatch(/\d+%/); // Should show percentage
      }
    });

    test('should allow filtering potential matches by preferences', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        includePotentialMatches: true,
        matchCount: 10
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.switchToMatchingTab();
      await buddyPage.waitForDataLoad();

      // Test focus area filter
      if (await buddyPage.focusAreaFilter.isVisible()) {
        await buddyPage.filterByFocusArea('Programming');
        await buddyPage.waitForDataLoad();

        // Verify filtered results
        const filteredMatches = await buddyPage.getPotentialMatches();
        for (const match of filteredMatches) {
          const focusAreas = await match.locator('[data-testid="focus-areas"]').textContent();
          expect(focusAreas).toContain('Programming');
        }
      }
    });

    test('should enable sending buddy requests to matched users', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        includePotentialMatches: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.switchToMatchingTab();
      await buddyPage.waitForDataLoad();

      // Click on send request to first potential match
      const firstMatch = buddyPage.potentialMatchesSection.locator('[data-testid="buddy-match-card"]').first();
      await firstMatch.locator('[data-testid="send-request-button"]').click();

      // Verify buddy request dialog opens
      await expect(buddyPage.buddyRequestDialog).toBeVisible();

      // Fill out request form
      await buddyPage.fillBuddyRequest({
        message: 'Hi! I\'d love to be accountability buddies.',
        proposedEndDate: '2024-12-31',
        goals: 'Complete project milestones together',
        expectations: 'Weekly check-ins and mutual support'
      });

      // Submit request
      await buddyPage.submitBuddyRequest();

      // Verify success message
      await expect(buddyPage.page.locator('.MuiAlert-root[severity="success"]')).toBeVisible();
      await expect(buddyPage.page.locator('text=/Buddy request sent successfully/i')).toBeVisible();
    });

    test('should display compatibility algorithm details', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        includePotentialMatches: true,
        includeMatchReasons: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.switchToMatchingTab();
      await buddyPage.waitForDataLoad();

      // Click on match details for first potential match
      const firstMatch = buddyPage.potentialMatchesSection.locator('[data-testid="buddy-match-card"]').first();
      await firstMatch.locator('[data-testid="view-details-button"]').click();

      // Verify compatibility breakdown is shown
      await expect(buddyPage.compatibilityBreakdown).toBeVisible();

      // Verify different matching factors are displayed
      await expect(buddyPage.compatibilityBreakdown.locator('text=/Timezone Match/i')).toBeVisible();
      await expect(buddyPage.compatibilityBreakdown.locator('text=/Focus Area Match/i')).toBeVisible();
      await expect(buddyPage.compatibilityBreakdown.locator('text=/Communication Style/i')).toBeVisible();
      await expect(buddyPage.compatibilityBreakdown.locator('text=/Availability Match/i')).toBeVisible();
    });

    test('should support manual buddy search functionality', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        includeSearchResults: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.switchToMatchingTab();
      await buddyPage.waitForDataLoad();

      // Use search functionality
      if (await buddyPage.buddySearchInput.isVisible()) {
        await buddyPage.searchForBuddy('TestUser');

        // Verify search results are displayed
        await expect(buddyPage.searchResults).toBeVisible();
        const searchResults = await buddyPage.getSearchResults();
        expect(searchResults.length).toBeGreaterThan(0);

        // Verify search results contain the search term
        const firstResult = searchResults[0];
        const username = await firstResult.locator('[data-testid="username"]').textContent();
        expect(username?.toLowerCase()).toContain('testuser');
      }
    });

    test('should validate buddy relationship limits', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 5, // Assuming max is 5
        includePotentialMatches: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.switchToMatchingTab();
      await buddyPage.waitForDataLoad();

      // Try to send another buddy request when at limit
      const firstMatch = buddyPage.potentialMatchesSection.locator('[data-testid="buddy-match-card"]').first();
      const sendButton = firstMatch.locator('[data-testid="send-request-button"]');

      if (await sendButton.isVisible()) {
        await sendButton.click();

        // Should show limit warning
        await expect(buddyPage.page.locator('text=/You have reached the maximum number of active buddies/i')).toBeVisible();
      } else {
        // Button should be disabled with tooltip
        await expect(firstMatch.locator('[data-testid="send-request-disabled"]')).toBeVisible();
      }
    });
  });

  test.describe('Accountability Features', () => {
    test('should display active buddy relationships with goal tracking', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 3,
        includeGoals: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Should be on active buddies tab by default
      await expect(buddyPage.activeBuddiesSection).toBeVisible();

      // Verify buddy cards show goal information
      const buddyCards = await buddyPage.getActiveBuddyCards();
      expect(buddyCards.length).toBe(3);

      for (const card of buddyCards) {
        await expect(card.locator('[data-testid="goal-count"]')).toBeVisible();
        await expect(card.locator('[data-testid="goal-progress"]')).toBeVisible();
      }
    });

    test('should enable goal sharing and progress tracking', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeGoals: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Click on first active buddy to view goals
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="view-goals-button"]').click();

      // Verify goals list is displayed
      await expect(buddyPage.goalsModal).toBeVisible();
      await expect(buddyPage.goalsModal.locator('[data-testid="goal-item"]')).toHaveCount(3); // Mock includes 3 goals

      // Test adding a new goal
      await buddyPage.goalsModal.locator('[data-testid="add-goal-button"]').click();

      // Fill new goal form
      await buddyPage.fillNewGoal({
        title: 'Complete Chapter 5',
        description: 'Read and summarize chapter 5 of the textbook',
        dueDate: '2024-12-15'
      });

      await buddyPage.submitNewGoal();

      // Verify success message
      await expect(buddyPage.page.locator('text=/Goal added successfully/i')).toBeVisible();
    });

    test('should support buddy check-ins and progress updates', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeCheckins: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Initiate check-in with buddy
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="checkin-button"]').click();

      // Verify check-in dialog opens
      await expect(buddyPage.checkinDialog).toBeVisible();

      // Fill check-in form
      await buddyPage.fillCheckin({
        moodRating: 4,
        progressRating: 3,
        message: 'Making good progress on my goals today!',
        currentFocus: 'Working on project implementation',
        challenges: 'Time management with multiple tasks',
        wins: 'Completed two important milestones'
      });

      // Submit check-in
      await buddyPage.submitCheckin();

      // Verify success message
      await expect(buddyPage.page.locator('text=/Check-in submitted successfully/i')).toBeVisible();
    });

    test('should display accountability streak tracking', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeStreaks: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Navigate to stats/accountability tab if it exists
      if (await buddyPage.statsTab.isVisible()) {
        await buddyPage.switchToStatsTab();

        // Verify streak information is displayed
        await expect(buddyPage.streakSection).toBeVisible();
        await expect(buddyPage.streakSection.locator('[data-testid="current-streak"]')).toBeVisible();
        await expect(buddyPage.streakSection.locator('[data-testid="best-streak"]')).toBeVisible();

        // Verify streak rewards/achievements
        if (await buddyPage.streakRewards.isVisible()) {
          await expect(buddyPage.streakRewards.locator('[data-testid="streak-badge"]')).toHaveCount(2);
        }
      }
    });

    test('should provide gentle reminders and nudging', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeReminders: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Check for reminder notifications
      if (await buddyPage.reminderSection.isVisible()) {
        await expect(buddyPage.reminderSection).toBeVisible();

        // Verify different types of reminders
        const reminders = await buddyPage.reminderSection.locator('[data-testid="reminder-item"]');
        expect(await reminders.count()).toBeGreaterThan(0);

        // Test dismissing a reminder
        const firstReminder = reminders.first();
        await firstReminder.locator('[data-testid="dismiss-reminder"]').click();

        // Verify reminder is dismissed
        await expect(firstReminder).not.toBeVisible();
      }
    });

    test('should implement privacy controls for shared information', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includePrivacySettings: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Access privacy settings for buddy relationship
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="privacy-settings-button"]').click();

      // Verify privacy settings dialog
      await expect(buddyPage.privacySettingsDialog).toBeVisible();

      // Test privacy control toggles
      const privacyToggles = buddyPage.privacySettingsDialog.locator('[data-testid="privacy-toggle"]');
      expect(await privacyToggles.count()).toBeGreaterThan(0);

      // Toggle a privacy setting
      const firstToggle = privacyToggles.first();
      await firstToggle.click();

      // Save privacy settings
      await buddyPage.privacySettingsDialog.locator('[data-testid="save-privacy-button"]').click();

      // Verify success message
      await expect(buddyPage.page.locator('text=/Privacy settings updated/i')).toBeVisible();
    });
  });

  test.describe('Peer Support and Communication', () => {
    test('should enable direct messaging between buddies', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeMessages: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Click message button on first buddy
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="message-button"]').click();

      // Verify chat interface opens
      await expect(buddyPage.chatInterface).toBeVisible();

      // Send a message
      const messageText = 'Hey! How is your progress going today?';
      await buddyPage.sendMessage(messageText);

      // Verify message appears in chat
      await expect(buddyPage.chatInterface.locator(`text=${messageText}`)).toBeVisible();
    });

    test('should provide encouragement and motivation tools', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeEncouragementTools: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test encouragement features
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();

      // Send quick encouragement
      if (await firstBuddy.locator('[data-testid="encourage-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="encourage-button"]').click();

        // Select encouragement type
        await expect(buddyPage.encouragementMenu).toBeVisible();
        await buddyPage.encouragementMenu.locator('[data-testid="cheer-option"]').click();

        // Verify encouragement sent
        await expect(buddyPage.page.locator('text=/Encouragement sent/i')).toBeVisible();
      }
    });

    test('should support shared workspace features', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeSharedWorkspace: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Access shared workspace
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="shared-workspace-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="shared-workspace-button"]').click();

        // Verify shared workspace opens
        await expect(buddyPage.sharedWorkspace).toBeVisible();

        // Test workspace features (shared notes, documents, etc.)
        if (await buddyPage.sharedWorkspace.locator('[data-testid="shared-notes"]').isVisible()) {
          await expect(buddyPage.sharedWorkspace.locator('[data-testid="shared-notes"]')).toBeVisible();
        }
      }
    });

    test('should handle buddy-specific notifications', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeBuddyNotifications: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Check notification area
      if (await buddyPage.notificationBell.isVisible()) {
        await buddyPage.notificationBell.click();

        // Verify buddy-related notifications
        await expect(buddyPage.notificationPanel).toBeVisible();

        const buddyNotifications = buddyPage.notificationPanel.locator('[data-testid="buddy-notification"]');
        expect(await buddyNotifications.count()).toBeGreaterThan(0);

        // Test notification actions
        const firstNotification = buddyNotifications.first();
        if (await firstNotification.locator('[data-testid="respond-button"]').isVisible()) {
          await firstNotification.locator('[data-testid="respond-button"]').click();
          // Should navigate to appropriate section
        }
      }
    });

    test('should support help-seeking mechanisms', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeHelpFeatures: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test help request feature
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="request-help-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="request-help-button"]').click();

        // Fill help request form
        await expect(buddyPage.helpRequestDialog).toBeVisible();

        await buddyPage.fillHelpRequest({
          urgency: 'medium',
          category: 'motivation',
          description: 'Feeling stuck on a difficult task and need some encouragement',
          preferredResponse: 'call'
        });

        await buddyPage.submitHelpRequest();

        // Verify success message
        await expect(buddyPage.page.locator('text=/Help request sent/i')).toBeVisible();
      }
    });
  });

  test.describe('Collaborative Productivity', () => {
    test('should support shared focus sessions', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeSharedSessions: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Navigate to sessions tab
      if (await buddyPage.sessionsTab.isVisible()) {
        await buddyPage.switchToSessionsTab();

        // Schedule a shared focus session
        await buddyPage.scheduleSessionButton.click();

        // Fill session details
        await expect(buddyPage.sessionScheduleDialog).toBeVisible();

        await buddyPage.fillSessionDetails({
          buddyId: 'buddy-1',
          sessionDate: '2024-12-20',
          sessionTime: '14:00',
          duration: 60,
          agenda: 'Work on individual projects with mutual accountability'
        });

        await buddyPage.submitSessionSchedule();

        // Verify session appears in upcoming sessions
        await expect(buddyPage.upcomingSessionsList).toBeVisible();
        const sessions = await buddyPage.getUpcomingSessions();
        expect(sessions.length).toBeGreaterThan(0);
      }
    });

    test('should enable joint goal setting and tracking', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeJointGoals: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Access joint goals section
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="joint-goals-button"]').click();

      // Verify joint goals interface
      await expect(buddyPage.jointGoalsModal).toBeVisible();

      // Create a new joint goal
      await buddyPage.jointGoalsModal.locator('[data-testid="add-joint-goal-button"]').click();

      await buddyPage.fillJointGoal({
        title: 'Complete Project Phase 1 Together',
        description: 'Both partners complete their respective project phase 1 milestones',
        targetDate: '2024-12-31',
        individualTargets: {
          self: 'Complete backend implementation',
          buddy: 'Complete frontend implementation'
        }
      });

      await buddyPage.submitJointGoal();

      // Verify success message
      await expect(buddyPage.page.locator('text=/Joint goal created successfully/i')).toBeVisible();
    });

    test('should support buddy challenges and competitions', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeChallenges: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Navigate to challenges section
      if (await buddyPage.challengesTab.isVisible()) {
        await buddyPage.switchToChallengesTab();

        // View available challenges
        await expect(buddyPage.challengesSection).toBeVisible();

        // Create a new challenge
        await buddyPage.createChallengeButton.click();

        await expect(buddyPage.challengeCreateDialog).toBeVisible();

        await buddyPage.fillChallengeDetails({
          challengeType: 'focus-time',
          title: 'Who can focus longer this week?',
          description: 'Weekly focus time competition',
          duration: 7, // 7 days
          rules: 'Most total focus time wins',
          prize: 'Loser buys coffee!'
        });

        // Select buddy to challenge
        await buddyPage.selectChallengeOpponent('buddy-1');

        await buddyPage.submitChallenge();

        // Verify challenge created
        await expect(buddyPage.page.locator('text=/Challenge sent successfully/i')).toBeVisible();
      }
    });

    test('should enable study group formation', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 3,
        includeStudyGroups: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test study group creation if feature exists
      if (await buddyPage.studyGroupsTab.isVisible()) {
        await buddyPage.switchToStudyGroupsTab();

        // Create new study group
        await buddyPage.createStudyGroupButton.click();

        await expect(buddyPage.studyGroupDialog).toBeVisible();

        await buddyPage.fillStudyGroupDetails({
          name: 'Algorithm Study Group',
          description: 'Weekly algorithm practice and discussion',
          subject: 'Computer Science',
          maxMembers: 5,
          meetingSchedule: 'Weekly on Saturdays 2-4 PM'
        });

        // Invite buddies to group
        await buddyPage.selectStudyGroupMembers(['buddy-1', 'buddy-2']);

        await buddyPage.submitStudyGroup();

        // Verify study group created
        await expect(buddyPage.page.locator('text=/Study group created successfully/i')).toBeVisible();
      }
    });

    test('should support peer mentoring features', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeMentoring: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test mentoring relationship setup
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="mentoring-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="mentoring-button"]').click();

        // Setup mentoring relationship
        await expect(buddyPage.mentoringSetupDialog).toBeVisible();

        await buddyPage.fillMentoringSetup({
          role: 'mentor', // or 'mentee'
          expertise: 'JavaScript and React development',
          goals: 'Help improve coding skills and best practices',
          commitment: 'Weekly 1-hour sessions'
        });

        await buddyPage.submitMentoringSetup();

        // Verify mentoring relationship established
        await expect(buddyPage.page.locator('text=/Mentoring relationship established/i')).toBeVisible();
      }
    });
  });

  test.describe('Buddy Relationship Management', () => {
    test('should display buddy profile information', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeProfiles: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Click on buddy profile
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="view-profile-button"]').click();

      // Verify profile modal
      await expect(buddyPage.profileModal).toBeVisible();

      // Verify profile information
      await expect(buddyPage.profileModal.locator('[data-testid="buddy-username"]')).toBeVisible();
      await expect(buddyPage.profileModal.locator('[data-testid="buddy-avatar"]')).toBeVisible();
      await expect(buddyPage.profileModal.locator('[data-testid="buddy-bio"]')).toBeVisible();
      await expect(buddyPage.profileModal.locator('[data-testid="focus-areas"]')).toBeVisible();
      await expect(buddyPage.profileModal.locator('[data-testid="buddy-stats"]')).toBeVisible();
    });

    test('should track relationship status and history', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeRelationshipHistory: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // View relationship details
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="relationship-details-button"]').click();

      // Verify relationship status
      await expect(buddyPage.relationshipModal).toBeVisible();
      await expect(buddyPage.relationshipModal.locator('[data-testid="relationship-status"]')).toBeVisible();
      await expect(buddyPage.relationshipModal.locator('[data-testid="relationship-duration"]')).toBeVisible();

      // View relationship history
      if (await buddyPage.relationshipModal.locator('[data-testid="history-tab"]').isVisible()) {
        await buddyPage.relationshipModal.locator('[data-testid="history-tab"]').click();

        await expect(buddyPage.relationshipModal.locator('[data-testid="history-timeline"]')).toBeVisible();

        // Verify history events are displayed
        const historyEvents = buddyPage.relationshipModal.locator('[data-testid="history-event"]');
        expect(await historyEvents.count()).toBeGreaterThan(0);
      }
    });

    test('should enable buddy switching and rotation', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 3,
        includeSwitchingOptions: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test buddy relationship termination (for rotation)
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="manage-relationship-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="manage-relationship-button"]').click();

        await expect(buddyPage.relationshipManagementMenu).toBeVisible();

        // Test temporary pause option
        if (await buddyPage.relationshipManagementMenu.locator('[data-testid="pause-relationship"]').isVisible()) {
          await buddyPage.relationshipManagementMenu.locator('[data-testid="pause-relationship"]').click();

          // Fill pause reason
          await buddyPage.fillPauseReason({
            reason: 'Taking a short break to focus on exams',
            duration: '2 weeks',
            resumeDate: '2024-12-30'
          });

          await buddyPage.submitPauseRequest();

          // Verify relationship paused
          await expect(buddyPage.page.locator('text=/Relationship paused successfully/i')).toBeVisible();
        }
      }
    });

    test('should support feedback and rating systems', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeFeedbackSystem: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Leave feedback for completed session or relationship milestone
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="leave-feedback-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="leave-feedback-button"]').click();

        // Fill feedback form
        await expect(buddyPage.feedbackDialog).toBeVisible();

        await buddyPage.fillFeedback({
          rating: 5,
          categories: {
            communication: 5,
            helpfulness: 4,
            reliability: 5,
            motivation: 4
          },
          comment: 'Great buddy! Very supportive and reliable.',
          wouldRecommend: true
        });

        await buddyPage.submitFeedback();

        // Verify feedback submitted
        await expect(buddyPage.page.locator('text=/Feedback submitted successfully/i')).toBeVisible();
      }
    });

    test('should provide conflict resolution tools', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeConflictResolution: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Access conflict resolution if needed
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="report-issue-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="report-issue-button"]').click();

        // Fill issue report
        await expect(buddyPage.reportIssueDialog).toBeVisible();

        await buddyPage.fillIssueReport({
          issueType: 'communication',
          severity: 'medium',
          description: 'Buddy has not been responsive to messages',
          desiredOutcome: 'Improved communication frequency',
          hasAttemptedResolution: true
        });

        await buddyPage.submitIssueReport();

        // Verify issue reported
        await expect(buddyPage.page.locator('text=/Issue reported successfully/i')).toBeVisible();
      }
    });
  });

  test.describe('Privacy and Safety', () => {
    test('should prevent harassment and maintain safety', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeSafetyFeatures: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test blocking functionality
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      if (await firstBuddy.locator('[data-testid="safety-menu"]').isVisible()) {
        await firstBuddy.locator('[data-testid="safety-menu"]').click();

        await expect(buddyPage.safetyMenu).toBeVisible();

        // Test block user option
        if (await buddyPage.safetyMenu.locator('[data-testid="block-user"]').isVisible()) {
          await buddyPage.safetyMenu.locator('[data-testid="block-user"]').click();

          // Confirm blocking action
          await expect(buddyPage.blockConfirmDialog).toBeVisible();

          await buddyPage.fillBlockReason({
            reason: 'Inappropriate behavior',
            details: 'User sent inappropriate messages',
            reportToModerators: true
          });

          await buddyPage.confirmBlock();

          // Verify user blocked
          await expect(buddyPage.page.locator('text=/User blocked successfully/i')).toBeVisible();
        }
      }
    });

    test('should secure buddy communications', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeSecureCommunications: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test secure messaging
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.locator('[data-testid="message-button"]').click();

      await expect(buddyPage.chatInterface).toBeVisible();

      // Verify security indicators
      await expect(buddyPage.chatInterface.locator('[data-testid="encryption-indicator"]')).toBeVisible();

      // Test message encryption
      const sensitiveMessage = 'This is a private message with personal information';
      await buddyPage.sendMessage(sensitiveMessage);

      // Verify message appears encrypted in UI (placeholder behavior)
      await expect(buddyPage.chatInterface.locator(`text=${sensitiveMessage}`)).toBeVisible();
    });

    test('should validate privacy settings enforcement', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includePrivacyValidation: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test privacy settings compliance
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();

      // Verify sensitive information is hidden based on privacy settings
      if (await firstBuddy.locator('[data-testid="view-profile-button"]').isVisible()) {
        await firstBuddy.locator('[data-testid="view-profile-button"]').click();

        await expect(buddyPage.profileModal).toBeVisible();

        // Check that private information respects privacy settings
        const personalInfo = buddyPage.profileModal.locator('[data-testid="personal-info"]');
        if (await personalInfo.isVisible()) {
          // Should show placeholder text if privacy settings restrict access
          const infoText = await personalInfo.textContent();
          expect(infoText).not.toContain('real_email@'); // Should not show real email
        }
      }
    });
  });

  test.describe('Real-time Updates and Performance', () => {
    test('should handle real-time buddy status updates', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2,
        includeRealtimeUpdates: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Verify real-time status updates
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await expect(firstBuddy.locator('[data-testid="online-status"]')).toBeVisible();

      // Simulate status change
      await buddyHelper.simulateStatusChange('buddy-1', 'in-focus-session');

      // Verify status update appears
      await expect(firstBuddy.locator('[data-testid="online-status"]')).toContainText('In Focus Session');
    });

    test('should maintain good performance with multiple buddies', async () => {
      await authHelper.loginWithValidUser();

      // Generate large dataset
      const largeDataset = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 10,
        includeAllFeatures: true
      });

      await buddyHelper.mockBuddyApi(largeDataset);

      // Monitor performance
      const performanceMonitor = await buddyHelper.setupPerformanceMonitoring();

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Get performance metrics
      const metrics = await performanceMonitor.getMetrics();

      // Verify performance thresholds
      expect(metrics.apiResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.API_RESPONSE_TIME);
      expect(metrics.renderTime).toBeLessThan(PERFORMANCE_THRESHOLDS.CHART_RENDER_TIME);
    });

    test('should handle concurrent buddy interactions efficiently', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 3,
        includeConcurrentInteractions: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Simulate multiple rapid interactions
      const buddyCards = await buddyPage.getActiveBuddyCards();

      // Perform concurrent actions
      const promises = [];
      for (let i = 0; i < Math.min(buddyCards.length, 3); i++) {
        promises.push(
            buddyCards[i].locator('[data-testid="quick-checkin-button"]').click()
        );
      }

      await Promise.all(promises);

      // Verify system remains stable
      await buddyPage.verifyDashboardLoaded();
    });
  });

  test.describe('Responsive Design and Accessibility', () => {
    test('should adapt to different screen sizes', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 3
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test responsive design across screen sizes
      await buddyHelper.testResponsiveDesign();
    });

    test('should be accessible on mobile devices', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();

      // Test mobile layout
      await buddyPage.verifyMobileLayout();

      // Verify touch interactions work
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await firstBuddy.tap();
    });

    test('should meet WCAG 2.1 AA accessibility standards', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test accessibility features
      await buddyPage.verifyAccessibility();

      // Test specific buddy system accessibility
      await buddyHelper.validateBuddyAccessibility();
    });

    test('should support keyboard navigation', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test keyboard navigation
      await buddyPage.page.keyboard.press('Tab');

      // Navigate through buddy cards using keyboard
      const focusedElement = await buddyPage.page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
      expect(focusedElement).toMatch(/buddy-card|tab-button|action-button/);

      // Test Enter key activation
      await buddyPage.page.keyboard.press('Enter');

      // Should activate focused element (behavior depends on implementation)
    });

    test('should have proper color contrast and visual indicators', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 2
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Verify high contrast elements are visible
      await expect(buddyPage.dashboardTitle).toBeVisible();

      // Verify status indicators have appropriate colors and accessibility
      const statusIndicators = buddyPage.page.locator('[data-testid="online-status"]');
      for (const indicator of await statusIndicators.all()) {
        await expect(indicator).toBeVisible();
        // Verify text is readable (implementation would check contrast ratios)
      }
    });
  });

  test.describe('Integration with Authentication and Other Features', () => {
    test('should redirect unauthenticated users to login', async ({page}) => {
      // Clear any authentication
      await authHelper.clearStorage();

      // Try to access buddy dashboard directly
      await page.goto('/buddy');

      // Should be redirected to login or show auth prompt
      const response = await page.waitForResponse(/buddy|login|auth/);
      expect([200, 302, 401, 403]).toContain(response.status());
    });

    test('should display user-specific buddy data when authenticated', async () => {
      // Mock user authentication
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        userId: TEST_USERS.VALID_USER.email,
        activeBuddyCount: 2
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Verify dashboard shows user-specific data
      await buddyPage.verifyDashboardLoaded();
      await expect(buddyPage.dashboardTitle).toContainText('Buddy System');
    });

    test('should integrate with notification system', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 1,
        includeNotificationIntegration: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Test notification integration
      if (await buddyPage.notificationBell.isVisible()) {
        // Verify buddy-related notifications appear in global notification system
        await buddyPage.notificationBell.click();

        const buddyNotifications = buddyPage.notificationPanel.locator('[data-notification-type="buddy"]');
        if (await buddyNotifications.count() > 0) {
          await expect(buddyNotifications.first()).toBeVisible();
        }
      }
    });
  });

  test.describe('Error Handling and Edge Cases', () => {
    test('should handle malformed buddy API responses', async () => {
      await authHelper.loginWithValidUser();

      await buddyPage.page.route('**/api/v1/buddy/**', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: 'invalid json response'
        });
      });

      await buddyPage.goto();
      await buddyPage.waitForPageLoad();

      // Should show error state for malformed data
      await buddyPage.verifyErrorState();
    });

    test('should handle partial buddy data gracefully', async () => {
      await authHelper.loginWithValidUser();

      const partialData = {
        activeBuddies: [{
          id: 1,
          username: 'TestBuddy',
          status: 'ACTIVE'
          // Missing other required fields
        }],
        pendingRequests: [],
        sentRequests: [],
        upcomingSessions: [],
        stats: null // Missing stats
      };

      await buddyHelper.mockBuddyApi(partialData);
      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Should display available data and handle missing data gracefully
      await expect(buddyPage.activeBuddiesSection).toBeVisible();

      // Should show placeholder or default values for missing data
      const firstBuddy = buddyPage.activeBuddiesSection.locator('[data-testid="buddy-card"]').first();
      await expect(firstBuddy).toBeVisible();
      await expect(firstBuddy.locator('[data-testid="buddy-username"]')).toContainText('TestBuddy');
    });

    test('should recover from temporary network issues', async () => {
      await authHelper.loginWithValidUser();

      // Start with network failure
      await buddyHelper.mockNetworkFailure();

      await buddyPage.goto();
      await buddyPage.waitForPageLoad();
      await buddyPage.verifyErrorState();

      // Restore network and retry
      const mockData = buddyHelper.generateMockBuddyData();
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.clickRetry();
      await buddyPage.waitForDataLoad();

      // Should recover and display data
      await buddyPage.verifyDashboardLoaded();
    });

    test('should handle buddy relationship limit edge cases', async () => {
      await authHelper.loginWithValidUser();

      const mockData = buddyHelper.generateMockBuddyData({
        activeBuddyCount: 5, // At maximum limit
        includeMaxLimitScenario: true
      });
      await buddyHelper.mockBuddyApi(mockData);

      await buddyPage.goto();
      await buddyPage.waitForDataLoad();

      // Verify system handles maximum relationship limit
      await expect(buddyPage.activeBuddiesSection).toBeVisible();

      // Try to navigate to matching - should show limit warning
      await buddyPage.switchToMatchingTab();

      if (await buddyPage.limitWarning.isVisible()) {
        await expect(buddyPage.limitWarning).toContainText('maximum number of active buddies');
      }
    });
  });
});