/**
 * Page Object Model for Dashboard Page
 */

import {expect, Locator, Page} from '@playwright/test';
import {SELECTORS, TEST_URLS, TIMEOUTS} from '../helpers/test-data';

export class DashboardPage {
  readonly page: Page;
  readonly userMenu: Locator;
  readonly logoutButton: Locator;
  readonly welcomeMessage: Locator;
  readonly mainContent: Locator;
  readonly navigationMenu: Locator;

  constructor(page: Page) {
    this.page = page;
    this.userMenu = page.locator(SELECTORS.USER_MENU);
    this.logoutButton = page.locator(SELECTORS.LOGOUT_BUTTON);
    this.welcomeMessage = page.locator('[data-testid="welcome-message"], .welcome, h1, h2');
    this.mainContent = page.locator('main, .main-content, [data-testid="dashboard"]');
    this.navigationMenu = page.locator('nav, .navigation, [data-testid="navigation"]');
  }

  /**
   * Navigate to dashboard page
   */
  async goto(): Promise<void> {
    await this.page.goto(TEST_URLS.DASHBOARD);
    await this.page.waitForLoadState('networkidle');
    await this.waitForPageLoad();
  }

  /**
   * Wait for dashboard page to fully load
   */
  async waitForPageLoad(): Promise<void> {
    // Wait for main content to be visible
    await expect(this.mainContent).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    // Wait for user menu to be available (indicates user is authenticated)
    await expect(this.userMenu).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Open user menu
   */
  async openUserMenu(): Promise<void> {
    await this.userMenu.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
    await this.userMenu.click();
  }

  /**
   * Click logout button
   */
  async logout(): Promise<void> {
    try {
      // Try to open user menu first
      await this.openUserMenu();

      // Wait for logout button to appear in menu
      await this.logoutButton.waitFor({state: 'visible', timeout: TIMEOUTS.SHORT});
      await this.logoutButton.click();
    } catch {
      // If user menu approach fails, try direct logout button
      await this.logoutButton.click();
    }
  }

  /**
   * Verify user is on dashboard
   */
  async verifyOnDashboard(): Promise<void> {
    await expect(this.page).toHaveURL(/\/dashboard|\/home|\/app/, {timeout: TIMEOUTS.NETWORK});
    await expect(this.mainContent).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify user menu is visible (user is authenticated)
   */
  async verifyUserMenuVisible(): Promise<void> {
    await expect(this.userMenu).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify user is authenticated by checking for authentication indicators
   */
  async verifyAuthenticated(): Promise<void> {
    // Check for user menu
    await this.verifyUserMenuVisible();

    // Verify we're on a protected route
    await this.verifyOnDashboard();
  }

  /**
   * Verify successful logout redirect
   */
  async verifyLogoutRedirect(): Promise<void> {
    // Should redirect to login page or home page
    await expect(this.page).toHaveURL(/\/login|\/$/, {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Get welcome message text
   */
  async getWelcomeMessage(): Promise<string> {
    await this.welcomeMessage.waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
    return await this.welcomeMessage.textContent() || '';
  }

  /**
   * Verify welcome message contains user info
   */
  async verifyWelcomeMessage(expectedUserInfo?: string): Promise<void> {
    await expect(this.welcomeMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    if (expectedUserInfo) {
      await expect(this.welcomeMessage).toContainText(expectedUserInfo);
    }
  }

  /**
   * Check if navigation menu is present
   */
  async verifyNavigationPresent(): Promise<void> {
    await expect(this.navigationMenu).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Navigate to a specific section via navigation menu
   */
  async navigateToSection(sectionName: string): Promise<void> {
    const sectionLink = this.page.locator(`nav a:has-text("${sectionName}"), .navigation a:has-text("${sectionName}")`);
    await sectionLink.click();
  }

  /**
   * Wait for dashboard data to load
   */
  async waitForDataLoad(): Promise<void> {
    // Wait for loading spinners to disappear
    const loadingSpinner = this.page.locator(SELECTORS.LOADING_SPINNER);

    try {
      await loadingSpinner.waitFor({state: 'hidden', timeout: TIMEOUTS.NETWORK});
    } catch {
      // Loading spinner might not be present, which is fine
    }

    // Wait for network to be idle
    await this.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Verify page title
   */
  async verifyPageTitle(expectedTitle: string): Promise<void> {
    await expect(this.page).toHaveTitle(new RegExp(expectedTitle, 'i'));
  }

  /**
   * Check if user has access to protected content
   */
  async verifyProtectedContent(): Promise<void> {
    // Verify main content is accessible
    await expect(this.mainContent).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    // Verify no "access denied" or "unauthorized" messages
    const accessDenied = this.page.locator(':text("Access Denied"), :text("Unauthorized"), :text("403"), :text("401")');
    await expect(accessDenied).not.toBeVisible();
  }

  /**
   * Get current user info from user menu or display
   */
  async getCurrentUserInfo(): Promise<{ displayName?: string; email?: string }> {
    await this.openUserMenu();

    const userInfo: { displayName?: string; email?: string } = {};

    try {
      // Try to get display name
      const displayNameElement = this.page.locator('[data-testid="user-display-name"], .user-name, .display-name');
      if (await displayNameElement.isVisible({timeout: TIMEOUTS.SHORT})) {
        userInfo.displayName = await displayNameElement.textContent() || undefined;
      }

      // Try to get email
      const emailElement = this.page.locator('[data-testid="user-email"], .user-email, .email');
      if (await emailElement.isVisible({timeout: TIMEOUTS.SHORT})) {
        userInfo.email = await emailElement.textContent() || undefined;
      }
    } catch {
      // User info might not be displayed, which is fine
    }

    return userInfo;
  }

  /**
   * Verify tokens are present in browser storage
   */
  async verifyTokensInStorage(): Promise<void> {
    // Check for access token in sessionStorage
    const accessToken = await this.page.evaluate(() => sessionStorage.getItem('access_token'));
    expect(accessToken).toBeTruthy();

    // Check for refresh token in localStorage
    const refreshToken = await this.page.evaluate(() => localStorage.getItem('refresh_token'));
    expect(refreshToken).toBeTruthy();
  }
}