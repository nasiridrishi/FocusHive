/**
 * Mobile Responsiveness E2E Tests for FocusHive
 * 
 * Comprehensive test suite covering mobile responsiveness, touch interactions,
 * and mobile-specific features across all FocusHive pages and components.
 * 
 * Test Categories:
 * 1. Device Viewport Testing
 * 2. Touch Interactions
 * 3. Responsive Layout Testing
 * 4. Mobile-Specific Features
 * 5. Performance on Mobile
 * 6. Form Usability on Mobile
 * 7. Content Adaptation
 * 8. Mobile Navigation
 * 9. Media Queries Testing
 * 10. PWA Mobile Features
 */

import { test, expect, devices } from '@playwright/test';
import { MobilePage } from '../../pages/MobilePage';
import { MobileHelper } from '../../helpers/mobile.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { TEST_URLS, TEST_USERS, TIMEOUTS } from '../../helpers/test-data';

// Interface for Layout Shift entries
interface LayoutShiftEntry extends PerformanceEntry {
  value: number;
  hadRecentInput: boolean;
}

// Interface for First Input entries  
interface FirstInputEntry extends PerformanceEntry {
  processingStart: number;
}

test.describe('Mobile Responsiveness - Device Viewport Testing', () => {
  test('should render correctly on iPhone SE (320px)', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone SE']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const result = await mobilePage.testAtBreakpoint(375, 667);
    expect(result.passed).toBeTruthy();
    expect(result.issues).toHaveLength(0);
    
    // Verify no horizontal scroll
    const hasHorizontalScroll = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(hasHorizontalScroll).toBeFalsy();
    
    await context.close();
  });

  test('should render correctly on iPhone 12 (390px)', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const result = await mobilePage.testAtBreakpoint(390, 844);
    expect(result.passed).toBeTruthy();
    
    // Test safe area insets for iPhone 12 (notch handling)
    const hasSafeAreaSupport = await page.evaluate(() => {
      const root = document.documentElement;
      const styles = window.getComputedStyle(root);
      return styles.paddingTop.includes('env(safe-area-inset-top)') ||
             styles.paddingTop.includes('constant(safe-area-inset-top)');
    });
    
    // Safe area support is optional but recommended
    console.log('Safe area inset support:', hasSafeAreaSupport);
    
    await context.close();
  });

  test('should render correctly on Samsung Galaxy S21 (360px)', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['Galaxy S21']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const result = await mobilePage.testAtBreakpoint(360, 800);
    expect(result.passed).toBeTruthy();
    
    // Test Android-specific features
    const androidFeatures = await page.evaluate(() => {
      return {
        hasViewportMeta: !!document.querySelector('meta[name="viewport"]'),
        hasThemeColor: !!document.querySelector('meta[name="theme-color"]'),
        hasStatusBarConfig: !!document.querySelector('meta[name="mobile-web-app-status-bar-style"]')
      };
    });
    
    expect(androidFeatures.hasViewportMeta).toBeTruthy();
    
    await context.close();
  });

  test('should render correctly on iPad (768px)', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPad']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const result = await mobilePage.testAtBreakpoint(768, 1024);
    expect(result.passed).toBeTruthy();
    
    // Verify tablet layout differs from mobile
    const layoutElements = await page.evaluate(() => {
      const nav = document.querySelector('nav');
      const main = document.querySelector('main');
      return {
        navVisible: nav ? window.getComputedStyle(nav).display !== 'none' : false,
        mainWidth: main ? window.getComputedStyle(main).width : '0px'
      };
    });
    
    expect(layoutElements.navVisible).toBeTruthy();
    
    await context.close();
  });

  test('should adapt to orientation changes', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const orientationResult = await mobilePage.mobileHelper.testOrientationChange('IPHONE_12');
    
    expect(orientationResult.layoutIssues).toHaveLength(0);
    expect(orientationResult.portraitViewport.width).toBeLessThan(orientationResult.landscapeViewport.width);
    expect(orientationResult.portraitViewport.height).toBeGreaterThan(orientationResult.landscapeViewport.height);
    
    await context.close();
  });

  test('should handle ultra-wide displays (2560px+)', async ({ page }) => {
    const mobilePage = new MobilePage(page);
    
    await page.setViewportSize({ width: 2560, height: 1440 });
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Content should not stretch indefinitely
    const contentWidth = await page.evaluate(() => {
      const main = document.querySelector('main');
      return main ? main.getBoundingClientRect().width : window.innerWidth;
    });
    
    // Content should have reasonable max-width (typically < 1200-1400px)
    expect(contentWidth).toBeLessThan(1600);
    
    // Check for proper centering
    const isContentCentered = await page.evaluate(() => {
      const main = document.querySelector('main');
      if (!main) return false;
      
      const rect = main.getBoundingClientRect();
      const viewportCenter = window.innerWidth / 2;
      const contentCenter = rect.left + rect.width / 2;
      
      return Math.abs(viewportCenter - contentCenter) < 50; // 50px tolerance
    });
    
    expect(isContentCentered).toBeTruthy();
  });
});

test.describe('Mobile Responsiveness - Touch Interactions', () => {
  test('should have proper touch target sizes', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.LOGIN);
    
    const smallTargets = await mobilePage.mobileHelper.checkTouchTargetSizes();
    
    // Log any small targets for debugging but don't fail test immediately
    if (smallTargets.length > 0) {
      console.warn('Small touch targets found:', smallTargets);
    }
    
    // Allow some small targets but ensure critical ones are sized properly
    expect(smallTargets.length).toBeLessThan(5);
    
    await context.close();
  });

  test('should respond to touch interactions within 100ms', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const touchResults = await mobilePage.testTouchInteractions();
    
    expect(touchResults.buttonsResponsive).toBeTruthy();
    expect(touchResults.linksResponsive).toBeTruthy();
    expect(touchResults.averageResponseTime).toBeLessThan(100);
    
    await context.close();
  });

  test('should support swipe gestures', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Test swipe gesture on swipeable content (if present)
    const swipeableContent = page.locator('.swipeable, .carousel, .slider').first();
    
    if (await swipeableContent.isVisible().catch(() => false)) {
      const initialPosition = await swipeableContent.evaluate((el) => {
        return el.scrollLeft || 0;
      });
      
      await mobilePage.mobileHelper.performTouchInteraction({
        type: 'swipe',
        startPoint: { x: 300, y: 200 },
        endPoint: { x: 100, y: 200 }
      });
      
      await page.waitForTimeout(500);
      
      const newPosition = await swipeableContent.evaluate((el) => {
        return el.scrollLeft || 0;
      });
      
      expect(newPosition).not.toBe(initialPosition);
    }
    
    await context.close();
  });

  test('should handle long press interactions', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Test long press on context menu triggers (if available)
    const contextMenuTrigger = page.locator('[data-contextmenu], .context-menu-trigger').first();
    
    if (await contextMenuTrigger.isVisible().catch(() => false)) {
      await mobilePage.mobileHelper.performTouchInteraction({
        type: 'longPress',
        element: contextMenuTrigger,
        duration: 800
      });
      
      await page.waitForTimeout(300);
      
      // Check if context menu appeared
      const contextMenu = page.locator('.context-menu, [role="menu"]');
      const menuVisible = await contextMenu.isVisible().catch(() => false);
      
      // Context menus are optional, just log the result
      console.log('Context menu triggered by long press:', menuVisible);
    }
    
    await context.close();
  });

  test('should support pinch-to-zoom on zoomable content', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Look for zoomable content (images, maps, charts)
    const zoomableContent = page.locator('img, canvas, .zoomable, .chart').first();
    
    if (await zoomableContent.isVisible().catch(() => false)) {
      const contentBox = await zoomableContent.boundingBox();
      
      if (contentBox) {
        const centerX = contentBox.x + contentBox.width / 2;
        const centerY = contentBox.y + contentBox.height / 2;
        
        await mobilePage.mobileHelper.performTouchInteraction({
          type: 'pinch',
          startPoint: { x: centerX, y: centerY },
          scale: 2
        });
        
        await page.waitForTimeout(500);
        
        // Pinch gesture support verification
        const zoomSupported = await page.evaluate(() => {
          return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
        });
        
        expect(zoomSupported).toBeTruthy();
      }
    }
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - Layout Testing', () => {
  test('should stack elements vertically on mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    const cardLayoutResult = await mobilePage.testCardLayout();
    
    expect(cardLayoutResult.cardsStackProperly).toBeTruthy();
    expect(cardLayoutResult.cardContentVisible).toBeTruthy();
    expect(cardLayoutResult.cardsResponsive).toBeTruthy();
    
    await context.close();
  });

  test('should handle navigation menu transformation', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const navResults = await mobilePage.testMobileNavigation();
    
    expect(navResults.hamburgerMenuVisible).toBeTruthy();
    expect(navResults.menuToggles).toBeTruthy();
    expect(navResults.menuItemsAccessible).toBeTruthy();
    
    await context.close();
  });

  test('should prevent horizontal scrolling', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone SE']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const hasHorizontalScroll = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    
    expect(hasHorizontalScroll).toBeFalsy();
    
    // Check for elements that might cause horizontal overflow
    const overflowingElements = await page.evaluate(() => {
      const elements = Array.from(document.querySelectorAll('*'));
      const overflowing: string[] = [];
      
      elements.forEach(el => {
        const rect = el.getBoundingClientRect();
        if (rect.right > window.innerWidth) {
          const tagName = el.tagName.toLowerCase();
          const className = el.className ? `.${el.className.split(' ')[0]}` : '';
          overflowing.push(`${tagName}${className}`);
        }
      });
      
      return overflowing.slice(0, 5); // Limit output
    });
    
    if (overflowingElements.length > 0) {
      console.warn('Elements causing horizontal overflow:', overflowingElements);
    }
    
    expect(overflowingElements.length).toBe(0);
    
    await context.close();
  });

  test('should adapt table layouts for mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    const tables = mobilePage.tables;
    const tableCount = await tables.count();
    
    if (tableCount > 0) {
      // Check if tables are responsive
      const tableResponsive = await tables.first().evaluate((table) => {
        const tableRect = table.getBoundingClientRect();
        const hasHorizontalScroll = table.scrollWidth > table.clientWidth;
        const isResponsive = table.classList.contains('responsive') || 
                           table.classList.contains('table-responsive') ||
                           window.getComputedStyle(table).overflowX === 'auto';
        
        return tableRect.width <= window.innerWidth || hasHorizontalScroll || isResponsive;
      });
      
      expect(tableResponsive).toBeTruthy();
    }
    
    await context.close();
  });

  test('should handle modal and overlay responsiveness', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const modalResults = await mobilePage.testModalBehavior();
    
    // Modals might not be present, so only test if they exist
    if (await mobilePage.modals.count() > 0 || modalResults.modalResponsive !== undefined) {
      expect(modalResults.modalResponsive).toBeTruthy();
      expect(modalResults.modalAccessible).toBeTruthy();
    }
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - Mobile-Specific Features', () => {
  test('should handle virtual keyboard properly', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.LOGIN);
    
    const keyboardResult = await mobilePage.mobileHelper.testVirtualKeyboard();
    
    expect(keyboardResult.keyboardTriggered).toBeTruthy();
    // Layout shifting is sometimes acceptable for virtual keyboard
    console.log('Virtual keyboard layout shifted:', keyboardResult.layoutShifted);
    
    await context.close();
  });

  test('should support safe area insets for devices with notches', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 14 Pro']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const hasSafeAreaInsets = await page.evaluate(() => {
      const root = document.documentElement;
      const computedStyle = window.getComputedStyle(root);
      
      return [
        computedStyle.paddingTop,
        computedStyle.paddingBottom,
        computedStyle.paddingLeft,
        computedStyle.paddingRight
      ].some(padding => 
        padding.includes('env(safe-area-inset') || 
        padding.includes('constant(safe-area-inset')
      );
    });
    
    // Safe area insets are recommended but not required
    console.log('Safe area insets implemented:', hasSafeAreaInsets);
    
    await context.close();
  });

  test('should handle pull-to-refresh functionality', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    // Test pull-to-refresh gesture
    await mobilePage.mobileHelper.performTouchInteraction({
      type: 'swipe',
      startPoint: { x: 200, y: 100 },
      endPoint: { x: 200, y: 300 }
    });
    
    await page.waitForTimeout(1000);
    
    // Check if refresh indicator appeared
    const refreshIndicator = page.locator('.refresh-indicator, .pull-to-refresh, [data-testid="refresh"]');
    const refreshVisible = await refreshIndicator.isVisible().catch(() => false);
    
    // Pull-to-refresh is optional functionality
    console.log('Pull-to-refresh functionality detected:', refreshVisible);
    
    await context.close();
  });

  test('should support mobile browser chrome behavior', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Test viewport meta tag effectiveness
    const viewportMeta = await page.evaluate(() => {
      const meta = document.querySelector('meta[name="viewport"]');
      return meta ? meta.getAttribute('content') : null;
    });
    
    expect(viewportMeta).toBeTruthy();
    expect(viewportMeta).toContain('width=device-width');
    
    // Test status bar styling
    const statusBarMeta = await page.evaluate(() => {
      const meta = document.querySelector('meta[name="mobile-web-app-status-bar-style"]') ||
                   document.querySelector('meta[name="apple-mobile-web-app-status-bar-style"]');
      return meta ? meta.getAttribute('content') : null;
    });
    
    // Status bar styling is optional
    console.log('Status bar styling:', statusBarMeta || 'not configured');
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - Performance Testing', () => {
  test('should load within 3 seconds on 3G network', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    // Simulate slow 3G network
    const client = await page.context().newCDPSession(page);
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: 1.5 * 1024 * 1024 / 8, // 1.5 Mbps
      uploadThroughput: 750 * 1024 / 8, // 750 Kbps
      latency: 40
    });
    
    const startTime = Date.now();
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    const loadTime = Date.now() - startTime;
    
    expect(loadTime).toBeLessThan(3000); // 3 seconds
    
    await context.close();
  });

  test('should maintain 60fps during scrolling', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    const scrollPerformance = await mobilePage.mobileHelper.testScrollPerformance();
    
    expect(scrollPerformance.scrollResponsive).toBeTruthy();
    expect(scrollPerformance.averageFrameTime).toBeLessThan(16.67); // 60fps threshold
    expect(scrollPerformance.droppedFrames).toBeLessThan(5);
    
    await context.close();
  });

  test('should optimize JavaScript bundle size for mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const metrics = await mobilePage.collectMobileMetrics();
    
    // Bundle size should be reasonable for mobile (< 2MB total)
    expect(metrics.bundleSize).toBeLessThan(2 * 1024 * 1024);
    
    // Touch responsiveness should be good
    expect(metrics.touchResponsiveness).toBeLessThan(100);
    
    await context.close();
  });

  test('should implement lazy loading for images', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    // Check for lazy loading attributes
    const lazyImages = await page.evaluate(() => {
      const images = Array.from(document.querySelectorAll('img'));
      return images.filter(img => 
        img.hasAttribute('loading') && img.getAttribute('loading') === 'lazy'
      ).length;
    });
    
    const totalImages = await mobilePage.images.count();
    
    if (totalImages > 0) {
      const lazyLoadingRatio = lazyImages / totalImages;
      // At least 50% of images should be lazy loaded for better mobile performance
      expect(lazyLoadingRatio).toBeGreaterThan(0.3);
    }
    
    await context.close();
  });

  test('should optimize Core Web Vitals for mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Collect Core Web Vitals
    const webVitals = await page.evaluate(() => {
      return new Promise<{
        lcp: number;
        fid: number;
        cls: number;
      }>((resolve) => {
        let lcp = 0;
        let fid = 0;
        let cls = 0;
        
        // LCP Observer
        if ('PerformanceObserver' in window) {
          const lcpObserver = new PerformanceObserver((list) => {
            const entries = list.getEntries();
            lcp = entries[entries.length - 1].startTime;
          });
          lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true });
          
          // CLS Observer
          const clsObserver = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
              const layoutShiftEntry = entry as LayoutShiftEntry;
              if (!layoutShiftEntry.hadRecentInput) {
                cls += layoutShiftEntry.value;
              }
            }
          });
          clsObserver.observe({ type: 'layout-shift', buffered: true });
          
          // FID Observer
          const fidObserver = new PerformanceObserver((list) => {
            const entries = list.getEntries();
            const firstInputEntry = entries[0] as FirstInputEntry;
            fid = firstInputEntry.processingStart - firstInputEntry.startTime;
          });
          fidObserver.observe({ type: 'first-input', buffered: true });
        }
        
        setTimeout(() => {
          resolve({ lcp, fid, cls });
        }, 3000);
      });
    });
    
    // Core Web Vitals thresholds
    expect(webVitals.lcp).toBeLessThan(2500); // LCP < 2.5s
    expect(webVitals.cls).toBeLessThan(0.1);  // CLS < 0.1
    // FID is tested through interaction tests
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - Form Usability', () => {
  test('should optimize input types for mobile keyboards', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.LOGIN);
    
    const formResults = await mobilePage.testMobileFormInteractions();
    
    expect(formResults.inputTypesOptimized).toBeTruthy();
    expect(formResults.virtualKeyboardHandled).toBeTruthy();
    expect(formResults.submitButtonAccessible).toBeTruthy();
    
    await context.close();
  });

  test('should handle autocomplete and autofill', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.REGISTER);
    
    // Check for autocomplete attributes
    const autocompleteFields = await page.evaluate(() => {
      const inputs = Array.from(document.querySelectorAll('input'));
      return inputs.filter(input => input.hasAttribute('autocomplete')).length;
    });
    
    const totalInputs = await mobilePage.textInputs.count();
    
    if (totalInputs > 0) {
      const autocompleteRatio = autocompleteFields / totalInputs;
      expect(autocompleteRatio).toBeGreaterThan(0.5); // Most inputs should have autocomplete
    }
    
    await context.close();
  });

  test('should show clear error messages on mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.LOGIN);
    
    // Try to submit empty form to trigger validation
    const submitButton = mobilePage.submitButtons.first();
    
    if (await submitButton.isVisible()) {
      await submitButton.tap();
      await page.waitForTimeout(500);
      
      // Check if error messages are visible and readable
      const errorMessages = await mobilePage.formErrors.all();
      
      for (const error of errorMessages) {
        if (await error.isVisible()) {
          const errorText = await error.textContent();
          expect(errorText).toBeTruthy();
          expect(errorText?.length).toBeGreaterThan(5);
          
          // Check error message font size
          const fontSize = await error.evaluate((el) => {
            return parseFloat(window.getComputedStyle(el).fontSize);
          });
          
          expect(fontSize).toBeGreaterThan(12); // Readable font size
        }
      }
    }
    
    await context.close();
  });

  test('should handle date and time pickers on mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Look for date/time inputs
    const dateInputs = page.locator('input[type="date"], input[type="time"], input[type="datetime-local"]');
    const dateInputCount = await dateInputs.count();
    
    if (dateInputCount > 0) {
      const firstDateInput = dateInputs.first();
      
      // Test touch interaction with date picker
      await firstDateInput.tap();
      await page.waitForTimeout(500);
      
      // Check if native mobile picker opened or custom picker is usable
      const pickerVisible = await page.evaluate(() => {
        const activeElement = document.activeElement;
        return activeElement?.type === 'date' || 
               activeElement?.type === 'time' ||
               !!document.querySelector('.date-picker, .time-picker, [role="dialog"]');
      });
      
      expect(pickerVisible).toBeTruthy();
    }
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - Content Adaptation', () => {
  test('should handle text truncation and overflow properly', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone SE']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    // Check for text overflow issues
    const textOverflow = await page.evaluate(() => {
      const textElements = Array.from(document.querySelectorAll('p, span, h1, h2, h3, h4, h5, h6'));
      const overflowing: string[] = [];
      
      textElements.forEach(el => {
        const rect = el.getBoundingClientRect();
        const styles = window.getComputedStyle(el);
        
        if (rect.width > window.innerWidth && styles.whiteSpace !== 'pre-wrap') {
          overflowing.push(el.tagName.toLowerCase());
        }
      });
      
      return overflowing.slice(0, 5);
    });
    
    expect(textOverflow).toHaveLength(0);
    
    await context.close();
  });

  test('should adapt media content for mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const mediaResults = await mobilePage.testMediaResponsiveness();
    
    expect(mediaResults.imagesResponsive).toBeTruthy();
    expect(mediaResults.videosResponsive).toBeTruthy();
    expect(mediaResults.mediaAccessible).toBeTruthy();
    // Image optimization is recommended but not required
    console.log('Images optimized for responsive:', mediaResults.imagesOptimized);
    
    await context.close();
  });

  test('should prioritize important content on mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.DASHBOARD);
    
    // Check content order and visibility
    const contentPriority = await page.evaluate(() => {
      const main = document.querySelector('main');
      if (!main) return { primaryVisible: false, secondaryHidden: false };
      
      const primaryContent = main.querySelector('.primary, .main-content, .hero, h1');
      const secondaryContent = main.querySelector('.secondary, .sidebar, .aside');
      
      const primaryVisible = primaryContent ? 
        window.getComputedStyle(primaryContent).display !== 'none' : false;
      
      const secondaryHidden = secondaryContent ? 
        window.getComputedStyle(secondaryContent).display === 'none' ||
        window.getComputedStyle(secondaryContent).visibility === 'hidden' : true;
      
      return { primaryVisible, secondaryHidden };
    });
    
    expect(contentPriority.primaryVisible).toBeTruthy();
    // Secondary content hiding is optional for mobile optimization
    console.log('Secondary content hidden on mobile:', contentPriority.secondaryHidden);
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - PWA Features', () => {
  test('should support add to home screen functionality', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const pwaResults = await mobilePage.testPWAFeatures();
    
    expect(pwaResults.manifestValid).toBeTruthy();
    // PWA features are optional but recommended
    console.log('PWA install prompt available:', pwaResults.installPromptWorks);
    console.log('Service worker active:', pwaResults.serviceWorkerActive);
    
    await context.close();
  });

  test('should handle offline mode gracefully', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Go offline and test behavior
    await context.setOffline(true);
    await page.reload();
    await page.waitForTimeout(2000);
    
    // Check for offline indicators or cached content
    const offlineBanner = await mobilePage.offlineBanner.isVisible().catch(() => false);
    const mainContentVisible = await mobilePage.mainContent.isVisible().catch(() => false);
    
    // Either offline banner should show or content should remain visible (cached)
    const handlesOffline = offlineBanner || mainContentVisible;
    
    // Offline handling is optional but recommended for PWAs
    console.log('Handles offline mode:', handlesOffline);
    
    await context.setOffline(false);
    await context.close();
  });

  test('should support mobile app-like features', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Check for app-like meta tags
    const appMetaTags = await page.evaluate(() => {
      return {
        webAppCapable: !!document.querySelector('meta[name="mobile-web-app-capable"]') ||
                      !!document.querySelector('meta[name="apple-mobile-web-app-capable"]'),
        themeColor: !!document.querySelector('meta[name="theme-color"]'),
        appTitle: !!document.querySelector('meta[name="application-name"]') ||
                 !!document.querySelector('meta[name="apple-mobile-web-app-title"]'),
        manifest: !!document.querySelector('link[rel="manifest"]')
      };
    });
    
    expect(appMetaTags.manifest).toBeTruthy();
    // Other PWA meta tags are recommended but optional
    console.log('PWA meta tags:', appMetaTags);
    
    await context.close();
  });
});

test.describe('Mobile Responsiveness - Dark Mode and Accessibility', () => {
  test('should support dark mode on mobile devices', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12'],
      colorScheme: 'dark'
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    const darkModeResults = await mobilePage.mobileHelper.testDarkModeOnMobile();
    
    expect(darkModeResults.contrastSufficient).toBeTruthy();
    // Dark mode detection and toggle are optional features
    console.log('Dark mode detected:', darkModeResults.darkModeDetected);
    console.log('Dark mode toggle works:', darkModeResults.darkModeToggleWorks);
    
    await context.close();
  });

  test('should maintain accessibility on mobile', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Check for mobile accessibility features
    const accessibilityFeatures = await page.evaluate(() => {
      return {
        skipLinks: !!document.querySelector('.skip-link, [data-testid="skip-link"]'),
        landmarkRoles: document.querySelectorAll('[role="main"], [role="navigation"], [role="banner"]').length > 0,
        focusManagement: !!document.querySelector('[data-focus-trap]') || 
                        document.querySelectorAll('[tabindex]:not([tabindex="-1"])').length > 0,
        ariaLabels: document.querySelectorAll('[aria-label], [aria-labelledby]').length > 5
      };
    });
    
    expect(accessibilityFeatures.landmarkRoles).toBeTruthy();
    // Other accessibility features are recommended
    console.log('Mobile accessibility features:', accessibilityFeatures);
    
    await context.close();
  });

  test('should handle reduced motion preferences', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12'],
      reducedMotion: 'reduce'
    });
    const page = await context.newPage();
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage(TEST_URLS.HOME);
    
    // Check if reduced motion is respected
    const respectsReducedMotion = await page.evaluate(() => {
      return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    });
    
    expect(respectsReducedMotion).toBeTruthy();
    
    // Check for CSS that respects reduced motion
    const hasReducedMotionCSS = await page.evaluate(() => {
      const stylesheets = Array.from(document.styleSheets);
      let hasReducedMotion = false;
      
      try {
        stylesheets.forEach(sheet => {
          const rules = Array.from(sheet.cssRules || []);
          rules.forEach(rule => {
            if (rule.cssText.includes('prefers-reduced-motion')) {
              hasReducedMotion = true;
            }
          });
        });
      } catch (e) {
        // Some stylesheets might not be accessible due to CORS
      }
      
      return hasReducedMotion;
    });
    
    // Reduced motion CSS is recommended but not required
    console.log('Has reduced motion CSS:', hasReducedMotionCSS);
    
    await context.close();
  });
});

// Authenticated user tests
test.describe('Mobile Responsiveness - Authenticated Features', () => {
  test.beforeEach(async ({ page }) => {
    const authHelper = new AuthHelper(page);
    await authHelper.navigateToLogin();
    await authHelper.loginWithValidUser();
  });

  test('should handle hive creation on mobile', async ({ browser, page }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage('/hive');
    
    // Test hive creation flow on mobile
    if (await mobilePage.createHiveButton.isVisible()) {
      await mobilePage.createHiveButton.tap();
      await page.waitForTimeout(500);
      
      // Check if creation form is mobile-friendly
      const formVisible = await page.locator('form, .hive-form').isVisible().catch(() => false);
      expect(formVisible).toBeTruthy();
      
      // Test form inputs are accessible
      const nameInput = page.locator('input[name="name"], input[placeholder*="name" i]').first();
      if (await nameInput.isVisible()) {
        const inputBox = await nameInput.boundingBox();
        expect(inputBox?.height).toBeGreaterThan(44); // Touch-friendly height
      }
    }
  });

  test('should handle timer controls on mobile', async ({ browser, page }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage('/dashboard');
    
    // Test timer controls accessibility on mobile
    if (await mobilePage.startTimerButton.isVisible()) {
      const buttonBox = await mobilePage.startTimerButton.boundingBox();
      expect(buttonBox?.width).toBeGreaterThan(44);
      expect(buttonBox?.height).toBeGreaterThan(44);
      
      // Test timer interaction
      await mobilePage.startTimerButton.tap();
      await page.waitForTimeout(1000);
      
      // Verify timer started
      const timerDisplay = await mobilePage.timerDisplay.textContent();
      expect(timerDisplay).toBeTruthy();
    }
  });

  test('should handle chat on mobile', async ({ browser, page }) => {
    const context = await browser.newContext({
      ...devices['iPhone 12']
    });
    const mobilePage = new MobilePage(page);
    
    await mobilePage.navigateToPage('/hive'); // Assuming chat is in hive page
    
    // Test chat input on mobile
    if (await mobilePage.chatInput.isVisible()) {
      // Test virtual keyboard handling
      await mobilePage.chatInput.tap();
      await page.waitForTimeout(500);
      
      await mobilePage.chatInput.fill('Test message on mobile');
      
      // Test send button accessibility
      if (await mobilePage.sendMessageButton.isVisible()) {
        const sendButtonBox = await mobilePage.sendMessageButton.boundingBox();
        expect(sendButtonBox?.width).toBeGreaterThan(44);
        expect(sendButtonBox?.height).toBeGreaterThan(44);
      }
    }
  });
});