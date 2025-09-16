/**
 * Authentication Contract Tests
 * Following TDD principles - Writing tests FIRST before implementation
 */

import { describe, it, expect, assertType, expectTypeOf } from 'vitest';
import type {
  User,
  AuthTokens,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  Persona,
  PersonaType,
  RefreshTokenRequest,
  RefreshTokenResponse,
  AuthError,
  AuthErrorCode,
  TokenValidation,
  PasswordResetRequest,
  PasswordResetConfirmation,
  EmailVerificationRequest,
  UserProfile,
  UserPreferences,
  AccountStatus
} from '../auth';

describe('Authentication Contracts', () => {
  describe('User Interface', () => {
    it('should have required user properties', () => {
      const user: User = {
        id: 'test-user-id',
        email: 'user@example.com',
        firstName: 'John',
        lastName: 'Doe',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        emailVerified: false,
        accountStatus: 'active' as AccountStatus
      };

      expect(user.id).toBeDefined();
      expect(user.email).toBeDefined();
      expect(user.firstName).toBeDefined();
      expect(user.lastName).toBeDefined();
      expectTypeOf(user.id).toEqualTypeOf<string>();
      expectTypeOf(user.email).toEqualTypeOf<string>();
      expectTypeOf(user.firstName).toEqualTypeOf<string>();
      expectTypeOf(user.lastName).toEqualTypeOf<string>();
    });

    it('should support optional persona', () => {
      const userWithPersona: User = {
        id: 'test-user-id',
        email: 'user@example.com',
        firstName: 'John',
        lastName: 'Doe',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        emailVerified: true,
        accountStatus: 'active' as AccountStatus,
        persona: {
          id: 'persona-id',
          type: 'work' as PersonaType,
          name: 'Professional',
          isActive: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      };

      expect(userWithPersona.persona).toBeDefined();
      expectTypeOf(userWithPersona.persona).toMatchTypeOf<Persona | undefined>();
    });

    it('should support optional profile', () => {
      const userWithProfile: User = {
        id: 'test-user-id',
        email: 'user@example.com',
        firstName: 'John',
        lastName: 'Doe',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        emailVerified: true,
        accountStatus: 'active' as AccountStatus,
        profile: {
          bio: 'Software Developer',
          avatarUrl: 'https://example.com/avatar.jpg',
          timezone: 'America/New_York',
          language: 'en',
          phoneNumber: '+1234567890',
          location: 'New York, USA'
        }
      };

      expect(userWithProfile.profile).toBeDefined();
      expectTypeOf(userWithProfile.profile).toMatchTypeOf<UserProfile | undefined>();
    });
  });

  describe('AuthTokens Interface', () => {
    it('should have required token properties', () => {
      const tokens: AuthTokens = {
        accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9',
        refreshToken: 'refresh-token-string',
        expiresIn: 3600000, // 1 hour in ms
        tokenType: 'Bearer',
        scope: 'read write'
      };

      expect(tokens.accessToken).toBeDefined();
      expect(tokens.refreshToken).toBeDefined();
      expect(tokens.expiresIn).toBeGreaterThan(0);
      expectTypeOf(tokens.accessToken).toEqualTypeOf<string>();
      expectTypeOf(tokens.refreshToken).toEqualTypeOf<string>();
      expectTypeOf(tokens.expiresIn).toEqualTypeOf<number>();
      expectTypeOf(tokens.tokenType).toEqualTypeOf<string>();
    });

    it('should calculate expiry time correctly', () => {
      const now = Date.now();
      const tokens: AuthTokens = {
        accessToken: 'token',
        refreshToken: 'refresh',
        expiresIn: 3600000,
        tokenType: 'Bearer',
        scope: 'read write',
        issuedAt: now
      };

      if (tokens.issuedAt) {
        const expiryTime = tokens.issuedAt + tokens.expiresIn;
        expect(expiryTime).toBeGreaterThan(now);
      }
    });
  });

  describe('Login Request Interface', () => {
    it('should have required login properties', () => {
      const loginRequest: LoginRequest = {
        email: 'user@example.com',
        password: 'SecurePassword123!',
        rememberMe: false
      };

      expect(loginRequest.email).toBeDefined();
      expect(loginRequest.password).toBeDefined();
      expectTypeOf(loginRequest.email).toEqualTypeOf<string>();
      expectTypeOf(loginRequest.password).toEqualTypeOf<string>();
      expectTypeOf(loginRequest.rememberMe).toMatchTypeOf<boolean | undefined>();
    });

    it('should support persona selection on login', () => {
      const loginWithPersona: LoginRequest = {
        email: 'user@example.com',
        password: 'SecurePassword123!',
        personaId: 'work-persona-id',
        rememberMe: true
      };

      expect(loginWithPersona.personaId).toBeDefined();
      expectTypeOf(loginWithPersona.personaId).toMatchTypeOf<string | undefined>();
    });
  });

  describe('Register Request Interface', () => {
    it('should have required registration properties', () => {
      const registerRequest: RegisterRequest = {
        email: 'newuser@example.com',
        password: 'SecurePassword123!',
        confirmPassword: 'SecurePassword123!',
        firstName: 'Jane',
        lastName: 'Smith',
        acceptTerms: true
      };

      expect(registerRequest.email).toBeDefined();
      expect(registerRequest.password).toBeDefined();
      expect(registerRequest.confirmPassword).toBeDefined();
      expect(registerRequest.firstName).toBeDefined();
      expect(registerRequest.lastName).toBeDefined();
      expect(registerRequest.acceptTerms).toBe(true);
      expectTypeOf(registerRequest.acceptTerms).toEqualTypeOf<boolean>();
    });

    it('should support optional registration fields', () => {
      const registerWithOptionals: RegisterRequest = {
        email: 'newuser@example.com',
        password: 'SecurePassword123!',
        confirmPassword: 'SecurePassword123!',
        firstName: 'Jane',
        lastName: 'Smith',
        acceptTerms: true,
        phoneNumber: '+1234567890',
        timezone: 'America/New_York',
        language: 'en',
        marketingConsent: true
      };

      expect(registerWithOptionals.phoneNumber).toBeDefined();
      expectTypeOf(registerWithOptionals.phoneNumber).toMatchTypeOf<string | undefined>();
      expectTypeOf(registerWithOptionals.marketingConsent).toMatchTypeOf<boolean | undefined>();
    });
  });

  describe('AuthResponse Interface', () => {
    it('should combine user and tokens', () => {
      const authResponse: AuthResponse = {
        user: {
          id: 'user-id',
          email: 'user@example.com',
          firstName: 'John',
          lastName: 'Doe',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          emailVerified: true,
          accountStatus: 'active' as AccountStatus
        },
        tokens: {
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
          expiresIn: 3600000,
          tokenType: 'Bearer',
          scope: 'read write'
        },
        sessionId: 'session-123',
        deviceId: 'device-456'
      };

      expect(authResponse.user).toBeDefined();
      expect(authResponse.tokens).toBeDefined();
      expectTypeOf(authResponse.user).toMatchTypeOf<User>();
      expectTypeOf(authResponse.tokens).toMatchTypeOf<AuthTokens>();
      expectTypeOf(authResponse.sessionId).toMatchTypeOf<string | undefined>();
    });
  });

  describe('Persona Types', () => {
    it('should have valid persona types', () => {
      const personaTypes: PersonaType[] = ['work', 'study', 'personal', 'custom'];

      personaTypes.forEach(type => {
        expectTypeOf(type).toMatchTypeOf<PersonaType>();
      });
    });

    it('should support persona interface', () => {
      const persona: Persona = {
        id: 'persona-id',
        type: 'work' as PersonaType,
        name: 'Professional',
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        settings: {
          theme: 'dark',
          notifications: true,
          privacy: 'public'
        }
      };

      expect(persona.id).toBeDefined();
      expect(persona.type).toBe('work');
      expect(persona.isActive).toBe(true);
      expectTypeOf(persona.type).toMatchTypeOf<PersonaType>();
    });
  });

  describe('Token Refresh', () => {
    it('should support refresh token request', () => {
      const refreshRequest: RefreshTokenRequest = {
        refreshToken: 'valid-refresh-token',
        grantType: 'refresh_token'
      };

      expect(refreshRequest.refreshToken).toBeDefined();
      expect(refreshRequest.grantType).toBe('refresh_token');
    });

    it('should support refresh token response', () => {
      const refreshResponse: RefreshTokenResponse = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600000,
        tokenType: 'Bearer',
        scope: 'read write'
      };

      expectTypeOf(refreshResponse).toMatchTypeOf<AuthTokens>();
    });
  });

  describe('Auth Error Handling', () => {
    it('should support auth error structure', () => {
      const authError: AuthError = {
        code: 'INVALID_CREDENTIALS' as AuthErrorCode,
        message: 'Invalid email or password',
        timestamp: new Date().toISOString(),
        path: '/api/v1/auth/login',
        details: {
          attempts: 3,
          lockoutTime: 300000
        }
      };

      expect(authError.code).toBeDefined();
      expect(authError.message).toBeDefined();
      expectTypeOf(authError.code).toMatchTypeOf<AuthErrorCode>();
      expectTypeOf(authError.details).toMatchTypeOf<Record<string, any> | undefined>();
    });

    it('should support all auth error codes', () => {
      const errorCodes: AuthErrorCode[] = [
        'INVALID_CREDENTIALS',
        'TOKEN_EXPIRED',
        'TOKEN_INVALID',
        'REFRESH_TOKEN_EXPIRED',
        'ACCOUNT_LOCKED',
        'ACCOUNT_DISABLED',
        'EMAIL_NOT_VERIFIED',
        'INVALID_PERSONA',
        'SESSION_EXPIRED',
        'UNAUTHORIZED',
        'FORBIDDEN',
        'RATE_LIMIT_EXCEEDED'
      ];

      errorCodes.forEach(code => {
        expectTypeOf(code).toMatchTypeOf<AuthErrorCode>();
      });
    });
  });

  describe('Token Validation', () => {
    it('should support token validation result', () => {
      const validation: TokenValidation = {
        valid: true,
        expired: false,
        userId: 'user-id',
        email: 'user@example.com',
        scope: ['read', 'write'],
        issuedAt: Date.now() - 1800000, // 30 minutes ago
        expiresAt: Date.now() + 1800000  // 30 minutes from now
      };

      expect(validation.valid).toBe(true);
      expect(validation.expired).toBe(false);
      expect(validation.userId).toBeDefined();
      expectTypeOf(validation.scope).toMatchTypeOf<string[] | undefined>();
    });
  });

  describe('Password Reset', () => {
    it('should support password reset request', () => {
      const resetRequest: PasswordResetRequest = {
        email: 'user@example.com',
        redirectUrl: 'https://app.focushive.com/reset-password'
      };

      expect(resetRequest.email).toBeDefined();
      expectTypeOf(resetRequest.redirectUrl).toMatchTypeOf<string | undefined>();
    });

    it('should support password reset confirmation', () => {
      const resetConfirm: PasswordResetConfirmation = {
        token: 'reset-token',
        newPassword: 'NewSecurePassword123!',
        confirmPassword: 'NewSecurePassword123!'
      };

      expect(resetConfirm.token).toBeDefined();
      expect(resetConfirm.newPassword).toBe(resetConfirm.confirmPassword);
    });
  });

  describe('Email Verification', () => {
    it('should support email verification request', () => {
      const verifyRequest: EmailVerificationRequest = {
        token: 'verification-token',
        email: 'user@example.com'
      };

      expect(verifyRequest.token).toBeDefined();
      expectTypeOf(verifyRequest.email).toMatchTypeOf<string | undefined>();
    });
  });

  describe('User Preferences', () => {
    it('should support user preferences', () => {
      const preferences: UserPreferences = {
        theme: 'dark',
        language: 'en',
        timezone: 'America/New_York',
        notifications: {
          email: true,
          push: false,
          sms: false,
          inApp: true
        },
        privacy: {
          profileVisibility: 'public',
          showOnlineStatus: true,
          allowMessages: true
        },
        accessibility: {
          highContrast: false,
          fontSize: 'medium',
          reducedMotion: false
        }
      };

      expect(preferences.theme).toBe('dark');
      expect(preferences.notifications?.email).toBe(true);
      expect(preferences.privacy).toBeDefined();
      expectTypeOf(preferences.privacy).toMatchTypeOf<UserPreferences['privacy']>();
    });
  });

  describe('Account Status', () => {
    it('should support all account status types', () => {
      const statuses: AccountStatus[] = ['active', 'inactive', 'suspended', 'pending', 'deleted'];

      statuses.forEach(status => {
        expectTypeOf(status).toMatchTypeOf<AccountStatus>();
      });
    });
  });
});