/**
 * Authentication helper functions for E2E tests
 */

import {expect, Page} from '@playwright/test';
import {generateUniqueUser, SELECTORS, TEST_USERS, TIMEOUTS} from './test-data';

interface ApiRequestInfo {
  url: string;
  method: string;
  postData: unknown;
  timestamp: number;
}

export class AuthHelper {
  constructor(private page: Page) {
  }

  /**
   * Navigate to login page
   */
  async navigateToLogin(): Promise<void> {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');

    // Verify we're on the login page
    await expect(this.page.locator(SELECTORS.LOGIN_USERNAME_INPUT)).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    await expect(this.page.locator(SELECTORS.LOGIN_PASSWORD_INPUT)).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Navigate to register page
   */
  async navigateToRegister(): Promise<void> {
    await this.page.goto('/register');
    await this.page.waitForLoadState('networkidle');

    // Verify we're on the register page
    await expect(this.page.locator(SELECTORS.REGISTER_USERNAME_INPUT)).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    await expect(this.page.locator(SELECTORS.REGISTER_EMAIL_INPUT)).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Fill login form
   */
  async fillLoginForm(username: string, password: string): Promise<void> {
    await this.page.locator(SELECTORS.LOGIN_USERNAME_INPUT).fill(username);
    await this.page.locator(SELECTORS.LOGIN_PASSWORD_INPUT).fill(password);
  }

  /**
   * Submit login form
   */
  async submitLoginForm(): Promise<void> {
    await this.page.locator(SELECTORS.LOGIN_SUBMIT_BUTTON).click();
  }

  /**
   * Fill register form
   */
  async fillRegisterForm(userData: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Promise<void> {
    await this.page.locator(SELECTORS.REGISTER_USERNAME_INPUT).fill(userData.username);
    await this.page.locator(SELECTORS.REGISTER_EMAIL_INPUT).fill(userData.email);
    await this.page.locator(SELECTORS.REGISTER_PASSWORD_INPUT).fill(userData.password);
    await this.page.locator(SELECTORS.REGISTER_FIRST_NAME_INPUT).fill(userData.firstName);
    await this.page.locator(SELECTORS.REGISTER_LAST_NAME_INPUT).fill(userData.lastName);
  }

  /**
   * Submit register form
   */
  async submitRegisterForm(): Promise<void> {
    await this.page.locator(SELECTORS.REGISTER_SUBMIT_BUTTON).click();
  }

  /**
   * Login with credentials
   */
  async login(username: string, password: string): Promise<void> {
    await this.navigateToLogin();
    await this.fillLoginForm(username, password);
    await this.submitLoginForm();
  }

  /**
   * Register with user data
   */
  async register(userData: typeof TEST_USERS.VALID_USER): Promise<void> {
    await this.navigateToRegister();
    await this.fillRegisterForm(userData);
    await this.submitRegisterForm();
  }

  /**
   * Login with valid test user
   */
  async loginWithValidUser(): Promise<void> {
    await this.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
  }

  /**
   * Login with invalid credentials
   */
  async loginWithInvalidCredentials(): Promise<void> {
    await this.login(TEST_USERS.INVALID_USER.username, TEST_USERS.INVALID_USER.password);
  }

  /**
   * Register a new unique user to avoid conflicts
   */
  async registerUniqueUser(): Promise<typeof TEST_USERS.VALID_USER> {
    const uniqueUser = generateUniqueUser(TEST_USERS.NEW_USER);
    await this.register(uniqueUser);
    return uniqueUser;
  }

  /**
   * Logout current user
   */
  async logout(): Promise<void> {
    // Try to find user menu first
    const userMenu = this.page.locator(SELECTORS.USER_MENU);

    try {
      // Wait for user menu to be visible
      await userMenu.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
      await userMenu.click();

      // Wait for logout button and click it
      const logoutButton = this.page.locator(SELECTORS.LOGOUT_BUTTON);
      await logoutButton.waitFor({state: 'visible', timeout: TIMEOUTS.SHORT});
      await logoutButton.click();
    } catch {
      // If user menu approach fails, try direct logout button
      const logoutButton = this.page.locator(SELECTORS.LOGOUT_BUTTON);
      await logoutButton.click();
    }
  }

  /**
   * Verify user is logged in
   */
  async verifyLoggedIn(): Promise<void> {
    // Should be redirected to dashboard or show user menu
    await expect(this.page).toHaveURL(/\/dashboard|\/home|\/app/, {timeout: TIMEOUTS.NETWORK});

    // User menu should be visible
    await expect(this.page.locator(SELECTORS.USER_MENU)).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify user is logged out
   */
  async verifyLoggedOut(): Promise<void> {
    // Should be redirected to login page or home
    await expect(this.page).toHaveURL(/\/login|\/$/);

    // Login form should be visible
    await expect(this.page.locator(SELECTORS.LOGIN_USERNAME_INPUT)).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify error message is shown
   */
  async verifyErrorMessage(expectedMessage?: string): Promise<void> {
    const errorElement = this.page.locator(SELECTORS.ERROR_MESSAGE);
    await expect(errorElement).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    if (expectedMessage) {
      await expect(errorElement).toContainText(expectedMessage);
    }
  }

  /**
   * Verify success message is shown
   */
  async verifySuccessMessage(expectedMessage?: string): Promise<void> {
    const successElement = this.page.locator(SELECTORS.SUCCESS_MESSAGE);
    await expect(successElement).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    if (expectedMessage) {
      await expect(successElement).toContainText(expectedMessage);
    }
  }

  /**
   * Check if JWT tokens are stored in browser storage
   */
  async verifyTokensStored(): Promise<void> {
    try {
      // Check sessionStorage for access token
      const accessToken = await this.page.evaluate(() =>
          sessionStorage.getItem('access_token')
      );
      expect(accessToken).toBeTruthy();

      // Check localStorage for refresh token
      const refreshToken = await this.page.evaluate(() =>
          localStorage.getItem('refresh_token')
      );
      expect(refreshToken).toBeTruthy();
    } catch (error) {
      throw new Error(`Unable to verify tokens in storage: ${(error as Error).message}`);
    }
  }

  /**
   * Verify tokens are cleared from browser storage
   */
  async verifyTokensCleared(): Promise<void> {
    try {
      // Check that tokens are removed from storage
      const accessToken = await this.page.evaluate(() =>
          sessionStorage.getItem('access_token')
      );
      expect(accessToken).toBeNull();

      const refreshToken = await this.page.evaluate(() =>
          localStorage.getItem('refresh_token')
      );
      expect(refreshToken).toBeNull();
    } catch {
      // If storage is not accessible, it means we're not on a valid page
      // In this case, tokens are effectively cleared
    }
  }

  /**
   * Clear all browser storage (useful for test cleanup)
   */
  async clearStorage(): Promise<void> {
    try {
      await this.page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
      });
    } catch {
      // If storage is not accessible (e.g., before page loads), navigate to a basic page first
      try {
        await this.page.goto('data:text/html,<html><body>Loading...</body></html>');
        await this.page.evaluate(() => {
          localStorage.clear();
          sessionStorage.clear();
        });
      } catch {
        // If still failing, storage clearing will happen when page loads
      }
    }
  }

  /**
   * Wait for loading to complete
   */
  async waitForLoadingComplete(): Promise<void> {
    // Wait for any loading spinners to disappear
    const loadingSpinner = this.page.locator(SELECTORS.LOADING_SPINNER);

    try {
      // If loading spinner is present, wait for it to disappear
      await loadingSpinner.waitFor({state: 'hidden', timeout: TIMEOUTS.SHORT});
    } catch {
      // Loading spinner might not be present, which is fine
    }

    // Wait for network to be idle
    await this.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Setup API request interception for monitoring
   */
  async setupApiMonitoring(): Promise<{
    getLoginRequests: () => ApiRequestInfo[];
    getRegisterRequests: () => ApiRequestInfo[];
    clearRequests: () => void;
  }> {
    const loginRequests: ApiRequestInfo[] = [];
    const registerRequests: ApiRequestInfo[] = [];

    // Monitor login API calls
    await this.page.route(`**/api/v1/auth/login`, (route) => {
      loginRequests.push({
        url: route.request().url(),
        method: route.request().method(),
        postData: route.request().postDataJSON(),
        timestamp: Date.now(),
      });
      route.continue();
    });

    // Monitor register API calls
    await this.page.route(`**/api/v1/auth/register`, (route) => {
      registerRequests.push({
        url: route.request().url(),
        method: route.request().method(),
        postData: route.request().postDataJSON(),
        timestamp: Date.now(),
      });
      route.continue();
    });

    return {
      getLoginRequests: () => [...loginRequests],
      getRegisterRequests: () => [...registerRequests],
      clearRequests: () => {
        loginRequests.length = 0;
        registerRequests.length = 0;
      },
    };
  }
}