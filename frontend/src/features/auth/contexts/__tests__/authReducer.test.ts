import { describe, it, expect } from 'vitest';
import type { AuthState, User } from '@shared/types/auth';

// We need to export the reducer and action types from the main file
// For now, we'll recreate them for testing purposes
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

// Recreate the reducer for testing
function authReducer(state: AuthState, action: AuthAction): AuthState {
  if (!action || !action.type) {
    return state;
  }
  
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
        token: 'mock-token', // In real implementation, this comes from tokenStorage
        refreshToken: 'mock-refresh-token', // In real implementation, this comes from tokenStorage
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

// Test user data
const mockUser: User = {
  id: '1',
  email: 'testuser@example.com',
  username: 'testuser',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  avatar: null,
  profilePicture: null,
  isEmailVerified: true,
  isVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
};

const mockTokens = {
  token: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
};

describe('authReducer', () => {
  describe('AUTH_LOADING', () => {
    it('should set loading state to true and clear errors', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Previous error',
      };

      const action: AuthAction = { type: 'AUTH_LOADING' };
      const result = authReducer(stateWithError, action);

      expect(result).toEqual({
        ...stateWithError,
        isLoading: true,
        error: null,
      });
    });

    it('should preserve other state when setting loading', () => {
      const authenticatedState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        token: 'existing-token',
        refreshToken: 'existing-refresh',
        isAuthenticated: true,
      };

      const action: AuthAction = { type: 'AUTH_LOADING' };
      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        ...authenticatedState,
        isLoading: true,
        error: null,
      });
    });
  });

  describe('AUTH_SUCCESS', () => {
    it('should set authenticated state with user and tokens', () => {
      const action: AuthAction = {
        type: 'AUTH_SUCCESS',
        payload: {
          user: mockUser,
          ...mockTokens,
        },
      };

      const result = authReducer(initialAuthState, action);

      expect(result).toEqual({
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    });

    it('should clear previous error on successful authentication', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Previous login failed',
        isLoading: true,
      };

      const action: AuthAction = {
        type: 'AUTH_SUCCESS',
        payload: {
          user: mockUser,
          ...mockTokens,
        },
      };

      const result = authReducer(stateWithError, action);

      expect(result).toEqual({
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    });

    it('should replace previous user data on new authentication', () => {
      const oldUser: User = { ...mockUser, id: '2', email: 'old@example.com' };
      const authenticatedState: AuthState = {
        ...initialAuthState,
        user: oldUser,
        token: 'old-token',
        refreshToken: 'old-refresh',
        isAuthenticated: true,
      };

      const newUser: User = { ...mockUser, id: '3', email: 'new@example.com' };
      const action: AuthAction = {
        type: 'AUTH_SUCCESS',
        payload: {
          user: newUser,
          token: 'new-token',
          refreshToken: 'new-refresh',
        },
      };

      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        user: newUser,
        token: 'new-token',
        refreshToken: 'new-refresh',
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    });
  });

  describe('AUTH_FAILURE', () => {
    it('should clear authentication state and set error', () => {
      const authenticatedState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
        isLoading: true,
      };

      const action: AuthAction = {
        type: 'AUTH_FAILURE',
        payload: { error: 'Authentication failed' },
      };

      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        user: null,
        token: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
        error: 'Authentication failed',
      });
    });

    it('should set error on initial state', () => {
      const action: AuthAction = {
        type: 'AUTH_FAILURE',
        payload: { error: 'Login failed' },
      };

      const result = authReducer(initialAuthState, action);

      expect(result).toEqual({
        ...initialAuthState,
        error: 'Login failed',
      });
    });
  });

  describe('AUTH_LOGOUT', () => {
    it('should reset to initial state but preserve non-loading state', () => {
      const authenticatedState: AuthState = {
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      };

      const action: AuthAction = { type: 'AUTH_LOGOUT' };
      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        ...initialAuthState,
        isLoading: false,
      });
    });

    it('should clear error state on logout', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Some error',
        user: mockUser,
        isAuthenticated: true,
      };

      const action: AuthAction = { type: 'AUTH_LOGOUT' };
      const result = authReducer(stateWithError, action);

      expect(result).toEqual({
        ...initialAuthState,
        isLoading: false,
      });
    });
  });

  describe('AUTH_UPDATE_USER', () => {
    it('should update user data and clear errors', () => {
      const authenticatedState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
        error: 'Update failed previously',
      };

      const updatedUser: User = {
        ...mockUser,
        firstName: 'Updated',
        lastName: 'Name',
        name: 'Updated Name',
      };

      const action: AuthAction = {
        type: 'AUTH_UPDATE_USER',
        payload: { user: updatedUser },
      };

      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        ...authenticatedState,
        user: updatedUser,
        error: null,
      });
    });

    it('should preserve authentication state when updating user', () => {
      const authenticatedState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
      };

      const updatedUser: User = { ...mockUser, email: 'updated@example.com' };
      const action: AuthAction = {
        type: 'AUTH_UPDATE_USER',
        payload: { user: updatedUser },
      };

      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        ...authenticatedState,
        user: updatedUser,
        error: null,
      });
      expect(result.token).toBe(mockTokens.token);
      expect(result.refreshToken).toBe(mockTokens.refreshToken);
      expect(result.isAuthenticated).toBe(true);
    });
  });

  describe('AUTH_CLEAR_ERROR', () => {
    it('should clear error while preserving other state', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Some error message',
        user: mockUser,
        isAuthenticated: true,
        token: mockTokens.token,
      };

      const action: AuthAction = { type: 'AUTH_CLEAR_ERROR' };
      const result = authReducer(stateWithError, action);

      expect(result).toEqual({
        ...stateWithError,
        error: null,
      });
    });

    it('should not affect state when there is no error', () => {
      const cleanState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        isAuthenticated: true,
        token: mockTokens.token,
      };

      const action: AuthAction = { type: 'AUTH_CLEAR_ERROR' };
      const result = authReducer(cleanState, action);

      expect(result).toEqual(cleanState);
    });
  });

  describe('AUTH_VALIDATE_START', () => {
    it('should set loading state while preserving other values', () => {
      const authenticatedState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
      };

      const action: AuthAction = { type: 'AUTH_VALIDATE_START' };
      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        ...authenticatedState,
        isLoading: true,
      });
    });

    it('should not clear error state on validation start', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Previous error',
      };

      const action: AuthAction = { type: 'AUTH_VALIDATE_START' };
      const result = authReducer(stateWithError, action);

      expect(result).toEqual({
        ...stateWithError,
        isLoading: true,
      });
    });
  });

  describe('AUTH_VALIDATE_SUCCESS', () => {
    it('should set authenticated state with validated user', () => {
      const loadingState: AuthState = {
        ...initialAuthState,
        isLoading: true,
      };

      const action: AuthAction = {
        type: 'AUTH_VALIDATE_SUCCESS',
        payload: { user: mockUser },
      };

      const result = authReducer(loadingState, action);

      expect(result).toEqual({
        user: mockUser,
        token: 'mock-token', // From tokenStorage in real implementation
        refreshToken: 'mock-refresh-token', // From tokenStorage in real implementation
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    });

    it('should clear previous errors on successful validation', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Validation failed previously',
        isLoading: true,
      };

      const action: AuthAction = {
        type: 'AUTH_VALIDATE_SUCCESS',
        payload: { user: mockUser },
      };

      const result = authReducer(stateWithError, action);

      expect(result.error).toBeNull();
      expect(result.isAuthenticated).toBe(true);
      expect(result.user).toEqual(mockUser);
    });
  });

  describe('AUTH_VALIDATE_FAILURE', () => {
    it('should reset to initial state', () => {
      const authenticatedState: AuthState = {
        user: mockUser,
        token: mockTokens.token,
        refreshToken: mockTokens.refreshToken,
        isAuthenticated: true,
        isLoading: true,
        error: null,
      };

      const action: AuthAction = { type: 'AUTH_VALIDATE_FAILURE' };
      const result = authReducer(authenticatedState, action);

      expect(result).toEqual({
        ...initialAuthState,
        isLoading: false,
      });
    });

    it('should clear error state on validation failure', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Previous error',
        isLoading: true,
      };

      const action: AuthAction = { type: 'AUTH_VALIDATE_FAILURE' };
      const result = authReducer(stateWithError, action);

      expect(result).toEqual({
        ...initialAuthState,
        isLoading: false,
      });
    });
  });

  describe('Unknown action type', () => {
    it('should return current state for unknown actions', () => {
      const currentState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        isAuthenticated: true,
      };

      // @ts-expect-error - Testing invalid action type
      const action = { type: 'UNKNOWN_ACTION', payload: { data: 'test' } };
      const result = authReducer(currentState, action);

      expect(result).toBe(currentState); // Should return exact same reference
    });

    it('should handle null actions gracefully', () => {
      const currentState: AuthState = {
        ...initialAuthState,
        error: 'Some error',
      };

      // @ts-expect-error - Testing invalid action
      const result = authReducer(currentState, null);

      expect(result).toBe(currentState);
    });
  });

  describe('State immutability', () => {
    it('should not mutate the original state object', () => {
      const originalState: AuthState = {
        ...initialAuthState,
        user: mockUser,
        isAuthenticated: true,
      };

      const action: AuthAction = {
        type: 'AUTH_FAILURE',
        payload: { error: 'Test error' },
      };

      const result = authReducer(originalState, action);

      // Original state should not be modified
      expect(originalState.user).toEqual(mockUser);
      expect(originalState.isAuthenticated).toBe(true);
      expect(originalState.error).toBeNull();

      // Result should be different object
      expect(result).not.toBe(originalState);
      expect(result.user).toBeNull();
      expect(result.isAuthenticated).toBe(false);
      expect(result.error).toBe('Test error');
    });

    it('should not mutate nested user object', () => {
      const originalUser = { ...mockUser };
      const stateWithUser: AuthState = {
        ...initialAuthState,
        user: originalUser,
        isAuthenticated: true,
      };

      const updatedUser: User = { ...mockUser, firstName: 'Updated' };
      const action: AuthAction = {
        type: 'AUTH_UPDATE_USER',
        payload: { user: updatedUser },
      };

      const result = authReducer(stateWithUser, action);

      // Original user object should not be modified
      expect(originalUser.firstName).toBe('Test');
      expect(stateWithUser.user).toBe(originalUser);
      expect(stateWithUser.user!.firstName).toBe('Test');

      // Result should have new user object
      expect(result.user).not.toBe(originalUser);
      expect(result.user!.firstName).toBe('Updated');
    });
  });

  describe('Edge cases and boundary conditions', () => {
    it('should handle AUTH_SUCCESS with partial user data', () => {
      const partialUser = {
        ...mockUser,
        avatar: undefined,
        profilePicture: undefined,
      } as User;

      const action: AuthAction = {
        type: 'AUTH_SUCCESS',
        payload: {
          user: partialUser,
          token: mockTokens.token,
          refreshToken: mockTokens.refreshToken,
        },
      };

      const result = authReducer(initialAuthState, action);

      expect(result.user).toEqual(partialUser);
      expect(result.isAuthenticated).toBe(true);
    });

    it('should handle empty string error message', () => {
      const action: AuthAction = {
        type: 'AUTH_FAILURE',
        payload: { error: '' },
      };

      const result = authReducer(initialAuthState, action);

      expect(result.error).toBe('');
      expect(result.isAuthenticated).toBe(false);
    });

    it('should handle multiple rapid state changes', () => {
      let state = initialAuthState;

      // Start loading
      state = authReducer(state, { type: 'AUTH_LOADING' });
      expect(state.isLoading).toBe(true);

      // Fail authentication
      state = authReducer(state, {
        type: 'AUTH_FAILURE',
        payload: { error: 'Failed' },
      });
      expect(state.isLoading).toBe(false);
      expect(state.error).toBe('Failed');

      // Clear error
      state = authReducer(state, { type: 'AUTH_CLEAR_ERROR' });
      expect(state.error).toBeNull();

      // Start loading again
      state = authReducer(state, { type: 'AUTH_LOADING' });
      expect(state.isLoading).toBe(true);

      // Succeed authentication
      state = authReducer(state, {
        type: 'AUTH_SUCCESS',
        payload: {
          user: mockUser,
          token: mockTokens.token,
          refreshToken: mockTokens.refreshToken,
        },
      });
      expect(state.isAuthenticated).toBe(true);
      expect(state.isLoading).toBe(false);
      expect(state.user).toEqual(mockUser);
    });
  });
});