/**
 * Third-Party Service Failure E2E Tests for FocusHive
 * Comprehensive testing of external service failures and fallback mechanisms
 * 
 * Test Coverage:
 * - Spotify API unavailability and rate limiting
 * - OAuth provider downtime and token issues
 * - Email service failures and delivery issues
 * - CDN unavailability and asset loading
 * - Analytics service timeouts and data loss
 * - Payment gateway errors and transaction failures
 * - Map service failures and location issues
 * - Social media API limits and authentication
 * - Push notification service failures
 * - File storage service outages
 */

import { test, expect, Page, BrowserContext } from '@playwright/test';
import { ErrorHandlingHelper } from '../../helpers/error-handling.helper';
import { AuthHelper } from '../../helpers/auth-helpers';
import { 
  TEST_USERS, 
  validateTestEnvironment, 
  TIMEOUTS,
  API_ENDPOINTS 
} from '../../helpers/test-data';

test.describe('Third-Party Service Failures', () => {
  let page: Page;
  let context: BrowserContext;
  let errorHelper: ErrorHandlingHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({ page: testPage, context: testContext }) => {
    page = testPage;
    context = testContext;
    errorHelper = new ErrorHandlingHelper(page, context);
    authHelper = new AuthHelper(page);
    
    validateTestEnvironment();
    
    // Set up third-party service monitoring
    await errorHelper.setupThirdPartyMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
    await errorHelper.clearThirdPartyMocks();
  });

  test.describe('Spotify API Failures', () => {
    test('should handle Spotify API unavailability gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Mock Spotify API failure
      await context.route('**/api/spotify/**', async (route) => {
        await route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Service Unavailable',
            message: 'Spotify API is temporarily unavailable'
          })
        });
      });
      
      // Try to access music features
      await page.click('[data-testid="music-tab"]');
      
      // Should detect Spotify unavailability
      await expect(page.locator('[data-testid="spotify-unavailable"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show service status information
      await expect(page.locator('[data-testid="spotify-status-message"]')).toContainText(/spotify.*unavailable|music.*service.*down/i);
      
      // Should offer alternative options
      await expect(page.locator('[data-testid="local-music-player"]')).toBeVisible();
      await expect(page.locator('[data-testid="focus-sounds"]')).toBeVisible();
      await expect(page.locator('[data-testid="white-noise-generator"]')).toBeVisible();
      
      // Should provide service status updates
      await expect(page.locator('[data-testid="check-spotify-status"]')).toBeVisible();
      
      // Test local alternatives
      await page.click('[data-testid="focus-sounds"]');
      
      // Should work without Spotify
      await expect(page.locator('[data-testid="focus-sounds-player"]')).toBeVisible();
      await expect(page.locator('[data-testid="play-focus-sound"]')).toBeEnabled();
      
      // Should not break other hive functionality
      await expect(page.locator('[data-testid="timer-controls"]')).toBeVisible();
      await expect(page.locator('[data-testid="chat-section"]')).toBeVisible();
    });

    test('should handle Spotify rate limiting with proper backoff', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Mock Spotify rate limiting
      await context.route('**/api/spotify/**', async (route) => {
        await route.fulfill({
          status: 429,
          headers: {
            'Retry-After': '60',
            'X-RateLimit-Limit': '100',
            'X-RateLimit-Remaining': '0'
          },
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Rate limit exceeded',
            message: 'Too many requests to Spotify API',
            retryAfter: 60
          })
        });
      });
      
      // Try to search for music
      await page.click('[data-testid="music-tab"]');
      await page.fill('[data-testid="music-search"]', 'focus music');
      await page.click('[data-testid="search-music"]');
      
      // Should detect rate limiting
      await expect(page.locator('[data-testid="spotify-rate-limited"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show rate limit information
      await expect(page.locator('[data-testid="rate-limit-message"]')).toContainText(/rate.*limit|too.*many.*requests/i);
      
      // Should show retry countdown
      await expect(page.locator('[data-testid="spotify-retry-countdown"]')).toBeVisible();
      await expect(page.locator('[data-testid="spotify-retry-countdown"]')).toContainText(/60|59/);
      
      // Should disable Spotify-dependent features temporarily
      await expect(page.locator('[data-testid="search-music"]')).toBeDisabled();
      await expect(page.locator('[data-testid="browse-playlists"]')).toBeDisabled();
      
      // Should offer cached content
      await expect(page.locator('[data-testid="use-cached-playlists"]')).toBeVisible();
      
      // Should suggest reducing music activity
      await expect(page.locator('[data-testid="reduce-music-activity"]')).toBeVisible();
      
      // Test cached playlists fallback
      await page.click('[data-testid="use-cached-playlists"]');
      
      await expect(page.locator('[data-testid="cached-playlists"]')).toBeVisible();
      await expect(page.locator('[data-testid="playlist-item"]')).toHaveCount.toBeGreaterThan(0);
    });

    test('should handle Spotify authentication failures', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Mock Spotify auth failure
      await context.route('**/api/spotify/auth/**', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Unauthorized',
            message: 'Spotify token expired or invalid'
          })
        });
      });
      
      // Try to access music features
      await page.click('[data-testid="music-tab"]');
      
      // Should detect authentication issue
      await expect(page.locator('[data-testid="spotify-auth-expired"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should offer re-authentication
      await expect(page.locator('[data-testid="reconnect-spotify"]')).toBeVisible();
      await expect(page.locator('[data-testid="spotify-auth-message"]')).toContainText(/reconnect.*spotify|spotify.*connection.*expired/i);
      
      // Should work without Spotify until reconnected
      await expect(page.locator('[data-testid="music-disabled-notice"]')).toBeVisible();
      
      // Should not affect other features
      await page.click('[data-testid="timer-tab"]');
      await expect(page.locator('[data-testid="timer-controls"]')).toBeVisible();
      
      // Test Spotify reconnection
      await page.click('[data-testid="music-tab"]');
      
      // Clear auth failure mock
      await context.unroute('**/api/spotify/auth/**');
      
      await page.click('[data-testid="reconnect-spotify"]');
      
      // Should open Spotify auth flow
      await expect(page.locator('[data-testid="spotify-auth-flow"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });
  });

  test.describe('OAuth Provider Failures', () => {
    test('should handle OAuth provider downtime during login', async () => {
      await page.goto('/login');
      
      // Mock OAuth provider failure
      await context.route('**/oauth/google/**', async (route) => {
        await route.abort('internetdisconnected');
      });
      
      await context.route('**/oauth/github/**', async (route) => {
        await route.fulfill({
          status: 503,
          body: JSON.stringify({ error: 'Service Unavailable' })
        });
      });
      
      // Try OAuth login
      await page.click('[data-testid="google-login"]');
      
      // Should detect OAuth provider failure
      await expect(page.locator('[data-testid="oauth-provider-unavailable"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show provider-specific error
      await expect(page.locator('[data-testid="google-unavailable"]')).toBeVisible();
      
      // Should offer alternative authentication methods
      await expect(page.locator('[data-testid="try-email-login"]')).toBeVisible();
      await expect(page.locator('[data-testid="try-other-providers"]')).toBeVisible();
      
      // Test GitHub OAuth failure
      await page.click('[data-testid="github-login"]');
      
      await expect(page.locator('[data-testid="github-unavailable"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should suggest manual login
      await expect(page.locator('[data-testid="manual-login-suggestion"]')).toBeVisible();
      await expect(page.locator('[data-testid="manual-login-suggestion"]')).toContainText(/email.*password|manual.*login/i);
      
      // Test fallback to email login
      await page.click('[data-testid="try-email-login"]');
      
      await expect(page.locator('[data-testid="email-login-form"]')).toBeVisible();
      await expect(page.locator('[data-testid="username-input"]')).toBeVisible();
      await expect(page.locator('[data-testid="password-input"]')).toBeVisible();
    });

    test('should handle OAuth token validation failures', async () => {
      await authHelper.login(TEST_USERS.OAUTH_USER.username, TEST_USERS.OAUTH_USER.password);
      await page.goto('/dashboard');
      
      // Mock OAuth token validation failure
      await context.route('**/api/auth/validate-oauth', async (route) => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Invalid Token',
            message: 'OAuth token validation failed',
            provider: 'google'
          })
        });
      });
      
      // Trigger token validation
      await page.reload();
      
      // Should detect token validation failure
      await expect(page.locator('[data-testid="oauth-token-invalid"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show provider-specific message
      await expect(page.locator('[data-testid="oauth-error-message"]')).toContainText(/google.*token|authentication.*expired/i);
      
      // Should offer re-authentication options
      await expect(page.locator('[data-testid="reauth-with-google"]')).toBeVisible();
      await expect(page.locator('[data-testid="switch-to-email-auth"]')).toBeVisible();
      
      // Should preserve user data during re-auth
      await expect(page.locator('[data-testid="data-preservation-notice"]')).toBeVisible();
      
      // Should not lose current session context
      const currentUrl = page.url();
      expect(currentUrl).toContain('/dashboard');
      
      // Test switching to email authentication
      await page.click('[data-testid="switch-to-email-auth"]');
      
      await expect(page.locator('[data-testid="link-email-account"]')).toBeVisible();
      await expect(page.locator('[data-testid="email-auth-setup"]')).toBeVisible();
    });

    test('should handle OAuth scope changes and permission issues', async () => {
      await authHelper.login(TEST_USERS.OAUTH_USER.username, TEST_USERS.OAUTH_USER.password);
      await page.goto('/profile');
      
      // Mock OAuth permission error
      await context.route('**/api/profile/import-data', async (route) => {
        await route.fulfill({
          status: 403,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Insufficient Permissions',
            message: 'Additional OAuth scopes required',
            required_scopes: ['profile', 'email', 'calendar.readonly'],
            current_scopes: ['profile', 'email']
          })
        });
      });
      
      // Try to import calendar data
      await page.click('[data-testid="import-calendar"]');
      
      // Should detect insufficient permissions
      await expect(page.locator('[data-testid="oauth-permissions-insufficient"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show required vs current permissions
      await expect(page.locator('[data-testid="required-scopes"]')).toContainText('calendar.readonly');
      await expect(page.locator('[data-testid="current-scopes"]')).not.toContainText('calendar.readonly');
      
      // Should offer permission upgrade
      await expect(page.locator('[data-testid="upgrade-permissions"]')).toBeVisible();
      await expect(page.locator('[data-testid="permission-explanation"]')).toBeVisible();
      
      // Should explain why permissions are needed
      await expect(page.locator('[data-testid="permission-explanation"]')).toContainText(/calendar.*import|schedule.*integration/i);
      
      // Should offer alternative without additional permissions
      await expect(page.locator('[data-testid="manual-calendar-entry"]')).toBeVisible();
      await expect(page.locator('[data-testid="skip-calendar-import"]')).toBeVisible();
      
      // Test manual alternative
      await page.click('[data-testid="manual-calendar-entry"]');
      
      await expect(page.locator('[data-testid="manual-schedule-form"]')).toBeVisible();
      await expect(page.locator('[data-testid="add-schedule-item"]')).toBeVisible();
    });
  });

  test.describe('Email Service Failures', () => {
    test('should handle email service outages gracefully', async () => {
      await page.goto('/register');
      
      // Mock email service failure
      await context.route('**/api/auth/send-verification', async (route) => {
        await route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Email Service Unavailable',
            message: 'Unable to send verification email at this time'
          })
        });
      });
      
      // Complete registration form
      await page.fill('[data-testid="username"]', 'testuser');
      await page.fill('[data-testid="email"]', 'test@example.com');
      await page.fill('[data-testid="password"]', 'TestPass123!');
      await page.click('[data-testid="register-btn"]');
      
      // Should detect email service failure
      await expect(page.locator('[data-testid="email-service-unavailable"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show service status message
      await expect(page.locator('[data-testid="email-error-message"]')).toContainText(/email.*service.*unavailable|verification.*email.*failed/i);
      
      // Should offer alternatives
      await expect(page.locator('[data-testid="manual-verification"]')).toBeVisible();
      await expect(page.locator('[data-testid="retry-email-later"]')).toBeVisible();
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();
      
      // Should still create account but mark email as unverified
      await expect(page.locator('[data-testid="account-created-unverified"]')).toBeVisible();
      
      // Should allow limited access
      await expect(page.locator('[data-testid="limited-access-notice"]')).toBeVisible();
      
      // Test retry mechanism
      await page.click('[data-testid="retry-email-later"]');
      
      await expect(page.locator('[data-testid="email-retry-scheduled"]')).toBeVisible();
      await expect(page.locator('[data-testid="retry-notification"]')).toContainText(/try.*again.*later|retry.*scheduled/i);
    });

    test('should handle email delivery delays and timeouts', async () => {
      await authHelper.login(TEST_USERS.UNVERIFIED_USER.username, TEST_USERS.UNVERIFIED_USER.password);
      await page.goto('/profile/verify');
      
      // Mock email service timeout
      await context.route('**/api/auth/resend-verification', async (route) => {
        // Add delay then timeout
        await new Promise(resolve => setTimeout(resolve, 30000));
        await route.fulfill({
          status: 408,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Request Timeout',
            message: 'Email service response timeout'
          })
        });
      });
      
      // Try to resend verification email
      await page.click('[data-testid="resend-verification"]');
      
      // Should show sending status
      await expect(page.locator('[data-testid="email-sending"]')).toBeVisible();
      
      // Should detect timeout
      await expect(page.locator('[data-testid="email-timeout"]')).toBeVisible({ 
        timeout: TIMEOUTS.EXTRA_LONG 
      });
      
      // Should show timeout message
      await expect(page.locator('[data-testid="email-timeout-message"]')).toContainText(/timeout|email.*service.*slow/i);
      
      // Should offer queue status
      await expect(page.locator('[data-testid="email-queued"]')).toBeVisible();
      await expect(page.locator('[data-testid="delivery-status"]')).toBeVisible();
      
      // Should provide alternative verification methods
      await expect(page.locator('[data-testid="sms-verification"]')).toBeVisible();
      await expect(page.locator('[data-testid="support-verification"]')).toBeVisible();
      
      // Should show estimated delivery time
      await expect(page.locator('[data-testid="estimated-delivery"]')).toBeVisible();
      
      // Test SMS alternative
      await page.click('[data-testid="sms-verification"]');
      
      await expect(page.locator('[data-testid="sms-verification-form"]')).toBeVisible();
      await expect(page.locator('[data-testid="phone-number-input"]')).toBeVisible();
    });

    test('should handle email blacklisting and spam filtering', async () => {
      await page.goto('/contact');
      
      // Mock email rejected due to spam filtering
      await context.route('**/api/support/send-message', async (route) => {
        await route.fulfill({
          status: 422,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Message Rejected',
            message: 'Email was rejected by spam filter',
            reason: 'CONTENT_BLOCKED',
            suggestions: [
              'Remove promotional language',
              'Check for suspicious links',
              'Try different email address'
            ]
          })
        });
      });
      
      // Send support message
      await page.fill('[data-testid="support-message"]', 'This is a test message with suspicious content');
      await page.click('[data-testid="send-message"]');
      
      // Should detect spam rejection
      await expect(page.locator('[data-testid="email-spam-rejected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show specific rejection reason
      await expect(page.locator('[data-testid="rejection-reason"]')).toContainText('CONTENT_BLOCKED');
      
      // Should provide improvement suggestions
      await expect(page.locator('[data-testid="improvement-suggestions"]')).toBeVisible();
      await expect(page.locator('[data-testid="suggestion-item"]')).toHaveCount(3);
      
      // Should offer alternative contact methods
      await expect(page.locator('[data-testid="alternative-contact"]')).toBeVisible();
      await expect(page.locator('[data-testid="phone-support"]')).toBeVisible();
      await expect(page.locator('[data-testid="live-chat"]')).toBeVisible();
      
      // Should preserve message for editing
      await expect(page.locator('[data-testid="support-message"]')).toHaveValue('This is a test message with suspicious content');
      
      // Should highlight problematic content
      await expect(page.locator('[data-testid="content-issues"]')).toBeVisible();
    });
  });

  test.describe('CDN and Asset Loading Failures', () => {
    test('should handle CDN unavailability with local fallbacks', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Block CDN resources
      await context.route('**/cdn.jsdelivr.net/**', route => route.abort('internetdisconnected'));
      await context.route('**/fonts.googleapis.com/**', route => route.abort('internetdisconnected'));
      await context.route('**/cdnjs.cloudflare.com/**', route => route.abort('internetdisconnected'));
      
      await page.goto('/dashboard');
      
      // Should detect CDN failures
      await expect(page.locator('[data-testid="cdn-resources-failed"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should fallback to local resources
      await expect(page.locator('[data-testid="local-fallback-active"]')).toBeVisible();
      
      // Should show performance impact notice
      await expect(page.locator('[data-testid="performance-degraded"]')).toBeVisible();
      
      // Application should still function
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
      await expect(page.locator('[data-testid="create-hive-btn"]')).toBeEnabled();
      
      // Fonts and styling should degrade gracefully
      const computedStyle = await page.locator('[data-testid="main-heading"]').evaluate(el => 
        window.getComputedStyle(el).fontFamily
      );
      expect(computedStyle).toBeDefined(); // Should fallback to system fonts
      
      // Should log CDN failures for monitoring
      const cdnErrors = await page.evaluate(() => 
        JSON.parse(sessionStorage.getItem('cdn-errors') || '[]')
      );
      expect(cdnErrors.length).toBeGreaterThan(0);
    });

    test('should handle slow CDN loading with progressive enhancement', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Simulate slow CDN responses
      await context.route('**/cdn.example.com/**', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 10000)); // 10 second delay
        await route.continue();
      });
      
      await page.goto('/dashboard');
      
      // Should show loading state for enhanced features
      await expect(page.locator('[data-testid="enhanced-features-loading"]')).toBeVisible();
      
      // Should render basic UI immediately
      await expect(page.locator('[data-testid="basic-ui-loaded"]')).toBeVisible();
      
      // Should show progressive loading indicators
      await expect(page.locator('[data-testid="loading-enhanced-icons"]')).toBeVisible();
      await expect(page.locator('[data-testid="loading-custom-fonts"]')).toBeVisible();
      
      // Core functionality should work while enhancements load
      await page.click('[data-testid="create-hive-btn"]');
      await expect(page.locator('[data-testid="hive-creation-form"]')).toBeVisible();
      
      // Should detect slow loading and offer options
      await expect(page.locator('[data-testid="slow-connection-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should offer lightweight mode
      await expect(page.locator('[data-testid="enable-lightweight-mode"]')).toBeVisible();
      
      // Test lightweight mode activation
      await page.click('[data-testid="enable-lightweight-mode"]');
      
      await expect(page.locator('[data-testid="lightweight-mode-active"]')).toBeVisible();
      await expect(page.locator('[data-testid="enhanced-ui-disabled"]')).toBeVisible();
    });

    test('should handle corrupted or malformed assets', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Serve corrupted assets
      await context.route('**/assets/icons/**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'image/svg+xml',
          body: 'corrupted-svg-content-not-valid'
        });
      });
      
      await context.route('**/assets/css/**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'text/css',
          body: 'invalid-css { syntax error;;;'
        });
      });
      
      await page.goto('/dashboard');
      
      // Should detect corrupted assets
      await expect(page.locator('[data-testid="asset-corruption-detected"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show asset loading errors
      await expect(page.locator('[data-testid="icon-loading-failed"]')).toBeVisible();
      await expect(page.locator('[data-testid="css-parsing-failed"]')).toBeVisible();
      
      // Should fallback to default styling
      await expect(page.locator('[data-testid="default-styling-active"]')).toBeVisible();
      
      // Should use text fallbacks for broken icons
      await expect(page.locator('[data-testid="text-icon-fallback"]')).toBeVisible();
      
      // Should report asset integrity issues
      const integrityErrors = await page.evaluate(() => 
        JSON.parse(sessionStorage.getItem('asset-integrity-errors') || '[]')
      );
      expect(integrityErrors.length).toBeGreaterThan(0);
      
      // Application should remain functional despite asset issues
      await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
      await page.click('[data-testid="create-hive-btn"]');
      await expect(page.locator('[data-testid="hive-creation-form"]')).toBeVisible();
    });
  });

  test.describe('Analytics and Monitoring Service Failures', () => {
    test('should handle analytics service outages without affecting UX', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      
      // Mock analytics service failure
      await context.route('**/analytics/**', async (route) => {
        await route.fulfill({
          status: 503,
          body: JSON.stringify({ error: 'Analytics service unavailable' })
        });
      });
      
      await page.goto('/dashboard');
      
      // Should detect analytics failure
      const analyticsError = page.locator('[data-testid="analytics-service-down"]');
      if (await analyticsError.isVisible({ timeout: 5000 })) {
        // Analytics failure should not affect core functionality
        await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
        
        // Should show analytics unavailable notice
        await expect(page.locator('[data-testid="analytics-unavailable-notice"]')).toBeVisible();
        
        // Should continue tracking offline
        await expect(page.locator('[data-testid="offline-tracking-active"]')).toBeVisible();
      }
      
      // Core user actions should still work
      await page.click('[data-testid="create-hive-btn"]');
      await expect(page.locator('[data-testid="hive-creation-form"]')).toBeVisible();
      
      await page.fill('[data-testid="hive-name"]', 'Analytics Test Hive');
      await page.click('[data-testid="submit-hive"]');
      
      // Should create hive successfully
      await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should queue analytics events for later
      const queuedEvents = await page.evaluate(() => 
        JSON.parse(localStorage.getItem('queued-analytics-events') || '[]')
      );
      expect(queuedEvents.length).toBeGreaterThan(0);
    });

    test('should handle monitoring service failures gracefully', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Mock monitoring service failures
      await context.route('**/api/monitoring/**', route => route.abort('internetdisconnected'));
      await context.route('**/telemetry/**', route => route.abort('internetdisconnected'));
      
      // Should continue normal operation without monitoring
      await page.click('[data-testid="join-hive"]');
      await expect(page.locator('[data-testid="join-success"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Should start timer normally
      await page.click('[data-testid="start-timer"]');
      await expect(page.locator('[data-testid="timer-running"]')).toBeVisible();
      
      // Should not show error messages for monitoring failures
      await expect(page.locator('[data-testid="monitoring-error"]')).not.toBeVisible();
      
      // Should continue collecting metrics locally
      const localMetrics = await page.evaluate(() => 
        JSON.parse(localStorage.getItem('local-metrics') || '{}')
      );
      expect(localMetrics).toBeDefined();
      
      // Should show reduced telemetry notice if configured
      const telemetryNotice = page.locator('[data-testid="telemetry-reduced"]');
      if (await telemetryNotice.isVisible({ timeout: 2000 })) {
        await expect(telemetryNotice).toContainText(/telemetry.*reduced|monitoring.*limited/i);
      }
    });
  });

  test.describe('File Storage Service Failures', () => {
    test('should handle file upload service outages', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/profile/edit');
      
      // Mock file storage service failure
      await context.route('**/api/files/upload/**', async (route) => {
        await route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'File Storage Unavailable',
            message: 'File upload service is temporarily unavailable'
          })
        });
      });
      
      // Try to upload avatar
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'avatar.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.alloc(50 * 1024)
      });
      
      // Should detect file storage failure
      await expect(page.locator('[data-testid="file-storage-unavailable"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show service status
      await expect(page.locator('[data-testid="file-service-status"]')).toContainText(/file.*service.*unavailable/i);
      
      // Should offer alternatives
      await expect(page.locator('[data-testid="save-file-locally"]')).toBeVisible();
      await expect(page.locator('[data-testid="try-different-service"]')).toBeVisible();
      await expect(page.locator('[data-testid="upload-later"]')).toBeVisible();
      
      // Should preserve file for retry
      await expect(page.locator('[data-testid="file-preserved"]')).toBeVisible();
      
      // Test local storage option
      await page.click('[data-testid="save-file-locally"]');
      
      await expect(page.locator('[data-testid="file-saved-locally"]')).toBeVisible();
      await expect(page.locator('[data-testid="sync-when-available"]')).toBeVisible();
      
      // Should continue with other profile updates
      await page.fill('[data-testid="display-name"]', 'Updated Name');
      await page.click('[data-testid="save-profile"]');
      
      await expect(page.locator('[data-testid="profile-updated-partial"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
    });

    test('should handle file download service failures', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/reports');
      
      // Mock file download service failure
      await context.route('**/api/reports/download/**', async (route) => {
        await route.fulfill({
          status: 503,
          body: JSON.stringify({ error: 'Download service unavailable' })
        });
      });
      
      // Try to download report
      await page.click('[data-testid="download-report"]');
      
      // Should detect download service failure
      await expect(page.locator('[data-testid="download-service-unavailable"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should offer alternatives
      await expect(page.locator('[data-testid="view-report-online"]')).toBeVisible();
      await expect(page.locator('[data-testid="email-report"]')).toBeVisible();
      await expect(page.locator('[data-testid="schedule-download"]')).toBeVisible();
      
      // Test online viewing alternative
      await page.click('[data-testid="view-report-online"]');
      
      await expect(page.locator('[data-testid="report-viewer"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      await expect(page.locator('[data-testid="report-content"]')).toBeVisible();
      
      // Should offer export options
      await expect(page.locator('[data-testid="copy-report-data"]')).toBeVisible();
      await expect(page.locator('[data-testid="print-report"]')).toBeVisible();
    });
  });

  test.describe('Third-Party Service Recovery', () => {
    test('should automatically recover when third-party services return', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');
      
      // Start with Spotify unavailable
      await context.route('**/api/spotify/**', route => route.abort('internetdisconnected'));
      
      // Try to access music
      await page.click('[data-testid="music-tab"]');
      
      // Should show Spotify unavailable
      await expect(page.locator('[data-testid="spotify-unavailable"]')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
      
      // Restore Spotify service
      await context.unroute('**/api/spotify/**');
      
      // Should automatically detect service recovery
      await expect(page.locator('[data-testid="spotify-service-restored"]')).toBeVisible({ 
        timeout: TIMEOUTS.LONG 
      });
      
      // Should restore music functionality
      await expect(page.locator('[data-testid="music-search"]')).toBeEnabled();
      await expect(page.locator('[data-testid="browse-playlists"]')).toBeEnabled();
      
      // Should show recovery notification
      await expect(page.locator('[data-testid="service-recovery-notification"]')).toContainText(/spotify.*restored|music.*available/i);
      
      // Should resume any queued operations
      const queuedOperations = await page.evaluate(() => 
        JSON.parse(sessionStorage.getItem('queued-spotify-operations') || '[]')
      );
      
      if (queuedOperations.length > 0) {
        await expect(page.locator('[data-testid="resuming-queued-operations"]')).toBeVisible();
      }
    });

    test('should show comprehensive service status dashboard', async () => {
      await authHelper.login(TEST_USERS.ADMIN_USER.username, TEST_USERS.ADMIN_USER.password);
      await page.goto('/admin/system-status');
      
      // Should show service status dashboard
      await expect(page.locator('[data-testid="service-status-dashboard"]')).toBeVisible();
      
      // Should show all third-party services
      const services = [
        'spotify-service',
        'oauth-providers',
        'email-service',
        'cdn-services',
        'analytics-service',
        'file-storage'
      ];
      
      for (const service of services) {
        await expect(page.locator(`[data-testid="${service}-status"]`)).toBeVisible();
      }
      
      // Should show service health metrics
      await expect(page.locator('[data-testid="service-uptime"]')).toBeVisible();
      await expect(page.locator('[data-testid="service-latency"]')).toBeVisible();
      await expect(page.locator('[data-testid="error-rates"]')).toBeVisible();
      
      // Should show recent incidents
      await expect(page.locator('[data-testid="recent-incidents"]')).toBeVisible();
      
      // Should provide service testing tools
      await expect(page.locator('[data-testid="test-service-connectivity"]')).toBeVisible();
      
      // Test service connectivity check
      await page.click('[data-testid="test-spotify-connectivity"]');
      
      await expect(page.locator('[data-testid="connectivity-test-running"]')).toBeVisible();
      await expect(page.locator('[data-testid="connectivity-test-results"]')).toBeVisible({ 
        timeout: TIMEOUTS.MEDIUM 
      });
      
      // Should show test results
      const testResults = page.locator('[data-testid="test-result-spotify"]');
      await expect(testResults).toContainText(/success|failure|timeout/i);
      
      // Should provide troubleshooting information
      await expect(page.locator('[data-testid="troubleshooting-guide"]')).toBeVisible();
    });
  });
});