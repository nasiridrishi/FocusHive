/**
 * Hive E2E Test Helper Functions
 * Provides reusable utilities for hive workflow testing
 */

import { Page, expect, Locator, BrowserContext, Browser } from '@playwright/test';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { 
  HIVE_TEST_USERS, 
  HIVE_TEMPLATES, 
  TIMER_CONFIGURATIONS, 
  PRESENCE_STATUSES,
  generateUniqueHiveData,
  generateTestMember,
  type TestHive 
} from './hive-fixtures';
import { CreateHiveRequest, Hive, HiveMember } from '../../../src/services/api/hiveApi';

// API Response types
export interface ApiResponse<T = unknown> {
  data?: T;
  message?: string;
  error?: string;
  status: number;
}

// User context for multi-user testing
export interface UserContext {
  context: BrowserContext;
  page: Page;
  helper: HiveWorkflowHelper;
  user: TestUser;
}

export interface TestUser {
  id: number;
  username: string;
  email: string;
  password: string;
  displayName: string;
  avatar?: string;
}

export interface HiveTestContext {
  page: Page;
  authHelper: EnhancedAuthHelper;
  currentUser: typeof HIVE_TEST_USERS[keyof typeof HIVE_TEST_USERS];
  currentHive?: TestHive;
}

export interface TimerSession {
  id: string;
  hiveId: number;
  startedAt: string;
  duration: number;
  remainingTime: number;
  isActive: boolean;
  participants: number[];
}

export interface PresenceUpdate {
  userId: number;
  status: 'active' | 'away' | 'break' | 'offline';
  activity?: string;
  timestamp: string;
}

export class HiveWorkflowHelper {
  constructor(private page: Page) {}

  /**
   * Navigate to hive discovery/browse page
   */
  async navigateToDiscovery(): Promise<void> {
    await this.page.goto('/hives/discover');
    await expect(this.page.locator('[data-testid="hive-discovery"]')).toBeVisible();
  }

  /**
   * Navigate to create hive page
   */
  async navigateToCreateHive(): Promise<void> {
    await this.page.goto('/hives/create');
    await expect(this.page.locator('[data-testid="create-hive-form"]')).toBeVisible();
  }

  /**
   * Navigate to specific hive page
   */
  async navigateToHive(hiveId: number | string): Promise<void> {
    await this.page.goto(`/hives/${hiveId}`);
    await expect(this.page.locator('[data-testid="hive-workspace"]')).toBeVisible();
  }

  /**
   * Navigate to dashboard
   */
  async navigateToDashboard(): Promise<void> {
    await this.page.goto('/dashboard');
    await expect(this.page.locator('[data-testid="user-dashboard"]')).toBeVisible();
  }

  /**
   * Create a new hive with specified configuration
   */
  async createHive(hiveData: Partial<CreateHiveRequest> = {}): Promise<TestHive> {
    await this.navigateToCreateHive();
    
    const defaultData = generateUniqueHiveData();
    const finalData = { ...defaultData, ...hiveData };

    // Fill basic information
    await this.page.fill('[data-testid="hive-name-input"]', finalData.name);
    if (finalData.description) {
      await this.page.fill('[data-testid="hive-description-input"]', finalData.description);
    }

    // Set privacy settings
    if (finalData.isPrivate) {
      await this.page.check('[data-testid="hive-private-checkbox"]');
    }

    // Set member limit
    await this.page.fill('[data-testid="hive-max-members-input"]', finalData.maxMembers.toString());

    // Add tags
    if (finalData.tags && finalData.tags.length > 0) {
      for (const tag of finalData.tags) {
        await this.page.fill('[data-testid="hive-tags-input"]', tag);
        await this.page.press('[data-testid="hive-tags-input"]', 'Enter');
      }
    }

    // Configure settings
    if (finalData.settings) {
      if (finalData.settings.allowChat !== undefined) {
        await this.toggleSetting('allowChat', finalData.settings.allowChat);
      }
      if (finalData.settings.allowMusic !== undefined) {
        await this.toggleSetting('allowMusic', finalData.settings.allowMusic);
      }
      if (finalData.settings.requireApproval !== undefined) {
        await this.toggleSetting('requireApproval', finalData.settings.requireApproval);
      }
    }

    // Submit form
    await this.page.click('[data-testid="create-hive-button"]');

    // Wait for creation success and redirect
    await expect(this.page.locator('[data-testid="hive-workspace"]')).toBeVisible();
    
    // Extract hive ID from URL
    const url = this.page.url();
    const hiveId = parseInt(url.split('/hives/')[1]);

    return {
      id: hiveId,
      ...finalData
    } as TestHive;
  }

  /**
   * Search for hives with filters
   */
  async searchHives(query: string, filters: Record<string, any> = {}): Promise<void> {
    await this.navigateToDiscovery();
    
    // Enter search query
    await this.page.fill('[data-testid="hive-search-input"]', query);
    await this.page.press('[data-testid="hive-search-input"]', 'Enter');

    // Apply filters
    if (filters.isPrivate !== undefined) {
      await this.page.click('[data-testid="filter-privacy"]');
      const option = filters.isPrivate ? 'Private' : 'Public';
      await this.page.click(`[data-testid="privacy-option-${option.toLowerCase()}"]`);
    }

    if (filters.hasAvailableSlots) {
      await this.page.check('[data-testid="filter-available-slots"]');
    }

    if (filters.tags && filters.tags.length > 0) {
      await this.page.click('[data-testid="filter-tags"]');
      for (const tag of filters.tags) {
        await this.page.click(`[data-testid="tag-filter-${tag}"]`);
      }
    }

    // Wait for results to load
    await expect(this.page.locator('[data-testid="hive-search-results"]')).toBeVisible();
  }

  /**
   * Join a hive by ID
   */
  async joinHive(hiveId: number): Promise<void> {
    await this.navigateToHive(hiveId);
    await this.page.click('[data-testid="join-hive-button"]');
    
    // Handle different join scenarios
    const approvalModal = this.page.locator('[data-testid="join-approval-modal"]');
    if (await approvalModal.isVisible()) {
      // Fill approval request message
      await this.page.fill('[data-testid="join-message-input"]', 'I would like to join this hive to improve my productivity.');
      await this.page.click('[data-testid="submit-join-request"]');
      await expect(this.page.locator('[data-testid="join-request-sent"]')).toBeVisible();
    } else {
      // Direct join
      await expect(this.page.locator('[data-testid="member-panel"]')).toBeVisible();
    }
  }

  /**
   * Leave a hive
   */
  async leaveHive(): Promise<void> {
    await this.page.click('[data-testid="hive-menu-button"]');
    await this.page.click('[data-testid="leave-hive-option"]');
    
    // Confirm leave action
    await this.page.click('[data-testid="confirm-leave-hive"]');
    await expect(this.page.locator('[data-testid="hive-left-message"]')).toBeVisible();
  }

  /**
   * Start a timer session
   */
  async startTimerSession(config: keyof typeof TIMER_CONFIGURATIONS = 'POMODORO_25_5'): Promise<TimerSession> {
    const timerConfig = TIMER_CONFIGURATIONS[config];
    
    // Open timer settings
    await this.page.click('[data-testid="timer-settings-button"]');
    
    // Select timer type
    await this.page.click(`[data-testid="timer-type-${timerConfig.type}"]`);
    
    // Configure timer based on type
    if (timerConfig.type === 'pomodoro') {
      await this.page.fill('[data-testid="work-duration-input"]', timerConfig.workDuration.toString());
      await this.page.fill('[data-testid="short-break-input"]', timerConfig.shortBreakDuration.toString());
      await this.page.fill('[data-testid="long-break-input"]', timerConfig.longBreakDuration.toString());
    } else if (timerConfig.type === 'continuous') {
      await this.page.fill('[data-testid="timer-duration-input"]', timerConfig.duration.toString());
    }

    // Start timer
    await this.page.click('[data-testid="start-timer-button"]');
    
    // Wait for timer to start
    await expect(this.page.locator('[data-testid="active-timer"]')).toBeVisible();
    
    // Extract session information
    const sessionId = await this.page.getAttribute('[data-testid="active-timer"]', 'data-session-id') || 'session_' + Date.now();
    const hiveId = parseInt(this.page.url().split('/hives/')[1]);
    
    return {
      id: sessionId,
      hiveId,
      startedAt: new Date().toISOString(),
      duration: timerConfig.type === 'pomodoro' ? timerConfig.workDuration * 60 : timerConfig.duration * 60,
      remainingTime: timerConfig.type === 'pomodoro' ? timerConfig.workDuration * 60 : timerConfig.duration * 60,
      isActive: true,
      participants: [1] // Current user
    };
  }

  /**
   * Stop current timer session
   */
  async stopTimerSession(): Promise<void> {
    await this.page.click('[data-testid="stop-timer-button"]');
    await expect(this.page.locator('[data-testid="timer-stopped"]')).toBeVisible();
  }

  /**
   * Pause/Resume timer session
   */
  async pauseResumeTimer(): Promise<void> {
    const pauseButton = this.page.locator('[data-testid="pause-timer-button"]');
    const resumeButton = this.page.locator('[data-testid="resume-timer-button"]');
    
    if (await pauseButton.isVisible()) {
      await pauseButton.click();
      await expect(resumeButton).toBeVisible();
    } else if (await resumeButton.isVisible()) {
      await resumeButton.click();
      await expect(pauseButton).toBeVisible();
    }
  }

  /**
   * Update user presence status
   */
  async updatePresenceStatus(status: keyof typeof PRESENCE_STATUSES): Promise<void> {
    await this.page.click('[data-testid="user-presence-dropdown"]');
    await this.page.click(`[data-testid="presence-status-${status.toLowerCase()}"]`);
    
    // Verify status update
    await expect(this.page.locator(`[data-testid="current-status-${status.toLowerCase()}"]`)).toBeVisible();
  }

  /**
   * Get list of current hive members
   */
  async getHiveMembers(): Promise<Array<{ username: string; status: string; isActive: boolean }>> {
    const memberElements = await this.page.locator('[data-testid="member-item"]').all();
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
   * Check real-time timer synchronization
   */
  async verifyTimerSync(expectedRemainingTime: number, toleranceMs: number = 2000): Promise<boolean> {
    const timerDisplay = this.page.locator('[data-testid="timer-display"]');
    const currentTime = await timerDisplay.textContent();
    
    if (!currentTime) return false;
    
    // Parse MM:SS format
    const [minutes, seconds] = currentTime.split(':').map(Number);
    const actualRemainingMs = (minutes * 60 + seconds) * 1000;
    const expectedRemainingMs = expectedRemainingTime * 1000;
    
    return Math.abs(actualRemainingMs - expectedRemainingMs) <= toleranceMs;
  }

  /**
   * Wait for WebSocket presence update
   */
  async waitForPresenceUpdate(userId: number, expectedStatus: string, timeoutMs: number = 5000): Promise<boolean> {
    const memberElement = this.page.locator(`[data-testid="member-${userId}"]`);
    const statusElement = memberElement.locator('[data-testid="member-status"]');
    
    try {
      await expect(statusElement).toHaveText(expectedStatus, { timeout: timeoutMs });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Navigate to analytics dashboard
   */
  async navigateToAnalytics(): Promise<void> {
    await this.page.click('[data-testid="analytics-tab"]');
    await expect(this.page.locator('[data-testid="analytics-dashboard"]')).toBeVisible();
  }

  /**
   * Get session analytics data
   */
  async getSessionAnalytics(): Promise<{
    totalFocusTime: number;
    completedSessions: number;
    averageSessionLength: number;
    currentStreak: number;
  }> {
    await this.navigateToAnalytics();
    
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
  async exportAnalyticsData(format: 'csv' | 'json' | 'pdf'): Promise<void> {
    await this.navigateToAnalytics();
    await this.page.click('[data-testid="export-menu-button"]');
    await this.page.click(`[data-testid="export-${format}"]`);
    
    // Wait for download
    const downloadPromise = this.page.waitForEvent('download');
    await this.page.click('[data-testid="confirm-export"]');
    const download = await downloadPromise;
    
    expect(download.suggestedFilename()).toContain(format);
  }

  /**
   * Toggle hive settings
   */
  private async toggleSetting(setting: string, enabled: boolean): Promise<void> {
    const checkbox = this.page.locator(`[data-testid="setting-${setting}"]`);
    const isChecked = await checkbox.isChecked();
    
    if (isChecked !== enabled) {
      await checkbox.click();
    }
  }

  /**
   * Wait for API response and validate data
   */
  async waitForApiResponse<T = unknown>(endpoint: string, method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET'): Promise<T> {
    const responsePromise = this.page.waitForResponse(response => 
      response.url().includes(endpoint) && response.request().method() === method
    );
    
    const response = await responsePromise;
    expect(response.ok()).toBeTruthy();
    
    return response.json() as T;
  }

  /**
   * Measure WebSocket message latency
   */
  async measureWebSocketLatency(): Promise<number> {
    const startTime = Date.now();
    
    // Send a presence update
    await this.updatePresenceStatus('ACTIVE');
    
    // Wait for the update to be reflected in UI
    await this.waitForPresenceUpdate(1, 'active');
    
    return Date.now() - startTime;
  }

  /**
   * Simulate network conditions
   */
  async simulateNetworkConditions(conditions: 'slow3g' | 'fast3g' | 'offline'): Promise<void> {
    const context = this.page.context();
    
    const networkConditions = {
      slow3g: { downloadThroughput: 50000, uploadThroughput: 50000, latency: 2000 },
      fast3g: { downloadThroughput: 1600000, uploadThroughput: 750000, latency: 150 },
      offline: { offline: true }
    };
    
    await context.setOffline(conditions === 'offline');
    if (conditions !== 'offline') {
      // Note: Full network throttling requires CDP, this is simplified
      await this.page.evaluate(`
        if ('connection' in navigator) {
          Object.defineProperty(navigator, 'connection', {
            value: { effectiveType: '${conditions}' },
            writable: false
          });
        }
      `);
    }
  }
}

/**
 * Multi-user test helper for concurrent user scenarios
 */
export class MultiUserHiveHelper {
  private contexts: UserContext[] = [];

  async createUserSession(browser: Browser, user: TestUser): Promise<HiveWorkflowHelper> {
    const context = await browser.newContext();
    const page = await context.newPage();
    const helper = new HiveWorkflowHelper(page);
    const authHelper = new EnhancedAuthHelper(page);
    
    // Login user
    await authHelper.loginUser(user.email, user.password);
    
    this.contexts.push({ context, page, helper, user });
    return helper;
  }

  async cleanup(): Promise<void> {
    for (const { context } of this.contexts) {
      await context.close();
    }
    this.contexts = [];
  }

  getHelpers(): HiveWorkflowHelper[] {
    return this.contexts.map(({ helper }) => helper);
  }

  async simulateConcurrentTimerStart(hiveId: number): Promise<TimerSession[]> {
    const sessions = [];
    
    // Navigate all users to the same hive
    await Promise.all(
      this.contexts.map(({ helper }) => helper.navigateToHive(hiveId))
    );

    // Start timers simultaneously
    const timerPromises = this.contexts.map(({ helper }) => 
      helper.startTimerSession('POMODORO_25_5')
    );
    
    const results = await Promise.all(timerPromises);
    return results;
  }

  async verifyConcurrentPresence(hiveId: number): Promise<boolean> {
    // Navigate all users to the same hive
    await Promise.all(
      this.contexts.map(({ helper }) => helper.navigateToHive(hiveId))
    );

    // Get member lists from all users
    const memberLists = await Promise.all(
      this.contexts.map(({ helper }) => helper.getHiveMembers())
    );

    // Verify all users see the same member count
    const memberCounts = memberLists.map(list => list.length);
    return new Set(memberCounts).size === 1;
  }
}

export default {
  HiveWorkflowHelper,
  MultiUserHiveHelper,
  HIVE_TEST_USERS,
  HIVE_TEMPLATES,
  TIMER_CONFIGURATIONS,
  PRESENCE_STATUSES
};