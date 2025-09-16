/**
 * Gamification Hooks
 * Export all gamification-related React hooks for easy importing
 */

import { useGamificationPoints } from './useGamificationPoints';
import { useGamificationAchievements } from './useGamificationAchievements';
import { useGamificationChallenges } from './useGamificationChallenges';

// Points hooks
export {
  useUserPoints,
  useAwardPoints,
  useDeductPoints,
  usePointHistory,
  usePointsLeaderboard,
  useGamificationPoints,
  gamificationPointsKeys,
} from './useGamificationPoints';

// Achievement hooks
export {
  useAllAchievements,
  useUserAchievements,
  useUnlockAchievement,
  useAchievementProgress,
  useAchievementCategories,
  useRecentAchievements,
  useGamificationAchievements,
  achievementKeys,
} from './useGamificationAchievements';

// Challenge hooks
export {
  useAvailableChallenges,
  useJoinChallenge,
  useCompleteChallenge,
  useChallengeProgress,
  useLeaveChallenge,
  useChallengeLeaderboard,
  useGamificationChallenges,
  challengeKeys,
} from './useGamificationChallenges';

// Re-export the gamification service for direct access if needed
export { gamificationService } from '../services/gamificationService';

/**
 * Combined query keys for all gamification features
 */
export const allGamificationKeys = {
  all: ['gamification'] as const,
  points: ['gamification', 'points'] as const,
  achievements: ['gamification', 'achievements'] as const,
  challenges: ['gamification', 'challenges'] as const,
  rewards: ['gamification', 'rewards'] as const,
  badges: ['gamification', 'badges'] as const,
  streaks: ['gamification', 'streaks'] as const,
  leaderboards: ['gamification', 'leaderboards'] as const,
  stats: ['gamification', 'stats'] as const,
};

/**
 * Main combined gamification hook
 * Provides access to all gamification features in one hook
 */
export function useGamification(userId: number) {
  const points = useGamificationPoints(userId);
  const achievements = useGamificationAchievements(userId);
  const challenges = useGamificationChallenges(userId);

  return {
    // Points
    ...points,

    // Achievements
    achievements: achievements.allAchievements,
    userAchievements: achievements.userAchievements,
    unlockAchievement: achievements.unlockAchievement,
    achievementProgress: achievements.getCompletionPercentage(),

    // Challenges
    challenges: challenges.challenges,
    joinChallenge: challenges.joinChallenge,
    completeChallenge: challenges.completeChallenge,
    leaveChallenge: challenges.leaveChallenge,

    // Combined statistics
    stats: {
      totalPoints: points.points?.totalPoints || 0,
      currentLevel: points.points?.currentLevel || 1,
      levelProgress: points.points?.levelProgress || 0,
      achievementsUnlocked: achievements.getUnlockedCount(),
      achievementsTotal: achievements.getTotalCount(),
      activeChallenges: challenges.getActiveChallenges().length,
      currentStreak: points.getStreak().current,
      longestStreak: points.getStreak().longest,
    },

    // Loading states
    isLoading: points.isLoadingPoints || achievements.isLoadingAll || challenges.isLoadingChallenges,

    // Refresh all data
    refreshAll: () => {
      points.refetchPoints();
      achievements.refetchAchievements();
      challenges.refetchChallenges();
    },
  };
}