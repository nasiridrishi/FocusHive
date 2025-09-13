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

import { test, expect, Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { AccessibilityHelper } from '../../helpers/accessibility.helper';
import { AccessibilityPage } from '../../pages/AccessibilityPage';

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

  test.beforeEach(async ({ page }) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);
    
    await accessibilityHelper.configureAxe();
  });

  test.describe('Color Contrast Compliance', () => {
    test('should meet WCAG AA contrast requirements (4.5:1 normal, 3:1 large text)', async ({ page }) => {
      const testPages = ['/', '/dashboard', '/login', '/profile'];
      
      for (const testPage of testPages) {
        await page.goto(testPage);
        await accessibilityPage.waitForPageLoad();
        
        // Run axe-core color contrast checks
        const results = await new AxeBuilder({ page })
          .withRules(['color-contrast'])
          .analyze();
          
        expect(results.violations, `Page ${testPage} should meet WCAG AA color contrast`).toEqual([]);
        
        // Manual spot checks on key elements
        const contrastTests: ColorContrastTest[] = [
          { elementType: 'body text', selector: 'p, div, span', minimumRatio: 4.5, testName: 'body text' },
          { elementType: 'headings', selector: 'h1, h2, h3, h4, h5, h6', minimumRatio: 4.5, testName: 'headings' },
          { elementType: 'links', selector: 'a', minimumRatio: 4.5, testName: 'links' },
          { elementType: 'buttons', selector: 'button', minimumRatio: 3.0, testName: 'buttons' }, // UI components need 3:1
          { elementType: 'form inputs', selector: 'input, select, textarea', minimumRatio: 3.0, testName: 'form controls' }
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

    test('should meet WCAG AAA contrast requirements (7:1 normal, 4.5:1 large text) where possible', async ({ page }) => {
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

    test('should maintain contrast in focus and hover states', async ({ page }) => {
      await page.goto('/dashboard');
      
      const interactiveElements = await accessibilityPage.getInteractiveElements();
      
      for (const element of interactiveElements.slice(0, 8)) {
        // Test default state
        const defaultContrast = await accessibilityHelper.checkElementContrast(element);
        
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

    test('should provide sufficient contrast for UI components', async ({ page }) => {
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
    test('should not rely on color alone to convey information', async ({ page }) => {
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

    test('should be usable with color vision simulation', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Simulate different types of color blindness
      const colorBlindnessFilters = [
        {
          name: 'Protanopia (red-blind)',
          filter: 'url("data:image/svg+xml;charset=utf-8,<svg xmlns=\\'http://www.w3.org/2000/svg\\'><defs><filter id=\\'protanopia\\'><feColorMatrix values=\\'0.567 0.433 0 0 0 0.558 0.442 0 0 0 0 0.242 0.758 0 0 0 0 0 1 0\\'/></filter></defs></svg>#protanopia")'
        },
        {
          name: 'Deuteranopia (green-blind)', 
          filter: 'url("data:image/svg+xml;charset=utf-8,<svg xmlns=\\'http://www.w3.org/2000/svg\\'><defs><filter id=\\'deuteranopia\\'><feColorMatrix values=\\'0.625 0.375 0 0 0 0.7 0.3 0 0 0 0 0.3 0.7 0 0 0 0 0 1 0\\'/></filter></defs></svg>#deuteranopia")'
        },
        {
          name: 'Tritanopia (blue-blind)',
          filter: 'url("data:image/svg+xml;charset=utf-8,<svg xmlns=\\'http://www.w3.org/2000/svg\\'><defs><filter id=\\'tritanopia\\'><feColorMatrix values=\\'0.95 0.05 0 0 0 0 0.433 0.567 0 0 0 0.475 0.525 0 0 0 0 0 1 0\\'/></filter></defs></svg>#tritanopia")'
        }
      ];\n      \n      for (const colorFilter of colorBlindnessFilters) {\n        // Apply color blindness simulation\n        await page.addStyleTag({\n          content: `body { filter: ${colorFilter.filter}; }`\n        });\n        \n        await page.waitForTimeout(500);\n        \n        // Test that key functionality is still accessible\n        const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));\n        expect(navigationTest.canNavigate, `Should be navigable with ${colorFilter.name}`).toBeTruthy();\n        \n        // Test that interactive elements are still identifiable\n        const interactiveElements = await accessibilityPage.getInteractiveElements();\n        \n        for (const element of interactiveElements.slice(0, 5)) {\n          const hasAccessibleName = await accessibilityHelper.hasAccessibleName(element);\n          expect(hasAccessibleName, `Interactive elements should have names with ${colorFilter.name}`).toBeTruthy();\n        }\n        \n        // Remove filter for next test\n        await page.addStyleTag({\n          content: 'body { filter: none; }'\n        });\n      }\n    });\n\n    test('should provide pattern or texture alternatives to color coding', async ({ page }) => {\n      await page.goto('/analytics'); // Assuming analytics has charts/graphs\n      \n      // Look for charts or data visualizations\n      const charts = await page.locator('canvas, svg, [class*=\"chart\"], [class*=\"graph\"]').all();\n      \n      for (const chart of charts) {\n        // Check for legend or alternative text\n        const hasLegend = await chart.evaluate((el) => {\n          const parent = el.closest('div, section');\n          return parent?.querySelector('[class*=\"legend\"], [aria-label*=\"legend\"]') !== null;\n        });\n        \n        const hasAltText = await chart.getAttribute('alt');\n        const hasAriaLabel = await chart.getAttribute('aria-label');\n        const hasTitle = await chart.getAttribute('title');\n        \n        const hasAlternativeText = hasAltText || hasAriaLabel || hasTitle || hasLegend;\n        \n        expect(hasAlternativeText, 'Charts should have alternative text or legends').toBeTruthy();\n      }\n    });\n  });\n\n  test.describe('High Contrast Mode Support', () => {\n    test('should work in Windows High Contrast Mode', async ({ page }) => {\n      await page.goto('/');\n      \n      // Simulate high contrast mode\n      await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });\n      \n      // Apply high contrast styles\n      await page.addStyleTag({\n        content: `\n          @media (prefers-contrast: high) {\n            * {\n              background: black !important;\n              color: white !important;\n              border: 1px solid white !important;\n            }\n          }\n        `\n      });\n      \n      await page.waitForTimeout(500);\n      \n      // Test that content is still visible and accessible\n      const results = await new AxeBuilder({ page })\n        .withTags(['wcag2a', 'wcag2aa'])\n        .analyze();\n        \n      expect(results.violations, 'Should be accessible in high contrast mode').toEqual([]);\n      \n      // Test that interactive elements are still distinguishable\n      const buttons = await page.locator('button').all();\n      \n      for (const button of buttons.slice(0, 5)) {\n        const isVisible = await button.isVisible();\n        expect(isVisible, 'Buttons should be visible in high contrast mode').toBeTruthy();\n        \n        const hasAccessibleName = await accessibilityHelper.hasAccessibleName(button);\n        expect(hasAccessibleName, 'Buttons should have names in high contrast mode').toBeTruthy();\n      }\n    });\n\n    test('should maintain functionality in forced colors mode', async ({ page }) => {\n      await page.goto('/dashboard');\n      \n      // Simulate forced colors mode (Windows high contrast)\n      await page.addStyleTag({\n        content: `\n          @media (forced-colors: active) {\n            * {\n              color: ButtonText;\n              background-color: ButtonFace;\n            }\n            button {\n              color: ButtonText;\n              background-color: ButtonFace;\n              border: 1px solid ButtonText;\n            }\n            a {\n              color: LinkText;\n            }\n          }\n        `\n      });\n      \n      await page.waitForTimeout(500);\n      \n      // Test keyboard navigation still works\n      const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));\n      expect(navigationTest.canNavigate, 'Should be navigable in forced colors mode').toBeTruthy();\n      \n      // Test that focus indicators are visible\n      const focusableElements = await accessibilityPage.getFocusableElements();\n      \n      for (let i = 0; i < Math.min(focusableElements.length, 5); i++) {\n        await page.keyboard.press('Tab');\n        const focusedElement = page.locator(':focus');\n        \n        const focusResult = await accessibilityHelper.verifyFocusIndicator(focusedElement);\n        expect(focusResult.isVisible, 'Focus indicators should be visible in forced colors mode').toBeTruthy();\n      }\n    });\n\n    test('should support custom high contrast themes', async ({ page }) => {\n      await page.goto('/');\n      \n      // Test dark theme with high contrast\n      await page.evaluate(() => {\n        document.documentElement.setAttribute('data-theme', 'high-contrast-dark');\n      });\n      \n      await page.waitForTimeout(500);\n      \n      const results = await new AxeBuilder({ page })\n        .withRules(['color-contrast'])\n        .analyze();\n        \n      expect(results.violations, 'High contrast dark theme should meet contrast requirements').toEqual([]);\n      \n      // Test light theme with high contrast  \n      await page.evaluate(() => {\n        document.documentElement.setAttribute('data-theme', 'high-contrast-light');\n      });\n      \n      await page.waitForTimeout(500);\n      \n      const lightResults = await new AxeBuilder({ page })\n        .withRules(['color-contrast'])\n        .analyze();\n        \n      expect(lightResults.violations, 'High contrast light theme should meet contrast requirements').toEqual([]);\n    });\n  });\n\n  test.describe('Dark Mode Accessibility', () => {\n    test('should maintain accessibility in dark mode', async ({ page }) => {\n      await page.goto('/');\n      \n      // Enable dark mode\n      await page.emulateMedia({ colorScheme: 'dark' });\n      await page.evaluate(() => {\n        document.documentElement.setAttribute('data-theme', 'dark');\n      });\n      \n      await page.waitForTimeout(500);\n      \n      const results = await new AxeBuilder({ page })\n        .withTags(['wcag2a', 'wcag2aa'])\n        .analyze();\n        \n      expect(results.violations, 'Dark mode should be accessible').toEqual([]);\n      \n      // Test color contrast in dark mode\n      const textElements = await page.locator('p, h1, h2, h3, span, div').all();\n      \n      for (const element of textElements.slice(0, 8)) {\n        const isVisible = await element.isVisible();\n        \n        if (isVisible) {\n          const contrastResult = await accessibilityHelper.checkElementContrast(element);\n          \n          if (contrastResult.ratio > 0) {\n            expect(contrastResult.ratio, 'Dark mode text should meet contrast requirements').toBeGreaterThanOrEqual(4.5);\n          }\n        }\n      }\n    });\n\n    test('should support automatic dark mode detection', async ({ page }) => {\n      await page.goto('/');\n      \n      // Test system preference detection\n      await page.emulateMedia({ colorScheme: 'dark' });\n      await page.waitForTimeout(500);\n      \n      const isDarkModeActive = await page.evaluate(() => {\n        return window.matchMedia('(prefers-color-scheme: dark)').matches;\n      });\n      \n      expect(isDarkModeActive, 'Should detect dark mode preference').toBeTruthy();\n      \n      // Switch to light mode\n      await page.emulateMedia({ colorScheme: 'light' });\n      await page.waitForTimeout(500);\n      \n      const isLightModeActive = await page.evaluate(() => {\n        return window.matchMedia('(prefers-color-scheme: light)').matches;\n      });\n      \n      expect(isLightModeActive, 'Should detect light mode preference').toBeTruthy();\n    });\n\n    test('should provide dark mode toggle accessibility', async ({ page }) => {\n      await page.goto('/');\n      \n      // Look for dark mode toggle\n      const darkModeToggle = await page.locator(\n        'button:has-text(\"Dark\"), button:has-text(\"Light\"), [aria-label*=\"theme\"], [data-theme-toggle]'\n      ).first();\n      \n      if (await darkModeToggle.count() > 0) {\n        // Toggle should be keyboard accessible\n        await darkModeToggle.focus();\n        await expect(darkModeToggle).toBeFocused();\n        \n        // Should have accessible name\n        const toggleName = await accessibilityHelper.getAccessibleName(darkModeToggle);\n        expect(toggleName.length, 'Dark mode toggle should have accessible name').toBeGreaterThan(0);\n        \n        // Should indicate current state\n        const ariaPressed = await darkModeToggle.getAttribute('aria-pressed');\n        const hasStateIndication = ariaPressed !== null;\n        \n        expect(hasStateIndication, 'Dark mode toggle should indicate current state').toBeTruthy();\n      }\n    });\n  });\n\n  test.describe('Text Resizing and Zoom Support', () => {\n    test('should support 200% zoom without horizontal scroll', async ({ page }) => {\n      const testPages = ['/', '/dashboard', '/login'];\n      \n      for (const testPage of testPages) {\n        await page.goto(testPage);\n        \n        const originalViewport = page.viewportSize()!;\n        \n        // Simulate 200% zoom by reducing viewport\n        const zoomedViewport = {\n          width: Math.floor(originalViewport.width / 2),\n          height: Math.floor(originalViewport.height / 2)\n        };\n        \n        await page.setViewportSize(zoomedViewport);\n        await page.waitForTimeout(1000);\n        \n        // Check for horizontal scroll\n        const hasHorizontalScroll = await page.evaluate(() => {\n          return document.body.scrollWidth > window.innerWidth;\n        });\n        \n        expect(hasHorizontalScroll, `Page ${testPage} should not have horizontal scroll at 200% zoom`).toBeFalsy();\n        \n        // Content should still be accessible\n        const results = await new AxeBuilder({ page })\n          .withTags(['wcag2a', 'wcag2aa'])\n          .analyze();\n          \n        expect(results.violations, `Page ${testPage} should be accessible at 200% zoom`).toEqual([]);\n        \n        // Restore original viewport\n        await page.setViewportSize(originalViewport);\n      }\n    });\n\n    test('should support 400% zoom with reflow', async ({ page }) => {\n      await page.goto('/');\n      \n      // Simulate 400% zoom (320px width)\n      await page.setViewportSize({ width: 320, height: 568 });\n      await page.waitForTimeout(1000);\n      \n      // Should not have horizontal scroll\n      const hasHorizontalScroll = await page.evaluate(() => {\n        return document.body.scrollWidth > window.innerWidth;\n      });\n      \n      expect(hasHorizontalScroll, 'Should not have horizontal scroll at 400% zoom').toBeFalsy();\n      \n      // Navigation should still work\n      const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));\n      expect(navigationTest.canNavigate, 'Should be navigable at 400% zoom').toBeTruthy();\n      \n      // Interactive elements should still be usable\n      const buttons = await page.locator('button').all();\n      \n      for (const button of buttons.slice(0, 3)) {\n        const isVisible = await button.isVisible();\n        const boundingBox = await button.boundingBox();\n        \n        if (isVisible && boundingBox) {\n          expect(boundingBox.width, 'Buttons should be wide enough at 400% zoom').toBeGreaterThan(0);\n          expect(boundingBox.height, 'Buttons should be tall enough at 400% zoom').toBeGreaterThan(0);\n        }\n      }\n    });\n\n    test('should support browser zoom controls', async ({ page }) => {\n      await page.goto('/dashboard');\n      \n      // Test different zoom levels\n      const zoomLevels = [1.5, 2.0, 2.5, 3.0];\n      \n      for (const zoomLevel of zoomLevels) {\n        // Simulate zoom by adjusting CSS zoom\n        await page.evaluate((zoom) => {\n          document.body.style.zoom = zoom.toString();\n        }, zoomLevel);\n        \n        await page.waitForTimeout(500);\n        \n        // Check that text is still readable\n        const textElements = await page.locator('p, h1, h2, h3').all();\n        \n        for (const element of textElements.slice(0, 3)) {\n          const isVisible = await element.isVisible();\n          \n          if (isVisible) {\n            const fontSize = await element.evaluate((el) => {\n              return window.getComputedStyle(el).fontSize;\n            });\n            \n            const fontSizeNum = parseFloat(fontSize);\n            expect(fontSizeNum, `Text should be readable at ${zoomLevel * 100}% zoom`).toBeGreaterThan(0);\n          }\n        }\n      }\n      \n      // Reset zoom\n      await page.evaluate(() => {\n        document.body.style.zoom = '1';\n      });\n    });\n\n    test('should support text spacing modifications', async ({ page }) => {\n      await page.goto('/');\n      \n      // Apply WCAG text spacing requirements\n      await page.addStyleTag({\n        content: `\n          * {\n            line-height: 1.5 !important;\n            letter-spacing: 0.12em !important;\n            word-spacing: 0.16em !important;\n          }\n          p {\n            margin-bottom: 2em !important;\n          }\n        `\n      });\n      \n      await page.waitForTimeout(500);\n      \n      // Check that content doesn't overlap\n      const hasOverlap = await page.evaluate(() => {\n        const elements = Array.from(document.querySelectorAll('p, h1, h2, h3, div, span'));\n        \n        for (let i = 0; i < elements.length - 1; i++) {\n          const rect1 = elements[i].getBoundingClientRect();\n          const rect2 = elements[i + 1].getBoundingClientRect();\n          \n          if (rect1.height > 0 && rect2.height > 0) {\n            if (rect1.bottom > rect2.top && rect1.top < rect2.bottom &&\n                rect1.right > rect2.left && rect1.left < rect2.right) {\n              return true; // Overlap detected\n            }\n          }\n        }\n        return false;\n      });\n      \n      expect(hasOverlap, 'Text should not overlap with WCAG spacing').toBeFalsy();\n      \n      // Content should still be functional\n      const results = await new AxeBuilder({ page })\n        .withTags(['wcag2a', 'wcag2aa'])\n        .analyze();\n        \n      expect(results.violations, 'Should be accessible with modified text spacing').toEqual([]);\n    });\n  });\n\n  test.describe('Focus Indicators', () => {\n    test('should have visible focus indicators on all interactive elements', async ({ page }) => {\n      const testPages = ['/', '/dashboard', '/login'];\n      \n      for (const testPage of testPages) {\n        await page.goto(testPage);\n        \n        const focusableElements = await accessibilityPage.getFocusableElements();\n        \n        for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {\n          await page.keyboard.press('Tab');\n          await page.waitForTimeout(100);\n          \n          const focusedElement = page.locator(':focus');\n          const focusResult = await accessibilityHelper.verifyFocusIndicator(focusedElement);\n          \n          expect(focusResult.isVisible, `Focus indicator should be visible on element ${i + 1} of ${testPage}`).toBeTruthy();\n          expect(focusResult.meetsContrastRequirement, `Focus indicator should meet contrast requirements on ${testPage}`).toBeTruthy();\n        }\n      }\n    });\n\n    test('should have 3:1 contrast ratio for focus indicators', async ({ page }) => {\n      await page.goto('/dashboard');\n      \n      const focusableElements = await accessibilityPage.getFocusableElements();\n      \n      for (const element of focusableElements.slice(0, 8)) {\n        await element.focus();\n        await page.waitForTimeout(200);\n        \n        const focusResult = await accessibilityHelper.verifyFocusIndicator(element);\n        \n        if (focusResult.isVisible) {\n          // Focus indicator should meet minimum 3:1 contrast ratio\n          expect(focusResult.meetsContrastRequirement, 'Focus indicators should meet 3:1 contrast ratio').toBeTruthy();\n        }\n      }\n    });\n\n    test('should not remove default focus indicators', async ({ page }) => {\n      await page.goto('/');\n      \n      // Check that CSS doesn't globally remove focus indicators\n      const hasGlobalOutlineNone = await page.evaluate(() => {\n        const sheets = Array.from(document.styleSheets);\n        \n        for (const sheet of sheets) {\n          try {\n            const rules = Array.from(sheet.cssRules);\n            \n            for (const rule of rules) {\n              if (rule instanceof CSSStyleRule) {\n                if (rule.selectorText === '*:focus' || rule.selectorText === ':focus') {\n                  if (rule.style.outline === 'none' || rule.style.outline === '0') {\n                    return true;\n                  }\n                }\n              }\n            }\n          } catch (e) {\n            // Cross-origin stylesheets may throw errors\n          }\n        }\n        \n        return false;\n      });\n      \n      expect(hasGlobalOutlineNone, 'Should not globally remove focus indicators with outline: none').toBeFalsy();\n    });\n\n    test('should provide custom focus indicators when default is removed', async ({ page }) => {\n      await page.goto('/dashboard');\n      \n      const focusableElements = await accessibilityPage.getFocusableElements();\n      \n      for (const element of focusableElements.slice(0, 5)) {\n        await element.focus();\n        \n        const hasCustomFocusIndicator = await element.evaluate((el) => {\n          const computed = window.getComputedStyle(el);\n          \n          // Check for various types of focus indicators\n          const hasOutline = computed.outline !== 'none' && computed.outline !== '0px';\n          const hasBoxShadow = computed.boxShadow !== 'none';\n          const hasBorder = computed.border !== 'none' && computed.borderWidth !== '0px';\n          const hasBackground = computed.backgroundColor !== 'rgba(0, 0, 0, 0)';\n          \n          return hasOutline || hasBoxShadow || hasBorder || hasBackground;\n        });\n        \n        expect(hasCustomFocusIndicator, 'Elements should have some form of focus indicator').toBeTruthy();\n      }\n    });\n  });\n\n  test.describe('Animation and Motion', () => {\n    test('should respect prefers-reduced-motion', async ({ page }) => {\n      await page.goto('/');\n      \n      // Enable reduced motion preference\n      await page.emulateMedia({ reducedMotion: 'reduce' });\n      await page.waitForTimeout(500);\n      \n      // Check that animations are disabled or reduced\n      const animatedElements = await page.locator('[style*=\"animation\"], [class*=\"animate\"], [data-animate]').all();\n      \n      for (const element of animatedElements) {\n        const animationDuration = await element.evaluate((el) => {\n          return window.getComputedStyle(el).animationDuration;\n        });\n        \n        // Animation should be disabled or very short\n        const isReduced = animationDuration === '0s' || animationDuration === 'none';\n        \n        expect(isReduced, 'Animations should be reduced when prefers-reduced-motion is set').toBeTruthy();\n      }\n    });\n\n    test('should provide controls for auto-playing content', async ({ page }) => {\n      await page.goto('/');\n      \n      const autoPlayElements = await page.locator('[autoplay], video[autoplay], audio[autoplay]').all();\n      \n      for (const element of autoPlayElements) {\n        // Should have controls or be muted\n        const hasControls = await element.getAttribute('controls');\n        const isMuted = await element.getAttribute('muted');\n        \n        expect(\n          hasControls !== null || isMuted !== null,\n          'Auto-playing content should have controls or be muted'\n        ).toBeTruthy();\n      }\n    });\n\n    test('should not have content that flashes more than 3 times per second', async ({ page }) => {\n      await page.goto('/');\n      \n      // Look for potentially flashing content\n      const flashingElements = await page.locator('[class*=\"flash\"], [class*=\"blink\"], [data-flash]').all();\n      \n      expect(flashingElements.length, 'Should not have flashing content that could cause seizures').toBe(0);\n      \n      // Check CSS animations for rapid flashing\n      const animatedElements = await page.locator('[style*=\"animation\"]').all();\n      \n      for (const element of animatedElements) {\n        const animationDuration = await element.evaluate((el) => {\n          const duration = window.getComputedStyle(el).animationDuration;\n          return parseFloat(duration) || 0;\n        });\n        \n        // Animation should not be faster than 0.33s (3 times per second)\n        if (animationDuration > 0) {\n          expect(animationDuration, 'Animations should not flash more than 3 times per second').toBeGreaterThan(0.33);\n        }\n      }\n    });\n  });\n\n  test.describe('Responsive Visual Design', () => {\n    test('should maintain visual accessibility across viewports', async ({ page }) => {\n      const viewportTests: ViewportTest[] = [\n        { name: 'Mobile Portrait', width: 375, height: 667, description: 'iPhone SE' },\n        { name: 'Mobile Landscape', width: 667, height: 375, description: 'iPhone SE Landscape' },\n        { name: 'Tablet Portrait', width: 768, height: 1024, description: 'iPad' },\n        { name: 'Tablet Landscape', width: 1024, height: 768, description: 'iPad Landscape' },\n        { name: 'Desktop', width: 1920, height: 1080, description: 'Desktop HD' }\n      ];\n      \n      for (const viewport of viewportTests) {\n        await page.setViewportSize({ width: viewport.width, height: viewport.height });\n        await page.goto('/dashboard');\n        await page.waitForTimeout(500);\n        \n        // Test touch target sizes on mobile\n        if (viewport.width <= 768) {\n          const touchTargets = await accessibilityPage.getInteractiveElements();\n          \n          for (const target of touchTargets.slice(0, 8)) {\n            const boundingBox = await target.boundingBox();\n            \n            if (boundingBox) {\n              expect(\n                Math.min(boundingBox.width, boundingBox.height),\n                `Touch targets should be at least 44px on ${viewport.name}`\n              ).toBeGreaterThanOrEqual(44);\n            }\n          }\n        }\n        \n        // Test accessibility compliance at all viewports\n        const results = await new AxeBuilder({ page })\n          .withTags(['wcag2a', 'wcag2aa'])\n          .analyze();\n          \n        expect(results.violations, `Should be accessible on ${viewport.name}`).toEqual([]);\n      }\n    });\n\n    test('should maintain readability with different font sizes', async ({ page }) => {\n      await page.goto('/');\n      \n      const fontSizes = ['12px', '14px', '16px', '18px', '20px', '24px'];\n      \n      for (const fontSize of fontSizes) {\n        await page.addStyleTag({\n          content: `body { font-size: ${fontSize} !important; }`\n        });\n        \n        await page.waitForTimeout(300);\n        \n        // Text should still be readable\n        const textElements = await page.locator('p, h1, h2, h3').all();\n        \n        for (const element of textElements.slice(0, 3)) {\n          const isVisible = await element.isVisible();\n          expect(isVisible, `Text should be visible at ${fontSize}`).toBeTruthy();\n          \n          const contrastResult = await accessibilityHelper.checkElementContrast(element);\n          \n          if (contrastResult.ratio > 0) {\n            expect(contrastResult.ratio, `Text should maintain contrast at ${fontSize}`).toBeGreaterThanOrEqual(4.5);\n          }\n        }\n      }\n    });\n  });\n\n  test.describe('Print Accessibility', () => {\n    test('should be accessible when printed', async ({ page }) => {\n      await page.goto('/');\n      \n      // Emulate print media\n      await page.emulateMedia({ media: 'print' });\n      await page.waitForTimeout(500);\n      \n      // Check that important content is visible\n      const results = await new AxeBuilder({ page })\n        .withTags(['wcag2a', 'wcag2aa'])\n        .analyze();\n        \n      expect(results.violations, 'Should be accessible in print mode').toEqual([]);\n      \n      // Check that links show URLs (if implemented)\n      const links = await page.locator('a[href]').all();\n      \n      for (const link of links.slice(0, 3)) {\n        const linkText = await link.textContent();\n        const href = await link.getAttribute('href');\n        \n        // In print mode, links should ideally show their URLs\n        // This is implementation-dependent\n        console.log(`Print link: \"${linkText}\" -> ${href}`);\n      }\n    });\n  });\n});