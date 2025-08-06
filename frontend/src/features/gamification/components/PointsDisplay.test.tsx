import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material/styles';
import PointsDisplay from './PointsDisplay';
import type { Points } from '../types/gamification';

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

const mockPoints: Points = {
  current: 1250,
  total: 15750,
  todayEarned: 150,
  weekEarned: 420,
};

describe('PointsDisplay', () => {
  describe('Basic Rendering', () => {
    it('renders current points correctly', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} />);
      
      expect(screen.getByText('1,250')).toBeInTheDocument();
      expect(screen.getByText(/points/i)).toBeInTheDocument();
    });

    it('renders total points when provided', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} />);
      
      expect(screen.getByText(/15,750/)).toBeInTheDocument();
      expect(screen.getByText(/total/i)).toBeInTheDocument();
    });

    it('handles zero points correctly', () => {
      const zeroPoints: Points = {
        current: 0,
        total: 0,
        todayEarned: 0,
        weekEarned: 0,
      };
      
      renderWithTheme(<PointsDisplay points={zeroPoints} />);
      
      expect(screen.getByText('0')).toBeInTheDocument();
    });

    it('handles large numbers correctly', () => {
      const largePoints: Points = {
        current: 1234567,
        total: 9876543,
        todayEarned: 5000,
        weekEarned: 25000,
      };
      
      renderWithTheme(<PointsDisplay points={largePoints} />);
      
      expect(screen.getByText('1,234,567')).toBeInTheDocument();
    });
  });

  describe('Today Points Display', () => {
    it('shows today points when showToday is true', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} showToday />);
      
      expect(screen.getByText('150')).toBeInTheDocument();
      expect(screen.getByText(/today/i)).toBeInTheDocument();
    });

    it('hides today points when showToday is false', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} showToday={false} />);
      
      expect(screen.queryByText(/today/i)).not.toBeInTheDocument();
    });

    it('shows zero for today points when none earned', () => {
      const noTodayPoints: Points = {
        ...mockPoints,
        todayEarned: 0,
      };
      
      renderWithTheme(<PointsDisplay points={noTodayPoints} showToday />);
      
      expect(screen.getByText(/today/i)).toBeInTheDocument();
      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });

  describe('Week Points Display', () => {
    it('shows week points when showWeek is true', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} showWeek />);
      
      expect(screen.getByText('420')).toBeInTheDocument();
      expect(screen.getByText(/week/i)).toBeInTheDocument();
    });

    it('hides week points when showWeek is false', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} showWeek={false} />);
      
      expect(screen.queryByText(/week/i)).not.toBeInTheDocument();
    });
  });

  describe('Size Variants', () => {
    it('applies small size styling', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} size="small" />);
      
      const pointsElement = screen.getByText('1,250');
      expect(pointsElement).toHaveStyle({ fontSize: expect.stringContaining('1.5rem') });
    });

    it('applies medium size styling (default)', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} size="medium" />);
      
      const pointsElement = screen.getByText('1,250');
      expect(pointsElement).toHaveStyle({ fontSize: expect.stringContaining('2rem') });
    });

    it('applies large size styling', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} size="large" />);
      
      const pointsElement = screen.getByText('1,250');
      expect(pointsElement).toHaveStyle({ fontSize: expect.stringContaining('2.5rem') });
    });

    it('defaults to medium size when size prop is not provided', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} />);
      
      const pointsElement = screen.getByText('1,250');
      expect(pointsElement).toHaveStyle({ fontSize: expect.stringContaining('2rem') });
    });
  });

  describe('Animation', () => {
    it('enables animation when animated prop is true', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} animated />);
      
      // Check for animation-related attributes
      const container = screen.getByText('1,250').closest('[data-testid="points-display"]');
      expect(container).toHaveAttribute('data-animated', 'true');
    });

    it('disables animation when animated prop is false', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} animated={false} />);
      
      const container = screen.getByText('1,250').closest('[data-testid="points-display"]');
      expect(container).toHaveAttribute('data-animated', 'false');
    });

    it('defaults to animated when animated prop is not provided', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} />);
      
      const container = screen.getByText('1,250').closest('[data-testid="points-display"]');
      expect(container).toHaveAttribute('data-animated', 'true');
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} showToday showWeek />);
      
      expect(screen.getByLabelText(/current points/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/total points/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/points earned today/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/points earned this week/i)).toBeInTheDocument();
    });

    it('has proper semantic structure', () => {
      renderWithTheme(<PointsDisplay points={mockPoints} />);
      
      const container = screen.getByRole('region', { name: /points display/i });
      expect(container).toBeInTheDocument();
    });
  });

  describe('Responsive Design', () => {
    it('adapts to mobile viewports', () => {
      // Mock mobile viewport
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 375,
      });

      renderWithTheme(<PointsDisplay points={mockPoints} showToday showWeek />);
      
      const container = screen.getByRole('region', { name: /points display/i });
      expect(container).toHaveClass('mobile-layout');
    });

    it('adapts to tablet viewports', () => {
      // Mock tablet viewport
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 768,
      });

      renderWithTheme(<PointsDisplay points={mockPoints} showToday showWeek />);
      
      const container = screen.getByRole('region', { name: /points display/i });
      expect(container).toHaveClass('tablet-layout');
    });
  });

  describe('Edge Cases', () => {
    it('handles negative points gracefully', () => {
      const negativePoints: Points = {
        current: -100,
        total: 500,
        todayEarned: -50,
        weekEarned: 100,
      };
      
      renderWithTheme(<PointsDisplay points={negativePoints} showToday />);
      
      expect(screen.getByText('-100')).toBeInTheDocument();
      expect(screen.getByText('-50')).toBeInTheDocument();
    });

    it('handles undefined optional props', () => {
      const incompletePoints = {
        current: 1000,
        total: 5000,
        todayEarned: 100,
        weekEarned: 300,
      };
      
      expect(() => {
        renderWithTheme(<PointsDisplay points={incompletePoints} />);
      }).not.toThrow();
    });
  });

  describe('Performance', () => {
    it('does not re-render unnecessarily', () => {
      const renderSpy = vi.fn();
      
      const TestComponent = () => {
        renderSpy();
        return <PointsDisplay points={mockPoints} />;
      };
      
      const { rerender } = renderWithTheme(<TestComponent />);
      
      // Initial render
      expect(renderSpy).toHaveBeenCalledTimes(1);
      
      // Re-render with same props
      rerender(<TestComponent />);
      
      // Should use memoization to prevent unnecessary re-renders
      expect(renderSpy).toHaveBeenCalledTimes(1);
    });
  });
});