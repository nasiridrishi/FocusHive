/**
 * E2E Tests for WebSocketService with Real Backend
 *
 * REQUIREMENTS:
 * - Real FocusHive Backend running at ws://localhost:8080/ws
 * - Real STOMP connection with SockJS
 * - No mocks or stubs
 * - Tests actual WebSocket communication
 */

import { describe, it, expect, beforeAll, beforeEach, afterEach, vi } from 'vitest';
import axios from 'axios';
import { webSocketService } from '../WebSocketService';
import type { WebSocketMessage, PresenceUpdate, NotificationMessage } from '../WebSocketService';
import { MessageType, PresenceStatus } from '../WebSocketService';

// Backend URLs
const BACKEND_URL = 'http://localhost:8080';
const WEBSOCKET_URL = 'ws://localhost:8080/ws';

describe('WebSocketService E2E Tests with Real Backend', () => {
  beforeAll(async () => {
    // Verify Backend is running (required for production)
    console.log('Verifying Backend at:', BACKEND_URL);
    const response = await axios.get(`${BACKEND_URL}/actuator/health`, {
      timeout: 5000,
      headers: {
        'Accept': 'application/json'
      }
    });

    if (response?.data?.status !== 'UP') {
      throw new Error(`Backend is not healthy. Status: ${response?.data?.status}`);
    }

    console.log('✅ Backend is running and healthy');
  });

  beforeEach(() => {
    // Set up auth token provider
    webSocketService.setAuthTokenProvider(() => 'test-token');
  });

  afterEach(() => {
    // Disconnect and cleanup after each test
    if (webSocketService.isConnectedStatus()) {
      webSocketService.disconnect();
    }
  });

  describe('Connection Management', () => {
    it('should attempt to connect without hanging', () => {
      // Just verify we can call connect without errors
      expect(() => {
        webSocketService.connect();
      }).not.toThrow();

      // Disconnect immediately
      webSocketService.disconnect();
    });
    it('should connect to the backend WebSocket', async () => {
      // Connect to the backend (service uses internal config)
      webSocketService.connect();

      // Wait for connection with timeout
      const timeout = 10000; // 10 seconds
      const startTime = Date.now();

      await new Promise<void>((resolve, reject) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve();
          } else if (Date.now() - startTime > timeout) {
            reject(new Error(`WebSocket connection timeout after ${timeout}ms. State: ${webSocketService.getConnectionState()}`));
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Should be connected
      expect(webSocketService.isConnectedStatus()).toBe(true);

      // Get connection state
      const connectionState = webSocketService.getConnectionState();
      expect(connectionState).toBe('CONNECTED');
    });

    it('should disconnect from the backend', async () => {
      // Connect first
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      expect(webSocketService.isConnectedStatus()).toBe(true);

      // Disconnect
      webSocketService.disconnect();
      expect(webSocketService.isConnectedStatus()).toBe(false);

      // Connection state should reflect disconnected state
      const connectionState = webSocketService.getConnectionState();
      expect(connectionState).toBe('DISCONNECTED');
    });

    it('should handle reconnection state', async () => {
      // Connect to backend
      webSocketService.connect();

      // Wait briefly
      await new Promise(resolve => setTimeout(resolve, 500));

      // Check reconnection info
      const reconnectionInfo = webSocketService.getReconnectionInfo();
      expect(reconnectionInfo).toBeDefined();
      expect(reconnectionInfo.attempts).toBeGreaterThanOrEqual(0);
      expect(reconnectionInfo.maxAttempts).toBeGreaterThan(0);
      expect(reconnectionInfo.isReconnecting).toBe(false);
    });

    it('should support retry connection', async () => {
      // Connect
      webSocketService.connect();

      // Wait briefly
      await new Promise(resolve => setTimeout(resolve, 500));

      // Disconnect
      webSocketService.disconnect();
      expect(webSocketService.isConnectedStatus()).toBe(false);

      // Retry connection
      webSocketService.retryConnection();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Should be reconnected
      expect(webSocketService.isConnectedStatus()).toBe(true);
    });
  });

  describe('Subscription Management', () => {
    it('should subscribe to a channel', async () => {
      // Connect first
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Subscribe to a test channel
      const messageReceived = vi.fn();
      const subscriptionId = webSocketService.subscribe(
        '/topic/test',
        messageReceived
      );

      expect(subscriptionId).toBeDefined();
      expect(subscriptionId).toBeTruthy();
    });

    it('should unsubscribe from a channel', async () => {
      // Connect and subscribe
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      const subscriptionId = webSocketService.subscribe(
        '/topic/test',
        vi.fn()
      );

      expect(subscriptionId).toBeTruthy();

      // Unsubscribe
      webSocketService.unsubscribe(subscriptionId);

      // Verify unsubscribed (no direct way to check, but operation should not throw)
      expect(() => webSocketService.unsubscribe(subscriptionId)).not.toThrow();
    });

    it('should handle multiple subscriptions', async () => {
      // Connect
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Subscribe to multiple channels
      const sub1 = webSocketService.subscribe('/topic/test1', vi.fn());
      const sub2 = webSocketService.subscribe('/topic/test2', vi.fn());
      const sub3 = webSocketService.subscribe('/topic/test3', vi.fn());

      expect(sub1).toBeTruthy();
      expect(sub2).toBeTruthy();
      expect(sub3).toBeTruthy();
      expect(sub1).not.toBe(sub2);
      expect(sub2).not.toBe(sub3);
    });
  });

  describe('Message Sending', () => {
    it('should send a message to a channel', async () => {
      // Connect
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Send a test message
      const testMessage = {
        text: 'Test message from E2E test',
        timestamp: new Date().toISOString()
      };

      // Should not throw
      expect(() => {
        webSocketService.sendMessage('/app/test', testMessage);
      }).not.toThrow();
    });

    it('should handle send when not connected', () => {
      // Ensure disconnected
      webSocketService.disconnect();

      // Try to send without connecting
      const testMessage = { text: 'Test' };

      // Service doesn't throw, just doesn't send
      expect(() => {
        webSocketService.sendMessage('/app/test', testMessage);
      }).not.toThrow();
    });
  });

  describe('Presence Updates', () => {
    it('should send presence updates', async () => {
      // Connect
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Subscribe to presence updates for a test hive
      const hiveId = 123;
      const subscriptionId = webSocketService.subscribeToHivePresence(hiveId);

      expect(subscriptionId).toBeTruthy();

      // Update presence status
      expect(() => {
        webSocketService.updatePresenceStatus(PresenceStatus.ONLINE, hiveId, 'Working');
      }).not.toThrow();

      // Start focus session
      expect(() => {
        webSocketService.startFocusSession(hiveId, 25);
      }).not.toThrow();
    });
  });

  describe('Buddy System Methods', () => {
    it('should send buddy requests', async () => {
      // Connect
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Send buddy request
      expect(() => {
        webSocketService.sendBuddyRequest(123, 'Let\'s work together!');
      }).not.toThrow();

      // Accept buddy request
      expect(() => {
        webSocketService.acceptBuddyRequest(456);
      }).not.toThrow();
    });

    it('should handle buddy sessions', async () => {
      // Connect
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Start buddy session
      expect(() => {
        webSocketService.startBuddySession(789);
      }).not.toThrow();

      // End buddy session
      expect(() => {
        webSocketService.endBuddySession(789);
      }).not.toThrow();
    });
  });

  describe('Event Handling', () => {
    it('should handle connection state changes', async () => {
      // Set up connection change handler
      const connectionHandler = vi.fn();
      webSocketService.onConnectionChange(connectionHandler);

      // Connect
      webSocketService.connect();

      // Wait for connection
      await new Promise((resolve) => {
        const checkConnection = () => {
          if (webSocketService.isConnectedStatus()) {
            resolve(undefined);
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        setTimeout(checkConnection, 100);
      });

      // Handler should be called with connection state
      expect(connectionHandler).toHaveBeenCalled();
      expect(connectionHandler).toHaveBeenCalledWith(true);

      // Disconnect
      webSocketService.disconnect();

      // Should be called with disconnected state
      expect(connectionHandler).toHaveBeenCalledWith(false);
    });

    it('should handle message handlers', () => {
      // Set up message handler
      const messageHandler = vi.fn();
      webSocketService.onMessage('test', messageHandler);

      // Set up presence handler
      const presenceHandler = vi.fn();
      webSocketService.onPresenceUpdate(presenceHandler);

      // Handlers should be registered (no way to verify directly without messages)
      expect(() => {
        webSocketService.onMessage('test2', vi.fn());
        webSocketService.onPresenceUpdate(vi.fn());
      }).not.toThrow();
    });
  });
});