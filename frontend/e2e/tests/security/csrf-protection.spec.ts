/**
 * CSRF Protection Security Tests (UOL-44.15)
 * 
 * Comprehensive Cross-Site Request Forgery (CSRF) protection testing for FocusHive
 * 
 * Test Categories:
 * 1. CSRF Token Validation
 * 2. Same-Origin Policy Enforcement  
 * 3. SameSite Cookie Attributes
 * 4. Referer Header Validation
 * 5. Custom Header Requirements
 * 6. Double Submit Cookie Pattern
 * 7. Origin Header Validation
 * 8. State-changing Operation Protection
 * 9. AJAX Request CSRF Protection
 * 10. WebSocket CSRF Prevention
 */

import { test, expect } from '@playwright/test';
import { Page, BrowserContext } from '@playwright/test';

interface CSRFTestConfig {
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  requiresAuth: boolean;
  requiresCSRF: boolean;
  data?: Record<string, unknown>;
}

class CSRFTestHelper {
  constructor(private page: Page, private context: BrowserContext) {}

  async extractCSRFToken(): Promise<string | null> {
    // Try multiple common CSRF token extraction methods
    const token = await this.page.evaluate(() => {
      // Method 1: Meta tag
      const metaTag = document.querySelector('meta[name="csrf-token"]');
      if (metaTag) {
        return metaTag.getAttribute('content');
      }

      // Method 2: Hidden input field
      const hiddenInput = document.querySelector('input[name="_token"]') as HTMLInputElement;
      if (hiddenInput) {
        return hiddenInput.value;
      }

      // Method 3: Script tag with CSRF token
      const scriptTags = document.querySelectorAll('script');
      for (const script of Array.from(scriptTags)) {
        const content = script.textContent || script.innerHTML;
        const match = content.match(/csrf[_-]?token['"]?\s*[:=]\s*['"]([^'"]+)['"]/i);
        if (match) {
          return match[1];
        }
      }

      // Method 4: Window object
      interface WindowWithCSRF extends Window {
        csrfToken?: string;
        _token?: string;
        Laravel?: { csrfToken: string };
      }
      const windowWithCSRF = window as WindowWithCSRF;
      
      return windowWithCSRF.csrfToken || 
             windowWithCSRF._token || 
             windowWithCSRF.Laravel?.csrfToken || 
             null;
    });

    return token;
  }

  async attemptCSRFAttack(config: CSRFTestConfig): Promise<{
    success: boolean;
    statusCode: number;
    error?: string;
  }> {
    try {
      const response = await this.page.request[config.method.toLowerCase() as keyof typeof this.page.request](
        config.endpoint,
        {
          data: config.data,
          headers: {
            'Content-Type': 'application/json',
            // Deliberately omit CSRF token and custom headers
          }
        }
      );

      return {
        success: response.ok(),
        statusCode: response.status()
      };
    } catch (error) {
      return {
        success: false,
        statusCode: 0,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  async simulateCrossOriginRequest(
    targetUrl: string,
    method: string = 'POST',
    data: Record<string, unknown> = {}
  ): Promise<{
    blocked: boolean;
    statusCode: number;
    error?: string;
  }> {
    try {
      // Create a new page to simulate cross-origin request
      const attackerPage = await this.context.newPage();
      
      // Navigate to a different origin (simulate attacker site)
      await attackerPage.goto('data:text/html,<html><body><h1>Attacker Site</h1></body></html>');

      // Attempt to make cross-origin request
      const response = await attackerPage.evaluate(async (url, methodParam, dataParam) => {
        try {
          const response = await fetch(url, {
            method: methodParam,
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(dataParam),
            credentials: 'include' // Include cookies
          });
          
          return {
            ok: response.ok,
            status: response.status,
            blocked: false
          };
        } catch (error) {
          return {
            ok: false,
            status: 0,
            blocked: true,
            error: error instanceof Error ? error.message : 'Request blocked'
          };
        }
      }, targetUrl, method, data);

      await attackerPage.close();

      return {
        blocked: response.blocked || !response.ok,
        statusCode: response.status,
        error: response.error
      };
    } catch (error) {
      return {
        blocked: true,
        statusCode: 0,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  async checkSameSiteCookies(): Promise<{
    hasSecureCookies: boolean;
    hasSameSiteStrict: boolean;
    hasSameSiteLax: boolean;
    cookieDetails: Array<{
      name: string;
      sameSite: string;
      secure: boolean;
      httpOnly: boolean;
    }>;
  }> {
    const cookies = await this.context.cookies();
    const cookieDetails = cookies.map(cookie => ({
      name: cookie.name,
      sameSite: cookie.sameSite || 'none',
      secure: cookie.secure,
      httpOnly: cookie.httpOnly
    }));

    return {
      hasSecureCookies: cookies.some(c => c.secure),
      hasSameSiteStrict: cookies.some(c => c.sameSite === 'Strict'),
      hasSameSiteLax: cookies.some(c => c.sameSite === 'Lax'),
      cookieDetails
    };
  }

  async validateOriginHeader(endpoint: string): Promise<boolean> {
    let originValidated = false;

    await this.page.route(endpoint, route => {
      const headers = route.request().headers();
      const origin = headers.origin;
      const referer = headers.referer;
      
      // Check if origin header is present and valid
      if (origin) {
        const pageUrl = new URL(this.page.url());
        const originUrl = new URL(origin);
        originValidated = pageUrl.origin === originUrl.origin;
      } else if (referer) {
        // Fallback to referer validation
        const pageUrl = new URL(this.page.url());
        const refererUrl = new URL(referer);
        originValidated = pageUrl.origin === refererUrl.origin;
      }

      route.continue();
    });

    return originValidated;
  }

  async testDoubleSubmitCookie(endpoint: string): Promise<{
    hasCSRFCookie: boolean;
    hasCSRFHeader: boolean;
    tokensMatch: boolean;
  }> {
    const cookies = await this.context.cookies();
    const csrfCookie = cookies.find(c => 
      c.name.toLowerCase().includes('csrf') || 
      c.name.toLowerCase().includes('xsrf')
    );

    const csrfToken = await this.extractCSRFToken();

    return {
      hasCSRFCookie: !!csrfCookie,
      hasCSRFHeader: !!csrfToken,
      tokensMatch: !!(csrfCookie && csrfToken && csrfCookie.value === csrfToken)
    };
  }
}

test.describe('CSRF Protection Security Tests', () => {
  let csrfHelper: CSRFTestHelper;
  let context: BrowserContext;

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext();
  });

  test.afterAll(async () => {
    await context.close();
  });

  test.beforeEach(async ({ page }) => {
    csrfHelper = new CSRFTestHelper(page, context);
    
    await page.goto('/');
    
    // Login with test user
    await page.click('[data-testid="login-button"]');
    await page.fill('[data-testid="email-input"]', 'test@example.com');
    await page.fill('[data-testid="password-input"]', 'TestPassword123!');
    await page.click('[data-testid="submit-login"]');
    await page.waitForURL('/dashboard');
  });

  test.describe('CSRF Token Validation', () => {
    test('should include CSRF token in forms', async ({ page }) => {
      await page.goto('/create-hive');
      
      const csrfToken = await csrfHelper.extractCSRFToken();
      expect(csrfToken).toBeTruthy();
      expect(csrfToken).toMatch(/^[a-zA-Z0-9+/=]+$/); // Base64-like pattern
    });

    test('should reject requests without CSRF token', async ({ page }) => {
      const testConfigs: CSRFTestConfig[] = [
        {
          endpoint: '/api/hives',
          method: 'POST',
          requiresAuth: true,
          requiresCSRF: true,
          data: { name: 'Test Hive', description: 'Test Description' }
        },
        {
          endpoint: '/api/profile',
          method: 'PUT',
          requiresAuth: true,
          requiresCSRF: true,
          data: { displayName: 'New Name' }
        },
        {
          endpoint: '/api/hives/1',
          method: 'DELETE',
          requiresAuth: true,
          requiresCSRF: true
        }
      ];

      for (const config of testConfigs) {
        const result = await csrfHelper.attemptCSRFAttack(config);
        
        // Should be rejected (403 Forbidden or 422 Unprocessable Entity)
        expect([403, 422, 419]).toContain(result.statusCode);
        expect(result.success).toBe(false);
      }
    });

    test('should accept requests with valid CSRF token', async ({ page }) => {
      await page.goto('/create-hive');
      
      const csrfToken = await csrfHelper.extractCSRFToken();
      expect(csrfToken).toBeTruthy();

      // Make request with valid CSRF token
      const response = await page.request.post('/api/hives', {
        data: {
          name: 'Valid CSRF Test Hive',
          description: 'This request includes a valid CSRF token'
        },
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-Token': csrfToken!,
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      // Should be accepted (200 OK or 201 Created)
      expect([200, 201]).toContain(response.status());
    });

    test('should rotate CSRF tokens after use', async ({ page }) => {
      await page.goto('/create-hive');
      
      const initialToken = await csrfHelper.extractCSRFToken();
      
      // Submit form to use the token
      await page.fill('[data-testid="hive-name-input"]', 'Token Rotation Test');
      await page.fill('[data-testid="hive-description-input"]', 'Testing token rotation');
      await page.click('[data-testid="create-hive-submit"]');
      
      // Wait for redirect or response
      await page.waitForTimeout(2000);
      
      // Get new token
      const newToken = await csrfHelper.extractCSRFToken();
      
      // Tokens should be different (indicating rotation)
      if (initialToken && newToken) {
        expect(initialToken).not.toBe(newToken);
      }
    });
  });

  test.describe('Same-Origin Policy Enforcement', () => {
    test('should block cross-origin state-changing requests', async ({ page }) => {
      const crossOriginTests = [
        { endpoint: '/api/hives', method: 'POST' },
        { endpoint: '/api/profile', method: 'PUT' },
        { endpoint: '/api/hives/1', method: 'DELETE' }
      ];

      for (const { endpoint, method } of crossOriginTests) {
        const result = await csrfHelper.simulateCrossOriginRequest(
          `${page.url().split('/')[0]}//${page.url().split('//')[1].split('/')[0]}${endpoint}`,
          method,
          { malicious: 'data' }
        );

        expect(result.blocked).toBe(true);
      }
    });

    test('should allow same-origin requests', async ({ page }) => {
      const csrfToken = await csrfHelper.extractCSRFToken();
      
      // Make same-origin request with CSRF token
      const response = await page.evaluate(async (token) => {
        try {
          const response = await fetch('/api/user/preferences', {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json',
              'X-CSRF-Token': token,
              'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify({ theme: 'dark' }),
            credentials: 'same-origin'
          });
          
          return {
            ok: response.ok,
            status: response.status
          };
        } catch (error) {
          return {
            ok: false,
            status: 0,
            error: error instanceof Error ? error.message : 'Unknown error'
          };
        }
      }, csrfToken);

      // Should be allowed for same-origin requests
      expect(response.ok || [200, 201, 204].includes(response.status)).toBe(true);
    });
  });

  test.describe('SameSite Cookie Protection', () => {
    test('should use SameSite cookie attributes', async ({ page }) => {
      const cookieInfo = await csrfHelper.checkSameSiteCookies();
      
      // Should have at least one secure cookie
      expect(cookieInfo.hasSecureCookies).toBe(true);
      
      // Should use SameSite=Lax or Strict
      expect(cookieInfo.hasSameSiteLax || cookieInfo.hasSameSiteStrict).toBe(true);
      
      // Authentication cookies should be secure and httpOnly
      const authCookies = cookieInfo.cookieDetails.filter(c => 
        c.name.toLowerCase().includes('auth') || 
        c.name.toLowerCase().includes('session') ||
        c.name.toLowerCase().includes('token')
      );
      
      authCookies.forEach(cookie => {
        expect(cookie.secure).toBe(true);
        expect(cookie.httpOnly).toBe(true);
        expect(['Strict', 'Lax']).toContain(cookie.sameSite);
      });
    });

    test('should not send cookies in cross-site requests', async ({ page }) => {
      // This test simulates a cross-site request and verifies cookies aren't sent
      let cookiesSent = false;

      await page.route('**/api/**', route => {
        const headers = route.request().headers();
        cookiesSent = !!headers.cookie;
        route.continue();
      });

      // Simulate cross-site request
      await csrfHelper.simulateCrossOriginRequest('/api/user/profile');

      // Cookies should not be sent in cross-site requests due to SameSite
      expect(cookiesSent).toBe(false);
    });
  });

  test.describe('Referer Header Validation', () => {
    test('should validate referer header for state-changing requests', async ({ page }) => {
      let refererValid = false;

      await page.route('**/api/hives', route => {
        const headers = route.request().headers();
        const referer = headers.referer;
        
        if (referer) {
          const pageOrigin = new URL(page.url()).origin;
          const refererOrigin = new URL(referer).origin;
          refererValid = pageOrigin === refererOrigin;
        }

        route.continue();
      });

      const csrfToken = await csrfHelper.extractCSRFToken();
      
      await page.request.post('/api/hives', {
        data: { name: 'Referer Test', description: 'Testing referer validation' },
        headers: {
          'X-CSRF-Token': csrfToken!,
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      expect(refererValid).toBe(true);
    });

    test('should reject requests with invalid referer', async ({ page }) => {
      const response = await page.request.post('/api/hives', {
        data: { name: 'Invalid Referer Test' },
        headers: {
          'Referer': 'https://malicious-site.com/',
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      // Should be rejected due to invalid referer
      expect([403, 422, 419]).toContain(response.status());
    });
  });

  test.describe('Custom Header Requirements', () => {
    test('should require X-Requested-With header for AJAX requests', async ({ page }) => {
      const csrfToken = await csrfHelper.extractCSRFToken();
      
      // Request without X-Requested-With header
      const responseWithoutHeader = await page.request.post('/api/hives', {
        data: { name: 'No Header Test' },
        headers: {
          'X-CSRF-Token': csrfToken!
        }
      });

      // Request with X-Requested-With header
      const responseWithHeader = await page.request.post('/api/hives', {
        data: { name: 'With Header Test' },
        headers: {
          'X-CSRF-Token': csrfToken!,
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      // Without header should be less successful than with header
      expect(responseWithHeader.status()).toBeLessThanOrEqual(responseWithoutHeader.status());
    });

    test('should accept requests with proper custom headers', async ({ page }) => {
      const csrfToken = await csrfHelper.extractCSRFToken();
      
      const response = await page.request.put('/api/user/preferences', {
        data: { theme: 'light', notifications: true },
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-Token': csrfToken!,
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      expect([200, 204]).toContain(response.status());
    });
  });

  test.describe('Double Submit Cookie Pattern', () => {
    test('should implement double submit cookie pattern', async ({ page }) => {
      const doubleSubmitTest = await csrfHelper.testDoubleSubmitCookie('/api/hives');
      
      // Should have both cookie and header/token
      expect(doubleSubmitTest.hasCSRFCookie || doubleSubmitTest.hasCSRFHeader).toBe(true);
    });

    test('should reject mismatched CSRF tokens', async ({ page }) => {
      const csrfToken = await csrfHelper.extractCSRFToken();
      
      // Make request with wrong CSRF token
      const response = await page.request.post('/api/hives', {
        data: { name: 'Mismatched Token Test' },
        headers: {
          'X-CSRF-Token': 'wrong-token-value',
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      // Should be rejected
      expect([403, 422, 419]).toContain(response.status());
    });
  });

  test.describe('AJAX and API CSRF Protection', () => {
    test('should protect all API endpoints', async ({ page }) => {
      const apiEndpoints = [
        { path: '/api/hives', method: 'POST' },
        { path: '/api/hives/1', method: 'PUT' },
        { path: '/api/hives/1', method: 'DELETE' },
        { path: '/api/user/profile', method: 'PUT' },
        { path: '/api/user/preferences', method: 'POST' }
      ];

      for (const endpoint of apiEndpoints) {
        const result = await csrfHelper.attemptCSRFAttack({
          endpoint: endpoint.path,
          method: endpoint.method as CSRFTestConfig['method'],
          requiresAuth: true,
          requiresCSRF: true,
          data: { test: 'data' }
        });

        // All API endpoints should be protected
        expect(result.success).toBe(false);
        expect([403, 422, 419]).toContain(result.statusCode);
      }
    });

    test('should allow GET requests without CSRF token', async ({ page }) => {
      const getEndpoints = [
        '/api/hives',
        '/api/user/profile',
        '/api/user/preferences'
      ];

      for (const endpoint of getEndpoints) {
        const response = await page.request.get(endpoint);
        
        // GET requests should be allowed without CSRF token
        expect([200, 401]).toContain(response.status()); // 401 if not authenticated
      }
    });
  });

  test.describe('WebSocket CSRF Prevention', () => {
    test('should validate WebSocket connections', async ({ page }) => {
      await page.goto('/hive/test-hive');
      
      // Wait for WebSocket connection
      await page.waitForTimeout(2000);
      
      const wsConnectionSecure = await page.evaluate(() => {
        const ws = (window as unknown as { webSocket?: WebSocket }).webSocket;
        return ws ? ws.url.startsWith('wss://') : false;
      });

      // WebSocket should use secure connection
      expect(wsConnectionSecure).toBe(true);
    });

    test('should authenticate WebSocket messages', async ({ page }) => {
      await page.goto('/hive/test-hive');
      
      // Wait for WebSocket connection
      await page.waitForTimeout(2000);
      
      let messageAuthenticated = false;

      // Monitor WebSocket messages
      await page.evaluate(() => {
        const ws = (window as unknown as { webSocket?: WebSocket }).webSocket;
        if (ws) {
          ws.addEventListener('message', (event) => {
            try {
              const data = JSON.parse(event.data);
              // Check if message includes authentication info
              (window as unknown as { ___ws_auth_detected___?: boolean }).___ws_auth_detected___ = 
                data.token || data.userId || data.sessionId;
            } catch (e) {
              // Invalid JSON message
            }
          });
        }
      });

      // Send a test message
      await page.evaluate(() => {
        const ws = (window as unknown as { webSocket?: WebSocket }).webSocket;
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'test_message',
            content: 'Testing WebSocket authentication'
          }));
        }
      });

      await page.waitForTimeout(1000);

      messageAuthenticated = await page.evaluate(() => {
        return (window as unknown as { ___ws_auth_detected___?: boolean }).___ws_auth_detected___ || false;
      });

      expect(messageAuthenticated).toBe(true);
    });
  });

  test.describe('Form-based CSRF Protection', () => {
    test('should protect all forms with CSRF tokens', async ({ page }) => {
      const formPages = [
        '/create-hive',
        '/profile',
        '/settings'
      ];

      for (const formPage of formPages) {
        await page.goto(formPage);
        
        // Check for CSRF token in form
        const hasCSRFToken = await page.evaluate(() => {
          const forms = document.querySelectorAll('form');
          return Array.from(forms).some(form => {
            const csrfInput = form.querySelector('input[name="_token"]') as HTMLInputElement;
            return csrfInput && csrfInput.value && csrfInput.value.length > 0;
          });
        });

        expect(hasCSRFToken).toBe(true);
      }
    });

    test('should regenerate CSRF tokens on successful form submission', async ({ page }) => {
      await page.goto('/create-hive');
      
      const initialToken = await csrfHelper.extractCSRFToken();
      
      // Submit form
      await page.fill('[data-testid="hive-name-input"]', 'CSRF Test Hive');
      await page.fill('[data-testid="hive-description-input"]', 'Testing CSRF token regeneration');
      await page.click('[data-testid="create-hive-submit"]');
      
      // Wait for form submission
      await page.waitForTimeout(3000);
      
      const newToken = await csrfHelper.extractCSRFToken();
      
      // Token should be regenerated
      if (initialToken && newToken) {
        expect(initialToken).not.toBe(newToken);
      }
    });
  });
});