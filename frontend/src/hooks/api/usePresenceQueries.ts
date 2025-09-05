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
import { presenceApiService, FocusSession as ApiFocusSession, PresenceUpdate as _ApiPresenceUpdate } from '@services/api/presenceApi';
import { queryKeys, STALE_TIMES, CACHE_TIMES } from '@lib/queryClient';
import { transformPresenceDTO, transformHiveDTO as _transformHiveDTO, type PresenceDTO, type HiveDTO as _HiveDTO } from './transformers';
import { useAuth } from './useAuthQueries';
import type { 
  UserPresence,
  PresenceStatus as _PresenceStatus
} from './types';
import type { 
  FocusSession,
  PresenceUpdate,
  HivePresenceInfo as _HivePresenceInfo,
} from '@shared/types/presence';

// ============================================================================
// QUERY HOOKS
// ============================================================================

/**
 * Get current user's presence status
 */
export const useMyPresence = () => {
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: [...queryKeys.presence.all, 'me'],
    queryFn: async () => {
      const dto = await presenceApiService.getMyPresence();
      return transformPresenceDTO(dto as unknown as PresenceDTO, currentUserId);
    },
    staleTime: STALE_TIMES.REALTIME, // Always considered stale for real-time updates
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 10000, // Refetch every 10 seconds
    refetchIntervalInBackground: true, // Continue polling in background
    refetchOnWindowFocus: true,
    refetchOnReconnect: 'always',
    retry: (failureCount, error: unknown) => {
      // Don't retry too aggressively for presence data  
      const axiosError = error as { response?: { status?: number } };
      if (axiosError?.response?.status >= 400 && axiosError?.response?.status < 500) {
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
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: queryKeys.presence.user(userId),
    queryFn: async () => {
      const dto = await presenceApiService.getUserPresence(parseInt(userId, 10));
      return transformPresenceDTO(dto as unknown as PresenceDTO, currentUserId);
    },
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
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: queryKeys.presence.hive(hiveId),
    queryFn: async () => {
      const response = await presenceApiService.getHivePresence(parseInt(hiveId, 10));
      // Transform the response which contains an array of presence DTOs
      const transformedPresences = (response as { presences?: unknown[] }).presences?.map((dto: unknown) => 
        transformPresenceDTO(dto as unknown as PresenceDTO, currentUserId)
      ) || [];
      return { presences: transformedPresences };
    },
    enabled: enabled && !!hiveId,
    staleTime: STALE_TIMES.REALTIME,
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 5000, // Very frequent for hive presence
    refetchIntervalInBackground: true,
    refetchOnWindowFocus: true,
    refetchOnReconnect: 'always',
    select: (data) => {
      // Sort by online status and activity time
      return data.presences.sort((a, b) => {
        const aOnline = a.status === 'online';
        const bOnline = b.status === 'online';
        if (aOnline !== bOnline) {
          return aOnline ? -1 : 1; // Online users first
        }
        return new Date(b.lastSeen).getTime() - new Date(a.lastSeen).getTime();
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
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: [...queryKeys.presence.all, 'hives-batch', hiveIds.sort()],
    queryFn: async () => {
      const response = await presenceApiService.getHivePresenceBatch(hiveIds.map(id => parseInt(id, 10)));
      // Transform the batch response - assuming it's an array of hive presence objects
      return (response as unknown[]).map((hivePresence: unknown) => ({
        ...(hivePresence as object),
        presences: (hivePresence as { presences?: unknown[] }).presences?.map((dto: unknown) => 
          transformPresenceDTO(dto as unknown as PresenceDTO, currentUserId)
        ) || []
      }));
    },
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
    queryFn: async () => {
      const sessions = await presenceApiService.getMySessions();
      // For now, return sessions as-is since they're already transformed by the API service
      // TODO: Create FocusSession transformer if needed
      return sessions;
    },
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.MEDIUM,
    refetchInterval: 30000, // Fixed interval for now
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
    queryFn: async () => {
      const sessions = await presenceApiService.getHiveSessions(parseInt(hiveId, 10));
      // For now, return sessions as-is since they're already transformed by the API service
      // TODO: Create FocusSession transformer if needed
      return sessions;
    },
    enabled: enabled && !!hiveId,
    staleTime: STALE_TIMES.REALTIME,
    gcTime: CACHE_TIMES.SHORT,
    refetchInterval: 5000, // Frequent updates for active sessions
    refetchIntervalInBackground: true,
    select: (data) => {
      // Group sessions by active status and sort by start time
      return {
        active: data.filter(s => s.isActive).sort((a, b) => 
          new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
        ),
        completed: data.filter(s => !s.isActive).sort((a, b) => 
          new Date(b.endTime || b.startTime).getTime() - new Date(a.endTime || a.startTime).getTime()
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
    queryFn: async () => {
      // For now, just call the API without parameters
      // TODO: Update API to support period parameters or add hiveId if needed
      const stats = await presenceApiService.getPresenceStats();
      // For now, return stats as-is since they're already transformed by the API service
      // TODO: Create PresenceStats transformer if needed
      return stats;
    },
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
    mutationFn: (presenceData: PresenceUpdate) => 
      presenceApiService.updateMyPresence(presenceData as unknown as _ApiPresenceUpdate),
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
    }) => {
      const focusSessionData: Omit<ApiFocusSession, 'id' | 'startTime' | 'isActive'> = {
        userId: 0, // Will be set by the API service
        hiveId: sessionData.hiveId ? parseInt(sessionData.hiveId, 10) : undefined,
        sessionType: 'FOCUS',
        targetDuration: sessionData.duration,
        task: sessionData.activity,
        endTime: undefined,
        actualDuration: undefined,
        metadata: { isPublic: sessionData.isPublic }
      };
      return presenceApiService.startFocusSession(focusSessionData);
    },
    mutationKey: ['presence', 'startSession'],

    onMutate: async (sessionData) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.presence.sessions() });
      
      // Create optimistic session
      const optimisticSession: unknown = {
        id: 'temp-' + Date.now(),
        startTime: new Date().toISOString(),
        task: sessionData.activity || 'Focusing', // Using 'task' instead of 'activity'
        hiveId: sessionData.hiveId ? parseInt(sessionData.hiveId, 10) : undefined,
        targetDuration: sessionData.duration,
        isActive: true,
      };

      // Add to sessions list
      queryClient.setQueryData(
        queryKeys.presence.sessions(),
        (old: FocusSession[] | undefined) => {
          return old ? [optimisticSession, ...old] : [optimisticSession];
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
    mutationFn: (sessionId: string) => presenceApiService.endFocusSession(parseInt(sessionId, 10)),
    mutationKey: ['presence', 'endSession'],

    onMutate: async (sessionId) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.presence.sessions() });

      // Snapshot previous value
      const previousSessions = queryClient.getQueryData<FocusSession[]>(queryKeys.presence.sessions());

      // Optimistically update session status
      if (previousSessions) {
        const updatedSessions = previousSessions.map(session => 
          session.id.toString() === sessionId 
            ? { ...session, endTime: new Date() }
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
            session.id.toString() === completedSession.id.toString() ? completedSession : session
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
          queryKey: queryKeys.presence.sessions(completedSession.hiveId.toString()) 
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
    mutationFn: ({ sessionId: _sessionId, updates: _updates }: { 
      sessionId: string; 
      updates: { activity?: string; isPublic?: boolean } 
    }) => {
      // Since updateFocusSession doesn't exist, we'll end and start a new session
      // This is a temporary solution - should implement proper update method
      throw new Error('Update focus session not implemented yet');
    },
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

    onSuccess: (updatedSession: FocusSession) => {
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
          queryKey: queryKeys.presence.sessions(updatedSession.hiveId.toString()) 
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
    isOnline: presenceQuery.data?.status === 'online' || false,
    currentActivity: presenceQuery.data?.currentActivity,
    activeSession: sessionsQuery.data?.find(s => s.isActive),
    
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
    onlineCount: presenceQuery.data?.filter(p => p.status === 'online').length || 0,
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