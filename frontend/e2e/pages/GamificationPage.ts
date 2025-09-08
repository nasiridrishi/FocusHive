/**
 * Page Object Model for Gamification Features
 * Provides methods to interact with gamification features in E2E tests
 */

import { Page, Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../helpers/test-data';

export class GamificationPage {
  readonly page: Page;

  // Main dashboard elements
  readonly dashboard: Locator;
  readonly dashboardTitle: Locator;
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;
  readonly retryButton: Locator;
  readonly refreshButton: Locator;
  readonly emptyStateMessage: Locator;

  // Points Display elements
  readonly pointsDisplay: Locator;
  readonly pointsCurrent: Locator;
  readonly pointsTotal: Locator;
  readonly pointsToday: Locator;
  readonly pointsWeek: Locator;
  readonly addPointsButton: Locator;
  readonly pointsAnimation: Locator;

  // Achievement elements
  readonly achievementsGrid: Locator;
  readonly achievementBadges: Locator;
  readonly unlockedAchievements: Locator;
  readonly lockedAchievements: Locator;
  readonly achievementProgress: Locator;
  readonly achievementModal: Locator;
  readonly unlockAchievementButton: Locator;

  // Streak elements
  readonly streakCounters: Locator;
  readonly dailyLoginStreak: Locator;
  readonly focusSessionStreak: Locator;
  readonly goalCompletionStreak: Locator;
  readonly hiveParticipationStreak: Locator;
  readonly streakFlame: Locator;
  readonly updateStreakButton: Locator;

  // Leaderboard elements
  readonly leaderboards: Locator;
  readonly weeklyLeaderboard: Locator;
  readonly monthlyLeaderboard: Locator;
  readonly allTimeLeaderboard: Locator;
  readonly leaderboardEntries: Locator;
  readonly userRank: Locator;
  readonly rankChange: Locator;

  // Challenge elements
  readonly challengesList: Locator;
  readonly activeChallenges: Locator;
  readonly completedChallenges: Locator;
  readonly challengeProgress: Locator;
  readonly participateChallengeButton: Locator;
  readonly challengeRewards: Locator;

  // Goal elements
  readonly goalsList: Locator;
  readonly activeGoals: Locator;
  readonly completedGoals: Locator;
  readonly goalProgress: Locator;
  readonly createGoalButton: Locator;
  readonly goalDeadline: Locator;

  // Social features elements
  readonly socialPanel: Locator;
  readonly friendsList: Locator;
  readonly buddiesList: Locator;
  readonly teamsList: Locator;
  readonly mentorshipsList: Locator;
  readonly friendComparisonModal: Locator;
  readonly addFriendButton: Locator;

  // Filter and control elements
  readonly periodFilter: Locator;
  readonly categoryFilter: Locator;
  readonly sortBySelect: Locator;
  readonly viewModeToggle: Locator;

  // Notification elements
  readonly achievementNotification: Locator;
  readonly pointsNotification: Locator;
  readonly challengeNotification: Locator;
  readonly streakNotification: Locator;

  constructor(page: Page) {
    this.page = page;

    // Main dashboard elements
    this.dashboard = page.locator('[data-testid="gamification-dashboard"]');
    this.dashboardTitle = page.locator('h1:has-text("Gamification"), h3:has-text("FocusHive Gamification")');
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"], .MuiCircularProgress-root');
    this.errorAlert = page.locator('.MuiAlert-root[severity="error"]');
    this.retryButton = page.locator('button:has-text("Retry")');
    this.refreshButton = page.locator('button[aria-label="refresh gamification data"]');
    this.emptyStateMessage = page.locator(':has-text("No gamification data yet")');

    // Points Display elements
    this.pointsDisplay = page.locator('[data-testid="points-display"]');
    this.pointsCurrent = page.locator('[data-testid="points-current"]');
    this.pointsTotal = page.locator('[data-testid="points-total"]');
    this.pointsToday = page.locator('[data-testid="points-today"]');
    this.pointsWeek = page.locator('[data-testid="points-week"]');
    this.addPointsButton = page.locator('button:has-text("Add") >> :has-text("Points")');
    this.pointsAnimation = page.locator('.points-animation, [data-testid="points-animation"]');

    // Achievement elements
    this.achievementsGrid = page.locator('[data-testid="achievements-grid"]');
    this.achievementBadges = page.locator('[data-testid^="achievement-"]');
    this.unlockedAchievements = page.locator('[data-testid^="achievement-"].unlocked');
    this.lockedAchievements = page.locator('[data-testid^="achievement-"]:not(.unlocked)');
    this.achievementProgress = page.locator('[data-testid^="achievement-progress-"]');
    this.achievementModal = page.locator('[data-testid="achievement-modal"]');
    this.unlockAchievementButton = page.locator('button:has-text("Unlock")');

    // Streak elements
    this.streakCounters = page.locator('[data-testid^="streak-counter-"]');
    this.dailyLoginStreak = page.locator('[data-testid="streak-counter-daily_login"]');
    this.focusSessionStreak = page.locator('[data-testid="streak-counter-focus_session"]');
    this.goalCompletionStreak = page.locator('[data-testid="streak-counter-goal_completion"]');
    this.hiveParticipationStreak = page.locator('[data-testid="streak-counter-hive_participation"]');
    this.streakFlame = page.locator('.streak-flame, [data-testid="streak-flame"]');
    this.updateStreakButton = page.locator('button:has-text("Update") >> :has-text("Streak")');

    // Leaderboard elements
    this.leaderboards = page.locator('[data-testid="leaderboards"]');
    this.weeklyLeaderboard = page.locator('[data-testid="leaderboard-weekly"]');
    this.monthlyLeaderboard = page.locator('[data-testid="leaderboard-monthly"]');
    this.allTimeLeaderboard = page.locator('[data-testid="leaderboard-all-time"]');
    this.leaderboardEntries = page.locator('[data-testid^="leaderboard-entry-"]');
    this.userRank = page.locator('[data-testid="user-rank"]');
    this.rankChange = page.locator('[data-testid="rank-change"]');

    // Challenge elements
    this.challengesList = page.locator('[data-testid="challenges-list"]');
    this.activeChallenges = page.locator('[data-testid="active-challenges"]');
    this.completedChallenges = page.locator('[data-testid="completed-challenges"]');
    this.challengeProgress = page.locator('[data-testid^="challenge-progress-"]');
    this.participateChallengeButton = page.locator('[data-testid^="participate-challenge-"]');
    this.challengeRewards = page.locator('[data-testid^="challenge-rewards-"]');

    // Goal elements
    this.goalsList = page.locator('[data-testid="goals-list"]');
    this.activeGoals = page.locator('[data-testid="active-goals"]');
    this.completedGoals = page.locator('[data-testid="completed-goals"]');
    this.goalProgress = page.locator('[data-testid^="goal-progress-"]');
    this.createGoalButton = page.locator('[data-testid="create-goal-button"]');
    this.goalDeadline = page.locator('[data-testid^="goal-deadline-"]');

    // Social features elements
    this.socialPanel = page.locator('[data-testid="social-panel"]');
    this.friendsList = page.locator('[data-testid="friends-list"]');
    this.buddiesList = page.locator('[data-testid="buddies-list"]');
    this.teamsList = page.locator('[data-testid="teams-list"]');
    this.mentorshipsList = page.locator('[data-testid="mentorships-list"]');
    this.friendComparisonModal = page.locator('[data-testid="friend-comparison-modal"]');
    this.addFriendButton = page.locator('[data-testid="add-friend-button"]');

    // Filter and control elements
    this.periodFilter = page.locator('[data-testid="period-filter"]');
    this.categoryFilter = page.locator('[data-testid="category-filter"]');
    this.sortBySelect = page.locator('[data-testid="sort-by-select"]');
    this.viewModeToggle = page.locator('[data-testid="view-mode-toggle"]');

    // Notification elements
    this.achievementNotification = page.locator('[data-testid="achievement-unlocked-notification"]');
    this.pointsNotification = page.locator('[data-testid="points-earned-notification"]');
    this.challengeNotification = page.locator('[data-testid="challenge-participation-success"]');
    this.streakNotification = page.locator('[data-testid="streak-updated-notification"]');
  }

  /**
   * Navigate to gamification page
   */
  async goto(): Promise<void> {
    await this.page.goto('/gamification');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to gamification demo page
   */
  async gotoDemo(): Promise<void> {
    await this.page.goto('/gamification/demo');
    await this.waitForPageLoad();
  }

  /**
   * Wait for page to load completely
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout: TIMEOUTS.NETWORK });
    
    try {
      await Promise.race([
        this.dashboard.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.errorAlert.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.emptyStateMessage.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.page.locator('main, .MuiContainer-root, body').waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM })
      ]);
    } catch {
      await this.page.waitForLoadState('domcontentloaded');
    }
  }

  /**
   * Wait for gamification data to load
   */
  async waitForDataLoad(): Promise<void> {
    try {
      await this.loadingSpinner.waitFor({ state: 'hidden', timeout: TIMEOUTS.NETWORK });
    } catch {
      // Spinner might not be visible
    }

    await Promise.race([
      this.pointsDisplay.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
      this.emptyStateMessage.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT }),
      this.errorAlert.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT })
    ]);
  }

  /**
   * Verify dashboard is loaded successfully
   */
  async verifyDashboardLoaded(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.dashboardTitle).toBeVisible();
    await expect(this.loadingSpinner).not.toBeVisible();
  }

  /**
   * Verify empty state
   */
  async verifyEmptyState(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.emptyStateMessage).toBeVisible();
  }

  /**
   * Verify error state
   */
  async verifyErrorState(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.errorAlert).toBeVisible();
    await expect(this.retryButton).toBeVisible();
  }

  /**
   * Click refresh button
   */
  async clickRefresh(): Promise<void> {
    await this.refreshButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Click retry button
   */
  async clickRetry(): Promise<void> {
    await this.retryButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Get current points value
   */
  async getCurrentPoints(): Promise<number> {
    const element = this.pointsCurrent;
    const text = await element.textContent() || '0';
    return parseInt(text.replace(/[^\d]/g, ''));
  }

  /**
   * Get total points value
   */
  async getTotalPoints(): Promise<number> {
    const element = this.pointsTotal;
    const text = await element.textContent() || '0';
    return parseInt(text.replace(/[^\d]/g, ''));
  }

  /**
   * Add points by clicking button
   */
  async addPoints(amount: number = 50): Promise<void> {
    const initialPoints = await this.getCurrentPoints();
    await this.addPointsButton.click();
    
    // Wait for points animation
    await this.page.waitForTimeout(1000);
    
    // Verify points were added
    const newPoints = await this.getCurrentPoints();
    expect(newPoints).toBeGreaterThan(initialPoints);
  }

  /**
   * Get achievement count by status
   */
  async getAchievementCount(status: 'unlocked' | 'locked' | 'all' = 'all'): Promise<number> {
    let locator: Locator;
    switch (status) {
      case 'unlocked':
        locator = this.unlockedAchievements;
        break;
      case 'locked':
        locator = this.lockedAchievements;
        break;
      default:
        locator = this.achievementBadges;
    }
    return await locator.count();
  }

  /**
   * Click achievement to view details
   */
  async viewAchievement(achievementId: string): Promise<void> {
    const achievement = this.page.locator(`[data-testid="achievement-${achievementId}"]`);
    await achievement.click();
    await expect(this.achievementModal).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * Get streak value for specific type
   */
  async getStreakValue(streakType: 'daily_login' | 'focus_session' | 'goal_completion' | 'hive_participation'): Promise<{
    current: number;
    best: number;
    isActive: boolean;
  }> {
    const streakCounter = this.page.locator(`[data-testid="streak-counter-${streakType}"]`);
    
    const currentElement = streakCounter.locator('[data-testid="current-streak"]');
    const bestElement = streakCounter.locator('[data-testid="best-streak"]');
    const isActive = await streakCounter.locator('.active, [data-active="true"]').isVisible();
    
    const current = parseInt(await currentElement.textContent() || '0');
    const best = parseInt(await bestElement.textContent() || '0');
    
    return { current, best, isActive };
  }

  /**
   * Get leaderboard entries
   */
  async getLeaderboardEntries(leaderboardType: 'weekly' | 'monthly' | 'all-time' = 'weekly'): Promise<{
    rank: number;
    name: string;
    points: number;
    change?: number;
  }[]> {
    const leaderboard = this.page.locator(`[data-testid="leaderboard-${leaderboardType}"]`);
    const entries = leaderboard.locator('[data-testid^="leaderboard-entry-"]');
    const count = await entries.count();
    
    const results = [];
    for (let i = 0; i < count; i++) {
      const entry = entries.nth(i);
      const rank = parseInt(await entry.locator('[data-testid="entry-rank"]').textContent() || '0');
      const name = await entry.locator('[data-testid="entry-name"]').textContent() || '';
      const points = parseInt(await entry.locator('[data-testid="entry-points"]').textContent()?.replace(/[^\d]/g, '') || '0');
      
      const changeElement = entry.locator('[data-testid="entry-change"]');
      let change: number | undefined;
      if (await changeElement.isVisible()) {
        const changeText = await changeElement.textContent() || '';
        const changeValue = parseInt(changeText.replace(/[^\d-]/g, ''));
        if (!isNaN(changeValue)) {
          change = changeValue;
        }
      }
      
      results.push({ rank, name, points, change });
    }
    
    return results;
  }

  /**
   * Participate in a challenge
   */
  async participateInChallenge(challengeId: string): Promise<void> {
    const participateButton = this.page.locator(`[data-testid="participate-challenge-${challengeId}"]`);
    await participateButton.click();
    
    // Wait for participation confirmation
    await expect(this.challengeNotification).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * Get challenge progress
   */
  async getChallengeProgress(challengeId: string): Promise<{
    current: number;
    target: number;
    percentage: number;
    isCompleted: boolean;
  }> {
    const challenge = this.page.locator(`[data-testid="challenge-${challengeId}"]`);
    const progressElement = challenge.locator('[data-testid^="challenge-progress-"]');
    
    const current = parseInt(await progressElement.locator('[data-testid="progress-current"]').textContent() || '0');
    const target = parseInt(await progressElement.locator('[data-testid="progress-target"]').textContent() || '0');
    const percentage = target > 0 ? (current / target) * 100 : 0;
    const isCompleted = percentage >= 100;
    
    return { current, target, percentage, isCompleted };
  }

  /**
   * Create a new goal
   */
  async createGoal(goalData: {
    title: string;
    description: string;
    category: 'focus' | 'consistency' | 'collaboration' | 'personal';
    target: number;
    metric: 'minutes' | 'sessions' | 'days' | 'achievements';
    deadline?: string;
  }): Promise<void> {
    await this.createGoalButton.click();
    
    const modal = this.page.locator('[data-testid="create-goal-modal"]');
    await expect(modal).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    
    // Fill goal form
    await modal.locator('[data-testid="goal-title-input"]').fill(goalData.title);
    await modal.locator('[data-testid="goal-description-input"]').fill(goalData.description);
    await modal.locator('[data-testid="goal-category-select"]').selectOption(goalData.category);
    await modal.locator('[data-testid="goal-target-input"]').fill(goalData.target.toString());
    await modal.locator('[data-testid="goal-metric-select"]').selectOption(goalData.metric);
    
    if (goalData.deadline) {
      await modal.locator('[data-testid="goal-deadline-input"]').fill(goalData.deadline);
    }
    
    // Submit form
    await modal.locator('[data-testid="create-goal-submit"]').click();
    
    // Wait for modal to close
    await expect(modal).not.toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * View friend comparison
   */
  async viewFriendComparison(friendId: string): Promise<void> {
    const friend = this.page.locator(`[data-testid="friend-${friendId}"]`);
    await friend.click();
    
    await expect(this.friendComparisonModal).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * Filter content by period
   */
  async filterByPeriod(period: 'daily' | 'weekly' | 'monthly' | 'all-time'): Promise<void> {
    await this.periodFilter.click();
    await this.page.locator(`[data-value="${period}"]`).click();
    await this.waitForDataLoad();
  }

  /**
   * Filter content by category
   */
  async filterByCategory(category: 'focus' | 'collaboration' | 'consistency' | 'milestone' | 'special'): Promise<void> {
    await this.categoryFilter.click();
    await this.page.locator(`[data-value="${category}"]`).click();
    await this.waitForDataLoad();
  }

  /**
   * Toggle view mode
   */
  async toggleViewMode(): Promise<void> {
    await this.viewModeToggle.click();
    await this.page.waitForTimeout(500);
  }

  /**
   * Verify responsive design on mobile
   */
  async verifyMobileLayout(): Promise<void> {
    await this.page.setViewportSize({ width: 375, height: 667 });
    await this.waitForDataLoad();
    
    await expect(this.dashboard).toBeVisible();
    
    // Verify elements stack properly on mobile
    const pointsDisplay = this.pointsDisplay;
    await expect(pointsDisplay).toBeVisible();
    
    // Check if achievements grid adapts to mobile
    if (await this.achievementsGrid.isVisible()) {
      await expect(this.achievementsGrid).toHaveClass(/responsive/);
    }
  }

  /**
   * Verify accessibility features
   */
  async verifyAccessibility(): Promise<void> {
    // Test keyboard navigation
    await this.page.keyboard.press('Tab');
    const focusedElement = await this.page.evaluate(() => document.activeElement?.tagName);
    expect(['BUTTON', 'A', 'INPUT', 'SELECT']).toContain(focusedElement);

    // Test ARIA labels
    const pointsDisplay = this.pointsDisplay;
    if (await pointsDisplay.isVisible()) {
      const ariaLabel = await pointsDisplay.getAttribute('aria-label');
      expect(ariaLabel).toBeTruthy();
    }

    // Test achievement accessibility
    const achievements = this.achievementBadges;
    const count = await achievements.count();
    if (count > 0) {
      const firstAchievement = achievements.first();
      const achievementAriaLabel = await firstAchievement.getAttribute('aria-label');
      expect(achievementAriaLabel).toBeTruthy();
    }

    // Test screen reader content
    const screenReaderElements = this.page.locator('[aria-live], .sr-only');
    expect(await screenReaderElements.count()).toBeGreaterThan(0);
  }

  /**
   * Measure page load performance
   */
  async measureLoadPerformance(): Promise<{
    loadTime: number;
    firstContentfulPaint: number;
    domContentLoaded: number;
  }> {
    const startTime = Date.now();
    
    await this.goto();
    await this.waitForDataLoad();
    
    const endTime = Date.now();
    const loadTime = endTime - startTime;
    
    const performanceMetrics = await this.page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0,
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      };
    });
    
    return {
      loadTime,
      firstContentfulPaint: performanceMetrics.firstContentfulPaint,
      domContentLoaded: performanceMetrics.domContentLoaded,
    };
  }

  /**
   * Verify real-time updates work
   */
  async verifyRealTimeUpdates(): Promise<void> {
    const initialPoints = await this.getCurrentPoints();
    
    // Trigger refresh or real-time update
    await this.clickRefresh();
    
    // Wait for potential updates
    await this.page.waitForTimeout(2000);
    
    // Verify that data can be refreshed
    await expect(this.pointsDisplay).toBeVisible();
  }
}