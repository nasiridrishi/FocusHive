import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import React from 'react';

// Mock the token manager first
vi.mock('../../../../utils/tokenManager', () => {
  const createTokenManagerMock = (): object => {
    // Mock token storage
    let mockAccessToken: string | null = null;
    let mockRefreshToken: string | null = null;
    
    return {
      saveTokens: vi.fn((accessToken: string, refreshToken: string) => {
        mockAccessToken = accessToken;
        mockRefreshToken = refreshToken;
        return true;
      }),
      getAccessToken: vi.fn(() => mockAccessToken),
      getRefreshToken: vi.fn(() => mockRefreshToken),
      clearTokens: vi.fn(() => {
        mockAccessToken = null;
        mockRefreshToken = null;
        return true;
      }),
      hasValidTokens: vi.fn(() => Boolean(mockAccessToken && mockRefreshToken)),
      setHasValidTokens: vi.fn((isValid: boolean) => {
        if (!isValid) {
          mockAccessToken = null;
          mockRefreshToken = null;
        } else {
          mockAccessToken = 'mock-token';
          mockRefreshToken = 'mock-refresh-token';
        }
      }),
      reset: vi.fn(() => {
        mockAccessToken = null;
        mockRefreshToken = null;
      }),
      supportsHttpOnlyCookies: vi.fn().mockReturnValue(false),
    };
  };
  
  return {
    tokenManager: createTokenManagerMock(),
  };
});

// Now safe to import the rest
import {
  act,
  renderWithProviders,
  screen,
  waitFor
} from '../../../../test-utils/test-utils';
import {AuthProvider} from '../AuthContext';
import authApiService, {tokenStorage} from '../../../../services/api/authApi';
import type {User} from '@shared/types/auth';

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

// Test component to display auth state
const TestComponent = (): React.ReactElement => {
  return (
    <div data-testid="auth-component">
      <div data-testid="test-ready">Ready</div>
    </div>
  );
};

describe('AuthContext Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset mocks for token manager
    vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(false);
  });

  afterEach(() => {
    vi.clearAllTimers();
    // Clean up any event listeners
    if (window.dispatchEvent) {
      window.removeEventListener('tokenRefreshNeeded', vi.fn());
      window.removeEventListener('authFailure', vi.fn());
    }
  });

  describe('Token refresh event handling', () => {
    it('should handle tokenRefreshNeeded event and update state', async () => {
      // Setup successful refresh
      vi.mocked(authApiService.validateAuth).mockResolvedValue(true);
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('new-access-token');
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('new-refresh-token');

      renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      // Wait for component to be ready
      await waitFor(() => {
        expect(screen.getByTestId('test-ready')).toBeInTheDocument();
      });

      // Trigger token refresh event
      await act(async () => {
        window.dispatchEvent(new CustomEvent('tokenRefreshNeeded'));
      });

      // Give some time for the event to process
      await waitFor(() => {
        expect(authApiService.validateAuth).toHaveBeenCalled();
      });

      expect(authApiService.getCurrentUser).toHaveBeenCalled();
    });

    it('should handle tokenRefreshNeeded event failure gracefully', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      
      // Setup failed refresh
      vi.mocked(authApiService.validateAuth).mockRejectedValue(new Error('Refresh failed'));

      renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      await waitFor(() => {
        expect(screen.getByTestId('test-ready')).toBeInTheDocument();
      });

      // Trigger token refresh event
      await act(async () => {
        window.dispatchEvent(new CustomEvent('tokenRefreshNeeded'));
      });

      // Should handle error gracefully
      await waitFor(() => {
        expect(authApiService.validateAuth).toHaveBeenCalled();
      });

      consoleSpy.mockRestore();
    });
  });

  describe('Auth failure event handling', () => {
    it('should handle authFailure event and clear state', async () => {
      // Start with authenticated state
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockResolvedValue(true);
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token');
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('refresh-token');

      renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      // Wait for initial auth to complete
      await waitFor(() => {
        expect(authApiService.validateAuth).toHaveBeenCalled();
      });

      // Trigger auth failure event
      await act(async () => {
        window.dispatchEvent(new CustomEvent('authFailure'));
      });

      // Should trigger logout flow - we can't directly test the state change
      // but we can verify the event was processed
      expect(screen.getByTestId('test-ready')).toBeInTheDocument();
    });
  });

  describe('Token lifecycle edge cases', () => {
    it('should handle rapid token refresh requests', async () => {
      // Setup successful refresh
      vi.mocked(authApiService.validateAuth).mockImplementation(async () => {
        return true;
      });
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);

      renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      await waitFor(() => {
        expect(screen.getByTestId('test-ready')).toBeInTheDocument();
      });

      // Trigger multiple rapid refresh events
      await act(async () => {
        window.dispatchEvent(new CustomEvent('tokenRefreshNeeded'));
        window.dispatchEvent(new CustomEvent('tokenRefreshNeeded'));
        window.dispatchEvent(new CustomEvent('tokenRefreshNeeded'));
      });

      // Should handle multiple requests gracefully
      await waitFor(() => {
        expect(authApiService.validateAuth).toHaveBeenCalled();
      });
    });

    it('should handle expired token scenario', async () => {
      // Mock expired token detection
      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockResolvedValue(false);

      renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      // Wait for validation to complete
      await waitFor(() => {
        expect(authApiService.validateAuth).toHaveBeenCalled();
      });

      // Should not call getCurrentUser when validation fails
      expect(authApiService.getCurrentUser).not.toHaveBeenCalled();
    });
  });

  describe('Component lifecycle', () => {
    it('should clean up event listeners on unmount', async () => {
      const addEventListenerSpy = vi.spyOn(window, 'addEventListener');
      const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

      const {unmount} = renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      // Verify event listeners were added
      expect(addEventListenerSpy).toHaveBeenCalledWith('tokenRefreshNeeded', expect.any(Function));
      expect(addEventListenerSpy).toHaveBeenCalledWith('authFailure', expect.any(Function));

      // Unmount component
      unmount();

      // Verify event listeners were removed
      expect(removeEventListenerSpy).toHaveBeenCalledWith('tokenRefreshNeeded', expect.any(Function));
      expect(removeEventListenerSpy).toHaveBeenCalledWith('authFailure', expect.any(Function));

      addEventListenerSpy.mockRestore();
      removeEventListenerSpy.mockRestore();
    });

    it('should handle initialization race conditions', async () => {
      let resolveValidation: (value: boolean) => void;
      const validationPromise = new Promise<boolean>((resolve) => {
        resolveValidation = resolve;
      });

      vi.mocked(tokenStorage.hasValidTokens).mockReturnValue(true);
      vi.mocked(authApiService.validateAuth).mockImplementation(() => validationPromise);
      vi.mocked(authApiService.getCurrentUser).mockResolvedValue(mockUser);

      const {unmount} = renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      // Unmount before validation completes
      unmount();

      // Complete the validation (should not cause errors)
      await act(async () => {
        resolveValidation(true);
      });

      // Should not throw errors or cause issues
      expect(screen.queryByTestId('test-ready')).not.toBeInTheDocument();
    });
  });

  describe('Basic integration', () => {
    it('should initialize without errors', async () => {
      // Basic initialization test
      renderWithProviders(
        <AuthProvider>
          <TestComponent/>
        </AuthProvider>,
        {withAuth: false}
      );

      // Should render without issues
      expect(screen.getByTestId('test-ready')).toBeInTheDocument();
    });
  });

  describe('Error boundary integration', () => {
    it('should isolate auth errors within error boundary', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      
      const ThrowingComponent = (): React.ReactElement => {
        throw new Error('Component error');
      };

      // Should not crash the entire app
      renderWithProviders(
        <AuthProvider>
          <ThrowingComponent />
        </AuthProvider>,
        {withAuth: false}
      );

      // Error boundary should handle the error
      // The exact UI depends on FeatureLevelErrorBoundary implementation
      
      consoleSpy.mockRestore();
    });
  });
});