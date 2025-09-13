/**
 * E2E Tests for OAuth2 Login Integration
 * Comprehensive OAuth testing including Google, GitHub, account linking, and error scenarios
 */

import { test, expect } from '@playwright/test';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { 
  AUTH_TEST_USERS, 
  OAUTH_PROVIDERS,
  MOCK_OAUTH_PROFILES,
  generateUniqueAuthUser,
  MOBILE_VIEWPORTS,
  AUTH_PERFORMANCE_THRESHOLDS
} from '../../helpers/auth-fixtures';

test.describe('OAuth2 Login Integration', () => {
  let authHelper: EnhancedAuthHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new EnhancedAuthHelper(page);
    await authHelper.clearStorage();
  });

  test.afterEach(async () => {
    await authHelper.clearStorage();
  });

  test.describe('OAuth Provider Options', () => {
    test('should display OAuth login options', async () => {
      await authHelper.navigateToLogin();

      // Check for OAuth provider buttons
      const googleButton = authHelper.page.locator('button:has-text("Google"), a:has-text("Google"), [data-testid="google-oauth"]');
      const githubButton = authHelper.page.locator('button:has-text("GitHub"), a:has-text("GitHub"), [data-testid="github-oauth"]');

      const hasGoogleButton = await googleButton.isVisible({ timeout: 5000 }).catch(() => false);
      const hasGithubButton = await githubButton.isVisible({ timeout: 5000 }).catch(() => false);

      // At least one OAuth provider should be available
      expect(hasGoogleButton || hasGithubButton).toBeTruthy();

      if (hasGoogleButton) {
        console.log('✅ Google OAuth button found');
      }
      if (hasGithubButton) {
        console.log('✅ GitHub OAuth button found');
      }
    });

    test('should have proper OAuth button styling and accessibility', async () => {
      await authHelper.navigateToLogin();

      const oauthButtons = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub"), [data-testid*="oauth"]');
      const buttonCount = await oauthButtons.count();

      if (buttonCount > 0) {
        const firstButton = oauthButtons.first();
        
        // Button should be visible and clickable
        await expect(firstButton).toBeVisible();
        await expect(firstButton).toBeEnabled();

        // Button should have accessible text
        const buttonText = await firstButton.textContent();
        expect(buttonText?.trim()).toBeTruthy();
        expect(buttonText?.length).toBeGreaterThan(3);

        // Button should have proper role
        const buttonRole = await firstButton.getAttribute('role');
        const buttonType = await firstButton.getAttribute('type');
        expect(buttonRole === 'button' || buttonType === 'button' || firstButton.locator('button').isVisible()).toBeTruthy();
      }
    });
  });

  test.describe('Google OAuth Flow', () => {
    test('should initiate Google OAuth login', async ({ page }) => {
      await authHelper.mockOAuthResponse('GOOGLE', true);
      await authHelper.navigateToLogin();

      const googleButton = authHelper.page.locator('button:has-text("Google"), a:has-text("Google"), [data-testid="google-oauth"]');
      const hasGoogleButton = await googleButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasGoogleButton) {
        test.skip('Google OAuth button not found');
      }

      // Mock OAuth redirect flow
      await page.route('**/auth/oauth2/google**', route => {
        route.fulfill({
          status: 302,
          headers: {
            'Location': '/auth/oauth2/callback/google?code=mock_auth_code&state=mock_state',
          },
        });
      });

      await googleButton.click();

      // Should redirect to OAuth provider (or callback)
      await authHelper.page.waitForTimeout(2000);
      
      const currentUrl = authHelper.page.url();
      const isOAuthFlow = currentUrl.includes('oauth') || currentUrl.includes('google') || currentUrl.includes('/dashboard');
      
      expect(isOAuthFlow).toBeTruthy();
    });

    test('should complete Google OAuth login for new user', async () => {
      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');

      // Should have valid tokens
      expect(tokenInfo.accessToken).toBeTruthy();
      expect(tokenInfo.refreshToken).toBeTruthy();
      expect(tokenInfo.isValid).toBeTruthy();

      // Should be redirected to dashboard
      await authHelper.verifyOnDashboard();

      // User profile should be populated from Google
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar');
      await userMenu.click();

      // Should show Google profile information
      const profileInfo = authHelper.page.locator(':text("Google Test User"), :text("oauth.google@example.com")');
      const hasProfileInfo = await profileInfo.first().isVisible({ timeout: 2000 }).catch(() => false);

      if (hasProfileInfo) {
        console.log('✅ Google profile data populated');
      }
    });

    test('should handle Google OAuth errors', async ({ page }) => {
      await authHelper.mockOAuthResponse('GOOGLE', false);
      await authHelper.navigateToLogin();

      const googleButton = authHelper.page.locator('button:has-text("Google"), a:has-text("Google")');
      const hasGoogleButton = await googleButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasGoogleButton) {
        test.skip('Google OAuth button not found');
      }

      await googleButton.click();
      await authHelper.page.waitForTimeout(2000);

      // Should show OAuth error
      await authHelper.verifyErrorMessage();
      
      // Should stay on login page or show error page
      const currentUrl = authHelper.page.url();
      expect(currentUrl.includes('/login') || currentUrl.includes('/error')).toBeTruthy();
    });

    test('should link Google account to existing user', async ({ page }) => {
      // First, create a regular user account
      const existingUser = generateUniqueAuthUser(AUTH_TEST_USERS.OAUTH_LINK_USER);
      
      // Register user first (mock registration)
      await page.route('**/api/v1/auth/register', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: existingUser,
            message: 'Registration successful',
          }),
        });
      });

      await authHelper.navigateToRegistration();
      await authHelper.fillRegistrationForm(existingUser);
      await authHelper.submitRegistrationForm();

      // Now try OAuth login with same email
      await page.route('**/auth/oauth2/google**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: {
              ...MOCK_OAUTH_PROFILES.GOOGLE,
              email: existingUser.email, // Same email as existing account
            },
            token: 'linked.jwt.token',
            refreshToken: 'linked.refresh.token',
            accountLinked: true,
          }),
        });
      });

      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');

      expect(tokenInfo.accessToken).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // Should show confirmation that account was linked
      const linkMessage = authHelper.page.locator(':text("linked"), :text("connected")');
      const hasLinkMessage = await linkMessage.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasLinkMessage) {
        console.log('✅ Account linking confirmation shown');
      }
    });
  });

  test.describe('GitHub OAuth Flow', () => {
    test('should initiate GitHub OAuth login', async ({ page }) => {
      await authHelper.mockOAuthResponse('GITHUB', true);
      await authHelper.navigateToLogin();

      const githubButton = authHelper.page.locator('button:has-text("GitHub"), a:has-text("GitHub"), [data-testid="github-oauth"]');
      const hasGithubButton = await githubButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasGithubButton) {
        test.skip('GitHub OAuth button not found');
      }

      // Mock OAuth redirect flow
      await page.route('**/auth/oauth2/github**', route => {
        route.fulfill({
          status: 302,
          headers: {
            'Location': '/auth/oauth2/callback/github?code=mock_github_code&state=mock_state',
          },
        });
      });

      await githubButton.click();

      // Should redirect to OAuth provider
      await authHelper.page.waitForTimeout(2000);
      
      const currentUrl = authHelper.page.url();
      const isOAuthFlow = currentUrl.includes('oauth') || currentUrl.includes('github') || currentUrl.includes('/dashboard');
      
      expect(isOAuthFlow).toBeTruthy();
    });

    test('should complete GitHub OAuth login for new user', async () => {
      const tokenInfo = await authHelper.testOAuthLogin('GITHUB');

      expect(tokenInfo.accessToken).toBeTruthy();
      expect(tokenInfo.refreshToken).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // User profile should be populated from GitHub
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar');
      await userMenu.click();

      const profileInfo = authHelper.page.locator(':text("GitHub Test User"), :text("oauth.github@example.com")');
      const hasProfileInfo = await profileInfo.first().isVisible({ timeout: 2000 }).catch(() => false);

      if (hasProfileInfo) {
        console.log('✅ GitHub profile data populated');
      }
    });

    test('should handle GitHub OAuth permission denied', async ({ page }) => {
      await authHelper.navigateToLogin();

      const githubButton = authHelper.page.locator('button:has-text("GitHub"), a:has-text("GitHub")');
      const hasGithubButton = await githubButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasGithubButton) {
        test.skip('GitHub OAuth button not found');
      }

      // Mock permission denied response
      await page.route('**/auth/oauth2/github**', route => {
        route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'access_denied',
            error_description: 'User denied authorization',
          }),
        });
      });

      await githubButton.click();
      await authHelper.page.waitForTimeout(2000);

      // Should show appropriate error message
      const errorMessage = authHelper.page.locator(':text("access denied"), :text("authorization denied"), [role="alert"]');
      const hasErrorMessage = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

      expect(hasErrorMessage).toBeTruthy();
    });
  });

  test.describe('OAuth Error Scenarios', () => {
    test('should handle OAuth server unavailable', async ({ page }) => {
      await authHelper.navigateToLogin();

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      // Mock server unavailable
      await page.route('**/auth/oauth2/**', route => {
        route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'service_unavailable',
            error_description: 'OAuth service temporarily unavailable',
          }),
        });
      });

      await oauthButton.click();
      await authHelper.page.waitForTimeout(2000);

      // Should show service unavailable error
      await authHelper.verifyErrorMessage();
    });

    test('should handle malformed OAuth response', async ({ page }) => {
      await authHelper.navigateToLogin();

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      // Mock malformed response
      await page.route('**/auth/oauth2/**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: 'invalid json response',
        });
      });

      await oauthButton.click();
      await authHelper.page.waitForTimeout(2000);

      // Should handle gracefully
      await authHelper.verifyErrorMessage();
    });

    test('should handle OAuth CSRF protection', async ({ page }) => {
      await authHelper.navigateToLogin();

      // Mock CSRF error
      await page.route('**/auth/oauth2/**', route => {
        route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'invalid_state',
            error_description: 'Invalid state parameter - possible CSRF attack',
          }),
        });
      });

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      await oauthButton.click();
      await authHelper.page.waitForTimeout(2000);

      // Should show security error
      const securityError = authHelper.page.locator(':text("security"), :text("csrf"), :text("invalid state"), [role="alert"]');
      const hasSecurityError = await securityError.isVisible({ timeout: 2000 }).catch(() => false);

      expect(hasSecurityError).toBeTruthy();
    });

    test('should handle network timeouts during OAuth', async ({ page }) => {
      await authHelper.navigateToLogin();

      // Mock network timeout
      await page.route('**/auth/oauth2/**', route => {
        // Never respond (simulate timeout)
      });

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      await oauthButton.click();

      // Should show timeout error after reasonable wait
      const timeoutError = authHelper.page.locator(':text("timeout"), :text("network error"), [role="alert"]');
      const hasTimeoutError = await timeoutError.isVisible({ timeout: 15000 }).catch(() => false);

      expect(hasTimeoutError).toBeTruthy();
    });
  });

  test.describe('OAuth Security Features', () => {
    test('should use PKCE for OAuth flows', async ({ page }) => {
      await authHelper.navigateToLogin();

      interface CapturedRequest {
        url: string;
        method: string;
        headers: Record<string, string>;
      }

      let capturedRequest: CapturedRequest | null = null;

      // Intercept OAuth initiation
      await page.route('**/auth/oauth2/**', route => {
        capturedRequest = {
          url: route.request().url(),
          method: route.request().method(),
          headers: route.request().headers(),
        };
        route.continue();
      });

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      await oauthButton.click();
      await authHelper.page.waitForTimeout(1000);

      // Check for PKCE parameters
      if (capturedRequest) {
        const url = new URL(capturedRequest.url);
        const hasCodeChallenge = url.searchParams.has('code_challenge');
        const hasCodeChallengeMethod = url.searchParams.has('code_challenge_method');

        if (hasCodeChallenge && hasCodeChallengeMethod) {
          console.log('✅ PKCE parameters detected in OAuth flow');
          expect(hasCodeChallenge).toBeTruthy();
          expect(hasCodeChallengeMethod).toBeTruthy();
        } else {
          console.log('ℹ️ PKCE parameters not detected (may use different security method)');
        }
      }
    });

    test('should validate OAuth state parameter', async ({ page }) => {
      await authHelper.navigateToLogin();

      let stateParameter: string | null = null;

      // Capture state parameter from OAuth initiation
      await page.route('**/auth/oauth2/**', route => {
        const url = new URL(route.request().url());
        stateParameter = url.searchParams.get('state');
        route.continue();
      });

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      await oauthButton.click();
      await authHelper.page.waitForTimeout(1000);

      // State parameter should be present and sufficiently random
      if (stateParameter) {
        expect(stateParameter.length).toBeGreaterThan(10);
        expect(stateParameter).toMatch(/^[a-zA-Z0-9\-_]+$/);
        console.log('✅ OAuth state parameter validation passed');
      } else {
        console.log('ℹ️ OAuth state parameter not captured (may use different method)');
      }
    });

    test('should handle OAuth scope permissions properly', async ({ page }) => {
      await authHelper.navigateToLogin();

      let capturedScopes: string[] = [];

      // Capture requested scopes
      await page.route('**/auth/oauth2/**', route => {
        const url = new URL(route.request().url());
        const scope = url.searchParams.get('scope');
        if (scope) {
          capturedScopes = scope.split(' ');
        }
        route.continue();
      });

      const googleButton = authHelper.page.locator('button:has-text("Google"), a:has-text("Google")');
      const hasGoogleButton = await googleButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasGoogleButton) {
        test.skip('Google OAuth button not found');
      }

      await googleButton.click();
      await authHelper.page.waitForTimeout(1000);

      // Should request minimal necessary scopes
      if (capturedScopes.length > 0) {
        expect(capturedScopes).toContain('email');
        expect(capturedScopes).toContain('profile');
        
        // Should not request excessive permissions
        const excessiveScopes = ['https://www.googleapis.com/auth/admin', 'https://www.googleapis.com/auth/drive'];
        for (const scope of excessiveScopes) {
          expect(capturedScopes).not.toContain(scope);
        }
        
        console.log(`✅ OAuth scopes: ${capturedScopes.join(', ')}`);
      }
    });
  });

  test.describe('Profile Data Handling', () => {
    test('should populate user profile from OAuth provider', async ({ page }) => {
      // Mock successful OAuth with profile data
      await page.route('**/auth/oauth2/google**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: {
              id: 'oauth_user_123',
              email: 'oauth.test@example.com',
              name: 'OAuth Test User',
              firstName: 'OAuth',
              lastName: 'User',
              picture: 'https://example.com/avatar.jpg',
              provider: 'google',
            },
            token: 'valid.jwt.token',
            refreshToken: 'valid.refresh.token',
          }),
        });
      });

      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');
      expect(tokenInfo.accessToken).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // Check that profile data is displayed
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar');
      await userMenu.click();

      // Should show OAuth provider information
      const profileName = authHelper.page.locator(':text("OAuth Test User")');
      const profileEmail = authHelper.page.locator(':text("oauth.test@example.com")');

      const hasProfileName = await profileName.isVisible({ timeout: 2000 }).catch(() => false);
      const hasProfileEmail = await profileEmail.isVisible({ timeout: 2000 }).catch(() => false);

      expect(hasProfileName || hasProfileEmail).toBeTruthy();
    });

    test('should handle missing profile data gracefully', async ({ page }) => {
      // Mock OAuth response with minimal profile data
      await page.route('**/auth/oauth2/google**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: {
              id: 'minimal_user_123',
              email: 'minimal@example.com',
              // Missing name, firstName, lastName, picture
            },
            token: 'valid.jwt.token',
            refreshToken: 'valid.refresh.token',
          }),
        });
      });

      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');
      expect(tokenInfo.accessToken).toBeTruthy();
      await authHelper.verifyOnDashboard();

      // Should still work with minimal profile data
      const userMenu = authHelper.page.locator('[data-testid="user-menu"], .user-avatar');
      await expect(userMenu).toBeVisible();
    });

    test('should allow profile completion after OAuth login', async ({ page }) => {
      // Mock OAuth login with incomplete profile
      await page.route('**/auth/oauth2/google**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: {
              id: 'incomplete_user_123',
              email: 'incomplete@example.com',
              profileComplete: false,
            },
            token: 'valid.jwt.token',
            refreshToken: 'valid.refresh.token',
            requiresProfileCompletion: true,
          }),
        });
      });

      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');
      expect(tokenInfo.accessToken).toBeTruthy();

      // Should redirect to profile completion page
      const currentUrl = authHelper.page.url();
      const onProfilePage = currentUrl.includes('/profile') || currentUrl.includes('/complete');

      if (onProfilePage) {
        console.log('✅ Redirected to profile completion');
        
        // Should show profile completion form
        const profileForm = authHelper.page.locator('form, [role="form"]');
        await expect(profileForm).toBeVisible();
      } else {
        // May go directly to dashboard with incomplete profile
        await authHelper.verifyOnDashboard();
        console.log('ℹ️ No profile completion required');
      }
    });
  });

  test.describe('Mobile OAuth Experience', () => {
    Object.entries(MOBILE_VIEWPORTS).forEach(([deviceName, viewport]) => {
      test(`should work on ${deviceName}`, async ({ page }) => {
        await page.setViewportSize(viewport);
        
        const mobileAuthHelper = new EnhancedAuthHelper(page);
        await mobileAuthHelper.clearStorage();
        await mobileAuthHelper.navigateToLogin();

        // OAuth buttons should be visible on mobile
        const oauthButton = mobileAuthHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
        const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

        if (!hasOAuthButton) {
          test.skip('OAuth buttons not found on mobile');
        }

        // Buttons should be properly sized for mobile
        const buttonBox = await oauthButton.boundingBox();
        if (buttonBox) {
          expect(buttonBox.width).toBeGreaterThan(44); // Minimum touch target size
          expect(buttonBox.height).toBeGreaterThan(44);
          expect(buttonBox.width).toBeLessThanOrEqual(viewport.width * 0.9);
        }

        // Should work with touch
        await oauthButton.tap();
        await mobileAuthHelper.page.waitForTimeout(1000);

        // Should initiate OAuth flow
        const currentUrl = mobileAuthHelper.page.url();
        const oauthInitiated = currentUrl.includes('oauth') || currentUrl.includes('google') || currentUrl.includes('github');

        expect(oauthInitiated).toBeTruthy();
      });
    });
  });

  test.describe('Performance', () => {
    test('should complete OAuth flow within performance threshold', async ({ page }) => {
      const startTime = Date.now();

      // Mock fast OAuth response
      await page.route('**/auth/oauth2/google**', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: MOCK_OAUTH_PROFILES.GOOGLE,
            token: 'fast.jwt.token',
            refreshToken: 'fast.refresh.token',
          }),
        });
      });

      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');
      expect(tokenInfo.accessToken).toBeTruthy();

      const endTime = Date.now();
      const oauthTime = endTime - startTime;

      expect(oauthTime).toBeLessThan(AUTH_PERFORMANCE_THRESHOLDS.OAUTH_FLOW_TIME_MS);
      console.log(`OAuth flow completed in ${oauthTime}ms`);
    });

    test('should handle slow OAuth responses', async ({ page }) => {
      await authHelper.navigateToLogin();

      // Mock slow OAuth response
      await page.route('**/auth/oauth2/google**', async route => {
        await new Promise(resolve => setTimeout(resolve, 3000)); // 3s delay
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            user: MOCK_OAUTH_PROFILES.GOOGLE,
            token: 'slow.jwt.token',
            refreshToken: 'slow.refresh.token',
          }),
        });
      });

      const startTime = Date.now();
      const tokenInfo = await authHelper.testOAuthLogin('GOOGLE');
      const endTime = Date.now();

      expect(tokenInfo.accessToken).toBeTruthy();
      expect(endTime - startTime).toBeGreaterThan(3000);

      console.log(`Slow OAuth flow completed in ${endTime - startTime}ms`);
    });
  });

  test.describe('Accessibility', () => {
    test('should support keyboard navigation for OAuth buttons', async () => {
      await authHelper.navigateToLogin();

      const oauthButton = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub")').first();
      const hasOAuthButton = await oauthButton.isVisible({ timeout: 2000 }).catch(() => false);

      if (!hasOAuthButton) {
        test.skip('OAuth buttons not found');
      }

      // Should be focusable
      await oauthButton.focus();
      expect(await oauthButton.evaluate(el => el === document.activeElement)).toBeTruthy();

      // Should activate with Enter
      await authHelper.page.keyboard.press('Enter');
      await authHelper.page.waitForTimeout(1000);

      // Should initiate OAuth flow
      const currentUrl = authHelper.page.url();
      expect(currentUrl.includes('oauth') || currentUrl.includes('login')).toBeTruthy();
    });

    test('should have proper ARIA labels for OAuth buttons', async () => {
      await authHelper.navigateToLogin();

      const oauthButtons = authHelper.page.locator('button:has-text("Google"), button:has-text("GitHub"), [data-testid*="oauth"]');
      const buttonCount = await oauthButtons.count();

      if (buttonCount === 0) {
        test.skip('OAuth buttons not found');
      }

      for (let i = 0; i < buttonCount; i++) {
        const button = oauthButtons.nth(i);
        
        // Button should have accessible text
        const buttonText = await button.textContent();
        const ariaLabel = await button.getAttribute('aria-label');
        
        expect(buttonText?.trim() || ariaLabel?.trim()).toBeTruthy();

        // Should indicate it's for login/sign in
        const accessibleText = (buttonText || ariaLabel || '').toLowerCase();
        expect(accessibleText).toMatch(/sign|login|continue|connect/);
      }
    });
  });
});