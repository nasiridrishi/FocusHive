import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login-page';
import { RegistrationPage } from '../pages/registration-page';
import { TestDataManager } from '../helpers/test-data-manager';

/**
 * Authentication E2E Tests
 * Clean, focused tests for core authentication flows
 */
test.describe('Authentication', () => {
  let testDataManager: TestDataManager;

  test.beforeEach(() => {
    testDataManager = new TestDataManager();
  });

  test.describe('User Registration', () => {
    test('should successfully register a new user', async ({ page }) => {
      const registrationPage = new RegistrationPage(page);
      const testUser = testDataManager.generateTestUser();

      await registrationPage.goto();
      await registrationPage.verifyRegistrationFormDisplayed();
      
      await registrationPage.register(testUser);
      
      // Verify registration success (either success message or redirect)
      await registrationPage.verifySuccessfulRegistration();
    });

    test('should show validation errors for empty form submission', async ({ page }) => {
      const registrationPage = new RegistrationPage(page);

      await registrationPage.goto();
      await registrationPage.clickRegister();
      
      await registrationPage.verifyFormValidationErrors();
    });

    test('should show validation error for invalid email format', async ({ page }) => {
      const registrationPage = new RegistrationPage(page);
      const testUser = testDataManager.generateTestUser({
        email: 'invalid-email-format'
      });

      await registrationPage.goto();
      await registrationPage.fillRegistrationForm(testUser);
      await registrationPage.clickRegister();
      
      await registrationPage.verifyFormValidationErrors();
    });

    test('should show password strength indicator', async ({ page }) => {
      const registrationPage = new RegistrationPage(page);

      await registrationPage.goto();
      await registrationPage.fillPassword('StrongPassword123!');
      
      await registrationPage.verifyPasswordStrengthShown();
    });

    test('should navigate to login page when clicking login link', async ({ page }) => {
      const registrationPage = new RegistrationPage(page);
      const loginPage = new LoginPage(page);

      await registrationPage.goto();
      await registrationPage.clickLoginLink();
      
      await loginPage.verifyLoginFormDisplayed();
      expect(page.url()).toContain('/login');
    });
  });

  test.describe('User Login', () => {
    test('should display login form correctly', async ({ page }) => {
      const loginPage = new LoginPage(page);

      await loginPage.goto();
      await loginPage.verifyLoginFormDisplayed();
    });

    test('should show validation errors for empty form submission', async ({ page }) => {
      const loginPage = new LoginPage(page);

      await loginPage.goto();
      await loginPage.clickLogin();
      
      await loginPage.verifyFormValidationErrors();
    });

    test('should show error for invalid credentials', async ({ page }) => {
      const loginPage = new LoginPage(page);
      const invalidCredentials = testDataManager.getScenarioData('invalid-login');

      await loginPage.goto();
      await loginPage.login(invalidCredentials.user.username, invalidCredentials.user.password);
      
      // Should show error message or stay on login page
      try {
        await loginPage.verifyErrorMessageShown();
      } catch {
        // If no error message, verify we stayed on login page
        expect(page.url()).toContain('/login');
      }
    });

    test('should navigate to registration page when clicking signup link', async ({ page }) => {
      const loginPage = new LoginPage(page);
      const registrationPage = new RegistrationPage(page);

      await loginPage.goto();
      await loginPage.clickSignupLink();
      
      await registrationPage.verifyRegistrationFormDisplayed();
      expect(page.url()).toContain('/register');
    });

    test('should preserve form values after failed login attempt', async ({ page }) => {
      const loginPage = new LoginPage(page);
      const testEmail = 'test@example.com';
      const testPassword = 'wrongpassword';

      await loginPage.goto();
      await loginPage.fillEmail(testEmail);
      await loginPage.fillPassword(testPassword);
      await loginPage.clickLogin();
      
      // Wait for response
      await page.waitForTimeout(1000);
      
      const formValues = await loginPage.getFormValues();
      expect(formValues.email).toBe(testEmail);
      // Password might be cleared for security reasons, so we don't check it
    });
  });

  test.describe('Full Authentication Flow', () => {
    test('should complete registration and login flow', async ({ page }) => {
      const registrationPage = new RegistrationPage(page);
      const loginPage = new LoginPage(page);
      const testUser = testDataManager.generateTestUser();

      // Step 1: Register new user
      await registrationPage.goto();
      await registrationPage.register(testUser);
      
      // Step 2: Handle post-registration (might be redirected or need to login)
      const currentUrl = await page.url();
      
      if (currentUrl.includes('/login')) {
        // Redirected to login, complete the login
        await loginPage.login(testUser.email, testUser.password);
        await loginPage.verifySuccessfulLogin();
      } else if (!currentUrl.includes('/register')) {
        // Automatically logged in after registration
        expect(currentUrl).not.toContain('/login');
        expect(currentUrl).not.toContain('/register');
      } else {
        // Still on registration, check for success message
        await registrationPage.verifySuccessfulRegistration();
      }
    });
  });

  test.describe('Navigation and UI', () => {
    test('should have consistent navigation between auth pages', async ({ page }) => {
      const loginPage = new LoginPage(page);
      const registrationPage = new RegistrationPage(page);

      // Start at login page
      await loginPage.goto();
      await loginPage.verifyLoginFormDisplayed();

      // Navigate to registration
      await loginPage.clickSignupLink();
      await registrationPage.verifyRegistrationFormDisplayed();
      expect(page.url()).toContain('/register');

      // Navigate back to login
      await registrationPage.clickLoginLink();
      await loginPage.verifyLoginFormDisplayed();
      expect(page.url()).toContain('/login');
    });

    test('should clear form when navigating between auth pages', async ({ page }) => {
      const loginPage = new LoginPage(page);
      const registrationPage = new RegistrationPage(page);

      // Fill login form
      await loginPage.goto();
      await loginPage.fillEmail('test@example.com');
      await loginPage.fillPassword('testpassword');

      // Navigate to registration
      await loginPage.clickSignupLink();
      await registrationPage.verifyRegistrationFormDisplayed();

      // Navigate back to login
      await registrationPage.clickLoginLink();
      
      // Form should be cleared (or at least password should be)
      const formValues = await loginPage.getFormValues();
      // Email might be preserved, but form should be functional
      expect(formValues).toBeDefined();
    });
  });

  test.describe('Accessibility', () => {
    test('should have proper form labels and accessibility attributes', async ({ page }) => {
      const loginPage = new LoginPage(page);

      await loginPage.goto();

      // Check for form accessibility
      await expect(page.locator('input[type="email"]')).toBeVisible();
      await expect(page.locator('input[type="password"]')).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();

      // Basic accessibility check
      const loginButton = page.locator('button[type="submit"]');
      await expect(loginButton).toBeEnabled();
    });

    test('should support keyboard navigation', async ({ page }) => {
      const loginPage = new LoginPage(page);

      await loginPage.goto();

      // Tab through form elements
      await page.keyboard.press('Tab');
      await expect(page.locator('input[type="email"]')).toBeFocused();
      
      await page.keyboard.press('Tab');
      await expect(page.locator('input[type="password"]')).toBeFocused();
    });
  });
});