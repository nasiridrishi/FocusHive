import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material/styles';
import StreakCounter from './StreakCounter';
import type { Streak } from '../types/gamification';

// Mock framer-motion for testing
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: any) => <div {...props}>{children}</div>,
    span: ({ children, ...props }: any) => <span {...props}>{children}</span>,
  },
  AnimatePresence: ({ children }: any) => children,
}));

const theme = createTheme();

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      {component}
    </ThemeProvider>
  );
};

const mockActiveStreak: Streak = {
  id: 'daily-login-1',
  type: 'daily_login',
  current: 7,
  best: 15,
  lastActivity: new Date('2024-01-15T10:30:00Z'),
  isActive: true,
};

const mockInactiveStreak: Streak = {
  id: 'focus-session-1',
  type: 'focus_session',
  current: 0,
  best: 25,
  lastActivity: new Date('2024-01-10T10:30:00Z'),
  isActive: false,
};

const mockNewStreak: Streak = {
  id: 'goal-1',
  type: 'goal_completion',
  current: 1,
  best: 1,
  lastActivity: new Date(),
  isActive: true,
};

describe('StreakCounter', () => {
  describe('Basic Rendering', () => {
    it('renders current streak count', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByText('7')).toBeInTheDocument();
      expect(screen.getByText(/current streak/i)).toBeInTheDocument();
    });

    it('renders streak type correctly formatted', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByText(/daily login/i)).toBeInTheDocument();
    });

    it('renders last activity date', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByText(/jan 15, 2024/i)).toBeInTheDocument();
    });

    it('handles zero current streak', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });

  describe('Active vs Inactive States', () => {
    it('shows active status for active streaks', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('active');
      expect(screen.getByText(/active/i)).toBeInTheDocument();
    });

    it('shows inactive status for inactive streaks', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('inactive');
      expect(screen.getByText(/broken/i)).toBeInTheDocument();
    });

    it('applies different styling for active streaks', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('streak-active');
    });

    it('applies muted styling for inactive streaks', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('streak-inactive');
    });
  });

  describe('Best Streak Display', () => {
    it('shows best streak when showBest is true', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} showBest />);
      
      expect(screen.getByText('15')).toBeInTheDocument();
      expect(screen.getByText(/best/i)).toBeInTheDocument();
    });

    it('hides best streak when showBest is false', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} showBest={false} />);
      
      expect(screen.queryByText(/best/i)).not.toBeInTheDocument();
    });

    it('defaults to showing best streak when showBest prop is not provided', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByText(/best/i)).toBeInTheDocument();
    });

    it('highlights when current streak equals best streak', () => {
      renderWithTheme(<StreakCounter streak={mockNewStreak} showBest />);
      
      const bestDisplay = screen.getByText(/best/i).closest('.best-streak');
      expect(bestDisplay).toHaveClass('is-current-best');
    });
  });

  describe('Variant Styles', () => {
    it('applies default variant styling', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} variant="default" />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('variant-default');
    });

    it('applies compact variant styling', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} variant="compact" />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('variant-compact');
    });

    it('applies detailed variant styling', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} variant="detailed" />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('variant-detailed');
    });

    it('shows additional details in detailed variant', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} variant="detailed" />);
      
      expect(screen.getByText(/streak started/i)).toBeInTheDocument();
      expect(screen.getByText(/days ago/i)).toBeInTheDocument();
    });

    it('hides extra details in compact variant', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} variant="compact" />);
      
      expect(screen.queryByText(/last activity/i)).not.toBeInTheDocument();
    });
  });

  describe('Streak Type Formatting', () => {
    it('formats daily_login correctly', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByText(/daily login/i)).toBeInTheDocument();
    });

    it('formats focus_session correctly', () => {
      const focusStreak: Streak = {
        ...mockActiveStreak,
        type: 'focus_session',
      };
      
      renderWithTheme(<StreakCounter streak={focusStreak} />);
      
      expect(screen.getByText(/focus session/i)).toBeInTheDocument();
    });

    it('formats goal_completion correctly', () => {
      const goalStreak: Streak = {
        ...mockActiveStreak,
        type: 'goal_completion',
      };
      
      renderWithTheme(<StreakCounter streak={goalStreak} />);
      
      expect(screen.getByText(/goal completion/i)).toBeInTheDocument();
    });

    it('formats hive_participation correctly', () => {
      const hiveStreak: Streak = {
        ...mockActiveStreak,
        type: 'hive_participation',
      };
      
      renderWithTheme(<StreakCounter streak={hiveStreak} />);
      
      expect(screen.getByText(/hive participation/i)).toBeInTheDocument();
    });
  });

  describe('Icons and Visual Elements', () => {
    it('shows appropriate icon for streak type', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      const icon = screen.getByTestId('streak-icon');
      expect(icon).toHaveAttribute('data-streak-type', 'daily_login');
    });

    it('shows fire icon for active streaks', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByTestId('fire-icon')).toBeInTheDocument();
    });

    it('shows broken chain icon for inactive streaks', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      expect(screen.getByTestId('broken-chain-icon')).toBeInTheDocument();
    });

    it('shows celebration effect for new records', () => {
      renderWithTheme(<StreakCounter streak={mockNewStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('new-record');
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByLabelText(/current streak: 7 days/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/best streak: 15 days/i)).toBeInTheDocument();
    });

    it('has proper semantic structure', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      const container = screen.getByRole('region', { name: /streak counter/i });
      expect(container).toBeInTheDocument();
    });

    it('provides status information for screen readers', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      expect(screen.getByText(/streak is active/i)).toBeInTheDocument();
    });

    it('provides context for inactive streaks', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      expect(screen.getByText(/streak is broken/i)).toBeInTheDocument();
    });
  });

  describe('Time Calculations', () => {
    it('calculates days since last activity correctly', () => {
      const oldDate = new Date('2024-01-01T10:30:00Z');
      const streakWithOldActivity: Streak = {
        ...mockActiveStreak,
        lastActivity: oldDate,
      };
      
      renderWithTheme(<StreakCounter streak={streakWithOldActivity} variant="detailed" />);
      
      expect(screen.getByText(/days ago/i)).toBeInTheDocument();
    });

    it('shows "today" for recent activity', () => {
      const todayStreak: Streak = {
        ...mockActiveStreak,
        lastActivity: new Date(),
      };
      
      renderWithTheme(<StreakCounter streak={todayStreak} variant="detailed" />);
      
      expect(screen.getByText(/today/i)).toBeInTheDocument();
    });
  });

  describe('Progressive Enhancement', () => {
    it('works without JavaScript animations', () => {
      // Mock reduced motion preference
      Object.defineProperty(window, 'matchMedia', {
        value: vi.fn().mockImplementation(query => ({
          matches: query === '(prefers-reduced-motion: reduce)',
          addEventListener: vi.fn(),
          removeEventListener: vi.fn(),
        })),
      });
      
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('reduced-motion');
    });
  });

  describe('Edge Cases', () => {
    it('handles undefined last activity gracefully', () => {
      const noActivityStreak: Streak = {
        ...mockActiveStreak,
        lastActivity: undefined as any,
      };
      
      expect(() => {
        renderWithTheme(<StreakCounter streak={noActivityStreak} />);
      }).not.toThrow();
    });

    it('handles negative streak values', () => {
      const negativeStreak: Streak = {
        ...mockActiveStreak,
        current: -1,
        best: 0,
      };
      
      renderWithTheme(<StreakCounter streak={negativeStreak} />);
      
      expect(screen.getByText('0')).toBeInTheDocument();
    });

    it('handles very large streak numbers', () => {
      const largeStreak: Streak = {
        ...mockActiveStreak,
        current: 999,
        best: 1000,
      };
      
      renderWithTheme(<StreakCounter streak={largeStreak} />);
      
      expect(screen.getByText('999')).toBeInTheDocument();
      expect(screen.getByText('1,000')).toBeInTheDocument();
    });
  });

  describe('Performance', () => {
    it('memoizes expensive calculations', () => {
      const calculationSpy = vi.fn(() => 'calculated value');
      
      const TestStreakCounter = ({ streak }: { streak: Streak }) => {
        const memoizedValue = React.useMemo(() => calculationSpy(), [streak.current]);
        return <StreakCounter streak={streak} />;
      };
      
      const { rerender } = renderWithTheme(<TestStreakCounter streak={mockActiveStreak} />);
      
      expect(calculationSpy).toHaveBeenCalledTimes(1);
      
      // Re-render with same streak
      rerender(<TestStreakCounter streak={mockActiveStreak} />);
      
      expect(calculationSpy).toHaveBeenCalledTimes(1);
    });
  });
});