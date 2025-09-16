import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { UserPoints, PointTransaction } from '@/contracts/gamification';
import { gamificationService } from '../services/gamificationService';

// Query keys factory
export const gamificationPointsKeys = {
  all: ['gamification', 'points'] as const,
  userPoints: (userId: number) => [...gamificationPointsKeys.all, 'user', userId] as const,
  history: (userId: number, page: number) =>
    [...gamificationPointsKeys.all, 'history', userId, page] as const,
  leaderboard: (period: string) => [...gamificationPointsKeys.all, 'leaderboard', period] as const,
};

/**
 * Hook to get user points
 */
export function useUserPoints(userId: number) {
  return useQuery({
    queryKey: gamificationPointsKeys.userPoints(userId),
    queryFn: () => gamificationService.getUserPoints(userId),
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: userId > 0,
  });
}

/**
 * Hook to award points to a user
 */
export function useAwardPoints() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, params }: { userId: number; params: any }) =>
      gamificationService.awardPoints(userId, params),
    onSuccess: (transaction, variables) => {
      // Invalidate user points to refetch
      queryClient.invalidateQueries({
        queryKey: gamificationPointsKeys.userPoints(variables.userId),
      });

      // Invalidate leaderboards
      queryClient.invalidateQueries({
        queryKey: gamificationPointsKeys.all,
      });
    },
    onError: (error) => {
      console.error('Failed to award points:', error);
    },
  });
}

/**
 * Hook to deduct points from a user
 */
export function useDeductPoints() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, params }: { userId: number; params: any }) =>
      gamificationService.deductPoints(userId, params),
    onSuccess: (transaction, variables) => {
      // Invalidate user points
      queryClient.invalidateQueries({
        queryKey: gamificationPointsKeys.userPoints(variables.userId),
      });
    },
    onError: (error) => {
      console.error('Failed to deduct points:', error);
    },
  });
}

/**
 * Hook to get point history
 */
export function usePointHistory(userId: number, page = 0, limit = 10) {
  return useQuery({
    queryKey: gamificationPointsKeys.history(userId, page),
    queryFn: () => gamificationService.getPointHistory(userId, { page, limit }),
    staleTime: 60000, // 1 minute
    gcTime: 10 * 60 * 1000, // 10 minutes
    enabled: userId > 0,
  });
}

/**
 * Hook to get points leaderboard
 */
export function usePointsLeaderboard(period: 'daily' | 'weekly' | 'monthly' | 'all-time' = 'weekly') {
  return useQuery({
    queryKey: gamificationPointsKeys.leaderboard(period),
    queryFn: () => gamificationService.getPointsLeaderboard(period),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 15 * 60 * 1000, // 15 minutes
    refetchInterval: 60000, // Refresh every minute for real-time updates
  });
}

/**
 * Combined hook for gamification points functionality
 */
export function useGamificationPoints(userId: number) {
  const userPoints = useUserPoints(userId);
  const awardPoints = useAwardPoints();
  const deductPoints = useDeductPoints();
  const dailyLeaderboard = usePointsLeaderboard('daily');
  const weeklyLeaderboard = usePointsLeaderboard('weekly');
  const monthlyLeaderboard = usePointsLeaderboard('monthly');

  return {
    // User Points
    points: userPoints.data,
    isLoadingPoints: userPoints.isLoading,
    pointsError: userPoints.error,

    // Actions
    awardPoints: (params: any) => awardPoints.mutate({ userId, params }),
    deductPoints: (params: any) => deductPoints.mutate({ userId, params }),

    // Loading states
    isAwardingPoints: awardPoints.isPending,
    isDeductingPoints: deductPoints.isPending,

    // Leaderboards
    dailyLeaderboard: dailyLeaderboard.data || [],
    weeklyLeaderboard: weeklyLeaderboard.data || [],
    monthlyLeaderboard: monthlyLeaderboard.data || [],
    isLoadingLeaderboards:
      dailyLeaderboard.isLoading || weeklyLeaderboard.isLoading || monthlyLeaderboard.isLoading,

    // Helpers
    refetchPoints: userPoints.refetch,
    hasPoints: (userPoints.data?.totalPoints || 0) > 0,
    canAfford: (cost: number) => (userPoints.data?.totalPoints || 0) >= cost,

    // Point statistics
    getPointsByCategory: () => userPoints.data?.pointsByCategory || [],
    getCurrentLevel: () => userPoints.data?.currentLevel || 1,
    getLevelProgress: () => userPoints.data?.levelProgress || 0,
    getStreak: () => userPoints.data?.streak || { current: 0, longest: 0, startDate: '' },

    // Quick actions
    quickAward: (points: number, description: string) =>
      awardPoints.mutate({
        userId,
        params: { points, description, category: 'focus' },
      }),

    quickDeduct: (points: number, description: string) =>
      deductPoints.mutate({
        userId,
        params: { points, description, category: 'milestone' },
      }),
  };
}