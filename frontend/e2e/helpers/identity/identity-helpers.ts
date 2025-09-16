/**
 * Reusable Test Helpers for Identity Service E2E Tests
 *
 * Provides common utilities for authentication, persona management, OAuth2 flows,
 * and test setup/teardown operations across all identity-related E2E tests
 *
 * @fileoverview Identity service E2E test helpers and utilities
 * @version 1.0.0
 */

import {type APIRequestContext, type BrowserContext, expect, type Page} from '@playwright/test';
import {AxeBuilder} from '@axe-core/playwright';
import type {
  LoginResponse,
  TestOAuth2Client,
  TestPersona
} from '../../fixtures/identity/identity-fixtures';
import {
  ACCESSIBILITY_CONFIG,
  IDENTITY_API,
  IDENTITY_ROUTES,
  PERFORMANCE_THRESHOLDS
} from '../tests/identity/identity.config';

/**
 * Authentication helper class
 */
export class AuthenticationHelper {
  private page: Page;
  private apiContext: APIRequestContext;

  constructor(page: Page, apiContext: APIRequestContext) {
    this.page = page;
    this.apiContext = apiContext;
  }

  /**
   * Login via UI form
   */
  async loginViaUI(email: string, password: string): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.LOGIN);

    // Fill login form
    await this.page.locator('[data-testid="email-input"]').fill(email);
    await this.page.locator('[data-testid="password-input"]').fill(password);

    // Click login button and wait for navigation
    const loginPromise = this.page.waitForURL('**/dashboard', {timeout: 10000});
    await this.page.locator('[data-testid="login-submit"]').click();
    await loginPromise;

    // Verify successful login
    await expect(this.page.locator('[data-testid="user-menu"]')).toBeVisible();
  }

  /**
   * Login via API and set browser context
   */
  async loginViaAPI(email: string, password: string): Promise<LoginResponse> {
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.LOGIN}`, {
      data: {email, password}
    });

    if (!response.ok()) {
      throw new Error(`Login failed: ${response.status()} ${await response.text()}`);
    }

    const loginResult = await response.json() as LoginResponse;

    // Set authentication state in browser
    await this.page.addInitScript((token: string) => {
      localStorage.setItem('focushive_token', token);
      localStorage.setItem('focushive_refresh_token', token);
    }, loginResult.accessToken);

    return loginResult;
  }

  /**
   * Logout via UI
   */
  async logoutViaUI(): Promise<void> {
    await this.page.locator('[data-testid="user-menu"]').click();
    await this.page.locator('[data-testid="logout-button"]').click();

    // Wait for redirect to login page
    await this.page.waitForURL('**/auth/login');
    await expect(this.page.locator('[data-testid="login-form"]')).toBeVisible();
  }

  /**
   * Logout via API
   */
  async logoutViaAPI(accessToken: string): Promise<void> {
    await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.LOGOUT}`, {
      headers: {'Authorization': `Bearer ${accessToken}`}
    });

    // Clear browser state
    await this.page.evaluate(() => {
      localStorage.removeItem('focushive_token');
      localStorage.removeItem('focushive_refresh_token');
      sessionStorage.clear();
    });
  }

  /**
   * Verify user is authenticated
   */
  async verifyAuthenticated(): Promise<void> {
    await expect(this.page.locator('[data-testid="user-menu"]')).toBeVisible();

    // Verify token exists in localStorage
    const token = await this.page.evaluate(() => localStorage.getItem('focushive_token'));
    expect(token).toBeTruthy();
  }

  /**
   * Verify user is not authenticated
   */
  async verifyNotAuthenticated(): Promise<void> {
    await expect(this.page.locator('[data-testid="login-form"]')).toBeVisible();

    // Verify token is cleared
    const token = await this.page.evaluate(() => localStorage.getItem('focushive_token'));
    expect(token).toBeFalsy();
  }
}

/**
 * Persona management helper class
 */
export class PersonaHelper {
  private page: Page;
  private apiContext: APIRequestContext;

  constructor(page: Page, apiContext: APIRequestContext) {
    this.page = page;
    this.apiContext = apiContext;
  }

  /**
   * Create persona via UI
   */
  async createPersonaViaUI(persona: Partial<TestPersona>): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PERSONA_CREATE);

    // Fill persona form
    await this.page.locator('[data-testid="persona-name"]').fill(persona.name || 'Test Persona');
    await this.page.locator('[data-testid="persona-type"]').selectOption(persona.type || 'PERSONAL');

    if (persona.displayName) {
      await this.page.locator('[data-testid="persona-display-name"]').fill(persona.displayName);
    }

    if (persona.bio) {
      await this.page.locator('[data-testid="persona-bio"]').fill(persona.bio);
    }

    // Submit form
    await this.page.locator('[data-testid="create-persona-submit"]').click();

    // Wait for success message
    await expect(this.page.locator('[data-testid="success-message"]')).toBeVisible();
  }

  /**
   * Create persona via API
   */
  async createPersonaViaAPI(persona: TestPersona, accessToken: string): Promise<TestPersona> {
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.PERSONAS}`, {
      headers: {'Authorization': `Bearer ${accessToken}`},
      data: persona
    });

    if (!response.ok()) {
      throw new Error(`Failed to create persona: ${response.status()} ${await response.text()}`);
    }

    return await response.json() as TestPersona;
  }

  /**
   * Switch persona via UI
   */
  async switchPersonaViaUI(personaName: string): Promise<void> {
    // Open persona switcher
    await this.page.locator('[data-testid="persona-switcher"]').click();

    // Select persona
    await this.page.locator(`[data-testid="persona-option-${personaName}"]`).click();

    // Wait for UI to update
    await expect(this.page.locator('[data-testid="current-persona"]')).toContainText(personaName);
  }

  /**
   * Switch persona via API
   */
  async switchPersonaViaAPI(personaId: string, accessToken: string): Promise<TestPersona> {
    const response = await this.apiContext.post(
        `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.PERSONA_SWITCH(personaId)}`,
        {headers: {'Authorization': `Bearer ${accessToken}`}}
    );

    if (!response.ok()) {
      throw new Error(`Failed to switch persona: ${response.status()} ${await response.text()}`);
    }

    return await response.json() as TestPersona;
  }

  /**
   * Get active persona
   */
  async getActivePersona(accessToken: string): Promise<TestPersona | null> {
    const response = await this.apiContext.get(
        `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.ACTIVE_PERSONA}`,
        {headers: {'Authorization': `Bearer ${accessToken}`}}
    );

    if (response.status() === 204) {
      return null; // No active persona
    }

    if (!response.ok()) {
      throw new Error(`Failed to get active persona: ${response.status()} ${await response.text()}`);
    }

    return await response.json() as TestPersona;
  }

  /**
   * Verify persona switch in UI
   */
  async verifyPersonaSwitched(personaName: string): Promise<void> {
    await expect(this.page.locator('[data-testid="current-persona"]')).toContainText(personaName);

    // Verify persona-specific UI elements
    await expect(this.page.locator('[data-testid="persona-avatar"]')).toBeVisible();
  }

  /**
   * Delete persona via API
   */
  async deletePersonaViaAPI(personaId: string, accessToken: string): Promise<void> {
    const response = await this.apiContext.delete(
        `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.PERSONA_BY_ID(personaId)}`,
        {headers: {'Authorization': `Bearer ${accessToken}`}}
    );

    if (!response.ok()) {
      throw new Error(`Failed to delete persona: ${response.status()} ${await response.text()}`);
    }
  }
}

/**
 * OAuth2 helper class
 */
export class OAuth2Helper {
  private page: Page;
  private apiContext: APIRequestContext;

  constructor(page: Page, apiContext: APIRequestContext) {
    this.page = page;
    this.apiContext = apiContext;
  }

  /**
   * Create OAuth2 client
   */
  async createClient(client: TestOAuth2Client, accessToken: string): Promise<TestOAuth2Client> {
    const response = await this.apiContext.post(
        `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.OAUTH2_CLIENTS}`,
        {
          headers: {'Authorization': `Bearer ${accessToken}`},
          data: client
        }
    );

    if (!response.ok()) {
      throw new Error(`Failed to create OAuth2 client: ${response.status()} ${await response.text()}`);
    }

    return await response.json() as TestOAuth2Client;
  }

  /**
   * Perform authorization code flow
   */
  async performAuthorizationFlow(
      clientId: string,
      redirectUri: string,
      scopes: string[] = ['profile']
  ): Promise<string> {
    const state = Math.random().toString(36).substring(7);
    const scopeString = scopes.join(' ');

    // Navigate to authorization endpoint
    const authUrl = `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.OAUTH2_AUTHORIZE}?` +
        `client_id=${clientId}&response_type=code&redirect_uri=${encodeURIComponent(redirectUri)}&` +
        `scope=${encodeURIComponent(scopeString)}&state=${state}`;

    await this.page.goto(authUrl);

    // If not logged in, should redirect to login
    if (await this.page.locator('[data-testid="login-form"]').isVisible()) {
      throw new Error('User must be logged in before authorization');
    }

    // Should show authorization consent screen
    await expect(this.page.locator('[data-testid="oauth-consent-form"]')).toBeVisible();

    // Verify client information is displayed
    await expect(this.page.locator('[data-testid="client-name"]')).toBeVisible();
    await expect(this.page.locator('[data-testid="requested-scopes"]')).toBeVisible();

    // Grant authorization
    await this.page.locator('[data-testid="authorize-button"]').click();

    // Wait for redirect and extract authorization code
    await this.page.waitForURL(`${redirectUri}*`);
    const url = new URL(this.page.url());
    const code = url.searchParams.get('code');

    if (!code) {
      throw new Error('Authorization code not found in redirect URL');
    }

    return code;
  }

  /**
   * Exchange authorization code for tokens
   */
  async exchangeCodeForTokens(
      code: string,
      clientId: string,
      clientSecret: string,
      redirectUri: string
  ): Promise<{
    access_token: string;
    refresh_token: string;
    token_type: string;
    expires_in: number
  }> {
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.OAUTH2_TOKEN}`, {
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      data: new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        client_id: clientId,
        client_secret: clientSecret,
        redirect_uri: redirectUri
      }).toString()
    });

    if (!response.ok()) {
      throw new Error(`Token exchange failed: ${response.status()} ${await response.text()}`);
    }

    return await response.json();
  }
}

/**
 * Privacy and data management helper class
 */
export class PrivacyHelper {
  private page: Page;
  private apiContext: APIRequestContext;

  constructor(page: Page, apiContext: APIRequestContext) {
    this.page = page;
    this.apiContext = apiContext;
  }

  /**
   * Update privacy settings via UI
   */
  async updatePrivacySettingsViaUI(settings: Record<string, boolean | string>): Promise<void> {
    await this.page.goto(IDENTITY_ROUTES.PRIVACY);

    for (const [key, value] of Object.entries(settings)) {
      if (typeof value === 'boolean') {
        const checkbox = this.page.locator(`[data-testid="privacy-${key}"]`);
        if (value !== await checkbox.isChecked()) {
          await checkbox.click();
        }
      } else {
        await this.page.locator(`[data-testid="privacy-${key}"]`).selectOption(value);
      }
    }

    await this.page.locator('[data-testid="save-privacy-settings"]').click();
    await expect(this.page.locator('[data-testid="settings-saved"]')).toBeVisible();
  }

  /**
   * Request data export
   */
  async requestDataExport(format: 'JSON' | 'CSV' = 'JSON', accessToken: string): Promise<string> {
    const response = await this.apiContext.post(
        `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.DATA_EXPORT}`,
        {
          headers: {'Authorization': `Bearer ${accessToken}`},
          data: {format, requestType: 'FULL'}
        }
    );

    if (!response.ok()) {
      throw new Error(`Data export request failed: ${response.status()} ${await response.text()}`);
    }

    const result = await response.json();
    return result.requestId;
  }

  /**
   * Check data export status
   */
  async checkExportStatus(requestId: string, accessToken: string): Promise<string> {
    const response = await this.apiContext.get(
        `${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.DATA_EXPORT}/${requestId}`,
        {headers: {'Authorization': `Bearer ${accessToken}`}}
    );

    if (!response.ok()) {
      throw new Error(`Failed to check export status: ${response.status()}`);
    }

    const result = await response.json();
    return result.status;
  }
}

/**
 * Performance testing helper class
 */
export class PerformanceHelper {
  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Measure page load time
   */
  async measurePageLoadTime(url: string): Promise<number> {
    const startTime = Date.now();
    await this.page.goto(url, {waitUntil: 'networkidle'});
    return Date.now() - startTime;
  }

  /**
   * Measure API response time
   */
  async measureAPIResponseTime(apiCall: () => Promise<unknown>): Promise<number> {
    const startTime = Date.now();
    await apiCall();
    return Date.now() - startTime;
  }

  /**
   * Verify performance meets thresholds
   */
  verifyPerformanceThreshold(actualTime: number, operation: 'FAST' | 'MEDIUM' | 'SLOW'): void {
    const threshold = PERFORMANCE_THRESHOLDS.API_RESPONSE_TIME[operation];
    expect(actualTime).toBeLessThan(threshold);
  }

  /**
   * Monitor persona switching performance
   */
  async measurePersonaSwitchTime(personaName: string): Promise<number> {
    const startTime = Date.now();

    await this.page.locator('[data-testid="persona-switcher"]').click();
    await this.page.locator(`[data-testid="persona-option-${personaName}"]`).click();
    await expect(this.page.locator('[data-testid="current-persona"]')).toContainText(personaName);

    return Date.now() - startTime;
  }
}

/**
 * Accessibility testing helper class
 */
export class AccessibilityHelper {
  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Run accessibility audit
   */
  async runAccessibilityAudit(selector?: string): Promise<void> {
    const builder = new AxeBuilder({page: this.page})
    .withTags(ACCESSIBILITY_CONFIG.WCAG_RULES.tags)
    .exclude('[data-testid="third-party-widget"]'); // Exclude third-party content

    if (selector) {
      builder.include(selector);
    }

    const results = await builder.analyze();
    expect(results.violations).toEqual([]);
  }

  /**
   * Test keyboard navigation
   */
  async testKeyboardNavigation(startSelector: string, endSelector: string): Promise<void> {
    await this.page.locator(startSelector).focus();

    // Tab through elements
    let currentElement = await this.page.locator(':focus').getAttribute('data-testid');
    const visitedElements: string[] = [];

    while (currentElement !== endSelector && visitedElements.length < 20) {
      visitedElements.push(currentElement || '');
      await this.page.keyboard.press('Tab');
      currentElement = await this.page.locator(':focus').getAttribute('data-testid');
    }

    expect(currentElement).toBe(endSelector);
  }

  /**
   * Verify focus indicators are visible
   */
  async verifyFocusIndicators(selectors: string[]): Promise<void> {
    for (const selector of selectors) {
      await this.page.locator(selector).focus();

      // Check if focus indicator is visible (outline, box-shadow, or border)
      const focusStyles = await this.page.locator(selector).evaluate((el) => {
        const styles = window.getComputedStyle(el);
        return {
          outline: styles.outline,
          outlineWidth: styles.outlineWidth,
          boxShadow: styles.boxShadow,
          borderColor: styles.borderColor
        };
      });

      const hasFocusIndicator =
          focusStyles.outline !== 'none' ||
          focusStyles.outlineWidth !== '0px' ||
          focusStyles.boxShadow !== 'none' ||
          focusStyles.borderColor !== 'rgba(0, 0, 0, 0)';

      expect(hasFocusIndicator).toBe(true);
    }
  }
}

/**
 * Security testing helper class
 */
export class SecurityHelper {
  private page: Page;
  private apiContext: APIRequestContext;

  constructor(page: Page, apiContext: APIRequestContext) {
    this.page = page;
    this.apiContext = apiContext;
  }

  /**
   * Verify JWT token structure and claims
   */
  verifyJWTToken(token: string): void {
    const parts = token.split('.');
    expect(parts).toHaveLength(3);

    const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString());

    // Verify required claims
    expect(payload.sub).toBeTruthy(); // Subject (user ID)
    expect(payload.iat).toBeTruthy(); // Issued at
    expect(payload.exp).toBeTruthy(); // Expires at
    expect(payload.iss).toBeTruthy(); // Issuer

    // Verify expiration is in the future
    expect(payload.exp * 1000).toBeGreaterThan(Date.now());
  }

  /**
   * Test CSRF protection
   */
  async testCSRFProtection(endpoint: string, accessToken: string): Promise<void> {
    // Attempt request without CSRF token (should fail)
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${endpoint}`, {
      headers: {'Authorization': `Bearer ${accessToken}`},
      data: {}
    });

    // Depending on endpoint, should either require CSRF token or validate origin
    expect([400, 403, 422]).toContain(response.status());
  }

  /**
   * Verify secure headers are present
   */
  async verifySecurityHeaders(url: string): Promise<void> {
    const response = await this.page.goto(url);
    const headers = response?.headers() || {};

    // Check for security headers
    expect(headers['x-frame-options']).toBeTruthy();
    expect(headers['x-content-type-options']).toBe('nosniff');
    expect(headers['referrer-policy']).toBeTruthy();
    expect(headers['permissions-policy']).toBeTruthy();

    // CSP should be present for HTML pages
    if (headers['content-type']?.includes('text/html')) {
      expect(headers['content-security-policy']).toBeTruthy();
    }
  }

  /**
   * Test rate limiting
   */
  async testRateLimit(endpoint: string, limit: number = 5): Promise<void> {
    const requests: Promise<unknown>[] = [];

    // Send requests rapidly
    for (let i = 0; i < limit + 2; i++) {
      requests.push(
          this.apiContext.post(`${IDENTITY_API.BASE_URL}${endpoint}`, {
            data: {test: `request-${i}`}
          })
      );
    }

    const responses = await Promise.all(requests);

    // At least one request should be rate limited
    const rateLimited = responses.some(response =>
        'status' in response && (response as { status: () => number }).status() === 429
    );
    expect(rateLimited).toBe(true);
  }
}

/**
 * Multi-session helper for concurrent testing
 */
export class MultiSessionHelper {
  private contexts: BrowserContext[] = [];
  private pages: Page[] = [];

  async createSessions(count: number, browser: {
    newContext: () => Promise<BrowserContext>
  }): Promise<Page[]> {
    for (let i = 0; i < count; i++) {
      const context = await browser.newContext();
      const page = await context.newPage();

      this.contexts.push(context);
      this.pages.push(page);
    }

    return this.pages;
  }

  async cleanup(): Promise<void> {
    await Promise.all(this.contexts.map(context => context.close()));
    this.contexts = [];
    this.pages = [];
  }

  getPage(index: number): Page {
    if (index >= this.pages.length) {
      throw new Error(`Page ${index} does not exist`);
    }
    return this.pages[index];
  }
}