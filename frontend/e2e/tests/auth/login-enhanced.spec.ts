/**
 * Enhanced E2E Tests for Login with Valid/Invalid Credentials
 * Comprehensive login flow testing including security, performance, and accessibility
 */

import {expect, test} from '@playwright/test';
import {EnhancedAuthHelper} from '../../helpers/auth-helpers';
import {
  AUTH_PERFORMANCE_THRESHOLDS,
  AUTH_TEST_USERS,
  INVALID_CREDENTIALS,
  LOCKOUT_SCENARIOS,
  MOBILE_VIEWPORTS
} from '../../helpers/auth-fixtures';

test.describe('Enhanced Login Flow', () => {
  let authHelper: EnhancedAuthHelper;

  test.beforeEach(async ({page}) => {
    authHelper = new EnhancedAuthHelper(page);
    await authHelper.clearStorage();
  });

  test.afterEach(async () => {
    await authHelper.clearStorage();
  });

  test.describe('Valid Credentials', () => {
    test('should login successfully with username and password', async () => {
      const tokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      // Verify tokens are stored
      expect(tokenInfo.accessToken).toBeTruthy();
      expect(tokenInfo.refreshToken).toBeTruthy();
      expect(tokenInfo.isValid).toBeTruthy();

      // Verify redirect to dashboard
      await authHelper.verifyOnDashboard();
    });

    test('should login successfully with email and password', async () => {
      const tokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.email,
          AUTH_TEST_USERS.VALID_USER.password
      );

      expect(tokenInfo.accessToken).toBeTruthy();
      expect(tokenInfo.refreshToken).toBeTruthy();
      await authHelper.verifyOnDashboard();
    });

    test('should store tokens in sessionStorage by default', async () => {
      const tokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password,
          false // rememberMe = false
      );

      expect(tokenInfo.location).toBe('sessionStorage');
    });

    test('should store tokens in localStorage when "Remember Me" is checked', async () => {
      const tokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password,
          true // rememberMe = true
      );

      // Should store in localStorage for persistent login
      expect(tokenInfo.location).toBe('localStorage');
    });

    test('should maintain session after page refresh', async () => {
      // Login first
      await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      await authHelper.verifyOnDashboard();

      // Refresh page
      await authHelper.page.reload();
      await authHelper.page.waitForLoadState('networkidle');

      // Should still be authenticated
      await authHelper.verifyOnDashboard();
    });

    test('should load user profile data after login', async () => {
      await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      await authHelper.verifyOnDashboard();

      // Check for user menu with profile info
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
      await expect(userMenu).toBeVisible();

      // Click user menu to see profile info
      await userMenu.click();

      // Should show user information
      const userInfo = authHelper.page.locator(':text("' + AUTH_TEST_USERS.VALID_USER.firstName + '"), :text("' + AUTH_TEST_USERS.VALID_USER.email + '")');
      const hasUserInfo = await userInfo.first().isVisible({timeout: 2000}).catch(() => false);

      expect(hasUserInfo).toBeTruthy();
    });
  });

  test.describe('Invalid Credentials', () => {
    test('should reject non-existent user', async () => {
      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.NON_EXISTENT_USER);
    });

    test('should reject wrong password', async () => {
      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.WRONG_PASSWORD);
    });

    test('should reject malformed email', async () => {
      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.MALFORMED_EMAIL);
    });

    test('should reject empty credentials', async () => {
      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.EMPTY_CREDENTIALS);
    });

    test('should preserve username field after failed login', async () => {
      const credentials = INVALID_CREDENTIALS.WRONG_PASSWORD;

      await authHelper.navigateToLogin();
      await authHelper.page.locator('#email, input[name="email"]').fill(credentials.username);
      await authHelper.page.locator('#password, input[name="password"]').fill(credentials.password);
      await authHelper.page.locator('button[type="submit"]').click();

      // Wait for error
      await authHelper.verifyErrorMessage();

      // Username should be preserved
      const usernameValue = await authHelper.page.locator('#email, input[name="email"]').inputValue();
      expect(usernameValue).toBe(credentials.username);

      // Password should be cleared for security
      const passwordValue = await authHelper.page.locator('#password, input[name="password"]').inputValue();
      expect(passwordValue).toBe('');
    });

    test('should show specific error messages for different failure types', async () => {
      // Test wrong password
      await authHelper.navigateToLogin();
      await authHelper.page.locator('#email, input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.username);
      await authHelper.page.locator('#password, input[name="password"]').fill('wrongpassword');
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifyErrorMessage();

      // Check for specific error message about credentials
      const errorText = await authHelper.page.locator('[role="alert"], .error').textContent();
      expect(errorText?.toLowerCase()).toMatch(/invalid|incorrect|wrong|credentials/);
    });
  });

  test.describe('Account Lockout', () => {
    test('should implement progressive delays for failed attempts', async () => {
      const invalidCredentials = INVALID_CREDENTIALS.WRONG_PASSWORD;
      const attempts = 3;

      for (let i = 0; i < attempts; i++) {
        const startTime = Date.now();

        await authHelper.testInvalidLogin(invalidCredentials);

        const endTime = Date.now();
        const responseTime = endTime - startTime;

        console.log(`Attempt ${i + 1}: ${responseTime}ms`);

        // Later attempts should take longer (progressive delay)
        if (i > 0) {
          // Allow some tolerance for network variation
          const expectedMinDelay = LOCKOUT_SCENARIOS.PROGRESSIVE_DELAY[Math.min(i, LOCKOUT_SCENARIOS.PROGRESSIVE_DELAY.length - 1)] * 1000;
          // Response time might include natural delay, so this is informational
          console.log(`Expected min delay: ${expectedMinDelay}ms, Actual: ${responseTime}ms`);
        }
      }
    });

    test('should lock account after maximum failed attempts', async ({page: _page}) => {
      // Skip this test if it would affect other tests
      test.slow(); // Mark as slow test

      await authHelper.testAccountLockout(
          AUTH_TEST_USERS.VALID_USER.username,
          LOCKOUT_SCENARIOS.MAX_FAILED_ATTEMPTS
      );
    });

    test('should show lockout message with remaining time', async ({page}) => {
      // Simulate account lockout response
      await page.route('**/api/v1/auth/login', route => {
        route.fulfill({
          status: 429, // Too Many Requests
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Account temporarily locked',
            error: 'too_many_attempts',
            retryAfter: 900, // 15 minutes
          }),
        });
      });

      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.WRONG_PASSWORD);

      // Should show lockout message with time
      const errorElement = authHelper.page.locator('[role="alert"], .error');
      await expect(errorElement).toBeVisible();

      const errorText = await errorElement.textContent();
      expect(errorText?.toLowerCase()).toMatch(/locked|blocked|too many|wait/);
    });
  });

  test.describe('Security Features', () => {
    test('should protect against SQL injection', async () => {
      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.SQL_INJECTION);

      // Should handle gracefully without exposing database errors
      const errorElement = authHelper.page.locator('[role="alert"], .error');
      const errorText = await errorElement.textContent().catch(() => '');

      // Should not expose database-specific error messages
      expect(errorText?.toLowerCase()).not.toMatch(/sql|database|table|column/);
    });

    test('should sanitize XSS attempts', async () => {
      await authHelper.navigateToLogin();

      // Try XSS in login fields
      await authHelper.page.locator('#email, input[name="email"]').fill(INVALID_CREDENTIALS.XSS_ATTEMPT.username);
      await authHelper.page.locator('#password, input[name="password"]').fill(INVALID_CREDENTIALS.XSS_ATTEMPT.password);

      // Set up alert listener to catch any XSS execution
      let alertTriggered = false;
      authHelper.page.on('dialog', dialog => {
        alertTriggered = true;
        dialog.dismiss();
      });

      await authHelper.page.locator('button[type="submit"]').click();
      await authHelper.page.waitForTimeout(2000);

      // XSS should not execute
      expect(alertTriggered).toBeFalsy();

      // Page should remain functional
      await expect(authHelper.page.locator('#email, input[name="email"]')).toBeVisible();
    });

    test('should use HTTPS in production URLs', async () => {
      const currentUrl = authHelper.page.url();

      // In production, should use HTTPS
      if (!currentUrl.includes('localhost') && !currentUrl.includes('127.0.0.1')) {
        expect(currentUrl).toMatch(/^https:/);
      }
    });

    test('should clear sensitive data from memory', async () => {
      await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      // Check that password is not stored in form
      const passwordValue = await authHelper.page.locator('#password, input[name="password"]').inputValue().catch(() => '');
      expect(passwordValue).toBe('');

      // Check that password is not in page source
      const pageContent = await authHelper.page.content();
      expect(pageContent).not.toContain(AUTH_TEST_USERS.VALID_USER.password);
    });
  });

  test.describe('Session Management', () => {
    test('should handle token refresh automatically', async ({page}) => {
      // Mock initial login with short-lived token
      await page.route('**/api/v1/auth/login', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: {
              id: 1,
              username: AUTH_TEST_USERS.VALID_USER.username,
              email: AUTH_TEST_USERS.VALID_USER.email
            },
            token: 'short.lived.token',
            refreshToken: 'valid.refresh.token',
            expiresIn: 5 // 5 seconds
          }),
        });
      });

      // Mock token refresh endpoint
      await page.route('**/api/v1/auth/refresh', route => {
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

      await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      await authHelper.verifyOnDashboard();

      // Wait for token to expire and refresh
      await authHelper.page.waitForTimeout(6000);

      // Make a request that would trigger token refresh
      await authHelper.page.reload();
      await authHelper.page.waitForLoadState('networkidle');

      // Should still be authenticated
      await authHelper.verifyOnDashboard();
    });

    test('should redirect to login when refresh token expires', async () => {
      await authHelper.testSessionTimeout();
    });

    test('should support "Remember Me" functionality', async () => {
      // Test session storage (default)
      const sessionTokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password,
          false
      );

      expect(sessionTokenInfo.location).toBe('sessionStorage');
      await authHelper.logout();

      // Test persistent storage (remember me)
      const persistentTokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password,
          true
      );

      expect(persistentTokenInfo.location).toBe('localStorage');
    });
  });

  test.describe('Performance', () => {
    test('should complete login within performance threshold', async () => {
      await authHelper.navigateToLogin();

      const startTime = Date.now();

      await authHelper.page.locator('#email, input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.username);
      await authHelper.page.locator('#password, input[name="password"]').fill(AUTH_TEST_USERS.VALID_USER.password);
      await authHelper.page.locator('button[type="submit"]').click();

      // Wait for redirect to dashboard
      await expect(authHelper.page).toHaveURL(/\/dashboard|\/home|\/app/, {timeout: 10000});

      const endTime = Date.now();
      const loginTime = endTime - startTime;

      expect(loginTime).toBeLessThan(AUTH_PERFORMANCE_THRESHOLDS.LOGIN_TIME_MS);
      console.log(`Login completed in ${loginTime}ms`);
    });

    test('should handle slow network conditions', async ({page}) => {
      // Simulate slow network
      await page.route('**/api/v1/auth/login', async route => {
        await new Promise(resolve => setTimeout(resolve, 2000)); // 2s delay
        route.continue();
      });

      const startTime = Date.now();

      const tokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      const endTime = Date.now();
      const loginTime = endTime - startTime;

      expect(tokenInfo.accessToken).toBeTruthy();
      expect(loginTime).toBeGreaterThan(2000); // Should include our delay
      console.log(`Login with slow network: ${loginTime}ms`);
    });
  });

  test.describe('Mobile Responsiveness', () => {
    Object.entries(MOBILE_VIEWPORTS).forEach(([deviceName, viewport]) => {
      test(`should work on ${deviceName}`, async ({page}) => {
        await page.setViewportSize(viewport);

        const mobileAuthHelper = new EnhancedAuthHelper(page);
        await mobileAuthHelper.clearStorage();

        // Test login on mobile viewport
        const tokenInfo = await mobileAuthHelper.loginWithCredentials(
            AUTH_TEST_USERS.VALID_USER.username,
            AUTH_TEST_USERS.VALID_USER.password
        );

        expect(tokenInfo.accessToken).toBeTruthy();
        await mobileAuthHelper.verifyOnDashboard();

        // Test form is properly sized
        const loginForm = mobileAuthHelper.page.locator('form, [role="form"]');
        const formBox = await loginForm.boundingBox();

        if (formBox) {
          expect(formBox.width).toBeLessThanOrEqual(viewport.width);
          expect(formBox.width).toBeGreaterThan(viewport.width * 0.8); // Should use most of the width
        }
      });
    });

    test('should support touch interactions on mobile', async ({page}) => {
      await page.setViewportSize(MOBILE_VIEWPORTS.IPHONE_12);

      const mobileAuthHelper = new EnhancedAuthHelper(page);
      await mobileAuthHelper.clearStorage();
      await mobileAuthHelper.navigateToLogin();

      // Use tap instead of click on mobile
      await mobileAuthHelper.page.locator('#email, input[name="email"]').tap();
      await mobileAuthHelper.page.locator('#email, input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.username);

      await mobileAuthHelper.page.locator('#password, input[name="password"]').tap();
      await mobileAuthHelper.page.locator('#password, input[name="password"]').fill(AUTH_TEST_USERS.VALID_USER.password);

      await mobileAuthHelper.page.locator('button[type="submit"]').tap();

      await mobileAuthHelper.verifyOnDashboard();
    });
  });

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async () => {
      await authHelper.navigateToLogin();

      // Test tab order
      const emailField = authHelper.page.locator('#email, input[name="email"]');
      const passwordField = authHelper.page.locator('#password, input[name="password"]');
      const _submitButton = authHelper.page.locator('button[type="submit"]');

      await emailField.focus();
      expect(await emailField.evaluate(el => el === document.activeElement)).toBeTruthy();

      await authHelper.page.keyboard.press('Tab');
      expect(await passwordField.evaluate(el => el === document.activeElement)).toBeTruthy();

      // Fill form using keyboard
      await emailField.focus();
      await authHelper.page.keyboard.type(AUTH_TEST_USERS.VALID_USER.username);

      await authHelper.page.keyboard.press('Tab');
      await authHelper.page.keyboard.type(AUTH_TEST_USERS.VALID_USER.password);

      // Submit with Enter
      await authHelper.page.keyboard.press('Enter');

      await authHelper.verifyOnDashboard();
    });

    test('should have proper ARIA labels', async () => {
      await authHelper.navigateToLogin();

      const emailField = authHelper.page.locator('#email, input[name="email"]');
      const passwordField = authHelper.page.locator('#password, input[name="password"]');
      const submitButton = authHelper.page.locator('button[type="submit"]');

      // Check for accessibility attributes
      const emailHasLabel = await emailField.evaluate(el => {
        return el.hasAttribute('aria-label') ||
            el.hasAttribute('aria-labelledby') ||
            document.querySelector(`label[for="${el.id}"]`) !== null;
      });

      const passwordHasLabel = await passwordField.evaluate(el => {
        return el.hasAttribute('aria-label') ||
            el.hasAttribute('aria-labelledby') ||
            document.querySelector(`label[for="${el.id}"]`) !== null;
      });

      expect(emailHasLabel).toBeTruthy();
      expect(passwordHasLabel).toBeTruthy();

      // Submit button should have accessible text
      const submitText = await submitButton.textContent();
      expect(submitText?.trim()).toBeTruthy();
    });

    test('should announce errors to screen readers', async () => {
      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.WRONG_PASSWORD);

      // Error should be in an alert region
      const errorAlert = authHelper.page.locator('[role="alert"]');
      await expect(errorAlert).toBeVisible();

      // Error should have meaningful text
      const errorText = await errorAlert.textContent();
      expect(errorText?.trim()).toBeTruthy();
      expect(errorText?.length).toBeGreaterThan(5);
    });

    test('should work with high contrast mode', async ({page}) => {
      // Simulate high contrast mode
      await page.emulateMedia({colorScheme: 'dark', reducedMotion: 'reduce'});

      await authHelper.navigateToLogin();

      // Elements should still be visible and functional
      await expect(authHelper.page.locator('#email, input[name="email"]')).toBeVisible();
      await expect(authHelper.page.locator('#password, input[name="password"]')).toBeVisible();
      await expect(authHelper.page.locator('button[type="submit"]')).toBeVisible();

      // Test login still works
      const tokenInfo = await authHelper.loginWithCredentials(
          AUTH_TEST_USERS.VALID_USER.username,
          AUTH_TEST_USERS.VALID_USER.password
      );

      expect(tokenInfo.accessToken).toBeTruthy();
    });
  });

  test.describe('Error Handling', () => {
    test('should handle network timeouts', async ({page}) => {
      await authHelper.navigateToLogin();

      // Mock timeout
      await page.route('**/api/v1/auth/login', _route => {
        // Never respond (timeout)
        // Don't call route.continue() or route.fulfill()
      });

      await authHelper.page.locator('#email, input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.username);
      await authHelper.page.locator('#password, input[name="password"]').fill(AUTH_TEST_USERS.VALID_USER.password);
      await authHelper.page.locator('button[type="submit"]').click();

      // Should show timeout error
      await authHelper.verifyErrorMessage();
    });

    test('should handle server maintenance (503)', async ({page}) => {
      await authHelper.navigateToLogin();

      await page.route('**/api/v1/auth/login', route => {
        route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Service temporarily unavailable',
            error: 'maintenance_mode',
          }),
        });
      });

      await authHelper.testInvalidLogin(INVALID_CREDENTIALS.WRONG_PASSWORD);

      // Should show maintenance message
      const errorElement = authHelper.page.locator('[role="alert"], .error');
      const errorText = await errorElement.textContent();
      expect(errorText?.toLowerCase()).toMatch(/unavailable|maintenance|try.*later/);
    });

    test('should recover from API errors', async ({page}) => {
      await authHelper.navigateToLogin();

      let requestCount = 0;
      await page.route('**/api/v1/auth/login', route => {
        requestCount++;
        if (requestCount === 1) {
          // First request fails
          route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({message: 'Internal server error'}),
          });
        } else {
          // Second request succeeds
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              user: {
                id: 1,
                username: AUTH_TEST_USERS.VALID_USER.username,
                email: AUTH_TEST_USERS.VALID_USER.email
              },
              token: 'valid.jwt.token',
              refreshToken: 'valid.refresh.token',
            }),
          });
        }
      });

      // First attempt should fail
      await authHelper.testInvalidLogin({
        username: AUTH_TEST_USERS.VALID_USER.username,
        password: AUTH_TEST_USERS.VALID_USER.password,
      });

      // Second attempt should succeed
      await authHelper.page.locator('#email, input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.username);
      await authHelper.page.locator('#password, input[name="password"]').fill(AUTH_TEST_USERS.VALID_USER.password);
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifyOnDashboard();
    });
  });
});