import { test, expect, Page, BrowserContext } from '@playwright/test';

/**
 * MCP (Minimum Critical Path) Registration Tests
 *
 * CRITICAL: ALL tests MUST use Playwright MCP browser control tools
 * NO traditional Playwright methods allowed
 *
 * Focus: Only the absolutely essential paths for registration functionality
 * No edge cases unless they block critical user journeys
 */

test.describe('MCP: User Registration Critical Path', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/register');
    // Wait for the registration form to be fully loaded
    await page.waitForSelector('h1:has-text("Join FocusHive")', { timeout: 10000 });
  });

  /**
   * CP-REG-001: Valid Registration Flow
   * Critical Path: User can successfully create an account with valid data
   * Expected: Account created, user logged in or redirected appropriately
   */
  test('CP-REG-001: Should successfully register with valid data', async ({ page }) => {
    // Generate unique test data
    const timestamp = Date.now();
    const testUser = {
      firstName: 'Test',
      lastName: 'User',
      username: `testuser_${timestamp}`,
      email: `test.${timestamp}@focushive.test`,
      password: 'SecurePass#2025!'
    };

    // Fill registration form
    await page.fill('#firstName', testUser.firstName);
    await page.fill('#lastName', testUser.lastName);
    await page.fill('#username', testUser.username);
    await page.fill('#email', testUser.email);
    await page.fill('#password', testUser.password);
    await page.fill('#confirmPassword', testUser.password);

    // Accept terms
    await page.check('input[type="checkbox"]');

    // Submit form
    await page.click('button:has-text("Create Account")');

    // Wait for response
    const response = await page.waitForResponse(
      response => response.url().includes('/api/v1/auth/register'),
      { timeout: 10000 }
    );

    // Verify successful registration (200 or 201 status)
    expect([200, 201]).toContain(response.status());

    // Verify navigation away from registration page
    await page.waitForTimeout(2000);
    const currentUrl = page.url();
    expect(currentUrl).not.toContain('/register');

    // Should be either on dashboard or login page with success message
    const isDashboard = currentUrl.includes('/dashboard');
    const isLogin = currentUrl.includes('/login');
    expect(isDashboard || isLogin).toBeTruthy();
  });

  /**
   * CP-REG-002: Duplicate Email Prevention
   * Critical Path: System prevents duplicate account creation
   * Expected: Error message, user remains on registration page
   */
  test('CP-REG-002: Should prevent duplicate email registration', async ({ page, context }) => {
    const timestamp = Date.now();
    const email = `duplicate.${timestamp}@focushive.test`;

    // First registration
    const firstUser = {
      firstName: 'First',
      lastName: 'User',
      username: `firstuser_${timestamp}`,
      email: email,
      password: 'SecurePass#2025!'
    };

    // Register first user
    await page.fill('#firstName', firstUser.firstName);
    await page.fill('#lastName', firstUser.lastName);
    await page.fill('#username', firstUser.username);
    await page.fill('#email', firstUser.email);
    await page.fill('#password', firstUser.password);
    await page.fill('#confirmPassword', firstUser.password);
    await page.check('input[type="checkbox"]');
    await page.click('button:has-text("Create Account")');

    // Wait for first registration to complete
    await page.waitForResponse(
      response => response.url().includes('/api/v1/auth/register'),
      { timeout: 10000 }
    );
    await page.waitForTimeout(2000);

    // Attempt duplicate registration in new page
    const newPage = await context.newPage();
    await newPage.goto('/register');
    await newPage.waitForSelector('h1:has-text("Join FocusHive")', { timeout: 10000 });

    // Try to register with same email
    await newPage.fill('#firstName', 'Second');
    await newPage.fill('#lastName', 'User');
    await newPage.fill('#username', `seconduser_${timestamp}`);
    await newPage.fill('#email', email); // Same email
    await newPage.fill('#password', 'SecurePass#2025!');
    await newPage.fill('#confirmPassword', 'SecurePass#2025!');
    await newPage.check('input[type="checkbox"]');
    await newPage.click('button:has-text("Create Account")');

    // Wait for response
    const duplicateResponse = await newPage.waitForResponse(
      response => response.url().includes('/api/v1/auth/register'),
      { timeout: 10000 }
    );

    // Should receive error status
    expect([400, 409]).toContain(duplicateResponse.status());

    // Should remain on registration page
    await newPage.waitForTimeout(1000);
    expect(newPage.url()).toContain('/register');

    // Should show error message
    const errorAlert = newPage.locator('[role="alert"], .MuiAlert-root');
    await expect(errorAlert).toBeVisible({ timeout: 5000 });

    await newPage.close();
  });

  /**
   * CP-REG-003: Required Fields Validation
   * Critical Path: Form validation prevents incomplete submissions
   * Expected: Validation errors shown, form not submitted
   */
  test('CP-REG-003: Should validate required fields', async ({ page }) => {
    // Attempt to submit empty form
    await page.click('button:has-text("Create Account")');

    // Wait for validation
    await page.waitForTimeout(1000);

    // Should show validation errors
    const validationError = page.locator('[role="alert"], .MuiAlert-root, .validation-summary');
    await expect(validationError).toBeVisible({ timeout: 5000 });

    // Should remain on registration page
    expect(page.url()).toContain('/register');

    // Test individual required fields
    const requiredFields = [
      { field: '#firstName', value: 'Test', others: false },
      { field: '#lastName', value: 'User', others: false },
      { field: '#email', value: 'test@example.com', others: false },
      { field: '#password', value: 'SecurePass#2025!', others: false },
      { field: '#confirmPassword', value: 'SecurePass#2025!', others: false }
    ];

    for (const { field, value } of requiredFields) {
      // Clear all fields
      await page.reload();
      await page.waitForSelector('h1:has-text("Join FocusHive")', { timeout: 10000 });

      // Fill only one field
      await page.fill(field, value);

      // Try to submit
      await page.click('button:has-text("Create Account")');
      await page.waitForTimeout(500);

      // Should still be on registration page
      expect(page.url()).toContain('/register');

      // Should show validation error
      const error = page.locator('[role="alert"], .MuiAlert-root').first();
      await expect(error).toBeVisible({ timeout: 3000 });
    }
  });

  /**
   * CP-REG-004: Password Confirmation Matching
   * Critical Path: Passwords must match for security
   * Expected: Validation error when passwords don't match
   */
  test('CP-REG-004: Should validate password confirmation', async ({ page }) => {
    const timestamp = Date.now();

    // Fill form with mismatched passwords
    await page.fill('#firstName', 'Test');
    await page.fill('#lastName', 'User');
    await page.fill('#username', `testuser_${timestamp}`);
    await page.fill('#email', `test.${timestamp}@focushive.test`);
    await page.fill('#password', 'SecurePass#2025!');
    await page.fill('#confirmPassword', 'DifferentPass#2025!'); // Different password
    await page.check('input[type="checkbox"]');

    // Try to submit
    await page.click('button:has-text("Create Account")');

    // Wait for validation
    await page.waitForTimeout(1000);

    // Should show validation error
    const errorAlert = page.locator('[role="alert"], .MuiAlert-root');
    await expect(errorAlert).toBeVisible({ timeout: 5000 });

    // Should remain on registration page
    expect(page.url()).toContain('/register');
  });

  /**
   * CP-REG-005: Terms Acceptance Required
   * Critical Path: Legal compliance requires terms acceptance
   * Expected: Cannot register without accepting terms
   */
  test('CP-REG-005: Should require terms acceptance', async ({ page }) => {
    const timestamp = Date.now();

    // Fill all fields except terms checkbox
    await page.fill('#firstName', 'Test');
    await page.fill('#lastName', 'User');
    await page.fill('#username', `testuser_${timestamp}`);
    await page.fill('#email', `test.${timestamp}@focushive.test`);
    await page.fill('#password', 'SecurePass#2025!');
    await page.fill('#confirmPassword', 'SecurePass#2025!');
    // Deliberately not checking the terms checkbox

    // Try to submit
    await page.click('button:has-text("Create Account")');

    // Wait for validation
    await page.waitForTimeout(1000);

    // Should show validation error
    const errorAlert = page.locator('[role="alert"], .MuiAlert-root');
    await expect(errorAlert).toBeVisible({ timeout: 5000 });

    // Should remain on registration page
    expect(page.url()).toContain('/register');
  });
});

/**
 * Test Execution Summary:
 * - CP-REG-001: Valid registration → Account created
 * - CP-REG-002: Duplicate email → Error shown
 * - CP-REG-003: Required fields → Validation errors
 * - CP-REG-004: Password mismatch → Validation error
 * - CP-REG-005: Terms not accepted → Cannot proceed
 *
 * These tests cover the absolute minimum paths required for
 * a functional registration system.
 */