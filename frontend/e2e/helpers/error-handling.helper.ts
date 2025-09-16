/**
 * Error Handling Helper Utilities for E2E Testing
 * Provides comprehensive error simulation and recovery testing capabilities
 */

import {BrowserContext, expect, Page} from '@playwright/test';
import {API_ENDPOINTS, TIMEOUTS} from './test-data';

export interface NetworkConditions {
  offline: boolean;
  downloadThroughput: number;
  uploadThroughput: number;
  latency: number;
}

export interface ErrorScenario {
  name: string;
  type: 'network' | 'api' | 'client' | 'validation' | 'auth' | 'websocket';
  description: string;
  setup: () => Promise<void>;
  verify: () => Promise<void>;
  cleanup?: () => Promise<void>;
}

export interface ErrorEvent extends Event {
  target: {
    error: Error;
  };
}

export interface MockWindow extends Window {
  mockWebSocketClose?: () => void;
  mockHeartbeatTimeout?: boolean;
}

export interface PerformanceMemory {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
}

export interface PerformanceWithMemory extends Performance {
  memory?: PerformanceMemory;
}

export interface HTMLInputElementWithValue extends Element {
  value: string;
}

export interface ReactFiberNode {
  _reactInternalFiber?: unknown;
}

export interface ComponentElement extends Element, ReactFiberNode {
}

export interface ApiError {
  status: number;
  message: string;
  code?: string;
  details?: Record<string, unknown>;
  retryAfter?: number;
}

export interface RecoveryMetrics {
  timeToDetection: number;
  timeToRecovery: number;
  userActionsRequired: number;
  dataLoss: boolean;
}

export class ErrorHandlingHelper {
  constructor(private page: Page, private context?: BrowserContext) {
  }

  /**
   * Network Error Simulation
   */
  async simulateNetworkErrors(): Promise<void> {
    return {
      // Connection timeout
      timeout: async (endpoint: string, timeoutMs = 30000) => {
        await this.page.route(`**${endpoint}**`, async (route) => {
          await this.page.waitForTimeout(timeoutMs);
          route.abort('timeout');
        });
      },

      // Connection refused (server down)
      connectionRefused: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.abort('connectionrefused');
        });
      },

      // Connection reset
      connectionReset: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.abort('connectionreset');
        });
      },

      // DNS failure
      dnsFailure: async (domain: string) => {
        await this.page.route(`**${domain}**`, (route) => {
          route.abort('namenotresolved');
        });
      },

      // SSL/TLS errors
      sslError: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.abort('invalidcert');
        });
      },

      // Intermittent connectivity (packet loss)
      intermittentConnectivity: async (endpoint: string, failureRate = 0.3) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          if (Math.random() < failureRate) {
            route.abort('connectionreset');
          } else {
            route.continue();
          }
        });
      },

      // Slow network conditions
      slowNetwork: async (downloadKbps = 50, uploadKbps = 25, latencyMs = 500) => {
        const cdpSession = await this.context?.newCDPSession(this.page);
        await cdpSession?.send('Network.emulateNetworkConditions', {
          offline: false,
          downloadThroughput: downloadKbps * 1024 / 8, // Convert to bytes/s
          uploadThroughput: uploadKbps * 1024 / 8,
          latency: latencyMs,
        });
      },

      // Complete network disconnect
      goOffline: async () => {
        const cdpSession = await this.context?.newCDPSession(this.page);
        await cdpSession?.send('Network.emulateNetworkConditions', {
          offline: true,
          downloadThroughput: 0,
          uploadThroughput: 0,
          latency: 0,
        });
      },

      // Restore normal network
      goOnline: async () => {
        const cdpSession = await this.context?.newCDPSession(this.page);
        await cdpSession?.send('Network.emulateNetworkConditions', {
          offline: false,
          downloadThroughput: -1, // No throttling
          uploadThroughput: -1,
          latency: 0,
        });
      },
    };
  }

  /**
   * API Error Response Simulation
   */
  async simulateApiErrors(): Promise<void> {
    return {
      // 400 Bad Request
      badRequest: async (endpoint: string, errors?: Record<string, string>) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Bad Request',
              code: 'VALIDATION_ERROR',
              errors: errors || {field: 'Invalid value'},
            }),
          });
        });
      },

      // 401 Unauthorized
      unauthorized: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Authentication required',
              code: 'UNAUTHORIZED',
            }),
          });
        });
      },

      // 403 Forbidden
      forbidden: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 403,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Access denied',
              code: 'FORBIDDEN',
            }),
          });
        });
      },

      // 404 Not Found
      notFound: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 404,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Resource not found',
              code: 'NOT_FOUND',
            }),
          });
        });
      },

      // 429 Rate Limiting
      rateLimited: async (endpoint: string, retryAfter = 60) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 429,
            contentType: 'application/json',
            headers: {'Retry-After': retryAfter.toString()},
            body: JSON.stringify({
              message: 'Rate limit exceeded',
              code: 'RATE_LIMITED',
              retryAfter,
            }),
          });
        });
      },

      // 500 Internal Server Error
      serverError: async (endpoint: string, includeStack = false) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          const response: ApiError = {
            status: 500,
            message: 'Internal server error',
            code: 'INTERNAL_ERROR',
          };

          if (includeStack) {
            response.details = {
              stack: 'Error at sensitive-function.js:123',
              database: 'postgres://user:pass@localhost/db',
            };
          }

          route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify(response),
          });
        });
      },

      // 502 Bad Gateway
      badGateway: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 502,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Bad gateway',
              code: 'BAD_GATEWAY',
            }),
          });
        });
      },

      // 503 Service Unavailable
      serviceUnavailable: async (endpoint: string, retryAfter = 120) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 503,
            contentType: 'application/json',
            headers: {'Retry-After': retryAfter.toString()},
            body: JSON.stringify({
              message: 'Service temporarily unavailable',
              code: 'SERVICE_UNAVAILABLE',
              retryAfter,
            }),
          });
        });
      },

      // Malformed JSON response
      malformedResponse: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: '{ invalid json response',
          });
        });
      },

      // Empty response
      emptyResponse: async (endpoint: string) => {
        await this.page.route(`**${endpoint}**`, (route) => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: '',
          });
        });
      },
    };
  }

  /**
   * Client-Side Error Simulation
   */
  async simulateClientErrors(): Promise<void> {
    return {
      // JavaScript runtime error
      runtimeError: async (errorMessage = 'Test runtime error') => {
        await this.page.evaluate((message) => {
          setTimeout(() => {
            throw new Error(message);
          }, 100);
        }, errorMessage);
      },

      // Memory exhaustion
      memoryExhaustion: async () => {
        await this.page.evaluate(() => {
          const arrays: number[][] = [];
          try {
            for (let i = 0; i < 10000; i++) {
              arrays.push(new Array(100000).fill(i));
            }
          } catch (error) {
            console.warn('Memory exhaustion triggered:', error);
          }
        });
      },

      // Chunk loading failure (dynamic imports)
      chunkLoadFailure: async (chunkPattern: string) => {
        await this.page.route(`**/${chunkPattern}*.js`, (route) => {
          route.abort('connectionreset');
        });
      },

      // LocalStorage quota exceeded
      storageQuotaExceeded: async () => {
        await this.page.evaluate(() => {
          try {
            const data = 'x'.repeat(1024); // 1KB string
            for (let i = 0; i < 10000; i++) {
              localStorage.setItem(`test-data-${i}`, data);
            }
          } catch (error) {
            console.warn('Storage quota exceeded:', error);
          }
        });
      },

      // IndexedDB failure
      indexedDbFailure: async () => {
        await this.page.evaluate(() => {
          // Mock IndexedDB to fail
          Object.defineProperty(window, 'indexedDB', {
            value: {
              open: () => {
                const request = new EventTarget();
                setTimeout(() => {
                  const event = new Event('error') as ErrorEvent;
                  event.target = {error: new Error('IndexedDB failed')};
                  request.dispatchEvent(event);
                }, 100);
                return request;
              },
            },
            configurable: true,
          });
        });
      },

      // Service Worker registration failure
      serviceWorkerFailure: async () => {
        await this.page.evaluate(() => {
          if ('serviceWorker' in navigator) {
            Object.defineProperty(navigator.serviceWorker, 'register', {
              value: () => Promise.reject(new Error('Service Worker registration failed')),
            });
          }
        });
      },
    };
  }

  /**
   * WebSocket Error Simulation
   */
  async simulateWebSocketErrors(): Promise<void> {
    return {
      // Connection failure
      connectionFailure: async (wsUrl: string) => {
        await this.page.route(wsUrl, (route) => {
          route.abort('connectionrefused');
        });
      },

      // Unexpected disconnection
      unexpectedDisconnection: async () => {
        await this.page.evaluate(() => {
          // Find existing WebSocket connections and close them
          (window as MockWindow).mockWebSocketClose?.();
        });
      },

      // Message delivery failure
      messageDeliveryFailure: async () => {
        await this.page.evaluate(() => {
          // Intercept WebSocket send method
          const originalWebSocket = window.WebSocket;
          window.WebSocket = class extends originalWebSocket {
            send(data: string | ArrayBufferLike | Blob | ArrayBufferView): void {
              // Simulate message loss
              if (Math.random() < 0.3) {
                console.warn('Simulating WebSocket message loss');
                return;
              }
              super.send(data);
            }
          } as typeof WebSocket;
        });
      },

      // Heartbeat timeout
      heartbeatTimeout: async () => {
        await this.page.evaluate(() => {
          // Simulate missing heartbeat responses
          (window as MockWindow).mockHeartbeatTimeout = true;
        });
      },
    };
  }

  /**
   * Form Validation Error Testing
   */
  async testFormValidationErrors(): Promise<void> {
    return {
      // Required field validation
      requiredFieldValidation: async (fieldName: string) => {
        const field = this.page.locator(`[name="${fieldName}"]`);
        await field.fill('');
        await field.blur();

        const errorMessage = this.page.locator(`[name="${fieldName}"] + .error, [data-testid="${fieldName}-error"]`);
        await expect(errorMessage).toBeVisible();
        await expect(errorMessage).toContainText(/required|mandatory/i);
      },

      // Email format validation
      emailFormatValidation: async (emailField: string) => {
        const field = this.page.locator(`[name="${emailField}"]`);
        const invalidEmails = ['invalid-email', 'test@', '@example.com', 'test.example.com'];

        for (const email of invalidEmails) {
          await field.fill(email);
          await field.blur();

          const errorMessage = this.page.locator(`[name="${emailField}"] + .error`);
          await expect(errorMessage).toBeVisible();
          await expect(errorMessage).toContainText(/email|format|invalid/i);
        }
      },

      // Password strength validation
      passwordStrengthValidation: async (passwordField: string) => {
        const field = this.page.locator(`[name="${passwordField}"]`);
        const weakPasswords = ['123', 'password', 'abc123', '12345678'];

        for (const password of weakPasswords) {
          await field.fill(password);
          await field.blur();

          const errorMessage = this.page.locator(`[name="${passwordField}"] + .error`);
          await expect(errorMessage).toBeVisible();
        }
      },

      // File upload validation
      fileUploadValidation: async (fileInput: string, validationRules: {
        maxSize?: number;
        allowedTypes?: string[]
      }) => {
        const input = this.page.locator(fileInput);

        if (validationRules.maxSize) {
          // Test file size limit
          const largeFileBuffer = Buffer.alloc(validationRules.maxSize + 1024, 'x');
          await input.setInputFiles({
            name: 'large-file.txt',
            mimeType: 'text/plain',
            buffer: largeFileBuffer,
          });

          await expect(this.page.locator('.error, [role="alert"]')).toContainText(/size|large|limit/i);
        }

        if (validationRules.allowedTypes) {
          // Test file type restriction
          await input.setInputFiles({
            name: 'test.exe',
            mimeType: 'application/octet-stream',
            buffer: Buffer.from('test'),
          });

          await expect(this.page.locator('.error, [role="alert"]')).toContainText(/type|format|allowed/i);
        }
      },
    };
  }

  /**
   * Recovery Testing Utilities
   */
  async testRecoveryMechanisms(): Promise<RecoveryMetrics> {
    const startTime = Date.now();
    let detectionTime = 0;
    let recoveryTime = 0;
    let userActions = 0;
    let dataLoss = false;

    // Simulate error condition
    const networkSim = await this.simulateNetworkErrors();
    await networkSim.goOffline();

    // Wait for error detection
    try {
      await expect(this.page.locator('[role="alert"], .error-message')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      detectionTime = Date.now() - startTime;
    } catch {
      detectionTime = TIMEOUTS.MEDIUM;
    }

    // Check for automatic retry mechanisms
    await this.page.waitForTimeout(2000);

    // Test user-initiated recovery
    const retryButton = this.page.locator('button:has-text("Retry"), button:has-text("Try Again")');
    if (await retryButton.isVisible()) {
      userActions++;
      await retryButton.click();
    }

    // Restore network and test recovery
    await networkSim.goOnline();

    try {
      await expect(this.page.locator('[role="alert"], .error-message')).not.toBeVisible({timeout: TIMEOUTS.MEDIUM});
      recoveryTime = Date.now() - startTime - detectionTime;
    } catch {
      recoveryTime = TIMEOUTS.MEDIUM;
    }

    // Check for data loss by verifying form fields or application state
    const forms = await this.page.locator('form').count();
    if (forms > 0) {
      const formValues = await this.page.evaluate(() => {
        const inputs = Array.from(document.querySelectorAll('input[type="text"], textarea')) as HTMLInputElementWithValue[];
        return inputs.some((input: HTMLInputElementWithValue) => input.value.trim() === '');
      });
      dataLoss = formValues;
    }

    return {
      timeToDetection: detectionTime,
      timeToRecovery: recoveryTime,
      userActionsRequired: userActions,
      dataLoss,
    };
  }

  /**
   * Error Boundary Testing
   */
  async testErrorBoundaries(): Promise<void> {
    return {
      // Trigger component error
      triggerComponentError: async (componentSelector: string) => {
        await this.page.evaluate((selector) => {
          const component = document.querySelector(selector) as ComponentElement | null;
          if (component) {
            // Trigger error in React component
            component._reactInternalFiber = null;
            component.dispatchEvent(new Event('error'));
          }
        }, componentSelector);
      },

      // Verify error boundary catch
      verifyErrorBoundaryCatch: async () => {
        const errorBoundary = this.page.locator('[data-testid="error-boundary"], .error-boundary');
        await expect(errorBoundary).toBeVisible({timeout: TIMEOUTS.MEDIUM});

        const errorMessage = errorBoundary.locator('.error-message, [role="alert"]');
        await expect(errorMessage).toBeVisible();
      },

      // Test error boundary recovery
      testErrorBoundaryRecovery: async () => {
        const retryButton = this.page.locator('button:has-text("Retry"), button:has-text("Try Again")');
        await expect(retryButton).toBeVisible();
        await retryButton.click();

        const errorBoundary = this.page.locator('[data-testid="error-boundary"], .error-boundary');
        await expect(errorBoundary).not.toBeVisible({timeout: TIMEOUTS.MEDIUM});
      },
    };
  }

  /**
   * Accessibility Testing for Error States
   */
  async testErrorAccessibility(): Promise<void> {
    return {
      // Verify ARIA attributes for errors
      verifyErrorAria: async () => {
        const errorMessages = this.page.locator('[role="alert"]');
        const count = await errorMessages.count();

        for (let i = 0; i < count; i++) {
          const error = errorMessages.nth(i);
          await expect(error).toHaveAttribute('role', 'alert');
        }
      },

      // Test screen reader announcements
      testScreenReaderAnnouncements: async () => {
        const ariaLive = this.page.locator('[aria-live]');
        await expect(ariaLive).toBeVisible();

        const liveValue = await ariaLive.getAttribute('aria-live');
        expect(['polite', 'assertive']).toContain(liveValue);
      },

      // Verify focus management during errors
      testFocusManagement: async () => {
        // Trigger error
        await this.page.keyboard.press('Tab');

        // Verify focus is managed appropriately
        const focusedElement = this.page.locator(':focus');
        await expect(focusedElement).toBeVisible();
      },
    };
  }

  /**
   * Performance Impact Testing
   */
  async measureErrorHandlingPerformance(): Promise<{
    errorDetectionTime: number;
    uiResponseTime: number;
    memoryUsage: number;
  }> {
    // Start performance measurement
    await this.page.evaluate(() => performance.mark('error-test-start'));

    // Trigger error
    const networkSim = await this.simulateNetworkErrors();
    await networkSim.timeout(API_ENDPOINTS.LOGIN, 100);

    // Measure detection time
    const startTime = Date.now();
    await expect(this.page.locator('[role="alert"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    const detectionTime = Date.now() - startTime;

    // Measure UI response time
    await this.page.evaluate(() => performance.mark('ui-response-start'));
    const retryButton = this.page.locator('button:has-text("Retry")');
    if (await retryButton.isVisible()) {
      await retryButton.click();
    }
    await this.page.evaluate(() => performance.mark('ui-response-end'));

    // Get performance metrics
    const metrics = await this.page.evaluate(() => {
      const entries = performance.getEntriesByType('measure');
      const performanceWithMemory = performance as PerformanceWithMemory;
      const memory = performanceWithMemory.memory;

      return {
        uiResponseTime: entries.length > 0 ? entries[entries.length - 1].duration : 0,
        memoryUsage: memory ? memory.usedJSHeapSize : 0,
      };
    });

    return {
      errorDetectionTime: detectionTime,
      uiResponseTime: metrics.uiResponseTime,
      memoryUsage: metrics.memoryUsage,
    };
  }

  /**
   * Cleanup helper
   */
  async cleanup(): Promise<void> {
    // Restore normal network conditions
    const cdpSession = await this.context?.newCDPSession(this.page);
    await cdpSession?.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: -1,
      uploadThroughput: -1,
      latency: 0,
    });

    // Clear all route handlers
    await this.page.unrouteAll();

    // Clear storage if needed
    await this.page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  }
}