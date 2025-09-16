import React from 'react';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {useAuth, useAuthActions, useAuthState} from '../useAuth';
import {AuthProvider} from '../../contexts/AuthContext';
import {AuthActionsContext, AuthStateContext} from '../../contexts/authContexts';
import type {AuthState, User} from '@shared/types/auth';

// Mock the auth API service
vi.mock('../../../../services/api/authApi', () => {
  const mockTokenStorage = {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setAccessToken: vi.fn(),
    setRefreshToken: vi.fn(),
    clearAllTokens: vi.fn(),
    hasValidTokens: vi.fn().mockReturnValue(false),
  };

  const mockAuthApi = {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    requestPasswordReset: vi.fn(),
    validateAuth: vi.fn(),
    isAuthenticated: vi.fn(),
    getAccessToken: vi.fn(),
  };

  return {
    default: mockAuthApi,
    tokenStorage: mockTokenStorage,
  };
});

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

const mockAuthState: AuthState = {
  user: mockUser,
  token: 'mock-token',
  refreshToken: 'mock-refresh-token',
  isLoading: false,
  isAuthenticated: true,
  error: null,
};

const mockAuthActions = {
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  refreshAuth: vi.fn(),
  updateProfile: vi.fn(),
  changePassword: vi.fn(),
  requestPasswordReset: vi.fn(),
  clearError: vi.fn(),
};

describe('useAuth hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('useAuthState', () => {
    it('should return auth state when used within AuthProvider', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthProvider>{children}</AuthProvider>
      );

      const {result} = renderHook(() => useAuthState(), {wrapper});

      expect(result.current).toBeDefined();
      expect(result.current).toHaveProperty('user');
      expect(result.current).toHaveProperty('token');
      expect(result.current).toHaveProperty('refreshToken');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('isAuthenticated');
      expect(result.current).toHaveProperty('error');
    });

    it('should throw error when used outside AuthProvider', () => {
      expect(() => {
        renderHook(() => useAuthState());
      }).toThrow('useAuthState must be used within an AuthProvider');
    });

    it('should return current auth state from context', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthStateContext.Provider value={mockAuthState}>
            {children}
          </AuthStateContext.Provider>
      );

      const {result} = renderHook(() => useAuthState(), {wrapper});

      expect(result.current).toEqual(mockAuthState);
    });

    it('should throw error when context value is null', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthStateContext.Provider value={null}>
            {children}
          </AuthStateContext.Provider>
      );

      expect(() => {
        renderHook(() => useAuthState(), {wrapper});
      }).toThrow('useAuthState must be used within an AuthProvider');
    });
  });

  describe('useAuthActions', () => {
    it('should return auth actions when used within AuthProvider', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthProvider>{children}</AuthProvider>
      );

      const {result} = renderHook(() => useAuthActions(), {wrapper});

      expect(result.current).toBeDefined();
      expect(result.current).toHaveProperty('login');
      expect(result.current).toHaveProperty('register');
      expect(result.current).toHaveProperty('logout');
      expect(result.current).toHaveProperty('refreshAuth');
      expect(result.current).toHaveProperty('updateProfile');
      expect(result.current).toHaveProperty('clearError');

      // Verify all actions are functions
      expect(typeof result.current.login).toBe('function');
      expect(typeof result.current.register).toBe('function');
      expect(typeof result.current.logout).toBe('function');
      expect(typeof result.current.refreshAuth).toBe('function');
      expect(typeof result.current.updateProfile).toBe('function');
      expect(typeof result.current.clearError).toBe('function');
    });

    it('should throw error when used outside AuthProvider', () => {
      expect(() => {
        renderHook(() => useAuthActions());
      }).toThrow('useAuthActions must be used within an AuthProvider');
    });

    it('should return current auth actions from context', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthActionsContext.Provider value={mockAuthActions}>
            {children}
          </AuthActionsContext.Provider>
      );

      const {result} = renderHook(() => useAuthActions(), {wrapper});

      expect(result.current).toEqual(mockAuthActions);
    });

    it('should throw error when context value is null', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthActionsContext.Provider value={null}>
            {children}
          </AuthActionsContext.Provider>
      );

      expect(() => {
        renderHook(() => useAuthActions(), {wrapper});
      }).toThrow('useAuthActions must be used within an AuthProvider');
    });
  });

  describe('useAuth', () => {
    it('should return combined auth state and actions when used within AuthProvider', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthProvider>{children}</AuthProvider>
      );

      const {result} = renderHook(() => useAuth(), {wrapper});

      expect(result.current).toBeDefined();

      // Should have authState property
      expect(result.current).toHaveProperty('authState');
      expect(result.current.authState).toHaveProperty('user');
      expect(result.current.authState).toHaveProperty('token');
      expect(result.current.authState).toHaveProperty('isAuthenticated');
      expect(result.current.authState).toHaveProperty('isLoading');
      expect(result.current.authState).toHaveProperty('error');

      // Should have action functions
      expect(result.current).toHaveProperty('login');
      expect(result.current).toHaveProperty('register');
      expect(result.current).toHaveProperty('logout');
      expect(result.current).toHaveProperty('refreshAuth');
      expect(result.current).toHaveProperty('updateProfile');
      expect(result.current).toHaveProperty('clearError');
    });

    it('should throw error when used outside AuthProvider', () => {
      expect(() => {
        renderHook(() => useAuth());
      }).toThrow('useAuthState must be used within an AuthProvider');
    });

    it('should return combined state and actions from separate contexts', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthStateContext.Provider value={mockAuthState}>
            <AuthActionsContext.Provider value={mockAuthActions}>
              {children}
            </AuthActionsContext.Provider>
          </AuthStateContext.Provider>
      );

      const {result} = renderHook(() => useAuth(), {wrapper});

      expect(result.current.authState).toEqual(mockAuthState);
      expect(result.current.login).toBe(mockAuthActions.login);
      expect(result.current.register).toBe(mockAuthActions.register);
      expect(result.current.logout).toBe(mockAuthActions.logout);
      expect(result.current.refreshAuth).toBe(mockAuthActions.refreshAuth);
      expect(result.current.updateProfile).toBe(mockAuthActions.updateProfile);
      expect(result.current.clearError).toBe(mockAuthActions.clearError);
    });

    it('should provide functional actions that work consistently', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthProvider>{children}</AuthProvider>
      );

      const {result, rerender} = renderHook(() => useAuth(), {wrapper});

      // Actions should be functions
      expect(typeof result.current.login).toBe('function');
      expect(typeof result.current.register).toBe('function');
      expect(typeof result.current.logout).toBe('function');
      expect(typeof result.current.refreshAuth).toBe('function');
      expect(typeof result.current.updateProfile).toBe('function');
      expect(typeof result.current.clearError).toBe('function');

      // Force re-render
      rerender();

      // Actions should still be functions after re-render
      expect(typeof result.current.login).toBe('function');
      expect(typeof result.current.register).toBe('function');
      expect(typeof result.current.logout).toBe('function');
      expect(typeof result.current.refreshAuth).toBe('function');
      expect(typeof result.current.updateProfile).toBe('function');
      expect(typeof result.current.clearError).toBe('function');
    });
  });

  describe('Hook type safety', () => {
    it('should ensure useAuthState returns correct AuthState type', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthStateContext.Provider value={mockAuthState}>
            {children}
          </AuthStateContext.Provider>
      );

      const {result} = renderHook(() => useAuthState(), {wrapper});

      // TypeScript should infer correct types
      const state = result.current;
      expect(typeof state.isAuthenticated).toBe('boolean');
      expect(typeof state.isLoading).toBe('boolean');
      expect(state.user === null || typeof state.user === 'object').toBe(true);
      expect(state.token === null || typeof state.token === 'string').toBe(true);
      expect(state.refreshToken === null || typeof state.refreshToken === 'string').toBe(true);
      expect(state.error === null || typeof state.error === 'string').toBe(true);
    });

    it('should ensure useAuthActions returns correct action types', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthActionsContext.Provider value={mockAuthActions}>
            {children}
          </AuthActionsContext.Provider>
      );

      const {result} = renderHook(() => useAuthActions(), {wrapper});

      // All actions should be functions
      const actions = result.current;
      expect(typeof actions.login).toBe('function');
      expect(typeof actions.register).toBe('function');
      expect(typeof actions.logout).toBe('function');
      expect(typeof actions.refreshAuth).toBe('function');
      expect(typeof actions.updateProfile).toBe('function');
      expect(typeof actions.clearError).toBe('function');
    });

    it('should ensure useAuth returns correct combined type', () => {
      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthStateContext.Provider value={mockAuthState}>
            <AuthActionsContext.Provider value={mockAuthActions}>
              {children}
            </AuthActionsContext.Provider>
          </AuthStateContext.Provider>
      );

      const {result} = renderHook(() => useAuth(), {wrapper});

      const auth = result.current;

      // Should have authState property with correct structure
      expect(auth.authState).toBeDefined();
      expect(typeof auth.authState.isAuthenticated).toBe('boolean');
      expect(typeof auth.authState.isLoading).toBe('boolean');

      // Should have action functions
      expect(typeof auth.login).toBe('function');
      expect(typeof auth.register).toBe('function');
      expect(typeof auth.logout).toBe('function');
      expect(typeof auth.refreshAuth).toBe('function');
      expect(typeof auth.updateProfile).toBe('function');
      expect(typeof auth.clearError).toBe('function');
    });
  });

  describe('Error handling edge cases', () => {
    it('should handle useAuthState with undefined context gracefully', () => {
      // This tests the error boundary case
      const {result} = renderHook(() => {
        try {
          return useAuthState();
        } catch (error) {
          return {error: (error as Error).message};
        }
      });

      expect(result.current).toEqual({
        error: 'useAuthState must be used within an AuthProvider'
      });
    });

    it('should handle useAuthActions with undefined context gracefully', () => {
      const {result} = renderHook(() => {
        try {
          return useAuthActions();
        } catch (error) {
          return {error: (error as Error).message};
        }
      });

      expect(result.current).toEqual({
        error: 'useAuthActions must be used within an AuthProvider'
      });
    });

    it('should handle nested provider scenarios', () => {
      const outerState: AuthState = {...mockAuthState, user: null};
      const innerState: AuthState = {...mockAuthState, isAuthenticated: false};

      const wrapper = ({children}: { children: React.ReactNode }) => (
          <AuthStateContext.Provider value={outerState}>
            <AuthStateContext.Provider value={innerState}>
              {children}
            </AuthStateContext.Provider>
          </AuthStateContext.Provider>
      );

      const {result} = renderHook(() => useAuthState(), {wrapper});

      // Should use the inner (most recent) provider
      expect(result.current).toEqual(innerState);
      expect(result.current.isAuthenticated).toBe(false);
    });
  });
});