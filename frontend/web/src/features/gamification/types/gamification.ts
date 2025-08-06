/**
 * Gamification system types for FocusHive
 * These types define the structure for points, achievements, streaks, and leaderboards
 */

export interface User {
  id: string;
  name: string;
  avatar?: string;
  email?: string;
}

export interface Points {
  current: number;
  total: number;
  todayEarned: number;
  weekEarned: number;
}

export interface Achievement {
  id: string;
  title: string;
  description: string;
  icon: string;
  category: AchievementCategory;
  points: number;
  unlockedAt?: Date;
  progress?: number;
  maxProgress?: number;
  isUnlocked: boolean;
  rarity: AchievementRarity;
}

export type AchievementCategory = 
  | 'focus' 
  | 'collaboration' 
  | 'consistency' 
  | 'milestone' 
  | 'special';

export type AchievementRarity = 
  | 'common' 
  | 'uncommon' 
  | 'rare' 
  | 'epic' 
  | 'legendary';

export interface Streak {
  id: string;
  type: StreakType;
  current: number;
  best: number;
  lastActivity: Date;
  isActive: boolean;
}

export type StreakType = 
  | 'daily_login' 
  | 'focus_session' 
  | 'goal_completion' 
  | 'hive_participation';

export interface LeaderboardEntry {
  user: User;
  points: number;
  rank: number;
  change?: number; // Position change from previous period
}

export interface Leaderboard {
  id: string;
  title: string;
  period: LeaderboardPeriod;
  entries: LeaderboardEntry[];
  lastUpdated: Date;
}

export type LeaderboardPeriod = 
  | 'daily' 
  | 'weekly' 
  | 'monthly' 
  | 'all_time';

export interface GamificationStats {
  points: Points;
  achievements: Achievement[];
  streaks: Streak[];
  level: number;
  rank: number;
  totalUsers: number;
}

export interface GamificationContextValue {
  stats: GamificationStats | null;
  loading: boolean;
  error: string | null;
  
  // Actions
  addPoints: (amount: number, source: string) => Promise<void>;
  unlockAchievement: (achievementId: string) => Promise<void>;
  updateStreak: (streakType: StreakType) => Promise<void>;
  refreshStats: () => Promise<void>;
}

export interface PointsDisplayProps {
  points: Points;
  showToday?: boolean;
  showWeek?: boolean;
  size?: 'small' | 'medium' | 'large';
  animated?: boolean;
}

export interface AchievementBadgeProps {
  achievement: Achievement;
  size?: 'small' | 'medium' | 'large';
  showProgress?: boolean;
  onClick?: () => void;
}

export interface StreakCounterProps {
  streak: Streak;
  variant?: 'default' | 'compact' | 'detailed';
  showBest?: boolean;
}

export interface LeaderboardCardProps {
  leaderboard: Leaderboard;
  currentUserId?: string;
  maxEntries?: number;
  showRankChange?: boolean;
}