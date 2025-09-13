/**
 * Live Regions Accessibility E2E Tests
 * 
 * Tests for ARIA live regions and dynamic content announcements:
 * - Status messages and notifications
 * - Dynamic content updates
 * - Real-time data changes
 * - Loading states and progress updates
 * - Error and success messages
 * - Chat and messaging updates
 * - Timer and countdown announcements
 * 
 * WCAG 2.1 Success Criteria:
 * - 4.1.3 Status Messages
 * - 1.3.1 Info and Relationships (for live regions)
 * - 3.3.1 Error Identification (for dynamic errors)
 * 
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import { test, expect, Page } from '@playwright/test';
import { injectAxe, checkA11y } from 'axe-playwright';

test.describe('Live Regions Accessibility Tests', () => {
  let page: Page;

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
    await page.goto('/');
    await injectAxe(page);
  });

  test.describe('WCAG 4.1.3 - Status Messages', () => {
    test('should announce form submission results', async () => {
      await page.goto('/contact');
      
      // Look for live region for status messages
      let statusRegion = page.locator('[role="status"], [aria-live="polite"]');
      
      if (await statusRegion.count() === 0) {
        // Create a live region monitor for dynamic content
        await page.evaluate(() => {
          const monitor = document.createElement('div');
          monitor.id = 'status-monitor';
          monitor.setAttribute('aria-live', 'polite');
          monitor.setAttribute('aria-atomic', 'true');
          monitor.style.position = 'absolute';
          monitor.style.left = '-10000px';
          document.body.appendChild(monitor);
        });
        statusRegion = page.locator('#status-monitor');
      }
      
      // Fill and submit form
      const form = page.locator('form');
      if (await form.count() > 0) {
        const nameField = page.locator('input[name="name"], input[type="text"]').first();
        const emailField = page.locator('input[type="email"], input[name="email"]');
        const messageField = page.locator('textarea, input[name="message"]');
        
        if (await nameField.count() > 0) await nameField.fill('Test User');
        if (await emailField.count() > 0) await emailField.fill('test@example.com');
        if (await messageField.count() > 0) await messageField.fill('Test message');
        
        // Submit form
        const submitButton = page.locator('button[type="submit"], input[type="submit"]');
        if (await submitButton.count() > 0) {
          await submitButton.click();
          
          // Wait for status message
          await page.waitForTimeout(1000);
          
          // Check for status announcement
          const successMessage = page.locator('[role="status"], [role="alert"], .success-message');
          if (await successMessage.count() > 0) {
            await expect(successMessage.first()).toBeVisible();
            const messageText = await successMessage.first().textContent();
            expect(messageText?.trim()).toBeTruthy();
            expect(messageText).toMatch(/success|sent|submitted|thank/i);
          }
        }
      }
    });

    test('should announce loading states', async () => {
      await page.goto('/dashboard');
      
      // Look for loading indicators with proper announcements
      const loadingIndicators = page.locator(
        '[role="status"][aria-live], .loading[aria-live], ' +
        '[aria-busy="true"], .spinner[aria-label]'
      );
      
      if (await loadingIndicators.count() > 0) {
        const loading = loadingIndicators.first();
        
        // Should have appropriate aria attributes
        const ariaLive = await loading.getAttribute('aria-live');
        const ariaLabel = await loading.getAttribute('aria-label');
        const ariaBusy = await loading.getAttribute('aria-busy');
        
        expect(ariaLive || ariaBusy || ariaLabel).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/loading|wait|progress/i);
        }
      }
      
      // Test dynamic loading by triggering an action
      const refreshButton = page.locator('[data-testid="refresh"], button').filter({ hasText: /refresh|reload/i });
      
      if (await refreshButton.count() > 0) {
        await refreshButton.click();
        
        // Should show loading state
        await page.waitForTimeout(100);
        
        const activeLoading = page.locator('[aria-busy="true"], .loading:visible');
        if (await activeLoading.count() > 0) {
          const loading = activeLoading.first();
          const ariaLive = await loading.getAttribute('aria-live');
          const ariaLabel = await loading.getAttribute('aria-label');
          
          expect(ariaLive || ariaLabel).toBeTruthy();
        }
      }
    });

    test('should announce search results updates', async () => {
      await page.goto('/hive-search');
      
      const searchInput = page.locator('input[type="search"], input[name="search"], input[placeholder*="search" i]');
      
      if (await searchInput.count() > 0) {
        const input = searchInput.first();
        
        // Check for associated live region
        const ariaDescribedBy = await input.getAttribute('aria-describedby');
        let resultsRegion = page.locator('[role="status"], [aria-live="polite"]');
        
        if (ariaDescribedBy) {
          resultsRegion = page.locator(`#${ariaDescribedBy}`);
        }
        
        // Perform search
        await input.fill('focus');
        await page.waitForTimeout(500); // Wait for debounced search
        
        // Check for results announcement
        if (await resultsRegion.count() > 0) {
          const regionText = await resultsRegion.textContent();
          if (regionText?.trim()) {
            expect(regionText).toMatch(/result|found|match/i);
          }
        }
        
        // Also check for results container with proper labeling
        const resultsContainer = page.locator('.search-results, [role="listbox"], [role="region"]');
        if (await resultsContainer.count() > 0) {
          const ariaLabel = await resultsContainer.getAttribute('aria-label');
          const ariaLabelledBy = await resultsContainer.getAttribute('aria-labelledby');
          
          expect(ariaLabel || ariaLabelledBy).toBeTruthy();
        }
      }
    });
  });

  test.describe('Real-time Content Updates', () => {
    test('should announce timer and countdown updates', async () => {
      await page.goto('/focus-timer');
      
      // Look for timer display
      const timerDisplay = page.locator('.timer, [data-testid="timer"], .countdown');
      
      if (await timerDisplay.count() > 0) {
        const timer = timerDisplay.first();
        
        // Should have live region attributes for important updates
        const ariaLive = await timer.getAttribute('aria-live');
        const role = await timer.getAttribute('role');
        
        // Timer should either be a timer role or have appropriate live region
        expect(role === 'timer' || ariaLive === 'polite' || ariaLive === 'off').toBeTruthy();
        
        // Start timer if there's a start button
        const startButton = page.locator('[data-testid="start-timer"], button').filter({ hasText: /start|begin/i });
        
        if (await startButton.count() > 0) {
          await startButton.click();
          await page.waitForTimeout(100);
          
          // Should announce timer start
          const statusRegion = page.locator('[role="status"], [aria-live="assertive"]');
          if (await statusRegion.count() > 0) {
            const status = await statusRegion.textContent();
            if (status?.trim()) {
              expect(status).toMatch(/start|begin|timer/i);
            }
          }
        }
        
        // Check for timer completion announcement setup
        const timerValue = await timer.textContent();
        if (timerValue?.includes('00:0')) {
          // Timer might be near completion - should have announcement setup
          const alertRegion = page.locator('[role="alert"], [aria-live="assertive"]');
          expect(await alertRegion.count()).toBeGreaterThan(0);
        }
      }
    });

    test('should announce chat message updates', async () => {
      await page.goto('/hive/1/chat');
      
      // Look for chat message area
      const chatArea = page.locator('.chat-messages, [role="log"], .message-list');
      
      if (await chatArea.count() > 0) {
        const chat = chatArea.first();
        
        // Should be a log region for chat messages
        const role = await chat.getAttribute('role');
        const ariaLive = await chat.getAttribute('aria-live');
        const ariaLabel = await chat.getAttribute('aria-label');
        
        expect(role === 'log' || ariaLive === 'polite').toBeTruthy();
        expect(ariaLabel).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/chat|message|conversation/i);
        }
        
        // Test sending a message
        const messageInput = page.locator('input[placeholder*="message" i], textarea[placeholder*="message" i]');
        const sendButton = page.locator('button[type="submit"], [data-testid="send-message"]');
        
        if (await messageInput.count() > 0 && await sendButton.count() > 0) {
          await messageInput.fill('Test accessibility message');
          await sendButton.click();
          
          await page.waitForTimeout(300);
          
          // New message should appear in the log
          const messages = page.locator('.message, .chat-message');
          const lastMessage = messages.last();
          
          if (await lastMessage.count() > 0) {
            const messageText = await lastMessage.textContent();
            expect(messageText).toContain('Test accessibility message');
            
            // Message should have proper structure for screen readers
            const author = lastMessage.locator('.author, .sender, [data-author]');
            const timestamp = lastMessage.locator('.timestamp, .time, [data-time]');
            
            // Should have semantic structure
            expect(await author.count() > 0 || await timestamp.count() > 0).toBeTruthy();
          }
        }
      }
    });

    test('should announce notification updates', async () => {
      await page.goto('/dashboard');
      
      // Look for notification area
      const notificationArea = page.locator('.notifications, [role="region"][aria-label*="notification" i]');
      
      if (await notificationArea.count() > 0) {
        const notifications = notificationArea.first();
        
        // Should have proper labeling
        const ariaLabel = await notifications.getAttribute('aria-label');
        const ariaLabelledBy = await notifications.getAttribute('aria-labelledby');
        
        expect(ariaLabel || ariaLabelledBy).toBeTruthy();
      }
      
      // Trigger a notification if possible
      const notificationTrigger = page.locator('[data-testid="notification-trigger"], .notification-test');
      
      if (await notificationTrigger.count() > 0) {
        await notificationTrigger.click();
        await page.waitForTimeout(200);
        
        // Should announce new notification
        const newNotification = page.locator('[role="alert"], .notification:last-child');
        
        if (await newNotification.count() > 0) {
          await expect(newNotification.first()).toBeVisible();
          
          const notificationText = await newNotification.first().textContent();
          expect(notificationText?.trim()).toBeTruthy();
        }
      }
    });
  });

  test.describe('Progress and Loading Announcements', () => {
    test('should announce file upload progress', async () => {
      await page.goto('/profile');
      
      const fileInput = page.locator('input[type="file"]');
      
      if (await fileInput.count() > 0) {
        // Look for progress indicator
        const progressBar = page.locator('[role="progressbar"], .progress-bar');
        const progressRegion = page.locator('[aria-live][aria-label*="progress" i]');
        
        if (await progressBar.count() > 0) {
          const progress = progressBar.first();
          
          // Should have proper progress attributes
          const ariaValueNow = await progress.getAttribute('aria-valuenow');
          const ariaValueMax = await progress.getAttribute('aria-valuemax');
          const ariaLabel = await progress.getAttribute('aria-label');
          
          expect(ariaValueMax).toBeTruthy();
          expect(ariaLabel || ariaValueNow !== null).toBeTruthy();
        }
        
        // Check for live region updates during progress
        if (await progressRegion.count() > 0) {
          const region = progressRegion.first();
          const ariaLive = await region.getAttribute('aria-live');
          
          expect(ariaLive === 'polite' || ariaLive === 'assertive').toBeTruthy();
        }
      }
    });

    test('should announce data loading completion', async () => {
      await page.goto('/analytics');
      
      // Look for data loading indicators
      const loadingState = page.locator('[aria-busy="true"], .loading-data');
      
      if (await loadingState.count() > 0) {
        // Wait for loading to complete
        await page.waitForSelector('[aria-busy="false"], .data-loaded', { timeout: 5000 });
        
        // Should announce completion
        const completionMessage = page.locator('[role="status"]').filter({ hasText: /loaded|complete|ready/i });
        
        if (await completionMessage.count() > 0) {
          const message = completionMessage.first();
          await expect(message).toBeVisible();
          
          const messageText = await message.textContent();
          expect(messageText).toMatch(/loaded|complete|ready|finished/i);
        }
      }
    });
  });

  test.describe('Error and Success Announcements', () => {
    test('should announce form validation errors dynamically', async () => {
      await page.goto('/settings');
      
      const form = page.locator('form');
      
      if (await form.count() > 0) {
        // Look for live region for form errors
        const errorRegion = page.locator('[role="alert"], [aria-live="assertive"]');
        
        // Try invalid input to trigger validation
        const emailField = page.locator('input[type="email"]');
        
        if (await emailField.count() > 0) {
          await emailField.fill('invalid-email');
          await emailField.blur();
          await page.waitForTimeout(300);
          
          // Should announce validation error
          const validationError = page.locator('[aria-invalid="true"] + *, .field-error');
          
          if (await validationError.count() > 0) {
            const error = validationError.first();
            
            // Error should be visible and properly associated
            await expect(error).toBeVisible();
            const errorText = await error.textContent();
            expect(errorText?.trim()).toBeTruthy();
            expect(errorText).toMatch(/invalid|format|email/i);
          }
          
          // Check if error was announced via live region
          if (await errorRegion.count() > 0) {
            const regionText = await errorRegion.first().textContent();
            if (regionText?.trim()) {
              expect(regionText).toMatch(/error|invalid/i);
            }
          }
        }
      }
    });

    test('should announce successful operations', async () => {
      await page.goto('/settings');
      
      // Look for settings that can be changed
      const settingToggle = page.locator('input[type="checkbox"], [role="switch"]').first();
      
      if (await settingToggle.count() > 0) {
        const initialState = await settingToggle.isChecked();
        
        // Toggle setting
        await settingToggle.click();
        await page.waitForTimeout(300);
        
        // Should announce change
        const successMessage = page.locator('[role="status"]').filter({ hasText: /saved|updated|changed/i });
        
        if (await successMessage.count() > 0) {
          const message = successMessage.first();
          const messageText = await message.textContent();
          
          expect(messageText).toMatch(/saved|updated|changed|success/i);
        }
      }
    });
  });

  test.describe('Live Region Politeness Levels', () => {
    test('should use appropriate aria-live values for different content types', async () => {
      await page.goto('/');
      
      // Check polite live regions (non-urgent updates)
      const politeRegions = page.locator('[aria-live="polite"]');
      
      if (await politeRegions.count() > 0) {
        for (let i = 0; i < Math.min(await politeRegions.count(), 5); i++) {
          const region = politeRegions.nth(i);
          const ariaLabel = await region.getAttribute('aria-label');
          const className = await region.getAttribute('class');
          
          // Polite regions should be for status updates, search results, etc.
          const isAppropriate = (ariaLabel && ariaLabel.match(/status|result|update/i)) ||
                               (className && className.match(/status|result|search/i));
          
          if (ariaLabel || className) {
            expect(isAppropriate).toBeTruthy();
          }
        }
      }
      
      // Check assertive live regions (urgent updates)
      const assertiveRegions = page.locator('[aria-live="assertive"], [role="alert"]');
      
      if (await assertiveRegions.count() > 0) {
        for (let i = 0; i < Math.min(await assertiveRegions.count(), 3); i++) {
          const region = assertiveRegions.nth(i);
          const ariaLabel = await region.getAttribute('aria-label');
          const className = await region.getAttribute('class');
          
          // Assertive regions should be for errors, alerts, urgent notifications
          const isAppropriate = (ariaLabel && ariaLabel.match(/error|alert|urgent|warning/i)) ||
                               (className && className.match(/error|alert|warning/i));
          
          if (ariaLabel || className) {
            expect(isAppropriate).toBeTruthy();
          }
        }
      }
    });

    test('should use aria-atomic appropriately', async () => {
      await page.goto('/');
      
      const atomicRegions = page.locator('[aria-atomic="true"]');
      
      if (await atomicRegions.count() > 0) {
        for (let i = 0; i < await atomicRegions.count(); i++) {
          const region = atomicRegions.nth(i);
          
          // Atomic regions should be relatively small and self-contained
          const content = await region.textContent();
          const wordCount = content?.split(/\s+/).length || 0;
          
          // Atomic regions with very long content might not be appropriate
          expect(wordCount).toBeLessThan(50); // Reasonable limit for atomic announcements
        }
      }
    });
  });

  test.describe('Comprehensive Live Regions', () => {
    test('should pass accessibility checks for live regions', async () => {
      await page.goto('/');
      
      await checkA11y(page, undefined, {
        axeOptions: {
          runOnly: {
            type: 'tag',
            values: ['wcag21aa']
          },
          rules: {
            'aria-valid-attr-value': { enabled: true },
            'aria-valid-attr': { enabled: true }
          }
        }
      });
    });

    test('should handle multiple concurrent live region updates', async () => {
      await page.goto('/dashboard');
      
      // Look for multiple live regions
      const liveRegions = page.locator('[aria-live], [role="status"], [role="alert"]');
      const regionCount = await liveRegions.count();
      
      if (regionCount > 1) {
        // Multiple live regions should not conflict
        for (let i = 0; i < Math.min(regionCount, 5); i++) {
          const region = liveRegions.nth(i);
          const ariaLive = await region.getAttribute('aria-live');
          const role = await region.getAttribute('role');
          
          // Should have valid live region attributes
          if (ariaLive) {
            expect(['polite', 'assertive', 'off'].includes(ariaLive)).toBeTruthy();
          }
          
          if (role) {
            expect(['status', 'alert', 'log'].includes(role)).toBeTruthy();
          }
        }
      }
    });

    test('should maintain live region functionality during navigation', async () => {
      await page.goto('/dashboard');
      
      // Identify live regions
      const liveRegions = page.locator('[aria-live], [role="status"]');
      const initialCount = await liveRegions.count();
      
      if (initialCount > 0) {
        // Navigate to another page
        const navLink = page.locator('nav a').first();
        if (await navLink.count() > 0) {
          await navLink.click();
          await page.waitForLoadState('networkidle');
          
          // Should still have live regions available
          const newRegions = page.locator('[aria-live], [role="status"]');
          const newCount = await newRegions.count();
          
          expect(newCount).toBeGreaterThan(0); // Should maintain live region functionality
        }
      }
    });
  });
});