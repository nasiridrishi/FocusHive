/**
 * Core Web Vitals Performance Tests
 * 
 * Tests for measuring and validating Core Web Vitals metrics:
 * - First Contentful Paint (FCP)
 * - Largest Contentful Paint (LCP)
 * - Time to Interactive (TTI)
 * - Cumulative Layout Shift (CLS)
 * - First Input Delay (FID)
 * - Time to First Byte (TTFB)
 * 
 * Tests across different network conditions and device types
 */

import { test, expect, Page } from '@playwright/test';
import { PerformanceTestHelper, CoreWebVitals } from './performance-helpers';
import { performanceCollector, PerformanceMetrics } from './performance-metrics';
import { AuthHelper } from '../../helpers/auth.helper';

// Test configuration
const TEST_CONFIG = {
  pages: [
    { name: 'Landing Page', url: '/', requiresAuth: false },
    { name: 'Dashboard', url: '/dashboard', requiresAuth: true },
    { name: 'Hive List', url: '/hives', requiresAuth: true },
    { name: 'Profile', url: '/profile', requiresAuth: true },
    { name: 'Settings', url: '/settings', requiresAuth: true }
  ],
  networkConditions: [
    { name: 'Fast 3G', condition: 'fast-3g' as const },
    { name: 'Slow 3G', condition: 'slow-3g' as const }
  ],
  devices: [
    { name: 'Desktop', viewport: { width: 1920, height: 1080 } },
    { name: 'Tablet', viewport: { width: 1024, height: 768 } },
    { name: 'Mobile', viewport: { width: 375, height: 667 } }
  ],
  thresholds: {
    fcp: { good: 1800, poor: 3000 },
    lcp: { good: 2500, poor: 4000 },
    tti: { good: 3800, poor: 5800 },
    cls: { good: 0.1, poor: 0.25 },
    ttfb: { good: 800, poor: 1500 }
  }
};

test.describe('Core Web Vitals Performance Tests', () => {
  let authHelper: AuthHelper;
  let performanceHelper: PerformanceTestHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    performanceHelper = new PerformanceTestHelper(page);
    await performanceHelper.initializePerformanceMonitoring();
  });

  test.afterEach(async () => {
    await performanceHelper.cleanup();
  });

  // Test each page under optimal conditions (WiFi, Desktop)
  for (const pageConfig of TEST_CONFIG.pages) {
    test(`Core Web Vitals - ${pageConfig.name} (Optimal Conditions)`, async ({ page }) => {
      performanceCollector.startTest(`CWV - ${pageConfig.name} - Optimal`);

      // Authenticate if required
      if (pageConfig.requiresAuth) {
        await authHelper.loginWithTestUser();
      }

      // Navigate to page
      const navigationPromise = page.goto(`http://localhost:3000${pageConfig.url}`);
      
      // Wait for page to be interactive
      await Promise.all([
        navigationPromise,
        page.waitForLoadState('networkidle'),
        page.waitForLoadState('domcontentloaded')
      ]);

      // Wait a bit more for dynamic content
      await page.waitForTimeout(2000);

      // Measure Core Web Vitals
      const coreWebVitals = await performanceHelper.measureCoreWebVitals();

      // Collect additional metrics
      const memoryUsage = await performanceHelper.getMemoryUsage();

      // Validate thresholds
      expect(coreWebVitals.fcp, `FCP should be under ${TEST_CONFIG.thresholds.fcp.good}ms`).toBeLessThan(TEST_CONFIG.thresholds.fcp.good);
      expect(coreWebVitals.lcp, `LCP should be under ${TEST_CONFIG.thresholds.lcp.good}ms`).toBeLessThan(TEST_CONFIG.thresholds.lcp.good);
      expect(coreWebVitals.tti, `TTI should be under ${TEST_CONFIG.thresholds.tti.good}ms`).toBeLessThan(TEST_CONFIG.thresholds.tti.good);
      expect(coreWebVitals.cls, `CLS should be under ${TEST_CONFIG.thresholds.cls.good}`).toBeLessThan(TEST_CONFIG.thresholds.cls.good);
      expect(coreWebVitals.ttfb, `TTFB should be under ${TEST_CONFIG.thresholds.ttfb.good}ms`).toBeLessThan(TEST_CONFIG.thresholds.ttfb.good);

      // Record results
      const metrics: PerformanceMetrics = {
        coreWebVitals,
        memoryMetrics: {
          initialUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          peakUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          finalUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          leakDetected: false,
          leakRate: 0,
          gcCount: 0,
          heapSize: memoryUsage.totalJSHeapSize / 1024 / 1024
        }
      };

      const result = performanceCollector.endTest(`CWV - ${pageConfig.name} - Optimal`, metrics);
      
      // Log detailed results
      console.log(`📊 ${pageConfig.name} Performance Metrics:`);
      console.log(`  FCP: ${coreWebVitals.fcp}ms`);
      console.log(`  LCP: ${coreWebVitals.lcp}ms`);
      console.log(`  TTI: ${coreWebVitals.tti}ms`);
      console.log(`  CLS: ${coreWebVitals.cls.toFixed(3)}`);
      console.log(`  TTFB: ${coreWebVitals.ttfb}ms`);
      console.log(`  Memory: ${(memoryUsage.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`);

      if (result.violations.length > 0) {
        console.warn(`⚠️ Performance violations detected:`, result.violations);
      }
    });
  }

  // Test Core Web Vitals under different network conditions
  for (const networkCondition of TEST_CONFIG.networkConditions) {
    test(`Core Web Vitals - Dashboard under ${networkCondition.name}`, async ({ page }) => {
      performanceCollector.startTest(`CWV - Dashboard - ${networkCondition.name}`);

      // Set up network throttling
      await performanceHelper.simulateNetworkConditions(networkCondition.condition);

      // Authenticate
      await authHelper.loginWithTestUser();

      // Navigate to dashboard
      const startTime = Date.now();
      await page.goto('http://localhost:3000/dashboard');
      await page.waitForLoadState('networkidle', { timeout: 30000 });

      // Measure Core Web Vitals
      const coreWebVitals = await performanceHelper.measureCoreWebVitals();
      const memoryUsage = await performanceHelper.getMemoryUsage();

      // Adjust expectations for slow networks
      const adjustedThresholds = networkCondition.condition === 'slow-3g' ? 
        {
          fcp: TEST_CONFIG.thresholds.fcp.poor * 1.5,
          lcp: TEST_CONFIG.thresholds.lcp.poor * 1.5,
          tti: TEST_CONFIG.thresholds.tti.poor * 1.5,
          cls: TEST_CONFIG.thresholds.cls.poor,
          ttfb: TEST_CONFIG.thresholds.ttfb.poor * 2
        } : {
          fcp: TEST_CONFIG.thresholds.fcp.poor,
          lcp: TEST_CONFIG.thresholds.lcp.poor,
          tti: TEST_CONFIG.thresholds.tti.poor,
          cls: TEST_CONFIG.thresholds.cls.poor,
          ttfb: TEST_CONFIG.thresholds.ttfb.poor
        };

      // Validate with adjusted thresholds
      expect(coreWebVitals.fcp, `FCP under ${networkCondition.name} should be reasonable`).toBeLessThan(adjustedThresholds.fcp);
      expect(coreWebVitals.lcp, `LCP under ${networkCondition.name} should be reasonable`).toBeLessThan(adjustedThresholds.lcp);
      expect(coreWebVitals.cls, `CLS should remain stable regardless of network`).toBeLessThan(adjustedThresholds.cls);

      // Record results
      const metrics: PerformanceMetrics = {
        coreWebVitals,
        memoryMetrics: {
          initialUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          peakUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          finalUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          leakDetected: false,
          leakRate: 0,
          gcCount: 0,
          heapSize: memoryUsage.totalJSHeapSize / 1024 / 1024
        },
        deviceMetrics: {
          deviceType: 'desktop',
          cpuThrottling: 1,
          networkCondition: networkCondition.name
        }
      };

      const result = performanceCollector.endTest(`CWV - Dashboard - ${networkCondition.name}`, metrics);
      
      console.log(`📊 Dashboard Performance on ${networkCondition.name}:`);
      console.log(`  FCP: ${coreWebVitals.fcp}ms (threshold: ${adjustedThresholds.fcp}ms)`);
      console.log(`  LCP: ${coreWebVitals.lcp}ms (threshold: ${adjustedThresholds.lcp}ms)`);
      console.log(`  CLS: ${coreWebVitals.cls.toFixed(3)}`);
    });
  }

  // Test Core Web Vitals on different device types
  for (const device of TEST_CONFIG.devices) {
    test(`Core Web Vitals - Dashboard on ${device.name}`, async ({ page }) => {
      performanceCollector.startTest(`CWV - Dashboard - ${device.name}`);

      // Set viewport
      await page.setViewportSize(device.viewport);

      // Authenticate
      await authHelper.loginWithTestUser();

      // Navigate to dashboard
      await page.goto('http://localhost:3000/dashboard');
      await page.waitForLoadState('networkidle');

      // Measure Core Web Vitals
      const coreWebVitals = await performanceHelper.measureCoreWebVitals();
      const memoryUsage = await performanceHelper.getMemoryUsage();

      // Mobile devices might have slightly different performance characteristics
      const deviceMultiplier = device.name === 'Mobile' ? 1.3 : 1.0;
      
      expect(coreWebVitals.fcp, `FCP on ${device.name} should be reasonable`).toBeLessThan(TEST_CONFIG.thresholds.fcp.good * deviceMultiplier);
      expect(coreWebVitals.lcp, `LCP on ${device.name} should be reasonable`).toBeLessThan(TEST_CONFIG.thresholds.lcp.good * deviceMultiplier);
      expect(coreWebVitals.cls, `CLS on ${device.name} should be minimal`).toBeLessThan(TEST_CONFIG.thresholds.cls.good);

      // Record results
      const metrics: PerformanceMetrics = {
        coreWebVitals,
        memoryMetrics: {
          initialUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          peakUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          finalUsage: memoryUsage.usedJSHeapSize / 1024 / 1024,
          leakDetected: false,
          leakRate: 0,
          gcCount: 0,
          heapSize: memoryUsage.totalJSHeapSize / 1024 / 1024
        },
        deviceMetrics: {
          deviceType: device.name.toLowerCase() as 'desktop' | 'tablet' | 'mobile',
          cpuThrottling: 1,
          networkCondition: 'WiFi'
        }
      };

      const result = performanceCollector.endTest(`CWV - Dashboard - ${device.name}`, metrics);
      
      console.log(`📱 Dashboard Performance on ${device.name} (${device.viewport.width}x${device.viewport.height}):`);
      console.log(`  FCP: ${coreWebVitals.fcp}ms`);
      console.log(`  LCP: ${coreWebVitals.lcp}ms`);
      console.log(`  CLS: ${coreWebVitals.cls.toFixed(3)}`);
    });
  }

  // Test First Input Delay (FID) with user interactions
  test('First Input Delay (FID) Measurement', async ({ page }) => {
    performanceCollector.startTest('FID - User Interactions');

    await authHelper.loginWithTestUser();
    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle');

    // Measure FID by performing user interactions
    const fidMeasurement = await page.evaluate(() => {
      return new Promise<number>((resolve) => {
        let fidValue = 0;
        
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries() as PerformanceEventTiming[];
          for (const entry of entries) {
            if (entry.name === 'first-input') {
              fidValue = entry.processingStart - entry.startTime;
              resolve(fidValue);
              return;
            }
          }
        });
        
        try {
          observer.observe({ type: 'first-input', buffered: true });
        } catch {
          // Fallback if first-input is not supported
          resolve(0);
        }
        
        // Timeout fallback
        setTimeout(() => resolve(fidValue), 5000);
      });
    });

    // Perform a click interaction to trigger FID measurement
    await page.click('button:first-of-type', { force: true });
    await page.waitForTimeout(1000);

    // Get final Core Web Vitals including FID
    const coreWebVitals = await performanceHelper.measureCoreWebVitals();
    coreWebVitals.fid = fidMeasurement;

    // Validate FID
    if (coreWebVitals.fid && coreWebVitals.fid > 0) {
      expect(coreWebVitals.fid, 'First Input Delay should be minimal').toBeLessThan(100);
    }

    // Record results
    const metrics: PerformanceMetrics = {
      coreWebVitals
    };

    const result = performanceCollector.endTest('FID - User Interactions', metrics);
    
    console.log(`👆 First Input Delay: ${coreWebVitals.fid}ms`);
  });

  // Test performance with CPU throttling (simulating low-end devices)
  test('Core Web Vitals with CPU Throttling', async ({ page }) => {
    performanceCollector.startTest('CWV - CPU Throttling');

    // Apply CPU throttling (4x slowdown)
    await performanceHelper.simulateCPUThrottling(4);

    await authHelper.loginWithTestUser();
    await page.goto('http://localhost:3000/dashboard');
    await page.waitForLoadState('networkidle', { timeout: 30000 });

    // Measure Core Web Vitals under CPU constraint
    const coreWebVitals = await performanceHelper.measureCoreWebVitals();
    const memoryUsage = await performanceHelper.getMemoryUsage();

    // Adjusted thresholds for CPU throttling
    const throttledThresholds = {
      fcp: TEST_CONFIG.thresholds.fcp.poor * 2,
      lcp: TEST_CONFIG.thresholds.lcp.poor * 2,
      tti: TEST_CONFIG.thresholds.tti.poor * 2,
      cls: TEST_CONFIG.thresholds.cls.poor,
      ttfb: TEST_CONFIG.thresholds.ttfb.good // TTFB shouldn't be affected by CPU
    };

    expect(coreWebVitals.fcp, 'FCP should be reasonable even with CPU throttling').toBeLessThan(throttledThresholds.fcp);
    expect(coreWebVitals.lcp, 'LCP should be reasonable even with CPU throttling').toBeLessThan(throttledThresholds.lcp);
    expect(coreWebVitals.cls, 'CLS should remain stable with CPU throttling').toBeLessThan(throttledThresholds.cls);

    // Record results
    const metrics: PerformanceMetrics = {
      coreWebVitals,
      deviceMetrics: {
        deviceType: 'desktop',
        cpuThrottling: 4,
        networkCondition: 'WiFi'
      }
    };

    const result = performanceCollector.endTest('CWV - CPU Throttling', metrics);
    
    console.log(`🐌 Performance with 4x CPU throttling:`);
    console.log(`  FCP: ${coreWebVitals.fcp}ms`);
    console.log(`  LCP: ${coreWebVitals.lcp}ms`);
    console.log(`  TTI: ${coreWebVitals.tti}ms`);
  });

  // Generate comprehensive report after all tests
  test.afterAll(async () => {
    const report = performanceCollector.generateReport();
    console.log(report);
    
    // Export results for CI/CD integration
    const results = performanceCollector.exportResults();
    
    // Write results to file for artifact collection
    const fs = require('fs').promises;
    const path = require('path');
    
    await fs.writeFile(
      path.join(__dirname, '../../reports/core-web-vitals-report.json'),
      JSON.stringify(results, null, 2)
    );
    
    console.log('📝 Performance report saved to core-web-vitals-report.json');
  });
});