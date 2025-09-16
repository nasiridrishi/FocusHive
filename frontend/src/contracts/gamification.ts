/**
 * Gamification System Contract Types
 * Defines types for points, achievements, leaderboards, and rewards
 */

/**
 * Point types and categories
 */
export type PointCategory = 'focus' | 'collaboration' | 'consistency' | 'achievement' | 'social' | 'milestone';
export type AchievementTier = 'bronze' | 'silver' | 'gold' | 'platinum' | 'diamond';
export type LeaderboardPeriod = 'daily' | 'weekly' | 'monthly' | 'all-time';
export type RewardType = 'badge' | 'title' | 'theme' | 'avatar' | 'frame' | 'boost';
export type ChallengeStatus = 'active' | 'completed' | 'expired' | 'abandoned';
export type ChallengeType = 'daily' | 'weekly' | 'special' | 'community' | 'personal';

/**
 * User points and level
 */
export interface UserPoints {
  userId: number;
  totalPoints: number;
  currentLevel: number;
  pointsToNextLevel: number;
  levelProgress: number; // 0-100 percentage
  pointsByCategory: {
    category: PointCategory;
    points: number;
    percentage: number;
  }[];
  weeklyPoints: number;
  monthlyPoints: number;
  streak: {
    current: number;
    longest: number;
    startDate: string;
  };
  lastEarnedAt: string;
  nextMilestone: {
    points: number;
    reward: string;
  };
}

/**
 * Point transaction
 */
export interface PointTransaction {
  id: string;
  userId: number;
  points: number;
  category: PointCategory;
  source: string;
  description: string;
  metadata?: {
    sessionId?: string;
    achievementId?: string;
    challengeId?: string;
    duration?: number;
    multiplier?: number;
  };
  createdAt: string;
}

/**
 * Achievement definition
 */
export interface Achievement {
  id: string;
  name: string;
  description: string;
  icon: string;
  tier: AchievementTier;
  category: string;
  points: number;
  requirements: AchievementRequirement[];
  unlockedBy: number; // number of users who have this
  rarity: 'common' | 'uncommon' | 'rare' | 'epic' | 'legendary';
  isSecret: boolean;
  prerequisiteIds?: string[];
}

/**
 * Achievement requirement
 */
export interface AchievementRequirement {
  type: 'points' | 'sessions' | 'streak' | 'time' | 'social' | 'custom';
  target: number;
  current?: number;
  description: string;
  metric?: string;
  timeframe?: string; // e.g., "7d", "30d", "all"
}

/**
 * User achievement
 */
export interface UserAchievement {
  achievementId: string;
  achievement: Achievement;
  userId: number;
  unlockedAt: string;
  progress: number; // 0-100 for partial achievements
  isNew: boolean;
  sharedAt?: string;
  showcased: boolean; // displayed on profile
}

/**
 * Leaderboard entry
 */
export interface LeaderboardEntry {
  rank: number;
  previousRank?: number;
  userId: number;
  username: string;
  avatarUrl?: string;
  level: number;
  points: number;
  pointsGained: number; // in the period
  sessionCount: number;
  totalTime: number; // minutes
  achievements: number;
  isCurrentUser: boolean;
  trend: 'up' | 'down' | 'stable';
}

/**
 * Leaderboard
 */
export interface Leaderboard {
  period: LeaderboardPeriod;
  startDate: string;
  endDate: string;
  entries: LeaderboardEntry[];
  totalParticipants: number;
  userRank?: number;
  pagination?: {
    page: number;
    pageSize: number;
    total: number;
    hasMore: boolean;
  };
}

/**
 * Challenge definition
 */
export interface Challenge {
  id: string;
  name: string;
  description: string;
  type: ChallengeType;
  status: ChallengeStatus;
  icon: string;
  color?: string;
  startDate: string;
  endDate: string;
  requirements: ChallengeRequirement[];
  rewards: ChallengeReward[];
  maxParticipants?: number;
  currentParticipants: number;
  difficulty: 'easy' | 'medium' | 'hard' | 'expert';
  tags: string[];
  createdBy?: {
    userId: number;
    username: string;
  };
}

/**
 * Challenge requirement
 */
export interface ChallengeRequirement {
  id: string;
  type: 'sessions' | 'time' | 'points' | 'streak' | 'collaboration' | 'specific';
  description: string;
  target: number;
  unit: string;
  timeframe?: string;
  conditions?: Record<string, any>;
}

/**
 * Challenge reward
 */
export interface ChallengeReward {
  type: RewardType;
  value: string | number;
  description: string;
  icon?: string;
  rarity?: 'common' | 'rare' | 'epic' | 'legendary';
}

/**
 * User challenge progress
 */
export interface UserChallenge {
  challengeId: string;
  challenge: Challenge;
  userId: number;
  status: ChallengeStatus;
  progress: {
    requirementId: string;
    current: number;
    target: number;
    completed: boolean;
  }[];
  startedAt: string;
  completedAt?: string;
  abandonedAt?: string;
  rewardsClaimed: boolean;
}

/**
 * Badge definition
 */
export interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  tier: AchievementTier;
  requirements: string;
  earnedCount: number;
  maxLevel?: number;
}

/**
 * User badge
 */
export interface UserBadge {
  badgeId: string;
  badge: Badge;
  userId: number;
  level: number;
  earnedAt: string;
  progress?: {
    current: number;
    nextLevel: number;
  };
  featured: boolean; // shown on profile
}

/**
 * Reward definition
 */
export interface Reward {
  id: string;
  name: string;
  description: string;
  type: RewardType;
  value: any;
  icon: string;
  cost?: number; // points cost if purchasable
  requirements?: string[];
  availability: 'always' | 'limited' | 'seasonal' | 'achievement';
  expiresAt?: string;
}

/**
 * User reward
 */
export interface UserReward {
  rewardId: string;
  reward: Reward;
  userId: number;
  acquiredAt: string;
  source: 'achievement' | 'challenge' | 'purchase' | 'gift';
  sourceId?: string;
  isActive: boolean;
  expiresAt?: string;
}

/**
 * Level definition
 */
export interface Level {
  level: number;
  name: string;
  minPoints: number;
  maxPoints: number;
  perks: string[];
  badge?: string;
  color?: string;
}

/**
 * Streak information
 */
export interface Streak {
  userId: number;
  currentStreak: number;
  longestStreak: number;
  lastActivityDate: string;
  startDate: string;
  freezesAvailable: number;
  freezesUsed: number;
  milestones: {
    days: number;
    achievedAt?: string;
    reward?: string;
  }[];
}

/**
 * Daily bonus
 */
export interface DailyBonus {
  day: number;
  points: number;
  bonus?: string;
  claimed: boolean;
  claimedAt?: string;
}

/**
 * Experience event
 */
export interface ExperienceEvent {
  type: 'session_complete' | 'achievement_unlock' | 'challenge_complete' |
        'buddy_session' | 'streak_milestone' | 'level_up' | 'special';
  points: number;
  multiplier?: number;
  description: string;
  timestamp: string;
}

/**
 * Gamification statistics
 */
export interface GamificationStats {
  userId: number;
  level: number;
  totalPoints: number;
  rank: number;
  totalUsers: number;
  percentile: number; // 0-100
  achievementsUnlocked: number;
  totalAchievements: number;
  badgesEarned: number;
  challengesCompleted: number;
  averageSessionPoints: number;
  mostProductiveDay: string;
  mostEarnedCategory: PointCategory;
  recentActivity: ExperienceEvent[];
  upcomingMilestones: {
    type: string;
    requirement: number;
    current: number;
    reward: string;
  }[];
}

/**
 * Season/Event
 */
export interface Season {
  id: string;
  name: string;
  description: string;
  theme: string;
  startDate: string;
  endDate: string;
  isActive: boolean;
  rewards: SeasonReward[];
  challenges: string[]; // challenge IDs
  leaderboard?: Leaderboard;
}

/**
 * Season reward tier
 */
export interface SeasonReward {
  tier: number;
  requiredPoints: number;
  rewards: Reward[];
  claimed?: boolean;
}

/**
 * User season progress
 */
export interface UserSeasonProgress {
  seasonId: string;
  userId: number;
  points: number;
  tier: number;
  rank?: number;
  rewardsClaimed: number[];
  completedChallenges: string[];
  startedAt: string;
  lastActivityAt: string;
}

/**
 * Notification preferences
 */
export interface GamificationPreferences {
  userId: number;
  enableNotifications: boolean;
  achievementAlerts: boolean;
  leaderboardUpdates: boolean;
  challengeReminders: boolean;
  streakReminders: boolean;
  levelUpCelebration: boolean;
  friendActivity: boolean;
  weeklyReport: boolean;
}

/**
 * API Response types
 */
export interface PointsResponse {
  points: UserPoints;
  recentTransactions: PointTransaction[];
}

export interface AchievementsResponse {
  achievements: UserAchievement[];
  totalUnlocked: number;
  totalAvailable: number;
  nextAchievements: Achievement[];
}

export interface ChallengesResponse {
  active: UserChallenge[];
  completed: UserChallenge[];
  available: Challenge[];
}

export interface RewardsResponse {
  rewards: UserReward[];
  available: Reward[];
  points: number;
}

/**
 * WebSocket events
 */
export interface GamificationWebSocketEvent {
  type: 'points_earned' | 'achievement_unlocked' | 'level_up' |
        'challenge_complete' | 'leaderboard_update' | 'streak_update';
  userId: number;
  data: any;
  timestamp: string;
}

/**
 * Request types
 */
export interface ClaimRewardRequest {
  rewardId: string;
  usePoints?: boolean;
}

export interface JoinChallengeRequest {
  challengeId: string;
}

export interface UpdatePreferencesRequest {
  preferences: Partial<GamificationPreferences>;
}

/**
 * Paginated response
 */
export interface PaginatedGamificationResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}