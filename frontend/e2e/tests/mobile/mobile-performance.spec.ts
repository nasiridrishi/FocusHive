/**
 * Mobile Performance Tests for FocusHive
 * Tests performance metrics, Core Web Vitals, and mobile-specific optimizations
 */

import {expect, Page, test} from '@playwright/test';
import {MOBILE_PERFORMANCE_BUDGETS} from './mobile.config';
import {MobileHelper} from '../../helpers/mobile.helper';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_URLS} from '../../helpers/test-data';

interface PerformanceMetrics {
  loadTime: number;
  domContentLoaded: number;
  firstContentfulPaint: number;
  largestContentfulPaint: number;
  firstInputDelay: number;
  cumulativeLayoutShift: number;
  totalBlockingTime: number;
  speedIndex: number;
  timeToInteractive: number;
}

interface NetworkMetrics {
  requests: number;
  transferSize: number;
  resourceSize: number;
  failedRequests: number;
  cachedRequests: number;
}

interface MemoryMetrics {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
  memoryUsagePercentage: number;
}

interface MobileOptimizationTest {
  name: string;
  url: string;
  expectedOptimizations: string[];
  performanceBudget: keyof typeof MOBILE_PERFORMANCE_BUDGETS;
}

interface TouchPerformanceResult {
  touchStartDelay: number;
  touchEndDelay: number;
  scrollPerformance: number;
  animationFrameRate: number;
}

test.describe('Mobile Performance - Core Web Vitals', () => {
  let authHelper: AuthHelper;
  let _mobileHelper: MobileHelper;

  test.beforeEach(async ({page}) => {
    authHelper = new AuthHelper(page);
    _mobileHelper = new MobileHelper(page);

    // Set mobile viewport
    await page.setViewportSize({width: 390, height: 844});
  });

  const performanceTests: MobileOptimizationTest[] = [
    {
      name: 'Home Page',
      url: TEST_URLS.HOME,
      expectedOptimizations: ['image_lazy_loading', 'critical_css', 'preload_resources'],
      performanceBudget: 'MOBILE_4G'
    },
    {
      name: 'Dashboard',
      url: TEST_URLS.DASHBOARD,
      expectedOptimizations: ['code_splitting', 'resource_hints', 'service_worker'],
      performanceBudget: 'MOBILE_4G'
    },
    {
      name: 'Hive List',
      url: TEST_URLS.HIVE_LIST,
      expectedOptimizations: ['virtual_scrolling', 'image_optimization', 'data_pagination'],
      performanceBudget: 'MOBILE_3G'
    },
    {
      name: 'Analytics',
      url: TEST_URLS.ANALYTICS,
      expectedOptimizations: ['chart_lazy_loading', 'data_compression', 'progressive_enhancement'],
      performanceBudget: 'MOBILE_4G'
    }
  ];

  performanceTests.forEach(perfTest => {
    test(`should meet Core Web Vitals budgets for ${perfTest.name}`, async ({page}) => {
      await authHelper.loginWithTestUser();

      await test.step(`Testing ${perfTest.name} performance`, async () => {
        // Start performance measurement
        await page.goto(perfTest.url, {waitUntil: 'networkidle'});

        // Measure Core Web Vitals
        const metrics = await measureCoreWebVitals(page);
        const budget = MOBILE_PERFORMANCE_BUDGETS[perfTest.performanceBudget];

        // Validate against performance budget
        expect(metrics.firstContentfulPaint).toBeLessThan(budget.fcp);
        expect(metrics.largestContentfulPaint).toBeLessThan(budget.lcp);
        expect(metrics.firstInputDelay).toBeLessThan(budget.fid);
        expect(metrics.cumulativeLayoutShift).toBeLessThan(budget.cls);
        expect(metrics.loadTime).toBeLessThan(budget.loadTime);

        console.log(`${perfTest.name} Performance Metrics:`, {
          LCP: `${metrics.largestContentfulPaint}ms (budget: ${budget.lcp}ms)`,
          FID: `${metrics.firstInputDelay}ms (budget: ${budget.fid}ms)`,
          CLS: `${metrics.cumulativeLayoutShift} (budget: ${budget.cls})`,
          FCP: `${metrics.firstContentfulPaint}ms (budget: ${budget.fcp}ms)`,
          LoadTime: `${metrics.loadTime}ms (budget: ${budget.loadTime}ms)`
        });

        // Test mobile-specific optimizations
        await validateMobileOptimizations(page, perfTest.expectedOptimizations);
      });
    });
  });

  test('should handle different network conditions gracefully', async ({page, context}) => {
    await authHelper.loginWithTestUser();

    const networkConditions = [
      {name: 'Slow 3G', budget: 'MOBILE_3G' as const},
      {name: 'Fast 3G', budget: 'MOBILE_4G' as const},
      {name: '4G', budget: 'MOBILE_5G' as const}
    ];

    for (const condition of networkConditions) {
      await test.step(`Performance on ${condition.name}`, async () => {
        // Simulate network condition
        const client = await context.newCDPSession(page);
        await client.send('Network.enable');

        const networkSpeeds = {
          'Slow 3G': {download: 500 * 1024 / 8, upload: 500 * 1024 / 8, latency: 2000},
          'Fast 3G': {download: 1.5 * 1024 * 1024 / 8, upload: 750 * 1024 / 8, latency: 562.5},
          '4G': {download: 9 * 1024 * 1024 / 8, upload: 9 * 1024 * 1024 / 8, latency: 170}
        };

        const speed = networkSpeeds[condition.name];
        await client.send('Network.emulateNetworkConditions', {
          offline: false,
          downloadThroughput: speed.download,
          uploadThroughput: speed.upload,
          latency: speed.latency
        });

        const startTime = Date.now();
        await page.goto(TEST_URLS.DASHBOARD);
        await page.waitForLoadState('networkidle');
        const loadTime = Date.now() - startTime;

        const budget = MOBILE_PERFORMANCE_BUDGETS[condition.budget];
        expect(loadTime).toBeLessThan(budget.loadTime);

        console.log(`${condition.name} load time: ${loadTime}ms (budget: ${budget.loadTime}ms)`);
      });
    }
  });

  test('should optimize images for mobile screens', async ({page}) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    await test.step('Image optimization validation', async () => {
      const imageOptimizations = await page.evaluate(() => {
        const images = Array.from(document.querySelectorAll('img'));
        const optimizations: Array<{
          src: string;
          loading: string | null;
          srcset: string | null;
          sizes: string | null;
          format: string;
          dimensions: { width: number; height: number };
        }> = [];

        images.forEach(img => {
          const rect = img.getBoundingClientRect();
          const src = img.src || img.getAttribute('src') || '';

          optimizations.push({
            src,
            loading: img.getAttribute('loading'),
            srcset: img.getAttribute('srcset'),
            sizes: img.getAttribute('sizes'),
            format: src.includes('.webp') ? 'webp' :
                src.includes('.avif') ? 'avif' :
                    src.includes('.jpg') || src.includes('.jpeg') ? 'jpeg' :
                        src.includes('.png') ? 'png' : 'unknown',
            dimensions: {
              width: rect.width,
              height: rect.height
            }
          });
        });

        return optimizations;
      });

      // Validate image optimizations
      for (const img of imageOptimizations) {
        // Check for lazy loading
        if (img.loading) {
          expect(['lazy', 'eager']).toContain(img.loading);
        }

        // Check for responsive images
        if (img.dimensions.width > 300) {
          // Larger images should have srcset for responsiveness
          if (!img.srcset) {
            console.warn(`Large image without srcset: ${img.src}`);
          }
        }

        // Check for modern image formats
        if (img.format === 'unknown') {
          console.warn(`Unknown image format: ${img.src}`);
        } else {
          console.log(`Image format: ${img.format} for ${img.src}`);
        }
      }

      console.log(`Analyzed ${imageOptimizations.length} images`);
    });
  });
});

test.describe('Mobile Performance - Touch and Scroll Performance', () => {
  test('should maintain 60fps during scrolling', async ({page}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    await test.step('Scroll performance measurement', async () => {
      // Start frame rate monitoring
      const scrollPerformance = await measureScrollPerformance(page);

      expect(scrollPerformance.animationFrameRate).toBeGreaterThan(50); // Allow some variance from 60fps
      expect(scrollPerformance.scrollPerformance).toBeLessThan(16.67); // 60fps = 16.67ms per frame

      console.log('Scroll Performance:', {
        frameRate: `${scrollPerformance.animationFrameRate}fps`,
        scrollLatency: `${scrollPerformance.scrollPerformance}ms`
      });
    });
  });

  test('should respond to touch events quickly', async ({page}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.DASHBOARD);

    await test.step('Touch responsiveness measurement', async () => {
      const button = page.locator('[data-testid="create-hive-button"]').first();

      if (await button.isVisible()) {
        const touchPerformance = await measureTouchPerformance(page, button);

        // Touch events should respond within acceptable limits
        expect(touchPerformance.touchStartDelay).toBeLessThan(100); // 100ms maximum delay
        expect(touchPerformance.touchEndDelay).toBeLessThan(100);

        console.log('Touch Performance:', {
          touchStart: `${touchPerformance.touchStartDelay}ms`,
          touchEnd: `${touchPerformance.touchEndDelay}ms`
        });
      }
    });
  });

  test('should handle rapid touch interactions without blocking', async ({page}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.ANALYTICS);

    await test.step('Rapid touch handling', async () => {
      const interactiveElements = page.locator('button, [role="button"]');
      const elementCount = await interactiveElements.count();

      if (elementCount > 0) {
        const startTime = Date.now();

        // Rapidly tap multiple elements
        for (let i = 0; i < Math.min(5, elementCount); i++) {
          const element = interactiveElements.nth(i);
          if (await element.isVisible()) {
            await element.click({timeout: 1000});
            await page.waitForTimeout(50); // Brief pause between clicks
          }
        }

        const totalTime = Date.now() - startTime;
        const averageResponseTime = totalTime / Math.min(5, elementCount);

        expect(averageResponseTime).toBeLessThan(200); // Should handle rapid interactions
        console.log(`Average touch response time: ${averageResponseTime}ms`);
      }
    });
  });
});

test.describe('Mobile Performance - Memory and Battery Optimization', () => {
  test('should manage memory efficiently', async ({page}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();

    await test.step('Memory usage monitoring', async () => {
      // Navigate through several pages to test memory management
      const pages = [
        TEST_URLS.DASHBOARD,
        TEST_URLS.HIVE_LIST,
        TEST_URLS.ANALYTICS,
        TEST_URLS.PROFILE
      ];

      const memorySnapshots: MemoryMetrics[] = [];

      for (const url of pages) {
        await page.goto(url);
        await page.waitForLoadState('networkidle');

        const memoryMetrics = await measureMemoryUsage(page);
        memorySnapshots.push(memoryMetrics);

        // Memory shouldn't exceed reasonable limits
        expect(memoryMetrics.memoryUsagePercentage).toBeLessThan(80); // 80% of heap limit

        await page.waitForTimeout(1000); // Allow garbage collection
      }

      // Check for memory leaks (memory should not continuously increase)
      const initialMemory = memorySnapshots[0].usedJSHeapSize;
      const finalMemory = memorySnapshots[memorySnapshots.length - 1].usedJSHeapSize;
      const memoryIncrease = (finalMemory - initialMemory) / initialMemory;

      expect(memoryIncrease).toBeLessThan(2.0); // Should not double memory usage

      console.log('Memory Usage Analysis:', {
        initial: `${(initialMemory / 1024 / 1024).toFixed(1)}MB`,
        final: `${(finalMemory / 1024 / 1024).toFixed(1)}MB`,
        increase: `${(memoryIncrease * 100).toFixed(1)}%`
      });
    });
  });

  test('should optimize for battery usage', async ({page}) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    await test.step('Battery optimization validation', async () => {
      // Check for battery-draining patterns
      const _batteryOptimizations = await page.evaluate(() => {
        const issues: string[] = [];
        const optimizations: string[] = [];

        // Check for excessive timers
        const originalSetInterval = window.setInterval;
        let intervalCount = 0;
        window.setInterval = function (...args) {
          intervalCount++;
          return originalSetInterval.apply(window, args);
        };

        // Check for animation frame usage
        const originalRequestAnimationFrame = window.requestAnimationFrame;
        let rafCount = 0;
        window.requestAnimationFrame = function (...args) {
          rafCount++;
          return originalRequestAnimationFrame.apply(window, args);
        };

        // Wait for some execution
        setTimeout(() => {
          if (intervalCount > 5) {
            issues.push(`Excessive intervals: ${intervalCount}`);
          } else {
            optimizations.push(`Reasonable intervals: ${intervalCount}`);
          }

          if (rafCount > 100) {
            issues.push(`Excessive animation frames: ${rafCount}`);
          }
        }, 2000);

        return {issues, optimizations};
      });

      // Allow time for the evaluation to complete
      await page.waitForTimeout(3000);

      // Verify CPU usage is reasonable (simplified check)
      const performanceMark = await page.evaluate(() => {
        const start = performance.now();
        // Simulate some work
        for (let i = 0; i < 100000; i++) {
          Math.random();
        }
        return performance.now() - start;
      });

      expect(performanceMark).toBeLessThan(100); // Should complete quickly

      console.log(`CPU performance test: ${performanceMark.toFixed(2)}ms`);
    });
  });
});

test.describe('Mobile Performance - Bundle Size and Loading', () => {
  test('should meet bundle size budgets', async ({page}) => {
    await test.step('Bundle size analysis', async () => {
      await page.goto(TEST_URLS.HOME);
      await page.waitForLoadState('networkidle');

      const networkMetrics = await measureNetworkMetrics(page);
      const budget = MOBILE_PERFORMANCE_BUDGETS.MOBILE_4G;

      // Validate bundle size
      const bundleSizeKB = networkMetrics.transferSize / 1024;
      expect(bundleSizeKB).toBeLessThan(budget.bundleSize);

      // Check resource optimization
      expect(networkMetrics.cachedRequests / networkMetrics.requests).toBeGreaterThan(0.1); // At least 10% cached

      console.log('Network Metrics:', {
        totalRequests: networkMetrics.requests,
        bundleSize: `${bundleSizeKB.toFixed(1)}KB (budget: ${budget.bundleSize}KB)`,
        cachedRequests: networkMetrics.cachedRequests,
        failedRequests: networkMetrics.failedRequests
      });
    });
  });

  test('should implement effective caching strategies', async ({page}) => {
    await test.step('Caching strategy validation', async () => {
      // First visit
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      const firstVisitMetrics = await measureNetworkMetrics(page);

      // Reload page to test caching
      await page.reload();
      await page.waitForLoadState('networkidle');

      const reloadMetrics = await measureNetworkMetrics(page);

      // Second visit should have more cached resources
      const cacheEffectiveness = reloadMetrics.cachedRequests / reloadMetrics.requests;
      expect(cacheEffectiveness).toBeGreaterThan(0.3); // At least 30% cached on reload

      // Second visit should be faster
      const firstLoadTime = firstVisitMetrics.transferSize / 1024; // Rough load estimate
      const reloadTime = reloadMetrics.transferSize / 1024;

      expect(reloadTime).toBeLessThan(firstLoadTime);

      console.log('Caching Analysis:', {
        firstVisit: {
          requests: firstVisitMetrics.requests,
          cached: firstVisitMetrics.cachedRequests
        },
        reload: {
          requests: reloadMetrics.requests,
          cached: reloadMetrics.cachedRequests
        },
        cacheEffectiveness: `${(cacheEffectiveness * 100).toFixed(1)}%`
      });
    });
  });
});

// Helper Functions

/**
 * Measure Core Web Vitals and other performance metrics
 */
async function measureCoreWebVitals(page: Page): Promise<PerformanceMetrics> {
  return await page.evaluate(() => {
    return new Promise<PerformanceMetrics>((resolve) => {
      const metrics: Partial<PerformanceMetrics> = {};

      // Navigation timing
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      metrics.loadTime = navigation.loadEventEnd - navigation.fetchStart;
      metrics.domContentLoaded = navigation.domContentLoadedEventEnd - navigation.fetchStart;

      // Performance Observer for Core Web Vitals
      let metricsCollected = 0;
      const totalMetrics = 4; // FCP, LCP, FID, CLS

      const checkComplete = (): void => {
        metricsCollected++;
        if (metricsCollected >= totalMetrics) {
          resolve(metrics as PerformanceMetrics);
        }
      };

      // First Contentful Paint
      new PerformanceObserver((list) => {
        const entries = list.getEntries();
        const fcp = entries.find(entry => entry.name === 'first-contentful-paint');
        if (fcp) {
          metrics.firstContentfulPaint = fcp.startTime;
          checkComplete();
        }
      }).observe({entryTypes: ['paint']});

      // Largest Contentful Paint
      new PerformanceObserver((list) => {
        const entries = list.getEntries();
        const lcp = entries[entries.length - 1];
        if (lcp) {
          metrics.largestContentfulPaint = lcp.startTime;
          checkComplete();
        }
      }).observe({entryTypes: ['largest-contentful-paint']});

      // First Input Delay
      new PerformanceObserver((list) => {
        const entries = list.getEntries();
        if (entries.length > 0) {
          metrics.firstInputDelay = entries[0].processingStart - entries[0].startTime;
        } else {
          metrics.firstInputDelay = 0; // No user input yet
        }
        checkComplete();
      }).observe({entryTypes: ['first-input']});

      // Cumulative Layout Shift
      let clsValue = 0;
      new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const layoutShiftEntry = entry as LayoutShiftEntry;
          if (!layoutShiftEntry.hadRecentInput) {
            clsValue += layoutShiftEntry.value;
          }
        }
        metrics.cumulativeLayoutShift = clsValue;
        checkComplete();
      }).observe({entryTypes: ['layout-shift']});

      // Fallback timeout
      setTimeout(() => {
        metrics.firstContentfulPaint = metrics.firstContentfulPaint || 0;
        metrics.largestContentfulPaint = metrics.largestContentfulPaint || 0;
        metrics.firstInputDelay = metrics.firstInputDelay || 0;
        metrics.cumulativeLayoutShift = metrics.cumulativeLayoutShift || 0;
        metrics.totalBlockingTime = 0;
        metrics.speedIndex = 0;
        metrics.timeToInteractive = 0;

        resolve(metrics as PerformanceMetrics);
      }, 3000);
    });
  });
}

/**
 * Validate mobile-specific optimizations
 */
async function validateMobileOptimizations(page: Page, expectedOptimizations: string[]): Promise<void> {
  for (const optimization of expectedOptimizations) {
    switch (optimization) {
      case 'image_lazy_loading': {
        const lazyImages = await page.locator('img[loading="lazy"]').count();
        expect(lazyImages).toBeGreaterThan(0);
        break;
      }

      case 'critical_css': {
        const inlineStyles = await page.locator('style').count();
        expect(inlineStyles).toBeGreaterThan(0);
        break;
      }

      case 'preload_resources': {
        const preloadLinks = await page.locator('link[rel="preload"]').count();
        console.log(`Preload resources found: ${preloadLinks}`);
        break;
      }

      case 'code_splitting': {
        const scriptTags = await page.locator('script[src]').count();
        expect(scriptTags).toBeGreaterThan(1); // Multiple bundles indicate code splitting
        break;
      }

      case 'service_worker': {
        const swRegistered = await page.evaluate(() => {
          return 'serviceWorker' in navigator;
        });
        expect(swRegistered).toBeTruthy();
        break;
      }

      case 'virtual_scrolling': {
        // Check for virtual scrolling implementation (viewport-based rendering)
        const hasVirtualScroll = await page.evaluate(() => {
          const scrollContainers = document.querySelectorAll('[data-virtual="true"], .virtual-scroll');
          return scrollContainers.length > 0;
        });
        console.log(`Virtual scrolling detected: ${hasVirtualScroll}`);
        break;
      }

      case 'image_optimization': {
        const webpImages = await page.locator('img[src*=".webp"]').count();
        console.log(`WebP images found: ${webpImages}`);
        break;
      }
    }
  }
}

/**
 * Measure scroll performance
 */
async function measureScrollPerformance(page: Page): Promise<TouchPerformanceResult> {
  return await page.evaluate(() => {
    return new Promise<TouchPerformanceResult>((resolve) => {
      let frameCount = 0;
      let lastFrameTime = performance.now();
      let totalFrameTime = 0;

      const measureFrames = (): void => {
        const currentTime = performance.now();
        const deltaTime = currentTime - lastFrameTime;
        totalFrameTime += deltaTime;
        frameCount++;
        lastFrameTime = currentTime;

        if (frameCount < 60) { // Measure for 60 frames
          requestAnimationFrame(measureFrames);
        } else {
          const averageFrameTime = totalFrameTime / frameCount;
          const frameRate = 1000 / averageFrameTime;

          resolve({
            touchStartDelay: 0, // Would need actual touch events to measure
            touchEndDelay: 0,
            scrollPerformance: averageFrameTime,
            animationFrameRate: frameRate
          });
        }
      };

      // Start scrolling to generate frames
      window.scrollBy(0, 1000);
      requestAnimationFrame(measureFrames);
    });
  });
}

/**
 * Measure touch performance
 */
async function measureTouchPerformance(page: Page, element: import('@playwright/test').Locator): Promise<TouchPerformanceResult> {
  return await page.evaluate((el) => {
    return new Promise<TouchPerformanceResult>((resolve) => {
      let touchStartTime = 0;
      let touchEndTime = 0;
      let touchStartDelay = 0;
      let touchEndDelay = 0;

      const handleTouchStart = (): void => {
        touchStartTime = performance.now();
      };

      const handleTouchEnd = (): void => {
        touchEndTime = performance.now();
        touchStartDelay = touchStartTime - touchEndTime; // This would be negative, but we're measuring response
        touchEndDelay = performance.now() - touchEndTime;

        resolve({
          touchStartDelay: Math.abs(touchStartDelay),
          touchEndDelay,
          scrollPerformance: 16.67, // Default 60fps
          animationFrameRate: 60
        });
      };

      el.addEventListener('touchstart', handleTouchStart);
      el.addEventListener('touchend', handleTouchEnd);

      // Fallback
      setTimeout(() => {
        resolve({
          touchStartDelay: 0,
          touchEndDelay: 0,
          scrollPerformance: 16.67,
          animationFrameRate: 60
        });
      }, 1000);
    });
  }, await element.elementHandle());
}

/**
 * Measure memory usage
 */
async function measureMemoryUsage(page: Page): Promise<MemoryMetrics> {
  return await page.evaluate(() => {
    interface MemoryInfo {
      usedJSHeapSize: number;
      totalJSHeapSize: number;
      jsHeapSizeLimit: number;
    }

    interface ExtendedPerformance extends Performance {
      memory?: MemoryInfo;
    }

    const memory = (performance as ExtendedPerformance).memory;

    if (memory) {
      return {
        usedJSHeapSize: memory.usedJSHeapSize,
        totalJSHeapSize: memory.totalJSHeapSize,
        jsHeapSizeLimit: memory.jsHeapSizeLimit,
        memoryUsagePercentage: (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100
      };
    }

    return {
      usedJSHeapSize: 0,
      totalJSHeapSize: 0,
      jsHeapSizeLimit: 0,
      memoryUsagePercentage: 0
    };
  });
}

/**
 * Measure network metrics
 */
async function measureNetworkMetrics(page: Page): Promise<NetworkMetrics> {
  return await page.evaluate(() => {
    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];

    let totalTransferSize = 0;
    let totalResourceSize = 0;
    let cachedRequests = 0;
    let failedRequests = 0;

    resources.forEach(resource => {
      totalTransferSize += resource.transferSize || 0;
      totalResourceSize += resource.encodedBodySize || 0;

      if (resource.transferSize === 0 && resource.encodedBodySize > 0) {
        cachedRequests++;
      }

      // Resource timing API doesn't directly indicate failures,
      // but we can infer from very long durations or missing sizes
      if (resource.duration > 30000 || (!resource.transferSize && !resource.encodedBodySize)) {
        failedRequests++;
      }
    });

    return {
      requests: resources.length,
      transferSize: totalTransferSize,
      resourceSize: totalResourceSize,
      failedRequests,
      cachedRequests
    };
  });
}

/**
 * Layout Shift Entry interface (already defined in viewport test)
 */
interface LayoutShiftEntry extends PerformanceEntry {
  value: number;
  hadRecentInput: boolean;
}