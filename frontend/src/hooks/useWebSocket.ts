import { useEffect, useState, useCallback, useRef } from 'react';
import webSocketService, { 
  WebSocketMessage, 
  PresenceUpdate, 
  PresenceStatus,
  NotificationMessage 
} from '../services/websocket/WebSocketService';

interface UseWebSocketOptions {
  autoConnect?: boolean;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onMessage?: (message: WebSocketMessage) => void;
  onPresenceUpdate?: (presence: PresenceUpdate) => void;
  onNotification?: (notification: NotificationMessage) => void;
}

export function useWebSocket(options: UseWebSocketOptions = {}) {
  const {
    autoConnect = true,
    onConnect,
    onDisconnect,
    onMessage,
    onPresenceUpdate,
    onNotification
  } = options;

  const [isConnected, setIsConnected] = useState(false);
  const [connectionState, setConnectionState] = useState<string>('DISCONNECTED');
  const [presenceStatus, setPresenceStatus] = useState<PresenceStatus>(PresenceStatus.OFFLINE);
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const subscriptionsRef = useRef<string[]>([]);

  useEffect(() => {
    // Connection status handler
    const handleConnectionChange = (connected: boolean) => {
      setIsConnected(connected);
      setConnectionState(webSocketService.getConnectionState());
      
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
      webSocketService.onMessage('notification', (message: WebSocketMessage<NotificationMessage>) => {
        const notification = message.payload;
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
  }, [autoConnect, onConnect, onDisconnect, onMessage, onPresenceUpdate, onNotification]);

  const connect = useCallback(() => {
    webSocketService.connect();
  }, []);

  const disconnect = useCallback(() => {
    webSocketService.disconnect();
  }, []);

  const sendMessage = useCallback((destination: string, body: any) => {
    webSocketService.sendMessage(destination, body);
  }, []);

  const subscribe = useCallback((destination: string, callback: (message: any) => void) => {
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

  return {
    // Connection state
    isConnected,
    connectionState,
    
    // Connection control
    connect,
    disconnect,
    
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
  }, [relationshipId, ws.isConnected]);

  const sendBuddyRequest = useCallback((toUserId: number, message?: string) => {
    ws.service.sendBuddyRequest(toUserId, message);
  }, []);

  const acceptBuddyRequest = useCallback((relationshipId: number) => {
    ws.service.acceptBuddyRequest(relationshipId);
  }, []);

  const sendCheckin = useCallback((relationshipId: number, checkin: any) => {
    ws.service.sendBuddyCheckin(relationshipId, checkin);
  }, []);

  const updateGoal = useCallback((goal: any) => {
    ws.service.updateBuddyGoal(goal);
  }, []);

  const startSession = useCallback((sessionId: number) => {
    ws.service.startBuddySession(sessionId);
  }, []);

  const endSession = useCallback((sessionId: number) => {
    ws.service.endBuddySession(sessionId);
  }, []);

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
        const data = message.payload as any;
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
  }, [postId, ws.isConnected]);

  const createPost = useCallback((post: any) => {
    ws.service.createForumPost(post);
  }, []);

  const createReply = useCallback((reply: any) => {
    ws.service.createForumReply(reply);
  }, []);

  const voteOnPost = useCallback((postId: number, voteType: number) => {
    ws.service.voteOnPost(postId, voteType);
  }, []);

  const voteOnReply = useCallback((replyId: number, voteType: number) => {
    ws.service.voteOnReply(replyId, voteType);
  }, []);

  const acceptReply = useCallback((replyId: number) => {
    ws.service.acceptReply(replyId);
  }, []);

  const editPost = useCallback((postId: number, post: any) => {
    ws.service.editForumPost(postId, post);
  }, []);

  const setTyping = useCallback((location: string, isTyping: boolean) => {
    ws.service.setTypingStatus(location, isTyping);
  }, []);

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
  }, [hiveId, ws.isConnected]);

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