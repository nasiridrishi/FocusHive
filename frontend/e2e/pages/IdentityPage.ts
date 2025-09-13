/**
 * Page Object Model for Identity and Profile Management
 * 
 * Provides a structured interface for interacting with identity-related UI elements
 * Supports authentication, profile management, privacy settings, and OAuth2 flows
 * 
 * @fileoverview Identity page object model for E2E tests
 * @version 1.0.0
 */

import { expect, type Page, type Locator } from '@playwright/test';
import { IDENTITY_ROUTES } from '../tests/identity/identity.config';

export class IdentityPage {
  readonly page: Page;

  // Authentication elements
  readonly loginForm: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginSubmit: Locator;
  readonly logoutButton: Locator;
  readonly registerLink: Locator;
  readonly forgotPasswordLink: Locator;

  // Profile elements
  readonly userMenu: Locator;
  readonly profileLink: Locator;
  readonly displayNameInput: Locator;
  readonly bioTextarea: Locator;
  readonly avatarUpload: Locator;
  readonly saveProfileButton: Locator;

  // Navigation elements
  readonly profileTab: Locator;
  readonly personasTab: Locator;
  readonly privacyTab: Locator;
  readonly securityTab: Locator;
  readonly dataTab: Locator;
  readonly oauthAppsTab: Locator;

  // Success/error messages
  readonly successMessage: Locator;
  readonly errorMessage: Locator;
  readonly validationErrors: Locator;

  constructor(page: Page) {
    this.page = page;

    // Authentication selectors
    this.loginForm = page.locator('[data-testid="login-form"]');
    this.emailInput = page.locator('[data-testid="email-input"]');
    this.passwordInput = page.locator('[data-testid="password-input"]');
    this.loginSubmit = page.locator('[data-testid="login-submit"]');
    this.logoutButton = page.locator('[data-testid="logout-button"]');
    this.registerLink = page.locator('[data-testid="register-link"]');
    this.forgotPasswordLink = page.locator('[data-testid="forgot-password-link"]');

    // Profile selectors
    this.userMenu = page.locator('[data-testid="user-menu"]');
    this.profileLink = page.locator('[data-testid="profile-link"]');
    this.displayNameInput = page.locator('[data-testid="display-name-input"]');
    this.bioTextarea = page.locator('[data-testid="bio-textarea"]');
    this.avatarUpload = page.locator('[data-testid="avatar-upload"]');
    this.saveProfileButton = page.locator('[data-testid="save-profile-button"]');

    // Navigation selectors
    this.profileTab = page.locator('[data-testid="profile-tab"]');
    this.personasTab = page.locator('[data-testid="personas-tab"]');
    this.privacyTab = page.locator('[data-testid="privacy-tab"]');
    this.securityTab = page.locator('[data-testid="security-tab"]');
    this.dataTab = page.locator('[data-testid="data-tab"]');
    this.oauthAppsTab = page.locator('[data-testid="oauth-apps-tab"]');

    // Message selectors
    this.successMessage = page.locator('[data-testid="success-message"]');
    this.errorMessage = page.locator('[data-testid="error-message"]');
    this.validationErrors = page.locator('[data-testid="validation-error"]');
  }

  /**
   * Navigate to login page
   */
  async goToLogin(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.LOGIN);
    await expect(this.loginForm).toBeVisible();
  }

  /**
   * Navigate to registration page
   */
  async goToRegister(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.REGISTER);
    await expect(this.page.locator('[data-testid="register-form"]')).toBeVisible();
  }

  /**
   * Navigate to profile page
   */
  async goToProfile(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PROFILE);
    await expect(this.profileTab).toBeVisible();
  }

  /**
   * Navigate to privacy settings
   */
  async goToPrivacySettings(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PRIVACY);
    await expect(this.privacyTab).toBeVisible();
  }

  /**
   * Navigate to OAuth2 apps management
   */
  async goToOAuthApps(): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.OAUTH2_APPS);
    await expect(this.oauthAppsTab).toBeVisible();
  }

  /**
   * Perform login
   */
  async login(email: string, password: string): Promise<void> {
    await this.goToLogin();
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    
    const navigationPromise = this.page.waitForURL('**/dashboard', { timeout: 10000 });
    await this.loginSubmit.click();
    await navigationPromise;
    
    await expect(this.userMenu).toBeVisible();
  }

  /**
   * Perform logout
   */
  async logout(): Promise<void> {
    await this.userMenu.click();
    await this.logoutButton.click();
    
    await this.page.waitForURL('**/auth/login');
    await expect(this.loginForm).toBeVisible();
  }

  /**
   * Update profile information
   */
  async updateProfile(displayName?: string, bio?: string): Promise<void> {
    await this.goToProfile();

    if (displayName) {
      await this.displayNameInput.clear();
      await this.displayNameInput.fill(displayName);
    }

    if (bio) {
      await this.bioTextarea.clear();
      await this.bioTextarea.fill(bio);
    }

    await this.saveProfileButton.click();
    await expect(this.successMessage).toBeVisible();
  }

  /**
   * Upload avatar image
   */
  async uploadAvatar(filePath: string): Promise<void> {
    await this.goToProfile();
    await this.avatarUpload.setInputFiles(filePath);
    await this.saveProfileButton.click();
    await expect(this.successMessage).toBeVisible();
  }

  /**
   * Verify user is authenticated
   */
  async verifyAuthenticated(): Promise<void> {
    await expect(this.userMenu).toBeVisible();
  }

  /**
   * Verify user is not authenticated
   */
  async verifyNotAuthenticated(): Promise<void> {
    await expect(this.loginForm).toBeVisible();
  }

  /**
   * Wait for success message
   */
  async waitForSuccess(): Promise<void> {
    await expect(this.successMessage).toBeVisible();
  }

  /**
   * Wait for error message
   */
  async waitForError(): Promise<void> {
    await expect(this.errorMessage).toBeVisible();
  }

  /**
   * Get validation error text
   */
  async getValidationError(): Promise<string> {
    await expect(this.validationErrors).toBeVisible();
    return await this.validationErrors.textContent() || '';
  }

  /**
   * Switch to specific tab
   */
  async switchToTab(tab: 'profile' | 'personas' | 'privacy' | 'security' | 'data' | 'oauth'): Promise<void> {
    const tabSelector = {
      profile: this.profileTab,
      personas: this.personasTab,
      privacy: this.privacyTab,
      security: this.securityTab,
      data: this.dataTab,
      oauth: this.oauthAppsTab
    };

    await tabSelector[tab].click();
    await expect(tabSelector[tab]).toHaveClass(/active|selected/);
  }

  /**
   * Verify profile data is displayed
   */
  async verifyProfileData(expectedData: {
    displayName?: string;
    email?: string;
    bio?: string;
  }): Promise<void> {
    if (expectedData.displayName) {
      await expect(this.displayNameInput).toHaveValue(expectedData.displayName);
    }

    if (expectedData.bio) {
      await expect(this.bioTextarea).toHaveValue(expectedData.bio);
    }

    if (expectedData.email) {
      await expect(this.page.locator('[data-testid="profile-email"]')).toContainText(expectedData.email);
    }
  }
}

/**
 * Privacy Settings Page Object Model
 */
export class PrivacySettingsPage extends IdentityPage {
  // Privacy control elements
  readonly profileVisibilitySelect: Locator;
  readonly showOnlineStatusToggle: Locator;
  readonly allowMessagesFromSelect: Locator;
  readonly shareActivityDataToggle: Locator;
  readonly allowDataExportToggle: Locator;
  readonly twoFactorToggle: Locator;
  readonly sessionTimeoutInput: Locator;
  readonly savePrivacyButton: Locator;

  // Data management elements
  readonly requestDataExportButton: Locator;
  readonly dataExportFormatSelect: Locator;
  readonly deleteAccountButton: Locator;
  readonly confirmDeleteInput: Locator;
  readonly confirmDeleteButton: Locator;

  constructor(page: Page) {
    super(page);

    // Privacy controls
    this.profileVisibilitySelect = page.locator('[data-testid="profile-visibility-select"]');
    this.showOnlineStatusToggle = page.locator('[data-testid="show-online-status-toggle"]');
    this.allowMessagesFromSelect = page.locator('[data-testid="allow-messages-from-select"]');
    this.shareActivityDataToggle = page.locator('[data-testid="share-activity-data-toggle"]');
    this.allowDataExportToggle = page.locator('[data-testid="allow-data-export-toggle"]');
    this.twoFactorToggle = page.locator('[data-testid="two-factor-toggle"]');
    this.sessionTimeoutInput = page.locator('[data-testid="session-timeout-input"]');
    this.savePrivacyButton = page.locator('[data-testid="save-privacy-button"]');

    // Data management
    this.requestDataExportButton = page.locator('[data-testid="request-data-export-button"]');
    this.dataExportFormatSelect = page.locator('[data-testid="data-export-format-select"]');
    this.deleteAccountButton = page.locator('[data-testid="delete-account-button"]');
    this.confirmDeleteInput = page.locator('[data-testid="confirm-delete-input"]');
    this.confirmDeleteButton = page.locator('[data-testid="confirm-delete-button"]');
  }

  /**
   * Update privacy visibility setting
   */
  async updateProfileVisibility(visibility: 'PUBLIC' | 'FRIENDS' | 'PRIVATE'): Promise<void> {
    await this.goToPrivacySettings();
    await this.profileVisibilitySelect.selectOption(visibility);
    await this.savePrivacyButton.click();
    await this.waitForSuccess();
  }

  /**
   * Toggle online status visibility
   */
  async toggleOnlineStatus(show: boolean): Promise<void> {
    await this.goToPrivacySettings();
    
    const currentState = await this.showOnlineStatusToggle.isChecked();
    if (currentState !== show) {
      await this.showOnlineStatusToggle.click();
    }
    
    await this.savePrivacyButton.click();
    await this.waitForSuccess();
  }

  /**
   * Set message permissions
   */
  async updateMessagePermissions(allowFrom: 'EVERYONE' | 'FRIENDS' | 'NOBODY'): Promise<void> {
    await this.goToPrivacySettings();
    await this.allowMessagesFromSelect.selectOption(allowFrom);
    await this.savePrivacyButton.click();
    await this.waitForSuccess();
  }

  /**
   * Request data export
   */
  async requestDataExport(format: 'JSON' | 'CSV' = 'JSON'): Promise<void> {
    await this.goToPrivacySettings();
    await this.switchToTab('data');
    
    await this.dataExportFormatSelect.selectOption(format);
    await this.requestDataExportButton.click();
    
    await expect(this.page.locator('[data-testid="export-request-confirmation"]')).toBeVisible();
  }

  /**
   * Initiate account deletion
   */
  async initiateAccountDeletion(confirmationText: string): Promise<void> {
    await this.goToPrivacySettings();
    await this.switchToTab('data');
    
    await this.deleteAccountButton.click();
    await expect(this.page.locator('[data-testid="delete-account-modal"]')).toBeVisible();
    
    await this.confirmDeleteInput.fill(confirmationText);
    await this.confirmDeleteButton.click();
  }

  /**
   * Verify privacy settings
   */
  async verifyPrivacySettings(expectedSettings: {
    profileVisibility?: string;
    showOnlineStatus?: boolean;
    allowMessagesFrom?: string;
    shareActivityData?: boolean;
    twoFactorEnabled?: boolean;
  }): Promise<void> {
    await this.goToPrivacySettings();

    if (expectedSettings.profileVisibility) {
      await expect(this.profileVisibilitySelect).toHaveValue(expectedSettings.profileVisibility);
    }

    if (expectedSettings.showOnlineStatus !== undefined) {
      if (expectedSettings.showOnlineStatus) {
        await expect(this.showOnlineStatusToggle).toBeChecked();
      } else {
        await expect(this.showOnlineStatusToggle).not.toBeChecked();
      }
    }

    if (expectedSettings.allowMessagesFrom) {
      await expect(this.allowMessagesFromSelect).toHaveValue(expectedSettings.allowMessagesFrom);
    }

    if (expectedSettings.shareActivityData !== undefined) {
      if (expectedSettings.shareActivityData) {
        await expect(this.shareActivityDataToggle).toBeChecked();
      } else {
        await expect(this.shareActivityDataToggle).not.toBeChecked();
      }
    }

    if (expectedSettings.twoFactorEnabled !== undefined) {
      if (expectedSettings.twoFactorEnabled) {
        await expect(this.twoFactorToggle).toBeChecked();
      } else {
        await expect(this.twoFactorToggle).not.toBeChecked();
      }
    }
  }
}

/**
 * OAuth2 Applications Management Page Object Model
 */
export class OAuth2AppsPage extends IdentityPage {
  // OAuth2 app management elements
  readonly createAppButton: Locator;
  readonly appsList: Locator;
  readonly appName: Locator;
  readonly appDescription: Locator;
  readonly redirectUrisTextarea: Locator;
  readonly scopesCheckboxes: Locator;
  readonly confidentialToggle: Locator;
  readonly createAppSubmit: Locator;

  // App details elements
  readonly clientIdDisplay: Locator;
  readonly clientSecretDisplay: Locator;
  readonly copyClientIdButton: Locator;
  readonly copyClientSecretButton: Locator;
  readonly regenerateSecretButton: Locator;
  readonly deleteAppButton: Locator;

  // Authorization elements
  readonly authorizeButton: Locator;
  readonly denyButton: Locator;
  readonly consentForm: Locator;
  readonly requestedScopesList: Locator;
  readonly clientNameDisplay: Locator;

  constructor(page: Page) {
    super(page);

    // App management
    this.createAppButton = page.locator('[data-testid="create-app-button"]');
    this.appsList = page.locator('[data-testid="apps-list"]');
    this.appName = page.locator('[data-testid="app-name-input"]');
    this.appDescription = page.locator('[data-testid="app-description-input"]');
    this.redirectUrisTextarea = page.locator('[data-testid="redirect-uris-textarea"]');
    this.scopesCheckboxes = page.locator('[data-testid^="scope-checkbox-"]');
    this.confidentialToggle = page.locator('[data-testid="confidential-toggle"]');
    this.createAppSubmit = page.locator('[data-testid="create-app-submit"]');

    // App details
    this.clientIdDisplay = page.locator('[data-testid="client-id-display"]');
    this.clientSecretDisplay = page.locator('[data-testid="client-secret-display"]');
    this.copyClientIdButton = page.locator('[data-testid="copy-client-id"]');
    this.copyClientSecretButton = page.locator('[data-testid="copy-client-secret"]');
    this.regenerateSecretButton = page.locator('[data-testid="regenerate-secret-button"]');
    this.deleteAppButton = page.locator('[data-testid="delete-app-button"]');

    // Authorization
    this.authorizeButton = page.locator('[data-testid="authorize-button"]');
    this.denyButton = page.locator('[data-testid="deny-button"]');
    this.consentForm = page.locator('[data-testid="oauth-consent-form"]');
    this.requestedScopesList = page.locator('[data-testid="requested-scopes"]');
    this.clientNameDisplay = page.locator('[data-testid="client-name"]');
  }

  /**
   * Create new OAuth2 application
   */
  async createOAuthApp(appData: {
    name: string;
    description?: string;
    redirectUris: string[];
    scopes: string[];
    confidential?: boolean;
  }): Promise<void> {
    await this.goToOAuthApps();
    await this.createAppButton.click();

    await this.appName.fill(appData.name);
    
    if (appData.description) {
      await this.appDescription.fill(appData.description);
    }

    await this.redirectUrisTextarea.fill(appData.redirectUris.join('\n'));

    // Select scopes
    for (const scope of appData.scopes) {
      await this.page.locator(`[data-testid="scope-checkbox-${scope}"]`).check();
    }

    if (appData.confidential !== undefined) {
      const isChecked = await this.confidentialToggle.isChecked();
      if (isChecked !== appData.confidential) {
        await this.confidentialToggle.click();
      }
    }

    await this.createAppSubmit.click();
    await this.waitForSuccess();
  }

  /**
   * View app details
   */
  async viewAppDetails(appName: string): Promise<{ clientId: string; clientSecret?: string }> {
    await this.goToOAuthApps();
    await this.page.locator(`[data-testid="app-item-${appName}"]`).click();

    await expect(this.clientIdDisplay).toBeVisible();
    const clientId = await this.clientIdDisplay.textContent() || '';

    let clientSecret: string | undefined;
    if (await this.clientSecretDisplay.isVisible()) {
      clientSecret = await this.clientSecretDisplay.textContent() || undefined;
    }

    return { clientId, clientSecret };
  }

  /**
   * Delete OAuth2 application
   */
  async deleteApp(appName: string): Promise<void> {
    await this.goToOAuthApps();
    await this.page.locator(`[data-testid="app-item-${appName}"]`).click();
    
    await this.deleteAppButton.click();
    
    // Confirm deletion
    await this.page.locator('[data-testid="confirm-delete-app"]').click();
    await this.waitForSuccess();
  }

  /**
   * Handle OAuth2 authorization consent
   */
  async handleAuthorizationConsent(authorize: boolean): Promise<void> {
    await expect(this.consentForm).toBeVisible();
    await expect(this.clientNameDisplay).toBeVisible();
    await expect(this.requestedScopesList).toBeVisible();

    if (authorize) {
      await this.authorizeButton.click();
    } else {
      await this.denyButton.click();
    }
  }

  /**
   * Verify app appears in list
   */
  async verifyAppExists(appName: string): Promise<void> {
    await this.goToOAuthApps();
    await expect(this.page.locator(`[data-testid="app-item-${appName}"]`)).toBeVisible();
  }

  /**
   * Copy client credentials to clipboard
   */
  async copyClientCredentials(): Promise<void> {
    await this.copyClientIdButton.click();
    await expect(this.page.locator('[data-testid="copy-success"]')).toBeVisible();

    if (await this.copyClientSecretButton.isVisible()) {
      await this.copyClientSecretButton.click();
      await expect(this.page.locator('[data-testid="copy-success"]')).toBeVisible();
    }
  }
}