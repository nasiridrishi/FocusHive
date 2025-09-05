import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should show login form when not authenticated', async ({ page }) => {
    // Navigate to login page (adjust the path based on your routing)
    await page.goto('/login');
    
    // Check for login form elements
    await expect(page.locator('input[name="username"], input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('should show validation errors for empty form', async ({ page }) => {
    await page.goto('/login');
    
    // Try to submit empty form
    await page.locator('button[type="submit"]').click();
    
    // Check for validation errors (adjust selectors based on your implementation)
    await expect(page.locator('[role="alert"], .error, .MuiFormHelperText-error')).toBeVisible();
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.goto('/login');
    
    // Fill form with invalid credentials
    const usernameInput = page.locator('input[name="username"], input[name="email"]');
    const passwordInput = page.locator('input[name="password"]');
    
    await usernameInput.fill('invalid@example.com');
    await passwordInput.fill('wrongpassword');
    await page.locator('button[type="submit"]').click();
    
    // Check for error message
    await expect(page.locator('[role="alert"], .error-message')).toBeVisible();
  });

  test('should successfully login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    
    // Fill form with valid credentials (these should match your MSW handlers)
    const usernameInput = page.locator('input[name="username"], input[name="email"]');
    const passwordInput = page.locator('input[name="password"]');
    
    await usernameInput.fill('testuser');
    await passwordInput.fill('password');
    await page.locator('button[type="submit"]').click();
    
    // Check for successful redirect or user menu
    await expect(page).toHaveURL(/\/dashboard|\/home|\/app/);
    // Or check for user menu/avatar indicating successful login
    await expect(page.locator('[data-testid="user-menu"], .user-avatar')).toBeVisible();
  });

  test('should navigate to register from login', async ({ page }) => {
    await page.goto('/login');
    
    // Click register link
    await page.locator('a[href="/register"], button:has-text("Register"), :text("Sign up")').click();
    
    // Should be on register page
    await expect(page).toHaveURL(/\/register/);
    await expect(page.locator('input[name="username"], input[name="email"]')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    // First login
    await page.goto('/login');
    await page.locator('input[name="username"], input[name="email"]').fill('testuser');
    await page.locator('input[name="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    
    // Wait for successful login
    await expect(page.locator('[data-testid="user-menu"], .user-avatar')).toBeVisible();
    
    // Click logout
    await page.locator('[data-testid="user-menu"], .user-avatar').click();
    await page.locator('button:has-text("Logout"), [data-testid="logout-button"]').click();
    
    // Should redirect to login or home page
    await expect(page).toHaveURL(/\/login|\/$/);
  });
});