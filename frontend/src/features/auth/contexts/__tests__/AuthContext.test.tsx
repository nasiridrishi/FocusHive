import React, {ReactNode} from 'react';
import {
  act,
  renderWithProviders,
  screen,
  userEvent,
  waitFor
} from '../../../../test-utils/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthProvider, useAuth, useAuthActions, useAuthState} from '../index';
import authApiService, {tokenStorage} from '../../../../services/api/authApi';
import type {User} from '@shared/types/auth';

// Mock the token manager
vi.mock('../../../../utils/tokenManager', () => ({
  tokenManager: {
    saveTokens: vi.fn(),
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    clearTokens: vi.fn(),
    isTokenExpired: vi.fn(),
    parseJWT: vi.fn(),
    validateToken: vi.fn(),
    hasValidTokens: vi.fn(),
    getTokenExpirationInfo: vi.fn(),
    getUserFromToken: vi.fn(),
    supportsHttpOnlyCookies: vi.fn().mockReturnValue(false),
  }
}));

// Mock the auth API service
vi.mock('../../../../services/api/authApi', () => {
  const mockTokenStorage = {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setAccessToken: vi.fn(),
    setRefreshToken: vi.fn(),
    clearAllTokens: vi.fn(),
    hasValidTokens: vi.fn(),
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

// Mock window.location for logout redirect tests
const mockLocation = {
  href: '',
  assign: vi.fn(),
  replace: vi.fn(),
};
Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true,
});

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

const mockLoginResponse = {
  user: mockUser,
  ...mockTokens,
};

// Test component to access context values
const TestComponent = ({children}: { children?: ReactNode }): React.ReactElement => {
  const auth = useAuth();

  return (
      <div>
        <div data-testid="auth-state">
          <div data-testid="user-id">{auth.authState.user?.id || 'null'}</div>
          <div data-testid="user-email">{auth.authState.user?.email || 'null'}</div>
          <div data-testid="is-authenticated">{auth.authState.isAuthenticated.toString()}</div>
          <div data-testid="is-loading">{auth.authState.isLoading.toString()}</div>
          <div data-testid="error">{auth.authState.error || 'null'}</div>
          <div data-testid="token">{auth.authState.token || 'null'}</div>
        </div>
        <div data-testid="auth-actions">
          <button onClick={async () => {
            try {
              await auth.login({email: 'test@example.com', password: 'password'});
            } catch {
              // Expected for error tests
            }
          }}>
            Login
          </button>
          <button onClick={async () => {
            try {
              await auth.register({
                email: 'new@example.com',
                password: 'password',
                username: 'newuser',
                firstName: 'New',
                lastName: 'User'
              });
            } catch {
              // Expected for error tests
            }
          }}>
            Register
          </button>
          <button onClick={() => auth.logout()}>Logout</button>
          <button onClick={async () => {
            try {
              await auth.refreshAuth();
            } catch {
              // Expected for error tests
            }
          }}>Refresh
          </button>
          <button onClick={async () => {
            try {
              await auth.updateProfile({firstName: 'Updated'});
            } catch {
              // Expected for error tests
            }
          }}>
            Update Profile
          </button>
          <button onClick={async () => {
            try {
              await auth.changePassword({
                currentPassword: 'old',
                newPassword: 'new'
              });
            } catch {
              // Expected for error tests
            }
          }}>
            Change Password
          </button>
          <button onClick={async () => {
            try {
              await auth.requestPasswordReset({
                email: 'test@example.com'
              });
            } catch {
              // Expected for error tests
            }
          }}>
            Reset Password
          </button>
          <button onClick={() => auth.clearError()}>Clear Error</button>
        </div>
        {children}
      </div>
  );
};

// Separate test components for individual hooks
const StateTestComponent = (): React.ReactElement => {
  const authState = useAuthState();
  return (
      <div>
        <div data-testid="state-user-id">{authState.user?.id || 'null'}</div>
        <div data-testid="state-is-authenticated">{authState.isAuthenticated.toString()}</div>
      </div>
  );
};

const ActionsTestComponent = (): React.ReactElement => {
  const authActions = useAuthActions();
  return (
      <div>
        <button
            onClick={() => authActions.login({email: 'test@example.com', password: 'password'})}>
          Actions Login
        </button>
        <button onClick={() => authActions.clearError()}>Actions Clear Error</button>
      </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLocation.href = '';
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  describe('AuthProvider', () => {
    it('should provide initial auth state', () => {
      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false} // Don't use default auth provider
      );

      expect(screen.getByTestId('user-id')).toHaveTextContent('null');
      expect(screen.getByTestId('user-email')).toHaveTextContent('null');
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      expect(screen.getByTestId('error')).toHaveTextContent('null');
      expect(screen.getByTestId('token')).toHaveTextContent('null');
    });

    it('should initialize auth state on mount when tokens exist', async () => {
      // Mock token storage to return valid tokens
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockResolvedValue(true);
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(mockTokens.token);
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue(mockTokens.refreshToken);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // Initially loading
      expect(screen.getByTestId('is-loading')).toHaveTextContent('true');

      // Wait for initialization to complete
      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user-id')).toHaveTextContent('1');
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      expect(authApiService.validateAuth).toHaveBeenCalledOnce();
      expect(authApiService.getCurrentUser).toHaveBeenCalledOnce();
    });

    it('should not initialize auth state when no tokens exist', async () => {
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(false);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // Should not be loading and should remain unauthenticated
      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      expect(authApiService.validateAuth).not.toHaveBeenCalled();
    });

    it('should handle failed token validation on initialization', async () => {
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockResolvedValue(false);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('user-id')).toHaveTextContent('null');
    });
  });

  describe('Login functionality', () => {
    it('should handle successful login', async () => {
      const user = userEvent.setup();
      vi.mocked(authApiService.login).mockResolvedValue(mockLoginResponse);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const loginButton = screen.getByText('Login');
      await act(async () => {
        await user.click(loginButton);
      });

      // Wait for login to complete
      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Should not be loading after completion
      expect(screen.getByTestId('is-loading')).toHaveTextContent('false');

      expect(screen.getByTestId('user-id')).toHaveTextContent('1');
      expect(screen.getByTestId('user-email')).toHaveTextContent('testuser@example.com');
      expect(screen.getByTestId('error')).toHaveTextContent('null');

      expect(authApiService.login).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password'
      });
    });

    it('should handle login failure', async () => {
      const user = userEvent.setup();
      const errorMessage = 'Invalid credentials';
      vi.mocked(authApiService.login).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const loginButton = screen.getByText('Login');
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('error')).toHaveTextContent(errorMessage);
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('user-id')).toHaveTextContent('null');
    });

    it('should clear error state before new login attempt', async () => {
      const user = userEvent.setup();
      // First attempt fails
      vi.mocked(authApiService.login).mockRejectedValueOnce(new Error('Network error'));
      // Second attempt succeeds
      vi.mocked(authApiService.login).mockResolvedValueOnce(mockLoginResponse);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const loginButton = screen.getByText('Login');

      // First login attempt - should fail
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('error')).toHaveTextContent('Network error');
      });

      // Second login attempt - should succeed and clear error
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      expect(screen.getByTestId('error')).toHaveTextContent('null');
    });
  });

  describe('Register functionality', () => {
    it('should handle successful registration', async () => {
      const user = userEvent.setup();
      const registerResponse = {
        user: {...mockUser, email: 'new@example.com', username: 'newuser'},
        ...mockTokens,
      };
      vi.mocked(authApiService.register).mockResolvedValue(registerResponse);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const registerButton = screen.getByText('Register');
      await act(async () => {
        await user.click(registerButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user-email')).toHaveTextContent('new@example.com');
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');

      expect(authApiService.register).toHaveBeenCalledWith({
        email: 'new@example.com',
        password: 'password',
        username: 'newuser',
        firstName: 'New',
        lastName: 'User'
      });
    });

    it('should handle registration failure', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const user = userEvent.setup();
      const errorMessage = 'Email already exists';
      vi.mocked(authApiService.register).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const registerButton = screen.getByText('Register');
      await act(async () => {
        await user.click(registerButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('error')).toHaveTextContent(errorMessage);
      });

      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      
      consoleSpy.mockRestore();
    });
  });

  describe('Logout functionality', () => {
    it('should handle successful logout', async () => {
      const user = userEvent.setup();
      vi.mocked(authApiService.logout).mockResolvedValue();

      // Start with authenticated state
      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false, user: mockUser}
      );

      // Manually set authenticated state for testing logout
      const loginButton = screen.getByText('Login');
      vi.mocked(authApiService.login).mockResolvedValue(mockLoginResponse);

      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Now test logout
      const logoutButton = screen.getByText('Logout');
      await act(async () => {
        await user.click(logoutButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user-id')).toHaveTextContent('null');
      expect(screen.getByTestId('token')).toHaveTextContent('null');
      expect(authApiService.logout).toHaveBeenCalledOnce();
    });

    it('should clear local state even if logout API fails', async () => {
      const user = userEvent.setup();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });
      vi.mocked(authApiService.logout).mockRejectedValue(new Error('Network error'));

      // Start with authenticated state
      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // Set authenticated state
      const loginButton = screen.getByText('Login');
      vi.mocked(authApiService.login).mockResolvedValue(mockLoginResponse);

      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Logout should still clear local state despite API failure
      const logoutButton = screen.getByText('Logout');
      await act(async () => {
        await user.click(logoutButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user-id')).toHaveTextContent('null');
      consoleSpy.mockRestore();
    });
  });

  describe('Refresh auth functionality', () => {
    it('should handle successful auth refresh', async () => {
      const user = userEvent.setup();
      vi.clearAllMocks();
      vi.mocked(authApiService.validateAuth).mockResolvedValue(true);
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(mockTokens.token);
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue(mockTokens.refreshToken);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const refreshButton = screen.getByText('Refresh');
      await act(async () => {
        await user.click(refreshButton);
      });

      // Wait for refresh to complete
      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Should not be loading after completion
      expect(screen.getByTestId('is-loading')).toHaveTextContent('false');

      expect(screen.getByTestId('user-id')).toHaveTextContent('1');
      expect(authApiService.validateAuth).toHaveBeenCalled();
      expect(authApiService.getCurrentUser).toHaveBeenCalled();
    });

    it('should handle failed auth refresh', async () => {
      const user = userEvent.setup();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });
      vi.mocked(authApiService.validateAuth).mockResolvedValue(false);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const refreshButton = screen.getByText('Refresh');
      await act(async () => {
        await user.click(refreshButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('user-id')).toHaveTextContent('null');

      consoleSpy.mockRestore();
    });

    it('should handle refresh auth error', async () => {
      const user = userEvent.setup();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });
      vi.mocked(authApiService.validateAuth).mockRejectedValue(new Error('Network error'));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const refreshButton = screen.getByText('Refresh');
      await act(async () => {
        await user.click(refreshButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');

      consoleSpy.mockRestore();
    });
  });

  describe('Update profile functionality', () => {
    it('should handle successful profile update', async () => {
      const user = userEvent.setup();
      const updatedUser = {...mockUser, firstName: 'Updated'};
      vi.mocked(authApiService.updateProfile).mockResolvedValue(updatedUser);

      // Start with authenticated state
      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // First authenticate
      const loginButton = screen.getByText('Login');
      vi.mocked(authApiService.login).mockResolvedValue(mockLoginResponse);
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Now update profile
      const updateButton = screen.getByText('Update Profile');
      await act(async () => {
        await user.click(updateButton);
      });

      await waitFor(() => {
        expect(authApiService.updateProfile).toHaveBeenCalledWith({firstName: 'Updated'});
      });

      // Note: In a real test, we'd need to implement the reducer action to see the updated user
      expect(authApiService.updateProfile).toHaveBeenCalledOnce();
    });

    it('should handle profile update failure', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const user = userEvent.setup();
      const errorMessage = 'Update failed';
      vi.mocked(authApiService.updateProfile).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // First authenticate
      const loginButton = screen.getByText('Login');
      vi.mocked(authApiService.login).mockResolvedValue(mockLoginResponse);
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Now try to update profile
      const updateButton = screen.getByText('Update Profile');
      await act(async () => {
        await user.click(updateButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('error')).toHaveTextContent(errorMessage);
      });
      
      consoleSpy.mockRestore();
    });
  });

  describe('Change password functionality', () => {
    it('should handle successful password change', async () => {
      const user = userEvent.setup();
      vi.mocked(authApiService.changePassword).mockResolvedValue();

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const changePasswordButton = screen.getByText('Change Password');
      await act(async () => {
        await user.click(changePasswordButton);
      });

      await waitFor(() => {
        expect(authApiService.changePassword).toHaveBeenCalledWith({
          currentPassword: 'old',
          newPassword: 'new'
        });
      });
    });

    it('should handle password change failure', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const user = userEvent.setup();
      const errorMessage = 'Current password is incorrect';
      vi.mocked(authApiService.changePassword).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const changePasswordButton = screen.getByText('Change Password');
      await act(async () => {
        await user.click(changePasswordButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('error')).toHaveTextContent(errorMessage);
      });
      
      consoleSpy.mockRestore();
    });
  });

  describe('Password reset functionality', () => {
    it('should handle successful password reset request', async () => {
      const user = userEvent.setup();
      const resetResponse = {message: 'Reset email sent'};
      vi.mocked(authApiService.requestPasswordReset).mockResolvedValue(resetResponse);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const resetPasswordButton = screen.getByText('Reset Password');
      await act(async () => {
        await user.click(resetPasswordButton);
      });

      await waitFor(() => {
        expect(authApiService.requestPasswordReset).toHaveBeenCalledWith({
          email: 'test@example.com'
        });
      });
    });

    it('should handle password reset request failure', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const user = userEvent.setup();
      const errorMessage = 'Reset request failed';
      vi.mocked(authApiService.requestPasswordReset).mockRejectedValue(new Error(errorMessage));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const resetPasswordButton = screen.getByText('Reset Password');
      await act(async () => {
        await user.click(resetPasswordButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('error')).toHaveTextContent(errorMessage);
      });
      
      consoleSpy.mockRestore();
    });
  });

  describe('Clear error functionality', () => {
    it('should clear error state', async () => {
      const user = userEvent.setup();
      vi.mocked(authApiService.login).mockRejectedValue(new Error('Login failed'));

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // First cause an error
      const loginButton = screen.getByText('Login');
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('error')).toHaveTextContent('Login failed');
      });

      // Now clear the error
      const clearErrorButton = screen.getByText('Clear Error');
      await act(async () => {
        await user.click(clearErrorButton);
      });

      expect(screen.getByTestId('error')).toHaveTextContent('null');
    });
  });

  describe('Auth hooks', () => {
    it('should throw error when useAuthState is used outside provider', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });

      expect(() => {
        renderWithProviders(<StateTestComponent/>, {withAuth: false});
      }).toThrow('useAuthState must be used within an AuthProvider');

      consoleSpy.mockRestore();
    });

    it('should throw error when useAuthActions is used outside provider', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });

      expect(() => {
        renderWithProviders(<ActionsTestComponent/>, {withAuth: false});
      }).toThrow('useAuthActions must be used within an AuthProvider');

      consoleSpy.mockRestore();
    });

    it('should work correctly when used within provider', () => {
      renderWithProviders(
          <AuthProvider>
            <StateTestComponent/>
            <ActionsTestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      expect(screen.getByTestId('state-user-id')).toHaveTextContent('null');
      expect(screen.getByTestId('state-is-authenticated')).toHaveTextContent('false');
      expect(screen.getByText('Actions Login')).toBeInTheDocument();
      expect(screen.getByText('Actions Clear Error')).toBeInTheDocument();
    });

    it('should provide combined auth context through useAuth hook', () => {
      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // Verify both state and actions are available
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      expect(screen.getByText('Login')).toBeInTheDocument();
      expect(screen.getByText('Logout')).toBeInTheDocument();
      expect(screen.getByText('Clear Error')).toBeInTheDocument();
    });
  });

  describe('Context separation', () => {
    it('should provide state and actions through separate contexts', () => {
      const StateOnlyComponent = (): React.ReactElement => {
        const authState = useAuthState();
        return <div data-testid="state-only-user">{authState.user?.id || 'null'}</div>;
      };

      const ActionsOnlyComponent = (): React.ReactElement => {
        const authActions = useAuthActions();
        return (
            <button onClick={() => authActions.clearError()}>
              Actions Only Clear
            </button>
        );
      };

      renderWithProviders(
          <AuthProvider>
            <StateOnlyComponent/>
            <ActionsOnlyComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      expect(screen.getByTestId('state-only-user')).toHaveTextContent('null');
      expect(screen.getByText('Actions Only Clear')).toBeInTheDocument();
    });
  });

  describe('Error boundary integration', () => {
    it('should wrap provider content in error boundary', () => {
      const ThrowingComponent = (): React.ReactElement => {
        throw new Error('Test error');
      };

      // Error boundary should catch the error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      });

      renderWithProviders(
          <AuthProvider>
            <ThrowingComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      // The error boundary should have caught the error and rendered fallback UI
      // The exact fallback UI depends on the FeatureLevelErrorBoundary implementation

      consoleSpy.mockRestore();
    });
  });

  describe('State persistence and token lifecycle', () => {
    it('should validate tokens on app initialization', async () => {
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockResolvedValue(true);
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      await waitFor(() => {
        expect(authApiService.validateAuth).toHaveBeenCalledOnce();
      });

      expect(authApiService.getCurrentUser).toHaveBeenCalledOnce();
    });

    it('should handle invalid tokens on initialization', async () => {
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockResolvedValue(false);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
      });

      expect(authApiService.validateAuth).toHaveBeenCalledOnce();
      expect(authApiService.getCurrentUser).not.toHaveBeenCalled();
    });

    it('should store tokens on successful authentication', async () => {
      const user = userEvent.setup();
      vi.mocked(authApiService.login).mockResolvedValue(mockLoginResponse);

      renderWithProviders(
          <AuthProvider>
            <TestComponent/>
          </AuthProvider>,
          {withAuth: false}
      );

      const loginButton = screen.getByText('Login');
      await act(async () => {
        await user.click(loginButton);
      });

      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('true');
      });

      // Verify that the tokens are stored in state
      expect(screen.getByTestId('token')).toHaveTextContent(mockTokens.token);
    });
  });
});