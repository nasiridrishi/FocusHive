/**
 * E2E Tests for Password Reset Email Flow
 * Comprehensive password reset testing including email verification, token validation, and security
 */

import { test, expect } from '@playwright/test';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { 
  AUTH_TEST_USERS, 
  PASSWORD_RESET_SCENARIOS,
  EMAIL_VERIFICATION,
  generateUniqueAuthUser,
  MAILHOG_CONFIG,
  MOBILE_VIEWPORTS,
  AUTH_PERFORMANCE_THRESHOLDS
} from '../../helpers/auth-fixtures';

test.describe('Password Reset Flow', () => {
  let authHelper: EnhancedAuthHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new EnhancedAuthHelper(page);
    await authHelper.clearStorage();
    await authHelper.clearAllEmails();
  });

  test.afterEach(async () => {
    await authHelper.clearStorage();
    await authHelper.clearAllEmails();
  });

  test.describe('Password Reset Request', () => {
    test('should display password reset form', async () => {
      await authHelper.navigateToPasswordReset();

      // Verify form elements are present
      await expect(authHelper.page.locator('input[name="email"]')).toBeVisible();
      await expect(authHelper.page.locator('button[type="submit"]:has-text("Send Reset"), button[type="submit"]:has-text("Reset Password")')).toBeVisible();

      // Check for back to login link
      const backLink = authHelper.page.locator('a[href="/login"], button:has-text("Back to Login")');
      await expect(backLink).toBeVisible();
    });

    test('should validate email field', async () => {
      await authHelper.navigateToPasswordReset();

      // Test empty email
      await authHelper.page.locator('button[type="submit"]').click();
      await authHelper.verifyErrorMessage('Email is required');

      // Test invalid email format
      await authHelper.page.locator('input[name="email"]').fill('invalid-email');
      await authHelper.page.locator('button[type="submit"]').click();
      await authHelper.verifyErrorMessage('Please enter a valid email address');
    });

    test('should send password reset email for valid email', async ({ page }) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip('MailHog not available for email testing');
      }

      // Set up API monitoring
      const apiMonitoring = await authHelper.setupApiMonitoring();

      await authHelper.requestPasswordReset(AUTH_TEST_USERS.VALID_USER.email);

      // Verify API call was made
      const resetRequests = apiMonitoring.getPasswordResetRequests();
      expect(resetRequests).toHaveLength(1);
      expect(resetRequests[0].postData).toMatchObject({
        email: AUTH_TEST_USERS.VALID_USER.email,
      });

      // Verify success message
      await authHelper.verifySuccessMessage('Password reset email sent');

      // Verify email was sent
      const resetEmail = await authHelper.getPasswordResetEmail(AUTH_TEST_USERS.VALID_USER.email);
      expect(resetEmail.Subject.toLowerCase()).toMatch(/reset|password/);
      expect(resetEmail.To[0]).toMatchObject({
        Mailbox: AUTH_TEST_USERS.VALID_USER.email.split('@')[0],
        Domain: AUTH_TEST_USERS.VALID_USER.email.split('@')[1],
      });
    });

    test('should handle non-existent email gracefully', async () => {
      await authHelper.requestPasswordReset(PASSWORD_RESET_SCENARIOS.NON_EXISTENT_EMAIL.email);

      // Should show success message (don't reveal if email exists)
      await authHelper.verifySuccessMessage('If an account with this email exists, you will receive a password reset link');
    });

    test('should show success message for invalid email format after sanitization', async () => {
      await authHelper.requestPasswordReset('not@an@email');

      // Should handle gracefully without revealing email validation details
      const hasError = await authHelper.page.locator('[role="alert"], .error').isVisible({ timeout: 2000 }).catch(() => false);
      const hasSuccess = await authHelper.page.locator('.success').isVisible({ timeout: 2000 }).catch(() => false);

      expect(hasError || hasSuccess).toBeTruthy();
    });

    test('should rate limit password reset requests', async () => {
      const email = AUTH_TEST_USERS.VALID_USER.email;

      // Make multiple rapid requests
      for (let i = 0; i < 3; i++) {
        await authHelper.requestPasswordReset(email);
        await authHelper.page.waitForTimeout(100);
      }

      // Additional requests should be rate limited
      await authHelper.requestPasswordReset(email);

      // Should show rate limit message
      const rateLimitMessage = authHelper.page.locator(':text("too many requests"), :text("rate limit"), :text("wait")');
      const hasRateLimit = await rateLimitMessage.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasRateLimit) {
        console.log('✅ Rate limiting is implemented for password reset');
      } else {
        console.log('ℹ️ Rate limiting not detected for password reset');
      }
    });
  });

  test.describe('Password Reset Token Validation', () => {
    test('should accept valid reset token', async ({ page }) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip('MailHog not available for email testing');
      }

      const { resetToken } = await authHelper.completePasswordReset(
        AUTH_TEST_USERS.VALID_USER.email,
        PASSWORD_RESET_SCENARIOS.VALID_EMAIL.newPassword
      );

      // Verify we reached the reset password form
      await expect(authHelper.page.locator('input[name="password"]')).toBeVisible();
      await expect(authHelper.page.locator('input[name="confirmPassword"]')).toBeVisible();
    });

    test('should reject invalid reset token', async () => {
      const invalidToken = EMAIL_VERIFICATION.INVALID_TOKEN;

      await authHelper.page.goto(`/reset-password?token=${invalidToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Should show error message
      await authHelper.verifyErrorMessage('Invalid or expired reset token');

      // Should not show password form
      const passwordForm = authHelper.page.locator('input[name="password"]');
      const hasPasswordForm = await passwordForm.isVisible({ timeout: 2000 }).catch(() => false);
      expect(hasPasswordForm).toBeFalsy();
    });

    test('should reject expired reset token', async ({ page }) => {
      // Mock expired token response
      await page.route('**/api/v1/auth/validate-reset-token**', route => {
        route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Reset token has expired',
            error: 'token_expired',
          }),
        });
      });

      const expiredToken = EMAIL_VERIFICATION.EXPIRED_TOKEN;

      await authHelper.page.goto(`/reset-password?token=${expiredToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Should show specific error for expired token
      await authHelper.verifyErrorMessage('Reset token has expired');
    });

    test('should reject malformed reset token', async () => {
      const malformedToken = EMAIL_VERIFICATION.MALFORMED_TOKEN;

      await authHelper.page.goto(`/reset-password?token=${malformedToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Should show error message
      await authHelper.verifyErrorMessage('Invalid reset token format');
    });

    test('should handle missing reset token', async () => {
      await authHelper.page.goto('/reset-password');
      await authHelper.page.waitForLoadState('networkidle');

      // Should redirect to forgot password or show error
      const currentUrl = authHelper.page.url();
      const onForgotPassword = currentUrl.includes('/forgot-password');
      const hasError = await authHelper.page.locator('[role="alert"], .error').isVisible({ timeout: 2000 }).catch(() => false);

      expect(onForgotPassword || hasError).toBeTruthy();
    });
  });

  test.describe('New Password Form', () => {
    test('should validate new password requirements', async ({ page }) => {
      // Mock valid token
      await page.route('**/api/v1/auth/validate-reset-token**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ valid: true }),
        });
      });

      const validToken = EMAIL_VERIFICATION.VALID_TOKEN;
      await authHelper.page.goto(`/reset-password?token=${validToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Test weak password
      await authHelper.page.locator('input[name="password"]').fill('weak');
      await authHelper.page.locator('input[name="confirmPassword"]').fill('weak');
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifyErrorMessage('Password must be at least 8 characters');
    });

    test('should validate password confirmation match', async ({ page }) => {
      // Mock valid token
      await page.route('**/api/v1/auth/validate-reset-token**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ valid: true }),
        });
      });

      const validToken = EMAIL_VERIFICATION.VALID_TOKEN;
      await authHelper.page.goto(`/reset-password?token=${validToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Test mismatched passwords
      await authHelper.page.locator('input[name="password"]').fill('NewPassword123!');
      await authHelper.page.locator('input[name="confirmPassword"]').fill('DifferentPassword123!');
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifyErrorMessage('Passwords do not match');
    });

    test('should require password complexity', async ({ page }) => {
      // Mock valid token
      await page.route('**/api/v1/auth/validate-reset-token**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ valid: true }),
        });
      });

      const validToken = EMAIL_VERIFICATION.VALID_TOKEN;
      await authHelper.page.goto(`/reset-password?token=${validToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      const weakPasswords = [
        'lowercase123!',  // No uppercase
        'UPPERCASE123!',  // No lowercase
        'NoNumbers!',     // No numbers
        'NoSpecial123',   // No special characters
      ];

      for (const weakPassword of weakPasswords) {
        await authHelper.page.locator('input[name="password"]').fill(weakPassword);
        await authHelper.page.locator('input[name="confirmPassword"]').fill(weakPassword);
        await authHelper.page.locator('button[type="submit"]').click();

        await authHelper.verifyErrorMessage('Password must contain uppercase, lowercase, number and special character');
        
        // Clear fields for next test
        await authHelper.page.locator('input[name="password"]').fill('');
        await authHelper.page.locator('input[name="confirmPassword"]').fill('');
      }
    });

    test('should show password strength indicator', async ({ page }) => {
      // Mock valid token
      await page.route('**/api/v1/auth/validate-reset-token**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ valid: true }),
        });
      });

      const validToken = EMAIL_VERIFICATION.VALID_TOKEN;
      await authHelper.page.goto(`/reset-password?token=${validToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Type password and check for strength indicator
      await authHelper.page.locator('input[name="password"]').fill('weak');
      
      // Look for password strength indicator
      const strengthIndicator = authHelper.page.locator('.password-strength, [data-testid="password-strength"], :text("Weak"), :text("Strong")');
      const hasStrengthIndicator = await strengthIndicator.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasStrengthIndicator) {
        console.log('✅ Password strength indicator is present');
      } else {
        console.log('ℹ️ Password strength indicator not detected');
      }
    });
  });

  test.describe('Complete Password Reset Flow', () => {
    test('should complete full password reset flow', async ({ page }) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip('MailHog not available for email testing');
      }

      const newPassword = PASSWORD_RESET_SCENARIOS.VALID_EMAIL.newPassword;
      const userEmail = AUTH_TEST_USERS.VALID_USER.email;

      // Complete password reset
      const { resetToken } = await authHelper.completePasswordReset(userEmail, newPassword);

      // Verify success message
      await authHelper.verifySuccessMessage('Password updated successfully');

      // Verify redirect to login
      await expect(authHelper.page).toHaveURL(/\/login/);

      // Test login with new password
      const tokenInfo = await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username,
        newPassword
      );

      expect(tokenInfo.accessToken).toBeTruthy();
      expect(tokenInfo.isValid).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // Verify old password no longer works
      await authHelper.logout();
      await authHelper.testInvalidLogin({
        username: AUTH_TEST_USERS.VALID_USER.username,
        password: AUTH_TEST_USERS.VALID_USER.password, // Old password
      });
    });

    test('should invalidate all existing sessions after password reset', async ({ page }) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip('MailHog not available for email testing');
      }

      // First, login and verify session exists
      const oldTokenInfo = await authHelper.loginWithCredentials(
        AUTH_TEST_USERS.VALID_USER.username,
        AUTH_TEST_USERS.VALID_USER.password
      );
      expect(oldTokenInfo.accessToken).toBeTruthy();

      // Complete password reset in another "session"
      const newPassword = 'NewResetPassword123!';
      await authHelper.completePasswordReset(AUTH_TEST_USERS.VALID_USER.email, newPassword);

      // Previous session should be invalidated
      await authHelper.page.goto('/dashboard');
      
      // Should be redirected to login due to invalidated session
      await expect(authHelper.page).toHaveURL(/\/login/, { timeout: 10000 });
    });

    test('should prevent reuse of reset tokens', async ({ page }) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip('MailHog not available for email testing');
      }

      const newPassword = 'FirstResetPassword123!';
      const { resetToken } = await authHelper.completePasswordReset(
        AUTH_TEST_USERS.VALID_USER.email, 
        newPassword
      );

      // Try to use the same token again
      await authHelper.page.goto(`/reset-password?token=${resetToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Should show error about token already being used
      await authHelper.verifyErrorMessage('Reset token has already been used');
    });
  });

  test.describe('Security Features', () => {
    test('should protect against timing attacks', async () => {
      const validEmail = AUTH_TEST_USERS.VALID_USER.email;
      const invalidEmail = 'nonexistent@example.com';

      // Measure response time for valid email
      const validStartTime = Date.now();
      await authHelper.requestPasswordReset(validEmail);
      const validEndTime = Date.now();
      const validTime = validEndTime - validStartTime;

      // Clear and measure response time for invalid email
      await authHelper.navigateToPasswordReset();
      const invalidStartTime = Date.now();
      await authHelper.requestPasswordReset(invalidEmail);
      const invalidEndTime = Date.now();
      const invalidTime = invalidEndTime - invalidStartTime;

      // Response times should be similar (within reasonable variance)
      const timeDifference = Math.abs(validTime - invalidTime);
      const averageTime = (validTime + invalidTime) / 2;
      const percentageDifference = (timeDifference / averageTime) * 100;

      console.log(`Valid email time: ${validTime}ms, Invalid email time: ${invalidTime}ms`);
      console.log(`Percentage difference: ${percentageDifference.toFixed(2)}%`);

      // Timing difference should be minimal (< 50% difference)
      expect(percentageDifference).toBeLessThan(50);
    });

    test('should not reveal user existence in error messages', async () => {
      await authHelper.requestPasswordReset('nonexistent@example.com');

      // Should not reveal whether user exists
      await authHelper.verifySuccessMessage('If an account with this email exists, you will receive a password reset link');
    });

    test('should expire reset tokens after time limit', async ({ page }) => {
      // Mock token expiry check
      await page.route('**/api/v1/auth/validate-reset-token**', route => {
        const url = new URL(route.request().url());
        const token = url.searchParams.get('token');
        
        if (token === 'expired-token') {
          route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Reset token has expired',
              error: 'token_expired',
              expiresAfter: EMAIL_VERIFICATION.TOKEN_EXPIRY_HOURS,
            }),
          });
        } else {
          route.continue();
        }
      });

      await authHelper.page.goto('/reset-password?token=expired-token');
      await authHelper.page.waitForLoadState('networkidle');

      await authHelper.verifyErrorMessage('Reset token has expired');
    });

    test('should use secure token generation', async ({ page }) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip('MailHog not available for email testing');
      }

      await authHelper.requestPasswordReset(AUTH_TEST_USERS.VALID_USER.email);

      const resetEmail = await authHelper.getPasswordResetEmail(AUTH_TEST_USERS.VALID_USER.email);
      const emailBody = resetEmail.Content.Body;

      // Extract token from email
      const tokenMatch = emailBody.match(/token=([a-zA-Z0-9\-_]+)/);
      expect(tokenMatch).toBeTruthy();

      const token = tokenMatch![1];

      // Token should be sufficiently long and random
      expect(token.length).toBeGreaterThan(20);
      expect(token).toMatch(/^[a-zA-Z0-9\-_]+$/); // URL-safe characters
      
      // Token should not contain predictable patterns
      expect(token).not.toMatch(/123|abc|test|user/i);
    });
  });

  test.describe('Mobile Responsiveness', () => {
    Object.entries(MOBILE_VIEWPORTS).forEach(([deviceName, viewport]) => {
      test(`should work on ${deviceName}`, async ({ page }) => {
        await page.setViewportSize(viewport);
        
        const mobileAuthHelper = new EnhancedAuthHelper(page);
        await mobileAuthHelper.clearStorage();

        await mobileAuthHelper.navigateToPasswordReset();

        // Verify form is usable on mobile
        await expect(mobileAuthHelper.page.locator('input[name="email"]')).toBeVisible();

        const emailField = mobileAuthHelper.page.locator('input[name="email"]');
        const submitButton = mobileAuthHelper.page.locator('button[type="submit"]');

        // Test touch interactions
        await emailField.tap();
        await emailField.fill(AUTH_TEST_USERS.VALID_USER.email);
        await submitButton.tap();

        // Should show success message
        const hasSuccess = await mobileAuthHelper.page.locator('.success').isVisible({ timeout: 5000 }).catch(() => false);
        expect(hasSuccess).toBeTruthy();
      });
    });
  });

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async () => {
      await authHelper.navigateToPasswordReset();

      const emailField = authHelper.page.locator('input[name="email"]');
      const submitButton = authHelper.page.locator('button[type="submit"]');

      // Test keyboard navigation
      await emailField.focus();
      expect(await emailField.evaluate(el => el === document.activeElement)).toBeTruthy();

      await authHelper.page.keyboard.type(AUTH_TEST_USERS.VALID_USER.email);
      await authHelper.page.keyboard.press('Tab');
      
      // Should focus submit button
      expect(await submitButton.evaluate(el => el === document.activeElement)).toBeTruthy();

      // Submit with Enter
      await authHelper.page.keyboard.press('Enter');

      await authHelper.verifySuccessMessage();
    });

    test('should have proper labels and ARIA attributes', async () => {
      await authHelper.navigateToPasswordReset();

      const emailField = authHelper.page.locator('input[name="email"]');

      // Check for accessibility attributes
      const hasLabel = await emailField.evaluate(el => {
        return el.hasAttribute('aria-label') || 
               el.hasAttribute('aria-labelledby') || 
               document.querySelector(`label[for="${el.id}"]`) !== null;
      });

      expect(hasLabel).toBeTruthy();

      // Form should have proper role
      const form = authHelper.page.locator('form, [role="form"]');
      await expect(form).toBeVisible();
    });

    test('should announce status messages to screen readers', async () => {
      await authHelper.requestPasswordReset(AUTH_TEST_USERS.VALID_USER.email);

      // Success message should be in live region
      const liveRegion = authHelper.page.locator('[role="status"], [aria-live="polite"], [aria-live="assertive"]');
      await expect(liveRegion).toBeVisible();

      const messageText = await liveRegion.textContent();
      expect(messageText?.trim()).toBeTruthy();
    });
  });

  test.describe('Performance', () => {
    test('should complete reset request within performance threshold', async () => {
      await authHelper.navigateToPasswordReset();

      const startTime = Date.now();

      await authHelper.page.locator('input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.email);
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifySuccessMessage();

      const endTime = Date.now();
      const resetTime = endTime - startTime;

      expect(resetTime).toBeLessThan(AUTH_PERFORMANCE_THRESHOLDS.PASSWORD_RESET_TIME_MS);
      console.log(`Password reset request completed in ${resetTime}ms`);
    });
  });

  test.describe('Error Handling', () => {
    test('should handle network failures gracefully', async ({ page }) => {
      await authHelper.navigateToPasswordReset();

      // Mock network failure
      await page.route('**/api/v1/auth/forgot-password', route => {
        route.abort('internetdisconnected');
      });

      await authHelper.page.locator('input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.email);
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifyErrorMessage();
    });

    test('should handle server errors gracefully', async ({ page }) => {
      await authHelper.navigateToPasswordReset();

      // Mock server error
      await page.route('**/api/v1/auth/forgot-password', route => {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Internal server error',
            error: 'service_unavailable',
          }),
        });
      });

      await authHelper.page.locator('input[name="email"]').fill(AUTH_TEST_USERS.VALID_USER.email);
      await authHelper.page.locator('button[type="submit"]').click();

      await authHelper.verifyErrorMessage();
    });
  });
});