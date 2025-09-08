/**
 * Page Object Model for Signup/Register Page
 */

import { Page, expect, Locator } from '@playwright/test';
import { SELECTORS, TIMEOUTS, TEST_URLS } from '../helpers/test-data';

export interface SignupFormData {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export class SignupPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly signupButton: Locator;
  readonly loginLink: Locator;
  readonly errorMessage: Locator;
  readonly successMessage: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator(SELECTORS.REGISTER_USERNAME_INPUT);
    this.emailInput = page.locator(SELECTORS.REGISTER_EMAIL_INPUT);
    this.passwordInput = page.locator(SELECTORS.REGISTER_PASSWORD_INPUT);
    this.firstNameInput = page.locator(SELECTORS.REGISTER_FIRST_NAME_INPUT);
    this.lastNameInput = page.locator(SELECTORS.REGISTER_LAST_NAME_INPUT);
    this.signupButton = page.locator(SELECTORS.REGISTER_SUBMIT_BUTTON);
    this.loginLink = page.locator(SELECTORS.LOGIN_LINK);
    this.errorMessage = page.locator(SELECTORS.ERROR_MESSAGE);
    this.successMessage = page.locator(SELECTORS.SUCCESS_MESSAGE);
    this.loadingSpinner = page.locator(SELECTORS.LOADING_SPINNER);
  }

  /**
   * Navigate to signup page
   */
  async goto(): Promise<void> {
    await this.page.goto(TEST_URLS.REGISTER);
    await this.page.waitForLoadState('networkidle');
    await this.waitForPageLoad();
  }

  /**
   * Wait for signup page to fully load
   */
  async waitForPageLoad(): Promise<void> {
    await expect(this.usernameInput).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    await expect(this.emailInput).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    await expect(this.passwordInput).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    await expect(this.firstNameInput).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    await expect(this.lastNameInput).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    await expect(this.signupButton).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * Fill username field
   */
  async fillUsername(username: string): Promise<void> {
    await this.usernameInput.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.usernameInput.fill(username);
  }

  /**
   * Fill email field
   */
  async fillEmail(email: string): Promise<void> {
    await this.emailInput.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.emailInput.fill(email);
  }

  /**
   * Fill password field
   */
  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.passwordInput.fill(password);
  }

  /**
   * Fill first name field
   */
  async fillFirstName(firstName: string): Promise<void> {
    await this.firstNameInput.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.firstNameInput.fill(firstName);
  }

  /**
   * Fill last name field
   */
  async fillLastName(lastName: string): Promise<void> {
    await this.lastNameInput.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.lastNameInput.fill(lastName);
  }

  /**
   * Click signup button
   */
  async clickSignup(): Promise<void> {
    await this.signupButton.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.signupButton.click();
  }

  /**
   * Click login link to navigate to login page
   */
  async clickLoginLink(): Promise<void> {
    await this.loginLink.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM });
    await this.loginLink.click();
  }

  /**
   * Fill entire signup form
   */
  async fillSignupForm(formData: SignupFormData): Promise<void> {
    await this.fillUsername(formData.username);
    await this.fillEmail(formData.email);
    await this.fillPassword(formData.password);
    await this.fillFirstName(formData.firstName);
    await this.fillLastName(formData.lastName);
  }

  /**
   * Complete signup process
   */
  async signup(formData: SignupFormData): Promise<void> {
    await this.fillSignupForm(formData);
    await this.clickSignup();
  }

  /**
   * Submit empty form (for validation testing)
   */
  async submitEmptyForm(): Promise<void> {
    await this.clickSignup();
  }

  /**
   * Fill partial form (for testing validation)
   */
  async fillPartialForm(fields: Partial<SignupFormData>): Promise<void> {
    if (fields.username) await this.fillUsername(fields.username);
    if (fields.email) await this.fillEmail(fields.email);
    if (fields.password) await this.fillPassword(fields.password);
    if (fields.firstName) await this.fillFirstName(fields.firstName);
    if (fields.lastName) await this.fillLastName(fields.lastName);
  }

  /**
   * Clear all form fields
   */
  async clearForm(): Promise<void> {
    await this.usernameInput.fill('');
    await this.emailInput.fill('');
    await this.passwordInput.fill('');
    await this.firstNameInput.fill('');
    await this.lastNameInput.fill('');
  }

  /**
   * Wait for loading to complete
   */
  async waitForLoading(): Promise<void> {
    try {
      // Wait for loading spinner to appear if present
      await this.loadingSpinner.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT });
      // Then wait for it to disappear
      await this.loadingSpinner.waitFor({ state: 'hidden', timeout: TIMEOUTS.NETWORK });
    } catch {
      // Loading spinner might not appear, which is fine
    }
    
    // Wait for network idle
    await this.page.waitForLoadState('networkidle', { timeout: TIMEOUTS.NETWORK });
  }

  /**
   * Verify error message is displayed
   */
  async verifyErrorMessage(expectedMessage?: string): Promise<void> {
    await expect(this.errorMessage).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    
    if (expectedMessage) {
      await expect(this.errorMessage).toContainText(expectedMessage);
    }
  }

  /**
   * Verify success message is displayed
   */
  async verifySuccessMessage(expectedMessage?: string): Promise<void> {
    await expect(this.successMessage).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    
    if (expectedMessage) {
      await expect(this.successMessage).toContainText(expectedMessage);
    }
  }

  /**
   * Verify no error message is displayed
   */
  async verifyNoErrorMessage(): Promise<void> {
    await expect(this.errorMessage).not.toBeVisible();
  }

  /**
   * Verify form validation errors for multiple fields
   */
  async verifyValidationErrors(): Promise<void> {
    // Check that validation errors are visible
    const errorElements = this.page.locator(SELECTORS.ERROR_MESSAGE);
    await expect(errorElements.first()).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * Verify successful signup redirect or auto-login
   */
  async verifySuccessfulSignup(): Promise<void> {
    // Should redirect to dashboard or login page depending on auto-login setting
    await expect(this.page).toHaveURL(/\/dashboard|\/login|\/home/, { timeout: TIMEOUTS.NETWORK });
  }

  /**
   * Verify staying on signup page (failed signup)
   */
  async verifyStayOnSignupPage(): Promise<void> {
    await expect(this.page).toHaveURL(/\/register/);
    await expect(this.usernameInput).toBeVisible();
  }

  /**
   * Get form field values (for testing purposes)
   */
  async getFormValues(): Promise<SignupFormData> {
    const username = await this.usernameInput.inputValue();
    const email = await this.emailInput.inputValue();
    const password = await this.passwordInput.inputValue();
    const firstName = await this.firstNameInput.inputValue();
    const lastName = await this.lastNameInput.inputValue();
    
    return { username, email, password, firstName, lastName };
  }

  /**
   * Verify form is empty
   */
  async verifyFormIsEmpty(): Promise<void> {
    await expect(this.usernameInput).toHaveValue('');
    await expect(this.emailInput).toHaveValue('');
    await expect(this.passwordInput).toHaveValue('');
    await expect(this.firstNameInput).toHaveValue('');
    await expect(this.lastNameInput).toHaveValue('');
  }

  /**
   * Verify form is filled with specific data
   */
  async verifyFormIsFilled(formData: SignupFormData): Promise<void> {
    await expect(this.usernameInput).toHaveValue(formData.username);
    await expect(this.emailInput).toHaveValue(formData.email);
    await expect(this.passwordInput).toHaveValue(formData.password);
    await expect(this.firstNameInput).toHaveValue(formData.firstName);
    await expect(this.lastNameInput).toHaveValue(formData.lastName);
  }

  /**
   * Check if signup button is enabled
   */
  async isSignupButtonEnabled(): Promise<boolean> {
    return await this.signupButton.isEnabled();
  }

  /**
   * Verify signup button is disabled
   */
  async verifySignupButtonDisabled(): Promise<void> {
    await expect(this.signupButton).toBeDisabled();
  }

  /**
   * Verify signup button is enabled
   */
  async verifySignupButtonEnabled(): Promise<void> {
    await expect(this.signupButton).toBeEnabled();
  }

  /**
   * Verify specific field validation error
   */
  async verifyFieldError(fieldName: keyof SignupFormData, expectedError: string): Promise<void> {
    let fieldLocator: Locator;
    
    switch (fieldName) {
      case 'username':
        fieldLocator = this.usernameInput;
        break;
      case 'email':
        fieldLocator = this.emailInput;
        break;
      case 'password':
        fieldLocator = this.passwordInput;
        break;
      case 'firstName':
        fieldLocator = this.firstNameInput;
        break;
      case 'lastName':
        fieldLocator = this.lastNameInput;
        break;
    }
    
    // Look for error message near the field or general error message
    const fieldError = this.page.locator(`${fieldLocator} ~ .error, ${fieldLocator} ~ .MuiFormHelperText-error`);
    await expect(fieldError.first()).toContainText(expectedError);
  }
}