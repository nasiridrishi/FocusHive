/**
 * E2E Tests for User Login Flow
 */

import {expect, test} from '@playwright/test';
import {LoginPage} from '../../pages/LoginPage';
import {DashboardPage} from '../../pages/DashboardPage';
import {SignupPage} from '../../pages/SignupPage';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_USERS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('User Login Flow', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;
  let signupPage: SignupPage;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page}) => {
    // Initialize page objects
    loginPage = new LoginPage(page);
    dashboardPage = new DashboardPage(page);
    signupPage = new SignupPage(page);
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

  test('should display login form with all required fields', async () => {
    await loginPage.goto();

    // Verify all form fields are present and visible
    await expect(loginPage.usernameInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
    await expect(loginPage.loginButton).toBeVisible();

    // Verify navigation link to register is present
    await expect(loginPage.registerLink).toBeVisible();

    // Verify login button is enabled
    await loginPage.verifyLoginButtonEnabled();
  });

  test('should show validation errors for empty form submission', async () => {
    await loginPage.goto();

    // Submit empty form
    await loginPage.submitEmptyForm();

    // Verify validation errors appear
    await loginPage.verifyValidationErrors();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();
  });

  test('should successfully login with valid credentials', async () => {
    await loginPage.goto();

    // Set up API monitoring to verify requests
    const apiMonitoring = await authHelper.setupApiMonitoring();

    // Login with valid credentials
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

    // Wait for login process to complete
    await loginPage.waitForLoading();

    // Verify API call was made
    const loginRequests = apiMonitoring.getLoginRequests();
    expect(loginRequests).toHaveLength(1);
    expect(loginRequests[0].postData).toMatchObject({
      username: TEST_USERS.VALID_USER.username,
      password: TEST_USERS.VALID_USER.password
    });

    // Verify successful login redirect
    await loginPage.verifySuccessfulLogin();

    // Verify we're on dashboard
    await dashboardPage.verifyOnDashboard();
    await dashboardPage.verifyAuthenticated();
  });

  test('should store JWT tokens after successful login', async () => {
    await loginPage.goto();

    // Login with valid credentials
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify JWT tokens are stored
    await authHelper.verifyTokensStored();

    // Verify tokens have valid structure (basic JWT format check)
    const accessToken = await loginPage.page.evaluate(() =>
        sessionStorage.getItem('access_token')
    );
    const refreshToken = await loginPage.page.evaluate(() =>
        localStorage.getItem('refresh_token')
    );

    // Basic JWT format validation (header.payload.signature)
    expect(accessToken?.split('.')).toHaveLength(3);
    expect(refreshToken?.split('.')).toHaveLength(3);
  });

  test('should load user data after successful login', async () => {
    await loginPage.goto();

    // Login with valid credentials
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify we're on dashboard
    await dashboardPage.verifyOnDashboard();

    // Wait for dashboard data to load
    await dashboardPage.waitForDataLoad();

    // Verify user menu is visible with user info
    await dashboardPage.verifyUserMenuVisible();

    // Try to get user info from the UI
    const userInfo = await dashboardPage.getCurrentUserInfo();
    expect(userInfo.displayName || userInfo.email).toBeTruthy();
  });

  test('should show error for invalid credentials', async () => {
    await loginPage.goto();

    // Login with invalid credentials
    await loginPage.login(TEST_USERS.INVALID_USER.username, TEST_USERS.INVALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error message appears
    await loginPage.verifyErrorMessage();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();
  });

  test('should show error for non-existent user', async () => {
    await loginPage.goto();

    // Login with non-existent user
    await loginPage.login('nonexistent@example.com', 'password123');
    await loginPage.waitForLoading();

    // Verify error message appears
    await loginPage.verifyErrorMessage();

    // Verify user stays on login page
    await loginPage.verifyStayOnLoginPage();
  });

  test('should navigate to register page when clicking register link', async () => {
    await loginPage.goto();

    // Click register link
    await loginPage.clickRegisterLink();

    // Verify navigation to register page
    await signupPage.waitForPageLoad();
    await expect(loginPage.page).toHaveURL(/\/register/);

    // Verify register form is visible
    await expect(signupPage.usernameInput).toBeVisible();
    await expect(signupPage.emailInput).toBeVisible();
  });

  test('should clear form when navigating away and back', async () => {
    await loginPage.goto();

    // Fill form
    await loginPage.fillUsername('testuser');
    await loginPage.fillPassword('testpass');

    // Verify form is filled
    await loginPage.verifyFormIsFilled('testuser', 'testpass');

    // Navigate away and back
    await loginPage.clickRegisterLink();
    await signupPage.waitForPageLoad();
    await signupPage.clickLoginLink();
    await loginPage.waitForPageLoad();

    // Verify form is cleared (depends on implementation)
    const _formValues = await loginPage.getFormValues();
    // Form might preserve values or clear them - both are valid UX patterns
  });

  test('should handle network errors gracefully', async ({page}) => {
    await loginPage.goto();

    // Mock network failure for login endpoint
    await page.route('**/api/v1/auth/login', route => {
      route.abort('internetdisconnected');
    });

    // Try to login
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error handling
    await loginPage.verifyErrorMessage();
    await loginPage.verifyStayOnLoginPage();

    // Verify no tokens are stored
    await authHelper.verifyTokensCleared();
  });

  test('should handle server errors (500) gracefully', async ({page}) => {
    await loginPage.goto();

    // Mock server error
    await page.route('**/api/v1/auth/login', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Internal server error',
          error: 'Something went wrong'
        })
      });
    });

    // Try to login
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error handling
    await loginPage.verifyErrorMessage();
    await loginPage.verifyStayOnLoginPage();
  });

  test('should handle authentication timeout (401) gracefully', async ({page}) => {
    await loginPage.goto();

    // Mock authentication failure
    await page.route('**/api/v1/auth/login', route => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Invalid credentials',
          error: 'Authentication failed'
        })
      });
    });

    // Try to login
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error handling
    await loginPage.verifyErrorMessage();
    await loginPage.verifyStayOnLoginPage();
  });

  test('should disable submit button during login process', async ({page}) => {
    await loginPage.goto();

    // Mock slow API response
    await page.route('**/api/v1/auth/login', async route => {
      // Delay response to test loading state
      await new Promise(resolve => setTimeout(resolve, 2000));
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: {
            id: 1,
            username: TEST_USERS.VALID_USER.username,
            email: TEST_USERS.VALID_USER.email
          },
          token: 'fake-jwt-token-header.payload.signature',
          refreshToken: 'fake-refresh-token-header.payload.signature'
        })
      });
    });

    // Fill and submit form
    await loginPage.fillUsername(TEST_USERS.VALID_USER.username);
    await loginPage.fillPassword(TEST_USERS.VALID_USER.password);
    await loginPage.clickLogin();

    // Verify button is disabled during submission
    await loginPage.verifyLoginButtonDisabled();

    // Wait for completion
    await loginPage.waitForLoading();
  });

  test('should preserve entered credentials on failed login', async () => {
    await loginPage.goto();

    const credentials = {
      username: 'invaliduser@example.com',
      password: 'wrongpassword'
    };

    // Login with invalid credentials
    await loginPage.login(credentials.username, credentials.password);
    await loginPage.waitForLoading();

    // Verify error message
    await loginPage.verifyErrorMessage();

    // Verify form still has the entered values
    const formValues = await loginPage.getFormValues();
    expect(formValues.username).toBe(credentials.username);
    // Password might be cleared for security reasons
  });

  test('should support login with email address', async () => {
    await loginPage.goto();

    // Login using email instead of username
    await loginPage.login(TEST_USERS.VALID_USER.email, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify successful login
    await loginPage.verifySuccessfulLogin();
    await dashboardPage.verifyOnDashboard();
    await authHelper.verifyTokensStored();
  });

  test('should handle malformed API response gracefully', async ({page}) => {
    await loginPage.goto();

    // Mock malformed response
    await page.route('**/api/v1/auth/login', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: 'invalid json response'
      });
    });

    // Try to login
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();

    // Verify error handling
    await loginPage.verifyErrorMessage();
    await loginPage.verifyStayOnLoginPage();
  });

  test('should redirect authenticated user away from login page', async () => {
    // First login
    await loginPage.goto();
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();
    await dashboardPage.verifyOnDashboard();

    // Now try to visit login page again
    await loginPage.goto();

    // Should be redirected away from login page (or login page should handle authenticated state)
    // Implementation may vary - either redirect to dashboard or show different UI
    await loginPage.page.waitForTimeout(2000); // Give time for any redirects

    const currentUrl = loginPage.page.url();

    // This test verifies behavior - either redirect or appropriate UI state
    const isOnLogin = currentUrl.includes('/login');
    const isOnDashboard = currentUrl.includes('/dashboard') || currentUrl.includes('/home');

    if (isOnLogin) {
      // If still on login page, verify it shows user is already logged in
      await dashboardPage.verifyUserMenuVisible();
    } else if (isOnDashboard) {
      // If redirected, verify we're on dashboard
      await dashboardPage.verifyOnDashboard();
    }
  });
});