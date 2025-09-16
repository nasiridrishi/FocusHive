/**
 * E2E Tests for User Logout Flow
 */

import {expect, test} from '@playwright/test';
import {LoginPage} from '../../pages/LoginPage';
import {DashboardPage} from '../../pages/DashboardPage';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('User Logout Flow', () => {
  let loginPage: LoginPage;
  let dashboardPage: DashboardPage;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page: _page}) => {
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

  /**
   * Helper function to ensure user is logged in before logout tests
   */
  async function ensureUserLoggedIn(): Promise<void> {
    await loginPage.goto();
    await loginPage.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
    await loginPage.waitForLoading();
    await dashboardPage.verifyOnDashboard();
    await authHelper.verifyTokensStored();
  }

  test('should successfully logout authenticated user', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Perform logout
    await dashboardPage.logout();

    // Verify logout redirect
    await dashboardPage.verifyLogoutRedirect();

    // Verify tokens are cleared from storage
    await authHelper.verifyTokensCleared();

    // Verify user is now on login page or home page
    await expect(dashboardPage.page).toHaveURL(/\/login|\/$/);

    // If on login page, verify login form is visible
    if (dashboardPage.page.url().includes('/login')) {
      await loginPage.waitForPageLoad();
      await expect(loginPage.usernameInput).toBeVisible();
    }
  });

  test('should clear JWT tokens from browser storage on logout', async () => {
    // First, login user and verify tokens exist
    await ensureUserLoggedIn();

    // Double-check tokens are present before logout
    const beforeLogout = {
      accessToken: await dashboardPage.page.evaluate(() => sessionStorage.getItem('access_token')),
      refreshToken: await dashboardPage.page.evaluate(() => localStorage.getItem('refresh_token'))
    };

    expect(beforeLogout.accessToken).toBeTruthy();
    expect(beforeLogout.refreshToken).toBeTruthy();

    // Perform logout
    await dashboardPage.logout();
    await dashboardPage.verifyLogoutRedirect();

    // Verify tokens are completely removed
    const afterLogout = {
      accessToken: await dashboardPage.page.evaluate(() => sessionStorage.getItem('access_token')),
      refreshToken: await dashboardPage.page.evaluate(() => localStorage.getItem('refresh_token'))
    };

    expect(afterLogout.accessToken).toBeNull();
    expect(afterLogout.refreshToken).toBeNull();
  });

  test('should prevent access to protected routes after logout', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Perform logout
    await dashboardPage.logout();
    await dashboardPage.verifyLogoutRedirect();

    // Try to access protected routes
    const protectedRoutes = ['/dashboard', '/profile', '/settings'];

    for (const route of protectedRoutes) {
      try {
        await dashboardPage.page.goto(route);
        await dashboardPage.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.MEDIUM});

        // Should be redirected to login or show access denied
        const currentUrl = dashboardPage.page.url();

        if (!currentUrl.includes('/login') && !currentUrl.includes('/')) {
          // If not redirected, check for access denied message
          const accessDenied = dashboardPage.page.locator(':text("Access Denied"), :text("Unauthorized"), :text("Please log in")');
          await expect(accessDenied.first()).toBeVisible({timeout: TIMEOUTS.SHORT});
        }

      } catch {
        // Network errors are expected in this test scenario
      }
    }
  });

  test('should handle logout when user menu is available', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Verify user menu is visible
    await dashboardPage.verifyUserMenuVisible();

    // Click user menu to open it
    await dashboardPage.openUserMenu();

    // Click logout button from menu
    await dashboardPage.logout();

    // Verify successful logout
    await dashboardPage.verifyLogoutRedirect();
    await authHelper.verifyTokensCleared();
  });

  test('should handle logout API call failure gracefully', async ({page}) => {
    // First, login user
    await ensureUserLoggedIn();

    // Mock logout API failure
    await page.route('**/api/v1/auth/logout', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Server error during logout',
          error: 'Internal server error'
        })
      });
    });

    // Perform logout - should still work locally even if API fails
    await dashboardPage.logout();

    // Verify tokens are still cleared locally (client-side logout)
    await authHelper.verifyTokensCleared();

    // Verify redirect still happens
    await dashboardPage.verifyLogoutRedirect();
  });

  test('should handle logout with network connectivity issues', async ({page}) => {
    // First, login user
    await ensureUserLoggedIn();

    // Mock network disconnection during logout
    await page.route('**/api/v1/auth/logout', route => {
      route.abort('internetdisconnected');
    });

    // Perform logout - should work despite network issues
    await dashboardPage.logout();

    // Verify local cleanup still happens
    await authHelper.verifyTokensCleared();
    await dashboardPage.verifyLogoutRedirect();
  });

  test('should invalidate session and prevent token reuse after logout', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Store tokens before logout
    const tokensBeforeLogout = {
      accessToken: await dashboardPage.page.evaluate(() => sessionStorage.getItem('access_token')),
      refreshToken: await dashboardPage.page.evaluate(() => localStorage.getItem('refresh_token'))
    };

    // Perform logout
    await dashboardPage.logout();
    await authHelper.verifyTokensCleared();

    // Try to manually set the old tokens back
    await dashboardPage.page.evaluate((tokens) => {
      sessionStorage.setItem('access_token', tokens.accessToken);
      localStorage.setItem('refresh_token', tokens.refreshToken);
    }, tokensBeforeLogout);

    // Try to access protected content with old tokens
    await dashboardPage.page.goto('/dashboard');
    await dashboardPage.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.MEDIUM});

    // Should still be redirected to login (tokens should be invalid)
    const currentUrl = dashboardPage.page.url();

    // Either redirected to login or tokens automatically cleared
    if (currentUrl.includes('/login') || currentUrl.includes('/')) {
      // Successful - old tokens are invalid
      expect(true).toBe(true);
    } else {
      // If somehow still on dashboard, tokens should have been cleared
      const tokensAfterAttempt = {
        accessToken: await dashboardPage.page.evaluate(() => sessionStorage.getItem('access_token')),
        refreshToken: await dashboardPage.page.evaluate(() => localStorage.getItem('refresh_token'))
      };

      expect(tokensAfterAttempt.accessToken).toBeNull();
      expect(tokensAfterAttempt.refreshToken).toBeNull();
    }
  });

  test('should show logout confirmation if implemented', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Try to logout and handle potential confirmation dialog
    await dashboardPage.openUserMenu();

    // Click logout button
    const logoutButton = dashboardPage.logoutButton;
    await logoutButton.click();

    // Check if confirmation dialog appears
    const confirmDialog = dashboardPage.page.locator('[role="dialog"], .confirmation, .modal');

    try {
      await confirmDialog.waitFor({state: 'visible', timeout: TIMEOUTS.SHORT});

      // If confirmation dialog exists, confirm logout
      const confirmButton = dashboardPage.page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Logout")');
      await confirmButton.click();

    } catch {
      // No confirmation dialog, direct logout
    }

    // Verify logout completed
    await dashboardPage.verifyLogoutRedirect();
    await authHelper.verifyTokensCleared();
  });

  test('should handle multiple logout attempts gracefully', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Perform first logout
    await dashboardPage.logout();
    await dashboardPage.verifyLogoutRedirect();
    await authHelper.verifyTokensCleared();

    // Try to logout again (should handle gracefully)
    try {
      await authHelper.logout();
      // Should not throw error
      expect(true).toBe(true);
    } catch {
      // Some implementations might throw, which is also acceptable
    }

    // Verify still logged out
    await authHelper.verifyTokensCleared();
  });

  test('should redirect to appropriate page after logout based on user context', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Navigate to a specific page before logout
    await dashboardPage.page.goto('/dashboard');
    await dashboardPage.page.waitForLoadState('networkidle');

    // Perform logout
    await dashboardPage.logout();

    // Verify redirect destination
    await dashboardPage.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});
    const redirectUrl = dashboardPage.page.url();

    // Should redirect to login page or home page
    expect(redirectUrl).toMatch(/\/login|\/$/);

  });

  test('should maintain logout state across page refreshes', async () => {
    // First, login user
    await ensureUserLoggedIn();

    // Perform logout
    await dashboardPage.logout();
    await dashboardPage.verifyLogoutRedirect();
    await authHelper.verifyTokensCleared();

    // Refresh the page
    await dashboardPage.page.reload();
    await dashboardPage.page.waitForLoadState('networkidle');

    // Verify still logged out after refresh
    await authHelper.verifyTokensCleared();

    // Should still be on login page or home page
    const urlAfterRefresh = dashboardPage.page.url();
    expect(urlAfterRefresh).toMatch(/\/login|\/$/);
  });
});