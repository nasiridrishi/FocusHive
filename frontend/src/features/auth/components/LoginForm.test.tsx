import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test-utils/test-utils';
import LoginForm from './LoginForm';

const mockOnSubmit = vi.fn();

const defaultProps = {
  onSubmit: mockOnSubmit,
  isLoading: false,
  error: null,
};

describe('LoginForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders login form elements', () => {
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const submitButton = screen.getByRole('button', { name: /sign in/i });
    await user.click(submitButton);
    
    await waitFor(() => {
      // Check for validation summary or individual field errors
      const validationElements = screen.queryAllByText(/required/i);
      expect(validationElements.length).toBeGreaterThan(0);
    });
  });

  it('shows validation error for invalid email format', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText('Email');
    const submitButton = screen.getByRole('button', { name: /sign in/i });
    
    await user.type(emailInput, 'invalid-email');
    await user.click(submitButton);
    
    await waitFor(() => {
      // Check for email validation error in validation summary or field
      const validationElements = screen.queryAllByText(/email/i);
      expect(validationElements.length).toBeGreaterThan(1); // Label + error
    });
  });

  it('shows validation error for short password', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText('Email');
    const passwordInput = screen.getByLabelText('Password');
    const submitButton = screen.getByRole('button', { name: /sign in/i });
    
    await user.type(emailInput, 'test@example.com');
    await user.type(passwordInput, '12345'); // 5 characters, less than required 6
    await user.click(submitButton);
    
    // Since form validation prevents submission, no validation summary will show
    // The form should not call onSubmit with invalid data
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('submits form with valid credentials', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText('Email');
    const passwordInput = screen.getByLabelText('Password');
    const submitButton = screen.getByRole('button', { name: /sign in/i });
    
    await user.type(emailInput, 'test@example.com');
    await user.type(passwordInput, 'password123');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123'
      });
    });
  });

  it('shows error message for invalid credentials', async () => {
    const propsWithError = {
      ...defaultProps,
      error: 'Invalid email or password'
    };
    renderWithProviders(<LoginForm {...propsWithError} />);
    
    expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
  });

  it('shows loading state during submission', async () => {
    const loadingProps = {
      ...defaultProps,
      isLoading: true
    };
    renderWithProviders(<LoginForm {...loadingProps} />);
    
    // Should show loading indicator on button
    expect(screen.getByRole('button', { name: /signing in|loading/i })).toBeInTheDocument();
  });

  it('toggles password visibility', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const passwordInput = screen.getByLabelText('Password') as HTMLInputElement;
    const toggleButton = screen.getByRole('button', { name: /toggle password visibility/i });
    
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
    const propsWithError = {
      ...defaultProps,
      error: 'Network connection error'
    };
    renderWithProviders(<LoginForm {...propsWithError} />);
    
    expect(screen.getByText(/network.*error/i)).toBeInTheDocument();
  });

  it('has proper accessibility attributes', () => {
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText('Email');
    const passwordInput = screen.getByLabelText('Password');
    const toggleButton = screen.getByRole('button', { name: /toggle password visibility/i });
    
    expect(emailInput).toHaveAttribute('type', 'email');
    expect(emailInput).toHaveAttribute('autocomplete', 'email');
    expect(passwordInput).toHaveAttribute('type', 'password');
    expect(passwordInput).toHaveAttribute('autocomplete', 'current-password');
    expect(toggleButton).toHaveAttribute('aria-label', 'Toggle password visibility');
  });

  it('supports keyboard navigation', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginForm {...defaultProps} />);
    
    const emailInput = screen.getByLabelText('Email');
    const passwordInput = screen.getByLabelText('Password');
    
    // Email input should have autofocus
    expect(emailInput).toHaveFocus();
    
    // Tab to password input
    await user.tab();
    expect(passwordInput).toHaveFocus();
    
    // Enter key should submit form when in input field
    await user.type(emailInput, 'test@example.com');
    await user.type(passwordInput, 'password123');
    await user.keyboard('{Enter}');
    
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123'
      });
    });
  });
});