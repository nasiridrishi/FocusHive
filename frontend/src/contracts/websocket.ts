/**
 * WebSocket Contracts
 * Core interfaces for real-time communication in FocusHive
 *
 * This module defines all WebSocket-related types used across the application.
 * All interfaces are designed to work with STOMP over WebSocket protocol.
 * Backend WebSocket endpoint: ws://localhost:8080/ws
 */

/**
 * WebSocket message types for different events
 */
export type WebSocketMessageType =
  | 'PRESENCE_UPDATE'
  | 'CHAT_MESSAGE'
  | 'HIVE_BROADCAST'
  | 'TIMER_SYNC'
  | 'NOTIFICATION'
  | 'BUDDY_EVENT'
  | 'FORUM_UPDATE'
  | 'ANALYTICS_EVENT'
  | 'SYSTEM_ALERT'
  | 'CONNECTION_ACK'
  | 'HEARTBEAT'
  | 'ERROR';

/**
 * WebSocket connection states
 */
export type WebSocketConnectionState =
  | 'CONNECTING'
  | 'CONNECTED'
  | 'DISCONNECTED'
  | 'RECONNECTING'
  | 'ERROR'
  | 'CLOSED';

/**
 * Channel types in STOMP protocol
 */
export type WebSocketChannelType =
  | 'TOPIC'      // Broadcast to all subscribers
  | 'QUEUE'      // Point-to-point messaging
  | 'USER_QUEUE' // User-specific queue
  | 'TEMP_QUEUE' // Temporary queue
  | 'APP';       // Application-level channel

/**
 * WebSocket event types for connection lifecycle
 */
export type WebSocketEventType =
  | 'CONNECTING'
  | 'CONNECTED'
  | 'DISCONNECTED'
  | 'RECONNECTING'
  | 'RECONNECTED'
  | 'ERROR'
  | 'MESSAGE_RECEIVED'
  | 'MESSAGE_SENT'
  | 'SUBSCRIPTION_ADDED'
  | 'SUBSCRIPTION_REMOVED'
  | 'HEARTBEAT_SENT'
  | 'HEARTBEAT_RECEIVED';

/**
 * WebSocket error codes
 */
export type WebSocketErrorCode =
  | 'CONNECTION_FAILED'
  | 'CONNECTION_LOST'
  | 'AUTHENTICATION_FAILED'
  | 'AUTHORIZATION_FAILED'
  | 'SUBSCRIPTION_FAILED'
  | 'MESSAGE_SEND_FAILED'
  | 'HEARTBEAT_TIMEOUT'
  | 'INVALID_MESSAGE'
  | 'RATE_LIMIT_EXCEEDED'
  | 'SERVER_ERROR'
  | 'CLIENT_ERROR'
  | 'UNKNOWN_ERROR';

/**
 * Presence status types
 */
export type PresenceStatus =
  | 'online'
  | 'away'
  | 'busy'
  | 'offline'
  | 'invisible';

/**
 * Generic WebSocket message structure
 */
export interface WebSocketMessage<T = any> {
  id: string;
  type: WebSocketMessageType;
  channel: string;
  payload: T;
  timestamp: string;
  senderId?: string;
  senderName?: string;
  correlationId?: string;
  metadata?: Record<string, any>;
}

/**
 * WebSocket configuration
 */
export interface WebSocketConfig {
  url: string;
  reconnect: boolean;
  reconnectInterval: number;
  reconnectMaxRetries?: number;
  heartbeatInterval: number;
  connectionTimeout: number;
  debug: boolean;
  protocols?: string[];
  headers?: Record<string, string>;
  stomp?: StompConfig;
}

/**
 * STOMP-specific configuration
 */
export interface StompConfig {
  brokerURL: string;
  connectHeaders?: StompHeaders;
  debug?: boolean;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  reconnectDelay?: number;
  stompVersion?: string;
  webSocketFactory?: () => WebSocket;
  beforeConnect?: () => void;
  onConnect?: (frame: any) => void;
  onDisconnect?: (frame: any) => void;
  onStompError?: (frame: any) => void;
  onWebSocketError?: (event: any) => void;
  onWebSocketClose?: (event: any) => void;
  onUnhandledMessage?: (message: any) => void;
  onChangeState?: (state: number) => void;
}

/**
 * STOMP headers
 */
export interface StompHeaders {
  [key: string]: string | number | boolean;
}

/**
 * WebSocket subscription
 */
export interface WebSocketSubscription {
  id: string;
  channel: string;
  channelType: WebSocketChannelType;
  callback: (message: WebSocketMessage) => void;
  active: boolean;
  createdAt: number;
  messageCount: number;
  filters?: Record<string, any>;
  errorCallback?: (error: WebSocketError) => void;
  headers?: StompHeaders;
}

/**
 * WebSocket channel definition
 */
export interface WebSocketChannel {
  name: string;
  type: WebSocketChannelType;
  description?: string;
  permissions?: string[];
  subscribers?: number;
  isPrivate?: boolean;
  metadata?: Record<string, any>;
}

/**
 * WebSocket event
 */
export interface WebSocketEvent {
  type: WebSocketEventType;
  timestamp: string;
  data?: any;
}

/**
 * WebSocket error
 */
export interface WebSocketError {
  code: WebSocketErrorCode;
  message: string;
  timestamp: string;
  details?: Record<string, any>;
  recoverable?: boolean;
  retry?: boolean;
  retryAfter?: number;
}

/**
 * WebSocket heartbeat
 */
export interface WebSocketHeartbeat {
  timestamp: number;
  sequence: number;
  latency?: number;
  serverTime?: string;
}

/**
 * Presence update message
 */
export interface PresenceUpdate {
  userId: string;
  hiveId?: string;
  status: PresenceStatus;
  lastActivity: string;
  activity?: string;
  deviceInfo?: {
    deviceId: string;
    deviceType: string;
    browser?: string;
    os?: string;
  };
  location?: {
    country?: string;
    city?: string;
    timezone?: string;
  };
}

/**
 * Chat message structure
 */
export interface ChatMessage {
  id: string;
  hiveId: string;
  senderId: string;
  senderName: string;
  text: string;
  timestamp: string;
  edited?: boolean;
  deleted?: boolean;
  reactions?: Array<{
    emoji: string;
    userId: string;
    userName: string;
  }>;
  mentions?: string[];
  attachments?: Array<{
    id: string;
    name: string;
    type: string;
    size: number;
    url: string;
  }>;
  threadId?: string;
  replyToId?: string;
}

/**
 * Hive broadcast message
 */
export interface HiveBroadcast {
  hiveId: string;
  type: string; // 'USER_JOINED', 'USER_LEFT', 'HIVE_UPDATED', etc.
  userId?: string;
  userName?: string;
  message: string;
  timestamp: string;
  data?: Record<string, any>;
}

/**
 * Timer sync message
 */
export interface TimerSync {
  userId: string;
  hiveId?: string;
  timerId: string;
  action: string; // 'START', 'PAUSE', 'RESUME', 'STOP', 'SYNC'
  duration: number;
  elapsedTime: number;
  remainingTime: number;
  isPaused: boolean;
  timestamp: string;
  syncedDevices?: string[];
}

/**
 * Notification payload
 */
export interface NotificationPayload {
  id: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  priority?: string;
  timestamp: string;
  read: boolean;
  actionUrl?: string;
  imageUrl?: string;
  data?: Record<string, any>;
}

/**
 * WebSocket metrics for monitoring
 */
export interface WebSocketMetrics {
  connectionTime: number;
  messagesReceived: number;
  messagesSent: number;
  bytesReceived: number;
  bytesSent: number;
  reconnectCount: number;
  errorCount: number;
  latency?: number;
  uptime: number;
  subscriptions: number;
}

/**
 * Connection information
 */
export interface ConnectionInfo {
  url: string;
  state: WebSocketConnectionState;
  sessionId?: string;
  connectedAt?: number;
  lastMessageAt?: number;
  lastHeartbeatAt?: number;
  protocol?: string;
  version?: string;
  serverInfo?: Record<string, any>;
}

/**
 * Reconnection strategy configuration
 */
export interface ReconnectionStrategy {
  enabled: boolean;
  maxRetries: number;
  baseDelay: number;
  maxDelay: number;
  backoffMultiplier: number;
  jitter?: boolean;
  retryOn?: string[];
}

/**
 * WebSocket service interface
 */
export interface WebSocketService {
  // Connection management
  connect(config: WebSocketConfig): Promise<void>;
  disconnect(): Promise<void>;
  isConnected(): boolean;
  getConnectionInfo(): ConnectionInfo;

  // Subscription management
  subscribe(channel: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription;
  unsubscribe(subscriptionId: string): void;
  getSubscriptions(): WebSocketSubscription[];

  // Message sending
  send(channel: string, message: any): void;
  sendWithAck(channel: string, message: any): Promise<void>;

  // Event handling
  on(event: WebSocketEventType, callback: (data: any) => void): void;
  off(event: WebSocketEventType, callback?: (data: any) => void): void;

  // Heartbeat and health
  sendHeartbeat(): void;
  getMetrics(): WebSocketMetrics;
  getLatency(): number;

  // Error handling
  onError(callback: (error: WebSocketError) => void): void;
  clearErrors(): void;
}

/**
 * WebSocket context state
 */
export interface WebSocketContextState {
  service: WebSocketService | null;
  connectionState: WebSocketConnectionState;
  connectionInfo: ConnectionInfo | null;
  subscriptions: WebSocketSubscription[];
  metrics: WebSocketMetrics | null;
  error: WebSocketError | null;
  isReconnecting: boolean;
  reconnectAttempts: number;
}

/**
 * WebSocket context methods
 */
export interface WebSocketContextMethods {
  connect(): Promise<void>;
  disconnect(): Promise<void>;
  subscribe(channel: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription | null;
  unsubscribe(subscriptionId: string): void;
  send(channel: string, message: any): void;
  sendPresenceUpdate(status: PresenceStatus): void;
  sendChatMessage(hiveId: string, text: string): void;
  sendTimerSync(action: string, timerId: string): void;
  getConnectionInfo(): ConnectionInfo | null;
  getMetrics(): WebSocketMetrics | null;
  clearError(): void;
}

/**
 * Complete WebSocket context type
 */
export interface WebSocketContextType extends WebSocketContextState, WebSocketContextMethods {}

/**
 * Channel definitions for FocusHive
 */
export const WEBSOCKET_CHANNELS = {
  // Hive channels
  HIVE_TOPIC: (hiveId: string) => `/topic/hive/${hiveId}`,
  HIVE_PRESENCE: (hiveId: string) => `/topic/presence/${hiveId}`,
  HIVE_CHAT: (hiveId: string) => `/topic/chat/${hiveId}`,

  // User-specific channels
  USER_TIMER: (userId: string) => `/queue/timer/${userId}`,
  USER_NOTIFICATIONS: '/user/queue/notifications',
  USER_BUDDY: '/user/queue/buddy',

  // System channels
  SYSTEM_ALERTS: '/topic/system/alerts',
  SYSTEM_MAINTENANCE: '/topic/system/maintenance',

  // Forum channels
  FORUM_POST: (postId: string) => `/topic/forum/post/${postId}`,
  FORUM_CATEGORY: (categoryId: string) => `/topic/forum/category/${categoryId}`,

  // Analytics channels
  ANALYTICS_EVENTS: '/topic/analytics/events',
  ANALYTICS_METRICS: '/topic/analytics/metrics',
} as const;

/**
 * Message type guards
 */
export const isPresenceUpdate = (message: WebSocketMessage): message is WebSocketMessage<PresenceUpdate> => {
  return message.type === 'PRESENCE_UPDATE';
};

export const isChatMessage = (message: WebSocketMessage): message is WebSocketMessage<ChatMessage> => {
  return message.type === 'CHAT_MESSAGE';
};

export const isHiveBroadcast = (message: WebSocketMessage): message is WebSocketMessage<HiveBroadcast> => {
  return message.type === 'HIVE_BROADCAST';
};

export const isTimerSync = (message: WebSocketMessage): message is WebSocketMessage<TimerSync> => {
  return message.type === 'TIMER_SYNC';
};

export const isNotification = (message: WebSocketMessage): message is WebSocketMessage<NotificationPayload> => {
  return message.type === 'NOTIFICATION';
};

export const isError = (message: WebSocketMessage): message is WebSocketMessage<WebSocketError> => {
  return message.type === 'ERROR';
};