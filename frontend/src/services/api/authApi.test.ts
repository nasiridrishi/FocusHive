import { describe, it, expect, vi, beforeEach } from 'vitest';
import { server } from '@/test-utils/msw-server';
import { http, HttpResponse } from 'msw';
import * as authApi from '../authApi';
import type { LoginRequest, RegisterRequest } from '@shared/types/auth';

// Mock the HTTP interceptors module to avoid circular dependencies
vi.mock('../httpInterceptors', () => ({
  setupAxiosInterceptors: vi.fn(),
  clearTokens: vi.fn(),
}));

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('should successfully login with valid credentials', async () => {
      const loginData: LoginRequest = {
        username: 'testuser',
        password: 'password'
      };

      const response = await authApi.login(loginData);

      expect(response).toEqual({
        user: expect.objectContaining({
          username: 'testuser',
          email: 'test@example.com'
        }),
        accessToken: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should handle login with email instead of username', async () => {
      const loginData: LoginRequest = {
        username: 'test@example.com', // Using email as username
        password: 'password'
      };

      const response = await authApi.login(loginData);

      expect(response).toEqual({
        user: expect.objectContaining({
          email: 'test@example.com'
        }),
        accessToken: expect.any(String),
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
        username: 'wronguser',
        password: 'wrongpassword'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });

    it('should handle network errors', async () => {
      server.use(
        http.post('/api/auth/login', () => {
          return HttpResponse.error();
        })
      );

      const loginData: LoginRequest = {
        username: 'testuser',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });

    it('should handle server errors', async () => {
      server.use(
        http.post('/api/auth/login', () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const loginData: LoginRequest = {
        username: 'testuser',
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
        displayName: 'New User'
      };

      const response = await authApi.register(registerData);

      expect(response).toEqual({
        user: expect.objectContaining({
          username: 'newuser',
          email: 'newuser@example.com',
          displayName: 'New User'
        }),
        accessToken: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should register without display name', async () => {
      const registerData: RegisterRequest = {
        username: 'newuser2',
        email: 'newuser2@example.com',
        password: 'password123'
      };

      const response = await authApi.register(registerData);

      expect(response).toEqual({
        user: expect.objectContaining({
          username: 'newuser2',
          email: 'newuser2@example.com',
          displayName: 'newuser2' // Should fallback to username
        }),
        accessToken: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should handle registration validation errors', async () => {
      server.use(
        http.post('/api/auth/register', () => {
          return HttpResponse.json(
            { message: 'Email already exists' },
            { status: 400 }
          );
        })
      );

      const registerData: RegisterRequest = {
        username: 'existinguser',
        email: 'existing@example.com',
        password: 'password123'
      };

      await expect(authApi.register(registerData)).rejects.toThrow();
    });

    it('should handle invalid registration data', async () => {
      server.use(
        http.post('/api/auth/register', () => {
          return HttpResponse.json(
            { message: 'Invalid registration data' },
            { status: 400 }
          );
        })
      );

      const registerData: RegisterRequest = {
        username: '',
        email: 'invalid-email',
        password: '123' // Too short
      };

      await expect(authApi.register(registerData)).rejects.toThrow();
    });
  });

  describe('refreshToken', () => {
    it('should successfully refresh tokens', async () => {
      const response = await authApi.refreshToken();

      expect(response).toEqual({
        accessToken: expect.any(String),
        refreshToken: expect.any(String)
      });
    });

    it('should handle invalid refresh token', async () => {
      server.use(
        http.post('/api/auth/refresh', () => {
          return HttpResponse.json(
            { message: 'Invalid refresh token' },
            { status: 401 }
          );
        })
      );

      await expect(authApi.refreshToken()).rejects.toThrow();
    });

    it('should handle expired refresh token', async () => {
      server.use(
        http.post('/api/auth/refresh', () => {
          return HttpResponse.json(
            { message: 'Refresh token expired' },
            { status: 401 }
          );
        })
      );

      await expect(authApi.refreshToken()).rejects.toThrow();
    });
  });

  describe('logout', () => {
    it('should successfully logout', async () => {
      const response = await authApi.logout();

      expect(response).toEqual({ success: true });
    });

    it('should handle logout errors gracefully', async () => {
      server.use(
        http.post('/api/auth/logout', () => {
          return HttpResponse.json(
            { message: 'Logout failed' },
            { status: 500 }
          );
        })
      );

      // Logout should not throw even if server returns error
      const response = await authApi.logout();
      expect(response).toBeDefined();
    });
  });

  describe('getCurrentUser', () => {
    it('should get current user with valid token', async () => {
      const response = await authApi.getCurrentUser();

      expect(response).toEqual(expect.objectContaining({
        username: 'testuser',
        email: 'test@example.com',
        id: expect.any(String)
      }));
    });

    it('should handle unauthorized access', async () => {
      server.use(
        http.get('/api/auth/me', () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      await expect(authApi.getCurrentUser()).rejects.toThrow();
    });

    it('should include authorization header', async () => {
      let capturedRequest: Request | null = null;

      server.use(
        http.get('/api/auth/me', ({ request }) => {
          capturedRequest = request;
          return HttpResponse.json({
            id: '1',
            username: 'testuser',
            email: 'test@example.com'
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
        http.post('/api/auth/login', async () => {
          // Simulate timeout
          await new Promise(resolve => setTimeout(resolve, 10000));
          return HttpResponse.json({});
        })
      );

      const loginData: LoginRequest = {
        username: 'testuser',
        password: 'password'
      };

      // This should timeout and throw an error
      await expect(authApi.login(loginData)).rejects.toThrow();
    }, 5000); // 5 second test timeout

    it('should handle malformed JSON responses', async () => {
      server.use(
        http.post('/api/auth/login', () => {
          return new Response('Invalid JSON{', {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
          });
        })
      );

      const loginData: LoginRequest = {
        username: 'testuser',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });

    it('should handle empty responses', async () => {
      server.use(
        http.post('/api/auth/login', () => {
          return new Response('', { status: 200 });
        })
      );

      const loginData: LoginRequest = {
        username: 'testuser',
        password: 'password'
      };

      await expect(authApi.login(loginData)).rejects.toThrow();
    });
  });

  describe('request validation', () => {
    it('should send correct headers', async () => {
      let capturedRequest: Request | null = null;

      server.use(
        http.post('/api/auth/login', ({ request }) => {
          capturedRequest = request;
          return HttpResponse.json({
            user: { username: 'test' },
            accessToken: 'token',
            refreshToken: 'refresh'
          });
        })
      );

      await authApi.login({ username: 'test', password: 'pass' });

      expect(capturedRequest?.headers.get('content-type')).toContain('application/json');
    });

    it('should send correct request body', async () => {
      let capturedBody: any = null;

      server.use(
        http.post('/api/auth/login', async ({ request }) => {
          capturedBody = await request.json();
          return HttpResponse.json({
            user: { username: 'test' },
            accessToken: 'token',
            refreshToken: 'refresh'
          });
        })
      );

      const loginData = { username: 'testuser', password: 'testpass' };
      await authApi.login(loginData);

      expect(capturedBody).toEqual(loginData);
    });
  });
});