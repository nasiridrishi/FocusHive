/**
 * Enhanced TanStack Query Configuration
 * 
 * Provides comprehensive API caching with:
 * - Tiered caching strategies (short, medium, long-term)
 * - Offline persistence and sync
 * - Optimistic updates
 * - Background refetching
 * - Error handling and retry policies
 */

import { QueryClient, QueryCache, MutationCache } from '@tanstack/react-query';
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister';
import { persistQueryClient } from '@tanstack/react-query-persist-client';

// Cache time constants (in milliseconds)
export const CACHE_TIMES = {
  // Short-term cache: User data, preferences, session-sensitive data
  SHORT: 5 * 60 * 1000,        // 5 minutes
  
  // Medium-term cache: Static content, configurations, reference data that changes occasionally
  MEDIUM: 2 * 60 * 60 * 1000,  // 2 hours
  
  // Long-term cache: Static reference data, app configuration, rarely changing content
  LONG: 24 * 60 * 60 * 1000,   // 24 hours
  
  // Immediate cache: Real-time data that should refetch immediately
  IMMEDIATE: 0,                 // 0 milliseconds
  
  // Static cache: Data that never changes during session
  STATIC: Infinity,
} as const;

// Stale times for different data types
export const STALE_TIMES = {
  // User data becomes stale quickly
  USER_DATA: 1 * 60 * 1000,    // 1 minute
  
  // Static content can be stale for longer
  STATIC_CONTENT: 10 * 60 * 1000, // 10 minutes
  
  // Reference data stays fresh longer
  REFERENCE_DATA: 30 * 60 * 1000, // 30 minutes
  
  // Real-time data should always be considered stale
  REALTIME: 0,
  
  // Session data moderate staleness
  SESSION_DATA: 5 * 60 * 1000,  // 5 minutes
} as const;

// Retry configuration for different types of operations
const RETRY_CONFIG = {
  // Default retry for most queries
  DEFAULT: {
    retry: (failureCount: number, error: unknown) => {
      // Don't retry on 4xx errors (client errors)
      const hasResponse = error && typeof error === 'object' && 'response' in error;
      const status = hasResponse ? (error as { response: { status?: number } }).response?.status : undefined;
      if (status >= 400 && status < 500) {
        return false;
      }
      // Retry up to 3 times for 5xx errors and network errors
      return failureCount < 3;
    },
    retryDelay: (attemptIndex: number) => Math.min(1000 * 2 ** attemptIndex, 30000),
  },
  
  // More aggressive retry for critical operations
  CRITICAL: {
    retry: (failureCount: number, error: unknown) => {
      const hasResponse = error && typeof error === 'object' && 'response' in error;
      const status = hasResponse ? (error as { response: { status?: number } }).response?.status : undefined;
      if (status >= 400 && status < 500) {
        return false;
      }
      return failureCount < 5;
    },
    retryDelay: (attemptIndex: number) => Math.min(500 * 2 ** attemptIndex, 30000),
  },
  
  // No retry for mutations by default
  MUTATION: {
    retry: 0,
  },
} as const;

// Create enhanced query cache with logging and error handling
const queryCache = new QueryCache({
  onError: (error, query) => {
    // Track error for analytics
    if (typeof window !== 'undefined' && 'gtag' in window) {
      // @ts-expect-error - gtag is loaded by Google Analytics script
      window.gtag('event', 'query_error', {
        query_key: query.queryKey.join('_'),
        error_message: error.message,
      });
    }
  },
  onSuccess: (_data, _query) => {
    // Query success tracked via analytics only
  },
});

// Create mutation cache with logging
const mutationCache = new MutationCache({
  onError: (error, _variables, _context, _mutation) => {
    // Track mutation errors
    if (typeof window !== 'undefined' && 'gtag' in window) {
      // @ts-expect-error - gtag is loaded by Google Analytics script
      window.gtag('event', 'mutation_error', {
        mutation_key: _mutation.options.mutationKey?.join('_') || 'unknown',
        error_message: error.message,
      });
    }
  },
  onSuccess: (_data, _variables, _context, _mutation) => {
    // Mutation success tracked via analytics only
  },
});

// Create the QueryClient with enhanced configuration
export const queryClient = new QueryClient({
  queryCache,
  mutationCache,
  defaultOptions: {
    queries: {
      // Default cache time - can be overridden per query
      gcTime: CACHE_TIMES.MEDIUM,
      
      // Default stale time
      staleTime: STALE_TIMES.SESSION_DATA,
      
      // Retry configuration
      ...RETRY_CONFIG.DEFAULT,
      
      // Refetch configuration
      refetchOnWindowFocus: false,  // Disable aggressive refetching
      refetchOnReconnect: 'always', // Always refetch on reconnect
      refetchOnMount: true,         // Refetch on component mount if stale
      
      // Network mode configuration
      networkMode: 'online',
      
      // Error handling
      throwOnError: false,          // Handle errors in components, not globally
      
      // Performance optimizations
      structuralSharing: true,      // Optimize re-renders
    },
    mutations: {
      // Default mutation configuration
      ...RETRY_CONFIG.MUTATION,
      
      // Network mode for mutations
      networkMode: 'online',
      
      // Error handling for mutations
      throwOnError: false,
    },
  },
});

// Create persister for offline support
export const localStoragePersister = createSyncStoragePersister({
  storage: typeof window !== 'undefined' ? window.localStorage : undefined,
  key: 'focushive-query-cache',
  serialize: JSON.stringify,
  deserialize: JSON.parse,
  
  // Handle persistence errors (e.g., storage quota exceeded)
  retry: (props) => {
    const { error, errorCount } = props;
    
    // Query persistence failed, attempting recovery
    
    // Try to free up space by removing old queries
    if (errorCount < 3 && error.message.includes('quota')) {
      try {
        // Remove oldest cached data
        const cacheKeys = Object.keys(localStorage).filter(key => 
          key.startsWith('focushive-query-cache')
        );
        
        if (cacheKeys.length > 0) {
          // Remove the first (oldest) cache entry
          localStorage.removeItem(cacheKeys[0]);
          return props.persistedClient; // Retry with same data
        }
      } catch (cleanupError) {
        // Cache cleanup failed - quota issue persists
      }
    }
    
    // Give up after 3 attempts
    return undefined;
  },
});

// Initialize persistence
if (typeof window !== 'undefined') {
  persistQueryClient({
    queryClient,
    persister: localStoragePersister,
    maxAge: CACHE_TIMES.LONG, // Keep persisted data for 24 hours max
    buster: 'focushive-v1',   // Cache buster for major app updates
    
    // Dehydration options - what to persist
    dehydrateOptions: {
      shouldDehydrateQuery: (query) => {
        // Only persist successful queries that aren't real-time
        return query.state.status === 'success' && 
               query.queryKey[0] !== 'presence' && // Don't persist real-time data
               query.queryKey[0] !== 'websocket';   // Don't persist websocket data
      },
    },
    
    // Hydration options - how to restore
    hydrateOptions: {
      // Add metadata to hydrated queries
      defaultOptions: {
        queries: {
          meta: {
            hydrated: true,
          },
        },
      },
    },
  });
}

// Query key factories for consistent key management
export const queryKeys = {
  // Authentication queries
  auth: {
    all: ['auth'] as const,
    user: () => [...queryKeys.auth.all, 'user'] as const,
    profile: () => [...queryKeys.auth.all, 'profile'] as const,
    permissions: () => [...queryKeys.auth.all, 'permissions'] as const,
  },
  
  // Hive queries
  hives: {
    all: ['hives'] as const,
    list: (filters?: unknown) => [...queryKeys.hives.all, 'list', filters] as const,
    detail: (id: string) => [...queryKeys.hives.all, 'detail', id] as const,
    members: (id: string) => [...queryKeys.hives.all, 'members', id] as const,
    search: (query: string) => [...queryKeys.hives.all, 'search', query] as const,
  },
  
  // Presence queries
  presence: {
    all: ['presence'] as const,
    user: (userId: string) => [...queryKeys.presence.all, 'user', userId] as const,
    hive: (hiveId: string) => [...queryKeys.presence.all, 'hive', hiveId] as const,
    sessions: (hiveId?: string) => [...queryKeys.presence.all, 'sessions', hiveId] as const,
  },
  
  // Timer queries
  timer: {
    all: ['timer'] as const,
    current: () => [...queryKeys.timer.all, 'current'] as const,
    history: (filters?: unknown) => [...queryKeys.timer.all, 'history', filters] as const,
    stats: (period: 'daily' | 'weekly' | 'monthly') => [...queryKeys.timer.all, 'stats', period] as const,
    settings: () => [...queryKeys.timer.all, 'settings'] as const,
  },
  
  // Analytics queries
  analytics: {
    all: ['analytics'] as const,
    userStats: (userId?: string) => [...queryKeys.analytics.all, 'user-stats', userId] as const,
    hiveLeaderboard: (hiveId: string) => [...queryKeys.analytics.all, 'leaderboard', hiveId] as const,
    insights: (period: string) => [...queryKeys.analytics.all, 'insights', period] as const,
  },
  
  // Chat queries
  chat: {
    all: ['chat'] as const,
    messages: (hiveId: string, params?: unknown) => [...queryKeys.chat.all, 'messages', hiveId, params] as const,
    recent: (hiveId: string) => [...queryKeys.chat.all, 'recent', hiveId] as const,
  },
  
  // Notifications queries
  notifications: {
    all: ['notifications'] as const,
    list: (filters?: unknown) => [...queryKeys.notifications.all, 'list', filters] as const,
    unread: () => [...queryKeys.notifications.all, 'unread'] as const,
    count: () => [...queryKeys.notifications.all, 'count'] as const,
  },
  
  // Buddy system queries
  buddy: {
    all: ['buddy'] as const,
    relationships: () => [...queryKeys.buddy.all, 'relationships'] as const,
    matches: () => [...queryKeys.buddy.all, 'matches'] as const,
    goals: (relationshipId: string) => [...queryKeys.buddy.all, 'goals', relationshipId] as const,
    sessions: (relationshipId: string) => [...queryKeys.buddy.all, 'sessions', relationshipId] as const,
    stats: () => [...queryKeys.buddy.all, 'stats'] as const,
  },
  
  // Music queries
  music: {
    all: ['music'] as const,
    recommendations: (filters?: unknown) => [...queryKeys.music.all, 'recommendations', filters] as const,
    playlists: () => [...queryKeys.music.all, 'playlists'] as const,
    current: () => [...queryKeys.music.all, 'current'] as const,
  },
  
  // Forum queries
  forum: {
    all: ['forum'] as const,
    posts: (filters?: unknown) => [...queryKeys.forum.all, 'posts', filters] as const,
    categories: () => [...queryKeys.forum.all, 'categories'] as const,
    post: (id: string) => [...queryKeys.forum.all, 'post', id] as const,
  },
} as const;

// Cache invalidation helpers
export const invalidateQueries = {
  // Invalidate all auth-related queries
  auth: () => queryClient.invalidateQueries({ queryKey: queryKeys.auth.all }),
  
  // Invalidate specific hive data
  hive: (hiveId: string) => {
    queryClient.invalidateQueries({ queryKey: queryKeys.hives.detail(hiveId) });
    queryClient.invalidateQueries({ queryKey: queryKeys.hives.members(hiveId) });
    queryClient.invalidateQueries({ queryKey: queryKeys.presence.hive(hiveId) });
    queryClient.invalidateQueries({ queryKey: queryKeys.chat.messages(hiveId) });
  },
  
  // Invalidate user-specific data
  user: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.auth.user() });
    queryClient.invalidateQueries({ queryKey: queryKeys.timer.current() });
    queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
    queryClient.invalidateQueries({ queryKey: queryKeys.buddy.all });
  },
  
  // Invalidate all presence data (for real-time updates)
  presence: () => queryClient.invalidateQueries({ queryKey: queryKeys.presence.all }),
  
  // Invalidate all notifications
  notifications: () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
};

// Prefetch helpers for common data
export const prefetchQueries = {
  // Prefetch user data on login
  userData: async () => {
    await Promise.allSettled([
      queryClient.prefetchQuery({
        queryKey: queryKeys.auth.user(),
        staleTime: STALE_TIMES.USER_DATA,
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.notifications.count(),
        staleTime: STALE_TIMES.SESSION_DATA,
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.timer.current(),
        staleTime: STALE_TIMES.SESSION_DATA,
      }),
    ]);
  },
  
  // Prefetch hive data when navigating
  hiveData: async (hiveId: string) => {
    await Promise.allSettled([
      queryClient.prefetchQuery({
        queryKey: queryKeys.hives.detail(hiveId),
        staleTime: STALE_TIMES.STATIC_CONTENT,
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.hives.members(hiveId),
        staleTime: STALE_TIMES.SESSION_DATA,
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.presence.hive(hiveId),
        staleTime: STALE_TIMES.REALTIME,
      }),
    ]);
  },
};

export default queryClient;