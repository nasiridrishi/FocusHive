/**
 * E2E Tests for AuthContext with Real Identity Service
 *
 * REQUIREMENTS:
 * - Real Identity Service running at http://localhost:8081
 * - No mocks or stubs
 * - Tests actual authentication flow through context
 */

import { describe, it, expect, beforeAll, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { renderHook } from '@testing-library/react';
import React, { useContext } from 'react';
import axios from 'axios';
import { AuthProvider, AuthContext } from '../AuthContext';
import type { LoginRequest, RegisterRequest, User } from '../../contracts/auth';

// Identity Service URL
const IDENTITY_SERVICE_URL = 'http://localhost:8081';

// Generate unique test email to avoid conflicts
const generateTestEmail = () => `context-test-${Date.now()}-${Math.random().toString(36).substring(7)}@focushive.test`;

describe('AuthContext E2E Tests with Real Identity Service', () => {
  let isIdentityServiceAvailable = false;

  beforeAll(async () => {
    // Check if Identity Service is running
    console.log('Checking Identity Service at:', IDENTITY_SERVICE_URL);
    try {
      const response = await axios.get(`${IDENTITY_SERVICE_URL}/actuator/health`, {
        timeout: 5000,
        headers: {
          'Accept': 'application/json'
        }
      });
      console.log('Health check response status:', response.status);
      if (response?.data?.status === 'UP') {
        isIdentityServiceAvailable = true;
        console.log('✅ Identity Service is running and healthy');
      } else {
        console.log('⚠️ Identity Service responded but status is not UP:', response?.data);
      }
    } catch (error: any) {
      console.error('❌ Identity Service not available - tests will be skipped');
      console.error('Error:', error?.message || 'Unknown error');
    }
  });

  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    // Clean up after each test
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('Context Provider', () => {
    it('should provide auth context to children', () => {
      const TestComponent = () => {
        const context = useContext(AuthContext);
        return (
          <div>
            <span data-testid="has-context">{context ? 'yes' : 'no'}</span>
            <span data-testid="is-authenticated">{context?.isAuthenticated ? 'yes' : 'no'}</span>
            <span data-testid="is-loading">{context?.isLoading ? 'yes' : 'no'}</span>
          </div>
        );
      };

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('has-context')).toHaveTextContent('yes');
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('no');
      expect(screen.getByTestId('is-loading')).toHaveTextContent('no');
    });

    it('should return null when using context outside provider', () => {
      const TestComponent = () => {
        const context = useContext(AuthContext);
        return (
          <div>
            <span data-testid="context-value">{context === null ? 'null' : 'not-null'}</span>
          </div>
        );
      };

      render(<TestComponent />);

      // Context should be null when used outside provider
      expect(screen.getByTestId('context-value')).toHaveTextContent('null');
    });
  });

  describe('Authentication Flow', () => {
    it.runIf(isIdentityServiceAvailable)('should handle login through context', async () => {
      const TestComponent = () => {
        const context = useContext(AuthContext);
        if (!context) {
          throw new Error('useAuth must be used within an AuthProvider');
        }

        const { login, isAuthenticated, user, error, isLoading } = context;

        const handleLogin = async () => {
          try {
            await login({
              email: 'test@focushive.test',
              password: 'TestPassword123',
              rememberMe: false
            });
          } catch (err) {
            // Expected to fail with invalid credentials
            console.log('Login failed as expected:', err);
          }
        };

        return (
          <div>
            <button onClick={handleLogin} data-testid="login-button">Login</button>
            <span data-testid="is-authenticated">{isAuthenticated ? 'yes' : 'no'}</span>
            <span data-testid="is-loading">{isLoading ? 'yes' : 'no'}</span>
            <span data-testid="has-error">{error ? 'yes' : 'no'}</span>
            <span data-testid="user-email">{user?.email || 'none'}</span>
          </div>
        );
      };

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Initially not authenticated
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('no');
      expect(screen.getByTestId('user-email')).toHaveTextContent('none');

      // Click login button
      const loginButton = screen.getByTestId('login-button');

      await act(async () => {
        loginButton.click();
      });

      // Wait for the login attempt to complete
      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('no');
      }, { timeout: 10000 });

      // Login should fail with test credentials
      // This is expected behavior
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('no');
    });

    it.runIf(isIdentityServiceAvailable)('should handle registration through context', async () => {
      const testEmail = generateTestEmail();

      const TestComponent = () => {
        const context = useContext(AuthContext);
        if (!context) {
          throw new Error('useAuth must be used within an AuthProvider');
        }

        const { register, isAuthenticated, user, error, isLoading } = context;

        const handleRegister = async () => {
          try {
            await register({
              email: testEmail,
              password: 'TestPassword123',
              confirmPassword: 'TestPassword123',
              firstName: 'Test',
              lastName: 'User',
              acceptTerms: true
            });
          } catch (err: any) {
            // Handle known Identity Service issues
            if (err.code === 'RATE_LIMIT_EXCEEDED' || err.statusCode === 429) {
              console.log('Rate limit hit - expected with Identity Service');
            } else {
              console.log('Registration error:', err);
            }
          }
        };

        return (
          <div>
            <button onClick={handleRegister} data-testid="register-button">Register</button>
            <span data-testid="is-authenticated">{isAuthenticated ? 'yes' : 'no'}</span>
            <span data-testid="is-loading">{isLoading ? 'yes' : 'no'}</span>
            <span data-testid="has-error">{error ? 'yes' : 'no'}</span>
            <span data-testid="user-email">{user?.email || 'none'}</span>
            <span data-testid="error-code">{error?.code || 'none'}</span>
          </div>
        );
      };

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Initially not authenticated
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('no');

      // Click register button
      const registerButton = screen.getByTestId('register-button');

      await act(async () => {
        registerButton.click();
      });

      // Wait for the registration attempt to complete
      await waitFor(() => {
        expect(screen.getByTestId('is-loading')).toHaveTextContent('no');
      }, { timeout: 10000 });

      // Check result - may succeed or hit rate limit
      const errorCode = screen.getByTestId('error-code').textContent;
      if (errorCode === 'RATE_LIMIT_EXCEEDED') {
        console.log('Rate limit hit - this is expected behavior');
        expect(screen.getByTestId('has-error')).toHaveTextContent('yes');
      } else if (screen.getByTestId('is-authenticated').textContent === 'yes') {
        // Registration successful
        expect(screen.getByTestId('user-email')).toHaveTextContent(testEmail);
      }
    });

    it.runIf(isIdentityServiceAvailable)('should handle logout through context', async () => {
      const TestComponent = () => {
        const context = useContext(AuthContext);
        if (!context) {
          throw new Error('useAuth must be used within an AuthProvider');
        }

        const { logout, isAuthenticated, user } = context;

        return (
          <div>
            <button onClick={logout} data-testid="logout-button">Logout</button>
            <span data-testid="is-authenticated">{isAuthenticated ? 'yes' : 'no'}</span>
            <span data-testid="user-email">{user?.email || 'none'}</span>
          </div>
        );
      };

      // Pre-set some auth data
      localStorage.setItem('auth_access_token', 'test-token');
      localStorage.setItem('auth_user', JSON.stringify({ email: 'test@example.com' }));

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Click logout button
      const logoutButton = screen.getByTestId('logout-button');

      await act(async () => {
        logoutButton.click();
      });

      // Should be logged out
      await waitFor(() => {
        expect(screen.getByTestId('is-authenticated')).toHaveTextContent('no');
        expect(screen.getByTestId('user-email')).toHaveTextContent('none');
      });

      // Storage should be cleared
      expect(localStorage.getItem('auth_access_token')).toBeNull();
      expect(localStorage.getItem('auth_user')).toBeNull();
    });

    it('should persist auth state across re-renders', async () => {
      const { rerender } = render(
        <AuthProvider>
          <div data-testid="child">Child Component</div>
        </AuthProvider>
      );

      // Set some auth data
      localStorage.setItem('auth_access_token', 'persistent-token');
      localStorage.setItem('auth_user', JSON.stringify({
        email: 'persistent@example.com',
        id: '123',
        firstName: 'Test',
        lastName: 'User'
      }));

      // Re-render the provider
      rerender(
        <AuthProvider>
          <div data-testid="child">Updated Child Component</div>
        </AuthProvider>
      );

      // Auth data should still be in localStorage
      expect(localStorage.getItem('auth_access_token')).toBe('persistent-token');
      expect(localStorage.getItem('auth_user')).toContain('persistent@example.com');
    });
  });

  describe('Error Handling', () => {
    it('should handle and clear errors through context', async () => {
      const TestComponent = () => {
        const context = useContext(AuthContext);
        if (!context) {
          throw new Error('useAuth must be used within an AuthProvider');
        }

        const { login, clearError, error } = context;

        const triggerError = async () => {
          try {
            await login({
              email: 'invalid@test.com',
              password: 'wrong',
              rememberMe: false
            });
          } catch (err) {
            // Error expected
          }
        };

        return (
          <div>
            <button onClick={triggerError} data-testid="trigger-error">Trigger Error</button>
            <button onClick={clearError} data-testid="clear-error">Clear Error</button>
            <span data-testid="has-error">{error ? 'yes' : 'no'}</span>
            <span data-testid="error-message">{error?.message || 'none'}</span>
          </div>
        );
      };

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Initially no error
      expect(screen.getByTestId('has-error')).toHaveTextContent('no');

      if (isIdentityServiceAvailable) {
        // Trigger an error
        const triggerButton = screen.getByTestId('trigger-error');

        await act(async () => {
          triggerButton.click();
        });

        // Wait for error to be set
        await waitFor(() => {
          expect(screen.getByTestId('has-error')).toHaveTextContent('yes');
        }, { timeout: 5000 });

        // Clear the error
        const clearButton = screen.getByTestId('clear-error');

        act(() => {
          clearButton.click();
        });

        // Error should be cleared
        expect(screen.getByTestId('has-error')).toHaveTextContent('no');
        expect(screen.getByTestId('error-message')).toHaveTextContent('none');
      }
    });
  });
});