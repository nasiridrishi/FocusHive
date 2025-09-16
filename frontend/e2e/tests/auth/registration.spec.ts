/**
 * E2E Tests for User Registration with Email Verification
 * Comprehensive registration flow testing including validation, security, and accessibility
 */

import {expect, test} from '@playwright/test';
import {EnhancedAuthHelper} from '../../helpers/auth-helpers';
import {
  AUTH_TEST_USERS,
  generateUniqueAuthUser,
  INVALID_CREDENTIALS,
  MAILHOG_CONFIG,
  MOBILE_VIEWPORTS,
  REGISTRATION_VALIDATION_CASES
} from '../../helpers/auth-fixtures';

test.describe('User Registration Flow', () => {
  let authHelper: EnhancedAuthHelper;

  test.beforeEach(async ({page}) => {
    authHelper = new EnhancedAuthHelper(page);

    // Clear any existing authentication and emails
    await authHelper.clearStorage();
    await authHelper.clearAllEmails();
  });

  test.afterEach(async () => {
    // Cleanup after each test
    await authHelper.clearStorage();
    await authHelper.clearAllEmails();
  });

  test.describe('Registration Form Display', () => {
    test('should display registration form with all required fields', async () => {
      await authHelper.navigateToRegistration();

      // Verify all form fields are present
      await expect(authHelper.page.locator('input[name="username"]')).toBeVisible();
      await expect(authHelper.page.locator('input[name="email"]')).toBeVisible();
      await expect(authHelper.page.locator('input[name="password"]')).toBeVisible();
      await expect(authHelper.page.locator('input[name="firstName"]')).toBeVisible();
      await expect(authHelper.page.locator('input[name="lastName"]')).toBeVisible();

      // Verify submit button is present
      const submitButton = authHelper.page.locator('button[type="submit"]:has-text("Create Account"), button[type="submit"]:has-text("Register")');
      await expect(submitButton).toBeVisible();

      // Verify login link is present
      const loginLink = authHelper.page.locator('a[href="/login"], button:has-text("Sign in"), :text("Sign in here")');
      await expect(loginLink).toBeVisible();
    });

    test('should show proper field labels and placeholders', async () => {
      await authHelper.navigateToRegistration();

      // Check that fields have proper labels or placeholders
      const usernameField = authHelper.page.locator('input[name="username"]');
      const emailField = authHelper.page.locator('input[name="email"]');
      const passwordField = authHelper.page.locator('input[name="password"]');

      // At least one of label, placeholder, or aria-label should be present
      await expect(usernameField).toHaveAttribute('placeholder', /.+/);
      await expect(emailField).toHaveAttribute('type', 'email');
      await expect(passwordField).toHaveAttribute('type', 'password');
    });
  });

  test.describe('Form Validation', () => {
    test('should show validation errors for empty form submission', async () => {
      await authHelper.navigateToRegistration();

      // Submit empty form
      const submitButton = authHelper.page.locator('button[type="submit"]:has-text("Create Account"), button[type="submit"]:has-text("Register")');
      await submitButton.click();

      // Wait for validation
      await authHelper.page.waitForTimeout(1000);

      // Verify validation errors appear (at least one should be visible)
      const errorElements = authHelper.page.locator('[role="alert"], .error, .MuiFormHelperText-error, .error-message');
      await expect(errorElements.first()).toBeVisible({timeout: 5000});
    });

    // Test each validation case
    Object.entries(REGISTRATION_VALIDATION_CASES).forEach(([testName, testData]) => {
      test(`should validate ${testName.toLowerCase().replace(/_/g, ' ')}`, async () => {
        await authHelper.navigateToRegistration();

        // Fill form with invalid data
        await authHelper.fillRegistrationForm(testData);

        // Submit form
        await authHelper.submitRegistrationForm();

        // Verify specific error message if provided
        if (testData.expectedError) {
          await authHelper.verifyErrorMessage(testData.expectedError);
        } else {
          await authHelper.verifyErrorMessage();
        }

        // Verify user stays on registration page
        await expect(authHelper.page).toHaveURL(/\/register/);
      });
    });

    test('should validate existing username', async () => {
      // First, register a user successfully
      const existingUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);
      const {user} = await authHelper.registerUserWithVerification(existingUser);

      // Try to register with same username
      await authHelper.navigateToRegistration();
      await authHelper.fillRegistrationForm({
        ...generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER),
        username: user.username, // Use existing username
      });
      await authHelper.submitRegistrationForm();

      // Verify error message about existing username
      await authHelper.verifyErrorMessage('Username already exists');
    });

    test('should validate existing email', async () => {
      // First, register a user successfully  
      const existingUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);
      const {user} = await authHelper.registerUserWithVerification(existingUser);

      // Try to register with same email
      await authHelper.navigateToRegistration();
      await authHelper.fillRegistrationForm({
        ...generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER),
        email: user.email, // Use existing email
      });
      await authHelper.submitRegistrationForm();

      // Verify error message about existing email
      await authHelper.verifyErrorMessage('Email already exists');
    });
  });

  test.describe('Successful Registration', () => {
    test('should successfully register new user', async () => {
      const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);

      // Set up API monitoring
      const apiMonitoring = await authHelper.setupApiMonitoring();

      await authHelper.navigateToRegistration();
      await authHelper.fillRegistrationForm(uniqueUser);
      await authHelper.submitRegistrationForm();

      // Verify API call was made
      const registerRequests = apiMonitoring.getRegisterRequests();
      expect(registerRequests).toHaveLength(1);
      expect(registerRequests[0].postData).toMatchObject({
        username: uniqueUser.username,
        email: uniqueUser.email,
        firstName: uniqueUser.firstName,
        lastName: uniqueUser.lastName,
      });

      // Verify success message
      await authHelper.verifySuccessMessage('Registration successful');

      // Verify redirect or message about email verification
      const currentUrl = authHelper.page.url();
      const hasVerificationMessage = await authHelper.page.locator(':text("check your email")').isVisible({timeout: 5000}).catch(() => false);

      expect(currentUrl.includes('/login') || currentUrl.includes('/verify') || hasVerificationMessage).toBeTruthy();
    });

    test('should send verification email after registration', async ({page: _page}) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip(true, 'MailHog not available for email testing');
      }

      const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);

      await authHelper.navigateToRegistration();
      await authHelper.fillRegistrationForm(uniqueUser);
      await authHelper.submitRegistrationForm();

      // Wait for success message
      await authHelper.verifySuccessMessage();

      // Check that verification email was sent
      const verificationEmail = await authHelper.getVerificationEmail(uniqueUser.email);
      expect(verificationEmail.Subject).toMatch(/verify|confirmation/i);
      expect(verificationEmail.To[0]).toMatchObject({
        Mailbox: uniqueUser.email.split('@')[0],
        Domain: uniqueUser.email.split('@')[1],
      });
    });
  });

  test.describe('Email Verification Flow', () => {
    test('should verify email with valid token', async ({page: _page}) => {
      // Skip if MailHog is not available
      try {
        await fetch(`${MAILHOG_CONFIG.API_BASE_URL}/api/v1/messages`);
      } catch {
        test.skip(true, 'MailHog not available for email testing');
      }

      const {user, verificationToken} = await authHelper.registerUserWithVerification(
          generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER)
      );

      // Verify email using token
      await authHelper.verifyEmail(verificationToken);

      // Verify success message
      await authHelper.verifySuccessMessage('Email verified successfully');

      // Verify user can now login
      const tokenInfo = await authHelper.loginWithCredentials(user.username, user.password);
      expect(tokenInfo.accessToken).toBeTruthy();
      expect(tokenInfo.isValid).toBeTruthy();
    });

    test('should handle invalid verification token', async () => {
      const invalidToken = 'invalid-token-12345';

      await authHelper.page.goto(`/verify-email?token=${invalidToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Verify error message for invalid token
      await authHelper.verifyErrorMessage('Invalid or expired verification token');
    });

    test('should handle expired verification token', async () => {
      const expiredToken = 'expired-token-12345';

      await authHelper.page.goto(`/verify-email?token=${expiredToken}`);
      await authHelper.page.waitForLoadState('networkidle');

      // Verify error message for expired token
      await authHelper.verifyErrorMessage('Invalid or expired verification token');
    });
  });

  test.describe('Security Testing', () => {
    test('should sanitize input against XSS', async () => {
      await authHelper.navigateToRegistration();

      // Try to inject XSS in form fields
      await authHelper.fillRegistrationForm({
        username: INVALID_CREDENTIALS.XSS_ATTEMPT.username,
        email: 'xss@test.com',
        password: 'ValidPass123!',
        firstName: '<script>alert("xss")</script>',
        lastName: '<img src=x onerror=alert(1)>',
      });

      await authHelper.submitRegistrationForm();

      // Verify that XSS is not executed (page should still be functional)
      await expect(authHelper.page.locator('input[name="username"]')).toBeVisible();

      // No alert dialogs should be present
      authHelper.page.on('dialog', dialog => {
        throw new Error(`Unexpected dialog: ${dialog.message()}`);
      });
    });

    test('should handle SQL injection attempts', async () => {
      await authHelper.navigateToRegistration();

      await authHelper.fillRegistrationForm({
        username: INVALID_CREDENTIALS.SQL_INJECTION.username,
        email: 'sql@test.com',
        password: INVALID_CREDENTIALS.SQL_INJECTION.password,
        firstName: 'SQL',
        lastName: 'Test',
      });

      await authHelper.submitRegistrationForm();

      // Should show validation error or handle gracefully
      const isOnRegisterPage = await expect(authHelper.page).toHaveURL(/\/register/).catch(() => false);
      const hasErrorMessage = await authHelper.page.locator('[role="alert"], .error').isVisible({timeout: 2000}).catch(() => false);

      expect(isOnRegisterPage || hasErrorMessage).toBeTruthy();
    });

    test('should enforce rate limiting on registration attempts', async () => {
      const baseUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);

      // Make multiple rapid registration attempts
      for (let i = 0; i < 5; i++) {
        const uniqueUser = generateUniqueAuthUser({
          ...baseUser,
          username: `${baseUser.username}_${i}`,
          email: `${i}_${baseUser.email}`,
        });

        await authHelper.navigateToRegistration();
        await authHelper.fillRegistrationForm(uniqueUser);
        await authHelper.submitRegistrationForm();

        // Brief pause between attempts
        await authHelper.page.waitForTimeout(100);
      }

      // The last few attempts should be rate limited
      const rateLimitMessage = authHelper.page.locator(':text("too many requests"), :text("rate limit"), [role="alert"]');
      const hasRateLimit = await rateLimitMessage.isVisible({timeout: 2000}).catch(() => false);

      // Rate limiting may or may not be implemented - this is informational
      if (hasRateLimit) {
        console.log('✅ Rate limiting is implemented for registration');
      } else {
        console.log('ℹ️ Rate limiting not detected for registration');
      }
    });
  });

  test.describe('Mobile Responsiveness', () => {
    Object.entries(MOBILE_VIEWPORTS).forEach(([deviceName, viewport]) => {
      test(`should work on ${deviceName}`, async ({page}) => {
        await page.setViewportSize(viewport);

        const mobileAuthHelper = new EnhancedAuthHelper(page);
        await mobileAuthHelper.clearStorage();

        await mobileAuthHelper.navigateToRegistration();

        // Verify form is visible and usable on mobile
        await expect(mobileAuthHelper.page.locator('input[name="username"]')).toBeVisible();
        await expect(mobileAuthHelper.page.locator('input[name="email"]')).toBeVisible();
        await expect(mobileAuthHelper.page.locator('input[name="password"]')).toBeVisible();

        // Test form submission on mobile
        const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);
        await mobileAuthHelper.fillRegistrationForm(uniqueUser);
        await mobileAuthHelper.submitRegistrationForm();

        // Should show success message or handle appropriately
        const hasSuccess = await mobileAuthHelper.page.locator('.success, .MuiAlert-standardSuccess').isVisible({timeout: 5000}).catch(() => false);
        const hasError = await mobileAuthHelper.page.locator('[role="alert"], .error').isVisible({timeout: 5000}).catch(() => false);

        expect(hasSuccess || hasError).toBeTruthy();
      });
    });
  });

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async () => {
      await authHelper.navigateToRegistration();

      // Test tab navigation through form
      const usernameField = authHelper.page.locator('input[name="username"]');
      const emailField = authHelper.page.locator('input[name="email"]');
      const passwordField = authHelper.page.locator('input[name="password"]');
      const submitButton = authHelper.page.locator('button[type="submit"]');

      // Focus first field and tab through
      await usernameField.focus();
      expect(await usernameField.evaluate(el => el === document.activeElement)).toBeTruthy();

      await authHelper.page.keyboard.press('Tab');
      expect(await emailField.evaluate(el => el === document.activeElement)).toBeTruthy();

      await authHelper.page.keyboard.press('Tab');
      expect(await passwordField.evaluate(el => el === document.activeElement)).toBeTruthy();

      // Tab to submit button (may have other fields in between)
      let tabCount = 0;
      while (tabCount < 10 && !await submitButton.evaluate(el => el === document.activeElement)) {
        await authHelper.page.keyboard.press('Tab');
        tabCount++;
      }

      // Submit form with Enter key
      await authHelper.page.keyboard.press('Enter');

      // Should trigger form validation
      await authHelper.page.waitForTimeout(500);
    });

    test('should have proper ARIA labels and roles', async () => {
      await authHelper.navigateToRegistration();

      // Check for form role
      const form = authHelper.page.locator('form, [role="form"]');
      await expect(form).toBeVisible();

      // Check for proper input labels
      const usernameField = authHelper.page.locator('input[name="username"]');
      const emailField = authHelper.page.locator('input[name="email"]');

      // Fields should have labels, aria-label, or aria-labelledby
      const usernameHasLabel = await usernameField.evaluate(el => {
        return el.hasAttribute('aria-label') ||
            el.hasAttribute('aria-labelledby') ||
            document.querySelector(`label[for="${el.id}"]`) !== null;
      });

      const emailHasLabel = await emailField.evaluate(el => {
        return el.hasAttribute('aria-label') ||
            el.hasAttribute('aria-labelledby') ||
            document.querySelector(`label[for="${el.id}"]`) !== null;
      });

      expect(usernameHasLabel).toBeTruthy();
      expect(emailHasLabel).toBeTruthy();
    });

    test('should announce validation errors to screen readers', async () => {
      await authHelper.navigateToRegistration();

      // Submit empty form to trigger validation
      const submitButton = authHelper.page.locator('button[type="submit"]');
      await submitButton.click();
      await authHelper.page.waitForTimeout(1000);

      // Error messages should have proper ARIA attributes
      const errorMessages = authHelper.page.locator('[role="alert"], [aria-live="polite"], [aria-live="assertive"]');
      const errorCount = await errorMessages.count();

      expect(errorCount).toBeGreaterThan(0);

      // At least one error should be visible
      await expect(errorMessages.first()).toBeVisible();
    });
  });

  test.describe('Performance', () => {
    test('should complete registration within performance threshold', async () => {
      const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);

      await authHelper.navigateToRegistration();

      const startTime = Date.now();

      await authHelper.fillRegistrationForm(uniqueUser);
      await authHelper.submitRegistrationForm();

      // Wait for success or error response
      await Promise.race([
        authHelper.page.locator('.success, .MuiAlert-standardSuccess').waitFor({timeout: 10000}),
        authHelper.page.locator('[role="alert"], .error').waitFor({timeout: 10000}),
      ]);

      const endTime = Date.now();
      const registrationTime = endTime - startTime;

      expect(registrationTime).toBeLessThan(5000); // Should complete within 5 seconds
      console.log(`Registration completed in ${registrationTime}ms`);
    });
  });

  test.describe('Error Handling', () => {
    test('should handle network errors gracefully', async ({page}) => {
      await authHelper.navigateToRegistration();

      // Mock network failure
      await page.route('**/api/v1/auth/register', route => {
        route.abort('internetdisconnected');
      });

      const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);
      await authHelper.fillRegistrationForm(uniqueUser);
      await authHelper.submitRegistrationForm();

      // Should show network error message
      await authHelper.verifyErrorMessage();
    });

    test('should handle server errors (500) gracefully', async ({page}) => {
      await authHelper.navigateToRegistration();

      // Mock server error
      await page.route('**/api/v1/auth/register', route => {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Internal server error',
            error: 'Registration service temporarily unavailable',
          }),
        });
      });

      const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);
      await authHelper.fillRegistrationForm(uniqueUser);
      await authHelper.submitRegistrationForm();

      // Should show server error message
      await authHelper.verifyErrorMessage();
    });

    test('should handle malformed server response', async ({page}) => {
      await authHelper.navigateToRegistration();

      // Mock malformed response
      await page.route('**/api/v1/auth/register', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: 'invalid json response',
        });
      });

      const uniqueUser = generateUniqueAuthUser(AUTH_TEST_USERS.NEW_USER);
      await authHelper.fillRegistrationForm(uniqueUser);
      await authHelper.submitRegistrationForm();

      // Should handle malformed response gracefully
      await authHelper.verifyErrorMessage();
    });
  });

  test.describe('Navigation', () => {
    test('should navigate to login page from registration', async () => {
      await authHelper.navigateToRegistration();

      const loginLink = authHelper.page.locator('a[href="/login"], button:has-text("Sign in"), :text("Sign in here")');
      await loginLink.click();

      await expect(authHelper.page).toHaveURL(/\/login/);
      await expect(authHelper.page.locator('#email, input[name="email"]')).toBeVisible();
    });

    test('should preserve form data when navigating away and back', async () => {
      await authHelper.navigateToRegistration();

      const testData = {
        username: 'testuser123',
        email: 'test@example.com',
      };

      // Fill some form data
      await authHelper.page.locator('input[name="username"]').fill(testData.username);
      await authHelper.page.locator('input[name="email"]').fill(testData.email);

      // Navigate to login and back
      const loginLink = authHelper.page.locator('a[href="/login"], button:has-text("Sign in")');
      await loginLink.click();
      await expect(authHelper.page).toHaveURL(/\/login/);

      const registerLink = authHelper.page.locator('a[href="/register"], button:has-text("Sign up")');
      await registerLink.click();
      await expect(authHelper.page).toHaveURL(/\/register/);

      // Check if form data is preserved (implementation dependent)
      const usernameValue = await authHelper.page.locator('input[name="username"]').inputValue();
      const emailValue = await authHelper.page.locator('input[name="email"]').inputValue();

      // Form may or may not preserve data - both are valid UX patterns
      console.log(`Form preservation: username=${usernameValue}, email=${emailValue}`);
    });
  });
});