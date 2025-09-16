import React from 'react';
import type {SvgIconProps} from '@mui/material/SvgIcon';
import {
  Adjust as TargetIcon,
  CalendarMonth as CalendarIcon,
  EmojiEvents as TrophyIcon,
  Handshake as HandshakeIcon,
  MilitaryTech as MedalIcon,
  Star as StarIcon,
} from '@mui/icons-material';
import type {
  Achievement,
  AchievementCategory,
  AchievementRarity,
  GamificationStats,
  LeaderboardEntry,
  Streak,
  StreakType,
} from '../types/gamification';

/**
 * Formats points with proper comma separators (US format)
 */
export const formatPoints = (points: number | null | undefined): string => {
  if (typeof points !== 'number' || isNaN(points)) return '0';
  return Math.round(points).toLocaleString('en-US');
};

/**
 * Formats streak type enum to human-readable string
 */
export const formatStreakType = (type: StreakType | null): string => {
  if (!type) return 'Unknown Type';

  const typeMap: Record<StreakType, string> = {
    daily_login: 'Daily Login',
    focus_session: 'Focus Session',
    goal_completion: 'Goal Completion',
    hive_participation: 'Hive Participation',
  };

  return typeMap[type] || 'Unknown Type';
};

/**
 * Calculates user level based on total points
 * Formula: level = floor(sqrt(points / 25)) + 1
 * Level 1: 0-24 points, Level 2: 25-99 points, Level 3: 100-224 points, etc.
 */
export const calculateLevel = (points: number | null | undefined): number => {
  if (typeof points !== 'number' || points < 0) return 1;
  if (points < 25) return 1;
  // Use a progressive level formula: level = floor(sqrt(points/25)) + 1
  return Math.floor(Math.sqrt(points / 25)) + 1;
};

/**
 * Calculates progress percentage (0-100)
 */
export const calculateProgress = (current: number, max: number): number => {
  if (max === 0) return current > 0 ? 100 : 0;
  return Math.min(100, Math.round((current / max) * 10000) / 100);
};

/**
 * Returns color code for achievement rarity
 */
export const getRarityColor = (rarity: AchievementRarity): string => {
  const colorMap: Record<AchievementRarity, string> = {
    common: '#9E9E9E',
    uncommon: '#4CAF50',
    rare: '#2196F3',
    epic: '#9C27B0',
    legendary: '#FF9800',
  };

  return colorMap[rarity] || colorMap.common;
};

/**
 * Returns Material UI icon for achievement category
 */
export const getCategoryIcon = (category: AchievementCategory): React.ComponentType<SvgIconProps> => {
  const iconMap: Record<AchievementCategory, React.ComponentType<SvgIconProps>> = {
    focus: TargetIcon,
    collaboration: HandshakeIcon,
    consistency: CalendarIcon,
    milestone: TrophyIcon,
    special: StarIcon,
  };

  return iconMap[category] || MedalIcon;
};

/**
 * Checks if achievement is unlocked
 */
export const isAchievementUnlocked = (achievement: Achievement): boolean => {
  return Boolean(achievement?.isUnlocked);
};

/**
 * Gets achievement progress percentage
 */
export const getAchievementProgress = (achievement: Achievement): number => {
  if (isAchievementUnlocked(achievement)) return 100;
  if (!achievement?.progress || !achievement?.maxProgress) return 0;
  return calculateProgress(achievement.progress, achievement.maxProgress);
};

/**
 * Calculates hours until a streak breaks
 */
export const calculateTimeUntilStreakBreaks = (
    streak: Streak,
    currentTime: Date = new Date()
): number => {
  if (!streak.isActive || !streak.lastActivity) return 0;

  const lastActivity = new Date(streak.lastActivity);
  const hoursElapsed = (currentTime.getTime() - lastActivity.getTime()) / (1000 * 60 * 60);
  const hoursUntilBreak = 24 - (hoursElapsed % 24);

  return Math.max(0, Math.floor(hoursUntilBreak));
};

/**
 * Formats rank change with arrows
 */
export const formatRankChange = (change: number | undefined): string => {
  if (change === undefined || change === 0) return '–';
  if (change > 0) return `↑${change}`;
  return `↓${Math.abs(change)}`;
};

/**
 * Finds user position in leaderboard
 */
export const getLeaderboardPosition = (
    entries: LeaderboardEntry[],
    userId: string
): number => {
  const position = entries.findIndex(entry => entry.user.id === userId);
  return position >= 0 ? position + 1 : -1;
};

/**
 * Calculates points needed for a specific level
 */
export const calculatePointsForLevel = (level: number): number => {
  if (level <= 1) return 0;
  // Match the level calculation formula: points = (level - 1)^2 * 25
  return Math.pow(level - 1, 2) * 25;
};

/**
 * Filters achievements by category
 */
export const getAchievementsByCategory = (
    achievements: Achievement[],
    category: AchievementCategory
): Achievement[] => {
  return achievements.filter(achievement => achievement.category === category);
};

/**
 * Gets only active streaks
 */
export const getActiveStreaks = (streaks: Streak[]): Streak[] => {
  return streaks.filter(streak => streak.isActive);
};

/**
 * Sorts achievements by rarity (legendary first)
 */
export const sortAchievementsByRarity = (achievements: Achievement[]): Achievement[] => {
  const rarityOrder: Record<AchievementRarity, number> = {
    legendary: 0,
    epic: 1,
    rare: 2,
    uncommon: 3,
    common: 4,
  };

  return [...achievements].sort((a, b) => {
    return rarityOrder[a.rarity] - rarityOrder[b.rarity];
  });
};

/**
 * Calculates days since last activity
 */
export const calculateDaysActive = (
    lastActivity: Date,
    currentTime: Date = new Date()
): number => {
  const diffTime = currentTime.getTime() - lastActivity.getTime();
  return Math.floor(diffTime / (1000 * 60 * 60 * 24));
};

/**
 * Checks if streak is about to break (within 6 hours)
 */
export const isStreakAboutToBreak = (
    streak: Streak,
    currentTime: Date = new Date()
): boolean => {
  if (!streak.isActive || !streak.lastActivity) return false;

  const lastActivity = new Date(streak.lastActivity);
  const hoursElapsed = (currentTime.getTime() - lastActivity.getTime()) / (1000 * 60 * 60);

  // If more than 18 hours have passed since last activity, it's about to break
  return hoursElapsed >= 18 && hoursElapsed < 24;
};

/**
 * Gets the next achievement to unlock (based on progress or lowest points)
 */
export const getNextAchievement = (achievements: Achievement[]): Achievement | null => {
  const locked = achievements.filter(a => !a.isUnlocked);
  if (locked.length === 0) return null;

  // Prioritize achievements with progress
  const withProgress = locked.filter(a => a.progress && a.progress > 0);
  if (withProgress.length > 0) {
    return withProgress.sort((a, b) => {
      const progressA = getAchievementProgress(a);
      const progressB = getAchievementProgress(b);
      return progressB - progressA; // Highest progress first
    })[0];
  }

  // Otherwise, return lowest point achievement
  return locked.sort((a, b) => a.points - b.points)[0];
};

/**
 * Calculates overall efficiency score (0-100)
 */
export const calculateEfficiencyScore = (stats: GamificationStats): number => {
  const {
    points,
    achievements,
    streaks,
    level,
    rank,
    totalUsers,
  } = stats;

  // Points efficiency (30%)
  const maxExpectedPoints = calculatePointsForLevel(level + 5);
  const pointsScore = Math.min(100, (points.total / maxExpectedPoints) * 100);

  // Achievement completion (25%)
  const unlockedCount = achievements.filter(a => a.isUnlocked).length;
  const achievementScore = achievements.length > 0
      ? (unlockedCount / achievements.length) * 100
      : 0;

  // Streak maintenance (25%)
  const activeCount = streaks.filter(s => s.isActive).length;
  const streakScore = streaks.length > 0
      ? (activeCount / streaks.length) * 100
      : 0;

  // Rank position (20%)
  const rankScore = totalUsers > 0
      ? ((totalUsers - rank + 1) / totalUsers) * 100
      : 50;

  const efficiency = (
      pointsScore * 0.3 +
      achievementScore * 0.25 +
      streakScore * 0.25 +
      rankScore * 0.2
  );

  return Math.min(100, Math.max(0, Math.round(efficiency)));
};

/**
 * Creates user initials from name
 */
export const getUserInitials = (name: string): string => {
  if (!name) return 'U';

  const words = name.trim().split(' ');
  if (words.length === 1) {
    return words[0].charAt(0).toUpperCase();
  }

  return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
};

/**
 * Formats time duration in human readable format
 */
export const formatDuration = (hours: number): string => {
  if (hours < 1) {
    const minutes = Math.round(hours * 60);
    return `${minutes}m`;
  }

  if (hours < 24) {
    return `${Math.floor(hours)}h`;
  }

  const days = Math.floor(hours / 24);
  const remainingHours = Math.floor(hours % 24);

  if (remainingHours === 0) {
    return `${days}d`;
  }

  return `${days}d ${remainingHours}h`;
};

/**
 * Generates a random achievement ID
 */
export const generateAchievementId = (title: string): string => {
  const sanitized = title.toLowerCase().replace(/[^a-z0-9]/g, '-');
  const timestamp = Date.now().toString(36);
  return `${sanitized}-${timestamp}`;
};

/**
 * Validates achievement data
 */
export const validateAchievement = (achievement: Partial<Achievement>): boolean => {
  return Boolean(
      achievement.id &&
      achievement.title &&
      achievement.description &&
      achievement.category &&
      achievement.rarity &&
      typeof achievement.points === 'number' &&
      achievement.points >= 0
  );
};

/**
 * Validates streak data
 */
export const validateStreak = (streak: Partial<Streak>): boolean => {
  return Boolean(
      streak.id &&
      streak.type &&
      typeof streak.current === 'number' &&
      typeof streak.best === 'number' &&
      streak.current >= 0 &&
      streak.best >= streak.current &&
      streak.lastActivity &&
      typeof streak.isActive === 'boolean'
  );
};