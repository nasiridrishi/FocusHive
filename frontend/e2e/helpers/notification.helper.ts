/**
 * Notification helper functions for E2E tests
 * Provides utilities for testing notification features, real-time delivery, and user preferences
 */

import {expect, Locator, Page} from '@playwright/test';
import {TIMEOUTS} from './test-data';

// Mock notification data types
export interface MockNotification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  priority: 'low' | 'normal' | 'high' | 'critical';
  channels: NotificationChannel[];
  timestamp: Date;
  read: boolean;
  userId: string;
  metadata?: Record<string, unknown>;
}

export interface NotificationPreferences {
  channels: {
    inApp: boolean;
    push: boolean;
    email: boolean;
    sms: boolean;
  };
  doNotDisturb: {
    enabled: boolean;
    schedule?: {
      start: string; // HH:MM format
      end: string;   // HH:MM format
    };
  };
  categories: {
    focusSessions: boolean;
    hiveActivity: boolean;
    buddySystem: boolean;
    achievements: boolean;
    system: boolean;
  };
  frequency: {
    immediate: boolean;
    digest: 'hourly' | 'daily' | 'weekly' | 'disabled';
  };
}

export type NotificationType =
    | 'focus_session_reminder'
    | 'focus_session_complete'
    | 'hive_member_joined'
    | 'hive_message'
    | 'buddy_checkin'
    | 'buddy_request'
    | 'achievement_unlocked'
    | 'system_maintenance'
    | 'system_update';

export type NotificationChannel = 'in_app' | 'push' | 'email' | 'sms';

// Global window extensions for testing
declare global {
  interface Window {
    mockNotifications?: Array<{ title: string; options?: NotificationOptions }>;
    notificationSoundPlayed?: boolean;
    vibrationPattern?: VibratePattern;
  }
}

export class NotificationHelper {
  constructor(private page: Page) {
  }

  // Mock data generation
  generateMockNotification(overrides: Partial<MockNotification> = {}): MockNotification {
    const baseNotification: MockNotification = {
      id: `notif_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      type: 'focus_session_reminder',
      title: 'Focus Session Starting',
      message: 'Your focus session will begin in 5 minutes',
      priority: 'normal',
      channels: ['in_app', 'push'],
      timestamp: new Date(),
      read: false,
      userId: 'test-user',
      ...overrides,
    };

    return baseNotification;
  }

  generateMockPreferences(overrides: Partial<NotificationPreferences> = {}): NotificationPreferences {
    return {
      channels: {
        inApp: true,
        push: true,
        email: true,
        sms: false,
      },
      doNotDisturb: {
        enabled: false,
      },
      categories: {
        focusSessions: true,
        hiveActivity: true,
        buddySystem: true,
        achievements: true,
        system: true,
      },
      frequency: {
        immediate: true,
        digest: 'daily',
      },
      ...overrides,
    };
  }

  // Mock API responses
  async mockNotificationAPI(): Promise<void> {
    await this.page.route('**/api/notifications**', async route => {
      const url = route.request().url();
      const method = route.request().method();

      if (method === 'GET' && url.includes('/notifications')) {
        // Return mock notification list
        const notifications = [
          this.generateMockNotification({
            type: 'focus_session_reminder',
            title: 'Focus Session Starting',
            message: 'Your deep work session starts in 5 minutes',
          }),
          this.generateMockNotification({
            type: 'achievement_unlocked',
            title: 'Achievement Unlocked!',
            message: 'You completed 5 focus sessions in a row',
            priority: 'high',
            read: false,
          }),
          this.generateMockNotification({
            type: 'hive_member_joined',
            title: 'New Hive Member',
            message: 'Alex joined your Study Group hive',
            read: true,
          }),
        ];

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({notifications, total: notifications.length}),
        });
      } else if (method === 'GET' && url.includes('/preferences')) {
        // Return mock notification preferences
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(this.generateMockPreferences()),
        });
      } else if (method === 'POST' && url.includes('/preferences')) {
        // Mock preferences update
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({success: true}),
        });
      } else if (method === 'PATCH' && url.includes('/read')) {
        // Mock mark as read
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({success: true}),
        });
      } else {
        await route.continue();
      }
    });
  }

  // Real-time WebSocket simulation
  async simulateRealtimeNotification(notification: MockNotification): Promise<void> {
    // Inject notification into page via WebSocket simulation
    await this.page.evaluate((notif) => {
      // Simulate WebSocket message
      const mockEvent = new CustomEvent('websocket-notification', {
        detail: notif,
      });
      window.dispatchEvent(mockEvent);
    }, notification);
  }

  // Browser push notification testing
  async mockPushNotificationPermission(permission: 'granted' | 'denied' | 'default'): Promise<void> {
    await this.page.addInitScript((perm) => {
      // Mock Notification API
      Object.defineProperty(window, 'Notification', {
        value: class MockNotification {
          static permission = perm;

          constructor(public title: string, public options?: NotificationOptions) {
            // Store for testing
            window.mockNotifications = window.mockNotifications || [];
            window.mockNotifications.push({title, options});
          }

          static requestPermission = (): Promise<NotificationPermission> => Promise.resolve(perm);
        },
        configurable: true,
      });
    }, permission);
  }

  async getPushNotifications(): Promise<Array<{ title: string; options?: NotificationOptions }>> {
    return await this.page.evaluate(() => {
      return window.mockNotifications || [];
    });
  }

  // Do not disturb mode testing
  async enableDoNotDisturb(schedule?: { start: string; end: string }): Promise<void> {
    const preferences = this.generateMockPreferences({
      doNotDisturb: {
        enabled: true,
        schedule,
      },
    });

    // Mock API call
    await this.page.route('**/api/notifications/preferences', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(preferences),
      });
    });

    return preferences;
  }

  // Email notification testing
  async mockEmailNotification(notification: MockNotification): Promise<void> {
    await this.page.route('**/api/notifications/email', async route => {
      const requestBody = await route.request().postDataJSON();

      // Validate email format
      const emailValid = requestBody.to &&
          requestBody.subject &&
          requestBody.html &&
          requestBody.subject.includes(notification.title);

      await route.fulfill({
        status: emailValid ? 200 : 400,
        contentType: 'application/json',
        body: JSON.stringify({
          success: emailValid,
          messageId: emailValid ? `msg_${Date.now()}` : undefined,
          error: emailValid ? undefined : 'Invalid email format',
        }),
      });
    });
  }

  // Performance testing
  async measureNotificationLatency(): Promise<number> {
    const startTime = Date.now();

    // Trigger notification
    const notification = this.generateMockNotification({
      priority: 'high',
      type: 'focus_session_reminder',
    });

    await this.simulateRealtimeNotification(notification);

    // Wait for notification to appear
    const notificationElement = this.page.locator('[data-testid="notification-toast"]').first();
    await notificationElement.waitFor({timeout: TIMEOUTS.NOTIFICATION});

    const endTime = Date.now();
    return endTime - startTime;
  }

  // Accessibility testing helpers
  async validateNotificationAccessibility(notificationLocator: Locator): Promise<void> {
    // Check ARIA attributes
    await expect(notificationLocator).toHaveAttribute('role', 'alert');
    await expect(notificationLocator).toHaveAttribute('aria-live', 'polite');

    // Check keyboard navigation
    await notificationLocator.focus();
    await expect(notificationLocator).toBeFocused();

    // Check close button accessibility
    const closeButton = notificationLocator.locator('[aria-label*="close"], [aria-label*="dismiss"]');
    if (await closeButton.count() > 0) {
      await expect(closeButton).toBeVisible();
      await expect(closeButton).toHaveAttribute('aria-label');
    }
  }

  // Notification UI validation
  async validateNotificationDisplay(notification: MockNotification, locator: Locator): Promise<void> {
    await expect(locator).toBeVisible();
    await expect(locator).toContainText(notification.title);
    await expect(locator).toContainText(notification.message);

    // Check priority styling
    if (notification.priority === 'critical') {
      await expect(locator).toHaveClass(/error|critical/);
    } else if (notification.priority === 'high') {
      await expect(locator).toHaveClass(/warning|important/);
    }

    // Check timestamp
    const timestamp = locator.locator('[data-testid="notification-timestamp"]');
    if (await timestamp.count() > 0) {
      await expect(timestamp).toBeVisible();
    }
  }

  // Notification management
  async markAsRead(notificationId: string): Promise<void> {
    const notificationElement = this.page.locator(`[data-testid="notification-${notificationId}"]`);
    const markReadButton = notificationElement.locator('[data-testid="mark-read-button"]');

    if (await markReadButton.count() > 0) {
      await markReadButton.click();
    } else {
      // Click notification itself to mark as read
      await notificationElement.click();
    }

    // Verify marked as read
    await expect(notificationElement).toHaveClass(/read/);
  }

  async clearAllNotifications(): Promise<void> {
    const clearAllButton = this.page.locator('[data-testid="clear-all-notifications"]');
    if (await clearAllButton.count() > 0) {
      await clearAllButton.click();

      // Confirm if dialog appears
      const confirmButton = this.page.locator('[data-testid="confirm-clear-notifications"]');
      if (await confirmButton.count() > 0) {
        await confirmButton.click();
      }
    }
  }

  // Sound and vibration testing
  async mockNotificationSound(): Promise<void> {
    await this.page.addInitScript(() => {
      // Mock Audio API
      window.HTMLAudioElement.prototype.play = function () {
        // Store for testing
        window.notificationSoundPlayed = true;
        return Promise.resolve();
      };
    });
  }

  async wasNotificationSoundPlayed(): Promise<boolean> {
    return await this.page.evaluate(() => {
      return window.notificationSoundPlayed || false;
    });
  }

  async mockVibrationAPI(): Promise<boolean> {
    await this.page.addInitScript(() => {
      // Mock Vibration API
      Object.defineProperty(navigator, 'vibrate', {
        value: function (pattern: VibratePattern) {
          window.vibrationPattern = pattern;
          return true;
        },
        configurable: true,
      });
    });
  }

  async getVibrationPattern(): Promise<VibratePattern | undefined> {
    return await this.page.evaluate(() => {
      return window.vibrationPattern;
    });
  }

  // Cleanup
  async cleanup(): Promise<void> {
    // Clear all routes
    await this.page.unrouteAll();

    // Clear mock data
    await this.page.evaluate(() => {
      delete window.mockNotifications;
      delete window.notificationSoundPlayed;
      delete window.vibrationPattern;
    });
  }

  // Utility methods for common notification scenarios
  async waitForNotification(title: string, timeout = TIMEOUTS.NOTIFICATION): Promise<void> {
    const notificationLocator = this.page.locator('[data-testid="notification-toast"]', {hasText: title});
    await notificationLocator.waitFor({timeout});
    return notificationLocator;
  }

  async countVisibleNotifications(): Promise<number> {
    return await this.page.locator('[data-testid="notification-toast"]:visible').count();
  }

  async getNotificationBadgeCount(): Promise<number> {
    const badge = this.page.locator('[data-testid="notification-badge"]');
    if (await badge.count() === 0) return 0;

    const text = await badge.textContent();
    return parseInt(text || '0', 10);
  }
}