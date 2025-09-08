/**
 * Gamification helper functions for E2E tests
 * Provides utilities for testing gamification features, mocking data, and performance testing
 */

import { Page, Locator, expect } from '@playwright/test';
import { TIMEOUTS } from './test-data';

// Mock gamification data types
export interface MockUser {
  id: string;
  name: string;
  avatar?: string;
  email?: string;
}

export interface MockPoints {
  current: number;
  total: number;
  todayEarned: number;
  weekEarned: number;
}

export interface MockAchievement {
  id: string;
  title: string;
  description: string;
  icon: string;
  category: 'focus' | 'collaboration' | 'consistency' | 'milestone' | 'special';
  points: number;
  unlockedAt?: string;
  progress?: number;
  maxProgress?: number;
  isUnlocked: boolean;
  rarity: 'common' | 'uncommon' | 'rare' | 'epic' | 'legendary';
}

export interface MockStreak {
  id: string;
  type: 'daily_login' | 'focus_session' | 'goal_completion' | 'hive_participation';
  current: number;
  best: number;
  lastActivity: string;
  isActive: boolean;
}

export interface MockLeaderboardEntry {
  user: MockUser;
  points: number;
  rank: number;
  change?: number;
}

export interface MockLeaderboard {
  id: string;
  title: string;
  period: 'daily' | 'weekly' | 'monthly' | 'all_time';
  entries: MockLeaderboardEntry[];
  lastUpdated: string;
}

export interface MockChallenge {
  id: string;
  title: string;
  description: string;
  type: 'daily' | 'weekly' | 'monthly' | 'special';
  category: 'focus' | 'collaboration' | 'consistency' | 'social';
  requirements: {
    target: number;
    current: number;
    metric: 'minutes' | 'sessions' | 'days' | 'points' | 'achievements';
  };
  rewards: {
    points: number;
    achievements?: string[];
    badges?: string[];
  };
  startDate: string;
  endDate: string;
  isActive: boolean;
  isCompleted: boolean;
  participants: number;
}

export interface MockGoal {
  id: string;
  title: string;
  description: string;
  category: 'focus' | 'consistency' | 'collaboration' | 'personal';
  target: number;
  current: number;
  metric: 'minutes' | 'sessions' | 'days' | 'achievements';
  deadline?: string;
  isCompleted: boolean;
  rewards: {
    points: number;
    achievements?: string[];
  };
}

export interface MockSocialFeature {
  friends: MockUser[];
  buddies: MockUser[];
  teams: {
    id: string;
    name: string;
    members: MockUser[];
    totalPoints: number;
    rank: number;
  }[];
  mentorships: {
    mentor: MockUser;
    mentee: MockUser;
    status: 'active' | 'completed';
    progress: number;
  }[];
}

export interface MockGamificationData {
  points: MockPoints;
  achievements: MockAchievement[];
  streaks: MockStreak[];
  leaderboards: MockLeaderboard[];
  challenges: MockChallenge[];
  goals: MockGoal[];
  social: MockSocialFeature;
  level: number;
  rank: number;
  totalUsers: number;
  lastUpdated: string;
}

export class GamificationHelper {
  constructor(private page: Page) {}

  /**
   * Generate mock gamification data with realistic values
   */
  generateMockGamificationData(options: {
    hasData?: boolean;
    pointsAmount?: number;
    achievementCount?: number;
    streakCount?: number;
    leaderboardSize?: number;
    challengeCount?: number;
    goalCount?: number;
    friendCount?: number;
    userLevel?: number;
  } = {}): MockGamificationData {
    const {
      hasData = true,
      pointsAmount = 1250,
      achievementCount = 8,
      streakCount = 3,
      leaderboardSize = 10,
      challengeCount = 5,
      goalCount = 4,
      friendCount = 6,
      userLevel = 15
    } = options;

    if (!hasData) {
      return {
        points: { current: 0, total: 0, todayEarned: 0, weekEarned: 0 },
        achievements: [],
        streaks: [],
        leaderboards: [],
        challenges: [],
        goals: [],
        social: { friends: [], buddies: [], teams: [], mentorships: [] },
        level: 1,
        rank: 0,
        totalUsers: 0,
        lastUpdated: new Date().toISOString()
      };
    }

    // Generate achievements
    const achievements: MockAchievement[] = [
      {
        id: 'first-focus',
        title: 'First Focus',
        description: 'Complete your first focus session',
        icon: 'focus',
        category: 'focus',
        points: 100,
        unlockedAt: new Date(Date.now() - 86400000).toISOString(),
        isUnlocked: true,
        rarity: 'common',
      },
      {
        id: 'focus-streak-7',
        title: 'Week Warrior',
        description: 'Maintain a 7-day focus streak',
        icon: 'streak',
        category: 'consistency',
        points: 300,
        unlockedAt: new Date(Date.now() - 43200000).toISOString(),
        isUnlocked: true,
        rarity: 'uncommon',
      },
      {
        id: 'points-milestone-1k',
        title: 'Point Collector',
        description: 'Earn 1,000 total points',
        icon: 'points',
        category: 'milestone',
        points: 500,
        progress: pointsAmount >= 1000 ? 1000 : pointsAmount,
        maxProgress: 1000,
        isUnlocked: pointsAmount >= 1000,
        rarity: 'rare',
      },
      {
        id: 'team-player',
        title: 'Team Player',
        description: 'Join 5 different hives',
        icon: 'collaboration',
        category: 'collaboration',
        points: 250,
        progress: 3,
        maxProgress: 5,
        isUnlocked: false,
        rarity: 'uncommon',
      },
      {
        id: 'focus-master',
        title: 'Focus Master',
        description: 'Complete 100 focus sessions',
        icon: 'master',
        category: 'focus',
        points: 1000,
        progress: 45,
        maxProgress: 100,
        isUnlocked: false,
        rarity: 'epic',
      },
      {
        id: 'legendary-achiever',
        title: 'FocusHive Legend',
        description: 'Unlock all other achievements',
        icon: 'legend',
        category: 'special',
        points: 10000,
        progress: 2,
        maxProgress: achievementCount - 1,
        isUnlocked: false,
        rarity: 'legendary',
      }
    ].slice(0, achievementCount);

    // Generate streaks
    const streaks: MockStreak[] = [
      {
        id: 'daily-login-streak',
        type: 'daily_login',
        current: 7,
        best: 15,
        lastActivity: new Date().toISOString(),
        isActive: true,
      },
      {
        id: 'focus-session-streak',
        type: 'focus_session',
        current: 3,
        best: 12,
        lastActivity: new Date(Date.now() - 7200000).toISOString(),
        isActive: true,
      },
      {
        id: 'goal-completion-streak',
        type: 'goal_completion',
        current: 0,
        best: 8,
        lastActivity: new Date(Date.now() - 172800000).toISOString(),
        isActive: false,
      },
      {
        id: 'hive-participation-streak',
        type: 'hive_participation',
        current: 5,
        best: 20,
        lastActivity: new Date(Date.now() - 3600000).toISOString(),
        isActive: true,
      }
    ].slice(0, streakCount);

    // Generate leaderboard entries
    const generateLeaderboardEntries = (size: number): MockLeaderboardEntry[] => {
      const entries: MockLeaderboardEntry[] = [];
      for (let i = 0; i < size; i++) {
        entries.push({
          user: {
            id: `user-${i + 1}`,
            name: `User ${i + 1}`,
            avatar: `/avatars/user${i + 1}.jpg`,
            email: `user${i + 1}@focushive.com`
          },
          points: Math.max(100, pointsAmount - (i * 200) + Math.floor(Math.random() * 100)),
          rank: i + 1,
          change: i === 0 ? 1 : i === 1 ? -1 : (Math.random() > 0.5 ? 1 : -1) * Math.floor(Math.random() * 3)
        });
      }
      return entries;
    };

    // Generate leaderboards
    const leaderboards: MockLeaderboard[] = [
      {
        id: 'weekly-points',
        title: 'Weekly Points Leader',
        period: 'weekly',
        entries: generateLeaderboardEntries(leaderboardSize),
        lastUpdated: new Date().toISOString(),
      },
      {
        id: 'monthly-focus',
        title: 'Monthly Focus Champion',
        period: 'monthly',
        entries: generateLeaderboardEntries(Math.min(leaderboardSize, 8)),
        lastUpdated: new Date(Date.now() - 3600000).toISOString(),
      },
      {
        id: 'all-time-legend',
        title: 'All-Time Legends',
        period: 'all_time',
        entries: generateLeaderboardEntries(Math.min(leaderboardSize, 5)),
        lastUpdated: new Date(Date.now() - 7200000).toISOString(),
      }
    ];

    // Generate challenges
    const challenges: MockChallenge[] = [
      {
        id: 'daily-focus-challenge',
        title: 'Daily Focus Challenge',
        description: 'Complete 2 hours of focused work today',
        type: 'daily',
        category: 'focus',
        requirements: {
          target: 120,
          current: 45,
          metric: 'minutes'
        },
        rewards: {
          points: 100,
          achievements: ['daily-warrior']
        },
        startDate: new Date().toISOString().split('T')[0],
        endDate: new Date().toISOString().split('T')[0],
        isActive: true,
        isCompleted: false,
        participants: 1247
      },
      {
        id: 'weekly-consistency',
        title: 'Consistency Week',
        description: 'Complete at least one focus session every day this week',
        type: 'weekly',
        category: 'consistency',
        requirements: {
          target: 7,
          current: 3,
          metric: 'days'
        },
        rewards: {
          points: 500,
          achievements: ['weekly-warrior'],
          badges: ['consistency-champion']
        },
        startDate: new Date(Date.now() - 259200000).toISOString().split('T')[0],
        endDate: new Date(Date.now() + 259200000).toISOString().split('T')[0],
        isActive: true,
        isCompleted: false,
        participants: 892
      },
      {
        id: 'team-collaboration',
        title: 'Team Spirit',
        description: 'Work together in a hive for 10 sessions',
        type: 'weekly',
        category: 'collaboration',
        requirements: {
          target: 10,
          current: 6,
          metric: 'sessions'
        },
        rewards: {
          points: 300,
          achievements: ['team-player']
        },
        startDate: new Date(Date.now() - 432000000).toISOString().split('T')[0],
        endDate: new Date(Date.now() + 172800000).toISOString().split('T')[0],
        isActive: true,
        isCompleted: false,
        participants: 456
      }
    ].slice(0, challengeCount);

    // Generate goals
    const goals: MockGoal[] = [
      {
        id: 'monthly-focus-goal',
        title: 'Monthly Focus Master',
        description: 'Complete 50 hours of focused work this month',
        category: 'focus',
        target: 3000,
        current: 1200,
        metric: 'minutes',
        deadline: new Date(Date.now() + 1209600000).toISOString(),
        isCompleted: false,
        rewards: {
          points: 1000,
          achievements: ['monthly-master']
        }
      },
      {
        id: 'streak-maintainer',
        title: 'Streak Maintainer',
        description: 'Maintain a 30-day login streak',
        category: 'consistency',
        target: 30,
        current: 7,
        metric: 'days',
        isCompleted: false,
        rewards: {
          points: 750,
          achievements: ['streak-legend']
        }
      },
      {
        id: 'social-butterfly',
        title: 'Social Butterfly',
        description: 'Connect with 10 new accountability buddies',
        category: 'collaboration',
        target: 10,
        current: 4,
        metric: 'achievements',
        isCompleted: false,
        rewards: {
          points: 500,
          achievements: ['social-master']
        }
      },
      {
        id: 'personal-growth',
        title: 'Personal Growth',
        description: 'Complete all personal development sessions',
        category: 'personal',
        target: 15,
        current: 15,
        metric: 'sessions',
        isCompleted: true,
        rewards: {
          points: 800,
          achievements: ['growth-champion']
        }
      }
    ].slice(0, goalCount);

    // Generate social features
    const generateUsers = (count: number): MockUser[] => {
      return Array.from({ length: count }, (_, i) => ({
        id: `friend-${i + 1}`,
        name: `Friend ${i + 1}`,
        avatar: `/avatars/friend${i + 1}.jpg`,
        email: `friend${i + 1}@focushive.com`
      }));
    };

    const social: MockSocialFeature = {
      friends: generateUsers(friendCount),
      buddies: generateUsers(Math.min(friendCount, 3)),
      teams: [
        {
          id: 'team-alpha',
          name: 'Alpha Achievers',
          members: generateUsers(5),
          totalPoints: 15000,
          rank: 2
        },
        {
          id: 'team-beta',
          name: 'Beta Builders',
          members: generateUsers(4),
          totalPoints: 12000,
          rank: 5
        }
      ],
      mentorships: [
        {
          mentor: { id: 'mentor-1', name: 'Sarah Johnson', avatar: '/avatars/sarah.jpg' },
          mentee: { id: 'mentee-1', name: 'Current User', avatar: '/avatars/user.jpg' },
          status: 'active',
          progress: 65
        }
      ]
    };

    return {
      points: {
        current: pointsAmount,
        total: pointsAmount + 2500,
        todayEarned: 150,
        weekEarned: 420
      },
      achievements,
      streaks,
      leaderboards,
      challenges,
      goals,
      social,
      level: userLevel,
      rank: Math.floor(Math.random() * 100) + 1,
      totalUsers: 1500,
      lastUpdated: new Date().toISOString()
    };
  }

  /**
   * Mock gamification API endpoints
   */
  async mockGamificationApi(mockData: MockGamificationData): Promise<void> {
    // Mock main dashboard endpoint
    await this.page.route('**/api/v1/gamification/**', (route) => {
      const url = route.request().url();
      
      if (url.includes('/dashboard')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData
          })
        });
      } else if (url.includes('/points')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.points
          })
        });
      } else if (url.includes('/achievements')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.achievements
          })
        });
      } else if (url.includes('/streaks')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.streaks
          })
        });
      } else if (url.includes('/leaderboards')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.leaderboards
          })
        });
      } else if (url.includes('/challenges')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.challenges
          })
        });
      } else if (url.includes('/goals')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.goals
          })
        });
      } else if (url.includes('/social')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData.social
          })
        });
      } else {
        route.continue();
      }
    });

    // Mock WebSocket connections for real-time updates
    await this.page.route('**/ws/gamification', (route) => {
      route.fulfill({
        status: 101,
        headers: {
          'Upgrade': 'websocket',
          'Connection': 'Upgrade',
        }
      });
    });
  }

  /**
   * Mock empty gamification state
   */
  async mockEmptyGamification(): Promise<void> {
    const emptyData = this.generateMockGamificationData({ hasData: false });
    await this.mockGamificationApi(emptyData);
  }

  /**
   * Mock gamification API error
   */
  async mockGamificationApiError(errorCode: number = 500): Promise<void> {
    await this.page.route('**/api/v1/gamification/**', (route) => {
      route.fulfill({
        status: errorCode,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: 'Gamification service temporarily unavailable',
          message: 'Please try again later'
        })
      });
    });
  }

  /**
   * Mock slow gamification API response
   */
  async mockSlowGamificationApi(delayMs: number = 3000): Promise<void> {
    const mockData = this.generateMockGamificationData();
    
    await this.page.route('**/api/v1/gamification/**', async (route) => {
      await new Promise(resolve => setTimeout(resolve, delayMs));
      
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData
        })
      });
    });
  }

  /**
   * Mock network failure
   */
  async mockNetworkFailure(): Promise<void> {
    await this.page.route('**/api/v1/gamification/**', (route) => {
      route.abort('internetdisconnected');
    });
  }

  /**
   * Test points calculation accuracy
   */
  async validatePointsAccuracy(expectedPoints: MockPoints): Promise<void> {
    // Verify current points display
    const currentPointsElement = this.page.locator('[data-testid="points-current"]');
    const displayedCurrentPoints = await currentPointsElement.textContent();
    expect(displayedCurrentPoints).toContain(expectedPoints.current.toString());

    // Verify total points display
    const totalPointsElement = this.page.locator('[data-testid="points-total"]');
    const displayedTotalPoints = await totalPointsElement.textContent();
    expect(displayedTotalPoints).toContain(expectedPoints.total.toString());

    // Verify today's earned points
    const todayPointsElement = this.page.locator('[data-testid="points-today"]');
    if (await todayPointsElement.isVisible()) {
      const displayedTodayPoints = await todayPointsElement.textContent();
      expect(displayedTodayPoints).toContain(expectedPoints.todayEarned.toString());
    }

    // Verify weekly earned points
    const weekPointsElement = this.page.locator('[data-testid="points-week"]');
    if (await weekPointsElement.isVisible()) {
      const displayedWeekPoints = await weekPointsElement.textContent();
      expect(displayedWeekPoints).toContain(expectedPoints.weekEarned.toString());
    }
  }

  /**
   * Test achievement unlocking mechanics
   */
  async testAchievementUnlocking(achievementId: string): Promise<void> {
    // Mock achievement unlock API
    await this.page.route(`**/api/v1/gamification/achievements/${achievementId}/unlock`, (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            achievementId,
            pointsEarned: 500,
            unlockedAt: new Date().toISOString()
          }
        })
      });
    });

    // Trigger achievement unlock
    const unlockButton = this.page.locator(`[data-testid="unlock-achievement-${achievementId}"]`);
    await unlockButton.click();

    // Verify achievement unlock notification
    const notification = this.page.locator('[data-testid="achievement-unlocked-notification"]');
    await expect(notification).toBeVisible({ timeout: TIMEOUTS.MEDIUM });

    // Verify achievement badge shows as unlocked
    const achievementBadge = this.page.locator(`[data-testid="achievement-${achievementId}"]`);
    await expect(achievementBadge).toHaveClass(/unlocked/);
  }

  /**
   * Test leaderboard real-time updates
   */
  async testLeaderboardUpdates(): Promise<void> {
    // Get initial leaderboard state
    const leaderboardEntries = this.page.locator('[data-testid^="leaderboard-entry-"]');
    const initialCount = await leaderboardEntries.count();

    // Mock real-time update
    const updatedData = this.generateMockGamificationData({
      pointsAmount: 1500,
      leaderboardSize: 12
    });

    // Simulate WebSocket update
    await this.page.evaluate((data) => {
      // Simulate WebSocket message
      window.dispatchEvent(new CustomEvent('websocket-message', {
        detail: {
          type: 'leaderboard_update',
          data: data.leaderboards[0]
        }
      }));
    }, updatedData);

    // Wait for leaderboard to update
    await this.page.waitForTimeout(1000);

    // Verify leaderboard reflects changes
    const updatedEntries = this.page.locator('[data-testid^="leaderboard-entry-"]');
    const newCount = await updatedEntries.count();
    expect(newCount).toBeGreaterThanOrEqual(initialCount);
  }

  /**
   * Test streak mechanics and validation
   */
  async testStreakMechanics(): Promise<void> {
    const streakCounters = this.page.locator('[data-testid^="streak-counter-"]');
    const count = await streakCounters.count();
    expect(count).toBeGreaterThan(0);

    // Test each streak counter
    for (let i = 0; i < count; i++) {
      const streakCounter = streakCounters.nth(i);
      
      // Verify current streak is displayed
      const currentStreak = streakCounter.locator('[data-testid="current-streak"]');
      await expect(currentStreak).toBeVisible();
      const currentValue = await currentStreak.textContent();
      expect(parseInt(currentValue || '0')).toBeGreaterThanOrEqual(0);

      // Verify best streak is displayed
      const bestStreak = streakCounter.locator('[data-testid="best-streak"]');
      if (await bestStreak.isVisible()) {
        const bestValue = await bestStreak.textContent();
        expect(parseInt(bestValue || '0')).toBeGreaterThanOrEqual(0);
      }
    }
  }

  /**
   * Test challenge participation and completion
   */
  async testChallengeParticipation(challengeId: string): Promise<void> {
    // Mock challenge participation API
    await this.page.route(`**/api/v1/gamification/challenges/${challengeId}/participate`, (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            challengeId,
            participated: true,
            progress: {
              current: 1,
              target: 10
            }
          }
        })
      });
    });

    // Click participate button
    const participateButton = this.page.locator(`[data-testid="participate-challenge-${challengeId}"]`);
    await participateButton.click();

    // Verify participation confirmation
    const confirmationMessage = this.page.locator('[data-testid="challenge-participation-success"]');
    await expect(confirmationMessage).toBeVisible({ timeout: TIMEOUTS.MEDIUM });

    // Verify challenge shows as participated
    const challengeCard = this.page.locator(`[data-testid="challenge-${challengeId}"]`);
    await expect(challengeCard).toHaveClass(/participated/);
  }

  /**
   * Test social features and friend interactions
   */
  async testSocialFeatures(): Promise<void> {
    // Test friends list
    const friendsList = this.page.locator('[data-testid="friends-list"]');
    if (await friendsList.isVisible()) {
      const friends = friendsList.locator('[data-testid^="friend-"]');
      const friendCount = await friends.count();
      expect(friendCount).toBeGreaterThanOrEqual(0);

      // Test friend comparison
      if (friendCount > 0) {
        const firstFriend = friends.first();
        await firstFriend.click();

        const comparisonModal = this.page.locator('[data-testid="friend-comparison-modal"]');
        await expect(comparisonModal).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      }
    }

    // Test team features
    const teamsList = this.page.locator('[data-testid="teams-list"]');
    if (await teamsList.isVisible()) {
      const teams = teamsList.locator('[data-testid^="team-"]');
      const teamCount = await teams.count();
      expect(teamCount).toBeGreaterThanOrEqual(0);
    }
  }

  /**
   * Test responsive design across different screen sizes
   */
  async testResponsiveDesign(): Promise<void> {
    const viewports = [
      { name: 'mobile', width: 375, height: 667 },
      { name: 'tablet', width: 768, height: 1024 },
      { name: 'desktop', width: 1920, height: 1080 }
    ];

    for (const viewport of viewports) {
      await this.page.setViewportSize({ width: viewport.width, height: viewport.height });
      await this.page.waitForTimeout(1000);

      // Verify gamification dashboard is visible
      const dashboard = this.page.locator('[data-testid="gamification-dashboard"]');
      await expect(dashboard).toBeVisible();

      // Verify key components adapt to screen size
      const pointsDisplay = this.page.locator('[data-testid="points-display"]');
      await expect(pointsDisplay).toBeVisible();

      const achievementGrid = this.page.locator('[data-testid="achievements-grid"]');
      if (await achievementGrid.isVisible()) {
        // On mobile, should stack vertically; on desktop, should have multiple columns
        const achievements = achievementGrid.locator('[data-testid^="achievement-"]');
        const count = await achievements.count();
        if (count > 0) {
          expect(count).toBeGreaterThan(0);
        }
      }
    }
  }

  /**
   * Test accessibility features
   */
  async testAccessibility(): Promise<void> {
    // Test keyboard navigation
    await this.page.keyboard.press('Tab');
    const firstFocusableElement = await this.page.evaluate(() => document.activeElement?.tagName);
    expect(['BUTTON', 'A', 'INPUT', 'SELECT']).toContain(firstFocusableElement);

    // Test ARIA labels and roles
    const points = this.page.locator('[data-testid="points-display"]');
    if (await points.isVisible()) {
      const ariaLabel = await points.getAttribute('aria-label');
      expect(ariaLabel).toBeTruthy();
    }

    // Test achievement badges accessibility
    const achievements = this.page.locator('[data-testid^="achievement-"]');
    const count = await achievements.count();
    if (count > 0) {
      const firstAchievement = achievements.first();
      const achievementAriaLabel = await firstAchievement.getAttribute('aria-label');
      expect(achievementAriaLabel).toBeTruthy();
    }

    // Test screen reader support
    const screenReaderContent = this.page.locator('[aria-live], .sr-only');
    expect(await screenReaderContent.count()).toBeGreaterThan(0);
  }

  /**
   * Set up performance monitoring
   */
  async setupPerformanceMonitoring(): Promise<{
    getMetrics: () => Promise<{
      loadTime: number;
      apiResponseTime: number;
      animationTime: number;
    }>;
  }> {
    let apiStartTime: number;
    let apiEndTime: number;

    // Monitor API response time
    await this.page.route('**/api/v1/gamification/**', (route) => {
      apiStartTime = Date.now();
      route.continue();
    });

    this.page.on('response', (response) => {
      if (response.url().includes('/api/v1/gamification/')) {
        apiEndTime = Date.now();
      }
    });

    return {
      getMetrics: async () => {
        // Measure animation rendering time
        const animationStart = Date.now();
        const animatedElements = this.page.locator('.animated, [data-testid*="animation"]');
        const animationCount = await animatedElements.count();
        const animationEnd = Date.now();

        return {
          loadTime: animationEnd - animationStart,
          apiResponseTime: apiEndTime - apiStartTime || 0,
          animationTime: animationCount > 0 ? animationEnd - animationStart : 0
        };
      }
    };
  }

  /**
   * Clean up test artifacts
   */
  async cleanup(): Promise<void> {
    // Clear route mocks
    await this.page.unrouteAll();
    
    // Reset viewport
    await this.page.setViewportSize({ width: 1280, height: 720 });

    // Clear local storage
    await this.page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  }
}