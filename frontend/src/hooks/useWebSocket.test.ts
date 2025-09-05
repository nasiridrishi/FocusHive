import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useWebSocket } from '../useWebSocket';

// Mock WebSocket
const mockWebSocket = {
  readyState: WebSocket.CONNECTING,
  send: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
};

// Mock WebSocket constructor
const mockWebSocketConstructor = vi.fn(() => mockWebSocket);
global.WebSocket = mockWebSocketConstructor as unknown as typeof WebSocket;

// WebSocket ready state constants
Object.defineProperty(global, 'WebSocket', {
  value: mockWebSocketConstructor,
  writable: true
});

Object.defineProperty(mockWebSocketConstructor, 'CONNECTING', { value: 0 });
Object.defineProperty(mockWebSocketConstructor, 'OPEN', { value: 1 });
Object.defineProperty(mockWebSocketConstructor, 'CLOSING', { value: 2 });
Object.defineProperty(mockWebSocketConstructor, 'CLOSED', { value: 3 });

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockWebSocket.readyState = WebSocket.CONNECTING;
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should initialize with correct default values', () => {
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    expect(result.current.connectionStatus).toBe('Connecting');
    expect(result.current.lastMessage).toBeNull();
    expect(typeof result.current.sendMessage).toBe('function');
  });

  it('should create WebSocket connection with correct URL', () => {
    const url = 'ws://localhost:8080/test';
    renderHook(() => useWebSocket(url, { shouldReconnect: () => false }));

    expect(mockWebSocketConstructor).toHaveBeenCalledWith(url);
  });

  it('should handle connection open event', async () => {
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    // Simulate WebSocket open event
    mockWebSocket.readyState = WebSocket.OPEN;
    const openHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'open'
    )?.[1];

    if (openHandler) {
      openHandler();
    }

    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('Open');
    });
  });

  it('should handle connection close event', async () => {
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    // Simulate WebSocket close event
    mockWebSocket.readyState = WebSocket.CLOSED;
    const closeHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'close'
    )?.[1];

    if (closeHandler) {
      closeHandler({ code: 1000, reason: 'Normal closure' });
    }

    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('Closed');
    });
  });

  it('should handle connection error event', async () => {
    const onError = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        onError,
        shouldReconnect: () => false
      })
    );

    // Simulate WebSocket error event
    const errorHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'error'
    )?.[1];

    if (errorHandler) {
      const errorEvent = new Event('error');
      errorHandler(errorEvent);
    }

    await waitFor(() => {
      expect(onError).toHaveBeenCalledWith(expect.any(Event));
    });
  });

  it('should handle incoming messages', async () => {
    const onMessage = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        onMessage,
        shouldReconnect: () => false
      })
    );

    // Simulate WebSocket message event
    const messageHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'message'
    )?.[1];

    if (messageHandler) {
      const messageEvent = {
        data: JSON.stringify({ type: 'test', payload: 'hello' })
      };
      messageHandler(messageEvent);
    }

    await waitFor(() => {
      expect(onMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          data: JSON.stringify({ type: 'test', payload: 'hello' })
        })
      );
      expect(result.current.lastMessage).toEqual(
        expect.objectContaining({
          data: JSON.stringify({ type: 'test', payload: 'hello' })
        })
      );
    });
  });

  it('should send messages when connection is open', async () => {
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    // Simulate connection is open
    mockWebSocket.readyState = WebSocket.OPEN;

    const message = { type: 'test', payload: 'hello' };
    result.current.sendMessage(message);

    expect(mockWebSocket.send).toHaveBeenCalledWith(JSON.stringify(message));
  });

  it('should not send messages when connection is not open', () => {
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    // Connection is not open (CONNECTING by default)
    mockWebSocket.readyState = WebSocket.CONNECTING;

    const message = { type: 'test', payload: 'hello' };
    result.current.sendMessage(message);

    expect(mockWebSocket.send).not.toHaveBeenCalled();
  });

  it('should handle string messages', async () => {
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    // Simulate connection is open
    mockWebSocket.readyState = WebSocket.OPEN;

    result.current.sendMessage('hello world');

    expect(mockWebSocket.send).toHaveBeenCalledWith('hello world');
  });

  it('should attempt reconnection when shouldReconnect returns true', async () => {
    const shouldReconnect = vi.fn(() => true);
    
    renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect,
        reconnectInterval: 100
      })
    );

    // Simulate connection close
    mockWebSocket.readyState = WebSocket.CLOSED;
    const closeHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'close'
    )?.[1];

    if (closeHandler) {
      closeHandler({ code: 1006, reason: 'Connection lost' });
    }

    // Wait for reconnection attempt
    await waitFor(() => {
      expect(shouldReconnect).toHaveBeenCalledWith({
        code: 1006,
        reason: 'Connection lost'
      });
    });

    // Fast-forward timers to trigger reconnection
    vi.advanceTimersByTime(100);
    
    await waitFor(() => {
      expect(mockWebSocketConstructor).toHaveBeenCalledTimes(2);
    });
  });

  it('should not reconnect when shouldReconnect returns false', async () => {
    const shouldReconnect = vi.fn(() => false);
    
    renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect
      })
    );

    // Simulate connection close
    mockWebSocket.readyState = WebSocket.CLOSED;
    const closeHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'close'
    )?.[1];

    if (closeHandler) {
      closeHandler({ code: 1000, reason: 'Normal closure' });
    }

    await waitFor(() => {
      expect(shouldReconnect).toHaveBeenCalledWith({
        code: 1000,
        reason: 'Normal closure'
      });
    });

    // Should not attempt reconnection
    expect(mockWebSocketConstructor).toHaveBeenCalledTimes(1);
  });

  it('should clean up connection on unmount', () => {
    const { unmount } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect: () => false
      })
    );

    unmount();

    expect(mockWebSocket.close).toHaveBeenCalled();
  });

  it('should respect reconnect attempts limit', async () => {
    const shouldReconnect = vi.fn(() => true);
    const reconnectAttempts = 2;
    
    renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        shouldReconnect,
        reconnectAttempts,
        reconnectInterval: 100
      })
    );

    // Simulate multiple connection failures
    for (let i = 0; i < reconnectAttempts + 2; i++) {
      mockWebSocket.readyState = WebSocket.CLOSED;
      const closeHandler = mockWebSocket.addEventListener.mock.calls.find(
        call => call[0] === 'close'
      )?.[1];

      if (closeHandler) {
        closeHandler({ code: 1006, reason: 'Connection lost' });
      }

      vi.advanceTimersByTime(100);
    }

    await waitFor(() => {
      // Should only attempt initial connection + reconnectAttempts
      expect(mockWebSocketConstructor).toHaveBeenCalledTimes(1 + reconnectAttempts);
    });
  });

  it('should handle JSON parsing errors gracefully', async () => {
    const onMessage = vi.fn();
    const { result } = renderHook(() => 
      useWebSocket('ws://localhost:8080', {
        onMessage,
        shouldReconnect: () => false
      })
    );

    // Simulate WebSocket message event with invalid JSON
    const messageHandler = mockWebSocket.addEventListener.mock.calls.find(
      call => call[0] === 'message'
    )?.[1];

    if (messageHandler) {
      const messageEvent = {
        data: 'invalid json{'
      };
      messageHandler(messageEvent);
    }

    await waitFor(() => {
      expect(onMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          data: 'invalid json{'
        })
      );
      expect(result.current.lastMessage).toEqual(
        expect.objectContaining({
          data: 'invalid json{'
        })
      );
    });
  });
});