import { Page } from '@playwright/test';
import { BasePage } from './base-page';
import { TestUser } from '../helpers/test-data-manager';

/**
 * Registration Page Object Model
 * Handles all registration page interactions
 */
export class RegistrationPage extends BasePage {
  // Selectors
  private readonly firstNameInput = 'input[name="firstName"]';
  private readonly lastNameInput = 'input[name="lastName"]';
  private readonly usernameInput = 'input[name="username"]';
  private readonly emailInput = 'input[name="email"], input[type="email"]';
  private readonly passwordInput = 'input[name="password"]:not([name="confirmPassword"])';
  private readonly confirmPasswordInput = 'input[name="confirmPassword"], input[name="passwordConfirm"]';
  private readonly termsCheckbox = 'input[type="checkbox"][name*="terms"], input[type="checkbox"][name*="accept"]';
  private readonly registerButton = 'button[type="submit"]:has-text("Create"), button[type="submit"]:has-text("Register")';
  private readonly loginLink = 'a[href="/login"], button:has-text("Sign In")';
  private readonly errorMessage = '[role="alert"], .error, .error-message, .MuiAlert-message';
  private readonly successMessage = '.success, .success-message, .MuiAlert-message';
  private readonly passwordStrength = '.password-strength, [class*="strength"]';
  
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to registration page
   */
  async goto() {
    await super.goto('/register');
    await this.waitForRegistrationForm();
  }

  /**
   * Wait for registration form to be ready
   */
  async waitForRegistrationForm() {
    await this.waitForElement(this.emailInput);
    await this.waitForElement(this.passwordInput);
    await this.waitForElement(this.registerButton);
  }

  /**
   * Fill first name field
   */
  async fillFirstName(firstName: string) {
    if (await this.elementExists(this.firstNameInput)) {
      await this.fillField(this.firstNameInput, firstName);
    }
  }

  /**
   * Fill last name field
   */
  async fillLastName(lastName: string) {
    if (await this.elementExists(this.lastNameInput)) {
      await this.fillField(this.lastNameInput, lastName);
    }
  }

  /**
   * Fill username field
   */
  async fillUsername(username: string) {
    if (await this.elementExists(this.usernameInput)) {
      await this.fillField(this.usernameInput, username);
    }
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
   * Fill confirm password field
   */
  async fillConfirmPassword(password: string) {
    if (await this.elementExists(this.confirmPasswordInput)) {
      await this.fillField(this.confirmPasswordInput, password);
    }
  }

  /**
   * Check terms and conditions checkbox
   */
  async checkTermsAndConditions() {
    if (await this.elementExists(this.termsCheckbox)) {
      const checkbox = this.page.locator(this.termsCheckbox);
      const isChecked = await checkbox.isChecked();
      if (!isChecked) {
        await checkbox.check();
      }
    }
  }

  /**
   * Click register button
   */
  async clickRegister() {
    await this.clickElement(this.registerButton);
  }

  /**
   * Click login link
   */
  async clickLoginLink() {
    await this.clickElement(this.loginLink);
  }

  /**
   * Fill complete registration form
   */
  async fillRegistrationForm(user: TestUser) {
    await this.fillFirstName(user.firstName);
    await this.fillLastName(user.lastName);
    
    if (user.username) {
      await this.fillUsername(user.username);
    }
    
    await this.fillEmail(user.email);
    await this.fillPassword(user.password);
    await this.fillConfirmPassword(user.password);
    await this.checkTermsAndConditions();
  }

  /**
   * Perform complete registration
   */
  async register(user: TestUser) {
    await this.fillRegistrationForm(user);
    await this.clickRegister();
    
    // Wait a bit for the request to complete
    await this.page.waitForTimeout(2000);
  }

  /**
   * Verify registration form is displayed
   */
  async verifyRegistrationFormDisplayed() {
    await this.verifyElementVisible(this.emailInput);
    await this.verifyElementVisible(this.passwordInput);
    await this.verifyElementVisible(this.registerButton);
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
   * Verify success message is shown
   */
  async verifySuccessMessageShown(expectedMessage?: string) {
    await this.verifyElementVisible(this.successMessage);
    
    if (expectedMessage) {
      await this.verifyElementText(this.successMessage, expectedMessage);
    }
  }

  /**
   * Verify successful registration (redirect or success message)
   */
  async verifySuccessfulRegistration() {
    // Either we get a success message or we're redirected
    try {
      await this.verifySuccessMessageShown();
    } catch {
      // Check for redirect
      await this.page.waitForFunction(
        () => !window.location.pathname.includes('/register'),
        { timeout: 5000 }
      );
    }
  }

  /**
   * Verify password strength indicator
   */
  async verifyPasswordStrengthShown() {
    if (await this.elementExists(this.passwordStrength)) {
      await this.verifyElementVisible(this.passwordStrength);
    }
  }

  /**
   * Get form field values (for validation)
   */
  async getFormValues() {
    const email = await this.page.locator(this.emailInput).inputValue();
    const password = await this.page.locator(this.passwordInput).inputValue();
    
    let firstName = '';
    let lastName = '';
    let username = '';
    
    if (await this.elementExists(this.firstNameInput)) {
      firstName = await this.page.locator(this.firstNameInput).inputValue();
    }
    
    if (await this.elementExists(this.lastNameInput)) {
      lastName = await this.page.locator(this.lastNameInput).inputValue();
    }
    
    if (await this.elementExists(this.usernameInput)) {
      username = await this.page.locator(this.usernameInput).inputValue();
    }
    
    return { email, password, firstName, lastName, username };
  }

  /**
   * Verify form validation errors
   */
  async verifyFormValidationErrors() {
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
   * Clear all form fields
   */
  async clearForm() {
    if (await this.elementExists(this.firstNameInput)) {
      await this.page.locator(this.firstNameInput).clear();
    }
    if (await this.elementExists(this.lastNameInput)) {
      await this.page.locator(this.lastNameInput).clear();
    }
    if (await this.elementExists(this.usernameInput)) {
      await this.page.locator(this.usernameInput).clear();
    }
    
    await this.page.locator(this.emailInput).clear();
    await this.page.locator(this.passwordInput).clear();
    
    if (await this.elementExists(this.confirmPasswordInput)) {
      await this.page.locator(this.confirmPasswordInput).clear();
    }
  }
}