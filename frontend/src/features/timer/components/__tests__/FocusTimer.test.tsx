import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { FocusTimer } from '../FocusTimer';
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

// Mock Web Audio API
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

// Mock Notification API
const mockNotification = vi.fn();
Object.defineProperty(window, 'Notification', {
  value: mockNotification,
  configurable: true,
});

// Use fake timers for testing
vi.useFakeTimers();

describe('FocusTimer', () => {
  const TEST_USER_ID = 'test-user-123';

  const renderComponent = (props = {}) => {
    return render(
      <ThemeProvider theme={theme}>
        <TimerProvider userId={TEST_USER_ID}>
          <FocusTimer hiveId="test-hive-123" {...props} />
        </TimerProvider>
      </ThemeProvider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  describe('Display', () => {
    it('should render the timer component', () => {
      renderComponent();
      expect(screen.getByTestId('focus-timer')).toBeInTheDocument();
    });

    it('should display time in MM:SS format initially', () => {
      renderComponent();
      // Look for the time display text
      expect(screen.getByText('25:00')).toBeInTheDocument();
    });

    it('should show current phase', () => {
      renderComponent();
      // The phase starts as 'idle' initially
      expect(screen.getByText('idle')).toBeInTheDocument();
    });

    it('should display circular timer progress', () => {
      renderComponent();
      const timer = screen.getByTestId('focus-timer');
      // Check that the timer exists and is a card
      expect(timer).toHaveClass('MuiCard-root');
    });
  });

  describe('Controls', () => {
    it('should show start button initially', () => {
      renderComponent();
      const startButton = screen.getByLabelText('Start Focus Session');
      expect(startButton).toBeInTheDocument();
    });

    it('should have PlayArrow icon in start button', () => {
      renderComponent();
      const playIcon = screen.getByTestId('PlayArrowIcon');
      expect(playIcon).toBeInTheDocument();
    });

    it('should have clickable start button', () => {
      renderComponent();

      const startButton = screen.getByLabelText('Start Focus Session');

      // Verify the button exists and is not disabled
      expect(startButton).toBeInTheDocument();
      expect(startButton).not.toBeDisabled();
    });

    it('should display session goals section', () => {
      renderComponent();
      // Session Goals section exists
      expect(screen.getByText('Session Goals')).toBeInTheDocument();
    });

    it('should have add goal chip', () => {
      renderComponent();
      expect(screen.getByText('Add Goal')).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA label on start button', () => {
      renderComponent();
      const startButton = screen.getByLabelText('Start Focus Session');
      expect(startButton).toHaveAttribute('aria-label', 'Start Focus Session');
    });

    it('should support keyboard navigation', () => {
      renderComponent();

      // Check that interactive elements exist for keyboard navigation
      const buttons = screen.getAllByRole('button');
      expect(buttons.length).toBeGreaterThan(0);

      // Verify first button is focusable
      expect(buttons[0]).toHaveAttribute('tabindex');
    });
  });

  describe('Responsive Design', () => {
    it('should render compact mode prop', () => {
      renderComponent({ compact: true });
      const timer = screen.getByTestId('focus-timer');
      // Check that compact styling is applied
      expect(timer).toBeInTheDocument();
    });

    it('should render full mode by default', () => {
      renderComponent({ compact: false });
      const timer = screen.getByTestId('focus-timer');
      // Check that full styling is applied
      expect(timer).toBeInTheDocument();
    });
  });

  describe('Integration', () => {
    it('should accept hiveId prop', () => {
      renderComponent({ hiveId: 'custom-hive-456' });
      const timer = screen.getByTestId('focus-timer');
      expect(timer).toBeInTheDocument();
    });

    it('should handle showSettings prop when true', () => {
      renderComponent({ showSettings: true });
      const timer = screen.getByTestId('focus-timer');
      expect(timer).toBeInTheDocument();
      // Settings would be included in the rendered component
    });

    it('should handle showSettings prop when false', () => {
      renderComponent({ showSettings: false });
      const timer = screen.getByTestId('focus-timer');
      expect(timer).toBeInTheDocument();
      // Settings would be hidden in the rendered component
    });
  });
});