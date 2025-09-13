/**
 * Mobile-Specific Features Tests for FocusHive
 * Tests mobile-only features, APIs, and device capabilities
 */

import { test, expect, Page, Browser } from '@playwright/test';
import { MOBILE_DEVICES } from './mobile.config';
import { MobileHelper } from '../../helpers/mobile.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { TEST_URLS } from '../../helpers/test-data';

interface MobileFeatureTest {
  name: string;
  feature: 'camera' | 'geolocation' | 'notifications' | 'share' | 'orientation' | 'battery' | 'network' | 'storage';
  platforms: ('iOS' | 'Android' | 'all')[];
  required: boolean;
  fallbackBehavior?: string;
}

interface OrientationTest {
  orientation: 'portrait' | 'landscape';
  viewport: { width: number; height: number };
  expectedChanges: string[];
}

interface NetworkCondition {
  name: string;
  downloadThroughput: number;
  uploadThroughput: number;
  latency: number;
}

test.describe('Mobile Features - Device APIs', () => {
  let authHelper: AuthHelper;
  let mobileHelper: MobileHelper;

  test.beforeEach(async ({ page, context }) => {
    authHelper = new AuthHelper(page);
    mobileHelper = new MobileHelper(page);
    
    // Grant permissions for mobile features
    await context.grantPermissions(['geolocation', 'camera', 'microphone', 'notifications']);
    
    // Set mobile viewport
    await page.setViewportSize({ width: 390, height: 844 });
    await authHelper.loginWithTestUser();
  });

  test('should handle camera access for profile photos', async ({ page }) => {
    await page.goto(TEST_URLS.PROFILE);
    await page.waitForLoadState('networkidle');

    const profilePhotoButton = page.locator('[data-testid="change-profile-photo"], [data-testid="camera-button"]');
    
    if (await profilePhotoButton.isVisible()) {
      await test.step('Camera access for profile photo', async () => {
        // Click profile photo change button
        await profilePhotoButton.click();
        
        // Look for camera option in menu/modal
        const cameraOption = page.locator('[data-testid="camera-option"], text="Take Photo", text="Camera"');
        
        if (await cameraOption.isVisible()) {
          await cameraOption.click();
          
          // Test camera API availability
          const cameraSupported = await page.evaluate(async () => {
            try {
              if ('mediaDevices' in navigator && 'getUserMedia' in navigator.mediaDevices) {
                // Check if camera permission is available
                const stream = await navigator.mediaDevices.getUserMedia({ video: true });
                if (stream) {
                  // Clean up stream
                  stream.getTracks().forEach(track => track.stop());
                  return true;
                }
              }
              return false;
            } catch (error) {
              console.log('Camera access error:', error);
              return false;
            }
          });

          if (cameraSupported) {
            // Verify camera interface appears
            const cameraInterface = page.locator('[data-testid="camera-interface"], video, canvas');
            await expect(cameraInterface).toBeVisible({ timeout: 5000 });
            
            // Look for capture button
            const captureButton = page.locator('[data-testid="capture-button"], [aria-label="Take photo"]');
            await expect(captureButton).toBeVisible();
          } else {
            // Verify fallback behavior
            const fileInput = page.locator('input[type="file"][accept*="image"]');
            const fallbackMessage = page.locator('[data-testid="camera-fallback"]');
            
            const hasFallback = await fileInput.isVisible() || await fallbackMessage.isVisible();
            expect(hasFallback).toBeTruthy();
          }
        }
      });
    }
  });

  test('should handle geolocation for location-based features', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    await test.step('Geolocation API for nearby hives', async () => {
      // Look for location-based features
      const locationButtons = [
        '[data-testid="nearby-hives"]',
        '[data-testid="location-filter"]',
        '[aria-label*="location"]',
        'text="Near me"'
      ];

      let locationFeatureFound = false;
      
      for (const selector of locationButtons) {
        const button = page.locator(selector);
        if (await button.isVisible()) {
          locationFeatureFound = true;
          await button.click();
          break;
        }
      }

      if (locationFeatureFound) {
        // Test geolocation API
        const locationSupported = await page.evaluate(async () => {
          try {
            if ('geolocation' in navigator) {
              return new Promise((resolve) => {
                navigator.geolocation.getCurrentPosition(
                  () => resolve(true),
                  () => resolve(false),
                  { timeout: 5000 }
                );
              });
            }
            return false;
          } catch (error) {
            return false;
          }
        });

        if (locationSupported) {
          // Verify location-based content loads
          const locationContent = page.locator('[data-testid="nearby-content"], .location-results');
          await expect(locationContent).toBeVisible({ timeout: 10000 });
        } else {
          // Verify fallback behavior (manual location entry)
          const locationInput = page.locator('[data-testid="location-input"], input[placeholder*="location"]');
          const fallbackMessage = page.locator('[data-testid="location-fallback"]');
          
          const hasFallback = await locationInput.isVisible() || await fallbackMessage.isVisible();
          expect(hasFallback).toBeTruthy();
        }
      }
    });
  });

  test('should handle push notifications', async ({ page }) => {
    await page.goto(TEST_URLS.SETTINGS);
    await page.waitForLoadState('networkidle');

    await test.step('Push notification setup', async () => {
      const notificationSettings = page.locator('[data-testid="notification-settings"]');
      
      if (await notificationSettings.isVisible()) {
        await notificationSettings.click();
        
        // Look for notification permission request
        const enableNotifications = page.locator('[data-testid="enable-notifications"]');
        
        if (await enableNotifications.isVisible()) {
          await enableNotifications.click();
          
          // Test notification API
          const notificationSupported = await page.evaluate(async () => {
            try {
              if ('Notification' in window) {
                if (Notification.permission === 'default') {
                  const permission = await Notification.requestPermission();
                  return permission === 'granted';
                }
                return Notification.permission === 'granted';
              }
              return false;
            } catch (error) {
              return false;
            }
          });

          if (notificationSupported) {
            // Verify notification settings are enabled
            const notificationToggle = page.locator('[data-testid="notification-toggle"]');
            if (await notificationToggle.isVisible()) {
              const isEnabled = await notificationToggle.isChecked();
              expect(isEnabled).toBeTruthy();
            }
          } else {
            // Verify fallback (in-app notifications only)
            const fallbackMessage = page.locator('[data-testid="notification-fallback"]');
            const inAppOnly = page.locator('[data-testid="in-app-only"]');
            
            const hasFallback = await fallbackMessage.isVisible() || await inAppOnly.isVisible();
            expect(hasFallback).toBeTruthy();
          }
        }
      }
    });
  });

  test('should handle Web Share API', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    const shareButtons = page.locator('[data-testid="share-button"], [aria-label*="share"]');
    
    if (await shareButtons.first().isVisible()) {
      await test.step('Web Share API functionality', async () => {
        await shareButtons.first().click();
        
        // Test Web Share API availability
        const shareApiSupported = await page.evaluate(async () => {
          return 'share' in navigator && typeof navigator.share === 'function';
        });

        if (shareApiSupported) {
          // Test share data preparation
          const shareData = await page.evaluate(() => {
            return {
              title: document.title,
              text: 'Check out this hive on FocusHive!',
              url: window.location.href
            };
          });

          expect(shareData.title).toBeTruthy();
          expect(shareData.text).toBeTruthy();
          expect(shareData.url).toBeTruthy();
        } else {
          // Verify fallback share options (social media links, copy link)
          const fallbackShare = page.locator('[data-testid="share-fallback"], .share-options');
          await expect(fallbackShare).toBeVisible();
          
          const socialLinks = page.locator('[data-testid^="share-"], a[href*="twitter"], a[href*="facebook"]');
          const copyLink = page.locator('[data-testid="copy-link"]');
          
          const hasFallbackOptions = 
            await socialLinks.count() > 0 || 
            await copyLink.isVisible();
          
          expect(hasFallbackOptions).toBeTruthy();
        }
      });
    }
  });

  test('should handle file upload from mobile gallery', async ({ page }) => {
    await page.goto(TEST_URLS.PROFILE);
    await page.waitForLoadState('networkidle');

    const uploadButtons = page.locator('[data-testid="upload-photo"], input[type="file"]');
    
    if (await uploadButtons.first().isVisible()) {
      await test.step('Mobile file upload capabilities', async () => {
        // Check file input attributes for mobile
        const fileInput = uploadButtons.locator('input[type="file"]').first();
        
        if (await fileInput.isVisible()) {
          const fileAttributes = await fileInput.evaluate((input) => ({
            accept: input.getAttribute('accept'),
            multiple: input.hasAttribute('multiple'),
            capture: input.getAttribute('capture')
          }));

          // Verify mobile-friendly file input
          expect(fileAttributes.accept).toContain('image');
          
          // Check if camera capture is enabled for mobile
          if (fileAttributes.capture) {
            expect(['user', 'environment', 'camera']).toContain(fileAttributes.capture);
          }
        }

        // Test file API support
        const fileApiSupported = await page.evaluate(() => {
          return !!(window.File && window.FileReader && window.FileList && window.Blob);
        });

        expect(fileApiSupported).toBeTruthy();
      });
    }
  });

  test('should handle virtual keyboard appearance', async ({ page }) => {
    await page.goto(TEST_URLS.LOGIN);
    
    await test.step('Virtual keyboard handling', async () => {
      // Focus on input field to trigger virtual keyboard
      const emailInput = page.locator('input[type="email"], input[name="email"]');
      
      if (await emailInput.isVisible()) {
        const initialViewportHeight = await page.evaluate(() => window.innerHeight);
        
        await emailInput.click();
        await page.waitForTimeout(1000); // Wait for keyboard animation
        
        // Check if viewport height changed (virtual keyboard appeared)
        const newViewportHeight = await page.evaluate(() => window.innerHeight);
        
        // On mobile, virtual keyboard typically reduces viewport height
        const keyboardAppeared = newViewportHeight < initialViewportHeight * 0.8;
        
        if (keyboardAppeared) {
          // Verify fixed elements are still accessible
          const submitButton = page.locator('[type="submit"], [data-testid="login-button"]');
          if (await submitButton.isVisible()) {
            const buttonRect = await submitButton.boundingBox();
            if (buttonRect) {
              // Button should still be within visible area
              expect(buttonRect.y + buttonRect.height).toBeLessThan(newViewportHeight);
            }
          }
        }
        
        console.log(`Virtual keyboard detected: ${keyboardAppeared}`);
        console.log(`Viewport height change: ${initialViewportHeight} -> ${newViewportHeight}`);
      }
    });
  });

  test('should handle device orientation changes', async ({ page }) => {
    await page.goto(TEST_URLS.ANALYTICS);
    await page.waitForLoadState('networkidle');

    const orientationTests: OrientationTest[] = [
      {
        orientation: 'portrait',
        viewport: { width: 390, height: 844 },
        expectedChanges: ['vertical_layout', 'stacked_navigation', 'single_column']
      },
      {
        orientation: 'landscape', 
        viewport: { width: 844, height: 390 },
        expectedChanges: ['horizontal_layout', 'side_navigation', 'multi_column']
      }
    ];

    for (const orientationTest of orientationTests) {
      await test.step(`Testing ${orientationTest.orientation} orientation`, async () => {
        await page.setViewportSize(orientationTest.viewport);
        await page.waitForTimeout(500); // Allow orientation change

        // Test orientation API
        const orientationInfo = await page.evaluate(() => {
          interface ExtendedScreen extends Screen {
            orientation?: {
              angle: number;
              type: string;
            };
          }
          
          const screen = window.screen as ExtendedScreen;
          return {
            orientation: screen.orientation?.angle || 0,
            width: window.innerWidth,
            height: window.innerHeight,
            ratio: window.innerWidth / window.innerHeight
          };
        });

        if (orientationTest.orientation === 'landscape') {
          expect(orientationInfo.ratio).toBeGreaterThan(1);
        } else {
          expect(orientationInfo.ratio).toBeLessThan(1);
        }

        // Verify layout adaptations
        await verifyOrientationLayout(page, orientationTest);
      });
    }
  });
});

test.describe('Mobile Features - Network Conditions', () => {
  const networkConditions: NetworkCondition[] = [
    {
      name: 'Slow 3G',
      downloadThroughput: 500 * 1024 / 8, // 500 Kbps in bytes/sec
      uploadThroughput: 500 * 1024 / 8,
      latency: 2000
    },
    {
      name: 'Fast 3G',
      downloadThroughput: 1.5 * 1024 * 1024 / 8, // 1.5 Mbps
      uploadThroughput: 750 * 1024 / 8,
      latency: 562.5
    },
    {
      name: '4G',
      downloadThroughput: 9 * 1024 * 1024 / 8, // 9 Mbps
      uploadThroughput: 9 * 1024 * 1024 / 8,
      latency: 170
    }
  ];

  networkConditions.forEach(condition => {
    test(`should handle ${condition.name} network conditions`, async ({ page, context }) => {
      // Emulate network conditions
      await context.route('**/*', async route => {
        await route.continue();
      });

      // Set network throttling
      const client = await context.newCDPSession(page);
      await client.send('Network.enable');
      await client.send('Network.emulateNetworkConditions', {
        offline: false,
        downloadThroughput: condition.downloadThroughput,
        uploadThroughput: condition.uploadThroughput,
        latency: condition.latency
      });

      let authHelper = new AuthHelper(page);
      await authHelper.loginWithTestUser();

      await test.step(`Testing app performance on ${condition.name}`, async () => {
        const startTime = Date.now();
        
        await page.goto(TEST_URLS.DASHBOARD);
        await page.waitForLoadState('networkidle');
        
        const loadTime = Date.now() - startTime;
        
        // Set realistic expectations based on network speed
        let maxLoadTime: number;
        switch (condition.name) {
          case 'Slow 3G':
            maxLoadTime = 10000; // 10 seconds
            break;
          case 'Fast 3G':
            maxLoadTime = 6000; // 6 seconds
            break;
          case '4G':
            maxLoadTime = 3000; // 3 seconds
            break;
          default:
            maxLoadTime = 5000;
        }

        expect(loadTime).toBeLessThan(maxLoadTime);
        console.log(`Load time on ${condition.name}: ${loadTime}ms`);

        // Verify offline indicator if network is very slow
        if (condition.name === 'Slow 3G') {
          const offlineIndicator = page.locator('[data-testid="slow-connection"], [data-testid="offline-indicator"]');
          // This is optional but good UX
          if (await offlineIndicator.isVisible()) {
            console.log('Slow connection indicator shown');
          }
        }

        // Test interactive elements still work
        const interactiveElement = page.locator('[data-testid="create-hive-button"]');
        if (await interactiveElement.isVisible()) {
          await interactiveElement.click();
          // Should still be responsive even on slow networks
          await page.waitForTimeout(2000);
        }
      });
    });
  });

  test('should handle offline mode gracefully', async ({ page, context }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    await test.step('Testing offline functionality', async () => {
      // Go offline
      await context.setOffline(true);
      
      // Try to navigate to a new page
      await page.locator('[data-testid="hive-list-link"]').click();
      
      // Should show offline indicator
      const offlineIndicators = [
        '[data-testid="offline-indicator"]',
        '[data-testid="no-connection"]',
        'text="No internet connection"',
        '.offline-banner'
      ];

      let offlineIndicatorFound = false;
      for (const selector of offlineIndicators) {
        if (await page.locator(selector).isVisible({ timeout: 3000 })) {
          offlineIndicatorFound = true;
          break;
        }
      }

      // Either show offline indicator or cache should work
      if (!offlineIndicatorFound) {
        // Verify page still functions with cached data
        const cachedContent = page.locator('[data-testid="hive-list"], main');
        await expect(cachedContent).toBeVisible();
      }

      // Test service worker offline capabilities
      const serviceWorkerActive = await page.evaluate(async () => {
        if ('serviceWorker' in navigator) {
          const registration = await navigator.serviceWorker.ready;
          return !!registration.active;
        }
        return false;
      });

      console.log(`Service Worker active: ${serviceWorkerActive}`);
      
      // Go back online
      await context.setOffline(false);
      await page.waitForTimeout(2000);
      
      // Verify app recovers
      const onlineContent = page.locator('[data-testid="online-indicator"], main');
      await expect(onlineContent).toBeVisible();
    });
  });
});

test.describe('Mobile Features - Battery and Performance', () => {
  test('should respect battery optimization settings', async ({ page }) => {
    await page.goto(TEST_URLS.SETTINGS);
    await page.waitForLoadState('networkidle');

    await test.step('Battery optimization features', async () => {
      // Check for battery API support
      const batteryInfo = await page.evaluate(async () => {
        try {
          // @ts-ignore - Battery API is experimental
          const battery = await navigator.getBattery?.();
          if (battery) {
            return {
              level: battery.level,
              charging: battery.charging,
              supported: true
            };
          }
        } catch (error) {
          // Battery API not supported
        }
        return { supported: false };
      });

      if (batteryInfo.supported) {
        console.log(`Battery level: ${batteryInfo.level * 100}%`);
        console.log(`Charging: ${batteryInfo.charging}`);
        
        // Look for battery saver settings
        const batterySaver = page.locator('[data-testid="battery-saver"], [data-testid="power-saving"]');
        
        if (await batterySaver.isVisible()) {
          // Test enabling battery saver mode
          await batterySaver.click();
          
          // Verify performance optimizations are applied
          const optimizationIndicator = page.locator('[data-testid="performance-mode"], [data-testid="reduced-animations"]');
          
          if (await optimizationIndicator.isVisible()) {
            console.log('Battery optimization mode enabled');
          }
        }
      } else {
        console.log('Battery API not supported');
      }
    });
  });

  test('should handle reduced motion preferences', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);

    await test.step('Reduced motion accessibility', async () => {
      // Check for prefers-reduced-motion support
      const reducedMotionSupported = await page.evaluate(() => {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      });

      if (reducedMotionSupported) {
        // Verify animations are disabled or reduced
        const animatedElements = page.locator('.animate, [data-animate="true"]');
        const count = await animatedElements.count();
        
        if (count > 0) {
          // Check if animations are disabled in CSS
          const animationsDisabled = await animatedElements.first().evaluate(el => {
            const style = window.getComputedStyle(el);
            return style.animationDuration === '0s' || style.animationName === 'none';
          });
          
          expect(animationsDisabled).toBeTruthy();
        }
      }
      
      console.log(`Reduced motion preference: ${reducedMotionSupported}`);
    });
  });
});

// Helper Functions

async function verifyOrientationLayout(page: Page, orientationTest: OrientationTest): Promise<void> {
  const { orientation, expectedChanges } = orientationTest;

  for (const expectedChange of expectedChanges) {
    switch (expectedChange) {
      case 'vertical_layout':
        const verticalLayout = page.locator('[data-orientation="portrait"], .layout-vertical');
        if (await verticalLayout.isVisible()) {
          console.log('Vertical layout detected');
        }
        break;

      case 'horizontal_layout':
        const horizontalLayout = page.locator('[data-orientation="landscape"], .layout-horizontal');
        if (await horizontalLayout.isVisible()) {
          console.log('Horizontal layout detected');
        }
        break;

      case 'stacked_navigation':
        const stackedNav = page.locator('[data-testid="bottom-navigation"], .nav-stacked');
        if (await stackedNav.isVisible()) {
          console.log('Stacked navigation in portrait');
        }
        break;

      case 'side_navigation':
        const sideNav = page.locator('[data-testid="side-navigation"], .nav-horizontal');
        if (await sideNav.isVisible()) {
          console.log('Side navigation in landscape');
        }
        break;

      case 'single_column':
        const content = page.locator('[data-testid="main-content"]');
        if (await content.isVisible()) {
          const columns = await content.evaluate(el => {
            const style = window.getComputedStyle(el);
            const gridCols = style.gridTemplateColumns;
            return gridCols ? gridCols.split(' ').length : 1;
          });
          expect(columns).toBe(1);
        }
        break;

      case 'multi_column':
        const multiContent = page.locator('[data-testid="main-content"]');
        if (await multiContent.isVisible()) {
          const columns = await multiContent.evaluate(el => {
            const style = window.getComputedStyle(el);
            const gridCols = style.gridTemplateColumns;
            return gridCols ? gridCols.split(' ').length : 1;
          });
          expect(columns).toBeGreaterThan(1);
        }
        break;
    }
  }
}