/**
 * Authentication Context
 * Provides authentication state and methods throughout the application
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { AuthService } from '../services/auth/authService';
import type {
  User,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UpdateProfileRequest,
  UpdatePreferencesRequest,
  AuthError,
  AuthContextState,
  AuthContextMethods,
  AuthContextType
} from '../contracts/auth';

// Create singleton AuthService instance
const authService = new AuthService({
  baseUrl: import.meta.env.VITE_IDENTITY_SERVICE_URL || 'http://localhost:8081',
  tokenStorage: 'localStorage',
  autoRefresh: true,
  refreshBuffer: 60000, // 1 minute before expiry
  onAuthError: (error) => {
    console.error('Auth error:', error);
    // Could trigger global error notification here
  }
});

/**
 * Default context value
 */
const defaultContextValue: AuthContextType = {
  // State
  user: null,
  isAuthenticated: false,
  isLoading: true,
  error: null,
  sessionExpiresAt: null,
  lastActivity: null,

  // Methods
  login: async () => { throw new Error('AuthContext not initialized'); },
  register: async () => { throw new Error('AuthContext not initialized'); },
  logout: async () => { throw new Error('AuthContext not initialized'); },
  refreshToken: async () => { throw new Error('AuthContext not initialized'); },
  checkSession: async () => false,
  updateProfile: async () => { throw new Error('AuthContext not initialized'); },
  updatePreferences: async () => { throw new Error('AuthContext not initialized'); },
  switchPersona: async () => { throw new Error('AuthContext not initialized'); },
  clearError: () => {}
};

/**
 * Create the AuthContext
 */
const AuthContext = createContext<AuthContextType>(defaultContextValue);

/**
 * AuthProvider props
 */
export interface AuthProviderProps {
  children: ReactNode;
  onSessionExpired?: () => void;
  onAuthError?: (error: AuthError) => void;
}

/**
 * AuthProvider component
 */
export function AuthProvider({
  children,
  onSessionExpired,
  onAuthError
}: AuthProviderProps) {
  const [state, setState] = useState<AuthContextState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
    sessionExpiresAt: null,
    lastActivity: null
  });

  /**
   * Initialize authentication state
   */
  useEffect(() => {
    const initAuth = async () => {
      try {
        setState(prev => ({ ...prev, isLoading: true }));

        if (authService.isAuthenticated()) {
          const user = await authService.getCurrentUser();
          const expiryTimestamp = authService.getTokenExpiry();
          const expiresAt = expiryTimestamp ? new Date(expiryTimestamp) : null;

          setState(prev => ({
            ...prev,
            user,
            isAuthenticated: true,
            sessionExpiresAt: expiresAt,
            lastActivity: new Date(),
            isLoading: false
          }));
        } else {
          setState(prev => ({
            ...prev,
            user: null,
            isAuthenticated: false,
            sessionExpiresAt: null,
            isLoading: false
          }));
        }
      } catch (error) {
        console.error('Failed to initialize auth:', error);
        setState(prev => ({
          ...prev,
          user: null,
          isAuthenticated: false,
          isLoading: false,
          error: error as AuthError
        }));
      }
    };

    initAuth();
  }, []);

  /**
   * Handle authentication response
   */
  const handleAuthResponse = useCallback((response: AuthResponse) => {
    const expiresAt = new Date(Date.now() + response.tokens.expiresIn);

    setState(prev => ({
      ...prev,
      user: response.user,
      isAuthenticated: true,
      sessionExpiresAt: expiresAt,
      lastActivity: new Date(),
      error: null,
      isLoading: false
    }));
  }, []);

  /**
   * Handle authentication error
   */
  const handleAuthError = useCallback((error: AuthError) => {
    setState(prev => ({
      ...prev,
      error,
      isLoading: false
    }));

    if (onAuthError) {
      onAuthError(error);
    }
  }, [onAuthError]);

  /**
   * Login method
   */
  const login = useCallback(async (credentials: LoginRequest): Promise<AuthResponse> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const response = await authService.login(credentials);
      handleAuthResponse(response);
      return response;
    } catch (error: any) {
      const authError: AuthError = {
        code: error.code || 'LOGIN_FAILED',
        message: error.message || 'Login failed',
        statusCode: error.statusCode
      };
      handleAuthError(authError);
      throw authError;
    }
  }, [handleAuthResponse, handleAuthError]);

  /**
   * Register method
   */
  const register = useCallback(async (userData: RegisterRequest): Promise<AuthResponse> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const response = await authService.register(userData);
      handleAuthResponse(response);
      return response;
    } catch (error: any) {
      const authError: AuthError = {
        code: error.code || 'REGISTRATION_FAILED',
        message: error.message || 'Registration failed',
        statusCode: error.statusCode
      };
      handleAuthError(authError);
      throw authError;
    }
  }, [handleAuthResponse, handleAuthError]);

  /**
   * Logout method
   */
  const logout = useCallback(async (): Promise<void> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      await authService.logout();

      setState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
        sessionExpiresAt: null,
        lastActivity: null
      });
    } catch (error: any) {
      const authError: AuthError = {
        code: 'LOGOUT_FAILED',
        message: error.message || 'Logout failed',
        statusCode: error.statusCode
      };
      handleAuthError(authError);
      throw authError;
    }
  }, [handleAuthError]);

  /**
   * Refresh token method
   */
  const refreshToken = useCallback(async (): Promise<void> => {
    try {
      setState(prev => ({ ...prev, error: null }));
      const tokens = await authService.refreshToken();

      if (tokens) {
        const expiresAt = new Date(Date.now() + tokens.expiresIn);
        setState(prev => ({
          ...prev,
          sessionExpiresAt: expiresAt,
          lastActivity: new Date()
        }));
      }
    } catch (error: any) {
      const authError: AuthError = {
        code: 'TOKEN_REFRESH_FAILED',
        message: error.message || 'Failed to refresh token',
        statusCode: error.statusCode
      };

      // If refresh fails, session is expired
      if (onSessionExpired) {
        onSessionExpired();
      }

      // Clear auth state
      setState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: authError,
        sessionExpiresAt: null,
        lastActivity: null
      });

      throw authError;
    }
  }, [onSessionExpired]);

  /**
   * Check session validity
   */
  const checkSession = useCallback(async (): Promise<boolean> => {
    try {
      if (!authService.isAuthenticated()) {
        return false;
      }

      const expiresAt = authService.getTokenExpiry();
      if (expiresAt && expiresAt < Date.now()) {
        // Token expired, try to refresh
        await refreshToken();
        return authService.isAuthenticated();
      }

      return true;
    } catch {
      return false;
    }
  }, [refreshToken]);

  /**
   * Update user profile
   */
  const updateProfile = useCallback(async (profile: UpdateProfileRequest): Promise<User> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const updatedUser = await authService.updateProfile(profile);

      setState(prev => ({
        ...prev,
        user: updatedUser,
        lastActivity: new Date(),
        isLoading: false
      }));

      return updatedUser;
    } catch (error: any) {
      const authError: AuthError = {
        code: 'PROFILE_UPDATE_FAILED',
        message: error.message || 'Failed to update profile',
        statusCode: error.statusCode
      };
      handleAuthError(authError);
      throw authError;
    }
  }, [handleAuthError]);

  /**
   * Update user preferences
   */
  const updatePreferences = useCallback(async (preferences: UpdatePreferencesRequest): Promise<User> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));
      const updatedUser = await authService.updatePreferences(preferences);

      setState(prev => ({
        ...prev,
        user: updatedUser,
        lastActivity: new Date(),
        isLoading: false
      }));

      return updatedUser;
    } catch (error: any) {
      const authError: AuthError = {
        code: 'PREFERENCES_UPDATE_FAILED',
        message: error.message || 'Failed to update preferences',
        statusCode: error.statusCode
      };
      handleAuthError(authError);
      throw authError;
    }
  }, [handleAuthError]);

  /**
   * Switch persona
   */
  const switchPersona = useCallback(async (personaId: string): Promise<void> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));

      // TODO: Implement persona switching when Identity Service supports it
      // For now, just update the active persona locally
      if (state.user) {
        const updatedUser = {
          ...state.user,
          activePersonaId: personaId
        };

        setState(prev => ({
          ...prev,
          user: updatedUser,
          lastActivity: new Date(),
          isLoading: false
        }));
      }
    } catch (error: any) {
      const authError: AuthError = {
        code: 'PERSONA_SWITCH_FAILED',
        message: error.message || 'Failed to switch persona',
        statusCode: error.statusCode
      };
      handleAuthError(authError);
      throw authError;
    }
  }, [state.user, handleAuthError]);

  /**
   * Clear error
   */
  const clearError = useCallback(() => {
    setState(prev => ({ ...prev, error: null }));
  }, []);

  /**
   * Update activity timestamp
   */
  useEffect(() => {
    const updateActivity = () => {
      if (state.isAuthenticated) {
        setState(prev => ({ ...prev, lastActivity: new Date() }));
      }
    };

    // Update activity on user interaction
    window.addEventListener('click', updateActivity);
    window.addEventListener('keypress', updateActivity);

    return () => {
      window.removeEventListener('click', updateActivity);
      window.removeEventListener('keypress', updateActivity);
    };
  }, [state.isAuthenticated]);

  const contextValue: AuthContextType = {
    ...state,
    login,
    register,
    logout,
    refreshToken,
    checkSession,
    updateProfile,
    updatePreferences,
    switchPersona,
    clearError
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook to use the AuthContext
 */
export function useAuthContext(): AuthContextType {
  const context = useContext(AuthContext);

  if (context === defaultContextValue) {
    throw new Error('useAuthContext must be used within an AuthProvider');
  }

  return context;
}

/**
 * Export the context for direct access if needed
 */
export { AuthContext };