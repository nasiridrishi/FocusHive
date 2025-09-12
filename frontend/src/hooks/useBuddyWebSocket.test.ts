import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useBuddyWebSocket } from './useWebSocket';
import { useWebSocket } from './useWebSocket';
import type { WebSocketMessage, PresenceUpdate } from '../services/websocket/WebSocketService';
import { PresenceStatus } from '../services/websocket/WebSocketService';

// Mock the base useWebSocket hook
vi.mock('./useWebSocket', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useWebSocket: vi.fn()
  };
});

// Define types for buddy system
interface BuddyCheckin {
  id: string;
  message: string;
  mood: 'great' | 'good' | 'okay' | 'struggling';
  completedTasks: number;
  timestamp: string;
}

interface BuddyGoal {
  id: string;
  title: string;
  description: string;
  targetDate: string;
  completed: boolean;
}

describe('useBuddyWebSocket', () => {
  const mockUseWebSocket = vi.mocked(useWebSocket);
  const mockWebSocketService = {
    subscribeToBuddyUpdates: vi.fn(() => 'buddy-sub-id'),
    sendBuddyRequest: vi.fn(),
    acceptBuddyRequest: vi.fn(),
    sendBuddyCheckin: vi.fn(),
    updateBuddyGoal: vi.fn(),
    startBuddySession: vi.fn(),
    endBuddySession: vi.fn()
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

  let capturedMessageHandler: ((message: WebSocketMessage) => void) | undefined;
  let capturedPresenceHandler: ((presence: PresenceUpdate) => void) | undefined;

  beforeEach(() => {
    vi.clearAllMocks();
    
    mockUseWebSocket.mockImplementation((options) => {
      // Capture the message and presence handlers
      capturedMessageHandler = options?.onMessage;
      capturedPresenceHandler = options?.onPresenceUpdate;
      
      return mockWebSocketReturn;
    });
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    expect(result.current.buddyMessages).toEqual([]);
    expect(result.current.buddyPresence).toBeNull();
    
    // Should have all base WebSocket properties
    expect(result.current.isConnected).toBe(false);
    expect(result.current.connectionState).toBe('DISCONNECTED');
    
    // Should have buddy-specific methods
    expect(typeof result.current.sendBuddyRequest).toBe('function');
    expect(typeof result.current.acceptBuddyRequest).toBe('function');
    expect(typeof result.current.sendCheckin).toBe('function');
    expect(typeof result.current.updateGoal).toBe('function');
    expect(typeof result.current.startSession).toBe('function');
    expect(typeof result.current.endSession).toBe('function');
  });

  it('should filter buddy messages from general messages', async () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    // Simulate buddy message
    const buddyMessage: WebSocketMessage = {
      id: 'msg-1',
      type: 'BUDDY_REQUEST' as WebSocketMessage['type'],
      event: 'buddy_request',
      payload: { fromUserId: 123, message: 'Be my buddy!' },
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(buddyMessage);
    });

    await waitFor(() => {
      expect(result.current.buddyMessages).toHaveLength(1);
      expect(result.current.buddyMessages[0]).toEqual(buddyMessage);
    });

    // Simulate non-buddy message
    const generalMessage: WebSocketMessage = {
      id: 'msg-2',
      type: 'NOTIFICATION' as WebSocketMessage['type'],
      event: 'notification',
      payload: { message: 'General notification' },
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(generalMessage);
    });

    await waitFor(() => {
      // Should still only have the buddy message
      expect(result.current.buddyMessages).toHaveLength(1);
    });
  });

  it('should handle presence updates for buddy', async () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    const presenceUpdate: PresenceUpdate = {
      userId: 456,
      username: 'buddy-user',
      status: PresenceStatus.ONLINE,
      lastSeen: new Date().toISOString()
    };

    act(() => {
      capturedPresenceHandler?.(presenceUpdate);
    });

    await waitFor(() => {
      expect(result.current.buddyPresence).toEqual(presenceUpdate);
    });
  });

  it('should subscribe to buddy updates when connected with relationshipId', async () => {
    const relationshipId = 789;
    
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => {
      capturedMessageHandler = options?.onMessage;
      capturedPresenceHandler = options?.onPresenceUpdate;
      
      return {
        ...mockWebSocketReturn,
        isConnected: true
      };
    });

    const { result } = renderHook(() => useBuddyWebSocket(relationshipId));

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToBuddyUpdates).toHaveBeenCalledWith(relationshipId);
    });

    // Verify unsubscribe on unmount
    const { unmount } = renderHook(() => useBuddyWebSocket(relationshipId));
    unmount();
    
    expect(result.current.unsubscribe).toHaveBeenCalledWith('buddy-sub-id');
  });

  it('should not subscribe when not connected', () => {
    const relationshipId = 789;
    
    renderHook(() => useBuddyWebSocket(relationshipId));

    expect(mockWebSocketService.subscribeToBuddyUpdates).not.toHaveBeenCalled();
  });

  it('should not subscribe when no relationshipId provided', async () => {
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    renderHook(() => useBuddyWebSocket());

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToBuddyUpdates).not.toHaveBeenCalled();
    });
  });

  it('should handle buddy request actions', () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    // Test sending buddy request
    act(() => {
      result.current.sendBuddyRequest(123, 'Would you like to be my buddy?');
    });

    expect(mockWebSocketService.sendBuddyRequest).toHaveBeenCalledWith(123, 'Would you like to be my buddy?');

    // Test accepting buddy request
    act(() => {
      result.current.acceptBuddyRequest(456);
    });

    expect(mockWebSocketService.acceptBuddyRequest).toHaveBeenCalledWith(456);
  });

  it('should handle buddy checkin', () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    const checkin: BuddyCheckin = {
      id: 'checkin-1',
      message: 'Making good progress!',
      mood: 'good',
      completedTasks: 3,
      timestamp: new Date().toISOString()
    };

    act(() => {
      result.current.sendCheckin(789, checkin);
    });

    expect(mockWebSocketService.sendBuddyCheckin).toHaveBeenCalledWith(789, checkin);
  });

  it('should handle buddy goal updates', () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    const goal: BuddyGoal = {
      id: 'goal-1',
      title: 'Complete project',
      description: 'Finish the final project for course',
      targetDate: '2024-12-31',
      completed: false
    };

    act(() => {
      result.current.updateGoal(goal);
    });

    expect(mockWebSocketService.updateBuddyGoal).toHaveBeenCalledWith(goal);
  });

  it('should handle buddy session management', () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    const sessionId = 101;

    // Test starting session
    act(() => {
      result.current.startSession(sessionId);
    });

    expect(mockWebSocketService.startBuddySession).toHaveBeenCalledWith(sessionId);

    // Test ending session
    act(() => {
      result.current.endSession(sessionId);
    });

    expect(mockWebSocketService.endBuddySession).toHaveBeenCalledWith(sessionId);
  });

  it('should accumulate multiple buddy messages', async () => {
    const { result } = renderHook(() => useBuddyWebSocket());

    const messages = [
      {
        id: 'msg-1',
        type: 'BUDDY_REQUEST' as WebSocketMessage['type'],
        event: 'buddy_request',
        payload: { fromUserId: 123, message: 'First message' },
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-2',
        type: 'BUDDY_CHECKIN' as WebSocketMessage['type'],
        event: 'buddy_checkin',
        payload: { mood: 'good', tasks: 2 },
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-3',
        type: 'BUDDY_SESSION_START' as WebSocketMessage['type'],
        event: 'buddy_session_start',
        payload: { sessionId: 456 },
        timestamp: new Date().toISOString()
      }
    ];

    for (const message of messages) {
      act(() => {
        capturedMessageHandler?.(message);
      });
    }

    await waitFor(() => {
      expect(result.current.buddyMessages).toHaveLength(3);
      expect(result.current.buddyMessages).toEqual(messages);
    });
  });

  it('should handle subscription errors gracefully', () => {
    const relationshipId = 789;
    
    // Mock service to return null (error case)
    mockWebSocketService.subscribeToBuddyUpdates.mockReturnValueOnce(null);
    
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    const { unmount } = renderHook(() => useBuddyWebSocket(relationshipId));

    expect(mockWebSocketService.subscribeToBuddyUpdates).toHaveBeenCalledWith(relationshipId);
    
    // Should not attempt to unsubscribe with null subscription
    unmount();
    expect(mockWebSocketReturn.unsubscribe).not.toHaveBeenCalled();
  });

  it('should resubscribe when relationshipId changes', async () => {
    // Mock connected state
    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    const { rerender } = renderHook(
      ({ relationshipId }: { relationshipId?: number }) => useBuddyWebSocket(relationshipId),
      { initialProps: { relationshipId: 123 } }
    );

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToBuddyUpdates).toHaveBeenCalledWith(123);
    });

    // Change relationship ID
    rerender({ relationshipId: 456 });

    await waitFor(() => {
      expect(mockWebSocketReturn.unsubscribe).toHaveBeenCalledWith('buddy-sub-id');
      expect(mockWebSocketService.subscribeToBuddyUpdates).toHaveBeenCalledWith(456);
    });
  });

  it('should handle connection state changes properly', async () => {
    const relationshipId = 789;
    let isConnected = false;

    mockUseWebSocket.mockImplementation((options) => ({
      ...mockWebSocketReturn,
      isConnected
    }));

    const { rerender } = renderHook(() => useBuddyWebSocket(relationshipId));

    // Initially not connected - should not subscribe
    expect(mockWebSocketService.subscribeToBuddyUpdates).not.toHaveBeenCalled();

    // Simulate connection
    isConnected = true;
    rerender();

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToBuddyUpdates).toHaveBeenCalledWith(relationshipId);
    });
  });

  it('should preserve all base useWebSocket functionality', () => {
    const { result } = renderHook(() => useBuddyWebSocket());

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
});