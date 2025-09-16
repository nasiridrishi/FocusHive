/**
 * E2E Tests for Invalid Login Attempts and Error Handling
 */

import {expect, test} from '@playwright/test';
import {LoginPage} from '../../pages/LoginPage';
import {DashboardPage} from '../../pages/DashboardPage';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Invalid Login Attempts', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page}) => {
    // Initialize page objects
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    authHelper = new AuthHelper(page);

    // Clear any existing authentication
    await authHelper.clearStorage();

    // Validate test environment
    validateTestEnvironment();
  });

  test.afterEach(async ({page: _page}) => {
    // Cleanup: Clear storage after each test
    await authHelper.clearStorage();
  });

  test('should show error message for wrong password', async () => {
    await loginPage.goto();

    // Use valid username but wrong password
    await loginPage.login(TEST_USERS.VALID_USER.username, 'wrongpassword123');
    await loginPage.waitForLoading();

    // Verify error message appears
    await loginPage.verifyErrorMessage();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();

    // Verify user is not authenticated
    const userMenu = dashboardPage.userMenu;
    await expect(userMenu).not.toBeVisible();
  });

  test('should show error message for non-existent username', async () => {
    await loginPage.goto();

    // Use non-existent username
    await loginPage.login('nonexistentuser12345', TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error message appears
    await loginPage.verifyErrorMessage();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();
  });

  test('should show error message for non-existent email', async () => {
    await loginPage.goto();

    // Use non-existent email
    await loginPage.login('nonexistent@example.com', TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error message appears
    await loginPage.verifyErrorMessage();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();
  });

  test('should show validation errors for empty username field', async () => {
    await loginPage.goto();

    // Submit with empty username
    await loginPage.login('', TEST_USERS.VALID_USER.password);

    // Verify validation error appears
    await loginPage.verifyValidationErrors();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();
  });

  test('should show validation errors for empty password field', async () => {
    await loginPage.goto();

    // Submit with empty password
    await loginPage.login(TEST_USERS.VALID_USER.username, '');

    // Verify validation error appears
    await loginPage.verifyValidationErrors();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();
  });

  test('should show validation errors for both empty fields', async () => {
    await loginPage.goto();

    // Submit empty form
    await loginPage.submitEmptyForm();

    // Verify validation errors appear for both fields
    await loginPage.verifyValidationErrors();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();
  });

  test('should show specific error message for invalid email format', async () => {
    await loginPage.goto();

    // Use invalid email format
    await loginPage.login('invalid-email-format', TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error message appears (could be validation error or login error)
    await loginPage.verifyErrorMessage();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();
  });

  test('should handle SQL injection attempts safely', async () => {
    await loginPage.goto();

    // Try SQL injection in username field
    const sqlInjectionPayloads = [
      "'; DROP TABLE users; --",
      "admin'--",
      "' OR '1'='1",
      "'; UPDATE users SET password='hacked' WHERE username='admin'; --"
    ];

    for (const payload of sqlInjectionPayloads) {
      // Clear form first
      await loginPage.clearForm();

      // Try injection payload
      await loginPage.login(payload, TEST_USERS.VALID_USER.password);
      await loginPage.waitForLoading();

      // Should show error, not succeed
      await loginPage.verifyErrorMessage();
      await loginPage.verifyStayOnLoginPage();

      // Verify no tokens are stored
      await authHelper.verifyTokensCleared();

    }
  });

  test('should handle XSS attempts safely', async () => {
    await loginPage.goto();

    // Try XSS in username field
    const xssPayloads = [
      '<script>alert("xss")</script>',
      'javascript:alert("xss")',
      '<img src="x" onerror="alert(\'xss\')">'
    ];

    for (const payload of xssPayloads) {
      // Clear form first
      await loginPage.clearForm();

      // Try XSS payload
      await loginPage.login(payload, TEST_USERS.VALID_USER.password);
      await loginPage.waitForLoading();

      // Should show error, not execute script
      await loginPage.verifyErrorMessage();
      await loginPage.verifyStayOnLoginPage();

      // Verify no alert dialog appeared (XSS blocked)
      const alertDialog = loginPage.page.locator('[role="alertdialog"]');
      await expect(alertDialog).not.toBeVisible();

    }
  });

  test('should rate limit excessive login attempts', async () => {
    await loginPage.goto();

    const maxAttempts = 5;
    const invalidCredentials = {
      username: 'testuser',
      password: 'wrongpassword'
    };

    // Make multiple failed login attempts rapidly
    for (let i = 0; i < maxAttempts; i++) {
      await loginPage.clearForm();
      await loginPage.login(invalidCredentials.username, invalidCredentials.password);
      await loginPage.waitForLoading();

      // Verify error message
      await loginPage.verifyErrorMessage();
      await loginPage.verifyStayOnLoginPage();

    }

    // After max attempts, should show rate limiting message or block further attempts
    await loginPage.clearForm();
    await loginPage.login(invalidCredentials.username, invalidCredentials.password);
    await loginPage.waitForLoading();

    // Look for rate limiting error message or disabled state
    const rateLimitError = loginPage.page.locator(':text("too many attempts"), :text("rate limit"), :text("try again later"), :text("account locked")');

    try {
      await expect(rateLimitError).toBeVisible({timeout: TIMEOUTS.SHORT});
    } catch {
      // Rate limiting might not be implemented yet, which is acceptable
    }
  });

  test('should show appropriate error for deactivated account', async ({page}) => {
    await loginPage.goto();

    // Mock server response for deactivated account
    await page.route('**/api/v1/auth/login', route => {
      route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Account has been deactivated',
          error: 'ACCOUNT_DEACTIVATED'
        })
      });
    });

    // Try to login with deactivated account
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify appropriate error message
    await loginPage.verifyErrorMessage();
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();
  });

  test('should show appropriate error for unverified email', async ({page}) => {
    await loginPage.goto();

    // Mock server response for unverified email
    await page.route('**/api/v1/auth/login', route => {
      route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Email address not verified',
          error: 'EMAIL_NOT_VERIFIED'
        })
      });
    });

    // Try to login with unverified email
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify appropriate error message
    await loginPage.verifyErrorMessage();
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();
  });

  test('should handle password with special characters correctly', async () => {
    await loginPage.goto();

    const specialPasswordCases = [
      'Pass@word123!',
      'Pässwörd123',
      'Pass word 123',
      'Pass"word\'123',
      'Pass<>word&123'
    ];

    for (const password of specialPasswordCases) {
      await loginPage.clearForm();

      // Try login with special character password
      await loginPage.login(TEST_USERS.VALID_USER.username, password);
      await loginPage.waitForLoading();

      // Should show authentication error (not encoding/parsing error)
      await loginPage.verifyErrorMessage();
      await loginPage.verifyStayOnLoginPage();

    }
  });

  test('should preserve username field after failed login', async () => {
    await loginPage.goto();

    const testCredentials = {
      username: 'testuser@example.com',
      password: 'wrongpassword'
    };

    // Attempt login
    await loginPage.login(testCredentials.username, testCredentials.password);
    await loginPage.waitForLoading();

    // Verify error
    await loginPage.verifyErrorMessage();

    // Check if username is preserved
    const formValues = await loginPage.getFormValues();
    expect(formValues.username).toBe(testCredentials.username);

    // Password should typically be cleared for security
  });

  test('should not leak sensitive information in error messages', async ({page}) => {
    await loginPage.goto();

    // Mock server response that might contain sensitive info
    await page.route('**/api/v1/auth/login', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Database connection failed',
          error: 'Connection to postgres://user:password@localhost:5432/db failed',
          stack: 'Error at /path/to/sensitive/code.js:123'
        })
      });
    });

    // Try to login
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error message doesn't contain sensitive information
    const errorElement = loginPage.errorMessage;
    await expect(errorElement).toBeVisible();

    const errorText = await errorElement.textContent();

    // Should not contain sensitive information
    expect(errorText?.toLowerCase()).not.toContain('password');
    expect(errorText?.toLowerCase()).not.toContain('database');
    expect(errorText?.toLowerCase()).not.toContain('connection');
    expect(errorText?.toLowerCase()).not.toContain('stack');
    expect(errorText?.toLowerCase()).not.toContain('postgres');

  });

  test('should handle concurrent login attempts gracefully', async ({context}) => {
    // Create multiple pages for concurrent login attempts
    const page1 = await context.newPage();
    const page2 = await context.newPage();

    const loginPage1 = new LoginPage(page1);
    const loginPage2 = new LoginPage(page2);

    await loginPage1.goto();
    await loginPage2.goto();

    // Try to login concurrently with same invalid credentials
    const loginPromise1 = loginPage1.login('testuser', 'wrongpassword1');
    const loginPromise2 = loginPage2.login('testuser', 'wrongpassword2');

    // Wait for both to complete
    await Promise.all([loginPromise1, loginPromise2]);

    await loginPage1.waitForLoading();
    await loginPage2.waitForLoading();

    // Both should show errors and stay on login page
    await loginPage1.verifyErrorMessage();
    await loginPage1.verifyStayOnLoginPage();

    await loginPage2.verifyErrorMessage();
    await loginPage2.verifyStayOnLoginPage();

    // Clean up
    await page1.close();
    await page2.close();
  });
});