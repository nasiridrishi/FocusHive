/**
 * Comprehensive Viewport Responsiveness Tests for FocusHive
 * Tests responsive behavior across all viewport sizes and orientations
 */

import {expect, Page, test} from '@playwright/test';
import {
  MOBILE_DEVICES,
  MobileDeviceConfig,
  VIEWPORT_BREAKPOINTS,
  ViewportBreakpoint
} from './mobile.config';
import {MobileHelper} from '../../helpers/mobile.helper';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_URLS} from '../../helpers/test-data';

interface ViewportTestResult {
  viewport: { width: number; height: number };
  orientation: 'portrait' | 'landscape';
  passed: boolean;
  issues: string[];
  metrics: {
    horizontalScroll: boolean;
    contentOverflow: boolean;
    touchTargetSize: number;
    textReadability: boolean;
    imageScaling: boolean;
    navigationAccessible: boolean;
  };
}

interface ResponsiveTest {
  name: string;
  selector: string;
  expectedBehavior: {
    mobile: string;
    tablet: string;
    desktop: string;
  };
  validator: (page: Page, viewport: { width: number; height: number }) => Promise<boolean>;
}

test.describe('Viewport Responsiveness - Breakpoint Testing', () => {
  let authHelper: AuthHelper;
  let _mobileHelper: MobileHelper;

  test.beforeEach(async ({page}) => {
    authHelper = new AuthHelper(page);
    _mobileHelper = new MobileHelper(page);

    // Authenticate user for protected pages
    await authHelper.loginWithTestUser();
  });

  /**
   * Test all major viewport breakpoints
   */
  Object.entries(VIEWPORT_BREAKPOINTS).forEach(([_key, breakpoint]) => {
    test(`should render correctly at ${breakpoint.name} breakpoint (${breakpoint.width}px)`, async ({page}) => {
      // Set viewport size
      await page.setViewportSize({
        width: breakpoint.width,
        height: 800 // Standard mobile height
      });

      // Test all critical pages
      const testPages = [
        {name: 'Home', url: TEST_URLS.HOME},
        {name: 'Dashboard', url: TEST_URLS.DASHBOARD},
        {name: 'Hive List', url: TEST_URLS.HIVE_LIST},
        {name: 'Profile', url: TEST_URLS.PROFILE}
      ];

      for (const testPage of testPages) {
        await test.step(`Testing ${testPage.name} at ${breakpoint.width}px`, async () => {
          await page.goto(testPage.url);
          await page.waitForLoadState('networkidle');

          const result = await validateViewportResponsiveness(page, breakpoint);

          expect(result.passed,
              `${testPage.name} failed at ${breakpoint.width}px: ${result.issues.join(', ')}`
          ).toBeTruthy();

          // Verify no horizontal scroll
          expect(result.metrics.horizontalScroll,
              `Horizontal scroll detected at ${breakpoint.width}px`
          ).toBeFalsy();

          // Verify content is accessible
          expect(result.metrics.navigationAccessible,
              `Navigation not accessible at ${breakpoint.width}px`
          ).toBeTruthy();
        });
      }
    });
  });

  /**
   * Test specific device configurations
   */
  Object.entries(MOBILE_DEVICES).forEach(([_key, device]) => {
    test(`should render correctly on ${device.name}`, async ({browser}) => {
      const context = await browser.newContext({
        viewport: device.viewport,
        userAgent: device.userAgent,
        deviceScaleFactor: device.deviceScaleFactor,
        isMobile: device.isMobile,
        hasTouch: device.hasTouch
      });

      const page = await context.newPage();
      const _helper = new MobileHelper(page);

      // Test authentication flow on device
      await page.goto(TEST_URLS.LOGIN);
      await authHelper.loginWithTestUser();

      // Test main app functionality
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      const result = await validateDeviceSpecificFeatures(page, device);

      expect(result.passed,
          `${device.name} failed: ${result.issues.join(', ')}`
      ).toBeTruthy();

      // Device-specific validations
      if (device.platform === 'iOS' && device.safeArea) {
        await validateSafeAreaHandling(page, device.safeArea);
      }

      if (device.category === 'foldable') {
        await validateFoldableFeatures(page, device);
      }

      await context.close();
    });
  });

  /**
   * Test orientation changes
   */
  test('should handle orientation changes gracefully', async ({browser}) => {
    const devices = ['iPhone 14', 'Pixel 7', 'iPad Air'];

    for (const deviceName of devices) {
      await test.step(`Testing orientation changes on ${deviceName}`, async () => {
        const context = await browser.newContext({
          ...devices[deviceName as keyof typeof devices]
        });

        const page = await context.newPage();
        await authHelper.loginWithTestUser();
        await page.goto(TEST_URLS.DASHBOARD);

        // Test portrait orientation
        await page.setViewportSize({width: 390, height: 844});
        let result = await validateViewportResponsiveness(page, {
          name: 'Portrait',
          width: 390,
          description: 'Portrait mode',
          testScenarios: []
        });
        expect(result.passed).toBeTruthy();

        // Test landscape orientation
        await page.setViewportSize({width: 844, height: 390});
        result = await validateViewportResponsiveness(page, {
          name: 'Landscape',
          width: 844,
          description: 'Landscape mode',
          testScenarios: []
        });
        expect(result.passed).toBeTruthy();

        // Verify layout adapted correctly
        const navigationVisible = await page.isVisible('[data-testid="mobile-navigation"]');
        const sidebarVisible = await page.isVisible('[data-testid="sidebar"]');

        // In landscape, we might show sidebar instead of bottom navigation
        expect(navigationVisible || sidebarVisible,
            'Navigation not accessible in landscape mode'
        ).toBeTruthy();

        await context.close();
      });
    }
  });

  /**
   * Test dynamic viewport changes (browser resize)
   */
  test('should respond to dynamic viewport changes', async ({page}) => {
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.DASHBOARD);

    // Test progression from mobile to tablet to desktop
    const viewportProgression = [
      {width: 320, height: 568, name: 'Mobile XS'},
      {width: 390, height: 844, name: 'Mobile Standard'},
      {width: 768, height: 1024, name: 'Tablet'},
      {width: 1024, height: 768, name: 'Tablet Landscape'},
      {width: 1440, height: 900, name: 'Desktop'}
    ];

    for (const viewport of viewportProgression) {
      await test.step(`Resizing to ${viewport.name} (${viewport.width}x${viewport.height})`, async () => {
        await page.setViewportSize({width: viewport.width, height: viewport.height});

        // Wait for CSS transitions and layout shifts
        await page.waitForTimeout(500);
        await page.waitForLoadState('networkidle');

        // Validate layout after resize
        const result = await validateViewportResponsiveness(page, {
          name: viewport.name,
          width: viewport.width,
          description: `Dynamic resize to ${viewport.name}`,
          testScenarios: ['layout_adaptation', 'content_reflow']
        });

        expect(result.passed,
            `Viewport change to ${viewport.name} failed: ${result.issues.join(', ')}`
        ).toBeTruthy();

        // Check for layout shifts during resize
        const clsScore = await measureCumulativeLayoutShift(page);
        expect(clsScore).toBeLessThan(0.1);
      });
    }
  });

  /**
   * Test content adaptation patterns
   */
  test('should adapt content correctly across breakpoints', async ({page}) => {
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.HIVE_LIST);

    const contentTests: ResponsiveTest[] = [
      {
        name: 'Navigation Menu',
        selector: '[data-testid="main-navigation"]',
        expectedBehavior: {
          mobile: 'hidden_with_hamburger',
          tablet: 'collapsed_drawer',
          desktop: 'full_sidebar'
        },
        validator: async (page, viewport) => {
          if (viewport.width < 768) {
            return await page.isVisible('[data-testid="mobile-menu-toggle"]');
          } else if (viewport.width < 1024) {
            return await page.isVisible('[data-testid="tablet-navigation"]');
          } else {
            return await page.isVisible('[data-testid="desktop-sidebar"]');
          }
        }
      },
      {
        name: 'Hive Grid',
        selector: '[data-testid="hive-grid"]',
        expectedBehavior: {
          mobile: 'single_column',
          tablet: 'two_columns',
          desktop: 'three_or_more_columns'
        },
        validator: async (page, viewport) => {
          const gridContainer = page.locator('[data-testid="hive-grid"]');
          const computedStyle = await gridContainer.evaluate((el) => {
            return window.getComputedStyle(el).gridTemplateColumns;
          });

          const columnCount = computedStyle.split(' ').length;

          if (viewport.width < 768) return columnCount === 1;
          if (viewport.width < 1024) return columnCount === 2;
          return columnCount >= 3;
        }
      },
      {
        name: 'Action Buttons',
        selector: '[data-testid="action-buttons"]',
        expectedBehavior: {
          mobile: 'floating_or_bottom',
          tablet: 'inline_with_content',
          desktop: 'toolbar_or_inline'
        },
        validator: async (page, viewport) => {
          const actionButtons = page.locator('[data-testid="action-buttons"]');
          const isVisible = await actionButtons.isVisible();

          if (!isVisible) return false;

          const position = await actionButtons.evaluate((el) => {
            const rect = el.getBoundingClientRect();
            const _windowHeight = window.innerHeight;
            return {
              bottom: rect.bottom,
              isFixed: window.getComputedStyle(el).position === 'fixed'
            };
          });

          // Mobile: likely floating or at bottom
          if (viewport.width < 768) {
            return position.isFixed || position.bottom > viewport.width * 0.8;
          }

          return true; // Tablet/desktop: more flexible positioning
        }
      }
    ];

    // Test each content pattern across breakpoints
    for (const contentTest of contentTests) {
      await test.step(`Testing ${contentTest.name} responsiveness`, async () => {
        const breakpoints = [
          {width: 360, category: 'mobile'},
          {width: 768, category: 'tablet'},
          {width: 1024, category: 'desktop'}
        ];

        for (const breakpoint of breakpoints) {
          await page.setViewportSize({width: breakpoint.width, height: 800});
          await page.waitForTimeout(300); // Allow CSS transitions

          const isValid = await contentTest.validator(page, {width: breakpoint.width, height: 800});

          expect(isValid,
              `${contentTest.name} failed validation at ${breakpoint.width}px (${breakpoint.category})`
          ).toBeTruthy();
        }
      });
    }
  });

  /**
   * Test viewport meta tag and scaling
   */
  test('should have correct viewport meta configuration', async ({page}) => {
    await page.goto(TEST_URLS.HOME);

    const viewportMeta = await page.locator('meta[name="viewport"]').getAttribute('content');

    expect(viewportMeta).toContain('width=device-width');
    expect(viewportMeta).toContain('initial-scale=1');
    expect(viewportMeta).not.toContain('maximum-scale=1'); // Allow zoom for accessibility
    expect(viewportMeta).not.toContain('user-scalable=no'); // Allow zoom for accessibility
  });

  /**
   * Test CSS media queries activation
   */
  test('should activate correct media queries at breakpoints', async ({page}) => {
    await page.goto(TEST_URLS.HOME);

    const mediaQueryTests = [
      {query: '(max-width: 767px)', width: 320, shouldMatch: true},
      {query: '(max-width: 767px)', width: 768, shouldMatch: false},
      {query: '(min-width: 768px)', width: 768, shouldMatch: true},
      {query: '(min-width: 768px)', width: 767, shouldMatch: false},
      {query: '(min-width: 1024px)', width: 1024, shouldMatch: true},
      {query: '(min-width: 1024px)', width: 1023, shouldMatch: false}
    ];

    for (const test of mediaQueryTests) {
      await page.setViewportSize({width: test.width, height: 800});

      const matches = await page.evaluate((query) => {
        return window.matchMedia(query).matches;
      }, test.query);

      expect(matches).toBe(test.shouldMatch);
    }
  });
});

/**
 * Helper function to validate viewport responsiveness
 */
async function validateViewportResponsiveness(
    page: Page,
    _breakpoint: ViewportBreakpoint
): Promise<ViewportTestResult> {
  const viewport = await page.viewportSize();
  if (!viewport) throw new Error('No viewport size available');

  const issues: string[] = [];

  // Check for horizontal scroll
  const horizontalScroll = await page.evaluate(() => {
    return document.documentElement.scrollWidth > window.innerWidth;
  });

  if (horizontalScroll) {
    issues.push('Horizontal scroll detected');
  }

  // Check for content overflow
  const contentOverflow = await page.evaluate(() => {
    const elements = Array.from(document.querySelectorAll('*'));
    return elements.some(el => {
      const rect = el.getBoundingClientRect();
      return rect.right > window.innerWidth + 10; // 10px tolerance
    });
  });

  if (contentOverflow) {
    issues.push('Content overflowing viewport');
  }

  // Check minimum touch target sizes
  const touchTargetIssues = await validateTouchTargets(page);
  issues.push(...touchTargetIssues);

  // Check text readability
  const textReadability = await validateTextReadability(page);
  if (!textReadability.passed) {
    issues.push(...textReadability.issues);
  }

  // Check image scaling
  const imageScaling = await validateImageScaling(page);
  if (!imageScaling.passed) {
    issues.push(...imageScaling.issues);
  }

  // Check navigation accessibility
  const navigationAccessible = await validateNavigationAccessibility(page, viewport);
  if (!navigationAccessible) {
    issues.push('Navigation not accessible');
  }

  return {
    viewport,
    orientation: viewport.width > viewport.height ? 'landscape' : 'portrait',
    passed: issues.length === 0,
    issues,
    metrics: {
      horizontalScroll,
      contentOverflow,
      touchTargetSize: await getMinimumTouchTargetSize(page),
      textReadability: textReadability.passed,
      imageScaling: imageScaling.passed,
      navigationAccessible
    }
  };
}

/**
 * Validate device-specific features
 */
async function validateDeviceSpecificFeatures(
    page: Page,
    device: MobileDeviceConfig
): Promise<ViewportTestResult> {
  const viewport = device.viewport;
  const issues: string[] = [];

  // Platform-specific validations
  if (device.platform === 'iOS') {
    // Test iOS-specific features
    const iosFeatures = await page.evaluate(() => {
      return {
        hasSafeArea: CSS.supports('padding-top', 'env(safe-area-inset-top)'),
        hasStandalone: 'standalone' in navigator,
        hasShareAPI: 'share' in navigator
      };
    });

    if (device.safeArea && !iosFeatures.hasSafeArea) {
      issues.push('Safe area insets not supported');
    }
  }

  if (device.platform === 'Android') {
    // Test Android-specific features
    const androidFeatures = await page.evaluate(() => {
      const ua = navigator.userAgent;
      return {
        isAndroid: ua.includes('Android'),
        hasWebAPK: 'serviceWorker' in navigator,
        hasShareAPI: 'share' in navigator
      };
    });

    if (!androidFeatures.isAndroid) {
      issues.push('Android user agent not detected');
    }
  }

  // Touch-specific validations
  if (device.hasTouch) {
    const touchSupport = await page.evaluate(() => {
      return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    });

    if (!touchSupport) {
      issues.push('Touch support not detected');
    }
  }

  return {
    viewport,
    orientation: viewport.width > viewport.height ? 'landscape' : 'portrait',
    passed: issues.length === 0,
    issues,
    metrics: {
      horizontalScroll: false,
      contentOverflow: false,
      touchTargetSize: 44,
      textReadability: true,
      imageScaling: true,
      navigationAccessible: true
    }
  };
}

/**
 * Validate safe area handling for iOS devices
 */
async function validateSafeAreaHandling(
    page: Page,
    _safeArea: { top: number; right: number; bottom: number; left: number }
): Promise<void> {
  const safeAreaSupport = await page.evaluate(() => {
    const testElement = document.createElement('div');
    testElement.style.paddingTop = 'env(safe-area-inset-top)';
    document.body.appendChild(testElement);

    const computedStyle = window.getComputedStyle(testElement);
    const hasSafeAreaSupport = computedStyle.paddingTop.includes('env(safe-area-inset-top)') ||
        computedStyle.paddingTop !== '0px';

    document.body.removeChild(testElement);
    return hasSafeAreaSupport;
  });

  // This is informational - safe area support is recommended but not required
  console.log(`Safe area support detected: ${safeAreaSupport}`);
}

/**
 * Validate foldable-specific features
 */
async function validateFoldableFeatures(page: Page, device: MobileDeviceConfig): Promise<void> {
  if (device.category !== 'foldable') return;

  // Test screen spanning API if available
  const screenSpanning = await page.evaluate(() => {
    return 'getScreenDetails' in window || 'screen' in window && 'availLeft' in screen;
  });

  console.log(`Screen spanning API support: ${screenSpanning}`);

  // Test CSS environment variables for foldables
  const foldableCSS = await page.evaluate(() => {
    return CSS.supports('left', 'env(fold-left)') && CSS.supports('width', 'env(fold-width)');
  });

  console.log(`Foldable CSS environment variables: ${foldableCSS}`);
}

/**
 * Validate touch target sizes
 */
async function validateTouchTargets(page: Page): Promise<string[]> {
  const issues = await page.evaluate(() => {
    const issues: string[] = [];
    const interactiveElements = document.querySelectorAll('button, a, input, textarea, select, [role="button"], [tabindex="0"]');

    interactiveElements.forEach((element, index) => {
      const rect = element.getBoundingClientRect();
      const minSize = 44; // iOS minimum

      if (rect.width < minSize || rect.height < minSize) {
        const tagName = element.tagName.toLowerCase();
        const id = element.id || `element-${index}`;
        issues.push(`Touch target ${tagName}#${id} too small: ${Math.round(rect.width)}x${Math.round(rect.height)}px`);
      }
    });

    return issues;
  });

  return issues;
}

/**
 * Get minimum touch target size across all interactive elements
 */
async function getMinimumTouchTargetSize(page: Page): Promise<number> {
  return await page.evaluate(() => {
    const interactiveElements = document.querySelectorAll('button, a, input, textarea, select, [role="button"], [tabindex="0"]');
    let minSize = Infinity;

    interactiveElements.forEach(element => {
      const rect = element.getBoundingClientRect();
      const size = Math.min(rect.width, rect.height);
      if (size > 0) {
        minSize = Math.min(minSize, size);
      }
    });

    return minSize === Infinity ? 0 : minSize;
  });
}

/**
 * Validate text readability
 */
async function validateTextReadability(page: Page): Promise<{ passed: boolean; issues: string[] }> {
  const textIssues = await page.evaluate(() => {
    const issues: string[] = [];
    const textElements = document.querySelectorAll('p, span, div, h1, h2, h3, h4, h5, h6, li, td, th');

    textElements.forEach((element, index) => {
      const computedStyle = window.getComputedStyle(element);
      const fontSize = parseFloat(computedStyle.fontSize);
      const lineHeight = computedStyle.lineHeight;

      // Minimum font size for mobile readability
      if (fontSize < 14) {
        issues.push(`Text too small at element ${index}: ${fontSize}px`);
      }

      // Check line height for readability
      if (lineHeight !== 'normal' && parseFloat(lineHeight) / fontSize < 1.2) {
        issues.push(`Line height too small at element ${index}`);
      }
    });

    return issues;
  });

  return {
    passed: textIssues.length === 0,
    issues: textIssues
  };
}

/**
 * Validate image scaling
 */
async function validateImageScaling(page: Page): Promise<{ passed: boolean; issues: string[] }> {
  const imageIssues = await page.evaluate(() => {
    const issues: string[] = [];
    const images = document.querySelectorAll('img');

    images.forEach((img, index) => {
      const rect = img.getBoundingClientRect();
      const naturalWidth = img.naturalWidth;
      const _naturalHeight = img.naturalHeight;

      // Check if image overflows container
      if (rect.width > window.innerWidth) {
        issues.push(`Image ${index} overflows viewport: ${rect.width}px wide`);
      }

      // Check if image is being scaled down excessively
      if (naturalWidth > 0 && rect.width / naturalWidth < 0.3) {
        issues.push(`Image ${index} scaled down excessively`);
      }
    });

    return issues;
  });

  return {
    passed: imageIssues.length === 0,
    issues: imageIssues
  };
}

/**
 * Validate navigation accessibility
 */
async function validateNavigationAccessibility(page: Page, _viewport: {
  width: number;
  height: number
}): Promise<boolean> {
  // Check if navigation is accessible via menu toggle, sidebar, or bottom navigation
  const navigationElements = [
    '[data-testid="mobile-menu-toggle"]',
    '[data-testid="navigation-drawer"]',
    '[data-testid="bottom-navigation"]',
    '[data-testid="main-navigation"]',
    'nav[role="navigation"]',
    '[role="menubar"]'
  ];

  for (const selector of navigationElements) {
    const isVisible = await page.isVisible(selector);
    if (isVisible) {
      // Check if element is actually clickable/accessible
      const isAccessible = await page.locator(selector).isEnabled();
      if (isAccessible) return true;
    }
  }

  return false;
}

/**
 * Interface for Layout Shift Performance Entries
 */
interface LayoutShiftEntry extends PerformanceEntry {
  value: number;
  hadRecentInput: boolean;
  sources?: Array<{
    node?: Node;
    currentRect: DOMRectReadOnly;
    previousRect: DOMRectReadOnly;
  }>;
}

/**
 * Measure Cumulative Layout Shift
 */
async function measureCumulativeLayoutShift(page: Page): Promise<number> {
  return await page.evaluate(() => {
    return new Promise<number>((resolve) => {
      let clsValue = 0;
      const clsEntries: LayoutShiftEntry[] = [];

      const observer = new PerformanceObserver((entryList) => {
        for (const entry of entryList.getEntries()) {
          const layoutShiftEntry = entry as LayoutShiftEntry;
          if (!layoutShiftEntry.hadRecentInput) {
            clsEntries.push(layoutShiftEntry);
            clsValue += layoutShiftEntry.value;
          }
        }
      });

      observer.observe({entryTypes: ['layout-shift']});

      // Measure for 2 seconds
      setTimeout(() => {
        observer.disconnect();
        resolve(clsValue);
      }, 2000);
    });
  });
}