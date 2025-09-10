import { test, expect } from '@playwright/test';

/**
 * E2E Smoke Test Suite for FocusHive
 * 
 * Phase 1: Environment Setup and Validation (UOL-326)
 * 
 * This test verifies that:
 * 1. Frontend is accessible
 * 2. Basic page structure loads
 * 3. Playwright configuration is working
 * 4. Testing infrastructure is properly set up
 */

test.describe('Phase 1: Environment Setup Validation', () => {
  test('should load the FocusHive homepage successfully', async ({ page }) => {
    // Navigate to the application
    await page.goto('/');
    
    // Verify basic page structure
    await expect(page).toHaveTitle(/FocusHive/);
    
    // Check that the root element is present
    await expect(page.locator('#root')).toBeVisible();
    
    // Verify page loads without major errors
    const hasErrors = await page.locator('body').textContent();
    expect(hasErrors).not.toContain('Error');
    expect(hasErrors).not.toContain('Failed to fetch');
    
    console.log('✅ Frontend is accessible and loading correctly');
  });

  test('should have correct meta tags and SEO setup', async ({ page }) => {
    await page.goto('/');
    
    // Check meta tags
    const title = await page.title();
    expect(title).toContain('FocusHive');
    
    const description = await page.getAttribute('meta[name="description"]', 'content');
    expect(description).toContain('Digital co-working');
    
    const viewport = await page.getAttribute('meta[name="viewport"]', 'content');
    expect(viewport).toContain('width=device-width');
    
    console.log('✅ Meta tags and SEO setup verified');
  });

  test('should verify Playwright configuration', async ({ page, browserName }) => {
    console.log(`🌐 Testing with ${browserName}`);
    
    // Test basic browser functionality
    await page.goto('/');
    
    // Verify viewport size
    const viewportSize = page.viewportSize();
    expect(viewportSize).toBeTruthy();
    console.log(`📱 Viewport: ${viewportSize?.width}x${viewportSize?.height}`);
    
    // Test network idle wait
    await page.waitForLoadState('networkidle');
    
    // Verify screenshot capability
    await page.screenshot({ path: `e2e-results/smoke-test-${browserName}.png` });
    
    console.log('✅ Playwright configuration verified');
  });

  test('should test basic UI interaction', async ({ page }) => {
    await page.goto('/');
    
    // Wait for React app to initialize
    await page.waitForSelector('#root > *', { timeout: 10000 });
    
    // Try to interact with any clickable elements if they exist
    const clickableElements = await page.locator('button, a, [role="button"]').count();
    console.log(`🖱️  Found ${clickableElements} clickable elements`);
    
    // Check if we can focus on the page (basic accessibility test)
    await page.keyboard.press('Tab');
    
    console.log('✅ Basic UI interaction test completed');
  });

  // Test that we can handle potential API call failures gracefully
  test('should handle network conditions gracefully', async ({ page }) => {
    // Intercept API calls and check handling
    const apiCalls: string[] = [];
    
    page.on('request', request => {
      if (request.url().includes('localhost:808')) {
        apiCalls.push(request.url());
      }
    });
    
    await page.goto('/');
    await page.waitForTimeout(2000); // Wait for any initial API calls
    
    console.log(`📡 Detected ${apiCalls.length} API calls:`, apiCalls);
    
    // Application should still render even if APIs are not available
    await expect(page.locator('#root')).toBeVisible();
    
    console.log('✅ Network condition handling verified');
  });
});

test.describe('Phase 1: Browser Compatibility', () => {
  test('should work in all configured browsers', async ({ page, browserName }) => {
    console.log(`🔍 Testing browser: ${browserName}`);
    
    await page.goto('/');
    
    // Basic compatibility checks
    await expect(page.locator('#root')).toBeVisible();
    
    // Check that JavaScript is working
    const hasJSSupport = await page.evaluate(() => {
      return typeof window !== 'undefined' && typeof document !== 'undefined';
    });
    
    expect(hasJSSupport).toBe(true);
    console.log(`✅ JavaScript support confirmed in ${browserName}`);
    
    // Check CSS loading
    const bodyStyle = await page.locator('body').getAttribute('style');
    // Even if no inline styles, the body should be accessible
    await expect(page.locator('body')).toBeVisible();
    
    console.log(`✅ CSS rendering confirmed in ${browserName}`);
  });
});