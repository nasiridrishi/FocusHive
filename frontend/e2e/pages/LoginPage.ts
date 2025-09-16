/**
 * Page Object Model for Login Page
 */

import {expect, Locator, Page} from '@playwright/test';
import {SELECTORS, TEST_URLS, TIMEOUTS} from '../helpers/test-data';

export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly registerLink: Locator;
  readonly errorMessage: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator(SELECTORS.LOGIN_USERNAME_INPUT);
    this.passwordInput = page.locator(SELECTORS.LOGIN_PASSWORD_INPUT);
    this.loginButton = page.locator(SELECTORS.LOGIN_SUBMIT_BUTTON);
    this.registerLink = page.locator(SELECTORS.REGISTER_LINK);
    this.errorMessage = page.locator(SELECTORS.ERROR_MESSAGE);
    this.loadingSpinner = page.locator(SELECTORS.LOADING_SPINNER);
  }

  /**
   * Navigate to login page
   */
  async goto(): Promise<void> {
    await this.page.goto(TEST_URLS.LOGIN);
    await this.page.waitForLoadState('networkidle');
    await this.waitForPageLoad();
  }

  /**
   * Wait for login page to fully load
   */
  async waitForPageLoad(): Promise<void> {
    await expect(this.usernameInput).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    await expect(this.passwordInput).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    await expect(this.loginButton).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Fill username/email field
   */
  async fillUsername(username: string): Promise<void> {
    await this.usernameInput.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
    await this.usernameInput.fill(username);
  }

  /**
   * Fill password field
   */
  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
    await this.passwordInput.fill(password);
  }

  /**
   * Click login button
   */
  async clickLogin(): Promise<void> {
    await this.loginButton.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
    await this.loginButton.click();
  }

  /**
   * Click register link to navigate to registration
   */
  async clickRegisterLink(): Promise<void> {
    await this.registerLink.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
    await this.registerLink.click();
  }

  /**
   * Login with credentials
   */
  async login(username: string, password: string): Promise<void> {
    await this.fillUsername(username);
    await this.fillPassword(password);
    await this.clickLogin();
  }

  /**
   * Submit empty form (for validation testing)
   */
  async submitEmptyForm(): Promise<void> {
    await this.clickLogin();
  }

  /**
   * Clear form fields
   */
  async clearForm(): Promise<void> {
    await this.usernameInput.fill('');
    await this.passwordInput.fill('');
  }

  /**
   * Wait for loading to complete
   */
  async waitForLoading(): Promise<void> {
    try {
      // Wait for loading spinner to appear if present
      await this.loadingSpinner.waitFor({state: 'visible', timeout: TIMEOUTS.SHORT});
      // Then wait for it to disappear
      await this.loadingSpinner.waitFor({state: 'hidden', timeout: TIMEOUTS.NETWORK});
    } catch {
      // Loading spinner might not appear, which is fine
    }

    // Wait for network idle
    await this.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Verify error message is displayed
   */
  async verifyErrorMessage(expectedMessage?: string): Promise<void> {
    await expect(this.errorMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    if (expectedMessage) {
      await expect(this.errorMessage).toContainText(expectedMessage);
    }
  }

  /**
   * Verify no error message is displayed
   */
  async verifyNoErrorMessage(): Promise<void> {
    await expect(this.errorMessage).not.toBeVisible();
  }

  /**
   * Verify form validation errors
   */
  async verifyValidationErrors(): Promise<void> {
    // Check that validation errors are visible
    const errorElements = this.page.locator(SELECTORS.ERROR_MESSAGE);
    await expect(errorElements.first()).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify successful login redirect
   */
  async verifySuccessfulLogin(): Promise<void> {
    // Should redirect to dashboard
    await expect(this.page).toHaveURL(/\/dashboard|\/home|\/app/, {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Verify staying on login page (failed login)
   */
  async verifyStayOnLoginPage(): Promise<void> {
    await expect(this.page).toHaveURL(/\/login/);
    await expect(this.usernameInput).toBeVisible();
  }

  /**
   * Get form field values (for testing purposes)
   */
  async getFormValues(): Promise<{ username: string; password: string }> {
    const username = await this.usernameInput.inputValue();
    const password = await this.passwordInput.inputValue();

    return {username, password};
  }

  /**
   * Verify form is empty
   */
  async verifyFormIsEmpty(): Promise<void> {
    await expect(this.usernameInput).toHaveValue('');
    await expect(this.passwordInput).toHaveValue('');
  }

  /**
   * Verify form is filled
   */
  async verifyFormIsFilled(username: string, password: string): Promise<void> {
    await expect(this.usernameInput).toHaveValue(username);
    await expect(this.passwordInput).toHaveValue(password);
  }

  /**
   * Check if login button is enabled
   */
  async isLoginButtonEnabled(): Promise<boolean> {
    return await this.loginButton.isEnabled();
  }

  /**
   * Check if login button is disabled
   */
  async verifyLoginButtonDisabled(): Promise<void> {
    await expect(this.loginButton).toBeDisabled();
  }

  /**
   * Check if login button is enabled
   */
  async verifyLoginButtonEnabled(): Promise<void> {
    await expect(this.loginButton).toBeEnabled();
  }
}