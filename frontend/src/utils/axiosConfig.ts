/**
 * Enhanced Axios Configuration with JWT Token Management
 *
 * Implements comprehensive request/response interceptors with:
 * - Automatic token injection
 * - Token refresh on 401 responses
 * - Request queuing during token refresh
 * - Failed request retry after successful refresh
 * - Error handling and logging
 * - Multiple API endpoint support
 */

import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios';
import {tokenManager} from './tokenManager';

interface QueuedRequest {
  resolve: (value: AxiosResponse) => void;
  reject: (error: AxiosError) => void;
  config: AxiosRequestConfig;
}

interface RefreshTokenResponse {
  token: string;
  refreshToken: string;
}

class ApiClient {
  // Axios instances for different services
  public readonly backend: AxiosInstance;
  public readonly identity: AxiosInstance;
  private isRefreshing = false;
  private failedQueue: QueuedRequest[] = [];
  private maxRetries = 3;
  private retryDelay = 1000; // 1 second base delay
  // API endpoints configuration
  private readonly apiEndpoints = {
    backend: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    identity: import.meta.env.VITE_IDENTITY_API_URL || 'http://localhost:8081'
  };

  constructor() {
    // Create backend API instance
    this.backend = this.createApiInstance(this.apiEndpoints.backend, '/api');

    // Create identity service instance
    this.identity = this.createApiInstance(this.apiEndpoints.identity, '/api');

    // Set up interceptors for both instances
    this.setupInterceptors(this.backend);
    this.setupInterceptors(this.identity);
  }

  /**
   * Add retry logic for failed requests (excluding auth failures)
   */
  async retryRequest<T>(
      requestFn: () => Promise<T>,
      maxRetries: number = this.maxRetries
  ): Promise<T> {
    let lastError: Error;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await requestFn();
      } catch (error) {
        lastError = error as Error;

        // Don't retry auth errors or client errors (4xx)
        if (error instanceof AxiosError) {
          const status = error.response?.status;
          if (status && (status >= 400 && status < 500)) {
            throw error;
          }
        }

        // If this isn't the last attempt, wait before retrying
        if (attempt < maxRetries) {
          const delay = this.retryDelay * Math.pow(2, attempt - 1); // Exponential backoff
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }
    }

    throw lastError || new Error('All retry attempts failed');
  }

  /**
   * Get health status of API endpoints
   */
  async getHealthStatus(): Promise<{ backend: boolean; identity: boolean }> {
    const checkHealth = async (instance: AxiosInstance): Promise<boolean> => {
      try {
        await instance.get('/health', {timeout: 5000});
        return true;
      } catch {
        return false;
      }
    };

    const [backendHealth, identityHealth] = await Promise.allSettled([
      checkHealth(this.backend),
      checkHealth(this.identity)
    ]);

    return {
      backend: backendHealth.status === 'fulfilled' ? backendHealth.value : false,
      identity: identityHealth.status === 'fulfilled' ? identityHealth.value : false
    };
  }

  /**
   * Create configured axios instance for a specific service
   */
  private createApiInstance(baseURL: string, apiPath: string = ''): AxiosInstance {
    const instance = axios.create({
      baseURL: `${baseURL}${apiPath}`,
      timeout: 10000, // 10 second timeout
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      withCredentials: true, // Enable cookies for httpOnly refresh tokens
      validateStatus: (status) => status < 500 // Don't throw on 4xx errors
    });

    return instance;
  }

  /**
   * Set up request and response interceptors for an axios instance
   */
  private setupInterceptors(instance: AxiosInstance): void {
    // Request interceptor - Add authorization header
    instance.interceptors.request.use(
        (config: InternalAxiosRequestConfig) => {
          const token = tokenManager.getAccessToken();

          if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
          }

          // Add request timestamp for timeout tracking
          config.metadata = {startTime: Date.now()};

          return config;
        },
        (error: AxiosError) => {
          // Debug statement removed
          return Promise.reject(error);
        }
    );

    // Response interceptor - Handle token refresh and retries
    instance.interceptors.response.use(
        (response: AxiosResponse) => {
          // Log successful requests in development
          if (import.meta.env.DEV) {
            const _duration = Date.now() - (response.config.metadata?.startTime || 0);
            // console.debug(`API Success: ${response.config.method?.toUpperCase()} ${response.config.url} (${_duration}ms)`);
          }
          return response;
        },
        async (error: AxiosError) => {
          const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

          // Handle network errors
          if (!error.response) {
            // Debug statement removed
            return Promise.reject(new Error('Network error - please check your connection'));
          }

          // Handle timeout errors
          if (error.code === 'ECONNABORTED') {
            // Debug statement removed
            return Promise.reject(new Error('Request timeout - please try again'));
          }

          // Handle 401 Unauthorized - Token refresh logic
          if (error.response.status === 401 && originalRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            // If already refreshing, queue this request
            if (this.isRefreshing) {
              return new Promise((resolve, reject) => {
                this.failedQueue.push({resolve, reject, config: originalRequest});
              });
            }

            this.isRefreshing = true;

            try {
              const newTokens = await this.refreshAccessToken();

              if (newTokens) {
                // Update the authorization header for the failed request
                if (originalRequest.headers) {
                  originalRequest.headers.Authorization = `Bearer ${newTokens.token}`;
                }

                // Process queued requests with new token
                this.processQueue(null, newTokens.token);

                // Retry the original request
                return instance(originalRequest);
              } else {
                // Refresh failed, redirect to login
                this.processQueue(new Error('Token refresh failed'), null);
                this.handleAuthFailure();
                return Promise.reject(error);
              }
            } catch (refreshError) {
              this.processQueue(refreshError as Error, null);
              this.handleAuthFailure();
              return Promise.reject(refreshError);
            } finally {
              this.isRefreshing = false;
            }
          }

          // Handle other HTTP errors
          const errorMessage = this.extractErrorMessage(error);
          // Debug statement removed
          return Promise.reject(new Error(errorMessage));
        }
    );
  }

  /**
   * Refresh access token using refresh token
   */
  private async refreshAccessToken(): Promise<RefreshTokenResponse | null> {
    const refreshToken = tokenManager.getRefreshToken();

    if (!refreshToken) {
      // Debug statement removed
      return null;
    }

    try {
      const response = await axios.post<RefreshTokenResponse>(
          `${this.apiEndpoints.identity}/api/v1/auth/refresh`,
          {refreshToken},
          {
            headers: {'Content-Type': 'application/json'},
            withCredentials: true,
            timeout: 10000
          }
      );

      if (response.data?.token && response.data?.refreshToken) {
        // Update stored tokens
        tokenManager.saveTokens(response.data.token, response.data.refreshToken);
        return response.data;
      }

      return null;
    } catch {
      // Debug statement removed
      tokenManager.clearTokens();
      return null;
    }
  }

  /**
   * Process queued requests after token refresh
   */
  private processQueue(error: Error | null, token: string | null): void {
    this.failedQueue.forEach(({resolve, reject, config}) => {
      if (error) {
        // Convert to AxiosError for consistent error handling
        const axiosError = new Error(error.message) as AxiosError;
        (axiosError as unknown as {isAxiosError: boolean; toJSON: () => {message: string}}).isAxiosError = true;
        (axiosError as unknown as {isAxiosError: boolean; toJSON: () => {message: string}}).toJSON = () => ({message: error.message});
        reject(axiosError);
      } else if (token) {
        // Update authorization header and retry request
        if (config.headers) {
          (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
        }
        // Resolve with the promise result, not the promise itself
        this.backend(config)
          .then(response => resolve(response))
          .catch(err => reject(err));
      }
    });

    this.failedQueue = [];
  }

  /**
   * Handle authentication failure - clear tokens and redirect
   */
  private handleAuthFailure(): void {
    tokenManager.clearTokens();

    // Dispatch event to notify the app about auth failure
    window.dispatchEvent(new CustomEvent('authFailure', {
      detail: {reason: 'Token refresh failed'}
    }));

    // Redirect to login if not already there
    if (!window.location.pathname.includes('/login')) {
      window.location.href = '/login';
    }
  }

  /**
   * Extract meaningful error message from axios error
   */
  private extractErrorMessage(error: AxiosError): string {
    if (error.response?.data) {
      const data = error.response.data as { message?: string; error?: string; errors?: string[] };

      if (data.message) return data.message;
      if (data.error) return data.error;
      if (data.errors && data.errors.length > 0) return data.errors[0];
    }

    switch (error.response?.status) {
      case 400:
        return 'Bad request - please check your input';
      case 401:
        return 'Authentication required - please log in';
      case 403:
        return 'Access forbidden - you don\'t have permission';
      case 404:
        return 'Resource not found';
      case 409:
        return 'Conflict - resource already exists';
      case 422:
        return 'Validation error - please check your input';
      case 429:
        return 'Too many requests - please try again later';
      case 500:
        return 'Server error - please try again later';
      case 502:
        return 'Service temporarily unavailable';
      case 503:
        return 'Service maintenance - please try again later';
      default:
        return error.message || 'An unexpected error occurred';
    }
  }

  /**
   * Check if a request should be retried based on error type
   */
  private shouldRetry(error: AxiosError, attempt: number): boolean {
    if (attempt >= this.maxRetries) return false;

    // Don't retry client errors (4xx)
    if (error.response?.status && error.response.status >= 400 && error.response.status < 500) {
      return false;
    }

    // Retry server errors (5xx) and network errors
    return !error.response || error.response.status >= 500;
  }
}

// Create and export singleton instance
export const apiClient = new ApiClient();

// Export individual instances for convenience
export const {backend: backendApi, identity: identityApi} = apiClient;

// Export default as the main API client
export default apiClient;

// Extend AxiosRequestConfig to include metadata
declare module 'axios' {
  interface AxiosRequestConfig {
    metadata?: {
      startTime: number;
    };
  }
}