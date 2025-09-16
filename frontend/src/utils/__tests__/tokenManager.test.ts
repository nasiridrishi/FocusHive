/**
 * Comprehensive tests for tokenManager utility
 *
 * Tests JWT token storage, validation, parsing, and security features
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {tokenManager} from '../tokenManager';

// Mock storage APIs
const mockSessionStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};

const mockLocalStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};

// Mock window.dispatchEvent
const mockDispatchEvent = vi.fn();

// Helper function to create valid JWT tokens for testing
const createJWT = (payload: Record<string, unknown>): string => {
  const header = {alg: 'HS256', typ: 'JWT'};
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify(payload));
  const signature = 'mock-signature';
  return `${encodedHeader}.${encodedPayload}.${signature}`;
};

// Sample JWT tokens for testing
const sampleTokens = {
  // Valid token (expires in future)
  validAccessToken: createJWT({
    sub: '1234567890',
    email: 'test@example.com',
    username: 'testuser',
    exp: 9999999999, // Far future
    iat: 1616239022
  }),

  // Expired token
  expiredAccessToken: createJWT({
    sub: '1234567890',
    email: 'test@example.com',
    username: 'testuser',
    exp: 1616239022, // Past date
    iat: 1616239022
  }),

  // Invalid token (malformed)
  invalidToken: 'invalid.token.here',

  // Valid refresh token
  validRefreshToken: createJWT({
    sub: '1234567890',
    typ: 'refresh',
    exp: 9999999999, // Far future
    iat: 1616239022
  })
};

describe('TokenManager', () => {
  beforeEach(() => {
    // Reset all mocks
    vi.clearAllMocks();

    // Mock the storage APIs
    Object.defineProperty(window, 'sessionStorage', {
      value: mockSessionStorage,
      writable: true,
    });

    Object.defineProperty(window, 'localStorage', {
      value: mockLocalStorage,
      writable: true,
    });

    Object.defineProperty(window, 'dispatchEvent', {
      value: mockDispatchEvent,
      writable: true,
    });

    // Mock Date.now for consistent testing
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-01T00:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllTimers();
  });

  describe('Token Storage', () => {
    it('should save tokens securely', () => {
      tokenManager.saveTokens(sampleTokens.validAccessToken, sampleTokens.validRefreshToken);

      expect(mockSessionStorage.setItem).toHaveBeenCalledWith(
          'focushive_access_token',
          sampleTokens.validAccessToken
      );
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
          'focushive_refresh_token',
          sampleTokens.validRefreshToken
      );
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
          'focushive_token_expires',
          expect.any(String)
      );
    });

    it('should throw error when saving invalid access token', () => {
      expect(() => {
        tokenManager.saveTokens(sampleTokens.invalidToken, sampleTokens.validRefreshToken);
      }).toThrow('Failed to save tokens: Invalid access token provided');
    });

    it('should throw error when saving invalid refresh token', () => {
      expect(() => {
        tokenManager.saveTokens(sampleTokens.validAccessToken, sampleTokens.invalidToken);
      }).toThrow('Failed to save tokens: Invalid refresh token provided');
    });

    it('should clear tokens on save failure', () => {
      const originalSetItem = mockSessionStorage.setItem;
      mockSessionStorage.setItem = vi.fn().mockImplementation(() => {
        throw new Error('Storage error');
      });

      expect(() => {
        tokenManager.saveTokens(sampleTokens.validAccessToken, sampleTokens.validRefreshToken);
      }).toThrow();

      expect(mockSessionStorage.removeItem).toHaveBeenCalled();
      expect(mockLocalStorage.removeItem).toHaveBeenCalled();

      // Restore original function
      mockSessionStorage.setItem = originalSetItem;
    });
  });

  describe('Token Retrieval', () => {
    it('should retrieve valid access token', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.validAccessToken);

      const token = tokenManager.getAccessToken();
      expect(token).toBe(sampleTokens.validAccessToken);
      expect(mockSessionStorage.getItem).toHaveBeenCalledWith('focushive_access_token');
    });

    it('should return null for expired access token', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.expiredAccessToken);

      const token = tokenManager.getAccessToken();
      expect(token).toBeNull();
      expect(mockSessionStorage.removeItem).toHaveBeenCalled();
    });

    it('should return null when no token stored', () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const token = tokenManager.getAccessToken();
      expect(token).toBeNull();
    });

    it('should retrieve valid refresh token', () => {
      mockLocalStorage.getItem.mockReturnValue(sampleTokens.validRefreshToken);

      const token = tokenManager.getRefreshToken();
      expect(token).toBe(sampleTokens.validRefreshToken);
      expect(mockLocalStorage.getItem).toHaveBeenCalledWith('focushive_refresh_token');
    });

    it('should handle storage errors gracefully', () => {
      mockSessionStorage.getItem.mockImplementation(() => {
        throw new Error('Storage error');
      });

      const token = tokenManager.getAccessToken();
      expect(token).toBeNull();
    });
  });

  describe('Token Validation', () => {
    it('should validate a correct token', () => {
      const result = tokenManager.validateToken(sampleTokens.validAccessToken);

      expect(result.isValid).toBe(true);
      expect(result.isExpired).toBe(false);
      expect(result.claims).toBeDefined();
      expect(result.claims?.sub).toBe('1234567890');
      expect(result.expiresAt).toBeInstanceOf(Date);
    });

    it('should invalidate an expired token', () => {
      const result = tokenManager.validateToken(sampleTokens.expiredAccessToken);

      expect(result.isValid).toBe(false);
      expect(result.isExpired).toBe(true);
      expect(result.error).toBe('Token has expired');
    });

    it('should invalidate a malformed token', () => {
      const result = tokenManager.validateToken(sampleTokens.invalidToken);

      expect(result.isValid).toBe(false);
      expect(result.isExpired).toBe(true);
      expect(result.error).toContain('Failed to parse token claims');
    });

    it('should invalidate empty token', () => {
      const result = tokenManager.validateToken('');

      expect(result.isValid).toBe(false);
      expect(result.isExpired).toBe(true);
      expect(result.error).toBe('Token is empty or invalid type');
    });
  });

  describe('Token Parsing', () => {
    it('should parse valid JWT token', () => {
      const claims = tokenManager.parseJWT(sampleTokens.validAccessToken);

      expect(claims).toBeDefined();
      expect(claims?.sub).toBe('1234567890');
      expect(claims?.email).toBe('test@example.com');
      expect(claims?.username).toBe('testuser');
      expect(claims?.exp).toBeDefined();
    });

    it('should return null for invalid token', () => {
      const claims = tokenManager.parseJWT(sampleTokens.invalidToken);
      expect(claims).toBeNull();
    });

    it('should return null for empty token', () => {
      const claims = tokenManager.parseJWT('');
      expect(claims).toBeNull();
    });

    it('should handle malformed JWT structure', () => {
      const claims = tokenManager.parseJWT('not.a.valid.jwt.token');
      expect(claims).toBeNull();
    });
  });

  describe('Token Expiration Checking', () => {
    it('should detect expired token', () => {
      const isExpired = tokenManager.isTokenExpired(sampleTokens.expiredAccessToken);
      expect(isExpired).toBe(true);
    });

    it('should detect valid token', () => {
      const isExpired = tokenManager.isTokenExpired(sampleTokens.validAccessToken);
      expect(isExpired).toBe(false);
    });

    it('should consider token expired if it expires within buffer time', () => {
      // Create a token that expires soon (within buffer time)
      const nowSeconds = Math.floor(Date.now() / 1000);
      const tokenExpiringInBuffer = createJWT({
        sub: '1234567890',
        email: 'test@example.com',
        exp: nowSeconds + 240, // Expires in 4 minutes (less than 5 minute buffer)
        iat: nowSeconds - 3600
      });

      const isExpired = tokenManager.isTokenExpired(tokenExpiringInBuffer);
      expect(isExpired).toBe(true);
    });

    it('should use stored token when no token provided', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.validAccessToken);

      const isExpired = tokenManager.isTokenExpired();
      expect(isExpired).toBe(false);
    });

    it('should return true when no token available', () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const isExpired = tokenManager.isTokenExpired();
      expect(isExpired).toBe(true);
    });
  });

  describe('Token Validity Checking', () => {
    it('should return true when access token is valid', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.validAccessToken);
      mockLocalStorage.getItem.mockReturnValue(sampleTokens.validRefreshToken);

      const hasValid = tokenManager.hasValidTokens();
      expect(hasValid).toBe(true);
    });

    it('should return true when access token expired but refresh token valid', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.expiredAccessToken);
      mockLocalStorage.getItem.mockReturnValue(sampleTokens.validRefreshToken);

      const hasValid = tokenManager.hasValidTokens();
      expect(hasValid).toBe(true);
    });

    it('should return false when no tokens available', () => {
      mockSessionStorage.getItem.mockReturnValue(null);
      mockLocalStorage.getItem.mockReturnValue(null);

      const hasValid = tokenManager.hasValidTokens();
      expect(hasValid).toBe(false);
    });

    it('should return false when all tokens expired', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.expiredAccessToken);
      mockLocalStorage.getItem.mockReturnValue(sampleTokens.expiredAccessToken); // Using expired for both

      const hasValid = tokenManager.hasValidTokens();
      expect(hasValid).toBe(false);
    });
  });

  describe('Token Expiration Info', () => {
    it('should return correct expiration information', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.validAccessToken);
      mockLocalStorage.getItem.mockReturnValue(sampleTokens.validRefreshToken);

      const info = tokenManager.getTokenExpirationInfo();

      expect(info.accessTokenExpiresAt).toBeInstanceOf(Date);
      expect(info.refreshTokenExpiresAt).toBeInstanceOf(Date);
      expect(info.needsRefresh).toBe(false);
    });

    it('should indicate refresh needed when access token expired', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.expiredAccessToken);
      mockLocalStorage.getItem.mockReturnValue(sampleTokens.validRefreshToken);

      const info = tokenManager.getTokenExpirationInfo();

      expect(info.needsRefresh).toBe(true);
      expect(info.refreshTokenExpiresAt).toBeInstanceOf(Date);
    });

    it('should handle missing tokens', () => {
      mockSessionStorage.getItem.mockReturnValue(null);
      mockLocalStorage.getItem.mockReturnValue(null);

      const info = tokenManager.getTokenExpirationInfo();

      expect(info.accessTokenExpiresAt).toBeNull();
      expect(info.refreshTokenExpiresAt).toBeNull();
      expect(info.needsRefresh).toBe(true);
    });
  });

  describe('User Information Extraction', () => {
    it('should extract user info from valid token', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.validAccessToken);

      const userInfo = tokenManager.getUserFromToken();

      expect(userInfo).toEqual({
        id: '1234567890',
        email: 'test@example.com',
        username: 'testuser'
      });
    });

    it('should return null when no token available', () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const userInfo = tokenManager.getUserFromToken();
      expect(userInfo).toBeNull();
    });

    it('should return null for invalid token', () => {
      mockSessionStorage.getItem.mockReturnValue(sampleTokens.invalidToken);

      const userInfo = tokenManager.getUserFromToken();
      expect(userInfo).toBeNull();
    });
  });

  describe('Token Clearing', () => {
    it('should clear all tokens and storage', () => {
      tokenManager.clearTokens();

      expect(mockSessionStorage.removeItem).toHaveBeenCalledWith('focushive_access_token');
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('focushive_refresh_token');
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('focushive_token_expires');
    });

    it('should handle storage errors during clearing', () => {
      mockSessionStorage.removeItem.mockImplementation(() => {
        throw new Error('Storage error');
      });

      // Should not throw
      expect(() => tokenManager.clearTokens()).not.toThrow();
    });
  });

  describe('Security Features', () => {
    it('should detect secure context support for httpOnly cookies', () => {
      // Mock secure context
      Object.defineProperty(window, 'isSecureContext', {value: true, writable: true});
      Object.defineProperty(navigator, 'cookieEnabled', {value: true, writable: true});

      const supports = tokenManager.supportsHttpOnlyCookies();
      expect(supports).toBe(true);
    });

    it('should detect insecure context', () => {
      Object.defineProperty(window, 'isSecureContext', {value: false, writable: true});
      Object.defineProperty(navigator, 'cookieEnabled', {value: true, writable: true});

      const supports = tokenManager.supportsHttpOnlyCookies();
      expect(supports).toBe(false);
    });

    it('should detect disabled cookies', () => {
      Object.defineProperty(window, 'isSecureContext', {value: true, writable: true});
      Object.defineProperty(navigator, 'cookieEnabled', {value: false, writable: true});

      const supports = tokenManager.supportsHttpOnlyCookies();
      expect(supports).toBe(false);
    });
  });

  describe('Automatic Token Refresh', () => {
    it('should support httpOnly cookie detection', () => {
      // Test the security feature for future httpOnly cookie implementation
      Object.defineProperty(window, 'isSecureContext', {value: true, writable: true});
      Object.defineProperty(navigator, 'cookieEnabled', {value: true, writable: true});

      const supports = tokenManager.supportsHttpOnlyCookies();
      expect(supports).toBe(true);
    });
  });
});