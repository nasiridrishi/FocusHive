import { useQuery, useQueryClient } from '@tanstack/react-query';
import type { BuddyStats } from '@/contracts/buddy';
import { buddyService } from '../services/buddyService';

// Query keys factory
export const buddyStatsKeys = {
  all: ['buddy', 'stats'] as const,
  myStats: () => [...buddyStatsKeys.all, 'me'] as const,
  userStats: (userId?: number) => [...buddyStatsKeys.all, 'user', userId || 'me'] as const,
  topBuddies: (period?: 'week' | 'month' | 'all') =>
    [...buddyStatsKeys.all, 'top-buddies', period || 'all'] as const,
  leaderboard: (period: 'daily' | 'weekly' | 'monthly' | string) =>
    [...buddyStatsKeys.all, 'leaderboard', period] as const,
  comparison: (buddyUserId: number) => [...buddyStatsKeys.all, 'comparison', buddyUserId] as const,
};

/**
 * Hook to get personal buddy statistics
 */
export function useBuddyStats() {
  return useQuery({
    queryKey: buddyStatsKeys.myStats(),
    queryFn: () => buddyService.getMyStats(),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to get top buddies list
 */
export function useTopBuddies() {
  return useQuery({
    queryKey: buddyStatsKeys.topBuddies(),
    queryFn: () => buddyService.getTopBuddies(),
    staleTime: 10 * 60 * 1000, // 10 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });
}

/**
 * Hook to get buddy leaderboard for a specific period
 */
export function useBuddyLeaderboard(period: 'daily' | 'weekly' | 'monthly' = 'weekly') {
  return useQuery({
    queryKey: buddyStatsKeys.leaderboard(period),
    queryFn: () => buddyService.getLeaderboard(period),
    staleTime: period === 'daily' ? 60000 : 5 * 60 * 1000, // 1 min for daily, 5 min for others
    gcTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Combined hook that provides all buddy statistics
 */
export function useBuddyStatistics() {
  const myStats = useBuddyStats();
  const topBuddies = useTopBuddies();
  const dailyLeaderboard = useBuddyLeaderboard('daily');
  const weeklyLeaderboard = useBuddyLeaderboard('weekly');
  const monthlyLeaderboard = useBuddyLeaderboard('monthly');

  return {
    // Personal stats
    stats: myStats.data,
    isLoadingStats: myStats.isLoading,
    statsError: myStats.error,

    // Top buddies
    topBuddies: topBuddies.data || [],
    isLoadingTopBuddies: topBuddies.isLoading,
    topBuddiesError: topBuddies.error,

    // Leaderboards
    dailyLeaderboard: dailyLeaderboard.data || [],
    weeklyLeaderboard: weeklyLeaderboard.data || [],
    monthlyLeaderboard: monthlyLeaderboard.data || [],

    isLoadingLeaderboards:
      dailyLeaderboard.isLoading ||
      weeklyLeaderboard.isLoading ||
      monthlyLeaderboard.isLoading,

    leaderboardErrors: {
      daily: dailyLeaderboard.error,
      weekly: weeklyLeaderboard.error,
      monthly: monthlyLeaderboard.error,
    },

    // Helpers
    refetchStats: myStats.refetch,
    refetchTopBuddies: topBuddies.refetch,
    refetchLeaderboards: {
      daily: dailyLeaderboard.refetch,
      weekly: weeklyLeaderboard.refetch,
      monthly: monthlyLeaderboard.refetch,
    },

    // Computed values
    hasStats: !!myStats.data,
    totalSessionTime: myStats.data?.totalHours || 0,
    completionRate: myStats.data?.completionRate || 0,
    averageRating: myStats.data?.averageRating || 0,
    currentStreak: myStats.data?.currentStreak || 0,
    longestStreak: myStats.data?.longestStreak || 0,
    totalBuddies: myStats.data?.totalBuddies || 0,
    badges: myStats.data?.badges || [],
  };
}