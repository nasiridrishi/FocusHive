/**
 * Core Browser Compatibility Tests
 * Tests fundamental browser features and compatibility across different browsers
 */

import {expect, test} from '@playwright/test';
import {
  BrowserSpecificUtils,
  detectFeatureSupport,
  generateCompatibilityReport,
  getBrowserInfo,
  getBrowserPerformanceMetrics
} from './browser-helpers';

test.describe('Browser Compatibility Detection', () => {
  let compatibilityReport: ReturnType<typeof generateCompatibilityReport>;

  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should detect browser information correctly', async ({page}) => {
    const browserInfo = await getBrowserInfo(page);

    expect(browserInfo.name).toMatch(/chrome|firefox|safari|edge/);
    expect(browserInfo.version).toMatch(/^\d+/);
    expect(browserInfo.platform).toBeTruthy();
    expect(browserInfo.engine).toMatch(/blink|gecko|webkit/);
    expect(browserInfo.viewport.width).toBeGreaterThan(0);
    expect(browserInfo.viewport.height).toBeGreaterThan(0);

    // Log browser information for debugging
    console.log('Browser Info:', JSON.stringify(browserInfo, null, 2));
  });

  test('should detect JavaScript feature support', async ({page}) => {
    const features = await detectFeatureSupport(page);

    // Core ES6+ features should be supported in modern browsers
    expect(features['promises']).toBe(true);
    expect(features['fetch']).toBe(true);
    expect(features['arrow-functions']).toBe(true);
    expect(features['template-literals']).toBe(true);
    expect(features['destructuring']).toBe(true);
    expect(features['spread-operator']).toBe(true);

    // Modern async features
    expect(features['async-await']).toBe(true);

    console.log('JavaScript Features:', JSON.stringify(features, null, 2));
  });

  test('should detect Web API support', async ({page}) => {
    const features = await detectFeatureSupport(page);

    // Essential Web APIs for FocusHive
    expect(features['websocket']).toBe(true);
    expect(features['localstorage']).toBe(true);
    expect(features['sessionstorage']).toBe(true);

    // PWA-related APIs
    expect(features['serviceworker']).toBe(true);
    expect(features['indexeddb']).toBe(true);

    // Notification API (may vary by browser/context)
    if (features['notifications']) {
      expect(typeof features['notifications']).toBe('boolean');
    }

    console.log('Web API Support:', {
      websocket: features['websocket'],
      serviceworker: features['serviceworker'],
      indexeddb: features['indexeddb'],
      notifications: features['notifications'],
      geolocation: features['geolocation']
    });
  });

  test('should detect CSS feature support', async ({page}) => {
    const features = await detectFeatureSupport(page);

    // Modern layout features
    expect(features['css-flexbox']).toBe(true);
    expect(features['css-grid']).toBe(true);
    expect(features['css-custom-properties']).toBe(true);

    // Animation and visual features
    expect(features['css-transforms']).toBe(true);
    expect(features['css-animations']).toBe(true);
    expect(features['css-transitions']).toBe(true);

    console.log('CSS Features:', {
      flexbox: features['css-flexbox'],
      grid: features['css-grid'],
      customProperties: features['css-custom-properties'],
      transforms: features['css-transforms'],
      animations: features['css-animations']
    });
  });

  test('should measure performance metrics', async ({page}) => {
    const performanceMetrics = await getBrowserPerformanceMetrics(page);

    if (performanceMetrics) {
      expect(performanceMetrics.navigation.domInteractive).toBeGreaterThan(0);
      expect(performanceMetrics.navigation.domContentLoaded).toBeGreaterThanOrEqual(0);

      if (performanceMetrics.paint.firstPaint) {
        expect(performanceMetrics.paint.firstPaint).toBeGreaterThan(0);
      }

      if (performanceMetrics.paint.firstContentfulPaint) {
        expect(performanceMetrics.paint.firstContentfulPaint).toBeGreaterThan(0);
      }

      console.log('Performance Metrics:', JSON.stringify(performanceMetrics, null, 2));
    }
  });

  test('should test browser-specific features', async ({page, browserName}) => {
    const features = await detectFeatureSupport(page);

    // Chrome-specific features
    if (browserName === 'chromium') {
      expect(features['webp']).toBe(true);
      expect(features['webgl']).toBe(true);
      expect(features['webgl2']).toBe(true);
    }

    // Firefox-specific behavior
    if (browserName === 'firefox') {
      expect(features['webgl']).toBe(true);
      // Firefox may not support AVIF
      if (!features['avif']) {
        console.log('Firefox: AVIF not supported, fallback to WebP/JPEG required');
      }
    }

    // Safari-specific behavior
    if (browserName === 'webkit') {
      expect(features['websocket']).toBe(true);
      expect(features['serviceworker']).toBe(true);
      // Safari has different IndexedDB behavior
      if (features['indexeddb']) {
        console.log('Safari: IndexedDB supported with potential limitations');
      }
    }
  });

  test('should generate comprehensive compatibility report', async ({page}) => {
    const browserInfo = await getBrowserInfo(page);
    const features = await detectFeatureSupport(page);
    const performanceMetrics = await getBrowserPerformanceMetrics(page);

    // Mock media support data for report generation
    const mediaSupport = {
      images: {webp: true, avif: false, jpeg: true, png: true, gif: true, svg: true},
      video: {mp4: 'probably', webm: 'maybe', ogg: ''},
      audio: {mp3: 'probably', ogg: 'maybe', wav: 'probably', aac: 'probably'}
    };

    compatibilityReport = generateCompatibilityReport(
        browserInfo,
        features,
        performanceMetrics,
        mediaSupport
    );

    expect(compatibilityReport.browser).toBeDefined();
    expect(compatibilityReport.features).toBeDefined();
    expect(compatibilityReport.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);

    console.log('Compatibility Report Generated:', compatibilityReport.timestamp);
  });

  test('should test console error detection', async ({page}) => {
    const consoleErrors: string[] = [];
    const consoleWarnings: string[] = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      } else if (msg.type() === 'warning') {
        consoleWarnings.push(msg.text());
      }
    });

    // Navigate to different pages and check for console errors
    await page.goto('/login');
    await page.waitForTimeout(1000);

    await page.goto('/dashboard');
    await page.waitForTimeout(1000);

    // Log any console errors/warnings found
    if (consoleErrors.length > 0) {
      console.log('Console Errors Found:', consoleErrors);
    }

    if (consoleWarnings.length > 0) {
      console.log('Console Warnings Found:', consoleWarnings);
    }

    // Fail test if there are critical console errors
    const criticalErrors = consoleErrors.filter(error =>
        !error.includes('favicon') &&
        !error.includes('sourcemap') &&
        !error.includes('DevTools')
    );

    expect(criticalErrors).toHaveLength(0);
  });

  test('should test viewport and responsive behavior', async ({page}) => {
    const viewports = [
      {width: 1920, height: 1080, name: 'Desktop Large'},
      {width: 1366, height: 768, name: 'Desktop Standard'},
      {width: 768, height: 1024, name: 'Tablet Portrait'},
      {width: 1024, height: 768, name: 'Tablet Landscape'},
      {width: 375, height: 667, name: 'Mobile Portrait'},
      {width: 667, height: 375, name: 'Mobile Landscape'}
    ];

    for (const viewport of viewports) {
      await page.setViewportSize({width: viewport.width, height: viewport.height});
      await page.waitForTimeout(500); // Allow layout to settle

      // Test that the page renders correctly at this viewport
      const body = await page.locator('body');
      await expect(body).toBeVisible();

      // Check that no horizontal scrollbar appears (indicates responsive issues)
      const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      const clientWidth = await page.evaluate(() => document.documentElement.clientWidth);

      if (scrollWidth > clientWidth + 5) { // 5px tolerance
        console.log(`Horizontal scroll detected at ${viewport.name} (${viewport.width}x${viewport.height})`);
      }

      expect(scrollWidth).toBeLessThanOrEqual(clientWidth + 5);
    }
  });

  test('should test network conditions impact', async ({page, context}) => {
    // Test with slow network simulation
    await BrowserSpecificUtils.simulateSlowNetwork(context);

    const startTime = Date.now();
    await page.goto('/', {waitUntil: 'networkidle'});
    const loadTime = Date.now() - startTime;

    console.log(`Page load time with slow network: ${loadTime}ms`);

    // Verify the page still loads and functions
    await expect(page.locator('body')).toBeVisible();

    // Test that loading states are handled gracefully
    const loadingIndicator = page.locator('[data-testid="loading"]');
    if (await loadingIndicator.isVisible()) {
      console.log('Loading indicator properly displayed during slow network');
    }
  });

  test('should test accessibility features across browsers', async ({page}) => {
    // Test keyboard navigation
    await page.keyboard.press('Tab');
    const focusedElement = await page.evaluate(() => document.activeElement?.tagName);
    expect(focusedElement).toBeTruthy();

    // Test screen reader attributes
    const elementsWithAriaLabels = await page.locator('[aria-label]').count();
    expect(elementsWithAriaLabels).toBeGreaterThan(0);

    // Test color contrast (basic check)
    const backgroundColor = await page.evaluate(() => {
      return window.getComputedStyle(document.body).backgroundColor;
    });

    const color = await page.evaluate(() => {
      return window.getComputedStyle(document.body).color;
    });

    expect(backgroundColor).toBeTruthy();
    expect(color).toBeTruthy();

    console.log('Accessibility check - Background:', backgroundColor, 'Color:', color);
  });

  test.afterAll(async () => {
    // Save compatibility report to file for CI analysis
    if (compatibilityReport) {
      console.log('Final Compatibility Report:', JSON.stringify(compatibilityReport, null, 2));
    }
  });
});

test.describe('Browser-Specific Edge Cases', () => {
  test('should handle Safari-specific quirks', async ({page, browserName}) => {
    test.skip(browserName !== 'webkit', 'Safari-specific test');

    // Test Safari-specific IndexedDB limitations
    const supportsIndexedDB = await page.evaluate(() => {
      return typeof indexedDB !== 'undefined';
    });

    expect(supportsIndexedDB).toBe(true);

    // Test Safari private browsing detection
    const isPrivateBrowsing = await page.evaluate(async () => {
      try {
        const testKey = 'test-private-browsing';
        localStorage.setItem(testKey, 'test');
        localStorage.removeItem(testKey);
        return false;
      } catch {
        return true;
      }
    });

    console.log('Safari private browsing detected:', isPrivateBrowsing);
  });

  test('should handle Firefox-specific behavior', async ({page, browserName}) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific test');

    // Test Firefox-specific WebGL context
    const webglSupport = await page.evaluate(() => {
      const canvas = document.createElement('canvas');
      const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
      return !!gl;
    });

    expect(webglSupport).toBe(true);

    // Test Firefox CSP behavior
    const cspViolations: string[] = [];
    page.on('pageerror', (error) => {
      if (error.message.includes('Content Security Policy')) {
        cspViolations.push(error.message);
      }
    });

    await page.waitForTimeout(2000);

    if (cspViolations.length > 0) {
      console.log('Firefox CSP violations:', cspViolations);
    }
  });

  test('should handle Chrome-specific features', async ({page, browserName}) => {
    test.skip(browserName !== 'chromium', 'Chrome-specific test');

    // Test Chrome-specific APIs
    const chromeFeatures = await page.evaluate(() => {
      return {
        webp: (() => {
          const canvas = document.createElement('canvas');
          return canvas.toDataURL('image/webp').indexOf('webp') !== -1;
        })(),
        avif: (() => {
          const canvas = document.createElement('canvas');
          return canvas.toDataURL('image/avif').indexOf('avif') !== -1;
        })(),
        webgl2: (() => {
          const canvas = document.createElement('canvas');
          return !!canvas.getContext('webgl2');
        })()
      };
    });

    expect(chromeFeatures.webp).toBe(true);
    expect(chromeFeatures.webgl2).toBe(true);

    console.log('Chrome-specific features:', chromeFeatures);
  });
});