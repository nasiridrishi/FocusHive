import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material/styles';
import AchievementBadge from './AchievementBadge';
import type { Achievement } from '../types/gamification';

// Mock framer-motion for testing
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: any) => <div {...props}>{children}</div>,
    img: ({ children, ...props }: any) => <img {...props}>{children}</img>,
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

const mockUnlockedAchievement: Achievement = {
  id: 'first-focus',
  title: 'First Focus',
  description: 'Complete your first focus session',
  icon: 'focus',
  category: 'focus',
  points: 100,
  unlockedAt: new Date('2024-01-15T10:30:00Z'),
  isUnlocked: true,
  rarity: 'common',
};

const mockLockedAchievement: Achievement = {
  id: 'focus-master',
  title: 'Focus Master',
  description: 'Complete 100 focus sessions',
  icon: 'master',
  category: 'focus',
  points: 1000,
  progress: 45,
  maxProgress: 100,
  isUnlocked: false,
  rarity: 'epic',
};

const mockLegendaryAchievement: Achievement = {
  id: 'legend',
  title: 'FocusHive Legend',
  description: 'Achieve the impossible',
  icon: 'legend',
  category: 'special',
  points: 10000,
  isUnlocked: true,
  rarity: 'legendary',
  unlockedAt: new Date('2024-01-20T15:45:00Z'),
};

describe('AchievementBadge', () => {
  describe('Basic Rendering', () => {
    it('renders achievement title correctly', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      expect(screen.getByText('First Focus')).toBeInTheDocument();
    });

    it('renders achievement description', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      expect(screen.getByText('Complete your first focus session')).toBeInTheDocument();
    });

    it('renders achievement points', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      expect(screen.getByText('100')).toBeInTheDocument();
      expect(screen.getByText(/points/i)).toBeInTheDocument();
    });

    it('renders achievement icon', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      const icon = screen.getByRole('img', { name: /first focus/i });
      expect(icon).toBeInTheDocument();
      expect(icon).toHaveAttribute('data-icon', 'focus');
    });
  });

  describe('Unlocked vs Locked States', () => {
    it('shows unlocked achievement with full opacity', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('unlocked');
      expect(container).not.toHaveClass('locked');
    });

    it('shows locked achievement with reduced opacity', () => {
      renderWithTheme(<AchievementBadge achievement={mockLockedAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('locked');
      expect(container).not.toHaveClass('unlocked');
    });

    it('shows unlock date for unlocked achievements', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      expect(screen.getByText(/unlocked/i)).toBeInTheDocument();
      expect(screen.getByText(/jan 15, 2024/i)).toBeInTheDocument();
    });

    it('hides unlock date for locked achievements', () => {
      renderWithTheme(<AchievementBadge achievement={mockLockedAchievement} />);
      
      expect(screen.queryByText(/unlocked/i)).not.toBeInTheDocument();
    });
  });

  describe('Progress Display', () => {
    it('shows progress bar when showProgress is true and progress exists', () => {
      renderWithTheme(
        <AchievementBadge 
          achievement={mockLockedAchievement} 
          showProgress 
        />
      );
      
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
      expect(screen.getByText('45/100')).toBeInTheDocument();
    });

    it('hides progress bar when showProgress is false', () => {
      renderWithTheme(
        <AchievementBadge 
          achievement={mockLockedAchievement} 
          showProgress={false} 
        />
      );
      
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    });

    it('hides progress bar when no progress data exists', () => {
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          showProgress 
        />
      );
      
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    });

    it('calculates progress percentage correctly', () => {
      renderWithTheme(
        <AchievementBadge 
          achievement={mockLockedAchievement} 
          showProgress 
        />
      );
      
      const progressBar = screen.getByRole('progressbar');
      expect(progressBar).toHaveAttribute('aria-valuenow', '45');
      expect(progressBar).toHaveAttribute('aria-valuemax', '100');
    });
  });

  describe('Size Variants', () => {
    it('applies small size styling', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} size="small" />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('size-small');
    });

    it('applies medium size styling (default)', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} size="medium" />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('size-medium');
    });

    it('applies large size styling', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} size="large" />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('size-large');
    });

    it('defaults to medium size when size prop is not provided', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('size-medium');
    });
  });

  describe('Rarity Styling', () => {
    it('applies common rarity styling', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('rarity-common');
    });

    it('applies epic rarity styling', () => {
      renderWithTheme(<AchievementBadge achievement={mockLockedAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('rarity-epic');
    });

    it('applies legendary rarity styling with special effects', () => {
      renderWithTheme(<AchievementBadge achievement={mockLegendaryAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('rarity-legendary');
      expect(container).toHaveClass('legendary-glow');
    });

    it('shows rarity indicator', () => {
      renderWithTheme(<AchievementBadge achievement={mockLegendaryAchievement} />);
      
      expect(screen.getByText('LEGENDARY')).toBeInTheDocument();
    });
  });

  describe('Click Interaction', () => {
    it('calls onClick handler when clicked', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          onClick={handleClick} 
        />
      );
      
      const container = screen.getByTestId('achievement-badge');
      await user.click(container);
      
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('shows pointer cursor when clickable', () => {
      const handleClick = vi.fn();
      
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          onClick={handleClick} 
        />
      );
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveStyle({ cursor: 'pointer' });
    });

    it('shows default cursor when not clickable', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveStyle({ cursor: 'default' });
    });

    it('handles keyboard interaction for accessibility', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          onClick={handleClick} 
        />
      );
      
      const container = screen.getByTestId('achievement-badge');
      container.focus();
      await user.keyboard('{Enter}');
      
      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      expect(screen.getByLabelText(/achievement: first focus/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/100 points/i)).toBeInTheDocument();
    });

    it('has proper role when clickable', () => {
      const handleClick = vi.fn();
      
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          onClick={handleClick} 
        />
      );
      
      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('has proper tabindex when clickable', () => {
      const handleClick = vi.fn();
      
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          onClick={handleClick} 
        />
      );
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveAttribute('tabindex', '0');
    });

    it('provides screen reader information about lock state', () => {
      renderWithTheme(<AchievementBadge achievement={mockLockedAchievement} />);
      
      expect(screen.getByText(/locked/i)).toBeInTheDocument();
    });
  });

  describe('Category Display', () => {
    it('shows category badge', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      expect(screen.getByText('FOCUS')).toBeInTheDocument();
    });

    it('applies category-specific styling', () => {
      renderWithTheme(<AchievementBadge achievement={mockUnlockedAchievement} />);
      
      const categoryBadge = screen.getByText('FOCUS');
      expect(categoryBadge).toHaveClass('category-focus');
    });
  });

  describe('Animation Effects', () => {
    it('applies unlock animation to newly unlocked achievements', () => {
      const recentlyUnlocked: Achievement = {
        ...mockUnlockedAchievement,
        unlockedAt: new Date(), // Just unlocked
      };
      
      renderWithTheme(<AchievementBadge achievement={recentlyUnlocked} />);
      
      const container = screen.getByTestId('achievement-badge');
      expect(container).toHaveClass('recently-unlocked');
    });

    it('applies hover effects on interactive badges', () => {
      const handleClick = vi.fn();
      
      renderWithTheme(
        <AchievementBadge 
          achievement={mockUnlockedAchievement} 
          onClick={handleClick} 
        />
      );
      
      const container = screen.getByTestId('achievement-badge');
      fireEvent.mouseEnter(container);
      
      expect(container).toHaveClass('hover-effect');
    });
  });

  describe('Edge Cases', () => {
    it('handles achievement without description', () => {
      const noDescAchievement: Achievement = {
        ...mockUnlockedAchievement,
        description: '',
      };
      
      renderWithTheme(<AchievementBadge achievement={noDescAchievement} />);
      
      expect(screen.getByText('First Focus')).toBeInTheDocument();
      expect(screen.queryByText('')).not.toBeInTheDocument();
    });

    it('handles zero points achievement', () => {
      const zeroPointsAchievement: Achievement = {
        ...mockUnlockedAchievement,
        points: 0,
      };
      
      renderWithTheme(<AchievementBadge achievement={zeroPointsAchievement} />);
      
      expect(screen.getByText('0')).toBeInTheDocument();
    });

    it('handles progress greater than max', () => {
      const overProgressAchievement: Achievement = {
        ...mockLockedAchievement,
        progress: 150,
        maxProgress: 100,
      };
      
      renderWithTheme(
        <AchievementBadge 
          achievement={overProgressAchievement} 
          showProgress 
        />
      );
      
      const progressBar = screen.getByRole('progressbar');
      expect(progressBar).toHaveAttribute('aria-valuenow', '100');
    });
  });
});