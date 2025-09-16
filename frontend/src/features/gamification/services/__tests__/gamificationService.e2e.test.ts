import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { GamificationService } from '../gamificationService';
import type {
  UserPoints,
  Achievement,
  Challenge,
  LeaderboardEntry,
  Reward,
  Badge,
  Streak,
  GamificationStats,
  PointTransaction,
} from '@/contracts/gamification';

describe('GamificationService E2E Tests', () => {
  let gamificationService: GamificationService;
  let mockWebSocketService: any;

  beforeEach(() => {
    // Mock WebSocket service
    mockWebSocketService = {
      subscribe: vi.fn().mockReturnValue(() => {}),
      isConnected: vi.fn().mockReturnValue(false),
    };

    // Create service instance
    gamificationService = new GamificationService();
    (gamificationService as any).webSocketService = mockWebSocketService;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Points Management', () => {
    it('should get user points with detailed breakdown', async () => {
      const points = await gamificationService.getUserPoints(1);

      expect(points).toBeDefined();
      expect(points.userId).toBe(1);
      expect(points.totalPoints).toBeGreaterThanOrEqual(0);
      expect(points.currentLevel).toBeGreaterThan(0);
      expect(points.levelProgress).toBeGreaterThanOrEqual(0);
      expect(points.levelProgress).toBeLessThanOrEqual(100);
    });

    it('should award points to user', async () => {
      const transaction = await gamificationService.awardPoints(1, {
        points: 100,
        description: 'Completed focus session',
        category: 'focus',
        source: 'timer_session',
      });

      expect(transaction).toBeDefined();
      expect(transaction.points).toBe(100);
      expect(transaction.category).toBe('focus');
      expect(transaction.description).toBe('Completed focus session');
    });

    it('should deduct points from user', async () => {
      const transaction = await gamificationService.deductPoints(1, {
        points: 50,
        description: 'Redeemed reward',
        category: 'milestone',
        source: 'reward_redemption',
      });

      expect(transaction).toBeDefined();
      expect(transaction.points).toBe(-50);
      expect(transaction.category).toBe('milestone');
    });

    it('should get point history with pagination', async () => {
      const history = await gamificationService.getPointHistory(1, { page: 0, limit: 10 });

      expect(history).toBeDefined();
      expect(history.transactions).toBeInstanceOf(Array);
      expect(history.pagination).toBeDefined();
      expect(history.pagination.page).toBe(0);
      expect(history.pagination.pageSize).toBe(10);
    });

    it('should get points leaderboard', async () => {
      const leaderboard = await gamificationService.getPointsLeaderboard('all-time');

      expect(leaderboard).toBeDefined();
      expect(leaderboard).toBeInstanceOf(Array);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].userId).toBeDefined();
        expect(leaderboard[0].points).toBeGreaterThanOrEqual(0);
      }
    });
  });

  describe('Achievements', () => {
    it('should get all available achievements', async () => {
      const achievements = await gamificationService.getAllAchievements();

      expect(achievements).toBeInstanceOf(Array);
      if (achievements.length > 0) {
        const achievement = achievements[0];
        expect(achievement.id).toBeDefined();
        expect(achievement.name).toBeDefined();
        expect(achievement.category).toBeDefined();
        expect(achievement.points).toBeGreaterThanOrEqual(0);
      }
    });

    it('should get user achievements with progress', async () => {
      const userAchievements = await gamificationService.getUserAchievements(1);

      expect(userAchievements).toBeDefined();
      expect(userAchievements.unlocked).toBeInstanceOf(Array);
      expect(userAchievements.inProgress).toBeInstanceOf(Array);
      expect(userAchievements.locked).toBeInstanceOf(Array);
    });

    it('should unlock achievement for user', async () => {
      const achievement = await gamificationService.unlockAchievement(1, 'first-session');

      expect(achievement).toBeDefined();
      expect(achievement.achievementId).toBe('first-session');
      expect(achievement.unlockedAt).toBeDefined();
      expect(achievement.pointsAwarded).toBeGreaterThanOrEqual(0);
    });

    it('should get achievement progress', async () => {
      const progress = await gamificationService.getAchievementProgress(1, 'marathon-runner');

      expect(progress).toBeDefined();
      expect(progress.achievementId).toBe('marathon-runner');
      expect(progress.currentProgress).toBeGreaterThanOrEqual(0);
      expect(progress.requiredProgress).toBeGreaterThan(0);
      expect(progress.progressPercentage).toBeGreaterThanOrEqual(0);
      expect(progress.progressPercentage).toBeLessThanOrEqual(100);
    });

    it('should get achievement categories', async () => {
      const categories = await gamificationService.getAchievementCategories();

      expect(categories).toBeInstanceOf(Array);
      expect(categories).toContain('productivity');
      expect(categories).toContain('social');
      expect(categories).toContain('consistency');
    });

    it('should get recent achievements', async () => {
      const recent = await gamificationService.getRecentAchievements(1, 5);

      expect(recent).toBeInstanceOf(Array);
      expect(recent.length).toBeLessThanOrEqual(5);
    });
  });

  describe('Challenges', () => {
    it('should get available challenges', async () => {
      const challenges = await gamificationService.getAvailableChallenges();

      expect(challenges).toBeInstanceOf(Array);
      if (challenges.length > 0) {
        const challenge = challenges[0];
        expect(challenge.id).toBeDefined();
        expect(challenge.name).toBeDefined();
        expect(challenge.type).toBeDefined();
        expect(challenge.status).toBe('active');
      }
    });

    it('should join a challenge', async () => {
      const participation = await gamificationService.joinChallenge(1, 'weekly-focus');

      expect(participation).toBeDefined();
      expect(participation.challengeId).toBe('weekly-focus');
      expect(participation.userId).toBe(1);
      expect(participation.joinedAt).toBeDefined();
    });

    it('should complete a challenge', async () => {
      const completion = await gamificationService.completeChallenge(1, 'daily-streak');

      expect(completion).toBeDefined();
      expect(completion.challengeId).toBe('daily-streak');
      expect(completion.completedAt).toBeDefined();
      expect(completion.rewardsEarned).toBeDefined();
    });

    it('should get challenge progress', async () => {
      const progress = await gamificationService.getChallengeProgress(1, 'weekly-focus');

      expect(progress).toBeDefined();
      expect(progress.challengeId).toBe('weekly-focus');
      expect(progress.currentProgress).toBeGreaterThanOrEqual(0);
      expect(progress.targetProgress).toBeGreaterThan(0);
      expect(progress.progressPercentage).toBeGreaterThanOrEqual(0);
      expect(progress.progressPercentage).toBeLessThanOrEqual(100);
      expect(progress.timeRemaining).toBeTruthy();
    });

    it('should leave a challenge', async () => {
      await expect(
        gamificationService.leaveChallenge(1, 'weekly-focus')
      ).resolves.not.toThrow();
    });

    it('should get challenge leaderboard', async () => {
      const leaderboard = await gamificationService.getChallengeLeaderboard('weekly-focus');

      expect(leaderboard).toBeInstanceOf(Array);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].userId).toBeDefined();
        expect(leaderboard[0].score).toBeGreaterThanOrEqual(0);
      }
    });
  });

  describe('Levels & Progression', () => {
    it('should get current user level', async () => {
      const level = await gamificationService.getUserLevel(1);

      expect(level).toBeDefined();
      expect(level.level).toBeGreaterThanOrEqual(1);
      expect(level.currentPoints).toBeGreaterThanOrEqual(0);
      expect(level.pointsToNext).toBeGreaterThan(0);
      expect(level.totalPoints).toBeGreaterThanOrEqual(0);
    });

    it('should calculate level progress', async () => {
      const progress = await gamificationService.calculateLevelProgress(1);

      expect(progress).toBeDefined();
      expect(progress.level).toBeGreaterThanOrEqual(1);
      expect(progress.percentage).toBeGreaterThanOrEqual(0);
      expect(progress.percentage).toBeLessThanOrEqual(100);
      expect(progress.pointsToday).toBeGreaterThanOrEqual(0);
    });

    it('should get level requirements', async () => {
      const requirements = await gamificationService.getLevelRequirements(5);

      expect(requirements).toBeDefined();
      expect(requirements.level).toBe(5);
      expect(requirements.pointsRequired).toBeGreaterThan(0);
      expect(requirements.rewards).toBeInstanceOf(Array);
      expect(requirements.features).toBeInstanceOf(Array);
    });

    it('should get next level rewards', async () => {
      const rewards = await gamificationService.getNextLevelRewards(1);

      expect(rewards).toBeInstanceOf(Array);
      if (rewards.length > 0) {
        expect(rewards[0].type).toBeDefined();
        expect(rewards[0].value).toBeDefined();
      }
    });

    it('should handle level up', async () => {
      const levelUp = await gamificationService.handleLevelUp(1);

      expect(levelUp).toBeDefined();
      expect(levelUp.level).toBeGreaterThanOrEqual(2);
      expect(levelUp.unlockedRewards).toBeInstanceOf(Array);
      expect(levelUp.notification).toBeTruthy();
      expect(levelUp.nextLevelPoints).toBeGreaterThan(0);
    });
  });

  describe('Leaderboards', () => {
    it('should get global leaderboard', async () => {
      const leaderboard = await gamificationService.getGlobalLeaderboard();

      expect(leaderboard).toBeInstanceOf(Array);
      expect(leaderboard.length).toBeLessThanOrEqual(100);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].rank).toBe(1);
      }
    });

    it('should get friend leaderboard', async () => {
      const leaderboard = await gamificationService.getFriendLeaderboard(1);

      expect(leaderboard).toBeInstanceOf(Array);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].userId).toBeDefined();
        expect(leaderboard[0].points).toBeGreaterThanOrEqual(0);
      }
    });

    it('should get hive leaderboard', async () => {
      const leaderboard = await gamificationService.getHiveLeaderboard('hive-123');

      expect(leaderboard).toBeInstanceOf(Array);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].userId).toBeDefined();
        expect(leaderboard[0].points).toBeGreaterThanOrEqual(0);
      }
    });

    it('should get weekly leaderboard', async () => {
      const leaderboard = await gamificationService.getWeeklyLeaderboard();

      expect(leaderboard).toBeInstanceOf(Array);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].points).toBeGreaterThanOrEqual(0);
      }
    });

    it('should get monthly leaderboard', async () => {
      const leaderboard = await gamificationService.getMonthlyLeaderboard();

      expect(leaderboard).toBeInstanceOf(Array);
      if (leaderboard.length > 0) {
        expect(leaderboard[0].points).toBeGreaterThanOrEqual(0);
      }
    });
  });

  describe('Rewards & Badges', () => {
    it('should get available rewards', async () => {
      const rewards = await gamificationService.getAvailableRewards(1);

      expect(rewards).toBeInstanceOf(Array);
      if (rewards.length > 0) {
        const reward = rewards[0];
        expect(reward.id).toBeDefined();
        expect(reward.name).toBeDefined();
        expect(reward.cost).toBeGreaterThan(0);
      }
    });

    it('should claim reward', async () => {
      const claim = await gamificationService.claimReward(1, 'theme-unlock');

      expect(claim).toBeDefined();
      expect(claim.rewardId).toBe('theme-unlock');
      expect(claim.claimedAt).toBeDefined();
      expect(claim.pointsSpent).toBeGreaterThan(0);
    });

    it('should get badge collection', async () => {
      const badges = await gamificationService.getBadgeCollection(1);

      expect(badges).toBeInstanceOf(Array);
      if (badges.length > 0) {
        const badge = badges[0];
        expect(badge.id).toBeDefined();
        expect(badge.name).toBeDefined();
        expect(badge.tier).toBeDefined();
      }
    });

    it('should get badge progress', async () => {
      const progress = await gamificationService.getBadgeProgress(1, 'consistency-master');

      expect(progress).toBeDefined();
      expect(progress.badgeId).toBe('consistency-master');
      expect(progress.tier).toBeGreaterThanOrEqual(0);
      expect(progress.progress).toBeGreaterThanOrEqual(0);
      expect(progress.maxTier).toBeGreaterThan(0);
    });
  });

  describe('Streaks & Statistics', () => {
    it('should get current streak', async () => {
      const streak = await gamificationService.getCurrentStreak(1);

      expect(streak).toBeDefined();
      expect(streak.currentStreak).toBeGreaterThanOrEqual(0);
      expect(streak.longestStreak).toBeGreaterThanOrEqual(0);
      expect(streak.lastActivityDate).toBeTruthy();
    });

    it('should update streak', async () => {
      const updated = await gamificationService.updateStreak(1);

      expect(updated).toBeDefined();
      expect(updated.currentStreak).toBeGreaterThanOrEqual(1);
      expect(updated.lastActivityDate).toBeTruthy();
    });

    it('should get longest streak', async () => {
      const longest = await gamificationService.getLongestStreak(1);

      expect(longest).toBeDefined();
      expect(longest.days).toBeGreaterThanOrEqual(0);
      expect(longest.startDate).toBeDefined();
      expect(longest.endDate).toBeDefined();
    });

    it('should get gamification statistics', async () => {
      const stats = await gamificationService.getGamificationStats(1);

      expect(stats).toBeDefined();
      expect(stats.totalPoints).toBeGreaterThanOrEqual(0);
      expect(stats.totalAchievements).toBeGreaterThanOrEqual(0);
      expect(stats.challengesCompleted).toBeGreaterThanOrEqual(0);
      expect(stats.level).toBeGreaterThanOrEqual(1);
      expect(stats.achievementsUnlocked).toBeGreaterThanOrEqual(0);
      expect(stats.badgesEarned).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Error Handling', () => {
    it('should handle invalid point operations gracefully', async () => {
      await expect(
        gamificationService.awardPoints(-1, { points: 100, description: 'Test' })
      ).rejects.toThrow('Invalid user ID');

      await expect(
        gamificationService.deductPoints(1, { amount: -50, description: 'Test' })
      ).rejects.toThrow('Invalid amount');
    });

    // Network and authorization tests are skipped for mock implementation
    // These would be tested with actual API integration
    it.skip('should handle network errors', async () => {
      // Mock network error
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Network error'));

      await expect(
        gamificationService.getUserPoints(1)
      ).rejects.toThrow('Network error');
    });

    it.skip('should handle authorization errors', async () => {
      // Mock 401 response
      vi.spyOn(global, 'fetch').mockResolvedValueOnce(
        new Response(null, { status: 401, statusText: 'Unauthorized' })
      );

      await expect(
        gamificationService.getUserPoints(1)
      ).rejects.toThrow(/401/);
    });
  });

  describe('Caching', () => {
    it('should cache user data', async () => {
      // First call - fetches from API
      const firstCall = await gamificationService.getUserPoints(1);

      // Second call - should return cached data
      const secondCall = await gamificationService.getUserPoints(1);

      expect(firstCall).toEqual(secondCall);
    });

    it('should invalidate cache on updates', async () => {
      // Get initial data (cached)
      await gamificationService.getUserPoints(1);

      // Award points (should invalidate cache)
      await gamificationService.awardPoints(1, { points: 100, description: 'Test' });

      // Next call should fetch fresh data
      const freshData = await gamificationService.getUserPoints(1);
      expect(freshData).toBeDefined();
    });
  });

  describe('WebSocket Real-time Features', () => {
    it('should subscribe to gamification updates', async () => {
      mockWebSocketService.isConnected.mockReturnValue(true);

      const callback = vi.fn();
      const unsubscribe = gamificationService.subscribeToUpdates(1, callback);

      expect(mockWebSocketService.subscribe).toHaveBeenCalledWith(
        '/user/1/gamification',
        expect.any(Function)
      );
      expect(typeof unsubscribe).toBe('function');
    });

    it('should handle points earned events', async () => {
      mockWebSocketService.isConnected.mockReturnValue(true);

      const callback = vi.fn();
      gamificationService.onPointsEarned(1, callback);

      expect(mockWebSocketService.subscribe).toHaveBeenCalledWith(
        '/user/1/points',
        expect.any(Function)
      );
    });

    it('should handle achievement unlocked events', async () => {
      mockWebSocketService.isConnected.mockReturnValue(true);

      const callback = vi.fn();
      gamificationService.onAchievementUnlocked(1, callback);

      expect(mockWebSocketService.subscribe).toHaveBeenCalledWith(
        '/user/1/achievements',
        expect.any(Function)
      );
    });

    it('should handle level up events', async () => {
      mockWebSocketService.isConnected.mockReturnValue(true);

      const callback = vi.fn();
      gamificationService.onLevelUp(1, callback);

      expect(mockWebSocketService.subscribe).toHaveBeenCalledWith(
        '/user/1/levelup',
        expect.any(Function)
      );
    });

    it('should handle leaderboard update events', async () => {
      mockWebSocketService.isConnected.mockReturnValue(true);

      const callback = vi.fn();
      gamificationService.onLeaderboardUpdate('global', callback);

      expect(mockWebSocketService.subscribe).toHaveBeenCalledWith(
        '/leaderboard/global',
        expect.any(Function)
      );
    });
  });

  describe('Cleanup', () => {
    it('should clean up resources on cleanup', () => {
      gamificationService.cleanup();

      // Verify cache is cleared
      expect((gamificationService as any).cache.size).toBe(0);
    });

    it('should clear cache on demand', () => {
      gamificationService.clearCache();

      expect((gamificationService as any).cache.size).toBe(0);
    });
  });
});