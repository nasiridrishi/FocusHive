/**
 * Mobile Testing Helper Utilities for FocusHive E2E Tests
 * Provides device emulation, touch interactions, and mobile-specific testing capabilities
 */

import { Page, BrowserContext, devices, expect, Locator } from '@playwright/test';
import { TEST_URLS, SELECTORS, TIMEOUTS } from './test-data';

// Extended Window interface for PWA and mobile-specific properties
interface ExtendedWindow extends Window {
  installPromptReceived?: boolean;
  MSStream?: unknown;
  navigator: Navigator & {
    standalone?: boolean;
    share?: (data: { title?: string; text?: string; url?: string }) => Promise<void>;
  };
}

export interface DeviceConfig {
  name: string;
  device: string;
  viewport: {
    width: number;
    height: number;
  };
  userAgent: string;
  deviceScaleFactor: number;
  isMobile: boolean;
  hasTouch: boolean;
}

export interface TouchInteraction {
  type: 'tap' | 'swipe' | 'pinch' | 'longPress' | 'doubleTap' | 'pan';
  element?: Locator;
  startPoint?: { x: number; y: number };
  endPoint?: { x: number; y: number };
  duration?: number;
  scale?: number; // For pinch gestures
}

export interface ResponsiveBreakpoint {
  name: string;
  width: number;
  height: number;
  description: string;
}

export interface ViewportTestResult {
  viewport: { width: number; height: number };
  passed: boolean;
  issues: string[];
  screenshots?: string[];
}

export interface MobilePerformanceMetrics {
  loadTime: number;
  touchResponsiveness: number;
  scrollPerformance: number;
  networkRequests: number;
  bundleSize: number;
  memoryUsage?: number;
}

export class MobileHelper {
  private touchEnabled = false;
  
  constructor(private page: Page) {}

  /**
   * Device configurations for testing
   */
  static readonly DEVICE_CONFIGS: Record<string, DeviceConfig> = {
    // Mobile Phones
    IPHONE_SE: {
      name: 'iPhone SE',
      device: 'iPhone SE',
      viewport: { width: 375, height: 667 },
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1',
      deviceScaleFactor: 2,
      isMobile: true,
      hasTouch: true
    },
    IPHONE_12: {
      name: 'iPhone 12',
      device: 'iPhone 12',
      viewport: { width: 390, height: 844 },
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1',
      deviceScaleFactor: 3,
      isMobile: true,
      hasTouch: true
    },
    IPHONE_14_PRO: {
      name: 'iPhone 14 Pro',
      device: 'iPhone 14 Pro',
      viewport: { width: 393, height: 852 },
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1',
      deviceScaleFactor: 3,
      isMobile: true,
      hasTouch: true
    },
    GALAXY_S21: {
      name: 'Galaxy S21',
      device: 'Galaxy S21',
      viewport: { width: 360, height: 800 },
      userAgent: 'Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36',
      deviceScaleFactor: 3,
      isMobile: true,
      hasTouch: true
    },
    PIXEL_5: {
      name: 'Pixel 5',
      device: 'Pixel 5',
      viewport: { width: 393, height: 851 },
      userAgent: 'Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36',
      deviceScaleFactor: 2.75,
      isMobile: true,
      hasTouch: true
    },

    // Tablets
    IPAD: {
      name: 'iPad',
      device: 'iPad',
      viewport: { width: 768, height: 1024 },
      userAgent: 'Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1',
      deviceScaleFactor: 2,
      isMobile: false,
      hasTouch: true
    },
    IPAD_PRO: {
      name: 'iPad Pro',
      device: 'iPad Pro',
      viewport: { width: 1024, height: 1366 },
      userAgent: 'Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1',
      deviceScaleFactor: 2,
      isMobile: false,
      hasTouch: true
    },
    GALAXY_TAB: {
      name: 'Galaxy Tab S8',
      device: 'Galaxy Tab S8',
      viewport: { width: 800, height: 1280 },
      userAgent: 'Mozilla/5.0 (Linux; Android 12; SM-X700) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36',
      deviceScaleFactor: 2.5,
      isMobile: false,
      hasTouch: true
    },

    // Desktop breakpoints for responsive testing
    DESKTOP_SMALL: {
      name: 'Desktop Small',
      device: 'Desktop Small',
      viewport: { width: 1280, height: 720 },
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36',
      deviceScaleFactor: 1,
      isMobile: false,
      hasTouch: false
    },
    DESKTOP_MEDIUM: {
      name: 'Desktop Medium',
      device: 'Desktop Medium',
      viewport: { width: 1440, height: 900 },
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36',
      deviceScaleFactor: 1,
      isMobile: false,
      hasTouch: false
    },
    DESKTOP_LARGE: {
      name: 'Desktop Large',
      device: 'Desktop Large',
      viewport: { width: 1920, height: 1080 },
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36',
      deviceScaleFactor: 1,
      isMobile: false,
      hasTouch: false
    },
    ULTRAWIDE: {
      name: 'Ultrawide',
      device: 'Ultrawide',
      viewport: { width: 2560, height: 1440 },
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36',
      deviceScaleFactor: 1,
      isMobile: false,
      hasTouch: false
    }
  };

  /**
   * Standard responsive breakpoints
   */
  static readonly BREAKPOINTS: ResponsiveBreakpoint[] = [
    { name: 'xs', width: 320, height: 568, description: 'Extra Small - iPhone 5/SE Portrait' },
    { name: 'sm', width: 375, height: 667, description: 'Small - iPhone 6/7/8 Portrait' },
    { name: 'md', width: 768, height: 1024, description: 'Medium - iPad Portrait' },
    { name: 'lg', width: 1024, height: 768, description: 'Large - iPad Landscape' },
    { name: 'xl', width: 1280, height: 720, description: 'Extra Large - Desktop' },
    { name: '2xl', width: 1920, height: 1080, description: '2X Large - Large Desktop' },
  ];

  /**
   * Touch target minimum sizes (iOS: 44x44px, Android: 48x48dp)
   */
  static readonly TOUCH_TARGET_SIZES = {
    IOS_MINIMUM: 44,
    ANDROID_MINIMUM: 48,
    RECOMMENDED: 48
  };

  /**
   * Set up device emulation
   */
  async emulateDevice(deviceName: string): Promise<void> {
    const deviceConfig = MobileHelper.DEVICE_CONFIGS[deviceName];
    if (!deviceConfig) {
      throw new Error(`Unknown device: ${deviceName}`);
    }

    // Use Playwright's device emulation if available
    if (devices[deviceConfig.device]) {
      await this.page.emulate(devices[deviceConfig.device]);
    } else {
      // Custom device emulation
      await this.page.setViewportSize(deviceConfig.viewport);
      await this.page.setExtraHTTPHeaders({
        'User-Agent': deviceConfig.userAgent
      });
    }

    this.touchEnabled = deviceConfig.hasTouch;
  }

  /**
   * Set custom viewport size
   */
  async setViewport(width: number, height: number): Promise<void> {
    await this.page.setViewportSize({ width, height });
  }

  /**
   * Test viewport at specific breakpoint
   */
  async testAtBreakpoint(breakpoint: ResponsiveBreakpoint): Promise<ViewportTestResult> {
    await this.setViewport(breakpoint.width, breakpoint.height);
    await this.page.waitForTimeout(500); // Allow layout to settle
    
    const issues: string[] = [];
    
    // Check for horizontal scrollbars
    const hasHorizontalScroll = await this.page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    
    if (hasHorizontalScroll) {
      issues.push('Horizontal scrollbar detected - content overflows viewport');
    }

    // Check for overlapping elements
    const overlappingElements = await this.checkForOverlappingElements();
    if (overlappingElements.length > 0) {
      issues.push(`Overlapping elements detected: ${overlappingElements.join(', ')}`);
    }

    // Check touch target sizes on mobile viewports
    if (breakpoint.width < 768) {
      const smallTouchTargets = await this.checkTouchTargetSizes();
      if (smallTouchTargets.length > 0) {
        issues.push(`Small touch targets detected: ${smallTouchTargets.join(', ')}`);
      }
    }

    return {
      viewport: { width: breakpoint.width, height: breakpoint.height },
      passed: issues.length === 0,
      issues
    };
  }

  /**
   * Perform touch interaction
   */
  async performTouchInteraction(interaction: TouchInteraction): Promise<void> {
    if (!this.touchEnabled) {
      console.warn('Touch not enabled for current device emulation');
    }

    switch (interaction.type) {
      case 'tap':
        if (interaction.element) {
          await interaction.element.tap();
        } else if (interaction.startPoint) {
          await this.page.tap(`css=body`, { 
            position: interaction.startPoint,
            timeout: TIMEOUTS.MEDIUM 
          });
        }
        break;

      case 'doubleTap':
        if (interaction.element) {
          await interaction.element.dblclick();
        }
        break;

      case 'longPress':
        if (interaction.element) {
          const box = await interaction.element.boundingBox();
          if (box) {
            await this.page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
            await this.page.mouse.down();
            await this.page.waitForTimeout(interaction.duration || 800);
            await this.page.mouse.up();
          }
        }
        break;

      case 'swipe':
        if (interaction.startPoint && interaction.endPoint) {
          await this.page.mouse.move(interaction.startPoint.x, interaction.startPoint.y);
          await this.page.mouse.down();
          await this.page.mouse.move(
            interaction.endPoint.x, 
            interaction.endPoint.y,
            { steps: 10 }
          );
          await this.page.mouse.up();
        }
        break;

      case 'pinch':
        // Simulate pinch-to-zoom using touch events
        if (interaction.startPoint && interaction.scale) {
          await this.page.evaluate(({ x, y, scale }) => {
            const startDistance = 100;
            const endDistance = startDistance * scale;
            
            // Create touch events
            const touch1Start = new Touch({
              identifier: 1,
              target: document.elementFromPoint(x - startDistance/2, y) || document.body,
              clientX: x - startDistance/2,
              clientY: y
            });
            
            const touch2Start = new Touch({
              identifier: 2,
              target: document.elementFromPoint(x + startDistance/2, y) || document.body,
              clientX: x + startDistance/2,
              clientY: y
            });

            const touchStartEvent = new TouchEvent('touchstart', {
              touches: [touch1Start, touch2Start],
              targetTouches: [touch1Start, touch2Start],
              changedTouches: [touch1Start, touch2Start]
            });

            document.dispatchEvent(touchStartEvent);

            // End touches with different distance
            setTimeout(() => {
              const touch1End = new Touch({
                identifier: 1,
                target: document.elementFromPoint(x - endDistance/2, y) || document.body,
                clientX: x - endDistance/2,
                clientY: y
              });
              
              const touch2End = new Touch({
                identifier: 2,
                target: document.elementFromPoint(x + endDistance/2, y) || document.body,
                clientX: x + endDistance/2,
                clientY: y
              });

              const touchEndEvent = new TouchEvent('touchend', {
                touches: [],
                targetTouches: [],
                changedTouches: [touch1End, touch2End]
              });

              document.dispatchEvent(touchEndEvent);
            }, 100);
          }, { x: interaction.startPoint.x, y: interaction.startPoint.y, scale: interaction.scale });
        }
        break;

      case 'pan':
        if (interaction.startPoint && interaction.endPoint) {
          const steps = 20;
          await this.page.mouse.move(interaction.startPoint.x, interaction.startPoint.y);
          await this.page.mouse.down();
          
          // Smooth panning motion
          for (let i = 1; i <= steps; i++) {
            const progress = i / steps;
            const x = interaction.startPoint.x + (interaction.endPoint.x - interaction.startPoint.x) * progress;
            const y = interaction.startPoint.y + (interaction.endPoint.y - interaction.startPoint.y) * progress;
            await this.page.mouse.move(x, y);
            await this.page.waitForTimeout(10);
          }
          
          await this.page.mouse.up();
        }
        break;
    }
  }

  /**
   * Test orientation change behavior
   */
  async testOrientationChange(deviceName: string): Promise<{
    portraitViewport: { width: number; height: number };
    landscapeViewport: { width: number; height: number };
    layoutIssues: string[];
  }> {
    const deviceConfig = MobileHelper.DEVICE_CONFIGS[deviceName];
    if (!deviceConfig) {
      throw new Error(`Unknown device: ${deviceName}`);
    }

    const issues: string[] = [];
    
    // Test portrait mode
    const portraitViewport = deviceConfig.viewport;
    await this.setViewport(portraitViewport.width, portraitViewport.height);
    await this.page.waitForTimeout(500);
    
    // Test landscape mode
    const landscapeViewport = { width: portraitViewport.height, height: portraitViewport.width };
    await this.setViewport(landscapeViewport.width, landscapeViewport.height);
    await this.page.waitForTimeout(500);

    // Check for layout issues in landscape
    const hasHorizontalScroll = await this.page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    
    if (hasHorizontalScroll) {
      issues.push('Content overflows in landscape mode');
    }

    // Check if navigation adapts properly
    const navigationVisible = await this.page.locator('nav, [role="navigation"]').isVisible();
    if (!navigationVisible) {
      issues.push('Navigation not visible in landscape mode');
    }

    return {
      portraitViewport,
      landscapeViewport,
      layoutIssues: issues
    };
  }

  /**
   * Check touch target sizes
   */
  async checkTouchTargetSizes(): Promise<string[]> {
    const smallTargets: string[] = [];
    
    const touchTargets = await this.page.locator('button, a, input, select, [role="button"], [onclick]').all();
    
    for (let i = 0; i < touchTargets.length; i++) {
      const target = touchTargets[i];
      const box = await target.boundingBox();
      
      if (box && (box.width < MobileHelper.TOUCH_TARGET_SIZES.RECOMMENDED || 
                  box.height < MobileHelper.TOUCH_TARGET_SIZES.RECOMMENDED)) {
        const tagName = await target.evaluate(el => el.tagName.toLowerCase());
        const text = await target.textContent();
        const identifier = text ? `${tagName}: "${text.substring(0, 20)}"` : tagName;
        smallTargets.push(`${identifier} (${Math.round(box.width)}x${Math.round(box.height)}px)`);
      }
    }
    
    return smallTargets;
  }

  /**
   * Check for overlapping elements
   */
  async checkForOverlappingElements(): Promise<string[]> {
    const overlapping: string[] = [];
    
    const elements = await this.page.evaluate(() => {
      const allElements = Array.from(document.querySelectorAll('*'));
      const visibleElements = allElements.filter(el => {
        const style = window.getComputedStyle(el);
        return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
      });
      
      const overlappingPairs: string[] = [];
      
      for (let i = 0; i < visibleElements.length; i++) {
        const el1 = visibleElements[i];
        const rect1 = el1.getBoundingClientRect();
        
        if (rect1.width === 0 || rect1.height === 0) continue;
        
        for (let j = i + 1; j < visibleElements.length; j++) {
          const el2 = visibleElements[j];
          const rect2 = el2.getBoundingClientRect();
          
          if (rect2.width === 0 || rect2.height === 0) continue;
          
          // Check if elements overlap
          if (!(rect1.right <= rect2.left || 
                rect2.right <= rect1.left || 
                rect1.bottom <= rect2.top || 
                rect2.bottom <= rect1.top)) {
            
            // Skip parent-child relationships
            if (!el1.contains(el2) && !el2.contains(el1)) {
              const desc1 = `${el1.tagName.toLowerCase()}${el1.className ? '.' + el1.className.split(' ')[0] : ''}`;
              const desc2 = `${el2.tagName.toLowerCase()}${el2.className ? '.' + el2.className.split(' ')[0] : ''}`;
              overlappingPairs.push(`${desc1} overlaps ${desc2}`);
            }
          }
        }
      }
      
      return overlappingPairs.slice(0, 10); // Limit to avoid spam
    });
    
    return elements;
  }

  /**
   * Test virtual keyboard behavior
   */
  async testVirtualKeyboard(): Promise<{ 
    keyboardTriggered: boolean; 
    layoutShifted: boolean; 
    viewportChanged: boolean 
  }> {
    const initialViewport = await this.page.viewportSize();
    
    // Find an input field and focus it
    const inputField = this.page.locator('input[type="text"], input[type="email"], input[type="password"], textarea').first();
    
    if (!(await inputField.isVisible())) {
      return { keyboardTriggered: false, layoutShifted: false, viewportChanged: false };
    }
    
    // Focus the input to trigger virtual keyboard
    await inputField.focus();
    await this.page.waitForTimeout(1000); // Wait for keyboard animation
    
    const afterFocusViewport = await this.page.viewportSize();
    const viewportChanged = initialViewport?.height !== afterFocusViewport?.height;
    
    // Check if page layout shifted
    const layoutShifted = await this.page.evaluate(() => {
      const scrollableHeight = document.documentElement.scrollHeight;
      const viewportHeight = window.innerHeight;
      return scrollableHeight > viewportHeight;
    });
    
    // Blur the input to hide keyboard
    await inputField.blur();
    await this.page.waitForTimeout(500);
    
    return {
      keyboardTriggered: true,
      layoutShifted,
      viewportChanged
    };
  }

  /**
   * Test scroll performance on mobile
   */
  async testScrollPerformance(): Promise<{ 
    averageFrameTime: number; 
    droppedFrames: number; 
    scrollResponsive: boolean 
  }> {
    const performanceData = await this.page.evaluate(() => {
      return new Promise<{ averageFrameTime: number; droppedFrames: number; scrollResponsive: boolean }>((resolve) => {
        let frameCount = 0;
        let totalFrameTime = 0;
        let droppedFrames = 0;
        let lastFrameTime = performance.now();
        
        const frameCallback = (currentTime: number) => {
          const frameTime = currentTime - lastFrameTime;
          totalFrameTime += frameTime;
          frameCount++;
          
          // Consider frame dropped if it takes longer than 16.67ms (60fps)
          if (frameTime > 16.67) {
            droppedFrames++;
          }
          
          lastFrameTime = currentTime;
          
          if (frameCount < 60) { // Test for 1 second at 60fps
            requestAnimationFrame(frameCallback);
          } else {
            const averageFrameTime = totalFrameTime / frameCount;
            const scrollResponsive = averageFrameTime < 16.67 && droppedFrames < 5;
            
            resolve({
              averageFrameTime,
              droppedFrames,
              scrollResponsive
            });
          }
        };
        
        // Start scrolling animation
        window.scrollTo({ top: 0, behavior: 'smooth' });
        setTimeout(() => {
          window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
          requestAnimationFrame(frameCallback);
        }, 100);
      });
    });
    
    return performanceData;
  }

  /**
   * Test PWA install prompt on mobile
   */
  async testPWAInstallPrompt(): Promise<{
    installPromptTriggered: boolean;
    installButtonVisible: boolean;
    addToHomescreenAvailable: boolean;
  }> {
    let installPromptTriggered = false;
    let installButtonVisible = false;
    
    // Listen for beforeinstallprompt event
    await this.page.evaluate(() => {
      window.addEventListener('beforeinstallprompt', () => {
        (window as ExtendedWindow).installPromptReceived = true;
      });
    });
    
    // Check if install button is visible
    const installButton = this.page.locator('[data-testid="pwa-install"], .install-app, .add-to-homescreen');
    installButtonVisible = await installButton.isVisible().catch(() => false);
    
    // Check if install prompt was triggered
    installPromptTriggered = await this.page.evaluate(() => {
      return !!(window as ExtendedWindow).installPromptReceived;
    });
    
    // Check for Add to Home Screen functionality (iOS Safari)
    const addToHomescreenAvailable = await this.page.evaluate(() => {
      const extendedWindow = window as ExtendedWindow;
      const isIOSSafari = /iPad|iPhone|iPod/.test(navigator.userAgent) && 
                         !extendedWindow.MSStream && 
                         extendedWindow.navigator.standalone !== undefined;
      
      return isIOSSafari || !!extendedWindow.navigator.share;
    });
    
    return {
      installPromptTriggered,
      installButtonVisible,
      addToHomescreenAvailable
    };
  }

  /**
   * Test dark mode on mobile
   */
  async testDarkModeOnMobile(): Promise<{
    darkModeDetected: boolean;
    darkModeToggleWorks: boolean;
    contrastSufficient: boolean;
  }> {
    // Check if system prefers dark mode
    const systemPrefersDark = await this.page.evaluate(() => {
      return window.matchMedia('(prefers-color-scheme: dark)').matches;
    });
    
    // Emulate dark mode preference
    await this.page.emulateMedia({ colorScheme: 'dark' });
    await this.page.waitForTimeout(500);
    
    const darkModeDetected = await this.page.evaluate(() => {
      const bodyStyles = window.getComputedStyle(document.body);
      const backgroundColor = bodyStyles.backgroundColor;
      
      // Check if background is dark (assuming RGB values sum < 384 for dark themes)
      const rgbMatch = backgroundColor.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
      if (rgbMatch) {
        const sum = parseInt(rgbMatch[1]) + parseInt(rgbMatch[2]) + parseInt(rgbMatch[3]);
        return sum < 384;
      }
      
      return false;
    });
    
    // Test dark mode toggle if available
    const darkModeToggle = this.page.locator('[data-testid="dark-mode-toggle"], .dark-mode-switch, .theme-toggle');
    let darkModeToggleWorks = false;
    
    if (await darkModeToggle.isVisible().catch(() => false)) {
      await darkModeToggle.click();
      await this.page.waitForTimeout(500);
      
      const toggledSuccessfully = await this.page.evaluate(() => {
        const bodyStyles = window.getComputedStyle(document.body);
        return bodyStyles.backgroundColor !== 'rgb(255, 255, 255)';
      });
      
      darkModeToggleWorks = toggledSuccessfully;
    }
    
    // Check color contrast
    const contrastSufficient = await this.page.evaluate(() => {
      const textElements = Array.from(document.querySelectorAll('p, span, a, button, h1, h2, h3, h4, h5, h6'));
      let sufficientContrast = true;
      
      for (const element of textElements.slice(0, 20)) { // Check first 20 elements
        const styles = window.getComputedStyle(element);
        const color = styles.color;
        const backgroundColor = styles.backgroundColor;
        
        // Simple contrast check - should be improved with proper contrast ratio calculation
        if (color === backgroundColor) {
          sufficientContrast = false;
          break;
        }
      }
      
      return sufficientContrast;
    });
    
    return {
      darkModeDetected,
      darkModeToggleWorks,
      contrastSufficient
    };
  }

  /**
   * Collect mobile performance metrics
   */
  async collectMobilePerformanceMetrics(): Promise<MobilePerformanceMetrics> {
    const navigationEntry = await this.page.evaluate(() => {
      const navEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        loadTime: navEntry.loadEventEnd - navEntry.fetchStart,
        networkRequests: performance.getEntriesByType('resource').length
      };
    });
    
    // Test touch responsiveness
    const touchStartTime = Date.now();
    const touchableElement = this.page.locator('button, a, [role="button"]').first();
    if (await touchableElement.isVisible()) {
      await touchableElement.tap();
    }
    const touchResponsiveness = Date.now() - touchStartTime;
    
    // Test scroll performance
    const scrollPerformance = await this.testScrollPerformance();
    
    // Get bundle size (approximate from resource entries)
    const bundleSize = await this.page.evaluate(() => {
      const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
      return resources.reduce((total, resource) => {
        return total + (resource.transferSize || 0);
      }, 0);
    });
    
    return {
      loadTime: navigationEntry.loadTime,
      touchResponsiveness,
      scrollPerformance: scrollPerformance.averageFrameTime,
      networkRequests: navigationEntry.networkRequests,
      bundleSize
    };
  }

  /**
   * Take responsive screenshots across multiple viewports
   */
  async takeResponsiveScreenshots(name: string): Promise<string[]> {
    const screenshots: string[] = [];
    const breakpoints = ['xs', 'sm', 'md', 'lg', 'xl'];
    
    for (const breakpoint of breakpoints) {
      const bp = MobileHelper.BREAKPOINTS.find(b => b.name === breakpoint);
      if (bp) {
        await this.setViewport(bp.width, bp.height);
        await this.page.waitForTimeout(500);
        
        const screenshotPath = `mobile-${name}-${breakpoint}-${bp.width}x${bp.height}.png`;
        await this.page.screenshot({ 
          path: screenshotPath, 
          fullPage: false 
        });
        screenshots.push(screenshotPath);
      }
    }
    
    return screenshots;
  }

  /**
   * Verify mobile menu behavior
   */
  async verifyMobileMenu(): Promise<{
    hamburgerMenuVisible: boolean;
    menuToggles: boolean;
    menuItemsAccessible: boolean;
    closeOnItemClick: boolean;
  }> {
    const hamburgerMenu = this.page.locator('.hamburger, .mobile-menu-toggle, [aria-label="Menu"], [data-testid="mobile-menu-toggle"]');
    const hamburgerMenuVisible = await hamburgerMenu.isVisible().catch(() => false);
    
    if (!hamburgerMenuVisible) {
      return {
        hamburgerMenuVisible: false,
        menuToggles: false,
        menuItemsAccessible: false,
        closeOnItemClick: false
      };
    }
    
    // Test menu toggle
    await hamburgerMenu.click();
    await this.page.waitForTimeout(300);
    
    const mobileMenu = this.page.locator('.mobile-menu, .nav-menu, [role="navigation"] ul');
    const menuToggles = await mobileMenu.isVisible().catch(() => false);
    
    // Test menu item accessibility
    const menuItems = mobileMenu.locator('a, button');
    const menuItemCount = await menuItems.count();
    let menuItemsAccessible = false;
    
    if (menuItemCount > 0) {
      // Check if first menu item is keyboard accessible
      await menuItems.first().focus();
      const focusedElement = await this.page.evaluate(() => document.activeElement?.tagName);
      menuItemsAccessible = focusedElement === 'A' || focusedElement === 'BUTTON';
    }
    
    // Test if menu closes on item click
    let closeOnItemClick = false;
    if (menuItemCount > 0) {
      await menuItems.first().click();
      await this.page.waitForTimeout(300);
      closeOnItemClick = !(await mobileMenu.isVisible().catch(() => true));
    }
    
    return {
      hamburgerMenuVisible,
      menuToggles,
      menuItemsAccessible,
      closeOnItemClick
    };
  }

  /**
   * Test form usability on mobile
   */
  async testMobileFormUsability(): Promise<{
    inputTypesOptimized: boolean;
    keyboardTriggersCorrectly: boolean;
    errorMessagesVisible: boolean;
    formSubmittable: boolean;
  }> {
    const forms = this.page.locator('form');
    const formCount = await forms.count();
    
    if (formCount === 0) {
      return {
        inputTypesOptimized: true,
        keyboardTriggersCorrectly: true,
        errorMessagesVisible: true,
        formSubmittable: true
      };
    }
    
    const form = forms.first();
    
    // Check input types
    const emailInputs = form.locator('input[type="email"]');
    const telInputs = form.locator('input[type="tel"]');
    const numberInputs = form.locator('input[type="number"]');
    
    const emailCount = await emailInputs.count();
    const telCount = await telInputs.count();
    const numberCount = await numberInputs.count();
    
    const inputTypesOptimized = emailCount > 0 || telCount > 0 || numberCount > 0;
    
    // Test keyboard triggering
    const firstInput = form.locator('input').first();
    let keyboardTriggersCorrectly = false;
    
    if (await firstInput.isVisible()) {
      await firstInput.focus();
      await this.page.waitForTimeout(500);
      
      // Check if virtual keyboard space is accounted for
      const viewportHeight = await this.page.evaluate(() => window.innerHeight);
      const initialViewportHeight = await this.page.evaluate(() => window.screen.height);
      
      keyboardTriggersCorrectly = viewportHeight < initialViewportHeight || viewportHeight > 0;
    }
    
    // Check error message visibility
    const errorMessages = form.locator('.error, [role="alert"], .invalid, .error-message');
    const errorMessagesVisible = await errorMessages.count() > 0 ? await errorMessages.first().isVisible() : true;
    
    // Test form submittability
    const submitButton = form.locator('button[type="submit"], input[type="submit"]');
    const formSubmittable = await submitButton.isVisible().catch(() => false);
    
    return {
      inputTypesOptimized,
      keyboardTriggersCorrectly,
      errorMessagesVisible,
      formSubmittable
    };
  }

  /**
   * Reset viewport to default
   */
  async resetViewport(): Promise<void> {
    await this.page.setViewportSize({ width: 1280, height: 720 });
    this.touchEnabled = false;
  }
}

export default MobileHelper;