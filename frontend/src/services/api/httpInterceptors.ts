import axios, { AxiosInstance, AxiosRequestConfig, AxiosError, AxiosResponse } from 'axios';
import { tokenStorage } from './authApi';

/**
 * HTTP Interceptors Service
 * 
 * Provides centralized HTTP interceptors for:
 * - Automatic JWT token attachment
 * - Token refresh on 401 responses
 * - Request/response logging in development
 * - Error standardization
 * - CORS handling
 * - Request timeout management
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const REQUEST_TIMEOUT = 10000; // 10 seconds

// Create a flag to prevent multiple refresh attempts
let isRefreshing = false;
let failedRequestQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (error?: unknown) => void;
}> = [];

/**
 * Create an authenticated axios instance with interceptors
 */
export function createAuthenticatedAxiosInstance(baseURL: string = API_BASE_URL): AxiosInstance {
  const instance = axios.create({
    baseURL,
    timeout: REQUEST_TIMEOUT,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true, // Enable cookies for future httpOnly cookie support
  });

  // Request interceptor
  instance.interceptors.request.use(
    (config: AxiosRequestConfig) => {
      // Add authentication token if available
      const token = tokenStorage.getAccessToken();
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      // Add correlation ID for request tracking
      const correlationId = generateCorrelationId();
      if (config.headers) {
        config.headers['X-Correlation-ID'] = correlationId;
      }

      // Development logging
      if (import.meta.env.DEV) {
        // HTTP request logged in development only
      }

      return config;
    },
    (error) => {
      // HTTP request error logged by error boundary
      return Promise.reject(error);
    }
  );

  // Response interceptor
  instance.interceptors.response.use(
    (response: AxiosResponse) => {
      // Development logging
      if (import.meta.env.DEV) {
        // HTTP response logged in development only
      }

      return response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config;
      const _correlationId = originalRequest?.headers?.['X-Correlation-ID'];

      // Development logging
      if (import.meta.env.DEV) {
        // HTTP response error logged in development only
      }

      // Handle 401 Unauthorized - attempt token refresh
      if (error.response?.status === 401 && originalRequest && !originalRequest._isRetry) {
        if (isRefreshing) {
          // If a refresh is already in progress, queue this request
          return new Promise((resolve, reject) => {
            failedRequestQueue.push({ resolve, reject });
          }).then(() => {
            // Update the authorization header and retry
            const newToken = tokenStorage.getAccessToken();
            if (newToken && originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
            }
            return instance(originalRequest);
          }).catch((refreshError) => {
            return Promise.reject(refreshError);
          });
        }

        originalRequest._isRetry = true;
        isRefreshing = true;

        try {
          const newTokens = await refreshTokenRequest();
          
          // Update stored tokens
          tokenStorage.setAccessToken(newTokens.token);
          tokenStorage.setRefreshToken(newTokens.refreshToken);

          // Process queued requests
          failedRequestQueue.forEach(({ resolve }) => resolve());
          failedRequestQueue = [];

          // Update the authorization header and retry the original request
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newTokens.token}`;
          }

          return instance(originalRequest);
        } catch (refreshError) {
          // Refresh failed, clear tokens and reject queued requests
          failedRequestQueue.forEach(({ reject }) => reject(refreshError));
          failedRequestQueue = [];
          
          tokenStorage.clearAllTokens();
          
          // Redirect to login if in browser environment
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
          
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }

      // Standardize error response
      const standardizedError = standardizeError(error);
      return Promise.reject(standardizedError);
    }
  );

  return instance;
}

/**
 * Refresh token request (separate from auth API to avoid circular dependency)
 */
async function refreshTokenRequest(): Promise<{ token: string; refreshToken: string }> {
  const refreshToken = tokenStorage.getRefreshToken();
  
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  const response = await axios.post<{ token: string; refreshToken: string }>(
    `${API_BASE_URL}/api/auth/refresh`,
    { refreshToken },
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: REQUEST_TIMEOUT,
      withCredentials: true
    }
  );

  return response.data;
}

/**
 * Standardize error responses for consistent error handling
 */
function standardizeError(error: AxiosError): StandardizedError {
  const standardized: StandardizedError = {
    message: 'An unexpected error occurred',
    status: 500,
    code: 'UNKNOWN_ERROR',
    correlationId: error.config?.headers?.['X-Correlation-ID'] as string,
    originalError: error
  };

  if (error.response) {
    // Server responded with error status
    standardized.status = error.response.status;
    standardized.message = getErrorMessage(error.response.data);
    standardized.code = getErrorCode(error.response.status, error.response.data);
  } else if (error.request) {
    // Network error
    standardized.status = 0;
    standardized.message = 'Network error. Please check your connection.';
    standardized.code = 'NETWORK_ERROR';
  } else {
    // Request setup error
    standardized.message = error.message || 'Request configuration error';
    standardized.code = 'REQUEST_ERROR';
  }

  return standardized;
}

/**
 * Extract error message from response data
 */
function getErrorMessage(data: unknown): string {
  if (typeof data === 'string') return data;
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  if (data?.detail) return data.detail;
  return 'An error occurred';
}

/**
 * Get standardized error code based on status and response
 */
function getErrorCode(status: number, data: unknown): string {
  // Check if response includes specific error code
  if (data?.code) return data.code;
  
  // Default codes based on HTTP status
  switch (status) {
    case 400: return 'BAD_REQUEST';
    case 401: return 'UNAUTHORIZED';
    case 403: return 'FORBIDDEN';
    case 404: return 'NOT_FOUND';
    case 409: return 'CONFLICT';
    case 422: return 'VALIDATION_ERROR';
    case 429: return 'TOO_MANY_REQUESTS';
    case 500: return 'INTERNAL_SERVER_ERROR';
    case 502: return 'BAD_GATEWAY';
    case 503: return 'SERVICE_UNAVAILABLE';
    case 504: return 'GATEWAY_TIMEOUT';
    default: return 'HTTP_ERROR';
  }
}

/**
 * Generate correlation ID for request tracking
 */
function generateCorrelationId(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Standardized error interface
 */
export interface StandardizedError extends Error {
  status: number;
  code: string;
  correlationId?: string;
  originalError: AxiosError;
}

/**
 * Create application-wide axios instance with interceptors
 */
export const apiClient = createAuthenticatedAxiosInstance();

/**
 * Create service-specific axios instances
 */
export const createServiceAxiosInstance = (servicePath: string) => {
  return createAuthenticatedAxiosInstance(`${API_BASE_URL}/api/${servicePath}`);
};

// Extend AxiosRequestConfig to include retry flag
declare module 'axios' {
  interface AxiosRequestConfig {
    _isRetry?: boolean;
  }
}

export default apiClient;