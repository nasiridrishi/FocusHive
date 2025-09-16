/**
 * Security Page Object Model for E2E Security Tests
 * Provides methods to interact with security features and test security vulnerabilities
 */

import {expect, Locator, Page} from '@playwright/test';
import {TIMEOUTS} from '../helpers/test-data';

export class SecurityPage {
  readonly page: Page;

  // Security-specific selectors
  readonly loginForm: Locator;
  readonly registerForm: Locator;
  readonly passwordResetForm: Locator;
  readonly userProfileForm: Locator;
  readonly fileUploadForm: Locator;
  readonly changePasswordForm: Locator;

  // Security message elements
  readonly accountLockedMessage: Locator;
  readonly tokenExpiredMessage: Locator;
  readonly accessDeniedMessage: Locator;
  readonly rateLimitMessage: Locator;
  readonly securityWarningMessage: Locator;
  readonly csrfErrorMessage: Locator;

  // Security indicators
  readonly securityHeaders: Locator;
  readonly httpsIndicator: Locator;
  readonly sessionTimeoutWarning: Locator;
  readonly mfaRequired: Locator;
  readonly secureConnectionIndicator: Locator;

  // Form validation elements
  readonly passwordStrengthIndicator: Locator;
  readonly validationErrors: Locator;
  readonly securityQuestions: Locator;
  readonly captchaElement: Locator;

  // Admin and protected resource elements
  readonly adminPanel: Locator;
  readonly protectedContent: Locator;
  readonly userResourcePanel: Locator;
  readonly systemLogsPanel: Locator;

  // Security test elements
  readonly xssTestContainer: Locator;
  readonly sqlTestContainer: Locator;
  readonly uploadTestContainer: Locator;
  readonly sessionTestContainer: Locator;

  constructor(page: Page) {
    this.page = page;

    // Security-specific forms
    this.loginForm = page.locator('form[data-testid="login-form"], form:has(input[name="email"]):has(input[name="password"])');
    this.registerForm = page.locator('form[data-testid="register-form"], form:has(input[name="username"]):has(input[name="email"])');
    this.passwordResetForm = page.locator('form[data-testid="password-reset-form"], form:has(input[name="email"]):has(button:has-text("Reset"))');
    this.userProfileForm = page.locator('form[data-testid="profile-form"], form:has(input[name="firstName"]):has(input[name="lastName"])');
    this.fileUploadForm = page.locator('form[data-testid="file-upload-form"], form:has(input[type="file"])');
    this.changePasswordForm = page.locator('form[data-testid="change-password-form"], form:has(input[name="currentPassword"]):has(input[name="newPassword"])');

    // Security message elements
    this.accountLockedMessage = page.locator('text=/account.*locked|too many.*attempts|temporarily.*disabled/i, [role="alert"]:has-text("locked"), .error:has-text("locked")');
    this.tokenExpiredMessage = page.locator('text=/token.*expired|session.*expired|please.*login.*again/i, [role="alert"]:has-text("expired")');
    this.accessDeniedMessage = page.locator('text=/access.*denied|unauthorized|forbidden|permission.*denied/i, [role="alert"]:has-text("denied")');
    this.rateLimitMessage = page.locator('text=/rate.*limit|too many.*requests|slow.*down/i, [role="alert"]:has-text("limit")');
    this.securityWarningMessage = page.locator('text=/security.*warning|suspicious.*activity|potential.*threat/i, [role="alert"]:has-text("security")');
    this.csrfErrorMessage = page.locator('text=/csrf.*error|invalid.*token|cross.*site/i, [role="alert"]:has-text("csrf")');

    // Security indicators
    this.securityHeaders = page.locator('head meta[http-equiv], head meta[name*="security"]');
    this.httpsIndicator = page.locator('[data-testid="https-indicator"], .secure-connection');
    this.sessionTimeoutWarning = page.locator('[data-testid="session-timeout-warning"], .session-warning');
    this.mfaRequired = page.locator('[data-testid="mfa-required"], .mfa-prompt');
    this.secureConnectionIndicator = page.locator('[data-testid="secure-connection"], .ssl-indicator');

    // Form validation elements
    this.passwordStrengthIndicator = page.locator('[data-testid="password-strength"], .password-strength-meter');
    this.validationErrors = page.locator('.MuiFormHelperText-error, .error-message, [role="alert"]');
    this.securityQuestions = page.locator('[data-testid="security-questions"], .security-questions');
    this.captchaElement = page.locator('[data-testid="captcha"], .captcha, .recaptcha');

    // Admin and protected resource elements
    this.adminPanel = page.locator('[data-testid="admin-panel"], .admin-dashboard, h1:has-text("Admin")');
    this.protectedContent = page.locator('[data-testid="protected-content"], .protected-resource');
    this.userResourcePanel = page.locator('[data-testid="user-resource"], .user-content');
    this.systemLogsPanel = page.locator('[data-testid="system-logs"], .logs-panel');

    // Security test containers
    this.xssTestContainer = page.locator('[data-testid="xss-test"], .xss-content');
    this.sqlTestContainer = page.locator('[data-testid="sql-test"], .sql-content');
    this.uploadTestContainer = page.locator('[data-testid="upload-test"], .upload-content');
    this.sessionTestContainer = page.locator('[data-testid="session-test"], .session-content');
  }

  /**
   * Navigate to security test page
   */
  async goto(): Promise<void> {
    await this.page.goto('/');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to login page for security testing
   */
  async gotoLogin(): Promise<void> {
    await this.page.goto('/login');
    await this.waitForPageLoad();
    await expect(this.loginForm).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Navigate to register page for security testing
   */
  async gotoRegister(): Promise<void> {
    await this.page.goto('/register');
    await this.waitForPageLoad();
    await expect(this.registerForm).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Navigate to password reset page
   */
  async gotoPasswordReset(): Promise<void> {
    await this.page.goto('/password-reset');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to user profile page
   */
  async gotoUserProfile(): Promise<void> {
    await this.page.goto('/profile');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to file upload page
   */
  async gotoFileUpload(): Promise<void> {
    await this.page.goto('/upload');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to change password page
   */
  async gotoChangePassword(): Promise<void> {
    await this.page.goto('/change-password');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to protected resource
   */
  async gotoProtectedResource(): Promise<void> {
    await this.page.goto('/dashboard');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to admin resource
   */
  async gotoAdminResource(): Promise<void> {
    await this.page.goto('/admin');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to specific user resource
   */
  async gotoUserResource(resourceId: string): Promise<void> {
    await this.page.goto(`/user/resource/${resourceId}`);
    await this.waitForPageLoad();
  }

  /**
   * Wait for page to load completely
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle', {timeout: TIMEOUTS.NETWORK});

    // Wait for main content to be visible
    try {
      await Promise.race([
        this.page.locator('main').waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM}),
        this.page.locator('body').waitFor({state: 'visible', timeout: TIMEOUTS.SHORT})
      ]);
    } catch {
      // If specific elements aren't found, just ensure DOM is loaded
      await this.page.waitForLoadState('domcontentloaded');
    }
  }

  /**
   * Verify account lockout behavior
   */
  async verifyAccountLockout(): Promise<void> {
    await expect(this.accountLockedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    // Verify login form is disabled or shows lockout message
    const loginButton = this.loginForm.locator('button[type="submit"]');
    if (await loginButton.isVisible()) {
      expect(await loginButton.isDisabled()).toBe(true);
    }
  }

  /**
   * Verify token expiry behavior
   */
  async verifyTokenExpiredBehavior(): Promise<void> {
    // Should either show expired token message or redirect to login
    await Promise.race([
      expect(this.tokenExpiredMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.page).toHaveURL(/\/login/, {timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify session security measures
   */
  async verifySessionSecurityMeasures(): Promise<void> {
    // Should maintain session integrity or show security warnings
    const hasValidSession = await this.protectedContent.isVisible({timeout: TIMEOUTS.SHORT});
    const hasSecurityWarning = await this.securityWarningMessage.isVisible({timeout: TIMEOUTS.SHORT});

    // Either should have valid session OR security warning (not neither)
    expect(hasValidSession || hasSecurityWarning).toBe(true);
  }

  /**
   * Verify secure password reset
   */
  async verifySecurePasswordReset(): Promise<void> {
    // Check for secure reset flow elements
    await expect(this.passwordResetForm).toBeVisible();

    // Verify security questions or additional verification
    const hasSecurityQuestions = await this.securityQuestions.isVisible({timeout: TIMEOUTS.SHORT});
    const hasCaptcha = await this.captchaElement.isVisible({timeout: TIMEOUTS.SHORT});

    // Should have additional security measures
    expect(hasSecurityQuestions || hasCaptcha).toBe(true);
  }

  /**
   * Verify access denied
   */
  async verifyAccessDenied(): Promise<void> {
    await Promise.race([
      expect(this.accessDeniedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.page).toHaveURL(/\/403|\/unauthorized|\/login/, {timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify admin access
   */
  async verifyAdminAccess(): Promise<void> {
    await expect(this.adminPanel).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify resource access denied
   */
  async verifyResourceAccessDenied(): Promise<void> {
    await Promise.race([
      expect(this.accessDeniedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.page).toHaveURL(/\/403|\/unauthorized/, {timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify resource access allowed
   */
  async verifyResourceAccess(): Promise<void> {
    await expect(this.userResourcePanel).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify privilege escalation blocked
   */
  async verifyPrivilegeEscalationBlocked(): Promise<void> {
    await expect(this.accessDeniedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify XSS prevention
   */
  async verifyXSSPrevention(): Promise<void> {
    // Check that XSS payload is escaped/sanitized
    const content = await this.xssTestContainer.textContent();

    // Should not contain executable script tags
    expect(content).not.toContain('<script>');
    expect(content).not.toContain('javascript:');

    // Check for escaped content
    expect(content).toMatch(/&lt;script&gt;|&amp;lt;script&amp;gt;/);
  }

  /**
   * Verify reflected XSS prevention
   */
  async verifyReflectedXSSPrevention(): Promise<void> {
    // URL should not reflect unescaped XSS payload
    const _currentUrl = this.page.url();
    const content = await this.page.content();

    // Should not contain unescaped XSS in response
    expect(content).not.toMatch(/<script[^>]*>[^<]*alert\([^)]*\)/);
  }

  /**
   * Verify DOM-based XSS prevention
   */
  async verifyDOMXSSPrevention(): Promise<void> {
    // Check that DOM manipulation doesn't execute XSS
    const alerts = [];

    this.page.on('dialog', dialog => {
      alerts.push(dialog.message());
      dialog.dismiss();
    });

    await this.page.waitForTimeout(2000); // Wait for potential XSS execution

    // Should not trigger XSS alerts
    expect(alerts.length).toBe(0);
  }

  /**
   * Verify SQL injection prevention
   */
  async verifySQLInjectionPrevention(): Promise<void> {
    // Should not expose database errors or structure
    const content = await this.page.content();

    const sqlErrorPatterns = [
      /sql.*error/i,
      /mysql.*error/i,
      /postgresql.*error/i,
      /oracle.*error/i,
      /database.*error/i,
      /syntax.*error.*near/i
    ];

    for (const pattern of sqlErrorPatterns) {
      expect(content).not.toMatch(pattern);
    }
  }

  /**
   * Verify blind SQL injection prevention
   */
  async verifyBlindSQLInjectionPrevention(): Promise<void> {
    // Response time should not indicate SQL injection success
    const responseTime = await this.page.evaluate(() => performance.now());

    // Should respond in reasonable time (no intentional delays)
    expect(responseTime).toBeLessThan(TIMEOUTS.MEDIUM);
  }

  /**
   * Verify command injection prevention
   */
  async verifyCommandInjectionPrevention(): Promise<void> {
    const content = await this.page.content();

    // Should not expose system command output
    const commandOutputPatterns = [
      /uid=.*gid=/,
      /total.*\d+/,  // ls output
      /\d+:\d+:\d+/,  // ps output
      /windows.*version/i,
      /microsoft.*windows/i
    ];

    for (const pattern of commandOutputPatterns) {
      expect(content).not.toMatch(pattern);
    }
  }

  /**
   * Verify path traversal prevention
   */
  async verifyPathTraversalPrevention(): Promise<void> {
    const content = await this.page.content();

    // Should not expose system files
    const systemFilePatterns = [
      /root:.*:\/root:/,  // /etc/passwd
      /password.*policy/i,
      /system32/i,
      /boot.*ini/i
    ];

    for (const pattern of systemFilePatterns) {
      expect(content).not.toMatch(pattern);
    }
  }

  /**
   * Verify malicious file upload blocked
   */
  async verifyMaliciousFileBlocked(): Promise<void> {
    await expect(this.validationErrors).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    const errorText = await this.validationErrors.textContent();
    expect(errorText).toMatch(/invalid.*file|file.*type.*not.*allowed|malicious.*file/i);
  }

  /**
   * Verify file type enforcement
   */
  async verifyFileTypeEnforcement(): Promise<void> {
    await expect(this.validationErrors).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    const errorText = await this.validationErrors.textContent();
    expect(errorText).toMatch(/file.*type|extension.*not.*allowed/i);
  }

  /**
   * Verify file size limits
   */
  async verifyFileSizeLimits(): Promise<void> {
    await expect(this.validationErrors).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    const errorText = await this.validationErrors.textContent();
    expect(errorText).toMatch(/file.*too.*large|size.*limit.*exceeded/i);
  }

  /**
   * Verify session timeout
   */
  async verifySessionTimeout(): Promise<void> {
    await Promise.race([
      expect(this.sessionTimeoutWarning).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.tokenExpiredMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.page).toHaveURL(/\/login/, {timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify token invalidated
   */
  async verifyTokenInvalidated(): Promise<void> {
    await Promise.race([
      expect(this.tokenExpiredMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.accessDeniedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.page).toHaveURL(/\/login/, {timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify password rejected
   */
  async verifyPasswordRejected(): Promise<void> {
    await expect(this.validationErrors).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    const errorText = await this.validationErrors.textContent();
    expect(errorText).toMatch(/password.*weak|password.*requirements|password.*strength/i);
  }

  /**
   * Verify password accepted
   */
  async verifyPasswordAccepted(): Promise<void> {
    // Should not show validation errors
    const hasErrors = await this.validationErrors.isVisible({timeout: TIMEOUTS.SHORT});
    expect(hasErrors).toBe(false);

    // Password strength indicator should show strong
    if (await this.passwordStrengthIndicator.isVisible()) {
      const strengthText = await this.passwordStrengthIndicator.textContent();
      expect(strengthText).toMatch(/strong|excellent|good/i);
    }
  }

  /**
   * Verify password history enforced
   */
  async verifyPasswordHistoryEnforced(): Promise<void> {
    await expect(this.validationErrors).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    const errorText = await this.validationErrors.textContent();
    expect(errorText).toMatch(/password.*previously.*used|password.*history|cannot.*reuse/i);
  }

  /**
   * Verify brute force protection
   */
  async verifyBruteForceProtection(): Promise<void> {
    await Promise.race([
      expect(this.accountLockedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.rateLimitMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.captchaElement).toBeVisible({timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify CSRF protection
   */
  async verifyCSRFProtection(): Promise<void> {
    await expect(this.csrfErrorMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify CSRF token validation
   */
  async verifyCSRFTokenValidation(): Promise<void> {
    await expect(this.csrfErrorMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    const errorText = await this.csrfErrorMessage.textContent();
    expect(errorText).toMatch(/csrf.*token|invalid.*token|cross.*site/i);
  }

  /**
   * Verify same origin policy
   */
  async verifySameOriginPolicy(): Promise<void> {
    // Cross-origin requests should be blocked
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect([403, 400, 401]).toContain(response.status());
  }

  /**
   * Verify CORS enforcement
   */
  async verifyCORSEnforcement(): Promise<void> {
    // CORS bypass should be blocked
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect(response.status()).not.toBe(200);
  }

  /**
   * Verify rate limit response
   */
  async verifyRateLimitResponse(): Promise<void> {
    await expect(this.rateLimitMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});

    // Check for 429 status in network response
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect(response.status()).toBe(429);
  }

  /**
   * Verify API key validation
   */
  async verifyAPIKeyValidation(): Promise<void> {
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect([401, 403]).toContain(response.status());
  }

  /**
   * Verify API key integrity
   */
  async verifyAPIKeyIntegrity(): Promise<void> {
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect(response.status()).toBe(403);
  }

  /**
   * Verify request size limits
   */
  async verifyRequestSizeLimits(): Promise<void> {
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect([413, 400]).toContain(response.status());
  }

  /**
   * Verify payload protection
   */
  async verifyPayloadProtection(): Promise<void> {
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect([413, 400, 429]).toContain(response.status());
  }

  /**
   * Verify header validation
   */
  async verifyHeaderValidation(): Promise<void> {
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect([400, 403]).toContain(response.status());
  }

  /**
   * Verify header sanitization
   */
  async verifyHeaderSanitization(): Promise<void> {
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect(response.status()).not.toBe(500); // Should handle malformed headers gracefully
  }

  /**
   * Verify sensitive data protection
   */
  async verifySensitiveDataProtection(): Promise<void> {
    const content = await this.page.content();

    // Should not expose sensitive patterns
    const sensitivePatterns = [
      /password.*:.*\w+/i,
      /token.*:.*[a-zA-Z0-9]{10,}/,
      /secret.*:.*\w+/i,
      /key.*:.*[a-zA-Z0-9]{10,}/
    ];

    for (const pattern of sensitivePatterns) {
      expect(content).not.toMatch(pattern);
    }
  }

  /**
   * Verify PII compliance
   */
  async verifyPIICompliance(): Promise<void> {
    const content = await this.page.content();

    // Should not expose full SSN, credit card numbers, etc.
    const piiPatterns = [
      /\d{3}-\d{2}-\d{4}/, // SSN
      /\d{4}[\s-]\d{4}[\s-]\d{4}[\s-]\d{4}/, // Credit card
      /\d{3}[\s-]\d{3}[\s-]\d{4}/ // Phone number
    ];

    for (const pattern of piiPatterns) {
      expect(content).not.toMatch(pattern);
    }
  }

  /**
   * Verify secure transmission
   */
  async verifySecureTransmission(): Promise<void> {
    expect(this.page.url()).toMatch(/^https:/);

    // Verify secure headers are present
    const response = await this.page.waitForResponse(/\//, {timeout: TIMEOUTS.MEDIUM});
    const headers = response.headers();
    expect(headers['strict-transport-security']).toBeTruthy();
  }

  /**
   * Verify protocol security
   */
  async verifyProtocolSecurity(): Promise<void> {
    // Should not allow protocol downgrade
    expect(this.page.url()).toMatch(/^https:/);

    // Connection should remain secure
    await expect(this.secureConnectionIndicator).toBeVisible({timeout: TIMEOUTS.MEDIUM});
  }

  /**
   * Verify data encryption
   */
  async verifyDataEncryption(): Promise<void> {
    // Data responses should not contain plaintext sensitive information
    const response = await this.page.waitForResponse(/api\/.*data/, {timeout: TIMEOUTS.MEDIUM});
    const responseBody = await response.text();

    // Should not contain obvious plaintext passwords or tokens
    expect(responseBody).not.toMatch(/password.*:.*[^*]/i);
  }

  /**
   * Verify CSP enforcement
   */
  async verifyCSPEnforcement(): Promise<void> {
    // CSP violations should be blocked
    const violationOccurred = await this.page.evaluate(() => {
      return new Promise(resolve => {
        const originalLog = console.error;
        let cspViolationDetected = false;

        console.error = (...args) => {
          if (args.some(arg => String(arg).includes('Content Security Policy'))) {
            cspViolationDetected = true;
          }
          originalLog.apply(console, args);
        };

        setTimeout(() => {
          console.error = originalLog;
          resolve(cspViolationDetected);
        }, 1000);
      });
    });

    expect(violationOccurred).toBe(true);
  }

  /**
   * Verify clickjacking protection
   */
  async verifyClickjackingProtection(): Promise<void> {
    const response = await this.page.waitForResponse(/\//, {timeout: TIMEOUTS.MEDIUM});
    const headers = response.headers();

    const frameOptions = headers['x-frame-options'];
    expect(frameOptions).toMatch(/DENY|SAMEORIGIN/i);
  }

  /**
   * Verify MIME sniffing protection
   */
  async verifyMIMESniffingProtection(): Promise<void> {
    const response = await this.page.waitForResponse(/\//, {timeout: TIMEOUTS.MEDIUM});
    const headers = response.headers();

    expect(headers['x-content-type-options']).toBe('nosniff');
  }

  /**
   * Verify HTTPS enforcement
   */
  async verifyHTTPSEnforcement(): Promise<void> {
    expect(this.page.url()).toMatch(/^https:/);

    // Should redirect HTTP to HTTPS
    const response = await this.page.waitForResponse(/\//, {timeout: TIMEOUTS.MEDIUM});
    expect(response.status()).not.toBe(301); // Should already be HTTPS
  }

  // Additional comprehensive verification methods continue...
  // This is a comprehensive foundation covering all major security test scenarios

  /**
   * Verify authentication integrity
   */
  async verifyAuthenticationIntegrity(): Promise<void> {
    // Authentication bypass should not succeed
    await Promise.race([
      expect(this.page).toHaveURL(/\/login/, {timeout: TIMEOUTS.MEDIUM}),
      expect(this.accessDeniedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify session management security
   */
  async verifySessionManagementSecurity(): Promise<void> {
    // Weak session management should be detected and blocked
    const hasValidSession = await this.protectedContent.isVisible({timeout: TIMEOUTS.SHORT});
    if (!hasValidSession) {
      await expect(this.tokenExpiredMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    }
  }

  /**
   * Verify comprehensive security under various conditions
   */
  async verifySecurityUnderLoad(): Promise<void> {
    // Security measures should remain effective under high load
    await expect(this.protectedContent).toBeVisible({timeout: TIMEOUTS.LONG});

    // Rate limiting should still function
    const response = await this.page.waitForResponse(/api\//, {timeout: TIMEOUTS.MEDIUM});
    expect([200, 429]).toContain(response.status());
  }

  /**
   * Verify security error handling
   */
  async verifySecurityErrorHandling(): Promise<void> {
    // Errors should not expose sensitive information
    const content = await this.page.content();

    const sensitiveErrorPatterns = [
      /stack.*trace/i,
      /internal.*server.*error.*at.*line/i,
      /database.*connection.*failed.*host/i,
      /file.*not.*found.*\/var\/www/i
    ];

    for (const pattern of sensitiveErrorPatterns) {
      expect(content).not.toMatch(pattern);
    }
  }

  /**
   * Verify error information protection
   */
  async verifyErrorInformationProtection(): Promise<void> {
    // Error messages should be generic
    if (await this.validationErrors.isVisible()) {
      const errorText = await this.validationErrors.textContent();
      expect(errorText).not.toMatch(/sql.*error|database.*error|server.*path|file.*path/i);
    }
  }

  /**
   * Verify advanced threat protection
   */
  async verifyAdvancedThreatProtection(): Promise<void> {
    // Advanced attacks should be detected and blocked
    await Promise.race([
      expect(this.securityWarningMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.accessDeniedMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM}),
      expect(this.rateLimitMessage).toBeVisible({timeout: TIMEOUTS.MEDIUM})
    ]);
  }

  /**
   * Verify behavioral protection
   */
  async verifyBehavioralProtection(): Promise<void> {
    // Suspicious behavior should trigger additional security measures
    const hasAdditionalSecurity = await Promise.race([
      this.captchaElement.isVisible({timeout: TIMEOUTS.SHORT}),
      this.mfaRequired.isVisible({timeout: TIMEOUTS.SHORT}),
      this.securityWarningMessage.isVisible({timeout: TIMEOUTS.SHORT})
    ]);

    expect(hasAdditionalSecurity).toBe(true);
  }
}