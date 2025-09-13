/**
 * Page Object Model for Persona Management and Context Switching
 * 
 * Provides a structured interface for persona CRUD operations, context switching,
 * and multi-persona session management within the FocusHive Identity Service
 * 
 * @fileoverview Persona management page object model for E2E tests
 * @version 1.0.0
 */

import { expect, type Page, type Locator } from '@playwright/test';
import { IDENTITY_ROUTES } from '../tests/identity/identity.config';
import type { TestPersona, PersonaType } from '../fixtures/identity/identity-fixtures';

export class PersonaSwitcherPage {
  readonly page: Page;

  // Persona switcher elements
  readonly personaSwitcher: Locator;
  readonly currentPersona: Locator;
  readonly currentPersonaAvatar: Locator;
  readonly currentPersonaName: Locator;
  readonly personaDropdown: Locator;
  readonly personaOptions: Locator;

  // Persona management elements
  readonly createPersonaButton: Locator;
  readonly editPersonaButton: Locator;
  readonly deletePersonaButton: Locator;
  readonly setDefaultPersonaButton: Locator;
  readonly personasList: Locator;

  // Persona form elements
  readonly personaNameInput: Locator;
  readonly personaTypeSelect: Locator;
  readonly personaDisplayNameInput: Locator;
  readonly personaBioTextarea: Locator;
  readonly personaAvatarUpload: Locator;
  readonly personaThemeSelect: Locator;
  readonly personaLanguageSelect: Locator;
  readonly personaTimezoneSelect: Locator;

  // Privacy settings elements
  readonly profileVisibilitySelect: Locator;
  readonly showOnlineStatusToggle: Locator;
  readonly allowMessagesFromSelect: Locator;
  readonly shareActivityDataToggle: Locator;

  // Notification preferences elements
  readonly emailNotificationsToggle: Locator;
  readonly pushNotificationsToggle: Locator;
  readonly desktopNotificationsToggle: Locator;
  readonly marketingNotificationsToggle: Locator;

  // Form actions
  readonly savePersonaButton: Locator;
  readonly cancelPersonaButton: Locator;
  readonly createPersonaSubmit: Locator;

  // Context switching indicators
  readonly contextIndicator: Locator;
  readonly activeSessionsCount: Locator;
  readonly sessionTimeoutWarning: Locator;

  // Template selection
  readonly templateSelector: Locator;
  readonly workTemplate: Locator;
  readonly personalTemplate: Locator;
  readonly studyTemplate: Locator;
  readonly gamingTemplate: Locator;
  readonly createFromTemplateButton: Locator;

  constructor(page: Page) {
    this.page = page;

    // Persona switcher selectors
    this.personaSwitcher = page.locator('[data-testid="persona-switcher"]');
    this.currentPersona = page.locator('[data-testid="current-persona"]');
    this.currentPersonaAvatar = page.locator('[data-testid="current-persona-avatar"]');
    this.currentPersonaName = page.locator('[data-testid="current-persona-name"]');
    this.personaDropdown = page.locator('[data-testid="persona-dropdown"]');
    this.personaOptions = page.locator('[data-testid^="persona-option-"]');

    // Management selectors
    this.createPersonaButton = page.locator('[data-testid="create-persona-button"]');
    this.editPersonaButton = page.locator('[data-testid="edit-persona-button"]');
    this.deletePersonaButton = page.locator('[data-testid="delete-persona-button"]');
    this.setDefaultPersonaButton = page.locator('[data-testid="set-default-persona-button"]');
    this.personasList = page.locator('[data-testid="personas-list"]');

    // Form selectors
    this.personaNameInput = page.locator('[data-testid="persona-name"]');
    this.personaTypeSelect = page.locator('[data-testid="persona-type"]');
    this.personaDisplayNameInput = page.locator('[data-testid="persona-display-name"]');
    this.personaBioTextarea = page.locator('[data-testid="persona-bio"]');
    this.personaAvatarUpload = page.locator('[data-testid="persona-avatar-upload"]');
    this.personaThemeSelect = page.locator('[data-testid="persona-theme"]');
    this.personaLanguageSelect = page.locator('[data-testid="persona-language"]');
    this.personaTimezoneSelect = page.locator('[data-testid="persona-timezone"]');

    // Privacy selectors
    this.profileVisibilitySelect = page.locator('[data-testid="persona-profile-visibility"]');
    this.showOnlineStatusToggle = page.locator('[data-testid="persona-show-online-status"]');
    this.allowMessagesFromSelect = page.locator('[data-testid="persona-allow-messages-from"]');
    this.shareActivityDataToggle = page.locator('[data-testid="persona-share-activity-data"]');

    // Notification selectors
    this.emailNotificationsToggle = page.locator('[data-testid="persona-email-notifications"]');
    this.pushNotificationsToggle = page.locator('[data-testid="persona-push-notifications"]');
    this.desktopNotificationsToggle = page.locator('[data-testid="persona-desktop-notifications"]');
    this.marketingNotificationsToggle = page.locator('[data-testid="persona-marketing-notifications"]');

    // Action selectors
    this.savePersonaButton = page.locator('[data-testid="save-persona-button"]');
    this.cancelPersonaButton = page.locator('[data-testid="cancel-persona-button"]');
    this.createPersonaSubmit = page.locator('[data-testid="create-persona-submit"]');

    // Context selectors
    this.contextIndicator = page.locator('[data-testid="context-indicator"]');
    this.activeSessionsCount = page.locator('[data-testid="active-sessions-count"]');
    this.sessionTimeoutWarning = page.locator('[data-testid="session-timeout-warning"]');

    // Template selectors
    this.templateSelector = page.locator('[data-testid="persona-template-selector"]');
    this.workTemplate = page.locator('[data-testid="template-work"]');
    this.personalTemplate = page.locator('[data-testid="template-personal"]');
    this.studyTemplate = page.locator('[data-testid="template-study"]');
    this.gamingTemplate = page.locator('[data-testid="template-gaming"]');
    this.createFromTemplateButton = page.locator('[data-testid="create-from-template"]');
  }

  /**
   * Navigate to personas management page
   */
  async goToPersonas(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PERSONAS);
    await expect(this.personasList).toBeVisible();
  }

  /**
   * Navigate to persona creation page
   */
  async goToCreatePersona(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PERSONA_CREATE);
    await expect(this.personaNameInput).toBeVisible();
  }

  /**
   * Navigate to persona edit page
   */
  async goToEditPersona(personaId: string): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PERSONA_EDIT(personaId));
    await expect(this.personaNameInput).toBeVisible();
  }

  /**
   * Switch to a different persona
   */
  async switchPersona(personaName: string): Promise<void> {
    await this.personaSwitcher.click();
    await expect(this.personaDropdown).toBeVisible();
    
    await this.page.locator(`[data-testid="persona-option-${personaName}"]`).click();
    
    // Wait for context switch to complete
    await expect(this.currentPersonaName).toContainText(personaName);
    await expect(this.personaDropdown).not.toBeVisible();
  }

  /**
   * Create a new persona from scratch
   */
  async createPersona(personaData: Partial<TestPersona>): Promise<void> {
    await this.goToCreatePersona();

    // Fill basic information
    await this.personaNameInput.fill(personaData.name || 'Test Persona');
    await this.personaTypeSelect.selectOption(personaData.type || 'PERSONAL');
    
    if (personaData.displayName) {
      await this.personaDisplayNameInput.fill(personaData.displayName);
    }
    
    if (personaData.bio) {
      await this.personaBioTextarea.fill(personaData.bio);
    }

    // Set theme and localization
    if (personaData.themePreference) {
      await this.personaThemeSelect.selectOption(personaData.themePreference);
    }
    
    if (personaData.language) {
      await this.personaLanguageSelect.selectOption(personaData.language);
    }
    
    if (personaData.timezone) {
      await this.personaTimezoneSelect.selectOption(personaData.timezone);
    }

    // Configure privacy settings
    if (personaData.privacySettings) {
      await this.profileVisibilitySelect.selectOption(personaData.privacySettings.profileVisibility);
      
      await this.setToggleState(this.showOnlineStatusToggle, personaData.privacySettings.showOnlineStatus);
      await this.allowMessagesFromSelect.selectOption(personaData.privacySettings.allowMessagesFrom);
      await this.setToggleState(this.shareActivityDataToggle, personaData.privacySettings.shareActivityData);
    }

    // Configure notifications
    if (personaData.notificationPreferences) {
      await this.setToggleState(this.emailNotificationsToggle, personaData.notificationPreferences.email);
      await this.setToggleState(this.pushNotificationsToggle, personaData.notificationPreferences.push);
      await this.setToggleState(this.desktopNotificationsToggle, personaData.notificationPreferences.desktop);
      await this.setToggleState(this.marketingNotificationsToggle, personaData.notificationPreferences.marketing);
    }

    // Submit form
    await this.createPersonaSubmit.click();
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
  }

  /**
   * Create persona from template
   */
  async createPersonaFromTemplate(templateType: 'work' | 'personal' | 'study' | 'gaming'): Promise<void> {
    await this.goToCreatePersona();
    
    // Open template selector
    await this.templateSelector.click();
    
    // Select template
    const templateLocator = {
      work: this.workTemplate,
      personal: this.personalTemplate,
      study: this.studyTemplate,
      gaming: this.gamingTemplate
    };
    
    await templateLocator[templateType].click();
    await this.createFromTemplateButton.click();
    
    // Form should be pre-filled
    await expect(this.personaNameInput).not.toHaveValue('');
    await expect(this.personaTypeSelect).not.toHaveValue('');
    
    // Submit form
    await this.createPersonaSubmit.click();
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
  }

  /**
   * Edit existing persona
   */
  async editPersona(personaName: string, updates: Partial<TestPersona>): Promise<void> {
    await this.goToPersonas();
    
    // Find and click edit button for the persona
    await this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="edit-button"]`).click();
    
    // Update fields
    if (updates.displayName) {
      await this.personaDisplayNameInput.clear();
      await this.personaDisplayNameInput.fill(updates.displayName);
    }
    
    if (updates.bio) {
      await this.personaBioTextarea.clear();
      await this.personaBioTextarea.fill(updates.bio);
    }
    
    if (updates.themePreference) {
      await this.personaThemeSelect.selectOption(updates.themePreference);
    }

    // Save changes
    await this.savePersonaButton.click();
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
  }

  /**
   * Delete persona
   */
  async deletePersona(personaName: string): Promise<void> {
    await this.goToPersonas();
    
    // Find and click delete button for the persona
    await this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="delete-button"]`).click();
    
    // Confirm deletion
    await expect(this.page.locator('[data-testid="delete-persona-modal"]')).toBeVisible();
    await this.page.locator('[data-testid="confirm-delete-persona"]').click();
    
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
  }

  /**
   * Set persona as default
   */
  async setAsDefault(personaName: string): Promise<void> {
    await this.goToPersonas();
    
    // Find and click set default button for the persona
    await this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="set-default-button"]`).click();
    
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
    
    // Verify default indicator appears
    await expect(this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="default-badge"]`)).toBeVisible();
  }

  /**
   * Upload persona avatar
   */
  async uploadPersonaAvatar(personaName: string, filePath: string): Promise<void> {
    await this.goToPersonas();
    
    // Open persona for editing
    await this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="edit-button"]`).click();
    
    // Upload avatar
    await this.personaAvatarUpload.setInputFiles(filePath);
    await this.savePersonaButton.click();
    
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
  }

  /**
   * Verify persona appears in switcher dropdown
   */
  async verifyPersonaInSwitcher(personaName: string): Promise<void> {
    await this.personaSwitcher.click();
    await expect(this.page.locator(`[data-testid="persona-option-${personaName}"]`)).toBeVisible();
    
    // Close dropdown
    await this.page.keyboard.press('Escape');
  }

  /**
   * Verify current active persona
   */
  async verifyCurrentPersona(personaName: string): Promise<void> {
    await expect(this.currentPersonaName).toContainText(personaName);
  }

  /**
   * Verify persona exists in management list
   */
  async verifyPersonaExists(personaName: string): Promise<void> {
    await this.goToPersonas();
    await expect(this.page.locator(`[data-testid="persona-item-${personaName}"]`)).toBeVisible();
  }

  /**
   * Verify persona does not exist
   */
  async verifyPersonaNotExists(personaName: string): Promise<void> {
    await this.goToPersonas();
    await expect(this.page.locator(`[data-testid="persona-item-${personaName}"]`)).not.toBeVisible();
  }

  /**
   * Get active sessions count
   */
  async getActiveSessionsCount(): Promise<number> {
    const text = await this.activeSessionsCount.textContent();
    return parseInt(text || '0', 10);
  }

  /**
   * Verify context switching performance
   */
  async verifySwitchPerformance(personaName: string, maxTime: number = 500): Promise<number> {
    const startTime = Date.now();
    
    await this.switchPersona(personaName);
    
    const switchTime = Date.now() - startTime;
    expect(switchTime).toBeLessThan(maxTime);
    
    return switchTime;
  }

  /**
   * Check if session timeout warning is visible
   */
  async hasSessionTimeoutWarning(): Promise<boolean> {
    return await this.sessionTimeoutWarning.isVisible();
  }

  /**
   * Verify persona privacy settings
   */
  async verifyPersonaPrivacySettings(personaName: string, expectedSettings: {
    profileVisibility?: string;
    showOnlineStatus?: boolean;
    allowMessagesFrom?: string;
    shareActivityData?: boolean;
  }): Promise<void> {
    await this.goToPersonas();
    await this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="edit-button"]`).click();

    if (expectedSettings.profileVisibility) {
      await expect(this.profileVisibilitySelect).toHaveValue(expectedSettings.profileVisibility);
    }

    if (expectedSettings.showOnlineStatus !== undefined) {
      const isChecked = await this.showOnlineStatusToggle.isChecked();
      expect(isChecked).toBe(expectedSettings.showOnlineStatus);
    }

    if (expectedSettings.allowMessagesFrom) {
      await expect(this.allowMessagesFromSelect).toHaveValue(expectedSettings.allowMessagesFrom);
    }

    if (expectedSettings.shareActivityData !== undefined) {
      const isChecked = await this.shareActivityDataToggle.isChecked();
      expect(isChecked).toBe(expectedSettings.shareActivityData);
    }
  }

  /**
   * Verify notification preferences
   */
  async verifyNotificationPreferences(personaName: string, expectedPreferences: {
    email?: boolean;
    push?: boolean;
    desktop?: boolean;
    marketing?: boolean;
  }): Promise<void> {
    await this.goToPersonas();
    await this.page.locator(`[data-testid="persona-item-${personaName}"] [data-testid="edit-button"]`).click();

    if (expectedPreferences.email !== undefined) {
      const isChecked = await this.emailNotificationsToggle.isChecked();
      expect(isChecked).toBe(expectedPreferences.email);
    }

    if (expectedPreferences.push !== undefined) {
      const isChecked = await this.pushNotificationsToggle.isChecked();
      expect(isChecked).toBe(expectedPreferences.push);
    }

    if (expectedPreferences.desktop !== undefined) {
      const isChecked = await this.desktopNotificationsToggle.isChecked();
      expect(isChecked).toBe(expectedPreferences.desktop);
    }

    if (expectedPreferences.marketing !== undefined) {
      const isChecked = await this.marketingNotificationsToggle.isChecked();
      expect(isChecked).toBe(expectedPreferences.marketing);
    }
  }

  /**
   * Helper method to set toggle state
   */
  private async setToggleState(toggle: Locator, desiredState: boolean): Promise<void> {
    const currentState = await toggle.isChecked();
    if (currentState !== desiredState) {
      await toggle.click();
    }
  }

  /**
   * Get list of all persona names
   */
  async getPersonaNames(): Promise<string[]> {
    await this.goToPersonas();
    
    const personaItems = await this.page.locator('[data-testid^="persona-item-"]').all();
    const names: string[] = [];
    
    for (const item of personaItems) {
      const nameElement = item.locator('[data-testid="persona-name"]');
      const name = await nameElement.textContent();
      if (name) {
        names.push(name.trim());
      }
    }
    
    return names;
  }

  /**
   * Verify persona order in switcher matches priority
   */
  async verifyPersonaOrder(expectedOrder: string[]): Promise<void> {
    await this.personaSwitcher.click();
    
    const personaElements = await this.personaOptions.all();
    const actualOrder: string[] = [];
    
    for (const element of personaElements) {
      const text = await element.textContent();
      if (text) {
        actualOrder.push(text.trim());
      }
    }
    
    expect(actualOrder).toEqual(expectedOrder);
    
    // Close dropdown
    await this.page.keyboard.press('Escape');
  }
}