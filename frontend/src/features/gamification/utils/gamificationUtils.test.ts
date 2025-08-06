import { describe, it, expect } from 'vitest';
import {
  formatPoints,
  formatStreakType,
  calculateLevel,
  calculateProgress,
  getRarityColor,
  getCategoryIcon,
  isAchievementUnlocked,
  getAchievementProgress,
  calculateTimeUntilStreakBreaks,
  formatRankChange,
  getLeaderboardPosition,
  calculatePointsForLevel,
  getAchievementsByCategory,
  getActiveStreaks,
  sortAchievementsByRarity,
  calculateDaysActive,
  isStreakAboutToBreak,
  getNextAchievement,
  calculateEfficiencyScore,
} from './gamificationUtils';
import type { Achievement, Streak, StreakType, AchievementRarity, AchievementCategory } from '../types/gamification';

const mockAchievements: Achievement[] = [
  {
    id: 'first-focus',
    title: 'First Focus',
    description: 'Complete your first focus session',
    icon: 'focus',
    category: 'focus',
    points: 100,
    unlockedAt: new Date('2024-01-15T10:30:00Z'),
    isUnlocked: true,
    rarity: 'common',
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
    id: 'collaborator',
    title: 'Team Player',
    description: 'Join 5 different hives',
    icon: 'team',
    category: 'collaboration',
    points: 200,
    progress: 3,
    maxProgress: 5,
    isUnlocked: false,
    rarity: 'uncommon',
  },
  {
    id: 'legend',
    title: 'FocusHive Legend',
    description: 'Achieve the impossible',
    icon: 'legend',
    category: 'special',
    points: 10000,
    isUnlocked: false,
    rarity: 'legendary',
  },
];

const mockStreaks: Streak[] = [
  {
    id: 'daily-login-1',
    type: 'daily_login',
    current: 7,
    best: 15,
    lastActivity: new Date('2024-01-15T10:30:00Z'),
    isActive: true,
  },
  {
    id: 'focus-session-1',
    type: 'focus_session',
    current: 0,
    best: 25,
    lastActivity: new Date('2024-01-10T10:30:00Z'),
    isActive: false,
  },
  {
    id: 'hive-participation-1',
    type: 'hive_participation',
    current: 3,
    best: 10,
    lastActivity: new Date(),
    isActive: true,
  },
];

describe('gamificationUtils', () => {
  describe('formatPoints', () => {
    it('formats small numbers correctly', () => {
      expect(formatPoints(0)).toBe('0');
      expect(formatPoints(42)).toBe('42');
      expect(formatPoints(999)).toBe('999');
    });

    it('formats thousands with commas', () => {
      expect(formatPoints(1000)).toBe('1,000');
      expect(formatPoints(1234)).toBe('1,234');
      expect(formatPoints(12345)).toBe('12,345');
    });

    it('formats millions correctly', () => {
      expect(formatPoints(1000000)).toBe('1,000,000');
      expect(formatPoints(1234567)).toBe('1,234,567');
    });

    it('handles negative numbers', () => {
      expect(formatPoints(-100)).toBe('-100');
      expect(formatPoints(-1234)).toBe('-1,234');
    });

    it('handles decimal numbers by rounding', () => {
      expect(formatPoints(123.7)).toBe('124');
      expect(formatPoints(123.4)).toBe('123');
    });
  });

  describe('formatStreakType', () => {
    it('formats all streak types correctly', () => {
      expect(formatStreakType('daily_login')).toBe('Daily Login');
      expect(formatStreakType('focus_session')).toBe('Focus Session');
      expect(formatStreakType('goal_completion')).toBe('Goal Completion');
      expect(formatStreakType('hive_participation')).toBe('Hive Participation');
    });

    it('handles unknown streak types gracefully', () => {
      expect(formatStreakType('unknown_type' as StreakType)).toBe('Unknown Type');
    });
  });

  describe('calculateLevel', () => {
    it('calculates correct level for points', () => {
      expect(calculateLevel(0)).toBe(1);
      expect(calculateLevel(25)).toBe(2);
      expect(calculateLevel(100)).toBe(3);
      expect(calculateLevel(225)).toBe(4);
      expect(calculateLevel(400)).toBe(5);
      expect(calculateLevel(1000)).toBe(7);
    });

    it('handles very high point values', () => {
      expect(calculateLevel(100000)).toBe(64);
      expect(calculateLevel(1000000)).toBe(201);
    });

    it('never returns level below 1', () => {
      expect(calculateLevel(-100)).toBe(1);
    });
  });

  describe('calculateProgress', () => {
    it('calculates progress percentage correctly', () => {
      expect(calculateProgress(0, 100)).toBe(0);
      expect(calculateProgress(25, 100)).toBe(25);
      expect(calculateProgress(50, 100)).toBe(50);
      expect(calculateProgress(100, 100)).toBe(100);
    });

    it('handles edge cases', () => {
      expect(calculateProgress(0, 0)).toBe(0);
      expect(calculateProgress(50, 0)).toBe(100); // Complete when no max
      expect(calculateProgress(150, 100)).toBe(100); // Cap at 100%
    });

    it('rounds to reasonable precision', () => {
      expect(calculateProgress(33, 100)).toBe(33);
      expect(calculateProgress(1, 3)).toBe(33.33);
    });
  });

  describe('getRarityColor', () => {
    it('returns correct colors for all rarities', () => {
      expect(getRarityColor('common')).toBe('#9E9E9E');
      expect(getRarityColor('uncommon')).toBe('#4CAF50');
      expect(getRarityColor('rare')).toBe('#2196F3');
      expect(getRarityColor('epic')).toBe('#9C27B0');
      expect(getRarityColor('legendary')).toBe('#FF9800');
    });

    it('handles unknown rarity gracefully', () => {
      expect(getRarityColor('unknown' as AchievementRarity)).toBe('#9E9E9E');
    });
  });

  describe('getCategoryIcon', () => {
    it('returns correct icons for all categories', () => {
      expect(getCategoryIcon('focus')).toBe('🎯');
      expect(getCategoryIcon('collaboration')).toBe('🤝');
      expect(getCategoryIcon('consistency')).toBe('📅');
      expect(getCategoryIcon('milestone')).toBe('🏆');
      expect(getCategoryIcon('special')).toBe('⭐');
    });

    it('handles unknown category gracefully', () => {
      expect(getCategoryIcon('unknown' as AchievementCategory)).toBe('🎖️');
    });
  });

  describe('isAchievementUnlocked', () => {
    it('correctly identifies unlocked achievements', () => {
      expect(isAchievementUnlocked(mockAchievements[0])).toBe(true);
    });

    it('correctly identifies locked achievements', () => {
      expect(isAchievementUnlocked(mockAchievements[1])).toBe(false);
      expect(isAchievementUnlocked(mockAchievements[2])).toBe(false);
    });
  });

  describe('getAchievementProgress', () => {
    it('returns correct progress for achievements with progress data', () => {
      expect(getAchievementProgress(mockAchievements[1])).toBe(45);
    });

    it('returns 100 for unlocked achievements', () => {
      expect(getAchievementProgress(mockAchievements[0])).toBe(100);
    });

    it('returns 0 for achievements without progress data', () => {
      expect(getAchievementProgress(mockAchievements[3])).toBe(0);
    });
  });

  describe('calculateTimeUntilStreakBreaks', () => {
    it('calculates time until daily streak breaks', () => {
      const now = new Date('2024-01-15T14:30:00Z');
      const streak = {
        ...mockStreaks[0],
        lastActivity: new Date('2024-01-15T10:30:00Z'),
      };
      
      const hoursUntilBreak = calculateTimeUntilStreakBreaks(streak, now);
      expect(hoursUntilBreak).toBe(20); // Should be within 24h cycle
    });

    it('returns 0 for broken streaks', () => {
      const now = new Date();
      const hoursUntilBreak = calculateTimeUntilStreakBreaks(mockStreaks[1], now);
      expect(hoursUntilBreak).toBe(0);
    });

    it('handles streaks that are about to break', () => {
      const now = new Date('2024-01-15T23:30:00Z');
      const streak = {
        ...mockStreaks[0],
        lastActivity: new Date('2024-01-15T22:30:00Z'),
      };
      
      const hoursUntilBreak = calculateTimeUntilStreakBreaks(streak, now);
      expect(hoursUntilBreak).toBe(23); // 23 hours left in cycle
    });
  });

  describe('formatRankChange', () => {
    it('formats positive rank changes', () => {
      expect(formatRankChange(3)).toBe('↑3');
      expect(formatRankChange(1)).toBe('↑1');
    });

    it('formats negative rank changes', () => {
      expect(formatRankChange(-2)).toBe('↓2');
      expect(formatRankChange(-1)).toBe('↓1');
    });

    it('formats no change', () => {
      expect(formatRankChange(0)).toBe('–');
    });

    it('handles undefined change', () => {
      expect(formatRankChange(undefined)).toBe('–');
    });
  });

  describe('getLeaderboardPosition', () => {
    const mockEntries = [
      { user: { id: '1', name: 'Alice' }, points: 1000, rank: 1 },
      { user: { id: '2', name: 'Bob' }, points: 800, rank: 2 },
      { user: { id: '3', name: 'Carol' }, points: 600, rank: 3 },
    ];

    it('finds user position correctly', () => {
      expect(getLeaderboardPosition(mockEntries, '2')).toBe(2);
      expect(getLeaderboardPosition(mockEntries, '1')).toBe(1);
    });

    it('returns -1 for user not on leaderboard', () => {
      expect(getLeaderboardPosition(mockEntries, '999')).toBe(-1);
    });

    it('handles empty leaderboard', () => {
      expect(getLeaderboardPosition([], '1')).toBe(-1);
    });
  });

  describe('calculatePointsForLevel', () => {
    it('calculates points needed for specific levels', () => {
      expect(calculatePointsForLevel(1)).toBe(0);
      expect(calculatePointsForLevel(2)).toBe(25);
      expect(calculatePointsForLevel(3)).toBe(100);
      expect(calculatePointsForLevel(5)).toBe(400);
    });

    it('handles high levels', () => {
      expect(calculatePointsForLevel(10)).toBeGreaterThan(calculatePointsForLevel(9));
      expect(calculatePointsForLevel(20)).toBeGreaterThan(calculatePointsForLevel(10));
    });
  });

  describe('getAchievementsByCategory', () => {
    it('filters achievements by category correctly', () => {
      const focusAchievements = getAchievementsByCategory(mockAchievements, 'focus');
      expect(focusAchievements).toHaveLength(2);
      expect(focusAchievements.every(a => a.category === 'focus')).toBe(true);
    });

    it('returns empty array for non-existent category', () => {
      const achievements = getAchievementsByCategory(mockAchievements, 'nonexistent' as AchievementCategory);
      expect(achievements).toHaveLength(0);
    });

    it('handles empty input array', () => {
      const achievements = getAchievementsByCategory([], 'focus');
      expect(achievements).toHaveLength(0);
    });
  });

  describe('getActiveStreaks', () => {
    it('filters active streaks correctly', () => {
      const activeStreaks = getActiveStreaks(mockStreaks);
      expect(activeStreaks).toHaveLength(2);
      expect(activeStreaks.every(s => s.isActive)).toBe(true);
    });

    it('handles all inactive streaks', () => {
      const inactiveStreaks = mockStreaks.map(s => ({ ...s, isActive: false }));
      const activeStreaks = getActiveStreaks(inactiveStreaks);
      expect(activeStreaks).toHaveLength(0);
    });

    it('handles empty input array', () => {
      const activeStreaks = getActiveStreaks([]);
      expect(activeStreaks).toHaveLength(0);
    });
  });

  describe('sortAchievementsByRarity', () => {
    it('sorts achievements by rarity correctly', () => {
      const sorted = sortAchievementsByRarity(mockAchievements);
      
      expect(sorted[0].rarity).toBe('legendary');
      expect(sorted[1].rarity).toBe('epic');
      expect(sorted[2].rarity).toBe('uncommon');
      expect(sorted[3].rarity).toBe('common');
    });

    it('maintains stable sort for same rarity', () => {
      const duplicateRarities = [
        { ...mockAchievements[0], id: 'common1' },
        { ...mockAchievements[0], id: 'common2' },
      ];
      
      const sorted = sortAchievementsByRarity(duplicateRarities);
      expect(sorted[0].id).toBe('common1');
      expect(sorted[1].id).toBe('common2');
    });

    it('handles empty array', () => {
      const sorted = sortAchievementsByRarity([]);
      expect(sorted).toHaveLength(0);
    });
  });

  describe('calculateDaysActive', () => {
    it('calculates days correctly for recent activity', () => {
      const now = new Date('2024-01-16T10:30:00Z');
      const lastActivity = new Date('2024-01-15T10:30:00Z');
      
      expect(calculateDaysActive(lastActivity, now)).toBe(1);
    });

    it('calculates days correctly for same day activity', () => {
      const now = new Date('2024-01-15T15:30:00Z');
      const lastActivity = new Date('2024-01-15T10:30:00Z');
      
      expect(calculateDaysActive(lastActivity, now)).toBe(0);
    });

    it('handles week-old activity', () => {
      const now = new Date('2024-01-22T10:30:00Z');
      const lastActivity = new Date('2024-01-15T10:30:00Z');
      
      expect(calculateDaysActive(lastActivity, now)).toBe(7);
    });
  });

  describe('isStreakAboutToBreak', () => {
    it('identifies streaks about to break (within 6 hours)', () => {
      const now = new Date('2024-01-16T12:00:00Z');
      const streak = {
        ...mockStreaks[0],
        lastActivity: new Date('2024-01-15T14:00:00Z'), // 22 hours ago
      };
      
      expect(isStreakAboutToBreak(streak, now)).toBe(true);
    });

    it('identifies safe streaks', () => {
      const now = new Date('2024-01-16T10:00:00Z');
      const streak = {
        ...mockStreaks[0],
        lastActivity: new Date('2024-01-16T08:00:00Z'),
      };
      
      expect(isStreakAboutToBreak(streak, now)).toBe(false);
    });

    it('handles broken streaks', () => {
      const now = new Date();
      expect(isStreakAboutToBreak(mockStreaks[1], now)).toBe(false);
    });
  });

  describe('getNextAchievement', () => {
    it('finds next locked achievement with lowest points', () => {
      const next = getNextAchievement(mockAchievements);
      expect(next?.id).toBe('collaborator'); // 200 points, lowest locked
    });

    it('finds next achievement with progress', () => {
      const achievementsWithProgress = mockAchievements.filter(a => a.progress);
      const next = getNextAchievement(achievementsWithProgress);
      expect(next?.progress).toBeGreaterThan(0);
    });

    it('returns null when all achievements are unlocked', () => {
      const allUnlocked = mockAchievements.map(a => ({ ...a, isUnlocked: true }));
      const next = getNextAchievement(allUnlocked);
      expect(next).toBeNull();
    });

    it('handles empty array', () => {
      const next = getNextAchievement([]);
      expect(next).toBeNull();
    });
  });

  describe('calculateEfficiencyScore', () => {
    it('calculates efficiency score correctly', () => {
      const stats = {
        points: { current: 1000, total: 1000, todayEarned: 100, weekEarned: 500 },
        achievements: mockAchievements,
        streaks: mockStreaks,
        level: 5,
        rank: 100,
        totalUsers: 1000,
      };
      
      const score = calculateEfficiencyScore(stats);
      expect(score).toBeGreaterThan(0);
      expect(score).toBeLessThanOrEqual(100);
    });

    it('handles perfect efficiency', () => {
      const perfectStats = {
        points: { current: 10000, total: 10000, todayEarned: 1000, weekEarned: 5000 },
        achievements: mockAchievements.map(a => ({ ...a, isUnlocked: true })),
        streaks: mockStreaks.map(s => ({ ...s, current: s.best, isActive: true })),
        level: 20,
        rank: 1,
        totalUsers: 1000,
      };
      
      const score = calculateEfficiencyScore(perfectStats);
      expect(score).toBeGreaterThan(80); // Allow for reasonable efficiency score
    });

    it('handles low efficiency', () => {
      const lowStats = {
        points: { current: 100, total: 100, todayEarned: 0, weekEarned: 10 },
        achievements: [],
        streaks: mockStreaks.map(s => ({ ...s, isActive: false, current: 0 })),
        level: 1,
        rank: 1000,
        totalUsers: 1000,
      };
      
      const score = calculateEfficiencyScore(lowStats);
      expect(score).toBeGreaterThanOrEqual(0);
      expect(score).toBeLessThan(30);
    });
  });

  describe('Edge Cases and Error Handling', () => {
    it('handles null and undefined inputs gracefully', () => {
      expect(() => formatPoints(null as unknown as number)).not.toThrow();
      expect(() => calculateLevel(undefined as unknown as number)).not.toThrow();
      expect(() => formatStreakType(null)).not.toThrow();
    });

    it('handles malformed data gracefully', () => {
      const malformedAchievement = {
        id: 'test',
        title: 'Test',
        description: 'Test desc',
        icon: 'test-icon',
        category: 'focus',
        points: 100,
        isUnlocked: false,
        rarity: 'common'
      } as Achievement;
      
      expect(() => isAchievementUnlocked(malformedAchievement)).not.toThrow();
      expect(() => getAchievementProgress(malformedAchievement)).not.toThrow();
    });

    it('handles extreme values', () => {
      expect(formatPoints(Number.MAX_SAFE_INTEGER)).toContain(',');
      expect(calculateLevel(Number.MAX_SAFE_INTEGER)).toBeGreaterThan(0);
      expect(calculateProgress(Number.MAX_SAFE_INTEGER, 100)).toBe(100);
    });
  });
});