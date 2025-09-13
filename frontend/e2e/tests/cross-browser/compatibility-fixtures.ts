/**
 * Cross-Browser Compatibility Test Fixtures
 * Provides test data, utilities, and fixtures for cross-browser testing
 */

import { Page, expect } from '@playwright/test';

// Type definitions for browser-specific APIs
interface PerformanceMemory {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
}

interface PerformanceWithMemory extends Performance {
  memory?: PerformanceMemory;
}

interface WindowWithWebkitAudioContext extends Window {
  webkitAudioContext?: typeof AudioContext;
}

interface TestHelpers {
  supports: (feature: string) => boolean;
  measurePerformance: (name: string, fn: Function) => { result: unknown; duration: number };
  captureConsole: () => {
    getLogs: () => string[];
    restore: () => void;
  };
}

interface WindowWithTestHelpers extends Window {
  testHelpers?: TestHelpers;
}

/**
 * Test data for various compatibility scenarios
 */
export const TestData = {
  // Text content for testing
  text: {
    simple: 'Hello World',
    withSpecialChars: 'Special chars: !@#$%^&*()_+-=[]{}|;:,.<>?',
    unicode: '🚀 Unicode: café, naïve, résumé, 中文, العربية, עברית, 日本語',
    longText: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. '.repeat(100),
    rtlText: 'مرحبا بالعالم - שלום עולם',
    htmlEntities: '&lt;script&gt;alert("test")&lt;/script&gt;'
  },

  // Form data for input testing
  forms: {
    login: {
      email: 'test@focushive.com',
      password: 'SecurePassword123!',
      remember: true
    },
    registration: {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      phone: '+1-555-123-4567',
      birthdate: '1990-05-15',
      website: 'https://johndoe.example.com'
    },
    profile: {
      bio: 'Software developer passionate about web technologies',
      skills: ['JavaScript', 'TypeScript', 'React', 'Node.js'],
      experience: 5,
      remote: true,
      salary: 75000
    }
  },

  // File data for upload testing
  files: {
    textFile: {
      name: 'test.txt',
      content: 'This is a test file for upload testing',
      mimeType: 'text/plain'
    },
    imageFile: {
      name: 'test.png',
      content: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==',
      mimeType: 'image/png'
    },
    jsonFile: {
      name: 'test.json',
      content: JSON.stringify({ test: 'data', numbers: [1, 2, 3], nested: { key: 'value' } }),
      mimeType: 'application/json'
    },
    csvFile: {
      name: 'test.csv',
      content: 'Name,Age,City\nJohn,30,New York\nJane,25,Los Angeles',
      mimeType: 'text/csv'
    }
  },

  // Media data for testing
  media: {
    // 1x1 pixel transparent PNG
    smallImage: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==',
    
    // Small SVG icon
    svgIcon: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjMDA3YmZmIi8+Cjwvc3ZnPgo=',
    
    // Minimal WebP image (if supported)
    webpImage: 'data:image/webp;base64,UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA=',
    
    // Small MP4 video placeholder
    videoPlaceholder: 'data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAAIZnJlZQAAAr1tZGF0'
  },

  // API endpoints for testing
  endpoints: {
    health: '/api/health',
    auth: '/api/auth/login',
    users: '/api/users',
    websocket: 'ws://localhost:8080/ws',
    sse: '/api/events/stream'
  },

  // Browser-specific test scenarios
  browsers: {
    chrome: {
      features: ['webgl2', 'webcodecs', 'paymentrequest', 'webusb'],
      limitations: [],
      optimizations: ['lazy-loading', 'code-splitting']
    },
    firefox: {
      features: ['webgl', 'webrtc', 'serviceworkers'],
      limitations: ['avif-images', 'webkit-prefixes'],
      optimizations: ['resource-hints', 'preload']
    },
    safari: {
      features: ['webgl', 'serviceworkers', 'webrtc'],
      limitations: ['indexeddb-private', 'websocket-background', 'autoplay'],
      optimizations: ['safari-specific-css', 'touch-optimized']
    },
    edge: {
      features: ['webgl2', 'webauthn', 'paymentrequest'],
      limitations: ['legacy-edge-apis'],
      optimizations: ['chromium-features', 'windows-integration']
    }
  }
};

/**
 * Compatibility test utilities
 */
export class CompatibilityUtils {
  static async getBrowserInfo(page: Page) {
    return await page.evaluate(() => {
      const ua = navigator.userAgent;
      return {
        userAgent: ua,
        vendor: navigator.vendor,
        platform: navigator.platform,
        language: navigator.language,
        languages: navigator.languages,
        onLine: navigator.onLine,
        cookieEnabled: navigator.cookieEnabled,
        viewport: {
          width: window.innerWidth,
          height: window.innerHeight,
          devicePixelRatio: window.devicePixelRatio
        }
      };
    });
  }

  static async getFeatureSupport(page: Page) {
    return await page.evaluate(() => {
      return {
        // Storage APIs
        localStorage: typeof localStorage !== 'undefined',
        sessionStorage: typeof sessionStorage !== 'undefined',
        indexedDB: typeof indexedDB !== 'undefined',
        
        // Network APIs
        fetch: typeof fetch !== 'undefined',
        websocket: typeof WebSocket !== 'undefined',
        eventSource: typeof EventSource !== 'undefined',
        
        // Media APIs
        webGL: (() => {
          try {
            const canvas = document.createElement('canvas');
            return !!(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
          } catch { return false; }
        })(),
        webGL2: (() => {
          try {
            const canvas = document.createElement('canvas');
            return !!canvas.getContext('webgl2');
          } catch { return false; }
        })(),
        webAudio: typeof AudioContext !== 'undefined' || typeof (window as WindowWithWebkitAudioContext).webkitAudioContext !== 'undefined',
        
        // File APIs
        fileAPI: typeof File !== 'undefined' && typeof FileReader !== 'undefined',
        dragDrop: (() => {
          const div = document.createElement('div');
          return 'draggable' in div || 'ondragstart' in div && 'ondrop' in div;
        })(),
        
        // Modern JavaScript features
        es6Classes: (() => { try { eval('class Test {}'); return true; } catch { return false; } })(),
        asyncAwait: (() => { try { eval('async function test() { await Promise.resolve(); }'); return true; } catch { return false; } })(),
        modules: typeof document.createElement('script').noModule === 'boolean',
        
        // PWA features
        serviceWorker: 'serviceWorker' in navigator,
        pushManager: 'serviceWorker' in navigator && 'pushManager' in ServiceWorkerRegistration.prototype,
        notifications: 'Notification' in window,
        
        // CSS features (basic detection)
        cssGrid: CSS.supports && CSS.supports('display', 'grid'),
        cssFlexbox: CSS.supports && CSS.supports('display', 'flex'),
        cssCustomProperties: CSS.supports && CSS.supports('color', 'var(--test)'),
        
        // Form features
        formValidation: (() => {
          const input = document.createElement('input');
          return typeof input.validity !== 'undefined';
        })(),
        inputTypes: (() => {
          const input = document.createElement('input');
          const types = ['email', 'url', 'number', 'range', 'date', 'time', 'color', 'search'];
          const supported: { [key: string]: boolean } = {};
          types.forEach(type => {
            input.type = type;
            supported[type] = input.type === type;
            input.type = 'text'; // Reset
          });
          return supported;
        })()
      };
    });
  }

  static async testPerformance(page: Page, operation: () => Promise<void>) {
    const startTime = Date.now();
    const startMemory = await page.evaluate(() => 
      (performance as PerformanceWithMemory).memory ? (performance as PerformanceWithMemory).memory.usedJSHeapSize : 0
    );

    await operation();

    const endTime = Date.now();
    const endMemory = await page.evaluate(() => 
      (performance as PerformanceWithMemory).memory ? (performance as PerformanceWithMemory).memory.usedJSHeapSize : 0
    );

    return {
      duration: endTime - startTime,
      memoryUsed: endMemory - startMemory
    };
  }

  static async waitForStableDOM(page: Page, timeout = 5000) {
    let lastHTML = '';
    let stableCount = 0;
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
      const currentHTML = await page.content();
      
      if (currentHTML === lastHTML) {
        stableCount++;
        if (stableCount >= 3) {
          return true; // DOM is stable
        }
      } else {
        stableCount = 0;
      }
      
      lastHTML = currentHTML;
      await page.waitForTimeout(100);
    }

    return false; // Timeout reached
  }

  static async injectTestHelpers(page: Page) {
    await page.addInitScript(() => {
      // Add global test helpers
      (window as WindowWithTestHelpers).testHelpers = {
        // Feature detection helper
        supports: (feature: string) => {
          const features: { [key: string]: () => boolean } = {
            'touch': () => 'ontouchstart' in window,
            'pointer': () => 'onpointerdown' in window,
            'webgl': () => {
              try {
                const canvas = document.createElement('canvas');
                return !!(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
              } catch { return false; }
            },
            'webrtc': () => 'RTCPeerConnection' in window,
            'websocket': () => 'WebSocket' in window,
            'serviceworker': () => 'serviceWorker' in navigator,
            'indexeddb': () => 'indexedDB' in window
          };
          
          return features[feature] ? features[feature]() : false;
        },
        
        // Performance measurement helper
        measurePerformance: (name: string, fn: Function) => {
          const start = performance.now();
          const result = fn();
          const end = performance.now();
          
          performance.mark(`${name}-start`);
          performance.mark(`${name}-end`);
          performance.measure(name, `${name}-start`, `${name}-end`);
          
          return { result, duration: end - start };
        },
        
        // Console capture helper
        captureConsole: () => {
          const logs: string[] = [];
          const originalLog = console.log;
          const originalError = console.error;
          const originalWarn = console.warn;
          
          console.log = (...args) => {
            logs.push(`LOG: ${args.join(' ')}`);
            originalLog.apply(console, args);
          };
          
          console.error = (...args) => {
            logs.push(`ERROR: ${args.join(' ')}`);
            originalError.apply(console, args);
          };
          
          console.warn = (...args) => {
            logs.push(`WARN: ${args.join(' ')}`);
            originalWarn.apply(console, args);
          };
          
          return {
            getLogs: () => logs,
            restore: () => {
              console.log = originalLog;
              console.error = originalError;
              console.warn = originalWarn;
            }
          };
        }
      };
    });
  }

  static async simulateSlowNetwork(page: Page) {
    // Simulate slow 3G connection
    const client = await page.context().newCDPSession(page);
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: 500 * 1024, // 500 KB/s
      uploadThroughput: 500 * 1024,   // 500 KB/s
      latency: 400 // 400ms latency
    });
  }

  static async simulateOffline(page: Page) {
    const client = await page.context().newCDPSession(page);
    await client.send('Network.emulateNetworkConditions', {
      offline: true,
      downloadThroughput: 0,
      uploadThroughput: 0,
      latency: 0
    });
  }

  static async restoreNetworkConditions(page: Page) {
    const client = await page.context().newCDPSession(page);
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: -1, // No throttling
      uploadThroughput: -1,   // No throttling
      latency: 0
    });
  }
}

/**
 * Cross-browser test assertions
 */
export class CrossBrowserAssertions {
  static async expectFeatureSupport(page: Page, feature: string, expectedSupport = true) {
    const supported = await page.evaluate((feature) => {
      return (window as WindowWithTestHelpers).testHelpers?.supports(feature) || false;
    }, feature);

    if (expectedSupport) {
      expect(supported, `${feature} should be supported`).toBe(true);
    } else {
      expect(supported, `${feature} should not be supported`).toBe(false);
    }
  }

  static async expectPerformanceWithin(page: Page, operation: () => Promise<void>, maxDuration: number) {
    const performance = await CompatibilityUtils.testPerformance(page, operation);
    expect(performance.duration, `Operation should complete within ${maxDuration}ms`).toBeLessThan(maxDuration);
    
    return performance;
  }

  static async expectNoConsoleErrors(page: Page) {
    const logs: string[] = [];
    
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        logs.push(msg.text());
      }
    });
    
    // Allow some time for any console errors to appear
    await page.waitForTimeout(1000);
    
    const criticalErrors = logs.filter(log => 
      !log.includes('favicon') && 
      !log.includes('sourcemap') &&
      !log.includes('DevTools')
    );
    
    expect(criticalErrors, 'Should not have critical console errors').toHaveLength(0);
  }

  static async expectAccessibleNavigation(page: Page, elements: string[]) {
    for (let i = 0; i < elements.length; i++) {
      await page.keyboard.press('Tab');
      const focused = await page.evaluate(() => document.activeElement?.tagName.toLowerCase());
      
      if (i === 0) {
        expect(focused, `First element should be focusable`).toBeTruthy();
      }
    }
  }

  static async expectResponsiveLayout(page: Page, viewports: Array<{width: number, height: number}>) {
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await page.waitForTimeout(200); // Allow layout to settle
      
      const hasHorizontalScroll = await page.evaluate(() => {
        return document.documentElement.scrollWidth > document.documentElement.clientWidth;
      });
      
      expect(hasHorizontalScroll, `No horizontal scroll at ${viewport.width}x${viewport.height}`).toBe(false);
    }
  }
}

/**
 * Browser-specific test configurations
 */
export const BrowserConfigs = {
  chrome: {
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    features: ['webgl2', 'webcodecs', 'paymentrequest'],
    skipTests: []
  },
  
  firefox: {
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
    features: ['webgl', 'webrtc'],
    skipTests: ['avif-images']
  },
  
  safari: {
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15',
    features: ['webgl', 'serviceworkers'],
    skipTests: ['indexeddb-private-browsing', 'websocket-background-tabs']
  },
  
  edge: {
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0',
    features: ['webgl2', 'webauthn', 'paymentrequest'],
    skipTests: []
  }
};

/**
 * Common viewports for responsive testing
 */
export const TestViewports = {
  mobile: [
    { width: 320, height: 568, name: 'iPhone SE' },
    { width: 375, height: 667, name: 'iPhone 8' },
    { width: 375, height: 812, name: 'iPhone X' },
    { width: 360, height: 640, name: 'Galaxy S5' }
  ],
  tablet: [
    { width: 768, height: 1024, name: 'iPad Portrait' },
    { width: 1024, height: 768, name: 'iPad Landscape' },
    { width: 834, height: 1194, name: 'iPad Pro Portrait' }
  ],
  desktop: [
    { width: 1024, height: 768, name: 'Small Desktop' },
    { width: 1366, height: 768, name: 'Standard Desktop' },
    { width: 1920, height: 1080, name: 'Full HD' },
    { width: 2560, height: 1440, name: '2K Display' }
  ]
};