import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { PresenceService } from '../presenceService';
import { authService } from '@/services/auth/authService';
import { webSocketService } from '@/services/websocket/WebSocketService';
import type {
  UserPresence,
  SetPresenceRequest,
  PresenceStatus,
  HivePresence,
  PresenceHeartbeat,
  UserActivity,
  PresenceStatistics,
  CollaborationSession,
  BulkPresenceResponse,
} from '@/contracts/presence';

// Mock the dependencies
vi.mock('@/services/auth/authService');
vi.mock('@/services/websocket/WebSocketService');

describe('PresenceService E2E Tests', () => {
  let presenceService: PresenceService;
  let heartbeatInterval: NodeJS.Timeout;

  beforeEach(() => {
    // Clear all mocks before each test
    vi.clearAllMocks();

    // Setup auth mock
    vi.spyOn(authService, 'getAccessToken').mockReturnValue('mock-token');
    vi.spyOn(authService, 'isAuthenticated').mockReturnValue(true);
    vi.spyOn(authService, 'getCurrentUser').mockReturnValue({
      id: 123,
      username: 'testuser',
      email: 'test@example.com',
    });

    // Setup WebSocket mock
    vi.spyOn(webSocketService, 'isConnectedStatus').mockReturnValue(true);
    vi.spyOn(webSocketService, 'subscribe').mockReturnValue('subscription-id');
    vi.spyOn(webSocketService, 'sendMessage').mockImplementation(() => {});

    // Create service instance
    presenceService = new PresenceService();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    // Clean up any intervals
    if (heartbeatInterval) {
      clearInterval(heartbeatInterval);
    }
    presenceService.cleanup();
  });

  describe('Set User Presence', () => {
    it('should set user presence status', async () => {
      const request: SetPresenceRequest = {
        status: 'ONLINE',
        activity: {
          type: 'WORKING',
          description: 'Coding',
        },
      };

      const mockPresence: UserPresence = {
        userId: 123,
        username: 'testuser',
        status: 'ONLINE',
        activity: {
          type: 'WORKING',
          description: 'Coding',
          startedAt: new Date().toISOString(),
        },
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      const result = await presenceService.setPresence(request);

      expect(result).toEqual(mockPresence);
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/presence',
        expect.objectContaining({
          method: 'PUT',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify(request),
        })
      );
    });

    it('should update presence with custom status', async () => {
      const request: SetPresenceRequest = {
        status: 'BUSY',
        customStatus: '🎯 Deep focus mode - back at 3pm',
      };

      const mockPresence: UserPresence = {
        userId: 123,
        username: 'testuser',
        status: 'BUSY',
        activity: {
          type: 'CUSTOM',
          customStatus: '🎯 Deep focus mode - back at 3pm',
          startedAt: new Date().toISOString(),
        },
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      const result = await presenceService.setPresence(request);

      expect(result.activity?.customStatus).toBe('🎯 Deep focus mode - back at 3pm');
    });

    it('should set offline status on logout', async () => {
      const mockPresence: UserPresence = {
        userId: 123,
        username: 'testuser',
        status: 'OFFLINE',
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      const result = await presenceService.setOffline();

      expect(result.status).toBe('OFFLINE');
    });
  });

  describe('Get User Presence', () => {
    it('should get presence for a specific user', async () => {
      const userId = 456;
      const mockPresence: UserPresence = {
        userId,
        username: 'otheruser',
        status: 'ONLINE',
        activity: {
          type: 'STUDYING',
          hiveId: 1,
          hiveName: 'Study Room',
          startedAt: new Date().toISOString(),
        },
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      const result = await presenceService.getUserPresence(userId);

      expect(result).toEqual(mockPresence);
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/presence/users/${userId}`,
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });

    it('should return cached presence if available', async () => {
      const userId = 456;
      const mockPresence: UserPresence = {
        userId,
        username: 'cacheduser',
        status: 'ONLINE',
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      // First call fetches from API
      await presenceService.getUserPresence(userId);
      expect(global.fetch).toHaveBeenCalledTimes(1);

      // Second call should use cache
      await presenceService.getUserPresence(userId);
      expect(global.fetch).toHaveBeenCalledTimes(1);
    });
  });

  describe('Get Hive Presence', () => {
    it('should get presence for all users in a hive', async () => {
      const hiveId = 1;
      const mockHivePresence: HivePresence = {
        hiveId,
        onlineCount: 5,
        awayCount: 2,
        busyCount: 1,
        totalMembers: 10,
        activeUsers: [
          {
            userId: 1,
            username: 'user1',
            status: 'ONLINE',
            lastSeen: new Date().toISOString(),
            lastHeartbeat: new Date().toISOString(),
          },
          {
            userId: 2,
            username: 'user2',
            status: 'AWAY',
            lastSeen: new Date().toISOString(),
            lastHeartbeat: new Date().toISOString(),
          },
        ],
        recentlyOffline: [],
        lastUpdated: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHivePresence),
      } as any);

      const result = await presenceService.getHivePresence(hiveId);

      expect(result).toEqual(mockHivePresence);
      expect(result.onlineCount).toBe(5);
      expect(result.activeUsers).toHaveLength(2);
    });
  });

  describe('Bulk Presence Operations', () => {
    it('should get presence for multiple users', async () => {
      const userIds = [1, 2, 3];
      const mockResponse: BulkPresenceResponse = {
        presences: [
          {
            userId: 1,
            username: 'user1',
            status: 'ONLINE',
            lastSeen: new Date().toISOString(),
            lastHeartbeat: new Date().toISOString(),
          },
          {
            userId: 2,
            username: 'user2',
            status: 'AWAY',
            lastSeen: new Date().toISOString(),
            lastHeartbeat: new Date().toISOString(),
          },
          {
            userId: 3,
            username: 'user3',
            status: 'OFFLINE',
            lastSeen: new Date().toISOString(),
            lastHeartbeat: new Date().toISOString(),
          },
        ],
        timestamp: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockResponse),
      } as any);

      const result = await presenceService.getBulkPresence(userIds);

      expect(result.presences).toHaveLength(3);
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/presence/bulk',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify({ userIds }),
        })
      );
    });
  });

  describe('Heartbeat Mechanism', () => {
    it('should send heartbeat regularly', async () => {
      const mockPresence: UserPresence = {
        userId: 123,
        username: 'testuser',
        status: 'ONLINE',
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      // Start heartbeat
      presenceService.startHeartbeat();

      // Wait for first heartbeat
      await new Promise(resolve => setTimeout(resolve, 100));

      expect(webSocketService.sendMessage).toHaveBeenCalledWith(
        '/app/presence/heartbeat',
        expect.objectContaining({
          userId: 123,
          status: expect.any(String),
          timestamp: expect.any(String),
        })
      );

      // Stop heartbeat
      presenceService.stopHeartbeat();
    });

    it('should include active hives in heartbeat', async () => {
      const hiveIds = [1, 2, 3];
      presenceService.setActiveHives(hiveIds);

      presenceService.sendHeartbeat();

      expect(webSocketService.sendMessage).toHaveBeenCalledWith(
        '/app/presence/heartbeat',
        expect.objectContaining({
          userId: 123,
          hiveIds,
          status: expect.any(String),
        })
      );
    });

    it('should handle heartbeat failures gracefully', async () => {
      vi.spyOn(webSocketService, 'sendMessage').mockImplementation(() => {
        throw new Error('WebSocket error');
      });

      // Should not throw
      expect(() => presenceService.sendHeartbeat()).not.toThrow();
    });
  });

  describe('Real-time Presence Updates', () => {
    it('should subscribe to presence updates for a hive', async () => {
      const hiveId = 1;
      const callback = vi.fn();
      let capturedCallback: any;

      vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, cb) => {
        capturedCallback = cb;
        return 'subscription-id';
      });

      const unsubscribe = presenceService.subscribeToHivePresence(hiveId, callback);

      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        `/topic/presence/${hiveId}`,
        expect.any(Function)
      );

      // Simulate presence update
      const presenceUpdate = {
        userId: 456,
        status: 'ONLINE' as PresenceStatus,
        timestamp: new Date().toISOString(),
      };

      capturedCallback({ body: JSON.stringify(presenceUpdate) } as any);
      expect(callback).toHaveBeenCalledWith(presenceUpdate);

      // Test unsubscribe
      expect(unsubscribe).toBeDefined();
    });

    it('should subscribe to user presence updates', async () => {
      const userId = 456;
      const callback = vi.fn();
      let capturedCallback: any;

      vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, cb) => {
        capturedCallback = cb;
        return 'subscription-id';
      });

      presenceService.subscribeToUserPresence(userId, callback);

      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        `/topic/presence/user/${userId}`,
        expect.any(Function)
      );
    });
  });

  describe('Presence Statistics', () => {
    it('should get presence statistics for a user', async () => {
      const userId = 123;
      const mockStats: PresenceStatistics = {
        userId,
        totalOnlineTime: 480, // 8 hours
        totalFocusTime: 360, // 6 hours
        averageSessionLength: 120, // 2 hours
        mostActiveHive: {
          hiveId: 1,
          hiveName: 'Productivity Hub',
          timeSpent: 240,
        },
        activityBreakdown: [
          { type: 'WORKING', timeSpent: 240, percentage: 50 },
          { type: 'STUDYING', timeSpent: 120, percentage: 25 },
          { type: 'BREAK', timeSpent: 60, percentage: 12.5 },
          { type: 'IDLE', timeSpent: 60, percentage: 12.5 },
        ],
        peakHours: [
          { hour: 9, activityLevel: 90 },
          { hour: 14, activityLevel: 85 },
          { hour: 20, activityLevel: 70 },
        ],
        currentStreak: 5,
        longestStreak: 21,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockStats),
      } as any);

      const result = await presenceService.getStatistics(userId);

      expect(result).toEqual(mockStats);
      expect(result.totalFocusTime).toBe(360);
      expect(result.currentStreak).toBe(5);
    });

    it('should get presence history for a date range', async () => {
      const userId = 123;
      const startDate = '2024-01-01';
      const endDate = '2024-01-07';

      const mockHistory = {
        userId,
        entries: [
          {
            userId,
            status: 'ONLINE' as PresenceStatus,
            activity: {
              type: 'WORKING' as const,
              startedAt: '2024-01-01T09:00:00Z',
            },
            startTime: '2024-01-01T09:00:00Z',
            endTime: '2024-01-01T17:00:00Z',
            duration: 28800, // 8 hours in seconds
          },
        ],
        summary: {
          totalOnlineTime: 480,
          totalFocusTime: 360,
          numberOfSessions: 5,
          mostUsedStatus: 'ONLINE' as PresenceStatus,
          hivesVisited: [1, 2, 3],
        },
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHistory),
      } as any);

      const result = await presenceService.getPresenceHistory(userId, startDate, endDate);

      expect(result).toBeDefined();
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/presence/users/${userId}/history?startDate=${startDate}&endDate=${endDate}`,
        expect.any(Object)
      );
    });
  });

  describe('Collaboration Sessions', () => {
    it('should create a collaboration session', async () => {
      const hiveId = 1;
      const sessionType = 'POMODORO';
      const duration = 25;

      const mockSession: CollaborationSession = {
        sessionId: 'session-123',
        hiveId,
        participants: [],
        sharedActivity: {
          type: 'POMODORO',
          startedAt: new Date().toISOString(),
          duration,
          currentPhase: 'WORK',
        },
        createdBy: 123,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockSession),
      } as any);

      const result = await presenceService.createCollaborationSession(hiveId, {
        type: sessionType,
        duration,
      });

      expect(result).toEqual(mockSession);
      expect(result.sharedActivity?.type).toBe('POMODORO');
    });

    it('should join an existing collaboration session', async () => {
      const sessionId = 'session-123';

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ success: true }),
      } as any);

      await presenceService.joinCollaborationSession(sessionId);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/presence/collaboration/${sessionId}/join`,
        expect.objectContaining({
          method: 'POST',
        })
      );
    });

    it('should leave a collaboration session', async () => {
      const sessionId = 'session-123';

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ success: true }),
      } as any);

      await presenceService.leaveCollaborationSession(sessionId);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/presence/collaboration/${sessionId}/leave`,
        expect.objectContaining({
          method: 'POST',
        })
      );
    });
  });

  describe('Automatic Away Detection', () => {
    it('should automatically set away status after inactivity', async () => {
      vi.useFakeTimers();

      const mockPresence: UserPresence = {
        userId: 123,
        username: 'testuser',
        status: 'AWAY',
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockPresence),
      } as any);

      presenceService.enableAutoAway(5000); // 5 seconds for testing

      // Simulate user activity
      presenceService.updateActivity();

      // Fast forward time
      vi.advanceTimersByTime(6000);

      // Should have set away status
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/presence'),
        expect.objectContaining({
          method: 'PUT',
          body: expect.stringContaining('AWAY'),
        })
      );

      vi.useRealTimers();
    });

    it('should reset away timer on activity', () => {
      vi.useFakeTimers();

      presenceService.enableAutoAway(5000);

      // Initial activity
      presenceService.updateActivity();

      // Some time passes
      vi.advanceTimersByTime(3000);

      // More activity (should reset timer)
      presenceService.updateActivity();

      // Original timeout would trigger now
      vi.advanceTimersByTime(2000);

      // Should not have set away yet
      expect(global.fetch).not.toHaveBeenCalled();

      vi.useRealTimers();
    });
  });

  describe('Error Handling', () => {
    it('should handle authentication errors', async () => {
      vi.spyOn(authService, 'getAccessToken').mockReturnValue(null);

      await expect(presenceService.setPresence({
        status: 'ONLINE',
      })).rejects.toThrow('Authentication required');
    });

    it('should handle API errors gracefully', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      } as any);

      await expect(presenceService.getUserPresence(123)).rejects.toThrow(
        'Failed to fetch user presence: 500 Internal Server Error'
      );
    });

    it('should handle WebSocket disconnection', () => {
      vi.spyOn(webSocketService, 'isConnectedStatus').mockReturnValue(false);

      const callback = vi.fn();
      const unsubscribe = presenceService.subscribeToHivePresence(1, callback);

      // Should return a no-op unsubscribe function
      expect(unsubscribe).toBeDefined();
      expect(webSocketService.subscribe).not.toHaveBeenCalled();
    });
  });

  describe('Cache Management', () => {
    it('should cache user presence', async () => {
      const userId = 456;
      const presence: UserPresence = {
        userId,
        username: 'cacheduser',
        status: 'ONLINE',
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      presenceService.cacheUserPresence(presence);

      const cached = presenceService.getCachedUserPresence(userId);
      expect(cached).toEqual(presence);

      // Clear cache
      presenceService.clearCache();
      const afterClear = presenceService.getCachedUserPresence(userId);
      expect(afterClear).toBeNull();
    });

    it('should expire cache after timeout', () => {
      const userId = 456;
      const presence: UserPresence = {
        userId,
        username: 'user',
        status: 'ONLINE',
        lastSeen: new Date().toISOString(),
        lastHeartbeat: new Date().toISOString(),
      };

      // Manually set with expired timestamp
      presenceService['presenceCache'].set(userId, {
        presence,
        timestamp: Date.now() - (11 * 60 * 1000), // 11 minutes ago
      });

      const cached = presenceService.getCachedUserPresence(userId);
      expect(cached).toBeNull();
    });
  });

  describe('Cleanup', () => {
    it('should clean up resources on destroy', () => {
      const stopHeartbeatSpy = vi.spyOn(presenceService, 'stopHeartbeat');
      const clearCacheSpy = vi.spyOn(presenceService, 'clearCache');

      presenceService.cleanup();

      expect(stopHeartbeatSpy).toHaveBeenCalled();
      expect(clearCacheSpy).toHaveBeenCalled();
    });
  });
});