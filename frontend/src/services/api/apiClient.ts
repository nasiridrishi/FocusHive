/**
 * API Client with Authentication & Token Management
 *
 * Core API client that handles:
 * - JWT token management and auto-refresh
 * - Request/response interceptors
 * - Error handling and retries
 * - Service-specific configurations
 */

import { getServiceUrls, REQUEST_CONFIG, SECURITY_CONFIG } from '@/services/config/services.config';

interface ApiClientOptions {
  baseURL?: string;
  timeout?: number;
  headers?: Record<string, string>;
}

interface RequestConfig extends RequestInit {
  params?: Record<string, string | number | boolean>;
  timeout?: number;
  skipAuth?: boolean;
  retryAttempts?: number;
}

/**
 * Token Manager Class
 * Handles JWT token storage, validation, and refresh
 */
export class TokenManager {
  private static instance: TokenManager | null = null;
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private refreshPromise: Promise<string> | null = null;

  private constructor() {
    this.loadTokens();
  }

  static getInstance(): TokenManager {
    if (!TokenManager.instance) {
      TokenManager.instance = new TokenManager();
    }
    return TokenManager.instance;
  }

  /**
   * Load tokens from localStorage
   */
  private loadTokens(): void {
    this.accessToken = localStorage.getItem(SECURITY_CONFIG.ACCESS_TOKEN_KEY);
    this.refreshToken = localStorage.getItem(SECURITY_CONFIG.REFRESH_TOKEN_KEY);
  }

  /**
   * Save tokens to localStorage
   */
  saveTokens(accessToken: string, refreshToken: string): void {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    localStorage.setItem(SECURITY_CONFIG.ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(SECURITY_CONFIG.REFRESH_TOKEN_KEY, refreshToken);
  }

  /**
   * Clear all tokens
   */
  clearTokens(): void {
    this.accessToken = null;
    this.refreshToken = null;
    this.refreshPromise = null;
    localStorage.removeItem(SECURITY_CONFIG.ACCESS_TOKEN_KEY);
    localStorage.removeItem(SECURITY_CONFIG.REFRESH_TOKEN_KEY);
    localStorage.removeItem(SECURITY_CONFIG.USER_ID_KEY);
    localStorage.removeItem(SECURITY_CONFIG.PERSONA_ID_KEY);
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationTime = payload.exp * 1000;
      const currentTime = Date.now();
      const threshold = SECURITY_CONFIG.TOKEN_REFRESH_THRESHOLD;

      // Refresh if within threshold of expiration
      return expirationTime - currentTime < threshold;
    } catch (error) {
      console.error('Error parsing token:', error);
      return true;
    }
  }

  /**
   * Get valid access token (refresh if needed)
   */
  async getValidToken(): Promise<string | null> {
    // No tokens available
    if (!this.accessToken || !this.refreshToken) {
      return null;
    }

    // Token is still valid
    if (!this.isTokenExpired(this.accessToken)) {
      return this.accessToken;
    }

    // Already refreshing
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    // Need to refresh
    this.refreshPromise = this.refreshAccessToken()
      .finally(() => {
        this.refreshPromise = null;
      });

    return this.refreshPromise;
  }

  /**
   * Refresh the access token
   */
  private async refreshAccessToken(): Promise<string> {
    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }

    const services = getServiceUrls();
    const url = `${services.IDENTITY}/api/v1/auth/refresh`;

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.refreshToken}`,
        },
      });

      if (!response.ok) {
        throw new Error('Token refresh failed');
      }

      const data = await response.json();
      this.saveTokens(data.accessToken, data.refreshToken || this.refreshToken);

      return data.accessToken;
    } catch (error) {
      // Clear tokens on refresh failure
      this.clearTokens();
      // Redirect to login
      window.location.href = '/login';
      throw error;
    }
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRefreshToken(): string | null {
    return this.refreshToken;
  }
}

/**
 * Base API Client Class
 */
export class ApiClient {
  private baseURL: string;
  private timeout: number;
  private defaultHeaders: Record<string, string>;
  private tokenManager: TokenManager;

  constructor(options: ApiClientOptions = {}) {
    const services = getServiceUrls();
    this.baseURL = options.baseURL || services.BACKEND;
    this.timeout = options.timeout || REQUEST_CONFIG.DEFAULT_TIMEOUT;
    this.defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...options.headers,
    };
    this.tokenManager = TokenManager.getInstance();
  }

  /**
   * Build URL with query parameters
   */
  private buildURL(endpoint: string, params?: Record<string, string | number | boolean>): string {
    const url = new URL(`${this.baseURL}${endpoint}`);

    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          url.searchParams.append(key, String(value));
        }
      });
    }

    return url.toString();
  }

  /**
   * Execute request with retries and error handling
   */
  private async executeRequest(
    url: string,
    config: RequestConfig,
    retryCount: number = 0
  ): Promise<Response> {
    const controller = new AbortController();
    const timeout = config.timeout || this.timeout;

    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(url, {
        ...config,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      // Handle 401 Unauthorized
      if (response.status === 401 && !config.skipAuth && retryCount === 0) {
        const newToken = await this.tokenManager.getValidToken();
        if (newToken) {
          config.headers = {
            ...config.headers,
            'Authorization': `Bearer ${newToken}`,
          };
          return this.executeRequest(url, config, 1);
        }
      }

      // Handle rate limiting
      if (response.status === 429) {
        const retryAfter = response.headers.get('Retry-After');
        const delay = retryAfter ? parseInt(retryAfter) * 1000 : REQUEST_CONFIG.RETRY_DELAY;
        await this.delay(delay);
        return this.executeRequest(url, config, retryCount + 1);
      }

      // Handle server errors with retry
      if (response.status >= 500 && retryCount < (config.retryAttempts || REQUEST_CONFIG.RETRY_ATTEMPTS)) {
        const delay = REQUEST_CONFIG.EXPONENTIAL_BACKOFF
          ? REQUEST_CONFIG.RETRY_DELAY * Math.pow(2, retryCount)
          : REQUEST_CONFIG.RETRY_DELAY;
        await this.delay(delay);
        return this.executeRequest(url, config, retryCount + 1);
      }

      return response;
    } catch (error) {
      clearTimeout(timeoutId);

      // Network error or timeout - retry if applicable
      if (retryCount < (config.retryAttempts || REQUEST_CONFIG.RETRY_ATTEMPTS)) {
        const delay = REQUEST_CONFIG.EXPONENTIAL_BACKOFF
          ? REQUEST_CONFIG.RETRY_DELAY * Math.pow(2, retryCount)
          : REQUEST_CONFIG.RETRY_DELAY;
        await this.delay(delay);
        return this.executeRequest(url, config, retryCount + 1);
      }

      throw error;
    }
  }

  /**
   * Delay helper for retries
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Main request method
   */
  async request<T = any>(
    endpoint: string,
    options: RequestConfig = {}
  ): Promise<T> {
    const url = this.buildURL(endpoint, options.params);

    // Build headers
    const headers: Record<string, string> = {
      ...this.defaultHeaders,
      ...(options.headers as Record<string, string> || {}),
    };

    // Add auth header if not skipped
    if (!options.skipAuth) {
      const token = await this.tokenManager.getValidToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    // Prepare request config
    const config: RequestConfig = {
      ...options,
      headers,
      credentials: 'include',
    };

    // Add body if present
    if (options.body && typeof options.body !== 'string') {
      config.body = JSON.stringify(options.body);
    }

    // Execute request
    const response = await this.executeRequest(url, config);

    // Handle response
    if (!response.ok) {
      const error = await this.parseError(response);
      throw error;
    }

    // Parse JSON response
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      return response.json();
    }

    // Return text for non-JSON responses
    return response.text() as T;
  }

  /**
   * Parse error response
   */
  private async parseError(response: Response): Promise<Error> {
    try {
      const errorData = await response.json();
      const error = new Error(errorData.message || `Request failed with status ${response.status}`);
      (error as any).status = response.status;
      (error as any).data = errorData;
      return error;
    } catch {
      const error = new Error(`Request failed with status ${response.status}`);
      (error as any).status = response.status;
      return error;
    }
  }

  /**
   * Convenience methods
   */
  async get<T = any>(endpoint: string, options: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'GET' });
  }

  async post<T = any>(endpoint: string, data?: any, options: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'POST', body: data });
  }

  async put<T = any>(endpoint: string, data?: any, options: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'PUT', body: data });
  }

  async patch<T = any>(endpoint: string, data?: any, options: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'PATCH', body: data });
  }

  async delete<T = any>(endpoint: string, options: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'DELETE' });
  }
}

/**
 * Service-specific API clients
 */
export function createServiceClients() {
  const services = getServiceUrls();

  return {
    identity: new ApiClient({ baseURL: services.IDENTITY }),
    backend: new ApiClient({ baseURL: services.BACKEND }),
    notification: new ApiClient({ baseURL: services.NOTIFICATION }),
    buddy: new ApiClient({ baseURL: services.BUDDY }),
  };
}

// Export singleton instances
const clients = createServiceClients();

export const identityAPI = clients.identity;
export const backendAPI = clients.backend;
export const notificationAPI = clients.notification;
export const buddyAPI = clients.buddy;

// Default export
export default ApiClient;