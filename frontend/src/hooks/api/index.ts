/**
 * API Query Hooks Index
 * 
 * Centralized exports for all TanStack Query hooks with:
 * - Organized by domain/service
 * - TypeScript support
 * - Performance optimizations
 */

// Authentication hooks
export {
  useCurrentUser,
  useAuthStatus,
  useUserPermissions,
  useLogin,
  useRegister,
  useLogout,
  useUpdateProfile,
  useChangePassword,
  useRequestPasswordReset,
  useAuth,
  usePermissions,
} from './useAuthQueries';

// Hive management hooks
export {
  useHives,
  useInfiniteHives,
  useHive,
  useHiveMembers,
  useSearchHives,
  useRecommendedHives,
  useCreateHive,
  useUpdateHive,
  useDeleteHive,
  useJoinHive,
  useLeaveHive,
  useHiveDetails,
  useHiveManagement,
} from './useHiveQueries';

// Presence and real-time hooks
export {
  useMyPresence,
  useUserPresence,
  useHivePresence,
  useBatchHivePresence,
  useMyFocusSessions,
  useHiveFocusSessions,
  usePresenceStats,
  useUpdatePresence,
  useStartFocusSession,
  useEndFocusSession,
  useUpdateFocusSession,
  useMyCompletePresence,
  useFocusSessionManagement,
  useRealTimeHivePresence,
} from './usePresenceQueries';

// Timer and productivity hooks (to be implemented)
export type {
  // Placeholder for timer hooks
  UseTimerHooks,
  UseProductivityHooks,
} from './useTimerQueries';

// Analytics hooks (to be implemented)
export type {
  // Placeholder for analytics hooks
  UseAnalyticsHooks,
} from './useAnalyticsQueries';

// Chat hooks (to be implemented)
export type {
  // Placeholder for chat hooks
  UseChatHooks,
} from './useChatQueries';

// Notifications hooks (to be implemented)
export type {
  // Placeholder for notifications hooks
  UseNotificationHooks,
} from './useNotificationQueries';

// Buddy system hooks (to be implemented)
export type {
  // Placeholder for buddy hooks
  UseBuddyHooks,
} from './useBuddyQueries';

// Music hooks (to be implemented)
export type {
  // Placeholder for music hooks
  UseMusicHooks,
} from './useMusicQueries';

// Forum hooks (to be implemented)
export type {
  // Placeholder for forum hooks
  UseForumHooks,
} from './useForumQueries';

// Query client and configuration
export {
  queryClient,
  queryKeys,
  invalidateQueries,
  prefetchQueries,
  CACHE_TIMES,
  STALE_TIMES,
} from '../../lib/queryClient';

// Re-export TanStack Query utilities for convenience
export {
  useQuery,
  useMutation,
  useQueryClient,
  useInfiniteQuery,
  useQueries,
  useSuspenseQuery,
  useMutationState,
} from '@tanstack/react-query';

// Hook composition utilities
export const useApiHooks = () => ({
  auth: useAuth(),
  // Add more composite hooks as needed
});

// Development utilities
export const useQueryDevtools = () => {
  const queryClient = useQueryClient();
  
  return {
    // Get query cache information
    getCacheInfo: () => ({
      queries: queryClient.getQueryCache().getAll().length,
      mutations: queryClient.getMutationCache().getAll().length,
    }),
    
    // Clear specific cache sections
    clearAuthCache: () => queryClient.removeQueries({ queryKey: ['auth'] }),
    clearHiveCache: () => queryClient.removeQueries({ queryKey: ['hives'] }),
    clearPresenceCache: () => queryClient.removeQueries({ queryKey: ['presence'] }),
    
    // Force refetch all queries
    refetchAll: () => queryClient.refetchQueries(),
    
    // Reset entire cache
    resetCache: () => queryClient.clear(),
  };
};

// TypeScript helper types
export type QueryHookResult<T> = {
  data: T | undefined;
  isLoading: boolean;
  isFetching: boolean;
  isError: boolean;
  error: Error | null;
  refetch: () => void;
};

export type MutationHookResult<TData, TVariables> = {
  mutate: (variables: TVariables) => void;
  mutateAsync: (variables: TVariables) => Promise<TData>;
  isPending: boolean;
  isError: boolean;
  isSuccess: boolean;
  error: Error | null;
  data: TData | undefined;
  reset: () => void;
};

// Cache management utilities
export const cacheUtils = {
  // Prefetch common data on app initialization
  prefetchCommonData: async () => {
    const promises = [
      prefetchQueries.userData(),
      // Add more common prefetch operations
    ];
    
    await Promise.allSettled(promises);
  },
  
  // Clear cache on logout
  clearUserData: () => {
    invalidateQueries.auth();
    invalidateQueries.user();
    // Clear other user-specific data
  },
  
  // Background sync for offline support
  syncOfflineData: async () => {
    // Implementation for offline data synchronization
    // This would handle queued mutations and data sync
  },
};

// Performance monitoring  
export const queryPerformanceUtils = {
  // Monitor query performance
  useSlowQueries: (threshold = 1000) => {
    const queryClient = useQueryClient();
    return queryClient
      .getQueryCache()
      .getAll()
      .filter(query => {
        const state = query.state;
        return state.fetchMeta?.duration && state.fetchMeta.duration > threshold;
      })
      .map(query => ({
        queryKey: query.queryKey,
        duration: query.state.fetchMeta?.duration,
        dataSize: JSON.stringify(query.state.data).length,
      }));
  },
  
  // Get cache memory usage estimate
  useCacheSize: () => {
    const queryClient = useQueryClient();
    const allData = queryClient.getQueryCache().getAll().map(q => q.state.data);
    return JSON.stringify(allData).length;
  },
  
  // Optimize cache by removing stale queries
  useOptimizeCache: () => {
    const queryClient = useQueryClient();
    queryClient.getQueryCache().clear();
  },
};

// Default export with commonly used hooks
export default {
  // Authentication
  useAuth,
  useCurrentUser,
  useLogin,
  useLogout,
  
  // Hives
  useHives,
  useHive,
  useHiveDetails,
  useHiveManagement,
  
  // Presence
  useMyPresence,
  useHivePresence,
  useFocusSessionManagement,
  useRealTimeHivePresence,
  
  // Utilities
  queryClient,
  queryKeys,
  invalidateQueries,
  cacheUtils,
  queryPerformanceUtils,
};