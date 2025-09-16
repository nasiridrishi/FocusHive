/**
 * E2E Tests for Token Manager with Real Identity Service
 *
 * REQUIREMENTS:
 * - Real Identity Service running at http://localhost:8081
 * - No mocks or stubs
 * - Tests actual token operations
 */

import { describe, it, expect, beforeAll, beforeEach, afterEach, vi } from 'vitest';
import axios from 'axios';
import { tokenManager } from '../tokenManager';
import type { AuthTokens, User } from '../../../contracts/auth';

// Identity Service URL
const IDENTITY_SERVICE_URL = 'http://localhost:8081';

describe('TokenManager E2E Tests with Real Identity Service', () => {
  let isIdentityServiceAvailable = false;

  beforeAll(async () => {
    // Check if Identity Service is running
    console.log('Checking Identity Service at:', IDENTITY_SERVICE_URL);
    try {
      const response = await axios.get(`${IDENTITY_SERVICE_URL}/actuator/health`, {
        timeout: 5000,
        headers: {
          'Accept': 'application/json'
        }
      });
      console.log('Health check response status:', response.status);
      if (response?.data?.status === 'UP') {
        isIdentityServiceAvailable = true;
        console.log('✅ Identity Service is running and healthy');
      }
    } catch (error: any) {
      console.error('❌ Identity Service not available - tests will be skipped');
      console.error('Error:', error?.message || 'Unknown error');
    }
  });

  beforeEach(() => {
    // Clear all storage before each test
    localStorage.clear();
    sessionStorage.clear();
    tokenManager.clearAuth();
  });

  afterEach(() => {
    // Clean up after each test
    localStorage.clear();
    sessionStorage.clear();
    tokenManager.clearAuth();
  });

  describe('Token Storage', () => {
    it('should store tokens in sessionStorage when rememberMe is false', () => {
      const mockTokens: AuthTokens = {
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        scope: 'read write'
      };

      tokenManager.storeTokens(mockTokens, false);

      expect(sessionStorage.getItem('auth_access_token')).toBe('test-access-token');
      expect(sessionStorage.getItem('auth_refresh_token')).toBe('test-refresh-token');
      expect(localStorage.getItem('auth_access_token')).toBeNull();
      expect(localStorage.getItem('auth_refresh_token')).toBeNull();
    });

    it('should store tokens in localStorage when rememberMe is true', () => {
      const mockTokens: AuthTokens = {
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        scope: 'read write'
      };

      tokenManager.storeTokens(mockTokens, true);

      expect(localStorage.getItem('auth_access_token')).toBe('test-access-token');
      expect(localStorage.getItem('auth_refresh_token')).toBe('test-refresh-token');
      expect(sessionStorage.getItem('auth_access_token')).toBeNull();
      expect(sessionStorage.getItem('auth_refresh_token')).toBeNull();
    });

    it('should retrieve tokens from either storage', () => {
      // Test sessionStorage
      sessionStorage.setItem('auth_access_token', 'session-token');
      sessionStorage.setItem('auth_refresh_token', 'session-refresh');

      expect(tokenManager.getAccessToken()).toBe('session-token');
      expect(tokenManager.getRefreshToken()).toBe('session-refresh');

      sessionStorage.clear();

      // Test localStorage
      localStorage.setItem('auth_access_token', 'local-token');
      localStorage.setItem('auth_refresh_token', 'local-refresh');

      expect(tokenManager.getAccessToken()).toBe('local-token');
      expect(tokenManager.getRefreshToken()).toBe('local-refresh');
    });
  });

  describe('User Storage', () => {
    it('should store and retrieve user information', () => {
      const mockUser: User = {
        id: '123',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
        roles: ['USER'],
        accountStatus: 'active',
        emailVerified: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };

      // Store in sessionStorage
      tokenManager.storeUser(mockUser, false);
      expect(sessionStorage.getItem('auth_user')).toContain('test@example.com');

      const retrievedUser = tokenManager.getUser();
      expect(retrievedUser).toBeDefined();
      expect(retrievedUser?.email).toBe('test@example.com');
      expect(retrievedUser?.id).toBe('123');

      sessionStorage.clear();

      // Store in localStorage
      tokenManager.storeUser(mockUser, true);
      expect(localStorage.getItem('auth_user')).toContain('test@example.com');

      const retrievedUser2 = tokenManager.getUser();
      expect(retrievedUser2).toBeDefined();
      expect(retrievedUser2?.email).toBe('test@example.com');
    });
  });

  describe('Authentication Status', () => {
    it('should return false when no token exists', () => {
      expect(tokenManager.isAuthenticated()).toBe(false);
    });

    it('should return false for expired tokens', () => {
      // Create an expired JWT token
      const header = { alg: 'HS256', typ: 'JWT' };
      const payload = {
        sub: 'test-user',
        email: 'test@example.com',
        exp: Math.floor(Date.now() / 1000) - 3600, // Expired 1 hour ago
        iat: Math.floor(Date.now() / 1000) - 7200
      };

      const base64Header = btoa(JSON.stringify(header));
      const base64Payload = btoa(JSON.stringify(payload));
      const expiredToken = `${base64Header}.${base64Payload}.fake-signature`;

      localStorage.setItem('auth_access_token', expiredToken);

      expect(tokenManager.isAuthenticated()).toBe(false);
    });

    it('should return true for valid tokens', () => {
      // Create a valid JWT token
      const header = { alg: 'HS256', typ: 'JWT' };
      const payload = {
        sub: 'test-user',
        email: 'test@example.com',
        exp: Math.floor(Date.now() / 1000) + 3600, // Expires in 1 hour
        iat: Math.floor(Date.now() / 1000)
      };

      const base64Header = btoa(JSON.stringify(header));
      const base64Payload = btoa(JSON.stringify(payload));
      const validToken = `${base64Header}.${base64Payload}.fake-signature`;

      localStorage.setItem('auth_access_token', validToken);

      expect(tokenManager.isAuthenticated()).toBe(true);
    });
  });

  describe('Clear Auth', () => {
    it('should clear all auth data from both storages', () => {
      // Set data in both storages
      localStorage.setItem('auth_access_token', 'local-token');
      localStorage.setItem('auth_refresh_token', 'local-refresh');
      localStorage.setItem('auth_user', '{"email":"local@test.com"}');

      sessionStorage.setItem('auth_access_token', 'session-token');
      sessionStorage.setItem('auth_refresh_token', 'session-refresh');
      sessionStorage.setItem('auth_user', '{"email":"session@test.com"}');

      // Clear auth
      tokenManager.clearAuth();

      // Check both storages are cleared
      expect(localStorage.getItem('auth_access_token')).toBeNull();
      expect(localStorage.getItem('auth_refresh_token')).toBeNull();
      expect(localStorage.getItem('auth_user')).toBeNull();

      expect(sessionStorage.getItem('auth_access_token')).toBeNull();
      expect(sessionStorage.getItem('auth_refresh_token')).toBeNull();
      expect(sessionStorage.getItem('auth_user')).toBeNull();
    });
  });

  describe('Token Refresh', () => {
    it.runIf(isIdentityServiceAvailable)('should handle token refresh errors gracefully', async () => {
      // Set an invalid refresh token
      localStorage.setItem('auth_refresh_token', 'invalid-refresh-token');

      try {
        await tokenManager.refreshAccessToken();
        // Should not reach here
        expect(true).toBe(false);
      } catch (error: any) {
        // Should throw an error
        expect(error).toBeDefined();
        expect(error.code).toBeDefined();
        expect(['TOKEN_REFRESH_FAILED', 'INVALID_TOKEN', 'UNAUTHORIZED']).toContain(error.code);

        // Auth should be cleared after failed refresh
        expect(localStorage.getItem('auth_access_token')).toBeNull();
        expect(localStorage.getItem('auth_refresh_token')).toBeNull();
      }
    });

    it('should throw error when no refresh token is available', async () => {
      // Clear all tokens
      tokenManager.clearAuth();

      try {
        await tokenManager.refreshAccessToken();
        // Should not reach here
        expect(true).toBe(false);
      } catch (error: any) {
        expect(error.message).toBe('No refresh token available');
      }
    });

    it.runIf(isIdentityServiceAvailable)('should prevent concurrent refresh attempts', async () => {
      // Set a refresh token
      localStorage.setItem('auth_refresh_token', 'test-refresh-token');

      // Start multiple refresh attempts
      const promise1 = tokenManager.refreshAccessToken();
      const promise2 = tokenManager.refreshAccessToken();
      const promise3 = tokenManager.refreshAccessToken();

      // All should return the same promise
      expect(promise1).toBe(promise2);
      expect(promise2).toBe(promise3);

      // Wait for all to complete (they will fail with invalid token, but that's ok)
      try {
        await Promise.all([promise1, promise2, promise3]);
      } catch {
        // Expected to fail with invalid token
      }
    });
  });

  describe('Authorization Header', () => {
    it('should return empty object when no token exists', () => {
      tokenManager.clearAuth();
      const header = tokenManager.getAuthHeader();
      expect(header).toEqual({});
    });

    it('should return Authorization header when token exists', () => {
      localStorage.setItem('auth_access_token', 'test-token-123');
      const header = tokenManager.getAuthHeader();
      expect(header).toEqual({ Authorization: 'Bearer test-token-123' });
    });
  });

  describe('401 Handler', () => {
    it.runIf(isIdentityServiceAvailable)('should return false when refresh fails', async () => {
      // Set invalid refresh token
      localStorage.setItem('auth_refresh_token', 'invalid-token');

      const result = await tokenManager.handle401();
      expect(result).toBe(false);

      // Auth should be cleared
      expect(localStorage.getItem('auth_access_token')).toBeNull();
      expect(localStorage.getItem('auth_refresh_token')).toBeNull();
    });

    it('should return false when no refresh token exists', async () => {
      tokenManager.clearAuth();

      const result = await tokenManager.handle401();
      expect(result).toBe(false);
    });
  });
});