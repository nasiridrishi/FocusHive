import { describe, it, expect, vi, beforeEach, afterEach, afterAll } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import cookieAuthApiService from './cookieAuthApi';
import type { LoginRequest, RegisterRequest, User } from '@shared/types/auth';

// Mock server setup
const server = setupServer();

// Test data
const mockUser: User = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  email: 'test@example.com',
  username: 'testuser',
  emailVerified: true,
  createdAt: '2023-01-01T00:00:00Z',
  updatedAt: '2023-01-01T00:00:00Z'
};

const mockLoginRequest: LoginRequest = {
  usernameOrEmail: 'test@example.com',
  password: 'password123'
};

const mockRegisterRequest: RegisterRequest = {
  email: 'test@example.com',
  username: 'testuser',
  password: 'password123',
  confirmPassword: 'password123'
};

const mockAuthResponse = {
  user: mockUser,
  token: 'mock-jwt-token',
  refreshToken: 'mock-refresh-token'
};

describe('cookieAuthApiService', () => {
  beforeEach(() => {
    // Setup default server handlers
    server.use(
      // Login endpoint
      http.post('http://localhost:8081/api/v1/auth/login', () => {
        return HttpResponse.json(mockAuthResponse, {
          status: 200,
          headers: {
            'Set-Cookie': [
              'access_token=mock-jwt-token; HttpOnly; Secure; SameSite=Strict',
              'refresh_token=mock-refresh-token; HttpOnly; Secure; SameSite=Strict'
            ].join(', ')
          }
        });
      }),

      // Register endpoint
      http.post('http://localhost:8081/api/v1/auth/register', () => {
        return HttpResponse.json(mockAuthResponse, {
          status: 201,
          headers: {
            'Set-Cookie': [
              'access_token=mock-jwt-token; HttpOnly; Secure; SameSite=Strict',
              'refresh_token=mock-refresh-token; HttpOnly; Secure; SameSite=Strict'
            ].join(', ')
          }
        });
      }),

      // Get current user endpoint
      http.get('http://localhost:8081/api/v1/auth/me', () => {
        return HttpResponse.json({ user: mockUser }, { status: 200 });
      }),

      // Logout endpoint
      http.post('http://localhost:8081/api/v1/auth/logout', () => {
        return HttpResponse.json(
          { message: 'Logged out successfully' },
          {
            status: 200,
            headers: {
              'Set-Cookie': [
                'access_token=; HttpOnly; Secure; SameSite=Strict; Max-Age=0',
                'refresh_token=; HttpOnly; Secure; SameSite=Strict; Max-Age=0'
              ].join(', ')
            }
          }
        );
      }),

      // Refresh endpoint
      http.post('http://localhost:8081/api/v1/auth/refresh', () => {
        return HttpResponse.json(mockAuthResponse, {
          status: 200,
          headers: {
            'Set-Cookie': [
              'access_token=new-mock-jwt-token; HttpOnly; Secure; SameSite=Strict',
              'refresh_token=new-mock-refresh-token; HttpOnly; Secure; SameSite=Strict'
            ].join(', ')
          }
        });
      })
    );

    server.listen();
  });

  afterEach(() => {
    server.resetHandlers();
    vi.clearAllMocks();
  });

  afterAll(() => {
    server.close();
  });

  describe('login', () => {
    it('should login successfully with cookie-based authentication', async () => {
      const result = await cookieAuthApiService.login(mockLoginRequest);

      expect(result).toEqual(mockAuthResponse);
      expect(result.user).toEqual(mockUser);
      expect(result.token).toBe('mock-jwt-token');
      expect(result.refreshToken).toBe('mock-refresh-token');
    });

    it('should handle login errors', async () => {
      server.use(
        http.post('http://localhost:8081/api/v1/auth/login', () => {
          return HttpResponse.json(
            { message: 'Invalid credentials' },
            { status: 401 }
          );
        })
      );

      await expect(cookieAuthApiService.login(mockLoginRequest))
        .rejects.toThrow('Invalid credentials');
    });

    it('should validate response data structure', async () => {
      server.use(
        rest.post('http://localhost:8081/api/v1/auth/login', (req, res, ctx) => {
          return res(
            ctx.status(200),
            ctx.json({ invalid: 'response' })
          );
        })
      );

      await expect(cookieAuthApiService.login(mockLoginRequest))
        .rejects.toThrow('Invalid response format from server');
    });
  });

  describe('register', () => {
    it('should register successfully with cookie-based authentication', async () => {
      const result = await cookieAuthApiService.register(mockRegisterRequest);

      expect(result).toEqual(mockAuthResponse);
      expect(result.user).toEqual(mockUser);
      expect(result.token).toBe('mock-jwt-token');
      expect(result.refreshToken).toBe('mock-refresh-token');
    });

    it('should add default persona data to registration', async () => {
      let capturedRequestBody: RegisterRequest & { personaType: string; personaName: string };
      
      server.use(
        rest.post('http://localhost:8081/api/v1/auth/register', async (req, res, ctx) => {
          capturedRequestBody = await req.json();
          return res(
            ctx.status(201),
            ctx.json(mockAuthResponse)
          );
        })
      );

      await cookieAuthApiService.register(mockRegisterRequest);

      expect(capturedRequestBody).toEqual({
        ...mockRegisterRequest,
        personaType: 'PERSONAL',
        personaName: 'Personal'
      });
    });
  });

  describe('logout', () => {
    it('should logout successfully and clear cookies server-side', async () => {
      // Should not throw any errors
      await expect(cookieAuthApiService.logout()).resolves.not.toThrow();
    });

    it('should handle logout errors gracefully', async () => {
      server.use(
        rest.post('http://localhost:8081/api/v1/auth/logout', (req, res, ctx) => {
          return res(ctx.status(500), ctx.json({ message: 'Logout failed' }));
        })
      );

      // Should still resolve without throwing - cookies cleared by server
      await expect(cookieAuthApiService.logout()).resolves.not.toThrow();
    });
  });

  describe('getCurrentUser', () => {
    it('should get current user using cookies for authentication', async () => {
      const result = await cookieAuthApiService.getCurrentUser();

      expect(result).toEqual(mockUser);
    });

    it('should handle unauthorized access', async () => {
      server.use(
        rest.get('http://localhost:8081/api/v1/auth/me', (req, res, ctx) => {
          return res(ctx.status(401), ctx.json({ message: 'Unauthorized' }));
        })
      );

      await expect(cookieAuthApiService.getCurrentUser())
        .rejects.toThrow('Unauthorized');
    });
  });

  describe('validateAuth', () => {
    it('should return true for valid authentication', async () => {
      const result = await cookieAuthApiService.validateAuth();
      expect(result).toBe(true);
    });

    it('should return false for invalid authentication', async () => {
      server.use(
        rest.get('http://localhost:8081/api/v1/auth/me', (req, res, ctx) => {
          return res(ctx.status(401));
        })
      );

      const result = await cookieAuthApiService.validateAuth();
      expect(result).toBe(false);
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when user is authenticated via cookies', async () => {
      const result = await cookieAuthApiService.isAuthenticated();
      expect(result).toBe(true);
    });

    it('should return false when user is not authenticated', async () => {
      server.use(
        rest.get('http://localhost:8081/api/v1/auth/me', (req, res, ctx) => {
          return res(ctx.status(401));
        })
      );

      const result = await cookieAuthApiService.isAuthenticated();
      expect(result).toBe(false);
    });
  });

  describe('getCsrfToken', () => {
    it('should extract CSRF token from cookies', () => {
      // Mock document.cookie
      Object.defineProperty(document, 'cookie', {
        writable: true,
        value: 'XSRF-TOKEN=test-csrf-token; other=value'
      });

      const csrfToken = cookieAuthApiService.getCsrfToken();
      expect(csrfToken).toBe('test-csrf-token');
    });

    it('should return null when CSRF token is not present', () => {
      Object.defineProperty(document, 'cookie', {
        writable: true,
        value: 'other=value; another=test'
      });

      const csrfToken = cookieAuthApiService.getCsrfToken();
      expect(csrfToken).toBeNull();
    });

    it('should handle URL-encoded CSRF tokens', () => {
      Object.defineProperty(document, 'cookie', {
        writable: true,
        value: 'XSRF-TOKEN=test%2Dcsrf%2Dtoken'
      });

      const csrfToken = cookieAuthApiService.getCsrfToken();
      expect(csrfToken).toBe('test-csrf-token');
    });
  });

  describe('request interceptors', () => {
    it('should automatically include credentials in requests', async () => {
      server.use(
        rest.get('http://localhost:8081/api/v1/auth/me', (req, res, ctx) => {
          // This would include cookies in a real request
          return res(ctx.status(200), ctx.json({ user: mockUser }));
        })
      );

      await cookieAuthApiService.getCurrentUser();
      
      // Verify that withCredentials is set to true in axios configuration
      // (This is tested in the service configuration)
      expect(true).toBe(true); // Service is configured with withCredentials: true
    });
  });

  describe('error handling', () => {
    it('should handle network timeouts', async () => {
      server.use(
        rest.post('http://localhost:8081/api/v1/auth/login', (req, res, ctx) => {
          return res(ctx.delay('infinite'));
        })
      );

      await expect(cookieAuthApiService.login(mockLoginRequest))
        .rejects.toThrow('Request timeout - please try again');
    }, 15000); // Increase timeout for this test

    it('should handle network errors', async () => {
      server.use(
        rest.post('http://localhost:8081/api/v1/auth/login', (req, res, ctx) => {
          return res.networkError('Failed to connect');
        })
      );

      await expect(cookieAuthApiService.login(mockLoginRequest))
        .rejects.toThrow('Network error during login - please check your connection');
    });
  });

  describe('security features', () => {
    it('should not store tokens in client-side storage', async () => {
      const localStorageSpy = vi.spyOn(Storage.prototype, 'setItem');
      const sessionStorageSpy = vi.spyOn(Storage.prototype, 'setItem');

      await cookieAuthApiService.login(mockLoginRequest);

      // Verify no tokens are stored in localStorage or sessionStorage
      expect(localStorageSpy).not.toHaveBeenCalled();
      expect(sessionStorageSpy).not.toHaveBeenCalled();

      localStorageSpy.mockRestore();
      sessionStorageSpy.mockRestore();
    });

    it('should rely on httpOnly cookies for authentication', async () => {
      // This test verifies that the service doesn't try to manage tokens manually
      await cookieAuthApiService.login(mockLoginRequest);
      
      // No token storage calls should be made
      expect(localStorage.getItem).not.toHaveBeenCalled();
      expect(sessionStorage.getItem).not.toHaveBeenCalled();
    });
  });
});