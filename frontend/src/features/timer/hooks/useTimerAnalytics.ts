import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { timerService } from '../services/timerService';
import type { TimerStats, TimerSession } from '@/contracts/timer';

/**
 * Time period for analytics
 */
export type AnalyticsPeriod = 'day' | 'week' | 'month' | 'year' | 'all';

/**
 * Analytics data structure
 */
interface TimerAnalytics {
  overview: {
    totalSessions: number;
    totalMinutes: number;
    averageSessionLength: number;
    completionRate: number;
    mostProductiveDay: string;
    mostProductiveTime: string;
  };
  trends: {
    daily: { date: string; sessions: number; minutes: number }[];
    weekly: { week: string; sessions: number; minutes: number }[];
    monthly: { month: string; sessions: number; minutes: number }[];
  };
  breakdown: {
    byType: Record<string, { count: number; minutes: number }>;
    byTag: Record<string, { count: number; minutes: number }>;
    byHive: Record<string, { count: number; minutes: number; name: string }>;
  };
  productivity: {
    focusScore: number;
    consistencyScore: number;
    streakData: { current: number; longest: number; average: number };
    peakHours: { hour: number; productivity: number }[];
  };
}

/**
 * Hook for timer analytics
 */
export function useTimerAnalytics(period: AnalyticsPeriod = 'week') {
  // Fetch base statistics
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['timer', 'statistics'],
    queryFn: () => timerService.getTimerStatistics(),
    staleTime: 5 * 60 * 1000,
  });

  // Fetch session history for detailed analytics
  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ['timer', 'analytics', 'history', period],
    queryFn: async () => {
      // TODO: Implement period-based filtering
      const response = await timerService.getTimerHistory({ page: 0, pageSize: 1000 });
      return response.sessions.map(entry => entry.session);
    },
    staleTime: 10 * 60 * 1000,
  });

  const analytics = useMemo<TimerAnalytics | null>(() => {
    if (!stats || !history) return null;

    // Calculate overview metrics
    const totalSessions = history.length;
    const totalMinutes = history.reduce((acc, s) => acc + (s.duration / 60000), 0);
    const completedSessions = history.filter(s => s.status === 'completed').length;
    const completionRate = totalSessions > 0 ? (completedSessions / totalSessions) * 100 : 0;
    const averageSessionLength = totalSessions > 0 ? totalMinutes / totalSessions : 0;

    // Calculate most productive day
    const dayStats = new Map<string, number>();
    history.forEach(session => {
      const day = new Date(session.startTime).toLocaleDateString('en-US', { weekday: 'long' });
      dayStats.set(day, (dayStats.get(day) || 0) + 1);
    });
    const mostProductiveDay = Array.from(dayStats.entries())
      .sort((a, b) => b[1] - a[1])[0]?.[0] || 'N/A';

    // Calculate most productive time
    const hourStats = new Map<number, number>();
    history.forEach(session => {
      const hour = new Date(session.startTime).getHours();
      hourStats.set(hour, (hourStats.get(hour) || 0) + 1);
    });
    const mostProductiveHour = Array.from(hourStats.entries())
      .sort((a, b) => b[1] - a[1])[0]?.[0];
    const mostProductiveTime = mostProductiveHour !== undefined
      ? `${mostProductiveHour}:00 - ${mostProductiveHour + 1}:00`
      : 'N/A';

    // Calculate trends
    const dailyTrends = calculateDailyTrends(history);
    const weeklyTrends = calculateWeeklyTrends(history);
    const monthlyTrends = calculateMonthlyTrends(history);

    // Calculate breakdown by type
    const typeBreakdown: Record<string, { count: number; minutes: number }> = {};
    history.forEach(session => {
      const type = session.sessionType || 'unknown';
      if (!typeBreakdown[type]) {
        typeBreakdown[type] = { count: 0, minutes: 0 };
      }
      typeBreakdown[type].count++;
      typeBreakdown[type].minutes += session.duration / 60000;
    });

    // Calculate breakdown by tag
    const tagBreakdown: Record<string, { count: number; minutes: number }> = {};
    history.forEach(session => {
      (session.tags || []).forEach(tag => {
        if (!tagBreakdown[tag]) {
          tagBreakdown[tag] = { count: 0, minutes: 0 };
        }
        tagBreakdown[tag].count++;
        tagBreakdown[tag].minutes += session.duration / 60000;
      });
    });

    // Calculate breakdown by hive
    const hiveBreakdown: Record<string, { count: number; minutes: number; name: string }> = {};
    history.forEach(session => {
      if (session.hiveId) {
        const hiveKey = `hive-${session.hiveId}`;
        if (!hiveBreakdown[hiveKey]) {
          hiveBreakdown[hiveKey] = {
            count: 0,
            minutes: 0,
            name: `Hive ${session.hiveId}` // TODO: Fetch actual hive names
          };
        }
        hiveBreakdown[hiveKey].count++;
        hiveBreakdown[hiveKey].minutes += session.duration / 60000;
      }
    });

    // Calculate productivity scores
    const focusScore = calculateFocusScore(history);
    const consistencyScore = calculateConsistencyScore(history);

    // Calculate peak hours
    const peakHours = Array.from(hourStats.entries())
      .map(([hour, count]) => ({
        hour,
        productivity: (count / totalSessions) * 100
      }))
      .sort((a, b) => a.hour - b.hour);

    return {
      overview: {
        totalSessions,
        totalMinutes: Math.round(totalMinutes),
        averageSessionLength: Math.round(averageSessionLength),
        completionRate: Math.round(completionRate),
        mostProductiveDay,
        mostProductiveTime,
      },
      trends: {
        daily: dailyTrends,
        weekly: weeklyTrends,
        monthly: monthlyTrends,
      },
      breakdown: {
        byType: typeBreakdown,
        byTag: tagBreakdown,
        byHive: hiveBreakdown,
      },
      productivity: {
        focusScore,
        consistencyScore,
        streakData: {
          current: stats.currentStreak || 0,
          longest: stats.longestStreak || 0,
          average: stats.averageStreak || 0,
        },
        peakHours,
      },
    };
  }, [stats, history]);

  return {
    analytics,
    isLoading: statsLoading || historyLoading,
  };
}

/**
 * Helper function to calculate daily trends
 */
function calculateDailyTrends(
  sessions: TimerSession[]
): { date: string; sessions: number; minutes: number }[] {
  const dailyData = new Map<string, { sessions: number; minutes: number }>();

  sessions.forEach(session => {
    const date = new Date(session.startTime).toISOString().split('T')[0];
    const current = dailyData.get(date) || { sessions: 0, minutes: 0 };
    current.sessions++;
    current.minutes += session.duration / 60000;
    dailyData.set(date, current);
  });

  return Array.from(dailyData.entries())
    .map(([date, data]) => ({ date, ...data }))
    .sort((a, b) => a.date.localeCompare(b.date))
    .slice(-30); // Last 30 days
}

/**
 * Helper function to calculate weekly trends
 */
function calculateWeeklyTrends(
  sessions: TimerSession[]
): { week: string; sessions: number; minutes: number }[] {
  const weeklyData = new Map<string, { sessions: number; minutes: number }>();

  sessions.forEach(session => {
    const date = new Date(session.startTime);
    const weekStart = new Date(date.setDate(date.getDate() - date.getDay()));
    const weekKey = weekStart.toISOString().split('T')[0];

    const current = weeklyData.get(weekKey) || { sessions: 0, minutes: 0 };
    current.sessions++;
    current.minutes += session.duration / 60000;
    weeklyData.set(weekKey, current);
  });

  return Array.from(weeklyData.entries())
    .map(([week, data]) => ({ week, ...data }))
    .sort((a, b) => a.week.localeCompare(b.week))
    .slice(-12); // Last 12 weeks
}

/**
 * Helper function to calculate monthly trends
 */
function calculateMonthlyTrends(
  sessions: TimerSession[]
): { month: string; sessions: number; minutes: number }[] {
  const monthlyData = new Map<string, { sessions: number; minutes: number }>();

  sessions.forEach(session => {
    const date = new Date(session.startTime);
    const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

    const current = monthlyData.get(monthKey) || { sessions: 0, minutes: 0 };
    current.sessions++;
    current.minutes += session.duration / 60000;
    monthlyData.set(monthKey, current);
  });

  return Array.from(monthlyData.entries())
    .map(([month, data]) => ({ month, ...data }))
    .sort((a, b) => a.month.localeCompare(b.month))
    .slice(-12); // Last 12 months
}

/**
 * Calculate focus score based on session completion and duration
 */
function calculateFocusScore(sessions: TimerSession[]): number {
  if (sessions.length === 0) return 0;

  const completedSessions = sessions.filter(s => s.status === 'completed');
  const completionRate = completedSessions.length / sessions.length;

  const averageDuration = sessions.reduce((acc, s) => acc + s.duration, 0) / sessions.length;
  const optimalDuration = 25 * 60 * 1000; // 25 minutes
  const durationScore = Math.min(averageDuration / optimalDuration, 1);

  return Math.round((completionRate * 0.7 + durationScore * 0.3) * 100);
}

/**
 * Calculate consistency score based on regular usage patterns
 */
function calculateConsistencyScore(sessions: TimerSession[]): number {
  if (sessions.length === 0) return 0;

  // Group sessions by day
  const dailySessions = new Map<string, number>();
  sessions.forEach(session => {
    const date = new Date(session.startTime).toISOString().split('T')[0];
    dailySessions.set(date, (dailySessions.get(date) || 0) + 1);
  });

  // Calculate days with sessions vs total days in range
  const dates = Array.from(dailySessions.keys()).sort();
  if (dates.length === 0) return 0;

  const firstDate = new Date(dates[0]);
  const lastDate = new Date(dates[dates.length - 1]);
  const totalDays = Math.ceil((lastDate.getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24)) + 1;
  const activeDays = dailySessions.size;

  const consistencyRate = activeDays / totalDays;

  // Calculate standard deviation of daily sessions for regularity
  const avgSessionsPerDay = sessions.length / activeDays;
  const variance = Array.from(dailySessions.values())
    .reduce((acc, count) => acc + Math.pow(count - avgSessionsPerDay, 2), 0) / activeDays;
  const stdDev = Math.sqrt(variance);
  const regularityScore = Math.max(0, 1 - (stdDev / avgSessionsPerDay));

  return Math.round((consistencyRate * 0.6 + regularityScore * 0.4) * 100);
}

/**
 * Hook for comparing timer analytics across periods
 */
export function useTimerComparison(
  period1: AnalyticsPeriod,
  period2: AnalyticsPeriod
) {
  const { analytics: analytics1 } = useTimerAnalytics(period1);
  const { analytics: analytics2 } = useTimerAnalytics(period2);

  const comparison = useMemo(() => {
    if (!analytics1 || !analytics2) return null;

    return {
      sessions: {
        period1: analytics1.overview.totalSessions,
        period2: analytics2.overview.totalSessions,
        change: analytics2.overview.totalSessions - analytics1.overview.totalSessions,
        changePercent: calculatePercentChange(
          analytics1.overview.totalSessions,
          analytics2.overview.totalSessions
        ),
      },
      minutes: {
        period1: analytics1.overview.totalMinutes,
        period2: analytics2.overview.totalMinutes,
        change: analytics2.overview.totalMinutes - analytics1.overview.totalMinutes,
        changePercent: calculatePercentChange(
          analytics1.overview.totalMinutes,
          analytics2.overview.totalMinutes
        ),
      },
      completionRate: {
        period1: analytics1.overview.completionRate,
        period2: analytics2.overview.completionRate,
        change: analytics2.overview.completionRate - analytics1.overview.completionRate,
      },
      focusScore: {
        period1: analytics1.productivity.focusScore,
        period2: analytics2.productivity.focusScore,
        change: analytics2.productivity.focusScore - analytics1.productivity.focusScore,
      },
    };
  }, [analytics1, analytics2]);

  return comparison;
}

/**
 * Calculate percentage change between two values
 */
function calculatePercentChange(oldValue: number, newValue: number): number {
  if (oldValue === 0) return newValue > 0 ? 100 : 0;
  return Math.round(((newValue - oldValue) / oldValue) * 100);
}

/**
 * Hook for timer insights and recommendations
 */
export function useTimerInsights() {
  const { analytics } = useTimerAnalytics('month');

  const insights = useMemo(() => {
    if (!analytics) return [];

    const insights = [];

    // Completion rate insight
    if (analytics.overview.completionRate < 70) {
      insights.push({
        type: 'warning',
        title: 'Low Completion Rate',
        message: `Your session completion rate is ${analytics.overview.completionRate}%. Try shorter sessions to improve completion.`,
        action: 'Adjust session duration',
      });
    } else if (analytics.overview.completionRate > 90) {
      insights.push({
        type: 'success',
        title: 'Excellent Completion Rate',
        message: `You're completing ${analytics.overview.completionRate}% of your sessions. Keep up the great work!`,
      });
    }

    // Productivity time insight
    if (analytics.overview.mostProductiveTime !== 'N/A') {
      insights.push({
        type: 'info',
        title: 'Peak Productivity Time',
        message: `You're most productive during ${analytics.overview.mostProductiveTime}. Schedule important tasks during this time.`,
      });
    }

    // Focus score insight
    if (analytics.productivity.focusScore < 50) {
      insights.push({
        type: 'warning',
        title: 'Focus Score Needs Improvement',
        message: 'Your focus score is below average. Try eliminating distractions and using the Pomodoro technique.',
        action: 'Enable Pomodoro mode',
      });
    }

    // Consistency insight
    if (analytics.productivity.consistencyScore < 60) {
      insights.push({
        type: 'info',
        title: 'Build Consistency',
        message: 'Working on a regular schedule can improve your productivity. Try setting daily timer goals.',
        action: 'Set daily goal',
      });
    }

    // Streak insight
    if (analytics.productivity.streakData.current > 7) {
      insights.push({
        type: 'success',
        title: `${analytics.productivity.streakData.current}-Day Streak!`,
        message: "You're on fire! Keep your momentum going.",
      });
    }

    return insights;
  }, [analytics]);

  return insights;
}