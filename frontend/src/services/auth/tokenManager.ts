/**
 * Token Manager Service
 *
 * Handles JWT token storage, retrieval, and refresh logic.
 * Works with real Identity Service for token refresh.
 *
 * PRODUCTION REQUIREMENTS:
 * - No mocks or stubs
 * - Real JWT token handling
 * - Secure storage management
 * - Automatic token refresh
 */

import axios, { AxiosInstance } from 'axios';
import type { AuthTokens, User } from '../../contracts/auth';

interface TokenPayload {
  sub: string;
  email: string;
  exp: number;
  iat: number;
  roles?: string[];
}

class TokenManager {
  private static instance: TokenManager;
  private refreshTimer: NodeJS.Timeout | null = null;
  private readonly TOKEN_REFRESH_BUFFER = 60000; // Refresh 1 minute before expiry
  private axiosInstance: AxiosInstance;
  private isRefreshing = false;
  private refreshPromise: Promise<AuthTokens> | null = null;

  // Storage keys
  private readonly ACCESS_TOKEN_KEY = 'auth_access_token';
  private readonly REFRESH_TOKEN_KEY = 'auth_refresh_token';
  private readonly USER_KEY = 'auth_user';

  private constructor() {
    // Use environment variable or default to localhost
    const identityServiceUrl = import.meta.env.VITE_IDENTITY_API_URL || 'http://localhost:8081';

    this.axiosInstance = axios.create({
      baseURL: identityServiceUrl,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      withCredentials: true
    });

    // Initialize auto-refresh if tokens exist
    this.initializeAutoRefresh();
  }

  /**
   * Get singleton instance
   */
  public static getInstance(): TokenManager {
    if (!TokenManager.instance) {
      TokenManager.instance = new TokenManager();
    }
    return TokenManager.instance;
  }

  /**
   * Store tokens in appropriate storage
   */
  public storeTokens(tokens: AuthTokens, rememberMe = false): void {
    const storage = rememberMe ? localStorage : sessionStorage;

    storage.setItem(this.ACCESS_TOKEN_KEY, tokens.accessToken);
    storage.setItem(this.REFRESH_TOKEN_KEY, tokens.refreshToken);

    // Schedule auto-refresh
    this.scheduleTokenRefresh(tokens.accessToken);
  }

  /**
   * Store user information
   */
  public storeUser(user: User, rememberMe = false): void {
    const storage = rememberMe ? localStorage : sessionStorage;
    storage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  /**
   * Get access token from storage
   */
  public getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY) ||
           sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Get refresh token from storage
   */
  public getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY) ||
           sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Get stored user
   */
  public getUser(): User | null {
    const userStr = localStorage.getItem(this.USER_KEY) ||
                   sessionStorage.getItem(this.USER_KEY);

    if (!userStr) return null;

    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }

  /**
   * Check if user is authenticated
   */
  public isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;

    // Check if token is expired
    const payload = this.decodeToken(token);
    if (!payload) return false;

    const now = Date.now() / 1000;
    return payload.exp > now;
  }

  /**
   * Clear all auth data
   */
  public clearAuth(): void {
    // Clear from both storages
    [localStorage, sessionStorage].forEach(storage => {
      storage.removeItem(this.ACCESS_TOKEN_KEY);
      storage.removeItem(this.REFRESH_TOKEN_KEY);
      storage.removeItem(this.USER_KEY);
    });

    // Cancel refresh timer
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  /**
   * Refresh access token using refresh token
   */
  public async refreshAccessToken(): Promise<AuthTokens> {
    // If already refreshing, return the existing promise
    if (this.isRefreshing && this.refreshPromise) {
      return this.refreshPromise;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    this.isRefreshing = true;
    this.refreshPromise = this.performTokenRefresh(refreshToken);

    try {
      const tokens = await this.refreshPromise;
      this.isRefreshing = false;
      this.refreshPromise = null;
      return tokens;
    } catch (error) {
      this.isRefreshing = false;
      this.refreshPromise = null;
      throw error;
    }
  }

  /**
   * Perform the actual token refresh
   */
  private async performTokenRefresh(refreshToken: string): Promise<AuthTokens> {
    try {
      const response = await this.axiosInstance.post<{ tokens: AuthTokens }>('/api/v1/auth/refresh', {
        refreshToken
      });

      const { tokens } = response.data;

      // Determine if we should use localStorage or sessionStorage
      const useLocalStorage = localStorage.getItem(this.REFRESH_TOKEN_KEY) !== null;
      this.storeTokens(tokens, useLocalStorage);

      return tokens;
    } catch (error: any) {
      // If refresh fails, clear auth
      this.clearAuth();

      const errorData = error.response?.data;
      throw {
        code: errorData?.code || 'TOKEN_REFRESH_FAILED',
        message: errorData?.message || 'Failed to refresh token',
        statusCode: error.response?.status
      };
    }
  }

  /**
   * Decode JWT token
   */
  private decodeToken(token: string): TokenPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;

      const payload = parts[1];
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded);
    } catch {
      return null;
    }
  }

  /**
   * Schedule automatic token refresh
   */
  private scheduleTokenRefresh(accessToken: string): void {
    // Cancel existing timer
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    const payload = this.decodeToken(accessToken);
    if (!payload) return;

    const now = Date.now();
    const expiresAt = payload.exp * 1000; // Convert to milliseconds
    const refreshAt = expiresAt - this.TOKEN_REFRESH_BUFFER;
    const delay = Math.max(0, refreshAt - now);

    if (delay > 0) {
      this.refreshTimer = setTimeout(async () => {
        try {
          await this.refreshAccessToken();
          console.log('Token refreshed successfully');
        } catch (error) {
          console.error('Failed to refresh token:', error);
          // Token refresh failed, user needs to login again
          this.clearAuth();
          // Dispatch custom event for app to handle
          window.dispatchEvent(new CustomEvent('auth:token-expired'));
        }
      }, delay);
    }
  }

  /**
   * Initialize auto-refresh on startup
   */
  private initializeAutoRefresh(): void {
    const accessToken = this.getAccessToken();
    if (accessToken && this.isAuthenticated()) {
      this.scheduleTokenRefresh(accessToken);
    }
  }

  /**
   * Get authorization header
   */
  public getAuthHeader(): { Authorization: string } | {} {
    const token = this.getAccessToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  /**
   * Handle 401 responses by refreshing token
   */
  public async handle401(): Promise<boolean> {
    try {
      await this.refreshAccessToken();
      return true; // Token refreshed successfully
    } catch {
      return false; // Refresh failed, user needs to login
    }
  }
}

// Export singleton instance
export const tokenManager = TokenManager.getInstance();

// Export type for testing
export type { TokenManager };