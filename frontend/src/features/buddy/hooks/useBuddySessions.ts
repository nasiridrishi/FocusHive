import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type {
  BuddySession,
  CreateSessionRequest,
  SessionCheckInRequest,
  BuddyWebSocketEvent,
} from '@/contracts/buddy';
import { buddyService } from '../services/buddyService';

// Query keys factory
export const buddySessionKeys = {
  all: ['buddy', 'sessions'] as const,
  sessions: () => [...buddySessionKeys.all, 'list'] as const,
  session: (sessionId: string) => [...buddySessionKeys.all, sessionId] as const,
  activeSessions: () => [...buddySessionKeys.all, 'active'] as const,
  history: () => [...buddySessionKeys.all, 'history'] as const,
  userSessions: (userId: number) => [...buddySessionKeys.all, 'user', userId] as const,
};

/**
 * Hook to create a new buddy session
 */
export function useCreateSession() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateSessionRequest) =>
      buddyService.createSession(request),
    onSuccess: (newSession) => {
      // Add to active sessions if session is scheduled/active
      if (newSession.status === 'scheduled' || newSession.status === 'active') {
        queryClient.setQueryData<BuddySession[]>(
          buddySessionKeys.activeSessions(),
          (old) => {
            if (!old) return [newSession];
            return [newSession, ...old];
          }
        );
      }

      // Cache the individual session
      queryClient.setQueryData(buddySessionKeys.session(newSession.id), newSession);

      // Invalidate history to include new session
      queryClient.invalidateQueries({ queryKey: buddySessionKeys.history() });
    },
    onError: (error) => {
      console.error('Failed to create session:', error);
    },
  });
}

/**
 * Hook to get active buddy sessions
 */
export function useActiveSessions() {
  return useQuery({
    queryKey: buddySessionKeys.activeSessions(),
    queryFn: () => buddyService.getActiveSessions(),
    staleTime: 30000, // 30 seconds
    gcTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 60000, // Refetch every minute
  });
}

/**
 * Hook to get the current active session (if any)
 */
export function useActiveSession() {
  const activeSessions = useActiveSessions();

  return {
    ...activeSessions,
    data: activeSessions.data?.[0] || null, // Return first active session or null
  };
}

/**
 * Hook to get session history
 */
export function useSessionHistory() {
  return useQuery({
    queryKey: buddySessionKeys.history(),
    queryFn: () => buddyService.getSessionHistory(),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to get a specific session
 */
export function useBuddySession(sessionId: string) {
  const queryClient = useQueryClient();
  const [isSubscribed, setIsSubscribed] = useState(false);

  const query = useQuery({
    queryKey: buddySessionKeys.session(sessionId),
    queryFn: () => buddyService.getActiveSessions().then(sessions =>
      sessions.find(s => s.id === sessionId) || null
    ),
    staleTime: 30000,
    enabled: !!sessionId,
  });

  // Subscribe to real-time session updates
  useEffect(() => {
    if (sessionId && !isSubscribed) {
      const unsubscribe = buddyService.subscribeToSessionUpdates(
        sessionId,
        (event: BuddyWebSocketEvent) => {
          // Update session data based on WebSocket events
          queryClient.invalidateQueries({ queryKey: buddySessionKeys.session(sessionId) });

          // Also invalidate active sessions if this affects the active list
          if (event.type === 'session_started' || event.type === 'session_ended') {
            queryClient.invalidateQueries({ queryKey: buddySessionKeys.activeSessions() });
          }
        }
      );
      setIsSubscribed(true);

      return () => {
        unsubscribe();
        setIsSubscribed(false);
      };
    }
  }, [sessionId, queryClient, isSubscribed]);

  return query;
}

/**
 * Hook for session actions (start, pause, resume, end)
 */
export function useSessionActions() {
  const queryClient = useQueryClient();

  const startSession = useMutation({
    mutationFn: (sessionId: string) => buddyService.startSession(sessionId),
    onSuccess: (updatedSession) => {
      queryClient.setQueryData(buddySessionKeys.session(updatedSession.id), updatedSession);
      queryClient.invalidateQueries({ queryKey: buddySessionKeys.activeSessions() });
    },
  });

  const pauseSession = useMutation({
    mutationFn: (sessionId: string) => buddyService.pauseSession(sessionId),
    onSuccess: (updatedSession) => {
      queryClient.setQueryData(buddySessionKeys.session(updatedSession.id), updatedSession);
    },
  });

  const resumeSession = useMutation({
    mutationFn: (sessionId: string) => buddyService.resumeSession(sessionId),
    onSuccess: (updatedSession) => {
      queryClient.setQueryData(buddySessionKeys.session(updatedSession.id), updatedSession);
    },
  });

  const endSession = useMutation({
    mutationFn: (sessionId: string) => buddyService.endSession(sessionId),
    onSuccess: (completedSession) => {
      // Update the session
      queryClient.setQueryData(buddySessionKeys.session(completedSession.id), completedSession);

      // Remove from active sessions
      queryClient.setQueryData<BuddySession[]>(
        buddySessionKeys.activeSessions(),
        (old) => {
          if (!old) return old;
          return old.filter(session => session.id !== completedSession.id);
        }
      );

      // Invalidate history to include completed session
      queryClient.invalidateQueries({ queryKey: buddySessionKeys.history() });
    },
  });

  return {
    startSession: startSession.mutate,
    pauseSession: pauseSession.mutate,
    resumeSession: resumeSession.mutate,
    endSession: endSession.mutate,

    isStarting: startSession.isPending,
    isPausing: pauseSession.isPending,
    isResuming: resumeSession.isPending,
    isEnding: endSession.isPending,
  };
}

/**
 * Hook for session check-ins
 */
export function useSessionCheckIn() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, checkIn }: { sessionId: string; checkIn: SessionCheckInRequest }) =>
      buddyService.checkIn(sessionId, checkIn),
    onSuccess: (_, variables) => {
      // Invalidate the session to refresh check-ins
      queryClient.invalidateQueries({ queryKey: buddySessionKeys.session(variables.sessionId) });
    },
    onError: (error) => {
      console.error('Failed to check in:', error);
    },
  });
}

/**
 * Combined hook that provides comprehensive session management
 */
export function useBuddySessions() {
  const activeSessions = useActiveSessions();
  const sessionHistory = useSessionHistory();
  const createSession = useCreateSession();
  const sessionActions = useSessionActions();
  const checkIn = useSessionCheckIn();

  return {
    // Data
    activeSessions: activeSessions.data || [],
    sessionHistory: sessionHistory.data || [],
    currentSession: activeSessions.data?.[0] || null,

    // Loading states
    isLoadingActive: activeSessions.isLoading,
    isLoadingHistory: sessionHistory.isLoading,

    // Errors
    activeSessionsError: activeSessions.error,
    historyError: sessionHistory.error,

    // Actions
    createSession: createSession.mutate,
    checkIn: checkIn.mutate,
    ...sessionActions,

    // Action loading states
    isCreatingSession: createSession.isPending,
    isCheckingIn: checkIn.isPending,

    // Helpers
    refetchActive: activeSessions.refetch,
    refetchHistory: sessionHistory.refetch,
    hasActiveSession: (activeSessions.data?.length || 0) > 0,
  };
}