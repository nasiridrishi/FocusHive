/**
 * Page Object Model for Error Handling Testing
 * Provides utilities for testing various error scenarios across the FocusHive application
 */

import {expect, Locator, Page} from '@playwright/test';
import {SELECTORS, TIMEOUTS} from '../helpers/test-data';

export interface NetworkCondition {
  name: string;
  downloadSpeed: number; // KB/s
  uploadSpeed: number; // KB/s
  latency: number; // ms
}

export interface ErrorBoundaryState {
  hasError: boolean;
  errorMessage: string;
  componentStack: string;
  timestamp: number;
}

export class ErrorHandlingPage {
  readonly page: Page;

  // Error display elements
  readonly errorMessage: Locator;
  readonly errorBoundary: Locator;
  readonly retryButton: Locator;
  readonly refreshButton: Locator;
  readonly offlineIndicator: Locator;
  readonly loadingSpinner: Locator;

  // Form elements for testing validation errors
  readonly formInputs: Locator;
  readonly submitButton: Locator;
  readonly validationErrors: Locator;

  // Network and connectivity
  readonly connectionStatus: Locator;
  readonly reconnectButton: Locator;

  // Notifications and toast messages
  readonly toastContainer: Locator;
  readonly successToast: Locator;
  readonly errorToast: Locator;
  readonly warningToast: Locator;

  constructor(page: Page) {
    this.page = page;

    // Error display elements
    this.errorMessage = page.locator(SELECTORS.ERROR_MESSAGE);
    this.errorBoundary = page.locator('[data-testid="error-boundary"], .error-boundary');
    this.retryButton = page.locator('button:has-text("Retry"), button:has-text("Try Again"), [data-testid="retry-button"]');
    this.refreshButton = page.locator('button:has-text("Refresh"), button:has-text("Reload"), [data-testid="refresh-button"]');
    this.offlineIndicator = page.locator('[data-testid="offline-indicator"], .offline-indicator');
    this.loadingSpinner = page.locator(SELECTORS.LOADING_SPINNER);

    // Form elements
    this.formInputs = page.locator('input, textarea, select');
    this.submitButton = page.locator('button[type="submit"]');
    this.validationErrors = page.locator('.MuiFormHelperText-error, .error-message, [role="alert"]');

    // Network and connectivity
    this.connectionStatus = page.locator('[data-testid="connection-status"], .connection-status');
    this.reconnectButton = page.locator('button:has-text("Reconnect"), [data-testid="reconnect-button"]');

    // Notifications
    this.toastContainer = page.locator('[data-testid="toast-container"], .MuiSnackbar-root, .toast-container');
    this.successToast = page.locator('.MuiAlert-standardSuccess, .success-toast, [data-severity="success"]');
    this.errorToast = page.locator('.MuiAlert-standardError, .error-toast, [data-severity="error"]');
    this.warningToast = page.locator('.MuiAlert-standardWarning, .warning-toast, [data-severity="warning"]');
  }

  /**
   * Navigate to a specific page for error testing
   */
  async navigateToPage(url: string): Promise<void> {
    await this.page.goto(url);
    await this.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Simulate network conditions
   */
  async setNetworkConditions(condition: NetworkCondition): Promise<void> {
    const cdpSession = await this.page.context().newCDPSession(this.page);

    await cdpSession.send('Network.emulateNetworkConditions', {
      offline: condition.downloadSpeed === 0,
      downloadThroughput: condition.downloadSpeed * 1024, // Convert KB/s to bytes/s
      uploadThroughput: condition.uploadSpeed * 1024,
      latency: condition.latency,
    });
  }

  /**
   * Simulate network disconnection
   */
  async goOffline(): Promise<void> {
    await this.setNetworkConditions({
      name: 'Offline',
      downloadSpeed: 0,
      uploadSpeed: 0,
      latency: 0,
    });
  }

  /**
   * Restore network connection
   */
  async goOnline(): Promise<void> {
    await this.setNetworkConditions({
      name: 'Online',
      downloadSpeed: 1000, // 1MB/s
      uploadSpeed: 1000,
      latency: 20,
    });
  }

  /**
   * Simulate slow network (3G conditions)
   */
  async simulate3G(): Promise<void> {
    await this.setNetworkConditions({
      name: '3G',
      downloadSpeed: 100, // 100 KB/s
      uploadSpeed: 50,    // 50 KB/s
      latency: 300,       // 300ms
    });
  }

  /**
   * Simulate very slow network (2G conditions)
   */
  async simulate2G(): Promise<void> {
    await this.setNetworkConditions({
      name: '2G',
      downloadSpeed: 20,  // 20 KB/s
      uploadSpeed: 10,    // 10 KB/s
      latency: 800,       // 800ms
    });
  }

  /**
   * Mock API response with specific status code
   */
  async mockApiResponse(endpoint: string, statusCode: number, responseData?: unknown): Promise<void> {
    await this.page.route(`**${endpoint}`, (route) => {
      const response = responseData || {
        message: `HTTP ${statusCode} error`,
        status: statusCode,
        timestamp: new Date().toISOString()
      };

      route.fulfill({
        status: statusCode,
        contentType: 'application/json',
        body: JSON.stringify(response),
      });
    });
  }

  /**
   * Mock network timeout for specific endpoint
   */
  async mockNetworkTimeout(endpoint: string, timeoutMs = 30000): Promise<void> {
    await this.page.route(`**${endpoint}`, async (route) => {
      await new Promise(resolve => setTimeout(resolve, timeoutMs));
      route.abort('timeout');
    });
  }

  /**
   * Mock DNS failure
   */
  async mockDnsFailure(domain: string): Promise<void> {
    await this.page.route(`**${domain}**`, (route) => {
      route.abort('namenotresolved');
    });
  }

  /**
   * Mock SSL certificate error
   */
  async mockSslError(endpoint: string): Promise<void> {
    await this.page.route(`**${endpoint}`, (route) => {
      route.abort('invalidcert');
    });
  }

  /**
   * Wait for error message to appear
   */
  async waitForError(timeout = TIMEOUTS.MEDIUM): Promise<void> {
    await expect(this.errorMessage).toBeVisible({timeout});
  }

  /**
   * Wait for error boundary to catch error
   */
  async waitForErrorBoundary(timeout = TIMEOUTS.MEDIUM): Promise<void> {
    await expect(this.errorBoundary).toBeVisible({timeout});
  }

  /**
   * Verify error message content
   */
  async verifyErrorMessage(expectedMessage: string | RegExp): Promise<void> {
    if (typeof expectedMessage === 'string') {
      await expect(this.errorMessage).toContainText(expectedMessage);
    } else {
      const errorText = await this.errorMessage.textContent();
      expect(errorText).toMatch(expectedMessage);
    }
  }

  /**
   * Verify error boundary is displayed
   */
  async verifyErrorBoundaryDisplayed(): Promise<void> {
    await expect(this.errorBoundary).toBeVisible();
  }

  /**
   * Verify retry mechanism is available
   */
  async verifyRetryMechanismAvailable(): Promise<void> {
    await expect(this.retryButton.or(this.refreshButton)).toBeVisible();
  }

  /**
   * Click retry button
   */
  async clickRetry(): Promise<void> {
    await this.retryButton.click();
  }

  /**
   * Click refresh button
   */
  async clickRefresh(): Promise<void> {
    await this.refreshButton.click();
  }

  /**
   * Verify offline indicator is shown
   */
  async verifyOfflineIndicator(): Promise<void> {
    await expect(this.offlineIndicator).toBeVisible();
  }

  /**
   * Verify connection status
   */
  async verifyConnectionStatus(status: 'online' | 'offline'): Promise<void> {
    if (status === 'offline') {
      await expect(this.offlineIndicator).toBeVisible();
    } else {
      await expect(this.offlineIndicator).not.toBeVisible();
    }
  }

  /**
   * Verify form validation errors
   */
  async verifyFormValidationErrors(expectedCount?: number): Promise<void> {
    await expect(this.validationErrors.first()).toBeVisible();

    if (expectedCount !== undefined) {
      await expect(this.validationErrors).toHaveCount(expectedCount);
    }
  }

  /**
   * Verify specific validation error message
   */
  async verifyValidationError(fieldName: string, errorMessage: string): Promise<void> {
    const fieldError = this.page.locator(`[name="${fieldName}"] + .MuiFormHelperText-error, [data-testid="${fieldName}-error"]`);
    await expect(fieldError).toContainText(errorMessage);
  }

  /**
   * Fill form with invalid data
   */
  async fillFormWithInvalidData(data: Record<string, string>): Promise<void> {
    for (const [fieldName, value] of Object.entries(data)) {
      const field = this.page.locator(`[name="${fieldName}"], #${fieldName}`);
      await field.fill(value);
    }
  }

  /**
   * Submit form and wait for response
   */
  async submitFormAndWait(): Promise<void> {
    await this.submitButton.click();
    await this.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});
  }

  /**
   * Verify toast notification
   */
  async verifyToastNotification(type: 'success' | 'error' | 'warning', message?: string): Promise<void> {
    const toast = type === 'success' ? this.successToast :
        type === 'error' ? this.errorToast :
            this.warningToast;

    await expect(toast).toBeVisible();

    if (message) {
      await expect(toast).toContainText(message);
    }
  }

  /**
   * Verify toast auto-dismissal
   */
  async verifyToastAutoDismissal(type: 'success' | 'error' | 'warning', timeoutMs = 5000): Promise<void> {
    const toast = type === 'success' ? this.successToast :
        type === 'error' ? this.errorToast :
            this.warningToast;

    await expect(toast).toBeVisible();
    await expect(toast).not.toBeVisible({timeout: timeoutMs + 1000});
  }

  /**
   * Trigger JavaScript runtime error
   */
  async triggerRuntimeError(): Promise<void> {
    await this.page.evaluate(() => {
      throw new Error('E2E Test Runtime Error');
    });
  }

  /**
   * Check error boundary recovery
   */
  async verifyErrorBoundaryRecovery(): Promise<void> {
    await this.verifyErrorBoundaryDisplayed();
    await this.clickRetry();
    await expect(this.errorBoundary).not.toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Simulate memory pressure
   */
  async simulateMemoryPressure(): Promise<void> {
    await this.page.evaluate(() => {
      // Create large arrays to consume memory
      const arrays: unknown[][] = [];
      try {
        for (let i = 0; i < 1000; i++) {
          arrays.push(new Array(100000).fill(i));
        }
      } catch (error) {
        console.warn('Memory pressure simulation triggered:', error);
      }
    });
  }

  /**
   * Test lazy loading failure recovery
   */
  async testLazyLoadingFailure(componentName: string): Promise<void> {
    // Mock chunk loading failure
    await this.page.route('**/*.js', (route) => {
      if (route.request().url().includes(componentName)) {
        route.abort('connectionreset');
      } else {
        route.continue();
      }
    });
  }

  /**
   * Verify graceful degradation
   */
  async verifyGracefulDegradation(): Promise<void> {
    // Check if fallback UI is displayed when main functionality fails
    const fallbackElements = this.page.locator('[data-testid="fallback"], .fallback-ui, .error-fallback');
    await expect(fallbackElements.first()).toBeVisible();
  }

  /**
   * Test file upload error handling
   */
  async testFileUploadError(fileInput: string, errorType: 'size' | 'type' | 'network'): Promise<void> {
    const input = this.page.locator(fileInput);

    switch (errorType) {
      case 'size':
        // Create a large file blob
        await this.page.evaluate((selector) => {
          const input = document.querySelector(selector) as HTMLInputElement;
          const file = new File(['x'.repeat(10 * 1024 * 1024)], 'large-file.txt', {type: 'text/plain'});
          const dt = new DataTransfer();
          dt.items.add(file);
          input.files = dt.files;
        }, fileInput);
        break;
      case 'type':
        await input.setInputFiles({
          name: 'test.exe',
          mimeType: 'application/octet-stream',
          buffer: Buffer.from('test')
        });
        break;
      case 'network':
        await this.mockApiResponse('/api/upload', 500, {message: 'Upload failed'});
        await input.setInputFiles({
          name: 'test.txt',
          mimeType: 'text/plain',
          buffer: Buffer.from('test')
        });
        break;
    }
  }

  /**
   * Verify data loss prevention
   */
  async verifyDataLossPrevention(formSelector: string): Promise<void> {
    const form = this.page.locator(formSelector);

    // Fill form with test data
    const testData = {name: 'Test Name', email: 'test@example.com', message: 'Test message'};

    for (const [field, value] of Object.entries(testData)) {
      await form.locator(`[name="${field}"]`).fill(value);
    }

    // Simulate error condition
    await this.goOffline();
    await this.submitButton.click();

    // Verify form data is preserved
    for (const [field, value] of Object.entries(testData)) {
      await expect(form.locator(`[name="${field}"]`)).toHaveValue(value);
    }

    // Restore connection
    await this.goOnline();
  }

  /**
   * Check WebSocket reconnection
   */
  async verifyWebSocketReconnection(): Promise<void> {
    // Disconnect network
    await this.goOffline();
    await this.page.waitForTimeout(2000);

    // Restore connection
    await this.goOnline();

    // Verify reconnection indicator or successful data sync
    const reconnectedIndicator = this.page.locator('[data-testid="connected"], .connection-restored');
    await expect(reconnectedIndicator).toBeVisible({timeout: TIMEOUTS.LONG});
  }

  /**
   * Test concurrent error handling
   */
  async testConcurrentErrors(): Promise<void> {
    // Trigger multiple errors simultaneously
    const promises = [
      this.mockApiResponse('/api/user', 500),
      this.mockApiResponse('/api/data', 404),
      this.mockApiResponse('/api/settings', 403),
    ];

    await Promise.all(promises);

    // Verify that all errors are handled appropriately
    await expect(this.errorMessage).toBeVisible();
  }

  /**
   * Verify accessibility of error states
   */
  async verifyErrorAccessibility(): Promise<void> {
    await expect(this.errorMessage).toHaveAttribute('role', 'alert');
    await expect(this.errorMessage).toHaveAttribute('aria-live', 'polite');

    if (await this.retryButton.isVisible()) {
      await expect(this.retryButton).toHaveAttribute('aria-label');
    }
  }

  /**
   * Test rate limiting error handling
   */
  async testRateLimitingError(endpoint: string): Promise<void> {
    await this.mockApiResponse(endpoint, 429, {
      message: 'Rate limit exceeded',
      retryAfter: 60,
      remaining: 0
    });
  }

  /**
   * Verify loading state management
   */
  async verifyLoadingStateHandling(): Promise<void> {
    // Trigger loading state
    await this.page.click('button:has-text("Load Data")');

    // Verify loading indicator appears
    await expect(this.loadingSpinner).toBeVisible();

    // Simulate error during loading
    await this.mockApiResponse('/api/data', 500);

    // Verify loading state is cleared and error is shown
    await expect(this.loadingSpinner).not.toBeVisible({timeout: TIMEOUTS.MEDIUM});
    await expect(this.errorMessage).toBeVisible();
  }
}