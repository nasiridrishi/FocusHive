/**
 * Server Error Handling E2E Tests for FocusHive
 * Comprehensive testing of HTTP status code handling and server error recovery
 *
 * Test Coverage:
 * - 4xx Client Error responses (400, 401, 403, 404, 409, 422, 429)
 * - 5xx Server Error responses (500, 501, 502, 503, 504)
 * - Database connection failures
 * - Microservice communication failures
 * - Rate limiting scenarios
 * - Service degradation patterns
 */

import {BrowserContext, expect, Page, test} from '@playwright/test';
import {ErrorHandlingHelper} from '../../helpers/error-handling.helper';
import {AuthHelper} from '../../helpers/auth-helpers';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Server Error Handling', () => {
  let page: Page;
  let context: BrowserContext;
  let errorHelper: ErrorHandlingHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page: testPage, context: testContext}) => {
    page = testPage;
    context = testContext;
    errorHelper = new ErrorHandlingHelper(page, context);
    authHelper = new AuthHelper(page);

    validateTestEnvironment();

    // Set up API response monitoring
    await errorHelper.setupApiMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
    await errorHelper.clearApiMocks();
  });

  test.describe('4xx Client Errors', () => {
    test('should handle 400 Bad Request with validation feedback', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock 400 response for hive creation
      await context.route('**/api/hives', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'Bad Request',
              message: 'Invalid hive configuration',
              details: {
                name: 'Hive name must be between 3-50 characters',
                maxParticipants: 'Must be between 2-20 participants'
              }
            })
          });
        } else {
          await route.continue();
        }
      });

      // Attempt to create hive with invalid data
      await page.click('[data-testid="create-hive-btn"]');
      await page.fill('[data-testid="hive-name"]', 'AB'); // Too short
      await page.fill('[data-testid="max-participants"]', '25'); // Too high
      await page.click('[data-testid="submit-hive"]');

      // Verify detailed validation errors are shown
      await expect(page.locator('[data-testid="validation-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="name-error"]')).toContainText('3-50 characters');
      await expect(page.locator('[data-testid="participants-error"]')).toContainText('2-20 participants');

      // Verify form remains open for correction
      await expect(page.locator('[data-testid="hive-form"]')).toBeVisible();
      await expect(page.locator('[data-testid="submit-hive"]')).toBeEnabled();
    });

    test('should handle 401 Unauthorized with login redirect', async () => {
      // Start with valid session
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock 401 response
      await context.route('**/api/hives/**', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Unauthorized',
            message: 'Token expired or invalid'
          })
        });
      });

      // Attempt protected action
      await page.click('[data-testid="hive-settings-123"]');

      // Should redirect to login with message
      await expect(page.locator('[data-testid="session-expired"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.url()).toContain('/login');

      // Verify return URL is preserved
      const returnUrl = new URL(page.url()).searchParams.get('returnUrl');
      expect(returnUrl).toContain('/dashboard');

      // Verify helpful error message
      await expect(page.locator('[data-testid="auth-error-message"]')).toContainText(/session expired|please log in/i);
    });

    test('should handle 403 Forbidden with clear permission messages', async () => {
      await authHelper.login(TEST_USERS.LIMITED_USER.username, TEST_USERS.LIMITED_USER.password);
      await page.goto('/admin');

      // Mock 403 response
      await context.route('**/api/admin/**', async (route) => {
        await route.fulfill({
          status: 403,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Forbidden',
            message: 'Insufficient permissions for admin access',
            requiredRole: 'ADMIN',
            userRole: 'USER'
          })
        });
      });

      // Should show permission denied message
      await expect(page.locator('[data-testid="permission-denied"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify specific permission details
      const permissionMessage = page.locator('[data-testid="permission-message"]');
      await expect(permissionMessage).toContainText(/insufficient permissions/i);
      await expect(permissionMessage).toContainText(/admin access/i);

      // Should offer appropriate next steps
      await expect(page.locator('[data-testid="contact-admin-btn"]')).toBeVisible();
      await expect(page.locator('[data-testid="return-dashboard-btn"]')).toBeVisible();
    });

    test('should handle 404 Not Found with helpful navigation', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock 404 response for specific hive
      await context.route('**/api/hives/nonexistent-hive', async (route) => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Not Found',
            message: 'Hive not found or has been deleted',
            resourceId: 'nonexistent-hive'
          })
        });
      });

      await page.goto('/hive/nonexistent-hive');

      // Should show 404 error page
      await expect(page.locator('[data-testid="not-found-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify helpful error message
      const errorMessage = page.locator('[data-testid="not-found-message"]');
      await expect(errorMessage).toContainText(/not found|deleted/i);

      // Should offer navigation options
      await expect(page.locator('[data-testid="go-to-dashboard"]')).toBeVisible();
      await expect(page.locator('[data-testid="browse-hives"]')).toBeVisible();
      await expect(page.locator('[data-testid="search-hives"]')).toBeVisible();

      // Search should be functional
      await page.fill('[data-testid="search-hives-input"]', 'test');
      await page.click('[data-testid="search-submit"]');
      await expect(page.url()).toContain('/hives?search=test');
    });

    test('should handle 409 Conflict with resolution options', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123/settings');

      // Mock 409 conflict response
      await context.route('**/api/hives/123', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 409,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'Conflict',
              message: 'Hive has been modified by another user',
              conflictType: 'CONCURRENT_MODIFICATION',
              lastModified: '2024-01-01T12:00:00Z',
              modifiedBy: 'other-user@example.com'
            })
          });
        } else {
          await route.continue();
        }
      });

      // Modify settings
      await page.fill('[data-testid="hive-description"]', 'Updated description');
      await page.click('[data-testid="save-settings"]');

      // Should show conflict resolution dialog
      await expect(page.locator('[data-testid="conflict-dialog"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify conflict details
      const conflictDetails = page.locator('[data-testid="conflict-details"]');
      await expect(conflictDetails).toContainText('modified by another user');
      await expect(conflictDetails).toContainText('other-user@example.com');

      // Should offer resolution options
      await expect(page.locator('[data-testid="reload-and-retry"]')).toBeVisible();
      await expect(page.locator('[data-testid="force-overwrite"]')).toBeVisible();
      await expect(page.locator('[data-testid="view-changes"]')).toBeVisible();
    });

    test('should handle 422 Unprocessable Entity with business rule explanations', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock 422 response for business rule violation
      await context.route('**/api/hives', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 422,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'Unprocessable Entity',
              message: 'Business rule validation failed',
              violations: [
                {
                  field: 'schedule',
                  code: 'OVERLAPPING_SESSIONS',
                  message: 'Cannot create overlapping study sessions'
                },
                {
                  field: 'participants',
                  code: 'MAX_HIVES_EXCEEDED',
                  message: 'Maximum number of active hives exceeded (limit: 5)'
                }
              ]
            })
          });
        } else {
          await route.continue();
        }
      });

      // Attempt to create hive that violates business rules
      await page.click('[data-testid="create-hive-btn"]');
      await page.fill('[data-testid="hive-name"]', 'Business Rule Test Hive');
      await page.click('[data-testid="submit-hive"]');

      // Should show business rule violations
      await expect(page.locator('[data-testid="business-rule-violations"]')).toBeVisible();

      // Verify specific violations are explained
      await expect(page.locator('[data-testid="violation-overlapping"]')).toContainText('overlapping study sessions');
      await expect(page.locator('[data-testid="violation-max-hives"]')).toContainText('Maximum number of active hives exceeded');

      // Should offer suggestions to resolve
      await expect(page.locator('[data-testid="resolve-overlapping"]')).toBeVisible();
      await expect(page.locator('[data-testid="manage-hives"]')).toBeVisible();
    });

    test('should handle 429 Rate Limiting with retry timing', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock rate limit response
      await context.route('**/api/hives', async (route) => {
        await route.fulfill({
          status: 429,
          contentType: 'application/json',
          headers: {
            'Retry-After': '60',
            'X-RateLimit-Limit': '100',
            'X-RateLimit-Remaining': '0',
            'X-RateLimit-Reset': String(Date.now() + 60000)
          },
          body: JSON.stringify({
            error: 'Too Many Requests',
            message: 'Rate limit exceeded. Please try again later.',
            retryAfter: 60
          })
        });
      });

      // Trigger rate limit
      await page.click('[data-testid="refresh-hives"]');

      // Should show rate limit message
      await expect(page.locator('[data-testid="rate-limit-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify retry timing information
      const rateLimitMessage = page.locator('[data-testid="rate-limit-message"]');
      await expect(rateLimitMessage).toContainText(/try again.*60.*second/i);

      // Should show countdown timer
      await expect(page.locator('[data-testid="retry-countdown"]')).toBeVisible();
      await expect(page.locator('[data-testid="retry-countdown"]')).toContainText(/59|60/);

      // Refresh button should be disabled during cooldown
      await expect(page.locator('[data-testid="refresh-hives"]')).toBeDisabled();

      // Should offer explanation about rate limits
      await expect(page.locator('[data-testid="rate-limit-explanation"]')).toContainText(/prevent overload|fair usage/i);
    });
  });

  test.describe('5xx Server Errors', () => {
    test('should handle 500 Internal Server Error with recovery options', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock 500 error
      await context.route('**/api/hives', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Internal Server Error',
            message: 'An unexpected error occurred',
            errorId: 'ERR-500-' + Date.now()
          })
        });
      });

      await page.click('[data-testid="refresh-hives"]');

      // Should show 500 error with helpful message
      await expect(page.locator('[data-testid="server-error-500"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify error details
      const errorMessage = page.locator('[data-testid="server-error-message"]');
      await expect(errorMessage).toContainText(/unexpected error|server error/i);

      // Should show error ID for support
      await expect(page.locator('[data-testid="error-id"]')).toContainText(/ERR-500-/);

      // Should offer recovery options
      await expect(page.locator('[data-testid="retry-action"]')).toBeVisible();
      await expect(page.locator('[data-testid="report-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();

      // Retry should work after clearing mock
      await errorHelper.clearApiMocks();
      await page.click('[data-testid="retry-action"]');
      await expect(page.locator('[data-testid="hive-list"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    });

    test('should handle 502 Bad Gateway with service status information', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock 502 error from gateway
      await context.route('**/api/analytics/**', async (route) => {
        await route.fulfill({
          status: 502,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Bad Gateway',
            message: 'Analytics service is temporarily unavailable',
            service: 'analytics-service',
            upstreamError: 'Connection refused'
          })
        });
      });

      await page.goto('/analytics');

      // Should show specific service unavailability
      await expect(page.locator('[data-testid="service-unavailable"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify service-specific messaging
      const serviceMessage = page.locator('[data-testid="service-message"]');
      await expect(serviceMessage).toContainText(/analytics.*temporarily unavailable/i);

      // Should show status page link
      await expect(page.locator('[data-testid="service-status-link"]')).toBeVisible();

      // Should offer fallback options
      await expect(page.locator('[data-testid="basic-analytics"]')).toBeVisible();
      await expect(page.locator('[data-testid="export-data"]')).toBeVisible();
    });

    test('should handle 503 Service Unavailable with maintenance notifications', async () => {
      // Mock 503 with maintenance information
      await context.route('**/api/**', async (route) => {
        await route.fulfill({
          status: 503,
          contentType: 'application/json',
          headers: {
            'Retry-After': '1800' // 30 minutes
          },
          body: JSON.stringify({
            error: 'Service Unavailable',
            message: 'System is under maintenance',
            maintenanceWindow: {
              start: '2024-01-01T02:00:00Z',
              end: '2024-01-01T04:00:00Z',
              reason: 'Scheduled database maintenance'
            }
          })
        });
      });

      await page.goto('/dashboard');

      // Should show maintenance notification
      await expect(page.locator('[data-testid="maintenance-notice"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify maintenance details
      const maintenanceMessage = page.locator('[data-testid="maintenance-message"]');
      await expect(maintenanceMessage).toContainText(/maintenance|scheduled/i);
      await expect(maintenanceMessage).toContainText(/database maintenance/i);

      // Should show estimated completion time
      await expect(page.locator('[data-testid="maintenance-end-time"]')).toBeVisible();

      // Should offer alternatives
      await expect(page.locator('[data-testid="offline-mode"]')).toBeVisible();
      await expect(page.locator('[data-testid="status-updates"]')).toBeVisible();
    });

    test('should handle 504 Gateway Timeout with load balancer status', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Mock 504 timeout
      await context.route('**/api/hives/123/realtime', async (route) => {
        await route.fulfill({
          status: 504,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Gateway Timeout',
            message: 'Real-time service did not respond in time',
            timeout: 30000,
            service: 'websocket-gateway'
          })
        });
      });

      // Should show timeout error
      await expect(page.locator('[data-testid="gateway-timeout"]')).toBeVisible({timeout: TIMEOUTS.LONG});

      // Verify timeout-specific messaging
      const timeoutMessage = page.locator('[data-testid="timeout-message"]');
      await expect(timeoutMessage).toContainText(/did not respond.*time|timeout/i);

      // Should offer degraded mode
      await expect(page.locator('[data-testid="degraded-mode-notice"]')).toBeVisible();
      await expect(page.locator('[data-testid="enable-polling-mode"]')).toBeVisible();

      // Should still allow basic functionality
      await expect(page.locator('[data-testid="basic-hive-view"]')).toBeVisible();
      await expect(page.locator('[data-testid="static-participant-list"]')).toBeVisible();
    });
  });

  test.describe('Database and Microservice Failures', () => {
    test('should handle database connection failures gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock database connection error
      await context.route('**/api/**', async (route) => {
        await route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Service Unavailable',
            message: 'Database connection failed',
            technicalDetails: 'Connection pool exhausted',
            errorCode: 'DB_CONNECTION_FAILED'
          })
        });
      });

      await page.goto('/dashboard');

      // Should show database error
      await expect(page.locator('[data-testid="database-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Verify database-specific messaging
      const dbMessage = page.locator('[data-testid="database-error-message"]');
      await expect(dbMessage).toContainText(/database.*unavailable|connection.*failed/i);

      // Should offer cached data fallback
      await expect(page.locator('[data-testid="cached-data-notice"]')).toBeVisible();
      await expect(page.locator('[data-testid="view-cached-data"]')).toBeVisible();

      // Should prevent data modification
      await expect(page.locator('[data-testid="create-hive-btn"]')).toBeDisabled();
      await expect(page.locator('[data-testid="read-only-mode"]')).toBeVisible();
    });

    test('should handle microservice communication failures', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      // Mock specific microservice failure
      await context.route('**/api/buddy/**', async (route) => {
        await route.abort('connectionrefused');
      });

      await context.route('**/api/music/**', async (route) => {
        await route.fulfill({
          status: 500,
          body: JSON.stringify({
            error: 'Service Error',
            message: 'Music service is experiencing issues'
          })
        });
      });

      await page.goto('/hive/123');

      // Core functionality should work
      await expect(page.locator('[data-testid="hive-content"]')).toBeVisible();
      await expect(page.locator('[data-testid="timer-section"]')).toBeVisible();

      // Failed services should show graceful degradation
      await expect(page.locator('[data-testid="buddy-service-unavailable"]')).toBeVisible();
      await expect(page.locator('[data-testid="music-service-error"]')).toBeVisible();

      // Should disable related features
      await expect(page.locator('[data-testid="find-buddy-btn"]')).toBeDisabled();
      await expect(page.locator('[data-testid="play-music-btn"]')).toBeDisabled();

      // Should show alternative options
      await expect(page.locator('[data-testid="manual-accountability"]')).toBeVisible();
      await expect(page.locator('[data-testid="local-focus-sounds"]')).toBeVisible();
    });
  });

  test.describe('Error Recovery and Retry Mechanisms', () => {
    test('should implement exponential backoff for retries', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/dashboard');

      let attemptCount = 0;
      const requestTimes: number[] = [];

      // Mock failing service that succeeds after 3 attempts
      await context.route('**/api/hives', async (route) => {
        requestTimes.push(Date.now());
        attemptCount++;

        if (attemptCount < 3) {
          await route.fulfill({
            status: 500,
            body: JSON.stringify({error: 'Temporary failure'})
          });
        } else {
          await route.continue();
        }
      });

      // Monitor retry attempts
      const _retryCount = 0;
      await page.route('**/api/hives', (route) => {
        retryCount++;
        route.continue();
      });

      await page.click('[data-testid="refresh-hives"]');

      // Should eventually succeed
      await expect(page.locator('[data-testid="hive-list"]')).toBeVisible({timeout: TIMEOUTS.LONG});

      // Verify exponential backoff timing
      if (requestTimes.length >= 3) {
        const delay1 = requestTimes[1] - requestTimes[0];
        const delay2 = requestTimes[2] - requestTimes[1];

        // Second delay should be longer than first (exponential backoff)
        expect(delay2).toBeGreaterThan(delay1 * 1.5);
      }

      // Should show retry progress to user
      await expect(page.locator('[data-testid="retry-attempts"]')).toHaveContainedText(['1', '2']);
    });

    test('should handle maximum retry attempts gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/analytics');

      // Mock persistent failure
      await context.route('**/api/analytics/**', async (route) => {
        await route.fulfill({
          status: 500,
          body: JSON.stringify({
            error: 'Persistent Error',
            message: 'Service is experiencing ongoing issues'
          })
        });
      });

      await page.click('[data-testid="generate-report"]');

      // Should show retry progress
      await expect(page.locator('[data-testid="retry-progress"]')).toBeVisible();

      // After max retries, should give up gracefully
      await expect(page.locator('[data-testid="max-retries-reached"]')).toBeVisible({
        timeout: TIMEOUTS.LONG
      });

      // Verify helpful error messaging
      const maxRetriesMessage = page.locator('[data-testid="max-retries-message"]');
      await expect(maxRetriesMessage).toContainText(/tried multiple times|persistent issue/i);

      // Should offer manual retry and alternative actions
      await expect(page.locator('[data-testid="manual-retry"]')).toBeVisible();
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();
      await expect(page.locator('[data-testid="try-later"]')).toBeVisible();
    });

    test('should show detailed error logs for debugging', async () => {
      await authHelper.login(TEST_USERS.ADMIN_USER.username, TEST_USERS.ADMIN_USER.password);
      await page.goto('/admin/diagnostics');

      // Mock complex error scenario
      await context.route('**/api/admin/system-health', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'System Health Check Failed',
            message: 'Multiple subsystem failures detected',
            details: {
              timestamp: new Date().toISOString(),
              requestId: 'req-12345',
              stackTrace: [
                'DatabaseConnectionError: Connection timeout',
                'at HealthController.checkDatabase:45',
                'at HealthService.performChecks:123'
              ],
              systemInfo: {
                nodeVersion: '18.17.0',
                memoryUsage: '512MB',
                cpuLoad: '78%'
              }
            }
          })
        });
      });

      await page.click('[data-testid="run-health-check"]');

      // Should show detailed error information for admin users
      await expect(page.locator('[data-testid="detailed-error-info"]')).toBeVisible();

      // Verify detailed error components
      await expect(page.locator('[data-testid="request-id"]')).toContainText('req-12345');
      await expect(page.locator('[data-testid="error-timestamp"]')).toBeVisible();
      await expect(page.locator('[data-testid="stack-trace"]')).toContainText('DatabaseConnectionError');
      await expect(page.locator('[data-testid="system-info"]')).toContainText('nodeVersion');

      // Should offer diagnostic tools
      await expect(page.locator('[data-testid="download-logs"]')).toBeVisible();
      await expect(page.locator('[data-testid="export-diagnostics"]')).toBeVisible();
      await expect(page.locator('[data-testid="system-metrics"]')).toBeVisible();
    });
  });
});