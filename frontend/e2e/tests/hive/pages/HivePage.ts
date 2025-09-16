/**
 * Hive Page Object Model
 * Centralizes hive-related page interactions for consistent testing
 */

import {expect, Locator, Page} from '@playwright/test';
import {TIMER_CONFIGURATIONS} from '../hive-fixtures';

export class HivePage {
  readonly page: Page;

  // Main navigation
  readonly hiveWorkspace: Locator;
  readonly headerTitle: Locator;
  readonly menuButton: Locator;
  readonly settingsButton: Locator;

  // Member panel
  readonly memberPanel: Locator;
  readonly memberList: Locator;
  readonly joinButton: Locator;
  readonly leaveButton: Locator;
  readonly inviteButton: Locator;

  // Timer section
  readonly timerSection: Locator;
  readonly timerDisplay: Locator;
  readonly startTimerButton: Locator;
  readonly pauseTimerButton: Locator;
  readonly resumeTimerButton: Locator;
  readonly stopTimerButton: Locator;
  readonly timerSettingsButton: Locator;
  readonly activeTimer: Locator;

  // Presence indicators
  readonly userPresenceDropdown: Locator;
  readonly currentStatusIndicator: Locator;

  // Analytics tab
  readonly analyticsTab: Locator;
  readonly analyticsDashboard: Locator;

  // Chat section
  readonly chatSection: Locator;
  readonly messageInput: Locator;
  readonly sendButton: Locator;
  readonly messagesList: Locator;

  constructor(page: Page) {
    this.page = page;

    // Main navigation
    this.hiveWorkspace = page.locator('[data-testid="hive-workspace"]');
    this.headerTitle = page.locator('[data-testid="hive-title"]');
    this.menuButton = page.locator('[data-testid="hive-menu-button"]');
    this.settingsButton = page.locator('[data-testid="hive-settings-button"]');

    // Member panel
    this.memberPanel = page.locator('[data-testid="member-panel"]');
    this.memberList = page.locator('[data-testid="member-list"]');
    this.joinButton = page.locator('[data-testid="join-hive-button"]');
    this.leaveButton = page.locator('[data-testid="leave-hive-option"]');
    this.inviteButton = page.locator('[data-testid="invite-members-button"]');

    // Timer section
    this.timerSection = page.locator('[data-testid="timer-section"]');
    this.timerDisplay = page.locator('[data-testid="timer-display"]');
    this.startTimerButton = page.locator('[data-testid="start-timer-button"]');
    this.pauseTimerButton = page.locator('[data-testid="pause-timer-button"]');
    this.resumeTimerButton = page.locator('[data-testid="resume-timer-button"]');
    this.stopTimerButton = page.locator('[data-testid="stop-timer-button"]');
    this.timerSettingsButton = page.locator('[data-testid="timer-settings-button"]');
    this.activeTimer = page.locator('[data-testid="active-timer"]');

    // Presence indicators
    this.userPresenceDropdown = page.locator('[data-testid="user-presence-dropdown"]');
    this.currentStatusIndicator = page.locator('[data-testid*="current-status-"]');

    // Analytics tab
    this.analyticsTab = page.locator('[data-testid="analytics-tab"]');
    this.analyticsDashboard = page.locator('[data-testid="analytics-dashboard"]');

    // Chat section
    this.chatSection = page.locator('[data-testid="chat-section"]');
    this.messageInput = page.locator('[data-testid="message-input"]');
    this.sendButton = page.locator('[data-testid="send-message-button"]');
    this.messagesList = page.locator('[data-testid="messages-list"]');
  }

  /**
   * Navigate to hive workspace
   */
  async goto(hiveId: number | string): Promise<void> {
    await this.page.goto(`/hives/${hiveId}`);
    await expect(this.hiveWorkspace).toBeVisible();
  }

  /**
   * Wait for page to be fully loaded
   */
  async waitForLoad(): Promise<void> {
    await expect(this.hiveWorkspace).toBeVisible();
    await expect(this.memberPanel).toBeVisible();
    await expect(this.timerSection).toBeVisible();
  }

  /**
   * Get hive information from the page
   */
  async getHiveInfo(): Promise<{
    title: string;
    memberCount: number;
    isJoined: boolean;
  }> {
    const title = await this.headerTitle.textContent() || '';
    const memberElements = await this.memberList.locator('[data-testid="member-item"]').count();
    const isJoined = await this.leaveButton.isVisible();

    return {
      title,
      memberCount: memberElements,
      isJoined
    };
  }

  /**
   * Join the hive
   */
  async joinHive(approvalMessage?: string): Promise<void> {
    await this.joinButton.click();

    // Handle approval modal if required
    const approvalModal = this.page.locator('[data-testid="join-approval-modal"]');
    if (await approvalModal.isVisible()) {
      if (approvalMessage) {
        await this.page.fill('[data-testid="join-message-input"]', approvalMessage);
      }
      await this.page.click('[data-testid="submit-join-request"]');
      await expect(this.page.locator('[data-testid="join-request-sent"]')).toBeVisible();
    } else {
      await expect(this.memberPanel).toBeVisible();
    }
  }

  /**
   * Leave the hive
   */
  async leaveHive(): Promise<void> {
    await this.menuButton.click();
    await this.leaveButton.click();

    // Confirm leave action
    await this.page.click('[data-testid="confirm-leave-hive"]');
    await expect(this.page.locator('[data-testid="hive-left-message"]')).toBeVisible();
  }

  /**
   * Start a timer session
   */
  async startTimer(config?: keyof typeof TIMER_CONFIGURATIONS): Promise<string> {
    if (config) {
      await this.configureTimer(config);
    }

    await this.startTimerButton.click();
    await expect(this.activeTimer).toBeVisible();

    // Extract session ID
    const sessionId = await this.activeTimer.getAttribute('data-session-id') || `session_${Date.now()}`;
    return sessionId;
  }

  /**
   * Configure timer settings
   */
  async configureTimer(config: keyof typeof TIMER_CONFIGURATIONS): Promise<void> {
    const timerConfig = TIMER_CONFIGURATIONS[config];

    await this.timerSettingsButton.click();
    await this.page.click(`[data-testid="timer-type-${timerConfig.type}"]`);

    if (timerConfig.type === 'pomodoro') {
      await this.page.fill('[data-testid="work-duration-input"]', timerConfig.workDuration.toString());
      await this.page.fill('[data-testid="short-break-input"]', timerConfig.shortBreakDuration.toString());
      await this.page.fill('[data-testid="long-break-input"]', timerConfig.longBreakDuration.toString());
    } else if (timerConfig.type === 'continuous') {
      await this.page.fill('[data-testid="timer-duration-input"]', timerConfig.duration.toString());
    }

    await this.page.click('[data-testid="save-timer-settings"]');
  }

  /**
   * Pause/Resume timer
   */
  async pauseResumeTimer(): Promise<'paused' | 'resumed'> {
    if (await this.pauseTimerButton.isVisible()) {
      await this.pauseTimerButton.click();
      await expect(this.resumeTimerButton).toBeVisible();
      return 'paused';
    } else if (await this.resumeTimerButton.isVisible()) {
      await this.resumeTimerButton.click();
      await expect(this.pauseTimerButton).toBeVisible();
      return 'resumed';
    }
    throw new Error('Neither pause nor resume button is visible');
  }

  /**
   * Stop timer
   */
  async stopTimer(): Promise<void> {
    await this.stopTimerButton.click();
    await expect(this.page.locator('[data-testid="timer-stopped"]')).toBeVisible();
  }

  /**
   * Get current timer state
   */
  async getTimerState(): Promise<{
    isActive: boolean;
    remainingTime: string;
    isPaused: boolean;
  }> {
    const isActive = await this.activeTimer.isVisible();
    const remainingTime = isActive ? await this.timerDisplay.textContent() || '00:00' : '00:00';
    const isPaused = await this.resumeTimerButton.isVisible();

    return {
      isActive,
      remainingTime,
      isPaused
    };
  }

  /**
   * Update presence status
   */
  async updatePresenceStatus(status: 'active' | 'away' | 'break' | 'offline'): Promise<void> {
    await this.userPresenceDropdown.click();
    await this.page.click(`[data-testid="presence-status-${status}"]`);

    // Verify status update
    await expect(this.page.locator(`[data-testid="current-status-${status}"]`)).toBeVisible();
  }

  /**
   * Get list of members
   */
  async getMembers(): Promise<Array<{
    username: string;
    status: string;
    isActive: boolean;
  }>> {
    const memberElements = await this.memberList.locator('[data-testid="member-item"]').all();
    const members = [];

    for (const element of memberElements) {
      const username = await element.locator('[data-testid="member-username"]').textContent();
      const status = await element.locator('[data-testid="member-status"]').textContent();
      const isActive = await element.locator('[data-testid="member-active-indicator"]').isVisible();

      members.push({
        username: username || '',
        status: status || '',
        isActive
      });
    }

    return members;
  }

  /**
   * Send chat message
   */
  async sendChatMessage(message: string): Promise<void> {
    await this.messageInput.fill(message);
    await this.sendButton.click();

    // Verify message appears in chat
    await expect(
        this.messagesList.locator(`[data-testid="message"]:has-text("${message}")`)
    ).toBeVisible();
  }

  /**
   * Navigate to analytics
   */
  async openAnalytics(): Promise<void> {
    await this.analyticsTab.click();
    await expect(this.analyticsDashboard).toBeVisible();
  }

  /**
   * Get analytics summary
   */
  async getAnalyticsSummary(): Promise<{
    totalFocusTime: number;
    completedSessions: number;
    averageSessionLength: number;
    currentStreak: number;
  }> {
    await this.openAnalytics();

    const totalFocusTime = await this.page.locator('[data-testid="total-focus-time"]').textContent();
    const completedSessions = await this.page.locator('[data-testid="completed-sessions"]').textContent();
    const averageSessionLength = await this.page.locator('[data-testid="average-session-length"]').textContent();
    const currentStreak = await this.page.locator('[data-testid="current-streak"]').textContent();

    return {
      totalFocusTime: parseInt(totalFocusTime?.replace(/\D/g, '') || '0'),
      completedSessions: parseInt(completedSessions?.replace(/\D/g, '') || '0'),
      averageSessionLength: parseInt(averageSessionLength?.replace(/\D/g, '') || '0'),
      currentStreak: parseInt(currentStreak?.replace(/\D/g, '') || '0')
    };
  }

  /**
   * Export analytics data
   */
  async exportAnalytics(format: 'csv' | 'json' | 'pdf'): Promise<void> {
    await this.openAnalytics();
    await this.page.click('[data-testid="export-menu-button"]');
    await this.page.click(`[data-testid="export-${format}"]`);

    // Wait for download
    const downloadPromise = this.page.waitForEvent('download');
    await this.page.click('[data-testid="confirm-export"]');
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toContain(format);
  }

  /**
   * Wait for real-time updates
   */
  async waitForPresenceUpdate(userId: number, expectedStatus: string, timeoutMs: number = 5000): Promise<boolean> {
    const memberElement = this.page.locator(`[data-testid="member-${userId}"]`);
    const statusElement = memberElement.locator('[data-testid="member-status"]');

    try {
      await expect(statusElement).toHaveText(expectedStatus, {timeout: timeoutMs});
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Wait for timer synchronization
   */
  async waitForTimerSync(expectedTime: string, toleranceSeconds: number = 2): Promise<boolean> {
    const [expectedMinutes, expectedSeconds] = expectedTime.split(':').map(Number);
    const expectedTotalSeconds = expectedMinutes * 60 + expectedSeconds;

    try {
      await expect(async () => {
        const currentTime = await this.timerDisplay.textContent() || '00:00';
        const [currentMinutes, currentSecs] = currentTime.split(':').map(Number);
        const currentTotalSeconds = currentMinutes * 60 + currentSecs;

        const difference = Math.abs(currentTotalSeconds - expectedTotalSeconds);
        expect(difference).toBeLessThanOrEqual(toleranceSeconds);
      }).toPass({timeout: 5000});

      return true;
    } catch {
      return false;
    }
  }

  /**
   * Invite members to hive
   */
  async inviteMembers(emails: string[]): Promise<void> {
    await this.inviteButton.click();

    const inviteModal = this.page.locator('[data-testid="invite-modal"]');
    await expect(inviteModal).toBeVisible();

    for (const email of emails) {
      await this.page.fill('[data-testid="invite-email-input"]', email);
      await this.page.click('[data-testid="add-invite-email"]');
    }

    await this.page.click('[data-testid="send-invitations"]');
    await expect(this.page.locator('[data-testid="invitations-sent"]')).toBeVisible();
  }

  /**
   * Check if user is hive owner
   */
  async isOwner(): Promise<boolean> {
    return await this.settingsButton.isVisible();
  }

  /**
   * Access hive settings (owner only)
   */
  async openHiveSettings(): Promise<void> {
    if (!await this.isOwner()) {
      throw new Error('Only hive owners can access settings');
    }

    await this.settingsButton.click();
    await expect(this.page.locator('[data-testid="hive-settings-modal"]')).toBeVisible();
  }
}

export default HivePage;