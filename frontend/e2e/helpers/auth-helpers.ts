/**
 * Enhanced Authentication Helper Functions for E2E Tests
 * Comprehensive authentication flow utilities including OAuth, email verification, and session management
 */

import { Page, expect, Browser, BrowserContext } from '@playwright/test';
import { 
  AUTH_TEST_USERS, 
  INVALID_CREDENTIALS, 
  OAUTH_PROVIDERS, 
  MOCK_OAUTH_PROFILES,
  PASSWORD_RESET_SCENARIOS,
  MAILHOG_CONFIG,
  generateUniqueAuthUser,
  type TestUser,
  type OAuthProvider,
  MOCK_JWT_TOKENS,
  SESSION_SCENARIOS
} from './auth-fixtures';

export interface EmailMessage {
  ID: string;
  From: { Mailbox: string; Domain: string };
  To: Array<{ Mailbox: string; Domain: string }>;
  Subject: string;
  Content: {
    Headers: Record<string, string[]>;
    Body: string;
    Size: number;
  };
  Created: string;
}

export interface TokenInfo {
  accessToken: string | null;
  refreshToken: string | null;
  isValid: boolean;
  location: 'localStorage' | 'sessionStorage' | 'none';
}

export interface ApiRequestInfo {
  url: string;
  method: string;
  postData: Record<string, unknown> | null;
  timestamp: number;
}

export class EnhancedAuthHelper {
  constructor(private page: Page) {}

  /**
   * Navigate to registration page and verify it loads
   */
  async navigateToRegistration(): Promise<void> {
    await this.page.goto('/register');
    await this.page.waitForLoadState('networkidle');
    
    // Verify we're on the registration page
    await expect(this.page.locator('input[name="username"]')).toBeVisible({ timeout: 10000 });
    await expect(this.page.locator('input[name="email"]')).toBeVisible({ timeout: 10000 });
    await expect(this.page.locator('input[name="password"]')).toBeVisible({ timeout: 10000 });
    await expect(this.page.locator('input[name="firstName"]')).toBeVisible({ timeout: 10000 });
    await expect(this.page.locator('input[name="lastName"]')).toBeVisible({ timeout: 10000 });
  }

  /**
   * Navigate to login page and verify it loads
   */
  async navigateToLogin(): Promise<void> {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
    
    // Verify we're on the login page
    await expect(this.page.locator('#email, input[name="email"]')).toBeVisible({ timeout: 10000 });
    await expect(this.page.locator('#password, input[name="password"]')).toBeVisible({ timeout: 10000 });
  }

  /**
   * Navigate to password reset page
   */
  async navigateToPasswordReset(): Promise<void> {
    await this.page.goto('/forgot-password');
    await this.page.waitForLoadState('networkidle');
    
    // Verify we're on the password reset page
    await expect(this.page.locator('input[name="email"]')).toBeVisible({ timeout: 10000 });
  }

  /**
   * Fill registration form with comprehensive validation
   */
  async fillRegistrationForm(userData: TestUser): Promise<void> {
    await this.page.locator('input[name="username"]').fill(userData.username);
    await this.page.locator('input[name="email"]').fill(userData.email);
    await this.page.locator('input[name="password"]').fill(userData.password);
    await this.page.locator('input[name="firstName"]').fill(userData.firstName);
    await this.page.locator('input[name="lastName"]').fill(userData.lastName);
    
    // Wait for validation to complete
    await this.page.waitForTimeout(500);
  }

  /**
   * Submit registration form and handle potential errors
   */
  async submitRegistrationForm(): Promise<void> {
    const submitButton = this.page.locator('button[type="submit"]:has-text("Create Account"), button[type="submit"]:has-text("Register")');
    await submitButton.click();
    
    // Wait for form submission to complete
    await this.page.waitForTimeout(1000);
  }

  /**
   * Complete user registration with email verification
   */
  async registerUserWithVerification(userData: TestUser): Promise<{ user: TestUser; verificationToken: string }> {
    const uniqueUser = generateUniqueAuthUser(userData);
    
    await this.navigateToRegistration();
    await this.fillRegistrationForm(uniqueUser);
    await this.submitRegistrationForm();
    
    // Wait for success message
    await this.verifySuccessMessage('Registration successful');
    
    // Get verification email
    const verificationEmail = await this.getVerificationEmail(uniqueUser.email);
    const verificationToken = this.extractVerificationToken(verificationEmail.Content.Body);
    
    return { user: uniqueUser, verificationToken };
  }

  /**
   * Verify email address using token
   */
  async verifyEmail(token: string): Promise<void> {
    await this.page.goto(`/verify-email?token=${token}`);
    await this.page.waitForLoadState('networkidle');
    
    // Check for verification success
    await this.verifySuccessMessage('Email verified successfully');
  }

  /**
   * Login with credentials and optional remember me
   */
  async loginWithCredentials(username: string, password: string, rememberMe = false): Promise<TokenInfo> {
    await this.navigateToLogin();
    
    await this.page.locator('#email, input[name="email"]').fill(username);
    await this.page.locator('#password, input[name="password"]').fill(password);
    
    if (rememberMe) {
      const rememberCheckbox = this.page.locator('input[type="checkbox"][name="rememberMe"], input[type="checkbox"]:near(:text("Remember"))');
      if (await rememberCheckbox.isVisible()) {
        await rememberCheckbox.check();
      }
    }
    
    await this.page.locator('button[type="submit"]:has-text("Sign In"), button[type="submit"]:has-text("Signing in")').click();
    
    // Wait for login to complete
    await this.page.waitForTimeout(2000);
    
    return await this.getTokenInfo();
  }

  /**
   * Test invalid login scenarios
   */
  async testInvalidLogin(credentials: { username: string; password: string }): Promise<void> {
    await this.navigateToLogin();
    
    await this.page.locator('#email, input[name="email"]').fill(credentials.username);
    await this.page.locator('#password, input[name="password"]').fill(credentials.password);
    await this.page.locator('button[type="submit"]:has-text("Sign In"), button[type="submit"]:has-text("Signing in")').click();
    
    // Verify error message appears
    await this.verifyErrorMessage();
    
    // Verify user stays on login page
    await expect(this.page).toHaveURL(/\/login/);
    
    // Verify no tokens are stored
    const tokenInfo = await this.getTokenInfo();
    expect(tokenInfo.accessToken).toBeNull();
    expect(tokenInfo.refreshToken).toBeNull();
  }

  /**
   * Request password reset
   */
  async requestPasswordReset(email: string): Promise<void> {
    await this.navigateToPasswordReset();
    
    await this.page.locator('input[name="email"]').fill(email);
    await this.page.locator('button[type="submit"]:has-text("Send Reset"), button[type="submit"]:has-text("Reset Password")').click();
    
    // Wait for request to complete
    await this.page.waitForTimeout(1000);
  }

  /**
   * Complete password reset flow
   */
  async completePasswordReset(email: string, newPassword: string): Promise<{ resetToken: string }> {
    await this.requestPasswordReset(email);
    
    // Get reset email
    const resetEmail = await this.getPasswordResetEmail(email);
    const resetToken = this.extractPasswordResetToken(resetEmail.Content.Body);
    
    // Navigate to reset password page with token
    await this.page.goto(`/reset-password?token=${resetToken}`);
    await this.page.waitForLoadState('networkidle');
    
    // Fill new password
    await this.page.locator('input[name="password"]').fill(newPassword);
    await this.page.locator('input[name="confirmPassword"]').fill(newPassword);
    await this.page.locator('button[type="submit"]:has-text("Update Password"), button[type="submit"]:has-text("Reset Password")').click();
    
    // Wait for completion
    await this.page.waitForTimeout(1000);
    
    return { resetToken };
  }

  /**
   * Initiate OAuth login flow
   */
  async initiateOAuthLogin(provider: keyof typeof OAUTH_PROVIDERS): Promise<void> {
    await this.navigateToLogin();
    
    const oauthButton = this.page.locator(`button:has-text("Continue with ${OAUTH_PROVIDERS[provider].name}"), a:has-text("${OAUTH_PROVIDERS[provider].name}")`);
    await oauthButton.click();
    
    // Wait for OAuth redirect
    await this.page.waitForTimeout(1000);
  }

  /**
   * Mock OAuth provider response
   */
  async mockOAuthResponse(provider: keyof typeof OAUTH_PROVIDERS, success = true): Promise<void> {
    const providerConfig = OAUTH_PROVIDERS[provider];
    const userProfile = MOCK_OAUTH_PROFILES[provider];
    
    if (success) {
      await this.page.route(`**${providerConfig.mockUrl}**`, route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: userProfile,
            token: MOCK_JWT_TOKENS.VALID_ACCESS_TOKEN,
            refreshToken: MOCK_JWT_TOKENS.VALID_REFRESH_TOKEN,
          }),
        });
      });
    } else {
      await this.page.route(`**${providerConfig.mockUrl}**`, route => {
        route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'oauth_error',
            error_description: 'OAuth authentication failed',
          }),
        });
      });
    }
  }

  /**
   * Test OAuth login flow
   */
  async testOAuthLogin(provider: keyof typeof OAUTH_PROVIDERS): Promise<TokenInfo> {
    await this.mockOAuthResponse(provider, true);
    await this.initiateOAuthLogin(provider);
    
    // Wait for OAuth flow to complete
    await this.page.waitForTimeout(3000);
    
    return await this.getTokenInfo();
  }

  /**
   * Logout user and verify cleanup
   */
  async logout(): Promise<void> {
    // Try to find user menu first
    const userMenu = this.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
    
    try {
      await userMenu.waitFor({ state: 'visible', timeout: 5000 });
      await userMenu.click();
      
      const logoutButton = this.page.locator('button:has-text("Logout"), [data-testid="logout-button"], button:has-text("Sign out")');
      await logoutButton.waitFor({ state: 'visible', timeout: 2000 });
      await logoutButton.click();
    } catch {
      // Try direct logout button
      const logoutButton = this.page.locator('button:has-text("Logout"), [data-testid="logout-button"], button:has-text("Sign out")');
      await logoutButton.click();
    }
    
    // Wait for logout to complete
    await this.page.waitForTimeout(1000);
  }

  /**
   * Test "logout all devices" functionality
   */
  async logoutAllDevices(): Promise<void> {
    const userMenu = this.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
    await userMenu.click();
    
    const logoutAllButton = this.page.locator('button:has-text("Logout All Devices"), [data-testid="logout-all-button"]');
    await logoutAllButton.click();
    
    // Confirm if needed
    const confirmButton = this.page.locator('button:has-text("Confirm"), button:has-text("Yes")');
    if (await confirmButton.isVisible({ timeout: 2000 })) {
      await confirmButton.click();
    }
    
    await this.page.waitForTimeout(1000);
  }

  /**
   * Test session timeout handling
   */
  async testSessionTimeout(): Promise<void> {
    // Mock token expiry
    await this.page.addInitScript(() => {
      // Override token storage to simulate expired tokens
      const originalSetItem = Storage.prototype.setItem;
      Storage.prototype.setItem = function(key, value) {
        if (key === 'access_token' || key === 'refresh_token') {
          // Set an expired token
          value = 'expired.token.here';
        }
        originalSetItem.call(this, key, value);
      };
    });
    
    // Try to access a protected route
    await this.page.goto('/dashboard');
    await this.page.waitForLoadState('networkidle');
    
    // Should be redirected to login
    await expect(this.page).toHaveURL(/\/login/);
  }

  /**
   * Get comprehensive token information
   */
  async getTokenInfo(): Promise<TokenInfo> {
    const [accessToken, refreshToken] = await Promise.all([
      this.page.evaluate(() => {
        return sessionStorage.getItem('access_token') || localStorage.getItem('access_token');
      }),
      this.page.evaluate(() => {
        return sessionStorage.getItem('refresh_token') || localStorage.getItem('refresh_token');
      }),
    ]);
    
    let location: TokenInfo['location'] = 'none';
    
    if (accessToken) {
      const sessionAccess = await this.page.evaluate(() => sessionStorage.getItem('access_token'));
      const localAccess = await this.page.evaluate(() => localStorage.getItem('access_token'));
      
      location = sessionAccess ? 'sessionStorage' : localAccess ? 'localStorage' : 'none';
    }
    
    const isValid = accessToken ? this.isValidJWTFormat(accessToken) : false;
    
    return {
      accessToken,
      refreshToken,
      isValid,
      location,
    };
  }

  /**
   * Verify JWT token format
   */
  private isValidJWTFormat(token: string): boolean {
    const parts = token.split('.');
    return parts.length === 3;
  }

  /**
   * Test concurrent sessions
   */
  async testConcurrentSessions(browser: Browser, users: TestUser[]): Promise<BrowserContext[]> {
    const contexts: BrowserContext[] = [];
    
    for (const user of users) {
      const context = await browser.newContext();
      const page = await context.newPage();
      
      const helper = new EnhancedAuthHelper(page);
      await helper.loginWithCredentials(user.username, user.password);
      
      // Verify login success
      const tokenInfo = await helper.getTokenInfo();
      expect(tokenInfo.accessToken).toBeTruthy();
      
      contexts.push(context);
    }
    
    return contexts;
  }

  /**
   * Clear all browser storage
   */
  async clearStorage(): Promise<void> {
    try {
      await this.page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
        // Clear cookies
        document.cookie.split(";").forEach(c => {
          const eqPos = c.indexOf("=");
          const name = eqPos > -1 ? c.substr(0, eqPos) : c;
          document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/";
        });
      });
    } catch {
      // If storage is not accessible, navigate to basic page first
      await this.page.goto('data:text/html,<html><body>Clearing storage...</body></html>');
      await this.page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
      });
    }
  }

  /**
   * Verify error message is displayed
   */
  async verifyErrorMessage(expectedMessage?: string): Promise<void> {
    const errorElement = this.page.locator('[role="alert"], .error, .MuiFormHelperText-error, .error-message');
    await expect(errorElement).toBeVisible({ timeout: 5000 });
    
    if (expectedMessage) {
      await expect(errorElement).toContainText(expectedMessage);
    }
  }

  /**
   * Verify success message is displayed
   */
  async verifySuccessMessage(expectedMessage?: string): Promise<void> {
    const successElement = this.page.locator('.success, .MuiAlert-standardSuccess, [role="status"]');
    await expect(successElement).toBeVisible({ timeout: 5000 });
    
    if (expectedMessage) {
      await expect(successElement).toContainText(expectedMessage);
    }
  }

  /**
   * Verify user is on dashboard
   */
  async verifyOnDashboard(): Promise<void> {
    await expect(this.page).toHaveURL(/\/dashboard|\/home|\/app/, { timeout: 10000 });
    
    // Verify user menu is visible
    const userMenu = this.page.locator('[data-testid="user-menu"], .user-avatar, [aria-label="Account"]');
    await expect(userMenu).toBeVisible({ timeout: 5000 });
  }

  /**
   * Test account lockout after failed attempts
   */
  async testAccountLockout(username: string, maxAttempts = 5): Promise<void> {
    for (let i = 0; i < maxAttempts; i++) {
      await this.testInvalidLogin({ username, password: 'wrongpassword' });
      
      if (i < maxAttempts - 1) {
        // Wait a bit between attempts
        await this.page.waitForTimeout(500);
      }
    }
    
    // Next attempt should show lockout message
    await this.testInvalidLogin({ username, password: 'wrongpassword' });
    await this.verifyErrorMessage('Account temporarily locked');
  }

  /**
   * Get verification email from MailHog
   */
  async getVerificationEmail(email: string, timeout = 30000): Promise<EmailMessage> {
    const startTime = Date.now();
    
    while (Date.now() - startTime < timeout) {
      try {
        const response = await fetch(`${MAILHOG_CONFIG.API_BASE_URL}${MAILHOG_CONFIG.MESSAGES_ENDPOINT}?limit=50`);
        const data = await response.json();
        
        const message = data.items?.find((msg: EmailMessage) => 
          msg.To.some(to => `${to.Mailbox}@${to.Domain}` === email) &&
          msg.Subject.toLowerCase().includes('verify')
        );
        
        if (message) {
          return message;
        }
        
        await this.page.waitForTimeout(1000);
      } catch (error) {
        console.warn('Failed to fetch emails from MailHog:', error);
        await this.page.waitForTimeout(1000);
      }
    }
    
    throw new Error(`Verification email not found for ${email} within ${timeout}ms`);
  }

  /**
   * Get password reset email from MailHog
   */
  async getPasswordResetEmail(email: string, timeout = 30000): Promise<EmailMessage> {
    const startTime = Date.now();
    
    while (Date.now() - startTime < timeout) {
      try {
        const response = await fetch(`${MAILHOG_CONFIG.API_BASE_URL}${MAILHOG_CONFIG.MESSAGES_ENDPOINT}?limit=50`);
        const data = await response.json();
        
        const message = data.items?.find((msg: EmailMessage) => 
          msg.To.some(to => `${to.Mailbox}@${to.Domain}` === email) &&
          (msg.Subject.toLowerCase().includes('reset') || msg.Subject.toLowerCase().includes('password'))
        );
        
        if (message) {
          return message;
        }
        
        await this.page.waitForTimeout(1000);
      } catch (error) {
        console.warn('Failed to fetch emails from MailHog:', error);
        await this.page.waitForTimeout(1000);
      }
    }
    
    throw new Error(`Password reset email not found for ${email} within ${timeout}ms`);
  }

  /**
   * Extract verification token from email content
   */
  private extractVerificationToken(emailBody: string): string {
    // Look for verification token in various formats
    const patterns = [
      /verify-email\?token=([a-zA-Z0-9-_]+)/,
      /verification[_-]?token[=:]?\s*([a-zA-Z0-9-_]+)/i,
      /token[=:]?\s*([a-zA-Z0-9-_]{20,})/i,
    ];
    
    for (const pattern of patterns) {
      const match = emailBody.match(pattern);
      if (match && match[1]) {
        return match[1];
      }
    }
    
    throw new Error('Verification token not found in email content');
  }

  /**
   * Extract password reset token from email content
   */
  private extractPasswordResetToken(emailBody: string): string {
    // Look for reset token in various formats
    const patterns = [
      /reset-password\?token=([a-zA-Z0-9-_]+)/,
      /reset[_-]?token[=:]?\s*([a-zA-Z0-9-_]+)/i,
      /token[=:]?\s*([a-zA-Z0-9-_]{20,})/i,
    ];
    
    for (const pattern of patterns) {
      const match = emailBody.match(pattern);
      if (match && match[1]) {
        return match[1];
      }
    }
    
    throw new Error('Password reset token not found in email content');
  }

  /**
   * Clear all emails from MailHog (test cleanup)
   */
  async clearAllEmails(): Promise<void> {
    try {
      await fetch(`${MAILHOG_CONFIG.API_BASE_URL}${MAILHOG_CONFIG.DELETE_ENDPOINT}`, {
        method: 'DELETE',
      });
    } catch (error) {
      console.warn('Failed to clear emails from MailHog:', error);
    }
  }

  /**
   * Setup API request monitoring
   */
  async setupApiMonitoring(): Promise<{
    getLoginRequests: () => ApiRequestInfo[];
    getRegisterRequests: () => ApiRequestInfo[];
    getPasswordResetRequests: () => ApiRequestInfo[];
    clearRequests: () => void;
  }> {
    const loginRequests: ApiRequestInfo[] = [];
    const registerRequests: ApiRequestInfo[] = [];
    const passwordResetRequests: ApiRequestInfo[] = [];

    await this.page.route('**/api/v1/auth/login', route => {
      loginRequests.push({
        url: route.request().url(),
        method: route.request().method(),
        postData: route.request().postDataJSON() as Record<string, unknown> | null,
        timestamp: Date.now(),
      });
      route.continue();
    });

    await this.page.route('**/api/v1/auth/register', route => {
      registerRequests.push({
        url: route.request().url(),
        method: route.request().method(),
        postData: route.request().postDataJSON() as Record<string, unknown> | null,
        timestamp: Date.now(),
      });
      route.continue();
    });

    await this.page.route('**/api/v1/auth/forgot-password', route => {
      passwordResetRequests.push({
        url: route.request().url(),
        method: route.request().method(),
        postData: route.request().postDataJSON() as Record<string, unknown> | null,
        timestamp: Date.now(),
      });
      route.continue();
    });

    return {
      getLoginRequests: () => [...loginRequests],
      getRegisterRequests: () => [...registerRequests],
      getPasswordResetRequests: () => [...passwordResetRequests],
      clearRequests: () => {
        loginRequests.length = 0;
        registerRequests.length = 0;
        passwordResetRequests.length = 0;
      },
    };
  }
}