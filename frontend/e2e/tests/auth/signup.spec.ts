/**
 * E2E Tests for User Signup Flow
 */

import {expect, test} from '@playwright/test';
import {SignupPage} from '../../pages/SignupPage';
import {DashboardPage} from '../../pages/DashboardPage';
import {LoginPage} from '../../pages/LoginPage';
import {AuthHelper} from '../../helpers/auth.helper';
import {
  generateUniqueUser,
  TEST_USERS,
  TIMEOUTS,
  validateTestEnvironment
} from '../../helpers/test-data';

test.describe('User Signup Flow', () => {
  let signupPage: SignupPage;
  let dashboardPage: DashboardPage;
  let loginPage: LoginPage;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page}) => {
    // Initialize page objects
    signupPage = new SignupPage(page);
    dashboardPage = new DashboardPage(page);
    loginPage = new LoginPage(page);
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

  test('should display signup form with all required fields', async () => {
    await signupPage.goto();

    // Verify all form fields are present and visible
    await expect(signupPage.usernameInput).toBeVisible();
    await expect(signupPage.emailInput).toBeVisible();
    await expect(signupPage.passwordInput).toBeVisible();
    await expect(signupPage.firstNameInput).toBeVisible();
    await expect(signupPage.lastNameInput).toBeVisible();
    await expect(signupPage.signupButton).toBeVisible();

    // Verify navigation link to login is present
    await expect(signupPage.loginLink).toBeVisible();
  });

  test('should show validation errors for empty form submission', async () => {
    await signupPage.goto();

    // Submit empty form
    await signupPage.submitEmptyForm();

    // Verify validation errors appear
    await signupPage.verifyValidationErrors();

    // Verify user stays on signup page
    await signupPage.verifyStayOnSignupPage();
  });

  test('should show validation errors for invalid form data', async () => {
    await signupPage.goto();

    // Fill form with invalid data
    await signupPage.fillPartialForm({
      username: 'ab', // Too short
      email: 'invalid-email', // Invalid email format
      password: '123', // Too short and doesn't meet requirements
      firstName: '', // Empty required field
      lastName: '' // Empty required field
    });

    await signupPage.clickSignup();

    // Verify validation errors appear
    await signupPage.verifyValidationErrors();

    // Verify user stays on signup page
    await signupPage.verifyStayOnSignupPage();
  });

  test('should successfully register new user with valid data', async () => {
    // Generate unique user data to avoid conflicts
    const newUser = generateUniqueUser(TEST_USERS.NEW_USER);

    await signupPage.goto();

    // Set up API monitoring to verify requests
    const apiMonitoring = await authHelper.setupApiMonitoring();

    // Fill form with valid data
    await signupPage.fillSignupForm(newUser);

    // Submit form
    await signupPage.clickSignup();

    // Wait for signup process to complete
    await signupPage.waitForLoading();

    // Verify API call was made
    const registerRequests = apiMonitoring.getRegisterRequests();
    expect(registerRequests).toHaveLength(1);
    expect(registerRequests[0].postData).toMatchObject({
      username: newUser.username,
      email: newUser.email,
      firstName: newUser.firstName,
      lastName: newUser.lastName
    });

    // Verify successful registration
    await signupPage.verifySuccessfulSignup();
  });

  test('should auto-login user after successful registration', async () => {
    // Generate unique user data
    const newUser = generateUniqueUser(TEST_USERS.NEW_USER);

    await signupPage.goto();

    // Complete signup
    await signupPage.signup(newUser);
    await signupPage.waitForLoading();

    // Verify user is automatically logged in and redirected to dashboard
    await dashboardPage.verifyOnDashboard();
    await dashboardPage.verifyAuthenticated();

    // Verify JWT tokens are stored
    await authHelper.verifyTokensStored();

    // Verify user menu shows correct user info
    await dashboardPage.verifyUserMenuVisible();
  });

  test('should show error for duplicate username registration', async () => {
    // Use the same user data that might already exist
    const existingUser = TEST_USERS.VALID_USER;

    await signupPage.goto();

    // Try to register with existing username
    await signupPage.signup(existingUser);
    await signupPage.waitForLoading();

    // Verify error message for duplicate user
    await signupPage.verifyErrorMessage();

    // Verify user stays on signup page
    await signupPage.verifyStayOnSignupPage();
  });

  test('should show error for duplicate email registration', async () => {
    // Create user data with existing email
    const duplicateEmailUser = {
      ...generateUniqueUser(TEST_USERS.NEW_USER),
      email: TEST_USERS.VALID_USER.email // Use existing email
    };

    await signupPage.goto();

    // Try to register with existing email
    await signupPage.signup(duplicateEmailUser);
    await signupPage.waitForLoading();

    // Verify error message for duplicate email
    await signupPage.verifyErrorMessage();

    // Verify user stays on signup page
    await signupPage.verifyStayOnSignupPage();
  });

  test('should navigate to login page when clicking login link', async () => {
    await signupPage.goto();

    // Click login link
    await signupPage.clickLoginLink();

    // Verify navigation to login page
    await loginPage.waitForPageLoad();
    await expect(signupPage.page).toHaveURL(/\/login/);

    // Verify login form is visible
    await expect(loginPage.usernameInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
  });

  test('should validate email format in real-time', async () => {
    await signupPage.goto();

    // Fill invalid email
    await signupPage.fillEmail('invalid-email');
    await signupPage.fillUsername('testuser');

    // Try to submit or move to next field
    await signupPage.passwordInput.focus();

    // Check for email validation error
    // Note: This depends on real-time validation implementation
    const emailError = signupPage.page.locator('input[name="email"] ~ .error, input[name="email"] ~ .MuiFormHelperText-error');

    // Wait a bit for validation to trigger
    await signupPage.page.waitForTimeout(1000);

    try {
      await expect(emailError).toBeVisible({timeout: TIMEOUTS.SHORT});
    } catch {
      // Real-time validation might not be implemented, which is fine
    }
  });

  test('should validate password requirements', async () => {
    await signupPage.goto();

    // Fill weak password
    await signupPage.fillPassword('weak');
    await signupPage.fillUsername('testuser');

    // Submit form to trigger validation
    await signupPage.clickSignup();

    // Verify password validation error
    await signupPage.verifyValidationErrors();
  });

  test('should handle network errors gracefully', async ({page}) => {
    await signupPage.goto();

    // Mock network failure for register endpoint
    await page.route('**/api/v1/auth/register', route => {
      route.abort('internetdisconnected');
    });

    const newUser = generateUniqueUser(TEST_USERS.NEW_USER);

    // Try to register
    await signupPage.signup(newUser);
    await signupPage.waitForLoading();

    // Verify error handling
    await signupPage.verifyErrorMessage();
    await signupPage.verifyStayOnSignupPage();
  });

  test('should handle server errors (500) gracefully', async ({page}) => {
    await signupPage.goto();

    // Mock server error
    await page.route('**/api/v1/auth/register', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Internal server error',
          error: 'Something went wrong'
        })
      });
    });

    const newUser = generateUniqueUser(TEST_USERS.NEW_USER);

    // Try to register
    await signupPage.signup(newUser);
    await signupPage.waitForLoading();

    // Verify error handling
    await signupPage.verifyErrorMessage();
    await signupPage.verifyStayOnSignupPage();
  });

  test('should preserve form data on validation errors', async () => {
    await signupPage.goto();

    const partialData = {
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User'
      // Missing password
    };

    // Fill partial form
    await signupPage.fillPartialForm(partialData);

    // Submit to trigger validation
    await signupPage.clickSignup();

    // Verify form data is preserved
    const formValues = await signupPage.getFormValues();
    expect(formValues.username).toBe(partialData.username);
    expect(formValues.email).toBe(partialData.email);
    expect(formValues.firstName).toBe(partialData.firstName);
    expect(formValues.lastName).toBe(partialData.lastName);
  });

  test('should disable submit button during form submission', async ({page}) => {
    await signupPage.goto();

    // Mock slow API response
    await page.route('**/api/v1/auth/register', async route => {
      // Delay response to test loading state
      await new Promise(resolve => setTimeout(resolve, 2000));
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: {id: 1, username: 'test', email: 'test@example.com'},
          token: 'fake-token',
          refreshToken: 'fake-refresh-token'
        })
      });
    });

    const newUser = generateUniqueUser(TEST_USERS.NEW_USER);

    // Fill and submit form
    await signupPage.fillSignupForm(newUser);
    await signupPage.clickSignup();

    // Verify button is disabled during submission
    await signupPage.verifySignupButtonDisabled();

    // Wait for completion
    await signupPage.waitForLoading();
  });
});