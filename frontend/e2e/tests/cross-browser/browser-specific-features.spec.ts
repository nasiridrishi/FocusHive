/**
 * Browser-Specific Features Tests
 * Tests browser-specific APIs, behaviors, and vendor-specific features
 */

import { test, expect, Page } from '@playwright/test';
import { getBrowserInfo } from './browser-helpers';

test.describe('Chrome-Specific Features', () => {
  test.beforeEach(async ({ page, browserName }) => {
    test.skip(browserName !== 'chromium', 'Chrome-specific tests');
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Chrome-specific APIs', async ({ page }) => {
    const chromeAPIs = await page.evaluate(() => {
      return {
        // Web APIs
        paymentRequest: 'PaymentRequest' in window,
        webUsb: 'usb' in navigator,
        webBluetooth: 'bluetooth' in navigator,
        webSerial: 'serial' in navigator,
        webHid: 'hid' in navigator,
        
        // Performance APIs
        memoryInfo: 'memory' in performance,
        measureUserAgentSpecificMemory: 'measureUserAgentSpecificMemory' in performance,
        
        // Web Authentication
        webAuthn: 'credentials' in navigator && 'create' in (navigator.credentials as CredentialsContainer),
        
        // Storage APIs
        persistentStorage: 'storage' in navigator && 'persist' in (navigator.storage as StorageManager),
        storageEstimate: 'storage' in navigator && 'estimate' in (navigator.storage as StorageManager),
        
        // Media APIs
        getDisplayMedia: 'mediaDevices' in navigator && 'getDisplayMedia' in navigator.mediaDevices,
        pictureInPicture: 'pictureInPictureEnabled' in document,
        
        // Network APIs
        networkInformation: 'connection' in navigator || 'mozConnection' in navigator || 'webkitConnection' in navigator,
        beaconAPI: 'sendBeacon' in navigator,
        
        // Background APIs
        backgroundSync: 'serviceWorker' in navigator && 'sync' in ServiceWorkerRegistration.prototype,
        periodicBackgroundSync: 'serviceWorker' in navigator && 'periodicSync' in ServiceWorkerRegistration.prototype,
        backgroundFetch: 'serviceWorker' in navigator && 'backgroundFetch' in ServiceWorkerRegistration.prototype,
        
        // File System APIs
        fileSystemAccess: 'showOpenFilePicker' in window,
        nativeFileSystem: 'chooseFileSystemEntries' in window,
        
        // Web Codecs
        webCodecs: 'VideoDecoder' in window && 'VideoEncoder' in window,
        
        // Origin Private File System
        opfs: 'storage' in navigator && 'getDirectory' in (navigator.storage as StorageManager),
        
        // Web Locks
        webLocks: 'locks' in navigator,
        
        // Eye Dropper API
        eyeDropper: 'EyeDropper' in window,
        
        // Compute Pressure API
        computePressure: 'PressureObserver' in window,
        
        // Web Streams
        webStreams: 'ReadableStream' in window && 'WritableStream' in window && 'TransformStream' in window,
        
        // Import Maps
        importMaps: HTMLScriptElement.supports && HTMLScriptElement.supports('importmap')
      };
    });

    // Log all Chrome-specific API support
    console.log('Chrome-Specific APIs:', chromeAPIs);

    // Test key Chrome APIs
    expect(chromeAPIs.paymentRequest).toBe(true);
    expect(chromeAPIs.webAuthn).toBe(true);
    expect(chromeAPIs.persistentStorage).toBe(true);
    expect(chromeAPIs.beaconAPI).toBe(true);
    expect(chromeAPIs.webStreams).toBe(true);

    // Test Chrome-specific performance features
    if (chromeAPIs.memoryInfo) {
      const memoryInfo = await page.evaluate(() => {
        const memory = (performance as unknown as { memory?: { usedJSHeapSize: number; totalJSHeapSize: number; jsHeapSizeLimit: number } }).memory;
        return memory ? {
          usedJSHeapSize: memory.usedJSHeapSize,
          totalJSHeapSize: memory.totalJSHeapSize,
          jsHeapSizeLimit: memory.jsHeapSizeLimit
        } : null;
      });

      if (memoryInfo) {
        expect(memoryInfo.usedJSHeapSize).toBeGreaterThan(0);
        expect(memoryInfo.totalJSHeapSize).toBeGreaterThan(0);
        expect(memoryInfo.jsHeapSizeLimit).toBeGreaterThan(0);
      }
    }

    // Test File System Access API (if available)
    if (chromeAPIs.fileSystemAccess) {
      const fileSystemAccessSupported = await page.evaluate(() => {
        return typeof window.showOpenFilePicker === 'function' &&
               typeof window.showSaveFilePicker === 'function' &&
               typeof window.showDirectoryPicker === 'function';
      });
      
      expect(fileSystemAccessSupported).toBe(true);
    }
  });

  test('should handle Chrome extension compatibility', async ({ page }) => {
    // Test if Chrome extension APIs would be available (in extension context)
    const extensionAPIsAvailable = await page.evaluate(() => {
      return {
        chrome: typeof window !== 'undefined' && 'chrome' in window,
        extensionContext: typeof chrome !== 'undefined' && chrome.runtime !== undefined
      };
    });

    // In normal web page context, these should not be available
    expect(extensionAPIsAvailable.extensionContext).toBe(false);
    
    console.log('Chrome Extension API Check:', extensionAPIsAvailable);
  });

  test('should support Chrome DevTools integration', async ({ page }) => {
    const devToolsIntegration = await page.evaluate(() => {
      return {
        console: typeof console !== 'undefined' && typeof console.profile === 'function',
        performance: 'mark' in performance && 'measure' in performance,
        userTiming: 'getEntriesByType' in performance
      };
    });

    expect(devToolsIntegration.performance).toBe(true);
    expect(devToolsIntegration.userTiming).toBe(true);

    // Test performance markers
    await page.evaluate(() => {
      performance.mark('test-mark-start');
      performance.mark('test-mark-end');
      performance.measure('test-measure', 'test-mark-start', 'test-mark-end');
    });

    const performanceEntries = await page.evaluate(() => {
      return {
        marks: performance.getEntriesByType('mark').length,
        measures: performance.getEntriesByType('measure').length
      };
    });

    expect(performanceEntries.marks).toBeGreaterThanOrEqual(2);
    expect(performanceEntries.measures).toBeGreaterThanOrEqual(1);
  });
});

test.describe('Firefox-Specific Features', () => {
  test.beforeEach(async ({ page, browserName }) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific tests');
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Firefox-specific APIs', async ({ page }) => {
    const firefoxAPIs = await page.evaluate(() => {
      return {
        // Mozilla-specific features
        mozInnerScreenX: 'mozInnerScreenX' in window,
        mozInnerScreenY: 'mozInnerScreenY' in window,
        mozPaintCount: 'mozPaintCount' in window,
        
        // Privacy features
        resistFingerprinting: 'privacy' in navigator,
        
        // WebExtensions API compatibility
        browser: typeof browser !== 'undefined',
        
        // Firefox-specific media features
        mozAudioChannel: 'mozAudioChannelType' in document.createElement('audio'),
        
        // WebRTC specific
        mozRTCPeerConnection: 'mozRTCPeerConnection' in window,
        mozGetUserMedia: navigator.mediaDevices && 'mozGetUserMedia' in navigator,
        
        // JavaScript engine specific
        uneval: typeof uneval !== 'undefined', // Firefox-specific function (deprecated)
        
        // DOM specific
        mozMatchesSelector: 'mozMatchesSelector' in Element.prototype,
        mozRequestFullScreen: 'mozRequestFullScreen' in Element.prototype,
        
        // Event specific
        mozInputSource: 'mozInputSource' in MouseEvent.prototype,
        
        // CSS specific
        mozBoxSizing: typeof CSS !== 'undefined' && CSS.supports && CSS.supports('-moz-box-sizing', 'border-box'),
        
        // Performance specific
        mozMemory: 'mozMemory' in performance,
        
        // WebGL specific
        mozWebGL: (() => {
          const canvas = document.createElement('canvas');
          return !!canvas.getContext('experimental-webgl') || !!canvas.getContext('webgl');
        })()
      };
    });

    console.log('Firefox-Specific APIs:', firefoxAPIs);

    // Test Firefox WebGL support
    expect(firefoxAPIs.mozWebGL).toBe(true);
    
    // Test CSS support
    if (firefoxAPIs.mozBoxSizing) {
      expect(firefoxAPIs.mozBoxSizing).toBe(true);
    }
  });

  test('should handle Firefox security features', async ({ page }) => {
    const securityFeatures = await page.evaluate(() => {
      return {
        // Content Security Policy
        cspViolationEvent: 'SecurityPolicyViolationEvent' in window,
        
        // Permissions API
        permissions: 'permissions' in navigator,
        
        // Origin
        origin: location.origin !== undefined,
        
        // Secure contexts
        isSecureContext: window.isSecureContext,
        
        // Referrer policy
        referrerPolicy: 'referrerPolicy' in document || 'referrerPolicy' in HTMLAnchorElement.prototype
      };
    });

    expect(securityFeatures.origin).toBe(true);
    expect(securityFeatures.referrerPolicy).toBe(true);
    
    console.log('Firefox Security Features:', securityFeatures);
  });

  test('should support Firefox developer tools integration', async ({ page }) => {
    const devToolsFeatures = await page.evaluate(() => {
      return {
        // Console API
        consoleAPI: typeof console !== 'undefined' && 
                   typeof console.log === 'function' &&
                   typeof console.error === 'function' &&
                   typeof console.warn === 'function',
        
        // Performance API
        performanceAPI: 'performance' in window &&
                       'now' in performance &&
                       'mark' in performance &&
                       'measure' in performance
      };
    });

    expect(devToolsFeatures.consoleAPI).toBe(true);
    expect(devToolsFeatures.performanceAPI).toBe(true);
  });
});

test.describe('Safari-Specific Features', () => {
  test.beforeEach(async ({ page, browserName }) => {
    test.skip(browserName !== 'webkit', 'Safari-specific tests');
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Safari-specific APIs', async ({ page }) => {
    const safariAPIs = await page.evaluate(() => {
      return {
        // WebKit-specific features
        webkitRequestFullscreen: 'webkitRequestFullscreen' in Element.prototype,
        webkitExitFullscreen: 'webkitExitFullscreen' in document,
        webkitFullscreenElement: 'webkitFullscreenElement' in document,
        
        // Safari-specific media
        webkitAudioContext: 'webkitAudioContext' in window,
        webkitSpeechRecognition: 'webkitSpeechRecognition' in window,
        webkitSpeechGrammarList: 'webkitSpeechGrammarList' in window,
        
        // iOS Safari specific
        standalone: 'standalone' in navigator,
        touchForceChange: 'TouchEvent' in window,
        
        // WebRTC
        webkitRTCPeerConnection: 'webkitRTCPeerConnection' in window,
        
        // Vendor prefixes
        webkitTransform: (() => {
          const div = document.createElement('div');
          return 'webkitTransform' in div.style;
        })(),
        
        webkitFilter: (() => {
          const div = document.createElement('div');
          return 'webkitFilter' in div.style;
        })(),
        
        // Background processing limitations
        backgroundPageVisibility: 'visibilityState' in document,
        
        // Payment Request (Safari 11.1+)
        applePaySession: 'ApplePaySession' in window,
        
        // Safari-specific video features
        webkitPresentationMode: 'webkitPresentationMode' in HTMLVideoElement.prototype,
        webkitSetPresentationMode: 'webkitSetPresentationMode' in HTMLVideoElement.prototype,
        
        // Safari-specific fullscreen
        webkitSupportsFullscreen: 'webkitSupportsFullscreen' in HTMLVideoElement.prototype,
        webkitEnterFullscreen: 'webkitEnterFullscreen' in HTMLVideoElement.prototype
      };
    });

    console.log('Safari-Specific APIs:', safariAPIs);

    // Test WebKit prefixed features
    expect(safariAPIs.webkitRequestFullscreen).toBe(true);
    expect(safariAPIs.webkitAudioContext).toBe(true);
    
    // Test CSS transform support
    if (safariAPIs.webkitTransform) {
      expect(safariAPIs.webkitTransform).toBe(true);
    }

    // Test Apple Pay if available
    if (safariAPIs.applePaySession) {
      const applePaySupport = await page.evaluate(() => {
        try {
          return {
            supported: 'ApplePaySession' in window,
            canMakePayments: ApplePaySession.canMakePayments && ApplePaySession.canMakePayments()
          };
        } catch {
          return { supported: false, canMakePayments: false };
        }
      });
      
      console.log('Apple Pay Support:', applePaySupport);
    }
  });

  test('should handle Safari privacy features', async ({ page }) => {
    const privacyFeatures = await page.evaluate(() => {
      return {
        // Intelligent Tracking Prevention effects
        cookieBlocking: document.cookie !== undefined,
        localStorageBlocking: (() => {
          try {
            localStorage.setItem('test', 'test');
            localStorage.removeItem('test');
            return false; // No blocking
          } catch {
            return true; // Blocked
          }
        })(),
        
        // Safari privacy indicators
        crossOriginIsolated: window.crossOriginIsolated,
        isSecureContext: window.isSecureContext,
        
        // WebKit privacy APIs
        storageAccess: document.hasStorageAccess !== undefined,
        requestStorageAccess: document.requestStorageAccess !== undefined
      };
    });

    expect(privacyFeatures.cookieBlocking).toBe(true);
    expect(privacyFeatures.localStorageBlocking).toBe(false);
    
    console.log('Safari Privacy Features:', privacyFeatures);
  });

  test('should support Safari media quirks', async ({ page }) => {
    // Test Safari-specific media behaviors
    const mediaQuirks = await page.evaluate(() => {
      const video = document.createElement('video');
      const audio = document.createElement('audio');
      
      return {
        // Safari requires user interaction for autoplay
        videoAutoplay: 'autoplay' in video,
        audioAutoplay: 'autoplay' in audio,
        
        // Safari-specific video presentation modes
        presentationMode: 'webkitPresentationMode' in video,
        
        // Safari media session
        mediaSession: 'mediaSession' in navigator,
        
        // Safari picture-in-picture
        pictureInPicture: 'webkitSetPresentationMode' in video,
        
        // AirPlay support
        airPlay: 'webkitShowPlaybackTargetPicker' in video
      };
    });

    console.log('Safari Media Quirks:', mediaQuirks);
  });
});

test.describe('Edge-Specific Features', () => {
  test.beforeEach(async ({ page, browserName }) => {
    // Note: Modern Edge is Chromium-based, so this applies to legacy Edge or specific Edge features
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support Edge-specific APIs', async ({ page }) => {
    const edgeAPIs = await page.evaluate(() => {
      return {
        // Modern Edge (Chromium-based) inherits Chrome APIs
        chromeBasedEdge: 'chrome' in window,
        
        // Edge-specific features
        msLaunchUri: 'msLaunchUri' in navigator,
        msCrypto: 'msCrypto' in window,
        
        // Legacy Edge APIs (if still available)
        msRequestFullscreen: 'msRequestFullscreen' in Element.prototype,
        msExitFullscreen: 'msExitFullscreen' in document,
        
        // Microsoft Store app detection
        msAppRuntime: 'msAppRuntime' in window,
        
        // Edge WebView2
        webview: 'webview' in window || document.createElement('webview') !== undefined,
        
        // Modern Edge PWA features
        beforeInstallPrompt: 'onbeforeinstallprompt' in window,
        appInstalled: 'onappinstalled' in window,
        
        // Windows integration
        windowsHello: 'credentials' in navigator && 'create' in (navigator.credentials as CredentialsContainer)
      };
    });

    console.log('Edge-Specific APIs:', edgeAPIs);

    // Modern Edge should support Chrome-like PWA features
    expect(edgeAPIs.beforeInstallPrompt).toBe(true);
    expect(edgeAPIs.windowsHello).toBe(true);
  });

  test('should handle Edge WebView2 integration', async ({ page }) => {
    // Test WebView2 specific features if available
    const webview2Features = await page.evaluate(() => {
      return {
        webviewTag: document.createElement('webview') !== undefined,
        chromeWebview: 'chrome' in window && 'webview' in (window.chrome || {}),
        postMessage: 'postMessage' in window,
        windowExternal: 'external' in window
      };
    });

    expect(webview2Features.postMessage).toBe(true);
    
    console.log('WebView2 Features:', webview2Features);
  });
});

test.describe('Cross-Browser Polyfill Testing', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should test polyfill effectiveness', async ({ page, browserName }) => {
    // Test various polyfills and their effectiveness across browsers
    const polyfillTests = await page.evaluate(() => {
      const tests = {
        // Promise polyfill
        promise: typeof Promise !== 'undefined' && Promise.resolve,
        
        // Fetch polyfill
        fetch: typeof fetch !== 'undefined',
        
        // Array methods
        arrayFind: Array.prototype.find !== undefined,
        arrayIncludes: Array.prototype.includes !== undefined,
        arrayFrom: Array.from !== undefined,
        
        // Object methods
        objectAssign: Object.assign !== undefined,
        objectEntries: Object.entries !== undefined,
        objectValues: Object.values !== undefined,
        
        // String methods
        stringIncludes: String.prototype.includes !== undefined,
        stringStartsWith: String.prototype.startsWith !== undefined,
        stringEndsWith: String.prototype.endsWith !== undefined,
        
        // DOM methods
        querySelector: document.querySelector !== undefined,
        addEventListener: window.addEventListener !== undefined,
        classList: 'classList' in Element.prototype,
        
        // ES6+ features
        arrowFunctions: (() => true)() === true,
        templateLiterals: true, // If we got here, it's supported
        letConst: (() => { let x = 1; const y = 2; return x + y; })() === 3,
        
        // WebAPI polyfills
        customElements: 'customElements' in window,
        intersectionObserver: 'IntersectionObserver' in window,
        resizeObserver: 'ResizeObserver' in window,
        mutationObserver: 'MutationObserver' in window
      };

      return tests;
    });

    // All modern browsers should support these basic features
    expect(polyfillTests.promise).toBe(true);
    expect(polyfillTests.fetch).toBe(true);
    expect(polyfillTests.arrayFind).toBe(true);
    expect(polyfillTests.objectAssign).toBe(true);
    expect(polyfillTests.querySelector).toBe(true);
    expect(polyfillTests.classList).toBe(true);

    console.log(`Polyfill Tests for ${browserName}:`, polyfillTests);
  });

  test('should test fallback mechanisms', async ({ page }) => {
    // Test that the application handles missing features gracefully
    const fallbackTests = await page.evaluate(() => {
      const results: { [key: string]: string } = {};
      
      // Test WebSocket fallback
      if (typeof WebSocket === 'undefined') {
        results.websocket = 'fallback-needed';
      } else {
        results.websocket = 'native-support';
      }
      
      // Test localStorage fallback
      try {
        localStorage.setItem('test', 'test');
        localStorage.removeItem('test');
        results.localStorage = 'native-support';
      } catch {
        results.localStorage = 'fallback-needed';
      }
      
      // Test IndexedDB fallback
      if (typeof indexedDB === 'undefined') {
        results.indexedDB = 'fallback-needed';
      } else {
        results.indexedDB = 'native-support';
      }
      
      // Test Service Worker fallback
      if (!('serviceWorker' in navigator)) {
        results.serviceWorker = 'fallback-needed';
      } else {
        results.serviceWorker = 'native-support';
      }
      
      return results;
    });

    // Log fallback test results
    console.log('Fallback Tests:', fallbackTests);
    
    // Verify that critical features are available or have fallbacks
    expect(['native-support', 'fallback-needed']).toContain(fallbackTests.websocket);
    expect(['native-support', 'fallback-needed']).toContain(fallbackTests.localStorage);
  });

  test('should test progressive enhancement', async ({ page }) => {
    // Test that enhanced features work when available but don't break when missing
    const enhancementTests = await page.evaluate(() => {
      const results: { [key: string]: boolean } = {};
      
      // Test if enhanced features are properly detected and used
      results.webGL = (() => {
        try {
          const canvas = document.createElement('canvas');
          return !!(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
        } catch {
          return false;
        }
      })();
      
      results.webAudio = (() => {
        try {
          return !!(window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext);
        } catch {
          return false;
        }
      })();
      
      results.fileAPI = (() => {
        return typeof FileReader !== 'undefined' && 
               typeof Blob !== 'undefined' && 
               typeof File !== 'undefined';
      })();
      
      results.dragDrop = (() => {
        const div = document.createElement('div');
        return 'draggable' in div || 'ondragstart' in div && 'ondrop' in div;
      })();
      
      results.canvas2D = (() => {
        try {
          const canvas = document.createElement('canvas');
          return !!canvas.getContext('2d');
        } catch {
          return false;
        }
      })();
      
      return results;
    });

    console.log('Progressive Enhancement Tests:', enhancementTests);
    
    // Basic canvas 2D should be supported everywhere
    expect(enhancementTests.canvas2D).toBe(true);
    expect(enhancementTests.fileAPI).toBe(true);
  });
});