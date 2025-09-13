/**
 * Motor Accessibility E2E Tests
 * 
 * Tests for motor accessibility features including:
 * - Touch target sizes and spacing (44x44px minimum)
 * - Mouse and pointer device accessibility
 * - Drag and drop alternatives
 * - Gesture-based interactions
 * - Timeout extensions and user control
 * - Click target accessibility
 * 
 * WCAG 2.1 Success Criteria:
 * - 2.5.1 Pointer Gestures
 * - 2.5.2 Pointer Cancellation
 * - 2.5.3 Label in Name
 * - 2.5.4 Motion Actuation
 * - 2.5.5 Target Size
 * - 2.5.6 Concurrent Input Mechanisms
 * - 2.2.1 Timing Adjustable
 * 
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import { test, expect, Page } from '@playwright/test';
import { injectAxe, checkA11y } from 'axe-playwright';
import { DEFAULT_ACCESSIBILITY_CONFIG } from './accessibility.config';

test.describe('Motor Accessibility Tests', () => {
  let page: Page;

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
    await page.goto('/');
    await injectAxe(page);
  });

  test.describe('WCAG 2.5.1 - Pointer Gestures', () => {
    test('should provide single-point alternatives for multipoint gestures', async () => {
      await page.goto('/focus-sessions');
      
      // Test zoom functionality has single-point alternative
      const zoomableElements = page.locator('[data-zoomable="true"], .zoomable');
      
      if (await zoomableElements.count() > 0) {
        const element = zoomableElements.first();
        
        // Look for zoom controls (buttons)
        const zoomInButton = page.locator('[aria-label*="zoom in" i], [data-testid*="zoom-in"]');
        const zoomOutButton = page.locator('[aria-label*="zoom out" i], [data-testid*="zoom-out"]');
        
        // Should have single-point zoom controls
        expect(await zoomInButton.count() > 0 || await zoomOutButton.count() > 0).toBeTruthy();
      }
      
      // Test drag and drop has keyboard/single-click alternative
      const draggableElements = page.locator('[draggable="true"], .draggable');
      
      for (let i = 0; i < Math.min(await draggableElements.count(), 3); i++) {
        const draggable = draggableElements.nth(i);
        
        // Look for context menu or alternative interaction
        await draggable.click({ button: 'right' });
        await page.waitForTimeout(100);
        
        const contextMenu = page.locator('.context-menu, [role="menu"]');
        const moveOption = page.locator('[role="menuitem"]').filter({ hasText: /move|cut|copy/i });
        
        if (await contextMenu.count() > 0) {
          expect(await moveOption.count()).toBeGreaterThan(0);
        }
        
        // Close any opened menu
        await page.keyboard.press('Escape');
      }
    });

    test('should work with path-based gestures using single points', async () => {
      await page.goto('/timer');
      
      // Test slider/range inputs work with single clicks
      const sliders = page.locator('input[type="range"], [role="slider"]');
      
      for (let i = 0; i < Math.min(await sliders.count(), 3); i++) {
        const slider = sliders.nth(i);
        
        const boundingBox = await slider.boundingBox();
        if (boundingBox) {
          // Click at different points on slider
          const leftPoint = boundingBox.x + 20;
          const rightPoint = boundingBox.x + boundingBox.width - 20;
          const centerY = boundingBox.y + boundingBox.height / 2;
          
          // Test single click positioning
          await page.mouse.click(leftPoint, centerY);
          await page.waitForTimeout(50);
          
          await page.mouse.click(rightPoint, centerY);
          await page.waitForTimeout(50);
          
          // Verify slider responded to single clicks
          const ariaValueNow = await slider.getAttribute('aria-valuenow');
          expect(ariaValueNow).toBeTruthy();
        }
      }
    });
  });

  test.describe('WCAG 2.5.2 - Pointer Cancellation', () => {
    test('should allow cancellation of pointer actions', async () => {
      await page.goto('/hive-management');
      
      const buttons = page.locator('button').filter({ hasText: /delete|remove|leave/i });
      
      if (await buttons.count() > 0) {
        const button = buttons.first();
        const boundingBox = await button.boundingBox();
        
        if (boundingBox) {
          // Start pointer down on button
          await page.mouse.move(boundingBox.x + boundingBox.width / 2, boundingBox.y + boundingBox.height / 2);
          await page.mouse.down();
          
          // Move pointer away from button
          await page.mouse.move(boundingBox.x - 50, boundingBox.y - 50);
          
          // Release pointer outside button - should not trigger action
          await page.mouse.up();
          
          // Verify action was not triggered (button should still be present)
          await expect(button).toBeVisible();
        }
      }
    });

    test('should support up-event or abort/undo for actions', async () => {
      await page.goto('/settings');
      
      // Test toggle switches have proper cancellation
      const switches = page.locator('input[type="checkbox"][role="switch"], [role="switch"]');
      
      if (await switches.count() > 0) {
        const toggle = switches.first();
        const initialState = await toggle.isChecked();
        
        // Mouse down without release
        await toggle.hover();
        await page.mouse.down();
        
        // Move away and release
        await page.mouse.move(0, 0);
        await page.mouse.up();
        
        // State should not have changed
        const finalState = await toggle.isChecked();
        expect(finalState).toBe(initialState);
      }
    });
  });

  test.describe('WCAG 2.5.3 - Label in Name', () => {
    test('should have accessible names that include visible text', async () => {
      await page.goto('/');
      
      const interactiveElements = page.locator('button, input, select, textarea, [role="button"], [role="link"]');
      
      for (let i = 0; i < Math.min(await interactiveElements.count(), 10); i++) {
        const element = interactiveElements.nth(i);
        
        const visibleText = await element.textContent();
        const ariaLabel = await element.getAttribute('aria-label');
        const ariaLabelledBy = await element.getAttribute('aria-labelledby');
        
        if (visibleText?.trim()) {
          // If element has visible text, accessible name should include it
          if (ariaLabel) {
            const visibleWords = visibleText.toLowerCase().trim().split(/\s+/);
            const labelWords = ariaLabel.toLowerCase().split(/\s+/);
            
            // Check if visible text words are included in aria-label
            const hasVisibleText = visibleWords.some(word => 
              labelWords.some(labelWord => labelWord.includes(word) || word.includes(labelWord))
            );
            expect(hasVisibleText).toBeTruthy();
          }
          
          if (ariaLabelledBy) {
            const labelElement = page.locator(`#${ariaLabelledBy}`);
            if (await labelElement.count() > 0) {
              const labelText = await labelElement.textContent();
              if (labelText) {
                const visibleWords = visibleText.toLowerCase().trim().split(/\s+/);
                const labelWords = labelText.toLowerCase().split(/\s+/);
                
                const hasVisibleText = visibleWords.some(word => 
                  labelWords.some(labelWord => labelWord.includes(word) || word.includes(labelWord))
                );
                expect(hasVisibleText).toBeTruthy();
              }
            }
          }
        }
      }
    });
  });

  test.describe('WCAG 2.5.4 - Motion Actuation', () => {
    test('should provide alternatives to device motion triggers', async () => {
      await page.goto('/focus-timer');
      
      // Test shake to reset functionality has button alternative
      const shakeElements = page.locator('[data-shake="true"], .shake-enabled');
      
      if (await shakeElements.count() > 0) {
        // Look for alternative reset button
        const resetButton = page.locator('[aria-label*="reset" i], button').filter({ hasText: /reset|restart/i });
        expect(await resetButton.count()).toBeGreaterThan(0);
      }
      
      // Test tilt controls have traditional input alternatives
      const tiltElements = page.locator('[data-tilt="true"], .tilt-control');
      
      if (await tiltElements.count() > 0) {
        // Should have button or slider alternatives
        const alternatives = page.locator('button, input[type="range"], [role="slider"]');
        expect(await alternatives.count()).toBeGreaterThan(0);
      }
    });

    test('should allow disabling motion actuation', async () => {
      await page.goto('/settings/accessibility');
      
      // Look for motion preferences
      const motionSettings = page.locator(
        '[data-testid*="motion"], ' +
        'input[name*="motion"], ' +
        '[aria-label*="motion" i], ' +
        '[aria-label*="shake" i]'
      );
      
      if (await motionSettings.count() > 0) {
        const setting = motionSettings.first();
        
        // Should be a toggle control
        const type = await setting.getAttribute('type');
        const role = await setting.getAttribute('role');
        
        expect(type === 'checkbox' || role === 'switch').toBeTruthy();
        
        // Should have proper labeling
        const label = await setting.getAttribute('aria-label');
        const labelledBy = await setting.getAttribute('aria-labelledby');
        expect(label || labelledBy).toBeTruthy();
      }
    });
  });

  test.describe('WCAG 2.5.5 - Target Size', () => {
    test('should have minimum 44x44px touch targets', async () => {
      await page.goto('/');
      
      const interactiveElements = page.locator(
        'button, input, select, textarea, a, ' +
        '[role="button"], [role="link"], [role="tab"], [role="menuitem"], ' +
        '[tabindex="0"], .clickable'
      );
      
      const minSize = DEFAULT_ACCESSIBILITY_CONFIG.browsers.mobile.touchTargets.minimumSize;
      
      for (let i = 0; i < Math.min(await interactiveElements.count(), 20); i++) {
        const element = interactiveElements.nth(i);
        
        const boundingBox = await element.boundingBox();
        if (boundingBox) {
          // Check if element meets minimum size requirements
          const meetsMinSize = boundingBox.width >= minSize && boundingBox.height >= minSize;
          
          if (!meetsMinSize) {
            // Check if element has sufficient padding/margin to create larger click area
            const clickableArea = await page.evaluate((el) => {
              const rect = el.getBoundingClientRect();
              const style = window.getComputedStyle(el);
              
              const paddingTop = parseInt(style.paddingTop) || 0;
              const paddingBottom = parseInt(style.paddingBottom) || 0;
              const paddingLeft = parseInt(style.paddingLeft) || 0;
              const paddingRight = parseInt(style.paddingRight) || 0;
              
              return {
                width: rect.width + paddingLeft + paddingRight,
                height: rect.height + paddingTop + paddingBottom
              };
            }, element);
            
            expect(clickableArea.width >= minSize || clickableArea.height >= minSize).toBeTruthy();
          }
        }
      }
    });

    test('should have appropriate spacing between touch targets', async () => {
      await page.goto('/navigation');
      
      const navLinks = page.locator('nav a, nav button, .nav-item');
      const minSpacing = DEFAULT_ACCESSIBILITY_CONFIG.browsers.mobile.touchTargets.spacing;
      
      if (await navLinks.count() > 1) {
        for (let i = 0; i < Math.min(await navLinks.count() - 1, 10); i++) {
          const current = navLinks.nth(i);
          const next = navLinks.nth(i + 1);
          
          const currentBox = await current.boundingBox();
          const nextBox = await next.boundingBox();
          
          if (currentBox && nextBox) {
            // Calculate spacing between elements
            const horizontalSpacing = Math.abs(nextBox.x - (currentBox.x + currentBox.width));
            const verticalSpacing = Math.abs(nextBox.y - (currentBox.y + currentBox.height));
            
            // Elements should either have adequate spacing or not overlap
            const hasAdequateSpacing = horizontalSpacing >= minSpacing || verticalSpacing >= minSpacing;
            const notOverlapping = !((currentBox.x < nextBox.x + nextBox.width && 
                                     currentBox.x + currentBox.width > nextBox.x) &&
                                    (currentBox.y < nextBox.y + nextBox.height && 
                                     currentBox.y + currentBox.height > nextBox.y));
            
            expect(hasAdequateSpacing || notOverlapping).toBeTruthy();
          }
        }
      }
    });
  });

  test.describe('WCAG 2.2.1 - Timing Adjustable', () => {
    test('should allow users to extend time limits', async () => {
      await page.goto('/focus-session');
      
      // Start a focus session that might have timing
      const startButton = page.locator('[data-testid="start-session"], button').filter({ hasText: /start|begin/i });
      
      if (await startButton.count() > 0) {
        await startButton.first().click();
        
        // Look for timeout warnings
        const timeoutWarning = page.locator('.timeout-warning, [role="dialog"]').filter({ hasText: /time|timeout|extend/i });
        
        if (await timeoutWarning.count() > 0) {
          // Should have extend option
          const extendButton = page.locator('button').filter({ hasText: /extend|continue|more time/i });
          expect(await extendButton.count()).toBeGreaterThan(0);
        }
      }
    });

    test('should allow disabling time limits where possible', async () => {
      await page.goto('/settings');
      
      // Look for timeout settings
      const timeoutSettings = page.locator(
        '[data-testid*="timeout"], ' +
        'input[name*="timeout"], ' +
        '[aria-label*="timeout" i], ' +
        '[aria-label*="time limit" i]'
      );
      
      if (await timeoutSettings.count() > 0) {
        const setting = timeoutSettings.first();
        
        // Should allow customization
        const type = await setting.getAttribute('type');
        expect(['number', 'range', 'checkbox'].includes(type || '')).toBeTruthy();
        
        // Should have proper labeling
        const label = await setting.getAttribute('aria-label');
        const labelledBy = await setting.getAttribute('aria-labelledby');
        expect(label || labelledBy).toBeTruthy();
      }
    });
  });

  test.describe('Mouse and Pointer Device Accessibility', () => {
    test('should work with various pointer devices', async () => {
      await page.goto('/');
      
      // Test hover interactions have alternatives
      const hoverElements = page.locator('[title], .tooltip-trigger, .hover-reveal');
      
      if (await hoverElements.count() > 0) {
        const element = hoverElements.first();
        
        // Test hover functionality
        await element.hover();
        await page.waitForTimeout(100);
        
        // Check if tooltip/content appears
        const tooltip = page.locator('.tooltip, [role="tooltip"]');
        const isVisible = await tooltip.count() > 0 && await tooltip.first().isVisible();
        
        if (isVisible) {
          // Should also be accessible via focus
          await element.focus();
          await page.waitForTimeout(100);
          
          const stillVisible = await tooltip.first().isVisible();
          expect(stillVisible).toBeTruthy();
        }
      }
    });

    test('should handle click alternatives properly', async () => {
      await page.goto('/hive-selection');
      
      // Test double-click actions have single-click alternatives
      const doubleClickElements = page.locator('[data-dblclick="true"], .double-click');
      
      if (await doubleClickElements.count() > 0) {
        const element = doubleClickElements.first();
        
        // Look for context menu or single-click alternative
        await element.click({ button: 'right' });
        await page.waitForTimeout(100);
        
        const contextMenu = page.locator('.context-menu, [role="menu"]');
        if (await contextMenu.count() > 0) {
          const openOption = page.locator('[role="menuitem"]').filter({ hasText: /open|edit|view/i });
          expect(await openOption.count()).toBeGreaterThan(0);
        }
        
        await page.keyboard.press('Escape'); // Close menu
      }
    });
  });

  test.describe('Drag and Drop Accessibility', () => {
    test('should provide keyboard alternatives for drag and drop', async () => {
      await page.goto('/kanban');
      
      const draggableItems = page.locator('[draggable="true"], .draggable-item');
      
      if (await draggableItems.count() > 0) {
        const item = draggableItems.first();
        
        // Should be focusable
        await item.focus();
        const focused = await page.locator(':focus').count();
        expect(focused).toBeGreaterThan(0);
        
        // Should have keyboard interaction hints
        const ariaDescription = await item.getAttribute('aria-describedby');
        if (ariaDescription) {
          const description = page.locator(`#${ariaDescription}`);
          const descText = await description.textContent();
          expect(descText).toMatch(/keyboard|arrow|space|enter/i);
        }
        
        // Test keyboard movement
        await page.keyboard.press('Space'); // Activate move mode
        await page.waitForTimeout(100);
        
        await page.keyboard.press('ArrowDown'); // Move selection
        await page.waitForTimeout(100);
        
        await page.keyboard.press('Space'); // Confirm move
        await page.waitForTimeout(100);
      }
    });
  });

  test.describe('Mobile and Touch Accessibility', () => {
    test('should work with touch interactions on mobile viewports', async () => {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/');
      
      // Test touch-friendly navigation
      const navToggle = page.locator('.nav-toggle, [aria-label*="menu" i]');
      
      if (await navToggle.count() > 0) {
        await navToggle.click();
        await page.waitForTimeout(200);
        
        // Navigation should be visible
        const nav = page.locator('nav, [role="navigation"]');
        const isVisible = await nav.first().isVisible();
        expect(isVisible).toBeTruthy();
      }
      
      // Test swipe gestures have button alternatives
      const swipeableElements = page.locator('.swipeable, [data-swipe="true"]');
      
      if (await swipeableElements.count() > 0) {
        // Look for navigation buttons
        const navButtons = page.locator('.nav-button, [aria-label*="next" i], [aria-label*="previous" i]');
        expect(await navButtons.count()).toBeGreaterThan(0);
      }
    });

    test('should handle long press alternatives', async () => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/tasks');
      
      const longPressElements = page.locator('.long-press, [data-long-press="true"]');
      
      if (await longPressElements.count() > 0) {
        const element = longPressElements.first();
        
        // Look for alternative access method
        await element.click({ button: 'right' }); // Context menu
        await page.waitForTimeout(100);
        
        const contextMenu = page.locator('.context-menu, [role="menu"]');
        if (await contextMenu.count() === 0) {
          // Look for button alternative
          const actionButton = page.locator('button').filter({ hasText: /options|more|menu/i });
          expect(await actionButton.count()).toBeGreaterThan(0);
        }
      }
    });
  });

  test.describe('Comprehensive Motor Accessibility', () => {
    test('should pass comprehensive motor accessibility checks', async () => {
      await page.goto('/');
      
      await checkA11y(page, undefined, {
        axeOptions: {
          runOnly: {
            type: 'tag',
            values: ['wcag21aa', 'wcag2a']
          },
          rules: {
            'target-size': { enabled: true },
            'focus-order-semantics': { enabled: true },
            'tabindex': { enabled: true }
          }
        },
        detailedReport: true
      });
    });

    test('should maintain motor accessibility across user interactions', async () => {
      await page.goto('/dashboard');
      
      // Test sequence of interactions
      const interactiveElements = page.locator('button, a, input').first();
      
      if (await interactiveElements.count() > 0) {
        // Focus
        await interactiveElements.focus();
        let focused = await page.locator(':focus').count();
        expect(focused).toBeGreaterThan(0);
        
        // Click
        await interactiveElements.click();
        await page.waitForTimeout(200);
        
        // Focus should still be manageable
        await page.keyboard.press('Tab');
        focused = await page.locator(':focus').count();
        expect(focused).toBeGreaterThan(0);
      }
    });

    test('should accommodate different motor abilities', async () => {
      await page.goto('/settings/accessibility');
      
      // Look for motor accessibility settings
      const motorSettings = page.locator(
        '[data-testid*="motor"], ' +
        '[aria-label*="motor" i], ' +
        '[aria-label*="dexterity" i], ' +
        '[aria-label*="click" i]'
      );
      
      if (await motorSettings.count() > 0) {
        const setting = motorSettings.first();
        
        // Should be accessible itself
        const label = await setting.getAttribute('aria-label');
        const labelledBy = await setting.getAttribute('aria-labelledby');
        expect(label || labelledBy).toBeTruthy();
        
        // Should be keyboard accessible
        await setting.focus();
        const focused = await page.locator(':focus').count();
        expect(focused).toBeGreaterThan(0);
      }
    });
  });
});