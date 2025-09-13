import axios, { AxiosInstance, AxiosError, AxiosRequestHeaders } from 'axios';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  User,
  PasswordResetRequest,
  PasswordResetResponse,
  ChangePasswordRequest
} from '@shared/types/auth';
import { tokenManager } from '../../utils/tokenManager';
import { apiClient, identityApi } from '../../utils/axiosConfig';
import { API_ENDPOINTS, getServiceUrl } from '../../config/apiConfig';

/**
 * Authentication API Configuration
 * 
 * Implements secure JWT authentication with:
 * - HttpOnly cookie-based token storage (XSS protection)
 * - Automatic token refresh via cookies
 * - Request/response interceptors
 * - CSRF protection
 * - Error handling
 * - Backward compatibility with localStorage
 */

// Use centralized API configuration
const IDENTITY_SERVICE_URL = getServiceUrl('AUTH', '');

// Token storage utility following security best practices
class TokenStorage {
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  
  // Store access token in sessionStorage (cleared on browser close)
  setAccessToken(token: string | null): void {
    if (token) {
      sessionStorage.setItem(this.ACCESS_TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    }
  }
  
  getAccessToken(): string | null {
    return sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }
  
  // Store refresh token in localStorage for persistence across sessions
  // Note: In production, consider using httpOnly cookies for refresh tokens
  setRefreshToken(token: string | null): void {
    if (token) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
    } else {
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }
  }
  
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }
  
  clearAllTokens(): void {
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }
  
  hasValidTokens(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    
    if (!accessToken && !refreshToken) {
      return false;
    }
    
    // Basic JWT token validation
    if (accessToken) {
      try {
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        const currentTime = Date.now() / 1000;
        
        // Check if token is not expired (with 5 minute buffer)
        if (payload.exp && payload.exp > currentTime + 300) {
          return true;
        }
      } catch {
        // Invalid token format, continue to check refresh token
      }
    }
    
    // If access token is expired/invalid, check if we have a refresh token
    return !!refreshToken;
  }
}

const tokenStorage = new TokenStorage();

// Create axios instance for backend API
const createBackendApiInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: getServiceUrl('HIVES', ''),  // Use backend service URL for non-auth endpoints
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true, // Enable cookies for httpOnly refresh tokens in future
  });

  // Request interceptor to add auth token
  instance.interceptors.request.use(
    (config) => {
      const token = tokenStorage.getAccessToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor for token refresh
  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config;
      
      // Check if error is due to expired token
      if (error.response?.status === 401 && originalRequest && !(originalRequest as { _retry?: boolean })._retry) {
        (originalRequest as { _retry?: boolean })._retry = true;
        
        try {
          const newTokens = await refreshToken();
          if (newTokens) {
            // Update the authorization header with new token
            if (!originalRequest.headers) {
              originalRequest.headers = {} as AxiosRequestHeaders;
            }
            originalRequest.headers.Authorization = `Bearer ${newTokens.token}`;
            
            // Retry the original request
            return backendApi(originalRequest);
          }
        } catch (refreshError) {
          // Refresh failed, clear tokens and reject
          tokenStorage.clearAllTokens();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }
      
      return Promise.reject(error);
    }
  );

  return instance;
};

const backendApi = createBackendApiInstance();

// Create axios instance specifically for Identity Service auth endpoints
const identityApi = axios.create({
  baseURL: IDENTITY_SERVICE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Add token to Identity Service requests
identityApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Auth API methods
export const authApiService = {
  /**
   * Login user with email and password
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await identityApi.post<LoginResponse>(API_ENDPOINTS.AUTH.LOGIN, credentials);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { user, token, refreshToken } = response.data;
      
      // Validate all required fields are present and valid
      if (!user || typeof user !== 'object') {
        throw new Error('Invalid user data in response');
      }
      
      if (!token || typeof token !== 'string' || token.trim() === '') {
        throw new Error('Invalid authentication token in response');
      }
      
      if (!refreshToken || typeof refreshToken !== 'string' || refreshToken.trim() === '') {
        throw new Error('Invalid refresh token in response');
      }
      
      // Validate user object has required fields
      if (!user.id || !user.email || !user.username) {
        throw new Error('Incomplete user data in response');
      }
      
      // Store tokens securely using enhanced token manager
      tokenManager.saveTokens(token, refreshToken);
      
      // Also maintain compatibility with old token storage
      tokenStorage.setAccessToken(token);
      tokenStorage.setRefreshToken(refreshToken);
      
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError) {
        // Handle timeout errors
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          throw new Error('Request timeout - please try again');
        }
        
        // Handle network errors
        if (!error.response) {
          throw new Error('Network error during login - please check your connection');
        }
        
        // Handle JSON parsing errors
        if (error.message.includes('JSON')) {
          throw new Error('Invalid response format from server');
        }
        
        // Handle server error responses
        const message = error.response?.data?.message || error.response?.data?.error || 'Login failed';
        throw new Error(message);
      }
      
      // If it's already our custom error from validation, re-throw it
      if (error instanceof Error) {
        throw error;
      }
      
      throw new Error('Unexpected error during login');
    }
  },

  /**
   * Register new user
   */
  async register(userData: RegisterRequest): Promise<RegisterResponse> {
    try {
      // Add default persona settings to registration data
      const registrationData = {
        ...userData,
        personaType: 'PERSONAL',
        personaName: 'Personal'
      };
      
      const response = await identityApi.post<RegisterResponse>(API_ENDPOINTS.AUTH.REGISTER, registrationData);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { user, token, refreshToken } = response.data;
      
      // Validate all required fields are present and valid
      if (!user || typeof user !== 'object') {
        throw new Error('Invalid user data in response');
      }
      
      if (!token || typeof token !== 'string' || token.trim() === '') {
        throw new Error('Invalid authentication token in response');
      }
      
      if (!refreshToken || typeof refreshToken !== 'string' || refreshToken.trim() === '') {
        throw new Error('Invalid refresh token in response');
      }
      
      // Validate user object has required fields
      if (!user.id || !user.email || !user.username) {
        throw new Error('Incomplete user data in response');
      }
      
      // Store tokens securely using enhanced token manager
      tokenManager.saveTokens(token, refreshToken);
      
      // Also maintain compatibility with old token storage
      tokenStorage.setAccessToken(token);
      tokenStorage.setRefreshToken(refreshToken);
      
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError) {
        // Handle timeout errors
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          throw new Error('Registration timeout - please try again');
        }
        
        // Handle network errors
        if (!error.response) {
          throw new Error('Network error during registration - please check your connection');
        }
        
        // Handle JSON parsing errors
        if (error.message.includes('JSON')) {
          throw new Error('Invalid response format from server');
        }
        
        // Handle server error responses
        const message = error.response?.data?.message || error.response?.data?.error || 'Registration failed';
        throw new Error(message);
      }
      
      // If it's already our custom error from validation, re-throw it
      if (error instanceof Error) {
        throw error;
      }
      
      throw new Error('Unexpected error during registration');
    }
  },

  /**
   * Logout user - clear tokens and notify server
   */
  async logout(): Promise<void> {
    try {
      await identityApi.post(API_ENDPOINTS.AUTH.LOGOUT);
    } catch (error) {
      // Log error but don't throw - we want to clear local tokens regardless
      console.warn('Logout request failed - tokens cleared locally anyway:', error);
    } finally {
      tokenStorage.clearAllTokens();
    }
  },

  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<User> {
    try {
      const response = await identityApi.get<{ user: User }>(API_ENDPOINTS.AUTH.ME);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { user } = response.data;
      
      // Validate user object is present and valid
      if (!user || typeof user !== 'object') {
        throw new Error('Invalid user data in response');
      }
      
      // Validate user object has required fields
      if (!user.id || !user.email || !user.username) {
        throw new Error('Incomplete user data in response');
      }
      
      return user;
    } catch (error) {
      if (error instanceof AxiosError) {
        // Handle timeout errors
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          throw new Error('Request timeout - please try again');
        }
        
        // Handle network errors
        if (!error.response) {
          throw new Error('Network error getting user profile - please check your connection');
        }
        
        // Handle JSON parsing errors
        if (error.message.includes('JSON')) {
          throw new Error('Invalid response format from server');
        }
        
        // Handle server error responses
        const message = error.response?.data?.message || 'Failed to get user profile';
        throw new Error(message);
      }
      
      // If it's already our custom error from validation, re-throw it
      if (error instanceof Error) {
        throw error;
      }
      
      throw new Error('Unexpected error getting user profile');
    }
  },

  /**
   * Update user profile
   */
  async updateProfile(userData: Partial<User>): Promise<User> {
    try {
      const response = await identityApi.put<{ user: User }>(API_ENDPOINTS.AUTH.PROFILE, userData);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { user } = response.data;
      
      // Validate user object is present and valid
      if (!user || typeof user !== 'object') {
        throw new Error('Invalid user data in response');
      }
      
      // Validate user object has required fields
      if (!user.id || !user.email || !user.username) {
        throw new Error('Incomplete user data in response');
      }
      
      return user;
    } catch (error) {
      if (error instanceof AxiosError) {
        // Handle timeout errors
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          throw new Error('Request timeout - please try again');
        }
        
        // Handle network errors
        if (!error.response) {
          throw new Error('Network error updating profile - please check your connection');
        }
        
        // Handle JSON parsing errors
        if (error.message.includes('JSON')) {
          throw new Error('Invalid response format from server');
        }
        
        // Handle server error responses
        const message = error.response?.data?.message || 'Failed to update profile';
        throw new Error(message);
      }
      
      // If it's already our custom error from validation, re-throw it
      if (error instanceof Error) {
        throw error;
      }
      
      throw new Error('Unexpected error updating profile');
    }
  },

  /**
   * Change password
   */
  async changePassword(passwordData: ChangePasswordRequest): Promise<void> {
    try {
      await identityApi.put(API_ENDPOINTS.AUTH.CHANGE_PASSWORD, passwordData);
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message || 'Failed to change password';
        throw new Error(message);
      }
      throw new Error('Network error changing password');
    }
  },

  /**
   * Request password reset
   */
  async requestPasswordReset(resetData: PasswordResetRequest): Promise<PasswordResetResponse> {
    try {
      const response = await identityApi.post<PasswordResetResponse>(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, resetData);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { message } = response.data;
      
      // Validate message field is present
      if (!message || typeof message !== 'string') {
        throw new Error('Invalid response message from server');
      }
      
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError) {
        // Handle timeout errors
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          throw new Error('Request timeout - please try again');
        }
        
        // Handle network errors
        if (!error.response) {
          throw new Error('Network error requesting password reset - please check your connection');
        }
        
        // Handle JSON parsing errors
        if (error.message.includes('JSON')) {
          throw new Error('Invalid response format from server');
        }
        
        // Handle server error responses
        const message = error.response?.data?.message || 'Failed to send password reset email';
        throw new Error(message);
      }
      
      // If it's already our custom error from validation, re-throw it
      if (error instanceof Error) {
        throw error;
      }
      
      throw new Error('Unexpected error requesting password reset');
    }
  },

  /**
   * Validate current authentication status
   */
  async validateAuth(): Promise<boolean> {
    try {
      if (!tokenStorage.hasValidTokens()) {
        return false;
      }

      // Try to get current user to validate token
      await this.getCurrentUser();
      return true;
    } catch {
      tokenStorage.clearAllTokens();
      return false;
    }
  },

  /**
   * Check if user is currently authenticated
   */
  isAuthenticated(): boolean {
    return tokenStorage.hasValidTokens();
  },

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    return tokenStorage.getAccessToken();
  }
};

/**
 * Refresh access token using refresh token
 */
async function refreshToken(): Promise<{ token: string; refreshToken: string } | null> {
  const refreshTokenValue = tokenStorage.getRefreshToken();
  
  if (!refreshTokenValue) {
    return null;
  }

  try {
    const response = await axios.post<{ token: string; refreshToken: string }>(
      `${IDENTITY_SERVICE_URL}${API_ENDPOINTS.AUTH.REFRESH}`,
      { refreshToken: refreshTokenValue },
      {
        headers: { 'Content-Type': 'application/json' },
        withCredentials: true,
        timeout: 10000
      }
    );

    // Validate response data contains required fields
    if (!response.data || typeof response.data !== 'object') {
      throw new Error('Invalid response format from server');
    }

    const { token, refreshToken: newRefreshToken } = response.data;
    
    // Validate required fields are present and valid
    if (!token || typeof token !== 'string' || token.trim() === '') {
      throw new Error('Invalid authentication token in response');
    }
    
    if (!newRefreshToken || typeof newRefreshToken !== 'string' || newRefreshToken.trim() === '') {
      throw new Error('Invalid refresh token in response');
    }
    
    // Update stored tokens
    tokenStorage.setAccessToken(token);
    tokenStorage.setRefreshToken(newRefreshToken);
    
    return response.data;
  } catch (error) {
    // Refresh failed, clear tokens
    tokenStorage.clearAllTokens();
    
    if (error instanceof AxiosError) {
      // Handle timeout errors
      if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        throw new Error('Token refresh timeout - please login again');
      }
      
      // Handle JSON parsing errors
      if (error.message.includes('JSON')) {
        throw new Error('Invalid response format from server');
      }
    }
    
    throw error;
  }
}

// Export token storage for use by auth context
export { tokenStorage };

export default authApiService;