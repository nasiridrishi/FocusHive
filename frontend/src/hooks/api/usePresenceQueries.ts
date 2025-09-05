/**
 * Presence Query Hooks
 * 
 * Provides React Query hooks for real-time presence operations with:
 * - Real-time polling and updates
 * - Optimized caching for live data
 * - WebSocket integration compatibility
 * - Background sync
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { presenceApiService } from '@services/api';
import { queryKeys, STALE_TIMES, CACHE_TIMES } from '@lib/queryClient';
import type { 
  UserPresence,
  FocusSession,
  PresenceUpdate,
  HivePresenceInfo as _HivePresenceInfo 
} from '@shared/types/presence';

// ============================================================================
// QUERY HOOKS
// ============================================================================

/**
 * Get current user's presence status
 */
export const useMyPresence = () => {
  return useQuery({
    queryKey: [...queryKeys.presence.all, 'me'],
    queryFn: () => presenceApiService.getMyPresence(),
    staleTime: STALE_TIMES.REALTIME, // Always considered stale for real-time updates
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 10000, // Refetch every 10 seconds
    refetchIntervalInBackground: true, // Continue polling in background
    refetchOnWindowFocus: true,
    refetchOnReconnect: 'always',
    retry: (failureCount, error: unknown) => {
      // Don't retry too aggressively for presence data
      if (error?.response?.status >= 400 && error?.response?.status < 500) {
        return false;
      }
      return failureCount < 2;
    },
    meta: {
      description: 'Current user presence status',
      requiresAuth: true,
      realtime: true,
    },
  });
};

/**
 * Get specific user's presence
 */
export const useUserPresence = (userId: string, enabled = true) => {
  return useQuery({
    queryKey: queryKeys.presence.user(userId),
    queryFn: () => presenceApiService.getUserPresence(userId),
    enabled: enabled && !!userId,
    staleTime: STALE_TIMES.REALTIME,
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 15000, // Slightly less frequent for other users
    refetchIntervalInBackground: false, // Don't poll other users in background
    refetchOnWindowFocus: true,
    meta: {
      description: 'User presence status',
      requiresAuth: true,
      realtime: true,
    },
  });
};

/**
 * Get presence for all users in a hive
 */
export const useHivePresence = (hiveId: string, enabled = true) => {
  return useQuery({
    queryKey: queryKeys.presence.hive(hiveId),
    queryFn: () => presenceApiService.getHivePresence(hiveId),
    enabled: enabled && !!hiveId,
    staleTime: STALE_TIMES.REALTIME,
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 5000, // Very frequent for hive presence
    refetchIntervalInBackground: true,
    refetchOnWindowFocus: true,
    refetchOnReconnect: 'always',
    select: (data) => {
      // Sort by online status and activity time
      return [...data].sort((a, b) => {
        if (a.isOnline !== b.isOnline) {
          return a.isOnline ? -1 : 1; // Online users first
        }
        return new Date(b.lastActivity).getTime() - new Date(a.lastActivity).getTime();
      });
    },
    meta: {
      description: 'Hive member presence status',
      requiresAuth: true,
      realtime: true,
    },
  });
};

/**
 * Get multiple hive presence data (batch)
 */
export const useBatchHivePresence = (hiveIds: string[], enabled = true) => {
  return useQuery({
    queryKey: [...queryKeys.presence.all, 'hives-batch', hiveIds.sort()],
    queryFn: () => presenceApiService.getBatchHivePresence(hiveIds),
    enabled: enabled && hiveIds.length > 0,
    staleTime: STALE_TIMES.REALTIME,
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 8000, // Balance between freshness and performance
    refetchIntervalInBackground: false,
    meta: {
      description: 'Batch hive presence data',
      requiresAuth: true,
      realtime: true,
    },
  });
};

/**
 * Get current user's focus sessions
 */
export const useMyFocusSessions = (filters?: { 
  limit?: number; 
  status?: 'active' | 'completed' | 'all';
  hiveId?: string;
}) => {
  return useQuery({
    queryKey: [...queryKeys.presence.sessions(), filters],
    queryFn: () => presenceApiService.getMyFocusSessions(filters),
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.MEDIUM,
    refetchInterval: (data) => {
      // Refetch more frequently if there are active sessions
      const hasActiveSessions = data?.some(session => session.status === 'active');
      return hasActiveSessions ? 5000 : 30000;
    },
    refetchIntervalInBackground: true,
    meta: {
      description: 'User focus sessions',
      requiresAuth: true,
    },
  });
};

/**
 * Get focus sessions for a specific hive
 */
export const useHiveFocusSessions = (hiveId: string, enabled = true) => {
  return useQuery({
    queryKey: [...queryKeys.presence.sessions(hiveId)],
    queryFn: () => presenceApiService.getHiveFocusSessions(hiveId),
    enabled: enabled && !!hiveId,
    staleTime: STALE_TIMES.REALTIME,
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 5000, // Frequent updates for active sessions
    refetchIntervalInBackground: true,
    select: (data) => {
      // Group sessions by status and sort by start time
      return {
        active: data.filter(s => s.status === 'active').sort((a, b) => 
          new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
        ),
        completed: data.filter(s => s.status === 'completed').sort((a, b) => 
          new Date(b.endTime || 0).getTime() - new Date(a.endTime || 0).getTime()
        ),
        all: data,
      };
    },
    meta: {
      description: 'Hive focus sessions',
      requiresAuth: true,
      realtime: true,
    },
  });
};

/**
 * Get presence statistics for analytics
 */
export const usePresenceStats = (period: 'daily' | 'weekly' | 'monthly' = 'weekly') => {
  return useQuery({
    queryKey: [...queryKeys.presence.all, 'stats', period],
    queryFn: () => presenceApiService.getPresenceStats(period),
    staleTime: STALE_TIMES.STATIC_CONTENT,
    gcTime: CACHE_TIMES.LONG,
    refetchOnWindowFocus: false,
    meta: {
      description: 'Presence analytics',
      requiresAuth: true,
    },
  });
};

// ============================================================================
// MUTATION HOOKS
// ============================================================================

/**
 * Update user presence status
 */
export const useUpdatePresence = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (presenceData: Partial<PresenceUpdate>) => 
      presenceApiService.updatePresence(presenceData),
    mutationKey: ['presence', 'update'],

    onMutate: async (presenceData) => {
      // Cancel any outgoing refetches for my presence
      await queryClient.cancelQueries({ queryKey: [...queryKeys.presence.all, 'me'] });

      // Snapshot previous value
      const previousPresence = queryClient.getQueryData<UserPresence>([...queryKeys.presence.all, 'me']);

      // Optimistically update presence
      if (previousPresence) {
        const optimisticPresence = {
          ...previousPresence,
          ...presenceData,
          lastActivity: new Date(),
        };
        queryClient.setQueryData([...queryKeys.presence.all, 'me'], optimisticPresence);
      }

      return { previousPresence };
    },

    onSuccess: (updatedPresence) => {
      // Update with server response
      queryClient.setQueryData([...queryKeys.presence.all, 'me'], updatedPresence);
      
      // Invalidate related presence data
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.presence.all,
        exact: false,
      });
    },

    onError: (error, presenceData, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic update
      if (context?.previousPresence) {
        queryClient.setQueryData([...queryKeys.presence.all, 'me'], context.previousPresence);
      }
    },

    meta: {
      description: 'Update user presence',
      requiresAuth: true,
    },
  });
};

/**
 * Start focus session
 */
export const useStartFocusSession = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionData: { 
      hiveId?: string; 
      duration?: number; 
      activity?: string;
      isPublic?: boolean;
    }) => presenceApiService.startFocusSession(sessionData),
    mutationKey: ['presence', 'startSession'],

    onMutate: async (sessionData) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.presence.sessions() });
      
      // Create optimistic session
      const optimisticSession: Partial<FocusSession> = {
        id: 'temp-' + Date.now(),
        startTime: new Date(),
        status: 'active',
        activity: sessionData.activity || 'Focusing',
        hiveId: sessionData.hiveId,
        duration: sessionData.duration,
        isPublic: sessionData.isPublic ?? true,
      };

      // Add to sessions list
      queryClient.setQueryData(
        queryKeys.presence.sessions(),
        (old: FocusSession[] | undefined) => {
          return old ? [optimisticSession as FocusSession, ...old] : [optimisticSession as FocusSession];
        }
      );

      return { optimisticSession };
    },

    onSuccess: (newSession, sessionData) => {
      // Replace optimistic session with real one
      queryClient.setQueryData(
        queryKeys.presence.sessions(),
        (old: FocusSession[] | undefined) => {
          if (!old) return [newSession];
          return [newSession, ...old.filter(s => !s.id.toString().startsWith('temp-'))];
        }
      );

      // If session is in a hive, update hive sessions
      if (sessionData.hiveId) {
        queryClient.invalidateQueries({ 
          queryKey: queryKeys.presence.sessions(sessionData.hiveId) 
        });
      }

      // Update presence status to show as focusing
      queryClient.invalidateQueries({ 
        queryKey: [...queryKeys.presence.all, 'me'] 
      });
    },

    onError: (_error, _sessionData, _context) => {
      // Error handled by error boundary and toast notifications
      
      // Remove optimistic update
      queryClient.setQueryData(
        queryKeys.presence.sessions(),
        (old: FocusSession[] | undefined) => {
          if (!old) return old;
          return old.filter(s => !s.id.toString().startsWith('temp-'));
        }
      );
    },

    onSettled: (data, error, sessionData) => {
      // Always refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: queryKeys.presence.sessions() });
      if (sessionData.hiveId) {
        queryClient.invalidateQueries({ 
          queryKey: queryKeys.presence.sessions(sessionData.hiveId) 
        });
      }
    },

    meta: {
      description: 'Start focus session',
      requiresAuth: true,
    },
  });
};

/**
 * End focus session
 */
export const useEndFocusSession = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: string) => presenceApiService.endFocusSession(sessionId),
    mutationKey: ['presence', 'endSession'],

    onMutate: async (sessionId) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.presence.sessions() });

      // Snapshot previous value
      const previousSessions = queryClient.getQueryData<FocusSession[]>(queryKeys.presence.sessions());

      // Optimistically update session status
      if (previousSessions) {
        const updatedSessions = previousSessions.map(session => 
          session.id === sessionId 
            ? { ...session, status: 'completed' as const, endTime: new Date() }
            : session
        );
        queryClient.setQueryData(queryKeys.presence.sessions(), updatedSessions);
      }

      return { previousSessions, sessionId };
    },

    onSuccess: (completedSession) => {
      // Update with server response
      queryClient.setQueryData(
        queryKeys.presence.sessions(),
        (old: FocusSession[] | undefined) => {
          if (!old) return [completedSession];
          return old.map(session => 
            session.id === completedSession.id ? completedSession : session
          );
        }
      );

      // Update presence status
      queryClient.invalidateQueries({ 
        queryKey: [...queryKeys.presence.all, 'me'] 
      });

      // If session was in a hive, update hive sessions
      if (completedSession.hiveId) {
        queryClient.invalidateQueries({ 
          queryKey: queryKeys.presence.sessions(completedSession.hiveId) 
        });
      }
    },

    onError: (error, sessionId, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic update
      if (context?.previousSessions) {
        queryClient.setQueryData(queryKeys.presence.sessions(), context.previousSessions);
      }
    },

    meta: {
      description: 'End focus session',
      requiresAuth: true,
    },
  });
};

/**
 * Update focus session activity
 */
export const useUpdateFocusSession = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, updates }: { 
      sessionId: string; 
      updates: { activity?: string; isPublic?: boolean } 
    }) => presenceApiService.updateFocusSession(sessionId, updates),
    mutationKey: ['presence', 'updateSession'],

    onMutate: async ({ sessionId, updates }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.presence.sessions() });

      // Snapshot previous value
      const previousSessions = queryClient.getQueryData<FocusSession[]>(queryKeys.presence.sessions());

      // Optimistically update session
      if (previousSessions) {
        const updatedSessions = previousSessions.map(session => 
          session.id === sessionId ? { ...session, ...updates } : session
        );
        queryClient.setQueryData(queryKeys.presence.sessions(), updatedSessions);
      }

      return { previousSessions, sessionId };
    },

    onSuccess: (updatedSession) => {
      // Update with server response
      queryClient.setQueryData(
        queryKeys.presence.sessions(),
        (old: FocusSession[] | undefined) => {
          if (!old) return [updatedSession];
          return old.map(session => 
            session.id === updatedSession.id ? updatedSession : session
          );
        }
      );

      // Update hive sessions if applicable
      if (updatedSession.hiveId) {
        queryClient.invalidateQueries({ 
          queryKey: queryKeys.presence.sessions(updatedSession.hiveId) 
        });
      }
    },

    onError: (error, { sessionId: _sessionId }, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic update
      if (context?.previousSessions) {
        queryClient.setQueryData(queryKeys.presence.sessions(), context.previousSessions);
      }
    },

    meta: {
      description: 'Update focus session details',
      requiresAuth: true,
    },
  });
};

// ============================================================================
// COMPOUND HOOKS
// ============================================================================

/**
 * Complete presence data for current user
 */
export const useMyCompletePresence = () => {
  const presenceQuery = useMyPresence();
  const sessionsQuery = useMyFocusSessions({ limit: 10, status: 'all' });

  return {
    // Data
    presence: presenceQuery.data,
    sessions: sessionsQuery.data || [],
    
    // Computed values
    isOnline: presenceQuery.data?.isOnline || false,
    currentActivity: presenceQuery.data?.currentActivity,
    activeSession: sessionsQuery.data?.find(s => s.status === 'active'),
    
    // Loading states
    isLoading: presenceQuery.isLoading || sessionsQuery.isLoading,
    isPresenceLoading: presenceQuery.isLoading,
    isSessionsLoading: sessionsQuery.isLoading,
    
    // Error states
    error: presenceQuery.error || sessionsQuery.error,
    presenceError: presenceQuery.error,
    sessionsError: sessionsQuery.error,
    
    // Utilities
    refetchPresence: presenceQuery.refetch,
    refetchSessions: sessionsQuery.refetch,
    refetchAll: () => {
      presenceQuery.refetch();
      sessionsQuery.refetch();
    },
  };
};

/**
 * Focus session management hook
 */
export const useFocusSessionManagement = () => {
  const startMutation = useStartFocusSession();
  const endMutation = useEndFocusSession();
  const updateMutation = useUpdateFocusSession();

  return {
    // Mutations
    startSession: startMutation.mutate,
    endSession: endMutation.mutate,
    updateSession: updateMutation.mutate,
    
    // Loading states
    isStarting: startMutation.isPending,
    isEnding: endMutation.isPending,
    isUpdating: updateMutation.isPending,
    
    // Error states
    startError: startMutation.error,
    endError: endMutation.error,
    updateError: updateMutation.error,
    
    // Success states
    isStartSuccess: startMutation.isSuccess,
    isEndSuccess: endMutation.isSuccess,
    isUpdateSuccess: updateMutation.isSuccess,
  };
};

/**
 * Real-time hive presence with automatic updates
 */
export const useRealTimeHivePresence = (hiveId: string, enabled = true) => {
  const presenceQuery = useHivePresence(hiveId, enabled);
  const sessionsQuery = useHiveFocusSessions(hiveId, enabled);

  return {
    // Data
    presence: presenceQuery.data || [],
    sessions: sessionsQuery.data?.all || [],
    activeSessions: sessionsQuery.data?.active || [],
    
    // Computed values
    onlineCount: presenceQuery.data?.filter(p => p.isOnline).length || 0,
    focusingCount: sessionsQuery.data?.active.length || 0,
    totalMembers: presenceQuery.data?.length || 0,
    
    // Loading states
    isLoading: presenceQuery.isLoading || sessionsQuery.isLoading,
    
    // Error states
    error: presenceQuery.error || sessionsQuery.error,
    
    // Utilities
    refetch: () => {
      presenceQuery.refetch();
      sessionsQuery.refetch();
    },
  };
};