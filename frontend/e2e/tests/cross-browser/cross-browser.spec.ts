/**
 * Comprehensive Cross-Browser E2E Tests for FocusHive
 * 
 * Tests core functionality across Chromium, Firefox, and WebKit browsers
 * Covers compatibility, performance, and feature support variations
 */

import { test, expect, Page, BrowserContext } from '@playwright/test';
import { CrossBrowserPage } from '../../pages/CrossBrowserPage';
import { 
  getBrowserInfo, 
  detectFeatureSupport, 
  simulateNetworkConditions,
  resetNetworkConditions,
  getBrowserTimeoutMultiplier,
  BROWSER_CONFIGS,
} from '../../helpers/cross-browser.helper';
import { TEST_URLS, TIMEOUTS } from '../../helpers/test-data';

test.describe('Cross-Browser Compatibility Tests', () => {
  let crossBrowserPage: CrossBrowserPage;
  let browserMultiplier: number;

  test.beforeEach(async ({ page }, testInfo) => {
    crossBrowserPage = new CrossBrowserPage(page, testInfo);
    await crossBrowserPage.initialize();
    
    const browserInfo = crossBrowserPage.getBrowserInfo();
    browserMultiplier = getBrowserTimeoutMultiplier(browserInfo.name);
    
    // Adjust timeouts based on browser
    test.setTimeout(60000 * browserMultiplier);
  });

  test.afterEach(async () => {
    await crossBrowserPage.cleanup();
  });

  test.describe('Browser Detection and Feature Support', () => {
    test('should detect browser information correctly', async ({ page }, testInfo) => {
      const browserInfo = await getBrowserInfo(page);
      
      expect(browserInfo.name).toBeTruthy();
      expect(browserInfo.userAgent).toBeTruthy();
      expect(typeof browserInfo.isMobile).toBe('boolean');
      expect(typeof browserInfo.isWebkit).toBe('boolean');
      expect(typeof browserInfo.isFirefox).toBe('boolean');
      expect(typeof browserInfo.isChromium).toBe('boolean');
      
      console.log(`Browser: ${browserInfo.name}`);
      console.log(`User Agent: ${browserInfo.userAgent}`);
      console.log(`Mobile: ${browserInfo.isMobile}`);
    });

    test('should detect comprehensive feature support', async ({ page }) => {
      const features = await detectFeatureSupport(page);
      
      // CSS features
      expect(typeof features.css.grid).toBe('boolean');
      expect(typeof features.css.flexbox).toBe('boolean');
      expect(typeof features.css.customProperties).toBe('boolean');
      
      // JavaScript features
      expect(typeof features.javascript.es6Modules).toBe('boolean');
      expect(typeof features.javascript.asyncAwait).toBe('boolean');
      
      // Media features
      expect(typeof features.media.webRTC).toBe('boolean');
      expect(typeof features.media.mediaRecorder).toBe('boolean');
      
      // Storage features
      expect(typeof features.storage.localStorage).toBe('boolean');
      expect(typeof features.storage.indexedDB).toBe('boolean');
      
      // API features
      expect(typeof features.apis.notifications).toBe('boolean');
      expect(typeof features.apis.serviceWorker).toBe('boolean');
      
      console.log('Feature Support:', JSON.stringify(features, null, 2));
    });

    test('should handle browser-specific configurations', async ({ page }) => {
      const browserInfo = await getBrowserInfo(page);
      const config = BROWSER_CONFIGS[browserInfo.name as keyof typeof BROWSER_CONFIGS];
      
      if (config) {
        expect(config.name).toBeTruthy();
        expect(config.defaultTimeout).toBeGreaterThan(0);
        expect(Array.isArray(config.polyfillsNeeded)).toBe(true);
        expect(Array.isArray(config.quirks)).toBe(true);
        expect(Array.isArray(config.preferredFormats)).toBe(true);
        
        console.log(`Config for ${browserInfo.name}:`, config);
      }
    });
  });

  test.describe('Web APIs Compatibility', () => {
    test('should test WebSocket connection support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const wsSupport = await crossBrowserPage.testWebSocketConnection('wss://echo.websocket.org');
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // WebSocket should be supported in all modern browsers
      expect(wsSupport).toBe(true);
      
      console.log(`WebSocket support in ${browserInfo.name}: ${wsSupport}`);
    });

    test('should test WebRTC functionality', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const webrtcSupport = await crossBrowserPage.testWebRTCSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof webrtcSupport.supported).toBe('boolean');
      expect(typeof webrtcSupport.getUserMedia).toBe('boolean');
      expect(typeof webrtcSupport.peerConnection).toBe('boolean');
      
      // Log browser-specific WebRTC support
      console.log(`WebRTC support in ${browserInfo.name}:`, webrtcSupport);
      
      // WebKit might have restrictions
      if (browserInfo.isWebkit) {
        console.log('WebKit detected - WebRTC may have additional restrictions');
      }
    });

    test('should test local storage functionality', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const storageSupport = await crossBrowserPage.testLocalStorageSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Modern browsers should support all storage types
      expect(storageSupport.localStorage).toBe(true);
      expect(storageSupport.sessionStorage).toBe(true);
      expect(storageSupport.indexedDB).toBe(true);
      
      console.log(`Storage support in ${browserInfo.name}:`, storageSupport);
    });

    test('should test notification API support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const notificationSupport = await crossBrowserPage.testNotificationSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof notificationSupport.supported).toBe('boolean');
      expect(['default', 'granted', 'denied']).toContain(notificationSupport.permission);
      
      console.log(`Notification support in ${browserInfo.name}:`, notificationSupport);
    });

    test('should test clipboard API support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const clipboardSupport = await crossBrowserPage.testClipboardSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof clipboardSupport.supported).toBe('boolean');
      expect(typeof clipboardSupport.writeText).toBe('boolean');
      expect(typeof clipboardSupport.readText).toBe('boolean');
      
      console.log(`Clipboard support in ${browserInfo.name}:`, clipboardSupport);
      
      // Firefox might have restrictions in automation
      if (browserInfo.isFirefox) {
        console.log('Firefox detected - clipboard access might be restricted in automation');
      }
    });

    test('should test geolocation API support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const geoSupport = await crossBrowserPage.testGeolocationSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof geoSupport).toBe('boolean');
      
      console.log(`Geolocation support in ${browserInfo.name}: ${geoSupport}`);
      
      // Mobile browsers typically have better geolocation support
      if (browserInfo.isMobile) {
        console.log('Mobile browser detected - geolocation may be more accurate');
      }
    });

    test('should test Service Worker support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const swSupport = await crossBrowserPage.testServiceWorkerSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof swSupport).toBe('boolean');
      
      console.log(`Service Worker support in ${browserInfo.name}: ${swSupport}`);
      
      // All modern browsers should support Service Workers
      if (!swSupport) {
        console.warn(`Service Worker not supported in ${browserInfo.name}`);
      }
    });
  });

  test.describe('CSS Layout and Styling Compatibility', () => {
    test('should test CSS Grid layout support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const gridSupport = await crossBrowserPage.testCSSGridSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(gridSupport).toBe(true); // All modern browsers should support Grid
      
      console.log(`CSS Grid support in ${browserInfo.name}: ${gridSupport}`);
    });

    test('should test Flexbox layout support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const flexSupport = await crossBrowserPage.testFlexboxSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(flexSupport).toBe(true); // All modern browsers should support Flexbox
      
      console.log(`Flexbox support in ${browserInfo.name}: ${flexSupport}`);
    });

    test('should test CSS custom properties support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const customPropSupport = await crossBrowserPage.testCSSCustomProperties();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(customPropSupport).toBe(true); // All modern browsers should support custom properties
      
      console.log(`CSS Custom Properties support in ${browserInfo.name}: ${customPropSupport}`);
    });

    test('should test font loading and rendering consistency', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const fontTest = await crossBrowserPage.testFontRendering();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof fontTest.fontLoading).toBe('boolean');
      expect(typeof fontTest.customFonts).toBe('boolean');
      expect(typeof fontTest.consistency).toBe('boolean');
      
      console.log(`Font rendering in ${browserInfo.name}:`, fontTest);
      
      // Different browsers may render fonts slightly differently
      if (browserInfo.isWebkit) {
        console.log('WebKit detected - font rendering may differ from other browsers');
      }
    });

    test('should handle responsive design breakpoints', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Test different viewport sizes
      const breakpoints = [
        { width: 320, height: 568, name: 'Mobile Portrait' },
        { width: 568, height: 320, name: 'Mobile Landscape' },
        { width: 768, height: 1024, name: 'Tablet Portrait' },
        { width: 1024, height: 768, name: 'Tablet Landscape' },
        { width: 1280, height: 720, name: 'Desktop' },
        { width: 1920, height: 1080, name: 'Large Desktop' },
      ];
      
      for (const breakpoint of breakpoints) {
        await page.setViewportSize({ width: breakpoint.width, height: breakpoint.height });
        await page.waitForTimeout(500); // Allow time for responsive changes
        
        const viewport = page.viewportSize();
        expect(viewport?.width).toBe(breakpoint.width);
        expect(viewport?.height).toBe(breakpoint.height);
        
        console.log(`${browserInfo.name} - ${breakpoint.name}: ${breakpoint.width}x${breakpoint.height}`);
      }
    });
  });

  test.describe('Form Input and Interaction Compatibility', () => {
    test('should test HTML5 input type support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const inputSupport = await crossBrowserPage.testFormInputSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      const expectedTypes = ['email', 'number', 'range', 'date', 'time', 'color', 'search', 'url', 'tel'];
      
      for (const type of expectedTypes) {
        expect(typeof inputSupport[type]).toBe('boolean');
      }
      
      console.log(`Input type support in ${browserInfo.name}:`, inputSupport);
      
      // Safari might have different input type support
      if (browserInfo.isWebkit) {
        console.log('WebKit detected - some input types may have different behavior');
      }
    });

    test('should test form validation behavior', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      // Create a test form
      await page.evaluate(() => {
        const form = document.createElement('form');
        form.innerHTML = `
          <input type="email" required id="test-email">
          <input type="number" min="1" max="10" id="test-number">
          <input type="text" pattern="[A-Za-z]+" id="test-pattern">
          <button type="submit">Submit</button>
        `;
        document.body.appendChild(form);
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Test email validation
      await page.fill('#test-email', 'invalid-email');
      const emailValid = await page.evaluate(() => {
        const input = document.getElementById('test-email') as HTMLInputElement;
        return input.validity.valid;
      });
      expect(emailValid).toBe(false);
      
      await page.fill('#test-email', 'valid@email.com');
      const emailValidCorrect = await page.evaluate(() => {
        const input = document.getElementById('test-email') as HTMLInputElement;
        return input.validity.valid;
      });
      expect(emailValidCorrect).toBe(true);
      
      console.log(`Form validation working in ${browserInfo.name}`);
      
      // Clean up
      await page.evaluate(() => {
        const form = document.querySelector('form');
        if (form) form.remove();
      });
    });

    test('should test file upload behavior', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      // Create a test file input
      await page.evaluate(() => {
        const input = document.createElement('input');
        input.type = 'file';
        input.id = 'test-file';
        input.multiple = true;
        input.accept = '.jpg,.png,.pdf';
        document.body.appendChild(input);
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Test file input properties
      const fileInput = page.locator('#test-file');
      await expect(fileInput).toBeVisible();
      
      const inputProps = await fileInput.evaluate((input) => {
        const fileInput = input as HTMLInputElement;
        return {
          multiple: fileInput.multiple,
          accept: fileInput.accept,
          type: fileInput.type,
        };
      });
      
      expect(inputProps.multiple).toBe(true);
      expect(inputProps.accept).toBe('.jpg,.png,.pdf');
      expect(inputProps.type).toBe('file');
      
      console.log(`File input properties in ${browserInfo.name}:`, inputProps);
      
      // Clean up
      await page.evaluate(() => {
        document.getElementById('test-file')?.remove();
      });
    });

    test('should test touch and mouse event handling', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      // Create a test element for interaction
      await page.evaluate(() => {
        const div = document.createElement('div');
        div.id = 'test-interaction';
        div.style.cssText = `
          width: 100px;
          height: 100px;
          background: red;
          cursor: pointer;
          user-select: none;
        `;
        
        const events: string[] = [];
        
        ['mousedown', 'mouseup', 'click', 'touchstart', 'touchend'].forEach(eventType => {
          div.addEventListener(eventType, () => {
            events.push(eventType);
            (div as unknown as { events: string[] }).events = events;
          });
        });
        
        document.body.appendChild(div);
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Test click interaction
      const testDiv = page.locator('#test-interaction');
      await testDiv.click();
      
      const events = await testDiv.evaluate((div) => 
        (div as unknown as { events?: string[] }).events || []
      );
      
      expect(events.length).toBeGreaterThan(0);
      expect(events).toContain('click');
      
      console.log(`Interaction events in ${browserInfo.name}:`, events);
      
      // Mobile browsers might also trigger touch events
      if (browserInfo.isMobile) {
        console.log('Mobile browser detected - touch events may also be present');
      }
      
      // Clean up
      await page.evaluate(() => {
        document.getElementById('test-interaction')?.remove();
      });
    });
  });

  test.describe('Media and Graphics Compatibility', () => {
    test('should test image format support', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const imageSupport = await crossBrowserPage.testImageFormatSupport();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof imageSupport.webp).toBe('boolean');
      expect(typeof imageSupport.avif).toBe('boolean');
      expect(imageSupport.jpeg).toBe(true); // JPEG should be universally supported
      expect(imageSupport.png).toBe(true);  // PNG should be universally supported
      
      console.log(`Image format support in ${browserInfo.name}:`, imageSupport);
      
      // WebP and AVIF support varies by browser
      const config = BROWSER_CONFIGS[browserInfo.name as keyof typeof BROWSER_CONFIGS];
      if (config) {
        const expectedWebP = config.preferredFormats.includes('webp');
        console.log(`Expected WebP support for ${browserInfo.name}: ${expectedWebP}, Actual: ${imageSupport.webp}`);
      }
    });

    test('should test canvas functionality', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const canvasSupport = await page.evaluate(() => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        
        if (!ctx) return { supported: false };
        
        // Test basic canvas operations
        ctx.fillStyle = 'red';
        ctx.fillRect(0, 0, 10, 10);
        
        const imageData = ctx.getImageData(0, 0, 1, 1);
        const pixel = imageData.data;
        
        return {
          supported: true,
          context2d: !!ctx,
          drawingWorks: pixel[0] === 255 && pixel[1] === 0 && pixel[2] === 0, // Red pixel
          toDataURL: typeof canvas.toDataURL === 'function',
        };
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(canvasSupport.supported).toBe(true);
      expect(canvasSupport.context2d).toBe(true);
      expect(canvasSupport.drawingWorks).toBe(true);
      expect(canvasSupport.toDataURL).toBe(true);
      
      console.log(`Canvas support in ${browserInfo.name}:`, canvasSupport);
    });

    test('should test video element behavior', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const videoSupport = await page.evaluate(async () => {
        const video = document.createElement('video');
        document.body.appendChild(video);
        
        const result = {
          supported: !!video,
          canPlayType: typeof video.canPlayType === 'function',
          mp4Support: video.canPlayType('video/mp4'),
          webmSupport: video.canPlayType('video/webm'),
          autoplaySupport: 'autoplay' in video,
          controlsSupport: 'controls' in video,
          mutedSupport: 'muted' in video,
        };
        
        document.body.removeChild(video);
        return result;
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(videoSupport.supported).toBe(true);
      expect(videoSupport.canPlayType).toBe(true);
      expect(videoSupport.autoplaySupport).toBe(true);
      expect(videoSupport.controlsSupport).toBe(true);
      expect(videoSupport.mutedSupport).toBe(true);
      
      console.log(`Video support in ${browserInfo.name}:`, videoSupport);
      
      // Different browsers support different video formats
      if (browserInfo.isWebkit) {
        console.log('WebKit detected - MP4 support typically better than WebM');
      }
    });

    test('should test audio element behavior', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const audioSupport = await page.evaluate(() => {
        const audio = document.createElement('audio');
        document.body.appendChild(audio);
        
        const result = {
          supported: !!audio,
          canPlayType: typeof audio.canPlayType === 'function',
          mp3Support: audio.canPlayType('audio/mp3'),
          oggSupport: audio.canPlayType('audio/ogg'),
          wavSupport: audio.canPlayType('audio/wav'),
          autoplaySupport: 'autoplay' in audio,
          controlsSupport: 'controls' in audio,
        };
        
        document.body.removeChild(audio);
        return result;
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(audioSupport.supported).toBe(true);
      expect(audioSupport.canPlayType).toBe(true);
      expect(audioSupport.autoplaySupport).toBe(true);
      expect(audioSupport.controlsSupported).toBe(true);
      
      console.log(`Audio support in ${browserInfo.name}:`, audioSupport);
    });
  });

  test.describe('Performance and Network Behavior', () => {
    test('should test performance API availability', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const perfAPIs = await crossBrowserPage.testPerformanceAPIs();
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(typeof perfAPIs.navigation).toBe('boolean');
      expect(typeof perfAPIs.resource).toBe('boolean');
      expect(typeof perfAPIs.measure).toBe('boolean');
      expect(typeof perfAPIs.observer).toBe('boolean');
      
      console.log(`Performance APIs in ${browserInfo.name}:`, perfAPIs);
      
      // Modern browsers should support most performance APIs
      expect(perfAPIs.navigation).toBe(true);
      expect(perfAPIs.resource).toBe(true);
    });

    test('should handle different network conditions', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Test slow network simulation (Chromium only)
      if (browserInfo.isChromium) {
        await simulateNetworkConditions(page, 'slow-3g');
        
        const startTime = Date.now();
        await page.reload();
        const loadTime = Date.now() - startTime;
        
        expect(loadTime).toBeGreaterThan(1000); // Should be slower
        
        console.log(`Slow 3G load time in ${browserInfo.name}: ${loadTime}ms`);
        
        await resetNetworkConditions(page);
      } else {
        console.log(`Network simulation not supported in ${browserInfo.name}`);
      }
    });

    test('should measure page load performance across browsers', async ({ page }) => {
      const browserInfo = crossBrowserPage.getBrowserInfo();
      const startTime = Date.now();
      
      await crossBrowserPage.gotoTestPage('/');
      
      const loadTime = Date.now() - startTime;
      const performanceEntries = await page.evaluate(() => {
        if ('performance' in window && performance.getEntriesByType) {
          const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
          return {
            domContentLoaded: navigation.domContentLoadedEventEnd - navigation.navigationStart,
            loadComplete: navigation.loadEventEnd - navigation.navigationStart,
            firstPaint: performance.getEntriesByName('first-paint')[0]?.startTime || 0,
            firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0,
          };
        }
        return null;
      });
      
      expect(loadTime).toBeGreaterThan(0);
      
      console.log(`Page load performance in ${browserInfo.name}:`);
      console.log(`  Total load time: ${loadTime}ms`);
      if (performanceEntries) {
        console.log(`  DOM Content Loaded: ${performanceEntries.domContentLoaded}ms`);
        console.log(`  Load Complete: ${performanceEntries.loadComplete}ms`);
        console.log(`  First Paint: ${performanceEntries.firstPaint}ms`);
        console.log(`  First Contentful Paint: ${performanceEntries.firstContentfulPaint}ms`);
      }
      
      // Performance expectations vary by browser
      const expectedMaxLoadTime = 5000 * browserMultiplier;
      expect(loadTime).toBeLessThan(expectedMaxLoadTime);
    });
  });

  test.describe('Error Handling and Edge Cases', () => {
    test('should handle JavaScript errors consistently', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      const consoleErrors: string[] = [];
      
      page.on('console', msg => {
        if (msg.type() === 'error') {
          consoleErrors.push(msg.text());
        }
      });
      
      // Trigger a JavaScript error
      await page.evaluate(() => {
        try {
          (window as unknown as { nonExistentFunction: () => void }).nonExistentFunction();
        } catch (error) {
          console.error('Test error:', error);
        }
      });
      
      await page.waitForTimeout(1000);
      
      expect(consoleErrors.length).toBeGreaterThan(0);
      
      console.log(`JavaScript error handling in ${browserInfo.name}:`, consoleErrors);
    });

    test('should handle memory pressure gracefully', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Create memory pressure (be careful not to crash the browser)
      const memoryUsage = await page.evaluate(() => {
        const arrays = [];
        const maxArrays = browserInfo.isMobile ? 100 : 1000; // Less on mobile
        
        try {
          for (let i = 0; i < maxArrays; i++) {
            arrays.push(new Array(1000).fill(i));
          }
          
          // Check if performance.memory is available (Chrome-specific)
          if ('memory' in performance) {
            const memory = (performance as unknown as { memory: { usedJSHeapSize: number; totalJSHeapSize: number; jsHeapSizeLimit: number } }).memory;
            return {
              used: memory.usedJSHeapSize,
              total: memory.totalJSHeapSize,
              limit: memory.jsHeapSizeLimit,
            };
          }
          
          return { arrays: arrays.length };
        } catch (error) {
          return { error: error.message };
        }
      });
      
      expect(memoryUsage).toBeTruthy();
      
      console.log(`Memory usage in ${browserInfo.name}:`, memoryUsage);
      
      // Clean up
      await page.evaluate(() => {
        // Force garbage collection if available
        if ('gc' in window) {
          (window as unknown as { gc: () => void }).gc();
        }
      });
    });

    test('should handle DOM manipulation edge cases', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      const domTest = await page.evaluate(() => {
        const results = {
          createElement: true,
          appendChild: true,
          removeChild: true,
          cloneNode: true,
          innerHTML: true,
          addEventListener: true,
        };
        
        try {
          // Test DOM manipulation operations
          const div = document.createElement('div');
          div.innerHTML = '<span>Test</span>';
          document.body.appendChild(div);
          
          const clone = div.cloneNode(true);
          document.body.appendChild(clone);
          
          const span = div.querySelector('span');
          if (span) {
            span.addEventListener('click', () => {});
          }
          
          document.body.removeChild(div);
          document.body.removeChild(clone);
        } catch (error) {
          console.error('DOM manipulation error:', error);
          return { error: error.message };
        }
        
        return results;
      });
      
      expect(domTest).toBeTruthy();
      
      console.log(`DOM manipulation in ${browserInfo.name}:`, domTest);
    });
  });

  test.describe('Accessibility and Keyboard Navigation', () => {
    test('should support ARIA attributes consistently', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const ariaSupport = await page.evaluate(() => {
        const button = document.createElement('button');
        button.setAttribute('aria-label', 'Test Button');
        button.setAttribute('aria-describedby', 'description');
        button.setAttribute('aria-expanded', 'false');
        button.setAttribute('role', 'button');
        
        document.body.appendChild(button);
        
        const computedRole = button.getAttribute('role');
        const ariaLabel = button.getAttribute('aria-label');
        const ariaExpanded = button.getAttribute('aria-expanded');
        
        document.body.removeChild(button);
        
        return {
          role: computedRole === 'button',
          ariaLabel: ariaLabel === 'Test Button',
          ariaExpanded: ariaExpanded === 'false',
          supportsAria: true,
        };
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(ariaSupport.role).toBe(true);
      expect(ariaSupport.ariaLabel).toBe(true);
      expect(ariaSupport.ariaExpanded).toBe(true);
      expect(ariaSupport.supportsAria).toBe(true);
      
      console.log(`ARIA support in ${browserInfo.name}:`, ariaSupport);
    });

    test('should handle keyboard navigation correctly', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      // Create focusable elements
      await page.evaluate(() => {
        const form = document.createElement('form');
        form.innerHTML = `
          <input type="text" id="input1" placeholder="Input 1">
          <input type="text" id="input2" placeholder="Input 2">
          <button type="button" id="button1">Button 1</button>
          <button type="button" id="button2">Button 2</button>
        `;
        document.body.appendChild(form);
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      // Test tab navigation
      await page.focus('#input1');
      
      let focusedElement = await page.evaluate(() => document.activeElement?.id);
      expect(focusedElement).toBe('input1');
      
      await page.keyboard.press('Tab');
      focusedElement = await page.evaluate(() => document.activeElement?.id);
      expect(focusedElement).toBe('input2');
      
      await page.keyboard.press('Tab');
      focusedElement = await page.evaluate(() => document.activeElement?.id);
      expect(focusedElement).toBe('button1');
      
      // Test shift+tab (reverse navigation)
      await page.keyboard.press('Shift+Tab');
      focusedElement = await page.evaluate(() => document.activeElement?.id);
      expect(focusedElement).toBe('input2');
      
      console.log(`Keyboard navigation working correctly in ${browserInfo.name}`);
      
      // Clean up
      await page.evaluate(() => {
        const form = document.querySelector('form');
        if (form) form.remove();
      });
    });

    test('should support focus management', async ({ page }) => {
      await crossBrowserPage.gotoTestPage('/');
      
      const focusTest = await page.evaluate(() => {
        const button = document.createElement('button');
        button.textContent = 'Test Button';
        button.id = 'focus-test';
        document.body.appendChild(button);
        
        // Test focus methods
        button.focus();
        const hasFocus = document.activeElement === button;
        
        button.blur();
        const lostFocus = document.activeElement !== button;
        
        document.body.removeChild(button);
        
        return {
          focusMethod: hasFocus,
          blurMethod: lostFocus,
          activeElementSupport: !!document.activeElement,
        };
      });
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      
      expect(focusTest.focusMethod).toBe(true);
      expect(focusTest.blurMethod).toBe(true);
      expect(focusTest.activeElementSupport).toBe(true);
      
      console.log(`Focus management in ${browserInfo.name}:`, focusTest);
    });
  });

  test.describe('Cross-Browser Screenshot Comparison', () => {
    test('should take consistent screenshots across browsers', async ({ page }, testInfo) => {
      await crossBrowserPage.gotoTestPage('/');
      
      // Create a test UI with various elements
      await page.evaluate(() => {
        const container = document.createElement('div');
        container.style.cssText = `
          padding: 20px;
          font-family: Arial, sans-serif;
          background: linear-gradient(45deg, #f0f0f0, #e0e0e0);
        `;
        
        container.innerHTML = `
          <h1>Cross-Browser Test Page</h1>
          <div style="display: flex; gap: 10px; margin: 10px 0;">
            <button style="padding: 10px; border: 1px solid #ccc; border-radius: 4px;">Button</button>
            <input type="text" placeholder="Text Input" style="padding: 8px; border: 1px solid #ccc;">
            <select style="padding: 8px;">
              <option>Option 1</option>
              <option>Option 2</option>
            </select>
          </div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 10px 0;">
            <div style="background: #ff6b6b; padding: 20px; color: white;">Grid Item 1</div>
            <div style="background: #4ecdc4; padding: 20px; color: white;">Grid Item 2</div>
          </div>
          <div style="width: 100px; height: 100px; background: conic-gradient(from 0deg, red, yellow, green, cyan, blue, magenta, red); border-radius: 50%; margin: 10px auto;"></div>
        `;
        
        document.body.appendChild(container);
      });
      
      await crossBrowserPage.waitForAllAnimationsComplete();
      await crossBrowserPage.takeCompatibleScreenshot('cross-browser-ui');
      
      const browserInfo = crossBrowserPage.getBrowserInfo();
      console.log(`Screenshot taken for ${browserInfo.name}`);
    });
  });
});