/**
 * Cross-browser testing utilities and browser detection helpers
 */

import { Page, Browser, BrowserContext, expect, TestInfo } from '@playwright/test';
import { TIMEOUTS } from './test-data';

export interface BrowserInfo {
  name: string;
  version: string;
  userAgent: string;
  isMobile: boolean;
  isWebkit: boolean;
  isFirefox: boolean;
  isChromium: boolean;
  supportsWebP: boolean;
  supportsServiceWorker: boolean;
  supportsWebRTC: boolean;
  supportsIndexedDB: boolean;
  supportsLocalStorage: boolean;
  supportsNotifications: boolean;
  supportsClipboard: boolean;
}

export interface FeatureSupport {
  css: {
    grid: boolean;
    flexbox: boolean;
    customProperties: boolean;
    backdrop: boolean;
    webpSupport: boolean;
    avifSupport: boolean;
  };
  javascript: {
    es6Modules: boolean;
    asyncAwait: boolean;
    webComponents: boolean;
    intersectionObserver: boolean;
    resizeObserver: boolean;
  };
  media: {
    webRTC: boolean;
    mediaRecorder: boolean;
    audioContext: boolean;
    videoConstraints: boolean;
  };
  storage: {
    localStorage: boolean;
    sessionStorage: boolean;
    indexedDB: boolean;
    webSQL: boolean;
  };
  apis: {
    geolocation: boolean;
    notifications: boolean;
    clipboard: boolean;
    fullscreen: boolean;
    serviceWorker: boolean;
    webWorkers: boolean;
  };
}

/**
 * Browser-specific configurations and workarounds
 */
export const BROWSER_CONFIGS = {
  chromium: {
    name: 'Chromium',
    defaultTimeout: TIMEOUTS.MEDIUM,
    polyfillsNeeded: [],
    quirks: ['aggressive-caching'],
    preferredFormats: ['webp', 'avif'],
  },
  firefox: {
    name: 'Firefox',
    defaultTimeout: TIMEOUTS.LONG, // Firefox can be slower
    polyfillsNeeded: ['css-backdrop-filter'],
    quirks: ['strict-content-security-policy', 'different-font-rendering'],
    preferredFormats: ['webp'],
  },
  webkit: {
    name: 'WebKit/Safari',
    defaultTimeout: TIMEOUTS.LONG,
    polyfillsNeeded: ['css-backdrop-filter', 'intersection-observer'],
    quirks: ['strict-cors', 'different-date-picker', 'video-autoplay-restrictions'],
    preferredFormats: ['jpeg', 'png'],
  },
  'Mobile Chrome': {
    name: 'Mobile Chrome',
    defaultTimeout: TIMEOUTS.LONG,
    polyfillsNeeded: [],
    quirks: ['touch-events', 'viewport-meta', 'mobile-video-constraints'],
    preferredFormats: ['webp'],
  },
  'Mobile Safari': {
    name: 'Mobile Safari',
    defaultTimeout: TIMEOUTS.LONG,
    polyfillsNeeded: ['intersection-observer'],
    quirks: ['ios-keyboard-behavior', 'video-inline-playback', 'strict-autoplay'],
    preferredFormats: ['jpeg', 'png'],
  },
} as const;

/**
 * Get browser information from the page context
 */
export async function getBrowserInfo(page: Page): Promise<BrowserInfo> {
  const userAgent = await page.evaluate(() => navigator.userAgent);
  const browserName = await page.context().browser()?.browserType().name() || 'unknown';
  const version = await page.evaluate(() => navigator.appVersion);
  
  const features = await page.evaluate(() => {
    return {
      isMobile: /Mobi|Android/i.test(navigator.userAgent),
      supportsWebP: () => {
        const canvas = document.createElement('canvas');
        canvas.width = 1;
        canvas.height = 1;
        return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
      },
      supportsServiceWorker: 'serviceWorker' in navigator,
      supportsWebRTC: 'RTCPeerConnection' in window,
      supportsIndexedDB: 'indexedDB' in window,
      supportsLocalStorage: 'localStorage' in window && window.localStorage !== null,
      supportsNotifications: 'Notification' in window,
      supportsClipboard: 'clipboard' in navigator,
    };
  });

  return {
    name: browserName,
    version,
    userAgent,
    isMobile: features.isMobile,
    isWebkit: browserName === 'webkit',
    isFirefox: browserName === 'firefox',
    isChromium: browserName === 'chromium',
    supportsWebP: await page.evaluate(features.supportsWebP),
    supportsServiceWorker: features.supportsServiceWorker,
    supportsWebRTC: features.supportsWebRTC,
    supportsIndexedDB: features.supportsIndexedDB,
    supportsLocalStorage: features.supportsLocalStorage,
    supportsNotifications: features.supportsNotifications,
    supportsClipboard: features.supportsClipboard,
  };
}

/**
 * Detect comprehensive feature support for the current browser
 */
export async function detectFeatureSupport(page: Page): Promise<FeatureSupport> {
  return await page.evaluate(() => {
    const testElement = document.createElement('div');
    document.body.appendChild(testElement);

    const support: FeatureSupport = {
      css: {
        grid: CSS.supports('display', 'grid'),
        flexbox: CSS.supports('display', 'flex'),
        customProperties: CSS.supports('--custom', 'property'),
        backdrop: CSS.supports('backdrop-filter', 'blur(10px)'),
        webpSupport: (() => {
          const canvas = document.createElement('canvas');
          return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
        })(),
        avifSupport: (() => {
          const canvas = document.createElement('canvas');
          return canvas.toDataURL('image/avif').indexOf('data:image/avif') === 0;
        })(),
      },
      javascript: {
        es6Modules: 'noModule' in document.createElement('script'),
        asyncAwait: (function() {
          try {
            return !!Function('return (async () => {})();')();
          } catch (e) {
            return false;
          }
        })(),
        webComponents: 'customElements' in window,
        intersectionObserver: 'IntersectionObserver' in window,
        resizeObserver: 'ResizeObserver' in window,
      },
      media: {
        webRTC: 'RTCPeerConnection' in window,
        mediaRecorder: 'MediaRecorder' in window,
        audioContext: 'AudioContext' in window || 'webkitAudioContext' in window,
        videoConstraints: 'mediaDevices' in navigator && 'getUserMedia' in navigator.mediaDevices,
      },
      storage: {
        localStorage: (() => {
          try {
            return 'localStorage' in window && window.localStorage !== null;
          } catch (e) {
            return false;
          }
        })(),
        sessionStorage: (() => {
          try {
            return 'sessionStorage' in window && window.sessionStorage !== null;
          } catch (e) {
            return false;
          }
        })(),
        indexedDB: 'indexedDB' in window,
        webSQL: 'openDatabase' in window,
      },
      apis: {
        geolocation: 'geolocation' in navigator,
        notifications: 'Notification' in window,
        clipboard: 'clipboard' in navigator,
        fullscreen: 'requestFullscreen' in testElement || 'webkitRequestFullscreen' in testElement,
        serviceWorker: 'serviceWorker' in navigator,
        webWorkers: 'Worker' in window,
      },
    };

    document.body.removeChild(testElement);
    return support;
  });
}

/**
 * Wait for fonts to load to prevent layout shifts during testing
 */
export async function waitForFontsLoaded(page: Page, timeout: number = TIMEOUTS.MEDIUM): Promise<void> {
  try {
    await page.waitForFunction(
      () => {
        if ('fonts' in document) {
          return document.fonts.ready;
        }
        return true; // Fallback for browsers without font loading API
      },
      { timeout }
    );
  } catch (error) {
    console.warn('Font loading detection failed, proceeding anyway:', error);
  }
}

/**
 * Check if an image format is supported
 */
export async function checkImageFormatSupport(page: Page, format: 'webp' | 'avif' | 'jpeg' | 'png'): Promise<boolean> {
  return await page.evaluate((fmt) => {
    const canvas = document.createElement('canvas');
    canvas.width = 1;
    canvas.height = 1;
    const dataUrl = canvas.toDataURL(`image/${fmt}`);
    return dataUrl.indexOf(`data:image/${fmt}`) === 0;
  }, format);
}

/**
 * Test CSS feature support
 */
export async function testCSSSupport(page: Page, property: string, value: string): Promise<boolean> {
  return await page.evaluate((prop, val) => {
    if (typeof CSS !== 'undefined' && CSS.supports) {
      return CSS.supports(prop, val);
    }
    
    // Fallback for older browsers
    const element = document.createElement('div');
    element.style.cssText = `${prop}: ${val}`;
    return element.style.getPropertyValue(prop) === val;
  }, property, value);
}

/**
 * Test JavaScript API availability
 */
export async function testAPISupport(page: Page, apiPath: string): Promise<boolean> {
  return await page.evaluate((path) => {
    const parts = path.split('.');
    let obj: Record<string, unknown> = window as Record<string, unknown>;
    
    for (const part of parts) {
      if (!(part in obj)) {
        return false;
      }
      const nextObj = obj[part];
      if (typeof nextObj === 'object' && nextObj !== null) {
        obj = nextObj as Record<string, unknown>;
      } else if (parts.indexOf(part) === parts.length - 1) {
        // Last part can be a non-object (function, primitive)
        return true;
      } else {
        return false;
      }
    }
    
    return true;
  }, apiPath);
}

/**
 * Simulate network conditions for testing
 */
export async function simulateNetworkConditions(
  page: Page,
  condition: 'fast-3g' | 'slow-3g' | 'offline'
): Promise<void> {
  const conditions = {
    'fast-3g': { downloadThroughput: 1.5 * 1024 * 1024, uploadThroughput: 750 * 1024, latency: 40 },
    'slow-3g': { downloadThroughput: 500 * 1024, uploadThroughput: 500 * 1024, latency: 400 },
    'offline': { offline: true },
  };

  if (condition === 'offline') {
    await page.context().setOffline(true);
  } else {
    const { downloadThroughput, uploadThroughput, latency } = conditions[condition];
    // Note: This is a Chromium-specific feature
    try {
      const client = await page.context().newCDPSession(page);
      await client.send('Network.emulateNetworkConditions', {
        offline: false,
        downloadThroughput,
        uploadThroughput,
        latency,
      });
    } catch (error) {
      console.warn('Network emulation not supported in this browser:', error);
    }
  }
}

/**
 * Reset network conditions
 */
export async function resetNetworkConditions(page: Page): Promise<void> {
  await page.context().setOffline(false);
  try {
    const client = await page.context().newCDPSession(page);
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: -1,
      uploadThroughput: -1,
      latency: 0,
    });
  } catch (error) {
    // Network emulation not supported, ignore
  }
}

/**
 * Take a screenshot with browser-specific naming
 */
export async function takeScreenshotWithBrowserInfo(
  page: Page,
  testInfo: TestInfo,
  screenshotName: string
): Promise<string> {
  const browserInfo = await getBrowserInfo(page);
  const filename = `${screenshotName}-${browserInfo.name.toLowerCase()}-${testInfo.retry}.png`;
  
  await page.screenshot({
    path: `test-results/${testInfo.title}/${filename}`,
    fullPage: true,
  });
  
  return filename;
}

/**
 * Wait for animations to complete
 */
export async function waitForAnimationsComplete(page: Page): Promise<void> {
  await page.waitForFunction(() => {
    // Check if CSS animations are running
    const animatingElements = document.querySelectorAll('*');
    for (const element of animatingElements) {
      const computedStyle = window.getComputedStyle(element);
      if (
        computedStyle.animationName !== 'none' ||
        computedStyle.transitionProperty !== 'none'
      ) {
        return false;
      }
    }
    return true;
  }, { timeout: TIMEOUTS.MEDIUM });
}

/**
 * Browser-specific workarounds
 */
export class BrowserWorkarounds {
  private page: Page;
  private browserInfo: BrowserInfo;

  constructor(page: Page, browserInfo: BrowserInfo) {
    this.page = page;
    this.browserInfo = browserInfo;
  }

  /**
   * Handle file upload with browser-specific considerations
   */
  async handleFileUpload(selector: string, filePath: string): Promise<void> {
    const fileInput = this.page.locator(selector);
    
    if (this.browserInfo.isWebkit) {
      // WebKit sometimes needs explicit interaction
      await fileInput.click();
      await this.page.waitForTimeout(500);
    }
    
    await fileInput.setInputFiles(filePath);
    
    if (this.browserInfo.isFirefox) {
      // Firefox might need additional time to process file
      await this.page.waitForTimeout(1000);
    }
  }

  /**
   * Handle date picker with browser-specific behavior
   */
  async handleDatePicker(selector: string, date: string): Promise<void> {
    const dateInput = this.page.locator(selector);
    
    if (this.browserInfo.isWebkit || this.browserInfo.isMobile) {
      // Safari and mobile browsers often have different date picker behavior
      await dateInput.click();
      await dateInput.fill(date);
      await dateInput.press('Enter');
    } else {
      await dateInput.fill(date);
    }
  }

  /**
   * Handle fullscreen requests with browser-specific APIs
   */
  async requestFullscreen(selector: string): Promise<void> {
    await this.page.locator(selector).evaluate((element) => {
      interface ExtendedElement extends Element {
        webkitRequestFullscreen?: () => Promise<void>;
        mozRequestFullScreen?: () => Promise<void>;
      }
      
      const extendedElement = element as ExtendedElement;
      
      if (element.requestFullscreen) {
        element.requestFullscreen();
      } else if (extendedElement.webkitRequestFullscreen) {
        extendedElement.webkitRequestFullscreen();
      } else if (extendedElement.mozRequestFullScreen) {
        extendedElement.mozRequestFullScreen();
      }
    });
  }

  /**
   * Handle clipboard operations with fallbacks
   */
  async copyToClipboard(text: string): Promise<void> {
    if (this.browserInfo.supportsClipboard) {
      await this.page.evaluate(async (textToCopy) => {
        await navigator.clipboard.writeText(textToCopy);
      }, text);
    } else {
      // Fallback for browsers without clipboard API
      await this.page.evaluate((textToCopy) => {
        const textArea = document.createElement('textarea');
        textArea.value = textToCopy;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
      }, text);
    }
  }

  /**
   * Handle video autoplay with browser-specific policies
   */
  async enableVideoAutoplay(selector: string): Promise<void> {
    const video = this.page.locator(selector);
    
    if (this.browserInfo.isWebkit || this.browserInfo.isMobile) {
      // Safari and mobile browsers require user interaction for autoplay
      await video.click();
    }
    
    await video.evaluate((videoElement) => {
      (videoElement as HTMLVideoElement).muted = true; // Muted videos can autoplay
      (videoElement as HTMLVideoElement).play();
    });
  }
}

/**
 * Get browser-specific timeout multipliers
 */
export function getBrowserTimeoutMultiplier(browserName: string): number {
  const multipliers: Record<string, number> = {
    'webkit': 1.5, // Safari can be slower
    'firefox': 1.2, // Firefox sometimes needs more time
    'chromium': 1.0, // Baseline
    'Mobile Chrome': 1.3, // Mobile can be slower
    'Mobile Safari': 1.5, // Mobile Safari can be quite slow
  };
  
  return multipliers[browserName] || 1.0;
}

/**
 * Create browser-specific test context
 */
export async function createBrowserSpecificContext(
  browser: Browser,
  browserName: string
): Promise<BrowserContext> {
  const baseOptions = {
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
  };

  if (browserName === 'webkit') {
    return await browser.newContext({
      ...baseOptions,
      // Safari-specific options
      permissions: ['microphone', 'camera'], // Pre-grant permissions
    });
  } else if (browserName === 'firefox') {
    return await browser.newContext({
      ...baseOptions,
      // Firefox-specific options
      firefoxUserPrefs: {
        'media.navigator.streams.fake': true, // Allow fake media streams
        'media.navigator.permission.disabled': true,
      },
    });
  } else {
    return await browser.newContext(baseOptions);
  }
}