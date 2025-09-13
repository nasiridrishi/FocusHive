/**
 * WebSocket Accessibility E2E Tests
 * 
 * Tests for real-time WebSocket features accessibility:
 * - Presence status updates and announcements
 * - Real-time chat message accessibility
 * - Timer synchronization announcements
 * - Hive activity notifications
 * - Connection status communication
 * - Real-time data updates
 * - WebSocket error handling accessibility
 * 
 * WCAG 2.1 Success Criteria:
 * - 4.1.3 Status Messages (for real-time updates)
 * - 1.3.1 Info and Relationships (for real-time content structure)
 * - 2.2.2 Pause, Stop, Hide (for auto-updating content)
 * - 3.2.1 On Focus (no unexpected context changes from real-time updates)
 * 
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import { test, expect, Page } from '@playwright/test';
import { injectAxe, checkA11y } from 'axe-playwright';

test.describe('WebSocket Accessibility Tests', () => {
  let page: Page;

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
    await page.goto('/');
    await injectAxe(page);
  });

  test.describe('Real-time Presence Updates', () => {
    test('should announce user presence changes accessibly', async () => {
      await page.goto('/hive/1');
      
      // Look for presence indicators and live regions
      const presenceArea = page.locator('.presence-list, [data-testid="presence"], .user-list');
      const statusRegion = page.locator('[role="status"], [aria-live="polite"]');
      
      if (await presenceArea.count() > 0) {
        const presence = presenceArea.first();
        
        // Should have proper labeling
        const ariaLabel = await presence.getAttribute('aria-label');
        const ariaLabelledBy = await presence.getAttribute('aria-labelledby');
        
        expect(ariaLabel || ariaLabelledBy).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/presence|users|online|participants/i);
        }
        
        // Individual presence indicators should be accessible
        const presenceItems = presence.locator('.presence-item, .user-item, [data-user-id]');
        
        if (await presenceItems.count() > 0) {
          const item = presenceItems.first();
          
          // Should have status information
          const statusElement = item.locator('.status, [data-status], [aria-label*="status" i]');
          
          if (await statusElement.count() > 0) {
            const status = statusElement.first();
            const statusLabel = await status.getAttribute('aria-label');
            const statusText = await status.textContent();
            
            expect(statusLabel || statusText).toBeTruthy();
            
            if (statusLabel) {
              expect(statusLabel).toMatch(/online|offline|away|busy|focus/i);
            }
          }
        }
      }
      
      // Test presence change announcements
      if (await statusRegion.count() > 0) {
        // Monitor for presence updates
        await page.waitForTimeout(2000); // Wait for potential presence updates
        
        const statusContent = await statusRegion.first().textContent();
        
        if (statusContent?.trim()) {
          expect(statusContent).toMatch(/joined|left|online|offline|status/i);
        }
      }
    });

    test('should handle focus status updates accessibly', async () => {
      await page.goto('/focus-session');
      
      // Start a focus session
      const startButton = page.locator('[data-testid="start-focus"], button').filter({ hasText: /start|begin focus/i });
      
      if (await startButton.count() > 0) {
        await startButton.click();
        await page.waitForTimeout(500);
        
        // Should announce focus state change
        const focusStatus = page.locator('[role="status"], .focus-status[aria-live]');
        
        if (await focusStatus.count() > 0) {
          const status = focusStatus.first();
          const statusText = await status.textContent();
          
          if (statusText?.trim()) {
            expect(statusText).toMatch(/focus|session|started|active/i);
          }
          
          // Should have proper live region attributes
          const ariaLive = await status.getAttribute('aria-live');
          expect(ariaLive === 'polite' || ariaLive === 'assertive').toBeTruthy();
        }
        
        // Focus indicators should be accessible
        const focusIndicator = page.locator('.focus-indicator, [data-focus-status]');
        
        if (await focusIndicator.count() > 0) {
          const indicator = focusIndicator.first();
          
          const ariaLabel = await indicator.getAttribute('aria-label');
          const ariaDescribedBy = await indicator.getAttribute('aria-describedby');
          
          expect(ariaLabel || ariaDescribedBy).toBeTruthy();
          
          if (ariaLabel) {
            expect(ariaLabel).toMatch(/focus|concentrating|active|session/i);
          }
        }
      }
    });
  });

  test.describe('Real-time Chat Accessibility', () => {
    test('should announce new chat messages appropriately', async () => {
      await page.goto('/hive/1/chat');
      
      // Look for chat area with proper live region
      const chatArea = page.locator('.chat-messages, [role="log"]');
      
      if (await chatArea.count() > 0) {
        const chat = chatArea.first();
        
        // Should be a log region for chat messages
        const role = await chat.getAttribute('role');
        const ariaLive = await chat.getAttribute('aria-live');
        const ariaLabel = await chat.getAttribute('aria-label');
        
        expect(role === 'log' || ariaLive === 'polite').toBeTruthy();
        expect(ariaLabel).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/chat|messages|conversation/i);
        }
        
        // Test message structure
        const messages = chat.locator('.message, .chat-message');
        
        if (await messages.count() > 0) {
          const message = messages.first();
          
          // Messages should have semantic structure
          const author = message.locator('.author, .sender, [data-author]');
          const content = message.locator('.content, .text, [data-content]');
          const timestamp = message.locator('.timestamp, .time, [data-time]');
          
          // Should have at least author and content
          expect(await author.count() > 0 || await content.count() > 0).toBeTruthy();
          
          if (await author.count() > 0) {
            const authorText = await author.first().textContent();
            expect(authorText?.trim()).toBeTruthy();
          }
          
          if (await timestamp.count() > 0) {
            const time = timestamp.first();
            const timeLabel = await time.getAttribute('aria-label');
            const timeText = await time.textContent();
            
            // Timestamp should be accessible
            expect(timeLabel || timeText).toBeTruthy();
          }
        }
      }
      
      // Test sending a message
      const messageInput = page.locator('input[type="text"][placeholder*="message" i], textarea[placeholder*="message" i]');
      const sendButton = page.locator('button[type="submit"], [data-testid="send-message"]');
      
      if (await messageInput.count() > 0 && await sendButton.count() > 0) {
        await messageInput.fill('Test WebSocket accessibility message');
        await sendButton.click();
        
        await page.waitForTimeout(500);
        
        // New message should appear in the log
        const lastMessage = page.locator('.message, .chat-message').last();
        
        if (await lastMessage.count() > 0) {
          const messageText = await lastMessage.textContent();
          expect(messageText).toContain('Test WebSocket accessibility message');
          
          // Should maintain proper chat log structure
          const chatArea = page.locator('[role="log"]');
          if (await chatArea.count() > 0) {
            const ariaLabel = await chatArea.getAttribute('aria-label');
            expect(ariaLabel).toMatch(/chat|message|conversation/i);
          }
        }
      }
    });

    test('should handle chat notification settings accessibly', async () => {
      await page.goto('/settings/notifications');
      
      // Look for chat notification settings
      const chatSettings = page.locator(
        '[data-testid*="chat"], [name*="chat"], ' +
        'input[aria-label*="chat" i], [aria-label*="message" i]'
      );
      
      if (await chatSettings.count() > 0) {
        const setting = chatSettings.first();
        
        // Should be properly labeled
        const ariaLabel = await setting.getAttribute('aria-label');
        const ariaLabelledBy = await setting.getAttribute('aria-labelledby');
        const id = await setting.getAttribute('id');
        
        expect(ariaLabel || ariaLabelledBy || id).toBeTruthy();
        
        if (id) {
          const label = page.locator(`label[for="${id}"]`);
          const hasLabel = await label.count() > 0;
          expect(hasLabel || ariaLabel || ariaLabelledBy).toBeTruthy();
        }
        
        // Test toggle functionality
        const type = await setting.getAttribute('type');
        if (type === 'checkbox') {
          const initialState = await setting.isChecked();
          await setting.click();
          const newState = await setting.isChecked();
          expect(newState).toBe(!initialState);
          
          // Should announce setting change
          const statusRegion = page.locator('[role="status"]');
          if (await statusRegion.count() > 0) {
            await page.waitForTimeout(200);
            const statusText = await statusRegion.first().textContent();
            if (statusText?.trim()) {
              expect(statusText).toMatch(/saved|updated|changed/i);
            }
          }
        }
      }
    });
  });

  test.describe('Real-time Timer Synchronization', () => {
    test('should synchronize timer updates accessibly across sessions', async () => {
      await page.goto('/focus-timer');
      
      // Look for synchronized timer display
      const timerDisplay = page.locator('.timer, [data-testid="timer"], [role="timer"]');
      
      if (await timerDisplay.count() > 0) {
        const timer = timerDisplay.first();
        
        // Timer should have appropriate role and labeling
        const role = await timer.getAttribute('role');
        const ariaLabel = await timer.getAttribute('aria-label');
        const ariaLive = await timer.getAttribute('aria-live');
        
        expect(role === 'timer' || ariaLabel || ariaLive).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/timer|countdown|remaining|focus/i);
        }
        
        // Timer should update but not be too disruptive
        if (ariaLive) {
          expect(ariaLive === 'off' || ariaLive === 'polite').toBeTruthy();
        }
        
        // Test timer controls accessibility
        const controls = page.locator('.timer-controls, .timer-buttons');
        
        if (await controls.count() > 0) {
          const startButton = controls.locator('button').filter({ hasText: /start|play/i });
          const pauseButton = controls.locator('button').filter({ hasText: /pause|stop/i });
          
          if (await startButton.count() > 0) {
            const button = startButton.first();
            const ariaLabel = await button.getAttribute('aria-label');
            const text = await button.textContent();
            
            expect((ariaLabel || text || '').toLowerCase()).toMatch(/start|play|begin/);
            
            // Test button functionality
            await button.click();
            await page.waitForTimeout(200);
            
            // Should announce timer start
            const statusRegion = page.locator('[role="status"]');
            if (await statusRegion.count() > 0) {
              const statusText = await statusRegion.first().textContent();
              if (statusText?.trim()) {
                expect(statusText).toMatch(/start|begin|timer|focus/i);
              }
            }
          }
        }
      }
    });

    test('should announce timer phase changes', async () => {
      await page.goto('/pomodoro-timer');
      
      // Look for phase indicators (focus, break, etc.)
      const phaseIndicator = page.locator('.phase, [data-phase], .timer-phase');
      
      if (await phaseIndicator.count() > 0) {
        const phase = phaseIndicator.first();
        
        // Should be announced to screen readers
        const ariaLive = await phase.getAttribute('aria-live');
        const role = await phase.getAttribute('role');
        const ariaLabel = await phase.getAttribute('aria-label');
        
        expect(ariaLive || role || ariaLabel).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/phase|focus|break|work|rest/i);
        }
        
        // Phase changes should be announced
        if (role === 'status' || ariaLive === 'polite') {
          const phaseText = await phase.textContent();
          expect(phaseText?.trim()).toBeTruthy();
        }
      }
      
      // Test phase transition announcements
      const completeButton = page.locator('[data-testid="complete-phase"], button').filter({ hasText: /complete|finish/i });
      
      if (await completeButton.count() > 0) {
        await completeButton.click();
        await page.waitForTimeout(500);
        
        // Should announce phase completion
        const announcement = page.locator('[role="alert"], [role="status"]');
        
        if (await announcement.count() > 0) {
          const announceText = await announcement.first().textContent();
          if (announceText?.trim()) {
            expect(announceText).toMatch(/complete|finished|break|focus|phase/i);
          }
        }
      }
    });
  });

  test.describe('Connection Status Communication', () => {
    test('should communicate WebSocket connection status', async () => {
      await page.goto('/dashboard');
      
      // Look for connection status indicators
      const connectionStatus = page.locator(
        '.connection-status, [data-connection], [aria-label*="connection" i], .online-status'
      );
      
      if (await connectionStatus.count() > 0) {
        const status = connectionStatus.first();
        
        // Should be accessible to screen readers
        const ariaLabel = await status.getAttribute('aria-label');
        const ariaLive = await status.getAttribute('aria-live');
        const role = await status.getAttribute('role');
        const title = await status.getAttribute('title');
        
        expect(ariaLabel || title || ariaLive || role).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/connect|online|offline|status/i);
        }
        
        // Connection changes should be announced
        if (ariaLive || role === 'status') {
          const statusText = await status.textContent();
          if (statusText?.trim()) {
            expect(statusText).toMatch(/connect|online|offline|disconnected/i);
          }
        }
      }
    });

    test('should handle connection loss gracefully with accessibility', async () => {
      await page.goto('/hive/1');
      
      // Simulate connection issues by blocking WebSocket
      await page.route('**/ws/**', route => route.abort());
      await page.route('**/websocket/**', route => route.abort());
      
      await page.waitForTimeout(2000);
      
      // Should show connection status
      const disconnectedAlert = page.locator('[role="alert"], .connection-error');
      
      if (await disconnectedAlert.count() > 0) {
        const alert = disconnectedAlert.first();
        
        // Should be visible and announce the issue
        await expect(alert).toBeVisible();
        
        const alertText = await alert.textContent();
        expect(alertText?.trim()).toBeTruthy();
        expect(alertText).toMatch(/connect|disconnect|offline|error|network/i);
        
        // Should have proper role
        const role = await alert.getAttribute('role');
        expect(role === 'alert').toBeTruthy();
      }
      
      // Should disable real-time features appropriately
      const realtimeElements = page.locator('.live-update, [data-realtime]');
      
      if (await realtimeElements.count() > 0) {
        const element = realtimeElements.first();
        
        // Should indicate that real-time features are unavailable
        const ariaDisabled = await element.getAttribute('aria-disabled');
        const disabled = await element.isDisabled();
        const ariaLabel = await element.getAttribute('aria-label');
        
        const indicatesUnavailable = ariaDisabled === 'true' || disabled || 
                                   (ariaLabel && ariaLabel.includes('unavailable'));
        
        expect(indicatesUnavailable).toBeTruthy();
      }
    });

    test('should announce reconnection successfully', async () => {
      await page.goto('/dashboard');
      
      // Look for reconnection status
      const reconnectStatus = page.locator(
        '[role="status"][aria-live], .reconnect-status, [data-reconnecting]'
      );
      
      if (await reconnectStatus.count() > 0) {
        const status = reconnectStatus.first();
        
        // Should have proper live region attributes
        const ariaLive = await status.getAttribute('aria-live');
        const role = await status.getAttribute('role');
        
        expect(ariaLive || role === 'status').toBeTruthy();
        
        if (ariaLive) {
          expect(['polite', 'assertive'].includes(ariaLive)).toBeTruthy();
        }
      }
      
      // Test reconnection announcement
      const reconnectButton = page.locator('[data-testid="reconnect"], button').filter({ hasText: /reconnect|retry/i });
      
      if (await reconnectButton.count() > 0) {
        await reconnectButton.click();
        await page.waitForTimeout(1000);
        
        // Should announce reconnection attempt or success
        const announcement = page.locator('[role="status"], [role="alert"]');
        
        if (await announcement.count() > 0) {
          const announceText = await announcement.first().textContent();
          if (announceText?.trim()) {
            expect(announceText).toMatch(/connect|reconnect|success|established/i);
          }
        }
      }
    });
  });

  test.describe('Real-time Activity Notifications', () => {
    test('should announce hive activity updates accessibly', async () => {
      await page.goto('/hive/1/activity');
      
      // Look for activity feed
      const activityFeed = page.locator('.activity-feed, [role="log"], .activity-list');
      
      if (await activityFeed.count() > 0) {
        const feed = activityFeed.first();
        
        // Should be a log region for activity updates
        const role = await feed.getAttribute('role');
        const ariaLabel = await feed.getAttribute('aria-label');
        const ariaLive = await feed.getAttribute('aria-live');
        
        expect(role === 'log' || ariaLive || ariaLabel).toBeTruthy();
        
        if (ariaLabel) {
          expect(ariaLabel).toMatch(/activity|feed|updates|notifications/i);
        }
        
        // Activity items should be accessible
        const activityItems = feed.locator('.activity-item, .update-item');
        
        if (await activityItems.count() > 0) {
          const item = activityItems.first();
          
          // Should have semantic structure
          const timestamp = item.locator('.timestamp, .time, [data-time]');
          const actor = item.locator('.actor, .user, [data-user]');
          const action = item.locator('.action, .description, [data-action]');
          
          // Should have activity details
          expect(await timestamp.count() > 0 || await actor.count() > 0 || await action.count() > 0).toBeTruthy();
          
          if (await action.count() > 0) {
            const actionText = await action.first().textContent();
            expect(actionText?.trim()).toBeTruthy();
          }
        }
      }
    });

    test('should provide activity notification controls', async () => {
      await page.goto('/settings/notifications');
      
      // Look for activity notification settings
      const activitySettings = page.locator(
        '[data-testid*="activity"], [name*="activity"], ' +
        'input[aria-label*="activity" i], [aria-label*="notification" i]'
      );
      
      if (await activitySettings.count() > 0) {
        const setting = activitySettings.first();
        
        // Should be properly labeled
        const ariaLabel = await setting.getAttribute('aria-label');
        const ariaLabelledBy = await setting.getAttribute('aria-labelledby');
        const id = await setting.getAttribute('id');
        
        if (id) {
          const label = page.locator(`label[for="${id}"]`);
          const hasLabel = await label.count() > 0;
          expect(hasLabel || ariaLabel || ariaLabelledBy).toBeTruthy();
        } else {
          expect(ariaLabel || ariaLabelledBy).toBeTruthy();
        }
        
        // Test setting functionality
        const type = await setting.getAttribute('type');
        if (type === 'checkbox') {
          await setting.click();
          
          // Should provide feedback
          const feedback = page.locator('[role="status"]').filter({ hasText: /saved|updated/i });
          if (await feedback.count() > 0) {
            const feedbackText = await feedback.first().textContent();
            expect(feedbackText).toMatch(/saved|updated|changed/i);
          }
        }
      }
    });
  });

  test.describe('WebSocket Error Handling', () => {
    test('should handle WebSocket errors accessibly', async () => {
      await page.goto('/real-time-dashboard');
      
      // Create a scenario that might cause WebSocket errors
      await page.evaluate(() => {
        // Simulate WebSocket error
        if (typeof window !== 'undefined' && (window as unknown as { WebSocket: unknown }).WebSocket) {
          const originalWebSocket = (window as unknown as { WebSocket: new (url: string) => WebSocket }).WebSocket;
          (window as unknown as { WebSocket: unknown }).WebSocket = class extends originalWebSocket {
            constructor(url: string) {
              super(url);
              setTimeout(() => {
                this.dispatchEvent(new Event('error'));
              }, 100);
            }
          };
        }
      });
      
      await page.waitForTimeout(500);
      
      // Should show error state accessibly
      const errorAlert = page.locator('[role="alert"], .websocket-error, .connection-error');
      
      if (await errorAlert.count() > 0) {
        const alert = errorAlert.first();
        
        // Should be visible and properly announced
        await expect(alert).toBeVisible();
        
        const alertText = await alert.textContent();
        expect(alertText?.trim()).toBeTruthy();
        expect(alertText).toMatch(/error|connect|fail|problem/i);
        
        // Should have proper alert role
        const role = await alert.getAttribute('role');
        expect(role === 'alert').toBeTruthy();
      }
      
      // Should provide recovery options
      const retryButton = page.locator('button').filter({ hasText: /retry|reconnect|reload/i });
      
      if (await retryButton.count() > 0) {
        const button = retryButton.first();
        
        // Should be accessible
        const ariaLabel = await button.getAttribute('aria-label');
        const text = await button.textContent();
        
        expect((ariaLabel || text || '').toLowerCase()).toMatch(/retry|reconnect|reload/);
      }
    });
  });

  test.describe('Comprehensive WebSocket Accessibility', () => {
    test('should maintain accessibility during WebSocket interactions', async () => {
      await page.goto('/collaborative-space');
      
      // Test that WebSocket updates don't break accessibility
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
      
      // Trigger some WebSocket activity
      const interactButton = page.locator('button, .interactive').first();
      if (await interactButton.count() > 0) {
        await interactButton.click();
        await page.waitForTimeout(500);
        
        // Re-check accessibility after WebSocket updates
        await checkA11y(page, undefined, {
          axeOptions: {
            rules: {
              'aria-valid-attr-value': { enabled: true },
              'aria-valid-attr': { enabled: true }
            }
          }
        });
      }
    });

    test('should provide controls for real-time content updates', async () => {
      await page.goto('/live-feed');
      
      // Look for controls to pause/resume live updates
      const pauseControl = page.locator(
        'button[aria-label*="pause" i], button[aria-label*="stop" i], ' +
        '[data-testid*="pause"], .pause-updates'
      );
      
      if (await pauseControl.count() > 0) {
        const control = pauseControl.first();
        
        // Should be properly labeled
        const ariaLabel = await control.getAttribute('aria-label');
        const text = await control.textContent();
        
        expect((ariaLabel || text || '').toLowerCase()).toMatch(/pause|stop|disable|control/);
        
        // Should work as expected
        await control.click();
        await page.waitForTimeout(200);
        
        // Should indicate paused state
        const ariaPressed = await control.getAttribute('aria-pressed');
        const newLabel = await control.getAttribute('aria-label');
        const newText = await control.textContent();
        
        const indicatesPaused = ariaPressed === 'true' || 
                               (newLabel && newLabel.includes('resume')) ||
                               (newText && newText.includes('resume'));
        
        expect(indicatesPaused).toBeTruthy();
      }
    });
  });
});