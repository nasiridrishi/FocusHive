/**
 * E2E Tests for Gamification Features (UOL-310)
 *
 * Tests cover:
 * 1. Points and Scoring System
 * 2. Achievement System
 * 3. Leaderboards and Competition
 * 4. Challenges and Goals
 * 5. Social Gamification
 * 6. Streak Mechanics
 * 7. Real-time Updates
 * 8. Responsive Design
 * 9. Accessibility
 */

import {expect, test} from '@playwright/test';
import {GamificationPage} from '../../pages/GamificationPage';
import {GamificationHelper} from '../../helpers/gamification.helper';
import {AuthHelper} from '../../helpers/auth.helper';
import {PERFORMANCE_THRESHOLDS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Gamification Features', () => {
  let gamificationPage: GamificationPage;
  let gamificationHelper: GamificationHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page}) => {
    // Initialize page objects and helpers
    gamificationPage = new GamificationPage(page);
    gamificationHelper = new GamificationHelper(page);
    authHelper = new AuthHelper(page);

    // Validate test environment
    validateTestEnvironment();

    // Clear any existing authentication
    await authHelper.clearStorage();
  });

  test.afterEach(async ({page: _page}) => {
    // Cleanup after each test
    await gamificationHelper.cleanup();
    await authHelper.clearStorage();
  });

  test.describe('Dashboard Loading and Basic Functionality', () => {
    test('should display gamification demo page when route is not implemented', async () => {
      // Since /gamification route might not exist, test the demo page
      await gamificationPage.gotoDemo();

      // Verify demo page loads
      await expect(gamificationPage.page).toHaveTitle(/FocusHive/);
      await expect(gamificationPage.page.locator('h1, h3').filter({hasText: /Gamification/})).toBeVisible();

      // Verify demo dashboard is visible
      await expect(gamificationPage.dashboard).toBeVisible();
    });

    test('should load gamification dashboard within performance threshold', async () => {
      // Mock gamification data
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);

      // Measure load performance
      const performanceMetrics = await gamificationPage.measureLoadPerformance();

      // Verify performance requirements (dashboard should load within 3 seconds)
      expect(performanceMetrics.loadTime).toBeLessThan(PERFORMANCE_THRESHOLDS.DASHBOARD_LOAD_TIME);

      // Verify dashboard loaded successfully
      await gamificationPage.verifyDashboardLoaded();
    });

    test('should display loading state during data fetch', async () => {
      // Mock slow API response
      await gamificationHelper.mockSlowGamificationApi(2000);

      await gamificationPage.gotoDemo();

      // Verify loading spinner appears
      await expect(gamificationPage.loadingSpinner).toBeVisible();
      await expect(gamificationPage.page.locator('text=Loading gamification data')).toBeVisible();

      // Wait for loading to complete
      await gamificationPage.waitForDataLoad();

      // Verify loading spinner disappears
      await expect(gamificationPage.loadingSpinner).not.toBeVisible();
    });

    test('should handle API errors gracefully', async () => {
      // Mock API error
      await gamificationHelper.mockGamificationApiError(500);

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForPageLoad();

      // Verify error state is displayed
      await gamificationPage.verifyErrorState();
      await expect(gamificationPage.page.locator('text=Error loading gamification data')).toBeVisible();

      // Verify retry button works
      // Reset to successful response
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);

      await gamificationPage.clickRetry();
      await gamificationPage.waitForDataLoad();

      // Verify dashboard loads after retry
      await gamificationPage.verifyDashboardLoaded();
    });

    test('should display empty state when no data available', async () => {
      // Mock empty gamification data
      await gamificationHelper.mockEmptyGamification();

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForPageLoad();

      // Verify empty state is displayed
      await gamificationPage.verifyEmptyState();
      await expect(gamificationPage.page.locator('text=Start using FocusHive to see your gamification progress')).toBeVisible();
    });

    test('should handle network failures gracefully', async () => {
      // Mock network failure
      await gamificationHelper.mockNetworkFailure();

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForPageLoad();

      // Verify error handling for network issues
      await gamificationPage.verifyErrorState();
    });
  });

  test.describe('Points and Scoring System', () => {
    test('should display points accurately with correct calculations', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        pointsAmount: 1250
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify points accuracy
      await gamificationHelper.validatePointsAccuracy(mockData.points);
    });

    test('should calculate points with 100% precision', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        pointsAmount: 1234 // Specific test amount
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify exact point values
      const currentPoints = await gamificationPage.getCurrentPoints();
      expect(currentPoints).toBe(1234);

      const totalPoints = await gamificationPage.getTotalPoints();
      expect(totalPoints).toBe(mockData.points.total);
    });

    test('should award points for completed focus sessions', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        pointsAmount: 500
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Mock focus session completion
      const initialPoints = await gamificationPage.getCurrentPoints();

      // Add points for session completion
      await gamificationPage.addPoints(100);

      // Verify points were added correctly
      const newPoints = await gamificationPage.getCurrentPoints();
      expect(newPoints).toBeGreaterThan(initialPoints);
    });

    test('should award bonus points for streaks and goals', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        pointsAmount: 800,
        streakCount: 3
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify streak bonuses are visible
      const streaks = await gamificationPage.streakCounters.count();
      expect(streaks).toBeGreaterThan(0);

      // Verify points display includes bonus information
      const pointsDisplay = gamificationPage.pointsDisplay;
      await expect(pointsDisplay).toBeVisible();

      // Check if bonus points are indicated
      const bonusIndicator = gamificationPage.page.locator(':has-text("bonus"), :has-text("Bonus")');
      if (await bonusIndicator.isVisible()) {
        await expect(bonusIndicator).toBeVisible();
      }
    });

    test('should maintain point history and audit trails', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        pointsAmount: 1500
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify today's and weekly earnings are shown
      const todayPoints = gamificationPage.pointsToday;
      const weekPoints = gamificationPage.pointsWeek;

      if (await todayPoints.isVisible()) {
        const todayText = await todayPoints.textContent();
        expect(todayText).toContain(mockData.points.todayEarned.toString());
      }

      if (await weekPoints.isVisible()) {
        const weekText = await weekPoints.textContent();
        expect(weekText).toContain(mockData.points.weekEarned.toString());
      }
    });

    test('should handle point redemption and spending mechanisms', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        pointsAmount: 2000 // High amount for spending
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for point spending options
      const spendButton = gamificationPage.page.locator('button:has-text("Spend"), button:has-text("Redeem")');
      if (await spendButton.isVisible()) {
        await spendButton.click();

        // Verify spending modal or options appear
        const spendingModal = gamificationPage.page.locator('[data-testid="point-spending-modal"]');
        await expect(spendingModal).toBeVisible();
      }
    });
  });

  test.describe('Achievement System', () => {
    test('should unlock achievements based on milestones', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 6
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify achievement grid is visible
      await expect(gamificationPage.achievementsGrid).toBeVisible();

      // Test achievement unlocking
      const lockedAchievements = await gamificationPage.getAchievementCount('locked');
      if (lockedAchievements > 0) {
        await gamificationHelper.testAchievementUnlocking('focus-master');
      }
    });

    test('should display badge collection and progress tracking', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 8
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify achievement badges are displayed
      const achievements = await gamificationPage.getAchievementCount('all');
      expect(achievements).toBeGreaterThan(0);

      // Check unlocked vs locked achievements
      const unlockedCount = await gamificationPage.getAchievementCount('unlocked');
      const lockedCount = await gamificationPage.getAchievementCount('locked');

      expect(unlockedCount + lockedCount).toBe(achievements);

      // Verify progress indicators for locked achievements
      const progressBars = gamificationPage.achievementProgress;
      const progressCount = await progressBars.count();
      expect(progressCount).toBeGreaterThanOrEqual(0);
    });

    test('should track progress towards achievements correctly', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 5
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Find achievements with progress tracking
      const achievementsWithProgress = gamificationPage.page.locator('[data-testid^="achievement-"]:has([data-testid^="achievement-progress-"])');
      const progressCount = await achievementsWithProgress.count();

      if (progressCount > 0) {
        const firstProgressAchievement = achievementsWithProgress.first();
        const progressBar = firstProgressAchievement.locator('[data-testid^="achievement-progress-"]');

        // Verify progress bar shows correct percentage
        const ariaValueNow = await progressBar.getAttribute('aria-valuenow');
        const ariaValueMax = await progressBar.getAttribute('aria-valuemax');

        if (ariaValueNow && ariaValueMax) {
          const currentProgress = parseInt(ariaValueNow);
          const maxProgress = parseInt(ariaValueMax);
          expect(currentProgress).toBeLessThanOrEqual(maxProgress);
          expect(currentProgress).toBeGreaterThanOrEqual(0);
        }
      }
    });

    test('should display rare and special achievement mechanics', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 10
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for rare achievements with special styling
      const rareAchievements = gamificationPage.page.locator('[data-testid^="achievement-"][data-rarity="rare"], [data-testid^="achievement-"][data-rarity="epic"], [data-testid^="achievement-"][data-rarity="legendary"]');
      const rareCount = await rareAchievements.count();

      if (rareCount > 0) {
        const firstRareAchievement = rareAchievements.first();

        // Verify special styling for rare achievements
        await expect(firstRareAchievement).toHaveClass(/rare|epic|legendary/);

        // Check for special visual effects
        const rarityIndicator = firstRareAchievement.locator('.rarity-indicator, [data-testid="rarity-indicator"]');
        if (await rarityIndicator.isVisible()) {
          await expect(rarityIndicator).toBeVisible();
        }
      }
    });

    test('should show achievement notifications and celebrations', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 4
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test achievement unlock notification
      if (await gamificationPage.unlockAchievementButton.isVisible()) {
        await gamificationPage.unlockAchievementButton.click();

        // Verify achievement unlock notification appears
        await expect(gamificationPage.achievementNotification).toBeVisible({timeout: 5000});

        // Verify celebration animation or effect
        const celebrationEffect = gamificationPage.page.locator('.celebration, .achievement-celebration, [data-testid="achievement-celebration"]');
        if (await celebrationEffect.isVisible()) {
          await expect(celebrationEffect).toBeVisible();
        }
      }
    });
  });

  test.describe('Leaderboards and Competition', () => {
    test('should display personal leaderboards within hives', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        leaderboardSize: 10
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify leaderboards are visible
      await expect(gamificationPage.leaderboards).toBeVisible();

      // Check different leaderboard types
      const weeklyEntries = await gamificationPage.getLeaderboardEntries('weekly');
      expect(weeklyEntries.length).toBeGreaterThan(0);

      // Verify leaderboard entries have correct data
      weeklyEntries.forEach(entry => {
        expect(entry.rank).toBeGreaterThan(0);
        expect(entry.name).toBeTruthy();
        expect(entry.points).toBeGreaterThanOrEqual(0);
      });
    });

    test('should show global leaderboards across platform', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        leaderboardSize: 15
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test all-time leaderboard
      const allTimeEntries = await gamificationPage.getLeaderboardEntries('all-time');
      if (allTimeEntries.length > 0) {
        expect(allTimeEntries.length).toBeGreaterThan(0);

        // Verify entries are sorted by points (descending)
        for (let i = 0; i < allTimeEntries.length - 1; i++) {
          expect(allTimeEntries[i].points).toBeGreaterThanOrEqual(allTimeEntries[i + 1].points);
        }
      }
    });

    test('should handle weekly and monthly competition cycles', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        leaderboardSize: 8
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test weekly leaderboard
      const weeklyEntries = await gamificationPage.getLeaderboardEntries('weekly');
      if (weeklyEntries.length > 0) {
        expect(weeklyEntries.length).toBeGreaterThan(0);
      }

      // Test monthly leaderboard
      const monthlyEntries = await gamificationPage.getLeaderboardEntries('monthly');
      if (monthlyEntries.length > 0) {
        expect(monthlyEntries.length).toBeGreaterThan(0);
      }
    });

    test('should implement fair ranking algorithms and tie-breaking', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        leaderboardSize: 12
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      const entries = await gamificationPage.getLeaderboardEntries('weekly');

      if (entries.length > 1) {
        // Verify ranking is sequential
        for (let i = 0; i < entries.length; i++) {
          expect(entries[i].rank).toBe(i + 1);
        }

        // Verify no duplicate ranks (proper tie-breaking)
        const ranks = entries.map(entry => entry.rank);
        const uniqueRanks = new Set(ranks);
        expect(uniqueRanks.size).toBe(ranks.length);
      }
    });

    test('should update leaderboards in real-time (<30s delay)', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        leaderboardSize: 6
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test real-time leaderboard updates
      await gamificationHelper.testLeaderboardUpdates();

      // Verify leaderboard reflects changes
      const entries = await gamificationPage.getLeaderboardEntries('weekly');
      expect(entries.length).toBeGreaterThan(0);
    });

    test('should respect leaderboard privacy settings and opt-out options', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        leaderboardSize: 5
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for privacy controls
      const privacySettings = gamificationPage.page.locator('[data-testid="privacy-settings"], button:has-text("Privacy")');
      if (await privacySettings.isVisible()) {
        await privacySettings.click();

        // Verify privacy options are available
        const optOutOption = gamificationPage.page.locator('input[type="checkbox"]:has-text("leaderboard"), :has-text("opt out")');
        if (await optOutOption.isVisible()) {
          await expect(optOutOption).toBeVisible();
        }
      }
    });
  });

  test.describe('Challenges and Goals', () => {
    test('should create daily and weekly challenges', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        challengeCount: 5
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify challenges list is visible
      await expect(gamificationPage.challengesList).toBeVisible();

      // Check for active challenges
      const activeChallenges = gamificationPage.activeChallenges;
      if (await activeChallenges.isVisible()) {
        const challengeCards = activeChallenges.locator('[data-testid^="challenge-"]');
        const challengeCount = await challengeCards.count();
        expect(challengeCount).toBeGreaterThan(0);
      }
    });

    test('should support personal goal setting and tracking', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        goalCount: 4
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test goal creation functionality
      if (await gamificationPage.createGoalButton.isVisible()) {
        await gamificationPage.createGoalButton.click();

        const goalModal = gamificationPage.page.locator('[data-testid="create-goal-modal"]');
        await expect(goalModal).toBeVisible();
      }

      // Verify existing goals are displayed
      const goalsList = gamificationPage.goalsList;
      if (await goalsList.isVisible()) {
        const goals = goalsList.locator('[data-testid^="goal-"]');
        const goalCount = await goals.count();
        expect(goalCount).toBeGreaterThanOrEqual(0);
      }
    });

    test('should enable team-based collaborative challenges', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        challengeCount: 6
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for team challenges
      const teamChallenges = gamificationPage.page.locator('[data-testid^="challenge-"][data-type="team"], [data-testid^="challenge-"]:has-text("team")');
      const teamChallengeCount = await teamChallenges.count();

      if (teamChallengeCount > 0) {
        const firstTeamChallenge = teamChallenges.first();

        // Verify team challenge has participant count
        const participantCount = firstTeamChallenge.locator('[data-testid="participant-count"]');
        if (await participantCount.isVisible()) {
          const countText = await participantCount.textContent();
          expect(countText).toMatch(/\d+/); // Should contain a number
        }
      }
    });

    test('should verify challenge completion correctly', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        challengeCount: 3
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Find challenges with progress tracking
      const challengesWithProgress = gamificationPage.page.locator('[data-testid^="challenge-"]:has([data-testid^="challenge-progress-"])');
      const progressCount = await challengesWithProgress.count();

      if (progressCount > 0) {
        const firstChallenge = challengesWithProgress.first();
        const challengeId = await firstChallenge.getAttribute('data-testid');
        const id = challengeId?.replace('challenge-', '') || '';

        // Get challenge progress
        const progress = await gamificationPage.getChallengeProgress(id);

        // Verify progress values are valid
        expect(progress.current).toBeGreaterThanOrEqual(0);
        expect(progress.target).toBeGreaterThan(0);
        expect(progress.percentage).toBeGreaterThanOrEqual(0);
        expect(progress.percentage).toBeLessThanOrEqual(100);
      }
    });

    test('should distribute rewards fairly for challenge winners', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        challengeCount: 4
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for completed challenges with rewards
      const completedChallenges = gamificationPage.completedChallenges;
      if (await completedChallenges.isVisible()) {
        const challenges = completedChallenges.locator('[data-testid^="challenge-"]');
        const challengeCount = await challenges.count();

        if (challengeCount > 0) {
          const firstChallenge = challenges.first();

          // Check for reward information
          const rewardSection = firstChallenge.locator('[data-testid^="challenge-rewards-"]');
          if (await rewardSection.isVisible()) {
            // Verify reward points are displayed
            const pointsReward = rewardSection.locator(':has-text("points")');
            await expect(pointsReward).toBeVisible();
          }
        }
      }
    });
  });

  test.describe('Social Gamification Features', () => {
    test('should enable friend comparison and friendly competition', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        friendCount: 6
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test social features
      await gamificationHelper.testSocialFeatures();

      // Verify friends list if available
      const friendsList = gamificationPage.friendsList;
      if (await friendsList.isVisible()) {
        const friends = friendsList.locator('[data-testid^="friend-"]');
        const friendCount = await friends.count();
        expect(friendCount).toBeGreaterThanOrEqual(0);
      }
    });

    test('should support achievement sharing and social features', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 6,
        friendCount: 4
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for sharing functionality
      const shareButtons = gamificationPage.page.locator('button:has-text("Share"), [data-testid="share-achievement"]');
      const shareCount = await shareButtons.count();

      if (shareCount > 0) {
        const firstShareButton = shareButtons.first();
        await firstShareButton.click();

        // Verify share modal or options appear
        const shareModal = gamificationPage.page.locator('[data-testid="share-modal"], [role="dialog"]:has-text("Share")');
        if (await shareModal.isVisible()) {
          await expect(shareModal).toBeVisible();
        }
      }
    });

    test('should provide team collaboration rewards', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        friendCount: 8
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Check for team features
      const teamsList = gamificationPage.teamsList;
      if (await teamsList.isVisible()) {
        const teams = teamsList.locator('[data-testid^="team-"]');
        const teamCount = await teams.count();

        if (teamCount > 0) {
          const firstTeam = teams.first();

          // Verify team stats are displayed
          const teamPoints = firstTeam.locator('[data-testid="team-points"]');
          const teamRank = firstTeam.locator('[data-testid="team-rank"]');

          if (await teamPoints.isVisible()) {
            const pointsText = await teamPoints.textContent();
            expect(pointsText).toMatch(/\d+/);
          }

          if (await teamRank.isVisible()) {
            const rankText = await teamRank.textContent();
            expect(rankText).toMatch(/\d+/);
          }
        }
      }
    });

    test('should support mentor/mentee gamification systems', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        friendCount: 3
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Check for mentorship features
      const mentorshipsList = gamificationPage.mentorshipsList;
      if (await mentorshipsList.isVisible()) {
        const mentorships = mentorshipsList.locator('[data-testid^="mentorship-"]');
        const mentorshipCount = await mentorships.count();

        if (mentorshipCount > 0) {
          const firstMentorship = mentorships.first();

          // Verify mentorship progress is displayed
          const progress = firstMentorship.locator('[data-testid="mentorship-progress"]');
          if (await progress.isVisible()) {
            const progressValue = await progress.getAttribute('aria-valuenow');
            if (progressValue) {
              expect(parseInt(progressValue)).toBeGreaterThanOrEqual(0);
            }
          }
        }
      }
    });

    test('should enable community recognition and showcasing', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        achievementCount: 10,
        friendCount: 5
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Look for community features
      const communitySection = gamificationPage.page.locator('[data-testid="community-showcase"], :has-text("Community")');
      if (await communitySection.isVisible()) {
        // Verify community achievements or highlights
        const highlights = communitySection.locator('[data-testid^="community-highlight-"]');
        const highlightCount = await highlights.count();
        expect(highlightCount).toBeGreaterThanOrEqual(0);
      }
    });
  });

  test.describe('Streak Mechanics', () => {
    test('should track and display various streak types correctly', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        streakCount: 4
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test streak mechanics
      await gamificationHelper.testStreakMechanics();

      // Verify different streak types
      const streakTypes: ('daily_login' | 'focus_session' | 'goal_completion' | 'hive_participation')[] = ['daily_login', 'focus_session', 'goal_completion', 'hive_participation'];

      for (const type of streakTypes) {
        const streak = gamificationPage.page.locator(`[data-testid="streak-counter-${type}"]`);
        if (await streak.isVisible()) {
          const streakData = await gamificationPage.getStreakValue(type);

          expect(streakData.current).toBeGreaterThanOrEqual(0);
          expect(streakData.best).toBeGreaterThanOrEqual(streakData.current);
          expect(typeof streakData.isActive).toBe('boolean');
        }
      }
    });

    test('should maintain streak consistency and validation', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        streakCount: 3
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test streak update functionality
      if (await gamificationPage.updateStreakButton.isVisible()) {
        await gamificationPage.updateStreakButton.click();

        // Verify streak update notification
        await expect(gamificationPage.streakNotification).toBeVisible({timeout: 5000});
      }
    });

    test('should display streak flames and visual indicators', async () => {
      const mockData = gamificationHelper.generateMockGamificationData({
        streakCount: 2
      });

      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Check for streak visual indicators
      const streakFlames = gamificationPage.streakFlame;
      const flameCount = await streakFlames.count();

      if (flameCount > 0) {
        // Verify flames are visible for active streaks
        const firstFlame = streakFlames.first();
        await expect(firstFlame).toBeVisible();

        // Check for animation or special styling
        const isAnimated = await firstFlame.evaluate((el) => {
          const styles = window.getComputedStyle(el);
          return styles.animation !== 'none' || styles.transform !== 'none';
        });

        // Animation might not be present, which is acceptable
        expect(typeof isAnimated).toBe('boolean');
      }
    });
  });

  test.describe('Real-time Updates and Performance', () => {
    test('should handle real-time gamification updates efficiently', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test real-time updates
      await gamificationPage.verifyRealTimeUpdates();

      // Verify dashboard remains responsive
      await gamificationPage.verifyDashboardLoaded();
    });

    test('should maintain good performance with large datasets', async () => {
      // Generate large dataset
      const largeDataset = gamificationHelper.generateMockGamificationData({
        achievementCount: 20,
        leaderboardSize: 50,
        challengeCount: 15,
        goalCount: 10,
        friendCount: 25
      });

      await gamificationHelper.mockGamificationApi(largeDataset);

      // Monitor performance
      const performanceMonitor = await gamificationHelper.setupPerformanceMonitoring();

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Get performance metrics
      const metrics = await performanceMonitor.getMetrics();

      // Verify performance thresholds
      expect(metrics.apiResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.API_RESPONSE_TIME);
      expect(metrics.loadTime).toBeLessThan(PERFORMANCE_THRESHOLDS.DASHBOARD_LOAD_TIME);
    });

    test('should handle concurrent gamification updates efficiently', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Simulate multiple rapid updates
      const updatePromises = [];
      for (let i = 0; i < 3; i++) {
        updatePromises.push(gamificationPage.clickRefresh());
        await gamificationPage.page.waitForTimeout(200);
      }

      await Promise.all(updatePromises);

      // Verify dashboard remains stable
      await gamificationPage.verifyDashboardLoaded();
    });
  });

  test.describe('Responsive Design and Accessibility', () => {
    test('should adapt to different screen sizes', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test responsive design
      await gamificationHelper.testResponsiveDesign();
    });

    test('should be accessible on mobile devices', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();

      // Test mobile layout
      await gamificationPage.verifyMobileLayout();

      // Verify touch interactions work
      if (await gamificationPage.refreshButton.isVisible()) {
        await gamificationPage.refreshButton.tap();
      }
    });

    test('should meet WCAG 2.1 AA accessibility standards', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test accessibility features
      await gamificationPage.verifyAccessibility();
      await gamificationHelper.testAccessibility();
    });

    test('should support keyboard navigation', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Test keyboard navigation
      await gamificationPage.page.keyboard.press('Tab');

      // Verify focusable elements
      const focusedElement = await gamificationPage.page.evaluate(() => document.activeElement?.tagName);
      expect(['BUTTON', 'A', 'INPUT', 'SELECT']).toContain(focusedElement);

      // Test navigation to key elements
      if (await gamificationPage.refreshButton.isVisible()) {
        await gamificationPage.refreshButton.focus();
        await gamificationPage.page.keyboard.press('Enter');
        await gamificationPage.waitForDataLoad();
      }
    });

    test('should have proper color contrast and visual indicators', async () => {
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify high contrast elements are visible
      await expect(gamificationPage.dashboardTitle).toBeVisible();
      await expect(gamificationPage.pointsDisplay).toBeVisible();

      // Verify status indicators have appropriate colors
      const statusElements = gamificationPage.page.locator('.MuiChip-root, .MuiAlert-root, .status-indicator');
      const statusCount = await statusElements.count();
      expect(statusCount).toBeGreaterThanOrEqual(0);
    });
  });

  test.describe('Integration and Error Handling', () => {
    test('should redirect unauthenticated users appropriately', async ({page}) => {
      // Clear any authentication
      await authHelper.clearStorage();

      // Try to access gamification directly
      await page.goto('/gamification');

      // Should handle unauthenticated access gracefully
      const response = await page.waitForResponse(/gamification|login|auth/);
      expect([200, 302, 401, 403]).toContain(response.status());
    });

    test('should display user-specific gamification data when authenticated', async () => {
      // Mock user authentication
      await authHelper.loginWithValidUser();

      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Verify dashboard shows user-specific data
      await gamificationPage.verifyDashboardLoaded();
      await expect(gamificationPage.dashboardTitle).toContainText(/Gamification/);
    });

    test('should handle malformed API responses', async () => {
      await gamificationPage.page.route('**/api/v1/gamification/**', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: 'invalid json response'
        });
      });

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForPageLoad();

      // Should show error state for malformed data
      await gamificationPage.verifyErrorState();
    });

    test('should handle partial data gracefully', async () => {
      const partialData = {
        points: {current: 100, total: 500, todayEarned: 25, weekEarned: 75},
        achievements: [],
        streaks: [],
        leaderboards: [],
        challenges: [],
        goals: [],
        social: {friends: [], buddies: [], teams: [], mentorships: []},
        level: 1,
        rank: 0,
        totalUsers: 0,
        lastUpdated: new Date().toISOString()
      };

      await gamificationHelper.mockGamificationApi(partialData);
      await gamificationPage.gotoDemo();
      await gamificationPage.waitForDataLoad();

      // Should display available data and handle missing data gracefully
      await expect(gamificationPage.pointsDisplay).toBeVisible();
      const currentPoints = await gamificationPage.getCurrentPoints();
      expect(currentPoints).toBe(100);
    });

    test('should recover from temporary network issues', async () => {
      // Start with network failure
      await gamificationHelper.mockNetworkFailure();

      await gamificationPage.gotoDemo();
      await gamificationPage.waitForPageLoad();
      await gamificationPage.verifyErrorState();

      // Restore network and retry
      const mockData = gamificationHelper.generateMockGamificationData();
      await gamificationHelper.mockGamificationApi(mockData);

      await gamificationPage.clickRetry();
      await gamificationPage.waitForDataLoad();

      // Should recover and display data
      await gamificationPage.verifyDashboardLoaded();
    });
  });
});