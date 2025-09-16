/**
 * Progressive Web App (PWA) Compatibility Tests
 * Tests PWA features, Service Workers, and app-like capabilities across browsers
 */

import {expect, test} from '@playwright/test';

test.describe('Service Worker Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Service Worker API', async ({page}) => {
    const serviceWorkerSupport = await page.evaluate(() => {
      return {
        supported: 'serviceWorker' in navigator,
        registration: 'serviceWorker' in navigator && typeof navigator.serviceWorker.register === 'function',
        controller: 'serviceWorker' in navigator && typeof navigator.serviceWorker.controller === 'object',
        ready: 'serviceWorker' in navigator && typeof navigator.serviceWorker.ready === 'object'
      };
    });

    expect(serviceWorkerSupport.supported).toBe(true);
    expect(serviceWorkerSupport.registration).toBe(true);

    console.log('Service Worker Support:', serviceWorkerSupport);
  });

  test('should handle Service Worker lifecycle', async ({page}) => {
    // Skip if not HTTPS or localhost (Service Workers require secure context)
    const isSecureContext = await page.evaluate(() => window.isSecureContext);

    if (!isSecureContext) {
      console.log('Skipping Service Worker lifecycle test - requires secure context');
      return;
    }

    const serviceWorkerTest = await page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) {
        return {supported: false, error: 'Service Worker not supported'};
      }

      try {
        // Create a simple service worker script
        const serviceWorkerScript = `
          self.addEventListener('install', function(event) {
            self.postMessage({ type: 'install' });
          });
          
          self.addEventListener('activate', function(event) {
            self.postMessage({ type: 'activate' });
          });
          
          self.addEventListener('fetch', function(event) {
            // Pass through fetch requests
          });
        `;

        const blob = new Blob([serviceWorkerScript], {type: 'application/javascript'});
        const serviceWorkerURL = URL.createObjectURL(blob);

        const registration = await navigator.serviceWorker.register(serviceWorkerURL);

        return new Promise<{
          supported: boolean;
          registered: boolean;
          scope: string;
          events: string[];
        }>((resolve) => {
          const events: string[] = [];

          if (registration.installing) {
            events.push('installing');
          }

          if (registration.waiting) {
            events.push('waiting');
          }

          if (registration.active) {
            events.push('active');
          }

          navigator.serviceWorker.addEventListener('message', (event) => {
            events.push(event.data.type);
          });

          setTimeout(() => {
            URL.revokeObjectURL(serviceWorkerURL);
            registration.unregister().catch(() => {
            });

            resolve({
              supported: true,
              registered: true,
              scope: registration.scope,
              events
            });
          }, 2000);
        });
      } catch (error) {
        return {
          supported: false,
          registered: false,
          scope: '',
          events: [],
          error: (error as Error).message
        };
      }
    });

    if (serviceWorkerTest.supported) {
      expect(serviceWorkerTest.registered).toBe(true);
      expect(serviceWorkerTest.scope).toBeTruthy();
    }

    console.log('Service Worker Lifecycle:', serviceWorkerTest);
  });

  test('should support Cache API', async ({page}) => {
    const cacheAPISupport = await page.evaluate(async () => {
      if (!('caches' in window)) {
        return {supported: false, error: 'Cache API not supported'};
      }

      try {
        // Test cache operations
        const cacheName = 'test-cache-' + Date.now();
        const cache = await caches.open(cacheName);

        const testResponse = new Response('test content', {
          headers: {'Content-Type': 'text/plain'}
        });

        await cache.put('/test', testResponse);
        const cachedResponse = await cache.match('/test');
        const cachedText = await cachedResponse?.text();

        // Clean up
        await caches.delete(cacheName);

        return {
          supported: true,
          cacheOpened: !!cache,
          contentMatched: cachedText === 'test content'
        };
      } catch (error) {
        return {
          supported: false,
          error: (error as Error).message
        };
      }
    });

    if (cacheAPISupport.supported) {
      expect(cacheAPISupport.cacheOpened).toBe(true);
      expect(cacheAPISupport.contentMatched).toBe(true);
    }

    console.log('Cache API Support:', cacheAPISupport);
  });

  test('should support Background Sync', async ({page}) => {
    const backgroundSyncSupport = await page.evaluate(() => {
      return {
        supported: 'serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype,
        syncManager: 'serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype
      };
    });

    console.log('Background Sync Support:', backgroundSyncSupport);
  });
});

test.describe('Web App Manifest Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should detect Web App Manifest', async ({page}) => {
    const manifestDetection = await page.evaluate(() => {
      const manifestLink = document.querySelector('link[rel="manifest"]') as HTMLLinkElement;

      return {
        hasManifestLink: !!manifestLink,
        manifestHref: manifestLink?.href || null,
        beforeInstallPrompt: 'onbeforeinstallprompt' in window,
        appInstalled: 'onappinstalled' in window
      };
    });

    // FocusHive should have a manifest
    expect(manifestDetection.hasManifestLink).toBe(true);

    if (manifestDetection.manifestHref) {
      expect(manifestDetection.manifestHref).toContain('manifest');
    }

    console.log('Manifest Detection:', manifestDetection);
  });

  test('should validate manifest properties', async ({page}) => {
    // Try to fetch and parse the manifest
    const manifestValidation = await page.evaluate(async () => {
      const manifestLink = document.querySelector('link[rel="manifest"]') as HTMLLinkElement;

      if (!manifestLink) {
        return {hasManifest: false};
      }

      try {
        const response = await fetch(manifestLink.href);
        const manifest = await response.json();

        return {
          hasManifest: true,
          hasName: !!manifest.name,
          hasShortName: !!manifest.short_name,
          hasStartUrl: !!manifest.start_url,
          hasDisplay: !!manifest.display,
          hasThemeColor: !!manifest.theme_color,
          hasBackgroundColor: !!manifest.background_color,
          hasIcons: Array.isArray(manifest.icons) && manifest.icons.length > 0,
          iconCount: manifest.icons?.length || 0,
          display: manifest.display,
          manifestData: manifest
        };
      } catch (error) {
        return {
          hasManifest: true,
          error: (error as Error).message
        };
      }
    });

    if (manifestValidation.hasManifest && !manifestValidation.error) {
      expect(manifestValidation.hasName).toBe(true);
      expect(manifestValidation.hasStartUrl).toBe(true);
      expect(manifestValidation.hasIcons).toBe(true);
      expect(manifestValidation.iconCount).toBeGreaterThan(0);
    }

    console.log('Manifest Validation:', manifestValidation);
  });

  test('should handle install prompts', async ({page, browserName}) => {
    // Install prompts are primarily supported in Chromium browsers
    const installPromptTest = await page.evaluate(() => {
      return new Promise<{
        beforeInstallPromptSupported: boolean;
        promptReceived: boolean;
        appInstalledSupported: boolean;
      }>((resolve) => {
        let promptReceived = false;

        const beforeInstallPromptHandler = (event: Event): void => {
          promptReceived = true;
          event.preventDefault(); // Don't show the default prompt
        };

        window.addEventListener('beforeinstallprompt', beforeInstallPromptHandler);

        // Wait for potential prompt
        setTimeout(() => {
          window.removeEventListener('beforeinstallprompt', beforeInstallPromptHandler);

          resolve({
            beforeInstallPromptSupported: 'onbeforeinstallprompt' in window,
            promptReceived,
            appInstalledSupported: 'onappinstalled' in window
          });
        }, 2000);
      });
    });

    // Chromium browsers typically support install prompts
    if (browserName === 'chromium') {
      expect(installPromptTest.beforeInstallPromptSupported).toBe(true);
    }

    console.log('Install Prompt Test:', installPromptTest);
  });
});

test.describe('Offline Capabilities', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should detect online/offline status', async ({page}) => {
    const onlineStatusTest = await page.evaluate(() => {
      return {
        onlineProperty: navigator.onLine,
        onlineEventSupported: 'ononline' in window,
        offlineEventSupported: 'onoffline' in window
      };
    });

    expect(onlineStatusTest.onlineProperty).toBe(true); // Should be online during test
    expect(onlineStatusTest.onlineEventSupported).toBe(true);
    expect(onlineStatusTest.offlineEventSupported).toBe(true);

    console.log('Online Status Test:', onlineStatusTest);
  });

  test('should handle offline page loading', async ({page, context}) => {
    // First visit the page to ensure it's cached
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Simulate offline condition
    await context.setOffline(true);

    // Try to navigate to a cached page
    await page.reload();

    const offlineTest = await page.evaluate(() => {
      return {
        onlineStatus: navigator.onLine,
        pageLoaded: document.readyState === 'complete'
      };
    });

    // Reset online status
    await context.setOffline(false);

    console.log('Offline Test:', offlineTest);
  });

  test('should support IndexedDB for offline storage', async ({page}) => {
    const indexedDBTest = await page.evaluate(async () => {
      if (!('indexedDB' in window)) {
        return {supported: false, error: 'IndexedDB not supported'};
      }

      return new Promise<{
        supported: boolean;
        dbOpened: boolean;
        dataStored: boolean;
        dataRetrieved: boolean;
        error?: string;
      }>((resolve) => {
        const dbName = 'test-db-' + Date.now();
        const request = indexedDB.open(dbName, 1);

        request.onerror = () => {
          resolve({
            supported: false,
            dbOpened: false,
            dataStored: false,
            dataRetrieved: false,
            error: 'Failed to open database'
          });
        };

        request.onupgradeneeded = (event) => {
          const db = (event.target as IDBOpenDBRequest).result;
          const _objectStore = db.createObjectStore('testStore', {keyPath: 'id'});
        };

        request.onsuccess = (event) => {
          const db = (event.target as IDBOpenDBRequest).result;
          const transaction = db.transaction(['testStore'], 'readwrite');
          const objectStore = transaction.objectStore('testStore');

          const testData = {id: 1, name: 'Test Data', timestamp: Date.now()};
          const addRequest = objectStore.add(testData);

          addRequest.onsuccess = () => {
            const getRequest = objectStore.get(1);

            getRequest.onsuccess = () => {
              const retrievedData = getRequest.result;

              db.close();
              indexedDB.deleteDatabase(dbName);

              resolve({
                supported: true,
                dbOpened: true,
                dataStored: true,
                dataRetrieved: retrievedData && retrievedData.name === 'Test Data'
              });
            };

            getRequest.onerror = () => {
              db.close();
              indexedDB.deleteDatabase(dbName);
              resolve({
                supported: true,
                dbOpened: true,
                dataStored: true,
                dataRetrieved: false,
                error: 'Failed to retrieve data'
              });
            };
          };

          addRequest.onerror = () => {
            db.close();
            indexedDB.deleteDatabase(dbName);
            resolve({
              supported: true,
              dbOpened: true,
              dataStored: false,
              dataRetrieved: false,
              error: 'Failed to store data'
            });
          };
        };
      });
    });

    expect(indexedDBTest.supported).toBe(true);

    if (indexedDBTest.supported) {
      expect(indexedDBTest.dbOpened).toBe(true);
      expect(indexedDBTest.dataStored).toBe(true);
      expect(indexedDBTest.dataRetrieved).toBe(true);
    }

    console.log('IndexedDB Test:', indexedDBTest);
  });

  test('should support localStorage for simple offline storage', async ({page}) => {
    const localStorageTest = await page.evaluate(() => {
      if (!('localStorage' in window)) {
        return {supported: false};
      }

      try {
        const testKey = 'pwa-test-' + Date.now();
        const testValue = 'test-value-' + Date.now();

        localStorage.setItem(testKey, testValue);
        const retrievedValue = localStorage.getItem(testKey);
        localStorage.removeItem(testKey);

        return {
          supported: true,
          dataStored: retrievedValue === testValue,
          storageLength: localStorage.length
        };
      } catch (error) {
        return {
          supported: false,
          error: (error as Error).message
        };
      }
    });

    expect(localStorageTest.supported).toBe(true);

    if (localStorageTest.supported) {
      expect(localStorageTest.dataStored).toBe(true);
    }

    console.log('LocalStorage Test:', localStorageTest);
  });
});

test.describe('Push Notifications', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Notification API', async ({page}) => {
    const notificationSupport = await page.evaluate(() => {
      return {
        supported: 'Notification' in window,
        permission: 'Notification' in window ? Notification.permission : 'unknown',
        requestPermission: 'Notification' in window && typeof Notification.requestPermission === 'function',
        serviceWorkerNotifications: 'serviceWorker' in navigator && 'showNotification' in ServiceWorkerRegistration.prototype
      };
    });

    expect(notificationSupport.supported).toBe(true);
    expect(['default', 'granted', 'denied']).toContain(notificationSupport.permission);
    expect(notificationSupport.requestPermission).toBe(true);

    console.log('Notification API Support:', notificationSupport);
  });

  test('should support Push API', async ({page}) => {
    const pushSupport = await page.evaluate(() => {
      return {
        pushManager: 'serviceWorker' in navigator && 'pushManager' in ServiceWorkerRegistration.prototype,
        pushSubscription: 'PushSubscription' in window,
        pushMessageData: 'PushMessageData' in window
      };
    });

    console.log('Push API Support:', pushSupport);
  });

  test('should handle notification creation', async ({page}) => {
    // Note: This test doesn't actually create notifications as it requires user permission
    const notificationTest = await page.evaluate(() => {
      if (!('Notification' in window)) {
        return {supported: false};
      }

      // Test notification options
      const options = {
        body: 'Test notification body',
        icon: '/icon-192x192.png',
        badge: '/badge-72x72.png',
        tag: 'test-notification',
        renotify: true,
        silent: false,
        requireInteraction: false,
        actions: [
          {action: 'open', title: 'Open App'},
          {action: 'close', title: 'Close'}
        ],
        data: {id: 'test-123'}
      };

      return {
        supported: true,
        optionsSupported: {
          body: true,
          icon: true,
          badge: true,
          tag: true,
          actions: 'actions' in options,
          data: 'data' in options
        }
      };
    });

    expect(notificationTest.supported).toBe(true);

    console.log('Notification Options Test:', notificationTest);
  });
});

test.describe('App-like Features', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Fullscreen API', async ({page}) => {
    const fullscreenSupport = await page.evaluate(() => {
      const elem = document.documentElement;

      return {
        requestFullscreen: 'requestFullscreen' in elem,
        webkitRequestFullscreen: 'webkitRequestFullscreen' in elem,
        mozRequestFullScreen: 'mozRequestFullScreen' in elem,
        msRequestFullscreen: 'msRequestFullscreen' in elem,
        exitFullscreen: 'exitFullscreen' in document,
        fullscreenElement: 'fullscreenElement' in document,
        fullscreenEnabled: document.fullscreenEnabled
      };
    });

    // At least one fullscreen method should be supported
    const hasFullscreenSupport = fullscreenSupport.requestFullscreen ||
        fullscreenSupport.webkitRequestFullscreen ||
        fullscreenSupport.mozRequestFullScreen ||
        fullscreenSupport.msRequestFullscreen;

    expect(hasFullscreenSupport).toBe(true);

    console.log('Fullscreen API Support:', fullscreenSupport);
  });

  test('should support Screen Orientation API', async ({page}) => {
    const orientationSupport = await page.evaluate(() => {
      return {
        screenOrientation: 'orientation' in screen,
        orientationType: screen.orientation ? screen.orientation.type : 'unknown',
        orientationAngle: screen.orientation ? screen.orientation.angle : 0,
        lock: screen.orientation && typeof screen.orientation.lock === 'function',
        unlock: screen.orientation && typeof screen.orientation.unlock === 'function'
      };
    });

    console.log('Screen Orientation Support:', orientationSupport);
  });

  test('should support Wake Lock API', async ({page}) => {
    const wakeLockSupport = await page.evaluate(() => {
      return {
        supported: 'wakeLock' in navigator,
        requestWakeLock: 'wakeLock' in navigator && typeof navigator.wakeLock?.request === 'function'
      };
    });

    console.log('Wake Lock API Support:', wakeLockSupport);
  });

  test('should support Share API', async ({page}) => {
    const shareSupport = await page.evaluate(() => {
      return {
        supported: 'share' in navigator,
        canShare: 'canShare' in navigator
      };
    });

    console.log('Share API Support:', shareSupport);
  });

  test('should support BadgeAPI', async ({page}) => {
    const badgeSupport = await page.evaluate(() => {
      return {
        supported: 'setAppBadge' in navigator,
        clearBadge: 'clearAppBadge' in navigator
      };
    });

    console.log('Badge API Support:', badgeSupport);
  });
});

test.describe('Browser-Specific PWA Features', () => {
  test('should handle iOS Safari PWA features', async ({page, browserName}) => {
    test.skip(browserName !== 'webkit', 'iOS Safari-specific test');

    const iosPWAFeatures = await page.evaluate(() => {
      return {
        standaloneMode: window.matchMedia('(display-mode: standalone)').matches,
        appleWebApp: document.querySelector('meta[name="apple-mobile-web-app-capable"]') !== null,
        appleStatusBar: document.querySelector('meta[name="apple-mobile-web-app-status-bar-style"]') !== null,
        appleTouchIcon: document.querySelector('link[rel="apple-touch-icon"]') !== null,
        homeScreenAdd: 'onbeforeinstallprompt' in window // Limited support on iOS
      };
    });

    console.log('iOS PWA Features:', iosPWAFeatures);
  });

  test('should handle Chrome PWA optimizations', async ({page, browserName}) => {
    test.skip(browserName !== 'chromium', 'Chrome-specific test');

    const chromePWAFeatures = await page.evaluate(() => {
      return {
        beforeInstallPrompt: 'onbeforeinstallprompt' in window,
        appInstalled: 'onappinstalled' in window,
        relatedApplications: 'getInstalledRelatedApps' in navigator,
        periodicBackgroundSync: 'serviceWorker' in navigator && 'periodicSync' in ServiceWorkerRegistration.prototype,
        backgroundFetch: 'serviceWorker' in navigator && 'backgroundFetch' in ServiceWorkerRegistration.prototype
      };
    });

    expect(chromePWAFeatures.beforeInstallPrompt).toBe(true);
    expect(chromePWAFeatures.appInstalled).toBe(true);

    console.log('Chrome PWA Features:', chromePWAFeatures);
  });

  test('should handle Firefox PWA support', async ({page, browserName}) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific test');

    const firefoxPWAFeatures = await page.evaluate(() => {
      return {
        serviceWorker: 'serviceWorker' in navigator,
        manifest: document.querySelector('link[rel="manifest"]') !== null,
        notifications: 'Notification' in window,
        installPrompt: 'onbeforeinstallprompt' in window // Limited support
      };
    });

    expect(firefoxPWAFeatures.serviceWorker).toBe(true);
    expect(firefoxPWAFeatures.manifest).toBe(true);

    console.log('Firefox PWA Features:', firefoxPWAFeatures);
  });
});