import { useState, useEffect, useCallback, useMemo } from 'react';
import { useTimer } from './useTimer';
import { timerService } from '../services/timerService';
import type { CreateTimerRequest, TimerSession } from '@/contracts/timer';
import { SessionType, SessionStatus } from '@/contracts/timer';

/**
 * Pomodoro configuration
 */
interface PomodoroConfig {
  focusDuration: number;        // Focus session duration in minutes (default: 25)
  shortBreakDuration: number;   // Short break duration in minutes (default: 5)
  longBreakDuration: number;    // Long break duration in minutes (default: 15)
  sessionsUntilLongBreak: number; // Number of focus sessions before long break (default: 4)
  autoStartBreaks: boolean;     // Auto-start breaks after focus sessions
  autoStartFocus: boolean;      // Auto-start focus after breaks
  soundEnabled: boolean;        // Enable notification sounds
}

/**
 * Pomodoro state
 */
interface PomodoroState {
  currentSession: TimerSession | null;
  sessionType: SessionType;
  completedFocusSessions: number;
  totalCompletedSessions: number;
  isRunning: boolean;
  isPaused: boolean;
  config: PomodoroConfig;
  dailyGoal: number;
  dailyProgress: number;
}

const DEFAULT_POMODORO_CONFIG: PomodoroConfig = {
  focusDuration: 25,
  shortBreakDuration: 5,
  longBreakDuration: 15,
  sessionsUntilLongBreak: 4,
  autoStartBreaks: false,
  autoStartFocus: false,
  soundEnabled: true,
};

/**
 * Format time in seconds to MM:SS format
 */
function formatTime(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
}

/**
 * Custom hook for Pomodoro timer functionality
 * Provides specialized Pomodoro technique features
 */
export function usePomodoro(hiveId?: number) {
  const timer = useTimer();
  const [config, setConfig] = useState<PomodoroConfig>(DEFAULT_POMODORO_CONFIG);
  const [completedFocusSessions, setCompletedFocusSessions] = useState(0);
  const [totalCompletedSessions, setTotalCompletedSessions] = useState(0);
  const [dailyGoal, setDailyGoal] = useState(8); // Default: 8 pomodoros per day
  const [dailyProgress, setDailyProgress] = useState(0);

  // Load saved configuration from localStorage
  useEffect(() => {
    const savedConfig = localStorage.getItem('pomodoro-config');
    if (savedConfig) {
      try {
        setConfig(JSON.parse(savedConfig));
      } catch (error) {
        console.error('Failed to load Pomodoro config:', error);
      }
    }

    const savedStats = localStorage.getItem('pomodoro-stats');
    if (savedStats) {
      try {
        const stats = JSON.parse(savedStats);
        const today = new Date().toDateString();
        if (stats.date === today) {
          setDailyProgress(stats.progress || 0);
        } else {
          // Reset daily progress for new day
          setDailyProgress(0);
          localStorage.setItem('pomodoro-stats', JSON.stringify({
            date: today,
            progress: 0
          }));
        }
      } catch (error) {
        console.error('Failed to load Pomodoro stats:', error);
      }
    }
  }, []);

  // Save configuration changes
  const updateConfig = useCallback((newConfig: Partial<PomodoroConfig>) => {
    const updatedConfig = { ...config, ...newConfig };
    setConfig(updatedConfig);
    localStorage.setItem('pomodoro-config', JSON.stringify(updatedConfig));
  }, [config]);

  // Determine current session type
  const currentSessionType = useMemo((): SessionType => {
    if (timer.session) {
      return timer.session.sessionType || SessionType.FOCUS;
    }

    // Determine next session type based on completed sessions
    if (completedFocusSessions > 0 && completedFocusSessions % config.sessionsUntilLongBreak === 0) {
      return SessionType.LONG_BREAK;
    } else if (completedFocusSessions > 0) {
      return SessionType.SHORT_BREAK;
    }
    return SessionType.FOCUS;
  }, [timer.session, completedFocusSessions, config.sessionsUntilLongBreak]);

  // Get duration for session type
  const getDurationForType = useCallback((type: SessionType): number => {
    switch (type) {
      case SessionType.FOCUS:
        return config.focusDuration * 60 * 1000; // Convert to milliseconds
      case SessionType.SHORT_BREAK:
        return config.shortBreakDuration * 60 * 1000;
      case SessionType.LONG_BREAK:
        return config.longBreakDuration * 60 * 1000;
      default:
        return 25 * 60 * 1000; // Default 25 minutes
    }
  }, [config]);

  // Start a Pomodoro session
  const startPomodoro = useCallback(async (
    type?: SessionType,
    customDuration?: number
  ) => {
    const sessionType = type || currentSessionType;
    const duration = customDuration || getDurationForType(sessionType);

    const request: CreateTimerRequest = {
      duration,
      sessionType,
      hiveId: hiveId || null,
      title: sessionType === SessionType.FOCUS ? 'Focus Session' : 'Break Time',
      description: `Pomodoro ${sessionType.toLowerCase()} session`,
      tags: ['pomodoro', sessionType.toLowerCase()],
      isPomodoro: true,
      pomodoroSettings: {
        sessionNumber: completedFocusSessions + 1,
        totalSessions: config.sessionsUntilLongBreak,
        autoStartNext: sessionType === SessionType.FOCUS ? config.autoStartBreaks : config.autoStartFocus,
      }
    };

    await timer.start(request);
  }, [currentSessionType, getDurationForType, timer, hiveId, completedFocusSessions, config]);

  // Handle session completion
  useEffect(() => {
    if ((timer.session?.status as any) === 'completed') {
      const wasFocusSession = timer.session.sessionType === SessionType.FOCUS;

      if (wasFocusSession) {
        const newCompleted = completedFocusSessions + 1;
        setCompletedFocusSessions(newCompleted);
        setTotalCompletedSessions(prev => prev + 1);

        // Update daily progress
        const newProgress = dailyProgress + 1;
        setDailyProgress(newProgress);
        const today = new Date().toDateString();
        localStorage.setItem('pomodoro-stats', JSON.stringify({
          date: today,
          progress: newProgress
        }));

        // Auto-start break if configured
        if (config.autoStartBreaks) {
          const nextType = newCompleted % config.sessionsUntilLongBreak === 0
            ? SessionType.LONG_BREAK
            : SessionType.SHORT_BREAK;
          setTimeout(() => startPomodoro(nextType), 1000); // 1 second delay
        }
      } else {
        // Completed a break, auto-start focus if configured
        if (config.autoStartFocus) {
          setTimeout(() => startPomodoro(SessionType.FOCUS), 1000);
        }
      }
    }
  }, [timer.session?.status, timer.session?.sessionType, completedFocusSessions,
      dailyProgress, config, startPomodoro]);

  // Skip to next session type
  const skipToNext = useCallback(async () => {
    if (timer.session) {
      await timer.cancel(String(timer.session.id));
    }

    const nextType = currentSessionType === SessionType.FOCUS
      ? (completedFocusSessions % config.sessionsUntilLongBreak === 0
        ? SessionType.LONG_BREAK
        : SessionType.SHORT_BREAK)
      : SessionType.FOCUS;

    await startPomodoro(nextType);
  }, [timer, currentSessionType, completedFocusSessions, config.sessionsUntilLongBreak, startPomodoro]);

  // Reset Pomodoro cycle
  const resetCycle = useCallback(() => {
    setCompletedFocusSessions(0);
    if (timer.session) {
      timer.cancel(String(timer.session.id));
    }
  }, [timer]);

  // Calculate statistics
  const stats = useMemo(() => ({
    todayCompleted: dailyProgress,
    todayGoal: dailyGoal,
    todayProgress: dailyGoal > 0 ? (dailyProgress / dailyGoal) * 100 : 0,
    currentStreak: completedFocusSessions,
    totalCompleted: totalCompletedSessions,
    nextBreakType: completedFocusSessions % config.sessionsUntilLongBreak === 0
      ? SessionType.LONG_BREAK
      : SessionType.SHORT_BREAK,
    sessionsUntilLongBreak: config.sessionsUntilLongBreak - (completedFocusSessions % config.sessionsUntilLongBreak),
  }), [dailyProgress, dailyGoal, completedFocusSessions, totalCompletedSessions, config.sessionsUntilLongBreak]);

  return {
    // State
    session: timer.session,
    sessionType: currentSessionType,
    isRunning: timer.isRunning,
    isPaused: timer.isPaused,
    remainingTime: timer.time,
    formattedTime: formatTime(timer.time),
    progress: timer.time > 0 ? ((getDurationForType(currentSessionType) / 1000) - timer.time) / (getDurationForType(currentSessionType) / 1000) * 100 : 0,

    // Configuration
    config,
    updateConfig,

    // Actions
    start: startPomodoro,
    pause: () => timer.session && timer.pause(),
    resume: () => timer.session && timer.start({}), // Resume by starting again
    complete: () => timer.session && timer.skip(), // Complete by skipping to next
    cancel: () => timer.session && timer.cancel(String(timer.session.id)),
    skipToNext,
    resetCycle,

    // Goals
    dailyGoal,
    setDailyGoal,

    // Statistics
    stats,

    // Loading states (useTimer doesn't have these, so default to false)
    isLoading: false,
    isStarting: false,
    isPausing: false,
    isResuming: false,
    isCompleting: false,
    isCanceling: false,
  };
}

/**
 * Hook for Pomodoro statistics over time
 */
export function usePomodoroStats(period: 'day' | 'week' | 'month' | 'all' = 'week') {
  const [stats, setStats] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setIsLoading(true);
        // TODO: Implement API call for Pomodoro statistics
        // const data = await timerService.getPomodoroStatistics(period);
        // setStats(data);

        // Temporary mock data
        setStats({
          period,
          totalSessions: 42,
          averagePerDay: 6,
          longestStreak: 12,
          currentStreak: 3,
          mostProductiveDay: 'Monday',
          mostProductiveTime: '10:00 AM',
        });
      } catch (err) {
        setError(err as Error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, [period]);

  return { stats, isLoading, error };
}