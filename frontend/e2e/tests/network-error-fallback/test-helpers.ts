/**
 * Helper functions for NetworkErrorFallback E2E testing
 * Provides utilities for setting up network scenarios and assertions
 */

import { Page, expect } from '@playwright/test';

export interface NetworkScenario {
  scenario: 'normal' | 'complete-failure' | 'intermittent' | 'slow' | 'gradual-failure' | 'random-errors';
  options?: {
    failureRate?: number;
    latencyMs?: number;
  };
}

export class NetworkTestHelper {
  constructor(private page: Page, private mockBackendUrl: string = 'http://localhost:8080') {}

  /**
   * Set the network scenario for the mock backend
   */
  async setNetworkScenario(scenario: NetworkScenario): Promise<void> {
    await this.page.request.post(`${this.mockBackendUrl}/test/scenario`, {
      data: scenario
    });
    
    // Wait a moment for the scenario to take effect
    await this.page.waitForTimeout(100);
  }

  /**
   * Reset the mock backend to normal operation
   */
  async resetMockBackend(): Promise<void> {
    await this.page.request.post(`${this.mockBackendUrl}/test/reset`);
    await this.page.waitForTimeout(100);
  }

  /**
   * Get current mock backend status
   */
  async getMockBackendStatus(): Promise<unknown> {
    const response = await this.page.request.get(`${this.mockBackendUrl}/test/status`);
    return await response.json();
  }

  /**
   * Wait for network error fallback to appear
   */
  async waitForNetworkErrorFallback(timeout: number = 10000): Promise<void> {
    await this.page.getByTestId('network-error-fallback').waitFor({ 
      state: 'visible', 
      timeout 
    });
  }

  /**
   * Wait for network error fallback to disappear (network restored)
   */
  async waitForNetworkRestore(timeout: number = 15000): Promise<void> {
    await this.page.getByTestId('network-error-fallback').waitFor({ 
      state: 'hidden', 
      timeout 
    });
  }

  /**
   * Get the current retry count displayed in the UI
   */
  async getRetryCount(): Promise<number> {
    const retryText = await this.page.getByTestId('retry-count').textContent();
    const match = retryText?.match(/(\d+)/);
    return match ? parseInt(match[1], 10) : 0;
  }

  /**
   * Get the current network status text
   */
  async getNetworkStatus(): Promise<string> {
    const statusElement = await this.page.getByTestId('network-status');
    return (await statusElement.textContent()) || '';
  }

  /**
   * Click the retry button and wait for the action to complete
   */
  async clickRetryButton(): Promise<void> {
    const retryButton = this.page.getByTestId('retry-button');
    await retryButton.click();
    
    // Wait for any loading states to settle
    await this.page.waitForTimeout(500);
  }

  /**
   * Click the check connection button
   */
  async clickCheckConnectionButton(): Promise<void> {
    const checkButton = this.page.getByTestId('check-connection-button');
    await checkButton.click();
    
    // Wait for network check to complete
    await this.page.waitForTimeout(1000);
  }

  /**
   * Toggle troubleshooting steps visibility
   */
  async toggleTroubleshootingSteps(): Promise<void> {
    const toggleButton = this.page.getByTestId('troubleshooting-toggle');
    await toggleButton.click();
  }

  /**
   * Assert that troubleshooting steps are visible
   */
  async assertTroubleshootingStepsVisible(): Promise<void> {
    await expect(this.page.getByTestId('troubleshooting-steps')).toBeVisible();
  }

  /**
   * Assert that the retry button is enabled/disabled
   */
  async assertRetryButtonState(enabled: boolean): Promise<void> {
    const retryButton = this.page.getByTestId('retry-button');
    if (enabled) {
      await expect(retryButton).toBeEnabled();
    } else {
      await expect(retryButton).toBeDisabled();
    }
  }

  /**
   * Assert that the check connection button is enabled/disabled
   */
  async assertCheckConnectionButtonState(enabled: boolean): Promise<void> {
    const checkButton = this.page.getByTestId('check-connection-button');
    if (enabled) {
      await expect(checkButton).toBeEnabled();
    } else {
      await expect(checkButton).toBeDisabled();
    }
  }

  /**
   * Simulate browser offline/online events
   */
  async simulateOfflineMode(): Promise<void> {
    await this.page.context().setOffline(true);
    await this.page.waitForTimeout(100);
  }

  async simulateOnlineMode(): Promise<void> {
    await this.page.context().setOffline(false);
    await this.page.waitForTimeout(100);
  }

  /**
   * Throttle network connection
   */
  async throttleNetwork(_downloadThroughput: number = 50000): Promise<void> {
    await this.page.context().route('**/*', async (route) => {
      // Simulate slow network by adding delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      return route.continue();
    });
  }

  /**
   * Clear network throttling
   */
  async clearNetworkThrottling(): Promise<void> {
    await this.page.context().unroute('**/*');
  }

  /**
   * Assert accessibility features
   */
  async assertAccessibilityFeatures(): Promise<void> {
    const errorFallback = this.page.getByTestId('network-error-fallback');
    
    // Check ARIA attributes
    await expect(errorFallback).toHaveAttribute('role', 'alert');
    await expect(errorFallback).toHaveAttribute('aria-live', 'polite');
    
    // Check that buttons have proper labels
    const retryButton = this.page.getByTestId('retry-button');
    await expect(retryButton).toHaveAttribute('aria-label');
    
    const checkButton = this.page.getByTestId('check-connection-button');
    await expect(checkButton).toHaveAttribute('aria-label');
  }

  /**
   * Test keyboard navigation
   */
  async testKeyboardNavigation(): Promise<void> {
    // Tab through interactive elements
    await this.page.keyboard.press('Tab');
    await expect(this.page.getByTestId('retry-button')).toBeFocused();
    
    await this.page.keyboard.press('Tab');
    await expect(this.page.getByTestId('check-connection-button')).toBeFocused();
    
    await this.page.keyboard.press('Tab');
    await expect(this.page.getByTestId('troubleshooting-toggle')).toBeFocused();
    
    // Test Enter key activation
    await this.page.keyboard.press('Enter');
    await this.assertTroubleshootingStepsVisible();
  }

  /**
   * Wait for specific network status text
   */
  async waitForNetworkStatus(expectedStatus: string, timeout: number = 5000): Promise<void> {
    await this.page.waitForFunction(
      (status) => {
        const element = document.querySelector('[data-testid="network-status"]');
        return element?.textContent?.includes(status);
      },
      expectedStatus,
      { timeout }
    );
  }

  /**
   * Assert error message content
   */
  async assertErrorMessage(expectedMessage: string): Promise<void> {
    const errorMessage = this.page.getByTestId('error-message');
    await expect(errorMessage).toContainText(expectedMessage);
  }

  /**
   * Assert retry count matches expected value
   */
  async assertRetryCount(expectedCount: number): Promise<void> {
    const retryCountElement = this.page.getByTestId('retry-count');
    await expect(retryCountElement).toContainText(expectedCount.toString());
  }

  /**
   * Wait for loading states to complete
   */
  async waitForLoadingToComplete(timeout: number = 10000): Promise<void> {
    // Wait for any loading spinners to disappear
    await this.page.waitForFunction(
      () => {
        const loadingElements = document.querySelectorAll('[data-testid*="loading"], .loading, .spinner');
        return loadingElements.length === 0;
      },
      undefined,
      { timeout }
    );
  }

  /**
   * Navigate and trigger network error
   */
  async navigateAndTriggerError(url: string): Promise<void> {
    // Set up network failure before navigation
    await this.setNetworkScenario({ scenario: 'complete-failure' });
    
    // Navigate to the page
    await this.page.goto(url);
    
    // Wait for error fallback to appear
    await this.waitForNetworkErrorFallback();
  }

  /**
   * Restore network and verify recovery
   */
  async restoreNetworkAndVerifyRecovery(): Promise<void> {
    // Reset mock backend to normal operation
    await this.resetMockBackend();
    
    // Click retry button
    await this.clickRetryButton();
    
    // Wait for error fallback to disappear
    await this.waitForNetworkRestore();
  }

  /**
   * Test max retries behavior
   */
  async testMaxRetriesBehavior(maxRetries: number = 3): Promise<void> {
    // Click retry button multiple times
    for (let i = 0; i < maxRetries; i++) {
      await this.clickRetryButton();
      await this.page.waitForTimeout(1000);
    }
    
    // After max retries, button should be disabled
    await this.assertRetryButtonState(false);
    
    // Error message should indicate max retries reached
    await this.assertErrorMessage('Maximum retry attempts reached');
  }

  /**
   * Verify component performance
   */
  async verifyComponentPerformance(): Promise<void> {
    const startTime = Date.now();
    
    // Trigger error and measure render time
    await this.navigateAndTriggerError('/dashboard');
    
    const renderTime = Date.now() - startTime;
    
    // Error fallback should render quickly (within 2 seconds)
    expect(renderTime).toBeLessThan(2000);
    
    // Check that UI is responsive
    await this.assertRetryButtonState(true);
    await this.assertCheckConnectionButtonState(true);
  }
}

/**
 * Test data and scenarios for comprehensive testing
 */
export const NetworkTestScenarios = {
  NORMAL: { scenario: 'normal' as const },
  COMPLETE_FAILURE: { scenario: 'complete-failure' as const },
  INTERMITTENT: { 
    scenario: 'intermittent' as const, 
    options: { failureRate: 0.5 } 
  },
  SLOW_NETWORK: { 
    scenario: 'slow' as const, 
    options: { latencyMs: 5000 } 
  },
  GRADUAL_FAILURE: { scenario: 'gradual-failure' as const },
  RANDOM_ERRORS: { scenario: 'random-errors' as const }
} as const;

/**
 * Common test pages and URLs
 */
export const TestPages = {
  DASHBOARD: '/dashboard',
  USER_PROFILE: '/profile',
  SETTINGS: '/settings',
  API_HEAVY_PAGE: '/api-dashboard',
  FILE_UPLOAD: '/upload'
} as const;

/**
 * Expected error messages for different scenarios
 */
export const ExpectedErrorMessages = {
  NETWORK_ERROR: 'Network connection lost',
  SERVER_ERROR: 'Server temporarily unavailable',
  TIMEOUT_ERROR: 'Request timed out',
  MAX_RETRIES: 'Maximum retry attempts reached'
} as const;