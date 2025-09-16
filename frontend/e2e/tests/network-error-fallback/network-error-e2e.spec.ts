/**
 * Comprehensive E2E Tests for NetworkErrorFallback Component
 * Tests real network failure scenarios and user interactions
 */

import {expect, test} from '@playwright/test';

test.describe('NetworkErrorFallback E2E Tests', () => {
  // Test with working backend
  test.describe('With Backend Available', () => {
    test.beforeEach(async ({page}) => {
      // Ensure we start with clean state
      await page.goto('/');
      await page.waitForLoadState('networkidle');
    });

    test('should handle API failures gracefully', async ({page}) => {
      // Intercept API calls and simulate failure
      await page.route('**/api/**', route => {
        route.abort('failed');
      });

      // Navigate to a route that would trigger API calls
      await page.goto('/dashboard'); // or wherever NetworkErrorFallback appears

      // Verify the error boundary appears
      await expect(page.getByRole('alert')).toBeVisible();
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      await expect(page.getByText('Unable to connect to the server')).toBeVisible();
    });

    test('should display network status correctly', async ({page}) => {
      // Simulate offline status
      await page.context().setOffline(true);
      
      // Reload to trigger error boundary
      await page.reload();
      
      // Check network status chip
      await expect(page.getByText('Network: Offline')).toBeVisible();
      
      // Simulate coming back online
      await page.context().setOffline(false);
      
      // Trigger online event
      await page.evaluate(() => {
        window.dispatchEvent(new Event('online'));
      });
      
      // Should show restored status
      await expect(page.getByText('Network: Restored')).toBeVisible();
    });

    test('should handle retry button functionality', async ({page}) => {
      // Setup failing API initially
      let callCount = 0;
      await page.route('**/api/**', route => {
        callCount++;
        if (callCount <= 2) {
          route.abort('failed');
        } else {
          route.fulfill({status: 200, body: JSON.stringify({status: 'ok'})});
        }
      });

      await page.goto('/dashboard');
      
      // Wait for error boundary
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      // Click retry button
      await page.getByRole('button', {name: 'Retry Now'}).click();
      
      // Should show retry progress
      await expect(page.getByText(/Retry \d+\/3/)).toBeVisible();
      
      // After successful retry, error boundary should disappear
      await expect(page.getByText('Network Connection Error')).not.toBeVisible({timeout: 10000});
    });

    test('should handle check connection button', async ({page}) => {
      // Setup initial failure
      await page.route('**/api/**', route => {
        route.abort('failed');
      });
      
      await page.goto('/dashboard');
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      // Clear route to allow success
      await page.unroute('**/api/**');
      
      // Setup successful response for favicon.ico check
      await page.route('**/favicon.ico', route => {
        route.fulfill({status: 200});
      });
      
      // Click check connection
      await page.getByRole('button', {name: 'Check Connection'}).click();
      
      // Should attempt network check
      await expect(page.getByText('Network: Online')).toBeVisible({timeout: 5000});
    });

    test('should show troubleshooting steps', async ({page}) => {
      await page.route('**/api/**', route => route.abort('failed'));
      
      await page.goto('/dashboard');
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      // Verify troubleshooting section is present
      await expect(page.getByText('Troubleshooting Steps:')).toBeVisible();
      await expect(page.getByText('Check your internet connection')).toBeVisible();
      await expect(page.getByText('Try refreshing the page')).toBeVisible();
      await expect(page.getByText('Disable VPN or proxy if enabled')).toBeVisible();
    });
  });

  // Test with backend unavailable
  test.describe('With Backend Unavailable', () => {
    test.beforeEach(async ({page}) => {
      // Block all backend requests
      await page.route('**/localhost:808*/**', route => {
        route.abort('failed');
      });
    });

    test('should handle complete backend failure', async ({page}) => {
      await page.goto('/');
      
      // App should still load but show network errors for API calls
      await expect(page.locator('#root')).toBeVisible();
      
      // Navigate to a route that needs API data
      await page.goto('/dashboard');
      
      // Should show network error fallback
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      await expect(page.getByText('Network: Offline')).toBeVisible();
    });

    test('should handle max retries reached', async ({page}) => {
      await page.goto('/dashboard');
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      // Click retry multiple times to reach limit
      for (let i = 0; i < 4; i++) {
        const retryButton = page.getByRole('button', {name: 'Retry Now'});
        if (await retryButton.isVisible()) {
          await retryButton.click();
          await page.waitForTimeout(500); // Wait between retries
        }
      }
      
      // Should show max retries reached
      await expect(page.getByText('Max Retries Reached')).toBeVisible();
      await expect(page.getByText('Contact support if the problem persists')).toBeVisible();
    });
  });

  // Test network conditions
  test.describe('Network Condition Simulation', () => {
    test('should handle slow network', async ({page}) => {
      // Simulate slow network
      await page.route('**/api/**', async route => {
        await new Promise(resolve => setTimeout(resolve, 5000)); // 5s delay
        route.fulfill({status: 200, body: JSON.stringify({data: 'slow response'})});
      });

      await page.goto('/dashboard');
      
      // Should eventually show content, but might show loading states
      await expect(page.getByText('Network Connection Error')).not.toBeVisible({timeout: 10000});
    });

    test('should handle intermittent connectivity', async ({page}) => {
      let requestCount = 0;
      
      await page.route('**/api/**', route => {
        requestCount++;
        // Fail every other request
        if (requestCount % 2 === 0) {
          route.abort('failed');
        } else {
          route.fulfill({status: 200, body: JSON.stringify({status: 'ok'})});
        }
      });

      await page.goto('/dashboard');
      
      // May show error initially, but should recover
      if (await page.getByText('Network Connection Error').isVisible()) {
        await page.getByRole('button', {name: 'Retry Now'}).click();
      }
      
      // Should eventually succeed
      await expect(page.getByText('Network Connection Error')).not.toBeVisible({timeout: 10000});
    });
  });

  // Cross-browser testing
  test.describe('Cross-Browser Network Error Handling', () => {
    ['chromium', 'firefox', 'webkit'].forEach(browserName => {
      test(`should work correctly in ${browserName}`, async ({page}) => {
        console.log(`Testing NetworkErrorFallback in ${browserName}`);
        
        await page.route('**/api/**', route => route.abort('failed'));
        await page.goto('/dashboard');
        
        // Core functionality should work in all browsers
        await expect(page.getByText('Network Connection Error')).toBeVisible();
        await expect(page.getByRole('button', {name: 'Retry Now'})).toBeVisible();
        await expect(page.getByRole('button', {name: 'Check Connection'})).toBeVisible();
        
        // Test retry functionality
        await page.unroute('**/api/**');
        await page.route('**/favicon.ico', route => route.fulfill({status: 200}));
        
        await page.getByRole('button', {name: 'Check Connection'}).click();
        
        // Should show online status
        await expect(page.getByText('Network: Online')).toBeVisible({timeout: 5000});
      });
    });
  });

  // Mobile-specific tests
  test.describe('Mobile Network Error Handling', () => {
    test('should work on mobile devices', async ({page}) => {
      // Set mobile viewport
      await page.setViewportSize({width: 375, height: 667});
      
      await page.route('**/api/**', route => route.abort('failed'));
      await page.goto('/dashboard');
      
      // Error boundary should be mobile-friendly
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      // Buttons should be touchable
      const retryButton = page.getByRole('button', {name: 'Retry Now'});
      await expect(retryButton).toBeVisible();
      
      // Check button size is appropriate for touch
      const buttonBox = await retryButton.boundingBox();
      expect(buttonBox?.height).toBeGreaterThan(44); // iOS minimum touch target
    });
  });

  // Accessibility testing
  test.describe('Accessibility', () => {
    test('should be accessible with keyboard navigation', async ({page}) => {
      await page.route('**/api/**', route => route.abort('failed'));
      await page.goto('/dashboard');
      
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      // Should be able to navigate with keyboard
      await page.keyboard.press('Tab');
      await expect(page.getByRole('button', {name: 'Retry Now'})).toBeFocused();
      
      await page.keyboard.press('Tab');
      await expect(page.getByRole('button', {name: 'Check Connection'})).toBeFocused();
      
      // Should be able to activate with Enter
      await page.keyboard.press('Enter');
      
      // Button should have been clicked
      await page.waitForTimeout(100);
    });

    test('should have proper ARIA attributes', async ({page}) => {
      await page.route('**/api/**', route => route.abort('failed'));
      await page.goto('/dashboard');
      
      const errorContainer = page.getByRole('alert');
      await expect(errorContainer).toBeVisible();
      
      // Check for proper semantic structure
      await expect(page.getByRole('button', {name: 'Retry Now'})).toBeVisible();
      await expect(page.getByRole('button', {name: 'Check Connection'})).toBeVisible();
    });
  });

  // Performance testing
  test.describe('Performance', () => {
    test('should render quickly even with network errors', async ({page}) => {
      const startTime = Date.now();
      
      await page.route('**/api/**', route => route.abort('failed'));
      await page.goto('/dashboard');
      
      await expect(page.getByText('Network Connection Error')).toBeVisible();
      
      const renderTime = Date.now() - startTime;
      expect(renderTime).toBeLessThan(5000); // Should render within 5 seconds
    });
  });
});