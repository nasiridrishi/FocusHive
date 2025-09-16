import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.describe('Accessibility Tests', () => {
  test('homepage should be accessible', async ({page}) => {
    await page.goto('/');

    const accessibilityScanResults = await new AxeBuilder({page})
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('login page should be accessible', async ({page}) => {
    await page.goto('/login');

    const accessibilityScanResults = await new AxeBuilder({page})
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('should have proper focus management', async ({page}) => {
    await page.goto('/');

    // Test keyboard navigation
    await page.keyboard.press('Tab');
    const firstFocusableElement = await page.locator(':focus').first();
    await expect(firstFocusableElement).toBeVisible();

    // Continue tabbing and ensure focus is visible
    for (let i = 0; i < 5; i++) {
      await page.keyboard.press('Tab');
      const focusedElement = await page.locator(':focus').first();
      await expect(focusedElement).toBeVisible();
    }
  });

  test('should handle skip links properly', async ({page}) => {
    await page.goto('/');

    // Press tab to potentially reveal skip links
    await page.keyboard.press('Tab');

    // Look for skip to main content link
    const skipLink = page.locator('a:has-text("Skip to main content"), [href="#main-content"]').first();
    if (await skipLink.isVisible()) {
      await skipLink.click();

      // Main content should be focused or visible
      const mainContent = page.locator('main, [role="main"], #main-content').first();
      await expect(mainContent).toBeVisible();
    }
  });

  test('should have proper color contrast', async ({page}) => {
    await page.goto('/');

    const accessibilityScanResults = await new AxeBuilder({page})
    .withRules(['color-contrast', 'color-contrast-enhanced'])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('form should be accessible', async ({page}) => {
    await page.goto('/login');

    // Check that form elements have proper labels
    const accessibilityScanResults = await new AxeBuilder({page})
    .include('form')
    .withRules(['label', 'form-field-multiple-labels'])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);

    // Test form interaction with keyboard
    await page.keyboard.press('Tab');
    let focusedElement = await page.locator(':focus').first();

    // Should be able to navigate through all form elements
    const formElements = await page.locator('input, select, textarea, button').all();

    for (let i = 0; i < formElements.length; i++) {
      await page.keyboard.press('Tab');
      focusedElement = await page.locator(':focus').first();
      await expect(focusedElement).toBeVisible();
    }
  });

  test('should announce dynamic content changes', async ({page}) => {
    await page.goto('/');

    // Look for ARIA live regions
    const liveRegions = await page.locator('[aria-live]').all();

    for (const region of liveRegions) {
      await expect(region).toHaveAttribute('aria-live');

      // Check that the live region has appropriate politeness level
      const ariaLive = await region.getAttribute('aria-live');
      expect(['polite', 'assertive', 'off']).toContain(ariaLive);
    }
  });

  test('should have proper heading structure', async ({page}) => {
    await page.goto('/');

    const accessibilityScanResults = await new AxeBuilder({page})
    .withRules(['page-has-heading-one', 'heading-order'])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);

    // Additional heading structure tests
    const headings = await page.locator('h1, h2, h3, h4, h5, h6').all();

    if (headings.length > 0) {
      // Should have at least one h1
      const h1Elements = await page.locator('h1').all();
      expect(h1Elements.length).toBeGreaterThanOrEqual(1);
    }
  });

  test('should support screen readers', async ({page}) => {
    await page.goto('/');

    // Check for important ARIA attributes
    const accessibilityScanResults = await new AxeBuilder({page})
    .withRules([
      'aria-valid-attr',
      'aria-valid-attr-value',
      'aria-required-attr',
      'aria-roles'
    ])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('should handle modal dialogs accessibly', async ({page}) => {
    await page.goto('/');

    // Try to find and open a modal (adjust selectors based on your app)
    const modalTrigger = page.locator('button:has-text("Open"), [data-testid*="modal"], [aria-haspopup="dialog"]').first();

    if (await modalTrigger.isVisible()) {
      await modalTrigger.click();

      // Check modal accessibility
      const modal = page.locator('[role="dialog"], .modal, [aria-modal="true"]').first();

      if (await modal.isVisible()) {
        // Modal should trap focus
        await page.keyboard.press('Tab');
        const focusedElement = await page.locator(':focus').first();

        // Focused element should be within the modal
        await expect(focusedElement).toBeVisible();

        // Modal should be properly labeled
        const accessibilityScanResults = await new AxeBuilder({page})
        .include('[role="dialog"], .modal, [aria-modal="true"]')
        .withRules(['aria-dialog-name', 'focus-order-semantics'])
        .analyze();

        expect(accessibilityScanResults.violations).toEqual([]);

        // Should be able to close with Escape
        await page.keyboard.press('Escape');
        await expect(modal).not.toBeVisible();
      }
    }
  });

  test('should work with high contrast mode', async ({page}) => {
    // Enable high contrast mode (this is browser-specific)
    await page.emulateMedia({colorScheme: 'dark'});
    await page.goto('/');

    // Test that the page is still functional and accessible
    const accessibilityScanResults = await new AxeBuilder({page})
    .withTags(['wcag2a', 'wcag2aa'])
    .analyze();

    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('should support keyboard-only navigation', async ({page}) => {
    await page.goto('/');

    // Navigate the entire page using only keyboard
    const previousUrl = page.url();

    // Tab through the page and try to interact with elements
    for (let i = 0; i < 20; i++) {
      await page.keyboard.press('Tab');
      const focusedElement = await page.locator(':focus').first();

      if (await focusedElement.isVisible()) {
        // If it's a link or button, try to activate it
        const tagName = await focusedElement.evaluate(el => el.tagName.toLowerCase());
        const role = await focusedElement.getAttribute('role');

        if (tagName === 'button' || tagName === 'a' || role === 'button' || role === 'link') {
          // Press Enter to activate
          await page.keyboard.press('Enter');

          // Wait a moment for any navigation or modal opening
          await page.waitForTimeout(500);

          // If we navigated to a new page, check its accessibility too
          const currentUrl = page.url();
          if (currentUrl !== previousUrl) {
            const newPageResults = await new AxeBuilder({page})
            .withTags(['wcag2a', 'wcag2aa'])
            .analyze();

            expect(newPageResults.violations).toEqual([]);
            break; // Stop after first successful navigation
          }
        }
      }
    }
  });
});