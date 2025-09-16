import {useCallback, useEffect} from 'react';
import {useWebSocket} from './useWebSocket';
import type {
  NotificationMessage,
  PresenceUpdate,
  WebSocketMessage
} from '../services/websocket/WebSocketService';

interface UseWebSocketWithAuthOptions {
  token?: string | null;
  autoConnect?: boolean;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onMessage?: (message: WebSocketMessage) => void;
  onPresenceUpdate?: (presence: PresenceUpdate) => void;
  onNotification?: (notification: NotificationMessage) => void;
}

/**
 * WebSocket hook with authentication integration
 * This hook automatically handles token changes and reconnection
 */
export function useWebSocketWithAuth(options: UseWebSocketWithAuthOptions = {}) {
  const {
    token,
    autoConnect = true,
    onConnect,
    onDisconnect,
    onMessage,
    onPresenceUpdate,
    onNotification
  } = options;

  // Create auth token provider
  const authTokenProvider = useCallback(() => {
    return token || null;
  }, [token]);

  // Use the main WebSocket hook
  const webSocket = useWebSocket({
    autoConnect,
    authTokenProvider,
    onConnect,
    onDisconnect,
    onMessage,
    onPresenceUpdate,
    onNotification
  });

  // Handle token changes - reconnect with new token
  useEffect(() => {
    if (token && webSocket.isConnected) {
      // Only reconnect if we have a new token and are already connected
      webSocket.reconnectWithNewToken();
    } else if (token && !webSocket.isConnected && autoConnect) {
      // Connect if we have a token but aren't connected
      webSocket.connect();
    }
  }, [token, webSocket, autoConnect]);

  return {
    ...webSocket,
    // Additional utility to know if we have auth
    hasAuth: !!token,
    token
  };
}

export default useWebSocketWithAuth;