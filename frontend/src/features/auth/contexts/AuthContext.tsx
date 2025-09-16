import React, {ReactNode, useCallback, useEffect, useReducer} from 'react';
import {
  AuthState,
  ChangePasswordRequest,
  LoginRequest,
  PasswordResetRequest,
  RegisterRequest,
  User
} from '@shared/types/auth';
import authApiService, {tokenStorage} from '../../../services/api/authApi';
import {tokenManager} from '../../../utils/tokenManager';
import {FeatureLevelErrorBoundary} from '@shared/components/error-boundary';
// Import contexts from separate file to avoid Fast Refresh warnings
import {AuthActionsContext, AuthStateContext} from './authContexts';

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


interface AuthProviderProps {
  children: ReactNode;
}

/**
 * AuthProvider Component
 *
 * Provides authentication context to the entire application
 * Handles automatic token validation on app initialization
 */
export function AuthProvider({children}: AuthProviderProps): JSX.Element {
  const [authState, dispatch] = useReducer(authReducer, initialAuthState);

  // Authentication actions
  const login = async (credentials: LoginRequest): Promise<void> => {
    try {
      dispatch({type: 'AUTH_LOADING'});

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
        payload: {error: errorMessage}
      });
      throw error;
    }
  };

  const register = async (userData: RegisterRequest): Promise<void> => {
    try {
      dispatch({type: 'AUTH_LOADING'});

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
        payload: {error: errorMessage}
      });
      throw error;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await authApiService.logout();
    } catch {
      // Auth logout error logged to error service
    } finally {
      dispatch({type: 'AUTH_LOGOUT'});
    }
  };

  const refreshAuth = async (): Promise<void> => {
    try {
      dispatch({type: 'AUTH_VALIDATE_START'});

      const isValid = await authApiService.validateAuth();

      if (isValid) {
        const user = await authApiService.getCurrentUser();
        dispatch({
          type: 'AUTH_VALIDATE_SUCCESS',
          payload: {user}
        });
      } else {
        dispatch({type: 'AUTH_VALIDATE_FAILURE'});
      }
    } catch {
      dispatch({type: 'AUTH_VALIDATE_FAILURE'});
    }
  };

  const updateProfile = async (userData: Partial<User>): Promise<void> => {
    try {
      const updatedUser = await authApiService.updateProfile(userData);

      dispatch({
        type: 'AUTH_UPDATE_USER',
        payload: {user: updatedUser}
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Profile update failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: {error: errorMessage}
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
        payload: {error: errorMessage}
      });
      throw error;
    }
  };

  const requestPasswordReset = async (resetData: PasswordResetRequest): Promise<{
    message: string
  }> => {
    try {
      const response = await authApiService.requestPasswordReset(resetData);
      return response;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Password reset request failed';
      dispatch({
        type: 'AUTH_FAILURE',
        payload: {error: errorMessage}
      });
      throw error;
    }
  };

  const clearError = (): void => {
    dispatch({type: 'AUTH_CLEAR_ERROR'});
  };

  // Handle automatic token refresh
  const handleTokenRefresh = useCallback(async () => {
    try {
      await refreshAuth();
    } catch {
      // console.error('Automatic token refresh failed:', error);
      dispatch({type: 'AUTH_LOGOUT'});
    }
  }, []);

  // Handle auth failure (logout user)
  const handleAuthFailure = useCallback(() => {
    dispatch({type: 'AUTH_LOGOUT'});
  }, []);

  // Initialize authentication state on app load
  useEffect(() => {
    const initializeAuth = async (): Promise<void> => {
      // Check if we have stored tokens using both token managers for compatibility
      const hasTokensFromOldManager = tokenStorage.hasValidTokens();
      const hasTokensFromNewManager = tokenManager.hasValidTokens();

      if (hasTokensFromOldManager || hasTokensFromNewManager) {
        await refreshAuth();
      }
    };

    initializeAuth();
  }, []);

  // Set up event listeners for token management
  useEffect(() => {
    // Listen for token refresh events from the token manager
    window.addEventListener('tokenRefreshNeeded', handleTokenRefresh);

    // Listen for auth failure events from axios interceptors
    window.addEventListener('authFailure', handleAuthFailure);

    // Cleanup event listeners
    return () => {
      window.removeEventListener('tokenRefreshNeeded', handleTokenRefresh);
      window.removeEventListener('authFailure', handleAuthFailure);
    };
  }, [handleTokenRefresh, handleAuthFailure]);

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
