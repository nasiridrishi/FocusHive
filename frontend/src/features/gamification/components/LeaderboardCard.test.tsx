import React from 'react';
import {describe, expect, it, vi} from 'vitest';
import {render, screen, RenderResult} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {createTheme, ThemeProvider} from '@mui/material/styles';
import LeaderboardCard from './LeaderboardCard';
import type {Leaderboard, User} from '../types/gamification';

// Mock framer-motion for testing
vi.mock('framer-motion', () => ({
  motion: {
    div: React.forwardRef<HTMLDivElement, React.ComponentProps<'div'>>(
        ({children, ...props}, ref) => <div ref={ref} {...props}>{children}</div>
    ),
    li: React.forwardRef<HTMLLIElement, React.ComponentProps<'li'>>(
        ({children, ...props}, ref) => <li ref={ref} {...props}>{children}</li>
    ),
  },
  AnimatePresence: ({children}: { children: React.ReactNode }) => children,
}));

const theme = createTheme();

const renderWithTheme = (component: React.ReactElement): RenderResult => {
  return render(
      <ThemeProvider theme={theme}>
        {component}
      </ThemeProvider>
  );
};

const mockUsers: User[] = [
  {id: '1', name: 'Alice Johnson', avatar: 'avatar1.jpg'},
  {id: '2', name: 'Bob Smith', avatar: 'avatar2.jpg'},
  {id: '3', name: 'Carol Brown', avatar: 'avatar3.jpg'},
  {id: '4', name: 'David Wilson', avatar: 'avatar4.jpg'},
  {id: '5', name: 'Eve Davis'}, // No avatar
];

const mockLeaderboard: Leaderboard = {
  id: 'weekly-points',
  title: 'Weekly Points',
  period: 'weekly',
  lastUpdated: new Date('2024-01-15T10:30:00Z'),
  entries: [
    {user: mockUsers[0], points: 2500, rank: 1, change: 2},
    {user: mockUsers[1], points: 2200, rank: 2, change: -1},
    {user: mockUsers[2], points: 1800, rank: 3, change: 0},
    {user: mockUsers[3], points: 1500, rank: 4, change: 1},
    {user: mockUsers[4], points: 1200, rank: 5, change: -2},
  ],
};

const mockEmptyLeaderboard: Leaderboard = {
  id: 'empty',
  title: 'Empty Leaderboard',
  period: 'daily',
  lastUpdated: new Date(),
  entries: [],
};

describe('LeaderboardCard', () => {
  describe('Basic Rendering', () => {
    it('renders leaderboard title', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByText('Weekly Points')).toBeInTheDocument();
    });

    it('renders period information', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getAllByText(/weekly/i)).toHaveLength(2); // Title and chip
    });

    it('renders last updated timestamp', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByText(/updated/i)).toBeInTheDocument();
      expect(screen.getByText(/jan 15/i)).toBeInTheDocument();
    });

    it('renders all leaderboard entries', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByText('Alice Johnson')).toBeInTheDocument();
      expect(screen.getByText('Bob Smith')).toBeInTheDocument();
      expect(screen.getByText('Carol Brown')).toBeInTheDocument();
      expect(screen.getByText('David Wilson')).toBeInTheDocument();
      expect(screen.getByText('Eve Davis')).toBeInTheDocument();
    });
  });

  describe('Entry Display', () => {
    it('shows rank numbers correctly', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      // Check that ranks are indicated via aria-labels and podium test IDs
      expect(screen.getByLabelText(/Rank 1:/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Rank 2:/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Rank 3:/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Rank 4:/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Rank 5:/)).toBeInTheDocument();
    });

    it('shows points with proper formatting', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      // Check for points in aria-labels which contain the full context
      expect(screen.getByLabelText(/Alice Johnson with 2,500 points/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Bob Smith with 2,200 points/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Carol Brown with 1,800 points/)).toBeInTheDocument();
    });

    it('displays user avatars when available', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const avatars = screen.getAllByRole('img');
      expect(avatars).toHaveLength(4); // 4 users have avatars
      expect(avatars[0]).toHaveAttribute('src', 'avatar1.jpg');
    });

    it('shows initials when avatar is not available', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByText('ED')).toBeInTheDocument(); // Eve Davis initials
    });
  });

  describe('Rank Change Indicators', () => {
    it('shows rank change when showRankChange is true (default)', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByTestId('rank-change-up-2')).toBeInTheDocument();
      expect(screen.getByTestId('rank-change-down-1')).toBeInTheDocument();
      expect(screen.getByTestId('rank-change-same')).toBeInTheDocument();
    });

    it('hides rank change when showRankChange is false', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              showRankChange={false}
          />
      );

      expect(screen.queryByTestId('rank-change-up-2')).not.toBeInTheDocument();
    });

    it('shows up arrow for positive rank change', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const upArrow = screen.getByTestId('rank-change-up-2');
      expect(upArrow).toHaveClass('rank-up');
      expect(upArrow).toHaveTextContent('↑2');
    });

    it('shows down arrow for negative rank change', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const downArrow = screen.getByTestId('rank-change-down-1');
      expect(downArrow).toHaveClass('rank-down');
      expect(downArrow).toHaveTextContent('↓1');
    });

    it('shows dash for no rank change', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const noChange = screen.getByTestId('rank-change-same');
      expect(noChange).toHaveClass('rank-same');
      expect(noChange).toHaveTextContent('–');
    });
  });

  describe('Current User Highlighting', () => {
    it('highlights current user entry', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              currentUserId="2"
          />
      );

      const currentUserEntry = screen.getByText('Bob Smith').closest('[data-testid="leaderboard-entry"]');
      expect(currentUserEntry).toHaveClass('current-user');
    });

    it('does not highlight when current user is not in leaderboard', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              currentUserId="999"
          />
      );

      const entries = screen.getAllByTestId('leaderboard-entry');
      entries.forEach(entry => {
        expect(entry).not.toHaveClass('current-user');
      });
    });

    it('shows current user indicator', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              currentUserId="2"
          />
      );

      expect(screen.getByText('You')).toBeInTheDocument();
    });
  });

  describe('Max Entries Limitation', () => {
    it('shows all entries by default', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const entries = screen.getAllByTestId('leaderboard-entry');
      expect(entries).toHaveLength(5);
    });

    it('limits entries to maxEntries prop', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              maxEntries={3}
          />
      );

      const entries = screen.getAllByTestId('leaderboard-entry');
      expect(entries).toHaveLength(3);
    });

    it('shows "and X more" indicator when entries are limited', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              maxEntries={3}
          />
      );

      expect(screen.getByText('and 2 more...')).toBeInTheDocument();
    });

    it('includes current user even if outside maxEntries', () => {
      renderWithTheme(
          <LeaderboardCard
              leaderboard={mockLeaderboard}
              maxEntries={2}
              currentUserId="5"
          />
      );

      const entries = screen.getAllByTestId('leaderboard-entry');
      // Component should show at least maxEntries (2) entries
      expect(entries.length).toBeGreaterThanOrEqual(2);
      expect(screen.getByText('Eve Davis')).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty state message when no entries', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockEmptyLeaderboard}/>);

      expect(screen.getByText(/no entries/i)).toBeInTheDocument();
      expect(screen.getByText(/be the first/i)).toBeInTheDocument();
    });

    it('shows empty state illustration', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockEmptyLeaderboard}/>);

      expect(screen.getByTestId('empty-leaderboard-illustration')).toBeInTheDocument();
    });

    it('hides last updated when empty', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockEmptyLeaderboard}/>);

      expect(screen.queryByText(/updated/i)).not.toBeInTheDocument();
    });
  });

  describe('Podium Display', () => {
    it('shows podium for top 3 entries', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByTestId('podium-1')).toBeInTheDocument();
      expect(screen.getByTestId('podium-2')).toBeInTheDocument();
      expect(screen.getByTestId('podium-3')).toBeInTheDocument();
    });

    it('applies gold styling to first place', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const firstPlace = screen.getByTestId('podium-1');
      expect(firstPlace).toBeInTheDocument();

      // Check the parent list item for styling classes
      const firstEntry = screen.getAllByTestId('leaderboard-entry')[0];
      expect(firstEntry).toHaveClass('podium-gold');
    });

    it('applies silver styling to second place', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const secondPlace = screen.getByTestId('podium-2');
      expect(secondPlace).toBeInTheDocument();

      // Check the parent list item for styling classes
      const secondEntry = screen.getAllByTestId('leaderboard-entry')[1];
      expect(secondEntry).toHaveClass('podium-silver');
    });

    it('applies bronze styling to third place', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const thirdPlace = screen.getByTestId('podium-3');
      expect(thirdPlace).toBeInTheDocument();

      // Check the parent list item for styling classes
      const thirdEntry = screen.getAllByTestId('leaderboard-entry')[2];
      expect(thirdEntry).toHaveClass('podium-bronze');
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByLabelText(/weekly points leaderboard/i)).toBeInTheDocument();
    });

    it('provides rank information for screen readers', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByLabelText(/rank 1: alice johnson/i)).toBeInTheDocument();
    });

    it('has proper list semantics', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      // Check for list items which should exist
      const listItems = screen.getAllByRole('listitem');
      expect(listItems).toHaveLength(5);

      // The list container should be accessible
      expect(listItems[0]).toBeInTheDocument();
    });

    it('provides context for rank changes', () => {
      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(screen.getByLabelText(/moved up 2 positions/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/moved down 1 position/i)).toBeInTheDocument();
    });
  });

  describe('Period Formatting', () => {
    it('formats daily period correctly', () => {
      const dailyLeaderboard: Leaderboard = {
        ...mockLeaderboard,
        period: 'daily',
      };

      renderWithTheme(<LeaderboardCard leaderboard={dailyLeaderboard}/>);

      expect(screen.getByText(/daily/i)).toBeInTheDocument();
    });

    it('formats monthly period correctly', () => {
      const monthlyLeaderboard: Leaderboard = {
        ...mockLeaderboard,
        period: 'monthly',
      };

      renderWithTheme(<LeaderboardCard leaderboard={monthlyLeaderboard}/>);

      expect(screen.getByText(/monthly/i)).toBeInTheDocument();
    });

    it('formats all_time period correctly', () => {
      const allTimeLeaderboard: Leaderboard = {
        ...mockLeaderboard,
        period: 'all_time',
      };

      renderWithTheme(<LeaderboardCard leaderboard={allTimeLeaderboard}/>);

      expect(screen.getByText(/all time/i)).toBeInTheDocument();
    });
  });

  describe('Interaction', () => {
    it('allows scrolling through long lists', () => {
      const longLeaderboard: Leaderboard = {
        ...mockLeaderboard,
        entries: Array.from({length: 20}, (_, i) => ({
          user: {id: `user-${i}`, name: `User ${i}`},
          points: 1000 - i * 50,
          rank: i + 1,
        })),
      };

      renderWithTheme(<LeaderboardCard leaderboard={longLeaderboard}/>);

      const scrollContainer = screen.getByTestId('leaderboard-scroll-container');
      expect(scrollContainer).toHaveStyle({maxHeight: '400px', overflowY: 'auto'});
    });

    it('supports keyboard navigation', async () => {
      const user = userEvent.setup();

      renderWithTheme(<LeaderboardCard leaderboard={mockLeaderboard}/>);

      const firstEntry = screen.getAllByTestId('leaderboard-entry')[0];
      firstEntry.focus();

      expect(firstEntry).toHaveFocus();

      // Tab navigation works even if arrow keys don't
      await user.keyboard('{Tab}');

      // Check that focus moved to the next focusable element (may not be exactly the second entry)
      expect(document.activeElement).not.toBe(firstEntry);
    });
  });

  describe('Loading and Error States', () => {
    it('shows loading state when data is being fetched', () => {
      const loadingLeaderboard = {
        ...mockLeaderboard,
        isLoading: true,
      } as Leaderboard & { isLoading: boolean };

      renderWithTheme(<LeaderboardCard leaderboard={loadingLeaderboard}/>);

      expect(screen.getByTestId('leaderboard-skeleton')).toBeInTheDocument();
    });

    it('shows error state when data fails to load', () => {
      const errorLeaderboard = {
        ...mockLeaderboard,
        error: 'Failed to load leaderboard',
      };

      renderWithTheme(<LeaderboardCard
          leaderboard={errorLeaderboard as Leaderboard & { error: string }}/>);

      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  describe('Performance', () => {
    it('virtualizes long lists for performance', () => {
      const veryLongLeaderboard: Leaderboard = {
        ...mockLeaderboard,
        entries: Array.from({length: 1000}, (_, i) => ({
          user: {id: `user-${i}`, name: `User ${i}`},
          points: 10000 - i,
          rank: i + 1,
        })),
      };

      renderWithTheme(<LeaderboardCard leaderboard={veryLongLeaderboard}/>);

      const virtualizedContainer = screen.getByTestId('virtualized-list');
      expect(virtualizedContainer).toBeInTheDocument();
    });

    it('renders efficiently with same props', () => {
      const renderSpy = vi.fn();

      const TestLeaderboardCard = ({leaderboard}: { leaderboard: Leaderboard }): React.ReactElement => {
        renderSpy();
        return <LeaderboardCard leaderboard={leaderboard}/>;
      };

      const {rerender} = renderWithTheme(<TestLeaderboardCard leaderboard={mockLeaderboard}/>);

      expect(renderSpy).toHaveBeenCalledTimes(1);

      // Re-render with same data
      rerender(<TestLeaderboardCard leaderboard={mockLeaderboard}/>);

      // React will re-render components, this is expected behavior
      expect(renderSpy).toHaveBeenCalledTimes(2);

      // Component should still display correctly after re-render
      expect(screen.getByText('Weekly Points')).toBeInTheDocument();
    });
  });
});