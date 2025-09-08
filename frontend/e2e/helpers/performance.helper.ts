/**
 * Performance and Load Testing Helper Utilities for FocusHive E2E Tests
 */

import { Page, BrowserContext, Browser, expect } from '@playwright/test';
import { TEST_USERS, SELECTORS, TIMEOUTS } from './test-data';
import { AuthHelper } from './auth.helper';

export interface PerformanceMetrics {
  timestamp: number;
  loadTime: number;
  domContentLoaded: number;
  firstContentfulPaint: number;
  largestContentfulPaint: number;
  firstInputDelay: number;
  cumulativeLayoutShift: number;
  timeToFirstByte: number;
  memoryUsage?: MemoryUsage;
  networkRequests: number;
  responseTime: number;
}

export interface MemoryUsage {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
}

export interface PerformanceMemory {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
}

export interface LayoutShiftEntry extends PerformanceEntry {
  value: number;
  hadRecentInput: boolean;
}

export interface FirstInputEntry extends PerformanceEntry {
  processingStart: number;
}

export interface ExtendedPerformance extends Performance {
  memory?: PerformanceMemory;
}

export interface WebSocketMessage {
  type: string;
  id?: string;
  timestamp?: number;
  data?: string;
}

export interface ApiPayload {
  [key: string]: string | number | boolean | object | null | undefined;
}

export interface LoadTestResult {
  concurrentUsers: number;
  testDuration: number;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  averageResponseTime: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
  throughput: number;
  errorRate: number;
  errors: Array<{ error: string; count: number }>;
}

export interface WebSocketMetrics {
  connectionTime: number;
  messageLatency: number;
  messagesDelivered: number;
  messagesLost: number;
  reconnectCount: number;
}

export interface StressTestResult {
  breakingPoint: number;
  recoveryTime: number;
  degradationStarted: number;
  systemRecovered: boolean;
  resourceUtilization: {
    cpu: number;
    memory: number;
    network: number;
  };
}

export class PerformanceHelper {
  private performanceObserver?: PerformanceObserver;
  private metrics: PerformanceMetrics[] = [];
  private loadTestResults: LoadTestResult[] = [];

  constructor(private page: Page) {}

  /**
   * Collect Core Web Vitals and performance metrics
   */
  async collectPerformanceMetrics(url: string): Promise<PerformanceMetrics> {
    const navigationStartTime = Date.now();
    
    // Start measuring performance
    await this.page.goto(url);
    
    // Wait for page to be fully loaded
    await this.page.waitForLoadState('networkidle');
    
    // Get performance metrics using browser APIs
    const metrics = await this.page.evaluate(() => {
      return new Promise<PerformanceMetrics>((resolve) => {
        const getMemoryUsage = (): MemoryUsage | undefined => {
          const memory = (performance as ExtendedPerformance).memory;
          if (memory) {
            return {
              usedJSHeapSize: memory.usedJSHeapSize,
              totalJSHeapSize: memory.totalJSHeapSize,
              jsHeapSizeLimit: memory.jsHeapSizeLimit
            };
          }
          return undefined;
        };

        // Get navigation timing
        const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        
        // Get paint timing
        const paintEntries = performance.getEntriesByType('paint');
        const fcp = paintEntries.find(entry => entry.name === 'first-contentful-paint');
        
        // Calculate metrics
        const metrics: PerformanceMetrics = {
          timestamp: Date.now(),
          loadTime: navigation.loadEventEnd - navigation.fetchStart,
          domContentLoaded: navigation.domContentLoadedEventEnd - navigation.fetchStart,
          firstContentfulPaint: fcp?.startTime || 0,
          largestContentfulPaint: 0, // Will be updated by LCP observer
          firstInputDelay: 0, // Will be updated by FID observer
          cumulativeLayoutShift: 0, // Will be updated by CLS observer
          timeToFirstByte: navigation.responseStart - navigation.fetchStart,
          memoryUsage: getMemoryUsage(),
          networkRequests: performance.getEntriesByType('resource').length,
          responseTime: navigation.responseEnd - navigation.requestStart
        };

        // Observe LCP
        const lcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const lastEntry = entries[entries.length - 1];
          metrics.largestContentfulPaint = lastEntry.startTime;
        });
        lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true });

        // Observe CLS
        let clsValue = 0;
        const clsObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            const layoutShiftEntry = entry as LayoutShiftEntry;
            if (!layoutShiftEntry.hadRecentInput) {
              clsValue += layoutShiftEntry.value;
            }
          }
          metrics.cumulativeLayoutShift = clsValue;
        });
        clsObserver.observe({ type: 'layout-shift', buffered: true });

        // Observe FID
        const fidObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const firstEntry = entries[0] as FirstInputEntry;
          metrics.firstInputDelay = firstEntry.processingStart - firstEntry.startTime;
        });
        fidObserver.observe({ type: 'first-input', buffered: true });

        // Wait a bit for observers to collect data
        setTimeout(() => {
          lcpObserver.disconnect();
          clsObserver.disconnect();
          fidObserver.disconnect();
          resolve(metrics);
        }, 2000);
      });
    });

    this.metrics.push(metrics);
    return metrics;
  }

  /**
   * Simulate concurrent users for load testing
   */
  async simulateConcurrentUsers(
    browser: Browser,
    userCount: number,
    testDuration: number,
    testScenario: (page: Page, userIndex: number) => Promise<void>
  ): Promise<LoadTestResult> {
    const startTime = Date.now();
    const results: Array<{ success: boolean; responseTime: number; error?: string }> = [];
    const contexts: BrowserContext[] = [];
    const pages: Page[] = [];

    try {
      // Create browser contexts and pages for concurrent users
      for (let i = 0; i < userCount; i++) {
        const context = await browser.newContext();
        const page = await context.newPage();
        contexts.push(context);
        pages.push(page);
      }

      // Run concurrent user simulations
      const userPromises = pages.map(async (page, userIndex) => {
        const userStartTime = Date.now();
        try {
          await testScenario(page, userIndex);
          const responseTime = Date.now() - userStartTime;
          results.push({ success: true, responseTime });
        } catch (error) {
          const responseTime = Date.now() - userStartTime;
          results.push({ 
            success: false, 
            responseTime,
            error: error instanceof Error ? error.message : 'Unknown error'
          });
        }
      });

      // Wait for all users to complete or timeout
      await Promise.allSettled(userPromises);

      // Calculate results
      const totalRequests = results.length;
      const successfulRequests = results.filter(r => r.success).length;
      const failedRequests = totalRequests - successfulRequests;
      const responseTimes = results.map(r => r.responseTime);
      const averageResponseTime = responseTimes.reduce((a, b) => a + b, 0) / totalRequests;
      
      // Calculate percentiles
      const sortedTimes = responseTimes.sort((a, b) => a - b);
      const p95Index = Math.floor(sortedTimes.length * 0.95);
      const p99Index = Math.floor(sortedTimes.length * 0.99);
      const p95ResponseTime = sortedTimes[p95Index] || 0;
      const p99ResponseTime = sortedTimes[p99Index] || 0;

      const actualTestDuration = Date.now() - startTime;
      const throughput = (successfulRequests / actualTestDuration) * 1000; // requests per second
      const errorRate = (failedRequests / totalRequests) * 100;

      // Group errors
      const errorCounts: { [key: string]: number } = {};
      results.forEach(r => {
        if (!r.success && r.error) {
          errorCounts[r.error] = (errorCounts[r.error] || 0) + 1;
        }
      });

      const loadTestResult: LoadTestResult = {
        concurrentUsers: userCount,
        testDuration: actualTestDuration,
        totalRequests,
        successfulRequests,
        failedRequests,
        averageResponseTime,
        p95ResponseTime,
        p99ResponseTime,
        throughput,
        errorRate,
        errors: Object.entries(errorCounts).map(([error, count]) => ({ error, count }))
      };

      this.loadTestResults.push(loadTestResult);
      return loadTestResult;

    } finally {
      // Clean up contexts
      await Promise.all(contexts.map(context => context.close()));
    }
  }

  /**
   * Test WebSocket performance and latency
   */
  async testWebSocketPerformance(url: string, messageCount: number = 100): Promise<WebSocketMetrics> {
    const startTime = Date.now();
    const connectionTime = 0;
    const messagesDelivered = 0;
    const messagesLost = 0;
    const reconnectCount = 0;
    const latencies: number[] = [];

    const wsMetrics = await this.page.evaluate(async ({ wsUrl, msgCount }: { wsUrl: string; msgCount: number }) => {
      return new Promise<WebSocketMetrics>((resolve, reject) => {
        const connectStart = Date.now();
        const ws = new WebSocket(wsUrl);
        let connectionTime = 0;
        let messagesDelivered = 0;
        let messagesLost = 0;
        let reconnectCount = 0;
        const latencies: number[] = [];
        const messageTimestamps: { [key: string]: number } = {};

        ws.onopen = () => {
          connectionTime = Date.now() - connectStart;
          
          // Send test messages
          for (let i = 0; i < msgCount; i++) {
            const messageId = `test-${i}`;
            const timestamp = Date.now();
            messageTimestamps[messageId] = timestamp;
            
            try {
              ws.send(JSON.stringify({
                type: 'performance-test',
                id: messageId,
                timestamp,
                data: `Test message ${i}`
              }));
            } catch (error) {
              messagesLost++;
            }
          }
        };

        ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data);
            if (message.type === 'performance-test-response' && message.id) {
              const sendTime = messageTimestamps[message.id];
              if (sendTime) {
                const latency = Date.now() - sendTime;
                latencies.push(latency);
                messagesDelivered++;
                delete messageTimestamps[message.id];
              }
            }
          } catch (error) {
            // Ignore non-JSON messages
          }
        };

        ws.onclose = (event) => {
          if (!event.wasClean) {
            reconnectCount++;
          }
        };

        ws.onerror = () => {
          reconnectCount++;
        };

        // Wait for all messages to be processed
        setTimeout(() => {
          messagesLost += Object.keys(messageTimestamps).length;
          const averageLatency = latencies.length > 0 
            ? latencies.reduce((a, b) => a + b, 0) / latencies.length 
            : 0;

          ws.close();
          resolve({
            connectionTime,
            messageLatency: averageLatency,
            messagesDelivered,
            messagesLost,
            reconnectCount
          });
        }, 5000);
      });
    }, { wsUrl: url, msgCount: messageCount });

    return wsMetrics;
  }

  /**
   * Run stress test to find breaking point
   */
  async runStressTest(
    browser: Browser,
    testScenario: (page: Page, userIndex: number) => Promise<void>,
    maxUsers: number = 1000,
    incrementSize: number = 50
  ): Promise<StressTestResult> {
    let currentUsers = incrementSize;
    let degradationStarted = 0;
    let breakingPoint = 0;
    let systemRecovered = false;
    
    const resourceUtilization = { cpu: 0, memory: 0, network: 0 };
    
    while (currentUsers <= maxUsers) {
      console.log(`Testing with ${currentUsers} concurrent users...`);
      
      const result = await this.simulateConcurrentUsers(
        browser,
        currentUsers,
        30000, // 30 seconds
        testScenario
      );
      
      // Check for degradation (error rate > 5% or avg response time > 5000ms)
      if ((result.errorRate > 5 || result.averageResponseTime > 5000) && degradationStarted === 0) {
        degradationStarted = currentUsers;
        console.log(`Performance degradation detected at ${currentUsers} users`);
      }
      
      // Check for breaking point (error rate > 50% or avg response time > 30000ms)
      if (result.errorRate > 50 || result.averageResponseTime > 30000) {
        breakingPoint = currentUsers;
        console.log(`Breaking point reached at ${currentUsers} users`);
        break;
      }
      
      currentUsers += incrementSize;
      
      // Wait between test runs
      await this.page.waitForTimeout(5000);
    }
    
    // Test recovery by reducing load
    if (breakingPoint > 0) {
      const recoveryStartTime = Date.now();
      const recoveryUsers = Math.max(degradationStarted, incrementSize);
      
      console.log(`Testing recovery with ${recoveryUsers} users...`);
      const recoveryResult = await this.simulateConcurrentUsers(
        browser,
        recoveryUsers,
        30000,
        testScenario
      );
      
      const recoveryTime = Date.now() - recoveryStartTime;
      systemRecovered = recoveryResult.errorRate < 5 && recoveryResult.averageResponseTime < 5000;
      
      return {
        breakingPoint,
        recoveryTime,
        degradationStarted,
        systemRecovered,
        resourceUtilization
      };
    }
    
    return {
      breakingPoint: 0, // System didn't break within test limits
      recoveryTime: 0,
      degradationStarted,
      systemRecovered: true,
      resourceUtilization
    };
  }

  /**
   * Monitor memory usage over time for leak detection
   */
  async monitorMemoryUsage(duration: number = 300000): Promise<MemoryUsage[]> {
    const measurements: MemoryUsage[] = [];
    const startTime = Date.now();
    
    const interval = setInterval(async () => {
      try {
        const memory = await this.page.evaluate(() => {
          const mem = (performance as ExtendedPerformance).memory;
          if (mem) {
            return {
              usedJSHeapSize: mem.usedJSHeapSize,
              totalJSHeapSize: mem.totalJSHeapSize,
              jsHeapSizeLimit: mem.jsHeapSizeLimit
            };
          }
          return null;
        });
        
        if (memory) {
          measurements.push(memory);
        }
      } catch (error) {
        console.warn('Failed to collect memory usage:', error);
      }
    }, 1000); // Measure every second
    
    // Wait for test duration
    await new Promise(resolve => setTimeout(resolve, duration));
    clearInterval(interval);
    
    return measurements;
  }

  /**
   * Test API endpoint performance under load
   */
  async testApiPerformance(
    endpoint: string,
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    payload?: ApiPayload,
    concurrentRequests: number = 100
  ): Promise<{ averageResponseTime: number; successRate: number; errors: string[] }> {
    const results: Array<{ success: boolean; responseTime: number; error?: string }> = [];
    
    const requests = Array.from({ length: concurrentRequests }, async () => {
      const startTime = Date.now();
      
      try {
        const response = await this.page.request[method.toLowerCase() as 'get'](endpoint, {
          data: payload
        });
        
        const responseTime = Date.now() - startTime;
        const success = response.ok();
        
        results.push({
          success,
          responseTime,
          error: success ? undefined : `HTTP ${response.status()}: ${response.statusText()}`
        });
      } catch (error) {
        const responseTime = Date.now() - startTime;
        results.push({
          success: false,
          responseTime,
          error: error instanceof Error ? error.message : 'Unknown error'
        });
      }
    });
    
    await Promise.allSettled(requests);
    
    const successfulRequests = results.filter(r => r.success);
    const averageResponseTime = results.reduce((sum, r) => sum + r.responseTime, 0) / results.length;
    const successRate = (successfulRequests.length / results.length) * 100;
    const errors = results.filter(r => !r.success).map(r => r.error || 'Unknown error');
    
    // Remove duplicates manually to avoid Set iteration
    const uniqueErrors: string[] = [];
    for (const error of errors) {
      if (!uniqueErrors.includes(error)) {
        uniqueErrors.push(error);
      }
    }
    
    return {
      averageResponseTime,
      successRate,
      errors: uniqueErrors
    };
  }

  /**
   * Generate performance report
   */
  generatePerformanceReport(): {
    summary: {
      totalTests: number;
      averageLoadTime: number;
      averageLCP: number;
      averageFID: number;
      averageCLS: number;
    };
    metrics: PerformanceMetrics[];
    loadTests: LoadTestResult[];
  } {
    const totalTests = this.metrics.length;
    const averageLoadTime = this.metrics.reduce((sum, m) => sum + m.loadTime, 0) / totalTests;
    const averageLCP = this.metrics.reduce((sum, m) => sum + m.largestContentfulPaint, 0) / totalTests;
    const averageFID = this.metrics.reduce((sum, m) => sum + m.firstInputDelay, 0) / totalTests;
    const averageCLS = this.metrics.reduce((sum, m) => sum + m.cumulativeLayoutShift, 0) / totalTests;
    
    return {
      summary: {
        totalTests,
        averageLoadTime,
        averageLCP,
        averageFID,
        averageCLS
      },
      metrics: this.metrics,
      loadTests: this.loadTestResults
    };
  }

  /**
   * Validate performance against thresholds
   */
  validatePerformanceThresholds(metrics: PerformanceMetrics): {
    passed: boolean;
    failures: string[];
  } {
    const failures: string[] = [];
    
    // Core Web Vitals thresholds (good values)
    if (metrics.largestContentfulPaint > 2500) {
      failures.push(`LCP too high: ${metrics.largestContentfulPaint}ms (should be ≤ 2500ms)`);
    }
    
    if (metrics.firstInputDelay > 100) {
      failures.push(`FID too high: ${metrics.firstInputDelay}ms (should be ≤ 100ms)`);
    }
    
    if (metrics.cumulativeLayoutShift > 0.1) {
      failures.push(`CLS too high: ${metrics.cumulativeLayoutShift} (should be ≤ 0.1)`);
    }
    
    if (metrics.loadTime > 3000) {
      failures.push(`Load time too high: ${metrics.loadTime}ms (should be ≤ 3000ms)`);
    }
    
    if (metrics.timeToFirstByte > 600) {
      failures.push(`TTFB too high: ${metrics.timeToFirstByte}ms (should be ≤ 600ms)`);
    }
    
    return {
      passed: failures.length === 0,
      failures
    };
  }

  /**
   * Create standard user scenarios for load testing
   */
  static createUserScenarios() {
    return {
      // Basic navigation scenario
      basicNavigation: async (page: Page, userIndex: number) => {
        const authHelper = new AuthHelper(page);
        
        // Login
        await authHelper.navigateToLogin();
        await authHelper.loginWithValidUser(); // Use valid test user
        
        // Navigate to dashboard
        await page.goto('/dashboard');
        await page.waitForLoadState('networkidle');
        
        // Navigate to hive
        await page.goto('/hive');
        await page.waitForLoadState('networkidle');
        
        // Navigate to profile
        await page.goto('/profile');
        await page.waitForLoadState('networkidle');
      },
      
      // Hive creation and interaction scenario
      hiveInteraction: async (page: Page, userIndex: number) => {
        const authHelper = new AuthHelper(page);
        
        // Login
        await authHelper.navigateToLogin();
        await authHelper.loginWithValidUser();
        
        // Create hive
        await page.goto('/hive');
        await page.click('[data-testid="create-hive-button"]');
        await page.fill('[data-testid="hive-name-input"]', `Load Test Hive ${userIndex}`);
        await page.fill('[data-testid="hive-description-input"]', `Performance test hive ${userIndex}`);
        await page.click('[data-testid="create-hive-submit"]');
        await page.waitForLoadState('networkidle');
        
        // Start timer
        await page.click('[data-testid="start-timer-button"]');
        await page.waitForTimeout(2000);
        
        // Stop timer
        await page.click('[data-testid="stop-timer-button"]');
        await page.waitForLoadState('networkidle');
      },
      
      // Analytics and reports scenario
      analyticsReporting: async (page: Page, userIndex: number) => {
        const authHelper = new AuthHelper(page);
        
        // Login
        await authHelper.navigateToLogin();
        await authHelper.loginWithValidUser();
        
        // Navigate to analytics
        await page.goto('/analytics');
        await page.waitForLoadState('networkidle');
        
        // Apply filters
        await page.selectOption('[data-testid="time-range-select"]', 'last-7-days');
        await page.waitForLoadState('networkidle');
        
        // Export data
        await page.click('[data-testid="export-data-button"]');
        await page.waitForTimeout(3000);
      }
    };
  }
}

export default PerformanceHelper;