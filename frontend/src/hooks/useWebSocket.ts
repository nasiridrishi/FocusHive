import { useEffect, useState, useCallback, useRef } from 'react';
import type { IMessage } from '@stomp/stompjs';
import webSocketService, { 
  WebSocketMessage, 
  PresenceUpdate, 
  PresenceStatus,
  NotificationMessage 
} from '../services/websocket/WebSocketService';
import type { BuddyCheckin, BuddyGoal } from '@features/buddy/types';
import type { ForumPost, ForumReply } from '@features/forum/types';

interface UseWebSocketOptions {
  autoConnect?: boolean;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onMessage?: (message: WebSocketMessage) => void;
  onPresenceUpdate?: (presence: PresenceUpdate) => void;
  onNotification?: (notification: NotificationMessage) => void;
  authTokenProvider?: () => string | null;
}

export interface WebSocketConnectionInfo {
  isConnected: boolean;
  connectionState: string;
  reconnectionInfo: {
    attempts: number;
    maxAttempts: number;
    isReconnecting: boolean;
  };
}

export function useWebSocket(options: UseWebSocketOptions = {}) {
  const {
    autoConnect = true,
    onConnect,
    onDisconnect,
    onMessage,
    onPresenceUpdate,
    onNotification,
    authTokenProvider
  } = options;

  const [isConnected, setIsConnected] = useState(false);
  const [connectionState, setConnectionState] = useState<string>('DISCONNECTED');
  const [connectionInfo, setConnectionInfo] = useState<WebSocketConnectionInfo>({
    isConnected: false,
    connectionState: 'DISCONNECTED',
    reconnectionInfo: {
      attempts: 0,
      maxAttempts: 10,
      isReconnecting: false
    }
  });
  const [presenceStatus, setPresenceStatus] = useState<PresenceStatus>(PresenceStatus.OFFLINE);
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const subscriptionsRef = useRef<string[]>([]);

  useEffect(() => {
    // Set up auth token provider if provided
    if (authTokenProvider) {
      webSocketService.setAuthTokenProvider(authTokenProvider);
    }

    // Connection status handler
    const handleConnectionChange = (connected: boolean) => {
      const state = webSocketService.getConnectionState();
      const reconnectionInfo = webSocketService.getReconnectionInfo();
      
      setIsConnected(connected);
      setConnectionState(state);
      setConnectionInfo({
        isConnected: connected,
        connectionState: state,
        reconnectionInfo
      });
      
      if (connected) {
        onConnect?.();
      } else {
        onDisconnect?.();
      }
    };

    webSocketService.onConnectionChange(handleConnectionChange);

    // Message handlers
    if (onMessage) {
      webSocketService.onMessage('general', onMessage);
    }

    if (onPresenceUpdate) {
      webSocketService.onPresenceUpdate(onPresenceUpdate);
    }

    if (onNotification) {
      webSocketService.onMessage('notification', (message: WebSocketMessage) => {
        const notification = message.payload as NotificationMessage;
        setNotifications(prev => [...prev, notification]);
        onNotification(notification);
      });
    }

    // Auto-connect if requested
    if (autoConnect) {
      webSocketService.connect();
    }

    return () => {
      // Cleanup subscriptions
      subscriptionsRef.current.forEach(subId => {
        webSocketService.unsubscribe(subId);
      });
      subscriptionsRef.current = [];
    };
  }, [autoConnect, onConnect, onDisconnect, onMessage, onPresenceUpdate, onNotification, authTokenProvider]);

  const connect = useCallback(() => {
    webSocketService.connect();
  }, []);

  const disconnect = useCallback(() => {
    webSocketService.disconnect();
  }, []);

  const sendMessage = useCallback((destination: string, body: unknown) => {
    webSocketService.sendMessage(destination, body);
  }, []);

  const subscribe = useCallback((destination: string, callback: (message: IMessage) => void) => {
    const subId = webSocketService.subscribe(destination, callback);
    if (subId) {
      subscriptionsRef.current.push(subId);
    }
    return subId;
  }, []);

  const unsubscribe = useCallback((subscriptionId: string) => {
    webSocketService.unsubscribe(subscriptionId);
    subscriptionsRef.current = subscriptionsRef.current.filter(id => id !== subscriptionId);
  }, []);

  const updatePresence = useCallback((status: PresenceStatus, hiveId?: number, activity?: string) => {
    setPresenceStatus(status);
    webSocketService.updatePresenceStatus(status, hiveId, activity);
  }, []);

  const startFocusSession = useCallback((hiveId: number | null, minutes: number) => {
    setPresenceStatus(PresenceStatus.IN_FOCUS_SESSION);
    webSocketService.startFocusSession(hiveId, minutes);
  }, []);

  const clearNotification = useCallback((notificationId: string) => {
    setNotifications(prev => prev.filter(n => n.id !== notificationId));
  }, []);

  const clearAllNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  const retryConnection = useCallback(() => {
    webSocketService.retryConnection();
  }, []);

  const reconnectWithNewToken = useCallback(() => {
    webSocketService.reconnectWithNewToken();
  }, []);

  return {
    // Connection state
    isConnected,
    connectionState,
    connectionInfo,
    
    // Connection control
    connect,
    disconnect,
    retryConnection,
    reconnectWithNewToken,
    
    // Messaging
    sendMessage,
    subscribe,
    unsubscribe,
    
    // Presence
    presenceStatus,
    updatePresence,
    startFocusSession,
    
    // Notifications
    notifications,
    clearNotification,
    clearAllNotifications,
    
    // Direct service access for advanced usage
    service: webSocketService
  };
}

// Hook for buddy system WebSocket features
export function useBuddyWebSocket(relationshipId?: number) {
  const [buddyMessages, setBuddyMessages] = useState<WebSocketMessage[]>([]);
  const [buddyPresence, setBuddyPresence] = useState<PresenceUpdate | null>(null);

  const ws = useWebSocket({
    onMessage: (message) => {
      if (message.type.startsWith('BUDDY_')) {
        setBuddyMessages(prev => [...prev, message]);
      }
    },
    onPresenceUpdate: (presence) => {
      // Update buddy presence if it's relevant
      setBuddyPresence(presence);
    }
  });

  useEffect(() => {
    if (relationshipId && ws.isConnected) {
      const subId = ws.service.subscribeToBuddyUpdates(relationshipId);
      return () => {
        if (subId) ws.unsubscribe(subId);
      };
    }
  }, [relationshipId, ws.isConnected, ws]);

  const sendBuddyRequest = useCallback((toUserId: number, message?: string) => {
    ws.service.sendBuddyRequest(toUserId, message);
  }, [ws.service]);

  const acceptBuddyRequest = useCallback((relationshipId: number) => {
    ws.service.acceptBuddyRequest(relationshipId);
  }, [ws.service]);

  const sendCheckin = useCallback((relationshipId: number, checkin: BuddyCheckin) => {
    ws.service.sendBuddyCheckin(relationshipId, checkin);
  }, [ws.service]);

  const updateGoal = useCallback((goal: BuddyGoal) => {
    ws.service.updateBuddyGoal(goal);
  }, [ws.service]);

  const startSession = useCallback((sessionId: number) => {
    ws.service.startBuddySession(sessionId);
  }, [ws.service]);

  const endSession = useCallback((sessionId: number) => {
    ws.service.endBuddySession(sessionId);
  }, [ws.service]);

  return {
    ...ws,
    buddyMessages,
    buddyPresence,
    sendBuddyRequest,
    acceptBuddyRequest,
    sendCheckin,
    updateGoal,
    startSession,
    endSession
  };
}

// Hook for forum WebSocket features
export function useForumWebSocket(postId?: number) {
  const [forumMessages, setForumMessages] = useState<WebSocketMessage[]>([]);
  const [typingUsers, setTypingUsers] = useState<Map<number, string>>(new Map());

  const ws = useWebSocket({
    onMessage: (message) => {
      if (message.type.startsWith('FORUM_')) {
        setForumMessages(prev => [...prev, message]);
      }
      
      if (message.type === 'USER_TYPING' || message.type === 'USER_STOPPED_TYPING') {
        const data = message.payload as { userId: number; username: string };
        setTypingUsers(prev => {
          const updated = new Map(prev);
          if (message.type === 'USER_TYPING') {
            updated.set(data.userId, data.username);
          } else {
            updated.delete(data.userId);
          }
          return updated;
        });
      }
    }
  });

  useEffect(() => {
    if (postId && ws.isConnected) {
      const subId = ws.service.subscribeToForumPost(postId);
      return () => {
        if (subId) ws.unsubscribe(subId);
      };
    }
  }, [postId, ws.isConnected, ws]);

  const createPost = useCallback((post: Partial<ForumPost>) => {
    ws.service.createForumPost(post);
  }, [ws.service]);

  const createReply = useCallback((reply: Partial<ForumReply>) => {
    ws.service.createForumReply(reply);
  }, [ws.service]);

  const voteOnPost = useCallback((postId: number, voteType: number) => {
    ws.service.voteOnPost(postId, voteType);
  }, [ws.service]);

  const voteOnReply = useCallback((replyId: number, voteType: number) => {
    ws.service.voteOnReply(replyId, voteType);
  }, [ws.service]);

  const acceptReply = useCallback((replyId: number) => {
    ws.service.acceptReply(replyId);
  }, [ws.service]);

  const editPost = useCallback((postId: number, post: Partial<ForumPost>) => {
    ws.service.editForumPost(postId, post);
  }, [ws.service]);

  const setTyping = useCallback((location: string, isTyping: boolean) => {
    ws.service.setTypingStatus(location, isTyping);
  }, [ws.service]);

  return {
    ...ws,
    forumMessages,
    typingUsers,
    createPost,
    createReply,
    voteOnPost,
    voteOnReply,
    acceptReply,
    editPost,
    setTyping
  };
}

// Hook for hive presence WebSocket features
export function useHivePresence(hiveId?: number) {
  const [hivePresence, setHivePresence] = useState<PresenceUpdate[]>([]);
  const [onlineCount, setOnlineCount] = useState(0);

  const ws = useWebSocket({
    onPresenceUpdate: (presence) => {
      if (presence.hiveId === hiveId) {
        setHivePresence(prev => {
          const updated = prev.filter(p => p.userId !== presence.userId);
          if (presence.status !== PresenceStatus.OFFLINE) {
            updated.push(presence);
          }
          setOnlineCount(updated.length);
          return updated;
        });
      }
    }
  });

  useEffect(() => {
    if (hiveId && ws.isConnected) {
      const subId = ws.service.subscribeToHivePresence(hiveId);
      
      // Get initial presence list
      ws.service.getHivePresence(hiveId);
      ws.service.getHiveOnlineCount(hiveId);
      
      return () => {
        if (subId) ws.unsubscribe(subId);
      };
    }
  }, [hiveId, ws.isConnected, ws]);

  const joinHive = useCallback((hiveId: number) => {
    ws.updatePresence(PresenceStatus.ONLINE, hiveId);
  }, [ws]);

  const leaveHive = useCallback(() => {
    ws.updatePresence(PresenceStatus.ONLINE);
  }, [ws]);

  return {
    ...ws,
    hivePresence,
    onlineCount,
    joinHive,
    leaveHive
  };
}

export default useWebSocket;