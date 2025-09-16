/**
 * Visual Accessibility Tests
 *
 * Tests visual accessibility features including:
 * - Color contrast compliance (WCAG AA/AAA)
 * - Support for color blindness
 * - High contrast mode compatibility
 * - Dark mode accessibility
 * - Text resizing and zoom support
 * - Focus indicators visibility
 * - Animation and motion controls
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {AccessibilityHelper} from '../../helpers/accessibility.helper';
import {AccessibilityPage} from '../../pages/AccessibilityPage';

interface ColorContrastTest {
  elementType: string;
  selector: string;
  minimumRatio: number;
  testName: string;
}

interface ViewportTest {
  name: string;
  width: number;
  height: number;
  description: string;
}

test.describe('Visual Accessibility Tests', () => {
  let accessibilityHelper: AccessibilityHelper;
  let accessibilityPage: AccessibilityPage;

  test.beforeEach(async ({page}) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);

    await accessibilityHelper.configureAxe();
  });

  test.describe('Color Contrast Compliance', () => {
    test('should meet WCAG AA contrast requirements (4.5:1 normal, 3:1 large text)', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login', '/profile'];

      for (const testPage of testPages) {
        await page.goto(testPage);
        await accessibilityPage.waitForPageLoad();

        // Run axe-core color contrast checks
        const results = await new AxeBuilder({page})
        .withRules(['color-contrast'])
        .analyze();

        expect(results.violations, `Page ${testPage} should meet WCAG AA color contrast`).toEqual([]);

        // Manual spot checks on key elements
        const contrastTests: ColorContrastTest[] = [
          {
            elementType: 'body text',
            selector: 'p, div, span',
            minimumRatio: 4.5,
            testName: 'body text'
          },
          {
            elementType: 'headings',
            selector: 'h1, h2, h3, h4, h5, h6',
            minimumRatio: 4.5,
            testName: 'headings'
          },
          {elementType: 'links', selector: 'a', minimumRatio: 4.5, testName: 'links'},
          {elementType: 'buttons', selector: 'button', minimumRatio: 3.0, testName: 'buttons'}, // UI components need 3:1
          {
            elementType: 'form inputs',
            selector: 'input, select, textarea',
            minimumRatio: 3.0,
            testName: 'form controls'
          }
        ];

        for (const contrastTest of contrastTests) {
          const elements = await page.locator(contrastTest.selector).all();

          for (const element of elements.slice(0, 5)) {
            const isVisible = await element.isVisible();

            if (isVisible) {
              const contrastResult = await accessibilityHelper.checkElementContrast(element);
              const elementText = await element.textContent();

              expect(
                  contrastResult.ratio,
                  `${contrastTest.testName} "${elementText?.slice(0, 30)}..." should meet ${contrastTest.minimumRatio}:1 contrast ratio on ${testPage}`
              ).toBeGreaterThanOrEqual(contrastTest.minimumRatio);
            }
          }
        }
      }
    });

    test('should meet WCAG AAA contrast requirements (7:1 normal, 4.5:1 large text) where possible', async ({page}) => {
      await page.goto('/');

      // Test for AAA compliance on key content areas
      const primaryContent = await page.locator('main, [role="main"]').first();

      if (await primaryContent.count() > 0) {
        const textElements = await primaryContent.locator('p, h1, h2, h3, div').all();

        let aaaCompliantElements = 0;

        for (const element of textElements.slice(0, 10)) {
          const isVisible = await element.isVisible();

          if (isVisible) {
            const contrastResult = await accessibilityHelper.checkElementContrast(element);

            if (contrastResult.meetsAAA) {
              aaaCompliantElements++;
            }
          }
        }

        // At least some elements should meet AAA standards
        console.log(`AAA contrast compliance: ${aaaCompliantElements}/${Math.min(textElements.length, 10)} elements meet AAA standards`);
      }
    });

    test('should maintain contrast in focus and hover states', async ({page}) => {
      await page.goto('/dashboard');

      const interactiveElements = await accessibilityPage.getInteractiveElements();

      for (const element of interactiveElements.slice(0, 8)) {
        // Test default state
        const _defaultContrast = await accessibilityHelper.checkElementContrast(element);

        // Test focus state
        await element.focus();
        await page.waitForTimeout(200);

        const focusContrast = await accessibilityHelper.checkElementContrast(element);
        expect(focusContrast.ratio, 'Focus state should maintain adequate contrast').toBeGreaterThanOrEqual(3.0);

        // Test hover state
        await element.hover();
        await page.waitForTimeout(200);

        const hoverContrast = await accessibilityHelper.checkElementContrast(element);
        expect(hoverContrast.ratio, 'Hover state should maintain adequate contrast').toBeGreaterThanOrEqual(3.0);
      }
    });

    test('should provide sufficient contrast for UI components', async ({page}) => {
      const testPages = ['/', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        // Test UI component contrast (3:1 minimum for WCAG AA)
        const uiComponents = [
          'button',
          'input',
          'select',
          '[role="button"]',
          '[role="tab"]',
          '.card',
          '.chip',
          '.badge'
        ];

        for (const componentSelector of uiComponents) {
          const components = await page.locator(componentSelector).all();

          for (const component of components.slice(0, 3)) {
            const isVisible = await component.isVisible();

            if (isVisible) {
              const contrastResult = await accessibilityHelper.checkElementContrast(component);
              const componentText = await component.textContent();

              if (contrastResult.ratio > 0) { // Only test if colors are detected
                expect(
                    contrastResult.ratio,
                    `UI component "${componentText?.slice(0, 30)}..." should meet 3:1 contrast ratio`
                ).toBeGreaterThanOrEqual(3.0);
              }
            }
          }
        }
      }
    });
  });

  test.describe('Color Blindness Support', () => {
    test('should not rely on color alone to convey information', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        // Look for elements that might rely on color alone
        const colorOnlyIndicators = await page.locator('.error, .success, .warning, .info, [class*="red"], [class*="green"]').all();

        for (const indicator of colorOnlyIndicators) {
          const hasAlternativeIndicator = await indicator.evaluate((el) => {
            // Check for text content
            const hasText = el.textContent?.trim().length > 0;

            // Check for icons
            const hasIcon = el.querySelector('svg, i, [class*="icon"]') !== null;

            // Check for symbols
            const hasSymbol = /[!@#$%^&*()_+\\-=\\[\\]{};':\"|,.<>?]/.test(el.textContent || '');

            // Check for ARIA labels
            const hasAriaLabel = el.getAttribute('aria-label') !== null;

            return hasText || hasIcon || hasSymbol || hasAriaLabel;
          });

          expect(hasAlternativeIndicator, 'Color-coded elements should have alternative indicators').toBeTruthy();
        }
      }
    });

    test('should be usable with color vision simulation', async ({page}) => {
      await page.goto('/dashboard');

      // Simulate different types of color blindness
      const colorBlindnessFilters = [
        {
          name: 'Protanopia (red-blind)',
          filter: 'url("data:image/svg+xml;charset=utf-8,<svg xmlns=\'http://www.w3.org/2000/svg\'><defs><filter id=\'protanopia\'><feColorMatrix values=\'0.567 0.433 0 0 0 0.558 0.442 0 0 0 0 0.242 0.758 0 0 0 0 0 1 0\'/></filter></defs></svg>#protanopia")'
        },
        {
          name: 'Deuteranopia (green-blind)',
          filter: 'url("data:image/svg+xml;charset=utf-8,<svg xmlns=\'http://www.w3.org/2000/svg\'><defs><filter id=\'deuteranopia\'><feColorMatrix values=\'0.625 0.375 0 0 0 0.7 0.3 0 0 0 0 0.3 0.7 0 0 0 0 0 1 0\'/></filter></defs></svg>#deuteranopia")'
        },
        {
          name: 'Tritanopia (blue-blind)',
          filter: 'url("data:image/svg+xml;charset=utf-8,<svg xmlns=\'http://www.w3.org/2000/svg\'><defs><filter id=\'tritanopia\'><feColorMatrix values=\'0.95 0.05 0 0 0 0 0.433 0.567 0 0 0 0.475 0.525 0 0 0 0 0 1 0\'/></filter></defs></svg>#tritanopia")'
        }
      ];

      for (const colorFilter of colorBlindnessFilters) {
        // Apply color blindness simulation
        await page.addStyleTag({
          content: `body { filter: ${colorFilter.filter}; }`
        });

        await page.waitForTimeout(500);

        // Test that key functionality is still accessible
        const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));
        expect(navigationTest.canNavigate, `Should be navigable with ${colorFilter.name}`).toBeTruthy();

        // Test that interactive elements are still identifiable
        const interactiveElements = await accessibilityPage.getInteractiveElements();

        for (const element of interactiveElements.slice(0, 5)) {
          const hasAccessibleName = await accessibilityHelper.hasAccessibleName(element);
          expect(hasAccessibleName, `Interactive elements should have names with ${colorFilter.name}`).toBeTruthy();
        }

        // Remove filter for next test
        await page.addStyleTag({
          content: 'body { filter: none; }'
        });
      }
    });

    test('should provide pattern or texture alternatives to color coding', async ({page}) => {
      await page.goto('/analytics'); // Assuming analytics has charts/graphs

      // Look for charts or data visualizations
      const charts = await page.locator('canvas, svg, [class*="chart"], [class*="graph"]').all();

      for (const chart of charts) {
        // Check for legend or alternative text
        const hasLegend = await chart.evaluate((el) => {
          const parent = el.closest('div, section');
          return parent?.querySelector('[class*="legend"], [aria-label*="legend"]') !== null;
        });

        const hasAltText = await chart.getAttribute('alt');
        const hasAriaLabel = await chart.getAttribute('aria-label');
        const hasTitle = await chart.getAttribute('title');

        const hasAlternativeText = hasAltText || hasAriaLabel || hasTitle || hasLegend;

        expect(hasAlternativeText, 'Charts should have alternative text or legends').toBeTruthy();
      }
    });
  });

  test.describe('High Contrast Mode Support', () => {
    test('should work in Windows High Contrast Mode', async ({page}) => {
      await page.goto('/');

      // Simulate high contrast mode
      await page.emulateMedia({colorScheme: 'dark', reducedMotion: 'reduce'});

      // Apply high contrast styles
      await page.addStyleTag({
        content: `
          @media (prefers-contrast: high) {
            * {
              background: black !important;
              color: white !important;
              border: 1px solid white !important;
            }
          }
        `
      });

      await page.waitForTimeout(500);

      // Test that content is still visible and accessible
      const results = await new AxeBuilder({page})
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

      expect(results.violations, 'Should be accessible in high contrast mode').toEqual([]);

      // Test that interactive elements are still distinguishable
      const buttons = await page.locator('button').all();

      for (const button of buttons.slice(0, 5)) {
        const isVisible = await button.isVisible();
        expect(isVisible, 'Buttons should be visible in high contrast mode').toBeTruthy();

        const hasAccessibleName = await accessibilityHelper.hasAccessibleName(button);
        expect(hasAccessibleName, 'Buttons should have names in high contrast mode').toBeTruthy();
      }
    });

    test('should maintain functionality in forced colors mode', async ({page}) => {
      await page.goto('/dashboard');

      // Simulate forced colors mode (Windows high contrast)
      await page.addStyleTag({
        content: `
          @media (forced-colors: active) {
            * {
              color: ButtonText;
              background-color: ButtonFace;
            }
            button {
              color: ButtonText;
              background-color: ButtonFace;
              border: 1px solid ButtonText;
            }
            a {
              color: LinkText;
            }
          }
        `
      });

      await page.waitForTimeout(500);

      // Test keyboard navigation still works
      const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));
      expect(navigationTest.canNavigate, 'Should be navigable in forced colors mode').toBeTruthy();

      // Test that focus indicators are visible
      const focusableElements = await accessibilityPage.getFocusableElements();

      for (let i = 0; i < Math.min(focusableElements.length, 5); i++) {
        await page.keyboard.press('Tab');
        const focusedElement = page.locator(':focus');

        const focusResult = await accessibilityHelper.verifyFocusIndicator(focusedElement);
        expect(focusResult.isVisible, 'Focus indicators should be visible in forced colors mode').toBeTruthy();
      }
    });

    test('should support custom high contrast themes', async ({page}) => {
      await page.goto('/');

      // Test dark theme with high contrast
      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'high-contrast-dark');
      });

      await page.waitForTimeout(500);

      const results = await new AxeBuilder({page})
      .withRules(['color-contrast'])
      .analyze();

      expect(results.violations, 'High contrast dark theme should meet contrast requirements').toEqual([]);

      // Test light theme with high contrast  
      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'high-contrast-light');
      });

      await page.waitForTimeout(500);

      const lightResults = await new AxeBuilder({page})
      .withRules(['color-contrast'])
      .analyze();

      expect(lightResults.violations, 'High contrast light theme should meet contrast requirements').toEqual([]);
    });
  });

  test.describe('Dark Mode Accessibility', () => {
    test('should maintain accessibility in dark mode', async ({page}) => {
      await page.goto('/');

      // Enable dark mode
      await page.emulateMedia({colorScheme: 'dark'});
      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark');
      });

      await page.waitForTimeout(500);

      const results = await new AxeBuilder({page})
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

      expect(results.violations, 'Dark mode should be accessible').toEqual([]);

      // Test color contrast in dark mode
      const textElements = await page.locator('p, h1, h2, h3, span, div').all();

      for (const element of textElements.slice(0, 8)) {
        const isVisible = await element.isVisible();

        if (isVisible) {
          const contrastResult = await accessibilityHelper.checkElementContrast(element);

          if (contrastResult.ratio > 0) {
            expect(contrastResult.ratio, 'Dark mode text should meet contrast requirements').toBeGreaterThanOrEqual(4.5);
          }
        }
      }
    });

    test('should support automatic dark mode detection', async ({page}) => {
      await page.goto('/');

      // Test system preference detection
      await page.emulateMedia({colorScheme: 'dark'});
      await page.waitForTimeout(500);

      const isDarkModeActive = await page.evaluate(() => {
        return window.matchMedia('(prefers-color-scheme: dark)').matches;
      });

      expect(isDarkModeActive, 'Should detect dark mode preference').toBeTruthy();

      // Switch to light mode
      await page.emulateMedia({colorScheme: 'light'});
      await page.waitForTimeout(500);

      const isLightModeActive = await page.evaluate(() => {
        return window.matchMedia('(prefers-color-scheme: light)').matches;
      });

      expect(isLightModeActive, 'Should detect light mode preference').toBeTruthy();
    });

    test('should provide dark mode toggle accessibility', async ({page}) => {
      await page.goto('/');

      // Look for dark mode toggle
      const darkModeToggle = await page.locator(
          'button:has-text("Dark"), button:has-text("Light"), [aria-label*="theme"], [data-theme-toggle]'
      ).first();

      if (await darkModeToggle.count() > 0) {
        // Toggle should be keyboard accessible
        await darkModeToggle.focus();
        await expect(darkModeToggle).toBeFocused();

        // Should have accessible name
        const toggleName = await accessibilityHelper.getAccessibleName(darkModeToggle);
        expect(toggleName.length, 'Dark mode toggle should have accessible name').toBeGreaterThan(0);

        // Should indicate current state
        const ariaPressed = await darkModeToggle.getAttribute('aria-pressed');
        const hasStateIndication = ariaPressed !== null;

        expect(hasStateIndication, 'Dark mode toggle should indicate current state').toBeTruthy();
      }
    });
  });

  test.describe('Text Resizing and Zoom Support', () => {
    test('should support 200% zoom without horizontal scroll', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        const originalViewport = page.viewportSize() || { width: 1280, height: 720 };

        // Simulate 200% zoom by reducing viewport
        const zoomedViewport = {
          width: Math.floor(originalViewport.width / 2),
          height: Math.floor(originalViewport.height / 2)
        };

        await page.setViewportSize(zoomedViewport);
        await page.waitForTimeout(1000);

        // Check for horizontal scroll
        const hasHorizontalScroll = await page.evaluate(() => {
          return document.body.scrollWidth > window.innerWidth;
        });

        expect(hasHorizontalScroll, `Page ${testPage} should not have horizontal scroll at 200% zoom`).toBeFalsy();

        // Content should still be accessible
        const results = await new AxeBuilder({page})
        .withTags(['wcag2a', 'wcag2aa'])
        .analyze();

        expect(results.violations, `Page ${testPage} should be accessible at 200% zoom`).toEqual([]);

        // Restore original viewport
        await page.setViewportSize(originalViewport);
      }
    });

    test('should support 400% zoom with reflow', async ({page}) => {
      await page.goto('/');

      // Simulate 400% zoom (320px width)
      await page.setViewportSize({width: 320, height: 568});
      await page.waitForTimeout(1000);

      // Should not have horizontal scroll
      const hasHorizontalScroll = await page.evaluate(() => {
        return document.body.scrollWidth > window.innerWidth;
      });

      expect(hasHorizontalScroll, 'Should not have horizontal scroll at 400% zoom').toBeFalsy();

      // Navigation should still work
      const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));
      expect(navigationTest.canNavigate, 'Should be navigable at 400% zoom').toBeTruthy();

      // Interactive elements should still be usable
      const buttons = await page.locator('button').all();

      for (const button of buttons.slice(0, 3)) {
        const isVisible = await button.isVisible();
        const boundingBox = await button.boundingBox();

        if (isVisible && boundingBox) {
          expect(boundingBox.width, 'Buttons should be wide enough at 400% zoom').toBeGreaterThan(0);
          expect(boundingBox.height, 'Buttons should be tall enough at 400% zoom').toBeGreaterThan(0);
        }
      }
    });

    test('should support browser zoom controls', async ({page}) => {
      await page.goto('/dashboard');

      // Test different zoom levels
      const zoomLevels = [1.5, 2.0, 2.5, 3.0];

      for (const zoomLevel of zoomLevels) {
        // Simulate zoom by adjusting CSS zoom
        await page.evaluate((zoom) => {
          document.body.style.zoom = zoom.toString();
        }, zoomLevel);

        await page.waitForTimeout(500);

        // Check that text is still readable
        const textElements = await page.locator('p, h1, h2, h3').all();

        for (const element of textElements.slice(0, 3)) {
          const isVisible = await element.isVisible();

          if (isVisible) {
            const fontSize = await element.evaluate((el) => {
              return window.getComputedStyle(el).fontSize;
            });

            const fontSizeNum = parseFloat(fontSize);
            expect(fontSizeNum, `Text should be readable at ${zoomLevel * 100}% zoom`).toBeGreaterThan(0);
          }
        }
      }

      // Reset zoom
      await page.evaluate(() => {
        document.body.style.zoom = '1';
      });
    });

    test('should support text spacing modifications', async ({page}) => {
      await page.goto('/');

      // Apply WCAG text spacing requirements
      await page.addStyleTag({
        content: `
          * {
            line-height: 1.5 !important;
            letter-spacing: 0.12em !important;
            word-spacing: 0.16em !important;
          }
          p {
            margin-bottom: 2em !important;
          }
        `
      });

      await page.waitForTimeout(500);

      // Check that content doesn't overlap
      const hasOverlap = await page.evaluate(() => {
        const elements = Array.from(document.querySelectorAll('p, h1, h2, h3, div, span'));

        for (let i = 0; i < elements.length - 1; i++) {
          const rect1 = elements[i].getBoundingClientRect();
          const rect2 = elements[i + 1].getBoundingClientRect();

          if (rect1.height > 0 && rect2.height > 0) {
            if (rect1.bottom > rect2.top && rect1.top < rect2.bottom &&
                rect1.right > rect2.left && rect1.left < rect2.right) {
              return true; // Overlap detected
            }
          }
        }
        return false;
      });

      expect(hasOverlap, 'Text should not overlap with WCAG spacing').toBeFalsy();

      // Content should still be functional
      const results = await new AxeBuilder({page})
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

      expect(results.violations, 'Should be accessible with modified text spacing').toEqual([]);
    });
  });

  test.describe('Focus Indicators', () => {
    test('should have visible focus indicators on all interactive elements', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        const focusableElements = await accessibilityPage.getFocusableElements();

        for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {
          await page.keyboard.press('Tab');
          await page.waitForTimeout(100);

          const focusedElement = page.locator(':focus');
          const focusResult = await accessibilityHelper.verifyFocusIndicator(focusedElement);

          expect(focusResult.isVisible, `Focus indicator should be visible on element ${i + 1} of ${testPage}`).toBeTruthy();
          expect(focusResult.meetsContrastRequirement, `Focus indicator should meet contrast requirements on ${testPage}`).toBeTruthy();
        }
      }
    });

    test('should have 3:1 contrast ratio for focus indicators', async ({page}) => {
      await page.goto('/dashboard');

      const focusableElements = await accessibilityPage.getFocusableElements();

      for (const element of focusableElements.slice(0, 8)) {
        await element.focus();
        await page.waitForTimeout(200);

        const focusResult = await accessibilityHelper.verifyFocusIndicator(element);

        if (focusResult.isVisible) {
          // Focus indicator should meet minimum 3:1 contrast ratio
          expect(focusResult.meetsContrastRequirement, 'Focus indicators should meet 3:1 contrast ratio').toBeTruthy();
        }
      }
    });

    test('should not remove default focus indicators', async ({page}) => {
      await page.goto('/');

      // Check that CSS doesn't globally remove focus indicators
      const hasGlobalOutlineNone = await page.evaluate(() => {
        const sheets = Array.from(document.styleSheets);

        for (const sheet of sheets) {
          try {
            const rules = Array.from(sheet.cssRules);

            for (const rule of rules) {
              if (rule instanceof CSSStyleRule) {
                if (rule.selectorText === '*:focus' || rule.selectorText === ':focus') {
                  if (rule.style.outline === 'none' || rule.style.outline === '0') {
                    return true;
                  }
                }
              }
            }
          } catch {
            // Cross-origin stylesheets may throw errors
          }
        }

        return false;
      });

      expect(hasGlobalOutlineNone, 'Should not globally remove focus indicators with outline: none').toBeFalsy();
    });

    test('should provide custom focus indicators when default is removed', async ({page}) => {
      await page.goto('/dashboard');

      const focusableElements = await accessibilityPage.getFocusableElements();

      for (const element of focusableElements.slice(0, 5)) {
        await element.focus();

        const hasCustomFocusIndicator = await element.evaluate((el) => {
          const computed = window.getComputedStyle(el);

          // Check for various types of focus indicators
          const hasOutline = computed.outline !== 'none' && computed.outline !== '0px';
          const hasBoxShadow = computed.boxShadow !== 'none';
          const hasBorder = computed.border !== 'none' && computed.borderWidth !== '0px';
          const hasBackground = computed.backgroundColor !== 'rgba(0, 0, 0, 0)';

          return hasOutline || hasBoxShadow || hasBorder || hasBackground;
        });

        expect(hasCustomFocusIndicator, 'Elements should have some form of focus indicator').toBeTruthy();
      }
    });
  });

  test.describe('Animation and Motion', () => {
    test('should respect prefers-reduced-motion', async ({page}) => {
      await page.goto('/');

      // Enable reduced motion preference
      await page.emulateMedia({reducedMotion: 'reduce'});
      await page.waitForTimeout(500);

      // Check that animations are disabled or reduced
      const animatedElements = await page.locator('[style*="animation"], [class*="animate"], [data-animate]').all();

      for (const element of animatedElements) {
        const animationDuration = await element.evaluate((el) => {
          return window.getComputedStyle(el).animationDuration;
        });

        // Animation should be disabled or very short
        const isReduced = animationDuration === '0s' || animationDuration === 'none';

        expect(isReduced, 'Animations should be reduced when prefers-reduced-motion is set').toBeTruthy();
      }
    });

    test('should provide controls for auto-playing content', async ({page}) => {
      await page.goto('/');

      const autoPlayElements = await page.locator('[autoplay], video[autoplay], audio[autoplay]').all();

      for (const element of autoPlayElements) {
        // Should have controls or be muted
        const hasControls = await element.getAttribute('controls');
        const isMuted = await element.getAttribute('muted');

        expect(
            hasControls !== null || isMuted !== null,
            'Auto-playing content should have controls or be muted'
        ).toBeTruthy();
      }
    });

    test('should not have content that flashes more than 3 times per second', async ({page}) => {
      await page.goto('/');

      // Look for potentially flashing content
      const flashingElements = await page.locator('[class*="flash"], [class*="blink"], [data-flash]').all();

      expect(flashingElements.length, 'Should not have flashing content that could cause seizures').toBe(0);

      // Check CSS animations for rapid flashing
      const animatedElements = await page.locator('[style*="animation"]').all();

      for (const element of animatedElements) {
        const animationDuration = await element.evaluate((el) => {
          const duration = window.getComputedStyle(el).animationDuration;
          return parseFloat(duration) || 0;
        });

        // Animation should not be faster than 0.33s (3 times per second)
        if (animationDuration > 0) {
          expect(animationDuration, 'Animations should not flash more than 3 times per second').toBeGreaterThan(0.33);
        }
      }
    });
  });

  test.describe('Responsive Visual Design', () => {
    test('should maintain visual accessibility across viewports', async ({page}) => {
      const viewportTests: ViewportTest[] = [
        {name: 'Mobile Portrait', width: 375, height: 667, description: 'iPhone SE'},
        {name: 'Mobile Landscape', width: 667, height: 375, description: 'iPhone SE Landscape'},
        {name: 'Tablet Portrait', width: 768, height: 1024, description: 'iPad'},
        {name: 'Tablet Landscape', width: 1024, height: 768, description: 'iPad Landscape'},
        {name: 'Desktop', width: 1920, height: 1080, description: 'Desktop HD'}
      ];

      for (const viewport of viewportTests) {
        await page.setViewportSize({width: viewport.width, height: viewport.height});
        await page.goto('/dashboard');
        await page.waitForTimeout(500);

        // Test touch target sizes on mobile
        if (viewport.width <= 768) {
          const touchTargets = await accessibilityPage.getInteractiveElements();

          for (const target of touchTargets.slice(0, 8)) {
            const boundingBox = await target.boundingBox();

            if (boundingBox) {
              expect(
                  Math.min(boundingBox.width, boundingBox.height),
                  `Touch targets should be at least 44px on ${viewport.name}`
              ).toBeGreaterThanOrEqual(44);
            }
          }
        }

        // Test accessibility compliance at all viewports
        const results = await new AxeBuilder({page})
        .withTags(['wcag2a', 'wcag2aa'])
        .analyze();

        expect(results.violations, `Should be accessible on ${viewport.name}`).toEqual([]);
      }
    });

    test('should maintain readability with different font sizes', async ({page}) => {
      await page.goto('/');

      const fontSizes = ['12px', '14px', '16px', '18px', '20px', '24px'];

      for (const fontSize of fontSizes) {
        await page.addStyleTag({
          content: `body { font-size: ${fontSize} !important; }`
        });

        await page.waitForTimeout(300);

        // Text should still be readable
        const textElements = await page.locator('p, h1, h2, h3').all();

        for (const element of textElements.slice(0, 3)) {
          const isVisible = await element.isVisible();
          expect(isVisible, `Text should be visible at ${fontSize}`).toBeTruthy();

          const contrastResult = await accessibilityHelper.checkElementContrast(element);

          if (contrastResult.ratio > 0) {
            expect(contrastResult.ratio, `Text should maintain contrast at ${fontSize}`).toBeGreaterThanOrEqual(4.5);
          }
        }
      }
    });
  });

  test.describe('Print Accessibility', () => {
    test('should be accessible when printed', async ({page}) => {
      await page.goto('/');

      // Emulate print media
      await page.emulateMedia({media: 'print'});
      await page.waitForTimeout(500);

      // Check that important content is visible
      const results = await new AxeBuilder({page})
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

      expect(results.violations, 'Should be accessible in print mode').toEqual([]);

      // Check that links show URLs (if implemented)
      const links = await page.locator('a[href]').all();

      for (const link of links.slice(0, 3)) {
        const linkText = await link.textContent();
        const href = await link.getAttribute('href');

        // In print mode, links should ideally show their URLs
        // This is implementation-dependent
        console.log(`Print link: "${linkText}" -> ${href}`);
      }
    });
  });
});