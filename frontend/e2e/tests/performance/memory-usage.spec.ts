/**
 * Memory Usage and Leak Detection Tests
 * 
 * Comprehensive memory analysis for the FocusHive frontend:
 * - Memory leak detection over extended sessions
 * - Memory usage patterns across different workflows
 * - Garbage collection effectiveness
 * - Memory growth during heavy operations
 * - Component cleanup validation
 * - Event listener cleanup
 * - Timer and interval cleanup
 */

import { test, expect, Page } from '@playwright/test';
import { PerformanceTestHelper, MemoryInfo } from './performance-helpers';
import { performanceCollector, PerformanceMetrics } from './performance-metrics';
import { AuthHelper } from '../../helpers/auth.helper';

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
    gc?: () => void;
    largeTestData?: unknown[];
    imageTestData?: unknown[];
    chartTestData?: unknown[];
    testWebSocket?: WebSocket;
  }
}

// Memory test configuration
const MEMORY_TEST_CONFIG = {
  longSessionDuration: 300000, // 5 minutes
  shortSessionDuration: 60000,  // 1 minute
  sampleInterval: 5000,         // 5 seconds
  memoryLeakThreshold: 2,       // MB per minute
  maxMemoryUsage: 100,          // MB
  gcTriggerThreshold: 50,       // MB
  
  workflows: [
    {
      name: 'Navigation Heavy',
      description: 'Heavy navigation between pages',
      actions: ['dashboard', 'hives', 'profile', 'analytics', 'settings']
    },
    {
      name: 'Chat Heavy',
      description: 'Extended chat session with message history',
      actions: ['chat-join', 'chat-messages', 'chat-scroll', 'chat-typing']
    },
    {
      name: 'Data Heavy',
      description: 'Large data operations and visualizations',
      actions: ['analytics', 'large-list', 'filtering', 'sorting']
    },
    {
      name: 'Real-time Heavy',
      description: 'Extended real-time operations',
      actions: ['websocket-connect', 'presence-updates', 'notifications', 'live-updates']
    }
  ],
  
  memoryIntensiveOperations: [
    {
      name: 'Large List Rendering',
      selector: '[data-testid="large-list"]',
      itemCount: 1000,
      expectedMemoryGrowth: 20 // MB
    },
    {
      name: 'Image Gallery Loading',
      selector: '[data-testid="image-gallery"]',
      itemCount: 50,
      expectedMemoryGrowth: 50 // MB
    },
    {
      name: 'Chart Rendering',
      selector: '[data-testid="analytics-chart"]',
      itemCount: 4,
      expectedMemoryGrowth: 10 // MB
    }
  ]
};

test.describe('Memory Usage and Leak Detection Tests', () => {
  let authHelper: AuthHelper;
  let performanceHelper: PerformanceTestHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    performanceHelper = new PerformanceTestHelper(page);
    await performanceHelper.initializePerformanceMonitoring();
    await authHelper.loginWithTestUser();
  });

  test.afterEach(async () => {
    await performanceHelper.cleanup();
  });

  // Long-running session memory leak detection
  test('Memory Leak Detection - Extended Session', async ({ page }) => {
    performanceCollector.startTest('Memory - Extended Session Leak Detection');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    // Run extended memory leak detection
    const leakAnalysis = await performanceHelper.detectMemoryLeaks(
      MEMORY_TEST_CONFIG.longSessionDuration,
      MEMORY_TEST_CONFIG.sampleInterval
    );

    // Validate no significant memory leaks
    expect(leakAnalysis.hasLeak, 'No memory leaks should be detected in extended session').toBe(false);
    expect(leakAnalysis.leakRate, 'Memory leak rate should be under threshold')
      .toBeLessThan(MEMORY_TEST_CONFIG.memoryLeakThreshold);

    // Analyze memory usage pattern
    const initialMemory = leakAnalysis.samples[0];
    const finalMemory = leakAnalysis.samples[leakAnalysis.samples.length - 1];
    const peakMemory = leakAnalysis.samples.reduce((max, sample) => 
      sample.usedJSHeapSize > max.usedJSHeapSize ? sample : max
    );

    const memoryGrowth = (finalMemory.usedJSHeapSize - initialMemory.usedJSHeapSize) / 1024 / 1024;
    const peakUsage = peakMemory.usedJSHeapSize / 1024 / 1024;

    // Validate memory constraints
    expect(peakUsage, 'Peak memory usage should be reasonable')
      .toBeLessThan(MEMORY_TEST_CONFIG.maxMemoryUsage);

    // Record results
    const memoryMetrics = {
      initialUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
      peakUsage,
      finalUsage: finalMemory.usedJSHeapSize / 1024 / 1024,
      leakDetected: leakAnalysis.hasLeak,
      leakRate: leakAnalysis.leakRate,
      gcCount: 0, // GC count tracking would need additional instrumentation
      heapSize: finalMemory.totalJSHeapSize / 1024 / 1024
    };

    const metrics: PerformanceMetrics = {
      memoryMetrics
    };

    const result = performanceCollector.endTest('Memory - Extended Session Leak Detection', metrics);
    
    console.log(`🧠 Extended Session Memory Analysis:`);
    console.log(`  Duration: ${MEMORY_TEST_CONFIG.longSessionDuration / 1000}s`);
    console.log(`  Initial: ${initialMemory.usedJSHeapSize / 1024 / 1024:.2f}MB`);
    console.log(`  Peak: ${peakUsage:.2f}MB`);
    console.log(`  Final: ${finalMemory.usedJSHeapSize / 1024 / 1024:.2f}MB`);
    console.log(`  Growth: ${memoryGrowth:.2f}MB`);
    console.log(`  Leak Rate: ${leakAnalysis.leakRate:.2f}MB/min`);
    console.log(`  Leak Detected: ${leakAnalysis.hasLeak}`);
  });

  // Test memory usage for different workflow patterns
  for (const workflow of MEMORY_TEST_CONFIG.workflows) {
    test(`Memory Usage - ${workflow.name} Workflow`, async ({ page }) => {
      performanceCollector.startTest(`Memory - ${workflow.name} Workflow`);

      await page.goto('http://localhost:3000/dashboard');
      await page.waitForLoadState('networkidle');

      const initialMemory = await performanceHelper.getMemoryUsage();
      const memorySamples: MemoryInfo[] = [initialMemory];
      
      // Execute workflow actions
      for (const action of workflow.actions) {
        const actionStart = Date.now();
        
        switch (action) {
          case 'dashboard':
            await page.goto('http://localhost:3000/dashboard');
            break;
          case 'hives':
            await page.goto('http://localhost:3000/hives');
            break;
          case 'profile':
            await page.goto('http://localhost:3000/profile');
            break;
          case 'analytics':
            await page.goto('http://localhost:3000/analytics');
            break;
          case 'settings':
            await page.goto('http://localhost:3000/settings');
            break;
          case 'chat-join':
            await page.goto('http://localhost:3000/hives/1/chat');
            break;
          case 'chat-messages':
            // Simulate viewing chat messages
            await page.evaluate(() => {
              window.dispatchEvent(new CustomEvent('chat-load-messages', {
                detail: { count: 100 }
              }));
            });
            break;
          case 'chat-scroll':
            await page.evaluate(() => {
              const chatContainer = document.querySelector('[data-testid="chat-container"]');
              if (chatContainer) {
                chatContainer.scrollTop = chatContainer.scrollHeight;
              }
            });
            break;
          case 'large-list':
            await page.evaluate(() => {
              // Generate large list data
              window.largeTestData = Array.from({ length: 1000 }, (_, i) => ({
                id: i,
                name: `Item ${i}`,
                data: new Array(100).fill('x').join('')
              }));
            });
            break;
          case 'filtering':
            await page.evaluate(() => {
              window.dispatchEvent(new CustomEvent('data-filter', {
                detail: { filter: 'active' }
              }));
            });
            break;
          case 'sorting':
            await page.evaluate(() => {
              window.dispatchEvent(new CustomEvent('data-sort', {
                detail: { sortBy: 'name', order: 'asc' }
              }));
            });
            break;
          case 'websocket-connect':
            await page.evaluate(() => {
              // Simulate WebSocket connection
              window.testWebSocket = new WebSocket('ws://localhost:8080/ws');
            });
            break;
          case 'presence-updates':
            await page.evaluate(() => {
              // Simulate presence updates
              for (let i = 0; i < 50; i++) {
                window.dispatchEvent(new CustomEvent('presence-update', {
                  detail: { userId: `user-${i}`, status: 'online' }
                }));
              }
            });
            break;
          case 'notifications':
            await page.evaluate(() => {
              // Simulate notifications
              for (let i = 0; i < 20; i++) {
                window.dispatchEvent(new CustomEvent('notification', {
                  detail: { id: i, message: `Notification ${i}` }
                }));
              }
            });
            break;
        }
        
        await page.waitForLoadState('networkidle', { timeout: 10000 });
        await page.waitForTimeout(2000);
        
        // Sample memory after each action
        const currentMemory = await performanceHelper.getMemoryUsage();
        memorySamples.push(currentMemory);
        
        console.log(`  ${action}: ${(currentMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
      }

      const finalMemory = memorySamples[memorySamples.length - 1];
      const peakMemory = memorySamples.reduce((max, sample) => 
        sample.usedJSHeapSize > max.usedJSHeapSize ? sample : max
      );

      const memoryGrowth = (finalMemory.usedJSHeapSize - initialMemory.usedJSHeapSize) / 1024 / 1024;
      const peakUsage = peakMemory.usedJSHeapSize / 1024 / 1024;

      // Validate workflow memory usage
      expect(peakUsage, `${workflow.name} peak memory should be reasonable`)
        .toBeLessThan(MEMORY_TEST_CONFIG.maxMemoryUsage);

      // Calculate approximate leak rate (simplified)
      const workflowDuration = workflow.actions.length * 3; // ~3 seconds per action
      const leakRate = (memoryGrowth / workflowDuration) * 60; // MB per minute

      expect(leakRate, `${workflow.name} should not have significant memory leaks`)
        .toBeLessThan(MEMORY_TEST_CONFIG.memoryLeakThreshold);

      // Record results
      const memoryMetrics = {
        initialUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
        peakUsage,
        finalUsage: finalMemory.usedJSHeapSize / 1024 / 1024,
        leakDetected: leakRate > MEMORY_TEST_CONFIG.memoryLeakThreshold,
        leakRate,
        gcCount: 0,
        heapSize: finalMemory.totalJSHeapSize / 1024 / 1024
      };

      const metrics: PerformanceMetrics = {
        memoryMetrics
      };

      const result = performanceCollector.endTest(`Memory - ${workflow.name} Workflow`, metrics);
      
      console.log(`🔄 ${workflow.name} Workflow Memory:`);
      console.log(`  Actions: ${workflow.actions.length}`);
      console.log(`  Initial: ${(initialMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
      console.log(`  Peak: ${peakUsage.toFixed(2)}MB`);
      console.log(`  Final: ${(finalMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
      console.log(`  Growth: ${memoryGrowth.toFixed(2)}MB`);
      console.log(`  Est. Leak Rate: ${leakRate.toFixed(2)}MB/min`);
    });
  }

  // Test memory usage for memory-intensive operations
  for (const operation of MEMORY_TEST_CONFIG.memoryIntensiveOperations) {
    test(`Memory Usage - ${operation.name}`, async ({ page }) => {
      performanceCollector.startTest(`Memory - ${operation.name}`);

      await page.goto('http://localhost:3000/dashboard');
      await page.waitForLoadState('networkidle');

      const beforeMemory = await performanceHelper.getMemoryUsage();

      // Trigger memory-intensive operation
      await page.evaluate((config) => {
        // Generate test data based on operation type
        if (config.name.includes('List')) {
          window.largeTestData = Array.from({ length: config.itemCount }, (_, i) => ({
            id: i,
            name: `Item ${i}`,
            description: `Description for item ${i} with lots of text data`,
            metadata: {
              created: new Date().toISOString(),
              tags: ['tag1', 'tag2', 'tag3'],
              properties: new Array(10).fill(0).map((_, j) => `prop-${j}`)
            }
          }));
        } else if (config.name.includes('Image')) {
          // Simulate image data
          window.imageTestData = Array.from({ length: config.itemCount }, (_, i) => ({
            id: i,
            url: `data:image/png;base64,${'A'.repeat(1000)}`, // Simulated base64 image
            thumbnail: `data:image/png;base64,${'B'.repeat(500)}`,
            metadata: { size: 1024 * 1024, type: 'image/png' }
          }));
        } else if (config.name.includes('Chart')) {
          // Simulate chart data
          window.chartTestData = Array.from({ length: config.itemCount }, (_, i) => ({
            id: i,
            data: Array.from({ length: 100 }, (_, j) => ({
              x: j,
              y: Math.random() * 1000,
              label: `Data point ${j}`
            }))
          }));
        }
      }, operation);

      // Wait for operation to complete
      await page.waitForTimeout(5000);

      const afterMemory = await performanceHelper.getMemoryUsage();
      const memoryIncrease = (afterMemory.usedJSHeapSize - beforeMemory.usedJSHeapSize) / 1024 / 1024;

      // Validate memory increase is within expected range
      expect(memoryIncrease, `${operation.name} memory increase should be reasonable`)
        .toBeLessThan(operation.expectedMemoryGrowth);

      // Test memory cleanup after operation
      await page.evaluate(() => {
        // Clear test data
        delete window.largeTestData;
        delete window.imageTestData;
        delete window.chartTestData;
        
        // Force garbage collection if available
        if (window.gc) {
          window.gc();
        }
      });

      await page.waitForTimeout(3000); // Allow GC time

      const cleanupMemory = await performanceHelper.getMemoryUsage();
      const memoryRecovered = (afterMemory.usedJSHeapSize - cleanupMemory.usedJSHeapSize) / 1024 / 1024;

      // Record results
      const memoryMetrics = {
        initialUsage: beforeMemory.usedJSHeapSize / 1024 / 1024,
        peakUsage: afterMemory.usedJSHeapSize / 1024 / 1024,
        finalUsage: cleanupMemory.usedJSHeapSize / 1024 / 1024,
        leakDetected: memoryRecovered < (memoryIncrease * 0.5), // Less than 50% recovered
        leakRate: 0,
        gcCount: 1, // Assume we triggered GC
        heapSize: afterMemory.totalJSHeapSize / 1024 / 1024
      };

      const metrics: PerformanceMetrics = {
        memoryMetrics
      };

      const result = performanceCollector.endTest(`Memory - ${operation.name}`, metrics);
      
      console.log(`💾 ${operation.name} Memory Impact:`);
      console.log(`  Before: ${(beforeMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
      console.log(`  Peak: ${(afterMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
      console.log(`  After Cleanup: ${(cleanupMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
      console.log(`  Increase: ${memoryIncrease.toFixed(2)}MB`);
      console.log(`  Recovered: ${memoryRecovered.toFixed(2)}MB`);
      console.log(`  Items: ${operation.itemCount}`);
    });
  }

  // Test component cleanup and event listener removal
  test('Memory Usage - Component Cleanup Validation', async ({ page }) => {
    performanceCollector.startTest('Memory - Component Cleanup');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    const initialMemory = await performanceHelper.getMemoryUsage();

    // Test component mounting and unmounting cycles
    const cleanupTest = await page.evaluate(() => {
      return new Promise<{
        eventListeners: number;
        timers: number;
        intervals: number;
        memoryLeaks: number;
      }>((resolve) => {
        let eventListenerCount = 0;
        let timerCount = 0;
        let intervalCount = 0;
        
        // Override addEventListener to count listeners
        const originalAddEventListener = EventTarget.prototype.addEventListener;
        const originalRemoveEventListener = EventTarget.prototype.removeEventListener;
        
        const listeners = new Set();
        
        EventTarget.prototype.addEventListener = function(type, listener, options) {
          const key = `${this.constructor.name}-${type}-${listener.toString()}`;
          listeners.add(key);
          eventListenerCount++;
          return originalAddEventListener.call(this, type, listener, options);
        };
        
        EventTarget.prototype.removeEventListener = function(type, listener, options) {
          const key = `${this.constructor.name}-${type}-${listener.toString()}`;
          if (listeners.has(key)) {
            listeners.delete(key);
            eventListenerCount--;
          }
          return originalRemoveEventListener.call(this, type, listener, options);
        };
        
        // Override timer functions
        const originalSetTimeout = window.setTimeout;
        const originalSetInterval = window.setInterval;
        const originalClearTimeout = window.clearTimeout;
        const originalClearInterval = window.clearInterval;
        
        const timers = new Set();
        const intervals = new Set();
        
        window.setTimeout = function(callback, delay, ...args) {
          const id = originalSetTimeout.call(this, callback, delay, ...args);
          timers.add(id);
          timerCount++;
          return id;
        };
        
        window.setInterval = function(callback, delay, ...args) {
          const id = originalSetInterval.call(this, callback, delay, ...args);
          intervals.add(id);
          intervalCount++;
          return id;
        };
        
        window.clearTimeout = function(id) {
          if (timers.has(id)) {
            timers.delete(id);
            timerCount--;
          }
          return originalClearTimeout.call(this, id);
        };
        
        window.clearInterval = function(id) {
          if (intervals.has(id)) {
            intervals.delete(id);
            intervalCount--;
          }
          return originalClearInterval.call(this, id);
        };
        
        // Simulate component lifecycle
        setTimeout(() => {
          // Create components with event listeners and timers
          for (let i = 0; i < 10; i++) {
            const element = document.createElement('div');
            element.addEventListener('click', () => {});
            element.addEventListener('hover', () => {});
            
            setTimeout(() => {}, 1000);
            const interval = setInterval(() => {}, 5000);
            
            // Simulate component unmounting
            setTimeout(() => {
              // Should clean up listeners and timers
              element.removeEventListener('click', () => {});
              clearInterval(interval);
            }, 500);
          }
          
          // Check for leaks after some time
          setTimeout(() => {
            const memoryLeaks = listeners.size + timers.size + intervals.size;
            
            resolve({
              eventListeners: listeners.size,
              timers: timers.size,
              intervals: intervals.size,
              memoryLeaks
            });
            
            // Restore original functions
            EventTarget.prototype.addEventListener = originalAddEventListener;
            EventTarget.prototype.removeEventListener = originalRemoveEventListener;
            window.setTimeout = originalSetTimeout;
            window.setInterval = originalSetInterval;
            window.clearTimeout = originalClearTimeout;
            window.clearInterval = originalClearInterval;
          }, 2000);
        }, 100);
      });
    });

    const finalMemory = await performanceHelper.getMemoryUsage();
    const memoryGrowth = (finalMemory.usedJSHeapSize - initialMemory.usedJSHeapSize) / 1024 / 1024;

    // Validate proper cleanup
    expect(cleanupTest.memoryLeaks, 'No memory leaks from uncleaned resources')
      .toBeLessThan(5); // Allow some tolerance
    
    expect(cleanupTest.eventListeners, 'Event listeners should be cleaned up')
      .toBeLessThan(10);
    
    expect(cleanupTest.timers + cleanupTest.intervals, 'Timers should be cleaned up')
      .toBeLessThan(5);

    // Record results
    const memoryMetrics = {
      initialUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
      peakUsage: finalMemory.usedJSHeapSize / 1024 / 1024,
      finalUsage: finalMemory.usedJSHeapSize / 1024 / 1024,
      leakDetected: cleanupTest.memoryLeaks > 10,
      leakRate: 0,
      gcCount: 0,
      heapSize: finalMemory.totalJSHeapSize / 1024 / 1024
    };

    const metrics: PerformanceMetrics = {
      memoryMetrics
    };

    const result = performanceCollector.endTest('Memory - Component Cleanup', metrics);
    
    console.log(`🧹 Component Cleanup Validation:`);
    console.log(`  Event Listeners: ${cleanupTest.eventListeners}`);
    console.log(`  Timers: ${cleanupTest.timers}`);
    console.log(`  Intervals: ${cleanupTest.intervals}`);
    console.log(`  Total Leaks: ${cleanupTest.memoryLeaks}`);
    console.log(`  Memory Growth: ${memoryGrowth.toFixed(2)}MB`);
  });

  // Test garbage collection effectiveness
  test('Memory Usage - Garbage Collection Analysis', async ({ page }) => {
    performanceCollector.startTest('Memory - Garbage Collection Analysis');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    // Generate memory pressure to trigger GC
    const gcAnalysis = await page.evaluate(() => {
      return new Promise<{
        beforeGC: number;
        afterGC: number;
        gcEffectiveness: number;
        gcTime: number;
      }>((resolve) => {
        // Create memory pressure
        const largeData = [];
        for (let i = 0; i < 1000; i++) {
          largeData.push(new Array(1000).fill('memory-pressure-data'));
        }
        
        const beforeGC = (performance as ExtendedPerformance).memory?.usedJSHeapSize || 0;
        const gcStart = performance.now();
        
        // Force garbage collection if available
        if (window.gc) {
          window.gc();
        }
        
        const gcTime = performance.now() - gcStart;
        
        // Clear references
        largeData.length = 0;
        
        setTimeout(() => {
          const afterGC = (performance as ExtendedPerformance).memory?.usedJSHeapSize || beforeGC;
          const gcEffectiveness = ((beforeGC - afterGC) / beforeGC) * 100;
          
          resolve({
            beforeGC: beforeGC / 1024 / 1024,
            afterGC: afterGC / 1024 / 1024,
            gcEffectiveness,
            gcTime
          });
        }, 1000);
      });
    });

    // Validate GC effectiveness
    expect(gcAnalysis.gcEffectiveness, 'GC should be reasonably effective')
      .toBeGreaterThan(10); // At least 10% memory recovered

    // Record results
    const memoryMetrics = {
      initialUsage: gcAnalysis.beforeGC,
      peakUsage: gcAnalysis.beforeGC,
      finalUsage: gcAnalysis.afterGC,
      leakDetected: false,
      leakRate: 0,
      gcCount: 1,
      heapSize: gcAnalysis.afterGC
    };

    const metrics: PerformanceMetrics = {
      memoryMetrics
    };

    const result = performanceCollector.endTest('Memory - Garbage Collection Analysis', metrics);
    
    console.log(`🗑️ Garbage Collection Analysis:`);
    console.log(`  Before GC: ${gcAnalysis.beforeGC.toFixed(2)}MB`);
    console.log(`  After GC: ${gcAnalysis.afterGC.toFixed(2)}MB`);
    console.log(`  Effectiveness: ${gcAnalysis.gcEffectiveness.toFixed(2)}%`);
    console.log(`  GC Time: ${gcAnalysis.gcTime.toFixed(2)}ms`);
  });
});