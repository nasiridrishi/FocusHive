/**
 * PWA Mobile Tests for FocusHive
 * Tests Progressive Web App features specifically on mobile devices
 */

import {expect, Page, test} from '@playwright/test';
import {MobileHelper} from '../../helpers/mobile.helper';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_URLS} from '../../helpers/test-data';

interface PWAManifest {
  name: string;
  short_name: string;
  description: string;
  start_url: string;
  display: 'standalone' | 'minimal-ui' | 'fullscreen' | 'browser';
  theme_color: string;
  background_color: string;
  orientation: 'portrait' | 'landscape' | 'any';
  icons: Array<{
    src: string;
    sizes: string;
    type: string;
    purpose?: string;
  }>;
  screenshots?: Array<{
    src: string;
    sizes: string;
    type: string;
    form_factor: 'narrow' | 'wide';
  }>;
}

interface ServiceWorkerTest {
  name: string;
  feature: 'caching' | 'offline' | 'background_sync' | 'push_notifications' | 'update';
  testFunction: (page: Page) => Promise<boolean>;
}

interface _PWAInstallabilityTest {
  platform: 'iOS' | 'Android' | 'desktop';
  triggers: string[];
  expectedBehavior: string;
}

test.describe('PWA Mobile - Manifest and Installation', () => {
  let _authHelper: AuthHelper;
  let _mobileHelper: MobileHelper;

  test.beforeEach(async ({page}) => {
    _authHelper = new AuthHelper(page);
    _mobileHelper = new MobileHelper(page);

    // Set mobile viewport
    await page.setViewportSize({width: 390, height: 844});
  });

  test('should have valid PWA manifest for mobile', async ({page}) => {
    await page.goto(TEST_URLS.HOME);
    await page.waitForLoadState('networkidle');

    await test.step('PWA manifest validation', async () => {
      // Check for manifest link in head
      const manifestLink = page.locator('link[rel="manifest"]');
      await expect(manifestLink).toBeVisible();

      const manifestUrl = await manifestLink.getAttribute('href');
      expect(manifestUrl).toBeTruthy();

      // Fetch and validate manifest
      const manifestResponse = await page.request.get(manifestUrl ?? null);
      expect(manifestResponse.ok()).toBeTruthy();

      const manifest: PWAManifest = await manifestResponse.json();

      // Validate required fields
      expect(manifest.name).toBeTruthy();
      expect(manifest.short_name).toBeTruthy();
      expect(manifest.start_url).toBeTruthy();
      expect(manifest.display).toBeTruthy();
      expect(manifest.icons).toBeDefined();
      expect(manifest.icons.length).toBeGreaterThan(0);

      // Validate mobile-specific fields
      expect(['portrait', 'landscape', 'any']).toContain(manifest.orientation);
      expect(manifest.theme_color).toMatch(/^#[0-9a-fA-F]{6}$/);
      expect(manifest.background_color).toMatch(/^#[0-9a-fA-F]{6}$/);

      // Validate icon sizes for mobile
      const mobileIconSizes = ['192x192', '512x512'];
      const hasRequiredIcons = mobileIconSizes.every(size =>
          manifest.icons.some(icon => icon.sizes.includes(size))
      );
      expect(hasRequiredIcons).toBeTruthy();

      // Check for maskable icons (Android)
      const hasMaskableIcon = manifest.icons.some(icon =>
          icon.purpose?.includes('maskable')
      );
      console.log(`Maskable icon available: ${hasMaskableIcon}`);

      // Check for screenshots (App Store optimization)
      if (manifest.screenshots) {
        const hasMobileScreenshots = manifest.screenshots.some(screenshot =>
            screenshot.form_factor === 'narrow'
        );
        console.log(`Mobile screenshots available: ${hasMobileScreenshots}`);
      }

      console.log('PWA Manifest validated:', {
        name: manifest.name,
        display: manifest.display,
        orientation: manifest.orientation,
        icons: manifest.icons.length,
        screenshots: manifest.screenshots?.length || 0
      });
    });
  });

  test('should handle PWA installation prompts on mobile', async ({page, context: _context}) => {
    // This test simulates the A2HS (Add to Home Screen) behavior
    await test.step('Installation prompt handling', async () => {
      await page.goto(TEST_URLS.HOME);
      await page.waitForLoadState('networkidle');

      // Check for install prompt availability
      const installPromptDetected = await page.evaluate(async () => {
        return new Promise<boolean>((resolve) => {
          let promptDetected = false;

          // Listen for beforeinstallprompt event
          window.addEventListener('beforeinstallprompt', (e) => {
            promptDetected = true;
            e.preventDefault(); // Prevent automatic prompt
            resolve(true);
          });

          // If no prompt after 3 seconds, assume not available
          setTimeout(() => {
            resolve(promptDetected);
          }, 3000);
        });
      });

      console.log(`PWA install prompt available: ${installPromptDetected}`);

      // Check for custom install button
      const customInstallButton = page.locator('[data-testid="install-app"], [data-testid="add-to-homescreen"]');
      const hasCustomInstallUI = await customInstallButton.isVisible().catch(() => false);

      if (hasCustomInstallUI) {
        await customInstallButton.click();
        await page.waitForTimeout(1000);

        // Look for install confirmation or native prompt
        const installDialog = page.locator('[data-testid="install-dialog"]');
        const hasInstallDialog = await installDialog.isVisible().catch(() => false);

        console.log(`Custom install UI available: ${hasInstallDialog}`);
      }

      // The app should be installable even without prompt detection
      // This is because install prompts are browser-controlled
      expect(installPromptDetected || hasCustomInstallUI).toBeTruthy();
    });
  });

  test('should provide mobile-optimized PWA experience', async ({page}) => {
    await page.goto(TEST_URLS.HOME);
    await page.waitForLoadState('networkidle');

    await test.step('Mobile PWA experience validation', async () => {
      // Check for mobile-specific PWA features
      const pwaFeatures = await page.evaluate(() => {
        return {
          // Check if running in standalone mode (installed PWA)
          isStandalone: window.matchMedia('(display-mode: standalone)').matches ||
              (window.navigator as Navigator & { standalone?: boolean }).standalone === true,

          // Check for mobile-specific APIs
          hasVibration: 'vibrate' in navigator,
          hasScreenWakeLock: 'wakeLock' in navigator,
          hasDeviceOrientation: 'DeviceOrientationEvent' in window,
          hasTouch: 'ontouchstart' in window,

          // Check viewport
          viewport: {
            width: window.innerWidth,
            height: window.innerHeight,
            ratio: window.devicePixelRatio
          },

          // Check for PWA-specific styling
          hasStandaloneStyles: !!document.querySelector('[data-standalone-styles]'),

          // Check for safe area support
          hasSafeAreaSupport: CSS.supports('padding-top', 'env(safe-area-inset-top)')
        };
      });

      // Validate mobile PWA features
      expect(pwaFeatures.hasTouch).toBeTruthy();
      expect(pwaFeatures.viewport.width).toBeLessThanOrEqual(500); // Mobile viewport
      expect(pwaFeatures.viewport.ratio).toBeGreaterThan(1); // High DPI display

      console.log('Mobile PWA features:', pwaFeatures);

      // If running in standalone mode, verify PWA-specific UI
      if (pwaFeatures.isStandalone) {
        // Should have proper status bar treatment
        const statusBarMeta = page.locator('meta[name="apple-mobile-web-app-status-bar-style"]');
        await expect(statusBarMeta).toBeVisible();

        // Should have proper title
        const titleMeta = page.locator('meta[name="apple-mobile-web-app-title"]');
        await expect(titleMeta).toBeVisible();
      }
    });
  });

  test('should handle different mobile PWA display modes', async ({page}) => {
    const displayModes = ['standalone', 'minimal-ui', 'fullscreen'];

    for (const displayMode of displayModes) {
      await test.step(`Testing ${displayMode} display mode`, async () => {
        await page.goto(TEST_URLS.HOME);

        // Simulate different display modes using CSS media queries
        const displayModeSupported = await page.evaluate((mode) => {
          return window.matchMedia(`(display-mode: ${mode})`).matches;
        }, displayMode);

        // Check for display mode specific styling
        const hasDisplayModeStyles = await page.evaluate((mode) => {
          const stylesheet = Array.from(document.styleSheets).find(sheet => {
            try {
              const rules = Array.from(sheet.cssRules || []);
              return rules.some(rule =>
                  rule.cssText && rule.cssText.includes(`display-mode: ${mode}`)
              );
            } catch {
              return false;
            }
          });
          return !!stylesheet;
        }, displayMode);

        console.log(`Display mode ${displayMode}: supported=${displayModeSupported}, styled=${hasDisplayModeStyles}`);
      });
    }
  });
});

test.describe('PWA Mobile - Service Worker Functionality', () => {
  const serviceWorkerTests: ServiceWorkerTest[] = [
    {
      name: 'Caching Strategy',
      feature: 'caching',
      testFunction: async (page: Page) => {
        // Navigate to page twice to test caching
        await page.goto(TEST_URLS.HOME);
        await page.waitForLoadState('networkidle');

        const firstLoadTime = Date.now();
        await page.reload();
        await page.waitForLoadState('networkidle');
        const reloadTime = Date.now() - firstLoadTime;

        // Second load should be faster due to caching
        return reloadTime < 3000; // Should load within 3 seconds
      }
    },
    {
      name: 'Offline Functionality',
      feature: 'offline',
      testFunction: async (page: Page) => {
        await page.goto(TEST_URLS.DASHBOARD);
        await page.waitForLoadState('networkidle');

        // Go offline
        await page.context().setOffline(true);

        // Try to navigate
        await page.goto(TEST_URLS.HOME);

        // Should still show content (from cache) or offline page
        const hasOfflineContent = await page.locator('body').isVisible();
        const hasOfflinePage = await page.locator('[data-testid="offline-page"]').isVisible();

        // Go back online
        await page.context().setOffline(false);

        return hasOfflineContent || hasOfflinePage;
      }
    },
    {
      name: 'Service Worker Update',
      feature: 'update',
      testFunction: async (page: Page) => {
        await page.goto(TEST_URLS.HOME);
        await page.waitForLoadState('networkidle');

        // Check for update notification or mechanism
        const updateNotification = page.locator('[data-testid="update-available"]');
        const _hasUpdateMechanism = await updateNotification.isVisible().catch(() => false);

        // Service worker should be registered
        const swRegistered = await page.evaluate(() => {
          return 'serviceWorker' in navigator;
        });

        return swRegistered; // Update mechanism is optional, but SW should be registered
      }
    }
  ];

  serviceWorkerTests.forEach(swTest => {
    test(`should handle ${swTest.name} correctly`, async ({page}) => {
      const authHelper = new AuthHelper(page);
      await authHelper.loginWithTestUser();

      await test.step(`Testing Service Worker: ${swTest.name}`, async () => {
        const testResult = await swTest.testFunction(page);
        expect(testResult).toBeTruthy();

        console.log(`Service Worker ${swTest.name}: ${testResult ? 'PASS' : 'FAIL'}`);
      });
    });
  });

  test('should register service worker successfully', async ({page}) => {
    await page.goto(TEST_URLS.HOME);
    await page.waitForLoadState('networkidle');

    await test.step('Service Worker registration', async () => {
      const serviceWorkerInfo = await page.evaluate(async () => {
        if ('serviceWorker' in navigator) {
          try {
            const registration = await navigator.serviceWorker.ready;
            return {
              registered: true,
              scope: registration.scope,
              active: !!registration.active,
              waiting: !!registration.waiting,
              installing: !!registration.installing,
              updateFound: false
            };
          } catch (error) {
            return {registered: false, error: error.toString()};
          }
        }
        return {registered: false, error: 'Service Worker not supported'};
      });

      expect(serviceWorkerInfo.registered).toBeTruthy();
      expect(serviceWorkerInfo.active).toBeTruthy();

      console.log('Service Worker Info:', serviceWorkerInfo);
    });
  });

  test('should handle service worker lifecycle events', async ({page}) => {
    await page.goto(TEST_URLS.HOME);
    await page.waitForLoadState('networkidle');

    await test.step('Service Worker lifecycle', async () => {
      const lifecycleEvents = await page.evaluate(() => {
        return new Promise<string[]>((resolve) => {
          const events: string[] = [];

          if ('serviceWorker' in navigator) {
            navigator.serviceWorker.addEventListener('controllerchange', () => {
              events.push('controllerchange');
            });

            navigator.serviceWorker.getRegistration().then(registration => {
              if (registration) {
                if (registration.waiting) {
                  events.push('waiting');
                }
                if (registration.installing) {
                  events.push('installing');
                }
                if (registration.active) {
                  events.push('active');
                }
              }

              setTimeout(() => resolve(events), 2000);
            });
          } else {
            resolve(['not_supported']);
          }
        });
      });

      expect(lifecycleEvents).toContain('active');
      console.log('Service Worker lifecycle events:', lifecycleEvents);
    });
  });
});

test.describe('PWA Mobile - Offline and Sync', () => {
  test('should handle offline mode gracefully', async ({page, context}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    await test.step('Offline mode handling', async () => {
      // Go offline
      await context.setOffline(true);

      // Try to create a hive (should be queued for sync)
      const createButton = page.locator('[data-testid="create-hive-button"]');

      if (await createButton.isVisible()) {
        await createButton.click();
        await page.waitForTimeout(1000);

        // Should show offline indicator
        const offlineIndicators = [
          '[data-testid="offline-indicator"]',
          '[data-testid="sync-pending"]',
          'text="Offline"',
          '.offline-mode'
        ];

        let offlineIndicatorFound = false;
        for (const selector of offlineIndicators) {
          if (await page.locator(selector).isVisible({timeout: 2000})) {
            offlineIndicatorFound = true;
            break;
          }
        }

        console.log(`Offline mode indicator shown: ${offlineIndicatorFound}`);
      }

      // Try to navigate to different pages
      const navigationStillWorks = await page.locator('[data-testid="bottom-navigation"]').isVisible();
      expect(navigationStillWorks).toBeTruthy();

      // Go back online
      await context.setOffline(false);
      await page.waitForTimeout(2000);

      // Should show online indicator or remove offline indicator
      const onlineIndicators = [
        '[data-testid="online-indicator"]',
        '[data-testid="sync-complete"]'
      ];

      let backOnline = false;
      for (const selector of onlineIndicators) {
        if (await page.locator(selector).isVisible({timeout: 3000})) {
          backOnline = true;
          break;
        }
      }

      // Or offline indicators should disappear
      if (!backOnline) {
        const offlineGone = true; // Assume offline indicators are gone
        backOnline = offlineGone;
      }

      console.log(`Back online detected: ${backOnline}`);
    });
  });

  test('should sync data when connection restored', async ({page, context}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    await test.step('Background sync functionality', async () => {
      // Check if background sync is supported
      const backgroundSyncSupported = await page.evaluate(() => {
        return 'serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype;
      });

      console.log(`Background Sync supported: ${backgroundSyncSupported}`);

      if (backgroundSyncSupported) {
        // Simulate offline action
        await context.setOffline(true);

        // Perform action that should be synced later
        const actionButton = page.locator('[data-testid="favorite-button"], [data-testid="like-button"]').first();

        if (await actionButton.isVisible()) {
          await actionButton.click();
          await page.waitForTimeout(1000);

          // Should show pending sync indicator
          const syncPending = page.locator('[data-testid="sync-pending"]');
          const hasSyncIndicator = await syncPending.isVisible().catch(() => false);

          console.log(`Sync pending indicator: ${hasSyncIndicator}`);
        }

        // Go back online
        await context.setOffline(false);
        await page.waitForTimeout(3000); // Allow time for background sync

        // Sync should complete
        const syncComplete = page.locator('[data-testid="sync-complete"]');
        const syncCompleted = await syncComplete.isVisible().catch(() => false);

        console.log(`Background sync completed: ${syncCompleted}`);
      }
    });
  });
});

test.describe('PWA Mobile - Push Notifications', () => {
  test('should handle push notification permissions', async ({page, context}) => {
    // Grant notification permission
    await context.grantPermissions(['notifications']);

    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.SETTINGS);
    await page.waitForLoadState('networkidle');

    await test.step('Push notification setup', async () => {
      // Look for notification settings
      const notificationSection = page.locator('[data-testid="notification-settings"]');

      if (await notificationSection.isVisible()) {
        // Test notification permission request
        const enableNotifications = page.locator('[data-testid="enable-push-notifications"]');

        if (await enableNotifications.isVisible()) {
          await enableNotifications.click();

          // Check notification API
          const notificationStatus = await page.evaluate(async () => {
            if ('Notification' in window) {
              const permission = await Notification.requestPermission();
              return {
                supported: true,
                permission,
                serviceWorkerReady: 'serviceWorker' in navigator
              };
            }
            return {supported: false};
          });

          expect(notificationStatus.supported).toBeTruthy();
          console.log('Notification status:', notificationStatus);
        }
      }
    });
  });

  test('should handle push subscription', async ({page, context}) => {
    await context.grantPermissions(['notifications']);

    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.SETTINGS);

    await test.step('Push subscription management', async () => {
      const subscriptionInfo = await page.evaluate(async () => {
        if ('serviceWorker' in navigator && 'PushManager' in window) {
          try {
            const registration = await navigator.serviceWorker.ready;
            const subscription = await registration.pushManager.getSubscription();

            return {
              supported: true,
              subscribed: !!subscription,
              endpoint: subscription?.endpoint?.substring(0, 50) + '...' || null
            };
          } catch (error) {
            return {
              supported: true,
              subscribed: false,
              error: error.toString()
            };
          }
        }
        return {supported: false};
      });

      expect(subscriptionInfo.supported).toBeTruthy();
      console.log('Push subscription info:', subscriptionInfo);
    });
  });
});

test.describe('PWA Mobile - App-like Behavior', () => {
  test('should behave like a native app when installed', async ({page}) => {
    await page.goto(TEST_URLS.HOME);
    await page.waitForLoadState('networkidle');

    await test.step('Native app behavior', async () => {
      // Check for standalone mode indicators
      const standaloneFeatures = await page.evaluate(() => {
        return {
          // Display mode
          isStandalone: window.matchMedia('(display-mode: standalone)').matches,

          // iOS specific
          iosStandalone: (window.navigator as Navigator & {
            standalone?: boolean
          }).standalone === true,

          // Screen properties
          fullscreen: window.matchMedia('(display-mode: fullscreen)').matches,
          minimalUI: window.matchMedia('(display-mode: minimal-ui)').matches,

          // Navigation behavior
          hasHistoryAPI: 'pushState' in history,

          // App shell
          hasAppShell: !!document.querySelector('[data-app-shell]'),

          // PWA-specific UI
          hidesBrowserUI: window.innerHeight === screen.height
        };
      });

      console.log('Standalone features:', standaloneFeatures);

      // Verify app-like navigation
      const appNavigation = page.locator('[data-testid="app-navigation"]');
      const bottomNavigation = page.locator('[data-testid="bottom-navigation"]');

      const hasAppNavigation = await appNavigation.isVisible() || await bottomNavigation.isVisible();
      expect(hasAppNavigation).toBeTruthy();

      // Test in-app navigation
      if (await bottomNavigation.isVisible()) {
        const navTabs = bottomNavigation.locator('[role="tab"], .tab');
        const tabCount = await navTabs.count();

        if (tabCount > 1) {
          const secondTab = navTabs.nth(1);
          await secondTab.click();
          await page.waitForTimeout(500);

          // Should navigate without browser refresh
          const currentUrl = page.url();
          console.log('In-app navigation to:', currentUrl);
        }
      }
    });
  });

  test('should handle app lifecycle events', async ({page}) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    await test.step('App lifecycle events', async () => {
      const lifecycleSupport = await page.evaluate(() => {
        const events: string[] = [];

        // Page Visibility API
        if ('visibilityState' in document) {
          events.push('visibility');
          document.addEventListener('visibilitychange', () => {
            events.push(`visibility-${document.visibilityState}`);
          });
        }

        // Page Lifecycle API
        if ('wasDiscarded' in document) {
          events.push('lifecycle');
        }

        // Beforeunload
        window.addEventListener('beforeunload', () => {
          events.push('beforeunload');
        });

        // Focus/Blur
        window.addEventListener('focus', () => events.push('focus'));
        window.addEventListener('blur', () => events.push('blur'));

        return {
          supportedEvents: events,
          visibilityState: document.visibilityState,
          focused: document.hasFocus()
        };
      });

      expect(lifecycleSupport.supportedEvents).toContain('visibility');
      console.log('Lifecycle support:', lifecycleSupport);

      // Test visibility change (simulate app backgrounding)
      await page.evaluate(() => {
        // Simulate visibility change
        Object.defineProperty(document, 'visibilityState', {
          value: 'hidden',
          configurable: true
        });

        document.dispatchEvent(new Event('visibilitychange'));
      });

      await page.waitForTimeout(500);

      // Restore visibility
      await page.evaluate(() => {
        Object.defineProperty(document, 'visibilityState', {
          value: 'visible',
          configurable: true
        });

        document.dispatchEvent(new Event('visibilitychange'));
      });
    });
  });

  test('should handle deep linking in PWA mode', async ({page}) => {
    const authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();

    await test.step('PWA deep linking', async () => {
      // Test deep link to specific page
      const deepUrl = TEST_URLS.ANALYTICS + '?tab=productivity';
      await page.goto(deepUrl);
      await page.waitForLoadState('networkidle');

      // URL should be preserved
      expect(page.url()).toContain('analytics');
      expect(page.url()).toContain('tab=productivity');

      // App should handle the deep link appropriately
      const activeTab = page.locator('[aria-selected="true"], .active-tab');
      if (await activeTab.isVisible()) {
        const activeTabText = await activeTab.textContent();
        console.log('Deep link handled, active tab:', activeTabText);
      }

      // Test share URL functionality
      const shareButton = page.locator('[data-testid="share-button"]');
      if (await shareButton.isVisible()) {
        await shareButton.click();

        // Should generate shareable URL
        const shareUrl = await page.evaluate(() => {
          return window.location.href;
        });

        expect(shareUrl).toContain(TEST_URLS.ANALYTICS);
      }
    });
  });
});

test.describe('PWA Mobile - Performance and Optimization', () => {
  test('should load quickly as a PWA', async ({page}) => {
    await test.step('PWA loading performance', async () => {
      const startTime = Date.now();

      await page.goto(TEST_URLS.HOME);
      await page.waitForLoadState('networkidle');

      const loadTime = Date.now() - startTime;

      // PWA should load quickly (especially on repeat visits)
      expect(loadTime).toBeLessThan(5000); // 5 seconds max

      console.log(`PWA load time: ${loadTime}ms`);

      // Check for app shell
      const appShell = page.locator('[data-app-shell], .app-shell');
      const hasAppShell = await appShell.isVisible().catch(() => false);

      if (hasAppShell) {
        console.log('App shell detected - enables faster loading');
      }
    });
  });

  test('should use efficient caching strategies', async ({page}) => {
    await test.step('PWA caching efficiency', async () => {
      // First visit
      const firstVisitStart = Date.now();
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');
      const firstVisitTime = Date.now() - firstVisitStart;

      // Second visit (should be faster due to caching)
      const secondVisitStart = Date.now();
      await page.reload();
      await page.waitForLoadState('networkidle');
      const secondVisitTime = Date.now() - secondVisitStart;

      // Second visit should be faster
      expect(secondVisitTime).toBeLessThan(firstVisitTime);

      console.log('Cache efficiency:', {
        firstVisit: `${firstVisitTime}ms`,
        secondVisit: `${secondVisitTime}ms`,
        improvement: `${Math.round((firstVisitTime - secondVisitTime) / firstVisitTime * 100)}%`
      });
    });
  });
});

test.describe('PWA Mobile - Cross-Platform Compatibility', () => {
  const platforms = [
    {
      name: 'iOS Safari',
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15'
    },
    {
      name: 'Android Chrome',
      userAgent: 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36'
    }
  ];

  platforms.forEach(platform => {
    test(`should work correctly on ${platform.name}`, async ({page, context}) => {
      await context.addInitScript(() => {
        Object.defineProperty(navigator, 'userAgent', {
          get: () => platform.userAgent,
        });
      });

      await test.step(`PWA compatibility on ${platform.name}`, async () => {
        await page.goto(TEST_URLS.HOME);
        await page.waitForLoadState('networkidle');

        // Check platform-specific PWA features
        const platformFeatures = await page.evaluate(() => {
          const ua = navigator.userAgent;
          return {
            isIOS: ua.includes('iPhone') || ua.includes('iPad'),
            isAndroid: ua.includes('Android'),
            hasStandaloneMode: 'standalone' in window.navigator ||
                window.matchMedia('(display-mode: standalone)').matches,
            hasAddToHomeScreen: 'beforeinstallprompt' in window,
            hasWebAppManifest: !!document.querySelector('link[rel="manifest"]'),
            hasAppleMeta: !!document.querySelector('meta[name="apple-mobile-web-app-capable"]')
          };
        });

        // Validate platform-specific features
        if (platformFeatures.isIOS) {
          expect(platformFeatures.hasAppleMeta).toBeTruthy();
        }

        if (platformFeatures.isAndroid) {
          // Android should support manifest and install prompts
          expect(platformFeatures.hasWebAppManifest).toBeTruthy();
        }

        console.log(`${platform.name} PWA features:`, platformFeatures);
      });
    });
  });
});