import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketMessage<T = any> {
  id: string;
  type: MessageType;
  event: string;
  payload: T;
  senderId?: string;
  senderUsername?: string;
  timestamp: string;
  metadata?: Record<string, any>;
}

export enum MessageType {
  // Buddy System Events
  BUDDY_REQUEST = 'BUDDY_REQUEST',
  BUDDY_REQUEST_ACCEPTED = 'BUDDY_REQUEST_ACCEPTED',
  BUDDY_REQUEST_REJECTED = 'BUDDY_REQUEST_REJECTED',
  BUDDY_CHECKIN = 'BUDDY_CHECKIN',
  BUDDY_GOAL_UPDATE = 'BUDDY_GOAL_UPDATE',
  BUDDY_SESSION_START = 'BUDDY_SESSION_START',
  BUDDY_SESSION_END = 'BUDDY_SESSION_END',
  BUDDY_SESSION_REMINDER = 'BUDDY_SESSION_REMINDER',
  
  // Forum Events
  FORUM_NEW_POST = 'FORUM_NEW_POST',
  FORUM_NEW_REPLY = 'FORUM_NEW_REPLY',
  FORUM_POST_VOTED = 'FORUM_POST_VOTED',
  FORUM_REPLY_VOTED = 'FORUM_REPLY_VOTED',
  FORUM_REPLY_ACCEPTED = 'FORUM_REPLY_ACCEPTED',
  FORUM_MENTION = 'FORUM_MENTION',
  FORUM_POST_EDITED = 'FORUM_POST_EDITED',
  FORUM_POST_DELETED = 'FORUM_POST_DELETED',
  
  // Presence Events
  USER_ONLINE = 'USER_ONLINE',
  USER_OFFLINE = 'USER_OFFLINE',
  USER_AWAY = 'USER_AWAY',
  USER_TYPING = 'USER_TYPING',
  USER_STOPPED_TYPING = 'USER_STOPPED_TYPING',
  
  // Hive Events
  HIVE_USER_JOINED = 'HIVE_USER_JOINED',
  HIVE_USER_LEFT = 'HIVE_USER_LEFT',
  HIVE_ANNOUNCEMENT = 'HIVE_ANNOUNCEMENT',
  
  // System Events
  NOTIFICATION = 'NOTIFICATION',
  ERROR = 'ERROR',
  SUCCESS = 'SUCCESS',
  WARNING = 'WARNING',
  INFO = 'INFO',
  PING = 'PING',
  PONG = 'PONG'
}

export enum PresenceStatus {
  ONLINE = 'ONLINE',
  AWAY = 'AWAY',
  BUSY = 'BUSY',
  IN_FOCUS_SESSION = 'IN_FOCUS_SESSION',
  IN_BUDDY_SESSION = 'IN_BUDDY_SESSION',
  DO_NOT_DISTURB = 'DO_NOT_DISTURB',
  OFFLINE = 'OFFLINE'
}

export interface PresenceUpdate {
  userId: number;
  username?: string;
  avatar?: string;
  status: PresenceStatus;
  statusMessage?: string;
  hiveId?: number;
  currentActivity?: string;
  lastSeen: string;
  focusMinutesRemaining?: number;
}

export interface NotificationMessage {
  id: string;
  type: string;
  title: string;
  message: string;
  actionUrl?: string;
  data?: Record<string, any>;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  requiresAction?: boolean;
  expiresAt?: string;
  createdAt: string;
}

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private heartbeatInterval: NodeJS.Timer | null = null;
  private isConnected = false;
  private messageHandlers: Map<string, ((message: WebSocketMessage) => void)[]> = new Map();
  private presenceHandlers: ((presence: PresenceUpdate) => void)[] = [];
  private connectionHandlers: ((connected: boolean) => void)[] = [];

  constructor() {
    this.setupClient();
  }

  private setupClient() {
    const token = localStorage.getItem('auth_token');
    
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: () => {
        // Debug logging disabled in production
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: () => {
        // WebSocket connected
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.setupSubscriptions();
        this.startHeartbeat();
        this.notifyConnectionHandlers(true);
      },
      
      onDisconnect: () => {
        // WebSocket disconnected
        this.isConnected = false;
        this.stopHeartbeat();
        this.notifyConnectionHandlers(false);
      },
      
      onStompError: (frame) => {
        // Handle WebSocket errors
        // Error details: frame.headers['message'] and frame.body
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          setTimeout(() => this.connect(), this.reconnectDelay * this.reconnectAttempts);
        }
      }
    });
  }

  connect(): void {
    if (this.client && !this.isConnected) {
      this.client.activate();
    }
  }

  disconnect(): void {
    this.stopHeartbeat();
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions.clear();
    
    if (this.client) {
      this.client.deactivate();
    }
    
    this.isConnected = false;
  }

  private setupSubscriptions() {
    // Subscribe to user-specific notifications
    this.subscribe('/user/queue/notifications', (message) => {
      this.handleNotification(message);
    });

    // Subscribe to presence updates
    this.subscribe('/topic/presence', (message) => {
      this.handlePresenceUpdate(message);
    });

    // Subscribe to system announcements
    this.subscribe('/topic/system/announcements', (message) => {
      this.handleSystemAnnouncement(message);
    });
  }

  private startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected) {
        this.sendMessage('/app/presence/heartbeat', {});
      }
    }, 30000); // Send heartbeat every 30 seconds
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  subscribe(destination: string, callback: (message: IMessage) => void): string {
    if (!this.client || !this.isConnected) {
      // Not connected, cannot subscribe
      return '';
    }

    const subscription = this.client.subscribe(destination, callback);
    const subId = subscription.id;
    this.subscriptions.set(subId, subscription);
    
    return subId;
  }

  unsubscribe(subscriptionId: string): void {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
    }
  }

  sendMessage(destination: string, body: any): void {
    if (!this.client || !this.isConnected) {
      // Not connected, cannot send message
      return;
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });
  }

  // Buddy System Methods
  sendBuddyRequest(toUserId: number, message?: string): void {
    this.sendMessage('/app/buddy/request', {
      toUserId,
      message
    });
  }

  acceptBuddyRequest(relationshipId: number): void {
    this.sendMessage(`/app/buddy/accept/${relationshipId}`, {});
  }

  sendBuddyCheckin(relationshipId: number, checkin: any): void {
    this.sendMessage(`/app/buddy/checkin/${relationshipId}`, checkin);
  }

  updateBuddyGoal(goal: any): void {
    this.sendMessage('/app/buddy/goal/update', goal);
  }

  startBuddySession(sessionId: number): void {
    this.sendMessage(`/app/buddy/session/${sessionId}/start`, {});
  }

  endBuddySession(sessionId: number): void {
    this.sendMessage(`/app/buddy/session/${sessionId}/end`, {});
  }

  // Forum Methods
  createForumPost(post: any): void {
    this.sendMessage('/app/forum/post/create', post);
  }

  createForumReply(reply: any): void {
    this.sendMessage('/app/forum/reply/create', reply);
  }

  voteOnPost(postId: number, voteType: number): void {
    this.sendMessage('/app/forum/vote', {
      postId,
      voteType
    });
  }

  voteOnReply(replyId: number, voteType: number): void {
    this.sendMessage('/app/forum/vote', {
      replyId,
      voteType
    });
  }

  acceptReply(replyId: number): void {
    this.sendMessage(`/app/forum/reply/${replyId}/accept`, {});
  }

  editForumPost(postId: number, post: any): void {
    this.sendMessage(`/app/forum/post/${postId}/edit`, post);
  }

  setTypingStatus(location: string, isTyping: boolean): void {
    this.sendMessage('/app/presence/typing', {
      location,
      isTyping
    });
  }

  // Presence Methods
  updatePresenceStatus(status: PresenceStatus, hiveId?: number, activity?: string): void {
    this.sendMessage('/app/presence/status', {
      status,
      hiveId,
      activity
    });
  }

  startFocusSession(hiveId: number | null, minutes: number): void {
    this.sendMessage('/app/presence/focus/start', {
      hiveId,
      minutes
    });
  }

  startBuddySessionPresence(buddyId: number): void {
    this.sendMessage('/app/presence/buddy/start', {
      buddyId
    });
  }

  getUserPresence(userId: number): void {
    this.sendMessage(`/app/presence/user/${userId}`, {});
  }

  getHivePresence(hiveId: number): void {
    this.sendMessage(`/app/presence/hive/${hiveId}`, {});
  }

  getHiveOnlineCount(hiveId: number): void {
    this.sendMessage(`/app/presence/hive/${hiveId}/count`, {});
  }

  // Subscribe to specific topics
  subscribeToBuddyUpdates(relationshipId: number): string {
    return this.subscribe(`/topic/buddy/sessions/${relationshipId}`, (message) => {
      const wsMessage: WebSocketMessage = JSON.parse(message.body);
      this.notifyHandlers('buddy', wsMessage);
    });
  }

  subscribeToForumPost(postId: number): string {
    return this.subscribe(`/topic/forum/post/${postId}/updates`, (message) => {
      const wsMessage: WebSocketMessage = JSON.parse(message.body);
      this.notifyHandlers('forum', wsMessage);
    });
  }

  subscribeToHivePresence(hiveId: number): string {
    return this.subscribe(`/topic/hive/${hiveId}/presence`, (message) => {
      const wsMessage: WebSocketMessage = JSON.parse(message.body);
      this.notifyHandlers('hive', wsMessage);
    });
  }

  // Handler registration
  onMessage(type: string, handler: (message: WebSocketMessage) => void): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, []);
    }
    this.messageHandlers.get(type)?.push(handler);
  }

  onPresenceUpdate(handler: (presence: PresenceUpdate) => void): void {
    this.presenceHandlers.push(handler);
  }

  onConnectionChange(handler: (connected: boolean) => void): void {
    this.connectionHandlers.push(handler);
    // Immediately notify with current status
    handler(this.isConnected);
  }

  // Private handler methods
  private handleNotification(message: IMessage): void {
    const wsMessage: WebSocketMessage<NotificationMessage> = JSON.parse(message.body);
    this.notifyHandlers('notification', wsMessage);
  }

  private handlePresenceUpdate(message: IMessage): void {
    const wsMessage: WebSocketMessage<PresenceUpdate> = JSON.parse(message.body);
    const presence = wsMessage.payload;
    
    this.presenceHandlers.forEach(handler => handler(presence));
    this.notifyHandlers('presence', wsMessage);
  }

  private handleSystemAnnouncement(message: IMessage): void {
    const wsMessage: WebSocketMessage = JSON.parse(message.body);
    this.notifyHandlers('system', wsMessage);
  }

  private notifyHandlers(type: string, message: WebSocketMessage): void {
    const handlers = this.messageHandlers.get(type) || [];
    handlers.forEach(handler => handler(message));
  }

  private notifyConnectionHandlers(connected: boolean): void {
    this.connectionHandlers.forEach(handler => handler(connected));
  }

  // Utility methods
  isConnectedStatus(): boolean {
    return this.isConnected;
  }

  getConnectionState(): string {
    if (this.isConnected) return 'CONNECTED';
    if (this.reconnectAttempts > 0) return 'RECONNECTING';
    return 'DISCONNECTED';
  }
}

// Export singleton instance
export const webSocketService = new WebSocketService();
export default webSocketService;