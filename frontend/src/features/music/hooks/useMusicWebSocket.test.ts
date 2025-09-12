import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useMusicWebSocket } from './useMusicWebSocket';
import type { WebSocketMessage } from '../types';

// Create mock socket object
const createMockSocket = () => ({
  connected: false,
  id: 'test-socket-id',
  io: {
    engine: {
      transport: {
        name: 'websocket'
      }
    }
  },
  on: vi.fn(),
  off: vi.fn(),
  emit: vi.fn(),
  disconnect: vi.fn(),
  connect: vi.fn()
});

// Mock socket.io-client
vi.mock('socket.io-client', () => {
  const mockSocket = createMockSocket();
  const mockIo = vi.fn(() => mockSocket);
  return {
    io: mockIo
  };
});

describe('useMusicWebSocket', () => {
  let eventHandlers: Record<string, (data?: unknown) => void> = {};
  let mockSocket: ReturnType<typeof createMockSocket>;
  let mockIo: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.clearAllMocks();
    eventHandlers = {};
    
    // Get fresh mocks
    const { io } = await import('socket.io-client');
    mockIo = vi.mocked(io);
    mockSocket = createMockSocket();
    mockIo.mockReturnValue(mockSocket);
    
    // Mock socket.on to capture event handlers
    mockSocket.on.mockImplementation((event: string, handler: (data?: unknown) => void) => {
      eventHandlers[event] = handler;
      return mockSocket;
    });
    
    // Reset socket state
    mockSocket.connected = false;
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should initialize with correct default state', () => {
    const { result } = renderHook(() => useMusicWebSocket());

    expect(result.current.isConnected).toBe(false);
    expect(result.current.isConnecting).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.lastMessage).toBeNull();
    expect(result.current.reconnectAttempts).toBe(0);
    expect(result.current.canReconnect).toBe(true);
    expect(result.current.isHealthy).toBe(false);
  });

  it('should establish connection with correct configuration', () => {
    const hiveId = 'test-hive-123';
    const onConnect = vi.fn();
    const onDisconnect = vi.fn();
    const onMessage = vi.fn();

    renderHook(() => useMusicWebSocket({
      hiveId,
      onConnect,
      onDisconnect,
      onMessage
    }));

    expect(mockIo).toHaveBeenCalledWith('ws://localhost:8084', {
      path: '/ws/music',
      transports: ['websocket'],
      query: { hiveId },
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      timeout: 20000
    });
  });

  it('should establish connection without hiveId when not provided', () => {
    renderHook(() => useMusicWebSocket());

    expect(mockIo).toHaveBeenCalledWith('ws://localhost:8084', {
      path: '/ws/music',
      transports: ['websocket'],
      query: {},
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      timeout: 20000
    });
  });

  it('should handle successful connection', async () => {
    const onConnect = vi.fn();
    
    const { result } = renderHook(() => useMusicWebSocket({
      onConnect
    }));

    expect(result.current.isConnecting).toBe(true);

    // Simulate successful connection
    act(() => {
      mockSocket.connected = true;
      eventHandlers['connect']?.();
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
      expect(result.current.isConnecting).toBe(false);
      expect(result.current.error).toBeNull();
      expect(result.current.reconnectAttempts).toBe(0);
      expect(result.current.isHealthy).toBe(true);
      expect(onConnect).toHaveBeenCalledTimes(1);
    });
  });

  it('should handle disconnection', async () => {
    const onDisconnect = vi.fn();
    
    const { result } = renderHook(() => useMusicWebSocket({
      onDisconnect
    }));

    // First establish connection
    act(() => {
      mockSocket.connected = true;
      eventHandlers['connect']?.();
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
    });

    // Then simulate disconnection
    act(() => {
      mockSocket.connected = false;
      eventHandlers['disconnect']?.('transport close');
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(false);
      expect(result.current.isConnecting).toBe(false);
      expect(result.current.error).toBe('Disconnected: transport close');
      expect(onDisconnect).toHaveBeenCalledTimes(1);
    });
  });

  it('should handle connection errors', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Simulate connection error
    act(() => {
      eventHandlers['connect_error']?.(new Error('Connection failed'));
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(false);
      expect(result.current.isConnecting).toBe(false);
      expect(result.current.error).toBe('Connection error: Connection failed');
      expect(result.current.reconnectAttempts).toBe(1);
    });
  });

  it('should handle track_added message', async () => {
    const onMessage = vi.fn();
    
    const { result } = renderHook(() => useMusicWebSocket({
      onMessage
    }));

    const trackData = {
      trackId: 'track-123',
      title: 'Test Song',
      artist: 'Test Artist',
      userId: 'user-456'
    };

    // Simulate track_added event
    act(() => {
      eventHandlers['track_added']?.(trackData);
    });

    await waitFor(() => {
      expect(result.current.lastMessage).toEqual({
        type: 'track_added',
        payload: trackData,
        timestamp: expect.any(String),
        userId: 'user-456'
      });
      expect(onMessage).toHaveBeenCalledWith(expect.objectContaining({
        type: 'track_added',
        payload: trackData
      }));
    });
  });

  it('should handle track_voted message', async () => {
    const onMessage = vi.fn();
    
    const { result } = renderHook(() => useMusicWebSocket({
      onMessage
    }));

    const voteData = {
      trackId: 'track-123',
      vote: 'up',
      votes: 5,
      userId: 'user-456'
    };

    act(() => {
      eventHandlers['track_voted']?.(voteData);
    });

    await waitFor(() => {
      expect(result.current.lastMessage).toEqual({
        type: 'track_voted',
        payload: voteData,
        timestamp: expect.any(String),
        userId: 'user-456'
      });
      expect(onMessage).toHaveBeenCalledWith(expect.objectContaining({
        type: 'track_voted',
        payload: voteData
      }));
    });
  });

  it('should handle queue_updated message', async () => {
    const onMessage = vi.fn();
    
    const { result } = renderHook(() => useMusicWebSocket({
      onMessage
    }));

    const queueData = {
      queue: [
        { trackId: 'track-1', position: 0, votes: 3 },
        { trackId: 'track-2', position: 1, votes: 1 }
      ]
    };

    act(() => {
      eventHandlers['queue_updated']?.(queueData);
    });

    await waitFor(() => {
      expect(result.current.lastMessage).toEqual({
        type: 'queue_updated',
        payload: queueData,
        timestamp: expect.any(String),
        userId: 'system'
      });
      expect(onMessage).toHaveBeenCalledWith(expect.objectContaining({
        type: 'queue_updated',
        payload: queueData
      }));
    });
  });

  it('should handle user_joined and user_left messages', async () => {
    const onMessage = vi.fn();
    
    renderHook(() => useMusicWebSocket({
      onMessage
    }));

    const userData = {
      userId: 'user-789',
      username: 'TestUser'
    };

    // Test user joined
    act(() => {
      eventHandlers['user_joined']?.(userData);
    });

    await waitFor(() => {
      expect(onMessage).toHaveBeenCalledWith(expect.objectContaining({
        type: 'user_joined',
        payload: userData
      }));
    });

    // Test user left
    act(() => {
      eventHandlers['user_left']?.(userData);
    });

    await waitFor(() => {
      expect(onMessage).toHaveBeenCalledWith(expect.objectContaining({
        type: 'user_left',
        payload: userData
      }));
    });
  });

  it('should send messages when connected', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Establish connection
    act(() => {
      mockSocket.connected = true;
      eventHandlers['connect']?.();
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
    });

    // Test sending message
    act(() => {
      result.current.sendMessage('test_event', { data: 'test' });
    });

    expect(mockSocket.emit).toHaveBeenCalledWith('test_event', { data: 'test' });
  });

  it('should queue messages when disconnected', () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Send message while disconnected
    act(() => {
      result.current.sendMessage('queued_event', { data: 'queued' });
    });

    // Message should not be sent immediately
    expect(mockSocket.emit).not.toHaveBeenCalled();

    // Establish connection
    act(() => {
      mockSocket.connected = true;
      eventHandlers['connect']?.();
    });

    // Queued messages should be sent on connection
    expect(mockSocket.emit).toHaveBeenCalledWith('queued_event', { data: 'queued' });
  });

  it('should provide music-specific action methods', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Establish connection
    act(() => {
      mockSocket.connected = true;
      eventHandlers['connect']?.();
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
    });

    // Test hive actions
    act(() => {
      result.current.joinHive('hive-123');
    });
    expect(mockSocket.emit).toHaveBeenCalledWith('join_hive', { hiveId: 'hive-123' });

    act(() => {
      result.current.leaveHive('hive-123');
    });
    expect(mockSocket.emit).toHaveBeenCalledWith('leave_hive', { hiveId: 'hive-123' });

    // Test queue actions
    act(() => {
      result.current.addTrackToQueue('track-456', 2);
    });
    expect(mockSocket.emit).toHaveBeenCalledWith('add_to_queue', { trackId: 'track-456', position: 2 });

    act(() => {
      result.current.voteOnTrack('queue-789', 'up');
    });
    expect(mockSocket.emit).toHaveBeenCalledWith('vote_track', { queueId: 'queue-789', vote: 'up' });

    // Test playback actions
    const playbackState = { isPlaying: true, currentTime: 30, volume: 0.8 };
    act(() => {
      result.current.updatePlaybackState(playbackState);
    });
    expect(mockSocket.emit).toHaveBeenCalledWith('playback_update', playbackState);
  });

  it('should handle manual reconnection', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Initial state should allow reconnection
    expect(result.current.canReconnect).toBe(true);

    // Attempt manual reconnection
    act(() => {
      result.current.reconnect();
    });

    // Should disconnect and attempt to reconnect after delay
    expect(mockSocket.disconnect).toHaveBeenCalled();
    
    // Fast-forward timers
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    
    // New connection attempt should be made
    expect(mockIo).toHaveBeenCalledTimes(2);
  });

  it('should prevent reconnection when already connecting or connected', () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Set connecting state
    act(() => {
      result.current.reconnect();
    });

    const initialCallCount = mockIo.mock.calls.length;

    // Try to reconnect again
    act(() => {
      result.current.reconnect();
    });

    // Should not make additional connection attempts
    expect(mockIo).toHaveBeenCalledTimes(initialCallCount);
  });

  it('should handle reconnection events', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Simulate reconnection successful
    act(() => {
      eventHandlers['reconnect']?.();
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
      expect(result.current.isConnecting).toBe(false);
      expect(result.current.error).toBeNull();
      expect(result.current.reconnectAttempts).toBe(0);
    });

    // Simulate reconnection error
    act(() => {
      eventHandlers['reconnect_error']?.(new Error('Reconnect failed'));
    });

    await waitFor(() => {
      expect(result.current.error).toBe('Reconnection failed: Reconnect failed');
      expect(result.current.reconnectAttempts).toBe(1);
    });

    // Simulate reconnection failure
    act(() => {
      eventHandlers['reconnect_failed']?.();
    });

    await waitFor(() => {
      expect(result.current.error).toBe('Failed to reconnect after maximum attempts');
      expect(result.current.isConnecting).toBe(false);
    });
  });

  it('should provide connection utilities', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    // Test checkConnection when disconnected
    expect(result.current.checkConnection()).toBe(false);

    // Test getConnectionInfo when disconnected
    expect(result.current.getConnectionInfo()).toBeNull();

    // Establish connection
    act(() => {
      mockSocket.connected = true;
      eventHandlers['connect']?.();
    });

    await waitFor(() => {
      expect(result.current.isConnected).toBe(true);
    });

    // Test checkConnection when connected
    expect(result.current.checkConnection()).toBe(true);

    // Test getConnectionInfo when connected
    expect(result.current.getConnectionInfo()).toEqual({
      id: 'test-socket-id',
      connected: true,
      transport: 'websocket'
    });
  });

  it('should cleanup on unmount', () => {
    const { unmount } = renderHook(() => useMusicWebSocket());

    unmount();

    expect(mockSocket.disconnect).toHaveBeenCalled();
  });

  it('should handle reconnection when hiveId changes', () => {
    const { rerender } = renderHook(
      ({ hiveId }: { hiveId?: string }) => useMusicWebSocket({ hiveId }),
      { initialProps: { hiveId: 'hive-1' } }
    );

    expect(mockIo).toHaveBeenCalledTimes(1);

    // Change hiveId
    rerender({ hiveId: 'hive-2' });

    // Should trigger disconnection and new connection
    expect(mockSocket.disconnect).toHaveBeenCalled();
    expect(mockIo).toHaveBeenCalledTimes(2);
    expect(mockIo).toHaveBeenLastCalledWith('ws://localhost:8084', expect.objectContaining({
      query: { hiveId: 'hive-2' }
    }));
  });

  it('should calculate canReconnect correctly', async () => {
    const { result } = renderHook(() => useMusicWebSocket());

    expect(result.current.canReconnect).toBe(true);

    // Simulate multiple connection errors to exceed max attempts
    for (let i = 0; i < 5; i++) {
      act(() => {
        eventHandlers['connect_error']?.(new Error('Connection failed'));
      });
    }

    await waitFor(() => {
      expect(result.current.reconnectAttempts).toBe(5);
      expect(result.current.canReconnect).toBe(false);
    });
  });

  it('should handle connection errors gracefully', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    // Mock io to throw error
    mockIo.mockImplementationOnce(() => {
      throw new Error('Connection setup failed');
    });

    const { result } = renderHook(() => useMusicWebSocket());

    expect(result.current.isConnecting).toBe(false);
    expect(result.current.error).toBe('Connection failed: Connection setup failed');

    consoleSpy.mockRestore();
  });
});