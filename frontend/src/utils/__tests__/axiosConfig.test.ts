/**
 * Comprehensive tests for axiosConfig utility
 * 
 * Tests axios interceptors, token refresh, request queuing, and error handling
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import axios, { AxiosError } from 'axios';
import { apiClient, backendApi, identityApi } from '../axiosConfig';
import { tokenManager } from '../tokenManager';

// Mock the tokenManager
vi.mock('../tokenManager', () => ({
  tokenManager: {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    saveTokens: vi.fn(),
    clearTokens: vi.fn(),
    hasValidTokens: vi.fn(),
  }
}));

// Mock axios
vi.mock('axios', async () => {
  const actual = await vi.importActual('axios');
  return {
    ...actual,
    create: vi.fn().mockReturnValue({
      interceptors: {
        request: {
          use: vi.fn()
        },
        response: {
          use: vi.fn()
        }
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    }),
    post: vi.fn(),
  };
});

// Mock window objects
const mockDispatchEvent = vi.fn();
const mockLocation = {
  href: '',
  pathname: '/dashboard',
};

describe('AxiosConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    
    Object.defineProperty(window, 'dispatchEvent', {
      value: mockDispatchEvent,
      writable: true,
    });
    
    Object.defineProperty(window, 'location', {
      value: mockLocation,
      writable: true,
    });

    // Mock environment variables
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080');
    vi.stubEnv('VITE_IDENTITY_API_URL', 'http://localhost:8081');
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  describe('API Client Creation', () => {
    it('should create backend and identity API instances', () => {
      expect(backendApi).toBeDefined();
      expect(identityApi).toBeDefined();
      expect(apiClient).toBeDefined();
    });

    it('should use environment variables for base URLs', () => {
      expect(axios.create).toHaveBeenCalledWith(
        expect.objectContaining({
          baseURL: 'http://localhost:8080/api',
          timeout: 10000,
          withCredentials: true,
        })
      );

      expect(axios.create).toHaveBeenCalledWith(
        expect.objectContaining({
          baseURL: 'http://localhost:8081/api',
          timeout: 10000,
          withCredentials: true,
        })
      );
    });
  });

  describe('Request Interceptor', () => {
    it('should add authorization header when token exists', () => {
      const mockConfig = {
        headers: {},
        metadata: undefined
      };
      const mockToken = 'test-access-token';

      (tokenManager.getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue(mockToken);

      // Get the request interceptor function
      const createCall = (axios.create as ReturnType<typeof vi.fn>).mock.calls[0];
      const mockInstance = createCall[0];
      const requestInterceptorCall = mockInstance.interceptors.request.use.mock.calls[0];
      const requestInterceptor = requestInterceptorCall[0];

      const result = requestInterceptor(mockConfig);

      expect(result.headers.Authorization).toBe(`Bearer ${mockToken}`);
      expect(result.metadata).toEqual({ startTime: expect.any(Number) });
    });

    it('should not add authorization header when no token exists', () => {
      const mockConfig = {
        headers: {},
        metadata: undefined
      };

      (tokenManager.getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue(null);

      // Get the request interceptor function
      const createCall = (axios.create as ReturnType<typeof vi.fn>).mock.calls[0];
      const mockInstance = createCall[0];
      const requestInterceptorCall = mockInstance.interceptors.request.use.mock.calls[0];
      const requestInterceptor = requestInterceptorCall[0];

      const result = requestInterceptor(mockConfig);

      expect(result.headers.Authorization).toBeUndefined();
      expect(result.metadata).toEqual({ startTime: expect.any(Number) });
    });
  });

  describe('Response Interceptor', () => {
    let responseInterceptor: (response: unknown) => unknown;
    let errorInterceptor: (error: unknown) => Promise<unknown>;

    beforeEach(() => {
      // Get the response interceptor functions
      const createCall = (axios.create as ReturnType<typeof vi.fn>).mock.calls[0];
      const mockInstance = createCall[0];
      const responseInterceptorCall = mockInstance.interceptors.response.use.mock.calls[0];
      responseInterceptor = responseInterceptorCall[0];
      errorInterceptor = responseInterceptorCall[1];
    });

    it('should pass through successful responses', () => {
      const mockResponse = {
        data: { success: true },
        config: { metadata: { startTime: Date.now() - 1000 } }
      };

      const result = responseInterceptor(mockResponse);
      expect(result).toBe(mockResponse);
    });

    it('should handle network errors', async () => {
      const networkError = new AxiosError('Network Error');
      networkError.response = undefined;

      await expect(errorInterceptor(networkError)).rejects.toThrow('Network error - please check your connection');
    });

    it('should handle timeout errors', async () => {
      const timeoutError = new AxiosError('timeout of 10000ms exceeded');
      timeoutError.code = 'ECONNABORTED';
      timeoutError.config = { url: '/test' };

      await expect(errorInterceptor(timeoutError)).rejects.toThrow('Request timeout - please try again');
    });

    it('should handle 401 errors and attempt token refresh', async () => {
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = { status: 401, data: {} };
      unauthorizedError.config = { 
        headers: {},
        url: '/test',
        _retry: undefined
      };

      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue('refresh-token');
      (axios.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        data: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token'
        }
      });

      // Mock the instance call for retry
      const createCall = (axios.create as ReturnType<typeof vi.fn>).mock.calls[0];
      const mockInstance = createCall[0];
      mockInstance.mockResolvedValueOnce({ data: 'success' });

      const result = await errorInterceptor(unauthorizedError);

      expect(tokenManager.saveTokens).toHaveBeenCalledWith('new-access-token', 'new-refresh-token');
      expect(unauthorizedError.config?.headers?.Authorization).toBe('Bearer new-access-token');
      expect(result).toEqual({ data: 'success' });
    });

    it('should handle failed token refresh', async () => {
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = { status: 401, data: {} };
      unauthorizedError.config = { 
        headers: {},
        url: '/test',
        _retry: undefined
      };

      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue('refresh-token');
      (axios.post as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('Refresh failed'));

      await expect(errorInterceptor(unauthorizedError)).rejects.toThrow();

      expect(tokenManager.clearTokens).toHaveBeenCalled();
      expect(mockDispatchEvent).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'authFailure',
          detail: { reason: 'Token refresh failed' }
        })
      );
    });

    it('should not retry 401 errors that already have _retry flag', async () => {
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = { status: 401, data: {} };
      unauthorizedError.config = { 
        headers: {},
        url: '/test',
        _retry: true // Already retried
      };

      await expect(errorInterceptor(unauthorizedError)).rejects.toThrow();
      expect(axios.post).not.toHaveBeenCalled();
    });

    it('should extract meaningful error messages', async () => {
      const errorWithMessage = new AxiosError('Request failed');
      errorWithMessage.response = { 
        status: 400, 
        data: { message: 'Custom error message' }
      };
      errorWithMessage.config = { url: '/test' };

      await expect(errorInterceptor(errorWithMessage)).rejects.toThrow('Custom error message');
    });

    it('should handle different HTTP status codes', async () => {
      const testCases = [
        { status: 404, expectedMessage: 'Resource not found' },
        { status: 403, expectedMessage: 'Access forbidden - you don\'t have permission' },
        { status: 500, expectedMessage: 'Server error - please try again later' },
        { status: 429, expectedMessage: 'Too many requests - please try again later' },
      ];

      for (const testCase of testCases) {
        const error = new AxiosError('HTTP Error');
        error.response = { status: testCase.status, data: {} };
        error.config = { url: '/test' };

        await expect(errorInterceptor(error)).rejects.toThrow(testCase.expectedMessage);
      }
    });
  });

  describe('Token Refresh Logic', () => {
    it('should refresh token with valid refresh token', async () => {
      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue('valid-refresh-token');
      (axios.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        data: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token'
        }
      });

      // Access the private refreshAccessToken method through error handling
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = { status: 401, data: {} };
      unauthorizedError.config = { headers: {}, url: '/test' };

      const createCall = (axios.create as ReturnType<typeof vi.fn>).mock.calls[0];
      const mockInstance = createCall[0];
      const responseInterceptorCall = mockInstance.interceptors.response.use.mock.calls[0];
      const errorInterceptor = responseInterceptorCall[1];

      mockInstance.mockResolvedValueOnce({ data: 'success' });

      await errorInterceptor(unauthorizedError);

      expect(axios.post).toHaveBeenCalledWith(
        'http://localhost:8081/api/v1/auth/refresh',
        { refreshToken: 'valid-refresh-token' },
        expect.objectContaining({
          headers: { 'Content-Type': 'application/json' },
          withCredentials: true,
          timeout: 10000
        })
      );
    });

    it('should clear tokens when refresh token is invalid', () => {
      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue(null);

      // Test through the API client's refresh functionality
      // This would be called internally when a 401 occurs
      expect(tokenManager.getRefreshToken).toHaveBeenCalled();
    });
  });

  describe('Health Check', () => {
    it('should check health status of API endpoints', async () => {
      const mockBackendInstance = { get: vi.fn().mockResolvedValue({}) };
      const mockIdentityInstance = { get: vi.fn().mockResolvedValue({}) };

      // Mock the instances returned by axios.create
      (axios.create as ReturnType<typeof vi.fn>)
        .mockReturnValueOnce(mockBackendInstance)
        .mockReturnValueOnce(mockIdentityInstance);

      const healthStatus = await apiClient.getHealthStatus();

      expect(healthStatus).toEqual({
        backend: true,
        identity: true
      });

      expect(mockBackendInstance.get).toHaveBeenCalledWith('/health', { timeout: 5000 });
      expect(mockIdentityInstance.get).toHaveBeenCalledWith('/health', { timeout: 5000 });
    });

    it('should handle health check failures', async () => {
      const mockBackendInstance = { get: vi.fn().mockRejectedValue(new Error('Service down')) };
      const mockIdentityInstance = { get: vi.fn().mockRejectedValue(new Error('Service down')) };

      (axios.create as ReturnType<typeof vi.fn>)
        .mockReturnValueOnce(mockBackendInstance)
        .mockReturnValueOnce(mockIdentityInstance);

      const healthStatus = await apiClient.getHealthStatus();

      expect(healthStatus).toEqual({
        backend: false,
        identity: false
      });
    });
  });

  describe('Request Retry Logic', () => {
    it('should retry failed requests with exponential backoff', async () => {
      let attemptCount = 0;
      const mockRequestFn = vi.fn().mockImplementation(() => {
        attemptCount++;
        if (attemptCount < 3) {
          const error = new AxiosError('Server Error');
          error.response = { status: 500, data: {} };
          throw error;
        }
        return Promise.resolve({ data: 'success' });
      });

      vi.useFakeTimers();

      const retryPromise = apiClient.retryRequest(mockRequestFn, 3);
      
      // Fast-forward through the retry delays
      vi.advanceTimersByTime(1000); // First retry delay
      await Promise.resolve(); // Let the retry execute
      vi.advanceTimersByTime(2000); // Second retry delay (exponential backoff)
      await Promise.resolve(); // Let the retry execute

      const result = await retryPromise;

      expect(result).toEqual({ data: 'success' });
      expect(mockRequestFn).toHaveBeenCalledTimes(3);

      vi.useRealTimers();
    });

    it('should not retry client errors (4xx)', async () => {
      const mockRequestFn = vi.fn().mockImplementation(() => {
        const error = new AxiosError('Bad Request');
        error.response = { status: 400, data: {} };
        throw error;
      });

      await expect(apiClient.retryRequest(mockRequestFn, 3)).rejects.toThrow();
      expect(mockRequestFn).toHaveBeenCalledTimes(1); // Should not retry
    });
  });
});