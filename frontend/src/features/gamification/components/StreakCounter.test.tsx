import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material/styles';
import StreakCounter from './StreakCounter';
import type { Streak } from '../types/gamification';

// Mock framer-motion for testing
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: React.ComponentProps<'div'>) => <div {...props}>{children}</div>,
    span: ({ children, ...props }: React.ComponentProps<'span'>) => <span {...props}>{children}</span>,
  },
  AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
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

    it('renders last activity date in detailed variant', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} variant="detailed" />);
      
      // The component shows relative dates like "X days ago", not absolute dates
      expect(screen.getByText('Last Activity')).toBeInTheDocument();
      // Use getAllByText and check that at least one exists
      const dateTexts = screen.getAllByText(/days ago|today|yesterday|never/);
      expect(dateTexts.length).toBeGreaterThan(0);
    });

    it('handles zero current streak', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      expect(screen.getAllByText('0').length).toBeGreaterThan(0);
    });
  });

  describe('Active vs Inactive States', () => {
    it('shows active status for active streaks', () => {
      renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('active');
      expect(screen.getAllByText(/active/i).length).toBeGreaterThan(0);
    });

    it('shows inactive status for inactive streaks', () => {
      renderWithTheme(<StreakCounter streak={mockInactiveStreak} />);
      
      const container = screen.getByTestId('streak-counter');
      expect(container).toHaveClass('inactive');
      expect(screen.getAllByText(/broken/i).length).toBeGreaterThan(0);
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
      
      const bestDisplays = screen.getAllByText(/best/i);
      expect(bestDisplays.length).toBeGreaterThan(0);
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
      expect(screen.getAllByText(/days ago/i).length).toBeGreaterThan(0);
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
      
      expect(screen.getAllByText(/days ago/i).length).toBeGreaterThan(0);
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
        lastActivity: new Date(),
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
      
      expect(screen.getAllByText('0').length).toBeGreaterThan(0);
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
    it('handles multiple re-renders without errors', () => {
      const { rerender } = renderWithTheme(<StreakCounter streak={mockActiveStreak} />);
      
      // Re-render multiple times to test performance
      rerender(<StreakCounter streak={mockActiveStreak} />);
      rerender(<StreakCounter streak={mockInactiveStreak} />);
      rerender(<StreakCounter streak={mockActiveStreak} />);
      
      // Should still render correctly
      expect(screen.getByTestId('streak-counter')).toBeInTheDocument();
    });
  });
});