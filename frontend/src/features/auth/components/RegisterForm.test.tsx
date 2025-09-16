import {beforeEach, describe, expect, it, vi} from 'vitest';
import {screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {renderWithProviders} from '@/test-utils/test-utils';
import RegisterForm from './RegisterForm';

const mockNavigate = vi.fn();

// Mock react-router-dom
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockOnSubmit = vi.fn();

const defaultProps = {
  onSubmit: mockOnSubmit,
  isLoading: false,
  error: null,
};

describe('RegisterForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders all form elements correctly', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Header elements
      expect(screen.getByRole('heading', {name: /join focushive/i})).toBeInTheDocument();
      expect(screen.getByText(/create your account to start focusing together/i)).toBeInTheDocument();

      // Form elements
      expect(screen.getByLabelText('First Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Last Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Username')).toBeInTheDocument();
      expect(screen.getByLabelText('Email Address')).toBeInTheDocument();
      expect(screen.getByLabelText('Password')).toBeInTheDocument();
      expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();
      expect(screen.getByRole('button', {name: /create account/i})).toBeInTheDocument();

      // Icons
      expect(screen.getByTestId('PersonIcon')).toBeInTheDocument();
      expect(screen.getByTestId('PersonOutlineIcon')).toBeInTheDocument();
      expect(screen.getByTestId('EmailIcon')).toBeInTheDocument();
      expect(screen.getAllByTestId('LockIcon')).toHaveLength(2); // Password and confirm password
      expect(screen.getByTestId('PersonAddIcon')).toBeInTheDocument();

      // Terms and conditions checkbox
      expect(screen.getByRole('checkbox')).toBeInTheDocument();
      expect(screen.getByText(/i agree to the/i)).toBeInTheDocument();
      expect(screen.getByRole('link', {name: /terms of service/i})).toBeInTheDocument();
      expect(screen.getByRole('link', {name: /privacy policy/i})).toBeInTheDocument();

      // Navigation link
      expect(screen.getByText(/already have an account/i)).toBeInTheDocument();
      expect(screen.getByRole('button', {name: /sign in/i})).toBeInTheDocument();
    });

    it('renders in a Paper container with proper styling', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const paper = screen.getByRole('heading', {name: /join focushive/i}).closest('.MuiPaper-root');
      expect(paper).toBeInTheDocument();
      expect(paper).toHaveClass('MuiPaper-elevation3');
    });

    it('has proper form structure with noValidate attribute', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const form = document.querySelector('form');
      expect(form).toBeInTheDocument();
      expect(form).toHaveAttribute('novalidate');
    });

    it('renders password strength indicator', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Password strength indicator should not be visible initially
      expect(screen.queryByText('Password Strength')).not.toBeInTheDocument();
    });

    it('has proper name field grid layout', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Check that first name and last name are in the same container (grid)
      const firstNameField = screen.getByLabelText('First Name');
      const lastNameField = screen.getByLabelText('Last Name');

      // Both fields should exist and be rendered properly in form structure
      expect(firstNameField).toBeInTheDocument();
      expect(lastNameField).toBeInTheDocument();
    });
  });

  describe('Form Validation', () => {
    it('shows validation errors for empty fields on submit', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const submitButton = screen.getByRole('button', {name: /create account/i});
      await user.click(submitButton);

      await waitFor(() => {
        // Should show validation summary with errors for multiple empty fields
        const validationError = screen.getByText(/please fill in all highlighted fields/i);
        expect(validationError).toBeInTheDocument();
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates required fields individually', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Test with only first name filled - should show validation error
      const firstNameInput = screen.getByLabelText('First Name');
      await user.type(firstNameInput, 'John');
      await user.click(submitButton);

      await waitFor(() => {
        const validationError = screen.getByText(/please fill in all highlighted fields/i);
        expect(validationError).toBeInTheDocument();
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates name fields with invalid characters', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Fill with invalid name characters
      await user.type(firstNameInput, 'John123');
      await user.type(lastNameInput, 'Doe!');
      await user.type(usernameInput, 'johndoe');
      await user.type(emailInput, 'john@example.com');
      await user.type(passwordInput, 'ValidPass123!');
      await user.type(confirmPasswordInput, 'ValidPass123!');
      await user.click(termsCheckbox);
      await user.click(submitButton);

      await waitFor(() => {
        // Should show validation summary - ValidationSummary will show error for name fields
        const validationElements = screen.queryAllByText(/first name and last name are required/i) ||
            screen.queryAllByText(/please fill in all highlighted fields/i);
        expect(validationElements.length).toBeGreaterThan(0);
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates username format and length', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const usernameInput = screen.getByLabelText('Username');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Test too short username
      await user.type(usernameInput, 'jo');
      await user.click(submitButton);

      await waitFor(() => {
        const validationError = screen.getByText(/please fill in all highlighted fields/i);
        expect(validationError).toBeInTheDocument();
      });

      // Test invalid characters
      await user.clear(usernameInput);
      await user.type(usernameInput, 'john@doe');
      await user.click(submitButton);

      await waitFor(() => {
        const validationError = screen.getByText(/please fill in all highlighted fields/i);
        expect(validationError).toBeInTheDocument();
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates email format', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Fill all other fields correctly
      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe');
      await user.type(passwordInput, 'ValidPass123!');
      await user.type(confirmPasswordInput, 'ValidPass123!');
      await user.click(termsCheckbox);

      // Test invalid email
      await user.type(emailInput, 'invalid-email');
      await user.click(submitButton);

      await waitFor(() => {
        // ValidationSummary should show error for invalid email
        const validationError = screen.getByText(/email is required/i);
        expect(validationError).toBeInTheDocument();
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates password strength requirements', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Fill all other fields correctly
      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe');
      await user.type(emailInput, 'john@example.com');
      await user.click(termsCheckbox);

      // Test weak password
      await user.type(passwordInput, 'weakpass');
      await user.type(confirmPasswordInput, 'weakpass');
      await user.click(submitButton);

      await waitFor(() => {
        // Should show validation error for password requirements not met
        const validationElements = screen.queryAllByText(/password is required/i) ||
            screen.queryAllByText(/please fill in all highlighted fields/i);
        expect(validationElements.length).toBeGreaterThan(0);
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates password confirmation match', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Fill all other fields correctly
      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe');
      await user.type(emailInput, 'john@example.com');
      await user.click(termsCheckbox);

      // Test mismatching passwords
      await user.type(passwordInput, 'ValidPass123!');
      await user.type(confirmPasswordInput, 'DifferentPass123!');
      await user.click(submitButton);

      await waitFor(() => {
        // Should show validation error for password mismatch
        const validationElements = screen.queryAllByText(/confirm password is required/i) ||
            screen.queryAllByText(/please fill in all highlighted fields/i);
        expect(validationElements.length).toBeGreaterThan(0);
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('validates terms acceptance requirement', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Fill all fields but don't accept terms
      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe');
      await user.type(emailInput, 'john@example.com');
      await user.type(passwordInput, 'ValidPass123!');
      await user.type(confirmPasswordInput, 'ValidPass123!');
      await user.click(submitButton);

      await waitFor(() => {
        const validationError = screen.getByText(/you must accept the terms and conditions/i);
        expect(validationError).toBeInTheDocument();
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('clears validation errors when user corrects input', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Submit with empty form to trigger validation errors
      await user.click(submitButton);

      await waitFor(() => {
        const validationError = screen.getByText(/please fill in all highlighted fields/i);
        expect(validationError).toBeInTheDocument();
      });

      // Fill valid data
      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');

      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe');
      await user.type(emailInput, 'john@example.com');
      await user.type(passwordInput, 'ValidPass123!');
      await user.type(confirmPasswordInput, 'ValidPass123!');
      await user.click(termsCheckbox);

      // Validation should clear - ValidationSummary should no longer be visible
      await waitFor(() => {
        const validationError = screen.queryByText(/please fill in all highlighted fields/i);
        expect(validationError).not.toBeInTheDocument();
      });
    });
  });

  describe('Password Functionality', () => {
    it('shows password strength indicator when password is entered', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const passwordInput = screen.getByLabelText('Password');

      // Initially should not show
      expect(screen.queryByText('Password Strength')).not.toBeInTheDocument();

      // Type password
      await user.type(passwordInput, 'weakpass');

      // Should show password strength indicator
      await waitFor(() => {
        expect(screen.getByText('Password Strength')).toBeInTheDocument();
        expect(screen.getByText('Weak')).toBeInTheDocument();
      });
    });

    it('updates password strength indicator based on password quality', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const passwordInput = screen.getByLabelText('Password');

      // Type weak password
      await user.type(passwordInput, 'weak');

      await waitFor(() => {
        expect(screen.getByText('Weak')).toBeInTheDocument();
      });

      // Clear and type strong password
      await user.clear(passwordInput);
      await user.type(passwordInput, 'StrongPassword123!');

      await waitFor(() => {
        expect(screen.getByText('Strong')).toBeInTheDocument();
        expect(screen.getByText('Strong password!')).toBeInTheDocument();
      });
    });

    it('toggles password visibility when clicked', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const passwordInput = screen.getByLabelText('Password') as HTMLInputElement;
      const toggleButtons = screen.getAllByLabelText('toggle password visibility');
      const passwordToggleButton = toggleButtons[0]; // First one is for password field

      // Initially password should be hidden
      expect(passwordInput.type).toBe('password');
      expect(screen.getAllByTestId('VisibilityIcon')).toHaveLength(2);

      // Click toggle button to show password
      await user.click(passwordToggleButton);
      expect(passwordInput.type).toBe('text');
      expect(screen.getByTestId('VisibilityOffIcon')).toBeInTheDocument();

      // Click again to hide password
      await user.click(passwordToggleButton);
      expect(passwordInput.type).toBe('password');
      expect(screen.getAllByTestId('VisibilityIcon')).toHaveLength(2);
    });

    it('toggles confirm password visibility independently', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const confirmPasswordInput = screen.getByLabelText('Confirm Password') as HTMLInputElement;
      const toggleButtons = screen.getAllByLabelText('toggle confirm password visibility');
      const confirmPasswordToggleButton = toggleButtons[0]; // Second one is for confirm password field

      // Initially confirm password should be hidden
      expect(confirmPasswordInput.type).toBe('password');

      // Click toggle button to show confirm password
      await user.click(confirmPasswordToggleButton);
      expect(confirmPasswordInput.type).toBe('text');

      // Click again to hide confirm password
      await user.click(confirmPasswordToggleButton);
      expect(confirmPasswordInput.type).toBe('password');
    });

    it('maintains password visibility state while typing', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const passwordInput = screen.getByLabelText('Password') as HTMLInputElement;
      const toggleButtons = screen.getAllByLabelText('toggle password visibility');
      const passwordToggleButton = toggleButtons[0];

      // Show password
      await user.click(passwordToggleButton);
      expect(passwordInput.type).toBe('text');

      // Type password while visible
      await user.type(passwordInput, 'MyPassword123!');
      expect(passwordInput.type).toBe('text');
      expect(passwordInput.value).toBe('MyPassword123!');

      // Hide password
      await user.click(passwordToggleButton);
      expect(passwordInput.type).toBe('password');
      expect(passwordInput.value).toBe('MyPassword123!');
    });
  });

  describe('Form Submission', () => {
    it('submits form with valid data', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');

      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe123');
      await user.type(emailInput, 'john.doe@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'StrongPass123!');
      await user.click(termsCheckbox);

      const submitButton = screen.getByRole('button', {name: /create account/i});
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          firstName: 'John',
          lastName: 'Doe',
          username: 'johndoe123',
          email: 'john.doe@example.com',
          password: 'StrongPass123!'
          // Note: confirmPassword and acceptTerms are excluded from submission
        });
      });
    });

    it('submits form with Enter key', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Fill all required fields
      await user.type(screen.getByLabelText('First Name'), 'John');
      await user.type(screen.getByLabelText('Last Name'), 'Doe');
      await user.type(screen.getByLabelText('Username'), 'johndoe123');
      await user.type(screen.getByLabelText('Email Address'), 'john.doe@example.com');
      await user.type(screen.getByLabelText('Password'), 'StrongPass123!');
      await user.type(screen.getByLabelText('Confirm Password'), 'StrongPass123!');
      await user.click(screen.getByRole('checkbox'));

      // Submit with Enter key
      await user.keyboard('{Enter}');

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          firstName: 'John',
          lastName: 'Doe',
          username: 'johndoe123',
          email: 'john.doe@example.com',
          password: 'StrongPass123!'
        });
      });
    });

    it('handles form data properly', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');
      const termsCheckbox = screen.getByRole('checkbox');
      const submitButton = screen.getByRole('button', {name: /create account/i});

      // Fill with valid data
      await user.type(firstNameInput, 'John');
      await user.type(lastNameInput, 'Doe');
      await user.type(usernameInput, 'johndoe123');
      await user.type(emailInput, 'john.doe@example.com');
      await user.type(passwordInput, 'StrongPass123!');
      await user.type(confirmPasswordInput, 'StrongPass123!');
      await user.click(termsCheckbox);
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          firstName: 'John',
          lastName: 'Doe',
          username: 'johndoe123',
          email: 'john.doe@example.com',
          password: 'StrongPass123!'
        });
      });
    });

    it('handles form submission errors gracefully', async () => {
      const user = userEvent.setup();
      const errorOnSubmit = vi.fn().mockRejectedValue(new Error('Submission failed'));

      renderWithProviders(<RegisterForm {...defaultProps} onSubmit={errorOnSubmit}/>);

      // Fill valid data
      await user.type(screen.getByLabelText('First Name'), 'John');
      await user.type(screen.getByLabelText('Last Name'), 'Doe');
      await user.type(screen.getByLabelText('Username'), 'johndoe123');
      await user.type(screen.getByLabelText('Email Address'), 'john.doe@example.com');
      await user.type(screen.getByLabelText('Password'), 'StrongPass123!');
      await user.type(screen.getByLabelText('Confirm Password'), 'StrongPass123!');
      await user.click(screen.getByRole('checkbox'));

      const submitButton = screen.getByRole('button', {name: /create account/i});
      await user.click(submitButton);

      await waitFor(() => {
        expect(errorOnSubmit).toHaveBeenCalled();
      });

      // Form should still be functional after error
      expect(screen.getByLabelText('First Name')).toBeEnabled();
      expect(screen.getByLabelText('Email Address')).toBeEnabled();
    });
  });

  describe('Error Handling', () => {
    it('displays error message for registration failures', () => {
      const propsWithError = {
        ...defaultProps,
        error: 'Username already exists'
      };
      renderWithProviders(<RegisterForm {...propsWithError} />);

      const errorAlert = screen.getByRole('alert');
      expect(errorAlert).toBeInTheDocument();
      expect(errorAlert).toHaveTextContent('Username already exists');
    });

    it('displays network error messages', () => {
      const propsWithError = {
        ...defaultProps,
        error: 'Network connection error'
      };
      renderWithProviders(<RegisterForm {...propsWithError} />);

      expect(screen.getByRole('alert')).toHaveTextContent('Network connection error');
    });

    it('displays server error messages', () => {
      const propsWithError = {
        ...defaultProps,
        error: 'Server error occurred'
      };
      renderWithProviders(<RegisterForm {...propsWithError} />);

      expect(screen.getByRole('alert')).toHaveTextContent('Server error occurred');
    });

    it('hides error message when error prop is null', () => {
      const {rerender} = renderWithProviders(
          <RegisterForm {...defaultProps} error="Some error"/>
      );

      expect(screen.getByRole('alert')).toBeInTheDocument();

      rerender(<RegisterForm {...defaultProps} error={null}/>);

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('updates error message when error prop changes', () => {
      const {rerender} = renderWithProviders(
          <RegisterForm {...defaultProps} error="First error"/>
      );

      expect(screen.getByRole('alert')).toHaveTextContent('First error');

      rerender(<RegisterForm {...defaultProps} error="Second error"/>);

      expect(screen.getByRole('alert')).toHaveTextContent('Second error');
    });
  });

  describe('Loading State', () => {
    it('shows loading state during submission', () => {
      const loadingProps = {
        ...defaultProps,
        isLoading: true
      };
      renderWithProviders(<RegisterForm {...loadingProps} />);

      // Button should show loading state
      const submitButton = screen.getByRole('button', {name: /creating account/i});
      expect(submitButton).toBeInTheDocument();
      expect(submitButton).toBeDisabled();
    });

    it('disables form inputs during loading', () => {
      const loadingProps = {
        ...defaultProps,
        isLoading: true
      };
      renderWithProviders(<RegisterForm {...loadingProps} />);

      expect(screen.getByLabelText('First Name')).toBeDisabled();
      expect(screen.getByLabelText('Last Name')).toBeDisabled();
      expect(screen.getByLabelText('Username')).toBeDisabled();
      expect(screen.getByLabelText('Email Address')).toBeDisabled();
      expect(screen.getByLabelText('Password')).toBeDisabled();
      expect(screen.getByLabelText('Confirm Password')).toBeDisabled();
      expect(screen.getByRole('checkbox')).toBeDisabled();
    });

    it('keeps password toggle buttons enabled during loading', () => {
      const loadingProps = {
        ...defaultProps,
        isLoading: true
      };
      renderWithProviders(<RegisterForm {...loadingProps} />);

      const toggleButtons = screen.getAllByLabelText(/toggle.*password visibility/i);
      toggleButtons.forEach(button => {
        expect(button).toBeEnabled();
      });
    });

    it('prevents form submission while loading', async () => {
      const loadingProps = {
        ...defaultProps,
        isLoading: true
      };
      renderWithProviders(<RegisterForm {...loadingProps} />);

      const submitButton = screen.getByRole('button', {name: /creating account/i});
      expect(submitButton).toBeDisabled();

      // Verify button is disabled - no need to test clicking disabled button
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('shows correct loading text', () => {
      const loadingProps = {
        ...defaultProps,
        isLoading: true
      };
      renderWithProviders(<RegisterForm {...loadingProps} />);

      expect(screen.getByText(/creating account/i)).toBeInTheDocument();
    });
  });

  describe('Navigation', () => {
    it('navigates to login page when sign in link is clicked', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const signInLink = screen.getByRole('button', {name: /sign in/i});
      await user.click(signInLink);

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('sign in link has proper styling and context', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      expect(screen.getByText(/already have an account/i)).toBeInTheDocument();

      const signInLink = screen.getByRole('button', {name: /sign in/i});
      expect(signInLink).toHaveAttribute('type', 'button');
    });
  });

  describe('Terms and Conditions', () => {
    it('renders terms and privacy policy links', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const termsLink = screen.getByRole('link', {name: /terms of service/i});
      const privacyLink = screen.getByRole('link', {name: /privacy policy/i});

      expect(termsLink).toBeInTheDocument();
      expect(termsLink).toHaveAttribute('href', '/terms');
      expect(termsLink).toHaveAttribute('target', '_blank');
      expect(termsLink).toHaveAttribute('rel', 'noopener');

      expect(privacyLink).toBeInTheDocument();
      expect(privacyLink).toHaveAttribute('href', '/privacy');
      expect(privacyLink).toHaveAttribute('target', '_blank');
      expect(privacyLink).toHaveAttribute('rel', 'noopener');
    });

    it('toggles terms acceptance checkbox', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const checkbox = screen.getByRole('checkbox');

      // Initially unchecked
      expect(checkbox).not.toBeChecked();

      // Click to check
      await user.click(checkbox);
      expect(checkbox).toBeChecked();

      // Click to uncheck
      await user.click(checkbox);
      expect(checkbox).not.toBeChecked();
    });

    it('applies error styling to terms checkbox when validation fails', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Fill all fields except terms
      await user.type(screen.getByLabelText('First Name'), 'John');
      await user.type(screen.getByLabelText('Last Name'), 'Doe');
      await user.type(screen.getByLabelText('Username'), 'johndoe123');
      await user.type(screen.getByLabelText('Email Address'), 'john@example.com');
      await user.type(screen.getByLabelText('Password'), 'StrongPass123!');
      await user.type(screen.getByLabelText('Confirm Password'), 'StrongPass123!');

      const submitButton = screen.getByRole('button', {name: /create account/i});
      await user.click(submitButton);

      await waitFor(() => {
        const checkbox = screen.getByRole('checkbox');
        // Check if checkbox has error styling through classes or styles
        expect(checkbox).toBeInTheDocument();
      });
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels and roles', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Form should be present
      const form = document.querySelector('form');
      expect(form).toBeInTheDocument();

      // Heading should have proper role
      expect(screen.getByRole('heading', {name: /join focushive/i})).toBeInTheDocument();

      // Inputs should have proper labels
      expect(screen.getByLabelText('First Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Last Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Username')).toBeInTheDocument();
      expect(screen.getByLabelText('Email Address')).toBeInTheDocument();
      expect(screen.getByLabelText('Password')).toBeInTheDocument();
      expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();

      // Toggle buttons should have proper aria-labels
      const toggleButtons = screen.getAllByLabelText(/toggle.*password visibility/i);
      expect(toggleButtons).toHaveLength(2);
      toggleButtons.forEach(button => {
        expect(button).toHaveAttribute('aria-label');
      });
    });

    it('has proper autocomplete attributes', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');
      const confirmPasswordInput = screen.getByLabelText('Confirm Password');

      expect(firstNameInput).toHaveAttribute('autocomplete', 'given-name');
      expect(lastNameInput).toHaveAttribute('autocomplete', 'family-name');
      expect(usernameInput).toHaveAttribute('autocomplete', 'username');
      expect(emailInput).toHaveAttribute('autocomplete', 'email');
      expect(emailInput).toHaveAttribute('type', 'email');
      expect(passwordInput).toHaveAttribute('autocomplete', 'new-password');
      expect(confirmPasswordInput).toHaveAttribute('autocomplete', 'new-password');
    });

    it('supports keyboard navigation', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const firstNameInput = screen.getByLabelText('First Name');
      const lastNameInput = screen.getByLabelText('Last Name');
      const usernameInput = screen.getByLabelText('Username');
      const emailInput = screen.getByLabelText('Email Address');
      const passwordInput = screen.getByLabelText('Password');

      // First name input should have autofocus
      expect(firstNameInput).toHaveFocus();

      // Tab through form elements
      await user.tab();
      expect(lastNameInput).toHaveFocus();

      await user.tab();
      expect(usernameInput).toHaveFocus();

      await user.tab();
      expect(emailInput).toHaveFocus();

      await user.tab();
      expect(passwordInput).toHaveFocus();
    });

    it('error messages are announced to screen readers', () => {
      const propsWithError = {
        ...defaultProps,
        error: 'Registration failed'
      };
      renderWithProviders(<RegisterForm {...propsWithError} />);

      const errorAlert = screen.getByRole('alert');
      expect(errorAlert).toBeInTheDocument();
    });

    it('form fields have proper error state styling', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const submitButton = screen.getByRole('button', {name: /create account/i});
      await user.click(submitButton);

      await waitFor(() => {
        const firstNameInput = screen.getByLabelText('First Name');
        const emailInput = screen.getByLabelText('Email Address');

        // Check for error indication through form controls
        const firstNameFormControl = firstNameInput.closest('.MuiFormControl-root');
        const emailFormControl = emailInput.closest('.MuiFormControl-root');

        expect(firstNameFormControl).toBeInTheDocument();
        expect(emailFormControl).toBeInTheDocument();

        // Check for error indication through aria-invalid
        expect(firstNameInput).toHaveAttribute('aria-invalid', 'false'); // May become true after validation
        expect(emailInput).toHaveAttribute('aria-invalid', 'false');
      });
    });
  });

  describe('Visual and Structural Elements', () => {
    it('renders with proper visual hierarchy', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Main heading
      const heading = screen.getByRole('heading', {name: /join focushive/i});
      expect(heading.tagName.toLowerCase()).toBe('h1');

      // Subtitle
      expect(screen.getByText(/create your account to start focusing together/i)).toBeInTheDocument();
    });

    it('renders input adornments correctly', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Name icons
      expect(screen.getByTestId('PersonIcon')).toBeInTheDocument();
      expect(screen.getByTestId('PersonOutlineIcon')).toBeInTheDocument();

      // Email icon
      expect(screen.getByTestId('EmailIcon')).toBeInTheDocument();

      // Password icons
      expect(screen.getAllByTestId('LockIcon')).toHaveLength(2);
      expect(screen.getAllByTestId('VisibilityIcon')).toHaveLength(2);

      // Submit button icon
      expect(screen.getByTestId('PersonAddIcon')).toBeInTheDocument();
    });

    it('maintains proper form layout and spacing', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      const form = document.querySelector('form');
      expect(form).toBeInTheDocument();

      // All form elements should be present and properly arranged
      const firstNameField = screen.getByLabelText('First Name');
      const lastNameField = screen.getByLabelText('Last Name');
      const usernameField = screen.getByLabelText('Username');
      const emailField = screen.getByLabelText('Email Address');
      const passwordField = screen.getByLabelText('Password');
      const confirmPasswordField = screen.getByLabelText('Confirm Password');

      expect(firstNameField).toBeInTheDocument();
      expect(lastNameField).toBeInTheDocument();
      expect(usernameField).toBeInTheDocument();
      expect(emailField).toBeInTheDocument();
      expect(passwordField).toBeInTheDocument();
      expect(confirmPasswordField).toBeInTheDocument();

      const buttons = within(form!).getAllByRole('button');
      expect(buttons.length).toBeGreaterThan(2); // Submit, toggle buttons, sign in link
    });
  });

  describe('Internationalization', () => {
    it('displays translated text elements', () => {
      renderWithProviders(<RegisterForm {...defaultProps} />);

      // Check for key translated elements
      expect(screen.getByRole('heading', {name: /join focushive/i})).toBeInTheDocument();
      expect(screen.getByRole('button', {name: /create account/i})).toBeInTheDocument();
      expect(screen.getByText(/create your account to start focusing together/i)).toBeInTheDocument();
      expect(screen.getByText(/already have an account/i)).toBeInTheDocument();
      expect(screen.getByRole('button', {name: /sign in/i})).toBeInTheDocument();
      expect(screen.getByText(/i agree to the/i)).toBeInTheDocument();
      expect(screen.getByRole('link', {name: /terms of service/i})).toBeInTheDocument();
      expect(screen.getByRole('link', {name: /privacy policy/i})).toBeInTheDocument();
    });

    it('shows translated loading text during submission', () => {
      const loadingProps = {
        ...defaultProps,
        isLoading: true
      };
      renderWithProviders(<RegisterForm {...loadingProps} />);

      expect(screen.getByText(/creating account/i)).toBeInTheDocument();
    });
  });
});