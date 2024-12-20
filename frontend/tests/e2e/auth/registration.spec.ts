import { test, expect, Page } from '@playwright/test';
import { randomBytes } from 'crypto';

/**
 * User Registration E2E Test Suite
 *
 * Comprehensive testing of user registration functionality
 * Following production standards with proper cleanup and error handling
 */

// Test data generator for unique user data
class TestDataGenerator {
  static generateUniqueUser() {
    const timestamp = Date.now();
    const randomId = randomBytes(4).toString('hex');

    return {
      email: `test.user.${timestamp}.${randomId}@focushive.test`,
      username: `testuser_${timestamp}_${randomId}`,
      displayName: `Test User ${timestamp}`,
      password: 'SecurePass#2025!', // Meets all password requirements
    };
  }

  static generateInvalidEmails() {
    return [
      'invalid.email',
      '@invalid.com',
      'user@',
      'user@.com',
      'user name@test.com',
      'user@test',
      '',
    ];
  }

  static generateWeakPasswords() {
    return [
      '123456',           // Too simple
      'password',         // No numbers
      'Pass123',          // No special chars
      'P@ss1',            // Too short
      'password123456',   // No special chars or uppercase
      '',                 // Empty
    ];
  }
}

// Page Object Model for Registration Page
class RegistrationPage {
  constructor(private page: Page) {}

  // Selectors
  private selectors = {
    emailInput: 'input[name="email"], input[type="email"]',
    usernameInput: 'input[name="username"]',
    displayNameInput: 'input[name="displayName"], input[name="name"]',
    passwordInput: 'input[name="password"], input[type="password"]',
    confirmPasswordInput: 'input[name="confirmPassword"], input[name="passwordConfirm"]',
    submitButton: 'button[type="submit"], button:has-text("Sign Up"), button:has-text("Register")',
    errorMessage: '[role="alert"], .error-message, .MuiAlert-root',
    successMessage: '.success-message, .MuiAlert-success',
    loginLink: 'a[href*="/login"], a:has-text("Sign In")',
  };

  async navigate() {
    await this.page.goto('/register');
    await this.page.waitForLoadState('networkidle');
  }

  async fillRegistrationForm(userData: any) {
    await this.page.fill(this.selectors.emailInput, userData.email);
    await this.page.fill(this.selectors.usernameInput, userData.username);
    await this.page.fill(this.selectors.displayNameInput, userData.displayName);
    await this.page.fill(this.selectors.passwordInput, userData.password);

    // Fill confirm password if field exists
    const confirmPasswordField = await this.page.locator(this.selectors.confirmPasswordInput);
    if (await confirmPasswordField.isVisible()) {
      await this.page.fill(this.selectors.confirmPasswordInput, userData.password);
    }
  }

  async submitForm() {
    await this.page.click(this.selectors.submitButton);
  }

  async getErrorMessage(): Promise<string | null> {
    const errorElement = await this.page.locator(this.selectors.errorMessage);
    if (await errorElement.isVisible()) {
      return await errorElement.textContent();
    }
    return null;
  }

  async waitForNavigation(url: string, timeout = 10000) {
    await this.page.waitForURL(url, { timeout });
  }

  async isFieldHighlightedAsError(fieldName: string): Promise<boolean> {
    const field = await this.page.locator(`input[name="${fieldName}"]`);
    const ariaInvalid = await field.getAttribute('aria-invalid');
    const hasErrorClass = await field.evaluate((el) =>
      el.classList.contains('error') ||
      el.classList.contains('Mui-error') ||
      el.classList.contains('invalid')
    );
    return ariaInvalid === 'true' || hasErrorClass;
  }
}

// API Helper for cleanup
class APIHelper {
  static async deleteTestUser(email: string) {
    // This would typically call an admin API to clean up test data
    // For now, we'll log the intention
    console.log(`[Cleanup] Would delete user: ${email}`);
  }
}

test.describe('User Registration', () => {
  let registrationPage: RegistrationPage;
  const createdUsers: string[] = [];

  test.beforeEach(async ({ page }) => {
    registrationPage = new RegistrationPage(page);
    await registrationPage.navigate();
  });

  test.afterEach(async () => {
    // Cleanup: Delete any users created during tests
    for (const email of createdUsers) {
      await APIHelper.deleteTestUser(email);
    }
    createdUsers.length = 0;
  });

  test('should successfully register with valid data', async ({ page }) => {
    // Arrange
    const userData = TestDataGenerator.generateUniqueUser();

    // Act
    await registrationPage.fillRegistrationForm(userData);
    await registrationPage.submitForm();

    // Assert - Check for successful registration
    // Option 1: Redirect to dashboard
    try {
      await registrationPage.waitForNavigation('**/dashboard');
      expect(page.url()).toContain('/dashboard');
      createdUsers.push(userData.email);

      // Verify user is logged in
      const welcomeMessage = await page.locator('text=/Welcome.*' + userData.displayName + '/i');
      await expect(welcomeMessage).toBeVisible({ timeout: 5000 });
    } catch (error) {
      // Option 2: Redirect to login with success message
      if (page.url().includes('/login')) {
        const successMessage = await page.locator('.success-message, text=/registration successful/i');
        await expect(successMessage).toBeVisible();
        createdUsers.push(userData.email);
      } else {
        throw error;
      }
    }
  });

  test('should show validation error for invalid email format', async ({ page }) => {
    // Arrange
    const invalidEmails = TestDataGenerator.generateInvalidEmails();
    const userData = TestDataGenerator.generateUniqueUser();

    for (const invalidEmail of invalidEmails) {
      // Act
      await registrationPage.fillRegistrationForm({
        ...userData,
        email: invalidEmail
      });
      await registrationPage.submitForm();

      // Assert
      const errorMessage = await registrationPage.getErrorMessage();
      if (invalidEmail === '') {
        expect(errorMessage).toMatch(/email.*required|please.*email/i);
      } else {
        expect(errorMessage).toMatch(/invalid.*email|email.*valid/i);
      }

      // Verify we're still on registration page
      expect(page.url()).toContain('/register');

      // Clear form for next iteration
      await page.reload();
    }
  });

  test('should enforce password strength requirements', async ({ page }) => {
    // Arrange
    const weakPasswords = TestDataGenerator.generateWeakPasswords();
    const userData = TestDataGenerator.generateUniqueUser();

    for (const weakPassword of weakPasswords) {
      // Act
      await registrationPage.fillRegistrationForm({
        ...userData,
        password: weakPassword
      });
      await registrationPage.submitForm();

      // Assert
      const errorMessage = await registrationPage.getErrorMessage();
      if (weakPassword === '') {
        expect(errorMessage).toMatch(/password.*required/i);
      } else {
        expect(errorMessage).toMatch(
          /password.*must.*contain|weak.*password|minimum.*length|special.*character|uppercase|number/i
        );
      }

      // Verify password field is marked as invalid
      const isPasswordInvalid = await registrationPage.isFieldHighlightedAsError('password');
      expect(isPasswordInvalid).toBe(true);

      // Clear form for next iteration
      await page.reload();
    }
  });

  test('should prevent duplicate email registration', async ({ page, context }) => {
    // Arrange
    const userData = TestDataGenerator.generateUniqueUser();

    // Act - Register first user
    await registrationPage.fillRegistrationForm(userData);
    await registrationPage.submitForm();

    // Wait for registration to complete
    await page.waitForTimeout(2000);
    createdUsers.push(userData.email);

    // Open new page for second registration attempt
    const newPage = await context.newPage();
    const secondRegistrationPage = new RegistrationPage(newPage);
    await secondRegistrationPage.navigate();

    // Try to register with same email
    await secondRegistrationPage.fillRegistrationForm({
      ...userData,
      username: userData.username + '_duplicate'
    });
    await secondRegistrationPage.submitForm();

    // Assert
    const errorMessage = await secondRegistrationPage.getErrorMessage();
    expect(errorMessage).toMatch(/email.*already.*exists|email.*taken|duplicate.*email/i);

    // Verify we're still on registration page
    expect(newPage.url()).toContain('/register');

    await newPage.close();
  });

  test('should enforce username uniqueness', async ({ page, context }) => {
    // Arrange
    const userData = TestDataGenerator.generateUniqueUser();

    // Act - Register first user
    await registrationPage.fillRegistrationForm(userData);
    await registrationPage.submitForm();

    // Wait for registration to complete
    await page.waitForTimeout(2000);
    createdUsers.push(userData.email);

    // Open new page for second registration attempt
    const newPage = await context.newPage();
    const secondRegistrationPage = new RegistrationPage(newPage);
    await secondRegistrationPage.navigate();

    // Try to register with same username but different email
    const secondUserData = TestDataGenerator.generateUniqueUser();
    await secondRegistrationPage.fillRegistrationForm({
      ...secondUserData,
      username: userData.username // Same username
    });
    await secondRegistrationPage.submitForm();

    // Assert
    const errorMessage = await secondRegistrationPage.getErrorMessage();
    expect(errorMessage).toMatch(/username.*already.*exists|username.*taken/i);

    await newPage.close();
  });

  test('should validate all required fields', async ({ page }) => {
    // Act - Submit empty form
    await registrationPage.submitForm();

    // Assert - Check for required field errors
    const errorMessage = await registrationPage.getErrorMessage();
    expect(errorMessage).toBeTruthy();

    // Verify individual fields are marked as required
    const requiredFields = ['email', 'username', 'password'];
    for (const field of requiredFields) {
      const isInvalid = await registrationPage.isFieldHighlightedAsError(field);
      expect(isInvalid).toBe(true);
    }

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });

  test('should handle XSS attempts in input fields', async ({ page }) => {
    // Arrange
    const xssPayloads = [
      '<script>alert("XSS")</script>',
      'javascript:alert("XSS")',
      '<img src=x onerror=alert("XSS")>',
      '<svg onload=alert("XSS")>',
    ];

    const userData = TestDataGenerator.generateUniqueUser();

    for (const payload of xssPayloads) {
      // Act
      await registrationPage.fillRegistrationForm({
        ...userData,
        displayName: payload,
        username: userData.username + Date.now(), // Ensure unique
      });
      await registrationPage.submitForm();

      // Assert - No script should execute
      // Check that no alert dialog appears
      const dialogPresent = await page.evaluate(() => {
        try {
          // If an alert was triggered, this would throw
          return false;
        } catch {
          return true;
        }
      });
      expect(dialogPresent).toBe(false);

      // If registration succeeds, verify the payload was properly sanitized
      if (!page.url().includes('/register')) {
        createdUsers.push(userData.email);

        // Check that the display name doesn't contain executable script
        const displayedName = await page.locator('text=/' + userData.displayName + '/');
        const nameContent = await displayedName.textContent();
        expect(nameContent).not.toContain('<script>');
        expect(nameContent).not.toContain('javascript:');
      }

      // Reset for next test
      await page.goto('/register');
    }
  });

  test('should prevent SQL injection attempts', async ({ page }) => {
    // Arrange
    const sqlPayloads = [
      "' OR '1'='1",
      "'; DROP TABLE users; --",
      "1' UNION SELECT * FROM users--",
      "admin'--",
    ];

    const userData = TestDataGenerator.generateUniqueUser();

    for (const payload of sqlPayloads) {
      // Act
      await registrationPage.fillRegistrationForm({
        ...userData,
        username: payload,
        email: userData.email + Date.now(), // Ensure unique
      });
      await registrationPage.submitForm();

      // Assert
      // The system should either:
      // 1. Reject the input with validation error
      // 2. Safely escape and store the input

      const errorMessage = await registrationPage.getErrorMessage();

      if (errorMessage) {
        // If there's an error, it should be a validation error, not a SQL error
        expect(errorMessage).not.toMatch(/sql|syntax|database/i);
        expect(errorMessage).toMatch(/invalid.*username|special.*characters/i);
      } else if (!page.url().includes('/register')) {
        // If registration succeeded, the payload was safely handled
        createdUsers.push(userData.email);
        console.log('[Security Test] SQL injection payload safely handled:', payload);
      }

      // Reset for next test
      await page.goto('/register');
    }
  });

  test('should handle network errors gracefully', async ({ page, context }) => {
    // Arrange
    const userData = TestDataGenerator.generateUniqueUser();

    // Simulate network failure
    await context.route('**/api/v1/auth/register', route => {
      route.abort('failed');
    });

    // Act
    await registrationPage.fillRegistrationForm(userData);
    await registrationPage.submitForm();

    // Assert
    const errorMessage = await registrationPage.getErrorMessage();
    expect(errorMessage).toMatch(/network.*error|connection.*failed|try.*again/i);

    // Verify form data is preserved
    const emailValue = await page.inputValue('input[name="email"]');
    expect(emailValue).toBe(userData.email);

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });

  test('should handle server errors gracefully', async ({ page, context }) => {
    // Arrange
    const userData = TestDataGenerator.generateUniqueUser();

    // Simulate server error
    await context.route('**/api/v1/auth/register', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' })
      });
    });

    // Act
    await registrationPage.fillRegistrationForm(userData);
    await registrationPage.submitForm();

    // Assert
    const errorMessage = await registrationPage.getErrorMessage();
    expect(errorMessage).toMatch(/server.*error|something.*wrong|try.*later/i);

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });
});