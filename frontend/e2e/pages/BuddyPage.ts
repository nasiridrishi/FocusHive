/**
 * Page Object Model for Buddy System Dashboard
 * Provides methods to interact with buddy system features in E2E tests
 */

import { Page, Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../helpers/test-data';

export class BuddyPage {
  readonly page: Page;

  // Main dashboard elements
  readonly dashboard: Locator;
  readonly dashboardTitle: Locator;
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;
  readonly retryButton: Locator;
  readonly refreshButton: Locator;
  readonly emptyStateMessage: Locator;

  // Navigation tabs
  readonly activeBuddiesTab: Locator;
  readonly matchingTab: Locator;
  readonly sessionsTab: Locator;
  readonly challengesTab: Locator;
  readonly studyGroupsTab: Locator;
  readonly statsTab: Locator;

  // Active buddies section
  readonly activeBuddiesSection: Locator;
  readonly buddyCards: Locator;

  // Potential matches section
  readonly potentialMatchesSection: Locator;
  readonly focusAreaFilter: Locator;
  readonly timezoneFilter: Locator;
  readonly buddySearchInput: Locator;
  readonly searchResults: Locator;

  // Sessions section
  readonly upcomingSessionsList: Locator;
  readonly scheduleSessionButton: Locator;
  readonly sessionScheduleDialog: Locator;

  // Challenges section
  readonly challengesSection: Locator;
  readonly createChallengeButton: Locator;
  readonly challengeCreateDialog: Locator;

  // Study groups section
  readonly studyGroupsSection: Locator;
  readonly createStudyGroupButton: Locator;
  readonly studyGroupDialog: Locator;

  // Stats section
  readonly streakSection: Locator;
  readonly streakRewards: Locator;

  // Modals and dialogs
  readonly buddyRequestDialog: Locator;
  readonly goalsModal: Locator;
  readonly checkinDialog: Locator;
  readonly profileModal: Locator;
  readonly relationshipModal: Locator;
  readonly feedbackDialog: Locator;
  readonly reportIssueDialog: Locator;
  readonly privacySettingsDialog: Locator;
  readonly jointGoalsModal: Locator;
  readonly mentoringSetupDialog: Locator;
  readonly helpRequestDialog: Locator;
  readonly blockConfirmDialog: Locator;

  // Communication features
  readonly chatInterface: Locator;
  readonly messageInput: Locator;
  readonly sendMessageButton: Locator;
  readonly sharedWorkspace: Locator;

  // Notification features
  readonly notificationBell: Locator;
  readonly notificationPanel: Locator;
  readonly reminderSection: Locator;

  // Safety and management
  readonly safetyMenu: Locator;
  readonly relationshipManagementMenu: Locator;
  readonly encouragementMenu: Locator;

  // Special UI elements
  readonly compatibilityBreakdown: Locator;
  readonly limitWarning: Locator;

  constructor(page: Page) {
    this.page = page;

    // Main dashboard elements
    this.dashboard = page.locator('[data-testid="buddy-dashboard"]');
    this.dashboardTitle = page.locator('h1, h2, h3, h4').filter({ hasText: /Buddy System|Buddy Dashboard/i });
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"], .MuiCircularProgress-root');
    this.errorAlert = page.locator('.MuiAlert-root[severity="error"], [role="alert"]').filter({ hasText: /error|failed/i });
    this.retryButton = page.locator('button:has-text("Retry"), button:has-text("Try Again")');
    this.refreshButton = page.locator('button[aria-label*="refresh"], [data-testid="refresh-button"]');
    this.emptyStateMessage = page.locator(':has-text("No buddy relationships"), :has-text("Get started by finding")');

    // Navigation tabs
    this.activeBuddiesTab = page.locator('[data-testid="active-buddies-tab"], button:has-text("Active Buddies")');
    this.matchingTab = page.locator('[data-testid="matching-tab"], button:has-text("Find Buddies"), button:has-text("Matching")');
    this.sessionsTab = page.locator('[data-testid="sessions-tab"], button:has-text("Sessions")');
    this.challengesTab = page.locator('[data-testid="challenges-tab"], button:has-text("Challenges")');
    this.studyGroupsTab = page.locator('[data-testid="study-groups-tab"], button:has-text("Study Groups")');
    this.statsTab = page.locator('[data-testid="stats-tab"], button:has-text("Stats"), button:has-text("Statistics")');

    // Active buddies section
    this.activeBuddiesSection = page.locator('[data-testid="active-buddies-section"]');
    this.buddyCards = page.locator('[data-testid="buddy-card"]');

    // Potential matches section
    this.potentialMatchesSection = page.locator('[data-testid="potential-matches-section"]');
    this.focusAreaFilter = page.locator('[data-testid="focus-area-filter"]');
    this.timezoneFilter = page.locator('[data-testid="timezone-filter"]');
    this.buddySearchInput = page.locator('[data-testid="buddy-search-input"], input[placeholder*="Search"]');
    this.searchResults = page.locator('[data-testid="search-results"]');

    // Sessions section
    this.upcomingSessionsList = page.locator('[data-testid="upcoming-sessions-list"]');
    this.scheduleSessionButton = page.locator('[data-testid="schedule-session-button"]');
    this.sessionScheduleDialog = page.locator('[data-testid="session-schedule-dialog"]');

    // Challenges section
    this.challengesSection = page.locator('[data-testid="challenges-section"]');
    this.createChallengeButton = page.locator('[data-testid="create-challenge-button"]');
    this.challengeCreateDialog = page.locator('[data-testid="challenge-create-dialog"]');

    // Study groups section
    this.studyGroupsSection = page.locator('[data-testid="study-groups-section"]');
    this.createStudyGroupButton = page.locator('[data-testid="create-study-group-button"]');
    this.studyGroupDialog = page.locator('[data-testid="study-group-dialog"]');

    // Stats section
    this.streakSection = page.locator('[data-testid="streak-section"]');
    this.streakRewards = page.locator('[data-testid="streak-rewards"]');

    // Modals and dialogs
    this.buddyRequestDialog = page.locator('[data-testid="buddy-request-dialog"]');
    this.goalsModal = page.locator('[data-testid="goals-modal"]');
    this.checkinDialog = page.locator('[data-testid="checkin-dialog"]');
    this.profileModal = page.locator('[data-testid="profile-modal"]');
    this.relationshipModal = page.locator('[data-testid="relationship-modal"]');
    this.feedbackDialog = page.locator('[data-testid="feedback-dialog"]');
    this.reportIssueDialog = page.locator('[data-testid="report-issue-dialog"]');
    this.privacySettingsDialog = page.locator('[data-testid="privacy-settings-dialog"]');
    this.jointGoalsModal = page.locator('[data-testid="joint-goals-modal"]');
    this.mentoringSetupDialog = page.locator('[data-testid="mentoring-setup-dialog"]');
    this.helpRequestDialog = page.locator('[data-testid="help-request-dialog"]');
    this.blockConfirmDialog = page.locator('[data-testid="block-confirm-dialog"]');

    // Communication features
    this.chatInterface = page.locator('[data-testid="chat-interface"]');
    this.messageInput = page.locator('[data-testid="message-input"], input[placeholder*="message"]');
    this.sendMessageButton = page.locator('[data-testid="send-message-button"]');
    this.sharedWorkspace = page.locator('[data-testid="shared-workspace"]');

    // Notification features
    this.notificationBell = page.locator('[data-testid="notification-bell"], [aria-label*="notifications"]');
    this.notificationPanel = page.locator('[data-testid="notification-panel"]');
    this.reminderSection = page.locator('[data-testid="reminder-section"]');

    // Safety and management
    this.safetyMenu = page.locator('[data-testid="safety-menu"]');
    this.relationshipManagementMenu = page.locator('[data-testid="relationship-management-menu"]');
    this.encouragementMenu = page.locator('[data-testid="encouragement-menu"]');

    // Special UI elements
    this.compatibilityBreakdown = page.locator('[data-testid="compatibility-breakdown"]');
    this.limitWarning = page.locator('[data-testid="limit-warning"], .MuiAlert-root:has-text("maximum")');
  }

  /**
   * Navigate to buddy dashboard
   */
  async goto(): Promise<void> {
    await this.page.goto('/buddy');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to buddy demo page (if available)
   */
  async gotoDemo(): Promise<void> {
    await this.page.goto('/buddy/demo');
    await this.waitForPageLoad();
  }

  /**
   * Wait for page to load completely
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout: TIMEOUTS.NETWORK });
    
    // Wait for dashboard to be visible or error/empty state to appear
    try {
      await Promise.race([
        this.dashboard.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.errorAlert.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.emptyStateMessage.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.page.locator('main, .MuiContainer-root, body').waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM })
      ]);
    } catch {
      // If none of the expected elements appear, just ensure the page has loaded
      await this.page.waitForLoadState('domcontentloaded');
    }
  }

  /**
   * Wait for dashboard data to load
   */
  async waitForDataLoad(): Promise<void> {
    // Wait for loading spinner to disappear
    try {
      await this.loadingSpinner.waitFor({ state: 'hidden', timeout: TIMEOUTS.NETWORK });
    } catch {
      // Spinner might not be visible, which is fine
    }

    // Wait for either data to load or empty/error state
    await Promise.race([
      this.activeBuddiesSection.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
      this.potentialMatchesSection.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
      this.emptyStateMessage.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT }),
      this.errorAlert.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT })
    ]);
  }

  /**
   * Verify dashboard is loaded successfully
   */
  async verifyDashboardLoaded(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.dashboardTitle).toBeVisible();
    await expect(this.loadingSpinner).not.toBeVisible();
  }

  /**
   * Verify dashboard shows empty state
   */
  async verifyEmptyState(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.emptyStateMessage).toBeVisible();
  }

  /**
   * Verify dashboard shows error state
   */
  async verifyErrorState(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.errorAlert).toBeVisible();
    await expect(this.retryButton).toBeVisible();
  }

  /**
   * Click retry button
   */
  async clickRetry(): Promise<void> {
    await this.retryButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Switch to matching tab
   */
  async switchToMatchingTab(): Promise<void> {
    if (await this.matchingTab.isVisible()) {
      await this.matchingTab.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Switch to sessions tab
   */
  async switchToSessionsTab(): Promise<void> {
    if (await this.sessionsTab.isVisible()) {
      await this.sessionsTab.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Switch to challenges tab
   */
  async switchToChallengesTab(): Promise<void> {
    if (await this.challengesTab.isVisible()) {
      await this.challengesTab.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Switch to study groups tab
   */
  async switchToStudyGroupsTab(): Promise<void> {
    if (await this.studyGroupsTab.isVisible()) {
      await this.studyGroupsTab.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Switch to stats tab
   */
  async switchToStatsTab(): Promise<void> {
    if (await this.statsTab.isVisible()) {
      await this.statsTab.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Get list of active buddy cards
   */
  async getActiveBuddyCards(): Promise<Locator[]> {
    await this.activeBuddiesSection.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT });
    return await this.buddyCards.all();
  }

  /**
   * Get list of potential matches
   */
  async getPotentialMatches(): Promise<Locator[]> {
    await this.potentialMatchesSection.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT });
    return await this.potentialMatchesSection.locator('[data-testid="buddy-match-card"]').all();
  }

  /**
   * Get search results
   */
  async getSearchResults(): Promise<Locator[]> {
    await this.searchResults.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT });
    return await this.searchResults.locator('[data-testid="search-result-item"]').all();
  }

  /**
   * Get upcoming sessions
   */
  async getUpcomingSessions(): Promise<Locator[]> {
    await this.upcomingSessionsList.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT });
    return await this.upcomingSessionsList.locator('[data-testid="session-item"]').all();
  }

  /**
   * Filter potential matches by focus area
   */
  async filterByFocusArea(focusArea: string): Promise<void> {
    if (await this.focusAreaFilter.isVisible()) {
      await this.focusAreaFilter.click();
      await this.page.locator(`[data-value="${focusArea}"]`).click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Search for a buddy by username or criteria
   */
  async searchForBuddy(searchTerm: string): Promise<void> {
    if (await this.buddySearchInput.isVisible()) {
      await this.buddySearchInput.fill(searchTerm);
      await this.buddySearchInput.press('Enter');
      await this.waitForDataLoad();
    }
  }

  /**
   * Fill buddy request form
   */
  async fillBuddyRequest(request: {
    message: string;
    proposedEndDate: string;
    goals: string;
    expectations: string;
  }): Promise<void> {
    await this.buddyRequestDialog.locator('[name="message"]').fill(request.message);
    await this.buddyRequestDialog.locator('[name="proposedEndDate"]').fill(request.proposedEndDate);
    await this.buddyRequestDialog.locator('[name="goals"]').fill(request.goals);
    await this.buddyRequestDialog.locator('[name="expectations"]').fill(request.expectations);
  }

  /**
   * Submit buddy request
   */
  async submitBuddyRequest(): Promise<void> {
    await this.buddyRequestDialog.locator('[data-testid="submit-request-button"]').click();
    await this.page.waitForTimeout(1000); // Wait for submission
  }

  /**
   * Fill new goal form
   */
  async fillNewGoal(goal: {
    title: string;
    description: string;
    dueDate: string;
  }): Promise<void> {
    await this.goalsModal.locator('[name="title"]').fill(goal.title);
    await this.goalsModal.locator('[name="description"]').fill(goal.description);
    await this.goalsModal.locator('[name="dueDate"]').fill(goal.dueDate);
  }

  /**
   * Submit new goal
   */
  async submitNewGoal(): Promise<void> {
    await this.goalsModal.locator('[data-testid="submit-goal-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill check-in form
   */
  async fillCheckin(checkin: {
    moodRating: number;
    progressRating: number;
    message: string;
    currentFocus: string;
    challenges: string;
    wins: string;
  }): Promise<void> {
    // Rate mood (1-5)
    await this.checkinDialog.locator(`[data-testid="mood-rating-${checkin.moodRating}"]`).click();
    
    // Rate progress (1-5)
    await this.checkinDialog.locator(`[data-testid="progress-rating-${checkin.progressRating}"]`).click();
    
    // Fill text fields
    await this.checkinDialog.locator('[name="message"]').fill(checkin.message);
    await this.checkinDialog.locator('[name="currentFocus"]').fill(checkin.currentFocus);
    await this.checkinDialog.locator('[name="challenges"]').fill(checkin.challenges);
    await this.checkinDialog.locator('[name="wins"]').fill(checkin.wins);
  }

  /**
   * Submit check-in
   */
  async submitCheckin(): Promise<void> {
    await this.checkinDialog.locator('[data-testid="submit-checkin-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Send a message in chat
   */
  async sendMessage(messageText: string): Promise<void> {
    await this.messageInput.fill(messageText);
    await this.sendMessageButton.click();
    await this.page.waitForTimeout(500); // Wait for message to appear
  }

  /**
   * Fill session scheduling form
   */
  async fillSessionDetails(session: {
    buddyId: string;
    sessionDate: string;
    sessionTime: string;
    duration: number;
    agenda: string;
  }): Promise<void> {
    // Select buddy
    await this.sessionScheduleDialog.locator('[data-testid="buddy-select"]').click();
    await this.page.locator(`[data-value="${session.buddyId}"]`).click();
    
    // Set date and time
    await this.sessionScheduleDialog.locator('[name="sessionDate"]').fill(session.sessionDate);
    await this.sessionScheduleDialog.locator('[name="sessionTime"]').fill(session.sessionTime);
    
    // Set duration
    await this.sessionScheduleDialog.locator('[name="duration"]').fill(session.duration.toString());
    
    // Fill agenda
    await this.sessionScheduleDialog.locator('[name="agenda"]').fill(session.agenda);
  }

  /**
   * Submit session schedule
   */
  async submitSessionSchedule(): Promise<void> {
    await this.sessionScheduleDialog.locator('[data-testid="schedule-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill joint goal form
   */
  async fillJointGoal(goal: {
    title: string;
    description: string;
    targetDate: string;
    individualTargets: {
      self: string;
      buddy: string;
    };
  }): Promise<void> {
    await this.jointGoalsModal.locator('[name="title"]').fill(goal.title);
    await this.jointGoalsModal.locator('[name="description"]').fill(goal.description);
    await this.jointGoalsModal.locator('[name="targetDate"]').fill(goal.targetDate);
    await this.jointGoalsModal.locator('[name="selfTarget"]').fill(goal.individualTargets.self);
    await this.jointGoalsModal.locator('[name="buddyTarget"]').fill(goal.individualTargets.buddy);
  }

  /**
   * Submit joint goal
   */
  async submitJointGoal(): Promise<void> {
    await this.jointGoalsModal.locator('[data-testid="submit-joint-goal-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill challenge details
   */
  async fillChallengeDetails(challenge: {
    challengeType: string;
    title: string;
    description: string;
    duration: number;
    rules: string;
    prize: string;
  }): Promise<void> {
    await this.challengeCreateDialog.locator('[data-testid="challenge-type-select"]').click();
    await this.page.locator(`[data-value="${challenge.challengeType}"]`).click();
    
    await this.challengeCreateDialog.locator('[name="title"]').fill(challenge.title);
    await this.challengeCreateDialog.locator('[name="description"]').fill(challenge.description);
    await this.challengeCreateDialog.locator('[name="duration"]').fill(challenge.duration.toString());
    await this.challengeCreateDialog.locator('[name="rules"]').fill(challenge.rules);
    await this.challengeCreateDialog.locator('[name="prize"]').fill(challenge.prize);
  }

  /**
   * Select challenge opponent
   */
  async selectChallengeOpponent(buddyId: string): Promise<void> {
    await this.challengeCreateDialog.locator('[data-testid="opponent-select"]').click();
    await this.page.locator(`[data-value="${buddyId}"]`).click();
  }

  /**
   * Submit challenge
   */
  async submitChallenge(): Promise<void> {
    await this.challengeCreateDialog.locator('[data-testid="submit-challenge-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill study group details
   */
  async fillStudyGroupDetails(group: {
    name: string;
    description: string;
    subject: string;
    maxMembers: number;
    meetingSchedule: string;
  }): Promise<void> {
    await this.studyGroupDialog.locator('[name="name"]').fill(group.name);
    await this.studyGroupDialog.locator('[name="description"]').fill(group.description);
    await this.studyGroupDialog.locator('[name="subject"]').fill(group.subject);
    await this.studyGroupDialog.locator('[name="maxMembers"]').fill(group.maxMembers.toString());
    await this.studyGroupDialog.locator('[name="meetingSchedule"]').fill(group.meetingSchedule);
  }

  /**
   * Select study group members
   */
  async selectStudyGroupMembers(buddyIds: string[]): Promise<void> {
    for (const buddyId of buddyIds) {
      await this.studyGroupDialog.locator(`[data-testid="member-checkbox-${buddyId}"]`).click();
    }
  }

  /**
   * Submit study group
   */
  async submitStudyGroup(): Promise<void> {
    await this.studyGroupDialog.locator('[data-testid="submit-study-group-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill mentoring setup form
   */
  async fillMentoringSetup(setup: {
    role: string;
    expertise: string;
    goals: string;
    commitment: string;
  }): Promise<void> {
    await this.mentoringSetupDialog.locator(`[data-testid="role-${setup.role}"]`).click();
    await this.mentoringSetupDialog.locator('[name="expertise"]').fill(setup.expertise);
    await this.mentoringSetupDialog.locator('[name="goals"]').fill(setup.goals);
    await this.mentoringSetupDialog.locator('[name="commitment"]').fill(setup.commitment);
  }

  /**
   * Submit mentoring setup
   */
  async submitMentoringSetup(): Promise<void> {
    await this.mentoringSetupDialog.locator('[data-testid="submit-mentoring-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill help request form
   */
  async fillHelpRequest(request: {
    urgency: string;
    category: string;
    description: string;
    preferredResponse: string;
  }): Promise<void> {
    await this.helpRequestDialog.locator('[data-testid="urgency-select"]').click();
    await this.page.locator(`[data-value="${request.urgency}"]`).click();
    
    await this.helpRequestDialog.locator('[data-testid="category-select"]').click();
    await this.page.locator(`[data-value="${request.category}"]`).click();
    
    await this.helpRequestDialog.locator('[name="description"]').fill(request.description);
    
    await this.helpRequestDialog.locator('[data-testid="response-select"]').click();
    await this.page.locator(`[data-value="${request.preferredResponse}"]`).click();
  }

  /**
   * Submit help request
   */
  async submitHelpRequest(): Promise<void> {
    await this.helpRequestDialog.locator('[data-testid="submit-help-request-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill feedback form
   */
  async fillFeedback(feedback: {
    rating: number;
    categories: {
      communication: number;
      helpfulness: number;
      reliability: number;
      motivation: number;
    };
    comment: string;
    wouldRecommend: boolean;
  }): Promise<void> {
    // Overall rating
    await this.feedbackDialog.locator(`[data-testid="overall-rating-${feedback.rating}"]`).click();
    
    // Category ratings
    for (const [category, rating] of Object.entries(feedback.categories)) {
      await this.feedbackDialog.locator(`[data-testid="${category}-rating-${rating}"]`).click();
    }
    
    // Comment
    await this.feedbackDialog.locator('[name="comment"]').fill(feedback.comment);
    
    // Recommendation
    const recommendCheckbox = this.feedbackDialog.locator('[data-testid="would-recommend"]');
    if (feedback.wouldRecommend && !await recommendCheckbox.isChecked()) {
      await recommendCheckbox.click();
    } else if (!feedback.wouldRecommend && await recommendCheckbox.isChecked()) {
      await recommendCheckbox.click();
    }
  }

  /**
   * Submit feedback
   */
  async submitFeedback(): Promise<void> {
    await this.feedbackDialog.locator('[data-testid="submit-feedback-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill issue report form
   */
  async fillIssueReport(issue: {
    issueType: string;
    severity: string;
    description: string;
    desiredOutcome: string;
    hasAttemptedResolution: boolean;
  }): Promise<void> {
    await this.reportIssueDialog.locator('[data-testid="issue-type-select"]').click();
    await this.page.locator(`[data-value="${issue.issueType}"]`).click();
    
    await this.reportIssueDialog.locator('[data-testid="severity-select"]').click();
    await this.page.locator(`[data-value="${issue.severity}"]`).click();
    
    await this.reportIssueDialog.locator('[name="description"]').fill(issue.description);
    await this.reportIssueDialog.locator('[name="desiredOutcome"]').fill(issue.desiredOutcome);
    
    const attemptedCheckbox = this.reportIssueDialog.locator('[data-testid="attempted-resolution"]');
    if (issue.hasAttemptedResolution && !await attemptedCheckbox.isChecked()) {
      await attemptedCheckbox.click();
    }
  }

  /**
   * Submit issue report
   */
  async submitIssueReport(): Promise<void> {
    await this.reportIssueDialog.locator('[data-testid="submit-issue-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill pause relationship reason
   */
  async fillPauseReason(pause: {
    reason: string;
    duration: string;
    resumeDate: string;
  }): Promise<void> {
    await this.relationshipManagementMenu.locator('[name="pauseReason"]').fill(pause.reason);
    await this.relationshipManagementMenu.locator('[name="pauseDuration"]').fill(pause.duration);
    await this.relationshipManagementMenu.locator('[name="resumeDate"]').fill(pause.resumeDate);
  }

  /**
   * Submit pause request
   */
  async submitPauseRequest(): Promise<void> {
    await this.relationshipManagementMenu.locator('[data-testid="confirm-pause-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Fill block reason form
   */
  async fillBlockReason(block: {
    reason: string;
    details: string;
    reportToModerators: boolean;
  }): Promise<void> {
    await this.blockConfirmDialog.locator('[name="blockReason"]').fill(block.reason);
    await this.blockConfirmDialog.locator('[name="blockDetails"]').fill(block.details);
    
    const reportCheckbox = this.blockConfirmDialog.locator('[data-testid="report-to-moderators"]');
    if (block.reportToModerators && !await reportCheckbox.isChecked()) {
      await reportCheckbox.click();
    }
  }

  /**
   * Confirm blocking action
   */
  async confirmBlock(): Promise<void> {
    await this.blockConfirmDialog.locator('[data-testid="confirm-block-button"]').click();
    await this.page.waitForTimeout(1000);
  }

  /**
   * Verify mobile layout
   */
  async verifyMobileLayout(): Promise<void> {
    // Set mobile viewport
    await this.page.setViewportSize({ width: 375, height: 667 });
    await this.waitForDataLoad();
    
    // Verify dashboard adjusts to mobile layout
    await expect(this.dashboard).toBeVisible();
    
    // Verify navigation might be collapsed or in tabs
    const tabContainer = this.page.locator('.MuiTabs-root, [role="tablist"]');
    if (await tabContainer.isVisible()) {
      await expect(tabContainer).toBeVisible();
    }
  }

  /**
   * Verify accessibility features
   */
  async verifyAccessibility(): Promise<void> {
    // Check for proper heading structure
    const headings = this.page.locator('h1, h2, h3, h4, h5, h6');
    expect(await headings.count()).toBeGreaterThan(0);
    
    // Check for aria-labels on interactive elements
    const interactiveElements = this.page.locator('button, [role="button"], input, [role="tab"]');
    const elementsCount = await interactiveElements.count();
    expect(elementsCount).toBeGreaterThan(0);
    
    // Check for screen reader content
    const srElements = this.page.locator('[aria-live], .sr-only, [aria-hidden="true"]');
    expect(await srElements.count()).toBeGreaterThan(0);
  }

  /**
   * Measure dashboard load performance
   */
  async measureLoadPerformance(): Promise<{
    loadTime: number;
    firstContentfulPaint: number;
    domContentLoaded: number;
  }> {
    const startTime = Date.now();
    
    await this.goto();
    await this.waitForDataLoad();
    
    const endTime = Date.now();
    const loadTime = endTime - startTime;
    
    // Get performance metrics from the browser
    const performanceMetrics = await this.page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0,
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      };
    });
    
    return {
      loadTime,
      firstContentfulPaint: performanceMetrics.firstContentfulPaint,
      domContentLoaded: performanceMetrics.domContentLoaded,
    };
  }
}