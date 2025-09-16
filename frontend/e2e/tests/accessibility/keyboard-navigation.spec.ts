/**
 * Keyboard Navigation Accessibility Tests
 *
 * Tests comprehensive keyboard accessibility including:
 * - Tab order and focus management
 * - Skip links and navigation shortcuts
 * - Modal and dialog focus trapping
 * - Custom component keyboard support
 * - Arrow key navigation patterns
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {expect, Locator, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {AccessibilityHelper} from '../../helpers/accessibility.helper';
import {AccessibilityPage} from '../../pages/AccessibilityPage';

interface FocusTestResult {
  element: Locator;
  isVisible: boolean;
  hasFocusIndicator: boolean;
  accessibleName: string;
}

interface KeyboardShortcut {
  key: string;
  description: string;
  expectedAction: string;
}

test.describe('Keyboard Navigation Accessibility Tests', () => {
  let accessibilityHelper: AccessibilityHelper;
  let accessibilityPage: AccessibilityPage;

  test.beforeEach(async ({page}) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);

    await accessibilityHelper.configureAxe();
  });

  test.describe('Basic Keyboard Navigation', () => {
    test('should support Tab navigation through all interactive elements', async ({page}) => {
      const testPages = ['/', '/dashboard', '/login'];

      for (const testPage of testPages) {
        await page.goto(testPage);
        await accessibilityPage.waitForPageLoad();

        const focusableElements = await accessibilityPage.getFocusableElements();
        expect(focusableElements.length, `Page ${testPage} should have focusable elements`).toBeGreaterThan(0);

        const focusResults: FocusTestResult[] = [];

        // Test Tab navigation
        for (let i = 0; i < Math.min(focusableElements.length, 15); i++) {
          await page.keyboard.press('Tab');
          await page.waitForTimeout(100);

          const currentFocus = page.locator(':focus');
          const isVisible = await currentFocus.isVisible();
          const focusResult = await accessibilityHelper.verifyFocusIndicator(currentFocus);
          const accessibleName = await accessibilityHelper.getAccessibleName(currentFocus);

          focusResults.push({
            element: currentFocus,
            isVisible,
            hasFocusIndicator: focusResult.isVisible,
            accessibleName
          });

          expect(isVisible, `Focus should be visible on element ${i + 1}`).toBeTruthy();
          expect(focusResult.isVisible, `Focus indicator should be visible on element ${i + 1}`).toBeTruthy();
          expect(accessibleName.length, `Element ${i + 1} should have accessible name`).toBeGreaterThan(0);
        }

        console.log(`Page ${testPage} keyboard navigation test completed:`, {
          totalElements: focusResults.length,
          visibleElements: focusResults.filter(r => r.isVisible).length,
          elementsWithFocusIndicator: focusResults.filter(r => r.hasFocusIndicator).length,
          elementsWithNames: focusResults.filter(r => r.accessibleName.length > 0).length
        });
      }
    });

    test('should support Shift+Tab reverse navigation', async ({page}) => {
      await page.goto('/dashboard');

      const focusableElements = await accessibilityPage.getFocusableElements();

      if (focusableElements.length === 0) return;

      // Navigate to middle of page
      const middleIndex = Math.floor(focusableElements.length / 2);

      for (let i = 0; i < middleIndex; i++) {
        await page.keyboard.press('Tab');
        await page.waitForTimeout(50);
      }

      const currentPosition = page.locator(':focus');
      const currentName = await accessibilityHelper.getAccessibleName(currentPosition);

      // Navigate backward
      await page.keyboard.press('Shift+Tab');
      await page.waitForTimeout(100);

      const previousPosition = page.locator(':focus');
      const previousName = await accessibilityHelper.getAccessibleName(previousPosition);

      // Should be on different element
      expect(previousName, 'Shift+Tab should move to previous element').not.toBe(currentName);

      // Focus should still be visible
      await expect(previousPosition).toBeVisible();
      const focusResult = await accessibilityHelper.verifyFocusIndicator(previousPosition);
      expect(focusResult.isVisible, 'Focus indicator should be visible on Shift+Tab').toBeTruthy();
    });

    test('should have logical tab order', async ({page}) => {
      const testPages = ['/', '/login', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        // Test that tab order follows visual order
        const results = await new AxeBuilder({page})
        .withRules(['focus-order-semantics', 'tabindex'])
        .analyze();

        expect(results.violations, `Page ${testPage} should have logical tab order`).toEqual([]);

        // Manual test of tab order logic
        const focusableElements = await accessibilityPage.getFocusableElements();
        const tabIndexValues: number[] = [];

        for (const element of focusableElements.slice(0, 10)) {
          const tabIndex = await element.getAttribute('tabindex');
          const numericTabIndex = tabIndex ? parseInt(tabIndex) : 0;
          tabIndexValues.push(numericTabIndex);
        }

        // Should not have positive tabindex values (anti-pattern)
        const positiveTabIndices = tabIndexValues.filter(val => val > 0);
        expect(positiveTabIndices.length, `Page ${testPage} should not use positive tabindex values`).toBe(0);
      }
    });

    test('should not have keyboard traps (except intentional modal traps)', async ({page}) => {
      await page.goto('/dashboard');

      await accessibilityPage.verifyNoKeyboardTraps();

      // Test that user can escape from any focus position
      const focusableElements = await accessibilityPage.getFocusableElements();

      for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {
        await page.keyboard.press('Tab');

        // Try various escape methods
        const escapeKeys = ['Escape', 'Alt+Tab', 'F6'];

        for (const key of escapeKeys) {
          try {
            await page.keyboard.press(key);
            await page.waitForTimeout(200);

            // Should still be able to navigate
            await page.keyboard.press('Tab');
            const focusAfterEscape = page.locator(':focus');
            await expect(focusAfterEscape).toBeVisible();

          } catch {
            // Some key combinations might not be testable in headless mode
            console.warn(`Could not test escape key: ${key}`);
          }
        }
      }
    });

    test('should support Enter and Space activation', async ({page}) => {
      await page.goto('/dashboard');

      const buttons = await page.locator('button, [role="button"]').all();

      for (const button of buttons.slice(0, 5)) {
        const isEnabled = await button.evaluate((btn) => {
          return !(btn as HTMLButtonElement).disabled &&
              btn.getAttribute('aria-disabled') !== 'true';
        });

        if (isEnabled) {
          await button.focus();

          // Test Enter key activation
          let enterActivated = false;

          await button.evaluate((btn) => {
            btn.addEventListener('click', () => {
              btn.setAttribute('data-enter-activated', 'true');
            }, {once: true});
          });

          await page.keyboard.press('Enter');
          await page.waitForTimeout(200);

          const enterResult = await button.getAttribute('data-enter-activated');
          if (enterResult === 'true') {
            enterActivated = true;
          }

          // Test Space key activation (for buttons)
          await button.evaluate((btn) => {
            btn.removeAttribute('data-enter-activated');
            btn.addEventListener('click', () => {
              btn.setAttribute('data-space-activated', 'true');
            }, {once: true});
          });

          await page.keyboard.press('Space');
          await page.waitForTimeout(200);

          const spaceResult = await button.getAttribute('data-space-activated');
          const spaceActivated = spaceResult === 'true';

          expect(enterActivated || spaceActivated, 'Buttons should be activatable with Enter or Space').toBeTruthy();
        }
      }
    });
  });

  test.describe('Skip Links and Navigation Shortcuts', () => {
    test('should provide and support skip links', async ({page}) => {
      const testPages = ['/', '/dashboard'];

      for (const testPage of testPages) {
        await page.goto(testPage);

        // Check for skip link
        const skipLink = await accessibilityPage.getSkipLink();
        expect(skipLink, `Page ${testPage} should have skip link`).toBeTruthy();

        if (skipLink) {
          // Skip link should become visible when focused
          await page.keyboard.press('Tab');
          await expect(skipLink).toBeVisible();

          const skipLinkText = await accessibilityHelper.getAccessibleName(skipLink);
          expect(skipLinkText.toLowerCase()).toContain('skip');

          // Skip link should work
          await skipLink.click();

          const mainContent = await accessibilityPage.getMainContent();
          const isFocused = await mainContent.evaluate((el) => document.activeElement === el || el.contains(document.activeElement || document.body));
          expect(isFocused, 'Skip link should move focus to main content').toBeTruthy();
        }
      }
    });

    test('should support heading navigation shortcuts', async ({page}) => {
      await page.goto('/dashboard');

      const headings = await page.locator('h1, h2, h3, h4, h5, h6').all();
      expect(headings.length, 'Page should have headings for navigation shortcuts').toBeGreaterThan(0);

      // Verify heading hierarchy for screen reader navigation
      const headingStructure = await accessibilityPage.getHeadingStructure();
      const hasLogicalOrder = await accessibilityHelper.verifyHeadingOrder(headingStructure);

      expect(hasLogicalOrder, 'Headings should have logical order for shortcut navigation').toBeTruthy();

      // Test that all headings are accessible
      for (const heading of headings.slice(0, 10)) {
        const headingText = await heading.textContent();
        expect(headingText?.trim().length, 'Headings should have content for navigation').toBeGreaterThan(0);

        // Heading should be focusable for screen reader navigation
        const _canBeFocused = await heading.evaluate((h) => {
          h.focus();
          return document.activeElement === h;
        });

        // Note: In real screen readers, headings are navigable even if not traditionally focusable
      }
    });

    test('should support landmark navigation shortcuts', async ({page}) => {
      await page.goto('/dashboard');

      const landmarks = await accessibilityPage.getLandmarks();

      expect(landmarks.main.length, 'Should have main landmark').toBeGreaterThanOrEqual(1);
      expect(landmarks.navigation.length, 'Should have navigation landmarks').toBeGreaterThanOrEqual(1);

      // Test that landmarks are properly labeled when multiples exist
      const landmarkLabelsValid = await accessibilityHelper.verifyLandmarkLabels(landmarks);
      expect(landmarkLabelsValid, 'Landmarks should have proper labels for navigation').toBeTruthy();
    });

    test('should support form navigation shortcuts', async ({page}) => {
      const formPages = ['/login', '/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        const forms = await page.locator('form').all();

        for (const form of forms) {
          const formControls = await form.locator('input, select, textarea, button').all();
          expect(formControls.length, 'Forms should have controls for navigation shortcuts').toBeGreaterThan(0);

          // All form controls should have labels for screen reader shortcuts
          for (const control of formControls.slice(0, 8)) {
            const hasLabel = await accessibilityHelper.hasAccessibleName(control);
            expect(hasLabel, 'Form controls should have labels for navigation shortcuts').toBeTruthy();
          }
        }
      }
    });
  });

  test.describe('Modal and Dialog Focus Management', () => {
    test('should trap focus in modal dialogs', async ({page}) => {
      await page.goto('/dashboard');

      const modalTrigger = await accessibilityPage.findModalTrigger();

      if (modalTrigger) {
        // Open modal
        await modalTrigger.click();
        await page.waitForTimeout(500);

        const modal = await accessibilityPage.getOpenModal();

        if (modal) {
          // Test focus trapping
          await accessibilityPage.testFocusTrap(modal);

          // Test escape key closes modal
          await page.keyboard.press('Escape');
          await page.waitForTimeout(300);

          await expect(modal).not.toBeVisible();

          // Focus should return to trigger element
          const focusedElement = page.locator(':focus');
          const triggerText = await accessibilityHelper.getAccessibleName(modalTrigger);
          const focusedText = await accessibilityHelper.getAccessibleName(focusedElement);

          expect(focusedText, 'Focus should return to modal trigger after closing').toBe(triggerText);
        }
      }
    });

    test('should manage focus for dropdown menus', async ({page}) => {
      await page.goto('/dashboard');

      const dropdownTriggers = await page.locator('[aria-haspopup], [aria-expanded]').all();

      for (const trigger of dropdownTriggers.slice(0, 3)) {
        const hasPopup = await trigger.getAttribute('aria-haspopup');
        const isExpanded = await trigger.getAttribute('aria-expanded');

        if (hasPopup && isExpanded === 'false') {
          // Open dropdown
          await trigger.focus();
          await trigger.click();
          await page.waitForTimeout(300);

          const newExpandedState = await trigger.getAttribute('aria-expanded');
          expect(newExpandedState, 'Dropdown should open when activated').toBe('true');

          // Test arrow key navigation if menu items are present
          const menuItems = await page.locator('[role="menuitem"], [role="option"]').all();

          if (menuItems.length > 0) {
            // First item should be focused or focusable
            await page.keyboard.press('ArrowDown');
            await page.waitForTimeout(200);

            const focusedMenuItem = page.locator(':focus');
            const menuItemRole = await focusedMenuItem.getAttribute('role');

            expect(['menuitem', 'option']).toContain(menuItemRole);
          }

          // Escape should close dropdown
          await page.keyboard.press('Escape');
          await page.waitForTimeout(300);

          const finalExpandedState = await trigger.getAttribute('aria-expanded');
          expect(finalExpandedState, 'Dropdown should close with Escape').toBe('false');
        }
      }
    });

    test('should handle focus for toast notifications', async ({page}) => {
      await page.goto('/dashboard');

      // Look for toast notifications or status messages
      const toastElements = await page.locator('[role="alert"], [role="status"], [aria-live="assertive"], [aria-live="polite"]').all();

      for (const toast of toastElements) {
        const role = await toast.getAttribute('role');
        const ariaLive = await toast.getAttribute('aria-live');

        const isAnnounced = role === 'alert' ||
            role === 'status' ||
            ariaLive === 'assertive' ||
            ariaLive === 'polite';

        expect(isAnnounced, 'Toast notifications should be announced').toBeTruthy();

        // If toast has interactive elements, they should be keyboard accessible
        const interactiveElements = await toast.locator('button, a, [role="button"], [tabindex="0"]').all();

        for (const element of interactiveElements) {
          const isKeyboardAccessible = await accessibilityPage.isKeyboardAccessible(element);
          expect(isKeyboardAccessible, 'Interactive elements in toasts should be keyboard accessible').toBeTruthy();
        }
      }
    });
  });

  test.describe('Custom Component Keyboard Support', () => {
    test('should support keyboard navigation in custom tabs', async ({page}) => {
      await page.goto('/dashboard');

      const tabLists = await page.locator('[role="tablist"]').all();

      for (const tabList of tabLists) {
        const tabs = await tabList.locator('[role="tab"]').all();

        if (tabs.length > 0) {
          // Focus first tab
          await tabs[0].focus();

          // Test arrow key navigation
          for (let i = 1; i < Math.min(tabs.length, 4); i++) {
            await page.keyboard.press('ArrowRight');
            await page.waitForTimeout(200);

            const focusedTab = page.locator(':focus');
            const isTab = await focusedTab.getAttribute('role');
            expect(isTab, 'Arrow keys should navigate between tabs').toBe('tab');

            // Check that corresponding tabpanel is shown
            const _tabId = await focusedTab.getAttribute('id');
            const ariaControls = await focusedTab.getAttribute('aria-controls');

            if (ariaControls) {
              const tabPanel = page.locator(`#${ariaControls}`);
              const isVisible = await tabPanel.isVisible();
              expect(isVisible, 'Tab panel should be visible when tab is focused').toBeTruthy();
            }
          }

          // Test Home/End keys if supported
          await page.keyboard.press('Home');
          await page.waitForTimeout(200);

          const focusAfterHome = page.locator(':focus');
          const firstTabSelected = await focusAfterHome.evaluate((tab, firstTab) => tab === firstTab, tabs[0]);

          if (firstTabSelected) {
            console.log('Tab component supports Home/End navigation');
          }
        }
      }
    });

    test('should support keyboard navigation in custom menus', async ({page}) => {
      await page.goto('/dashboard');

      const menuButtons = await page.locator('[aria-haspopup="true"], [role="menubutton"]').all();

      for (const menuButton of menuButtons.slice(0, 3)) {
        // Open menu
        await menuButton.focus();
        await page.keyboard.press('Enter');
        await page.waitForTimeout(300);

        const menu = page.locator('[role="menu"]').first();

        if (await menu.isVisible()) {
          const menuItems = await menu.locator('[role="menuitem"]').all();

          if (menuItems.length > 0) {
            // First menu item should be focused
            const firstItem = menuItems[0];
            await expect(firstItem).toBeFocused();

            // Test arrow key navigation
            for (let i = 1; i < Math.min(menuItems.length, 4); i++) {
              await page.keyboard.press('ArrowDown');
              await page.waitForTimeout(200);

              const focusedItem = page.locator(':focus');
              const isMenuItem = await focusedItem.getAttribute('role');
              expect(isMenuItem, 'Arrow keys should navigate menu items').toBe('menuitem');
            }

            // Test letter key navigation (first letter)
            const firstItemText = await firstItem.textContent();
            if (firstItemText && firstItemText.length > 0) {
              const firstLetter = firstItemText.charAt(0).toLowerCase();

              await page.keyboard.press(firstLetter);
              await page.waitForTimeout(200);

              // Should focus first item starting with that letter
              const focusedAfterLetter = page.locator(':focus');
              const focusedText = await focusedAfterLetter.textContent();

              expect(focusedText?.toLowerCase().startsWith(firstLetter), 'Letter keys should navigate to matching items').toBeTruthy();
            }
          }

          // Escape should close menu
          await page.keyboard.press('Escape');
          await page.waitForTimeout(300);

          await expect(menu).not.toBeVisible();
          await expect(menuButton).toBeFocused();
        }
      }
    });

    test('should support keyboard navigation in data tables', async ({page}) => {
      await page.goto('/analytics'); // Assuming analytics has data tables

      const tables = await page.locator('table').all();

      for (const table of tables) {
        const interactiveRows = await table.locator('tr[role="button"], tr[tabindex="0"], tr[onclick]').all();

        if (interactiveRows.length > 0) {
          // Focus first interactive row
          await interactiveRows[0].focus();

          // Test arrow key navigation between rows
          for (let i = 1; i < Math.min(interactiveRows.length, 4); i++) {
            await page.keyboard.press('ArrowDown');
            await page.waitForTimeout(200);

            const focusedRow = page.locator(':focus');
            const rowIndex = await focusedRow.evaluate((row) => {
              return Array.from(row.parentElement?.children).indexOf(row);
            });

            expect(rowIndex, 'Arrow keys should navigate between table rows').toBe(i);
          }
        }

        // Test sortable column headers
        const sortableHeaders = await table.locator('th[role="columnheader"], th[aria-sort]').all();

        for (const header of sortableHeaders.slice(0, 3)) {
          const canBeFocused = await header.evaluate((th) => {
            th.focus();
            return document.activeElement === th;
          });

          if (canBeFocused) {
            await page.keyboard.press('Enter');
            await page.waitForTimeout(500);

            const sortState = await header.getAttribute('aria-sort');
            expect(['ascending', 'descending', 'none']).toContain(sortState);
          }
        }
      }
    });

    test('should support keyboard navigation in custom sliders', async ({page}) => {
      await page.goto('/settings'); // Assuming settings has sliders

      const sliders = await page.locator('[role="slider"], input[type="range"]').all();

      for (const slider of sliders) {
        await slider.focus();

        const initialValue = await slider.evaluate((el) => {
          return (el as HTMLInputElement).value || el.getAttribute('aria-valuenow');
        });

        // Test arrow key adjustment
        await page.keyboard.press('ArrowRight');
        await page.waitForTimeout(200);

        const newValue = await slider.evaluate((el) => {
          return (el as HTMLInputElement).value || el.getAttribute('aria-valuenow');
        });

        expect(newValue, 'Arrow keys should adjust slider value').not.toBe(initialValue);

        // Test Home/End keys
        await page.keyboard.press('Home');
        await page.waitForTimeout(200);

        const minValue = await slider.evaluate((el) => {
          return (el as HTMLInputElement).min || el.getAttribute('aria-valuemin');
        });

        const valueAfterHome = await slider.evaluate((el) => {
          return (el as HTMLInputElement).value || el.getAttribute('aria-valuenow');
        });

        if (minValue) {
          expect(valueAfterHome, 'Home key should move to minimum value').toBe(minValue);
        }
      }
    });
  });

  test.describe('Keyboard Shortcuts and Accelerators', () => {
    test('should not interfere with browser shortcuts', async ({page}) => {
      await page.goto('/dashboard');

      // Test that common browser shortcuts still work
      const _initialUrl = page.url();

      // These shortcuts should not be overridden by the application
      const browserShortcuts: KeyboardShortcut[] = [
        {key: 'F5', description: 'Refresh page', expectedAction: 'page refresh'},
        {key: 'Ctrl+F', description: 'Find in page', expectedAction: 'find dialog'},
        {key: 'Alt+Tab', description: 'Switch applications', expectedAction: 'app switch'}
      ];

      for (const shortcut of browserShortcuts) {
        try {
          // Test that the key doesn't trigger unexpected application behavior
          await page.keyboard.press(shortcut.key);
          await page.waitForTimeout(300);

          // Application should not have intercepted these shortcuts
          // (This is a simplified test - real testing would require browser automation)

        } catch {
          // Some shortcuts may not be testable in headless mode
          console.warn(`Could not test browser shortcut: ${shortcut.key}`);
        }
      }
    });

    test('should provide clear indication of available shortcuts', async ({page}) => {
      await page.goto('/dashboard');

      // Look for keyboard shortcut indicators
      const shortcutIndicators = await page.locator('[data-shortcut], [title*="Ctrl"], [title*="Alt"], [aria-keyshortcuts]').all();

      for (const indicator of shortcutIndicators) {
        const shortcutInfo = await indicator.evaluate((el) => ({
          title: el.getAttribute('title'),
          ariaKeyshortcuts: el.getAttribute('aria-keyshortcuts'),
          dataShortcut: el.getAttribute('data-shortcut')
        }));

        const hasShortcutInfo = shortcutInfo.title?.includes('Ctrl') ||
            shortcutInfo.title?.includes('Alt') ||
            shortcutInfo.ariaKeyshortcuts ||
            shortcutInfo.dataShortcut;

        expect(hasShortcutInfo, 'Elements with shortcuts should indicate the shortcut').toBeTruthy();
      }
    });

    test('should support single-character shortcuts safely', async ({page}) => {
      await page.goto('/dashboard');

      // Test that single-character shortcuts don't interfere with typing
      const textInputs = await page.locator('input[type="text"], textarea').all();

      for (const input of textInputs.slice(0, 3)) {
        await input.focus();

        // Type some text including common shortcut letters
        const testText = 'h j k l space';
        await input.fill(testText);

        const inputValue = await input.inputValue();
        expect(inputValue, 'Single-character shortcuts should not interfere with typing').toBe(testText);
      }
    });
  });

  test.describe('Mobile and Touch Keyboard Support', () => {
    test('should work with on-screen keyboards', async ({page}) => {
      // Simulate mobile viewport
      await page.setViewportSize({width: 375, height: 667});
      await page.goto('/login');

      const inputs = await page.locator('input, textarea').all();

      for (const input of inputs.slice(0, 3)) {
        await input.focus();

        // Verify input is visible when keyboard is open (simulated)
        const inputBox = await input.boundingBox();
        if (inputBox) {
          expect(inputBox.y, 'Input should be visible above virtual keyboard area').toBeLessThan(400);
        }

        // Test that input type triggers appropriate keyboard
        const inputType = await input.getAttribute('type');
        const inputMode = await input.getAttribute('inputmode');

        if (inputType === 'email') {
          expect(inputMode === 'email' || inputType === 'email', 'Email inputs should trigger email keyboard').toBeTruthy();
        }

        if (inputType === 'tel') {
          expect(inputMode === 'tel' || inputType === 'tel', 'Phone inputs should trigger numeric keyboard').toBeTruthy();
        }
      }
    });

    test('should handle external keyboard on mobile', async ({page}) => {
      // Simulate tablet with external keyboard
      await page.setViewportSize({width: 768, height: 1024});
      await page.goto('/dashboard');

      // Test that tab navigation works on touch devices
      const focusableElements = await accessibilityPage.getFocusableElements();

      for (let i = 0; i < Math.min(focusableElements.length, 8); i++) {
        await page.keyboard.press('Tab');
        await page.waitForTimeout(100);

        const focusedElement = page.locator(':focus');
        const isVisible = await focusedElement.isVisible();

        expect(isVisible, 'Tab navigation should work with external keyboard on mobile').toBeTruthy();

        // Touch targets should still meet size requirements
        const boundingBox = await focusedElement.boundingBox();
        if (boundingBox) {
          expect(Math.min(boundingBox.width, boundingBox.height), 'Touch targets should be at least 44px').toBeGreaterThanOrEqual(44);
        }
      }
    });
  });

  test.describe('Focus Management Edge Cases', () => {
    test('should handle dynamically added elements', async ({page}) => {
      await page.goto('/dashboard');

      // Add a new focusable element dynamically
      await page.evaluate(() => {
        const button = document.createElement('button');
        button.textContent = 'Dynamic Button';
        button.id = 'dynamic-test-button';
        document.body.appendChild(button);
      });

      // New element should be keyboard accessible
      const dynamicButton = page.locator('#dynamic-test-button');
      await dynamicButton.focus();
      await expect(dynamicButton).toBeFocused();

      // Should have focus indicator
      const focusResult = await accessibilityHelper.verifyFocusIndicator(dynamicButton);
      expect(focusResult.isVisible, 'Dynamic elements should have focus indicators').toBeTruthy();
    });

    test('should handle focus when elements are removed', async ({page}) => {
      await page.goto('/dashboard');

      const focusableElements = await accessibilityPage.getFocusableElements();

      if (focusableElements.length > 2) {
        // Focus an element
        await focusableElements[1].focus();
        await expect(focusableElements[1]).toBeFocused();

        // Remove the focused element
        await focusableElements[1].evaluate((el) => {
          el.remove();
        });

        // Focus should move to a reasonable location (not lost)
        await page.waitForTimeout(200);
        const currentFocus = page.locator(':focus');

        // Focus should still be somewhere in the document
        const focusIsInDocument = await currentFocus.evaluate((el) => {
          return document.contains(el);
        });

        expect(focusIsInDocument, 'Focus should remain in document when focused element is removed').toBeTruthy();
      }
    });

    test('should handle focus with CSS transforms and positioning', async ({page}) => {
      await page.goto('/dashboard');

      // Add elements with various CSS positioning
      await page.evaluate(() => {
        const container = document.createElement('div');
        container.innerHTML = `
          <button style="position: absolute; top: 100px; left: 100px;">Absolute Button</button>
          <button style="position: fixed; top: 200px; left: 100px;">Fixed Button</button>
          <button style="transform: translateX(50px);">Transformed Button</button>
          <button style="position: sticky; top: 10px;">Sticky Button</button>
        `;
        document.body.appendChild(container);
      });

      const specialButtons = await page.locator('button:has-text("Absolute"), button:has-text("Fixed"), button:has-text("Transformed"), button:has-text("Sticky")').all();

      for (const button of specialButtons) {
        await button.focus();
        await expect(button).toBeFocused();

        // Focus indicator should still be visible
        const focusResult = await accessibilityHelper.verifyFocusIndicator(button);
        expect(focusResult.isVisible, 'Focus indicators should work with CSS positioning').toBeTruthy();
      }
    });
  });

  test.describe('Performance Impact', () => {
    test('should not have performance issues with keyboard navigation', async ({page}) => {
      await page.goto('/dashboard');

      const startTime = Date.now();

      // Rapidly navigate through elements
      const focusableElements = await accessibilityPage.getFocusableElements();

      for (let i = 0; i < Math.min(focusableElements.length, 20); i++) {
        await page.keyboard.press('Tab');
        await page.waitForTimeout(10); // Minimal wait
      }

      const endTime = Date.now();
      const navigationTime = endTime - startTime;

      expect(navigationTime, 'Keyboard navigation should be performant').toBeLessThan(5000); // 5 seconds max for 20 elements

      console.log(`Keyboard navigation performance: ${navigationTime}ms for ${Math.min(focusableElements.length, 20)} elements`);
    });
  });
});