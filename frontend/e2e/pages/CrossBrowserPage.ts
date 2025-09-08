/**
 * Page Object Model for Cross-Browser Testing
 */

import { Page, expect, Locator, TestInfo } from '@playwright/test';
import { SELECTORS, TIMEOUTS, TEST_URLS } from '../helpers/test-data';
import { 
  getBrowserInfo, 
  detectFeatureSupport, 
  BrowserWorkarounds,
  waitForFontsLoaded,
  waitForAnimationsComplete,
  type BrowserInfo,
  type FeatureSupport,
} from '../helpers/cross-browser.helper';

export class CrossBrowserPage {
  readonly page: Page;
  readonly testInfo: TestInfo;
  private browserInfo: BrowserInfo | null = null;
  private featureSupport: FeatureSupport | null = null;
  private workarounds: BrowserWorkarounds | null = null;

  // Common UI elements that behave differently across browsers
  readonly fileInput: Locator;
  readonly dateInput: Locator;
  readonly videoElement: Locator;
  readonly audioElement: Locator;
  readonly canvasElement: Locator;
  readonly notificationButton: Locator;
  readonly fullscreenButton: Locator;
  readonly clipboardButton: Locator;
  readonly geolocationButton: Locator;
  readonly cameraButton: Locator;
  readonly microphoneButton: Locator;
  readonly downloadButton: Locator;
  readonly uploadButton: Locator;

  // Form elements for cross-browser testing
  readonly textInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly numberInput: Locator;
  readonly rangeInput: Locator;
  readonly colorInput: Locator;
  readonly searchInput: Locator;
  readonly urlInput: Locator;
  readonly telInput: Locator;
  readonly textareaInput: Locator;
  readonly selectInput: Locator;
  readonly checkboxInput: Locator;
  readonly radioInput: Locator;

  // Layout and styling elements
  readonly flexContainer: Locator;
  readonly gridContainer: Locator;
  readonly animatedElement: Locator;
  readonly customPropertyElement: Locator;
  readonly backdropElement: Locator;

  constructor(page: Page, testInfo: TestInfo) {
    this.page = page;
    this.testInfo = testInfo;

    // Initialize locators for cross-browser testing
    this.fileInput = page.locator('input[type="file"]');
    this.dateInput = page.locator('input[type="date"]');
    this.videoElement = page.locator('video');
    this.audioElement = page.locator('audio');
    this.canvasElement = page.locator('canvas');
    this.notificationButton = page.locator('[data-testid="notification-button"]');
    this.fullscreenButton = page.locator('[data-testid="fullscreen-button"]');
    this.clipboardButton = page.locator('[data-testid="clipboard-button"]');
    this.geolocationButton = page.locator('[data-testid="geolocation-button"]');
    this.cameraButton = page.locator('[data-testid="camera-button"]');
    this.microphoneButton = page.locator('[data-testid="microphone-button"]');
    this.downloadButton = page.locator('[data-testid="download-button"]');
    this.uploadButton = page.locator('[data-testid="upload-button"]');

    // Form elements
    this.textInput = page.locator('input[type="text"]').first();
    this.emailInput = page.locator('input[type="email"]').first();
    this.passwordInput = page.locator('input[type="password"]').first();
    this.numberInput = page.locator('input[type="number"]').first();
    this.rangeInput = page.locator('input[type="range"]').first();
    this.colorInput = page.locator('input[type="color"]').first();
    this.searchInput = page.locator('input[type="search"]').first();
    this.urlInput = page.locator('input[type="url"]').first();
    this.telInput = page.locator('input[type="tel"]').first();
    this.textareaInput = page.locator('textarea').first();
    this.selectInput = page.locator('select').first();
    this.checkboxInput = page.locator('input[type="checkbox"]').first();
    this.radioInput = page.locator('input[type="radio"]').first();

    // Layout elements
    this.flexContainer = page.locator('.flex-container, [style*="display: flex"]');
    this.gridContainer = page.locator('.grid-container, [style*="display: grid"]');
    this.animatedElement = page.locator('.animated, [style*="animation"]');
    this.customPropertyElement = page.locator('[style*="--"]');
    this.backdropElement = page.locator('.backdrop-filter, [style*="backdrop-filter"]');
  }

  /**
   * Initialize browser-specific information and workarounds
   */
  async initialize(): Promise<void> {
    this.browserInfo = await getBrowserInfo(this.page);
    this.featureSupport = await detectFeatureSupport(this.page);
    this.workarounds = new BrowserWorkarounds(this.page, this.browserInfo);
    
    // Wait for fonts to load to prevent layout shifts
    await waitForFontsLoaded(this.page);
  }

  /**
   * Get browser information
   */
  getBrowserInfo(): BrowserInfo {
    if (!this.browserInfo) {
      throw new Error('Browser info not initialized. Call initialize() first.');
    }
    return this.browserInfo;
  }

  /**
   * Get feature support information
   */
  getFeatureSupport(): FeatureSupport {
    if (!this.featureSupport) {
      throw new Error('Feature support not initialized. Call initialize() first.');
    }
    return this.featureSupport;
  }

  /**
   * Navigate to a test page with browser-specific handling
   */
  async gotoTestPage(path: string = '/'): Promise<void> {
    await this.page.goto(path);
    await this.page.waitForLoadState('networkidle');
    await waitForFontsLoaded(this.page);
    
    // Browser-specific initialization
    if (this.browserInfo?.isWebkit) {
      // Safari might need extra time for certain features
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * Test WebSocket connections across browsers
   */
  async testWebSocketConnection(url: string): Promise<boolean> {
    return await this.page.evaluate(async (wsUrl) => {
      return new Promise<boolean>((resolve) => {
        try {
          const ws = new WebSocket(wsUrl);
          
          ws.onopen = () => {
            ws.close();
            resolve(true);
          };
          
          ws.onerror = () => {
            resolve(false);
          };
          
          // Timeout after 5 seconds
          setTimeout(() => {
            ws.close();
            resolve(false);
          }, 5000);
        } catch (error) {
          resolve(false);
        }
      });
    }, url);
  }

  /**
   * Test WebRTC functionality across browsers
   */
  async testWebRTCSupport(): Promise<{
    supported: boolean;
    getUserMedia: boolean;
    peerConnection: boolean;
  }> {
    return await this.page.evaluate(async () => {
      const result = {
        supported: false,
        getUserMedia: false,
        peerConnection: false,
      };

      // Test getUserMedia
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        try {
          const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
          result.getUserMedia = true;
          stream.getTracks().forEach(track => track.stop());
        } catch {
          result.getUserMedia = false;
        }
      }

      // Test RTCPeerConnection
      if ('RTCPeerConnection' in window) {
        try {
          const pc = new RTCPeerConnection();
          result.peerConnection = true;
          pc.close();
        } catch {
          result.peerConnection = false;
        }
      }

      result.supported = result.getUserMedia && result.peerConnection;
      return result;
    });
  }

  /**
   * Test local storage functionality across browsers
   */
  async testLocalStorageSupport(): Promise<{
    localStorage: boolean;
    sessionStorage: boolean;
    indexedDB: boolean;
  }> {
    return await this.page.evaluate(async () => {
      const result = {
        localStorage: false,
        sessionStorage: false,
        indexedDB: false,
      };

      // Test localStorage
      try {
        localStorage.setItem('test', 'value');
        result.localStorage = localStorage.getItem('test') === 'value';
        localStorage.removeItem('test');
      } catch {
        result.localStorage = false;
      }

      // Test sessionStorage
      try {
        sessionStorage.setItem('test', 'value');
        result.sessionStorage = sessionStorage.getItem('test') === 'value';
        sessionStorage.removeItem('test');
      } catch {
        result.sessionStorage = false;
      }

      // Test IndexedDB
      if ('indexedDB' in window) {
        try {
          const request = indexedDB.open('test-db', 1);
          await new Promise<void>((resolve, reject) => {
            request.onsuccess = () => {
              result.indexedDB = true;
              request.result.close();
              resolve();
            };
            request.onerror = () => reject();
            setTimeout(() => reject(), 2000); // Timeout
          });
        } catch {
          result.indexedDB = false;
        }
      }

      return result;
    });
  }

  /**
   * Test CSS Grid layout support and behavior
   */
  async testCSSGridSupport(): Promise<boolean> {
    return await this.page.evaluate(() => {
      const testElement = document.createElement('div');
      testElement.style.display = 'grid';
      document.body.appendChild(testElement);
      
      const computedStyle = window.getComputedStyle(testElement);
      const isSupported = computedStyle.display === 'grid';
      
      document.body.removeChild(testElement);
      return isSupported;
    });
  }

  /**
   * Test Flexbox layout support and behavior
   */
  async testFlexboxSupport(): Promise<boolean> {
    return await this.page.evaluate(() => {
      const testElement = document.createElement('div');
      testElement.style.display = 'flex';
      document.body.appendChild(testElement);
      
      const computedStyle = window.getComputedStyle(testElement);
      const isSupported = computedStyle.display === 'flex';
      
      document.body.removeChild(testElement);
      return isSupported;
    });
  }

  /**
   * Test CSS custom properties (variables) support
   */
  async testCSSCustomProperties(): Promise<boolean> {
    return await this.page.evaluate(() => {
      const testElement = document.createElement('div');
      testElement.style.setProperty('--test-var', 'red');
      testElement.style.color = 'var(--test-var)';
      document.body.appendChild(testElement);
      
      const computedStyle = window.getComputedStyle(testElement);
      const isSupported = computedStyle.color === 'red' || computedStyle.color === 'rgb(255, 0, 0)';
      
      document.body.removeChild(testElement);
      return isSupported;
    });
  }

  /**
   * Test image format support
   */
  async testImageFormatSupport(): Promise<{
    webp: boolean;
    avif: boolean;
    jpeg: boolean;
    png: boolean;
  }> {
    return await this.page.evaluate(async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 1;
      canvas.height = 1;
      
      return {
        webp: canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0,
        avif: canvas.toDataURL('image/avif').indexOf('data:image/avif') === 0,
        jpeg: canvas.toDataURL('image/jpeg').indexOf('data:image/jpeg') === 0,
        png: canvas.toDataURL('image/png').indexOf('data:image/png') === 0,
      };
    });
  }

  /**
   * Test form input types and validation
   */
  async testFormInputSupport(): Promise<Record<string, boolean>> {
    return await this.page.evaluate(() => {
      const inputTypes = [
        'email', 'number', 'range', 'date', 'datetime-local', 
        'time', 'color', 'search', 'url', 'tel'
      ];
      
      const support: Record<string, boolean> = {};
      
      inputTypes.forEach(type => {
        const input = document.createElement('input');
        input.type = type;
        support[type] = input.type === type;
      });
      
      return support;
    });
  }

  /**
   * Test clipboard API functionality
   */
  async testClipboardSupport(): Promise<{
    supported: boolean;
    writeText: boolean;
    readText: boolean;
  }> {
    if (!this.workarounds) {
      throw new Error('Workarounds not initialized. Call initialize() first.');
    }

    return await this.page.evaluate(async () => {
      const result = {
        supported: 'clipboard' in navigator,
        writeText: false,
        readText: false,
      };

      if (result.supported && navigator.clipboard) {
        try {
          await navigator.clipboard.writeText('test');
          result.writeText = true;
          
          const text = await navigator.clipboard.readText();
          result.readText = text === 'test';
        } catch {
          // Clipboard access might be restricted
          result.writeText = false;
          result.readText = false;
        }
      }

      return result;
    });
  }

  /**
   * Test geolocation API support
   */
  async testGeolocationSupport(): Promise<boolean> {
    return await this.page.evaluate(async () => {
      if (!('geolocation' in navigator)) {
        return false;
      }

      return new Promise<boolean>((resolve) => {
        navigator.geolocation.getCurrentPosition(
          () => resolve(true),
          () => resolve(false),
          { timeout: 5000 }
        );
      });
    });
  }

  /**
   * Test notification API support
   */
  async testNotificationSupport(): Promise<{
    supported: boolean;
    permission: string;
  }> {
    return await this.page.evaluate(async () => {
      const result = {
        supported: 'Notification' in window,
        permission: 'default',
      };

      if (result.supported) {
        result.permission = Notification.permission;
        
        if (Notification.permission === 'default') {
          try {
            const permission = await Notification.requestPermission();
            result.permission = permission;
          } catch {
            // Permission request might fail
          }
        }
      }

      return result;
    });
  }

  /**
   * Test Service Worker support
   */
  async testServiceWorkerSupport(): Promise<boolean> {
    return await this.page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) {
        return false;
      }

      try {
        const registration = await navigator.serviceWorker.register('/test-sw.js');
        await navigator.serviceWorker.ready;
        await registration.unregister();
        return true;
      } catch {
        return false;
      }
    });
  }

  /**
   * Test font loading and rendering consistency
   */
  async testFontRendering(): Promise<{
    fontLoading: boolean;
    customFonts: boolean;
    consistency: boolean;
  }> {
    return await this.page.evaluate(async () => {
      const result = {
        fontLoading: 'fonts' in document,
        customFonts: false,
        consistency: true,
      };

      if (result.fontLoading) {
        try {
          await document.fonts.ready;
          
          // Test custom font loading
          const testFont = new FontFace('TestFont', 'url(data:font/woff2;base64,d09GMgABAAAAAAUgAAoAAAAABNgAAATPAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGhYbIBwqBmAAgTIBNgIkAzgEBgWDGwcgGykJEZWkAR7I/uuEmzfb5F6ebUUB5gPt6dGVhQKLiT2JJ19M4qKi2+vFRE6gIhqkxEzEkSxkpEqhILQIWdHq9+7FG9lGVz7Y/GvEJBNPXMCMUDAJBQOKUJCJBTSxJBLgYRIDt9dKSjUJJFRJSWKpUEKFZUtU5uT7PKKUOZnzOdKI1jGgGLFQQgWJKBLxmLF+zMBCqYfNFGpY/WBUOxDN2A)');
          await testFont.load();
          document.fonts.add(testFont);
          result.customFonts = true;
        } catch {
          result.customFonts = false;
        }
      }

      // Test font rendering consistency (simplified)
      const testDiv = document.createElement('div');
      testDiv.style.fontFamily = 'Arial, sans-serif';
      testDiv.style.fontSize = '16px';
      testDiv.textContent = 'Test Font Rendering';
      document.body.appendChild(testDiv);
      
      const rect = testDiv.getBoundingClientRect();
      result.consistency = rect.width > 0 && rect.height > 0;
      
      document.body.removeChild(testDiv);
      
      return result;
    });
  }

  /**
   * Test performance APIs availability
   */
  async testPerformanceAPIs(): Promise<{
    navigation: boolean;
    resource: boolean;
    measure: boolean;
    observer: boolean;
  }> {
    return await this.page.evaluate(() => {
      return {
        navigation: 'performance' in window && 'navigation' in performance,
        resource: 'performance' in window && 'getEntriesByType' in performance,
        measure: 'performance' in window && 'measure' in performance,
        observer: 'PerformanceObserver' in window,
      };
    });
  }

  /**
   * Wait for all animations to complete
   */
  async waitForAllAnimationsComplete(): Promise<void> {
    await waitForAnimationsComplete(this.page);
  }

  /**
   * Take a cross-browser compatible screenshot
   */
  async takeCompatibleScreenshot(name: string): Promise<void> {
    // Wait for animations and fonts
    await this.waitForAllAnimationsComplete();
    await waitForFontsLoaded(this.page);
    
    const browserName = this.getBrowserInfo().name.toLowerCase().replace(/\s+/g, '-');
    const filename = `${name}-${browserName}.png`;
    
    await this.page.screenshot({
      path: `test-results/${this.testInfo.title}/${filename}`,
      fullPage: true,
    });
  }

  /**
   * Cleanup after tests
   */
  async cleanup(): Promise<void> {
    // Clear any test data from storage
    await this.page.evaluate(() => {
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch {
        // Storage might not be available
      }
    });
    
    // Reset any modified page state
    await this.page.evaluate(() => {
      // Remove any test elements that might have been added
      const testElements = document.querySelectorAll('[data-test-element]');
      testElements.forEach(el => el.remove());
      
      // Reset any modified styles
      document.body.style.cssText = '';
      document.documentElement.style.cssText = '';
    });
  }
}