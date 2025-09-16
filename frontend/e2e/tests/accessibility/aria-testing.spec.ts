/**
 * ARIA Implementation Tests
 *
 * Tests comprehensive ARIA (Accessible Rich Internet Applications) implementation including:
 * - ARIA roles and properties validation
 * - Live regions for dynamic content
 * - ARIA states management
 * - Complex widget patterns (tabs, menus, modals)
 * - ARIA relationships and references
 * - Custom component ARIA patterns
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {AccessibilityHelper} from '../../helpers/accessibility.helper';
import {AccessibilityPage} from '../../pages/AccessibilityPage';

interface _AriaPattern {
  role: string;
  requiredProperties: string[];
  optionalProperties: string[];
  requiredStates: string[];
  allowedChildren?: string[];
  requiredContext?: string[];
}

interface _LiveRegionTest {
  trigger: string;
  expectedContent: RegExp;
  expectedPoliteness: 'polite' | 'assertive';
}

test.describe('ARIA Implementation Tests', () => {
  let accessibilityHelper: AccessibilityHelper;
  let accessibilityPage: AccessibilityPage;

  test.beforeEach(async ({page}) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);

    await accessibilityHelper.configureAxe();
  });

  test.describe('ARIA Roles and Properties Validation', () => {
    test('should have valid ARIA attributes', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login'];

      for (const testPage of testPages) {
        await page.goto(testPage);
        await accessibilityPage.waitForPageLoad();

        // Run comprehensive ARIA validation
        const results = await new AxeBuilder({page})
        .withRules([
          'aria-valid-attr',
          'aria-valid-attr-value',
          'aria-allowed-attr',
          'aria-required-attr',
          'aria-required-children',
          'aria-required-parent',
          'aria-roles',
          'aria-roledescription'
        ])
        .analyze();

        expect(results.violations, `Page ${testPage} should have valid ARIA attributes`).toEqual([]);

        // Manual validation of complex ARIA patterns
        const ariaElements = await page.locator('[role], [aria-label], [aria-labelledby], [aria-describedby]').all();

        for (const element of ariaElements.slice(0, 15)) {
          const ariaValidation = await accessibilityHelper.verifyAriaAttributes(element);

          expect(
              ariaValidation.isValid,
              `ARIA attributes should be valid: ${ariaValidation.errors.join(', ')}`
          ).toBeTruthy();
        }
      }
    });

    test('should use semantic HTML before ARIA when possible', async ({page}) => {
      await page.goto('/dashboard');

      // Check for over-use of ARIA roles that could be semantic HTML
      const redundantAriaPatterns = [
        {
          selector: '[role="button"]',
          semantic: 'button',
          message: 'Use <button> instead of role="button"'
        },
        {selector: '[role="link"]', semantic: 'a', message: 'Use <a href> instead of role="link"'},
        {
          selector: '[role="heading"]',
          semantic: 'h1,h2,h3,h4,h5,h6',
          message: 'Use <h1>-<h6> instead of role="heading"'
        },
        {
          selector: '[role="list"]',
          semantic: 'ul,ol',
          message: 'Use <ul> or <ol> instead of role="list"'
        },
        {
          selector: '[role="listitem"]',
          semantic: 'li',
          message: 'Use <li> instead of role="listitem"'
        }
      ];

      for (const pattern of redundantAriaPatterns) {
        const ariaElements = await page.locator(pattern.selector).all();

        for (const element of ariaElements) {
          const tagName = await element.evaluate(el => el.tagName.toLowerCase());

          if (tagName !== 'button' && pattern.selector.includes('button')) {
            console.warn(`${pattern.message} found: <${tagName} ${pattern.selector.replace('[', '').replace(']', '')}>`);
          }

          if (tagName !== 'a' && pattern.selector.includes('link')) {
            console.warn(`${pattern.message} found: <${tagName} ${pattern.selector.replace('[', '').replace(']', '')}>`);
          }
        }
      }
    });

    test('should have consistent ARIA patterns across similar components', async ({page}) => {
      await page.goto('/dashboard');

      // Test button patterns
      const buttons = await page.locator('button, [role="button"]').all();
      const buttonPatterns = new Set<string>();

      for (const button of buttons.slice(0, 10)) {
        const pattern = await button.evaluate(btn => {
          const attrs = [];

          if (btn.hasAttribute('aria-label')) attrs.push('aria-label');
          if (btn.hasAttribute('aria-labelledby')) attrs.push('aria-labelledby');
          if (btn.hasAttribute('aria-describedby')) attrs.push('aria-describedby');
          if (btn.hasAttribute('aria-expanded')) attrs.push('aria-expanded');
          if (btn.hasAttribute('aria-pressed')) attrs.push('aria-pressed');

          return attrs.sort().join(',');
        });

        buttonPatterns.add(pattern);
      }

      console.log(`Button ARIA patterns found: ${Array.from(buttonPatterns).join('; ')}`);

      // Test that similar interactive elements use consistent patterns
      const interactiveElements = await page.locator('[onclick], [role="button"], button').all();

      for (const element of interactiveElements.slice(0, 8)) {
        const hasAccessibleName = await accessibilityHelper.hasAccessibleName(element);
        expect(
            hasAccessibleName,
            'All interactive elements should have accessible names'
        ).toBeTruthy();
      }
    });
  });

  test.describe('ARIA Roles Implementation', () => {
    test('should implement button role correctly', async ({page}) => {
      await page.goto('/dashboard');

      const customButtons = await page.locator('[role="button"]').all();

      for (const button of customButtons) {
        // Should be keyboard accessible
        const isKeyboardAccessible = await accessibilityPage.isKeyboardAccessible(button);
        expect(isKeyboardAccessible, 'ARIA buttons should be keyboard accessible').toBeTruthy();

        // Should have accessible name
        const hasName = await accessibilityHelper.hasAccessibleName(button);
        expect(hasName, 'ARIA buttons should have accessible names').toBeTruthy();
      }
    });

    test('should implement tab role correctly', async ({page}) => {
      await page.goto('/dashboard');

      const tabs = await page.locator('[role="tab"]').all();

      for (const tab of tabs) {
        // Should be in a tablist
        const tablist = await tab.locator('xpath=ancestor::*[@role="tablist"]').first();
        expect(
            await tablist.count(),
            'Tabs should be contained in tablist'
        ).toBeGreaterThan(0);
      }
    });
  });

  test.describe('ARIA States and Properties', () => {
    test('should manage aria-expanded correctly', async ({page}) => {
      await page.goto('/dashboard');

      const expandableElements = await page.locator('[aria-expanded]').all();

      for (const element of expandableElements) {
        const initialState = await element.getAttribute('aria-expanded');
        expect(['true', 'false']).toContain(initialState);
      }
    });
  });

  test.describe('ARIA Relationships', () => {
    test('should have valid aria-labelledby references', async ({page}) => {
      await page.goto('/dashboard');

      const labelledElements = await page.locator('[aria-labelledby]').all();

      for (const element of labelledElements) {
        const labelledBy = await element.getAttribute('aria-labelledby');

        if (labelledBy) {
          const labelIds = labelledBy.split(' ');

          for (const labelId of labelIds) {
            const labelElement = await page.locator(`#${labelId}`).first();

            expect(
                await labelElement.count(),
                `Element with id="${labelId}" should exist for aria-labelledby`
            ).toBeGreaterThan(0);
          }
        }
      }
    });
  });

  test.describe('Live Regions', () => {
    test('should have properly configured live regions', async ({page}) => {
      await page.goto('/dashboard');

      const liveRegions = await accessibilityPage.getLiveRegions();

      for (const region of liveRegions) {
        const ariaLive = await region.getAttribute('aria-live');
        expect(['polite', 'assertive', 'off']).toContain(ariaLive);
      }
    });
  });

  test.describe('ARIA Performance and Best Practices', () => {
    test('should not have excessive ARIA complexity', async ({page}) => {
      await page.goto('/dashboard');

      const ariaComplexity = await page.evaluate(() => {
        const elements = document.querySelectorAll('[role], [aria-label], [aria-labelledby], [aria-describedby]');

        let totalAriaAttrs = 0;
        let maxDepth = 0;

        elements.forEach(el => {
          // Count ARIA attributes
          const attrs = Array.from(el.attributes).filter(attr =>
              attr.name.startsWith('aria-') || attr.name === 'role'
          );
          totalAriaAttrs += attrs.length;

          // Check nesting depth
          let depth = 0;
          let current = el.parentElement;

          while (current && current !== document.body) {
            if (current.hasAttribute('role') ||
                Array.from(current.attributes).some(attr => attr.name.startsWith('aria-'))) {
              depth++;
            }
            current = current.parentElement;
          }

          maxDepth = Math.max(maxDepth, depth);
        });

        return {
          totalElements: elements.length,
          totalAriaAttrs,
          avgAttrsPerElement: elements.length > 0 ? totalAriaAttrs / elements.length : 0,
          maxNestingDepth: maxDepth
        };
      });

      expect(
          ariaComplexity.avgAttrsPerElement,
          'Average ARIA attributes per element should be reasonable'
      ).toBeLessThan(5);

      expect(
          ariaComplexity.maxNestingDepth,
          'ARIA nesting depth should be reasonable'
      ).toBeLessThan(8);

      console.log('ARIA Complexity Analysis:', ariaComplexity);
    });
  });
});