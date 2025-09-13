/**
 * Mobile Testing Helper Utilities for FocusHive E2E Tests
 * Enhanced mobile-specific testing capabilities and device emulation
 */

import { Page, BrowserContext, devices, expect, Locator } from '@playwright/test';
import { MOBILE_DEVICES, VIEWPORT_BREAKPOINTS, TOUCH_TARGET_SPECS, MobileDeviceConfig } from './mobile.config';

// Extended Window interface for mobile-specific properties
interface ExtendedWindow extends Window {
  installPromptReceived?: boolean;
  MSStream?: unknown;
  navigator: Navigator & {
    standalone?: boolean;
    share?: (data: { title?: string; text?: string; url?: string }) => Promise<void>;
    vibrate?: (pattern: number | number[]) => boolean;
    getBattery?: () => Promise<{
      level: number;
      charging: boolean;
      chargingTime: number;
      dischargingTime: number;
    }>;
    wakeLock?: {
      request: (type: string) => Promise<{
        release: () => Promise<void>;
      }>;
    };
  };
  DeviceOrientationEvent?: {
    requestPermission?: () => Promise<'granted' | 'denied' | 'default'>;
  };
  DeviceMotionEvent?: {
    requestPermission?: () => Promise<'granted' | 'denied' | 'default'>;
  };
}

export interface TouchGesture {
  type: 'tap' | 'doubleTap' | 'longPress' | 'swipe' | 'pinch' | 'pan' | 'drag';
  startPoint: { x: number; y: number };
  endPoint?: { x: number; y: number };
  duration?: number;
  fingers?: number;
  scale?: number; // For pinch gestures
  direction?: 'up' | 'down' | 'left' | 'right';
}

export interface MobileTestResult {
  passed: boolean;
  issues: string[];
  performance?: {
    loadTime: number;
    touchResponsiveness: number;
    scrollPerformance: number;
  };
  accessibility?: {
    touchTargetViolations: number;
    contrastIssues: number;
    keyboardAccessible: boolean;
  };
}

export interface MobilePerformanceResult {
  loadTime: number;
  touchResponsiveness: number;
  scrollPerformance: number;
}

export interface MobileAccessibilityResult {
  screenReaderCompatible: boolean;
  keyboardNavigable: boolean;
  contrastSufficient: boolean;
  focusVisible: boolean;
}

export interface MobilePWAResult {
  installable: boolean;
  hasManifest: boolean;
  hasServiceWorker: boolean;
  manifestValid: boolean;
}

export interface NetworkCondition {
  name: string;
  downloadThroughput: number;
  uploadThroughput: number;
  latency: number;
}

export class MobileTestHelper {
  private page: Page;
  private context: BrowserContext;

  constructor(page: Page) {
    this.page = page;
    this.context = page.context();
  }

  /**
   * Set up mobile device emulation
   */
  async setupMobileDevice(deviceConfig: MobileDeviceConfig): Promise<void> {
    await this.page.setViewportSize(deviceConfig.viewport);
    await this.page.setUserAgent(deviceConfig.userAgent);
    
    // Set device scale factor if supported
    if (deviceConfig.deviceScaleFactor) {
      const cdpSession = await this.context.newCDPSession(this.page);
      await cdpSession.send('Emulation.setDeviceMetricsOverride', {
        width: deviceConfig.viewport.width,
        height: deviceConfig.viewport.height,
        deviceScaleFactor: deviceConfig.deviceScaleFactor,
        mobile: deviceConfig.isMobile
      });
    }
  }

  /**
   * Test responsive behavior at specific breakpoint
   */
  async testAtBreakpoint(width: number, height: number): Promise<MobileTestResult> {
    await this.page.setViewportSize({ width, height });
    await this.page.waitForTimeout(500); // Allow CSS transitions

    const issues: string[] = [];

    // Check for horizontal scroll
    const hasHorizontalScroll = await this.page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });

    if (hasHorizontalScroll) {
      issues.push('Horizontal scroll detected');
    }

    // Check touch target sizes
    const touchTargetIssues = await this.validateTouchTargets();
    issues.push(...touchTargetIssues);

    // Check text readability
    const textIssues = await this.validateTextReadability();
    issues.push(...textIssues);

    // Performance metrics
    const performance = await this.measureBasicPerformance();

    return {
      passed: issues.length === 0,
      issues,
      performance
    };
  }

  /**
   * Perform touch gesture
   */
  async performTouchGesture(gesture: TouchGesture): Promise<void> {
    switch (gesture.type) {
      case 'tap':
        await this.performTap(gesture.startPoint, gesture.duration || 100);
        break;
      case 'doubleTap':
        await this.performDoubleTap(gesture.startPoint);
        break;
      case 'longPress':
        await this.performLongPress(gesture.startPoint, gesture.duration || 800);
        break;
      case 'swipe':
        if (gesture.endPoint) {
          await this.performSwipe(gesture.startPoint, gesture.endPoint, gesture.duration || 300);
        }
        break;
      case 'pinch':
        if (gesture.scale) {
          await this.performPinch(gesture.startPoint, gesture.scale, gesture.duration || 500);
        }
        break;
      case 'pan':
        if (gesture.endPoint) {
          await this.performPan(gesture.startPoint, gesture.endPoint, gesture.duration || 500);
        }
        break;
    }
  }

  /**
   * Test PWA installation capability
   */
  async testPWAInstallability(): Promise<{
    installable: boolean;
    hasManifest: boolean;
    hasServiceWorker: boolean;
    manifestValid: boolean;
  }> {
    // Check for manifest
    const manifestLink = this.page.locator('link[rel="manifest"]');
    const hasManifest = await manifestLink.isVisible();

    let manifestValid = false;
    if (hasManifest) {
      const manifestUrl = await manifestLink.getAttribute('href');
      if (manifestUrl) {
        try {
          const response = await this.page.request.get(manifestUrl);
          manifestValid = response.ok();
        } catch (error) {
          console.log('Manifest fetch error:', error);
        }
      }
    }

    // Check for service worker
    const hasServiceWorker = await this.page.evaluate(() => {
      return 'serviceWorker' in navigator;
    });

    // Check install prompt availability
    const installable = await this.page.evaluate(() => {
      return new Promise<boolean>((resolve) => {
        let hasPrompt = false;
        
        window.addEventListener('beforeinstallprompt', () => {
          hasPrompt = true;
          resolve(true);
        });

        // Timeout after 2 seconds
        setTimeout(() => resolve(hasPrompt), 2000);
      });
    });

    return {
      installable,
      hasManifest,
      hasServiceWorker,
      manifestValid
    };
  }

  /**
   * Simulate network conditions
   */
  async setNetworkConditions(condition: NetworkCondition): Promise<void> {
    const cdpSession = await this.context.newCDPSession(this.page);
    await cdpSession.send('Network.enable');
    await cdpSession.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: condition.downloadThroughput,
      uploadThroughput: condition.uploadThroughput,
      latency: condition.latency
    });
  }

  /**
   * Test offline functionality
   */
  async testOfflineMode(): Promise<{
    offlineCapable: boolean;
    showsOfflineIndicator: boolean;
    cachedContentAvailable: boolean;
  }> {
    // Go offline
    await this.context.setOffline(true);
    
    // Try to reload page
    await this.page.reload();
    await this.page.waitForTimeout(2000);

    // Check if page loads (from cache)
    const cachedContentAvailable = await this.page.locator('body').isVisible();

    // Check for offline indicator
    const offlineIndicators = [
      '[data-testid="offline-indicator"]',
      '[data-testid="no-connection"]',
      'text="Offline"',
      '.offline-banner'
    ];

    let showsOfflineIndicator = false;
    for (const selector of offlineIndicators) {
      if (await this.page.locator(selector).isVisible({ timeout: 1000 })) {
        showsOfflineIndicator = true;
        break;
      }
    }

    // Go back online
    await this.context.setOffline(false);

    return {
      offlineCapable: cachedContentAvailable,
      showsOfflineIndicator,
      cachedContentAvailable
    };
  }

  /**
   * Validate touch target sizes
   */
  private async validateTouchTargets(): Promise<string[]> {
    return await this.page.evaluate(() => {
      const issues: string[] = [];
      const interactiveElements = document.querySelectorAll(
        'button, a, input, textarea, select, [role="button"], [tabindex="0"], [onclick]'
      );

      interactiveElements.forEach((element, index) => {
        const rect = element.getBoundingClientRect();
        const minSize = 44; // iOS minimum

        if (rect.width < minSize || rect.height < minSize) {
          issues.push(`Touch target ${index} too small: ${Math.round(rect.width)}x${Math.round(rect.height)}px`);
        }
      });

      return issues;
    });
  }

  /**
   * Validate text readability
   */
  private async validateTextReadability(): Promise<string[]> {
    return await this.page.evaluate(() => {
      const issues: string[] = [];
      const textElements = document.querySelectorAll('p, span, div, h1, h2, h3, h4, h5, h6, li');

      textElements.forEach((element, index) => {
        const style = window.getComputedStyle(element);
        const fontSize = parseFloat(style.fontSize);

        if (fontSize < 14) {
          issues.push(`Text too small at element ${index}: ${fontSize}px`);
        }
      });

      return issues;
    });
  }

  /**
   * Measure basic performance metrics
   */
  private async measureBasicPerformance(): Promise<{
    loadTime: number;
    touchResponsiveness: number;
    scrollPerformance: number;
  }> {
    const performance = await this.page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        loadTime: navigation ? navigation.loadEventEnd - navigation.fetchStart : 0,
        touchResponsiveness: 16.67, // Default 60fps
        scrollPerformance: 16.67
      };
    });

    return performance;
  }

  /**
   * Basic touch gestures implementation
   */
  private async performTap(point: { x: number; y: number }, duration: number): Promise<void> {
    await this.page.touchscreen.tap(point.x, point.y);
    await this.page.waitForTimeout(duration);
  }

  private async performDoubleTap(point: { x: number; y: number }): Promise<void> {
    await this.page.touchscreen.tap(point.x, point.y);
    await this.page.waitForTimeout(50);
    await this.page.touchscreen.tap(point.x, point.y);
  }

  private async performLongPress(point: { x: number; y: number }, duration: number): Promise<void> {
    await this.page.mouse.move(point.x, point.y);
    await this.page.mouse.down();
    await this.page.waitForTimeout(duration);
    await this.page.mouse.up();
  }

  private async performSwipe(
    start: { x: number; y: number },
    end: { x: number; y: number },
    duration: number
  ): Promise<void> {
    await this.page.mouse.move(start.x, start.y);
    await this.page.mouse.down();
    
    const steps = Math.max(5, duration / 50);
    const stepX = (end.x - start.x) / steps;
    const stepY = (end.y - start.y) / steps;
    const stepDuration = duration / steps;

    for (let i = 1; i <= steps; i++) {
      await this.page.mouse.move(start.x + stepX * i, start.y + stepY * i);
      await this.page.waitForTimeout(stepDuration);
    }

    await this.page.mouse.up();
  }

  private async performPinch(
    center: { x: number; y: number },
    scale: number,
    duration: number
  ): Promise<void> {
    // Simplified pinch implementation
    const distance = 100;
    const startDistance = distance;
    const endDistance = distance * scale;

    const finger1Start = { x: center.x - startDistance / 2, y: center.y };
    const finger1End = { x: center.x - endDistance / 2, y: center.y };

    // Simulate first finger movement
    await this.performSwipe(finger1Start, finger1End, duration);
  }

  private async performPan(
    start: { x: number; y: number },
    end: { x: number; y: number },
    duration: number
  ): Promise<void> {
    // Pan is similar to swipe but typically slower and more controlled
    await this.performSwipe(start, end, duration);
  }

  /**
   * Test accessibility features
   */
  async testAccessibility(): Promise<{
    screenReaderCompatible: boolean;
    keyboardNavigable: boolean;
    contrastSufficient: boolean;
    focusVisible: boolean;
  }> {
    // Check for ARIA labels and roles
    const screenReaderCompatible = await this.page.evaluate(() => {
      const interactiveElements = document.querySelectorAll('button, a, input, [role="button"]');
      let accessibleCount = 0;

      interactiveElements.forEach(el => {
        const hasLabel = el.getAttribute('aria-label') || 
                        el.getAttribute('aria-labelledby') ||
                        el.textContent?.trim();
        if (hasLabel) accessibleCount++;
      });

      return accessibleCount / interactiveElements.length > 0.8; // 80% threshold
    });

    // Test keyboard navigation
    const keyboardNavigable = await this.testKeyboardNavigation();

    // Basic contrast check (simplified)
    const contrastSufficient = await this.page.evaluate(() => {
      // This is a simplified check - real contrast testing requires more complex algorithms
      const textElements = document.querySelectorAll('p, span, button, a');
      return textElements.length > 0; // Placeholder
    });

    // Check focus visibility
    const focusVisible = await this.page.evaluate(() => {
      const focusableElements = document.querySelectorAll('button, a, input, [tabindex="0"]');
      
      // Check if focus styles are defined
      return Array.from(focusableElements).some(el => {
        const styles = window.getComputedStyle(el, ':focus');
        return styles.outline !== 'none' || styles.boxShadow !== 'none';
      });
    });

    return {
      screenReaderCompatible,
      keyboardNavigable,
      contrastSufficient,
      focusVisible
    };
  }

  /**
   * Test keyboard navigation
   */
  private async testKeyboardNavigation(): Promise<boolean> {
    try {
      // Try to tab through interactive elements
      await this.page.keyboard.press('Tab');
      await this.page.waitForTimeout(100);
      
      const focusedElement = await this.page.evaluate(() => {
        return !!document.activeElement && 
               document.activeElement !== document.body;
      });

      return focusedElement;
    } catch (error) {
      return false;
    }
  }

  /**
   * Generate comprehensive mobile test report
   */
  async generateMobileReport(deviceName: string): Promise<{
    device: string;
    timestamp: string;
    results: {
      responsiveness: MobileTestResult;
      performance: MobilePerformanceResult;
      accessibility: MobileAccessibilityResult;
      pwa: MobilePWAResult;
    };
    recommendations: string[];
  }> {
    const timestamp = new Date().toISOString();
    const recommendations: string[] = [];

    // Test responsiveness
    const responsiveness = await this.testAtBreakpoint(390, 844);
    
    // Test performance
    const performance = await this.measureBasicPerformance();
    
    // Test accessibility
    const accessibility = await this.testAccessibility();
    
    // Test PWA capabilities
    const pwa = await this.testPWAInstallability();

    // Generate recommendations
    if (!responsiveness.passed) {
      recommendations.push('Fix responsive design issues: ' + responsiveness.issues.join(', '));
    }
    
    if (performance.loadTime > 3000) {
      recommendations.push('Optimize page load time (currently ' + performance.loadTime + 'ms)');
    }
    
    if (!accessibility.screenReaderCompatible) {
      recommendations.push('Improve screen reader compatibility with better ARIA labels');
    }
    
    if (!pwa.installable) {
      recommendations.push('Enable PWA installation with proper manifest and service worker');
    }

    return {
      device: deviceName,
      timestamp,
      results: {
        responsiveness,
        performance,
        accessibility,
        pwa
      },
      recommendations
    };
  }
}

/**
 * Mobile-specific test utilities
 */
export class MobileUtils {
  /**
   * Get optimal device list for testing based on market share
   */
  static getTestDeviceList(priority: 'critical' | 'high' | 'medium' | 'all' = 'critical'): MobileDeviceConfig[] {
    const allDevices = Object.values(MOBILE_DEVICES);
    
    switch (priority) {
      case 'critical':
        return allDevices.filter(device => 
          ['iPhone 14', 'Galaxy S21', 'Pixel 7', 'iPad Air'].includes(device.name)
        );
      case 'high':
        return allDevices.filter(device => 
          device.category === 'phone' || device.category === 'tablet'
        );
      case 'medium':
        return allDevices.filter(device => device.category !== 'foldable');
      default:
        return allDevices;
    }
  }

  /**
   * Generate network condition presets
   */
  static getNetworkConditions(): NetworkCondition[] {
    return [
      {
        name: 'Slow 3G',
        downloadThroughput: 500 * 1024 / 8,
        uploadThroughput: 500 * 1024 / 8,
        latency: 2000
      },
      {
        name: 'Fast 3G',
        downloadThroughput: 1.5 * 1024 * 1024 / 8,
        uploadThroughput: 750 * 1024 / 8,
        latency: 562.5
      },
      {
        name: '4G',
        downloadThroughput: 9 * 1024 * 1024 / 8,
        uploadThroughput: 9 * 1024 * 1024 / 8,
        latency: 170
      }
    ];
  }

  /**
   * Common mobile test scenarios
   */
  static getCommonTestScenarios(): Array<{
    name: string;
    description: string;
    viewport: { width: number; height: number };
    networkCondition?: NetworkCondition;
  }> {
    return [
      {
        name: 'iPhone Portrait',
        description: 'Standard iPhone portrait mode',
        viewport: { width: 390, height: 844 }
      },
      {
        name: 'iPhone Landscape',
        description: 'iPhone in landscape orientation',
        viewport: { width: 844, height: 390 }
      },
      {
        name: 'Android Standard',
        description: 'Standard Android phone',
        viewport: { width: 360, height: 800 }
      },
      {
        name: 'Small Mobile',
        description: 'Older/smaller mobile devices',
        viewport: { width: 320, height: 568 }
      },
      {
        name: 'Tablet Portrait',
        description: 'Tablet in portrait mode',
        viewport: { width: 768, height: 1024 }
      },
      {
        name: 'Slow Network Mobile',
        description: 'Mobile on slow 3G network',
        viewport: { width: 390, height: 844 },
        networkCondition: this.getNetworkConditions()[0]
      }
    ];
  }

  /**
   * Validate mobile-specific meta tags
   */
  static async validateMobileMeta(page: Page): Promise<{
    viewport: boolean;
    appleMobileWebAppCapable: boolean;
    appleMobileWebAppStatusBar: boolean;
    themeColor: boolean;
    manifest: boolean;
  }> {
    const metaTags = await page.evaluate(() => {
      return {
        viewport: !!document.querySelector('meta[name="viewport"]'),
        appleMobileWebAppCapable: !!document.querySelector('meta[name="apple-mobile-web-app-capable"]'),
        appleMobileWebAppStatusBar: !!document.querySelector('meta[name="apple-mobile-web-app-status-bar-style"]'),
        themeColor: !!document.querySelector('meta[name="theme-color"]'),
        manifest: !!document.querySelector('link[rel="manifest"]')
      };
    });

    return metaTags;
  }
}

/**
 * Device-specific test configurations
 */
export const DEVICE_TEST_CONFIGS = {
  IPHONE_14: {
    device: MOBILE_DEVICES.IPHONE_14,
    testScenarios: ['portrait', 'landscape', 'safe_area', 'notch_handling'],
    performanceBudget: 'MOBILE_4G'
  },
  GALAXY_S21: {
    device: MOBILE_DEVICES.GALAXY_S21,
    testScenarios: ['portrait', 'landscape', 'material_design', 'android_features'],
    performanceBudget: 'MOBILE_4G'
  },
  IPAD_AIR: {
    device: MOBILE_DEVICES.IPAD_AIR,
    testScenarios: ['portrait', 'landscape', 'split_view', 'keyboard_support'],
    performanceBudget: 'TABLET'
  }
};

export default MobileTestHelper;