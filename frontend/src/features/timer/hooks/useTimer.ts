import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient, UseQueryResult, UseMutationResult } from '@tanstack/react-query';
import { timerService } from '../services/timerService';
import type {
  TimerSession,
  CreateTimerRequest,
  TimerStats,
  TimerPreferences,
} from '@/contracts/timer';
import type { PaginatedResponse } from '@/contracts/common';

/**
 * Custom hook for managing active timer session
 * Provides real-time updates via WebSocket
 */
export function useActiveTimer() {
  const queryClient = useQueryClient();
  const [isSubscribed, setIsSubscribed] = useState(false);

  // Query for active timer session
  const query = useQuery({
    queryKey: ['timer', 'active'],
    queryFn: () => timerService.getCurrentTimer(),
    refetchInterval: 30000, // Refetch every 30 seconds as backup
    staleTime: 10000, // Consider data stale after 10 seconds
  });

  // Subscribe to real-time updates
  useEffect(() => {
    if (query.data?.id && !isSubscribed) {
      const unsubscribe = timerService.subscribeToTimerUpdates(
        query.data.id,
        (updatedSession) => {
          queryClient.setQueryData(['timer', 'active'], updatedSession);
        }
      );
      setIsSubscribed(true);

      return () => {
        unsubscribe();
        setIsSubscribed(false);
      };
    }
  }, [query.data?.id, queryClient, isSubscribed]);

  return query;
}

/**
 * Hook for starting a new timer session
 */
export function useStartTimer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: CreateTimerRequest) => {
      const response = await timerService.startTimer(request);
      return response.session;
    },
    onSuccess: (session) => {
      // Update active session cache
      queryClient.setQueryData(['timer', 'active'], session);

      // Invalidate history to include new session
      queryClient.invalidateQueries({ queryKey: ['timer', 'history'] });

      // Invalidate statistics
      queryClient.invalidateQueries({ queryKey: ['timer', 'statistics'] });
    },
    onError: (error) => {
      console.error('Failed to start timer session:', error);
    },
  });
}

/**
 * Hook for pausing the active timer
 */
export function usePauseTimer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: number) => timerService.pauseTimer(sessionId),
    onSuccess: (session) => {
      queryClient.setQueryData(['timer', 'active'], session);
    },
    onError: (error) => {
      console.error('Failed to pause timer:', error);
    },
  });
}

/**
 * Hook for resuming a paused timer
 */
export function useResumeTimer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: number) => timerService.resumeTimer(sessionId),
    onSuccess: (session) => {
      queryClient.setQueryData(['timer', 'active'], session);
    },
    onError: (error) => {
      console.error('Failed to resume timer:', error);
    },
  });
}

/**
 * Hook for completing a timer session
 */
export function useCompleteTimer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: number) => timerService.stopTimer(sessionId),
    onSuccess: () => {
      // Clear active session
      queryClient.setQueryData(['timer', 'active'], null);

      // Invalidate history and statistics
      queryClient.invalidateQueries({ queryKey: ['timer', 'history'] });
      queryClient.invalidateQueries({ queryKey: ['timer', 'statistics'] });
    },
    onError: (error) => {
      console.error('Failed to complete timer:', error);
    },
  });
}

/**
 * Hook for canceling a timer session
 */
export function useCancelTimer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: number) => timerService.cancelTimer(sessionId),
    onSuccess: () => {
      // Clear active session
      queryClient.setQueryData(['timer', 'active'], null);

      // Invalidate history
      queryClient.invalidateQueries({ queryKey: ['timer', 'history'] });
    },
    onError: (error) => {
      console.error('Failed to cancel timer:', error);
    },
  });
}

/**
 * Hook for fetching timer session history
 */
export function useTimerHistory(
  page = 0,
  size = 20
): UseQueryResult<PaginatedResponse<TimerSession>, Error> {
  return useQuery({
    queryKey: ['timer', 'history', page, size],
    queryFn: async () => {
      const response = await timerService.getTimerHistory({ page, pageSize: size });
      return {
        content: response.sessions.map(entry => entry.session),
        totalElements: response.pagination.total,
        totalPages: Math.ceil(response.pagination.total / size),
        number: page
      } as PaginatedResponse<TimerSession>;
    },
    staleTime: 60000, // Consider data stale after 1 minute
    gcTime: 5 * 60 * 1000, // Keep in cache for 5 minutes
  });
}

/**
 * Hook for fetching timer statistics
 */
export function useTimerStatistics(): UseQueryResult<TimerStats, Error> {
  return useQuery({
    queryKey: ['timer', 'statistics'],
    queryFn: () => timerService.getTimerStatistics(),
    staleTime: 5 * 60 * 1000, // Consider data stale after 5 minutes
    gcTime: 10 * 60 * 1000, // Keep in cache for 10 minutes
  });
}

/**
 * Hook for managing timer preferences
 * TODO: Implement preferences methods in TimerService
 */
export function useTimerPreferences(): UseQueryResult<TimerPreferences, Error> {
  return useQuery({
    queryKey: ['timer', 'preferences'],
    queryFn: async () => {
      // TODO: Implement getPreferences in timerService
      return {} as TimerPreferences;
    },
    staleTime: 10 * 60 * 1000, // Consider data stale after 10 minutes
    gcTime: 30 * 60 * 1000, // Keep in cache for 30 minutes
    enabled: false, // Disabled until implemented
  });
}

/**
 * Hook for updating timer preferences
 * TODO: Implement preferences methods in TimerService
 */
export function useUpdateTimerPreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (preferences: Partial<TimerPreferences>) => {
      // TODO: Implement updatePreferences in timerService
      return preferences as TimerPreferences;
    },
    onSuccess: (updatedPreferences) => {
      queryClient.setQueryData(['timer', 'preferences'], updatedPreferences);
    },
    onError: (error) => {
      console.error('Failed to update timer preferences:', error);
    },
  });
}

/**
 * Combined timer hook that provides all timer functionality
 * Useful for components that need comprehensive timer control
 */
export function useTimer() {
  const activeTimer = useActiveTimer();
  const startTimer = useStartTimer();
  const pauseTimer = usePauseTimer();
  const resumeTimer = useResumeTimer();
  const completeTimer = useCompleteTimer();
  const cancelTimer = useCancelTimer();
  const [localTime, setLocalTime] = useState<number | null>(null);

  // Local timer countdown for UI updates
  useEffect(() => {
    if (activeTimer.data?.status === 'active' && activeTimer.data.remainingTime) {
      setLocalTime(activeTimer.data.remainingTime);

      const interval = setInterval(() => {
        setLocalTime((prev) => {
          if (prev === null || prev <= 0) {
            clearInterval(interval);
            return 0;
          }
          return prev - 1000;
        });
      }, 1000);

      return () => clearInterval(interval);
    } else {
      setLocalTime(null);
    }
  }, [activeTimer.data]);

  // Helper functions
  const formatTime = useCallback((milliseconds: number): string => {
    const totalSeconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }, []);

  const getProgress = useCallback((): number => {
    if (!activeTimer.data) return 0;
    const { duration, remainingTime } = activeTimer.data;
    if (!duration || !remainingTime) return 0;
    return ((duration - remainingTime) / duration) * 100;
  }, [activeTimer.data]);

  return {
    // Data
    session: activeTimer.data,
    isLoading: activeTimer.isLoading,
    error: activeTimer.error,
    remainingTime: localTime ?? activeTimer.data?.remainingTime ?? 0,
    formattedTime: formatTime(localTime ?? activeTimer.data?.remainingTime ?? 0),
    progress: getProgress(),
    isActive: activeTimer.data?.status === 'active',
    isPaused: activeTimer.data?.status === 'paused',

    // Actions
    start: startTimer.mutate,
    pause: pauseTimer.mutate,
    resume: resumeTimer.mutate,
    complete: completeTimer.mutate,
    cancel: cancelTimer.mutate,

    // Loading states for actions
    isStarting: startTimer.isPending,
    isPausing: pauseTimer.isPending,
    isResuming: resumeTimer.isPending,
    isCompleting: completeTimer.isPending,
    isCanceling: cancelTimer.isPending,
  };
}

/**
 * Hook for subscribing to timer notifications
 */
export function useTimerNotifications(
  onNotification: (notification: any) => void,
  enabled = true
) {
  useEffect(() => {
    if (!enabled) return;

    // TODO: Implement notification subscription in timerService
    // For now, return empty cleanup function
    const unsubscribe = () => {};
    // const unsubscribe = timerService.subscribeToTimerNotifications(onNotification);
    return unsubscribe;
  }, [onNotification, enabled]);
}