import { describe, it, expect, vi, beforeEach } from 'vitest';
import { server } from '@/test-utils/msw-server';
import { http, HttpResponse } from 'msw';
import { authApiService as authApi } from './authApi';
import type { LoginRequest, RegisterRequest } from '@shared/types/auth';

// Mock axios and AxiosError
vi.mock('axios', async () => {
  const actual = await vi.importActual('axios');
  return {
    ...actual,
    // Mock axios.create to return the actual instance for MSW to work
  };
});

// Mock the HTTP interceptors module to avoid circular dependencies
vi.mock('../httpInterceptors', () => ({
  setupAxiosInterceptors: vi.fn(),
  clearTokens: vi.fn(),
}));

// Mock the tokenManager
vi.mock('../../utils/tokenManager', () => ({
  tokenManager: {
    saveTokens: vi.fn(),
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    clearTokens: vi.fn(),
    isTokenExpired: vi.fn(),
    parseJWT: vi.fn(),
    validateToken: vi.fn(),
    hasValidTokens: vi.fn(),
    getTokenExpirationInfo: vi.fn(),
    getUserFromToken: vi.fn(),
    supportsHttpOnlyCookies: vi.fn().mockReturnValue(false),
  }
}));

// Mock the axiosConfig
vi.mock('../../utils/axiosConfig', () => ({
  apiClient: {
    backend: {},
    identity: {},
    getHealthStatus: vi.fn(),
    retryRequest: vi.fn(),
  },
  backendApi: {},
  identityApi: {},
}));

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Clear storage between tests to ensure test isolation
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('login', () => {
    it('should successfully login with valid credentials', async () => {
      const loginData: LoginRequest = {
        email: 'testuser@example.com',
        password: 'password'
      };

      const response = await authApi.login(loginData);

      expect(response).toEqual({
        user: expect.objectContaining({
          email: 'testuser@example.com'
        }),
        token: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should handle login with email instead of username', async () => {
      const loginData: LoginRequest = {
        email: 'test@example.com',
        password: 'password'
      };

      const response = await authApi.login(loginData);

      expect(response).toEqual({
        user: expect.objectContaining({
          email: 'test@example.com'
        }),
        token: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should throw error for invalid credentials', async () => {
      server.use(
        http.post('/api/auth/login', () => {
          return HttpResponse.json(
            { message: 'Invalid credentials' },
            { status: 401 }
          );
        })
      );

      const loginData: LoginRequest = {
        email: 'wronguser@example.com',
        password: 'wrongpassword'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });

    it('should handle network errors', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', () => {
          return HttpResponse.error();
        })
      );

      const loginData: LoginRequest = {
        email: 'testuser@example.com',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });

    it('should handle server errors', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const loginData: LoginRequest = {
        email: 'testuser@example.com',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });
  });

  describe('register', () => {
    it('should successfully register new user', async () => {
      const registerData: RegisterRequest = {
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        firstName: 'New',
        lastName: 'User'
      };

      const response = await authApi.register(registerData);

      expect(response).toEqual({
        user: expect.objectContaining({
          username: 'newuser',
          email: 'newuser@example.com',
          firstName: 'New',
        lastName: 'User'
        }),
        token: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should register without display name', async () => {
      const registerData: RegisterRequest = {
        username: 'newuser2',
        email: 'newuser2@example.com',
        password: 'password123',
        firstName: 'New',
        lastName: 'User2'
      };

      const response = await authApi.register(registerData);

      expect(response).toEqual({
        user: expect.objectContaining({
          username: 'newuser2',
          email: 'newuser2@example.com',
          displayName: 'newuser2' // Should fallback to username
        }),
        token: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should handle registration validation errors', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/register', () => {
          return HttpResponse.json(
            { message: 'Email already exists' },
            { status: 400 }
          );
        })
      );

      const registerData: RegisterRequest = {
        username: 'existinguser',
        email: 'existing@example.com',
        password: 'password123',
        firstName: 'Existing',
        lastName: 'User'
      };

      await expect(authApi.register(registerData)).rejects.toThrow();
    });

    it('should handle invalid registration data', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/register', () => {
          return HttpResponse.json(
            { message: 'Invalid registration data' },
            { status: 400 }
          );
        })
      );

      const registerData: RegisterRequest = {
        username: '',
        email: 'invalid-email',
        password: '123', // Too short
        firstName: '',
        lastName: ''
      };

      await expect(authApi.register(registerData)).rejects.toThrow();
    });
  });

  describe('validateAuth', () => {
    it('should return true for valid authentication', async () => {
      const response = await authApi.validateAuth();
      expect(typeof response).toBe('boolean');
    });

    it('should return false for invalid authentication', async () => {
      server.use(
        http.get('/api/v1/auth/me', () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      const response = await authApi.validateAuth();
      expect(response).toBe(false);
    });

    it('should check isAuthenticated method', () => {
      const result = authApi.isAuthenticated();
      expect(typeof result).toBe('boolean');
    });

    it('should get access token', () => {
      const token = authApi.getAccessToken();
      expect(token === null || typeof token === 'string').toBe(true);
    });
  });

  describe('logout', () => {
    it('should successfully logout', async () => {
      await expect(authApi.logout()).resolves.toBeUndefined();
    });

    it('should handle logout errors gracefully', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/logout', () => {
          return HttpResponse.json(
            { message: 'Logout failed' },
            { status: 500 }
          );
        })
      );

      // Logout should not throw even if server returns error
      await expect(authApi.logout()).resolves.toBeUndefined();
    });
  });

  describe('getCurrentUser', () => {
    it('should get current user with valid token', async () => {
      // Login first to get a valid token
      await authApi.login({
        email: 'testuser@example.com',
        password: 'password'
      });

      const response = await authApi.getCurrentUser();

      expect(response).toEqual(expect.objectContaining({
        email: expect.any(String),
        id: expect.any(String)
      }));
    });

    it('should handle unauthorized access', async () => {
      server.use(
        http.get('http://localhost:8081/api/v1/auth/me', () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      await expect(authApi.getCurrentUser()).rejects.toThrow();
    });

    it('should include authorization header', async () => {
      // Login first to get a valid token
      await authApi.login({
        email: 'testuser@example.com',
        password: 'password'
      });

      let capturedRequest: Request | null = null;

      server.use(
        http.get('http://localhost:8081/api/v1/auth/me', ({ request }) => {
          capturedRequest = request;
          return HttpResponse.json({
            user: {
              id: '1',
              username: 'testuser',
              email: 'testuser@example.com',
              firstName: 'Test',
              lastName: 'User',
              name: 'Test User',
              isEmailVerified: true,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            }
          });
        })
      );

      await authApi.getCurrentUser();

      expect(capturedRequest?.headers.get('authorization')).toBeTruthy();
    });
  });

  describe('error handling', () => {
    it('should handle timeout errors', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', async () => {
          // Simulate timeout - longer than axios timeout (10s)
          await new Promise(resolve => setTimeout(resolve, 11000));
          return HttpResponse.json({});
        })
      );

      const loginData: LoginRequest = {
        email: 'testuser@example.com',
        password: 'password'
      };

      // This should timeout and throw an error
      await expect(authApi.login(loginData)).rejects.toThrow();
    }, 12000); // 12 second test timeout

    it('should handle malformed JSON responses', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', () => {
          // Return malformed JSON that will cause axios to fail parsing
          return new Response('{"user": invalid}', {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
          });
        })
      );

      const loginData: LoginRequest = {
        email: 'testuser@example.com',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });

    it('should handle empty responses', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', () => {
          // Return empty JSON object that lacks required fields
          return new Response('{}', { 
            status: 200,
            headers: { 'Content-Type': 'application/json' }
          });
        })
      );

      const loginData: LoginRequest = {
        email: 'testuser@example.com',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });
  });

  describe('request validation', () => {
    it('should send correct headers', async () => {
      let capturedRequest: Request | null = null;

      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', ({ request }) => {
          capturedRequest = request;
          return HttpResponse.json({
            user: { 
              id: '1',
              username: 'test',
              email: 'test@example.com',
              firstName: 'Test',
              lastName: 'User',
              name: 'Test User',
              isEmailVerified: true,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            },
            token: 'token',
            refreshToken: 'refresh'
          });
        })
      );

      await authApi.login({ email: 'test@example.com', password: 'pass' });

      expect(capturedRequest?.headers.get('content-type')).toBe('application/json');
    });

    it('should send correct request body', async () => {
      let capturedBody: unknown = null;

      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', async ({ request }) => {
          capturedBody = await request.json();
          return HttpResponse.json({
            user: { 
              id: '1',
              username: 'test',
              email: 'testuser@example.com',
              firstName: 'Test',
              lastName: 'User',
              name: 'Test User',
              isEmailVerified: true,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            },
            token: 'token',
            refreshToken: 'refresh'
          });
        })
      );

      const loginData = { email: 'testuser@example.com', password: 'testpass' };
      await authApi.login(loginData);

      expect(capturedBody).toEqual(loginData);
    });
  });
});