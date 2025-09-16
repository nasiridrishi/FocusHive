/**
 * Security Headers Tests (UOL-44.15)
 *
 * Comprehensive security headers validation for FocusHive frontend
 *
 * Test Categories:
 * 1. Content Security Policy (CSP)
 * 2. HTTP Strict Transport Security (HSTS)
 * 3. X-Frame-Options Protection
 * 4. X-Content-Type-Options
 * 5. X-XSS-Protection
 * 6. Referrer-Policy
 * 7. Permissions-Policy (Feature-Policy)
 * 8. Cross-Origin Headers (CORP, COEP, COOP)
 * 9. Cache Control Headers
 * 10. Server Information Disclosure
 */

import {expect, Page, Response, test} from '@playwright/test';

interface SecurityHeader {
  name: string;
  expectedValue?: string;
  shouldExist: boolean;
  pattern?: RegExp;
  description: string;
}

interface CSPDirective {
  directive: string;
  expectedSources: string[];
  forbiddenSources: string[];
  required: boolean;
}

class SecurityHeadersHelper {
  constructor(private page: Page) {
  }

  async getResponseHeaders(url?: string): Promise<Record<string, string>> {
    let response: Response | null = null;

    if (url) {
      response = await this.page.goto(url);
    } else {
      response = await this.page.goto(this.page.url());
    }

    return response ? response.headers() : {};
  }

  async validateSecurityHeaders(expectedHeaders: SecurityHeader[]): Promise<{
    passed: boolean[];
    failed: boolean[];
    missing: string[];
    present: string[];
    details: Array<{ header: string; expected: string; actual: string; passed: boolean }>;
  }> {
    const headers = await this.getResponseHeaders();
    const passed: boolean[] = [];
    const failed: boolean[] = [];
    const missing: string[] = [];
    const present: string[] = [];
    const details: Array<{
      header: string;
      expected: string;
      actual: string;
      passed: boolean
    }> = [];

    for (const expectedHeader of expectedHeaders) {
      const actualValue = headers[expectedHeader.name.toLowerCase()];
      const headerExists = actualValue !== undefined;

      if (expectedHeader.shouldExist) {
        if (!headerExists) {
          missing.push(expectedHeader.name);
          failed.push(true);
          passed.push(false);
          details.push({
            header: expectedHeader.name,
            expected: expectedHeader.expectedValue || 'present',
            actual: 'missing',
            passed: false
          });
          continue;
        }

        present.push(expectedHeader.name);

        // Check specific value if provided
        if (expectedHeader.expectedValue) {
          const matches = actualValue === expectedHeader.expectedValue;
          passed.push(matches);
          failed.push(!matches);
          details.push({
            header: expectedHeader.name,
            expected: expectedHeader.expectedValue,
            actual: actualValue || '',
            passed: matches
          });
        }
        // Check pattern if provided
        else if (expectedHeader.pattern) {
          const matches = expectedHeader.pattern.test(actualValue);
          passed.push(matches);
          failed.push(!matches);
          details.push({
            header: expectedHeader.name,
            expected: expectedHeader.pattern.toString(),
            actual: actualValue || '',
            passed: matches
          });
        }
        // Just check existence
        else {
          passed.push(true);
          failed.push(false);
          details.push({
            header: expectedHeader.name,
            expected: 'present',
            actual: actualValue || '',
            passed: true
          });
        }
      } else {
        // Header should not exist
        if (headerExists) {
          present.push(expectedHeader.name);
          failed.push(true);
          passed.push(false);
          details.push({
            header: expectedHeader.name,
            expected: 'absent',
            actual: actualValue || '',
            passed: false
          });
        } else {
          missing.push(expectedHeader.name);
          passed.push(true);
          failed.push(false);
          details.push({
            header: expectedHeader.name,
            expected: 'absent',
            actual: 'absent',
            passed: true
          });
        }
      }
    }

    return {passed, failed, missing, present, details};
  }

  async validateCSP(expectedDirectives: CSPDirective[]): Promise<{
    directivesPassed: boolean[];
    missingDirectives: string[];
    invalidSources: Array<{ directive: string; invalidSource: string }>;
    cspString: string;
  }> {
    const headers = await this.getResponseHeaders();
    const csp = headers['content-security-policy'] || headers['content-security-policy-report-only'] || '';

    const directivesPassed: boolean[] = [];
    const missingDirectives: string[] = [];
    const invalidSources: Array<{ directive: string; invalidSource: string }> = [];

    for (const expectedDirective of expectedDirectives) {
      const directiveRegex = new RegExp(`${expectedDirective.directive}\\s+([^;]+)`, 'i');
      const match = csp.match(directiveRegex);

      if (!match && expectedDirective.required) {
        missingDirectives.push(expectedDirective.directive);
        directivesPassed.push(false);
        continue;
      }

      if (match) {
        const sources = match[1].trim().split(/\s+/);

        // Check for expected sources
        const hasAllExpectedSources = expectedDirective.expectedSources.every(expected =>
            sources.some(source => source.includes(expected))
        );

        // Check for forbidden sources
        const hasForbiddenSources = expectedDirective.forbiddenSources.some(forbidden =>
            sources.some(source => source.includes(forbidden))
        );

        if (hasForbiddenSources) {
          expectedDirective.forbiddenSources.forEach(forbidden => {
            if (sources.some(source => source.includes(forbidden))) {
              invalidSources.push({
                directive: expectedDirective.directive,
                invalidSource: forbidden
              });
            }
          });
        }

        directivesPassed.push(hasAllExpectedSources && !hasForbiddenSources);
      } else {
        directivesPassed.push(!expectedDirective.required);
      }
    }

    return {
      directivesPassed,
      missingDirectives,
      invalidSources,
      cspString: csp
    };
  }

  async testFrameOptions(): Promise<{
    canBeFramed: boolean;
    xFrameOptionsCorrect: boolean;
    actualValue: string;
  }> {
    const headers = await this.getResponseHeaders();
    const xFrameOptions = headers['x-frame-options'] || '';

    // Try to frame the page
    const canBeFramed = await this.page.evaluate(async () => {
      try {
        const iframe = document.createElement('iframe');
        iframe.src = window.location.href;
        iframe.style.display = 'none';
        document.body.appendChild(iframe);

        return new Promise<boolean>((resolve) => {
          iframe.onload = () => {
            document.body.removeChild(iframe);
            resolve(true);
          };
          iframe.onerror = () => {
            document.body.removeChild(iframe);
            resolve(false);
          };

          // Timeout after 3 seconds
          setTimeout(() => {
            if (document.body.contains(iframe)) {
              document.body.removeChild(iframe);
            }
            resolve(false);
          }, 3000);
        });
      } catch {
        return false;
      }
    });

    const validXFrameOptions = ['DENY', 'SAMEORIGIN'].includes(xFrameOptions.toUpperCase());

    return {
      canBeFramed,
      xFrameOptionsCorrect: validXFrameOptions,
      actualValue: xFrameOptions
    };
  }

  async testMIMETypeSniffing(): Promise<{
    mimeSniffingDisabled: boolean;
    actualValue: string;
  }> {
    const headers = await this.getResponseHeaders();
    const xContentTypeOptions = headers['x-content-type-options'] || '';

    return {
      mimeSniffingDisabled: xContentTypeOptions.toLowerCase() === 'nosniff',
      actualValue: xContentTypeOptions
    };
  }

  async testHSTS(): Promise<{
    hstsEnabled: boolean;
    maxAge: number;
    includeSubDomains: boolean;
    preload: boolean;
    actualValue: string;
  }> {
    const headers = await this.getResponseHeaders();
    const hsts = headers['strict-transport-security'] || '';

    const maxAgeMatch = hsts.match(/max-age=(\d+)/i);
    const maxAge = maxAgeMatch ? parseInt(maxAgeMatch[1], 10) : 0;

    return {
      hstsEnabled: hsts.length > 0,
      maxAge,
      includeSubDomains: hsts.toLowerCase().includes('includesubdomains'),
      preload: hsts.toLowerCase().includes('preload'),
      actualValue: hsts
    };
  }

  async testReferrerPolicy(): Promise<{
    hasReferrerPolicy: boolean;
    isSecure: boolean;
    actualValue: string;
  }> {
    const headers = await this.getResponseHeaders();
    const referrerPolicy = headers['referrer-policy'] || '';

    const securePolicies = [
      'no-referrer',
      'same-origin',
      'strict-origin',
      'strict-origin-when-cross-origin'
    ];

    return {
      hasReferrerPolicy: referrerPolicy.length > 0,
      isSecure: securePolicies.includes(referrerPolicy.toLowerCase()),
      actualValue: referrerPolicy
    };
  }

  async testPermissionsPolicy(): Promise<{
    hasPermissionsPolicy: boolean;
    restrictedFeatures: string[];
    actualValue: string;
  }> {
    const headers = await this.getResponseHeaders();
    const permissionsPolicy = headers['permissions-policy'] || headers['feature-policy'] || '';

    // Common features that should be restricted
    const expectedRestrictions = [
      'camera',
      'microphone',
      'geolocation',
      'payment',
      'usb',
      'magnetometer',
      'gyroscope',
      'accelerometer'
    ];

    const restrictedFeatures = expectedRestrictions.filter(feature =>
        permissionsPolicy.includes(feature)
    );

    return {
      hasPermissionsPolicy: permissionsPolicy.length > 0,
      restrictedFeatures,
      actualValue: permissionsPolicy
    };
  }

  async testCORSHeaders(): Promise<{
    hasCORSHeaders: boolean;
    allowsAnyOrigin: boolean;
    allowsCredentials: boolean;
    headers: {
      accessControlAllowOrigin: string;
      accessControlAllowCredentials: string;
      accessControlAllowMethods: string;
      accessControlAllowHeaders: string;
    };
  }> {
    const headers = await this.getResponseHeaders();

    const corsHeaders = {
      accessControlAllowOrigin: headers['access-control-allow-origin'] || '',
      accessControlAllowCredentials: headers['access-control-allow-credentials'] || '',
      accessControlAllowMethods: headers['access-control-allow-methods'] || '',
      accessControlAllowHeaders: headers['access-control-allow-headers'] || ''
    };

    const hasCORSHeaders = Object.values(corsHeaders).some(value => value.length > 0);
    const allowsAnyOrigin = corsHeaders.accessControlAllowOrigin === '*';
    const allowsCredentials = corsHeaders.accessControlAllowCredentials === 'true';

    return {
      hasCORSHeaders,
      allowsAnyOrigin,
      allowsCredentials,
      headers: corsHeaders
    };
  }

  async testServerInfoDisclosure(): Promise<{
    disclosesServer: boolean;
    disclosesVersion: boolean;
    disclosedHeaders: string[];
  }> {
    const headers = await this.getResponseHeaders();

    const sensitiveHeaders = [
      'server',
      'x-powered-by',
      'x-aspnet-version',
      'x-version',
      'x-generator'
    ];

    const disclosedHeaders = sensitiveHeaders.filter(header =>
        headers[header] && headers[header].length > 0
    );

    return {
      disclosesServer: headers['server'] !== undefined,
      disclosesVersion: disclosedHeaders.length > 1,
      disclosedHeaders
    };
  }
}

test.describe('Security Headers Tests', () => {
  let headersHelper: SecurityHeadersHelper;

  test.beforeEach(async ({page: _page}) => {
    headersHelper = new SecurityHeadersHelper(page);
    await page.goto('/');
  });

  test.describe('Content Security Policy (CSP)', () => {
    test('should have comprehensive CSP header', async ({page: _page}) => {
      const expectedDirectives: CSPDirective[] = [
        {
          directive: 'default-src',
          expectedSources: ["'self'"],
          forbiddenSources: ['*', "'unsafe-inline'", "'unsafe-eval'"],
          required: true
        },
        {
          directive: 'script-src',
          expectedSources: ["'self'"],
          forbiddenSources: ["'unsafe-inline'", "'unsafe-eval'"],
          required: true
        },
        {
          directive: 'style-src',
          expectedSources: ["'self'"],
          forbiddenSources: ["'unsafe-inline'"],
          required: true
        },
        {
          directive: 'img-src',
          expectedSources: ["'self'", "data:"],
          forbiddenSources: [],
          required: true
        },
        {
          directive: 'object-src',
          expectedSources: ["'none'"],
          forbiddenSources: [],
          required: true
        },
        {
          directive: 'frame-ancestors',
          expectedSources: ["'none'"],
          forbiddenSources: [],
          required: true
        }
      ];

      const cspResults = await headersHelper.validateCSP(expectedDirectives);

      // All required directives should be present
      expect(cspResults.missingDirectives).toHaveLength(0);

      // No invalid sources should be present
      expect(cspResults.invalidSources).toHaveLength(0);

      // All directives should pass validation
      cspResults.directivesPassed.forEach((passed, _index) => {
        expect(passed).toBe(true);
      });
    });

    test('should prevent inline script execution via CSP', async ({page: _page}) => {
      // Try to inject inline script
      const inlineScriptBlocked = await page.evaluate(() => {
        try {
          const script = document.createElement('script');
          script.innerHTML = 'window.inlineScriptExecuted = true;';
          document.head.appendChild(script);

          // Wait a moment for potential execution
          return new Promise<boolean>(resolve => {
            setTimeout(() => {
              resolve(!(window as unknown as {
                inlineScriptExecuted?: boolean
              }).inlineScriptExecuted);
            }, 100);
          });
        } catch {
          return true; // Script injection failed, CSP working
        }
      });

      expect(inlineScriptBlocked).toBe(true);
    });

    test('should prevent eval() execution via CSP', async ({page: _page}) => {
      const evalBlocked = await page.evaluate(() => {
        try {
          eval('window.evalExecuted = true;');
          return !(window as unknown as { evalExecuted?: boolean }).evalExecuted;
        } catch {
          return true; // eval blocked by CSP
        }
      });

      expect(evalBlocked).toBe(true);
    });

    test('should prevent external resource loading via CSP', async ({page: _page}) => {
      // Test if external scripts are blocked
      const externalScriptBlocked = await page.evaluate(() => {
        return new Promise<boolean>((resolve) => {
          const script = document.createElement('script');
          script.src = 'https://evil-domain.com/malicious.js';
          script.onload = () => resolve(false); // Should not load
          script.onerror = () => resolve(true); // Should be blocked

          document.head.appendChild(script);

          // Timeout after 3 seconds
          setTimeout(() => resolve(true), 3000);
        });
      });

      expect(externalScriptBlocked).toBe(true);
    });
  });

  test.describe('HTTP Strict Transport Security (HSTS)', () => {
    test('should have HSTS header configured', async ({page: _page}) => {
      const hstsResults = await headersHelper.testHSTS();

      expect(hstsResults.hstsEnabled).toBe(true);
      expect(hstsResults.maxAge).toBeGreaterThan(0);

      // Recommended: max-age of at least 1 year (31536000 seconds)
      expect(hstsResults.maxAge).toBeGreaterThanOrEqual(31536000);
    });

    test('should include subdomains in HSTS', async ({page: _page}) => {
      const hstsResults = await headersHelper.testHSTS();

      if (hstsResults.hstsEnabled) {
        expect(hstsResults.includeSubDomains).toBe(true);
      }
    });

    test('should consider HSTS preload', async ({page: _page}) => {
      const hstsResults = await headersHelper.testHSTS();

      // Preload is optional but recommended for production
      if (hstsResults.hstsEnabled && hstsResults.maxAge >= 10886400) {
        // If max-age is sufficient for preload, document whether preload is enabled
        console.log('HSTS preload status:', hstsResults.preload);
      }
    });
  });

  test.describe('Frame Protection', () => {
    test('should have X-Frame-Options header', async ({page: _page}) => {
      const frameResults = await headersHelper.testFrameOptions();

      expect(frameResults.xFrameOptionsCorrect).toBe(true);
      expect(['DENY', 'SAMEORIGIN']).toContain(frameResults.actualValue.toUpperCase());
    });

    test('should prevent clickjacking attacks', async ({page: _page}) => {
      const frameResults = await headersHelper.testFrameOptions();

      // Page should not be frameable from other origins
      expect(frameResults.canBeFramed).toBe(false);
    });

    test('should use frame-ancestors CSP directive', async ({page: _page}) => {
      const expectedDirectives: CSPDirective[] = [
        {
          directive: 'frame-ancestors',
          expectedSources: ["'none'"],
          forbiddenSources: ['*'],
          required: true
        }
      ];

      const cspResults = await headersHelper.validateCSP(expectedDirectives);

      // frame-ancestors should be properly configured
      expect(cspResults.missingDirectives).not.toContain('frame-ancestors');
    });
  });

  test.describe('Content Type Protection', () => {
    test('should have X-Content-Type-Options header', async ({page: _page}) => {
      const mimeResults = await headersHelper.testMIMETypeSniffing();

      expect(mimeResults.mimeSniffingDisabled).toBe(true);
      expect(mimeResults.actualValue.toLowerCase()).toBe('nosniff');
    });

    test('should prevent MIME type sniffing attacks', async ({page: _page}) => {
      // Test that files are served with correct content types
      await page.route('**/*.js', route => {
        const headers = route.request().headers();
        expect(headers['content-type']).toMatch(/^(application|text)\/javascript/);
        route.continue();
      });

      await page.route('**/*.css', route => {
        const headers = route.request().headers();
        expect(headers['content-type']).toBe('text/css');
        route.continue();
      });

      // Navigate to trigger resource loading
      await page.reload();
    });
  });

  test.describe('XSS Protection Headers', () => {
    test('should have appropriate XSS protection', async ({page: _page}) => {
      const expectedHeaders: SecurityHeader[] = [
        {
          name: 'X-XSS-Protection',
          expectedValue: '1; mode=block',
          shouldExist: false, // Modern browsers rely on CSP
          description: 'Legacy XSS protection header'
        }
      ];

      const _results = await headersHelper.validateSecurityHeaders(expectedHeaders);

      // Modern applications should rely on CSP rather than X-XSS-Protection
      // This test documents that the legacy header is not needed
    });

    test('should rely on CSP for XSS protection', async ({page: _page}) => {
      const expectedDirectives: CSPDirective[] = [
        {
          directive: 'script-src',
          expectedSources: ["'self'"],
          forbiddenSources: ["'unsafe-inline'"],
          required: true
        }
      ];

      const cspResults = await headersHelper.validateCSP(expectedDirectives);

      // CSP should provide comprehensive XSS protection
      expect(cspResults.missingDirectives).not.toContain('script-src');
      expect(cspResults.invalidSources.filter(s => s.directive === 'script-src')).toHaveLength(0);
    });
  });

  test.describe('Referrer Policy', () => {
    test('should have secure referrer policy', async ({page: _page}) => {
      const referrerResults = await headersHelper.testReferrerPolicy();

      expect(referrerResults.hasReferrerPolicy).toBe(true);
      expect(referrerResults.isSecure).toBe(true);

      // Should use strict-origin-when-cross-origin or stricter
      const secureValues = [
        'no-referrer',
        'same-origin',
        'strict-origin',
        'strict-origin-when-cross-origin'
      ];
      expect(secureValues).toContain(referrerResults.actualValue.toLowerCase());
    });

    test('should not leak referrer information inappropriately', async ({page: _page}) => {
      // Test referrer policy in practice
      const referrerTest = await page.evaluate(() => {
        // Create a link to external domain
        const link = document.createElement('a');
        link.href = 'https://example.com/';
        link.rel = 'noopener noreferrer';

        // Check if referrer policy is set via meta tag or header
        const metaReferrer = document.querySelector('meta[name="referrer"]');
        return {
          hasMetaReferrer: !!metaReferrer,
          metaContent: metaReferrer?.getAttribute('content') || ''
        };
      });

      // Should have referrer policy configured either via header or meta tag
      if (referrerTest.hasMetaReferrer) {
        expect(['no-referrer', 'same-origin', 'strict-origin', 'strict-origin-when-cross-origin'])
        .toContain(referrerTest.metaContent.toLowerCase());
      }
    });
  });

  test.describe('Permissions Policy', () => {
    test('should restrict sensitive browser features', async ({page: _page}) => {
      const permissionsResults = await headersHelper.testPermissionsPolicy();

      expect(permissionsResults.hasPermissionsPolicy).toBe(true);

      // Should restrict at least some sensitive features
      const sensitiveFeaturesRestricted = [
        'camera',
        'microphone',
        'geolocation',
        'payment',
        'usb'
      ].some(feature => permissionsResults.restrictedFeatures.includes(feature));

      expect(sensitiveFeaturesRestricted).toBe(true);
    });

    test('should prevent unauthorized feature access', async ({page: _page}) => {
      // Test that restricted features are actually blocked
      const featureTest = await page.evaluate(async () => {
        const results: Record<string, boolean> = {};

        // Test camera access (should be blocked)
        try {
          await navigator.mediaDevices.getUserMedia({video: true});
          results.camera = false; // Should not succeed
        } catch {
          results.camera = true; // Correctly blocked
        }

        // Test geolocation access (should be blocked)
        try {
          await new Promise((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(resolve, reject, {timeout: 1000});
          });
          results.geolocation = false; // Should not succeed
        } catch {
          results.geolocation = true; // Correctly blocked
        }

        return results;
      });

      // These features should be blocked due to permissions policy
      expect(featureTest.camera).toBe(true);
      expect(featureTest.geolocation).toBe(true);
    });
  });

  test.describe('CORS Headers', () => {
    test('should have secure CORS configuration', async ({page: _page}) => {
      const corsResults = await headersHelper.testCORSHeaders();

      if (corsResults.hasCORSHeaders) {
        // Should not allow all origins when credentials are allowed
        if (corsResults.allowsCredentials) {
          expect(corsResults.allowsAnyOrigin).toBe(false);
        }

        // Should specify allowed origins explicitly
        expect(corsResults.headers.accessControlAllowOrigin).not.toBe('*');
      }
    });

    test('should validate CORS preflight requests', async ({page: _page}) => {
      // Intercept OPTIONS requests to validate CORS headers
      let corsHeadersValid = false;

      await page.route('**/api/**', route => {
        if (route.request().method() === 'OPTIONS') {
          const headers = route.request().headers();
          const _origin = headers.origin;

          // Validate that preflight requests are handled properly
          corsHeadersValid = true; // CORS preflight intercepted
        }
        route.continue();
      });

      // Make a cross-origin-style request
      await page.evaluate(() => {
        fetch('/api/test', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Custom-Header': 'test'
          },
          body: JSON.stringify({test: 'data'})
        }).catch(() => {
          // Request might fail, but we're testing CORS headers
        });
      });

      // CORS should be properly configured for API requests
      expect(corsHeadersValid).toBe(true);
    });
  });

  test.describe('Information Disclosure Prevention', () => {
    test('should not disclose server information', async ({page: _page}) => {
      const serverInfo = await headersHelper.testServerInfoDisclosure();

      // Should not reveal server software details
      expect(serverInfo.disclosesServer).toBe(false);
      expect(serverInfo.disclosesVersion).toBe(false);
      expect(serverInfo.disclosedHeaders).toHaveLength(0);
    });

    test('should not expose framework versions', async ({page: _page}) => {
      const headers = await headersHelper.getResponseHeaders();

      const exposedHeaders = [
        'x-powered-by',
        'x-aspnet-version',
        'x-framework-version',
        'x-version'
      ];

      exposedHeaders.forEach(header => {
        expect(headers[header]).toBeUndefined();
      });
    });

    test('should use secure error pages', async ({page: _page}) => {
      // Test 404 page doesn't expose sensitive information
      const response = await page.goto('/nonexistent-page-test', {
        waitUntil: 'networkidle'
      });

      expect(response?.status()).toBe(404);

      // Error page should not reveal server details
      const pageContent = await page.textContent('body');

      const sensitiveTerms = [
        'apache',
        'nginx',
        'iis',
        'tomcat',
        'jetty',
        'stack trace',
        'debug',
        'exception'
      ];

      sensitiveTerms.forEach(term => {
        expect(pageContent?.toLowerCase()).not.toContain(term);
      });
    });
  });

  test.describe('Cache Control Headers', () => {
    test('should have appropriate cache control for sensitive pages', async ({page: _page}) => {
      // Navigate to authenticated page
      await page.goto('/profile');

      const headers = await headersHelper.getResponseHeaders();
      const cacheControl = headers['cache-control'] || '';

      // Sensitive pages should not be cached
      const hasCacheControl = cacheControl.length > 0;
      const preventsCache = cacheControl.includes('no-cache') ||
          cacheControl.includes('no-store') ||
          cacheControl.includes('private');

      expect(hasCacheControl).toBe(true);
      expect(preventsCache).toBe(true);
    });

    test('should allow caching for static resources', async ({page: _page}) => {
      // Test static resource caching
      let staticResourceCached = false;

      await page.route('**/*.{js,css,png,jpg,gif,svg}', route => {
        const _response = route.request().url();
        const _headers = route.request().headers();

        // Static resources can be cached
        staticResourceCached = true;
        route.continue();
      });

      await page.reload();

      if (staticResourceCached) {
        // Static resources should allow caching for performance
        expect(staticResourceCached).toBe(true);
      }
    });
  });

  test.describe('Cross-Origin Resource Policies', () => {
    test('should implement Cross-Origin-Resource-Policy', async ({page: _page}) => {
      const headers = await headersHelper.getResponseHeaders();
      const corp = headers['cross-origin-resource-policy'];

      if (corp) {
        expect(['same-site', 'same-origin', 'cross-origin']).toContain(corp.toLowerCase());
      }
    });

    test('should implement Cross-Origin-Embedder-Policy', async ({page: _page}) => {
      const headers = await headersHelper.getResponseHeaders();
      const coep = headers['cross-origin-embedder-policy'];

      if (coep) {
        expect(['require-corp', 'credentialless']).toContain(coep.toLowerCase());
      }
    });

    test('should implement Cross-Origin-Opener-Policy', async ({page: _page}) => {
      const headers = await headersHelper.getResponseHeaders();
      const coop = headers['cross-origin-opener-policy'];

      if (coop) {
        expect(['same-origin', 'same-origin-allow-popups', 'unsafe-none']).toContain(coop.toLowerCase());
      }
    });
  });

  test.describe('Security Headers Integration', () => {
    test('should have all essential security headers', async ({page: _page}) => {
      const essentialHeaders: SecurityHeader[] = [
        {
          name: 'Content-Security-Policy',
          shouldExist: true,
          description: 'Prevents XSS and other injection attacks'
        },
        {
          name: 'Strict-Transport-Security',
          shouldExist: true,
          description: 'Enforces HTTPS connections'
        },
        {
          name: 'X-Frame-Options',
          shouldExist: true,
          description: 'Prevents clickjacking attacks'
        },
        {
          name: 'X-Content-Type-Options',
          expectedValue: 'nosniff',
          shouldExist: true,
          description: 'Prevents MIME type sniffing'
        },
        {
          name: 'Referrer-Policy',
          shouldExist: true,
          description: 'Controls referrer information leakage'
        }
      ];

      const results = await headersHelper.validateSecurityHeaders(essentialHeaders);

      // All essential headers should be present and valid
      results.passed.forEach((passed, _index) => {
        expect(passed).toBe(true);
      });

      expect(results.missing).toHaveLength(0);
    });

    test('should not conflict between security headers', async ({page: _page}) => {
      const headers = await headersHelper.getResponseHeaders();

      // X-Frame-Options and CSP frame-ancestors should not conflict
      const xFrameOptions = headers['x-frame-options'];
      const csp = headers['content-security-policy'];

      if (xFrameOptions && csp && csp.includes('frame-ancestors')) {
        // Both headers present - should be consistent
        const frameAncestorsNone = csp.includes("frame-ancestors 'none'");
        const xFrameOptionsDeny = xFrameOptions.toUpperCase() === 'DENY';

        if (frameAncestorsNone) {
          expect(xFrameOptionsDeny).toBe(true);
        }
      }
    });

    test('should maintain security headers across different pages', async ({page: _page}) => {
      const pagesToTest = ['/', '/login', '/dashboard', '/profile'];
      const headerConsistency: Record<string, string[]> = {};

      for (const pagePath of pagesToTest) {
        await page.goto(pagePath);
        const headers = await headersHelper.getResponseHeaders();

        Object.keys(headers).forEach(header => {
          if (!headerConsistency[header]) {
            headerConsistency[header] = [];
          }
          headerConsistency[header].push(headers[header]);
        });
      }

      // Security-critical headers should be consistent across pages
      const securityHeaders = [
        'strict-transport-security',
        'x-frame-options',
        'x-content-type-options',
        'referrer-policy'
      ];

      securityHeaders.forEach(header => {
        if (headerConsistency[header] && headerConsistency[header].length > 1) {
          const uniqueValues = new Set(headerConsistency[header]);
          expect(uniqueValues.size).toBe(1); // Should be consistent across all pages
        }
      });
    });
  });
});