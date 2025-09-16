import { useQuery, useMutation, useQueryClient, UseQueryResult } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { analyticsService } from '../services/analyticsService';
import type {
  FocusSession,
  DailyAnalytics,
  WeeklyAnalytics,
  MonthlyAnalytics,
  ProductivityGoal,
  Leaderboard,
  AnalyticsInsight,
  ChartConfig,
  HeatmapData,
  ProductivityPattern,
  Streak,
  BurnoutRisk,
  GetAnalyticsRequest,
  GetLeaderboardRequest,
  ExportAnalyticsRequest,
  AnalyticsWebSocketEvent,
  MetricType,
  AggregationPeriod,
} from '@/contracts/analytics';

// Query keys for cache management
const QUERY_KEYS = {
  all: ['analytics'] as const,
  daily: (userId: number, date?: string) => [...QUERY_KEYS.all, 'daily', userId, date] as const,
  weekly: (userId: number, weekStart?: string) => [...QUERY_KEYS.all, 'weekly', userId, weekStart] as const,
  monthly: (userId: number, month?: number, year?: number) => [...QUERY_KEYS.all, 'monthly', userId, month, year] as const,
  goals: (userId: number) => [...QUERY_KEYS.all, 'goals', userId] as const,
  insights: (userId: number) => [...QUERY_KEYS.all, 'insights', userId] as const,
  streaks: (userId: number) => [...QUERY_KEYS.all, 'streaks', userId] as const,
  leaderboard: (request: GetLeaderboardRequest) => [...QUERY_KEYS.all, 'leaderboard', request] as const,
  heatmap: (userId: number, year: number) => [...QUERY_KEYS.all, 'heatmap', userId, year] as const,
  patterns: (userId: number) => [...QUERY_KEYS.all, 'patterns', userId] as const,
  burnout: (userId: number) => [...QUERY_KEYS.all, 'burnout', userId] as const,
  charts: (type: string, period: AggregationPeriod) => [...QUERY_KEYS.all, 'charts', type, period] as const,
};

/**
 * Hook for focus session management
 */
export function useFocusSession() {
  const queryClient = useQueryClient();
  const [currentSession, setCurrentSession] = useState<FocusSession | null>(null);
  const sessionTimerRef = useRef<NodeJS.Timeout>();

  // Start session mutation
  const startSessionMutation = useMutation({
    mutationFn: (params: {
      type: 'POMODORO' | 'DEEP_WORK' | 'REGULAR' | 'BREAK';
      taskName?: string;
      hiveId?: number;
      plannedDuration?: number;
    }) => analyticsService.startFocusSession(params),
    onSuccess: (session) => {
      setCurrentSession(session);
      // Invalidate analytics queries to reflect new session
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.all });
    },
  });

  // End session mutation
  const endSessionMutation = useMutation({
    mutationFn: ({ sessionId, completed }: { sessionId: number; completed: boolean }) =>
      analyticsService.endFocusSession(sessionId, completed),
    onSuccess: () => {
      setCurrentSession(null);
      if (sessionTimerRef.current) {
        clearTimeout(sessionTimerRef.current);
      }
      // Invalidate analytics queries
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.all });
    },
  });

  // Pause session mutation
  const pauseSessionMutation = useMutation({
    mutationFn: (sessionId: number) => analyticsService.pauseFocusSession(sessionId),
    onSuccess: (session) => {
      setCurrentSession(session);
    },
  });

  // Resume session mutation
  const resumeSessionMutation = useMutation({
    mutationFn: (sessionId: number) => analyticsService.resumeFocusSession(sessionId),
    onSuccess: (session) => {
      setCurrentSession(session);
    },
  });

  // Track interruption
  const trackInterruptionMutation = useMutation({
    mutationFn: ({ sessionId, reason }: { sessionId: number; reason?: string }) =>
      analyticsService.trackInterruption(sessionId, reason),
  });

  return {
    currentSession,
    startSession: startSessionMutation.mutate,
    endSession: endSessionMutation.mutate,
    pauseSession: pauseSessionMutation.mutate,
    resumeSession: resumeSessionMutation.mutate,
    trackInterruption: trackInterruptionMutation.mutate,
    isStarting: startSessionMutation.isPending,
    isEnding: endSessionMutation.isPending,
  };
}

/**
 * Hook for daily analytics
 */
export function useDailyAnalytics(userId: number | undefined, date?: string) {
  const analyticsQuery = useQuery({
    queryKey: QUERY_KEYS.daily(userId!, date),
    queryFn: () => analyticsService.getDailyAnalytics(userId!, date),
    enabled: !!userId,
    staleTime: 1000 * 60 * 5, // 5 minutes
    refetchInterval: 1000 * 60 * 5, // Refetch every 5 minutes
  });

  return {
    analytics: analyticsQuery.data,
    isLoading: analyticsQuery.isLoading,
    error: analyticsQuery.error,
    refetch: analyticsQuery.refetch,
  };
}

/**
 * Hook for weekly analytics
 */
export function useWeeklyAnalytics(userId: number | undefined, weekStart?: string) {
  const analyticsQuery = useQuery({
    queryKey: QUERY_KEYS.weekly(userId!, weekStart),
    queryFn: () => analyticsService.getWeeklyAnalytics(userId!, weekStart),
    enabled: !!userId,
    staleTime: 1000 * 60 * 5,
  });

  return {
    analytics: analyticsQuery.data,
    isLoading: analyticsQuery.isLoading,
    error: analyticsQuery.error,
    trend: analyticsQuery.data?.productivityTrend,
    consistencyScore: analyticsQuery.data?.consistencyScore,
  };
}

/**
 * Hook for monthly analytics
 */
export function useMonthlyAnalytics(
  userId: number | undefined,
  month?: number,
  year?: number
) {
  const analyticsQuery = useQuery({
    queryKey: QUERY_KEYS.monthly(userId!, month, year),
    queryFn: () => analyticsService.getMonthlyAnalytics(userId!, month, year),
    enabled: !!userId,
    staleTime: 1000 * 60 * 5,
  });

  return {
    analytics: analyticsQuery.data,
    isLoading: analyticsQuery.isLoading,
    error: analyticsQuery.error,
    topHives: analyticsQuery.data?.topHives,
    topTasks: analyticsQuery.data?.topTasks,
  };
}

/**
 * Hook for productivity goals
 */
export function useProductivityGoals(userId: number | undefined) {
  const queryClient = useQueryClient();

  // Get active goals
  const goalsQuery = useQuery({
    queryKey: QUERY_KEYS.goals(userId!),
    queryFn: () => analyticsService.getActiveGoals(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 2,
    refetchInterval: 1000 * 60 * 2, // Refetch every 2 minutes
  });

  // Create goal mutation
  const createGoalMutation = useMutation({
    mutationFn: (params: {
      type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
      metric: MetricType;
      target: number;
    }) => analyticsService.createGoal(params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.goals(userId!) });
    },
  });

  // Update goal progress mutation
  const updateProgressMutation = useMutation({
    mutationFn: ({ goalId, progress }: { goalId: number; progress: number }) =>
      analyticsService.updateGoalProgress(goalId, progress),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.goals(userId!) });
    },
  });

  // Delete goal mutation
  const deleteGoalMutation = useMutation({
    mutationFn: (goalId: number) => analyticsService.deleteGoal(goalId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.goals(userId!) });
    },
  });

  return {
    goals: goalsQuery.data || [],
    isLoading: goalsQuery.isLoading,
    error: goalsQuery.error,
    createGoal: createGoalMutation.mutate,
    updateProgress: updateProgressMutation.mutate,
    deleteGoal: deleteGoalMutation.mutate,
    isCreating: createGoalMutation.isPending,
    isUpdating: updateProgressMutation.isPending,
    isDeleting: deleteGoalMutation.isPending,
  };
}

/**
 * Hook for insights
 */
export function useInsights(userId: number | undefined) {
  const queryClient = useQueryClient();

  const insightsQuery = useQuery({
    queryKey: QUERY_KEYS.insights(userId!),
    queryFn: () => analyticsService.getInsights(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 10, // 10 minutes
  });

  const dismissInsightMutation = useMutation({
    mutationFn: (insightId: string) => analyticsService.dismissInsight(insightId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.insights(userId!) });
    },
  });

  return {
    insights: insightsQuery.data || [],
    isLoading: insightsQuery.isLoading,
    error: insightsQuery.error,
    dismissInsight: dismissInsightMutation.mutate,
    actionableInsights: insightsQuery.data?.filter(i => i.actionable) || [],
    highPriorityInsights: insightsQuery.data?.filter(i => i.importance === 'HIGH') || [],
  };
}

/**
 * Hook for streaks
 */
export function useStreaks(userId: number | undefined) {
  const queryClient = useQueryClient();

  const streaksQuery = useQuery({
    queryKey: QUERY_KEYS.streaks(userId!),
    queryFn: () => analyticsService.getStreaks(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 5,
  });

  const updateStreakMutation = useMutation({
    mutationFn: (type: 'DAILY_LOGIN' | 'FOCUS_GOAL' | 'TASK_COMPLETION' | 'HIVE_PARTICIPATION') =>
      analyticsService.updateStreak(userId!, type),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.streaks(userId!) });
    },
  });

  const currentLoginStreak = streaksQuery.data?.find(s => s.type === 'DAILY_LOGIN')?.currentStreak || 0;
  const longestStreak = Math.max(...(streaksQuery.data?.map(s => s.longestStreak) || [0]));

  return {
    streaks: streaksQuery.data || [],
    isLoading: streaksQuery.isLoading,
    error: streaksQuery.error,
    updateStreak: updateStreakMutation.mutate,
    currentLoginStreak,
    longestStreak,
  };
}

/**
 * Hook for leaderboard
 */
export function useLeaderboard(request: GetLeaderboardRequest) {
  const leaderboardQuery = useQuery({
    queryKey: QUERY_KEYS.leaderboard(request),
    queryFn: () => analyticsService.getLeaderboard(request),
    enabled: !!request,
    staleTime: 1000 * 60 * 2,
  });

  const currentUserEntry = leaderboardQuery.data?.entries.find(e => e.isCurrentUser);

  return {
    leaderboard: leaderboardQuery.data,
    entries: leaderboardQuery.data?.entries || [],
    isLoading: leaderboardQuery.isLoading,
    error: leaderboardQuery.error,
    currentUserRank: currentUserEntry?.rank,
    currentUserScore: currentUserEntry?.score,
  };
}

/**
 * Hook for activity heatmap
 */
export function useActivityHeatmap(userId: number | undefined, year: number) {
  const heatmapQuery = useQuery({
    queryKey: QUERY_KEYS.heatmap(userId!, year),
    queryFn: () => analyticsService.getActivityHeatmap(userId!, year),
    enabled: !!userId,
    staleTime: 1000 * 60 * 10,
  });

  return {
    heatmap: heatmapQuery.data,
    isLoading: heatmapQuery.isLoading,
    error: heatmapQuery.error,
    totalActivity: heatmapQuery.data?.totalValue || 0,
  };
}

/**
 * Hook for productivity patterns
 */
export function useProductivityPatterns(userId: number | undefined) {
  const patternsQuery = useQuery({
    queryKey: QUERY_KEYS.patterns(userId!),
    queryFn: () => analyticsService.detectPatterns(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 30, // 30 minutes
  });

  const timeOfDayPatterns = patternsQuery.data?.filter(p => p.patternType === 'TIME_OF_DAY') || [];
  const bestTimes = timeOfDayPatterns[0]?.data.bestTimes || [];

  return {
    patterns: patternsQuery.data || [],
    isLoading: patternsQuery.isLoading,
    error: patternsQuery.error,
    bestProductivityTimes: bestTimes,
    highConfidencePatterns: patternsQuery.data?.filter(p => p.confidence > 0.8) || [],
  };
}

/**
 * Hook for burnout risk assessment
 */
export function useBurnoutRisk(userId: number | undefined) {
  const burnoutQuery = useQuery({
    queryKey: QUERY_KEYS.burnout(userId!),
    queryFn: () => analyticsService.assessBurnoutRisk(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 60, // 1 hour
  });

  return {
    risk: burnoutQuery.data,
    riskLevel: burnoutQuery.data?.riskLevel,
    recommendations: burnoutQuery.data?.recommendations || [],
    isLoading: burnoutQuery.isLoading,
    error: burnoutQuery.error,
    isHighRisk: burnoutQuery.data?.riskLevel === 'HIGH',
  };
}

/**
 * Hook for chart data
 */
export function useChartData(
  chartType: string,
  period: AggregationPeriod,
  userId?: number
) {
  const chartQuery = useQuery({
    queryKey: QUERY_KEYS.charts(chartType, period),
    queryFn: () => analyticsService.getChartData(chartType, period, userId),
    enabled: !!chartType && !!period,
    staleTime: 1000 * 60 * 5,
  });

  return {
    chart: chartQuery.data,
    isLoading: chartQuery.isLoading,
    error: chartQuery.error,
  };
}

/**
 * Hook for exporting analytics
 */
export function useExportAnalytics() {
  const exportMutation = useMutation({
    mutationFn: (request: ExportAnalyticsRequest) =>
      analyticsService.exportAnalytics(request),
  });

  return {
    exportAnalytics: exportMutation.mutate,
    isExporting: exportMutation.isPending,
    exportResult: exportMutation.data,
    error: exportMutation.error,
  };
}

/**
 * Hook for today's quick stats
 */
export function useTodayStats(userId: number | undefined) {
  const statsQuery = useQuery({
    queryKey: [...QUERY_KEYS.all, 'today-stats', userId],
    queryFn: () => analyticsService.getTodayStats(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 2,
    refetchInterval: 1000 * 60 * 2,
  });

  return {
    stats: statsQuery.data,
    focusMinutes: statsQuery.data?.focusMinutes || 0,
    sessionsCompleted: statsQuery.data?.sessionsCompleted || 0,
    currentStreak: statsQuery.data?.currentStreak || 0,
    productivityScore: statsQuery.data?.productivityScore || 0,
    isLoading: statsQuery.isLoading,
    error: statsQuery.error,
  };
}

/**
 * Hook for weekly comparison
 */
export function useWeeklyComparison(userId: number | undefined) {
  const comparisonQuery = useQuery({
    queryKey: [...QUERY_KEYS.all, 'weekly-comparison', userId],
    queryFn: () => analyticsService.getWeeklyComparison(userId!),
    enabled: !!userId,
    staleTime: 1000 * 60 * 10,
  });

  return {
    comparison: comparisonQuery.data,
    thisWeek: comparisonQuery.data?.thisWeek || 0,
    lastWeek: comparisonQuery.data?.lastWeek || 0,
    percentageChange: comparisonQuery.data?.percentageChange || 0,
    trend: comparisonQuery.data?.trend,
    isImproving: comparisonQuery.data?.trend === 'UP',
    isLoading: comparisonQuery.isLoading,
    error: comparisonQuery.error,
  };
}

/**
 * Hook for real-time analytics updates
 */
export function useAnalyticsUpdates(userId: number | undefined) {
  const queryClient = useQueryClient();
  const [lastUpdate, setLastUpdate] = useState<AnalyticsWebSocketEvent | null>(null);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (!userId) return;

    unsubscribeRef.current = analyticsService.subscribeToAnalyticsUpdates(
      userId,
      (event: AnalyticsWebSocketEvent) => {
        setLastUpdate(event);

        // Invalidate relevant queries based on event type
        switch (event.type) {
          case 'METRIC_UPDATE':
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.daily(userId) });
            break;
          case 'GOAL_ACHIEVED':
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.goals(userId) });
            break;
          case 'LEADERBOARD_CHANGE':
            queryClient.invalidateQueries({
              queryKey: [...QUERY_KEYS.all, 'leaderboard']
            });
            break;
          case 'INSIGHT_GENERATED':
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.insights(userId) });
            break;
        }
      }
    );

    return () => {
      unsubscribeRef.current?.();
    };
  }, [userId, queryClient]);

  return {
    lastUpdate,
  };
}