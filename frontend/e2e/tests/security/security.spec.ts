/**
 * E2E Security Tests for FocusHive Application (UOL-315)
 *
 * Comprehensive security testing covering:
 * 1. Authentication Security (login limits, JWT validation, session hijacking)
 * 2. Authorization & Access Control (RBAC, resource ownership, privilege escalation)
 * 3. Input Validation & Injection Prevention (XSS, SQL injection, command injection)
 * 4. Session Management (timeouts, concurrent sessions, secure logout)
 * 5. Password Security (strength, history, brute force protection)
 * 6. CSRF Protection (token validation, same-origin policy)
 * 7. API Security (rate limiting, API key validation, request limits)
 * 8. Data Security (sensitive data masking, PII protection, HTTPS)
 * 9. Security Headers (CSP, X-Frame-Options, HSTS)
 * 10. OWASP Top 10 Coverage (comprehensive vulnerability testing)
 */

import {expect, test} from '@playwright/test';
import {SecurityPage} from '../../pages/SecurityPage';
import {SecurityHelper} from '../../helpers/security.helper';
import {AuthHelper} from '../../helpers/auth.helper';
import {TEST_USERS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Security Comprehensive Testing Suite', () => {
  let securityPage: SecurityPage;
  let securityHelper: SecurityHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page}) => {
    // Initialize page objects and helpers
    securityPage = new SecurityPage(page);
    securityHelper = new SecurityHelper(page);
    authHelper = new AuthHelper(page);

    // Validate test environment
    validateTestEnvironment();

    // Clear any existing authentication
    await authHelper.clearStorage();
  });

  test.afterEach(async ({page: _page}) => {
    // Cleanup after each test
    await securityHelper.cleanup();
    await authHelper.clearStorage();
  });

  test.describe('Authentication Security', () => {
    test('should enforce login attempt limits and account lockout', async () => {
      await securityPage.goto();

      // Setup monitoring for lockout behavior
      const monitor = await securityHelper.setupBruteForceMonitoring();

      // Attempt multiple failed logins
      const maxAttempts = 5;
      for (let i = 0; i < maxAttempts + 2; i++) {
        await authHelper.navigateToLogin();
        await authHelper.fillLoginForm(TEST_USERS.INVALID_USER.username, 'wrong-password');
        await authHelper.submitLoginForm();

        if (i >= maxAttempts - 1) {
          // Should see account lockout message after max attempts
          await securityPage.verifyAccountLockout();
        }
      }

      // Verify lockout metrics
      const metrics = await monitor.getMetrics();
      expect(metrics.failedAttempts).toBeGreaterThanOrEqual(maxAttempts);
      expect(metrics.lockoutTriggered).toBe(true);
    });

    test('should validate JWT token expiry and refresh', async () => {
      // Login with valid user
      await authHelper.loginWithValidUser();
      await authHelper.verifyTokensStored();

      // Extract token and verify structure
      const tokenInfo = await securityHelper.extractJWTToken();
      expect(tokenInfo.isValid).toBe(true);
      expect(tokenInfo.hasExpiry).toBe(true);

      // Simulate expired token
      await securityHelper.simulateExpiredToken();

      // Attempt to access protected resource
      await securityPage.gotoProtectedResource();

      // Should be redirected to login or show token refresh
      await securityPage.verifyTokenExpiredBehavior();
    });

    test('should prevent session hijacking attacks', async () => {
      // Login and get session info
      await authHelper.loginWithValidUser();
      const originalSession = await securityHelper.getSessionInfo();

      // Simulate session fixation attack
      await securityHelper.simulateSessionFixation();

      // Try to access protected resources with fixed session
      await securityPage.gotoProtectedResource();
      await securityPage.verifySessionSecurityMeasures();

      // Verify original session is still valid
      await securityHelper.verifySessionIntegrity(originalSession);
    });

    test('should secure password reset flow', async () => {
      await securityPage.gotoPasswordReset();

      // Test password reset token validation
      await securityHelper.testPasswordResetSecurity();

      // Verify reset link expiry
      await securityHelper.verifyResetTokenExpiry();

      // Test against reset token manipulation
      await securityHelper.testResetTokenManipulation();

      // Verify secure password reset completion
      await securityPage.verifySecurePasswordReset();
    });

    test('should implement secure multi-factor authentication', async () => {
      // Test MFA bypass attempts
      await securityHelper.testMFABypass();

      // Verify MFA token validation
      await securityHelper.testMFATokenValidation();

      // Test MFA backup codes security
      await securityHelper.testMFABackupCodes();
    });
  });

  test.describe('Authorization & Access Control', () => {
    test('should enforce role-based access control (RBAC)', async () => {
      // Create users with different roles
      const adminUser = await securityHelper.createTestUser('admin');
      const regularUser = await securityHelper.createTestUser('user');

      // Test admin-only resources with regular user
      await authHelper.login(regularUser.username, regularUser.password);
      await securityPage.gotoAdminResource();
      await securityPage.verifyAccessDenied();

      // Test with admin user
      await authHelper.login(adminUser.username, adminUser.password);
      await securityPage.gotoAdminResource();
      await securityPage.verifyAdminAccess();
    });

    test('should validate resource ownership', async () => {
      // Create two users
      const user1 = await securityHelper.createTestUser('user');
      const user2 = await securityHelper.createTestUser('user');

      // User1 creates a resource
      await authHelper.login(user1.username, user1.password);
      const resourceId = await securityHelper.createUserResource();

      // User2 tries to access User1's resource
      await authHelper.login(user2.username, user2.password);
      await securityPage.gotoUserResource(resourceId);
      await securityPage.verifyResourceAccessDenied();

      // Verify User1 can still access their resource
      await authHelper.login(user1.username, user1.password);
      await securityPage.gotoUserResource(resourceId);
      await securityPage.verifyResourceAccess();
    });

    test('should prevent privilege escalation attacks', async () => {
      await authHelper.loginWithValidUser();

      // Test horizontal privilege escalation
      await securityHelper.testHorizontalPrivilegeEscalation();

      // Test vertical privilege escalation
      await securityHelper.testVerticalPrivilegeEscalation();

      // Test parameter manipulation
      await securityHelper.testParameterManipulation();

      // Verify escalation attempts are blocked
      await securityPage.verifyPrivilegeEscalationBlocked();
    });

    test('should validate API endpoint authorization', async () => {
      await authHelper.loginWithValidUser();

      // Test unauthorized API endpoints
      const unauthorizedEndpoints = [
        '/api/v1/admin/users',
        '/api/v1/admin/settings',
        '/api/v1/system/logs'
      ];

      for (const endpoint of unauthorizedEndpoints) {
        const response = await securityHelper.testUnauthorizedAPI(endpoint);
        expect(response.status).toBe(403);
      }
    });
  });

  test.describe('Input Validation & Injection Prevention', () => {
    test('should prevent XSS (Cross-Site Scripting) attacks', async () => {
      await authHelper.loginWithValidUser();
      await securityPage.gotoUserProfile();

      // Test stored XSS
      const xssPayloads = securityHelper.getXSSPayloads();
      for (const payload of xssPayloads) {
        await securityHelper.testXSSInjection(payload);
        await securityPage.verifyXSSPrevention();
      }

      // Test reflected XSS
      await securityHelper.testReflectedXSS();
      await securityPage.verifyReflectedXSSPrevention();

      // Test DOM-based XSS
      await securityHelper.testDOMBasedXSS();
      await securityPage.verifyDOMXSSPrevention();
    });

    test('should prevent SQL injection attacks', async () => {
      await authHelper.loginWithValidUser();

      // Test SQL injection in search fields
      const sqlPayloads = securityHelper.getSQLInjectionPayloads();
      for (const payload of sqlPayloads) {
        await securityHelper.testSQLInjection(payload);
        await securityPage.verifySQLInjectionPrevention();
      }

      // Test blind SQL injection
      await securityHelper.testBlindSQLInjection();
      await securityPage.verifyBlindSQLInjectionPrevention();
    });

    test('should prevent command injection attacks', async () => {
      await authHelper.loginWithValidUser();

      // Test command injection payloads
      const commandPayloads = securityHelper.getCommandInjectionPayloads();
      for (const payload of commandPayloads) {
        await securityHelper.testCommandInjection(payload);
        await securityPage.verifyCommandInjectionPrevention();
      }
    });

    test('should prevent path traversal attacks', async () => {
      await authHelper.loginWithValidUser();

      // Test directory traversal
      const traversalPayloads = securityHelper.getPathTraversalPayloads();
      for (const payload of traversalPayloads) {
        await securityHelper.testPathTraversal(payload);
        await securityPage.verifyPathTraversalPrevention();
      }
    });

    test('should validate file upload security', async () => {
      await authHelper.loginWithValidUser();
      await securityPage.gotoFileUpload();

      // Test malicious file uploads
      await securityHelper.testMaliciousFileUpload();
      await securityPage.verifyMaliciousFileBlocked();

      // Test file type validation
      await securityHelper.testFileTypeValidation();
      await securityPage.verifyFileTypeEnforcement();

      // Test file size limits
      await securityHelper.testFileSizeLimits();
      await securityPage.verifyFileSizeLimits();
    });
  });

  test.describe('Session Management Security', () => {
    test('should enforce session timeout handling', async () => {
      await authHelper.loginWithValidUser();

      // Monitor session timeout
      const sessionMonitor = await securityHelper.setupSessionMonitoring();

      // Simulate idle session
      await securityHelper.simulateIdleSession();

      // Attempt to access protected resource after timeout
      await securityPage.gotoProtectedResource();
      await securityPage.verifySessionTimeout();

      const sessionMetrics = await sessionMonitor.getMetrics();
      expect(sessionMetrics.timeoutEnforced).toBe(true);
    });

    test('should limit concurrent sessions', async () => {
      const user = TEST_USERS.VALID_USER;

      // Create multiple concurrent sessions
      const sessions = await securityHelper.createConcurrentSessions(user, 3);

      // Verify session limit enforcement
      await securityHelper.verifyConcurrentSessionLimits();

      // Clean up sessions
      for (const session of sessions) {
        await session.close();
      }
    });

    test('should implement secure logout with token invalidation', async () => {
      await authHelper.loginWithValidUser();
      const tokenBefore = await securityHelper.extractJWTToken();

      // Perform logout
      await authHelper.logout();
      await authHelper.verifyLoggedOut();

      // Verify tokens are invalidated
      await authHelper.verifyTokensCleared();

      // Try to use old token
      await securityHelper.testInvalidatedToken(tokenBefore.token);
      await securityPage.verifyTokenInvalidated();
    });

    test('should secure remember me functionality', async () => {
      await securityPage.gotoLogin();

      // Test remember me with security measures
      await securityHelper.testRememberMeSecurity();

      // Verify secure cookie attributes
      await securityHelper.verifySecureCookies();

      // Test remember me expiry
      await securityHelper.testRememberMeExpiry();
    });
  });

  test.describe('Password Security', () => {
    test('should enforce password strength requirements', async () => {
      await securityPage.gotoRegister();

      // Test weak passwords
      const weakPasswords = securityHelper.getWeakPasswords();
      for (const password of weakPasswords) {
        await securityHelper.testPasswordStrength(password);
        await securityPage.verifyPasswordRejected();
      }

      // Test strong password acceptance
      await securityHelper.testPasswordStrength('StrongP@ssw0rd123!');
      await securityPage.verifyPasswordAccepted();
    });

    test('should validate password history', async () => {
      await authHelper.loginWithValidUser();
      await securityPage.gotoChangePassword();

      // Attempt to reuse previous passwords
      await securityHelper.testPasswordHistory();
      await securityPage.verifyPasswordHistoryEnforced();
    });

    test('should implement brute force protection', async () => {
      const monitor = await securityHelper.setupBruteForceMonitoring();

      // Test password brute force attack
      await securityHelper.simulatePasswordBruteForce();

      // Verify protection mechanisms
      await securityPage.verifyBruteForceProtection();

      const metrics = await monitor.getMetrics();
      expect(metrics.protectionActivated).toBe(true);
    });
  });

  test.describe('CSRF Protection', () => {
    test('should validate CSRF tokens', async () => {
      await authHelper.loginWithValidUser();

      // Test requests without CSRF token
      await securityHelper.testCSRFAttack();
      await securityPage.verifyCSRFProtection();

      // Test CSRF token manipulation
      await securityHelper.testCSRFTokenManipulation();
      await securityPage.verifyCSRFTokenValidation();
    });

    test('should enforce same-origin policy', async () => {
      await authHelper.loginWithValidUser();

      // Test cross-origin requests
      await securityHelper.testCrossOriginRequests();
      await securityPage.verifySameOriginPolicy();

      // Test CORS bypass attempts
      await securityHelper.testCORSBypass();
      await securityPage.verifyCORSEnforcement();
    });
  });

  test.describe('API Security', () => {
    test('should enforce rate limiting with 429 responses', async () => {
      await authHelper.loginWithValidUser();

      // Test API rate limiting
      const rateLimitMonitor = await securityHelper.testRateLimiting();

      // Verify 429 responses
      await securityPage.verifyRateLimitResponse();

      const metrics = await rateLimitMonitor.getMetrics();
      expect(metrics.rateLimitHit).toBe(true);
      expect(metrics.responseStatus).toBe(429);
    });

    test('should validate API key security', async () => {
      // Test invalid API keys
      await securityHelper.testInvalidAPIKeys();
      await securityPage.verifyAPIKeyValidation();

      // Test API key manipulation
      await securityHelper.testAPIKeyManipulation();
      await securityPage.verifyAPIKeyIntegrity();
    });

    test('should enforce request size limits', async () => {
      await authHelper.loginWithValidUser();

      // Test oversized requests
      await securityHelper.testOversizedRequests();
      await securityPage.verifyRequestSizeLimits();

      // Test payload bomb attacks
      await securityHelper.testPayloadBombs();
      await securityPage.verifyPayloadProtection();
    });

    test('should validate request headers', async () => {
      await authHelper.loginWithValidUser();

      // Test header injection
      await securityHelper.testHeaderInjection();
      await securityPage.verifyHeaderValidation();

      // Test malformed headers
      await securityHelper.testMalformedHeaders();
      await securityPage.verifyHeaderSanitization();
    });
  });

  test.describe('Data Security', () => {
    test('should mask sensitive data in responses', async () => {
      await authHelper.loginWithValidUser();

      // Test sensitive data exposure
      await securityHelper.testSensitiveDataMasking();
      await securityPage.verifySensitiveDataProtection();

      // Test PII protection
      await securityHelper.testPIIProtection();
      await securityPage.verifyPIICompliance();
    });

    test('should enforce secure data transmission', async () => {
      await authHelper.loginWithValidUser();

      // Verify HTTPS enforcement
      await securityHelper.verifyHTTPSEnforcement();
      await securityPage.verifySecureTransmission();

      // Test protocol downgrade attacks
      await securityHelper.testProtocolDowngrade();
      await securityPage.verifyProtocolSecurity();
    });

    test('should implement data encryption at rest', async () => {
      await authHelper.loginWithValidUser();

      // Test encrypted data storage
      await securityHelper.testDataEncryption();
      await securityPage.verifyDataEncryption();
    });
  });

  test.describe('Security Headers', () => {
    test('should implement Content Security Policy (CSP)', async () => {
      await securityPage.goto();

      // Verify CSP headers
      const cspHeader = await securityHelper.getSecurityHeader('Content-Security-Policy');
      expect(cspHeader).toBeTruthy();

      // Test CSP violation
      await securityHelper.testCSPViolation();
      await securityPage.verifyCSPEnforcement();
    });

    test('should implement X-Frame-Options header', async () => {
      await securityPage.goto();

      // Verify X-Frame-Options header
      const frameOptions = await securityHelper.getSecurityHeader('X-Frame-Options');
      expect(frameOptions).toMatch(/DENY|SAMEORIGIN/);

      // Test clickjacking protection
      await securityHelper.testClickjackingProtection();
      await securityPage.verifyClickjackingProtection();
    });

    test('should implement X-Content-Type-Options header', async () => {
      await securityPage.goto();

      // Verify X-Content-Type-Options header
      const contentTypeOptions = await securityHelper.getSecurityHeader('X-Content-Type-Options');
      expect(contentTypeOptions).toBe('nosniff');

      // Test MIME type sniffing protection
      await securityHelper.testMIMESniffingProtection();
      await securityPage.verifyMIMESniffingProtection();
    });

    test('should implement Strict-Transport-Security header', async () => {
      await securityPage.goto();

      // Verify HSTS header
      const hsts = await securityHelper.getSecurityHeader('Strict-Transport-Security');
      expect(hsts).toBeTruthy();
      expect(hsts).toContain('max-age');

      // Test HTTPS enforcement
      await securityHelper.testHTTPSRedirection();
      await securityPage.verifyHTTPSEnforcement();
    });

    test('should implement comprehensive security headers', async () => {
      await securityPage.goto();

      // Test all security headers
      const securityHeaders = await securityHelper.getAllSecurityHeaders();

      // Required security headers
      const requiredHeaders = [
        'Content-Security-Policy',
        'X-Frame-Options',
        'X-Content-Type-Options',
        'Strict-Transport-Security',
        'X-XSS-Protection',
        'Referrer-Policy'
      ];

      for (const header of requiredHeaders) {
        expect(securityHeaders[header]).toBeTruthy();
      }
    });
  });

  test.describe('OWASP Top 10 Comprehensive Coverage', () => {
    test('should prevent broken authentication (A02:2021)', async () => {
      // Test authentication bypass
      await securityHelper.testAuthenticationBypass();
      await securityPage.verifyAuthenticationIntegrity();

      // Test weak session management
      await securityHelper.testWeakSessionManagement();
      await securityPage.verifySessionManagementSecurity();
    });

    test('should prevent sensitive data exposure (A02:2021)', async () => {
      await authHelper.loginWithValidUser();

      // Test data exposure in transit
      await securityHelper.testDataExposureInTransit();
      await securityPage.verifyDataTransitSecurity();

      // Test data exposure at rest
      await securityHelper.testDataExposureAtRest();
      await securityPage.verifyDataStorageSecurity();
    });

    test('should prevent XML external entities (XXE) (A05:2021)', async () => {
      await authHelper.loginWithValidUser();

      // Test XXE attacks
      await securityHelper.testXXEAttacks();
      await securityPage.verifyXXEProtection();
    });

    test('should prevent security misconfiguration (A05:2021)', async () => {
      // Test default credentials
      await securityHelper.testDefaultCredentials();
      await securityPage.verifyConfigurationSecurity();

      // Test debug information exposure
      await securityHelper.testDebugInformationExposure();
      await securityPage.verifyDebugInfoProtection();
    });

    test('should prevent vulnerable components (A06:2021)', async () => {
      // Test component vulnerabilities
      await securityHelper.testVulnerableComponents();
      await securityPage.verifyComponentSecurity();
    });

    test('should prevent insufficient logging (A09:2021)', async () => {
      await authHelper.loginWithValidUser();

      // Test security event logging
      await securityHelper.testSecurityEventLogging();
      await securityPage.verifySecurityLogging();

      // Test log tampering protection
      await securityHelper.testLogTamperingProtection();
      await securityPage.verifyLogIntegrity();
    });

    test('should prevent server-side request forgery (A10:2021)', async () => {
      await authHelper.loginWithValidUser();

      // Test SSRF attacks
      await securityHelper.testSSRFAttacks();
      await securityPage.verifySSRFProtection();
    });
  });

  test.describe('Advanced Security Testing', () => {
    test('should handle security during high load conditions', async () => {
      // Setup concurrent security testing
      const concurrentTests = await securityHelper.setupConcurrentSecurityTests();

      // Run security tests under load
      await Promise.all(concurrentTests.map(test => test.execute()));

      // Verify security measures remain effective
      await securityPage.verifySecurityUnderLoad();
    });

    test('should maintain security during error conditions', async () => {
      await authHelper.loginWithValidUser();

      // Test security during application errors
      await securityHelper.testSecurityDuringErrors();
      await securityPage.verifySecurityErrorHandling();

      // Test information disclosure in errors
      await securityHelper.testErrorInformationDisclosure();
      await securityPage.verifyErrorInformationProtection();
    });

    test('should protect against advanced persistent threats', async () => {
      await authHelper.loginWithValidUser();

      // Test advanced attack patterns
      await securityHelper.testAdvancedAttackPatterns();
      await securityPage.verifyAdvancedThreatProtection();

      // Test behavioral analysis
      await securityHelper.testBehavioralAnalysis();
      await securityPage.verifyBehavioralProtection();
    });
  });
});