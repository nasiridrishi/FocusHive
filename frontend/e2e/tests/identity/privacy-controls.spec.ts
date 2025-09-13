/**
 * E2E Tests for Privacy Controls and Data Access Management
 * 
 * Comprehensive testing of privacy settings, data access permissions, GDPR compliance,
 * and data protection features within the FocusHive Identity Service
 * 
 * @fileoverview Privacy controls E2E tests
 * @version 1.0.0
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { IdentityPage, PrivacySettingsPage } from '../../pages/IdentityPage';
import { PersonaSwitcherPage } from '../../pages/PersonaSwitcherPage';
import {
  AuthenticationHelper,
  PrivacyHelper,
  PerformanceHelper,
  AccessibilityHelper,
  SecurityHelper
} from '../../helpers/identity/identity-helpers';
import {
  IdentityTestDataManager,
  IdentityTestDataFactory,
  type TestUser,
  type TestPersona
} from '../../fixtures/identity/identity-fixtures';
import { 
  TEST_USERS, 
  PERFORMANCE_THRESHOLDS,
  SECURITY_CONFIG
} from './identity.config';

test.describe('Privacy Controls and Data Access Management', () => {
  let page: Page;
  let apiContext: APIRequestContext;
  let identityPage: IdentityPage;
  let privacyPage: PrivacySettingsPage;
  let personaPage: PersonaSwitcherPage;
  let authHelper: AuthenticationHelper;
  let privacyHelper: PrivacyHelper;
  let perfHelper: PerformanceHelper;
  let a11yHelper: AccessibilityHelper;
  let securityHelper: SecurityHelper;
  let dataManager: IdentityTestDataManager;
  let testUser: TestUser;
  let privacyUser: TestUser;

  test.beforeAll(async ({ browser, playwright }) => {
    // Create API context for setup
    apiContext = await playwright.request.newContext({
      baseURL: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081'
    });

    // Create browser context and page
    const context = await browser.newContext();
    page = await context.newPage();

    // Initialize page objects and helpers
    identityPage = new IdentityPage(page);
    privacyPage = new PrivacySettingsPage(page);
    personaPage = new PersonaSwitcherPage(page);
    authHelper = new AuthenticationHelper(page, apiContext);
    privacyHelper = new PrivacyHelper(page, apiContext);
    perfHelper = new PerformanceHelper(page);
    a11yHelper = new AccessibilityHelper(page);
    securityHelper = new SecurityHelper(page, apiContext);
    dataManager = new IdentityTestDataManager(page, apiContext);

    // Create test users
    testUser = await dataManager.createUser(TEST_USERS.MULTI_PERSONA_USER);
    privacyUser = await dataManager.createUser(TEST_USERS.PRIVACY_USER);
  });

  test.beforeEach(async () => {
    // Login before each test
    await authHelper.loginViaAPI(testUser.email, testUser.password);
    await page.goto('/dashboard');
    await authHelper.verifyAuthenticated();
  });

  test.afterAll(async () => {
    // Cleanup test data
    await dataManager.cleanup();
    await apiContext.dispose();
    await page.close();
  });

  test.describe('Profile Visibility Controls', () => {
    test('should update profile visibility to public', async () => {
      await privacyPage.updateProfileVisibility('PUBLIC');

      // Verify setting was saved
      await privacyPage.verifyPrivacySettings({
        profileVisibility: 'PUBLIC'
      });

      // Verify visibility reflects in persona settings
      const activePersona = await personaPage.currentPersonaName.textContent();
      if (activePersona) {
        await personaPage.verifyPersonaPrivacySettings(activePersona, {
          profileVisibility: 'PUBLIC'
        });
      }
    });

    test('should update profile visibility to friends only', async () => {
      await privacyPage.updateProfileVisibility('FRIENDS');

      await privacyPage.verifyPrivacySettings({
        profileVisibility: 'FRIENDS'
      });
    });

    test('should update profile visibility to private', async () => {
      await privacyPage.updateProfileVisibility('PRIVATE');

      await privacyPage.verifyPrivacySettings({
        profileVisibility: 'PRIVATE'
      });

      // Verify additional privacy restrictions are applied
      await privacyPage.verifyPrivacySettings({
        showOnlineStatus: false,
        allowMessagesFrom: 'NOBODY'
      });
    });

    test('should apply visibility changes to all personas', async () => {
      // Create multiple personas
      const workPersona = IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Work Privacy Test'
      });
      const personalPersona = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Personal Privacy Test'
      });

      await dataManager.createPersonas(testUser.id!, [workPersona, personalPersona], testUser.accessToken!);

      // Update global privacy setting
      await privacyPage.updateProfileVisibility('PRIVATE');

      // Verify both personas inherit the setting
      await personaPage.verifyPersonaPrivacySettings(workPersona.name, {
        profileVisibility: 'PRIVATE'
      });
      await personaPage.verifyPersonaPrivacySettings(personalPersona.name, {
        profileVisibility: 'PRIVATE'
      });
    });

    test('should handle persona-specific visibility overrides', async () => {
      // Create a work persona
      const workPersona = IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Work Override Test',
        privacySettings: IdentityTestDataFactory.createPrivacySettings({
          profileVisibility: 'PUBLIC'
        })
      });

      await dataManager.createPersonas(testUser.id!, [workPersona], testUser.accessToken!);

      // Set global visibility to private
      await privacyPage.updateProfileVisibility('PRIVATE');

      // Work persona should maintain its public visibility
      await personaPage.verifyPersonaPrivacySettings(workPersona.name, {
        profileVisibility: 'PUBLIC'
      });
    });
  });

  test.describe('Online Status and Messaging Controls', () => {
    test('should toggle online status visibility', async () => {
      await privacyPage.toggleOnlineStatus(false);

      await privacyPage.verifyPrivacySettings({
        showOnlineStatus: false
      });

      // Verify status is not shown in UI
      await expect(page.locator('[data-testid="online-status-indicator"]')).not.toBeVisible();
    });

    test('should update message permissions to everyone', async () => {
      await privacyPage.updateMessagePermissions('EVERYONE');

      await privacyPage.verifyPrivacySettings({
        allowMessagesFrom: 'EVERYONE'
      });
    });

    test('should update message permissions to friends only', async () => {
      await privacyPage.updateMessagePermissions('FRIENDS');

      await privacyPage.verifyPrivacySettings({
        allowMessagesFrom: 'FRIENDS'
      });
    });

    test('should update message permissions to nobody', async () => {
      await privacyPage.updateMessagePermissions('NOBODY');

      await privacyPage.verifyPrivacySettings({
        allowMessagesFrom: 'NOBODY'
      });

      // Verify messaging UI is disabled
      await expect(page.locator('[data-testid="send-message-button"]')).toBeDisabled();
    });

    test('should enforce message permissions across personas', async () => {
      // Set strict message permissions
      await privacyPage.updateMessagePermissions('NOBODY');

      // Switch personas and verify restriction applies
      const personalPersona = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Message Test Personal'
      });
      await dataManager.createPersonas(testUser.id!, [personalPersona], testUser.accessToken!);
      
      await personaPage.switchPersona(personalPersona.name);
      await expect(page.locator('[data-testid="send-message-button"]')).toBeDisabled();
    });
  });

  test.describe('Activity Data Sharing Controls', () => {
    test('should disable activity data sharing', async () => {
      await privacyPage.goToPrivacySettings();
      
      // Disable activity data sharing
      const currentState = await privacyPage.shareActivityDataToggle.isChecked();
      if (currentState) {
        await privacyPage.shareActivityDataToggle.click();
      }
      
      await privacyPage.savePrivacyButton.click();
      await privacyPage.waitForSuccess();

      // Verify setting was saved
      await privacyPage.verifyPrivacySettings({
        shareActivityData: false
      });

      // Verify analytics collection is disabled
      await expect(page.locator('[data-testid="analytics-disabled-notice"]')).toBeVisible();
    });

    test('should enable activity data sharing with consent', async () => {
      await privacyPage.goToPrivacySettings();
      
      // Enable activity data sharing
      const currentState = await privacyPage.shareActivityDataToggle.isChecked();
      if (!currentState) {
        await privacyPage.shareActivityDataToggle.click();
      }

      // Should show consent dialog
      await expect(page.locator('[data-testid="data-sharing-consent-modal"]')).toBeVisible();
      await expect(page.locator('[data-testid="consent-details"]')).toContainText('productivity analytics');
      
      // Accept consent
      await page.locator('[data-testid="accept-consent"]').click();
      
      await privacyPage.savePrivacyButton.click();
      await privacyPage.waitForSuccess();

      await privacyPage.verifyPrivacySettings({
        shareActivityData: true
      });
    });

    test('should show detailed data usage information', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Click on data usage details
      await page.locator('[data-testid="data-usage-details"]').click();

      // Verify detailed information is shown
      await expect(page.locator('[data-testid="data-collection-info"]')).toBeVisible();
      await expect(page.locator('[data-testid="data-retention-info"]')).toBeVisible();
      await expect(page.locator('[data-testid="data-sharing-partners"]')).toBeVisible();
      await expect(page.locator('[data-testid="user-rights-info"]')).toBeVisible();
    });

    test('should allow granular control over data types', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Toggle specific data types
      await page.locator('[data-testid="share-focus-times"]').click();
      await page.locator('[data-testid="share-break-patterns"]').uncheck();
      await page.locator('[data-testid="share-productivity-scores"]').click();

      await privacyPage.savePrivacyButton.click();
      await privacyPage.waitForSuccess();

      // Verify granular settings were saved
      await expect(page.locator('[data-testid="share-focus-times"]')).toBeChecked();
      await expect(page.locator('[data-testid="share-break-patterns"]')).not.toBeChecked();
      await expect(page.locator('[data-testid="share-productivity-scores"]')).toBeChecked();
    });
  });

  test.describe('Two-Factor Authentication', () => {
    test('should enable two-factor authentication', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('security');

      // Enable 2FA
      await privacyPage.twoFactorToggle.click();

      // Should show 2FA setup process
      await expect(page.locator('[data-testid="2fa-setup-modal"]')).toBeVisible();
      await expect(page.locator('[data-testid="qr-code"]')).toBeVisible();
      await expect(page.locator('[data-testid="backup-codes"]')).toBeVisible();

      // Enter verification code (simulated)
      await page.locator('[data-testid="2fa-code-input"]').fill('123456');
      await page.locator('[data-testid="verify-2fa-button"]').click();

      // Should show success and save backup codes
      await expect(page.locator('[data-testid="2fa-enabled-success"]')).toBeVisible();
      await page.locator('[data-testid="download-backup-codes"]').click();

      await privacyPage.verifyPrivacySettings({
        twoFactorEnabled: true
      });
    });

    test('should disable two-factor authentication with verification', async () => {
      // First enable 2FA (using API for speed)
      await apiContext.post(`${process.env.E2E_IDENTITY_API_URL}/api/v1/auth/2fa/enable`, {
        headers: { 'Authorization': `Bearer ${testUser.accessToken}` },
        data: { code: '123456' }
      });

      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('security');

      // Disable 2FA
      await privacyPage.twoFactorToggle.click();

      // Should require current password and 2FA code
      await expect(page.locator('[data-testid="disable-2fa-modal"]')).toBeVisible();
      await page.locator('[data-testid="current-password-input"]').fill(testUser.password);
      await page.locator('[data-testid="2fa-code-input"]').fill('123456');
      await page.locator('[data-testid="confirm-disable-2fa"]').click();

      await privacyPage.verifyPrivacySettings({
        twoFactorEnabled: false
      });
    });

    test('should show backup codes management', async () => {
      // Enable 2FA first
      await apiContext.post(`${process.env.E2E_IDENTITY_API_URL}/api/v1/auth/2fa/enable`, {
        headers: { 'Authorization': `Bearer ${testUser.accessToken}` },
        data: { code: '123456' }
      });

      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('security');

      // Access backup codes management
      await page.locator('[data-testid="manage-backup-codes"]').click();

      await expect(page.locator('[data-testid="backup-codes-modal"]')).toBeVisible();
      await expect(page.locator('[data-testid="remaining-codes-count"]')).toBeVisible();
      
      // Generate new backup codes
      await page.locator('[data-testid="generate-new-codes"]').click();
      await expect(page.locator('[data-testid="new-backup-codes"]')).toBeVisible();
    });
  });

  test.describe('Session Timeout Management', () => {
    test('should update session timeout', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('security');

      // Set custom session timeout (30 minutes)
      await privacyPage.sessionTimeoutInput.clear();
      await privacyPage.sessionTimeoutInput.fill('1800');
      await privacyPage.savePrivacyButton.click();
      await privacyPage.waitForSuccess();

      // Verify timeout was set
      await expect(privacyPage.sessionTimeoutInput).toHaveValue('1800');
    });

    test('should enforce session timeout', async () => {
      // Set very short timeout for testing (5 seconds)
      await apiContext.patch(`${process.env.E2E_IDENTITY_API_URL}/api/v1/users/${testUser.id}/settings`, {
        headers: { 'Authorization': `Bearer ${testUser.accessToken}` },
        data: { sessionTimeout: 5 }
      });

      // Wait for timeout
      await page.waitForTimeout(6000);

      // Should show session timeout warning
      await expect(page.locator('[data-testid="session-timeout-warning"]')).toBeVisible();
      
      // Attempt to perform action - should redirect to login
      await page.locator('[data-testid="user-menu"]').click();
      await expect(page.locator('[data-testid="login-form"]')).toBeVisible();
    });

    test('should allow session extension', async () => {
      // Set medium timeout
      await apiContext.patch(`${process.env.E2E_IDENTITY_API_URL}/api/v1/users/${testUser.id}/settings`, {
        headers: { 'Authorization': `Bearer ${testUser.accessToken}` },
        data: { sessionTimeout: 300 } // 5 minutes
      });

      // Wait until near timeout
      await page.waitForTimeout(240000); // 4 minutes

      // Should show extension option
      await expect(page.locator('[data-testid="extend-session-prompt"]')).toBeVisible();
      await page.locator('[data-testid="extend-session-button"]').click();

      // Should remain logged in
      await authHelper.verifyAuthenticated();
    });
  });

  test.describe('GDPR Compliance and User Rights', () => {
    test('should display privacy policy and user rights', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Verify GDPR information is displayed
      await expect(page.locator('[data-testid="gdpr-rights-info"]')).toBeVisible();
      await expect(page.locator('[data-testid="privacy-policy-link"]')).toBeVisible();
      
      // Check specific rights are mentioned
      await expect(page.locator('[data-testid="right-to-access"]')).toBeVisible();
      await expect(page.locator('[data-testid="right-to-rectification"]')).toBeVisible();
      await expect(page.locator('[data-testid="right-to-erasure"]')).toBeVisible();
      await expect(page.locator('[data-testid="right-to-portability"]')).toBeVisible();
    });

    test('should handle data subject access request', async () => {
      await privacyPage.requestDataExport('JSON');

      // Verify export request was created
      await expect(page.locator('[data-testid="export-request-confirmation"]')).toBeVisible();
      await expect(page.locator('[data-testid="export-status"]')).toContainText('PENDING');
      
      // Should receive email notification
      await expect(page.locator('[data-testid="email-notification-sent"]')).toBeVisible();
    });

    test('should provide data in machine-readable format', async () => {
      const exportRequestId = await privacyHelper.requestDataExport('JSON', testUser.accessToken!);

      // Simulate export completion
      await apiContext.patch(`${process.env.E2E_IDENTITY_API_URL}/api/v1/privacy/data-export/${exportRequestId}`, {
        headers: { 'Authorization': `Bearer ${testUser.accessToken}` },
        data: { status: 'COMPLETED', downloadUrl: '/exports/test-export.json' }
      });

      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Verify download link is available
      await expect(page.locator('[data-testid="download-export-link"]')).toBeVisible();
      
      // Download and verify JSON structure
      const downloadPromise = page.waitForDownload();
      await page.locator('[data-testid="download-export-link"]').click();
      const download = await downloadPromise;
      
      expect(download.suggestedFilename()).toMatch(/\.json$/);
    });

    test('should handle right to erasure (account deletion)', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Initiate account deletion
      await privacyPage.deleteAccountButton.click();

      // Should show detailed deletion information
      await expect(page.locator('[data-testid="deletion-info-modal"]')).toBeVisible();
      await expect(page.locator('[data-testid="deletion-consequences"]')).toBeVisible();
      await expect(page.locator('[data-testid="data-retention-policy"]')).toBeVisible();

      // Require explicit confirmation
      await privacyPage.confirmDeleteInput.fill('DELETE MY ACCOUNT');
      await privacyPage.confirmDeleteButton.click();

      // Should show final confirmation
      await expect(page.locator('[data-testid="final-deletion-confirmation"]')).toBeVisible();
    });

    test('should maintain audit trail for privacy actions', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Perform various privacy actions
      await privacyPage.updateProfileVisibility('PRIVATE');
      await privacyPage.requestDataExport('JSON');
      
      // View audit trail
      await page.locator('[data-testid="view-privacy-audit"]').click();

      await expect(page.locator('[data-testid="privacy-audit-log"]')).toBeVisible();
      await expect(page.locator('[data-testid="audit-entry"]')).toHaveCount(2);
      
      // Verify audit entries contain required information
      const auditEntries = await page.locator('[data-testid="audit-entry"]').all();
      for (const entry of auditEntries) {
        await expect(entry.locator('[data-testid="audit-timestamp"]')).toBeVisible();
        await expect(entry.locator('[data-testid="audit-action"]')).toBeVisible();
        await expect(entry.locator('[data-testid="audit-details"]')).toBeVisible();
      }
    });
  });

  test.describe('Data Consent Management', () => {
    test('should track and manage consent preferences', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // Access consent management
      await page.locator('[data-testid="manage-consent"]').click();

      await expect(page.locator('[data-testid="consent-management-modal"]')).toBeVisible();
      
      // Verify different consent categories
      await expect(page.locator('[data-testid="essential-consent"]')).toBeVisible();
      await expect(page.locator('[data-testid="analytics-consent"]')).toBeVisible();
      await expect(page.locator('[data-testid="marketing-consent"]')).toBeVisible();
      await expect(page.locator('[data-testid="third-party-consent"]')).toBeVisible();

      // Essential consent should be required and non-toggleable
      await expect(page.locator('[data-testid="essential-consent"] input')).toBeDisabled();
      await expect(page.locator('[data-testid="essential-consent"] input')).toBeChecked();
    });

    test('should update consent preferences', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');
      await page.locator('[data-testid="manage-consent"]').click();

      // Update consent preferences
      await page.locator('[data-testid="analytics-consent"] input').uncheck();
      await page.locator('[data-testid="marketing-consent"] input').check();
      await page.locator('[data-testid="third-party-consent"] input').uncheck();

      await page.locator('[data-testid="save-consent-preferences"]').click();
      await expect(page.locator('[data-testid="consent-updated"]')).toBeVisible();

      // Verify preferences were saved
      await page.locator('[data-testid="manage-consent"]').click();
      await expect(page.locator('[data-testid="analytics-consent"] input')).not.toBeChecked();
      await expect(page.locator('[data-testid="marketing-consent"] input')).toBeChecked();
      await expect(page.locator('[data-testid="third-party-consent"] input')).not.toBeChecked();
    });

    test('should show consent withdrawal effects', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');
      await page.locator('[data-testid="manage-consent"]').click();

      // Hover over consent toggle to see effects
      await page.locator('[data-testid="analytics-consent"] input').hover();
      await expect(page.locator('[data-testid="consent-effects-tooltip"]')).toBeVisible();
      await expect(page.locator('[data-testid="consent-effects-tooltip"]')).toContainText('productivity insights');

      // Withdraw analytics consent
      await page.locator('[data-testid="analytics-consent"] input').uncheck();
      
      // Should show immediate effect warning
      await expect(page.locator('[data-testid="analytics-warning"]')).toBeVisible();
      await expect(page.locator('[data-testid="analytics-warning"]')).toContainText('analytics features will be disabled');
    });

    test('should maintain consent history', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // View consent history
      await page.locator('[data-testid="consent-history"]').click();

      await expect(page.locator('[data-testid="consent-history-modal"]')).toBeVisible();
      await expect(page.locator('[data-testid="consent-record"]')).toHaveCount(1); // Initial consent

      // Verify consent record contains required information
      const consentRecord = page.locator('[data-testid="consent-record"]').first();
      await expect(consentRecord.locator('[data-testid="consent-timestamp"]')).toBeVisible();
      await expect(consentRecord.locator('[data-testid="consent-version"]')).toBeVisible();
      await expect(consentRecord.locator('[data-testid="consent-preferences"]')).toBeVisible();
    });
  });

  test.describe('Performance and Security', () => {
    test('should load privacy settings quickly', async () => {
      const loadTime = await perfHelper.measurePageLoadTime('/profile/privacy');
      
      perfHelper.verifyPerformanceThreshold(loadTime, 'MEDIUM');
      expect(loadTime).toBeLessThan(PERFORMANCE_THRESHOLDS.UI_RESPONSE_TIME.DATA_LOAD);
    });

    test('should save privacy settings quickly', async () => {
      await privacyPage.goToPrivacySettings();

      const saveTime = await perfHelper.measureAPIResponseTime(async () => {
        await privacyPage.updateProfileVisibility('PUBLIC');
      });

      perfHelper.verifyPerformanceThreshold(saveTime, 'FAST');
    });

    test('should validate CSRF protection on privacy endpoints', async () => {
      await securityHelper.testCSRFProtection('/api/v1/privacy/preferences', testUser.accessToken!);
    });

    test('should encrypt sensitive privacy data', async () => {
      // Verify that privacy settings are encrypted in transit
      const privacyResponse = await apiContext.get(`${process.env.E2E_IDENTITY_API_URL}/api/v1/privacy/preferences`, {
        headers: { 'Authorization': `Bearer ${testUser.accessToken}` }
      });

      expect(privacyResponse.ok()).toBe(true);
      
      // Response should be over HTTPS and contain encrypted data
      const responseData = await privacyResponse.json();
      expect(responseData.encrypted).toBe(true);
    });

    test('should rate limit privacy-sensitive operations', async () => {
      await securityHelper.testRateLimit('/api/v1/privacy/data-export', 3);
    });
  });

  test.describe('Accessibility Compliance', () => {
    test('should meet WCAG 2.1 AA standards on privacy settings page', async () => {
      await privacyPage.goToPrivacySettings();
      await a11yHelper.runAccessibilityAudit();
    });

    test('should support keyboard navigation in privacy controls', async () => {
      await privacyPage.goToPrivacySettings();
      
      await a11yHelper.testKeyboardNavigation(
        '[data-testid="profile-visibility-select"]',
        '[data-testid="save-privacy-button"]'
      );
    });

    test('should have proper ARIA labels for privacy controls', async () => {
      await privacyPage.goToPrivacySettings();

      // Verify ARIA labels on important controls
      await expect(privacyPage.profileVisibilitySelect).toHaveAttribute('aria-label');
      await expect(privacyPage.showOnlineStatusToggle).toHaveAttribute('aria-label');
      await expect(privacyPage.shareActivityDataToggle).toHaveAttribute('aria-label');
      await expect(privacyPage.twoFactorToggle).toHaveAttribute('aria-label');
    });

    test('should provide clear descriptions for privacy implications', async () => {
      await privacyPage.goToPrivacySettings();

      // Each privacy control should have explanatory text
      await expect(page.locator('[data-testid="profile-visibility-help"]')).toBeVisible();
      await expect(page.locator('[data-testid="online-status-help"]')).toBeVisible();
      await expect(page.locator('[data-testid="activity-data-help"]')).toBeVisible();
      
      // Help text should be descriptive and not just generic
      const helpTexts = await Promise.all([
        page.locator('[data-testid="profile-visibility-help"]').textContent(),
        page.locator('[data-testid="online-status-help"]').textContent(),
        page.locator('[data-testid="activity-data-help"]').textContent()
      ]);

      helpTexts.forEach(text => {
        expect(text).toBeTruthy();
        expect(text!.length).toBeGreaterThan(20); // Ensure meaningful descriptions
      });
    });
  });

  test.describe('Cross-Persona Privacy Management', () => {
    let workPersona: TestPersona;
    let personalPersona: TestPersona;

    test.beforeEach(async () => {
      workPersona = IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Work Privacy',
        privacySettings: IdentityTestDataFactory.createPrivacySettings({
          profileVisibility: 'PUBLIC',
          shareActivityData: true
        })
      });

      personalPersona = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Personal Privacy',
        privacySettings: IdentityTestDataFactory.createPrivacySettings({
          profileVisibility: 'PRIVATE',
          shareActivityData: false
        })
      });

      await dataManager.createPersonas(testUser.id!, [workPersona, personalPersona], testUser.accessToken!);
    });

    test('should maintain separate privacy settings per persona', async () => {
      // Verify work persona settings
      await personaPage.switchPersona(workPersona.name);
      await personaPage.verifyPersonaPrivacySettings(workPersona.name, {
        profileVisibility: 'PUBLIC',
        shareActivityData: true
      });

      // Verify personal persona settings
      await personaPage.switchPersona(personalPersona.name);
      await personaPage.verifyPersonaPrivacySettings(personalPersona.name, {
        profileVisibility: 'PRIVATE',
        shareActivityData: false
      });
    });

    test('should apply global privacy changes to all personas', async () => {
      // Set global restriction
      await privacyPage.updateMessagePermissions('NOBODY');

      // Verify both personas inherit the restriction
      await personaPage.verifyPersonaPrivacySettings(workPersona.name, {
        allowMessagesFrom: 'NOBODY'
      });
      await personaPage.verifyPersonaPrivacySettings(personalPersona.name, {
        allowMessagesFrom: 'NOBODY'
      });
    });

    test('should show privacy summary across all personas', async () => {
      await privacyPage.goToPrivacySettings();
      await privacyPage.switchToTab('data');

      // View cross-persona privacy summary
      await page.locator('[data-testid="privacy-summary"]').click();

      await expect(page.locator('[data-testid="privacy-summary-modal"]')).toBeVisible();
      
      // Should show settings for each persona
      await expect(page.locator(`[data-testid="persona-privacy-${workPersona.name}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="persona-privacy-${personalPersona.name}"]`)).toBeVisible();

      // Should highlight any inconsistencies
      await expect(page.locator('[data-testid="privacy-inconsistency-warning"]')).toBeVisible();
    });
  });
});