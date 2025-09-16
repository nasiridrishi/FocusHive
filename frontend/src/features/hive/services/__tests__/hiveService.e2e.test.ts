import { describe, it, expect, beforeEach, afterEach, vi, MockedFunction } from 'vitest';
import { hiveService } from '../hiveService';
import { authService } from '@/services/auth/authService';
import { webSocketService } from '@/services/websocket/WebSocketService';
import type { Hive, CreateHiveRequest, UpdateHiveRequest, HiveMember } from '@/contracts/hive';

// Mock the dependencies
vi.mock('@/services/auth/authService');
vi.mock('@/services/websocket/WebSocketService');

describe('HiveService E2E Tests', () => {
  beforeEach(() => {
    // Clear all mocks before each test
    vi.clearAllMocks();

    // Setup auth mock
    vi.spyOn(authService, 'getAccessToken').mockReturnValue('mock-token');
    vi.spyOn(authService, 'isAuthenticated').mockReturnValue(true);

    // Setup WebSocket mock
    vi.spyOn(webSocketService, 'isConnectedStatus').mockReturnValue(true);
    vi.spyOn(webSocketService, 'subscribe').mockReturnValue('subscription-id');

    // Clear hive service cache
    hiveService.clearCache();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Create Hive', () => {
    it('should create a new hive', async () => {
      const request: CreateHiveRequest = {
        name: 'Study Group',
        description: 'A focused study group',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        tags: ['study', 'productivity'],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
      };

      const mockHive: Hive = {
        ...request,
        id: 1,
        slug: 'study-group',
        memberCount: 1,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        createdBy: 'user123',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        tags: request.tags || [],
      };

      // Mock fetch
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHive),
      } as any);

      const result = await hiveService.createHive(request);

      expect(result).toEqual(mockHive);
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/hives',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify(request),
        })
      );
    });
  });

  describe('Get Hives', () => {
    it('should fetch all hives with pagination', async () => {
      const mockHives: Hive[] = [
        {
          id: 1,
          name: 'Study Hive',
          description: 'Focus on studying',
          type: 'STUDY',
          visibility: 'PUBLIC',
          maxMembers: 10,
          memberCount: 5,
          isActive: true,
          isPrivate: false,
          ownerId: 1,
          slug: 'study-hive',
          tags: ['study'],
          settings: {
            allowChat: true,
            requireApproval: false,
            enableTimer: true,
            enableMusic: false,
          },
          createdBy: 'user123',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        },
        {
          id: 2,
          name: 'Work Hive',
          description: 'Professional work environment',
          type: 'WORK',
          visibility: 'PRIVATE',
          maxMembers: 20,
          memberCount: 10,
          isActive: true,
          isPrivate: true,
          ownerId: 2,
          slug: 'work-hive',
          tags: ['work', 'professional'],
          settings: {
            allowChat: false,
            requireApproval: true,
            enableTimer: true,
            enableMusic: true,
          },
          createdBy: 'user456',
          createdAt: '2024-01-02T00:00:00Z',
          updatedAt: '2024-01-02T00:00:00Z',
        },
      ];

      const mockResponse = {
        content: mockHives,
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        number: 0,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockResponse),
      } as any);

      const result = await hiveService.getHives(0, 20);

      expect(result).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/hives?page=0&size=20',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });
  });

  describe('Get Single Hive', () => {
    it('should fetch a hive by ID', async () => {
      const hiveId = 1;
      const mockHive: Hive = {
        id: hiveId,
        name: 'Test Hive',
        description: 'A test hive',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        memberCount: 5,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        slug: 'test-hive',
        tags: ['test'],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
        createdBy: 'user123',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHive),
      } as any);

      const result = await hiveService.getHive(hiveId);

      expect(result).toEqual(mockHive);
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/hives/${hiveId}`,
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });

    it('should return cached hive if available', async () => {
      const hiveId = 1;
      const mockHive: Hive = {
        id: hiveId,
        name: 'Cached Hive',
        description: 'This should be cached',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        memberCount: 5,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        slug: 'cached-hive',
        tags: ['cached'],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
        createdBy: 'user123',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHive),
      } as any);

      // First call should fetch from API
      const result1 = await hiveService.getHive(hiveId);
      expect(global.fetch).toHaveBeenCalledTimes(1);

      // Second call should use cache
      const result2 = await hiveService.getHive(hiveId);
      expect(global.fetch).toHaveBeenCalledTimes(1); // Still only 1 call
      expect(result2).toEqual(mockHive);
    });
  });

  describe('Update Hive', () => {
    it('should update a hive with optimistic updates', async () => {
      const hiveId = 1;
      const request: UpdateHiveRequest = {
        name: 'Updated Hive',
        description: 'Updated description',
      };

      const updatedHive: Hive = {
        id: hiveId,
        name: 'Updated Hive',
        description: 'Updated description',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        memberCount: 5,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        slug: 'test-hive',
        tags: ['test'],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
        createdBy: 'user123',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(updatedHive),
      } as any);

      const result = await hiveService.updateHive(hiveId, request);

      expect(result).toEqual(updatedHive);
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/hives/${hiveId}`,
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
  });

  describe('Delete Hive', () => {
    it('should delete a hive', async () => {
      const hiveId = 1;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
      } as any);

      await hiveService.deleteHive(hiveId);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/hives/${hiveId}`,
        expect.objectContaining({
          method: 'DELETE',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });
  });

  describe('Join/Leave Hive', () => {
    it('should join a hive', async () => {
      const hiveId = 1;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({}),
      } as any);

      await hiveService.joinHive(hiveId);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/hives/${hiveId}/join`,
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
        })
      );
    });

    it('should leave a hive', async () => {
      const hiveId = 1;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({}),
      } as any);

      await hiveService.leaveHive(hiveId);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/hives/${hiveId}/leave`,
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
        })
      );
    });
  });

  describe('Real-time Updates', () => {
    it('should subscribe to hive updates after joining', async () => {
      const updateCallback = vi.fn();
      let capturedCallback: any;

      const subscribeSpy = vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, callback) => {
        capturedCallback = callback;
        return 'subscription-id';
      });

      hiveService.subscribeToHiveUpdates(1, updateCallback);

      expect(subscribeSpy).toHaveBeenCalledWith('/topic/hive/1', expect.any(Function));

      // Simulate a real-time update
      const mockHive: Hive = {
        id: 1,
        name: 'Real-time Hive',
        description: 'Updated via WebSocket',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        memberCount: 6,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        slug: 'real-time-hive',
        tags: ['real-time'],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
        createdBy: 'user123',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: new Date().toISOString(),
      };

      // Call the captured callback
      capturedCallback({ body: JSON.stringify(mockHive) } as any);

      // Wait a bit and check if our callback was called
      await new Promise(resolve => setTimeout(resolve, 10));
      expect(updateCallback).toHaveBeenCalledWith(mockHive);
    });
  });

  describe('Error Handling', () => {
    it('should handle authentication errors', async () => {
      vi.spyOn(authService, 'getAccessToken').mockReturnValue(null);

      await expect(hiveService.getHives()).rejects.toThrow('Authentication required');
    });

    it('should handle API errors', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      } as any);

      await expect(hiveService.getHives()).rejects.toThrow('Failed to fetch hives: 500 Internal Server Error');
    });
  });

  describe('Error Recovery', () => {
    it('should handle network errors with retry logic', async () => {
      vi.spyOn(authService, 'isAuthenticated').mockReturnValue(true);

      const hiveId = 1;
      const mockHive: Hive = {
        id: hiveId,
        name: 'Test Hive',
        description: 'A test hive',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        memberCount: 5,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        slug: 'test-hive',
        tags: ['test'],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
        createdBy: 'user123',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      // First two calls fail, third succeeds
      let callCount = 0;
      global.fetch = vi.fn().mockImplementation(() => {
        callCount++;
        if (callCount < 3) {
          return Promise.reject(new Error('Network error'));
        }
        return Promise.resolve({
          ok: true,
          json: vi.fn().mockResolvedValue(mockHive),
        });
      });

      // This should retry and eventually succeed
      const result = await hiveService.handleNetworkError(
        () => hiveService.getHive(hiveId),
        1
      );

      expect(result).toBeDefined();
      expect(global.fetch).toHaveBeenCalledTimes(3);
    });
  });

  describe('Cache Management', () => {
    it('should cache hives after fetching', async () => {
      const hiveId = 1;
      const mockHive: Hive = {
        id: hiveId,
        name: 'Test Hive',
        description: 'A test hive',
        type: 'STUDY',
        visibility: 'PUBLIC',
        maxMembers: 10,
        memberCount: 5,
        isActive: true,
        isPrivate: false,
        ownerId: 1,
        slug: 'test-hive',
        tags: [],
        settings: {
          allowChat: true,
          requireApproval: false,
          enableTimer: true,
          enableMusic: false,
        },
        createdBy: 'user123',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHive),
      } as any);

      // First call fetches from API
      await hiveService.getHive(hiveId);
      expect(global.fetch).toHaveBeenCalledTimes(1);

      // Check cache status
      expect(hiveService.isCached(hiveId)).toBe(true);

      // Clear cache
      hiveService.clearCache();
      expect(hiveService.isCached(hiveId)).toBe(false);
    });
  });
});