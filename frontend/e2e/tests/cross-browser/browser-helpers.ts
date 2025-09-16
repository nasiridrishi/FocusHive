/**
 * Browser Detection and Compatibility Utilities
 * Helper functions for cross-browser testing and feature detection
 */

import {BrowserContext, Page} from '@playwright/test';

export interface BrowserInfo {
  name: string;
  version: string;
  platform: string;
  engine: string;
  isMobile: boolean;
  isTablet: boolean;
  viewport: {
    width: number;
    height: number;
  };
}

export interface FeatureSupport {
  [feature: string]: boolean;
}

export interface PerformanceMemory {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
}

export interface BrowserPerformanceMetrics {
  navigation: {
    domContentLoaded: number;
    loadComplete: number;
    domInteractive: number;
    firstByteTime: number;
  };
  paint: {
    firstPaint?: number;
    firstContentfulPaint?: number;
  };
  memory: PerformanceMemory | null;
}

export interface MediaSupport {
  images: { [format: string]: boolean };
  video: { [format: string]: string };
  audio: { [format: string]: string };
}

export interface CompatibilityReport {
  browser: BrowserInfo;
  features: FeatureSupport;
  performance: BrowserPerformanceMetrics | null;
  media: MediaSupport;
  timestamp: string;
  userAgent: string;
}

// Extend the global Window interface for browser-specific properties
declare global {
  interface Window {
    WebSocket?: typeof WebSocket;
    indexedDB?: IDBFactory;
  }

  interface Performance {
    memory?: PerformanceMemory;
  }
}

/**
 * Get detailed browser information from the page
 */
export async function getBrowserInfo(page: Page): Promise<BrowserInfo> {
  return await page.evaluate(() => {
    const ua = navigator.userAgent;
    const platform = navigator.platform;

    // Browser detection
    let name = 'unknown';
    let version = 'unknown';
    let engine = 'unknown';

    if (ua.includes('Chrome') && !ua.includes('Chromium')) {
      name = 'chrome';
      version = ua.match(/Chrome\/([0-9.]+)/)?.[1] || 'unknown';
      engine = 'blink';
    } else if (ua.includes('Firefox')) {
      name = 'firefox';
      version = ua.match(/Firefox\/([0-9.]+)/)?.[1] || 'unknown';
      engine = 'gecko';
    } else if (ua.includes('Safari') && !ua.includes('Chrome')) {
      name = 'safari';
      version = ua.match(/Version\/([0-9.]+)/)?.[1] || 'unknown';
      engine = 'webkit';
    } else if (ua.includes('Edg')) {
      name = 'edge';
      version = ua.match(/Edg\/([0-9.]+)/)?.[1] || 'unknown';
      engine = 'blink';
    }

    // Device detection
    const isMobile = /Mobile|Android|iPhone|iPad/i.test(ua);
    const isTablet = /iPad|Android.*(?!Mobile)/i.test(ua);

    return {
      name,
      version,
      platform,
      engine,
      isMobile,
      isTablet,
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      }
    };
  });
}

/**
 * Detect browser feature support
 */
export async function detectFeatureSupport(page: Page): Promise<FeatureSupport> {
  return await page.evaluate(() => {
    const features: { [key: string]: boolean } = {};

    // JavaScript features
    features['es6-modules'] = typeof Symbol !== 'undefined';
    features['async-await'] = (async () => {
    })().constructor.name === 'AsyncFunction';
    features['arrow-functions'] = (() => {
    }).constructor.name === 'Function';
    features['template-literals'] = true; // If we get here, it's supported
    features['destructuring'] = true; // If we get here, it's supported
    features['spread-operator'] = true; // If we get here, it's supported
    features['promises'] = typeof Promise !== 'undefined';
    features['fetch'] = typeof fetch !== 'undefined';

    // Web APIs
    features['websocket'] = typeof WebSocket !== 'undefined';
    features['serviceworker'] = 'serviceWorker' in navigator;
    features['indexeddb'] = 'indexedDB' in window;
    features['localstorage'] = typeof Storage !== 'undefined';
    features['sessionstorage'] = typeof sessionStorage !== 'undefined';
    features['geolocation'] = 'geolocation' in navigator;
    features['notifications'] = 'Notification' in window;
    features['fullscreen'] = 'requestFullscreen' in document.documentElement ||
        'webkitRequestFullscreen' in document.documentElement ||
        'mozRequestFullScreen' in document.documentElement ||
        'msRequestFullscreen' in document.documentElement;
    features['web-audio'] = 'AudioContext' in window || 'webkitAudioContext' in window;
    features['webrtc'] = 'RTCPeerConnection' in window;
    features['webgl'] = (() => {
      try {
        const canvas = document.createElement('canvas');
        return !!(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
      } catch {
        return false;
      }
    })();
    features['webgl2'] = (() => {
      try {
        const canvas = document.createElement('canvas');
        return !!canvas.getContext('webgl2');
      } catch {
        return false;
      }
    })();

    // CSS features
    features['css-grid'] = CSS.supports('display', 'grid');
    features['css-flexbox'] = CSS.supports('display', 'flex');
    features['css-custom-properties'] = CSS.supports('color', 'var(--test)');
    features['css-transforms'] = CSS.supports('transform', 'rotate(45deg)');
    features['css-animations'] = CSS.supports('animation-name', 'test');
    features['css-transitions'] = CSS.supports('transition', 'all 1s');
    features['css-filters'] = CSS.supports('filter', 'blur(1px)');
    features['css-backdrop-filter'] = CSS.supports('backdrop-filter', 'blur(1px)');
    features['css-sticky'] = CSS.supports('position', 'sticky');
    features['css-aspect-ratio'] = CSS.supports('aspect-ratio', '1/1');
    features['css-container-queries'] = CSS.supports('container-type', 'inline-size');

    // Media format support
    features['webp'] = (() => {
      const canvas = document.createElement('canvas');
      canvas.width = canvas.height = 1;
      return canvas.toDataURL('image/webp').indexOf('webp') !== -1;
    })();

    features['avif'] = (() => {
      const canvas = document.createElement('canvas');
      canvas.width = canvas.height = 1;
      return canvas.toDataURL('image/avif').indexOf('avif') !== -1;
    })();

    // Video/Audio support
    const video = document.createElement('video');
    features['video-mp4'] = video.canPlayType('video/mp4') !== '';
    features['video-webm'] = video.canPlayType('video/webm') !== '';
    features['video-ogg'] = video.canPlayType('video/ogg') !== '';

    const audio = document.createElement('audio');
    features['audio-mp3'] = audio.canPlayType('audio/mpeg') !== '';
    features['audio-ogg'] = audio.canPlayType('audio/ogg') !== '';
    features['audio-wav'] = audio.canPlayType('audio/wav') !== '';
    features['audio-aac'] = audio.canPlayType('audio/aac') !== '';

    // Touch and input
    features['touch-events'] = 'ontouchstart' in window;
    features['pointer-events'] = 'onpointerdown' in window;
    features['device-orientation'] = 'ondeviceorientation' in window;

    // PWA features
    features['app-manifest'] = 'onappinstalled' in window;
    features['beforeinstallprompt'] = 'onbeforeinstallprompt' in window;

    // Performance APIs
    features['performance-observer'] = 'PerformanceObserver' in window;
    features['intersection-observer'] = 'IntersectionObserver' in window;
    features['resize-observer'] = 'ResizeObserver' in window;
    features['mutation-observer'] = 'MutationObserver' in window;

    return features;
  });
}

/**
 * Check if browser supports a specific feature
 */
export async function supportsFeature(page: Page, feature: string): Promise<boolean> {
  const features = await detectFeatureSupport(page);
  return features[feature] || false;
}

/**
 * Get browser performance metrics
 */
export async function getBrowserPerformanceMetrics(page: Page): Promise<BrowserPerformanceMetrics | null> {
  return await page.evaluate(() => {
    if (!window.performance) return null;

    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    const paintEntries = performance.getEntriesByType('paint');

    return {
      navigation: {
        domContentLoaded: navigation?.domContentLoadedEventEnd - navigation?.domContentLoadedEventStart,
        loadComplete: navigation?.loadEventEnd - navigation?.loadEventStart,
        domInteractive: navigation?.domInteractive - navigation?.navigationStart,
        firstByteTime: navigation?.responseStart - navigation?.requestStart,
      },
      paint: {
        firstPaint: paintEntries.find(entry => entry.name === 'first-paint')?.startTime,
        firstContentfulPaint: paintEntries.find(entry => entry.name === 'first-contentful-paint')?.startTime,
      },
      memory: performance.memory ? {
        usedJSHeapSize: performance.memory.usedJSHeapSize,
        totalJSHeapSize: performance.memory.totalJSHeapSize,
        jsHeapSizeLimit: performance.memory.jsHeapSizeLimit,
      } : null,
    };
  });
}

/**
 * Test WebSocket connectivity
 */
export async function testWebSocketConnection(page: Page, url: string): Promise<{
  supported: boolean;
  connectionTime?: number;
  error?: string;
}> {
  return await page.evaluate(async (wsUrl) => {
    if (typeof WebSocket === 'undefined') {
      return {supported: false, error: 'WebSocket not supported'};
    }

    return new Promise((resolve) => {
      const startTime = Date.now();
      const ws = new WebSocket(wsUrl);

      const cleanup = (): void => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.close();
        }
      };

      const timeout = setTimeout(() => {
        cleanup();
        resolve({supported: false, error: 'Connection timeout'});
      }, 5000);

      ws.onopen = () => {
        clearTimeout(timeout);
        const connectionTime = Date.now() - startTime;
        cleanup();
        resolve({supported: true, connectionTime});
      };

      ws.onerror = (_error) => {
        clearTimeout(timeout);
        cleanup();
        resolve({supported: false, error: 'Connection failed'});
      };
    });
  }, url);
}

/**
 * Get console logs categorized by type
 */
export async function getConsoleLogs(page: Page): Promise<{
  errors: string[];
  warnings: string[];
  logs: string[];
}> {
  return await page.evaluate(() => {
    // This is a simplified version - in practice, you'd want to capture these during page execution
    return {
      errors: [],
      warnings: [],
      logs: []
    };
  });
}

/**
 * Test CSS feature support
 */
export async function testCSSSupport(page: Page, property: string, value: string): Promise<boolean> {
  return await page.evaluate(([prop, val]) => {
    return CSS.supports(prop, val);
  }, [property, value]);
}

/**
 * Capture detailed viewport information
 */
export async function getViewportInfo(page: Page): Promise<boolean> {
  return await page.evaluate(() => {
    return {
      width: window.innerWidth,
      height: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio,
      orientation: screen.orientation ? {
        angle: screen.orientation.angle,
        type: screen.orientation.type
      } : null,
      colorDepth: screen.colorDepth,
      pixelDepth: screen.pixelDepth,
    };
  });
}

/**
 * Test media format support
 */
export async function testMediaSupport(page: Page): Promise<{
  images: { [format: string]: boolean };
  video: { [format: string]: string };
  audio: { [format: string]: string };
}> {
  return await page.evaluate(() => {
    // Image format testing
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 1;

    const images = {
      webp: canvas.toDataURL('image/webp').indexOf('webp') !== -1,
      avif: canvas.toDataURL('image/avif').indexOf('avif') !== -1,
      jpeg: true, // Always supported
      png: true,  // Always supported
      gif: true,  // Always supported
      svg: true   // Always supported
    };

    // Video format testing
    const video = document.createElement('video');
    const videoFormats = {
      mp4: video.canPlayType('video/mp4'),
      webm: video.canPlayType('video/webm'),
      ogg: video.canPlayType('video/ogg'),
      mov: video.canPlayType('video/quicktime'),
      avi: video.canPlayType('video/x-msvideo')
    };

    // Audio format testing
    const audio = document.createElement('audio');
    const audioFormats = {
      mp3: audio.canPlayType('audio/mpeg'),
      ogg: audio.canPlayType('audio/ogg'),
      wav: audio.canPlayType('audio/wav'),
      aac: audio.canPlayType('audio/aac'),
      flac: audio.canPlayType('audio/flac')
    };

    return {
      images,
      video: videoFormats,
      audio: audioFormats
    };
  });
}

/**
 * Generate compatibility report
 */
export function generateCompatibilityReport(
    browserInfo: BrowserInfo,
    features: FeatureSupport,
    performanceMetrics: BrowserPerformanceMetrics | null,
    mediaSupport: MediaSupport
): CompatibilityReport {
  return {
    browser: browserInfo,
    features,
    performance: performanceMetrics,
    media: mediaSupport,
    timestamp: new Date().toISOString(),
    userAgent: '' // Will be filled from browser context
  };
}

/**
 * Browser-specific test utilities
 */
export const BrowserSpecificUtils = {
  async enableDevTools(page: Page) {
    // Enable development tools if available
    await page.evaluate(() => {
      if (window.console && typeof window.console.clear === 'function') {
        window.console.clear();
      }
    });
  },

  async disableCaching(context: BrowserContext) {
    // Disable caching for more accurate testing
    await context.route('**/*', (route) => {
      route.continue({
        headers: {
          ...route.request().headers(),
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
    });
  },

  async simulateSlowNetwork(context: BrowserContext) {
    // Simulate slow network conditions
    await context.route('**/*', (route) => {
      setTimeout(() => route.continue(), 200 + Math.random() * 300);
    });
  },

  async mockBrowserFeatures(page: Page, features: { [key: string]: boolean }) {
    // Mock browser features for testing fallbacks
    await page.addInitScript((featuresObj) => {
      Object.entries(featuresObj).forEach(([feature, supported]) => {
        switch (feature) {
          case 'websocket':
            if (!supported) {
              delete window.WebSocket;
            }
            break;
          case 'indexeddb':
            if (!supported) {
              delete window.indexedDB;
            }
            break;
            // Add more feature mocking as needed
        }
      });
    }, features);
  }
};