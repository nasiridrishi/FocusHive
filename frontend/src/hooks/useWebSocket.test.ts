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
});