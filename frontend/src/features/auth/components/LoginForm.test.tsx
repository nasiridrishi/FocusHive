import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test-utils/test-utils';
import { server } from '@/test-utils/msw-server';
import { http, HttpResponse } from 'msw';
import LoginForm from './LoginForm';

const mockOnSuccess = vi.fn();
const mockOnError = vi.fn();
const mockOnSubmit = vi.fn();

const defaultProps = {
  onSuccess: mockOnSuccess,
  onError: mockOnError,
  onSubmit: mockOnSubmit,
};

describe('LoginForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders login form elements', () => {
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    expect(screen.getByLabelText(/username|email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login|sign in/i })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/username.*required|email.*required/i)).toBeInTheDocument();
      expect(screen.getByText(/password.*required/i)).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid email format', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText(/username|email/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    await user.type(emailInput, 'invalid-email');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/valid email|email format/i)).toBeInTheDocument();
    });
  });

  it('shows validation error for short password', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    await user.type(emailInput, 'test@example.com');
    await user.type(passwordInput, '123');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/password.*at least.*characters/i)).toBeInTheDocument();
    });
  });

  it('submits form with valid credentials', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const usernameInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    await user.type(usernameInput, 'testuser');
    await user.type(passwordInput, 'password');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(mockOnSuccess).toHaveBeenCalledWith(
        expect.objectContaining({
          user: expect.objectContaining({
            username: 'testuser'
          }),
          accessToken: expect.any(String),
          refreshToken: expect.any(String)
        })
      );
    });
  });

  it('shows error message for invalid credentials', async () => {
    // Override the MSW handler to return an error
    server.use(
      http.post('/api/auth/login', () => {
        return HttpResponse.json(
          { message: 'Invalid credentials' },
          { status: 401 }
        );
      })
    );

    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const usernameInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    await user.type(usernameInput, 'wronguser');
    await user.type(passwordInput, 'wrongpassword');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
      expect(mockOnError).toHaveBeenCalledWith(expect.any(Error));
    });
  });

  it('shows loading state during submission', async () => {
    // Override the MSW handler to add delay
    server.use(
      http.post('/api/auth/login', async () => {
        await new Promise(resolve => setTimeout(resolve, 100));
        return HttpResponse.json({
          user: { username: 'testuser' },
          accessToken: 'token',
          refreshToken: 'refresh'
        });
      })
    );

    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const usernameInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    await user.type(usernameInput, 'testuser');
    await user.type(passwordInput, 'password');
    await user.click(submitButton);
    
    // Should show loading indicator
    expect(screen.getByRole('button', { name: /logging in|loading/i })).toBeInTheDocument();
    
    await waitFor(() => {
      expect(mockOnSuccess).toHaveBeenCalled();
    });
  });

  it('toggles password visibility', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const passwordInput = screen.getByLabelText(/password/i) as HTMLInputElement;
    const toggleButton = screen.getByRole('button', { name: /show password|toggle password/i });
    
    // Initially password should be hidden
    expect(passwordInput.type).toBe('password');
    
    // Click toggle button
    await user.click(toggleButton);
    expect(passwordInput.type).toBe('text');
    
    // Click again to hide
    await user.click(toggleButton);
    expect(passwordInput.type).toBe('password');
  });

  it('handles network errors gracefully', async () => {
    // Override the MSW handler to simulate network error
    server.use(
      http.post('/api/auth/login', () => {
        return HttpResponse.error();
      })
    );

    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const usernameInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    await user.type(usernameInput, 'testuser');
    await user.type(passwordInput, 'password');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/network error|connection error|try again/i)).toBeInTheDocument();
      expect(mockOnError).toHaveBeenCalled();
    });
  });

  it('has proper accessibility attributes', () => {
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const form = screen.getByRole('form') || screen.getByLabelText(/login form/i);
    expect(form).toBeInTheDocument();
    
    const usernameInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    
    expect(usernameInput).toHaveAttribute('required');
    expect(passwordInput).toHaveAttribute('required');
    expect(passwordInput).toHaveAttribute('type', 'password');
  });

  it('supports keyboard navigation', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const usernameInput = screen.getByLabelText(/username|email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login|sign in/i });
    
    // Tab navigation should work
    await user.tab();
    expect(usernameInput).toHaveFocus();
    
    await user.tab();
    expect(passwordInput).toHaveFocus();
    
    await user.tab();
    expect(submitButton).toHaveFocus();
    
    // Enter key should submit form
    await user.type(usernameInput, 'testuser');
    await user.type(passwordInput, 'password');
    await user.keyboard('{Enter}');
    
    await waitFor(() => {
      expect(mockOnSuccess).toHaveBeenCalled();
    });
  });
});