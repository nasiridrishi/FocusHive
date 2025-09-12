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

/**
 * Cookie-based Authentication API Service
 * 
 * Implements secure JWT authentication using httpOnly cookies:
 * - No client-side token storage (XSS protection)
 * - Automatic token handling via cookies
 * - CSRF protection
 * - Secure cookie attributes
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const IDENTITY_API_URL = import.meta.env.VITE_IDENTITY_API_URL || 'http://localhost:8081';

// Create axios instance for backend API with cookie support
const createBackendApiInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: `${API_BASE_URL}/api`,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true, // Essential for httpOnly cookies
  });

  // Response interceptor for token refresh via cookies
  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config;
      
      // Check if error is due to expired token
      if (error.response?.status === 401 && originalRequest && !(originalRequest as { _retry?: boolean })._retry) {
        (originalRequest as { _retry?: boolean })._retry = true;
        
        try {
          // Attempt to refresh token (cookies handled automatically)
          await refreshToken();
          
          // Retry the original request (cookies will be sent automatically)
          return backendApi(originalRequest);
        } catch (refreshError) {
          // Refresh failed, redirect to login
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
  baseURL: `${IDENTITY_API_URL}/api`,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Essential for httpOnly cookies
});

// Cookie-based Auth API methods
export const cookieAuthApiService = {
  /**
   * Login user with email and password
   * Tokens are set as httpOnly cookies by the server
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await identityApi.post<LoginResponse>('/v1/auth/login', credentials);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { user, token, refreshToken } = response.data;
      
      // Validate all required fields are present
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
      
      // Tokens are automatically stored as httpOnly cookies by the server
      // No client-side storage needed for security
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
   * Tokens are set as httpOnly cookies by the server
   */
  async register(userData: RegisterRequest): Promise<RegisterResponse> {
    try {
      // Add default persona settings to registration data
      const registrationData = {
        ...userData,
        personaType: 'PERSONAL',
        personaName: 'Personal'
      };
      
      const response = await identityApi.post<RegisterResponse>('/v1/auth/register', registrationData);
      
      // Validate response data contains required fields
      if (!response.data || typeof response.data !== 'object') {
        throw new Error('Invalid response format from server');
      }
      
      const { user, token, refreshToken } = response.data;
      
      // Validate all required fields are present
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
      
      // Tokens are automatically stored as httpOnly cookies by the server
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
   * Logout user - clear cookies on server side
   */
  async logout(): Promise<void> {
    try {
      // Server will clear httpOnly cookies and blacklist tokens
      await identityApi.post('/v1/auth/logout', {});
    } catch (error) {
      // Log error but don't throw - cookies will be cleared by server regardless
      console.warn('Logout request failed - cookies cleared by server anyway:', error);
    }
    // No client-side cleanup needed since tokens are in httpOnly cookies
  },

  /**
   * Get current user profile
   * Authentication via httpOnly cookies
   */
  async getCurrentUser(): Promise<User> {
    try {
      const response = await identityApi.get<{ user: User }>('/v1/auth/me');
      
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
      const response = await identityApi.put<{ user: User }>('/v1/auth/profile', userData);
      
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
      await identityApi.put('/v1/auth/change-password', passwordData);
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
      const response = await identityApi.post<PasswordResetResponse>('/v1/auth/forgot-password', resetData);
      
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
   * Uses httpOnly cookies automatically
   */
  async validateAuth(): Promise<boolean> {
    try {
      // Try to get current user to validate authentication
      // Cookies will be sent automatically
      await this.getCurrentUser();
      return true;
    } catch {
      return false;
    }
  },

  /**
   * Check if user is currently authenticated
   * Since tokens are in httpOnly cookies, we can't check them directly
   * This method attempts a lightweight request to verify authentication
   */
  async isAuthenticated(): Promise<boolean> {
    try {
      // Make a simple request that requires authentication
      const response = await identityApi.get('/v1/auth/me');
      return response.status === 200;
    } catch {
      return false;
    }
  },

  /**
   * Get CSRF token for protected requests
   * CSRF token is available as a non-httpOnly cookie
   */
  getCsrfToken(): string | null {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'XSRF-TOKEN') {
        return decodeURIComponent(value);
      }
    }
    return null;
  }
};

/**
 * Refresh access token using refresh token
 * Both tokens are handled as httpOnly cookies by the server
 */
async function refreshToken(): Promise<void> {
  try {
    // Server will use refresh token from httpOnly cookie
    // and set new tokens as httpOnly cookies
    await axios.post(
      `${IDENTITY_API_URL}/api/v1/auth/refresh`,
      {}, // Empty body - refresh token is in httpOnly cookie
      {
        headers: { 'Content-Type': 'application/json' },
        withCredentials: true,
        timeout: 10000
      }
    );
    
    // New tokens are now set as httpOnly cookies
  } catch (error) {
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

export default cookieAuthApiService;