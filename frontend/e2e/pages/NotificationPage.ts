/**
 * Page Object Model for Notification-related pages and components
 * Provides methods for interacting with notification UI elements
 */

import { Page, Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../helpers/test-data';

export class NotificationPage {
  // Page elements - notification center/panel
  readonly notificationCenter: Locator;
  readonly notificationToggle: Locator;
  readonly notificationBadge: Locator;
  readonly notificationList: Locator;
  readonly clearAllButton: Locator;
  readonly markAllReadButton: Locator;
  readonly notificationSettings: Locator;

  // Toast notifications
  readonly toastContainer: Locator;
  readonly toastNotification: Locator;
  readonly toastCloseButton: Locator;

  // Settings elements
  readonly settingsPanel: Locator;
  readonly channelToggles: {
    inApp: Locator;
    push: Locator;
    email: Locator;
    sms: Locator;
  };
  readonly categoryToggles: {
    focusSessions: Locator;
    hiveActivity: Locator;
    buddySystem: Locator;
    achievements: Locator;
    system: Locator;
  };
  readonly doNotDisturbToggle: Locator;
  readonly doNotDisturbSchedule: {
    enabled: Locator;
    startTime: Locator;
    endTime: Locator;
  };
  readonly digestFrequencySelect: Locator;
  readonly saveSettingsButton: Locator;

  // Permission dialogs
  readonly permissionDialog: Locator;
  readonly allowNotificationsButton: Locator;
  readonly blockNotificationsButton: Locator;

  constructor(public readonly page: Page) {
    // Notification center elements
    this.notificationCenter = page.locator('[data-testid="notification-center"]');
    this.notificationToggle = page.locator('[data-testid="notification-toggle"]');
    this.notificationBadge = page.locator('[data-testid="notification-badge"]');
    this.notificationList = page.locator('[data-testid="notification-list"]');
    this.clearAllButton = page.locator('[data-testid="clear-all-notifications"]');
    this.markAllReadButton = page.locator('[data-testid="mark-all-read"]');
    this.notificationSettings = page.locator('[data-testid="notification-settings"]');

    // Toast notifications
    this.toastContainer = page.locator('[data-testid="toast-container"]');
    this.toastNotification = page.locator('[data-testid="notification-toast"]');
    this.toastCloseButton = page.locator('[data-testid="toast-close-button"]');

    // Settings panel
    this.settingsPanel = page.locator('[data-testid="notification-settings-panel"]');
    
    // Channel toggles
    this.channelToggles = {
      inApp: page.locator('[data-testid="channel-toggle-in-app"]'),
      push: page.locator('[data-testid="channel-toggle-push"]'),
      email: page.locator('[data-testid="channel-toggle-email"]'),
      sms: page.locator('[data-testid="channel-toggle-sms"]'),
    };

    // Category toggles
    this.categoryToggles = {
      focusSessions: page.locator('[data-testid="category-toggle-focus-sessions"]'),
      hiveActivity: page.locator('[data-testid="category-toggle-hive-activity"]'),
      buddySystem: page.locator('[data-testid="category-toggle-buddy-system"]'),
      achievements: page.locator('[data-testid="category-toggle-achievements"]'),
      system: page.locator('[data-testid="category-toggle-system"]'),
    };

    // Do not disturb settings
    this.doNotDisturbToggle = page.locator('[data-testid="do-not-disturb-toggle"]');
    this.doNotDisturbSchedule = {
      enabled: page.locator('[data-testid="dnd-schedule-enabled"]'),
      startTime: page.locator('[data-testid="dnd-start-time"]'),
      endTime: page.locator('[data-testid="dnd-end-time"]'),
    };

    this.digestFrequencySelect = page.locator('[data-testid="digest-frequency-select"]');
    this.saveSettingsButton = page.locator('[data-testid="save-notification-settings"]');

    // Permission dialogs
    this.permissionDialog = page.locator('[data-testid="notification-permission-dialog"]');
    this.allowNotificationsButton = page.locator('[data-testid="allow-notifications"]');
    this.blockNotificationsButton = page.locator('[data-testid="block-notifications"]');
  }

  // Navigation methods
  async goto() {
    await this.page.goto('/notifications');
    await this.page.waitForLoadState('networkidle');
  }

  async openNotificationCenter() {
    await this.notificationToggle.click();
    await this.notificationCenter.waitFor({ state: 'visible', timeout: TIMEOUTS.UI_ACTION });
  }

  async closeNotificationCenter() {
    // Click outside or use close button if available
    const closeButton = this.notificationCenter.locator('[data-testid="close-notification-center"]');
    if (await closeButton.count() > 0) {
      await closeButton.click();
    } else {
      // Click outside the notification center
      await this.page.click('body', { position: { x: 10, y: 10 } });
    }
    
    await this.notificationCenter.waitFor({ state: 'hidden', timeout: TIMEOUTS.UI_ACTION });
  }

  async openNotificationSettings() {
    await this.notificationSettings.click();
    await this.settingsPanel.waitFor({ state: 'visible', timeout: TIMEOUTS.UI_ACTION });
  }

  // Notification interaction methods
  async getNotificationCount(): Promise<number> {
    if (await this.notificationBadge.count() === 0) return 0;
    
    const badgeText = await this.notificationBadge.textContent();
    return parseInt(badgeText || '0', 10);
  }

  async getVisibleToastCount(): Promise<number> {
    return await this.toastNotification.count();
  }

  async getNotificationById(id: string): Promise<Locator> {
    return this.notificationList.locator(`[data-testid="notification-${id}"]`);
  }

  async getNotificationByTitle(title: string): Promise<Locator> {
    return this.notificationList.locator(`text="${title}"`).first();
  }

  async markNotificationAsRead(id: string) {
    const notification = await this.getNotificationById(id);
    const markReadButton = notification.locator('[data-testid="mark-read-button"]');
    
    if (await markReadButton.count() > 0) {
      await markReadButton.click();
    } else {
      // Click notification itself to mark as read
      await notification.click();
    }
  }

  async deleteNotification(id: string) {
    const notification = await this.getNotificationById(id);
    const deleteButton = notification.locator('[data-testid="delete-notification-button"]');
    
    await deleteButton.click();
    
    // Confirm deletion if dialog appears
    const confirmButton = this.page.locator('[data-testid="confirm-delete-notification"]');
    if (await confirmButton.count() > 0) {
      await confirmButton.click();
    }
  }

  async clearAllNotifications() {
    await this.clearAllButton.click();
    
    // Confirm if dialog appears
    const confirmButton = this.page.locator('[data-testid="confirm-clear-notifications"]');
    if (await confirmButton.count() > 0) {
      await confirmButton.click();
    }

    // Wait for notifications to be cleared
    await expect(this.notificationList.locator('[data-testid^="notification-"]')).toHaveCount(0);
  }

  async markAllAsRead() {
    await this.markAllReadButton.click();
    
    // Wait for all notifications to be marked as read
    const unreadNotifications = this.notificationList.locator('[data-testid^="notification-"]:not(.read)');
    await expect(unreadNotifications).toHaveCount(0);
  }

  // Toast notification methods
  async waitForToast(title?: string, timeout = TIMEOUTS.NOTIFICATION): Promise<Locator> {
    if (title) {
      const toastWithTitle = this.toastNotification.filter({ hasText: title });
      await toastWithTitle.waitFor({ timeout });
      return toastWithTitle.first();
    } else {
      await this.toastNotification.first().waitFor({ timeout });
      return this.toastNotification.first();
    }
  }

  async dismissToast(toast?: Locator) {
    const targetToast = toast || this.toastNotification.first();
    const closeButton = targetToast.locator('[data-testid="toast-close-button"]');
    
    if (await closeButton.count() > 0) {
      await closeButton.click();
    } else {
      // Click on toast itself if no close button
      await targetToast.click();
    }

    await targetToast.waitFor({ state: 'hidden', timeout: TIMEOUTS.UI_ACTION });
  }

  async dismissAllToasts() {
    const toasts = await this.toastNotification.all();
    for (const toast of toasts) {
      try {
        await this.dismissToast(toast);
      } catch (error) {
        // Continue if toast already disappeared
        console.warn('Failed to dismiss toast:', error);
      }
    }
  }

  // Settings management methods
  async toggleChannel(channel: keyof typeof this.channelToggles, enabled: boolean) {
    const toggle = this.channelToggles[channel];
    
    // Check current state
    const isCurrentlyEnabled = await toggle.isChecked();
    
    if (isCurrentlyEnabled !== enabled) {
      await toggle.click();
    }
    
    // Verify state change
    await expect(toggle).toBeChecked({ checked: enabled });
  }

  async toggleCategory(category: keyof typeof this.categoryToggles, enabled: boolean) {
    const toggle = this.categoryToggles[category];
    
    const isCurrentlyEnabled = await toggle.isChecked();
    
    if (isCurrentlyEnabled !== enabled) {
      await toggle.click();
    }
    
    await expect(toggle).toBeChecked({ checked: enabled });
  }

  async enableDoNotDisturb(schedule?: { start: string; end: string }) {
    // Enable do not disturb
    await this.doNotDisturbToggle.check();
    await expect(this.doNotDisturbToggle).toBeChecked();

    // Set schedule if provided
    if (schedule) {
      await this.doNotDisturbSchedule.enabled.check();
      await this.doNotDisturbSchedule.startTime.fill(schedule.start);
      await this.doNotDisturbSchedule.endTime.fill(schedule.end);
    }
  }

  async disableDoNotDisturb() {
    await this.doNotDisturbToggle.uncheck();
    await expect(this.doNotDisturbToggle).not.toBeChecked();
  }

  async setDigestFrequency(frequency: 'hourly' | 'daily' | 'weekly' | 'disabled') {
    await this.digestFrequencySelect.selectOption(frequency);
    await expect(this.digestFrequencySelect).toHaveValue(frequency);
  }

  async saveNotificationSettings() {
    await this.saveSettingsButton.click();
    
    // Wait for success message or settings to be saved
    const successMessage = this.page.locator('[data-testid="settings-saved-message"]');
    if (await successMessage.count() > 0) {
      await successMessage.waitFor({ timeout: TIMEOUTS.UI_ACTION });
    }
  }

  // Permission handling methods
  async handleNotificationPermission(allow: boolean) {
    // Wait for permission dialog to appear
    await this.permissionDialog.waitFor({ timeout: TIMEOUTS.UI_ACTION });
    
    if (allow) {
      await this.allowNotificationsButton.click();
    } else {
      await this.blockNotificationsButton.click();
    }
    
    // Wait for dialog to disappear
    await this.permissionDialog.waitFor({ state: 'hidden', timeout: TIMEOUTS.UI_ACTION });
  }

  // Validation methods
  async validateNotificationDisplay(expectedTitle: string, expectedMessage: string) {
    const notification = await this.getNotificationByTitle(expectedTitle);
    
    await expect(notification).toBeVisible();
    await expect(notification).toContainText(expectedTitle);
    await expect(notification).toContainText(expectedMessage);
    
    // Check timestamp
    const timestamp = notification.locator('[data-testid="notification-timestamp"]');
    if (await timestamp.count() > 0) {
      await expect(timestamp).toBeVisible();
    }
  }

  async validateToastDisplay(expectedTitle: string, expectedMessage: string) {
    const toast = await this.waitForToast(expectedTitle);
    
    await expect(toast).toBeVisible();
    await expect(toast).toContainText(expectedTitle);
    await expect(toast).toContainText(expectedMessage);
  }

  async validateNotificationAccessibility(notificationLocator: Locator) {
    // Check ARIA attributes
    await expect(notificationLocator).toHaveAttribute('role');
    
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

  async validateSettingsAccessibility() {
    // Check all toggles have labels
    const allToggles = [
      ...Object.values(this.channelToggles),
      ...Object.values(this.categoryToggles),
      this.doNotDisturbToggle,
    ];

    for (const toggle of allToggles) {
      if (await toggle.count() > 0) {
        await expect(toggle).toHaveAttribute('aria-label');
      }
    }

    // Check form accessibility
    await expect(this.digestFrequencySelect).toHaveAttribute('aria-label');
    await expect(this.saveSettingsButton).toBeVisible();
  }

  // Utility methods
  async waitForNotificationUpdate(timeout = TIMEOUTS.API_RESPONSE) {
    // Wait for any pending API calls to complete
    await this.page.waitForLoadState('networkidle', { timeout });
  }

  async isDoNotDisturbActive(): Promise<boolean> {
    return await this.doNotDisturbToggle.isChecked();
  }

  async getChannelStatus(): Promise<Record<string, boolean>> {
    const status: Record<string, boolean> = {};
    
    for (const [channel, toggle] of Object.entries(this.channelToggles)) {
      if (await toggle.count() > 0) {
        status[channel] = await toggle.isChecked();
      }
    }
    
    return status;
  }

  async getCategoryStatus(): Promise<Record<string, boolean>> {
    const status: Record<string, boolean> = {};
    
    for (const [category, toggle] of Object.entries(this.categoryToggles)) {
      if (await toggle.count() > 0) {
        status[category] = await toggle.isChecked();
      }
    }
    
    return status;
  }
}