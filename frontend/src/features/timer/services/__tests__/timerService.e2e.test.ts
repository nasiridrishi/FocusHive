/**
 * Timer Service E2E Tests
 * Following TDD approach - these tests are written before implementation
 */

import { describe, it, expect, beforeEach, afterEach, vi, beforeAll, afterAll } from 'vitest';
import { TimerService } from '../timerService';
import type {
  TimerSession,
  TimerStatus,
  TimerType,
  TimerStats,
  TimerHistoryEntry,
  TimerTemplate,
  TimerGoal,
  CreateTimerRequest,
  UpdateTimerRequest,
  TimerStartResponse,
  TimerHistoryResponse,
  TimerTemplatesResponse,
  TimerWebSocketEvent,
  TimerCommand,
  TimerSyncData,
  SessionStatus,
  SessionType
} from '@/contracts/timer';
import { server } from '@/test-utils/msw-server';

describe('TimerService E2E Tests', () => {
  let timerService: TimerService;
  let mockWebSocket: any;

  beforeAll(() => {
    // Stop MSW server for unit tests that mock fetch directly
    server.close();
  });

  afterAll(() => {
    // Restart MSW server after tests
    server.listen();
  });

  beforeEach(() => {
    // Mock WebSocket service
    mockWebSocket = {
      subscribe: vi.fn().mockReturnValue(() => {}),
      sendMessage: vi.fn(),
      isConnected: vi.fn().mockReturnValue(true),
      on: vi.fn(),
      off: vi.fn()
    };

    // Initialize service with mocked dependencies
    timerService = new TimerService();
    (timerService as any).webSocketService = mockWebSocket;

    // Clear any cached data
    if (timerService.clearCache) {
      timerService.clearCache();
    }

    // Mock localStorage
    const localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    };
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true
    });

    // Mock fetch globally
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.clearAllMocks();
    if (timerService.cleanup) {
      timerService.cleanup();
    }
  });

  describe('Timer Session Management', () => {
    it('should start a new timer session', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000, // 25 minutes
        remainingTime: 25 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Focus Session',
        description: 'Working on feature',
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      const mockResponse: TimerStartResponse = {
        session: mockSession,
        message: 'Timer started successfully'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      } as Response);

      const request: CreateTimerRequest = {
        hiveId: 456,
        duration: 25 * 60 * 1000,
        type: 'pomodoro',
        title: 'Focus Session',
        description: 'Working on feature'
      };

      const result = await timerService.startTimer(request);

      expect(result.session).toEqual(mockSession);
      expect(result.message).toBe('Timer started successfully');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/start'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(request)
        })
      );
    });

    it('should pause an active timer', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 20 * 60 * 1000,
        type: 'pomodoro',
        status: 'paused',
        title: 'Focus Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: new Date().toISOString(),
        completedAt: null,
        breaks: 0,
        totalPausedTime: 5 * 60 * 1000
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession
      } as Response);

      const result = await timerService.pauseTimer(1);

      expect(result).toEqual(mockSession);
      expect(result.status).toBe('paused');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/1/pause'),
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should resume a paused timer', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 20 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Focus Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 5 * 60 * 1000
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession
      } as Response);

      const result = await timerService.resumeTimer(1);

      expect(result).toEqual(mockSession);
      expect(result.status).toBe('active');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/1/resume'),
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should stop and complete a timer', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 0,
        type: 'pomodoro',
        status: 'completed',
        title: 'Focus Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
        endedAt: new Date().toISOString(),
        pausedAt: null,
        completedAt: new Date().toISOString(),
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession
      } as Response);

      const result = await timerService.stopTimer(1);

      expect(result).toEqual(mockSession);
      expect(result.status).toBe('completed');
      expect(result.completedAt).not.toBeNull();
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/1/stop'),
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should cancel a timer session', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        type: 'pomodoro',
        status: 'cancelled',
        title: 'Focus Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: new Date().toISOString(),
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession
      } as Response);

      const result = await timerService.cancelTimer(1);

      expect(result).toEqual(mockSession);
      expect(result.status).toBe('cancelled');
      expect(result.completedAt).toBeNull();
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/1/cancel'),
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should get current active timer', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Current Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession
      } as Response);

      const result = await timerService.getCurrentTimer();

      expect(result).toEqual(mockSession);
      expect(result?.status).toBe('active');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/current'),
        expect.objectContaining({
          method: 'GET'
        })
      );
    });
  });

  describe('Timer History and Statistics', () => {
    it('should fetch timer history with pagination', async () => {
      const mockHistory: TimerHistoryEntry[] = [
        {
          id: '1',
          userId: 123,
          session: {
            id: 1,
            userId: 123,
            hiveId: 456,
            duration: 25 * 60 * 1000,
            remainingTime: 0,
            type: 'pomodoro',
            status: 'completed',
            title: 'Past Session 1',
            description: null,
            startTime: new Date().toISOString(), startedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
            endedAt: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString(),
            pausedAt: null,
            completedAt: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString(),
            breaks: 0,
            totalPausedTime: 0
          },
          productivity: 4,
          feedback: 'Good session',
          distractions: 2,
          completedAt: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString(),
          tags: ['work', 'focused']
        }
      ];

      const mockStats: TimerStats = {
        totalSessions: 100,
        totalFocusTime: 150 * 60 * 1000,
        averageSessionLength: 25 * 60 * 1000,
        completionRate: 0.85,
        currentStreak: 5,
        longestStreak: 15,
        todaysFocusTime: 75 * 60 * 1000,
        weeklyFocusTime: 20 * 60 * 60 * 1000,
        monthlyFocusTime: 80 * 60 * 60 * 1000,
        preferredSessionType: 'pomodoro',
        mostProductiveTime: 'morning'
      };

      const mockResponse: TimerHistoryResponse = {
        sessions: mockHistory,
        statistics: mockStats,
        pagination: {
          page: 1,
          pageSize: 20,
          total: 100,
          hasMore: true
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      } as Response);

      const result = await timerService.getTimerHistory({ page: 1, pageSize: 20 });

      expect(result.sessions).toHaveLength(1);
      expect(result.statistics.totalSessions).toBe(100);
      expect(result.pagination.hasMore).toBe(true);
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/sessions/history'),
        expect.objectContaining({
          method: 'GET'
        })
      );
    });

    it('should fetch timer statistics', async () => {
      const mockStats: TimerStats = {
        totalSessions: 150,
        totalFocusTime: 225 * 60 * 1000,
        averageSessionLength: 30 * 60 * 1000,
        completionRate: 0.90,
        currentStreak: 10,
        longestStreak: 25,
        todaysFocusTime: 120 * 60 * 1000,
        weeklyFocusTime: 35 * 60 * 60 * 1000,
        monthlyFocusTime: 140 * 60 * 60 * 1000,
        preferredSessionType: 'deep-work',
        mostProductiveTime: 'afternoon'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockStats
      } as Response);

      const result = await timerService.getTimerStatistics();

      expect(result).toEqual(mockStats);
      expect(result.currentStreak).toBe(10);
      expect(result.completionRate).toBe(0.90);
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/statistics'),
        expect.objectContaining({
          method: 'GET'
        })
      );
    });
  });

  describe('Timer Templates', () => {
    it('should fetch timer templates', async () => {
      const mockTemplates: TimerTemplatesResponse = {
        builtIn: [
          {
            id: '1',
            name: 'Pomodoro',
            description: 'Classic 25-minute focus session',
            type: 'pomodoro',
            duration: 25 * 60 * 1000,
            category: 'Productivity',
            isDefault: true,
            isCustom: false,
            createdAt: new Date().toISOString()
          },
          {
            id: '2',
            name: 'Short Break',
            description: '5-minute break',
            type: 'short-break',
            duration: 5 * 60 * 1000,
            category: 'Break',
            isDefault: true,
            isCustom: false,
            createdAt: new Date().toISOString()
          }
        ],
        custom: [
          {
            id: '3',
            name: 'Deep Work',
            description: '90-minute deep focus',
            type: 'deep-work',
            duration: 90 * 60 * 1000,
            category: 'Work',
            isDefault: false,
            isCustom: true,
            createdAt: new Date().toISOString()
          }
        ],
        recent: []
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockTemplates
      } as Response);

      const result = await timerService.getTimerTemplates();

      expect(result.builtIn).toHaveLength(2);
      expect(result.custom).toHaveLength(1);
      expect(result.builtIn[0].name).toBe('Pomodoro');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/templates'),
        expect.objectContaining({
          method: 'GET'
        })
      );
    });

    it('should create a custom timer template', async () => {
      const newTemplate: TimerTemplate = {
        id: '4',
        name: 'Focus Sprint',
        description: '45-minute intense focus',
        type: 'custom',
        duration: 45 * 60 * 1000,
        category: 'Custom',
        isDefault: false,
        isCustom: true,
        createdAt: new Date().toISOString()
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => newTemplate
      } as Response);

      const result = await timerService.createTimerTemplate({
        name: 'Focus Sprint',
        description: '45-minute intense focus',
        type: 'custom',
        duration: 45 * 60 * 1000,
        category: 'Custom'
      });

      expect(result).toEqual(newTemplate);
      expect(result.isCustom).toBe(true);
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/templates'),
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should delete a custom timer template', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true })
      } as Response);

      await timerService.deleteTimerTemplate('4');

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/templates/4'),
        expect.objectContaining({
          method: 'DELETE'
        })
      );
    });
  });

  describe('Timer Goals', () => {
    it('should fetch timer goals', async () => {
      const mockGoals: TimerGoal[] = [
        {
          id: '1',
          userId: 123,
          type: 'daily',
          targetMinutes: 120,
          targetSessions: 4,
          currentProgress: 60,
          sessionsCompleted: 2,
          startDate: new Date().toISOString(),
          endDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          isActive: true,
          achievedAt: undefined
        },
        {
          id: '2',
          userId: 123,
          type: 'weekly',
          targetMinutes: 600,
          targetSessions: 20,
          currentProgress: 300,
          sessionsCompleted: 10,
          startDate: new Date().toISOString(),
          endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
          isActive: true,
          achievedAt: undefined
        }
      ];

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockGoals
      } as Response);

      const result = await timerService.getTimerGoals();

      expect(result).toHaveLength(2);
      expect(result[0].type).toBe('daily');
      expect(result[1].type).toBe('weekly');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/goals'),
        expect.objectContaining({
          method: 'GET'
        })
      );
    });

    it('should create a timer goal', async () => {
      const newGoal: TimerGoal = {
        id: '3',
        userId: 123,
        type: 'monthly',
        targetMinutes: 2400,
        targetSessions: 80,
        currentProgress: 0,
        sessionsCompleted: 0,
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        isActive: true,
        achievedAt: undefined
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => newGoal
      } as Response);

      const result = await timerService.createTimerGoal({
        type: 'monthly',
        targetMinutes: 2400,
        targetSessions: 80
      });

      expect(result).toEqual(newGoal);
      expect(result.type).toBe('monthly');
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/goals'),
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should update goal progress', async () => {
      const updatedGoal: TimerGoal = {
        id: '1',
        userId: 123,
        type: 'daily',
        targetMinutes: 120,
        targetSessions: 4,
        currentProgress: 120,
        sessionsCompleted: 4,
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        isActive: true,
        achievedAt: new Date().toISOString()
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => updatedGoal
      } as Response);

      const result = await timerService.updateGoalProgress('1', 60);

      expect(result).toEqual(updatedGoal);
      expect(result.achievedAt).toBeDefined();
      expect(result.currentProgress).toBe(120);
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/timer/goals/1/progress'),
        expect.objectContaining({
          method: 'PUT'
        })
      );
    });
  });

  describe('WebSocket Real-time Features', () => {
    it('should subscribe to timer updates', () => {
      const callback = vi.fn();
      const unsubscribe = timerService.subscribeToTimerUpdates(1, callback);

      expect(mockWebSocket.subscribe).toHaveBeenCalledWith(
        '/topic/timer/1',
        expect.any(Function)
      );
      expect(typeof unsubscribe).toBe('function');
    });

    it('should handle timer tick events', () => {
      const callback = vi.fn();
      timerService.subscribeToTimerUpdates(1, callback);

      const subscribeCallback = mockWebSocket.subscribe.mock.calls[0][1];
      const mockEvent: TimerWebSocketEvent = {
        type: 'timer_tick',
        sessionId: '1',
        userId: 123,
        hiveId: 456,
        data: {
          sessionId: '1',
          userId: 123,
          status: 'active',
          elapsedTime: 5 * 60 * 1000,
          remainingTime: 20 * 60 * 1000,
          timestamp: new Date().toISOString()
        },
        timestamp: new Date().toISOString()
      };

      subscribeCallback(mockEvent);

      expect(callback).toHaveBeenCalledWith(mockEvent);
    });

    it('should send timer commands via WebSocket', () => {
      const command: TimerCommand = {
        action: 'start',
        sessionId: 1,
        data: {
          type: 'pomodoro',
          duration: 25 * 60 * 1000,
          hiveId: 456
        }
      };

      timerService.sendTimerCommand(command);

      expect(mockWebSocket.sendMessage).toHaveBeenCalledWith(
        '/app/timer/command',
        command
      );
    });

    it('should sync timer state across devices', () => {
      const syncData: TimerSyncData = {
        sessionId: '1',
        userId: 123,
        status: 'active',
        elapsedTime: 10 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        timestamp: new Date().toISOString()
      };

      timerService.syncTimerState(syncData);

      expect(mockWebSocket.sendMessage).toHaveBeenCalledWith(
        '/app/timer/sync',
        syncData
      );
    });
  });

  describe('Pomodoro Workflow', () => {
    it('should handle complete Pomodoro cycle', async () => {
      // Start Pomodoro
      const pomodoroSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 25 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Pomodoro 1',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ session: pomodoroSession, message: 'Started' })
      } as Response);

      const result = await timerService.startPomodoro();

      expect(result.type).toBe('pomodoro');
      expect(result.duration).toBe(25 * 60 * 1000);
    });

    it('should auto-start break after Pomodoro', async () => {
      const breakSession: TimerSession = {
        id: 2,
        userId: 123,
        hiveId: 456,
        duration: 5 * 60 * 1000,
        remainingTime: 5 * 60 * 1000,
        type: 'short-break',
        status: 'active',
        title: 'Short Break',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ session: breakSession, message: 'Break started' })
      } as Response);

      const result = await timerService.startBreak('short');

      expect(result.type).toBe('short-break');
      expect(result.duration).toBe(5 * 60 * 1000);
    });

    it('should track Pomodoro count for long break', async () => {
      const count = await timerService.getPomodoroCount();

      // Mock localStorage return value
      (window.localStorage.getItem as any).mockReturnValueOnce('3');

      const pomodoroCount = timerService.getPomodoroCount();
      expect(pomodoroCount).toBe(3);
    });
  });

  describe('Caching and Offline Support', () => {
    it('should cache timer sessions', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Cached Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockSession
      } as Response);

      // First call - should fetch from API
      const result1 = await timerService.getCurrentTimer();
      expect(fetch).toHaveBeenCalledTimes(1);

      // Second call - should use cache
      const result2 = await timerService.getCurrentTimer();
      expect(fetch).toHaveBeenCalledTimes(1); // Still only 1 call
      expect(result2).toEqual(result1);
    });

    it('should persist timer state to localStorage', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Persisted Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      timerService.persistTimerState(mockSession);

      expect(window.localStorage.setItem).toHaveBeenCalledWith(
        'timer_current_session',
        JSON.stringify(mockSession)
      );
    });

    it('should restore timer state from localStorage', () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Restored Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      (window.localStorage.getItem as any).mockReturnValueOnce(
        JSON.stringify(mockSession)
      );

      const result = timerService.restoreTimerState();

      expect(result).toEqual(mockSession);
      expect(window.localStorage.getItem).toHaveBeenCalledWith('timer_current_session');
    });
  });

  describe('Error Handling', () => {
    it('should handle API errors gracefully', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: async () => ({ error: 'Server error' })
      } as Response);

      await expect(timerService.startTimer({
        hiveId: 456,
        duration: 25 * 60 * 1000,
        type: 'pomodoro'
      })).rejects.toThrow('Failed to start timer: 500 Internal Server Error');
    });

    it('should handle network errors', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Network error'));

      await expect(timerService.getCurrentTimer()).rejects.toThrow('Network error');
    });

    it('should handle WebSocket disconnection', () => {
      mockWebSocket.isConnected.mockReturnValue(false);

      expect(() => {
        timerService.sendTimerCommand({
          action: 'start',
          sessionId: 1,
          data: {
            type: 'pomodoro',
            duration: 25 * 60 * 1000
          }
        });
      }).toThrow('WebSocket is not connected');
    });
  });

  describe('Cleanup', () => {
    it('should clean up resources on cleanup', () => {
      const unsubscribe = vi.fn();
      mockWebSocket.subscribe.mockReturnValueOnce(unsubscribe);

      timerService.subscribeToTimerUpdates(1, vi.fn());
      timerService.cleanup();

      expect(unsubscribe).toHaveBeenCalled();
    });

    it('should clear cache on demand', async () => {
      const mockSession: TimerSession = {
        id: 1,
        userId: 123,
        hiveId: 456,
        duration: 25 * 60 * 1000,
        remainingTime: 15 * 60 * 1000,
        type: 'pomodoro',
        status: 'active',
        title: 'Test Session',
        description: null,
        startTime: new Date().toISOString(), startedAt: new Date().toISOString(),
        endedAt: null,
        pausedAt: null,
        completedAt: null,
        breaks: 0,
        totalPausedTime: 0
      };

      vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: true,
        json: async () => mockSession
      } as Response);

      // First call - fetch from API
      await timerService.getCurrentTimer();
      expect(fetch).toHaveBeenCalledTimes(1);

      // Clear cache
      timerService.clearCache();

      // Second call - should fetch again (cache cleared)
      await timerService.getCurrentTimer();
      expect(fetch).toHaveBeenCalledTimes(2);
    });
  });
});