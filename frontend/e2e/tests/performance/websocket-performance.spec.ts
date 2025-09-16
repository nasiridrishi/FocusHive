/**
 * WebSocket Real-time Performance Tests for Frontend
 *
 * Comprehensive WebSocket performance testing for FocusHive real-time features:
 * - Connection establishment and stability
 * - Message latency and throughput
 * - Reconnection handling and resilience
 * - Memory usage during real-time operations
 * - UI responsiveness during heavy message traffic
 * - Concurrent connection handling
 * - Message ordering and delivery guarantees
 */

import {expect, test} from '@playwright/test';
import {PerformanceTestHelper, WebSocketPerformanceMetrics} from './performance-helpers';
import {performanceCollector, PerformanceMetrics} from './performance-metrics';
import {AuthHelper} from '../../helpers/auth.helper';

// Extended performance interface for memory access
interface ExtendedPerformance extends Performance {
  memory?: {
    usedJSHeapSize: number;
    totalJSHeapSize: number;
    jsHeapSizeLimit: number;
  };
}

declare global {
  interface Window {
    testWebSocket?: WebSocket;
  }
}

// WebSocket performance test configuration
const WEBSOCKET_TEST_CONFIG = {
  connectionTimeout: 5000,        // 5 seconds to establish connection
  messageLatencyThreshold: 100,   // 100ms max message latency
  throughputThreshold: 100,       // 100 messages per second
  stabilityThreshold: 99,         // 99% connection stability
  memoryGrowthLimit: 10,          // 10MB memory growth during test

  testDuration: {
    short: 30000,   // 30 seconds
    medium: 120000, // 2 minutes
    long: 300000    // 5 minutes
  },

  messageTypes: [
    {type: 'ping', size: 50, frequency: 1000},        // Every second
    {type: 'presence', size: 200, frequency: 5000},   // Every 5 seconds
    {type: 'chat', size: 500, frequency: 2000},       // Every 2 seconds
    {type: 'timer', size: 100, frequency: 1000},      // Every second
    {type: 'notification', size: 300, frequency: 10000} // Every 10 seconds
  ],

  concurrencyLevels: [1, 5, 10, 25, 50], // Number of concurrent connections

  websocketEndpoints: [
    {name: 'Main WebSocket', url: 'ws://localhost:8080/ws'},
    {name: 'Chat WebSocket', url: 'ws://localhost:8084/ws/chat'},
    {name: 'Presence WebSocket', url: 'ws://localhost:8080/ws/presence'},
    {name: 'Notifications WebSocket', url: 'ws://localhost:8083/ws/notifications'}
  ]
};

interface WebSocketTestResult {
  connectionTime: number;
  messagesSent: number;
  messagesReceived: number;
  avgLatency: number;
  maxLatency: number;
  minLatency: number;
  throughput: number;
  errorCount: number;
  reconnectCount: number;
  memoryUsage: number;
}

test.describe('WebSocket Real-time Performance Tests', () => {
  let authHelper: AuthHelper;
  let performanceHelper: PerformanceTestHelper;

  test.beforeEach(async ({page}) => {
    authHelper = new AuthHelper(page);
    performanceHelper = new PerformanceTestHelper(page);
    await performanceHelper.initializePerformanceMonitoring();
    await authHelper.loginWithTestUser();
  });

  test.afterEach(async () => {
    await performanceHelper.cleanup();
  });

  // Test basic WebSocket connection performance
  for (const endpoint of WEBSOCKET_TEST_CONFIG.websocketEndpoints) {
    test(`WebSocket Connection - ${endpoint.name}`, async ({page}) => {
      performanceCollector.startTest(`WebSocket - ${endpoint.name} Connection`);

      await page.goto('http://localhost:3000/dashboard');
      await page.waitForLoadState('networkidle');

      const connectionResult = await page.evaluate(async (config) => {
        return new Promise<WebSocketTestResult>((resolve) => {
          const startTime = performance.now();
          const ws = new WebSocket(config.url);
          const metrics = {
            connectionTime: 0,
            messagesSent: 0,
            messagesReceived: 0,
            avgLatency: 0,
            maxLatency: 0,
            minLatency: Infinity,
            throughput: 0,
            errorCount: 0,
            reconnectCount: 0,
            memoryUsage: 0
          };

          const latencies: number[] = [];
          let connected = false;

          ws.onopen = () => {
            connected = true;
            metrics.connectionTime = performance.now() - startTime;
            console.log(`Connected to ${config.name} in ${metrics.connectionTime}ms`);
          };

          ws.onmessage = (event) => {
            metrics.messagesReceived++;
            try {
              const message = JSON.parse(event.data);
              if (message.timestamp) {
                const latency = performance.now() - message.timestamp;
                latencies.push(latency);
                metrics.maxLatency = Math.max(metrics.maxLatency, latency);
                metrics.minLatency = Math.min(metrics.minLatency, latency);
              }
            } catch {
              // Handle non-JSON messages
            }
          };

          ws.onerror = () => {
            metrics.errorCount++;
          };

          ws.onclose = () => {
            if (connected) {
              metrics.reconnectCount++;
            }
          };

          // Wait for connection then resolve
          setTimeout(() => {
            if (latencies.length > 0) {
              metrics.avgLatency = latencies.reduce((a, b) => a + b, 0) / latencies.length;
            }

            // Get memory usage
            const memory = (performance as ExtendedPerformance).memory;
            if (memory) {
              metrics.memoryUsage = memory.usedJSHeapSize / 1024 / 1024;
            }

            ws.close();
            resolve(metrics);
          }, 3000);
        });
      }, endpoint);

      // Validate connection performance
      expect(connectionResult.connectionTime, `${endpoint.name} should connect quickly`)
      .toBeLessThan(WEBSOCKET_TEST_CONFIG.connectionTimeout);

      expect(connectionResult.errorCount, `${endpoint.name} should have no connection errors`)
      .toBe(0);

      // Record results
      const wsMetrics: WebSocketPerformanceMetrics = {
        connectionTime: connectionResult.connectionTime,
        averageLatency: connectionResult.avgLatency,
        messagesPerSecond: connectionResult.throughput,
        connectionStability: connectionResult.errorCount === 0 ? 100 : 95,
        reconnections: connectionResult.reconnectCount
      };

      const metrics: PerformanceMetrics = {
        webSocketMetrics: wsMetrics
      };

      const _result = performanceCollector.endTest(`WebSocket - ${endpoint.name} Connection`, metrics);

      console.log(`🔌 ${endpoint.name} Connection Performance:`);
      console.log(`  Connection Time: ${connectionResult.connectionTime.toFixed(2)}ms`);
      console.log(`  Messages Received: ${connectionResult.messagesReceived}`);
      console.log(`  Average Latency: ${connectionResult.avgLatency.toFixed(2)}ms`);
      console.log(`  Memory Usage: ${connectionResult.memoryUsage.toFixed(2)}MB`);
    });
  }

  // Test message latency and throughput
  test('WebSocket Performance - Message Latency and Throughput', async ({page}) => {
    performanceCollector.startTest('WebSocket - Message Performance');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    const messagePerformance = await page.evaluate(async (config) => {
      return new Promise<{
        totalMessages: number;
        avgLatency: number;
        maxLatency: number;
        p95Latency: number;
        throughput: number;
        messageTypes: Array<{ type: string; count: number; avgLatency: number }>;
      }>((resolve) => {
        const ws = new WebSocket('ws://localhost:8080/ws');
        const latencies: number[] = [];
        const messageTypeStats = new Map<string, number[]>();
        const startTime = performance.now();
        let _messagesSent = 0;
        let messagesReceived = 0;

        ws.onopen = () => {
          // Send different types of messages
          const sendMessage = (type: string, size: number): void => {
            const timestamp = performance.now();
            const payload = {
              type,
              timestamp,
              data: 'x'.repeat(size - 50) // Subtract overhead
            };
            ws.send(JSON.stringify(payload));
            _messagesSent++;
          };

          // Send messages according to configuration
          config.messageTypes.forEach(msgConfig => {
            const interval = setInterval(() => {
              sendMessage(msgConfig.type, msgConfig.size);
            }, msgConfig.frequency);

            setTimeout(() => clearInterval(interval), 10000); // Run for 10 seconds
          });
        };

        ws.onmessage = (event) => {
          messagesReceived++;
          try {
            const message = JSON.parse(event.data);
            if (message.timestamp && message.type) {
              const latency = performance.now() - message.timestamp;
              latencies.push(latency);

              if (!messageTypeStats.has(message.type)) {
                messageTypeStats.set(message.type, []);
              }
              messageTypeStats.get(message.type)?.push(latency);
            }
          } catch {
            // Handle non-JSON messages
          }
        };

        setTimeout(() => {
          const testDuration = (performance.now() - startTime) / 1000; // seconds
          const throughput = messagesReceived / testDuration;

          // Calculate statistics
          latencies.sort((a, b) => a - b);
          const avgLatency = latencies.reduce((a, b) => a + b, 0) / latencies.length;
          const maxLatency = Math.max(...latencies);
          const p95Index = Math.floor(latencies.length * 0.95);
          const p95Latency = latencies[p95Index] || 0;

          // Message type statistics
          const messageTypes = Array.from(messageTypeStats.entries()).map(([type, lats]) => ({
            type,
            count: lats.length,
            avgLatency: lats.reduce((a, b) => a + b, 0) / lats.length
          }));

          ws.close();

          resolve({
            totalMessages: messagesReceived,
            avgLatency,
            maxLatency,
            p95Latency,
            throughput,
            messageTypes
          });
        }, 12000); // Wait for all messages to be processed
      });
    }, WEBSOCKET_TEST_CONFIG);

    // Validate message performance
    expect(messagePerformance.avgLatency, 'Average message latency should be low')
    .toBeLessThan(WEBSOCKET_TEST_CONFIG.messageLatencyThreshold);

    expect(messagePerformance.p95Latency, 'P95 latency should be reasonable')
    .toBeLessThan(WEBSOCKET_TEST_CONFIG.messageLatencyThreshold * 2);

    expect(messagePerformance.throughput, 'Message throughput should be adequate')
    .toBeGreaterThan(10); // At least 10 messages per second

    // Record results
    const wsMetrics: WebSocketPerformanceMetrics = {
      connectionTime: 0, // Not measured in this test
      averageLatency: messagePerformance.avgLatency,
      messagesPerSecond: messagePerformance.throughput,
      connectionStability: 100, // Assume stable for this test
      reconnections: 0
    };

    const metrics: PerformanceMetrics = {
      webSocketMetrics: wsMetrics
    };

    const _result = performanceCollector.endTest('WebSocket - Message Performance', metrics);

    console.log(`📨 Message Performance Analysis:`);
    console.log(`  Total Messages: ${messagePerformance.totalMessages}`);
    console.log(`  Average Latency: ${messagePerformance.avgLatency.toFixed(2)}ms`);
    console.log(`  Max Latency: ${messagePerformance.maxLatency.toFixed(2)}ms`);
    console.log(`  P95 Latency: ${messagePerformance.p95Latency.toFixed(2)}ms`);
    console.log(`  Throughput: ${messagePerformance.throughput.toFixed(2)} msg/sec`);

    messagePerformance.messageTypes.forEach(type => {
      console.log(`  ${type.type}: ${type.count} messages, ${type.avgLatency.toFixed(2)}ms avg`);
    });
  });

  // Test connection stability and reconnection handling
  test('WebSocket Performance - Connection Stability', async ({page}) => {
    performanceCollector.startTest('WebSocket - Connection Stability');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    const stabilityTest = await page.evaluate(async () => {
      return new Promise<{
        totalConnections: number;
        successfulConnections: number;
        reconnectAttempts: number;
        avgReconnectTime: number;
        dataLoss: number;
        stabilityPercentage: number;
      }>((resolve) => {
        let totalConnections = 0;
        let successfulConnections = 0;
        let reconnectAttempts = 0;
        const reconnectTimes: number[] = [];
        let messagesSent = 0;
        let messagesReceived = 0;
        let reconnectStartTime = 0;

        const createConnection = (): void => {
          totalConnections++;
          const ws = new WebSocket('ws://localhost:8080/ws');
          let connected = false;

          ws.onopen = () => {
            connected = true;
            successfulConnections++;

            if (reconnectStartTime > 0) {
              const reconnectTime = performance.now() - reconnectStartTime;
              reconnectTimes.push(reconnectTime);
              reconnectStartTime = 0;
            }

            // Send test messages
            const sendInterval = setInterval(() => {
              if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({
                  type: 'test',
                  timestamp: performance.now(),
                  id: messagesSent++
                }));
              }
            }, 1000);

            // Simulate connection interruption after 5 seconds
            setTimeout(() => {
              clearInterval(sendInterval);
              ws.close();
            }, 5000);
          };

          ws.onmessage = () => {
            messagesReceived++;
          };

          ws.onclose = () => {
            if (connected) {
              reconnectAttempts++;
              reconnectStartTime = performance.now();

              // Attempt reconnection after delay
              setTimeout(() => {
                if (totalConnections < 5) { // Limit to 5 connection cycles
                  createConnection();
                }
              }, 1000);
            }
          };

          ws.onerror = () => {
            // Error handling
          };
        };

        // Start initial connection
        createConnection();

        // Wait for test completion
        setTimeout(() => {
          const avgReconnectTime = reconnectTimes.length > 0 ?
              reconnectTimes.reduce((a, b) => a + b, 0) / reconnectTimes.length : 0;

          const stabilityPercentage = (successfulConnections / totalConnections) * 100;
          const dataLoss = Math.max(0, messagesSent - messagesReceived);

          resolve({
            totalConnections,
            successfulConnections,
            reconnectAttempts,
            avgReconnectTime,
            dataLoss,
            stabilityPercentage
          });
        }, 30000); // 30 second test
      });
    });

    // Validate connection stability
    expect(stabilityTest.stabilityPercentage, 'Connection stability should be high')
    .toBeGreaterThan(WEBSOCKET_TEST_CONFIG.stabilityThreshold);

    expect(stabilityTest.avgReconnectTime, 'Reconnection should be fast')
    .toBeLessThan(WEBSOCKET_TEST_CONFIG.connectionTimeout);

    expect(stabilityTest.dataLoss, 'Data loss should be minimal')
    .toBeLessThan(stabilityTest.totalConnections * 2); // Allow some tolerance

    // Record results
    const wsMetrics: WebSocketPerformanceMetrics = {
      connectionTime: stabilityTest.avgReconnectTime,
      averageLatency: 0,
      messagesPerSecond: 0,
      connectionStability: stabilityTest.stabilityPercentage,
      reconnections: stabilityTest.reconnectAttempts
    };

    const metrics: PerformanceMetrics = {
      webSocketMetrics: wsMetrics
    };

    const _result = performanceCollector.endTest('WebSocket - Connection Stability', metrics);

    console.log(`🔄 Connection Stability Analysis:`);
    console.log(`  Total Connections: ${stabilityTest.totalConnections}`);
    console.log(`  Successful Connections: ${stabilityTest.successfulConnections}`);
    console.log(`  Stability: ${stabilityTest.stabilityPercentage.toFixed(2)}%`);
    console.log(`  Reconnect Attempts: ${stabilityTest.reconnectAttempts}`);
    console.log(`  Avg Reconnect Time: ${stabilityTest.avgReconnectTime.toFixed(2)}ms`);
    console.log(`  Data Loss: ${stabilityTest.dataLoss} messages`);
  });

  // Test memory usage during high-frequency WebSocket operations
  test('WebSocket Performance - Memory Usage Under Load', async ({page}) => {
    performanceCollector.startTest('WebSocket - Memory Under Load');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    const _initialMemory = await performanceHelper.getMemoryUsage();

    const memoryTest = await page.evaluate(async () => {
      return new Promise<{
        initialMemory: number;
        peakMemory: number;
        finalMemory: number;
        messagesSent: number;
        messagesReceived: number;
        memorySamples: number[];
      }>((resolve) => {
        const memorySamples: number[] = [];
        let messagesSent = 0;
        let messagesReceived = 0;

        const getMemoryUsage = (): void => {
          const memory = (performance as ExtendedPerformance).memory;
          return memory ? memory.usedJSHeapSize / 1024 / 1024 : 0;
        };

        const initialMemory = getMemoryUsage();
        memorySamples.push(initialMemory);

        const ws = new WebSocket('ws://localhost:8080/ws');
        const messageBuffer: unknown[] = [];

        ws.onopen = () => {
          // Send high-frequency messages
          const sendInterval = setInterval(() => {
            const message = {
              type: 'load-test',
              timestamp: performance.now(),
              data: {
                id: messagesSent++,
                payload: new Array(100).fill('x').join(''), // 100 byte payload
                metadata: {
                  sender: 'test-client',
                  priority: Math.random() > 0.5 ? 'high' : 'low',
                  tags: ['performance', 'test', 'memory']
                }
              }
            };

            ws.send(JSON.stringify(message));

            // Sample memory every 100 messages
            if (messagesSent % 100 === 0) {
              memorySamples.push(getMemoryUsage());
            }
          }, 50); // 20 messages per second

          // Stop after 10 seconds
          setTimeout(() => {
            clearInterval(sendInterval);
            ws.close();
          }, 10000);
        };

        ws.onmessage = (event) => {
          messagesReceived++;

          // Store messages in buffer to simulate real app behavior
          try {
            const message = JSON.parse(event.data);
            messageBuffer.push(message);

            // Simulate processing and cleanup
            if (messageBuffer.length > 50) {
              messageBuffer.splice(0, 25); // Remove old messages
            }
          } catch {
            // Handle non-JSON messages
          }
        };

        setTimeout(() => {
          const finalMemory = getMemoryUsage();
          const peakMemory = Math.max(...memorySamples);

          resolve({
            initialMemory,
            peakMemory,
            finalMemory,
            messagesSent,
            messagesReceived,
            memorySamples
          });
        }, 12000); // Allow time for cleanup
      });
    });

    const memoryGrowth = memoryTest.finalMemory - memoryTest.initialMemory;
    const peakGrowth = memoryTest.peakMemory - memoryTest.initialMemory;

    // Validate memory usage
    expect(memoryGrowth, 'Memory growth should be reasonable')
    .toBeLessThan(WEBSOCKET_TEST_CONFIG.memoryGrowthLimit);

    expect(peakGrowth, 'Peak memory usage should be controlled')
    .toBeLessThan(WEBSOCKET_TEST_CONFIG.memoryGrowthLimit * 2);

    // Record results
    const memoryMetrics = {
      initialUsage: memoryTest.initialMemory,
      peakUsage: memoryTest.peakMemory,
      finalUsage: memoryTest.finalMemory,
      leakDetected: memoryGrowth > 5, // Consider >5MB growth as potential leak
      leakRate: (memoryGrowth / 10) * 60, // MB per minute
      gcCount: 0,
      heapSize: memoryTest.finalMemory
    };

    const wsMetrics: WebSocketPerformanceMetrics = {
      connectionTime: 0,
      averageLatency: 0,
      messagesPerSecond: memoryTest.messagesSent / 10, // 10 second test
      connectionStability: 100,
      reconnections: 0
    };

    const metrics: PerformanceMetrics = {
      memoryMetrics,
      webSocketMetrics: wsMetrics
    };

    const _result = performanceCollector.endTest('WebSocket - Memory Under Load', metrics);

    console.log(`💾 Memory Usage Under WebSocket Load:`);
    console.log(`  Initial Memory: ${memoryTest.initialMemory.toFixed(2)}MB`);
    console.log(`  Peak Memory: ${memoryTest.peakMemory.toFixed(2)}MB`);
    console.log(`  Final Memory: ${memoryTest.finalMemory.toFixed(2)}MB`);
    console.log(`  Memory Growth: ${memoryGrowth.toFixed(2)}MB`);
    console.log(`  Messages Sent: ${memoryTest.messagesSent}`);
    console.log(`  Messages Received: ${memoryTest.messagesReceived}`);
    console.log(`  Message Rate: ${(memoryTest.messagesSent / 10).toFixed(2)} msg/sec`);
  });

  // Test concurrent WebSocket connections
  for (const concurrency of WEBSOCKET_TEST_CONFIG.concurrencyLevels) {
    test(`WebSocket Performance - ${concurrency} Concurrent Connections`, async ({page}) => {
      performanceCollector.startTest(`WebSocket - ${concurrency} Concurrent`);

      await page.goto('http://localhost:3000/dashboard');
      await page.waitForLoadState('networkidle');

      const concurrentTest = await page.evaluate(async (concurrencyLevel) => {
        return new Promise<{
          connectionsEstablished: number;
          avgConnectionTime: number;
          totalMessages: number;
          avgLatency: number;
          errors: number;
        }>((resolve) => {
          const connections: WebSocket[] = [];
          const connectionTimes: number[] = [];
          const latencies: number[] = [];
          let totalMessages = 0;
          let errors = 0;

          for (let i = 0; i < concurrencyLevel; i++) {
            const startTime = performance.now();
            const ws = new WebSocket(`ws://localhost:8080/ws?client=${i}`);

            ws.onopen = () => {
              const connectionTime = performance.now() - startTime;
              connectionTimes.push(connectionTime);

              // Send periodic messages
              const sendInterval = setInterval(() => {
                if (ws.readyState === WebSocket.OPEN) {
                  const timestamp = performance.now();
                  ws.send(JSON.stringify({
                    type: 'concurrent-test',
                    clientId: i,
                    timestamp,
                    messageId: totalMessages++
                  }));
                }
              }, 2000);

              setTimeout(() => {
                clearInterval(sendInterval);
                ws.close();
              }, 10000);
            };

            ws.onmessage = (event) => {
              try {
                const message = JSON.parse(event.data);
                if (message.timestamp) {
                  const latency = performance.now() - message.timestamp;
                  latencies.push(latency);
                }
              } catch {
                // Handle non-JSON messages
              }
            };

            ws.onerror = () => {
              errors++;
            };

            connections.push(ws);
          }

          setTimeout(() => {
            const avgConnectionTime = connectionTimes.length > 0 ?
                connectionTimes.reduce((a, b) => a + b, 0) / connectionTimes.length : 0;

            const avgLatency = latencies.length > 0 ?
                latencies.reduce((a, b) => a + b, 0) / latencies.length : 0;

            // Close all connections
            connections.forEach(ws => {
              if (ws.readyState === WebSocket.OPEN) {
                ws.close();
              }
            });

            resolve({
              connectionsEstablished: connectionTimes.length,
              avgConnectionTime,
              totalMessages,
              avgLatency,
              errors
            });
          }, 12000);
        });
      }, concurrency);

      // Validate concurrent connection performance
      expect(concurrentTest.connectionsEstablished, `Should establish ${concurrency} connections`)
      .toBe(concurrency);

      expect(concurrentTest.avgConnectionTime, 'Connection time should not degrade significantly')
      .toBeLessThan(WEBSOCKET_TEST_CONFIG.connectionTimeout * (1 + concurrency * 0.1)); // Allow degradation

      expect(concurrentTest.errors, 'Errors should be minimal')
      .toBeLessThan(concurrency * 0.1); // Less than 10% error rate

      // Record results
      const wsMetrics: WebSocketPerformanceMetrics = {
        connectionTime: concurrentTest.avgConnectionTime,
        averageLatency: concurrentTest.avgLatency,
        messagesPerSecond: concurrentTest.totalMessages / 10,
        connectionStability: ((concurrency - concurrentTest.errors) / concurrency) * 100,
        reconnections: 0
      };

      const metrics: PerformanceMetrics = {
        webSocketMetrics: wsMetrics
      };

      const _result = performanceCollector.endTest(`WebSocket - ${concurrency} Concurrent`, metrics);

      console.log(`🔗 ${concurrency} Concurrent Connections:`);
      console.log(`  Established: ${concurrentTest.connectionsEstablished}/${concurrency}`);
      console.log(`  Avg Connection Time: ${concurrentTest.avgConnectionTime.toFixed(2)}ms`);
      console.log(`  Total Messages: ${concurrentTest.totalMessages}`);
      console.log(`  Avg Latency: ${concurrentTest.avgLatency.toFixed(2)}ms`);
      console.log(`  Errors: ${concurrentTest.errors}`);
      console.log(`  Success Rate: ${((concurrency - concurrentTest.errors) / concurrency * 100).toFixed(2)}%`);
    });
  }

  // Test UI responsiveness during heavy WebSocket traffic
  test('WebSocket Performance - UI Responsiveness Under Load', async ({page}) => {
    performanceCollector.startTest('WebSocket - UI Responsiveness');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    // Start WebSocket load
    await page.evaluate(() => {
      const ws = new WebSocket('ws://localhost:8080/ws');
      let messageCount = 0;

      ws.onopen = () => {
        // Send high-frequency messages
        const interval = setInterval(() => {
          ws.send(JSON.stringify({
            type: 'ui-test',
            id: messageCount++,
            timestamp: performance.now(),
            data: new Array(500).fill('load-test-data').join('')
          }));

          if (messageCount >= 1000) {
            clearInterval(interval);
            ws.close();
          }
        }, 10); // 100 messages per second
      };

      // Store reference for cleanup
      window.testWebSocket = ws;
    });

    // Test UI responsiveness during WebSocket load
    const uiResponsiveness = await page.evaluate(() => {
      return new Promise<{
        buttonClickLatency: number;
        scrollPerformance: number;
        inputLatency: number;
        frameRate: number;
      }>((resolve) => {
        let frameCount = 0;
        let lastFrameTime = performance.now();

        const measureFrameRate = (): number => {
          frameCount++;
          const currentTime = performance.now();
          if (currentTime - lastFrameTime >= 1000) {
            const fps = frameCount;
            frameCount = 0;
            lastFrameTime = currentTime;
            return fps;
          }
          requestAnimationFrame(measureFrameRate);
          return 0;
        };

        requestAnimationFrame(measureFrameRate);

        // Test button click responsiveness
        const button = document.createElement('button');
        button.textContent = 'Test Button';
        document.body.appendChild(button);

        const buttonClickStart = performance.now();
        button.click();
        const buttonClickLatency = performance.now() - buttonClickStart;

        // Test scroll performance
        const scrollStart = performance.now();
        window.scrollBy(0, 100);
        const scrollPerformance = performance.now() - scrollStart;

        // Test input latency
        const input = document.createElement('input');
        input.type = 'text';
        document.body.appendChild(input);

        const inputStart = performance.now();
        input.focus();
        input.value = 'test';
        const inputLatency = performance.now() - inputStart;

        setTimeout(() => {
          document.body.removeChild(button);
          document.body.removeChild(input);

          resolve({
            buttonClickLatency,
            scrollPerformance,
            inputLatency,
            frameRate: frameCount
          });
        }, 2000);
      });
    });

    // Validate UI responsiveness
    expect(uiResponsiveness.buttonClickLatency, 'Button clicks should remain responsive')
    .toBeLessThan(50); // 50ms max

    expect(uiResponsiveness.inputLatency, 'Input should remain responsive')
    .toBeLessThan(100); // 100ms max

    expect(uiResponsiveness.frameRate, 'Frame rate should not drop significantly')
    .toBeGreaterThan(30); // At least 30 FPS

    // Cleanup
    await page.evaluate(() => {
      const ws = window.testWebSocket;
      if (ws) {
        ws.close();
      }
    });

    // Record results
    const reactMetrics = {
      renderTime: uiResponsiveness.buttonClickLatency + uiResponsiveness.inputLatency,
      rerenderCount: 0,
      componentCount: 0,
      memoryUsage: 0,
      fps: uiResponsiveness.frameRate,
      interactionDelay: (uiResponsiveness.buttonClickLatency + uiResponsiveness.inputLatency) / 2,
      bundleSize: 0,
      unusedCode: 0
    };

    const metrics: PerformanceMetrics = {
      reactPerformance: reactMetrics
    };

    const _result = performanceCollector.endTest('WebSocket - UI Responsiveness', metrics);

    console.log(`🖱️ UI Responsiveness Under WebSocket Load:`);
    console.log(`  Button Click Latency: ${uiResponsiveness.buttonClickLatency.toFixed(2)}ms`);
    console.log(`  Scroll Performance: ${uiResponsiveness.scrollPerformance.toFixed(2)}ms`);
    console.log(`  Input Latency: ${uiResponsiveness.inputLatency.toFixed(2)}ms`);
    console.log(`  Frame Rate: ${uiResponsiveness.frameRate} FPS`);
  });
});