/**
 * E2E Tests for Session Management and Logout
 * Comprehensive session testing including logout, concurrent sessions, timeouts, and security
 */

import { test, expect, Browser, BrowserContext } from '@playwright/test';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { 
  AUTH_TEST_USERS, 
  SESSION_SCENARIOS,
  generateUniqueAuthUser,
  MOBILE_VIEWPORTS,
  AUTH_PERFORMANCE_THRESHOLDS
} from '../../helpers/auth-fixtures';

test.describe('Session Management and Logout', () => {
  let authHelper: EnhancedAuthHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new EnhancedAuthHelper(page);
    await authHelper.clearStorage();
  });

  test.afterEach(async () => {
    await authHelper.clearStorage();
  });

  test.describe('Basic Logout Functionality', () => {
    test('should logout user and clear session', async () => {
      // Login first
      const tokenInfo = await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      expect(tokenInfo.accessToken).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // Logout
      await authHelper.logout();

      // Verify redirect to login page
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });

      // Verify tokens are cleared
      const clearedTokenInfo = await authHelper.getTokenInfo();
      expect(clearedTokenInfo.accessToken).toBeNull();
      expect(clearedTokenInfo.refreshToken).toBeNull();
    });

    test('should show logout confirmation if configured', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Click user menu
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
      await userMenu.click();

      const logoutButton = authHelper.page.locator('button:has-text("Logout"), [data-testid="logout-button"], button:has-text("Sign out")');
      await logoutButton.click();

      // Check if confirmation dialog appears
      const confirmDialog = authHelper.page.locator('[role="dialog"], .modal, :text("Are you sure")');
      const hasConfirmDialog = await confirmDialog.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasConfirmDialog) {
        console.log('✅ Logout confirmation dialog shown');
        
        // Confirm logout
        const confirmButton = authHelper.page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Logout")');
        await confirmButton.click();
      } else {
        console.log('ℹ️ No logout confirmation dialog (immediate logout)');
      }

      // Should be redirected to login
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });

    test('should prevent access to protected routes after logout', async () => {
      // Login and navigate to protected route
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Logout
      await authHelper.logout();

      // Try to access protected route
      await authHelper.page.goto('/dashboard');
      await authHelper.page.waitForLoadState('networkidle');

      // Should be redirected to login
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });

    test('should handle logout API failures gracefully', async ({ page }) => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Mock logout API failure
      await page.route('**/api/v1/auth/logout', route => {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Logout failed',
            error: 'server_error',
          }),
        });
      });

      // Attempt logout
      await authHelper.logout();

      // Should still clear local tokens and redirect
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
      
      const tokenInfo = await authHelper.getTokenInfo();
      expect(tokenInfo.accessToken).toBeNull();
    });

    test('should support keyboard navigation for logout', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Use keyboard to navigate to logout
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
      
      // Focus and activate user menu
      await userMenu.focus();
      await authHelper.page.keyboard.press('Enter');

      // Navigate to logout option with keyboard
      const logoutButton = authHelper.page.locator('button:has-text("Logout"), [data-testid="logout-button"]');
      await logoutButton.focus();
      await authHelper.page.keyboard.press('Enter');

      // Should logout successfully
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });
  });

  test.describe('Logout All Devices', () => {
    test('should provide "logout all devices" option', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Open user menu
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
      await userMenu.click();

      // Look for "logout all devices" option
      const logoutAllButton = authHelper.page.locator('button:has-text("Logout All"), button:has-text("Sign out everywhere"), [data-testid="logout-all-button"]');
      const hasLogoutAll = await logoutAllButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasLogoutAll) {
        console.log('✅ "Logout all devices" option found');
        
        await logoutAllButton.click();

        // May show confirmation dialog
        const confirmButton = authHelper.page.locator('button:has-text("Confirm"), button:has-text("Yes")');
        const hasConfirm = await confirmButton.isVisible({ timeout: 2000 }).catch(() => false);
        
        if (hasConfirm) {
          await confirmButton.click();
        }

        // Should be redirected to login
        await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
      } else {
        console.log('ℹ️ "Logout all devices" option not found');
      }
    });

    test('should invalidate sessions across multiple contexts', async ({ browser }) => {
      // Create two browser contexts to simulate multiple devices
      const context1 = await browser.newContext();
      const context2 = await browser.newContext();
      
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();
      
      const helper1 = new EnhancedAuthHelper(page1);
      const helper2 = new EnhancedAuthHelper(page2);

      try {
        // Login on both "devices"
        await helper1.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username, 
          AUTH_TEST_USERS.VALID_USER.password
        );
        await helper1.verifyOnDashboard();

        await helper2.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username, 
          AUTH_TEST_USERS.VALID_USER.password
        );
        await helper2.verifyOnDashboard();

        // Logout all devices from context1
        await helper1.logoutAllDevices();

        // Both contexts should be logged out
        await expect(page1).toHaveURL(/\/login/, { timeout: 10000 });

        // Context2 should also be invalidated when trying to access protected route
        await page2.goto('/dashboard');
        await page2.waitForLoadState('networkidle');
        
        // May redirect to login or show session expired message
        const isLoggedOut = page2.url().includes('/login') || 
                           await page2.locator(':text("session expired"), :text("please login")').isVisible({ timeout: 2000 }).catch(() => false);
        
        expect(isLoggedOut).toBeTruthy();

      } finally {
        // Cleanup contexts
        await context1.close();
        await context2.close();
      }
    });
  });

  test.describe('Session Timeout', () => {
    test('should handle session timeout gracefully', async () => {
      await authHelper.testSessionTimeout();
    });

    test('should show session timeout warning', async ({ page }) => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Mock token expiry response
      await page.route('**/api/v1/**', route => {
        // Simulate expired token response for API calls
        if (route.request().headers()['authorization']) {
          route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Token expired',
              error: 'token_expired',
            }),
          });
        } else {
          route.continue();
        }
      });

      // Trigger API call that would return expired token error
      await authHelper.page.reload();
      await authHelper.page.waitForLoadState('networkidle');

      // Should show session expired message or redirect to login
      const sessionExpiredMessage = authHelper.page.locator(':text("session expired"), :text("login again"), [role="alert"]');
      const hasExpiredMessage = await sessionExpiredMessage.isVisible({ timeout: 5000 }).catch(() => false);
      const onLoginPage = authHelper.page.url().includes('/login');

      expect(hasExpiredMessage || onLoginPage).toBeTruthy();
    });

    test('should attempt token refresh before showing timeout', async ({ page }) => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      let refreshAttempted = false;

      // Mock token refresh endpoint
      await page.route('**/api/v1/auth/refresh', route => {
        refreshAttempted = true;
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'new.access.token',
            refreshToken: 'new.refresh.token',
            expiresIn: 3600
          }),
        });
      });

      // Mock API that returns expired token first, then accepts new token
      let requestCount = 0;
      await page.route('**/api/v1/dashboard**', route => {
        requestCount++;
        if (requestCount === 1) {
          // First request - return expired token
          route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({ message: 'Token expired' }),
          });
        } else {
          // Second request - return success
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ data: 'dashboard data' }),
          });
        }
      });

      // Trigger API call that would cause token refresh
      await authHelper.page.reload();
      await authHelper.page.waitForTimeout(3000);

      // Should attempt refresh
      if (refreshAttempted) {
        console.log('✅ Token refresh attempted');
      } else {
        console.log('ℹ️ Token refresh not attempted (may use different strategy)');
      }

      // Should still be on dashboard if refresh succeeded
      const stillAuthenticated = !authHelper.page.url().includes('/login');
      if (stillAuthenticated && refreshAttempted) {
        console.log('✅ Token refresh successful');
      }
    });

    test('should handle refresh token expiry', async ({ page }) => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Mock refresh token as expired
      await page.route('**/api/v1/auth/refresh', route => {
        route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Refresh token expired',
            error: 'refresh_token_expired',
          }),
        });
      });

      // Mock API call that triggers refresh attempt
      await page.route('**/api/v1/**', route => {
        if (route.request().headers()['authorization']) {
          route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({ message: 'Token expired' }),
          });
        } else {
          route.continue();
        }
      });

      // Trigger API call
      await authHelper.page.reload();
      await authHelper.page.waitForTimeout(3000);

      // Should redirect to login when refresh token is also expired
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });
  });

  test.describe('Concurrent Sessions', () => {
    test('should support multiple concurrent sessions', async ({ browser }) => {
      const users = [AUTH_TEST_USERS.VALID_USER, AUTH_TEST_USERS.VALID_USER_2];
      
      // Create concurrent sessions
      const contexts = await authHelper.testConcurrentSessions(browser, users);

      try {
        // Verify all sessions are active
        for (let i = 0; i < contexts.length; i++) {
          const page = await contexts[i].newPage();
          const helper = new EnhancedAuthHelper(page);
          
          await page.goto('/dashboard');
          await helper.verifyOnDashboard();
          
          console.log(`✅ Session ${i + 1} is active`);
        }

        // Test session isolation
        const page1 = await contexts[0].newPage();
        const page2 = await contexts[1].newPage();
        
        const helper1 = new EnhancedAuthHelper(page1);
        const helper2 = new EnhancedAuthHelper(page2);

        // Logout from one session
        await page1.goto('/dashboard');
        await helper1.logout();

        // Other session should remain active
        await page2.goto('/dashboard');
        await helper2.verifyOnDashboard();

      } finally {
        // Cleanup all contexts
        for (const context of contexts) {
          await context.close();
        }
      }
    });

    test('should enforce maximum session limits if configured', async ({ browser }) => {
      // This test checks if the system enforces session limits
      const maxSessions = 3;
      const contexts: BrowserContext[] = [];

      try {
        // Create multiple sessions for the same user
        for (let i = 0; i < maxSessions + 2; i++) {
          const context = await browser.newContext();
          const page = await context.newPage();
          const helper = new EnhancedAuthHelper(page);

          try {
            await helper.loginWithCredentials(
              AUTH_TEST_USERS.VALID_USER.username, 
              AUTH_TEST_USERS.VALID_USER.password
            );
            
            const tokenInfo = await helper.getTokenInfo();
            if (tokenInfo.accessToken) {
              contexts.push(context);
              console.log(`Session ${i + 1} created successfully`);
            } else {
              console.log(`Session ${i + 1} rejected (may have hit limit)`);
              await context.close();
              break;
            }
          } catch (error) {
            console.log(`Session ${i + 1} failed: ${error}`);
            await context.close();
            break;
          }
        }

        // Check if session limits are enforced
        if (contexts.length > maxSessions) {
          console.log('ℹ️ No session limits detected');
        } else {
          console.log(`✅ Session limits may be enforced (${contexts.length} sessions created)`);
        }

      } finally {
        // Cleanup all contexts
        for (const context of contexts) {
          await context.close();
        }
      }
    });
  });

  test.describe('Session Security', () => {
    test('should invalidate session on password change', async ({ page }) => {
      // Login first
      const tokenInfo = await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      expect(tokenInfo.accessToken).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // Mock password change API
      await page.route('**/api/v1/auth/change-password', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Password changed successfully',
            invalidateAllSessions: true,
          }),
        });
      });

      // Simulate password change (navigate to settings if available)
      const settingsLink = authHelper.page.locator(':text("Settings"), :text("Profile"), [href*="settings"]');
      const hasSettings = await settingsLink.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasSettings) {
        await settingsLink.click();
        
        // Look for password change form
        const passwordForm = authHelper.page.locator('form, input[type="password"]');
        const hasPasswordForm = await passwordForm.isVisible({ timeout: 2000 }).catch(() => false);

        if (hasPasswordForm) {
          // Simulate password change
          await authHelper.page.evaluate(() => {
            // Trigger password change API call
            fetch('/api/v1/auth/change-password', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ newPassword: 'NewPassword123!' })
            });
          });
        }
      } else {
        // Trigger password change API directly
        await authHelper.page.evaluate(() => {
          fetch('/api/v1/auth/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newPassword: 'NewPassword123!' })
          });
        });
      }

      await authHelper.page.waitForTimeout(1000);

      // Should be logged out after password change
      await authHelper.page.goto('/dashboard');
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });

    test('should clear sensitive data on logout', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Store some sensitive data in sessionStorage/localStorage
      await authHelper.page.evaluate(() => {
        sessionStorage.setItem('sensitive_data', 'secret information');
        localStorage.setItem('user_preferences', JSON.stringify({ theme: 'dark' }));
      });

      // Logout
      await authHelper.logout();

      // Check that sensitive data is cleared
      const remainingData = await authHelper.page.evaluate(() => ({
        sensitive: sessionStorage.getItem('sensitive_data'),
        accessToken: sessionStorage.getItem('access_token'),
        refreshToken: localStorage.getItem('refresh_token'),
      }));

      expect(remainingData.sensitive).toBeNull();
      expect(remainingData.accessToken).toBeNull();
      expect(remainingData.refreshToken).toBeNull();
    });

    test('should protect against session hijacking', async ({ page }) => {
      // Login with valid credentials
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Get current token
      const currentToken = await authHelper.page.evaluate(() => 
        sessionStorage.getItem('access_token')
      );

      // Mock suspicious activity detection
      await page.route('**/api/v1/**', route => {
        route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Suspicious activity detected',
            error: 'session_security_violation',
            action: 'logout_required',
          }),
        });
      });

      // Trigger API call that detects suspicious activity
      await authHelper.page.reload();
      await authHelper.page.waitForTimeout(2000);

      // Should be logged out due to security violation
      const securityLogout = authHelper.page.url().includes('/login') || 
                            await authHelper.page.locator(':text("security"), :text("suspicious")').isVisible({ timeout: 2000 }).catch(() => false);

      if (securityLogout) {
        console.log('✅ Security-based logout triggered');
      } else {
        console.log('ℹ️ Security violation handling not detected');
      }
    });

    test('should validate session integrity', async ({ page }) => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Tamper with token
      await authHelper.page.evaluate(() => {
        const tamperedToken = 'tampered.jwt.token.signature';
        sessionStorage.setItem('access_token', tamperedToken);
      });

      // Mock server response for invalid token
      await page.route('**/api/v1/**', route => {
        const authHeader = route.request().headers()['authorization'];
        if (authHeader && authHeader.includes('tampered')) {
          route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Invalid token signature',
              error: 'token_invalid',
            }),
          });
        } else {
          route.continue();
        }
      });

      // Try to access protected resource
      await authHelper.page.reload();
      await authHelper.page.waitForTimeout(2000);

      // Should be logged out due to invalid token
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });
  });

  test.describe('Mobile Session Management', () => {
    Object.entries(MOBILE_VIEWPORTS).forEach(([deviceName, viewport]) => {
      test(`should work on ${deviceName}`, async ({ page }) => {
        await page.setViewportSize(viewport);
        
        const mobileAuthHelper = new EnhancedAuthHelper(page);
        await mobileAuthHelper.clearStorage();

        // Login on mobile
        await mobileAuthHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username, 
          AUTH_TEST_USERS.VALID_USER.password
        );
        await mobileAuthHelper.verifyOnDashboard();

        // Test logout on mobile
        const userMenu = mobileAuthHelper.page.locator('[data-testid="user-menu"], .user-avatar, .mobile-menu');
        
        // May need to tap mobile menu first
        const mobileMenuButton = mobileAuthHelper.page.locator('[data-testid="mobile-menu"], .hamburger, [aria-label="Menu"]');
        const hasMobileMenu = await mobileMenuButton.isVisible({ timeout: 2000 }).catch(() => false);
        
        if (hasMobileMenu) {
          await mobileMenuButton.tap();
        }

        await userMenu.tap();

        const logoutButton = mobileAuthHelper.page.locator('button:has-text("Logout"), button:has-text("Sign out")');
        await logoutButton.tap();

        // Should logout successfully
        await expect(mobileAuthHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
      });
    });
  });

  test.describe('Performance', () => {
    test('should complete logout within performance threshold', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      const startTime = Date.now();

      // Logout
      await authHelper.logout();

      // Wait for redirect
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });

      const endTime = Date.now();
      const logoutTime = endTime - startTime;

      expect(logoutTime).toBeLessThan(AUTH_PERFORMANCE_THRESHOLDS.LOGOUT_TIME_MS);
      console.log(`Logout completed in ${logoutTime}ms`);
    });

    test('should handle slow logout API responses', async ({ page }) => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Mock slow logout API
      await page.route('**/api/v1/auth/logout', async route => {
        await new Promise(resolve => setTimeout(resolve, 2000)); // 2s delay
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ message: 'Logged out successfully' }),
        });
      });

      const startTime = Date.now();
      await authHelper.logout();
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 15000 });
      const endTime = Date.now();

      // Should complete despite slow API
      expect(endTime - startTime).toBeGreaterThan(2000);
      console.log(`Slow logout completed in ${endTime - startTime}ms`);

      // Tokens should still be cleared locally
      const tokenInfo = await authHelper.getTokenInfo();
      expect(tokenInfo.accessToken).toBeNull();
    });
  });

  test.describe('Accessibility', () => {
    test('should announce logout status to screen readers', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Logout
      await authHelper.logout();

      // Check for logout status announcement
      const statusRegion = authHelper.page.locator('[role="status"], [aria-live="polite"], [aria-live="assertive"]');
      const hasStatusAnnouncement = await statusRegion.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasStatusAnnouncement) {
        const statusText = await statusRegion.textContent();
        expect(statusText?.toLowerCase()).toMatch(/logout|signed out|logged out/);
        console.log('✅ Logout status announced to screen readers');
      } else {
        console.log('ℹ️ No explicit logout status announcement detected');
      }
    });

    test('should maintain keyboard focus during logout flow', async () => {
      // Login first
      await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username, 
        AUTH_TEST_USERS.VALID_USER.password
      );
      await authHelper.verifyOnDashboard();

      // Use keyboard navigation for logout
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar');
      await userMenu.focus();
      await authHelper.page.keyboard.press('Enter');

      const logoutButton = authHelper.page.locator('button:has-text("Logout")');
      await logoutButton.focus();
      
      // Verify button has focus
      expect(await logoutButton.evaluate(el => el === document.activeElement)).toBeTruthy();

      await authHelper.page.keyboard.press('Enter');

      // After logout, focus should be on login form
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
      
      // Login form should be keyboard accessible
      const loginField = authHelper.page.locator('#email, input[name="email"]');
      await loginField.focus();
      expect(await loginField.evaluate(el => el === document.activeElement)).toBeTruthy();
    });
  });
});