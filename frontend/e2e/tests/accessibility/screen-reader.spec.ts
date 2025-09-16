/**
 * Screen Reader Compatibility Tests
 *
 * Tests compatibility with major screen readers:
 * - NVDA (Windows) - 65.6% market share
 * - JAWS (Windows) - 60.5% market share
 * - VoiceOver (macOS/iOS) - Primary for Apple users
 * - TalkBack (Android) - Mobile screen reader
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {AccessibilityHelper} from '../../helpers/accessibility.helper';
import {AccessibilityPage} from '../../pages/AccessibilityPage';

interface _ScreenReaderTest {
  name: string;
  elements: string[];
  expectedAnnouncements: string[];
}

test.describe('Screen Reader Compatibility Tests', () => {
  let accessibilityHelper: AccessibilityHelper;
  let accessibilityPage: AccessibilityPage;

  test.beforeEach(async ({page}) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);

    await accessibilityHelper.configureAxe();
  });

  test.describe('ARIA Implementation', () => {
    test('should have proper ARIA labels for all interactive elements', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login'];

      for (const testPage of testPages) {
        await page.goto(testPage);
        await accessibilityPage.waitForPageLoad();

        const results = await new AxeBuilder({page})
        .withRules([
          'aria-valid-attr',
          'aria-valid-attr-value',
          'aria-required-attr',
          'aria-allowed-attr',
          'button-name',
          'link-name',
          'input-button-name'
        ])
        .analyze();

        expect(results.violations, `Page ${testPage} should have proper ARIA labels`).toEqual([]);

        // Test that all interactive elements have accessible names
        const interactiveElements = await accessibilityPage.getInteractiveElements();

        for (const element of interactiveElements.slice(0, 15)) {
          const accessibleName = await accessibilityHelper.getAccessibleName(element);
          expect(accessibleName.length, 'Interactive elements should have accessible names').toBeGreaterThan(0);
        }
      }
    });

    test('should have proper ARIA roles and properties', async ({page}) => {
      await page.goto('/dashboard');

      const results = await new AxeBuilder({page})
      .withRules([
        'aria-roles',
        'aria-required-children',
        'aria-required-parent'
      ])
      .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should have proper ARIA states and properties', async ({page}) => {
      await page.goto('/dashboard');

      // Test dynamic ARIA states
      const toggleButtons = await page.locator('[aria-expanded]').all();

      for (const button of toggleButtons.slice(0, 5)) {
        const initialState = await button.getAttribute('aria-expanded');
        expect(['true', 'false']).toContain(initialState);

        // Click to toggle state
        await button.click();
        await page.waitForTimeout(500);

        const newState = await button.getAttribute('aria-expanded');
        expect(['true', 'false']).toContain(newState);
        expect(newState).not.toBe(initialState);
      }
    });

    test('should have proper form ARIA attributes', async ({page}) => {
      const formPages = ['/login', '/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        // Test aria-required on required fields
        const requiredFields = await accessibilityPage.getRequiredFields();

        for (const field of requiredFields) {
          const ariaRequired = await field.getAttribute('aria-required');
          const isRequired = await field.getAttribute('required');

          expect(ariaRequired === 'true' || isRequired !== null,
              'Required fields should have aria-required or required attribute').toBeTruthy();
        }

        // Test aria-describedby for field descriptions and errors
        const describedFields = await page.locator('[aria-describedby]').all();

        for (const field of describedFields) {
          const describedBy = await field.getAttribute('aria-describedby');
          if (describedBy) {
            const referencedElements = describedBy.split(' ');

            for (const id of referencedElements) {
              const referencedElement = page.locator(`#${id}`);
              await expect(referencedElement, `Element with id ${id} should exist`).toBeAttached();
            }
          }
        }
      }
    });
  });

  test.describe('Semantic HTML Structure', () => {
    test('should use proper heading hierarchy', async ({page}) => {
      const testPages = ['/', '/dashboard', '/profile'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        const headings = await accessibilityPage.getHeadingStructure();

        // Should have exactly one h1
        expect(headings.h1.length, `Page ${testPage} should have exactly one h1`).toBe(1);

        // Verify logical heading order
        const isLogicalOrder = await accessibilityHelper.verifyHeadingOrder(headings);
        expect(isLogicalOrder, `Page ${testPage} should have logical heading hierarchy`).toBeTruthy();

        // Headings should not be empty
        for (const [level, headingElements] of Object.entries(headings)) {
          for (const heading of headingElements) {
            const text = await heading.textContent();
            expect(text?.trim().length, `${level} headings should not be empty`).toBeGreaterThan(0);
          }
        }
      }
    });

    test('should have proper landmark structure', async ({page}) => {
      const testPages = ['/', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        const landmarks = await accessibilityPage.getLandmarks();

        // Should have main landmark
        expect(landmarks.main.length, `Page ${testPage} should have main landmark`).toBeGreaterThanOrEqual(1);
        expect(landmarks.main.length, `Page ${testPage} should have only one main landmark`).toBeLessThanOrEqual(1);

        // Should have navigation landmarks
        expect(landmarks.navigation.length, `Page ${testPage} should have navigation landmarks`).toBeGreaterThanOrEqual(1);

        // Verify landmark labels when multiple of same type exist
        const landmarkLabelsValid = await accessibilityHelper.verifyLandmarkLabels(landmarks);
        expect(landmarkLabelsValid, `Page ${testPage} landmarks should have proper labels`).toBeTruthy();
      }
    });

    test('should use semantic form elements', async ({page}) => {
      const formPages = ['/login', '/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        // Check that form controls are properly associated with labels
        const results = await new AxeBuilder({page})
        .withRules(['label', 'form-field-multiple-labels'])
        .analyze();

        expect(results.violations, `Page ${formPage} forms should have proper labels`).toEqual([]);

        // Check for proper fieldset/legend usage for grouped fields
        const fieldsets = await page.locator('fieldset').all();

        for (const fieldset of fieldsets) {
          const legend = fieldset.locator('legend').first();
          await expect(legend, 'Fieldsets should have legends').toBeAttached();

          const legendText = await legend.textContent();
          expect(legendText?.trim().length, 'Legend should not be empty').toBeGreaterThan(0);
        }
      }
    });

    test('should use semantic list structures', async ({page}) => {
      const testPages = ['/', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        const results = await new AxeBuilder({page})
        .withRules(['list', 'listitem'])
        .analyze();

        expect(results.violations, `Page ${testPage} should use proper list structures`).toEqual([]);
      }
    });

    test('should use semantic table structures', async ({page}) => {
      await page.goto('/analytics'); // Assuming analytics page has tables

      const tables = await page.locator('table').all();

      for (const table of tables) {
        // Should have proper header structure
        const headers = await table.locator('th').all();
        expect(headers.length, 'Tables should have header cells').toBeGreaterThan(0);

        // Check scope attributes on headers
        for (const header of headers) {
          const scope = await header.getAttribute('scope');
          if (scope) {
            expect(['col', 'row', 'colgroup', 'rowgroup']).toContain(scope);
          }
        }

        // Check for table caption
        const caption = table.locator('caption').first();
        if (await caption.count() > 0) {
          const captionText = await caption.textContent();
          expect(captionText?.trim().length, 'Table captions should not be empty').toBeGreaterThan(0);
        }
      }
    });
  });

  test.describe('Live Regions and Dynamic Content', () => {
    test('should announce dynamic content changes', async ({page}) => {
      await page.goto('/dashboard');

      // Find live regions
      const liveRegions = await accessibilityPage.getLiveRegions();

      for (const region of liveRegions) {
        const ariaLive = await region.getAttribute('aria-live');
        expect(['polite', 'assertive', 'off']).toContain(ariaLive);

        // Test that region is not empty when visible
        if (await region.isVisible()) {
          const hasContent = await region.evaluate((el) => {
            return el.textContent?.trim().length > 0 || el.children.length > 0;
          });

          if (hasContent) {
            const content = await region.textContent();
            expect(content?.trim().length, 'Live regions with content should not be empty').toBeGreaterThan(0);
          }
        }
      }
    });

    test('should have proper status messages', async ({page}) => {
      await page.goto('/dashboard');

      // Look for status regions
      const statusRegions = await page.locator('[role="status"], [role="alert"], [aria-live="polite"], [aria-live="assertive"]').all();

      for (const region of statusRegions) {
        const role = await region.getAttribute('role');
        const ariaLive = await region.getAttribute('aria-live');

        const hasProperAnnouncement = role === 'status' ||
            role === 'alert' ||
            ariaLive === 'polite' ||
            ariaLive === 'assertive';

        expect(hasProperAnnouncement, 'Status messages should be properly announced').toBeTruthy();
      }
    });

    test('should handle loading states accessibly', async ({page}) => {
      await page.goto('/dashboard');

      // Look for loading indicators
      const loadingElements = await page.locator('[aria-busy="true"], [data-loading], .loading').all();

      for (const loader of loadingElements) {
        const ariaBusy = await loader.getAttribute('aria-busy');
        const hasLabel = await accessibilityHelper.hasAccessibleName(loader);

        expect(ariaBusy === 'true' || hasLabel, 'Loading indicators should be announced').toBeTruthy();
      }
    });

    test('should announce form validation errors', async ({page}) => {
      await page.goto('/login');

      // Submit form to trigger validation
      const submitButton = page.locator('button[type="submit"]').first();
      await submitButton.click();
      await page.waitForTimeout(1000);

      const errorMessages = await accessibilityPage.getErrorMessages();

      for (const error of errorMessages) {
        const role = await error.getAttribute('role');
        const ariaLive = await error.getAttribute('aria-live');

        const isAnnounced = role === 'alert' ||
            ariaLive === 'assertive' ||
            ariaLive === 'polite';

        expect(isAnnounced, 'Error messages should be announced to screen readers').toBeTruthy();
      }
    });
  });

  test.describe('Navigation and Reading Order', () => {
    test('should have logical reading order', async ({page}) => {
      const testPages = ['/', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        // Test reading order by disabling CSS
        await page.addStyleTag({content: '* { all: unset !important; display: block !important; }'});

        const headings = await accessibilityPage.getHeadingStructure();
        const allHeadings: Array<{ level: number; text: string; element: unknown }> = [];

        for (const [level, headingElements] of Object.entries(headings)) {
          const levelNumber = parseInt(level.substring(1));

          for (const heading of headingElements) {
            const text = await heading.textContent();
            allHeadings.push({level: levelNumber, text: text || '', element: heading});
          }
        }

        // Reading order should be logical (no major level skips)
        let previousLevel = 0;
        let hasLogicalOrder = true;

        for (const heading of allHeadings) {
          if (heading.level > previousLevel + 1 && previousLevel > 0) {
            hasLogicalOrder = false;
            break;
          }
          previousLevel = Math.max(previousLevel, heading.level);
        }

        expect(hasLogicalOrder, `Page ${testPage} should have logical reading order`).toBeTruthy();
      }
    });

    test('should support screen reader navigation shortcuts', async ({page}) => {
      await page.goto('/dashboard');

      // Test heading navigation (H key in screen readers)
      const headings = await page.locator('h1, h2, h3, h4, h5, h6').all();
      expect(headings.length, 'Page should have headings for navigation').toBeGreaterThan(0);

      // Test landmark navigation (R key in screen readers)  
      const landmarks = await page.locator('main, nav, aside, header, footer, [role="main"], [role="navigation"], [role="banner"], [role="contentinfo"], [role="complementary"]').all();
      expect(landmarks.length, 'Page should have landmarks for navigation').toBeGreaterThan(0);

      // Test form navigation (F key in screen readers)
      const forms = await page.locator('form').all();
      if (forms.length > 0) {
        for (const form of forms) {
          const formControls = await form.locator('input, select, textarea, button').all();
          expect(formControls.length, 'Forms should have form controls').toBeGreaterThan(0);
        }
      }

      // Test link navigation (K key in screen readers)
      const links = await page.locator('a[href]').all();
      expect(links.length, 'Page should have links for navigation').toBeGreaterThan(0);
    });

    test('should provide skip navigation links', async ({page}) => {
      await page.goto('/');

      const skipLink = await accessibilityPage.getSkipLink();
      expect(skipLink, 'Page should have skip navigation link').toBeTruthy();

      if (skipLink) {
        // Skip link should become visible on focus
        await page.keyboard.press('Tab');
        await expect(skipLink).toBeVisible();

        // Skip link should navigate to main content
        await skipLink.click();

        const mainContent = await accessibilityPage.getMainContent();
        await expect(mainContent).toBeFocused();
      }
    });
  });

  test.describe('Screen Reader Specific Features', () => {
    test('should work with NVDA screen reader patterns', async ({page}) => {
      await page.goto('/dashboard');

      // Test NVDA-specific features
      const results = await new AxeBuilder({page})
      .withRules([
        'aria-valid-attr',
        'button-name',
        'link-name',
        'image-alt',
        'label'
      ])
      .analyze();

      expect(results.violations, 'Should work with NVDA').toEqual([]);

      // Test browse mode navigation elements
      const navigableElements = await page.locator('h1, h2, h3, h4, h5, h6, a, button, input, select, textarea, [role="button"], [role="link"]').all();
      expect(navigableElements.length, 'Should have elements for NVDA browse mode').toBeGreaterThan(0);
    });

    test('should work with JAWS screen reader patterns', async ({page}) => {
      await page.goto('/dashboard');

      // Test JAWS-specific requirements
      const results = await new AxeBuilder({page})
      .withRules([
        'th-has-data-cells',
        'td-headers-attr',
        'scope-attr-valid',
        'aria-required-attr'
      ])
      .analyze();

      expect(results.violations, 'Should work with JAWS').toEqual([]);

      // Test table navigation if tables exist
      const tables = await page.locator('table').all();

      for (const table of tables) {
        const headers = await table.locator('th').all();

        for (const header of headers) {
          const hasProperAssociation = await header.evaluate((th) => {
            const scope = th.getAttribute('scope');
            const id = th.getAttribute('id');

            if (scope) return ['col', 'row', 'colgroup', 'rowgroup'].includes(scope);
            if (id) {
              const cells = document.querySelectorAll(`td[headers*="${id}"]`);
              return cells.length > 0;
            }

            return false;
          });

          expect(hasProperAssociation, 'Table headers should be properly associated for JAWS').toBeTruthy();
        }
      }
    });

    test('should work with VoiceOver patterns', async ({page}) => {
      await page.goto('/dashboard');

      // Test VoiceOver-specific features (rotor navigation)
      const results = await new AxeBuilder({page})
      .withRules([
        'heading-order',
        'landmark-one-main',
        'region'
      ])
      .analyze();

      expect(results.violations, 'Should work with VoiceOver').toEqual([]);

      // Test rotor categories
      const rotorElements = {
        headings: await page.locator('h1, h2, h3, h4, h5, h6').count(),
        links: await page.locator('a[href]').count(),
        formControls: await page.locator('input, select, textarea, button').count(),
        landmarks: await page.locator('main, nav, aside, header, footer, [role]').count()
      };

      expect(rotorElements.headings, 'Should have headings for VoiceOver rotor').toBeGreaterThan(0);
      expect(rotorElements.landmarks, 'Should have landmarks for VoiceOver rotor').toBeGreaterThan(0);
    });

    test('should announce button and link purposes clearly', async ({page}) => {
      const testPages = ['/', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        // Test buttons have clear purposes
        const buttons = await page.locator('button, [role="button"]').all();

        for (const button of buttons.slice(0, 10)) {
          const name = await accessibilityHelper.getAccessibleName(button);

          expect(name.length, 'Buttons should have accessible names').toBeGreaterThan(0);

          // Avoid vague button text
          const vaguePhrases = ['click', 'button', 'here', 'more'];
          const isVague = vaguePhrases.some(phrase =>
              name.toLowerCase().trim() === phrase
          );

          expect(isVague, `Button text "${name}" should be descriptive`).toBeFalsy();
        }

        // Test links have clear purposes
        const links = await page.locator('a[href]').all();

        for (const link of links.slice(0, 10)) {
          const name = await accessibilityHelper.getAccessibleName(link);

          expect(name.length, 'Links should have accessible names').toBeGreaterThan(0);

          // Avoid vague link text
          const vaguePhrases = ['click here', 'read more', 'more', 'here', 'link'];
          const isVague = vaguePhrases.some(phrase =>
              name.toLowerCase().trim() === phrase
          );

          expect(isVague, `Link text "${name}" should be descriptive`).toBeFalsy();
        }
      }
    });
  });

  test.describe('Content Comprehension', () => {
    test('should provide context for form fields', async ({page}) => {
      const formPages = ['/login', '/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        const inputs = await page.locator('input, select, textarea').all();

        for (const input of inputs.slice(0, 10)) {
          const label = await accessibilityHelper.getAccessibleName(input);
          expect(label.length, 'Form fields should have labels').toBeGreaterThan(0);

          // Check for additional description if available
          const describedBy = await input.getAttribute('aria-describedby');
          if (describedBy) {
            const description = await page.locator(`#${describedBy}`).textContent();
            expect(description?.trim().length, 'Field descriptions should not be empty').toBeGreaterThan(0);
          }
        }
      }
    });

    test('should announce field requirements and formats', async ({page}) => {
      const formPages = ['/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        const requiredFields = await accessibilityPage.getRequiredFields();

        for (const field of requiredFields) {
          // Should indicate that field is required
          const ariaRequired = await field.getAttribute('aria-required');
          const hasRequiredInLabel = await field.evaluate((el) => {
            const label = document.querySelector(`label[for="${el.id}"]`);
            return label?.textContent?.includes('*') ||
                label?.textContent?.toLowerCase().includes('required');
          });

          expect(
              ariaRequired === 'true' || hasRequiredInLabel,
              'Required fields should be announced as required'
          ).toBeTruthy();
        }
      }
    });

    test('should provide meaningful error messages', async ({page}) => {
      await page.goto('/login');

      // Submit form with invalid data
      const emailField = page.locator('input[type="email"]').first();
      if (await emailField.count() > 0) {
        await emailField.fill('invalid-email');
      }

      const submitButton = page.locator('button[type="submit"]').first();
      await submitButton.click();
      await page.waitForTimeout(1000);

      const errorMessages = await accessibilityPage.getErrorMessages();

      for (const error of errorMessages) {
        const errorText = await error.textContent();

        // Error should be meaningful
        expect(errorText?.trim().length, 'Error messages should not be empty').toBeGreaterThan(0);

        // Should provide guidance
        const hasGuidance = errorText?.toLowerCase().includes('please') ||
            errorText?.toLowerCase().includes('should') ||
            errorText?.toLowerCase().includes('must') ||
            errorText?.toLowerCase().includes('required');

        expect(hasGuidance, 'Error messages should provide guidance').toBeTruthy();
      }
    });

    test('should announce data table information clearly', async ({page}) => {
      await page.goto('/analytics'); // Assuming analytics has data tables

      const tables = await page.locator('table').all();

      for (const table of tables) {
        // Check for table caption or summary
        const caption = table.locator('caption').first();
        const summary = await table.getAttribute('summary');

        if (await caption.count() > 0) {
          const captionText = await caption.textContent();
          expect(captionText?.trim().length, 'Table captions should be descriptive').toBeGreaterThan(0);
        }

        if (summary) {
          expect(summary.trim().length, 'Table summaries should be descriptive').toBeGreaterThan(0);
        }

        // Check header associations
        const headers = await table.locator('th').all();

        for (const header of headers) {
          const headerText = await header.textContent();
          expect(headerText?.trim().length, 'Table headers should not be empty').toBeGreaterThan(0);
        }
      }
    });
  });

  test.describe('Cross-browser Screen Reader Support', () => {
    test('should work consistently across browsers', async ({page, browserName}) => {
      await page.goto('/dashboard');

      // Core accessibility features should work in all browsers
      const results = await new AxeBuilder({page})
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

      expect(results.violations, `Screen reader support should work in ${browserName}`).toEqual([]);
    });
  });

  test.describe('Performance Impact on Screen Readers', () => {
    test('should not have excessive DOM complexity', async ({page}) => {
      await page.goto('/dashboard');

      const domComplexity = await page.evaluate(() => {
        return {
          totalElements: document.getElementsByTagName('*').length,
          interactiveElements: document.querySelectorAll('a, button, input, select, textarea, [role="button"], [role="link"], [tabindex]').length,
          ariaElements: document.querySelectorAll('[aria-label], [aria-labelledby], [aria-describedby], [role]').length
        };
      });

      // These are reasonable limits for good screen reader performance
      expect(domComplexity.totalElements, 'Total DOM elements should be reasonable').toBeLessThan(5000);
      expect(domComplexity.interactiveElements, 'Interactive elements should be manageable').toBeLessThan(200);

      console.log(`DOM Complexity for screen readers:`, domComplexity);
    });

    test('should have efficient ARIA tree structure', async ({page}) => {
      await page.goto('/dashboard');

      // Check for overly nested ARIA structures that might confuse screen readers
      const deeplyNestedAria = await page.evaluate(() => {
        const ariaElements = document.querySelectorAll('[role], [aria-label], [aria-labelledby]');
        let maxDepth = 0;

        ariaElements.forEach(element => {
          let depth = 0;
          let current = element.parentElement;

          while (current && current !== document.body) {
            if (current.hasAttribute('role') ||
                current.hasAttribute('aria-label') ||
                current.hasAttribute('aria-labelledby')) {
              depth++;
            }
            current = current.parentElement;
          }

          maxDepth = Math.max(maxDepth, depth);
        });

        return maxDepth;
      });

      expect(deeplyNestedAria, 'ARIA nesting should not be excessive').toBeLessThan(10);
    });
  });
});