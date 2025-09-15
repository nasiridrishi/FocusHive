import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { TimerControls } from '../TimerControls';
import { TimerProvider } from '../../contexts/TimerContext';

// Create a theme for testing
const theme = createTheme();

// Mock WebSocket context
vi.mock('@/shared/contexts/WebSocketContext', () => ({
  useWebSocket: () => ({
    isConnected: true,
    emit: vi.fn(),
    on: vi.fn(() => () => {}),
  }),
}));

// Mock Presence context
vi.mock('@/shared/contexts/PresenceContext', () => ({
  usePresence: () => ({
    currentPresence: { hiveId: 'test-hive-123' },
    updatePresence: vi.fn(),
  }),
}));

// Mock Audio
const mockAudioContext = {
  createOscillator: vi.fn(() => ({
    connect: vi.fn(),
    frequency: { setValueAtTime: vi.fn() },
    start: vi.fn(),
    stop: vi.fn(),
  })),
  createGain: vi.fn(() => ({
    connect: vi.fn(),
    gain: {
      setValueAtTime: vi.fn(),
      exponentialRampToValueAtTime: vi.fn(),
    },
  })),
  destination: {},
  currentTime: 0,
  close: vi.fn(),
};

Object.defineProperty(window, 'AudioContext', {
  value: vi.fn(() => mockAudioContext),
  configurable: true,
});

describe('TimerControls', () => {
  const TEST_USER_ID = 'test-user-123';

  const renderComponent = (props = {}) => {
    return render(
      <ThemeProvider theme={theme}>
        <TimerProvider userId={TEST_USER_ID}>
          <TimerControls {...props} />
        </TimerProvider>
      </ThemeProvider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Display', () => {
    it('should render the timer controls component', () => {
      renderComponent();
      expect(screen.getByTestId('timer-controls')).toBeInTheDocument();
    });

    it('should display duration selector label', () => {
      renderComponent();
      expect(screen.getByText(/Duration/i)).toBeInTheDocument();
    });

    it('should display sound toggle', () => {
      renderComponent();
      expect(screen.getByTestId('sound-toggle')).toBeInTheDocument();
    });

    it('should display auto-start breaks toggle', () => {
      renderComponent();
      expect(screen.getByTestId('auto-start-breaks-toggle')).toBeInTheDocument();
    });
  });

  describe('Duration Selector', () => {
    it('should display all duration options', () => {
      renderComponent();

      // Check for duration buttons or dropdown options
      const durations = ['15', '25', '45', '60'];
      durations.forEach(duration => {
        expect(screen.getByTestId(`duration-${duration}`)).toBeInTheDocument();
      });
    });

    it('should show 25 minutes as default selected', () => {
      renderComponent();
      const defaultDuration = screen.getByTestId('duration-25');
      expect(defaultDuration).toHaveAttribute('aria-pressed', 'true');
    });

    it('should update selection when duration is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const duration45 = screen.getByTestId('duration-45');
      await user.click(duration45);

      expect(duration45).toHaveAttribute('aria-pressed', 'true');
    });

    it('should display minutes label for each duration', () => {
      renderComponent();
      expect(screen.getByText('15 min')).toBeInTheDocument();
      expect(screen.getByText('25 min')).toBeInTheDocument();
      expect(screen.getByText('45 min')).toBeInTheDocument();
      expect(screen.getByText('60 min')).toBeInTheDocument();
    });
  });

  describe('Sound Toggle', () => {
    it('should display sound icon when enabled', () => {
      renderComponent();
      const soundToggle = screen.getByTestId('sound-toggle');
      expect(soundToggle.querySelector('[data-testid="VolumeUpIcon"]')).toBeInTheDocument();
    });

    it('should toggle sound on click', async () => {
      const user = userEvent.setup();
      renderComponent();

      const soundToggle = screen.getByTestId('sound-toggle');
      await user.click(soundToggle);

      // After clicking, should show volume off icon
      expect(soundToggle.querySelector('[data-testid="VolumeOffIcon"]')).toBeInTheDocument();
    });

    it('should have proper aria-label', () => {
      renderComponent();
      const soundToggle = screen.getByTestId('sound-toggle');
      expect(soundToggle).toHaveAttribute('aria-label', 'Toggle sound');
    });
  });

  describe('Auto-Start Breaks Toggle', () => {
    it('should display auto-start label', () => {
      renderComponent();
      expect(screen.getByText(/Auto-start breaks/i)).toBeInTheDocument();
    });

    it('should be unchecked by default', () => {
      renderComponent();
      const autoStartToggle = screen.getByTestId('auto-start-breaks-toggle');
      // MUI Switch renders an input inside the element with data-testid
      const switchInput = autoStartToggle.querySelector('input[type="checkbox"]') as HTMLInputElement;
      expect(switchInput).toBeDefined();
      expect(switchInput.checked).toBe(false);
    });

    it('should toggle auto-start on click', async () => {
      const user = userEvent.setup();
      renderComponent();

      const autoStartToggle = screen.getByTestId('auto-start-breaks-toggle');

      await user.click(autoStartToggle);

      // Check the input element after click
      const switchInput = autoStartToggle.querySelector('input[type="checkbox"]') as HTMLInputElement;
      expect(switchInput).toBeDefined();
      expect(switchInput.checked).toBe(true);
    });

    it('should have proper aria-label', () => {
      renderComponent();
      const autoStartToggle = screen.getByTestId('auto-start-breaks-toggle');
      expect(autoStartToggle).toHaveAttribute('aria-label', 'Toggle auto-start breaks');
    });
  });

  describe('Break Settings', () => {
    it('should display short break duration setting', () => {
      renderComponent();
      expect(screen.getByText('Short break: 5 min')).toBeInTheDocument();
    });

    it('should display long break duration setting', () => {
      renderComponent();
      expect(screen.getByText('Long break: 15 min')).toBeInTheDocument();
    });

    it('should display long break interval setting', () => {
      renderComponent();
      expect(screen.getByText('Long break after: 4 pomodoros')).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      renderComponent();
      const controls = screen.getByTestId('timer-controls');
      expect(controls).toHaveAttribute('role', 'region');
      expect(controls).toHaveAttribute('aria-label', 'Timer Settings');
    });

    it('should support keyboard navigation', () => {
      renderComponent();

      // All interactive elements should be keyboard accessible
      const buttons = screen.getAllByRole('button');
      buttons.forEach(button => {
        expect(button).toHaveAttribute('tabindex');
      });
    });

    it('should have descriptive labels for screen readers', () => {
      renderComponent();

      expect(screen.getByLabelText(/Focus duration/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Toggle sound/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Toggle auto-start breaks/i)).toBeInTheDocument();
    });
  });

  describe('Responsive Design', () => {
    it('should render in compact mode when prop is set', () => {
      renderComponent({ compact: true });
      const controls = screen.getByTestId('timer-controls');
      expect(controls).toHaveClass('timer-controls-compact');
    });

    it('should render in full mode by default', () => {
      renderComponent({ compact: false });
      const controls = screen.getByTestId('timer-controls');
      expect(controls).toHaveClass('timer-controls-full');
    });

    it('should stack vertically on small screens', () => {
      renderComponent({ compact: true });

      // Check that the layout is vertical for compact mode
      const container = screen.getByTestId('controls-container');
      expect(container).toHaveStyle({ flexDirection: 'column' });
    });
  });

  describe('Integration', () => {
    it('should call onDurationChange when duration is selected', async () => {
      const onDurationChange = vi.fn();
      const user = userEvent.setup();
      renderComponent({ onDurationChange });

      const duration45 = screen.getByTestId('duration-45');
      await user.click(duration45);

      expect(onDurationChange).toHaveBeenCalledWith(45);
    });

    it('should call onSoundToggle when sound is toggled', async () => {
      const onSoundToggle = vi.fn();
      const user = userEvent.setup();
      renderComponent({ onSoundToggle });

      const soundToggle = screen.getByTestId('sound-toggle');
      await user.click(soundToggle);

      expect(onSoundToggle).toHaveBeenCalled();
    });

    it('should call onAutoStartToggle when auto-start is toggled', async () => {
      const onAutoStartToggle = vi.fn();
      const user = userEvent.setup();
      renderComponent({ onAutoStartToggle });

      const autoStartToggle = screen.getByTestId('auto-start-breaks-toggle');
      await user.click(autoStartToggle);

      expect(onAutoStartToggle).toHaveBeenCalled();
    });
  });
});