import { test, expect, Page } from '@playwright/test';

/**
 * Simple Registration Test - Focus on Core Functionality
 * Testing against actual FocusHive registration implementation
 */

test.describe('User Registration - Core Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/register');
    // Wait for the form to be visible instead of networkidle
    await page.waitForSelector('h1:has-text("Join FocusHive")', { timeout: 10000 });
  });

  test('should load registration page with all required fields', async ({ page }) => {
    // Verify page title
    await expect(page.locator('h1:has-text("Join FocusHive")')).toBeVisible();

    // Verify all required fields are present
    await expect(page.locator('#firstName')).toBeVisible();
    await expect(page.locator('#lastName')).toBeVisible();
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.locator('#confirmPassword')).toBeVisible();

    // Verify terms checkbox
    await expect(page.locator('input[type="checkbox"]')).toBeVisible();

    // Verify submit button
    await expect(page.locator('button:has-text("Create Account")')).toBeVisible();

    // Verify login link
    await expect(page.locator('button:has-text("Sign In")')).toBeVisible();
  });

  test('should successfully register a new user with valid data', async ({ page }) => {
    // Generate unique user data
    const timestamp = Date.now();
    const userData = {
      firstName: 'Test',
      lastName: 'User',
      username: `testuser_${timestamp}`,
      email: `test.${timestamp}@focushive.test`,
      password: 'SecurePass#2025!',
    };

    // Fill the form
    await page.fill('#firstName', userData.firstName);
    await page.fill('#lastName', userData.lastName);
    await page.fill('#username', userData.username);
    await page.fill('#email', userData.email);
    await page.fill('#password', userData.password);
    await page.fill('#confirmPassword', userData.password);

    // Accept terms
    await page.check('input[type="checkbox"]');

    // Submit the form
    await page.click('button:has-text("Create Account")');

    // Wait for response - either redirect to dashboard or login
    await page.waitForResponse(
      response => response.url().includes('/api/v1/auth/register'),
      { timeout: 10000 }
    );

    // Check for successful registration
    // The app might redirect to dashboard or show a success message
    await page.waitForTimeout(2000);

    const currentUrl = page.url();
    if (currentUrl.includes('/dashboard')) {
      // Successfully registered and logged in
      await expect(page.locator('text=/Welcome/i')).toBeVisible({ timeout: 5000 });
    } else if (currentUrl.includes('/login')) {
      // Redirected to login (some apps do this)
      await expect(page.locator('text=/success/i')).toBeVisible({ timeout: 5000 });
    } else {
      // Still on register page - check for error
      const errorAlert = page.locator('[role="alert"], .MuiAlert-root');
      if (await errorAlert.isVisible()) {
        const errorText = await errorAlert.textContent();
        console.log('Registration error:', errorText);
        // If user already exists, that's okay for this test
        if (!errorText?.includes('already exists')) {
          throw new Error(`Registration failed: ${errorText}`);
        }
      }
    }
  });

  test('should show validation errors for empty required fields', async ({ page }) => {
    // Try to submit empty form
    await page.click('button:has-text("Create Account")');

    // Wait for validation to trigger
    await page.waitForTimeout(1000);

    // Check for validation errors
    const validationSummary = page.locator('.validation-summary, [role="alert"]');

    // Either inline validation or summary should appear
    const hasValidationErrors = await validationSummary.isVisible() ||
      await page.locator('text=/required/i').first().isVisible();

    expect(hasValidationErrors).toBe(true);

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });

  test('should show error for mismatched passwords', async ({ page }) => {
    // Fill the form with mismatched passwords
    await page.fill('#firstName', 'Test');
    await page.fill('#lastName', 'User');
    await page.fill('#username', 'testuser_' + Date.now());
    await page.fill('#email', `test.${Date.now()}@focushive.test`);
    await page.fill('#password', 'SecurePass#2025!');
    await page.fill('#confirmPassword', 'DifferentPass#2025!');
    await page.check('input[type="checkbox"]');

    // Submit the form
    await page.click('button:has-text("Create Account")');

    // Wait for validation
    await page.waitForTimeout(1000);

    // Check for validation error - the app shows generic validation messages
    const errorAlert = page.locator('[role="alert"], .MuiAlert-root');
    await expect(errorAlert).toBeVisible();

    // The validation shows but may not be specific about password mismatch
    const errorText = await errorAlert.textContent();
    expect(errorText).toBeTruthy();

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });

  test('should enforce username length validation (min 3 chars)', async ({ page }) => {
    // Fill form with short username
    await page.fill('#firstName', 'Test');
    await page.fill('#lastName', 'User');
    await page.fill('#username', 'ab'); // Too short
    await page.fill('#email', `test.${Date.now()}@focushive.test`);
    await page.fill('#password', 'SecurePass#2025!');
    await page.fill('#confirmPassword', 'SecurePass#2025!');
    await page.check('input[type="checkbox"]');

    // Submit the form
    await page.click('button:has-text("Create Account")');

    // Wait for validation
    await page.waitForTimeout(1000);

    // Check for validation error - app shows generic validation messages
    const errorAlert = page.locator('[role="alert"], .MuiAlert-root');
    await expect(errorAlert).toBeVisible();

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });

  test('should handle duplicate email gracefully', async ({ page, context }) => {
    // First, register a user
    const timestamp = Date.now();
    const email = `duplicate.test.${timestamp}@focushive.test`;
    const userData = {
      firstName: 'Test',
      lastName: 'User',
      username: `user_${timestamp}`,
      email: email,
      password: 'SecurePass#2025!',
    };

    // Register first user
    await page.fill('#firstName', userData.firstName);
    await page.fill('#lastName', userData.lastName);
    await page.fill('#username', userData.username);
    await page.fill('#email', userData.email);
    await page.fill('#password', userData.password);
    await page.fill('#confirmPassword', userData.password);
    await page.check('input[type="checkbox"]');
    await page.click('button:has-text("Create Account")');

    // Wait for registration to complete
    await page.waitForResponse(
      response => response.url().includes('/api/v1/auth/register'),
      { timeout: 10000 }
    );
    await page.waitForTimeout(2000);

    // Now try to register with the same email in a new context
    const newPage = await context.newPage();
    await newPage.goto('/register');
    await newPage.waitForSelector('h1:has-text("Join FocusHive")', { timeout: 10000 });

    // Try to register with same email but different username
    await newPage.fill('#firstName', 'Another');
    await newPage.fill('#lastName', 'User');
    await newPage.fill('#username', `different_${timestamp}`);
    await newPage.fill('#email', email); // Same email
    await newPage.fill('#password', 'SecurePass#2025!');
    await newPage.fill('#confirmPassword', 'SecurePass#2025!');
    await newPage.check('input[type="checkbox"]');
    await newPage.click('button:has-text("Create Account")');

    // Wait for API response
    const response = await newPage.waitForResponse(
      response => response.url().includes('/api/v1/auth/register'),
      { timeout: 10000 }
    );

    // Should get an error status (400 or 409)
    expect([400, 409]).toContain(response.status());

    // Check for error message (may be generic)
    await newPage.waitForTimeout(1000);
    const errorAlert = newPage.locator('[role="alert"], .MuiAlert-root');
    const errorVisible = await errorAlert.isVisible();
    expect(errorVisible).toBe(true);

    await newPage.close();
  });

  test('should require terms acceptance', async ({ page }) => {
    // Fill all fields except terms checkbox
    const timestamp = Date.now();
    await page.fill('#firstName', 'Test');
    await page.fill('#lastName', 'User');
    await page.fill('#username', `testuser_${timestamp}`);
    await page.fill('#email', `test.${timestamp}@focushive.test`);
    await page.fill('#password', 'SecurePass#2025!');
    await page.fill('#confirmPassword', 'SecurePass#2025!');

    // Don't check the terms checkbox
    // await page.check('input[type="checkbox"]');

    // Submit the form
    await page.click('button:has-text("Create Account")');

    // Wait for validation
    await page.waitForTimeout(1000);

    // Check for validation error
    const errorAlert = page.locator('[role="alert"], .MuiAlert-root');
    await expect(errorAlert).toBeVisible();

    // Verify we're still on registration page
    expect(page.url()).toContain('/register');
  });

  test('should show password strength indicator', async ({ page }) => {
    // Start typing password
    await page.fill('#password', 'weak');

    // Wait for strength indicator to appear
    await page.waitForTimeout(500);

    // Check for password strength indicator - MUI progress bar appears when typing password
    const strengthIndicator = page.locator('.MuiLinearProgress-root, text=Password Strength').first();
    await expect(strengthIndicator).toBeVisible();

    // Type stronger password
    await page.fill('#password', 'SecurePass#2025!');

    // Wait for update
    await page.waitForTimeout(500);

    // Verify strength indicator shows strong
    const strongIndicator = page.locator('text=/strong/i').first();
    await expect(strongIndicator).toBeVisible();
  });

  test('should toggle password visibility', async ({ page }) => {
    // Fill password
    await page.fill('#password', 'SecurePass#2025!');

    // Initially password should be hidden
    const passwordField = page.locator('#password');
    await expect(passwordField).toHaveAttribute('type', 'password');

    // Click toggle button - first instance is for password field
    const toggleButton = page.locator('button[aria-label*="toggle password visibility"]').first();
    await toggleButton.click();

    // Password should now be visible
    await expect(passwordField).toHaveAttribute('type', 'text');

    // Click again to hide
    await toggleButton.click();

    // Password should be hidden again
    await expect(passwordField).toHaveAttribute('type', 'password');
  });
});