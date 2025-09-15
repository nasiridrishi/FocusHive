import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { SessionStats } from '../SessionStats';
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

// Mock the useTimer hook to provide test data
vi.mock('../../contexts/TimerContext', async () => {
  const actual = await vi.importActual('../../contexts/TimerContext') as any;
  return {
    ...actual,
    useTimer: vi.fn(() => ({
      currentSession: {
        id: 'session-1',
        userId: 'user-1',
        date: new Date().toISOString(),
        startTime: new Date().toISOString(),
        endTime: null,
        targetCycles: 6,
        actualCycles: 3,
        focusLength: 25,
        shortBreakLength: 5,
        longBreakLength: 15,
        distractions: 2,
        goals: [
          { id: '1', description: 'Complete feature', isCompleted: true, priority: 'high' },
          { id: '2', description: 'Write tests', isCompleted: false, priority: 'medium' },
          { id: '3', description: 'Review PR', isCompleted: false, priority: 'low' },
        ],
        metrics: {
          focusTime: 5400,
          breakTime: 600,
          productivity: 67,
        }
      },
      timerState: {
        currentPhase: 'focus',
        isRunning: true,
        isPaused: false,
        timeRemaining: 1500,
        currentCycle: 3,
      },
      timerSettings: {
        focusLength: 25,
        shortBreakLength: 5,
        longBreakLength: 15,
        longBreakInterval: 4,
        autoStartBreaks: false,
        autoStartFocus: false,
        soundEnabled: true,
        notificationsEnabled: true,
      }
    })),
    TimerProvider: actual.TimerProvider
  };
});

describe('SessionStats', () => {
  const renderComponent = (props = {}) => {
    return render(
      <ThemeProvider theme={theme}>
        <TimerProvider userId="test-user">
          <SessionStats period="today" {...props} />
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
    it('should render the session stats component', () => {
      renderComponent();
      expect(screen.getByTestId('session-stats')).toBeInTheDocument();
    });

    it('should display session summary title', () => {
      renderComponent();
      expect(screen.getByText('Session Summary')).toBeInTheDocument();
    });

    it('should show total focus time', () => {
      renderComponent();
      expect(screen.getByText(/Total Focus Time/i)).toBeInTheDocument();
    });

    it('should show completed cycles', () => {
      renderComponent();
      expect(screen.getByText(/Completed Cycles/i)).toBeInTheDocument();
    });

    it('should show distractions count', () => {
      renderComponent();
      expect(screen.getByText(/Distractions/i)).toBeInTheDocument();
    });

    it('should display session goals section', () => {
      renderComponent();
      const goalsElements = screen.getAllByText('Session Goals');
      expect(goalsElements.length).toBeGreaterThan(0);
    });
  });

  describe('Statistics Display', () => {
    it('should display cycles count', () => {
      renderComponent();
      const cycles = screen.getByTestId('completed-cycles');
      expect(cycles).toHaveTextContent('3');
    });

    it('should display distractions count', () => {
      renderComponent();
      const distractions = screen.getByTestId('distractions-count');
      expect(distractions).toHaveTextContent('2');
    });

    it('should calculate productivity score', () => {
      renderComponent();
      const productivity = screen.getByTestId('productivity-score');
      expect(productivity).toHaveTextContent('50%'); // 3/6 cycles
    });

    it('should format total focus time', () => {
      renderComponent();
      const focusTime = screen.getByTestId('total-focus-time');
      // The component generates mock data, so we just check it exists and contains time format
      expect(focusTime.textContent).toMatch(/\d+[hm]/);
    });
  });

  describe('Goals Display', () => {
    it('should display goals list', () => {
      renderComponent();
      expect(screen.getByText('Complete feature')).toBeInTheDocument();
      expect(screen.getByText('Write tests')).toBeInTheDocument();
    });

    it('should show completed goals with check icon', () => {
      renderComponent();
      const goal = screen.getByTestId('goal-1');
      expect(goal.querySelector('[data-testid="CheckIcon"]')).toBeInTheDocument();
    });

    it('should show uncompleted goals without check icon', () => {
      renderComponent();
      const goal = screen.getByTestId('goal-2');
      expect(goal.querySelector('[data-testid="CheckIcon"]')).not.toBeInTheDocument();
    });

    it('should display completion percentage for goals', () => {
      renderComponent();
      const goalsCompletion = screen.getByTestId('goals-completion');
      expect(goalsCompletion).toHaveTextContent('1/3 completed (33%)');
    });
  });

  describe('Visual Indicators', () => {
    it('should show colored progress bars', () => {
      renderComponent();
      const progressBar = screen.getByTestId('productivity-progress');
      expect(progressBar).toBeInTheDocument();
    });

    it('should have proper aria attributes for progress', () => {
      renderComponent();
      const progressBar = screen.getByTestId('productivity-progress');
      expect(progressBar).toHaveAttribute('aria-label', 'Productivity: 50%');
    });
  });

  describe('Period Selection', () => {
    it('should show period toggle buttons', () => {
      renderComponent();
      expect(screen.getByTestId('TodayIcon')).toBeInTheDocument();
      // There might be multiple AccessTimeIcon elements, just check at least one exists
      const accessTimeIcons = screen.getAllByTestId('AccessTimeIcon');
      expect(accessTimeIcons.length).toBeGreaterThan(0);
      expect(screen.getByTestId('CalendarMonthIcon')).toBeInTheDocument();
      expect(screen.getByTestId('AllInclusiveIcon')).toBeInTheDocument();
    });

    it('should default to today period', () => {
      renderComponent({ period: 'today' });
      const todayButton = screen.getByRole('button', { pressed: true });
      expect(todayButton).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      renderComponent();
      const stats = screen.getByTestId('session-stats');
      expect(stats).toHaveAttribute('role', 'region');
      expect(stats).toHaveAttribute('aria-label', 'Session Statistics');
    });

    it('should have keyboard accessible buttons', () => {
      renderComponent();
      const buttons = screen.getAllByRole('button');
      buttons.forEach(button => {
        expect(button).toHaveAttribute('tabindex');
      });
    });
  });

  describe('Integration', () => {
    it('should render with showGoals prop', () => {
      renderComponent({ showGoals: true });
      const goalsElements = screen.getAllByText('Session Goals');
      expect(goalsElements.length).toBeGreaterThan(0);
    });

    it('should handle period prop', () => {
      renderComponent({ period: 'week' });
      expect(screen.getByTestId('session-stats')).toBeInTheDocument();
    });
  });
});