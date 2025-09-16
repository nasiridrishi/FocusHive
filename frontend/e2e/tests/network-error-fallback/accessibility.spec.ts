/**
 * Advanced Accessibility Testing for Network Error Scenarios
 * Tests screen reader compatibility, keyboard navigation, and ARIA compliance
 */

import { test, expect, Page } from '@playwright/test';
import { injectAxe, checkA11y, getViolations, configureAxe } from 'axe-playwright';
import { createNetworkSimulator } from './utils/networkSim';
import { NetworkTestHelper } from './test-helpers';

test.describe('Network Error Accessibility Testing', () => {
  let networkSim: ReturnType<typeof createNetworkSimulator>;
  let testHelper: NetworkTestHelper;

  test.beforeEach(async ({ page }) => {
    networkSim = createNetworkSimulator(page);
    testHelper = new NetworkTestHelper(page);
    
    // Inject axe-core for accessibility testing
    await injectAxe(page);
    
    // Configure axe with custom rules
    await configureAxe(page, {
      rules: {
        // Enable specific rules for error states
        'aria-live-region-must-have-polite-or-assertive': { enabled: true },
        'color-contrast': { enabled: true },
        'focus-order-semantics': { enabled: true },
        'keyboard-navigation': { enabled: true },
      }
    });

    await testHelper.resetMockBackend();
  });

  test.afterEach(async () => {
    await networkSim.cleanup();
  });

  test.describe('ARIA Compliance During Network Errors', () => {
    test('should have proper ARIA attributes on error fallback', async ({ page }) => {
      // Trigger network error
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Check general accessibility
      await checkA11y(page);

      // Verify specific ARIA attributes
      const errorFallback = page.getByTestId('network-error-fallback');
      await expect(errorFallback).toHaveAttribute('role', 'alert');
      await expect(errorFallback).toHaveAttribute('aria-live', 'polite');
      
      // Check error message has proper labeling
      const errorMessage = page.getByTestId('error-message');
      await expect(errorMessage).toHaveAttribute('aria-describedby');
      
      // Verify buttons have accessible names
      const retryButton = page.getByTestId('retry-button');
      await expect(retryButton).toHaveAccessibleName();
      
      const checkConnectionButton = page.getByTestId('check-connection-button');
      await expect(checkConnectionButton).toHaveAccessibleName();
    });

    test('should announce network status changes to screen readers', async ({ page }) => {
      await page.goto('/dashboard');

      // Monitor aria-live regions
      const liveRegions = await page.locator('[aria-live]').all();
      expect(liveRegions.length).toBeGreaterThan(0);

      // Go offline and check announcements
      await networkSim.goOffline();
      
      // Should have live region announcing offline state
      await expect(page.getByRole('status')).toContainText(/offline|disconnected/i);
      
      // Come back online
      await networkSim.goOnline();
      
      // Should announce online state
      await expect(page.getByRole('status')).toContainText(/online|connected/i, { timeout: 10000 });
    });

    test('should maintain proper heading hierarchy in error states', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Check heading hierarchy
      const headings = await page.locator('h1, h2, h3, h4, h5, h6').all();
      
      for (let i = 0; i < headings.length - 1; i++) {
        const currentLevel = parseInt(await headings[i].evaluate(el => el.tagName.slice(1)));
        const nextLevel = parseInt(await headings[i + 1].evaluate(el => el.tagName.slice(1)));
        
        // Next heading should not skip levels (e.g., h1 -> h3)
        expect(nextLevel - currentLevel).toBeLessThanOrEqual(1);
      }
    });

    test('should provide alternative text for status icons', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');

      // Check network status icons have alt text or ARIA labels
      const statusIcons = page.locator('[data-testid*="status-icon"], [data-testid*="network-icon"]');
      const iconCount = await statusIcons.count();

      for (let i = 0; i < iconCount; i++) {
        const icon = statusIcons.nth(i);
        
        // Should have either alt text or aria-label
        const hasAltOrLabel = await icon.evaluate(el => {
          return el.hasAttribute('alt') || 
                 el.hasAttribute('aria-label') || 
                 el.hasAttribute('aria-labelledby');
        });
        
        expect(hasAltOrLabel).toBeTruthy();
      }
    });
  });

  test.describe('Keyboard Navigation', () => {
    test('should support full keyboard navigation in error states', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Start keyboard navigation from the top
      await page.keyboard.press('Tab');
      
      // Should focus on retry button first
      await expect(page.getByTestId('retry-button')).toBeFocused();
      
      // Navigate to next interactive element
      await page.keyboard.press('Tab');
      await expect(page.getByTestId('check-connection-button')).toBeFocused();
      
      // Navigate to troubleshooting toggle
      await page.keyboard.press('Tab');
      await expect(page.getByTestId('troubleshooting-toggle')).toBeFocused();
      
      // Test activation with Enter key
      await page.keyboard.press('Enter');
      await expect(page.getByTestId('troubleshooting-steps')).toBeVisible();
      
      // Test activation with Space key
      await page.keyboard.press('Space');
      await expect(page.getByTestId('troubleshooting-steps')).not.toBeVisible();
    });

    test('should handle focus management during network state transitions', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Focus on a button
      await page.getByTestId('refresh-button').focus();
      const _initialFocusedElement = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
      
      // Go offline - focus should be managed appropriately
      await networkSim.goOffline();
      await testHelper.waitForNetworkErrorFallback();
      
      // Focus should move to an appropriate element in the error UI
      const focusedAfterError = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
      expect(focusedAfterError).toBeTruthy();
      
      // Come back online
      await networkSim.goOnline();
      await testHelper.clickRetryButton();
      await testHelper.waitForNetworkRestore();
      
      // Focus should be restored or moved to a logical location
      const focusedAfterRestore = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
      expect(focusedAfterRestore).toBeTruthy();
    });

    test('should support escape key to dismiss error details', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Open troubleshooting details
      await page.getByTestId('troubleshooting-toggle').click();
      await expect(page.getByTestId('troubleshooting-steps')).toBeVisible();
      
      // Press Escape to close
      await page.keyboard.press('Escape');
      await expect(page.getByTestId('troubleshooting-steps')).not.toBeVisible();
    });

    test('should trap focus within modal error dialogs', async ({ page }) => {
      // Trigger a modal error dialog
      await networkSim.goOffline();
      await page.goto('/critical-action');
      
      // Should show modal dialog for critical errors
      const modal = page.getByRole('dialog');
      await expect(modal).toBeVisible();
      
      // Tab through modal elements
      await page.keyboard.press('Tab');
      const firstFocusable = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
      
      // Keep tabbing until we cycle back
      let currentFocus = firstFocusable;
      let tabCount = 0;
      const maxTabs = 10; // Prevent infinite loop
      
      do {
        await page.keyboard.press('Tab');
        tabCount++;
        currentFocus = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
      } while (currentFocus !== firstFocusable && tabCount < maxTabs);
      
      // Should have cycled back to first element (focus trap working)
      expect(currentFocus).toBe(firstFocusable);
    });
  });

  test.describe('Screen Reader Compatibility', () => {
    test('should provide meaningful error descriptions for screen readers', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Error message should be descriptive
      const errorMessage = page.getByTestId('error-message');
      const messageText = await errorMessage.textContent();
      
      // Should contain contextual information
      expect(messageText).toMatch(/(network|connection|offline|internet)/i);
      expect(messageText?.length).toBeGreaterThan(20); // Descriptive, not just "Error"
      
      // Should have appropriate aria-describedby relationships
      const describedBy = await errorMessage.getAttribute('aria-describedby');
      expect(describedBy).toBeTruthy();
      
      if (describedBy) {
        const descriptionElement = page.locator(`#${describedBy}`);
        await expect(descriptionElement).toBeVisible();
      }
    });

    test('should announce retry attempts to screen readers', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Click retry and monitor announcements
      const statusRegion = page.getByRole('status');
      
      await testHelper.clickRetryButton();
      
      // Should announce retry attempt
      await expect(statusRegion).toContainText(/retrying|attempting/i);
      
      // After failure, should announce result
      await expect(statusRegion).toContainText(/failed|unsuccessful/i, { timeout: 10000 });
    });

    test('should provide progress feedback for long operations', async ({ page }) => {
      await networkSim.throttle('SLOW_2G'); // Very slow connection
      await page.goto('/dashboard');

      // Should show progress indication
      const progressRegion = page.getByRole('progressbar');
      await expect(progressRegion).toBeVisible();
      
      // Progress should have accessible label
      await expect(progressRegion).toHaveAttribute('aria-label');
      
      // Should announce progress changes
      const statusRegion = page.getByRole('status');
      await expect(statusRegion).toContainText(/loading|progress/i);
    });
  });

  test.describe('High Contrast and Visual Accessibility', () => {
    test('should maintain accessibility in high contrast mode', async ({ page }) => {
      // Enable high contrast mode
      await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
      await page.addStyleTag({
        content: `
          @media (prefers-contrast: high) {
            * {
              background: black !important;
              color: white !important;
              border-color: white !important;
            }
          }
        `
      });

      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Check color contrast ratios
      await checkA11y(page, null, {
        rules: {
          'color-contrast': { enabled: true }
        }
      });

      // Verify interactive elements are still visible
      await expect(page.getByTestId('retry-button')).toBeVisible();
      await expect(page.getByTestId('check-connection-button')).toBeVisible();
    });

    test('should respect reduced motion preferences', async ({ page }) => {
      // Enable reduced motion
      await page.emulateMedia({ reducedMotion: 'reduce' });
      
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Check that animations are disabled or reduced
      const animatedElements = page.locator('[data-testid*="loading"], [data-testid*="spinner"], .animate');
      const elementCount = await animatedElements.count();

      for (let i = 0; i < elementCount; i++) {
        const element = animatedElements.nth(i);
        const animationDuration = await element.evaluate(el => {
          const style = window.getComputedStyle(el);
          return style.animationDuration;
        });
        
        // Animation should be very short or disabled
        expect(animationDuration).toMatch(/(0s|none|0\.01s)/);
      }
    });

    test('should provide sufficient focus indicators', async ({ page }) => {
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Test focus indicators on interactive elements
      const interactiveElements = page.locator('button, [tabindex="0"], a, input');
      const elementCount = await interactiveElements.count();

      for (let i = 0; i < elementCount; i++) {
        const element = interactiveElements.nth(i);
        
        // Focus the element
        await element.focus();
        
        // Check focus indicator visibility
        const focusStyles = await element.evaluate(el => {
          const style = window.getComputedStyle(el);
          return {
            outline: style.outline,
            outlineWidth: style.outlineWidth,
            outlineColor: style.outlineColor,
            boxShadow: style.boxShadow
          };
        });
        
        // Should have visible focus indicator
        const hasFocusIndicator = 
          focusStyles.outline !== 'none' || 
          focusStyles.outlineWidth !== '0px' ||
          focusStyles.boxShadow.includes('inset') ||
          focusStyles.boxShadow.includes('shadow');
          
        expect(hasFocusIndicator).toBeTruthy();
      }
    });
  });

  test.describe('Mobile Accessibility', () => {
    test('should support touch accessibility on mobile', async ({ page }) => {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 });
      
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      // Check touch target sizes
      const touchTargets = page.locator('button, [role="button"], a');
      const targetCount = await touchTargets.count();

      for (let i = 0; i < targetCount; i++) {
        const target = touchTargets.nth(i);
        const boundingBox = await target.boundingBox();
        
        if (boundingBox) {
          // Touch targets should be at least 44px (iOS) or 48dp (Android)
          expect(Math.min(boundingBox.width, boundingBox.height)).toBeGreaterThanOrEqual(44);
        }
      }
    });

    test('should handle swipe gestures for error dismissal', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();

      const errorBanner = page.getByTestId('network-error-banner');
      if (await errorBanner.isVisible()) {
        // Test swipe gesture (if supported)
        const box = await errorBanner.boundingBox();
        if (box) {
          await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
          
          // Swipe left to dismiss
          await page.touchscreen.tap(box.x + box.width - 10, box.y + box.height / 2);
          await page.touchscreen.tap(box.x + 10, box.y + box.height / 2);
          
          // Banner should be dismissed or show dismiss animation
          // This would depend on implementation
        }
      }
    });
  });

  test.describe('Form Accessibility During Network Errors', () => {
    test('should maintain form accessibility during submission failures', async ({ page }) => {
      await page.goto('/contact-form');
      
      // Fill out form
      await page.getByLabel('Name').fill('Test User');
      await page.getByLabel('Email').fill('test@example.com');
      await page.getByLabel('Message').fill('Test message');
      
      // Go offline before submission
      await networkSim.goOffline();
      
      // Try to submit
      await page.getByRole('button', { name: 'Submit' }).click();
      
      // Should show accessible error message
      const errorMessage = page.getByRole('alert');
      await expect(errorMessage).toBeVisible();
      await expect(errorMessage).toHaveAccessibleName();
      
      // Form fields should retain their values and labels
      await expect(page.getByLabel('Name')).toHaveValue('Test User');
      await expect(page.getByLabel('Email')).toHaveValue('test@example.com');
      await expect(page.getByLabel('Message')).toHaveValue('Test message');
      
      // Error should be associated with form
      const formElement = page.locator('form');
      const ariaDescribedBy = await formElement.getAttribute('aria-describedby');
      
      if (ariaDescribedBy) {
        const errorElement = page.locator(`#${ariaDescribedBy}`);
        await expect(errorElement).toBeVisible();
      }
    });

    test('should provide clear validation messages during network issues', async ({ page }) => {
      await networkSim.throttle('SLOW_2G');
      await page.goto('/registration-form');
      
      // Submit form with invalid data during slow network
      await page.getByLabel('Email').fill('invalid-email');
      await page.getByRole('button', { name: 'Register' }).click();
      
      // Should show validation errors clearly
      const validationError = page.getByText('Please enter a valid email address');
      await expect(validationError).toBeVisible();
      await expect(validationError).toHaveAttribute('role', 'alert');
      
      // Error should be associated with the field
      const emailField = page.getByLabel('Email');
      await expect(emailField).toHaveAttribute('aria-invalid', 'true');
      await expect(emailField).toHaveAttribute('aria-describedby');
    });
  });

  test.describe('Language and Internationalization', () => {
    test('should maintain accessibility across different languages', async ({ page }) => {
      // Test with different language
      await page.addInitScript(() => {
        Object.defineProperty(navigator, 'language', {
          get: () => 'es-ES'
        });
      });
      
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();
      
      // Should have appropriate lang attribute
      const htmlElement = page.locator('html');
      const langAttribute = await htmlElement.getAttribute('lang');
      expect(langAttribute).toBeTruthy();
      
      // Error messages should be in the correct language
      const errorMessage = page.getByTestId('error-message');
      await expect(errorMessage).toBeVisible();
      
      // Run accessibility check for international content
      await checkA11y(page);
    });

    test('should handle RTL languages properly', async ({ page }) => {
      // Set RTL language
      await page.addStyleTag({
        content: `
          html { direction: rtl; }
          body { text-align: right; }
        `
      });
      
      await networkSim.goOffline();
      await page.goto('/dashboard');
      await testHelper.waitForNetworkErrorFallback();
      
      // Check that layout works correctly in RTL
      const errorFallback = page.getByTestId('network-error-fallback');
      const direction = await errorFallback.evaluate(el => 
        window.getComputedStyle(el).direction
      );
      
      expect(direction).toBe('rtl');
      
      // Accessibility should still pass
      await checkA11y(page);
    });
  });
});

// Helper function to check for specific accessibility violations
async function _checkNetworkErrorAccessibility(page: Page) {
  const violations = await getViolations(page);
  
  // Filter for violations specific to network error components
  const networkErrorViolations = violations.filter(violation => 
    violation.nodes.some(node => 
      node.target.some(target => 
        typeof target === 'string' && 
        (target.includes('network-error') || 
         target.includes('retry-button') ||
         target.includes('connection-status'))
      )
    )
  );
  
  if (networkErrorViolations.length > 0) {
    console.error('Network Error Accessibility Violations:', networkErrorViolations);
    throw new Error(`Found ${networkErrorViolations.length} accessibility violations in network error components`);
  }
}

// Helper to simulate screen reader navigation
async function _simulateScreenReaderNavigation(page: Page) {
  // Navigate by headings (simulating H key in screen reader)
  const headings = await page.locator('h1, h2, h3, h4, h5, h6').all();
  
  for (const heading of headings) {
    await heading.focus();
    const text = await heading.textContent();
    console.log(`Screen reader would announce: "${text}"`);
  }
  
  // Navigate by landmarks (simulating D key in screen reader)
  const landmarks = await page.locator('[role="main"], [role="banner"], [role="navigation"], [role="complementary"], [role="contentinfo"]').all();
  
  for (const landmark of landmarks) {
    const role = await landmark.getAttribute('role');
    const label = await landmark.getAttribute('aria-label') || await landmark.getAttribute('aria-labelledby');
    console.log(`Screen reader would announce landmark: "${role}" ${label ? `"${label}"` : ''}`);
  }
}