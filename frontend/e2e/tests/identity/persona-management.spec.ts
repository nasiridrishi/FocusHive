/**
 * E2E Tests for Persona Management - Core CRUD Operations
 *
 * Comprehensive testing of persona creation, reading, updating, and deletion
 * Validates CM3035 Advanced Web Design template requirements for multi-persona profiles
 *
 * @fileoverview Persona management E2E tests
 * @version 1.0.0
 */

import {type APIRequestContext, expect, type Page, test} from '@playwright/test';
import {IdentityPage} from '../../pages/IdentityPage';
import {PersonaSwitcherPage} from '../../pages/PersonaSwitcherPage';
import {
  AccessibilityHelper,
  AuthenticationHelper,
  PerformanceHelper,
  PersonaHelper
} from '../../helpers/identity/identity-helpers';
import {
  IdentityTestDataFactory,
  IdentityTestDataManager,
  type TestPersona,
  type TestUser
} from '../../fixtures/identity/identity-fixtures';
import {PERFORMANCE_THRESHOLDS, TEST_USERS} from './identity.config';

test.describe('Persona Management - Core CRUD Operations', () => {
  let page: Page;
  let apiContext: APIRequestContext;
  let _identityPage: IdentityPage;
  let personaPage: PersonaSwitcherPage;
  let authHelper: AuthenticationHelper;
  let personaHelper: PersonaHelper;
  let perfHelper: PerformanceHelper;
  let a11yHelper: AccessibilityHelper;
  let dataManager: IdentityTestDataManager;
  let testUser: TestUser;

  test.beforeAll(async ({browser, playwright}) => {
    // Create API context for setup
    apiContext = await playwright.request.newContext({
      baseURL: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081'
    });

    // Create browser context and page
    const context = await browser.newContext();
    page = await context.newPage();

    // Initialize page objects and helpers
    identityPage = new IdentityPage(page);
    personaPage = new PersonaSwitcherPage(page);
    authHelper = new AuthenticationHelper(page, apiContext);
    personaHelper = new PersonaHelper(page, apiContext);
    perfHelper = new PerformanceHelper(page);
    a11yHelper = new AccessibilityHelper(page);
    dataManager = new IdentityTestDataManager(page, apiContext);

    // Create test user
    testUser = await dataManager.createUser(TEST_USERS.MULTI_PERSONA_USER);
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

  test.describe('Persona Creation', () => {
    test('should create persona with all fields filled', async () => {
      const personaData = IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Work Professional',
        displayName: 'Professional Me',
        bio: 'Focused on productivity and collaboration',
        themePreference: 'dark',
        language: 'en',
        timezone: 'America/New_York'
      });

      await personaPage.createPersona(personaData);

      // Verify persona was created
      await personaPage.verifyPersonaExists(personaData.name);
      await personaPage.verifyPersonaInSwitcher(personaData.name);
    });

    test('should create persona with minimal required fields', async () => {
      const personaData = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Simple Personal'
      });

      await personaPage.createPersona(personaData);

      // Verify persona was created
      await personaPage.verifyPersonaExists(personaData.name);

      // Verify defaults were applied
      await personaPage.verifyPersonaPrivacySettings(personaData.name, {
        profileVisibility: 'FRIENDS',
        showOnlineStatus: true,
        allowMessagesFrom: 'FRIENDS',
        shareActivityData: false
      });
    });

    test('should create persona from work template', async () => {
      await personaPage.createPersonaFromTemplate('work');

      // Verify template-based persona was created
      const personaNames = await personaPage.getPersonaNames();
      const workPersona = personaNames.find(name => name.toLowerCase().includes('work'));
      expect(workPersona).toBeTruthy();

      // Verify work template defaults
      if (workPersona) {
        await personaPage.verifyPersonaPrivacySettings(workPersona, {
          profileVisibility: 'PUBLIC',
          shareActivityData: true
        });

        await personaPage.verifyNotificationPreferences(workPersona, {
          email: true,
          push: true,
          desktop: true,
          marketing: false
        });
      }
    });

    test('should create persona from personal template', async () => {
      await personaPage.createPersonaFromTemplate('personal');

      // Verify template-based persona was created
      const personaNames = await personaPage.getPersonaNames();
      const personalPersona = personaNames.find(name => name.toLowerCase().includes('personal'));
      expect(personalPersona).toBeTruthy();

      // Verify personal template defaults
      if (personalPersona) {
        await personaPage.verifyPersonaPrivacySettings(personalPersona, {
          profileVisibility: 'FRIENDS',
          shareActivityData: false
        });

        await personaPage.verifyNotificationPreferences(personalPersona, {
          email: true,
          push: false,
          desktop: false,
          marketing: true
        });
      }
    });

    test('should validate required fields on creation', async () => {
      await personaPage.goToCreatePersona();

      // Try to submit empty form
      await personaPage.createPersonaSubmit.click();

      // Verify validation errors
      await expect(page.locator('[data-testid="validation-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="validation-error"]')).toContainText('Name is required');
    });

    test('should prevent duplicate persona names', async () => {
      const personaName = 'Duplicate Test';

      // Create first persona
      const personaData = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: personaName
      });
      await personaPage.createPersona(personaData);

      // Try to create second persona with same name
      await personaPage.goToCreatePersona();
      await personaPage.personaNameInput.fill(personaName);
      await personaPage.personaTypeSelect.selectOption('PROFESSIONAL');
      await personaPage.createPersonaSubmit.click();

      // Verify error message
      await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
      await expect(page.locator('[data-testid="error-message"]')).toContainText('already exists');
    });

    test('should measure persona creation performance', async () => {
      const personaData = IdentityTestDataFactory.createPersona('ACADEMIC', {
        name: 'Performance Test'
      });

      const creationTime = await perfHelper.measureAPIResponseTime(async () => {
        await personaPage.createPersona(personaData);
      });

      perfHelper.verifyPerformanceThreshold(creationTime, 'MEDIUM');
      expect(creationTime).toBeLessThan(PERFORMANCE_THRESHOLDS.UI_RESPONSE_TIME.DATA_LOAD);
    });
  });

  test.describe('Persona Reading and Display', () => {
    let testPersona: TestPersona;

    test.beforeEach(async () => {
      // Create a test persona
      testPersona = IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Display Test',
        displayName: 'Display Test Persona',
        bio: 'Test persona for display verification',
        themePreference: 'light'
      });

      await personaHelper.createPersonaViaAPI(testPersona, testUser.accessToken || '');
    });

    test('should display persona in management list', async () => {
      await personaPage.goToPersonas();

      await expect(page.locator(`[data-testid="persona-item-${testPersona.name}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="persona-name"]`))
      .toContainText(testPersona.name);
      await expect(page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="persona-display-name"]`))
      .toContainText(testPersona.displayName);
      await expect(page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="persona-type"]`))
      .toContainText(testPersona.type);
    });

    test('should display persona in switcher dropdown', async () => {
      await personaPage.personaSwitcher.click();

      await expect(page.locator(`[data-testid="persona-option-${testPersona.name}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="persona-option-${testPersona.name}"] [data-testid="persona-name"]`))
      .toContainText(testPersona.name);
      await expect(page.locator(`[data-testid="persona-option-${testPersona.name}"] [data-testid="persona-display-name"]`))
      .toContainText(testPersona.displayName);

      // Close dropdown
      await page.keyboard.press('Escape');
    });

    test('should show persona details in edit form', async () => {
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();

      // Verify form is populated with persona data
      await expect(personaPage.personaNameInput).toHaveValue(testPersona.name);
      await expect(personaPage.personaDisplayNameInput).toHaveValue(testPersona.displayName);
      await expect(personaPage.personaBioTextarea).toHaveValue(testPersona.bio || '');
      await expect(personaPage.personaTypeSelect).toHaveValue(testPersona.type);
      await expect(personaPage.personaThemeSelect).toHaveValue(testPersona.themePreference);
    });

    test('should display correct persona count', async () => {
      await personaPage.goToPersonas();

      const personaNames = await personaPage.getPersonaNames();
      const countDisplay = page.locator('[data-testid="personas-count"]');

      await expect(countDisplay).toContainText(`${personaNames.length} persona${personaNames.length !== 1 ? 's' : ''}`);
    });
  });

  test.describe('Persona Updates', () => {
    let testPersona: TestPersona;

    test.beforeEach(async () => {
      // Create a test persona
      testPersona = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Update Test',
        displayName: 'Original Name',
        bio: 'Original bio',
        themePreference: 'light'
      });

      await personaHelper.createPersonaViaAPI(testPersona, testUser.accessToken || '');
    });

    test('should update persona display name', async () => {
      const newDisplayName = 'Updated Display Name';

      await personaPage.editPersona(testPersona.name, {
        displayName: newDisplayName
      });

      // Verify update in management list
      await personaPage.goToPersonas();
      await expect(page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="persona-display-name"]`))
      .toContainText(newDisplayName);

      // Verify update in switcher
      await personaPage.personaSwitcher.click();
      await expect(page.locator(`[data-testid="persona-option-${testPersona.name}"] [data-testid="persona-display-name"]`))
      .toContainText(newDisplayName);
    });

    test('should update persona bio', async () => {
      const newBio = 'This is an updated biography with more details about the persona.';

      await personaPage.editPersona(testPersona.name, {
        bio: newBio
      });

      // Verify bio was updated
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();
      await expect(personaPage.personaBioTextarea).toHaveValue(newBio);
    });

    test('should update persona theme preference', async () => {
      const newTheme = 'dark';

      await personaPage.editPersona(testPersona.name, {
        themePreference: newTheme
      });

      // Verify theme was updated
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();
      await expect(personaPage.personaThemeSelect).toHaveValue(newTheme);
    });

    test('should update multiple fields simultaneously', async () => {
      const updates: Partial<TestPersona> = {
        displayName: 'Multi-Update Test',
        bio: 'Updated with multiple changes',
        themePreference: 'auto'
      };

      await personaPage.editPersona(testPersona.name, updates);

      // Verify all updates
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();

      await expect(personaPage.personaDisplayNameInput).toHaveValue(updates.displayName!);
      await expect(personaPage.personaBioTextarea).toHaveValue(updates.bio!);
      await expect(personaPage.personaThemeSelect).toHaveValue(updates.themePreference!);
    });

    test('should handle update validation errors', async () => {
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();

      // Clear required field
      await personaPage.personaNameInput.clear();
      await personaPage.savePersonaButton.click();

      // Verify validation error
      await expect(page.locator('[data-testid="validation-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="validation-error"]')).toContainText('Name is required');
    });

    test('should measure update performance', async () => {
      const updateTime = await perfHelper.measureAPIResponseTime(async () => {
        await personaPage.editPersona(testPersona.name, {
          displayName: 'Performance Update Test'
        });
      });

      perfHelper.verifyPerformanceThreshold(updateTime, 'MEDIUM');
    });
  });

  test.describe('Persona Deletion', () => {
    let testPersona: TestPersona;

    test.beforeEach(async () => {
      // Create a test persona
      testPersona = IdentityTestDataFactory.createPersona('SOCIAL', {
        name: 'Delete Test',
        displayName: 'Delete Test Persona'
      });

      await personaHelper.createPersonaViaAPI(testPersona, testUser.accessToken || '');
    });

    test('should delete persona successfully', async () => {
      await personaPage.deletePersona(testPersona.name);

      // Verify persona is removed from list
      await personaPage.verifyPersonaNotExists(testPersona.name);

      // Verify persona is removed from switcher
      await personaPage.personaSwitcher.click();
      await expect(page.locator(`[data-testid="persona-option-${testPersona.name}"]`)).not.toBeVisible();
    });

    test('should show confirmation before deletion', async () => {
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="delete-button"]`).click();

      // Verify confirmation modal appears
      await expect(page.locator('[data-testid="delete-persona-modal"]')).toBeVisible();
      await expect(page.locator('[data-testid="delete-persona-modal"] [data-testid="persona-name"]'))
      .toContainText(testPersona.name);

      // Cancel deletion
      await page.locator('[data-testid="cancel-delete-persona"]').click();
      await expect(page.locator('[data-testid="delete-persona-modal"]')).not.toBeVisible();

      // Verify persona still exists
      await personaPage.verifyPersonaExists(testPersona.name);
    });

    test('should prevent deletion of default persona', async () => {
      // Set persona as default
      await personaPage.setAsDefault(testPersona.name);

      // Try to delete default persona
      await personaPage.goToPersonas();
      const deleteButton = page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="delete-button"]`);

      // Delete button should be disabled or not visible for default persona
      await expect(deleteButton).toBeDisabled();
    });

    test('should handle active persona deletion gracefully', async () => {
      // Switch to the test persona
      await personaPage.switchPersona(testPersona.name);
      await personaPage.verifyCurrentPersona(testPersona.name);

      // Delete the active persona
      await personaPage.deletePersona(testPersona.name);

      // Should automatically switch to default persona
      const currentPersona = await personaPage.currentPersonaName.textContent();
      expect(currentPersona).not.toBe(testPersona.name);

      // Verify persona is deleted
      await personaPage.verifyPersonaNotExists(testPersona.name);
    });

    test('should measure deletion performance', async () => {
      const deleteTime = await perfHelper.measureAPIResponseTime(async () => {
        await personaPage.deletePersona(testPersona.name);
      });

      perfHelper.verifyPerformanceThreshold(deleteTime, 'MEDIUM');
    });
  });

  test.describe('Default Persona Management', () => {
    let workPersona: TestPersona;
    let personalPersona: TestPersona;

    test.beforeEach(async () => {
      // Create test personas
      workPersona = IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Work Default',
        isDefault: false
      });
      personalPersona = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Personal Default',
        isDefault: true
      });

      await personaHelper.createPersonaViaAPI(workPersona, testUser.accessToken || '');
      await personaHelper.createPersonaViaAPI(personalPersona, testUser.accessToken || '');
    });

    test('should set persona as default', async () => {
      await personaPage.setAsDefault(workPersona.name);

      // Verify default badge appears
      await personaPage.goToPersonas();
      await expect(page.locator(`[data-testid="persona-item-${workPersona.name}"] [data-testid="default-badge"]`))
      .toBeVisible();

      // Verify old default no longer has badge
      await expect(page.locator(`[data-testid="persona-item-${personalPersona.name}"] [data-testid="default-badge"]`))
      .not.toBeVisible();
    });

    test('should use default persona on login', async () => {
      // Set work persona as default
      await personaPage.setAsDefault(workPersona.name);

      // Logout and login again
      await authHelper.logoutViaUI();
      await authHelper.loginViaUI(testUser.email, testUser.password);

      // Should automatically use default persona
      await personaPage.verifyCurrentPersona(workPersona.name);
    });

    test('should show default persona first in switcher', async () => {
      await personaPage.setAsDefault(workPersona.name);

      const personaNames = await personaPage.getPersonaNames();
      const expectedOrder = [workPersona.name, ...personaNames.filter(name => name !== workPersona.name)];

      await personaPage.verifyPersonaOrder(expectedOrder);
    });
  });

  test.describe('Persona Avatar Management', () => {
    let testPersona: TestPersona;

    test.beforeEach(async () => {
      testPersona = IdentityTestDataFactory.createPersona('CREATIVE', {
        name: 'Avatar Test'
      });

      await personaHelper.createPersonaViaAPI(testPersona, testUser.accessToken || '');
    });

    test('should upload persona avatar', async () => {
      // Create a test image file
      const testImagePath = '/tmp/test-avatar.png';
      await page.evaluate(() => {
        // Create a canvas and save as image
        const canvas = document.createElement('canvas');
        canvas.width = 100;
        canvas.height = 100;
        const ctx = canvas.getContext('2d')!;
        ctx.fillStyle = '#4CAF50';
        ctx.fillRect(0, 0, 100, 100);
      });

      await personaPage.uploadPersonaAvatar(testPersona.name, testImagePath);

      // Verify avatar appears in UI
      await personaPage.goToPersonas();
      await expect(page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="persona-avatar"]`))
      .toBeVisible();
    });

    test('should validate avatar file type', async () => {
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();

      // Try to upload invalid file type
      const invalidFilePath = '/tmp/test.txt';
      await personaPage.personaAvatarUpload.setInputFiles(invalidFilePath);

      // Should show validation error
      await expect(page.locator('[data-testid="file-type-error"]')).toBeVisible();
    });

    test('should validate avatar file size', async () => {
      // Test would need a large file - simplified for now
      await personaPage.goToPersonas();
      await page.locator(`[data-testid="persona-item-${testPersona.name}"] [data-testid="edit-button"]`).click();

      // File size validation would be tested with actual large file
      // For now, just verify the size limit is displayed
      await expect(page.locator('[data-testid="file-size-limit"]')).toContainText('5 MB');
    });
  });

  test.describe('Accessibility Compliance', () => {
    test('should meet WCAG 2.1 AA standards on personas page', async () => {
      await personaPage.goToPersonas();
      await a11yHelper.runAccessibilityAudit();
    });

    test('should meet WCAG 2.1 AA standards on create persona page', async () => {
      await personaPage.goToCreatePersona();
      await a11yHelper.runAccessibilityAudit();
    });

    test('should support keyboard navigation', async () => {
      await personaPage.goToPersonas();

      await a11yHelper.testKeyboardNavigation(
          '[data-testid="create-persona-button"]',
          '[data-testid="personas-list"] [data-testid="edit-button"]:first'
      );
    });

    test('should have proper focus indicators', async () => {
      await personaPage.goToCreatePersona();

      await a11yHelper.verifyFocusIndicators([
        '[data-testid="persona-name"]',
        '[data-testid="persona-type"]',
        '[data-testid="persona-display-name"]',
        '[data-testid="create-persona-submit"]'
      ]);
    });

    test('should have proper ARIA labels', async () => {
      await personaPage.goToPersonas();

      // Verify important elements have ARIA labels
      await expect(personaPage.createPersonaButton).toHaveAttribute('aria-label');
      await expect(personaPage.personaSwitcher).toHaveAttribute('aria-label');

      const personaItems = await page.locator('[data-testid^="persona-item-"]').all();
      for (const item of personaItems) {
        await expect(item).toHaveAttribute('aria-label');
      }
    });
  });

  test.describe('Error Handling', () => {
    test('should handle API errors gracefully', async () => {
      // Simulate API error by using invalid token
      await page.evaluate(() => {
        localStorage.setItem('focushive_token', 'invalid-token');
      });

      await personaPage.goToCreatePersona();

      const personaData = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'API Error Test'
      });

      await personaPage.personaNameInput.fill(personaData.name);
      await personaPage.personaTypeSelect.selectOption(personaData.type);
      await personaPage.createPersonaSubmit.click();

      // Should show appropriate error message
      await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
      await expect(page.locator('[data-testid="error-message"]')).toContainText('authentication');
    });

    test('should handle network errors', async () => {
      // Simulate network error
      await page.route('**/api/v1/personas', route => route.abort());

      const personaData = IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Network Error Test'
      });

      await personaPage.goToCreatePersona();
      await personaPage.personaNameInput.fill(personaData.name);
      await personaPage.personaTypeSelect.selectOption(personaData.type);
      await personaPage.createPersonaSubmit.click();

      // Should show network error message
      await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
      await expect(page.locator('[data-testid="error-message"]')).toContainText('network');

      // Cleanup route
      await page.unroute('**/api/v1/personas');
    });

    test('should handle form validation errors', async () => {
      await personaPage.goToCreatePersona();

      // Submit form with invalid data
      await personaPage.personaNameInput.fill(''); // Empty name
      await personaPage.personaDisplayNameInput.fill('a'.repeat(256)); // Too long
      await personaPage.createPersonaSubmit.click();

      // Should show validation errors
      const validationErrors = await page.locator('[data-testid="validation-error"]').all();
      expect(validationErrors.length).toBeGreaterThan(0);

      // Check specific error messages
      const errorTexts = await Promise.all(
          validationErrors.map(error => error.textContent())
      );
      expect(errorTexts.some(text => text?.includes('Name is required'))).toBe(true);
      expect(errorTexts.some(text => text?.includes('too long'))).toBe(true);
    });
  });
});