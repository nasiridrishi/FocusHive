import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {renderHook} from '@testing-library/react';
import {useWebSocketWithAuth} from './useWebSocketWithAuth';
import {useWebSocket} from './useWebSocket';
import type {NotificationMessage} from '../services/websocket/WebSocketService';
import {PresenceStatus} from '../services/websocket/WebSocketService';
import webSocketService from '../services/websocket/WebSocketService';

// Define interface for extended mock return type
interface ExtendedWebSocketReturn {
  isConnected: boolean;
  connectionState: string;
  connectionInfo: {
    isConnected: boolean;
    connectionState: string;
    reconnectionInfo: {
      attempts: number;
      maxAttempts: number;
      isReconnecting: boolean;
    };
  };
  connect: ReturnType<typeof vi.fn>;
  disconnect: ReturnType<typeof vi.fn>;
  retryConnection: ReturnType<typeof vi.fn>;
  reconnectWithNewToken: ReturnType<typeof vi.fn>;
  sendMessage: ReturnType<typeof vi.fn>;
  subscribe: ReturnType<typeof vi.fn>;
  unsubscribe: ReturnType<typeof vi.fn>;
  presenceStatus: PresenceStatus;
  updatePresence: ReturnType<typeof vi.fn>;
  startFocusSession: ReturnType<typeof vi.fn>;
  notifications: NotificationMessage[];
  clearNotification: ReturnType<typeof vi.fn>;
  clearAllNotifications: ReturnType<typeof vi.fn>;
  service: typeof webSocketService;
  customProperty?: string;
  customMethod?: ReturnType<typeof vi.fn>;
}

// Mock the useWebSocket hook
vi.mock('./useWebSocket', () => ({
  useWebSocket: vi.fn()
}));

describe('useWebSocketWithAuth', () => {
  const mockUseWebSocket = vi.mocked(useWebSocket);
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
    service: webSocketService
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockUseWebSocket.mockReturnValue(mockWebSocketReturn);
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should initialize with correct default values', () => {
    const {result} = renderHook(() =>
        useWebSocketWithAuth()
    );

    expect(result.current.hasAuth).toBe(false);
    expect(result.current.token).toBeUndefined();
    expect(result.current.isConnected).toBe(false);
    expect(result.current.connectionState).toBe('DISCONNECTED');

    // Verify useWebSocket was called with correct default options
    expect(mockUseWebSocket).toHaveBeenCalledWith({
      autoConnect: true,
      authTokenProvider: expect.any(Function),
      onConnect: undefined,
      onDisconnect: undefined,
      onMessage: undefined,
      onPresenceUpdate: undefined,
      onNotification: undefined
    });
  });

  it('should pass through all options to useWebSocket', () => {
    const onConnect = vi.fn();
    const onDisconnect = vi.fn();
    const onMessage = vi.fn();
    const onPresenceUpdate = vi.fn();
    const onNotification = vi.fn();

    renderHook(() =>
        useWebSocketWithAuth({
          token: 'test-token',
          autoConnect: false,
          onConnect,
          onDisconnect,
          onMessage,
          onPresenceUpdate,
          onNotification
        })
    );

    expect(mockUseWebSocket).toHaveBeenCalledWith({
      autoConnect: false,
      authTokenProvider: expect.any(Function),
      onConnect,
      onDisconnect,
      onMessage,
      onPresenceUpdate,
      onNotification
    });
  });

  it('should create auth token provider that returns provided token', () => {
    const token = 'test-jwt-token';

    renderHook(() =>
        useWebSocketWithAuth({token})
    );

    // Get the auth token provider function that was passed to useWebSocket
    const authTokenProvider = mockUseWebSocket.mock.calls[0][0].authTokenProvider;

    expect(authTokenProvider).toBeDefined();
    expect(authTokenProvider?.()).toBe(token);
  });

  it('should create auth token provider that returns null when no token', () => {
    renderHook(() =>
        useWebSocketWithAuth({token: null})
    );

    const authTokenProvider = mockUseWebSocket.mock.calls[0][0].authTokenProvider;

    expect(authTokenProvider?.()).toBeNull();
  });

  it('should have hasAuth as true when token is provided', () => {
    const {result} = renderHook(() =>
        useWebSocketWithAuth({token: 'valid-token'})
    );

    expect(result.current.hasAuth).toBe(true);
    expect(result.current.token).toBe('valid-token');
  });

  it('should have hasAuth as false when token is null', () => {
    const {result} = renderHook(() =>
        useWebSocketWithAuth({token: null})
    );

    expect(result.current.hasAuth).toBe(false);
    expect(result.current.token).toBeNull();
  });

  it('should have hasAuth as false when token is undefined', () => {
    const {result} = renderHook(() =>
        useWebSocketWithAuth({token: undefined})
    );

    expect(result.current.hasAuth).toBe(false);
    expect(result.current.token).toBeUndefined();
  });

  it('should reconnect with new token when token changes and already connected', () => {
    const mockWebSocketConnected = {
      ...mockWebSocketReturn,
      isConnected: true
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketConnected);

    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token}),
        {initialProps: {token: 'initial-token'}}
    );

    // Change token
    rerender({token: 'new-token'});

    expect(mockWebSocketConnected.reconnectWithNewToken).toHaveBeenCalled();
  });

  it('should connect when token is provided and not connected with autoConnect', () => {
    const mockWebSocketDisconnected = {
      ...mockWebSocketReturn,
      isConnected: false
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketDisconnected);

    renderHook(() =>
        useWebSocketWithAuth({
          token: 'new-token',
          autoConnect: true
        })
    );

    expect(mockWebSocketDisconnected.connect).toHaveBeenCalled();
  });

  it('should not connect when autoConnect is false', () => {
    const mockWebSocketDisconnected = {
      ...mockWebSocketReturn,
      isConnected: false
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketDisconnected);

    renderHook(() =>
        useWebSocketWithAuth({
          token: 'new-token',
          autoConnect: false
        })
    );

    expect(mockWebSocketDisconnected.connect).not.toHaveBeenCalled();
  });

  it('should not reconnect when token changes but not connected', () => {
    const mockWebSocketDisconnected = {
      ...mockWebSocketReturn,
      isConnected: false
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketDisconnected);

    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token, autoConnect: false}),
        {initialProps: {token: 'initial-token'}}
    );

    // Change token
    rerender({token: 'new-token'});

    expect(mockWebSocketDisconnected.reconnectWithNewToken).not.toHaveBeenCalled();
  });

  it('should handle token changing from null to valid token', () => {
    const mockWebSocketDisconnected = {
      ...mockWebSocketReturn,
      isConnected: false
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketDisconnected);

    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token}),
        {initialProps: {token: null}}
    );

    // Change token from null to valid
    rerender({token: 'valid-token'});

    expect(mockWebSocketDisconnected.connect).toHaveBeenCalled();
  });

  it('should handle token changing from valid to null', () => {
    const mockWebSocketConnected = {
      ...mockWebSocketReturn,
      isConnected: true
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketConnected);

    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token}),
        {initialProps: {token: 'valid-token'}}
    );

    // Clear the mock calls from initial render
    mockWebSocketConnected.reconnectWithNewToken.mockClear();

    // Change token from valid to null
    rerender({token: null});

    // Should not call reconnectWithNewToken when token becomes null
    expect(mockWebSocketConnected.reconnectWithNewToken).not.toHaveBeenCalled();
  });

  it('should update auth token provider when token changes', () => {
    let capturedAuthProvider: (() => string | null) | undefined;

    mockUseWebSocket.mockImplementation((options) => {
      capturedAuthProvider = options.authTokenProvider;
      return mockWebSocketReturn;
    });

    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token}),
        {initialProps: {token: 'initial-token'}}
    );

    expect(capturedAuthProvider?.()).toBe('initial-token');

    // Change token
    rerender({token: 'updated-token'});

    expect(capturedAuthProvider?.()).toBe('updated-token');
  });

  it('should expose all useWebSocket properties and methods', () => {
    const mockExtendedWebSocket: ExtendedWebSocketReturn = {
      ...mockWebSocketReturn,
      customProperty: 'test',
      customMethod: vi.fn()
    };
    mockUseWebSocket.mockReturnValue(mockExtendedWebSocket as ExtendedWebSocketReturn);

    const {result} = renderHook(() =>
        useWebSocketWithAuth({token: 'test-token'})
    );

    // Check that all original properties are available
    expect(result.current.isConnected).toBe(mockExtendedWebSocket.isConnected);
    expect(result.current.connectionState).toBe(mockExtendedWebSocket.connectionState);
    expect(result.current.connect).toBe(mockExtendedWebSocket.connect);
    expect(result.current.disconnect).toBe(mockExtendedWebSocket.disconnect);
    expect(result.current.sendMessage).toBe(mockExtendedWebSocket.sendMessage);

    // Check additional auth-related properties
    expect(result.current.hasAuth).toBe(true);
    expect(result.current.token).toBe('test-token');

    // Check that custom properties are also spread (type-safe access)
    const resultWithCustomProps = result.current as typeof result.current & {
      customProperty?: string;
      customMethod?: ReturnType<typeof vi.fn>;
    };
    expect(resultWithCustomProps.customProperty).toBe('test');
    expect(resultWithCustomProps.customMethod).toBe(mockExtendedWebSocket.customMethod);
  });

  it('should handle rapid token changes gracefully', () => {
    const mockWebSocketConnected = {
      ...mockWebSocketReturn,
      isConnected: true
    };
    mockUseWebSocket.mockReturnValue(mockWebSocketConnected);

    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token}),
        {initialProps: {token: 'token1'}}
    );

    // Clear initial render calls
    mockWebSocketConnected.reconnectWithNewToken.mockClear();

    // Rapid token changes
    rerender({token: 'token2'});
    rerender({token: 'token3'});
    rerender({token: 'token4'});

    // Should call reconnectWithNewToken for each change (excluding initial render)
    expect(mockWebSocketConnected.reconnectWithNewToken).toHaveBeenCalledTimes(3);
  });

  it('should handle empty string token', () => {
    const {result} = renderHook(() =>
        useWebSocketWithAuth({token: ''})
    );

    expect(result.current.hasAuth).toBe(false);
    expect(result.current.token).toBe('');

    const authTokenProvider = mockUseWebSocket.mock.calls[0][0].authTokenProvider;
    expect(authTokenProvider?.()).toBeNull(); // Empty string becomes null via || null
  });

  it('should handle whitespace-only token', () => {
    const {result} = renderHook(() =>
        useWebSocketWithAuth({token: '   '})
    );

    expect(result.current.hasAuth).toBe(true); // Truthy check
    expect(result.current.token).toBe('   ');

    const authTokenProvider = mockUseWebSocket.mock.calls[0][0].authTokenProvider;
    expect(authTokenProvider?.()).toBe('   ');
  });

  it('should maintain stable auth token provider reference when token unchanged', () => {
    const {rerender} = renderHook(
        ({token}: { token?: string | null }) =>
            useWebSocketWithAuth({token}),
        {initialProps: {token: 'stable-token'}}
    );

    const firstCall = mockUseWebSocket.mock.calls[0][0].authTokenProvider;

    // Re-render with same token
    rerender({token: 'stable-token'});

    const secondCall = mockUseWebSocket.mock.calls[1][0].authTokenProvider;

    // The auth token provider function should be referentially stable
    expect(firstCall).toBe(secondCall);
  });
});