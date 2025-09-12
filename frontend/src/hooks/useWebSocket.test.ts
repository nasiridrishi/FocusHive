import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useWebSocket } from './useWebSocket';
import type { WebSocketMessage, PresenceUpdate } from '../services/websocket/WebSocketService';
import webSocketService, { PresenceStatus } from '../services/websocket/WebSocketService';

// Mock the WebSocketService module
vi.mock('../services/websocket/WebSocketService', () => ({
  default: {
    connect: vi.fn(),
    disconnect: vi.fn(),
    sendMessage: vi.fn(),
    subscribe: vi.fn(() => 'mock-subscription-id'),
    unsubscribe: vi.fn(),
    setAuthTokenProvider: vi.fn(),
    getConnectionState: vi.fn(() => 'DISCONNECTED'),
    getReconnectionInfo: vi.fn(() => ({ attempts: 0, maxAttempts: 10, isReconnecting: false })),
    onConnectionChange: vi.fn(),
    onMessage: vi.fn(),
    onPresenceUpdate: vi.fn(),
    updatePresenceStatus: vi.fn(),
    startFocusSession: vi.fn(),
    retryConnection: vi.fn(),
    reconnectWithNewToken: vi.fn(),
    subscribeToBuddyUpdates: vi.fn(() => 'buddy-sub-id'),
    sendBuddyRequest: vi.fn(),
    acceptBuddyRequest: vi.fn(),
    sendBuddyCheckin: vi.fn(),
    updateBuddyGoal: vi.fn(),
    startBuddySession: vi.fn(),
    endBuddySession: vi.fn(),
    subscribeToForumPost: vi.fn(() => 'forum-sub-id'),
    createForumPost: vi.fn(),
    createForumReply: vi.fn(),
    voteOnPost: vi.fn(),
    voteOnReply: vi.fn(),
    acceptReply: vi.fn(),
    editForumPost: vi.fn(),
    setTypingStatus: vi.fn(),
    subscribeToHivePresence: vi.fn(() => 'hive-presence-sub-id'),
    getHivePresence: vi.fn(),
    getHiveOnlineCount: vi.fn()
  },
  PresenceStatus: {
    ONLINE: 'ONLINE',
    AWAY: 'AWAY',
    BUSY: 'BUSY',
    IN_FOCUS_SESSION: 'IN_FOCUS_SESSION',
    IN_BUDDY_SESSION: 'IN_BUDDY_SESSION',
    DO_NOT_DISTURB: 'DO_NOT_DISTURB',
    OFFLINE: 'OFFLINE'
  }
}));

describe('useWebSocket', () => {
  let connectionChangeHandler: (connected: boolean) => void;
  let _messageHandler: ((message: WebSocketMessage) => void) | undefined;
  let _presenceHandler: ((presence: PresenceUpdate) => void) | undefined;
  const mockService = vi.mocked(webSocketService);

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Reset service state
    mockService.getConnectionState.mockReturnValue('DISCONNECTED');
    mockService.getReconnectionInfo.mockReturnValue({
      attempts: 0,
      maxAttempts: 10,
      isReconnecting: false
    });
    
    // Capture handlers when they are registered
    mockService.onConnectionChange.mockImplementation((handler) => {
      connectionChangeHandler = handler;
    });
    
    mockService.onMessage.mockImplementation((type, handler) => {
      if (type === 'general') {
        _messageHandler = handler;
      }
    });
    
    mockService.onPresenceUpdate.mockImplementation((handler) => {
      _presenceHandler = handler;
    });
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should initialize with correct default values', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    expect(result.current.connectionState).toBe('DISCONNECTED');
    expect(result.current.isConnected).toBe(false);
    expect(result.current.presenceStatus).toBe(PresenceStatus.OFFLINE);
    expect(result.current.notifications).toEqual([]);
    expect(typeof result.current.sendMessage).toBe('function');
    expect(typeof result.current.connect).toBe('function');
    expect(typeof result.current.disconnect).toBe('function');
    expect(typeof result.current.subscribe).toBe('function');
    expect(typeof result.current.unsubscribe).toBe('function');
    expect(result.current.service).toBe(webSocketService);
  });

  it('should auto-connect when autoConnect is true', () => {
    renderHook(() => useWebSocket({ autoConnect: true }));

    expect(mockService.connect).toHaveBeenCalled();
  });
  
  it('should not auto-connect when autoConnect is false', () => {
    renderHook(() => useWebSocket({ autoConnect: false }));

    expect(mockService.connect).not.toHaveBeenCalled();
  });

  it('should handle connection state changes', async () => {
    const onConnect = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false, onConnect })
    );

    // Simulate connection established
    mockService.getConnectionState.mockReturnValue('CONNECTED');
    
    act(() => {
      connectionChangeHandler(true);
    });

    await waitFor(() => {
      expect(result.current.connectionState).toBe('CONNECTED');
      expect(result.current.isConnected).toBe(true);
      expect(onConnect).toHaveBeenCalled();
    });
  });

  it('should handle connection disconnection', async () => {
    const onDisconnect = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false, onDisconnect })
    );

    // First establish connection
    mockService.getConnectionState.mockReturnValue('CONNECTED');
    
    act(() => {
      connectionChangeHandler(true);
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
    });

    // Then simulate disconnection
    mockService.getConnectionState.mockReturnValue('DISCONNECTED');
    
    act(() => {
      connectionChangeHandler(false);
    });

    await waitFor(() => {
      expect(result.current.connectionState).toBe('DISCONNECTED');
      expect(result.current.isConnected).toBe(false);
      expect(onDisconnect).toHaveBeenCalled();
    });
  });

  it('should call onDisconnect on connection loss', async () => {
    const onDisconnect = vi.fn();
    renderHook(() => 
      useWebSocket({ 
        onDisconnect,
        autoConnect: false 
      })
    );

    // Simulate disconnection
    act(() => {
      connectionChangeHandler(false);
    });

    await waitFor(() => {
      expect(onDisconnect).toHaveBeenCalled();
    });
  });

  it('should handle incoming messages through service', async () => {
    const onMessage = vi.fn();
    renderHook(() => 
      useWebSocket({
        onMessage,
        autoConnect: false
      })
    );

    expect(mockService.onMessage).toHaveBeenCalledWith('general', onMessage);

    // Simulate message received through service handler
    const testMessage: WebSocketMessage = {
      id: 'test-id',
      type: 'TEST_MESSAGE' as WebSocketMessage['type'],
      event: 'test',
      payload: { data: 'hello' },
      timestamp: new Date().toISOString()
    };

    // Call the onMessage handler directly since it's registered with the service
    onMessage(testMessage);

    expect(onMessage).toHaveBeenCalledWith(testMessage);
  });

  it('should delegate sendMessage to service', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    const destination = '/app/test';
    const message = { type: 'test', payload: 'hello' };
    result.current.sendMessage(destination, message);

    expect(mockService.sendMessage).toHaveBeenCalledWith(destination, message);
  });

  it('should always delegate sendMessage regardless of connection state', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    // Service handles connection state internally
    const destination = '/app/test';
    const message = { type: 'test', payload: 'hello' };
    result.current.sendMessage(destination, message);

    expect(mockService.sendMessage).toHaveBeenCalledWith(destination, message);
  });

  it('should handle string messages through service', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    const destination = '/app/test';
    const message = 'hello world';
    result.current.sendMessage(destination, message);

    expect(mockService.sendMessage).toHaveBeenCalledWith(destination, message);
  });

  it('should provide retryConnection method', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    result.current.retryConnection();
    expect(mockService.retryConnection).toHaveBeenCalled();
  });
  
  it('should provide reconnectWithNewToken method', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    result.current.reconnectWithNewToken();
    expect(mockService.reconnectWithNewToken).toHaveBeenCalled();
  });

  it('should handle presence updates', async () => {
    const onPresenceUpdate = vi.fn();
    renderHook(() => 
      useWebSocket({ 
        onPresenceUpdate,
        autoConnect: false 
      })
    );

    expect(mockService.onPresenceUpdate).toHaveBeenCalledWith(onPresenceUpdate);

    // Simulate presence update
    const presenceUpdate: PresenceUpdate = {
      userId: 1,
      username: 'testuser',
      status: PresenceStatus.ONLINE,
      lastSeen: new Date().toISOString()
    };

    onPresenceUpdate(presenceUpdate);
    expect(onPresenceUpdate).toHaveBeenCalledWith(presenceUpdate);
  });

  it('should clean up subscriptions on unmount', () => {
    const { result, unmount } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    // Add a subscription
    const subId = result.current.subscribe('/test', vi.fn());
    expect(mockService.subscribe).toHaveBeenCalled();

    unmount();

    expect(mockService.unsubscribe).toHaveBeenCalledWith(subId);
  });

  it('should provide connection info with reconnection details', async () => {
    // Set up specific reconnection info for this test
    const reconnectionInfo = {
      attempts: 3,
      maxAttempts: 10,
      isReconnecting: true
    };
    
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    // Update mock to return specific reconnection info
    mockService.getReconnectionInfo.mockReturnValue(reconnectionInfo);
    mockService.getConnectionState.mockReturnValue('CONNECTING');
    
    // Trigger connection change handler to update connection info
    act(() => {
      connectionChangeHandler(false);
    });

    await waitFor(() => {
      // The connection info should reflect the mocked reconnection details
      expect(result.current.connectionInfo.reconnectionInfo).toEqual(reconnectionInfo);
      expect(result.current.connectionInfo.connectionState).toBe('CONNECTING');
    });
  });

  it('should handle notifications', async () => {
    const onNotification = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket({
        onNotification,
        autoConnect: false
      })
    );

    expect(mockService.onMessage).toHaveBeenCalledWith(
      'notification', 
      expect.any(Function)
    );

    expect(result.current.notifications).toEqual([]);
    expect(typeof result.current.clearNotification).toBe('function');
    expect(typeof result.current.clearAllNotifications).toBe('function');
  });
  
  it('should set auth token provider', () => {
    const authTokenProvider = vi.fn(() => 'test-token');
    renderHook(() => 
      useWebSocket({ 
        authTokenProvider,
        autoConnect: false 
      })
    );

    expect(mockService.setAuthTokenProvider).toHaveBeenCalledWith(authTokenProvider);
  });
  
  it('should provide presence management methods', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    expect(result.current.presenceStatus).toBe(PresenceStatus.OFFLINE);
    expect(typeof result.current.updatePresence).toBe('function');
    expect(typeof result.current.startFocusSession).toBe('function');
    
    // Test updatePresence
    act(() => {
      result.current.updatePresence(PresenceStatus.ONLINE, 1, 'Working');
    });
    expect(mockService.updatePresenceStatus).toHaveBeenCalledWith(
      PresenceStatus.ONLINE, 1, 'Working'
    );
    
    // Test startFocusSession
    act(() => {
      result.current.startFocusSession(1, 25);
    });
    expect(mockService.startFocusSession).toHaveBeenCalledWith(1, 25);
  });
  
  it('should provide subscribe/unsubscribe methods', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    const callback = vi.fn();
    const subId = result.current.subscribe('/test/destination', callback);
    
    expect(mockService.subscribe).toHaveBeenCalledWith('/test/destination', callback);
    expect(subId).toBe('mock-subscription-id');
    
    result.current.unsubscribe(subId);
    expect(mockService.unsubscribe).toHaveBeenCalledWith(subId);
  });

  it('should handle notification management', async () => {
    const onNotification = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket({
        onNotification,
        autoConnect: false
      })
    );

    // Mock notification handler call
    const mockNotificationHandler = vi.mocked(mockService.onMessage).mock.calls
      .find(([type]) => type === 'notification')?.[1];

    expect(mockNotificationHandler).toBeDefined();

    // Simulate notification received
    const testNotification = {
      id: 'notif-1',
      type: 'INFO',
      title: 'Test Notification',
      message: 'This is a test',
      priority: 'NORMAL' as const,
      createdAt: new Date().toISOString()
    };

    const notificationMessage = {
      id: 'msg-1',
      type: 'NOTIFICATION' as const,
      event: 'notification',
      payload: testNotification,
      timestamp: new Date().toISOString()
    };

    act(() => {
      mockNotificationHandler!(notificationMessage);
    });

    await waitFor(() => {
      expect(result.current.notifications).toHaveLength(1);
      expect(result.current.notifications[0]).toEqual(testNotification);
      expect(onNotification).toHaveBeenCalledWith(testNotification);
    });

    // Test clearing specific notification
    act(() => {
      result.current.clearNotification('notif-1');
    });

    await waitFor(() => {
      expect(result.current.notifications).toHaveLength(0);
    });
  });

  it('should handle multiple notifications and clear all', async () => {
    const onNotification = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket({ 
        autoConnect: false,
        onNotification 
      })
    );

    const mockNotificationHandler = vi.mocked(mockService.onMessage).mock.calls
      .find(([type]) => type === 'notification')?.[1];

    expect(mockNotificationHandler).toBeDefined();

    // Add multiple notifications
    const notifications = [
      {
        id: 'notif-1',
        type: 'INFO',
        title: 'First Notification',
        message: 'First message',
        priority: 'NORMAL' as const,
        createdAt: new Date().toISOString()
      },
      {
        id: 'notif-2',
        type: 'WARNING',
        title: 'Second Notification',
        message: 'Second message',
        priority: 'HIGH' as const,
        createdAt: new Date().toISOString()
      }
    ];

    for (const notif of notifications) {
      act(() => {
        mockNotificationHandler!({
          id: 'msg-' + notif.id,
          type: 'NOTIFICATION' as const,
          event: 'notification',
          payload: notif,
          timestamp: new Date().toISOString()
        });
      });
    }

    await waitFor(() => {
      expect(result.current.notifications).toHaveLength(2);
    });

    // Clear all notifications
    act(() => {
      result.current.clearAllNotifications();
    });

    await waitFor(() => {
      expect(result.current.notifications).toHaveLength(0);
    });
  });

  it('should handle reconnection scenarios', async () => {
    const onConnect = vi.fn();
    const onDisconnect = vi.fn();
    
    const { result } = renderHook(() => 
      useWebSocket({ 
        onConnect, 
        onDisconnect,
        autoConnect: false 
      })
    );

    // Initial connection
    mockService.getConnectionState.mockReturnValue('CONNECTED');
    act(() => {
      connectionChangeHandler(true);
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
      expect(onConnect).toHaveBeenCalledTimes(1);
    });

    // Connection lost
    mockService.getConnectionState.mockReturnValue('DISCONNECTED');
    mockService.getReconnectionInfo.mockReturnValue({
      attempts: 1,
      maxAttempts: 10,
      isReconnecting: true
    });

    act(() => {
      connectionChangeHandler(false);
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(false);
      expect(result.current.connectionInfo.reconnectionInfo.isReconnecting).toBe(true);
      expect(onDisconnect).toHaveBeenCalledTimes(1);
    });

    // Reconnection successful
    mockService.getConnectionState.mockReturnValue('CONNECTED');
    mockService.getReconnectionInfo.mockReturnValue({
      attempts: 0,
      maxAttempts: 10,
      isReconnecting: false
    });

    act(() => {
      connectionChangeHandler(true);
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
      expect(result.current.connectionInfo.reconnectionInfo.isReconnecting).toBe(false);
      expect(onConnect).toHaveBeenCalledTimes(2);
    });
  });

  it('should handle multiple subscriptions cleanup', () => {
    const { result, unmount } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    // Create multiple subscriptions
    const callback1 = vi.fn();
    const callback2 = vi.fn();
    const callback3 = vi.fn();
    
    const subId1 = result.current.subscribe('/test/1', callback1);
    const subId2 = result.current.subscribe('/test/2', callback2);  
    const subId3 = result.current.subscribe('/test/3', callback3);

    expect(mockService.subscribe).toHaveBeenCalledTimes(3);

    // Manually unsubscribe one
    result.current.unsubscribe(subId2);
    expect(mockService.unsubscribe).toHaveBeenCalledWith(subId2);

    // Unmount should cleanup remaining subscriptions
    unmount();
    
    expect(mockService.unsubscribe).toHaveBeenCalledWith(subId1);
    expect(mockService.unsubscribe).toHaveBeenCalledWith(subId3);
  });

  it('should handle edge case where subscription returns null', () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    // Mock subscribe to return null (error case)
    mockService.subscribe.mockReturnValueOnce(null);

    const callback = vi.fn();
    const subId = result.current.subscribe('/test/destination', callback);
    
    expect(subId).toBeNull();
    expect(mockService.subscribe).toHaveBeenCalledWith('/test/destination', callback);
  });

  it('should handle connection state during different phases', async () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    // Test CONNECTING state
    mockService.getConnectionState.mockReturnValue('CONNECTING');
    mockService.getReconnectionInfo.mockReturnValue({
      attempts: 0,
      maxAttempts: 10,
      isReconnecting: false
    });

    act(() => {
      connectionChangeHandler(false);
    });

    await waitFor(() => {
      expect(result.current.connectionState).toBe('CONNECTING');
      expect(result.current.isConnected).toBe(false);
    });

    // Test CONNECTED state
    mockService.getConnectionState.mockReturnValue('CONNECTED');
    act(() => {
      connectionChangeHandler(true);
    });

    await waitFor(() => {
      expect(result.current.connectionState).toBe('CONNECTED');
      expect(result.current.isConnected).toBe(true);
    });

    // Test ERROR state
    mockService.getConnectionState.mockReturnValue('ERROR');
    act(() => {
      connectionChangeHandler(false);
    });

    await waitFor(() => {
      expect(result.current.connectionState).toBe('ERROR');
      expect(result.current.isConnected).toBe(false);
    });
  });

  it('should update presence status locally when calling updatePresence', async () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    expect(result.current.presenceStatus).toBe(PresenceStatus.OFFLINE);

    act(() => {
      result.current.updatePresence(PresenceStatus.BUSY, 123, 'In a meeting');
    });

    await waitFor(() => {
      expect(result.current.presenceStatus).toBe(PresenceStatus.BUSY);
    });

    expect(mockService.updatePresenceStatus).toHaveBeenCalledWith(
      PresenceStatus.BUSY, 123, 'In a meeting'
    );
  });

  it('should update presence status when starting focus session', async () => {
    const { result } = renderHook(() => 
      useWebSocket({ autoConnect: false })
    );

    act(() => {
      result.current.startFocusSession(456, 30);
    });

    await waitFor(() => {
      expect(result.current.presenceStatus).toBe(PresenceStatus.IN_FOCUS_SESSION);
    });

    expect(mockService.startFocusSession).toHaveBeenCalledWith(456, 30);
  });
});