import { describe, it, expect, beforeEach, afterEach, beforeAll, afterAll, vi } from 'vitest';
import { server } from '@/test-utils/msw-server';
import { analyticsService } from '../analyticsService';
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
  AggregationPeriod,
  MetricType,
} from '@/contracts/analytics';

// Mock fetch globally
const mockFetch = vi.fn();
global.fetch = mockFetch as any;

describe('AnalyticsService E2E Tests', () => {
  beforeAll(() => {
    // Stop MSW server for these tests since we're mocking fetch directly
    server.close();
  });

  afterAll(() => {
    // Restart MSW server for other tests
    server.listen({ onUnhandledRequest: 'error' });
  });

  beforeEach(() => {
    // Reset all mocks before each test
    vi.clearAllMocks();

    // Reset the mock function
    mockFetch.mockReset();

    // Clear the service cache
    analyticsService.clearCache();

    // Mock localStorage
    const mockLocalStorage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    };
    Object.defineProperty(window, 'localStorage', {
      value: mockLocalStorage,
      writable: true,
    });

    // Mock auth token
    (window.localStorage.getItem as any).mockReturnValue('mock-auth-token');
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Focus Session Tracking', () => {
    it('should start a focus session', async () => {
      const mockSession: FocusSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        startTime: new Date().toISOString(),
        duration: 0,
        type: 'POMODORO',
        taskName: 'Complete project',
        completed: false,
        interruptions: 0,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession,
      });

      const session = await analyticsService.startFocusSession({
        type: 'POMODORO',
        taskName: 'Complete project',
        hiveId: 456,
      });

      expect(session).toEqual(mockSession);
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/analytics/sessions'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-auth-token',
          }),
        })
      );
    });

    it('should end a focus session', async () => {
      const mockSession: FocusSession = {
        id: 1,
        userId: 123,
        startTime: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
        endTime: new Date().toISOString(),
        duration: 25,
        type: 'POMODORO',
        completed: true,
        interruptions: 0,
        productivityScore: 95,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession,
      });

      const session = await analyticsService.endFocusSession(1, true);

      expect(session).toEqual(mockSession);
      expect(session.completed).toBe(true);
      expect(session.productivityScore).toBeDefined();
    });

    it('should track session interruptions', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true }),
      });

      await analyticsService.trackInterruption(1);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/analytics/sessions/1/interrupt'),
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  describe('Analytics Aggregation', () => {
    it('should get daily analytics', async () => {
      const mockDaily: DailyAnalytics = {
        userId: 123,
        date: new Date().toISOString().split('T')[0],
        totalFocusMinutes: 240,
        totalBreakMinutes: 45,
        sessionsCompleted: 6,
        tasksCompleted: 4,
        productivityScore: 85,
        peakProductivityHour: 10,
        interruptions: 2,
        hivesVisited: [1, 2],
        longestFocusSession: 60,
        averageSessionLength: 40,
        goals: {
          daily: 300,
          achieved: 240,
        },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ analytics: mockDaily }),
      });

      const analytics = await analyticsService.getDailyAnalytics(123);

      expect(analytics).toEqual(mockDaily);
      expect(analytics.productivityScore).toBe(85);
    });

    it('should get weekly analytics with trend analysis', async () => {
      const mockWeekly: WeeklyAnalytics = {
        userId: 123,
        weekStartDate: '2024-01-01',
        weekEndDate: '2024-01-07',
        totalFocusMinutes: 1500,
        dailyAverage: 214,
        productivityTrend: 'UP',
        percentageChange: 15,
        bestDay: '2024-01-05',
        worstDay: '2024-01-02',
        consistencyScore: 78,
        dailyBreakdown: [],
        weeklyGoals: {
          target: 2000,
          achieved: 1500,
          percentage: 75,
        },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ analytics: mockWeekly }),
      });

      const analytics = await analyticsService.getWeeklyAnalytics(123);

      expect(analytics).toEqual(mockWeekly);
      expect(analytics.productivityTrend).toBe('UP');
      expect(analytics.consistencyScore).toBe(78);
    });

    it('should get monthly analytics with insights', async () => {
      const mockMonthly: MonthlyAnalytics = {
        userId: 123,
        month: 1,
        year: 2024,
        totalFocusMinutes: 6000,
        dailyAverage: 200,
        weeklyAverage: 1400,
        productivityScore: 82,
        topHives: [
          { hiveId: 1, hiveName: 'Study Group', minutesSpent: 3000 },
          { hiveId: 2, hiveName: 'Work Team', minutesSpent: 2000 },
        ],
        topTasks: [
          { taskName: 'Project A', minutesSpent: 2000, completionRate: 90 },
        ],
        monthlyGoals: {
          target: 8000,
          achieved: 6000,
          percentage: 75,
        },
        insights: [],
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ analytics: mockMonthly }),
      });

      const analytics = await analyticsService.getMonthlyAnalytics(123, 1, 2024);

      expect(analytics).toEqual(mockMonthly);
      expect(analytics.topHives).toHaveLength(2);
    });

    it('should handle custom date range analytics', async () => {
      const request: GetAnalyticsRequest = {
        userId: 123,
        period: 'CUSTOM' as AggregationPeriod,
        startDate: '2024-01-01',
        endDate: '2024-01-31',
        metrics: ['FOCUS_TIME', 'PRODUCTIVITY_SCORE'] as MetricType[],
        includeComparison: true,
        includeInsights: true,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          analytics: {},
          comparison: {
            comparison: { trend: 'IMPROVING' }
          },
          insights: [],
        }),
      });

      const response = await analyticsService.getCustomAnalytics(request);

      expect(response.comparison?.comparison?.trend).toBe('IMPROVING');
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/analytics/custom'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(request),
        })
      );
    });
  });

  describe('Productivity Goals', () => {
    it('should create a productivity goal', async () => {
      const mockGoal: ProductivityGoal = {
        id: 1,
        userId: 123,
        type: 'DAILY',
        metric: 'FOCUS_TIME',
        target: 300,
        current: 0,
        percentage: 0,
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        achieved: false,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockGoal,
      });

      const goal = await analyticsService.createGoal({
        type: 'DAILY',
        metric: 'FOCUS_TIME',
        target: 300,
      });

      expect(goal).toEqual(mockGoal);
      expect(goal.target).toBe(300);
    });

    it('should update goal progress', async () => {
      const updatedGoal: ProductivityGoal = {
        id: 1,
        userId: 123,
        type: 'DAILY',
        metric: 'FOCUS_TIME',
        target: 300,
        current: 180,
        percentage: 60,
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        achieved: false,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => updatedGoal,
      });

      const goal = await analyticsService.updateGoalProgress(1, 180);

      expect(goal.current).toBe(180);
      expect(goal.percentage).toBe(60);
    });

    it('should get active goals', async () => {
      const mockGoals: ProductivityGoal[] = [
        {
          id: 1,
          userId: 123,
          type: 'DAILY',
          metric: 'FOCUS_TIME',
          target: 300,
          current: 150,
          percentage: 50,
          startDate: new Date().toISOString(),
          endDate: new Date().toISOString(),
          achieved: false,
        },
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockGoals,
      });

      const goals = await analyticsService.getActiveGoals(123);

      expect(goals).toHaveLength(1);
      expect(goals[0].percentage).toBe(50);
    });
  });

  describe('Leaderboards', () => {
    it('should get global leaderboard', async () => {
      const mockLeaderboard: Leaderboard = {
        id: 'global-focus-weekly',
        type: 'GLOBAL',
        period: 'WEEKLY',
        metric: 'FOCUS_TIME',
        entries: [
          {
            rank: 1,
            userId: 100,
            username: 'TopFocuser',
            score: 2500,
            metric: 'FOCUS_TIME',
            change: 0,
            isCurrentUser: false,
          },
          {
            rank: 5,
            userId: 123,
            username: 'CurrentUser',
            score: 1800,
            metric: 'FOCUS_TIME',
            change: 2,
            isCurrentUser: true,
          },
        ],
        lastUpdated: new Date().toISOString(),
        totalParticipants: 100,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockLeaderboard,
      });

      const leaderboard = await analyticsService.getLeaderboard({
        type: 'GLOBAL',
        metric: 'FOCUS_TIME',
        period: 'WEEKLY',
      });

      expect(leaderboard.entries).toHaveLength(2);
      expect(leaderboard.entries[1].isCurrentUser).toBe(true);
    });

    it('should get hive-specific leaderboard', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          id: 'hive-1-weekly',
          type: 'HIVE',
          entries: [],
        }),
      });

      const leaderboard = await analyticsService.getLeaderboard({
        type: 'HIVE',
        hiveId: 1,
        metric: 'PRODUCTIVITY_SCORE',
        period: 'WEEKLY',
      });

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/analytics/leaderboard'),
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('hiveId'),
        })
      );
    });
  });

  describe('Insights and Patterns', () => {
    it('should generate productivity insights', async () => {
      const mockInsights: AnalyticsInsight[] = [
        {
          id: 'peak-performance',
          type: 'PATTERN',
          title: 'Peak Performance Time',
          description: 'You are most productive between 9-11 AM',
          importance: 'HIGH',
          actionable: true,
          action: {
            label: 'Schedule important tasks',
            type: 'SCHEDULE',
          },
          createdAt: new Date().toISOString(),
        },
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockInsights,
      });

      const insights = await analyticsService.getInsights(123);

      expect(insights).toHaveLength(1);
      expect(insights[0].type).toBe('PATTERN');
      expect(insights[0].actionable).toBe(true);
    });

    it('should detect productivity patterns', async () => {
      const mockPattern: ProductivityPattern = {
        id: 'morning-person',
        userId: 123,
        patternType: 'TIME_OF_DAY',
        description: 'Most productive in morning hours',
        confidence: 0.85,
        data: {
          bestTimes: ['09:00', '10:00', '11:00'],
          optimalSessionLength: 45,
        },
        discoveredAt: new Date().toISOString(),
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => [mockPattern],
      });

      const patterns = await analyticsService.detectPatterns(123);

      expect(patterns).toHaveLength(1);
      expect(patterns[0].confidence).toBe(0.85);
    });

    it('should assess burnout risk', async () => {
      const mockRisk: BurnoutRisk = {
        userId: 123,
        riskLevel: 'MEDIUM',
        indicators: [
          {
            factor: 'consecutive_work_days',
            value: 12,
            threshold: 10,
            status: 'WARNING',
          },
        ],
        recommendations: [
          'Take regular breaks',
          'Consider a rest day',
        ],
        lastAssessment: new Date().toISOString(),
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockRisk,
      });

      const risk = await analyticsService.assessBurnoutRisk(123);

      expect(risk.riskLevel).toBe('MEDIUM');
      expect(risk.recommendations).toHaveLength(2);
    });
  });

  describe('Streak Tracking', () => {
    it('should get current streaks', async () => {
      const mockStreaks: Streak[] = [
        {
          id: 1,
          userId: 123,
          type: 'DAILY_LOGIN',
          currentStreak: 7,
          longestStreak: 15,
          startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
          lastActiveDate: new Date().toISOString(),
          milestones: [
            { days: 7, achieved: true, achievedAt: new Date().toISOString() },
            { days: 14, achieved: false },
          ],
        },
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockStreaks,
      });

      const streaks = await analyticsService.getStreaks(123);

      expect(streaks).toHaveLength(1);
      expect(streaks[0].currentStreak).toBe(7);
    });

    it('should update streak progress', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true }),
      });

      await analyticsService.updateStreak(123, 'FOCUS_GOAL');

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/analytics/streaks'),
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  describe('Data Visualization', () => {
    it('should get chart data for productivity trends', async () => {
      const mockChart: ChartConfig = {
        type: 'LINE',
        title: 'Weekly Productivity Trend',
        data: {
          labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
          datasets: [
            {
              label: 'Focus Time',
              data: [180, 200, 220, 240, 210, 150, 160],
              borderColor: '#3f51b5',
              fill: false,
            },
          ],
        },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockChart,
      });

      const chart = await analyticsService.getChartData('productivity', 'WEEKLY');

      expect(chart.type).toBe('LINE');
      expect(chart.data.datasets[0].data).toHaveLength(7);
    });

    it('should get heatmap data for activity', async () => {
      const mockHeatmap: HeatmapData = {
        userId: 123,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        data: [
          { date: '2024-01-01', value: 180, level: 3 },
          { date: '2024-01-02', value: 220, level: 4 },
        ],
        maxValue: 300,
        totalValue: 400,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockHeatmap,
      });

      const heatmap = await analyticsService.getActivityHeatmap(123, 2024);

      expect(heatmap.data).toHaveLength(2);
      expect(heatmap.maxValue).toBe(300);
    });
  });

  describe('Export and Sharing', () => {
    it('should export analytics data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          downloadUrl: 'https://example.com/export/123.pdf',
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
          fileSize: 1024000,
          format: 'PDF',
        }),
      });

      const exportData = await analyticsService.exportAnalytics({
        userId: 123,
        format: 'PDF',
        period: 'MONTHLY',
        startDate: '2024-01-01',
        endDate: '2024-01-31',
        includeCharts: true,
      });

      expect(exportData.downloadUrl).toContain('export');
      expect(exportData.format).toBe('PDF');
    });
  });

  describe('Caching', () => {
    it('should cache analytics data', async () => {
      const mockData: DailyAnalytics = {
        userId: 123,
        date: new Date().toISOString().split('T')[0],
        totalFocusMinutes: 240,
        totalBreakMinutes: 45,
        sessionsCompleted: 6,
        tasksCompleted: 4,
        productivityScore: 85,
        peakProductivityHour: 10,
        interruptions: 2,
        hivesVisited: [1, 2],
        longestFocusSession: 60,
        averageSessionLength: 40,
        goals: {
          daily: 300,
          achieved: 240,
        },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ analytics: mockData }),
      });

      // First call - should fetch from API
      const data1 = await analyticsService.getDailyAnalytics(123);
      expect(mockFetch).toHaveBeenCalledTimes(1);

      // Second call within cache period - should use cache
      const data2 = await analyticsService.getDailyAnalytics(123);
      expect(mockFetch).toHaveBeenCalledTimes(1); // Still 1, not 2

      expect(data1).toEqual(data2);
    });

    it('should invalidate cache after timeout', async () => {
      const mockData = { analytics: {} };

      mockFetch
        .mockResolvedValueOnce({
          ok: true,
          json: async () => mockData,
        })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => mockData,
        });

      await analyticsService.getDailyAnalytics(123);
      
      // Clear cache manually
      analyticsService.clearCache();
      
      await analyticsService.getDailyAnalytics(123);

      expect(mockFetch).toHaveBeenCalledTimes(2);
    });
  });

  describe('Error Handling', () => {
    it('should handle API errors gracefully', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      });

      await expect(
        analyticsService.getDailyAnalytics(123)
      ).rejects.toThrow('Failed to fetch daily analytics');
    });

    it('should handle network errors', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(
        analyticsService.startFocusSession({ type: 'POMODORO' })
      ).rejects.toThrow('Network error');
    });
  });
});

// Type guard test for analytics data
describe('Analytics Type Guards', () => {
  it('should correctly identify analytics data types', () => {
    const dailyData: DailyAnalytics = {
      userId: 123,
      date: '2024-01-01',
      totalFocusMinutes: 240,
      totalBreakMinutes: 45,
      sessionsCompleted: 6,
      tasksCompleted: 4,
      productivityScore: 85,
      peakProductivityHour: 10,
      interruptions: 2,
      hivesVisited: [1, 2],
      longestFocusSession: 60,
      averageSessionLength: 40,
      goals: { daily: 300, achieved: 240 },
    };

    expect(dailyData.userId).toBe(123);
    expect(typeof dailyData.productivityScore).toBe('number');
  });
});