/**
 * Comprehensive Accessibility E2E Tests for WCAG 2.1 AA Compliance
 * UOL-318: Accessibility Testing Suite
 * 
 * This test suite validates compliance with Web Content Accessibility Guidelines (WCAG) 2.1 AA
 * across all core functionality of the FocusHive application.
 */

import { test, expect, Page, devices } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { AccessibilityPage } from '../../pages/AccessibilityPage';
import { AccessibilityHelper } from '../../helpers/accessibility.helper';

test.describe('WCAG 2.1 AA Compliance Tests', () => {
  let accessibilityPage: AccessibilityPage;
  let axeHelper: AccessibilityHelper;

  test.beforeEach(async ({ page }) => {
    accessibilityPage = new AccessibilityPage(page);
    axeHelper = new AccessibilityHelper(page);
    
    // Configure axe-core for WCAG 2.1 AA compliance
    await axeHelper.configureAxe();
  });

  test.describe('1. Keyboard Navigation', () => {
    test('should support complete keyboard navigation on homepage', async ({ page }) => {
      await page.goto('/');
      await accessibilityPage.waitForPageLoad();

      // Test tab order and focus management
      const focusableElements = await accessibilityPage.getFocusableElements();
      expect(focusableElements.length).toBeGreaterThan(0);

      // Navigate through all focusable elements
      for (let i = 0; i < Math.min(focusableElements.length, 20); i++) {
        await page.keyboard.press('Tab');
        const focusedElement = await page.locator(':focus').first();
        await expect(focusedElement).toBeVisible();
        
        // Verify focus indicator is visible
        await axeHelper.verifyFocusIndicator(focusedElement);
      }
    });

    test('should provide skip links functionality', async ({ page }) => {
      await page.goto('/');
      
      // Press Tab to reveal skip links
      await page.keyboard.press('Tab');
      
      const skipLink = await accessibilityPage.getSkipLink();
      if (skipLink) {
        await expect(skipLink).toBeVisible();
        await skipLink.click();
        
        // Main content should receive focus
        const mainContent = await accessibilityPage.getMainContent();
        await expect(mainContent).toBeVisible();
      }
    });

    test('should handle keyboard shortcuts correctly', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Test common keyboard shortcuts
      await page.keyboard.press('Escape'); // Should close any open dialogs
      await page.waitForTimeout(500);
      
      // Test Alt+M for main menu (if applicable)
      await page.keyboard.press('Alt+m');
      await page.waitForTimeout(500);
      
      // Verify no keyboard traps exist
      await accessibilityPage.verifyNoKeyboardTraps();
    });

    test('should handle modal focus trapping', async ({ page }) => {
      await page.goto('/dashboard');
      
      const modalTrigger = await accessibilityPage.findModalTrigger();
      if (modalTrigger) {
        await modalTrigger.click();
        await page.waitForTimeout(500);
        
        const modal = await accessibilityPage.getOpenModal();
        if (modal) {
          // Test focus trapping
          await accessibilityPage.testFocusTrap(modal);
          
          // Test Escape key closes modal
          await page.keyboard.press('Escape');
          await expect(modal).not.toBeVisible();
        }
      }
    });

    test('should support arrow key navigation in menus', async ({ page }) => {
      await page.goto('/');
      
      const menu = await accessibilityPage.findNavigationMenu();
      if (menu) {
        await menu.focus();
        
        // Test arrow key navigation
        const menuItems = await menu.locator('[role="menuitem"], a, button').all();
        
        for (let i = 0; i < Math.min(menuItems.length, 5); i++) {
          await page.keyboard.press('ArrowDown');
          const focusedElement = page.locator(':focus');
          await expect(focusedElement).toBeVisible();
        }
      }
    });
  });

  test.describe('2. Screen Reader Compatibility', () => {
    test('should have proper ARIA labels and descriptions', async ({ page }) => {
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules([
          'aria-valid-attr',
          'aria-valid-attr-value',
          'aria-required-attr',
          'aria-roles',
          'aria-allowed-attr'
        ])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should have proper heading hierarchy', async ({ page }) => {
      await page.goto('/');
      
      // Check heading structure
      const headings = await accessibilityPage.getHeadingStructure();
      expect(headings.h1.length).toBeGreaterThanOrEqual(1);
      expect(headings.h1.length).toBeLessThanOrEqual(1); // Should have only one h1
      
      // Verify heading order
      await axeHelper.verifyHeadingOrder(headings);
    });

    test('should have live regions for dynamic content', async ({ page }) => {
      await page.goto('/dashboard');
      
      const liveRegions = await accessibilityPage.getLiveRegions();
      
      for (const region of liveRegions) {
        const ariaLive = await region.getAttribute('aria-live');
        expect(['polite', 'assertive', 'off']).toContain(ariaLive);
      }
    });

    test('should provide landmark navigation', async ({ page }) => {
      await page.goto('/');
      
      const landmarks = await accessibilityPage.getLandmarks();
      
      // Should have main landmark
      expect(landmarks.main.length).toBeGreaterThanOrEqual(1);
      
      // Should have navigation landmark
      expect(landmarks.navigation.length).toBeGreaterThanOrEqual(1);
      
      // Verify landmarks have proper labels
      await axeHelper.verifyLandmarkLabels(landmarks);
    });

    test('should have alternative text for images', async ({ page }) => {
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules(['image-alt', 'image-redundant-alt'])
        .analyze();

      expect(results.violations).toEqual([]);
    });
  });

  test.describe('3. Color and Contrast', () => {
    test('should meet WCAG AA contrast requirements', async ({ page }) => {
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules(['color-contrast'])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should not rely on color alone for information', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Test form validation indicators
      const results = await new AxeBuilder({ page })
        .withRules(['color-contrast', 'use-landmarks'])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should support high contrast mode', async ({ page }) => {
      await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules(['color-contrast'])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should have visible focus indicators', async ({ page }) => {
      await page.goto('/');
      
      const focusableElements = await accessibilityPage.getFocusableElements();
      
      for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {
        await page.keyboard.press('Tab');
        const focusedElement = page.locator(':focus');
        await axeHelper.verifyFocusIndicator(focusedElement);
      }
    });
  });

  test.describe('4. Form Accessibility', () => {
    test('should have proper label associations', async ({ page }) => {
      await page.goto('/login');
      
      const results = await new AxeBuilder({ page })
        .withRules([
          'label',
          'form-field-multiple-labels',
          'label-content-name-mismatch'
        ])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should provide clear error messages', async ({ page }) => {
      await page.goto('/login');
      
      // Submit form without filling fields
      await page.locator('button[type="submit"]').click();
      await page.waitForTimeout(1000);
      
      const errorMessages = await accessibilityPage.getErrorMessages();
      
      for (const error of errorMessages) {
        await expect(error).toBeVisible();
        
        // Error should be associated with form field
        const errorId = await error.getAttribute('id');
        if (errorId) {
          const associatedField = page.locator(`[aria-describedby*="${errorId}"]`);
          await expect(associatedField).toBeVisible();
        }
      }
    });

    test('should indicate required fields properly', async ({ page }) => {
      await page.goto('/register');
      
      const requiredFields = await accessibilityPage.getRequiredFields();
      
      for (const field of requiredFields) {
        // Should have aria-required or required attribute
        const hasAriaRequired = await field.getAttribute('aria-required');
        const hasRequired = await field.getAttribute('required');
        
        expect(hasAriaRequired === 'true' || hasRequired !== null).toBeTruthy();
      }
    });

    test('should use fieldsets for related form controls', async ({ page }) => {
      await page.goto('/profile');
      
      const fieldsets = await page.locator('fieldset').all();
      
      for (const fieldset of fieldsets) {
        const legend = fieldset.locator('legend').first();
        await expect(legend).toBeVisible();
      }
    });
  });

  test.describe('5. Interactive Elements', () => {
    test('should use proper button vs link semantics', async ({ page }) => {
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules(['button-name', 'link-name'])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should have adequate clickable area sizes', async ({ page }) => {
      await page.goto('/');
      
      const buttons = await accessibilityPage.getInteractiveElements();
      
      for (const button of buttons.slice(0, 10)) {
        const boundingBox = await button.boundingBox();
        if (boundingBox) {
          // WCAG requirement: 44x44px minimum
          expect(boundingBox.width).toBeGreaterThanOrEqual(44);
          expect(boundingBox.height).toBeGreaterThanOrEqual(44);
        }
      }
    });

    test('should have proper hover and focus states', async ({ page }) => {
      await page.goto('/');
      
      const interactiveElements = await accessibilityPage.getInteractiveElements();
      
      for (const element of interactiveElements.slice(0, 5)) {
        // Test focus state
        await element.focus();
        await axeHelper.verifyFocusIndicator(element);
        
        // Test hover state (if applicable)
        await element.hover();
        await page.waitForTimeout(200);
      }
    });

    test('should communicate loading states properly', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Look for loading states
      const loadingElements = await page.locator('[aria-busy="true"], [aria-live="polite"]').all();
      
      for (const element of loadingElements) {
        const ariaBusy = await element.getAttribute('aria-busy');
        const ariaLive = await element.getAttribute('aria-live');
        
        expect(ariaBusy === 'true' || ['polite', 'assertive'].includes(ariaLive!)).toBeTruthy();
      }
    });

    test('should communicate disabled state properly', async ({ page }) => {
      await page.goto('/dashboard');
      
      const disabledElements = await page.locator(':disabled, [aria-disabled="true"]').all();
      
      for (const element of disabledElements) {
        const isDisabled = await element.isDisabled();
        const ariaDisabled = await element.getAttribute('aria-disabled');
        
        expect(isDisabled || ariaDisabled === 'true').toBeTruthy();
      }
    });
  });

  test.describe('6. Media Accessibility', () => {
    test('should provide video controls and captions', async ({ page }) => {
      await page.goto('/help'); // Assuming help page might have videos
      
      const videos = await page.locator('video').all();
      
      for (const video of videos) {
        const hasControls = await video.getAttribute('controls');
        expect(hasControls).toBeTruthy();
        
        // Check for caption tracks
        const tracks = await video.locator('track[kind="captions"]').all();
        if (tracks.length > 0) {
          for (const track of tracks) {
            const src = await track.getAttribute('src');
            expect(src).toBeTruthy();
          }
        }
      }
    });

    test('should provide audio descriptions when needed', async ({ page }) => {
      await page.goto('/help');
      
      const videos = await page.locator('video').all();
      
      for (const video of videos) {
        const descriptionTracks = await video.locator('track[kind="descriptions"]').all();
        // Audio descriptions are not always required, but if present should be valid
        for (const track of descriptionTracks) {
          const src = await track.getAttribute('src');
          expect(src).toBeTruthy();
        }
      }
    });
  });

  test.describe('7. Content Structure', () => {
    test('should use semantic HTML properly', async ({ page }) => {
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules([
          'region',
          'page-has-heading-one',
          'bypass',
          'landmark-one-main'
        ])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should have proper table structure', async ({ page }) => {
      await page.goto('/analytics'); // Assuming analytics page has tables
      
      const tables = await page.locator('table').all();
      
      for (const table of tables) {
        const headers = await table.locator('th').all();
        expect(headers.length).toBeGreaterThan(0);
        
        // Check for proper scope attributes
        for (const header of headers) {
          const scope = await header.getAttribute('scope');
          if (scope) {
            expect(['col', 'row', 'colgroup', 'rowgroup']).toContain(scope);
          }
        }
      }
    });

    test('should have proper list structure', async ({ page }) => {
      await page.goto('/');
      
      const results = await new AxeBuilder({ page })
        .withRules(['list', 'listitem'])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should have proper language attributes', async ({ page }) => {
      await page.goto('/');
      
      const htmlElement = await page.locator('html').first();
      const lang = await htmlElement.getAttribute('lang');
      expect(lang).toBeTruthy();
      expect(lang).toMatch(/^[a-z]{2}(-[A-Z]{2})?$/); // e.g., 'en' or 'en-US'
    });

    test('should have proper page titles', async ({ page }) => {
      const pages = ['/', '/login', '/dashboard', '/profile'];
      
      for (const pagePath of pages) {
        await page.goto(pagePath);
        
        const title = await page.title();
        expect(title).toBeTruthy();
        expect(title.length).toBeGreaterThan(0);
        expect(title.length).toBeLessThan(60); // SEO best practice
      }
    });
  });

  test.describe('8. Motion and Animation', () => {
    test('should respect reduced motion preferences', async ({ page }) => {
      await page.emulateMedia({ reducedMotion: 'reduce' });
      await page.goto('/');
      
      // Check for CSS animations that should be disabled
      const animatedElements = await page.locator('[style*="animation"], [class*="animate"]').all();
      
      for (const element of animatedElements) {
        const computedStyle = await element.evaluate((el) => 
          window.getComputedStyle(el).getPropertyValue('animation-duration')
        );
        
        // Animation should be disabled or very short with reduced motion
        expect(computedStyle === '0s' || computedStyle === 'none').toBeTruthy();
      }
    });

    test('should provide controls for auto-playing content', async ({ page }) => {
      await page.goto('/dashboard');
      
      const autoplayElements = await page.locator('[autoplay], video[autoplay], audio[autoplay]').all();
      
      for (const element of autoplayElements) {
        const hasControls = await element.getAttribute('controls');
        const isMuted = await element.getAttribute('muted');
        
        // Auto-playing content should either have controls or be muted
        expect(hasControls !== null || isMuted !== null).toBeTruthy();
      }
    });
  });

  test.describe('9. Responsive Accessibility', () => {
    test('should maintain accessibility at 200% zoom', async ({ page }) => {
      await page.goto('/');
      
      // Zoom to 200%
      await page.setViewportSize({ width: 640, height: 480 }); // Simulate 200% zoom
      
      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa'])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should reflow properly at 320px width', async ({ page }) => {
      await page.setViewportSize({ width: 320, height: 568 });
      await page.goto('/');
      
      // Should not have horizontal scrolling
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      const viewportWidth = await page.evaluate(() => window.innerWidth);
      
      expect(bodyWidth).toBeLessThanOrEqual(viewportWidth);
    });

    test('should have adequate touch targets on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 }); // iPhone dimensions
      await page.goto('/');
      
      const touchTargets = await accessibilityPage.getInteractiveElements();
      
      for (const target of touchTargets.slice(0, 10)) {
        const boundingBox = await target.boundingBox();
        if (boundingBox) {
          // Touch targets should be at least 44x44px
          expect(boundingBox.width).toBeGreaterThanOrEqual(44);
          expect(boundingBox.height).toBeGreaterThanOrEqual(44);
        }
      }
    });
  });

  test.describe('10. Assistive Technology Support', () => {
    test('should work with screen reader simulation', async ({ page }) => {
      await page.goto('/');
      
      // Simulate screen reader navigation
      await page.keyboard.press('Tab');
      let focusedElement = page.locator(':focus');
      
      // Navigate through content using screen reader keys
      const screenReaderKeys = ['ArrowDown', 'ArrowUp', 'Tab', 'Shift+Tab'];
      
      for (const key of screenReaderKeys) {
        await page.keyboard.press(key);
        await page.waitForTimeout(100);
        
        focusedElement = page.locator(':focus');
        if (await focusedElement.isVisible()) {
          // Element should have accessible name
          const accessibleName = await axeHelper.getAccessibleName(focusedElement);
          expect(accessibleName).toBeTruthy();
        }
      }
    });

    test('should provide proper NVDA/JAWS support', async ({ page }) => {
      await page.goto('/');
      
      // Test common screen reader functionality
      const results = await new AxeBuilder({ page })
        .withRules([
          'aria-valid-attr',
          'aria-required-attr',
          'button-name',
          'link-name',
          'image-alt'
        ])
        .analyze();

      expect(results.violations).toEqual([]);
    });

    test('should support voice control software', async ({ page }) => {
      await page.goto('/');
      
      // Elements should have accessible names for voice control
      const interactiveElements = await accessibilityPage.getInteractiveElements();
      
      for (const element of interactiveElements.slice(0, 10)) {
        const accessibleName = await axeHelper.getAccessibleName(element);
        expect(accessibleName).toBeTruthy();
        expect(accessibleName.length).toBeGreaterThan(0);
      }
    });
  });

  test.describe('Cross-Browser Accessibility', () => {
    const browsers = [
      { name: 'chromium', device: devices['Desktop Chrome'] },
      { name: 'firefox', device: devices['Desktop Firefox'] },
      { name: 'webkit', device: devices['Desktop Safari'] }
    ];

    browsers.forEach(({ name, device }) => {
      test(`should maintain accessibility in ${name}`, async ({ page, browserName }) => {
        test.skip(browserName !== name, `Test only runs on ${name}`);
        
        await page.goto('/');
        
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
          .analyze();

        expect(results.violations).toEqual([]);
      });
    });
  });

  test.describe('Performance and Accessibility', () => {
    test('should maintain accessibility during loading states', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Test accessibility during loading
      const loadingState = page.locator('[aria-busy="true"]').first();
      
      if (await loadingState.isVisible()) {
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa'])
          .analyze();

        expect(results.violations).toEqual([]);
      }
    });

    test('should maintain accessibility with dynamic content', async ({ page }) => {
      await page.goto('/dashboard');
      
      // Trigger dynamic content loading
      const dynamicTrigger = page.locator('button:has-text("Load More"), [data-load-more]').first();
      
      if (await dynamicTrigger.isVisible()) {
        await dynamicTrigger.click();
        await page.waitForTimeout(1000);
        
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa'])
          .analyze();

        expect(results.violations).toEqual([]);
      }
    });
  });

  // Comprehensive accessibility audit of all major pages
  test.describe('Full Page Accessibility Audits', () => {
    const pages = [
      { path: '/', name: 'Homepage' },
      { path: '/login', name: 'Login Page' },
      { path: '/register', name: 'Register Page' },
      { path: '/dashboard', name: 'Dashboard' },
      { path: '/profile', name: 'Profile Page' },
      { path: '/settings', name: 'Settings Page' },
      { path: '/help', name: 'Help Page' }
    ];

    pages.forEach(({ path, name }) => {
      test(`${name} should be fully accessible`, async ({ page }) => {
        await page.goto(path);
        await accessibilityPage.waitForPageLoad();
        
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
          .analyze();

        if (results.violations.length > 0) {
          console.error(`Accessibility violations found on ${name}:`, results.violations);
        }

        expect(results.violations).toEqual([]);
      });
    });
  });
});

// Utility test for generating accessibility reports
test.describe('Accessibility Reporting', () => {
  interface AccessibilityResult {
    page: string;
    violations: Array<{
      id: string;
      description: string;
      nodes: Array<{
        target: string[];
        html: string;
      }>;
    }>;
    passes: number;
    incomplete: number;
  }

  test('should generate comprehensive accessibility report', async ({ page }) => {
    const pages = ['/', '/login', '/dashboard'];
    const allResults: AccessibilityResult[] = [];
    
    for (const pagePath of pages) {
      await page.goto(pagePath);
      
      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
        .analyze();
      
      allResults.push({
        page: pagePath,
        violations: results.violations,
        passes: results.passes.length,
        incomplete: results.incomplete.length
      });
    }
    
    // Generate summary report
    const totalViolations = allResults.reduce((sum, result) => sum + result.violations.length, 0);
    const totalPasses = allResults.reduce((sum, result) => sum + result.passes, 0);
    
    console.log('Accessibility Summary Report:', {
      totalPages: allResults.length,
      totalViolations,
      totalPasses,
      complianceRate: totalViolations === 0 ? 100 : ((totalPasses / (totalPasses + totalViolations)) * 100).toFixed(2)
    });
    
    expect(totalViolations).toBe(0);
  });
});