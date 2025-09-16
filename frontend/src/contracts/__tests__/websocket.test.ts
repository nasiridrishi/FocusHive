/**
 * WebSocket Contract Tests
 * Following TDD principles - Writing tests FIRST before implementation
 */

import { describe, it, expect, expectTypeOf } from 'vitest';
import type {
  WebSocketMessage,
  WebSocketMessageType,
  WebSocketConnectionState,
  WebSocketConfig,
  WebSocketSubscription,
  WebSocketChannel,
  WebSocketChannelType,
  WebSocketEvent,
  WebSocketEventType,
  WebSocketError,
  WebSocketErrorCode,
  WebSocketHeartbeat,
  StompConfig,
  StompHeaders,
  PresenceUpdate,
  PresenceStatus,
  ChatMessage,
  HiveBroadcast,
  TimerSync,
  NotificationPayload,
  WebSocketMetrics,
  ConnectionInfo,
  ReconnectionStrategy
} from '../websocket';

describe('WebSocket Contracts', () => {
  describe('WebSocketMessage Interface', () => {
    it('should have required message properties', () => {
      const message: WebSocketMessage = {
        id: 'msg-123',
        type: 'CHAT_MESSAGE' as WebSocketMessageType,
        channel: '/topic/chat/hive-456',
        payload: {
          text: 'Hello, world!',
          senderId: 'user-123'
        },
        timestamp: new Date().toISOString(),
        correlationId: 'corr-789'
      };

      expect(message.id).toBeDefined();
      expect(message.type).toBeDefined();
      expect(message.channel).toBeDefined();
      expect(message.payload).toBeDefined();
      expect(message.timestamp).toBeDefined();
      expectTypeOf(message.id).toEqualTypeOf<string>();
      expectTypeOf(message.type).toEqualTypeOf<WebSocketMessageType>();
      expectTypeOf(message.channel).toEqualTypeOf<string>();
      expectTypeOf(message.correlationId).toMatchTypeOf<string | undefined>();
    });

    it('should support optional metadata', () => {
      const messageWithMetadata: WebSocketMessage = {
        id: 'msg-123',
        type: 'PRESENCE_UPDATE' as WebSocketMessageType,
        channel: '/topic/presence/hive-456',
        payload: { status: 'online' },
        timestamp: new Date().toISOString(),
        metadata: {
          priority: 'high',
          ttl: 60000,
          retries: 0,
          source: 'client'
        }
      };

      expect(messageWithMetadata.metadata).toBeDefined();
      expectTypeOf(messageWithMetadata.metadata).toMatchTypeOf<Record<string, any> | undefined>();
    });
  });

  describe('WebSocketMessageType', () => {
    it('should support all message types', () => {
      const messageTypes: WebSocketMessageType[] = [
        'PRESENCE_UPDATE',
        'CHAT_MESSAGE',
        'HIVE_BROADCAST',
        'TIMER_SYNC',
        'NOTIFICATION',
        'BUDDY_EVENT',
        'FORUM_UPDATE',
        'ANALYTICS_EVENT',
        'SYSTEM_ALERT',
        'CONNECTION_ACK',
        'HEARTBEAT',
        'ERROR'
      ];

      messageTypes.forEach(type => {
        expectTypeOf(type).toMatchTypeOf<WebSocketMessageType>();
      });
    });
  });

  describe('WebSocketConnectionState', () => {
    it('should support all connection states', () => {
      const states: WebSocketConnectionState[] = [
        'CONNECTING',
        'CONNECTED',
        'DISCONNECTED',
        'RECONNECTING',
        'ERROR',
        'CLOSED'
      ];

      states.forEach(state => {
        expectTypeOf(state).toMatchTypeOf<WebSocketConnectionState>();
      });
    });
  });

  describe('WebSocketConfig Interface', () => {
    it('should have required configuration properties', () => {
      const config: WebSocketConfig = {
        url: 'ws://localhost:8080/ws',
        reconnect: true,
        reconnectInterval: 5000,
        reconnectMaxRetries: 10,
        heartbeatInterval: 4000,
        connectionTimeout: 30000,
        debug: false
      };

      expect(config.url).toBeDefined();
      expect(config.reconnect).toBeDefined();
      expect(config.reconnectInterval).toBeGreaterThan(0);
      expectTypeOf(config.url).toEqualTypeOf<string>();
      expectTypeOf(config.reconnect).toEqualTypeOf<boolean>();
      expectTypeOf(config.reconnectMaxRetries).toMatchTypeOf<number | undefined>();
    });

    it('should support STOMP configuration', () => {
      const configWithStomp: WebSocketConfig = {
        url: 'ws://localhost:8080/ws',
        reconnect: true,
        reconnectInterval: 5000,
        heartbeatInterval: 4000,
        connectionTimeout: 30000,
        debug: false,
        stomp: {
          brokerURL: 'ws://localhost:8080/ws',
          connectHeaders: {
            Authorization: 'Bearer token-123',
            'X-Client-Id': 'client-456'
          },
          debug: false,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          reconnectDelay: 5000,
          stompVersion: '1.2'
        }
      };

      expect(configWithStomp.stomp).toBeDefined();
      expectTypeOf(configWithStomp.stomp).toMatchTypeOf<StompConfig | undefined>();
    });
  });

  describe('WebSocketSubscription Interface', () => {
    it('should have subscription properties', () => {
      const subscription: WebSocketSubscription = {
        id: 'sub-123',
        channel: '/topic/chat/hive-456',
        channelType: 'TOPIC' as WebSocketChannelType,
        callback: (message) => console.log(message),
        active: true,
        createdAt: Date.now(),
        messageCount: 0
      };

      expect(subscription.id).toBeDefined();
      expect(subscription.channel).toBeDefined();
      expect(subscription.active).toBe(true);
      expectTypeOf(subscription.callback).toEqualTypeOf<(message: WebSocketMessage) => void>();
      expectTypeOf(subscription.filters).toMatchTypeOf<Record<string, any> | undefined>();
    });

    it('should support subscription filters', () => {
      const subscriptionWithFilters: WebSocketSubscription = {
        id: 'sub-123',
        channel: '/topic/chat/hive-456',
        channelType: 'TOPIC' as WebSocketChannelType,
        callback: (message) => console.log(message),
        active: true,
        createdAt: Date.now(),
        messageCount: 0,
        filters: {
          userId: 'user-123',
          messageType: 'CHAT_MESSAGE',
          priority: 'high'
        }
      };

      expect(subscriptionWithFilters.filters).toBeDefined();
      expect(subscriptionWithFilters.filters?.userId).toBe('user-123');
    });
  });

  describe('WebSocketChannel Types', () => {
    it('should define channel structure', () => {
      const channel: WebSocketChannel = {
        name: '/topic/chat/hive-123',
        type: 'TOPIC' as WebSocketChannelType,
        description: 'Chat channel for hive 123',
        permissions: ['read', 'write'],
        subscribers: 5,
        isPrivate: false
      };

      expect(channel.name).toBeDefined();
      expect(channel.type).toBeDefined();
      expectTypeOf(channel.type).toMatchTypeOf<WebSocketChannelType>();
      expectTypeOf(channel.permissions).toMatchTypeOf<string[] | undefined>();
    });

    it('should support all channel types', () => {
      const channelTypes: WebSocketChannelType[] = [
        'TOPIC',
        'QUEUE',
        'USER_QUEUE',
        'TEMP_QUEUE',
        'APP'
      ];

      channelTypes.forEach(type => {
        expectTypeOf(type).toMatchTypeOf<WebSocketChannelType>();
      });
    });
  });

  describe('Presence Update Messages', () => {
    it('should define presence update structure', () => {
      const presence: PresenceUpdate = {
        userId: 'user-123',
        hiveId: 'hive-456',
        status: 'online' as PresenceStatus,
        lastActivity: new Date().toISOString(),
        deviceInfo: {
          deviceId: 'device-789',
          deviceType: 'desktop',
          browser: 'Chrome',
          os: 'Windows'
        },
        location: {
          country: 'US',
          city: 'New York',
          timezone: 'America/New_York'
        }
      };

      expect(presence.userId).toBeDefined();
      expect(presence.status).toBeDefined();
      expectTypeOf(presence.status).toMatchTypeOf<PresenceStatus>();
      expectTypeOf(presence.activity).toMatchTypeOf<string | undefined>();
    });

    it('should support all presence statuses', () => {
      const statuses: PresenceStatus[] = [
        'online',
        'away',
        'busy',
        'offline',
        'invisible'
      ];

      statuses.forEach(status => {
        expectTypeOf(status).toMatchTypeOf<PresenceStatus>();
      });
    });
  });

  describe('Chat Message Structure', () => {
    it('should define chat message properties', () => {
      const chatMessage: ChatMessage = {
        id: 'chat-123',
        hiveId: 'hive-456',
        senderId: 'user-789',
        senderName: 'John Doe',
        text: 'Hello everyone!',
        timestamp: new Date().toISOString(),
        edited: false,
        deleted: false,
        reactions: [],
        mentions: ['@user-456'],
        attachments: [],
        threadId: undefined,
        replyToId: undefined
      };

      expect(chatMessage.id).toBeDefined();
      expect(chatMessage.text).toBeDefined();
      expect(chatMessage.edited).toBe(false);
      expectTypeOf(chatMessage.reactions).toMatchTypeOf<any[] | undefined>();
      expectTypeOf(chatMessage.threadId).toMatchTypeOf<string | undefined>();
    });
  });

  describe('Hive Broadcast Messages', () => {
    it('should define hive broadcast structure', () => {
      const broadcast: HiveBroadcast = {
        hiveId: 'hive-123',
        type: 'USER_JOINED',
        userId: 'user-456',
        userName: 'Jane Smith',
        message: 'Jane Smith has joined the hive',
        timestamp: new Date().toISOString(),
        data: {
          totalUsers: 10,
          activeUsers: 8
        }
      };

      expect(broadcast.hiveId).toBeDefined();
      expect(broadcast.type).toBeDefined();
      expect(broadcast.message).toBeDefined();
      expectTypeOf(broadcast.data).toMatchTypeOf<Record<string, any> | undefined>();
    });
  });

  describe('Timer Sync Messages', () => {
    it('should define timer sync structure', () => {
      const timerSync: TimerSync = {
        userId: 'user-123',
        hiveId: 'hive-456',
        timerId: 'timer-789',
        action: 'START',
        duration: 1500000, // 25 minutes
        elapsedTime: 0,
        remainingTime: 1500000,
        isPaused: false,
        timestamp: new Date().toISOString(),
        syncedDevices: ['device-1', 'device-2']
      };

      expect(timerSync.action).toBeDefined();
      expect(timerSync.duration).toBeGreaterThan(0);
      expectTypeOf(timerSync.action).toEqualTypeOf<string>();
      expectTypeOf(timerSync.isPaused).toEqualTypeOf<boolean>();
    });
  });

  describe('WebSocket Error Handling', () => {
    it('should define error structure', () => {
      const error: WebSocketError = {
        code: 'CONNECTION_FAILED' as WebSocketErrorCode,
        message: 'Failed to connect to WebSocket server',
        timestamp: new Date().toISOString(),
        details: {
          attempt: 3,
          maxRetries: 10,
          nextRetryIn: 5000
        },
        recoverable: true
      };

      expect(error.code).toBeDefined();
      expect(error.message).toBeDefined();
      expect(error.recoverable).toBe(true);
      expectTypeOf(error.code).toMatchTypeOf<WebSocketErrorCode>();
      expectTypeOf(error.details).toMatchTypeOf<Record<string, any> | undefined>();
    });

    it('should support all error codes', () => {
      const errorCodes: WebSocketErrorCode[] = [
        'CONNECTION_FAILED',
        'CONNECTION_LOST',
        'AUTHENTICATION_FAILED',
        'AUTHORIZATION_FAILED',
        'SUBSCRIPTION_FAILED',
        'MESSAGE_SEND_FAILED',
        'HEARTBEAT_TIMEOUT',
        'INVALID_MESSAGE',
        'RATE_LIMIT_EXCEEDED',
        'SERVER_ERROR',
        'CLIENT_ERROR',
        'UNKNOWN_ERROR'
      ];

      errorCodes.forEach(code => {
        expectTypeOf(code).toMatchTypeOf<WebSocketErrorCode>();
      });
    });
  });

  describe('WebSocket Events', () => {
    it('should define event structure', () => {
      const event: WebSocketEvent = {
        type: 'CONNECTED' as WebSocketEventType,
        timestamp: new Date().toISOString(),
        data: {
          sessionId: 'session-123',
          serverTime: new Date().toISOString()
        }
      };

      expect(event.type).toBeDefined();
      expect(event.timestamp).toBeDefined();
      expectTypeOf(event.type).toMatchTypeOf<WebSocketEventType>();
      expectTypeOf(event.data).toMatchTypeOf<any | undefined>();
    });

    it('should support all event types', () => {
      const eventTypes: WebSocketEventType[] = [
        'CONNECTING',
        'CONNECTED',
        'DISCONNECTED',
        'RECONNECTING',
        'RECONNECTED',
        'ERROR',
        'MESSAGE_RECEIVED',
        'MESSAGE_SENT',
        'SUBSCRIPTION_ADDED',
        'SUBSCRIPTION_REMOVED',
        'HEARTBEAT_SENT',
        'HEARTBEAT_RECEIVED'
      ];

      eventTypes.forEach(type => {
        expectTypeOf(type).toMatchTypeOf<WebSocketEventType>();
      });
    });
  });

  describe('WebSocket Metrics', () => {
    it('should define metrics structure', () => {
      const metrics: WebSocketMetrics = {
        connectionTime: Date.now(),
        messagesReceived: 150,
        messagesSent: 75,
        bytesReceived: 45000,
        bytesSent: 22500,
        reconnectCount: 2,
        errorCount: 1,
        latency: 45,
        uptime: 3600000, // 1 hour
        subscriptions: 5
      };

      expect(metrics.messagesReceived).toBeGreaterThanOrEqual(0);
      expect(metrics.messagesSent).toBeGreaterThanOrEqual(0);
      expectTypeOf(metrics.latency).toMatchTypeOf<number | undefined>();
      expectTypeOf(metrics.subscriptions).toEqualTypeOf<number>();
    });
  });

  describe('Connection Info', () => {
    it('should define connection information', () => {
      const connectionInfo: ConnectionInfo = {
        url: 'ws://localhost:8080/ws',
        state: 'CONNECTED' as WebSocketConnectionState,
        sessionId: 'session-123',
        connectedAt: Date.now(),
        lastMessageAt: Date.now(),
        lastHeartbeatAt: Date.now(),
        protocol: 'STOMP',
        version: '1.2',
        serverInfo: {
          name: 'FocusHive Backend',
          version: '1.0.0',
          time: new Date().toISOString()
        }
      };

      expect(connectionInfo.url).toBeDefined();
      expect(connectionInfo.state).toBe('CONNECTED');
      expectTypeOf(connectionInfo.protocol).toMatchTypeOf<string | undefined>();
      expectTypeOf(connectionInfo.serverInfo).toMatchTypeOf<Record<string, any> | undefined>();
    });
  });

  describe('Reconnection Strategy', () => {
    it('should define reconnection strategy', () => {
      const strategy: ReconnectionStrategy = {
        enabled: true,
        maxRetries: 10,
        baseDelay: 1000,
        maxDelay: 30000,
        backoffMultiplier: 2,
        jitter: true,
        retryOn: ['CONNECTION_LOST', 'HEARTBEAT_TIMEOUT']
      };

      expect(strategy.enabled).toBe(true);
      expect(strategy.maxRetries).toBeGreaterThan(0);
      expect(strategy.backoffMultiplier).toBeGreaterThanOrEqual(1);
      expectTypeOf(strategy.retryOn).toMatchTypeOf<string[] | undefined>();
    });
  });

  describe('STOMP Configuration', () => {
    it('should define STOMP config structure', () => {
      const stompConfig: StompConfig = {
        brokerURL: 'ws://localhost:8080/ws',
        connectHeaders: {
          Authorization: 'Bearer token-123',
          'X-Client-Id': 'client-456'
        },
        debug: false,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        reconnectDelay: 5000,
        stompVersion: '1.2',
        webSocketFactory: undefined,
        beforeConnect: undefined,
        onConnect: undefined,
        onDisconnect: undefined,
        onStompError: undefined,
        onWebSocketError: undefined
      };

      expect(stompConfig.brokerURL).toBeDefined();
      expect(stompConfig.heartbeatIncoming).toBe(4000);
      expectTypeOf(stompConfig.connectHeaders).toMatchTypeOf<StompHeaders | undefined>();
      expectTypeOf(stompConfig.stompVersion).toMatchTypeOf<string | undefined>();
    });
  });

  describe('Notification Payload', () => {
    it('should define notification structure', () => {
      const notification: NotificationPayload = {
        id: 'notif-123',
        userId: 'user-456',
        type: 'HIVE_INVITE',
        title: 'Hive Invitation',
        message: 'You have been invited to join Study Group',
        priority: 'high',
        timestamp: new Date().toISOString(),
        read: false,
        actionUrl: '/hives/hive-789',
        data: {
          hiveId: 'hive-789',
          invitedBy: 'user-123'
        }
      };

      expect(notification.id).toBeDefined();
      expect(notification.type).toBeDefined();
      expect(notification.read).toBe(false);
      expectTypeOf(notification.priority).toMatchTypeOf<string | undefined>();
      expectTypeOf(notification.actionUrl).toMatchTypeOf<string | undefined>();
    });
  });

  describe('WebSocket Heartbeat', () => {
    it('should define heartbeat structure', () => {
      const heartbeat: WebSocketHeartbeat = {
        timestamp: Date.now(),
        sequence: 42,
        latency: 35,
        serverTime: new Date().toISOString()
      };

      expect(heartbeat.timestamp).toBeDefined();
      expect(heartbeat.sequence).toBeGreaterThanOrEqual(0);
      expectTypeOf(heartbeat.latency).toMatchTypeOf<number | undefined>();
      expectTypeOf(heartbeat.serverTime).toMatchTypeOf<string | undefined>();
    });
  });
});