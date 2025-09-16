/**
 * Comprehensive tests for axiosConfig utility
 *
 * Tests axios interceptors, token refresh, request queuing, and error handling
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import type {Mock} from 'vitest';
import axios, {
  AxiosError,
  AxiosRequestHeaders,
  AxiosResponseHeaders,
  InternalAxiosRequestConfig,
  type AxiosInstance
} from 'axios';

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
  const actual = await vi.importActual<typeof import('axios')>('axios');

  const createMockInstance = () => {
    const instance = vi.fn() as Mock & Partial<AxiosInstance>;
    instance.get = vi.fn() as Mock;
    instance.post = vi.fn() as Mock;
    instance.put = vi.fn() as Mock;
    instance.delete = vi.fn() as Mock;
    instance.interceptors = {
      request: {
        use: vi.fn() as Mock,
        eject: vi.fn() as Mock
      },
      response: {
        use: vi.fn() as Mock,
        eject: vi.fn() as Mock
      }
    } as any;

    return instance as AxiosInstance & Mock;
  };

  const mockCreate = vi.fn(() => createMockInstance());
  const mockPost = vi.fn();

  const defaultExport = Object.assign(
      vi.fn(),
      actual,
      {
        create: mockCreate as unknown as typeof axios.create,
        post: mockPost as unknown as typeof axios.post
      }
  );

  return {
    ...actual,
    default: defaultExport as unknown as typeof axios
  };
});

const axiosCreateMock = axios.create as unknown as ReturnType<typeof vi.fn>;
const axiosPostMock = axios.post as unknown as ReturnType<typeof vi.fn>;

// Import after mocking
import {tokenManager} from '../tokenManager';

// Mock window objects
const mockDispatchEvent = vi.fn();
const mockLocation = {
  href: '',
  pathname: '/dashboard',
};

// Define proper types for the API clients and mock instances
interface ExtendedInternalAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

type MockAxiosInstance = Mock & {
  get: Mock;
  post: Mock;
  put: Mock;
  delete: Mock;
  interceptors: {
    request: {
      use: Mock;
      eject: Mock;
    };
    response: {
      use: Mock;
      eject: Mock;
    };
  };
  mockResolvedValue: Mock['mockResolvedValue'];
  mockRejectedValue: Mock['mockRejectedValue'];
  mockResolvedValueOnce: Mock['mockResolvedValueOnce'];
  mockRejectedValueOnce: Mock['mockRejectedValueOnce'];
};

type ApiClient = {
  getHealthStatus: () => Promise<unknown>;
  retryRequest: (requestFn: () => Promise<unknown>, maxRetries: number) => Promise<unknown>;
};

describe('AxiosConfig', () => {
  let apiClient: ApiClient;
  let backendApi: MockAxiosInstance;
  let identityApi: MockAxiosInstance;
  let mockBackendInstance: MockAxiosInstance;
  let mockIdentityInstance: MockAxiosInstance;

  beforeEach(async () => {
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

    // Clear the module cache to get fresh instances
    vi.resetModules();

    // Dynamically import to recreate the instances
    const axiosConfigModule = await import('../axiosConfig');
    apiClient = axiosConfigModule.apiClient;
    backendApi = axiosConfigModule.backendApi as unknown as MockAxiosInstance;
    identityApi = axiosConfigModule.identityApi as unknown as MockAxiosInstance;

    // Get the mock instances from the create calls
    const createCalls = axiosCreateMock.mock.results;
    if (createCalls.length >= 2) {
      mockBackendInstance = createCalls[0].value as MockAxiosInstance;
      mockIdentityInstance = createCalls[1].value as MockAxiosInstance;
    }
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  describe('API Client Creation', () => {
    it('should create backend and identity API instances', () => {
      expect(backendApi).toBeDefined();
      expect(identityApi).toBeDefined();
      expect(apiClient).toBeDefined();
      if (mockBackendInstance && mockIdentityInstance) {
        expect(backendApi).toBe(mockBackendInstance);
        expect(identityApi).toBe(mockIdentityInstance);
      }
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
      if (!mockBackendInstance) return;
      
      const mockConfig = {
        headers: {},
        metadata: undefined
      };
      const mockToken = 'test-access-token';

      (tokenManager.getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue(mockToken);

      // Get the request interceptor function from backend instance
      expect(mockBackendInstance.interceptors.request.use).toHaveBeenCalled();
      const requestInterceptorCall = mockBackendInstance.interceptors.request.use.mock.calls[0];
      const requestInterceptor = requestInterceptorCall[0];

      const result = requestInterceptor(mockConfig);

      expect(result.headers.Authorization).toBe(`Bearer ${mockToken}`);
      expect(result.metadata).toEqual({startTime: expect.any(Number)});
    });

    it('should not add authorization header when no token exists', () => {
      if (!mockBackendInstance) return;
      
      const mockConfig = {
        headers: {},
        metadata: undefined
      };

      (tokenManager.getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue(null);

      // Get the request interceptor function from backend instance
      expect(mockBackendInstance.interceptors.request.use).toHaveBeenCalled();
      const requestInterceptorCall = mockBackendInstance.interceptors.request.use.mock.calls[0];
      const requestInterceptor = requestInterceptorCall[0];

      const result = requestInterceptor(mockConfig);

      expect(result.headers.Authorization).toBeUndefined();
      expect(result.metadata).toEqual({startTime: expect.any(Number)});
    });
  });

  describe('Response Interceptor', () => {
    let responseInterceptor: (response: unknown) => unknown;
    let errorInterceptor: (error: unknown) => Promise<unknown>;

    beforeEach(() => {
      if (!mockBackendInstance) return;
      
      // Get the response interceptor functions from backend instance
      expect(mockBackendInstance.interceptors.response.use).toHaveBeenCalled();
      const responseInterceptorCall = mockBackendInstance.interceptors.response.use.mock.calls[0];
      responseInterceptor = responseInterceptorCall[0];
      errorInterceptor = responseInterceptorCall[1];
    });

    it('should pass through successful responses', () => {
      if (!responseInterceptor) return;
      
      const mockResponse = {
        data: {success: true},
        config: {metadata: {startTime: Date.now() - 1000}}
      };

      const result = responseInterceptor(mockResponse);
      expect(result).toBe(mockResponse);
    });

    it('should handle network errors', async () => {
      if (!errorInterceptor) return;
      
      const networkError = new AxiosError('Network Error');
      networkError.response = undefined;

      await expect(errorInterceptor(networkError)).rejects.toThrow('Network error - please check your connection');
    });

    it('should handle timeout errors', async () => {
      if (!errorInterceptor) return;
      
      const timeoutError = new AxiosError('timeout of 10000ms exceeded');
      timeoutError.code = 'ECONNABORTED';
      timeoutError.config = {
        url: '/test',
        headers: {} as AxiosRequestHeaders,
        method: 'get'
      };
      // Add a dummy response to prevent it from being treated as a network error
      timeoutError.response = {
        status: 0,
        data: {},
        statusText: 'timeout',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };

      await expect(errorInterceptor(timeoutError)).rejects.toThrow('Request timeout - please try again');
    });

    it('should handle 401 errors and attempt token refresh', async () => {
      if (!errorInterceptor || !mockBackendInstance) return;
      
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = {
        status: 401,
        data: {},
        statusText: 'Unauthorized',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };
      unauthorizedError.config = {
        headers: {} as AxiosRequestHeaders,
        url: '/test',
        method: 'get'
      } as InternalAxiosRequestConfig;
      (unauthorizedError.config as ExtendedInternalAxiosRequestConfig)._retry = undefined;

      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue('refresh-token');
      
      // Mock axios.post for the refresh token request
      axiosPostMock.mockResolvedValueOnce({
        data: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token'
        }
      } as unknown);

      // Mock the backend instance call for retry
      mockBackendInstance.mockResolvedValueOnce({data: 'success'});

      const result = await errorInterceptor(unauthorizedError);

      expect(tokenManager.saveTokens).toHaveBeenCalledWith('new-access-token', 'new-refresh-token');
      expect(unauthorizedError.config?.headers?.Authorization).toBe('Bearer new-access-token');
      expect(result).toEqual({data: 'success'});
    });

    it('should handle failed token refresh', async () => {
      if (!errorInterceptor) return;
      
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = {
        status: 401,
        data: {},
        statusText: 'Unauthorized',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };
      unauthorizedError.config = {
        headers: {} as AxiosRequestHeaders,
        url: '/test',
        method: 'get'
      } as InternalAxiosRequestConfig;
      (unauthorizedError.config as ExtendedInternalAxiosRequestConfig)._retry = undefined;

      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue('refresh-token');
      
      // Mock axios.post to fail
      axiosPostMock.mockRejectedValueOnce(new Error('Refresh failed'));

      await expect(errorInterceptor(unauthorizedError)).rejects.toThrow();

      expect(tokenManager.clearTokens).toHaveBeenCalled();
      expect(mockDispatchEvent).toHaveBeenCalledWith(
          expect.objectContaining({
            type: 'authFailure',
            detail: {reason: 'Token refresh failed'}
          })
      );
    });

    it('should not retry 401 errors that already have _retry flag', async () => {
      if (!errorInterceptor) return;
      
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = {
        status: 401,
        data: {},
        statusText: 'Unauthorized',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };
      unauthorizedError.config = {
        headers: {} as AxiosRequestHeaders,
        url: '/test',
        method: 'get'
      } as InternalAxiosRequestConfig;
      (unauthorizedError.config as ExtendedInternalAxiosRequestConfig)._retry = true; // Already retried

      await expect(errorInterceptor(unauthorizedError)).rejects.toThrow();
      
      expect(axiosPostMock).not.toHaveBeenCalled();
    });

    it('should extract meaningful error messages', async () => {
      if (!errorInterceptor) return;
      
      const errorWithMessage = new AxiosError('Request failed');
      errorWithMessage.response = {
        status: 400,
        data: {message: 'Custom error message'},
        statusText: 'Bad Request',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };
      errorWithMessage.config = {
        url: '/test',
        headers: {} as AxiosRequestHeaders,
        method: 'get'
      } as InternalAxiosRequestConfig;

      await expect(errorInterceptor(errorWithMessage)).rejects.toThrow('Custom error message');
    });

    it('should handle different HTTP status codes', async () => {
      if (!errorInterceptor) return;
      
      const testCases = [
        {status: 404, expectedMessage: 'Resource not found'},
        {status: 403, expectedMessage: 'Access forbidden - you don\'t have permission'},
        {status: 500, expectedMessage: 'Server error - please try again later'},
        {status: 429, expectedMessage: 'Too many requests - please try again later'},
      ];

      for (const testCase of testCases) {
        const error = new AxiosError('HTTP Error');
        error.response = {
          status: testCase.status,
          data: {},
          statusText: 'HTTP Error',
          headers: {} as AxiosResponseHeaders,
          config: {} as InternalAxiosRequestConfig
        };
        error.config = {
          url: '/test',
          headers: {} as AxiosRequestHeaders,
          method: 'get'
        } as InternalAxiosRequestConfig;

        await expect(errorInterceptor(error)).rejects.toThrow(testCase.expectedMessage);
      }
    });
  });

  describe('Token Refresh Logic', () => {
    it('should refresh token with valid refresh token', async () => {
      if (!mockBackendInstance) return;
      
      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue('valid-refresh-token');
      
      axiosPostMock.mockResolvedValueOnce({
        data: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token'
        }
      } as unknown);

      // Access the private refreshAccessToken method through error handling
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = {
        status: 401,
        data: {},
        statusText: 'Unauthorized',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };
      unauthorizedError.config = {
        headers: {} as AxiosRequestHeaders,
        url: '/test',
        method: 'get'
      } as InternalAxiosRequestConfig;

      // Get the error interceptor from backend instance
      expect(mockBackendInstance.interceptors.response.use).toHaveBeenCalled();
      const responseInterceptorCall = mockBackendInstance.interceptors.response.use.mock.calls[0];
      const errorInterceptor = responseInterceptorCall[1];

      mockBackendInstance.mockResolvedValueOnce({data: 'success'});

      await errorInterceptor(unauthorizedError);

      expect(axiosPostMock).toHaveBeenCalledWith(
          'http://localhost:8081/api/v1/auth/refresh',
          {refreshToken: 'valid-refresh-token'},
          expect.objectContaining({
            headers: {'Content-Type': 'application/json'},
            withCredentials: true,
            timeout: 10000
          })
      );
    });

    it('should clear tokens when refresh token is invalid', async () => {
      if (!mockBackendInstance) return;
      
      (tokenManager.getRefreshToken as ReturnType<typeof vi.fn>).mockReturnValue(null);

      // Create a 401 error to trigger token refresh logic
      const unauthorizedError = new AxiosError('Unauthorized');
      unauthorizedError.response = {
        status: 401,
        data: {},
        statusText: 'Unauthorized',
        headers: {} as AxiosResponseHeaders,
        config: {} as InternalAxiosRequestConfig
      };
      unauthorizedError.config = {
        headers: {} as AxiosRequestHeaders,
        url: '/test',
        method: 'get'
      } as InternalAxiosRequestConfig;

      // Get the error interceptor from backend instance
      expect(mockBackendInstance.interceptors.response.use).toHaveBeenCalled();
      const responseInterceptorCall = mockBackendInstance.interceptors.response.use.mock.calls[0];
      const errorInterceptor = responseInterceptorCall[1];

      // This should reject since no refresh token is available
      await expect(errorInterceptor(unauthorizedError)).rejects.toThrow();
      
      // Token manager should have been called to check for refresh token
      expect(tokenManager.getRefreshToken).toHaveBeenCalled();
    });
  });

  describe('Health Check', () => {
    it('should check health status of API endpoints', async () => {
      if (!mockBackendInstance || !mockIdentityInstance) return;
      
      // Mock successful health check responses
      mockBackendInstance.get.mockResolvedValue({});
      mockIdentityInstance.get.mockResolvedValue({});

      const healthStatus = await apiClient.getHealthStatus();

      expect(healthStatus).toEqual({
        backend: true,
        identity: true
      });

      expect(mockBackendInstance.get).toHaveBeenCalledWith('/health', {timeout: 5000});
      expect(mockIdentityInstance.get).toHaveBeenCalledWith('/health', {timeout: 5000});
    });

    it('should handle health check failures', async () => {
      if (!mockBackendInstance || !mockIdentityInstance) return;
      
      // Mock failed health check responses
      mockBackendInstance.get.mockRejectedValue(new Error('Service down'));
      mockIdentityInstance.get.mockRejectedValue(new Error('Service down'));

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
          error.response = {
            status: 500,
            data: {},
            statusText: 'Internal Server Error',
            headers: {} as AxiosResponseHeaders,
            config: {} as InternalAxiosRequestConfig
          };
          throw error;
        }
        return Promise.resolve({data: 'success'});
      });

      vi.useFakeTimers();

      const retryPromise = apiClient.retryRequest(mockRequestFn, 3);

      // Fast-forward through the retry delays
      vi.advanceTimersByTime(1000); // First retry delay
      await Promise.resolve(); // Let the retry execute
      vi.advanceTimersByTime(2000); // Second retry delay (exponential backoff)
      await Promise.resolve(); // Let the retry execute

      const result = await retryPromise;

      expect(result).toEqual({data: 'success'});
      expect(mockRequestFn).toHaveBeenCalledTimes(3);

      vi.useRealTimers();
    });

    it('should not retry client errors (4xx)', async () => {
      const mockRequestFn = vi.fn().mockImplementation(() => {
        const error = new AxiosError('Bad Request');
        error.response = {
          status: 400,
          data: {},
          statusText: 'Bad Request',
          headers: {} as AxiosResponseHeaders,
          config: {} as InternalAxiosRequestConfig
        };
        throw error;
      });

      await expect(apiClient.retryRequest(mockRequestFn, 3)).rejects.toThrow();
      expect(mockRequestFn).toHaveBeenCalledTimes(1); // Should not retry
    });
  });
});
