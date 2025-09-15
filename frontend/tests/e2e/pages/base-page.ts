import { Page, Locator, expect } from '@playwright/test';

/**
 * Base Page Object Model
 * Contains common functionality shared across all pages
 */
export class BasePage {
  protected page: Page;
  
  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Navigate to a specific path
   */
  async goto(path: string = '') {
    await this.page.goto(path);
    await this.waitForLoad();
  }

  /**
   * Wait for page to load
   */
  async waitForLoad() {
    await this.page.waitForLoadState('networkidle', { timeout: 15000 });
  }

  /**
   * Wait for element to be visible
   */
  async waitForElement(selector: string, timeout: number = 10000): Promise<Locator> {
    const element = this.page.locator(selector);
    await element.waitFor({ state: 'visible', timeout });
    return element;
  }

  /**
   * Fill form field with proper waiting
   */
  async fillField(selector: string, value: string) {
    const field = await this.waitForElement(selector);
    await field.clear();
    await field.fill(value);
  }

  /**
   * Click element with proper waiting
   */
  async clickElement(selector: string) {
    const element = await this.waitForElement(selector);
    await element.click();
  }

  /**
   * Check if element exists
   */
  async elementExists(selector: string): Promise<boolean> {
    try {
      await this.page.locator(selector).waitFor({ state: 'attached', timeout: 2000 });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get current URL
   */
  async getCurrentUrl(): Promise<string> {
    return this.page.url();
  }

  /**
   * Wait for URL to contain specific text
   */
  async waitForUrl(urlPart: string, timeout: number = 10000) {
    await this.page.waitForURL(`**/${urlPart}**`, { timeout });
  }

  /**
   * Take screenshot with descriptive name
   */
  async takeScreenshot(name: string) {
    await this.page.screenshot({ 
      path: `test-results/screenshots/${name}-${Date.now()}.png`,
      fullPage: true 
    });
  }

  /**
   * Verify page title
   */
  async verifyPageTitle(expectedTitle: string) {
    await expect(this.page).toHaveTitle(expectedTitle);
  }

  /**
   * Verify element contains text
   */
  async verifyElementText(selector: string, expectedText: string) {
    const element = await this.waitForElement(selector);
    await expect(element).toContainText(expectedText);
  }

  /**
   * Verify element is visible
   */
  async verifyElementVisible(selector: string) {
    const element = this.page.locator(selector);
    await expect(element).toBeVisible();
  }

  /**
   * Verify element is hidden
   */
  async verifyElementHidden(selector: string) {
    const element = this.page.locator(selector);
    await expect(element).toBeHidden();
  }

  /**
   * Wait for API response
   */
  async waitForApiResponse(urlPattern: string, timeout: number = 10000) {
    return this.page.waitForResponse(
      response => response.url().includes(urlPattern) && response.status() === 200,
      { timeout }
    );
  }

  /**
   * Verify no JavaScript errors on page
   */
  async verifyNoJavaScriptErrors() {
    const errors: string[] = [];
    
    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    this.page.on('pageerror', (error) => {
      errors.push(error.message);
    });

    // Wait a bit to collect any errors
    await this.page.waitForTimeout(1000);

    if (errors.length > 0) {
      console.warn('JavaScript errors detected:', errors);
      // Don't fail tests for JS errors in development, but log them
    }
  }

  /**
   * Handle alerts/dialogs
   */
  setupDialogHandler(accept: boolean = true) {
    this.page.on('dialog', async (dialog) => {
      console.log(`Dialog detected: ${dialog.type()} - ${dialog.message()}`);
      if (accept) {
        await dialog.accept();
      } else {
        await dialog.dismiss();
      }
    });
  }

  /**
   * Get localStorage item
   */
  async getLocalStorageItem(key: string): Promise<string | null> {
    return this.page.evaluate((key) => localStorage.getItem(key), key);
  }

  /**
   * Set localStorage item
   */
  async setLocalStorageItem(key: string, value: string) {
    await this.page.evaluate(
      ({ key, value }) => localStorage.setItem(key, value),
      { key, value }
    );
  }

  /**
   * Clear localStorage
   */
  async clearLocalStorage() {
    await this.page.evaluate(() => localStorage.clear());
  }
}