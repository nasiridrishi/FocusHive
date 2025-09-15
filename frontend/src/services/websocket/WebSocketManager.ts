/**
 * WebSocket Manager with STOMP Support
 *
 * Handles real-time communication for:
 * - Presence updates
 * - Chat messages
 * - Timer sessions
 * - Notifications
 * - Hive updates
 */

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { TokenManager } from '@/services/api/apiClient';
import {
  getWebSocketUrls,
  WS_EVENTS,
  STOMP_DESTINATIONS,
  SECURITY_CONFIG
} from '@/services/config/services.config';

interface WebSocketConfig {
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  debug?: boolean;
}

interface SubscriptionCallback {
  (message: any): void;
}

interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp: string;
  userId?: string;
  hiveId?: string;
}

/**
 * WebSocket Manager Class
 * Singleton pattern for managing WebSocket connections
 */
export class WebSocketManager {
  private static instance: WebSocketManager | null = null;
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private eventHandlers: Map<string, Set<SubscriptionCallback>> = new Map();
  private tokenManager: TokenManager;
  private config: WebSocketConfig;
  private isConnected: boolean = false;
  private currentHiveId: string | null = null;
  private userId: string | null = null;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 10;

  private constructor(config: WebSocketConfig = {}) {
    this.tokenManager = TokenManager.getInstance();
    this.config = {
      reconnectDelay: config.reconnectDelay || 5000,
      heartbeatIncoming: config.heartbeatIncoming || 4000,
      heartbeatOutgoing: config.heartbeatOutgoing || 4000,
      debug: config.debug || false,
    };
    this.userId = localStorage.getItem(SECURITY_CONFIG.USER_ID_KEY);
  }

  /**
   * Get singleton instance
   */
  static getInstance(config?: WebSocketConfig): WebSocketManager {
    if (!WebSocketManager.instance) {
      WebSocketManager.instance = new WebSocketManager(config);
    }
    return WebSocketManager.instance;
  }

  /**
   * Connect to WebSocket server
   */
  async connect(): Promise<void> {
    if (this.isConnected) {
      console.log('WebSocket already connected');
      return;
    }

    const token = await this.tokenManager.getValidToken();
    if (!token) {
      throw new Error('No authentication token available');
    }

    const wsUrls = getWebSocketUrls();

    this.client = new Client({
      webSocketFactory: () => {
        // Use SockJS for fallback support
        return new SockJS(wsUrls.BACKEND.replace('ws://', 'http://').replace('wss://', 'https://'));
      },
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: this.config.debug ? (str) => console.log('STOMP:', str) : undefined,
      reconnectDelay: this.config.reconnectDelay!,
      heartbeatIncoming: this.config.heartbeatIncoming!,
      heartbeatOutgoing: this.config.heartbeatOutgoing!,
      onConnect: this.handleConnect.bind(this),
      onStompError: this.handleError.bind(this),
      onWebSocketClose: this.handleDisconnect.bind(this),
      onWebSocketError: this.handleWebSocketError.bind(this),
    });

    this.client.activate();
  }

  /**
   * Handle successful connection
   */
  private handleConnect(): void {
    console.log('WebSocket connected successfully');
    this.isConnected = true;
    this.reconnectAttempts = 0;

    // Setup default subscriptions
    this.setupDefaultSubscriptions();

    // Emit connect event
    this.emitEvent(WS_EVENTS.CONNECT, { connected: true });

    // Rejoin current hive if any
    if (this.currentHiveId) {
      this.subscribeToHive(this.currentHiveId);
    }
  }

  /**
   * Handle connection error
   */
  private handleError(frame: any): void {
    console.error('STOMP error:', frame.headers['message']);
    this.emitEvent(WS_EVENTS.ERROR, {
      error: frame.headers['message'],
      details: frame.body
    });
  }

  /**
   * Handle disconnection
   */
  private handleDisconnect(): void {
    console.log('WebSocket disconnected');
    this.isConnected = false;
    this.emitEvent(WS_EVENTS.DISCONNECT, { connected: false });

    // Attempt reconnection if not at max attempts
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(30000, this.config.reconnectDelay! * this.reconnectAttempts);
      console.log(`Attempting reconnection ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${delay}ms`);
      setTimeout(() => this.connect(), delay);
    }
  }

  /**
   * Handle WebSocket error
   */
  private handleWebSocketError(event: Event): void {
    console.error('WebSocket error:', event);
    this.emitEvent(WS_EVENTS.ERROR, { error: 'WebSocket connection error', event });
  }

  /**
   * Setup default subscriptions
   */
  private setupDefaultSubscriptions(): void {
    if (!this.client || !this.isConnected) return;

    // Personal notifications
    this.subscribe(
      STOMP_DESTINATIONS.SUBSCRIBE.USER_NOTIFICATIONS,
      (message) => {
        const notification = JSON.parse(message.body);
        this.emitEvent(WS_EVENTS.NOTIFICATION_RECEIVED, notification);
      }
    );

    // Personal messages
    this.subscribe(
      STOMP_DESTINATIONS.SUBSCRIBE.USER_MESSAGES,
      (message) => {
        const chatMessage = JSON.parse(message.body);
        this.emitEvent(WS_EVENTS.MESSAGE_RECEIVED, chatMessage);
      }
    );

    // Error messages
    this.subscribe(
      STOMP_DESTINATIONS.SUBSCRIBE.USER_ERRORS,
      (message) => {
        const error = JSON.parse(message.body);
        console.error('Server error:', error);
        this.emitEvent(WS_EVENTS.ERROR, error);
      }
    );
  }

  /**
   * Subscribe to a destination
   */
  private subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription | null {
    if (!this.client || !this.isConnected) {
      console.warn('Cannot subscribe - WebSocket not connected');
      return null;
    }

    // Check if already subscribed
    if (this.subscriptions.has(destination)) {
      console.log(`Already subscribed to ${destination}`);
      return this.subscriptions.get(destination)!;
    }

    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);
    console.log(`Subscribed to ${destination}`);

    return subscription;
  }

  /**
   * Unsubscribe from a destination
   */
  private unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log(`Unsubscribed from ${destination}`);
    }
  }

  /**
   * Subscribe to hive-specific channels
   */
  subscribeToHive(hiveId: string): void {
    this.currentHiveId = hiveId;

    // Presence updates
    const presenceDestination = STOMP_DESTINATIONS.SUBSCRIBE.HIVE_PRESENCE.replace(':hiveId', hiveId);
    this.subscribe(presenceDestination, (message) => {
      const presence = JSON.parse(message.body);
      this.emitEvent(WS_EVENTS.PRESENCE_UPDATE, { ...presence, hiveId });
    });

    // Hive updates
    const hiveDestination = STOMP_DESTINATIONS.SUBSCRIBE.HIVE_UPDATES.replace(':hiveId', hiveId);
    this.subscribe(hiveDestination, (message) => {
      const update = JSON.parse(message.body);
      this.emitEvent(WS_EVENTS.HIVE_UPDATE, { ...update, hiveId });
    });

    // Chat messages
    const chatDestination = STOMP_DESTINATIONS.SUBSCRIBE.HIVE_CHAT.replace(':hiveId', hiveId);
    this.subscribe(chatDestination, (message) => {
      const chatMessage = JSON.parse(message.body);
      this.emitEvent(WS_EVENTS.MESSAGE_RECEIVED, { ...chatMessage, hiveId });
    });

    // Timer updates
    const timerDestination = STOMP_DESTINATIONS.SUBSCRIBE.HIVE_TIMER.replace(':hiveId', hiveId);
    this.subscribe(timerDestination, (message) => {
      const timerUpdate = JSON.parse(message.body);
      this.handleTimerUpdate(timerUpdate);
    });

    console.log(`Subscribed to hive ${hiveId}`);
  }

  /**
   * Unsubscribe from hive channels
   */
  unsubscribeFromHive(hiveId: string): void {
    const destinations = [
      STOMP_DESTINATIONS.SUBSCRIBE.HIVE_PRESENCE.replace(':hiveId', hiveId),
      STOMP_DESTINATIONS.SUBSCRIBE.HIVE_UPDATES.replace(':hiveId', hiveId),
      STOMP_DESTINATIONS.SUBSCRIBE.HIVE_CHAT.replace(':hiveId', hiveId),
      STOMP_DESTINATIONS.SUBSCRIBE.HIVE_TIMER.replace(':hiveId', hiveId),
    ];

    destinations.forEach(dest => this.unsubscribe(dest));

    if (this.currentHiveId === hiveId) {
      this.currentHiveId = null;
    }

    console.log(`Unsubscribed from hive ${hiveId}`);
  }

  /**
   * Handle timer updates
   */
  private handleTimerUpdate(update: any): void {
    const eventMap: Record<string, string> = {
      'SESSION_STARTED': WS_EVENTS.SESSION_STARTED,
      'SESSION_ENDED': WS_EVENTS.SESSION_ENDED,
      'SESSION_PAUSED': WS_EVENTS.SESSION_PAUSED,
      'SESSION_RESUMED': WS_EVENTS.SESSION_RESUMED,
    };

    const eventType = eventMap[update.type] || WS_EVENTS.HIVE_UPDATE;
    this.emitEvent(eventType, update);
  }

  /**
   * Send message to a destination
   */
  send(destination: string, body: any): void {
    if (!this.client || !this.isConnected) {
      console.error('Cannot send message - WebSocket not connected');
      throw new Error('WebSocket not connected');
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  /**
   * Send chat message
   */
  sendChatMessage(hiveId: string, message: string): void {
    this.send(STOMP_DESTINATIONS.SEND.CHAT_MESSAGE, {
      hiveId,
      content: message,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Update presence
   */
  updatePresence(hiveId: string, status: 'online' | 'away' | 'busy' | 'offline'): void {
    this.send(STOMP_DESTINATIONS.SEND.PRESENCE_UPDATE, {
      hiveId,
      status,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Send heartbeat
   */
  sendHeartbeat(): void {
    this.send(STOMP_DESTINATIONS.SEND.HEARTBEAT, {
      timestamp: new Date().toISOString(),
      userId: this.userId,
    });
  }

  /**
   * Register event handler
   */
  on(event: string, callback: SubscriptionCallback): void {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, new Set());
    }
    this.eventHandlers.get(event)!.add(callback);
  }

  /**
   * Unregister event handler
   */
  off(event: string, callback: SubscriptionCallback): void {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      handlers.delete(callback);
    }
  }

  /**
   * Emit event to registered handlers
   */
  private emitEvent(event: string, data: any): void {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(data);
        } catch (error) {
          console.error(`Error in event handler for ${event}:`, error);
        }
      });
    }
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.subscriptions.clear();
    this.isConnected = false;
    this.currentHiveId = null;
    this.reconnectAttempts = 0;
  }

  /**
   * Get connection status
   */
  getConnectionStatus(): boolean {
    return this.isConnected;
  }

  /**
   * Get current hive ID
   */
  getCurrentHive(): string | null {
    return this.currentHiveId;
  }

  /**
   * Start heartbeat interval
   */
  startHeartbeat(interval: number = 30000): void {
    setInterval(() => {
      if (this.isConnected) {
        this.sendHeartbeat();
      }
    }, interval);
  }
}

// Export singleton instance
export const websocketManager = WebSocketManager.getInstance();

// Export event types for convenience
export { WS_EVENTS };

export default WebSocketManager;