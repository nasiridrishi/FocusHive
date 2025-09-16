import {Client, IMessage, StompSubscription} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type {BuddyCheckin, BuddyGoal} from '@features/buddy/types';
import type {ForumPost, ForumReply} from '@features/forum/types';
import {getWebSocketConfig} from '../config/environmentConfig';

// WebSocket configuration from validated environment variables
const WEBSOCKET_CONFIG = {
  ...getWebSocketConfig(),
  maxReconnectDelay: 30000, // Max 30 seconds between reconnection attempts
} as const;

export interface WebSocketMessage<T = unknown> {
  id: string;
  type: MessageType;
  event: string;
  payload: T;
  senderId?: string;
  senderUsername?: string;
  timestamp: string;
  metadata?: Record<string, string | number | boolean | null>;
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
  data?: Record<string, string | number | boolean | null>;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  requiresAction?: boolean;
  expiresAt?: string;
  createdAt: string;
}

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private heartbeatInterval: number | null = null;
  private isConnected = false;
  private messageHandlers: Map<string, ((message: WebSocketMessage) => void)[]> = new Map();
  private presenceHandlers: ((presence: PresenceUpdate) => void)[] = [];
  private connectionHandlers: ((connected: boolean) => void)[] = [];
  private authTokenProvider: (() => string | null) | null = null;

  constructor() {
    this.setupClient();
  }

  // Method to set auth token provider (from auth context)
  setAuthTokenProvider(provider: () => string | null): void {
    this.authTokenProvider = provider;
  }

  connect(): void {
    if (this.client && !this.isConnected) {
      this.client.activate();
    }
  }

  // Method to reconnect with new auth token (useful when token refreshes)
  reconnectWithNewToken(): void {
    if (this.isConnected) {
      this.disconnect();
    }
    // Setup client with new token and reconnect
    this.setupClient();
    this.connect();
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

  sendMessage(destination: string, body: unknown): void {
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

  sendBuddyCheckin(relationshipId: number, checkin: BuddyCheckin): void {
    this.sendMessage(`/app/buddy/checkin/${relationshipId}`, checkin);
  }

  updateBuddyGoal(goal: BuddyGoal): void {
    this.sendMessage('/app/buddy/goal/update', goal);
  }

  startBuddySession(sessionId: number): void {
    this.sendMessage(`/app/buddy/session/${sessionId}/start`, {});
  }

  endBuddySession(sessionId: number): void {
    this.sendMessage(`/app/buddy/session/${sessionId}/end`, {});
  }

  // Forum Methods
  createForumPost(post: Partial<ForumPost>): void {
    this.sendMessage('/app/forum/post/create', post);
  }

  createForumReply(reply: Partial<ForumReply>): void {
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

  editForumPost(postId: number, post: Partial<ForumPost>): void {
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

  // Utility methods
  isConnectedStatus(): boolean {
    return this.isConnected;
  }

  getConnectionState(): string {
    if (this.isConnected) return 'CONNECTED';
    if (this.reconnectAttempts > 0 && this.reconnectAttempts < WEBSOCKET_CONFIG.reconnectAttempts) {
      return 'RECONNECTING';
    }
    if (this.reconnectAttempts >= WEBSOCKET_CONFIG.reconnectAttempts) {
      return 'FAILED';
    }
    return 'DISCONNECTED';
  }

  getReconnectionInfo(): { attempts: number; maxAttempts: number; isReconnecting: boolean } {
    return {
      attempts: this.reconnectAttempts,
      maxAttempts: WEBSOCKET_CONFIG.reconnectAttempts,
      isReconnecting: this.reconnectAttempts > 0 && this.reconnectAttempts < WEBSOCKET_CONFIG.reconnectAttempts
    };
  }

  // Method to manually retry connection (after max attempts reached)
  retryConnection(): void {
    this.reconnectAttempts = 0;
    this.setupClient();
    this.connect();
  }

  private setupClient(): void {
    // Get auth token from provider or localStorage
    const getToken = () => {
      return this.authTokenProvider?.() || localStorage.getItem('auth_token');
    };

    // Build WebSocket URL - convert http/https to ws/wss if needed
    const wsUrl = this.buildWebSocketUrl();

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${wsUrl}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${getToken()}`
      },
      debug: (_str) => {
        // Debug logging only in development
        if (import.meta.env.DEV) {
          // STOMP debug messages in development only
        }
      },
      reconnectDelay: WEBSOCKET_CONFIG.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: (_frame) => {
        // WebSocket connected - handled by connection handlers
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.setupSubscriptions();
        this.startHeartbeat();
        this.notifyConnectionHandlers(true);
      },

      onDisconnect: (_frame) => {
        // WebSocket disconnected - handled by connection handlers
        this.isConnected = false;
        this.stopHeartbeat();
        this.notifyConnectionHandlers(false);
      },

      onStompError: (_frame) => {
        // WebSocket STOMP error handled by reconnection logic
        this.handleReconnection();
      },

      onWebSocketClose: (_event) => {
        // WebSocket closed - handled by reconnection logic
        this.handleReconnection();
      },

      onWebSocketError: (_event) => {
        // WebSocket error handled by reconnection logic
        this.handleReconnection();
      }
    });
  }

  private buildWebSocketUrl(): string {
    let wsUrl = WEBSOCKET_CONFIG.url;

    // Auto-detect protocol based on page protocol if not specified
    if (wsUrl.startsWith('//')) {
      wsUrl = window.location.protocol === 'https:' ? `wss:${wsUrl}` : `ws:${wsUrl}`;
    } else if (wsUrl.startsWith('http://')) {
      wsUrl = wsUrl.replace('http://', 'ws://');
    } else if (wsUrl.startsWith('https://')) {
      wsUrl = wsUrl.replace('https://', 'wss://');
    }

    return wsUrl;
  }

  private handleReconnection(): void {
    if (this.reconnectAttempts < WEBSOCKET_CONFIG.reconnectAttempts) {
      this.reconnectAttempts++;

      // Exponential backoff with jitter
      const baseDelay = WEBSOCKET_CONFIG.reconnectDelay;
      const exponentialDelay = Math.min(
          baseDelay * Math.pow(2, this.reconnectAttempts - 1),
          WEBSOCKET_CONFIG.maxReconnectDelay
      );
      // Add jitter (±25%)
      const jitter = exponentialDelay * 0.25 * (Math.random() * 2 - 1);
      const delay = exponentialDelay + jitter;

      // Reconnection attempt scheduled

      setTimeout(() => {
        if (!this.isConnected) {
          this.connect();
        }
      }, delay);
    } else {
      // Maximum reconnection attempts reached - user notification required
      this.notifyConnectionHandlers(false);
    }
  }

  private setupSubscriptions(): void {
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

  private startHeartbeat(): void {
    this.heartbeatInterval = window.setInterval(() => {
      if (this.isConnected) {
        this.sendMessage('/app/presence/heartbeat', {});
      }
    }, WEBSOCKET_CONFIG.heartbeatInterval);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
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
}

// Export singleton instance
export const webSocketService = new WebSocketService();
export default webSocketService;