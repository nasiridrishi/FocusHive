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

// Extended types with computed properties  
export type {
  UserPresence,
  Hive,
  User,
  PresenceStatus,
  HiveMembershipStatus
} from './types';

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

// Import useAuth specifically for useApiHooks
import {useAuth as useAuthImported} from './useAuthQueries';
// Import useQueryClient for utility functions
import {useQueryClient} from '@tanstack/react-query';
// Import queryClient functions for cacheUtils
// Import all required items from lib/queryClient
import {
  CACHE_TIMES as CACHE_TIMES_IMPORTED,
  invalidateQueries as invalidateQueriesImported,
  prefetchQueries as prefetchQueriesImported,
  queryClient as queryClientImported,
  queryKeys as queryKeysImported,
  STALE_TIMES as STALE_TIMES_IMPORTED
} from '../../lib/queryClient';

// Hook composition utilities
export const useApiHooks = () => {
  const auth = useAuthImported();
  return {
    auth,
    // Add more composite hooks as needed
  };
};

// Development utilities
export const useQueryDevtools = () => {
  const queryClientInstance = useQueryClient();

  return {
    // Get query cache information
    getCacheInfo: () => ({
      queries: queryClientInstance.getQueryCache().getAll().length,
      mutations: queryClientInstance.getMutationCache().getAll().length,
    }),

    // Clear specific cache sections
    clearAuthCache: () => queryClientInstance.removeQueries({queryKey: ['auth']}),
    clearHiveCache: () => queryClientInstance.removeQueries({queryKey: ['hives']}),
    clearPresenceCache: () => queryClientInstance.removeQueries({queryKey: ['presence']}),

    // Force refetch all queries
    refetchAll: () => queryClientInstance.refetchQueries(),

    // Reset entire cache
    resetCache: () => queryClientInstance.clear(),
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
      prefetchQueriesImported.userData(),
      // Add more common prefetch operations
    ];

    await Promise.allSettled(promises);
  },

  // Clear cache on logout
  clearUserData: () => {
    invalidateQueriesImported.auth();
    invalidateQueriesImported.user();
    // Clear other user-specific data
  },

  // Background sync for offline support
  syncOfflineData: async () => {
    // Implementation for offline data synchronization
    // This would handle queued mutations and data sync
  },
};

// Performance monitoring hooks (must be used within React components)
export const useSlowQueries = (threshold = 1000) => {
  const queryClientInstance = useQueryClient();
  return queryClientInstance
  .getQueryCache()
  .getAll()
  .filter(query => {
    const state = query.state;
    // Use dataUpdatedAt and fetchFailureCount as proxy for slow queries
    const lastFetch = state.dataUpdatedAt ? Date.now() - state.dataUpdatedAt : 0;
    return lastFetch > threshold;
  })
  .map(query => ({
    queryKey: query.queryKey,
    duration: query.state.dataUpdatedAt ? Date.now() - query.state.dataUpdatedAt : 0,
    dataSize: JSON.stringify(query.state.data).length,
  }));
};

export const useCacheSize = (): number => {
  const queryClientInstance = useQueryClient();
  const allData = queryClientInstance.getQueryCache().getAll().map(q => q.state.data);
  return JSON.stringify(allData).length;
};

export const useOptimizeCache = () => {
  const queryClientInstance = useQueryClient();
  return () => queryClientInstance.getQueryCache().clear();
};

// Default export with commonly used utilities
export default {
  // Utilities
  queryClient: queryClientImported,
  queryKeys: queryKeysImported,
  invalidateQueries: invalidateQueriesImported,
  prefetchQueries: prefetchQueriesImported,
  cacheUtils,
  CACHE_TIMES: CACHE_TIMES_IMPORTED,
  STALE_TIMES: STALE_TIMES_IMPORTED,
};