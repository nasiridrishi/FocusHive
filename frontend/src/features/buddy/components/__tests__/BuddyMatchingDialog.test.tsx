import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BuddyMatchingDialog } from '../BuddyMatchingDialog';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import { useBuddyMatching } from '@/features/buddy/hooks/useBuddyMatching';

// Mock dependencies
vi.mock('@/features/buddy/hooks/useBuddyMatching', () => ({
  useBuddyMatching: () => ({
    submitPreferences: vi.fn(),
    findMatches: vi.fn(),
    connectWithBuddy: vi.fn(),
    loading: false,
    error: null,
    suggestedMatches: mockSuggestedMatches
  })
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'current-user-id',
      username: 'CurrentUser',
      email: 'user@example.com',
      timezone: 'America/New_York'
    },
    isAuthenticated: true
  })
}));

const mockSuggestedMatches = [
  {
    id: 'buddy-1',
    username: 'Alice',
    avatar: '/avatar1.png',
    timezone: 'America/New_York',
    focusAreas: ['Web Development', 'React'],
    studyHours: 'Morning',
    matchScore: 95,
    bio: 'Passionate about frontend development'
  },
  {
    id: 'buddy-2',
    username: 'Bob',
    avatar: '/avatar2.png',
    timezone: 'Europe/London',
    focusAreas: ['Data Science', 'Python'],
    studyHours: 'Evening',
    matchScore: 78,
    bio: 'Working on ML projects'
  }
];

const renderComponent = (props = {}) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <BuddyMatchingDialog
          open={true}
          onClose={() => {}}
          {...props}
        />
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe('BuddyMatchingDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering Tests', () => {
    it('should render dialog container', () => {
      renderComponent();
      expect(screen.getByTestId('buddy-matching-dialog')).toBeInTheDocument();
    });

    it('should display dialog title', () => {
      renderComponent();
      expect(screen.getByText('Find Your Study Buddy')).toBeInTheDocument();
    });

    it('should render preferences form', () => {
      renderComponent();
      expect(screen.getByTestId('preferences-form')).toBeInTheDocument();
    });

    it('should display timezone selector', () => {
      renderComponent();
      expect(screen.getByTestId('timezone-selector')).toBeInTheDocument();
    });

    it('should show focus areas input', () => {
      renderComponent();
      expect(screen.getByLabelText('Focus Areas')).toBeInTheDocument();
    });

    it('should display study hours selector', () => {
      renderComponent();
      expect(screen.getByTestId('study-hours-selector')).toBeInTheDocument();
    });

    it('should show communication preferences', () => {
      renderComponent();
      expect(screen.getByText('Communication Preferences')).toBeInTheDocument();
    });

    it('should render availability schedule', () => {
      renderComponent();
      expect(screen.getByTestId('availability-schedule')).toBeInTheDocument();
    });

    it('should display language preference', () => {
      renderComponent();
      expect(screen.getByLabelText('Preferred Languages')).toBeInTheDocument();
    });

    it('should show submit button', () => {
      renderComponent();
      expect(screen.getByTestId('submit-preferences-button')).toBeInTheDocument();
    });
  });

  describe('Form Interaction Tests', () => {
    it('should handle focus area selection', () => {
      renderComponent();
      const focusInput = screen.getByLabelText('Focus Areas');
      fireEvent.change(focusInput, { target: { value: 'React, TypeScript' } });
      expect((focusInput as HTMLInputElement).value).toBe('React, TypeScript');
    });

    it('should update timezone selection', () => {
      renderComponent();
      const timezoneSelect = screen.getByTestId('timezone-selector');
      fireEvent.change(timezoneSelect, { target: { value: 'Europe/London' } });
      expect((timezoneSelect as HTMLSelectElement).value).toBe('Europe/London');
    });

    it('should handle study hours selection', () => {
      renderComponent();
      const morningCheckbox = screen.getByLabelText('Morning (6AM - 12PM)');
      fireEvent.click(morningCheckbox);
      expect(morningCheckbox).toBeChecked();
    });

    it('should toggle communication preferences', () => {
      renderComponent();
      const videoCallCheckbox = screen.getByLabelText('Video Calls');
      fireEvent.click(videoCallCheckbox);
      expect(videoCallCheckbox).toBeChecked();
    });

    it('should set availability days', () => {
      renderComponent();
      const mondayCheckbox = screen.getByLabelText('Monday');
      fireEvent.click(mondayCheckbox);
      expect(mondayCheckbox).toBeChecked();
    });

    it('should select experience level', () => {
      renderComponent();
      const experienceSelect = screen.getByLabelText('Experience Level');
      fireEvent.change(experienceSelect, { target: { value: 'intermediate' } });
      expect((experienceSelect as HTMLSelectElement).value).toBe('intermediate');
    });

    it('should add language preference', () => {
      renderComponent();
      const languageInput = screen.getByLabelText('Preferred Languages');
      fireEvent.change(languageInput, { target: { value: 'English, Spanish' } });
      expect((languageInput as HTMLInputElement).value).toBe('English, Spanish');
    });

    it('should set commitment level', () => {
      renderComponent();
      const commitmentSlider = screen.getByTestId('commitment-slider');
      fireEvent.change(commitmentSlider, { target: { value: '15' } });
      expect((commitmentSlider as HTMLInputElement).value).toBe('15');
    });

    it('should enter bio text', () => {
      renderComponent();
      const bioInput = screen.getByLabelText('About You');
      fireEvent.change(bioInput, { target: { value: 'Looking for accountability partner' } });
      expect((bioInput as HTMLTextAreaElement).value).toBe('Looking for accountability partner');
    });

    it('should validate required fields', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Focus areas are required')).toBeInTheDocument();
      });
    });
  });

  describe('Timezone Features', () => {
    it('should display current timezone', () => {
      renderComponent();
      expect(screen.getByText(/America\/New_York/)).toBeInTheDocument();
    });

    it('should search timezones', () => {
      renderComponent();
      const searchInput = screen.getByPlaceholderText('Search timezone...');
      fireEvent.change(searchInput, { target: { value: 'London' } });
      expect(screen.getByText('Europe/London')).toBeInTheDocument();
    });

    it('should group timezones by region', () => {
      renderComponent();
      expect(screen.getByText('Americas')).toBeInTheDocument();
      expect(screen.getByText('Europe')).toBeInTheDocument();
      expect(screen.getByText('Asia')).toBeInTheDocument();
    });

    it('should show UTC offset', () => {
      renderComponent();
      expect(screen.getByText(/UTC-5/)).toBeInTheDocument();
    });

    it('should detect user timezone automatically', () => {
      renderComponent();
      const detectButton = screen.getByTestId('detect-timezone-button');
      fireEvent.click(detectButton);
      expect((screen.getByTestId('timezone-selector') as HTMLSelectElement).value).toBeTruthy();
    });
  });

  describe('Matching Suggestions', () => {
    it('should display suggested matches after submission', async () => {
      renderComponent();
      const focusInput = screen.getByLabelText('Focus Areas');
      fireEvent.change(focusInput, { target: { value: 'React' } });

      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByTestId('suggested-matches')).toBeInTheDocument();
      });
    });

    it('should show match scores', async () => {
      renderComponent();
      // Submit form first
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('95% Match')).toBeInTheDocument();
        expect(screen.getByText('78% Match')).toBeInTheDocument();
      });
    });

    it('should display buddy profiles', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
        expect(screen.getByText('Bob')).toBeInTheDocument();
      });
    });

    it('should show buddy focus areas', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Web Development')).toBeInTheDocument();
        expect(screen.getByText('Data Science')).toBeInTheDocument();
      });
    });

    it('should handle connect request', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        const connectButton = screen.getByTestId('connect-buddy-buddy-1');
        fireEvent.click(connectButton);
        expect(connectButton).toHaveTextContent('Request Sent');
      });
    });
  });

  describe('Advanced Preferences', () => {
    it('should toggle advanced options', () => {
      renderComponent();
      const advancedToggle = screen.getByTestId('advanced-options-toggle');
      fireEvent.click(advancedToggle);
      expect(screen.getByTestId('advanced-preferences')).toBeInTheDocument();
    });

    it('should set learning style preference', () => {
      renderComponent();
      const advancedToggle = screen.getByTestId('advanced-options-toggle');
      fireEvent.click(advancedToggle);

      const learningStyle = screen.getByLabelText('Learning Style');
      fireEvent.change(learningStyle, { target: { value: 'visual' } });
      expect((learningStyle as HTMLSelectElement).value).toBe('visual');
    });

    it('should specify goals', () => {
      renderComponent();
      const advancedToggle = screen.getByTestId('advanced-options-toggle');
      fireEvent.click(advancedToggle);

      const goalsInput = screen.getByLabelText('Goals');
      fireEvent.change(goalsInput, { target: { value: 'Complete React course' } });
      expect((goalsInput as HTMLTextAreaElement).value).toBe('Complete React course');
    });

    it('should set accountability style', () => {
      renderComponent();
      const advancedToggle = screen.getByTestId('advanced-options-toggle');
      fireEvent.click(advancedToggle);

      const strictRadio = screen.getByLabelText('Strict Accountability');
      fireEvent.click(strictRadio);
      expect(strictRadio).toBeChecked();
    });

    it('should configure meeting frequency', () => {
      renderComponent();
      const advancedToggle = screen.getByTestId('advanced-options-toggle');
      fireEvent.click(advancedToggle);

      const frequencySelect = screen.getByLabelText('Meeting Frequency');
      fireEvent.change(frequencySelect, { target: { value: 'daily' } });
      expect((frequencySelect as HTMLSelectElement).value).toBe('daily');
    });
  });

  describe('Form Validation', () => {
    it('should require at least one focus area', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('At least one focus area is required')).toBeInTheDocument();
      });
    });

    it('should require timezone selection', async () => {
      renderComponent();
      const timezoneSelect = screen.getByTestId('timezone-selector');
      fireEvent.change(timezoneSelect, { target: { value: '' } });

      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Timezone is required')).toBeInTheDocument();
      });
    });

    it('should limit bio length', () => {
      renderComponent();
      const bioInput = screen.getByLabelText('About You');
      const longText = 'a'.repeat(501);
      fireEvent.change(bioInput, { target: { value: longText } });
      expect(screen.getByText('500/500')).toBeInTheDocument();
    });

    it('should validate commitment hours', () => {
      renderComponent();
      const commitmentSlider = screen.getByTestId('commitment-slider');
      fireEvent.change(commitmentSlider, { target: { value: '50' } });
      expect((commitmentSlider as HTMLInputElement).value).toBe('40'); // Max value
    });

    it('should require at least one availability day', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Select at least one available day')).toBeInTheDocument();
      });
    });
  });

  describe('Dialog Controls', () => {
    it('should close dialog on cancel', () => {
      const onClose = vi.fn();
      renderComponent({ onClose });

      const cancelButton = screen.getByTestId('cancel-button');
      fireEvent.click(cancelButton);
      expect(onClose).toHaveBeenCalled();
    });

    it('should reset form on reset button', () => {
      renderComponent();
      const focusInput = screen.getByLabelText('Focus Areas');
      fireEvent.change(focusInput, { target: { value: 'React' } });

      const resetButton = screen.getByTestId('reset-form-button');
      fireEvent.click(resetButton);
      expect((focusInput as HTMLInputElement).value).toBe('');
    });

    it('should show loading state during submission', async () => {
      vi.mocked(useBuddyMatching).mockReturnValueOnce({
        submitPreferences: vi.fn(),
        findMatches: vi.fn(),
        connectWithBuddy: vi.fn(),
        loading: true,
        error: null,
        suggestedMatches: []
      });

      renderComponent();
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    });

    it('should display error messages', () => {
      vi.mocked(useBuddyMatching).mockReturnValueOnce({
        submitPreferences: vi.fn(),
        findMatches: vi.fn(),
        connectWithBuddy: vi.fn(),
        loading: false,
        error: new Error('Failed to submit preferences'),
        suggestedMatches: []
      });

      renderComponent();
      expect(screen.getByText('Failed to submit preferences')).toBeInTheDocument();
    });

    it('should show success message after matching', async () => {
      renderComponent();
      const focusInput = screen.getByLabelText('Focus Areas');
      fireEvent.change(focusInput, { target: { value: 'React' } });

      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Matches found!')).toBeInTheDocument();
      });
    });
  });

  describe('Accessibility Tests', () => {
    it('should have proper ARIA labels', () => {
      renderComponent();
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-labelledby');
      expect(screen.getByLabelText('Focus Areas')).toBeInTheDocument();
    });

    it('should support keyboard navigation', () => {
      renderComponent();
      const focusInput = screen.getByLabelText('Focus Areas');
      focusInput.focus();

      fireEvent.keyDown(focusInput, { key: 'Tab' });
      expect(document.activeElement).toBe(screen.getByTestId('timezone-selector'));
    });

    it('should announce form errors to screen readers', async () => {
      renderComponent();
      const submitButton = screen.getByTestId('submit-preferences-button');
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });

    it('should have focus trap in dialog', () => {
      renderComponent();
      const lastElement = screen.getByTestId('cancel-button');
      lastElement.focus();

      fireEvent.keyDown(lastElement, { key: 'Tab' });
      expect(document.activeElement).toBe(screen.getByTestId('dialog-title'));
    });

    it('should close on Escape key', () => {
      const onClose = vi.fn();
      renderComponent({ onClose });

      fireEvent.keyDown(document, { key: 'Escape' });
      expect(onClose).toHaveBeenCalled();
    });
  });

  describe('Responsive Design', () => {
    it('should stack form fields on mobile', () => {
      global.innerWidth = 375;
      renderComponent();

      const form = screen.getByTestId('preferences-form');
      expect(form).toHaveStyle({ flexDirection: 'column' });
    });

    it('should show compact timezone selector on mobile', () => {
      global.innerWidth = 375;
      renderComponent();

      expect(screen.getByTestId('timezone-selector-mobile')).toBeInTheDocument();
    });

    it('should use full screen dialog on mobile', () => {
      global.innerWidth = 375;
      renderComponent();

      const dialog = screen.getByTestId('buddy-matching-dialog');
      expect(dialog).toHaveClass('MuiDialog-fullScreen');
    });

    it('should show horizontal layout on desktop', () => {
      global.innerWidth = 1920;
      renderComponent();

      const form = screen.getByTestId('preferences-form');
      expect(form).toHaveStyle({ flexDirection: 'row' });
    });

    it('should display side-by-side matches on desktop', () => {
      global.innerWidth = 1920;
      renderComponent();

      const matchesGrid = screen.getByTestId('matches-grid');
      expect(matchesGrid).toHaveStyle({ gridTemplateColumns: 'repeat(2, 1fr)' });
    });
  });
});