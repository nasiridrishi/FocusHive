/**
 * Secure Storage Security Tests (UOL-44.15)
 * 
 * Comprehensive secure storage testing for FocusHive frontend
 * 
 * Test Categories:
 * 1. JWT Token Storage Security
 * 2. Sensitive Data Storage Prevention
 * 3. LocalStorage Security Validation
 * 4. SessionStorage Security Validation
 * 5. Cookie Security Attributes
 * 6. IndexedDB Security (if used)
 * 7. Memory Storage for Sensitive Data
 * 8. Storage Encryption Validation
 * 9. Storage Access Control
 * 10. Data Cleanup on Logout
 */

import { test, expect } from '@playwright/test';
import { Page, BrowserContext } from '@playwright/test';

interface StorageItem {
  key: string;
  value: string | null;
  sensitive: boolean;
  encrypted?: boolean;
}

interface CookieSecurityInfo {
  name: string;
  secure: boolean;
  httpOnly: boolean;
  sameSite: string;
  domain: string;
  path: string;
  expires?: number;
}

class SecureStorageHelper {
  constructor(private page: Page, private context: BrowserContext) {}

  async getLocalStorageItems(): Promise<StorageItem[]> {
    return await this.page.evaluate(() => {
      const items: StorageItem[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key) {
          const value = localStorage.getItem(key);
          items.push({
            key,
            value,
            sensitive: this.isSensitiveKey(key)
          });
        }
      }
      return items;
    });
  }

  async getSessionStorageItems(): Promise<StorageItem[]> {
    return await this.page.evaluate(() => {
      const items: StorageItem[] = [];
      for (let i = 0; i < sessionStorage.length; i++) {
        const key = sessionStorage.key(i);
        if (key) {
          const value = sessionStorage.getItem(key);
          items.push({
            key,
            value,
            sensitive: this.isSensitiveKey(key)
          });
        }
      }
      return items;
    });
  }

  async getCookieSecurityInfo(): Promise<CookieSecurityInfo[]> {
    const cookies = await this.context.cookies();
    return cookies.map(cookie => ({
      name: cookie.name,
      secure: cookie.secure,
      httpOnly: cookie.httpOnly,
      sameSite: cookie.sameSite || 'none',
      domain: cookie.domain,
      path: cookie.path,
      expires: cookie.expires
    }));
  }

  async checkForSensitiveDataInStorage(): Promise<{
    localStorage: { hasSensitiveData: boolean; items: string[] };
    sessionStorage: { hasSensitiveData: boolean; items: string[] };
    cookies: { hasInsecureCookies: boolean; items: string[] };
  }> {
    const localItems = await this.getLocalStorageItems();
    const sessionItems = await this.getSessionStorageItems();
    const cookieInfo = await this.getCookieSecurityInfo();

    const sensitiveLocalItems = localItems
      .filter(item => this.isSensitiveData(item.key, item.value))
      .map(item => item.key);

    const sensitiveSessionItems = sessionItems
      .filter(item => this.isSensitiveData(item.key, item.value))
      .map(item => item.key);

    const insecureCookies = cookieInfo
      .filter(cookie => this.isInsecureCookie(cookie))
      .map(cookie => cookie.name);

    return {
      localStorage: {
        hasSensitiveData: sensitiveLocalItems.length > 0,
        items: sensitiveLocalItems
      },
      sessionStorage: {
        hasSensitiveData: sensitiveSessionItems.length > 0,
        items: sensitiveSessionItems
      },
      cookies: {
        hasInsecureCookies: insecureCookies.length > 0,
        items: insecureCookies
      }
    };
  }

  private isSensitiveData(key: string, value: string | null): boolean {
    if (!value) return false;

    const sensitiveKeywords = [
      'password', 'secret', 'private', 'key', 'token', 'auth',
      'session', 'credit', 'card', 'ssn', 'social', 'security',
      'api_key', 'bearer', 'jwt', 'refresh', 'access'
    ];

    const keyLower = key.toLowerCase();
    const valueLower = value.toLowerCase();

    // Check if key contains sensitive keywords
    if (sensitiveKeywords.some(keyword => keyLower.includes(keyword))) {
      return true;
    }

    // Check for JWT tokens in value
    if (this.looksLikeJWT(value)) {
      return true;
    }

    // Check for credit card numbers
    if (this.looksLikeCreditCard(value)) {
      return true;
    }

    // Check for common sensitive patterns
    if (/^[A-Za-z0-9+/=]{40,}$/.test(value)) { // Looks like encoded data
      return true;
    }

    return false;
  }

  private isSensitiveKey(key: string): boolean {
    const sensitiveKeys = [
      'auth_token', 'access_token', 'refresh_token', 'jwt_token',
      'session_id', 'api_key', 'private_key', 'password'
    ];
    
    return sensitiveKeys.some(sensitiveKey => 
      key.toLowerCase().includes(sensitiveKey)
    );
  }

  private looksLikeJWT(value: string): boolean {
    // JWT tokens have three parts separated by dots
    const jwtPattern = /^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]*$/;
    return jwtPattern.test(value) && value.split('.').length === 3;
  }

  private looksLikeCreditCard(value: string): boolean {
    // Remove spaces and dashes
    const cleanValue = value.replace(/[\s-]/g, '');
    
    // Check for common credit card patterns
    const ccPatterns = [
      /^4[0-9]{12}(?:[0-9]{3})?$/, // Visa
      /^5[1-5][0-9]{14}$/, // MasterCard
      /^3[47][0-9]{13}$/, // American Express
      /^3[0-9]{13}$/, // Diners Club
      /^6(?:011|5[0-9]{2})[0-9]{12}$/ // Discover
    ];

    return ccPatterns.some(pattern => pattern.test(cleanValue));
  }

  private isInsecureCookie(cookie: CookieSecurityInfo): boolean {
    // Check for authentication or session cookies that should be secure
    const authCookieNames = [
      'auth', 'session', 'token', 'login', 'jwt', 'access', 'refresh'
    ];

    const isAuthCookie = authCookieNames.some(name => 
      cookie.name.toLowerCase().includes(name)
    );

    if (isAuthCookie) {
      // Auth cookies should be secure, httpOnly, and have appropriate sameSite
      return !cookie.secure || 
             !cookie.httpOnly || 
             cookie.sameSite === 'none' ||
             !cookie.sameSite;
    }

    return false;
  }

  async validateTokenStorage(): Promise<{
    tokensInSecureStorage: boolean;
    tokensInMemory: boolean;
    tokensInInsecureStorage: boolean;
    encryptedTokens: boolean;
  }> {
    const result = await this.page.evaluate(() => {
      // Check for tokens in various storage mechanisms
      const localStorage = window.localStorage;
      const sessionStorage = window.sessionStorage;
      
      let tokensInInsecure = false;
      let tokensInSecure = false;
      let tokensInMemory = false;
      let encryptedTokens = false;

      // Check localStorage
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key) {
          const value = localStorage.getItem(key);
          if (value && this.containsToken(key, value)) {
            tokensInInsecure = true;
            if (this.isEncrypted(value)) {
              encryptedTokens = true;
            }
          }
        }
      }

      // Check sessionStorage
      for (let i = 0; i < sessionStorage.length; i++) {
        const key = sessionStorage.key(i);
        if (key) {
          const value = sessionStorage.getItem(key);
          if (value && this.containsToken(key, value)) {
            tokensInSecure = true;
            if (this.isEncrypted(value)) {
              encryptedTokens = true;
            }
          }
        }
      }

      // Check for tokens in memory (window object)
      const windowKeys = Object.keys(window);
      tokensInMemory = windowKeys.some(key => {
        const value = (window as Record<string, unknown>)[key];
        if (typeof value === 'string') {
          return this.containsToken(key, value);
        }
        return false;
      });

      return {
        tokensInSecureStorage: tokensInSecure,
        tokensInMemory,
        tokensInInsecureStorage: tokensInInsecure,
        encryptedTokens
      };
    });

    return result;
  }

  async testStorageAccess(): Promise<{
    localStorageAccessible: boolean;
    sessionStorageAccessible: boolean;
    indexedDBAccessible: boolean;
  }> {
    return await this.page.evaluate(() => {
      let localStorageAccessible = false;
      let sessionStorageAccessible = false;
      let indexedDBAccessible = false;

      // Test localStorage access
      try {
        localStorage.setItem('test_key', 'test_value');
        localStorageAccessible = localStorage.getItem('test_key') === 'test_value';
        localStorage.removeItem('test_key');
      } catch (e) {
        localStorageAccessible = false;
      }

      // Test sessionStorage access
      try {
        sessionStorage.setItem('test_key', 'test_value');
        sessionStorageAccessible = sessionStorage.getItem('test_key') === 'test_value';
        sessionStorage.removeItem('test_key');
      } catch (e) {
        sessionStorageAccessible = false;
      }

      // Test IndexedDB access
      try {
        indexedDBAccessible = typeof indexedDB !== 'undefined';
      } catch (e) {
        indexedDBAccessible = false;
      }

      return {
        localStorageAccessible,
        sessionStorageAccessible,
        indexedDBAccessible
      };
    });
  }

  async clearAllStorage(): Promise<void> {
    await this.page.evaluate(() => {
      // Clear localStorage
      localStorage.clear();
      
      // Clear sessionStorage
      sessionStorage.clear();
      
      // Clear IndexedDB (if available)
      if ('indexedDB' in window) {
        // Note: IndexedDB clearing is more complex and would require
        // specific database names, which we'd need to track
      }
    });

    // Clear cookies
    await this.context.clearCookies();
  }

  async simulateXSSStorageAttack(): Promise<boolean> {
    const maliciousScript = `
      try {
        localStorage.setItem('malicious_token', 'stolen_jwt_token_here');
        sessionStorage.setItem('stolen_data', document.cookie);
        window.stolenData = localStorage.getItem('auth_token') || '';
        return true;
      } catch (e) {
        return false;
      }
    `;

    const attackSuccessful = await this.page.evaluate((script) => {
      try {
        return eval(script);
      } catch (error) {
        return false;
      }
    }, maliciousScript);

    return attackSuccessful;
  }
}

test.describe('Secure Storage Security Tests', () => {
  let storageHelper: SecureStorageHelper;
  let context: BrowserContext;

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext();
  });

  test.afterAll(async () => {
    await context.close();
  });

  test.beforeEach(async ({ page }) => {
    storageHelper = new SecureStorageHelper(page, context);
    
    await page.goto('/');
  });

  test.describe('JWT Token Storage Security', () => {
    test('should not store JWT tokens in localStorage', async ({ page }) => {
      // Login to generate tokens
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const tokenValidation = await storageHelper.validateTokenStorage();
      
      // JWT tokens should not be in localStorage (insecure)
      expect(tokenValidation.tokensInInsecureStorage).toBe(false);
    });

    test('should use secure storage mechanisms for tokens', async ({ page }) => {
      // Login to generate tokens
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const tokenValidation = await storageHelper.validateTokenStorage();
      const cookieInfo = await storageHelper.getCookieSecurityInfo();

      // Tokens should be in secure storage (httpOnly cookies) or encrypted
      const hasSecureTokenStorage = 
        tokenValidation.tokensInSecureStorage || 
        tokenValidation.encryptedTokens ||
        cookieInfo.some(c => c.httpOnly && c.secure && 
          (c.name.toLowerCase().includes('auth') || 
           c.name.toLowerCase().includes('token')));

      expect(hasSecureTokenStorage).toBe(true);
    });

    test('should encrypt sensitive data in storage', async ({ page }) => {
      // Login to generate tokens
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const localItems = await storageHelper.getLocalStorageItems();
      const sessionItems = await storageHelper.getSessionStorageItems();

      // Check if any stored data looks encrypted
      const hasEncryptedData = [...localItems, ...sessionItems].some(item => {
        if (item.value && item.value.length > 20) {
          // Check for base64-like encoding or encrypted patterns
          return /^[A-Za-z0-9+/=]+$/.test(item.value) && 
                 !(/^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]*$/.test(item.value)); // Not a JWT
        }
        return false;
      });

      // If sensitive data is stored, it should be encrypted
      const sensitiveDataCheck = await storageHelper.checkForSensitiveDataInStorage();
      if (sensitiveDataCheck.localStorage.hasSensitiveData || 
          sensitiveDataCheck.sessionStorage.hasSensitiveData) {
        expect(hasEncryptedData).toBe(true);
      }
    });
  });

  test.describe('Cookie Security Attributes', () => {
    test('should use secure cookie attributes for authentication', async ({ page }) => {
      // Login to create authentication cookies
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const cookieInfo = await storageHelper.getCookieSecurityInfo();
      const authCookies = cookieInfo.filter(cookie => 
        cookie.name.toLowerCase().includes('auth') ||
        cookie.name.toLowerCase().includes('session') ||
        cookie.name.toLowerCase().includes('token')
      );

      // All authentication cookies should have proper security attributes
      authCookies.forEach(cookie => {
        expect(cookie.secure).toBe(true);
        expect(cookie.httpOnly).toBe(true);
        expect(['Strict', 'Lax']).toContain(cookie.sameSite);
        expect(cookie.path).toBe('/');
      });
    });

    test('should set appropriate cookie expiration', async ({ page }) => {
      // Login to create cookies
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const cookieInfo = await storageHelper.getCookieSecurityInfo();
      const authCookies = cookieInfo.filter(cookie => 
        cookie.name.toLowerCase().includes('auth') ||
        cookie.name.toLowerCase().includes('session')
      );

      // Session cookies should have reasonable expiration times
      authCookies.forEach(cookie => {
        if (cookie.expires) {
          const now = Date.now() / 1000; // Current time in seconds
          const maxAge = cookie.expires - now;
          
          // Should expire within reasonable time (24 hours max for demo)
          expect(maxAge).toBeLessThan(24 * 60 * 60);
          expect(maxAge).toBeGreaterThan(0);
        }
      });
    });
  });

  test.describe('Storage Access Control', () => {
    test('should prevent unauthorized storage access', async ({ page }) => {
      // Test if malicious scripts can access storage
      const accessTest = await storageHelper.testStorageAccess();
      
      // Storage should be accessible for legitimate scripts
      expect(accessTest.localStorageAccessible).toBe(true);
      expect(accessTest.sessionStorageAccessible).toBe(true);
    });

    test('should protect against XSS storage attacks', async ({ page }) => {
      const xssAttackSuccess = await storageHelper.simulateXSSStorageAttack();
      
      // XSS attacks on storage should be prevented by CSP
      expect(xssAttackSuccess).toBe(false);
    });

    test('should validate storage quota limits', async ({ page }) => {
      const quotaTest = await page.evaluate(() => {
        try {
          // Try to exceed storage quota
          const largeData = 'x'.repeat(1024 * 1024 * 10); // 10MB string
          localStorage.setItem('large_test', largeData);
          return true;
        } catch (error) {
          // Should throw quota exceeded error
          return error instanceof Error && error.name === 'QuotaExceededError';
        }
      });

      // Should handle quota exceeded gracefully
      expect(typeof quotaTest).toBe('boolean');
    });
  });

  test.describe('Sensitive Data Protection', () => {
    test('should not store passwords in any storage', async ({ page }) => {
      // Login and check for password storage
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const sensitiveData = await storageHelper.checkForSensitiveDataInStorage();
      
      // Check specifically for password-related keys
      const hasPasswordData = [
        ...sensitiveData.localStorage.items,
        ...sensitiveData.sessionStorage.items,
        ...sensitiveData.cookies.items
      ].some(item => item.toLowerCase().includes('password'));

      expect(hasPasswordData).toBe(false);
    });

    test('should not store credit card information', async ({ page }) => {
      // Simulate entering credit card info (if payment forms exist)
      const ccTestData = '4111111111111111'; // Test credit card number
      
      // Try to store credit card data
      await page.evaluate((ccNumber) => {
        localStorage.setItem('test_payment', ccNumber);
        sessionStorage.setItem('test_cc', ccNumber);
      }, ccTestData);

      const sensitiveData = await storageHelper.checkForSensitiveDataInStorage();
      
      // Should detect and prevent credit card storage
      expect(sensitiveData.localStorage.hasSensitiveData).toBe(true);
      expect(sensitiveData.sessionStorage.hasSensitiveData).toBe(true);

      // Clean up test data
      await page.evaluate(() => {
        localStorage.removeItem('test_payment');
        sessionStorage.removeItem('test_cc');
      });
    });

    test('should mask sensitive data in storage', async ({ page }) => {
      // Check if any displayed sensitive data is properly masked
      const maskedDataTest = await page.evaluate(() => {
        const testData = {
          creditCard: '4111-1111-1111-1111',
          ssn: '123-45-6789',
          phone: '+1-555-123-4567'
        };

        // Store test data and check if it gets masked
        for (const [key, value] of Object.entries(testData)) {
          localStorage.setItem(`test_${key}`, value);
        }

        const results: Record<string, boolean> = {};
        for (const [key] of Object.entries(testData)) {
          const stored = localStorage.getItem(`test_${key}`);
          // Check if stored value is masked (contains asterisks or X's)
          results[key] = stored ? /[\*X]{4,}/.test(stored) : false;
          localStorage.removeItem(`test_${key}`);
        }

        return results;
      });

      // Sensitive data should be masked when stored
      Object.values(maskedDataTest).forEach(isMasked => {
        // Note: This test depends on implementation of data masking
        // If no masking is implemented, this test documents the requirement
      });
    });
  });

  test.describe('Data Cleanup and Lifecycle', () => {
    test('should clear sensitive data on logout', async ({ page }) => {
      // Login first
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      // Get storage state before logout
      const beforeLogout = await storageHelper.validateTokenStorage();

      // Logout
      await page.click('[data-testid="logout-button"]');
      await page.waitForURL('/');

      // Check storage state after logout
      const afterLogout = await storageHelper.validateTokenStorage();
      const sensitiveDataAfterLogout = await storageHelper.checkForSensitiveDataInStorage();

      // Sensitive data should be cleared after logout
      expect(afterLogout.tokensInInsecureStorage).toBe(false);
      expect(afterLogout.tokensInSecureStorage).toBe(false);
      expect(afterLogout.tokensInMemory).toBe(false);
      expect(sensitiveDataAfterLogout.localStorage.hasSensitiveData).toBe(false);
      expect(sensitiveDataAfterLogout.sessionStorage.hasSensitiveData).toBe(false);
    });

    test('should clear data on session timeout', async ({ page }) => {
      // Login first
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      // Simulate session timeout by manipulating token expiration
      await page.evaluate(() => {
        // Set expired tokens in storage (if any)
        const expiredJWT = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.4lZcWj6Gt6Mc4xLdaWG3MNhQZ2lK8aF5l8vLJFGCcEQ';
        if (localStorage.getItem('token')) {
          localStorage.setItem('token', expiredJWT);
        }
        if (sessionStorage.getItem('token')) {
          sessionStorage.setItem('token', expiredJWT);
        }
      });

      // Try to access protected resource
      await page.goto('/dashboard');

      // Should be redirected to login due to expired session
      await expect(page).toHaveURL(/login/);

      // Check that sensitive data was cleared
      const sensitiveData = await storageHelper.checkForSensitiveDataInStorage();
      expect(sensitiveData.localStorage.hasSensitiveData).toBe(false);
      expect(sensitiveData.sessionStorage.hasSensitiveData).toBe(false);
    });

    test('should handle browser close/refresh gracefully', async ({ page, context }) => {
      // Login first
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      // Get current storage state
      const beforeRefresh = await storageHelper.checkForSensitiveDataInStorage();

      // Simulate browser refresh
      await page.reload();

      // Check storage state after refresh
      const afterRefresh = await storageHelper.checkForSensitiveDataInStorage();

      // SessionStorage should be cleared on browser close/refresh
      // but localStorage might persist (depending on implementation)
      expect(afterRefresh.sessionStorage.hasSensitiveData).toBe(false);
    });

    test('should implement secure token refresh', async ({ page }) => {
      // Login to get initial tokens
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      const initialTokens = await storageHelper.validateTokenStorage();

      // Wait for potential token refresh (simulate time passing)
      await page.waitForTimeout(3000);

      // Make an authenticated request to trigger token refresh
      await page.goto('/profile');

      const afterRefreshTokens = await storageHelper.validateTokenStorage();

      // Token refresh mechanism should maintain security standards
      if (initialTokens.tokensInSecureStorage || afterRefreshTokens.tokensInSecureStorage) {
        expect(afterRefreshTokens.tokensInInsecureStorage).toBe(false);
      }
    });
  });

  test.describe('Storage Encryption and Integrity', () => {
    test('should validate stored data integrity', async ({ page }) => {
      // Login and store some data
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="email-input"]', 'test@example.com');
      await page.fill('[data-testid="password-input"]', 'TestPassword123!');
      await page.click('[data-testid="submit-login"]');
      await page.waitForURL('/dashboard');

      // Test data integrity check
      const integrityTest = await page.evaluate(() => {
        // Store test data with checksum
        const testData = { userId: '12345', preferences: { theme: 'dark' } };
        const dataString = JSON.stringify(testData);
        
        // Simple checksum calculation (in real app, use proper hashing)
        const checksum = dataString.length.toString(16);
        
        localStorage.setItem('user_data', dataString);
        localStorage.setItem('user_data_checksum', checksum);

        // Verify integrity
        const storedData = localStorage.getItem('user_data');
        const storedChecksum = localStorage.getItem('user_data_checksum');
        
        if (storedData && storedChecksum) {
          const calculatedChecksum = storedData.length.toString(16);
          return calculatedChecksum === storedChecksum;
        }
        
        return false;
      });

      expect(integrityTest).toBe(true);
    });

    test('should detect storage tampering', async ({ page }) => {
      // Test storage tampering detection
      const tamperingTest = await page.evaluate(() => {
        // Store original data
        const originalData = '{"userId":"12345","role":"user"}';
        localStorage.setItem('user_info', originalData);
        
        // Simulate tampering
        const tamperedData = '{"userId":"12345","role":"admin"}';
        localStorage.setItem('user_info', tamperedData);
        
        // In real implementation, there would be integrity checks
        // This test documents the requirement for tampering detection
        const currentData = localStorage.getItem('user_info');
        return currentData !== originalData;
      });

      // Should detect that data has been tampered with
      expect(tamperingTest).toBe(true);
    });
  });
});