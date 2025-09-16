/**
 * React Component Performance Tests
 *
 * Tests for measuring and validating React-specific performance metrics:
 * - Component rendering performance
 * - Re-render optimization
 * - State update performance
 * - Animation frame rates
 * - Component mount/unmount timing
 * - Virtual DOM efficiency
 * - Bundle size and code splitting effectiveness
 */

import {expect, test} from '@playwright/test';
import {PerformanceTestHelper, ReactPerformanceMetrics} from './performance-helpers';
import {performanceCollector, PerformanceMetrics} from './performance-metrics';
import {AuthHelper} from '../../helpers/auth.helper';

// React performance test configuration
const REACT_TEST_CONFIG = {
  components: [
    {
      name: 'HiveCard',
      selector: '[data-testid="hive-card"]',
      route: '/hives',
      expectedCount: 5,
      interactionType: 'hover' as const
    },
    {
      name: 'UserProfile',
      selector: '[data-testid="user-profile"]',
      route: '/profile',
      expectedCount: 1,
      interactionType: 'click' as const
    },
    {
      name: 'NotificationList',
      selector: '[data-testid="notification-item"]',
      route: '/notifications',
      expectedCount: 10,
      interactionType: 'click' as const
    },
    {
      name: 'ChatMessage',
      selector: '[data-testid="chat-message"]',
      route: '/hives/1/chat',
      expectedCount: 20,
      interactionType: 'scroll' as const
    },
    {
      name: 'AnalyticsDashboard',
      selector: '[data-testid="analytics-chart"]',
      route: '/analytics',
      expectedCount: 4,
      interactionType: 'click' as const
    }
  ],
  thresholds: {
    renderTime: 16, // 60fps = 16ms per frame
    rerenderCount: 5,
    fps: 55,
    memoryGrowth: 5, // MB
    bundleLoadTime: 1000, // ms
    interactionDelay: 50 // ms
  }
};

test.describe('React Component Performance Tests', () => {
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

  // Test individual component rendering performance
  for (const component of REACT_TEST_CONFIG.components) {
    test(`React Performance - ${component.name} Rendering`, async ({page}) => {
      performanceCollector.startTest(`React - ${component.name} - Rendering`);

      // Navigate to component route
      const navigationStart = Date.now();
      await page.goto(`http://localhost:3000${component.route}`);
      await page.waitForLoadState('networkidle');

      // Wait for component to be visible
      await page.waitForSelector(component.selector, {timeout: 10000});

      // Get initial performance metrics
      const initialMemory = await performanceHelper.getMemoryUsage();
      const navigationEnd = Date.now();

      // Measure component-specific rendering performance
      const renderingMetrics = await page.evaluate((selector) => {
        return new Promise<{
          componentCount: number;
          renderTime: number;
          domUpdateTime: number;
        }>((resolve) => {
          const startTime = performance.now();
          const observer = new MutationObserver(() => {
            const endTime = performance.now();
            const components = document.querySelectorAll(selector);

            observer.disconnect();
            resolve({
              componentCount: components.length,
              renderTime: endTime - startTime,
              domUpdateTime: endTime - startTime
            });
          });

          observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true
          });

          // Fallback timeout
          setTimeout(() => {
            const components = document.querySelectorAll(selector);
            observer.disconnect();
            resolve({
              componentCount: components.length,
              renderTime: performance.now() - startTime,
              domUpdateTime: performance.now() - startTime
            });
          }, 2000);
        });
      }, component.selector);

      // Measure FPS during initial render
      const fps = await performanceHelper.measureFPS();

      // Validate component count
      expect(renderingMetrics.componentCount, `Should render expected number of ${component.name} components`)
      .toBeGreaterThanOrEqual(component.expectedCount);

      // Validate render time
      expect(renderingMetrics.renderTime, `${component.name} render time should be under 16ms for 60fps`)
      .toBeLessThan(REACT_TEST_CONFIG.thresholds.renderTime);

      // Validate FPS
      expect(fps, `FPS should be above ${REACT_TEST_CONFIG.thresholds.fps}`)
      .toBeGreaterThan(REACT_TEST_CONFIG.thresholds.fps);

      // Record results
      const reactMetrics: ReactPerformanceMetrics = {
        renderTime: renderingMetrics.renderTime,
        rerenderCount: 0, // Initial render
        componentCount: renderingMetrics.componentCount,
        memoryUsage: initialMemory.usedJSHeapSize,
        fps,
        interactionDelay: navigationEnd - navigationStart,
        bundleSize: 0, // Will be measured separately
        unusedCode: 0
      };

      const metrics: PerformanceMetrics = {
        reactPerformance: reactMetrics,
        memoryMetrics: {
          initialUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
          peakUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
          finalUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
          leakDetected: false,
          leakRate: 0,
          gcCount: 0,
          heapSize: initialMemory.totalJSHeapSize / 1024 / 1024
        }
      };

      const _result = performanceCollector.endTest(`React - ${component.name} - Rendering`, metrics);

      console.log(`⚛️ ${component.name} Performance:`);
      console.log(`  Components: ${renderingMetrics.componentCount}`);
      console.log(`  Render Time: ${renderingMetrics.renderTime.toFixed(2)}ms`);
      console.log(`  FPS: ${fps.toFixed(1)}`);
      console.log(`  Memory: ${(initialMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);
    });
  }

  // Test component re-rendering performance
  test('React Performance - Component Re-rendering', async ({page}) => {
    performanceCollector.startTest('React - Re-rendering Performance');

    await page.goto('http://localhost:3000/hives');
    await page.waitForLoadState('networkidle');

    const initialMemory = await performanceHelper.getMemoryUsage();

    // Measure re-render performance by triggering state updates
    const rerenderMetrics = await page.evaluate(() => {
      return new Promise<{
        rerenderCount: number;
        totalRerenderTime: number;
        averageRerenderTime: number;
      }>((resolve) => {
        let rerenderCount = 0;
        let totalTime = 0;
        const startTime = performance.now();

        const observer = new MutationObserver(() => {
          rerenderCount++;
          totalTime = performance.now() - startTime;
        });

        observer.observe(document.body, {
          childList: true,
          subtree: true,
          attributes: true,
          characterData: true
        });

        // Trigger multiple state updates
        const triggers = [
          () => window.dispatchEvent(new CustomEvent('filter-change')),
          () => window.dispatchEvent(new CustomEvent('sort-change')),
          () => window.dispatchEvent(new CustomEvent('search-change')),
        ];

        let triggerIndex = 0;
        const triggerInterval = setInterval(() => {
          if (triggerIndex < triggers.length) {
            triggers[triggerIndex]();
            triggerIndex++;
          } else {
            clearInterval(triggerInterval);
            observer.disconnect();

            setTimeout(() => {
              resolve({
                rerenderCount,
                totalRerenderTime: totalTime,
                averageRerenderTime: rerenderCount > 0 ? totalTime / rerenderCount : 0
              });
            }, 500);
          }
        }, 100);
      });
    });

    const finalMemory = await performanceHelper.getMemoryUsage();
    const memoryGrowth = (finalMemory.usedJSHeapSize - initialMemory.usedJSHeapSize) / 1024 / 1024;

    // Validate re-render performance
    expect(rerenderMetrics.averageRerenderTime, 'Average re-render time should be minimal')
    .toBeLessThan(REACT_TEST_CONFIG.thresholds.renderTime);

    expect(rerenderMetrics.rerenderCount, 'Re-render count should be reasonable')
    .toBeLessThan(REACT_TEST_CONFIG.thresholds.rerenderCount * 2);

    expect(memoryGrowth, 'Memory growth during re-renders should be minimal')
    .toBeLessThan(REACT_TEST_CONFIG.thresholds.memoryGrowth);

    // Record results
    const reactMetrics: ReactPerformanceMetrics = {
      renderTime: rerenderMetrics.averageRerenderTime,
      rerenderCount: rerenderMetrics.rerenderCount,
      componentCount: 0,
      memoryUsage: finalMemory.usedJSHeapSize,
      fps: 0,
      interactionDelay: rerenderMetrics.totalRerenderTime,
      bundleSize: 0,
      unusedCode: 0
    };

    const metrics: PerformanceMetrics = {
      reactPerformance: reactMetrics,
      memoryMetrics: {
        initialUsage: initialMemory.usedJSHeapSize / 1024 / 1024,
        peakUsage: finalMemory.usedJSHeapSize / 1024 / 1024,
        finalUsage: finalMemory.usedJSHeapSize / 1024 / 1024,
        leakDetected: memoryGrowth > REACT_TEST_CONFIG.thresholds.memoryGrowth,
        leakRate: 0,
        gcCount: 0,
        heapSize: finalMemory.totalJSHeapSize / 1024 / 1024
      }
    };

    const _result = performanceCollector.endTest('React - Re-rendering Performance', metrics);

    console.log(`🔄 Re-rendering Performance:`);
    console.log(`  Re-renders: ${rerenderMetrics.rerenderCount}`);
    console.log(`  Avg Time: ${rerenderMetrics.averageRerenderTime.toFixed(2)}ms`);
    console.log(`  Memory Growth: ${memoryGrowth.toFixed(2)}MB`);
  });

  // Test interaction response time
  for (const component of REACT_TEST_CONFIG.components) {
    test(`React Performance - ${component.name} Interaction Response`, async ({page}) => {
      performanceCollector.startTest(`React - ${component.name} - Interaction`);

      await page.goto(`http://localhost:3000${component.route}`);
      await page.waitForLoadState('networkidle');
      await page.waitForSelector(component.selector);

      const interactionMetrics = await page.evaluate((config) => {
        return new Promise<{
          interactionTime: number;
          fps: number;
        }>((resolve) => {
          const element = document.querySelector(config.selector);
          if (!element) {
            resolve({interactionTime: 0, fps: 0});
            return;
          }

          let frameCount = 0;
          const startTime = performance.now();
          const measureFrames = (): void => {
            frameCount++;
            if (performance.now() - startTime < 1000) {
              requestAnimationFrame(measureFrames);
            }
          };

          const interactionStart = performance.now();

          // Simulate interaction based on type
          const event = config.interactionType === 'hover'
              ? new MouseEvent('mouseenter', {bubbles: true})
              : config.interactionType === 'click'
                  ? new MouseEvent('click', {bubbles: true})
                  : new Event('scroll', {bubbles: true});

          element.dispatchEvent(event);

          // Start measuring frames
          requestAnimationFrame(measureFrames);

          // Wait for interaction to complete
          setTimeout(() => {
            const interactionEnd = performance.now();
            resolve({
              interactionTime: interactionEnd - interactionStart,
              fps: frameCount
            });
          }, 1000);
        });
      }, component);

      // Validate interaction response time
      expect(interactionMetrics.interactionTime, `${component.name} interaction should be responsive`)
      .toBeLessThan(REACT_TEST_CONFIG.thresholds.interactionDelay);

      // Record results
      const reactMetrics: ReactPerformanceMetrics = {
        renderTime: 0,
        rerenderCount: 0,
        componentCount: 1,
        memoryUsage: 0,
        fps: interactionMetrics.fps,
        interactionDelay: interactionMetrics.interactionTime,
        bundleSize: 0,
        unusedCode: 0
      };

      const metrics: PerformanceMetrics = {
        reactPerformance: reactMetrics
      };

      const _result = performanceCollector.endTest(`React - ${component.name} - Interaction`, metrics);

      console.log(`👆 ${component.name} Interaction:`);
      console.log(`  Response Time: ${interactionMetrics.interactionTime.toFixed(2)}ms`);
      console.log(`  FPS: ${interactionMetrics.fps}`);
    });
  }

  // Test component mount/unmount performance
  test('React Performance - Component Lifecycle', async ({page}) => {
    performanceCollector.startTest('React - Component Lifecycle');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    const lifecycleMetrics = await page.evaluate(() => {
      return new Promise<{
        mountTime: number;
        unmountTime: number;
        rerenderCount: number;
      }>((resolve) => {
        let rerenderCount = 0;
        let mountTime = 0;
        let unmountTime = 0;

        const observer = new MutationObserver(() => {
          rerenderCount++;
        });

        observer.observe(document.body, {
          childList: true,
          subtree: true
        });

        // Simulate navigation to trigger mount/unmount
        const startMount = performance.now();
        window.history.pushState({}, '', '/hives');
        window.dispatchEvent(new PopStateEvent('popstate'));

        setTimeout(() => {
          mountTime = performance.now() - startMount;

          const startUnmount = performance.now();
          window.history.pushState({}, '', '/profile');
          window.dispatchEvent(new PopStateEvent('popstate'));

          setTimeout(() => {
            unmountTime = performance.now() - startUnmount;
            observer.disconnect();

            resolve({
              mountTime,
              unmountTime,
              rerenderCount
            });
          }, 500);
        }, 1000);
      });
    });

    // Validate lifecycle performance
    expect(lifecycleMetrics.mountTime, 'Component mount should be fast')
    .toBeLessThan(100);

    expect(lifecycleMetrics.unmountTime, 'Component unmount should be fast')
    .toBeLessThan(50);

    // Record results
    const reactMetrics: ReactPerformanceMetrics = {
      renderTime: lifecycleMetrics.mountTime,
      rerenderCount: lifecycleMetrics.rerenderCount,
      componentCount: 0,
      memoryUsage: 0,
      fps: 0,
      interactionDelay: lifecycleMetrics.mountTime + lifecycleMetrics.unmountTime,
      bundleSize: 0,
      unusedCode: 0
    };

    const metrics: PerformanceMetrics = {
      reactPerformance: reactMetrics
    };

    const _result = performanceCollector.endTest('React - Component Lifecycle', metrics);

    console.log(`🔄 Component Lifecycle:`);
    console.log(`  Mount Time: ${lifecycleMetrics.mountTime.toFixed(2)}ms`);
    console.log(`  Unmount Time: ${lifecycleMetrics.unmountTime.toFixed(2)}ms`);
    console.log(`  Re-renders: ${lifecycleMetrics.rerenderCount}`);
  });

  // Test React Concurrent Features performance
  test('React Performance - Concurrent Features', async ({page}) => {
    performanceCollector.startTest('React - Concurrent Features');

    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    // Test concurrent rendering performance
    const concurrentMetrics = await page.evaluate(() => {
      return new Promise<{
        frameDrops: number;
        responsiveness: number;
        prioritizationTime: number;
      }>((resolve) => {
        let frameDrops = 0;
        let lastFrameTime = performance.now();
        let responsiveness = 0;

        const measureFrameDrops = (): void => {
          const currentTime = performance.now();
          const frameDelta = currentTime - lastFrameTime;

          // Frame drop if more than 16.67ms (60fps)
          if (frameDelta > 16.67 * 1.5) {
            frameDrops++;
          }

          lastFrameTime = currentTime;

          if (currentTime - startTime < 2000) {
            requestAnimationFrame(measureFrameDrops);
          }
        };

        const startTime = performance.now();

        // Trigger high-priority and low-priority updates
        const highPriorityStart = performance.now();

        // Simulate user input (high priority)
        document.dispatchEvent(new CustomEvent('user-input', {
          detail: {value: 'test'}
        }));

        // Simulate data fetching (lower priority)
        setTimeout(() => {
          document.dispatchEvent(new CustomEvent('data-update', {
            detail: {data: new Array(1000).fill('item')}
          }));
        }, 100);

        const prioritizationTime = performance.now() - highPriorityStart;

        requestAnimationFrame(measureFrameDrops);

        setTimeout(() => {
          const endTime = performance.now();
          responsiveness = endTime - startTime;

          resolve({
            frameDrops,
            responsiveness,
            prioritizationTime
          });
        }, 2000);
      });
    });

    // Validate concurrent features performance
    expect(concurrentMetrics.frameDrops, 'Frame drops should be minimal with concurrent features')
    .toBeLessThan(5);

    expect(concurrentMetrics.prioritizationTime, 'High-priority updates should be fast')
    .toBeLessThan(50);

    // Record results
    const reactMetrics: ReactPerformanceMetrics = {
      renderTime: concurrentMetrics.prioritizationTime,
      rerenderCount: 0,
      componentCount: 0,
      memoryUsage: 0,
      fps: 60 - (concurrentMetrics.frameDrops * 2), // Estimate FPS based on frame drops
      interactionDelay: concurrentMetrics.responsiveness,
      bundleSize: 0,
      unusedCode: 0
    };

    const metrics: PerformanceMetrics = {
      reactPerformance: reactMetrics
    };

    const _result = performanceCollector.endTest('React - Concurrent Features', metrics);

    console.log(`⚡ Concurrent Features Performance:`);
    console.log(`  Frame Drops: ${concurrentMetrics.frameDrops}`);
    console.log(`  Responsiveness: ${concurrentMetrics.responsiveness.toFixed(2)}ms`);
    console.log(`  Prioritization: ${concurrentMetrics.prioritizationTime.toFixed(2)}ms`);
  });

  // Test bundle analysis and code splitting effectiveness
  test('React Performance - Bundle Analysis', async ({page}) => {
    performanceCollector.startTest('React - Bundle Analysis');

    // Navigate to different routes to test code splitting
    const routes = ['/dashboard', '/hives', '/profile', '/analytics'];
    const bundleMetrics = [];

    for (const route of routes) {
      const routeStart = Date.now();
      await page.goto(`http://localhost:3000${route}`);
      await page.waitForLoadState('networkidle');
      const routeEnd = Date.now();

      // Measure bundle loading for this route
      const routeMetrics = await performanceHelper.measureNetworkPerformance();

      bundleMetrics.push({
        route,
        loadTime: routeEnd - routeStart,
        requests: routeMetrics.totalRequests,
        bytes: routeMetrics.totalBytesTransferred,
        bundleSize: routeMetrics.bundleMetrics.initialBundleSize
      });
    }

    // Calculate average metrics
    const avgLoadTime = bundleMetrics.reduce((sum, m) => sum + m.loadTime, 0) / bundleMetrics.length;
    const totalBundleSize = bundleMetrics.reduce((sum, m) => sum + m.bundleSize, 0);
    const avgBundleSize = totalBundleSize / bundleMetrics.length;

    // Validate bundle performance
    expect(avgLoadTime, 'Average route load time should be reasonable')
    .toBeLessThan(REACT_TEST_CONFIG.thresholds.bundleLoadTime);

    expect(avgBundleSize / 1024, 'Average bundle size should be optimized')
    .toBeLessThan(500); // 500KB per route

    // Record results
    const reactMetrics: ReactPerformanceMetrics = {
      renderTime: 0,
      rerenderCount: 0,
      componentCount: 0,
      memoryUsage: 0,
      fps: 0,
      interactionDelay: avgLoadTime,
      bundleSize: totalBundleSize,
      unusedCode: 0 // This would require more complex analysis
    };

    const networkMetrics = {
      requestCount: bundleMetrics.reduce((sum, m) => sum + m.requests, 0),
      failedRequests: 0,
      averageLatency: avgLoadTime,
      totalBytes: bundleMetrics.reduce((sum, m) => sum + m.bytes, 0),
      cachedResources: 0,
      compressionRatio: 0,
      httpVersion: 'HTTP/2'
    };

    const metrics: PerformanceMetrics = {
      reactPerformance: reactMetrics,
      networkMetrics
    };

    const _result = performanceCollector.endTest('React - Bundle Analysis', metrics);

    console.log(`📦 Bundle Analysis:`);
    console.log(`  Avg Load Time: ${avgLoadTime.toFixed(2)}ms`);
    console.log(`  Avg Bundle Size: ${(avgBundleSize / 1024).toFixed(2)}KB`);
    console.log(`  Total Bundle Size: ${(totalBundleSize / 1024).toFixed(2)}KB`);

    bundleMetrics.forEach(metric => {
      console.log(`  ${metric.route}: ${metric.loadTime}ms, ${(metric.bundleSize / 1024).toFixed(2)}KB`);
    });
  });
});