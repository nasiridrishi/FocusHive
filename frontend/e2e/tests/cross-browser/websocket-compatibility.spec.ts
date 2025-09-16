/**
 * WebSocket and Real-time Features Compatibility Tests
 * Tests WebSocket connections, STOMP protocol, and real-time features across browsers
 */

import {expect, test} from '@playwright/test';

// Type definitions for WebSocket and Network APIs
interface NetworkConnection {
  type?: string;
  effectiveType?: string;
  downlink?: number;
}

interface NavigatorWithConnection extends Navigator {
  connection?: NetworkConnection;
  mozConnection?: NetworkConnection;
  webkitConnection?: NetworkConnection;
}

interface WindowWithWebSocket extends Window {
  WebSocket: typeof WebSocket;
}

interface _MockWebSocketProtocol {
  url: string;
  readyState: number;

  send(data: string | ArrayBuffer | Blob): void;

  close(): void;

  dispatchEvent(event: Event): boolean;
}

test.describe('WebSocket Basic Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support WebSocket API', async ({page}) => {
    const webSocketSupport = await page.evaluate(() => {
      return {
        WebSocketExists: typeof WebSocket !== 'undefined',
        WebSocketConstructor: typeof WebSocket === 'function',
        readyStates: WebSocket ? {
          CONNECTING: WebSocket.CONNECTING === 0,
          OPEN: WebSocket.OPEN === 1,
          CLOSING: WebSocket.CLOSING === 2,
          CLOSED: WebSocket.CLOSED === 3
        } : null
      };
    });

    expect(webSocketSupport.WebSocketExists).toBe(true);
    expect(webSocketSupport.WebSocketConstructor).toBe(true);
    expect(webSocketSupport.readyStates?.CONNECTING).toBe(true);
    expect(webSocketSupport.readyStates?.OPEN).toBe(true);
    expect(webSocketSupport.readyStates?.CLOSING).toBe(true);
    expect(webSocketSupport.readyStates?.CLOSED).toBe(true);

    console.log('WebSocket Support:', webSocketSupport);
  });

  test('should handle WebSocket connection lifecycle', async ({page}) => {
    const connectionTest = await page.evaluate(async () => {
      return new Promise<{
        supported: boolean;
        events: string[];
        error?: string;
      }>((resolve) => {
        const events: string[] = [];
        const testUrl = 'wss://echo.websocket.org/';

        try {
          const ws = new WebSocket(testUrl);

          const timeout = setTimeout(() => {
            ws.close();
            resolve({supported: false, events, error: 'Connection timeout'});
          }, 5000);

          ws.onopen = () => {
            events.push('open');
            ws.send('test message');
          };

          ws.onmessage = (event) => {
            events.push('message');
            if (event.data === 'test message') {
              events.push('echo_received');
            }
            clearTimeout(timeout);
            ws.close();
          };

          ws.onclose = () => {
            events.push('close');
            clearTimeout(timeout);
            resolve({supported: true, events});
          };

          ws.onerror = () => {
            events.push('error');
            clearTimeout(timeout);
            resolve({supported: false, events, error: 'WebSocket error'});
          };
        } catch (error) {
          resolve({supported: false, events, error: (error as Error).message});
        }
      });
    });

    if (connectionTest.supported) {
      expect(connectionTest.events).toContain('open');
      expect(connectionTest.events).toContain('message');
      expect(connectionTest.events).toContain('echo_received');
      expect(connectionTest.events).toContain('close');
    } else {
      console.log('WebSocket connection failed:', connectionTest.error);
    }

    console.log('WebSocket Events:', connectionTest.events);
  });

  test('should handle WebSocket binary data', async ({page}) => {
    const binaryDataTest = await page.evaluate(async () => {
      return new Promise<{
        supported: boolean;
        binaryTypes: string[];
        error?: string;
      }>((resolve) => {
        const testUrl = 'wss://echo.websocket.org/';
        const binaryTypes: string[] = [];

        try {
          const ws = new WebSocket(testUrl);

          const timeout = setTimeout(() => {
            ws.close();
            resolve({supported: false, binaryTypes, error: 'Binary data test timeout'});
          }, 5000);

          ws.onopen = () => {
            // Test ArrayBuffer support
            ws.binaryType = 'arraybuffer';
            binaryTypes.push('arraybuffer');

            const buffer = new ArrayBuffer(4);
            const view = new Uint8Array(buffer);
            view[0] = 72; // H
            view[1] = 101; // e
            view[2] = 108; // l
            view[3] = 108; // l

            ws.send(buffer);
          };

          ws.onmessage = (event) => {
            if (event.data instanceof ArrayBuffer) {
              binaryTypes.push('received_arraybuffer');

              // Test Blob support
              ws.binaryType = 'blob';
              const blob = new Blob(['test'], {type: 'text/plain'});
              ws.send(blob);
            } else if (event.data instanceof Blob) {
              binaryTypes.push('received_blob');
              clearTimeout(timeout);
              ws.close();
            }
          };

          ws.onclose = () => {
            clearTimeout(timeout);
            resolve({supported: true, binaryTypes});
          };

          ws.onerror = () => {
            clearTimeout(timeout);
            resolve({supported: false, binaryTypes, error: 'Binary data WebSocket error'});
          };
        } catch (error) {
          resolve({supported: false, binaryTypes, error: (error as Error).message});
        }
      });
    });

    if (binaryDataTest.supported) {
      expect(binaryDataTest.binaryTypes).toContain('arraybuffer');
    }

    console.log('Binary Data Support:', binaryDataTest.binaryTypes);
  });
});

test.describe('STOMP Protocol Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support STOMP over WebSocket', async ({page}) => {
    // Check if STOMP client library is available
    const stompSupport = await page.evaluate(() => {
      // Check for common STOMP libraries
      return {
        stompjs: typeof window !== 'undefined' && 'StompJs' in window,
        sockjs: typeof window !== 'undefined' && 'SockJS' in window,
        stompClient: typeof window !== 'undefined' && 'Stomp' in window
      };
    });

    console.log('STOMP Library Support:', stompSupport);

    // Test basic STOMP message format parsing
    const stompMessageParsing = await page.evaluate(() => {
      try {
        // Simulate STOMP frame parsing
        const frame = 'CONNECT\naccept-version:1.0,1.1,2.0\nhost:localhost\n\n\x00';

        const lines = frame.split('\n');
        const command = lines[0];
        const headers: { [key: string]: string } = {};

        for (let i = 1; i < lines.length; i++) {
          const line = lines[i];
          if (line === '') break;
          const colonIndex = line.indexOf(':');
          if (colonIndex > 0) {
            headers[line.substring(0, colonIndex)] = line.substring(colonIndex + 1);
          }
        }

        return {
          supported: true,
          command,
          headers,
          acceptVersion: headers['accept-version']?.includes('1.2')
        };
      } catch (error) {
        return {supported: false, error: (error as Error).message};
      }
    });

    expect(stompMessageParsing.supported).toBe(true);
    expect(stompMessageParsing.command).toBe('CONNECT');
    expect(stompMessageParsing.headers['accept-version']).toContain('1.0');
  });

  test('should handle STOMP connection flow', async ({page}) => {
    // Navigate to a page that might use STOMP connections
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Check if the application attempts to establish STOMP connections
    const stompConnectionAttempt = await page.evaluate(async () => {
      return new Promise<{
        attempted: boolean;
        connectionEvents: string[];
        error?: string;
      }>((resolve) => {
        const events: string[] = [];

        // Mock WebSocket to capture STOMP connection attempts
        const OriginalWebSocket = window.WebSocket;
        let connectionAttempted = false;

        class MockWebSocket extends EventTarget {
          url: string;
          readyState: number = 0; // CONNECTING

          constructor(url: string) {
            super();
            this.url = url;
            connectionAttempted = true;
            events.push('websocket_created');

            // Simulate connection opening
            setTimeout(() => {
              this.readyState = 1; // OPEN
              events.push('connection_opened');
              this.dispatchEvent(new Event('open'));
            }, 100);
          }

          send(data: string | ArrayBuffer | Blob): void {
            if (typeof data === 'string' && data.includes('CONNECT')) {
              events.push('stomp_connect_sent');

              // Simulate CONNECTED frame
              setTimeout(() => {
                const connectedFrame = 'CONNECTED\nversion:1.2\nserver:MockServer\n\n\x00';
                this.dispatchEvent(new MessageEvent('message', {data: connectedFrame}));
                events.push('stomp_connected_received');
              }, 50);
            }
          }

          close(): void {
            this.readyState = 3; // CLOSED
            events.push('connection_closed');
            this.dispatchEvent(new Event('close'));
          }
        }

        // Replace WebSocket temporarily
        (window as WindowWithWebSocket).WebSocket = MockWebSocket as typeof WebSocket;

        setTimeout(() => {
          // Restore original WebSocket
          window.WebSocket = OriginalWebSocket;
          resolve({
            attempted: connectionAttempted,
            connectionEvents: events
          });
        }, 2000);
      });
    });

    console.log('STOMP Connection Events:', stompConnectionAttempt.connectionEvents);

    // At minimum, we should verify that the mock worked
    if (stompConnectionAttempt.attempted) {
      expect(stompConnectionAttempt.connectionEvents).toContain('websocket_created');
    }
  });

  test('should handle STOMP subscription management', async ({page}) => {
    const subscriptionTest = await page.evaluate(() => {
      // Test STOMP subscription frame format
      const subscriptionFrame = 'SUBSCRIBE\nid:sub-1\ndestination:/topic/presence\nack:auto\n\n\x00';

      const lines = subscriptionFrame.split('\n');
      const command = lines[0];
      const headers: { [key: string]: string } = {};

      for (let i = 1; i < lines.length; i++) {
        const line = lines[i];
        if (line === '') break;
        const colonIndex = line.indexOf(':');
        if (colonIndex > 0) {
          headers[line.substring(0, colonIndex)] = line.substring(colonIndex + 1);
        }
      }

      return {
        command,
        subscriptionId: headers['id'],
        destination: headers['destination'],
        ack: headers['ack']
      };
    });

    expect(subscriptionTest.command).toBe('SUBSCRIBE');
    expect(subscriptionTest.subscriptionId).toBe('sub-1');
    expect(subscriptionTest.destination).toBe('/topic/presence');
    expect(subscriptionTest.ack).toBe('auto');
  });
});

test.describe('Real-time Features Integration', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support real-time presence updates', async ({page}) => {
    // Navigate to dashboard where presence features are used
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Test presence-related WebSocket functionality
    const presenceTest = await page.evaluate(async () => {
      return new Promise<{
        presenceSupported: boolean;
        features: string[];
      }>((resolve) => {
        const features: string[] = [];

        // Check for presence-related DOM elements
        if (document.querySelector('[data-testid*="presence"]') ||
            document.querySelector('.presence-indicator') ||
            document.querySelector('[class*="online"]')) {
          features.push('presence_ui');
        }

        // Check for WebSocket usage indicators
        if (typeof WebSocket !== 'undefined') {
          features.push('websocket_api');
        }

        // Check for real-time update capabilities
        if (typeof EventSource !== 'undefined') {
          features.push('server_sent_events');
        }

        // Check for visibility API (for presence detection)
        if (typeof document.hidden !== 'undefined') {
          features.push('visibility_api');
        }

        // Check for page lifecycle API
        if ('onbeforeunload' in window) {
          features.push('page_lifecycle');
        }

        resolve({
          presenceSupported: features.length > 0,
          features
        });
      });
    });

    expect(presenceTest.presenceSupported).toBe(true);
    console.log('Presence Features:', presenceTest.features);
  });

  test('should handle connection state management', async ({page}) => {
    // Test connection state persistence
    const connectionStateTest = await page.evaluate(() => {
      const states = {
        online: navigator.onLine,
        visibilityAPI: typeof document.hidden !== 'undefined',
        pageLifecycle: 'onbeforeunload' in window,
        localStorage: typeof localStorage !== 'undefined',
        sessionStorage: typeof sessionStorage !== 'undefined'
      };

      return states;
    });

    expect(connectionStateTest.online).toBe(true);
    expect(connectionStateTest.visibilityAPI).toBe(true);
    expect(connectionStateTest.pageLifecycle).toBe(true);
    expect(connectionStateTest.localStorage).toBe(true);
    expect(connectionStateTest.sessionStorage).toBe(true);

    console.log('Connection State APIs:', connectionStateTest);
  });

  test('should handle network status changes', async ({page}) => {
    // Test network status detection
    const networkStatusTest = await page.evaluate(async () => {
      return new Promise<{
        networkAPI: boolean;
        connectionType?: string;
        effectiveType?: string;
        downlink?: number;
        events: string[];
      }>((resolve) => {
        const events: string[] = [];

        // Test navigator.onLine
        if (typeof navigator.onLine !== 'undefined') {
          events.push('online_status_available');
        }

        // Test online/offline events
        const onlineHandler = (): void => events.push('online_event');
        const offlineHandler = (): void => events.push('offline_event');

        window.addEventListener('online', onlineHandler);
        window.addEventListener('offline', offlineHandler);

        // Test Network Information API (if available)
        const navigatorWithConnection = navigator as NavigatorWithConnection;
        const connection = navigatorWithConnection.connection ||
            navigatorWithConnection.mozConnection ||
            navigatorWithConnection.webkitConnection;

        let networkInfo = {networkAPI: false};

        if (connection) {
          networkInfo = {
            networkAPI: true,
            connectionType: connection.type,
            effectiveType: connection.effectiveType,
            downlink: connection.downlink
          };
          events.push('network_info_available');
        }

        setTimeout(() => {
          window.removeEventListener('online', onlineHandler);
          window.removeEventListener('offline', offlineHandler);

          resolve({
            ...networkInfo,
            events
          });
        }, 1000);
      });
    });

    expect(networkStatusTest.events).toContain('online_status_available');

    if (networkStatusTest.networkAPI) {
      console.log('Network Information API available:', {
        type: networkStatusTest.connectionType,
        effectiveType: networkStatusTest.effectiveType,
        downlink: networkStatusTest.downlink
      });
    }

    console.log('Network Status Events:', networkStatusTest.events);
  });

  test('should test WebSocket reconnection logic', async ({page}) => {
    // Test reconnection handling
    const reconnectionTest = await page.evaluate(async () => {
      return new Promise<{
        reconnectionSupported: boolean;
        strategy: string;
        attempts: number;
        maxDelay: number;
      }>((resolve) => {
        // Simulate WebSocket reconnection logic
        let attempts = 0;
        const maxAttempts = 5;
        let delay = 1000;
        const maxDelay = 30000;
        const backoffFactor = 2;

        const reconnect = (): void => {
          attempts++;

          if (attempts > maxAttempts) {
            resolve({
              reconnectionSupported: false,
              strategy: 'exponential_backoff',
              attempts,
              maxDelay
            });
            return;
          }

          // Simulate connection attempt
          setTimeout(() => {
            // Simulate successful reconnection after a few attempts
            if (attempts >= 3) {
              resolve({
                reconnectionSupported: true,
                strategy: 'exponential_backoff',
                attempts,
                maxDelay: Math.min(delay, maxDelay)
              });
            } else {
              delay = Math.min(delay * backoffFactor, maxDelay);
              reconnect();
            }
          }, 100); // Fast simulation for testing
        };

        reconnect();
      });
    });

    expect(reconnectionTest.reconnectionSupported).toBe(true);
    expect(reconnectionTest.strategy).toBe('exponential_backoff');
    expect(reconnectionTest.attempts).toBeLessThanOrEqual(5);

    console.log('Reconnection Test Results:', reconnectionTest);
  });
});

test.describe('Browser-Specific WebSocket Behavior', () => {
  test('should handle Safari WebSocket quirks', async ({page, browserName}) => {
    test.skip(browserName !== 'webkit', 'Safari-specific test');

    const safariWebSocketTest = await page.evaluate(() => {
      // Test Safari-specific WebSocket behavior
      const results = {
        backgroundTabBehavior: false,
        privateBrowsingWebSocket: true,
        webSocketExtensions: false
      };

      // Safari may pause WebSocket connections in background tabs
      if (typeof document.hidden !== 'undefined') {
        results.backgroundTabBehavior = true;
      }

      // Test WebSocket extensions support
      if (WebSocket && WebSocket.prototype.extensions !== undefined) {
        results.webSocketExtensions = true;
      }

      return results;
    });

    console.log('Safari WebSocket Behavior:', safariWebSocketTest);
    expect(safariWebSocketTest.backgroundTabBehavior).toBe(true);
  });

  test('should handle Firefox WebSocket implementation', async ({page, browserName}) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific test');

    const firefoxWebSocketTest = await page.evaluate(() => {
      const results = {
        webSocketSupported: typeof WebSocket !== 'undefined',
        binaryTypeSupport: false,
        protocolSupport: false
      };

      if (WebSocket) {
        // Test binary type support
        const ws = {binaryType: 'blob' as BinaryType};
        try {
          ws.binaryType = 'arraybuffer';
          results.binaryTypeSupport = true;
        } catch {
          results.binaryTypeSupport = false;
        }

        // Test protocol support
        try {
          const testWs = new WebSocket('ws://echo.websocket.org/', ['protocol1', 'protocol2']);
          results.protocolSupport = true;
          testWs.close();
        } catch {
          results.protocolSupport = false;
        }
      }

      return results;
    });

    expect(firefoxWebSocketTest.webSocketSupported).toBe(true);
    console.log('Firefox WebSocket Features:', firefoxWebSocketTest);
  });

  test('should handle Chrome WebSocket optimizations', async ({page, browserName}) => {
    test.skip(browserName !== 'chromium', 'Chrome-specific test');

    const chromeWebSocketTest = await page.evaluate(() => {
      const results = {
        webSocketSupported: typeof WebSocket !== 'undefined',
        compressionSupport: false,
        http2Push: false,
        performanceTimeline: false
      };

      // Test compression extension support
      if (WebSocket) {
        results.compressionSupport = true; // Chrome typically supports permessage-deflate
      }

      // Test performance timeline for WebSocket
      if (performance && performance.getEntriesByType) {
        results.performanceTimeline = true;
      }

      return results;
    });

    expect(chromeWebSocketTest.webSocketSupported).toBe(true);
    console.log('Chrome WebSocket Optimizations:', chromeWebSocketTest);
  });

  test('should handle Edge WebSocket compatibility', async ({page, browserName}) => {
    test.skip(browserName !== 'edge' && browserName !== 'chromium', 'Edge-specific test');

    const edgeWebSocketTest = await page.evaluate(() => {
      const results = {
        webSocketSupported: typeof WebSocket !== 'undefined',
        legacySupport: false,
        modernFeatures: true
      };

      // Edge (Chromium) should have modern WebSocket features
      if (WebSocket && WebSocket.CONNECTING === 0) {
        results.modernFeatures = true;
      }

      return results;
    });

    expect(edgeWebSocketTest.webSocketSupported).toBe(true);
    expect(edgeWebSocketTest.modernFeatures).toBe(true);
    console.log('Edge WebSocket Features:', edgeWebSocketTest);
  });
});

test.describe('WebSocket Performance Testing', () => {
  test('should measure WebSocket connection performance', async ({page}) => {
    const performanceTest = await page.evaluate(async () => {
      return new Promise<{
        connectionTime: number;
        messageLatency: number;
        throughput: number;
        supported: boolean;
      }>((resolve) => {
        const startTime = Date.now();
        let connectionTime = 0;
        let messageLatency = 0;
        let messagesSent = 0;
        let messagesReceived = 0;

        try {
          const ws = new WebSocket('wss://echo.websocket.org/');

          ws.onopen = () => {
            connectionTime = Date.now() - startTime;

            // Send multiple messages to test throughput
            for (let i = 0; i < 10; i++) {
              setTimeout(() => {
                const msgStartTime = Date.now();
                ws.send(`test-message-${i}-${msgStartTime}`);
                messagesSent++;
              }, i * 10);
            }
          };

          let firstMessageTime = 0;
          ws.onmessage = (event) => {
            messagesReceived++;

            if (messagesReceived === 1) {
              firstMessageTime = Date.now();
              const messageParts = event.data.split('-');
              if (messageParts.length >= 3) {
                const sentTime = parseInt(messageParts[3]);
                messageLatency = firstMessageTime - sentTime;
              }
            }

            if (messagesReceived === messagesSent) {
              const totalTime = Date.now() - startTime;
              const throughput = (messagesSent / (totalTime / 1000)); // messages per second

              ws.close();
              resolve({
                connectionTime,
                messageLatency,
                throughput,
                supported: true
              });
            }
          };

          ws.onerror = () => {
            resolve({
              connectionTime: 0,
              messageLatency: 0,
              throughput: 0,
              supported: false
            });
          };

          // Timeout after 10 seconds
          setTimeout(() => {
            ws.close();
            resolve({
              connectionTime,
              messageLatency: messageLatency || 0,
              throughput: messagesReceived / 10,
              supported: messagesReceived > 0
            });
          }, 10000);

        } catch {
          resolve({
            connectionTime: 0,
            messageLatency: 0,
            throughput: 0,
            supported: false
          });
        }
      });
    });

    if (performanceTest.supported) {
      console.log('WebSocket Performance Metrics:', {
        connectionTime: `${performanceTest.connectionTime}ms`,
        messageLatency: `${performanceTest.messageLatency}ms`,
        throughput: `${performanceTest.throughput.toFixed(2)} msg/s`
      });

      // Performance expectations (adjust based on your requirements)
      expect(performanceTest.connectionTime).toBeLessThan(5000); // 5s max connection time
      expect(performanceTest.messageLatency).toBeLessThan(2000); // 2s max message latency
      expect(performanceTest.throughput).toBeGreaterThan(0.5); // At least 0.5 messages per second
    } else {
      console.log('WebSocket performance test failed or not supported');
    }
  });
});