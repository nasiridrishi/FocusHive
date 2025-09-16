/**
 * Performance Testing for Network Error Scenarios
 * Measures performance impact and memory usage during network failures
 */

import { test, expect, Page, CDPSession } from '@playwright/test';
import { createNetworkSimulator } from './utils/networkSim';
import { NetworkTestHelper } from './test-helpers';

interface PerformanceMetrics {
  memoryUsage: {
    usedJSHeapSize: number;
    totalJSHeapSize: number;
    jsHeapSizeLimit: number;
  };
  timing: {
    renderStart: number;
    domContentLoaded: number;
    loadComplete: number;
    firstContentfulPaint: number;
    largestContentfulPaint: number;
  };
  networkRequests: {
    total: number;
    failed: number;
    retries: number;
    duplicates: number;
  };
}

test.describe('Network Error Performance Testing', () => {
  let networkSim: ReturnType<typeof createNetworkSimulator>;
  let testHelper: NetworkTestHelper;
  let cdpSession: CDPSession;

  test.beforeEach(async ({ page }) => {
    networkSim = createNetworkSimulator(page);
    testHelper = new NetworkTestHelper(page);
    
    // Enable CDP for performance monitoring
    try {
      cdpSession = await page.context().newCDPSession(page);
      await cdpSession.send('Performance.enable');
      await cdpSession.send('Runtime.enable');
    } catch {
      console.warn('CDP not available, performance metrics will be limited');
    }
    
    await testHelper.resetMockBackend();
  });

  test.afterEach(async () => {
    await networkSim.cleanup();
    if (cdpSession) {
      await cdpSession.detach();
    }
  });

  test.describe('Memory Usage During Network Failures', () => {
    test('should not leak memory during repeated network failures', async ({ page }) => {
      const initialMetrics = await captureMemoryMetrics(page);
      console.log('Initial memory usage:', initialMetrics.memoryUsage);

      // Simulate repeated network failures and recoveries
      for (let cycle = 0; cycle < 5; cycle++) {
        console.log(`Network failure cycle ${cycle + 1}/5`);
        
        // Trigger network error
        await networkSim.goOffline();
        await page.goto('/dashboard');
        await testHelper.waitForNetworkErrorFallback();
        
        // Recover network
        await networkSim.goOnline();
        await testHelper.clickRetryButton();
        await testHelper.waitForNetworkRestore();
        
        // Force garbage collection if available
        await page.evaluate(() => {
          if (window.gc) {
            window.gc();
          }
        });
        
        await page.waitForTimeout(1000); // Let things settle
      }

      const finalMetrics = await captureMemoryMetrics(page);
      console.log('Final memory usage:', finalMetrics.memoryUsage);

      // Memory should not grow significantly (less than 10MB increase)
      const memoryGrowth = finalMetrics.memoryUsage.usedJSHeapSize - initialMetrics.memoryUsage.usedJSHeapSize;
      expect(memoryGrowth).toBeLessThan(10 * 1024 * 1024); // 10MB threshold
      
      console.log(`Memory growth: ${(memoryGrowth / 1024 / 1024).toFixed(2)}MB`);
    });

    test('should manage memory efficiently during long offline periods', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');
      
      // Go offline for extended period
      await networkSim.goOffline();
      
      // Simulate user activity while offline
      for (let i = 0; i < 20; i++) {
        await page.evaluate((iteration) => {
          // Simulate data accumulation
          const existingData = JSON.parse(localStorage.getItem('offlineData') || '[]');
          existingData.push({
            id: iteration,
            timestamp: Date.now(),
            data: 'A'.repeat(100) // Small data chunks
          });
          localStorage.setItem('offlineData', JSON.stringify(existingData));
        }, i);
        
        await page.waitForTimeout(100);
      }

      const offlineMetrics = await captureMemoryMetrics(page);
      
      // Come back online and clear offline data
      await networkSim.goOnline();
      await page.evaluate(() => {
        localStorage.removeItem('offlineData');
      });

      // Force garbage collection
      await page.evaluate(() => {
        if (window.gc) {
          window.gc();
        }
      });

      await page.waitForTimeout(2000);
      const recoveredMetrics = await captureMemoryMetrics(page);

      // Memory should recover after clearing offline data
      const memoryRecovery = offlineMetrics.memoryUsage.usedJSHeapSize - recoveredMetrics.memoryUsage.usedJSHeapSize;
      expect(memoryRecovery).toBeGreaterThan(0); // Some memory should be freed
      
      console.log(`Memory recovered: ${(memoryRecovery / 1024).toFixed(2)}KB`);
    });
  });

  test.describe('Render Performance During Network Issues', () => {
    test('should render error fallback quickly', async ({ page }) => {
      // Start timing
      const startTime = Date.now();
      
      // Trigger network error
      await networkSim.goOffline();
      await page.goto('/dashboard');
      
      // Wait for error fallback to appear
      await testHelper.waitForNetworkErrorFallback(5000);
      
      const renderTime = Date.now() - startTime;
      console.log(`Error fallback render time: ${renderTime}ms`);
      
      // Error fallback should render within 2 seconds
      expect(renderTime).toBeLessThan(2000);
      
      // Verify error UI is actually visible and interactive
      await expect(page.getByTestId('retry-button')).toBeVisible();
      await expect(page.getByTestId('retry-button')).toBeEnabled();
    });

    test('should maintain UI responsiveness during slow network', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Apply very slow network conditions
      await networkSim.throttle({
        downloadThroughput: 1000, // 1KB/s
        latency: 3000 // 3 second latency
      });

      // Measure UI responsiveness
      const interactions = [
        { action: 'click', selector: '[data-testid="menu-button"]' },
        { action: 'type', selector: '[data-testid="search-input"]', text: 'test' },
        { action: 'click', selector: '[data-testid="settings-button"]' }
      ];

      for (const interaction of interactions) {
        const startTime = Date.now();
        
        if (interaction.action === 'click') {
          await page.locator(interaction.selector).click();
        } else if (interaction.action === 'type') {
          await page.locator(interaction.selector).fill(interaction.text || '');
        }
        
        const responseTime = Date.now() - startTime;
        console.log(`${interaction.action} response time: ${responseTime}ms`);
        
        // UI interactions should remain snappy (under 500ms)
        expect(responseTime).toBeLessThan(500);
      }
    });

    test('should optimize performance based on connection quality', async ({ page }) => {
      const testCases = [
        { condition: 'SLOW_2G', expectedOptimizations: ['reduced-animations', 'compressed-images', 'lazy-loading'] },
        { condition: 'SLOW_3G', expectedOptimizations: ['reduced-animations', 'compressed-images'] },
        { condition: 'FAST_3G', expectedOptimizations: [] }
      ];

      for (const testCase of testCases) {
        await networkSim.throttle(testCase.condition);
        await page.goto('/dashboard');
        
        // Check if performance optimizations are applied
        for (const optimization of testCase.expectedOptimizations) {
          await expect(page.locator(`[data-performance-mode="${optimization}"]`)).toBeVisible();
        }
        
        // Measure performance metrics
        const metrics = await page.evaluate(() => {
          const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
          return {
            domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
            loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
          };
        });

        console.log(`Performance for ${testCase.condition}:`, metrics);
        
        // Verify reasonable performance even on slow connections
        if (testCase.condition === 'SLOW_2G') {
          expect(metrics.domContentLoaded).toBeLessThan(5000); // 5 seconds
        } else if (testCase.condition === 'SLOW_3G') {
          expect(metrics.domContentLoaded).toBeLessThan(3000); // 3 seconds
        }
      }
    });
  });

  test.describe('Network Request Efficiency', () => {
    test('should not make redundant requests during retries', async ({ page }) => {
      const networkRequests = new Map<string, number>();
      
      // Monitor network requests
      page.on('request', request => {
        const url = request.url();
        networkRequests.set(url, (networkRequests.get(url) || 0) + 1);
      });

      await page.goto('/dashboard');
      
      // Set up intermittent failures
      await testHelper.setNetworkScenario({ 
        scenario: 'intermittent', 
        options: { failureRate: 0.5 } 
      });
      
      // Trigger multiple retry attempts
      for (let i = 0; i < 5; i++) {
        await testHelper.clickRetryButton();
        await page.waitForTimeout(1000);
      }
      
      // Analyze request patterns
      const apiRequests = Array.from(networkRequests.entries())
        .filter(([url]) => url.includes('/api/'))
        .map(([url, count]) => ({ url, count }));
      
      console.log('API request counts:', apiRequests);
      
      // Should not have excessive duplicate requests
      for (const request of apiRequests) {
        expect(request.count).toBeLessThan(10); // Reasonable retry limit
      }
      
      // Should not make requests to same endpoint simultaneously
      const simultaneousRequests = apiRequests.filter(req => req.count > 3);
      expect(simultaneousRequests.length).toBeLessThan(3); // Max 3 endpoints with high retry counts
    });

    test('should batch requests efficiently during network recovery', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Go offline and accumulate pending requests
      await networkSim.goOffline();
      
      // Simulate multiple user actions that would trigger API calls
      const actions = [
        () => page.getByTestId('refresh-data').click(),
        () => page.getByTestId('load-more').click(),
        () => page.getByTestId('sync-changes').click()
      ];
      
      for (const action of actions) {
        await action();
        await page.waitForTimeout(100);
      }
      
      const requestLog: string[] = [];
      page.on('request', request => {
        requestLog.push(`${Date.now()}: ${request.url()}`);
      });
      
      // Come back online
      await networkSim.goOnline();
      
      // Wait for requests to be processed
      await page.waitForTimeout(3000);
      
      console.log('Request timeline:', requestLog);
      
      // Requests should be batched/queued, not all fired simultaneously
      const requestTimes = requestLog.map(log => parseInt(log.split(':')[0]));
      const timeSpread = Math.max(...requestTimes) - Math.min(...requestTimes);
      
      // Requests should be spread over at least 1 second (indicating queuing/batching)
      expect(timeSpread).toBeGreaterThan(1000);
    });
  });

  test.describe('CPU Usage During Network Errors', () => {
    test('should not consume excessive CPU during error states', async ({ page }) => {
      if (!cdpSession) {
        test.skip('CDP not available for CPU profiling');
      }

      // Start CPU profiling
      await cdpSession.send('Profiler.enable');
      await cdpSession.send('Profiler.start');
      
      const startTime = Date.now();
      
      // Trigger network errors and recovery cycles
      for (let i = 0; i < 3; i++) {
        await networkSim.goOffline();
        await page.goto('/dashboard');
        await page.waitForTimeout(1000);
        
        await networkSim.goOnline();
        await page.waitForTimeout(1000);
      }
      
      const duration = Date.now() - startTime;
      
      // Stop profiling and get results
      const profile = await cdpSession.send('Profiler.stop');
      
      // Analyze CPU usage (simplified analysis)
      const totalSamples = profile.profile.samples.length;
      const samplingRate = totalSamples / (duration / 1000); // samples per second
      
      console.log(`CPU sampling rate: ${samplingRate.toFixed(2)} samples/sec`);
      
      // Should not have excessive CPU usage (this is a rough estimate)
      expect(samplingRate).toBeLessThan(200); // Reasonable threshold
    });
  });

  test.describe('Large Dataset Performance', () => {
    test('should handle large datasets efficiently during network failures', async ({ page }) => {
      // Create large dataset in memory
      await page.evaluate(() => {
        const largeDataset = Array.from({ length: 10000 }, (_, i) => ({
          id: i,
          name: `Item ${i}`,
          description: 'A'.repeat(100), // 100 chars each
          timestamp: Date.now()
        }));
        
        (window as unknown).testDataset = largeDataset;
      });

      const startTime = Date.now();
      
      // Go offline
      await networkSim.goOffline();
      await page.goto('/large-dataset-page');
      
      // Render large dataset
      await page.evaluate(() => {
        const dataset = (window as unknown).testDataset;
        const container = document.getElementById('data-container');
        if (container && dataset) {
          container.innerHTML = dataset
            .slice(0, 100) // Only render first 100 items
            .map((item: unknown) => `<div data-testid="data-item-${item.id}">${item.name}</div>`)
            .join('');
        }
      });
      
      const renderTime = Date.now() - startTime;
      console.log(`Large dataset render time: ${renderTime}ms`);
      
      // Should render within reasonable time even when offline
      expect(renderTime).toBeLessThan(5000);
      
      // Verify some items are visible
      await expect(page.getByTestId('data-item-0')).toBeVisible();
      await expect(page.getByTestId('data-item-10')).toBeVisible();
      
      // Memory usage should be reasonable
      const metrics = await captureMemoryMetrics(page);
      expect(metrics.memoryUsage.usedJSHeapSize).toBeLessThan(100 * 1024 * 1024); // Under 100MB
    });
  });
});

// Utility functions for performance testing
async function captureMemoryMetrics(page: Page): Promise<PerformanceMetrics> {
  return await page.evaluate(() => {
    const memory = (performance as unknown).memory || {
      usedJSHeapSize: 0,
      totalJSHeapSize: 0,
      jsHeapSizeLimit: 0
    };

    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    const paintEntries = performance.getEntriesByType('paint');
    
    const fcp = paintEntries.find(entry => entry.name === 'first-contentful-paint');
    const lcp = paintEntries.find(entry => entry.name === 'largest-contentful-paint');

    return {
      memoryUsage: {
        usedJSHeapSize: memory.usedJSHeapSize,
        totalJSHeapSize: memory.totalJSHeapSize,
        jsHeapSizeLimit: memory.jsHeapSizeLimit
      },
      timing: {
        renderStart: navigation.responseStart - navigation.fetchStart,
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
        loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
        firstContentfulPaint: fcp?.startTime || 0,
        largestContentfulPaint: lcp?.startTime || 0
      },
      networkRequests: {
        total: performance.getEntriesByType('resource').length,
        failed: 0, // Would need additional tracking
        retries: 0, // Would need additional tracking  
        duplicates: 0 // Would need additional tracking
      }
    };
  });
}

async function _measureInteractionLatency(page: Page, selector: string, action: 'click' | 'type', text?: string): Promise<number> {
  const startTime = Date.now();
  
  if (action === 'click') {
    await page.locator(selector).click();
  } else if (action === 'type' && text) {
    await page.locator(selector).fill(text);
  }
  
  return Date.now() - startTime;
}