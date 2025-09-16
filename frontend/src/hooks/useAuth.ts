/**
 * Authentication Hook
 * Provides authentication functionality and state management
 */

import { useState, useCallback, useEffect } from 'react';
import { AuthService } from '../services/auth/authService';
import type {
  User,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  AuthTokens,
  UpdateProfileRequest,
  UpdatePreferencesRequest,
  AuthError
} from '../contracts/auth';

// Create a singleton instance of AuthService
const authService = new AuthService({
  baseUrl: import.meta.env.VITE_IDENTITY_SERVICE_URL || 'http://localhost:8081',
  tokenStorage: 'localStorage',
  autoRefresh: true,
  refreshBuffer: 60000 // 1 minute before expiry
});

/**
 * Authentication hook return type
 */
export interface UseAuthReturn {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: AuthError | null;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
  updateProfile: (profile: UpdateProfileRequest) => Promise<void>;
  updatePreferences: (preferences: UpdatePreferencesRequest) => Promise<void>;
  clearError: () => void;
}

/**
 * Custom hook for authentication
 */
export function useAuth(): UseAuthReturn {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<AuthError | null>(null);

  // Check authentication status on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        setIsLoading(true);
        const isAuth = authService.isAuthenticated();

        if (isAuth) {
          const currentUser = await authService.getCurrentUser();
          setUser(currentUser);
        } else {
          setUser(null);
        }
      } catch (err) {
        console.error('Failed to check authentication:', err);
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  /**
   * Login with credentials
   */
  const login = useCallback(async (credentials: LoginRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await authService.login(credentials);
      setUser(response.user);
    } catch (err: any) {
      const authError: AuthError = {
        code: err.code || 'LOGIN_FAILED',
        message: err.message || 'Login failed',
        statusCode: err.statusCode
      };
      setError(authError);
      throw authError;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Register new user
   */
  const register = useCallback(async (userData: RegisterRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await authService.register(userData);
      setUser(response.user);
    } catch (err: any) {
      const authError: AuthError = {
        code: err.code || 'REGISTRATION_FAILED',
        message: err.message || 'Registration failed',
        statusCode: err.statusCode
      };
      setError(authError);
      throw authError;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Logout current user
   */
  const logout = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      await authService.logout();
      setUser(null);
    } catch (err: any) {
      const authError: AuthError = {
        code: 'LOGOUT_FAILED',
        message: err.message || 'Logout failed',
        statusCode: err.statusCode
      };
      setError(authError);
      throw authError;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Refresh access token
   */
  const refreshToken = useCallback(async () => {
    try {
      setError(null);
      await authService.refreshToken();
    } catch (err: any) {
      const authError: AuthError = {
        code: 'TOKEN_REFRESH_FAILED',
        message: err.message || 'Failed to refresh token',
        statusCode: err.statusCode
      };
      setError(authError);
      // If refresh fails, user needs to login again
      setUser(null);
      throw authError;
    }
  }, []);

  /**
   * Update user profile
   */
  const updateProfile = useCallback(async (profile: UpdateProfileRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      const updatedUser = await authService.updateProfile(profile);
      setUser(updatedUser);
    } catch (err: any) {
      const authError: AuthError = {
        code: 'PROFILE_UPDATE_FAILED',
        message: err.message || 'Failed to update profile',
        statusCode: err.statusCode
      };
      setError(authError);
      throw authError;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Update user preferences
   */
  const updatePreferences = useCallback(async (preferences: UpdatePreferencesRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      const updatedUser = await authService.updatePreferences(preferences);
      setUser(updatedUser);
    } catch (err: any) {
      const authError: AuthError = {
        code: 'PREFERENCES_UPDATE_FAILED',
        message: err.message || 'Failed to update preferences',
        statusCode: err.statusCode
      };
      setError(authError);
      throw authError;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Clear error state
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    user,
    isAuthenticated: !!user,
    isLoading,
    error,
    login,
    register,
    logout,
    refreshToken,
    updateProfile,
    updatePreferences,
    clearError
  };
}