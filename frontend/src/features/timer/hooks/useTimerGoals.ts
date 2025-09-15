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
  const activeGoals = goals.filter((g: TimerGoal) => !g.completed && new Date(g.endDate) > new Date());
  const completedGoals = goals.filter((g: TimerGoal) => g.completed);
  const pausedGoals = goals.filter((g: TimerGoal) => !g.completed && new Date(g.endDate) <= new Date()); // Expired but not completed

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
        (old = []) => old.filter(g => String(g.id) !== String(goalId))
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

      // For now, assume all goals are focus time in minutes
      // Since TimerGoal only has targetMinutes, we use that
      switch (goal.type) {
        case 'daily':
          current = Math.floor((stats.totalTime || 0) / (1000 * 60)); // Convert ms to minutes for daily
          break;
        case 'weekly':
          current = Math.floor((stats.totalTime || 0) / (1000 * 60)); // Convert ms to minutes for weekly
          break;
        case 'monthly':
          current = Math.floor((stats.totalTime || 0) / (1000 * 60)); // Convert ms to minutes for monthly
          break;
        case 'custom':
          current = Math.floor((stats.totalTime || 0) / (1000 * 60)); // Convert ms to minutes for custom
          break;
        default:
          current = goal.currentMinutes;
      }

      const percentage = goal.targetMinutes > 0 ? (current / goal.targetMinutes) * 100 : 0;
      const remaining = Math.max(0, goal.targetMinutes - current);
      const isCompleted = current >= goal.targetMinutes;

      // Calculate projected completion based on average
      const dailyAverage = stats.averageSessionLength ? (stats.totalTime / stats.totalSessions) / (1000 * 60 * 60 * 24) : 0;
      let projectedCompletion: Date | undefined;
      if (dailyAverage > 0 && remaining > 0) {
        const daysToComplete = Math.ceil(remaining / dailyAverage);
        projectedCompletion = new Date(now.getTime() + daysToComplete * 24 * 60 * 60 * 1000);
      }

      setProgress({
        current,
        target: goal.targetMinutes,
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
    const avgDaily = stats.totalSessions / Math.max(1, stats.streakDays || 1);
    const avgMinutes = (stats.totalTime || 0) / (1000 * 60); // Convert ms to minutes

    // Recommend based on current performance
    if (avgDaily > 0) {
      recs.push({
        type: 'daily',
        targetMinutes: Math.ceil(avgMinutes / 7 * 1.2), // 20% increase for daily
        title: 'Daily Focus Challenge',
        description: `Increase your daily focus to ${Math.ceil(avgMinutes / 7 * 1.2)} minutes`,
      });
    }

    if (avgMinutes > 0) {
      recs.push({
        type: 'weekly',
        targetMinutes: Math.ceil(avgMinutes * 1.1), // 10% increase weekly
        title: 'Weekly Focus Time',
        description: `Aim for ${Math.ceil(avgMinutes * 1.1)} minutes per week`,
      });
    }

    // Streak recommendation
    // Streak-based goal (using custom type since it's not time-based)
    if (stats.currentStreak && stats.currentStreak > 0) {
      recs.push({
        type: 'custom',
        targetMinutes: Math.ceil(avgMinutes), // Maintain current average
        title: 'Extend Your Streak',
        description: `Keep your ${stats.currentStreak}-day streak going`,
      });
    } else {
      recs.push({
        type: 'daily',
        targetMinutes: 25, // Standard Pomodoro
        title: 'Start a Streak',
        description: 'Build a daily focus habit with 25 minutes',
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
      updateGoal.mutateAsync({ id: goalId, updates: { reminderEnabled: false } }), // Disable reminders as a "pause"
  });

  const resumeGoal = useMutation({
    mutationFn: (goalId: number) =>
      updateGoal.mutateAsync({ id: goalId, updates: { reminderEnabled: true } }), // Re-enable reminders as "resume"
  });

  const completeGoal = useMutation({
    mutationFn: (goalId: number) =>
      updateGoal.mutateAsync({ id: goalId, updates: { completed: true, completedAt: new Date().toISOString() } }),
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