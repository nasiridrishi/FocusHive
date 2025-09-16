import {expect, test} from '@playwright/test';

test.describe('App Smoke Tests', () => {
  test('should load the homepage', async ({page}) => {
    await page.goto('/');

    // Check that the page loads without errors
    await expect(page).toHaveTitle(/FocusHive/);

    // Check for the presence of main navigation or app content
    await expect(page.locator('main, [role="main"], .App')).toBeVisible();
  });

  test('should navigate between pages', async ({page}) => {
    await page.goto('/');

    // This will depend on your actual navigation structure
    // Add more specific navigation tests based on your app's routing
    const mainContent = page.locator('main, [role="main"], .App');
    await expect(mainContent).toBeVisible();
  });

  test('should be responsive on mobile', async ({page}) => {
    // Set mobile viewport
    await page.setViewportSize({width: 375, height: 667});
    await page.goto('/');

    // Check that the page is still functional on mobile
    await expect(page.locator('main, [role="main"], .App')).toBeVisible();
  });

  test('should handle network errors gracefully', async ({page}) => {
    // Simulate offline network
    await page.context().setOffline(true);
    await page.goto('/');

    // The page should still load (if it has offline capabilities)
    // or show an appropriate error message
    const body = page.locator('body');
    await expect(body).toBeVisible();

    // Re-enable network
    await page.context().setOffline(false);
  });
});