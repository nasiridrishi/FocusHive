import { useQuery, useMutation, useQueryClient, UseQueryResult } from '@tanstack/react-query';
import { useCallback, useEffect, useState, useRef } from 'react';
import { presenceService } from '../services/presenceService';
import type {
  UserPresence,
  HivePresence,
  PresenceStatus,
  UserActivity,
  SetPresenceRequest,
  BulkPresenceRequest,
  PresenceSearchParams,
  PresenceHistoryEntry,
  PresenceStatistics,
  PresenceUpdate,
  CollaborationSession,
} from '@/contracts/presence';
import type { PaginatedResponse } from '@/contracts/common';

// Query keys for cache management
const QUERY_KEYS = {
  all: ['presence'] as const,
  user: (userId: string | number) => [...QUERY_KEYS.all, 'user', String(userId)] as const,
  hive: (hiveId: string | number) => [...QUERY_KEYS.all, 'hive', String(hiveId)] as const,
  bulk: (userIds: (string | number)[]) => [...QUERY_KEYS.all, 'bulk', userIds.map(String)] as const,
  search: (params: PresenceSearchParams) => [...QUERY_KEYS.all, 'search', params] as const,
  history: (userId: string | number, date?: string) => [...QUERY_KEYS.all, 'history', String(userId), date] as const,
  statistics: (userId: string | number, period?: string) => [...QUERY_KEYS.all, 'statistics', String(userId), period] as const,
  collaboration: (sessionId: string) => [...QUERY_KEYS.all, 'collaboration', sessionId] as const,
};

/**
 * Hook to manage user presence
 */
export function useUserPresence(userId: string | number | undefined) {
  const queryClient = useQueryClient();
  const [realtimePresence, setRealtimePresence] = useState<UserPresence | null>(null);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  // Fetch user presence
  const presenceQuery = useQuery({
    queryKey: QUERY_KEYS.user(userId!),
    queryFn: () => presenceService.getUserPresence(Number(userId!)),
    enabled: !!userId,
    staleTime: 1000 * 30, // 30 seconds
    gcTime: 1000 * 60, // 1 minute
  });

  // Set presence mutation
  const setPresenceMutation = useMutation({
    mutationFn: (request: SetPresenceRequest) =>
      presenceService.setPresence(request),
    onSuccess: (data) => {
      // Update cache with new presence
      queryClient.setQueryData(QUERY_KEYS.user(userId!), data);
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: [...QUERY_KEYS.all, 'hive'] });
    },
  });

  // Subscribe to real-time presence updates
  useEffect(() => {
    if (!userId) return;

    // Subscribe to presence updates via WebSocket
    // NOTE: This requires WebSocket integration
    const wsService = (window as any).webSocketService;
    if (wsService?.subscribe) {
      unsubscribeRef.current = wsService.subscribe(
        `/topic/presence/${userId}`,
        (update: PresenceUpdate) => {
          if (String(update.userId) === String(userId)) {
          const newPresence: UserPresence = {
            userId: String(update.userId),
            username: presenceQuery.data?.username || 'Unknown',
            avatar: presenceQuery.data?.avatar,
            status: update.status,
            focusLevel: 'available',
            lastSeen: new Date().toISOString(),
            lastActivity: new Date().toISOString(),
            device: presenceQuery.data?.device || {
              type: 'web',
              id: 'browser-' + Date.now()
            },
            metadata: presenceQuery.data?.metadata,
          };
          setRealtimePresence(newPresence);
          // Update cache
            queryClient.setQueryData(QUERY_KEYS.user(userId), newPresence);
          }
        }
      );
    }

    return () => {
      unsubscribeRef.current?.();
    };
  }, [userId, queryClient, presenceQuery.data]);

  // Combined presence (realtime or cached)
  const presence = realtimePresence || presenceQuery.data;

  return {
    presence,
    isLoading: presenceQuery.isLoading,
    error: presenceQuery.error,
    setPresence: setPresenceMutation.mutate,
    isUpdating: setPresenceMutation.isPending,
  };
}

/**
 * Hook to manage hive presence
 */
export function useHivePresence(hiveId: string | number | undefined) {
  const queryClient = useQueryClient();
  const [realtimeUpdates, setRealtimeUpdates] = useState<Map<string, UserPresence>>(new Map());
  const unsubscribeRef = useRef<(() => void) | null>(null);

  // Fetch hive presence
  const hivePresenceQuery = useQuery({
    queryKey: QUERY_KEYS.hive(hiveId!),
    queryFn: () => presenceService.getHivePresence(Number(hiveId!)),
    enabled: !!hiveId,
    staleTime: 1000 * 30, // 30 seconds
    gcTime: 1000 * 60, // 1 minute
    refetchInterval: 1000 * 60, // Refetch every minute
  });

  // Subscribe to real-time hive presence updates
  useEffect(() => {
    if (!hiveId) return;

    // Subscribe to hive presence updates via WebSocket
    const wsService = (window as any).webSocketService;
    if (wsService?.subscribe) {
      unsubscribeRef.current = wsService.subscribe(
        `/topic/hive/${hiveId}/presence`,
        (update: PresenceUpdate) => {
          setRealtimeUpdates(prev => {
          const updated = new Map(prev);
          const userPresence: UserPresence = {
            userId: update.userId,
            username: update.username || '',
            status: update.status,
            focusLevel: 'available',
            lastSeen: new Date().toISOString(),
            lastActivity: new Date().toISOString(),
            device: {
              type: 'web',
              id: 'browser-' + Date.now()
            }
          };
          updated.set(String(update.userId), userPresence);
          return updated;
        });

        // Invalidate hive presence to trigger refetch
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.hive(hiveId) });
        }
      );
    }

    return () => {
      unsubscribeRef.current?.();
    };
  }, [hiveId, queryClient]);

  // Merge realtime updates with cached data
  const mergedPresence = hivePresenceQuery.data
    ? {
        ...hivePresenceQuery.data,
        activeUsers: hivePresenceQuery.data.activeUsers.map(user => {
          const realtimeUser = realtimeUpdates.get(String(user.userId));
          return realtimeUser || user;
        }),
      }
    : null;

  return {
    hivePresence: mergedPresence,
    activeUsers: mergedPresence?.activeUsers || [],
    onlineCount: mergedPresence?.onlineCount || 0,
    isLoading: hivePresenceQuery.isLoading,
    error: hivePresenceQuery.error,
    refetch: hivePresenceQuery.refetch,
  };
}

/**
 * Hook to manage user activity
 */
export function useActivity() {
  const queryClient = useQueryClient();

  // Start activity mutation
  const startActivityMutation = useMutation({
    mutationFn: (activity: UserActivity) =>
      presenceService.setPresence({ status: 'busy', activity }),
    onSuccess: () => {
      // Invalidate presence queries to reflect new activity
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.all });
    },
  });

  // End activity mutation
  const endActivityMutation = useMutation({
    mutationFn: () => presenceService.setPresence({ status: 'online' }),
    onSuccess: () => {
      // Invalidate presence queries
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.all });
    },
  });

  return {
    startActivity: startActivityMutation.mutate,
    endActivity: endActivityMutation.mutate,
    isStarting: startActivityMutation.isPending,
    isEnding: endActivityMutation.isPending,
  };
}

/**
 * Hook to get bulk presence
 */
export function useBulkPresence(userIds: (string | number)[]) {
  const bulkPresenceQuery = useQuery({
    queryKey: QUERY_KEYS.bulk(userIds),
    queryFn: () => presenceService.getBulkPresence(userIds.map(Number)),
    enabled: userIds.length > 0,
    staleTime: 1000 * 30, // 30 seconds
  });

  return {
    presences: bulkPresenceQuery.data?.presences || [],
    isLoading: bulkPresenceQuery.isLoading,
    error: bulkPresenceQuery.error,
  };
}

/**
 * Hook to search presence
 */
export function usePresenceSearch(params: PresenceSearchParams) {
  const searchQuery = useQuery({
    queryKey: QUERY_KEYS.search(params),
    // Search presence is not directly implemented, use getBulkPresence
    queryFn: async () => {
      // For now, return empty results for search
      // This would need backend implementation
      return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
    },
    enabled: !!params,
    staleTime: 1000 * 30, // 30 seconds
  });

  return {
    results: searchQuery.data?.content || [],
    totalResults: searchQuery.data?.totalElements || 0,
    isSearching: searchQuery.isLoading,
    error: searchQuery.error,
  };
}

/**
 * Hook to get presence history
 */
export function usePresenceHistory(userId: string | number | undefined, date?: string) {
  const historyQuery = useQuery({
    queryKey: QUERY_KEYS.history(userId!, date),
    queryFn: () => {
      const endDate = date || new Date().toISOString();
      const startDate = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
      return presenceService.getPresenceHistory(Number(userId!), startDate, endDate);
    },
    enabled: !!userId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  return {
    history: historyQuery.data || [],
    isLoading: historyQuery.isLoading,
    error: historyQuery.error,
  };
}

/**
 * Hook to get presence statistics
 */
export function usePresenceStatistics(userId: string | number | undefined, period?: string) {
  const statisticsQuery = useQuery({
    queryKey: QUERY_KEYS.statistics(userId!, period),
    queryFn: () => presenceService.getStatistics(Number(userId!)),
    enabled: !!userId,
    staleTime: 1000 * 60 * 10, // 10 minutes
  });

  return {
    statistics: statisticsQuery.data,
    isLoading: statisticsQuery.isLoading,
    error: statisticsQuery.error,
  };
}

/**
 * Hook to manage collaboration sessions
 */
export function useCollaboration(sessionId?: string) {
  const queryClient = useQueryClient();
  const [currentSession, setCurrentSession] = useState<CollaborationSession | null>(null);

  // Get collaboration session
  const sessionQuery = useQuery({
    queryKey: QUERY_KEYS.collaboration(sessionId!),
    // Get collaboration session is not directly implemented
    queryFn: async () => {
      // This would need backend implementation
      return null as any;
    },
    enabled: !!sessionId,
    staleTime: 1000 * 30, // 30 seconds
  });

  // Create collaboration session mutation
  const createSessionMutation = useMutation({
    mutationFn: (hiveId: number) =>
      presenceService.createCollaborationSession(
        hiveId,
        { type: 'STUDY_SESSION', duration: 60 }
      ),
    onSuccess: (data) => {
      setCurrentSession(data);
      queryClient.setQueryData(QUERY_KEYS.collaboration(data.id), data);
    },
  });

  // Join collaboration session mutation
  const joinSessionMutation = useMutation({
    mutationFn: async (sessionId: string) => {
      await presenceService.joinCollaborationSession(sessionId);
      return { id: sessionId, userId: '', startedAt: new Date().toISOString(), plannedDuration: 0, focusLevel: 'available', breaks: [] } as CollaborationSession;
    },
    onSuccess: (data) => {
      setCurrentSession(data);
      queryClient.setQueryData(QUERY_KEYS.collaboration(data.id), data);
    },
  });

  // Leave collaboration session mutation
  const leaveSessionMutation = useMutation({
    mutationFn: (sessionId: string) =>
      presenceService.leaveCollaborationSession(sessionId),
    onSuccess: () => {
      setCurrentSession(null);
      if (sessionId) {
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.collaboration(sessionId) });
      }
    },
  });

  return {
    session: currentSession || sessionQuery.data,
    isLoading: sessionQuery.isLoading,
    error: sessionQuery.error,
    createSession: createSessionMutation.mutate,
    joinSession: joinSessionMutation.mutate,
    leaveSession: leaveSessionMutation.mutate,
    isCreating: createSessionMutation.isPending,
    isJoining: joinSessionMutation.isPending,
    isLeaving: leaveSessionMutation.isPending,
  };
}

/**
 * Hook to automatically manage presence
 * Handles heartbeats, page visibility, and idle detection
 */
export function useAutoPresence(userId: string | number | undefined, enabled = true) {
  const { setPresence } = useUserPresence(userId);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout>();
  const idleTimeoutRef = useRef<NodeJS.Timeout>();
  const lastActivityRef = useRef<number>(Date.now());

  // Track user activity
  const handleActivity = useCallback(() => {
    lastActivityRef.current = Date.now();

    // Clear idle timeout
    if (idleTimeoutRef.current) {
      clearTimeout(idleTimeoutRef.current);
    }

    // Set new idle timeout (5 minutes)
    idleTimeoutRef.current = setTimeout(() => {
      presenceService.setPresence({ status: 'away' });
    }, 5 * 60 * 1000);
  }, []);

  // Handle page visibility changes
  useEffect(() => {
    if (!enabled || !userId) return;

    const handleVisibilityChange = () => {
      if (document.hidden) {
        setPresence({ status: 'away' });
      } else {
        setPresence({ status: 'online' });
        handleActivity();
      }
    };

    // Page visibility tracking is handled internally

    // Listen for visibility changes
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // Track user activity
    const events = ['mousedown', 'keydown', 'scroll', 'touchstart'];
    events.forEach(event => {
      document.addEventListener(event, handleActivity);
    });

    // Start heartbeat
    const sendHeartbeat = () => {
      // Heartbeat is handled automatically by the service
      // Just set online status to keep presence active
      presenceService.setPresence({ status: 'online' }).catch(error => {
        console.error('Failed to update presence:', error);
      });
    };

    sendHeartbeat(); // Send initial heartbeat
    heartbeatIntervalRef.current = setInterval(sendHeartbeat, 30000); // 30 seconds

    // Set initial activity timeout
    handleActivity();

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      events.forEach(event => {
        document.removeEventListener(event, handleActivity);
      });

      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }
      if (idleTimeoutRef.current) {
        clearTimeout(idleTimeoutRef.current);
      }
    };
  }, [enabled, userId, setPresence, handleActivity]);
}

/**
 * Hook for managing presence in a specific context
 */
export function useContextPresence(context: 'hive' | 'global', contextId?: string | number) {
  const queryClient = useQueryClient();
  const [onlineUsers, setOnlineUsers] = useState<UserPresence[]>([]);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (context === 'hive' && !contextId) return;

    const channel = context === 'hive' 
      ? `/topic/hive/${contextId}/presence`
      : '/topic/presence';

    // Subscribe via WebSocket
    const wsService = (window as any).webSocketService;
    if (wsService?.subscribe) {
      unsubscribeRef.current = wsService.subscribe(
        channel,
        (update: PresenceUpdate) => {
          setOnlineUsers(prev => {
            const filtered = prev.filter(u => u.userId !== update.userId);

            if (update.status !== 'offline') {
              const userPresence: UserPresence = {
                userId: update.userId,
                username: update.username || '',
                status: update.status,
                focusLevel: 'available',
                lastSeen: new Date().toISOString(),
                lastActivity: new Date().toISOString(),
                device: {
                  type: 'web',
                  id: 'browser-' + Date.now()
                }
              };
              return [...filtered, userPresence];
            }

            return filtered;
          });
        }
      );
    }

    return () => {
      unsubscribeRef.current?.();
    };
  }, [context, contextId]);

  return {
    onlineUsers,
    onlineCount: onlineUsers.length,
  };
}