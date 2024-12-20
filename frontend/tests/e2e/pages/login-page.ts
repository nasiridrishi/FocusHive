import { Page, Locator } from '@playwright/test';
import { BasePage } from './base-page';

/**
 * Login Page Object Model
 * Handles all login page interactions
 */
export class LoginPage extends BasePage {
  // Selectors
  private readonly emailInput = 'input[name="email"], input[type="email"]';
  private readonly passwordInput = 'input[name="password"], input[type="password"]';
  private readonly loginButton = 'button[type="submit"]:has-text("Sign In"), button:has-text("Login")';
  private readonly signupLink = 'a[href="/register"], button:has-text("Sign up")';
  private readonly forgotPasswordLink = 'a[href="/forgot-password"], button:has-text("Forgot")';
  private readonly errorMessage = '[role="alert"], .error, .error-message, .MuiAlert-message';
  
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to login page
   */
  async goto() {
    await super.goto('/login');
    await this.waitForLoginForm();
  }

  /**
   * Wait for login form to be ready
   */
  async waitForLoginForm() {
    await this.waitForElement(this.emailInput);
    await this.waitForElement(this.passwordInput);
    await this.waitForElement(this.loginButton);
  }

  /**
   * Fill email field
   */
  async fillEmail(email: string) {
    await this.fillField(this.emailInput, email);
  }

  /**
   * Fill password field
   */
  async fillPassword(password: string) {
    await this.fillField(this.passwordInput, password);
  }

  /**
   * Click login button
   */
  async clickLogin() {
    await this.clickElement(this.loginButton);
  }

  /**
   * Click sign up link
   */
  async clickSignupLink() {
    await this.clickElement(this.signupLink);
  }

  /**
   * Click forgot password link
   */
  async clickForgotPasswordLink() {
    await this.clickElement(this.forgotPasswordLink);
  }

  /**
   * Perform complete login
   */
  async login(email: string, password: string) {
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.clickLogin();
    
    // Wait a bit for the request to complete
    await this.page.waitForTimeout(2000);
  }

  /**
   * Verify login form is displayed
   */
  async verifyLoginFormDisplayed() {
    await this.verifyElementVisible(this.emailInput);
    await this.verifyElementVisible(this.passwordInput);
    await this.verifyElementVisible(this.loginButton);
  }

  /**
   * Verify error message is shown
   */
  async verifyErrorMessageShown(expectedMessage?: string) {
    await this.verifyElementVisible(this.errorMessage);
    
    if (expectedMessage) {
      await this.verifyElementText(this.errorMessage, expectedMessage);
    }
  }

  /**
   * Verify successful login (redirect)
   */
  async verifySuccessfulLogin() {
    // Wait for redirect away from login page
    await this.page.waitForFunction(
      () => !window.location.pathname.includes('/login'),
      { timeout: 10000 }
    );
    
    // Should be redirected to dashboard or home
    const currentUrl = await this.getCurrentUrl();
    if (!currentUrl.includes('/dashboard') && !currentUrl.includes('/home') && !currentUrl.includes('/app')) {
      throw new Error(`Expected to be redirected after login, but still on: ${currentUrl}`);
    }
  }

  /**
   * Get form field values (for validation)
   */
  async getFormValues() {
    const email = await this.page.locator(this.emailInput).inputValue();
    const password = await this.page.locator(this.passwordInput).inputValue();
    
    return { email, password };
  }

  /**
   * Verify form validation errors
   */
  async verifyFormValidationErrors() {
    // Look for validation errors on the form
    const hasErrors = await this.elementExists(this.errorMessage);
    if (!hasErrors) {
      // Also check for HTML5 validation
      const emailValid = await this.page.locator(this.emailInput).evaluate(
        (el: HTMLInputElement) => el.checkValidity()
      );
      const passwordValid = await this.page.locator(this.passwordInput).evaluate(
        (el: HTMLInputElement) => el.checkValidity()
      );
      
      if (emailValid && passwordValid) {
        throw new Error('Expected form validation errors, but form appears valid');
      }
    }
  }

  /**
   * Clear form fields
   */
  async clearForm() {
    await this.page.locator(this.emailInput).clear();
    await this.page.locator(this.passwordInput).clear();
  }
}