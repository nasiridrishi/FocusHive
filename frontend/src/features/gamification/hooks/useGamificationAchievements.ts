import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { Achievement, UserAchievement } from '@/contracts/gamification';
import { gamificationService } from '../services/gamificationService';

// Query keys factory
export const achievementKeys = {
  all: ['gamification', 'achievements'] as const,
  list: () => [...achievementKeys.all, 'list'] as const,
  userAchievements: (userId: number) => [...achievementKeys.all, 'user', userId] as const,
  progress: (userId: number, achievementId: string) =>
    [...achievementKeys.all, 'progress', userId, achievementId] as const,
  categories: () => [...achievementKeys.all, 'categories'] as const,
  recent: (userId: number, limit: number) =>
    [...achievementKeys.all, 'recent', userId, limit] as const,
};

/**
 * Hook to get all available achievements
 */
export function useAllAchievements() {
  return useQuery({
    queryKey: achievementKeys.list(),
    queryFn: () => gamificationService.getAllAchievements(),
    staleTime: 10 * 60 * 1000, // 10 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });
}

/**
 * Hook to get user achievements
 */
export function useUserAchievements(userId: number) {
  return useQuery({
    queryKey: achievementKeys.userAchievements(userId),
    queryFn: () => gamificationService.getUserAchievements(userId),
    staleTime: 60000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: userId > 0,
  });
}

/**
 * Hook to unlock an achievement
 */
export function useUnlockAchievement() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, achievementId }: { userId: number; achievementId: string }) =>
      gamificationService.unlockAchievement(userId, achievementId),
    onSuccess: (unlockedAchievement, variables) => {
      // Invalidate user achievements
      queryClient.invalidateQueries({
        queryKey: achievementKeys.userAchievements(variables.userId),
      });

      // Invalidate user points (achievement rewards)
      queryClient.invalidateQueries({
        queryKey: ['gamification', 'points', 'user', variables.userId],
      });

      // Show notification
      if (unlockedAchievement.notification) {
        console.log('Achievement unlocked!', unlockedAchievement);
      }
    },
    onError: (error) => {
      console.error('Failed to unlock achievement:', error);
    },
  });
}

/**
 * Hook to get achievement progress
 */
export function useAchievementProgress(userId: number, achievementId: string) {
  return useQuery({
    queryKey: achievementKeys.progress(userId, achievementId),
    queryFn: () => gamificationService.getAchievementProgress(userId, achievementId),
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: userId > 0 && !!achievementId,
  });
}

/**
 * Hook to get achievement categories
 */
export function useAchievementCategories() {
  return useQuery({
    queryKey: achievementKeys.categories(),
    queryFn: () => gamificationService.getAchievementCategories(),
    staleTime: 30 * 60 * 1000, // 30 minutes
    gcTime: 60 * 60 * 1000, // 1 hour
  });
}

/**
 * Hook to get recent achievements
 */
export function useRecentAchievements(userId: number, limit = 5) {
  return useQuery({
    queryKey: achievementKeys.recent(userId, limit),
    queryFn: () => gamificationService.getRecentAchievements(userId, limit),
    staleTime: 60000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: userId > 0,
  });
}

/**
 * Combined hook for achievement functionality
 */
export function useGamificationAchievements(userId: number) {
  const allAchievements = useAllAchievements();
  const userAchievements = useUserAchievements(userId);
  const recentAchievements = useRecentAchievements(userId, 5);
  const categories = useAchievementCategories();
  const unlockAchievement = useUnlockAchievement();

  return {
    // All achievements
    allAchievements: allAchievements.data || [],
    isLoadingAll: allAchievements.isLoading,

    // User achievements
    userAchievements: userAchievements.data || {
      unlocked: [],
      inProgress: [],
      locked: [],
    },
    isLoadingUser: userAchievements.isLoading,

    // Recent achievements
    recentAchievements: recentAchievements.data || [],

    // Categories
    categories: categories.data || [],

    // Actions
    unlockAchievement: (achievementId: string) =>
      unlockAchievement.mutate({ userId, achievementId }),

    // Loading states
    isUnlocking: unlockAchievement.isPending,

    // Helpers
    refetchAchievements: () => {
      allAchievements.refetch();
      userAchievements.refetch();
      recentAchievements.refetch();
    },

    // Achievement statistics
    getUnlockedCount: () => userAchievements.data?.unlocked?.length || 0,
    getInProgressCount: () => userAchievements.data?.inProgress?.length || 0,
    getLockedCount: () => userAchievements.data?.locked?.length || 0,
    getTotalCount: () => allAchievements.data?.length || 0,

    // Check achievement status
    isUnlocked: (achievementId: string) =>
      userAchievements.data?.unlocked?.some((a: any) => a.id === achievementId) || false,

    isInProgress: (achievementId: string) =>
      userAchievements.data?.inProgress?.some((a: any) => a.id === achievementId) || false,

    // Get achievement by ID
    getAchievementById: (achievementId: string) =>
      allAchievements.data?.find(a => a.id === achievementId),

    // Filter achievements by category
    getAchievementsByCategory: (category: string) =>
      allAchievements.data?.filter(a => a.category === category) || [],

    // Get completion percentage
    getCompletionPercentage: () => {
      const unlocked = userAchievements.data?.unlocked?.length || 0;
      const total = allAchievements.data?.length || 0;
      return total > 0 ? Math.round((unlocked / total) * 100) : 0;
    },
  };
}