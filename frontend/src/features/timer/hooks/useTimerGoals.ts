import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect, useMemo } from 'react';
import { timerService } from '../services/timerService';
import type {
  TimerGoal,
  CreateTimerGoalRequest,
  UpdateTimerGoalRequest,
  TimerStats
} from '@/contracts/timer';

/**
 * Goal progress calculation
 */
interface GoalProgress {
  current: number;
  target: number;
  percentage: number;
  remaining: number;
  isCompleted: boolean;
  projectedCompletion?: Date;
  dailyAverage: number;
  streak: number;
}

/**
 * Hook for managing timer goals
 */
export function useTimerGoals() {
  const queryClient = useQueryClient();

  const {
    data: goals = [],
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['timer', 'goals'],
    queryFn: () => timerService.getTimerGoals(),
    staleTime: 5 * 60 * 1000, // Consider data stale after 5 minutes
    gcTime: 10 * 60 * 1000, // Keep in cache for 10 minutes
  });

  // Separate active and completed goals
  const activeGoals = goals.filter((g: TimerGoal) => g.status === 'active');
  const completedGoals = goals.filter((g: TimerGoal) => g.status === 'completed');
  const pausedGoals = goals.filter((g: TimerGoal) => g.status === 'paused');

  return {
    goals,
    activeGoals,
    completedGoals,
    pausedGoals,
    isLoading,
    error,
    refetch,
  };
}

/**
 * Hook for getting a single timer goal with progress
 */
export function useTimerGoal(goalId: number | undefined) {
  const { data: goal, isLoading } = useQuery({
    queryKey: ['timer', 'goals', goalId],
    queryFn: () => goalId ? timerService.getTimerGoal(goalId) : null,
    enabled: !!goalId,
    staleTime: 5 * 60 * 1000,
  });

  const { data: progress } = useQuery({
    queryKey: ['timer', 'goals', goalId, 'progress'],
    queryFn: () => goalId ? timerService.getGoalProgress(goalId) : null,
    enabled: !!goalId,
    refetchInterval: 60000, // Refresh every minute
  });

  return {
    goal,
    progress,
    isLoading,
  };
}

/**
 * Hook for creating a timer goal
 */
export function useCreateTimerGoal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (goal: CreateTimerGoalRequest) => timerService.createTimerGoal(goal),
    onSuccess: (newGoal) => {
      queryClient.setQueryData<TimerGoal[]>(
        ['timer', 'goals'],
        (old = []) => [...old, newGoal]
      );
      queryClient.invalidateQueries({ queryKey: ['timer', 'goals'] });
    },
    onError: (error) => {
      console.error('Failed to create timer goal:', error);
    },
  });
}

/**
 * Hook for updating a timer goal
 */
export function useUpdateTimerGoal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, updates }: { id: number; updates: UpdateTimerGoalRequest }) =>
      timerService.updateTimerGoal(id, updates),
    onSuccess: (updatedGoal) => {
      queryClient.setQueryData<TimerGoal[]>(
        ['timer', 'goals'],
        (old = []) => old.map(g => g.id === updatedGoal.id ? updatedGoal : g)
      );
      queryClient.setQueryData(['timer', 'goals', updatedGoal.id], updatedGoal);
      queryClient.invalidateQueries({ queryKey: ['timer', 'goals'] });
    },
    onError: (error) => {
      console.error('Failed to update timer goal:', error);
    },
  });
}

/**
 * Hook for deleting a timer goal
 */
export function useDeleteTimerGoal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (goalId: number) => timerService.deleteTimerGoal(goalId),
    onSuccess: (_, goalId) => {
      queryClient.setQueryData<TimerGoal[]>(
        ['timer', 'goals'],
        (old = []) => old.filter(g => g.id !== goalId)
      );
      queryClient.removeQueries({ queryKey: ['timer', 'goals', goalId] });
      queryClient.invalidateQueries({ queryKey: ['timer', 'goals'] });
    },
    onError: (error) => {
      console.error('Failed to delete timer goal:', error);
    },
  });
}

/**
 * Hook for tracking goal progress
 */
export function useGoalProgress(goalId: number | undefined) {
  const [progress, setProgress] = useState<GoalProgress | null>(null);
  const { data: stats } = useQuery({
    queryKey: ['timer', 'statistics'],
    queryFn: () => timerService.getTimerStatistics(),
    enabled: !!goalId,
  });

  const { goal } = useTimerGoal(goalId) || { goal: null };

  useEffect(() => {
    if (!goal || !stats) return;

    // Calculate progress based on goal type and period
    const calculateProgress = () => {
      let current = 0;
      const now = new Date();

      // Calculate current progress based on goal type
      switch (goal.goalType) {
        case 'sessions':
          current = goal.period === 'daily' ? stats.dailyStats?.sessionsCompleted || 0 :
                   goal.period === 'weekly' ? stats.weeklyStats?.sessionsCompleted || 0 :
                   stats.monthlyStats?.sessionsCompleted || 0;
          break;

        case 'minutes':
          const minutes = goal.period === 'daily' ? stats.dailyStats?.totalMinutes || 0 :
                         goal.period === 'weekly' ? stats.weeklyStats?.totalMinutes || 0 :
                         stats.monthlyStats?.totalMinutes || 0;
          current = Math.floor(minutes);
          break;

        case 'streak':
          current = stats.currentStreak || 0;
          break;

        case 'focus_time':
          const focusMinutes = goal.period === 'daily' ? stats.dailyStats?.focusMinutes || 0 :
                              goal.period === 'weekly' ? stats.weeklyStats?.focusMinutes || 0 :
                              stats.monthlyStats?.focusMinutes || 0;
          current = Math.floor(focusMinutes);
          break;
      }

      const percentage = goal.targetValue > 0 ? (current / goal.targetValue) * 100 : 0;
      const remaining = Math.max(0, goal.targetValue - current);
      const isCompleted = current >= goal.targetValue;

      // Calculate projected completion based on average
      const dailyAverage = stats.dailyStats?.averageSessionsPerDay || 0;
      let projectedCompletion: Date | undefined;
      if (dailyAverage > 0 && remaining > 0) {
        const daysToComplete = Math.ceil(remaining / dailyAverage);
        projectedCompletion = new Date(now.getTime() + daysToComplete * 24 * 60 * 60 * 1000);
      }

      setProgress({
        current,
        target: goal.targetValue,
        percentage: Math.min(100, percentage),
        remaining,
        isCompleted,
        projectedCompletion,
        dailyAverage,
        streak: stats.currentStreak || 0,
      });
    };

    calculateProgress();
  }, [goal, stats]);

  return progress;
}

/**
 * Hook for goal achievements and milestones
 */
export function useGoalAchievements(userId?: number) {
  const { data: achievements, isLoading } = useQuery({
    queryKey: ['timer', 'goals', 'achievements', userId],
    queryFn: async () => {
      // TODO: Implement API call for goal achievements
      // return timerService.getGoalAchievements(userId);

      // Mock data for now
      return {
        totalGoalsCompleted: 15,
        currentStreak: 7,
        longestStreak: 21,
        milestones: [
          { name: 'First Goal', date: '2024-01-15', icon: '🎯' },
          { name: '7-Day Streak', date: '2024-01-22', icon: '🔥' },
          { name: '100 Sessions', date: '2024-02-01', icon: '💯' },
        ],
        badges: [
          { name: 'Goal Getter', description: 'Complete 5 goals', earned: true },
          { name: 'Streak Master', description: '30-day streak', earned: false },
          { name: 'Focus Champion', description: '1000 minutes focused', earned: true },
        ],
      };
    },
    enabled: !!userId,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });

  return {
    achievements,
    isLoading,
  };
}

/**
 * Hook for goal recommendations based on user patterns
 */
export function useGoalRecommendations() {
  const { data: stats } = useQuery({
    queryKey: ['timer', 'statistics'],
    queryFn: () => timerService.getTimerStatistics(),
  });

  const recommendations = useMemo(() => {
    if (!stats) return [];

    const recs = [];
    const avgDaily = stats.dailyStats?.averageSessionsPerDay || 0;
    const avgMinutes = stats.dailyStats?.totalMinutes || 0;

    // Recommend based on current performance
    if (avgDaily > 0) {
      recs.push({
        type: 'sessions',
        period: 'daily',
        targetValue: Math.ceil(avgDaily * 1.2), // 20% increase
        title: 'Daily Sessions Challenge',
        description: `Increase your daily sessions to ${Math.ceil(avgDaily * 1.2)}`,
      });
    }

    if (avgMinutes > 0) {
      recs.push({
        type: 'minutes',
        period: 'weekly',
        targetValue: Math.ceil(avgMinutes * 7 * 1.1), // 10% increase weekly
        title: 'Weekly Focus Time',
        description: `Aim for ${Math.ceil(avgMinutes * 7 * 1.1)} minutes per week`,
      });
    }

    // Streak recommendation
    if (stats.currentStreak && stats.currentStreak > 0) {
      recs.push({
        type: 'streak',
        period: 'continuous',
        targetValue: stats.currentStreak + 7,
        title: 'Extend Your Streak',
        description: `Keep your streak going for ${stats.currentStreak + 7} days`,
      });
    } else {
      recs.push({
        type: 'streak',
        period: 'continuous',
        targetValue: 7,
        title: 'Start a Streak',
        description: 'Build a 7-day focus streak',
      });
    }

    return recs;
  }, [stats]);

  return recommendations;
}

/**
 * Hook for managing all goal operations
 */
export function useTimerGoalActions() {
  const createGoal = useCreateTimerGoal();
  const updateGoal = useUpdateTimerGoal();
  const deleteGoal = useDeleteTimerGoal();

  const pauseGoal = useMutation({
    mutationFn: (goalId: number) =>
      updateGoal.mutateAsync({ id: goalId, updates: { status: 'paused' } }),
  });

  const resumeGoal = useMutation({
    mutationFn: (goalId: number) =>
      updateGoal.mutateAsync({ id: goalId, updates: { status: 'active' } }),
  });

  const completeGoal = useMutation({
    mutationFn: (goalId: number) =>
      updateGoal.mutateAsync({ id: goalId, updates: { status: 'completed', completedAt: new Date().toISOString() } }),
  });

  return {
    create: createGoal.mutate,
    update: updateGoal.mutate,
    delete: deleteGoal.mutate,
    pause: pauseGoal.mutate,
    resume: resumeGoal.mutate,
    complete: completeGoal.mutate,

    isCreating: createGoal.isPending,
    isUpdating: updateGoal.isPending,
    isDeleting: deleteGoal.isPending,
    isPausing: pauseGoal.isPending,
    isResuming: resumeGoal.isPending,
    isCompleting: completeGoal.isPending,
  };
}