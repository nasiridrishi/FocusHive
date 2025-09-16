/**
 * WebSocket Helper Functions for Hive E2E Tests
 * Provides utilities for testing real-time features and WebSocket connections
 */

import {Page} from '@playwright/test';

export interface WebSocketMessage {
  type: string;
  payload: Record<string, unknown>;
  timestamp: string;
}

// Extend Window interface for WebSocket testing
declare global {
  interface Window {
    __wsConnection?: WebSocketConnection;
    __wsMessageLog?: WebSocketMessage[];
    __currentWebSocket?: WebSocket;
  }
}

export interface WebSocketConnection {
  url: string;
  readyState: number;
  isConnected: boolean;
  messageCount: number;
  lastMessage?: WebSocketMessage;
}

export interface PresenceEvent {
  type: 'PRESENCE_UPDATE' | 'USER_JOINED' | 'USER_LEFT';
  userId: number;
  hiveId: number;
  status?: 'active' | 'away' | 'break' | 'offline';
  timestamp: string;
}

export interface TimerEvent {
  type: 'TIMER_START' | 'TIMER_PAUSE' | 'TIMER_RESUME' | 'TIMER_STOP' | 'TIMER_SYNC';
  sessionId: string;
  hiveId: number;
  remainingTime?: number;
  isActive?: boolean;
  participants?: number[];
  timestamp: string;
}

export interface ChatEvent {
  type: 'MESSAGE_SENT' | 'MESSAGE_RECEIVED' | 'TYPING_START' | 'TYPING_STOP';
  hiveId: number;
  userId: number;
  messageId?: string;
  content?: string;
  timestamp: string;
}

export type WebSocketEvent = PresenceEvent | TimerEvent | ChatEvent;

export class WebSocketTestHelper {
  private page: Page;
  private messageLog: WebSocketMessage[] = [];
  private eventLog: WebSocketEvent[] = [];
  private connectionInfo: WebSocketConnection | null = null;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Initialize WebSocket monitoring
   */
  async initializeWebSocketMonitoring(): Promise<void> {
    await this.page.evaluateOnNewDocument(() => {
      // Store original WebSocket
      const OriginalWebSocket = window.WebSocket;
      const messageLog: WebSocketMessage[] = [];

      // Override WebSocket constructor
      window.WebSocket = class extends OriginalWebSocket {
        constructor(url: string | URL, protocols?: string | string[]) {
          super(url, protocols);

          // Store connection info
          window.__wsConnection = {
            url: url.toString(),
            readyState: this.readyState,
            isConnected: false,
            messageCount: 0
          };

          // Monitor connection events
          this.addEventListener('open', () => {
            if (window.__wsConnection) {
              window.__wsConnection.isConnected = true;
              window.__wsConnection.readyState = this.readyState;
            }
          });

          this.addEventListener('close', () => {
            if (window.__wsConnection) {
              window.__wsConnection.isConnected = false;
              window.__wsConnection.readyState = this.readyState;
            }
          });

          this.addEventListener('error', () => {
            if (window.__wsConnection) {
              window.__wsConnection.readyState = this.readyState;
            }
          });

          // Monitor messages
          this.addEventListener('message', (event) => {
            try {
              const data = JSON.parse(event.data);
              const message: WebSocketMessage = {
                type: data.type || 'UNKNOWN',
                payload: data.payload || data,
                timestamp: new Date().toISOString()
              };

              messageLog.push(message);
              if (window.__wsConnection) {
                window.__wsConnection.messageCount = messageLog.length;
                window.__wsConnection.lastMessage = message;
              }

              // Store in global accessible location
              window.__wsMessageLog = messageLog;
            } catch (error) {
              console.warn('Failed to parse WebSocket message:', error);
            }
          });
        }
      };
    });
  }

  /**
   * Get current WebSocket connection info
   */
  async getConnectionInfo(): Promise<WebSocketConnection | null> {
    const connectionInfo = await this.page.evaluate(() => window.__wsConnection || null);
    this.connectionInfo = connectionInfo;
    return connectionInfo;
  }

  /**
   * Wait for WebSocket connection to be established
   */
  async waitForConnection(timeoutMs: number = 10000): Promise<boolean> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeoutMs) {
      const info = await this.getConnectionInfo();
      if (info?.isConnected) {
        return true;
      }
      await this.page.waitForTimeout(100);
    }

    return false;
  }

  /**
   * Wait for specific WebSocket message type
   */
  async waitForMessage(messageType: string, timeoutMs: number = 5000): Promise<WebSocketMessage | null> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeoutMs) {
      const messages = await this.getMessageLog();
      const targetMessage = messages.find(msg => msg.type === messageType);

      if (targetMessage) {
        return targetMessage;
      }

      await this.page.waitForTimeout(100);
    }

    return null;
  }

  /**
   * Get all WebSocket messages
   */
  async getMessageLog(): Promise<WebSocketMessage[]> {
    const messages = await this.page.evaluate(() => window.__wsMessageLog || []);
    this.messageLog = messages;
    return messages;
  }

  /**
   * Clear message log
   */
  async clearMessageLog(): Promise<void> {
    await this.page.evaluate(() => {
      window.__wsMessageLog = [];
      if (window.__wsConnection) {
        window.__wsConnection.messageCount = 0;
      }
    });
    this.messageLog = [];
  }

  /**
   * Send WebSocket message
   */
  async sendMessage(message: WebSocketMessage): Promise<void> {
    await this.page.evaluate((msg) => {
      const ws = window.__currentWebSocket;
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(msg));
      }
    }, message);
  }

  /**
   * Measure WebSocket message latency
   */
  async measureMessageLatency(messageType: string): Promise<number> {
    const startTime = Date.now();

    // Send a test message
    await this.sendMessage({
      type: messageType,
      payload: {timestamp: startTime},
      timestamp: new Date().toISOString()
    });

    // Wait for response
    const response = await this.waitForMessage(`${messageType}_RESPONSE`);

    if (response) {
      return Date.now() - startTime;
    }

    return -1; // Timeout
  }

  /**
   * Test presence update propagation
   */
  async testPresenceUpdate(userId: number, status: string): Promise<{
    sent: boolean;
    received: boolean;
    latency: number;
  }> {
    const startTime = Date.now();

    // Clear previous messages
    await this.clearMessageLog();

    // Trigger presence update in UI
    await this.page.click('[data-testid="user-presence-dropdown"]');
    await this.page.click(`[data-testid="presence-status-${status}"]`);

    // Wait for WebSocket message
    const message = await this.waitForMessage('PRESENCE_UPDATE');
    const latency = message ? Date.now() - startTime : -1;

    return {
      sent: message !== null,
      received: message?.payload.userId === userId,
      latency
    };
  }

  /**
   * Test timer synchronization
   */
  async testTimerSync(_sessionId: string): Promise<{
    syncReceived: boolean;
    latency: number;
    participantCount: number;
  }> {
    const startTime = Date.now();

    // Clear previous messages
    await this.clearMessageLog();

    // Start timer
    await this.page.click('[data-testid="start-timer-button"]');

    // Wait for timer sync message
    const syncMessage = await this.waitForMessage('TIMER_SYNC');
    const latency = syncMessage ? Date.now() - startTime : -1;

    return {
      syncReceived: syncMessage !== null,
      latency,
      participantCount: Array.isArray(syncMessage?.payload.participants)
          ? (syncMessage.payload.participants as unknown[]).length
          : 0
    };
  }

  /**
   * Monitor connection stability
   */
  async monitorConnectionStability(durationMs: number): Promise<{
    disconnections: number;
    averageLatency: number;
    messageCount: number;
  }> {
    const startTime = Date.now();
    const initialMessageCount = await this.getMessageCount();
    let disconnections = 0;
    const latencies: number[] = [];

    while (Date.now() - startTime < durationMs) {
      const info = await this.getConnectionInfo();

      if (info && !info.isConnected) {
        disconnections++;
      }

      // Test latency with ping
      const pingLatency = await this.measureMessageLatency('PING');
      if (pingLatency > 0) {
        latencies.push(pingLatency);
      }

      await this.page.waitForTimeout(1000); // Check every second
    }

    const finalMessageCount = await this.getMessageCount();
    const averageLatency = latencies.length > 0
        ? latencies.reduce((sum, lat) => sum + lat, 0) / latencies.length
        : 0;

    return {
      disconnections,
      averageLatency,
      messageCount: finalMessageCount - initialMessageCount
    };
  }

  /**
   * Get message count
   */
  async getMessageCount(): Promise<number> {
    const info = await this.getConnectionInfo();
    return info?.messageCount || 0;
  }

  /**
   * Simulate connection issues
   */
  async simulateConnectionIssues(type: 'disconnect' | 'slowNetwork' | 'highLatency'): Promise<void> {
    switch (type) {
      case 'disconnect':
        await this.page.evaluate(() => {
          const ws = window.__currentWebSocket;
          if (ws) {
            ws.close();
          }
        });
        break;

      case 'slowNetwork':
        await this.page.context().setOffline(true);
        await this.page.waitForTimeout(2000);
        await this.page.context().setOffline(false);
        break;

      case 'highLatency':
        // Simulate high latency by delaying WebSocket sends
        await this.page.evaluate(() => {
          const OriginalSend = WebSocket.prototype.send;
          WebSocket.prototype.send = function (data) {
            setTimeout(() => OriginalSend.call(this, data), 1000);
          };
        });
        break;
    }
  }

  /**
   * Verify message ordering
   */
  async verifyMessageOrdering(expectedOrder: string[]): Promise<boolean> {
    const messages = await this.getMessageLog();
    const messageTypes = messages.map(msg => msg.type);

    // Check if the expected messages appear in order
    let lastIndex = -1;
    for (const expectedType of expectedOrder) {
      const index = messageTypes.indexOf(expectedType, lastIndex + 1);
      if (index === -1 || index <= lastIndex) {
        return false;
      }
      lastIndex = index;
    }

    return true;
  }

  /**
   * Test concurrent message handling
   */
  async testConcurrentMessages(messageCount: number): Promise<{
    sent: number;
    received: number;
    duplicates: number;
    outOfOrder: number;
  }> {
    await this.clearMessageLog();
    const sentMessages: string[] = [];

    // Send multiple messages rapidly
    for (let i = 0; i < messageCount; i++) {
      const messageId = `test_${i}_${Date.now()}`;
      sentMessages.push(messageId);

      await this.sendMessage({
        type: 'TEST_MESSAGE',
        payload: {id: messageId, sequence: i},
        timestamp: new Date().toISOString()
      });
    }

    // Wait for all messages to be processed
    await this.page.waitForTimeout(2000);

    const receivedMessages = await this.getMessageLog();
    const testMessages = receivedMessages.filter(msg => msg.type === 'TEST_MESSAGE');

    // Analyze results
    const receivedIds = testMessages.map(msg => msg.payload.id as string);
    const duplicates = receivedIds.length - new Set(receivedIds).size;

    let outOfOrder = 0;
    for (let i = 1; i < testMessages.length; i++) {
      const currentSeq = testMessages[i].payload.sequence as number;
      const prevSeq = testMessages[i - 1].payload.sequence as number;
      if (currentSeq < prevSeq) {
        outOfOrder++;
      }
    }

    return {
      sent: messageCount,
      received: testMessages.length,
      duplicates,
      outOfOrder
    };
  }

  /**
   * Get connection performance metrics
   */
  async getPerformanceMetrics(): Promise<{
    connectionTime: number;
    averageLatency: number;
    messageRate: number;
    errorRate: number;
  }> {
    const info = await this.getConnectionInfo();
    const messages = await this.getMessageLog();

    // Calculate metrics (simplified implementation)
    const connectionTime = info?.isConnected ? 0 : -1; // Would need to track actual connection time
    const averageLatency = 0; // Would need to implement latency tracking
    const messageRate = messages.length; // Messages per session
    const errorRate = 0; // Would need to track errors

    return {
      connectionTime,
      averageLatency,
      messageRate,
      errorRate
    };
  }
}

export default WebSocketTestHelper;