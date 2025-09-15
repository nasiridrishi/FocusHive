/**
 * Authentication Service
 * Production-ready service for managing authentication with Identity Service
 *
 * This service provides:
 * - User registration and login
 * - Token management with auto-refresh
 * - Secure token storage
 * - Profile and preference management
 * - Real integration with Identity Service at port 8081
 */

import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import type {
  User,
  AuthTokens,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  AuthError,
  AuthErrorCode,
  TokenValidation,
  PasswordResetRequest,
  PasswordResetConfirmation,
  EmailVerificationRequest,
  EmailVerificationResponse,
  UserProfile,
  UserPreferences,
  TokenStorage,
  AuthConfig
} from '../../contracts/auth';

/**
 * Default authentication configuration
 */
const DEFAULT_CONFIG: Partial<AuthConfig> = {
  tokenRefreshBuffer: 60000, // Refresh 1 minute before expiry
  sessionTimeout: 30 * 60 * 1000, // 30 minutes idle timeout
  rememberMeDuration: 30 * 24 * 60 * 60 * 1000, // 30 days
  maxLoginAttempts: 5,
  lockoutDuration: 15 * 60 * 1000, // 15 minutes
  passwordMinLength: 8,
  passwordRequireSpecialChar: true,
  passwordRequireNumber: true,
  passwordRequireUpperCase: true
};

/**
 * Token storage keys
 */
const STORAGE_KEYS = {
  ACCESS_TOKEN: 'auth_access_token',
  REFRESH_TOKEN: 'auth_refresh_token',
  USER: 'auth_user',
  TOKEN_EXPIRY: 'auth_token_expiry',
  REMEMBER_ME: 'auth_remember_me'
} as const;

/**
 * Auth service configuration options
 */
export interface AuthServiceConfig {
  baseUrl: string;
  tokenStorage: 'localStorage' | 'sessionStorage' | 'memory';
  autoRefresh: boolean;
  refreshBuffer?: number;
  onAuthError?: (error: AuthError) => void;
  onTokenRefresh?: (tokens: AuthTokens) => void;
}

/**
 * Authentication Service
 */
export class AuthService implements TokenStorage {
  private axiosInstance: AxiosInstance;
  private config: AuthServiceConfig & Partial<AuthConfig>;
  private refreshTimer?: NodeJS.Timeout;
  private isRefreshing = false;
  private refreshPromise?: Promise<AuthTokens>;
  private memoryStorage: Map<string, string> = new Map();
  private storage: Storage | Map<string, string>;

  constructor(config: AuthServiceConfig) {
    this.config = { ...DEFAULT_CONFIG, ...config };

    // Set up storage based on configuration
    this.storage = this.getStorageProvider();

    // Create axios instance with base configuration
    this.axiosInstance = axios.create({
      baseURL: config.baseUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      },
      withCredentials: false // Don't send cookies to avoid CORS credential issues
    });

    // Set up request interceptor to add auth token
    this.setupRequestInterceptor();

    // Set up response interceptor for token refresh
    this.setupResponseInterceptor();

    // Start auto-refresh if enabled and tokens exist
    if (config.autoRefresh && this.getAccessToken()) {
      this.scheduleTokenRefresh();
    }
  }

  /**
   * Get storage provider based on configuration
   */
  private getStorageProvider(): Storage | Map<string, string> {
    switch (this.config.tokenStorage) {
      case 'localStorage':
        return localStorage;
      case 'sessionStorage':
        return sessionStorage;
      case 'memory':
        return this.memoryStorage;
      default:
        return localStorage;
    }
  }

  /**
   * Storage helper methods
   */
  private setStorageItem(key: string, value: string): void {
    if (this.storage instanceof Map) {
      this.storage.set(key, value);
    } else {
      // Type narrowing: storage is Storage (localStorage or sessionStorage)
      const webStorage = this.storage as Storage;
      webStorage.setItem(key, value);
    }
  }

  private getStorageItem(key: string): string | null {
    if (this.storage instanceof Map) {
      return this.storage.get(key) || null;
    } else {
      // Type narrowing: storage is Storage (localStorage or sessionStorage)
      const webStorage = this.storage as Storage;
      return webStorage.getItem(key);
    }
  }

  private removeStorageItem(key: string): void {
    if (this.storage instanceof Map) {
      this.storage.delete(key);
    } else {
      // Type narrowing: storage is Storage (localStorage or sessionStorage)
      const webStorage = this.storage as Storage;
      webStorage.removeItem(key);
    }
  }

  private clearStorage(): void {
    if (this.storage instanceof Map) {
      this.storage.clear();
    } else {
      // Type narrowing: storage is Storage (localStorage or sessionStorage)
      const webStorage = this.storage as Storage;
      Object.values(STORAGE_KEYS).forEach(key => {
        webStorage.removeItem(key);
      });
    }
  }

  /**
   * Set up request interceptor to add authentication header
   */
  private setupRequestInterceptor(): void {
    this.axiosInstance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = this.getAccessToken();
        if (token && !config.headers['Authorization']) {
          config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );
  }

  /**
   * Set up response interceptor for automatic token refresh
   */
  private setupResponseInterceptor(): void {
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        // If error is 401 and we haven't already tried to refresh
        if (error.response?.status === 401 && !originalRequest._retry && this.getRefreshToken()) {
          originalRequest._retry = true;

          try {
            const tokens = await this.refreshToken();

            // Update the authorization header and retry
            originalRequest.headers['Authorization'] = `Bearer ${tokens.accessToken}`;
            return this.axiosInstance(originalRequest);
          } catch (refreshError) {
            // Refresh failed, clear auth and propagate error
            this.clearAuth();
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(this.handleAuthError(error));
      }
    );
  }

  /**
   * Register a new user
   */
  async register(request: RegisterRequest): Promise<AuthResponse> {
    // Validate password requirements
    this.validatePassword(request.password);

    if (request.password !== request.confirmPassword) {
      throw new Error('Passwords do not match');
    }

    try {
      const response = await this.axiosInstance.post<AuthResponse>(
        '/api/v1/auth/register',
        request
      );

      const authResponse = response.data;

      // Store tokens and user
      this.handleAuthSuccess(authResponse, request.acceptTerms);

      return authResponse;
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Login with credentials
   */
  async login(request: LoginRequest): Promise<AuthResponse> {
    try {
      // Transform frontend LoginRequest to match backend expectations
      const backendRequest = {
        usernameOrEmail: request.email,
        password: request.password,
        rememberMe: (request as any).rememberMe || false
      };

      const response = await this.axiosInstance.post<AuthResponse>(
        '/api/v1/auth/login',
        backendRequest
      );

      const authResponse = response.data;

      // Store tokens and user
      this.handleAuthSuccess(authResponse, request.rememberMe || false);

      return authResponse;
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Logout and clear authentication
   */
  async logout(): Promise<void> {
    const refreshToken = this.getRefreshToken();

    try {
      if (refreshToken) {
        // Call logout endpoint to invalidate refresh token on server
        await this.axiosInstance.post('/api/v1/auth/logout', {
          refreshToken
        });
      }
    } catch (error) {
      // Log error but don't throw - we still want to clear local auth
      console.error('Logout request failed:', error);
    } finally {
      this.clearAuth();
    }
  }

  /**
   * Refresh access token
   */
  async refreshToken(): Promise<AuthTokens> {
    // Prevent multiple simultaneous refresh attempts
    if (this.isRefreshing && this.refreshPromise) {
      return this.refreshPromise;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    this.isRefreshing = true;

    const request: RefreshTokenRequest = {
      refreshToken,
      grantType: 'refresh_token'
    };

    this.refreshPromise = this.axiosInstance
      .post<RefreshTokenResponse>('/api/v1/auth/refresh', request)
      .then((response) => {
        const tokens = response.data;
        this.setTokens(tokens);

        // Notify listener if configured
        if (this.config.onTokenRefresh) {
          this.config.onTokenRefresh(tokens);
        }

        // Reschedule next refresh
        if (this.config.autoRefresh) {
          this.scheduleTokenRefresh();
        }

        return tokens;
      })
      .finally(() => {
        this.isRefreshing = false;
        this.refreshPromise = undefined;
      });

    return this.refreshPromise;
  }

  /**
   * Get current user
   */
  async getCurrentUser(): Promise<User | null> {
    if (!this.isAuthenticated()) {
      return null;
    }

    try {
      const response = await this.axiosInstance.get<User>('/api/v1/users/me');
      const user = response.data;

      // Update stored user
      this.setStorageItem(STORAGE_KEYS.USER, JSON.stringify(user));

      return user;
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Update user profile
   */
  async updateProfile(profile: Partial<UserProfile>): Promise<User> {
    try {
      const response = await this.axiosInstance.put<User>('/api/v1/users/me/profile', profile);
      const user = response.data;

      // Update stored user
      this.setStorageItem(STORAGE_KEYS.USER, JSON.stringify(user));

      return user;
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Update user preferences
   */
  async updatePreferences(preferences: Partial<UserPreferences>): Promise<User> {
    try {
      const response = await this.axiosInstance.put<User>('/api/v1/users/me/preferences', preferences);
      const user = response.data;

      // Update stored user
      this.setStorageItem(STORAGE_KEYS.USER, JSON.stringify(user));

      return user;
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Request password reset
   */
  async requestPasswordReset(request: PasswordResetRequest): Promise<void> {
    try {
      await this.axiosInstance.post('/api/v1/auth/password-reset', request);
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Confirm password reset
   */
  async confirmPasswordReset(request: PasswordResetConfirmation): Promise<void> {
    // Validate new password
    this.validatePassword(request.newPassword);

    if (request.newPassword !== request.confirmPassword) {
      throw new Error('Passwords do not match');
    }

    try {
      await this.axiosInstance.post('/api/v1/auth/password-reset/confirm', request);
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * Verify email address
   */
  async verifyEmail(request: EmailVerificationRequest): Promise<EmailVerificationResponse> {
    try {
      const response = await this.axiosInstance.post<EmailVerificationResponse>(
        '/api/v1/auth/verify-email',
        request
      );
      return response.data;
    } catch (error) {
      throw this.handleAuthError(error as AxiosError);
    }
  }

  /**
   * TokenStorage implementation: Get access token
   */
  getAccessToken(): string | null {
    return this.getStorageItem(STORAGE_KEYS.ACCESS_TOKEN);
  }

  /**
   * TokenStorage implementation: Get refresh token
   */
  getRefreshToken(): string | null {
    return this.getStorageItem(STORAGE_KEYS.REFRESH_TOKEN);
  }

  /**
   * TokenStorage implementation: Set tokens
   */
  setTokens(tokens: AuthTokens): void {
    if (!tokens.accessToken || !tokens.refreshToken) {
      throw new Error('Invalid tokens: both access and refresh tokens are required');
    }

    if (tokens.expiresIn <= 0) {
      throw new Error('Invalid token expiry time');
    }

    this.setStorageItem(STORAGE_KEYS.ACCESS_TOKEN, tokens.accessToken);
    this.setStorageItem(STORAGE_KEYS.REFRESH_TOKEN, tokens.refreshToken);

    const expiryTime = Date.now() + tokens.expiresIn;
    this.setStorageItem(STORAGE_KEYS.TOKEN_EXPIRY, expiryTime.toString());

    // Schedule refresh if auto-refresh is enabled
    if (this.config.autoRefresh) {
      this.scheduleTokenRefresh();
    }
  }

  /**
   * TokenStorage implementation: Clear tokens
   */
  clearTokens(): void {
    this.removeStorageItem(STORAGE_KEYS.ACCESS_TOKEN);
    this.removeStorageItem(STORAGE_KEYS.REFRESH_TOKEN);
    this.removeStorageItem(STORAGE_KEYS.TOKEN_EXPIRY);

    // Cancel any scheduled refresh
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = undefined;
    }
  }

  /**
   * TokenStorage implementation: Get token expiry time
   */
  getTokenExpiry(): number | null {
    const expiry = this.getStorageItem(STORAGE_KEYS.TOKEN_EXPIRY);
    return expiry ? parseInt(expiry, 10) : null;
  }

  /**
   * TokenStorage implementation: Check if token is expired
   */
  isTokenExpired(): boolean {
    const expiry = this.getTokenExpiry();
    if (!expiry) {
      return true;
    }
    return Date.now() >= expiry;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken() && !this.isTokenExpired();
  }

  /**
   * Check if auto-refresh is enabled
   */
  isAutoRefreshEnabled(): boolean {
    return this.config.autoRefresh && !!this.refreshTimer;
  }

  /**
   * Get axios instance for making authenticated requests
   */
  getAxiosInstance(): AxiosInstance {
    return this.axiosInstance;
  }

  /**
   * Clean up service (cancel timers, clear interceptors)
   */
  destroy(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = undefined;
    }

    // Clear interceptors (axios uses eject, not clear)
    // Note: We would need to store interceptor IDs to properly eject them
    // For now, we'll just leave them as axios will clean up on instance destruction
  }

  /**
   * Private helper methods
   */

  private handleAuthSuccess(response: AuthResponse, rememberMe: boolean): void {
    // Switch storage based on remember me
    if (rememberMe && this.config.tokenStorage === 'sessionStorage') {
      this.storage = localStorage;
    } else if (!rememberMe && this.config.tokenStorage === 'localStorage') {
      this.storage = sessionStorage;
    }

    // Store tokens
    this.setTokens(response.tokens);

    // Store user
    if (response.user) {
      this.setStorageItem(STORAGE_KEYS.USER, JSON.stringify(response.user));
    }

    // Store remember me preference
    this.setStorageItem(STORAGE_KEYS.REMEMBER_ME, rememberMe.toString());
  }

  private clearAuth(): void {
    this.clearTokens();
    this.removeStorageItem(STORAGE_KEYS.USER);
    this.removeStorageItem(STORAGE_KEYS.REMEMBER_ME);
  }

  private scheduleTokenRefresh(): void {
    // Cancel any existing timer
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    const expiry = this.getTokenExpiry();
    if (!expiry) {
      return;
    }

    const now = Date.now();
    const timeUntilExpiry = expiry - now;
    const refreshBuffer = this.config.refreshBuffer || 60000;
    const refreshIn = Math.max(0, timeUntilExpiry - refreshBuffer);

    // Don't schedule if token is already expired or will expire very soon
    if (refreshIn < 1000) {
      // Try to refresh immediately
      this.refreshToken().catch(() => {
        this.clearAuth();
      });
      return;
    }

    this.refreshTimer = setTimeout(() => {
      this.refreshToken().catch(() => {
        this.clearAuth();
      });
    }, refreshIn);
  }

  private validatePassword(password: string): void {
    const config = this.config;

    if (password.length < (config.passwordMinLength || 8)) {
      throw new Error(`Password must be at least ${config.passwordMinLength || 8} characters`);
    }

    if (config.passwordRequireUpperCase && !/[A-Z]/.test(password)) {
      throw new Error('Password must contain at least one uppercase letter');
    }

    if (config.passwordRequireNumber && !/\d/.test(password)) {
      throw new Error('Password must contain at least one number');
    }

    if (config.passwordRequireSpecialChar && !/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      throw new Error('Password must contain at least one special character');
    }
  }

  private handleAuthError(error: AxiosError): Error {
    let authError: AuthError = {
      code: 'UNAUTHORIZED' as AuthErrorCode,
      message: 'An authentication error occurred',
      timestamp: new Date().toISOString()
    };

    if (error.response) {
      const status = error.response.status;
      const data = error.response.data as any;

      // Map HTTP status to auth error codes
      switch (status) {
        case 400:
          authError.code = 'VALIDATION_ERROR';
          // Check if it's actually a rate limit issue disguised as 400
          if (data?.error?.includes('rate limit') || error.response?.headers?.['x-ratelimit-remaining'] === '0') {
            authError.code = 'RATE_LIMIT_EXCEEDED';
            authError.message = 'Rate limit exceeded. Please wait before trying again.';
          } else {
            authError.message = data?.message || data?.error || 'Invalid request';
          }
          break;
        case 401:
          authError.code = data?.code || 'INVALID_CREDENTIALS';
          authError.message = data?.message || 'Invalid credentials';
          break;
        case 403:
          authError.code = 'FORBIDDEN';
          authError.message = data?.message || 'Access forbidden';
          break;
        case 429:
          authError.code = 'RATE_LIMIT_EXCEEDED';
          authError.message = data?.message || data?.error || 'Too many attempts. Please wait before trying again.';
          authError.retryAfter = error.response?.headers?.['x-ratelimit-reset'] ?
            parseInt(error.response.headers['x-ratelimit-reset']) * 1000 : undefined;
          break;
        default:
          authError.message = data?.message || error.message;
      }

      authError.details = data?.details;
      authError.statusCode = status;
    } else if (error.request) {
      authError.message = 'Network error - unable to reach authentication service';
    }

    // Notify error handler if configured
    if (this.config.onAuthError) {
      this.config.onAuthError(authError);
    }

    return new Error(authError.message);
  }
}

// Export a default instance for backward compatibility
// Note: This uses environment variables and should be configured properly
const authServiceConfig = {
  baseUrl: process.env.VITE_IDENTITY_SERVICE_URL || 'http://localhost:8081',
  tokenStorage: 'localStorage' as const,
  autoRefresh: true,
  refreshBuffer: 60000
};

export const authService = new AuthService(authServiceConfig);
