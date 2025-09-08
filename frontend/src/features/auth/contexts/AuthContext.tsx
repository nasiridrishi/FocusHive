import React, { useReducer, useEffect, ReactNode } from 'react';
import {
  AuthState,
  LoginRequest,
  RegisterRequest,
  User,
  ChangePasswordRequest,
  PasswordResetRequest
} from '@shared/types/auth';
import authApiService, { tokenStorage } from '../../../services/api/authApi';
import { FeatureLevelErrorBoundary } from '@shared/components/error-boundary';

/**
 * Authentication Context Implementation
 * 
 * Implements secure authentication state management with:
 * - useReducer for predictable state updates
 * - Automatic token validation on app load
 * - Secure token storage
 * - Error handling and loading states
 * - Context pattern for global auth state
 */

// Auth Actions
type AuthAction =
  | { type: 'AUTH_LOADING' }
  | { type: 'AUTH_SUCCESS'; payload: { user: User; token: string; refreshToken: string } }
  | { type: 'AUTH_FAILURE'; payload: { error: string } }
  | { type: 'AUTH_LOGOUT' }
  | { type: 'AUTH_UPDATE_USER'; payload: { user: User } }
  | { type: 'AUTH_CLEAR_ERROR' }
  | { type: 'AUTH_VALIDATE_START' }
  | { type: 'AUTH_VALIDATE_SUCCESS'; payload: { user: User } }
  | { type: 'AUTH_VALIDATE_FAILURE' };

// Initial auth state
const initialAuthState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  isLoading: false,
  isAuthenticated: false,
  error: null
};

// Auth reducer with comprehensive state management
function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'AUTH_LOADING':
      return {
        ...state,
        isLoading: true,
        error: null
      };

    case 'AUTH_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        refreshToken: action.payload.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        error: null
      };

    case 'AUTH_FAILURE':
      return {
        ...state,
        user: null,
        token: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
        error: action.payload.error
      };

    case 'AUTH_LOGOUT':
      return {
        ...initialAuthState,
        isLoading: false
      };

    case 'AUTH_UPDATE_USER':
      return {
        ...state,
        user: action.payload.user,
        error: null
      };

    case 'AUTH_CLEAR_ERROR':
      return {
        ...state,
        error: null
      };

    case 'AUTH_VALIDATE_START':
      return {
        ...state,
        isLoading: true
      };

    case 'AUTH_VALIDATE_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        token: tokenStorage.getAccessToken(),
        refreshToken: tokenStorage.getRefreshToken(),
        isAuthenticated: true,
        isLoading: false,
        error: null
      };

    case 'AUTH_VALIDATE_FAILURE':
      return {
        ...initialAuthState,
        isLoading: false
      };

    default:
      return state;
  }
}

// Import contexts from separate file to avoid Fast Refresh warnings
import { AuthStateContext, AuthActionsContext } from './authContexts';




interface AuthProviderProps {
  children: ReactNode;
}

/**
 * AuthProvider Component
 * 
 * Provides authentication context to the entire application
 * Handles automatic token validation on app initialization
 */
export function AuthProvider({ children }: AuthProviderProps): JSX.Element {
  const [authState, dispatch] = useReducer(authReducer, initialAuthState);

  // Authentication actions
  const login = async (credentials: LoginRequest): Promise<void> => {
    try {
      dispatch({ type: 'AUTH_LOADING' });
      
      const response = await authApiService.login(credentials);
      
      dispatch({
        type: 'AUTH_SUCCESS',
        payload: {
          user: response.user,
          token: response.token,
          refreshToken: response.refreshToken
        }
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Login failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: { error: errorMessage }
      });
      throw error;
    }
  };

  const register = async (userData: RegisterRequest): Promise<void> => {
    try {
      dispatch({ type: 'AUTH_LOADING' });
      
      const response = await authApiService.register(userData);
      
      dispatch({
        type: 'AUTH_SUCCESS',
        payload: {
          user: response.user,
          token: response.token,
          refreshToken: response.refreshToken
        }
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Registration failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: { error: errorMessage }
      });
      throw error;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await authApiService.logout();
    } catch (error) {
      console.error('Auth logout error:', error);
    } finally {
      dispatch({ type: 'AUTH_LOGOUT' });
    }
  };

  const refreshAuth = async (): Promise<void> => {
    try {
      dispatch({ type: 'AUTH_VALIDATE_START' });
      
      const isValid = await authApiService.validateAuth();
      
      if (isValid) {
        const user = await authApiService.getCurrentUser();
        dispatch({
          type: 'AUTH_VALIDATE_SUCCESS',
          payload: { user }
        });
      } else {
        dispatch({ type: 'AUTH_VALIDATE_FAILURE' });
      }
    } catch (error) {
      console.error('Auth validation error:', error);
      dispatch({ type: 'AUTH_VALIDATE_FAILURE' });
    }
  };

  const updateProfile = async (userData: Partial<User>): Promise<void> => {
    try {
      const updatedUser = await authApiService.updateProfile(userData);
      
      dispatch({
        type: 'AUTH_UPDATE_USER',
        payload: { user: updatedUser }
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Profile update failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: { error: errorMessage }
      });
      throw error;
    }
  };

  const changePassword = async (passwordData: ChangePasswordRequest): Promise<void> => {
    try {
      await authApiService.changePassword(passwordData);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Password change failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: { error: errorMessage }
      });
      throw error;
    }
  };

  const requestPasswordReset = async (resetData: PasswordResetRequest): Promise<{ message: string }> => {
    try {
      const response = await authApiService.requestPasswordReset(resetData);
      return response;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Password reset request failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: { error: errorMessage }
      });
      throw error;
    }
  };

  const clearError = (): void => {
    dispatch({ type: 'AUTH_CLEAR_ERROR' });
  };

  // Initialize authentication state on app load
  useEffect(() => {
    const initializeAuth = async () => {
      // Check if we have stored tokens
      if (tokenStorage.hasValidTokens()) {
        await refreshAuth();
      }
    };

    initializeAuth();
  }, []);

  // Auth actions object
  const authActions = {
    login,
    register,
    logout,
    refreshAuth,
    updateProfile,
    changePassword,
    requestPasswordReset,
    clearError
  };

  return (
    <FeatureLevelErrorBoundary featureName="Authentication">
      <AuthStateContext.Provider value={authState}>
        <AuthActionsContext.Provider value={authActions}>
          {children}
        </AuthActionsContext.Provider>
      </AuthStateContext.Provider>
    </FeatureLevelErrorBoundary>
  );
}

// Hooks are now exported from useAuth.ts to avoid Fast Refresh warnings

export default AuthProvider;
