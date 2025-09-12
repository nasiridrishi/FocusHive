import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useHivePresence } from './useWebSocket';
import { useWebSocket } from './useWebSocket';
import type { PresenceUpdate } from '../services/websocket/WebSocketService';
import { PresenceStatus } from '../services/websocket/WebSocketService';

// Mock the base useWebSocket hook
vi.mock('./useWebSocket', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useWebSocket: vi.fn()
  };
});

describe('useHivePresence', () => {
  const mockUseWebSocket = vi.mocked(useWebSocket);
  const mockWebSocketService = {
    subscribeToHivePresence: vi.fn(() => 'hive-presence-sub-id'),
    getHivePresence: vi.fn(),
    getHiveOnlineCount: vi.fn()
  };

  const mockWebSocketReturn = {
    isConnected: false,
    connectionState: 'DISCONNECTED',
    connectionInfo: {
      isConnected: false,
      connectionState: 'DISCONNECTED',
      reconnectionInfo: {
        attempts: 0,
        maxAttempts: 10,
        isReconnecting: false
      }
    },
    connect: vi.fn(),
    disconnect: vi.fn(),
    retryConnection: vi.fn(),
    reconnectWithNewToken: vi.fn(),
    sendMessage: vi.fn(),
    subscribe: vi.fn(),
    unsubscribe: vi.fn(),
    presenceStatus: PresenceStatus.OFFLINE,
    updatePresence: vi.fn(),
    startFocusSession: vi.fn(),
    notifications: [],
    clearNotification: vi.fn(),
    clearAllNotifications: vi.fn(),
    service: mockWebSocketService
  };

  let capturedPresenceHandler: ((presence: PresenceUpdate) => void) | undefined;

  beforeEach(() => {
    vi.clearAllMocks();
    
    mockUseWebSocket.mockImplementation((options) => {
      // Capture the presence handler
      capturedPresenceHandler = options?.onPresenceUpdate;
      
      return mockWebSocketReturn;
    });
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useHivePresence());

    expect(result.current.hivePresence).toEqual([]);
    expect(result.current.onlineCount).toBe(0);
    
    // Should have all base WebSocket properties
    expect(result.current.isConnected).toBe(false);
    expect(result.current.connectionState).toBe('DISCONNECTED');
    
    // Should have hive-specific methods
    expect(typeof result.current.joinHive).toBe('function');
    expect(typeof result.current.leaveHive).toBe('function');
  });

  it('should subscribe to hive presence when connected with hiveId', async () => {
    const hiveId = 123;
    
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => {
      capturedPresenceHandler = options?.onPresenceUpdate;
      
      return {
        ...mockWebSocketReturn,
        isConnected: true
      };
    });

    const { result } = renderHook(() => useHivePresence(hiveId));

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToHivePresence).toHaveBeenCalledWith(hiveId);
      expect(mockWebSocketService.getHivePresence).toHaveBeenCalledWith(hiveId);
      expect(mockWebSocketService.getHiveOnlineCount).toHaveBeenCalledWith(hiveId);
    });

    // Verify unsubscribe on unmount
    const { unmount } = renderHook(() => useHivePresence(hiveId));
    unmount();
    
    expect(result.current.unsubscribe).toHaveBeenCalledWith('hive-presence-sub-id');
  });

  it('should not subscribe when not connected', () => {
    const hiveId = 123;
    
    renderHook(() => useHivePresence(hiveId));

    expect(mockWebSocketService.subscribeToHivePresence).not.toHaveBeenCalled();
    expect(mockWebSocketService.getHivePresence).not.toHaveBeenCalled();
    expect(mockWebSocketService.getHiveOnlineCount).not.toHaveBeenCalled();
  });

  it('should not subscribe when no hiveId provided', async () => {
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    renderHook(() => useHivePresence());

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToHivePresence).not.toHaveBeenCalled();
      expect(mockWebSocketService.getHivePresence).not.toHaveBeenCalled();
      expect(mockWebSocketService.getHiveOnlineCount).not.toHaveBeenCalled();
    });
  });

  it('should handle presence updates for the specific hive', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    const presenceUpdate: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      hiveId: 123,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(presenceUpdate);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(1);
      expect(result.current.hivePresence[0]).toEqual(presenceUpdate);
      expect(result.current.onlineCount).toBe(1);
    });
  });

  it('should ignore presence updates for different hives', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    const presenceUpdate: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      hiveId: 999, // Different hive
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(presenceUpdate);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(0);
      expect(result.current.onlineCount).toBe(0);
    });
  });

  it('should update existing user presence', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    // Initial presence
    const initialPresence: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      hiveId: 123,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(initialPresence);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(1);
      expect(result.current.onlineCount).toBe(1);
    });

    // Updated presence for same user
    const updatedPresence: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.BUSY,
      hiveId: 123,
      currentActivity: 'In a meeting',
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(updatedPresence);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(1);
      expect(result.current.hivePresence[0]).toEqual(updatedPresence);
      expect(result.current.onlineCount).toBe(1);
    });
  });

  it('should remove user from presence when they go offline', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    // Add user
    const onlinePresence: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      hiveId: 123,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(onlinePresence);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(1);
      expect(result.current.onlineCount).toBe(1);
    });

    // User goes offline
    const offlinePresence: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.OFFLINE,
      hiveId: 123,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(offlinePresence);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(0);
      expect(result.current.onlineCount).toBe(0);
    });
  });

  it('should handle multiple users in hive presence', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    const users = [
      {
        userId: 456,
        username: 'user1',
        status: PresenceStatus.ONLINE,
        hiveId: 123,
        lastSeen: new Date().toISOString()
      },
      {
        userId: 789,
        username: 'user2',
        status: PresenceStatus.IN_FOCUS_SESSION,
        hiveId: 123,
        lastSeen: new Date().toISOString()
      },
      {
        userId: 101,
        username: 'user3',
        status: PresenceStatus.AWAY,
        hiveId: 123,
        lastSeen: new Date().toISOString()
      }
    ];

    for (const user of users) {
      act(() => {
        capturedPresenceHandler?.(user);
      });
    }

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(3);
      expect(result.current.onlineCount).toBe(3);
      
      // Check all users are present
      const userIds = result.current.hivePresence.map(p => p.userId);
      expect(userIds).toContain(456);
      expect(userIds).toContain(789);
      expect(userIds).toContain(101);
    });

    // One user leaves
    const offlineUser: PresenceUpdate = {
      userId: 789,
      username: 'user2',
      status: PresenceStatus.OFFLINE,
      hiveId: 123,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(offlineUser);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(2);
      expect(result.current.onlineCount).toBe(2);
      
      const userIds = result.current.hivePresence.map(p => p.userId);
      expect(userIds).toContain(456);
      expect(userIds).toContain(101);
      expect(userIds).not.toContain(789);
    });
  });

  it('should provide joinHive method that updates presence', () => {
    const { result } = renderHook(() => useHivePresence());

    const hiveId = 123;

    act(() => {
      result.current.joinHive(hiveId);
    });

    expect(mockWebSocketReturn.updatePresence).toHaveBeenCalledWith(
      PresenceStatus.ONLINE, 
      hiveId
    );
  });

  it('should provide leaveHive method that updates presence', () => {
    const { result } = renderHook(() => useHivePresence());

    act(() => {
      result.current.leaveHive();
    });

    expect(mockWebSocketReturn.updatePresence).toHaveBeenCalledWith(
      PresenceStatus.ONLINE
    );
  });

  it('should handle subscription errors gracefully', () => {
    const hiveId = 123;
    
    // Mock service to return null (error case)
    mockWebSocketService.subscribeToHivePresence.mockReturnValueOnce(null);
    
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    const { unmount } = renderHook(() => useHivePresence(hiveId));

    expect(mockWebSocketService.subscribeToHivePresence).toHaveBeenCalledWith(hiveId);
    
    // Should not attempt to unsubscribe with null subscription
    unmount();
    expect(mockWebSocketReturn.unsubscribe).not.toHaveBeenCalled();
  });

  it('should resubscribe when hiveId changes', async () => {
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    const { rerender } = renderHook(
      ({ hiveId }: { hiveId?: number }) => useHivePresence(hiveId),
      { initialProps: { hiveId: 123 } }
    );

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToHivePresence).toHaveBeenCalledWith(123);
      expect(mockWebSocketService.getHivePresence).toHaveBeenCalledWith(123);
    });

    // Change hive ID
    rerender({ hiveId: 456 });

    await waitFor(() => {
      expect(mockWebSocketReturn.unsubscribe).toHaveBeenCalledWith('hive-presence-sub-id');
      expect(mockWebSocketService.subscribeToHivePresence).toHaveBeenCalledWith(456);
      expect(mockWebSocketService.getHivePresence).toHaveBeenCalledWith(456);
    });
  });

  it('should clear presence when hiveId changes', async () => {
    const { result, rerender } = renderHook(
      ({ hiveId }: { hiveId?: number }) => useHivePresence(hiveId),
      { initialProps: { hiveId: 123 } }
    );

    // Add some presence data
    const presenceUpdate: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      hiveId: 123,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(presenceUpdate);
    });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(1);
    });

    // Change hive ID
    rerender({ hiveId: 456 });

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(0);
      expect(result.current.onlineCount).toBe(0);
    });
  });

  it('should handle connection state changes properly', async () => {
    const hiveId = 123;
    let isConnected = false;

    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected
    }));

    const { rerender } = renderHook(() => useHivePresence(hiveId));

    // Initially not connected - should not subscribe
    expect(mockWebSocketService.subscribeToHivePresence).not.toHaveBeenCalled();

    // Simulate connection
    isConnected = true;
    rerender();

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToHivePresence).toHaveBeenCalledWith(hiveId);
      expect(mockWebSocketService.getHivePresence).toHaveBeenCalledWith(hiveId);
      expect(mockWebSocketService.getHiveOnlineCount).toHaveBeenCalledWith(hiveId);
    });
  });

  it('should preserve all base useWebSocket functionality', () => {
    const { result } = renderHook(() => useHivePresence());

    // Check that all base properties are available
    expect(result.current.isConnected).toBe(mockWebSocketReturn.isConnected);
    expect(result.current.connectionState).toBe(mockWebSocketReturn.connectionState);
    expect(result.current.connect).toBe(mockWebSocketReturn.connect);
    expect(result.current.disconnect).toBe(mockWebSocketReturn.disconnect);
    expect(result.current.sendMessage).toBe(mockWebSocketReturn.sendMessage);
    expect(result.current.subscribe).toBe(mockWebSocketReturn.subscribe);
    expect(result.current.unsubscribe).toBe(mockWebSocketReturn.unsubscribe);
    expect(result.current.updatePresence).toBe(mockWebSocketReturn.updatePresence);
    expect(result.current.startFocusSession).toBe(mockWebSocketReturn.startFocusSession);
    expect(result.current.notifications).toBe(mockWebSocketReturn.notifications);
    expect(result.current.clearNotification).toBe(mockWebSocketReturn.clearNotification);
    expect(result.current.clearAllNotifications).toBe(mockWebSocketReturn.clearAllNotifications);
    expect(result.current.service).toBe(mockWebSocketReturn.service);
  });

  it('should handle presence updates without hiveId field', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    // Presence update without hiveId (global presence)
    const presenceUpdate: PresenceUpdate = {
      userId: 456,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      lastSeen: new Date().toISOString()
      // No hiveId field
    };

    act(() => {
      capturedPresenceHandler?.(presenceUpdate);
    });

    await waitFor(() => {
      // Should not add to hive presence since hiveId doesn't match
      expect(result.current.hivePresence).toHaveLength(0);
      expect(result.current.onlineCount).toBe(0);
    });
  });

  it('should handle rapid presence updates correctly', async () => {
    const hiveId = 123;
    const { result } = renderHook(() => useHivePresence(hiveId));

    const userId = 456;
    const basePresence = {
      userId,
      username: 'testuser',
      hiveId,
      lastSeen: new Date().toISOString()
    };

    // Rapid status changes
    const statuses = [
      PresenceStatus.ONLINE,
      PresenceStatus.AWAY,
      PresenceStatus.BUSY,
      PresenceStatus.IN_FOCUS_SESSION,
      PresenceStatus.ONLINE
    ];

    for (const status of statuses) {
      act(() => {
        capturedPresenceHandler?.({
          ...basePresence,
          status,
          lastSeen: new Date().toISOString()
        });
      });
    }

    await waitFor(() => {
      expect(result.current.hivePresence).toHaveLength(1);
      expect(result.current.hivePresence[0].status).toBe(PresenceStatus.ONLINE);
      expect(result.current.hivePresence[0].userId).toBe(userId);
      expect(result.current.onlineCount).toBe(1);
    });
  });
});